package io.sentry.telemetry;

import io.sentry.*;
import io.sentry.cli.SentryCliException;
import io.sentry.cli.SentryCliRunner;
import io.sentry.config.PluginConfig;
import io.sentry.exception.ExceptionMechanismException;
import io.sentry.protocol.Mechanism;
import io.sentry.protocol.User;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.Maven;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentryTelemetryService {

  private static volatile @Nullable SentryTelemetryService instance;

  private static final @NotNull Logger logger =
      LoggerFactory.getLogger(SentryTelemetryService.class);

  public static @NotNull SentryTelemetryService getInstance() {
    if (instance == null) {
      synchronized (SentryTelemetryService.class) {
        if (instance == null) {
          instance = new SentryTelemetryService();
        }
      }
    }

    return instance;
  }

  public static final @NotNull String SENTRY_SAAS_DSN =
      "https://f62d853396929db8f6211f8b53a7f745@o447951.ingest.us.sentry.io/4506229061058560";
  public static final @NotNull String MECHANISM_TYPE = "MavenTelemetry";
  private @NotNull IScopes scopes = NoOpScopes.getInstance();
  private @NotNull ITransaction transaction = NoOpTransaction.getInstance();
  private @NotNull ISentryLifecycleToken token = NoOpScopesLifecycleToken.getInstance();

  private boolean started = false;
  private boolean didAddChildSpans = false;

  private SentryTelemetryService() {}

  public void start(
      final @NotNull PluginConfig pluginConfig,
      final @NotNull MavenProject mavenProject,
      final @NotNull MavenSession mavenSession,
      final @NotNull BuildPluginManager pluginManager) {
    try {
      if (started) {
        return;
      }
      started = true;

      if (pluginConfig.isSkipTelemetry()) {
        logger.info("Sentry telemetry has been disabled.");
      } else {
        final @NotNull SentryCliRunner cliRunner =
            new SentryCliRunner(
                pluginConfig.isDebugSentryCli(),
                pluginConfig.getSentryCliExecutablePath(),
                mavenProject,
                mavenSession,
                pluginManager);
        final @Nullable InfoOutput infoOutput = determineSentryCliInfo(cliRunner, pluginConfig);

        if (infoOutput != null && !infoOutput.isSaas) {
          logger.info(
              "Sentry telemetry has been disabled because this build is running against a self hosted instance.");
          return;
        }

        final @Nullable String sentryCliVersion = determineVersionFromSentryCli(cliRunner);
        logger.info("Sentry telemetry is enabled. To disable set `<skipTelemetry>` to `true`.");
        Sentry.init(
            (options) -> {
              options.setDsn(SENTRY_SAAS_DSN);
              options.setDebug(pluginConfig.isDebug());
              options.setEnablePrettySerializationOutput(false);
              options.setEnvironment("JVM");
              options.setSendModules(false);
              options.setTracesSampleRate(1.0);
              options.setRelease(
                  SentryTelemetryService.class.getPackage().getImplementationVersion());
              options.setTag("BUILD_SYSTEM", "maven");
              options.setTag("BUILD_TYPE", "JVM");
              options.setTag("MAVEN_VERSION", Maven.class.getPackage().getImplementationVersion());
              if (sentryCliVersion != null) {
                options.setTag("SENTRY_CLI_VERSION", sentryCliVersion);
              }
              options.setTag(
                  "SENTRY_autoInstallation_enabled",
                  String.valueOf(!pluginConfig.isSkipAutoInstall()));
              options.setTag(
                  "SENTRY_includeDependenciesReport",
                  String.valueOf(!pluginConfig.isSkipReportDependencies()));
              options.setTag(
                  "SENTRY_includeSourceContext",
                  String.valueOf(!pluginConfig.isSkipSourceBundle()));
            });
        scopes = Sentry.forkedScopes("SentryTelemetryService");
        token = scopes.makeCurrent();

        scopes.addBreadcrumb("SentryTelemetryServiceBreadcrumb");

        startRun("maven build");

        scopes.configureScope(
            (scope) -> {
              final @NotNull User user = new User();

              if (infoOutput != null) {
                String defaultOrg = infoOutput.org;
                if (defaultOrg != null) {
                  user.setId(defaultOrg);
                }
              }
              final @Nullable String org = pluginConfig.getOrg();
              if (org != null) {
                user.setId(org);
              }
              scope.setUser(user);
            });
      }
    } catch (Throwable t) {
      logger.error("Sentry failed to initialize.", t);
    }
  }

  private static class InfoOutput {
    boolean isSaas;
    @Nullable String org;
  }

  private @Nullable InfoOutput determineSentryCliInfo(
      final @NotNull SentryCliRunner cliRunner, final @NotNull PluginConfig pluginConfig) {
    List<String> command = new ArrayList<>();

    final @Nullable String url = pluginConfig.getUrl();
    if (url != null) {
      command.add("--url");
      command.add(url);
    }

    final @Nullable String authToken = pluginConfig.getAuthToken();
    if (authToken != null) {
      command.add("--auth-token");
      command.add(authToken);
    }

    command.add("info");
    try {
      final @Nullable String infoOutput = cliRunner.runSentryCli(String.join(" ", command), false);
      if (infoOutput != null) {
        final InfoOutput info = new InfoOutput();
        Pattern serverPattern = Pattern.compile("Sentry Server: .*sentry.io$", Pattern.MULTILINE);
        info.isSaas = serverPattern.matcher(infoOutput).find();

        final @NotNull Pattern orgRegex =
            Pattern.compile("Default Organization: (.*)$", Pattern.MULTILINE);

        final @NotNull Matcher matcher = orgRegex.matcher(infoOutput);
        if (matcher.find()) {
          info.org = matcher.group(1);
        }
        return info;
      } else {
        return null;
      }
    } catch (MojoExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private @Nullable String determineVersionFromSentryCli(final @NotNull SentryCliRunner cliRunner) {
    List<String> command = new ArrayList<>();

    command.add("--log-level=error");
    command.add("--version");
    try {
      final @Nullable String versionOutput =
          cliRunner.runSentryCli(String.join(" ", command), false);

      if (versionOutput != null) {
        final @NotNull Pattern versionRegex =
            Pattern.compile("sentry-cli (.*)$", Pattern.MULTILINE);
        final @NotNull Matcher matcher = versionRegex.matcher(versionOutput);
        if (matcher.find()) {
          return matcher.group(1);
        }
      }
    } catch (MojoExecutionException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  private void startRun(final @NotNull String transactionName) {
    scopes.startSession();
    final @NotNull TransactionOptions transactionOptions = new TransactionOptions();
    transactionOptions.setBindToScope(true);
    transaction = scopes.startTransaction(transactionName, "build", transactionOptions);
  }

  private void endRun() {
    if (didAddChildSpans) {
      scopes.endSession();
      transaction.finish();
    }
  }

  public @Nullable ISpan startTask(final @NotNull String operation) {
    didAddChildSpans = true;
    scopes.setTag("step", operation);
    final @Nullable ISpan span = scopes.getSpan();

    if (span != null) {
      return span.startChild(operation);
    } else {
      return null;
    }
  }

  public void endTask(final @Nullable ISpan span) {
    if (span != null) {
      span.finish();
    }
  }

  public void captureError(final @NotNull Throwable exception, final @NotNull String operation) {
    final @NotNull String message =
        exception instanceof SentryCliException
            ? operation
                + " failed with SentryCliException and reason "
                + ((SentryCliException) exception).getReason().name()
            : operation + " failed with " + exception.getClass();

    final @NotNull Mechanism mechanism = new Mechanism();
    mechanism.setType(MECHANISM_TYPE);
    mechanism.setHandled(false);
    final @NotNull SentryMinimalException throwable = new SentryMinimalException(message);
    final @NotNull Throwable mechanismException =
        new ExceptionMechanismException(mechanism, throwable, Thread.currentThread());
    final @NotNull SentryEvent event = new SentryEvent(mechanismException);
    event.setLevel(SentryLevel.FATAL);

    scopes.captureEvent(event);
  }

  public void markFailed() {
    transaction.setStatus(SpanStatus.UNKNOWN_ERROR);
  }

  public void close() {
    if (!transaction.isFinished()) {
      endRun();
    }
    token.close();
    Sentry.close();
  }

  public @NotNull List<String> traceCli() {
    final @NotNull List<String> args = new ArrayList<String>();
    final @Nullable SentryTraceHeader traceparent = scopes.getTraceparent();
    if (traceparent != null) {
      args.add("--header");
      args.add(traceparent.getName() + ":" + traceparent.getValue());
    }

    final @Nullable BaggageHeader baggage = scopes.getBaggage();
    if (baggage != null) {
      args.add("--header");
      args.add(baggage.getName() + ":" + baggage.getValue());
    }

    return args;
  }

  public void addTag(final @NotNull String key, final @NotNull String value) {
    scopes.setTag(key, value);
  }

  public static class SentryMinimalException extends RuntimeException {
    public SentryMinimalException(final @NotNull String message) {
      super(message);
    }
  }
}
