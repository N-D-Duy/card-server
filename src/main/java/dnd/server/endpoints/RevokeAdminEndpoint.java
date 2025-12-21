package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;

/**
 * DELETE /api/staff/:id/admin - Xóa quyền admin của staff
 * Response: {"success": true}
 */
public class RevokeAdminEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public RevokeAdminEndpoint(DbManager dbManager) {
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

        // Check if admin account exists
        HashMap<String, Object> existing = dbManager.queryOne(
            "SELECT id FROM admin_accounts WHERE staff_id = ?", staffId);
        if (existing == null) {
            return Response.notFound("Admin account not found");
        }

        // Delete admin account
        String sql = "DELETE FROM admin_accounts WHERE staff_id = ?";
        int affected = dbManager.update(sql, staffId);
        
        if (affected > 0) {
            return Response.success("Admin account deleted successfully");
        } else {
            return Response.internalError("Failed to delete admin account");
        }
    }
}

