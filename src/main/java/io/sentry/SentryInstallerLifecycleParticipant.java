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
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.DefaultDependencyResolutionRequest;
import org.apache.maven.project.DependencyResolutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.LoggerFactory;

@Named("sentry-installer")
@Singleton
public class SentryInstallerLifecycleParticipant extends AbstractMavenLifecycleParticipant {

  private static final String AUTO_INSTALL_ENABLED_PROPERTY = "sentry.autoinstall.enabled";

  private static final List<Class<? extends AbstractIntegrationInstaller>> installers =
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

  @Inject ProjectDependenciesResolver resolver;

  private static org.slf4j.Logger logger =
      LoggerFactory.getLogger(SentryInstallerLifecycleParticipant.class);

  @Override
  public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
    for (MavenProject project : session.getProjects()) {
      Properties properties = project.getProperties();

      if (!isEnabled(properties)) {
        logger.info("Auto Install disabled, not installing dependencies");
        continue;
      }

      logger.info("Checking project '" + project.getId() + "'");

      List<Artifact> resolvedArtifacts;
      try {
        var request =
            new DefaultDependencyResolutionRequest(project, session.getRepositorySession());
        var result = resolver.resolve(request);
        resolvedArtifacts =
            result.getDependencies().stream()
                .map(it -> it.getArtifact())
                .collect(Collectors.toList());
      } catch (DependencyResolutionException e) {
        logger.error("Unable to resolve all dependencies", e);
        throw new RuntimeException(e);
      }

      Model currModel = project.getModel();

      List<Dependency> dependencyList = currModel.getDependencies();
      String sentryVersion = new SentryInstaller().install(dependencyList, resolvedArtifacts);

      AutoInstallState autoInstallState = new AutoInstallState();
      autoInstallState.setSentryVersion(sentryVersion);
      autoInstallState.setInstallSpring(shouldInstallSpring(resolvedArtifacts));
      autoInstallState.setInstallLogback(!isModuleAvailable(resolvedArtifacts, SENTRY_LOGBACK_ID));
      autoInstallState.setInstallLog4j2(!isModuleAvailable(resolvedArtifacts, SENTRY_LOG4J2_ID));
      autoInstallState.setInstallGraphql(!isModuleAvailable(resolvedArtifacts, SENTRY_GRAPHQL_ID));
      autoInstallState.setInstallJdbc(!isModuleAvailable(resolvedArtifacts, SENTRY_JDBC_ID));
      autoInstallState.setInstallQuartz(!isModuleAvailable(resolvedArtifacts, SENTRY_QUARTZ_ID));

      for (Class<? extends AbstractIntegrationInstaller> installerClass : installers) {
        try {
          AbstractIntegrationInstaller installer =
              installerClass.getDeclaredConstructor().newInstance();
          installer.install(dependencyList, resolvedArtifacts, autoInstallState);
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      }

      super.afterProjectsRead(session);
    }
  }

  private boolean isEnabled(Properties userProperties) {
    return Boolean.parseBoolean(userProperties.getProperty(AUTO_INSTALL_ENABLED_PROPERTY, "true"));
  }

  private boolean shouldInstallSpring(List<Artifact> resolvedArtifacts) {
    return !(isModuleAvailable(resolvedArtifacts, SENTRY_SPRING_5_ID)
        && isModuleAvailable(resolvedArtifacts, SENTRY_SPRING_6_ID)
        && isModuleAvailable(resolvedArtifacts, SENTRY_SPRING_BOOT_2_ID)
        && isModuleAvailable(resolvedArtifacts, SENTRY_SPRING_BOOT_3_ID));
  }

  public static boolean isModuleAvailable(List<Artifact> resolvedArtifacts, String artifactId) {
    return resolvedArtifacts.stream()
        .anyMatch(
            (dep) ->
                dep.getGroupId().equals(SENTRY_GROUP_ID) && dep.getArtifactId().equals(artifactId));
  }
}
