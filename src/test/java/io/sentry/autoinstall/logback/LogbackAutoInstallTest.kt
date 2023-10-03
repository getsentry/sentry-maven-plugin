package io.sentry.autoinstall.logback

import io.sentry.autoinstall.AutoInstallState
import io.sentry.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class LogbackAutoInstallTest {

    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val resolvedArtifacts = ArrayList<Artifact>()
        val installState = AutoInstallState()


        fun getSut(
            installLogback: Boolean = true,
            logbackVersion: String = "2.0.0"

        ): LogbackInstallStrategy {
            resolvedArtifacts.add(
                DefaultArtifact(
                    "ch.qos.logback",
                    "logback-classic",
                    null,
                    logbackVersion
                )
            )

            installState.isInstallLogback = installLogback
            installState.sentryVersion = "6.25.2"

            return LogbackInstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-logback is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(false)
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-logback won't be installed because it was already installed directly"
        }

        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-logback" })
    }

    @Test
    fun `when logback version is unsupported logs a message and does nothing`() {
        val sut = fixture.getSut(logbackVersion = "0.0.1")
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-logback won't be installed because the current " +
                "version is lower than the minimum supported version 1.0.0"
        }
        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-logback" })
    }

    @Test
    fun `installs sentry-logback with info message`() {
        val sut = fixture.getSut()
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-logback was successfully installed with version: 6.25.2"
        }

        assertTrue(fixture.dependencies.any { it.groupId == "io.sentry" && it.artifactId == "sentry-logback" })
    }

    private class LogbackInstallStrategyImpl(logger: Logger) : LogbackInstallStrategy(logger)
}

