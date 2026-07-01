package io.sentry.autoinstall.opentelemetry

import io.sentry.unit.fakes.CapturingTestLogger
import org.apache.maven.model.Dependency
import org.apache.maven.model.DependencyManagement
import org.apache.maven.model.Model
import org.apache.maven.project.MavenProject
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenTelemetryBomInstallerTest {
    private fun managed(
        groupId: String,
        artifactId: String,
        version: String?,
    ) = Dependency().apply {
        this.groupId = groupId
        this.artifactId = artifactId
        this.version = version
    }

    private fun project(
        effective: List<Dependency> = emptyList(),
        userManaged: List<Dependency> = emptyList(),
        userDirect: List<Dependency> = emptyList(),
    ): MavenProject {
        val model =
            Model().apply {
                dependencyManagement = DependencyManagement().apply { effective.forEach { addDependency(it) } }
            }
        val original =
            Model().apply {
                dependencyManagement = DependencyManagement().apply { userManaged.forEach { addDependency(it) } }
                userDirect.forEach { addDependency(it) }
            }
        return MavenProject(model).apply { originalModel = original }
    }

    private fun managedVersion(
        project: MavenProject,
        artifactId: String,
    ): String? =
        project.model.dependencyManagement.dependencies
            .firstOrNull { it.groupId == "io.opentelemetry" && it.artifactId == artifactId }
            ?.version

    @Test
    fun `overwrites inherited opentelemetry version in place`() {
        val project = project(effective = listOf(managed("io.opentelemetry", "opentelemetry-sdk", "1.43.0")))
        val logger = CapturingTestLogger()

        OpenTelemetryBomInstaller(logger).apply(project, mapOf("io.opentelemetry:opentelemetry-sdk" to "1.57.0"))

        assertEquals("1.57.0", managedVersion(project, "opentelemetry-sdk"))
        assertTrue { logger.capturedMessage?.contains("Aligning") == true }
    }

    @Test
    fun `adds managed entry when opentelemetry artifact is not managed`() {
        val project = project()

        OpenTelemetryBomInstaller(CapturingTestLogger())
            .apply(project, mapOf("io.opentelemetry:opentelemetry-sdk" to "1.57.0"))

        assertEquals("1.57.0", managedVersion(project, "opentelemetry-sdk"))
    }

    @Test
    fun `skips and warns when user pinned a different version in dependencyManagement`() {
        val project =
            project(
                effective = listOf(managed("io.opentelemetry", "opentelemetry-sdk", "1.39.0")),
                userManaged = listOf(managed("io.opentelemetry", "opentelemetry-sdk", "1.39.0")),
            )
        val logger = CapturingTestLogger()

        OpenTelemetryBomInstaller(logger).apply(project, mapOf("io.opentelemetry:opentelemetry-sdk" to "1.57.0"))

        assertEquals("1.39.0", managedVersion(project, "opentelemetry-sdk"))
        assertTrue { logger.capturedMessage?.contains("is pinned to 1.39.0") == true }
    }

    @Test
    fun `skips user direct dependency with explicit version`() {
        val project =
            project(
                userDirect = listOf(managed("io.opentelemetry", "opentelemetry-sdk", "1.39.0")),
            )

        OpenTelemetryBomInstaller(CapturingTestLogger())
            .apply(project, mapOf("io.opentelemetry:opentelemetry-sdk" to "1.57.0"))

        assertNull(managedVersion(project, "opentelemetry-sdk"))
    }

    @Test
    fun `does not warn when user pin matches bom version`() {
        val project =
            project(userManaged = listOf(managed("io.opentelemetry", "opentelemetry-sdk", "1.57.0")))
        val logger = CapturingTestLogger()

        OpenTelemetryBomInstaller(logger).apply(project, mapOf("io.opentelemetry:opentelemetry-sdk" to "1.57.0"))

        assertNull(logger.capturedMessage)
    }

    @Test
    fun `does not warn when user pin uses a property placeholder`() {
        val project =
            project(userManaged = listOf(managed("io.opentelemetry", "opentelemetry-sdk", "\${otel.version}")))
        val logger = CapturingTestLogger()

        OpenTelemetryBomInstaller(logger).apply(project, mapOf("io.opentelemetry:opentelemetry-sdk" to "1.57.0"))

        assertNull(logger.capturedMessage)
    }

    @Test
    fun `userPinnedVersions reads managed and direct explicit versions only`() {
        val original =
            Model().apply {
                dependencyManagement =
                    DependencyManagement().apply { addDependency(managed("io.opentelemetry", "opentelemetry-api", "1.39.0")) }
                addDependency(managed("io.opentelemetry", "opentelemetry-sdk", "1.40.0"))
                addDependency(managed("io.opentelemetry", "opentelemetry-context", null))
            }

        val pinned = OpenTelemetryBomInstaller.userPinnedVersions(original)

        assertEquals("1.39.0", pinned["io.opentelemetry:opentelemetry-api"])
        assertEquals("1.40.0", pinned["io.opentelemetry:opentelemetry-sdk"])
        assertFalse { pinned.containsKey("io.opentelemetry:opentelemetry-context") }
    }

    @Test
    fun `eligibility detects sentry opentelemetry dependency excluding bom and agent`() {
        assertTrue {
            OpenTelemetryBomInstaller.hasEligibleOpenTelemetryDependency(
                listOf(artifact("io.sentry", "sentry-opentelemetry-agentless", "8.34.1")),
            )
        }
        assertFalse {
            OpenTelemetryBomInstaller.hasEligibleOpenTelemetryDependency(
                listOf(artifact("io.sentry", "sentry-opentelemetry-agent", "8.34.1")),
            )
        }
        assertFalse {
            OpenTelemetryBomInstaller.hasEligibleOpenTelemetryDependency(
                listOf(artifact("io.sentry", "sentry-opentelemetry-bom", "8.34.1")),
            )
        }
        assertFalse {
            OpenTelemetryBomInstaller.hasEligibleOpenTelemetryDependency(
                listOf(artifact("io.sentry", "sentry", "8.34.1")),
            )
        }
    }

    @Test
    fun `detects bom already managed via original model`() {
        val withBom =
            project(
                userManaged =
                    listOf(
                        Dependency().apply {
                            groupId = "io.sentry"
                            artifactId = "sentry-opentelemetry-bom"
                            version = "8.34.1"
                            type = "pom"
                            scope = "import"
                        },
                    ),
            )
        assertTrue { OpenTelemetryBomInstaller.isBomAlreadyManaged(withBom) }
        assertFalse { OpenTelemetryBomInstaller.isBomAlreadyManaged(project()) }
    }

    private fun artifact(
        groupId: String,
        artifactId: String,
        version: String,
    ): Artifact = DefaultArtifact(groupId, artifactId, null, version)
}
