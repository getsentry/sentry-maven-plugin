package io.sentry.autoinstall.log4j2;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.autoinstall.SentryInstaller;
import io.sentry.semver.Version;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Log4j2InstallStrategy extends AbstractIntegrationInstaller {
  private static final @NotNull String LOG4J2_GROUP = "org.apache.logging.log4j";
  private static final @NotNull String LOG4J2_ID = "log4j-api";
  public static final @NotNull String SENTRY_LOG4J2_ID = "sentry-log4j2";

  public Log4j2InstallStrategy() {
    this(LoggerFactory.getLogger(SentryInstaller.class));
  }

  public Log4j2InstallStrategy(final @NotNull Logger logger) {
    super(logger);
  }

  @Override
  protected @Nullable Artifact findThirdPartyDependency(
      final @NotNull List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .filter(
            (dep) -> dep.getGroupId().equals(LOG4J2_GROUP) && dep.getArtifactId().equals(LOG4J2_ID))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected boolean shouldInstallModule(final @NotNull AutoInstallState autoInstallState) {
    return autoInstallState.isInstallLog4j2();
  }

  @Override
  protected @Nullable Version minSupportedThirdPartyVersion() {
    return Version.create(2, 0, 0);
  }

  @Override
  protected @NotNull Version minSupportedSentryVersion() {
    return Version.create(6, 25, 2);
  }

  @Override
  protected @NotNull String sentryModuleId() {
    return SENTRY_LOG4J2_ID;
  }
}
