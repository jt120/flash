package com.jt.flash.proxy.server;

import com.jt.flash.proxy.handler.SpringMvcInit;
import com.jt.flash.proxy.service.ConfigService;
import com.jt.flash.proxy.util.CloseUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * since 2016/6/20.
 */
@Service
public class ConfigServer implements Runnable, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ConfigServer.class);
    @Value("${configPort}")
    private int configPort;
    private EventLoopGroup configServerBossGroup;
    private EventLoopGroup configServerWorkerGroup;

    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    @Resource
    private ConfigurableApplicationContext applicationContext;

    private Channel channel;


    private void start() {
        try {
            log.info("start config server at port {}", configPort);
            ServerBootstrap config = new ServerBootstrap();
            channel = config.group(configServerBossGroup, configServerWorkerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new SpringMvcInit(ConfigService.getProxyContext(), applicationContext))
                    .bind(configPort)
                    .channel();
        } catch (Exception e) {
            log.warn("config thread start fail");
        }
    }

     
    public void destroy() throws Exception {
        log.warn("close config server...");
        CloseUtil.close(channel);
        CloseUtil.close(configServerBossGroup);
        CloseUtil.close(configServerWorkerGroup);
    }

     
    public void afterPropertiesSet() throws Exception {
        configServerBossGroup = new NioEventLoopGroup(1);
        configServerWorkerGroup = new NioEventLoopGroup();
        taskExecutor.submit(this);

    }

     
    public void run() {
        start();
    }
}
