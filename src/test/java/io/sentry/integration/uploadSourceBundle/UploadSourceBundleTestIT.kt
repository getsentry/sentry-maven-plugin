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
}
