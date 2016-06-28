package com.jt.flash.proxy.handler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import com.jt.flash.proxy.bean.ProxyContext;
import com.jt.flash.proxy.config.WebConfig;

import javax.servlet.ServletException;

/**
 * since 2016/6/17.
 */
public class SpringMvcInit extends ChannelInitializer<SocketChannel> {

    private final DispatcherServlet dispatcherServlet;

    //private ConfigurableApplicationContext applicationContext;

    public SpringMvcInit(ProxyContext proxyContext, ConfigurableApplicationContext applicationContext) throws ServletException {
        MockServletContext servletContext = new MockServletContext();
        MockServletConfig servletConfig = new MockServletConfig(servletContext);

        //this.applicationContext = applicationContext;

        AnnotationConfigWebApplicationContext wac = new AnnotationConfigWebApplicationContext();
        wac.setParent(applicationContext);
        wac.setServletContext(servletContext);
        wac.setServletConfig(servletConfig);
        wac.register(WebConfig.class);
        wac.refresh();

        this.dispatcherServlet = new DispatcherServlet(wac);
        this.dispatcherServlet.init(servletConfig);
        //this.dispatcherServlet.getServletContext().setAttribute("proxyConfig", proxyContext);
        //this.dispatcherServlet.getServletContext().setAttribute("flashApplicationContext", applicationContext);
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // Create a default pipeline implementation.
        ChannelPipeline pipeline = ch.pipeline();

        // Uncomment the following line if you want HTTPS
        //SSLEngine engine = SecureChatSslContextFactory.getServerContext().createSSLEngine();
        //engine.setUseClientMode(false);
        //pipeline.addLast("ssl", new SslHandler(engine));

        //ch.pipeline().addLast(
        //        new LoggingHandler(LogLevel.DEBUG),
        //        new HttpServerCodec(),
        //        new HexDumpProxyFrontendHandler(null));

        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
        pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("gzip", new HttpContentCompressor());

        pipeline.addLast("handler", new ServletHandler(this.dispatcherServlet));
    }
}
