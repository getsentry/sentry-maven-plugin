package io.sentry.autoinstall

import basePom
import createExtensionInFolder
import org.apache.maven.shared.verifier.VerificationException
import org.apache.maven.shared.verifier.Verifier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class SentryAutoInstallTest {

    @TempDir()
    lateinit var file: File

    fun getPOM(
        installLog4j2: Boolean = true,
        log4j2Version: String = "2.17.0",
        withExtension: Boolean = true,
    ): String {
        var dependencies =
            """
                    <dependency>
                        <groupId>org.apache.logging.log4j</groupId>
                        <artifactId>log4j-api</artifactId>
                        <version>${log4j2Version}</version>
                    </dependency>
                """.trimIndent()

        if (!installLog4j2) {
            dependencies = dependencies.plus(
                """
                    <dependency>
                        <groupId>io.sentry</groupId>
                        <artifactId>sentry-log4j2</artifactId>
                        <version>6.25.2</version>
                    </dependency>
                """.trimIndent()
            )
        }

        val pomContent = basePom(dependencies)


        Files.write(Path(file.absolutePath + "/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        if (withExtension) {
            createExtensionInFolder(file)
        }

        return file.absolutePath
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun verifySentryInstalled() {
        val path = getPOM(true)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyFilePresent("target/lib/sentry-${SentryInstaller.SENTRY_VERSION}.jar")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun verifySentryInstalledAndLog4jInstalled() {
        val path = getPOM(true)
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyFilePresent("target/lib/sentry-log4j2-${SentryInstaller.SENTRY_VERSION}.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }
}
