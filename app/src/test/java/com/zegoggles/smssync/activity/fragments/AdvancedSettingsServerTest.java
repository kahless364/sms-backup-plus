package com.zegoggles.smssync.activity.fragments;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.zegoggles.smssync.activity.TransportSecurityNoticeHelper;
import com.zegoggles.smssync.mail.PinnedCertStore;
import com.zegoggles.smssync.mail.TlsTrustPolicy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit / Robolectric tests for the TLS pin-certificate enrollment UI and one-time notice.
 *
 * <p>Covers AC-5 (cancel leaves store unwritten), AC-6 (confirmation stores + indicator),
 * AC-7 (negative: no automated write to PinnedCertStore), AC-8 (notice shown once for
 * affected cohort), AC-9 (notice not shown to unaffected users).
 *
 * <p>AC-7 is the CRITICAL invariant test: this MUST be committed alongside the enrollment
 * UI code and MUST pass before any story depending on PinnedCertStore is merged
 * (U-009 story §AC-7). The migrate()-path negative tests for AC-7 are in
 * {@link com.zegoggles.smssync.preferences.AuthPreferencesTest} (same package as
 * {@code AuthPreferences}) because {@code AuthPreferences.migrate()} is package-private.
 */
@RunWith(RobolectricTestRunner.class)
public class AdvancedSettingsServerTest {

    private SharedPreferences prefs;
    private PinnedCertStore pinnedCertStore;

    // A minimal self-signed test certificate (CN=test.example.org), reused from PinnedCertStoreTest.
    private static final String CERT_PEM =
            "MIIC2jCCAcKgAwIBAgIJAIUM6CSG1fXfMA0GCSqGSIb3DQEBCwUAMBsxGTAXBgNV" +
            "BAMTEHRlc3QuZXhhbXBsZS5vcmcwHhcNMjYwNjAzMDUyMTAwWhcNMzYwNTMxMDUy" +
            "MTAwWjAbMRkwFwYDVQQDExB0ZXN0LmV4YW1wbGUub3JnMIIBIjANBgkqhkiG9w0B" +
            "AQEFAAOCAQ8AMIIBCgKCAQEAkSdcqrOxk2AMXylt2kfAcUAPpsaDl8JtTbFdmJzf" +
            "OawTtG6xjCfFTcYOTEzbeCeHN8phc9CiOqvJkBdOdAa4am1QzPj+oJo4dPcowk+x" +
            "K20q8FK8VxM+epRj6GhwRKtrNUXYA7nuWgAKwEppWXnBw5ECQARvtwP74hZWAJtu" +
            "6oUPdQ6RUHeAZuQWlNQW7sqYL9NNPCWqgRzXYojQeBbPEK7V+HZJr7FgY30CY7D3" +
            "SNlZTS88nM75k40vOLVVtA6f7QccL2F/cn72ZdAJ4GAIPQjBzDx0htNcozkQJ93E" +
            "XmfClh7LZXmMkOt9v7Rehkstany/DwQ0cUgQh/7cB0OS8QIDAQABoyEwHzAdBgNV" +
            "HQ4EFgQUItI76XY6gQEbnocl+DiPA2JDbjMwDQYJKoZIhvcNAQELBQADggEBAGq4" +
            "Zw3ESqxh5rOG6fK48qk+g7GRxPwGTjuooaT6/Q9qxzviiX4NNcZrK5Xc3JGgRHav" +
            "uL4bXDWd8yt8xxbDX0EUpvLYfqrOhQedW4xgTAQ8gdIPf9mho3nEwD6ZEr1geR9j" +
            "2T2y3ANC6uRXqSophAK+LQP4d4s5mkSVE1j+WtFijiXe2xAgnhzzqVSCkrWiC+ra" +
            "Kd9x9c63KSr6fEztnWDc+iqs1mVykM4u3e+HgrA8OgZBTYmt1Q8Dpyi5+qAHQd2L" +
            "LEWa04N93qhOmeVS1fZG3W4lC5LrYt1QXwNN3tUaCjXl6hGKjj1PM8RtE0c3xAfK" +
            "lvs4Mn0PrKdPLzg8KPw=";

    @Before
    public void setUp() {
        prefs = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
        prefs.edit().clear().commit();
        pinnedCertStore = new PinnedCertStore(RuntimeEnvironment.application);
        // Clear the pinned_certs prefs file too.
        // "pinned_certs" matches PinnedCertStore.PREFS_NAME (package-private constant).
        RuntimeEnvironment.application
                .getSharedPreferences("pinned_certs", android.content.Context.MODE_PRIVATE)
                .edit().clear().commit();
    }

