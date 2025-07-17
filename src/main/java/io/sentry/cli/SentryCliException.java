package io.sentry.cli;

public class SentryCliException extends RuntimeException {

  private CliFailureReason reason;

  public SentryCliException(CliFailureReason reason, Throwable cause) {
    super(reason.getText(), cause);
    this.reason = reason;
  }

  public CliFailureReason getReason() {
    return reason;
  }
}
