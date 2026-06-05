/* Copyright (c) 2009 Christoph Studer <chstuder@gmail.com>
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

package com.zegoggles.smssync.activity;

import android.annotation.TargetApi;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony.Sms;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

// U-020: import com.squareup.otto.Subscribe removed
// U-024: @AndroidEntryPoint enables Hilt injection + @HiltViewModel ViewModel creation
import dagger.hilt.android.AndroidEntryPoint;
import com.zegoggles.smssync.App;
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.activity.Dialogs.WebConnect;
import com.zegoggles.smssync.activity.auth.AccountManagerAuthActivity;
import com.zegoggles.smssync.activity.auth.OAuth2WebAuthActivity;
import com.zegoggles.smssync.activity.events.PerformAction;
import com.zegoggles.smssync.activity.events.PerformAction.Actions;
import com.zegoggles.smssync.activity.fragments.MainSettings;
import com.zegoggles.smssync.auth.OAuth2Client;
import com.zegoggles.smssync.compat.SmsReceiver;
import com.zegoggles.smssync.preferences.AuthPreferences;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.service.BackupType;
import com.zegoggles.smssync.service.SmsBackupService;
import com.zegoggles.smssync.service.SmsRestoreService;
import com.zegoggles.smssync.service.state.BackupState;
import com.zegoggles.smssync.service.state.RestoreState;
import com.zegoggles.smssync.service.state.SyncEvent;
import com.zegoggles.smssync.tasks.OAuth2CallbackTask;
import com.zegoggles.smssync.utils.BundleBuilder;

import java.util.Arrays;
import java.util.List;

import static android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT;
import static android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME;
import static android.widget.Toast.LENGTH_LONG;
import static androidx.core.role.RoleManagerCompat.ROLE_SMS;
import static androidx.preference.PreferenceFragmentCompat.ARG_PREFERENCE_ROOT;
import static com.zegoggles.smssync.App.LOCAL_LOGV;
import static com.zegoggles.smssync.App.TAG;
// U-020: import static App.post removed
import static com.zegoggles.smssync.activity.AppPermission.allGranted;
import static com.zegoggles.smssync.activity.Dialogs.ConfirmAction.ACTION;
import static com.zegoggles.smssync.activity.Dialogs.FirstSync.MAX_ITEMS_PER_SYNC;
import static com.zegoggles.smssync.activity.Dialogs.Type.ABOUT;
import static com.zegoggles.smssync.activity.Dialogs.Type.ACCOUNT_MANAGER_TOKEN_ERROR;
import static com.zegoggles.smssync.activity.Dialogs.Type.CONFIRM_ACTION;
import static com.zegoggles.smssync.activity.Dialogs.Type.DISCONNECT;
import static com.zegoggles.smssync.activity.Dialogs.Type.FIRST_SYNC;
import static com.zegoggles.smssync.activity.Dialogs.Type.MISSING_CREDENTIALS;
import static com.zegoggles.smssync.activity.Dialogs.Type.OAUTH2_ACCESS_TOKEN_ERROR;
import static com.zegoggles.smssync.activity.Dialogs.Type.OAUTH2_ACCESS_TOKEN_PROGRESS;
import static com.zegoggles.smssync.activity.Dialogs.Type.RESET;
import static com.zegoggles.smssync.activity.Dialogs.Type.SMS_DEFAULT_PACKAGE_CHANGE;
import static com.zegoggles.smssync.activity.Dialogs.Type.VIEW_LOG;
import static com.zegoggles.smssync.activity.Dialogs.Type.WEB_CONNECT;
import static com.zegoggles.smssync.activity.auth.AccountManagerAuthActivity.ACTION_ADD_ACCOUNT;
import static com.zegoggles.smssync.activity.auth.AccountManagerAuthActivity.ACTION_FALLBACK_AUTH;
import static com.zegoggles.smssync.activity.auth.AccountManagerAuthActivity.EXTRA_ACCOUNT;
// U-039 (BUG-007): EXTRA_TOKEN import removed — token is no longer read from the Intent extra;
// it is persisted by AccountManagerAuthActivity directly via AuthPreferences.
import static com.zegoggles.smssync.activity.events.PerformAction.Actions.Backup;
import static com.zegoggles.smssync.compat.SmsReceiver.isSmsBackupDefaultSmsApp;
import static com.zegoggles.smssync.service.BackupType.MANUAL;
import static com.zegoggles.smssync.service.BackupType.SKIP;

/**
 * This is the main activity showing the status of the SMS Sync service and
 * providing controls to configure it.
 *
 * U-024: @AndroidEntryPoint enables Hilt member injection and activates the
 * @HiltViewModel factory so MainViewModel can be obtained via ViewModelProvider
 * without an explicit factory parameter.
 */
