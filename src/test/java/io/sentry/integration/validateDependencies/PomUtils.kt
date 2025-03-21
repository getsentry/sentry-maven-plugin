package io.sentry.integration.validateDependencies

fun basePom(
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
