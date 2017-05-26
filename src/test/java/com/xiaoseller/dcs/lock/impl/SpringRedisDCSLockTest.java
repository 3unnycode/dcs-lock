package com.xiaoseller.dcs.lock.impl;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@SpringBootApplication
public class SpringRedisDCSLockTest {
	private Random random = new Random();
	private AtomicInteger num = new AtomicInteger(0);
	private static final int THREAD_NUM = 2;
	private CyclicBarrier cyclicBarrier = new CyclicBarrier(2, () -> {
		System.out.println("cyclicBarrier");
	});
	private CountDownLatch countDownLatch = new CountDownLatch(2);
	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Before
	public void setup() {
	}

	@Test
   public void getLock()throws InterruptedException
	{
		this.stringRedisTemplate.delete("testLock");
		SpringRedisDCSLock lock = new SpringRedisDCSLock("testLock", stringRedisTemplate);
		Assert.assertTrue(lock.tryLock(100L, TimeUnit.MILLISECONDS));
		SpringRedisDCSLock lock2 = new SpringRedisDCSLock("testLock", stringRedisTemplate);
		Assert.assertFalse(lock2.tryLock(100L, TimeUnit.MILLISECONDS));
		lock.unlock();
		lock2 = new SpringRedisDCSLock("testLock", stringRedisTemplate);
		Assert.assertTrue(lock2.tryLock());
	}

	@Test
   public void 过期测试_1s无法获取_3s后可以获取()throws InterruptedException
	{
		this.stringRedisTemplate.delete("testLock");
		SpringRedisDCSLock lock = new SpringRedisDCSLock("testLock", stringRedisTemplate);
		Assert.assertTrue(lock.tryLock(100L, TimeUnit.MILLISECONDS, 3000L));
		Thread.sleep(1000L);
		SpringRedisDCSLock lock2 = new SpringRedisDCSLock("testLock", stringRedisTemplate);
		Assert.assertFalse(lock2.tryLock(100L, TimeUnit.MILLISECONDS));
		Thread.sleep(2000L);
		SpringRedisDCSLock lock3 = new SpringRedisDCSLock("testLock",stringRedisTemplate);
		Assert.assertTrue(lock3.tryLock(100L, TimeUnit.MILLISECONDS));
		Assert.assertTrue(lock3.unlock());
	}

	@Test
   public void 过期测试_不能释放别人的锁() throws InterruptedException {
      this.stringRedisTemplate.delete("testLock");
      SpringRedisDCSLock lock = new SpringRedisDCSLock("testLock", stringRedisTemplate);
      Assert.assertTrue(lock.tryLock(100L, TimeUnit.MILLISECONDS, 3000L));
      Thread.sleep(1000L);
      SpringRedisDCSLock lock2 = new SpringRedisDCSLock("testLock", stringRedisTemplate);
      Assert.assertFalse(lock2.tryLock(100L, TimeUnit.MILLISECONDS));
      Thread.sleep(2000L);
      SpringRedisDCSLock lock3 = new SpringRedisDCSLock("testLock", stringRedisTemplate);
      Assert.assertTrue(lock3.tryLock(100L, TimeUnit.MILLISECONDS));
      Assert.assertTrue(lock.unlock() == false);
      Assert.assertTrue(lock3.unlock());
   }

	@Test
	public void 并发测试() throws InterruptedException {
		this.stringRedisTemplate.delete("testlock");
		long start = System.currentTimeMillis();

		for (int i = 0; i < 2; ++i) {
			SpringRedisDCSLockTest.Locker e = new SpringRedisDCSLockTest.Locker(10);
			e.start();
		}

		this.countDownLatch.await();
		System.out.println("成功次数:" + this.num + ",总次数:" + 20 + ",耗时:" + (System.currentTimeMillis() - start));
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
				SpringRedisDCSLockTest.this.cyclicBarrier.await();
			} catch (InterruptedException arg8) {
				arg8.printStackTrace();
			} catch (BrokenBarrierException arg9) {
				arg9.printStackTrace();
			}

			try {
				for (int i = 0; i < this.times; ++i) {
					SpringRedisDCSLock lock = new SpringRedisDCSLock("testlock", stringRedisTemplate);

					try {
						if (lock.tryLock(2147483647L, TimeUnit.MILLISECONDS, 2147483647L)) {
							SpringRedisDCSLockTest.this.num.addAndGet(1);
							System.out.println(System.currentTimeMillis() + "-" + Thread.currentThread().getName() + ":"
									+ this.counter + "获取锁");
							++this.counter;
							Thread.sleep(50L);
							lock.unlock();
							System.out.println(System.currentTimeMillis() + "-" + Thread.currentThread().getName() + ":"
									+ this.counter + "释放锁");
						} else {
							System.err.println(System.currentTimeMillis() + "-" + Thread.currentThread().getName() + ":"
									+ this.counter + "获取锁超时");
						}
					} catch (InterruptedException arg10) {
						Thread.currentThread().interrupt();
						return;
					}
				}

			} finally {
				SpringRedisDCSLockTest.this.countDownLatch.countDown();
			}
		}

		public int count() {
			return this.counter;
		}
	}
}
