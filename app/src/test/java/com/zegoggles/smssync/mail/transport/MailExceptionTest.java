package com.zegoggles.smssync.mail.transport;

import com.zegoggles.smssync.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for the app-owned exception hierarchy per AC-3 / CNTR-MODERNIZATION-007.
 */
@RunWith(RobolectricTestRunner.class)
public class MailExceptionTest {

    @Test
    public void mailException_getCause_roundTrip() {
        Throwable cause = new RuntimeException("original k9 error");
        MailException ex = new MailException(cause);

        assertThat(ex.getCause()).isSameInstanceAs(cause);
    }

    @Test
    public void mailException_messageConstructor_noCause() {
        MailException ex = new MailException("config error");

        assertThat(ex.getCause()).isNull();
        assertThat(ex.getMessage()).isEqualTo("config error");
    }

    @Test
    public void mailException_errorResourceId_returnsUnknownError() {
        MailException ex = new MailException(new RuntimeException("test"));

        assertThat(ex.errorResourceId()).isEqualTo(R.string.status_unknown_error);
    }

    @Test
    public void temporaryImapException_errorResourceId_returnsGmailTempError() {
        Throwable cause = new RuntimeException("Unable to get IMAP prefix");
        TemporaryImapException ex = new TemporaryImapException(cause);

        assertThat(ex.errorResourceId()).isEqualTo(R.string.status_gmail_temp_error);
    }

    @Test
    public void temporaryImapException_getCause_roundTrip() {
        Throwable cause = new RuntimeException("imap prefix error");
        TemporaryImapException ex = new TemporaryImapException(cause);

        assertThat(ex.getCause()).isSameInstanceAs(cause);
    }

    @Test
    public void temporaryImapException_nullCause_isAllowed() {
        // AC-3 specifies TemporaryImapException(null) is valid (used in StateTest AC-8)
        TemporaryImapException ex = new TemporaryImapException(null);

        assertThat(ex.getCause()).isNull();
        assertThat(ex.errorResourceId()).isEqualTo(R.string.status_gmail_temp_error);
    }

    @Test
    public void xOAuth2FailedException_getStatus_returnsStoredStatus() {
        Throwable cause = new RuntimeException("xoauth2 failure");
        XOAuth2FailedException ex = new XOAuth2FailedException(400, cause);

        assertThat(ex.getStatus()).isEqualTo(400);
    }

    @Test
    public void xOAuth2FailedException_getCause_roundTrip() {
        Throwable cause = new RuntimeException("xoauth2 failure");
        XOAuth2FailedException ex = new XOAuth2FailedException(400, cause);

        assertThat(ex.getCause()).isSameInstanceAs(cause);
    }

    @Test
    public void xOAuth2FailedException_nonStandard_statusCode() {
        Throwable cause = new RuntimeException("unexpected status");
        XOAuth2FailedException ex = new XOAuth2FailedException(401, cause);

        assertThat(ex.getStatus()).isEqualTo(401);
    }

    @Test
    public void mailException_isInstanceOf_LocalizableException() {
        MailException ex = new MailException(new RuntimeException());

        assertThat(ex).isInstanceOf(com.zegoggles.smssync.service.exception.LocalizableException.class);
    }

    @Test
    public void temporaryImapException_isSubclassOf_MailException() {
        TemporaryImapException ex = new TemporaryImapException(null);

        assertThat(ex).isInstanceOf(MailException.class);
    }

    @Test
    public void xOAuth2FailedException_isSubclassOf_MailException() {
        XOAuth2FailedException ex = new XOAuth2FailedException(400, null);

        assertThat(ex).isInstanceOf(MailException.class);
    }
}
