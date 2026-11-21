package com.zegoggles.smssync.mail;

import com.zegoggles.smssync.mail.ssl.LoopbackTlsTestServer;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.EXPIRED_HOST_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.EXPIRED_HOST_KEY_PKCS8;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_NAME;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.TRUSTED_CA_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.certificate;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.serverContext;
import static org.junit.Assert.fail;

/**
 * Gap test for the pinned factory's validity-window check (story U-064, acceptance criterion AC-1; the gap
 * is U-063 security review finding W-1 and scope concern SC-6). It adds one test to the baseline of
 * {@code PinnedCertificateSocketFactoryCreateSocketTest} and {@code PinnedCertificateSocketFactoryTest}
 * and repeats nothing of them. The seam {@code getTrustManagerForTesting()} is not used.
 *
 * <p><b>What it proves.</b> Through the trust manager that {@code createSocket} installs, a server that
 * presents exactly the enrolled leaf passes the fingerprint step (the SHA-256 fingerprints are equal) and
 * is then rejected by the validity step, because the enrolled certificate has expired. The handshake
 * fails with an {@link SSLException} whose direct cause is a {@link CertificateExpiredException}, and the
 * message of that cause does not carry the text "Pinned certificate mismatch", which shows that the
 * fingerprint step was passed and the validity step failed. The behaviour is required by the producer table
 * and the error table of CNTR-MODERNIZATION-001 ("validity-window check"; RFC 5280 section 4.1.2.5).
 *
 * <p><b>What it cannot prove.</b>
 * <ul>
 *   <li>A device's TLS provider; the provider here is a desktop test provider.</li>
 *   <li>A certificate that is not yet valid. That case was left out on purpose (decision point DP-1 of the
 *       story, decided by the product owner on 2026-11-20): every fixture starts in the past, and a new
 *       key and certificate would be needed.</li>
 *   <li>The device clock. The check reads the clock at connection time. The fixture expired in January
 *       2020, so the test depends on the machine clock being later than that, and says so in a
 *       precondition message if it is not.</li>
 *   <li>Chain and host-name checks, which the pinned path deliberately omits.</li>
 *   <li>Anything below TLS 1.3 (U-063 environment probe).</li>
 *   <li>It relies on a fresh TLS context for every {@code createSocket} call (the pinned factory builds a
 *       new context each time), because a resumed session would skip the trust manager.</li>
 * </ul>
 *
 * <p><b>Mutation that must break it.</b> M-N1: the call {@code enrolledCert.checkValidity()} in
 * {@code PinnedCertificateSocketFactory} disabled. The handshake then completes and the test fails with
 * "the handshake must be rejected, but it completed". A fingerprint comparison that is inverted also breaks
 * it (the direct cause or its message).
 *
 * <p><b>Record correction.</b> The sentence in the Javadoc of
 * {@code PinnedCertificateSocketFactoryCreateSocketTest} that says no test presents an expired enrolled
 * certificate (scope concern SC-6 of U-063) is superseded by this class. It is left unedited under the
 * additions-only rule of U-064 (its scope concern SC-2).
 *
 * <p><b>Global state.</b> None is changed. No host-name verifier is installed and the vendored default
 * trust manager is not touched. The after-method closes the socket and the server. Every wait is bounded
 * (10 seconds) by the helper, there are no sleeps and no retries, the port is chosen by the operating
 * system, and only the loopback address object is used.
 *
 * <p><b>After the CR-009 move:</b> a missing field, method or class means this test's seam needs
 * retargeting to the moved class; a failed behavioural assertion is a regression, and the test is
 * never edited to pass.
 */
@RunWith(RobolectricTestRunner.class)
public class PinnedCertificateSocketFactoryExpiryTest {

    private static final String PIN_MARKER = "Pinned certificate mismatch";

    private LoopbackTlsTestServer server;
    private SSLSocket client;

    @After
    public void tearDown() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException ignored) {
                // nothing to do for a test socket
            }
        }
        if (server != null) {
            server.close();
        }
    }

    /**
     * AC-1. A server presenting the enrolled but expired leaf is rejected at the handshake as expired, not
     * as a fingerprint mismatch. Breaks when M-N1 is applied. The server's own outcome is not asserted (it
     * may hang up or reset after the client rejects).
     */
    @Test
    public void handshake_expiredEnrolledCertificate_isRejectedAsExpired() throws Exception {
        X509Certificate enrolled = certificate(EXPIRED_HOST_CERT);
        assertWithMessage("precondition: the enrolled fixture's end date lies before the machine clock")
                .that(enrolled.getNotAfter().before(new Date())).isTrue();

        server = LoopbackTlsTestServer.start(LoopbackTlsTestServer.Mode.IMPLICIT_TLS,
                serverContext(EXPIRED_HOST_CERT, EXPIRED_HOST_KEY_PKCS8, TRUSTED_CA_CERT));
        client = (SSLSocket) new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(),
                RIGHT_HOST_NAME, enrolled).createSocket(null, RIGHT_HOST_NAME, server.port(), null);
        LoopbackTlsTestServer.connect(client, server.port());

        try {
            client.startHandshake();
            fail("the handshake must be rejected, but it completed");
        } catch (SSLException e) {
            Throwable cause = e.getCause();
            assertWithMessage("direct cause of the SSLException; cause chain: " + causeChain(e))
                    .that(cause).isInstanceOf(CertificateExpiredException.class);
            assertWithMessage("message of the direct cause")
                    .that(String.valueOf(cause.getMessage())).doesNotContain(PIN_MARKER);
        }
    }

    /** Class names of the cause chain, outermost first, at most 10 entries (for the failure message only). */
    private static String causeChain(Throwable t) {
        StringBuilder out = new StringBuilder();
        Throwable current = t;
        for (int i = 0; i < 10 && current != null; i++) {
            if (i > 0) {
                out.append(" <- ");
            }
            out.append(current.getClass().getName());
            Throwable next = current.getCause();
            current = (next == current) ? null : next;
        }
        return out.toString();
    }
}
