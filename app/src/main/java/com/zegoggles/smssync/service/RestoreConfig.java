package com.zegoggles.smssync.service;

import com.zegoggles.smssync.mail.transport.MailTransport;

/**
 * Configuration snapshot for a single restore run.
 *
 * <p>U-026: {@code imapStore} field type changed from {@code BackupImapStore} to
 * {@link MailTransport} (app-owned ACL port). {@code retryWithStore} signature updated
 * accordingly. All k-9 imports removed from this class.
 */
public class RestoreConfig {
    final int tries;
    final boolean restoreSms;
    final boolean restoreCallLog;
    final boolean restoreOnlyStarred;
    final int maxRestore;
    final int currentRestoredItem;
    final MailTransport imapStore;

    public RestoreConfig(MailTransport imapStore,
                         int tries,
                         boolean restoreSms,
                         boolean restoreCallLog,
                         boolean restoreOnlyStarred,
                         int maxRestore,
                         int currentRestoredItem) {

        this.tries = tries;
        this.imapStore = imapStore;
        this.restoreSms = restoreSms;
        this.restoreCallLog = restoreCallLog;
        this.restoreOnlyStarred = restoreOnlyStarred;
        this.maxRestore = maxRestore;
        this.currentRestoredItem = currentRestoredItem;
    }

    /**
     * U-026: parameter type updated from {@code BackupImapStore} to {@link MailTransport}.
     */
    public RestoreConfig retryWithStore(int currentItem, MailTransport transport) {
        return new RestoreConfig(
                transport,
                tries + 1,
                restoreSms,
                restoreCallLog,
                restoreOnlyStarred,
                maxRestore,
                currentItem
        );
    }

    @Override public String toString() {
        return "RestoreConfig{" +
                "currentTry=" + tries +
                ", restoreSms=" + restoreSms +
                ", restoreCallLog=" + restoreCallLog +
                ", restoreOnlyStarred=" + restoreOnlyStarred +
                ", maxRestore=" + maxRestore +
                ", currentRestoredItem=" + currentRestoredItem +
                ", imapStore=" + imapStore +
                '}';
    }
}
