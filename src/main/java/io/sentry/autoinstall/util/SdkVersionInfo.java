package io.sentry.autoinstall.util;

import io.sentry.autoinstall.SentryInstaller;
import java.io.IOException;
import java.util.Properties;

public class SdkVersionInfo {
  public static String getSentryVersion() {
    Properties sdkProperties = new Properties();
    try {
      sdkProperties.load(SentryInstaller.class.getResourceAsStream("/sentry-sdk.properties"));
    } catch (IOException e) {
      return null;
    }

    return sdkProperties.getProperty("sdk_version");
  }
}
