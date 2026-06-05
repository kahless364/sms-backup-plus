---
artifact_type: review-security
story_id: U-041
verdict: PASS
agent: "Security Reviewer"
timestamp: "2026-06-05"
supersedes: "FAIL verdict issued 2026-06-05 (pre-remediation)"
blockers: 0
warnings: 2
---

# Security Review: U-041 (Re-review — Post-Remediation)

## Review Summary

This re-review verifies the remediation commit that addressed the single blocker from the prior
FAIL verdict. The blocker was that on Android Q+, `onActivityResult` gated `startRestore()` on
`preferences.getSmsDefaultPackage() != null`, a condition that is never true on Q+ because the
Q+ branch of `startRestore()` does not call `setSmsDefaultPackage()`. The fix replaces that gate
with an API-level-aware ternary: on Q+ it calls `isSmsBackupDefaultSmsApp(this)` (which resolves
to `roleManager.isRoleHeld(ROLE_SMS)`); on pre-Q it retains the original `getSmsDefaultPackage()
!= null` check. The full role round-trip — acquire, use, release — is now correct on both code
paths. No new security issues were introduced.

## Verdict: PASS

---

## Findings

### Blockers

None. The prior BLOCKER-1 (Q+ restore silently aborted after role grant) is resolved by the
remediation. See the verification trace in the Detailed Role Round-Trip Trace section below.

---

### Warnings

#### WARNING-1: `checkDefaultSmsApp()` safety-net on Q+ passes null `smsPackage` to `restoreDefaultSmsProvider()` (Low / informational, pre-existing)

**File:** `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java`
**Lines:** 538-546 (`checkDefaultSmsApp`), 505-515 (`restoreDefaultSmsProvider`)

`checkDefaultSmsApp()` calls `restoreDefaultSmsProvider(preferences.getSmsDefaultPackage())`.
On Q+ fresh installs `getSmsDefaultPackage()` returns null (the value is never stored by the Q+
branch). Inside `restoreDefaultSmsProvider`, the Q+ arm calls `SmsReceiver.disable(this)`
unconditionally — it does not inspect the passed `smsPackage` argument — so this null is harmless
at runtime. However the call signature is misleading: passing a null package name to a method
named `restoreDefaultSmsProvider` implies there is something to restore to. This was a pre-existing
condition not introduced by U-041, and it carries no exploitable surface. Noted because a future
refactor that accidentally starts using the `smsPackage` argument in the Q+ arm could introduce a
NullPointerException.

#### WARNING-2: `onActivityResult` uses deprecated API (Low / informational, pre-existing)

**File:** `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java`
**Line:** 224

`onActivityResult` is deprecated in favour of the AndroidX `ActivityResultLauncher` contract API.
This is pre-existing and not altered by U-041. The deprecated API remains fully functional on all
Android versions targeted by this app; there is no security vulnerability here. Noted because the
Q+ role-request path traverses this callback and future migration to typed result contracts would
eliminate the risk of incorrect `requestCode` handling at the call site. Low priority.

---

### Security Checklist

- [x] No hardcoded credentials or secrets — confirmed; no credential literals in changed or interacting code
- [x] Input validation on all user inputs — no direct user-controlled input in this change path; the `resultCode` from `onActivityResult` originates from the Android system role-grant dialog and is correctly checked (`RESULT_CANCELED` breaks early before any logic executes)
- [x] Output encoding prevents injection attacks — N/A; no dynamic user-facing output with injectable content in this change
- [x] Authentication tokens handled securely — N/A; this change is in the SMS role flow only
- [x] Authorization checks on all protected resources — `requestDefaultSmsPackageChange()` (line 494) guards the role request with `!roleManager.isRoleHeld(ROLE_SMS)`, preventing duplicate role acquisition; the `isSmsBackupDefaultSmsApp()` check at `startRestore()` line 424 prevents `startService` from firing if the role was not actually granted
- [x] Role round-trip (acquire → use → release) — NOW COMPLETE on Q+: grant is detected by `isSmsBackupDefaultSmsApp(this)` in `onActivityResult`; `startRestore()` is called; `onRestoreStateChanged(finished)` fires; `restoreDefaultSmsProvider()` calls `SmsReceiver.disable(this)` — role released
- [x] Sensitive data encrypted at rest and in transit — no change to SMS data storage or transmission path; `SmsReceiver.storeMessage()` writes to the platform SMS content provider as before
- [x] Error messages don't leak internal details — the `error_no_sms_default_package` toast is now correctly scoped to pre-Q only; Q+ users receive the system RoleManager dialog instead of an opaque "running on tablet?" fallback
- [x] Dependencies have no known critical vulnerabilities — no dependency changes; `RoleManager` / `RoleManagerCompat.ROLE_SMS` are platform and AndroidX APIs with no relevant CVEs
- [x] Consent model preserved — role request is always mediated by the system `RoleManager` dialog; the app never silently acquires the SMS role; the `SMS_DEFAULT_PACKAGE_CHANGE` informational dialog is shown on first use before the system dialog is triggered

