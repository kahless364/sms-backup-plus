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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Characterisation tests for the cipher and protocol hardening of {@link DefaultTrustedSocketFactory}
 * (story U-063, Group D, acceptance criteria AC-11 to AC-17; protocol tests changed and tests added by
 * story U-065; cipher tests changed and added by story U-066), run under Robolectric's default TLS
 * provider. At the environment probes (U-063 I0b, U-066 I2) that provider was Conscrypt.
 *
 * <p><b>What the code does, and what these tests pin down.</b> The factory computes two static lists
 * once per class loader. Cipher suites, since U-066 (finding F-1 of the U-065 security review; product
 * owner decision Record 11, answer 2): the suites of a fixed allowed list of ten that the provider
 * enables by default, in the allowed list's order; every other suite stays off. Protocols: the
 * provider's <em>supported</em> protocols with SSLv3 removed, TLS 1.2, 1.1, 1.0 placed first in that
 * order, and then only TLS 1.2 and TLS 1.3 kept (U-065; BUG-019; Record 8). Every socket a factory
 * creates gets both lists; if they could not be computed at class initialisation, both are computed
 * from the socket itself. The tests check the reordering function (still the protocol step's), the
 * protocol constants and the allowed cipher list against literal copies kept here (snapshots, so that
 * an edit to a list is a conscious, reviewed change), the allowed cipher list against the name forms of
 * CR-014's statement C2 (a check that does not depend on the snapshot), the cipher and protocol steps
 * fed given lists through a stand-in socket, and the exact suites on the direct and layered sockets of
 * both factories (BUG-020; Record 9, "Same as default"). Expected lists are literals; the provider
 * default lists recorded by U-063 appear only as input to the cipher step.
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>The lists here are those of a desktop test provider, not of a device (the two emulators measured
 *       for U-066 list the same 18 default suites as this provider; the owner's devices are not shown).
 *   <li>"Enabled" is not "negotiated": no handshake is run in this class; the handshake classes show
 *       which suite and protocol are negotiated and that a server with only pruned suites is refused.
 *   <li>The array of enabled protocols is not a preference order (SC-5); it is recorded as it is.
 *   <li>This provider reports its three TLS 1.3 suites as enabled whatever is set, first, in its own
 *       order, which is the allowed list's (U-066 DP-4). On this provider the first three entries of the
 *       exact cipher list are therefore the provider's doing; the tests assert that fact, and the JDK
 *       provider class shows the factory keeping them where nothing is provider-fixed.
 *   <li>The class initialiser's failure branch (both static lists null) cannot be made to run: it runs
 *       once per class loader and the lists are final. Its code is called directly with null lists.
 *   <li>Under this provider the supported protocols equal the default enabled ones, so the stand-in
 *       socket tests carry the "supported, not enabled" checks for protocols and the "enabled by
 *       default, not supported" check for cipher suites.
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063, U-065 and U-066 mutation logs).
 * <ul>
 *   <li>AC-11 to AC-13 (reorder): M-D1 to M-D7, M-D15 of U-063, which ran against the code before U-065;
 *       since U-066 the cipher lists are test literals, and reorder is the protocol step's function.
 *   <li>U-066 exact lists (both factories, both shapes): a pruned suite put back (M-X1 (a), (b)), the
 *       allow filter removed (M-X2), the signalling value removed (M-X4 (ii)), the step removed from one
 *       factory (M-X5), the layered shape skipped (M-X6, default factory), two entries swapped (M-X8).
 *       Removing the TLS 1.3 names (M-X4 (i)) cannot fail them on this provider (provider-fixed).
 *   <li>U-066 allowed-list snapshot and name forms: M-X1 (a) to (e), M-X4 (i), (ii) (snapshot), M-X8
 *       (snapshot); the forms check also fails under M-X1 and M-X4 (i).
 *   <li>U-066 cipher step: M-X1, M-X2, M-X4, M-X8, and the step reading the supported instead of the
 *       enabled suites (M-X9).
 *   <li>U-066 null-lists test: the cipher fallback removed (M-X7); M-X9 in its empty case.
 *   <li>U-065 protocol tests: M-P1/M-P8, M-P6 (ii), M-D10, M-D11r, M-F1, as recorded there.
 * </ul>
 *
 * <p><b>Isolation guard A1-G2.</b> The before-method fails, naming A1-G2, if the TLS context's provider
 * is not Conscrypt, which would mean the JDK-provider class's Robolectric sandbox was reused here. Until
 * U-066 the guard read this from the factory's static cipher list (a DHE suite appeared only under the
 * JDK provider). Since U-066 both providers give the same ten allowed suites, so the cached lists no
 * longer differ by provider and cannot be wrong for this class, and the guard checks the live provider
 * instead (U-066 AC-8). The SSLv2Hello clause on the protocol list can no longer fire since U-065 and is
 * kept unchanged. On the wrong provider the preconditions of the exact-list tests fail as well.
 *
 * <p><b>After the CR-009 move:</b> a missing field, method or class means this test's seam needs
 * retargeting to the moved class; a failed behavioural assertion is a regression, and the test is
 * never edited to pass.
 */
