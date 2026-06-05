---
type: story
status: planned
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-034
title: 'Fix TLS pin-certificate enrollment dialog crash: build AlertDialog with a themed Activity context'
pipeline: ''
domain: modernization
requirement_source: bug:BUG-002
sprint: '000004'
---

# U-034: Fix TLS pin-certificate enrollment dialog crash: build AlertDialog with a themed Activity context

## Story

As a user enrolling a pinned TLS certificate,
I want the certificate-trust dialog to display instead of crashing the app,
So that I can review and trust or cancel a server certificate.

## Acceptance Criteria

- [ ] AC-1: Triggering pin-certificate enrollment (Advanced Settings -> Server section -> enter a server address -> tap **Pin server certificate**) shows the enrollment dialog with no crash. No `IllegalStateException` with the message "You need to use a Theme.AppCompat theme (or descendant) with this activity" appears in logcat at any point during the enrollment flow. Verified on-device on emulator-5554 (API 37) and on a physical device if available — this AC cannot be satisfied by unit tests alone.

- [ ] AC-2: The `androidx.appcompat.app.AlertDialog` is built with an AppCompat-themed Activity context, not with `getApplicationContext()`. Per SA option A (preferred): the `DialogShower` interface contract at `PinCertificateEnrollmentFlow.java:132-134` is changed from `void show(AlertDialog dialog)` to pass the cert display data (subject DN, issuer DN, SHA-256 fingerprint, expiry, title/button strings, and the positive/negative click semantics — as a parameter object or as existing listener callbacks) so that the `AdvancedSettings` anonymous implementation builds `new AlertDialog.Builder(getActivity())` using the Activity's AppCompat-themed context and calls `dialog.show()` at `AdvancedSettings.java:471`. An alternative is acceptable: `showEnrollmentDialog` receives a themed `Context` parameter obtained at call-time in `onPostExecute` (main thread only) without being retained as a field, supplying the themed context exclusively to `AlertDialog.Builder`. Either approach must ensure the AppCompat-themed context reaches `AlertDialog.Builder`; the approach is confirmed by code review.

- [ ] AC-3: No Activity context is retained as a long-lived field across the async `FetchCertTask` TLS handshake. Any AppCompat-themed or Activity-backed context is obtained and used only on the main thread inside `onPostExecute` (the same thread where `showEnrollmentDialog` is already invoked at `PinCertificateEnrollmentFlow.java:280`) and is never stored as a field on `PinCertificateEnrollmentFlow` or `FetchCertTask`. The `getApplicationContext()` strip at `PinCertificateEnrollmentFlow.java:103` is preserved (or removed only if the themed context is never assigned to `this.context`); `this.context` (application context) continues to serve the `context.getString(...)` lookups at lines 322, 352-355, 385, and 401. Verified by code review: no Activity or Fragment reference exists as an instance field on either class.

- [ ] AC-4: The **Trust this certificate** (positive) button stores the certificate via `storeCertOnConfirm(host, port, cert)` (U-009 AC-6). After tapping Trust, `PinnedCertStore` contains an entry for the host and port, and the pin-cert indicator in Advanced Settings updates to reflect the pinned state (i.e., `updatePinCertIndicator()` is called via `Listener.onEnrolled`).

- [ ] AC-5: The **Cancel** (negative) button writes nothing to `PinnedCertStore` (U-009 AC-5). After tapping Cancel, the pin-cert indicator is unchanged. Cancel is the safe default and is not the default-focused button: in the `AlertDialog.Builder` chain, `.setNegativeButton` remains listed before `.setPositiveButton` (current lines 362 and 369), or focus is explicitly set to the negative button, so the Trust button is never default-focused.

- [ ] AC-6: The enrollment dialog displays all four mandatory cert fields: subject DN (from `cert.getSubjectX500Principal().getName()`), issuer DN (from `cert.getIssuerX500Principal().getName()`), SHA-256 fingerprint (colon-separated uppercase hex, e.g. `AB:CD:EF:...`), and expiry date (locale-formatted medium date). All four fields use the `R.string.ui_protocol_pin_certificate_dialog_*` string resources as they do today.

