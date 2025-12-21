package dnd.server.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP Request wrapper
 */
public class HttpRequest {
    private String method;
    private String path;
    private Map<String, String> queryParams;
    private Map<String, String> headers;
    private JsonObject body;

    private HttpRequest() {
        this.queryParams = new HashMap<>();
        this.headers = new HashMap<>();
    }

    /**
     * Parse từ Netty FullHttpRequest
     */
    public static HttpRequest fromNettyRequest(FullHttpRequest nettyRequest) {
        HttpRequest request = new HttpRequest();
        
        // Method
        request.method = nettyRequest.method().name();
        
        // Path và query params
        QueryStringDecoder decoder = new QueryStringDecoder(nettyRequest.uri());
        request.path = decoder.path();
        
        // Query parameters
        decoder.parameters().forEach((key, values) -> {
            if (!values.isEmpty()) {
                request.queryParams.put(key, values.get(0));
            }
        });
        
        // Headers
        nettyRequest.headers().forEach(entry -> {
            request.headers.put(entry.getKey().toLowerCase(), entry.getValue());
        });
        
        // Body (nếu có)
        if (nettyRequest.content().readableBytes() > 0) {
            String bodyString = nettyRequest.content().toString(StandardCharsets.UTF_8);
            if (!bodyString.isEmpty()) {
                try {
                    request.body = JsonParser.parseString(bodyString).getAsJsonObject();
                } catch (Exception e) {
                    // Nếu không parse được JSON, để null
                    request.body = null;
                }
            }
        }
        
        return request;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
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

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String key) {
        return headers.get(key.toLowerCase());
    }

    public JsonObject getBody() {
        return body;
    }

    public <T> T getBodyAs(Class<T> clazz) {
        if (body == null) {
            return null;
        }
        com.google.gson.Gson gson = new com.google.gson.Gson();
        return gson.fromJson(body, clazz);
    }
}

