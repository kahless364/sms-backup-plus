package com.zegoggles.smssync.mail.ssl;

import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.zegoggles.smssync.mail.PinnedCertificateSocketFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.ConscryptMode;
import org.robolectric.annotation.LooperMode;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.OTHER_HOST_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.certificate;
import static org.junit.Assert.fail;
import static org.robolectric.annotation.ConscryptMode.Mode.OFF;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

/**
 * Characterisation tests for {@link DefaultTrustedSocketFactory} that run on the JDK's own TLS
 * provider (SunJSSE) instead of the Conscrypt provider that Robolectric installs by default
 * (story U-063, acceptance criteria AC-3, AC-4 and the supported-protocols clause of AC-16; changed and
 * extended by story U-065). It also holds the pinned factory's checks that need this provider: under
 * Conscrypt the server-name observable reads nothing whatever is set, so mutation M-E2 was not detected
 * there (product owner decision of 2026-11-20), and only this provider supports old protocols while
 * leaving them off by default.
 *
 * <p><b>Why this class exists.</b> Under Conscrypt, the default test provider, three things cannot
 * be observed. The socket reports no server names before or after a handshake whatever the factory
 * sets (so AC-3 would pass for nothing and AC-4 could not pass). The provider's supported
 * protocols equal its enabled protocols (so building the factory's protocol list from the
 * supported list instead of the enabled list changes nothing). TLS 1.0 and 1.1 are enabled by the
 * provider itself (so the factory's own part in enabling them cannot be told from the provider's).
 * The JDK provider reports the server names, lists SSLv3, TLS 1.0 and TLS 1.1 as supported but
 * leaves the latter two off by default, so all three are observable. The product owner decided on
 * 2026-11-17 that these checks live in one separate class.
 *
 * <p><b>The looper-mode annotation.</b> {@code @LooperMode(LEGACY)} is here only to give this class
 * its own Robolectric sandbox (its own class loader). Robolectric 4.15.1 keys sandboxes on looper
 * mode but not on Conscrypt mode, so without it the factory's static cipher and protocol lists
 * would be shared with the Conscrypt-mode classes and computed under whichever provider ran first.
 * The class uses no looper. Guards A1-G1 to A1-G3 (in the before-method) check this isolation on
 * every run. The annotation is deprecated in this Robolectric version, so the compiler prints a
 * deprecation note for this file; that note is expected.
 *
 * <p><b>What these tests prove.</b> With the JDK provider: the default factory's socket for a DNS host
 * carries exactly that host as its one server name; the socket for an eight-group IPv6 literal carries
 * none and creating it does not throw (RFC 6066 section 3: the server name list holds DNS names, never IP
 * literals). Since U-065 (BUG-019; product owner decision Record 8 of 2026-11-20) the default factory
 * enables exactly the provider's supported protocols that are TLS 1.2 or newer, TLS 1.2 first, and none of
 * TLS 1.0, TLS 1.1, SSLv3 and SSLv2Hello on the direct or the layered socket, although this provider
 * supports all four (until U-065 it enabled TLS 1.0, 1.1 and SSLv2Hello here; RFC 8996 deprecates TLS 1.0
 * and 1.1). For the pinned-certificate factory (BUG-020; Record 9, "Same as default"): its sockets enable
 * exactly TLS 1.2 and 1.3 on both shapes, so it gains no protocol this provider leaves off; its direct
 * socket for a DNS host carries that host as its only server name, with the floor and the default
 * factory's cipher order still in place after that step; for an IPv4-form host, a host with a colon, a
 * null, empty or blank host, and a name SNIHostName rejects it sets no server name and does not throw
 * (statements S2 to S5 of CR-013); and its sockets match the default factory's in protocols, cipher suites
 * and server names (U-065 AC-5). Since U-066 (finding F-1 of the U-065
 * security review; Record 11) both factories' direct and layered sockets enable exactly the ten allowed cipher
 * suites, in the allowed order, although this provider enables 31 by default, among them DHE, DSS and CBC
 * suites; nothing is provider-fixed here, so the three TLS 1.3 suites in that list are the factory's.
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>This provider is neither the app's runtime provider nor a device's. Nothing here shows what
 *       a device's TLS stack sends or enables.
 *   <li>"Enabled" is not "negotiable". The JDK security property for disabled algorithms lists
 *       TLS 1.0 and 1.1 and refuses them in a handshake whatever the client enables, so the tests
 *       assert only what the factory enables; the handshake classes show what is negotiated.
 *   <li>No handshake is run in this class: every socket here is unconnected, or layered over a plain
 *       connection that never carries TLS. Rejection of certificates, host names and resumed sessions
 *       is the business of the handshake classes.
 *   <li>On the layered shape this provider sets the host as server name by itself (U-065 probe), so
 *       the factories' server-name step cannot be observed there; for that shape only the absence of an
 *       exception is asserted, and the statement rests on reading the code (CR-013 Scope Concern D).
 *   <li>Whether the protocol list is built from the supported or the enabled protocols is no longer
 *       visible here (both give TLS 1.2 and 1.3 on this provider); the stand-in socket test of the
 *       Hardening class covers it.
 *   <li>The wire-level contents of a client hello are not examined.
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063 and U-065 mutation logs).
 * <ul>
 *   <li>AC-3: the guard and the safety net in {@code setSniViaSSLParameters} both removed (M-A1,
 *       the exception escapes {@code createSocket}); {@code setSniHost} reporting a name for an IP
 *       literal (M-A3).
 *   <li>AC-4: {@code setSniHost} made a no-op or deleted from {@code createSocket} (M-A4, and U-065's
 *       M-P6 (iii)); the host replaced by another name (M-A3).
 *   <li>AC-16, both tests as changed by U-065: the TLS 1.2 floor removed (M-P1/M-P8), or the old list
 *       given back to the default factory alone (M-P6 (ii)).
 *   <li>AC-19 as changed by U-065 (pinned, DNS host): the pinned server-name step removed (M-P5); the
 *       pinned cipher handling removed (M-P4) or one of its entries swapped (M-P6 (i)); the pinned list
 *       widened (M-P3); the floor removed (M-P1/M-P8).
 *   <li>U-065 pinned exact-protocol test: M-P3, M-P1/M-P8. It compares sets, so it does not detect a
 *       layered socket left untouched (M-P7): this provider already enables exactly TLS 1.3 and 1.2 there;
 *       the parity test detects M-P7 by order.
 *   <li>U-065 pinned rejected-name test: the catch in {@code setSniViaSSLParameters} removed (M-S5).
 *   <li>U-065 parity test: each of M-P2 to M-P7 and M-S5, in particular the three one-factory drifts
 *       M-P6 (i) to (iii).
 *   <li>U-066 exact lists (J2, J3) and the pinned cipher literal after the server-name step (J1): a pruned
 *       suite this provider offers put back (M-X1 (a), (c), (d)); the allow filter removed (M-X2); the TLS 1.3
 *       names or the signalling value removed (M-X4); the step removed from one factory (M-X5); the layered
 *       shape skipped in the default factory (M-X6, J2 and the parity test); two entries swapped (M-X8).
 * </ul>
 *
 * <p><b>Guard A1-G3 is a permanent tripwire.</b> If the JDK's TLS classes were initialised under
 * Conscrypt earlier in the same JVM by some other test, they cannot initialise here, and the guard
 * fails with its own name. A failure of A1-G3 therefore points at the other test, not at this one.
 * Until U-066, guard A1-G1 also required a TLS_DHE_ suite in the factory's cipher
 * list; since U-066 both providers give the same ten suites, so that clause is removed (it would fail every run)
 * and the provider check carries A1-G1 (U-066 AC-8).
 *
 * <p><b>After the CR-009 move:</b> a missing field, method or class means this test's seam needs
 * retargeting to the moved class; a failed behavioural assertion is a regression, and the test is
 * never edited to pass.
 */
