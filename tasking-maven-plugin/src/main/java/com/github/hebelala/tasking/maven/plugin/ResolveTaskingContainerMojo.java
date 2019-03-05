package com.github.hebelala.tasking.maven.plugin;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;

/**
 * @author hebelala
 */
public abstract class ResolveTaskingContainerMojo extends ResolveArtifactMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    protected File resolveTC() throws MojoExecutionException {
        String taskingApiVersion = getTaskingApiVersion();
        if (taskingApiVersion == null) {
            throw new MojoExecutionException("Couldn't find dependency: com.github.hebelala:tasking-api:jar");
        }

        return resolveArtifact("com.github.hebelala", "tasking-container", taskingApiVersion, "jar", null);
    }

    private String getTaskingApiVersion() {
        List<Dependency> dependencies = project.getDependencies();
        if (dependencies != null) {
            for (Dependency dependency : dependencies) {
                if ("com.github.hebelala".equals(dependency.getGroupId()) && "tasking-api".equals(dependency.getArtifactId()) && "jar".equals(dependency.getType())) {
                    return dependency.getVersion();
                }
            }
        }
        return null;
    }

}
