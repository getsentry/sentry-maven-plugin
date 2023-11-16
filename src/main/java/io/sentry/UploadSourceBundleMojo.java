package io.sentry;

import static io.sentry.config.PluginConfig.*;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import io.sentry.cli.SentryCliRunner;
import io.sentry.telemetry.SentryTelemetryService;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mojo(name = "uploadSourceBundle", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class UploadSourceBundleMojo extends AbstractMojo {

  private static Logger logger = LoggerFactory.getLogger(UploadSourceBundleMojo.class);

  @Parameter(property = "sentry.cli.debug", defaultValue = DEFAULT_DEBUG_SENTRY_CLI_STRING)
  private boolean debugSentryCli;

  @Parameter(property = "sentry.cli.path")
  private @Nullable String sentryCliExecutablePath;

  //  @Parameter(property = "sentry.cli.properties.path")
  //  private @Nullable String sentryPropertiesPath;

  @Parameter(property = "sentry.org")
  private @Nullable String org;

  @Parameter(property = "sentry.project")
  private @Nullable String project;

  @Parameter(property = "sentry.url")
  private @Nullable String url;

  @Parameter(property = "sentry.authToken")
  private @Nullable String authToken;

  @SuppressWarnings("NullAway")
  @Parameter(property = "project.build.directory")
  private @NotNull File outputDirectory;

  @SuppressWarnings("NullAway")
  @Parameter(defaultValue = "${project}", readonly = true)
  private @NotNull MavenProject mavenProject;

  @SuppressWarnings("NullAway")
  @Parameter(defaultValue = "${session}", readonly = true)
  private @NotNull MavenSession mavenSession;

  @Parameter(defaultValue = DEFAULT_SKIP_STRING)
  private boolean skip;

  @Parameter(defaultValue = DEFAULT_SKIP_SOURCE_BUNDLE_STRING)
  private boolean skipSourceBundle;

  @SuppressWarnings("NullAway")
  @Component
  private @NotNull BuildPluginManager pluginManager;

  @Override
  public void execute() throws MojoExecutionException {
    if (skip || skipSourceBundle) {
      logger.info("Upload Source Bundle skipped");
      return;
    }

    final @NotNull String bundleId = UUID.randomUUID().toString();
    final @NotNull File sourceBundleTargetDir = new File(sentryBuildDir(), "source-bundle");
    final @NotNull SentryCliRunner cliRunner =
        new SentryCliRunner(
            debugSentryCli, sentryCliExecutablePath, mavenProject, mavenSession, pluginManager);
    createDebugMetaPropertiesFile(bundleId);
    bundleSources(cliRunner, bundleId, sourceBundleTargetDir);
    uploadSourceBundle(cliRunner, sourceBundleTargetDir);
  }

  private @NotNull File sentryBuildDir() {
    return new File(outputDirectory, "sentry");
  }

  private void bundleSources(
      final @NotNull SentryCliRunner cliRunner,
      final @NotNull String bundleId,
      final @NotNull File sourceBundleTargetDir)
      throws MojoExecutionException {
    final @Nullable ISpan span = SentryTelemetryService.getInstance().startTask("bundleSources");
    try {
      if (!sourceBundleTargetDir.exists()) {
        sourceBundleTargetDir.mkdirs();
      }

      final @NotNull List<String> bundleSourcesCommand = new ArrayList<>();
      final @NotNull List<String> sourceRoots = mavenProject.getCompileSourceRoots();

      if (sourceRoots != null && sourceRoots.size() > 0) {
        final @Nullable String sourceRoot = sourceRoots.get(0);
        if (sourceRoots.size() > 1) {
          logger.warn("There's more than one source root, using {}", sourceRoot);
        }

        if (debugSentryCli) {
          bundleSourcesCommand.add("--log-level=debug");
        }

        final @NotNull List<String> tracingArgs = SentryTelemetryService.getInstance().traceCli();
        for (final @NotNull String tracingArg : tracingArgs) {
          bundleSourcesCommand.add(tracingArg);
        }

        /*
         * TODO maybe at some point copy all of them into one dir and pass that to
         *  sentry-cli or allow sentry-cli to take more than one dir
         */
        logger.debug("Bundling sources located in {}", sourceRoot);

        if (url != null) {
          bundleSourcesCommand.add("--url=" + url);
        }
        if (authToken != null) {
          bundleSourcesCommand.add("--auth-token=" + authToken);
        }

        bundleSourcesCommand.add("debug-files");
        bundleSourcesCommand.add("bundle-jvm");
        bundleSourcesCommand.add("--output=" + sourceBundleTargetDir.getAbsolutePath());
        bundleSourcesCommand.add("--debug-id=" + bundleId);
        if (org != null) {
          bundleSourcesCommand.add("--org=" + org);
        }
        if (project != null) {
          bundleSourcesCommand.add("--project=" + project);
        }
        bundleSourcesCommand.add(sourceRoot);

        cliRunner.runSentryCli(String.join(" ", bundleSourcesCommand), true);
      } else {
        throw new MojoExecutionException("Unable to find source root");
      }
    } catch (Throwable t) {
      SentryTelemetryService.getInstance().captureError(t, "bundleSources");
      throw t;
    } finally {
      SentryTelemetryService.getInstance().endTask(span);
    }
  }

  private void uploadSourceBundle(
      final @NotNull SentryCliRunner cliRunner, final @NotNull File sourceBundleTargetDir)
      throws MojoExecutionException {
    final @Nullable ISpan span =
        SentryTelemetryService.getInstance().startTask("uploadSourceBundle");
    try {
      final @NotNull List<String> command = new ArrayList<>();

      if (debugSentryCli) {
        command.add("--log-level=debug");
      }

      final @NotNull List<String> tracingArgs = SentryTelemetryService.getInstance().traceCli();
      for (final @NotNull String tracingArg : tracingArgs) {
        command.add(tracingArg);
      }

      if (url != null) {
        command.add("--url=" + url);
      }
      if (authToken != null) {
        command.add("--auth-token=" + authToken);
      }

      command.add("debug-files");
      command.add("upload");
      command.add("--type=jvm");
      if (org != null) {
        command.add("--org=" + org);
      }
      if (project != null) {
        command.add("--project=" + project);
      }
      command.add(sourceBundleTargetDir.getAbsolutePath());

      cliRunner.runSentryCli(String.join(" ", command), true);
    } catch (Throwable t) {
      SentryTelemetryService.getInstance().captureError(t, "uploadSourceBundle");
      throw t;
    } finally {
      SentryTelemetryService.getInstance().endTask(span);
    }
  }

  private void createDebugMetaPropertiesFile(final @NotNull String bundleId)
      throws MojoExecutionException {
    final @Nullable ISpan span =
        SentryTelemetryService.getInstance().startTask("createDebugMetaPropertiesFile");
    final @NotNull File sentryBuildDir = new File(sentryBuildDir(), "properties");
    if (!sentryBuildDir.exists()) {
      sentryBuildDir.mkdirs();
    }

    final @NotNull File debugMetaFile = new File(sentryBuildDir, "sentry-debug-meta.properties");
    final @NotNull Properties properties = createDebugMetaProperties(bundleId);

    try (final @NotNull BufferedWriter fileWriter =
            Files.newBufferedWriter(debugMetaFile.toPath(), Charset.defaultCharset())) {
      properties.store(fileWriter, "Generated by sentry-maven-plugin");

      final @NotNull Resource resource = new Resource();
      resource.setDirectory(sentryBuildDir.getPath());
      resource.setFiltering(false);
      mavenProject.addResource(resource);
    } catch (IOException e) {
      SentryTelemetryService.getInstance().captureError(e, "createDebugMetaPropertiesFile");
      throw new MojoExecutionException("Error creating file " + debugMetaFile, e);
    } catch (Throwable t) {
      SentryTelemetryService.getInstance().captureError(t, "createDebugMetaPropertiesFile");
      throw t;
    } finally {
      SentryTelemetryService.getInstance().endTask(span);
    }
  }

  private @NotNull Properties createDebugMetaProperties(final @NotNull String bundleId) {
    final @NotNull Properties properties = new Properties();

    properties.setProperty("io.sentry.bundle-ids", bundleId);
    properties.setProperty("io.sentry.build-tool", "maven");

    return properties;
  }
}