- [ ] AC-7: Build passes: `./gradlew assembleDebug testDebugUnitTest jacocoTestCoverageVerification` completes with zero build errors, zero test failures, and per-package LINE coverage >= 70%. Unit tests for `PinCertificateEnrollmentFlow` and any `DialogShower` test double are added or adjusted to match the updated interface contract. The themed-context defect class (an `IllegalStateException` thrown by `AppCompatDelegateImpl` at real dialog inflation) cannot be reproduced by JVM unit tests; on-device verification per AC-1 is the definitive gate.

- [ ] AC-8: No regression to any other U-009 enrollment or removal flow acceptance criteria. The fetch-error path (`Listener.onFetchError` called when the TLS handshake returns no leaf cert, at `PinCertificateEnrollmentFlow.java:322` and `:385`) is unaffected by the fix; no dialog is shown on error and no store write occurs.

### Integration Criteria

- [ ] IC-1: If the `DialogShower` interface signature changes, all call sites in `AdvancedSettings.java` (the anonymous implementation at lines 468-475) and all test doubles for `DialogShower` in the test suite are updated to match the new signature. No call site compiles against the old `void show(AlertDialog dialog)` signature after the change.

- [ ] IC-2: If `showEnrollmentDialog` receives a new parameter (e.g., a themed `Context`), all call sites — currently `FetchCertTask.onPostExecute` at `PinCertificateEnrollmentFlow.java:280` and any unit tests that invoke `showEnrollmentDialog` directly — pass a value consistent with the new signature. No existing call site is silently left with the old arity.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/PinCertificateEnrollmentFlow.java` | Constructor strips to app context at line 103. `showEnrollmentDialog` builds `AlertDialog` with `this.context` (application context) at line 357. `DialogShower.show(AlertDialog dialog)` receives a pre-built dialog at line 380. | Dialog construction moves to `AdvancedSettings` (option A) or `showEnrollmentDialog` receives a themed Context at call-time. `DialogShower` contract updated accordingly. `this.context` (app context) retained for `getString` calls. |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java` | `DialogShower` anonymous class at lines 468-475 receives a pre-built `AlertDialog` and calls `dialog.show()` inside the `getActivity() != null && !getActivity().isFinishing()` guard. | Under option A: the `DialogShower` implementation receives cert data and builds `new AlertDialog.Builder(getActivity())` before calling `dialog.show()`. The activity-null/isFinishing guard is retained for the `show()` call. |
| `app/src/test/java/com/zegoggles/smssync/activity/fragments/PinCertificateEnrollmentFlowTest.java` (if present) | Any `DialogShower` test double implementing `void show(AlertDialog dialog)`. | Must be updated to match the revised `DialogShower` signature. |

## Existing Behavior to Preserve

- **U-009 AC-5 (Cancel is safe default):** The negative (Cancel) button must not write to `PinnedCertStore`. `listener.onCancelled()` is called on cancel. `.setNegativeButton` remains ahead of `.setPositiveButton` in the builder chain so Trust is never the default-focused button.
- **U-009 AC-6 (Trust stores cert):** `storeCertOnConfirm(host, port, cert)` is called from the positive button's `OnClickListener`. This is the only code path that writes `PinnedCertStore`.
- **Four mandatory cert fields:** Subject DN, issuer DN, SHA-256 fingerprint (colon-separated uppercase hex), and expiry date (locale-formatted medium date) all appear in the dialog message. Field formatting at `PinCertificateEnrollmentFlow.java:352-355` is unchanged.
- **Anti-leak design:** `FetchCertTask` must not hold a strong Activity or Fragment reference for the duration of the async TLS handshake. The application context (or a `WeakReference`) is the only long-lived context reference on `PinCertificateEnrollmentFlow` and `FetchCertTask`.
- **Fetch-error path:** If the TLS handshake returns no leaf cert or fingerprint computation fails, `listener.onFetchError(message)` is called and no dialog is shown. This path is unaffected by any change to dialog construction.
- **`getActivity()` null/finishing guard:** The guard at `AdvancedSettings.java:471` is retained for the `show()` call. It does not prevent the themed-context crash (the crash is in construction, not in `show()`) and must not be removed as a substitute for the fix.
- **`AsyncTask` usage:** `FetchCertTask` extends `AsyncTask` (deprecated API 30). This is a pre-existing condition introduced in U-009. The async execution model must not change as part of this fix.