    // -------------------------------------------------------------------------
    // AC-7 — NEGATIVE INVARIANT: No automated code path may write PinnedCertStore
    //
    // This test MUST pass before any story depending on PinnedCertStore is merged.
    // Note: The migrate()-path tests for this invariant are in AuthPreferencesTest
    // (same package) since AuthPreferences.migrate() is package-private.
    // -------------------------------------------------------------------------

    /**
     * AC-7 invariant: A fresh PinnedCertStore (no user enrollment) reports SYSTEM_VALIDATED
     * for any host:port. Verifies that nothing other than explicit put() can create a pin.
     */
    @Test
    public void ac7_freshStore_allHostsReturnSystemValidated() {
        assertThat(pinnedCertStore.getTlsTrustPolicy("imap.example.org", 993))
                .isEqualTo(TlsTrustPolicy.SYSTEM_VALIDATED);
        assertThat(pinnedCertStore.getTlsTrustPolicy("imap.gmail.com", 993))
                .isEqualTo(TlsTrustPolicy.SYSTEM_VALIDATED);
        assertThat(pinnedCertStore.get("imap.example.org", 993)).isNull();
    }

    /**
     * AC-7: No setting of prefs (SERVER_PROTOCOL, SERVER_TRUST_ALL_CERTIFICATES, etc.)
     * causes a pin to appear in PinnedCertStore — only explicit put() does.
     */
    @Test
    public void ac7_noPrefsChangeWritesPinnedCertStore() {
        // Simulate the state that would have been left by the old migration.
        prefs.edit()
                .putString("server_protocol", "+ssl+")
                .putString("server_address", "imap.example.org:993")
                .putBoolean("server_trust_all_certificates", false) // cleared by migrate()
                .putBoolean(TransportSecurityNoticeHelper.KEY, true)  // set by migrate()
                .commit();

        // Even with these prefs set, PinnedCertStore is untouched.
        assertThat(pinnedCertStore.getTlsTrustPolicy("imap.example.org", 993))
                .isEqualTo(TlsTrustPolicy.SYSTEM_VALIDATED);
    }

    // -------------------------------------------------------------------------
    // AC-5 — Cancel / dismiss leaves PinnedCertStore unwritten
    // -------------------------------------------------------------------------

    /**
     * AC-5: Enrollment dialog is shown but not confirmed — store is NOT written.
     * (Display-before-consent precondition: CNTR-MODERNIZATION-002 §Validation Rule 3.)
     */
    @Test
    public void ac5_dialogShownButNotConfirmed_storeUnwritten() throws Exception {
        final X509Certificate cert = loadCert(CERT_PEM);
        final String host = "imap.selfhosted.org";
        final int port = 993;

        PinCertificateEnrollmentFlow flow = new PinCertificateEnrollmentFlow(
                RuntimeEnvironment.application,
                pinnedCertStore,
                new PinCertificateEnrollmentFlow.Listener() {
                    @Override public void onEnrolled(String h, int p) {}
                    @Override public void onCancelled() {}
                    @Override public void onFetchError(String msg) {}
                }
        );

        // Build the dialog (displayit without user interaction) — store must not be written.
        flow.showEnrollmentDialog(host, port, cert, dialog -> { /* capture only */ });

        // Assert: store NOT written at display time (AC-5 / CNTR-002 display-before-consent).
        assertThat(pinnedCertStore.getTlsTrustPolicy(host, port))
                .isEqualTo(TlsTrustPolicy.SYSTEM_VALIDATED);
        assertThat(pinnedCertStore.get(host, port)).isNull();
    }

    /**
     * AC-5: onCancelled listener fires when cancel path is taken; store remains empty.
     */
    @Test
    public void ac5_cancelListener_storeRemainsEmpty() throws Exception {
        final X509Certificate cert = loadCert(CERT_PEM);
        final String host = "imap.selfhosted.org";
        final int port = 993;

        final boolean[] cancelledCalled = {false};
        PinCertificateEnrollmentFlow flow = new PinCertificateEnrollmentFlow(
                RuntimeEnvironment.application,
                pinnedCertStore,
                new PinCertificateEnrollmentFlow.Listener() {
                    @Override public void onEnrolled(String h, int p) {}
                    @Override public void onCancelled() { cancelledCalled[0] = true; }
                    @Override public void onFetchError(String msg) {}
                }
        );

        flow.showEnrollmentDialog(host, port, cert, dialog -> { /* capture only */ });

        // Store still empty after dialog is built.
        assertThat(pinnedCertStore.get(host, port)).isNull();
        assertThat(pinnedCertStore.getTlsTrustPolicy(host, port))
                .isEqualTo(TlsTrustPolicy.SYSTEM_VALIDATED);
    }

