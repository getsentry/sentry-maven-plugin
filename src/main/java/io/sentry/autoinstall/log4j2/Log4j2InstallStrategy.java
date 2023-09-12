package io.sentry.autoinstall.log4j2;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.semver.Version;
import org.apache.maven.model.Dependency;

import java.util.List;

public class Log4j2InstallStrategy extends AbstractIntegrationInstaller {
    private static final String LOG4J2_GROUP = "org.apache.logging.log4j";
    private static final String LOG4J2_ID = "log4j-api";
    private static final String SENTRY_LOG4J2_ID = "sentry-log4j2";

    @Override
    protected Dependency findThirdPartyDependency(List<Dependency> dependencyList) {
        return dependencyList.stream().filter((dep) ->
            dep.getGroupId().equals(LOG4J2_GROUP) && dep.getArtifactId().equals(LOG4J2_ID)
        ).findFirst().orElse(null);
    }

    @Override
    protected boolean shouldInstallModule(AutoInstallState autoInstallState) {
        return true;
    }

    @Override
    protected Version minSupportedThirdPartyVersion() {
        return Version.create(2, 0, 0);
    }

    @Override
    protected Version minSupportedSentryVersion() {
        return Version.create(6, 25, 2);
    }

    @Override
    protected String sentryModuleId() {
        return SENTRY_LOG4J2_ID;
    }
}
