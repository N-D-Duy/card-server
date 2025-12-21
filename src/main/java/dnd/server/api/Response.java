package dnd.server.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Response object đại diện cho một API response
 */
public class Response {
    private int statusCode;
    private String message;
    private Object data;
    private JsonObject error;

    private Response(int statusCode, String message, Object data) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }

    private Response(int statusCode, JsonObject error) {
        this.statusCode = statusCode;
        this.error = error;
    }

    /**
     * Tạo success response
     */
    public static Response success(Object data) {
        return new Response(200, "Success", data);
    }

    public static Response success(String message, Object data) {
        return new Response(200, message, data);
    }

    /**
     * Tạo error response
     */
    public static Response error(int statusCode, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("message", message);
        return new Response(statusCode, error);
    }

    public static Response notFound(String message) {
        return error(404, message);
    }

    public static Response badRequest(String message) {
        return error(400, message);
    }

    public static Response internalError(String message) {
        return error(500, message);
    }

    /**
     * Convert response sang JSON string
     */
    public String toJson() {
        Gson gson = new Gson();
        JsonObject json = new JsonObject();
        json.addProperty("statusCode", statusCode);
        
        if (error != null) {
            json.add("error", error);
        } else {
            if (message != null) {
                json.addProperty("message", message);
            }
            if (data != null) {
                json.add("data", gson.toJsonTree(data));
            }
        }
        
        return gson.toJson(json);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public JsonObject getError() {
        return error;
    }
}

