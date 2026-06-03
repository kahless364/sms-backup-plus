package com.zegoggles.smssync.service;

import com.zegoggles.smssync.service.state.SyncEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

/**
 * U-020: CancelEvent.java is deleted; tests migrated to SyncEvent.Cancel.
 * Pins the cancel origin semantics (USER vs SYSTEM) — preserving U-006 characterization.
 */
@RunWith(RobolectricTestRunner.class)
public class CancelEventTest {

    @Test public void defaultConstructor_isUserOrigin_doesNotInterruptIfRunning() {
        // U-020: CancelEvent() → new SyncEvent.Cancel(Origin.USER)
        SyncEvent.Cancel event = new SyncEvent.Cancel(SyncEvent.Cancel.Origin.USER);
        // User-initiated cancels should not forcibly interrupt running tasks
        assertThat(event.mayInterruptIfRunning()).isFalse();
    }

    @Test public void toString_containsOrigin() {
        // U-020: CancelEvent toString → SyncEvent.Cancel.toString
        SyncEvent.Cancel event = new SyncEvent.Cancel(SyncEvent.Cancel.Origin.USER);
        assertThat(event.toString()).contains("USER");
    }
}
