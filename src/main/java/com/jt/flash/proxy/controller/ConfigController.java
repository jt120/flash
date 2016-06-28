package com.jt.flash.proxy.controller;

import com.jt.flash.proxy.bean.Body;
import com.jt.flash.proxy.bean.Config;
import com.jt.flash.proxy.bean.Location;
import com.jt.flash.proxy.bean.Upstream;
import com.jt.flash.proxy.dao.ConfigDao;
import com.jt.flash.proxy.service.BodyCareService;
import com.jt.flash.proxy.service.ConfigService;
import com.jt.flash.proxy.service.ShutdownService;
import com.jt.flash.proxy.service.ZkService;
import com.jt.flash.proxy.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * since 2016/6/17.
 */
@Controller
@RequestMapping("/config")
public class ConfigController {

    private static final Logger log = LoggerFactory.getLogger(ConfigController.class);

    @Resource
    private ConfigDao configDao;

    @Resource
    private ZkService zkService;

    @Resource
    private BodyCareService bodyCareService;

    @Resource
    private ShutdownService shutdownService;

    @RequestMapping("/show")
    @ResponseBody
    public Object showConfig() {
        Config config = ConfigService.getProxyContext().getConfig();
        if (config == null) {
            config = configDao.loadLastest();
            log.info("load config from db {}", config);
            ConfigService.reload(config);
        }
        return config;
    }

    @RequestMapping(value = "/change", method = RequestMethod.POST)
    @ResponseBody
    public Object changeConfig(@RequestBody Config config) {
        log.info("change config request {}", config);
        Preconditions.checkNotNull(config);
        final Location[] locations = config.getLocations();
        final Upstream[] upstreams = config.getUpstreams();
        Preconditions.checkNotNull(locations);
        Preconditions.checkNotNull(upstreams);
        boolean isOk = false;
        try {
            if (zkService.lock()) {
                int version = configDao.version() + 1;
                configDao.insertConfig(config, version);
                zkService.setConfig(version);
                log.info("change config success {}", version);
                isOk = true;
            }
        } finally {
            zkService.unlock();
        }

        return isOk;
    }

    @RequestMapping(value = "/close")
    @ResponseBody
    public Object closeFlash() {
        log.info("close application");
        return shutdownService.shutdown();
    }

    @RequestMapping(value = "/state")
    @ResponseBody
    public Object state() {
        Body body = bodyCareService.getBody();
        log.info("state {}", body);
        return body;
    }

    @RequestMapping(value = "/reload")
    @ResponseBody
    public Object reload() {
        Config config = configDao.loadLastest();
        ConfigService.reload(config);
        return "ok";
    }
}
