package io.sentry.autoinstall.profiler;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.semver.Version;
import java.util.List;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProfilerInstallStrategy extends AbstractIntegrationInstaller {
  public static final @NotNull String SENTRY_ASYNC_PROFILER_ID = "sentry-async-profiler";

  public ProfilerInstallStrategy() {
    this(LoggerFactory.getLogger(ProfilerInstallStrategy.class));
  }

  public ProfilerInstallStrategy(final @NotNull Logger logger) {
    super(logger);
  }

  @Override
  protected @Nullable Artifact findThirdPartyDependency(
      final @NotNull List<Artifact> resolvedArtifacts) {
    // Return a dummy artifact as there are no third-party dependencies required
    return new DefaultArtifact("dummy:dummy:1.0.0");
  }

  @Override
  protected boolean shouldInstallModule(final @NotNull AutoInstallState autoInstallState) {
    return autoInstallState.isInstallProfiler();
  }

  @Override
  protected @NotNull Version minSupportedSentryVersion() {
    return Version.create(8, 23, 0);
  }

  @Override
  protected @NotNull String sentryModuleId() {
    return SENTRY_ASYNC_PROFILER_ID;
  }
}
