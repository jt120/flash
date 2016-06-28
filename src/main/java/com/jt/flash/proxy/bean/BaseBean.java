package com.jt.flash.proxy.bean;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.Serializable;

/**
 * since 2016/6/19.
 */
public class BaseBean implements Serializable {

    private static final long serialVersionUID = 6956082211501833553L;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
