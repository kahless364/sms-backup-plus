---
artifact_type: security-review
story_id: "U-034"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-04"
blockers: 0
warnings: 0
---

# Security Review: U-034

## Summary

The fix addresses a UI crash and does not touch any security-sensitive logic paths. No changes
are made to the TLS handshake, the `EnrollmentCaptureTrustManager`, the `PinnedCertStore` write
path, the affirmative-consent requirement, or any cryptographic operations.

## Scope of Change

The change is structurally limited to:
1. How the enrollment dialog is constructed (which Context is passed to `AlertDialog.Builder`)
2. Where that construction occurs (moved from `PinCertificateEnrollmentFlow` to `AdvancedSettings`)

No security-critical code paths are modified.

## Security Properties Reviewed

### Affirmative Consent (CNTR-MODERNIZATION-002)

`PinnedCertStore.put()` is called exclusively from `storeCertOnConfirm()`, which is now invoked
from the `onTrust` click listener in `EnrollmentDialogData`. The click listener is created in
`showEnrollmentDialog()` and passed to `AdvancedSettings` only through the `EnrollmentDialogData`
parameter object — the store-write logic remains entirely inside `PinCertificateEnrollmentFlow`.
`AdvancedSettings` cannot and does not write to `PinnedCertStore` directly.

**Cancel path:** `onCancel` calls `listener.onCancelled()` only — no store write, no cert stored.
The cancel behavior is identical to before the fix.

### No New Trust Surface

The ephemeral `EnrollmentCaptureTrustManager` (trust-any for display-only cert capture) is
unchanged. It is still scoped exclusively to the enrollment fetch TLS socket and is not reused.

### No Context Retained Across Async Boundary

The only change to context handling is that `AlertDialog.Builder(context)` is no longer called
with application context in `PinCertificateEnrollmentFlow`. The Activity context (`getActivity()`)
is obtained on the main thread inside `onPostExecute` via the `DialogShower` callback — it is
not stored as a field and does not cross the async boundary.

### No Hardcoded Credentials or Secrets

No credentials, API keys, or secrets are introduced or modified.

### Input Validation Unchanged

`parseHost()` and `parsePort()` parsing logic is unchanged. No new user-supplied input paths.

## Verdict

PASS. The fix is a pure UI-construction refactor. No security properties are weakened. The
affirmative-consent (CNTR-MODERNIZATION-002) invariant is maintained. No new attack surface
is introduced.
