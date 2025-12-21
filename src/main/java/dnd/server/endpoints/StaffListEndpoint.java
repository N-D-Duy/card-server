package dnd.server.endpoints;

import dnd.server.db.DbManager;
import dnd.server.api.Request;
import dnd.server.api.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /api/staff - Lấy danh sách staff
 * Query params: role (optional, comma-separated), active (optional, default true)
 * Response: [{"staffId": "...", "shortName": "...", "fullName": "...", "role": 1, ...}, ...]
 */
public class StaffListEndpoint implements EndpointHandler {
    private final DbManager dbManager;

    public StaffListEndpoint(DbManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Response handle(Request request) throws Exception {
        Map<String, String> query = request.getQueryParams();
        
        // Build SQL query
        StringBuilder sql = new StringBuilder("""
                SELECT 
                    s.staff_id,
                    s.short_name,
                    s.full_name,
                    s.role,
                    s.department,
                    s.active,
                    CASE WHEN a.id IS NOT NULL AND a.active = 1 THEN 1 ELSE 0 END AS is_admin,
                    CASE 
                        WHEN EXISTS (
                            SELECT 1 FROM card_keys ck 
                            WHERE ck.staff_id = s.staff_id AND ck.status = 1
                        ) THEN 1 
                        ELSE 0 
                    END AS has_card
                FROM staff_info s
                LEFT JOIN admin_accounts a ON a.staff_id = s.staff_id
                WHERE 1=1
                """);
        
        List<Object> params = new ArrayList<>();
        
        // Filter by active status
        String activeParam = query != null ? query.get("active") : null;
        if (activeParam != null && !activeParam.isEmpty()) {
            boolean active = Boolean.parseBoolean(activeParam);
            sql.append(" AND s.active = ?");
            params.add(active ? 1 : 0);
        } else {
            // Default to active only
            sql.append(" AND s.active = 1");
        }
        
        // Filter by role
        String roleParam = query != null ? query.get("role") : null;
        if (roleParam != null && !roleParam.isEmpty()) {
            String[] roles = roleParam.split(",");
            if (roles.length > 0) {
                sql.append(" AND s.role IN (");
                for (int i = 0; i < roles.length; i++) {
                    if (i > 0) sql.append(",");
                    sql.append("?");
                    try {
                        params.add(Integer.parseInt(roles[i].trim()));
                    } catch (NumberFormatException e) {
                        // Skip invalid role
                    }
                }
                sql.append(")");
            }
        }
        
        sql.append(" ORDER BY s.staff_id");
        
        List<HashMap<String, Object>> results = dbManager.query(sql.toString(), params.toArray());
        
        // Convert to response format
        List<Map<String, Object>> staffList = new ArrayList<>();
        for (HashMap<String, Object> row : results) {
            Map<String, Object> staff = new HashMap<>();
            staff.put("staffId", asString(row.get("staff_id")));
            staff.put("shortName", asString(row.get("short_name")));
            staff.put("fullName", asString(row.get("full_name")));
            staff.put("role", toInt(row.get("role"), 0));
            staff.put("department", asString(row.get("department")));
            staff.put("active", toBoolean(row.get("active")));
            staff.put("isAdmin", toBoolean(row.get("is_admin")));
            staff.put("hasCard", toBoolean(row.get("has_card")));
            staffList.add(staff);
        }
        
        return Response.success(staffList);
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }
    
    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private boolean toBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            return str.equals("true") || str.equals("1") || str.equals("yes");
        }
        return false;
    }
}

