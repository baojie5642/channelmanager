package com.baojie.channelmanager.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

public class CheckNull {

	private static final Logger log = LoggerFactory.getLogger(CheckNull.class);

	private CheckNull() {

	}

	public static void checkNull(final Object object) {
		if (null == object) {
			log.error("被检查的object为null，之后会抛出空指针异常，这个错误很严重，请检查……！！！");
			throw new NullPointerException("object is null.");
		}
	}

	public static void checkChannelNull(final Channel channel) {
		if (null == channel) {
			log.error("被检查的channel为null，之后会抛出空指针异常，这个错误很严重，请检查……！！！");
			throw new NullPointerException("channel must not be null.");
		}
	}

	public static void checkChannelFutureNull(final ChannelFuture channelFuture) {
		if (null == channelFuture) {
			log.error("被检查的channelFuture为null，之后会抛出空指针异常，这个错误很严重，请检查……！！！");
			throw new NullPointerException("channelFuture must not be null.");
		}
	}
	
	public static void checkBytesNull(final byte[] bytes){
		if(null==bytes){
			log.error("被检查的byte[] bytes为null，之后会抛出空指针异常，这个错误很严重，请检查……！！！");
			throw new NullPointerException("byte[] bytes must not be null.");
		}
	}
	
	public static void checkStringNull(final String string ){
		if(null==string){
			log.error("被检查的string为null，之后会抛出空指针异常，这个错误很严重，请检查……！！！");
			throw new NullPointerException("string must not be null.");
		}
	}

}
