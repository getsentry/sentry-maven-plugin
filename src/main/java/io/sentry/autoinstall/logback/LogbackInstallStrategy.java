package io.sentry.autoinstall.logback;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.autoinstall.SentryInstaller;
import io.sentry.semver.Version;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackInstallStrategy extends AbstractIntegrationInstaller {
  private static final String LOGBACK_GROUP = "ch.qos.logback";
  private static final String LOGBACK_ID = "logback-classic";
  public static final String SENTRY_LOGBACK_ID = "sentry-logback";

  public LogbackInstallStrategy() {
    this(LoggerFactory.getLogger(SentryInstaller.class));
  }

  public LogbackInstallStrategy(Logger logger) {
    this.logger = logger;
  }

  @Override
  protected Artifact findThirdPartyDependency(List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .filter(
            (dep) ->
                dep.getGroupId().equals(LOGBACK_GROUP) && dep.getArtifactId().equals(LOGBACK_ID))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected boolean shouldInstallModule(AutoInstallState autoInstallState) {
    return autoInstallState.isInstallLogback();
  }

  @Override
  protected Version minSupportedThirdPartyVersion() {
    return Version.create(1, 0, 0);
  }

  @Override
  protected Version minSupportedSentryVersion() {
    return Version.create(6, 25, 2);
  }

  @Override
  protected String sentryModuleId() {
    return SENTRY_LOGBACK_ID;
  }
}
