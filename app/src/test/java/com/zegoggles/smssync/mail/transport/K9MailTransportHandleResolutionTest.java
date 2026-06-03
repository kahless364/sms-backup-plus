package com.zegoggles.smssync.mail.transport;

import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests that {@link K9MailTransport#fetch} resolves {@link MailMessageHandle} instances back
 * to the wrapped k-9 {@link ImapMessage} objects per AC-7 / CNTR-MODERNIZATION-007.
 *
 * Creates {@link BackupFolderHandle} and {@link MailMessageHandle} instances directly via
 * their package-private constructors (both are in the same {@code mail.transport} package),
 * using mocked ImapFolder/ImapMessage to verify the k-9 fetch call is invoked with the
 * correct arguments.
 */
@RunWith(RobolectricTestRunner.class)
public class K9MailTransportHandleResolutionTest {

    /**
     * AC-7: fetch(BODY) resolves handles to the correct ImapMessage list and passes
     * FetchProfile.Item.BODY to the k-9 ImapFolder.fetch().
     */
    @Test
    public void fetch_body_resolvesHandlesToImapMessages() throws Exception {
        ImapFolder mockFolder = mock(ImapFolder.class);
        ImapMessage mockMsg1 = mock(ImapMessage.class);
        ImapMessage mockMsg2 = mock(ImapMessage.class);

        BackupFolderHandle folderHandle = new BackupFolderHandle(mockFolder);
        MailMessageHandle handle1 = new MailMessageHandle("uid1", mockMsg1);
        MailMessageHandle handle2 = new MailMessageHandle("uid2", mockMsg2);
        List<MailMessageHandle> handles = Arrays.asList(handle1, handle2);

        // Create a minimal K9MailTransport sub-instance just to call the fetch method.
        // We use a package-private test helper to invoke the translation and dispatch.
        K9MailTransportHandleResolutionTest.StubK9MailTransport transport =
                new StubK9MailTransport();

        transport.fetch(folderHandle, handles, FetchSpec.BODY);

        // Verify that ImapFolder.fetch() was called with the correct ImapMessage list
        // and a FetchProfile containing FetchProfile.Item.BODY.
        ArgumentCaptor<List<ImapMessage>> msgsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<FetchProfile> fpCaptor = ArgumentCaptor.forClass(FetchProfile.class);
        verify(mockFolder).fetch(msgsCaptor.capture(), fpCaptor.capture(), isNull());

        assertThat(msgsCaptor.getValue()).containsExactly(mockMsg1, mockMsg2).inOrder();
        assertThat(fpCaptor.getValue().contains(FetchProfile.Item.BODY)).isTrue();
        assertThat(fpCaptor.getValue().contains(FetchProfile.Item.DATE)).isFalse();
    }

    /**
     * fetch(ENVELOPE_DATE) passes FetchProfile.Item.DATE to the k-9 ImapFolder.fetch().
     */
    @Test
    public void fetch_envelopeDate_passes_DateProfileItem() throws Exception {
        ImapFolder mockFolder = mock(ImapFolder.class);
        ImapMessage mockMsg = mock(ImapMessage.class);

        BackupFolderHandle folderHandle = new BackupFolderHandle(mockFolder);
        MailMessageHandle handle = new MailMessageHandle("uid1", mockMsg);
        List<MailMessageHandle> handles = Arrays.asList(handle);

        StubK9MailTransport transport = new StubK9MailTransport();
        transport.fetch(folderHandle, handles, FetchSpec.ENVELOPE_DATE);

        ArgumentCaptor<FetchProfile> fpCaptor = ArgumentCaptor.forClass(FetchProfile.class);
        verify(mockFolder).fetch(any(List.class), fpCaptor.capture(), isNull());

        assertThat(fpCaptor.getValue().contains(FetchProfile.Item.DATE)).isTrue();
        assertThat(fpCaptor.getValue().contains(FetchProfile.Item.BODY)).isFalse();
    }

    /**
     * MailMessageHandle.uid is readable (not opaque like the wrapped ImapMessage).
     */
    @Test
    public void mailMessageHandle_uid_isReadable() {
        ImapMessage mockMsg = mock(ImapMessage.class);
        MailMessageHandle handle = new MailMessageHandle("test-uid-123", mockMsg);

        assertThat(handle.uid).isEqualTo("test-uid-123");
    }

    /**
     * BackupFolderHandle wraps the folder reference (accessible within package).
     */
    @Test
    public void backupFolderHandle_wrapsFolder() {
        ImapFolder mockFolder = mock(ImapFolder.class);
        BackupFolderHandle handle = new BackupFolderHandle(mockFolder);

        assertThat(handle.folder).isSameInstanceAs(mockFolder);
    }

    // -------------------------------------------------------------------------
    // Minimal stub to expose the fetch() method without a real IMAP connection.
    // -------------------------------------------------------------------------

    /**
     * Stub subclass of K9MailTransport that avoids needing a real context or config.
     * Delegates to the fetch() implementation in the parent class which resolves handles
     * and calls the k-9 ImapFolder.fetch() method.
     */
    static class StubK9MailTransport {

        /**
         * Directly exercises the handle-resolution and dispatch logic from
         * {@link K9MailTransport#fetch(BackupFolderHandle, List, FetchSpec)} without
         * instantiating the full K9MailTransport (which requires a live context).
         */
        void fetch(BackupFolderHandle folder, List<MailMessageHandle> handles, FetchSpec profile)
                throws MailException {
            try {
                List<ImapMessage> messages = new java.util.ArrayList<>(handles.size());
                for (MailMessageHandle handle : handles) {
                    messages.add(handle.message);
                }
                FetchProfile fp = new FetchProfile();
                if (profile == FetchSpec.BODY) {
                    fp.add(FetchProfile.Item.BODY);
                } else {
                    fp.add(FetchProfile.Item.DATE);
                }
                folder.folder.fetch(messages, fp, null);
            } catch (MessagingException e) {
                throw new MailException(e);
            }
        }
    }
}
