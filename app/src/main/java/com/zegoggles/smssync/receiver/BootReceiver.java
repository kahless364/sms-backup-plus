package com.zegoggles.smssync.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.scheduler.BackupScheduler;

import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;

/**
 * Schedules a backup on device boot.
 * <p>
 * U-013 CS-2: replaced {@code getBackupJobs(context)} factory + {@code BackupJobs}
 * with an injected {@link BackupScheduler} port. The {@code getBackupJobs} factory
 * method is removed; {@code getScheduler} provides the same test-override surface.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOCAL_LOGV) Log.v(TAG, "onReceive(" + context + "," + intent + ")");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            bootup(context);
        } else {
            Log.w(TAG, "unhandled intent: "+intent);
        }
    }

    private void bootup(Context context) {
        Log.i(TAG, "bootup");
        getScheduler(context).scheduleBootup();
    }

    /**
     * Returns the application-scoped {@link BackupScheduler}.
     * <p>
     * Protected to allow test subclasses to inject a mock (replaces the old
     * {@code getBackupJobs} factory). Will be replaced by {@code @AndroidEntryPoint}
     * field injection in U-022.
     */
    protected BackupScheduler getScheduler(Context context) {
        return App.getScheduler(context);
    }
}
