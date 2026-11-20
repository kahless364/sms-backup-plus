package com.zegoggles.smssync.mail;

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
 * Characterisation tests for the sockets that {@link PinnedCertificateSocketFactory#createSocket} creates
 * (story U-063, Group E, acceptance criteria AC-18 to AC-21). The existing
 * {@code PinnedCertificateSocketFactoryTest} calls the trust manager directly through its package-private
 * seam and never calls {@code createSocket}; these tests check the trust manager that {@code createSocket}
 * actually installs, by running real handshakes, and the socket it returns. Nothing the existing test
 * covers is repeated, and the seam is not used here.
 *
 * <p>AC-19 (the pinned factory sets no server name) is tested in
 * {@code DefaultTrustedSocketFactoryJdkProviderTest}, not here. The default test provider reports no server
 * name whatever is set, as mutation M-E2 showed on 2026-11-17: a pinned factory made to set a name still
 * passed the test. This class therefore holds AC-18, AC-20 and AC-21 (4 tests).
 *
 * <p><b>What the code does, and what these tests record.</b> The pinned factory builds a plain TLS context
 * with its own pinning trust manager and returns an unconnected socket or one layered over a plain socket.
 * It calls neither the SNI setter nor any cipher or protocol hardening, it does not verify the host name
 * (the host given to its constructor is not stored), and it ignores the client-certificate alias. Every
 * factory in this class is built with the host {@code imap.right.test}, whatever certificate is enrolled, so
 * that a mutation which compares that host with the presented certificate can be detected.
 * <ul>
 *   <li>AC-18 (two tests): on both call shapes the enabled cipher suites and protocols equal, in order,
 *       those of an unhardened socket from a fresh default TLS context. This records that the pinned path
 *       does <em>not</em> harden, unlike the default factory. It is present behaviour, not an approved
 *       design; whether it should change is a separate matter and is not decided here.</li>
 *   <li>AC-20: a handshake against a server presenting the enrolled certificate is accepted although the
 *       host name asked for ({@code imap.right.test}) is not in that certificate (it is valid for
 *       {@code imap.other.test}). The pin replaces host-name verification. The control assertions are those
 *       of Group B: the client reads the byte the server writes after its own handshake and the server
 *       reports {@code OK}. No host-name verifier is installed.</li>
 *   <li>AC-21: a handshake against a server presenting any other certificate (here a valid chain from the
 *       test CA) is rejected with an {@link SSLException} whose direct cause is a
 *       {@link CertificateException} carrying the text "Pinned certificate mismatch".</li>
 * </ul>
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>The provider is a desktop test provider, not a device's. The enabled lists compared in AC-18 are
 *       the provider's defaults here; what a device enables by default is not shown.</li>
 *   <li>Every handshake here ran at TLS 1.3 (environment probe). Nothing is shown about TLS 1.2 or earlier.</li>
 *   <li>The rejection test relies on a fresh TLS context for every {@code createSocket} call (the pinned
 *       factory builds a new context each time), because a resumed session skips the certificate check. If
 *       a later change caches a context per factory, the test could stay green while that guarantee
 *       weakens. The same holds for the acceptance test.</li>
 *   <li>The tests call {@code createSocket} directly and do not drive the IMAP connection class. Where a
 *       rejection surfaces (the handshake, not {@code createSocket}) is scope concern SC-1.</li>
 *   <li>An expired enrolled certificate is not presented to the pinning trust manager by any test (scope
 *       concern SC-6, a follow-up). Certificate path validation (RFC 5280) and host-name checks (RFC 6125
 *       section 6) are deliberately absent from the pinned path; pinning compares a SHA-256 fingerprint.</li>
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063 mutation log).
 * <ul>
 *   <li>AC-18, both tests: a protocol or cipher call added to the pinned {@code createSocket} (M-E1).</li>
 *   <li>AC-20: a host-name comparison added to the pinning trust manager (M-E3), or the fingerprint
 *       comparison inverted (M-E4).</li>
 *   <li>AC-21: the fingerprint comparison inverted (M-E4), or the pinning trust manager replaced by an
 *       accept-all manager (M-E5).</li>
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

    /** AC-18, implicit shape. Breaks when M-E1 is applied. */
    @Test
    public void createSocket_implicit_leavesProviderDefaultsUntouched() throws Exception {
        SSLSocket reference = defaultSocket();
        try {
            client = (SSLSocket) factory().createSocket(null, RIGHT_HOST_NAME, 993, null);

            assertThat(client.getEnabledCipherSuites()).isEqualTo(reference.getEnabledCipherSuites());
            assertThat(client.getEnabledProtocols()).isEqualTo(reference.getEnabledProtocols());
        } finally {
            closeQuietly(reference);
        }
    }

    /**
     * AC-18, layered shape: over a plain loopback connection with one accept and no handshake. Breaks when
     * M-E1 is applied.
     */
    @Test
    public void createSocket_layered_leavesProviderDefaultsUntouched() throws Exception {
        SSLSocket reference = defaultSocket();
        try {
            listener = LoopbackTlsTestServer.bindPlainLoopback();
            plainClient = LoopbackTlsTestServer.connectPlain(listener.getLocalPort());
            plainAccepted = listener.accept();
            client = (SSLSocket) factory().createSocket(plainClient, RIGHT_HOST_NAME, listener.getLocalPort(), null);

            assertThat(client.getEnabledCipherSuites()).isEqualTo(reference.getEnabledCipherSuites());
            assertThat(client.getEnabledProtocols()).isEqualTo(reference.getEnabledProtocols());
        } finally {
            closeQuietly(reference);
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
