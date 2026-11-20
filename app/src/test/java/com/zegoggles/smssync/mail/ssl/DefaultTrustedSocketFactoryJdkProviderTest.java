package com.zegoggles.smssync.mail.ssl;

import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
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
 * (story U-063, acceptance criteria AC-3, AC-4 and the supported-protocols clause of AC-16). It also
 * holds AC-19 for the pinned factory, because that check needs the same provider: under Conscrypt the
 * server-name observable reads nothing whatever is set, so mutation M-E2 was not detected there
 * (product owner decision of 2026-11-20).
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
 * <p><b>What these tests prove.</b> With the JDK provider: the factory's socket for a DNS host
 * carries exactly that host as its one server name; the socket for an eight-group IPv6 literal
 * carries none and creating it does not throw (RFC 6066 section 3: the server name list holds DNS
 * names, never IP literals); the factory enables exactly the provider's supported protocols minus
 * SSLv3, with TLS 1.2, 1.1 and 1.0 first in that order; and the factory enables TLS 1.0 and 1.1 on
 * a provider that leaves them off by default (RFC 8996 deprecates both; this is the decision
 * point DP-2 finding, recorded as a characterisation, not as approval).
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>This provider is neither the app's runtime provider nor a device's. Nothing here shows what
 *       a device's TLS stack sends or enables.
 *   <li>"Enabled" is not "negotiable". The JDK security property for disabled algorithms lists
 *       TLS 1.0 and 1.1 and may still refuse them during a handshake, so the tests assert only
 *       what the factory enables.
 *   <li>No handshake is run in this class: every socket here is unconnected. Rejection of
 *       certificates, host names and resumed sessions is the business of the handshake classes.
 *   <li>The wire-level contents of a client hello are not examined.
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063 mutation log).
 * <ul>
 *   <li>AC-3: the guard and the safety net in {@code setSniViaSSLParameters} both removed (M-A1,
 *       the exception escapes {@code createSocket}); {@code setSniHost} reporting a name for an IP
 *       literal (M-A3).
 *   <li>AC-4: {@code setSniHost} made a no-op or deleted from {@code createSocket} (M-A4); the
 *       host replaced by another name (M-A3).
 *   <li>AC-16 first test: the protocol list built from the enabled instead of the supported
 *       protocols (M-D11); SSLv3 no longer blocked (M-D2); the known order changed (M-D5); the
 *       hardening call removed (M-D8) or its protocol part removed (M-D13).
 *   <li>AC-16 second test: M-D11; TLS 1.0 added to the blocked protocols (M-D12).
 *   <li>AC-19 (pinned factory): the pinned factory's {@code createSocket} made to set a server name
 *       (M-E2).
 * </ul>
 *
 * <p><b>Guard A1-G3 is a permanent tripwire.</b> If the JDK's TLS classes were initialised under
 * Conscrypt earlier in the same JVM by some other test, they cannot initialise here, and the guard
 * fails with its own name. A failure of A1-G3 therefore points at the other test, not at this one.
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
    private static final int CIPHER_ENTRIES_IN_MESSAGES = 5;

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

        // A1-G1, the factory's reflected cipher list must come from SunJSSE: it has DHE suites,
        // Conscrypt's default list has none.
        String[] ciphers;
        try {
            ciphers = constant("ENABLED_CIPHERS");
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            fail("A1-G3 (via factory static init): the factory's static initialiser could not run: "
                    + e);
            return;
        }
        boolean hasDhe = false;
        if (ciphers != null) {
            for (String cipher : ciphers) {
                if (cipher.startsWith("TLS_DHE_")) {
                    hasDhe = true;
                    break;
                }
            }
        }
        if (!hasDhe) {
            fail("A1-G1: the factory's ENABLED_CIPHERS has no TLS_DHE_ suite, so it was computed "
                    + "under another provider: TLS context provider=" + provider
                    + ", first entries=" + firstEntries(ciphers));
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
     * AC-16 (supported-protocols clause): the factory enables the provider's supported protocols
     * minus SSLv3, with TLS 1.2, 1.1 and 1.0 first in that order. Precondition A1-P1: on this
     * provider the supported set differs from the default enabled set, otherwise this class cannot
     * tell a list built from the supported protocols from one built from the enabled ones.
     */
    @Test
    public void createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider()
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

        SSLSocket socket = hardenedSocket(DNS_HOST);
        List<String> enabled;
        try {
            enabled = Arrays.asList(socket.getEnabledProtocols());
        } finally {
            closeQuietly(socket);
        }

        assertThat(enabled).doesNotContain("SSLv3");
        Set<String> expected = new HashSet<>(supported);
        expected.remove("SSLv3");
        assertThat(new HashSet<>(enabled)).isEqualTo(expected);
        assertThat(enabled).containsNoDuplicates();

        // Among the protocols present, TLSv1.2 precedes TLSv1.1 precedes TLSv1, and all of
        // them precede any other protocol.
        List<String> known = Arrays.asList("TLSv1.2", "TLSv1.1", "TLSv1");
        int last = -1;
        for (String name : known) {
            int index = enabled.indexOf(name);
            if (index >= 0) {
                assertWithMessage(name + " must come after the preceding known protocol in " + enabled)
                        .that(index).isGreaterThan(last);
                last = index;
            }
        }
        for (int i = 0; i < enabled.size(); i++) {
            if (!known.contains(enabled.get(i))) {
                assertWithMessage(enabled.get(i) + " must come after every known protocol in "
                        + enabled).that(i).isGreaterThan(last);
            }
        }
    }

    /**
     * AC-16 (second test, JDK form; decision point DP-2): TLS 1.0 and TLS 1.1 are enabled by the
     * factory although this provider leaves them off by default. RFC 8996 deprecates both. This
     * records the factory's own part in enabling them; it is a characterisation of present
     * behaviour, not an endorsement. Precondition A1-P2: for each of the two the default socket
     * lists it as supported and not enabled, otherwise the test cannot tell the factory from the
     * provider. Bug id: BUG-019 (filed; Option A applies, so this test passes as a characterisation).
     */
    @Test
    public void createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff()
            throws Exception {
        List<String> legacy = Arrays.asList("TLSv1", "TLSv1.1");
        SSLSocket plain = defaultSocket();
        try {
            List<String> supported = Arrays.asList(plain.getSupportedProtocols());
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
            List<String> enabled = Arrays.asList(socket.getEnabledProtocols());
            for (String name : legacy) {
                assertWithMessage(name + " should be enabled by the factory, enabled=" + enabled)
                        .that(enabled).contains(name);
            }
        } finally {
            closeQuietly(socket);
        }
    }

    /**
     * AC-19, implicit shape, unconnected: the pinned factory's socket for the DNS host reports no server
     * name. The control is AC-4 in this class, which shows that the observable can report a name on this
     * provider. The mutation that must break this test is M-E2 (the pinned factory made to set a server
     * name).
     *
     * <p>What it cannot prove: this is a desktop JDK provider, not a device's. The environment
     * diagnostic's capture of the first handshake message found that the default test provider puts a
     * name into the client hello on its own for both factories, so "the factory sets no name" is not
     * "no name is sent". The layered shape is not asserted, because a provider may add a name itself.
     */
    @Test
    public void pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider() throws Exception {
        SSLSocket socket = (SSLSocket) new PinnedCertificateSocketFactory(
                RuntimeEnvironment.getApplication(), DNS_HOST, certificate(OTHER_HOST_CERT))
                .createSocket(null, DNS_HOST, 993, null);
        try {
            assertThat(socket).isInstanceOf(SSLSocket.class);
            List<SNIServerName> names = socket.getSSLParameters().getServerNames();
            assertWithMessage("server names on the pinned implicit socket")
                    .that(names == null || names.isEmpty()).isTrue();
        } finally {
            closeQuietly(socket);
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

    private static void closeQuietly(SSLSocket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // nothing to do for an unconnected test socket
        }
    }

    private static String firstEntries(String[] values) {
        if (values == null) {
            return "null";
        }
        return Arrays.asList(values).subList(0, Math.min(values.length, CIPHER_ENTRIES_IN_MESSAGES))
                .toString();
    }
}
