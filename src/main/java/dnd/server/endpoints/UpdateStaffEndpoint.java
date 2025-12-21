package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * PUT /api/staff/:id - Cập nhật staff
 * Body: {"shortName": "...", "fullName": "...", "role": 1, "department": "...", "active": true}
 * Response: {"success": true}
 */
public class UpdateStaffEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public UpdateStaffEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("staffId is required");
        }

        String staffId = parts[parts.length - 1];
        JsonObject body = request.getBody();
        if (body == null) {
            return Response.badRequest("Request body is required");
        }

        // Check if staff exists
        HashMap<String, Object> existing = dbManager.queryOne("SELECT staff_id FROM staff_info WHERE staff_id = ?", staffId);
        if (existing == null) {
            return Response.notFound("Staff not found");
        }

        String shortName = body.has("shortName") ? body.get("shortName").getAsString() : null;
        String fullName = body.has("fullName") ? body.get("fullName").getAsString() : null;
        Integer role = body.has("role") ? body.get("role").getAsInt() : null;
        String department = body.has("department") && !body.get("department").isJsonNull()
            ? body.get("department").getAsString() : null;
        Boolean active = body.has("active") ? body.get("active").getAsBoolean() : null;

        // Build update query dynamically
        StringBuilder sql = new StringBuilder("UPDATE staff_info SET ");
        java.util.List<Object> params = new java.util.ArrayList<>();
        boolean hasUpdate = false;

        if (shortName != null) {
            sql.append(hasUpdate ? ", " : "").append("short_name = ?");
            params.add(shortName);
            hasUpdate = true;
        }
        if (fullName != null) {
            sql.append(hasUpdate ? ", " : "").append("full_name = ?");
            params.add(fullName);
            hasUpdate = true;
        }
        if (role != null) {
            sql.append(hasUpdate ? ", " : "").append("role = ?");
            params.add(role);
            hasUpdate = true;
        }
        if (department != null) {
            sql.append(hasUpdate ? ", " : "").append("department = ?");
            params.add(department);
            hasUpdate = true;
        }
        if (active != null) {
            sql.append(hasUpdate ? ", " : "").append("active = ?");
            params.add(active ? 1 : 0);
            hasUpdate = true;
        }

        if (!hasUpdate) {
            return Response.badRequest("At least one field must be provided for update");
        }

        sql.append(" WHERE staff_id = ?");
        params.add(staffId);

        int affected = dbManager.update(sql.toString(), params.toArray());
        
        if (affected > 0) {
            return Response.success("Staff updated successfully");
        } else {
            return Response.internalError("Failed to update staff");
        }
    }
}

