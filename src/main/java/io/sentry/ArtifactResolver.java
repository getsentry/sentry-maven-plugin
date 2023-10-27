package io.sentry;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.artifact.Artifact;

@Named("sentry-artifact-resolver")
public class ArtifactResolver {

  @Inject ProjectDependenciesResolver resolver;

  public List<Artifact> resolveArtifactsForProject(MavenProject project, MavenSession session)
      throws DependencyResolutionException {
    var request = new DefaultDependencyResolutionRequest(project, session.getRepositorySession());
    var result = resolver.resolve(request);

    return result.getDependencies().stream()
        .map(it -> it.getArtifact())
        .collect(Collectors.toList());
  }
}
