package io.sentry.autoinstall;

public class AutoInstallState {

  private String sentryVersion;
  private boolean installSpring = false;
  private boolean installLogback = false;
  private boolean installLog4j2 = false;
  private boolean installJdbc = false;
  private boolean installGraphql = false;

  private boolean installQuartz = false;

  public String getSentryVersion() {
    return sentryVersion;
  }

  public void setSentryVersion(String sentryVersion) {
    this.sentryVersion = sentryVersion;
  }

  public boolean isInstallSpring() {
    return installSpring;
  }

  public void setInstallSpring(boolean installSpring) {
    this.installSpring = installSpring;
  }

  public boolean isInstallLogback() {
    return installLogback;
  }

  public void setInstallLogback(boolean installLogback) {
    this.installLogback = installLogback;
  }

  public boolean isInstallLog4j2() {
    return installLog4j2;
  }

  public void setInstallLog4j2(boolean installLog4j2) {
    this.installLog4j2 = installLog4j2;
  }

  public boolean isInstallJdbc() {
    return installJdbc;
  }

  public void setInstallJdbc(boolean installJdbc) {
    this.installJdbc = installJdbc;
  }

  public boolean isInstallGraphql() {
    return installGraphql;
  }

  public void setInstallGraphql(boolean installGraphql) {
    this.installGraphql = installGraphql;
  }

  public boolean isInstallQuartz() {
    return installQuartz;
  }

  public void setInstallQuartz(boolean installQuartz) {
    this.installQuartz = installQuartz;
  }
}
