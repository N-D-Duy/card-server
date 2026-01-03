package dnd.server.storage;

import dnd.server.config.MinIOConfig;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import io.minio.errors.MinioException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * MinIO client wrapper để upload files lên MinIO
 */
public class MinIOClient {
    private static MinIOClient instance;
    private final MinioClient minioClient; // Client để upload (dùng endpoint nội bộ)
    private final MinioClient publicMinioClient; // Client để tạo presigned URL (dùng public endpoint)
    private final String bucketName;
    private final String publicEndpoint;
    
    private MinIOClient() {
        MinIOConfig config = MinIOConfig.getInstance();
        this.publicEndpoint = config.getPublicEndpoint(); // Dùng public endpoint cho URL trong database
        this.bucketName = config.getBucketName();
        
        // Dùng endpoint gốc (có thể là hostname) để connect đến MinIO cho upload
        String connectEndpoint = config.getEndpoint();
        this.minioClient = MinioClient.builder()
                .endpoint(connectEndpoint)
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();
        
        // Tạo client riêng với public endpoint để tạo presigned URL với signature đúng
        // Lưu ý: Client này chỉ dùng để tạo presigned URL, không dùng để upload
        String publicEndpointForClient = publicEndpoint;
        if (!publicEndpointForClient.startsWith("http://") && !publicEndpointForClient.startsWith("https://")) {
            publicEndpointForClient = "http://" + publicEndpointForClient;
        }
        this.publicMinioClient = MinioClient.builder()
                .endpoint(publicEndpointForClient)
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
            
            // Tạo presigned URL với thời hạn tối đa (7 ngày) - MinIO chỉ cho phép tối đa 7 ngày
            // Dùng publicMinioClient để tạo presigned URL với public endpoint
            // Signature sẽ được tính toán với public endpoint nên sẽ hợp lệ
            String presignedUrl = publicMinioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry(7, TimeUnit.DAYS) // Tối đa 7 ngày theo giới hạn của MinIO
                    .build()
            );
            
            return presignedUrl;
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

