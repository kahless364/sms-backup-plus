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
package com.zegoggles.smssync.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.activity.MainActivity;
import com.zegoggles.smssync.mail.PinnedCertStore;
import com.zegoggles.smssync.mail.TlsTrustPolicy;
import com.zegoggles.smssync.mail.transport.K9MailTransport;
import com.zegoggles.smssync.mail.transport.MailException;
import com.zegoggles.smssync.mail.transport.MailTransport;
import com.zegoggles.smssync.mail.transport.MailTransportConfig;
import com.zegoggles.smssync.preferences.AuthPreferences;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.service.state.State;
import com.zegoggles.smssync.utils.AppLog;
import javax.inject.Inject;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static com.zegoggles.smssync.App.CHANNEL_ID;
import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
import static java.util.Locale.ENGLISH;

// U-022: ServiceBase is an abstract Service; concrete subclasses (SmsBackupService,
// SmsRestoreService) are annotated @AndroidEntryPoint so Hilt can inject @Inject
// fields declared here. The @Inject annotations on the fields below replace the
// per-call Service-Locator construction (ARCH-002 defect) that existed at
// ServiceBase.java:73,108,112 (verified source lines in DES-MODERNIZATION-008).
public abstract class ServiceBase extends Service {
    @Nullable private PowerManager.WakeLock wakeLock;
    @Nullable private WifiManager.WifiLock wifiLock;

    private AppLog appLog;
    @Nullable Notification notification;

    // U-022: @Inject fields that will be populated by Hilt once @AndroidEntryPoint is
    // applied to concrete service subclasses in U-023. Names are prefixed with 'injected'
    // to prevent Java field-shadowing in Robolectric test anonymous subclasses that declare
    // their own 'preferences'/'authPreferences' fields in the enclosing test class
    // (AC-10: existing tests must not be broken by this story).
    //
    // These replace the per-call Service-Locator construction pattern (ARCH-002):
    //   old: new Preferences(this) in onCreate()
    //   old: new Preferences(getApplicationContext()) in getPreferences()
    //   old: new AuthPreferences(this) in getAuthPreferences()
    // PreferencesModule provides both as @Singleton (AC-4, AC-6, IC-3).
    // Until @AndroidEntryPoint is applied (U-023), getPreferences()/getAuthPreferences()
    // fall back to on-demand construction when these fields are null.
    @Inject Preferences injectedPreferences;
    @Inject AuthPreferences injectedAuthPreferences;

