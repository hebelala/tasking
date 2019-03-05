package com.github.hebelala.tasking.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.File;

/**
 * @author hebelala
 */
@Mojo(name = "zip")
public class ZipMojo extends ResolveTaskingContainerMojo {

    @Override
    public void execute() throws MojoExecutionException {
        File taskingContainer = resolveTC();
        getLog().info("resolved file: " + taskingContainer.getAbsoluteFile());
    }

}
