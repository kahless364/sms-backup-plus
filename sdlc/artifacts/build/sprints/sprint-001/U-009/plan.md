---
artifact_type: plan
story_id: U-009
verdict: PASS
agent: Developer
timestamp: "2026-06-03T00:00:00Z"
---

# Plan: U-009 TLS Enrollment UI + One-Time Security Notice

## Approach

Implement in six parts:

1. **preferences.xml** — Remove `<CheckBoxPreference android:key="server_trust_all_certificates">`.
   Add `<Preference android:key="pin_server_certificate">` and `<Preference android:key="remove_pinned_certificate">`.

2. **strings.xml** — Remove `ui_protocol_trust_all_certificates_label/desc` from all 13 locale files.
   Add 4 required + 9 supporting string resources to `values/strings.xml`.

3. **PinCertificateEnrollmentFlow.java** — New class in `activity/fragments/`.
   TLS-fetch via ephemeral capture TrustManager, cert display dialog, affirmative-consent enforcement,
   `PinnedCertStore.put()` on confirmation only.

4. **AdvancedSettings.Server** — Wire `pin_server_certificate` click listener (IC-1),
   `remove_pinned_certificate` click listener (IC-3), `updatePinCertIndicator()` helper (AC-6),
   called from `onResume()`. Preserve IMAP_PASSWORD listener.

5. **TransportSecurityNoticeHelper.java** — New class in `activity/`.
   `checkAndClearNoticePending()` reads/clears `transport_security_notice_pending` flag (set by U-007).
   `consumeTransportSecurityNotice()` wraps check+dialog show.
   Production entry in `MainActivity.onResume()`.

6. **Tests** — `AdvancedSettingsServerTest.java` (20 tests) + 9 new tests in `AuthPreferencesTest.java`.

## Key Design Decisions

- The U-008 `PinnedCertStore` uses `get/put/remove` (not `store/retrieve/isPinned/clear` per contract
  naming) — use actual API.
- The notice flag key is `transport_security_notice_pending` (as written by U-007), not
  `transport_security_notice_shown` (theoretical contract name). Value `true` = pending show.
- `checkAndClearNoticePending()` separated from `showNoticeDialog()` so flag-lifecycle can be tested
  without requiring an Activity context (Robolectric cannot show AppCompat dialogs from application context).
- `TransportSecurityNoticeHelper.KEY` is `public` to enable testing from other packages.
- `AuthPreferences.migrate()` tests for AC-7/AC-8/AC-9 live in `AuthPreferencesTest` (same package)
  since `migrate()` is package-private.