## Verification Steps

1. **On-device crash reproduction baseline (before fix):** On emulator-5554 (API 37), open Advanced Settings -> Server section, enter `imap.example.org:993`, tap **Pin server certificate**. Confirm the app crashes with `IllegalStateException: You need to use a Theme.AppCompat theme` in logcat. This establishes the repro baseline.

2. **On-device dialog display after fix (AC-1, AC-2):** Apply the fix. Repeat step 1. Confirm the enrollment dialog appears (all four cert fields visible in the dialog body) with no FATAL exception and no `IllegalStateException` in logcat from `AppCompatDelegateImpl`. Repeat on a physical device if available.

3. **Trust path (AC-4):** In the enrollment dialog, tap **Trust this certificate**. Confirm in Advanced Settings that the pin-cert indicator updates to show the server is pinned. Confirm `PinnedCertStore` contains an entry for the host and port (via the indicator or a debug log / logcat `onEnrolled` event).

4. **Cancel path (AC-5):** Trigger enrollment again. In the dialog, tap **Cancel**. Confirm the pin-cert indicator is unchanged from its pre-dialog state. Confirm no store write occurred (indicator unchanged; no `onEnrolled` logcat line).

5. **Cancel is not default-focused (AC-5):** Inspect the builder chain in the updated code: `.setNegativeButton` appears before `.setPositiveButton`. Optionally verify on-device that the Cancel button does not have the default-focused highlight when the dialog first opens.

6. **Anti-leak code review (AC-3):** After the fix, confirm via code review that neither `PinCertificateEnrollmentFlow` nor `FetchCertTask` holds an Activity, Fragment, or AppCompat-themed Context as an instance field. Confirm `this.context` (or an equivalent field) remains `getApplicationContext()` for the `getString` calls.

7. **Device rotation during async fetch (AC-3):** Trigger enrollment. Before the dialog appears (while `FetchCertTask` is still in `doInBackground`), rotate the device. Confirm no NPE or crash occurs when `onPostExecute` eventually runs. If the activity is gone, the dialog is silently skipped (per the existing `getActivity() != null` guard).

8. **Fetch-error path (AC-8):** With the fix applied, configure an unreachable server address (e.g., `unreachable.invalid:993`). Trigger enrollment and let the fetch time out. Confirm a Toast error message appears (from `Listener.onFetchError`) and no dialog is shown.

9. **Build and coverage gate (AC-7):** Run `./gradlew assembleDebug testDebugUnitTest jacocoTestCoverageVerification`. Confirm zero errors, zero failures, and per-package LINE >= 70%.

10. **Full U-009 regression (AC-8):** Execute the complete U-009 story verification checklist. Confirm all U-009 ACs pass with no regressions.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android | All changes: `PinCertificateEnrollmentFlow.java` dialog-construction refactor, `AdvancedSettings.java` `DialogShower` update, unit test alignment, on-device verification | Developer |

## Technical Context

**Root cause (precise).** `PinCertificateEnrollmentFlow.java:26` imports `androidx.appcompat.app.AlertDialog`. This variant delegates window decoration to `AppCompatDelegateImpl`, which at `createSubDecor()` checks the supplied Context for a `Theme.AppCompat` descendant attribute and throws `IllegalStateException` if the theme is absent. The constructor at line 103 stores `context.getApplicationContext()` — stripping the AppCompat Activity theme from the Fragment-scoped Context passed by `AdvancedSettings.getContext()`. Line 357 builds `new AlertDialog.Builder(context)` with this application context. The crash fires inside `dialog.show()` -> `AlertDialog.onCreate()` -> `AppCompatDelegateImpl.createSubDecor()`, after the `getActivity() != null && !getActivity().isFinishing()` guard at `AdvancedSettings.java:471` has already passed — the guard is irrelevant to the defect.

