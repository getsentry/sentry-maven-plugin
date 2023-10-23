package io.sentry.integration.autoinstall.jdbc

import basePom
import installMavenWrapper
import io.sentry.autoinstall.util.SdkVersionInfo
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

class JdbcAutoInstallTestIT {
    @TempDir()
    lateinit var file: File

    @BeforeEach
    fun installWrapper() {
        installMavenWrapper(file, "3.8.6")
    }

    fun getPOM(
        installJdbc: Boolean = true,
        jdbcVersion: String = "3.1.3",
        sentryJdbcVersion: String = "6.25.2",
        installedSentryVersion: String? = null,
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
            dependencies =
                dependencies.plus(
                    """
                    <dependency>
                        <groupId>io.sentry</groupId>
                        <artifactId>sentry-jdbc</artifactId>
                        <version>$sentryJdbcVersion</version>
                    </dependency>
                    """.trimIndent(),
                )
        }

        val pomContent = basePom(dependencies, installedSentryVersion)

        Files.write(Path("${file.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        return file.absolutePath
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when sentry-jdbc is a direct dependency logs a message and does nothing`() {
        val path = getPOM(false)
        val verifier = Verifier(path)
        verifier.isAutoclean = false
        verifier.executeGoal("install")
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
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-jdbc was successfully installed with version: ${SdkVersionInfo.getSentryVersion()}")
        verifier.verifyFilePresent("target/lib/sentry-jdbc-${SdkVersionInfo.getSentryVersion()}.jar")
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
        verifier.executeGoal("install")
        verifier.verifyTextInLog("sentry-jdbc was successfully installed with version: $sentryVersion")
        verifier.verifyFilePresent("target/lib/sentry-jdbc-$sentryVersion.jar")
        verifier.resetStreams()
        verifier.deleteDirectory(path)
    }
}
