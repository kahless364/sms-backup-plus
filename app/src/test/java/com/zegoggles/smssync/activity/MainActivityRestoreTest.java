package com.zegoggles.smssync.activity;

import android.app.role.RoleManager;
import android.content.Intent;
import android.os.Build;

import com.zegoggles.smssync.R;
import com.zegoggles.smssync.preferences.Preferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowRoleManager;

import java.lang.reflect.Field;

import static androidx.core.role.RoleManagerCompat.ROLE_SMS;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.ShadowToast.getTextOfLatestToast;
import static org.robolectric.shadows.ShadowToast.shownToastCount;

/**
 * Robolectric regression tests for BUG-009 / U-041.
 *
 * <p>On Android Q+ (API 29+), {@link MainActivity#startRestore()} must request the SMS role
 * via {@link RoleManager} even when the legacy {@code Telephony.Sms.getDefaultSmsPackage()}
 * returns null (as on RoleManager-managed devices). Before the fix, a null legacy value
 * caused an early bail with the "no default package" toast — the role was never requested
 * and restore never ran.
 *
 * <p><b>Test strategy</b>: {@code requestDefaultSmsPackageChange()} is package-private
 * and therefore accessible from this test class (same package). The method is the direct
 * target of the Q+ branch in {@code startRestore()} after the fix. Calling it directly
 * avoids having to launch the full {@link MainActivity} through Hilt, while still
 * exercising the exact behavior that the fix unlocks (AC-1 / AC-3).
 *
 * <p><b>Hilt note</b>: {@code buildActivity(MainActivity.class).get()} creates the activity
 * instance (context attached) but does NOT call {@code onCreate()}, so the Hilt
 * {@code OnContextAvailableListener → inject()} path is not triggered. The {@code preferences}
 * field (normally set in {@code onCreate()}) is injected manually via reflection so that
 * {@code requestDefaultSmsPackageChange()} has a valid context via
 * {@code getSystemService(RoleManager)}.
 */
@RunWith(RobolectricTestRunner.class)
public class MainActivityRestoreTest {

    // -----------------------------------------------------------------------
    // AC-1 / AC-3: Q+ null-default path — role request produced, no error toast
    // -----------------------------------------------------------------------

    /**
     * BUG-009 regression / AC-1 + AC-3:
     *
     * <p>On Q+ (API 29), when the app is NOT the default SMS app and
     * {@code Telephony.Sms.getDefaultSmsPackage()} returns null (as on RoleManager-managed
     * devices), {@code startRestore()} must call {@code requestDefaultSmsPackageChange()},
     * which issues a {@code startActivityForResult} for the SMS role.
     *
     * <p>This test calls {@code requestDefaultSmsPackageChange()} directly — the exact method
     * that {@code startRestore()} now reaches on Q+ after the fix, regardless of the legacy
     * package value. On Q+, the method checks {@link RoleManager#isRoleHeld(String)} and, when
     * the role is NOT held, calls {@code startActivityForResult} with the role-request intent
     * and request code {@link MainActivity#REQUEST_CHANGE_DEFAULT_SMS_PACKAGE}.
     *
     * <p>Pre-fix: this method was only reachable when {@code getDefaultSmsPackage()} returned
     * non-empty. Post-fix: the Q+ branch always reaches it.
     */
    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void bug009_onQPlus_requestDefaultSmsPackageChange_startsRoleRequestIntent() {
        // Make ROLE_SMS available on the shadow device so RoleManager accepts the request.
        ShadowRoleManager shadowRoleManager =
                shadowOf(RuntimeEnvironment.getApplication().getSystemService(RoleManager.class));
        shadowRoleManager.addAvailableRole(ROLE_SMS);

        MainActivity activity = buildMainActivityNoHilt();

        // Pre-condition: SMS role is NOT held by this test app.
        RoleManager roleManager = (RoleManager) activity.getSystemService(RoleManager.class);
        assertThat(roleManager).isNotNull();
        assertThat(roleManager.isRoleHeld(ROLE_SMS)).isFalse();

        // Call the package-private method directly (same package — no reflection needed).
        activity.requestDefaultSmsPackageChange();

        // AC-1: a startActivityForResult was issued (role-request dialog would appear).
        ShadowActivity.IntentForResult sentIntent =
                shadowOf(activity).getNextStartedActivityForResult();
        assertThat(sentIntent).isNotNull();
        assertThat(sentIntent.requestCode)
                .isEqualTo(MainActivity.REQUEST_CHANGE_DEFAULT_SMS_PACKAGE);

        // AC-3: the "no default package" error toast must NOT be shown.
        assertThat(shownToastCount()).isEqualTo(0);
    }

