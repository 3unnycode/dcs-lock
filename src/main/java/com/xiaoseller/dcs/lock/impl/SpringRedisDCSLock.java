package com.xiaoseller.dcs.lock.impl;

import java.util.concurrent.TimeUnit;

import com.xiaoseller.dcs.lock.DCSLock;

public class SpringRedisDCSLock implements DCSLock {
	
	public boolean tryLock() throws InterruptedException {
		return false;
	}

	public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
		return false;
	}

	public boolean tryLock(long waitTime, TimeUnit unit, long leaseTime) throws InterruptedException {
		return false;
	}

	public boolean unLock() {
		return false;
	}

}
