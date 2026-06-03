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
package com.zegoggles.smssync.scheduler

import android.util.Log
import com.zegoggles.smssync.service.BackupType

/**
 * Debug-only [BackupScheduler] that delegates to both [LegacyScheduler] and
 * [WorkManagerScheduler] in sequence, logging any divergences.
 *
 * This is the pre-cutover confidence gate (AC-7 / DES-MODERNIZATION-005 §Branch-by-abstraction).
 * It proves that [WorkManagerScheduler] produces behaviorally-equivalent scheduling before
 * the production binding flip (U-017).
 *
 * **ABSENT FROM RELEASE BUILDS**: This class is in the `debug` source set and is therefore
 * compiled only into debug builds. The `LegacyScheduler` class (in `main`) is present in
 * all builds but is only bound as the [BackupScheduler] production implementation; the
 * composite binding is debug-only.
 *
 * **Divergence logging**: each operation logs the [LegacyScheduler] and [WorkManagerScheduler]
 * results at DEBUG level. Expected divergences: none — the adapter is a mechanical translation
 * of the same invariants. Any unexpected divergence logged during QA testing of the debug build
 * is a blocker for the binding flip (U-017).
 *
 * **Binding**: to activate this scheduler, change [com.zegoggles.smssync.App.onCreate]'s
 * scheduler assignment to use [CompositeScheduler] in debug builds (guarded by BuildConfig.DEBUG).
 * The production binding flip (WorkManagerScheduler only, no composite) is U-017.
 */
class CompositeScheduler(
    private val legacy: LegacyScheduler,
    private val workManager: WorkManagerScheduler
) : BackupScheduler {

    override fun scheduleIncoming(): ScheduledJob? {
        val legacyResult = legacy.scheduleIncoming()
        val wmResult = workManager.scheduleIncoming()
        logDivergence("scheduleIncoming", legacyResult?.tag, wmResult?.tag)
        return legacyResult // Legacy is authoritative during parallel-run
    }

    override fun scheduleRegular(): ScheduledJob? {
        val legacyResult = legacy.scheduleRegular()
        val wmResult = workManager.scheduleRegular()
        logDivergence("scheduleRegular", legacyResult?.tag, wmResult?.tag)
        return legacyResult
    }

    override fun scheduleContentTrigger(): ScheduledJob? {
        val legacyResult = legacy.scheduleContentTrigger()
        val wmResult = workManager.scheduleContentTrigger()
        logDivergence("scheduleContentTrigger", legacyResult?.tag, wmResult?.tag)
        return legacyResult
    }

    override fun scheduleBootup(): ScheduledJob? {
        val legacyResult = legacy.scheduleBootup()
        val wmResult = workManager.scheduleBootup()
        logDivergence("scheduleBootup", legacyResult?.tag, wmResult?.tag)
        return legacyResult
    }

    override fun scheduleImmediate(): ScheduledJob? {
        val legacyResult = legacy.scheduleImmediate()
        val wmResult = workManager.scheduleImmediate()
        logDivergence("scheduleImmediate", legacyResult?.tag, wmResult?.tag)
        return legacyResult
    }

    override fun scheduleRestore(config: RestoreSchedulerConfig): ScheduledJob? {
        val legacyResult = legacy.scheduleRestore(config)
        val wmResult = workManager.scheduleRestore(config)
        logDivergence("scheduleRestore[${config.uniqueName}]", legacyResult?.tag, wmResult?.tag)
        return legacyResult
    }

    override fun cancelAll() {
        legacy.cancelAll()
        workManager.cancelAll()
        Log.d(TAG, "CompositeScheduler.cancelAll: delegated to both adapters")
    }

    override fun cancelRegular() {
        legacy.cancelRegular()
        workManager.cancelRegular()
        Log.d(TAG, "CompositeScheduler.cancelRegular: delegated to both adapters")
    }

    override fun cancel(jobKind: BackupType) {
        legacy.cancel(jobKind)
        workManager.cancel(jobKind)
        Log.d(TAG, "CompositeScheduler.cancel($jobKind): delegated to both adapters")
    }

    override fun observe(jobKind: BackupType): SchedulerObservable<SchedulerState> {
        // Return WorkManagerScheduler's observable — it has richer state (Enqueued vs Unknown).
        return workManager.observe(jobKind)
    }

    // -----------------------------------------------------------------------
    // Divergence logging
    // -----------------------------------------------------------------------

    /**
     * Logs at DEBUG level when the legacy and WorkManager results differ.
     *
     * Expected divergences: none (both adapters should produce the same unique-work name).
     * An unexpected divergence here is a blocker for the production binding flip (U-017).
     *
     * @param operation  the port operation name, for log context
     * @param legacyTag  the unique-work tag from LegacyScheduler (null if not scheduled)
     * @param wmTag      the unique-work tag from WorkManagerScheduler (null if not scheduled)
     */
    private fun logDivergence(operation: String, legacyTag: String?, wmTag: String?) {
        val match = legacyTag == wmTag
        if (match) {
            Log.d(TAG, "CompositeScheduler.$operation: MATCH — legacy=$legacyTag, wm=$wmTag")
        } else {
            // Expected divergences: none. Any divergence here is a blocker for U-017 binding flip.
            Log.d(TAG, "CompositeScheduler.$operation: DIVERGENCE — legacy=$legacyTag, wm=$wmTag " +
                    "[BLOCKER if unexpected — investigate before U-017 cutover]")
        }
    }

    companion object {
        private const val TAG = "SMSBackup+"
    }
}
