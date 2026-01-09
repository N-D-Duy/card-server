package dnd.server;

import dnd.server.db.DbManager;
import dnd.server.handler.HttpServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.logging.Logger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;

/**
 * MedCard Server sử dụng Netty để xử lý HTTP REST API
 */
public class MedCardServer {
    private static final Logger logger = Logger.getLogger(MedCardServer.class.getName());
    private static final int DEFAULT_PORT = 8888;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private DbManager dbManager;
    private boolean running = false;
    private int port;
    private ScheduledExecutorService scheduler;

    public MedCardServer(int port) {
        this.port = port;
    }

    public MedCardServer() {
        this(DEFAULT_PORT);
    }

    /**
     * Khởi động server
     */
    public void start() throws InterruptedException {
        if (running) {
            logger.warning("Server đã đang chạy!");
            return;
        }

        // Khởi tạo database connection pool
        logger.info("Đang khởi tạo database connection pool...");
        dbManager = DbManager.getInstance();
        if (!dbManager.start()) {
            throw new RuntimeException("Không thể kết nối database! Kiểm tra file db.properties và đảm bảo MySQL đang chạy.");
        }
        logger.info("✓ Database đã kết nối!");

        // Khởi động scheduled task để tự động hủy đơn thuốc quá 5 phút
        startPrescriptionTimeoutTask();

        // Tạo event loop groups
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new HttpServerCodec()) // HTTP encoder/decoder
                                    .addLast(new HttpObjectAggregator(1048576)) // Aggregate HTTP messages (max 1MB)
                                    .addLast(new HttpServerHandler(dbManager)); // Custom handler
                        }
                    });

            // Bind và start server
            serverChannel = bootstrap.bind(port).sync().channel();
            running = true;
            logger.info("MedCard Server đang lắng nghe trên http://localhost:" + port);

            // Đợi server channel đóng
            serverChannel.closeFuture().sync();
        } finally {
            stop();
        }
    }

    /**
     * Dừng server
     */
    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        logger.info("Đang dừng server...");

        // Dừng scheduled task
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (serverChannel != null) {
            serverChannel.close();
        }

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        // Đóng database connection pool
        if (dbManager != null) {
            dbManager.shutdown();
            logger.info("Database connection pool đã đóng");
        }

        logger.info("Server đã dừng");
    }

    /**
     * Kiểm tra xem server có đang chạy không
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Khởi động scheduled task để tự động hủy đơn thuốc quá 5 phút chưa thanh toán
     */
    private void startPrescriptionTimeoutTask() {
        scheduler = Executors.newScheduledThreadPool(1);
        
        // Chạy task mỗi 30 giây để kiểm tra và hủy đơn thuốc quá 5 phút
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cancelExpiredPrescriptions();
            } catch (Exception e) {
                logger.warning("Error in prescription timeout task: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS); // Bắt đầu sau 30s, lặp lại mỗi 30s
        
        logger.info("✓ Prescription timeout task đã khởi động (chạy mỗi 30 giây)");
    }

    /**
     * Hủy các đơn thuốc đã quá 5 phút chưa được thanh toán
     */
    private void cancelExpiredPrescriptions() {
        try {
            // Tìm các prescription có status = 0 (Mới) hoặc 1 (Đang xử lý)
            // và created_at > 5 phút trước, chưa có transaction thanh toán
            String sql = """
                SELECT p.id, p.prescription_code, p.created_at
                FROM prescriptions p
                WHERE (p.status = 0 OR p.status = 1)
                  AND p.created_at < DATE_SUB(NOW(), INTERVAL 5 MINUTE)
                  AND NOT EXISTS (
                      SELECT 1 FROM transactions t
                      WHERE t.ref = CONCAT('medcard ', p.id)
                  )
                """;
            
            java.util.List<HashMap<String, Object>> expiredPrescriptions = dbManager.query(sql);
            
            if (expiredPrescriptions.isEmpty()) {
                return; // Không có đơn nào cần hủy
            }
            
            logger.info("Tìm thấy " + expiredPrescriptions.size() + " đơn thuốc quá 5 phút chưa thanh toán");
            
            // Cập nhật status = 3 (Hủy) cho các đơn này
            String updateSql = """
                UPDATE prescriptions
                SET status = 3, updated_at = NOW()
                WHERE id = ?
                  AND (status = 0 OR status = 1)
                  AND created_at < DATE_SUB(NOW(), INTERVAL 5 MINUTE)
                  AND NOT EXISTS (
                      SELECT 1 FROM transactions t
                      WHERE t.ref = CONCAT('medcard ', prescriptions.id)
                  )
                """;
            
            int cancelledCount = 0;
            for (HashMap<String, Object> prescription : expiredPrescriptions) {
                Long prescriptionId = ((Number) prescription.get("id")).longValue();
                int affected = dbManager.update(updateSql, prescriptionId);
                if (affected > 0) {
                    cancelledCount++;
                    String prescriptionCode = (String) prescription.get("prescription_code");
                    logger.info("Đã hủy đơn thuốc: " + prescriptionCode + " (ID: " + prescriptionId + ") - quá 5 phút chưa thanh toán");
                }
            }
            
            if (cancelledCount > 0) {
                logger.info("Đã tự động hủy " + cancelledCount + " đơn thuốc quá 5 phút");
            }
        } catch (Exception e) {
            logger.warning("Lỗi khi hủy đơn thuốc quá 5 phút: " + e.getMessage());
        }
    }

    /**
     * Main method để chạy server
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Port không hợp lệ, sử dụng port mặc định: " + DEFAULT_PORT);
            }
        }

        MedCardServer server = new MedCardServer(port);
        
        // Đăng ký shutdown hook để đảm bảo server được dừng đúng cách
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nNhận tín hiệu shutdown, đang dừng server...");
            server.stop();
        }));

        try {
            server.start();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            server.stop();
        } catch (Exception e) {
            System.err.println("Lỗi khi khởi động server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
