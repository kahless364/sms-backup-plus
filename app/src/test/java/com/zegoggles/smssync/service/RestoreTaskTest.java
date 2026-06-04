package com.zegoggles.smssync.service;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.Telephony;
import com.zegoggles.smssync.Consts;
import com.zegoggles.smssync.auth.TokenRefresher;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.MessageConverter;
import com.zegoggles.smssync.mail.transport.BackupFolderHandle;
import com.zegoggles.smssync.mail.transport.MailMessageHandle;
import com.zegoggles.smssync.mail.transport.MailTransport;
import com.zegoggles.smssync.mail.transport.MailTransportTestFactories;
import com.zegoggles.smssync.mail.transport.MessageImportResult;
import com.zegoggles.smssync.preferences.DataTypePreferences;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.service.state.RestoreState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * U-026: RestoreTaskTest updated — BackupImapStore/BackupFolder mocks replaced with
 * MailTransport/BackupFolderHandle/MailMessageHandle/MessageImportResult.
 * All IMAP interactions now go through the MailTransport port.
 * [MailTransportTestFactories] provides package-scoped constructors for handle types.
 */
@RunWith(RobolectricTestRunner.class)
public class RestoreTaskTest {
    RestoreTask task;
    RestoreConfig config;
    // U-026: MailTransport replaces BackupImapStore; BackupFolderHandle replaces BackupFolder
    @Mock MailTransport store;
    BackupFolderHandle folder;
    @Mock SmsRestoreService service;
    @Mock RestoreState state;
    @Mock MessageConverter converter;
    @Mock ContentResolver resolver;
    @Mock TokenRefresher tokenRefresher;

    @Before
    public void before() throws Exception {
        openMocks(this);
        // U-026: BackupFolderHandle created via test factory (package-private constructor)
        folder = MailTransportTestFactories.createFolderHandle();
        config = new RestoreConfig(store, 0, true, false, false, -1, 0);
        when(service.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        when(service.getState()).thenReturn(state);
        when(service.getPreferences()).thenReturn(new Preferences(RuntimeEnvironment.application));

        // U-026: openFolder replaces store.getFolder; returns BackupFolderHandle
        when(store.openFolder(any(DataType.class), any(DataTypePreferences.class))).thenReturn(folder);
        // U-026: getMessages returns empty list by default (no-op restore)
        when(store.getMessages(any(BackupFolderHandle.class), anyInt(), anyBoolean(), nullable(Date.class)))
                .thenReturn(Collections.<MailMessageHandle>emptyList());

        task = new RestoreTask(service, converter, resolver, tokenRefresher);
    }

    @Test public void shouldAcquireAndReleaseLocksDuringRestore() throws Exception {
        task.doInBackground(config);
        verify(service).acquireLocks();
        verify(service).releaseLocks();
    }

    @Test public void shouldVerifyStoreSettings() throws Exception {
        task.doInBackground(config);
        // U-026: checkSettings called via transport port
        verify(store).checkSettings();
    }

    @Test public void shouldCloseFolders() throws Exception {
        task.doInBackground(config);
        // U-026: closeFolders called via transport port
        verify(store).closeFolders();
    }

    @Test
    public void shouldRestoreItems() throws Exception {
        Date now = new Date();
        ContentValues values = new ContentValues();
        values.put(Telephony.TextBasedSmsColumns.TYPE, Telephony.TextBasedSmsColumns.MESSAGE_TYPE_INBOX);
        values.put(Telephony.TextBasedSmsColumns.DATE, now.getTime());

        // U-026: MailMessageHandle created via test factory (package-private constructor in mail.transport)
        MailMessageHandle mockHandle = MailTransportTestFactories.createHandle("msg-uid-1");

        // Mock getMessages to return our test handle
        List<MailMessageHandle> messages = new ArrayList<MailMessageHandle>();
        messages.add(mockHandle);
        when(store.getMessages(any(BackupFolderHandle.class), anyInt(), anyBoolean(), nullable(Date.class)))
                .thenReturn(messages);

        // U-026: importMessageBody returns a MessageImportResult (replaces per-message fetch + converter calls)
        // The bounded-residual converter is called inside K9MailTransport.importMessageBody; at the service.*
        // boundary the test stubs the result directly.
        MessageImportResult importResult = new MessageImportResult("msg-uid-1", DataType.SMS, values);
        when(store.importMessageBody(any(BackupFolderHandle.class), any(MailMessageHandle.class), any(MessageConverter.class)))
                .thenReturn(importResult);

        when(resolver.insert(Consts.SMS_PROVIDER, values)).thenReturn(Uri.parse("content://sms/123"));
        task.doInBackground(config);

        verify(resolver).insert(Consts.SMS_PROVIDER, values);
        verify(resolver).delete(Uri.parse("content://sms/conversations/-1"), null, null);

        assertThat(service.getPreferences().getDataTypePreferences().getMaxSyncedDate(DataType.SMS)).isEqualTo(now.getTime());
        assertThat(task.getSmsIds()).containsExactly("123");

        verify(store).closeFolders();
    }
}
