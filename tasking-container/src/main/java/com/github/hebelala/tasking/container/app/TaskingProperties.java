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
package com.github.hebelala.tasking.container.app;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Properties;

import com.github.hebelala.tasking.utils.CloseableUtils;
import com.github.hebelala.tasking.utils.StringUtils;

/**
 * TODO Use yaml
 *
 * @author hebelala
 */
public class TaskingProperties {

	private String app;
	private String namespace;
	private String connectString;
	private String sessionTimeout;
	private String username;
	private String password;

	public TaskingProperties(String app, String namespace, String connectString, String sessionTimeout, String username,
			String password) {
		this.app = app;
		this.namespace = namespace;
		this.connectString = connectString;
		this.sessionTimeout = sessionTimeout;
		this.username = username;
		this.password = password;
	}

	public String getApp() {
		return app;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getConnectString() {
		return connectString;
	}

	public String getSessionTimeout() {
		return sessionTimeout;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public static class Loader {

		public static TaskingProperties load(URLClassLoader appClassLoader) {
			try {
				Enumeration<URL> resources = appClassLoader.findResources("tasking.properties");
				if (resources == null || !resources.hasMoreElements()) {
					throw new RuntimeException("Cannot find tasking.properties");
				}
				int count = 0;
				URL url = null;
				while (resources.hasMoreElements()) {
					if (++count >= 2) {
						throw new RuntimeException("The tasking.properties count cannot exceed 2");
					}
					url = resources.nextElement();
				}
				InputStream inputStream = url.openStream();
				try {
					Properties properties = new Properties();
					properties.load(inputStream);
					String app = checkExistingAndGet(properties, "app");
					String namespace = checkExistingAndGet(properties, "namespace");
					String connectString = checkExistingAndGet(properties, "connectString");
					String sessionTimeout = checkExistingAndGet(properties, "sessionTimeout");
					String username = checkExistingAndGet(properties, "username");
					String password = checkExistingAndGet(properties, "password");
					return new TaskingProperties(app, namespace, connectString, sessionTimeout, username, password);
				} finally {
					CloseableUtils.closeQuietly(inputStream);
				}
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage(), e);
			}
		}

		private static String checkExistingAndGet(Properties properties, String key) {
			String value = properties.getProperty(key);
			if (StringUtils.isBlank(value)) {
				throw new RuntimeException("The " + key + " must be declared in tasking.properties");
			}
			return value.trim();
		}
	}
}
