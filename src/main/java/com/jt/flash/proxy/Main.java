package com.jt.flash.proxy;

import com.jt.flash.proxy.config.MainConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * since 2016/6/20.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /**
     * 1. init mysql
     * 2. init zk
     * 3. run main
     * 4. visit http://localhost:8081/web/index.html
     * 5. change config and check
     * @param args
     */
    public static void main(String[] args) {
        log.info("start flash upstream");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(MainConfig.class);
        context.start();
    }
}
