package io.sentry.autoinstall;

import static io.sentry.autoinstall.Constants.SENTRY_ARTIFACT_ID;
import static io.sentry.autoinstall.Constants.SENTRY_GROUP_ID;

import io.sentry.autoinstall.util.SdkVersionInfo;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentryInstaller {
  private final Logger logger;

  public SentryInstaller() {
    this(LoggerFactory.getLogger(SentryInstaller.class));
  }

  public SentryInstaller(Logger logger) {
    this.logger = logger;
  }

  public String install(List<Dependency> dependencyList, List<Artifact> resolvedArtifacts) {

    Artifact sentryDependency =
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
      String sentryVersion = SdkVersionInfo.getSentryVersion();

      if (sentryVersion == null) {
        logger.error("Unable to load sentry version, Sentry SDK cannot be auto-installed");
        return null;
      }

      logger.info("Installing Sentry with version " + sentryVersion);
      Dependency newDep = new Dependency();
      newDep.setGroupId(SENTRY_GROUP_ID);
      newDep.setArtifactId(SENTRY_ARTIFACT_ID);
      newDep.setVersion(sentryVersion);

      dependencyList.add(newDep);
      return sentryVersion;
    }
  }
}
