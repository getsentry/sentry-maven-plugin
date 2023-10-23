package io.sentry.autoinstall.graphql;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.autoinstall.SentryInstaller;
import io.sentry.semver.Version;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GraphqlInstallStrategy extends AbstractIntegrationInstaller {
  private static final String GRAPHQL_GROUP = "com.graphql-java";
  private static final String GRAPHQL_ID = "graphql-java";
  public static final String SENTRY_GRAPHQL_ID = "sentry-graphql";

  public GraphqlInstallStrategy() {
    this(LoggerFactory.getLogger(SentryInstaller.class));
  }

  public GraphqlInstallStrategy(Logger logger) {
    this.logger = logger;
  }

  @Override
  protected Artifact findThirdPartyDependency(List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .filter(
            (dep) ->
                dep.getGroupId().equals(GRAPHQL_GROUP) && dep.getArtifactId().equals(GRAPHQL_ID))
        .findFirst()
        .orElse(null);
  }

  @Override
  protected boolean shouldInstallModule(AutoInstallState autoInstallState) {
    return autoInstallState.isInstallGraphql();
  }

  @Override
  protected Version minSupportedSentryVersion() {
    return Version.create(6, 25, 2);
  }

  @Override
  protected String sentryModuleId() {
    return SENTRY_GRAPHQL_ID;
  }
}
