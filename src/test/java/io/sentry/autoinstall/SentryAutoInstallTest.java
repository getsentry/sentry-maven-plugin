package io.sentry.autoinstall;

import org.apache.maven.shared.verifier.VerificationException;
import org.apache.maven.shared.verifier.Verifier;
import org.apache.maven.shared.verifier.util.ResourceExtractor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class SentryAutoInstallTest {

    @Test
    public void verifySentryInstalled()
        throws VerificationException, IOException {
        //File file = new File( "src/test/resources/mshared104.jar!fud.xml" );
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/installsentry");

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteDirectory("target");
        verifier.setAutoclean(false);
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyFilePresent( "target/installsentry-1.0.0.jar!/io/sentry/Sentry.class" );
        verifier.resetStreams();
    }

    @Test
    public void verifySentryInstalledAndLog4jInstalled()
        throws VerificationException, IOException {
        //File file = new File( "src/test/resources/mshared104.jar!fud.xml" );
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/installlog4j");

        Verifier verifier = new Verifier( testDir.getAbsolutePath() );
        verifier.deleteDirectory("target");
        verifier.setAutoclean(false);
        verifier.addCliArgument("install");
        verifier.execute();
        verifier.verifyFilePresent( "target/installsentry-1.0.0.jar!/io/sentry/log4j2/SentryAppender.class" );
        verifier.resetStreams();
    }
}
