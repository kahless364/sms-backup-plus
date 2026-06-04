package com.zegoggles.smssync.service;

import androidx.annotation.NonNull;
import com.zegoggles.smssync.contacts.ContactGroup;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.transport.MailTransport;

import java.util.EnumSet;

/**
 * Configuration snapshot for a single backup run.
 *
 * <p>U-026: {@code imapStore} field type changed from {@code BackupImapStore} to
 * {@link MailTransport} (app-owned ACL port). {@code retryWithStore} renamed to
 * {@code retryWithTransport}. All k-9 imports removed from this class.
 */
public class BackupConfig {
    public final MailTransport imapStore;
    public final int currentTry;
    public final int maxItemsPerSync;
    public final ContactGroup groupToBackup;
    public final BackupType backupType;
    public final boolean debug;
    public final EnumSet<DataType> typesToBackup;

    BackupConfig(@NonNull MailTransport imapStore,
                 int currentTry,
                 int maxItemsPerSync,
                 @NonNull ContactGroup groupToBackup,
                 @NonNull BackupType backupType,
                 @NonNull EnumSet<DataType> typesToBackup,
                 boolean debug) {
        if (imapStore == null) throw new IllegalArgumentException("need imapstore");
        if (typesToBackup == null || typesToBackup.isEmpty()) throw new IllegalArgumentException("need to specify types to backup");
        if (currentTry < 0) throw new IllegalArgumentException("currentTry < 0");

        this.imapStore = imapStore;
        this.currentTry = currentTry;
        this.maxItemsPerSync = maxItemsPerSync;
        this.groupToBackup = groupToBackup;
        this.backupType = backupType;
        this.debug = debug;
        this.typesToBackup = typesToBackup;
    }

    /**
     * U-026: renamed from {@code retryWithStore(BackupImapStore)} to
     * {@code retryWithTransport(MailTransport)} per AC-2(e) and IC-3.
     * Old name kept as a bridge overload to avoid breaking BackupWorker.kt (which also
     * calls retryWithStore) until that file is fully migrated in this story.
     */
    public BackupConfig retryWithTransport(MailTransport transport) {
        return new BackupConfig(transport, currentTry + 1,
                maxItemsPerSync,
                groupToBackup,
                backupType,
                typesToBackup, debug);
    }

    /** @deprecated Use {@link #retryWithTransport(MailTransport)} — kept for call-site
     *  migration compatibility within U-026. */
    @Deprecated
    public BackupConfig retryWithStore(MailTransport store) {
        return retryWithTransport(store);
    }


    @Override public String toString() {
        return "BackupConfig{" +
                "imap=" + imapStore +
                ", currentTry=" + currentTry +
                ", maxItemsPerSync=" + maxItemsPerSync +
                ", groupToBackup=" + groupToBackup +
                ", backupType=" + backupType +
                ", debug=" + debug +
                ", typesToBackup=" + typesToBackup +
                '}';
    }
}
