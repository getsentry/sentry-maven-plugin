package io.sentry.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluginConfig {
  public static final boolean DEFAULT_SKIP = false;
  public static final @NotNull String DEFAULT_SKIP_STRING = "false";
  public static final boolean DEFAULT_SKIP_REPORT_DEPENDENCIES = false;
  public static final @NotNull String DEFAULT_SKIP_REPORT_DEPENDENCIES_STRING = "false";
  public static final boolean DEFAULT_SKIP_AUTO_INSTALL = false;
  public static final @NotNull String DEFAULT_SKIP_AUTO_INSTALL_STRING = "false";
  public static final boolean DEFAULT_SKIP_SOURCE_BUNDLE = false;
  public static final @NotNull String DEFAULT_SKIP_SOURCE_BUNDLE_STRING = "false";
  public static final boolean DEFAULT_SKIP_TELEMETRY = false;
  public static final @NotNull String DEFAULT_SKIP_TELEMETRY_STRING = "false";
  public static final boolean DEFAULT_DEBUG_SENTRY_CLI = false;
  public static final @NotNull String DEFAULT_DEBUG_SENTRY_CLI_STRING = "false";
  public static final boolean DEFAULT_DEBUG = false;
  public static final @NotNull String DEFAULT_DEBUG_STRING = "false";
  public static final boolean DEFAULT_SKIP_VALIDATE_SDK_DEPENDENCY_VERSIONS = false;
  public static final @NotNull String DEFAULT_SKIP_VALIDATE_SDK_DEPENDENCY_VERSIONS_STRING =
      "false";
  public static final @NotNull String DEFAULT_ADDITIONAL_SOURCE_DIRS_FOR_SOURCE_CONTEXT = "";

  private boolean skip = DEFAULT_SKIP;
  private boolean skipAutoInstall = DEFAULT_SKIP_AUTO_INSTALL;
  private boolean skipTelemetry = DEFAULT_SKIP_TELEMETRY;
  private boolean skipReportDependencies = DEFAULT_SKIP_REPORT_DEPENDENCIES;
  private boolean skipSourceBundle = DEFAULT_SKIP_SOURCE_BUNDLE;
  private boolean debugSentryCli = DEFAULT_DEBUG_SENTRY_CLI;
  private boolean debug = DEFAULT_DEBUG;
  private boolean skipValidateSdkDependencyVersions = DEFAULT_SKIP_VALIDATE_SDK_DEPENDENCY_VERSIONS;
  private @NotNull String additionalSourceDirsForSourceContext =
      DEFAULT_ADDITIONAL_SOURCE_DIRS_FOR_SOURCE_CONTEXT;

  private @Nullable String org;
  private @Nullable String project;
  private @Nullable String url;
  private @Nullable String authToken;
  private @Nullable String sentryCliExecutablePath;

  public void setDebugSentryCli(final boolean debugSentryCli) {
    this.debugSentryCli = debugSentryCli;
  }

  public boolean isDebugSentryCli() {
    return debugSentryCli || debug;
  }

  public void setDebug(final boolean debug) {
    this.debug = debug;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setSentryCliExecutablePath(final @Nullable String sentryCliExecutablePath) {
    this.sentryCliExecutablePath = sentryCliExecutablePath;
  }

  public @Nullable String getSentryCliExecutablePath() {
    return sentryCliExecutablePath;
  }

  public void setSkip(final boolean skip) {
    this.skip = skip;
  }

  public boolean isSkip() {
    return skip;
  }

  public void setSkipAutoInstall(final boolean skipAutoInstall) {
    this.skipAutoInstall = skipAutoInstall;
  }

  public void setSkipTelemetry(final boolean skipTelemetry) {
    this.skipTelemetry = skipTelemetry;
  }

  public void setSkipReportDependencies(final boolean skipReportDependencies) {
    this.skipReportDependencies = skipReportDependencies;
  }

  public void setSkipSourceBundle(final boolean skipSourceBundle) {
    this.skipSourceBundle = skipSourceBundle;
  }

  public void setSkipValidateSdkDependencyVersions(
      final boolean skipValidateSdkDependencyVersions) {
    this.skipValidateSdkDependencyVersions = skipValidateSdkDependencyVersions;
  }

  public boolean isSkipAutoInstall() {
    return skipAutoInstall || skip;
  }

  public boolean isSkipTelemetry() {
    return skipTelemetry || skip;
  }

  public boolean isSkipReportDependencies() {
    return skipReportDependencies || skip;
  }

  public boolean isSkipSourceBundle() {
    return skipSourceBundle || skip;
  }

  public boolean isSkipValidateSdkDependencyVersions() {
    return skipValidateSdkDependencyVersions;
  }

  public @Nullable String getOrg() {
    return org;
  }

  public void setOrg(final @Nullable String org) {
    this.org = org;
  }

  public @Nullable String getProject() {
    return project;
  }

  public void setProject(final @Nullable String project) {
    this.project = project;
  }

  public @Nullable String getUrl() {
    return url;
  }

  public void setUrl(final @Nullable String url) {
    this.url = url;
  }

  public @Nullable String getAuthToken() {
    return authToken;
  }

  public void setAuthToken(final @Nullable String authToken) {
    this.authToken = authToken;
  }

  public @NotNull String getAdditionalSourceDirsForSourceContext() {
    return additionalSourceDirsForSourceContext;
  }

  public void setAdditionalSourceDirsForSourceContext(
      final @NotNull String additionalSourceDirsForSourceContext) {
    this.additionalSourceDirsForSourceContext = additionalSourceDirsForSourceContext;
  }
}
