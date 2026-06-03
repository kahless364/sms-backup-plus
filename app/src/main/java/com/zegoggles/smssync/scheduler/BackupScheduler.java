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
package com.zegoggles.smssync.scheduler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.zegoggles.smssync.service.BackupType;

/**
 * Hexagonal port — the single scheduling boundary between caller code and the
 * background-execution mechanism in SMS Backup+.
 * <p>
 * <strong>No Android scheduler types appear in any method signature.</strong>
 * No {@code com.firebase.*}, {@code androidx.work.*}, {@code android.app.AlarmManager},
 * {@code Constraints}, {@code WorkRequest}, or {@code BackoffPolicy} types appear
 * here. Android-specific construction lives entirely in the adapters
 * ({@code LegacyScheduler}, and the future {@code WorkManagerScheduler}).
 * <p>
 * All seven call sites (BackupBroadcastReceiver, BootReceiver, SmsBroadcastReceiver,
 * SmsJobService, SmsBackupService, App ×2) are routed through this port.
 * The bound implementation is {@link LegacyScheduler} for this story (U-013);
 * U-014 introduces {@code WorkManagerScheduler} and U-017 flips the binding.
 * <p>
 * <strong>Contract: CNTR-MODERNIZATION-004</strong> — all nine operations below,
 * the {@link ScheduledJob} return type, and the {@link SchedulerObservable} observable
 * state surface are binding clauses of that contract.
 */
public interface BackupScheduler {

    // -----------------------------------------------------------------------
    // Scheduling operations
    // -----------------------------------------------------------------------

    /**
     * Schedules a delayed one-off backup triggered by an incoming SMS.
     * <p>
     * Delay = {@code Preferences.getIncomingTimeoutSecs()}; network constraint per
     * {@code isWifiOnly()}; REPLACE semantics. Returns {@code null} if auto-backup is
     * disabled or the timeout is ≤ 0.
     *
     * @return the enqueued job, or {@code null} if nothing was scheduled
     */
    @Nullable
    ScheduledJob scheduleIncoming();

    /**
     * Schedules a recurring automatic backup.
     * <p>
     * Network constraint per {@code isWifiOnly()}; exponential backoff (30 s initial,
     * 300 s cap); REPLACE/UPDATE on a stable unique name. Returns {@code null} if
     * auto-backup is disabled.
     *
     * @return the enqueued job, or {@code null} if not scheduled
     */
    @Nullable
    ScheduledJob scheduleRegular();

    /**
     * Schedules a recurring content-URI observer over the SMS provider (and the
     * call-log URI when {@code isCallLogBackupAfterCallEnabled()}).
     * <p>
     * API-level branching (content-URI triggers are only available on API 24+) lives
     * inside the adapter, not in this signature.
     *
     * @return the enqueued job, or {@code null} if the adapter does not support it
     */
    @Nullable
    ScheduledJob scheduleContentTrigger();

    /**
     * Schedules (or cancels) jobs on device boot.
     * <p>
     * If {@code !isAutoBackupEnabled()}: calls {@link #cancelAll()} and returns
     * {@code null}. Otherwise schedules a regular backup with the 60-second boot delay.
     *
     * @return the enqueued job, or {@code null}
     */
    @Nullable
    ScheduledJob scheduleBootup();

    /**
     * Schedules an immediate one-off backup with no network constraint.
     * <p>
     * Triggered by the {@code com.zegoggles.smssync.BACKUP} broadcast intent
     * (CNTR-MODERNIZATION-005). {@code BackupType.BROADCAST_INTENT}; REPLACE; no delay;
     * no network constraint ({@code Constraints.NONE} in the WorkManager adapter).
     *
     * @return the enqueued job, or {@code null}
     */
    @Nullable
    ScheduledJob scheduleImmediate();

    /**
     * Schedules a one-off restore job.
     * <p>
     * New capability — there is no restore scheduler in the current source. The
     * {@link LegacyScheduler} stub returns {@code null} with a log message; callers
     * must not crash when this returns {@code null}. The full implementation arrives
     * in U-014 / U-016.
     *
     * @param config restore configuration carrying the unique-work identity and
     *               durable checkpoint key
     * @return the enqueued job, or {@code null} if the adapter does not support it yet
     */
    @Nullable
    ScheduledJob scheduleRestore(@NonNull RestoreSchedulerConfig config);

    // -----------------------------------------------------------------------
    // Cancellation operations
    // -----------------------------------------------------------------------

    /**
     * Cancels all scheduled jobs (equivalent to {@link #cancelRegular()} plus cancel
     * of the content-trigger job).
     */
    void cancelAll();

    /**
     * Cancels the periodic backup job only.
     */
    void cancelRegular();

    /**
     * Cancels a single job by kind.
     *
     * @param jobKind the job type to cancel
     */
    void cancel(@NonNull BackupType jobKind);

    // -----------------------------------------------------------------------
    // Observability
    // -----------------------------------------------------------------------

    /**
     * Returns an observable carrying the current state of the given job kind.
     * <p>
     * The {@link LegacyScheduler} returns an observable permanently set to
     * {@link SchedulerState#Unknown} — Firebase JobDispatcher has no state API.
     * The {@code WorkManagerScheduler} (U-014) will return a live observable backed
     * by WorkManager {@code WorkInfo}. This method must not block.
     *
     * @param jobKind the job type to observe
     * @return an observable emitting {@link SchedulerState} values; never {@code null}
     */
    @NonNull
    SchedulerObservable<SchedulerState> observe(@NonNull BackupType jobKind);
}