**SA recommended fix — option A.** Change the `DialogShower` contract (`PinCertificateEnrollmentFlow.java:132-134`) from `void show(AlertDialog dialog)` to a signature that passes cert display data (or a factory/supplier) rather than a pre-built dialog. The `AdvancedSettings` anonymous implementation then builds `new AlertDialog.Builder(getActivity())` in `show()` using the AppCompat-themed Activity context and wires the click handlers. The click-handler logic (`storeCertOnConfirm` on Trust, `listener.onCancelled()` on Cancel) crosses the boundary as parameters or as the existing `Listener` callbacks the flow already owns — store-write logic stays in the flow. `PinCertificateEnrollmentFlow` never holds a themed Context. `this.context` (application context) is still correct and required for `getString(...)` lookups at lines 322, 352-355, 385, and 401.

**Anti-leak boundary (binding on all fix options).** `FetchCertTask` runs an async TLS handshake. Any fix may touch a themed Activity context only on the main thread inside `onPostExecute` (already where `showEnrollmentDialog` is called at line 280) and must not retain that context as a field on `PinCertificateEnrollmentFlow` or `FetchCertTask` across the async boundary. Option A satisfies this for free — the themed context never enters the flow.

**Why on-device verification is mandatory.** JVM unit tests use a `DialogShower` test double that never calls `dialog.show()` and never inflates a real `AlertDialog`. `AppCompatDelegateImpl.createSubDecor()` is never executed in the test JVM. A regression of this defect class — wrong context passed to `AlertDialog.Builder` — will compile cleanly, pass all unit tests, and fail only at real dialog inflation on a device. The build gate alone is insufficient. The U-009 smoke in sprint-001 checked only app launch, which is why this shipped.

**`AsyncTask` deprecation.** `FetchCertTask extends AsyncTask` (API 30 deprecated). This is a pre-existing condition from U-009, out of scope for this story. The async model must not change here.

**`android.app.AlertDialog` is not an acceptable fallback.** Replacing `androidx.appcompat.app.AlertDialog` with the platform variant would eliminate the theme requirement but is inconsistent with the codebase's AppCompat adoption and loses Material styling. It is not an acceptable approach unless approved by the SA.

## Supporting Documentation

- BUG-002: `sdlc/artifacts/stories/BUG-002-pincert-enrollment-dialog-appcompat-theme-crash.md` — root cause analysis, evidence, SA option A rationale
- U-009: TLS pinned-certificate enrollment story (parent feature; ACs 5 and 6 must be preserved)
- `CNTR-MODERNIZATION-002`: Affirmative consent requirement referenced in `PinCertificateEnrollmentFlow` Javadoc

## Integration Contract References

No standalone integration contracts govern this story. IC-1 and IC-2 above cover all call-site alignment requirements for the `DialogShower` interface and `showEnrollmentDialog` signature.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Introduced in sprint-001 during U-009 implementation. The `getApplicationContext()` anti-leak pattern was applied correctly for the async fetch phase but was not adjusted for `showEnrollmentDialog`, which runs on the main thread in `onPostExecute` and builds an `androidx.appcompat.app.AlertDialog` — a variant that unconditionally requires an AppCompat-themed context. The U-009 smoke only verified app launch, not the enrollment dialog display path.

Estimation guidance: low-medium. The change is structurally focused — dialog construction refactor in one method (`showEnrollmentDialog`), one interface contract update (`DialogShower`), one call-site update in `AdvancedSettings`, and test-double alignment. On-device verification (emulator + physical device) is the largest time cost because the defect class cannot be caught by automated tests.
