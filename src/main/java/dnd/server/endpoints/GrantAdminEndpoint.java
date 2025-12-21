package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * POST /api/staff/:id/admin - Gán quyền admin cho staff
 * Body: {"username": "...", "password": "..."}
 * Response: {"success": true}
 */
public class GrantAdminEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public GrantAdminEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("staffId is required");
        }

        String staffId = parts[parts.length - 2]; // Second to last (before "admin")
        JsonObject body = request.getBody();
        if (body == null || !body.has("username") || !body.has("password")) {
            return Response.badRequest("username and password are required");
        }

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();

        // Check if staff exists and is admin role
        HashMap<String, Object> staff = dbManager.queryOne("SELECT role FROM staff_info WHERE staff_id = ?", staffId);
        if (staff == null) {
            return Response.notFound("Staff not found");
        }
        Object roleObj = staff.get("role");
        int role = roleObj instanceof Number ? ((Number) roleObj).intValue() : -1;
        if (role != 0) {
            return Response.error(400, "Only admin role (0) can be granted admin account");
        }

        // Check if username already exists
        HashMap<String, Object> existing = dbManager.queryOne(
            "SELECT id FROM admin_accounts WHERE username = ?", username);
        if (existing != null) {
            return Response.error(409, "Username already exists");
        }

        // Create admin account
        String sql = """
                INSERT INTO admin_accounts (staff_id, username, password_hash, active)
                VALUES (?, ?, SHA2(?, 256), 1)
                """;
        int affected = dbManager.update(sql, staffId, username, password);
        
        if (affected > 0) {
            return Response.success("Admin account created successfully");
        } else {
            return Response.internalError("Failed to create admin account");
        }
    }
}

