package com.zegoggles.smssync.service;

import com.zegoggles.smssync.mail.transport.MailTransport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

/**
 * U-006 characterization tests for RestoreConfig.
 * Pins restore configuration behavior before SmsRestoreService refactoring.
 *
 * U-026: BackupImapStore mocks replaced with MailTransport mocks.
 * RestoreConfig.imapStore is now typed as MailTransport per U-026 AC-1.
 */
@RunWith(RobolectricTestRunner.class)
public class RestoreConfigTest {

    @Test public void constructor_storesAllFields() {
        // U-026: MailTransport replaces BackupImapStore
        MailTransport transport = mock(MailTransport.class);
        RestoreConfig config = new RestoreConfig(transport, 0, true, false, false, 100, 0);

        assertThat(config.imapStore).isSameInstanceAs(transport);
        assertThat(config.tries).isEqualTo(0);
        assertThat(config.restoreSms).isTrue();
        assertThat(config.restoreCallLog).isFalse();
        assertThat(config.restoreOnlyStarred).isFalse();
        assertThat(config.maxRestore).isEqualTo(100);
        assertThat(config.currentRestoredItem).isEqualTo(0);
    }

    @Test public void retryWithStore_incrementsTriesAndUpdatesStore() {
        // U-026: MailTransport replaces BackupImapStore
        MailTransport transport1 = mock(MailTransport.class);
        MailTransport transport2 = mock(MailTransport.class);
        RestoreConfig original = new RestoreConfig(transport1, 0, true, false, false, 100, 0);
        RestoreConfig retry = original.retryWithStore(5, transport2);

        assertThat(retry.tries).isEqualTo(1);
        assertThat(retry.imapStore).isSameInstanceAs(transport2);
        assertThat(retry.currentRestoredItem).isEqualTo(5);
        assertThat(retry.restoreSms).isTrue();
        assertThat(retry.maxRestore).isEqualTo(100);
    }

    @Test public void toString_containsFields() {
        // U-026: MailTransport replaces BackupImapStore
        MailTransport transport = mock(MailTransport.class);
        RestoreConfig config = new RestoreConfig(transport, 2, true, true, false, 50, 10);
        String str = config.toString();
        assertThat(str).contains("RestoreConfig");
        assertThat(str).contains("2");   // currentTry
        assertThat(str).contains("50");  // maxRestore
    }
}
