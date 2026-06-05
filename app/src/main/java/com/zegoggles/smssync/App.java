/*
 * Copyright (c) 2010 Jan Berkel <jan.berkel@gmail.com>
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

package com.zegoggles.smssync;

import android.Manifest;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import android.util.Log;
import com.fsck.k9.mail.K9MailLib;
import com.zegoggles.smssync.compat.GooglePlayServices;
import com.zegoggles.smssync.preferences.CredentialMigrationGate;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.receiver.BootReceiver;
import com.zegoggles.smssync.receiver.SmsBroadcastReceiver;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import com.zegoggles.smssync.scheduler.WorkManagerScheduler;
import com.zegoggles.smssync.service.state.FlowSyncStateRepository;
import com.zegoggles.smssync.service.state.SyncStateRepository;
import dagger.hilt.android.HiltAndroidApp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

/**
 * Application class.
 *
 * U-020: Otto bus (Bus field, register/unregister/post helpers, @Subscribe handler)
 * removed. SyncStateRepository is now a live FlowSyncStateRepository backed by
 * MutableStateFlow + MutableSharedFlow. The autoBackupSettingsChanged handler is
 * collected in an application-scoped coroutine via FlowCollectHelper.
 *
 * AC-6: App.bus field, register(), unregister(), post() are deleted.
 * AC-6(d): autoBackupSettingsChanged @Subscribe migrated to Flow collection.
 *
 * U-022: @HiltAndroidApp triggers code generation of the Hilt application component.
 * Hilt generates Hilt_App which this class extends (transparently, via the plugin).
 * The @Inject Preferences field is populated by Hilt before the onCreate() body runs
 * (Hilt_App.onCreate() calls inject(this) then super.onCreate()).
 *
 * U-024: App implements Configuration.Provider so WorkManager uses HiltWorkerFactory
 * instead of the default reflective no-arg factory. WorkManager auto-initialization via
 * androidx.startup.InitializationProvider is disabled in AndroidManifest.xml
 * (tools:node="remove") to prevent WorkManager from initializing before Hilt can inject
 * the HiltWorkerFactory. WorkManager is initialized explicitly in onCreate() via
 * WorkManager.initialize(this, getWorkManagerConfiguration()) after Hilt injection.
 *
 * IC-1 (DES-MODERNIZATION-008): HiltWorkerFactory must be a field-injected member,
 * not a constructor parameter — @HiltAndroidApp only supports field injection on App.
 */
@HiltAndroidApp
public class App extends Application implements Configuration.Provider {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    public static final boolean LOCAL_LOGV = DEBUG;
    public static final String TAG = "SMSBackup+";
    public static final String LOG = "sms_backup_plus.log";
    public static final String CHANNEL_ID = "sms_backup_plus";

    // U-020: FlowSyncStateRepository replaces DefaultSyncStateRepository (Otto-delegating).
    // AC-18: manual-DI singleton — NOT wired via Hilt @Singleton (deferred to U-022).
    // TODO U-022/MU-007: replace manual singleton with Hilt @Singleton
    private static SyncStateRepository syncStateRepositoryInstance;

    /** Google Play Services present on this device? */
    public static boolean gcmAvailable;

    // U-022: @Inject replaces the manual 'new Preferences(this)' call at the old line 105.
    // PreferencesModule.providePreferences(@ApplicationContext) supplies this singleton.
    // Hilt populates this field before onCreate() body executes (AC-3).
    @Inject Preferences preferences;

    /**
     * U-024: HiltWorkerFactory injected by Hilt (field injection only — @HiltAndroidApp
     * does not support constructor injection on Application). This field is populated by
     * Hilt_App.onCreate() before this class's onCreate() runs.
     *
     * Used in {@link #getWorkManagerConfiguration()} to configure WorkManager with the
     * Hilt-managed factory. Must be non-null by the time getWorkManagerConfiguration()
     * is called (ensured by calling WorkManager.initialize() inside onCreate() after
     * super.onCreate() completes Hilt injection).
     */
    @Inject HiltWorkerFactory hiltWorkerFactory;

    /**
     * Application-scoped {@link BackupScheduler} singleton.
     * U-013: replaced BackupJobs field with this port-level field.
     * U-017: binding flipped from LegacyScheduler to WorkManagerScheduler.
     * TODO U-022: replace with Hilt @Inject BackupScheduler.
     */
    private BackupScheduler scheduler;

    @NonNull
    public static BackupScheduler getScheduler(@NonNull Context context) {
        return ((App) context.getApplicationContext()).scheduler;
    }

