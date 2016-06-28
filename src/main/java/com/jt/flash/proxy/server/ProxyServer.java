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
package com.jt.flash.proxy.server;

import com.jt.flash.proxy.dao.ConfigDao;
import com.jt.flash.proxy.handler.ProxyInitializer;
import com.jt.flash.proxy.service.ConfigService;
import com.jt.flash.proxy.util.CloseUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public final class ProxyServer implements InitializingBean, DisposableBean, Runnable {

    private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);

    @Value("${upstreamPort}")
    private int upstreamPort;

    private EventLoopGroup proxyServerBossGroup;
    private EventLoopGroup proxyServerWorkerGroup;
    private Channel channel;

    @Resource
    private ConfigDao configDao;

    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    public void start() {
        try {
            log.info("Proxying server start at port {}", upstreamPort);
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
            ConfigService.reload(configDao.loadLastest());
            ServerBootstrap b = new ServerBootstrap();
            channel = b.group(proxyServerBossGroup, proxyServerWorkerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.AUTO_READ, false)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ProxyInitializer(sslCtx))
                    .bind(upstreamPort).channel();
        } catch (Exception e) {
            log.warn("start proxy server fail", e);
        }
    }


    public void destroy() throws Exception {
        log.warn("close upstream server");
        CloseUtil.close(channel);
        CloseUtil.close(proxyServerBossGroup);
        CloseUtil.close(proxyServerWorkerGroup);
    }


    public void afterPropertiesSet() throws Exception {
        // Configure the bootstrap.
        proxyServerBossGroup = new NioEventLoopGroup(1);
        proxyServerWorkerGroup = new NioEventLoopGroup();
        taskExecutor.submit(this);
    }


    public void run() {
        start();
    }
}
