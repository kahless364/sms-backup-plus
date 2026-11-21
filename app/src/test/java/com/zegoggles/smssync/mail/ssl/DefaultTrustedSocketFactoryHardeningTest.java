package com.zegoggles.smssync.mail.ssl;

import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.zegoggles.smssync.mail.PinnedCertificateSocketFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Characterisation tests for the cipher and protocol hardening of {@link DefaultTrustedSocketFactory}
 * (story U-063, Group D, acceptance criteria AC-11 to AC-17; protocol tests changed and tests added by
 * story U-065), run under Robolectric's default TLS provider. At the environment probe (I0b) that
 * provider was Conscrypt.
 *
 * <p><b>What the code does, and what these tests pin down.</b> The factory computes two static lists
 * once per class loader: the provider's default enabled cipher suites with 16 blocked names removed
 * and 24 known names placed first in a fixed order, and the provider's <em>supported</em> protocols
 * with SSLv3 removed, TLS 1.2, 1.1, 1.0 placed first in that order, and then only TLS 1.2 and TLS 1.3
 * kept. The last step is the protocol floor of U-065 (BUG-019; product owner decision Record 8 of
 * 2026-11-20: no protocol older than TLS 1.2, for either factory). Every socket it creates gets both
 * lists applied; if the lists could not be computed at class initialisation, the floor is computed
 * from the socket itself. The tests check the pure reordering function, the four constant lists and
 * the allowed-protocol list against literal copies kept here (snapshot tests, so that any edit to a
 * list is a conscious, reviewed change), the protocol step fed given supported lists through a
 * stand-in socket, the lists on a real implicit-TLS socket, and the same lists on a socket layered
 * over a plain connection. Since U-065 the pinned-certificate factory's cipher suites are checked here
 * against the same recorded rule (BUG-020; Record 9, "Same as default"). Expectations come from the
 * live provider at test time plus those literal baselines; no list of provider suites is hard-coded.
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>The lists here are those of a desktop test provider, not of a device. What a device's
 *       platform enables or reorders is not shown.
 *   <li>"Enabled" is not "negotiable": nothing here connects to a server, so nothing shows which
 *       suite or protocol a handshake would settle on. No handshake is run in this class; the
 *       handshake classes show TLS 1.2 negotiated and older versions refused.
 *   <li>The array of enabled protocols is not a preference order (SC-5). Since U-065 it holds TLS 1.2
 *       and then TLS 1.3 where both are supported; the TLS stack does not read the array as a
 *       preference list, and this is not claimed to be a defect. The order is recorded as it is.
 *   <li>The provider fixes some suites in front of whatever the caller sets (under Conscrypt, the
 *       three TLS 1.3 suites). The order checks of AC-15 therefore run on the list with those
 *       provider-fixed suites removed (plan deviation 7), within bounds that are asserted so that the
 *       checks cannot pass on an empty or tiny remainder.
 *   <li>The class initialiser's failure branch (provider query failed, both static lists null) cannot
 *       be made to run: the initialiser runs once per class loader and the lists are final. The code
 *       of that branch is called directly with null lists (U-065 AC-1 (e)).
 *   <li>Under this provider the supported protocols equal the default enabled ones, so whether the
 *       protocol list is built from the supported or the enabled list is shown only by the stand-in
 *       socket test (U-065 AC-1 (d)).
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063 and U-065 mutation logs).
 * <ul>
 *   <li>AC-11: a name removed from the blocked-cipher list (M-D1), the blocked-protocol list
 *       emptied (M-D2), or the removal loop in {@code reorder} deleted (M-D3).
 *   <li>AC-12: two entries of a known list swapped (M-D4, M-D5), or the unknown items put first or
 *       dropped (M-D6a, M-D6b).
 *   <li>AC-13: the null-blocklist guard removed (M-D7), or empty input mishandled (M-D15).
 *   <li>AC-14, ciphers: any entry of the known or blocked cipher list changed (M-D1, M-D4, M-D14).
 *       AC-14, protocols: any entry of the known or blocked protocol list changed (M-D2, M-D5, M-D12).
 *   <li>AC-15: the hardening call removed from {@code createSocket} (M-D8), the cipher part of
 *       {@code hardenSocket} removed (M-D9), or the unknown items dropped (M-D6b).
 *   <li>AC-16, both tests as changed by U-065: the TLS 1.2 floor removed (M-P1/M-P8), or the old
 *       protocol list given back to the default factory alone (M-P6 (ii)).
 *   <li>AC-17: hardening applied only when no plain socket is passed in (M-D10).
 *   <li>U-065 stand-in socket test: the floor removed (M-P1/M-P8); the list built from the enabled
 *       instead of the supported protocols (M-D11r).
 *   <li>U-065 null-lists test: the floor not applied when the lists are null (M-F1).
 *   <li>U-065 pinned cipher test: the pinned factory's cipher handling removed (M-P4), or one entry of
 *       its order swapped (M-P6 (i)).
 * </ul>
 * The U-063 mutations ran against the code before U-065 and are not repeated.
 *
 * <p><b>Isolation guard A1-G2.</b> The before-method fails, naming A1-G2, if the factory's static
 * cipher list was computed under the JDK provider (it would contain DHE suites, which this class's
 * provider never produces). That would mean the JDK-provider class's Robolectric sandbox was reused
 * here. The guard also has an SSLv2Hello clause on the protocol list; since U-065 the factory never
 * enables SSLv2Hello under either provider, so that clause can no longer fire and the DHE clause alone
 * carries the guard (U-065 AC-6 (e); the guard code is left unchanged).
 *
 * <p><b>After the CR-009 move:</b> a missing field, method or class means this test's seam needs
 * retargeting to the moved class; a failed behavioural assertion is a regression, and the test is
 * never edited to pass.
 */
