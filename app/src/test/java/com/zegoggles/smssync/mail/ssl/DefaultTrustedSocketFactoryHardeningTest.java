package com.zegoggles.smssync.mail.ssl;

import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

/**
 * Characterisation tests for the cipher and protocol hardening of {@link DefaultTrustedSocketFactory}
 * (story U-063, Group D, acceptance criteria AC-11 to AC-17), run under Robolectric's default TLS
 * provider. At the environment probe (I0b) that provider was Conscrypt.
 *
 * <p><b>What the code does, and what these tests pin down.</b> The factory computes two static lists
 * once per class loader: the provider's default enabled cipher suites with 16 blocked names removed
 * and 24 known names placed first in a fixed order, and the provider's <em>supported</em> protocols
 * with SSLv3 removed and TLS 1.2, 1.1, 1.0 placed first in that order. Every socket it creates gets
 * both lists applied. The tests check the pure reordering function, the four constant lists against
 * literal copies kept here (snapshot tests, so that any edit to a list is a conscious, reviewed
 * change), the lists on a real implicit-TLS socket, and the same lists on a socket layered over a
 * plain connection. Expectations come from the live provider at test time plus those literal
 * baselines; no list of provider suites is hard-coded.
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>The lists here are those of a desktop test provider, not of a device. What a device's
 *       platform enables or reorders is not shown.
 *   <li>"Enabled" is not "negotiable": nothing here connects to a server, so nothing shows which
 *       suite or protocol a handshake would settle on. No handshake is run in this class.
 *   <li>The array of enabled protocols is not a preference order (SC-5). The factory appends
 *       protocols it does not know after TLS 1.0, so TLS 1.3, where supported, sits after TLS 1.0;
 *       the TLS stack does not read the array as a preference list, and this is not claimed to be a
 *       defect. The order is recorded as it is.
 *   <li>The provider fixes some suites in front of whatever the caller sets (under Conscrypt, the
 *       three TLS 1.3 suites). The order checks of AC-15 therefore run on the list with those
 *       provider-fixed suites removed (plan deviation 7), within bounds that are asserted so that the
 *       checks cannot pass on an empty or tiny remainder.
 *   <li>The branches where the factory's static lists are null (provider failure at class
 *       initialisation) are not reached.
 *   <li>The second AC-16 test cannot tell the factory from the provider under this provider (see
 *       its own comment); the form that can is in {@code DefaultTrustedSocketFactoryJdkProviderTest}.
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063 mutation log).
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
 *   <li>AC-16, first test: the protocol part of {@code hardenSocket} removed (M-D13, the order
 *       changes), the hardening call removed (M-D8), or the known order changed (M-D5). Building the
 *       list from the enabled instead of the supported protocols (M-D11) is not detectable here,
 *       because under this provider the two sets are equal; the JDK-provider class kills M-D11.
 *   <li>AC-16, second test: nothing under this provider (criterion status NOTED-AC16 for M-D11 and
 *       M-D12; the JDK-provider class detects both).
 *   <li>AC-17: hardening applied only when no plain socket is passed in (M-D10).
 * </ul>
 *
 * <p><b>Isolation guard A1-G2.</b> The before-method fails, naming A1-G2, if the factory's static
 * lists were computed under the JDK provider (they would contain SSLv2Hello or DHE suites, which
 * this class's provider never produces). That would mean the JDK-provider class's Robolectric
 * sandbox was reused here.
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
     * AC-16 (first test): SSLv3 is not enabled; the enabled protocols are the provider's supported
     * protocols minus SSLv3; among those present TLS 1.2 precedes 1.1 precedes 1.0, and all three
     * precede any other protocol (SC-5: an order of the array, not a preference).
     */
    @Test
    public void createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder()
            throws Exception {
        SSLSocket plain = defaultSocket();
        Set<String> supported;
        try {
            supported = new HashSet<>(Arrays.asList(plain.getSupportedProtocols()));
        } finally {
            closeQuietly(plain);
        }
        SSLSocket socket = hardenedSocket();
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

        List<String> known = Arrays.asList(KNOWN_PROTOCOLS_BASELINE);
        int last = -1;
        for (String name : known) {
            int index = enabled.indexOf(name);
            if (index >= 0) {
                assertWithMessage(name + " must come after the preceding known protocol in "
                        + enabled).that(index).isGreaterThan(last);
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

    // DP-2 (decision point: TLS 1.0 and 1.1 stay enabled). Bug id: BUG-019 (filed). Option A applies:
    // this test passes as a characterisation of present behaviour.
    /**
     * AC-16 (second test): TLS 1.0 and TLS 1.1 are enabled if and only if the provider supports
     * them. RFC 8996 deprecates both. This records that the factory does not remove them.
     *
     * <p><b>Status NOTED-AC16.</b> Under this provider (Conscrypt) TLS 1.0 and 1.1 are supported
     * and also enabled by the provider's own default, so this test cannot tell the factory's
     * behaviour from the provider's: it would pass if the factory enabled nothing at all about them,
     * and it does not detect M-D11 or M-D12. The form of the test that can tell the two apart, on a
     * provider that supports these protocols but leaves them off by default, is
     * {@code createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff} in
     * {@code DefaultTrustedSocketFactoryJdkProviderTest}. "Enabled" is not "negotiable": the JDK's
     * disabled-algorithms setting may still refuse these at handshake, and a device's platform
     * behaviour is not shown.
     */
    @Test
    public void createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem() throws Exception {
        SSLSocket plain = defaultSocket();
        List<String> supported;
        try {
            supported = Arrays.asList(plain.getSupportedProtocols());
        } finally {
            closeQuietly(plain);
        }
        SSLSocket socket = hardenedSocket();
        List<String> enabled;
        try {
            enabled = Arrays.asList(socket.getEnabledProtocols());
        } finally {
            closeQuietly(socket);
        }

        for (String name : Arrays.asList("TLSv1", "TLSv1.1")) {
            assertWithMessage(name + " enabled must equal " + name + " supported; supported="
                    + supported + ", enabled=" + enabled)
                    .that(enabled.contains(name)).isEqualTo(supported.contains(name));
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
