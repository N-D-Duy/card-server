package dnd.server.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Request object đại diện cho một API request
 */
public class Request {
    public String method; // GET, POST, PUT, DELETE
    public String path;
    public Map<String, String> headers;
    public JsonObject body;
    public Map<String, String> queryParams;

    private Request() {
        this.headers = new HashMap<>();
        this.queryParams = new HashMap<>();
    }

    /**
     * Tạo Request từ HttpRequest
     */
    public static Request fromHttpRequest(HttpRequest httpRequest) {
        Request request = new Request();
        request.method = httpRequest.getMethod();
        request.path = httpRequest.getPath();
        request.headers = httpRequest.getHeaders();
        request.body = httpRequest.getBody();
        request.queryParams = httpRequest.getQueryParams();
        return request;
    }

    /**
     * Parse request từ JSON string
     * Format: {"method": "GET", "path": "/api/medicines", "headers": {}, "body": {}, "query": {}}
     */
    public static Request parse(String jsonString) {
        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            Request request = new Request();
            
            request.method = json.has("method") ? json.get("method").getAsString() : "GET";
            request.path = json.has("path") ? json.get("path").getAsString() : "/";
            
            if (json.has("headers") && json.get("headers").isJsonObject()) {
                JsonObject headersObj = json.getAsJsonObject("headers");
                headersObj.entrySet().forEach(entry -> {
                    request.headers.put(entry.getKey(), entry.getValue().getAsString());
                });
            }
            
            if (json.has("body") && json.get("body").isJsonObject()) {
                request.body = json.getAsJsonObject("body");
            }
            
            if (json.has("query") && json.get("query").isJsonObject()) {
                JsonObject queryObj = json.getAsJsonObject("query");
                queryObj.entrySet().forEach(entry -> {
                    request.queryParams.put(entry.getKey(), entry.getValue().getAsString());
                });
            }
            
            return request;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid request format: " + e.getMessage(), e);
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public JsonObject getBody() {
        return body;
    }

    public <T> T getBodyAs(Class<T> clazz) {
        if (body == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.fromJson(body, clazz);
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public String getQueryParam(String key) {
        return queryParams.get(key);
    }

    public String getQueryParam(String key, String defaultValue) {
        return queryParams.getOrDefault(key, defaultValue);
    }
}

