package com.github.hebelala.tasking.maven.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.ArtifactCoordinate;
import org.apache.maven.shared.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.dependencies.DependableCoordinate;

/**
 * @author hebelala
 */
public abstract class ResolveArtifactMojo extends AbstractMojo {

	@Parameter(defaultValue = "${session}", required = true, readonly = true)
	private MavenSession session;

	@Component
	private ArtifactResolver artifactResolver;

	@Component
	private ArtifactHandlerManager artifactHandlerManager;

	@Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
	private List<ArtifactRepository> pomRemoteRepositories;

	protected File resolveArtifact(String groupId, String artifactId, String version, String type, String classifier)
			throws MojoExecutionException, MojoFailureException {
		DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();
		coordinate.setGroupId(groupId);
		coordinate.setArtifactId(artifactId);
		coordinate.setVersion(version);
		coordinate.setType(type);
		coordinate.setClassifier(classifier);

		List<ArtifactRepository> repoList = new ArrayList<ArtifactRepository>();

		if (pomRemoteRepositories != null) {
			repoList.addAll(pomRemoteRepositories);
		}

		try {
			ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(
					session.getProjectBuildingRequest());
			buildingRequest.setRemoteRepositories(repoList);
			ArtifactResult resolveArtifact = artifactResolver.resolveArtifact(buildingRequest,
					toArtifactCoordinate(coordinate));
			return resolveArtifact.getArtifact().getFile();
		} catch (ArtifactResolverException e) {
			throw new MojoExecutionException("Couldn't download artifact, " + e.getMessage(), e);
		}
	}

	private ArtifactCoordinate toArtifactCoordinate(DependableCoordinate dependableCoordinate) {
		ArtifactHandler artifactHandler = artifactHandlerManager.getArtifactHandler(dependableCoordinate.getType());
		DefaultArtifactCoordinate artifactCoordinate = new DefaultArtifactCoordinate();
		artifactCoordinate.setGroupId(dependableCoordinate.getGroupId());
		artifactCoordinate.setArtifactId(dependableCoordinate.getArtifactId());
		artifactCoordinate.setVersion(dependableCoordinate.getVersion());
		artifactCoordinate.setClassifier(dependableCoordinate.getClassifier());
		artifactCoordinate.setExtension(artifactHandler.getExtension());
		return artifactCoordinate;
	}

}
