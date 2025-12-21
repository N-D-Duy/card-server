package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.List;

/**
 * GET /api/medicines - Lấy danh sách thuốc
 */
public class MedicinesEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public MedicinesEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String sql = "SELECT * FROM medicines ORDER BY code";
        List<HashMap<String, Object>> medicines = dbManager.query(sql);
        return Response.success(medicines);
    }
}

