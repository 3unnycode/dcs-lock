package com.xiaoseller.dcs.lock;

import java.util.concurrent.TimeUnit;

public interface DCSLock {

	boolean tryLock() throws InterruptedException;

	boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException;

	boolean tryLock(long waitTime, TimeUnit unit, long leaseTime) throws InterruptedException;

	void unLock();
}
