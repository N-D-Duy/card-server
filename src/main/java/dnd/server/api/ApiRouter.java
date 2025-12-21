package dnd.server.api;

import dnd.server.db.DbManager;
import dnd.server.endpoints.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Router để route các API requests đến các endpoint handlers tương ứng
 */
public class ApiRouter {
    private static final Logger logger = Logger.getLogger(ApiRouter.class.getName());
    
    private final DbManager dbManager;
    private final Map<String, EndpointHandler> routes;

    public ApiRouter(DbManager dbManager) {
        this.dbManager = dbManager;
        this.routes = new HashMap<>();
        registerRoutes();
    }

    /**
     * Đăng ký các routes
     */
    private void registerRoutes() {
        // Medicines endpoints
        routes.put("GET /api/medicines", new MedicinesEndpoint(dbManager));
        routes.put("GET /api/medicines/:id", new MedicineDetailEndpoint(dbManager));
        routes.put("POST /api/medicines", new CreateMedicineEndpoint(dbManager));
        routes.put("PUT /api/medicines/:id", new UpdateMedicineEndpoint(dbManager));
        routes.put("DELETE /api/medicines/:id", new DeleteMedicineEndpoint(dbManager));
        
        // Prescriptions endpoints
        routes.put("GET /api/prescriptions", new PrescriptionsEndpoint(dbManager));
        routes.put("GET /api/prescriptions/:id", new PrescriptionDetailEndpoint(dbManager));
        routes.put("POST /api/prescriptions", new CreatePrescriptionEndpoint(dbManager));
        routes.put("PUT /api/prescriptions/:id", new UpdatePrescriptionEndpoint(dbManager));
        
        // Inventory endpoints
        routes.put("GET /api/inventory/logs", new InventoryLogsEndpoint(dbManager));
        routes.put("POST /api/inventory/import", new ImportInventoryEndpoint(dbManager));
        routes.put("POST /api/inventory/export", new ExportInventoryEndpoint(dbManager));
        
        // History endpoints
        routes.put("GET /api/history", new HistoryEndpoint(dbManager));
        
        // Dashboard/Stats endpoints
        routes.put("GET /api/dashboard/stats", new DashboardStatsEndpoint(dbManager));
        
        // Authentication endpoints
        routes.put("POST /api/auth/start", new dnd.server.endpoints.AuthStartEndpoint(dbManager));
        routes.put("POST /api/auth/verify", new dnd.server.endpoints.AuthVerifyEndpoint(dbManager));
        routes.put("POST /api/auth/complete", new dnd.server.endpoints.AuthCompleteEndpoint(dbManager));
        
        // Admin endpoints
        routes.put("POST /api/admin/login", new dnd.server.endpoints.AdminLoginEndpoint(dbManager));
        
        // System endpoints
        routes.put("POST /api/system/logs", new dnd.server.endpoints.SystemLogEndpoint(dbManager));
        routes.put("GET /api/system/logs/system", new dnd.server.endpoints.SystemLogsListEndpoint(dbManager));
        routes.put("GET /api/system/logs/audit", new dnd.server.endpoints.AuditHistoryEndpoint(dbManager));
        routes.put("GET /api/system/inventory/summary", new dnd.server.endpoints.InventorySummaryEndpoint(dbManager));
        
        // Staff endpoints
        routes.put("GET /api/staff", new dnd.server.endpoints.StaffListEndpoint(dbManager));
        routes.put("GET /api/staff/:id/profile", new dnd.server.endpoints.GetStaffProfileEndpoint(dbManager));
        routes.put("POST /api/staff", new dnd.server.endpoints.CreateStaffEndpoint(dbManager));
        routes.put("PUT /api/staff/:id", new dnd.server.endpoints.UpdateStaffEndpoint(dbManager));
        routes.put("PUT /api/staff/:id/active", new dnd.server.endpoints.UpdateStaffActiveEndpoint(dbManager));
        routes.put("PUT /api/staff/:id/password", new dnd.server.endpoints.ResetStaffPasswordEndpoint(dbManager));
        routes.put("POST /api/staff/:id/admin", new dnd.server.endpoints.GrantAdminEndpoint(dbManager));
        routes.put("DELETE /api/staff/:id/admin", new dnd.server.endpoints.RevokeAdminEndpoint(dbManager));
        
        // Card endpoints
        routes.put("POST /api/cards/issue", new dnd.server.endpoints.IssueCardEndpoint(dbManager));
        routes.put("GET /api/cards/issue-counter/:type", new dnd.server.endpoints.GetIssueCounterEndpoint(dbManager));
        routes.put("GET /api/cards", new dnd.server.endpoints.GetAllCardsEndpoint(dbManager));
        routes.put("GET /api/cards/:id", new dnd.server.endpoints.GetCardEndpoint(dbManager));
        routes.put("GET /api/cards/:id/keys", new dnd.server.endpoints.GetCardKeysEndpoint(dbManager));
        routes.put("GET /api/cards/staff/:id", new dnd.server.endpoints.GetCardsByStaffEndpoint(dbManager));
        routes.put("PUT /api/cards/staff/:id/revoke", new dnd.server.endpoints.RevokeCardByStaffEndpoint(dbManager));
        routes.put("PUT /api/cards/:id/revoke", new dnd.server.endpoints.RevokeCardEndpoint(dbManager));
        routes.put("PUT /api/cards/:id/activate", new dnd.server.endpoints.ActivateCardEndpoint(dbManager));
        routes.put("PUT /api/cards/:id/last-auth", new dnd.server.endpoints.UpdateCardLastAuthEndpoint(dbManager));
        routes.put("POST /api/cards/sessions", new dnd.server.endpoints.CreateCardSessionEndpoint(dbManager));
        
        // APDU forwarding endpoint
        routes.put("POST /api/card/apdu", new dnd.server.endpoints.ApduForwardEndpoint(dbManager));
        
        // Card info endpoint
        routes.put("GET /api/card/:id/staff", new dnd.server.endpoints.GetStaffIdEndpoint(dbManager));
        
        // Health check
        routes.put("GET /api/health", new HealthCheckEndpoint(dbManager));
    }

