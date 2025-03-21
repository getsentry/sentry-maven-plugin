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
import kotlin.test.assertTrue

class UploadSourceBundleTestIT {
    @TempDir()
    lateinit var file: File

    fun getPOM(
        baseDir: File,
        skipPlugin: Boolean = false,
        skipSourceBundle: Boolean = false,
        sentryCliPath: String? = null,
    ): String {
        val pomContent = basePom(skipPlugin, skipSourceBundle, sentryCliPath)

        Files.write(Path("${baseDir.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        return baseDir.absolutePath
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
        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("Skipping module, as it doesn't have any source roots")
    }

    private fun setupEmptyProject(): File {
        return setupProject(emptyList())
    }

    private fun setupProject(
        sourceRoots: List<String> = listOf("src/main/java"),
        baseDir: String = "base",
    ): File {
        val baseDir = File(file, baseDir)
        baseDir.mkdirs()

        val createdSourceRoots: MutableList<Boolean> = mutableListOf()
        val createdSourceFiles: MutableList<Boolean> = mutableListOf()
        var i = 0; // need to create different files otherwise they will override each other in bundle as they have the same relative path
        for (sourceRoot in sourceRoots) {
            val dir = File(baseDir.resolve(sourceRoot).absolutePath)
            createdSourceRoots.add(dir.mkdirs())
            val file = File(dir, "Main${i++}.java")
            createdSourceFiles.add(file.createNewFile())
        }

        assertTrue(createdSourceRoots.all { it }, "Error creating source roots")
        assertTrue(createdSourceFiles.all { it }, "Error creating source files")

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
        val uploadedIdRegex = """UPLOADED (\w+-\w+-\w+-\w+-\w+)""".toRegex()
        return uploadedIdRegex.find(output)?.groupValues?.get(1)
    }

    private fun getBundleIdFromProperties(baseDir: String): String {
        val myProps = Properties()
        myProps.load(FileInputStream("$baseDir/target/sentry/properties/sentry-debug-meta.properties"))
        return myProps.getProperty("io.sentry.bundle-ids")
    }
}
