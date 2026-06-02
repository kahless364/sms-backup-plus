---
type: story
status: planned
sprint: "000001"
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-002
design_docs:
  - DES-MODERNIZATION-002
integration_contracts:
  - CNTR-MODERNIZATION-002
depends_on:
  - U-008
dependencies:
  - U-008
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-009
title: 'TLS enrollment UI: pin-cert preference action, one-time security notice, and string cleanup'
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-009: TLS Enrollment UI — Pin-Cert Preference Action, One-Time Security Notice, and String Cleanup

## Story

As a developer maintaining the SMS Backup+ app,
I want the `AdvancedSettings$Server` screen to expose a user-initiated pin-certificate action
(replacing the removed `server_trust_all_certificates` checkbox), implement the one-time
`transport_security_notice` presentation for users previously affected by the silent
security-downgrade migration, and scrub the trust-all checkbox strings from all 13 locales,
so that the settings UI truthfully represents the app's security posture and gives
self-hosted-IMAP users a consensual, informed path to relaxed validation without any code path
that silently degrades TLS protection.

## Acceptance Criteria

- [ ] **AC-1 — `server_trust_all_certificates` checkbox removed from preferences.xml**

  Given `app/src/main/res/xml/preferences.xml`,
  when a developer opens the file,
  then the `<CheckBoxPreference android:key="server_trust_all_certificates">` element that
  currently occupies lines 54-58 does not exist in the file; no `CheckBoxPreference` or any
  other preference element references the key `server_trust_all_certificates` in
  `preferences.xml`; and the `AdvancedSettings$Server` fragment's preference screen no longer
  presents a trust-all checkbox when the app is launched.

- [ ] **AC-2 — Trust-all strings deleted from all 13 locale string files**

  Given all `res/values*/strings.xml` files (the 12 verified locale files:
  `values-da`, `values-de`, `values-fr`, `values-gl`, `values-ko`, `values-nl`, `values-pl`,
  `values-pt-rPT`, `values-sv`, `values-tr`, `values-zh-rCN`, `values-zh-rTW`, plus the
  default `values` file),
  when a developer greps for `ui_protocol_trust_all_certificates_label` or
  `ui_protocol_trust_all_certificates_desc` across all files in `app/src/main/res/`,
  then zero matches are returned; the lint build (`./gradlew lint`) reports no missing-string
  or unused-string error caused by these removals.

- [ ] **AC-3 — New "Pin a certificate" preference action added to `AdvancedSettings$Server`**

  Given the `AdvancedSettings$Server` fragment (`AdvancedSettings.java:347-368`) with a
  configured `SERVER_ADDRESS` in `AuthPreferences`,
  when a user navigates to Advanced Settings > Server in the running app,
  then a `Preference` (or `PreferenceCategory`-contained `Preference`) with the key
  `pin_server_certificate` is present in that screen, rendered as an action-style preference
  (not a checkbox), with title text sourced from a new string resource
  `ui_protocol_pin_certificate_action` (value: "Pin server certificate") and summary text
  sourced from `ui_protocol_pin_certificate_action_desc`
  (value: "Required for servers with a self-signed or private CA certificate"); tapping the
  preference launches the enrollment flow described in AC-4 through AC-7.

- [ ] **AC-4 — Enrollment flow fetches and displays the leaf certificate before consent**

  Given the user has tapped the "Pin server certificate" preference and the app successfully
  opens a TLS connection to the host and port stored in `AuthPreferences.SERVER_ADDRESS`
  (resolved to default port 993 if no explicit port is present),
  when the enrollment flow executes,
  then: (a) a dialog or full-screen view is presented to the user before any consent control
  is actionable, displaying all four mandatory fields — subject DN (including SANs if present),
  issuer DN, SHA-256 fingerprint formatted as uppercase-hex colon-separated octets
  (e.g., `AB:CD:...`, 95 characters for a 32-octet hash), and the expiry date in the device
  locale's medium date format; (b) no data is read from or written to the IMAP server during
  the TLS fetch — the connection is used solely to capture the leaf certificate and is closed
  immediately after; (c) the `PinnedCertStore.store(...)` method has not been called at this
  point.

