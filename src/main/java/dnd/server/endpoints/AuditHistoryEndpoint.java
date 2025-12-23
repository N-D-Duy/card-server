package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/system/logs/audit - Lấy audit history
 * Query params: limit (optional, default 200)
 * Response: [{"id": 1, "sessionId": "...", "timestamp": "...", "result": "...", "staffId": "..."}, ...]
 */
public class AuditHistoryEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public AuditHistoryEndpoint(DbManager dbManager) {
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
                    NULL as session_id,
                    timestamp,
                    result,
                    staff_id
                FROM audit_history
                ORDER BY timestamp DESC
                LIMIT ?
                """;

        List<HashMap<String, Object>> results = dbManager.query(sql, limit);

        // Convert to response format
        List<Map<String, Object>> history = new ArrayList<>();
        for (HashMap<String, Object> row : results) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", row.get("id"));
            item.put("sessionId", asString(row.get("session_id")));
            item.put("timestamp", asString(row.get("timestamp")));
            item.put("result", asString(row.get("result")));
            item.put("staffId", asString(row.get("staff_id")));
            history.add(item);
        }

        return Response.success(history);
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}

