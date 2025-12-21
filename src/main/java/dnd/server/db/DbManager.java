package dnd.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Database Manager với connection pooling sử dụng HikariCP
 * Copy từ dnd.db.DbManager để server độc lập
 */
public class DbManager {
    private static DbManager instance = null;
    private HikariDataSource hikariDataSource;
    private static final Logger logger = Logger.getLogger(DbManager.class.getName());

    /**
     * Singleton pattern
     */
    public static DbManager getInstance() {
        if (instance == null) {
            synchronized (DbManager.class) {
                if (instance == null) {
                    instance = new DbManager();
                }
            }
        }
        return instance;
    }

    private DbManager() {
        // Private constructor for singleton
    }

    /**
     * Khởi tạo connection pool
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean start() {
        if (this.hikariDataSource != null) {
            logger.warning("DB Connection Pool has already been created.");
            return false;
        }

        try {
            DbConfig config = DbConfig.getInstance();
            HikariConfig hikariConfig = new HikariConfig();

            hikariConfig.setJdbcUrl(config.getJdbcUrl());
            hikariConfig.setDriverClassName(config.getDriverClassName());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());

            // Connection pool settings
            hikariConfig.setMinimumIdle(config.getMinConnections());
            hikariConfig.setMaximumPoolSize(config.getMaxConnections());
            hikariConfig.setConnectionTimeout(config.getConnectionTimeout());
            hikariConfig.setIdleTimeout(config.getIdleTimeout());
            hikariConfig.setLeakDetectionThreshold(config.getLeakDetectionThreshold());

            // Performance optimizations
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
            hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");

            // Connection validation
            hikariConfig.setConnectionTestQuery("SELECT 1");

            this.hikariDataSource = new HikariDataSource(hikariConfig);
            logger.info("DB Connection Pool has been created successfully.");
            return true;

        } catch (Exception e) {
            logger.severe("DB Connection Pool Creation has failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy connection từ pool
     * @return Connection object
     * @throws SQLException nếu không thể lấy connection
     */
    public Connection getConnection() throws SQLException {
        if (hikariDataSource == null) {
            throw new SQLException("Connection pool has not been initialized. Call start() first.");
        }
        return hikariDataSource.getConnection();
    }

    /**
     * Chuyển đổi ResultSet thành List<HashMap<String, Object>>
     * Tự động detect và convert các kiểu dữ liệu phổ biến
     * 
     * @param rs ResultSet cần convert
     * @return List chứa các HashMap, mỗi HashMap là một row
     */
    public List<HashMap<String, Object>> convertResultSetToList(ResultSet rs) {
        List<HashMap<String, Object>> list = new ArrayList<>();

        if (rs == null) {
            return list;
        }

        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                HashMap<String, Object> map = new HashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    int columnType = metaData.getColumnType(i);
                    Object value = getValueByType(rs, i, columnType);
                    map.put(columnName, value);
                }

