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
package com.github.hebelala.tasking.zookeeper.monitor;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

/**
 * Stat monitor, will watch the path's create/delete events.
 *
 * @author hebelala
 */
public abstract class StatMonitor extends AbstractMonitor implements Watcher, AsyncCallback.StatCallback {

	private String path;
	private boolean existing;

	public StatMonitor(String path, boolean initExisting) {
		this.path = path;
		this.existing = initExisting;
	}

	@Override
	protected void register() {
		registerWatcher();
	}

	@Override
	public void process(WatchedEvent event) {
		switch (event.getType()) {
		case NodeCreated:
		case NodeDeleted:
		case NodeDataChanged:
			registerWatcher();
			break;
		default:
		}
	}

	@Override
	public void processResult(int rc, String path, Object ctx, Stat stat) {
		switch (Code.get(rc)) {
		case OK:
			if (!existing) {
				existing = true;
				created();
			}
			break;
		case NONODE:
			if (existing) {
				existing = false;
				deleted();
			}
			break;
		default:
			registerWatcher();
		}
	}

	private void registerWatcher() {
		if (closed) {
			return;
		}
		zooKeeper.exists(path, this, this, null);
	}

	protected abstract void created();

	protected abstract void deleted();

}
