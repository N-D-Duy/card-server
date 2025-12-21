package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.List;

/**
 * GET /api/prescriptions - Lấy danh sách đơn thuốc
 */
public class PrescriptionsEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public PrescriptionsEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String sql = "SELECT * FROM prescriptions ORDER BY created_at DESC";
        List<HashMap<String, Object>> prescriptions = dbManager.query(sql);
        return Response.success(prescriptions);
    }
}

