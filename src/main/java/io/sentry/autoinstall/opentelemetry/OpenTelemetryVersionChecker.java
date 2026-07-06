package io.sentry.autoinstall.opentelemetry;

import static io.sentry.Constants.SENTRY_GROUP_ID;
import static io.sentry.Constants.SENTRY_OPENTELEMETRY_BOM_ARTIFACT_ID;

import io.sentry.semver.Version;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fails the build when the OpenTelemetry versions resolved for a project are downgraded below the
 * versions Sentry's {@code sentry-opentelemetry-*} artifacts were built and tested against.
 *
 * <p>Sentry's OpenTelemetry artifacts declare the exact OpenTelemetry versions they require. When
 * another dependency management mechanism (most commonly Spring Boot) forces a lower OpenTelemetry
 * version — which happens silently, without the user declaring any OpenTelemetry version — running
 * against the mismatched versions can throw {@code ClassNotFoundException} / {@code
 * NoSuchMethodError} at runtime. This surfaces the problem at build time with actionable guidance
 * (import {@code sentry-opentelemetry-bom}) instead of letting it fail at runtime.
 *
 * <p>Only OpenTelemetry modules that a {@code sentry-opentelemetry-*} artifact actually requires
 * are inspected, so unrelated OpenTelemetry usage is never flagged.
 */
public class OpenTelemetryVersionChecker {

  static final @NotNull String OPENTELEMETRY_GROUP_ID = "io.opentelemetry";
  static final @NotNull String SENTRY_OPENTELEMETRY_ARTIFACT_PREFIX = "sentry-opentelemetry-";
  static final @NotNull String SPRING_BOOT_GROUP_ID = "org.springframework.boot";
  static final @NotNull String DOCS_URL =
      "https://docs.sentry.io/platforms/java/tracing/instrumentation/opentelemetry/troubleshooting/";

  private final @NotNull Logger logger;

  public OpenTelemetryVersionChecker() {
    this(LoggerFactory.getLogger(OpenTelemetryVersionChecker.class));
  }

  public OpenTelemetryVersionChecker(final @NotNull Logger logger) {
    this.logger = logger;
  }

  public void check(
      final @NotNull MavenProject project,
      final @NotNull List<Artifact> resolvedArtifacts,
      final @NotNull String sentryVersion,
      final @NotNull ProjectBuilder projectBuilder,
      final @NotNull RepositorySystem repositorySystem,
      final @NotNull ProjectBuildingRequest baseRequest)
      throws MavenExecutionException {

    if (!hasSentryOpenTelemetryDependency(resolvedArtifacts)) {
      return;
    }

    final @NotNull Map<String, String> requiredVersions =
        collectRequiredVersions(
            project, resolvedArtifacts, projectBuilder, repositorySystem, baseRequest);
    if (requiredVersions.isEmpty()) {
      return;
    }

    final @NotNull Map<String, String> resolvedVersions =
        resolvedOpenTelemetryVersions(resolvedArtifacts);

    final @NotNull List<VersionMismatch> downgrades =
        collectDowngrades(requiredVersions, resolvedVersions);

    if (!downgrades.isEmpty()) {
      throw new MavenExecutionException(
          buildMessage(downgrades, sentryVersion, hasSpringBoot(resolvedArtifacts)),
          project.getFile());
    }
  }

  static boolean hasSentryOpenTelemetryDependency(final @NotNull List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream().anyMatch(OpenTelemetryVersionChecker::isSentryOpenTelemetry);
  }

  private static boolean isSentryOpenTelemetry(final @NotNull Artifact artifact) {
    return artifact.getGroupId().equals(SENTRY_GROUP_ID)
        && artifact.getArtifactId().startsWith(SENTRY_OPENTELEMETRY_ARTIFACT_PREFIX)
        && !artifact.getArtifactId().equals(SENTRY_OPENTELEMETRY_BOM_ARTIFACT_ID);
  }

  private static boolean isOpenTelemetryGroup(final @NotNull String groupId) {
    return groupId.equals(OPENTELEMETRY_GROUP_ID)
        || groupId.startsWith(OPENTELEMETRY_GROUP_ID + ".");
  }

