package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * PUT /api/cards/staff/:staffId/revoke - Vô hiệu hóa tất cả thẻ của staff
 * Response: {"affected": 1}
 */
public class RevokeCardByStaffEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public RevokeCardByStaffEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        // Extract staffId from path: /api/cards/staff/STAFF001/revoke -> STAFF001
        String[] parts = path.split("/");
        if (parts.length < 5) {
            return Response.badRequest("staffId is required");
        }

        String staffId = parts[parts.length - 2]; // Second to last part
        if (staffId == null || staffId.trim().isEmpty()) {
            return Response.badRequest("staffId cannot be empty");
        }

        String sql = "UPDATE card_keys SET status = 0 WHERE staff_id = ? AND status = 1";
        int affected = dbManager.update(sql, staffId);

        Map<String, Object> data = new HashMap<>();
        data.put("affected", affected);
        return Response.success(data);
    }
}

