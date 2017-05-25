package com.xiaoseller.dcs.lock.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.xiaoseller.dcs.lock.DCSLock;
import com.xiaoseller.dcs.lock.Secret;
import com.xiaoseller.dcs.lock.constant.Contants;
import com.xiaoseller.dcs.lock.exception.DCSLockException;

import redis.clients.jedis.Jedis;

public class JedisDCSLock implements DCSLock {
	
	private Jedis jedis;
	private String lockKey;
	private final UUID lockUUID;
	private Secret secret = null;

	public JedisDCSLock(Jedis jedis, String lockKey) {
		this.jedis = jedis;
		this.lockKey = lockKey;
		this.lockUUID = UUID.randomUUID();
	}

	@Override
	public boolean tryLock() throws InterruptedException {
		return tryLock(Contants.DEFAULT_ACQUIRE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
		return tryLock(waitTime, unit, Contants.DEFAULT_EXPIRY_TIME_MILLIS);
	}

	@Override
	public boolean tryLock(long waitTime, TimeUnit unit, long leaseTime) throws InterruptedException {
		long timeout = unit.toMillis(waitTime);
		long internalLockLeaseTime = unit.toMillis(leaseTime);
		while (timeout > 0) {
			Secret newSecret = new Secret(lockUUID, internalLockLeaseTime);
			if (jedis.setnx(lockKey, newSecret.toString()) == 1) {
				this.secret = newSecret;
				return true;
			}
			String currentValueStr = jedis.get(lockKey);
			Secret currentLock = Secret.format(currentValueStr);
			if (currentValueStr != null && currentLock != null && currentLock.isExpiredOrMine(lockUUID)) {
				String oldValueStr = jedis.getSet(lockKey, newSecret.toString());
				if (currentValueStr.equals(oldValueStr)) {
					this.secret = newSecret;
					return true;
				}
			}
			timeout -= Contants.DEFAULT_ACQUIRY_RESOLUTION_MILLIS;
			Thread.sleep(Contants.DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
		}
		return false;
	}

	@Override
	public void unLock() {
		List<String> keys = new ArrayList<String>();
		keys.add(lockKey);
		List<String> args = new ArrayList<String>();
		args.add(secret.toString());
		// 采用lua脚本方式保证在多线程环境下也是原子操作
		try {
			jedis.eval("if (redis.call('exists', KEYS[1]) == 0) then " + "return -1; " + "end;"
					+ "if (redis.call('get', KEYS[1]) == ARGV[1]) then " + "redis.call('del', KEYS[1]); " + "return 1;"
					+ "end; " + "return 0;", keys, args);
		} catch (Exception e) {
			throw new DCSLockException(String.format("unlock fail, lockKey:{}", lockKey), e);
		}
	}

}
