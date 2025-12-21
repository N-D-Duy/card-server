package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/medicines - Lấy danh sách thuốc
 * Response bao gồm nearestExpiry và nearestBatch từ medicine_batches
 */
public class MedicinesEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public MedicinesEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String sql = "SELECT * FROM medicines ORDER BY code";
        List<HashMap<String, Object>> medicines = dbManager.query(sql);
        
        // Thêm nearestExpiry và nearestBatch cho mỗi medicine
        List<Map<String, Object>> result = new ArrayList<>();
        for (HashMap<String, Object> medicine : medicines) {
            Map<String, Object> item = new HashMap<>(medicine);
            String code = (String) medicine.get("code");
            
            // Query batch gần hết hạn nhất (có quantity > 0)
            String batchSql = """
                SELECT batch_number, expiry_date 
                FROM medicine_batches 
                WHERE medicine_code = ? AND quantity > 0 
                ORDER BY expiry_date ASC 
                LIMIT 1
                """;
            HashMap<String, Object> nearestBatch = dbManager.queryOne(batchSql, code);
            
            if (nearestBatch != null) {
                item.put("nearestBatch", nearestBatch.get("batch_number"));
                if (nearestBatch.get("expiry_date") != null) {
                    item.put("nearestExpiry", nearestBatch.get("expiry_date"));
                } else {
                    item.put("nearestExpiry", null);
                }
            } else {
                item.put("nearestBatch", null);
                item.put("nearestExpiry", null);
            }
            
            result.add(item);
        }
        
        return Response.success(result);
    }
}

