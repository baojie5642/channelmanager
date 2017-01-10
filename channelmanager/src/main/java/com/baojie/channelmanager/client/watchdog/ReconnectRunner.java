package com.baojie.channelmanager.client.watchdog;

import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.baojie.channelmanager.client.channelgroup.YunChannelGroup;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

public class ReconnectRunner implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(ReconnectRunner.class);
	private final ReentrantReadWriteLock readWriteLockInRunner;
	private final YunChannelGroup yunChannelGroup;
	private final ExecuteHolder executeHolder;
	private final NettyHolder nettyHolder;

	private ReconnectRunner(final NettyHolder nettyHolder, final ExecuteHolder executeHolder,
			final ReentrantReadWriteLock readWriteLockInRunner, final YunChannelGroup yunChannelGroup) {
		this.nettyHolder = nettyHolder;
		this.executeHolder = executeHolder;
		this.readWriteLockInRunner = readWriteLockInRunner;
		this.yunChannelGroup = yunChannelGroup;
	}

	public static ReconnectRunner create(final NettyHolder nettyHolder, final ExecuteHolder executeHolder,
			final ReentrantReadWriteLock readWriteLockInRunner, final YunChannelGroup yunChannelGroup) {
		return new ReconnectRunner(nettyHolder, executeHolder, readWriteLockInRunner, yunChannelGroup);
	}

	@Override
	public void run() {
		final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = executeHolder.getScheduledThreadPoolExecutor();
		final String threadName = Thread.currentThread().getName();
		if (canReturn(scheduledThreadPoolExecutor)) {
			return;
		} else {
			doReconnect(threadName, scheduledThreadPoolExecutor);
		}
	}

	private boolean canReturn(final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
		final boolean scheduledShutDown = scheduledThreadExecutorShutDown(scheduledThreadPoolExecutor);
		final boolean channelHasActive = isChannelsHasCreated(scheduledThreadPoolExecutor);
		if (scheduledShutDown || channelHasActive) {
			return true;
		} else {
			return false;
		}
	}

	private boolean scheduledThreadExecutorShutDown(final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
		boolean scheduledShutDown = false;
		if (scheduledThreadPoolExecutor.isShutdown()) {
			log.error("定时重连线程池被异常终结，请检查……！！！");
			stopAllScheduledTask();
			scheduledShutDown = true;
		} else {
			scheduledShutDown = false;
		}
		return scheduledShutDown;
	}

	private boolean isChannelsHasCreated(final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
		boolean channelsHasActive = false;
		final ReentrantReadWriteLock readWriteLock = readWriteLockInRunner;
		final String threadName = Thread.currentThread().getName();
		readWriteLock.writeLock().lock();
		try {
			if (yunChannelGroup.activeState()) {
				stopAllScheduledTask();
				log.info("线程 " + threadName + ",执行重连时发现channels已经被其他线程重建，销毁本次定时任务。已经销毁其他全部定时任务。");
				channelsHasActive = true;
			} else {
				channelsHasActive = false;
			}
		} finally {
			readWriteLock.writeLock().unlock();
		}
		return channelsHasActive;
	}

	private void stopAllScheduledTask() {
		cancleRunners();
		cleanWorkQueueInScheduledThreadPoolExecutor();
	}

	private void cancleRunners() {
		final LinkedBlockingQueue<Future<?>> futuresQueue = executeHolder.getLinkedBlockingQueue();
		Future<?> future = null;
		while (!futuresQueue.isEmpty()) {
			future = futuresQueue.poll();
			if (null != future) {
				future.cancel(true);
			}
		}
	}

	private void cleanWorkQueueInScheduledThreadPoolExecutor() {
		final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = executeHolder.getScheduledThreadPoolExecutor();
		scheduledThreadPoolExecutor.purge();
		scheduledThreadPoolExecutor.remove(this);
	}

	private void doReconnect(final String threadName, final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
		final ReentrantReadWriteLock readWriteLock = readWriteLockInRunner;
		readWriteLock.writeLock().lock();
		try {
			reconnectAfterChannelFutureDone(threadName, scheduledThreadPoolExecutor);
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	private void reconnectAfterChannelFutureDone(final String threadName,
			final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
		ChannelFuture newChannelFuture = null;
		newChannelFuture = getAndWaitForFutureDone();// 这里会获取锁后去等待，因为发送消息是也会获取锁，会造成重连时消息发送的延迟
		if (newChannelFuture.isDone() && newChannelFuture.isSuccess()) {
			channelFutureDoneThenDoThis(newChannelFuture, threadName, scheduledThreadPoolExecutor);
		} else {
			log.info("线程 " + threadName + ",本次重连获取的channelFuture失败。");
			return;
		}
	}

	private void channelFutureDoneThenDoThis(final ChannelFuture newChannelFuture, final String threadName,
			final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
		log.info("本次线程" + threadName + "执行重连时获取的channelFuture成功。");
		if (!yunChannelGroup.activeState()) {
			channelInactiveDo(newChannelFuture, threadName);
		} else {
			stopAllScheduledTask();
			log.info("线程 " + threadName + ",执行重连时发现channels已经被重新初始化，销毁本次定时重连,取消其他全部定时。");
		}
	}

	private ChannelFuture getAndWaitForFutureDone() {
		final ChannelFuture newChannelFuture = getFuture();
		try {
			newChannelFuture.awaitUninterruptibly();
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			log.error("执行channelFuture.awaitUninterruptibly()时出错，请检查……！！！");
		}
		return newChannelFuture;
	}

	private ChannelFuture getFuture() {
		final Bootstrap bootstrap = nettyHolder.getBootstrap();
		ChannelFuture channelFuture = null;
		synchronized (bootstrap) {
			channelFuture = onlyGetChannelFuture(bootstrap);
		}
		return channelFuture;
	}

	private ChannelFuture onlyGetChannelFuture(final Bootstrap bootstrap) {
		final HostAndPort hostAndPort = nettyHolder.getHostAndPort();
		ChannelFuture channelFuture = null;
		channelFuture = bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
		checkChannelFutureNull(channelFuture);
		return channelFuture;
	}

	private void checkChannelFutureNull(final ChannelFuture channelFuture) {
		if (null == channelFuture) {
			throw new NullPointerException();
		}
	}

	private void channelInactiveDo(final ChannelFuture newChannelFuture, final String threadName) {
		final ChannelHandlerContext channelHandlerContext = nettyHolder.getChannelHandlerContext();
		final Channel oldChannel = channelHandlerContext.channel();
		final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = executeHolder.getScheduledThreadPoolExecutor();
		if (yunChannelGroup.isContainThisChannel(oldChannel)) {
			realDoTheWork(newChannelFuture, scheduledThreadPoolExecutor);
			log.info("线程 " + threadName + ",已经将channels全部初始化成功，结束定时重连，已经取消其他全部定时任务。");
		} else {
			stopAllScheduledTask();
			log.error("线程 " + threadName + ",执行重连时发现，当channels还没有被重新创建时却不包含以前的缓存channel，可能出错，请检查……！！！定时任务已经全部取消。");
		}
	}

	private void realDoTheWork(final ChannelFuture channelFuture,
			final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
		final Bootstrap bootstrap = nettyHolder.getBootstrap();
		cancleChannelsAndCleanGroup(yunChannelGroup);
		rebuildChannels(channelFuture, yunChannelGroup, bootstrap);
		yunChannelGroup.setActive();
		stopAllScheduledTask();
	}

	private void cancleChannelsAndCleanGroup(final YunChannelGroup yunChannelGroup) {
		ArrayList<Channel> channelsList = yunChannelGroup.getChannels();
		int length = channelsList.size();
		Channel channel = null;
		for (int i = 0; i < length; i++) {
			channel = channelsList.get(i);
			closeChannel(channel);
		}
		yunChannelGroup.clean();
	}

	private void closeChannel(final Channel channel) {
		if (null != channel) {
			channel.disconnect();
			channel.close();
		}
	}

	private void rebuildChannels(final ChannelFuture channelFuture, final YunChannelGroup yunChannelGroup,
			final Bootstrap bootstrap) {
		addFirstChannel(channelFuture, yunChannelGroup);
		addOtherChannels(yunChannelGroup, bootstrap);
	}

	private void addFirstChannel(final ChannelFuture channelFuture, final YunChannelGroup yunChannelGroup) {
		Channel channel = channelFuture.channel();
		checkChannelNull(channel);
		yunChannelGroup.addOneChannel(channel);
	}

	private void addOtherChannels(final YunChannelGroup yunChannelGroup, final Bootstrap bootstrap) {
		Channel otherChannels = null;
		ChannelFuture channelFuture = null;
		for (int i = 0; i < yunChannelGroup.getChannelNum() - 1; i++) {
			channelFuture = onlyGetChannelFuture(bootstrap);
			otherChannels = channelFuture.channel();
			checkChannelNull(otherChannels);
			yunChannelGroup.addOneChannel(otherChannels);
		}
	}

	private void checkChannelNull(final Channel channel) {
		if (null == channel) {
			throw new NullPointerException();
		}
	}

}
