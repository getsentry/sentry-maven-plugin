package io.sentry.autoinstall;

import org.apache.maven.model.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.sentry.autoinstall.Constants.SENTRY_ARTIFACT_ID;
import static io.sentry.autoinstall.Constants.SENTRY_GROUP_ID;

public class SentryInstaller {
    public static final String SENTRY_VERSION = "6.28.0";

    private final Logger logger;

    public SentryInstaller() {
        this(LoggerFactory.getLogger(SentryInstaller.class));
    }

    public SentryInstaller(Logger logger) {
        this.logger = logger;
    }

    public String install(List<Dependency> dependencyList) {

        Dependency sentryDependency = dependencyList.stream().filter((dep) ->
            dep.getGroupId().equals(SENTRY_GROUP_ID) && dep.getArtifactId().equals(SENTRY_ARTIFACT_ID)
        ).findFirst().orElse(null);

        if (sentryDependency != null) {
            logger.info("Sentry already installed " + sentryDependency.getVersion());
            return sentryDependency.getVersion();
        } else {
            logger.info("Installing Sentry with version " + SENTRY_VERSION);
            Dependency newDep = new Dependency();
            newDep.setGroupId(SENTRY_GROUP_ID);
            newDep.setArtifactId(SENTRY_ARTIFACT_ID);
            newDep.setVersion(SENTRY_VERSION);

            dependencyList.add(newDep);
        }
        return SENTRY_VERSION;
    }
}
