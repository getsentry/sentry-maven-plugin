package io.sentry.unit.autoinstall

import io.sentry.autoinstall.SentryInstaller
import io.sentry.autoinstall.util.SdkVersionInfo
import io.sentry.unit.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.slf4j.Logger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SentryAutoInstallTest {
    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val resolvedArtifacts = ArrayList<Artifact>()

        fun getSut(
            installSentry: Boolean = true,
            sentryVersion: String = "6.28.0",
        ): SentryInstaller {
            if (!installSentry) {
                resolvedArtifacts.add(
                    DefaultArtifact(
                        "io.sentry",
                        "sentry",
                        null,
                        sentryVersion,
                    ),
                )
            }

            return SentryInstallerImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry is already installed logs a message and does nothing`() {
        val sut = fixture.getSut(false, sentryVersion = "6.25.2")
        val sentryVersion = sut.install(fixture.dependencies, fixture.resolvedArtifacts)

        assertEquals("6.25.2", sentryVersion)

        assertTrue {
            fixture.logger.capturedMessage ==
                "Sentry already installed 6.25.2"
        }

        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry" })
    }

    @Test
    fun `installs sentry with info message`() {
        val sut = fixture.getSut()

        val sentryVersion = sut.install(fixture.dependencies, fixture.resolvedArtifacts)

        assertEquals(SdkVersionInfo.getSentryVersion(), sentryVersion)

        assertTrue {
            fixture.logger.capturedMessage ==
                "Installing Sentry with version ${SdkVersionInfo.getSentryVersion()}"
        }

        assertTrue(fixture.dependencies.any { it.groupId == "io.sentry" && it.artifactId == "sentry" })
    }

    @Test
    fun `if sentry version cannot be resolved, don't install sentry and return null`() {
        val sut = fixture.getSut()

        Mockito.mockStatic(SdkVersionInfo::class.java).use { utilities ->
            utilities.`when`<String>(SdkVersionInfo::getSentryVersion).thenReturn(null)

            val sentryVersion = sut.install(fixture.dependencies, fixture.resolvedArtifacts)

            assertEquals(null, sentryVersion)

            assertTrue {
                fixture.logger.capturedMessage ==
                    "Unable to load sentry version, Sentry SDK cannot be auto-installed"
            }

            assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry" })
        }
    }

    private class SentryInstallerImpl(logger: Logger) : SentryInstaller(logger)
}
