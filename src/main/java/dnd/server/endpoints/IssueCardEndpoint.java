package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * POST /api/cards/issue - Nạp thẻ mới
 * Body: {"cardId": "...", "staticKeyEncrypted": "base64", "staticKeyIv": "base64", 
 *        "publicKeyRsa": "base64", "staffId": "..."}
 * Response: {"id": 123}
 */
public class IssueCardEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public IssueCardEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null || !body.has("cardId") || !body.has("staticKeyEncrypted") 
            || !body.has("staticKeyIv") || !body.has("publicKeyRsa")) {
            return Response.badRequest("cardId, staticKeyEncrypted, staticKeyIv, and publicKeyRsa are required");
        }

        String cardId = body.get("cardId").getAsString();
        String staticKeyEncryptedB64 = body.get("staticKeyEncrypted").getAsString();
        String staticKeyIvB64 = body.get("staticKeyIv").getAsString();
        String publicKeyRsaB64 = body.get("publicKeyRsa").getAsString();
        String staffId = body.has("staffId") && !body.get("staffId").isJsonNull() 
            ? body.get("staffId").getAsString() : null;

        // Decode base64
        byte[] staticKeyEncrypted = Base64.getDecoder().decode(staticKeyEncryptedB64);
        byte[] staticKeyIv = Base64.getDecoder().decode(staticKeyIvB64);
        byte[] publicKeyRsa = Base64.getDecoder().decode(publicKeyRsaB64);

        // Validate staff_id if provided
        String validatedStaffId = null;
        if (staffId != null && !staffId.trim().isEmpty()) {
            if (staffIdExists(staffId)) {
                validatedStaffId = staffId;
            }
        }

        String sql = """
                INSERT INTO card_keys (card_id, static_key_encrypted, static_key_iv, public_key_rsa, staff_id, status)
                VALUES (?, ?, ?, ?, ?, 1)
                """;
        
        Long id = dbManager.insertAndGetId(sql, cardId, staticKeyEncrypted, staticKeyIv, 
                                           publicKeyRsa, validatedStaffId);
        
        if (id == null) {
            return Response.internalError("Failed to save card");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        return Response.success(data);
    }

    private boolean staffIdExists(String staffId) {
        HashMap<String, Object> row = dbManager.queryOne(
            "SELECT COUNT(*) as cnt FROM staff_info WHERE staff_id = ?", staffId);
        if (row != null) {
            Object cnt = row.get("cnt");
            if (cnt instanceof Number) {
                return ((Number) cnt).intValue() > 0;
            }
        }
        return false;
    }
}

