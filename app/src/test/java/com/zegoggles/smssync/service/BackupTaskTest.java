package com.zegoggles.smssync.service;


import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.MimeMessage;
import com.zegoggles.smssync.auth.TokenRefreshException;
import com.zegoggles.smssync.auth.TokenRefresher;
import com.zegoggles.smssync.contacts.ContactAccessor;
import com.zegoggles.smssync.contacts.ContactGroup;
import com.zegoggles.smssync.contacts.ContactGroupIds;
import com.zegoggles.smssync.mail.ConversionResult;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.mail.MessageConverter;
import com.zegoggles.smssync.mail.transport.BackupFolderHandle;
import com.zegoggles.smssync.mail.transport.MailTransport;
import com.zegoggles.smssync.mail.transport.XOAuth2FailedException;
import com.zegoggles.smssync.preferences.AuthPreferences;
import com.zegoggles.smssync.preferences.DataTypePreferences;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.service.state.BackupState;
import com.zegoggles.smssync.service.state.SmsSyncState;
import dagger.Lazy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.EnumSet;
import java.util.HashMap;

import static com.google.common.truth.Truth.assertThat;
import static com.zegoggles.smssync.mail.DataType.CALLLOG;
import static com.zegoggles.smssync.mail.DataType.MMS;
import static com.zegoggles.smssync.mail.DataType.SMS;
import static com.zegoggles.smssync.service.BackupItemsFetcher.emptyCursor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * U-026: BackupTaskTest updated — BackupImapStore/BackupFolder mocks replaced with
 * MailTransport/BackupFolderHandle mocks. All IMAP interactions now go through the
 * MailTransport port (transport.openFolder, transport.appendMessages, transport.closeFolders).
 */
@RunWith(RobolectricTestRunner.class)
public class BackupTaskTest {
    BackupTask task;
    BackupConfig config;
    Context context;
    // U-026: MailTransport replaces BackupImapStore; BackupFolderHandle replaces BackupFolder
    @Mock MailTransport store;
    @Mock BackupFolderHandle folder;
    @Mock SmsBackupService service;
    @Mock BackupState state;
    @Mock BackupItemsFetcher fetcher;
    @Mock MessageConverter converter;
    @Mock CalendarSyncer syncer;
    @Mock AuthPreferences authPreferences;
    @Mock DataTypePreferences dataTypePreferences;
    @Mock Preferences preferences;
    @Mock ContactAccessor accessor;
    @Mock TokenRefresher tokenRefresher;

