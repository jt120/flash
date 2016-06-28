package com.jt.flash.proxy.bean;

import java.util.Map;

/**
 * since 2016/6/19.
 */
public class ProxyContext {

    private Config config;
    private Map<Location, Upstream> mapping;

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public Map<Location, Upstream> getMapping() {
        return mapping;
    }

    public void setMapping(Map<Location, Upstream> mapping) {
        this.mapping = mapping;
    }
}
