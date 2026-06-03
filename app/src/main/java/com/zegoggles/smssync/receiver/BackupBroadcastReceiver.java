/* Copyright (c) 2010 Jan Berkel <jan.berkel@gmail.com>
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
package com.zegoggles.smssync.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.scheduler.BackupScheduler;

import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;

/**
 * Receives the public {@code com.zegoggles.smssync.BACKUP} broadcast.
 * <p>
 * CNTR-MODERNIZATION-005: the action string {@link #BACKUP_ACTION}, the receiver
 * class name, the manifest intent-filter, and the {@code isAllow3rdPartyIntegration()}
 * guard are all frozen and must not change. The only change in U-013 is the
 * replacement of {@code new BackupJobs(context).scheduleImmediate()} with
 * {@code scheduler.scheduleImmediate()} (CS-1). All other behavior is byte-for-byte
 * identical to the pre-U-013 version.
 */
public class BackupBroadcastReceiver extends BroadcastReceiver {
    public static final String BACKUP_ACTION = "com.zegoggles.smssync.BACKUP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOCAL_LOGV) Log.v(TAG, "onReceive(" + context + "," + intent + ")");

        if (BACKUP_ACTION.equals(intent.getAction())) {
            backupRequested(context, intent);
        }
    }

    private void backupRequested(Context context, Intent intent) {
        if (new Preferences(context).isAllow3rdPartyIntegration()) {
            Log.d(TAG, "backup requested via broadcast intent");
            // U-013 CS-1: was new BackupJobs(context).scheduleImmediate()
            getScheduler(context).scheduleImmediate();
        } else {
            Log.d(TAG, "backup requested via broadcast intent but ignored");
        }
    }

    /**
     * Returns the application-scoped {@link BackupScheduler}.
     * <p>
     * Protected to allow test subclasses to inject a mock scheduler without
     * depending on the Application singleton (replaces the old getBackupJobs factory).
     * Will be replaced by {@code @AndroidEntryPoint} field injection in U-022.
     */
    protected BackupScheduler getScheduler(Context context) {
        return App.getScheduler(context);
    }
}