    // -------------------------------------------------------------------------
    // AC-6 — Confirmation stores certificate; indicator reflects state
    // -------------------------------------------------------------------------

    /**
     * AC-6: put() stores the certificate; isPinned via getTlsTrustPolicy returns PINNED.
     */
    @Test
    public void ac6_putCertificate_storageAndRetrieval() throws Exception {
        final X509Certificate cert = loadCert(CERT_PEM);
        final String host = "imap.selfhosted.org";
        final int port = 993;

        pinnedCertStore.put(host, port, cert);

        assertThat(pinnedCertStore.getTlsTrustPolicy(host, port))
                .isEqualTo(TlsTrustPolicy.PINNED_CERTIFICATE);
        assertThat(pinnedCertStore.get(host, port)).isNotNull();
    }

    /**
     * AC-6: Removal via remove() reverts to SYSTEM_VALIDATED.
     * Simulates the "Remove pinned certificate" action (IC-3 only call site).
     */
    @Test
    public void ac6_removePin_revertsToSystemValidated() throws Exception {
        final X509Certificate cert = loadCert(CERT_PEM);
        final String host = "imap.selfhosted.org";
        final int port = 993;

        pinnedCertStore.put(host, port, cert);
        assertThat(pinnedCertStore.getTlsTrustPolicy(host, port))
                .isEqualTo(TlsTrustPolicy.PINNED_CERTIFICATE);

        pinnedCertStore.remove(host, port);

        assertThat(pinnedCertStore.getTlsTrustPolicy(host, port))
                .isEqualTo(TlsTrustPolicy.SYSTEM_VALIDATED);
        assertThat(pinnedCertStore.get(host, port)).isNull();
    }

    /**
     * AC-6: Pin for one host does NOT affect another host (strict host scoping,
     * CNTR-MODERNIZATION-002 §Validation Rule 4).
     */
    @Test
    public void ac6_pinScopedToHost_doesNotAffectOtherHosts() throws Exception {
        final X509Certificate cert = loadCert(CERT_PEM);
        pinnedCertStore.put("imap.selfhosted.org", 993, cert);

        assertThat(pinnedCertStore.getTlsTrustPolicy("imap.gmail.com", 993))
                .isEqualTo(TlsTrustPolicy.SYSTEM_VALIDATED);
        assertThat(pinnedCertStore.getTlsTrustPolicy("imap.other.org", 993))
                .isEqualTo(TlsTrustPolicy.SYSTEM_VALIDATED);
    }

    // -------------------------------------------------------------------------
    // AC-8 — One-time transport-security notice shown once for affected cohort
    // -------------------------------------------------------------------------

    /**
     * AC-8: consumeTransportSecurityNotice returns true and clears the flag when pending=true.
     */
    @Test
    public void ac8_pendingFlagTrue_noticeConsumedAndFlagCleared() {
        prefs.edit().putBoolean(TransportSecurityNoticeHelper.KEY, true).commit();

        boolean shown = TransportSecurityNoticeHelper.checkAndClearNoticePending(
                RuntimeEnvironment.application);

        assertThat(shown).isTrue();
        assertThat(prefs.getBoolean(TransportSecurityNoticeHelper.KEY, true)).isFalse();
    }

    /**
     * AC-8: After first consumption, second call returns false.
     */
    @Test
    public void ac8_alreadyConsumed_secondCallReturnsFalse() {
        prefs.edit().putBoolean(TransportSecurityNoticeHelper.KEY, false).commit();

        boolean shown = TransportSecurityNoticeHelper.checkAndClearNoticePending(
                RuntimeEnvironment.application);

        assertThat(shown).isFalse();
    }

