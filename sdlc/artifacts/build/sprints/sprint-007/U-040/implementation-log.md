---
artifact_type: implementation-log
story_id: U-040
verdict: PASS
agent: Developer
timestamp: 2026-06-04
files_changed: 1
files_created: 1
tests_added: 4
tests_passing: 523
---

# Implementation Log: U-040

## Summary
Fixed BUG-008: Keystore failure during plaintext→encrypted credential migration is now surfaced
via a persisted `ENCRYPTION_DEGRADED_KEY` boolean flag in a separate plaintext SharedPreferences
file (`credentials_meta`). `MIGRATION_COMPLETE_KEY` is NOT written on failure, so the next app
launch retries migration automatically once the Keystore recovers. A successful migration clears
the degraded flag.

## Degraded-Security Mechanism Chosen
**Persisted boolean flag in plain SharedPreferences.**

Rationale:
- The flag survives app restarts without requiring the encrypted store to be open.
- `isEncryptionDegraded()` is a gettable state that future backup gating or UI code can
  consume without changes to this file.
- The flag lives in `credentials_meta` (separate file) to avoid polluting the default
  SharedPreferences namespace.
- Backup gating was NOT added in this story — the story explicitly allows "persisted degraded
  flag + clear log + gettable state (for a future UI) is acceptable — document the boundary."
  The boundary: future backup gating reads `isEncryptionDegraded()` from the `EncryptedPrefsSecretStore`
  instance; no changes to the backup engine are needed here.
- MIGRATION_COMPLETE_KEY is not written on failure, guaranteeing the next launch retries.

## Files Modified

### `app/src/main/java/com/zegoggles/smssync/preferences/EncryptedPrefsSecretStore.java`
- Added `ENCRYPTION_DEGRADED_KEY = "__secretstore_encryption_degraded__"` constant with
  full Javadoc explaining the lifecycle (line ~49).
- Changed `getEncrypted()` visibility from `private` to package-private to enable test subclass
  override without reflection.
- Catch block in `migrateFromPlaintext()`: replaced WARN-only with `setEncryptionDegraded(true)`
  call + enhanced log message (line ~252-262).
- After step-4 success: added `setEncryptionDegraded(false)` to clear the flag on recovery
  (line ~287-289).
- Added public `isEncryptionDegraded()` method — reads from `credentials_meta` SharedPreferences
  (line ~315-319).
- Added private `setEncryptionDegraded(boolean)` helper — writes to `credentials_meta`
  SharedPreferences with `commit()` semantics (line ~330-336).

## Files Created

### `app/src/test/java/com/zegoggles/smssync/preferences/EncryptedPrefsSecretStoreDegradedTest.java`
4 tests using Robolectric with two inner test subclasses:
- `AlwaysThrowingEncryptedPrefsSecretStore`: overrides `getEncrypted()` to throw, simulating
  Keystore failure.
- `SucceedingEncryptedPrefsSecretStore`: overrides `getEncrypted()` to return a Robolectric-
  backed plain SharedPreferences, simulating Keystore recovery.

Tests:
1. `u040_ac1_keystoreFailure_setsDegradedFlag` — verifies flag is set on failure.
2. `u040_ac2a_keystoreFailure_doesNotWriteMigrationCompleteKey` — verifies marker NOT written
   on failure, so retry runs on next launch.
3. `u040_ac2b_subsequentSuccess_completesMigrationAndClearsDegradedFlag` — simulates 2-launch
   scenario: fail → succeed; verifies migration completes and flag is cleared.
4. `u040_noRegression_successOnFirstAttempt_notDegraded` — verifies no false-positive flag
   when migration succeeds on first attempt.

## Test Results
- 523 total tests; 1 @Ignored (pre-existing).
- 4 new tests added; all pass.
- Build: assembleDebug PASS, testDebugUnitTest PASS, jacocoTestCoverageVerification PASS.

## Regression Results — U-012 Invariants
- RETAINED: Rollback-safe ordering — plaintext cleared only after encrypted commit. Unchanged.
- RETAINED: Exactly-once via MIGRATION_COMPLETE_KEY — NOT written on failure. Unchanged.
- RETAINED: OAuth2-not-logged-out — no token clearing on failure path. Unchanged.

## Regression Results — U-035 Invariants
- RETAINED: Off-main-thread execution — `setEncryptionDegraded()` called on background migration
  thread (dispatched from `App.onCreate()` via executor). Main thread is NOT blocked.
  The write is a plain SharedPreferences `commit()` (< 1ms expected; not a Keystore operation).

## Integration Path
`App.onCreate()` → executor → `secretStore.migrateFromPlaintext()` → on Keystore failure:
`EncryptedPrefsSecretStore.setEncryptionDegraded(true)` → written to `credentials_meta` prefs.
Future consumer: `EncryptedPrefsSecretStore.isEncryptionDegraded()` readable from any thread
without opening the encrypted store.

## Boundary Documentation
This story implements the minimum: flag + log + gettable state. What is explicitly OUT OF SCOPE:
- User-visible notification (future story should read `isEncryptionDegraded()` and post a notification).
- Backup gating (future story: `SmsBackupService` reads `isEncryptionDegraded()` and returns early).
These are not blocked by this fix; the hook (`isEncryptionDegraded()`) is ready.
