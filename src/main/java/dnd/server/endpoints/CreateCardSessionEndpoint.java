package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.sql.Timestamp;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * POST /api/cards/sessions - Lưu card session
 * Body: {"cardId": "...", "sessionId": "...", "challengeServer": "base64", 
 *        "challengeCard": "base64", "expiresAt": "timestamp"}
 * Response: {"id": 123}
 */
public class CreateCardSessionEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public CreateCardSessionEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null || !body.has("cardId") || !body.has("sessionId") 
            || !body.has("challengeServer") || !body.has("challengeCard") 
            || !body.has("expiresAt")) {
            return Response.badRequest("cardId, sessionId, challengeServer, challengeCard, and expiresAt are required");
        }

        String cardId = body.get("cardId").getAsString();
        String sessionId = body.get("sessionId").getAsString();
        byte[] challengeServer = Base64.getDecoder().decode(body.get("challengeServer").getAsString());
        byte[] challengeCard = Base64.getDecoder().decode(body.get("challengeCard").getAsString());
        Timestamp expiresAt = Timestamp.valueOf(body.get("expiresAt").getAsString());

        String sql = """
                INSERT INTO card_sessions (card_id, session_id, challenge_server, challenge_card, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        
        Long id = dbManager.insertAndGetId(sql, cardId, sessionId, challengeServer, challengeCard, expiresAt);
        
        if (id == null) {
            return Response.internalError("Failed to create card session");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        return Response.success(data);
    }
}

