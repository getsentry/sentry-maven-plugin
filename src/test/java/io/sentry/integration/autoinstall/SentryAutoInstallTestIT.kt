package io.sentry.autoinstall

import basePom
import installMavenWrapper
import org.apache.maven.shared.verifier.VerificationException
import org.apache.maven.shared.verifier.Verifier
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

    fun getPOM(installedSentryVersion: String? = null): String {
        val pomContent = basePom("", installedSentryVersion)

        Files.write(Path("${file.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        return file.absolutePath
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun verifySentryInstalled() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyFilePresent("target/lib/sentry-${SentryInstaller.SENTRY_VERSION}.jar")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun verifySentryNotInstalledIfAlreadyInDependencies() {
        val alreadyInstalledSentryVersion = "6.25.2"
        val path = getPOM(alreadyInstalledSentryVersion)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyFilePresent("target/lib/sentry-$alreadyInstalledSentryVersion.jar")
        verifier.verifyTextInLog("Sentry already installed $alreadyInstalledSentryVersion")
        verifier.resetStreams()
    }
}
