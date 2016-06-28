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

import com.jt.flash.proxy.bean.ProxyContext;
import com.jt.flash.proxy.bean.Upstream;
import com.jt.flash.proxy.bean.UpstreamNode;
import com.jt.flash.proxy.service.ConfigService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpHealthCheckHandler extends SimpleChannelInboundHandler<HttpObject> {
    private static final Logger log = LoggerFactory.getLogger(HttpHealthCheckHandler.class);
    private UpstreamNode upstreamNode;

    public HttpHealthCheckHandler(UpstreamNode upstreamNode) {
        this.upstreamNode = upstreamNode;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
        log.info("health check resp {}", msg);
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;

            final ProxyContext proxyContext = ConfigService.getProxyContext();

            if (response.getStatus().equals(HttpResponseStatus.OK)) {
                for (Upstream upstream : proxyContext.getConfig().getUpstreams()) {
                    for (UpstreamNode n : upstream.getUpstreamNodes()) {
                        if (upstreamNode.equals(n)) {
                            log.info("{} is ok", upstream);
                            n.setHealth(true);
                        }
                    }
                }


            } else {
                log.warn("health check fail {}", upstreamNode);
                for (Upstream upstream : proxyContext.getConfig().getUpstreams()) {
                    for (UpstreamNode n : upstream.getUpstreamNodes()) {
                        if (upstreamNode.equals(n)) {
                            log.info("{} before", upstream);
                            n.setHealth(false);
                            log.info("{} change", upstream);
                        }

                    }
                }
            }

            ctx.close();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
