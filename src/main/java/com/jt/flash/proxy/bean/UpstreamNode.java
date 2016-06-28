package com.jt.flash.proxy.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * since 2016/6/19.
 */
public class UpstreamNode extends BaseBean {

    private String ip;
    private int port;
    @JsonIgnore
    private boolean health = true;

    public boolean isHealth() {
        return health;
    }

    public void setHealth(boolean health) {
        this.health = health;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UpstreamNode node = (UpstreamNode) o;

        if (port != node.port) return false;
        if (ip != null ? !ip.equals(node.ip) : node.ip != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }
}
