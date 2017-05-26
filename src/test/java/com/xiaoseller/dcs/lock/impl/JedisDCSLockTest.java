package com.xiaoseller.dcs.lock.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.xiaoseller.dcs.lock.impl.JedisDCSLock;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisDCSLockTest {
	private Random random = new Random();
	private AtomicInteger num = new AtomicInteger(0);
	private JedisPool pool;
	private static final int THREAD_NUM = 20;
	private CyclicBarrier cyclicBarrier = new CyclicBarrier(20, () -> {
		System.out.println("cyclicBarrier");
	});
	private CountDownLatch countDownLatch = new CountDownLatch(20);

	@Before
	public void setup() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(500);
		this.pool = new JedisPool(poolConfig, "192.168.11.29", 6379, 2000, "wIvJt@_redis");
	}

	@Test
	public void getLock() throws InterruptedException {
		Jedis jedis = this.pool.getResource();
		jedis.del("testLock");
		JedisDCSLock lock = new JedisDCSLock(jedis, "testLock");
		assertTrue(lock.tryLock(100L, TimeUnit.MILLISECONDS));
		JedisDCSLock lock2 = new JedisDCSLock(jedis, "testLock");
		assertFalse(lock2.tryLock(100L, TimeUnit.MILLISECONDS));
		lock.unlock();
		lock2 = new JedisDCSLock(jedis, "testLock");
		assertTrue(lock2.tryLock());
		jedis.close();
	}

	@Test
   public void 过期测试_1s无法获取_3s后可以获取()throws InterruptedException
	{
		Jedis jedis = this.pool.getResource();
		jedis.del("testLock");
		JedisDCSLock lock = new JedisDCSLock(jedis, "testLock");
		assertTrue(lock.tryLock(100L, TimeUnit.MILLISECONDS, 3000L));
		Thread.sleep(1000L);
		JedisDCSLock lock2 = new JedisDCSLock(jedis, "testLock");
		assertFalse(lock2.tryLock(100L, TimeUnit.MILLISECONDS));
		Thread.sleep(2000L);
		JedisDCSLock lock3 = new JedisDCSLock(jedis, "testLock");
		assertTrue(lock3.tryLock(100L, TimeUnit.MILLISECONDS));
		lock3.unlock();
		jedis.close();
	}

	@Test
   public void 过期测试_不能释放别人的锁() throws InterruptedException {
      Jedis jedis = this.pool.getResource();
      jedis.del("testLock");
      JedisDCSLock lock = new JedisDCSLock(jedis, "testLock");
      assertTrue(lock.tryLock(100L, TimeUnit.MILLISECONDS, 3000L));
      Thread.sleep(1000L);
      JedisDCSLock lock2 = new JedisDCSLock(jedis, "testLock");
      assertFalse(lock2.tryLock(100L, TimeUnit.MILLISECONDS));
      Thread.sleep(2000L);
      JedisDCSLock lock3 = new JedisDCSLock(jedis, "testLock");
      assertTrue(lock3.tryLock(100L, TimeUnit.MILLISECONDS));
      boolean result = lock.unlock();
      assertTrue(result == false);
      result = lock3.unlock();
      assertTrue(result);
      jedis.close();
   }

	@Test
	public void 并发测试() throws InterruptedException {
		boolean count = true;

		for (int i = 0; i < 20; ++i) {
			JedisDCSLockTest.Locker e = new JedisDCSLockTest.Locker(10);
			e.start();
		}

		this.countDownLatch.await();
		System.out.println("成功次数:" + this.num + ",总次数:" + 200);
	}

	private class Locker extends Thread {
		private final int times;
		private int counter;

		public Locker(int times) {
			this.times = times;
			this.counter = 0;
		}

		public void run() {
			try {
				JedisDCSLockTest.this.cyclicBarrier.await();
			} catch (InterruptedException arg9) {
				arg9.printStackTrace();
			} catch (BrokenBarrierException arg10) {
				arg10.printStackTrace();
			}

			try {
				for (int i = 0; i < this.times; ++i) {
					Jedis resource = JedisDCSLockTest.this.pool.getResource();
					JedisDCSLock lock = new JedisDCSLock(resource, "testlock");

					try {
						if (lock.tryLock(100L, TimeUnit.MILLISECONDS)) {
							JedisDCSLockTest.this.num.addAndGet(1);
							System.out.println(System.currentTimeMillis() + "-" + Thread.currentThread().getName() + ":"
									+ this.counter + "获取锁");
							++this.counter;
							Thread.sleep((long) JedisDCSLockTest.this.random.nextInt(100));
							lock.unlock();
							System.out.println(System.currentTimeMillis() + "-" + Thread.currentThread().getName() + ":"
									+ this.counter + "释放锁");
						} else {
							System.err.println(System.currentTimeMillis() + "-" + Thread.currentThread().getName() + ":"
									+ this.counter + "获取锁超时");
						}
					} catch (InterruptedException arg11) {
						Thread.currentThread().interrupt();
						return;
					}

					resource.close();
				}

			} finally {
				JedisDCSLockTest.this.countDownLatch.countDown();
			}
		}

		public int count() {
			return this.counter;
		}
	}
}
