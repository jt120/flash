package com.jt.flash.proxy.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * since 2016/6/17.
 */
public class ServletHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger log = LoggerFactory.getLogger(ServletHandler.class);


    private final Servlet servlet;

    private final ServletContext servletContext;

    public ServletHandler(Servlet servlet) {
        this.servlet = servlet;
        this.servletContext = servlet.getServletConfig().getServletContext();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        //log.info("servlet req {}", msg);
        MockHttpServletRequest req = createServletRequest(msg);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        this.servlet.service(req, resp);
        //log.info("servlet resp {} {}", resp.getStatus(), resp.getContentAsString());
        boolean keepAlive = HttpHeaders.isKeepAlive(msg);

        HttpResponse nettyResp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(resp
                .getStatus()));

        for (String name : resp.getHeaderNames()) {
            for (Object obj : resp.getHeaderValues(name)) {
                nettyResp.headers().add(name, obj);
                //log.info("add header {} {}", name, obj);
            }
        }

        ctx.write(nettyResp);

        InputStream contentStream = new ByteArrayInputStream(resp.getContentAsByteArray());

        // Write the content and flush it.

        //if (!keepAlive) {
        //    log.info("not keep alive, close channel");
        ChannelFuture writeFuture = ctx.writeAndFlush(new ChunkedStream(contentStream));
        writeFuture.addListener(ChannelFutureListener.CLOSE);
        //} else {
        //    nettyResp.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        //    ctx.writeAndFlush(new ChunkedStream(contentStream));
        //}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        ByteBuf content = Unpooled.copiedBuffer(
                "Failure: " + status.toString() + "\r\n",
                CharsetUtil.UTF_8);

        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(
                HTTP_1_1,
                status,
                content
        );
        fullHttpResponse.headers().add(CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.write(fullHttpResponse).addListener(ChannelFutureListener.CLOSE);
    }

    private MockHttpServletRequest createServletRequest(FullHttpRequest fullHttpRequest) {
        UriComponents uriComponents = UriComponentsBuilder.fromUriString(fullHttpRequest.getUri()).build();

        MockHttpServletRequest servletRequest = new MockHttpServletRequest(this.servletContext);
        servletRequest.setRequestURI(uriComponents.getPath());
        servletRequest.setPathInfo(uriComponents.getPath());
        servletRequest.setMethod(fullHttpRequest.getMethod().name());

        if (uriComponents.getScheme() != null) {
            servletRequest.setScheme(uriComponents.getScheme());
        }
        if (uriComponents.getHost() != null) {
            servletRequest.setServerName(uriComponents.getHost());
        }
        if (uriComponents.getPort() != -1) {
            servletRequest.setServerPort(uriComponents.getPort());
        }

        boolean isJson = false;
        for (String name : fullHttpRequest.headers().names()) {
            if (name.equals("Content-Type")) {
                isJson = fullHttpRequest.headers().get(name).equals("application/json");
            }
            servletRequest.addHeader(name, fullHttpRequest.headers().get(name));
        }

//		ByteBuf bbContent = fullHttpRequest.content();
//		if(bbContent.hasArray()) {
//			byte[] baContent = bbContent.array();
//			servletRequest.setContent(baContent);
//		}

        // 将post请求的参数，添加到HttpServletRrequest的parameter
        ByteBuf buf = fullHttpRequest.content();
        try {
            int readable = buf.readableBytes();
            byte[] bytes = new byte[readable];
            buf.readBytes(bytes);
            String contentStr = UriUtils.decode(new String(bytes, "UTF-8"), "UTF-8");
            //log.info("content {}", contentStr);
            if (isJson) {
                servletRequest.setContent(contentStr.getBytes());
            } else {
                for (String params : contentStr.split("&")) {
                    String[] para = params.split("=");
                    if (para.length > 1) {
                        servletRequest.addParameter(para[0], para[1]);
                    } else {
                        servletRequest.addParameter(para[0], "");
                    }
                }
            }

        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            ReferenceCountUtil.release(buf);
        }

        try {
            if (uriComponents.getQuery() != null) {
                String query = UriUtils.decode(uriComponents.getQuery(), "UTF-8");
                servletRequest.setQueryString(query);
            }

            for (Map.Entry<String, List<String>> entry : uriComponents.getQueryParams().entrySet()) {
                for (String value : entry.getValue()) {
                    servletRequest.addParameter(
                            UriUtils.decode(entry.getKey(), "UTF-8"),
                            UriUtils.decode(value, "UTF-8"));
                }
            }
        } catch (UnsupportedEncodingException ex) {
            // shouldn't happen
        }

        return servletRequest;
    }
}
