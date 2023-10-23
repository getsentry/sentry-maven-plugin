package io.sentry.autoinstall;

import static io.sentry.autoinstall.Constants.SENTRY_GROUP_ID;

import io.sentry.semver.Version;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.eclipse.aether.artifact.Artifact;

public abstract class AbstractIntegrationInstaller {

  protected org.slf4j.Logger logger;

  protected Version minSupportedThirdPartyVersion() {
    return null;
  }

  protected Version maxSupportedThirdPartyVersion() {
    return null;
  }

  protected Version minSupportedSentryVersion() {
    return Version.create(0, 0, 0);
  }

  protected abstract Artifact findThirdPartyDependency(List<Artifact> resolvedArtifacts);

  protected abstract boolean shouldInstallModule(AutoInstallState autoInstallState);

  protected abstract String sentryModuleId();

  public void install(
      List<Dependency> dependencyList,
      List<Artifact> resolvedArtifacts,
      AutoInstallState autoInstallState) {
    if (!shouldInstallModule(autoInstallState)) {
      logger.info(
          sentryModuleId() + " won't be installed because it was already installed directly");
      return;
    }

    String sentryVersion = autoInstallState.getSentryVersion();

    Artifact thirdPartyDependency = findThirdPartyDependency(resolvedArtifacts);

    if (thirdPartyDependency == null) {
      logger.info(
          sentryModuleId()
              + " won't be installed because its third party dependency could not be found ");
      return;
    }

    if (minSupportedThirdPartyVersion() != null) {
      if (parseVersion(thirdPartyDependency.getVersion())
          .isLowerThan(minSupportedThirdPartyVersion())) {
        logger.info(
            sentryModuleId()
                + " won't be installed because the current version is "
                + "lower than the minimum supported version "
                + minSupportedThirdPartyVersion());
        return;
      }
    }

    if (maxSupportedThirdPartyVersion() != null) {
      if (parseVersion(thirdPartyDependency.getVersion())
          .isGreaterThan(maxSupportedThirdPartyVersion())) {
        logger.info(
            sentryModuleId()
                + " won't be installed because the current version is "
                + "higher than the maximum supported version "
                + maxSupportedThirdPartyVersion());
        return;
      }
    }

    if (minSupportedSentryVersion().getMajor() > 0) {
      try {
        Version sentrySemVersion = Version.parseVersion(sentryVersion);
        if (sentrySemVersion.isLowerThan(minSupportedSentryVersion())) {
          logger.warn(
              sentryModuleId()
                  + " won't be installed because the current version is "
                  + "lower than the minimum supported sentry version "
                  + sentryVersion);
          return;
        }
      } catch (IllegalArgumentException ex) {
        logger.warn(
            sentryModuleId()
                + " won't be installed because the provided "
                + "sentry version($autoInstallState.sentryVersion) could not be processed "
                + "as a semantic version.");
        return;
      }
    }

    logger.info("Installing " + sentryModuleId());
    Dependency newDep = new Dependency();
    newDep.setGroupId(SENTRY_GROUP_ID);
    newDep.setArtifactId(sentryModuleId());
    newDep.setVersion(sentryVersion);

    dependencyList.add(newDep);

    logger.info(sentryModuleId() + " was successfully installed with version: " + sentryVersion);
  }

  private Version parseVersion(String version) {
    final String suffix = ".RELEASE";
    return Version.parseVersion(
        version.endsWith(suffix)
            ? version.substring(0, version.length() - suffix.length())
            : version);
  }
}
