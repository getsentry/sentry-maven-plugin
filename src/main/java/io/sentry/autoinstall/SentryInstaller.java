package io.sentry.autoinstall;

import org.apache.maven.model.Dependency;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.sentry.autoinstall.Constants.SENTRY_ARTIFACT_ID;
import static io.sentry.autoinstall.Constants.SENTRY_GROUP_ID;

public class SentryInstaller {

    private static final String SENTRY_VERSION = "6.26.0";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SentryInstaller.class);

    public String install(List<Dependency> dependencyList) {
        Dependency sentryDependency = dependencyList.stream().filter((dep) -> dep.getGroupId().equals("io.sentry") && dep.getArtifactId().equals("sentry")).findFirst().orElse(null);

        if(sentryDependency != null) {
            logger.info("Sentry already installed " + sentryDependency.getVersion());
            return sentryDependency.getVersion();
        } else {
            logger.info("Installing Sentry");
            Dependency newDep = new Dependency();
            newDep.setGroupId(SENTRY_GROUP_ID);
            newDep.setArtifactId(SENTRY_ARTIFACT_ID);
            newDep.setVersion(SENTRY_VERSION);

            dependencyList.add(newDep);
        }
        return SENTRY_VERSION;
    }
}
