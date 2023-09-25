package io.sentry;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.autoinstall.SentryInstaller;
import io.sentry.autoinstall.graphql.GraphqlInstallStrategy;
import io.sentry.autoinstall.jdbc.JdbcInstallStrategy;
import io.sentry.autoinstall.log4j2.Log4j2InstallStrategy;
import io.sentry.autoinstall.logback.LogbackInstallStrategy;
import io.sentry.autoinstall.spring.Spring5InstallStrategy;
import io.sentry.autoinstall.spring.Spring6InstallStrategy;
import io.sentry.autoinstall.spring.SpringBoot2InstallStrategy;
import io.sentry.autoinstall.spring.SpringBoot3InstallStrategy;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.sentry.autoinstall.Constants.SENTRY_ARTIFACT_ID;
import static io.sentry.autoinstall.Constants.SENTRY_GROUP_ID;
import static io.sentry.autoinstall.graphql.GraphqlInstallStrategy.SENTRY_GRAPHQL_ID;
import static io.sentry.autoinstall.jdbc.JdbcInstallStrategy.SENTRY_JDBC_ID;
import static io.sentry.autoinstall.log4j2.Log4j2InstallStrategy.SENTRY_LOG4J2_ID;
import static io.sentry.autoinstall.logback.LogbackInstallStrategy.SENTRY_LOGBACK_ID;
import static io.sentry.autoinstall.spring.Spring5InstallStrategy.SENTRY_SPRING_5_ID;
import static io.sentry.autoinstall.spring.Spring6InstallStrategy.SENTRY_SPRING_6_ID;
import static io.sentry.autoinstall.spring.SpringBoot2InstallStrategy.SENTRY_SPRING_BOOT_2_ID;
import static io.sentry.autoinstall.spring.SpringBoot3InstallStrategy.SENTRY_SPRING_BOOT_3_ID;

@Named( "sentry-installer")
@Singleton
public class SentryInstallerLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private static final List<Class<? extends AbstractIntegrationInstaller>> installers = Stream.of(
        Spring5InstallStrategy.class,
        Spring6InstallStrategy.class,
        SpringBoot2InstallStrategy.class,
        SpringBoot3InstallStrategy.class,
        Log4j2InstallStrategy.class,
        LogbackInstallStrategy.class,
        GraphqlInstallStrategy.class,
        JdbcInstallStrategy.class
        ).collect(Collectors.toList());

    private static final String SENTRY_MAVEN_PLUGIN_ARTIFACT_ID = "sentry-maven-plugin";

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(SentryInstallerLifecycleParticipant.class);

    @Override
    public void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        for (MavenProject project : session.getProjects()) {
            logger.info( "Checking project '" + project.getId() + "'" );


//            Plugin sentryPlugin = project.getBuildPlugins().stream().filter((plugin) -> plugin.getGroupId().equals(SENTRY_GROUP_ID) && plugin.getArtifactId().equals(SENTRY_MAVEN_PLUGIN_ARTIFACT_ID)).findFirst().orElse(null);
//
//            if(sentryPlugin == null) {
//                continue;
//            }

//            PlexusConfiguration pomConfiguration = new XmlPlexusConfiguration(((Xpp3Dom) sentryPlugin.getConfiguration()));
//            boolean autoInstall = Boolean.parseBoolean(pomConfiguration.getChild("autoInstall").getValue());
//
//            if(!autoInstall) {
//                logger.info("Auto Install disabled, not installing dependencies");
//            }

            Model currModel = project.getModel();
            List<Dependency> dependencyList = currModel.getDependencies();
            String sentryVersion = new SentryInstaller().install(dependencyList);

            AutoInstallState autoInstallState = new AutoInstallState();
            autoInstallState.setSentryVersion(sentryVersion);
            autoInstallState.setInstallSpring(shouldInstallSpring(dependencyList));
            autoInstallState.setInstallLogback(!isModuleAvailable(dependencyList, SENTRY_LOGBACK_ID));
            autoInstallState.setInstallLog4j2(!isModuleAvailable(dependencyList, SENTRY_LOG4J2_ID));
            autoInstallState.setInstallGraphql(!isModuleAvailable(dependencyList, SENTRY_GRAPHQL_ID));
            autoInstallState.setInstallJdbc(!isModuleAvailable(dependencyList, SENTRY_JDBC_ID));

            for(Class<? extends AbstractIntegrationInstaller> installerClass : installers) {
                try {
                    AbstractIntegrationInstaller installer = installerClass.getDeclaredConstructor().newInstance();
                    installer.install(currModel.getDependencies(), autoInstallState);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            super.afterProjectsRead(session);
        }
    }

    private boolean shouldInstallSpring(List<Dependency> dependencies) {
        return !(
            isModuleAvailable(dependencies, SENTRY_SPRING_5_ID) &&
                isModuleAvailable(dependencies, SENTRY_SPRING_6_ID) &&
                isModuleAvailable(dependencies, SENTRY_SPRING_BOOT_2_ID) &&
                isModuleAvailable(dependencies, SENTRY_SPRING_BOOT_3_ID)
        );
    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {

        super.afterSessionEnd(session);
    }

    public static boolean isModuleAvailable(List<Dependency> dependencyList, String artifactId) {
        return dependencyList.stream().anyMatch((dep) ->
            dep.getGroupId().equals(SENTRY_GROUP_ID) && dep.getArtifactId().equals(artifactId)
        );
    }
}