- [ ] **AC-5 — Affirmative consent is required; cancel leaves the store unwritten**

  Given the enrollment dialog is presented with the certificate details visible (AC-4),
  when the user takes any action other than tapping the explicit "Trust this certificate"
  control (i.e., taps Cancel, presses Back, or dismisses the dialog),
  then `PinnedCertStore.store(...)` is not called; `PinnedCertStore.isPinned(host, port)`
  returns `false` for that host after dismissal; and the `AdvancedSettings$Server` screen shows
  no persistent indicator for that host.
  Additionally, when a developer inspects the "Trust this certificate" button in the dialog,
  the button must not be the default-focused control — the Cancel/dismiss path is the safe
  default.

- [ ] **AC-6 — Confirmed enrollment stores the certificate and updates the persistent indicator**

  Given the enrollment dialog is showing the certificate details for `host:port`,
  when the user taps "Trust this certificate",
  then: (a) `PinnedCertStore.store(host, port, enrolledCertificate)` is called exactly once
  with the same `EnrolledCertificate` object that was displayed in the dialog (same
  `sha256Fingerprint`); (b) `PinnedCertStore.isPinned(host, port)` returns `true` immediately
  after the call returns; (c) the `AdvancedSettings$Server` screen shows a persistent summary
  label on the `pin_server_certificate` preference reading
  "Relaxed validation active — pinned certificate for {host}" (where `{host}` is the resolved
  hostname); (d) a "Remove pinned certificate" action is visible (a separate `Preference` or
  a secondary button within the same preference), tapping which calls
  `PinnedCertStore.clear(host, port)` and removes the summary label.

- [ ] **AC-7 — No automated code path may write PinnedCertStore (negative boundary test)**

  Given a fresh app install, an app upgrade from any prior version, an app launch on a device
  that previously ran the old `AuthPreferences.migrate()` (with stale
  `SERVER_TRUST_ALL_CERTIFICATES=true`), and any permutation of stored `SERVER_PROTOCOL`
  values (`+ssl`, `+tls`, `+ssl+`, `+tls+`, `ssl`, `tls`, or any custom value),
  when `App.onCreate()`, `Preferences.migrate()`, `AuthPreferences.migrate()`, preference
  fragment `onCreatePreferences`, and preference fragment `onResume` each execute (tested
  individually via Robolectric),
  then `PinnedCertStore.isPinned(host, port)` returns `false` for all hosts after each
  execution path, and the mock or spy on `PinnedCertStore.store(...)` records zero invocations.
  This test MUST be committed alongside the enrollment UI code and MUST pass in CI before
  any story depending on `PinnedCertStore` is merged.

- [ ] **AC-8 — One-time transport-security notice shown once for the affected cohort and never again**

  Given a device whose `AuthPreferences.migrate()` has set the `transport_security_notice_shown`
  flag to the pending state (i.e., `"transport_security_notice_shown"` is absent from or
  `false` in the default `SharedPreferences`), which occurs for users who were on `+ssl`/`+tls`
  protocol or carried a stale `SERVER_TRUST_ALL_CERTIFICATES=true` before migration (per
  DES-MODERNIZATION-002 Decision 6 and CNTR-MODERNIZATION-002 §TransportSecurityNoticePending),
  when the first UI `Activity` (or hosting fragment) reaches `onResume()` after `migrate()`
  has run on this launch,
  then: (a) a non-blocking, clearly titled notice is presented to the user (dialog, snackbar,
  or info card) containing all three required content points: (i) the connection will now use
  certificate validation, (ii) self-hosted/private-CA users can enroll a pinned certificate
  via Settings > Advanced Settings > Server, and (iii) no action is required for Gmail or
  other servers with publicly trusted certificates; (b) immediately after the notice is shown,
  `"transport_security_notice_shown"` is written as `true` in default `SharedPreferences`;
  (c) on the next app launch, `"transport_security_notice_shown"` is `true`, the notice is not
  shown again, and no additional write to `"transport_security_notice_shown"` occurs.

- [ ] **AC-9 — One-time notice is never shown to unaffected users**

  Given a user whose stored `SERVER_PROTOCOL` was not `+ssl` or `+tls` at the time of
  migration (already on validated TLS, using OAuth2, or using any non-downgrade-trigger
  protocol value), and whose `SERVER_TRUST_ALL_CERTIFICATES` preference was `false` (the
  default, never silently downgraded),
  when `App.onCreate()` runs `migrate()` and the first UI Activity reaches `onResume()`,
  then `"transport_security_notice_shown"` remains absent from default `SharedPreferences`
  (the flag was never set to pending, so it is never set to shown), and no notice dialog,
  snackbar, or card is presented to the user.

