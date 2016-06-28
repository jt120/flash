package com.jt.flash.proxy.bean;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * since 2016/6/19.
 */
public class Upstream extends BaseBean {

    private String name;
    private String healthCheck = "/hello";
 
    private LinkedBlockingQueue<UpstreamNode> upstreamNodes = new LinkedBlockingQueue<UpstreamNode>();
 

    public BlockingQueue<UpstreamNode> getUpstreamNodes() {
        return upstreamNodes;
    }

    public String getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(String healthCheck) {
        this.healthCheck = healthCheck;
    }

    public void setUpstreamNodes(LinkedBlockingQueue<UpstreamNode> upstreamNodes) {
        this.upstreamNodes = upstreamNodes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Upstream upstream = (Upstream) o;

        if (!name.equals(upstream.name)) return false;
        if (!upstreamNodes.equals(upstream.upstreamNodes)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + upstreamNodes.hashCode();
        return result;
    }
}
