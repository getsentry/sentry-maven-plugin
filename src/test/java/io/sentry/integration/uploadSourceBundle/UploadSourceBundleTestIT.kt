package io.sentry.integration.uploadSourceBundle

import io.sentry.SentryCliProvider
import io.sentry.integration.installMavenWrapper
import org.apache.maven.it.VerificationException
import org.apache.maven.it.Verifier
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.Properties
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UploadSourceBundleTestIT {
    @TempDir()
    lateinit var file: File

    fun getPOM(
        baseDir: File,
        skipPlugin: Boolean = false,
        skipSourceBundle: Boolean = false,
        ignoreSourceBundleUploadFailure: Boolean = false,
        reproducibleBundleId: Boolean = false,
        sentryCliPath: String? = null,
        extraSourceRoots: List<String> = listOf(),
        extraSourceContextDirs: List<String> = emptyList(),
        sentryUrl: String? = null,
    ): String {
        val pomContent =
            basePom(
                skipPlugin,
                skipSourceBundle,
                ignoreSourceBundleUploadFailure,
                reproducibleBundleId,
                sentryCliPath,
                extraSourceRoots,
                extraSourceContextDirs,
                sentryUrl,
            )

        Files.write(Path("${baseDir.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        return baseDir.absolutePath
    }

    @Test
    fun `does not fail build when upload fails and ignoreSourceBundleUploadFailure is true`() {
        val baseDir = setupProject()
        val path = getPOM(baseDir, ignoreSourceBundleUploadFailure = true, sentryUrl = "http://unknown")
        val verifier = Verifier(path)
        verifier.isAutoclean = false

        verifier.executeGoal("install")
        verifier.verifyTextInLog("Source bundle upload failed, ignored by configuration")
        verifier.resetStreams()
    }

    @Test
    fun `fails build when upload fails and ignoreSourceBundleUploadFailure is false`() {
        val baseDir = setupProject()
        val path = getPOM(baseDir, sentryUrl = "http://unknown")
        val verifier = Verifier(path)
        verifier.isAutoclean = false

        assertFailsWith<VerificationException> {
            verifier.executeGoal("install")
        }

        verifier.verifyTextInLog("Could not resolve hostname (Could not resolve host: unknown)")
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `uploads source bundle`() {
        val baseDir = setupProject()
        val path = getPOM(baseDir)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("Collected sources from 1 source directories")
        val output = verifier.loadLines(verifier.logFileName, Charset.defaultCharset().name()).joinToString("\n")
        assertTrue(bundleUploadedSuccessfully(baseDir, output))

        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `uploads source bundle when project path contains whitespaces`() {
        val baseDir = setupProject(baseDir = "base with spaces")
        val path = getPOM(baseDir)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("Collected sources from 1 source directories")
        val output = verifier.loadLines(verifier.logFileName, Charset.defaultCharset().name()).joinToString("\n")
        assertTrue(bundleUploadedSuccessfully(baseDir, output))

        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `uploads source bundle when project path and cli path contain whitespaces`() {
        val cliPath = SentryCliProvider.getCliPath(MavenProject(), null)
        val baseDir = setupProject(baseDir = "base with spaces")
        val cliPathWithSpaces = Files.copy(Path(cliPath), Path(baseDir.absolutePath, "sentry-cli"))
        cliPathWithSpaces.toFile().setExecutable(true)

        val path = getPOM(baseDir, sentryCliPath = cliPathWithSpaces.absolutePathString())

        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("Collected sources from 1 source directories")
        val output = verifier.loadLines(verifier.logFileName, Charset.defaultCharset().name()).joinToString("\n")
        assertTrue(bundleUploadedSuccessfully(baseDir, output))

        verifier.resetStreams()
    }

    @Test
    fun `does not fail build when base directory is empty`() {
        val cliPath = SentryCliProvider.getCliPath(MavenProject(), null)
        val baseDir = setupEmptyProject()

        val path = getPOM(baseDir, sentryCliPath = cliPath)

        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")

        verifier.verifyTextInLog("Collected sources from 0 source directories")
    }

    @Test
    fun `uploads source bundle with multiple source roots`() {
        val roots = listOf("src/main/java", "src/main/kotlin", "src/main/groovy")
        val baseDir = setupProject(subdirectories = roots)
        val path = getPOM(baseDir, extraSourceRoots = roots)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("Collected sources from 3 source directories")
        val output = verifier.loadLines(verifier.logFileName, Charset.defaultCharset().name()).joinToString("\n")
        assertTrue(bundleUploadedSuccessfully(baseDir, output))

        verifier.resetStreams()
    }

    @Test
    fun `uploads source bundle with single source root and extra source dirs`() {
        val roots = listOf("src/main/java")
        val extraDirs = listOf("src/main/extra1", "src/main/extra2")
        val baseDir = setupProject(subdirectories = roots + extraDirs)
        val path = getPOM(baseDir, extraSourceContextDirs = extraDirs)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("Collected sources from 3 source directories")
        val output = verifier.loadLines(verifier.logFileName, Charset.defaultCharset().name()).joinToString("\n")
        assertTrue(bundleUploadedSuccessfully(baseDir, output))

        verifier.resetStreams()
    }

    @Test
    fun `uploads source bundle with multiple source roots and extra source dirs`() {
        val roots = listOf("src/main/java", "src/main/kotlin")
        val extraDirs = listOf("src/main/extra1", "src/main/extra2")
        val baseDir = setupProject(subdirectories = roots + extraDirs)
        val path = getPOM(baseDir, extraSourceRoots = roots, extraSourceContextDirs = extraDirs)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")

        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("Collected sources from 4 source directories")
        val output = verifier.loadLines(verifier.logFileName, Charset.defaultCharset().name()).joinToString("\n")
        assertTrue(bundleUploadedSuccessfully(baseDir, output))

        verifier.resetStreams()
    }

    private fun setupEmptyProject(): File = setupProject(subdirectories = emptyList())

    private fun setupProject(
        baseDir: String = "base",
        subdirectories: List<String> = listOf("src/main/java"),
    ): File {
        val baseDir = File(file, baseDir)
        baseDir.mkdirs()

        val createdDirs: MutableList<Boolean> = mutableListOf()
        val createdFiles: MutableList<Boolean> = mutableListOf()
        var i = 0; // need to create different files otherwise they will override each other in bundle as they have the same relative path
        for (subdirectory in subdirectories) {
            val dir = File(baseDir.resolve(subdirectory).absolutePath)
            createdDirs.add(dir.mkdirs())
            val file = File(dir, "Main${i++}.java")
            createdFiles.add(file.createNewFile())
        }

        assertTrue(createdDirs.all { it }, "Error creating dirs")
        assertTrue(createdFiles.all { it }, "Error creating files")

        installMavenWrapper(baseDir, "3.8.6")

        return baseDir
    }

    private fun bundleUploadedSuccessfully(
        baseDir: File,
        cliOutput: String,
    ): Boolean {
        val bundleId = getBundleIdFromProperties(baseDir.absolutePath)
        val uploadedId = getUploadedBundleIdFromLog(cliOutput)
        assertEquals(bundleId, uploadedId, "Bundle ID from properties file should match the one from the log")
        return bundleId.equals(uploadedId)
    }

    private fun getUploadedBundleIdFromLog(output: String): String? {
        val uploadedIdRegex = """\w+":\{"state":"ok","missingChunks":\[],"uploaded_id":"(\w+-\w+-\w+-\w+-\w+)""".toRegex()
        return uploadedIdRegex.find(output)?.groupValues?.get(1)
    }

    private fun getBundleIdFromProperties(baseDir: String): String {
        val myProps = Properties()
        myProps.load(FileInputStream("$baseDir/target/sentry/properties/sentry-debug-meta.properties"))
        return myProps.getProperty("io.sentry.bundle-ids")
    }

    private fun getPropertiesFileContent(baseDir: String): String =
        File("$baseDir/target/sentry/properties/sentry-debug-meta.properties").readText()

    @Test
    fun `bundle ID changes when source content changes`() {
        val projectDir = File(file, "content-change-test")
        projectDir.mkdirs()

        val sourceDir = File(projectDir, "src/main/java")
        sourceDir.mkdirs()

        installMavenWrapper(projectDir, "3.8.6")
        getPOM(projectDir, reproducibleBundleId = true)

        // First build with initial content
        File(sourceDir, "Main.java").writeText("public class Main { int version = 1; }")

        val verifier1 = Verifier(projectDir.absolutePath)
        verifier1.isAutoclean = false
        verifier1.executeGoal("install")
        verifier1.verifyErrorFreeLog()

        val bundleId1 = getBundleIdFromProperties(projectDir.absolutePath)
        verifier1.resetStreams()

        // Clean and rebuild with different content
        File(projectDir, "target").deleteRecursively()
        File(sourceDir, "Main.java").writeText("public class Main { int version = 2; }")

        val verifier2 = Verifier(projectDir.absolutePath)
        verifier2.isAutoclean = false
        verifier2.executeGoal("install")
        verifier2.verifyErrorFreeLog()

        val bundleId2 = getBundleIdFromProperties(projectDir.absolutePath)
        verifier2.resetStreams()

        // Verify bundle ID changed
        assertTrue(
            bundleId1 != bundleId2,
            "Bundle ID should change when source content changes",
        )
    }

    @Test
    fun `properties file does not contain timestamp`() {
        val baseDir = setupProject()
        val path = getPOM(baseDir)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyErrorFreeLog()

        val propertiesContent = getPropertiesFileContent(baseDir.absolutePath)

        // Properties.store() adds a timestamp like "#Wed Jan 08 10:30:00 CET 2025"
        // Verify this pattern is not present
        val timestampPattern = Regex("#\\w{3} \\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2}")
        assertTrue(
            !timestampPattern.containsMatchIn(propertiesContent),
            "Properties file should not contain a timestamp comment. Content: $propertiesContent",
        )

        verifier.resetStreams()
    }

    @Test
    fun `build is reproducible with reproducibleBundleId enabled`() {
        val projectDir = File(file, "reproducible-artifact-compare-test")
        projectDir.mkdirs()

        val sourceDir = File(projectDir, "src/main/java/com/example")
        sourceDir.mkdirs()

        // Create source files
        File(sourceDir, "Main.java").writeText(
            """
            package com.example;
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Reproducible build test");
                }
            }
            """.trimIndent(),
        )

        installMavenWrapper(projectDir, "3.9.11")

        // Create POM with outputTimestamp for reproducible builds
        val pomContent =
            basePom(reproducibleBundleId = true)
                .replace(
                    "<properties>",
                    """<properties>
                <project.build.outputTimestamp>2025-01-01T00:00:00Z</project.build.outputTimestamp>""",
                )
        Files.write(Path("${projectDir.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        // First build: clean install
        val verifier1 = Verifier(projectDir.absolutePath)
        verifier1.isAutoclean = false
        verifier1.executeGoal("clean")
        verifier1.executeGoal("install")
        verifier1.verifyErrorFreeLog()
        verifier1.resetStreams()

        // Second build: clean verify artifact:compare
        val verifier2 = Verifier(projectDir.absolutePath)
        verifier2.isAutoclean = false
        verifier2.executeGoal("clean")
        verifier2.executeGoal("verify")
        verifier2.addCliOption("-Dartifact.buildCompare.saveAll=true")
        verifier2.executeGoal("artifact:compare")
        verifier2.verifyErrorFreeLog()

        verifier2.resetStreams()
    }

    @Test
    fun `build is not reproducible with reproducibleBundleId disabled`() {
        val projectDir = File(file, "non-reproducible-artifact-compare-test")
        projectDir.mkdirs()

        val sourceDir = File(projectDir, "src/main/java/com/example")
        sourceDir.mkdirs()

        File(sourceDir, "Main.java").writeText(
            """
            package com.example;
            public class Main {
                public static void main(String[] args) {
                    System.out.println("Non-reproducible bundle ID test");
                }
            }
            """.trimIndent(),
        )

        installMavenWrapper(projectDir, "3.9.11")

        // Create POM with reproducibleBundleId=false but still with outputTimestamp
        // This means the JAR will be reproducible, but the bundle ID will change
        val pomContent =
            basePom(reproducibleBundleId = false)
                .replace(
                    "<properties>",
                    """<properties>
                <project.build.outputTimestamp>2025-01-01T00:00:00Z</project.build.outputTimestamp>""",
                )
        Files.write(Path("${projectDir.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        // First build
        val verifier1 = Verifier(projectDir.absolutePath)
        verifier1.isAutoclean = false
        verifier1.executeGoal("clean")
        verifier1.executeGoal("install")
        verifier1.verifyErrorFreeLog()
        verifier1.resetStreams()

        // Second build - should fail artifact:compare because bundle ID changes
        val verifier2 = Verifier(projectDir.absolutePath)
        verifier2.isAutoclean = false
        verifier2.executeGoal("clean")

        assertFailsWith<VerificationException> {
            verifier2.executeGoals(listOf("verify", "artifact:compare"))
        }

        verifier2.verifyTextInLog("[ERROR] [Reproducible Builds] rebuild comparison result: 1 files match, 1 differ")

        verifier2.resetStreams()
    }
}
