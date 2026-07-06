package io.sentry.autoinstall.opentelemetry

import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenTelemetryVersionCheckerTest {
    private fun artifact(
        groupId: String,
        artifactId: String,
        version: String,
    ): Artifact = DefaultArtifact(groupId, artifactId, null, version)

    @Test
    fun `flags a downgraded opentelemetry module`() {
        val downgrades =
            OpenTelemetryVersionChecker.collectDowngrades(
                mapOf("io.opentelemetry:opentelemetry-sdk" to "1.57.0"),
                mapOf("io.opentelemetry:opentelemetry-sdk" to "1.43.0"),
            )

        assertEquals(1, downgrades.size)
        assertEquals("io.opentelemetry:opentelemetry-sdk", downgrades[0].module)
        assertEquals("1.57.0", downgrades[0].required)
        assertEquals("1.43.0", downgrades[0].resolved)
    }

    @Test
    fun `does not flag matching versions`() {
        val downgrades =
            OpenTelemetryVersionChecker.collectDowngrades(
                mapOf("io.opentelemetry:opentelemetry-sdk" to "1.57.0"),
                mapOf("io.opentelemetry:opentelemetry-sdk" to "1.57.0"),
            )
        assertTrue { downgrades.isEmpty() }
    }

    @Test
    fun `does not flag upgrades`() {
        val downgrades =
            OpenTelemetryVersionChecker.collectDowngrades(
                mapOf("io.opentelemetry:opentelemetry-sdk" to "1.57.0"),
                mapOf("io.opentelemetry:opentelemetry-sdk" to "1.60.0"),
            )
        assertTrue { downgrades.isEmpty() }
    }

    @Test
    fun `ignores modules that are not resolved in the project`() {
        val downgrades =
            OpenTelemetryVersionChecker.collectDowngrades(
                mapOf("io.opentelemetry:opentelemetry-exporter-zipkin" to "1.57.0"),
                mapOf("io.opentelemetry:opentelemetry-sdk" to "1.43.0"),
            )
        assertTrue { downgrades.isEmpty() }
    }

    @Test
    fun `flags downgraded alpha versions`() {
        val downgrades =
            OpenTelemetryVersionChecker.collectDowngrades(
                mapOf("io.opentelemetry:opentelemetry-semconv-incubating" to "1.37.0-alpha"),
                mapOf("io.opentelemetry:opentelemetry-semconv-incubating" to "1.36.0-alpha"),
            )
        assertEquals(1, downgrades.size)
    }

    @Test
    fun `does not flag unparseable versions`() {
        val downgrades =
            OpenTelemetryVersionChecker.collectDowngrades(
                mapOf("io.opentelemetry:opentelemetry-sdk" to "1.57.0"),
                mapOf("io.opentelemetry:opentelemetry-sdk" to "weird-version"),
            )
        assertTrue { downgrades.isEmpty() }
    }

    @Test
    fun `detects an eligible sentry opentelemetry dependency`() {
        assertTrue {
            OpenTelemetryVersionChecker.hasSentryOpenTelemetryDependency(
                listOf(artifact("io.sentry", "sentry-opentelemetry-agentless", "8.34.1")),
            )
        }
        assertFalse {
            OpenTelemetryVersionChecker.hasSentryOpenTelemetryDependency(
                listOf(artifact("io.sentry", "sentry-opentelemetry-bom", "8.34.1")),
            )
        }
        assertFalse {
            OpenTelemetryVersionChecker.hasSentryOpenTelemetryDependency(
                listOf(artifact("io.sentry", "sentry", "8.34.1")),
            )
        }
    }

    @Test
    fun `message includes the mismatch, the bom snippet and the opt-out`() {
        val message =
            OpenTelemetryVersionChecker.buildMessage(
                listOf(
                    OpenTelemetryVersionChecker.VersionMismatch(
                        "io.opentelemetry:opentelemetry-sdk",
                        "1.57.0",
                        "1.43.0",
                    ),
                ),
                "8.34.1",
                false,
            )

        assertTrue { message.contains("io.opentelemetry:opentelemetry-sdk: Sentry requires 1.57.0 but 1.43.0 was resolved") }
        assertTrue { message.contains("<artifactId>sentry-opentelemetry-bom</artifactId>") }
        assertTrue { message.contains("<version>8.34.1</version>") }
        assertTrue { message.contains("skipValidateOpenTelemetryVersions") }
    }

    @Test
    fun `message mentions ordering when spring boot is present`() {
        val message =
            OpenTelemetryVersionChecker.buildMessage(
                listOf(
                    OpenTelemetryVersionChecker.VersionMismatch(
                        "io.opentelemetry:opentelemetry-sdk",
                        "1.57.0",
                        "1.43.0",
                    ),
                ),
                "8.34.1",
                true,
            )

        assertTrue { message.contains("before spring-boot-dependencies") }
    }
}
