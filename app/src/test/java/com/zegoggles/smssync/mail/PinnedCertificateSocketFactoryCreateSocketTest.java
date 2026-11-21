package com.zegoggles.smssync.mail;

import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.zegoggles.smssync.mail.ssl.LoopbackTlsTestServer;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.OTHER_HOST_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.OTHER_HOST_KEY_PKCS8;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_KEY_PKCS8;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_NAME;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.TRUSTED_CA_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.certificate;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.serverContext;
import static org.junit.Assert.fail;

/**
 * Tests for the sockets that {@link PinnedCertificateSocketFactory#createSocket} creates (story U-063,
 * Group E, acceptance criteria AC-18 to AC-21; changed and extended by story U-065). The existing
 * {@code PinnedCertificateSocketFactoryTest} calls the trust manager directly through its package-private
 * seam and never calls {@code createSocket}; these tests check the trust manager that {@code createSocket}
 * actually installs, by running real handshakes, and the socket it returns. Nothing the existing test
 * covers is repeated, and the seam is not used here.
 *
 * <p>The pinned factory's server-name checks (U-063 AC-19, U-065 AC-4) are in
 * {@code DefaultTrustedSocketFactoryJdkProviderTest}: the default test provider reports no server name
 * whatever is set, as mutation M-E2 showed on 2026-11-17. Its cipher suites are also checked against the
 * recorded rule in {@code DefaultTrustedSocketFactoryHardeningTest} (U-065 AC-3 (b)).
 *
 * <p><b>What the code does, and what these tests record.</b> The pinned factory builds a plain TLS context
 * with its own pinning trust manager and returns an unconnected socket or one layered over a plain socket.
 * Since U-065 it gives every socket the default factory's handling by calling the default factory's
 * {@code hardenSocket} and then its {@code setSniHost}: only TLS 1.2 and TLS 1.3 where the provider supports
 * them, the default factory's cipher-suite order and blocked list, and the host as server name for a DNS
 * name. Decided by the product owner on 2026-11-20 (Record 8: nothing older than TLS 1.2, for both
 * factories; Record 9: cipher suites and server name "Same as default"; BUG-019, BUG-020). Until U-065 the
 * pinned path left the provider's defaults untouched, which the two AC-18 tests recorded. It still does not
 * verify the host name (the host given to its constructor is not stored) and ignores the client-certificate
 * alias. Every factory in this class is built with the host {@code imap.right.test}, whatever certificate is
 * enrolled, so that a mutation which compares that host with the presented certificate can be detected.
 * <ul>
 *   <li>AC-18 as changed by U-065 (two tests; U-065 AC-2, AC-3 (a) and AC-5 under this provider): on both
 *       call shapes none of TLSv1, TLSv1.1, SSLv3 and SSLv2Hello is enabled, TLS 1.2 and 1.3 are enabled if
 *       and only if the provider supports them, and the enabled protocols and cipher suites equal, in order,
 *       those of the default factory's socket of the same shape. Preconditions: the provider's own default
 *       socket enables TLS 1.0 and 1.1, and its cipher order is not the default factory's, so neither
 *       assertion can be met by the provider's defaults.</li>
 *   <li>AC-20: a handshake against a server presenting the enrolled certificate is accepted although the
 *       host name asked for ({@code imap.right.test}) is not in that certificate (it is valid for
 *       {@code imap.other.test}). The pin replaces host-name verification. The control assertions are those
 *       of Group B: the client reads the byte the server writes after its own handshake and the server
 *       reports {@code OK}. No host-name verifier is installed.</li>
 *   <li>AC-21: a handshake against a server presenting any other certificate (here a valid chain from the
 *       test CA) is rejected with an {@link SSLException} whose direct cause is a
 *       {@link CertificateException} carrying the text "Pinned certificate mismatch".</li>
 *   <li>U-065 AC-8: against a server limited to TLS 1.2 the handshake completes at TLS 1.2 on both shapes;
 *       against a server limited to TLS 1.1 or to TLS 1.0 the client refuses with an {@link SSLException}
 *       that is not a certificate rejection, and the server does not report {@code OK}. The server presents
 *       the enrolled certificate in all of them, so only the protocol can stop them.</li>
 * </ul>
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>The provider is a desktop test provider, not a device's. What a device enables by default is not
 *       shown.</li>
 *   <li>Every handshake here runs at TLS 1.3, except the U-065 tests whose server is limited to one
 *       version. TLS 1.0 and 1.1 are shown only as refused, under this provider; a downgrade forced by a
 *       third party is not examined.</li>
 *   <li>The rejection test relies on a fresh TLS context for every {@code createSocket} call (the pinned
 *       factory builds a new context each time), because a resumed session skips the certificate check. If
 *       a later change caches a context per factory, the test could stay green while that guarantee
 *       weakens. The same holds for the acceptance test.</li>
 *   <li>The tests call {@code createSocket} directly and do not drive the IMAP connection class. Where a
 *       rejection surfaces (the handshake, not {@code createSocket}) is scope concern SC-1.</li>
 *   <li>An expired enrolled certificate is presented by {@code PinnedCertificateSocketFactoryExpiryTest}
 *       (U-064); a not-yet-valid one by no test. Certificate path validation (RFC 5280) and host-name checks
 *       (RFC 6125 section 6) are deliberately absent from the pinned path; pinning compares a SHA-256
 *       fingerprint.</li>
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063 and U-065 mutation logs).
 * <ul>
 *   <li>AC-18 as changed, both tests: the floor removed from the shared step (M-P1/M-P8); the pinned
 *       protocol handling removed (M-P2) or its cipher handling removed (M-P4); one factory drifting
 *       (M-P6 (i), (ii)); the layered test also: the handling applied to the direct shape only (M-P7).</li>
 *   <li>AC-20: a host-name comparison added to the pinning trust manager (M-E3), or the fingerprint
 *       comparison inverted (M-E4).</li>
 *   <li>AC-21: the fingerprint comparison inverted (M-E4), or the pinning trust manager replaced by an
 *       accept-all manager (M-E5); both re-run by U-065.</li>
 *   <li>U-065 AC-8: the TLS 1.2 tests break under M-E4 (the pin then fails); the refusal tests break when
 *       the floor is removed (M-P1/M-P8) or the pinned protocol handling is removed (M-P2), the STARTTLS
 *       ones also under M-P7.</li>
 * </ul>
 *
 * <p><b>Global state.</b> None is changed. The pinned factory builds its own trust manager, so the vendored
 * default trust manager is neither read nor replaced here, and no host-name verifier is installed. The
 * after-method closes every socket and the server. Every wait is bounded (10 seconds), there are no sleeps
 * and no retries, ports are chosen by the operating system, and only the loopback address object is used.
 *
 * <p><b>After the CR-009 move:</b> a missing field, method or class means this test's seam needs
 * retargeting to the moved class; a failed behavioural assertion is a regression, and the test is
 * never edited to pass.
 */
