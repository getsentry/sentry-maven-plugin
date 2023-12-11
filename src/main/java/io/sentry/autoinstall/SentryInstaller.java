package io.sentry.autoinstall;

import static io.sentry.autoinstall.Constants.SENTRY_ARTIFACT_ID;
import static io.sentry.autoinstall.Constants.SENTRY_GROUP_ID;

import io.sentry.autoinstall.util.SdkVersionInfo;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentryInstaller {
  private final @NotNull Logger logger;

  public SentryInstaller() {
    this(LoggerFactory.getLogger(SentryInstaller.class));
  }

  public SentryInstaller(final @NotNull Logger logger) {
    this.logger = logger;
  }

  public @Nullable String install(
      final @NotNull List<Dependency> dependencyList,
      final @NotNull List<Artifact> resolvedArtifacts) {

    final @Nullable Artifact sentryDependency =
        resolvedArtifacts.stream()
            .filter(
                (dep) ->
                    dep.getGroupId().equals(SENTRY_GROUP_ID)
                        && dep.getArtifactId().equals(SENTRY_ARTIFACT_ID))
            .findFirst()
            .orElse(null);

    if (sentryDependency != null) {
      logger.info("Sentry already installed " + sentryDependency.getVersion());
      return sentryDependency.getVersion();
    } else {
      final @Nullable String sentryVersion = SdkVersionInfo.getSentryVersion();

      if (sentryVersion == null) {
        logger.error("Unable to load sentry version, Sentry SDK cannot be auto-installed");
        return null;
      }

      logger.info("Installing Sentry with version " + sentryVersion);
      final @NotNull Dependency newDep = new Dependency();
      newDep.setGroupId(SENTRY_GROUP_ID);
      newDep.setArtifactId(SENTRY_ARTIFACT_ID);
      newDep.setVersion(sentryVersion);

      dependencyList.add(newDep);
      return sentryVersion;
    }
  }
}