@RunWith(RobolectricTestRunner.class)
public class DefaultTrustedSocketFactoryHardeningTest {

    private static final String HOST = "imap.right.test";
    private static final int ENTRIES_IN_MESSAGES = 5;

    /** The 24 ordered known cipher names of the factory, in order (snapshot). */
    private static final String[] KNOWN_CIPHERS_BASELINE = {
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
            "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
    };

    /** The 16 blocked cipher names of the factory, in order (snapshot). */
    private static final String[] BLOCKED_CIPHERS_BASELINE = {
            "SSL_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_DSS_WITH_DES_CBC_SHA",
            "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
            "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
            "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
            "TLS_ECDH_RSA_WITH_RC4_128_SHA",
            "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_MD5",
    };

    private static final String[] KNOWN_PROTOCOLS_BASELINE = {"TLSv1.2", "TLSv1.1", "TLSv1"};
    private static final String[] BLOCKED_PROTOCOLS_BASELINE = {"SSLv3"};

    /** The allowed protocols of the factory, in order (snapshot; U-065, Record 8). */
    private static final String[] ALLOWED_PROTOCOLS_BASELINE = {"TLSv1.2", "TLSv1.3"};

    /** Protocol versions older than TLS 1.2 that must never be enabled (Record 8). */
    private static final String[] OLD_PROTOCOLS = {"TLSv1", "TLSv1.1", "SSLv3", "SSLv2Hello"};

    /**
     * Isolation guard A1-G2: the factory's static lists must have been computed under this class's
     * provider. SSLv2Hello and DHE suites appear only under the JDK provider.
     */
    @Before
    public void checkIsolationGuardA1G2() throws Exception {
        String[] protocols = constant("ENABLED_PROTOCOLS");
        String[] ciphers = constant("ENABLED_CIPHERS");
        String provider = SSLContext.getInstance("TLS").getProvider().getName();

        if (protocols != null && Arrays.asList(protocols).contains("SSLv2Hello")) {
            fail("A1-G2: the factory's ENABLED_PROTOCOLS contains SSLv2Hello, so it was computed "
                    + "under the JDK provider: TLS context provider=" + provider
                    + ", first entries=" + firstEntries(protocols));
        }
        if (ciphers != null) {
            for (String cipher : ciphers) {
                if (cipher.startsWith("TLS_DHE_")) {
                    fail("A1-G2: the factory's ENABLED_CIPHERS contains " + cipher
                            + ", so it was computed under the JDK provider: TLS context provider="
                            + provider + ", first entries=" + firstEntries(ciphers));
                }
            }
        }
    }

