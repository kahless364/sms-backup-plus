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
package com.zegoggles.smssync.worker

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Stub backup worker — executes no backup logic in U-014.
 *
 * This is a placeholder so that [com.zegoggles.smssync.scheduler.WorkManagerScheduler]
 * can reference a concrete worker class for REGULAR / INCOMING / BROADCAST_INTENT jobs
 * without a compile error. The full [androidx.work.CoroutineWorker] rewrite is U-015.
 *
 * U-014 scope: WorkRequest enqueue + constraints + backoff must be REAL and test-verified.
 * The worker body is a stub that returns [Result.success] immediately.
 *
 * Split-responsibility note (U-014 Technical Notes):
 * The 300s backoff cap mandated by INV-3 (newRetryStrategy(EXPONENTIAL, 30, 300)) is
 * WorkManager-internal; WorkManager's own MAX_BACKOFF_MILLIS is ~5 hours, which is larger
 * than 300s. The 300s ceiling MUST be re-imposed here in U-015 by checking the run attempt
 * count before returning Result.retry() and returning Result.failure() once the effective
 * delay would exceed 300s. This responsibility is explicitly deferred to U-015.
 *
 * Worker factory dependency note:
 * WorkManager custom initialization (Configuration.Provider / HiltWorkerFactory) is owned
 * by U-024. Until U-024 lands, WorkManager uses its default initializer; this worker is
 * constructed via the default [WorkerFactory]. Do NOT remove the default initializer from
 * AndroidManifest.xml before U-024 is merged.
 */
class BackupWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        Log.d(TAG, "BackupWorker.doWork: stub — full CoroutineWorker rewrite is U-015")
        return Result.success()
    }

    companion object {
        private const val TAG = "SMSBackup+"
    }
}
