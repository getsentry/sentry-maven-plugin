package io.sentry;

import static io.sentry.autoinstall.Constants.SENTRY_GROUP_ID;
import static io.sentry.autoinstall.graphql.GraphqlInstallStrategy.SENTRY_GRAPHQL_ID;
import static io.sentry.autoinstall.jdbc.JdbcInstallStrategy.SENTRY_JDBC_ID;
import static io.sentry.autoinstall.log4j2.Log4j2InstallStrategy.SENTRY_LOG4J2_ID;
import static io.sentry.autoinstall.logback.LogbackInstallStrategy.SENTRY_LOGBACK_ID;
import static io.sentry.autoinstall.quartz.QuartzInstallStrategy.SENTRY_QUARTZ_ID;
import static io.sentry.autoinstall.spring.Spring5InstallStrategy.SENTRY_SPRING_5_ID;
import static io.sentry.autoinstall.spring.Spring6InstallStrategy.SENTRY_SPRING_6_ID;
import static io.sentry.autoinstall.spring.SpringBoot2InstallStrategy.SENTRY_SPRING_BOOT_2_ID;
import static io.sentry.autoinstall.spring.SpringBoot3InstallStrategy.SENTRY_SPRING_BOOT_3_ID;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.autoinstall.SentryInstaller;
import io.sentry.autoinstall.graphql.GraphqlInstallStrategy;
import io.sentry.autoinstall.jdbc.JdbcInstallStrategy;
import io.sentry.autoinstall.log4j2.Log4j2InstallStrategy;
import io.sentry.autoinstall.logback.LogbackInstallStrategy;
import io.sentry.autoinstall.quartz.QuartzInstallStrategy;
import io.sentry.autoinstall.spring.Spring5InstallStrategy;
import io.sentry.autoinstall.spring.Spring6InstallStrategy;
import io.sentry.autoinstall.spring.SpringBoot2InstallStrategy;
import io.sentry.autoinstall.spring.SpringBoot3InstallStrategy;
import io.sentry.config.ConfigParser;
import io.sentry.config.PluginConfig;
import io.sentry.telemetry.SentryTelemetryService;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

@Named("sentry-installer")
@Singleton
public class SentryInstallerLifecycleParticipant extends AbstractMavenLifecycleParticipant {

  private static final @NotNull List<Class<? extends AbstractIntegrationInstaller>> installers =
      Stream.of(
              Spring5InstallStrategy.class,
              Spring6InstallStrategy.class,
              SpringBoot2InstallStrategy.class,
              SpringBoot3InstallStrategy.class,
              Log4j2InstallStrategy.class,
              LogbackInstallStrategy.class,
              GraphqlInstallStrategy.class,
              JdbcInstallStrategy.class,
              QuartzInstallStrategy.class)
          .collect(Collectors.toList());

  @Inject @NotNull ArtifactResolver resolver;

  @SuppressWarnings("NullAway")
  @Inject
  private @NotNull BuildPluginManager pluginManager;

  private static final @NotNull org.slf4j.Logger logger =
      LoggerFactory.getLogger(SentryInstallerLifecycleParticipant.class);

  @Override
  public void afterProjectsRead(final @NotNull MavenSession session)
      throws MavenExecutionException {
    for (final @NotNull MavenProject project : session.getProjects()) {
      final @NotNull PluginConfig pluginConfig = new ConfigParser().parseConfig(project);
      SentryTelemetryService.getInstance().start(pluginConfig, project, session, pluginManager);

      if (pluginConfig.isSkipAutoInstall()) {
        logger.info(
            "Auto Install disabled for project "
                + project.getId()
                + " , not installing dependencies");
        continue;
      }

      final @Nullable ISpan span = SentryTelemetryService.getInstance().startTask("auto-install");

      try {
        @Nullable List<Artifact> resolvedArtifacts;
        try {
          resolvedArtifacts = resolver.resolveArtifactsForProject(project, session);
        } catch (DependencyResolutionException e) {
          logger.error("Unable to resolve all dependencies", e);
          continue;
        }

        final @NotNull Model currModel = project.getModel();

        final @NotNull List<Dependency> dependencyList = currModel.getDependencies();
        final @Nullable String sentryVersion =
            new SentryInstaller().install(dependencyList, resolvedArtifacts);

        if (sentryVersion == null) {
          logger.error(
              "No Sentry SDK Version found, cannot auto-install sentry integrations for project + "
                  + project.getId());
          continue;
        }

        SentryTelemetryService.getInstance().addTag("SDK_VERSION", sentryVersion);

        final @NotNull AutoInstallState autoInstallState = new AutoInstallState(sentryVersion);
        autoInstallState.setInstallSpring(shouldInstallSpring(resolvedArtifacts));
        autoInstallState.setInstallLogback(
            !isModuleAvailable(resolvedArtifacts, SENTRY_LOGBACK_ID));
        autoInstallState.setInstallLog4j2(!isModuleAvailable(resolvedArtifacts, SENTRY_LOG4J2_ID));
        autoInstallState.setInstallGraphql(
            !isModuleAvailable(resolvedArtifacts, SENTRY_GRAPHQL_ID));
        autoInstallState.setInstallJdbc(!isModuleAvailable(resolvedArtifacts, SENTRY_JDBC_ID));
        autoInstallState.setInstallQuartz(!isModuleAvailable(resolvedArtifacts, SENTRY_QUARTZ_ID));

        for (final @NotNull Class<? extends AbstractIntegrationInstaller> installerClass :
            installers) {
          try {
            final @NotNull AbstractIntegrationInstaller installer =
                installerClass.getDeclaredConstructor().newInstance();
            installer.install(dependencyList, resolvedArtifacts, autoInstallState);
          } catch (Throwable e) {
            logger.error("Unable to instantiate installer class: " + installerClass.getName(), e);
          }
        }
      } catch (Throwable t) {
        SentryTelemetryService.getInstance().captureError(t, "auto-install");
        throw t;
      } finally {
        SentryTelemetryService.getInstance().endTask(span);
      }
    }
    super.afterProjectsRead(session);
  }

  private boolean shouldInstallSpring(final @NotNull List<Artifact> resolvedArtifacts) {
    return !(isModuleAvailable(resolvedArtifacts, SENTRY_SPRING_5_ID)
        && isModuleAvailable(resolvedArtifacts, SENTRY_SPRING_6_ID)
        && isModuleAvailable(resolvedArtifacts, SENTRY_SPRING_BOOT_2_ID)
        && isModuleAvailable(resolvedArtifacts, SENTRY_SPRING_BOOT_3_ID));
  }

  public static boolean isModuleAvailable(
      final @NotNull List<Artifact> resolvedArtifacts, final @NotNull String artifactId) {
    return resolvedArtifacts.stream()
        .anyMatch(
            (dep) ->
                dep.getGroupId().equals(SENTRY_GROUP_ID) && dep.getArtifactId().equals(artifactId));
  }

  @Override
  public void afterSessionEnd(final @Nullable MavenSession session) throws MavenExecutionException {
    if (session != null) {
      final @Nullable MavenExecutionResult result = session.getResult();
      if (result != null) {
        if (result.hasExceptions()) {
          SentryTelemetryService.getInstance().markFailed();
        }
      }
    }
    SentryTelemetryService.getInstance().close();
    super.afterSessionEnd(session);
  }
}
