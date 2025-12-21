package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/prescriptions/:id/payment-status - Kiểm tra trạng thái thanh toán của prescription
 * 
 * Response:
 * {
 *   "prescriptionId": number,
 *   "status": "pending" | "paid" | "not_found",
 *   "transactionId": number (nếu đã thanh toán),
 *   "amount": number (nếu đã thanh toán),
 *   "paidAt": "datetime" (nếu đã thanh toán)
 * }
 */
public class GetPrescriptionPaymentStatusEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public GetPrescriptionPaymentStatusEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        // Extract prescription ID from path: /api/prescriptions/123/payment-status -> 123
        String[] parts = path.split("/");
        if (parts.length < 4 || !parts[parts.length - 1].equals("payment-status")) {
            return Response.badRequest("Invalid path format");
        }
        
        String prescriptionIdStr = parts[parts.length - 2];
        Long prescriptionId;
        try {
            prescriptionId = Long.parseLong(prescriptionIdStr);
        } catch (NumberFormatException e) {
            return Response.badRequest("Invalid prescription ID");
        }
        
        // Verify prescription exists
        HashMap<String, Object> prescription = dbManager.queryOne(
            "SELECT id FROM prescriptions WHERE id = ?", prescriptionId);
        if (prescription == null) {
            return Response.notFound("Prescription not found");
        }
        
        // Check if there's a transaction with ref matching "medcard {prescriptionId}"
        String refPattern = "medcard " + prescriptionId;
        HashMap<String, Object> transaction = dbManager.queryOne(
            "SELECT id, amount, created_at FROM transactions WHERE ref = ? ORDER BY created_at DESC LIMIT 1",
            refPattern
        );
        
        Map<String, Object> data = new HashMap<>();
        data.put("prescriptionId", prescriptionId);
        
        if (transaction != null) {
            data.put("status", "paid");
            data.put("transactionId", transaction.get("id"));
            data.put("amount", transaction.get("amount"));
            data.put("paidAt", transaction.get("created_at"));
        } else {
            data.put("status", "pending");
        }
        
        return Response.success(data);
    }
}

