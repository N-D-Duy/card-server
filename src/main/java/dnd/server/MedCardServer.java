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
