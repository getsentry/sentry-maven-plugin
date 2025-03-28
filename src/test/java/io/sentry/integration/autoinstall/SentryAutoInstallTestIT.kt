package io.sentry.integration.autoinstall

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

class SentryAutoInstallTestIT {
    @TempDir()
    lateinit var file: File

    @BeforeEach
    fun installWrapper() {
        installMavenWrapper(file, "3.6.3")
    }

    fun getPOM(
        installedSentryVersion: String? = null,
        pluginConfiguration: String = "",
    ): String {
        val pomContent = basePom("", installedSentryVersion, pluginConfiguration)

        Files.write(Path("${file.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        return file.absolutePath
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `installs sentry`() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyFilePresent("target/lib/sentry-${SdkVersionInfo.getSentryVersion()}.jar")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `does not install sentry if already present in dependencies`() {
        val alreadyInstalledSentryVersion = "6.25.2"
        val path = getPOM(alreadyInstalledSentryVersion)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyFilePresent("target/lib/sentry-$alreadyInstalledSentryVersion.jar")
        verifier.verifyTextInLog("Sentry already installed $alreadyInstalledSentryVersion")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `does nothing if skip is true, even if skipAutoInstall is false`() {
        val pluginConfiguration =
            """
            <configuration>
                <skip>true</skip>
                <skipAutoInstall>false</skipAutoInstall>
            </configuration>
            """.trimIndent()

        val path = getPOM(pluginConfiguration = pluginConfiguration)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyFileNotPresent("target/lib/sentry-${SdkVersionInfo.getSentryVersion()}.jar")
        verifier.verifyTextInLog("Auto Install disabled for project ")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `does nothing if skipAutoInstall is true`() {
        val pluginConfiguration =
            """
            <configuration>
                <skipAutoInstall>true</skipAutoInstall>
            </configuration>
            """.trimIndent()

        val path = getPOM(pluginConfiguration = pluginConfiguration)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyFileNotPresent("target/lib/sentry-${SdkVersionInfo.getSentryVersion()}.jar")
        verifier.verifyTextInLog("Auto Install disabled for project ")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `installs sentry if skipAutoInstall is false`() {
        val pluginConfiguration =
            """
            <configuration>
                <skipAutoInstall>false</skipAutoInstall>
            </configuration>
            """.trimIndent()

        val path = getPOM(pluginConfiguration = pluginConfiguration)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyFilePresent("target/lib/sentry-${SdkVersionInfo.getSentryVersion()}.jar")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `does nothing and logs if sentry version cannot be resolved`() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
        verifier.verifyFilePresent("target/lib/sentry-${SdkVersionInfo.getSentryVersion()}.jar")
        verifier.resetStreams()
    }
}
