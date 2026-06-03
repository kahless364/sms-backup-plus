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
import android.util.Log;
import com.fsck.k9.mail.K9MailLib;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.zegoggles.smssync.activity.events.AutoBackupSettingsChangedEvent;
import com.zegoggles.smssync.compat.GooglePlayServices;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.receiver.BootReceiver;
import com.zegoggles.smssync.receiver.SmsBroadcastReceiver;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import com.zegoggles.smssync.scheduler.LegacyScheduler;
import com.zegoggles.smssync.service.BackupJobs;
import com.zegoggles.smssync.service.state.DefaultSyncStateRepository;
import com.zegoggles.smssync.service.state.SyncStateRepository;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;

public class App extends Application {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    public static final boolean LOCAL_LOGV = DEBUG;
    public static final String TAG = "SMSBackup+";
    public static final String LOG = "sms_backup_plus.log";
    public static final String CHANNEL_ID = "sms_backup_plus";

    private static final Bus bus = new Bus();

    /**
     * U-019: Manual application singleton for SyncStateRepository.
     * Constructed in onCreate; exposed via syncStateRepository() accessor.
     * Hilt injection deferred to U-022 (DES-MODERNIZATION-008).
     * AC-5: exactly one construction site; IC-1: accessible via static accessor.
     */
    private static SyncStateRepository syncStateRepositoryInstance;

    /** Google Play Services present on this device? */
    public static boolean gcmAvailable;

    private Preferences preferences;

    /**
     * Application-scoped {@link BackupScheduler} singleton.
     * <p>
     * U-013: replaced {@code BackupJobs} field with this port-level field so that
     * the bound implementation can be swapped in U-014/U-017 by changing this one
     * assignment (or, once Hilt is introduced in U-022, by changing the Hilt binding).
     * <p>
     * The static {@link #getScheduler(Context)} accessor lets BroadcastReceiver call
     * sites resolve the scheduler without a Hilt entry point (which is not yet
     * available). This is the "EntryPoints.get fallback" documented in the story's
     * Technical Notes and will be replaced by {@code @AndroidEntryPoint} injection
     * in U-022.
     */
    private BackupScheduler scheduler;

    /**
     * Returns the application-scoped {@link BackupScheduler} singleton.
     * <p>
     * Used by BroadcastReceiver call sites (BackupBroadcastReceiver, BootReceiver,
     * SmsBroadcastReceiver) that cannot use constructor injection because
     * {@code BroadcastReceiver.onReceive} is system-managed. This is the approved
     * pre-Hilt DI fallback (U-013 Technical Notes; replaced by Hilt in U-022).
     *
     * @param context any context; used to obtain the Application instance
     * @return the singleton BackupScheduler; never null after Application.onCreate()
     */
    @NonNull
    public static BackupScheduler getScheduler(@NonNull Context context) {
        return ((App) context.getApplicationContext()).scheduler;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setupStrictMode();
        // U-019: construct the SyncStateRepository singleton before any other component
        // that may call syncStateRepository(). Must be first so the accessor is non-null
        // when register(this) fires Otto subscriptions later in onCreate.
        syncStateRepositoryInstance = new DefaultSyncStateRepository();
        gcmAvailable = GooglePlayServices.isAvailable(this);
        preferences = new Preferences(this);
        preferences.migrate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        // U-013: construct LegacyScheduler (wrapping BackupJobs) as the BackupScheduler
        // binding. This is the strangler seam: all call sites route through the port.
        // U-014 will introduce WorkManagerScheduler; U-017 will flip this binding and
        // remove BackupJobs. BackupJobs.java is unmodified by this story.
        scheduler = new LegacyScheduler(new BackupJobs(this));

        if (gcmAvailable) {
            setBroadcastReceiversEnabled(false);
        } else {
            Log.v(TAG, "Google Play Services not available, forcing use of old scheduler");
            preferences.setUseOldScheduler(true);
        }

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
        register(this);
    }

    @Subscribe public void autoBackupSettingsChanged(final AutoBackupSettingsChangedEvent event) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "autoBackupSettingsChanged("+event+")");
        }
        setBroadcastReceiversEnabled(preferences.isUseOldScheduler() && preferences.isAutoBackupEnabled());
        rescheduleJobs();
    }

    public static void register(Object listener) {
        try {
            bus.register(listener);
        } catch (IllegalArgumentException ignored) {
            Log.w(TAG, ignored);
        }
     }

    public static void unregister(Object listener) {
        try {
            bus.unregister(listener);
        } catch (IllegalArgumentException ignored) {
            Log.w(TAG, ignored);
        }
    }

    public static void post(Object event) {
        bus.post(event);
    }

    /**
     * U-019: Application-scoped SyncStateRepository accessor.
     * Constructed in onCreate; non-null for the lifetime of the application process.
     * AC-5: single construction site; IC-1: reachable from all production call sites.
     * Hilt injection deferred to U-022 (DES-MODERNIZATION-008).
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
        // NB: changes made via setComponentEnabledSetting are persisted across reboots
        getPackageManager().setComponentEnabledSetting(
            new ComponentName(this, component),
            enabled ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DISABLED,
            DONT_KILL_APP /* apply setting without restart */);
    }

    /**
     * Cancels all existing backup jobs and reschedules them according to current
     * preferences.
     * <p>
     * U-013: migrated from direct {@code BackupJobs} calls to the
     * {@link BackupScheduler} port. AC-6 / App.rescheduleJobs() compliance:
     * - cancelAll()
     * - scheduleRegular()
     * - scheduleContentTrigger() (guarded by isUseOldScheduler() check per AC-6 note)
     */
    private void rescheduleJobs() {
        scheduler.cancelAll();

        if (preferences.isAutoBackupEnabled()) {
            scheduler.scheduleRegular();

            // AC-6 note: preserve the isUseOldScheduler() guard from the original
            // App.java:201. The content-trigger job is only scheduled for the new
            // (GCM-backed) scheduler path. The guard is preserved here unchanged.
            if (preferences.getIncomingTimeoutSecs() > 0 && !preferences.isUseOldScheduler()) {
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