@RunWith(RobolectricTestRunner.class)
public class PinnedCertificateSocketFactoryCreateSocketTest {

    private static final String PIN_MARKER = "Pinned certificate mismatch";

    private LoopbackTlsTestServer server;
    private SSLSocket client;
    private Socket plainClient;
    private Socket plainAccepted;
    private ServerSocket listener;

    @After
    public void tearDown() {
        closeQuietly(client);
        closeQuietly(plainClient);
        closeQuietly(plainAccepted);
        closeQuietly(listener);
        if (server != null) {
            server.close();
        }
    }

    /**
     * U-065 AC-2 (a), (b), AC-3 (a), AC-5 under this provider, implicit shape. Was AC-18 of U-063, which
     * recorded that the pinned path left the provider's defaults untouched (BUG-020); changed by the
     * product owner's decisions of 2026-11-20 (Records 8 and 9).
     */
    @Test
    public void createSocket_implicit_appliesDefaultFactoryProtocolAndCipherHandling() throws Exception {
        SSLSocket reference = defaultSocket();
        SSLSocket defaultFactorySocket = null;
        try {
            List<String> supported = Arrays.asList(reference.getSupportedProtocols());
            assertProviderDefaultsEnableTls10AndTls11(reference);
            client = (SSLSocket) factory().createSocket(null, RIGHT_HOST_NAME, 993, null);
            defaultFactorySocket = (SSLSocket) defaultFactory().createSocket(null, RIGHT_HOST_NAME, 993, null);

            assertCipherOrderIsNotTheProviders(reference, defaultFactorySocket);
            assertProtocolFloor("pinned implicit socket", supported, Arrays.asList(client.getEnabledProtocols()));
            assertSameHandling("implicit", defaultFactorySocket, client);
        } finally {
            closeQuietly(reference);
            closeQuietly(defaultFactorySocket);
        }
    }

