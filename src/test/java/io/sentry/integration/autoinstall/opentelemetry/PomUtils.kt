package io.sentry.integration.autoinstall.opentelemetry

import java.io.File

/**
 * Writes a fake `io.sentry:sentry-opentelemetry-bom` into a local file repository under [repoDir].
 * The BOM imports the real `io.opentelemetry:opentelemetry-bom:[otelVersion]` (resolved from Maven
 * Central), so the version alignment exercised by the tests is genuine.
 */
fun writeFakeOpenTelemetryBom(
    repoDir: File,
    sentryVersion: String,
    otelVersion: String,
) {
    val pom =
        File(
            repoDir,
            "io/sentry/sentry-opentelemetry-bom/$sentryVersion/sentry-opentelemetry-bom-$sentryVersion.pom",
        )
    pom.parentFile.mkdirs()
    pom.writeText(
        """
        <project xmlns="http://maven.apache.org/POM/4.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
          <modelVersion>4.0.0</modelVersion>
          <groupId>io.sentry</groupId>
          <artifactId>sentry-opentelemetry-bom</artifactId>
          <version>$sentryVersion</version>
          <packaging>pom</packaging>
          <dependencyManagement>
            <dependencies>
              <dependency>
                <groupId>io.opentelemetry</groupId>
                <artifactId>opentelemetry-bom</artifactId>
                <version>$otelVersion</version>
                <type>pom</type>
                <scope>import</scope>
              </dependency>
            </dependencies>
          </dependencyManagement>
        </project>
        """.trimIndent(),
    )
}

/**
 * Builds a POM for the OpenTelemetry BOM auto-install integration tests.
 *
 * @param repoUrl file URL of the repository that serves the fake sentry-opentelemetry-bom
 * @param useSpringBootParent inherit OpenTelemetry version management from spring-boot-starter-parent
 * @param otelDependency the Sentry OpenTelemetry dependency to declare (agentless by default)
 * @param userPinnedOtelVersion when set, pins io.opentelemetry:opentelemetry-sdk in the project's
 *   own dependencyManagement
 * @param importBom when true, the project imports the sentry-opentelemetry-bom itself
 * @param pluginConfiguration optional `<configuration>` block for the Sentry plugin
 */
fun openTelemetryPom(
    repoUrl: String,
    sentryVersion: String,
    useSpringBootParent: Boolean = false,
    otelDependency: String = "sentry-opentelemetry-agentless",
    userPinnedOtelVersion: String? = null,
    importBom: Boolean = false,
    pluginConfiguration: String = "",
): String {
    val parent =
        if (useSpringBootParent) {
            """
            <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.4.1</version>
                <relativePath/>
            </parent>
            """.trimIndent()
        } else {
            ""
        }

    val managedDeps =
        buildString {
            if (userPinnedOtelVersion != null) {
                append(
                    """
                    <dependency>
                        <groupId>io.opentelemetry</groupId>
                        <artifactId>opentelemetry-sdk</artifactId>
                        <version>$userPinnedOtelVersion</version>
                    </dependency>
                    """.trimIndent(),
                )
            }
            if (importBom) {
                append(
                    """
                    <dependency>
                        <groupId>io.sentry</groupId>
                        <artifactId>sentry-opentelemetry-bom</artifactId>
                        <version>$sentryVersion</version>
                        <type>pom</type>
                        <scope>import</scope>
                    </dependency>
                    """.trimIndent(),
                )
            }
        }

    val dependencyManagement =
        if (managedDeps.isNotEmpty()) {
            "<dependencyManagement><dependencies>$managedDeps</dependencies></dependencyManagement>"
        } else {
            ""
        }

    return """
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
            $parent
            <groupId>io.sentry.autoinstall</groupId>
            <artifactId>installotelbom</artifactId>
            <version>1.0-SNAPSHOT</version>
            <packaging>jar</packaging>

            <properties>
                <maven.compiler.source>17</maven.compiler.source>
                <maven.compiler.target>17</maven.compiler.target>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            </properties>

            <repositories>
                <repository>
                    <id>fake-sentry-bom-repo</id>
                    <url>$repoUrl</url>
                </repository>
            </repositories>

            $dependencyManagement

            <dependencies>
                <dependency>
                    <groupId>io.sentry</groupId>
                    <artifactId>$otelDependency</artifactId>
                    <version>$sentryVersion</version>
                </dependency>
            </dependencies>

            <build>
                <plugins>
                    <plugin>
                        <groupId>io.sentry</groupId>
                        <artifactId>sentry-maven-plugin</artifactId>
                        <version>1.0-SNAPSHOT</version>
                        <extensions>true</extensions>
                        $pluginConfiguration
                    </plugin>
                    <plugin>
                        <artifactId>maven-dependency-plugin</artifactId>
                        <executions>
                            <execution>
                                <phase>install</phase>
                                <goals>
                                    <goal>copy-dependencies</goal>
                                </goals>
                                <configuration>
                                    <outputDirectory>target/lib</outputDirectory>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </project>
        """.trimIndent()
}
