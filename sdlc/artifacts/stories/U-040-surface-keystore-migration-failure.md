---
type: bug
status: ready
artifact_type: user-story
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-040
title: Surface Keystore failure during credential migration (fix BUG-008)
pipeline: ''
domain: modernization
requirement_source: bug:BUG-008
---

# U-040: Surface Keystore failure during credential migration

## Story
As a user, I want to be told (or have backups gated) if my credentials could not be encrypted due to a Keystore failure, so that plaintext credentials don't silently persist with no signal.

## Source
Fixes **BUG-008** (security, medium) — `sdlc/artifacts/stories/BUG-008-silent-plaintext-credentials-on-keystore-failure.md`.

## Acceptance Criteria
1. AC-1: When `getEncrypted()` throws during `migrateFromPlaintext()` (Keystore/EncryptedSharedPreferences failure), the failure is NOT silently swallowed: surface a user-visible degraded-security indication AND/OR gate backup operations until encryption succeeds (decide the exact mechanism during implementation; document it).
2. AC-2: Migration remains rollback-safe and exactly-once (U-012 invariants); a later launch retries and completes the migration once the Keystore recovers; OAuth2 users are not logged out.
3. AC-3: The off-main-thread migration (U-035) is preserved — surfacing the state must not reintroduce main-thread blocking.
4. AC-4: Build green (assembleDebug, testDebugUnitTest, jacoco ≥70%); a test covers the failure path (e.g., inject a throwing SecretStore/getEncrypted and assert the degraded-security signal / backup gate, and that a later success completes migration).

## Technical Notes
- File: `EncryptedPrefsSecretStore.java:229-235` (the WARN-log-and-skip path). Decide mechanism at implementation: a persisted "encryption-degraded" flag surfaced via notification/UI, and/or a guard that blocks backup enqueue until migration succeeds. Coordinate with U-035's `CredentialMigrationGate` (surface state off the main thread). Security medium.

## Estimation Guidance
Medium — failure-path handling + a user-visible signal/gate + a test; correctness of the retry/idempotency interplay is the main care.
