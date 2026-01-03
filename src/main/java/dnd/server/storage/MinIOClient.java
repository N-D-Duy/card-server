package dnd.server.storage;

import dnd.server.config.MinIOConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.errors.MinioException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * MinIO client wrapper để upload files lên MinIO
 */
public class MinIOClient {
    private static MinIOClient instance;
    private final MinioClient minioClient;
    private final String bucketName;
    private final String endpoint;
    
    private MinIOClient() {
        MinIOConfig config = MinIOConfig.getInstance();
        this.endpoint = config.getEndpoint();
        this.bucketName = config.getBucketName();
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
    }
    
    public static MinIOClient getInstance() {
        if (instance == null) {
            instance = new MinIOClient();
        }
        return instance;
    }
    
    /**
     * Upload file lên MinIO và trả về URL
     * @param objectName Tên object trong bucket (ví dụ: "avatars/staff001.jpg")
     * @param data Binary data của file
     * @param contentType Content type (ví dụ: "image/jpeg")
     * @return URL để truy cập file
     * @throws Exception Nếu upload thất bại
     */
    public String uploadFile(String objectName, byte[] data, String contentType) throws Exception {
        try {
            // Đảm bảo bucket tồn tại (tạo nếu chưa có)
            ensureBucketExists();
            
            // Upload file
            InputStream inputStream = new ByteArrayInputStream(data);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, data.length, -1)
                            .contentType(contentType)
                            .build()
            );
            
            // Trả về URL
            // Format: http://endpoint/bucket/object
            String url = String.format("http://%s/%s/%s", endpoint, bucketName, objectName);
            return url;
        } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new Exception("Failed to upload file to MinIO: " + e.getMessage(), e);
        }
    }
    
    private void ensureBucketExists() throws Exception {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build())) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
            }
        } catch (Exception e) {
            // Log nhưng không fail nếu bucket đã tồn tại
            System.err.println("Warning: Could not ensure bucket exists: " + e.getMessage());
        }
    }
}