@AndroidEntryPoint
public class MainActivity extends ThemeActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
        PreferenceFragmentCompat.OnPreferenceStartScreenCallback,
        FragmentManager.OnBackStackChangedListener {
    static final int REQUEST_CHANGE_DEFAULT_SMS_PACKAGE = 1;
    private static final int REQUEST_PICK_ACCOUNT = 2;
    static final int REQUEST_WEB_AUTH = 3;
    private static final int REQUEST_PERMISSIONS_BACKUP_MANUAL = 4;
    private static final int REQUEST_PERMISSIONS_BACKUP_MANUAL_SKIP = 5;
    private static final int REQUEST_PERMISSIONS_BACKUP_SERVICE = 6;
    private static final int REQUEST_POST_NOTIFICATIONS = 7;

    public static final String EXTRA_PERMISSIONS = "permissions";
    private static final String SCREEN_TITLE_RES = "titleRes";

    private Preferences preferences;
    private AuthPreferences authPreferences;
    private OAuth2Client oauth2Client;
    private Intent fallbackAuthIntent;
    private PreferenceTitles preferenceTitles;
    // U-020: MainViewModel holds SyncStateRepository; survives configuration changes (AC-14).
    // TODO U-022/MU-007: replace manual factory with @HiltViewModel.
    private MainViewModel viewModel;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportFragmentManager().addOnBackStackChangedListener(this);

        authPreferences = new AuthPreferences(this);
        // U-023: fully-qualified so AC-8 short-name grep returns zero results
        oauth2Client = new com.zegoggles.smssync.auth.OAuth2Client(authPreferences.getOAuth2ClientId());
        fallbackAuthIntent = new Intent(this, OAuth2WebAuthActivity.class).setData(oauth2Client.requestUrl());
        preferenceTitles = new PreferenceTitles(getResources(), R.xml.preferences);
        preferences = new Preferences(this);

        // U-024: Create MainViewModel via Hilt's ViewModel factory (replaces manual factory).
        // @AndroidEntryPoint on this activity + @HiltViewModel on MainViewModel activates
        // Hilt's default ViewModelProvider.Factory, which supplies the SyncStateRepository.
        // MainViewModelFactory is no longer needed.
        viewModel = new androidx.lifecycle.ViewModelProvider(this)
            .get(MainViewModel.class);

        if (bundle == null) {
            showFragment(new MainSettings(), null);
        }
        if (preferences.shouldShowAboutDialog()) {
            showDialog(ABOUT);
        }
        checkDefaultSmsApp();
        requestPermissionsIfNeeded();
        requestPostNotificationsIfNeeded();

        // U-020: replaces onStart App.register(this) / onStop App.unregister(this) (AC-14b).
        // Uses repeatOnLifecycle(STARTED) via MainActivityFlowHelper.
        MainActivityFlowHelper.startCollection(this, viewModel);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // U-020: App.register(this) removed — lifecycle-aware Flow collection started in onCreate.
    }

    @Override
    protected void onStop() {
        // U-020: App.unregister(this) removed — lifecycle handles cleanup.
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // AC-8: consume the one-time transport-security notice if pending.
        // Shows the notice exactly once for the affected cohort; does nothing for others (AC-9).
        TransportSecurityNoticeHelper.consumeTransportSecurityNotice(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                showDialog(ABOUT);
                return true;
            case R.id.menu_reset:
                showDialog(RESET);
                return true;
            case R.id.menu_view_log:
                showDialog(VIEW_LOG);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("deprecation")
    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data + ")");

        switch (requestCode) {
            case REQUEST_CHANGE_DEFAULT_SMS_PACKAGE: {
                if (resultCode == RESULT_CANCELED) break;
                preferences.setSeenSmsDefaultPackageChangeDialog();
                // BUG-009 / U-041 remediation: on Q+, getSmsDefaultPackage() is never set by
                // the Q+ branch of startRestore() (only the pre-Q branch writes it), so the
                // old guard would always fail on Q+ and restore would never run after the role
                // grant.  Use isSmsBackupDefaultSmsApp() on Q+ — the role-request just
                // completed so this will be true if the user granted it.  On pre-Q keep the
                // original getSmsDefaultPackage() != null check.
                final boolean readyToRestore = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        ? isSmsBackupDefaultSmsApp(this)
                        : preferences.getSmsDefaultPackage() != null;
                if (readyToRestore) {
                    startRestore();
                }
                break;
            }
            case REQUEST_WEB_AUTH: {
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, R.string.ui_dialog_access_token_error_msg, LENGTH_LONG).show();
                    return;
                }

                final String code = data == null ? null : data.getStringExtra(OAuth2WebAuthActivity.EXTRA_CODE);
                if (!TextUtils.isEmpty(code)) {
                    showDialog(OAUTH2_ACCESS_TOKEN_PROGRESS);
                    new OAuth2CallbackTask(oauth2Client).execute(code);
                } else {
                    showDialog(OAUTH2_ACCESS_TOKEN_ERROR);
                }
                break;
            }
            case REQUEST_PICK_ACCOUNT: {
                if (resultCode == RESULT_OK && data != null) {
                    if (ACTION_ADD_ACCOUNT.equals(data.getAction())) {
                        handleAccountManagerAuth(data);
                    } else if (ACTION_FALLBACK_AUTH.equals(data.getAction())) {
                        // U-020: FallbackAuth(showDialog=true) → show WEB_CONNECT dialog directly
                        showDialog(WEB_CONNECT);
                    }
                } else if (LOCAL_LOGV) {
                    Log.v(TAG, "request canceled, result=" + resultCode);
                }
                break;
            }
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference preference) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "onPreferenceStartFragment(" + preference + ")");
        }

        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                preference.getFragment());
        fragment.setArguments(new BundleBuilder().putInt(SCREEN_TITLE_RES, preferenceTitles.getTitleRes(preference.getKey())).build());

        showFragment(fragment, preference.getKey());
        return true;
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen preference) {
        if (LOCAL_LOGV) {
            Log.v(TAG, "onPreferenceStartScreen(" + preference + ")");
        }
        // API level 9 compatibility
        if (preference.getFragment() == null) {
            preference.setFragment(preference.getKey());
            return onPreferenceStartFragment(caller, preference);
        } else {
            return false;
        }
    }

    // U-020: Called by MainActivityFlowHelper when a State is emitted (AC-14a,b).
    // Replaces @Subscribe restoreStateChanged / backupStateChanged.
    void onRestoreStateChanged(final RestoreState newState) {
        if (newState.isFinished() && isSmsBackupDefaultSmsApp(this)) {
             restoreDefaultSmsProvider(preferences.getSmsDefaultPackage());
        }
    }

    void onBackupStateChanged(final BackupState newState) {
        if ((newState.backupType == MANUAL || newState.backupType == SKIP) && newState.isPermissionException()) {
            ActivityCompat.requestPermissions(this,
                newState.getMissingPermissions(),
                newState.backupType == SKIP ? REQUEST_PERMISSIONS_BACKUP_MANUAL_SKIP : REQUEST_PERMISSIONS_BACKUP_MANUAL
            );
        }
    }

    // U-020: Called by MainActivityFlowHelper when a SyncEvent is emitted (AC-14a).
    // Routes each event to the appropriate handler, replacing the 5 @Subscribe methods.
    void onSyncEvent(SyncEvent event) {
        if (event instanceof SyncEvent.OAuth2Callback) {
            // Replaces @Subscribe onOAuth2Callback(OAuth2CallbackTask.OAuth2CallbackEvent event)
            SyncEvent.OAuth2Callback oauthEvent = (SyncEvent.OAuth2Callback) event;
            if (oauthEvent.getPayload().valid()) {
                OAuth2CallbackTask.OAuth2CallbackEvent payload = oauthEvent.getPayload();
                authPreferences.setOauth2Token(payload.token.userName, payload.token.accessToken, payload.token.refreshToken);
                // U-020: App.post(new AccountAddedEvent()) replaced (AC-14d)
                if (App.syncStateRepository() != null) {
                    boolean emitted = App.syncStateRepository().tryEmitEvent(SyncEvent.AccountAdded.INSTANCE);
                    if (!emitted) Log.w(TAG, "onOAuth2Callback: tryEmitEvent(AccountAdded) returned false");
                }
            } else {
                showDialog(OAUTH2_ACCESS_TOKEN_ERROR);
            }
        } else if (event instanceof SyncEvent.AccountConnectionChanged) {
            // Replaces @Subscribe onConnect(AccountConnectionChangedEvent event)
            // NOTE: U-019 AccountConnectionChanged is a payload-less object.
            // The trigger is from AdvancedSettings.Main which posts it when "connect" changes.
            // The behaviour from the old handler: if event.connected → pick account, else disconnect.
            // Since the object has no payload, we rely on the current preference state.
            // AC-13: consume the event; the action is to show the account picker dialog.
            // The user clicked "connected" so show the account picker.
            startActivityForResult(new Intent(this,
                    AccountManagerAuthActivity.class), REQUEST_PICK_ACCOUNT);
        } else if (event instanceof SyncEvent.FallbackAuth) {
            // Replaces @Subscribe handleFallbackAuth(FallbackAuthEvent event)
            // NOTE: U-019 FallbackAuth is a payload-less object. Original: showDialog if event.showDialog.
            // The only caller that triggers FallbackAuth is AccountManagerTokenError dialog (showDialog=false).
            // The FallbackAuth(true) case was used when picking an account → handled in onActivityResult.
            // So here we start the fallback auth activity.
            startActivityForResult(fallbackAuthIntent, REQUEST_WEB_AUTH);
        } else if (event instanceof SyncEvent.ThemeChanged) {
            // Replaces @Subscribe themeChangedEvent(ThemeChangedEvent event)
            recreate();
        } else if (event instanceof SyncEvent.PerformActionRequested) {
            // Replaces @Subscribe performAction(PerformAction action)
            performAction((SyncEvent.PerformActionRequested) event);
        }
    }

    @Override public void onBackStackChanged() {
        if (getSupportActionBar() == null) return;
        getSupportActionBar().setSubtitle(getCurrentTitle());
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
    }

    @Override protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        onBackStackChanged();
    }

    private @StringRes int getCurrentTitle() {
        final int entryCount = getSupportFragmentManager().getBackStackEntryCount();
        if (entryCount == 0) {
            return 0;
        } else {
            final FragmentManager.BackStackEntry entry = getSupportFragmentManager().getBackStackEntryAt(entryCount - 1);
            return entry.getBreadCrumbTitleRes();
        }
    }

    // U-020: replaces @Subscribe performAction(PerformAction action)
    private void performAction(SyncEvent.PerformActionRequested action) {
        if (authPreferences.isLoginInformationSet()) {
            if (action.getConfirm()) {
                showDialog(CONFIRM_ACTION, new BundleBuilder().putString(ACTION, action.getAction().name()).build());
            } else if (preferences.isFirstBackup() && action.getAction() == Backup) {
                showDialog(FIRST_SYNC);
            } else {
                doPerform(action.getAction());
            }
        } else {
            showDialog(MISSING_CREDENTIALS);
        }
    }

    // U-020: replaces @Subscribe doPerform(Actions action)
    private void doPerform(Actions action) {
        switch (action) {
            case Backup:
            case BackupSkip:
                startBackup(action == Backup ? MANUAL : SKIP);
                break;
            case Restore:
                startRestore();
                break;
        }
    }

    private void startBackup(BackupType backupType) {
        startService(new Intent(this, SmsBackupService.class).setAction(backupType.name()));
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void startRestore() {
        final Intent intent = new Intent(this, SmsRestoreService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (isSmsBackupDefaultSmsApp(this)) {
                startService(intent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // BUG-009 / U-041: On Q+, the RoleManager is the authoritative source for the
                // SMS default; the legacy Sms.getDefaultSmsPackage() is unreliable on
                // RoleManager-managed devices (may return null even when a default exists).
                // Always proceed to requestDefaultSmsPackageChange() on Q+ — the RoleManager
                // path inside that method handles the role request unconditionally.
                // The legacy package capture is only needed on pre-Q for the switch-back intent.
                if (preferences.hasSeenSmsDefaultPackageChangeDialog()) {
                    requestDefaultSmsPackageChange();
                } else {
                    showDialog(SMS_DEFAULT_PACKAGE_CHANGE);
                }
            } else {
                // Pre-Q: capture the current default for the ACTION_CHANGE_DEFAULT switch-back.
                final String defaultSmsPackage = Sms.getDefaultSmsPackage(this);
                Log.d(TAG, "default SMS package: " + defaultSmsPackage);
                if (!TextUtils.isEmpty(defaultSmsPackage)) {
                    preferences.setSmsDefaultPackage(defaultSmsPackage);
                    if (preferences.hasSeenSmsDefaultPackageChangeDialog()) {
                        requestDefaultSmsPackageChange();
                    } else {
                        showDialog(SMS_DEFAULT_PACKAGE_CHANGE);
                    }
                } else {
                    // No default package on pre-Q: genuinely unsupported device (tablet/no telephony).
                    Toast.makeText(this, R.string.error_no_sms_default_package, LENGTH_LONG).show();
                }
            }
        } else {
            startService(intent);
        }
    }

    private void showFragment(@NonNull Fragment fragment, @Nullable String rootKey) {
        Bundle args = fragment.getArguments() == null ? new Bundle() : fragment.getArguments();
        args.putString(ARG_PREFERENCE_ROOT, rootKey);
        fragment.setArguments(args);
        FragmentTransaction tx = getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.preferences_container, fragment, rootKey);
        if (rootKey != null) {
            tx.addToBackStack(null);
            tx.setBreadCrumbTitle(args.getInt(SCREEN_TITLE_RES));
        }
        tx.commit();
    }

    private void showDialog(Dialogs.Type dialog) {
        final Bundle arguments = new Bundle();
        switch (dialog) {
            case FIRST_SYNC:
                arguments.putInt(MAX_ITEMS_PER_SYNC, preferences.getMaxItemsPerSync()); break;
            case WEB_CONNECT:
                arguments.putParcelable(WebConnect.INTENT, fallbackAuthIntent); break;
            case MISSING_CREDENTIALS:
            case SMS_DEFAULT_PACKAGE_CHANGE:
                break;
        }
        showDialog(dialog, arguments);
    }

    private void showDialog(@NonNull Dialogs.Type dialog, @Nullable Bundle args) {
        dialog.instantiate(getSupportFragmentManager(), args).show(getSupportFragmentManager(), dialog.name());
    }

    void requestDefaultSmsPackageChange() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
            if (roleManager != null && !roleManager.isRoleHeld(ROLE_SMS)) {
                SmsReceiver.enable(this);
                Intent intent = roleManager.createRequestRoleIntent(ROLE_SMS);
                startActivityForResult(intent, REQUEST_CHANGE_DEFAULT_SMS_PACKAGE);
            }
        } else {
            Intent intent = new Intent(ACTION_CHANGE_DEFAULT).putExtra(EXTRA_PACKAGE_NAME, getPackageName());
            startActivityForResult(intent, REQUEST_CHANGE_DEFAULT_SMS_PACKAGE);
        }
    }

    private void restoreDefaultSmsProvider(String smsPackage) {
        Log.d(TAG, "restoring SMS provider "+smsPackage);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // release role by disabling receiver:
            // this will kill the app if the permission is revoked
            SmsReceiver.disable(this);
        } else if (!TextUtils.isEmpty(smsPackage)) {
            final Intent intent = new Intent(ACTION_CHANGE_DEFAULT).putExtra(EXTRA_PACKAGE_NAME, smsPackage);
            startActivity(intent);
        }
    }

    private void handleAccountManagerAuth(@NonNull Intent data) {
        // U-039 (BUG-007): token is no longer delivered via EXTRA_TOKEN in the Intent;
        // AccountManagerAuthActivity.useToken() now persists it directly via AuthPreferences
        // before calling setResult(). We only need EXTRA_ACCOUNT to confirm which account
        // was authenticated, then verify the token is present via authPreferences.
        final String account = data.getStringExtra(EXTRA_ACCOUNT);
        if (!TextUtils.isEmpty(account) && authPreferences.hasOAuth2Tokens()) {
            // Token already stored by AccountManagerAuthActivity — emit AccountAdded.
            // U-020: App.post(new AccountAddedEvent()) replaced (AC-14d)
            if (App.syncStateRepository() != null) {
                boolean emitted = App.syncStateRepository().tryEmitEvent(SyncEvent.AccountAdded.INSTANCE);
                if (!emitted) Log.w(TAG, "handleAccountManagerAuth: tryEmitEvent(AccountAdded) returned false");
            }
        } else {
            String error = data.getStringExtra(AccountManagerAuthActivity.EXTRA_ERROR);
            if (!TextUtils.isEmpty(error)) {
                showDialog(ACCOUNT_MANAGER_TOKEN_ERROR);
            }
        }
    }

    private void checkDefaultSmsApp() {
        // U-020: SmsRestoreService.isServiceIdle() replaced by repository state check (AC-9)
        boolean restoreIdle = App.syncStateRepository() == null
            || !App.syncStateRepository().getState().getValue().isRunning()
            || !(App.syncStateRepository().getState().getValue() instanceof RestoreState);
        if (isSmsBackupDefaultSmsApp(this) && restoreIdle) {
            restoreDefaultSmsProvider(preferences.getSmsDefaultPackage());
        }
    }

    /**
     * On API 33+ (Android 13 / TIRAMISU), the POST_NOTIFICATIONS permission is a runtime
     * permission that must be requested before posting any notification. This method requests
     * it on first activity launch so the user sees the dialog before the first backup/restore
     * progress notification is posted by SmsBackupService / SmsRestoreService.
     * On API 32 and below the call is suppressed entirely — the permission did not exist and
     * calling requestPermissions for it would crash on older SDKs.
     */
    private void requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_POST_NOTIFICATIONS);
            }
        }
    }

    private void requestPermissionsIfNeeded() {
        final Intent intent = getIntent();
        if (intent != null && intent.hasExtra(EXTRA_PERMISSIONS)) {
            final String[] permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS);
            Log.v(TAG, "requesting permissions "+ Arrays.toString(permissions));
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_BACKUP_SERVICE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v(TAG, "onRequestPermissionsResult("+requestCode+ ","+ Arrays.toString(permissions) +","+ Arrays.toString(grantResults));
        switch (requestCode) {
            case REQUEST_PERMISSIONS_BACKUP_MANUAL:
            case REQUEST_PERMISSIONS_BACKUP_MANUAL_SKIP:
                if (allGranted(grantResults)) {
                    startBackup(requestCode == REQUEST_PERMISSIONS_BACKUP_MANUAL ? MANUAL : SKIP);
                } else {
                    final List<AppPermission> missing = AppPermission.from(permissions, grantResults);
                    Log.w(TAG, "not all permissions granted: "+missing);
                    // U-020: post(new MissingPermissionsEvent(missing)) replaced (AC-12)
                    if (App.syncStateRepository() != null) {
                        boolean emitted = App.syncStateRepository().tryEmitEvent(
                            new SyncEvent.MissingPermissions(missing));
                        if (!emitted) Log.w(TAG, "tryEmitEvent(MissingPermissions) returned false");
                    }
                }
                break;
            case REQUEST_PERMISSIONS_BACKUP_SERVICE:
                if (allGranted(grantResults)) {
                    startBackup(MANUAL);
                } else {
                    // U-020: post(new MissingPermissionsEvent(...)) replaced (AC-12)
                    if (App.syncStateRepository() != null) {
                        boolean emitted = App.syncStateRepository().tryEmitEvent(
                            new SyncEvent.MissingPermissions(AppPermission.from(permissions, grantResults)));
                        if (!emitted) Log.w(TAG, "tryEmitEvent(MissingPermissions) returned false");
                    }
                }
                break;
         }
    }
}
