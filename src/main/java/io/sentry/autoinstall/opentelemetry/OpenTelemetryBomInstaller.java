package io.sentry.autoinstall.opentelemetry;

import static io.sentry.Constants.SENTRY_GROUP_ID;
import static io.sentry.Constants.SENTRY_OPENTELEMETRY_BOM_ARTIFACT_ID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
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
 * Aligns OpenTelemetry dependency versions with the versions Sentry was tested against, by
 * expanding {@code io.sentry:sentry-opentelemetry-bom} and applying its managed OpenTelemetry
 * versions to the project's {@code dependencyManagement}.
 *
 * <p>Maven resolves BOM imports during effective-model building, which happens before {@code
 * afterProjectsRead}. A late-injected import-scope BOM is therefore a no-op. Instead we expand the
 * BOM into concrete versions and: overwrite OpenTelemetry versions inherited from parents/other
 * BOMs (e.g. Spring Boot) in place, add managed entries for OpenTelemetry artifacts that are not
 * managed yet, and skip artifacts the user pinned explicitly (respecting their choice, warning when
 * it differs from Sentry's tested version).
 */
public class OpenTelemetryBomInstaller {

  public static final @NotNull String SENTRY_OPENTELEMETRY_BOM_ID =
      SENTRY_OPENTELEMETRY_BOM_ARTIFACT_ID;
  public static final @NotNull String SENTRY_OPENTELEMETRY_AGENT_ID = "sentry-opentelemetry-agent";
  public static final @NotNull String SENTRY_OPENTELEMETRY_ARTIFACT_PREFIX =
      "sentry-opentelemetry-";
  public static final @NotNull String OPENTELEMETRY_GROUP_ID = "io.opentelemetry";

  private final @NotNull Logger logger;

  public OpenTelemetryBomInstaller() {
    this(LoggerFactory.getLogger(OpenTelemetryBomInstaller.class));
  }

  public OpenTelemetryBomInstaller(final @NotNull Logger logger) {
    this.logger = logger;
  }

  public void install(
      final @NotNull MavenProject project,
      final @NotNull List<Artifact> resolvedArtifacts,
      final @NotNull String sentryVersion,
      final @NotNull ProjectBuilder projectBuilder,
      final @NotNull RepositorySystem repositorySystem,
      final @NotNull ProjectBuildingRequest baseRequest) {

    if (!hasEligibleOpenTelemetryDependency(resolvedArtifacts)) {
      logger.info(
          SENTRY_OPENTELEMETRY_BOM_ID
              + " won't be installed because no Sentry OpenTelemetry dependency was found");
      return;
    }

    if (isBomAlreadyManaged(project)) {
      logger.info(
          SENTRY_OPENTELEMETRY_BOM_ID
              + " won't be installed because it was already installed directly");
      return;
    }

    final @NotNull Map<String, String> bomVersions =
        expandBom(project, sentryVersion, projectBuilder, repositorySystem, baseRequest);

    if (bomVersions.isEmpty()) {
      logger.info(
          SENTRY_OPENTELEMETRY_BOM_ID
              + " won't be installed because its OpenTelemetry versions could not be resolved");
      return;
    }

    apply(project, bomVersions);
  }

  /**
   * Returns true if the project (transitively) uses a Sentry OpenTelemetry integration that is
   * managed by the BOM. The standalone agent fat jar and the BOM itself are excluded.
   */
  public static boolean hasEligibleOpenTelemetryDependency(
      final @NotNull List<Artifact> resolvedArtifacts) {
    return resolvedArtifacts.stream()
        .anyMatch(
            (dep) ->
                dep.getGroupId().equals(SENTRY_GROUP_ID)
                    && dep.getArtifactId().startsWith(SENTRY_OPENTELEMETRY_ARTIFACT_PREFIX)
                    && !dep.getArtifactId().equals(SENTRY_OPENTELEMETRY_BOM_ID)
                    && !dep.getArtifactId().equals(SENTRY_OPENTELEMETRY_AGENT_ID));
  }

  /**
   * Returns true if the project already imports {@code sentry-opentelemetry-bom} in its own {@code
   * dependencyManagement}. Import-scope BOMs are consumed during effective-model building and are
   * not resolved artifacts, so this is checked against the authored model.
   */
  public static boolean isBomAlreadyManaged(final @NotNull MavenProject project) {
    final @Nullable Model original = project.getOriginalModel();
    if (original == null || original.getDependencyManagement() == null) {
      return false;
    }
    return original.getDependencyManagement().getDependencies().stream()
        .anyMatch(
            (dep) ->
                dep.getGroupId().equals(SENTRY_GROUP_ID)
                    && dep.getArtifactId().equals(SENTRY_OPENTELEMETRY_BOM_ID));
  }

  /**
   * Resolves {@code io.sentry:sentry-opentelemetry-bom:<sentryVersion>} and returns its managed
   * OpenTelemetry versions as a {@code groupId:artifactId -> version} map. Returns an empty map if
   * the BOM cannot be resolved or built (e.g. not published for this version).
   */
  @NotNull
  Map<String, String> expandBom(
      final @NotNull MavenProject project,
      final @NotNull String sentryVersion,
      final @NotNull ProjectBuilder projectBuilder,
      final @NotNull RepositorySystem repositorySystem,
      final @NotNull ProjectBuildingRequest baseRequest) {
    final @NotNull Map<String, String> versions = new HashMap<>();
    try {
      final org.apache.maven.artifact.@NotNull Artifact bomArtifact =
          repositorySystem.createProjectArtifact(
              SENTRY_GROUP_ID, SENTRY_OPENTELEMETRY_BOM_ID, sentryVersion);

      final @NotNull ProjectBuildingRequest request =
          new DefaultProjectBuildingRequest(baseRequest);
      request.setProject(null);
      request.setResolveDependencies(false);
      // resolve the BOM using the project's own repositories, not just the session defaults
      request.setRemoteRepositories(project.getRemoteArtifactRepositories());
      request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);

      final @NotNull MavenProject bomProject =
          projectBuilder.build(bomArtifact, true, request).getProject();

      final @Nullable DependencyManagement dependencyManagement =
          bomProject.getDependencyManagement();
      if (dependencyManagement != null) {
        for (final @NotNull Dependency dependency : dependencyManagement.getDependencies()) {
          if (OPENTELEMETRY_GROUP_ID.equals(dependency.getGroupId())
              && dependency.getVersion() != null) {
            versions.put(
                key(dependency.getGroupId(), dependency.getArtifactId()), dependency.getVersion());
          }
        }
      }
    } catch (Throwable t) {
      logger.info(
          "Unable to resolve "
              + SENTRY_OPENTELEMETRY_BOM_ID
              + ":"
              + sentryVersion
              + ", skipping OpenTelemetry version alignment: "
              + t.getMessage());
    }
    return versions;
  }

  void apply(final @NotNull MavenProject project, final @NotNull Map<String, String> bomVersions) {
    final @NotNull Map<String, String> userPinned = userPinnedVersions(project.getOriginalModel());

    final @NotNull Model model = project.getModel();
    @Nullable DependencyManagement dependencyManagement = model.getDependencyManagement();
    if (dependencyManagement == null) {
      dependencyManagement = new DependencyManagement();
      model.setDependencyManagement(dependencyManagement);
    }

    final @NotNull Map<String, Dependency> managedByKey = new HashMap<>();
    for (final @NotNull Dependency dependency : dependencyManagement.getDependencies()) {
      managedByKey.putIfAbsent(
          key(dependency.getGroupId(), dependency.getArtifactId()), dependency);
    }

    for (final Map.@NotNull Entry<String, String> entry : bomVersions.entrySet()) {
      final @NotNull String gaKey = entry.getKey();
      final @NotNull String bomVersion = entry.getValue();

      if (userPinned.containsKey(gaKey)) {
        final @NotNull String pinnedVersion = userPinned.get(gaKey);
        if (!pinnedVersion.contains("${") && !pinnedVersion.equals(bomVersion)) {
          logger.warn(
              gaKey
                  + " is pinned to "
                  + pinnedVersion
                  + " in your project, but Sentry was tested with "
                  + bomVersion
                  + ". Leaving your version in place. Remove the pin (or import "
                  + SENTRY_OPENTELEMETRY_BOM_ID
                  + " first) to let Sentry manage it.");
        }
        continue;
      }

      final @Nullable Dependency existing = managedByKey.get(gaKey);
      if (existing != null) {
        if (!bomVersion.equals(existing.getVersion())) {
          logger.info(
              "Aligning "
                  + gaKey
                  + " from "
                  + existing.getVersion()
                  + " to "
                  + bomVersion
                  + " ("
                  + SENTRY_OPENTELEMETRY_BOM_ID
                  + ")");
          existing.setVersion(bomVersion);
        }
      } else {
        final @NotNull Dependency managed = new Dependency();
        final int separator = gaKey.indexOf(':');
        managed.setGroupId(gaKey.substring(0, separator));
        managed.setArtifactId(gaKey.substring(separator + 1));
        managed.setVersion(bomVersion);
        dependencyManagement.addDependency(managed);
        logger.info(
            "Managing " + gaKey + ":" + bomVersion + " (" + SENTRY_OPENTELEMETRY_BOM_ID + ")");
      }
    }
  }

  /**
   * Versions the user declared explicitly, read from the authored (un-flattened) model: entries in
   * the project's own {@code dependencyManagement} and direct dependencies with an explicit
   * version. Inherited or BOM-sourced versions are not included.
   */
  static @NotNull Map<String, String> userPinnedVersions(final @Nullable Model originalModel) {
    final @NotNull Map<String, String> pinned = new HashMap<>();
    if (originalModel == null) {
      return pinned;
    }
    final @Nullable DependencyManagement dependencyManagement =
        originalModel.getDependencyManagement();
    if (dependencyManagement != null) {
      for (final @NotNull Dependency dependency : dependencyManagement.getDependencies()) {
        if (dependency.getVersion() != null) {
          pinned.put(
              key(dependency.getGroupId(), dependency.getArtifactId()), dependency.getVersion());
        }
      }
    }
    for (final @NotNull Dependency dependency : originalModel.getDependencies()) {
      if (dependency.getVersion() != null) {
        // a direct dependency with an explicit version wins over dependencyManagement
        pinned.put(
            key(dependency.getGroupId(), dependency.getArtifactId()), dependency.getVersion());
      }
    }
    return pinned;
  }

  private static @NotNull String key(
      final @NotNull String groupId, final @NotNull String artifactId) {
    return groupId + ":" + artifactId;
  }
}