@RunWith(RobolectricTestRunner.class)
@ConscryptMode(OFF)
@LooperMode(LEGACY)
public class DefaultTrustedSocketFactoryJdkProviderTest {

    private static final String IPV6_EIGHT_GROUP_LITERAL = "2001:0db8:0000:0000:0000:0000:0000:0001";
    private static final String DNS_HOST = "imap.right.test";

    /** The allowed protocols of the factory (U-065, Record 8). */
    private static final String[] ALLOWED_PROTOCOLS_BASELINE = {"TLSv1.2", "TLSv1.3"};

    /** The allowed cipher suites of the factory, in order (U-066; literal, never read from the factory). */
    private static final String[] ALLOWED_CIPHERS_BASELINE = {
            "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
    };

    /** Protocol versions older than TLS 1.2 that must never be enabled (Record 8). */
    private static final String[] OLD_PROTOCOLS = {"TLSv1", "TLSv1.1", "SSLv3", "SSLv2Hello"};

    /**
     * Isolation guards, in this order (A-W2): A1-G3 first, so that nothing touches the factory's
     * static initialiser before the JDK TLS classes are known to be usable; then the provider
     * check of A1-G1; then the factory's own cipher list (which triggers its static initialiser).
     */
    @Before
    public void checkIsolationGuards() throws Exception {
        // A1-G3: one unconnected default socket. Does not touch the factory.
        try {
            defaultSocket().close();
        } catch (NoClassDefFoundError | ExceptionInInitializerError e) {
            fail("A1-G3: the JDK TLS classes were initialised under Conscrypt earlier in this JVM "
                    + "(another test is the cause, not this one): " + e);
        }

        // A1-G1, provider check.
        String provider = SSLContext.getInstance("TLS").getProvider().getName();
        List<String> registered = new ArrayList<>();
        for (Provider p : Security.getProviders()) {
            registered.add(p.getName());
        }
        if (!"SunJSSE".equals(provider) || registered.contains("Conscrypt")) {
            fail("A1-G1: this class is not running on the JDK provider: TLS context provider="
                    + provider + ", registered providers=" + registered
                    + " (the sandbox of the Conscrypt-mode classes was probably reused)");
        }

        // A1-G3 via the factory's static initialiser: reading its cipher list runs it. Until U-066 the list
        // also told the providers apart (a TLS_DHE_ suite meant SunJSSE); since U-066 both providers give the
        // same ten allowed suites, so the provider check above carries A1-G1 (U-066 AC-8).
        try {
            constant("ENABLED_CIPHERS");
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            fail("A1-G3 (via factory static init): the factory's static initialiser could not run: " + e);
        }
    }

