package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * PUT /api/staff/:id/active - Kích hoạt/vô hiệu hóa staff
 * Body: {"active": true/false}
 * Response: {"success": true}
 */
public class UpdateStaffActiveEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public UpdateStaffActiveEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("staffId is required");
        }

        String staffId = parts[parts.length - 2]; // Second to last (before "active")
        JsonObject body = request.getBody();
        if (body == null || !body.has("active")) {
            return Response.badRequest("active field is required");
        }

        boolean active = body.get("active").getAsBoolean();

        // Check if staff exists
        HashMap<String, Object> existing = dbManager.queryOne("SELECT staff_id FROM staff_info WHERE staff_id = ?", staffId);
        if (existing == null) {
            return Response.notFound("Staff not found");
        }

        String sql = "UPDATE staff_info SET active = ? WHERE staff_id = ?";
        int affected = dbManager.update(sql, active ? 1 : 0, staffId);
        
        if (affected > 0) {
            return Response.success("Staff status updated successfully");
        } else {
            return Response.internalError("Failed to update staff status");
        }
    }
}