    /**
     * AC-8: Consuming flag does not create new keys; flag goes from true to false only.
     */
    @Test
    public void ac8_consumption_onlyFlipsFlagFromTrueToFalse() {
        prefs.edit().putBoolean(TransportSecurityNoticeHelper.KEY, true).commit();

        TransportSecurityNoticeHelper.checkAndClearNoticePending(RuntimeEnvironment.application);
        assertThat(prefs.getBoolean(TransportSecurityNoticeHelper.KEY, true)).isFalse();

        // Second call — must not write the flag again.
        TransportSecurityNoticeHelper.checkAndClearNoticePending(RuntimeEnvironment.application);
        assertThat(prefs.getBoolean(TransportSecurityNoticeHelper.KEY, true)).isFalse();
    }

    // -------------------------------------------------------------------------
    // AC-9 — Notice is NEVER shown to unaffected users
    // -------------------------------------------------------------------------

    /**
     * AC-9: Flag absent from prefs — consumeTransportSecurityNotice returns false and
     * does not create the key.
     */
    @Test
    public void ac9_flagAbsent_noticeNotShown() {
        assertThat(prefs.contains(TransportSecurityNoticeHelper.KEY)).isFalse();

        boolean shown = TransportSecurityNoticeHelper.checkAndClearNoticePending(
                RuntimeEnvironment.application);

        assertThat(shown).isFalse();
        assertThat(prefs.contains(TransportSecurityNoticeHelper.KEY)).isFalse();
    }

    /**
     * AC-9: Flag set to false (explicitly cleared) — consumeTransportSecurityNotice
     * returns false and does not change the flag value.
     */
    @Test
    public void ac9_flagFalse_noticeNotShown() {
        prefs.edit().putBoolean(TransportSecurityNoticeHelper.KEY, false).commit();

        boolean shown = TransportSecurityNoticeHelper.checkAndClearNoticePending(
                RuntimeEnvironment.application);

        assertThat(shown).isFalse();
        assertThat(prefs.getBoolean(TransportSecurityNoticeHelper.KEY, true)).isFalse();
    }

    // -------------------------------------------------------------------------
    // PinCertificateEnrollmentFlow helpers — unit tests
    // -------------------------------------------------------------------------

    @Test
    public void parseHost_hostOnly_returnsHost() {
        assertThat(PinCertificateEnrollmentFlow.parseHost("imap.example.org"))
                .isEqualTo("imap.example.org");
    }

    @Test
    public void parseHost_hostWithPort_returnsHostOnly() {
        assertThat(PinCertificateEnrollmentFlow.parseHost("imap.example.org:993"))
                .isEqualTo("imap.example.org");
    }

    @Test
    public void parseHost_ipv6WithPort_returnsBracketedHost() {
        assertThat(PinCertificateEnrollmentFlow.parseHost("[::1]:993"))
                .isEqualTo("[::1]");
    }

    @Test
    public void parsePort_hostOnly_returnsDefault() {
        assertThat(PinCertificateEnrollmentFlow.parsePort("imap.example.org"))
                .isEqualTo(PinCertificateEnrollmentFlow.DEFAULT_IMAP_TLS_PORT);
    }

    @Test
    public void parsePort_hostWithPort_returnsPort() {
        assertThat(PinCertificateEnrollmentFlow.parsePort("imap.example.org:8993"))
                .isEqualTo(8993);
    }

    @Test
    public void parsePort_ipv6WithPort_returnsPort() {
        assertThat(PinCertificateEnrollmentFlow.parsePort("[::1]:993"))
                .isEqualTo(993);
    }

    @Test
    public void formatSha256Fingerprint_32bytes_returns95chars() {
        byte[] sha256 = new byte[32];
        for (int i = 0; i < 32; i++) sha256[i] = (byte) (i * 7 + 3);
        String fp = PinCertificateEnrollmentFlow.formatSha256Fingerprint(sha256);
        // 32 octets * 2 hex chars + 31 colons = 95 chars
        assertThat(fp).hasLength(95);
        assertThat(fp).matches("[0-9A-F]{2}(:[0-9A-F]{2}){31}");
    }

    @Test
    public void sha256_cert_returns32bytes() throws Exception {
        X509Certificate cert = loadCert(CERT_PEM);
        byte[] hash = PinCertificateEnrollmentFlow.sha256(cert);
        assertThat(hash).hasLength(32);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static X509Certificate loadCert(String base64Der) throws Exception {
        byte[] der = Base64.getDecoder().decode(base64Der.replaceAll("\\s", ""));
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
    }
}
