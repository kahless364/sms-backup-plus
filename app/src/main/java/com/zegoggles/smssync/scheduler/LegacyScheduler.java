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

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.zegoggles.smssync.service.BackupJobs;
import com.zegoggles.smssync.service.BackupType;

import static com.zegoggles.smssync.App.TAG;

/**
 * {@link BackupScheduler} adapter that wraps the existing {@link BackupJobs}
 * machinery without modifying it.
 * <p>
 * This is the "branch-by-abstraction seam" introduced by U-013. All current
 * scheduling behavior (retry strategy, network constraints, recurring vs. one-off
 * job types, {@code setReplaceCurrent(true)}, tag strings) passes through
 * {@link BackupJobs} unchanged. The {@code LegacyScheduler} is a pure delegation
 * layer.
 * <p>
 * <strong>BackupJobs.java has zero diffs from this story</strong> — this adapter
 * holds a reference and calls its existing public methods; it does not modify it.
 * <p>
 * No Firebase JobDispatcher types appear in this file (AC-10). The
 * adapter calls {@link BackupJobs} public methods; the returned Job objects
 * are not inspected — the job tag is known from the BackupType constant or
 * the documented CONTENT_TRIGGER_TAG string ("contentTrigger").
 * <p>
 * Stubs for unimplemented capabilities:
 * <ul>
 *   <li>{@link #scheduleRestore}: returns {@code null} with a log message — there is
 *       no restore scheduler in the legacy path.</li>
 *   <li>{@link #observe}: returns a {@code SchedulerObservable} permanently set to
 *       {@link SchedulerState#Unknown} — Firebase JobDispatcher exposes no state API.
 *       This is not a regression because the legacy path had no observable state.</li>
 * </ul>
 * <p>
 * CNTR-MODERNIZATION-004 AC-2 compliance verified:
 * <ul>
 *   <li>scheduleIncoming  → backupJobs.scheduleIncoming()</li>
 *   <li>scheduleRegular   → backupJobs.scheduleRegular()</li>
 *   <li>scheduleContentTrigger → backupJobs.scheduleContentTriggerJob()</li>
 *   <li>scheduleBootup    → backupJobs.scheduleBootup()</li>
 *   <li>scheduleImmediate → backupJobs.scheduleImmediate()</li>
 *   <li>cancelAll         → backupJobs.cancelAll()</li>
 *   <li>cancelRegular     → backupJobs.cancelRegular()</li>
 *   <li>cancel(kind)      → backupJobs.cancelRegular() (REGULAR) or no-op for others
 *       (content-trigger cancel is private in BackupJobs and exercised transitively
 *       via cancelAll())</li>
 * </ul>
 */
public class LegacyScheduler implements BackupScheduler {

    /**
     * Tag for the content-trigger job — matches {@code BackupJobs.CONTENT_TRIGGER_TAG}
     * ("contentTrigger"). Declared here to avoid a dependency on the package-private
     * BackupJobs constant while remaining accurate (value verified against BackupJobs.java:57).
     */
    static final String CONTENT_TRIGGER_TAG = "contentTrigger";

    @NonNull private final BackupJobs backupJobs;

    /**
     * Creates a {@code LegacyScheduler} backed by the provided {@link BackupJobs} instance.
     *
     * @param backupJobs the existing scheduler implementation; must not be {@code null}
     */
    public LegacyScheduler(@NonNull BackupJobs backupJobs) {
        this.backupJobs = backupJobs;
    }

    // -----------------------------------------------------------------------
    // Scheduling operations — all delegate to BackupJobs
    // -----------------------------------------------------------------------

    @Override
    @Nullable
    public ScheduledJob scheduleIncoming() {
        return backupJobs.scheduleIncoming() != null
                ? new ScheduledJob(BackupType.INCOMING.name(), "LegacyScheduler:INCOMING")
                : null;
    }

    @Override
    @Nullable
    public ScheduledJob scheduleRegular() {
        return backupJobs.scheduleRegular() != null
                ? new ScheduledJob(BackupType.REGULAR.name(), "LegacyScheduler:REGULAR")
                : null;
    }

