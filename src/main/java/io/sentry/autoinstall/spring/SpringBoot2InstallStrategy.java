package io.sentry.autoinstall.spring;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.autoinstall.SentryInstaller;
import io.sentry.semver.Version;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringBoot2InstallStrategy extends AbstractIntegrationInstaller {

  public static final String SENTRY_SPRING_BOOT_2_ID = "sentry-spring-boot";
  private static final String SPRING_GROUP = "org.springframework.boot";
  private static final String SPRING_BOOT_2_ID = "spring-boot-starter";

  public SpringBoot2InstallStrategy() {
    this(LoggerFactory.getLogger(SentryInstaller.class));
  }

  public SpringBoot2InstallStrategy(Logger logger) {
    this.logger = logger;
  }

  @Override
  protected Version minSupportedSentryVersion() {
    return Version.create(6, 28, 0);
  }

  @Override
  protected Artifact findThirdPartyDependency(List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .filter(
            (dep) ->
                dep.getGroupId().equals(SPRING_GROUP)
                    && dep.getArtifactId().equals(SPRING_BOOT_2_ID))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected Version minSupportedThirdPartyVersion() {
    return Version.create(2, 1, 0);
  }

  @Override
  protected Version maxSupportedThirdPartyVersion() {
    return Version.create(2, 9999, 9999);
  }

  @Override
  protected boolean shouldInstallModule(AutoInstallState autoInstallState) {
    return autoInstallState.isInstallSpring();
  }

  @Override
  protected String sentryModuleId() {
    return SENTRY_SPRING_BOOT_2_ID;
  }
}
