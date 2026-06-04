package com.zegoggles.smssync.mail.transport;

import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;

import static org.mockito.Mockito.mock;

/**
 * Test-only factory helpers for mail.transport package-private types.
 *
 * <p>{@link MailMessageHandle} and {@link BackupFolderHandle} have package-private
 * constructors (so that only {@link K9MailTransport} can create them in production code).
 * This factory lives in the same package as the types and provides construction helpers
 * for unit tests that need to create instances without going through K9MailTransport.
 *
 * <p>U-026: Added for RestoreTaskTest and RestoreWorkerTest which mock the MailTransport
 * port and need concrete handle instances for stubs/verifications.
 */
public final class MailTransportTestFactories {

    private MailTransportTestFactories() { /* static helpers only */ }

    /**
     * Creates a {@link MailMessageHandle} with the given UID and a mock {@link ImapMessage}.
     * For use in unit tests that stub {@link MailTransport#getMessages} return values.
     */
    public static MailMessageHandle createHandle(String uid) {
        ImapMessage mockMessage = mock(ImapMessage.class);
        return new MailMessageHandle(uid, mockMessage);
    }

    /**
     * Creates a {@link BackupFolderHandle} wrapping a mock {@link ImapFolder}.
     * For use in unit tests that stub {@link MailTransport#openFolder} return values.
     */
    public static BackupFolderHandle createFolderHandle() {
        ImapFolder mockFolder = mock(ImapFolder.class);
        return new BackupFolderHandle(mockFolder);
    }
}
