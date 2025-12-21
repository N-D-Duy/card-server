package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.HashMap;

/**
 * POST /api/prescriptions - Tạo đơn thuốc mới
 */
public class CreatePrescriptionEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public CreatePrescriptionEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        JsonObject body = request.getBody();
        if (body == null) {
            return Response.badRequest("Request body is required");
        }

        String prescriptionCode = body.has("prescription_code") ? body.get("prescription_code").getAsString() : null;
        String patientId = body.has("patient_id") ? body.get("patient_id").getAsString() : null;
        String doctorStaffId = body.has("doctor_staff_id") ? body.get("doctor_staff_id").getAsString() : null;
        int status = body.has("status") ? body.get("status").getAsInt() : 0;
        String note = body.has("note") ? body.get("note").getAsString() : null;

        if (prescriptionCode == null || patientId == null || doctorStaffId == null) {
            return Response.badRequest("prescription_code, patient_id, and doctor_staff_id are required");
        }

        // Tạo prescription header
        String sql = "INSERT INTO prescriptions (prescription_code, patient_id, doctor_staff_id, status, note) VALUES (?, ?, ?, ?, ?)";
        Long prescriptionId = dbManager.insertAndGetId(sql, prescriptionCode, patientId, doctorStaffId, status, note);
        
        if (prescriptionId == null) {
            return Response.internalError("Failed to create prescription");
        }

        // Tạo prescription items nếu có
        if (body.has("items") && body.get("items").isJsonArray()) {
            JsonArray items = body.getAsJsonArray("items");
            String itemSql = "INSERT INTO prescription_items (prescription_id, medicine_code, quantity, dosage, note) VALUES (?, ?, ?, ?, ?)";
            
            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                String medicineCode = item.has("medicine_code") ? item.get("medicine_code").getAsString() : null;
                int quantity = item.has("quantity") ? item.get("quantity").getAsInt() : 0;
                String dosage = item.has("dosage") ? item.get("dosage").getAsString() : null;
                String itemNote = item.has("note") ? item.get("note").getAsString() : null;
                
                if (medicineCode != null && quantity > 0) {
                    dbManager.update(itemSql, prescriptionId, medicineCode, quantity, dosage, itemNote);
                }
            }
        }

        // Lấy lại prescription vừa tạo
        HashMap<String, Object> prescription = dbManager.queryOne("SELECT * FROM prescriptions WHERE id = ?", prescriptionId);
        return Response.success("Prescription created successfully", prescription);
    }
}

