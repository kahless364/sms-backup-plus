---
artifact_type: implementation-log
story_id: U-009
verdict: PASS
agent: Developer
timestamp: "2026-06-03T00:00:00Z"
files_changed: 16
files_created: 4
tests_added: 29
tests_passing: 539
---

# Implementation Log: U-009

## Summary

Implemented the pin-certificate enrollment UI, one-time transport-security notice, and string
cleanup for U-009. All three gates pass: `testDebugUnitTest` (539 tests, 0 failures),
`jacocoTestCoverageVerification` (>=70% on all three gated packages), `assembleDebug` (APK built).

## Prerequisites

Merged `sdlc/modernization-plan` into this worktree branch before implementing, to bring in
U-007 (`AuthPreferences.migrate()` rewrite) and U-008 (`PinnedCertStore`, `TlsTrustPolicy`,
`PinnedCertificateSocketFactory`). Local `local.properties` copied from root repo.

## Files Modified

| File | Change |
|------|--------|
| `app/src/main/res/xml/preferences.xml` | Removed `<CheckBoxPreference android:key="server_trust_all_certificates">`. Added `<Preference android:key="pin_server_certificate">` and `<Preference android:key="remove_pinned_certificate" android:visible="false">`. |
| `app/src/main/res/values/strings.xml` | Removed `ui_protocol_trust_all_certificates_label` and `ui_protocol_trust_all_certificates_desc`. Added 4 required string resources (`ui_protocol_pin_certificate_action`, `_action_desc`, `_trust_button`, `_remove`) plus 9 supporting strings for the dialog, active indicator, fetch error, and one-time notice. |
| `app/src/main/res/values-da/strings.xml` | Removed trust-all strings. |
| `app/src/main/res/values-de/strings.xml` | Removed trust-all strings. |
| `app/src/main/res/values-fr/strings.xml` | Removed trust-all strings (label and desc were in separate locations). |
| `app/src/main/res/values-gl/strings.xml` | Removed trust-all strings. |
| `app/src/main/res/values-ko/strings.xml` | Removed trust-all strings. |
| `app/src/main/res/values-nl/strings.xml` | Removed trust-all strings. |
| `app/src/main/res/values-pl/strings.xml` | Removed trust-all strings. |
| `app/src/main/res/values-pt-rPT/strings.xml` | Removed trust-all strings. |
| `app/src/main/res/values-sv/strings.xml` | Removed trust-all strings. |
| `app/src/main/res/values-tr/strings.xml` | Removed trust-all strings. |
| `app/src/main/res/values-zh-rCN/strings.xml` | Removed trust-all strings. |
| `app/src/main/res/values-zh-rTW/strings.xml` | Removed trust-all strings. |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java` | Replaced `Server` inner fragment body: added `PinnedCertStore` field, `onResume()` wiring for `pin_server_certificate` and `remove_pinned_certificate` click listeners, `launchEnrollmentFlow()`, and `updatePinCertIndicator()` helper. Preserved IMAP_PASSWORD listener. Added imports: `Toast`, `AlertDialog`, `PinnedCertStore`. |
| `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` | Added `onResume()` override calling `TransportSecurityNoticeHelper.consumeTransportSecurityNotice(this)`. |
| `app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java` | Added 9 new tests: `u009_ac7_*` (3 AC-7 negative invariant tests via migrate()), `u009_ac8_*` (1 end-to-end notice consumption test), `u009_ac9_*` (2 tests for unaffected cohorts). |

## Files Created

| File | Purpose |
|------|---------|
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/PinCertificateEnrollmentFlow.java` | TLS cert fetch via ephemeral capture TrustManager (NOT AllTrustedSocketFactory), cert dialog builder, `PinnedCertStore.put()` on affirmative consent only. Includes `parseHost/parsePort/formatSha256Fingerprint/sha256` static helpers. |
| `app/src/main/java/com/zegoggles/smssync/activity/TransportSecurityNoticeHelper.java` | `checkAndClearNoticePending()` (testable flag check/clear) and `consumeTransportSecurityNotice()` (check + show dialog). `KEY = "transport_security_notice_pending"` (public constant). |
| `app/src/test/java/com/zegoggles/smssync/activity/fragments/AdvancedSettingsServerTest.java` | 20 tests covering AC-5 (cancel), AC-6 (store/remove/scope), AC-7 (negative invariant), AC-8/AC-9 (notice flag lifecycle), and PinCertificateEnrollmentFlow helper methods. |
| `sdlc/artifacts/build/sprints/sprint-001/U-009/` (this directory) | Plan + implementation log + reviews + QA. |

