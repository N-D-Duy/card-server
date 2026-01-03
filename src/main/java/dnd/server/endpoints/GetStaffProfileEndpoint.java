package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/staff/:staffId/profile - Lấy thông tin staff để build profile payload
 * Response: {"shortName": "...", "department": "...", "avatar": <byte[]>}
 */
public class GetStaffProfileEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public GetStaffProfileEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
        // Đảm bảo cột avatar tồn tại
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
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not ensure avatar_url column exists: " + e.getMessage());
        }
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        // Extract staffId from path: /api/staff/STAFF001/profile -> STAFF001
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("staffId is required");
        }

        String staffId = parts[parts.length - 2]; // Second to last part (before "profile")
        if (staffId == null || staffId.trim().isEmpty()) {
            return Response.badRequest("staffId cannot be empty");
        }

        String sql = "SELECT short_name, department, avatar_url FROM staff_info WHERE staff_id = ?";
        HashMap<String, Object> row = dbManager.queryOne(sql, staffId);
        
        if (row == null) {
            return Response.notFound("Staff not found");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("shortName", asString(row.get("short_name")));
        data.put("department", asString(row.get("department")));
        
        // Avatar: trả về URL nếu có
        String avatarUrl = asString(row.get("avatar_url"));
        data.put("avatarUrl", avatarUrl);
        
        return Response.success(data);
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}

