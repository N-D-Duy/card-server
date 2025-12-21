package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * PUT /api/prescriptions/:id - Cập nhật đơn thuốc
 */
public class UpdatePrescriptionEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public UpdatePrescriptionEndpoint(DbManager dbManager) {
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

        JsonObject body = request.getBody();
        if (body == null) {
            return Response.badRequest("Request body is required");
        }

        // Kiểm tra prescription có tồn tại không
        HashMap<String, Object> existing = dbManager.queryOne("SELECT * FROM prescriptions WHERE id = ?", prescriptionId);
        if (existing == null) {
            return Response.notFound("Prescription not found: " + prescriptionId);
        }

        // Build update query
        StringBuilder sql = new StringBuilder("UPDATE prescriptions SET ");
        java.util.List<Object> params = new java.util.ArrayList<>();
        boolean first = true;

        if (body.has("status")) {
            if (!first) sql.append(", ");
            sql.append("status = ?");
            params.add(body.get("status").getAsInt());
            first = false;
        }
        if (body.has("pharmacist_staff_id")) {
            if (!first) sql.append(", ");
            sql.append("pharmacist_staff_id = ?");
            params.add(body.get("pharmacist_staff_id").getAsString());
            first = false;
        }
        if (body.has("note")) {
            if (!first) sql.append(", ");
            sql.append("note = ?");
            params.add(body.get("note").getAsString());
            first = false;
        }

        if (first) {
            return Response.badRequest("No fields to update");
        }

        sql.append(" WHERE id = ?");
        params.add(prescriptionId);

        int result = dbManager.update(sql.toString(), params.toArray());
        
        if (result > 0) {
            HashMap<String, Object> updated = dbManager.queryOne("SELECT * FROM prescriptions WHERE id = ?", prescriptionId);
            return Response.success("Prescription updated successfully", updated);
        } else {
            return Response.internalError("Failed to update prescription");
        }
    }
}

