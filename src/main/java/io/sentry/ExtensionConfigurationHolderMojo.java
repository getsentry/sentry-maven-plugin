package io.sentry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "extensionConfigurationHolder", defaultPhase = LifecyclePhase.NONE)
public class ExtensionConfigurationHolderMojo extends AbstractMojo {

    @Parameter(property = "autoInstall", defaultValue = "false", required = true)
    private boolean autoInstall;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

    }
}
