package com.zegoggles.smssync.service;

import com.zegoggles.smssync.contacts.ContactGroup;
import com.zegoggles.smssync.mail.BackupImapStore;
import com.zegoggles.smssync.mail.DataType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.EnumSet;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class BackupConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckForDataTypesEmpty() throws Exception {
        new BackupConfig(mock(BackupImapStore.class),
                0,
                -1,
                ContactGroup.EVERYBODY,
                BackupType.MANUAL,
                EnumSet.noneOf(DataType.class),
                false);

    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckForDataTypesNull() throws Exception {
        new BackupConfig(mock(BackupImapStore.class),
                0,
                -1,
                ContactGroup.EVERYBODY,
                BackupType.MANUAL,
                null,
                false);
    }


    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckForPositiveTry() throws Exception {
        new BackupConfig(mock(BackupImapStore.class),
                -1,
                -1,
                ContactGroup.EVERYBODY,
                BackupType.MANUAL,
                EnumSet.of(DataType.MMS),
                false);
    }

    // U-006 coverage additions

    @Test public void toString_containsFields() throws Exception {
        BackupConfig config = new BackupConfig(mock(BackupImapStore.class),
                0, 100, ContactGroup.EVERYBODY, BackupType.MANUAL,
                EnumSet.of(DataType.SMS), false);
        String str = config.toString();
        assertThat(str).contains("BackupConfig");
        assertThat(str).contains("0");  // currentTry
        assertThat(str).contains("100"); // maxItemsPerSync
    }

    @Test public void retryWithStore_incrementsCurrentTry() throws Exception {
        BackupImapStore store1 = mock(BackupImapStore.class);
        BackupImapStore store2 = mock(BackupImapStore.class);
        BackupConfig original = new BackupConfig(store1, 0, 100, ContactGroup.EVERYBODY,
                BackupType.MANUAL, EnumSet.of(DataType.SMS), false);
        BackupConfig retry = original.retryWithStore(store2);

        assertThat(retry.currentTry).isEqualTo(1);
        assertThat(retry.imapStore).isSameInstanceAs(store2);
        assertThat(retry.maxItemsPerSync).isEqualTo(100);
        assertThat(retry.backupType).isEqualTo(BackupType.MANUAL);
    }
}
