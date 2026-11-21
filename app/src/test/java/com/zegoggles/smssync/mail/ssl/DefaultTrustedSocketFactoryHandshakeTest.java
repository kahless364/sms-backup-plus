package com.zegoggles.smssync.mail.ssl;

import com.fsck.k9.mail.CertificateChainException;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustManagerFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509TrustManager;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.zegoggles.smssync.mail.ssl.LoopbackTlsTestServer.Mode.IMPLICIT_TLS;
import static com.zegoggles.smssync.mail.ssl.LoopbackTlsTestServer.Mode.STARTTLS;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.EXPIRED_HOST_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.EXPIRED_HOST_KEY_PKCS8;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.OTHER_HOST_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.OTHER_HOST_KEY_PKCS8;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_KEY_PKCS8;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_NAME;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.TRUSTED_CA_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.UNTRUSTED_CA_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.UNTRUSTED_HOST_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.UNTRUSTED_HOST_KEY_PKCS8;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.serverContext;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.trustManagerFor;
import static org.junit.Assert.fail;

/**
 * Characterisation tests for what {@link DefaultTrustedSocketFactory} does with a server certificate
 * (story U-063, Groups B and C, acceptance criteria AC-5 to AC-10). They run real TLS handshakes over
 * the loopback interface between a client socket made by {@code createSocket} and
 * {@link LoopbackTlsTestServer}, with the vendored {@code TrustManagerFactory} validating against a test
 * CA. Under Robolectric's default provider (Conscrypt at the environment probe) the host-name check is
 * Robolectric's own verifier, which accepts a name present in the certificate's subject alternative
 * names and rejects another; no verifier is swapped here.
 *
 * <p><b>What they prove.</b>
 * <ul>
 *   <li>Controls (AC-5, AC-7): a chain for {@code imap.right.test} signed by the test CA is accepted, on
 *       both call shapes, and data the server writes after its own handshake reaches the client. A control
 *       passes only if the client handshake completes, the client reads that byte and the server reports
 *       {@code OK}. They stop the rejection tests from passing for the wrong reason.</li>
 *   <li>Host name (AC-6, AC-8): a valid chain for {@code imap.other.test} is rejected when the client asks
 *       for {@code imap.right.test}; the cause chain carries the text "was not verified against the
 *       server certificate", which only the host-name check produces (RFC 6125 section 6).</li>
 *   <li>Untrusted and expired chains (AC-9, AC-10): a chain for the right host signed by a CA that is not a
 *       trust anchor, and a chain for the right host signed by the trusted CA but expired, are rejected
 *       without that text, so the host-name check cannot be the cause (RFC 5280, certificate path
 *       validation).</li>
 *   <li>In every rejection the handshake throws an {@link SSLException} whose direct cause is a
 *       {@link CertificateException} that is a {@link CertificateChainException}. That is the shape the IMAP
 *       connection code needs to report a certificate error rather than a plain I/O error. Nothing below
 *       {@code CertificateException} is asserted, and the server's outcome is not asserted in a rejection.</li>
 *   <li>Protocol floor (U-065 AC-8; BUG-019; Record 8 of 2026-11-20): against a server limited to TLS 1.2
 *       the handshake completes at TLS 1.2 on both call shapes (the first handshakes below TLS 1.3 in these
 *       tests); against a server limited to TLS 1.1 or to TLS 1.0 the client refuses with an
 *       {@link SSLException} that is not a certificate rejection, and the server does not report
 *       {@code OK}. The server presents the right-host chain in all of them, so only the protocol can stop
 *       them.</li>
 *   <li>Cipher suites (U-066; F-1 of the U-065 security review; Record 11): the TLS 1.2 handshakes negotiate a
 *       TLS 1.2 suite of the allowed list (ECDHE with AES-GCM or ChaCha20-Poly1305); against a server that limits
 *       nothing the handshake negotiates TLS 1.3 with a TLS 1.3 suite; against a server limited to TLS 1.2 and to
 *       one pruned suite (ECDHE with CBC and SHA-1, or RSA key exchange with GCM) the handshake fails with an
 *       {@link SSLException} that is not a certificate rejection, and the server does not report {@code OK}.</li>
 * </ul>
 * The two call shapes are those of the IMAP connection code: implicit TLS ({@code createSocket(null, host,
 * port, null)}, then connect and handshake) and STARTTLS ({@code createSocket(plainSocket, host, port,
 * null)} after a plain greeting, then handshake).
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>The provider is a desktop test provider, not a device's. What a device's TLS stack accepts or
 *       rejects is not shown.</li>
 *   <li>Every handshake here ran at TLS 1.3 (environment probe), except the U-065 tests whose server is
 *       limited to one version. TLS 1.0 and 1.1 are shown only as refused, under this provider.</li>
 *   <li>The rejection tests rely on a fresh TLS context for every {@code createSocket} call (the factory
 *       builds a new context each time), because a resumed session skips the certificate check. If a later
 *       change caches a context per factory, these tests could stay green while that guarantee weakens.</li>
 *   <li>The tests call {@code createSocket} directly. They do not drive the IMAP connection class, so they do
 *       not show that it calls the factory on both paths or how it maps the exception (out of scope).</li>
 *   <li>Where a rejection surfaces: the exception comes from the handshake, which the IMAP code triggers on
 *       its first read, not from {@code createSocket} itself (scope concern SC-1, which concerns the wording
 *       of the contract). The tests follow the code.</li>
 *   <li>The trust anchor is a test CA installed in place of the platform's trust manager (see below), and
 *       the certificates are throwaway fixtures; nothing is shown about real certificate authorities.</li>
 *   <li>The local key-store fallback of the trust manager is not exercised (it holds no entry).</li>
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063 mutation log).
 * <ul>
 *   <li>AC-5 and AC-7 (controls): the host-name call given the wrong host (M-B3), the peer-certificate
 *       array of the session shim returned empty (M-B4); AC-7 also: the layered branch replaced by returning
 *       the plain socket (M-B5).</li>
 *   <li>AC-6 and AC-8 (other host): the host-name call removed (M-B1), the trust manager replaced by an
 *       accept-all manager (M-B2), the fallback made to accept (M-C2); AC-8 also M-B5.</li>
 *   <li>AC-9 and AC-10 (untrusted, expired; both call shapes): the chain validation call removed (M-C1), the
 *       accept-all manager (M-B2), the fallback made to accept (M-C2); the STARTTLS tests also M-B5.</li>
 *   <li>U-065 AC-8: the refusal tests break when the TLS 1.2 floor is removed (M-P1/M-P8) or the old list
 *       is given back to the default factory alone (M-P6 (ii)); the TLS 1.2 tests are controls that keep
 *       passing under those mutations.</li>
 *   <li>U-066 AC-6: the refusal tests break when a pruned suite is put back (M-X1 (a) or (b), each for its own
 *       suite), the allow filter is removed (M-X2), the default factory's step is removed (M-X5 (ii)), or, for the
 *       STARTTLS ones, the layered shape is skipped (M-X6). The TLS 1.3 test is a control.</li>
 * </ul>
 *
 * <p><b>Global state.</b> The only JVM-wide state this class touches is the private static default trust
 * manager of the vendored {@code TrustManagerFactory}. The before-method replaces it with a validating
 * trust manager whose only anchor is the test CA (never a mock and never an accept-all manager); the
 * after-method puts the original back and asserts that it is the same instance. The factory's cached
 * per-host trust managers are not restored: each reads the static field on every call, so they hold no
 * trust decision. The default host-name verifier is not replaced; the before-method fails if the current
 * one looks like a leftover from a test, and the after-method asserts it is unchanged.
 *
 * <p><b>Isolation guard A1-G2.</b> The before-method fails, naming A1-G2, if the TLS context's provider is
 * not Conscrypt, which would mean the JDK-provider class's Robolectric sandbox was reused here. Until U-066
 * the guard read this from the factory's static cipher list (a DHE suite appeared only under the JDK
 * provider); since U-066 both providers give the same ten allowed suites, so it checks the live provider
 * (U-066 AC-8). The SSLv2Hello clause can no longer fire since U-065 and is kept unchanged.
 *
 * <p><b>Time bounds.</b> Every wait is bounded (10 seconds: connect, reads, accept and the wait for the
 * server). There are no sleeps and no retries. Ports are chosen by the operating system and only the
 * loopback address object is used, so no name is resolved.
 *
 * <p><b>After the CR-009 move:</b> a missing field, method or class means this test's seam needs
 * retargeting to the moved class; a failed behavioural assertion is a regression, and the test is
 * never edited to pass.
 */
