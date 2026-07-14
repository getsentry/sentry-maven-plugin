package io.sentry.integration.autoinstall.opentelemetry

/**
 * Builds a POM for the OpenTelemetry version-check integration tests.
 *
 * @param sentryVersion version used for the Sentry OpenTelemetry dependency
 * @param useSpringBootParent inherit OpenTelemetry version management (which downgrades OTel) from
 *   spring-boot-starter-parent
 * @param otelDependency the Sentry dependency to declare (agentless by default; use `sentry` to
 *   exercise the no-OpenTelemetry case)
 * @param pluginConfiguration optional `<configuration>` block for the Sentry plugin
 */
fun openTelemetryPom(
    sentryVersion: String,
    useSpringBootParent: Boolean = false,
    otelDependency: String = "sentry-opentelemetry-agentless",
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

    return """
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
            $parent
            <groupId>io.sentry.autoinstall</groupId>
            <artifactId>verifyotel</artifactId>
            <version>1.0-SNAPSHOT</version>
            <packaging>jar</packaging>

            <properties>
                <maven.compiler.source>17</maven.compiler.source>
                <maven.compiler.target>17</maven.compiler.target>
                <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            </properties>

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
                </plugins>
            </build>
        </project>
        """.trimIndent()
}
