package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.HashMap;

/**
 * DELETE /api/medicines/:id - Xóa thuốc
 */
public class DeleteMedicineEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public DeleteMedicineEndpoint(DbManager dbManager) {
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

        // Kiểm tra medicine có tồn tại không
        HashMap<String, Object> existing = dbManager.queryOne("SELECT * FROM medicines WHERE code = ?", medicineCode);
        if (existing == null) {
            return Response.notFound("Medicine not found: " + medicineCode);
        }

        String sql = "DELETE FROM medicines WHERE code = ?";
        int result = dbManager.update(sql, medicineCode);
        
        if (result > 0) {
            return Response.success("Medicine deleted successfully", null);
        } else {
            return Response.internalError("Failed to delete medicine");
        }
    }
}

