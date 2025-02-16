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

public class GraphqlInstallStrategy extends AbstractIntegrationInstaller {
  private static final @NotNull String GRAPHQL_GROUP = "com.graphql-java";
  private static final @NotNull String GRAPHQL_ID = "graphql-java";
  public static final @NotNull String SENTRY_GRAPHQL_ID = "sentry-graphql";

  public GraphqlInstallStrategy() {
    this(LoggerFactory.getLogger(GraphqlInstallStrategy.class));
  }

  public GraphqlInstallStrategy(final @NotNull Logger logger) {
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
  protected @Nullable Version maxSupportedThirdPartyVersion() {
    return Version.create(21, 9999, 9999);
  }

  @Override
  protected @NotNull Version minSupportedSentryVersion() {
    return Version.create(6, 25, 2);
  }

  @Override
  protected @NotNull String sentryModuleId() {
    return SENTRY_GRAPHQL_ID;
  }
}
