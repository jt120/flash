package com.jt.flash.proxy.util;

/**
 * since 2016/6/22.
 */
public class Preconditions {
    public static void checkNotNull(Object o) {
        if (o == null) {
            throw new NullPointerException("is null");
        }
    }
}
