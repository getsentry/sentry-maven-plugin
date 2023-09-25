package io.sentry.autoinstall.spring;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.autoinstall.SentryInstaller;
import io.sentry.semver.Version;
import org.apache.maven.model.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.sentry.autoinstall.Constants.SENTRY_ARTIFACT_ID;
import static io.sentry.autoinstall.Constants.SENTRY_GROUP_ID;

public class SpringBoot3InstallStrategy extends AbstractIntegrationInstaller {

    public static final String SENTRY_SPRING_BOOT_3_ID = "sentry-spring-boot-jakarta";
    private static final String SPRING_GROUP = "org.springframework.boot";
    private static final String SPRING_BOOT_3_ID = "spring-boot-starter";

    public SpringBoot3InstallStrategy() {
        this(LoggerFactory.getLogger(SentryInstaller.class));
    }

    public SpringBoot3InstallStrategy(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected Version minSupportedSentryVersion() {
        return Version.create(6, 28, 0);
    }

    @Override
    protected Dependency findThirdPartyDependency(List<Dependency> dependencyList) {
        return dependencyList.stream().filter((dep) ->
            dep.getGroupId().equals(SPRING_GROUP) && dep.getArtifactId().equals(SPRING_BOOT_3_ID)
        ).findFirst().orElse(null);
    }

    @Override
    protected Version minSupportedThirdPartyVersion() {
        return Version.create(3, 0, 0);
    }

    @Override
    protected boolean shouldInstallModule(AutoInstallState autoInstallState) {
        return autoInstallState.isInstallSpring();
    }

    @Override
    protected String sentryModuleId() {
        return SENTRY_SPRING_BOOT_3_ID;
    }
}
