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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;

import com.github.hebelala.tasking.utils.FileUtils;

/**
 * @author hebelala
 */
@Mojo(name = "zip", requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ZipMojo extends TaskingMojo {

	private final String type = "zip";

	@Component
	protected MavenProjectHelper projectHelper;

	@Override
	public void execute() throws MojoExecutionException {
		try {
			String packaging = validateAndGetPackaging();
			validateAndGetTaskingApiVersion();

			File appJar = new File(project.getBuild().getDirectory(),
					project.getBuild().getFinalName() + "." + packaging);

			if (!appJar.exists()) {
				throw new MojoExecutionException(
						"Please configure the package execution in tasking-maven-plugin, and then run the command: mvn clean install");
			}

			// get all jars that will be packaged
			List<File> appFileList = new ArrayList<>();
			appFileList.add(appJar);

			Set<Artifact> artifacts = project.getArtifacts();

			for (Artifact artifact : artifacts) {
				File file = artifact.getFile();
				if (file != null) {
					appFileList.add(file);
				}
			}

			String namespace = validateTaskingProperties(appFileList);

			// zip
			File file = new File(project.getBuild().getDirectory(),
					String.format("%s.%s", project.getBuild().getFinalName(), type));
			FileUtils.zipToFile(appFileList, file, namespace);

			projectHelper.attachArtifact(project, type, file);
		} catch (MojoExecutionException e) {
			throw e;
		} catch (Exception e) {
			getLog().error(e.getMessage(), e);
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
