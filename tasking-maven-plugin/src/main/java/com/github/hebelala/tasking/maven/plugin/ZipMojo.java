package com.github.hebelala.tasking.maven.plugin;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * @author hebelala
 */
@Mojo(name = "zip")
public class ZipMojo extends ResolveTaskingContainerMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File taskingContainer = resolveTC();
		getLog().info("resolved file: " + taskingContainer.getAbsoluteFile());
	}

}
