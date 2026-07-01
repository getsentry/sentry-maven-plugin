package io.sentry.unit.autoinstall

import io.sentry.Constants.SENTRY_BOM_ARTIFACT_ID
import io.sentry.Constants.SENTRY_GROUP_ID
import io.sentry.Constants.SENTRY_OPENTELEMETRY_BOM_ARTIFACT_ID
import io.sentry.autoinstall.util.ManagedSentryVersionResolver
import org.apache.maven.model.Dependency
import org.apache.maven.model.DependencyManagement
import org.apache.maven.model.Model
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ManagedSentryVersionResolverTest {
    @Test
    fun `detects sentry bom version`() {
        val project = projectWithDependencyManagement(bom(SENTRY_BOM_ARTIFACT_ID, "8.33.0"))

        assertEquals("8.33.0", ManagedSentryVersionResolver.getManagedSentryVersion(project))
    }

    @Test
    fun `detects sentry opentelemetry bom version`() {
        val project = projectWithDependencyManagement(bom(SENTRY_OPENTELEMETRY_BOM_ARTIFACT_ID, "8.32.0"))

        assertEquals("8.32.0", ManagedSentryVersionResolver.getManagedSentryVersion(project))
    }

    @Test
    fun `resolves bom version property`() {
        val project = projectWithDependencyManagement(bom(SENTRY_BOM_ARTIFACT_ID, "\${sentry.version}"))
        project.properties.setProperty("sentry.version", "8.31.0")

        assertEquals("8.31.0", ManagedSentryVersionResolver.getManagedSentryVersion(project))
    }

    private fun projectWithDependencyManagement(vararg dependencies: Dependency): MavenProject {
        val dependencyManagement = DependencyManagement()
        dependencies.forEach(dependencyManagement::addDependency)

        val model = Model()
        model.dependencyManagement = dependencyManagement

        return MavenProject(model)
    }

    private fun bom(
        artifactId: String,
        version: String,
    ): Dependency {
        val dependency = Dependency()
        dependency.groupId = SENTRY_GROUP_ID
        dependency.artifactId = artifactId
        dependency.version = version
        dependency.type = "pom"
        dependency.scope = "import"
        return dependency
    }
}
