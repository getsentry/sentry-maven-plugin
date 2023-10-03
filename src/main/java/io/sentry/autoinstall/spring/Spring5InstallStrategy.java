package io.sentry.autoinstall.spring;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.autoinstall.SentryInstaller;
import io.sentry.semver.Version;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Spring5InstallStrategy extends AbstractIntegrationInstaller {

    public static final String SENTRY_SPRING_5_ID = "sentry-spring";
    private static final String SPRING_GROUP = "org.springframework";
    private static final String SPRING_5_ID = "spring-core";

    public Spring5InstallStrategy() {
        this(LoggerFactory.getLogger(SentryInstaller.class));
    }

    public Spring5InstallStrategy(Logger logger) {
        this.logger = logger;
    }

    protected Version minSupportedSentryVersion() {
        return Version.create(4, 1, 0);
    }

    @Override
    protected Artifact findThirdPartyDependency(List<Artifact> resolvedArtifacts) {
        return resolvedArtifacts.stream().filter((dep) ->
            dep.getGroupId().equals(SPRING_GROUP) && dep.getArtifactId().equals(SPRING_5_ID)
        ).findFirst().orElse(null);
    }

    @Override
    protected Version minSupportedThirdPartyVersion() {
        return Version.create(5, 1, 2);
    }

    @Override
    protected Version maxSupportedThirdPartyVersion() {
        return Version.create(5, 9999, 9999);
    }

    @Override
    protected boolean shouldInstallModule(AutoInstallState autoInstallState) {
        return autoInstallState.isInstallSpring();
    }

    @Override
    protected String sentryModuleId() {
        return SENTRY_SPRING_5_ID;
    }
}