    /**
     * AC-11: reorder removes every blocked item, for ciphers and for protocols.
     */
    @Test
    public void reorder_removesEveryBlockedItem() throws Exception {
        List<String> input = new ArrayList<>(Arrays.asList(BLOCKED_CIPHERS_BASELINE));
        input.add("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
        input.add("TLS_RSA_WITH_AES_128_CBC_SHA");
        input.add("TLS_U063_UNKNOWN_SUITE");

        List<String> result = Arrays.asList(reorder(input.toArray(new String[0]),
                constant("ORDERED_KNOWN_CIPHERS"), constant("BLACKLISTED_CIPHERS")));

        for (String blocked : BLOCKED_CIPHERS_BASELINE) {
            assertWithMessage(blocked + " should have been removed, result=" + result)
                    .that(result).doesNotContain(blocked);
        }
        assertThat(result).contains("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
        assertThat(result).contains("TLS_RSA_WITH_AES_128_CBC_SHA");
        assertThat(result).contains("TLS_U063_UNKNOWN_SUITE");

        List<String> protocols = Arrays.asList(reorder(new String[] {"SSLv3", "TLSv1.2", "X"},
                constant("ORDERED_KNOWN_PROTOCOLS"), constant("BLACKLISTED_PROTOCOLS")));
        assertThat(protocols).doesNotContain("SSLv3");
        assertThat(protocols).contains("TLSv1.2");
        assertThat(protocols).contains("X");
    }

    /**
     * AC-12: known items come first in the order of the known list, then unknown items in their
     * original order. The two known ciphers K1 and K2 are adjacent in the list, so swapping them
     * is visible.
     */
    @Test
    public void reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder() throws Exception {
        String k1 = KNOWN_CIPHERS_BASELINE[0];
        String k2 = KNOWN_CIPHERS_BASELINE[1];
        String k20 = KNOWN_CIPHERS_BASELINE[19];
        String k24 = KNOWN_CIPHERS_BASELINE[23];

        String[] ciphers = reorder(new String[] {k24, "TLS_U063_U1", k1, k20, "TLS_U063_U2", k2},
                constant("ORDERED_KNOWN_CIPHERS"), constant("BLACKLISTED_CIPHERS"));
        assertThat(Arrays.asList(ciphers))
                .containsExactly(k1, k2, k20, k24, "TLS_U063_U1", "TLS_U063_U2").inOrder();

        String[] protocols = reorder(new String[] {"TLSv1", "X", "TLSv1.2", "Y", "TLSv1.1"},
                constant("ORDERED_KNOWN_PROTOCOLS"), constant("BLACKLISTED_PROTOCOLS"));
        assertThat(Arrays.asList(protocols))
                .containsExactly("TLSv1.2", "TLSv1.1", "TLSv1", "X", "Y").inOrder();
    }

    /**
     * AC-13: a null blocklist keeps every input item, and an empty input gives an empty array.
     */
    @Test
    public void reorder_withNullBlocklistAndEmptyInput_behavesAsRecorded() throws Exception {
        String[] known = constant("ORDERED_KNOWN_CIPHERS");

        String[] kept = reorder(new String[] {"A", "B"}, known, null);
        assertThat(Arrays.asList(kept)).containsExactly("A", "B").inOrder();

        String[] empty = reorder(new String[0], known, constant("BLACKLISTED_CIPHERS"));
        assertThat(empty).isEmpty();
    }

    /**
     * AC-14 (ciphers): the known and blocked cipher lists equal the recorded baselines. A snapshot:
     * any edit to either list fails here and must be a conscious, reviewed change.
     */
    @Test
    public void cipherConstants_matchRecordedBaseline() throws Exception {
        assertThat(Arrays.asList(constant("ORDERED_KNOWN_CIPHERS")))
                .containsExactlyElementsIn(KNOWN_CIPHERS_BASELINE).inOrder();
        assertThat(Arrays.asList(constant("BLACKLISTED_CIPHERS")))
                .containsExactlyElementsIn(BLOCKED_CIPHERS_BASELINE).inOrder();
    }

    /**
     * AC-14 (protocols): the known and blocked protocol lists equal the recorded baselines.
     */
    @Test
    public void protocolConstants_matchRecordedBaseline() throws Exception {
        assertThat(Arrays.asList(constant("ORDERED_KNOWN_PROTOCOLS")))
                .containsExactlyElementsIn(KNOWN_PROTOCOLS_BASELINE).inOrder();
        assertThat(Arrays.asList(constant("BLACKLISTED_PROTOCOLS")))
                .containsExactlyElementsIn(BLOCKED_PROTOCOLS_BASELINE).inOrder();
    }

    /**
     * AC-15: the enabled cipher suites of the factory's socket are the provider defaults minus the
     * blocked names (i), with the known suites in the recorded order (ii), all before any unknown
     * suite (iii). (ii) and (iii) are evaluated without the provider-fixed suites F (deviation 7).
     * F is measured live; the test fails, naming D6'', if a bound on F does not hold or if no
     * unknown suite remains once F is removed.
     */
    @Test
    public void createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst()
            throws Exception {
        List<String> defaults = enabledCiphersOf(defaultSocket());
        List<String> hardened = enabledCiphersOf(hardenedSocket());

        // (i) the set equals the provider default set minus the literal blocked names.
        Set<String> expectedSet = new HashSet<>(defaults);
        expectedSet.removeAll(Arrays.asList(BLOCKED_CIPHERS_BASELINE));
        assertThat(new HashSet<>(hardened)).isEqualTo(expectedSet);

        // Bounds on F (deviation 7).
        List<String> fixed = providerFixedSuites();
        for (String name : fixed) {
            assertWithMessage("D6'': provider-fixed suite " + name + " is a baseline known or "
                    + "blocked suite, so removing F could hide a suite the factory orders; F=" + fixed)
                    .that(Arrays.asList(KNOWN_CIPHERS_BASELINE).contains(name)
                            || Arrays.asList(BLOCKED_CIPHERS_BASELINE).contains(name)).isFalse();
            assertWithMessage("D6'': provider-fixed suite " + name + " contains _WITH_, so it is "
                    + "not a signalling or TLS 1.3 suite; F=" + fixed)
                    .that(name.contains("_WITH_")).isFalse();
        }
        List<String> remainder = new ArrayList<>(hardened);
        remainder.removeAll(fixed);
        List<String> knownInRemainder = new ArrayList<>();
        for (String name : KNOWN_CIPHERS_BASELINE) {
            if (remainder.contains(name)) {
                knownInRemainder.add(name);
            }
        }
        assertWithMessage("D6'': after removing the provider-fixed suites " + fixed + " fewer than "
                + "2 known ciphers remain, so the order checks would be empty; remainder="
                + remainder).that(knownInRemainder.size()).isAtLeast(2);
        assertWithMessage("D6'': no unknown suite remains after removing the provider-fixed suites "
                + fixed + ", so check (iii) would be empty; remainder=" + remainder)
                .that(remainder.size() - knownInRemainder.size()).isAtLeast(1);

        // (ii) the known ciphers present appear in the literal order.
        List<String> actualKnownOrder = new ArrayList<>();
        for (String name : remainder) {
            if (Arrays.asList(KNOWN_CIPHERS_BASELINE).contains(name)) {
                actualKnownOrder.add(name);
            }
        }
        assertThat(actualKnownOrder).containsExactlyElementsIn(knownInRemainder).inOrder();

        // (iii) every known cipher precedes every unknown cipher.
        int lastKnown = -1;
        int firstUnknown = Integer.MAX_VALUE;
        for (int i = 0; i < remainder.size(); i++) {
            if (Arrays.asList(KNOWN_CIPHERS_BASELINE).contains(remainder.get(i))) {
                lastKnown = i;
            } else if (firstUnknown == Integer.MAX_VALUE) {
                firstUnknown = i;
            }
        }
        assertWithMessage("a known cipher follows an unknown one in " + remainder)
                .that(lastKnown).isLessThan(firstUnknown);
    }

    /**
     * AC-16 (first test), as changed by U-065 (AC-1 (b), BUG-019 AC-4): SSLv3 is not enabled; the
     * enabled protocols are exactly the provider's supported protocols that are TLS 1.2 or newer;
     * TLS 1.2 comes before every other enabled protocol (SC-5: an order of the array, not a
     * preference). Implicit shape; the layered shape is in the second test.
     */
    @Test
    public void createSocket_enabledProtocols_areProviderSupportedTls12AndNewer_inRecordedOrder()
            throws Exception {
        SSLSocket plain = defaultSocket();
        Set<String> supported;
        try {
            supported = new HashSet<>(Arrays.asList(plain.getSupportedProtocols()));
        } finally {
            closeQuietly(plain);
        }
        assertWithMessage("precondition: the provider supports TLS 1.2; supported=" + supported)
                .that(supported).contains("TLSv1.2");
        SSLSocket socket = hardenedSocket();
        List<String> enabled;
        try {
            enabled = Arrays.asList(socket.getEnabledProtocols());
        } finally {
            closeQuietly(socket);
        }

        assertThat(enabled).doesNotContain("SSLv3");
        Set<String> expected = new HashSet<>(supported);
        expected.retainAll(Arrays.asList(ALLOWED_PROTOCOLS_BASELINE));
        assertWithMessage("enabled protocols must be the supported protocols that are TLS 1.2 or newer;"
                + " supported=" + supported).that(new HashSet<>(enabled)).isEqualTo(expected);
        assertThat(enabled).containsNoDuplicates();
        assertWithMessage("TLSv1.2 must come first in " + enabled).that(enabled.get(0)).isEqualTo("TLSv1.2");
    }

    // DP-2 (decision point: TLS 1.0 and 1.1). Bug id: BUG-019, decided by the product owner on
    // 2026-11-20 (Record 8: no protocol older than TLS 1.2 is enabled, by either factory). Since
    // U-065 this test asserts the decided behaviour; until then it recorded that the two stayed enabled.
    /**
     * AC-16 (second test), as changed by U-065 (AC-1 (a), (b), (c); BUG-019 AC-1, AC-2): none of
     * TLSv1, TLSv1.1, SSLv3 and SSLv2Hello is enabled on the factory's socket, direct or layered, and
     * TLS 1.2 and 1.3 are each enabled if and only if the provider supports them. RFC 8996 deprecates
     * TLS 1.0 and 1.1.
     *
     * <p>Precondition (AC-1 (c)): this provider (Conscrypt) enables TLS 1.0 and 1.1 on its own default
     * socket, so their absence on the factory's sockets can only be the factory's doing. "Enabled" is
     * not "negotiable"; the handshake class shows what is negotiated.
     */
    @Test
    public void createSocket_legacyProtocols_notEnabled_althoughProviderEnablesThemByDefault() throws Exception {
        SSLSocket plain = defaultSocket();
        List<String> supported;
        List<String> providerEnabled;
        try {
            supported = Arrays.asList(plain.getSupportedProtocols());
            providerEnabled = Arrays.asList(plain.getEnabledProtocols());
        } finally {
            closeQuietly(plain);
        }
        for (String name : Arrays.asList("TLSv1", "TLSv1.1")) {
            assertWithMessage("precondition AC-1 (c): the provider's default socket must enable " + name
                    + ", so that its absence is the factory's doing; default enabled=" + providerEnabled)
                    .that(providerEnabled).contains(name);
        }

        SSLSocket direct = hardenedSocket();
        try {
            assertProtocolFloor("default factory, direct socket", supported,
                    Arrays.asList(direct.getEnabledProtocols()));
        } finally {
            closeQuietly(direct);
        }
        try (Layered layered = new Layered(
                new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()), HOST)) {
            assertProtocolFloor("default factory, layered socket", supported,
                    Arrays.asList(layered.socket.getEnabledProtocols()));
        }
    }

    /**
     * AC-17: a socket the factory layers over an existing plain connection gets the same cipher
     * suites and protocols, in the same order, as the implicit-TLS socket. A plain loopback
     * connection with bounded waits; no handshake is run.
     */
    @Test
    public void createSocket_layeredOverPlainSocket_appliesSameHardening() throws Exception {
        SSLSocket implicit = hardenedSocket();
        ServerSocket listener = null;
        Socket client = null;
        Socket accepted = null;
        SSLSocket layered = null;
        try {
            listener = LoopbackTlsTestServer.bindPlainLoopback();
            client = LoopbackTlsTestServer.connectPlain(listener.getLocalPort());
            accepted = listener.accept();
            layered = (SSLSocket) new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication())
                    .createSocket(client, HOST, listener.getLocalPort(), null);

            assertThat(Arrays.asList(layered.getEnabledCipherSuites()))
                    .containsExactlyElementsIn(Arrays.asList(implicit.getEnabledCipherSuites()))
                    .inOrder();
            assertThat(Arrays.asList(layered.getEnabledProtocols()))
                    .containsExactlyElementsIn(Arrays.asList(implicit.getEnabledProtocols()))
                    .inOrder();
        } finally {
            closeQuietly(layered);
            closeQuietly(client);
            closeQuietly(accepted);
            closeQuietly(listener);
            closeQuietly(implicit);
        }
    }

