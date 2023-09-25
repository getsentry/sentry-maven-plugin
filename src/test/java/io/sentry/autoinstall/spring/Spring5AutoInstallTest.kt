package io.sentry.autoinstall.spring

import io.sentry.autoinstall.AutoInstallState
import io.sentry.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class Spring5AutoInstallTest {

    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val installState = AutoInstallState()


        fun getSut(
            installSpring: Boolean = true,
            springVersion: String = "5.1.2"

        ): Spring5InstallStrategy {
            dependencies.add(
                Dependency().apply {
                    groupId = "org.springframework"
                    artifactId = "spring-core"
                    version = springVersion
                }
            )

            installState.isInstallSpring = installSpring
            installState.sentryVersion = "6.25.2"

            return Spring5InstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-spring is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(installSpring = false)
        sut.install(fixture.dependencies, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring won't be installed because it was already " +
                "installed directly"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring" })
    }

    @Test
    fun `when spring version is too low logs a message and does nothing`() {
        val sut = fixture.getSut(springVersion = "5.1.1")
        sut.install(fixture.dependencies, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring won't be installed because the current " +
                "version is lower than the minimum supported version 5.1.2"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring" })
    }

    @Test
    fun `when spring version is too high logs a message and does nothing`() {
        val sut = fixture.getSut(springVersion = "6.0.0")
        sut.install(fixture.dependencies, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring won't be installed because the current " +
                "version is higher than the maximum supported version 5.9999.9999"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring" })
    }

    @Test
    fun `installs sentry-spring with info message`() {
        val sut = fixture.getSut()
        sut.install(fixture.dependencies, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring was successfully installed with version: 6.25.2"
        }

        assertTrue(fixture.dependencies.any { it.artifactId == "sentry-spring" })
    }

    private class Spring5InstallStrategyImpl(logger: Logger) : Spring5InstallStrategy(logger)

}