- [ ] **AC-10 — Pin-cert strings added to default locale; lint build passes**

  Given `app/src/main/res/values/strings.xml`,
  when a developer opens the file,
  then the following four string resources exist with exactly the specified values:
  `ui_protocol_pin_certificate_action` = "Pin server certificate";
  `ui_protocol_pin_certificate_action_desc` = "Required for servers with a self-signed or private CA certificate";
  `ui_protocol_pin_certificate_trust_button` = "Trust this certificate";
  `ui_protocol_pin_certificate_remove` = "Remove pinned certificate";
  and `./gradlew lint` (or `./gradlew lintDebug`) exits with code 0 with no
  `MissingTranslation` errors attributed to these new keys (new keys fall back to the default
  locale until translated — this is intentional and must not be configured as a
  `warningsAsErrors` violation for translation gaps introduced in this story).

### Integration Criteria

- [ ] **IC-1 — `pin_server_certificate` preference is wired to the enrollment flow entry point**

  Given the `AdvancedSettings$Server.onCreatePreferences` or `onResume` setup,
  when `findPreference("pin_server_certificate")` is called,
  the returned `Preference` object is non-null, and its `OnPreferenceClickListener` invokes
  the certificate-fetch-and-display flow described in AC-4 when triggered; this listener is
  set in `AdvancedSettings.java` in the `Server` fragment class body, not in a base class.

- [ ] **IC-2 — `transport_security_notice_shown` flag is consumed by exactly one presentation site**

  Given a grep for `transport_security_notice_shown` across `app/src/main/java/`,
  when the results are reviewed,
  then the flag is read in exactly one location (the notice-presentation consumer), written as
  `true` in exactly one location (immediately after the notice is presented), and written as
  `false`/pending in exactly one location (`AuthPreferences.migrate()` for the affected
  cohort); no other class reads or writes this key.

- [ ] **IC-3 — `PinnedCertStore.clear(host, port)` call site is the "Remove" action only**

  Given a grep for `PinnedCertStore` across `app/src/main/java/`,
  when every call site of `.clear(` is reviewed,
  then the only production call site is the "Remove pinned certificate" `Preference` click
  listener inside `AdvancedSettings$Server`; no other class calls `clear(...)`.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/res/xml/preferences.xml` (lines 54-58) | Contains `<CheckBoxPreference android:key="server_trust_all_certificates">` with label and description strings | Remove the `CheckBoxPreference` element entirely; add `<Preference android:key="pin_server_certificate">` with new string resources in its place |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java` (`Server` class, lines 347-368) | `Server` fragment has `onCreatePreferences` and `onResume`; `onResume` wires only the `IMAP_PASSWORD` listener | Add `OnPreferenceClickListener` for `pin_server_certificate` that launches enrollment; add `updatePinCertIndicator()` helper to refresh the summary label based on `PinnedCertStore.isPinned`; add "Remove" action listener |
| `app/src/main/res/values/strings.xml` (lines 90-91) | Contains `ui_protocol_trust_all_certificates_label` and `ui_protocol_trust_all_certificates_desc` | Remove those two string entries; add `ui_protocol_pin_certificate_action`, `ui_protocol_pin_certificate_action_desc`, `ui_protocol_pin_certificate_trust_button`, `ui_protocol_pin_certificate_remove` |
| `app/src/main/res/values-da/strings.xml` | Contains `ui_protocol_trust_all_certificates_label` and/or `_desc` | Remove matching entries; no new strings required (fallback to default locale) |
| `app/src/main/res/values-de/strings.xml` | Same as above | Same removal |
| `app/src/main/res/values-fr/strings.xml` | Same as above | Same removal |
| `app/src/main/res/values-gl/strings.xml` | Same as above | Same removal |
| `app/src/main/res/values-ko/strings.xml` | Same as above | Same removal |
| `app/src/main/res/values-nl/strings.xml` | Same as above | Same removal |
| `app/src/main/res/values-pl/strings.xml` | Same as above | Same removal |
| `app/src/main/res/values-pt-rPT/strings.xml` | Same as above | Same removal |
| `app/src/main/res/values-sv/strings.xml` | Same as above | Same removal |
| `app/src/main/res/values-tr/strings.xml` | Same as above | Same removal |
| `app/src/main/res/values-zh-rCN/strings.xml` | Same as above | Same removal |
| `app/src/main/res/values-zh-rTW/strings.xml` | Same as above | Same removal |
| One-time-notice presentation site (new, exact file TBD by developer — may be `MainActivity` or a dedicated `NoticePresenter` helper) | No notice logic exists | Add `consumeTransportSecurityNotice()` call in the first resuming Activity's `onResume`; show notice if pending; persist shown |

