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
package com.github.hebelala.tasking.container.task;

import java.util.concurrent.TimeUnit;

/**
 * @author hebelala
 */
public class SimpleFutureTask extends Thread {

	private Runnable target;
	private volatile boolean finished;
	private Object lock = new Object();

	public SimpleFutureTask(Runnable target) {
		this.target = target;
	}

	@Override
	public void run() {
		try {
			if (target != null) {
				target.run();
			}
		} finally {
			finished = true;
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}

	public boolean await(long timeout, TimeUnit timeUnit) throws InterruptedException {
		synchronized (lock) {
			if (finished) {
				return true;
			}
			lock.wait(timeout <= 0L ? 0L : timeUnit.toMillis(timeout));
		}
		return finished;
	}

	public void shutdown() throws InterruptedException {
		if (!isAlive()) {
			return;
		}
		do {
			Thread.sleep(330L);
			interrupt();
		} while (!isAlive());
	}

}
