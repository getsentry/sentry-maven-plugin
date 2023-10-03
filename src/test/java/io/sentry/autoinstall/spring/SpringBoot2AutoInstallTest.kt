package io.sentry.autoinstall.spring

import io.sentry.autoinstall.AutoInstallState
import io.sentry.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class SpringBoot2AutoInstallTest {

    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val resolvedArtifacts = ArrayList<Artifact>()
        val installState = AutoInstallState()


        fun getSut(
            installSpring: Boolean = true,
            springVersion: String = "2.1.0"

        ): SpringBoot2InstallStrategy {

            resolvedArtifacts.add(
                DefaultArtifact(
                    "org.springframework.boot",
                    "spring-boot-starter",
                    null,
                    springVersion
                )
            )

            installState.isInstallSpring = installSpring
            installState.sentryVersion = "6.28.0"

            return SpringBoot2InstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-spring-boot is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(installSpring = false)
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-boot won't be installed because it was already " +
                "installed directly"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring-boot" })
    }

    @Test
    fun `when spring version is too low logs a message and does nothing`() {
        val sut = fixture.getSut(springVersion = "2.0.0")
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-boot won't be installed because the current " +
                "version is lower than the minimum supported version 2.1.0"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring-boot" })
    }

    @Test
    fun `when spring version is too high logs a message and does nothing`() {
        val sut = fixture.getSut(springVersion = "3.0.0")
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-boot won't be installed because the current " +
                "version is higher than the maximum supported version 2.9999.9999"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring-boot" })
    }

    @Test
    fun `installs sentry-spring-boot with info message`() {
        val sut = fixture.getSut()
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-boot was successfully installed with version: 6.28.0"
        }

        assertTrue(fixture.dependencies.any { it.artifactId == "sentry-spring-boot" })
    }

    private class SpringBoot2InstallStrategyImpl(logger: Logger) : SpringBoot2InstallStrategy(logger)

}
