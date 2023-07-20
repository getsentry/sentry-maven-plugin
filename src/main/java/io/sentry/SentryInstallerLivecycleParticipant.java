package io.sentry;

import io.sentry.autoinstall.SentryInstaller;
import io.sentry.autoinstall.SpringIntegrationInstaller;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
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

import static io.sentry.autoinstall.Constants.SENTRY_GROUP_ID;

@Named( "sentry-installer")
@Singleton
public class SentryInstallerLivecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final String SENTRY_MAVEN_PLUGIN_ARTIFACT_ID = "sentry-maven-plugin";

    @Parameter(property = "autoInstall")
    private String org;

    private static org.slf4j.Logger logger = LoggerFactory.getLogger(SentryInstallerLivecycleParticipant.class);

    @Override
    public void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        for (MavenProject project : session.getProjects()) {
            logger.info( "Checking project '" + project.getId() + "'" );


            Plugin sentryPlugin = project.getBuildPlugins().stream().filter((plugin) -> plugin.getGroupId().equals(SENTRY_GROUP_ID) && plugin.getArtifactId().equals(SENTRY_MAVEN_PLUGIN_ARTIFACT_ID)).findFirst().orElse(null);

            if(sentryPlugin == null) {
                return;
            }

            PlexusConfiguration pomConfiguration = new XmlPlexusConfiguration(((Xpp3Dom) sentryPlugin.getConfiguration()));
            boolean autoInstall = Boolean.parseBoolean(pomConfiguration.getChild("autoInstall").getValue());

            if(!autoInstall) {
                logger.info("Auto Install disabled, not installing dependencies");
            }

            Model currModel = project.getModel();

            String sentryVersion = new SentryInstaller().install(currModel.getDependencies());

            new SpringIntegrationInstaller().install(currModel.getDependencies(), sentryVersion);
        }

    }
}
