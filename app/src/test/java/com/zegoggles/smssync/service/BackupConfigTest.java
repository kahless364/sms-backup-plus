package com.zegoggles.smssync.service;

import com.zegoggles.smssync.contacts.ContactGroup;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.transport.MailTransport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.EnumSet;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

/**
 * U-026: BackupConfigTest updated — BackupImapStore mocks replaced with MailTransport mocks.
 * BackupConfig.imapStore is now typed as MailTransport per U-026 AC-1.
 */
@RunWith(RobolectricTestRunner.class)
public class BackupConfigTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckForDataTypesEmpty() throws Exception {
        new BackupConfig(mock(MailTransport.class),
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
        new BackupConfig(mock(MailTransport.class),
                0,
                -1,
                ContactGroup.EVERYBODY,
                BackupType.MANUAL,
                null,
                false);
    }


    @Test(expected = IllegalArgumentException.class)
    public void shouldCheckForPositiveTry() throws Exception {
        new BackupConfig(mock(MailTransport.class),
                -1,
                -1,
                ContactGroup.EVERYBODY,
                BackupType.MANUAL,
                EnumSet.of(DataType.MMS),
                false);
    }

    // U-006 coverage additions

    @Test public void toString_containsFields() throws Exception {
        BackupConfig config = new BackupConfig(mock(MailTransport.class),
                0, 100, ContactGroup.EVERYBODY, BackupType.MANUAL,
                EnumSet.of(DataType.SMS), false);
        String str = config.toString();
        assertThat(str).contains("BackupConfig");
        assertThat(str).contains("0");  // currentTry
        assertThat(str).contains("100"); // maxItemsPerSync
    }

    @Test public void retryWithStore_incrementsCurrentTry() throws Exception {
        // U-026: MailTransport replaces BackupImapStore in BackupConfig
        MailTransport transport1 = mock(MailTransport.class);
        MailTransport transport2 = mock(MailTransport.class);
        BackupConfig original = new BackupConfig(transport1, 0, 100, ContactGroup.EVERYBODY,
                BackupType.MANUAL, EnumSet.of(DataType.SMS), false);
        BackupConfig retry = original.retryWithTransport(transport2);

        assertThat(retry.currentTry).isEqualTo(1);
        assertThat(retry.imapStore).isSameInstanceAs(transport2);
        assertThat(retry.maxItemsPerSync).isEqualTo(100);
        assertThat(retry.backupType).isEqualTo(BackupType.MANUAL);
    }
}
