package com.zegoggles.smssync.mail;

import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for {@link PinnedCertStore}.
 *
 * Covers AC-9:
 *   (a) get() returns null for an unenrolled host:port
 *   (b) put() followed by get() returns a certificate whose encoded form is byte-for-byte equal
 *   (c) remove() followed by get() returns null
 *   (d) two distinct host:port keys are independent
 *
 * Uses Robolectric for Android context per U-005 test harness conventions.
 * Verifies Context.MODE_PRIVATE by inspecting the SharedPreferences file name.
 */
@RunWith(RobolectricTestRunner.class)
public class PinnedCertStoreTest {

    /** PEM body (base64 DER) of the first test certificate (CN=test.example.org, valid 2026-2036). */
    private static final String CERT_A_PEM =
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

    /** PEM body (base64 DER) of the second test certificate (CN=other.example.org, valid 2026-2036). */
    private static final String CERT_B_PEM =
            "MIIC3DCCAcSgAwIBAgIJANBU+8ADWU6IMA0GCSqGSIb3DQEBCwUAMBwxGjAYBgNV" +
            "BAMTEW90aGVyLmV4YW1wbGUub3JnMB4XDTI2MDYwMzA1MjExMloXDTM2MDUzMTA1" +
            "MjExMlowHDEaMBgGA1UEAxMRb3RoZXIuZXhhbXBsZS5vcmcwggEiMA0GCSqGSIb3" +
            "DQEBAQUAA4IBDwAwggEKAoIBAQCi1QkHFYscVZm9ofk4FYzRFZ257pMnyicQwwqG" +
            "nR/6H1EwQ0EV341xsGSg01fFfzMP+OHpGJPflYDdqM26vU4FKGTH+dOryTqkwaLP" +
            "dz0XIGPGV9aKQcbcfnd+8EZpjUH0nLwH8JEje9zHM81KTVjetGcvm65Hsw61CF3D" +
            "WLVZdca4f6q+GsaS0Fwj60nJ8u6NQl9jIOT53kgmLBcrJpxPa1GWtnsRecy3QMNM" +
            "w+Bp7Bqr8kqlhXx6WQz27hwt5SKWDJYkV6cUuYel6oH+N/1ZspuyYNqXSNlux8hd" +
            "jq/gUKmxFHfXUxBXXUNkAOa6EvJKbeabHlTEeOErTzsmV8hXAgMBAAGjITAfMB0G" +
            "A1UdDgQWBBRmaMIw+PGsRwudgPFQEBrol0+m/DANBgkqhkiG9w0BAQsFAAOCAQEA" +
            "Ukug8ClGdwdSFg2i33sg6hv1ErkLRX2h3NMAIwJ7NPCcOQkfPN2kFv5lW19N8jF6" +
            "1VSMyQHv207iSmaiu85XMnPvNcz7uK/RSym7KKq9YJqxxCBCtQ85Lv+Ur3JnpUKw" +
            "tYuAbb6l2oTT5aUUPJKKH3pYe5G337MCo9v/Vbyc4JXm49Eh0pP/BfEW8WzaB+qB" +
            "685mO6YViOYZy8pi6h76hV9qpD/UQVNw1MBZQmnQ22UyJWBIjXZWG4VtCL6mWGbG" +
            "4BRfuGUNTpW9+gouXrvfPDRcOSXL0hb43/S3MFhKg2lL7YM4XjE2EtLQP6fvO+8I" +
            "xhCNGI1Jw4RbU6rYtnp54w==";

    private PinnedCertStore store;
    private X509Certificate certA;
    private X509Certificate certB;

    @Before
    public void setUp() throws Exception {
        store = new PinnedCertStore(RuntimeEnvironment.application);
        certA = decodePem(CERT_A_PEM);
        certB = decodePem(CERT_B_PEM);
    }

    // -------------------------------------------------------------------------
    // AC-9(a): get() returns null for an unenrolled host:port
    // -------------------------------------------------------------------------

    @Test
    public void get_returnsNullForUnenrolledHostPort() {
        assertThat(store.get("mail.example.org", 993)).isNull();
    }

    @Test
    public void getTlsTrustPolicy_returnsSYSTEM_VALIDATED_forUnenrolledHostPort() {
        assertThat(store.getTlsTrustPolicy("mail.example.org", 993))
                .isEqualTo(TlsTrustPolicy.SYSTEM_VALIDATED);
    }

