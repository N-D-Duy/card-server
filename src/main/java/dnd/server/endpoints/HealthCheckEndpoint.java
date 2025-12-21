package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check endpoint
 */
public class HealthCheckEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public HealthCheckEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "ok");
        data.put("database", dbManager.isInitialized() ? "connected" : "disconnected");
        if (dbManager.isInitialized()) {
            data.put("poolInfo", dbManager.getPoolInfo());
        }
        return Response.success(data);
    }
}

