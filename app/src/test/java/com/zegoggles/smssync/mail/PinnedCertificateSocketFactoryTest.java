package com.zegoggles.smssync.mail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import javax.net.ssl.X509TrustManager;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link PinnedCertificateSocketFactory}.
 *
 * Covers AC-8:
 *   - one test that presents the enrolled certificate and expects no exception
 *   - one test that presents a different certificate and expects CertificateException
 * Also covers AC-2 invariants: non-empty checkServerTrusted(), getAcceptedIssuers() not null / not empty.
 */
@RunWith(RobolectricTestRunner.class)
public class PinnedCertificateSocketFactoryTest {

    /**
     * PEM body (base64 DER) of the "enrolled" test certificate (CN=test.example.org,
     * valid 2026-2036). Generated offline with keytool for CI stability.
     */
    private static final String ENROLLED_CERT_PEM =
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

    /**
     * PEM body (base64 DER) of a DIFFERENT test certificate (CN=other.example.org,
     * valid 2026-2036). Used to verify that a mismatched certificate is rejected.
     */
    private static final String OTHER_CERT_PEM =
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

    private X509Certificate enrolledCert;
    private X509Certificate otherCert;
    private PinnedCertificateSocketFactory factory;
    private X509TrustManager trustManager;

    @Before
    public void setUp() throws Exception {
        enrolledCert = decodePem(ENROLLED_CERT_PEM);
        otherCert    = decodePem(OTHER_CERT_PEM);
        factory      = new PinnedCertificateSocketFactory(
                RuntimeEnvironment.application, "test.example.org", enrolledCert);
        trustManager = extractTrustManager(factory);
    }

    // -------------------------------------------------------------------------
    // AC-8 core validation tests
    // -------------------------------------------------------------------------

    /**
     * AC-8: Presenting the enrolled certificate must not throw CertificateException.
     */
    @Test
    public void checkServerTrusted_withEnrolledCert_doesNotThrow() throws Exception {
        // Should complete without exception.
        trustManager.checkServerTrusted(new X509Certificate[]{ enrolledCert }, "RSA");
    }

    /**
     * AC-8: Presenting a DIFFERENT certificate must throw CertificateException.
     */
    @Test
    public void checkServerTrusted_withDifferentCert_throwsCertificateException() {
        try {
            trustManager.checkServerTrusted(new X509Certificate[]{ otherCert }, "RSA");
            fail("Expected CertificateException for mismatched certificate");
        } catch (CertificateException e) {
            // Pass — exception is expected per CNTR-MODERNIZATION-001 §Error Handling.
            assertThat(e.getMessage()).isNotEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // AC-2 invariant tests
    // -------------------------------------------------------------------------

    /**
     * AC-2: getAcceptedIssuers() must never return null.
     */
    @Test
    public void getAcceptedIssuers_isNotNull() {
        assertThat(trustManager.getAcceptedIssuers()).isNotNull();
    }

    /**
     * AC-2: getAcceptedIssuers() must not return an empty array.
     */
    @Test
    public void getAcceptedIssuers_isNotEmpty() {
        assertThat(trustManager.getAcceptedIssuers()).hasLength(1);
    }

    /**
     * AC-2: getAcceptedIssuers() must return the enrolled certificate.
     */
    @Test
    public void getAcceptedIssuers_containsEnrolledCert() {
        X509Certificate[] issuers = trustManager.getAcceptedIssuers();
        assertThat(issuers[0]).isEqualTo(enrolledCert);
    }

    /**
     * AC-2: checkServerTrusted() must not be empty — verified by the fact that it throws
     * CertificateException on a mismatched chain rather than silently passing.
     */
    @Test
    public void checkServerTrusted_isNonEmpty_evidencedByRejection() {
        try {
            trustManager.checkServerTrusted(new X509Certificate[]{ otherCert }, "RSA");
            fail("An empty checkServerTrusted() would silently succeed — this must throw");
        } catch (CertificateException e) {
            // Confirmed: the body performs validation and throws.
        }
    }

    /**
     * Null / empty chain must throw CertificateException (fail closed).
     */
    @Test
    public void checkServerTrusted_withNullChain_throwsCertificateException() {
        try {
            trustManager.checkServerTrusted(null, "RSA");
            fail("Expected CertificateException for null chain");
        } catch (CertificateException e) {
            // Pass.
        }
    }

    /**
     * IC-2: Constructor must not accept a null enrolledCert.
     */
    @Test(expected = IllegalArgumentException.class)
    public void constructor_withNullCert_throwsIllegalArgumentException() {
        new PinnedCertificateSocketFactory(RuntimeEnvironment.application, "host", null);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static X509Certificate decodePem(String base64Der) throws Exception {
        byte[] der = Base64.getDecoder().decode(base64Der);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
    }

    /**
     * Extracts the inner {@link X509TrustManager} from the factory by creating an SSLContext
     * via {@link PinnedCertificateSocketFactory#createSocket} — but we need to test the TrustManager
     * directly. We use reflection on the inner class to avoid coupling the test to implementation details
     * beyond the public API.
     *
     * <p>Since {@code PinnedX509TrustManager} is a private inner class, we use the factory's
     * {@code createSocket(null, ...)} to populate an SSLContext and then retrieve the installed
     * TrustManager via the SSLContext reflection path. However, to keep tests simple and avoid
     * network dependencies, we instead use a direct reflection approach on the factory to obtain
     * the TrustManager instance.
     */
    private static X509TrustManager extractTrustManager(PinnedCertificateSocketFactory factory) throws Exception {
        // Build an SSLContext with the factory's trust manager by invoking createSocket(null,...).
        // Then retrieve the trust manager from the SSLContext. Since SSLContext doesn't expose
        // TrustManagers after init, we instead call the trust manager setup path via reflection
        // on the inner class.
        //
        // Alternative: access the TrustManager directly via a package-private test accessor.
        // PinnedCertificateSocketFactory exposes its TrustManager behavior through createSocket;
        // for direct checkServerTrusted() testing we need the TrustManager instance. We obtain it
        // by constructing an SSLContext that captures the TrustManagers array, which requires
        // hooking into javax.net.ssl via a custom SSLContextSpi — too complex. Instead, we
        // use the simpler approach of a package-private accessor in the production class.
        //
        // Since PinnedCertificateSocketFactory is in the same package as this test class,
        // we add a package-private method getTrustManager() to the factory for testing.
        return factory.getTrustManagerForTesting();
    }
}
