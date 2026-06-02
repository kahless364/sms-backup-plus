package com.zegoggles.smssync.service;

import com.zegoggles.smssync.mail.BackupImapStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

/**
 * U-006 characterization tests for RestoreConfig.
 * Pins restore configuration behavior before SmsRestoreService refactoring.
 */
@RunWith(RobolectricTestRunner.class)
public class RestoreConfigTest {

    @Test public void constructor_storesAllFields() {
        BackupImapStore store = mock(BackupImapStore.class);
        RestoreConfig config = new RestoreConfig(store, 0, true, false, false, 100, 0);

        assertThat(config.imapStore).isSameInstanceAs(store);
        assertThat(config.tries).isEqualTo(0);
        assertThat(config.restoreSms).isTrue();
        assertThat(config.restoreCallLog).isFalse();
        assertThat(config.restoreOnlyStarred).isFalse();
        assertThat(config.maxRestore).isEqualTo(100);
        assertThat(config.currentRestoredItem).isEqualTo(0);
    }

    @Test public void retryWithStore_incrementsTriesAndUpdatesStore() {
        BackupImapStore store1 = mock(BackupImapStore.class);
        BackupImapStore store2 = mock(BackupImapStore.class);
        RestoreConfig original = new RestoreConfig(store1, 0, true, false, false, 100, 0);
        RestoreConfig retry = original.retryWithStore(5, store2);

        assertThat(retry.tries).isEqualTo(1);
        assertThat(retry.imapStore).isSameInstanceAs(store2);
        assertThat(retry.currentRestoredItem).isEqualTo(5);
        assertThat(retry.restoreSms).isTrue();
        assertThat(retry.maxRestore).isEqualTo(100);
    }

    @Test public void toString_containsFields() {
        BackupImapStore store = mock(BackupImapStore.class);
        RestoreConfig config = new RestoreConfig(store, 2, true, true, false, 50, 10);
        String str = config.toString();
        assertThat(str).contains("RestoreConfig");
        assertThat(str).contains("2");   // currentTry
        assertThat(str).contains("50");  // maxRestore
    }
}
