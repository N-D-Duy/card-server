package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/cards/:cardId/keys - Lấy public key và static key (encrypted) của thẻ
 * Response: {"publicKeyRsa": "base64", "staticKeyEncrypted": "base64", "staticKeyIv": "base64"}
 */
public class GetCardKeysEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public GetCardKeysEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("cardId is required");
        }

        String cardId = parts[parts.length - 2]; // Second to last (before "keys")
        String sql = "SELECT public_key_rsa, static_key_encrypted, static_key_iv FROM card_keys WHERE card_id = ? AND status = 1";
        HashMap<String, Object> row = dbManager.queryOne(sql, cardId);
        
        if (row == null) {
            return Response.notFound("Card not found");
        }

        Map<String, Object> data = new HashMap<>();
        
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

        return Response.success(data);
    }
}

