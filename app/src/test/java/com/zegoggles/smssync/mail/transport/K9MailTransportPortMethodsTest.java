package com.zegoggles.smssync.mail.transport;

import android.content.ContentValues;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.MessageConverter;
import com.zegoggles.smssync.preferences.DataTypePreferences;
import com.zegoggles.smssync.service.exception.RequiresLoginException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests for the new port methods added to {@link K9MailTransport} in U-026:
 * {@code openFolder()}, {@code getMessages()}, and {@code importMessageBody()}.
 *
 * <p>Uses the package-private test constructor
 * {@code K9MailTransport(BackupImapStoreDelegate, TrustedSocketFactory)} with a spy on
 * a real {@link K9MailTransport.BackupImapStoreDelegate} to exercise exception-translation
 * paths without requiring a live IMAP connection.
 *
 * <p>Covers the U-026 coverage gap:
 * K9MailTransport 55% → ≥70% for mail.transport package aggregate.
 */
@RunWith(RobolectricTestRunner.class)
public class K9MailTransportPortMethodsTest {

    private static final String TEST_URI = "imap://xoauth:foooo@imap.gmail.com";

    /** Creates a real BackupImapStoreDelegate for spying (same helper as K9MailTransportTest). */
    private K9MailTransport.BackupImapStoreDelegate createDelegate() throws Exception {
        return new K9MailTransport.BackupImapStoreDelegate(
                RuntimeEnvironment.application, TEST_URI,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));
    }

    // =========================================================================
    // openFolder() — exception translation
    // =========================================================================

    @Test
    public void openFolder_messagingException_translatesTo_MailException() throws Exception {
        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport.BackupImapStoreDelegate spyDelegate = spy(delegate);
        doThrow(new MessagingException("folder open failed"))
                .when(spyDelegate).openFolder(
                        ArgumentMatchers.any(DataType.class),
                        ArgumentMatchers.any(DataTypePreferences.class));

        K9MailTransport transport = new K9MailTransport(spyDelegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        DataTypePreferences mockPrefs = mock(DataTypePreferences.class);
        try {
            transport.openFolder(DataType.SMS, mockPrefs);
            throw new AssertionError("Expected MailException");
        } catch (MailException e) {
            assertThat(e).isNotInstanceOf(TemporaryImapException.class);
            assertThat(e.getCause()).isInstanceOf(MessagingException.class);
        }
    }

    @Test
    public void openFolder_imapPrefixException_translatesTo_TemporaryImapException()
            throws Exception {
        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport.BackupImapStoreDelegate spyDelegate = spy(delegate);
        doThrow(new MessagingException("Unable to get IMAP prefix"))
                .when(spyDelegate).openFolder(
                        ArgumentMatchers.any(DataType.class),
                        ArgumentMatchers.any(DataTypePreferences.class));

        K9MailTransport transport = new K9MailTransport(spyDelegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        DataTypePreferences mockPrefs = mock(DataTypePreferences.class);
        try {
            transport.openFolder(DataType.SMS, mockPrefs);
            throw new AssertionError("Expected TemporaryImapException");
        } catch (TemporaryImapException e) {
            // expected
        }
    }

    @Test
    public void openFolder_authFailedException_translatesTo_RequiresLoginException()
            throws Exception {
        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport.BackupImapStoreDelegate spyDelegate = spy(delegate);
        doThrow(new AuthenticationFailedException("bad creds"))
                .when(spyDelegate).openFolder(
                        ArgumentMatchers.any(DataType.class),
                        ArgumentMatchers.any(DataTypePreferences.class));

        K9MailTransport transport = new K9MailTransport(spyDelegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        DataTypePreferences mockPrefs = mock(DataTypePreferences.class);
        try {
            transport.openFolder(DataType.SMS, mockPrefs);
            throw new AssertionError("Expected RequiresLoginException");
        } catch (RequiresLoginException e) {
            // expected
        }
    }

    @Test
    public void openFolder_xOAuth2Exception_translatesTo_XOAuth2FailedException()
            throws Exception {
        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport.BackupImapStoreDelegate spyDelegate = spy(delegate);
        XOAuth2AuthenticationFailedException xoauthEx =
                mock(XOAuth2AuthenticationFailedException.class);
        when(xoauthEx.getStatus()).thenReturn(400);
        doThrow(xoauthEx)
                .when(spyDelegate).openFolder(
                        ArgumentMatchers.any(DataType.class),
                        ArgumentMatchers.any(DataTypePreferences.class));

        K9MailTransport transport = new K9MailTransport(spyDelegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        DataTypePreferences mockPrefs = mock(DataTypePreferences.class);
        try {
            transport.openFolder(DataType.SMS, mockPrefs);
            throw new AssertionError("Expected XOAuth2FailedException");
        } catch (XOAuth2FailedException e) {
            assertThat(e.getStatus()).isEqualTo(400);
        }
    }

    // =========================================================================
    // importMessageBody() — happy path + exception handling
    //
    // importMessageBody() catches MessagingException, IOException, and
    // IllegalArgumentException and returns MessageImportResult.failure() for each.
    // Uses the BackupFolderHandle(ImapFolder) + MailMessageHandle package-private
    // constructors; mocks ImapFolder.fetch() and MessageConverter methods.
    // =========================================================================

    @Test
    public void importMessageBody_happyPath_returnsSuccessResult() throws Exception {
        // Arrange: mock ImapFolder to not throw on fetch()
        ImapFolder mockFolder = mock(ImapFolder.class);
        ImapMessage mockMsg = mock(ImapMessage.class);
        BackupFolderHandle folderHandle = new BackupFolderHandle(mockFolder);
        MailMessageHandle msgHandle = new MailMessageHandle("uid-happy", mockMsg);

        ContentValues cv = new ContentValues();
        cv.put("body", "hello");

        MessageConverter mockConverter = mock(MessageConverter.class);
        when(mockConverter.getDataType(mockMsg)).thenReturn(DataType.SMS);
        when(mockConverter.messageToContentValues(mockMsg)).thenReturn(cv);

        // Use the real transport (delegate is not called in importMessageBody)
        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport transport = new K9MailTransport(delegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        // Act
        MessageImportResult result = transport.importMessageBody(folderHandle, msgHandle,
                mockConverter);

        // Assert
        assertThat(result.failed).isFalse();
        assertThat(result.uid).isEqualTo("uid-happy");
        assertThat(result.dataType).isEqualTo(DataType.SMS);
        assertThat(result.contentValues).isSameInstanceAs(cv);
    }

    @Test
    public void importMessageBody_messagingException_returnsFailureResult() throws Exception {
        // Arrange: mock ImapFolder.fetch() to throw MessagingException
        ImapFolder mockFolder = mock(ImapFolder.class);
        ImapMessage mockMsg = mock(ImapMessage.class);
        BackupFolderHandle folderHandle = new BackupFolderHandle(mockFolder);
        MailMessageHandle msgHandle = new MailMessageHandle("uid-msg-ex", mockMsg);

        doThrow(new MessagingException("fetch failed"))
                .when(mockFolder).fetch(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.isNull());

        MessageConverter mockConverter = mock(MessageConverter.class);

        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport transport = new K9MailTransport(delegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        // Act
        MessageImportResult result = transport.importMessageBody(folderHandle, msgHandle,
                mockConverter);

        // Assert: failure result (not thrown), uid preserved
        assertThat(result.failed).isTrue();
        assertThat(result.uid).isEqualTo("uid-msg-ex");
    }

    @Test
    public void importMessageBody_ioException_from_fetch_returnsFailureResult() throws Exception {
        // IOException cannot be thrown from ImapFolder.fetch() directly (its signature only
        // declares MessagingException). However, IOException can propagate during body reading
        // inside the converter (messageToContentValues declares throws IOException).
        // This test exercises the IOException branch via converter.getDataType() which wraps
        // a body-read IOException in some implementations. We simulate via doAnswer.
        ImapFolder mockFolder = mock(ImapFolder.class);
        ImapMessage mockMsg = mock(ImapMessage.class);
        BackupFolderHandle folderHandle = new BackupFolderHandle(mockFolder);
        MailMessageHandle msgHandle = new MailMessageHandle("uid-io-ex", mockMsg);

        MessageConverter mockConverter = mock(MessageConverter.class);
        // messageToContentValues() can propagate IOException from MIME body reads
        when(mockConverter.getDataType(mockMsg)).thenReturn(DataType.SMS);
        when(mockConverter.messageToContentValues(mockMsg))
                .thenThrow(new IOException("body stream closed"));

        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport transport = new K9MailTransport(delegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        // Act
        MessageImportResult result = transport.importMessageBody(folderHandle, msgHandle,
                mockConverter);

        // Assert
        assertThat(result.failed).isTrue();
        assertThat(result.uid).isEqualTo("uid-io-ex");
    }

    @Test
    public void importMessageBody_illegalArgumentException_returnsFailureResult()
            throws Exception {
        // Arrange: mock ImapFolder.fetch() to throw IllegalArgumentException
        // (e.g. malformed body data from k-9 internals)
        ImapFolder mockFolder = mock(ImapFolder.class);
        ImapMessage mockMsg = mock(ImapMessage.class);
        BackupFolderHandle folderHandle = new BackupFolderHandle(mockFolder);
        MailMessageHandle msgHandle = new MailMessageHandle("uid-iae", mockMsg);

        doThrow(new IllegalArgumentException("malformed data"))
                .when(mockFolder).fetch(
                        ArgumentMatchers.any(),
                        ArgumentMatchers.any(),
                        ArgumentMatchers.isNull());

        MessageConverter mockConverter = mock(MessageConverter.class);

        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport transport = new K9MailTransport(delegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        // Act
        MessageImportResult result = transport.importMessageBody(folderHandle, msgHandle,
                mockConverter);

        // Assert
        assertThat(result.failed).isTrue();
        assertThat(result.uid).isEqualTo("uid-iae");
    }

    @Test
    public void importMessageBody_converterMessagingException_returnsFailureResult()
            throws Exception {
        // Arrange: ImapFolder.fetch() succeeds; converter.getDataType() throws MessagingException
        ImapFolder mockFolder = mock(ImapFolder.class);
        ImapMessage mockMsg = mock(ImapMessage.class);
        BackupFolderHandle folderHandle = new BackupFolderHandle(mockFolder);
        MailMessageHandle msgHandle = new MailMessageHandle("uid-conv-ex", mockMsg);

        MessageConverter mockConverter = mock(MessageConverter.class);
        when(mockConverter.getDataType(mockMsg))
                .thenThrow(new MessagingException("cannot determine data type"));

        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport transport = new K9MailTransport(delegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        // Act
        MessageImportResult result = transport.importMessageBody(folderHandle, msgHandle,
                mockConverter);

        // Assert
        assertThat(result.failed).isTrue();
        assertThat(result.uid).isEqualTo("uid-conv-ex");
    }

    // =========================================================================
    // getMessages() — exception translation
    //
    // getMessages() casts folder.folder to BackupImapStoreDelegate.BackupFolder.
    // To test the exception branches we mock BackupImapStoreDelegate.BackupFolder
    // directly (it is a non-final class in this package, mockable by Mockito).
    // =========================================================================

    @Test
    public void getMessages_messagingException_translatesTo_MailException() throws Exception {
        // Arrange: create a mock BackupFolder (extends ImapFolder) that throws MessagingException
        K9MailTransport.BackupImapStoreDelegate.BackupFolder mockBackupFolder =
                mock(K9MailTransport.BackupImapStoreDelegate.BackupFolder.class);
        doThrow(new MessagingException("search failed"))
                .when(mockBackupFolder).getMessagesInternal(
                        ArgumentMatchers.anyInt(),
                        ArgumentMatchers.anyBoolean(),
                        ArgumentMatchers.any());

        // BackupFolderHandle wraps an ImapFolder; BackupFolder extends ImapFolder
        BackupFolderHandle folderHandle = new BackupFolderHandle(mockBackupFolder);

        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport transport = new K9MailTransport(delegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        try {
            transport.getMessages(folderHandle, 100, false, null);
            throw new AssertionError("Expected MailException");
        } catch (MailException e) {
            assertThat(e).isNotInstanceOf(TemporaryImapException.class);
        }
    }

    @Test
    public void getMessages_imapPrefixException_translatesTo_TemporaryImapException()
            throws Exception {
        K9MailTransport.BackupImapStoreDelegate.BackupFolder mockBackupFolder =
                mock(K9MailTransport.BackupImapStoreDelegate.BackupFolder.class);
        doThrow(new MessagingException("Unable to get IMAP prefix"))
                .when(mockBackupFolder).getMessagesInternal(
                        ArgumentMatchers.anyInt(),
                        ArgumentMatchers.anyBoolean(),
                        ArgumentMatchers.any());

        BackupFolderHandle folderHandle = new BackupFolderHandle(mockBackupFolder);

        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport transport = new K9MailTransport(delegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        try {
            transport.getMessages(folderHandle, 100, false, null);
            throw new AssertionError("Expected TemporaryImapException");
        } catch (TemporaryImapException e) {
            // expected
        }
    }

    @Test
    public void getMessages_authFailedException_translatesTo_RequiresLoginException()
            throws Exception {
        K9MailTransport.BackupImapStoreDelegate.BackupFolder mockBackupFolder =
                mock(K9MailTransport.BackupImapStoreDelegate.BackupFolder.class);
        doThrow(new AuthenticationFailedException("session expired"))
                .when(mockBackupFolder).getMessagesInternal(
                        ArgumentMatchers.anyInt(),
                        ArgumentMatchers.anyBoolean(),
                        ArgumentMatchers.any());

        BackupFolderHandle folderHandle = new BackupFolderHandle(mockBackupFolder);

        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport transport = new K9MailTransport(delegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        try {
            transport.getMessages(folderHandle, 100, false, null);
            throw new AssertionError("Expected RequiresLoginException");
        } catch (RequiresLoginException e) {
            // expected
        }
    }

    @Test
    public void getMessages_xOAuth2Exception_translatesTo_XOAuth2FailedException()
            throws Exception {
        K9MailTransport.BackupImapStoreDelegate.BackupFolder mockBackupFolder =
                mock(K9MailTransport.BackupImapStoreDelegate.BackupFolder.class);
        XOAuth2AuthenticationFailedException xoauthEx =
                mock(XOAuth2AuthenticationFailedException.class);
        when(xoauthEx.getStatus()).thenReturn(400);
        doThrow(xoauthEx)
                .when(mockBackupFolder).getMessagesInternal(
                        ArgumentMatchers.anyInt(),
                        ArgumentMatchers.anyBoolean(),
                        ArgumentMatchers.any());

        BackupFolderHandle folderHandle = new BackupFolderHandle(mockBackupFolder);

        K9MailTransport.BackupImapStoreDelegate delegate = createDelegate();
        K9MailTransport transport = new K9MailTransport(delegate,
                new DefaultTrustedSocketFactory(RuntimeEnvironment.application));

        try {
            transport.getMessages(folderHandle, 100, false, null);
            throw new AssertionError("Expected XOAuth2FailedException");
        } catch (XOAuth2FailedException e) {
            assertThat(e.getStatus()).isEqualTo(400);
        }
    }
}
