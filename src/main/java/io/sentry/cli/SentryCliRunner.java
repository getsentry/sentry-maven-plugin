package io.sentry.cli;

import static io.sentry.SentryCliProvider.getCliPath;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
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

  public SentryCliRunner(
      final boolean debugSentryCli,
      final @Nullable String sentryCliExecutablePath,
      final @Nullable String authToken,
      final @NotNull MavenProject mavenProject) {
    this.debugSentryCli = debugSentryCli;
    this.sentryCliExecutablePath = sentryCliExecutablePath;
    this.authToken = authToken;
    this.mavenProject = mavenProject;
  }

  /**
   * Runs sentry-cli with the given arguments and returns its combined stdout and stderr output.
   *
   * <p>The CLI is executed directly, without a shell, and each argument is passed as a separate
   * argv entry, so arguments must not be quoted or escaped. The auth token is passed via the
   * SENTRY_AUTH_TOKEN environment variable of the child process only, so it does not show up in
   * process listings and is not written to any file.
   */
  public @Nullable String runSentryCli(final @NotNull List<String> args, final boolean failOnError)
      throws MojoExecutionException {
    final @NotNull List<String> command = new ArrayList<>();
    command.add(getCliPath(mavenProject, sentryCliExecutablePath));
    command.addAll(args);

    final @NotNull ProcessBuilder processBuilder =
        new ProcessBuilder(command).directory(mavenProject.getBasedir()).redirectErrorStream(true);
    if (authToken != null) {
      processBuilder.environment().put("SENTRY_AUTH_TOKEN", authToken);
    }

    final @NotNull String output;
    final int exitCode;
    try {
      final @NotNull Process process = processBuilder.start();
      output = readFully(process.getInputStream());
      exitCode = process.waitFor();
    } catch (IOException e) {
      logger.error("Error while attempting to run Sentry CLI: ", e);
      throw new MojoExecutionException("Failed to run Sentry CLI", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new MojoExecutionException("Interrupted while running Sentry CLI", e);
    }

    if (exitCode != 0 && failOnError) {
      logger.error("Sentry CLI exited with code {}", exitCode);
      throw new SentryCliException(failureReasonFromCliOutput(output));
    }

    if (debugSentryCli) {
      logger.info(output);
    }
    return output;
  }

  private static @NotNull String readFully(final @NotNull InputStream inputStream)
      throws IOException {
    try (final @NotNull InputStream in = inputStream) {
      final @NotNull ByteArrayOutputStream out = new ByteArrayOutputStream();
      final byte[] buffer = new byte[8192];
      int read;
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      return new String(out.toByteArray(), Charset.defaultCharset());
    }
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
