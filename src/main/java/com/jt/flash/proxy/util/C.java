package com.jt.flash.proxy.util;

import org.apache.commons.lang.math.NumberUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Properties;

/**
 * since 2016/6/25.
 */
public class C {

    public static final String app_name = "flash";
    public static final String name = "flash.qunar.com";
    public static final String root_path = "/upstreams";
    public static final String config_path = "/upstreams/config";
    public static final String name_parent_path = root_path + "/names";
    public static final String name_path = name_parent_path + "/" + name;
    public static final String lock_path = "/lock";
    public static final int ssl_port = 443;


    private static Properties properties = new Properties();

    static {
        try {
            properties.load(C.class.getClassLoader().getResourceAsStream("app.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getValue(String key) {
        return properties.getProperty(key);
    }

    public static int getInt(String key) {
        return NumberUtils.toInt(getValue(key));
    }

    public static String getIp() {
        try {
            Enumeration<NetworkInterface> interfaceList = NetworkInterface.getNetworkInterfaces();
            while (interfaceList.hasMoreElements()) {
                NetworkInterface networkInterface = interfaceList.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress address = inetAddresses.nextElement();
                    String hostAddress = address.getHostAddress();
                    if (hostAddress.startsWith("10")) {
                        return hostAddress;
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
