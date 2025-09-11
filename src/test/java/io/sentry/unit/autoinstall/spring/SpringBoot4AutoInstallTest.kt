package io.sentry.unit.autoinstall.spring

import io.sentry.autoinstall.AutoInstallState
import io.sentry.autoinstall.spring.SpringBoot4InstallStrategy
import io.sentry.unit.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class SpringBoot4AutoInstallTest {
    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val resolvedArtifacts = ArrayList<Artifact>()
        lateinit var installState: AutoInstallState

        fun getSut(
            installSpring: Boolean = true,
            springVersion: String = "4.0.0",
        ): SpringBoot4InstallStrategy {
            installState = AutoInstallState("8.21.0")
            installState.isInstallSpring = installSpring

            resolvedArtifacts.add(
                DefaultArtifact(
                    "org.springframework.boot",
                    "spring-boot-starter",
                    null,
                    springVersion,
                ),
            )

            return SpringBoot4InstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-spring-boot-4 is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(installSpring = false)
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-boot-4 won't be installed because it was already " +
                "installed directly"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring-boot-4" })
    }

    @Test
    fun `when spring version is too low logs a message and does nothing`() {
        val sut = fixture.getSut(springVersion = "3.5.5")
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-boot-4 won't be installed because the current " +
                "version (3.5.5) is lower than the minimum supported version 4.0.0-M1"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring-boot-4" })
    }

    @Test
    fun `when spring version is too high logs a message and does nothing`() {
        val sut = fixture.getSut(springVersion = "5.0.0")
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-boot-4 won't be installed because the current " +
                "version (5.0.0) is higher than the maximum supported version 4.9999.9999"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring-boot-4" })
    }

    @Test
    fun `installs sentry-spring-boot-4 with info message`() {
        val sut = fixture.getSut()
        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-boot-4 was successfully installed with version: 8.21.0"
        }

        assertTrue(fixture.dependencies.any { it.artifactId == "sentry-spring-boot-4" })
    }

    private class SpringBoot4InstallStrategyImpl(logger: Logger) : SpringBoot4InstallStrategy(logger)
}