    @Before public void before() {
        openMocks(this);
        config = getBackupConfig(EnumSet.of(SMS));
        when(service.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        when(service.getState()).thenReturn(state);
        when(preferences.getDataTypePreferences()).thenReturn(dataTypePreferences);

        // U-023 AC-4/AC-6: BackupTask now has a single @Inject constructor with Lazy<CalendarSyncer>.
        // The test wraps the @Mock CalendarSyncer in a lambda to satisfy the Lazy<T> interface.
        // All test assertions are unchanged — the mock is still used via lazy.get() in production code.
        task = new BackupTask(service, fetcher, converter, () -> syncer, authPreferences, preferences, accessor, tokenRefresher);
        context = RuntimeEnvironment.application;
    }

    private BackupConfig getBackupConfig(EnumSet<DataType> types) {
        return new BackupConfig(store, 0, 100, new ContactGroup(-1), BackupType.MANUAL, types,
                false
        );
    }

    @Test public void shouldAcquireAndReleaseLocksDuringBackup() throws Exception {
        mockAllFetchEmpty();

        task.doInBackground(config);

        verify(service).acquireLocks();
        verify(service).releaseLocks();
        verify(service).transition(SmsSyncState.FINISHED_BACKUP, null);
    }

    @Test public void shouldVerifyStoreSettings() throws Exception {
        mockFetch(SMS, 1);
        when(converter.convertMessages(any(Cursor.class), eq(SMS))).thenReturn(result(SMS, 1));
        // U-026: openFolder replaces store.getFolder
        when(store.openFolder(eq(SMS), same(dataTypePreferences))).thenReturn(folder);
        task.doInBackground(config);
        // U-026: checkSettings is called via transport port
        verify(store).checkSettings();
    }

    @Test public void shouldBackupItems() throws Exception {
        mockFetch(SMS, 1);

        when(converter.convertMessages(any(Cursor.class), eq(SMS))).thenReturn(result(SMS, 1));
        // U-026: openFolder replaces store.getFolder
        when(store.openFolder(notNull(), same(dataTypePreferences))).thenReturn(folder);

        BackupState finalState = task.doInBackground(config);

        // U-026: appendMessages is called via transport port (not folder.appendMessages directly)
        verify(store).appendMessages(same(folder), any());

        verify(service).transition(SmsSyncState.LOGIN, null);
        verify(service).transition(SmsSyncState.CALC, null);

        assertThat(finalState).isNotNull();
        assertThat(finalState.isFinished()).isTrue();
        assertThat(finalState.currentSyncedItems).isEqualTo(1);
        assertThat(finalState.itemsToSync).isEqualTo(1);
        assertThat(finalState.backupType).isEqualTo(config.backupType);
    }

    @Test
    public void shouldBackupMultipleTypes() throws Exception {
        mockFetch(SMS, 1);
        mockFetch(MMS, 2);
        // U-026: openFolder replaces store.getFolder
        when(store.openFolder(notNull(), same(dataTypePreferences))).thenReturn(folder);
        when(converter.convertMessages(any(Cursor.class), any(DataType.class))).thenReturn(result(SMS, 1));

        BackupState finalState = task.doInBackground(getBackupConfig(EnumSet.of(SMS, MMS)));

        assertThat(finalState.currentSyncedItems).isEqualTo(3);

        // U-026: appendMessages via transport (3 times)
        verify(store, times(3)).appendMessages(any(), any());
    }

    @Test public void shouldCreateFoldersLazilyOnlyForNeededTypes() throws Exception {
        mockFetch(SMS, 1);

        when(converter.convertMessages(any(Cursor.class), eq(SMS))).thenReturn(result(SMS, 1));
        // U-026: openFolder replaces getFolder
        when(store.openFolder(notNull(), same(dataTypePreferences))).thenReturn(folder);

        task.doInBackground(config);

        // U-026: openFolder was called for SMS only
        verify(store).openFolder(SMS, dataTypePreferences);
        verify(store, never()).openFolder(MMS, dataTypePreferences);
        verify(store, never()).openFolder(CALLLOG, dataTypePreferences);
    }

    @Test public void shouldCloseImapFolderAfterBackup() throws Exception {
        mockFetch(SMS, 1);
        when(converter.convertMessages(any(Cursor.class), eq(SMS))).thenReturn(result(SMS, 1));
        // U-026: openFolder replaces getFolder
        when(store.openFolder(notNull(), same(dataTypePreferences))).thenReturn(folder);

        task.doInBackground(config);

        // U-026: closeFolders via transport port
        verify(store).closeFolders();
    }

    @Test public void shouldCreateNoFoldersIfNoItemsToBackup() throws Exception {
        mockFetch(SMS, 0);
        task.doInBackground(config);
        // U-026: no openFolder calls when nothing to backup
        verifyNoInteractions(store);
    }

    @Test public void shouldSkipItems() throws Exception {
        when(fetcher.getMostRecentTimestamp(any(DataType.class))).thenReturn(-23L);

        BackupState finalState = task.doInBackground(new BackupConfig(
            store, 0, 100, new ContactGroup(-1), BackupType.SKIP, EnumSet.of(SMS), false
            )
        );
        verify(dataTypePreferences).setMaxSyncedDate(DataType.SMS, -23);
        // verifyNoMoreInteractions: confirm no other interactions with dataTypePreferences beyond setMaxSyncedDate
        verifyNoMoreInteractions(dataTypePreferences);

        assertThat(finalState).isNotNull();
        assertThat(finalState.isFinished()).isTrue();
    }

    @Test public void shouldHandleAuthErrorAndTokenCannotBeRefreshed() throws Exception {
        mockFetch(SMS, 1);
        when(converter.convertMessages(any(Cursor.class), notNull())).thenReturn(result(SMS, 1));

        // U-026: XOAuth2FailedException replaces k-9 XOAuth2AuthenticationFailedException
        XOAuth2FailedException exception = mock(XOAuth2FailedException.class);
        when(exception.getStatus()).thenReturn(400);

        // U-026: openFolder replaces getFolder; throws the app-owned exception
        when(store.openFolder(notNull(), same(dataTypePreferences))).thenThrow(exception);

        doThrow(new TokenRefreshException("failed")).when(tokenRefresher).refreshOAuth2Token();

        task.doInBackground(config);

        verify(tokenRefresher, times(1)).refreshOAuth2Token();
        verify(service).transition(SmsSyncState.ERROR, exception);

        // make sure locks only get acquired+released once
        verify(service).acquireLocks();
        verify(service).releaseLocks();
    }

    @Test public void shouldHandleAuthErrorAndTokenCouldBeRefreshed() throws Exception {
        mockFetch(SMS, 1);
        when(converter.convertMessages(any(Cursor.class), notNull())).thenReturn(result(SMS, 1));

        // U-026: XOAuth2FailedException replaces k-9 XOAuth2AuthenticationFailedException
        XOAuth2FailedException exception = mock(XOAuth2FailedException.class);
        when(exception.getStatus()).thenReturn(400);

        // U-026: openFolder throws XOAuth2FailedException (replaces getFolder throwing k-9 exception)
        when(store.openFolder(notNull(), same(dataTypePreferences))).thenThrow(exception);
        // U-026: getMailTransport() replaces getBackupImapStore()
        when(service.getMailTransport()).thenReturn(store);

        task.doInBackground(config);

        verify(tokenRefresher).refreshOAuth2Token();

        verify(service, times(2)).transition(SmsSyncState.LOGIN, null);
        verify(service, times(2)).transition(SmsSyncState.CALC, null);
        verify(service).transition(SmsSyncState.ERROR, exception);

        // make sure locks only get acquired+released once
        verify(service).acquireLocks();
        verify(service).releaseLocks();
    }


    private ConversionResult result(DataType type, int n) {
        ConversionResult result = new ConversionResult(type);
        for (int i = 0; i<n; i++) {
            result.add(new MimeMessage(), new HashMap<String, String>());
        }
        return result;
    }

    private void mockFetch(DataType type, final int n) {
        // nullable() matches null ContactGroupIds — production code passes null when no contact group
        when(fetcher.getItemsForDataType(eq(type), nullable(ContactGroupIds.class), anyInt())).then(new Answer<Object>() {
            @Override public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return testMessages(n);
            }
        });
    }

    private Cursor testMessages(int n) {
        MatrixCursor cursor = new MatrixCursor(new String[] {"_id"} );
        for (int i = 0; i < n; i++) {
            cursor.addRow(new Object[]{
                    "12345"
            });
        }
        return cursor;
    }


    private void mockAllFetchEmpty() {
        // nullable() matches null ContactGroupIds — production code passes null when no contact group
        when(fetcher.getItemsForDataType(any(DataType.class), nullable(ContactGroupIds.class), anyInt())).thenReturn(emptyCursor());
    }
}
