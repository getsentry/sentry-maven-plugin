package io.sentry.autoinstall.graphql;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.semver.Version;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Graphql22InstallStrategy extends AbstractIntegrationInstaller {
  private static final @NotNull String GRAPHQL_GROUP = "com.graphql-java";
  private static final @NotNull String GRAPHQL_ID = "graphql-java";
  public static final @NotNull String SENTRY_GRAPHQL_22_ID = "sentry-graphql-22";

  public Graphql22InstallStrategy() {
    this(LoggerFactory.getLogger(Graphql22InstallStrategy.class));
  }

  public Graphql22InstallStrategy(final @NotNull Logger logger) {
    super(logger);
  }

  @Override
  protected @Nullable Artifact findThirdPartyDependency(
      final @NotNull List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .filter(
            (dep) ->
                dep.getGroupId().equals(GRAPHQL_GROUP) && dep.getArtifactId().equals(GRAPHQL_ID))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected boolean shouldInstallModule(final @NotNull AutoInstallState autoInstallState) {
    return autoInstallState.isInstallGraphql();
  }

  @Override
  protected @Nullable Version minSupportedThirdPartyVersion() {
    return Version.create(22, 0, 0);
  }

  @Override
  protected @NotNull Version minSupportedSentryVersion() {
    return Version.create(8, 0, 0);
  }

  @Override
  protected @NotNull String sentryModuleId() {
    return SENTRY_GRAPHQL_22_ID;
  }
}