New files introduced by this story (in addition to `PinnedCertStore` and `EnrolledCertificate`
which are introduced by U-008):

| New File | Purpose |
|----------|---------|
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/PinCertificateEnrollmentFlow.java` (or equivalent) | Orchestrates TLS fetch, builds `EnrolledCertificate`, shows the enrollment dialog, invokes `PinnedCertStore.store(...)` on confirmation |
| `app/src/test/java/com/zegoggles/smssync/activity/fragments/AdvancedSettingsServerTest.java` | Unit / Robolectric tests for AC-5, AC-6, AC-7, AC-8, AC-9 |

## Existing Behavior to Preserve

- The `AdvancedSettings$Server` fragment's `onResume` must continue to set the
  `OnPreferenceChangeListener` on `AuthPreferences.IMAP_PASSWORD` (current lines 360-366);
  this listener is not related to TLS and must not be removed.
- `AuthPreferences.SERVER_ADDRESS` is read-only within this story; no change to how the
  server address is stored, parsed, or displayed in the Server fragment.
- The `server_protocol` `ListPreference` (`preferences.xml:46-52`) and its strings are
  untouched.
- All other `AdvancedSettings` inner fragments (`Backup`, `Restore`, `Advanced`) are not
  modified.
- Gmail users (OAuth2 path; `useXOAuth()` returns `true`) have no trust-all flag to clear
  and see neither the enrollment preference nor the one-time notice; this invariant must not
  be broken.
- The `Preferences.java:247-253` one-time-seen pattern for `SMS_DEFAULT_PACKAGE_CHANGE_SEEN`
  must be left untouched; the new `transport_security_notice_shown` key mirrors it in a
  separate implementation.

## Verification Steps

1. **AC-1 — Checkbox absent from XML**
   Open `app/src/main/res/xml/preferences.xml`. Confirm that no element has
   `android:key="server_trust_all_certificates"`. Build the app with `./gradlew assembleDebug`
   and confirm the build succeeds. Launch the app on an emulator (API 21+), navigate to
   Advanced Settings > Server; confirm no "Trust all certificates" checkbox is visible.

2. **AC-2 — String removal grep**
   Run `grep -rn "ui_protocol_trust_all_certificates" app/src/main/res/` from the repo root.
   Expected: zero matches. Run `./gradlew lint`; confirm exit code 0 with no errors related
   to missing string keys or resources.

3. **AC-3 — New preference action visible**
   Launch the app on an emulator (API 21+). Navigate to Advanced Settings > Server. Confirm a
   preference with title "Pin server certificate" is present in the list. Confirm it does not
   display a checkbox or toggle. Confirm its summary reads
   "Required for servers with a self-signed or private CA certificate".

4. **AC-4 — Enrollment dialog shows all four fields**
   Configure a test IMAP server address in settings (or use a local test server whose
   certificate is known). Tap "Pin server certificate". Before any Trust/Cancel button is
   tappable, confirm the dialog shows: Subject DN, Issuer DN, a SHA-256 fingerprint formatted
   as colon-separated uppercase hex pairs (verify the length: 95 characters), and an expiry
   date in human-readable form. Verify via test: inject a mock `PinnedCertStore`; call the
   enrollment flow up to the point just before the confirmation dialog is actionable; assert
   `store(...)` has zero invocations.

5. **AC-5 — Cancel leaves store empty**
   From the enrollment dialog (AC-4), tap Cancel (or press Back). Confirm the dialog dismisses.
   Navigate back to the Server settings screen. Confirm the `pin_server_certificate` preference
   shows no "Relaxed validation active" summary. Via Robolectric test: invoke enrollment flow,
   deliver a certificate, then simulate Cancel; assert `PinnedCertStore.store(...)` was never
   called.

6. **AC-6 — Confirmation stores and indicator appears**
   From the enrollment dialog (AC-4), tap "Trust this certificate". Confirm the dialog
   dismisses. Confirm the `pin_server_certificate` preference summary changes to
   "Relaxed validation active — pinned certificate for {host}". Confirm a "Remove pinned
   certificate" preference or button is visible. Tap "Remove pinned certificate"; confirm the
   summary reverts to `ui_protocol_pin_certificate_action_desc`. Via unit test: assert
   `PinnedCertStore.store(host, port, cert)` was called exactly once with the same fingerprint
   displayed in the dialog; assert `PinnedCertStore.isPinned(host, port)` is `true`; assert
   tapping Remove calls `PinnedCertStore.clear(host, port)`.

7. **AC-7 — Negative test: no automated write to PinnedCertStore**
   In `AdvancedSettingsServerTest` (Robolectric): create a spy/mock of `PinnedCertStore`.
   Execute each of the following in isolation: `App.onCreate()`, `Preferences.migrate()`,
   `AuthPreferences.migrate()` (with `SERVER_PROTOCOL` = `+ssl`), `AuthPreferences.migrate()`
   (with `SERVER_PROTOCOL` = `+tls`), `Server.onCreatePreferences(null, null)`,
   `Server.onResume()`. After each: assert `store(...)` invocation count is zero. This test
   must be present and green in CI before this story's PR is merged.

8. **AC-8 — Notice shown once for affected cohort**
   In a Robolectric test: set up `SharedPreferences` with
   `SERVER_PROTOCOL = "+ssl"` and no `transport_security_notice_shown` key; run
   `AuthPreferences.migrate()`; assert `transport_security_notice_shown` is now `false`
   (pending). Simulate `Activity.onResume()`; assert the notice dialog/card is shown. Assert
   `transport_security_notice_shown` is now `true`. Simulate a second `onResume()` on a new
   instance; assert no dialog/card is shown and `transport_security_notice_shown` remains
   `true` with no additional writes.

9. **AC-9 — Notice not shown to unaffected cohort**
   In a Robolectric test: set up `SharedPreferences` with `SERVER_PROTOCOL = "+ssl+"` and
   `SERVER_TRUST_ALL_CERTIFICATES = false`; run `AuthPreferences.migrate()`; assert
   `transport_security_notice_shown` is absent from `SharedPreferences`. Simulate
   `Activity.onResume()`; assert no notice dialog/card is presented.

10. **AC-10 — New strings present; lint clean**
    Open `app/src/main/res/values/strings.xml`. Confirm the four new string resources are
    present with the exact values specified in AC-10. Run `./gradlew lint`; confirm exit code 0
    with no `MissingTranslation` errors for the new keys.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java) | Remove trust-all checkbox from `preferences.xml`; add `pin_server_certificate` preference; implement `PinCertificateEnrollmentFlow` (TLS fetch, dialog, `PinnedCertStore` write on confirm); implement one-time notice presentation in first UI Activity; string additions and removals in all 13 locale files | Developer |

## Technical Notes

**Scope boundary with U-008.** U-008 delivers `PinnedCertStore`, `EnrolledCertificate`,
`PinnedCertificateSocketFactory`, and `TlsTrustPolicy`. U-009 consumes those types; it must
not re-implement them. The enrollment flow in U-009 calls `PinnedCertStore.store(host, port,
EnrolledCertificate)` per CNTR-MODERNIZATION-002's `PinnedCertificateEnrolled` event
semantics. U-009 depends on U-008 being merged first (`depends_on: U-008`).

**Certificate fetch during enrollment.** The enrollment flow opens a
`javax.net.ssl.SSLSocket` (or equivalent) to the configured `SERVER_ADDRESS:port`, calls
`getSession().getPeerCertificates()[0]` to obtain the leaf, then closes the socket. No IMAP
command is sent; this is a TLS handshake only. The fetch uses an ephemeral
`TrustManager` that accepts any cert (for display purposes only, within the enrollment flow
and not on any data connection); the accepted cert is then displayed for out-of-band
verification before the user decides whether to pin it. This "trust anything for display, trust
only the user-verified cert for data" pattern is the standard Android pinning enrollment UX.

**Key normalization for `PinnedCertStore`.** The host key passed to `PinnedCertStore.store`
must be normalized to lowercase ASCII. The effective port must be resolved before keying:
if `SERVER_ADDRESS` contains no explicit port, use 993 (IMAP over TLS default). See
CNTR-MODERNIZATION-002 §"Key format (normative)".

**One-time notice flag semantics.** The flag key `"transport_security_notice_shown"` lives in
the default `SharedPreferences` (obtained via `PreferenceManager.getDefaultSharedPreferences`),
consistent with the analogue `SMS_DEFAULT_PACKAGE_CHANGE_SEEN` pattern at
`Preferences.java:247-253`. The `AuthPreferences.migrate()` rewrite (U-007) writes the flag
to the pending state; this story only reads and consumes it. Do not add a write to the pending
state in this story's code.

**String removal and lint.** Before removing any locale string, verify it is not referenced
from any source other than the `server_trust_all_certificates` preference element (which is
itself being removed). Run `./gradlew lint` after removals. If `warningsAsErrors` is enabled
for `MissingTranslation`, confirm the new pin-cert strings are exempt via an
`<issue id="MissingTranslation" severity="ignore">` entry scoped to the four new keys, or by
adding a `translatable="false"` attribute until translations are contributed — do not silently
break the lint gate.

**Persistent indicator refresh.** The `pin_server_certificate` preference summary and the
"Remove" action must reflect the current `PinnedCertStore.isPinned(host, port)` state on every
`onResume()` of the `Server` fragment. Call a helper such as `updatePinCertIndicator()` at the
end of `Server.onResume()` to ensure the indicator is accurate after the user navigates away
and returns.

**No AllTrustedSocketFactory in enrollment.** Although the ephemeral certificate-fetch uses a
lenient `TrustManager`, it MUST NOT use the deleted `AllTrustedSocketFactory`. The lenient
trust is implemented inline within the enrollment flow only (e.g., an anonymous
`X509TrustManager` whose sole purpose is to capture and return the presented leaf certificate)
and must not be reused or accessible outside the enrollment code path.

## Estimation

Medium (3-5 developer-days). The enrollment dialog, TLS-fetch code, persistent-indicator
logic, one-time-notice presentation, and 13-file string cleanup are individually straightforward;
the complexity lies in integrating them with the correct scoping (only the enrollment path
writes the store), verifying the negative test (AC-7), and confirming lint cleanliness across
13 locale files.

## Supporting Documentation

- REQ-MODERNIZATION-002#ac-4 — Consensual user-initiated pinned-cert path (entry point, display fields, affirmative consent, persist, scope, no-auto, persistent indicator)
- REQ-MODERNIZATION-002#ac-6 — One-time notice for previously-affected users
- REQ-MODERNIZATION-002#ac-10 — SERVER_TRUST_ALL_CERTIFICATES writable only via enrollment
- DES-MODERNIZATION-002#decision-3 — Pinned-certificate enrollment flow design (steps 1-7)
- DES-MODERNIZATION-002#decision-4 — Remove trust-all preference key and UI
- DES-MODERNIZATION-002#decision-6 — Rewrite migrate(); one-time notice (one-time notice flag is set by U-007, consumed here)
- DES-MODERNIZATION-002 §"Pinned-cert requires explicit action" — negative test obligation
- CNTR-MODERNIZATION-002#interface-pinnedcertstore — `store`, `retrieve`, `isPinned`, `clear` signatures
- CNTR-MODERNIZATION-002#data-shape-enrolledcertificate — mandatory display fields and persistence encoding
- CNTR-MODERNIZATION-002#event-pinnedcertificateenrolled — affirmative-consent requirements
- CNTR-MODERNIZATION-002#event-transportsecuritynoticepending — flag key, set-by, consumed-by, shown-at-most-once semantics
- CNTR-MODERNIZATION-002#validation-rules — rules 1 (single write path), 2 (never automatic), 3 (display-before-consent), 6 (read-only consumer)

## Integration Contract References

- CNTR-MODERNIZATION-002#interface-pinnedcertstore — `PinnedCertStore.store(host, port, EnrolledCertificate)` called by enrollment confirmation handler; `PinnedCertStore.isPinned(host, port)` read by `updatePinCertIndicator()`; `PinnedCertStore.clear(host, port)` called by Remove action
- CNTR-MODERNIZATION-002#event-pinnedcertificateenrolled — the confirming control name ("Trust this certificate"), the cancel-is-safe-default requirement, the display-before-consent precondition, and the single `store(...)` call
- CNTR-MODERNIZATION-002#event-transportsecuritynoticepending — `"transport_security_notice_shown"` flag key, read and cleared (written `true`) by the notice presenter implemented in this story
- CNTR-MODERNIZATION-001 — (upstream of this story, delivered by U-008) the resolved `TrustedSocketFactory` that this enrollment makes available; U-009 does not directly touch factory selection but its `PinnedCertStore.store(...)` write is what causes CNTR-001's resolver to return `PINNED_CERTIFICATE` at the next connect

## Notes

Story scope is the settings UI layer and one-time notice presentation only. The underlying `PinnedCertStore`, `EnrolledCertificate`, `PinnedCertificateSocketFactory`, and `TlsTrustPolicy` types that this story consumes are the responsibility of U-008. Not finalized — frontmatter fields (status, pipeline, platforms, gate_additions) are Artifact-Librarian-owned.
