---
artifact_type: plan
story_id: U-041
verdict: PASS
agent: Developer
timestamp: 2026-06-05
---

# Plan: U-041 — Request SMS role on Q+ regardless of legacy getDefaultSmsPackage (fix BUG-009)

## Root Cause Analysis

`MainActivity.startRestore()` (lines 411-435) captures the current default SMS app via the
legacy `Telephony.Sms.getDefaultSmsPackage()` and gates the ENTIRE restore flow on that
value being non-empty. On Android Q+ (API 29+), `RoleManager` is the authoritative source
for the SMS default; `getDefaultSmsPackage()` queries the legacy `sms_default_application`
secure setting, which is frequently null on RoleManager-managed devices (the RoleManager
manages the SMS role independently of the secure setting). When null, the code falls through
to the "no default package — running on tablet?" branch (line 427-429), shows an error toast,
and bails — the role is never requested and restore never runs.

The `requestDefaultSmsPackageChange()` method (lines 469-481) already has the correct Q+
code path (uses `RoleManager.createRequestRoleIntent(ROLE_SMS)`) but was only reachable when
the legacy API returned a non-empty value.

## Fix Summary

Decouple the Q+ role request from the legacy package capture by adding an `else if`
branch for `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q` BEFORE the pre-Q legacy-capture
block:

```
if (isSmsBackupDefaultSmsApp(this)) {
    startService(intent);                      // Already the default — fast path (unchanged)
} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    // BUG-009: On Q+, always proceed to RoleManager request regardless of legacy value.
    if (preferences.hasSeenSmsDefaultPackageChangeDialog()) {
        requestDefaultSmsPackageChange();
    } else {
        showDialog(SMS_DEFAULT_PACKAGE_CHANGE);
    }
} else {
    // Pre-Q: capture legacy default for ACTION_CHANGE_DEFAULT switch-back.
    final String defaultSmsPackage = Sms.getDefaultSmsPackage(this);
    if (!TextUtils.isEmpty(defaultSmsPackage)) {
        preferences.setSmsDefaultPackage(defaultSmsPackage);
        if (preferences.hasSeenSmsDefaultPackageChangeDialog()) {
            requestDefaultSmsPackageChange();
        } else {
            showDialog(SMS_DEFAULT_PACKAGE_CHANGE);
        }
    } else {
        // Genuinely unsupported device (tablet / no telephony) on pre-Q.
        Toast.makeText(this, R.string.error_no_sms_default_package, LENGTH_LONG).show();
    }
}
```

The `requestDefaultSmsPackageChange()` method is unchanged — the existing Q+ branch inside
it already calls `RoleManager.createRequestRoleIntent(ROLE_SMS)`.

## Files to Modify

| File | Change |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` | Restructure `startRestore()` to separate Q+ (RoleManager) and pre-Q (legacy capture) branches |

## Preserved Behaviors

- **Pre-Q (KITKAT to P)**: legacy `getDefaultSmsPackage()` capture for `ACTION_CHANGE_DEFAULT`
  switch-back in `restoreDefaultSmsProvider()` is preserved.
- **Q+**: role released after restore via `SmsReceiver.disable()` in `restoreDefaultSmsProvider()`
  (unchanged).
- **Already-default fast path**: `isSmsBackupDefaultSmsApp()` returns true → `startService()`
  directly (unchanged).
- **Genuine no-telephony devices (pre-Q)**: toast still fires when legacy API returns null on
  a device without telephony support.
- **Dialog flow**: `SMS_DEFAULT_PACKAGE_CHANGE` dialog still shown on first use (before
  `hasSeenSmsDefaultPackageChangeDialog()` returns true), on both Q+ and pre-Q paths.

## Tests

Add `MainActivityRestoreTest.java` in `app/src/test/java/com/zegoggles/smssync/activity/`:
- `bug009_onQPlus_requestDefaultSmsPackageChange_startsRoleRequestIntent`: On API Q with SMS
  role available but not held, calling `requestDefaultSmsPackageChange()` issues a
  `startActivityForResult` with `REQUEST_CHANGE_DEFAULT_SMS_PACKAGE`. Verifies AC-1.
- `bug009_onQPlus_requestDefaultSmsPackageChange_doesNotShowErrorToast`: No toast on Q+ path.
  Verifies AC-3.
- `bug009_onQPlus_whenAlreadyRoleHolder_noIntentStarted`: Guard — when role is already held,
  no duplicate intent is started.

## Constraints

- No changes to `requestDefaultSmsPackageChange()`, `restoreDefaultSmsProvider()`, or
  any other restore wiring.
- The `@TargetApi(Build.VERSION_CODES.KITKAT)` annotation on `startRestore()` is retained
  (the method body was already guarded at KITKAT).
- Jacoco coverage gate (service*, mail*, auth* packages, LINE >= 70%) is unaffected — the
  changed code is in the `activity` package which is not gated.
