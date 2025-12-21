package dnd.server.endpoints;

import dnd.server.api.Request;
import dnd.server.api.Response;

/**
 * Interface cho các endpoint handlers
 */
public interface EndpointHandler {
    /**
     * Xử lý request và trả về response
     */
    Response handle(Request request) throws Exception;
}