    /**
     * U-065 AC-2 (a), (b), AC-3 (a), AC-5 under this provider, layered shape: over a plain loopback
     * connection with one accept and no handshake; the default factory's socket for comparison is layered
     * over a second plain connection. Was the layered AC-18 test of U-063.
     */
    @Test
    public void createSocket_layered_appliesDefaultFactoryProtocolAndCipherHandling() throws Exception {
        SSLSocket reference = defaultSocket();
        ServerSocket otherListener = null;
        Socket otherPlain = null;
        Socket otherAccepted = null;
        SSLSocket defaultFactorySocket = null;
        try {
            List<String> supported = Arrays.asList(reference.getSupportedProtocols());
            assertProviderDefaultsEnableTls10AndTls11(reference);
            listener = LoopbackTlsTestServer.bindPlainLoopback();
            plainClient = LoopbackTlsTestServer.connectPlain(listener.getLocalPort());
            plainAccepted = listener.accept();
            client = (SSLSocket) factory().createSocket(plainClient, RIGHT_HOST_NAME, listener.getLocalPort(), null);
            otherListener = LoopbackTlsTestServer.bindPlainLoopback();
            otherPlain = LoopbackTlsTestServer.connectPlain(otherListener.getLocalPort());
            otherAccepted = otherListener.accept();
            defaultFactorySocket = (SSLSocket) defaultFactory()
                    .createSocket(otherPlain, RIGHT_HOST_NAME, otherListener.getLocalPort(), null);

            assertCipherOrderIsNotTheProviders(reference, defaultFactorySocket);
            assertProtocolFloor("pinned layered socket", supported, Arrays.asList(client.getEnabledProtocols()));
            assertSameHandling("layered", defaultFactorySocket, client);
        } finally {
            closeQuietly(reference);
            closeQuietly(defaultFactorySocket);
            closeQuietly(otherPlain);
            closeQuietly(otherAccepted);
            closeQuietly(otherListener);
        }
    }

    /**
     * AC-20: the enrolled certificate is accepted although its names do not include the host asked for. A
     * control: the byte is read and the server reports OK. Breaks when M-E3 or M-E4 is applied.
     */
    @Test
    public void handshake_enrolledCertificateForDifferentHostName_isAccepted() throws Exception {
        server = LoopbackTlsTestServer.start(LoopbackTlsTestServer.Mode.IMPLICIT_TLS,
                serverContext(OTHER_HOST_CERT, OTHER_HOST_KEY_PKCS8, TRUSTED_CA_CERT));
        client = (SSLSocket) factory().createSocket(null, RIGHT_HOST_NAME, server.port(), null);
        LoopbackTlsTestServer.connect(client, server.port());

        client.startHandshake();
        int data = client.getInputStream().read();

        assertWithMessage("the byte the server writes after the handshake").that(data)
                .isEqualTo(LoopbackTlsTestServer.PAYLOAD);
        assertWithMessage("server outcome").that(server.awaitOutcome()).isEqualTo("OK");
    }

    /**
     * AC-21: a certificate other than the enrolled one is rejected, although its chain is valid for the
     * host asked for, so only the pin can fail. The server's outcome is not asserted. Breaks when M-E4 or
     * M-E5 is applied.
     */
    @Test
    public void handshake_certificateOtherThanEnrolled_isRejected() throws Exception {
        server = LoopbackTlsTestServer.start(LoopbackTlsTestServer.Mode.IMPLICIT_TLS,
                serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT));
        client = (SSLSocket) factory().createSocket(null, RIGHT_HOST_NAME, server.port(), null);
        LoopbackTlsTestServer.connect(client, server.port());

