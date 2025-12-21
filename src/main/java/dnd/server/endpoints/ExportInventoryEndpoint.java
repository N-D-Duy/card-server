package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;

import com.google.gson.JsonObject;

/**
 * POST /api/inventory/export - Xuất kho
 */
public class ExportInventoryEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public ExportInventoryEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null) {
            return Response.badRequest("Request body is required");
        }

        String medicineCode = body.has("medicine_code") ? body.get("medicine_code").getAsString() : null;
        Long batchId = body.has("batch_id") && !body.get("batch_id").isJsonNull() ? body.get("batch_id").getAsLong() : null;
        int quantity = body.has("quantity") ? body.get("quantity").getAsInt() : 0;
        String staffId = body.has("staff_id") ? body.get("staff_id").getAsString() : null;
        Long prescriptionId = body.has("prescription_id") && !body.get("prescription_id").isJsonNull() ? body.get("prescription_id").getAsLong() : null;
        String note = body.has("note") ? body.get("note").getAsString() : null;

        if (medicineCode == null || quantity <= 0 || staffId == null) {
            return Response.badRequest("medicine_code, quantity, and staff_id are required");
        }

        // Kiểm tra tồn kho
        String checkSql = "SELECT quantity FROM medicines WHERE code = ?";
        HashMap<String, Object> medicine = dbManager.queryOne(checkSql, medicineCode);
        if (medicine == null) {
            return Response.badRequest("Medicine not found: " + medicineCode);
        }
        
        int currentQuantity = ((Number) medicine.get("quantity")).intValue();
        if (currentQuantity < quantity) {
            return Response.badRequest("Insufficient inventory. Available: " + currentQuantity + ", Requested: " + quantity);
        }

        // Cập nhật quantity trong medicines
        String updateMedicineSql = "UPDATE medicines SET quantity = quantity - ? WHERE code = ?";
        dbManager.update(updateMedicineSql, quantity, medicineCode);

        // Cập nhật batch nếu có
        if (batchId != null) {
            String updateBatchSql = "UPDATE medicine_batches SET quantity = quantity - ? WHERE id = ?";
            dbManager.update(updateBatchSql, quantity, batchId);
        }

        // Ghi log (type = 1 là xuất kho)
        String logSql = "INSERT INTO inventory_logs (timestamp, type, medicine_code, batch_id, quantity_change, staff_id, prescription_id, note) VALUES (NOW(), 1, ?, ?, ?, ?, ?, ?)";
        dbManager.update(logSql, medicineCode, batchId, -quantity, staffId, prescriptionId, note);

        return Response.success("Inventory exported successfully", null);
    }
}

