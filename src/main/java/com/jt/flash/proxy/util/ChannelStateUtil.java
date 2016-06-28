package com.jt.flash.proxy.util;

import io.netty.channel.Channel;

public class ChannelStateUtil {
	public static String watch(Channel c){
		if(c==null){return null;};
		String str=c.toString();
		//
		str+="|O|"+c.isOpen();
		str+="|A|"+c.isActive();
		str+="|R|"+c.isRegistered();
		str+="|W|"+c.isWritable();
		return str;
	}
}
