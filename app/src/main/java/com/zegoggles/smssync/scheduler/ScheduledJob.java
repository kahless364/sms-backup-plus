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

/**
 * Port-level value object representing an enqueued scheduling item.
 * <p>
 * This type crosses the {@link BackupScheduler} port boundary and carries no
 * Android scheduler-framework types ({@code com.firebase.*} or
 * {@code androidx.work.*}), so consumers and core logic remain scheduler-agnostic.
 * <p>
 * CNTR-MODERNIZATION-004: this is the return type for all nine scheduling
 * operations. Adapters construct an instance from the framework-specific result.
 */
public final class ScheduledJob {
    /**
     * Stable tag / unique-work name that identifies this job within the scheduler.
     * Corresponds to {@code BackupType.name()} for regular/incoming/broadcast jobs,
     * {@code BackupJobs.CONTENT_TRIGGER_TAG} for the content-trigger job, or a
     * restore-specific name for restore jobs.
     */
    @NonNull public final String tag;

    /**
     * Human-readable description, e.g. "REGULAR @ 2000s" or "BROADCAST_INTENT immediate".
     * Optional – primarily for logging.
     */
    @NonNull public final String description;

    public ScheduledJob(@NonNull String tag, @NonNull String description) {
        this.tag = tag;
        this.description = description;
    }

    @Override
    public String toString() {
        return "ScheduledJob{tag='" + tag + "', description='" + description + "'}";
    }
}
