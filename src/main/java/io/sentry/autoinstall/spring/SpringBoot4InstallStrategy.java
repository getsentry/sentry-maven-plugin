package io.sentry.autoinstall.spring;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.semver.Version;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringBoot4InstallStrategy extends AbstractIntegrationInstaller {

  public static final @NotNull String SENTRY_SPRING_BOOT_4_ID = "sentry-spring-boot-4";
  private static final @NotNull String SPRING_GROUP = "org.springframework.boot";
  private static final @NotNull String SPRING_BOOT_4_ID = "spring-boot-starter";

  public SpringBoot4InstallStrategy() {
    this(LoggerFactory.getLogger(SpringBoot4InstallStrategy.class));
  }

  public SpringBoot4InstallStrategy(final @NotNull Logger logger) {
    super(logger);
  }

  @Override
  protected @NotNull Version minSupportedSentryVersion() {
    return Version.create(8, 21, 0);
  }

  @Override
  protected @Nullable Artifact findThirdPartyDependency(
      final @NotNull List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .filter(
            (dep) ->
                dep.getGroupId().equals(SPRING_GROUP)
                    && dep.getArtifactId().equals(SPRING_BOOT_4_ID))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected @Nullable Version minSupportedThirdPartyVersion() {
    return Version.create(4, 0, 0, "M1");
  }

  @Override
  protected @Nullable Version maxSupportedThirdPartyVersion() {
    return Version.create(4, 9999, 9999);
  }

  @Override
  protected boolean shouldInstallModule(final @NotNull AutoInstallState autoInstallState) {
    return autoInstallState.isInstallSpring();
  }

  @Override
  protected @NotNull String sentryModuleId() {
    return SENTRY_SPRING_BOOT_4_ID;
  }
}
