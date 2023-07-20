package io.sentry.autoinstall;

import org.apache.maven.model.Dependency;

import java.util.List;

public abstract class AbstractIntegrationInstaller {
    public abstract void install(List<Dependency> dependencyList, String sentryVersion);
}
