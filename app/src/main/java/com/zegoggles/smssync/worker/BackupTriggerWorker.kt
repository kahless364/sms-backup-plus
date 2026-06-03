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
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Worker
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.service.BackupType
import com.zegoggles.smssync.service.BackupWorker
import java.util.concurrent.TimeUnit

/**
 * Content-URI trigger worker — implements the INV-4 two-stage debounce.
 *
 * This worker is enqueued by the CONTENT_TRIGGER [androidx.work.PeriodicWorkRequest]
 * (set up by [com.zegoggles.smssync.scheduler.WorkManagerScheduler.scheduleContentTrigger]).
 * It does NOT perform a backup directly; instead it enqueues a delayed
 * [OneTimeWorkRequest] for [BackupWorker] with [initialDelay = getIncomingTimeoutSecs()].
 *
 * This preserves the two-stage debounce (INV-4):
 * - Stage 1: content-URI change fires this trigger worker
 * - Stage 2: this worker enqueues the delayed incoming backup via scheduleIncoming()
 *
 * The worker does NOT back up on the raw content change (INV-4, DES-MODERNIZATION-005
 * §Incoming-SMS trigger).
 *
 * U-015 dependency: once BackupWorker is a CoroutineWorker with full execution logic,
 * this trigger worker's follow-up enqueue will invoke the real backup. No changes to this
 * worker are required by U-015.
 */
class BackupTriggerWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val prefs = Preferences(applicationContext)
        val incomingTimeoutSecs = prefs.incomingTimeoutSecs.toLong()

        Log.d(TAG, "BackupTriggerWorker.doWork: content-URI trigger fired; " +
                "enqueueing delayed incoming backup with initialDelay=${incomingTimeoutSecs}s")

        // INV-4: two-stage debounce — enqueue delayed incoming backup; do NOT back up directly.
        val incomingWork = OneTimeWorkRequest.Builder(BackupWorker::class.java)
            .setInitialDelay(incomingTimeoutSecs, TimeUnit.SECONDS)
            .addTag(BackupType.INCOMING.name)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                BackupType.INCOMING.name,
                androidx.work.ExistingWorkPolicy.REPLACE,
                incomingWork
            )

        return Result.success()
    }

    companion object {
        private const val TAG = "SMSBackup+"
    }
}