        try {
            client.startHandshake();
            fail("the handshake must be rejected, but it completed");
        } catch (SSLException e) {
            Throwable cause = e.getCause();
            assertWithMessage("direct cause of the SSLException " + e.getClass().getName())
                    .that(cause).isInstanceOf(CertificateException.class);
            assertWithMessage("message of the direct cause").that(cause.getMessage()).contains(PIN_MARKER);
        }
    }

    /** U-065 AC-8 (a), implicit TLS: a server limited to TLS 1.2 presenting the enrolled certificate. */
    @Test
    public void handshake_implicitTls_serverLimitedToTls12_completesAtTls12() throws Exception {
        assertPinnedHandshakeCompletesAt(LoopbackTlsTestServer.Mode.IMPLICIT_TLS, "TLSv1.2");
    }

    /** U-065 AC-8 (a), STARTTLS. */
    @Test
    public void handshake_startTls_serverLimitedToTls12_completesAtTls12() throws Exception {
        assertPinnedHandshakeCompletesAt(LoopbackTlsTestServer.Mode.STARTTLS, "TLSv1.2");
    }

    /** U-065 AC-8 (b), implicit TLS, TLS 1.1. Control: M-P1/M-P8 and M-P2 make it complete. */
    @Test
    public void handshake_implicitTls_serverLimitedToTls11_isRefused() throws Exception {
        assertPinnedHandshakeRefused(LoopbackTlsTestServer.Mode.IMPLICIT_TLS, "TLSv1.1");
    }

    /** U-065 AC-8 (b), STARTTLS, TLS 1.1. */
    @Test
    public void handshake_startTls_serverLimitedToTls11_isRefused() throws Exception {
        assertPinnedHandshakeRefused(LoopbackTlsTestServer.Mode.STARTTLS, "TLSv1.1");
    }

    /** U-065 AC-8 (b), implicit TLS, TLS 1.0. */
    @Test
    public void handshake_implicitTls_serverLimitedToTls10_isRefused() throws Exception {
        assertPinnedHandshakeRefused(LoopbackTlsTestServer.Mode.IMPLICIT_TLS, "TLSv1");
    }

    /** U-065 AC-8 (b), STARTTLS, TLS 1.0. */
    @Test
    public void handshake_startTls_serverLimitedToTls10_isRefused() throws Exception {
        assertPinnedHandshakeRefused(LoopbackTlsTestServer.Mode.STARTTLS, "TLSv1");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** The pinned factory, always built with the host the client asks for; the other-host leaf is enrolled. */
    private static PinnedCertificateSocketFactory factory() throws Exception {
        return new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(), RIGHT_HOST_NAME,
                certificate(OTHER_HOST_CERT));
    }

    /** An unconnected socket from a fresh default TLS context, untouched by any factory. */
    private static SSLSocket defaultSocket() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        return (SSLSocket) context.getSocketFactory().createSocket();
    }

    private static final String[] ALLOWED_PROTOCOLS_BASELINE = {"TLSv1.2", "TLSv1.3"};
    private static final String[] OLD_PROTOCOLS = {"TLSv1", "TLSv1.1", "SSLv3", "SSLv2Hello"};

    private static DefaultTrustedSocketFactory defaultFactory() {
        return new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication());
    }

    /** Creates the pinned client in the call shape of the IMAP code; the enrolled leaf's server must run. */
    private SSLSocket connectPinned(LoopbackTlsTestServer.Mode mode) throws Exception {
        int port = server.port();
        if (mode == LoopbackTlsTestServer.Mode.IMPLICIT_TLS) {
            client = (SSLSocket) factory().createSocket(null, RIGHT_HOST_NAME, port, null);
            LoopbackTlsTestServer.connect(client, port);
        } else {
            plainClient = LoopbackTlsTestServer.connectPlain(port);
            assertThat(LoopbackTlsTestServer.readLine(plainClient)).isEqualTo(LoopbackTlsTestServer.GREETING);
            client = (SSLSocket) factory().createSocket(plainClient, RIGHT_HOST_NAME, port, null);
        }
        return client;
    }

    private void assertPinnedHandshakeCompletesAt(LoopbackTlsTestServer.Mode mode, String protocol)
            throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(OTHER_HOST_CERT, OTHER_HOST_KEY_PKCS8, TRUSTED_CA_CERT), new String[] {protocol});
        SSLSocket ssl = connectPinned(mode);

        ssl.startHandshake();
        int data = ssl.getInputStream().read();

        assertWithMessage("the byte the server writes after the handshake").that(data)
                .isEqualTo(LoopbackTlsTestServer.PAYLOAD);
        assertWithMessage("negotiated protocol").that(ssl.getSession().getProtocol()).isEqualTo(protocol);
        assertWithMessage("server outcome").that(server.awaitOutcome()).isEqualTo("OK");
    }

    private void assertPinnedHandshakeRefused(LoopbackTlsTestServer.Mode mode, String protocol)
            throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(OTHER_HOST_CERT, OTHER_HOST_KEY_PKCS8, TRUSTED_CA_CERT), new String[] {protocol});
        SSLSocket ssl = connectPinned(mode);

        try {
            ssl.startHandshake();
            fail("the handshake against a server limited to " + protocol + " must be refused, but it"
                    + " completed at " + ssl.getSession().getProtocol());
        } catch (SSLException e) {
            assertWithMessage("the refusal must not be a certificate rejection; direct cause="
                    + e.getCause()).that(e.getCause() instanceof CertificateException).isFalse();
            assertWithMessage("server outcome against a client that refused " + protocol)
                    .that(server.awaitOutcome()).isNotEqualTo("OK");
        }
    }

    private static void assertProviderDefaultsEnableTls10AndTls11(SSLSocket reference) {
        List<String> enabled = Arrays.asList(reference.getEnabledProtocols());
        for (String name : Arrays.asList("TLSv1", "TLSv1.1")) {
            assertWithMessage("precondition AC-2 (b): the provider's default socket must enable " + name
                    + ", so that its absence on the pinned socket is the factory's doing; default enabled="
                    + enabled).that(enabled).contains(name);
        }
    }

    private static void assertCipherOrderIsNotTheProviders(SSLSocket reference, SSLSocket defaultFactorySocket) {
        assertWithMessage("precondition AC-3 (a): the default factory's cipher order must differ from the"
                + " provider's own, so that parity cannot hold on the provider's defaults")
                .that(Arrays.asList(defaultFactorySocket.getEnabledCipherSuites()))
                .isNotEqualTo(Arrays.asList(reference.getEnabledCipherSuites()));
    }

    private static void assertProtocolFloor(String label, List<String> supported, List<String> enabled) {
        for (String old : OLD_PROTOCOLS) {
            assertWithMessage(label + ": " + old + " must not be enabled; enabled=" + enabled)
                    .that(enabled).doesNotContain(old);
        }
        for (String modern : ALLOWED_PROTOCOLS_BASELINE) {
            assertWithMessage(label + ": " + modern + " enabled must equal " + modern + " supported;"
                    + " supported=" + supported + ", enabled=" + enabled)
                    .that(enabled.contains(modern)).isEqualTo(supported.contains(modern));
        }
        Set<String> expected = new HashSet<>(supported);
        expected.retainAll(Arrays.asList(ALLOWED_PROTOCOLS_BASELINE));
        assertWithMessage(label + ": the enabled set must be the supported protocols that are TLS 1.2 or"
                + " newer").that(new HashSet<>(enabled)).isEqualTo(expected);
    }

    private static void assertSameHandling(String shape, SSLSocket reference, SSLSocket candidate) {
        List<String> expectedProtocols = Arrays.asList(reference.getEnabledProtocols());
        List<String> actualProtocols = Arrays.asList(candidate.getEnabledProtocols());
        assertWithMessage(shape + ": enabled protocols as a set")
                .that(new HashSet<>(actualProtocols)).isEqualTo(new HashSet<>(expectedProtocols));
        assertWithMessage(shape + ": enabled protocols in order, against the default factory")
                .that(actualProtocols).containsExactlyElementsIn(expectedProtocols).inOrder();
        assertWithMessage(shape + ": enabled cipher suites in order, against the default factory")
                .that(Arrays.asList(candidate.getEnabledCipherSuites()))
                .containsExactlyElementsIn(Arrays.asList(reference.getEnabledCipherSuites())).inOrder();
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // nothing to do for a test socket
            }
        }
    }
}
