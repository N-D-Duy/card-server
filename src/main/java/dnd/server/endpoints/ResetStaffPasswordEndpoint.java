package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * PUT /api/staff/:id/password - Reset password cho staff
 * Body: {"oldPassword": "...", "newPassword": "..."} hoặc {"newPassword": "..."} (admin reset)
 * Response: {"success": true}
 */
public class ResetStaffPasswordEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public ResetStaffPasswordEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("staffId is required");
        }

        String staffId = parts[parts.length - 2]; // Second to last (before "password")
        JsonObject body = request.getBody();
        if (body == null || !body.has("newPassword")) {
            return Response.badRequest("newPassword is required");
        }

        String newPassword = body.get("newPassword").getAsString();
        String oldPassword = body.has("oldPassword") && !body.get("oldPassword").isJsonNull()
            ? body.get("oldPassword").getAsString() : null;

        // If oldPassword provided, verify it first
        if (oldPassword != null) {
            String verifySql = "SELECT staff_id FROM staff_info WHERE staff_id = ? AND password_hash = SHA2(?, 256)";
            HashMap<String, Object> verifyResult = dbManager.queryOne(verifySql, staffId, oldPassword);
            if (verifyResult == null) {
                return Response.error(401, "Invalid old password");
            }
        }

        // Update password
        String sql = "UPDATE staff_info SET password_hash = SHA2(?, 256) WHERE staff_id = ?";
        int affected = dbManager.update(sql, newPassword, staffId);
        
        if (affected > 0) {
            return Response.success("Password updated successfully");
        } else {
            return Response.internalError("Failed to update password");
        }
    }
}

