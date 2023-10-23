package io.sentry;

import java.io.File;
import java.io.IOException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mojo(name = "reportDependencies", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ReportDependenciesMojo extends AbstractMojo {

  public static final String EXTERNAL_MODULES_FILE = "external-modules.txt";

  private static Logger logger = LoggerFactory.getLogger(ReportDependenciesMojo.class);

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject mavenProject;

  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession mavenSession;

  @Parameter(property = "project.build.directory")
  private File outputDirectory;

  @Inject protected ArtifactResolver artifactResolver;

  @Parameter(defaultValue = "false")
  private boolean skip;

  @Parameter(defaultValue = "false")
  private boolean skipReportDependencies;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip || skipReportDependencies) {
      logger.info("Report dependencies skipped");
      return;
    }

    try {
      collectDependencies(
          mavenProject, artifactResolver.resolveArtifactsForProject(mavenProject, mavenSession));
    } catch (IOException | DependencyResolutionException e) {
      throw new RuntimeException(e);
    }
  }

  private void collectDependencies(MavenProject mavenProject, List<Artifact> resolvedArtifacts)
      throws IOException {
    File sentryBuildDir = new File(outputDirectory, "external");
    if (!sentryBuildDir.exists()) {
      sentryBuildDir.mkdirs();
    }

    File modulesFile = new File(sentryBuildDir, EXTERNAL_MODULES_FILE);

    Files.write(
        modulesFile.toPath(),
        resolvedArtifacts.stream()
            .map(it -> it.getGroupId() + ":" + it.getArtifactId() + ":" + it.getVersion())
            .sorted()
            .collect(Collectors.toList()),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING);

    final Resource resource = new Resource();
    resource.setDirectory(sentryBuildDir.getPath());
    resource.setFiltering(false);
    mavenProject.addResource(resource);
  }
}
