package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * POST /api/card/apdu - Forward APDU command từ server xuống thẻ
 * 
 * Body: {
 *   "apdu": "hex_string" // APDU command dạng hex
 * }
 * 
 * Response: {
 *   "apdu": "hex_string", // APDU response dạng hex
 *   "sw": "9000" // Status word
 * }
 * 
 * NOTE: Endpoint này chỉ forward APDU, desktop app sẽ thực sự gửi xuống thẻ
 * và trả về response. Server không có quyền truy cập thẻ trực tiếp.
 */
public class ApduForwardEndpoint implements EndpointHandler {
    
    public ApduForwardEndpoint(DbManager dbManager) {
        // Không cần dbManager cho endpoint này
    }
    
    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null || !body.has("apdu")) {
            return Response.badRequest("apdu is required");
        }
        
        // Endpoint này chỉ là placeholder
        // Desktop app sẽ implement logic thực sự để forward APDU xuống thẻ
        // Server chỉ cần định nghĩa API contract
        
        Map<String, Object> data = new HashMap<>();
        data.put("message", "APDU forwarding should be handled by desktop app");
        data.put("note", "Desktop app receives APDU command and forwards to card, then returns response");
        
        return Response.success(data);
    }
}

