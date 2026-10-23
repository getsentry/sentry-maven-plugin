package io.sentry.integration

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Utility to install a specific version of the Maven wrapper for tests
 */
fun installMavenWrapper(
    directory: File,
    version: String,
) {
    bootstrapMavenWrapper(directory)

    val mavenVersionToUse = System.getProperty("maven.test.version") ?: version
    setWrapperMavenVersion(directory, mavenVersionToUse)
}

private fun setWrapperMavenVersion(
    directory: File,
    version: String,
) {
    val propertiesFile = File(directory, ".mvn/wrapper/maven-wrapper.properties")
    val distributionUrl =
        "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version/apache-maven-$version-bin.zip"
    val otherProperties = propertiesFile.readLines().filterNot { it.trim().startsWith("distributionUrl=") }
    propertiesFile.writeText((listOf(distributionUrl) + otherProperties).joinToString(separator = "\n", postfix = "\n"))
}

/**
 * If no Maven wrapper is present in `targetDirectory`, obtain it by copying it from this project's root directory
 */
private fun bootstrapMavenWrapper(targetDirectory: File) {
    val projectRoot = File(System.getProperty("user.dir"))

    if (File(targetDirectory.toPath().resolve("mvnw").toString()).exists()) {
        return
    }

    Files.copy(
        projectRoot.toPath().resolve("mvnw"),
        targetDirectory.toPath().resolve("mvnw"),
        StandardCopyOption.REPLACE_EXISTING,
    )
    targetDirectory.resolve("mvnw").setExecutable(true)
    Files.copy(
        projectRoot.toPath().resolve("mvnw.cmd"),
        targetDirectory.toPath().resolve("mvnw.cmd"),
        StandardCopyOption.REPLACE_EXISTING,
    )

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
