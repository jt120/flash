package com.jt.flash.proxy.util;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;

/**
 * since 2016/6/20.
 */
public class CloseUtil {

    public static void close(EventLoopGroup group) {
        if (group != null) {
            try {
                group.shutdownGracefully();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void close(Channel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
