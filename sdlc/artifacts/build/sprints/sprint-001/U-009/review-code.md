---
artifact_type: review-code
story_id: U-009
verdict: PASS
agent: Developer
timestamp: "2026-06-03T00:00:00Z"
---

# Code Review: U-009

## Summary

Code is functionally correct, follows existing patterns, and satisfies all acceptance criteria
within scope. Three minor notes recorded below; none are blocking.

## Review Findings

### PASS — AC-1: CheckBoxPreference removed

`preferences.xml` no longer contains any element referencing `server_trust_all_certificates`.
New `pin_server_certificate` and `remove_pinned_certificate` preferences are present.

### PASS — AC-2: Trust-all strings removed from all 13 locale files

`grep -rn "ui_protocol_trust_all_certificates" app/src/main/res/` returns zero matches.
`MissingTranslation` is globally suppressed in `app/lint.xml` so no lint error.

### PASS — AC-3: New "Pin a certificate" preference present in Server screen

`preferences.xml:54-58` contains `<Preference android:key="pin_server_certificate">` with
correct string resource references.

### PASS — AC-4: Enrollment flow architecture

`PinCertificateEnrollmentFlow` uses an `EnrollmentCaptureTrustManager` (anonymous inner class) to
capture the leaf cert during TLS handshake. This is NOT `AllTrustedSocketFactory`. The ephemeral
trust manager is scoped to this class only. No IMAP data is exchanged (TLS handshake only, socket
immediately closed).

### PASS — AC-5: Cancel leaves PinnedCertStore unwritten

`showEnrollmentDialog()` builds the dialog without writing to the store. `store()` / `put()` is
called only from `storeCertOnConfirm()` which is invoked only by the positive button
`OnClickListener`. Cancel path calls `listener.onCancelled()` with no store write.

### PASS — AC-6: Confirmation stores and indicator updates

`storeCertOnConfirm()` calls `pinnedCertStore.put(host.toLowerCase(Locale.US), port, cert)` once.
`updatePinCertIndicator()` is called from `onResume()` and after enrollment/removal.
`remove_pinned_certificate` preference `setVisible(false)` when no pin, `setVisible(true)` when pinned.

### PASS — AC-7: Negative invariant — no automated store write

AC-7 tests are in both `AdvancedSettingsServerTest` (fresh-store assertions) and
`AuthPreferencesTest` (migrate-path assertions, requires package-private `migrate()`).
`PinnedCertStore.PREFS_NAME` file is empty after any migration path.

### PASS — AC-8/IC-2: One-time notice shown once

`TransportSecurityNoticeHelper.checkAndClearNoticePending()` is the only reader; writes `false`
exactly once. Flag `true` only from `AuthPreferences.migrate()`. Single site pattern verified.

### PASS — AC-9: Notice not shown to unaffected users

Tests confirm: `transport_security_notice_pending` absent from prefs → `checkAndClearNoticePending()`
returns `false` → no dialog.

### PASS — IC-1: Listener set in Server fragment body

`findPreference(KEY_PIN_CERT).setOnPreferenceClickListener()` is set in `Server.onResume()`,
which is in the `Server` fragment class body (not a base class).

### PASS — IC-3: `remove()` call site is the Remove action only

Only one call to `pinnedCertStore.remove()` in production code; it is in the Remove action
`OnPreferenceClickListener` in `AdvancedSettings.Server.onResume()`.

## Minor Notes (non-blocking)

1. **`displayHost` variable in `launchEnrollmentFlow()` is unused** — declared but not used in the
   listener. Should be cleaned up in a follow-on, but does not affect correctness.

2. **`AsyncTask` deprecation** — `PinCertificateEnrollmentFlow.FetchCertTask` uses `AsyncTask` which
   is deprecated in API 30+. The existing codebase uses `AsyncTask` in other places; migration to
   coroutines/Executor is out of scope for this story (DES-MODERNIZATION-005 addresses this).

3. **`AuthPreferences.TRANSPORT_SECURITY_NOTICE_PENDING` duplicated in `TransportSecurityNoticeHelper`** —
   The string constant `"transport_security_notice_pending"` is replicated because the original is
   package-private. A comment explains the deliberate design decision. No runtime risk.
