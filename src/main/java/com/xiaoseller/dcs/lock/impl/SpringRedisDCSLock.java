package com.xiaoseller.dcs.lock.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.xiaoseller.dcs.lock.DCSLock;
import com.xiaoseller.dcs.lock.constant.Contants;
import com.xiaoseller.dcs.lock.exception.DCSLockException;

import redis.clients.jedis.Jedis;

public class SpringRedisDCSLock implements DCSLock {
	
	private String lockKey;
	private final UUID lockUUID;
	private StringRedisTemplate stringRedisTemplate;

	public SpringRedisDCSLock(String lockKey, StringRedisTemplate stringRedisTemplate) {
		lockUUID = UUID.randomUUID();
		this.lockKey = lockKey;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public boolean tryLock() throws InterruptedException {
		return tryLock(Contants.DEFAULT_ACQUIRE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
		return tryLock(waitTime, unit, Contants.DEFAULT_EXPIRY_TIME_MILLIS);
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
			timeout -= Contants.DEFAULT_ACQUIRY_RESOLUTION_MILLIS;
			Thread.sleep(Contants.DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
		}
		return false;
	}

	public void unLock() {
		List<String> keys = new ArrayList<String>();
		keys.add(lockKey);
		// 采用lua脚本方式保证在多线程环境下也是原子操作
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setResultType(Long.class);
		script.setScriptText("if (redis.call('exists', KEYS[1]) == 0) then " + "return -1; " + "end;"
				+ "if (redis.call('get', KEYS[1]) == ARGV[1]) then " + "redis.call('del', KEYS[1]); " + "return 1;"
				+ "end; " + "return 0;");
		try {
			stringRedisTemplate.execute(script, keys, lockUUID.toString());
		} catch (Exception e) {
			throw new DCSLockException(String.format("unlock fail, lockKey:{}", lockKey), e);
		}
	}

}
