package io.sentry.integration.autoinstall.spring

import basePom
import io.sentry.autoinstall.util.SdkVersionInfo
import io.sentry.integration.installMavenWrapper
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

class SpringAutoInstallTestIT {
    @TempDir()
    lateinit var file: File

    @BeforeEach
    fun installWrapper() {
        installMavenWrapper(file, "3.8.6")
    }

    fun getPOM(
        installSpring: Boolean = true,
        springVersion: String = "6.0.0",
        sentrySpringArtifact: String = "sentry-spring-jakarta",
        sentrySpringVersion: String = "6.28.0",
        installedSentryVersion: String? = null,
    ): String {
        var dependencies =
            """
            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-core</artifactId>
                <version>$springVersion</version>
            </dependency>
            """.trimIndent()

        if (!installSpring) {
            dependencies =
                dependencies.plus(
                    """
                    <dependency>
                        <groupId>io.sentry</groupId>
                        <artifactId>$sentrySpringArtifact</artifactId>
                        <version>$sentrySpringVersion</version>
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
    fun `when sentry-spring-jakarta is a direct dependency logs a message and does nothing`() {
        val path = getPOM(false)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-spring-jakarta won't be installed because it was already installed directly")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when sentry-spring is a direct dependency logs a message and does nothing`() {
        val path = getPOM(sentrySpringArtifact = "sentry-spring", installSpring = false)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-spring-jakarta won't be installed because it was already installed directly")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when sentry-spring-boot is a direct dependency logs a message and does nothing`() {
        val path = getPOM(sentrySpringArtifact = "sentry-spring-boot", installSpring = false)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-spring-jakarta won't be installed because it was already installed directly")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when sentry-spring-boot-jakarta is a direct dependency logs a message and does nothing`() {
        val path = getPOM(sentrySpringArtifact = "sentry-spring-boot-jakarta", installSpring = false)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-spring-jakarta won't be installed because it was already installed directly")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `installs sentry-spring-jakarta with info message`() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-spring-jakarta was successfully installed with version: ${SdkVersionInfo.getSentryVersion()}")
        verifier.verifyFilePresent("target/lib/sentry-spring-jakarta-${SdkVersionInfo.getSentryVersion()}.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `auto-installed sentry-spring-jakarta version matches sentry version`() {
        val sentryVersion = "6.28.0"
        val path = getPOM(installedSentryVersion = sentryVersion)
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-spring-jakarta was successfully installed with version: $sentryVersion")
        verifier.verifyFilePresent("target/lib/sentry-spring-jakarta-$sentryVersion.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }
}