@RunWith(RobolectricTestRunner.class)
public class DefaultTrustedSocketFactoryHardeningTest {

    private static final String HOST = "imap.right.test";
    private static final int ENTRIES_IN_MESSAGES = 5;

    /**
     * The allowed cipher suites of the factory, in order (snapshot; U-066, DP-1 option A, DP-4). A literal,
     * never read from the factory, so a wrong constant cannot make a test agree with itself.
     */
    private static final String[] ALLOWED_CIPHERS_BASELINE = {
            "TLS_AES_128_GCM_SHA256",
            "TLS_AES_256_GCM_SHA384",
            "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
    };

    /**
     * The factory's 24 ordered known cipher names before U-066 (the U-063 snapshot), kept as test input:
     * the reorder tests feed them to reorder, which the protocol step still uses, and the cipher step's
     * input test feeds them to the cipher step. The factory no longer holds this list.
     */
    private static final String[] KNOWN_CIPHERS_BEFORE_U066 = {
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

    /** The factory's 16 blocked cipher names before U-066 (the U-063 snapshot), kept as test input as above. */
    private static final String[] BLOCKED_CIPHERS_BEFORE_U066 = {
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

    /** This provider's (Conscrypt's) default-enabled suites as recorded by U-063 (I0-probe.txt:30). Input only. */
    private static final String[] CONSCRYPT_DEFAULT_CIPHERS_RECORDED = {
            "TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384", "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA",
            "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
    };

    /** The JDK provider's (SunJSSE's) default-enabled suites as recorded by U-063 (I0c-diag.txt:14). Input only. */
    private static final String[] JDK_DEFAULT_CIPHERS_RECORDED = {
            "TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256", "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_DHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384", "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA", "TLS_EMPTY_RENEGOTIATION_INFO_SCSV",
    };

    /** One name each of W8 (null), W9 (anonymous) and W10 (export), held by no provider and no list (AC-3 (c)). */
    private static final String[] NULL_ANON_EXPORT_EXAMPLES = {
            "TLS_RSA_WITH_NULL_SHA", "TLS_DH_anon_WITH_AES_128_CBC_SHA", "TLS_RSA_EXPORT_WITH_RC4_40_MD5",
    };

    /** Default-enabled suites none of which is allowed (CR-014 C2: the producer then enables none). */
    private static final String[] NO_ALLOWED_SUITE = {
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
    };

    /**
     * Names that a deny-list by W1 to W12 would let through and that the name forms of CR-014 C2 must
     * refuse (CR-014, Check of Amendment 1, point 1): static DH, PSK, CCM, CCM_8, ARIA and Camellia GCM,
     * integrity-only TLS 1.3 suites and the fallback signalling value.
     */
    private static final String[] FORM_ESCAPES = {
            "TLS_DH_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_PSK_WITH_AES_128_GCM_SHA256",
            "TLS_PSK_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_AES_128_CCM",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8", "TLS_ECDHE_RSA_WITH_ARIA_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_CAMELLIA_128_GCM_SHA256", "TLS_AES_128_CCM_SHA256",
            "TLS_AES_128_CCM_8_SHA256", "TLS_SHA256_SHA256", "TLS_SHA384_SHA384", "TLS_FALLBACK_SCSV",
    };

    /** The renegotiation signalling value: listed with the suites, never negotiated, exempt from W1 to W12. */
    private static final String SCSV = "TLS_EMPTY_RENEGOTIATION_INFO_SCSV";

    /** The weak categories of U-066 AC-3. */
    private static final String[] WEAK_CATEGORY_IDS = {
            "W1", "W2", "W3", "W4", "W5", "W6", "W7", "W8", "W9", "W10", "W11", "W12",
    };

    private static final String[] KNOWN_PROTOCOLS_BASELINE = {"TLSv1.2", "TLSv1.1", "TLSv1"};
    private static final String[] BLOCKED_PROTOCOLS_BASELINE = {"SSLv3"};

    /** The allowed protocols of the factory, in order (snapshot; U-065, Record 8). */
    private static final String[] ALLOWED_PROTOCOLS_BASELINE = {"TLSv1.2", "TLSv1.3"};

    /** Protocol versions older than TLS 1.2 that must never be enabled (Record 8). */
    private static final String[] OLD_PROTOCOLS = {"TLSv1", "TLSv1.1", "SSLv3", "SSLv2Hello"};

    /**
     * Isolation guard A1-G2: this class must run on its own provider, Conscrypt. Until U-066 the guard read
     * the provider from the factory's cipher list (a DHE suite meant the JDK provider); since U-066 both
     * providers give the same ten allowed suites, so it checks the TLS context's provider (U-066 AC-8).
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
        if (!"Conscrypt".equals(provider)) {
            fail("A1-G2: this class must run on the Conscrypt provider, but the TLS context provider is "
                    + provider + " (the JDK-provider class's sandbox was probably reused); the factory's"
                    + " ENABLED_CIPHERS first entries=" + firstEntries(ciphers));
        }
    }

    /**
     * AC-11: reorder removes every blocked item, for ciphers and for protocols. Since U-066 the cipher
     * lists are the factory's lists before U-066, given as literals (the factory no longer holds them);
     * reorder is still the protocol step's function.
     */
    @Test
    public void reorder_removesEveryBlockedItem() throws Exception {
        List<String> input = new ArrayList<>(Arrays.asList(BLOCKED_CIPHERS_BEFORE_U066));
        input.add("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
        input.add("TLS_RSA_WITH_AES_128_CBC_SHA");
        input.add("TLS_U063_UNKNOWN_SUITE");

        List<String> result = Arrays.asList(reorder(input.toArray(new String[0]),
                KNOWN_CIPHERS_BEFORE_U066, BLOCKED_CIPHERS_BEFORE_U066));

        for (String blocked : BLOCKED_CIPHERS_BEFORE_U066) {
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
     * is visible. Since U-066 the cipher lists are the factory's lists before U-066, given as literals
     * (the factory no longer holds them); reorder is still the protocol step's function.
     */
    @Test
    public void reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder() throws Exception {
        String k1 = KNOWN_CIPHERS_BEFORE_U066[0];
        String k2 = KNOWN_CIPHERS_BEFORE_U066[1];
        String k20 = KNOWN_CIPHERS_BEFORE_U066[19];
        String k24 = KNOWN_CIPHERS_BEFORE_U066[23];

        String[] ciphers = reorder(new String[] {k24, "TLS_U063_U1", k1, k20, "TLS_U063_U2", k2},
                KNOWN_CIPHERS_BEFORE_U066, BLOCKED_CIPHERS_BEFORE_U066);
        assertThat(Arrays.asList(ciphers))
                .containsExactly(k1, k2, k20, k24, "TLS_U063_U1", "TLS_U063_U2").inOrder();

        String[] protocols = reorder(new String[] {"TLSv1", "X", "TLSv1.2", "Y", "TLSv1.1"},
                constant("ORDERED_KNOWN_PROTOCOLS"), constant("BLACKLISTED_PROTOCOLS"));
        assertThat(Arrays.asList(protocols))
                .containsExactly("TLSv1.2", "TLSv1.1", "TLSv1", "X", "Y").inOrder();
    }

    /**
     * AC-13: a null blocklist keeps every input item, and an empty input gives an empty array. Since
     * U-066 the cipher lists are the factory's lists before U-066, given as literals (the factory no
     * longer holds them); reorder is still the protocol step's function.
     */
    @Test
    public void reorder_withNullBlocklistAndEmptyInput_behavesAsRecorded() throws Exception {
        String[] known = KNOWN_CIPHERS_BEFORE_U066;

        String[] kept = reorder(new String[] {"A", "B"}, known, null);
        assertThat(Arrays.asList(kept)).containsExactly("A", "B").inOrder();

        String[] empty = reorder(new String[0], known, BLOCKED_CIPHERS_BEFORE_U066);
        assertThat(empty).isEmpty();
    }

    /**
     * AC-14 (ciphers), as changed by U-066 (AC-7 (a)): the factory's allowed cipher list equals the recorded
     * baseline, in order. A snapshot: any edit fails here and must be a conscious, reviewed change. The two
     * lists recorded here until U-066 (24 known, 16 blocked) are kept above as test input. Why each name may
     * be on the list is checked by allowedCipherConstant_holdsOnlyTheAllowedNameForms, which does not depend
     * on this snapshot.
     */
    @Test
    public void cipherConstants_matchRecordedBaseline() throws Exception {
        assertThat(Arrays.asList(constant("ALLOWED_CIPHERS")))
                .containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
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
     * U-066 AC-1 (this provider), AC-3 (a): the default factory's direct and layered sockets enable exactly
     * the ten allowed suites, in the allowed order. Preconditions (AC-1 (a)): this provider's own default
     * socket enables ECDHE suites with CBC and SHA-1 and RSA key-exchange suites, and its list is not the
     * allowed list, so neither their absence nor the order can be the provider's. The three TLS 1.3 suites
     * are provider-fixed here (asserted, DP-4): the factory's part is the other seven. On another provider
     * the preconditions fail (AC-8 (d)). Was AC-15 of U-063 (provider defaults minus the blocked names, known
     * names first), whose rule the allow-list replaced.
     */
    @Test
    public void createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes() throws Exception {
        assertConscryptDefaultsOfferPrunedSuites(enabledCiphersOf(defaultSocket()));
        assertTls13SuitesAreProviderFixed();
        assertAllowedCipherList("default factory, direct socket", enabledCiphersOf(hardenedSocket()));
        try (Layered layered = new Layered(
                new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()), HOST)) {
            assertAllowedCipherList("default factory, layered socket",
                    Arrays.asList(layered.socket.getEnabledCipherSuites()));
        }
    }

    /**
     * U-066 AC-3 (c), CR-014 C1 and C2: the cipher step, fed through a stand-in socket, keeps exactly the
     * allowed suites that the socket enables by default, in the allowed order. (i) The input is the union of
     * the two recorded provider defaults, the 40 names of the factory's lists before U-066 and one null,
     * anonymous and export name each (62 names, every category W1 to W12 present, asserted first); (ii) the
     * same names reversed; (iii) a socket that supports every name but enables four by default gives only
     * the allowed ones among those four (never a suite the platform only supports: C1); (iv) a socket that
     * enables none of the allowed suites gives an empty list, with no fallback (C2; the model is case (iv) of
     * the protocol step's test). Mutations: M-X1, M-X2, M-X4, M-X8 ((i)); M-X9 ((iii)).
     */
    @Test
    public void ciphersToEnable_givenDefaultEnabledList_keepsOnlyAllowedSuitesInAllowedOrder() throws Exception {
        String[] protocols = {"TLSv1.2", "TLSv1.3"};
        String[] input = cipherStepInput();
        assertWithMessage("precondition: the input holds 62 distinct names").that(input).hasLength(62);
        for (String category : WEAK_CATEGORY_IDS) {
            boolean present = false;
            for (String name : input) {
                if (weakCategories(name).contains(category)) {
                    present = true;
                    break;
                }
            }
            assertWithMessage("precondition AC-3 (c): the input holds a suite of category " + category)
                    .that(present).isTrue();
        }

        assertWithMessage("(i) provider defaults, the lists before U-066, one null, anonymous and export name")
                .that(Arrays.asList(ciphersToEnable(stubSocket(protocols, protocols, input, input))))
                .containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
        List<String> reversed = new ArrayList<>(Arrays.asList(input));
        Collections.reverse(reversed);
        String[] backwards = reversed.toArray(new String[0]);
        assertWithMessage("(ii) the same names reversed: the allowed order, not the platform's")
                .that(Arrays.asList(ciphersToEnable(stubSocket(protocols, protocols, backwards, backwards))))
                .containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
        String[] fourByDefault = {
                "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384"};
        assertWithMessage("(iii) only suites the socket enables by default, never one it only supports (C1)")
                .that(Arrays.asList(ciphersToEnable(stubSocket(protocols, protocols, fourByDefault, input))))
                .containsExactly("TLS_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                        "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256").inOrder();
        assertWithMessage("(iv) no allowed suite enabled by default: an empty list, no fallback (C2)")
                .that(ciphersToEnable(stubSocket(protocols, protocols, NO_ALLOWED_SUITE, input))).isEmpty();
    }

    /**
     * U-066 AC-3 (b), CR-014 C2: every name on the factory's allowed list, read by reflection, is of one of
     * the allowed name forms (TLS 1.3: starts TLS_AES_ and contains _GCM_, or starts TLS_CHACHA20_POLY1305_;
     * TLS 1.2: TLS_ECDHE_RSA_WITH_ or TLS_ECDHE_ECDSA_WITH_ followed by AES_128_GCM_, AES_256_GCM_ or
     * CHACHA20_POLY1305_; or the signalling value), matches none of W1 to W12, appears once, and the list
     * holds a TLS 1.3 suite and a TLS 1.2 suite (CR-014 Part H 4, V2). Not a snapshot: it says why a name may
     * be there. First the form check is shown able to refuse: every non-allowed name of the step's input and
     * each name a deny-list would miss is refused by it. Mutations: M-X1 (a) to (e), M-X4 (i).
     */
    @Test
    public void allowedCipherConstant_holdsOnlyTheAllowedNameForms() throws Exception {
        List<String> literal = Arrays.asList(ALLOWED_CIPHERS_BASELINE);
        for (String name : cipherStepInput()) {
            if (!literal.contains(name)) {
                assertWithMessage("check of the forms: " + name + " is not allowed, so no allowed form may"
                        + " match it").that(hasAllowedForm(name)).isFalse();
            }
        }
        for (String name : FORM_ESCAPES) {
            assertWithMessage("check of the forms: " + name + " must not match an allowed form")
                    .that(hasAllowedForm(name)).isFalse();
        }

        List<String> allowed = Arrays.asList(constant("ALLOWED_CIPHERS"));
        assertWithMessage("the allowed list holds no name twice; allowed=" + allowed)
                .that(allowed).containsNoDuplicates();
        boolean tls13 = false;
        boolean tls12 = false;
        for (String name : allowed) {
            assertWithMessage("CR-014 C2: " + name + " is not of an allowed name form")
                    .that(hasAllowedForm(name)).isTrue();
            assertWithMessage("AC-3 (b): " + name + " matches a weak category")
                    .that(weakCategories(name)).isEmpty();
            tls13 |= name.startsWith("TLS_AES_") || name.startsWith("TLS_CHACHA20_POLY1305_");
            tls12 |= name.startsWith("TLS_ECDHE_");
        }
        assertWithMessage("CR-014 C2 and V2: the allowed list must hold a TLS 1.3 suite; allowed=" + allowed)
                .that(tls13).isTrue();
        assertWithMessage("CR-014 C2 and V2: the allowed list must hold a TLS 1.2 suite; allowed=" + allowed)
                .that(tls12).isTrue();
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
     * U-066 AC-4 (DP-3 option 1, CR-014 C4), was U-065 AC-1 (e): the branch taken when the provider query
     * failed at class initialisation (both static lists null). The initialiser cannot be made to fail here
     * (it runs once per class loader and the lists are final), so the branch's code is called directly on
     * stand-in sockets. (a) A socket that enables the step's 62-name input by default and supports every old
     * protocol: the suites set, in one call, are exactly the allowed list, and the protocols are exactly TLS
     * 1.2 and 1.3. (b) A socket that enables none of the allowed suites: the branch sets an empty list and
     * does not fall back to the platform's suites. (c) Outside the null branch, a computed list that is empty
     * (a platform whose defaults hold no allowed suite) is set as it is and never replaced by the platform's
     * suites (CR-014 C2). Until U-066 this test asserted that the suites were left as they were. Mutations:
     * the cipher fallback removed (M-X7); the step reading the supported suites (M-X9, case (b)); an empty
     * list skipped (M-X10, case (c)); the protocol fallback removed (M-F1 of U-065).
     */
    @Test
    public void hardenSocket_withNullLists_appliesTheAllowedSuitesAndTheProtocolFloorFromTheSocket()
            throws Exception {
        String[] everything = {"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};
        String[] input = cipherStepInput();
        SSLSocket socket = stubSocket(everything, everything, input, input);

        hardenSocket(socket, null, null);

        ArgumentCaptor<String[]> suites = ArgumentCaptor.forClass(String[].class);
        verify(socket).setEnabledCipherSuites(suites.capture());
        assertWithMessage("(a) suites set in the null branch").that(Arrays.asList(suites.getValue()))
                .containsExactlyElementsIn(ALLOWED_CIPHERS_BASELINE).inOrder();
        ArgumentCaptor<String[]> protocols = ArgumentCaptor.forClass(String[].class);
        verify(socket).setEnabledProtocols(protocols.capture());
        assertThat(Arrays.asList(protocols.getValue())).containsExactly("TLSv1.2", "TLSv1.3").inOrder();

        SSLSocket none = stubSocket(everything, everything, NO_ALLOWED_SUITE, input);
        hardenSocket(none, null, null);
        ArgumentCaptor<String[]> noneSuites = ArgumentCaptor.forClass(String[].class);
        verify(none).setEnabledCipherSuites(noneSuites.capture());
        assertWithMessage("(b) no allowed suite enabled by default: the branch enables none, no fallback")
                .that(noneSuites.getValue()).isEmpty();

        SSLSocket emptyList = stubSocket(everything, everything, input, input);
        hardenSocket(emptyList, new String[0], new String[] {"TLSv1.2", "TLSv1.3"});
        ArgumentCaptor<String[]> emptySuites = ArgumentCaptor.forClass(String[].class);
        verify(emptyList).setEnabledCipherSuites(emptySuites.capture());
        assertWithMessage("(c) a computed empty list is set as it is, never replaced by the platform's suites (C2)")
                .that(emptySuites.getValue()).isEmpty();
    }

    /**
     * U-066 AC-2 (a) (this provider), AC-3 (a), was U-065 AC-3 (b): the pinned-certificate factory's direct
     * and layered sockets enable exactly the ten allowed suites in the allowed order, checked against the
     * literal, so that the two factories cannot be wrong the same way (BUG-020, Record 9). Preconditions as
     * the default factory's test. Mutations: the pinned call to the shared step removed (M-X5 (i)); M-X1
     * (a), (b); M-X2; M-X4 (ii); M-X8.
     */
    @Test
    public void pinnedFactory_createSocket_enabledCipherSuites_areExactlyTheAllowedList_bothShapes()
            throws Exception {
        assertConscryptDefaultsOfferPrunedSuites(enabledCiphersOf(defaultSocket()));
        assertTls13SuitesAreProviderFixed();
        assertAllowedCipherList("pinned factory, direct socket", enabledCiphersOf(pinnedSocket()));
        try (Layered layered = new Layered(pinnedFactory(), HOST)) {
            assertAllowedCipherList("pinned factory, layered socket",
                    Arrays.asList(layered.socket.getEnabledCipherSuites()));
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
     * socket, enable only the first allowed suite with _WITH_ (a TLS 1.2 suite) that the provider lists, read back what is
     * enabled and remove that suite. Under Conscrypt this is the three TLS 1.3 suites.
     */
    private static List<String> providerFixedSuites() throws Exception {
        SSLSocket socket = defaultSocket();
        try {
            List<String> enabled = Arrays.asList(socket.getEnabledCipherSuites());
            String first = null;
            for (String name : ALLOWED_CIPHERS_BASELINE) {
                if (name.contains("_WITH_") && enabled.contains(name)) {
                    first = name;
                    break;
                }
            }
            if (first == null) {
                fail("DP-4: no allowed TLS 1.2 suite in the provider default list: " + enabled);
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

    /** The pinned-certificate factory (the other-host leaf enrolled; no handshake in this class). */
    private static PinnedCertificateSocketFactory pinnedFactory() throws Exception {
        return new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(), HOST,
                TlsTestFixtures.certificate(TlsTestFixtures.OTHER_HOST_CERT));
    }

    /** A stand-in socket with the given protocols and the given default-enabled and supported suites (U-066). */
    private static SSLSocket stubSocket(String[] supported, String[] enabled, String[] enabledSuites,
                                        String[] supportedSuites) {
        SSLSocket socket = stubSocket(supported, enabled);
        when(socket.getEnabledCipherSuites()).thenReturn(enabledSuites.clone());
        when(socket.getSupportedCipherSuites()).thenReturn(supportedSuites.clone());
        return socket;
    }

    /** Calls the protected static ciphersToEnable of the factory by reflection (U-066). */
    private static String[] ciphersToEnable(SSLSocket socket) throws Exception {
        Method method = DefaultTrustedSocketFactory.class.getDeclaredMethod("ciphersToEnable", SSLSocket.class);
        method.setAccessible(true);
        return (String[]) method.invoke(null, socket);
    }

    /**
     * AC-1 (a) for this provider: its own default socket enables suites of the categories the factory
     * prunes here (ECDHE with CBC and SHA-1; RSA key exchange) and is not the allowed list, so their absence
     * on the factory's socket, and its order, can only be the factory's doing.
     */
    private static void assertConscryptDefaultsOfferPrunedSuites(List<String> defaults) {
        assertWithMessage("precondition AC-1 (a): the provider's default socket must enable an ECDHE suite with"
                + " CBC and SHA-1; defaults=" + defaults)
                .that(containsMatch(defaults, "TLS_ECDHE_", "_CBC_SHA")).isTrue();
        assertWithMessage("precondition AC-1 (a): the provider's default socket must enable an RSA"
                + " key-exchange suite; defaults=" + defaults)
                .that(containsMatch(defaults, "TLS_RSA_", "")).isTrue();
        assertWithMessage("precondition AC-1 (a): the provider's own list must not be the allowed list")
                .that(defaults).isNotEqualTo(Arrays.asList(ALLOWED_CIPHERS_BASELINE));
    }

    /**
     * DP-4, an environment fact: this provider reports exactly the three TLS 1.3 suites as enabled whatever
     * is set, in its own order, which is the allowed list's first three entries (U-066 probe I2).
     */
    private static void assertTls13SuitesAreProviderFixed() throws Exception {
        assertWithMessage("environment (DP-4): the suites this provider reports whatever is set")
                .that(providerFixedSuites())
                .containsExactly(ALLOWED_CIPHERS_BASELINE[0], ALLOWED_CIPHERS_BASELINE[1],
                        ALLOWED_CIPHERS_BASELINE[2]).inOrder();
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
     * The input of AC-3 (c), 62 distinct names in this order: the two recorded provider defaults (18 and 31),
     * the 40 names of the factory's lists before U-066, one null, anonymous and export name.
     */
    private static String[] cipherStepInput() {
        Set<String> names = new LinkedHashSet<>();
        Collections.addAll(names, CONSCRYPT_DEFAULT_CIPHERS_RECORDED);
        Collections.addAll(names, JDK_DEFAULT_CIPHERS_RECORDED);
        Collections.addAll(names, KNOWN_CIPHERS_BEFORE_U066);
        Collections.addAll(names, BLOCKED_CIPHERS_BEFORE_U066);
        Collections.addAll(names, NULL_ANON_EXPORT_EXAMPLES);
        return names.toArray(new String[0]);
    }

    /**
     * The weak categories of U-066 AC-3 that a name matches: W1 CBC, W2 SHA-1 MAC, W3 RSA key exchange, W4
     * static ECDH, W5 DHE, W6 DES and 3DES, W7 DSS, W8 NULL, W9 anonymous, W10 export, W11 RC4, W12 MD5. The
     * renegotiation signalling value is exempt by name: it is not a cipher.
     */
    private static List<String> weakCategories(String name) {
        List<String> found = new ArrayList<>();
        if (SCSV.equals(name)) {
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

    /** True if {@code name} is of one of the allowed name forms of CR-014 C2. */
    private static boolean hasAllowedForm(String name) {
        if (SCSV.equals(name)) {
            return true;
        }
        if (name.startsWith("TLS_AES_") && name.contains("_GCM_")) {
            return true;
        }
        if (name.startsWith("TLS_CHACHA20_POLY1305_")) {
            return true;
        }
        for (String exchange : new String[] {"TLS_ECDHE_RSA_WITH_", "TLS_ECDHE_ECDSA_WITH_"}) {
            if (name.startsWith(exchange)) {
                String rest = name.substring(exchange.length());
                return rest.startsWith("AES_128_GCM_") || rest.startsWith("AES_256_GCM_")
                        || rest.startsWith("CHACHA20_POLY1305_");
            }
        }
        return false;
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
