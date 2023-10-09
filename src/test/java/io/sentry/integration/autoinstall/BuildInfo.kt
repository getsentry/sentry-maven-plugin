package io.sentry.integration.autoinstall

import java.util.Properties

object BuildInfo {
    val projectVersion: String by lazy {
        Properties().apply {
            load({}.javaClass.getResourceAsStream("build-info.properties"))
        }.getProperty("build.version", "0")
    }
}
