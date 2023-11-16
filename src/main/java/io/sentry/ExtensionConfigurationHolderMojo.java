package io.sentry;

import static io.sentry.config.PluginConfig.DEFAULT_SKIP_AUTO_INSTALL_STRING;
import static io.sentry.config.PluginConfig.DEFAULT_SKIP_TELEMETRY_STRING;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/** Placeholder class to define skipAutoInstall property to be added to plugin definition */
@Mojo(name = "extensionConfigurationHolder", defaultPhase = LifecyclePhase.VALIDATE)
public class ExtensionConfigurationHolderMojo extends AbstractMojo {
  @SuppressWarnings("UnusedVariable")
  @Parameter(defaultValue = DEFAULT_SKIP_AUTO_INSTALL_STRING)
  private boolean skipAutoInstall;

  @SuppressWarnings("UnusedVariable")
  @Parameter(defaultValue = DEFAULT_SKIP_TELEMETRY_STRING)
  private boolean skipTelemetry;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {}
}
