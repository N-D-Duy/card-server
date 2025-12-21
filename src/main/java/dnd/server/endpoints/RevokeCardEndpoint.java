package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * PUT /api/cards/:cardId/revoke - Vô hiệu hóa thẻ
 * Response: {"affected": 1}
 */
public class RevokeCardEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public RevokeCardEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("cardId is required");
        }

        String cardId = parts[parts.length - 2]; // Second to last (before "revoke")
        // Normalize cardId từ request: bỏ khoảng trắng để so sánh
        String normalizedCardId = cardId.replaceAll("\\s+", "");
        String sql = "UPDATE card_keys SET status = 0 WHERE REPLACE(card_id, ' ', '') = ?";
        int affected = dbManager.update(sql, normalizedCardId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("affected", affected);
        return Response.success(data);
    }
}

