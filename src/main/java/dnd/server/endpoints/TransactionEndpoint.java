package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import dnd.server.crypto.HmacUtils;
import dnd.server.config.BankConfig;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * POST /api/transactions - Nhận request từ bank server và update audit/log
 * 
 * Request body:
 * {
 *   "bankId": "string",
 *   "amount": number,
 *   "ref": "string",
 *   "timestamp": number (ms),
 *   "content": "string" (optional),
 *   "playerId": "string" (optional),
 *   "idempotencyKey": "string"
 * }
 * 
 * Headers:
 * - X-Signature: HMAC-SHA256-Base64 signature của request body
 * 
 * Response:
 * {
 *   "success": true,
 *   "data": {
 *     "status": "processed" | "duplicate",
 *     "transactionId": number
 *   },
 *   "message": "OK" | "Duplicate"
 * }
 */
public class TransactionEndpoint implements EndpointHandler {
    private static final Logger logger = Logger.getLogger(TransactionEndpoint.class.getName());
    private final DbManager dbManager;
    private final BankConfig bankConfig;
    private final String hmacSecret;

    public TransactionEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
        this.bankConfig = BankConfig.getInstance();
        this.hmacSecret = bankConfig.getHmacSecret();
    }

    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null) {
            return Response.badRequest("Request body is required");
        }

        String paymentMethod = body.has("paymentMethod") ? body.get("paymentMethod").getAsString() : "qr";
        boolean isCash = "cash".equalsIgnoreCase(paymentMethod);

        // Với thanh toán tiền mặt (do client nội bộ gọi), bỏ qua HMAC
        if (!isCash) {
            // Verify HMAC signature cho giao dịch QR từ bank server
            // Headers được lưu dưới dạng lowercase trong HttpRequest
            String signature = request.headers != null ? request.headers.get("x-signature") : null;
            if (signature == null || signature.trim().isEmpty()) {
                return Response.error(401, "X-Signature header is required");
            }

            if (hmacSecret == null || hmacSecret.isEmpty()) {
                logger.severe("HMAC_SECRET is not configured");
                return Response.error(500, "Server HMAC secret missing");
            }

            // Convert body to JSON string (same format as Node.js JSON.stringify)
            Gson gson = new Gson();
            String bodyJson = gson.toJson(body);

            // Verify signature
            String expectedSignature = HmacUtils.hmacSha256Base64(hmacSecret, bodyJson);
            if (!HmacUtils.timingSafeEqual(signature.trim(), expectedSignature)) {
                logger.warning("Invalid HMAC signature");
                return Response.error(401, "Invalid signature");
            }
        }

        // Parse request body
        String bankId = body.has("bankId") ? body.get("bankId").getAsString() : null;
        if (bankId == null || bankId.isEmpty()) {
            bankId = isCash ? "CASH" : null;
        }
        if (bankId == null || bankId.isEmpty()) {
            return Response.badRequest("bankId is required");
        }

        if (!body.has("amount") || !body.get("amount").isJsonPrimitive()) {
            return Response.badRequest("amount must be a number");
        }
        double amountDouble = body.get("amount").getAsDouble();
        if (!Double.isFinite(amountDouble) || amountDouble <= 0) {
            return Response.badRequest("amount must be positive number");
        }
        long amount = (long) amountDouble;

        String ref = body.has("ref") ? body.get("ref").getAsString() : null;
        if (ref == null || ref.isEmpty()) {
            return Response.badRequest("ref is required");
        }

        long timestamp;
        if (!body.has("timestamp") || !body.get("timestamp").isJsonPrimitive()) {
            timestamp = System.currentTimeMillis();
        } else {
            timestamp = body.get("timestamp").getAsLong();
        }
        if (timestamp <= 0) {
            return Response.badRequest("timestamp invalid");
        }

        String idempotencyKey = body.has("idempotencyKey") ? body.get("idempotencyKey").getAsString() : null;
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            // Cho tiền mặt có thể không truyền, tự tạo để tránh duplicate
            if (isCash) {
                idempotencyKey = "cash-" + ref + "-" + timestamp;
            } else {
                return Response.badRequest("idempotencyKey is required");
            }
        }

        String content = body.has("content") && !body.get("content").isJsonNull() 
            ? body.get("content").getAsString() : null;

        // Ensure transactions table exists
        ensureTransactionsTableExists();

        // Check idempotency
        String checkSql = "SELECT id FROM transactions WHERE idempotency_key = ?";
        HashMap<String, Object> existing = dbManager.queryOne(checkSql, idempotencyKey);
        if (existing != null) {
            Long existingId = ((Number) existing.get("id")).longValue();
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("message", "Duplicate");
            data.put("transaction_id", existingId.toString());
            return Response.success(data);
        }

        // Check if this is a prescription payment
        // Prescription ID có thể nằm trong ref (format: "medcard {prescriptionId}") hoặc trong content
        Long prescriptionId = null;
        String staffId = null;
        
        // Tìm prescription ID từ ref hoặc content
        String prescriptionIdStr = null;
        if (ref != null && ref.startsWith("medcard ")) {
            prescriptionIdStr = ref.substring("medcard ".length()).trim();
        } else if (content != null && content.contains("medcard ")) {
            // Tìm "medcard {id}" trong content
            int medcardIndex = content.indexOf("medcard ");
            if (medcardIndex >= 0) {
                String afterMedcard = content.substring(medcardIndex + "medcard ".length());
                // Lấy số ID (có thể có khoảng trắng hoặc ký tự khác sau)
                String[] parts = afterMedcard.split("\\s+");
                if (parts.length > 0) {
                    prescriptionIdStr = parts[0].trim();
                }
            }
        }
        
        if (prescriptionIdStr != null && !prescriptionIdStr.isEmpty()) {
            try {
                prescriptionId = Long.parseLong(prescriptionIdStr);
                logger.info("Transaction linked to prescription: " + prescriptionId);
                
                // Lookup prescription để lấy staff_id (ưu tiên pharmacist_staff_id, nếu không có thì dùng doctor_staff_id)
                String prescriptionSql = "SELECT doctor_staff_id, pharmacist_staff_id, status FROM prescriptions WHERE id = ?";
                HashMap<String, Object> prescription = dbManager.queryOne(prescriptionSql, prescriptionId);
                if (prescription != null) {
                    // Ưu tiên pharmacist_staff_id, nếu không có thì dùng doctor_staff_id
                    staffId = prescription.get("pharmacist_staff_id") != null 
                        ? prescription.get("pharmacist_staff_id").toString()
                        : (prescription.get("doctor_staff_id") != null 
                            ? prescription.get("doctor_staff_id").toString() 
                            : null);
                    logger.info("Found staff_id from prescription: " + staffId);
                } else {
                    logger.warning("Prescription not found: " + prescriptionId);
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid prescription ID format: " + prescriptionIdStr);
            }
        }

        // Insert transaction (staff_id sẽ được update sau nếu có prescription)
        String insertSql = """
            INSERT INTO transactions (bank_id, amount, ref, content, staff_id, ts_ms, idempotency_key, payment_method, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        Long transactionId = dbManager.insertAndGetId(insertSql, bankId, amount, ref, content, staffId, timestamp, idempotencyKey, paymentMethod.toLowerCase());

        if (transactionId == null) {
            return Response.internalError("Failed to insert transaction");
        }

        // Nếu có prescription, update status và staff_id
        if (prescriptionId != null && staffId != null) {
            try {
                // Update prescription status = 2 (Hoàn tất)
                String updatePrescriptionSql = "UPDATE prescriptions SET status = 2, updated_at = NOW() WHERE id = ?";
                dbManager.update(updatePrescriptionSql, prescriptionId);
                logger.info("Updated prescription " + prescriptionId + " status to completed");
                
                // Update staff_id vào transaction (nếu chưa có)
                if (staffId != null) {
                    String updateTransactionSql = "UPDATE transactions SET staff_id = ? WHERE id = ?";
                    dbManager.update(updateTransactionSql, staffId, transactionId);
                }
            } catch (Exception e) {
                logger.warning("Failed to update prescription: " + e.getMessage());
            }
        }

        // Update audit_history (chỉ nếu có staffId từ prescription)
        if (staffId != null && !staffId.trim().isEmpty()) {
            try {
                String auditSql = """
                    INSERT INTO audit_history (timestamp, result, staff_id)
                    VALUES (NOW(), ?, ?)
                    """;
                // result=1 (thành công), chi tiết đã ghi ở system_logs
                dbManager.update(auditSql, 1, staffId);
            } catch (Exception e) {
                // Log error but don't fail transaction
                logger.warning("Failed to update audit_history: " + e.getMessage());
            }
        }

        // Update system_logs
        try {
            String systemLogSql = """
                INSERT INTO system_logs (action, admin_staff_id, description, created_at)
                VALUES (?, ?, ?, NOW())
                """;
            String logDescription = String.format("Transaction processed: ID=%d, Bank=%s, Amount=%d, Ref=%s, Method=%s", 
                transactionId, bankId, amount, ref, paymentMethod);
            dbManager.update(systemLogSql, "BANK_TRANSACTION", staffId, logDescription);
        } catch (Exception e) {
            // Log error but don't fail transaction
            logger.warning("Failed to update system_logs: " + e.getMessage());
        }

        // Trả về format mà client expect
        Map<String, Object> data = new HashMap<>();
        data.put("success", true);
        data.put("message", "OK");
        data.put("transaction_id", transactionId.toString());

        return Response.success(data);
    }

    /**
     * Tạo bảng transactions nếu chưa tồn tại
     */
    private void ensureTransactionsTableExists() {
        try {
            String createTableSql = """
                CREATE TABLE IF NOT EXISTS transactions (
                    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    bank_id VARCHAR(64) NOT NULL,
                    amount BIGINT NOT NULL,
                    ref VARCHAR(191) NOT NULL,
                    content TEXT NULL,
                    staff_id VARCHAR(64) NULL,
                    ts_ms BIGINT NOT NULL,
                    idempotency_key VARCHAR(191) NOT NULL,
                    payment_method VARCHAR(16) NOT NULL DEFAULT 'qr',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (id),
                    UNIQUE KEY uniq_idem (idempotency_key),
                    INDEX idx_bank (bank_id),
                    INDEX idx_staff (staff_id),
                    INDEX idx_method (payment_method),
                    INDEX idx_created (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
            dbManager.update(createTableSql);

            // Thêm cột nếu bảng cũ thiếu
            try { dbManager.update("CREATE INDEX IF NOT EXISTS idx_staff ON transactions(staff_id)"); } catch (Exception ignore) {}
            try { dbManager.update("CREATE INDEX IF NOT EXISTS idx_method ON transactions(payment_method)"); } catch (Exception ignore) {}
        } catch (Exception e) {
            // Table might already exist, ignore
            logger.fine("Transactions table check: " + e.getMessage());
        }
    }
}

