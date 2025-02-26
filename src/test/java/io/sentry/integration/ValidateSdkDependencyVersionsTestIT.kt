package io.sentry.integration

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
import kotlin.test.assertTrue

class ValidateSdkDependencyVersionsTestIT {
    @TempDir
    lateinit var file: File

    @BeforeEach
    fun installWrapper() {
        installMavenWrapper(file, "3.6.3")
    }

    private fun basePom(
        dependencies: String = "",
        dependencyManagement: String = "",
        skipValidateSdkDependencyVersions: Boolean = false,
    ): String {
        return """
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>

                <groupId>io.sentry.test</groupId>
                <artifactId>validate-versions</artifactId>
                <version>1.0-SNAPSHOT</version>

                <packaging>jar</packaging>

                <properties>
                    <maven.compiler.source>11</maven.compiler.source>
                    <maven.compiler.target>11</maven.compiler.target>
                </properties>

                <dependencies>
                    $dependencies
                </dependencies>

                <dependencyManagement>
                    $dependencyManagement
                </dependencyManagement>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>io.sentry</groupId>
                            <artifactId>sentry-maven-plugin</artifactId>
                            <version>1.0-SNAPSHOT</version>
                            <extensions>true</extensions>
                            <configuration>
                                <skipTelemetry>true</skipTelemetry>
                                <skipValidateSdkDependencyVersions>$skipValidateSdkDependencyVersions</skipValidateSdkDependencyVersions>
                            </configuration>
                            <executions>
                                <execution>
                                    <goals>
                                        <goal>validateSdkDependencyVersions</goal>
                                    </goals>
                                </execution>
                            </executions>
                        </plugin>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>3.11.0</version>
                            <configuration>
                                <compilerArgs>
                                    <arg>-XepDisableAllChecks</arg>
                                </compilerArgs>
                            </configuration>
                        </plugin>
                    </plugins>
                </build>
            </project>
            """.trimIndent()
    }

