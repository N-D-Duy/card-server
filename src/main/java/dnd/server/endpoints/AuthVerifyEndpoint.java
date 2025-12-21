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
 * POST /api/auth/verify - Verify signature từ thẻ
 * Body: {
 *   "sessionId": "...",
 *   "signature": "hex_string",
 *   "challengeCard": "hex_string"
 * }
 * Response: {"valid": true}
 */
public class AuthVerifyEndpoint implements EndpointHandler {
    private final AuthenticationService authService;
    private final SessionManager sessionManager;
    
    public AuthVerifyEndpoint(DbManager dbManager) {
        CardKeyDao cardKeyDao = new CardKeyDao(dbManager);
        CardSessionDao cardSessionDao = new CardSessionDao(dbManager);
        this.authService = new AuthenticationService(cardKeyDao, cardSessionDao);
        this.sessionManager = SessionManager.getInstance();
    }
    
    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null || !body.has("sessionId") || !body.has("signature") || !body.has("challengeCard")) {
            return Response.badRequest("sessionId, signature, and challengeCard are required");
        }
        
        String sessionId = body.get("sessionId").getAsString();
        byte[] signature = HexUtils.hexToBytes(body.get("signature").getAsString());
        byte[] challengeCard = HexUtils.hexToBytes(body.get("challengeCard").getAsString());
        
        // Lấy session
        SessionManager.AuthSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            return Response.badRequest("Invalid or expired session");
        }
        
        // Verify signature
        AuthenticationService.AuthVerifyResult result = authService.verifySignature(
            session.publicKeyBytes,
            session.challengeServer,
            signature,
            challengeCard
        );
        
        // Cập nhật session với challengeCard
        // (Có thể tạo một class mới hoặc extend, nhưng tạm thời giữ nguyên)
        
        Map<String, Object> data = new HashMap<>();
        data.put("valid", result.valid);
        
        return Response.success(data);
    }
}

