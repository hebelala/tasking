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
package com.github.hebelala.tasking.zookeeper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.OpResult;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hebelala.tasking.zookeeper.monitor.Monitor;
import com.github.hebelala.tasking.zookeeper.retry.Callback;
import com.github.hebelala.tasking.zookeeper.retry.RetryForever;

/**
 * @author hebelala
 */
public class TaskingZookeeper implements Watcher {

	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private volatile boolean closed;
	private String connectString;
	private int sessionTimeout;
	private String namespace;
	private String username;
	private String password;
	private Listener listener;
	private volatile ZooKeeper zooKeeper;
	private volatile Thread eventThread;
	private volatile RetryForever retryForever;

	private Thread restartThread;
	private volatile boolean shouldRestart;

	public TaskingZookeeper(String connectString, int sessionTimeout, String namespace, String username,
			String password, Listener listener) {
		if (namespace != null && namespace.contains("/")) {
			throw new IllegalArgumentException("The namespace cannot include /");
		}
		this.connectString = connectString;
		this.sessionTimeout = sessionTimeout;
		this.namespace = namespace;
		this.username = username;
		this.password = password;
		this.listener = listener;
	}

	@Override
	public void process(WatchedEvent event) {
		if (event.getType() == Event.EventType.None) {
			Event.KeeperState state = event.getState();
			switch (state) {
			case SyncConnected:
				eventThread = Thread.currentThread();
				break;
			case Expired:
				shouldRestart = true;
				break;
			default:
			}
		}
	}

	public void start() throws IOException {
		AtomicBoolean started = new AtomicBoolean();
		restartThread = new Thread(() -> {
			while (!started.get()) {
				try {
					Thread.sleep(100L);
				} catch (InterruptedException e) {
					return;
				}
			}
			while (true) {
				if (shouldRestart) {
					try {
						if (closed) {
							return;
						}
						if (listener != null) {
							listener.beforeCloseZooKeeper();
						}
						retryForever.close();
						zooKeeper.close();
						blockUntilEventThreadIsNotAlive();
						start0();
					} catch (InterruptedException e) {
						return;
					} catch (Throwable t) {
						logger.error(t.getMessage(), t);
					}
				} else {
					try {
						Thread.sleep(1000L);
					} catch (InterruptedException e) {
						return;
					}
				}
			}

		}, "tasking-zk-restart");
		restartThread.start();
		start0();
		started.set(true);
	}

	private void blockUntilEventThreadIsNotAlive() throws InterruptedException {
		while (eventThread != null && eventThread.isAlive()) {
			Thread.sleep(100L);
		}
	}

	private void start0() throws IOException {
		retryForever = new RetryForever();
		zooKeeper = new ZooKeeper(connectString + "/" + namespace, sessionTimeout, this);
		zooKeeper.addAuthInfo("digest",
				new StringBuilder().append(username).append(':').append(password).toString().getBytes("utf-8"));
		if (listener != null) {
			listener.afterNewZooKeeper();
		}
	}

	public void stop() throws InterruptedException {
		closed = true;
		if (restartThread != null) {
			restartThread.interrupt();
		}
		if (retryForever != null) {
			retryForever.close();
		}
		zooKeeper.close();
		blockUntilEventThreadIsNotAlive();
	}

	public void blockUntilConnected() throws InterruptedException {
		while (!zooKeeper.getState().isConnected() || eventThread == null || !eventThread.isAlive()) {
			Thread.sleep(100L);
		}
	}

	public ZooKeeper getZooKeeper() {
		return zooKeeper;
	}

	public long getSessionId() {
		return zooKeeper.getSessionId();
	}

	public boolean isStopped() {
		return closed && (eventThread == null || !eventThread.isAlive());
	}

	public Stat exists(String path) throws InterruptedException {
		return retryForever.call(() -> zooKeeper.exists(path, null));
	}

	public void register(Monitor monitor) {
		monitor.registerTo(zooKeeper);
	}

	public String create(String path, String data, CreateMode createMode) throws InterruptedException {
		return retryForever.call(() -> {
			try {
				return zooKeeper.create(path, data.getBytes("utf-8"), ZooDefs.Ids.CREATOR_ALL_ACL, createMode);
			} catch (KeeperException.NodeExistsException e) {
				return null;
			} catch (KeeperException.NoNodeException e) {
				String parentPath = path.substring(0, path.lastIndexOf('/'));
				if (parentPath.isEmpty()) { // The root is a chroot, so fix the parentPath to create
					parentPath = "/";
				}
				create(parentPath, "", CreateMode.PERSISTENT);
				return create(path, data, createMode);
			}
		});
	}

	/**
	 * Get data with stat, return null when NoNode.
	 */
	public String get(String path, Stat stat) throws InterruptedException {
		return retryForever.call(() -> {
			try {
				byte[] data = zooKeeper.getData(path, false, stat);
				return data == null ? null : new String(data, "utf-8");
			} catch (KeeperException.NoNodeException e) {
				return null;
			}
		});
	}

	public void delete(String path) throws InterruptedException {
		retryForever.call(new Callback() {
			@Override
			public void call() throws Exception {
				delete0(path);
			}

			private void delete0(String path) throws KeeperException, InterruptedException {
				try {
					zooKeeper.delete(path, -1);
				} catch (KeeperException.NoNodeException e) {
					return;
				} catch (KeeperException.NotEmptyException e) {
					List<String> children = zooKeeper.getChildren(path, false);
					for (String child : children) {
						delete0(child);
					}
				}
			}
		});
	}

	public List<OpResult> multi(Op... ops) throws InterruptedException {
		return retryForever.call(() -> zooKeeper.multi(Arrays.asList(ops)));
	}

	public interface Listener {

		void beforeCloseZooKeeper();

		void afterNewZooKeeper();
	}
}