    /**
     * U-065 AC-1 (d): the protocol step, fed a given supported list through a stand-in socket, keeps
     * exactly the supported protocols that are TLS 1.2 or newer, whatever the stand-in enables by
     * default. Case (iii) is a provider that supports TLS 1.2 but leaves it off by default: TLS 1.2 is
     * still enabled ("stay enabled where the platform supports them", Record 8, read literally). Case
     * (iv) shows that an unknown name is never enabled and that a platform with nothing at or above
     * TLS 1.2 gets an empty list (story Scope Concern 7). The allowed list is checked against its
     * literal copy. Mutations: the floor removed (M-P1/M-P8); the list built from the enabled instead
     * of the supported protocols (M-D11r, which only case (iii) can tell).
     */
    @Test
    public void protocolsToEnable_givenSupportedList_keepsOnlySupportedTls12AndNewer() throws Exception {
        assertThat(Arrays.asList(constant("ALLOWED_PROTOCOLS")))
                .containsExactlyElementsIn(ALLOWED_PROTOCOLS_BASELINE).inOrder();
        String[] everything = {"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};

        assertWithMessage("(i) every old name supported and enabled")
                .that(Arrays.asList(protocolsToEnable(stubSocket(everything, everything))))
                .containsExactly("TLSv1.2", "TLSv1.3").inOrder();
        assertWithMessage("(ii) only TLS 1.2 supported")
                .that(Arrays.asList(protocolsToEnable(
                        stubSocket(new String[] {"TLSv1.2"}, new String[] {"TLSv1.2"}))))
                .containsExactly("TLSv1.2");
        assertWithMessage("(iii) TLS 1.2 supported but off by default")
                .that(Arrays.asList(protocolsToEnable(stubSocket(
                        new String[] {"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"},
                        new String[] {"TLSv1.3"}))))
                .containsExactly("TLSv1.2", "TLSv1.3").inOrder();
        assertWithMessage("(iv) nothing at or above TLS 1.2, and an unknown name")
                .that(Arrays.asList(protocolsToEnable(stubSocket(
                        new String[] {"SSLv3", "TLSv1", "TLSv1.1", "U065_UNKNOWN_PROTOCOL"},
                        new String[] {"TLSv1", "TLSv1.1", "U065_UNKNOWN_PROTOCOL"}))))
                .isEmpty();
    }

    /**
     * U-065 AC-1 (e): the branch taken when the provider query failed at class initialisation (both
     * static lists null). The class initialiser cannot be made to fail here: it runs once per class
     * loader and the lists are final. So the code of that branch is called directly, with both lists
     * null, on a stand-in socket that supports and enables every old version: the protocol floor is
     * still applied, computed from the socket itself, and the cipher suites are left as they are.
     * Mutation: the fallback removed (M-F1, the behaviour before U-065: nothing applied).
     */
    @Test
    public void hardenSocket_withNullLists_stillAppliesTheProtocolFloorFromTheSocket() throws Exception {
        String[] everything = {"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};
        SSLSocket socket = stubSocket(everything, everything);

        hardenSocket(socket, null, null);

        ArgumentCaptor<String[]> protocols = ArgumentCaptor.forClass(String[].class);
        verify(socket).setEnabledProtocols(protocols.capture());
        assertThat(Arrays.asList(protocols.getValue())).containsExactly("TLSv1.2", "TLSv1.3").inOrder();
        verify(socket, never()).setEnabledCipherSuites(any());
    }

    /**
     * U-065 AC-3 (b) (BUG-020, Record 9): the pinned-certificate factory's direct socket passes the
     * same three checks as AC-15 above, against the literal baselines, so that the two factories cannot
     * be wrong the same way. Mutations: the pinned cipher handling removed (M-P4); one entry of the
     * cipher order swapped in the pinned factory only (M-P6 (i)).
     */
    @Test
    public void pinnedFactory_createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst()
            throws Exception {
        List<String> defaults = enabledCiphersOf(defaultSocket());
        List<String> pinned = enabledCiphersOf(pinnedSocket());
        assertCipherRule("pinned direct socket", defaults, pinned);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Reads a protected static String[] field of the factory by reflection. */
    private static String[] constant(String name) throws Exception {
        java.lang.reflect.Field field = DefaultTrustedSocketFactory.class.getDeclaredField(name);
        field.setAccessible(true);
        return (String[]) field.get(null);
    }

    /** Calls the protected static reorder of the factory by reflection. */
    private static String[] reorder(String[] input, String[] known, String[] blocked) throws Exception {
        Method method = DefaultTrustedSocketFactory.class.getDeclaredMethod(
                "reorder", String[].class, String[].class, String[].class);
        method.setAccessible(true);
        return (String[]) method.invoke(null, input, known, blocked);
    }

    /** An unconnected socket from a fresh default TLS context, untouched by the factory. */
    private static SSLSocket defaultSocket() throws Exception {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        return (SSLSocket) context.getSocketFactory().createSocket();
    }

    /** The factory's own implicit-TLS socket, unconnected. */
    private static SSLSocket hardenedSocket() throws Exception {
        return (SSLSocket) new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication())
                .createSocket(null, HOST, 993, null);
    }

    /** The enabled cipher suites of the socket; the socket is closed. */
    private static List<String> enabledCiphersOf(SSLSocket socket) {
        try {
            return Arrays.asList(socket.getEnabledCipherSuites());
        } finally {
            closeQuietly(socket);
        }
    }

    /**
     * The suites the provider reports as enabled whatever the caller sets: take a fresh default
     * socket, enable only the first baseline known suite that the provider lists, read back what is
     * enabled and remove that suite. Under Conscrypt this is the three TLS 1.3 suites.
     */
    private static List<String> providerFixedSuites() throws Exception {
        SSLSocket socket = defaultSocket();
        try {
            List<String> enabled = Arrays.asList(socket.getEnabledCipherSuites());
            String first = null;
            for (String name : KNOWN_CIPHERS_BASELINE) {
                if (enabled.contains(name)) {
                    first = name;
                    break;
                }
            }
            if (first == null) {
                fail("D6'': no baseline cipher in the provider default list: " + enabled);
            }
            socket.setEnabledCipherSuites(new String[] {first});
            List<String> fixed = new ArrayList<>(Arrays.asList(socket.getEnabledCipherSuites()));
            fixed.remove(first);
            return fixed;
        } finally {
            closeQuietly(socket);
        }
    }

    /** Calls the protected static protocolsToEnable of the factory by reflection (U-065). */
    private static String[] protocolsToEnable(SSLSocket socket) throws Exception {
        Method method = DefaultTrustedSocketFactory.class.getDeclaredMethod("protocolsToEnable",
                SSLSocket.class);
        method.setAccessible(true);
        return (String[]) method.invoke(null, socket);
    }

    /** Calls the protected static three-argument hardenSocket of the factory by reflection (U-065). */
    private static void hardenSocket(SSLSocket socket, String[] ciphers, String[] protocols)
            throws Exception {
        Method method = DefaultTrustedSocketFactory.class.getDeclaredMethod("hardenSocket",
                SSLSocket.class, String[].class, String[].class);
        method.setAccessible(true);
        method.invoke(null, socket, ciphers, protocols);
    }

    /** A stand-in socket that reports the given supported and default-enabled protocols. */
    private static SSLSocket stubSocket(String[] supported, String[] enabled) {
        SSLSocket socket = mock(SSLSocket.class);
        when(socket.getSupportedProtocols()).thenReturn(supported.clone());
        when(socket.getEnabledProtocols()).thenReturn(enabled.clone());
        return socket;
    }

    /** The pinned-certificate factory's implicit-TLS socket, unconnected (the other-host leaf enrolled). */
    private static SSLSocket pinnedSocket() throws Exception {
        return (SSLSocket) new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(), HOST,
                TlsTestFixtures.certificate(TlsTestFixtures.OTHER_HOST_CERT))
                .createSocket(null, HOST, 993, null);
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

    /**
     * The three checks of AC-15 for a hardened cipher list against the provider defaults, with the
     * same bounds on the provider-fixed suites F. A copy of AC-15's body, kept separate so that AC-15
     * stays as U-063 wrote it (U-065 AC-3 (b)).
     */
    private static void assertCipherRule(String label, List<String> defaults, List<String> hardened)
            throws Exception {
        Set<String> expectedSet = new HashSet<>(defaults);
        expectedSet.removeAll(Arrays.asList(BLOCKED_CIPHERS_BASELINE));
        assertWithMessage(label + ": (i) the set must equal the provider defaults minus the blocked names")
                .that(new HashSet<>(hardened)).isEqualTo(expectedSet);

        List<String> fixed = providerFixedSuites();
        for (String name : fixed) {
            assertWithMessage(label + ": D6'': provider-fixed suite " + name + " is a baseline known or"
                    + " blocked suite; F=" + fixed)
                    .that(Arrays.asList(KNOWN_CIPHERS_BASELINE).contains(name)
                            || Arrays.asList(BLOCKED_CIPHERS_BASELINE).contains(name)).isFalse();
            assertWithMessage(label + ": D6'': provider-fixed suite " + name + " contains _WITH_; F="
                    + fixed).that(name.contains("_WITH_")).isFalse();
        }
        List<String> remainder = new ArrayList<>(hardened);
        remainder.removeAll(fixed);
        List<String> knownInRemainder = new ArrayList<>();
        for (String name : KNOWN_CIPHERS_BASELINE) {
            if (remainder.contains(name)) {
                knownInRemainder.add(name);
            }
        }
        assertWithMessage(label + ": D6'': fewer than 2 known ciphers remain; remainder=" + remainder)
                .that(knownInRemainder.size()).isAtLeast(2);
        assertWithMessage(label + ": D6'': no unknown suite remains; remainder=" + remainder)
                .that(remainder.size() - knownInRemainder.size()).isAtLeast(1);

        List<String> actualKnownOrder = new ArrayList<>();
        for (String name : remainder) {
            if (Arrays.asList(KNOWN_CIPHERS_BASELINE).contains(name)) {
                actualKnownOrder.add(name);
            }
        }
        assertWithMessage(label + ": (ii) the known suites must appear in the literal order")
                .that(actualKnownOrder).containsExactlyElementsIn(knownInRemainder).inOrder();

        int lastKnown = -1;
        int firstUnknown = Integer.MAX_VALUE;
        for (int i = 0; i < remainder.size(); i++) {
            if (Arrays.asList(KNOWN_CIPHERS_BASELINE).contains(remainder.get(i))) {
                lastKnown = i;
            } else if (firstUnknown == Integer.MAX_VALUE) {
                firstUnknown = i;
            }
        }
        assertWithMessage(label + ": (iii) a known cipher follows an unknown one in " + remainder)
                .that(lastKnown).isLessThan(firstUnknown);
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
                closeQuietly(c);
                closeQuietly(a);
                closeQuietly(l);
                throw e;
            }
            listener = l;
            client = c;
            accepted = a;
            socket = s;
        }

        @Override
        public void close() {
            closeQuietly(socket);
            closeQuietly(client);
            closeQuietly(accepted);
            closeQuietly(listener);
        }
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
        if (values == null) {
            return "null";
        }
        return Arrays.asList(values).subList(0, Math.min(values.length, ENTRIES_IN_MESSAGES))
                .toString();
    }
}
