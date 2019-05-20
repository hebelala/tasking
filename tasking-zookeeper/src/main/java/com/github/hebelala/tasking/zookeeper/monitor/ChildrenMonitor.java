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

import java.util.List;

import org.apache.zookeeper.AsyncCallback;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

import com.github.hebelala.tasking.utils.CollectionUtils;

/**
 * Data monitor, will watch the create/delete events of path's children. The zk
 * client must has the READ permission with the path.
 *
 * @author hebelala
 */
public abstract class ChildrenMonitor extends AbstractMonitor
		implements Watcher, AsyncCallback.StatCallback, AsyncCallback.ChildrenCallback {

	private String path;
	private List<String> preChildren;

	public ChildrenMonitor(String path, List<String> initChildren) {
		this.path = path;
		this.preChildren = initChildren;
	}

	@Override
	protected void register() {
		registerExistsWatcher();
	}

	@Override
	public void process(WatchedEvent event) {
		switch (event.getType()) {
		case NodeDataChanged:
		case NodeDeleted:
			registerExistsWatcher();
			break;
		case NodeCreated:
		case NodeChildrenChanged:
			registerGetChildrenWatcher();
			break;
		default:
		}
	}

	@Override
	public void processResult(int rc, String path, Object ctx, List<String> children) {
		switch (Code.get(rc)) {
		case OK:
			doBusiness(children);
			break;
		case NONODE:
			assert children == null : "The children should be null when NoNode, oh my god!";
			doBusiness(null);
			registerExistsWatcher();
			break;
		default:
			registerGetChildrenWatcher();
		}
	}

	@Override
	public void processResult(int rc, String path, Object ctx, Stat stat) {
		switch (Code.get(rc)) {
		case OK:
			registerGetChildrenWatcher();
			break;
		case NONODE:
			assert stat == null : "The stat should be null when NoNode, oh my god!";
			doBusiness(null);
			break;
		default:
			registerExistsWatcher();
		}
	}

	private void registerExistsWatcher() {
		if (closed) {
			return;
		}
		zooKeeper.exists(path, this, this, null);
	}

	private void registerGetChildrenWatcher() {
		if (closed) {
			return;
		}
		zooKeeper.getChildren(path, this, this, null);
	}

	private void doBusiness(List<String> children) {
		if (!CollectionUtils.equals(preChildren, children)) {
			childrenChanged(children);
			preChildren = children;
		}
	}

	protected abstract void childrenChanged(List<String> children);

}
