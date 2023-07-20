package io.sentry.autoinstall;

import org.apache.maven.model.Dependency;

import java.util.List;

public class SpringIntegrationInstaller extends AbstractIntegrationInstaller {

    private static final String SPRING_ARTIFACT_ID = "";
    private static final String SPRING_JAKARTA_ARTIFACT_ID = "";
    @Override
    public void install(List<Dependency> dependencyList, String sentryVersion) {

    }
}