    fun getPOM(
        dependencies: String = "",
        dependencyManagement: String = "",
        skipValidateSdkDependencyVersions: Boolean = false,
    ): String {
        val pomContent =
            basePom(
                dependencies = dependencies,
                dependencyManagement = dependencyManagement,
                skipValidateSdkDependencyVersions = skipValidateSdkDependencyVersions,
            )

        Files.write(Path("${file.absolutePath}/pom.xml"), pomContent.toByteArray(), StandardOpenOption.CREATE)

        return file.absolutePath
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when no Sentry dependencies exist validation succeeds`() {
        val path = getPOM()
        val verifier = Verifier(path)
        verifier.executeGoal("validate")
        verifier.verifyErrorFreeLog()
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when single Sentry dependency on SDK exists validation succeeds`() {
        val dependencies =
            """
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry</artifactId>
                <version>8.1.0</version>
            </dependency>
            """.trimIndent()

        val path = getPOM(dependencies = dependencies)
        val verifier = Verifier(path)
        verifier.executeGoal("validate")
        verifier.verifyErrorFreeLog()
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when single Sentry dependency on integration exists validation succeeds`() {
        val dependencies =
            """
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry-spring-boot</artifactId>
                <version>8.1.0</version>
            </dependency>
            """.trimIndent()

        val path = getPOM(dependencies = dependencies)
        val verifier = Verifier(path)
        verifier.executeGoal("validate")
        verifier.verifyErrorFreeLog()
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when multiple Sentry dependencies with same version exist validation succeeds`() {
        val dependencies =
            """
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry-spring-boot</artifactId>
                <version>8.1.0</version>
            </dependency>
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry-logback</artifactId>
                <version>8.1.0</version>
            </dependency>
            """.trimIndent()

        val path = getPOM(dependencies = dependencies)
        val verifier = Verifier(path)
        verifier.executeGoal("validate")
        verifier.verifyErrorFreeLog()
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when dependency on Sentry SDK and integration with different versions exist validation fails`() {
        val dependencies =
            """
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry</artifactId>
                <version>8.2.0</version>
            </dependency>
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry-spring-boot</artifactId>
                <version>8.1.0</version>
            </dependency>
            """.trimIndent()

        val path = getPOM(dependencies = dependencies)
        val verifier = Verifier(path)
        var didThrowVerificationException = false
        try {
            verifier.executeGoal("validate")
        } catch (e: VerificationException) {
            didThrowVerificationException = true
        }
        assertTrue(didThrowVerificationException, "Expected validate goal to fail with VerificationException")
        verifier.verifyTextInLog("Found inconsistency in Sentry SDK dependency versions.")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when multiple Sentry integration dependencies with different versions exist validation fails`() {
        val dependencies =
            """
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry-logback</artifactId>
                <version>8.2.0</version>
            </dependency>
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry-spring-boot</artifactId>
                <version>8.1.0</version>
            </dependency>
            """.trimIndent()

        val path = getPOM(dependencies = dependencies)
        val verifier = Verifier(path)
        var didThrowVerificationException = false
        try {
            verifier.executeGoal("validate")
        } catch (e: VerificationException) {
            didThrowVerificationException = true
        }
        assertTrue(didThrowVerificationException, "Expected validate goal to fail with VerificationException")
        verifier.verifyTextInLog("Found inconsistency in Sentry SDK dependency versions.")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when only bom exists validation succeeds`() {
        val bom =
            """
            <dependencies>
                <dependency>
                    <groupId>io.sentry</groupId>
                    <artifactId>sentry-bom</artifactId>
                    <version>8.2.0</version>
                    <type>pom</type>
                    <scope>import</scope>
                </dependency>
            </dependencies>
            """.trimIndent()

        val path = getPOM(dependencyManagement = bom)
        val verifier = Verifier(path)
        verifier.executeGoal("validate")
        verifier.verifyErrorFreeLog()
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when bom and explicit Sentry dependency with no version exist validation succeeds`() {
        val dependencies =
            """
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry-spring-boot</artifactId>
            </dependency>
            """.trimIndent()
        val bom =
            """
            <dependencies>
                <dependency>
                    <groupId>io.sentry</groupId>
                    <artifactId>sentry-bom</artifactId>
                    <version>8.2.0</version>
                    <type>pom</type>
                    <scope>import</scope>
                </dependency>
            </dependencies>
            """.trimIndent()

        val path = getPOM(dependencies = dependencies, dependencyManagement = bom)
        val verifier = Verifier(path)
        verifier.executeGoal("validate")
        verifier.verifyErrorFreeLog()
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when bom and explicit Sentry dependency with same version exist validation succeeds`() {
        val dependencies =
            """
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry-spring-boot</artifactId>
                <version>8.2.0</version>
            </dependency>
            """.trimIndent()
        val bom =
            """
            <dependencies>
                <dependency>
                    <groupId>io.sentry</groupId>
                    <artifactId>sentry-bom</artifactId>
                    <version>8.2.0</version>
                    <type>pom</type>
                    <scope>import</scope>
                </dependency>
            </dependencies>
            """.trimIndent()

        val path = getPOM(dependencies = dependencies, dependencyManagement = bom)
        val verifier = Verifier(path)
        verifier.executeGoal("validate")
        verifier.verifyErrorFreeLog()
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when bom and explicit Sentry dependency with different version exist validation fails`() {
        val dependencies =
            """
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry-spring-boot</artifactId>
                <version>8.1.0</version>
            </dependency>
            """.trimIndent()
        val bom =
            """
            <dependencies>
                <dependency>
                    <groupId>io.sentry</groupId>
                    <artifactId>sentry-bom</artifactId>
                    <version>8.2.0</version>
                    <type>pom</type>
                    <scope>import</scope>
                </dependency>
            </dependencies>
            """.trimIndent()

        val path = getPOM(dependencies = dependencies, dependencyManagement = bom)
        val verifier = Verifier(path)
        var didThrowVerificationException = false
        try {
            verifier.executeGoal("validate")
        } catch (e: VerificationException) {
            didThrowVerificationException = true
        }
        assertTrue(didThrowVerificationException, "Expected validate goal to fail with VerificationException")
        verifier.verifyTextInLog("Found inconsistency in Sentry SDK dependency versions.")
        verifier.resetStreams()
    }

    @Test
    @Throws(VerificationException::class, IOException::class)
    fun `when Sentry dependency versions validation is skipped validation succeeds despite version mismatch`() {
        val dependencies =
            """
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry</artifactId>
                <version>8.2.0</version>
            </dependency>
            <dependency>
                <groupId>io.sentry</groupId>
                <artifactId>sentry-spring-boot</artifactId>
                <version>8.1.0</version>
            </dependency>
            """.trimIndent()

        val path = getPOM(dependencies = dependencies, skipValidateSdkDependencyVersions = true)
        val verifier = Verifier(path)
        verifier.executeGoal("validate")
        verifier.verifyErrorFreeLog()
        verifier.resetStreams()
    }
}
