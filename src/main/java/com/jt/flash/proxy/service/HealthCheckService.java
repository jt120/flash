package com.jt.flash.proxy.service;

import com.jt.flash.proxy.bean.Config;
import com.jt.flash.proxy.bean.Upstream;
import com.jt.flash.proxy.bean.UpstreamNode;
import com.jt.flash.proxy.dao.ConfigDao;
import com.jt.flash.proxy.handler.HealthCheckInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.ClientCookieEncoder;
import io.netty.handler.codec.http.DefaultCookie;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * since 2016/6/19.
 */
@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    @Resource
    private ConfigDao configDao;

    @Scheduled(initialDelay = 1000, fixedRate = 2000)
    public void check() {
        Config config = ConfigService.getProxyContext().getConfig();
        if (config == null) {
            config = configDao.loadLastest();
            ConfigService.reload(config);
        }
        final Upstream[] healtUpstream = config.getUpstreams();
        log.info("health check {}", healtUpstream);
        UpstreamNode chooseNode = null;
        for (Upstream upstream : healtUpstream) {
            for (UpstreamNode node : upstream.getUpstreamNodes()) {
                String host = node.getIp();
                chooseNode = node;
                int port = node.getPort();
                // Configure the client.
                EventLoopGroup group = new NioEventLoopGroup();
                try {
                    Bootstrap b = new Bootstrap();
                    b.group(group)
                            .channel(NioSocketChannel.class)
                            .handler(new HealthCheckInitializer(node));

                    // Make the connection attempt.
                    Channel ch = b.connect(host, port).sync().channel();

                    // Prepare the HTTP request.
                    HttpRequest request = new DefaultFullHttpRequest(
                            HttpVersion.HTTP_1_1, HttpMethod.GET, upstream.getHealthCheck());
                    request.headers().set(HttpHeaders.Names.HOST, host);
                    request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
                    request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);

                    // Set some example cookies.
                    request.headers().set(
                            HttpHeaders.Names.COOKIE,
                            ClientCookieEncoder.encode(
                                    new DefaultCookie("my-cookie", "foo"),
                                    new DefaultCookie("another-cookie", "bar")));

                    // Send the HTTP request.
                    ch.writeAndFlush(request);

                    // Wait for the server to close the connection.
                    ch.closeFuture().sync();
                } catch (Exception e) {
                    log.warn("health check fail {} {}", chooseNode, e.getMessage());
                    if (chooseNode != null) {
                        chooseNode.setHealth(false);
                    }
                } finally {
                    // Shut down executor threads to exit.
                    group.shutdownGracefully();
                }

            }
        }
    }

}
