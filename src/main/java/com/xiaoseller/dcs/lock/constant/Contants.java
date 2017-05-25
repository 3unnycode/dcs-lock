package com.xiaoseller.dcs.lock.constant;

public class Contants {
	private Contants() {
		//
	}
	
	private static final int ONE_SECOND = 1000;
	/**
	 * 默认锁的过期时间
	 */
	public static final int DEFAULT_EXPIRY_TIME_MILLIS = 3 * ONE_SECOND;
	/**
	 * 默认请求获得锁超时时间
	 */
	public static final int DEFAULT_ACQUIRE_TIMEOUT_MILLIS = 3 * ONE_SECOND;
	public static final int DEFAULT_ACQUIRY_RESOLUTION_MILLIS = 100;
}
