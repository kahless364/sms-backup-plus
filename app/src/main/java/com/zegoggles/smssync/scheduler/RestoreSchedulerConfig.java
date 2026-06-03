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
 * Configuration for {@link BackupScheduler#scheduleRestore(RestoreSchedulerConfig)}.
 * <p>
 * CNTR-MODERNIZATION-004 §RestoreSchedulerConfig: carries the unique-work identity
 * and durable checkpoint key required for a resumable restore job. There is no
 * restore scheduler in current source (BackupJobs.java has no restore method); this
 * type is introduced to complete the port surface so U-014/U-016 can implement it
 * without changing the port.
 * <p>
 * The {@code LegacyScheduler.scheduleRestore} stub returns {@code null} — callers
 * that invoke it before U-016 ships must not crash.
 */
public final class RestoreSchedulerConfig {
    /** Unique work name identifying the restore job in the scheduler. */
    @NonNull public final String uniqueName;
    /**
     * Key for the durable restore cursor (currentRestoredItem), persisted after
     * each successful provider insert and before advancing the cursor.
     * See DES-MODERNIZATION-005 §Durable restore checkpoint.
     */
    @NonNull public final String checkpointKey;

    public RestoreSchedulerConfig(@NonNull String uniqueName, @NonNull String checkpointKey) {
        this.uniqueName = uniqueName;
        this.checkpointKey = checkpointKey;
    }

    @Override
    public String toString() {
        return "RestoreSchedulerConfig{uniqueName='" + uniqueName + "', checkpointKey='" + checkpointKey + "'}";
    }
}