---

## Detailed Role Round-Trip Trace (Post-Remediation Verification)

### Q+ Fresh Install — the BUG-009 Device Profile

The blocker scenario is the exact device profile BUG-009 was reported on: Android Q+ where
`Telephony.Sms.getDefaultSmsPackage()` returns null.

1. **`startRestore()` called** (line 420).
2. `isSmsBackupDefaultSmsApp(this)` → `roleManager.isRoleHeld(ROLE_SMS)` → **false** (line 424).
3. `Build.VERSION.SDK_INT >= Q` → true → Q+ branch entered (line 426).
4. `hasSeenSmsDefaultPackageChangeDialog()` determines dialog or direct call; either path leads to
   `requestDefaultSmsPackageChange()` (lines 433-437).
5. `requestDefaultSmsPackageChange()` (line 491): `!roleManager.isRoleHeld(ROLE_SMS)` → true →
   `SmsReceiver.enable(this)` → `startActivityForResult(roleIntent, REQUEST_CHANGE_DEFAULT_SMS_PACKAGE)` (lines 494-497).
6. User grants the SMS role in the system dialog.
7. **`onActivityResult(REQUEST_CHANGE_DEFAULT_SMS_PACKAGE, RESULT_OK, ...)` fires** (line 229).
8. `resultCode != RESULT_CANCELED` → continues.
9. `setSeenSmsDefaultPackageChangeDialog()` called (line 231).
10. **NEW GUARD (remediation):** `Build.VERSION.SDK_INT >= Q` → true →
    `isSmsBackupDefaultSmsApp(this)` → `roleManager.isRoleHeld(ROLE_SMS)` → **true** (role just granted) (lines 238-239).
11. `readyToRestore` → true → `startRestore()` called (lines 241-242).
12. Back in `startRestore()`: `isSmsBackupDefaultSmsApp(this)` → **true** this time (line 424) →
    `startService(intent)` fires `SmsRestoreService` (line 425). Restore executes.
13. `onRestoreStateChanged(finished)` fires on restore completion (line 308).
14. `isSmsBackupDefaultSmsApp(this)` → true → `restoreDefaultSmsProvider(preferences.getSmsDefaultPackage())` called (lines 309-310).
15. `restoreDefaultSmsProvider`: Q+ arm → `SmsReceiver.disable(this)` (line 510). **Role released.**

The role round-trip is complete. There is no window in which the role is held without the restore
running (beyond the normal restore execution window itself).

### Pre-Q Path — Unchanged, Continues to Work

On pre-Q devices, `getSmsDefaultPackage() != null` remains the guard (line 240), which is correct
because `setSmsDefaultPackage(defaultSmsPackage)` is only called in the pre-Q branch of
`startRestore()` (line 443). The pre-Q path is functionally identical to before the remediation.

### Edge Case: Role Denied by User

If the user cancels the system RoleManager dialog, `resultCode == RESULT_CANCELED` (line 230) and
the switch block breaks immediately. Neither `startRestore()` nor any further logic runs.
`SmsReceiver.enable()` was called before `startActivityForResult()` (step 5 above), but a denied
role means `SmsReceiver.onReceive()` will check `isSmsBackupDefaultSmsApp()` → false and discard
any incoming SMS. There is no privilege-without-use scenario on denial.

---

## Summary of Changes Verified Against Prior FAIL

| Prior finding | Status in remediation |
|---|---|
| BLOCKER-1: Q+ restore silently aborts after role grant | **RESOLVED** — guard changed to `isSmsBackupDefaultSmsApp(this)` on Q+ (lines 238-240); full trace confirms role is acquired, used, and released |
| WARNING-1: cold-start safety net held role until restart | **Superseded** — BLOCKER-1 fix means the cold-start path is only a backstop for unexpected abort scenarios, not the primary release path; risk level remains Low |
| WARNING-2: deprecated `onActivityResult` | Pre-existing; unchanged; remains Low informational |
