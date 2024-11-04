package io.sentry.unit.autoinstall.graphql

import io.sentry.autoinstall.AutoInstallState
import io.sentry.autoinstall.graphql.Graphql22InstallStrategy
import io.sentry.unit.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class Graphql22AutoInstallTest {
    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val resolvedArtifacts = ArrayList<Artifact>()
        lateinit var installState: AutoInstallState

        fun getSut(
            installGraphql: Boolean = true,
            graphqlVersion: String = "22.0",
        ): Graphql22InstallStrategy {
            installState = AutoInstallState("8.0.0")
            installState.isInstallGraphql = installGraphql

            resolvedArtifacts.add(
                DefaultArtifact(
                    "com.graphql-java",
                    "graphql-java",
                    null,
                    graphqlVersion,
                ),
            )

            return Graphql22InstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-graphql is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(false)

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-graphql-22 won't be installed because it was already installed directly"
        }

        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-graphql-22" })
    }

    @Test
    fun `installs sentry-graphql with info message`() {
        val sut = fixture.getSut()

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-graphql-22 was successfully installed with version: 8.0.0"
        }

        assertTrue(fixture.dependencies.any { it.groupId == "io.sentry" && it.artifactId == "sentry-graphql-22" })
    }

    @Test
    fun `when graphql version is too low logs a message and does nothing`() {
        val sut = fixture.getSut(graphqlVersion = "21.9")

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-graphql-22 won't be installed because the current " +
                "version is lower than the minimum supported version 22.0.0"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-graphql-22" })
    }

    private class Graphql22InstallStrategyImpl(logger: Logger) : Graphql22InstallStrategy(logger)
}
