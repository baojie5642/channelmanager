package com.baojie.channelmanager.util;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.util.concurrent.ConcurrentHashMap;

public class SerializationUtil {

	private static final Logger log = LoggerFactory.getLogger(SerializationUtil.class);

	private static final Objenesis objenesis = new ObjenesisStd(true);

	private static final ConcurrentHashMap<Class<?>, Schema<?>> CachedSchema = new ConcurrentHashMap<Class<?>, Schema<?>>();

	static {
		System.getProperties().setProperty("protostuff.runtime.always_use_sun_reflection_factory", "true");
	}

	private SerializationUtil() {
	}

	public static <T> byte[] serialize(final T obj) {
		if (null == obj) {
			log.error("'T obj' must not be null.");
			throw new NullPointerException("'T obj' must not be null.");
		}
		final byte[] realBytes = realBytes(obj);
		return realBytes;
	}

	
	@SuppressWarnings("unchecked")
	private static <T> byte[] realBytes(final T obj) {
		byte[] bytes = null;
		Class<T> cls = (Class<T>) obj.getClass();
		Schema<T> schema = getSchema(cls);
		byte[] bytesForBuffer = new byte[LinkedBuffer.DEFAULT_BUFFER_SIZE];
		LinkedBuffer buffer = LinkedBuffer.use(bytesForBuffer);
		try {
			bytes = ProtostuffIOUtil.toByteArray(obj, schema, buffer);
			if ((null == bytes) || (bytes.length == 0)) {
				bytes = new byte[0];
			}
		} finally {
			releaseResource(buffer, bytesForBuffer);
		}
		return bytes;
	}

	@SuppressWarnings("unchecked")
	private static <T> Schema<T> getSchema(final Class<T> cls) {
		if (null == cls) {
			log.error("'Class<T> cls' must not be null.");
			throw new NullPointerException("'Class<T> cls' must not be null.");
		}
		Schema<T> schema = (Schema<T>) CachedSchema.get(cls);
		if (null == schema) {
			setSystemProperties();
			schema = RuntimeSchema.getSchema(cls);
			if (null != schema) {
				CachedSchema.putIfAbsent(cls, schema);
			}
		}
		return schema;
	}

	private static void setSystemProperties() {

	}

	private static <T> void releaseResource(LinkedBuffer buffer, byte bytesForBuffer[]) {
		if (null != buffer) {
			buffer.clear();
			buffer = null;
		}
		if (null != bytesForBuffer) {
			bytesForBuffer = null;
		}
	}

	public static <T> T deserialize(final byte[] bytes, final Class<T> cls) {
		innerCheck(cls, bytes);
		T message = getInstanceInner(cls);
		Schema<T> schema = getSchema(cls);
		try{
			ProtostuffIOUtil.mergeFrom(bytes, 0, bytes.length, message, schema);
		}catch (Throwable throwable) {
			throwable.printStackTrace();
		}
		return message;
	}

	private static <T> T getInstanceInner(final Class<T> cls) {
		final T t = realGet(cls);
		if (null != t) {
			return t;
		} else {
			return (T) objenesis.newInstance(cls);
		}
	}

	private static <T> T realGet(final Class<T> cls) {
		T t = null;
		try {
			t = cls.newInstance();
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			t = null;
		}
		return t;
	}

	private static <T> void innerCheck(final Class<T> cls, final byte[] bytes) {
		if (null == cls) {
			log.error("'Class<T> cls' must not be null.");
			throw new NullPointerException("'Class<T> cls' must not be null.");
		}
		if (null == bytes) {
			log.error("'byte[] bytes' must not be null.");
			throw new NullPointerException("'byte[] bytes' must not be null.");
		}
		if (bytes.length == 0) {
			log.error("'byte[] bytes.length' must not be zero.");
			throw new IllegalArgumentException("'byte[] bytes.length' must not be zero.");
		}
	}

}
