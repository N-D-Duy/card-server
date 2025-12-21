package dnd.server.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration class cho database connection
 * Copy từ dnd.db.DbConfig để server độc lập
 */
public class DbConfig {
    private static DbConfig instance;
    private Properties properties;

    // Default values
    private String jdbcUrl = "jdbc:mysql://localhost:3306/medcard?useSSL=false&serverTimezone=UTC";
    private String driverClassName = "com.mysql.cj.jdbc.Driver";
    private String username = "root";
    private String password = "";
    private int minConnections = 2;
    private int maxConnections = 10;
    private long connectionTimeout = 30000; // 30 seconds
    private long idleTimeout = 600000; // 10 minutes
    private long leakDetectionThreshold = 60000; // 60 seconds

    private DbConfig() {
        loadConfig();
    }

    public static DbConfig getInstance() {
        if (instance == null) {
            instance = new DbConfig();
        }
        return instance;
    }

    /**
     * Load configuration from properties file or use defaults
     * Tìm file db.properties trong thư mục hiện tại hoặc thư mục server/
     */
    private void loadConfig() {
        properties = new Properties();
        
        // Thử load từ server/db.properties trước, sau đó thử db.properties ở root
        String[] paths = {"db.properties", "../db.properties"};
        
        for (String path : paths) {
            try (InputStream input = new FileInputStream(path)) {
                properties.load(input);
                jdbcUrl = properties.getProperty("db.url", jdbcUrl);
                driverClassName = properties.getProperty("db.driver", driverClassName);
                username = properties.getProperty("db.username", username);
                password = properties.getProperty("db.password", password);
                minConnections = Integer.parseInt(properties.getProperty("db.minConnections", String.valueOf(minConnections)));
                maxConnections = Integer.parseInt(properties.getProperty("db.maxConnections", String.valueOf(maxConnections)));
                connectionTimeout = Long.parseLong(properties.getProperty("db.connectionTimeout", String.valueOf(connectionTimeout)));
                idleTimeout = Long.parseLong(properties.getProperty("db.idleTimeout", String.valueOf(idleTimeout)));
                leakDetectionThreshold = Long.parseLong(properties.getProperty("db.leakDetectionThreshold", String.valueOf(leakDetectionThreshold)));
                return; // Load thành công, thoát
            } catch (IOException e) {
                // Tiếp tục thử path tiếp theo
            }
        }
        
        // Nếu không tìm thấy file, dùng default values
        System.out.println("db.properties not found, using default configuration");
    }

    // Getters
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getMinConnections() {
        return minConnections;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public long getLeakDetectionThreshold() {
        return leakDetectionThreshold;
    }
}

