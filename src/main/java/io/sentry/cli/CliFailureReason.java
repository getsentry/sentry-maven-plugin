package io.sentry.cli;

import org.jetbrains.annotations.NotNull;

public enum CliFailureReason {
  OUTDATED(
      "Hit a non-existing endpoint on Sentry. Most likely you have to update your self-hosted Sentry version to get all of the latest features."),
  ORG_SLUG(
      "An organization slug is required. You might want to provide your Sentry org name via the '<org>' configuration option or provide an org auth token via the '<authToken>' configuration option"),
  PROJECT_SLUG(
      "A project slug is required. You might want to provide your Sentry project name via the '<project> configuration option"),
  INVALID_ORG_AUTH_TOKEN(
      "Failed to parse org auth token. You might want to provide a custom url to your self-hosted Sentry instance via the '<url>' configuration option"),
  INVALID_TOKEN(
      "An API request has failed due to an invalid token. Please provide a valid token with required permissions."),
  UNKNOWN("An error occurred while executing sentry-cli. Please check the detailed log output.");

  private final @NotNull String text;

  CliFailureReason(final @NotNull String text) {
    this.text = text;
  }

  public @NotNull String getText() {
    return text;
  }
}
