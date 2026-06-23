package com.zegoggles.smssync.service

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.zegoggles.smssync.mail.PersonLookup
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.Preferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for [WorkerEngineFactory] (U-050 AR-004).
 *
 * Verifies that the factory creates collaborators correctly without manual `new` calls
 * inside the workers. Tests focus on:
 * - Each factory method returns a non-null instance of the expected type.
 * - Per-run semantics: [createPersonLookup] returns distinct instances across calls
 *   (fresh LRU cache per run — prevents stale cross-run cache entries).
 * - Conditional: [createCalendarSyncerIfEnabled] returns null when calendar sync disabled.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class WorkerEngineFactoryTest {

    private lateinit var context: Context
    private lateinit var preferences: Preferences
    private lateinit var authPreferences: AuthPreferences
    private lateinit var factory: WorkerEngineFactory

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
        preferences = Preferences(context)
        authPreferences = AuthPreferences(context)
        factory = WorkerEngineFactory(context, preferences, authPreferences)
    }

    // -----------------------------------------------------------------------
    // AR-004: createPersonLookup
    // -----------------------------------------------------------------------

    /**
     * AR-004: [createPersonLookup] returns a non-null [PersonLookup] backed by the
     * application ContentResolver.
     */
    @Test
    fun createPersonLookup_returnsNonNull() {
        val lookup = factory.createPersonLookup()
        assertThat(lookup).isNotNull()
        assertThat(lookup).isInstanceOf(PersonLookup::class.java)
    }

    /**
     * AR-004: Per-run cache semantics — each call to [createPersonLookup] returns a
     * distinct instance. This ensures each run starts with an empty LRU cache and
     * prevents stale contact entries from bleeding across runs.
     */
    @Test
    fun createPersonLookup_distinctInstancePerCall() {
        val lookup1 = factory.createPersonLookup()
        val lookup2 = factory.createPersonLookup()
        assertThat(lookup1).isNotSameInstanceAs(lookup2)
    }

    // -----------------------------------------------------------------------
    // AR-004: createTokenRefresher
    // -----------------------------------------------------------------------

    /**
     * AR-004: [createTokenRefresher] returns a non-null [TokenRefresher] built from
     * injected [AuthPreferences] state.
     */
    @Test
    fun createTokenRefresher_returnsNonNull() {
        val refresher = factory.createTokenRefresher()
        assertThat(refresher).isNotNull()
        assertThat(refresher).isInstanceOf(com.zegoggles.smssync.auth.TokenRefresher::class.java)
    }

    // -----------------------------------------------------------------------
    // AR-004: createCalendarSyncerIfEnabled — null when disabled
    // -----------------------------------------------------------------------

    /**
     * AR-004: [createCalendarSyncerIfEnabled] returns null when call-log calendar sync is
     * disabled (the default in a fresh test environment). This matches the original
     * conditional construction: `if (preferences.isCallLogCalendarSyncEnabled) ... else null`.
     */
    @Test
    fun createCalendarSyncerIfEnabled_returnsNull_whenDisabled() {
        // By default in a fresh test environment, isCallLogCalendarSyncEnabled is false.
        val personLookup = factory.createPersonLookup()
        val syncer = factory.createCalendarSyncerIfEnabled(personLookup)
        assertThat(syncer).isNull()
    }
}
