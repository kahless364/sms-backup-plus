package com.zegoggles.smssync.service.state;

import android.content.res.Resources;
import com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException;
import org.mockito.Mockito;
import com.zegoggles.smssync.mail.DataType;
import com.zegoggles.smssync.service.BackupType;
import com.zegoggles.smssync.service.exception.NoConnectionException;
import com.zegoggles.smssync.service.exception.MissingPermissionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.HashSet;

import static com.google.common.truth.Truth.assertThat;

/**
 * U-006 characterization tests for BackupState and State base class.
 * Pins isFinished, isRunning, isPermissionException, isCanceled, getMissingPermissions,
 * isConnectivityError, isAuthException, and BackupState notification label behaviors.
 */
@RunWith(RobolectricTestRunner.class)
public class BackupStateCoverageTest {
    private Resources resources;

    @Before public void before() {
        resources = RuntimeEnvironment.application.getResources();
    }

    @Test public void backupState_defaultConstructor_isInitial() {
        BackupState state = new BackupState();
        assertThat(state.state).isEqualTo(SmsSyncState.INITIAL);
        assertThat(state.isInitialState()).isTrue();
        assertThat(state.isRunning()).isFalse();
        assertThat(state.isFinished()).isFalse();
    }

    @Test public void backupState_transition_preservesCounters() {
        BackupState original = new BackupState(
            SmsSyncState.BACKUP, 5, 10, BackupType.REGULAR, DataType.SMS, null
        );
        BackupState transitioned = original.transition(SmsSyncState.FINISHED_BACKUP, null);

        assertThat(transitioned.state).isEqualTo(SmsSyncState.FINISHED_BACKUP);
        assertThat(transitioned.currentSyncedItems).isEqualTo(5);
        assertThat(transitioned.itemsToSync).isEqualTo(10);
        assertThat(transitioned.backupType).isEqualTo(BackupType.REGULAR);
    }

    @Test public void backupState_getNotificationLabel_backup_withDataType() {
        BackupState state = new BackupState(
            SmsSyncState.BACKUP, 3, 10, BackupType.REGULAR, DataType.SMS, null
        );
        String label = state.getNotificationLabel(resources);
        assertThat(label).isNotNull();
        assertThat(label).contains("3");
        assertThat(label).contains("10");
    }

    @Test public void backupState_getNotificationLabel_backup_withoutDataType() {
        BackupState state = new BackupState(
            SmsSyncState.BACKUP, 0, 0, BackupType.REGULAR, null, null
        );
        String label = state.getNotificationLabel(resources);
        assertThat(label).isNotNull();
    }

    @Test public void backupState_getNotificationLabel_nonBackupState_returnsEmpty() {
        BackupState state = new BackupState(
            SmsSyncState.FINISHED_BACKUP, 0, 0, BackupType.REGULAR, null, null
        );
        String label = state.getNotificationLabel(resources);
        assertThat(label).isEmpty();
    }

    @Test public void backupState_toString_containsFields() {
        BackupState state = new BackupState(
            SmsSyncState.BACKUP, 3, 10, BackupType.REGULAR, DataType.SMS, null
        );
        String str = state.toString();
        assertThat(str).contains("3");
        assertThat(str).contains("10");
    }

    @Test public void state_isPermissionException_true() {
        MissingPermissionException mpe = new MissingPermissionException(
            new HashSet<>(Arrays.asList("android.permission.READ_SMS"))
        );
        BackupState state = new BackupState(
            SmsSyncState.ERROR, 0, 0, BackupType.REGULAR, null, mpe
        );
        assertThat(state.isPermissionException()).isTrue();
    }

    @Test public void state_isPermissionException_false_forOtherExceptions() {
        BackupState state = new BackupState(
            SmsSyncState.ERROR, 0, 0, BackupType.REGULAR, null, new RuntimeException()
        );
        assertThat(state.isPermissionException()).isFalse();
    }

    @Test public void state_getMissingPermissions_returnsPermissionArray() {
        HashSet<String> permissions = new HashSet<>(Arrays.asList(
            "android.permission.READ_SMS",
            "android.permission.READ_CONTACTS"
        ));
        MissingPermissionException mpe = new MissingPermissionException(permissions);
        BackupState state = new BackupState(
            SmsSyncState.ERROR, 0, 0, BackupType.REGULAR, null, mpe
        );
        String[] result = state.getMissingPermissions();
        assertThat(result).hasLength(2);
        assertThat(Arrays.asList(result)).containsExactlyElementsIn(permissions);
    }

    @Test public void state_getMissingPermissions_returnsEmptyWhenNoPermissionException() {
        BackupState state = new BackupState(
            SmsSyncState.ERROR, 0, 0, BackupType.REGULAR, null, new RuntimeException()
        );
        assertThat(state.getMissingPermissions()).hasLength(0);
    }

    @Test public void state_isConnectivityError_true() {
        // NoConnectionException is a concrete subclass of ConnectivityException
        NoConnectionException nce = new NoConnectionException();
        BackupState state = new BackupState(
            SmsSyncState.ERROR, 0, 0, BackupType.REGULAR, null, nce
        );
        assertThat(state.isConnectivityError()).isTrue();
    }

    @Test public void state_isConnectivityError_false_forOtherExceptions() {
        BackupState state = new BackupState(
            SmsSyncState.ERROR, 0, 0, BackupType.REGULAR, null, new RuntimeException()
        );
        assertThat(state.isConnectivityError()).isFalse();
    }

    @Test public void state_isCanceled_forCanceledBackup() {
        BackupState state = new BackupState(
            SmsSyncState.CANCELED_BACKUP, 0, 0, BackupType.REGULAR, null, null
        );
        assertThat(state.isCanceled()).isTrue();
        assertThat(state.isFinished()).isTrue();
    }

    @Test public void state_isCanceled_forCanceledRestore() {
        BackupState state = new BackupState(
            SmsSyncState.CANCELED_RESTORE, 0, 0, BackupType.REGULAR, null, null
        );
        assertThat(state.isCanceled()).isTrue();
    }

    @Test public void state_isRunning_forBackupState() {
        BackupState state = new BackupState(
            SmsSyncState.BACKUP, 0, 0, BackupType.REGULAR, null, null
        );
        assertThat(state.isRunning()).isTrue();
        assertThat(state.isFinished()).isFalse();
    }

    @Test public void state_isFinished_forErrorState() {
        BackupState state = new BackupState(
            SmsSyncState.ERROR, 0, 0, BackupType.REGULAR, null, new RuntimeException()
        );
        assertThat(state.isFinished()).isTrue();
        assertThat(state.isRunning()).isFalse();
    }

    @Test public void state_isAuthException_forXOAuth2Failure() {
        XOAuth2AuthenticationFailedException authEx = Mockito.mock(XOAuth2AuthenticationFailedException.class);
        BackupState state = new BackupState(
            SmsSyncState.ERROR, 0, 0, BackupType.REGULAR, null, authEx
        );
        assertThat(state.isAuthException()).isTrue();
    }

    @Test public void state_getErrorMessage_nullExceptionReturnsNull() {
        BackupState state = new BackupState(
            SmsSyncState.FINISHED_BACKUP, 0, 0, BackupType.REGULAR, null, null
        );
        assertThat(state.getErrorMessage(resources)).isNull();
    }

    @Test public void state_getDetailedErrorMessage_nullExceptionReturnsNull() {
        BackupState state = new BackupState(
            SmsSyncState.FINISHED_BACKUP, 0, 0, BackupType.REGULAR, null, null
        );
        assertThat(state.getDetailedErrorMessage(resources)).isNull();
    }
}
