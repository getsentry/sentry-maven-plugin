package io.sentry;

import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

/*
 * Helper class to find sentry-cli
 * Ported from sentry-android-gradle-plugin
 */
class SentryCliProvider {

    private static Logger logger = LoggerFactory.getLogger(SentryCliProvider.class);

    protected static String getCliPath(MavenProject mavenProject, String cliPathParameter) {

        if(cliPathParameter != null && !cliPathParameter.isBlank()) {
            return cliPathParameter;
        }

        String cliSuffix = getCliSuffix();

        if(cliSuffix != null && !cliSuffix.isBlank()) {
            String resourcePath = "/bin/sentry-cli-" + cliSuffix;
            String cliAbsolutePath = searchCliInResources(resourcePath);

            if(cliAbsolutePath != null) {
                logger.info("Cli found in " + cliAbsolutePath);
                return cliAbsolutePath;
            }

            String cliTempPath = loadCliFromResourcesToTemp(resourcePath);

            if(cliTempPath != null) {
                logger.info("Cli found in " + cliTempPath);
                return cliTempPath;
            }

        }

        return "sentry-cli";
    }

    private static String getCliSuffix() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch");

        if(osName.contains("mac")) {
            return "Darwin-universal";
        }

        if(osName.contains("linux")) {
            return osArch.equals("amd64") ? "Linux-x86_64" : "Linux-" + osArch;
        }

        if(osName.contains("win")) {
            return "Windows-i686.exe";
        }

        return null;
    }

    private static String searchCliInResources(String resourcePath) {
        URL resourceUrl = SentryCliProvider.class.getResource(resourcePath);

        if(resourceUrl != null) {
            File resourceFile = new File(resourceUrl.getFile());
            if(resourceFile.exists()) {
                return resourceFile.getAbsolutePath();
            }
        }
        return null;
    }

    private static String loadCliFromResourcesToTemp(String resourcePath) {

        try (InputStream inputStream = SentryCliProvider.class.getResourceAsStream(resourcePath)) {
            File tempFile = File.createTempFile(".sentry-cli", ".exe");
            tempFile.deleteOnExit();
            tempFile.setExecutable(true);

            FileOutputStream outputStream = new FileOutputStream(tempFile);

            if(inputStream != null) {
                inputStream.transferTo(outputStream);
                outputStream.close();
                return tempFile.getAbsolutePath();
            } else {
                return null;
            }

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return null;
        }

    }

}
