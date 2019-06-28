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
package com.github.hebelala.tasking.actor;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.hebelala.tasking.actor.app.ApplicationKeeper;
import com.github.hebelala.tasking.utils.CloseableUtils;

/**
 * TODO Hot deploy app supported
 *
 * @author hebelala
 */
public class Actor {

	private Logger logger;

	private com.github.hebelala.tasking.actor.entity.Actor actor;
	private ClassLoader actorClassLoader;
	private Map<String, URLClassLoader> appClassloaderMap = new HashMap<>();
	private Map<String, ApplicationKeeper> applicationKeeperMap = new HashMap<>();

	public static void main(String[] args) throws Exception {
		new Actor().start();
	}

	public void start() throws Exception {
		actorClassLoader = getClass().getClassLoader();
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(actorClassLoader);

			File taJar = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
			File libFile = taJar.getParentFile();

			setEnv(libFile);
			initLog();
			initActorEntity();
			initAppClassLoaders(libFile);
			initApplications();
		} finally {
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
	}

	private void setEnv(File libFile) {
		File logsFile = new File(libFile.getParent(), "logs");
		System.setProperty("tasking.log.dir", logsFile.getAbsolutePath());
	}

	private void initLog() {
		logger = LoggerFactory.getLogger(getClass());
	}

	private void initActorEntity() throws Exception {
		actor = new com.github.hebelala.tasking.actor.entity.Actor();

		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		if (networkInterfaces == null) {
			throw new Exception("Cannot find any network interfaces");
		}
		boolean find = false;
		outer: while (networkInterfaces.hasMoreElements()) {
			NetworkInterface networkInterface = networkInterfaces.nextElement();
			Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
			if (inetAddresses != null) {
				while (inetAddresses.hasMoreElements()) {
					InetAddress inetAddress = inetAddresses.nextElement();
					if (inetAddress.isSiteLocalAddress()) {
						actor.setHostAddress(inetAddress.getHostAddress());
						actor.setHostName(inetAddress.getHostName());
						find = true;
						break outer;
					}
				}
			}
		}
		if (!find) {
			throw new Exception("Cannot find any SiteLocalAddress");
		}

		actor.setStartTime(System.currentTimeMillis());
	}

	private void initAppClassLoaders(File libFile) throws MalformedURLException {
		File appsFile = new File(libFile.getParent(), "apps");
		if (appsFile.exists()) {
			File[] files = appsFile.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file.isDirectory()) {
						String namespace = file.getName();
						logger.info("Found the app, its namespace is {}", namespace);
						// Support load jar file and classes folder under the root of namespace
						List<URL> appUrlList = new ArrayList<>();
						File[] appFiles = file.listFiles();
						if (appFiles != null) {
							for (File temp : appFiles) {
								appUrlList.add(temp.toURI().toURL());
							}
						}
						appClassloaderMap.put(namespace, new URLClassLoader(
								appUrlList.toArray(new URL[appUrlList.size()]), actorClassLoader.getParent()));
					}
				}
			}
		} else {
			logger.info("The apps directory does not exists");
		}
	}

	private void initApplications() {
		Iterator<Map.Entry<String, URLClassLoader>> iterator = appClassloaderMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, URLClassLoader> next = iterator.next();
			String namespace = next.getKey();
			URLClassLoader appClassLoader = next.getValue();
			try {
				ApplicationKeeper applicationKeeper = new ApplicationKeeper(actor, appClassLoader);
				applicationKeeperMap.put(namespace, applicationKeeper);
			} catch (Throwable t) {
				logger.error(String.format("Init Application failed, its namespace is %s", namespace), t);
				iterator.remove();
				CloseableUtils.closeQuietly(appClassLoader);
			}
		}
	}

}
