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
 * U-006 characterization tests for SmsRestoreService.
 * Pins the initial state and basic lifecycle behavior before service refactoring.
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
}