    /**
     * AC-3: a socket for the eight-group IPv6 literal is created without an exception and reports
     * no server name. RFC 6066 section 3. The socket is never connected.
     */
    @Test
    public void createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset() throws Exception {
        SSLSocket socket = hardenedSocket(IPV6_EIGHT_GROUP_LITERAL);
        try {
            assertThat(socket).isInstanceOf(SSLSocket.class);
            List<SNIServerName> names = socket.getSSLParameters().getServerNames();
            if (names != null) {
                assertThat(names).isEmpty();
            }
        } finally {
            closeQuietly(socket);
        }
    }

    /**
     * AC-4 (control for AC-3): for a DNS host the same observable reports exactly one name, an
     * SNIHostName holding that host. This shows that the observable used in AC-3 can report a name
     * in this environment, so AC-3 is not empty.
     */
    @Test
    public void createSocket_dnsHost_setsServerNameOnRealSocket() throws Exception {
        SSLSocket socket = hardenedSocket(DNS_HOST);
        try {
            List<SNIServerName> names = socket.getSSLParameters().getServerNames();
            assertThat(names).isNotNull();
            assertThat(names).hasSize(1);
            assertThat(names.get(0)).isInstanceOf(SNIHostName.class);
            assertThat(((SNIHostName) names.get(0)).getAsciiName()).isEqualTo(DNS_HOST);
        } finally {
            closeQuietly(socket);
        }
    }

    /**
     * AC-16 (supported-protocols clause), as changed by U-065 (AC-1 (b), BUG-019 AC-4): the factory
     * enables exactly the provider's supported protocols that are TLS 1.2 or newer, TLS 1.2 first, and
     * not SSLv3. Precondition A1-P1: on this provider the supported set differs from the default enabled
     * set. Since U-065 both sets give the same list here, so this test no longer tells them apart; the
     * stand-in socket test of the Hardening class does (AC-1 (d)).
     */
    @Test
    public void createSocket_enabledProtocols_areProviderSupportedTls12AndNewer_inRecordedOrder_jdkProvider()
            throws Exception {
        SSLSocket plain = defaultSocket();
        Set<String> supported;
        Set<String> defaultEnabled;
        try {
            supported = new HashSet<>(Arrays.asList(plain.getSupportedProtocols()));
            defaultEnabled = new HashSet<>(Arrays.asList(plain.getEnabledProtocols()));
        } finally {
            closeQuietly(plain);
        }
        assertWithMessage("A1-P1: the provider's supported protocols equal its default enabled "
                + "protocols, so this class cannot tell supported from enabled; supported="
                + supported + ", default enabled=" + defaultEnabled)
                .that(supported.equals(defaultEnabled)).isFalse();
        assertWithMessage("precondition: the provider supports TLS 1.2; supported=" + supported)
                .that(supported).contains("TLSv1.2");

        SSLSocket socket = hardenedSocket(DNS_HOST);
        List<String> enabled;
        try {
            enabled = Arrays.asList(socket.getEnabledProtocols());
        } finally {
            closeQuietly(socket);
        }

        assertThat(enabled).doesNotContain("SSLv3");
        Set<String> expected = new HashSet<>(supported);
        expected.retainAll(Arrays.asList(ALLOWED_PROTOCOLS_BASELINE));
        assertWithMessage("enabled protocols must be the supported protocols that are TLS 1.2 or newer")
                .that(new HashSet<>(enabled)).isEqualTo(expected);
        assertThat(enabled).containsNoDuplicates();
        assertWithMessage("TLSv1.2 must come first in " + enabled).that(enabled.get(0)).isEqualTo("TLSv1.2");
    }

