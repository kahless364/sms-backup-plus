package com.zegoggles.smssync.service.state;

import android.content.res.Resources;
import com.zegoggles.smssync.mail.DataType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * U-006 characterization tests for RestoreState.
 * Pins notification labels and state transitions before SmsRestoreService refactoring.
 */
@RunWith(RobolectricTestRunner.class)
public class RestoreStateTest {
    private Resources resources;

    @Before public void before() {
        resources = RuntimeEnvironment.application.getResources();
    }

    @Test public void defaultConstructor_setsInitialState() {
        RestoreState state = new RestoreState();
        assertThat(state.state).isEqualTo(SmsSyncState.INITIAL);
        assertThat(state.currentRestoredCount).isEqualTo(0);
        assertThat(state.itemsToRestore).isEqualTo(0);
        assertThat(state.actualRestoredCount).isEqualTo(0);
        assertThat(state.duplicateCount).isEqualTo(0);
    }

    @Test public void transition_createsNewStateWithSameCounters() {
        RestoreState original = new RestoreState(
            SmsSyncState.RESTORE, 5, 10, 4, 1, DataType.SMS, null
        );
        RestoreState transitioned = original.transition(SmsSyncState.FINISHED_RESTORE, null);

        assertThat(transitioned.state).isEqualTo(SmsSyncState.FINISHED_RESTORE);
        assertThat(transitioned.currentRestoredCount).isEqualTo(5);
        assertThat(transitioned.itemsToRestore).isEqualTo(10);
        assertThat(transitioned.actualRestoredCount).isEqualTo(4);
        assertThat(transitioned.duplicateCount).isEqualTo(1);
    }

    @Test public void getNotificationLabel_restore_withDataType() {
        RestoreState state = new RestoreState(
            SmsSyncState.RESTORE, 3, 10, 2, 0, DataType.SMS, null
        );
        String label = state.getNotificationLabel(resources);
        assertThat(label).isNotEmpty();
        // Label should contain the counts (3 and 10)
        assertThat(label).contains("3");
        assertThat(label).contains("10");
    }

    @Test public void getNotificationLabel_restore_withoutDataType() {
        RestoreState state = new RestoreState(
            SmsSyncState.RESTORE, 3, 10, 2, 0, null, null
        );
        String label = state.getNotificationLabel(resources);
        assertThat(label).isNotEmpty();
    }

    @Test public void getNotificationLabel_updatingThreads() {
        RestoreState state = new RestoreState(
            SmsSyncState.UPDATING_THREADS, 0, 0, 0, 0, null, null
        );
        String label = state.getNotificationLabel(resources);
        assertThat(label).isNotEmpty();
    }

    @Test public void getNotificationLabel_unknownState_returnsEmpty() {
        RestoreState state = new RestoreState(
            SmsSyncState.INITIAL, 0, 0, 0, 0, null, null
        );
        String label = state.getNotificationLabel(resources);
        assertThat(label).isEmpty();
    }

    @Test public void toString_containsRelevantFields() {
        RestoreState state = new RestoreState(
            SmsSyncState.RESTORE, 3, 10, 2, 1, DataType.SMS, null
        );
        String str = state.toString();
        assertThat(str).contains("3");
        assertThat(str).contains("10");
    }
}
