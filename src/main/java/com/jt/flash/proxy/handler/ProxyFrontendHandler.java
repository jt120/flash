/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.jt.flash.proxy.handler;

import com.jt.flash.proxy.bean.Location;
import com.jt.flash.proxy.bean.Upstream;
import com.jt.flash.proxy.bean.UpstreamNode;
import com.jt.flash.proxy.service.ConfigService;
import com.jt.flash.proxy.util.C;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.BlockingQueue;


public class ProxyFrontendHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ProxyFrontendHandler.class);

    private String remoteHost;
    private int remotePort;

    private volatile Channel outboundChannel;

    private final SslContext sslCtxForClient;

    public String getRemoteHost() {
        return remoteHost;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public ProxyFrontendHandler(SslContext sslCtxForClient) {
        this.sslCtxForClient = sslCtxForClient;
    }

    private synchronized UpstreamNode getNode(BlockingQueue<UpstreamNode> upstreamNodes) {
        final int size = upstreamNodes.size();
        try {
            int count = 0;
            while (count < size) {
                count++;
                final UpstreamNode take = upstreamNodes.take();
                upstreamNodes.add(take);
                if (take.isHealth()) {
                    log.info("get node {} {}", count, take);
                    return take;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().read();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        final Channel inboundChannel = ctx.channel();
        log.info("read {}", msg);
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            final String uri = request.getUri();
            String host = request.headers().get("Host");
            if (host.contains(":")) {
                host = host.substring(0, host.indexOf(":"));
            }
            final Map<Location, Upstream> mapping = ConfigService.getProxyContext().getMapping();
            for (Map.Entry<Location, Upstream> entry : mapping.entrySet()) {
                if (uri.startsWith(entry.getKey().getUri())) {
                    if ("localhost".equalsIgnoreCase(host) || host.equals(entry.getKey().getHost())) {
                        final BlockingQueue<UpstreamNode> upstreamNodes = entry.getValue().getUpstreamNodes();
                        UpstreamNode choose = getNode(upstreamNodes);
                        if (choose != null) {
                            remoteHost = choose.getIp();
                            remotePort = choose.getPort();
                        }
                    }
                }
            }
            log.info("uri {}, host {} {}, port {}|inboundChannel|{}", uri, host, remoteHost, remotePort,
                    inboundChannel);

            if (remoteHost != null) {
                processRequest(ctx, msg, inboundChannel, remoteHost, remotePort);
            } else {
                log.warn("no upstream|inboundChannel|" + inboundChannel + "|remoteHost|" + remoteHost);
                ctx.close();
            }
        }
    }

    private void processRequest(final ChannelHandlerContext ctx, final Object msg, final Channel inboundChannel,
                                String host, final int port) {
        Bootstrap b = buildBackendBootstrap(ctx, inboundChannel, port);
        ChannelFuture f = b.connect(host, port);
        outboundChannel = f.channel();

        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    log.info("connect ok, write {}", msg);
                    outboundChannel.writeAndFlush(msg);
                            /*
                            .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) {
                            if (future.isSuccess()) {
                                ctx.channel().read();
                            } else {
                                future.channel().close();
                            }
                        }
                    });*/

                } else {
                    log.warn("connect fail");
                    inboundChannel.close();
                }
            }
        });
    }

    private Bootstrap buildBackendBootstrap(ChannelHandlerContext ctx, final Channel inboundChannel, final int port) {
        Bootstrap b = new Bootstrap()
                .group(inboundChannel.eventLoop())
                .channel(ctx.channel().getClass())
                .option(ChannelOption.AUTO_READ, false)
                .handler(new BackendInitChannel(port, inboundChannel));
        return b;
    }

    private class BackendInitChannel extends ChannelInitializer<SocketChannel> {

        private int port;
        private Channel inboundChannel;

        private BackendInitChannel(int port, Channel inboundChannel) {
            this.port = port;
            this.inboundChannel = inboundChannel;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            if (port == C.ssl_port) {
                pipeline.addLast(sslCtxForClient.newHandler(ch.alloc()));
            }
            pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));
            pipeline.addLast(new HttpClientCodec());
            pipeline.addLast(new HttpContentDecompressor());
            pipeline.addLast(new HttpObjectAggregator(1048576));
            pipeline.addLast(new ProxyBackendHandler(inboundChannel));
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
