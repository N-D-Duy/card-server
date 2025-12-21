package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;
import java.util.List;

/**
 * GET /api/prescriptions/:id - Lấy chi tiết đơn thuốc (bao gồm items)
 */
public class PrescriptionDetailEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public PrescriptionDetailEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("Invalid prescription ID");
        }
        String prescriptionId = parts[3];
        
        // Lấy prescription header
        String sql = "SELECT * FROM prescriptions WHERE id = ?";
        HashMap<String, Object> prescription = dbManager.queryOne(sql, prescriptionId);
        
        if (prescription == null) {
            return Response.notFound("Prescription not found: " + prescriptionId);
        }
        
        // Lấy prescription items
        String itemsSql = "SELECT * FROM prescription_items WHERE prescription_id = ?";
        List<HashMap<String, Object>> items = dbManager.query(itemsSql, prescriptionId);
        prescription.put("items", items);
        
        return Response.success(prescription);
    }
}

