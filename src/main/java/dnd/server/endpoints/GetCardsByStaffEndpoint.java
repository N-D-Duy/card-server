package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/cards/staff/:staffId - Lấy danh sách thẻ của staff
 * Response: [{"cardId": "...", "status": 1, ...}, ...]
 */
public class GetCardsByStaffEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public GetCardsByStaffEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("staffId is required");
        }

        String staffId = parts[parts.length - 1];
        String sql = "SELECT * FROM card_keys WHERE staff_id = ? AND status = 1 ORDER BY issued_at DESC";
        List<HashMap<String, Object>> results = dbManager.query(sql, staffId);

        List<Map<String, Object>> cards = new ArrayList<>();
        for (HashMap<String, Object> row : results) {
            Map<String, Object> card = new HashMap<>();
            card.put("cardId", asString(row.get("card_id")));
            card.put("staffId", asString(row.get("staff_id")));
            card.put("status", row.get("status"));
            if (row.get("issued_at") != null) {
                card.put("issuedAt", row.get("issued_at").toString());
            }
            if (row.get("last_auth_at") != null) {
                card.put("lastAuthAt", row.get("last_auth_at").toString());
            }
            cards.add(card);
        }

        return Response.success(cards);
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}

