package io.sentry.unit.autoinstall.log4j2

import io.sentry.autoinstall.AutoInstallState
import io.sentry.autoinstall.log4j2.Log4j2InstallStrategy
import io.sentry.unit.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class Log4jAutoIntallTest {
    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val resolvedArtifacts = ArrayList<Artifact>()
        val installState = AutoInstallState()

        fun getSut(
            installLog4j2: Boolean = true,
            log4j2Version: String = "2.0.0",
        ): Log4j2InstallStrategy {
            resolvedArtifacts.add(
                DefaultArtifact(
                    "org.apache.logging.log4j",
                    "log4j-api",
                    null,
                    log4j2Version,
                ),
            )

            installState.isInstallLog4j2 = installLog4j2
            installState.sentryVersion = "6.25.2"

            return Log4j2InstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-log4j2 is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(false)
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-log4j2 won't be installed because it was already installed directly"
        }

        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-log4j2" })
    }

    @Test
    fun `when log4j2 version is unsupported logs a message and does nothing`() {
        val sut = fixture.getSut(log4j2Version = "1.0.0")
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-log4j2 won't be installed because the current " +
                "version is lower than the minimum supported version 2.0.0"
        }
        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-log4j2" })
    }

    @Test
    fun `installs sentry-log4j2 with info message`() {
        val sut = fixture.getSut()
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-log4j2 was successfully installed with version: 6.25.2"
        }

        assertTrue(fixture.dependencies.any { it.groupId == "io.sentry" && it.artifactId == "sentry-log4j2" })
    }

    private class Log4j2InstallStrategyImpl(logger: Logger) : Log4j2InstallStrategy(logger)
}
