package com.baojie.channelmanager.client.watchdog;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

public class NettyHolder {

	private final Bootstrap bootstrap;
	private final HostAndPort hostAndPort;
	private final ChannelHandlerContext channelHandlerContext;
 	
	private NettyHolder(final Bootstrap bootstrap,final HostAndPort hostAndPort,final ChannelHandlerContext channelHandlerContext){
		this.bootstrap=bootstrap;
		this.hostAndPort=hostAndPort;
		this.channelHandlerContext=channelHandlerContext;
	}
	
	public static NettyHolder create(final Bootstrap bootstrap,final HostAndPort hostAndPort,final ChannelHandlerContext channelHandlerContext){
		return new NettyHolder(bootstrap, hostAndPort,channelHandlerContext);
	}

	public Bootstrap getBootstrap() {
		return bootstrap;
	}

	public HostAndPort getHostAndPort() {
		return hostAndPort;
	}

	public ChannelHandlerContext getChannelHandlerContext() {
		return channelHandlerContext;
	}

}