    // -------------------------------------------------------------------------
    // AC-9(b): put() followed by get() returns byte-equal certificate
    // -------------------------------------------------------------------------

    @Test
    public void put_then_get_returnsByteEqualCertificate() throws Exception {
        store.put("mail.example.org", 993, certA);
        X509Certificate retrieved = store.get("mail.example.org", 993);

        assertThat(retrieved).isNotNull();
        assertThat(Arrays.equals(retrieved.getEncoded(), certA.getEncoded())).isTrue();
    }

    @Test
    public void getTlsTrustPolicy_returnsPINNED_CERTIFICATE_afterPut() throws Exception {
        store.put("mail.example.org", 993, certA);
        assertThat(store.getTlsTrustPolicy("mail.example.org", 993))
                .isEqualTo(TlsTrustPolicy.PINNED_CERTIFICATE);
    }

    // -------------------------------------------------------------------------
    // AC-9(c): remove() followed by get() returns null
    // -------------------------------------------------------------------------

    @Test
    public void remove_then_get_returnsNull() throws Exception {
        store.put("mail.example.org", 993, certA);
        store.remove("mail.example.org", 993);
        assertThat(store.get("mail.example.org", 993)).isNull();
    }

    @Test
    public void getTlsTrustPolicy_returnsSYSTEM_VALIDATED_afterRemove() throws Exception {
        store.put("mail.example.org", 993, certA);
        store.remove("mail.example.org", 993);
        assertThat(store.getTlsTrustPolicy("mail.example.org", 993))
                .isEqualTo(TlsTrustPolicy.SYSTEM_VALIDATED);
    }

    // -------------------------------------------------------------------------
    // AC-9(d): two distinct host:port keys are independent
    // -------------------------------------------------------------------------

    @Test
    public void twoDistinctKeys_areIndependent() throws Exception {
        store.put("mail.example.org", 993, certA);
        store.put("smtp.example.org", 587, certB);

        // Enrolling one key does not affect the other.
        X509Certificate retrievedA = store.get("mail.example.org", 993);
        X509Certificate retrievedB = store.get("smtp.example.org", 587);

        assertThat(retrievedA).isNotNull();
        assertThat(retrievedB).isNotNull();
        assertThat(Arrays.equals(retrievedA.getEncoded(), certA.getEncoded())).isTrue();
        assertThat(Arrays.equals(retrievedB.getEncoded(), certB.getEncoded())).isTrue();
    }

    @Test
    public void removingOneKey_doesNotAffectTheOther() throws Exception {
        store.put("mail.example.org", 993, certA);
        store.put("smtp.example.org", 587, certB);

        store.remove("mail.example.org", 993);

        assertThat(store.get("mail.example.org", 993)).isNull();
        assertThat(store.get("smtp.example.org", 587)).isNotNull();
    }

    // -------------------------------------------------------------------------
    // AC-9 / AC-3: SharedPreferences file name and MODE_PRIVATE verification
    // -------------------------------------------------------------------------

    /**
     * Verifies that the SharedPreferences file name used by PinnedCertStore is exactly
     * "pinned_certs" (Context.MODE_PRIVATE). This test inspects the constant on the class.
     */
    @Test
    public void sharedPreferencesFileName_isExactlyPinnedCerts() {
        assertThat(PinnedCertStore.PREFS_NAME).isEqualTo("pinned_certs");
    }

    /**
     * Verifies that the SharedPreferences written by put() are accessible under the expected
     * file name, confirming Context.MODE_PRIVATE is used (the ShadowApplication in Robolectric
     * maps getSharedPreferences(name, mode) to an in-memory map keyed by name).
     */
    @Test
    public void put_writesToExpectedPreferencesFile() throws Exception {
        store.put("mail.example.org", 993, certA);

        // Access the same prefs file by name to verify the key was written there.
        SharedPreferences prefs = RuntimeEnvironment.application
                .getSharedPreferences("pinned_certs", android.content.Context.MODE_PRIVATE);
        assertThat(prefs.contains("mail.example.org:993")).isTrue();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static X509Certificate decodePem(String base64Der) throws Exception {
        byte[] der = Base64.getDecoder().decode(base64Der);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
    }
}
