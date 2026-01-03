package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import dnd.server.storage.MinIOClient;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * POST /api/staff/:staffId/avatar - Upload ảnh đại diện cho nhân viên (admin only)
 * Body: {"avatar": "base64_encoded_image"}
 * Response: {"success": true, "message": "Avatar uploaded successfully", "url": "http://..."}
 */
public class UploadStaffAvatarEndpoint implements EndpointHandler {
    private final DbManager dbManager;
    private final MinIOClient minioClient;

    public UploadStaffAvatarEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
        this.minioClient = MinIOClient.getInstance();
        ensureAvatarColumnExists();
    }

    private void ensureAvatarColumnExists() {
        try {
            String checkSql = "SELECT COUNT(*) as cnt FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_info' AND COLUMN_NAME = 'avatar_url'";
            HashMap<String, Object> result = dbManager.queryOne(checkSql);
            if (result != null && ((Number) result.get("cnt")).intValue() == 0) {
                // Thêm cột avatar_url (VARCHAR) thay vì BLOB
                String alterSql = "ALTER TABLE staff_info ADD COLUMN avatar_url VARCHAR(500) NULL COMMENT 'URL ảnh đại diện trên MinIO'";
                dbManager.update(alterSql);
                // Xóa cột avatar cũ nếu có (BLOB)
                try {
                    String dropOldSql = "ALTER TABLE staff_info DROP COLUMN avatar";
                    dbManager.update(dropOldSql);
                } catch (Exception e) {
                    // Ignore nếu cột không tồn tại
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not ensure avatar_url column exists: " + e.getMessage());
        }
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        // Extract staffId from path: /api/staff/STAFF001/avatar -> STAFF001
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("staffId is required");
        }

        String staffId = parts[parts.length - 2]; // Second to last part (before "avatar")
        if (staffId == null || staffId.trim().isEmpty()) {
            return Response.badRequest("staffId cannot be empty");
        }

        // Kiểm tra staff tồn tại
        HashMap<String, Object> existing = dbManager.queryOne(
                "SELECT staff_id FROM staff_info WHERE staff_id = ?", staffId);
        if (existing == null) {
            return Response.notFound("Staff not found");
        }

        // Lấy avatar từ request body (base64 string)
        byte[] avatarData = null;
        
        if (request.getBody() != null && request.getBody().has("avatar")) {
            String avatarBase64 = request.getBody().get("avatar").getAsString();
            if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                try {
                    // Decode base64
                    avatarData = java.util.Base64.getDecoder().decode(avatarBase64);
                } catch (IllegalArgumentException e) {
                    return Response.badRequest("Invalid base64 avatar data");
                }
            }
        }

        if (avatarData == null || avatarData.length == 0) {
            return Response.badRequest("Avatar data is required");
        }

        // Giới hạn kích thước avatar (ví dụ: 50KB)
        if (avatarData.length > 50 * 1024) {
            return Response.badRequest("Avatar size exceeds 50KB limit");
        }

        // Xác định content type từ data (đơn giản: giả sử là JPEG)
        String contentType = "image/jpeg";
        String extension = "jpg";
        
        // Detect image type từ magic bytes
        if (avatarData.length >= 4) {
            if (avatarData[0] == (byte)0x89 && avatarData[1] == (byte)0x50 && 
                avatarData[2] == (byte)0x4E && avatarData[3] == (byte)0x47) {
                contentType = "image/png";
                extension = "png";
            } else if (avatarData[0] == (byte)0xFF && avatarData[1] == (byte)0xD8) {
                contentType = "image/jpeg";
                extension = "jpg";
            } else if (avatarData[0] == (byte)0x47 && avatarData[1] == (byte)0x49 && 
                       avatarData[2] == (byte)0x46) {
                contentType = "image/gif";
                extension = "gif";
            }
        }

        // Upload lên MinIO
        String objectName = String.format("avatars/%s.%s", staffId, extension);
        String avatarUrl;
        try {
            avatarUrl = minioClient.uploadFile(objectName, avatarData, contentType);
        } catch (Exception e) {
            System.err.println("Failed to upload to MinIO: " + e.getMessage());
            return Response.badRequest("Failed to upload avatar to storage: " + e.getMessage());
        }

        // Cập nhật avatar_url vào database
        String sql = "UPDATE staff_info SET avatar_url = ? WHERE staff_id = ?";
        dbManager.update(sql, avatarUrl, staffId);

        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("message", "Avatar uploaded successfully");
        data.put("url", avatarUrl);
        data.put("size", avatarData.length);
        
        return Response.success(data);
    }
}

