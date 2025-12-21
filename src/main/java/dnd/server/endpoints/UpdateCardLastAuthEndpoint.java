package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

/**
 * PUT /api/cards/:cardId/last-auth - Cập nhật last_auth_at
 * Response: {"success": true}
 */
public class UpdateCardLastAuthEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public UpdateCardLastAuthEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("cardId is required");
        }

        String cardId = parts[parts.length - 2]; // Second to last (before "last-auth")
        String sql = "UPDATE card_keys SET last_auth_at = CURRENT_TIMESTAMP WHERE card_id = ?";
        int affected = dbManager.update(sql, cardId);
        
        if (affected > 0) {
            return Response.success("Last auth time updated");
        } else {
            return Response.internalError("Failed to update last auth time");
        }
    }
}

