package com.jt.flash.proxy.service;

/**
 * since 2016/6/25.
 */
public interface Lock {

    boolean lock();

    void unlock();
}
