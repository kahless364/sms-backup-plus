---
type: bug
status: done
artifact_type: bug
severity: high
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-034
platforms: []
tags: []
id: BUG-002
title: 'TLS pin-certificate enrollment dialog crashes: AppCompat AlertDialog built with application context'
domain: build
origin: qa
---

# BUG-002: TLS pin-certificate enrollment dialog crashes: AppCompat AlertDialog built with application context

## Description

Triggering the TLS pin-certificate enrollment flow from Advanced Settings crashes the app with a FATAL `IllegalStateException`. The crash occurs the moment the enrollment confirmation dialog attempts to show.

Root cause: `PinCertificateEnrollmentFlow` stores `context.getApplicationContext()` at construction time (line 103). When `showEnrollmentDialog` builds the dialog at line 357 using `new AlertDialog.Builder(context)`, it passes that application context to `androidx.appcompat.app.AlertDialog.Builder`. The `AppCompatDelegateImpl` requires a Context themed with `Theme.AppCompat` (or a descendant). The application context carries the base application theme, not an AppCompat Activity theme — this violates the requirement and causes an `IllegalStateException` before the dialog can display.

The Activity context available via `getActivity()` in `AdvancedSettings` is AppCompat-themed and would satisfy the requirement. The application context was chosen deliberately to avoid leaking an Activity reference across the async `FetchCertTask` — the dialog is built in `onPostExecute`, which runs on the main thread after the async cert fetch completes. The fix must supply an AppCompat-themed context to the dialog builder while preserving the anti-leak intent of the original design.

## Steps to Reproduce

1. Install the modernized build on a device or emulator running API 37 (tested: emulator-5554).
2. Launch the app.
3. Open the navigation drawer and navigate to **Advanced Settings**.
4. Expand the **Server** section.
5. Enter any valid server address in the server address field (e.g., `imap.example.org:993`).
6. Tap **Pin server certificate** (or the equivalent control that triggers certificate enrollment).
7. Wait for `FetchCertTask` to complete the TLS handshake and call `onPostExecute`.
8. Observe: the app crashes with a FATAL exception before the enrollment dialog appears.

## Expected Behavior

After the TLS handshake completes, the enrollment confirmation dialog displays without crashing. The dialog shows all four mandatory fields:

- Subject DN (from `cert.getSubjectX500Principal().getName()`)
- Issuer DN (from `cert.getIssuerX500Principal().getName()`)
- SHA-256 fingerprint (colon-separated uppercase hex, e.g., `AB:CD:EF:...`)
- Expiry date (locale-formatted medium date)

The dialog presents a **Trust this certificate** button (positive) and a **Cancel** button (negative). Cancel is the safe default — it is not the default-focused button. The user can confirm or dismiss without the process dying.

## Actual Behavior

The app terminates with a FATAL exception immediately when the dialog attempts to show. The process dies; no dialog is ever presented to the user. The complete crash stack:

```
FATAL EXCEPTION: main
Process: com.zegoggles.smssync
java.lang.IllegalStateException: You need to use a Theme.AppCompat theme (or descendant) with this activity.
    at androidx.appcompat.app.AppCompatDelegateImpl.createSubDecor(AppCompatDelegateImpl.java:696)
    at androidx.appcompat.app.AlertDialog.onCreate(AlertDialog.java:279)
    at android.app.Dialog.show(Dialog.java:325)
    at com.zegoggles.smssync.activity.fragments.AdvancedSettings$Server$4.show(AdvancedSettings.java:472)
    at com.zegoggles.smssync.activity.fragments.PinCertificateEnrollmentFlow.showEnrollmentDialog(PinCertificateEnrollmentFlow.java:380)
    at com.zegoggles.smssync.activity.fragments.PinCertificateEnrollmentFlow$FetchCertTask.onPostExecute(PinCertificateEnrollmentFlow.java:280)
-> Process com.zegoggles.smssync (pid 27031) has died (data_app_crash).
```

The U-009 TLS pinned-certificate enrollment security feature is completely unusable as a result.

## Evidence

**PinCertificateEnrollmentFlow.java**

