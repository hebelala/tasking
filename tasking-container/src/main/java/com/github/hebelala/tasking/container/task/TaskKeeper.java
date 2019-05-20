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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.ZooDefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hebelala.tasking.api.Response;
import com.github.hebelala.tasking.container.Server;
import com.github.hebelala.tasking.container.task.execution.Completed;
import com.github.hebelala.tasking.container.task.execution.Running;
import com.github.hebelala.tasking.zookeeper.TaskingZookeeper;
import com.github.hebelala.tasking.zookeeper.monitor.DataMonitor;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * @author hebelala
 */
public class TaskKeeper {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private Server server;
	private String namespace;
	private String name;
	private TaskingZookeeper taskingZookeeper;
	private Object application;

	private DataMonitor taskMonitor;
	private ScheduledExecutorService scheduledExecutorService;

	private String taskPath;
	private String executionPath;
	private String serverPath;
	private String runningPath;
	private String completedPath;

	private Gson gson = new Gson();

	public TaskKeeper(Server server, String namespace, String name, TaskingZookeeper taskingZookeeper,
			Object application) {
		this.server = server;
		this.namespace = namespace;
		this.name = name;
		this.taskingZookeeper = taskingZookeeper;
		this.application = application;

		this.taskPath = String.format("/%s/%s", namespace, name);
		this.executionPath = String.format("/%s/execution", taskPath);
		this.serverPath = String.format("/%s/server", executionPath);
		this.runningPath = String.format("/%s/running", executionPath);
		this.completedPath = String.format("/%s/completed", executionPath);
	}

	public void start() {
		// TODO leader monitor, just leader can do business
		scheduledExecutorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread();
				thread.setName(String.format("TaskRunner-%s-%s", namespace, name));
				return thread;
			}
		});
		taskMonitor = new TaskMonitor("/" + namespace + "/" + name, null);
	}

	public void close() {
		taskMonitor.close();
		scheduledExecutorService.shutdownNow();
	}

	class TaskMonitor extends DataMonitor {

		public TaskMonitor(String path, byte[] initData) {
			super(path, initData);
		}

		@Override
		protected void changed(byte[] data) {
			TaskConfig taskConfig;
			try {
				taskConfig = gson.fromJson(new String(data, "utf-8"), TaskConfig.class);
			} catch (UnsupportedEncodingException | JsonSyntaxException e) {
				logger.error("Gson deserialize error", e);
				return;
			}

			scheduledExecutorService.shutdownNow();
			try {
				scheduledExecutorService.awaitTermination(3, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				return;
			}

			TaskRunner taskRunner = new TaskRunner(taskConfig);
			int type = taskConfig.getType();
			switch (type) {
			case 0:
				scheduledExecutorService.schedule(taskRunner, taskConfig.getDelay(), taskConfig.getTimeUnit());
				break;
			case 1:
				scheduledExecutorService.scheduleAtFixedRate(taskRunner, taskConfig.getInitialDelay(),
						taskConfig.getDelay(), taskConfig.getTimeUnit());
				break;
			case 2:
				scheduledExecutorService.scheduleWithFixedDelay(taskRunner, taskConfig.getInitialDelay(),
						taskConfig.getDelay(), taskConfig.getTimeUnit());
				break;
			default:
				logger.error("The TaskConfig type is not supported: {}", type);
			}
		}
	}

	class TaskRunner implements Runnable {

		private TaskConfig taskConfig;

		public TaskRunner(TaskConfig taskConfig) {
			this.taskConfig = taskConfig;
		}

		@Override
		public void run() {
			Completed completed = new Completed();

			try {
				Running running = new Running();
				running.setRound(getLastRound());

				taskingZookeeper.delete(completedPath);
				taskingZookeeper.create(runningPath, gson.toJson(running), CreateMode.EPHEMERAL);

				// Prepare for business
				Class<?> appClass = application.getClass();
				ClassLoader appClassLoader = appClass.getClassLoader();
				Thread.currentThread().setContextClassLoader(appClassLoader);
				Object taskObj = appClass
						.getMethod("getTask", appClassLoader.loadClass("com.github.hebelala.tasking.api.Task"))
						.invoke(application, appClassLoader.loadClass(taskConfig.getClazz()));
				Object bzRequestObj = constructRequest(appClassLoader);

				// Run business code
				try {
					completed.setBzStartTime(System.currentTimeMillis());
					Object bzResponseObj = taskObj.getClass().getMethod("run", bzRequestObj.getClass()).invoke(taskObj,
							bzRequestObj);
					Response bzResponse = convertToResponse(bzResponseObj);
					completed.setBzResponse(bzResponse);
				} catch (Throwable t) {
					completed.setBzThrowableString(t.toString());
					logger.error("Run business code error", t);
				} finally {
					completed.setBzEndTime(System.currentTimeMillis());
				}
			} catch (Throwable t) {
				completed.setThrowableString(t.toString());
				logger.error("TaskRunner error", t);
			} finally {
				try {
					taskingZookeeper.multi(Op.create(completedPath, gson.toJson(completed).getBytes("utf-8"),
							ZooDefs.Ids.CREATOR_ALL_ACL, CreateMode.PERSISTENT), Op.delete(runningPath, -1));
				} catch (UnsupportedEncodingException | InterruptedException e) {
				}
			}
		}

		private int getLastRound() throws InterruptedException {
			String completedData = taskingZookeeper.get(completedPath, null);
			return completedData == null ? 0 : gson.fromJson(completedData, Completed.class).getRound();
		}

		@SuppressWarnings("unchecked")
		private Object constructRequest(ClassLoader appClassLoader)
				throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException {
			Class<?> requestClass = appClassLoader.loadClass("com.github.hebelala.tasking.api.RequestImpl");
			Object request = requestClass.newInstance();

			Field field = requestClass.getDeclaredField("namespace");
			field.setAccessible(true);
			field.set(request, namespace);

			field = requestClass.getDeclaredField("name");
			field.setAccessible(true);
			field.set(request, taskConfig.getName());

			field = requestClass.getDeclaredField("timeout");
			field.setAccessible(true);
			field.setLong(request, taskConfig.getTimeout());

			field = requestClass.getDeclaredField("parameters");
			field.setAccessible(true);
			((Map<String, String>) field.get(request)).putAll(taskConfig.getParameters());

			return request;
		}

		private Response convertToResponse(Object from) throws NoSuchFieldException, IllegalAccessException {
			if (from == null) {
				return null;
			}

			Response response = new Response();
			Class<?> fromCLass = from.getClass();

			Field fromField = fromCLass.getDeclaredField("status");
			fromField.setAccessible(true);
			Field toField = response.getClass().getDeclaredField("status");
			toField.setAccessible(true);
			toField.setInt(response, fromField.getInt(from));

			fromField = fromCLass.getDeclaredField("message");
			fromField.setAccessible(true);
			toField = response.getClass().getDeclaredField("message");
			toField.setAccessible(true);
			toField.set(response, fromField.get(from));

			return response;
		}

	}

}
