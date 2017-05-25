package com.xiaoseller.dcs.lock.impl;

import java.util.concurrent.TimeUnit;

import com.xiaoseller.dcs.lock.DCSLock;

public class JedisDCSLock implements DCSLock {

	@Override
	public boolean tryLock() throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean tryLock(long waitTime, TimeUnit unit, long leaseTime) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long unLock() {
		// TODO Auto-generated method stub
		return 0;
	}

}
