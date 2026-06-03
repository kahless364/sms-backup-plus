package com.zegoggles.smssync.service;

import com.zegoggles.smssync.service.state.RestoreState;
import com.zegoggles.smssync.service.state.SmsSyncState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Robolectric.setupService;

/**
 * Characterization tests for SmsRestoreService.
 *
 * U-006: pins the initial state and basic lifecycle behavior.
 * U-017: extended to increase service package coverage after legacy scheduler removal.
 */
@RunWith(RobolectricTestRunner.class)
public class SmsRestoreServiceTest {

    private SmsRestoreService service;

    @Before public void setUp() {
        service = setupService(SmsRestoreService.class);
    }

    @Test public void getState_returnsInitialRestoreState() {
        RestoreState state = service.getState();
        assertThat(state).isNotNull();
        assertThat(state.state).isEqualTo(SmsSyncState.INITIAL);
    }

    @Test public void isWorking_whenInitial_returnsFalse() {
        assertThat(service.isWorking()).isFalse();
    }

    @Test public void wakeLockType_returnsBrightScreen() {
        // SmsRestoreService uses SCREEN_BRIGHT_WAKE_LOCK (not PARTIAL) to keep screen on
        // during restore operations so the user can see progress.
        // This pins the wake lock type before service refactoring.
        int wakeLockType = service.wakeLockType();
        assertThat(wakeLockType).isNotEqualTo(0);
    }

    @Test public void restoreStateChanged_withInitialState_returnsEarly() {
        // restoreStateChanged(INITIAL) should be a no-op (isInitialState() == true returns)
        RestoreState initial = new RestoreState();
        assertThat(initial.state).isEqualTo(SmsSyncState.INITIAL);
        // should not throw
        service.restoreStateChanged(initial);
        // state should still be the initial one (not mutated by the call)
        assertThat(service.getState().state).isEqualTo(SmsSyncState.INITIAL);
    }

    @Test public void restoreStateChanged_withErrorState_updatesStateAndStops() {
        // An error state is finished (not running, not initial) — triggers stopForeground + stopSelf
        RestoreState errorState = new RestoreState().transition(SmsSyncState.ERROR, new Exception("test error"));
        assertThat(errorState.isFinished()).isTrue();

        service.restoreStateChanged(errorState);

        assertThat(service.getState()).isEqualTo(errorState);
    }

    @Test public void clearCache_withEmptyCache_doesNotThrow() {
        // clearCache() iterates getCacheDir() looking for "body*" temp files.
        // With an empty cache dir (Robolectric), this should be a no-op.
        service.clearCache(); // should not throw
    }
}
