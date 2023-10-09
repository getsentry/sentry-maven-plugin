package io.sentry.autoinstall.log4j2

import basePom
import createExtensionInFolder
import io.sentry.SdkVersionInfo
import org.apache.maven.shared.verifier.VerificationException
import org.apache.maven.shared.verifier.Verifier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class Log4jAutoInstallTestIT {
    @TempDir()
    lateinit var file: File

    fun getPOM(
        installLog4j2: Boolean = true,
        log4j2Version: String = "2.17.0",
        withExtension: Boolean = true,
        sentryLog4j2Version: String = "6.25.2",
        installedSentryVersion: String? = null,
    ): String {
        var dependencies =
            """
            <dependency>
                <groupId>org.apache.logging.log4j</groupId>
                <artifactId>log4j-api</artifactId>
                <version>$log4j2Version</version>
            </dependency>
            """.trimIndent()

        if (!installLog4j2) {
            dependencies =
                dependencies.plus(
                    """
                    <dependency>
                        <groupId>io.sentry</groupId>
                        <artifactId>sentry-log4j2</artifactId>
                        <version>$sentryLog4j2Version</version>
                    </dependency>
                    """.trimIndent(),
                )
        }

        val pomContent = basePom(dependencies, installedSentryVersion)

        Files.write(Path("${file.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        if (withExtension) {
            createExtensionInFolder(file)
        }

        return file.absolutePath
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when sentry-log4j2 is a direct dependency logs a message and does nothing`() {
        val path = getPOM(false)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("sentry-log4j2 won't be installed because it was already installed directly")
        verifier.resetStreams()
    }

//    @Test
//    @Throws(VerificationException::class, IOException::class)
//    fun `when log4j2 version is unsupported logs a message and does nothing`() {
//        val path = getPOM(log4j2Version = "1.2.17")
//        val verifier = Verifier(path)
//        verifier.isAutoclean = false
//        verifier.addCliArgument("install")
//        verifier.execute()
//        verifier.verifyTextInLog("[sentry] sentry-log4j2 won't be installed because the current " +
//            "version is lower than the minimum supported version (2.0.0)")
//        verifier.verifyFileNotPresent("target/lib/sentry-${SdkVersionInfo.sentryVersion}.jar")
//        verifier.resetStreams()
//    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `installs sentry-log4j2 with info message`() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("sentry-log4j2 was successfully installed with version: ${SdkVersionInfo.sentryVersion}")
        verifier.verifyFilePresent("target/lib/sentry-log4j2-${SdkVersionInfo.sentryVersion}.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `auto-installed sentry-log4j2 version matches sentry version`() {
        val sentryVersion = "6.25.2"
        val path = getPOM(installedSentryVersion = sentryVersion)
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("sentry-log4j2 was successfully installed with version: $sentryVersion")
        verifier.verifyFilePresent("target/lib/sentry-log4j2-$sentryVersion.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }
}
