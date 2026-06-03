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

/**
 * Observable state of a scheduled job, emitted by {@link BackupScheduler#observe}.
 * <p>
 * CNTR-MODERNIZATION-004 §SchedulerState: maps the WorkManager WorkInfo lifecycle
 * (ENQUEUED → RUNNING → SUCCEEDED/FAILED/CANCELLED) into an Android-framework-free
 * representation. The {@code LegacyScheduler} returns {@code Unknown} because
 * Firebase JobDispatcher exposes no observable state API.
 * <p>
 * Variants are modelled as instances to avoid an enum with a mutable
 * {@code Running.progress} field. A full sealed-class hierarchy would be written
 * in Kotlin; this Java version uses a flat enum with a nullable progress payload.
 */
public enum SchedulerState {
    /** State is unknown or not supported by the current adapter (LegacyScheduler). */
    Unknown,
    /** The job has been accepted by the scheduler and is waiting to run. */
    Enqueued,
    /** The job is currently executing. */
    Running,
    /** The job finished successfully. */
    Succeeded,
    /** The job failed (non-terminal retries may still occur). */
    Failed,
    /** The job was cancelled via {@link BackupScheduler#cancel} or {@link BackupScheduler#cancelAll}. */
    Cancelled
}
