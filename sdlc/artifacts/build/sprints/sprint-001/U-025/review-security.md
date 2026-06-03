---
artifact_type: review-security
story_id: "U-025"
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
issues_found: 0
issues_blocking: 0
---

# Security Review: U-025

## Summary

Security review of the MailTransport port and K9MailTransport adapter. No security issues
found. The adapter upholds all CNTR-MODERNIZATION-001 invariants.

## TLS Trust-Policy Review

**`K9MailTransport.buildSocketFactory()` (lines 107-120):**

- `SYSTEM_VALIDATED` → `new DefaultTrustedSocketFactory(context)` — correct; full CA chain validation
- `PINNED_CERTIFICATE` with non-null cert → `new PinnedCertificateSocketFactory(context, host, config.pinnedCert)` — correct; enforces SHA-256 + validity-window check
- `PINNED_CERTIFICATE` with null cert → `throw new MailException(...)` at construction time — CORRECT; fail-closed, no trust-all fallback
- `AllTrustedSocketFactory` is never instantiated — grep in `mail.transport` confirms zero matches — PASS

**No `AllTrustedSocketFactory` path exists.** The factory selection is deterministic and
fail-closed. CNTR-MODERNIZATION-001 Invariant #2 is upheld.

## Credential Masking

`K9MailTransport.getStoreUriForLogging()` delegates to `BackupImapStoreDelegate.getStoreUriForLogging()`
which is copied verbatim from `BackupImapStore`. The `parts[1].replaceAll(".", "X")` pattern
masks the password portion correctly. No credentials leak in logs.

## Exception Chain Preservation

`MailException(Throwable cause)` calls `super(cause)`, ensuring the original k-9 throwable
is preserved as `getCause()`. This is required for `State.getDetailedErrorMessage()` diagnostic
output and does NOT leak k-9 types to callers (the `Throwable` type itself is app-owned).

## ACL Boundary Integrity

The `MailTransport` interface contains no k-9 types. `K9MailTransport` is the sole producer-side
class. No mechanism exists for engine code (`service.*`) to bypass the translation layer.
`RequiresLoginException` is an existing app-owned exception type included in the throws clause
— not a new k-9 type leakage.
