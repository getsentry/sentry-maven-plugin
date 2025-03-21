package io.sentry.integration.reportDependencies

fun basePom(
    skipPlugin: Boolean = false,
    skipReportDependencies: Boolean = false,
): String {
    return """
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>

            <groupId>io.sentry.autoinstall</groupId>
            <artifactId>installsentry</artifactId>
            <version>1.0-SNAPSHOT</version>

            <packaging>jar</packaging>

            <properties>
                <maven.compiler.source>11</maven.compiler.source>
                <maven.compiler.target>11</maven.compiler.target>
            </properties>

            <dependencies>
                <dependency>
                    <groupId>com.graphql-java</groupId>
                    <artifactId>graphql-java</artifactId>
                    <version>2.0.0</version>
                </dependency>
                <dependency>
                    <groupId>io.sentry</groupId>
                    <artifactId>sentry-graphql</artifactId>
                    <version>6.32.0</version>
                </dependency>
            </dependencies>

            <build>
                <plugins>
                    <plugin>
                        <groupId>io.sentry</groupId>
                        <artifactId>sentry-maven-plugin</artifactId>
                        <version>1.0-SNAPSHOT</version>
                        <extensions>true</extensions>
                        <configuration>
                            <skip>$skipPlugin</skip>
                            <skipSourceBundle>true</skipSourceBundle>
                            <skipReportDependencies>$skipReportDependencies</skipReportDependencies>
                        </configuration>
                        <executions>
                            <execution>
                                <goals>
                                    <goal>reportDependencies</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>
        </project>
        """.trimIndent()
}
