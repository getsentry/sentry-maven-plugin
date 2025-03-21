package io.sentry.config;

import static io.sentry.Constants.SENTRY_GROUP_ID;
import static io.sentry.Constants.SENTRY_PLUGIN_ARTIFACT_ID;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigParser {

  private static final @NotNull String SKIP_ALL_FLAG = "skip";
  private static final @NotNull String SKIP_AUTO_INSTALL_FLAG = "skipAutoInstall";
  private static final @NotNull String SKIP_TELEMETRY_FLAG = "skipTelemetry";
  private static final @NotNull String SKIP_REPORT_DEPENDENCIES_FLAG = "skipReportDependencies";
  private static final @NotNull String SKIP_SOURCE_BUNDLE_FLAG = "skipSourceBundle";
  private static final @NotNull String SKIP_VALIDATE_SDK_DEPENDENCY_VERSIONS_OPTION =
      "skipValidateSdkDependencyVersions";
  private static final @NotNull String DEBUG_SENTRY_CLI_FLAG = "debugSentryCli";
  private static final @NotNull String DEBUG_FLAG = "debug";
  private static final @NotNull String ORG_OPTION = "org";
  private static final @NotNull String PROJECT_OPTION = "project";
  private static final @NotNull String URL_OPTION = "url";
  private static final @NotNull String AUTH_TOKEN_OPTION = "authToken";
  private static final @NotNull String ADDITIONAL_SOURCE_DIRS_FOR_SOURCE_CONTEXT =
      "additionalSourceDirsForSourceContext";

  public @NotNull PluginConfig parseConfig(final @NotNull MavenProject project) {
    final @NotNull PluginConfig pluginConfig = new PluginConfig();
    final @Nullable Plugin sentryPlugin =
        project.getBuildPlugins().stream()
            .filter(
                (plugin) ->
                    plugin.getGroupId().equals(SENTRY_GROUP_ID)
                        && plugin.getArtifactId().equals(SENTRY_PLUGIN_ARTIFACT_ID))
            .findFirst()
            .orElse(null);

    if (sentryPlugin == null) {
      pluginConfig.setSkip(true);
      return pluginConfig;
    }

    final @Nullable Xpp3Dom dom = (Xpp3Dom) sentryPlugin.getConfiguration();

    if (dom != null) {
      pluginConfig.setSkip(
          dom.getChild(SKIP_ALL_FLAG) != null
              && Boolean.parseBoolean(dom.getChild(SKIP_ALL_FLAG).getValue()));

      pluginConfig.setSkipAutoInstall(
          dom.getChild(SKIP_AUTO_INSTALL_FLAG) != null
              && Boolean.parseBoolean(dom.getChild(SKIP_AUTO_INSTALL_FLAG).getValue()));

      pluginConfig.setSkipTelemetry(
          dom.getChild(SKIP_TELEMETRY_FLAG) != null
              && Boolean.parseBoolean(dom.getChild(SKIP_TELEMETRY_FLAG).getValue()));

      pluginConfig.setSkipSourceBundle(
          dom.getChild(SKIP_SOURCE_BUNDLE_FLAG) != null
              && Boolean.parseBoolean(dom.getChild(SKIP_SOURCE_BUNDLE_FLAG).getValue()));

      pluginConfig.setSkipReportDependencies(
          dom.getChild(SKIP_REPORT_DEPENDENCIES_FLAG) != null
              && Boolean.parseBoolean(dom.getChild(SKIP_REPORT_DEPENDENCIES_FLAG).getValue()));

      pluginConfig.setDebugSentryCli(
          dom.getChild(DEBUG_SENTRY_CLI_FLAG) != null
              && Boolean.parseBoolean(dom.getChild(DEBUG_SENTRY_CLI_FLAG).getValue()));

      pluginConfig.setDebug(
          dom.getChild(DEBUG_FLAG) != null
              && Boolean.parseBoolean(dom.getChild(DEBUG_FLAG).getValue()));

      pluginConfig.setOrg(
          dom.getChild(ORG_OPTION) == null ? null : dom.getChild(ORG_OPTION).getValue());

      pluginConfig.setProject(
          dom.getChild(PROJECT_OPTION) == null ? null : dom.getChild(PROJECT_OPTION).getValue());

      pluginConfig.setUrl(
          dom.getChild(URL_OPTION) == null ? null : dom.getChild(URL_OPTION).getValue());

      pluginConfig.setAuthToken(
          dom.getChild(AUTH_TOKEN_OPTION) == null
              ? null
              : dom.getChild(AUTH_TOKEN_OPTION).getValue());

      pluginConfig.setSkipValidateSdkDependencyVersions(
          dom.getChild(SKIP_VALIDATE_SDK_DEPENDENCY_VERSIONS_OPTION) != null
              && Boolean.parseBoolean(
                  dom.getChild(SKIP_VALIDATE_SDK_DEPENDENCY_VERSIONS_OPTION).getValue()));

      pluginConfig.setAdditionalSourceDirsForSourceContext(
          dom.getChild(ADDITIONAL_SOURCE_DIRS_FOR_SOURCE_CONTEXT) == null
              ? ""
              : Arrays.stream(dom.getChild(ADDITIONAL_SOURCE_DIRS_FOR_SOURCE_CONTEXT).getChildren())
                  .map(Xpp3Dom::getValue)
                  .collect(Collectors.joining(",")));
    }

    return pluginConfig;
  }
}
