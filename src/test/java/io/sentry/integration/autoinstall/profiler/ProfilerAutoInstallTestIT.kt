package io.sentry.integration.autoinstall.profiler

import basePom
import io.sentry.autoinstall.util.SdkVersionInfo
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

class ProfilerAutoInstallTestIT {
    @TempDir()
    lateinit var file: File

    @BeforeEach
    fun installWrapper() {
        installMavenWrapper(file, "3.8.6")
    }

    fun getPOM(
        enableInstallProfilerFlag: Boolean = true,
        profilerAlreadyInstalled: Boolean = false,
        sentryProfilerVersion: String = SdkVersionInfo.getSentryVersion() ?: "8.23.0",
        installedSentryVersion: String? = null,
    ): String {
        var dependencies = ""
        var pluginConfiguration = ""

        if (profilerAlreadyInstalled) {
            dependencies =
                """
                <dependency>
                    <groupId>io.sentry</groupId>
                    <artifactId>sentry-async-profiler</artifactId>
                    <version>$sentryProfilerVersion</version>
                </dependency>
                """.trimIndent()
        }

        if (enableInstallProfilerFlag) {
            pluginConfiguration =
                """
                <configuration>
                    <installProfiler>true</installProfiler>
                </configuration>
                """.trimIndent()
        }

        val pomContent = basePom(dependencies, installedSentryVersion, pluginConfiguration)

        Files.write(Path("${file.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        return file.absolutePath
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when installProfiler flag is false does not install`() {
        val path = getPOM(enableInstallProfilerFlag = false)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyFileNotPresent("target/lib/sentry-async-profiler-${SdkVersionInfo.getSentryVersion()}.jar")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when sentry-async-profiler is a direct dependency does not auto-install`() {
        val path = getPOM(enableInstallProfilerFlag = true, profilerAlreadyInstalled = true)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-async-profiler won't be installed because it was already installed directly")
        verifier.verifyFilePresent("target/lib/sentry-async-profiler-${SdkVersionInfo.getSentryVersion()}.jar")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `installs sentry-async-profiler with info message`() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-async-profiler was successfully installed with version: ${SdkVersionInfo.getSentryVersion()}")
        verifier.verifyFilePresent("target/lib/sentry-async-profiler-${SdkVersionInfo.getSentryVersion()}.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `auto-installed sentry-async-profiler version matches sentry version`() {
        val sentryVersion = "8.23.0"
        val path = getPOM(installedSentryVersion = sentryVersion)
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-async-profiler was successfully installed with version: $sentryVersion")
        verifier.verifyFilePresent("target/lib/sentry-async-profiler-$sentryVersion.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `does not install when sentry version is too low`() {
        val sentryVersion = "8.22.0"
        val path = getPOM(installedSentryVersion = sentryVersion)
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-async-profiler won't be installed because the current version")
        verifier.verifyFileNotPresent("target/lib/sentry-async-profiler-$sentryVersion.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }
}