- Line 26: `import androidx.appcompat.app.AlertDialog;` — the AppCompat AlertDialog variant is imported. This variant unconditionally requires a `Theme.AppCompat`-themed Context.
- Line 103: `this.context = context.getApplicationContext();` — the constructor receives a Fragment-scoped Context from `AdvancedSettings.getContext()` and immediately strips it to the application context. From this point forward, `this.context` has no Activity theme.
- Line 357: `AlertDialog dialog = new AlertDialog.Builder(context)` — the dialog builder is constructed with the application context stored at line 103.
- Line 380: `onShowDialog.show(dialog);` — the already-constructed dialog (built with the app context) is passed to the `DialogShower` callback for display.
- Line 280 (`FetchCertTask.onPostExecute`): `showEnrollmentDialog(host, port, result.cert, onShowDialog);` — `showEnrollmentDialog` is called on the main thread after the async TLS fetch; by this point the dialog construction (and the crash) occur before the `DialogShower` can guard anything.

**AdvancedSettings.java**

- Line 472: `dialog.show()` — the `DialogShower` implementation checks `getActivity() != null && !getActivity().isFinishing()` before calling `dialog.show()`, but the crash occurs inside `Dialog.show()` → `AlertDialog.onCreate()` → `AppCompatDelegateImpl.createSubDecor()`, triggered by `dialog.show()` itself. The guard does not prevent the crash because the fault is the context theme, not a null Activity.

## Acceptance Criteria

- [ ] AC-1: Triggering pin-certificate enrollment (Advanced Settings → Server → server address → enroll) displays the enrollment confirmation dialog without crashing. Verified on-device (emulator-5554, API 37) and on a physical device.
- [ ] AC-2: The enrollment dialog is constructed with a `Context` themed with `Theme.AppCompat` (or a descendant). No `IllegalStateException` is thrown by `AppCompatDelegateImpl.createSubDecor`. Verified by confirming the dialog builds and shows cleanly with no `IllegalStateException` in logcat.
- [ ] AC-3: No Activity context is held as a long-lived field across the async `FetchCertTask` execution. The fix preserves the anti-leak intent of the original `getApplicationContext()` design — the Activity (or themed Context) is obtained at dialog-show time on the main thread, not captured before `execute()` is called. Verified by code review: no Activity or Fragment reference is stored as a field on `PinCertificateEnrollmentFlow` or `FetchCertTask` that outlives the task's `onPostExecute`.
- [ ] AC-4: The **Trust this certificate** (positive) button stores the certificate in `PinnedCertStore` via `storeCertOnConfirm` (U-009 AC-6). After tapping Trust, the pin-cert indicator in Advanced Settings updates to reflect the pinned state.
- [ ] AC-5: The **Cancel** (negative) button writes nothing to `PinnedCertStore` (U-009 AC-5). After tapping Cancel, the pin-cert indicator is unchanged.
- [ ] AC-6: The enrollment dialog correctly displays all four mandatory cert fields — subject DN, issuer DN, SHA-256 fingerprint (colon-separated uppercase hex), and expiry date — using the strings loaded from `R.string.ui_protocol_pin_certificate_dialog_*` resources.
- [ ] AC-7: No regression to any other U-009 enrollment or removal flow acceptance criteria. The full enrollment happy path and the cancel path both behave as specified in U-009.

### Integration Criteria

- [ ] IC-1: If the `DialogShower` interface contract changes (e.g., to receive a `Context` rather than a pre-built `AlertDialog`), all call sites in `AdvancedSettings.java` and all test doubles in the test suite are updated to match the new signature.
- [ ] IC-2: If `showEnrollmentDialog` receives a themed `Context` parameter at call-time, all call sites (currently `FetchCertTask.onPostExecute` at line 280 and tests) pass an AppCompat-themed Context.

## Impact Analysis

Manual impact analysis (change_records layers not configured; advisory only).

**Directly affected files:**