@RunWith(RobolectricTestRunner.class)
public class DefaultTrustedSocketFactoryHandshakeTest {

    private static final String HOST_MARKER = "was not verified against the server certificate";
    private static final int ENTRIES_IN_MESSAGES = 5;
    private static final int MAX_CAUSE_STEPS = 20;

    /** The TLS 1.3 suites of the factory's allowed list (U-066). */
    private static final List<String> TLS13_SUITES = Arrays.asList(
            "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256");

    /** The TLS 1.2 suites of the factory's allowed list (U-066): ECDHE with AES-GCM or ChaCha20-Poly1305. */
    private static final List<String> AEAD12_SUITES = Arrays.asList(
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256");

    private Field trustManagerField;
    private X509TrustManager originalTrustManager;
    private HostnameVerifier originalVerifier;
    private boolean substituted;

    private LoopbackTlsTestServer server;
    private Socket plain;
    private SSLSocket client;

    /**
     * Isolation guard A1-G2, the leak guards, then the substitution of the default trust manager by the
     * test-CA validating trust manager.
     */
    @Before
    public void setUp() throws Exception {
        checkIsolationGuardA1G2();

        trustManagerField = TrustManagerFactory.class.getDeclaredField("defaultTrustManager");
        trustManagerField.setAccessible(true);
        originalTrustManager = (X509TrustManager) trustManagerField.get(null);
        originalVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

        if (originalTrustManager == null) {
            fail("TrustManagerFactory.defaultTrustManager is null: production static init failed; "
                    + "contradicts probe P8 (stop and report)");
        }
        if (originalVerifier == null) {
            fail("HttpsURLConnection default host-name verifier is null; contradicts the environment "
                    + "probe (stop and report)");
        }
        for (X509Certificate issuer : originalTrustManager.getAcceptedIssuers()) {
            String subject = issuer.getSubjectX500Principal().getName();
            if (subject.contains("U-063")) {
                fail("leak guard: the current default trust manager trusts a U-063 test CA ("
                        + subject + "); a test left its substitute in place");
            }
        }
        String verifierName = originalVerifier.getClass().getName();
        if (verifierName.startsWith("com.zegoggles.smssync.mail.ssl.")) {
            fail("leak guard: the current default host-name verifier is a test class: " + verifierName);
        }

        trustManagerField.set(null, trustManagerFor(TRUSTED_CA_CERT));
        substituted = true;
    }

    @After
    public void tearDown() throws Exception {
        try {
            closeQuietly(client);
            closeQuietly(plain);
            if (server != null) {
                server.close();
            }
        } finally {
            if (substituted) {
                try {
                    trustManagerField.set(null, originalTrustManager);
                } finally {
                    assertThat(trustManagerField.get(null)).isSameInstanceAs(originalTrustManager);
                    assertThat(HttpsURLConnection.getDefaultHostnameVerifier())
                            .isSameInstanceAs(originalVerifier);
                }
            }
        }
    }

    /**
     * AC-5 (implicit TLS, control): the chain for the right host, signed by the trusted CA, is accepted.
     * Breaks when M-B3 or M-B4 is applied.
     */
    @Test
    public void implicitTls_rightHost_handshakeCompletes() throws Exception {
        assertControl(IMPLICIT_TLS);
    }

    /**
     * AC-6 (implicit TLS, mismatch): the chain for the other host is rejected by the host-name check.
     * Breaks when M-B1, M-B2 or M-C2 is applied.
     */
    @Test
    public void implicitTls_otherHost_rejectedByHostnameCheck() throws Exception {
        assertRejected(IMPLICIT_TLS, OTHER_HOST_CERT, OTHER_HOST_KEY_PKCS8, TRUSTED_CA_CERT, true);
    }

    /**
     * AC-7 (STARTTLS, control): the plain greeting is read, the same connection is upgraded through the
     * layered call, and the chain for the right host is accepted. Breaks when M-B3, M-B4 or M-B5 is applied.
     */
    @Test
    public void startTls_rightHost_handshakeCompletes() throws Exception {
        assertControl(STARTTLS);
    }

    /**
     * AC-8 (STARTTLS, mismatch): as AC-6 on the layered call. Breaks when M-B1, M-B2, M-B5 or M-C2 is applied.
     */
    @Test
    public void startTls_otherHost_rejectedByHostnameCheck() throws Exception {
        assertRejected(STARTTLS, OTHER_HOST_CERT, OTHER_HOST_KEY_PKCS8, TRUSTED_CA_CERT, true);
    }

    /**
     * AC-9 (untrusted chain, implicit TLS): right host name, signed by a CA that is not a trust anchor,
     * so the host-name check cannot be the cause. Breaks when M-C1, M-B2 or M-C2 is applied.
     */
    @Test
    public void implicitTls_untrustedChain_rejected() throws Exception {
        assertRejected(IMPLICIT_TLS, UNTRUSTED_HOST_CERT, UNTRUSTED_HOST_KEY_PKCS8, UNTRUSTED_CA_CERT, false);
    }

    /**
     * AC-9 (untrusted chain, STARTTLS). Breaks when M-C1, M-B2, M-B5 or M-C2 is applied.
     */
    @Test
    public void startTls_untrustedChain_rejected() throws Exception {
        assertRejected(STARTTLS, UNTRUSTED_HOST_CERT, UNTRUSTED_HOST_KEY_PKCS8, UNTRUSTED_CA_CERT, false);
    }

    /**
     * AC-10 (expired chain, implicit TLS): right host name, signed by the trusted CA, validity ended in
     * the past (the fixture is expired for every later date, so no clock control is needed). Breaks when
     * M-C1, M-B2 or M-C2 is applied.
     */
    @Test
    public void implicitTls_expiredChain_rejected() throws Exception {
        assertRejected(IMPLICIT_TLS, EXPIRED_HOST_CERT, EXPIRED_HOST_KEY_PKCS8, TRUSTED_CA_CERT, false);
    }

    /**
     * AC-10 (expired chain, STARTTLS). Breaks when M-C1, M-B2, M-B5 or M-C2 is applied.
     */
    @Test
    public void startTls_expiredChain_rejected() throws Exception {
        assertRejected(STARTTLS, EXPIRED_HOST_CERT, EXPIRED_HOST_KEY_PKCS8, TRUSTED_CA_CERT, false);
    }

    /**
     * U-065 AC-8 (a), implicit TLS: against a server limited to TLS 1.2 the handshake completes at TLS 1.2,
     * the client reads the byte the server writes, and the server reports OK. It shows that the TLS 1.2
     * floor does not exclude TLS 1.2.
     */
    @Test
    public void implicitTls_serverLimitedToTls12_handshakeCompletesAtTls12() throws Exception {
        assertCompletesAt(IMPLICIT_TLS, "TLSv1.2");
    }

    /** U-065 AC-8 (a), STARTTLS: as the implicit test, on the layered call. */
    @Test
    public void startTls_serverLimitedToTls12_handshakeCompletesAtTls12() throws Exception {
        assertCompletesAt(STARTTLS, "TLSv1.2");
    }

    /**
     * U-065 AC-8 (b), implicit TLS: against a server limited to TLS 1.1 the client's handshake throws an
     * SSLException that is not a certificate rejection, and the server does not report OK. Control: with
     * the floor removed (M-P1/M-P8) the same handshake completes (U-065 probe and mutation log).
     */
    @Test
    public void implicitTls_serverLimitedToTls11_handshakeRefused() throws Exception {
        assertRefusedByProtocolFloor(IMPLICIT_TLS, "TLSv1.1");
    }

    /** U-065 AC-8 (b), STARTTLS, TLS 1.1. */
    @Test
    public void startTls_serverLimitedToTls11_handshakeRefused() throws Exception {
        assertRefusedByProtocolFloor(STARTTLS, "TLSv1.1");
    }

    /** U-065 AC-8 (b), implicit TLS, TLS 1.0. */
    @Test
    public void implicitTls_serverLimitedToTls10_handshakeRefused() throws Exception {
        assertRefusedByProtocolFloor(IMPLICIT_TLS, "TLSv1");
    }

    /** U-065 AC-8 (b), STARTTLS, TLS 1.0. */
    @Test
    public void startTls_serverLimitedToTls10_handshakeRefused() throws Exception {
        assertRefusedByProtocolFloor(STARTTLS, "TLSv1");
    }

    /**
     * U-066 AC-5 (b), default factory: against a server that limits nothing, the handshake completes at TLS
     * 1.3 with one of the three TLS 1.3 suites, so the pruning leaves TLS 1.3 its suites. A control: under this
     * provider the TLS 1.3 suites are provider-fixed (DP-4), so no U-066 mutation is expected to break it; the
     * JDK provider class's exact-list tests carry the TLS 1.3 names (M-X4 (i)).
     */
    @Test
    public void implicitTls_unrestrictedServer_negotiatesTls13WithAnAllowedSuite() throws Exception {
        server = LoopbackTlsTestServer.start(IMPLICIT_TLS,
                serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT));
        SSLSocket ssl = connectClient(IMPLICIT_TLS);

        ssl.startHandshake();
        int data = ssl.getInputStream().read();

        assertWithMessage("the byte the server writes after the handshake").that(data)
                .isEqualTo(LoopbackTlsTestServer.PAYLOAD);
        assertWithMessage("negotiated protocol").that(ssl.getSession().getProtocol()).isEqualTo("TLSv1.3");
        assertWithMessage("negotiated suite (U-066 AC-5 (b))").that(TLS13_SUITES)
                .contains(ssl.getSession().getCipherSuite());
        assertWithMessage("server outcome").that(server.awaitOutcome()).isEqualTo("OK");
    }

