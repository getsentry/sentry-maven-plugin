package io.sentry.autoinstall.spring;

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

public class Spring6InstallStrategy extends AbstractIntegrationInstaller {

  public static final @NotNull String SENTRY_SPRING_6_ID = "sentry-spring-jakarta";
  private static final @NotNull String SPRING_GROUP = "org.springframework";
  private static final @NotNull String SPRING_6_ID = "spring-core";

  public Spring6InstallStrategy() {
    this(LoggerFactory.getLogger(SentryInstaller.class));
  }

  public Spring6InstallStrategy(final @NotNull Logger logger) {
    super(logger);
  }

  @Override
  protected @NotNull Version minSupportedSentryVersion() {
    return Version.create(6, 7, 0);
  }

  @Override
  protected @Nullable Artifact findThirdPartyDependency(
      final @NotNull List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .filter(
            (dep) ->
                dep.getGroupId().equals(SPRING_GROUP) && dep.getArtifactId().equals(SPRING_6_ID))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected @Nullable Version minSupportedThirdPartyVersion() {
    return Version.create(6, 0, 0);
  }

  @Override
  protected boolean shouldInstallModule(AutoInstallState autoInstallState) {
    return autoInstallState.isInstallSpring();
  }

  @Override
  protected @NotNull String sentryModuleId() {
    return SENTRY_SPRING_6_ID;
  }
}
