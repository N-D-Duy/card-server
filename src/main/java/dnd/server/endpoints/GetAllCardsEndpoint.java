package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/cards - Lấy tất cả thẻ active
 * Response: [{"cardId": "...", "staffId": "...", ...}, ...]
 */
public class GetAllCardsEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public GetAllCardsEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String sql = "SELECT * FROM card_keys WHERE status = 1 ORDER BY issued_at DESC";
        List<HashMap<String, Object>> results = dbManager.query(sql);

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

