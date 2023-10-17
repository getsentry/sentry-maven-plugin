package io.sentry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** Placeholder class to define skipAutoInstall property to be added to plugin definition */
@Mojo(name = "extensionConfigurationHolder", defaultPhase = LifecyclePhase.VALIDATE)
public class ExtensionConfigurationHolderMojo extends AbstractMojo {

  @Parameter(property = "skipAutoInstall", defaultValue = "false")
  private boolean skipAutoInstall;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {}
}
