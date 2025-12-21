package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * POST /api/inventory/import - Nhập kho
 */
public class ImportInventoryEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public ImportInventoryEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null) {
            return Response.badRequest("Request body is required");
        }

        String medicineCode = body.has("medicine_code") ? body.get("medicine_code").getAsString() : null;
        String batchNumber = body.has("batch_number") ? body.get("batch_number").getAsString() : null;
        int quantity = body.has("quantity") ? body.get("quantity").getAsInt() : 0;
        String staffId = body.has("staff_id") ? body.get("staff_id").getAsString() : null;
        String note = body.has("note") ? body.get("note").getAsString() : null;

        if (medicineCode == null || quantity <= 0 || staffId == null) {
            return Response.badRequest("medicine_code, quantity, and staff_id are required");
        }

        // Tìm hoặc tạo batch
        String batchSql = "SELECT * FROM medicine_batches WHERE medicine_code = ? AND batch_number = ?";
        HashMap<String, Object> batch = dbManager.queryOne(batchSql, medicineCode, batchNumber);
        
        Long batchId;
        if (batch == null) {
            // Tạo batch mới
            String createBatchSql = "INSERT INTO medicine_batches (medicine_code, batch_number, quantity) VALUES (?, ?, ?)";
            batchId = dbManager.insertAndGetId(createBatchSql, medicineCode, batchNumber, quantity);
        } else {
            batchId = ((Number) batch.get("id")).longValue();
            // Cập nhật quantity
            String updateBatchSql = "UPDATE medicine_batches SET quantity = quantity + ? WHERE id = ?";
            dbManager.update(updateBatchSql, quantity, batchId);
        }

        // Cập nhật tổng quantity trong medicines
        String updateMedicineSql = "UPDATE medicines SET quantity = quantity + ? WHERE code = ?";
        dbManager.update(updateMedicineSql, quantity, medicineCode);

        // Ghi log
        String logSql = "INSERT INTO inventory_logs (timestamp, type, medicine_code, batch_id, quantity_change, staff_id, note) VALUES (NOW(), 0, ?, ?, ?, ?, ?)";
        dbManager.update(logSql, medicineCode, batchId, quantity, staffId, note);

        return Response.success("Inventory imported successfully", null);
    }
}

