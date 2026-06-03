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

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Production [RestoreCheckpointStore] adapter backed by [SharedPreferences] (U-016).
 *
 * Uses [SharedPreferences.Editor.commit] (synchronous) rather than [apply] (async) so that
 * the checkpoint write is durable before the restore loop advances to the next item.
 * This is critical for crash-safety: if the process is killed immediately after the write
 * returns, the value is guaranteed to be persisted to flash.
 *
 * SharedPreferences name: [PREFS_NAME] — a dedicated file distinct from app preferences
 * to avoid key collisions and to allow the file to be wiped independently.
 *
 * Key format: "checkpoint_<workName>" — one entry per unique restore work name.
 *
 * Thread safety: SharedPreferences is thread-safe for concurrent reads; [commit] is
 * blocking and serialised per the Android documentation. Since [write] and [clear] are
 * called from the WorkManager worker coroutine on a single thread per worker execution,
 * no additional synchronization is required.
 *
 * Per IC-1 / CNTR-MODERNIZATION-004 §IC-1, this adapter lives in the app module; the
 * [RestoreCheckpointStore] interface (the port) carries no Android types.
 *
 * TODO U-024: When Hilt DI is wired, inject this as the bound implementation of
 * [RestoreCheckpointStore] via a @Binds Hilt module.
 */
class SharedPreferencesCheckpointStore(private val context: Context) : RestoreCheckpointStore {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the stored checkpoint index for [workName], or [RestoreCheckpointStore.NO_CHECKPOINT]
     * (0) if none has been written.
     */
    override suspend fun read(workName: String): Int {
        val key = key(workName)
        val value = prefs.getInt(key, RestoreCheckpointStore.NO_CHECKPOINT)
        Log.d(TAG, "SharedPreferencesCheckpointStore.read($workName) = $value")
        return value
    }

    /**
     * Writes [index] as the checkpoint for [workName] using a synchronous [commit].
     * Returns only after the value is durably persisted to flash.
     */
    override suspend fun write(workName: String, index: Int) {
        val key = key(workName)
        val committed = prefs.edit().putInt(key, index).commit()
        if (!committed) {
            Log.w(TAG, "SharedPreferencesCheckpointStore.write($workName, $index): commit returned false")
        } else {
            Log.d(TAG, "SharedPreferencesCheckpointStore.write($workName, $index): committed")
        }
    }

    /**
     * Clears the checkpoint for [workName] by removing its key from the preferences file.
     * Uses synchronous [commit].
     */
    override suspend fun clear(workName: String) {
        val key = key(workName)
        val committed = prefs.edit().remove(key).commit()
        Log.d(TAG, "SharedPreferencesCheckpointStore.clear($workName): committed=$committed")
    }

    private fun key(workName: String) = "checkpoint_$workName"

    companion object {
        /** SharedPreferences file name for restore checkpoints. */
        const val PREFS_NAME = "restore_checkpoints"
        private const val TAG = "SMSBackup+"
    }
}