    /**
     * Xử lý HTTP request và trả về HTTP response
     */
    public HttpResponse handle(HttpRequest request) {
        String method = request.getMethod().toUpperCase();
        String path = request.getPath();
        
        // Tìm route khớp
        EndpointHandler handler = findHandler(method, path);
        
        if (handler == null) {
            logger.warning("Không tìm thấy handler cho: " + method + " " + path);
            return HttpResponse.notFound("Endpoint không tồn tại: " + method + " " + path);
        }

        try {
            // Convert HttpRequest thành Request (legacy) để tương thích với endpoints
            Request legacyRequest = convertToLegacyRequest(request);
            Response legacyResponse = handler.handle(legacyRequest);
            
            // Convert Response thành HttpResponse
            return convertToHttpResponse(legacyResponse);
        } catch (Exception e) {
            logger.severe("Lỗi khi xử lý request: " + e.getMessage());
            e.printStackTrace();
            return HttpResponse.internalError("Lỗi server: " + e.getMessage());
        }
    }

    /**
     * Convert HttpRequest thành Request (legacy) để tương thích
     */
    private Request convertToLegacyRequest(HttpRequest httpRequest) {
        return Request.fromHttpRequest(httpRequest);
    }

    /**
     * Convert Response thành HttpResponse
     */
    private HttpResponse convertToHttpResponse(Response response) {
        if (response.getStatusCode() == 200) {
            return HttpResponse.success(response.getMessage(), response.getData());
        } else {
            String errorMsg = response.getError() != null 
                ? response.getError().get("message").getAsString() 
                : "Error";
            return HttpResponse.error(response.getStatusCode(), errorMsg);
        }
    }

    /**
     * Tìm handler phù hợp với method và path
     */
    private EndpointHandler findHandler(String method, String path) {
        // Thử exact match trước
        String key = method + " " + path;
        EndpointHandler handler = routes.get(key);
        if (handler != null) {
            return handler;
        }

        // Thử pattern matching cho routes có parameters (ví dụ: /api/medicines/:id)
        for (Map.Entry<String, EndpointHandler> entry : routes.entrySet()) {
            String routeKey = entry.getKey();
            String[] parts = routeKey.split(" ", 2);
            if (parts.length == 2 && parts[0].equals(method)) {
                String routePath = parts[1];
                if (matchesPath(routePath, path)) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

    /**
     * Kiểm tra xem path có khớp với route pattern không
     * Ví dụ: /api/medicines/:id khớp với /api/medicines/123
     */
    private boolean matchesPath(String routePattern, String actualPath) {
        // Convert pattern sang regex
        String regex = routePattern.replaceAll(":[^/]+", "[^/]+");
        return Pattern.matches(regex, actualPath);
    }
}

