package io.sentry.autoinstall;

import static io.sentry.Constants.SENTRY_GROUP_ID;

import io.sentry.semver.Version;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractIntegrationInstaller {

  protected @NotNull org.slf4j.Logger logger;

  protected @Nullable Version minSupportedThirdPartyVersion() {
    return null;
  }

  protected @Nullable Version maxSupportedThirdPartyVersion() {
    return null;
  }

  protected @NotNull Version minSupportedSentryVersion() {
    return Version.create(0, 0, 0);
  }

  protected abstract @Nullable Artifact findThirdPartyDependency(
      final @NotNull List<Artifact> resolvedArtifacts);

  protected abstract boolean shouldInstallModule(final @NotNull AutoInstallState autoInstallState);

  protected abstract @NotNull String sentryModuleId();

  protected AbstractIntegrationInstaller(final @NotNull org.slf4j.Logger logger) {
    this.logger = logger;
  }

  public void install(
      final @NotNull List<Dependency> dependencyList,
      final @NotNull List<Artifact> resolvedArtifacts,
      final @NotNull AutoInstallState autoInstallState) {
    if (!shouldInstallModule(autoInstallState)) {
      logger.info(
          sentryModuleId() + " won't be installed because it was already installed directly");
      return;
    }

    final @NotNull String sentryVersion = autoInstallState.getSentryVersion();

    final @Nullable Artifact thirdPartyDependency = findThirdPartyDependency(resolvedArtifacts);

    if (thirdPartyDependency == null) {
      logger.info(
          sentryModuleId()
              + " won't be installed because its third party dependency could not be found ");
      return;
    }

    if (minSupportedThirdPartyVersion() != null) {
      final @NotNull Version thirdPartyVersion = parseVersion(thirdPartyDependency.getVersion());
      if (thirdPartyVersion.isLowerThan(minSupportedThirdPartyVersion())) {
        logger.info(
            sentryModuleId()
                + " won't be installed because the current version ("
                + thirdPartyVersion
                + ") is lower than the minimum supported version "
                + minSupportedThirdPartyVersion());
        return;
      }
    }

    if (maxSupportedThirdPartyVersion() != null) {
      final @NotNull Version thirdPartyVersion = parseVersion(thirdPartyDependency.getVersion());
      if (thirdPartyVersion.isGreaterThan(maxSupportedThirdPartyVersion())) {
        logger.info(
            sentryModuleId()
                + " won't be installed because the current version ("
                + thirdPartyVersion
                + ") is higher than the maximum supported version "
                + maxSupportedThirdPartyVersion());
        return;
      }
    }

    if (minSupportedSentryVersion().getMajor() > 0) {
      try {
        final @NotNull Version sentrySemVersion = Version.parseVersion(sentryVersion);
        if (sentrySemVersion.isLowerThan(minSupportedSentryVersion())) {
          logger.warn(
              sentryModuleId()
                  + " won't be installed because the current version ("
                  + sentrySemVersion
                  + ") is lower than the minimum supported sentry version "
                  + sentryVersion);
          return;
        }
      } catch (IllegalArgumentException ex) {
        logger.warn(
            sentryModuleId()
                + " won't be installed because the provided "
                + "sentry version ("
                + sentryVersion
                + ") could not be processed "
                + "as a semantic version.");
        return;
      }
    }

    logger.info("Installing " + sentryModuleId());
    final @NotNull Dependency newDep = new Dependency();
    newDep.setGroupId(SENTRY_GROUP_ID);
    newDep.setArtifactId(sentryModuleId());
    newDep.setVersion(sentryVersion);

    dependencyList.add(newDep);

    logger.info(sentryModuleId() + " was successfully installed with version: " + sentryVersion);
  }

  private @NotNull Version parseVersion(final @NotNull String version) {
    final @NotNull String suffix = ".RELEASE";
    return Version.parseVersion(
        version.endsWith(suffix)
            ? version.substring(0, version.length() - suffix.length())
            : version);
  }
}
