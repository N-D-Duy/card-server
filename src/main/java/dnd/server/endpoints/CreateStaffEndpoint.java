package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * POST /api/staff - Tạo staff mới
 * Body: {"staffId": "...", "shortName": "...", "fullName": "...", "role": 1, "department": "...", "active": true}
 * Response: {"success": true}
 */
public class CreateStaffEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public CreateStaffEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null || !body.has("staffId") || !body.has("shortName") || !body.has("fullName")) {
            return Response.badRequest("staffId, shortName, and fullName are required");
        }

        String staffId = body.get("staffId").getAsString();
        String shortName = body.get("shortName").getAsString();
        String fullName = body.get("fullName").getAsString();
        int role = body.has("role") ? body.get("role").getAsInt() : 2; // Default: warehouse
        String department = body.has("department") && !body.get("department").isJsonNull()
            ? body.get("department").getAsString() : null;
        boolean active = body.has("active") ? body.get("active").getAsBoolean() : true;

        // Check if staffId already exists
        HashMap<String, Object> existing = dbManager.queryOne("SELECT staff_id FROM staff_info WHERE staff_id = ?", staffId);
        if (existing != null) {
            return Response.error(409, "Staff ID already exists");
        }

        String sql = """
                INSERT INTO staff_info (staff_id, short_name, full_name, role, department, active)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        
        int affected = dbManager.update(sql, staffId, shortName, fullName, role, department, active ? 1 : 0);
        
        if (affected > 0) {
            return Response.success("Staff created successfully");
        } else {
            return Response.internalError("Failed to create staff");
        }
    }
}

