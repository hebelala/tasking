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
package com.github.hebelala.tasking.actor.app;

import java.io.IOException;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hebelala.tasking.actor.entity.Actor;
import com.github.hebelala.tasking.actor.task.TaskKeeper;
import com.github.hebelala.tasking.utils.CollectionUtils;
import com.github.hebelala.tasking.zookeeper.TaskingZookeeper;
import com.github.hebelala.tasking.zookeeper.monitor.ChildrenMonitor;

/**
 * @author hebelala
 */
public class ApplicationKeeper {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private volatile boolean closed;
	private Object lock = new Object();

	private Actor actor;
	private URLClassLoader appClassloader;
	private TaskingProperties taskingProperties;
	private Object application;
	private TaskingZookeeper taskingZookeeper;
	private TasksMonitor tasksMonitor;
	private Map<String, TaskKeeper> taskKeeperMap = new HashMap<>();

	public ApplicationKeeper(Actor actor, URLClassLoader appClassloader) throws ClassNotFoundException, IOException,
			InstantiationException, InterruptedException, IllegalAccessException {
		try {
			this.actor = actor;
			this.appClassloader = appClassloader;
			taskingProperties = TaskingProperties.Loader.load(appClassloader);

			ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(appClassloader);
				application = appClassloader.loadClass(taskingProperties.getApp()).newInstance();
			} finally {
				Thread.currentThread().setContextClassLoader(contextClassLoader);
			}

			taskingZookeeper = new TaskingZookeeper(taskingProperties.getConnectString(),
					Integer.parseInt(taskingProperties.getSessionTimeout()), taskingProperties.getNamespace(),
					taskingProperties.getUsername(), taskingProperties.getPassword(), new ZookeeperListener());
			taskingZookeeper.start();
			taskingZookeeper.blockUntilConnected();
			logger.info("The zookeeper connect to {} successfully", taskingProperties.getConnectString());
		} catch (Throwable t) {
			if (taskingZookeeper != null) {
				taskingZookeeper.stop();
			}
			throw t;
		}
	}

	public void stop() throws IOException, InterruptedException {
		synchronized (lock) {
			closed = true;
			tasksMonitor.close();
			taskKeeperMap.forEach((task, taskKeeper) -> taskKeeper.close());
			taskKeeperMap.clear();
			taskingZookeeper.stop();
			appClassloader.close();
		}
	}

	class ZookeeperListener implements TaskingZookeeper.Listener {

		@Override
		public void beforeCloseZooKeeper() {
			synchronized (lock) {
				if (closed) {
					return;
				}
				if (tasksMonitor != null) {
					tasksMonitor.close();
				}
				taskKeeperMap.forEach((task, taskKeeper) -> taskKeeper.close());
				taskKeeperMap.clear();
			}
		}

		@Override
		public void afterNewZooKeeper() {
			synchronized (lock) {
				if (closed) {
					return;
				}
				tasksMonitor = new TasksMonitor("/" + taskingProperties.getNamespace(), null);
				taskingZookeeper.register(tasksMonitor);
			}
		}
	}

	class TasksMonitor extends ChildrenMonitor {

		public TasksMonitor(String path, List<String> initChildren) {
			super(path, initChildren);
		}

		@Override
		public void childrenChanged(List<String> children) {
			synchronized (lock) {
				if (super.closed) {
					return;
				}
				if (CollectionUtils.isNotBlank(children)) {
					children.forEach(task -> {
						if (!taskKeeperMap.containsKey(task)) {
							TaskKeeper taskKeeper = new TaskKeeper(actor, taskingProperties.getNamespace(), task,
									taskingZookeeper, application);
							taskKeeperMap.put(task, taskKeeper);
						}
					});
				}
				Iterator<Map.Entry<String, TaskKeeper>> iterator = taskKeeperMap.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, TaskKeeper> next = iterator.next();
					String task = next.getKey();
					if (CollectionUtils.notContains(children, task)) {
						next.getValue().close();
						iterator.remove();
					}
				}
			}
		}

	}
}
