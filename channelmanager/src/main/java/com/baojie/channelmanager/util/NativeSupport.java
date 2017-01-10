package com.baojie.channelmanager.util;

public final class NativeSupport {

    private static final boolean Support_Native_Epoll;

    static {
        boolean epoll;
        try {
            Class.forName("io.netty.channel.epoll.Native");
            epoll = true;
        } catch (Throwable e) {
            epoll = false;
        }
        Support_Native_Epoll = epoll;
    }

    public static boolean isSupportNativeET() {
        return Support_Native_Epoll;
    }
    
}
