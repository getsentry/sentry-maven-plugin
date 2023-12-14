package io.sentry;

import java.io.*;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Helper class to find sentry-cli
 * Ported from sentry-android-gradle-plugin
 */
public class SentryCliProvider {

  private static final @NotNull Logger logger = LoggerFactory.getLogger(SentryCliProvider.class);

  public static @NotNull String getCliPath(
      final @NotNull MavenProject mavenProject, final @Nullable String cliPathParameter) {
    if (cliPathParameter != null && !cliPathParameter.isBlank()) {
      return cliPathParameter;
    }

    final @Nullable String pathFromProperties = searchCliInPropertiesFile(mavenProject);

    if (pathFromProperties != null && !pathFromProperties.isBlank()) {
      logger.info("Cli found in sentry properties, using " + pathFromProperties);
      return pathFromProperties;
    }

    final @Nullable String cliSuffix = getCliSuffix();

    if (cliSuffix != null && !cliSuffix.isBlank()) {
      logger.info("Looking for CLI with suffix " + cliSuffix);
      final @NotNull String resourcePath = "/bin/sentry-cli-" + cliSuffix;
      final @Nullable String cliAbsolutePath = searchCliInResources(resourcePath);

      if (cliAbsolutePath != null) {
        logger.info("Cli found in " + cliAbsolutePath);
        return cliAbsolutePath;
      }

      final @Nullable String cliTempPath = loadCliFromResourcesToTemp(resourcePath);

      if (cliTempPath != null) {
        logger.info("Cli found in .jar using " + cliTempPath);
        return cliTempPath;
      }
    }

    return "sentry-cli";
  }

  private static @Nullable String searchCliInPropertiesFile(
      final @NotNull MavenProject mavenProject) {
    @NotNull File propertiesFileToUse = new File(mavenProject.getBasedir(), "sentry.properties");

    if (!propertiesFileToUse.exists() && mavenProject.getParent() != null) {
      propertiesFileToUse = new File(mavenProject.getParent().getBasedir(), "sentry.properties");
    }

    try {
      final @NotNull Properties sentryProperties = new Properties();
      sentryProperties.load(new FileInputStream(propertiesFileToUse));

      return sentryProperties.getProperty("cli.executable");

    } catch (IOException e) {
      logger.info("Properties file not found");
      return null;
    }
  }

  private static @Nullable String getCliSuffix() {
    final @NotNull String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
    final @NotNull String osArch = System.getProperty("os.arch");

    if (osName.contains("mac")) {
      return "Darwin-universal";
    }

    if (osName.contains("linux")) {
      return osArch.equals("amd64") ? "Linux-x86_64" : "Linux-" + osArch;
    }

    if (osName.contains("win")) {
      return "Windows-i686.exe";
    }

    return null;
  }

  private static @Nullable String searchCliInResources(final @NotNull String resourcePath) {
    final @Nullable URL resourceUrl = SentryCliProvider.class.getResource(resourcePath);

    if (resourceUrl != null) {
      final @NotNull File resourceFile = new File(resourceUrl.getFile());
      if (resourceFile.exists()) {
        return resourceFile.getAbsolutePath();
      }
    }
    return null;
  }

  private static @Nullable String loadCliFromResourcesToTemp(final @NotNull String resourcePath) {

    try (final @Nullable InputStream inputStream =
            SentryCliProvider.class.getResourceAsStream(resourcePath)) {
      final @NotNull File tempFile = File.createTempFile(".sentry-cli", ".exe");
      tempFile.deleteOnExit();
      tempFile.setExecutable(true);

      final @NotNull FileOutputStream outputStream = new FileOutputStream(tempFile);

      if (inputStream != null) {
        inputStream.transferTo(outputStream);
        outputStream.close();
        return tempFile.getAbsolutePath();
      } else {
        return null;
      }

    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }
}
