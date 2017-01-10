package com.baojie.channelmanager.client.watchdog;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baojie.channelmanager.client.channelgroup.YunChannelGroup;
import com.baojie.channelmanager.util.YunThreadFactory;

@Sharable
public abstract class ConnectionWatchdog extends ChannelInboundHandlerAdapter implements ChannelHandlerHolder {//implements ChannelHandlerHolder
	
	private static final ScheduledThreadPoolExecutor Reconnect_ThreadPoolExecutor = new ScheduledThreadPoolExecutor(32,YunThreadFactory.create("ReconnectRunner"));
	private static final LinkedBlockingQueue<Future<?>> Futures_ForReconnect = new LinkedBlockingQueue<>(512);
	private static final Logger log = LoggerFactory.getLogger(ConnectionWatchdog.class);
	private final ReentrantReadWriteLock readWriteLock;
	private final YunChannelGroup yunChannelGroup;
	private final HostAndPort hostAndPort;
	private final Bootstrap bootstrap;
	public ConnectionWatchdog(final Bootstrap bootstrap, final HostAndPort hostAndPort,
			final ReentrantReadWriteLock readWriteLock, final YunChannelGroup yunChannelGroup) {
		this.bootstrap = bootstrap;
		this.hostAndPort = hostAndPort;
		this.readWriteLock = readWriteLock;
		this.yunChannelGroup = yunChannelGroup;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("当前链路已经激活了");
		ctx.fireChannelActive();
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.info("链接关闭");
		closeChannelAndFire(ctx);
		yunChannelGroup.setInactive();
		doReconnect(ctx);
		log.info("链接关闭，将进行重连");
	}

	private void closeChannelAndFire(final ChannelHandlerContext ctx) {
		ctx.channel().close();
		ctx.fireChannelInactive();
	}

	private void doReconnect(final ChannelHandlerContext ctx) {
		ReconnectRunner reconnectRunner = ReconnectRunner.create(buildNettyHolder(ctx),buildExecuteHolder(),readWriteLock,yunChannelGroup);
		Future<?> futureReconnect = null;
		futureReconnect = Reconnect_ThreadPoolExecutor.scheduleWithFixedDelay(reconnectRunner, 1, 3, TimeUnit.SECONDS);
		Futures_ForReconnect.offer(futureReconnect);
	}

	private NettyHolder buildNettyHolder(final ChannelHandlerContext ctx){
		return NettyHolder.create(bootstrap, hostAndPort, ctx);
	}
	
	private ExecuteHolder buildExecuteHolder(){
		return ExecuteHolder.create(Reconnect_ThreadPoolExecutor, Futures_ForReconnect);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.channel().close();
		cause.printStackTrace();
		log.error("管道channel出现异常！！！");
	}

}
