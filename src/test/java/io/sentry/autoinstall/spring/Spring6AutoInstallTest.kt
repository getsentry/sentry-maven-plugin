package io.sentry.autoinstall.spring

import io.sentry.autoinstall.AutoInstallState
import io.sentry.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class Spring6AutoInstallTest {

    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val resolvedArtifacts = ArrayList<Artifact>()
        val installState = AutoInstallState()


        fun getSut(
            installSpring: Boolean = true,
            springVersion: String = "6.0.0"

        ): Spring6InstallStrategy {
            resolvedArtifacts.add(
                DefaultArtifact(
                    "org.springframework",
                    "spring-core",
                    null,
                    springVersion
                )
            )

            installState.isInstallSpring = installSpring
            installState.sentryVersion = "6.25.2"

            return Spring6InstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-spring-jakarta is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(installSpring = false)
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-jakarta won't be installed because it was already " +
                "installed directly"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring-jakarta" })
    }

    @Test
    fun `when spring version is too low logs a message and does nothing`() {
        val sut = fixture.getSut(springVersion = "5.7.4")
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-jakarta won't be installed because the current " +
                "version is lower than the minimum supported version 6.0.0"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring-jakarta" })
    }

    @Test
    fun `installs sentry-spring-jakarta with info message`() {
        val sut = fixture.getSut()
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-jakarta was successfully installed with version: 6.25.2"
        }

        assertTrue(fixture.dependencies.any { it.artifactId == "sentry-spring-jakarta" })
    }

    private class Spring6InstallStrategyImpl(logger: Logger) : Spring6InstallStrategy(logger)

}
