package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/dashboard/stats - Lấy thống kê tổng quan cho dashboard
 */
public class DashboardStatsEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public DashboardStatsEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        Map<String, Object> stats = new HashMap<>();
        
        // Tổng số thuốc
        HashMap<String, Object> totalMedicines = dbManager.queryOne("SELECT COUNT(*) as count FROM medicines");
        stats.put("totalMedicines", totalMedicines != null ? totalMedicines.get("count") : 0);
        
        // Tổng số lượng thuốc
        HashMap<String, Object> totalQuantity = dbManager.queryOne("SELECT COALESCE(SUM(quantity), 0) as total FROM medicines");
        stats.put("totalQuantity", totalQuantity != null ? totalQuantity.get("total") : 0);
        
        // Số thuốc sắp hết
        HashMap<String, Object> lowStock = dbManager.queryOne(
            "SELECT COUNT(*) as count FROM medicines WHERE quantity < COALESCE(min_quantity, 10)"
        );
        stats.put("lowStockCount", lowStock != null ? lowStock.get("count") : 0);
        
        // Số đơn thuốc
        HashMap<String, Object> totalPrescriptions = dbManager.queryOne("SELECT COUNT(*) as count FROM prescriptions");
        stats.put("totalPrescriptions", totalPrescriptions != null ? totalPrescriptions.get("count") : 0);
        
        // Số đơn đang xử lý
        HashMap<String, Object> pendingPrescriptions = dbManager.queryOne(
            "SELECT COUNT(*) as count FROM prescriptions WHERE status = 0 OR status = 1"
        );
        stats.put("pendingPrescriptions", pendingPrescriptions != null ? pendingPrescriptions.get("count") : 0);
        
        // Staff statistics
        HashMap<String, Object> totalStaff = dbManager.queryOne("SELECT COUNT(*) as count FROM staff_info");
        stats.put("totalStaff", totalStaff != null ? totalStaff.get("count") : 0);
        
        HashMap<String, Object> totalAdmin = dbManager.queryOne("SELECT COUNT(*) as count FROM staff_info WHERE role = 0 AND active = 1");
        stats.put("totalAdmin", totalAdmin != null ? totalAdmin.get("count") : 0);
        
        HashMap<String, Object> totalPharmacist = dbManager.queryOne("SELECT COUNT(*) as count FROM staff_info WHERE role = 1 AND active = 1");
        stats.put("totalPharmacist", totalPharmacist != null ? totalPharmacist.get("count") : 0);
        
        HashMap<String, Object> totalWarehouse = dbManager.queryOne("SELECT COUNT(*) as count FROM staff_info WHERE role = 2 AND active = 1");
        stats.put("totalWarehouse", totalWarehouse != null ? totalWarehouse.get("count") : 0);
        
        HashMap<String, Object> activeStaff = dbManager.queryOne("SELECT COUNT(*) as count FROM staff_info WHERE active = 1");
        stats.put("activeStaff", activeStaff != null ? activeStaff.get("count") : 0);
        
        return Response.success(stats);
    }
}

