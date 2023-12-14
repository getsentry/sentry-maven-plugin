package io.sentry;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.*;
import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.NotNull;

@Named("sentry-artifact-resolver")
public class ArtifactResolver {

  @Inject ProjectDependenciesResolver resolver;

  public List<Artifact> resolveArtifactsForProject(
      final @NotNull MavenProject project, final @NotNull MavenSession session)
      throws DependencyResolutionException {
    final @NotNull DefaultDependencyResolutionRequest request =
        new DefaultDependencyResolutionRequest(project, session.getRepositorySession());
    final @NotNull DependencyResolutionResult result = resolver.resolve(request);

    return result.getDependencies().stream()
        .map(it -> it.getArtifact())
        .collect(Collectors.toList());
  }
}
