package io.sentry.autoinstall.quartz;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.autoinstall.SentryInstaller;
import io.sentry.semver.Version;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzInstallStrategy extends AbstractIntegrationInstaller {
  private static final String QUARTZ_GROUP = "org.quartz-scheduler";
  private static final String QUARTZ_ID = "quartz";
  public static final String SENTRY_QUARTZ_ID = "sentry-quartz";

  public QuartzInstallStrategy() {
    this(LoggerFactory.getLogger(SentryInstaller.class));
  }

  public QuartzInstallStrategy(Logger logger) {
    this.logger = logger;
  }

  @Override
  protected Artifact findThirdPartyDependency(List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .filter(
            (dep) -> dep.getGroupId().equals(QUARTZ_GROUP) && dep.getArtifactId().equals(QUARTZ_ID))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected boolean shouldInstallModule(AutoInstallState autoInstallState) {
    return autoInstallState.isInstallQuartz();
  }

  @Override
  protected Version minSupportedSentryVersion() {
    return Version.create(6, 30, 0);
  }

  @Override
  protected String sentryModuleId() {
    return SENTRY_QUARTZ_ID;
  }
}
