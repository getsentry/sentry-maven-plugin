package io.sentry;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "uploadSourceBundle", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class UploadSourceBundleMojo extends AbstractMojo {

    private static Logger logger = LoggerFactory.getLogger(UploadSourceBundleMojo.class);

    @Parameter(property = "sentry.cli.debug", defaultValue = "false")
    private boolean debugSentryCli;

    @Parameter(property = "sentry.cli.path")
    private String sentryCliExecutablePath;

    @Parameter(property = "sentry.cli.properties.path")
    private String sentryPropertiesPath;

    @Parameter(property = "sentry.org")
    private String org;

    @Parameter(property = "sentry.project")
    private String project;

    @Parameter(property = "sentry.url")
    private String url;

    @Parameter(property = "sentry.authToken")
    private String authToken;

    @Parameter(property = "project.build.directory")
    private File outputDirectory;

    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject mavenProject;

    @Parameter( defaultValue = "${session}", readonly = true )
    private MavenSession mavenSession;

    @Component
    private BuildPluginManager pluginManager;

    public void execute() throws MojoExecutionException {
        String bundleId = UUID.randomUUID().toString();
        File sourceBundleTargetDir = new File(sentryBuildDir(), "source-bundle");
        createDebugMetaPropertiesFile(bundleId);
        bundleSources(bundleId, sourceBundleTargetDir);
        uploadSourceBundle(sourceBundleTargetDir);
    }

    private File sentryBuildDir() {
        return new File(outputDirectory, "sentry");
    }

    private void bundleSources(String bundleId, File sourceBundleTargetDir) throws MojoExecutionException {
        if (!sourceBundleTargetDir.exists()) {
            sourceBundleTargetDir.mkdirs();
        }

        List<String> bundleSourcesCommand = new ArrayList<>();
        List<String> sourceRoots = mavenProject.getCompileSourceRoots();

        if (sourceRoots != null && sourceRoots.size() > 0) {
            String sourceRoot = sourceRoots.get(0);
            if (sourceRoots.size() > 1) {
                logger.warn("There's more than one source root, using {}", sourceRoot);
            }

            if (debugSentryCli) {
                bundleSourcesCommand.add("--log-level=debug");
            }

            /*
             * TODO maybe at some point copy all of them into one dir and pass that to
             *  sentry-cli or allow sentry-cli to take more than one dir
             */
            logger.debug("Bundling sources located in {}", sourceRoot);

            if (url != null) {
                bundleSourcesCommand.add("--url=" + url);
            }
            if (authToken != null) {
                bundleSourcesCommand.add("--auth-token=" + authToken);
            }

            bundleSourcesCommand.add("debug-files");
            bundleSourcesCommand.add("bundle-jvm");
            bundleSourcesCommand.add("--output=" + sourceBundleTargetDir.getAbsolutePath());
            bundleSourcesCommand.add("--debug-id=" + bundleId);
            if (org != null) {
                bundleSourcesCommand.add("--org=" + org);
            }
            if (project != null) {
                bundleSourcesCommand.add("--project=" + project);
            }
            bundleSourcesCommand.add(sourceRoot);

            runSentryCli(String.join(" ", bundleSourcesCommand));
        } else {
            throw new MojoExecutionException("Unable to find source root");
        }
    }

    private void uploadSourceBundle(File sourceBundleTargetDir) throws MojoExecutionException {
        List<String> command = new ArrayList<>();

        if (debugSentryCli) {
            command.add("--log-level=debug");
        }

        if (url != null) {
            command.add("--url=" + url);
        }
        if (authToken != null) {
            command.add("--auth-token=" + authToken);
        }

        command.add("debug-files");
        command.add("upload");
        command.add("--type=jvm");
        if (org != null) {
            command.add("--org=" + org);
        }
        if (project != null) {
            command.add("--project=" + project);
        }
        command.add(sourceBundleTargetDir.getAbsolutePath());

        runSentryCli(String.join(" ", command));
    }

    private void runSentryCli(String sentryCliCommand) throws MojoExecutionException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        String executable = isWindows ? "cmd.exe" : "/bin/sh";
        String cArg = isWindows ? "/c" : "-c";

        executeMojo(
            plugin(
                groupId("org.apache.maven.plugins"),
                artifactId("maven-antrun-plugin"),
                version("3.1.0")
            ),
            goal("run"),
            configuration(
                element(name("target"),
                    element(name("exec"),
                        attributes(
                            attribute("executable", executable)
                        ),
                        element(name("arg"), attributes(attribute("value", cArg))),
                        element(name("arg"), attributes(attribute("value", sentryCliExecutablePath + " " + sentryCliCommand)))
                    )
                )
            ),
            executionEnvironment(
                mavenProject,
                mavenSession,
                pluginManager
            )
        );
    }

    private void createDebugMetaPropertiesFile(String bundleId) throws MojoExecutionException {
        File sentryBuildDir = new File(sentryBuildDir(), "properties");
        if (!sentryBuildDir.exists()) {
            sentryBuildDir.mkdirs();
        }

        File debugMetaFile = new File(sentryBuildDir, "sentry-debug-meta.properties");
        Properties properties = createDebugMetaProperties(bundleId);

        try (FileWriter fileWriter = new FileWriter(debugMetaFile)) {
            properties.store(fileWriter, "Generated by sentry-maven-plugin");

            final Resource resource = new Resource();
            resource.setDirectory(sentryBuildDir.getPath());
            resource.setFiltering(false);
            mavenProject.addResource(resource);
        } catch ( IOException e ) {
            throw new MojoExecutionException( "Error creating file " + debugMetaFile, e );
        }
    }

    private Properties createDebugMetaProperties(String bundleId) {
        Properties properties = new Properties();

        properties.setProperty("io.sentry.bundle-ids", bundleId);
        properties.setProperty("io.sentry.build-tool", "maven");

        return properties;
    }
}
