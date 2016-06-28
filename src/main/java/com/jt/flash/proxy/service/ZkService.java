package com.jt.flash.proxy.service;

import com.jt.flash.proxy.bean.Body;
import com.jt.flash.proxy.bean.Config;
import com.jt.flash.proxy.dao.ConfigDao;
import com.jt.flash.proxy.util.C;
import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * since 2016/6/25.
 */
@Service
public class ZkService implements Watcher, Lock, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ZkService.class);

    @Resource
    private BodyCareService bodyCareService;

    @Resource
    private ConfigDao configDao;


    @Value("${zk.ip}")
    private String zkIp;
    @Value("${zk.port}")
    private int zkPort;

    private ZooKeeper zk;

    private int sessionTimeout = 5000;


    private volatile boolean stop;

    public void setBodyCareService(BodyCareService bodyCareService) {
        this.bodyCareService = bodyCareService;
    }

    public void setZkIp(String zkIp) {
        this.zkIp = zkIp;
    }

    public void setZkPort(int zkPort) {
        this.zkPort = zkPort;
    }

    public boolean isStop() {
        return stop;
    }

    public void bootstrap() {
        createParent(C.root_path, new byte[0]);
        createParent(C.config_path, new byte[0]);
        createParent(C.name_parent_path, new byte[0]);
        createParent(C.name_path, new byte[0]);

        try {
            zk.getData(C.config_path, configChangeWather, new Stat());
        } catch (Exception e) {
            log.warn("config load fail", e);
        }
        registerUpstream();
    }

     
    public void process(WatchedEvent e) {
        log.info("zk server got event: {}", e);
        if (e.getType() == Event.EventType.None) {
            switch (e.getState()) {
                case SyncConnected:
                    log.info("sync connected");
                    break;
                case Disconnected:
                    stop = true;
                    log.info("disconnected");
                    break;
                case Expired:
                    stop = true;
                    log.error("Session expiration");
                default:
                    break;
            }
        }
    }

    private String buildPath() {
        return C.name_path + "/" + C.getIp();
    }

    /**
     * Registering the new worker, which consists of adding a worker
     * znode to /workers.
     */
    public void registerUpstream() {
        String path = buildPath();
        log.info("reg zk address {}", path);
        zk.create(path,
                upstreamState(bodyCareService.getBody()),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL,
                upstreamCallback, null);
    }

    private AsyncCallback.StringCallback upstreamCallback = new AsyncCallback.StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            log.info("upstream callback {}", KeeperException.Code.get(rc));
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    registerUpstream();
                    break;
                case OK:
                    log.info("register upstream ok {}", path);
                    break;
                case NODEEXISTS:
                    log.warn("no need register upstream {}", path);
                    break;
                default:
                    log.error("register upstream fail",
                            KeeperException.create(KeeperException.Code.get(rc), path));
            }
        }
    };

    private byte[] upstreamState(Body body) {
        return (body.red + ":" + body.blue).getBytes();
    }

    @Scheduled(fixedRate = 5000)
    public void updateState() {
        try {
            Body cache = BodyCareService.cache;
            Body body = bodyCareService.getBody();

            if (cache == null) {
                BodyCareService.cache = body;
            } else {
                if (!cache.equals(body)) {
                    BodyCareService.cache = body;
                    zk.setData(buildPath(),
                            upstreamState(body), -1);
                    log.info("update upstream state {}", body);
                }
            }

        } catch (Exception e) {
            log.warn("update state fail", e);
            registerUpstream();
        }
    }


     
    public boolean lock() {
        try {
            zk.create(C.lock_path,
                    null,
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL);
            return true;
        } catch (Exception e) {
            log.warn("lock", e);
        }
        return false;
    }

     
    public void unlock() {
        try {
            zk.delete(C.lock_path, -1);
        } catch (Exception e) {
            log.warn("unlock fail", e);
        }
    }

     
    public void destroy() throws Exception {
        if (zk != null) {
            zk.close();
        }
    }

     
    public void afterPropertiesSet() throws Exception {
        zk = new ZooKeeper(zkIp + ":" + zkPort, sessionTimeout, this);
        bootstrap();
    }

    Watcher configChangeWather = new Watcher() {
        public void process(WatchedEvent e) {
            log.info("config change watcher {}", e);
            if (e.getType() == Event.EventType.NodeDataChanged) {
                Config config = configDao.loadLastest();
                ConfigService.reload(config);
                getVersion();
            }
        }
    };

    public void setConfig(int version) {
        try {
            zk.setData(C.config_path, (version + "").getBytes(), -1);
        } catch (Exception e) {
            log.info("set data fail");
        }
    }


    public void getVersion() {
        try {
            byte[] data = zk.getData(C.config_path, configChangeWather, new Stat());
        } catch (Exception e) {
            log.warn("config load fail", e);
        }
    }


    private void createParent(String path, byte[] data) {
        zk.create(path,
                data,
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.PERSISTENT,
                createParentCallback,
                data);
    }

    private AsyncCallback.StringCallback createParentCallback = new AsyncCallback.StringCallback() {
        public void processResult(int rc, String path, Object ctx, String name) {
            switch (KeeperException.Code.get(rc)) {
                case CONNECTIONLOSS:
                    createParent(path, (byte[]) ctx);
                    break;
                case OK:
                    log.info("{} created", path);
                    break;
                case NODEEXISTS:
                    log.warn("node exist {}", path);
                    break;
                default:
                    log.error("create parent fail",
                            KeeperException.create(KeeperException.Code.get(rc), path));
            }
        }
    };
}
