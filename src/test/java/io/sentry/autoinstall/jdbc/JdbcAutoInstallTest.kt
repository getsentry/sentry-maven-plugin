package io.sentry.autoinstall.jdbc

import basePom
import createExtensionInFolder
import io.sentry.autoinstall.SentryInstaller
import org.apache.maven.shared.verifier.VerificationException
import org.apache.maven.shared.verifier.Verifier
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class JdbcAutoInstallTest {

    @TempDir()
    lateinit var file: File

    fun getPOM(
        installJdbc: Boolean = true,
        jdbcVersion: String = "3.1.3",
        withExtension: Boolean = true,
        sentryJdbcVersion: String = "6.25.2",
        installedSentryVersion: String? = null
    ): String {
        var dependencies =
            """
                    <dependency>
                        <groupId>org.mariadb.jdbc</groupId>
                        <artifactId>mariadb-java-client</artifactId>
                        <version>$jdbcVersion</version>
                    </dependency>
                """.trimIndent()

        if (!installJdbc) {
            dependencies = dependencies.plus(
                """
                    <dependency>
                        <groupId>io.sentry</groupId>
                        <artifactId>sentry-jdbc</artifactId>
                        <version>$sentryJdbcVersion</version>
                    </dependency>
                """.trimIndent()
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
    fun `when sentry-jdbc is a direct dependency logs a message and does nothing`() {
        val path = getPOM(false)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("won't be installed because it was already installed directly")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `installs sentry-jdbc with info message`() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("sentry-jdbc was successfully installed with version: ${SentryInstaller.SENTRY_VERSION}")
        verifier.verifyFilePresent("target/lib/sentry-jdbc-${SentryInstaller.SENTRY_VERSION}.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `auto-installed sentry-jdbc version matches sentry version`() {
        val sentryVersion = "6.25.2"
        val path = getPOM(installedSentryVersion = sentryVersion)
        val verifier = Verifier(path)
        verifier.deleteDirectory("target")
        verifier.isAutoclean = false
        verifier.addCliArgument("install")
        verifier.execute()
        verifier.verifyTextInLog("sentry-jdbc was successfully installed with version: ${sentryVersion}")
        verifier.verifyFilePresent("target/lib/sentry-jdbc-${sentryVersion}.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }
}
