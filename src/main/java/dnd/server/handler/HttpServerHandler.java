package dnd.server.handler;

import dnd.server.db.DbManager;
import dnd.server.api.ApiRouter;
import dnd.server.api.HttpRequest;
import dnd.server.api.HttpResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.logging.Logger;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * HTTP handler để xử lý các HTTP requests
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = Logger.getLogger(HttpServerHandler.class.getName());
    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    
    private final DbManager dbManager;
    private final ApiRouter apiRouter;

    public HttpServerHandler(DbManager dbManager) {
        this.dbManager = dbManager;
        this.apiRouter = new ApiRouter(dbManager);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // Chỉ xử lý HTTP GET và POST
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        // CORS headers (nếu cần)
        if (request.method() == HttpMethod.OPTIONS) {
            sendCorsResponse(ctx);
            return;
        }

        try {
            // Parse HTTP request thành HttpRequest object
            HttpRequest httpRequest = HttpRequest.fromNettyRequest(request);
            logger.info("Nhận request: " + httpRequest.getMethod() + " " + httpRequest.getPath());

            // Xử lý request qua ApiRouter
            HttpResponse httpResponse = apiRouter.handle(httpRequest);

            // Gửi response
            sendResponse(ctx, httpResponse);
        } catch (Exception e) {
            logger.severe("Lỗi khi xử lý request: " + e.getMessage());
            e.printStackTrace();
            sendError(ctx, INTERNAL_SERVER_ERROR, "Internal Server Error: " + e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warning("Exception caught: " + cause.getMessage());
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Gửi HTTP response
     */
    private void sendResponse(ChannelHandlerContext ctx, HttpResponse httpResponse) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1,
                HttpResponseStatus.valueOf(httpResponse.getStatusCode()),
                Unpooled.copiedBuffer(httpResponse.getBody(), CharsetUtil.UTF_8)
        );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, CONTENT_TYPE_JSON);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        // CORS headers
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");

        ctx.writeAndFlush(response);
    }

    /**
     * Gửi error response
     */
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        sendError(ctx, status, status.reasonPhrase());
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        HttpResponse httpResponse = HttpResponse.error(status.code(), message);
        sendResponse(ctx, httpResponse);
    }

    /**
     * Gửi CORS preflight response
     */
    private void sendCorsResponse(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        ctx.writeAndFlush(response);
    }
}