    /** U-066 AC-6, implicit TLS: a server limited to TLS 1.2 and to ECDHE with AES-CBC and SHA-1 is refused. */
    @Test
    public void implicitTls_serverLimitedToEcdheCbcSha_handshakeRefused() throws Exception {
        assertRefusedByCipherPolicy(IMPLICIT_TLS, "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
    }

    /** U-066 AC-6, STARTTLS, ECDHE with AES-CBC and SHA-1. */
    @Test
    public void startTls_serverLimitedToEcdheCbcSha_handshakeRefused() throws Exception {
        assertRefusedByCipherPolicy(STARTTLS, "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA");
    }

    /** U-066 AC-6, implicit TLS: a server limited to TLS 1.2 and to RSA key exchange with AES-GCM is refused. */
    @Test
    public void implicitTls_serverLimitedToRsaKeyExchangeGcm_handshakeRefused() throws Exception {
        assertRefusedByCipherPolicy(IMPLICIT_TLS, "TLS_RSA_WITH_AES_128_GCM_SHA256");
    }

    /** U-066 AC-6, STARTTLS, RSA key exchange with AES-GCM. */
    @Test
    public void startTls_serverLimitedToRsaKeyExchangeGcm_handshakeRefused() throws Exception {
        assertRefusedByCipherPolicy(STARTTLS, "TLS_RSA_WITH_AES_128_GCM_SHA256");
    }

    /**
     * The right-host chain, a server limited to one protocol; the handshake must complete at it. Since U-066
     * the negotiated suite must be an allowed suite of that protocol.
     */
    private void assertCompletesAt(LoopbackTlsTestServer.Mode mode, String protocol) throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT), new String[] {protocol});
        SSLSocket ssl = connectClient(mode);

        ssl.startHandshake();
        int data = ssl.getInputStream().read();

        assertWithMessage("the byte the server writes after the handshake").that(data)
                .isEqualTo(LoopbackTlsTestServer.PAYLOAD);
        assertWithMessage("negotiated protocol").that(ssl.getSession().getProtocol()).isEqualTo(protocol);
        assertWithMessage("negotiated suite at " + protocol + " (U-066 AC-5 (a))")
                .that("TLSv1.3".equals(protocol) ? TLS13_SUITES : AEAD12_SUITES)
                .contains(ssl.getSession().getCipherSuite());
        assertWithMessage("server outcome").that(server.awaitOutcome()).isEqualTo("OK");
    }

    /** The right-host chain, a server limited to one old protocol; the client must refuse the handshake. */
    private void assertRefusedByProtocolFloor(LoopbackTlsTestServer.Mode mode, String protocol)
            throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT), new String[] {protocol});
        SSLSocket ssl = connectClient(mode);

        try {
            ssl.startHandshake();
            fail("the handshake against a server limited to " + protocol + " must be refused, but it"
                    + " completed at " + ssl.getSession().getProtocol());
        } catch (SSLException e) {
            assertWithMessage("the refusal must not be a certificate rejection; chain=" + describeChain(e))
                    .that(e.getCause() instanceof CertificateException).isFalse();
            assertWithMessage("server outcome against a client that refused " + protocol)
                    .that(server.awaitOutcome()).isNotEqualTo("OK");
        }
    }

    /**
     * U-066 AC-6: the right-host chain, a server limited to TLS 1.2 and to one pruned suite; the handshake must
     * fail, not on the certificate, and the server must not report OK. Control (probe I2 and M-X5 (ii)): a
     * client with the provider's default suites completes the same handshake.
     */
    private void assertRefusedByCipherPolicy(LoopbackTlsTestServer.Mode mode, String prunedSuite)
            throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT),
                new String[] {"TLSv1.2"}, new String[] {prunedSuite});
        SSLSocket ssl = connectClient(mode);

        try {
            ssl.startHandshake();
            fail("the handshake against a server limited to " + prunedSuite + " must be refused, but it"
                    + " completed with " + ssl.getSession().getCipherSuite());
        } catch (SSLException e) {
            assertWithMessage("the refusal must not be a certificate rejection; chain=" + describeChain(e))
                    .that(e.getCause() instanceof CertificateException).isFalse();
            assertWithMessage("server outcome against a client that offers none of the server's suites")
                    .that(server.awaitOutcome()).isNotEqualTo("OK");
        }
    }

    // -----------------------------------------------------------------------
    // Scenario helpers
    // -----------------------------------------------------------------------

    /**
     * Control: the server presents the right-host chain; the client handshake completes, the client
     * reads the byte the server writes after its own handshake, and the server reports OK.
     */
    private void assertControl(LoopbackTlsTestServer.Mode mode) throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT));
        SSLSocket ssl = connectClient(mode);

        ssl.startHandshake();
        int data = ssl.getInputStream().read();

        assertWithMessage("the byte the server writes after the handshake").that(data)
                .isEqualTo(LoopbackTlsTestServer.PAYLOAD);
        assertWithMessage("server outcome").that(server.awaitOutcome()).isEqualTo("OK");
    }

    /**
     * Rejection: the handshake must fail with an SSLException of the exact certificate-check shape. The
     * server's outcome is not asserted.
     */
    private void assertRejected(LoopbackTlsTestServer.Mode mode, String leaf, String key, String issuer,
                                boolean hostnameMarkerExpected) throws Exception {
        server = LoopbackTlsTestServer.start(mode, serverContext(leaf, key, issuer));
        SSLSocket ssl = connectClient(mode);

        try {
            ssl.startHandshake();
            fail("the handshake must be rejected, but it completed");
        } catch (SSLException e) {
            assertRejectedByCertificateCheck(e, hostnameMarkerExpected);
        }
    }

    /**
     * Creates the client socket through the factory in the call shape of the IMAP connection code and
     * returns it, ready for the handshake. The socket is never configured further.
     */
    private SSLSocket connectClient(LoopbackTlsTestServer.Mode mode) throws Exception {
        DefaultTrustedSocketFactory factory = new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication());
        int port = server.port();
        if (mode == IMPLICIT_TLS) {
            client = (SSLSocket) factory.createSocket(null, RIGHT_HOST_NAME, port, null);
            LoopbackTlsTestServer.connect(client, port);
        } else {
            plain = LoopbackTlsTestServer.connectPlain(port);
            assertThat(LoopbackTlsTestServer.readLine(plain)).isEqualTo(LoopbackTlsTestServer.GREETING);
            client = (SSLSocket) factory.createSocket(plain, RIGHT_HOST_NAME, port, null);
        }
        return client;
    }

    /**
     * The direct cause is a CertificateChainException (so also a CertificateException), and the host-name
     * text is in the exception chain if and only if it is expected. The chain is read for at most 20 steps.
     */
    private static void assertRejectedByCertificateCheck(SSLException e, boolean hostnameMarkerExpected) {
        String chain = describeChain(e);
        Throwable cause = e.getCause();
        assertWithMessage("direct cause of the SSLException; chain=" + chain)
                .that(cause).isInstanceOf(CertificateException.class);
        assertWithMessage("direct cause of the SSLException; chain=" + chain)
                .that(cause).isInstanceOf(CertificateChainException.class);

        boolean marker = false;
        Throwable t = e;
        for (int i = 0; i < MAX_CAUSE_STEPS && t != null; i++) {
            if (t.getMessage() != null && t.getMessage().contains(HOST_MARKER)) {
                marker = true;
            }
            t = t.getCause();
        }
        assertWithMessage("host-name text \"" + HOST_MARKER + "\" present in the cause chain=" + chain)
                .that(marker).isEqualTo(hostnameMarkerExpected);
    }

    private static String describeChain(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable t = e;
        for (int i = 0; i < MAX_CAUSE_STEPS && t != null; i++) {
            sb.append(i > 0 ? " > " : "").append(t.getClass().getName());
            t = t.getCause();
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Isolation guard and small helpers
    // -----------------------------------------------------------------------

    /**
     * Isolation guard A1-G2: this class must run on its own provider, Conscrypt. Until U-066 the guard read
     * the provider from the factory's cipher list (a DHE suite meant the JDK provider); since U-066 both
     * providers give the same ten allowed suites, so it checks the TLS context's provider (U-066 AC-8).
     */
    private static void checkIsolationGuardA1G2() throws Exception {
        String[] protocols = constant("ENABLED_PROTOCOLS");
        String[] ciphers = constant("ENABLED_CIPHERS");
        String provider = SSLContext.getInstance("TLS").getProvider().getName();

        if (protocols != null && Arrays.asList(protocols).contains("SSLv2Hello")) {
            fail("A1-G2: the factory's ENABLED_PROTOCOLS contains SSLv2Hello, so it was computed "
                    + "under the JDK provider: TLS context provider=" + provider
                    + ", first entries=" + firstEntries(protocols));
        }
        if (!"Conscrypt".equals(provider)) {
            fail("A1-G2: this class must run on the Conscrypt provider, but the TLS context provider is "
                    + provider + " (the JDK-provider class's sandbox was probably reused); the factory's"
                    + " ENABLED_CIPHERS first entries=" + (ciphers == null ? "null" : firstEntries(ciphers)));
        }
    }

    /** Reads a protected static String[] field of the factory by reflection. */
    private static String[] constant(String name) throws Exception {
        Field field = DefaultTrustedSocketFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        return (String[]) field.get(null);
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

    private static String firstEntries(String[] values) {
        return Arrays.asList(values).subList(0, Math.min(values.length, ENTRIES_IN_MESSAGES)).toString();
    }
}
