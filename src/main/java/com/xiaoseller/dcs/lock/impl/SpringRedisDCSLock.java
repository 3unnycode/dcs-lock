package com.xiaoseller.dcs.lock.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.xiaoseller.dcs.lock.DCSLock;

import redis.clients.jedis.Jedis;

public class SpringRedisDCSLock implements DCSLock {
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
	private String lockKey;
	private final UUID lockUUID;
	private StringRedisTemplate stringRedisTemplate;
	
	public SpringRedisDCSLock(String lockKey, StringRedisTemplate stringRedisTemplate) {
		lockUUID = UUID.randomUUID();
		this.lockKey = lockKey;
		this.stringRedisTemplate = stringRedisTemplate;
	}
	
	public boolean tryLock() throws InterruptedException {
		return tryLock(DEFAULT_ACQUIRE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
		return tryLock(waitTime, unit, DEFAULT_EXPIRY_TIME_MILLIS);
	}

	@SuppressWarnings("resource")
	public boolean tryLock(long waitTime, TimeUnit unit, long leaseTime) throws InterruptedException {
		long timeout = unit.toMillis(waitTime);
		long internalLockLeaseTime = unit.toMillis(leaseTime);
		while (timeout > 0) {
			Boolean success = stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
				Object nativeConnection = connection.getNativeConnection();
				Jedis jedis = (Jedis) nativeConnection;
				String status = jedis.set(lockKey, lockUUID.toString(), "NX", "PX", internalLockLeaseTime);
				return status != null && status.equalsIgnoreCase("OK");
			});
			if (success) {
				return true;
			}
			timeout -= DEFAULT_ACQUIRY_RESOLUTION_MILLIS;
			Thread.sleep(DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
		}
		return false;
	}

	public long unLock() {
		List<String> keys = new ArrayList<String>();
		keys.add(lockKey);
		// 采用lua脚本方式保证在多线程环境下也是原子操作
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setResultType(Long.class);
		script.setScriptText("if (redis.call('exists', KEYS[1]) == 0) then " + "return -1; " + "end;"
				+ "if (redis.call('get', KEYS[1]) == ARGV[1]) then " + "redis.call('del', KEYS[1]); " + "return 1;"
				+ "end; " + "return 0;");
		return stringRedisTemplate.execute(script, keys, lockUUID.toString());
	}

}
