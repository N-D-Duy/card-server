package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import dnd.server.util.HexUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * GET /api/cards/issue-counter/:cardType - Lấy issue counter tiếp theo cho card type
 * Response: {"counter": 123}
 */
public class GetIssueCounterEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public GetIssueCounterEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        // Extract cardType from path: /api/cards/issue-counter/1 -> 1
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("cardType is required");
        }

        byte cardType;
        try {
            cardType = Byte.parseByte(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            return Response.badRequest("Invalid cardType");
        }

        // Get all active cards
        String sql = "SELECT card_id FROM card_keys WHERE status = 1";
        java.util.List<HashMap<String, Object>> results = dbManager.query(sql);

        short maxCounter = 0;
        for (HashMap<String, Object> row : results) {
            String cardIdHex = asString(row.get("card_id"));
            if (cardIdHex != null && cardIdHex.length() >= 8) {
                try {
                    byte[] cardIdBytes = HexUtils.hexToBytes(cardIdHex);
                    if (cardIdBytes.length >= 4 && cardIdBytes[1] == cardType) {
                        // Extract counter (bytes 2-3, big-endian)
                        short counter = (short) (((cardIdBytes[2] & 0xFF) << 8) | (cardIdBytes[3] & 0xFF));
                        if (counter > maxCounter) {
                            maxCounter = counter;
                        }
                    }
                } catch (Exception e) {
                    // Skip invalid card_id
                }
            }
        }

        // Increment counter
        if (maxCounter >= 65535) {
            return Response.error(500, "Issue counter overflow for card type: " + cardType);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("counter", (short) (maxCounter + 1));
        return Response.success(data);
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
}

