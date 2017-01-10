package com.baojie.channelmanager.client.channelgroup;



import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;

public class YunChannelGroup {
	private static final Logger log = LoggerFactory.getLogger(YunChannelGroup.class);

	private final int channelNum;
//test commit git
	private final AtomicBoolean isActive = new AtomicBoolean(true);

	private final ReentrantReadWriteLock readWriteLock;

	private volatile int concurrentWriteFlag;

	private final ArrayList<Channel> channels;

	private YunChannelGroup(final int channelNum, final ReentrantReadWriteLock readWriteLock) {
		this.channelNum = channelNum;
		this.channels = new ArrayList<>(channelNum);
		this.readWriteLock = readWriteLock;
		this.concurrentWriteFlag = channelNum;
	}

	public static YunChannelGroup create(final int channelNum, final ReentrantReadWriteLock readWriteLock) {
		return new YunChannelGroup(channelNum, readWriteLock);
	}

	public int getChannelNum() {
		return channelNum;
	}

	public ArrayList<Channel> getChannels() {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.readLock().lock();
		try {
			return channels;
		} finally {
			readWriteLockInner.readLock().unlock();
		}
	}

	public boolean addOneChannel(final Channel channel) {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		if (null == channel) {
			throw new NullPointerException();
		}
		readWriteLockInner.writeLock().lock();
		try {
			return realAdd(channel);
		} finally {
			readWriteLockInner.writeLock().unlock();
		}
	}

	private boolean realAdd(final Channel channel) {
		boolean isAddSuccess = true;
		try {
			channels.add(channel);
		} catch (Throwable t) {
			t.printStackTrace();
			log.error("数组越界。");
			isAddSuccess = false;
		}
		return isAddSuccess;
	}

	public Channel getOneChannel(final int channelNum) {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.readLock().lock();
		try {
			return realGet(channelNum);
		} finally {
			readWriteLockInner.readLock().unlock();
		}
	}

	private Channel realGet(final int channelNum) {
		Channel channel = null;
		try {
			channel = channels.get(channelNum);
		} catch (Throwable t) {
			t.printStackTrace();
			log.error("元素对象不存在。");
			channel = null;
		}
		return channel;
	}

	public boolean isFirstOneReconnect() {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.readLock().lock();
		try {
			if (concurrentWriteFlag == channelNum) {
				return true;
			} else {
				return false;
			}
		} finally {
			readWriteLockInner.readLock().unlock();
		}
	}

	public int getAndDecConcurrentFlag() {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.writeLock().lock();
		try {
			concurrentWriteFlag = concurrentWriteFlag - 1;
			return concurrentWriteFlag;
		} finally {
			readWriteLockInner.writeLock().unlock();
		}
	}

	public boolean isLastOneReconnect() {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.readLock().lock();
		try {
			if (concurrentWriteFlag == 0) {
				return true;
			} else {
				return false;
			}
		} finally {
			readWriteLockInner.readLock().unlock();
		}
	}

	public void resetFlag() {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.writeLock().lock();
		try {
			concurrentWriteFlag = channelNum;
		} finally {
			readWriteLockInner.writeLock().unlock();
		}
	}

	public void clean() {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.writeLock().lock();
		try {
			channels.clear();
		} finally {
			readWriteLockInner.writeLock().unlock();
		}
	}

	public boolean isContainThisChannel(final Channel channel) {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.readLock().lock();
		try {
			if (channels.contains(channel)) {
				return true;
			} else {
				return false;
			}
		} finally {
			readWriteLockInner.readLock().unlock();
		}
	}

	public boolean channelGroupIsEmpty() {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.readLock().lock();
		try {
			if (channels.size() == channelNum) {
				return true;
			} else {
				return false;
			}
		} finally {
			readWriteLockInner.readLock().unlock();
		}
	}

	public int howManyChannelsInGroup() {
		final ReentrantReadWriteLock readWriteLockInner = readWriteLock;
		readWriteLockInner.readLock().lock();
		try {
			return channels.size();
		} finally {
			readWriteLockInner.readLock().unlock();
		}
	}

	public void setActive() {
		isActive.set(true);
	}

	public void setInactive() {
		isActive.set(false);
	}

	public boolean activeState() {
		return isActive.get();
	}

}