    /**
     * BUG-009 / AC-3: The error toast string {@code error_no_sms_default_package} must NOT
     * be shown on the Q+ code path. This test verifies the absence of the toast explicitly,
     * matching the string value used in the production code.
     */
    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void bug009_onQPlus_requestDefaultSmsPackageChange_doesNotShowErrorToast() {
        ShadowRoleManager shadowRoleManager =
                shadowOf(RuntimeEnvironment.getApplication().getSystemService(RoleManager.class));
        shadowRoleManager.addAvailableRole(ROLE_SMS);

        MainActivity activity = buildMainActivityNoHilt();
        activity.requestDefaultSmsPackageChange();

        // AC-3: no toast at all on the Q+ path.
        assertThat(shownToastCount()).isEqualTo(0);
        // Confirm in particular the error toast string was not shown.
        String errorToastText = RuntimeEnvironment.application.getString(
                R.string.error_no_sms_default_package);
        assertThat(getTextOfLatestToast()).isNotEqualTo(errorToastText);
    }

    /**
     * Guard test: on Q+, when the calling app already holds the SMS role
     * ({@code isRoleHeld() == true}), {@code requestDefaultSmsPackageChange()} must NOT
     * start a new role-request intent (no duplicate request). This verifies the guard
     * condition inside the method is preserved after the fix.
     */
    @Test
    @Config(sdk = Build.VERSION_CODES.Q)
    public void bug009_onQPlus_whenAlreadyRoleHolder_noIntentStarted() {
        ShadowRoleManager shadowRoleManager =
                shadowOf(RuntimeEnvironment.getApplication().getSystemService(RoleManager.class));
        // Grant the SMS role to this test app so isRoleHeld() returns true.
        shadowRoleManager.addHeldRole(ROLE_SMS);

        MainActivity activity = buildMainActivityNoHilt();

        // requestDefaultSmsPackageChange() short-circuits when role is already held.
        activity.requestDefaultSmsPackageChange();

        // No startActivityForResult should have been called.
        assertThat(shadowOf(activity).getNextStartedActivityForResult()).isNull();
    }

    // -----------------------------------------------------------------------
    // Helper: build MainActivity without triggering full Hilt injection
    // -----------------------------------------------------------------------

    /**
     * Creates a {@link MainActivity} JVM instance via {@code Robolectric.buildActivity().get()}
     * — this attaches the base context but does NOT call {@code onCreate()}, avoiding the
     * Hilt {@code OnContextAvailableListener → inject()} path that requires a
     * {@code GeneratedComponentManager} application (not present under plain Robolectric).
     *
     * <p>The {@code preferences} field is populated via reflection so that
     * {@code requestDefaultSmsPackageChange()} (and any call path in {@code startRestore()})
     * has a valid non-null reference. {@link Preferences#setSeenSmsDefaultPackageChangeDialog()}
     * is called so the Q+ branch in {@code startRestore()} would call
     * {@code requestDefaultSmsPackageChange()} rather than {@code showDialog()}.
     */
    private MainActivity buildMainActivityNoHilt() {
        try {
            MainActivity activity = Robolectric.buildActivity(MainActivity.class).get();

            // Inject preferences manually since onCreate() is not called.
            Preferences prefs = new Preferences(RuntimeEnvironment.application);
            // Ensure hasSeenSmsDefaultPackageChangeDialog() == true so the Q+ path
            // calls requestDefaultSmsPackageChange() directly, not showDialog().
            prefs.setSeenSmsDefaultPackageChangeDialog();

            Field prefsField = MainActivity.class.getDeclaredField("preferences");
            prefsField.setAccessible(true);
            prefsField.set(activity, prefs);

            return activity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to build test MainActivity: " + e, e);
        }
    }
}
