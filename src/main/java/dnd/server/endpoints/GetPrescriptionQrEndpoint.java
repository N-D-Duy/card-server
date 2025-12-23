package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import dnd.server.config.BankConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * GET /api/prescriptions/:id/qr-code - Lấy QR code URL cho prescription
 * 
 * Response:
 * {
 *   "qrImageUrl": "https://img.vietqr.io/image/...",
 *   "qrContent": "medcard {prescriptionId}",
 *   "accountNumber": "...",
 *   "accountName": "...",
 *   "accounts": [{"number": "...", "name": "..."}, ...]
 * }
 */
public class GetPrescriptionQrEndpoint implements EndpointHandler {
    private final DbManager dbManager;
    private final BankConfig bankConfig;
    
    public GetPrescriptionQrEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
        this.bankConfig = BankConfig.getInstance();
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        // Extract prescription ID from path: /api/prescriptions/123/qr-code -> 123
        String[] parts = path.split("/");
        if (parts.length < 4 || !parts[parts.length - 1].equals("qr-code")) {
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
            "SELECT id, prescription_code FROM prescriptions WHERE id = ?", prescriptionId);
        if (prescription == null) {
            return Response.notFound("Prescription not found");
        }
        
        // Generate QR content: "medcard {prescriptionId}"
        String qrContent = "medcard " + prescriptionId;
        
        // Build QR URLs for all accounts (server-side only)
        List<Map<String, Object>> qrAccounts = new ArrayList<>();
        List<BankConfig.BankAccount> bankAccounts = bankConfig.getAccounts();
        for (BankConfig.BankAccount account : bankAccounts) {
            String accountNumber = account.getNumber();
            String accountName = account.getName();
            
            // Build QR code URL
            String qrImageUrl = String.format(
                "https://img.vietqr.io/image/MB-%s-qr_only.png?&addInfo=%s&accountName=%s",
                URLEncoder.encode(accountNumber, StandardCharsets.UTF_8),
                URLEncoder.encode(qrContent, StandardCharsets.UTF_8),
                URLEncoder.encode(accountName, StandardCharsets.UTF_8)
            );
            
            Map<String, Object> qrAccount = new HashMap<>();
            qrAccount.put("number", accountNumber);
            qrAccount.put("name", accountName);
            qrAccount.put("qrImageUrl", qrImageUrl);
            qrAccounts.add(qrAccount);
        }
        
        // Use first account as default
        Map<String, Object> defaultAccount = qrAccounts.get(0);
        
        Map<String, Object> data = new HashMap<>();
        data.put("qrImageUrl", defaultAccount.get("qrImageUrl")); // Default QR URL
        data.put("qrContent", qrContent);
        data.put("accountNumber", defaultAccount.get("number"));
        data.put("accountName", defaultAccount.get("name"));
        data.put("qrAccounts", qrAccounts); // All QR URLs pre-built
        data.put("prescriptionId", prescriptionId);
        data.put("prescriptionCode", prescription.get("prescription_code"));
        
        return Response.success(data);
    }
}