package io.sentry;

import static io.sentry.config.PluginConfig.*;

import io.sentry.telemetry.SentryTelemetryService;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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

@Mojo(name = "reportDependencies", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class ReportDependenciesMojo extends AbstractMojo {

  public static final @NotNull String EXTERNAL_MODULES_FILE = "external-modules.txt";

  private static final @NotNull Logger logger =
      LoggerFactory.getLogger(ReportDependenciesMojo.class);

  @SuppressWarnings("NullAway")
  @Parameter(defaultValue = "${project}", readonly = true)
  private @NotNull MavenProject mavenProject;

  @SuppressWarnings("NullAway")
  @Parameter(defaultValue = "${session}", readonly = true)
  private @NotNull MavenSession mavenSession;

  @SuppressWarnings("NullAway")
  @Parameter(property = "project.build.directory")
  private @NotNull File outputDirectory;

  @Inject protected @NotNull ArtifactResolver artifactResolver;

  @Parameter(defaultValue = DEFAULT_SKIP_STRING)
  private boolean skip;

  @Parameter(defaultValue = DEFAULT_SKIP_REPORT_DEPENDENCIES_STRING)
  private boolean skipReportDependencies;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip || skipReportDependencies) {
      logger.info("Report dependencies skipped");
      return;
    }

    final @Nullable ISpan span =
        SentryTelemetryService.getInstance().startTask("reportDependencies");
    try {
      collectDependencies(
          mavenProject, artifactResolver.resolveArtifactsForProject(mavenProject, mavenSession));
    } catch (IOException | DependencyResolutionException e) {
      SentryTelemetryService.getInstance().captureError(e, "reportDependencies");
      throw new RuntimeException(e);
    } catch (Throwable t) {
      SentryTelemetryService.getInstance().captureError(t, "reportDependencies");
      throw t;
    } finally {
      SentryTelemetryService.getInstance().endTask(span);
    }
  }

  private void collectDependencies(
      final @NotNull MavenProject mavenProject, final @NotNull List<Artifact> resolvedArtifacts)
      throws IOException {
    final @NotNull File sentryBuildDir = new File(outputDirectory, "external");
    if (!sentryBuildDir.exists()) {
      sentryBuildDir.mkdirs();
    }

    final @NotNull File modulesFile = new File(sentryBuildDir, EXTERNAL_MODULES_FILE);

    Files.write(
        modulesFile.toPath(),
        resolvedArtifacts.stream()
            .map(it -> it.getGroupId() + ":" + it.getArtifactId() + ":" + it.getVersion())
            .sorted()
            .collect(Collectors.joining("\n"))
            .getBytes(StandardCharsets.UTF_8),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);

    final @NotNull Resource resource = new Resource();
    resource.setDirectory(sentryBuildDir.getPath());
    resource.setFiltering(false);
    mavenProject.addResource(resource);
  }
}
