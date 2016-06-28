package com.jt.flash.proxy.service;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

/**
 * since 2016/6/25.
 */
@Service
public class ShutdownService implements ApplicationContextAware {

    private ConfigurableApplicationContext context;

    public String shutdown() {
        if (this.context == null) {
            return "no context";
        }
        try {
            return "bye bye...";
        } finally {
            new Thread(new Runnable() {
                 
                public void run() {
                    try {
                        Thread.sleep(500L);
                    } catch (InterruptedException e) {
                    }
                    ShutdownService.this.context.close();
                }
            }).start();
        }
    }
     
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            this.context = (ConfigurableApplicationContext) applicationContext;
        }
    }
}
