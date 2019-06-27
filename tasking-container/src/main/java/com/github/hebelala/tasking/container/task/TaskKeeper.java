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

import static com.github.hebelala.tasking.container.task.entity.ExecutionStat.BZ_COMPLETED;
import static com.github.hebelala.tasking.container.task.entity.ExecutionStat.BZ_RUNNING;
import static com.github.hebelala.tasking.container.task.entity.ExecutionStat.FAILED;
import static com.github.hebelala.tasking.container.task.entity.ExecutionStat.INTERRUPTED;
import static com.github.hebelala.tasking.container.task.entity.ExecutionStat.RUNNING;
import static com.github.hebelala.tasking.container.task.entity.ExecutionStat.SUCCESSFUL;
import static com.github.hebelala.tasking.container.task.entity.ExecutionStat.TIMEOUT;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hebelala.tasking.api.Response;
import com.github.hebelala.tasking.container.Server;
import com.github.hebelala.tasking.container.task.entity.Business;
import com.github.hebelala.tasking.container.task.entity.Execution;
import com.github.hebelala.tasking.container.task.entity.ExecutionStat;
import com.github.hebelala.tasking.container.task.entity.Status;
import com.github.hebelala.tasking.container.task.entity.TaskType;
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

	private TaskMonitor taskMonitor;

	private String executionPath;

	private volatile boolean closed;
	private Lock lock = new ReentrantLock();

	private Gson gson = new Gson();

	public TaskKeeper(Server server, String namespace, String name, TaskingZookeeper taskingZookeeper,
			Object application) {
		this.server = server;
		this.namespace = namespace;
		this.name = name;
		this.taskingZookeeper = taskingZookeeper;
		this.application = application;

		String taskPath = String.format("/%s/%s", namespace, name);
		this.executionPath = String.format("/%s/execution", taskPath);

		// TODO leader monitor, just leader can do business
		taskMonitor = new TaskMonitor(taskPath, null);
		taskingZookeeper.register(taskMonitor);
	}

	public void interrupt() {
		try {
			lock.lockInterruptibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
		try {
			if (closed) {
				return;
			}
			if (taskMonitor != null) {
				taskMonitor.interrupt();
			}
		} finally {
			lock.unlock();
		}
	}

	public void close() {
		try {
			lock.lockInterruptibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
		try {
			if (closed) {
				return;
			}
			if (taskMonitor != null) {
				taskMonitor.close();
			}
		} finally {
			lock.unlock();
		}
	}

	class TaskMonitor extends DataMonitor {

		private volatile TaskScheduledExecutor taskScheduledExecutor;

		public TaskMonitor(String path, byte[] initData) {
			super(path, initData);
		}

		@Override
		protected void changed(byte[] data) {
			try {
				lock.lockInterruptibly();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			try {
				// If TaskKeeper.this.closed, then definitely super.closed
				if (super.closed) {
					return;
				}
				TaskConfig taskConfig;
				try {
					taskConfig = gson.fromJson(new String(data, "utf-8"), TaskConfig.class);
				} catch (UnsupportedEncodingException | JsonSyntaxException e) {
					logger.error("Gson deserialize error", e);
					return;
				}
				if (taskScheduledExecutor != null) {
					try {
						taskScheduledExecutor.close();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
				taskScheduledExecutor = new TaskScheduledExecutor(taskConfig);
				taskScheduledExecutor.start();
			} finally {
				lock.unlock();
			}
		}

		@Override
		public void close() {
			/*
			 * Unnecessary lock, because this method is used by TaskKeeper.this.close() that
			 * has lock
			 */
			super.close();
			if (taskScheduledExecutor != null) {
				try {
					taskScheduledExecutor.close();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
			}
		}

		public void interrupt() {
			try {
				lock.lockInterruptibly();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
			try {
				if (taskScheduledExecutor != null) {
					try {
						taskScheduledExecutor.interrupt();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
			} finally {
				lock.unlock();
			}
		}
	}

	class TaskScheduledExecutor {

		private TaskConfig taskConfig;
		private ScheduledExecutorService scheduledExecutor;
		private TaskRunner taskRunner;

		public TaskScheduledExecutor(TaskConfig taskConfig) {
			this.taskConfig = taskConfig;
		}

		public void start() {
			scheduledExecutor = Executors.newScheduledThreadPool(1, (r) -> {
				Thread thread = new Thread(r);
				thread.setName(String.format("task-scheduledExecutor-%s-%s", namespace, name));
				return thread;
			});

			String type = taskConfig.getType();
			TaskType taskType;
			try {
				taskType = TaskType.valueOf(type);
			} catch (IllegalArgumentException e) {
				logger.error("The TaskConfig type is unrecognized: {}", type);
				return;
			}

			taskRunner = new TaskRunner();

			switch (taskType) {
			case ONE_SHORT:
				scheduledExecutor.schedule(taskRunner, taskConfig.getDelay(), taskConfig.getTimeUnit());
				break;
			case AT_FIXED_RATE:
				scheduledExecutor.scheduleAtFixedRate(taskRunner, taskConfig.getInitialDelay(), taskConfig.getDelay(),
						taskConfig.getTimeUnit());
				break;
			case WITH_FIXED_DELAY:
				scheduledExecutor.scheduleWithFixedDelay(taskRunner, taskConfig.getInitialDelay(),
						taskConfig.getDelay(), taskConfig.getTimeUnit());
				break;
			default:
				logger.error("The TaskConfig type is not supported for now: {}", type);
			}
		}

		public void interrupt() throws InterruptedException {
			taskRunner.interrupt();
		}

		public void close() throws InterruptedException {
			scheduledExecutor.shutdown();
			while (!scheduledExecutor.isTerminated()) {
				taskRunner.interrupt();
				Thread.sleep(330L);
			}
		}

		class TaskRunner implements Runnable {

			private AtomicReference<ExecutionStat> executionStat = new AtomicReference<>();
			private volatile SimpleFutureTask simpleFutureTask;

			public void interrupt() throws InterruptedException {
				if (executionStat.compareAndSet(BZ_RUNNING, INTERRUPTED)) {
					SimpleFutureTask localSft = simpleFutureTask;
					if (localSft != null) {
						localSft.shutdown();
					}
				}
			}

			@Override
			public void run() {
				executionStat.set(RUNNING);
				Execution execution = createExecution();
				Stat stat = new Stat();
				try {
					persistRunning(execution, stat);

					BridgeInfo bridgeInfo = constructBridgeInfo();

					simpleFutureTask = new SimpleFutureTask(() -> {
						executionStat.compareAndSet(RUNNING, BZ_RUNNING);
						Thread.currentThread().setContextClassLoader(bridgeInfo.taskObj.getClass().getClassLoader());
						try {
							execution.getBusiness().setStartTime(System.currentTimeMillis());
							bridgeInfo.responseObj = bridgeInfo.taskObj.getClass()
									.getMethod("run", bridgeInfo.requestObj.getClass())
									.invoke(bridgeInfo.taskObj, bridgeInfo.requestObj);
						} catch (Throwable t) {
							ExecutionStat localStat = executionStat.get();
							if (localStat != TIMEOUT && localStat != INTERRUPTED) {
								execution.getBusiness().setMessage(t.toString());
								logger.error("Task business error", t);
							}
						} finally {
							execution.getBusiness().setEndTime(System.currentTimeMillis());
							executionStat.compareAndSet(BZ_RUNNING, BZ_COMPLETED);
						}
					});

					simpleFutureTask.start();

					try {
						if (!simpleFutureTask.await(taskConfig.getTimeout(), taskConfig.getTimeUnit())) {
							executionStat.compareAndSet(BZ_RUNNING, TIMEOUT);
						}

						if (executionStat.get() == BZ_COMPLETED) {
							try {
								execution.getBusiness().setResponse(convertToResponse(bridgeInfo.responseObj));
							} catch (NoSuchFieldException | IllegalAccessException e) {
								logger.error("Deserialize business error", e);
							}
						}
					} finally {
						simpleFutureTask.shutdown();
						simpleFutureTask = null;
					}

				} catch (Throwable t) {
					ExecutionStat localStat = executionStat.get();
					executionStat.set(FAILED);
					logger.error(String.format("Task runner error, current execution stat is %s", localStat), t);
				} finally {
					executionStat.compareAndSet(BZ_COMPLETED, SUCCESSFUL);
					try {
						persistCompleted(execution, stat);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			}

			private Execution createExecution() {
				Execution execution = new Execution();
				execution.setServer(server);

				Status status = new Status();
				status.setState(executionStat.get().name()); // running
				execution.setStatus(status);

				Business business = new Business();
				business.setThreadName(Thread.currentThread().getName());
				execution.setBusiness(business);

				return execution;
			}

			private void persistRunning(Execution execution, Stat stat) throws InterruptedException {
				for (;;) {
					String executionDataStr = taskingZookeeper.get(executionPath, stat);
					if (executionDataStr == null) { // The path is not existing
						String createResult = taskingZookeeper.create(executionPath, gson.toJson(execution),
								CreateMode.PERSISTENT);
						if (createResult == null) { // Created by other server, sleep and retry
							Thread.sleep(500L);
						} else { // Created by me
							stat = taskingZookeeper.exists(executionPath);
							if (stat != null) {
								break;
							}
							// Removed, retry now
						}
					} else {
						Execution lastExecution = gson.fromJson(executionDataStr, Execution.class);
						if ("running".equals(lastExecution.getStatus().getState())) { // It's running at other server
							Thread.sleep(500L);
						} else {
							execution.getStatus().setRound(lastExecution.getStatus().getRound() + 1);
							boolean updateResult = taskingZookeeper.update(executionPath, gson.toJson(execution),
									stat.getVersion());
							if (updateResult) {
								break;
							}
						}
					}
				}
			}

			private BridgeInfo constructBridgeInfo() throws ClassNotFoundException, NoSuchMethodException,
					InvocationTargetException, IllegalAccessException, NoSuchFieldException, InstantiationException {
				Class<?> appClass = application.getClass();
				ClassLoader appClassLoader = appClass.getClassLoader();
				ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(appClassLoader);
				try {
					Object taskObj = appClass
							.getMethod("getTask", appClassLoader.loadClass("com.github.hebelala.tasking.api.Task"))
							.invoke(application, appClassLoader.loadClass(taskConfig.getClazz()));
					Object requestObj = constructRequest(appClassLoader);
					return new BridgeInfo(taskObj, requestObj);
				} finally {
					Thread.currentThread().setContextClassLoader(oldClassLoader);
				}
			}

			@SuppressWarnings("unchecked")
			private Object constructRequest(ClassLoader appClassLoader) throws ClassNotFoundException,
					IllegalAccessException, InstantiationException, NoSuchFieldException {
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

			private void persistCompleted(Execution execution, Stat stat) throws InterruptedException {
				execution.getStatus().setState(executionStat.get().name());
				String executionDataStr = taskingZookeeper.get(executionPath, stat);
				if (executionDataStr == null) {
					logger.error("Try to persist completed, but the execution path is not existing");
				} else {
					boolean updateResult = taskingZookeeper.update(executionPath, gson.toJson(execution),
							stat.getVersion());
					if (!updateResult) {
						logger.error("Try to persist completed, but update failed");
					}
				}
			}

			class BridgeInfo {

				Object taskObj;
				Object requestObj;
				Object responseObj;

				public BridgeInfo(Object taskObj, Object requestObj) {
					this.taskObj = taskObj;
					this.requestObj = requestObj;
				}
			}

		}

	}

}
