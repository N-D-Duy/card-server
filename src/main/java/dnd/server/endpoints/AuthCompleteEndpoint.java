package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import dnd.server.db.CardKeyDao;
import dnd.server.db.CardSessionDao;
import dnd.server.service.AuthenticationService;
import dnd.server.service.SessionManager;
import dnd.server.util.HexUtils;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * POST /api/auth/complete - Hoàn tất authentication
 * Body: {
 *   "sessionId": "...",
 *   "challengeCard": "hex_string"
 * }
 * Response: {
 *   "cryptogram": "hex_string",
 *   "sessionId": "...",
 *   "sessionEncKey": "hex_string",
 *   "sessionMacKey": "hex_string"
 * }
 */
public class AuthCompleteEndpoint implements EndpointHandler {
    private final AuthenticationService authService;
    private final SessionManager sessionManager;
    
    public AuthCompleteEndpoint(DbManager dbManager) {
        CardKeyDao cardKeyDao = new CardKeyDao(dbManager);
        CardSessionDao cardSessionDao = new CardSessionDao(dbManager);
        this.authService = new AuthenticationService(cardKeyDao, cardSessionDao);
        this.sessionManager = SessionManager.getInstance();
    }
    
    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null || !body.has("sessionId") || !body.has("challengeCard")) {
            return Response.badRequest("sessionId and challengeCard are required");
        }
        
        String tempSessionId = body.get("sessionId").getAsString();
        byte[] challengeCard = HexUtils.hexToBytes(body.get("challengeCard").getAsString());
        
        // Lấy session
        SessionManager.AuthSession session = sessionManager.getSession(tempSessionId);
        if (session == null) {
            return Response.badRequest("Invalid or expired session");
        }
        
        // Hoàn tất authentication
        AuthenticationService.AuthCompleteResult result = authService.completeAuthentication(
            session.cardId,
            session.staticKey,
            session.challengeServer,
            challengeCard
        );
        
        // Xóa temp session
        sessionManager.removeSession(tempSessionId);
        
        // Trả về response
        Map<String, Object> data = new HashMap<>();
        data.put("cryptogram", HexUtils.bytesToHex(result.cryptogram));
        data.put("sessionId", result.sessionId);
        data.put("sessionEncKey", HexUtils.bytesToHex(result.sessionEncKey));
        data.put("sessionMacKey", HexUtils.bytesToHex(result.sessionMacKey));
        
        return Response.success(data);
    }
}

