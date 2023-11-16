package io.sentry.autoinstall.logback;

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

public class LogbackInstallStrategy extends AbstractIntegrationInstaller {
  private static final @NotNull String LOGBACK_GROUP = "ch.qos.logback";
  private static final @NotNull String LOGBACK_ID = "logback-classic";
  public static final @NotNull String SENTRY_LOGBACK_ID = "sentry-logback";

  public LogbackInstallStrategy() {
    this(LoggerFactory.getLogger(SentryInstaller.class));
  }

  public LogbackInstallStrategy(final @NotNull Logger logger) {
    super(logger);
  }

  @Override
  protected @Nullable Artifact findThirdPartyDependency(
      final @NotNull List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .filter(
            (dep) ->
                dep.getGroupId().equals(LOGBACK_GROUP) && dep.getArtifactId().equals(LOGBACK_ID))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected boolean shouldInstallModule(final @NotNull AutoInstallState autoInstallState) {
    return autoInstallState.isInstallLogback();
  }

  @Override
  protected @Nullable Version minSupportedThirdPartyVersion() {
    return Version.create(1, 0, 0);
  }

  @Override
  protected @NotNull Version minSupportedSentryVersion() {
    return Version.create(6, 25, 2);
  }

  @Override
  protected @NotNull String sentryModuleId() {
    return SENTRY_LOGBACK_ID;
  }
}
