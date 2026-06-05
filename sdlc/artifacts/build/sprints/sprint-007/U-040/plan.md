---
artifact_type: plan
story_id: U-040
verdict: PASS
agent: Developer
timestamp: 2026-06-04
---

# Plan: U-040 — Surface Keystore failure during credential migration

## Problem
When `EncryptedPrefsSecretStore.getEncrypted()` throws during `migrateFromPlaintext()` (e.g.,
transient Android Keystore failure), the migration is silently skipped (WARN log only). Plaintext
credentials remain in `credentials.xml` indefinitely with no user-visible signal (BUG-008 /
M-002).

## Mechanism Chosen: Persisted Degraded-Security Flag
A boolean flag `ENCRYPTION_DEGRADED_KEY = "__secretstore_encryption_degraded__"` is written
to a separate plaintext `SharedPreferences` file (`credentials_meta`) when the encrypted store
cannot be opened. The flag:
1. Is set when `getEncrypted()` throws in `migrateFromPlaintext()`.
2. Is NOT set on a successful migration or on idempotency short-circuit.
3. Is cleared after a successful migration completes.
4. Is readable via `isEncryptionDegraded()` without opening the encrypted store.
5. Lives in a separate `credentials_meta` file to avoid polluting the main prefs namespace.

### Why a persisted flag (not backup gating for now)
- The story says "decide the exact mechanism during implementation; document it."
- A persisted flag is the simplest correct mechanism: it survives restarts, is readable
  without crypto, and provides a surface for future UI / backup gating without changing
  the backup engine in this story.
- A later story can read `isEncryptionDegraded()` from `EncryptedPrefsSecretStore` to gate
  backup; the hook is in place. This story does not gate backup to keep complexity minimal
  (the story explicitly allows "persisted degraded flag + clear log + gettable state (for
  a future UI) is acceptable").

## U-012 Invariants Preserved
- Rollback-safe ordering: unchanged (plaintext cleared only after encrypted commit).
- Exactly-once via `MIGRATION_COMPLETE_KEY`: unchanged — the flag is NOT written on failure.
- OAuth2-not-logged-out: unchanged — migration retried on next launch.

## U-035 Invariants Preserved
- Off-main-thread execution: `setEncryptionDegraded()` calls `commit()` on a plain
  SharedPreferences — this is synchronous I/O, but it occurs on the background migration
  thread dispatched by `App.onCreate()`. The main thread is NOT blocked.

## Files to Modify
- `EncryptedPrefsSecretStore.java`:
  - Add `ENCRYPTION_DEGRADED_KEY` constant.
  - Change `getEncrypted()` from `private` to package-private (test override seam).
  - Set degraded flag in catch block; clear on successful migration.
  - Add `isEncryptionDegraded()` public method.
  - Add private `setEncryptionDegraded(boolean)` helper.

## Tests
- New file `EncryptedPrefsSecretStoreDegradedTest.java` with 4 tests:
  - `u040_ac1_keystoreFailure_setsDegradedFlag`
  - `u040_ac2a_keystoreFailure_doesNotWriteMigrationCompleteKey`
  - `u040_ac2b_subsequentSuccess_completesMigrationAndClearsDegradedFlag`
  - `u040_noRegression_successOnFirstAttempt_notDegraded`
  Uses `AlwaysThrowingEncryptedPrefsSecretStore` and `SucceedingEncryptedPrefsSecretStore`
  inner test subclasses that override the package-private `getEncrypted()`.
