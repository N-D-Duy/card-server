package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;

import com.google.gson.JsonObject;

/**
 * POST /api/medicines - Tạo thuốc mới
 */
public class CreateMedicineEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public CreateMedicineEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null) {
            return Response.badRequest("Request body is required");
        }

        String code = body.has("code") ? body.get("code").getAsString() : null;
        String name = body.has("name") ? body.get("name").getAsString() : null;
        String unit = body.has("unit") ? body.get("unit").getAsString() : "viên";
        int quantity = body.has("quantity") ? body.get("quantity").getAsInt() : 0;
        int minQuantity = body.has("min_quantity") ? body.get("min_quantity").getAsInt() : 0;

        if (code == null || name == null) {
            return Response.badRequest("code and name are required");
        }

        String sql = "INSERT INTO medicines (code, name, unit, quantity, min_quantity) VALUES (?, ?, ?, ?, ?)";
        int result = dbManager.update(sql, code, name, unit, quantity, minQuantity);
        
        if (result > 0) {
            // Lấy lại medicine vừa tạo
            HashMap<String, Object> medicine = dbManager.queryOne("SELECT * FROM medicines WHERE code = ?", code);
            return Response.success("Medicine created successfully", medicine);
        } else {
            return Response.internalError("Failed to create medicine");
        }
    }
}

