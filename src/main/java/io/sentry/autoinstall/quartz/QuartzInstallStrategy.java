package io.sentry.autoinstall.quartz;

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

public class QuartzInstallStrategy extends AbstractIntegrationInstaller {
  private static final @NotNull String QUARTZ_GROUP = "org.quartz-scheduler";
  private static final @NotNull String QUARTZ_ID = "quartz";
  public static final @NotNull String SENTRY_QUARTZ_ID = "sentry-quartz";

  public QuartzInstallStrategy() {
    this(LoggerFactory.getLogger(SentryInstaller.class));
  }

  public QuartzInstallStrategy(final @NotNull Logger logger) {
    super(logger);
  }

  @Override
  protected @Nullable Artifact findThirdPartyDependency(
      final @NotNull List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .filter(
            (dep) -> dep.getGroupId().equals(QUARTZ_GROUP) && dep.getArtifactId().equals(QUARTZ_ID))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected boolean shouldInstallModule(final @NotNull AutoInstallState autoInstallState) {
    return autoInstallState.isInstallQuartz();
  }

  @Override
  protected @NotNull Version minSupportedSentryVersion() {
    return Version.create(6, 30, 0);
  }

  @Override
  protected @NotNull String sentryModuleId() {
    return SENTRY_QUARTZ_ID;
  }
}
