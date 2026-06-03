package com.zegoggles.smssync.activity.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.zegoggles.smssync.App;
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.service.state.SyncEvent;

import static com.zegoggles.smssync.App.TAG;

// U-020: App.post(event) replaced by App.syncStateRepository().tryEmitEvent(event).
// addPreferenceListener(Object event, ...) signature changed to SyncEvent event.
public abstract class SMSBackupPreferenceFragment extends PreferenceFragmentCompat {
    protected Preferences preferences;
    private Handler handler;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        preferences = new Preferences(getContext(), getPreferenceManager().getSharedPreferences());
        handler = new Handler();
    }

    void addPreferenceListener(String... prefKeys) {
        addPreferenceListener(SyncEvent.AutoBackupSettingsChanged.INSTANCE, prefKeys);
    }

    void addPreferenceListener(final SyncEvent event, String... prefKeys) {
        for (String prefKey : prefKeys) {
            findPreference(prefKey).setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        public boolean onPreferenceChange(Preference preference, final Object newValue) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    // U-020: App.post(event) replaced by tryEmitEvent(event)
                                    if (App.syncStateRepository() != null) {
                                        boolean emitted = App.syncStateRepository().tryEmitEvent(event);
                                        if (!emitted) {
                                            Log.w(TAG, "SMSBackupPrefFragment: tryEmitEvent returned false for " + event);
                                        }
                                    }
                                }
                            });
                            return true;
                        }
                    });
        }
    }
}
