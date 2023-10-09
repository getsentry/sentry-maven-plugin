package io.sentry

import io.sentry.autoinstall.SentryInstaller
import java.util.Properties

object SdkVersionInfo {
    val sentryVersion: String by lazy {
        Properties().apply {
            load(SentryInstaller::class.java.getResourceAsStream("/sentry-sdk.properties"))
        }.getProperty("sdk_version", "0")
    }
}
