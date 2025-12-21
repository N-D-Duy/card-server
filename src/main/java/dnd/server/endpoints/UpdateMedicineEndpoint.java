package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;

import java.util.HashMap;

/**
 * PUT /api/medicines/:id - Cập nhật thuốc
 */
public class UpdateMedicineEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public UpdateMedicineEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        String path = request.getPath();
        String[] parts = path.split("/");
        if (parts.length < 4) {
            return Response.badRequest("Invalid medicine ID");
        }
        String medicineCode = parts[3];

        JsonObject body = request.getBody();
        if (body == null) {
            return Response.badRequest("Request body is required");
        }

        // Kiểm tra medicine có tồn tại không
        HashMap<String, Object> existing = dbManager.queryOne("SELECT * FROM medicines WHERE code = ?", medicineCode);
        if (existing == null) {
            return Response.notFound("Medicine not found: " + medicineCode);
        }

        // Build update query
        StringBuilder sql = new StringBuilder("UPDATE medicines SET ");
        java.util.List<Object> params = new java.util.ArrayList<>();
        boolean first = true;

        if (body.has("name")) {
            if (!first) sql.append(", ");
            sql.append("name = ?");
            params.add(body.get("name").getAsString());
            first = false;
        }
        if (body.has("unit")) {
            if (!first) sql.append(", ");
            sql.append("unit = ?");
            params.add(body.get("unit").getAsString());
            first = false;
        }
        if (body.has("quantity")) {
            if (!first) sql.append(", ");
            sql.append("quantity = ?");
            params.add(body.get("quantity").getAsInt());
            first = false;
        }
        if (body.has("min_quantity")) {
            if (!first) sql.append(", ");
            sql.append("min_quantity = ?");
            params.add(body.get("min_quantity").getAsInt());
            first = false;
        }

        if (first) {
            return Response.badRequest("No fields to update");
        }

        sql.append(" WHERE code = ?");
        params.add(medicineCode);

        int result = dbManager.update(sql.toString(), params.toArray());
        
        if (result > 0) {
            HashMap<String, Object> updated = dbManager.queryOne("SELECT * FROM medicines WHERE code = ?", medicineCode);
            return Response.success("Medicine updated successfully", updated);
        } else {
            return Response.internalError("Failed to update medicine");
        }
    }
}

