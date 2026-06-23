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
package com.zegoggles.smssync.activity

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.core.role.RoleManagerCompat.ROLE_SMS
import com.zegoggles.smssync.App.TAG
import com.zegoggles.smssync.R
import com.zegoggles.smssync.compat.SmsReceiver
import com.zegoggles.smssync.compat.SmsReceiver.isSmsBackupDefaultSmsApp
import com.zegoggles.smssync.preferences.Preferences
import com.zegoggles.smssync.service.state.RestoreState
import com.zegoggles.smssync.service.state.State

/**
 * U-050 AR-003: Helper that encapsulates SMS-default-role negotiation extracted from
 * [MainActivity].
 *
 * Handles the cohesive concern of requesting / releasing the SMS default role or the
 * pre-Q ACTION_CHANGE_DEFAULT intent, and restoring the original SMS app after restore.
 *
 * Behavior preserved:
 * - BUG-009 / U-041: Q+ path always goes through [requestDefaultSmsPackageChange];
 *   the legacy [Telephony.Sms.getDefaultSmsPackage] check is only used on pre-Q.
 * - U-034: dialog flow (SMS_DEFAULT_PACKAGE_CHANGE) is delegated back to [MainActivity]
 *   via the [DialogDelegate] callback so theming is preserved.
 * - Cooperative: methods accept the [MainActivity] instance only for the mandatory Android
 *   APIs (startActivityForResult, getSystemService, getPackageName) — no business logic
 *   leaks back into the Activity beyond what it already delegated.
 */
object SmsDefaultRoleHelper {

    /**
     * Callback interface so [SmsDefaultRoleHelper] can delegate dialog display back to
     * [MainActivity] (preserves U-034 themed-dialog path).
     */
    interface DialogDelegate {
        fun showSmsDefaultPackageChangeDialog()
        fun requestDefaultSmsPackageChange()
        fun startViewModelRestore()
    }

    /**
     * Starts the restore flow by negotiating the SMS default role / package, then
     * delegates to [DialogDelegate.startViewModelRestore] once the role is established.
     *
     * Extracted from [MainActivity.startRestore] — behavior preserved verbatim.
     *
     * BUG-009 / U-041: On Q+, always calls [requestDefaultSmsPackageChange] regardless of
     * the legacy [Telephony.Sms.getDefaultSmsPackage] value (unreliable on Q+).
     */
    @JvmStatic
    fun startRestore(activity: MainActivity, preferences: Preferences, delegate: DialogDelegate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (isSmsBackupDefaultSmsApp(activity)) {
                delegate.startViewModelRestore()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // BUG-009 / U-041: On Q+, always proceed to requestDefaultSmsPackageChange().
                if (preferences.hasSeenSmsDefaultPackageChangeDialog()) {
                    delegate.requestDefaultSmsPackageChange()
                } else {
                    delegate.showSmsDefaultPackageChangeDialog()
                }
            } else {
                @Suppress("DEPRECATION")
                val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(activity)
                Log.d(TAG, "default SMS package: $defaultSmsPackage")
                if (!TextUtils.isEmpty(defaultSmsPackage)) {
                    preferences.setSmsDefaultPackage(defaultSmsPackage)
                    if (preferences.hasSeenSmsDefaultPackageChangeDialog()) {
                        delegate.requestDefaultSmsPackageChange()
                    } else {
                        delegate.showSmsDefaultPackageChangeDialog()
                    }
                } else {
                    Toast.makeText(activity, R.string.error_no_sms_default_package, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // Pre-KitKat: no SMS default permission required.
            delegate.startViewModelRestore()
        }
    }

    /**
     * Requests the SMS default role (Q+) or sends ACTION_CHANGE_DEFAULT intent (pre-Q).
     * Extracted from [MainActivity.requestDefaultSmsPackageChange] — behavior preserved.
     */
    @JvmStatic
    fun requestDefaultSmsPackageChange(activity: MainActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as? RoleManager
            if (roleManager != null && !roleManager.isRoleHeld(ROLE_SMS)) {
                SmsReceiver.enable(activity)
                val intent = roleManager.createRequestRoleIntent(ROLE_SMS)
                activity.startActivityForResult(intent, MainActivity.REQUEST_CHANGE_DEFAULT_SMS_PACKAGE)
            }
        } else {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.packageName)
            activity.startActivityForResult(intent, MainActivity.REQUEST_CHANGE_DEFAULT_SMS_PACKAGE)
        }
    }

    /**
     * Restores the original SMS provider after restore completes.
     * Extracted from [MainActivity.restoreDefaultSmsProvider] — behavior preserved.
     *
     * On Q+: releases the SMS role by disabling [SmsReceiver].
     * On pre-Q: sends ACTION_CHANGE_DEFAULT to switch the default back to [smsPackage].
     */
    @JvmStatic
    fun restoreDefaultSmsProvider(activity: MainActivity, smsPackage: String?) {
        Log.d(TAG, "restoring SMS provider $smsPackage")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            SmsReceiver.disable(activity)
        } else if (!TextUtils.isEmpty(smsPackage)) {
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, smsPackage)
            activity.startActivity(intent)
        }
    }

    /**
     * Checks on startup whether this app is the default SMS app and, if restore is not
     * active, restores the previous default.
     *
     * Extracted from [MainActivity.checkDefaultSmsApp] — behavior preserved.
     * U-020: SmsRestoreService.isServiceIdle() replaced by repository state check (AC-9).
     */
    @JvmStatic
    fun checkDefaultSmsApp(activity: MainActivity, preferences: Preferences, currentState: State?) {
        val restoreIdle = currentState == null || !currentState.isRunning || currentState !is RestoreState
        if (isSmsBackupDefaultSmsApp(activity) && restoreIdle) {
            restoreDefaultSmsProvider(activity, preferences.getSmsDefaultPackage())
        }
    }
}
