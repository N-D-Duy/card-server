package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;

/**
 * GET /api/medicines/:id - Lấy chi tiết một thuốc
 */
public class MedicineDetailEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public MedicineDetailEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        // Extract ID from path: /api/medicines/123 -> 123
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("Invalid medicine ID");
        }
        String medicineCode = parts[3];
        
        String sql = "SELECT * FROM medicines WHERE code = ?";
        HashMap<String, Object> medicine = dbManager.queryOne(sql, medicineCode);
        
        if (medicine == null) {
            return Response.notFound("Medicine not found: " + medicineCode);
        }
        
        return Response.success(medicine);
    }
}

