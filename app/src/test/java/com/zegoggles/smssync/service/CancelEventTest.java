package com.zegoggles.smssync.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.google.common.truth.Truth.assertThat;

/**
 * U-006 characterization tests for CancelEvent.
 * Pins the cancel origin semantics (USER vs SYSTEM) before service refactoring.
 */
@RunWith(RobolectricTestRunner.class)
public class CancelEventTest {

    @Test public void defaultConstructor_isUserOrigin_doesNotInterruptIfRunning() {
        CancelEvent event = new CancelEvent();
        // User-initiated cancels should not forcibly interrupt running tasks
        assertThat(event.mayInterruptIfRunning()).isFalse();
    }

    @Test public void toString_containsOrigin() {
        CancelEvent event = new CancelEvent();
        assertThat(event.toString()).contains("USER");
    }
}
