package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * POST /api/prescriptions/:id/cancel-payment - Hủy thanh toán cho đơn thuốc
 * 
 * Response:
 * {
 *   "success": true,
 *   "message": "Payment cancelled successfully",
 *   "data": {
 *     "prescriptionId": 123,
 *     "prescriptionCode": "...",
 *     "status": 3
 *   }
 * }
 */
public class CancelPrescriptionPaymentEndpoint implements EndpointHandler {
    private static final Logger logger = Logger.getLogger(CancelPrescriptionPaymentEndpoint.class.getName());
    private final DbManager dbManager;

    public CancelPrescriptionPaymentEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        
        // Extract prescription ID from path: /api/prescriptions/123/cancel-payment -> 123
        if (parts.length < 5 || !parts[parts.length - 1].equals("cancel-payment")) {
            return Response.badRequest("Invalid path format. Expected: /api/prescriptions/:id/cancel-payment");
        }
        
        String prescriptionIdStr = parts[parts.length - 2];
        Long prescriptionId;
        try {
            prescriptionId = Long.parseLong(prescriptionIdStr);
        } catch (NumberFormatException e) {
            return Response.badRequest("Invalid prescription ID: " + prescriptionIdStr);
        }
        
        // Kiểm tra prescription có tồn tại không
        HashMap<String, Object> prescription = dbManager.queryOne(
            "SELECT id, prescription_code, status FROM prescriptions WHERE id = ?", prescriptionId);
        if (prescription == null) {
            return Response.notFound("Prescription not found: " + prescriptionId);
        }
        
        Integer currentStatus = ((Number) prescription.get("status")).intValue();
        
        // Kiểm tra xem đã có transaction thanh toán chưa
        String checkTransactionSql = "SELECT id FROM transactions WHERE ref = ? LIMIT 1";
        String ref = "medcard " + prescriptionId;
        HashMap<String, Object> transaction = dbManager.queryOne(checkTransactionSql, ref);
        
        if (transaction != null) {
            // Đã có transaction thanh toán, không thể hủy
            logger.warning("Cannot cancel payment for prescription " + prescriptionId + ": payment already processed");
            return Response.error(400, "Cannot cancel payment: payment has already been processed");
        }
        
        // Kiểm tra status hiện tại
        if (currentStatus == 3) {
            // Đã hủy rồi
            return Response.error(400, "Prescription is already cancelled");
        }
        
        if (currentStatus == 2) {
            // Đã hoàn tất
            return Response.error(400, "Cannot cancel payment: prescription is already completed");
        }
        
        // Cập nhật status = 3 (Hủy)
        String updateSql = "UPDATE prescriptions SET status = 3, updated_at = NOW() WHERE id = ?";
        int affected = dbManager.update(updateSql, prescriptionId);
        
        if (affected > 0) {
            // Lấy thông tin prescription sau khi cập nhật
            HashMap<String, Object> updated = dbManager.queryOne(
                "SELECT id, prescription_code, status FROM prescriptions WHERE id = ?", prescriptionId);
            
            HashMap<String, Object> data = new HashMap<>();
            data.put("prescriptionId", prescriptionId);
            data.put("prescriptionCode", updated.get("prescription_code"));
            data.put("status", updated.get("status"));
            
            logger.info("Payment cancelled for prescription: " + prescriptionId);
            
            // Ghi log vào system_logs
            try {
                String logSql = """
                    INSERT INTO system_logs (action, admin_staff_id, description, created_at)
                    VALUES (?, ?, ?, NOW())
                    """;
                String logDescription = String.format("Payment cancelled for prescription: ID=%d, Code=%s", 
                    prescriptionId, updated.get("prescription_code"));
                dbManager.update(logSql, "CANCEL_PAYMENT", null, logDescription);
            } catch (Exception e) {
                logger.warning("Failed to log cancel payment: " + e.getMessage());
            }
            
            return Response.success("Payment cancelled successfully", data);
        } else {
            return Response.internalError("Failed to cancel payment");
        }
    }
}

