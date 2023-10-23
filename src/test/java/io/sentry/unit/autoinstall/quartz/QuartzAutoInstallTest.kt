package io.sentry.unit.autoinstall.quartz

import io.sentry.autoinstall.AutoInstallState
import io.sentry.autoinstall.quartz.QuartzInstallStrategy
import io.sentry.unit.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class QuartzAutoInstallTest {
    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val resolvedArtifacts = ArrayList<Artifact>()
        val installState = AutoInstallState()

        fun getSut(
            installQuartz: Boolean = true,
            quartzVersion: String = "2.0.0",
            sentryVersion: String = "6.30.0",
        ): QuartzInstallStrategy {
            resolvedArtifacts.add(
                DefaultArtifact(
                    "org.quartz-scheduler",
                    "quartz",
                    null,
                    quartzVersion,
                ),
            )

            installState.isInstallQuartz = installQuartz
            installState.sentryVersion = sentryVersion

            return QuartzInstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-quartz is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(false)

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-quartz won't be installed because it was already installed directly"
        }

        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-quartz" })
    }

    @Test
    fun `installs sentry-quartz with info message`() {
        val sut = fixture.getSut()

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-quartz was successfully installed with version: 6.30.0"
        }

        assertTrue(fixture.dependencies.any { it.groupId == "io.sentry" && it.artifactId == "sentry-quartz" })
    }

    @Test
    fun `when sentry version is too low logs message and does nothing`() {
        val sut = fixture.getSut(sentryVersion = "6.28.0")

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-quartz won't be installed because the current version is " +
                "lower than the minimum supported sentry version 6.28.0"
        }

        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-quartz" })
    }

    private class QuartzInstallStrategyImpl(logger: Logger) : QuartzInstallStrategy(logger)
}
