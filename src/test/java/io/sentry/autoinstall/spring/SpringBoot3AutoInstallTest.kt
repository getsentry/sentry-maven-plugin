package io.sentry.autoinstall.spring

import io.sentry.autoinstall.AutoInstallState
import io.sentry.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class SpringBoot3AutoInstallTest {

    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val installState = AutoInstallState()


        fun getSut(installSpring: Boolean = true,
                   springVersion: String = "3.0.0"

        ): SpringBoot3InstallStrategy {
            dependencies.add(
                Dependency().apply {
                    groupId = "org.springframework.boot"
                    artifactId = "spring-boot-starter"
                    version = springVersion
                }
            )

            installState.isInstallSpring = installSpring
            installState.sentryVersion = "6.28.0"

            return SpringBoot3InstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-spring-boot-jakarta is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(installSpring = false)
        sut.install(fixture.dependencies, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-boot-jakarta won't be installed because it was already " +
                "installed directly"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring-boot-jakarta" })
    }

    @Test
    fun `when spring version is too low logs a message and does nothing`() {
        val sut = fixture.getSut(springVersion = "2.7.13")
        sut.install(fixture.dependencies, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-boot-jakarta won't be installed because the current " +
                "version is lower than the minimum supported version 3.0.0"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-spring-boot-jakarta" })
    }

    @Test
    fun `installs sentry-spring-boot-jakarta with info message`() {
        val sut = fixture.getSut()
        sut.install(fixture.dependencies, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-spring-boot-jakarta was successfully installed with version: 6.28.0"
        }

        assertTrue(fixture.dependencies.any { it.artifactId == "sentry-spring-boot-jakarta" })
    }

    private class SpringBoot3InstallStrategyImpl(logger: Logger) : SpringBoot3InstallStrategy(logger)

}
