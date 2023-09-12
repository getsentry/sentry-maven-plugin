package io.sentry;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.autoinstall.SentryInstaller;
import io.sentry.autoinstall.log4j2.Log4j2InstallStrategy;
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
import static io.sentry.autoinstall.spring.SpringBoot3InstallStrategy.SENTRY_SPRING_BOOT_3_ID;

@Named( "sentry-installer")
@Singleton
public class SentryInstallerLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private static final List<Class<? extends AbstractIntegrationInstaller>> installers = Stream.of(
        SpringBoot3InstallStrategy.class, Log4j2InstallStrategy.class)
        .collect(Collectors.toList());

    private static final String SENTRY_MAVEN_PLUGIN_ARTIFACT_ID = "sentry-maven-plugin";



    @Parameter(property = "autoInstall")
    private String autoInstall;

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(SentryInstallerLifecycleParticipant.class);

    @Override
    public void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        for (MavenProject project : session.getProjects()) {
            logger.info( "Checking project '" + project.getId() + "'" );


            Plugin sentryPlugin = project.getBuildPlugins().stream().filter((plugin) -> plugin.getGroupId().equals(SENTRY_GROUP_ID) && plugin.getArtifactId().equals(SENTRY_MAVEN_PLUGIN_ARTIFACT_ID)).findFirst().orElse(null);

            if(sentryPlugin == null) {
                continue;
            }

//            PlexusConfiguration pomConfiguration = new XmlPlexusConfiguration(((Xpp3Dom) sentryPlugin.getConfiguration()));
//            boolean autoInstall = Boolean.parseBoolean(pomConfiguration.getChild("autoInstall").getValue());
//
//            if(!autoInstall) {
//                logger.info("Auto Install disabled, not installing dependencies");
//            }

            Model currModel = project.getModel();

            String sentryVersion = SentryInstaller.install(currModel.getDependencies());

            AutoInstallState autoInstallState = new AutoInstallState();

            boolean shouldInstallSpring = !(
                isModuleAvailable(currModel.getDependencies(), SENTRY_SPRING_BOOT_3_ID) &&
                    isModuleAvailable(currModel.getDependencies(), SENTRY_SPRING_BOOT_3_ID) &&
                    isModuleAvailable(currModel.getDependencies(), SENTRY_SPRING_BOOT_3_ID) &&
                    isModuleAvailable(currModel.getDependencies(), SENTRY_SPRING_BOOT_3_ID)
            );

            autoInstallState.setInstallSpring(shouldInstallSpring);

            for(Class<? extends AbstractIntegrationInstaller> installerClass : installers) {
                try {
                    AbstractIntegrationInstaller installer = installerClass.getDeclaredConstructor().newInstance();
                    installer.install(currModel.getDependencies(), autoInstallState, sentryVersion);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            super.afterProjectsRead(session);
        }
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
