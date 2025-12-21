package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;

/**
 * GET /api/card/:cardId/staff - Lấy staff_id từ card_id
 */
public class GetStaffIdEndpoint implements EndpointHandler {
    private final DbManager dbManager;
    
    public GetStaffIdEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }
    
    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        // Extract cardId from path: /api/card/123/staff -> 123
        String[] parts = path.split("/");
        if (parts.length < 5 || !parts[4].equals("staff")) {
            return Response.badRequest("Invalid path format");
        }
        String cardId = parts[3];
        
        String sql = "SELECT staff_id FROM card_keys WHERE card_id = ? AND status = 1";
        HashMap<String, Object> result = dbManager.queryOne(sql, cardId);
        
        if (result == null) {
            return Response.notFound("Card not found or inactive: " + cardId);
        }
        
        HashMap<String, Object> data = new HashMap<>();
        data.put("staff_id", result.get("staff_id"));
        data.put("card_id", cardId);
        
        return Response.success(data);
    }
}

