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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.hebelala.tasking.utils.CloseableUtils;
import com.github.hebelala.tasking.utils.FileUtils;

/**
 * @author hebelala
 */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.COMPILE)
public class RunMojo extends ResolveArtifactMojo {

	protected File taskingDir = new File(System.getProperty("user.home"), ".tasking");
	protected File cachesDir = new File(taskingDir, "caches");
	protected File lockFile = new File(taskingDir, "lock");

	public RunMojo() {
		cachesDir.mkdirs();
	}

	@Override
	public void execute() throws MojoExecutionException {
		RandomAccessFile randomAccessFile = null;
		FileLock fileLock = null;
		try {
			randomAccessFile = new RandomAccessFile(lockFile, "rw");
			FileChannel channel = randomAccessFile.getChannel();
			fileLock = channel.tryLock();
			if (fileLock == null) {
				getLog().info("Wait other tasking:run process completed...");
				fileLock = channel.lock();
			}

			validateAndGetPackaging();
			String taskingApiVersion = validateAndGetTaskingApiVersion();

			// If tasking-api is SNAPSHOT, or the cache is not existing, then download
			// tasking-container
			// and cover it to cache.
			File tcFileCached = new File(cachesDir, "tasking-container-" + taskingApiVersion);
			if (taskingApiVersion.toUpperCase().endsWith("SNAPSHOT") || !tcFileCached.exists()) {
				File tcFile = resolveArtifact("com.github.hebelala", "tasking-container", taskingApiVersion, "zip",
						"zip");
				FileUtils.delete(tcFileCached);

				FileUtils.unzipToDirectory(tcFile, cachesDir);
			}

			// Delete apps, because it has children with ran before.
			File appsFile = new File(tcFileCached, "apps");
			FileUtils.delete(appsFile);

			// Deploy the current project app to apps
			deployApp(appsFile);
		} catch (MojoExecutionException e) {
			throw e;
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			CloseableUtils.closeQuietly(fileLock);
			CloseableUtils.closeQuietly(randomAccessFile);
		}
	}

	private void deployApp(File appsFile) throws MojoExecutionException {
		try {
			List<File> appFileList = new ArrayList<>();
			Set<Artifact> artifacts = project.getArtifacts();

			for (Artifact artifact : artifacts) {
				File file = artifact.getFile();
				if (file != null) {
					appFileList.add(file);
				}
			}
			// add output directory
			appFileList.add(new File(project.getBuild().getOutputDirectory()));

			String namespace = validateTaskingProperties(appFileList);

			File appRoot = new File(appsFile, namespace);
			for (File file : appFileList) {
				FileUtils.copyToDirectory(file, appRoot);
			}
		} catch (MojoExecutionException e) {
			throw e;
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
