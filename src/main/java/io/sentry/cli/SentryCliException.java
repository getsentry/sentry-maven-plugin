package io.sentry.cli;

public class SentryCliException extends RuntimeException {

  private CliFailureReason reason;

  public SentryCliException(CliFailureReason reason) {
    super(reason.getText());
    this.reason = reason;
  }

  public CliFailureReason getReason() {
    return reason;
  }
}
