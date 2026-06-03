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
 * Fault-injection seam for [RestoreWorker] (U-016, IC-4).
 *
 * Called inside the restore loop immediately after a confirmed provider insert
 * (after [RestoreCheckpointStore.write] and before the loop index advances).
 *
 * In production: [NoOp] is used — no behaviour change.
 * In tests: [CrashAfterK] throws after exactly K successful inserts, allowing
 * fault-injection tests to verify checkpoint-resume behaviour (AC-4).
 *
 * This interface carries no Android types; it is test-only-visible in practice
 * (per IC-4, supplied via [RestoreWorker.TestableRestoreWorkerFactory]).
 */
interface RestoreInsertInterceptor {

    /**
     * Called after a successful insert at [insertIndex].
     *
     * @throws SimulatedCrashException if this interceptor is configured to crash after K inserts.
     */
    @Throws(SimulatedCrashException::class)
    suspend fun afterInsert(insertIndex: Int)

    /** Production no-op implementation. */
    object NoOp : RestoreInsertInterceptor {
        override suspend fun afterInsert(insertIndex: Int) {
            // No-op in production.
        }
    }

    /**
     * Test implementation that throws [SimulatedCrashException] after exactly [crashAfterK]
     * successful inserts (1-indexed: first crash occurs on the [crashAfterK]-th afterInsert call).
     *
     * This simulates a process kill in the crash window after the provider insert commits
     * but before the loop index advances — the worst-case scenario for duplicate-row production.
     *
     * Note: this throws AFTER [RestoreCheckpointStore.write] has been called for insert K-1
     * (the checkpoint ordering invariant is: write checkpoint, then call interceptor).
     * So the checkpoint at crash time reflects the last write BEFORE the throw.
     *
     * @param crashAfterK the number of successful inserts before the simulated crash.
     *                    Must be >= 1.
     */
    class CrashAfterK(private val crashAfterK: Int) : RestoreInsertInterceptor {
        private var insertCount = 0

        override suspend fun afterInsert(insertIndex: Int) {
            insertCount++
            if (insertCount >= crashAfterK) {
                throw SimulatedCrashException(
                    "Simulated process kill after $insertCount inserts (crashAfterK=$crashAfterK, insertIndex=$insertIndex)"
                )
            }
        }
    }

    /**
     * Thrown by [CrashAfterK] to simulate a process kill / worker interruption mid-restore.
     * Caught by the test harness to simulate re-execution; must NOT be caught by [RestoreWorker]
     * production code (it will bubble up and cause a Result.failure, which is the correct
     * behaviour — the WorkManager will re-execute the worker after the simulated death).
     */
    class SimulatedCrashException(message: String) : RuntimeException(message)
}
