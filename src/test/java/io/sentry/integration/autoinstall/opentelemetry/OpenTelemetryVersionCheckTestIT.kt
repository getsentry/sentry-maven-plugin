package io.sentry.integration.autoinstall.opentelemetry

import io.sentry.autoinstall.util.SdkVersionInfo
import io.sentry.integration.installMavenWrapper
import org.apache.maven.it.VerificationException
import org.apache.maven.it.Verifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.test.assertFailsWith

class OpenTelemetryVersionCheckTestIT {
    @TempDir
    lateinit var file: File

    private val sentryVersion: String = SdkVersionInfo.getSentryVersion()!!

    @BeforeEach
    fun setup() {
        installMavenWrapper(file, "3.8.6")
    }

    private fun writePom(content: String): Verifier {
        Files.write(Path("${file.absolutePath}/pom.xml"), content.toByteArray(), StandardOpenOption.CREATE)
        return Verifier(file.absolutePath).apply { isAutoclean = false }
    }

    @Test
    fun `fails the build when OpenTelemetry is downgraded by spring boot`() {
        val verifier =
            writePom(
                openTelemetryPom(sentryVersion = sentryVersion, useSpringBootParent = true),
            )

        assertFailsWith<VerificationException> { verifier.executeGoal("validate") }

        verifier.verifyTextInLog("Sentry detected that OpenTelemetry was downgraded")
        verifier.verifyTextInLog("io.opentelemetry:opentelemetry-sdk")
        verifier.verifyTextInLog("<artifactId>sentry-opentelemetry-bom</artifactId>")
        verifier.resetStreams()
    }

    @Test
    fun `succeeds when OpenTelemetry versions are not downgraded`() {
        val verifier =
            writePom(openTelemetryPom(sentryVersion = sentryVersion))

        verifier.executeGoal("validate")

        verifier.resetStreams()
    }

    @Test
    fun `does not fail when the check is disabled`() {
        val verifier =
            writePom(
                openTelemetryPom(
                    sentryVersion = sentryVersion,
                    useSpringBootParent = true,
                    pluginConfiguration =
                        """
                        <configuration>
                            <skipValidateOpenTelemetryVersions>true</skipValidateOpenTelemetryVersions>
                        </configuration>
                        """.trimIndent(),
                ),
            )

        verifier.executeGoal("validate")

        verifier.resetStreams()
    }

    @Test
    fun `fails the build when OpenTelemetry is downgraded even with auto-install disabled`() {
        val verifier =
            writePom(
                openTelemetryPom(
                    sentryVersion = sentryVersion,
                    useSpringBootParent = true,
                    pluginConfiguration =
                        """
                        <configuration>
                            <skipAutoInstall>true</skipAutoInstall>
                        </configuration>
                        """.trimIndent(),
                ),
            )

        assertFailsWith<VerificationException> { verifier.executeGoal("validate") }

        verifier.verifyTextInLog("Sentry detected that OpenTelemetry was downgraded")
        verifier.resetStreams()
    }

    @Test
    fun `does nothing without a sentry opentelemetry dependency`() {
        val verifier =
            writePom(
                openTelemetryPom(
                    sentryVersion = sentryVersion,
                    useSpringBootParent = true,
                    otelDependency = "sentry",
                ),
            )

        verifier.executeGoal("validate")

        verifier.resetStreams()
    }
}
