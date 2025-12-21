package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * POST /api/system/logs - Ghi log hệ thống
 * Body: {"action": "...", "adminStaffId": "...", "description": "..."}
 * Response: {"success": true}
 */
public class SystemLogEndpoint implements EndpointHandler {
    private final DbManager dbManager;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SystemLogEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null || !body.has("action")) {
            return Response.badRequest("action is required");
        }

        String action = body.get("action").getAsString();
        String adminStaffId = body.has("adminStaffId") && !body.get("adminStaffId").isJsonNull() 
            ? body.get("adminStaffId").getAsString() : null;
        String description = body.has("description") && !body.get("description").isJsonNull()
            ? body.get("description").getAsString() : null;

        // Tạo bảng nếu chưa tồn tại
        ensureTableExists();

        String timestamp = LocalDateTime.now().format(formatter);
        String sql = """
                INSERT INTO system_logs (action, admin_staff_id, description, created_at)
                VALUES (?, ?, ?, ?)
                """;
        
        int affected = dbManager.update(sql, action, adminStaffId, description, timestamp);
        
        if (affected > 0) {
            return Response.success("Log recorded successfully");
        } else {
            return Response.internalError("Failed to record log");
        }
    }

    private void ensureTableExists() {
        try {
            String createTableSql = """
                    CREATE TABLE IF NOT EXISTS system_logs (
                        id INTEGER PRIMARY KEY AUTO_INCREMENT,
                        action VARCHAR(50) NOT NULL,
                        admin_staff_id VARCHAR(50),
                        description TEXT,
                        created_at VARCHAR(50) NOT NULL,
                        INDEX idx_action (action),
                        INDEX idx_admin (admin_staff_id),
                        INDEX idx_created (created_at)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """;
            dbManager.update(createTableSql);
        } catch (Exception e) {
            // Table might already exist, ignore
        }
    }
}

