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

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * POST /api/auth/start - Bắt đầu authentication
 * Body: {"cardId": "hex_string"}
 * Response: {"sessionId": "...", "challengeServer": "hex_string"}
 */
public class AuthStartEndpoint implements EndpointHandler {
    private final AuthenticationService authService;
    private final SessionManager sessionManager;
    
    public AuthStartEndpoint(DbManager dbManager) {
        CardKeyDao cardKeyDao = new CardKeyDao(dbManager);
        CardSessionDao cardSessionDao = new CardSessionDao(dbManager);
        this.authService = new AuthenticationService(cardKeyDao, cardSessionDao);
        this.sessionManager = SessionManager.getInstance();
    }
    
    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null || !body.has("cardId")) {
            return Response.badRequest("cardId is required");
        }
        
        String cardId = body.get("cardId").getAsString();
        
        // Bypass nếu cardId toàn 0 (thẻ reset/chưa nạp) để tránh lỗi
        String normalized = cardId.replaceAll("\\s+", "").toUpperCase();
        if (normalized.matches("^0+$")) {
            return Response.success("Card empty, bypass", new HashMap<>());
        }
        
        // Bắt đầu authentication
        AuthenticationService.AuthStartResult result = authService.startAuthentication(cardId);
        
        // Tạo session ID tạm thời để lưu static key
        String tempSessionId = generateTempSessionId();
        SessionManager.AuthSession session = new SessionManager.AuthSession(
            cardId,
            result.staticKey,
            result.publicKeyBytes,
            result.challengeServer
        );
        sessionManager.putSession(tempSessionId, session);
        
        // Trả về response
        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", tempSessionId);
        data.put("challengeServer", HexUtils.bytesToHex(result.challengeServer));
        
        return Response.success(data);
    }
    
    private String generateTempSessionId() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder(32);
        for (byte value : b) {
            sb.append(String.format("%02x", value));
        }
        return sb.toString();
    }
}

