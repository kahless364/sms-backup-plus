/*
 * Copyright (c) 2024 SMS Backup+
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zegoggles.smssync.service

import android.content.Context
import com.zegoggles.smssync.auth.OAuth2Client
import com.zegoggles.smssync.auth.TokenRefresher
import com.zegoggles.smssync.calendar.CalendarAccessor
import com.zegoggles.smssync.contacts.ContactAccessor
import com.zegoggles.smssync.mail.CallFormatter
import com.zegoggles.smssync.mail.MessageConverter
import com.zegoggles.smssync.mail.PersonLookup
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * U-050 AR-004: Hilt-injectable factory for per-run engine collaborators.
 *
 * Replaces the hand-`new` construction of [PersonLookup], [MessageConverter],
 * [TokenRefresher], and [CalendarSyncer] inside [BackupWorker] and [RestoreWorker].
 *
 * **Why a factory rather than direct injection?**
 * - [PersonLookup] holds an LRU cache; a fresh instance per run avoids stale
 *   cross-run cache entries and matches the original per-call construction semantics.
 * - [MessageConverter] reads [AuthPreferences.userEmail] at construction time (used
 *   as the sender address in generated MIME messages). Since `userEmail` is a runtime
 *   value that can change if the user re-authenticates, the converter must be created
 *   fresh per run from current [AuthPreferences] state.
 * - [TokenRefresher] captures an [OAuth2Client] whose `clientId` comes from
 *   [AuthPreferences.oAuth2ClientId] — again a runtime value, so created per run.
 * - [CalendarSyncer] takes a `Long calendarId` and a [CalendarAccessor] (backed by
 *   [CalendarAccessor.Get.instance(resolver)]) that both require a live [ContentResolver]
 *   obtained at run time. Hilt cannot bind `Long` without a qualifier and
 *   [CalendarAccessor.Get.instance] is a legacy static factory — CalendarSyncer
 *   **must remain hand-built** and [createCalendarSyncerIfEnabled] documents this.
 * - [ContactAccessor] has a zero-arg `@Inject` constructor and carries no per-run state;
 *   it is injected directly into [BackupWorker] / [RestoreWorker] as a constructor param
 *   (not through this factory) and is a @Singleton in the Hilt graph.
 *
 * This factory is a `@Singleton` (cheap to hold); only the instances it creates are
 * per-run and short-lived.
 */
@Singleton
class WorkerEngineFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Preferences,
    private val authPreferences: AuthPreferences
) {

    /**
     * Creates a fresh [PersonLookup] backed by the application's [ContentResolver].
     * Each call returns a new instance with an empty LRU cache — correct per-run semantics.
     */
    fun createPersonLookup(): PersonLookup =
        PersonLookup(context.contentResolver)

    /**
     * Creates a [MessageConverter] from current [Preferences] and [AuthPreferences] state.
     * The [userEmail] is read from [AuthPreferences] at creation time — fresh per run.
     *
     * @param personLookup per-run [PersonLookup] instance (pass the one from [createPersonLookup])
     * @param contactAccessor injected [ContactAccessor] singleton
     */
    fun createMessageConverter(personLookup: PersonLookup, contactAccessor: ContactAccessor): MessageConverter =
        MessageConverter(
            context,
            preferences,
            authPreferences.userEmail,
            personLookup,
            contactAccessor
        )

    /**
     * Creates a [TokenRefresher] from current [AuthPreferences] state.
     * The [OAuth2Client] is constructed per run so it captures the current `clientId`.
     */
    fun createTokenRefresher(): TokenRefresher =
        TokenRefresher(
            context,
            OAuth2Client(authPreferences.oAuth2ClientId),
            authPreferences
        )

    /**
     * Creates a [CalendarSyncer] if call-log calendar sync is enabled in preferences,
     * otherwise returns null (matching the original conditional-construction pattern).
     *
     * **Why hand-built:** [CalendarSyncer] requires:
     * - `calendarId` (a `Long` from [Preferences.callLogCalendarId]) — Hilt cannot
     *   bind a raw `Long` without a `@Named` qualifier, and adding one here would
     *   couple the Hilt graph to a dynamic preference value.
     * - [CalendarAccessor.Get.instance(resolver)] — a legacy static factory that
     *   produces an Android-API-level-specific accessor; no `@Inject` constructor exists.
     * This method encapsulates and documents the hand-build obligation.
     *
     * `internal` because [CalendarSyncer] is package-private (no public modifier in Java);
     * Kotlin's public function cannot expose a package-private return type without a
     * compiler error. The callers ([BackupWorker]) are in the same package and module.
     *
     * @param personLookup per-run [PersonLookup] to share with the syncer
     */
    internal fun createCalendarSyncerIfEnabled(personLookup: PersonLookup): CalendarSyncer? {
        if (!preferences.isCallLogCalendarSyncEnabled) return null
        return CalendarSyncer(
            CalendarAccessor.Get.instance(context.contentResolver),
            preferences.callLogCalendarId.toLong(),
            personLookup,
            CallFormatter(context.resources)
        )
    }
}
