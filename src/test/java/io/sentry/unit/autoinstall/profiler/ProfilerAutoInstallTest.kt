package io.sentry.unit.autoinstall.profiler

import io.sentry.autoinstall.AutoInstallState
import io.sentry.autoinstall.profiler.ProfilerInstallStrategy
import io.sentry.unit.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.eclipse.aether.artifact.Artifact
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class ProfilerAutoInstallTest {
    class Fixture {
        val logger = CapturingTestLogger()
        var dependencies = ArrayList<Dependency>()
        val resolvedArtifacts = ArrayList<Artifact>()
        lateinit var installState: AutoInstallState

        fun getSut(
            installProfiler: Boolean = true,
            sentryVersion: String = "8.23.0",
        ): ProfilerInstallStrategy {
            dependencies = ArrayList<Dependency>()
            installState = AutoInstallState(sentryVersion)
            installState.isInstallProfiler = installProfiler

            return ProfilerInstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when installProfiler flag is false does not install`() {
        val sut = fixture.getSut(installProfiler = false)

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-async-profiler won't be installed because it was already installed directly"
        }

        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-async-profiler" })
    }

    @Test
    fun `installs sentry-async-profiler with info message`() {
        val sut = fixture.getSut()

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-async-profiler was successfully installed with version: 8.23.0"
        }

        assertTrue(fixture.dependencies.any { it.groupId == "io.sentry" && it.artifactId == "sentry-async-profiler" })
    }

    @Test
    fun `when sentry version is too low logs message and does nothing`() {
        val sut = fixture.getSut(sentryVersion = "8.22.0")

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-async-profiler won't be installed because the current version (8.22.0) is " +
                "lower than the minimum supported sentry version 8.22.0"
        }

        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-async-profiler" })
    }

    @Test
    fun `installs with higher sentry version`() {
        val sut = fixture.getSut(sentryVersion = "9.0.0")

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-async-profiler was successfully installed with version: 9.0.0"
        }

        assertTrue(fixture.dependencies.any { it.groupId == "io.sentry" && it.artifactId == "sentry-async-profiler" })
    }

    private class ProfilerInstallStrategyImpl(logger: Logger) : ProfilerInstallStrategy(logger)
}
