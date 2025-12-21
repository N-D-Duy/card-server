package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/staff/:staffId/profile - Lấy thông tin staff để build profile payload
 * Response: {"shortName": "...", "department": "..."}
 */
public class GetStaffProfileEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public GetStaffProfileEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
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

        String sql = "SELECT short_name, department FROM staff_info WHERE staff_id = ?";
        HashMap<String, Object> row = dbManager.queryOne(sql, staffId);
        
        if (row == null) {
            return Response.notFound("Staff not found");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("shortName", asString(row.get("short_name")));
        data.put("department", asString(row.get("department")));
        return Response.success(data);
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}

