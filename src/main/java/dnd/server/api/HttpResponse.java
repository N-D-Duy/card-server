package dnd.server.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * HTTP Response wrapper
 */
public class HttpResponse {
    private int statusCode;
    private String body;

    private HttpResponse(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    /**
     * Tạo success response
     */
    public static HttpResponse success(Object data) {
        return success("Success", data);
    }

    public static HttpResponse success(String message, Object data) {
        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        
        // Nếu data là Map và có key "success", merge vào response (cho transaction endpoint)
        // Format: {"success": true, "message": "...", "transaction_id": "..."}
        if (data instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
            if (dataMap.containsKey("success")) {
                // Merge tất cả fields từ data vào response, không dùng message từ parameter
                for (java.util.Map.Entry<String, Object> entry : dataMap.entrySet()) {
                    if (entry.getValue() != null) {
                        json.add(entry.getKey(), gson.toJsonTree(entry.getValue()));
                    }
                }
                return new HttpResponse(200, gson.toJson(json));
            }
        }
        
        // Format mặc định
        json.addProperty("statusCode", 200);
        json.addProperty("message", message);
        if (data != null) {
            json.add("data", gson.toJsonTree(data));
        }
        return new HttpResponse(200, gson.toJson(json));
    }

    /**
     * Tạo error response
     */
    public static HttpResponse error(int statusCode, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("statusCode", statusCode);
        JsonObject error = new JsonObject();
        error.addProperty("message", message);
        json.add("error", error);
        Gson gson = new Gson();
        return new HttpResponse(statusCode, gson.toJson(json));
    }

    public static HttpResponse notFound(String message) {
        return error(404, message);
    }

    public static HttpResponse badRequest(String message) {
        return error(400, message);
    }

    public static HttpResponse internalError(String message) {
        return error(500, message);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }
}

