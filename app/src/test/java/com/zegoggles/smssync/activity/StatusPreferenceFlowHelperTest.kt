package com.zegoggles.smssync.activity

import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.service.state.FlowSyncStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * U-021: Unit tests for the StatusPreferenceFlowHelper scope-cancellation contract.
 *
 * AC-5 / AC-9: Verifies that:
 *   - startCollection() returns an active CoroutineScope.
 *   - cancelScope() cancels that scope so it is no longer active after onDetached.
 *   - The scope contract (created in onBindViewHolder, cancelled in onDetached) means
 *     no coroutine leaks survive beyond the preference's attached lifetime.
 *
 * The test for StatusPreference.onDetached() cancelling its scope is validated
 * structurally: StatusPreference.onDetached() calls
 *   StatusPreferenceFlowHelper.cancelScope(collectionScope)
 * These unit tests confirm that cancelScope() makes any given active scope inactive,
 * which is the core invariant that prevents the scope leak the story guards against.
 */
@RunWith(RobolectricTestRunner::class)
class StatusPreferenceFlowHelperTest {

    // -----------------------------------------------------------------------
    // cancelScope makes a scope inactive — core AC-5/AC-9 contract
    // -----------------------------------------------------------------------

    @Test
    fun `cancelScope cancels an active CoroutineScope`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        assertThat(scope.isActive).isTrue()

        StatusPreferenceFlowHelper.cancelScope(scope)

        assertThat(scope.isActive).isFalse()
    }

    @Test
    fun `cancelScope is idempotent on an already-cancelled scope`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        StatusPreferenceFlowHelper.cancelScope(scope)
        assertThat(scope.isActive).isFalse()

        // Second call must not throw.
        StatusPreferenceFlowHelper.cancelScope(scope)
        assertThat(scope.isActive).isFalse()
    }

    // -----------------------------------------------------------------------
    // startCollection returns an active scope of the correct type
    // -----------------------------------------------------------------------

    @Test
    fun `startCollection returns an active CoroutineScope`() {
        // Build a StatusPreference in a Robolectric context (same pattern as StatusPreferenceTest).
        val preference = StatusPreference(RuntimeEnvironment.application)
        val repository = FlowSyncStateRepository()

        val scope = StatusPreferenceFlowHelper.startCollection(repository, preference)

        assertThat(scope).isNotNull()
        assertThat(scope.isActive).isTrue()

        // Clean up to prevent test-process scope leaks.
        StatusPreferenceFlowHelper.cancelScope(scope)
    }

    @Test
    fun `scope returned by startCollection is cancelled by cancelScope`() {
        val preference = StatusPreference(RuntimeEnvironment.application)
        val repository = FlowSyncStateRepository()

        val scope = StatusPreferenceFlowHelper.startCollection(repository, preference)
        assertThat(scope.isActive).isTrue()

        // Simulates StatusPreference.onDetached() cancelling the scope.
        StatusPreferenceFlowHelper.cancelScope(scope)

        assertThat(scope.isActive).isFalse()
    }
}
