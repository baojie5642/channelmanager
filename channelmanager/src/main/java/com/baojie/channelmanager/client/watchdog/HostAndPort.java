package com.baojie.channelmanager.client.watchdog;

public class HostAndPort {

	private final String host;

	private final int port;

	private HostAndPort(final String host, final int port) {
		this.host = host;
		this.port = port;
	}

	public static HostAndPort create(final String host, final int port) {
		return new HostAndPort(host, port);
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

}