| File | Why affected |
|------|-------------|
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/PinCertificateEnrollmentFlow.java` | Dialog construction at line 357 uses `this.context` (application context). Fix requires supplying an AppCompat-themed Context to the `AlertDialog.Builder` call, which means either removing the field's `getApplicationContext()` strip, changing the `DialogShower` contract, or passing a themed Context into `showEnrollmentDialog` at call-time. |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java` | The `DialogShower` anonymous class at line 468–475 may need to change if the fix restructures when and where the dialog is built (e.g., moving dialog construction into the `DialogShower.show()` implementation so the Activity's themed Context is used). The constructor call at line 449–451 passes `getContext()`, which is correct — the issue is the `getApplicationContext()` strip inside the constructor. |

**Potentially affected files:**

| File | Why potentially affected |
|------|--------------------------|
| `app/src/test/java/com/zegoggles/smssync/activity/fragments/PinCertificateEnrollmentFlowTest.java` (if it exists) | Test doubles for `DialogShower` must match any updated interface contract. |

**Feature impact:** The U-009 TLS pinned-certificate enrollment feature is completely unusable. Every path through the enrollment UI results in a FATAL crash. Core backup, restore, OAuth, and application launch are unaffected — the crash is isolated to this single dialog invocation.

**Security impact:** Because enrollment is unusable, users cannot configure TLS certificate pinning. Connections to custom IMAP servers fall back to default trust policy. This is a security-capability regression introduced in sprint-001 by U-009.

## Suggested Approach

Three viable approaches, in order of preference:

1. **Move dialog construction into the `DialogShower` callback (preferred):** Change the `DialogShower.show(AlertDialog)` signature to `DialogShower.show(Context, ...)` or pass a `Supplier<AlertDialog>` / builder arguments, so the `AdvancedSettings` anonymous class builds the `AlertDialog` using `getActivity()` (the AppCompat-themed context) at show-time. `PinCertificateEnrollmentFlow` never holds a themed Context; it only holds the application context for resource string lookups in `fetchLeafCert` (line 322). The dialog parameters (message string, cert fields) can be computed using the app context for strings, then the `AlertDialog` is built by the caller with the Activity context.

2. **Pass a themed Context into `showEnrollmentDialog` at call-time:** Add a `Context themedContext` parameter to `showEnrollmentDialog`. `FetchCertTask` would need to capture a themed Context reference before `execute()`. This risks the Activity being destroyed before `onPostExecute` runs — a WeakReference to the Activity, checked at call-time in `onPostExecute`, would mitigate the leak while providing the needed Context.

3. **Replace `androidx.appcompat.app.AlertDialog` with `android.app.AlertDialog`:** The platform `AlertDialog` does not require an AppCompat theme. This avoids the context issue entirely but loses Material styling. Given the project has already adopted AppCompat, this approach is inconsistent with the codebase direction and should only be considered if the other approaches present unacceptable complexity.

Note: The application context is adequate for `context.getString(...)` calls in `fetchLeafCert` (line 322) and `storeCertOnConfirm` (line 399). The themed context is only required by `AlertDialog.Builder`. Any fix should keep the application context for non-UI resource access and supply the themed context exclusively at dialog construction time.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/PinCertificateEnrollmentFlow.java` | Constructor strips to app context (line 103); `showEnrollmentDialog` builds `AlertDialog` with app context (line 357) | Dialog must be built with an AppCompat-themed Context; construction may move to the `DialogShower` implementation or `showEnrollmentDialog` must receive a themed Context |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java` | `DialogShower.show(AlertDialog)` receives pre-built dialog and calls `dialog.show()` (lines 470–472) | May change to build the dialog within the `show` implementation using `getActivity()` as the themed Context, depending on chosen fix approach |

## Existing Behavior to Preserve

- **U-009 AC-5:** Cancel writes nothing to `PinnedCertStore`. The negative button's `OnClickListener` must remain a no-op for the store, with `listener.onCancelled()` called if a listener is set.
- **U-009 AC-5 (default focus):** Cancel is the safe default. The positive (Trust) button must not be default-focused. `.setNegativeButton` must remain listed before `.setPositiveButton` in the builder chain, or focus must be explicitly set to the negative button.
- **U-009 AC-6:** Trust stores the certificate. `storeCertOnConfirm(host, port, cert)` must be called from the positive button's `OnClickListener`.
- **Four mandatory cert fields:** Subject DN, issuer DN, SHA-256 fingerprint, and expiry date must all appear in the dialog message, formatted as they are today (lines 352–355).
- **No Activity leak across async fetch:** `FetchCertTask` must not hold a strong reference to the Activity or Fragment for the duration of the async TLS handshake. Application context (or a WeakReference) must be the only long-lived context reference.
- **`listener.onFetchError` path:** If the cert fetch fails or fingerprint computation fails, `listener.onFetchError(message)` is called and no dialog is shown. This path must be unaffected by the fix.

## Verification Steps

1. **On-device crash repro (AC-1):** On emulator-5554 (API 37), open Advanced Settings → Server section → enter `imap.example.org:993` → tap **Pin server certificate**. Confirm the enrollment dialog appears (cert fields visible) with no FATAL in logcat.
2. **Trust path (AC-4):** In the enrollment dialog, tap **Trust this certificate**. Confirm the pin-cert indicator in Advanced Settings updates to show the server is pinned. Confirm `PinnedCertStore` contains an entry for the host/port via the indicator or a debug log.
3. **Cancel path (AC-5):** Trigger enrollment again. In the dialog, tap **Cancel**. Confirm the indicator is unchanged (cert not stored). Confirm no store write occurs (logcat or indicator check).
4. **Theme context verification (AC-2):** With fix applied, confirm no `IllegalStateException` from `AppCompatDelegateImpl` appears in logcat at any point during enrollment. Run on both emulator (API 37) and a physical device if available.
5. **Anti-leak verification (AC-3):** Code review: confirm no Activity or Fragment reference is held as a field on `PinCertificateEnrollmentFlow` or `FetchCertTask`. Rotate the device after triggering enrollment (before `onPostExecute` fires) and confirm no NPE or crash occurs.
6. **Regression check (AC-7):** Execute the full U-009 story verification checklist. Confirm all U-009 ACs pass.
7. **Build gate:** Run `./gradlew assembleDebug test` and confirm zero test failures and zero build errors.

## Root Cause Analysis

<!-- Filled during planning phase -->

## Technical Context

- `androidx.appcompat.app.AlertDialog` delegates window decoration to `AppCompatDelegateImpl`, which inspects the supplied Context for a `Theme.AppCompat` (or descendant) theme attribute. The platform `android.app.AlertDialog` does not perform this check. The distinction is critical: importing and using the appcompat variant requires an Activity-themed Context everywhere a dialog is constructed.
- `PinCertificateEnrollmentFlow` stores `context.getApplicationContext()` at line 103 to avoid retaining an Activity reference during the async `FetchCertTask` TLS handshake. This is a valid anti-leak pattern for non-UI work. The conflict arises because `showEnrollmentDialog` — a UI method — runs on the main thread in `onPostExecute` and reuses the same stored context for `AlertDialog.Builder`. The stored context is correct for resource lookups (`context.getString(...)`) but incorrect for dialog construction.
- `AdvancedSettings` is an `AppCompatActivity`-hosted `PreferenceFragmentCompat`. Its `getContext()` and `getActivity()` return an AppCompat-themed Context/Activity. At the time `PinCertificateEnrollmentFlow`'s constructor is called (line 449), the Fragment is attached and `getContext()` returns an Activity-backed Context. The `getApplicationContext()` strip inside the constructor discards this theme information.
- Any fix must ensure the AppCompat-themed Context is supplied to `AlertDialog.Builder` without being captured as a long-lived field that outlives the Activity lifecycle. Obtaining the themed Context at `onPostExecute` time (main thread, after async completes) via a `WeakReference<Activity>` or via the `DialogShower` callback's own access to `getActivity()` are both sound approaches.
- `AsyncTask` is deprecated as of API 30. This is a pre-existing condition introduced in U-009 and is out of scope for this bug fix. The fix must not change the async execution model; that is a separate refactoring concern.

## Supporting Documentation

- U-009: TLS pinned-certificate enrollment story (related_items)
- `CNTR-MODERNIZATION-002`: Affirmative consent requirement (referenced in `PinCertificateEnrollmentFlow` Javadoc)

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Introduced in sprint-001 during U-009 implementation. The application context anti-leak pattern was applied correctly for the async fetch but was not adjusted for the dialog construction path, which runs on the main thread in `onPostExecute` and requires an Activity-themed Context for the AppCompat dialog variant.

## Suggested Approach (Solution Architect — advisory)

Advisory only — refined during story planning. The notes below are grounded in a direct read of `PinCertificateEnrollmentFlow.java` and `AdvancedSettings.java`; they are not binding tasks or acceptance criteria.

**Root fix.** The crash is structural, not incidental: an `androidx.appcompat.app.AlertDialog` (imported at `PinCertificateEnrollmentFlow.java:26`) is built with `this.context`, which is `getApplicationContext()` (`:103`). The application context carries the base application theme, not a `Theme.AppCompat` descendant, so `AppCompatDelegateImpl.createSubDecor` throws when `dialog.show()` runs. The dialog must be constructed against an AppCompat-themed Activity context. `AdvancedSettings` is the only place that has one — `getActivity()` in the `DialogShower` anonymous class (`AdvancedSettings.java:468-475`) returns the AppCompat-themed Activity.

Three options, in recommended order:

- **(A) Recommended — move dialog construction into the themed Activity.** Change the `DialogShower` contract (`PinCertificateEnrollmentFlow.java:132-134`) so the flow passes the *cert display data* (subject DN, issuer DN, fingerprint, expiry, title/button strings, and the positive/negative click semantics) rather than a pre-built `AlertDialog`. `AdvancedSettings` then builds `new AlertDialog.Builder(getActivity())...` and shows it at `:471`. This keeps the flow entirely Activity-free — it continues to hold only `getApplicationContext()` (still correct for the `context.getString(...)` lookups at `:322`, `:352-355`, `:385`, `:401`) — and relocates the one operation that genuinely needs an Activity theme into the one component that owns one. It preserves the anti-leak intent that motivated the `getApplicationContext()` strip in the first place. The trade-off is that the click-handler wiring (`storeCertOnConfirm` on Trust, `listener.onCancelled()` on Cancel) must cross the contract boundary — pass them as a small parameter object or as the listener callbacks the flow already owns, so the store-write logic stays in the flow and only the dialog shell moves.

- **(B) Pass a themed `Context` (or `ContextThemeWrapper`) at show-time.** Supply the themed context to `showEnrollmentDialog` / the builder only inside `onPostExecute` (main thread), obtained fresh from the caller at that moment, and never stored on the instance or the task. Sound, but it threads a `Context` parameter through `showEnrollmentDialog` purely for theming and keeps dialog construction in the flow.

- **(C) Build with an Activity context obtained at show-time** via the `DialogShower` (e.g., the shower hands back `getActivity()` for the builder). Functionally close to (A) but leaves construction split across both classes; (A) is cleaner.

Recommend **(A)**.

**Anti-leak trade-off (binding on any option).** The `getApplicationContext()` strip at `:103` exists deliberately: the flow outlives nothing dangerous, but `FetchCertTask` runs an async TLS handshake and must not pin an Activity across it. Any fix may touch a themed Activity context **only on the main thread inside `onPostExecute`** (which is where `showEnrollmentDialog` is already invoked, `:280`) and must **not** retain that context as a field on `PinCertificateEnrollmentFlow` or `FetchCertTask` across the async boundary. Option (A) satisfies this for free, because the themed context never enters the flow at all.

**Verified nuance — do not "fix" the guard.** The `getActivity() != null && !getActivity().isFinishing()` guard at `AdvancedSettings.java:471` does **not** and cannot prevent this crash: the `IllegalStateException` is thrown from inside `dialog.show()` → `AlertDialog.onCreate()` → `AppCompatDelegateImpl.createSubDecor()`, after the guard has already passed. The defect is the context used to *construct* the dialog, not a null/finishing Activity at show-time. Leave the guard as-is and fix the construction.

**Scope and preservation.** This is a single story, isolated to `PinCertificateEnrollmentFlow` and the `DialogShower` interface, with the matching call-site update in `AdvancedSettings` (and any `DialogShower` test double, if one exists — verify before assuming). It must preserve the U-009 acceptance criteria already enumerated above: Trust stores the cert via `storeCertOnConfirm` (`:374`/`:394`); Cancel writes nothing (`:362-368`); Cancel remains the safe (non-default-focused) button — keep `.setNegativeButton` ahead of `.setPositiveButton` in the builder chain (`:362`/`:369`); and all four cert fields (subject, issuer, fingerprint, expiry) remain shown (`:352-355`). Because the failure is a theme/context error surfaced only at real dialog inflation, **on-device verification is mandatory** (emulator-5554 API 37 plus a physical device if available); unit tests with a `DialogShower` double will not exercise `AppCompatDelegateImpl` and will not catch a regression of this class.
