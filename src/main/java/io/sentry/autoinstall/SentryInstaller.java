package io.sentry.autoinstall;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.maven.model.Dependency;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.sentry.autoinstall.Constants.SENTRY_ARTIFACT_ID;
import static io.sentry.autoinstall.Constants.SENTRY_GROUP_ID;

public class SentryInstaller {
    public static final String SENTRY_VERSION = "6.28.0";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SentryInstaller.class);

    public static String install(List<Dependency> dependencyList) {

        Dependency sentryDependency = dependencyList.stream().filter((dep) ->
            dep.getGroupId().equals(SENTRY_GROUP_ID) && dep.getArtifactId().equals(SENTRY_ARTIFACT_ID)
        ).findFirst().orElse(null);

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
