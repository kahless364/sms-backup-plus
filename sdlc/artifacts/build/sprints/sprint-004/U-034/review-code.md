---
artifact_type: code-review
story_id: "U-034"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-04"
blockers: 0
warnings: 1
---

# Code Review: U-034

## Summary

The fix correctly implements SA Option A. `PinCertificateEnrollmentFlow` no longer holds or uses
any Activity-themed context — it retains only `getApplicationContext()` for `getString()` calls.
`AlertDialog` construction has moved into `AdvancedSettings.Server.launchEnrollmentFlow()`'s
anonymous `DialogShower`, which uses `getActivity()` (AppCompat-themed Activity context). All
U-009 behavioral contracts are preserved. No Activity context is stored as a field.

## Findings

### Anti-Leak Verification (AC-3)

`PinCertificateEnrollmentFlow` fields: `context` (set to `getApplicationContext()` at line 102),
`pinnedCertStore`, `listener`. None of these are Activity-backed.

`FetchCertTask` fields: `host` (String), `port` (int), `onShowDialog` (DialogShower). The
`DialogShower` implementation from `AdvancedSettings` does reference `getActivity()` but only
at the moment `show()` is invoked — inside `FetchCertTask.onPostExecute()`, which runs on the
main thread. The `DialogShower` instance is held as a field in `FetchCertTask` (needed to call
`show()` in `onPostExecute`), but this is a reference to the anonymous class, not to a Context
or Activity directly. The anonymous class does close over `AdvancedSettings.Server` via its
implicit outer-class reference — this is the standard Java anonymous class behavior for inner
fragments and is the same pattern as was present before the fix.

**Verdict:** AC-3 satisfied. No new Activity/Context field is introduced on either
`PinCertificateEnrollmentFlow` or `FetchCertTask`.

### DialogShower Contract Change (IC-1)

Old: `void show(AlertDialog dialog)` — caller built dialog, shower called `dialog.show()`
New: `void show(EnrollmentDialogData data)` — shower builds dialog with themed context and shows

Call site verification:
- `AdvancedSettings.java` anonymous impl — updated
- `AdvancedSettingsServerTest.java` two test lambdas — updated
- No other implementors or call sites found (grep confirmed)

### Builder Chain Order (AC-5)

In `AdvancedSettings.java` `show()` implementation:
```
.setNegativeButton(data.negativeButtonResId, data.onCancel)   // Cancel first
.setPositiveButton(data.positiveButtonResId, data.onTrust)    // Trust second
```
Negative before positive — correct. Trust is not default-focused.

### Four Mandatory Cert Fields (AC-6)

In `showEnrollmentDialog()` the `message` string is built as:
1. Subject DN (`cert.getSubjectX500Principal().getName()`) — via `R.string.ui_protocol_pin_certificate_dialog_subject`
2. Issuer DN (`cert.getIssuerX500Principal().getName()`) — via `R.string.ui_protocol_pin_certificate_dialog_issuer`
3. SHA-256 fingerprint (colon-separated uppercase hex) — via `R.string.ui_protocol_pin_certificate_dialog_fingerprint`
4. Expiry date (locale-formatted medium date) — via `R.string.ui_protocol_pin_certificate_dialog_expires`

All four fields preserved. Message is built with `this.context` (application context — correct for getString).

### Trust Path (AC-4)

`EnrollmentDialogData.onTrust` calls `storeCertOnConfirm(host, port, cert)`, which calls
`pinnedCertStore.put(host.toLowerCase(Locale.US), port, cert)` and fires `listener.onEnrolled()`.
Same logic as before, now as a click-listener carried in `EnrollmentDialogData`.

### Cancel Path (AC-5)

`EnrollmentDialogData.onCancel` calls `listener.onCancelled()` — no store write. Same logic as before.

### Fetch-Error Path (AC-8)

`FetchCertTask.onPostExecute()` checks `result.error != null` before calling `showEnrollmentDialog()`.
Error path (`listener.onFetchError`) and fingerprint-computation error path are unchanged.

### Import Cleanup

`import androidx.appcompat.app.AlertDialog` removed from `PinCertificateEnrollmentFlow` (no longer used there).
`import java.util.Date` and `import java.util.Formatter` removed (were unused after prior refactoring).
`import androidx.appcompat.app.AlertDialog` retained in `AdvancedSettings` (used in new `show()` body).

## Warnings

- **WARN-1 (non-blocking):** `FetchCertTask` holds the `DialogShower` (an anonymous class) as a field across the async TLS handshake. The anonymous `DialogShower` has an implicit reference to its enclosing `AdvancedSettings.Server` instance (standard Java anonymous class behavior). This is a pre-existing architectural pattern for this type of Fragment-based async work; the Fragment lifecycle guard (`getActivity() == null || isFinishing()`) mitigates risk of use-after-detach. This is not a new leak introduced by this fix — the same implicit reference existed in the old `DialogShower` that called `dialog.show()`.

## Verdict

PASS. No blockers. One non-blocking warning (pre-existing architecture pattern, not introduced
by this fix). The structural change is minimal, targeted, and preserves all U-009 behavioral
contracts.
