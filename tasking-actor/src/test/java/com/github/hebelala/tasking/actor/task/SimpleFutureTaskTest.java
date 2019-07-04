/**
 * Copyright © 2019 hebelala (hebelala@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.hebelala.tasking.actor.task;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class SimpleFutureTaskTest {

	@BeforeAll
	public static void beforeClass() {
		System.setProperty("tasking.log.appender", "Console");
	}

	@Test
	@Order(1)
	public void testAwaitFalse() throws InterruptedException {
		SimpleFutureTask simpleFutureTask = new SimpleFutureTask(new SleepOneSecond());
		simpleFutureTask.start();
		assertFalse(simpleFutureTask.await(900L, TimeUnit.MILLISECONDS));
		simpleFutureTask.shutdown();
	}

	@Test
	@Order(2)
	public void testAwaitTrue() throws InterruptedException {
		SimpleFutureTask simpleFutureTask = new SimpleFutureTask(new SleepOneSecond());
		simpleFutureTask.start();
		assertTrue(simpleFutureTask.await(1100L, TimeUnit.MILLISECONDS));
		// Still could be shutdown while simpleFutureTask is finished
		simpleFutureTask.shutdown();
	}

	@Test
	@Order(3)
	public void testShutdown() throws InterruptedException {
		SleepOneSecond sleepOneSecond = new SleepOneSecond();
		SimpleFutureTask simpleFutureTask = new SimpleFutureTask(sleepOneSecond);
		simpleFutureTask.start();

		assertTimeout(ofMillis(1000L), () -> simpleFutureTask.shutdown());
		assertAll(() -> {
			Throwable throwable = sleepOneSecond.getThrowable();
			assertNotNull(throwable);
			assertTrue(throwable instanceof InterruptedException);
		});
	}

	@Test
	@Order(4)
	public void testShutdownWithRetrySleep() throws InterruptedException {
		RetrySleepOneSecond retrySleepOneSecond = new RetrySleepOneSecond();
		SimpleFutureTask simpleFutureTask = new SimpleFutureTask(retrySleepOneSecond);
		simpleFutureTask.start();

		assertTimeout(ofMillis(1000L), () -> simpleFutureTask.shutdown());
		assertAll(() -> {
			Throwable throwable = retrySleepOneSecond.getThrowable();
			assertNotNull(throwable);
			assertTrue(throwable instanceof InterruptedException);
		});
		assertAll(() -> {
			Throwable throwable2 = retrySleepOneSecond.getThrowable2();
			assertNotNull(throwable2);
			assertTrue(throwable2 instanceof InterruptedException);
		});
	}

	@Test
	@Order(5)
	public void testAwaitWhenAlreadyShutdown() throws InterruptedException {
		SimpleFutureTask simpleFutureTask = new SimpleFutureTask(new SleepOneSecond());
		simpleFutureTask.start();
		simpleFutureTask.shutdown();
		assertTrue(simpleFutureTask.await(1L, TimeUnit.MILLISECONDS));
	}

	@Test
	@Order(5)
	public void testAwaitNegativeTime() throws InterruptedException {
		SimpleFutureTask simpleFutureTask = new SimpleFutureTask(new SleepOneSecond());
		simpleFutureTask.start();
		long startNano = System.nanoTime();
		assertTrue(simpleFutureTask.await(-1L, TimeUnit.MILLISECONDS));
		long costMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
		assertTrue(costMillis > 900L && costMillis < 1100L);
	}

	class SleepOneSecond implements Runnable {

		private volatile Throwable throwable;

		@Override
		public void run() {
			try {
				Thread.sleep(1000L);
			} catch (Throwable t) {
				throwable = t;
			}
		}

		public Throwable getThrowable() {
			return throwable;
		}

	}

	class RetrySleepOneSecond implements Runnable {

		private volatile Throwable throwable;
		private volatile Throwable throwable2;

		@Override
		public void run() {
			while (throwable2 == null) {
				try {
					Thread.sleep(1000L);
				} catch (Throwable t) {
					if (throwable == null) {
						throwable = t;
					} else {
						throwable2 = t;
					}
				}
			}
		}

		public Throwable getThrowable() {
			return throwable;
		}

		public Throwable getThrowable2() {
			return throwable2;
		}

	}

}
