package io.sentry.samples.maven.extra;
import io.sentry.Sentry;

public class MyClass {
    public static void myFunction() {
        Sentry.captureException(new RuntimeException("Exception thrown in additional sources root"));
    }
}
