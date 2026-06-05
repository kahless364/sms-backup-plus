---
artifact_type: plan
story_id: U-035
verdict: PASS
agent: Developer
timestamp: "2026-06-04"
---

# Plan: U-035 — Move credential migration off the main thread

## Problem

`App.onCreate()` called `preferences.migrate()` synchronously on the main thread, which
chains into `EncryptedPrefsSecretStore.migrateFromPlaintext()`. That method performs:

- `MasterKey.Builder.build()` — Keystore IPC (hardware-backed AES-256-GCM key retrieval)
- `EncryptedSharedPreferences.create()` — Tink keyset load + disk I/O
- Two `commit()` calls — synchronous disk writes (durability barriers)

`setupStrictMode()` at line 152 arms `detectDiskWrites()` + `penaltyFlashScreen()` before
the migration at line 163, producing StrictMode violations on first-run-after-upgrade.

## Chosen Approach: Option A (off-thread dispatch + completion gate)

### Gate mechanism: `CountDownLatch(1)` in `CredentialMigrationGate`

A `CountDownLatch(1)` is used as the one-shot gate because:
- It is the lowest-complexity correct concurrent primitive for a "wait for one event" pattern.
- It has strong JMM guarantees: all writes before `countDown()` are visible to threads
  that return from `await()` (JMM §17.4.5, happens-before via latch release).
- No risk of deadlock: if migration fails, `signalComplete()` is called in `finally` so
  consumers are never permanently blocked.
- Idiomatic Java with no Kotlin/coroutine dependency in the production Java classes.

### Dispatch: `Executors.newSingleThreadExecutor()`

A single-thread executor is used in `App.onCreate()` to dispatch `preferences.migrate()`.
This is consistent with the project's pattern of using dedicated executor/background-thread
dispatch for I/O (U-015 used `CoroutineWorker`; the gate here is lower-weight than WM).
The executor is shut down after the single task completes to free resources.

### Gate wiring

- `CredentialMigrationGate.prepare()` — called in `App.onCreate()` BEFORE dispatch.
  Initializes the `CountDownLatch`. Idempotent (synchronized, null-check).

- `CredentialMigrationGate.signalComplete()` — called in the background task's `finally`
  block. Sets `migrationComplete = true` (volatile write) then calls `latch.countDown()`.
  The volatile write enables the fast-path check in `awaitIfNeeded()` to skip latch overhead
  on subsequent credential reads after migration is done.

- `CredentialMigrationGate.awaitIfNeeded()` — called at the top of three credential getters
  in `AuthPreferences`: `getOauth2Token()`, `getOauth2RefreshToken()`, `getImapPassword()`.
  Returns immediately if `migrationComplete == true` (volatile read, no latch contention).
  Blocks on the latch otherwise. Main-thread guard prevents ANR (logs warning, skips await).
  10-second timeout prevents permanent blocking on Keystore stall.

## Race-safety argument

1. `prepare()` and `executor.submit()` both execute on the main thread in `App.onCreate()`
   in that order. The main thread establishes happens-before between prepare and submit.

2. The submitted task runs on a background thread. Its `migrateFromPlaintext()` call and
   subsequent `signalComplete()` run on that thread.

3. Any consumer calling `awaitIfNeeded()` either:
   (a) Sees `migrationComplete == true` (volatile read) — migration already done, returns
       immediately. All migration writes visible (volatile write of `migrationComplete` in
       `signalComplete()` happens-before this volatile read, which happens-before the
       `secretStore.get()` call; JMM §17.4.5 synchronization order).
   (b) Does not see `migrationComplete == true` yet — calls `latch.await()`. The latch
       release (via `countDown()` called after `migrationComplete = true`) establishes
       happens-before for the await return. All writes before `countDown()` are visible
       after `await()` returns (JMM latch guarantee).

4. The gate is initialized before the executor submits, so no consumer can call
   `awaitIfNeeded()` and see a null latch that gets replaced after. (Synchronized prepare
   + volatile latch field guarantees safe publication.)

## Files to modify

| File | Change |
|------|--------|
| `App.java` | Replace sync `preferences.migrate()` with off-thread dispatch + gate |
| `AuthPreferences.java` | Add `CredentialMigrationGate.awaitIfNeeded()` to 3 credential getters |

## Files to create

| File | Purpose |
|------|---------|
| `CredentialMigrationGate.java` | Completion gate (CountDownLatch + volatile flag) |
| `CredentialMigrationGateTest.java` | Gate unit tests |
| U-035 test additions to `AuthPreferencesTest.java` | Off-thread + gate integration tests |

## Invariants preserved

- Both `commit()` calls in `migrateFromPlaintext()` run on the background executor thread (AC-3)
- `commit()` semantics unchanged — NOT converted to `apply()` (CNTR-MODERNIZATION-003)
- Rollback-safe four-step ordering in `EncryptedPrefsSecretStore` unchanged (U-012)
- Idempotency (`MIGRATION_COMPLETE_KEY` fast-path) unchanged (U-012)
- OAuth2 users not logged out — `secretStore.migrateFromPlaintext()` before `useXOAuth()`
  return in `AuthPreferences.migrate()` (line 344 before 346) unchanged (AC-6)
- `credentials.xml` backing file name unchanged (backup_descriptor.xml rule still matches)
