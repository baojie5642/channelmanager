package com.baojie.channelmanager.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.baojie.channelmanager.client.channelgroup.YunChannelGroup;
import com.baojie.channelmanager.client.handler.HeartBeatClientHandler;
import com.baojie.channelmanager.client.sendrunner.MessageSendRunner;
import com.baojie.channelmanager.client.watchdog.ConnectionWatchdog;
import com.baojie.channelmanager.client.watchdog.HostAndPort;
import com.baojie.channelmanager.message.MessageResponse;
import com.baojie.channelmanager.util.UnitedCloudFutureReturnObject;
import com.baojie.channelmanager.util.YunThreadFactory;

public class YunIPClient {

	private static final Logger log = LoggerFactory.getLogger(YunIPClient.class);

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private final YunChannelGroup yunChannelGroup;

	private final ConcurrentHashMap<String, UnitedCloudFutureReturnObject<MessageResponse>> messageFutureMap = new ConcurrentHashMap<String, UnitedCloudFutureReturnObject<MessageResponse>>(
			99999);

	private final ThreadPoolExecutor sendThreadPool = new ThreadPoolExecutor(64, 1024, 180, TimeUnit.SECONDS,
			new SynchronousQueue<>(), YunThreadFactory.create("SendMessageRunner"));

	private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(3,
			YunThreadFactory.create("MonitorYunChannelGroupRunner"));

	private final ListMonitor listMonitor = new ListMonitor();

	private final int howManyChannel;

	private volatile Bootstrap boot;

	private YunIPClient(final int howManyChannel) {
		this.howManyChannel = howManyChannel;
		this.yunChannelGroup = YunChannelGroup.create(howManyChannel, readWriteLock);
		// scheduledStart();
	}

	private void scheduledStart() {
		scheduledThreadPoolExecutor.scheduleWithFixedDelay(listMonitor, 6, 15, TimeUnit.SECONDS);
	}

	public static YunIPClient create(final int howManyChannel) {
		return new YunIPClient(howManyChannel);
	}

	public void connect(int port, String host) {
		EventLoopGroup group = new NioEventLoopGroup();
		boot = new Bootstrap();
		boot.group(group).channel(NioSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO));
		final ConnectionWatchdog watchdog = makeDog(port, host);
		ChannelFuture future = null;
		Channel channelInner = null;
		try {
			synchronized (boot) {
				boot.handler(new HeartCeatsClientChannelInitializer(watchdog, messageFutureMap));
				boot.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);
				boot.option(ChannelOption.TCP_NODELAY, true);
			}
			readWriteLock.writeLock().lock();
			for (int i = 0; i < howManyChannel; i++) {
				future = boot.connect(host, port);
				future.awaitUninterruptibly();
				if (future.isDone() && future.isSuccess()) {
					channelInner = future.channel();
					yunChannelGroup.addOneChannel(channelInner);
					log.debug("HeartBeatsClient channel 连接成功。i的值是：" + i + "。");
				} else {
					log.debug("HeartBeatsClient channel 连接失败。i的值是：" + i + "。");
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			readWriteLock.writeLock().unlock();
		}
	}

	private ConnectionWatchdog makeDog(int port, String host) {
		final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, HostAndPort.create(host, port), readWriteLock,
				yunChannelGroup) {
			@Override
			public void handlers() {
			}
		};
		return watchdog;
	}

	private class HeartCeatsClientChannelInitializer extends ChannelInitializer<SocketChannel> {
		private final ConnectionWatchdog connectionWatchdog;
		private final ConcurrentHashMap<String, UnitedCloudFutureReturnObject<MessageResponse>> messageFutureMap;

		public HeartCeatsClientChannelInitializer(final ConnectionWatchdog connectionWatchdog,
				final ConcurrentHashMap<String, UnitedCloudFutureReturnObject<MessageResponse>> messageFutureMap) {
			this.connectionWatchdog = connectionWatchdog;
			this.messageFutureMap = messageFutureMap;
		}

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ch.pipeline().addLast(connectionWatchdog);
			ch.pipeline().addLast("length", new LengthFieldPrepender(4));
			ch.pipeline().addLast("encoder", new ByteArrayEncoder());
			ch.pipeline().addLast("decoder", new ByteArrayDecoder());
			ch.pipeline().addLast(HeartBeatClientHandler.create(messageFutureMap));
		}
	}

	private class ListMonitor implements Runnable {

		public ListMonitor() {

		}

		@Override
		public void run() {
			if (null == yunChannelGroup) {
				log.debug("ListMonitor channels 为null 。");
				return;
			}
			Iterator<Channel> iterator = yunChannelGroup.getChannels().iterator();
			Channel channel = null;
			try {
				log.debug("ListMonitor yunChannelGroup 的大小是：" + yunChannelGroup.getChannels().size());
				while (iterator.hasNext()) {
					channel = iterator.next();
					if (null != channel) {
						log.debug("ListMonitor channel id is :" + channel.id());
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
				log.debug("ListMonitor catch a throwable !");
			}
		}
	}

	public void startSend() {
		MessageSendRunner messageSendRunner = null;
		for (int i = 0; i < howManyChannel; i++) {
			messageSendRunner = MessageSendRunner.create(messageFutureMap, yunChannelGroup, i);
			sendThreadPool.submit(messageSendRunner);
		}
	}

	public YunChannelGroup getYunChannelGroup() {
		return yunChannelGroup;
	}

	public ConcurrentHashMap<String, UnitedCloudFutureReturnObject<MessageResponse>> getMessageFutureMap() {
		return messageFutureMap;
	}

	public static void main(String[] args) {
		int port = 8080;
		if (args != null && args.length > 0) {
			try {
				port = Integer.valueOf(args[0]);
			} catch (NumberFormatException e) {
			}
		}
		YunIPClient heartBeatsClient = new YunIPClient(32);
		try {
			heartBeatsClient.connect(port, "192.168.12.74");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		heartBeatsClient.startSend();
		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
