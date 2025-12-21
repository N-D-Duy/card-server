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

