package io.sentry.integration.autoinstall.opentelemetry

import io.sentry.autoinstall.util.SdkVersionInfo
import io.sentry.integration.installMavenWrapper
import org.apache.maven.it.Verifier
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class OpenTelemetryBomAutoInstallTestIT {
    @TempDir
    lateinit var file: File

    private val sentryVersion: String = SdkVersionInfo.getSentryVersion()!!
    private val bomOtelVersion = "1.40.0"

    @BeforeEach
    fun setup() {
        installMavenWrapper(file, "3.8.6")
        val repoDir = File(file, "repo")
        writeFakeOpenTelemetryBom(repoDir, sentryVersion, bomOtelVersion)
    }

    @AfterEach
    fun cleanup() {
        // The fake BOM served from the file repo is cached into the local repo during the build.
        // Remove it so it can't shadow the real artifact in later builds.
        val localRepo =
            System.getProperty("maven.repo.local")
                ?: (System.getProperty("user.home") + "/.m2/repository")
        File(localRepo, "io/sentry/sentry-opentelemetry-bom").deleteRecursively()
    }

    private fun writePom(content: String): Verifier {
        Files.write(Path("${file.absolutePath}/pom.xml"), content.toByteArray(), StandardOpenOption.CREATE)
        return Verifier(file.absolutePath).apply {
            isAutoclean = false
            deleteDirectory("target")
        }
    }

    private fun repoUrl(): String = File(file, "repo").toURI().toString()

    @Test
    fun `aligns OpenTelemetry versions inherited from spring-boot-starter-parent`() {
        val verifier =
            writePom(
                openTelemetryPom(
                    repoUrl = repoUrl(),
                    sentryVersion = sentryVersion,
                    useSpringBootParent = true,
                ),
            )

        verifier.executeGoal("install")

        verifier.verifyFilePresent("target/lib/opentelemetry-sdk-$bomOtelVersion.jar")
        verifier.verifyFileNotPresent("target/lib/opentelemetry-sdk-1.43.0.jar")
        verifier.resetStreams()
    }

    @Test
    fun `aligns transitive OpenTelemetry versions without spring boot`() {
        val verifier =
            writePom(openTelemetryPom(repoUrl = repoUrl(), sentryVersion = sentryVersion))

        verifier.executeGoal("install")

        verifier.verifyFilePresent("target/lib/opentelemetry-sdk-$bomOtelVersion.jar")
        verifier.verifyFileNotPresent("target/lib/opentelemetry-sdk-1.57.0.jar")
        verifier.resetStreams()
    }

    @Test
    fun `respects a user pinned OpenTelemetry version and warns`() {
        val verifier =
            writePom(
                openTelemetryPom(
                    repoUrl = repoUrl(),
                    sentryVersion = sentryVersion,
                    userPinnedOtelVersion = "1.39.0",
                ),
            )

        verifier.executeGoal("install")

        verifier.verifyFilePresent("target/lib/opentelemetry-sdk-1.39.0.jar")
        verifier.verifyFileNotPresent("target/lib/opentelemetry-sdk-$bomOtelVersion.jar")
        verifier.verifyTextInLog("opentelemetry-sdk is pinned to 1.39.0")
        verifier.resetStreams()
    }

    @Test
    fun `does nothing when skipInstallOpenTelemetryBom is true`() {
        val verifier =
            writePom(
                openTelemetryPom(
                    repoUrl = repoUrl(),
                    sentryVersion = sentryVersion,
                    pluginConfiguration =
                        """
                        <configuration>
                            <skipInstallOpenTelemetryBom>true</skipInstallOpenTelemetryBom>
                        </configuration>
                        """.trimIndent(),
                ),
            )

        verifier.executeGoal("install")

        verifier.verifyFilePresent("target/lib/opentelemetry-sdk-1.57.0.jar")
        verifier.verifyFileNotPresent("target/lib/opentelemetry-sdk-$bomOtelVersion.jar")
        verifier.resetStreams()
    }

    @Test
    fun `does nothing for sentry-opentelemetry-agent`() {
        val verifier =
            writePom(
                openTelemetryPom(
                    repoUrl = repoUrl(),
                    sentryVersion = sentryVersion,
                    otelDependency = "sentry-opentelemetry-agent",
                ),
            )

        verifier.executeGoal("install")

        verifier.verifyTextInLog(
            "sentry-opentelemetry-bom won't be installed because no Sentry OpenTelemetry dependency was found",
        )
        verifier.resetStreams()
    }

    @Test
    fun `does nothing when bom is already imported`() {
        val verifier =
            writePom(
                openTelemetryPom(
                    repoUrl = repoUrl(),
                    sentryVersion = sentryVersion,
                    importBom = true,
                ),
            )

        verifier.executeGoal("install")

        verifier.verifyTextInLog(
            "sentry-opentelemetry-bom won't be installed because it was already installed directly",
        )
        verifier.resetStreams()
    }
}
