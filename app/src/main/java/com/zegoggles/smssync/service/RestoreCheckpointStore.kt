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

/**
 * Port interface for the durable restore checkpoint store (U-016).
 *
 * Stores the index of the last successfully-restored item so that the [RestoreWorker]
 * can resume from `lastIndex + 1` after a process kill and WorkManager re-execution.
 *
 * Contract (CNTR-MODERNIZATION-004 §RestoreSchedulerConfig, §Validation Rules rule 6):
 * - [write] MUST be called AFTER a confirmed provider insert and BEFORE advancing the
 *   loop index, so that re-execution skips already-committed items.
 * - [clear] MUST be called when the worker reaches SUCCEEDED terminal state; it is NOT
 *   called on CANCELLED so that a subsequent re-schedule can resume from the same position.
 * - [read] returns 0 (or the sentinel "not started" value) when no checkpoint exists.
 *
 * This interface carries no Android types in its signature (no SharedPreferences, no DataStore,
 * no WorkManager types) — per IC-1 / CNTR-MODERNIZATION-004 §IC-1.
 * The production adapter ([SharedPreferencesCheckpointStore]) implements the persistence
 * mechanism; the core restore loop depends only on this port.
 */
interface RestoreCheckpointStore {
    /**
     * Returns the last successfully-restored item index for the given [workName], or
     * [NO_CHECKPOINT] (0) if no checkpoint has been written or it was cleared.
     *
     * Called once at the beginning of [RestoreWorker.doWork] before the restore loop.
     */
    suspend fun read(workName: String): Int

    /**
     * Persists [index] as the last successfully-restored item for [workName].
     *
     * MUST be called after a confirmed provider insert for item at [index] and
     * BEFORE advancing the loop variable to [index] + 1.
     * This is a suspending call — awaited, not fire-and-forget.
     */
    suspend fun write(workName: String, index: Int)

    /**
     * Clears the checkpoint for [workName], resetting it to [NO_CHECKPOINT].
     *
     * Called when the worker completes all items and reaches SUCCEEDED state.
     * NOT called on CANCELLED (per AC-7).
     */
    suspend fun clear(workName: String)

    companion object {
        /** Sentinel value returned by [read] when no checkpoint exists. */
        const val NO_CHECKPOINT = 0
    }
}
