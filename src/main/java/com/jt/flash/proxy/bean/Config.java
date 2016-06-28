package com.jt.flash.proxy.bean;

/**
 * since 2016/6/19.
 */
public class Config extends BaseBean {

    private Location[] locations;
    private Upstream[] upstreams;
    private String gateway;

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public Location[] getLocations() {
        return locations;
    }

    public void setLocations(Location[] locations) {
        this.locations = locations;
    }

    public Upstream[] getUpstreams() {
        return upstreams;
    }

    public void setUpstreams(Upstream[] upstreams) {
        this.upstreams = upstreams;
    }
}
