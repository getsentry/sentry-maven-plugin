package io.sentry.autoinstall

import io.sentry.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SentryAutoInstallTest {

    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()

        fun getSut(
            installSentry: Boolean = true,
            sentryVersion: String = "6.28.0"

        ): SentryInstaller {
            if (!installSentry) {
                dependencies.add(
                    Dependency().apply {
                        groupId = "io.sentry"
                        artifactId = "sentry"
                        version = sentryVersion
                    }
                )
            }

            return SentryInstallerImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-log4j2 is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(false, sentryVersion = "6.25.2")
        val sentryVersion = sut.install(fixture.dependencies)

        assertEquals("6.25.2", sentryVersion)

        assertTrue {
            fixture.logger.capturedMessage ==
                "Sentry already installed 6.25.2"
        }

        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-log4j2" })
    }

    @Test
    fun `installs sentry with info message`() {
        val sut = fixture.getSut()

        val sentryVersion = sut.install(fixture.dependencies)

        assertEquals(SentryInstaller.SENTRY_VERSION, sentryVersion)

        assertTrue {
            fixture.logger.capturedMessage ==
                "Installing Sentry with version " + SentryInstaller.SENTRY_VERSION
        }

        assertTrue(fixture.dependencies.any { it.groupId == "io.sentry" && it.artifactId == "sentry" })
    }

    private class SentryInstallerImpl(logger: Logger) : SentryInstaller(logger)
}

