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
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import androidx.work.Worker
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.scheduler.BackupScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Content-URI trigger worker — implements the INV-4 two-stage debounce.
 *
 * This worker is enqueued by the CONTENT_TRIGGER [androidx.work.PeriodicWorkRequest]
 * (set up by [com.zegoggles.smssync.scheduler.WorkManagerScheduler.scheduleContentTrigger]).
 * It does NOT perform a backup directly; instead it calls [BackupScheduler.scheduleIncoming],
 * the one builder of the delayed incoming backup (unique work INCOMING, REPLACE). This path
 * therefore gets every rule of that call: the encryption-degraded gate (U-054), the
 * auto-backup and delay rule, the network constraint per Wi-Fi only (INV-2), the exponential
 * 30 s backoff (INV-3) and the delay of getIncomingTimeoutSecs() (INV-4).
 *
 * This preserves the two-stage debounce (INV-4):
 * - Stage 1: content-URI change fires this trigger worker
 * - Stage 2: this worker enqueues the delayed incoming backup via scheduleIncoming()
 *
 * The worker does NOT back up on the raw content change (INV-4, DES-MODERNIZATION-005
 * §Incoming-SMS trigger). It returns success whether or not the scheduler enqueued work:
 * a declined enqueue is a rule working, and the scheduler logs the reason.
 *
 * U-069 (BUG-023): created by HiltWorkerFactory through the assisted factory generated for
 * [HiltWorker], like the backup and restore workers. The class name and package must not
 * change: CONTENT_TRIGGER work persisted on a device stores this class name.
 */
@HiltWorker
class BackupTriggerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scheduler: BackupScheduler,
    private val preferences: Preferences
) : Worker(context, params) {

    override fun doWork(): Result {
        // Read for the log line only; scheduleIncoming() reads the preferences itself and decides.
        val incomingTimeoutSecs = preferences.incomingTimeoutSecs

        Log.d(TAG, "BackupTriggerWorker.doWork: content-URI trigger fired; " +
                "enqueueing delayed incoming backup with initialDelay=${incomingTimeoutSecs}s")

        // INV-4 stage 2: the one builder of INCOMING work applies INV-1 to INV-3, the U-054 gate
        // and the auto-backup and delay rule. A null result (declined) is not a failure of this run.
        scheduler.scheduleIncoming()

        return Result.success()
    }

    companion object {
        private const val TAG = "SMSBackup+"
    }
}
