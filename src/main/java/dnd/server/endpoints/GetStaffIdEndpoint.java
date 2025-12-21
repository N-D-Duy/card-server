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
        
        // Normalize cardId từ request: bỏ khoảng trắng để so sánh
        // DB lưu dưới dạng "XX XX XX", request có thể là "XXXXXX" hoặc "XX XX XX"
        String normalizedCardId = cardId.replaceAll("\\s+", "");
        
        // Query: normalize cả cardId trong DB để so sánh (bỏ khoảng trắng)
        String sql = "SELECT staff_id FROM card_keys WHERE REPLACE(card_id, ' ', '') = ? AND status = 1";
        HashMap<String, Object> result = dbManager.queryOne(sql, normalizedCardId);
        
        if (result == null) {
            return Response.notFound("Card not found or inactive: " + cardId);
        }
        
        HashMap<String, Object> data = new HashMap<>();
        data.put("staff_id", result.get("staff_id"));
        data.put("card_id", cardId);
        
        return Response.success(data);
    }
}

