package io.sentry;

import static io.sentry.Constants.SENTRY_GROUP_ID;
import static io.sentry.Constants.SENTRY_PLUGIN_ARTIFACT_ID;
import static io.sentry.config.PluginConfig.DEFAULT_SKIP_STRING;
import static io.sentry.config.PluginConfig.DEFAULT_SKIP_VALIDATE_SDK_DEPENDENCY_VERSIONS_STRING;

import io.sentry.telemetry.SentryTelemetryService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mojo(name = "validateSdkDependencyVersions", defaultPhase = LifecyclePhase.VALIDATE)
public class ValidateSdkDependencyVersionsMojo extends AbstractMojo {

  @Parameter(defaultValue = DEFAULT_SKIP_STRING)
  private boolean skip;

  @Parameter(defaultValue = DEFAULT_SKIP_VALIDATE_SDK_DEPENDENCY_VERSIONS_STRING)
  private boolean skipValidateSdkDependencyVersions;

  private static final @NotNull Logger logger =
      LoggerFactory.getLogger(ValidateSdkDependencyVersionsMojo.class);

  private static final String ERROR_MESSAGE =
      "Detected inconsistency in Sentry dependency versions.";
  private static final String RESOLUTION_MESSAGE =
      "Please remove any explicit dependencies in the "
          + SENTRY_GROUP_ID
          + " group from your `pom.xml`, or ensure that all of them are present with the same version.";
  private static final String ESCAPE_HATCH_MESSAGE =
      "You can disable this check by setting `<skipValidateSdkDependencyVersions>true</skipValidateSdkDependencyVersions>` in your configuration for "
          + SENTRY_GROUP_ID
          + ":"
          + SENTRY_PLUGIN_ARTIFACT_ID
          + ".\nThis is not recommended, as mismatched dependency versions could lead to build time or run time failures and crashes.";

  @SuppressWarnings("NullAway")
  @Parameter(defaultValue = "${project}", readonly = true)
  private @NotNull MavenProject mavenProject;

  @SuppressWarnings("NullAway")
  @Parameter(defaultValue = "${session}", readonly = true)
  private @NotNull MavenSession mavenSession;

  @Inject protected @NotNull ArtifactResolver artifactResolver;

  @Override
  public void execute() throws MojoExecutionException {
    if (skip || skipValidateSdkDependencyVersions) {
      logger.info("Skipping Sentry SDK dependency versions validation.");
      return;
    }
    logger.info("Validating Sentry SDK dependency versions.");
    final @Nullable ISpan span =
        SentryTelemetryService.getInstance().startTask("validateSdkDependencyVersions");

    try {
      validateSdkDependencyVersions();
    } catch (MojoExecutionException e) {
      throw e;
    } catch (DependencyResolutionException e) {
      SentryTelemetryService.getInstance().captureError(e, "validateSdkDependencyVersions");
      throw new RuntimeException(e);
    } catch (Throwable t) {
      SentryTelemetryService.getInstance().captureError(t, "validateSdkDependencyVersions");
      throw t;
    } finally {
      SentryTelemetryService.getInstance().endTask(span);
    }
  }

  private void validateSdkDependencyVersions()
      throws MojoExecutionException, DependencyResolutionException {

    Map<String, List<Artifact>> versionToArtifacts = new HashMap<>();
    Set<Artifact> dependencies =
        new HashSet<>(artifactResolver.resolveArtifactsForProject(mavenProject, mavenSession));
    for (Artifact artifact : dependencies) {
      if (!artifact.getGroupId().equals(SENTRY_GROUP_ID)) {
        continue;
      }
      if (artifact.getArtifactId().equals(SENTRY_PLUGIN_ARTIFACT_ID)) {
        continue;
      }
      versionToArtifacts
          .computeIfAbsent(artifact.getVersion(), v -> new ArrayList<>())
          .add(artifact);
    }

    if (versionToArtifacts.size() > 1) {
      StringBuilder exceptionMessage = new StringBuilder(ERROR_MESSAGE).append('\n');
      versionToArtifacts.forEach(
          (version, artifacts) ->
              exceptionMessage.append(
                  String.format(
                      "Version %s required for: %s\n",
                      version,
                      artifacts.stream()
                          .map((artifact) -> artifact.getGroupId() + ":" + artifact.getArtifactId())
                          .collect(Collectors.joining(", ")))));
      exceptionMessage.append(RESOLUTION_MESSAGE).append("\n\n");
      exceptionMessage.append(ESCAPE_HATCH_MESSAGE);
      logger.error("Found inconsistency in Sentry SDK dependency versions.");
      throw new MojoExecutionException(exceptionMessage.toString());
    }

    logger.info("Sentry SDK dependency versions are all consistent.");
  }
}
