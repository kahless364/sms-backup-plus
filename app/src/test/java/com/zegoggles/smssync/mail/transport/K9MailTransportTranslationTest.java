package com.zegoggles.smssync.mail.transport;

import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException;
import com.zegoggles.smssync.service.exception.RequiresLoginException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the k-9-throwable → app-exception translation mapping per AC-6 /
 * CNTR-MODERNIZATION-007 §Exception-translation mapping.
 *
 * Uses the package-private {@code K9MailTransport.translateMessagingException()} seam to
 * exercise each translation row in isolation, without requiring a live IMAP connection.
 * Mockito is used to stub {@link XOAuth2AuthenticationFailedException} (which has a complex
 * constructor requiring a JSONObject) so {@code getStatus()} can be controlled.
 */
@RunWith(RobolectricTestRunner.class)
public class K9MailTransportTranslationTest {

    // -------------------------------------------------------------------------
    // Row 1: XOAuth2AuthenticationFailedException → XOAuth2FailedException
    // -------------------------------------------------------------------------

    @Test
    public void xOAuth2AuthException_translatesTo_XOAuth2FailedException() {
        XOAuth2AuthenticationFailedException k9ex =
                mock(XOAuth2AuthenticationFailedException.class);
        when(k9ex.getStatus()).thenReturn(400);

        Exception translated = K9MailTransport.translateMessagingException(k9ex);

        assertThat(translated).isInstanceOf(XOAuth2FailedException.class);
    }

    @Test
    public void xOAuth2AuthException_preservesStatusCode() {
        XOAuth2AuthenticationFailedException k9ex =
                mock(XOAuth2AuthenticationFailedException.class);
        when(k9ex.getStatus()).thenReturn(400);

        Exception translated = K9MailTransport.translateMessagingException(k9ex);

        assertThat(((XOAuth2FailedException) translated).getStatus()).isEqualTo(400);
    }

    @Test
    public void xOAuth2AuthException_preservesCause() {
        XOAuth2AuthenticationFailedException k9ex =
                mock(XOAuth2AuthenticationFailedException.class);
        when(k9ex.getStatus()).thenReturn(400);

        Exception translated = K9MailTransport.translateMessagingException(k9ex);

        assertThat(translated.getCause()).isSameInstanceAs(k9ex);
    }

    // -------------------------------------------------------------------------
    // Row 2: AuthenticationFailedException → RequiresLoginException
    // -------------------------------------------------------------------------

    @Test
    public void authFailedException_translatesTo_RequiresLoginException() {
        AuthenticationFailedException k9ex =
                new AuthenticationFailedException("bad credentials");

        Exception translated = K9MailTransport.translateMessagingException(k9ex);

        assertThat(translated).isInstanceOf(RequiresLoginException.class);
    }

    // -------------------------------------------------------------------------
    // Row 3: MessagingException("Unable to get IMAP prefix") → TemporaryImapException
    // -------------------------------------------------------------------------

    @Test
    public void imapPrefixMessagingException_translatesTo_TemporaryImapException() {
        MessagingException k9ex = new MessagingException("Unable to get IMAP prefix");

        Exception translated = K9MailTransport.translateMessagingException(k9ex);

        assertThat(translated).isInstanceOf(TemporaryImapException.class);
    }

    @Test
    public void imapPrefixMessagingException_preservesCause() {
        MessagingException k9ex = new MessagingException("Unable to get IMAP prefix");

        Exception translated = K9MailTransport.translateMessagingException(k9ex);

        assertThat(translated.getCause()).isSameInstanceAs(k9ex);
    }

    // -------------------------------------------------------------------------
    // Row 4: generic MessagingException → MailException (backstop)
    // -------------------------------------------------------------------------

    @Test
    public void genericMessagingException_translatesTo_MailException() {
        MessagingException k9ex = new MessagingException("connection refused");

        Exception translated = K9MailTransport.translateMessagingException(k9ex);

        assertThat(translated).isInstanceOf(MailException.class);
        // must NOT be a TemporaryImapException (that's a distinct subtype)
        assertThat(translated).isNotInstanceOf(TemporaryImapException.class);
    }

    @Test
    public void genericMessagingException_preservesCause() {
        MessagingException k9ex = new MessagingException("connect error");

        Exception translated = K9MailTransport.translateMessagingException(k9ex);

        assertThat(translated.getCause()).isSameInstanceAs(k9ex);
    }

    // -------------------------------------------------------------------------
    // Row 5: re-wrapped IllegalArgumentException (BackupImapStore.java:129-133) → MailException
    // -------------------------------------------------------------------------

    @Test
    public void rewrappedIllegalArgException_translatesTo_MailException() {
        // BackupImapStore.createAndOpenFolder catches IllegalArgumentException and
        // re-wraps it as MessagingException(e.getMessage()) — same catch-all path.
        MessagingException k9ex = new MessagingException("K9 internal error from IAE");

        Exception translated = K9MailTransport.translateMessagingException(k9ex);

        assertThat(translated).isInstanceOf(MailException.class);
        assertThat(translated.getCause()).isSameInstanceAs(k9ex);
    }

    // -------------------------------------------------------------------------
    // Catch-ordering invariant: XOAuth2 must be caught before AuthenticationFailed
    // -------------------------------------------------------------------------

    @Test
    public void xOAuth2AuthException_isNot_treatedAsGenericAuthFailed() {
        // XOAuth2AuthenticationFailedException extends AuthenticationFailedException;
        // it must be caught first so getStatus() is preserved.
        XOAuth2AuthenticationFailedException k9ex =
                mock(XOAuth2AuthenticationFailedException.class);
        when(k9ex.getStatus()).thenReturn(400);

        Exception translated = K9MailTransport.translateMessagingException(k9ex);

        // If catch ordering was wrong, this would be RequiresLoginException
        assertThat(translated).isInstanceOf(XOAuth2FailedException.class);
        assertThat(translated).isNotInstanceOf(RequiresLoginException.class);
    }
}
