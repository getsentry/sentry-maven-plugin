package io.sentry.autoinstall.logback

import basePom
import installMavenWrapper
import io.sentry.autoinstall.SentryInstaller
import org.apache.maven.shared.verifier.VerificationException
import org.apache.maven.shared.verifier.Verifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class LogbackAutoInstallTestIT {
    @TempDir()
    lateinit var file: File

    @BeforeEach
    fun installWrapper() {
        installMavenWrapper(file, "3.8.6")
    }

    fun getPOM(
        installLogback: Boolean = true,
        logbackVersion: String = "1.4.5",
        sentryLogbackVersion: String = "6.25.2",
        installedSentryVersion: String? = null,
    ): String {
        var dependencies =
            """
            <dependency>
                <groupId>ch.qos.logback</groupId>
                <artifactId>logback-classic</artifactId>
                <version>$logbackVersion</version>
            </dependency>
            """.trimIndent()

        if (!installLogback) {
            dependencies =
                dependencies.plus(
                    """
                    <dependency>
                        <groupId>io.sentry</groupId>
                        <artifactId>sentry-logback</artifactId>
                        <version>$sentryLogbackVersion</version>
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
    fun `when sentry-logback is a direct dependency logs a message and does nothing`() {
        val path = getPOM(false)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("sentry-logback won't be installed because it was already installed directly")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when logback version is unsupported logs a message and does nothing`() {
        val path = getPOM(logbackVersion = "0.9.30")
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog(
            "sentry-logback won't be installed because the current version is " +
                "lower than the minimum supported version 1.0.0",
        )
        verifier.verifyFileNotPresent("target/lib/sentry-logback-${SentryInstaller.SENTRY_VERSION}.jar")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `installs sentry-logback with info message`() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("sentry-logback was successfully installed with version: ${SentryInstaller.SENTRY_VERSION}")
        verifier.verifyFilePresent("target/lib/sentry-logback-${SentryInstaller.SENTRY_VERSION}.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `auto-installed logback version matches sentry version`() {
        val sentryVersion = "6.25.2"
        val path = getPOM(installedSentryVersion = sentryVersion)
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("sentry-logback was successfully installed with version: $sentryVersion")
        verifier.verifyFilePresent("target/lib/sentry-logback-$sentryVersion.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }
}
