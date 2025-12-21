package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/inventory/logs - Lấy lịch sử xuất nhập kho
 */
public class InventoryLogsEndpoint implements EndpointHandler {
    private final DbManager dbManager;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public InventoryLogsEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String limit = request.getQueryParam("limit", "100");
        String offset = request.getQueryParam("offset", "0");
        
        String sql = "SELECT * FROM inventory_logs ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        List<HashMap<String, Object>> logs = dbManager.query(sql, Integer.parseInt(limit), Integer.parseInt(offset));
        
        // Convert timestamp to String format
        List<Map<String, Object>> result = new ArrayList<>();
        for (HashMap<String, Object> log : logs) {
            Map<String, Object> item = new HashMap<>(log);
            // Convert timestamp to String
            Object timestamp = log.get("timestamp");
            if (timestamp != null) {
                if (timestamp instanceof Timestamp ts) {
                    item.put("timestamp", ts.toLocalDateTime().format(DATETIME_FORMATTER));
                } else if (timestamp instanceof java.util.Date date) {
                    item.put("timestamp", new Timestamp(date.getTime()).toLocalDateTime().format(DATETIME_FORMATTER));
                } else {
                    item.put("timestamp", timestamp.toString());
                }
            }
            result.add(item);
        }
        
        return Response.success(result);
    }
}

