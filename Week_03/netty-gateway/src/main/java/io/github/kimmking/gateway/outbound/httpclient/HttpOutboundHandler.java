package io.github.kimmking.gateway.outbound.httpclient;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpOutboundHandler {
    private static Logger logger = LoggerFactory.getLogger(HttpOutboundHandler.class);

    private static PoolingHttpClientConnectionManager connectionManager;
    private static CloseableHttpClient httpClient;
    private final static int CONNECT_TIMEOUT = 5000;
    private final static RequestConfig requestConfig;

    static {
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(500);
        connectionManager.setDefaultMaxPerRoute(50);
        requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(CONNECT_TIMEOUT).build();

        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    private String getBackendUrl(String server) {
        return server.endsWith("/") ? server.substring(0, server.length() - 1) : server;
    }

    public void handle(final FullHttpRequest fullRequest, final ChannelHandlerContext ctx, String server) {
        final String url = getBackendUrl(server) + fullRequest.uri();
        try {
            logger.info("fetchGet url={}", url);
            this.fetchGet(fullRequest, ctx, url);
        } catch (Exception e) {
            logger.error("fetchGet error", e);
        }
    }

    private void fetchGet(final FullHttpRequest inbound, final ChannelHandlerContext ctx, final String url) {
        final HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_KEEP_ALIVE);
        try {
            CloseableHttpResponse response = httpClient.execute(httpGet);
            String content = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            FullHttpResponse fullResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(content.getBytes(StandardCharsets.UTF_8)));
            fullResponse.headers().set("Content-Type", response.getEntity().getContentType());
            fullResponse.headers().set("Content-Length", response.getEntity().getContentLength());
            if (inbound != null) {
                if (!HttpUtil.isKeepAlive(inbound)) {
                    ctx.write(fullResponse).addListener(ChannelFutureListener.CLOSE);
                } else {
                    fullResponse.headers().set(CONNECTION, KEEP_ALIVE);
                    ctx.write(fullResponse);
                }
            }
        } catch (Exception e) {
            logger.error("httpClient request error", e);
        }
    }
}
