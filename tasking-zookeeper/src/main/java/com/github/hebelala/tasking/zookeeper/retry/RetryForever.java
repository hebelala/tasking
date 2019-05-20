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
package com.github.hebelala.tasking.zookeeper.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author hebelala
 */
public class RetryForever {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private volatile boolean closed;

	public void call(Callback callback) throws InterruptedException {
		while (!closed) {
			try {
				callback.call();
				return;
			} catch (Throwable t) {
				if (t instanceof InterruptedException) {
					throw (InterruptedException) t;
				}
				logger.error(t.getMessage(), t);
			}
		}
		handleClosed();
	}

	public <V> V call(Callback2<V> callback2) throws InterruptedException {
		while (!closed) {
			try {
				return callback2.call();
			} catch (Throwable t) {
				if (t instanceof InterruptedException) {
					throw (InterruptedException) t;
				}
				logger.error(t.getMessage(), t);
			}
		}
		return handleClosed();
	}

	private <V> V handleClosed() throws InterruptedException {
		throw new InterruptedException(getClass().getName() + " is closed, interrupt current thread");
	}

	public void close() {
		closed = true;
	}
}
