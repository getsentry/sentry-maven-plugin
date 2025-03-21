package io.sentry.integration

import org.apache.maven.it.Verifier
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Utility to install a specific version of the Maven wrapper for tests
 */
fun installMavenWrapper(
    directory: File,
    version: String,
) {
    bootstrapMavenWrapper(directory)

    val mavenVersionToUse = System.getProperty("maven.test.version") ?: version

    val emptyPom =
        """
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
            <groupId>io.sentry.autoinstall</groupId>
            <artifactId>installmaven</artifactId>
            <version>1.0-SNAPSHOT</version>
        </project>
        """.trimIndent()

    Files.write(directory.toPath().resolve("pom.xml"), emptyPom.toByteArray(), StandardOpenOption.CREATE)
    println("wrote pom")
    Verifier(directory.absolutePath).apply {
        addCliOption("-N")
        addCliOption("-Dmaven=$mavenVersionToUse")
        executeGoal("wrapper:wrapper")
    }
    Files.delete(directory.toPath().resolve("pom.xml"))
}

/**
 * If no Maven wrapper is present in `targetDirectory`, obtain it by copying it from this project's root directory
 */
private fun bootstrapMavenWrapper(targetDirectory: File) {
    val projectRoot = File(System.getProperty("user.dir"))

    println("Bootstrapping")

    if (File(targetDirectory.toPath().resolve("mvnw").toString()).exists()) {
        println("already there")
        return
    }

    println("copying")
    Files.copy(
        projectRoot.toPath().resolve("mvnw"),
        targetDirectory.toPath().resolve("mvnw"),
        StandardCopyOption.REPLACE_EXISTING,
    )
    Files.copy(
        projectRoot.toPath().resolve("mvnw.cmd"),
        targetDirectory.toPath().resolve("mvnw.cmd"),
        StandardCopyOption.REPLACE_EXISTING,
    )

    println("copied")
    val sourceMvnDir = File(projectRoot, ".mvn")
    val targetMvnDir = File(targetDirectory, ".mvn")
    targetMvnDir.mkdirs()
    sourceMvnDir.walk().forEach { sourceFile ->
        if (sourceFile.isFile) {
            val relativePath = sourceFile.relativeTo(sourceMvnDir)
            val targetFile = File(targetMvnDir, relativePath.path)
            targetFile.parentFile.mkdirs()
            Files.copy(
                sourceFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }
}
