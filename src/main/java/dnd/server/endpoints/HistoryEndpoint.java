package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.List;

/**
 * GET /api/history - Lấy lịch sử (có thể là audit history hoặc inventory logs)
 */
public class HistoryEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public HistoryEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String type = request.getQueryParam("type", "inventory"); // inventory hoặc audit
        
        if ("audit".equals(type)) {
            String sql = "SELECT * FROM audit_history ORDER BY timestamp DESC LIMIT 100";
            List<HashMap<String, Object>> history = dbManager.query(sql);
            return Response.success(history);
        } else {
            // Mặc định là inventory logs
            String sql = "SELECT * FROM inventory_logs ORDER BY timestamp DESC LIMIT 100";
            List<HashMap<String, Object>> history = dbManager.query(sql);
            return Response.success(history);
        }
    }
}