    @Override
    public void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // U-022: getPreferences() now returns the @Inject field (preferences).
        // Using the accessor preserves test-override behaviour: test subclasses that
        // override getPreferences() to return a mock will still work correctly here,
        // even in the Robolectric test context where Hilt injection is not active
        // (AC-10: existing tests must not be broken by this story).
        if (getPreferences().isAppLogEnabled()) {
            this.appLog = new AppLog(this);
        }
        // U-020: App.register(this) removed (AC-7). No Otto bus registration needed.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (appLog != null) appLog.close();
        // U-020: App.unregister(this) removed (AC-7). No Otto bus deregistration needed.
        notification = null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_NOT_STICKY;
    }

    public abstract @NonNull State getState();

    public boolean isWorking() {
        return getState().isRunning();
    }

    /**
     * U-026 AC-1: Replaces {@code getBackupImapStore()} with an app-owned seam.
     *
     * <p>Builds a {@link MailTransportConfig} from the IMAP URI in {@link AuthPreferences}
     * and the {@link TlsTrustPolicy} resolved for this host via {@link PinnedCertStore}.
     * Constructs a {@link K9MailTransport} from that config. The k-9
     * {@code MessagingException} from the K9MailTransport constructor is wrapped in a
     * {@link MailException} so the engine-side seam only sees app-owned exception types.
     *
     * <p>Per AC-1 / IC-1: this is the sole construction point for {@link MailTransport}
     * in the engine. BackupTask and RestoreTask obtain their transport through this seam —
     * never by constructing a {@code BackupImapStore} or {@code K9MailTransport} directly.
     *
     * @throws MailException if the URI is invalid, or if K9MailTransport construction fails
     */
    protected MailTransport getMailTransport() throws MailException {
        final String uri = getAuthPreferences().getStoreUri();
        if (!com.zegoggles.smssync.mail.BackupImapStore.isValidUri(uri)) {
            throw new MailException("No valid IMAP URI: " + uri);
        }

        // Resolve host and port from the URI for per-host certificate lookup.
        final Uri parsed = Uri.parse(uri);
        final String host = parsed.getHost();
        final int port = parsed.getPort();

        // 1. Resolve the TLS trust policy for this host:port via PinnedCertStore.
        //    SYSTEM_VALIDATED is the unconditional default when no cert is enrolled.
        final PinnedCertStore pinnedCertStore = new PinnedCertStore(getApplicationContext());
        final TlsTrustPolicy policy = pinnedCertStore.getTlsTrustPolicy(host, port);

        // 2. Build MailTransportConfig — app-owned, no k-9 types.
        final MailTransportConfig config;
        if (policy == TlsTrustPolicy.PINNED_CERTIFICATE) {
            config = new MailTransportConfig(uri, policy, pinnedCertStore.get(host, port));
        } else {
            config = new MailTransportConfig(uri, policy);
        }

        // 3. Construct K9MailTransport — constructor throws only MailException (CNTR-007 Clause C-1).
        return new K9MailTransport(getApplicationContext(), config);
    }

    /**
     * U-022: Returns the @Inject-supplied singleton when Hilt injection has run
     * (production path via @AndroidEntryPoint in U-023). Falls back to on-demand construction
     * for Robolectric tests that create anonymous service subclasses without @AndroidEntryPoint
     * Hilt lifecycle (AC-10 coexistence: existing tests must not be broken).
     * injectedAuthPreferences is null only when @AndroidEntryPoint has not yet been applied
     * (bootstrap coexistence period); in U-023 production, it is always non-null.
     */
    protected AuthPreferences getAuthPreferences() {
        return injectedAuthPreferences != null ? injectedAuthPreferences : new AuthPreferences(this);
    }

    /**
     * U-022: Returns the @Inject-supplied singleton when Hilt injection has run
     * (production path via @AndroidEntryPoint in U-023). Falls back to on-demand construction
     * for Robolectric tests that create anonymous service subclasses without @AndroidEntryPoint
     * Hilt lifecycle (AC-10 coexistence: existing tests must not be broken).
     * injectedPreferences is null only when @AndroidEntryPoint has not yet been applied
     * (bootstrap coexistence period); in U-023 production, it is always non-null.
     */
    protected Preferences getPreferences() {
        return injectedPreferences != null ? injectedPreferences : new Preferences(getApplicationContext());
    }

    protected synchronized void acquireLocks() {
        if (wakeLock == null) {
            PowerManager pMgr = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pMgr.newWakeLock(wakeLockType(), "com.zegoggles.smssync:"+TAG);
        }
        wakeLock.acquire(10*60*1000L /*10 minutes*/);

        if (isConnectedViaWifi()) {
            // we have Wifi, lock it
            WifiManager wMgr = getWifiManager();
            if (wifiLock == null) {
                wifiLock = wMgr.createWifiLock(getWifiLockType(), TAG);
            }
            wifiLock.acquire();
        }
    }

    protected int wakeLockType() {
        return PowerManager.PARTIAL_WAKE_LOCK;
    }

    @SuppressWarnings("deprecation")
    private int getWifiLockType() {
        return WifiManager.WIFI_MODE_FULL_HIGH_PERF;
    }

    protected synchronized void releaseLocks() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            wifiLock = null;
        }
    }

    protected boolean isBackgroundTask() {
        return false;
    }

    protected abstract void handleIntent(final Intent intent);

    protected void appLog(int id, Object... args) {
        final String msg = getString(id, args);
        if (appLog != null) {
            appLog.append(msg);
        } else if (LOCAL_LOGV) {
            Log.d(App.TAG, "AppLog: "+msg);
        }
    }

    protected void appLogDebug(String message, Object... args) {
        if (getPreferences().isAppLogDebug() && appLog != null) {
            appLog.append(String.format(ENGLISH, message, args));
        } else if (LOCAL_LOGV) {
            Log.v(App.TAG, "AppLog: "+String.format(ENGLISH, message, args));
        }
    }

    NotificationManager getNotifier() {
        return (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    }

    protected ConnectivityManager getConnectivityManager() {
        return (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    protected WifiManager getWifiManager() {
        return (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @SuppressWarnings("deprecation")
    @NonNull NotificationCompat.Builder createNotification(int resId) {
        return new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_notification)
            .setTicker(getString(resId))
            .setChannelId(CHANNEL_ID)
            .setWhen(System.currentTimeMillis())
            .setOngoing(true);
    }

    PendingIntent getPendingIntent(@Nullable Bundle extras) {
        final Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        if (extras != null) {
            intent.putExtras(extras);
        }
         return PendingIntent.getActivity(getApplicationContext(),
                 0,
                 intent,
                 FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    boolean isConnectedViaWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return isConnectedViaWifi_SDK21();
        } else {
            return isConnectedViaWifi_pre_SDK21();
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isConnectedViaWifi_pre_SDK21() {
        WifiManager wifiManager = getWifiManager();
        return (wifiManager != null &&
                wifiManager.isWifiEnabled() &&
                getConnectivityManager().getNetworkInfo(TYPE_WIFI) != null &&
                getConnectivityManager().getNetworkInfo(TYPE_WIFI).isConnected());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("deprecation")
    private boolean isConnectedViaWifi_SDK21() {
        for (Network network : getConnectivityManager().getAllNetworks()) {
            final android.net.NetworkInfo networkInfo = getConnectivityManager().getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == TYPE_WIFI && networkInfo.isConnectedOrConnecting()) {
                return true;
            }
        }
        return false;
    }
}