    @Override
    @Nullable
    public ScheduledJob scheduleContentTrigger() {
        // Port name differs from BackupJobs method name: scheduleContentTrigger() vs
        // scheduleContentTriggerJob(). The discrepancy is entirely inside this adapter.
        return backupJobs.scheduleContentTriggerJob() != null
                ? new ScheduledJob(CONTENT_TRIGGER_TAG, "LegacyScheduler:" + CONTENT_TRIGGER_TAG)
                : null;
    }

    @Override
    @Nullable
    public ScheduledJob scheduleBootup() {
        // BackupJobs.scheduleBootup() returns: a REGULAR job (oldScheduler=true, autoBackup=true),
        // null (newScheduler path, autoBackup=true), or null (autoBackup=false, after cancelAll).
        // Since the returned Job is never null when a real schedule occurred (and is null
        // otherwise), we check for null consistently.
        return backupJobs.scheduleBootup() != null
                ? new ScheduledJob(BackupType.REGULAR.name(), "LegacyScheduler:bootup")
                : null;
    }

    @Override
    @Nullable
    public ScheduledJob scheduleImmediate() {
        return backupJobs.scheduleImmediate() != null
                ? new ScheduledJob(BackupType.BROADCAST_INTENT.name(), "LegacyScheduler:BROADCAST_INTENT")
                : null;
    }

    /**
     * Stub: no restore scheduler exists in the legacy path.
     * Returns {@code null} with a log message. Callers must not crash on {@code null}.
     */
    @Override
    @Nullable
    public ScheduledJob scheduleRestore(@NonNull RestoreSchedulerConfig config) {
        Log.i(TAG, "LegacyScheduler.scheduleRestore: no legacy restore scheduler; "
                + "returning null. Will be implemented in U-014/U-016. config=" + config);
        return null;
    }

    // -----------------------------------------------------------------------
    // Cancellation operations — all delegate to BackupJobs
    // -----------------------------------------------------------------------

    @Override
    public void cancelAll() {
        backupJobs.cancelAll();
    }

    @Override
    public void cancelRegular() {
        backupJobs.cancelRegular();
    }

    /**
     * Single-flight cancellation by job kind.
     * <p>
     * {@code BackupJobs.cancelContentUriTrigger()} is private and is exercised
     * transitively via {@link #cancelAll()}; it is not called here directly.
     * For {@link BackupType#REGULAR}, delegates to {@code backupJobs.cancelRegular()}.
     * Content-trigger cancellation should be done via {@link #cancelAll()}.
     *
     * @param jobKind the job type to cancel
     */
    @Override
    public void cancel(@NonNull BackupType jobKind) {
        switch (jobKind) {
            case REGULAR:
                backupJobs.cancelRegular();
                break;
            default:
                // INCOMING and BROADCAST_INTENT jobs are one-off; cancelling them by
                // kind is a no-op for the legacy scheduler since there is no public
                // cancel(tag) method on BackupJobs (the private method is only reachable
                // through cancelAll / cancelRegular). Log for diagnostics.
                Log.d(TAG, "LegacyScheduler.cancel(" + jobKind + "): no-op for legacy scheduler "
                        + "(use cancelAll() to cancel content-trigger and regular)");
                break;
        }
    }

    // -----------------------------------------------------------------------
    // Observability — stub for legacy path
    // -----------------------------------------------------------------------

    /**
     * Returns a {@link SchedulerObservable} permanently set to
     * {@link SchedulerState#Unknown}.
     * <p>
     * Firebase JobDispatcher exposes no observable state API; returning Unknown is a
     * no-op semantically. This is not a behavioral regression: the legacy path had
     * no observable scheduler state (progress was via the Otto bus). The observable
     * becomes live in U-014/U-019 when the WorkManagerScheduler is introduced.
     *
     * @param jobKind ignored in the legacy path
     * @return a static observable emitting {@link SchedulerState#Unknown}
     */
    @Override
    @NonNull
    public SchedulerObservable<SchedulerState> observe(@NonNull BackupType jobKind) {
        return new SchedulerObservable<>(SchedulerState.Unknown);
    }
}
