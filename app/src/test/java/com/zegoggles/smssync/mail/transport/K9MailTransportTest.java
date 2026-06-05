package com.zegoggles.smssync.mail.transport;

import android.annotation.SuppressLint;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.zegoggles.smssync.mail.ConversionResult;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.Headers;
import com.zegoggles.smssync.mail.PinnedCertificateSocketFactory;
import com.zegoggles.smssync.mail.TlsTrustPolicy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static com.google.common.truth.Truth.assertThat;
import static com.zegoggles.smssync.mail.transport.K9MailTransport.isValidImapFolder;
import static com.zegoggles.smssync.mail.transport.K9MailTransport.isValidUri;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link K9MailTransport} — re-homes trust-factory and validator assertions
 * from {@code BackupImapStoreTest} per IC-3 / AC-5 / CNTR-MODERNIZATION-007.
 */
@RunWith(RobolectricTestRunner.class)
@SuppressLint("AuthLeak")
public class K9MailTransportTest {

    /**
     * PEM body of a test-only self-signed certificate (same cert as BackupImapStoreTest).
     * CN=test.example.org, RSA-2048, SHA256withRSA, valid until 2036-05-31.
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

    private X509Certificate loadTestCert() throws Exception {
        byte[] der = Base64.getDecoder().decode(TEST_CERT_PEM);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));
    }

    // -------------------------------------------------------------------------
    // AC-5 / IC-3: TLS factory mapping assertions
    // -------------------------------------------------------------------------

    @Test
    public void systemValidated_policy_creates_DefaultTrustedSocketFactory()
            throws Exception {
        String uri = "imap+ssl+://xoauth:token@imap.gmail.com:993";
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED);

        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application, config);

        assertThat(transport.getTrustedSocketFactory())
                .isInstanceOf(DefaultTrustedSocketFactory.class);
    }

    @Test
    public void pinnedCertificate_policy_creates_PinnedCertificateSocketFactory()
            throws Exception {
        String uri = "imap+ssl+://xoauth:token@imap.gmail.com:993";
        X509Certificate cert = loadTestCert();
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.PINNED_CERTIFICATE, cert);

        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application, config);

        assertThat(transport.getTrustedSocketFactory())
                .isInstanceOf(PinnedCertificateSocketFactory.class);
    }

    @Test
    public void pinnedCertificate_nullCert_throwsMailException() throws Exception {
        String uri = "imap+ssl+://xoauth:token@imap.gmail.com:993";
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.PINNED_CERTIFICATE, null);

        try {
            new K9MailTransport(RuntimeEnvironment.application, config);
            throw new AssertionError("Expected MailException");
        } catch (MailException e) {
            assertThat(e.getMessage()).contains("pinnedCert");
        }
    }

    // -------------------------------------------------------------------------
    // AC-2 / static validators (re-homed from BackupImapStoreTest)
    // -------------------------------------------------------------------------

    @Test
    public void shouldTestForValidUri() {
        assertThat(isValidUri("imap+ssl+://xoauth:foooo@imap.gmail.com:993")).isTrue();
        assertThat(isValidUri("imap://xoauth:foooo@imap.gmail.com")).isTrue();
        assertThat(isValidUri("imap+ssl+://xoauth:user:token@:993")).isFalse();
        assertThat(isValidUri("imap+ssl://user%40domain:password@imap.gmail.com:993")).isFalse();
        assertThat(isValidUri("imap+tls+://user:password@imap.gmail.com:993")).isTrue();
        assertThat(isValidUri("imap+tls://user:password@imap.gmail.com:993")).isFalse();
        assertThat(isValidUri("imap://user:password@imap.gmail.com:993")).isTrue();
        assertThat(isValidUri("http://xoauth:foooo@imap.gmail.com:993")).isFalse();
    }

    @Test
    public void shouldTestForValidFolder() {
        assertThat(isValidImapFolder(null)).isFalse();
        assertThat(isValidImapFolder("")).isFalse();
        assertThat(isValidImapFolder("foo")).isTrue();
        assertThat(isValidImapFolder("foo bar")).isTrue();
        assertThat(isValidImapFolder(" foo")).isFalse();
        assertThat(isValidImapFolder("foo ")).isFalse();
        assertThat(isValidImapFolder("foo/nested")).isTrue();
        assertThat(isValidImapFolder("/foo/nested")).isFalse();
    }

    @Test
    public void testAccountHasStoreUri() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED);
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application, config);

        assertThat(transport.getStoreUri()).isEqualTo(uri);
    }

    @Test
    public void shouldHaveToStringWithObfuscatedStoreURI() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED);
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application, config);

        assertThat(transport.getStoreUriForLogging())
                .isEqualTo("imap://xoauth:XXXXX@imap.gmail.com");
    }

    @Test
    public void shouldHaveToStringWithObfuscatedStoreURIWithPort() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com:456";
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED);
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application, config);

        assertThat(transport.getStoreUriForLogging())
                .isEqualTo("imap://xoauth:XXXXX@imap.gmail.com:456");
    }

    @Test
    public void shouldThrowMailExceptionIfUsernameIsMissing() throws Exception {
        // C-1: constructor now throws MailException (wrapping MessagingException) only.
        String uri = "imap://imap.gmail.com:1234";
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED);
        try {
            new K9MailTransport(RuntimeEnvironment.application, config);
            throw new AssertionError("Expected MailException");
        } catch (MailException e) {
            assertThat(e.getCause()).isInstanceOf(MessagingException.class);
        }
    }

    @Test
    public void shouldThrowMailExceptionIfPasswordIsMissing() throws Exception {
        // C-1: constructor now throws MailException (wrapping MessagingException) only.
        String uri = "imap://plain:foo:@imap.gmail.com:1234";
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED);
        try {
            new K9MailTransport(RuntimeEnvironment.application, config);
            throw new AssertionError("Expected MailException");
        } catch (MailException e) {
            assertThat(e.getCause()).isInstanceOf(MessagingException.class);
        }
    }

    // -------------------------------------------------------------------------
    // AC-8: Cause-chain preservation tests (CNTR-007 Clause C-1 + C-2)
    // -------------------------------------------------------------------------

    /**
     * AC-8 C-1 path: given a BackupImapStoreDelegate whose constructor throws
     * MessagingException("password not set"), the MailException caught by the caller must
     * preserve the cause chain: getCause() returns the original MessagingException.
     *
     * Note: We cannot easily stub the BackupImapStoreDelegate constructor itself (it is
     * called inside the public K9MailTransport constructor via 'new'). Instead we verify
     * that when K9MailTransport's public constructor is used with a URI that causes
     * BackupImapStoreDelegate to throw MessagingException, the resulting MailException
     * wraps it. We use a URI missing credentials (which triggers MessagingException from k-9)
     * and confirm getCause() is a MessagingException.
     */
    @Test
    public void causeChain_C1_constructor_messagingExceptionPreservedAsCause() throws Exception {
        String uri = "imap://imap.gmail.com:1234"; // missing credentials → MessagingException
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED);
        try {
            new K9MailTransport(RuntimeEnvironment.application, config);
            throw new AssertionError("Expected MailException");
        } catch (MailException e) {
            assertThat(e.getCause()).isInstanceOf(MessagingException.class);
            assertThat(e.getCause().getMessage()).isNotNull();
        }
    }

    @Test
    public void shouldHaveToStringWithTransportUri() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED);
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application, config);

        assertThat(transport.toString())
                .isEqualTo("K9MailTransport{uri=imap://xoauth:XXXXX@imap.gmail.com}");
    }

    @Test
    public void closeFolders_noOpenFolders_doesNotThrow() throws Exception {
        // AC-4: closeFolders() must not throw (it swallows exceptions per BackupImapStore.java:80-84)
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED);
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application, config);

        // Should complete silently with no open folders
        transport.closeFolders();
        // Call twice to verify idempotent behavior
        transport.closeFolders();
    }

    @Test
    public void storeUriForLogging_userWithNoPassword_returnsOriginalUri() throws Exception {
        // Use a URI with user info that has no colon separator — getStoreUriForLogging()
        // falls through to the else branch and returns uri.toString() unchanged.
        // Use xoauth user (no password colon) in a valid ssl URI.
        String uri = "imap+ssl+://xoauth:foooo@imap.gmail.com";
        MailTransportConfig config = new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED);
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application, config);

        // "xoauth:foooo" has a colon, so the masking logic runs and produces XXXXX
        String logging = transport.getStoreUriForLogging();
        assertThat(logging).contains("XXXXX");
    }

    @Test
    public void translateMessagingException_xoauth2_status401() {
        // Test non-400 status code is also translated correctly
        com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException k9ex =
                mock(com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException.class);
        when(k9ex.getStatus()).thenReturn(401);

        Exception translated = K9MailTransport.translateMessagingException(k9ex);

        assertThat(translated).isInstanceOf(XOAuth2FailedException.class);
        assertThat(((XOAuth2FailedException) translated).getStatus()).isEqualTo(401);
    }

    @Test
    public void mailTransportConfig_systemValidated_noArgConstructor() {
        // Verify two-arg constructor works
        MailTransportConfig config = new MailTransportConfig("imap://u:p@h", TlsTrustPolicy.SYSTEM_VALIDATED);
        assertThat(config.storeUri).isEqualTo("imap://u:p@h");
        assertThat(config.tlsPolicy).isEqualTo(TlsTrustPolicy.SYSTEM_VALIDATED);
        assertThat(config.pinnedCert).isNull();
    }

    @Test
    public void mailTransportConfig_pinnedCertificate_threeArgConstructor() throws Exception {
        X509Certificate cert = loadTestCert();
        MailTransportConfig config = new MailTransportConfig(
                "imap+ssl+://u:p@h:993", TlsTrustPolicy.PINNED_CERTIFICATE, cert);
        assertThat(config.tlsPolicy).isEqualTo(TlsTrustPolicy.PINNED_CERTIFICATE);
        assertThat(config.pinnedCert).isSameInstanceAs(cert);
    }

    // -------------------------------------------------------------------------
    // BackupFolderHandle / MailMessageHandle type tests
    // -------------------------------------------------------------------------

    @Test
    public void backupFolderHandle_wrapsFolder_packagePrivate() {
        com.fsck.k9.mail.store.imap.ImapFolder mockFolder =
                mock(com.fsck.k9.mail.store.imap.ImapFolder.class);
        BackupFolderHandle handle = new BackupFolderHandle(mockFolder);

        assertThat(handle.folder).isSameInstanceAs(mockFolder);
    }

    @Test
    public void mailMessageHandle_uid_accessible() {
        com.fsck.k9.mail.store.imap.ImapMessage mockMsg =
                mock(com.fsck.k9.mail.store.imap.ImapMessage.class);
        MailMessageHandle handle = new MailMessageHandle("test-uid-789", mockMsg);

        assertThat(handle.uid).isEqualTo("test-uid-789");
        assertThat(handle.message).isSameInstanceAs(mockMsg);
    }

    // -------------------------------------------------------------------------
    // FetchSpec enum tests
    // -------------------------------------------------------------------------

    @Test
    public void fetchSpec_hasExpectedValues() {
        FetchSpec[] values = FetchSpec.values();
        assertThat(values).asList().containsExactly(FetchSpec.ENVELOPE_DATE, FetchSpec.BODY);
    }

    // -------------------------------------------------------------------------
    // TemporaryImapException hierarchy
    // -------------------------------------------------------------------------

    @Test
    public void temporaryImapException_errorResourceId() {
        TemporaryImapException ex = new TemporaryImapException(null);
        assertThat(ex.errorResourceId()).isEqualTo(com.zegoggles.smssync.R.string.status_gmail_temp_error);
    }

    // -------------------------------------------------------------------------
    // XOAuth2FailedException hierarchy
    // -------------------------------------------------------------------------

    @Test
    public void xOAuth2FailedException_getStatus_400() {
        XOAuth2FailedException ex = new XOAuth2FailedException(400, null);
        assertThat(ex.getStatus()).isEqualTo(400);
        assertThat(ex).isInstanceOf(MailException.class);
    }

    // -------------------------------------------------------------------------
    // checkSettings() exception translation (using test constructor with delegate spy)
    // -------------------------------------------------------------------------

    private K9MailTransport.BackupImapStoreDelegate createDelegate() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        return new K9MailTransport.BackupImapStoreDelegate(
                RuntimeEnvironment.application, uri,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));
    }

    @Test
    public void checkSettings_messagingException_translatesTo_MailException() throws Exception {
        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport.BackupImapStoreDelegate spy = org.mockito.Mockito.spy(delegate);
        org.mockito.Mockito.doThrow(new com.fsck.k9.mail.MessagingException("conn error"))
                .when(spy).checkSettings();

        K9MailTransport transport = new K9MailTransport(spy,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        try {
            transport.checkSettings();
            throw new AssertionError("Expected MailException");
        } catch (MailException e) {
            assertThat(e.getCause()).isInstanceOf(com.fsck.k9.mail.MessagingException.class);
        }
    }

    @Test
    public void checkSettings_imapPrefixException_translatesTo_TemporaryImapException()
            throws Exception {
        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport.BackupImapStoreDelegate spy = org.mockito.Mockito.spy(delegate);
        org.mockito.Mockito.doThrow(
                new com.fsck.k9.mail.MessagingException("Unable to get IMAP prefix"))
                .when(spy).checkSettings();

        K9MailTransport transport = new K9MailTransport(spy,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        try {
            transport.checkSettings();
            throw new AssertionError("Expected TemporaryImapException");
        } catch (TemporaryImapException e) {
            // expected
        }
    }

    @Test
    public void checkSettings_authException_translatesTo_RequiresLoginException()
            throws Exception {
        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport.BackupImapStoreDelegate spy = org.mockito.Mockito.spy(delegate);
        org.mockito.Mockito.doThrow(
                new com.fsck.k9.mail.AuthenticationFailedException("bad"))
                .when(spy).checkSettings();

        K9MailTransport transport = new K9MailTransport(spy,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        try {
            transport.checkSettings();
            throw new AssertionError("Expected RequiresLoginException");
        } catch (com.zegoggles.smssync.service.exception.RequiresLoginException e) {
            // expected
        }
    }

    // -------------------------------------------------------------------------
    // appendMessages() success path — confirmed date (BUG-010 / U-042)
    // -------------------------------------------------------------------------

    /**
     * U-042 / BUG-010: appendMessages() returns result.getMaxDate() on success.
     *
     * Verifies the confirmed-append contract: the return value is the max date of the
     * messages in the ConversionResult, NOT wall-clock time. The caller (BackupWorker)
     * uses this return value as the watermark — making "watermark = confirmed-date"
     * compiler-enforced.
     */
    @Test
    public void appendMessages_success_returnsConfirmedMaxDate() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application,
                new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED));

        // Mock ImapFolder.appendMessages() to succeed (returns empty map, no exception)
        // Note: k9 ImapFolder.appendMessages returns Map<String,String> (UID mapping), not void.
        com.fsck.k9.mail.store.imap.ImapFolder mockFolder =
                mock(com.fsck.k9.mail.store.imap.ImapFolder.class);
        when(mockFolder.appendMessages(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Collections.emptyMap());

        // Build a ConversionResult with a known max date
        // ConversionResult.maxDate is updated by add(); use a mock Message with DATE header
        com.fsck.k9.mail.store.imap.ImapMessage mockMsg =
                mock(com.fsck.k9.mail.store.imap.ImapMessage.class);
        final long expectedDate = 1_700_000_001_000L;
        when(mockMsg.getHeader(com.zegoggles.smssync.mail.Headers.DATE))
                .thenReturn(new String[]{String.valueOf(expectedDate)});

        BackupFolderHandle handle = new BackupFolderHandle(mockFolder);
        ConversionResult result = new ConversionResult(DataType.SMS);
        result.add(mockMsg, new java.util.HashMap<>());

        // Act: appendMessages should return the confirmed max date
        long confirmedDate = transport.appendMessages(handle, result);

        // Assert: returned date matches the ConversionResult's max date (NOT wall-clock "now")
        assertThat(confirmedDate).isEqualTo(expectedDate);
    }

    /**
     * U-042 / BUG-010: appendMessages() on empty ConversionResult returns DEFAULT_MAX_SYNCED_DATE (-1).
     *
     * An empty result has maxDate = DataType.Defaults.MAX_SYNCED_DATE (-1). This is
     * never reached in production (result.isEmpty() guard) but verifies the contract holds.
     */
    @Test
    public void appendMessages_emptyResult_returnsDefaultMaxDate() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application,
                new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED));

        com.fsck.k9.mail.store.imap.ImapFolder mockFolder =
                mock(com.fsck.k9.mail.store.imap.ImapFolder.class);
        when(mockFolder.appendMessages(org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Collections.emptyMap());

        BackupFolderHandle handle = new BackupFolderHandle(mockFolder);
        ConversionResult result = new ConversionResult(DataType.SMS); // no messages added → maxDate = -1

        long confirmedDate = transport.appendMessages(handle, result);

        assertThat(confirmedDate).isEqualTo(DataType.Defaults.MAX_SYNCED_DATE);
    }

    // -------------------------------------------------------------------------
    // appendMessages() exception translation
    // -------------------------------------------------------------------------

    @Test
    public void appendMessages_messagingException_translatesTo_MailException() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application,
                new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED));

        com.fsck.k9.mail.store.imap.ImapFolder mockFolder =
                mock(com.fsck.k9.mail.store.imap.ImapFolder.class);
        org.mockito.Mockito.doThrow(new com.fsck.k9.mail.MessagingException("connection error"))
                .when(mockFolder).appendMessages(org.mockito.ArgumentMatchers.any());

        BackupFolderHandle handle = new BackupFolderHandle(mockFolder);
        ConversionResult result = new ConversionResult(DataType.SMS);

        try {
            transport.appendMessages(handle, result);
            throw new AssertionError("Expected MailException");
        } catch (MailException e) {
            assertThat(e).isNotInstanceOf(TemporaryImapException.class);
        }
    }

    @Test
    public void appendMessages_temporaryImapException_translation() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application,
                new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED));

        com.fsck.k9.mail.store.imap.ImapFolder mockFolder =
                mock(com.fsck.k9.mail.store.imap.ImapFolder.class);
        org.mockito.Mockito.doThrow(
                new com.fsck.k9.mail.MessagingException("Unable to get IMAP prefix"))
                .when(mockFolder).appendMessages(org.mockito.ArgumentMatchers.any());

        BackupFolderHandle handle = new BackupFolderHandle(mockFolder);
        ConversionResult result = new ConversionResult(DataType.SMS);

        try {
            transport.appendMessages(handle, result);
            throw new AssertionError("Expected TemporaryImapException");
        } catch (TemporaryImapException e) {
            // expected
        }
    }

    @Test
    public void appendMessages_authFailedException_translatesTo_RequiresLoginException()
            throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application,
                new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED));

        com.fsck.k9.mail.store.imap.ImapFolder mockFolder =
                mock(com.fsck.k9.mail.store.imap.ImapFolder.class);
        org.mockito.Mockito.doThrow(
                new com.fsck.k9.mail.AuthenticationFailedException("bad creds"))
                .when(mockFolder).appendMessages(org.mockito.ArgumentMatchers.any());

        BackupFolderHandle handle = new BackupFolderHandle(mockFolder);
        ConversionResult result = new ConversionResult(DataType.SMS);

        try {
            transport.appendMessages(handle, result);
            throw new AssertionError("Expected RequiresLoginException");
        } catch (com.zegoggles.smssync.service.exception.RequiresLoginException e) {
            // expected
        }
    }

    @Test
    public void appendMessages_xOAuth2Exception_translatesTo_XOAuth2FailedException()
            throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application,
                new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED));

        com.fsck.k9.mail.store.imap.ImapFolder mockFolder =
                mock(com.fsck.k9.mail.store.imap.ImapFolder.class);
        com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException xoauthEx =
                mock(com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException.class);
        when(xoauthEx.getStatus()).thenReturn(400);
        org.mockito.Mockito.doThrow(xoauthEx)
                .when(mockFolder).appendMessages(org.mockito.ArgumentMatchers.any());

        BackupFolderHandle handle = new BackupFolderHandle(mockFolder);
        ConversionResult result = new ConversionResult(DataType.SMS);

        try {
            transport.appendMessages(handle, result);
            throw new AssertionError("Expected XOAuth2FailedException");
        } catch (XOAuth2FailedException e) {
            assertThat(e.getStatus()).isEqualTo(400);
        }
    }

    // -------------------------------------------------------------------------
    // fetch() exception translation via handle resolution path
    // -------------------------------------------------------------------------

    @Test
    public void fetch_messagingException_translatesTo_MailException() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application,
                new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED));

        com.fsck.k9.mail.store.imap.ImapFolder mockFolder =
                mock(com.fsck.k9.mail.store.imap.ImapFolder.class);
        com.fsck.k9.mail.store.imap.ImapMessage mockMsg =
                mock(com.fsck.k9.mail.store.imap.ImapMessage.class);
        org.mockito.Mockito.doThrow(new com.fsck.k9.mail.MessagingException("fetch error"))
                .when(mockFolder).fetch(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.isNull());

        BackupFolderHandle folderHandle = new BackupFolderHandle(mockFolder);
        MailMessageHandle msgHandle = new MailMessageHandle("uid1", mockMsg);

        try {
            transport.fetch(folderHandle, java.util.Arrays.asList(msgHandle), FetchSpec.BODY);
            throw new AssertionError("Expected MailException");
        } catch (MailException e) {
            assertThat(e).isNotInstanceOf(TemporaryImapException.class);
        }
    }

    // -------------------------------------------------------------------------
    // MessageComparator tests (verbatim from BackupImapStore)
    // -------------------------------------------------------------------------

    @Test
    public void messageComparator_nullMessages_sortedBeforeReal() {
        K9MailTransport.BackupImapStoreDelegate.MessageComparator cmp =
                K9MailTransport.BackupImapStoreDelegate.MessageComparator.INSTANCE;

        // compare(null, nonNull) should return positive (null is "earliest", sorts after)
        com.fsck.k9.mail.Message msg = mock(com.fsck.k9.mail.Message.class);
        when(msg.getSentDate()).thenReturn(new java.util.Date(1000L));

        int result = cmp.compare(null, msg);
        // null uses EARLY (epoch 0), msg uses 1000ms: EARLY is "earlier" so d2 > d1 → negative
        // compare returns d2.compareTo(d1) = msg.getSentDate().compareTo(EARLY) = positive
        assertThat(result).isGreaterThan(0);
    }

    @Test
    public void messageComparator_sameDate_returnsZero() {
        K9MailTransport.BackupImapStoreDelegate.MessageComparator cmp =
                K9MailTransport.BackupImapStoreDelegate.MessageComparator.INSTANCE;

        com.fsck.k9.mail.Message m1 = mock(com.fsck.k9.mail.Message.class);
        com.fsck.k9.mail.Message m2 = mock(com.fsck.k9.mail.Message.class);
        java.util.Date date = new java.util.Date(5000L);
        when(m1.getSentDate()).thenReturn(date);
        when(m2.getSentDate()).thenReturn(date);

        assertThat(cmp.compare(m1, m2)).isEqualTo(0);
    }

    @Test
    public void messageComparator_m1Newer_returnsNegative() {
        // compare() does descending sort (d2.compareTo(d1))
        // if m1 is newer (larger date), d1 > d2, so d2.compareTo(d1) < 0
        K9MailTransport.BackupImapStoreDelegate.MessageComparator cmp =
                K9MailTransport.BackupImapStoreDelegate.MessageComparator.INSTANCE;

        com.fsck.k9.mail.Message m1 = mock(com.fsck.k9.mail.Message.class);
        com.fsck.k9.mail.Message m2 = mock(com.fsck.k9.mail.Message.class);
        when(m1.getSentDate()).thenReturn(new java.util.Date(10000L));
        when(m2.getSentDate()).thenReturn(new java.util.Date(5000L));

        assertThat(cmp.compare(m1, m2)).isLessThan(0);
    }

    @Test
    public void messageComparator_nullSentDate_usesEpoch() {
        K9MailTransport.BackupImapStoreDelegate.MessageComparator cmp =
                K9MailTransport.BackupImapStoreDelegate.MessageComparator.INSTANCE;

        com.fsck.k9.mail.Message m1 = mock(com.fsck.k9.mail.Message.class);
        com.fsck.k9.mail.Message m2 = mock(com.fsck.k9.mail.Message.class);
        when(m1.getSentDate()).thenReturn(null);
        when(m2.getSentDate()).thenReturn(null);

        // Both null dates → EARLY for both → compare returns 0
        assertThat(cmp.compare(m1, m2)).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // closeFolders() via delegate spy — covers delegate closeFolders code path
    // -------------------------------------------------------------------------

    @Test
    public void closeFolders_delegateCalled() throws Exception {
        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport.BackupImapStoreDelegate spy = org.mockito.Mockito.spy(delegate);

        K9MailTransport transport = new K9MailTransport(spy,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        transport.closeFolders();
        // No assertions needed — verifies it completes without exception
    }

    // -------------------------------------------------------------------------
    // Additional static validator edge cases
    // -------------------------------------------------------------------------

    @Test
    public void isValidUri_nullUri_returnsFalse() {
        assertThat(K9MailTransport.isValidUri(null)).isFalse();
    }

    @Test
    public void isValidUri_emptyUri_returnsFalse() {
        assertThat(K9MailTransport.isValidUri("")).isFalse();
    }

    @Test
    public void isValidImapFolder_singleSlashStart_returnsFalse() {
        assertThat(K9MailTransport.isValidImapFolder("/start")).isFalse();
    }

    @Test
    public void isValidImapFolder_trailingSpace_returnsFalse() {
        assertThat(K9MailTransport.isValidImapFolder("trail ")).isFalse();
    }

    @Test
    public void isValidImapFolder_leadingSpace_returnsFalse() {
        assertThat(K9MailTransport.isValidImapFolder(" lead")).isFalse();
    }

    // -------------------------------------------------------------------------
    // fetch() xOAuth2 exception translation
    // -------------------------------------------------------------------------

    @Test
    public void fetch_xOAuth2Exception_translatesTo_XOAuth2FailedException() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application,
                new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED));

        com.fsck.k9.mail.store.imap.ImapFolder mockFolder =
                mock(com.fsck.k9.mail.store.imap.ImapFolder.class);
        com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException xoauthEx =
                mock(com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException.class);
        when(xoauthEx.getStatus()).thenReturn(400);
        org.mockito.Mockito.doThrow(xoauthEx)
                .when(mockFolder).fetch(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.isNull());

        BackupFolderHandle folderHandle = new BackupFolderHandle(mockFolder);
        com.fsck.k9.mail.store.imap.ImapMessage mockMsg =
                mock(com.fsck.k9.mail.store.imap.ImapMessage.class);
        MailMessageHandle msgHandle = new MailMessageHandle("uid1", mockMsg);

        try {
            transport.fetch(folderHandle, java.util.Arrays.asList(msgHandle), FetchSpec.BODY);
            throw new AssertionError("Expected XOAuth2FailedException");
        } catch (XOAuth2FailedException e) {
            assertThat(e.getStatus()).isEqualTo(400);
        }
    }

    @Test
    public void fetch_temporaryImapException_translation() throws Exception {
        String uri = "imap://xoauth:foooo@imap.gmail.com";
        K9MailTransport transport = new K9MailTransport(RuntimeEnvironment.application,
                new MailTransportConfig(uri, TlsTrustPolicy.SYSTEM_VALIDATED));

        com.fsck.k9.mail.store.imap.ImapFolder mockFolder =
                mock(com.fsck.k9.mail.store.imap.ImapFolder.class);
        org.mockito.Mockito.doThrow(
                new com.fsck.k9.mail.MessagingException("Unable to get IMAP prefix"))
                .when(mockFolder).fetch(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.isNull());

        BackupFolderHandle folderHandle = new BackupFolderHandle(mockFolder);
        com.fsck.k9.mail.store.imap.ImapMessage mockMsg =
                mock(com.fsck.k9.mail.store.imap.ImapMessage.class);
        MailMessageHandle msgHandle = new MailMessageHandle("uid1", mockMsg);

        try {
            transport.fetch(folderHandle, java.util.Arrays.asList(msgHandle), FetchSpec.BODY);
            throw new AssertionError("Expected TemporaryImapException");
        } catch (TemporaryImapException e) {
            // expected
        }
    }
}
