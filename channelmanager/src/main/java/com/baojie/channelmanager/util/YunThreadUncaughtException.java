package com.baojie.channelmanager.util;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YunThreadUncaughtException implements UncaughtExceptionHandler {
	
	private static final Logger log = LoggerFactory.getLogger(YunThreadUncaughtException.class);
	
	public YunThreadUncaughtException(){
		
	}
	
	@Override
	public void uncaughtException(final Thread t, final Throwable e) {
		innerCheck(t, e);
		final String threadName=getThreadName(t);
		yunThreadInterrupted(t);
		log.error("线程"+threadName+",发生了UncaughtExceptionHandler异常，这很严重，请检查……！！！已经将线程中断。异常信息："+e.toString());
	}

	private void innerCheck(final Thread t, final Throwable e) {
		CheckNull.checkNull(t);
		CheckNull.checkNull(e);
	}

	private String getThreadName(final Thread thread){
		String string =Thread.currentThread().getName();
		CheckNull.checkNull(string);
		return string;
	}
	
	private void yunThreadInterrupted(final Thread t) {
		try {
			t.interrupt();
		} finally {
			alwaysInterrupt(t);
		}
	}

	private void alwaysInterrupt(final Thread t) {
		if (!t.isInterrupted()) {
			t.interrupt();
		}
		if (t.isAlive()) {
			t.interrupt();
		}
	}
}
