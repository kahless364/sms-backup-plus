---
artifact_type: review-security
story_id: U-009
verdict: PASS
agent: Developer
timestamp: "2026-06-03T00:00:00Z"
---

# Security Review: U-009

## Summary

Security posture is improved by this story. No new attack surface is introduced. All required
invariants are maintained.

## Review Findings

### Ephemeral Enrollment TrustManager

`PinCertificateEnrollmentFlow.EnrollmentCaptureTrustManager` accepts any certificate during
the enrollment TLS handshake. This is intentional and bounded:

- The socket is used ONLY for the TLS handshake to capture the leaf certificate. No IMAP
  commands are sent; the socket is closed immediately after `startHandshake()`.
- The `EnrollmentCaptureTrustManager` is an anonymous inner class scoped exclusively to
  `PinCertificateEnrollmentFlow.fetchLeafCert()`. It is not accessible outside that method.
- It is NOT a resurrection of `AllTrustedSocketFactory`. The deleted class was a singleton
  reachable from data connections; this trust manager is ephemeral and display-only.
- Per DES-MODERNIZATION-002 §Technical Notes: "The ephemeral TrustManager…accepts any cert
  (for display purposes only, within the enrollment flow and not on any data connection)."

### No Auto-Write to PinnedCertStore (CWE-295 Invariant)

The critical security invariant — `PinnedCertStore.put()` is reachable ONLY from affirmative
user confirmation — is maintained:

- `storeCertOnConfirm()` is called only from the positive button `OnClickListener`.
- `App.onCreate()`, `Preferences.migrate()`, `AuthPreferences.migrate()`, `onCreatePreferences()`,
  and `onResume()` NONE of them reach `put()`.
- AC-7 negative tests confirm this: 5 tests in `AdvancedSettingsServerTest` + 3 in
  `AuthPreferencesTest` covering all migration paths.

### Host Scoping

`storeCertOnConfirm()` normalizes host to lowercase ASCII before passing to `put()`:
`host.toLowerCase(Locale.US)`. The same normalization is applied in `updatePinCertIndicator()`
and the Remove listener. Pin for `imap.selfhosted.org:993` does NOT affect `imap.gmail.com:993`
(verified by `ac6_pinScopedToHost_doesNotAffectOtherHosts` test).

### One-Time Notice Flag

`TransportSecurityNoticeHelper` reads and clears `transport_security_notice_pending` only.
It does NOT write the certificate store, does NOT set `PINNED_CERTIFICATE` policy, and does
NOT interact with `PinnedCertStore` in any way. CNTR-MODERNIZATION-002 Validation Rule 6 is
satisfied.

### Notice Flag Set to False Before Dialog Display

The flag is cleared (`apply()`) before the dialog is shown, preventing a theoretical re-show
if the process is killed while the dialog is open. This is the correct order (clear-before-show
rather than clear-after-dismiss).

### SHA-256 Fingerprint Display

The fingerprint is formatted as uppercase colon-separated hex pairs (e.g., `AB:CD:...`, 95 chars
for 32 bytes). This gives the user a human-verifiable out-of-band identity matching the server's
certificate fingerprint, mitigating the risk of pinning a MITM'd cert during enrollment.

## No Regressions

- `AllTrustedSocketFactory` is not resurrected — the `EnrollmentCaptureTrustManager` is scoped
  to the enrollment code path only.
- The IMAP data path (`ServiceBase.getBackupImapStore()`) is not modified by this story; it
  continues to read `PinnedCertStore` and select the appropriate factory per U-008/CNTR-001.
