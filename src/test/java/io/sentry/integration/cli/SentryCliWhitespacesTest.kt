package io.sentry.integration.cli

import io.sentry.SentryCliProvider
import io.sentry.autoinstall.util.SdkVersionInfo
import io.sentry.integration.installMavenWrapper
import org.apache.maven.it.VerificationException
import org.apache.maven.it.Verifier
import org.apache.maven.project.MavenProject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.test.assertTrue

class SentryCliWhitespacesTest {

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
        verifier.verifyTextInLog("Bundled 1 file for upload")
        verifier.verifyTextInLog("Uploaded 1 missing debug information file");
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun sentryCliExecutionInProjectAndCliPathWithSpaces() {
        val cliPath = SentryCliProvider.getCliPath(MavenProject(), null);
        val baseDir = setupProject()
        val cliPathWithSpaces = Files.copy(Path(cliPath), Path(baseDir.absolutePath, "sentry-cli"))
        val path = getPOM(baseDir, sentryCliPath = cliPathWithSpaces.absolutePathString())
        val verifier = Verifier(path)

        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyErrorFreeLog()
        verifier.verifyTextInLog("Bundled 1 file for upload")
        verifier.verifyTextInLog("Uploaded 1 missing debug information file");
        verifier.resetStreams()
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
}
