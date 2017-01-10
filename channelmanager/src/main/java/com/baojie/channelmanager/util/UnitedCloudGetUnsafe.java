package com.baojie.channelmanager.util;


import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import sun.misc.Unsafe;

public class UnitedCloudGetUnsafe {

	private static final String Unsafe_Object_Name = "theUnsafe";

	private UnitedCloudGetUnsafe() {

	}

	public static Unsafe getUnsafe() {
		final Unsafe unsafe = getUnsafeInner();
		if (unsafe == null) {
			throw new NullPointerException("unsafe must not be null");
		}
		return unsafe;
	}

	private static Unsafe getUnsafeInner() {
		Unsafe unsafe = null;
		try {
			unsafe = AccessController.doPrivileged(action);
		} catch (final PrivilegedActionException e) {
			e.printStackTrace();
		}
		return unsafe;
	}

	private static final PrivilegedExceptionAction<Unsafe> action = new PrivilegedExceptionAction<Unsafe>() {
		public Unsafe run() throws Exception {
			final Field theUnsafeField = makeField();
			return makeUnsafe(theUnsafeField);
		}
	};

	private static Field makeField() {
		Field field = null;
		try {
			field = Unsafe.class.getDeclaredField(Unsafe_Object_Name);
			field.setAccessible(true);
		} catch (final NoSuchFieldException e) {
			e.printStackTrace();
		} catch (final SecurityException e) {
			e.printStackTrace();
		}
		if (null == field) {
			throw new NullPointerException("field get from unsafe must not be null");
		}
		return field;
	}

	private static Unsafe makeUnsafe(final Field field) {
		Unsafe unsafe = null;
		try {
			unsafe = (Unsafe) field.get(null);
		} catch (final IllegalArgumentException e) {
			e.printStackTrace();
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
		}
		if (null == unsafe) {
			throw new NullPointerException("unsafe must not be null");
		}
		return unsafe;
	}

}
