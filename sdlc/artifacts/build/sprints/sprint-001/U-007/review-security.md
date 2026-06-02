---
artifact_type: security-review
story_id: "U-007"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# Security Review: U-007

## Scope

This review covers the rewritten `AuthPreferences.migrate()` method and the
`TRANSPORT_SECURITY_NOTICE_PENDING` constant. It does not cover BackupImapStore,
AllTrustedSocketFactory, or ServiceBase (U-008/U-010 scope).

## SEC-001 / ARCH-008 — Silent Security-Downgrade Migration

**Finding: CLOSED by this story.**

The rewritten `migrate()`:
- Has NO branch that writes `SERVER_TRUST_ALL_CERTIFICATES = true` (verified by code read + grep)
- Actively clears any stale `true` value to `false`, closing the exposure on first launch of the fixed build
- Does NOT touch `BackupImapStore` factory selection — that remains for U-008/U-010, but the preference gate that enabled the trust-all factory is now cleaned up

The attack path described in REQ-MODERNIZATION-002 §Exploit Path is broken at the
preference-write step: `migrate()` can no longer write the flag that causes
`isTrustAllCertificates()` to return `true` for the silent-downgrade cohort.

Note: the `AllTrustedSocketFactory.INSTANCE` reference in `BackupImapStore.java` still
exists (it is deleted in U-010). However, the prerequisite condition for its selection —
`isTrustAllCertificates()` returning `true` due to `migrate()` — is eliminated by this
story for newly-upgraded devices. Stale `true` values from OLD migrate() runs are also
cleared on the next launch after this fix is deployed.

## Transport Security Notice

The `transport_security_notice_pending` flag is a boolean stored in default
`SharedPreferences` (app-private). It contains no sensitive data. It is correctly
written only by `migrate()` in the scenarios where the user needs to be informed
(legacy +ssl/+tls protocol or stale trust_all=true). The consumption layer (U-009)
will read it, show the notice once, and mark it shown — standard one-time-notice pattern.

## apply() vs commit()

`edit.apply()` is used throughout the rewritten method. There are no synchronous
fsync operations on the main thread in this code path (ARCH-017). The write is
idempotent, so the weaker durability guarantee is safe.

## Sensitive Data Handling

`migrate()` does not read or write any credential, token, or personally-identifiable
data. It operates only on:
- `server_protocol` (protocol string)
- `server_trust_all_certificates` (boolean)
- `transport_security_notice_pending` (boolean)

No credentials are exposed by this change.

## Hardcoded Values

No hardcoded secrets, credentials, or security-sensitive constants were introduced.
The key string `"transport_security_notice_pending"` is a preference key, not a secret.

## Verdict

PASS. The rewrite eliminates the ARCH-008 silent-downgrade vector within migrate()'s
scope. The broader CWE-295 closure (AllTrustedSocketFactory deletion) is deferred to
U-010 as specified in the story scope.
