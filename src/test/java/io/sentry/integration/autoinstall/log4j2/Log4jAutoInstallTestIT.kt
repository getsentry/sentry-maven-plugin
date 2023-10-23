package io.sentry.integration.autoinstall.log4j2

import basePom
import installMavenWrapper
import io.sentry.autoinstall.util.SdkVersionInfo
import org.apache.maven.it.VerificationException
import org.apache.maven.it.Verifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class Log4jAutoInstallTestIT {
    @TempDir()
    lateinit var file: File

    @BeforeEach
    fun installWrapper() {
        installMavenWrapper(file, "3.8.6")
    }

    fun getPOM(
        installLog4j2: Boolean = true,
        log4j2Version: String = "2.17.0",
        sentryLog4j2Version: String = "6.25.2",
        installedSentryVersion: String? = null,
    ): String {
        var dependencies =
            """
            <dependency>
                <groupId>org.apache.logging.log4j</groupId>
                <artifactId>log4j-api</artifactId>
                <version>$log4j2Version</version>
            </dependency>
            """.trimIndent()

        if (!installLog4j2) {
            dependencies =
                dependencies.plus(
                    """
                    <dependency>
                        <groupId>io.sentry</groupId>
                        <artifactId>sentry-log4j2</artifactId>
                        <version>$sentryLog4j2Version</version>
                    </dependency>
                    """.trimIndent(),
                )
        }

        val pomContent = basePom(dependencies, installedSentryVersion)

        Files.write(Path("${file.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        return file.absolutePath
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when sentry-log4j2 is a direct dependency logs a message and does nothing`() {
        val path = getPOM(false)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-log4j2 won't be installed because it was already installed directly")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `installs sentry-log4j2 with info message`() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-log4j2 was successfully installed with version: ${SdkVersionInfo.getSentryVersion()}")
        verifier.verifyFilePresent("target/lib/sentry-log4j2-${SdkVersionInfo.getSentryVersion()}.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `auto-installed sentry-log4j2 version matches sentry version`() {
        val sentryVersion = "6.25.2"
        val path = getPOM(installedSentryVersion = sentryVersion)
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-log4j2 was successfully installed with version: $sentryVersion")
        verifier.verifyFilePresent("target/lib/sentry-log4j2-$sentryVersion.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }
}
