package io.sentry.autoinstall.jdbc

import io.sentry.autoinstall.AutoInstallState
import io.sentry.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class JdbcAutoIntallTest {

    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val installState = AutoInstallState()


        fun getSut(
            installJdbc: Boolean = true,
            jdbcVersion: String = "2.0.0"

        ): JdbcInstallStrategy {
            dependencies.add(
                Dependency().apply {
                    groupId = "org.mariadb.jdbc"
                    artifactId = "mariadb-java-client"
                    version = jdbcVersion
                }
            )

            installState.isInstallJdbc = installJdbc
            installState.sentryVersion = "6.25.2"

            return JdbcInstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-jdbc is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(false)

        sut.install(fixture.dependencies, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-jdbc won't be installed because it was already installed directly"
        }

        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-jdbc" })
    }

    @Test
    fun `installs sentry-jdbc with info message`() {
        val sut = fixture.getSut()

        sut.install(fixture.dependencies, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-jdbc was successfully installed with version: 6.25.2"
        }

        assertTrue(fixture.dependencies.any { it.groupId == "io.sentry" && it.artifactId == "sentry-jdbc" })
    }

    private class JdbcInstallStrategyImpl(logger: Logger) : JdbcInstallStrategy(logger)
}

