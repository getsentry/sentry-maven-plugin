package io.sentry.autoinstall.util;

import static io.sentry.Constants.SENTRY_BOM_ARTIFACT_ID;
import static io.sentry.Constants.SENTRY_GROUP_ID;
import static io.sentry.Constants.SENTRY_OPENTELEMETRY_BOM_ARTIFACT_ID;
import static io.sentry.Constants.SENTRY_SDK_ARTIFACT_ID;

import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ManagedSentryVersionResolver {
  private static final @NotNull String SENTRY_OPENTELEMETRY_ARTIFACT_PREFIX =
      "sentry-opentelemetry-";

  private ManagedSentryVersionResolver() {}

  public static @Nullable String getManagedSentryVersion(final @NotNull MavenProject project) {
    final @NotNull Set<String> managedVersions = new LinkedHashSet<>();

    collectManagedSentryVersions(project.getDependencyManagement(), project, managedVersions);
    collectManagedSentryVersions(
        project.getModel().getDependencyManagement(), project, managedVersions);

    final @Nullable Model originalModel = project.getOriginalModel();
    if (originalModel != null) {
      collectManagedSentryVersions(
          originalModel.getDependencyManagement(), project, managedVersions);
    }

    return managedVersions.stream().findFirst().orElse(null);
  }

  private static void collectManagedSentryVersions(
      final @Nullable DependencyManagement dependencyManagement,
      final @NotNull MavenProject project,
      final @NotNull Set<String> managedVersions) {
    if (dependencyManagement == null) {
      return;
    }

    for (final @NotNull Dependency dependency : dependencyManagement.getDependencies()) {
      if (!isSentryManagedVersionSource(dependency)) {
        continue;
      }

      final @Nullable String version = resolveVersion(dependency.getVersion(), project);
      if (version != null) {
        managedVersions.add(version);
      }
    }
  }

  private static boolean isSentryManagedVersionSource(final @NotNull Dependency dependency) {
    if (!SENTRY_GROUP_ID.equals(dependency.getGroupId())) {
      return false;
    }

    final @NotNull String artifactId = dependency.getArtifactId();
    return SENTRY_SDK_ARTIFACT_ID.equals(artifactId)
        || SENTRY_BOM_ARTIFACT_ID.equals(artifactId)
        || SENTRY_OPENTELEMETRY_BOM_ARTIFACT_ID.equals(artifactId)
        || artifactId.startsWith(SENTRY_OPENTELEMETRY_ARTIFACT_PREFIX);
  }

  private static @Nullable String resolveVersion(
      final @Nullable String version, final @NotNull MavenProject project) {
    if (version == null || version.trim().isEmpty()) {
      return null;
    }

    if (version.startsWith("${") && version.endsWith("}")) {
      final @NotNull String propertyName = version.substring(2, version.length() - 1);
      return resolveVersion(project.getProperties().getProperty(propertyName), project);
    }

    if (version.contains("${")) {
      return null;
    }

    return version;
  }
}
