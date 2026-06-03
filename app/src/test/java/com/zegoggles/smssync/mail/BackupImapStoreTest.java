package com.zegoggles.smssync.mail;

import android.annotation.SuppressLint;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static com.google.common.truth.Truth.assertThat;
import static com.zegoggles.smssync.mail.BackupImapStore.isValidImapFolder;
import static com.zegoggles.smssync.mail.BackupImapStore.isValidUri;

@RunWith(RobolectricTestRunner.class)
@SuppressLint("AuthLeak")
public class BackupImapStoreTest {

    /**
     * PEM body of a test-only self-signed certificate (CN=test.example.org, RSA-2048,
     * SHA256withRSA, valid until 2036-05-31). Generated offline; expiry far-future so
     * CI does not fail due to clock drift. Used to exercise the PinnedCertificateSocketFactory
     * constructor path in BackupImapStore.
     */
    private static final String TEST_CERT_PEM =
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

    /** Decode the hardcoded PEM body into an X509Certificate. */
    private X509Certificate loadTestCert() throws Exception {
        byte[] der = Base64.getDecoder().decode(TEST_CERT_PEM);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
    }

    @Test public void shouldTestForValidUri() throws Exception {
        assertThat(isValidUri("imap+ssl+://xoauth:foooo@imap.gmail.com:993")).isTrue();
        assertThat(isValidUri("imap://xoauth:foooo@imap.gmail.com")).isTrue();
        assertThat(isValidUri("imap+ssl+://xoauth:user:token@:993")).isFalse();
        assertThat(isValidUri("imap+ssl://user%40domain:password@imap.gmail.com:993")).isFalse();
        assertThat(isValidUri("imap+tls+://user:password@imap.gmail.com:993")).isTrue();
        assertThat(isValidUri("imap+tls://user:password@imap.gmail.com:993")).isFalse();
        assertThat(isValidUri("imap://user:password@imap.gmail.com:993")).isTrue();
        assertThat(isValidUri("http://xoauth:foooo@imap.gmail.com:993")).isFalse();
    }

    @Test public void shouldTestForValidFolder() throws Exception {
        assertThat(isValidImapFolder(null)).isFalse();
        assertThat(isValidImapFolder("")).isFalse();
        assertThat(isValidImapFolder("foo")).isTrue();
        assertThat(isValidImapFolder("foo bar")).isTrue();
        assertThat(isValidImapFolder(" foo")).isFalse();
        assertThat(isValidImapFolder("foo ")).isFalse();
        assertThat(isValidImapFolder("foo/nested")).isTrue();
        assertThat(isValidImapFolder("/foo/nested")).isFalse();
    }

    @Test public void testAccountHasStoreUri() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        BackupImapStore store = new BackupImapStore(RuntimeEnvironment.application, uri,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));
        assertThat(store.getStoreUri()).isEqualTo(uri);
    }

    /**
     * AC-7(c): SYSTEM_VALIDATED path — DefaultTrustedSocketFactory is returned via the test seam.
     */
    @Test public void testShouldCreateCorrectTrustFactoryForTrustedSSLUrl() throws Exception {
        String uri = "imap+ssl+://xoauth:foooo@imap.gmail.com";
        DefaultTrustedSocketFactory factory = new DefaultTrustedSocketFactory(RuntimeEnvironment.application);
        BackupImapStore store = new BackupImapStore(RuntimeEnvironment.application, uri, factory);
        assertThat(store.getTrustedSocketFactory()).isInstanceOf(DefaultTrustedSocketFactory.class);
    }

    /**
     * AC-7(b): PINNED_CERTIFICATE path — PinnedCertificateSocketFactory is returned via the test seam.
     * Replaces the old trust-all assertion (was: AllTrustedSocketFactory) per U-008 AC-7(b).
     */
    @Test public void testShouldCreateCorrectTrustFactoryForPinnedCertUrl() throws Exception {
        String uri = "imap+ssl+://xoauth:foooo@imap.gmail.com";
        X509Certificate testCert = loadTestCert();
        PinnedCertificateSocketFactory factory = new PinnedCertificateSocketFactory(
                RuntimeEnvironment.application, "imap.gmail.com", testCert);
        BackupImapStore store = new BackupImapStore(RuntimeEnvironment.application, uri, factory);
        assertThat(store.getTrustedSocketFactory()).isInstanceOf(PinnedCertificateSocketFactory.class);
    }

    @Test public void testShouldCreateCorrectTrustFactoryForTrustedTLSUrl() throws Exception {
        String uri = "imap+tls+://xoauth:foooo@imap.gmail.com";
        DefaultTrustedSocketFactory factory = new DefaultTrustedSocketFactory(RuntimeEnvironment.application);
        BackupImapStore store = new BackupImapStore(RuntimeEnvironment.application, uri, factory);
        assertThat(store.getTrustedSocketFactory()).isInstanceOf(DefaultTrustedSocketFactory.class);
    }

    @Test public void shouldHaveToStringWithObfuscatedStoreURI() throws Exception {
        BackupImapStore store = new BackupImapStore(RuntimeEnvironment.application,
                "imap://xoauth:foooo@imap.gmail.com",
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));
        assertThat(store.getStoreUriForLogging()).isEqualTo("imap://xoauth:XXXXX@imap.gmail.com");
    }

    @Test public void shouldHaveToStringWithObfuscatedStoreURIWithPort() throws Exception {
        BackupImapStore store = new BackupImapStore(RuntimeEnvironment.application,
                "imap://xoauth:foooo@imap.gmail.com:456",
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));
        assertThat(store.getStoreUriForLogging()).isEqualTo("imap://xoauth:XXXXX@imap.gmail.com:456");
    }

    @Test(expected = MessagingException.class) public void shouldThrowExceptionIfUsernameIsMissing() throws Exception {
        new BackupImapStore(RuntimeEnvironment.application, "imap://imap.gmail.com:1234",
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));
    }

    @Test(expected = MessagingException.class) public void shouldThrowExceptionIfPasswordIsMissing() throws Exception {
        new BackupImapStore(RuntimeEnvironment.application, "imap://plain:foo:@imap.gmail.com:1234",
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));
    }

    @Test public void shouldHaveToStringWithStoreUriForLogging() throws Exception {
        BackupImapStore store = new BackupImapStore(RuntimeEnvironment.application,
                "imap://xoauth:foooo@imap.gmail.com",
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));
        assertThat(store.toString()).isEqualTo("BackupImapStore{uri=imap://xoauth:XXXXX@imap.gmail.com}");
    }
}
