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

import java.util.Arrays;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

/**
 * Data monitor, will watch the path's create/change/delete events. The zk
 * client must has the READ permission with the path.
 *
 * @author hebelala
 */
public abstract class DataMonitor extends AbstractMonitor
		implements Watcher, AsyncCallback.StatCallback, AsyncCallback.DataCallback {

	private String path;
	private byte[] preData;

	public DataMonitor(String path, byte[] initData) {
		this.path = path;
		this.preData = initData;
	}

	@Override
	protected void register() {
		registerExistsWatcher();
	}

	@Override
	public void process(WatchedEvent event) {
		switch (event.getType()) {
		case NodeCreated:
		case NodeDataChanged:
			registerGetDataWatcher();
			break;
		case NodeDeleted:
			registerExistsWatcher();
			break;
		default:
		}
	}

	@Override
	public void processResult(int rc, String path, Object ctx, Stat stat) {
		byte[] data = null;
		switch (Code.get(rc)) {
		case OK:
			try {
				data = zooKeeper.getData(path, false, null);
			} catch (KeeperException e) {
				// We don't need to worry about recovering now.
				// The watch callbacks will kick off any exception handling.
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		case NONODE:
			doBusiness(data);
			break;
		default:
			registerExistsWatcher();
		}
	}

	@Override
	public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
		switch (Code.get(rc)) {
		case OK:
			doBusiness(data);
			break;
		case NONODE:
			doBusiness(data);
			registerExistsWatcher();
			break;
		default:
			registerGetDataWatcher();
		}
	}

	private void registerExistsWatcher() {
		if (closed) {
			return;
		}
		zooKeeper.exists(path, this, this, null);
	}

	private void registerGetDataWatcher() {
		if (closed) {
			return;
		}
		zooKeeper.getData(path, this, this, null);
	}

	private void doBusiness(byte[] data) {
		if (!Arrays.equals(preData, data)) {
			changed(data);
			preData = data;
		}
	}

	protected abstract void changed(byte[] data);

}
