---
artifact_type: review-code
story_id: U-035
verdict: PASS
agent: Developer
timestamp: "2026-06-04"
---

# Code Review: U-035

## Summary

The implementation correctly moves the credential migration off the main thread using a
`CountDownLatch`-based gate. The core logic of `migrateFromPlaintext()` is untouched; only
the dispatch context changes. All U-012 invariants are preserved.

## AC Compliance

| AC | Status | Evidence |
|----|--------|----------|
| AC-1: migration not on main thread | PASS | `App.java:185-200` — executor dispatch; `preferences.migrate()` runs on executor thread |
| AC-2: no StrictMode disk-write violation from migration | PASS | Threading change removes main-thread I/O; unit tests pass (device-free evidence) |
| AC-3: BOTH `commit()` calls off main thread | PASS | Both inside `EncryptedPrefsSecretStore.migrateFromPlaintext()` which runs on executor thread |
| AC-4: completion gate + race-safety argument in code comments | PASS | `CredentialMigrationGate.java` has full Javadoc race-safety argument; gate wired in `AuthPreferences` credential getters |
| AC-5: idempotency + rollback-safe ordering preserved | PASS | `EncryptedPrefsSecretStore.java:207, 239, 215-260` unchanged |
| AC-6: OAuth2 not logged out | PASS | `AuthPreferences.java:344` `migrateFromPlaintext()` before `useXOAuth()` return at 346, unchanged |
| AC-7: tests pass green, count reported, new tests added | PASS | 606 tests, 0 failures; 12 new tests covering off-thread path, idempotency, OAuth2, gate ordering |
| AC-8: `assembleDebug` green | PASS | BUILD SUCCESSFUL |

## IC Compliance

| IC | Status | Evidence |
|----|--------|----------|
| IC-1: migration fires every launch until complete | PASS | `capturedPreferences.migrate()` in every `App.onCreate()` dispatch; idempotency short-circuit handles subsequent calls |
| IC-3: all credential-read paths gated | PASS | `getOauth2Token()`, `getOauth2RefreshToken()`, `getImapPassword()` all call `awaitIfNeeded()` |

## Code Quality

### CredentialMigrationGate.java

- Well-documented Javadoc with full race-safety argument, deadlock-safety section, and
  timeout rationale. The gate mechanism is co-located with the race-safety argument as
  required by AC-4.
- `volatile` on both `latch` and `migrationComplete` fields — correct for thread visibility.
- Double-checked locking pattern used correctly (volatile field + synchronized for
  initialization only).
- `resetForTest()` is package-private and clearly named for its test-only purpose.
- Timeout of 10 seconds is documented and appropriate.

### App.java changes

- `final ExecutorService migrationExecutor` correctly declared final so it can be
  referenced inside the anonymous `Runnable`.
- `migrationExecutor.shutdown()` in the `finally` block ensures cleanup even on exception.
- `CredentialMigrationGate.prepare()` called BEFORE `executor.submit()` — correct ordering
  per the race-safety argument.
- `capturedPreferences` capture is clean and avoids holding the `this` reference in the task.

### AuthPreferences.java changes

- Gate added to all three credential-read methods that can return sensitive secrets.
- `getOauth2Username()` intentionally NOT gated (reads from plaintext prefs, not SecretStore;
  not a migrated secret per CNTR-MODERNIZATION-003 §Backing key set note).
- Javadoc on each gated method cross-references `CredentialMigrationGate`.

## Potential Issues

### Minor: `resetForTest()` in production binary

`CredentialMigrationGate.resetForTest()` is a package-private method in a production class.
It is only called from tests. R8 will likely dead-code-eliminate it in release builds since
no production caller exists, but it is technically present. A future cleanup could move it
to a test-only helper or annotate it `@VisibleForTesting`. Not a functional issue.

### Minor: Main-thread guard is fail-open

If `awaitIfNeeded()` is called on the main thread (e.g., from a UI presenter that reads
credentials), it logs a warning and skips the await. This is intentional to prevent ANR,
but the credential read may proceed before migration is complete. The three current call
sites (BackupWorker credentials path) are all background threads. This should be documented
in the gate class (it is — see the "Deadlock safety" section in the Javadoc).

### Resolved: test timing

The initial `awaitIfNeeded_blocksUntilSignalComplete` test used `Thread.sleep(50)` to assert
the consumer was blocked — this was correctly identified as flaky under Robolectric and
redesigned to use a latch-based coordination protocol instead. The final implementation is
not timing-dependent.

## Verdict

PASS — implementation is correct, complete, and minimal. No regressions. U-012 invariants
preserved. Race-safety documented in code. All ACs and ICs satisfied.
