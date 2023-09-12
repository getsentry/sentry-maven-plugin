package io.sentry.autoinstall.spring;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.semver.Version;
import org.apache.maven.model.Dependency;

import java.util.List;

public class Spring6InstallStrategy extends AbstractIntegrationInstaller {

    public static final String SENTRY_SPRING_6_ID = "sentry-spring-jakarta";
    private static final String SPRING_GROUP = "org.springframework";
    private static final String SPRING_6_ID = "spring-core";

    protected Version minSupportedSentryVersion() {
        return Version.create(6, 7, 0);
    }

    @Override
    protected Dependency findThirdPartyDependency(List<Dependency> dependencyList) {
        return dependencyList.stream().filter((dep) ->
            dep.getGroupId().equals(SPRING_GROUP) && dep.getArtifactId().equals(SPRING_6_ID)
        ).findFirst().orElse(null);
    }

    @Override
    protected Version minSupportedThirdPartyVersion() {
        return Version.create(6, 0, 0);
    }

    @Override
    protected boolean shouldInstallModule(AutoInstallState autoInstallState) {
        return autoInstallState.isInstallSpring();
    }

    @Override
    protected String sentryModuleId() {
        return SENTRY_SPRING_6_ID;
    }
}
