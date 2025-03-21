package io.sentry.integration.reportDependencies

import io.sentry.ReportDependenciesMojo.EXTERNAL_MODULES_FILE
import io.sentry.integration.installMavenWrapper
import org.apache.maven.it.VerificationException
import org.apache.maven.it.Verifier
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.test.assertEquals

class ReportDependenciesTestIT {
    @TempDir()
    lateinit var file: File

    @BeforeEach
    fun installWrapper() {
        installMavenWrapper(file, "3.6.3")
    }

    fun getPOM(
        skipPlugin: Boolean = false,
        skipReportDependencies: Boolean = false,
    ): String {
        val pomContent = basePom(skipPlugin, skipReportDependencies)

        Files.write(Path("${file.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        return file.absolutePath
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun verifyDependencyReport() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        file.verifyContents()
        verifier.verifyFilePresent("target/classes/$EXTERNAL_MODULES_FILE")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun verifyDependencyReportNotWrittenIfPluginIsSkipped() {
        val path = getPOM(skipPlugin = true)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyFileNotPresent("target/classes/$EXTERNAL_MODULES_FILE")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun verifyDependencyReportNotWrittenIfDependencyReportIsSkipped() {
        val path = getPOM(skipReportDependencies = true)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyFileNotPresent("target/classes/$EXTERNAL_MODULES_FILE")
        verifier.resetStreams()
    }

    private fun File.verifyContents() {
        assertEquals(
            """
            com.graphql-java:graphql-java:2.0.0
            io.sentry:sentry-graphql:6.32.0
            io.sentry:sentry:6.32.0
            org.antlr:antlr4-runtime:4.5.1
            org.slf4j:slf4j-api:1.7.12
            """.trimIndent(),
            File(this, "target/classes/$EXTERNAL_MODULES_FILE").readText(),
        )
    }
}
