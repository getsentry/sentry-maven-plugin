package io.sentry.integration.cli

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

class SentryCliWhitespacesTestIT {
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
    fun sentryCliExecutionInProjectPathWithSpaces() {
        val baseDir = setupProject()
        val path = getPOM(baseDir)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyErrorFreeLog()

        val output = verifier.loadLines(verifier.logFileName, Charset.defaultCharset().name()).joinToString("\n")

        val uploadedId = getUploadedBundleIdFromLog(output)
        val bundleId = getBundleIdFromProperties(baseDir.absolutePath)

        assertEquals(bundleId, uploadedId, "Bundle ID from properties file should match the one from the log")

        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun sentryCliExecutionInProjectAndCliPathWithSpaces() {
        val cliPath = SentryCliProvider.getCliPath(MavenProject(), null)
        val baseDir = setupProject()
        val cliPathWithSpaces = Files.copy(Path(cliPath), Path(baseDir.absolutePath, "sentry-cli"))
        cliPathWithSpaces.toFile().setExecutable(true)

        val path = getPOM(baseDir, sentryCliPath = cliPathWithSpaces.absolutePathString())

        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyErrorFreeLog()

        val output = verifier.loadLines(verifier.logFileName, Charset.defaultCharset().name()).joinToString("\n")

        val uploadedId = getUploadedBundleIdFromLog(output)
        val bundleId = getBundleIdFromProperties(baseDir.absolutePath)

        assertEquals(bundleId, uploadedId, "Bundle ID from properties file should match the one from the log")

        verifier.resetStreams()
    }

    @Test
    fun buildsSuccessfullyWithNoSourceRootAndLogs() {
        val cliPath = SentryCliProvider.getCliPath(MavenProject(), null)
        val baseDir = setupEmptyProject()

        val path = getPOM(baseDir, sentryCliPath = cliPath)

        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("Not collecting sources in {}: directory does not exist".format(Path(path).resolve("/src/main/java")))
    }

    private fun setupProject(): File {
        val baseDir = File(file, "base with spaces")
        val srcDir = File(baseDir, "/src/main/java")
        val srcFile = File(srcDir, "Main.java")
        val baseDirResult = baseDir.mkdir()
        val srcDirResult = srcDir.mkdirs()
        val srcFileResult = srcFile.createNewFile()

        assertTrue(baseDirResult, "Error creating base directory")
        assertTrue(srcDirResult, "Error creating source directory")
        assertTrue(srcFileResult, "Error creating source file")
        installMavenWrapper(baseDir, "3.8.6")

        return baseDir
    }

    private fun setupEmptyProject(): File {
        val baseDir = File(file, "empty-base-dir")
        val baseDirResult = baseDir.mkdir()

        assertTrue(baseDirResult, "Error creating base directory")
        installMavenWrapper(baseDir, "3.8.6")

        return baseDir
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
