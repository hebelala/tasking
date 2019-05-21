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
package com.github.hebelala.tasking.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.github.hebelala.tasking.utils.CloseableUtils;
import com.github.hebelala.tasking.utils.StringUtils;

/**
 * @author hebelala
 */
public abstract class TaskingMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;

	protected String validateAndGetPackaging() throws MojoExecutionException {
		String packaging = project.getPackaging();
		if (!"jar".equals(packaging)) {
			throw new MojoExecutionException("The packaging must be the jar");
		}
		return packaging;
	}

	protected String validateAndGetTaskingApiVersion() throws MojoExecutionException {
		List<Dependency> dependencies = project.getDependencies();
		if (dependencies != null) {
			for (Dependency dependency : dependencies) {
				if ("com.github.hebelala".equals(dependency.getGroupId())
						&& "tasking-api".equals(dependency.getArtifactId()) && "jar".equals(dependency.getType())) {
					return dependency.getVersion();
				}
			}
		}
		throw new MojoExecutionException("Couldn't find dependency: com.github.hebelala:tasking-api:jar");
	}

	/**
	 * @return namespace
	 */
	protected String validateTaskingProperties(List<File> appFileList) throws IOException, MojoExecutionException {
		List<URL> urlList = new ArrayList<>();

		for (File file : appFileList) {
			urlList.add(file.toURI().toURL());
		}

		URLClassLoader appClassLoader = new URLClassLoader(urlList.toArray(new URL[urlList.size()]));
		try {
			Enumeration<URL> resources = appClassLoader.findResources("tasking.properties");
			URL url = null;
			if (resources != null) {
				int count = 0;
				while (resources.hasMoreElements()) {
					if (++count >= 2) {
						throw new MojoExecutionException("The tasking.properties count cannot exceed 2");
					}
					url = resources.nextElement();
				}
			}
			if (url == null) {
				throw new MojoExecutionException("Cannot find tasking.properties");
			}
			InputStream inputStream = url.openStream();
			try {
				Properties properties = new Properties();
				properties.load(inputStream);
				String app = properties.getProperty("app");
				if (StringUtils.isBlank(app)) {
					throw new MojoExecutionException("The app must be declared in tasking.properties");
				}
				String namespace = properties.getProperty("namespace");
				if (StringUtils.isBlank(namespace)) {
					throw new MojoExecutionException("The namespace must be declared in tasking.properties");
				}
				return namespace.trim();
			} finally {
				CloseableUtils.closeQuietly(inputStream);
			}
		} finally {
			appClassLoader.close();
		}
	}
}