    /**
     * AC-16 (second test, JDK form; decision point DP-2), as changed by U-065 (AC-1 (a), (b), (c);
     * BUG-019 AC-1, AC-2): the factory enables none of TLSv1, TLSv1.1, SSLv3 and SSLv2Hello, on the
     * direct and on the layered socket, although this provider supports all four; TLS 1.2 and 1.3 are
     * enabled if and only if supported. Precondition A1-P2: TLSv1 and TLSv1.1 are supported and not
     * enabled by default, so the test is made against a provider that supports them. Until U-065 the
     * factory enabled TLS 1.0, 1.1 and SSLv2Hello here (BUG-019); decided by the product owner on
     * 2026-11-20 (Record 8).
     */
    @Test
    public void createSocket_legacyProtocols_notEnabledByFactory_whereJdkProviderSupportsThem()
            throws Exception {
        List<String> legacy = Arrays.asList("TLSv1", "TLSv1.1");
        SSLSocket plain = defaultSocket();
        List<String> supported;
        try {
            supported = Arrays.asList(plain.getSupportedProtocols());
            List<String> defaultEnabled = Arrays.asList(plain.getEnabledProtocols());
            for (String name : legacy) {
                assertWithMessage("A1-P2: " + name + " must be supported and not enabled by default "
                        + "on this provider; supported=" + supported + ", default enabled="
                        + defaultEnabled)
                        .that(supported.contains(name) && !defaultEnabled.contains(name)).isTrue();
            }
        } finally {
            closeQuietly(plain);
        }

        SSLSocket socket = hardenedSocket(DNS_HOST);
        try {
            assertProtocolFloor("default factory, direct socket", supported,
                    Arrays.asList(socket.getEnabledProtocols()));
        } finally {
            closeQuietly(socket);
        }
        try (Layered layered = new Layered(
                new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()), DNS_HOST)) {
            assertProtocolFloor("default factory, layered socket", supported,
                    Arrays.asList(layered.socket.getEnabledProtocols()));
        }
    }

    /**
     * AC-19 as changed by U-065 (AC-4 (a), (d); BUG-020 AC-2, AC-3; CR-013 S1 and S6): the pinned
     * factory's direct socket for a DNS host carries exactly one server name, an SNIHostName equal to
     * the host, and on the same socket, after that step, exactly TLS 1.2 and 1.3 and the default
     * factory's cipher suites in the default factory's order. Precondition: an unconnected socket of this
     * provider carries no server name of its own, so the name can only be the factory's (U-063 mutation
     * M-E2-jdk showed that this observable reports what the factory sets). Control: the default factory's
     * createSocket_dnsHost_setsServerNameOnRealSocket. The layered shape is not asserted for the name:
     * on this provider a layered socket already carries the host before any factory step (U-065 probe).
     * Since U-066 the same socket's suites are also compared with the allowed list as a literal (CR-014
     * Check of Amendment 1, point 6).
     */
    @Test
    public void pinnedFactory_createSocket_implicit_setsDnsHostAsOnlyServerName_jdkProvider() throws Exception {
        SSLSocket plain = defaultSocket();
        try {
            assertWithMessage("precondition: an unconnected default socket carries no server name")
                    .that(serverNames(plain)).isEmpty();
        } finally {
            closeQuietly(plain);
        }
        SSLSocket socket = pinnedSocket(DNS_HOST);
        SSLSocket reference = null;
        try {
            reference = hardenedSocket(DNS_HOST);
            assertThat(socket).isInstanceOf(SSLSocket.class);
            List<SNIServerName> names = socket.getSSLParameters().getServerNames();
            assertWithMessage("server names on the pinned implicit socket").that(names).isNotNull();
            assertThat(names).hasSize(1);
            assertThat(names.get(0)).isInstanceOf(SNIHostName.class);
            assertThat(((SNIHostName) names.get(0)).getAsciiName()).isEqualTo(DNS_HOST);
            assertWithMessage("AC-4 (d): protocols on the same socket after the server-name step")
                    .that(Arrays.asList(socket.getEnabledProtocols())).containsExactly("TLSv1.2", "TLSv1.3");
            assertWithMessage("AC-4 (d): cipher suites on the same socket after the server-name step,"
                    + " in the default factory's order")
                    .that(Arrays.asList(socket.getEnabledCipherSuites()))
                    .containsExactlyElementsIn(Arrays.asList(reference.getEnabledCipherSuites())).inOrder();
            assertWithMessage("U-066 AC-2 (a): cipher suites on the same socket after the server-name step,"
                    + " the allowed list as recorded")
                    .that(Arrays.asList(socket.getEnabledCipherSuites()))
                    .containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
        } finally {
            closeQuietly(socket);
            if (reference != null) {
                closeQuietly(reference);
            }
        }
    }

    /**
     * U-065 AC-2 (a), (c) (BUG-020, first refinement of its Suggested Approach): the pinned factory's
     * direct and layered sockets enable exactly TLS 1.2 and TLS 1.3 on this provider, which supports
     * SSLv3, TLS 1.0, TLS 1.1 and SSLv2Hello and leaves them off by default. A pinned factory that took
     * the default factory's old list (every supported protocol except SSLv3) would add TLSv1, TLSv1.1 and
     * SSLv2Hello here and fail. Mutations: M-P3 (that widening), M-P1/M-P8 (the floor removed). It
     * compares sets, so a socket the factory left untouched passes it on this provider (its defaults are
     * TLS 1.3 and 1.2); the parity test below catches that by order (M-P7).
     */
    @Test
    public void pinnedFactory_createSocket_enablesExactlyTls12AndTls13_bothShapes_jdkProvider() throws Exception {
        SSLSocket plain = defaultSocket();
        List<String> supported;
        List<String> defaultEnabled;
        try {
            supported = Arrays.asList(plain.getSupportedProtocols());
            defaultEnabled = Arrays.asList(plain.getEnabledProtocols());
        } finally {
            closeQuietly(plain);
        }
        for (String name : OLD_PROTOCOLS) {
            assertWithMessage("precondition: " + name + " must be supported and not enabled by default on"
                    + " this provider; supported=" + supported + ", default enabled=" + defaultEnabled)
                    .that(supported.contains(name) && !defaultEnabled.contains(name)).isTrue();
        }

        SSLSocket direct = pinnedSocket(DNS_HOST);
        try {
            List<String> enabled = Arrays.asList(direct.getEnabledProtocols());
            assertWithMessage("pinned direct socket").that(enabled).containsExactly("TLSv1.2", "TLSv1.3");
            assertProtocolFloor("pinned direct socket", supported, enabled);
        } finally {
            closeQuietly(direct);
        }
        try (Layered layered = new Layered(pinnedFactory(), DNS_HOST)) {
            List<String> enabled = Arrays.asList(layered.socket.getEnabledProtocols());
            assertWithMessage("pinned layered socket").that(enabled).containsExactly("TLSv1.2", "TLSv1.3");
            assertProtocolFloor("pinned layered socket", supported, enabled);
        }
    }

    /**
     * U-065 AC-4, statements S2, S3 and S4 of CR-013 for the pinned factory: for an IPv4-form host, a
     * host with a colon, and a null, empty or blank host, the direct socket carries no server name and
     * createSocket does not throw; the layered socket is created without an exception (AC-4 (b), (c): on
     * this provider a layered socket may carry a name the provider set itself, so only the absence of an
     * exception is asserted there; this provider accepts a null layered host, U-065 probe). Every host is
     * tried before the assertion, so a failure lists all of them. These hold with or without the step
     * (the shared guard skips them); they fail if the pinned path ever sets a name for them (S6).
     */
    @Test
    public void pinnedFactory_createSocket_ipLiteralAndBlankHosts_setNoServerName_jdkProvider() throws Exception {
        String[] hosts = {"192.168.1.10", "::1", "fe80::1", IPV6_EIGHT_GROUP_LITERAL, null, "", "   "};
        List<String> problems = new ArrayList<>();
        for (String host : hosts) {
            SSLSocket direct;
            try {
                direct = pinnedSocket(host);
            } catch (RuntimeException e) {
                problems.add("direct " + quote(host) + " threw " + e);
                continue;
            }
            try {
                List<String> names = serverNames(direct);
                if (!names.isEmpty()) {
                    problems.add("direct " + quote(host) + " carries server names " + names);
                }
            } finally {
                closeQuietly(direct);
            }
            try (Layered layered = new Layered(pinnedFactory(), host)) {
                // created without an exception; the cast in Layered checks that it is an SSLSocket
            } catch (RuntimeException e) {
                problems.add("layered " + quote(host) + " threw " + e);
            }
        }
        assertWithMessage("S2 to S4 on the pinned factory; hosts tried: " + Arrays.toString(hosts))
                .that(problems).isEmpty();
    }

    /**
     * U-065 AC-4, statement S5 of CR-013 for the pinned factory (the call-through test the U-064 reviews
     * asked for): for each name that passes the IP/blank guard and that SNIHostName rejects, the guard
     * answers false, the pinned factory's direct createSocket throws nothing and its socket carries no
     * server name, and the layered createSocket throws nothing. Only the catch of IllegalArgumentException
     * in DefaultTrustedSocketFactory.setSniViaSSLParameters provides this; the pinned factory reaches it
     * through setSniHost. Mutation: that catch removed (M-S5, the edit of U-064's M-N2).
     */
    @Test
    public void pinnedFactory_createSocket_nameRejectedBySniHostName_doesNotThrowAndSetsNoServerName_jdkProvider()
            throws Exception {
        String[] names = {"bad_host.example", "host.example.", "-x.example"};
        java.lang.reflect.Method guard = DefaultTrustedSocketFactory.class
                .getDeclaredMethod("isIpOrBlankHostname", String.class);
        guard.setAccessible(true);
        List<String> escaped = new ArrayList<>();

        for (String name : names) {
            assertWithMessage("isIpOrBlankHostname(\"" + name + "\") must be false, so the name reaches"
                    + " SNIHostName").that((Boolean) guard.invoke(null, name)).isFalse();
            SSLSocket direct;
            try {
                direct = pinnedSocket(name);
            } catch (RuntimeException e) {
                escaped.add("direct " + name + " -> " + e);
                continue;
            }
            try {
                assertWithMessage("server names on the pinned direct socket for \"" + name + "\"")
                        .that(serverNames(direct)).isEmpty();
            } finally {
                closeQuietly(direct);
            }
            try (Layered layered = new Layered(pinnedFactory(), name)) {
                // created without an exception
            } catch (RuntimeException e) {
                escaped.add("layered " + name + " -> " + e);
            }
        }
        assertWithMessage("the pinned createSocket must not throw for a name SNIHostName rejects; names"
                + " tried: " + Arrays.toString(names) + "; escaped").that(escaped).isEmpty();
    }

    /**
     * U-065 AC-5 and AC-3 (a) (JDK provider): for each host, the pinned factory's direct socket and the
     * default factory's direct socket have the same enabled protocols (as a set and in order), the same
     * enabled cipher suites in the same order, and the same server names; for a DNS host the protocols and
     * cipher suites of the two layered sockets are compared too (their names are not: the provider sets
     * them itself). Precondition: the default factory's cipher order is not the provider's own, so parity
     * cannot hold on provider defaults. A drift of one factory's protocol, cipher or server-name handling
     * fails here (M-P6 (i) to (iii)). It does not prescribe how the code is shared.
     */
    @Test
    public void pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider() throws Exception {
        SSLSocket plain = defaultSocket();
        SSLSocket probe = hardenedSocket(DNS_HOST);
        try {
            assertWithMessage("precondition: the default factory's cipher order must differ from the"
                    + " provider's own")
                    .that(Arrays.asList(probe.getEnabledCipherSuites()))
                    .isNotEqualTo(Arrays.asList(plain.getEnabledCipherSuites()));
        } finally {
            closeQuietly(plain);
            closeQuietly(probe);
        }

        String[] hosts = {DNS_HOST, "192.168.1.10", "::1", "", null, "bad_host.example"};
        for (String host : hosts) {
            SSLSocket pinned = pinnedSocket(host);
            SSLSocket reference = null;
            try {
                reference = hardenedSocket(host);
                assertSameHandling("direct socket for host " + quote(host), reference, pinned);
                assertWithMessage("server names, direct socket for host " + quote(host))
                        .that(serverNames(pinned)).isEqualTo(serverNames(reference));
            } finally {
                closeQuietly(pinned);
                if (reference != null) {
                    closeQuietly(reference);
                }
            }
        }
        try (Layered pinned = new Layered(pinnedFactory(), DNS_HOST);
             Layered reference = new Layered(
                     new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()), DNS_HOST)) {
            assertSameHandling("layered socket for host " + quote(DNS_HOST), reference.socket, pinned.socket);
        }
    }

    /**
     * U-066 AC-1 (this provider), AC-3 (a): the default factory's direct and layered sockets enable exactly
     * the ten allowed suites, in the allowed order. Preconditions (AC-1 (a)): this provider's own default
     * socket enables DHE-RSA, DHE-DSS, ECDHE CBC with SHA-1 and CBC with SHA-256 or SHA-384 suites, and its
     * list is not the allowed list. Nothing is provider-fixed here, so the TLS 1.3 suites in the result are
     * the factory's (AC-1 (c)). On another provider the preconditions fail (AC-8 (d)).
     */
    @Test
    public void createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes_jdkProvider()
            throws Exception {
        assertJdkDefaultsOfferPrunedSuites(defaultEnabledCiphers());
        SSLSocket direct = hardenedSocket(DNS_HOST);
        try {
            assertAllowedCipherList("default factory, direct socket",
                    Arrays.asList(direct.getEnabledCipherSuites()));
        } finally {
            closeQuietly(direct);
        }
        try (Layered layered = new Layered(
                new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()), DNS_HOST)) {
            assertAllowedCipherList("default factory, layered socket",
                    Arrays.asList(layered.socket.getEnabledCipherSuites()));
        }
    }

    /**
     * U-066 AC-2 (a) (this provider), AC-3 (a): the pinned-certificate factory's direct and layered sockets
     * enable exactly the ten allowed suites, in the allowed order, against the literal. Preconditions as
     * above.
     */
    @Test
    public void pinnedFactory_createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes_jdkProvider()
            throws Exception {
        assertJdkDefaultsOfferPrunedSuites(defaultEnabledCiphers());
        SSLSocket direct = pinnedSocket(DNS_HOST);
        try {
            assertAllowedCipherList("pinned factory, direct socket",
                    Arrays.asList(direct.getEnabledCipherSuites()));
        } finally {
            closeQuietly(direct);
        }
        try (Layered layered = new Layered(pinnedFactory(), DNS_HOST)) {
            assertAllowedCipherList("pinned factory, layered socket",
                    Arrays.asList(layered.socket.getEnabledCipherSuites()));
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Reads a protected static String[] field of the factory by reflection. */
    private static String[] constant(String name) throws Exception {
        Field field = DefaultTrustedSocketFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        return (String[]) field.get(null);
    }

    /** An unconnected socket from a fresh default TLS context (SunJSSE here), untouched by the factory. */
    private static SSLSocket defaultSocket() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        return (SSLSocket) context.getSocketFactory().createSocket();
    }

    /** The factory's own socket for the given host, unconnected. */
    private static SSLSocket hardenedSocket(String host) throws Exception {
        return (SSLSocket) new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication())
                .createSocket(null, host, 993, null);
    }

    /** The pinned factory, built with the DNS host; the other-host leaf is enrolled (no handshake here). */
    private static PinnedCertificateSocketFactory pinnedFactory() throws Exception {
        return new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(), DNS_HOST,
                certificate(OTHER_HOST_CERT));
    }

    /** The pinned factory's own socket for the given host, unconnected. */
    private static SSLSocket pinnedSocket(String host) throws Exception {
        return (SSLSocket) pinnedFactory().createSocket(null, host, 993, null);
    }

    /** The ASCII server names the socket reports; empty for null or none. */
    private static List<String> serverNames(SSLSocket socket) {
        List<SNIServerName> names = socket.getSSLParameters().getServerNames();
        List<String> out = new ArrayList<>();
        if (names != null) {
            for (SNIServerName name : names) {
                out.add(name instanceof SNIHostName ? ((SNIHostName) name).getAsciiName() : name.toString());
            }
        }
        return out;
    }

    /** AC-1 (a) and (b) for one socket: none of the old four; TLS 1.2 and 1.3 each enabled iff supported. */
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

    /** Protocols as a set and in order, and cipher suites in order, of candidate against reference. */
    private static void assertSameHandling(String label, SSLSocket reference, SSLSocket candidate) {
        List<String> expectedProtocols = Arrays.asList(reference.getEnabledProtocols());
        List<String> actualProtocols = Arrays.asList(candidate.getEnabledProtocols());
        assertWithMessage(label + ": enabled protocols as a set")
                .that(new HashSet<>(actualProtocols)).isEqualTo(new HashSet<>(expectedProtocols));
        assertWithMessage(label + ": enabled protocols in order")
                .that(actualProtocols).containsExactlyElementsIn(expectedProtocols).inOrder();
        assertWithMessage(label + ": enabled cipher suites in order")
                .that(Arrays.asList(candidate.getEnabledCipherSuites()))
                .containsExactlyElementsIn(Arrays.asList(reference.getEnabledCipherSuites())).inOrder();
    }

    private static String quote(String host) {
        return host == null ? "null" : "\"" + host + "\"";
    }

    private static void quietClose(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // nothing to do for a test socket
            }
        }
    }

    /** A socket a factory layered over a fresh plain loopback connection, with the plain sockets it needs. */
    private static final class Layered implements java.io.Closeable {
        private final ServerSocket listener;
        private final Socket client;
        private final Socket accepted;
        private final SSLSocket socket;

        Layered(TrustedSocketFactory factory, String host) throws Exception {
            ServerSocket l = LoopbackTlsTestServer.bindPlainLoopback();
            Socket c = null;
            Socket a = null;
            SSLSocket s;
            try {
                c = LoopbackTlsTestServer.connectPlain(l.getLocalPort());
                a = l.accept();
                s = (SSLSocket) factory.createSocket(c, host, l.getLocalPort(), null);
            } catch (Exception | Error e) {
                quietClose(c);
                quietClose(a);
                quietClose(l);
                throw e;
            }
            listener = l;
            client = c;
            accepted = a;
            socket = s;
        }

        @Override
        public void close() {
            quietClose(socket);
            quietClose(client);
            quietClose(accepted);
            quietClose(listener);
        }
    }

    private static void closeQuietly(SSLSocket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // nothing to do for an unconnected test socket
        }
    }

    /** The default-enabled cipher suites of an untouched default socket of this provider. */
    private static List<String> defaultEnabledCiphers() throws Exception {
        SSLSocket plain = defaultSocket();
        try {
            return Arrays.asList(plain.getEnabledCipherSuites());
        } finally {
            closeQuietly(plain);
        }
    }

    /** AC-1 (a) for this provider: the pruned categories it offers are in its default list, which is not the allowed list. */
    private static void assertJdkDefaultsOfferPrunedSuites(List<String> defaults) {
        assertWithMessage("precondition AC-1 (a): a TLS_DHE_RSA_ suite in the defaults; defaults=" + defaults)
                .that(containsMatch(defaults, "TLS_DHE_RSA_", "")).isTrue();
        assertWithMessage("precondition AC-1 (a): a TLS_DHE_DSS_ suite in the defaults; defaults=" + defaults)
                .that(containsMatch(defaults, "TLS_DHE_DSS_", "")).isTrue();
        assertWithMessage("precondition AC-1 (a): an ECDHE suite with CBC and SHA-1 in the defaults")
                .that(containsMatch(defaults, "TLS_ECDHE_", "_CBC_SHA")).isTrue();
        assertWithMessage("precondition AC-1 (a): a CBC suite with SHA-256 or SHA-384 in the defaults")
                .that(containsMatch(defaults, "", "_CBC_SHA256") || containsMatch(defaults, "", "_CBC_SHA384"))
                .isTrue();
        assertWithMessage("precondition AC-1 (a): the provider's own list must not be the allowed list")
                .that(defaults).isNotEqualTo(Arrays.asList(ALLOWED_CIPHERS_BASELINE));
    }

    /** AC-1, AC-2: exactly the allowed list, in order; AC-3 (a): no suite of W1 to W12. */
    private static void assertAllowedCipherList(String label, List<String> enabled) {
        assertWithMessage(label + ": the enabled cipher suites must be exactly the allowed list, in its order")
                .that(enabled).containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
        for (String suite : enabled) {
            assertWithMessage(label + ": AC-3 (a): " + suite + " matches a weak category")
                    .that(weakCategories(suite)).isEmpty();
        }
    }

    /**
     * The weak categories of U-066 AC-3 that a name matches: W1 CBC, W2 SHA-1 MAC, W3 RSA key exchange, W4
     * static ECDH, W5 DHE, W6 DES and 3DES, W7 DSS, W8 NULL, W9 anonymous, W10 export, W11 RC4, W12 MD5. The
     * renegotiation signalling value is exempt by name: it is not a cipher.
     */
    private static List<String> weakCategories(String name) {
        List<String> found = new ArrayList<>();
        if ("TLS_EMPTY_RENEGOTIATION_INFO_SCSV".equals(name)) {
            return found;
        }
        if (name.contains("_CBC_")) found.add("W1");
        if (name.endsWith("_SHA")) found.add("W2");
        if (name.startsWith("TLS_RSA_") || name.startsWith("SSL_RSA_")) found.add("W3");
        if (name.startsWith("TLS_ECDH_")) found.add("W4");
        if (name.startsWith("TLS_DHE_") || name.startsWith("SSL_DHE_")) found.add("W5");
        if (name.contains("DES")) found.add("W6");
        if (name.contains("_DSS_")) found.add("W7");
        if (name.contains("_NULL_")) found.add("W8");
        if (name.toLowerCase(java.util.Locale.ROOT).contains("_anon_")) found.add("W9");
        if (name.contains("EXPORT")) found.add("W10");
        if (name.contains("RC4")) found.add("W11");
        if (name.contains("MD5")) found.add("W12");
        return found;
    }

    /** True if a name in {@code names} starts with {@code prefix} and ends with {@code suffix}. */
    private static boolean containsMatch(List<String> names, String prefix, String suffix) {
        for (String name : names) {
            if (name.startsWith(prefix) && name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
