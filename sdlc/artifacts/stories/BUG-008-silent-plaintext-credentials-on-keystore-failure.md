---
type: bug
status: ready
artifact_type: bug
severity: medium
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-040
platforms: []
tags: []
id: BUG-008
title: Silent plaintext credential persistence when Keystore fails during migration
domain: build
origin: code-review
---

# BUG-008: Silent plaintext credential persistence on Keystore failure

## Description
During the plaintext→encrypted credential migration, if `getEncrypted()` throws (e.g., a transient Android Keystore failure), `migrateFromPlaintext()` silently skips the migration (WARN log only) and leaves plaintext credentials in `credentials.xml`. No user-visible degraded-security signal, so credentials can remain unencrypted indefinitely on an affected device.

## Steps to Reproduce
1. Induce a Keystore/`EncryptedSharedPreferences` failure at first-run migration time.
2. Observe: migration skipped (WARN logged), plaintext credentials remain, no user notification.

## Expected Behavior
A Keystore failure during migration is surfaced (user-visible degraded-security notification) and/or backup operations are blocked until encryption succeeds; the system does not silently retain plaintext credentials with no signal.

## Actual Behavior
`EncryptedPrefsSecretStore.java:229-235` — on `getEncrypted()` throw, migration is skipped with only a WARN log; plaintext persists.

## Evidence
- `EncryptedPrefsSecretStore.java:229-235`.
- Consolidated security review: `sdlc/artifacts/build/reviews/consolidated-2026-06/security-review.md` finding M-002.

## Acceptance Criteria
- [ ] AC-1: A Keystore failure during migration produces a user-visible degraded-security indication and/or gates backup until encryption succeeds (decide at planning); not silently swallowed.
- [ ] AC-2: Migration remains rollback-safe and exactly-once (U-012 invariants); a later-launch retry completes migration when the Keystore recovers.
- [ ] AC-3: Build green; tests cover the failure path.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `EncryptedPrefsSecretStore.java:229-235` | WARN-log and skip on getEncrypted failure | surface degraded-security state and/or gate backups; document retry |

## Existing Behavior to Preserve
- U-012/U-035 migration correctness: rollback-safe ordering, exactly-once, OAuth2-not-logged-out, off-main-thread (U-035).

## Verification Steps
1. Simulate getEncrypted failure; assert the user-visible signal / backup gate; assert later success completes migration.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- Security medium; interacts with U-035's off-thread migration (surface state without blocking the main thread). Backlog.

## Supporting Documentation
- `sdlc/artifacts/build/reviews/consolidated-2026-06/security-review.md` (M-002)

## Notes
Backlog (security medium) — not in the immediate B-1/B-2/B-3 fix batch.
