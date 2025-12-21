package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/system/inventory/summary - Thống kê tổng quan kho
 * Response: {"totalMedicines": 100, "totalQuantity": 5000, "lowStockCount": 5, ...}
 */
public class InventorySummaryEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public InventorySummaryEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        Map<String, Object> summary = new HashMap<>();

        // Tổng số thuốc
        HashMap<String, Object> totalMedicines = dbManager.queryOne("SELECT COUNT(*) as count FROM medicines");
        summary.put("totalMedicines", totalMedicines != null ? totalMedicines.get("count") : 0);

        // Tổng số lượng thuốc
        HashMap<String, Object> totalQuantity = dbManager.queryOne("SELECT COALESCE(SUM(quantity), 0) as total FROM medicines");
        summary.put("totalQuantity", totalQuantity != null ? totalQuantity.get("total") : 0);

        // Số thuốc sắp hết
        HashMap<String, Object> lowStock = dbManager.queryOne(
            "SELECT COUNT(*) as count FROM medicines WHERE quantity < COALESCE(min_quantity, 10)");
        summary.put("lowStockCount", lowStock != null ? lowStock.get("count") : 0);

        // Số đơn thuốc
        HashMap<String, Object> totalPrescriptions = dbManager.queryOne("SELECT COUNT(*) as count FROM prescriptions");
        summary.put("totalPrescriptions", totalPrescriptions != null ? totalPrescriptions.get("count") : 0);

        return Response.success(summary);
    }
}