    /**
     * U-024: Configuration.Provider implementation — returns WorkManager configuration
     * that uses Hilt's managed factory instead of the default reflective factory.
     *
     * WorkManager calls this method once during initialization. The hiltWorkerFactory
     * field must be non-null at call time; it is populated by Hilt_App.onCreate() before
     * this class's onCreate() body executes, ensuring non-null state when
     * WorkManager.initialize(this, getWorkManagerConfiguration()) is called below.
     *
     * AC-3 (DES-MODERNIZATION-008): HiltWorkerFactory is the sole WorkerFactory in
     * production. No DefaultWorkerFactory or hand-rolled WorkerFactory survives here.
     */
    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(hiltWorkerFactory)
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupStrictMode();

        // U-020: FlowSyncStateRepository replaces DefaultSyncStateRepository.
        // Constructed before any component reaches syncStateRepository().
        // AC-3: MutableStateFlow + MutableSharedFlow(replay=0, extraBufferCapacity=1).
        syncStateRepositoryInstance = new FlowSyncStateRepository();

        gcmAvailable = GooglePlayServices.isAvailable(this);

        // U-035 / BUG-003: Run the one-time plaintext-to-encrypted credential migration
        // OFF the main thread to avoid StrictMode disk-write violations and ANR risk.
        //
        // Previously preferences.migrate() was called synchronously here (U-022/AC-3),
        // which triggered EncryptedPrefsSecretStore.migrateFromPlaintext() on the main
        // thread — Keystore IPC + two synchronous commit() calls = disk-write violation.
        //
        // Fix: prepare the completion gate BEFORE dispatching so the latch is always in
        // place when credential consumers check it (race-safety: prepare() -> submit() ->
        // signalComplete() is the guaranteed ordering; no consumer can see a null latch
        // because prepare() happens-before submit() on the same thread).
        //
        // IC-1: Migration still fires on every App.onCreate() until the
        // MIGRATION_COMPLETE_KEY marker is present (EncryptedPrefsSecretStore idempotency
        // short-circuit). After migration, the background task completes quickly and the
        // gate is signalled before any BackupWorker credential read occurs.
        //
        // See CredentialMigrationGate for the full race-safety argument.
        final Preferences capturedPreferences = preferences;
        CredentialMigrationGate.prepare();
        final ExecutorService migrationExecutor = Executors.newSingleThreadExecutor();
        migrationExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // Both commit() calls inside migrateFromPlaintext() run here,
                    // on a background I/O thread — NOT the main thread (AC-3).
                    capturedPreferences.migrate();
                } finally {
                    // Signal the gate regardless of success/failure so credential
                    // consumers are never permanently blocked.
                    CredentialMigrationGate.signalComplete();
                    // Shut down the single-thread executor after the one job completes.
                    migrationExecutor.shutdown();
                }
            }
        });

        // U-024: Explicitly initialize WorkManager with Hilt's configuration so the
        // HiltWorkerFactory is registered. Auto-initialization is disabled in
        // AndroidManifest.xml (tools:node="remove" on InitializationProvider).
        // This call must happen after super.onCreate() (which runs Hilt injection) so
        // hiltWorkerFactory is non-null.
        //
        // Guard: In Robolectric unit tests, WorkManagerTestInitHelper.initializeTestWorkManager()
        // may have already initialized WorkManager before App.onCreate() runs. Since
        // WorkManager.initialize() throws IllegalStateException if called twice, we guard
        // with a try-catch to allow both production (manual init) and test (already inited)
        // paths to coexist. hiltWorkerFactory may be null in tests (Hilt not active in
        // Robolectric non-@HiltAndroidTest tests), so only call initialize if non-null.
        if (hiltWorkerFactory != null) {
            try {
                androidx.work.WorkManager.initialize(this, getWorkManagerConfiguration());
            } catch (IllegalStateException alreadyInitialized) {
                Log.d(TAG, "WorkManager already initialized (test environment), skipping: " + alreadyInitialized.getMessage());
            }
        } else {
            // In non-Hilt test environments (Robolectric without HiltAndroidTest), hiltWorkerFactory
            // is null because Hilt injection does not run. WorkManager initialization via
            // Configuration.Provider is not available; WorkManager should have been initialized
            // by the test setUp via WorkManagerTestInitHelper.initializeTestWorkManager().
            Log.d(TAG, "hiltWorkerFactory is null — skipping WorkManager.initialize() (test environment without Hilt)");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        // U-017: binding flipped to WorkManagerScheduler (Gate G3).
        // The legacy scheduling classes have been deleted per AC-3/AC-4/AC-5.
        // WorkManagerScheduler is now the sole production BackupScheduler implementation.
        scheduler = new WorkManagerScheduler(this, preferences);

        // U-017: SmsBroadcastReceiver / BootReceiver enable/disable logic simplified:
        // WorkManagerScheduler owns all scheduling; legacy GCM/AlarmManager toggle removed.
        // Receivers remain enabled (WorkManager does not need manual component toggling).
        setBroadcastReceiversEnabled(preferences.isAutoBackupEnabled());

        K9MailLib.setDebugStatus(new K9MailLib.DebugStatus() {
            @Override
            public boolean enabled() {
                return preferences.isAppLogDebug();
            }

            @Override
            public boolean debugSensitive() {
                return false;
            }
        });

        if (gcmAvailable && DEBUG) {
            getContentResolver().registerContentObserver(Consts.SMS_PROVIDER, true, new LoggingContentObserver());
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                getContentResolver().registerContentObserver(Consts.CALLLOG_PROVIDER, true, new LoggingContentObserver());
            }
        }

        // U-020: replaces @Subscribe autoBackupSettingsChanged + register(this).
        // Collects SyncEvent.AutoBackupSettingsChanged in an Application-scoped coroutine.
        // TODO U-022/MU-007: replace manual scope with ProcessLifecycleOwner scope.
        FlowCollectHelper.collectAutoBackupSettings(
            syncStateRepositoryInstance,
            new Runnable() {
                @Override public void run() {
                    if (LOCAL_LOGV) {
                        Log.v(TAG, "autoBackupSettingsChanged()");
                    }
                    // U-017: isUseOldScheduler() check removed; WorkManagerScheduler is now sole impl.
                    setBroadcastReceiversEnabled(preferences.isAutoBackupEnabled());
                    rescheduleJobs();
                }
            }
        );
    }

    // U-020: register(), unregister(), post() deleted (AC-6a,b,c).
    // All call sites migrated to App.syncStateRepository().emitState/tryEmitEvent/emitEvent.

    /**
     * U-019: Application-scoped SyncStateRepository accessor.
     * U-020: now returns FlowSyncStateRepository (Flow-backed, no Otto delegation).
     * Constructed in onCreate; non-null for the lifetime of the application process.
     * IC-1: reachable from all production consumers (services, activities, workers).
     * TODO U-022/MU-007: replace with @Inject SyncStateRepository.
     */
    public static SyncStateRepository syncStateRepository() {
        return syncStateRepositoryInstance;
    }

    @Nullable
    public static String getVersionName(Context context) {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, null, e);
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static int getVersionCode(Context context) {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, null, e);
            return -1;
        }
    }

    public static boolean isInstalledOnSDCard(Context context) {
        PackageInfo pInfo;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return (pInfo.applicationInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "error", e);
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "default",
                NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManagerCompat.from(this).createNotificationChannel(channel);
    }

    private void setBroadcastReceiversEnabled(boolean enabled) {
        enableOrDisableComponent(enabled, SmsBroadcastReceiver.class);
        enableOrDisableComponent(enabled, BootReceiver.class);
    }

    private void enableOrDisableComponent(boolean enabled, Class<?> component) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "enableOrDisableComponent("+enabled+", "+component.getSimpleName()+")");
        }
        getPackageManager().setComponentEnabledSetting(
            new ComponentName(this, component),
            enabled ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DISABLED,
            DONT_KILL_APP);
    }

    private void rescheduleJobs() {
        scheduler.cancelAll();

        if (preferences.isAutoBackupEnabled()) {
            scheduler.scheduleRegular();

            // U-017: isUseOldScheduler() guard removed; WorkManagerScheduler handles
            // API-level branching internally (content-URI triggers on API 24+).
            if (preferences.getIncomingTimeoutSecs() > 0) {
                scheduler.scheduleContentTrigger();
            }
        }
    }

    private void setupStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                    .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyFlashScreen()
            .build());
    }

    @SuppressWarnings("deprecation")
    private static class LoggingContentObserver extends ContentObserver {
        LoggingContentObserver() {
            super(new Handler());
        }
        @Override public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }
        @Override public void onChange(boolean selfChange, Uri uri) {
            Log.v(TAG, "onChange("+selfChange+", " + uri+")");
        }
    }
}
