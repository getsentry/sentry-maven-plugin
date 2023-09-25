import io.sentry.autoinstall.Constants
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

private val extensionString = """
        <extensions xmlns="http://maven.apache.org/EXTENSIONS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/EXTENSIONS/1.0.0 http://maven.apache.org/xsd/core-extensions-1.0.0.xsd">
            <extension>
                <groupId>io.sentry</groupId>
                <artifactId>sentry-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
            </extension>
        </extensions>
            """.trimIndent()

fun createExtensionInFolder(file: File) {
    val mvnDir = File(file, ".mvn").apply {
        mkdirs()
    }

    Files.write(
        Path("${mvnDir.absolutePath}/extensions.xml"),
        extensionString.toByteArray(),
        StandardOpenOption.CREATE
    )
}

fun basePom(dependencies: String?, installedSentryVersion: String?): String {
    val sentryDependency = installedSentryVersion?.let {
        """
            <dependency>
                <groupId>${Constants.SENTRY_GROUP_ID}</groupId>
                <artifactId>${Constants.SENTRY_ARTIFACT_ID}</artifactId>
                <version>${it}</version>
            </dependency>
        """.trimIndent()
    } ?: ""

    return """
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>

            <groupId>io.sentry.autoinstall</groupId>
            <artifactId>installsentry</artifactId>
            <version>1.0.0</version>

            <packaging>jar</packaging>

            <properties>
                <maven.compiler.source>11</maven.compiler.source>
                <maven.compiler.target>11</maven.compiler.target>
            </properties>

            <dependencies>
               $dependencies
               $sentryDependency
            </dependencies>

            <build>
                <plugins>
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
