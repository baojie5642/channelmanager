package com.baojie.channelmanager.client.watchdog;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class ExecuteHolder {

	private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
	private final LinkedBlockingQueue<Future<?>> linkedBlockingQueue;

	private ExecuteHolder(final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor,
			final LinkedBlockingQueue<Future<?>> linkedBlockingQueue) {

		this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
		this.linkedBlockingQueue = linkedBlockingQueue;
	}

	public static ExecuteHolder create(final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor,
			final LinkedBlockingQueue<Future<?>> linkedBlockingQueue){
		return new ExecuteHolder(scheduledThreadPoolExecutor, linkedBlockingQueue);
	}
	
	
	public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
		return scheduledThreadPoolExecutor;
	}

	public LinkedBlockingQueue<Future<?>> getLinkedBlockingQueue() {
		return linkedBlockingQueue;
	}

}