                list.add(map);
            }

        } catch (SQLException e) {
            logger.severe("Error converting ResultSet to List: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null && !rs.isClosed()) {
                    rs.close();
                }
            } catch (SQLException e) {
                logger.warning("Error closing ResultSet: " + e.getMessage());
            }
        }

        return list;
    }

    /**
     * Lấy giá trị từ ResultSet theo type
     */
    private Object getValueByType(ResultSet rs, int index, int sqlType) throws SQLException {
        switch (sqlType) {
            case Types.BIGINT:
                return rs.getLong(index);
            case Types.INTEGER:
                return rs.getInt(index);
            case Types.SMALLINT:
            case Types.TINYINT:
                return rs.getByte(index);
            case Types.FLOAT:
            case Types.REAL:
                return rs.getFloat(index);
            case Types.DOUBLE:
                return rs.getDouble(index);
            case Types.DECIMAL:
            case Types.NUMERIC:
                return rs.getBigDecimal(index);
            case Types.BOOLEAN:
            case Types.BIT:
                return rs.getBoolean(index);
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
                return rs.getString(index);
            case Types.DATE:
                return rs.getDate(index);
            case Types.TIME:
                return rs.getTime(index);
            case Types.TIMESTAMP:
                return rs.getTimestamp(index);
            case Types.BLOB:
                return rs.getBytes(index);
            case Types.CLOB:
                return rs.getClob(index);
            default:
                return rs.getObject(index);
        }
    }

    /**
     * Thực hiện UPDATE/INSERT/DELETE query
     * 
     * @param sql SQL query với placeholders (?)
     * @param params Các tham số để bind vào query
     * @return Số dòng bị ảnh hưởng, -1 nếu có lỗi
     */
    public int update(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Bind parameters
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            int result = stmt.executeUpdate();
            return result;

        } catch (SQLException e) {
            logger.severe("Error executing update query: " + e.getMessage());
            logger.severe("SQL: " + sql);
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Thực hiện SELECT query và trả về List<HashMap>
     * 
     * @param sql SQL query với placeholders (?)
     * @param params Các tham số để bind vào query
     * @return List chứa kết quả, empty list nếu không có kết quả hoặc có lỗi
     */
    public List<HashMap<String, Object>> query(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Bind parameters
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return convertResultSetToList(rs);
            }

        } catch (SQLException e) {
            logger.severe("Error executing query: " + e.getMessage());
            logger.severe("SQL: " + sql);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Thực hiện SELECT query và trả về một row duy nhất
     * 
     * @param sql SQL query với placeholders (?)
     * @param params Các tham số để bind vào query
     * @return HashMap chứa kết quả, null nếu không có kết quả hoặc có lỗi
     */
    public HashMap<String, Object> queryOne(String sql, Object... params) {
        List<HashMap<String, Object>> results = query(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Thực hiện INSERT và trả về generated key
     * 
     * @param sql SQL INSERT query với placeholders (?)
     * @param params Các tham số để bind vào query
     * @return Generated key (thường là ID), null nếu có lỗi
     */
    public Long insertAndGetId(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // Bind parameters
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                return null;
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
            }

            return null;

        } catch (SQLException e) {
            logger.severe("Error executing insert query: " + e.getMessage());
            logger.severe("SQL: " + sql);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Thực hiện batch update (nhiều queries cùng lúc)
     * 
     * @param sql SQL query với placeholders (?)
     * @param paramsList List các mảng params, mỗi mảng là một batch
     * @return Mảng chứa số dòng bị ảnh hưởng cho mỗi batch
     */
    public int[] batchUpdate(String sql, List<Object[]> paramsList) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (Object[] params : paramsList) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
                stmt.addBatch();
            }

            return stmt.executeBatch();

        } catch (SQLException e) {
            logger.severe("Error executing batch update: " + e.getMessage());
            logger.severe("SQL: " + sql);
            e.printStackTrace();
            return new int[0];
        }
    }

    /**
     * Kiểm tra xem connection pool đã được khởi tạo chưa
     */
    public boolean isInitialized() {
        return hikariDataSource != null && !hikariDataSource.isClosed();
    }

    /**
     * Lấy thông tin về connection pool
     */
    public String getPoolInfo() {
        if (hikariDataSource == null) {
            return "Connection pool not initialized";
        }
        return String.format(
            "Active: %d, Idle: %d, Total: %d, Waiting: %d",
            hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
            hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
            hikariDataSource.getHikariPoolMXBean().getTotalConnections(),
            hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }

    /**
     * Đóng connection pool
     */
    public void shutdown() {
        try {
            if (this.hikariDataSource != null && !this.hikariDataSource.isClosed()) {
                this.hikariDataSource.close();
                logger.info("DB Connection Pool is shutting down.");
            }
            this.hikariDataSource = null;
        } catch (Exception e) {
            logger.warning("Error when shutting down DB Connection Pool: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

