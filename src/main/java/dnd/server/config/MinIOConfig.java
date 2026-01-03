package dnd.server.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Config cho MinIO storage
 */
public class MinIOConfig {
    private static MinIOConfig instance;
    private final String endpoint;
    private final String publicEndpoint;
    private final String accessKey;
    private final String secretKey;
    private final String bucketName;
    
    private MinIOConfig() {
        Properties props = new Properties();
        try {
            // Load từ minio.properties hoặc dùng default
            try (FileInputStream fis = new FileInputStream("minio.properties")) {
                props.load(fis);
            } catch (IOException e) {
                // Nếu không có file, dùng default
                System.out.println("minio.properties not found, using defaults");
            }
            
            this.endpoint = props.getProperty("minio.endpoint", "localhost:9000");
            this.publicEndpoint = props.getProperty("minio.publicEndpoint", "localhost:9000");
            this.accessKey = props.getProperty("minio.accessKey", "minioadmin");
            this.secretKey = props.getProperty("minio.secretKey", "minioadmin");
            this.bucketName = props.getProperty("minio.bucket", "card");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load MinIO config", e);
        }
    }
    
    public static MinIOConfig getInstance() {
        if (instance == null) {
            instance = new MinIOConfig();
        }
        return instance;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public String getPublicEndpoint() {
        return publicEndpoint;
    }
    
    public String getAccessKey() {
        return accessKey;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public String getBucketName() {
        return bucketName;
    }
}

