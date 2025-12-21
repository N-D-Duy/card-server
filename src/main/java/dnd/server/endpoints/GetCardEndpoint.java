package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/cards/:cardId - Lấy thông tin thẻ
 * Response: {"cardId": "...", "publicKeyRsa": "base64", "staticKeyEncrypted": "base64", "staticKeyIv": "base64", ...}
 */
public class GetCardEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public GetCardEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("cardId is required");
        }

        String cardId = parts[parts.length - 1];
        String sql = "SELECT * FROM card_keys WHERE card_id = ? AND status = 1";
        HashMap<String, Object> row = dbManager.queryOne(sql, cardId);
        
        if (row == null) {
            return Response.notFound("Card not found");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("cardId", asString(row.get("card_id")));
        data.put("staffId", asString(row.get("staff_id")));
        data.put("status", row.get("status"));
        
        // Encode binary data to base64
        if (row.get("public_key_rsa") != null) {
            data.put("publicKeyRsa", Base64.getEncoder().encodeToString((byte[]) row.get("public_key_rsa")));
        }
        if (row.get("static_key_encrypted") != null) {
            data.put("staticKeyEncrypted", Base64.getEncoder().encodeToString((byte[]) row.get("static_key_encrypted")));
        }
        if (row.get("static_key_iv") != null) {
            data.put("staticKeyIv", Base64.getEncoder().encodeToString((byte[]) row.get("static_key_iv")));
        }
        
        if (row.get("issued_at") != null) {
            data.put("issuedAt", row.get("issued_at").toString());
        }
        if (row.get("last_auth_at") != null) {
            data.put("lastAuthAt", row.get("last_auth_at").toString());
        }

        return Response.success(data);
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}

