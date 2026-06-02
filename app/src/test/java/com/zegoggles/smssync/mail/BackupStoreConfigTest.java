package com.zegoggles.smssync.mail;

import com.fsck.k9.mail.NetworkType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

/**
 * U-006 characterization tests for BackupStoreConfig.
 * Pins the StoreConfig adapter behavior before BackupImapStore refactoring.
 * BackupStoreConfig wraps the IMAP store URI and provides sensible defaults
 * for the k-9 StoreConfig interface.
 */
@RunWith(RobolectricTestRunner.class)
public class BackupStoreConfigTest {

    private static final String TEST_URI = "imap+ssl+://XOAUTH2:user:token@imap.gmail.com:993";

    @Test public void getStoreUri_returnsConstructorValue() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.getStoreUri()).isEqualTo(TEST_URI);
    }

    @Test public void getTransportUri_returnsNull() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.getTransportUri()).isNull();
    }

    @Test public void subscribedFoldersOnly_returnsFalse() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.subscribedFoldersOnly()).isFalse();
    }

    @Test public void useCompression_returnsFalse_forAnyNetworkType() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.useCompression(NetworkType.WIFI)).isFalse();
        assertThat(config.useCompression(NetworkType.MOBILE)).isFalse();
        assertThat(config.useCompression(NetworkType.OTHER)).isFalse();
    }

    @Test public void getInboxFolderName_returnsINBOX() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.getInboxFolderName()).isEqualTo("INBOX");
    }

    @Test public void getOutboxFolderName_returnsNull() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.getOutboxFolderName()).isNull();
    }

    @Test public void getDraftsFolderName_returnsNull() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.getDraftsFolderName()).isNull();
    }

    @Test public void setters_doNotThrow() {
        // These methods are no-ops in BackupStoreConfig (the store is read-only)
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        config.setArchiveFolderName("Archive");
        config.setDraftsFolderName("Drafts");
        config.setTrashFolderName("Trash");
        config.setSpamFolderName("Spam");
        config.setSentFolderName("Sent");
        config.setAutoExpandFolderName("INBOX");
        config.setInboxFolderName("INBOX");
        // No assertion needed — verifies no exception is thrown
    }

    @Test public void getMaximumAutoDownloadMessageSize_returnsZero() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.getMaximumAutoDownloadMessageSize()).isEqualTo(0);
    }

    @Test public void allowRemoteSearch_returnsFalse() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.allowRemoteSearch()).isFalse();
    }

    @Test public void isRemoteSearchFullText_returnsFalse() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.isRemoteSearchFullText()).isFalse();
    }

    @Test public void isPushPollOnConnect_returnsFalse() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.isPushPollOnConnect()).isFalse();
    }

    @Test public void getDisplayCount_returnsZero() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.getDisplayCount()).isEqualTo(0);
    }

    @Test public void getIdleRefreshMinutes_returnsZero() {
        BackupStoreConfig config = new BackupStoreConfig(TEST_URI);
        assertThat(config.getIdleRefreshMinutes()).isEqualTo(0);
    }
}
