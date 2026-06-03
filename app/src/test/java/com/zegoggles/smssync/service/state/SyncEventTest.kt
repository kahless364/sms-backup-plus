package com.zegoggles.smssync.service.state

import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.service.state.SyncEvent.Cancel
import com.zegoggles.smssync.service.state.SyncEvent.Cancel.Origin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * U-019: Unit tests for SyncEvent.Cancel Origin semantics (AC-8).
 *
 * Guards the load-bearing USER/SYSTEM distinction that drives interrupt behavior
 * in the engine (CNTR-MODERNIZATION-006 §Validation Rules rule 7).
 */
@RunWith(RobolectricTestRunner::class)
class SyncEventTest {

    // --- AC-8: Cancel Origin semantics ---

    @Test
    fun `Cancel with USER origin does not interrupt if running`() {
        val event = Cancel(Origin.USER)
        assertThat(event.mayInterruptIfRunning()).isFalse()
    }

    @Test
    fun `Cancel with SYSTEM origin does interrupt if running`() {
        val event = Cancel(Origin.SYSTEM)
        assertThat(event.mayInterruptIfRunning()).isTrue()
    }

    @Test
    fun `Cancel default constructor has USER origin and does not interrupt`() {
        val event = Cancel()
        assertThat(event.origin).isEqualTo(Origin.USER)
        assertThat(event.mayInterruptIfRunning()).isFalse()
    }

    @Test
    fun `Cancel USER and SYSTEM are distinguishable`() {
        val user = Cancel(Origin.USER)
        val system = Cancel(Origin.SYSTEM)
        assertThat(user).isNotEqualTo(system)
    }

    // --- Additional SyncEvent variant smoke tests ---

    @Test
    fun `AccountAdded is a SyncEvent`() {
        val event: SyncEvent = SyncEvent.AccountAdded
        assertThat(event).isInstanceOf(SyncEvent::class.java)
    }

    @Test
    fun `AccountRemoved is a SyncEvent`() {
        val event: SyncEvent = SyncEvent.AccountRemoved
        assertThat(event).isInstanceOf(SyncEvent::class.java)
    }

    @Test
    fun `AccountConnectionChanged is a SyncEvent`() {
        val event: SyncEvent = SyncEvent.AccountConnectionChanged
        assertThat(event).isInstanceOf(SyncEvent::class.java)
    }

    @Test
    fun `AutoBackupSettingsChanged is a SyncEvent`() {
        val event: SyncEvent = SyncEvent.AutoBackupSettingsChanged
        assertThat(event).isInstanceOf(SyncEvent::class.java)
    }

    @Test
    fun `FallbackAuth is a SyncEvent`() {
        val event: SyncEvent = SyncEvent.FallbackAuth
        assertThat(event).isInstanceOf(SyncEvent::class.java)
    }

    @Test
    fun `SettingsReset is a SyncEvent`() {
        val event: SyncEvent = SyncEvent.SettingsReset
        assertThat(event).isInstanceOf(SyncEvent::class.java)
    }

    @Test
    fun `ThemeChanged is a SyncEvent`() {
        val event: SyncEvent = SyncEvent.ThemeChanged
        assertThat(event).isInstanceOf(SyncEvent::class.java)
    }
}
