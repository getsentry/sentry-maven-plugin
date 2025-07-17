package io.sentry.cli;

import static io.sentry.SentryCliProvider.getCliPath;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
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
  private final @NotNull MavenProject mavenProject;
  private final @NotNull MavenSession mavenSession;
  private final @NotNull BuildPluginManager pluginManager;

  public SentryCliRunner(
      final boolean debugSentryCli,
      final @Nullable String sentryCliExecutablePath,
      final @NotNull MavenProject mavenProject,
      final @NotNull MavenSession mavenSession,
      final @NotNull BuildPluginManager pluginManager) {
    this.debugSentryCli = debugSentryCli;
    this.sentryCliExecutablePath = sentryCliExecutablePath;
    this.mavenProject = mavenProject;
    this.mavenSession = mavenSession;
    this.pluginManager = pluginManager;
  }

  public @Nullable String runSentryCli(
      final @NotNull String sentryCliCommand, final boolean failOnError)
      throws MojoExecutionException {
    final boolean isWindows = isWindows();

    final @NotNull String executable = isWindows ? "cmd.exe" : "/bin/sh";
    final @NotNull String cArg = isWindows ? "/c" : "-c";
    @Nullable File logFile = null;
    try {
      logFile = File.createTempFile("maven", "cli");

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
                          attribute("executable", executable),
                          attribute("failOnError", String.valueOf(failOnError)),
                          attribute("output", escape(logFile.getAbsolutePath()))),
                      element(name("arg"), attributes(attribute("value", cArg))),
                      element(
                          name("arg"),
                          attributes(
                              attribute(
                                  "value",
                                  wrapForWindows(
                                      escape(getCliPath(mavenProject, sentryCliExecutablePath))
                                          + " "
                                          + sentryCliCommand))))))),
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

  private static boolean isWindows() {
    final boolean isWindows =
        System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");
    return isWindows;
  }

  private @Nullable String wrapForWindows(final @Nullable String toWrap) {
    // Wrap whole command in double quotes as Windows cmd will remove the first and last double
    // quote
    if (toWrap != null && isWindows()) {
      return "\"" + toWrap + "\"";
    } else {
      return toWrap;
    }
  }

  public @Nullable String escape(final @Nullable String escapePath) {
    if (escapePath == null) {
      return null;
    }
    if (isWindows()) {
      // Wrap paths that contain a whitespace in double quotes
      // For some reason wrapping paths that do not contain a whitespace leads to an error
      if (escapePath.contains(" ")) {
        return "\"" + escapePath + "\"";
      }
      return escapePath;
    } else {
      return escapePath.replaceAll(" ", "\\\\ ");
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