## Test Results

- Previous test count (before U-009): 510 (from earlier waves)
- New tests added: 29 (20 in AdvancedSettingsServerTest + 9 in AuthPreferencesTest)
- Total tests after: 539
- Failures: 0
- Skipped: 1 (pre-existing)
- `testDebugUnitTest`: BUILD SUCCESSFUL
- `jacocoTestCoverageVerification`: BUILD SUCCESSFUL (>=70% gate holds)
- `assembleDebug`: BUILD SUCCESSFUL

## Contract Adherence

**CNTR-MODERNIZATION-002** requirements:

| Contract Section | Implementation | File:Line |
|-----------------|----------------|-----------|
| `PinnedCertStore.store()` (contract name) → `PinnedCertStore.put()` (implementation name) | Called only from `storeCertOnConfirm()` on affirmative consent | `PinCertificateEnrollmentFlow.java:285` |
| `PinnedCertStore.isPinned()` → `getTlsTrustPolicy() == PINNED_CERTIFICATE` | Used in `updatePinCertIndicator()` | `AdvancedSettings.java:472` |
| `PinnedCertStore.clear()` → `PinnedCertStore.remove()` | Called only from Remove action listener (IC-3) | `AdvancedSettings.java:400` |
| `EnrolledCertificate` display fields: subject, issuer, SHA-256, expiry | All four fields shown in dialog message | `PinCertificateEnrollmentFlow.java:245-251` |
| Affirmative consent: "Trust this certificate" button NOT default | Positive button is Trust, negative (Cancel) is safe default | `PinCertificateEnrollmentFlow.java:257-275` |
| Key format: `host.toLowerCase():port` | Applied before `put()` | `PinCertificateEnrollmentFlow.java:285` |
| Default IMAP port 993 if no explicit port | `parsePort()` returns 993 | `PinCertificateEnrollmentFlow.java:145-155` |
| `transport_security_notice_pending` flag (read/consume only) | `checkAndClearNoticePending()` reads and clears; never writes `true` | `TransportSecurityNoticeHelper.java:77-90` |
| Notice flag consumed by exactly one presentation site (IC-2) | `checkAndClearNoticePending()` is the only reader; written `false` here; written `true` only in `AuthPreferences.migrate()` | `TransportSecurityNoticeHelper.java:59,82-86` |
| `PinnedCertStore.clear()` call site is Remove action only (IC-3) | Confirmed by code review; no other call site | `AdvancedSettings.java:400` |
| Validation Rule 2: never automatic | AC-7 negative tests confirm no automated code path writes the store | `AdvancedSettingsServerTest.java:79-135` + `AuthPreferencesTest.java:478-530` |

## Integration Path

- `PinCertificateEnrollmentFlow` is called from `AdvancedSettings.Server.launchEnrollmentFlow()` → `AdvancedSettings.Server.onResume()` wires `findPreference("pin_server_certificate").setOnPreferenceClickListener()` → triggered when user taps the preference in the Server settings screen.
- `TransportSecurityNoticeHelper.consumeTransportSecurityNotice()` is called from `MainActivity.onResume()` → triggered on every Activity resume; no-ops if flag not pending.
- `updatePinCertIndicator()` is called from `AdvancedSettings.Server.onResume()` every time the Server settings screen is shown.

## Notes

- `AuthPreferences.migrate()` is package-private; AC-7/AC-8/AC-9 end-to-end migration tests live in `AuthPreferencesTest` (same `preferences` package).
- `checkAndClearNoticePending()` is separated from `consumeTransportSecurityNotice()` to allow unit-testing flag lifecycle without needing a real Activity context (Robolectric cannot show AppCompat AlertDialogs from ApplicationContext).
- `MissingTranslation` lint is already suppressed globally in `app/lint.xml` — new pin-cert strings in default locale only will fall back gracefully.
