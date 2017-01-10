package com.baojie.channelmanager.util;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YunThreadFactory implements ThreadFactory {

	private final UncaughtExceptionHandler unCaughtException = new YunThreadUncaughtException();
	private static final Logger log = LoggerFactory.getLogger(YunThreadFactory.class);
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	private final AtomicLong threadNumber = new AtomicLong(1);
	private static final int No_Thread_Priority = -7;
	private final ThreadGroup group;
	private final String namePrefix;
	private final String factoryName;
	private final int threadPriority;
	private final boolean isDaemon;

	public static YunThreadFactory create(final String name) {
		return new YunThreadFactory(name, false, No_Thread_Priority);
	}

	public static YunThreadFactory create(final String name, final boolean isDaemon) {
		return new YunThreadFactory(name, isDaemon, No_Thread_Priority);
	}

	public static YunThreadFactory create(final String name, final boolean isDaemon, final int threadPriority) {
		return new YunThreadFactory(name, isDaemon, threadPriority);
	}

	private YunThreadFactory(final String name, final boolean isDaemon, final int threadPriority) {
		this.group = getThreadGroup();
		this.factoryName = name;
		this.isDaemon = isDaemon;
		this.threadPriority = threadPriority;
		this.namePrefix = factoryName + "-" + poolNumber.getAndIncrement() + "-thread-";
	}

	private ThreadGroup getThreadGroup() {
		final SecurityManager sm = getSecurityManager();
		final ThreadGroup threadGroup = innerThreadGroup(sm);
		return threadGroup;
	}

	private SecurityManager getSecurityManager() {
		SecurityManager sm = null;
		sm = System.getSecurityManager();
		if (null == sm) {
			log.debug("YunThreadFactory 获取的 SecurityManager为null。");
		}
		return sm;
	}

	private ThreadGroup innerThreadGroup(final SecurityManager sm) {
		ThreadGroup threadGroup = null;
		if (null != sm) {
			threadGroup = sm.getThreadGroup();
		} else {
			threadGroup = Thread.currentThread().getThreadGroup();
		}
		if (null == threadGroup) {
			log.error("YunThreadFactory最终获取的ThreadGroup为null，这个很严重的问题，下面一条代码会抛出NullPointerException异常！！！");
			throw new NullPointerException("threadgroup must not be null.");
		}
		return threadGroup;
	}

	@Override
	public Thread newThread(final Runnable r) {
		final Thread thread = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
		setThreadProperties(thread);
		return thread;
	}

	private void setThreadProperties(final Thread thread) {
		setDaemon(thread);
		setThreadPriority(thread);
		thread.setUncaughtExceptionHandler(unCaughtException);
	}

	private void setDaemon(final Thread thread) {
		if (isDaemon == true) {
			thread.setDaemon(true);
		} else {
			if (thread.isDaemon()) {
				thread.setDaemon(false);
			}
		}
	}

	private void setThreadPriority(final Thread thread) {
		if (threadPriority == No_Thread_Priority) {
			if (thread.getPriority() != Thread.NORM_PRIORITY) {
				thread.setPriority(Thread.NORM_PRIORITY);
			}
		} else {
			final int priority = checkThreadPriority();
			thread.setPriority(priority);
		}
	}

	private int checkThreadPriority() {
		if (threadPriority < Thread.MIN_PRIORITY) {
			return Thread.NORM_PRIORITY;
		} else if (threadPriority > Thread.MAX_PRIORITY) {
			return Thread.NORM_PRIORITY;
		} else {
			return threadPriority;
		}
	}

}
