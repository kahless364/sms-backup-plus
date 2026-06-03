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
package com.zegoggles.smssync.activity;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.zegoggles.smssync.R;

/**
 * One-time transport-security notice presenter.
 *
 * <p>Reads the {@code "transport_security_notice_pending"} flag written by
 * {@code AuthPreferences.migrate()} for users in the affected cohort (legacy {@code +ssl}/
 * {@code +tls} protocol, or stale {@code SERVER_TRUST_ALL_CERTIFICATES=true} cleared by
 * the migration). Presents the notice exactly once and then writes the flag back to
 * {@code false} so it is never shown again.
 *
 * <p>This is the exclusive read-and-consume site for the pending flag per IC-2:
 * <ul>
 *   <li>Flag is read here (one location)</li>
 *   <li>Flag is written {@code false} (consumed) here (one location)</li>
 *   <li>Flag is written {@code true} (pending) only in {@code AuthPreferences.migrate()}</li>
 * </ul>
 *
 * <p>Analogous to the existing {@code sms_default_package_change_seen} one-time pattern
 * at {@code Preferences.java:247-253}.
 *
 * <p>CNTR-MODERNIZATION-002 §TransportSecurityNoticePending: the consumer may only flip
 * {@code transport_security_notice_pending} — it MUST NOT mutate the certificate store.
 */
public class TransportSecurityNoticeHelper {

    /**
     * The flag key written by {@code AuthPreferences} when the pending state is set.
     *
     * <p>This string value MUST match {@code AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING}
     * exactly. The constant is duplicated here rather than referenced directly because
     * {@code TRANSPORT_SECURITY_NOTICE_PENDING} is package-private in {@code AuthPreferences}
     * (intentionally: only the preferences package should write it; this class only reads/clears).
     */
    public static final String KEY = "transport_security_notice_pending";

    /**
     * Checks whether the one-time transport-security notice is pending and clears the flag.
     *
     * <p>This method is idempotent: if the flag is {@code false} or absent, it returns
     * {@code false} immediately without any side effects. If the flag is {@code true},
     * it immediately writes {@code false} (consumed) and returns {@code true} — the
     * caller is responsible for showing the notice UI.
     *
     * <p>Separation of flag-check from UI display makes the flag-lifecycle logic
     * testable without requiring a UI context.
     *
     * <p>CNTR-MODERNIZATION-002 Validation Rule 6: this method only reads/flips the
     * notice flag. It never calls {@code PinnedCertStore.store()} or any other method
     * that could affect TLS policy.
     *
     * @param context any application context (used to access default SharedPreferences)
     * @return {@code true} if the flag was pending (caller should show the notice),
     *         {@code false} if it was not pending (no action needed)
     */
    public static boolean checkAndClearNoticePending(@NonNull Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean(KEY, false)) {
            // Notice is not pending — do nothing (AC-9: unaffected users see nothing).
            return false;
        }

        // Mark as consumed BEFORE showing the dialog (prevents re-display if the user
        // kills the process from the dialog — notice is still "consumed").
        prefs.edit().putBoolean(KEY, false).apply();
        return true;
    }

    /**
     * Checks whether the one-time transport-security notice is pending and, if so,
     * clears the flag and presents the notice dialog to the user.
     *
     * <p>This is the production entry point called from {@code MainActivity.onResume()}.
     *
     * @param context an Activity context (required to build the AlertDialog)
     * @return {@code true} if the notice was shown (flag was pending), {@code false} otherwise
     */
    public static boolean consumeTransportSecurityNotice(@NonNull Context context) {
        if (!checkAndClearNoticePending(context)) {
            return false;
        }
        showNoticeDialog(context);
        return true;
    }

    /**
     * Builds and shows the one-time security notice dialog.
     *
     * <p>Content per CNTR-MODERNIZATION-002 §TransportSecurityNoticePending:
     * (a) the connection now uses certificate validation;
     * (b) self-hosted/private-CA users can enroll a pinned cert via settings;
     * (c) no action needed for Gmail or publicly-trusted servers.
     */
    private static void showNoticeDialog(@NonNull Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.ui_transport_security_notice_title)
                .setMessage(R.string.ui_transport_security_notice_message)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .show();
    }
}
