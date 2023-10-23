package io.sentry.autoinstall.spring;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.autoinstall.SentryInstaller;
import io.sentry.semver.Version;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Spring6InstallStrategy extends AbstractIntegrationInstaller {

  public static final String SENTRY_SPRING_6_ID = "sentry-spring-jakarta";
  private static final String SPRING_GROUP = "org.springframework";
  private static final String SPRING_6_ID = "spring-core";

  public Spring6InstallStrategy() {
    this(LoggerFactory.getLogger(SentryInstaller.class));
  }

  public Spring6InstallStrategy(Logger logger) {
    this.logger = logger;
  }

  protected Version minSupportedSentryVersion() {
    return Version.create(6, 7, 0);
  }

  @Override
  protected Artifact findThirdPartyDependency(List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .filter(
            (dep) ->
                dep.getGroupId().equals(SPRING_GROUP) && dep.getArtifactId().equals(SPRING_6_ID))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected Version minSupportedThirdPartyVersion() {
    return Version.create(6, 0, 0);
  }

  @Override
  protected boolean shouldInstallModule(AutoInstallState autoInstallState) {
    return autoInstallState.isInstallSpring();
  }

  @Override
  protected String sentryModuleId() {
    return SENTRY_SPRING_6_ID;
  }
}
