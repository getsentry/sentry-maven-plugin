package io.sentry.unit.autoinstall.graphql

import io.sentry.autoinstall.AutoInstallState
import io.sentry.autoinstall.graphql.GraphqlInstallStrategy
import io.sentry.unit.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class GraphqlAutoInstallTest {
    class Fixture {
        val logger = CapturingTestLogger()
        val dependencies = ArrayList<Dependency>()
        val resolvedArtifacts = ArrayList<Artifact>()
        lateinit var installState: AutoInstallState

        fun getSut(
            installGraphql: Boolean = true,
            graphqlVersion: String = "2.0.0",
        ): GraphqlInstallStrategy {
            installState = AutoInstallState("6.25.2")
            installState.isInstallGraphql = installGraphql

            resolvedArtifacts.add(
                DefaultArtifact(
                    "com.graphql-java",
                    "graphql-java",
                    null,
                    graphqlVersion,
                ),
            )

            return GraphqlInstallStrategyImpl(logger)
        }
    }

    private val fixture = Fixture()

    @Test
    fun `when sentry-graphql is a direct dependency logs a message and does nothing`() {
        val sut = fixture.getSut(false)

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-graphql won't be installed because it was already installed directly"
        }

        assertTrue(fixture.dependencies.none { it.groupId == "io.sentry" && it.artifactId == "sentry-graphql" })
    }

    @Test
    fun `installs sentry-graphql with info message`() {
        val sut = fixture.getSut()

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-graphql was successfully installed with version: 6.25.2"
        }

        assertTrue(fixture.dependencies.any { it.groupId == "io.sentry" && it.artifactId == "sentry-graphql" })
    }

    @Test
    fun `when graphql version is too high logs a message and does nothing`() {
        val sut = fixture.getSut(graphqlVersion = "22.1")

        sut.install(fixture.dependencies, fixture.resolvedArtifacts, fixture.installState)

        assertTrue {
            fixture.logger.capturedMessage ==
                "sentry-graphql won't be installed because the current " +
                "version is higher than the maximum supported version 21.9999.9999"
        }

        assertTrue(fixture.dependencies.none { it.artifactId == "sentry-graphql" })
    }

    private class GraphqlInstallStrategyImpl(logger: Logger) : GraphqlInstallStrategy(logger)
}
