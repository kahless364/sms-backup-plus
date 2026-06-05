---
artifact_type: plan
story_id: "U-034"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-04"
---

# Plan: U-034 — Fix TLS pin-certificate enrollment dialog crash (BUG-002)

## Problem Statement

`PinCertificateEnrollmentFlow.showEnrollmentDialog` builds an `androidx.appcompat.app.AlertDialog`
using `this.context`, which is `getApplicationContext()` (stripped at constructor line 103).
`AppCompatDelegateImpl.createSubDecor()` requires a `Theme.AppCompat` descendant — the application
context carries the base application theme, not an AppCompat Activity theme — causing a FATAL
`IllegalStateException` when `dialog.show()` is called from `FetchCertTask.onPostExecute`.

## Chosen Approach — Option A (SA recommended)

Move AlertDialog construction into the `DialogShower` callback (`AdvancedSettings`), which has
access to `getActivity()` (AppCompat-themed). `PinCertificateEnrollmentFlow` never holds a
themed context.

**Contract change:** `DialogShower.show(AlertDialog dialog)` → `DialogShower.show(EnrollmentDialogData data)`

`EnrollmentDialogData` is a new public static inner class of `PinCertificateEnrollmentFlow` that
carries all the display parameters the dialog needs:
- `titleResId` (int) — string resource ID for the dialog title
- `message` (String) — pre-formatted body text (subject DN, issuer DN, fingerprint, expiry)
- `positiveButtonResId` (int) — Trust button label resource ID
- `negativeButtonResId` (int) — Cancel button label resource ID
- `onTrust` (DialogInterface.OnClickListener) — stores cert via storeCertOnConfirm
- `onCancel` (DialogInterface.OnClickListener) — fires listener.onCancelled()

`showEnrollmentDialog` builds the message string using `this.context` (application context,
correct for `getString()`), then constructs `EnrollmentDialogData` and passes it to
`onShowDialog.show(data)`.

`AdvancedSettings.Server.launchEnrollmentFlow()` anonymous `DialogShower` receives
`EnrollmentDialogData` and builds `new AlertDialog.Builder(getActivity())...` using the
AppCompat-themed Activity context.

## Anti-Leak Constraint (binding)

`PinCertificateEnrollmentFlow` and `FetchCertTask` never hold an Activity or themed context as
a field. `this.context` remains `getApplicationContext()`. The themed context (`getActivity()`)
is obtained and used only in `onShowDialog.show(data)`, which executes on the main thread in
`onPostExecute` — the Activity lifecycle guard `getActivity() == null || isFinishing()` is
preserved.

## Files Changed

| File | Change |
|------|--------|
| `PinCertificateEnrollmentFlow.java` | Remove `AlertDialog` import; add `EnrollmentDialogData` inner class; change `DialogShower` to `void show(EnrollmentDialogData)`; rewrite `showEnrollmentDialog` to produce `EnrollmentDialogData` |
| `AdvancedSettings.java` | Update anonymous `DialogShower` to accept `EnrollmentDialogData`, build `AlertDialog.Builder(getActivity())`, call `dialog.show()` |
| `AdvancedSettingsServerTest.java` | Update two existing `dialog ->` lambdas to `data ->` to match new interface; add 4 new U-034 test methods |

## Preserved Behaviors

- `this.context = getApplicationContext()` at line 103 — unchanged (AC-3, anti-leak)
- `context.getString(...)` calls for message building — unchanged (lines 352-355 equivalent)
- `setNegativeButton` before `setPositiveButton` in builder chain — preserved in AdvancedSettings (AC-5)
- `storeCertOnConfirm` called from `onTrust` listener only (AC-4/AC-6)
- `listener.onCancelled()` called from `onCancel` listener only (AC-5)
- `getActivity() == null || isFinishing()` guard — preserved in AdvancedSettings (AC-3)
- Four mandatory cert fields (subject DN, issuer DN, fingerprint, expiry) in dialog message (AC-6)
- `FetchCertTask` async model unchanged (pre-existing AsyncTask deprecation, out of scope)

## Verification

- Build gate: `./gradlew assembleDebug testDebugUnitTest jacocoTestCoverageVerification` — all pass
- On-device verification required for AC-1 (themed context crash cannot be caught by JVM tests)
