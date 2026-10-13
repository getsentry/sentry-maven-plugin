package io.sentry.cli;

import static io.sentry.SentryCliProvider.getCliPath;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentryCliRunner {

  private static Logger logger = LoggerFactory.getLogger(SentryCliRunner.class);

  private final boolean debugSentryCli;
  private final @Nullable String sentryCliExecutablePath;
  private final @Nullable String authToken;
  private final @NotNull MavenProject mavenProject;
  private final @NotNull MavenSession mavenSession;
  private final @NotNull BuildPluginManager pluginManager;

  public SentryCliRunner(
      final boolean debugSentryCli,
      final @Nullable String sentryCliExecutablePath,
      final @Nullable String authToken,
      final @NotNull MavenProject mavenProject,
      final @NotNull MavenSession mavenSession,
      final @NotNull BuildPluginManager pluginManager) {
    this.debugSentryCli = debugSentryCli;
    this.sentryCliExecutablePath = sentryCliExecutablePath;
    this.authToken = authToken;
    this.mavenProject = mavenProject;
    this.mavenSession = mavenSession;
    this.pluginManager = pluginManager;
  }

  /**
   * Runs sentry-cli with the given arguments. The CLI is executed directly, without a shell, and
   * each argument is passed as a separate argv entry, so arguments must not be quoted or escaped.
   * The auth token is passed via the SENTRY_AUTH_TOKEN environment variable rather than as an
   * argument, so it does not show up in process listings.
   */
  public @Nullable String runSentryCli(final @NotNull List<String> args, final boolean failOnError)
      throws MojoExecutionException {
    @Nullable File logFile = null;
    try {
      logFile = File.createTempFile("maven", "cli");

      final @NotNull List<Element> execElements = new ArrayList<>();
      if (authToken != null) {
        execElements.add(
            element(
                name("env"),
                attributes(attribute("key", "SENTRY_AUTH_TOKEN"), attribute("value", authToken))));
      }
      for (final @NotNull String arg : args) {
        execElements.add(element(name("arg"), attributes(attribute("value", arg))));
      }

      executeMojo(
          plugin(
              groupId("org.apache.maven.plugins"),
              artifactId("maven-antrun-plugin"),
              version("3.1.0")),
          goal("run"),
          configuration(
              element(
                  name("target"),
                  element(
                      name("exec"),
                      attributes(
                          attribute(
                              "executable", getCliPath(mavenProject, sentryCliExecutablePath)),
                          attribute("failOnError", String.valueOf(failOnError)),
                          attribute("output", logFile.getAbsolutePath())),
                      execElements.toArray(new Element[0])))),
          executionEnvironment(mavenProject, mavenSession, pluginManager));

      return collectAndMaybePrintOutput(logFile, debugSentryCli);
    } catch (MojoExecutionException e) {
      logger.error("Error while attempting to run Sentry CLI: ", e);
      if (logFile != null) {
        final @Nullable String output = collectAndMaybePrintOutput(logFile, true);
        if (output != null) {
          final @NotNull CliFailureReason failureReason = failureReasonFromCliOutput(output);
          throw new SentryCliException(failureReason);
        }
      }
      throw e;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private @Nullable String collectAndMaybePrintOutput(
      final @NotNull File logFile, final boolean shouldPrint) {
    try {
      final @NotNull String output = new String(Files.readAllBytes(logFile.toPath()));
      if (shouldPrint) {
        logger.info(output);
      }
      return output;
    } catch (IOException e) {
      logger.error("Failed to read sentry-cli output file.", e);
    }
    return null;
  }

  private @NotNull CliFailureReason failureReasonFromCliOutput(final @NotNull String outputString) {
    logger.error(outputString);

    if (outputString.contains("error: resource not found")) {
      return CliFailureReason.OUTDATED;
    }
    if (outputString.contains("error: An organization slug is required")) {
      return CliFailureReason.ORG_SLUG;
    }
    if (outputString.contains("error: A project slug is required")) {
      return CliFailureReason.PROJECT_SLUG;
    }
    if (outputString.contains("error: Failed to parse org auth token")) {
      return CliFailureReason.INVALID_ORG_AUTH_TOKEN;
    }
    if (outputString.contains("error: API request failed")
        && outputString.contains("Invalid token (http status:")) {
      return CliFailureReason.INVALID_TOKEN;
    }
    return CliFailureReason.UNKNOWN;
  }
}
