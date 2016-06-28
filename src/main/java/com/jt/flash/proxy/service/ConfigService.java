package com.jt.flash.proxy.service;

import com.jt.flash.proxy.bean.Config;
import com.jt.flash.proxy.bean.Location;
import com.jt.flash.proxy.bean.ProxyContext;
import com.jt.flash.proxy.bean.Upstream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * since 2016/6/19.
 */
public class ConfigService {

    private static final Logger log = LoggerFactory.getLogger(ConfigService.class);


    private static ProxyContext proxyContext = new ProxyContext();

    public static ProxyContext getProxyContext() {
        return proxyContext;
    }

    public static void setProxyContext(ProxyContext proxyContext) {
        ConfigService.proxyContext = proxyContext;
    }

    public static void reload(Config config) {
        if (config == null) {
            log.warn("config is null");
        }
        Map<Location, Upstream> map = new HashMap<Location, Upstream>();
        Set<Upstream> upstreamSet = new HashSet<Upstream>();
        for (Location location : config.getLocations()) {
            for (Upstream upstream : config.getUpstreams()) {
                if (location.getUpstreamName().equals(upstream.getName())) {
                    map.put(location, upstream);
                }
            }
        }

        for (Upstream upstream : config.getUpstreams()) {
            upstreamSet.add(upstream);
        }
        proxyContext.setMapping(map);
        proxyContext.setConfig(config);
    }
}
