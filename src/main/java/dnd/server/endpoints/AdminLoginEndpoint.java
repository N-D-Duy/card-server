package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * POST /api/admin/login - Đăng nhập admin
 * Body: {"username": "...", "password": "..."}
 * Response: {"username": "...", "staffId": "...", "shortName": "...", "fullName": "...", "role": 0, "department": "..."}
 */
public class AdminLoginEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public AdminLoginEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null || !body.has("username") || !body.has("password")) {
            return Response.badRequest("username and password are required");
        }

        String username = body.get("username").getAsString();
        String password = body.get("password").getAsString();

        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return Response.badRequest("username and password cannot be empty");
        }

        String sql = """
                SELECT 
                    a.username,
                    a.staff_id,
                    s.short_name,
                    s.full_name,
                    s.role,
                    s.department
                FROM admin_accounts a
                JOIN staff_info s ON s.staff_id = a.staff_id
                WHERE a.username = ?
                  AND a.password_hash = SHA2(?, 256)
                  AND a.active = 1
                  AND s.active = 1
                  AND s.role = 0
                LIMIT 1
                """;

        HashMap<String, Object> row = dbManager.queryOne(sql, username, password);
        if (row == null) {
            return Response.error(401, "Invalid username or password");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", asString(row.get("username")));
        data.put("staffId", asString(row.get("staff_id")));
        data.put("shortName", asString(row.get("short_name")));
        data.put("fullName", asString(row.get("full_name")));
        data.put("role", row.get("role") != null ? ((Number) row.get("role")).intValue() : 0);
        data.put("department", asString(row.get("department")));

        return Response.success(data);
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}

