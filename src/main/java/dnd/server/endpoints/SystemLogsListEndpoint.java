package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/system/logs/system - Lấy danh sách system logs
 * Query params: limit (optional, default 200)
 * Response: [{"id": 1, "createdAt": "...", "adminStaffId": "...", "action": "...", "description": "..."}, ...]
 */
public class SystemLogsListEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public SystemLogsListEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        Map<String, String> query = request.getQueryParams();
        String limitParam = query != null ? query.get("limit") : null;
        int limit = limitParam != null ? Integer.parseInt(limitParam) : 200;

        String sql = """
                SELECT 
                    id,
                    created_at,
                    admin_staff_id,
                    action,
                    description
                FROM system_logs
                ORDER BY created_at DESC
                LIMIT ?
                """;

        List<HashMap<String, Object>> results = dbManager.query(sql, limit);

        // Convert to response format
        List<Map<String, Object>> logs = new ArrayList<>();
        for (HashMap<String, Object> row : results) {
            Map<String, Object> log = new HashMap<>();
            log.put("id", row.get("id"));
            log.put("createdAt", asString(row.get("created_at")));
            log.put("adminStaffId", asString(row.get("admin_staff_id")));
            log.put("action", asString(row.get("action")));
            log.put("description", asString(row.get("description")));
            logs.add(log);
        }

        return Response.success(logs);
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}