  /**
   * Resolves each present {@code sentry-opentelemetry-*} artifact and collects the OpenTelemetry
   * versions it requires (from its declared dependencies and its own dependency management),
   * keeping the highest version requested per module.
   */
  private @NotNull Map<String, String> collectRequiredVersions(
      final @NotNull MavenProject project,
      final @NotNull List<Artifact> resolvedArtifacts,
      final @NotNull ProjectBuilder projectBuilder,
      final @NotNull RepositorySystem repositorySystem,
      final @NotNull ProjectBuildingRequest baseRequest) {
    final @NotNull Map<String, String> required = new LinkedHashMap<>();
    for (final @NotNull Artifact artifact : resolvedArtifacts) {
      if (!isSentryOpenTelemetry(artifact)) {
        continue;
      }
      try {
        final org.apache.maven.artifact.@NotNull Artifact projectArtifact =
            repositorySystem.createProjectArtifact(
                artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
        final @NotNull ProjectBuildingRequest request =
            new DefaultProjectBuildingRequest(baseRequest);
        request.setProject(null);
        request.setResolveDependencies(false);
        request.setRemoteRepositories(project.getRemoteArtifactRepositories());
        request.setValidationLevel(
            org.apache.maven.model.building.ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

        final @NotNull MavenProject artifactProject =
            projectBuilder.build(projectArtifact, true, request).getProject();

        mergeOpenTelemetryVersions(artifactProject.getDependencies(), required);
        final @Nullable DependencyManagement dependencyManagement =
            artifactProject.getDependencyManagement();
        if (dependencyManagement != null) {
          mergeOpenTelemetryVersions(dependencyManagement.getDependencies(), required);
        }
      } catch (Throwable t) {
        logger.info(
            "Unable to resolve "
                + artifact.getArtifactId()
                + " to verify OpenTelemetry versions: "
                + t.getMessage());
      }
    }
    return required;
  }

  private void mergeOpenTelemetryVersions(
      final @NotNull List<Dependency> dependencies, final @NotNull Map<String, String> target) {
    for (final @NotNull Dependency dependency : dependencies) {
      if (!isOpenTelemetryGroup(dependency.getGroupId()) || dependency.getVersion() == null) {
        continue;
      }
      final @NotNull String key = key(dependency.getGroupId(), dependency.getArtifactId());
      final @Nullable String existing = target.get(key);
      if (existing == null || isHigher(dependency.getVersion(), existing)) {
        target.put(key, dependency.getVersion());
      }
    }
  }

  private @NotNull Map<String, String> resolvedOpenTelemetryVersions(
      final @NotNull List<Artifact> resolvedArtifacts) {
    final @NotNull Map<String, String> resolved = new LinkedHashMap<>();
    for (final @NotNull Artifact artifact : resolvedArtifacts) {
      if (isOpenTelemetryGroup(artifact.getGroupId())) {
        resolved.put(key(artifact.getGroupId(), artifact.getArtifactId()), artifact.getVersion());
      }
    }
    return resolved;
  }

  static @NotNull List<VersionMismatch> collectDowngrades(
      final @NotNull Map<String, String> requiredVersions,
      final @NotNull Map<String, String> resolvedVersions) {
    final @NotNull List<VersionMismatch> downgrades = new ArrayList<>();
    for (final Map.@NotNull Entry<String, String> entry : requiredVersions.entrySet()) {
      final @Nullable String resolved = resolvedVersions.get(entry.getKey());
      if (resolved != null && isDowngrade(entry.getValue(), resolved)) {
        downgrades.add(new VersionMismatch(entry.getKey(), entry.getValue(), resolved));
      }
    }
    return downgrades;
  }

  private static boolean isDowngrade(
      final @NotNull String required, final @NotNull String resolved) {
    if (required.equals(resolved)) {
      return false;
    }
    try {
      return Version.parseVersion(resolved).isLowerThan(Version.parseVersion(required));
    } catch (RuntimeException e) {
      // versions that can't be parsed as semver can't be compared reliably; don't flag them
      return false;
    }
  }

  private static boolean isHigher(final @NotNull String candidate, final @NotNull String existing) {
    try {
      return Version.parseVersion(candidate).isGreaterThan(Version.parseVersion(existing));
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static boolean hasSpringBoot(final @NotNull List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .anyMatch((artifact) -> artifact.getGroupId().startsWith(SPRING_BOOT_GROUP_ID));
  }

  static @NotNull String buildMessage(
      final @NotNull List<VersionMismatch> downgrades,
      final @NotNull String sentryVersion,
      final boolean springBoot) {
    final @NotNull StringBuilder details = new StringBuilder();
    for (final @NotNull VersionMismatch mismatch : downgrades) {
      details
          .append("  - ")
          .append(mismatch.module)
          .append(": Sentry requires ")
          .append(mismatch.required)
          .append(" but ")
          .append(mismatch.resolved)
          .append(" was resolved\n");
    }

    final @NotNull String ordering =
        springBoot
            ? "In Maven the first matching <dependencyManagement> entry wins, so declare "
                + SENTRY_OPENTELEMETRY_BOM_ARTIFACT_ID
                + " before spring-boot-dependencies (or in your child POM, which takes "
                + "precedence over a parent POM's dependency management).\n\n"
            : "";

    return "Sentry detected that OpenTelemetry was downgraded below the version its integration "
        + "requires.\n\n"
        + "The Sentry OpenTelemetry integration was built against specific OpenTelemetry versions, "
        + "but the following were downgraded by another dependency management mechanism:\n\n"
        + details
        + "\nRunning with these downgraded OpenTelemetry versions can cause "
        + "ClassNotFoundException / NoSuchMethodError at runtime.\n\n"
        + "To fix this, import the Sentry OpenTelemetry BOM in your <dependencyManagement> so its "
        + "versions win dependency resolution:\n\n"
        + ordering
        + "  <dependencyManagement>\n"
        + "    <dependencies>\n"
        + "      <dependency>\n"
        + "        <groupId>"
        + SENTRY_GROUP_ID
        + "</groupId>\n"
        + "        <artifactId>"
        + SENTRY_OPENTELEMETRY_BOM_ARTIFACT_ID
        + "</artifactId>\n"
        + "        <version>"
        + sentryVersion
        + "</version>\n"
        + "        <type>pom</type>\n"
        + "        <scope>import</scope>\n"
        + "      </dependency>\n"
        + "    </dependencies>\n"
        + "  </dependencyManagement>\n\n"
        + "See "
        + DOCS_URL
        + " for details.\n\n"
        + "You can disable this check by setting "
        + "<skipValidateOpenTelemetryVersions>true</skipValidateOpenTelemetryVersions> in the "
        + SENTRY_GROUP_ID
        + " plugin configuration.";
  }

  private static @NotNull String key(
      final @NotNull String groupId, final @NotNull String artifactId) {
    return groupId + ":" + artifactId;
  }

  static class VersionMismatch {
    final @NotNull String module;
    final @NotNull String required;
    final @NotNull String resolved;

    VersionMismatch(
        final @NotNull String module,
        final @NotNull String required,
        final @NotNull String resolved) {
      this.module = module;
      this.required = required;
      this.resolved = resolved;
    }
  }
}
