package com.jt.flash.proxy.bean;

/**
 * since 2016/6/19.
 */
public class Location extends BaseBean {

    private String host;
    private String uri;
    private int port;
    private String upstreamName;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getUpstreamName() {
        return upstreamName;
    }

    public void setUpstreamName(String upstreamName) {
        this.upstreamName = upstreamName;
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

        Location location = (Location) o;

        if (port != location.port) return false;
        if (!host.equals(location.host)) return false;
        if (!upstreamName.equals(location.upstreamName)) return false;
        if (!uri.equals(location.uri)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + uri.hashCode();
        result = 31 * result + port;
        result = 31 * result + upstreamName.hashCode();
        return result;
    }
}
