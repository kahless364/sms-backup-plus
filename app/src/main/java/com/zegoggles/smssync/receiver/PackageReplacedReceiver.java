package com.zegoggles.smssync.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.service.state.SyncEvent;

import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;

// U-020: App.post(new AutoBackupSettingsChangedEvent()) replaced by
// App.syncStateRepository().tryEmitEvent(SyncEvent.AutoBackupSettingsChanged) (AC-17).
public class PackageReplacedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (LOCAL_LOGV) Log.v(TAG, "onReceive(" + context + "," + intent + ")");

        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            Log.d(TAG, "now installed version: " + App.getVersionCode(context));
            // U-020: replaces App.post(new AutoBackupSettingsChangedEvent())
            if (App.syncStateRepository() != null) {
                boolean emitted = App.syncStateRepository().tryEmitEvent(
                    SyncEvent.AutoBackupSettingsChanged.INSTANCE);
                if (!emitted) {
                    Log.w(TAG, "PackageReplacedReceiver: tryEmitEvent(AutoBackupSettingsChanged) returned false");
                }
            }
        } else {
            Log.w(TAG, "unhandled intent: "+intent);
        }
    }
}
