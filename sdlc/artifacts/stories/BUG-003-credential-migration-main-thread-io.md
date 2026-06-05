---
type: bug
status: ready
artifact_type: bug
severity: low
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-035
platforms: []
tags: []
id: BUG-003
title: Credential migration performs disk/crypto I/O on the main thread in App.onCreate (StrictMode violation)
domain: build
origin: qa
---

# BUG-003: Credential migration performs disk/crypto I/O on the main thread in App.onCreate (StrictMode violation)

## Description

The one-time plaintext-to-encrypted credential migration introduced by U-012 runs
synchronously on the main thread inside `App.onCreate()`. The call chain is:

```
App.onCreate()  [line 163]
  -> Preferences.migrate()  [Preferences.java:295]
    -> new AuthPreferences(context).migrate()  [AuthPreferences.java:344]
      -> secretStore.migrateFromPlaintext()  [EncryptedPrefsSecretStore.java:230]
        -> getEncrypted()  [EncryptedPrefsSecretStore.java:93]
```

`EncryptedPrefsSecretStore.getEncrypted()` (line 83-108) constructs a `MasterKey` via
`MasterKey.Builder` and opens `EncryptedSharedPreferences` via
`EncryptedSharedPreferences.create(...)`. This operation performs:

- Android Keystore master key generation or retrieval (crypto / hardware-backed I/O)
- Tink keyset load and AES-256-GCM key derivation
- Disk read and write of the `credentials.xml` backing file

Additionally, `migrateFromPlaintext()` calls `editor.commit()` twice (line 251: encrypt
write + marker, line 254-258: plaintext clear) — `commit()` is synchronous and blocks the
caller until the disk write completes. Both `commit()` calls occur on the main thread.

`App.setupStrictMode()` (called at line 152, before `preferences.migrate()` at line 163)
configures a `StrictMode.ThreadPolicy` with `detectDiskWrites()` and `detectNetwork()`.
`detectDiskReads()` is commented out. The `penaltyFlashScreen()` penalty is set without
`penaltyDeath()`, so violations are logged and cause a screen flash but do NOT crash the
process. This is confirmed by the `D/StrictMode` (debug-level) log entries, not a fatal
exception.

This is currently a low-severity logged warning. However, on cold-start or slow-storage
devices the synchronous `MasterKey` construction and dual `commit()` calls represent a
genuine ANR risk: if the main thread is blocked for more than 5 seconds during startup,
Android will deliver an ANR. The risk is higher on first-run-after-upgrade (migration not
yet complete, `encrypted` field null, full keyset creation required) than on subsequent
launches (migration complete, idempotency short-circuit at
`EncryptedPrefsSecretStore.java:207` returns immediately).

## Steps to Reproduce

1. Install a build that includes U-012 (credential migration) on a device or emulator
   (tested on emulator-5554, API 37).
2. Launch the app for the first time after upgrade (migration not yet complete: the
   `__secretstore_migration_complete__` key is absent from the encrypted store, and the
   `encrypted` field in `EncryptedPrefsSecretStore` is null).
3. Observe logcat for `D/StrictMode` tag output.
4. Observe: repeated `StrictMode` disk-write violations with the following stack are logged
   at startup before any user interaction.

## Expected Behavior

The plaintext-to-encrypted credential migration does not perform blocking disk or crypto I/O
on the main thread. No `StrictMode` thread-policy violation is attributable to the migration
in `App.onCreate()`. There is no ANR risk introduced at application startup by the migration
path.

## Actual Behavior

`StrictMode` logs main-thread disk-write violations during `App.onCreate()` on first run
after upgrade. The repeated violation stack (from logcat, emulator-5554, API 37):

```
D/StrictMode: at com.zegoggles.smssync.preferences.EncryptedPrefsSecretStore.getEncrypted(EncryptedPrefsSecretStore.java:93)
D/StrictMode: at com.zegoggles.smssync.preferences.EncryptedPrefsSecretStore.migrateFromPlaintext(EncryptedPrefsSecretStore.java:230)
D/StrictMode: at com.zegoggles.smssync.preferences.AuthPreferences.migrate(AuthPreferences.java:344)
D/StrictMode: at com.zegoggles.smssync.preferences.Preferences.migrate(Preferences.java:295)
D/StrictMode: at com.zegoggles.smssync.App.onCreate(App.java:163)
```

`penaltyFlashScreen()` causes a visual screen flash; the process is not killed
(`penaltyDeath()` is not set). The violation is silent to the user but represents latent ANR
risk on slow devices.

## Evidence

- `App.java:163` — `preferences.migrate()` called synchronously in `onCreate()` after
  `setupStrictMode()` (line 152) has already armed the disk-write detection policy.
- `App.java:328-335` — `setupStrictMode()`: `detectDiskWrites()` + `detectNetwork()` +
  `penaltyFlashScreen()` active; `detectDiskReads()` commented out.
- `Preferences.java:295` — `migrate()` delegates to `new AuthPreferences(context).migrate()`.
- `AuthPreferences.java:344` — `secretStore.migrateFromPlaintext()` is the entry point into
  blocking disk/crypto work.
- `EncryptedPrefsSecretStore.java:83-108` (`getEncrypted()`) — `MasterKey.Builder` +
  `EncryptedSharedPreferences.create(...)`: Keystore access, Tink keyset load, backing-file
  disk I/O; all triggered on first call when `encrypted == null`.
- `EncryptedPrefsSecretStore.java:196-261` (`migrateFromPlaintext()`) — lines 251 and
  254-258 both call `commit()` (synchronous disk write), blocking the calling thread until
  each write completes; both calls reachable on the main thread.
- StrictMode logcat stack (reproduction details above): confirms the full main-thread call
  chain from `App.onCreate` to `getEncrypted`.

## Acceptance Criteria

- [ ] AC-1: The plaintext-to-encrypted credential migration no longer performs blocking
  disk or crypto I/O on the main thread. The migration must be moved off the main thread
  (e.g., a background coroutine dispatched to `Dispatchers.IO`, a `WorkManager` one-shot
  worker, or deferred to first credential access on a non-main thread). The chosen approach
  must ensure credentials are available before the first use point that requires them.

- [ ] AC-2: No `StrictMode` disk-write violation attributable to
  `EncryptedPrefsSecretStore.migrateFromPlaintext()` or `getEncrypted()` appears in logcat
  during `App.onCreate()` after the fix, on a device/emulator where the migration has not
  yet run (fresh first-run-after-upgrade path).

- [ ] AC-3: Idempotency and rollback safety from U-012 are fully preserved. The migration
  still completes exactly once: the `__secretstore_migration_complete__` marker is written
  atomically with the encrypted values; the plaintext clear step (step 4 in
  `migrateFromPlaintext()`) occurs only after the encrypted commit succeeds; the migration
  re-runs on the next launch if interrupted before the marker is written.

- [ ] AC-4: OAuth2 users are not logged out. The migration of `oauth2_token` and
  `oauth2_refresh_token` (as well as `login_password`) is completed before the first
  credential read that requires them; the OAuth2 early-return in `AuthPreferences.migrate()`
  (line 346) continues to execute after the `secretStore.migrateFromPlaintext()` call.

- [ ] AC-5: The U-012 Option-A ordering invariant is preserved: the raw
  `SharedPreferences("credentials", MODE_PRIVATE)` plaintext values are read into memory
  before `EncryptedSharedPreferences` is constructed over the same backing file, regardless
  of where the migration is dispatched to.

- [ ] AC-6: The existing unit suite (all tests covering `EncryptedPrefsSecretStore`,
  `AuthPreferences`, and `Preferences`) passes green. The coverage gate holds at its
  current threshold.

### Integration Criteria

- [ ] IC-1: If `Preferences.migrate()` call site in `App.onCreate` (line 163) is changed
  or removed, all callers of `preferences.migrate()` in the codebase are updated
  consistently and the migration still fires on every app launch until complete.
- [ ] IC-2: If the migration is deferred to a background worker, the worker is configured
  as a one-shot `WorkManager` job; the worker's task ID does not conflict with existing
  `WorkManager` task IDs defined in `WorkManagerScheduler`.

## Impact Analysis

Manual impact analysis (change_records.layers not configured).

**Affected startup path:** `App.onCreate()` -> `Preferences.migrate()` ->
`AuthPreferences.migrate()` -> `EncryptedPrefsSecretStore.migrateFromPlaintext()` /
`getEncrypted()`. The fix modifies how and when this chain is invoked (on-thread vs. off-thread
or deferred), not its internal logic.

**Current impact (pre-fix):** Logged StrictMode disk-write violation with
`penaltyFlashScreen()` on first run after upgrade. No crash, no data loss. Latent ANR risk
on cold-start / slow-storage devices when Keystore access stalls or `commit()` is slow.
Subsequent launches hit the idempotency short-circuit at `EncryptedPrefsSecretStore.java:207`
(`encrypted != null && encrypted.contains(MIGRATION_COMPLETE_KEY)` check) and do not
re-enter the migration body.

**Risk of fix:** Low. The migration logic itself is unchanged; only the dispatch thread or
timing changes. Primary risk is a TOCTOU window if credentials are accessed before the
background migration completes — this must be addressed in the fix design (e.g., coroutine
await, or make credential access block on migration completion via a `CompletableDeferred`
or `Mutex`).

**Priority:** Low. The violation is logged-only today (no penalty crash). Not blocking any
current sprint work.

## Suggested Approach

Three viable approaches in increasing implementation cost:

1. **Coroutine dispatch (simplest):** In `App.onCreate()`, replace the synchronous
   `preferences.migrate()` call with a coroutine launched on `Dispatchers.IO`. Gate
   credential first-access (e.g., `AuthPreferences.getPassword()` / `getOAuth2Token()`) on
   migration completion using a `CompletableDeferred<Unit>` or a `Mutex`. This is consistent
   with U-015's lesson that background work belongs in workers/coroutines.

2. **Lazy first-access deferral:** Remove the eager migration call from `App.onCreate()`
   entirely. Invoke `secretStore.migrateFromPlaintext()` on first credential read in
   `AuthPreferences.get*()`/`SecretStore.get()` dispatched to a background thread. The
   idempotency guard prevents double execution. Simpler to thread-safety-reason about but
   requires the migration to be reentrant if two credential reads race at startup.

3. **WorkManager one-shot worker:** Schedule a `CoroutineWorker` (consistent with U-015
   `BackupWorker` pattern) as a one-shot `WorkManager` task that runs the migration.
   `enqueueUniqueWork` with `KEEP` policy handles idempotency at the WorkManager level in
   addition to the existing marker. Higher infrastructure cost but aligns with the project's
   WorkManager-first background work policy.

**Note:** `detectDiskReads()` is currently commented out in `setupStrictMode()`. If it is
re-enabled in the future, additional I/O violations may surface from the idempotency check
path (read of `encrypted.contains(MIGRATION_COMPLETE_KEY)`). The fix should be robust to
that scenario.

## Suggested Approach (Solution Architect — advisory)

Advisory only — refined during story planning.

**Priority framing.** This is LOW priority and belongs in the backlog. The StrictMode
penalty in `App.setupStrictMode()` is `penaltyFlashScreen()` only — there is no
`penaltyDeath()`, so the violation is logged-and-flashed, not fatal (confirmed by reading
`App.java:328-335`). This fix must NOT block the BUG-002 fix or live testing. Treat it as a
single, self-contained low-priority story.

**Non-negotiable U-012 invariants the fix must preserve.** Whatever dispatch strategy is
chosen, the internal logic of `migrateFromPlaintext()` (`EncryptedPrefsSecretStore.java:196-261`)
should stay intact. Specifically:
- **Idempotency** via the `MIGRATION_COMPLETE_KEY` marker (short-circuit at line 207 and the
  post-open check at line 239) — exactly-once execution.
- **Rollback safety** via the buffer→encrypt→verify→clear ordering (Step 2 reads plaintext
  into `pw`/`at`/`rt`; Step 3 writes + commits; Step 4 clears plaintext only after Step 3's
  commit returns). Do not reorder these steps.
- **OAuth2 not-logged-out**: the call sits before the `useXOAuth()` early-return in
  `AuthPreferences.migrate()` (line 344, return at 346). Any deferral must still complete the
  `oauth2_token` / `oauth2_refresh_token` migration before the first OAuth2 credential read.
- **Option-A ordering**: raw `SharedPreferences("credentials", MODE_PRIVATE)` is read before
  `EncryptedSharedPreferences` is constructed over the same backing file (Step 2 precedes the
  `getEncrypted()` call at line 230).

**Options (advisory; recommend A).**

- **(A) — RECOMMENDED. Off-main-thread migration WITH a completion gate.** Dispatch
  `preferences.migrate()` off the main thread (e.g. `Dispatchers.IO` or a background executor,
  consistent with the background patterns U-015 established), and gate the *first* credential
  read so it blocks on migration completion (e.g. a `CompletableDeferred<Unit>` / latch / mutex
  that the first `getEncrypted()`-backed read awaits). This removes the main-thread I/O while
  guaranteeing credentials are correct and available the first time they are read. The exact
  race-resolution mechanism should be settled at planning. This is the recommended option
  because a scheduled `BackupWorker` (U-015) may need credentials early, and a gate guarantees
  correctness without re-introducing a blocking main-thread path.

- **(B) — Lazy/deferred migration on first credential access, off-thread.** Remove the eager
  `App.onCreate()` call and run the migration on the first credential read, dispatched off the
  main thread. The existing marker guard makes it reentrant-safe, but two startup credential
  reads could race — that race must be reasoned about and closed at planning.

- **(C) — Move the migrate() call out of `App.onCreate()` to the first background credential
  touch.** A narrower variant of (B): keep migration eager-ish but relocate its trigger to the
  first background credential interaction rather than `onCreate()`.

**Trade-off to settle at planning.** A scheduled `BackupWorker` may require credentials early
in its run. The fix must guarantee credentials are correct and available when first read,
*without* a blocking main-thread path. Option (A)'s completion gate is the cleanest way to
satisfy both constraints, which is why it is recommended.

**Both `commit()` calls are main-thread disk writes — moving only `getEncrypted()` off-thread
is incomplete.** Verified in source: `migrateFromPlaintext()` calls `commit()` at line 251
(encrypted write + marker) and again at lines 254-258 (plaintext clear). Both are synchronous
disk writes on the calling thread. A fix that relocates only the `EncryptedSharedPreferences`
construction but leaves either `commit()` on the main thread does not close the violation.
**Do NOT convert `commit()` to `apply()`** — the rollback-safe durability barrier depends on
synchronous commit semantics (per CNTR-MODERNIZATION-003); the correct remedy is to run the
whole sequence on a thread where blocking I/O is acceptable. Note also that
`EncryptedSharedPreferences` keyset load (`getEncrypted()`, lines 88-99) is Keystore-IPC heavy
and is itself a primary source of unbounded main-thread latency.

**Reuse opportunity.** U-015 established the project's background-worker patterns
(`CoroutineWorker` / `WorkManager`); those patterns are a natural fit and should be considered
for whichever dispatch mechanism planning selects. IC-2 in this bug already anticipates a
one-shot WorkManager job and the task-ID-collision concern if that route is taken.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/App.java` | Calls `preferences.migrate()` synchronously on the main thread at line 163 in `onCreate()` | Move migration off the main thread (dispatch to background, or remove call and defer to first credential access) |
| `app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java` | `migrate()` at line 294-295 delegates synchronously to `new AuthPreferences(context).migrate()` | May require threading/dispatch coordination if the call site moves |
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | `migrate()` at line 333 calls `secretStore.migrateFromPlaintext()` synchronously | No change to migration logic; change is in dispatch context |
| `app/src/main/java/com/zegoggles/smssync/preferences/EncryptedPrefsSecretStore.java` | `migrateFromPlaintext()` (line 196) and `getEncrypted()` (line 83) perform blocking Keystore + disk I/O; `commit()` called twice | No change to internal logic; must be called from a background thread after fix |

Introduced by: U-011 (`EncryptedPrefsSecretStore` + `SecretStore` interface) and U-012
(migration wiring in `AuthPreferences.migrate()` and `App.onCreate()` call site).

## Existing Behavior to Preserve

- **U-012 idempotency:** The migration runs exactly once. The `__secretstore_migration_complete__`
  marker in the encrypted store gates re-entry. This invariant must be preserved regardless
  of how the migration is dispatched.
- **U-012 rollback safety:** The four-step sequence — (1) buffer plaintext, (2) open
  encrypted store, (3) write values + marker + commit, (4) clear plaintext — must not be
  reordered. Step 4 must not execute if step 3's `commit()` fails.
- **U-012 Option-A ordering:** The raw `SharedPreferences("credentials")` handle must be
  read before `EncryptedSharedPreferences` is constructed over the same file.
- **U-012 OAuth2 not-logged-out:** `oauth2_token` and `oauth2_refresh_token` are migrated
  before the first OAuth2 credential read; users are not forced to re-authenticate after upgrade.
- **U-011 / U-007 security posture:** Credentials remain encrypted at rest in
  `EncryptedSharedPreferences` with AES-256-GCM; the fix must not introduce any path that
  reads or writes plaintext credentials outside the migration sequence.
- **U-012 backup exclusion invariant:** The `credentials.xml` backing file name remains
  `"credentials"` so the existing `backup_descriptor.xml` exclusion rule continues to match.

## Verification Steps

1. Install a build with the fix on a fresh emulator or device (API 24+) that has not
   previously run the app after upgrade (migration not yet complete).
2. Launch the app. Filter logcat for `StrictMode`. Confirm: no `StrictMode` disk-write
   violation with a stack trace rooted in `EncryptedPrefsSecretStore.migrateFromPlaintext`
   or `App.onCreate` appears. (AC-2)
3. Wait for the migration to complete (verify via logcat:
   `"EncryptedPrefsSecretStore: plaintext-to-encrypted credential migration complete."`).
   Confirm: the `__secretstore_migration_complete__` key is present in the encrypted store
   (readable via `EncryptedPrefsSecretStore.contains(MIGRATION_COMPLETE_KEY)`). (AC-3)
4. Confirm no plaintext credential values (`login_password`, `oauth2_token`,
   `oauth2_refresh_token`) remain in the raw `SharedPreferences("credentials")` store after
   migration completes. (AC-3, AC-4)
5. Kill the app mid-migration (before the marker is written) and relaunch. Confirm: the
   migration re-runs and completes correctly on the second launch; no credential loss.
   (AC-3 rollback safety)
6. With an existing account (OAuth2 or IMAP), upgrade and confirm the user is not logged
   out and the backup flow proceeds normally after migration completes. (AC-4)
7. Run the full unit suite: `./gradlew testDebugUnitTest`. Confirm: all tests pass, coverage
   gate holds. (AC-6)
8. Relaunch the app a second time (migration already complete). Confirm: no `StrictMode`
   violation and the idempotency short-circuit fires (logcat: no migration-complete log
   line on second launch). (AC-3)

## Root Cause Analysis

<!-- Filled during planning phase -->

## Technical Context

- `EncryptedSharedPreferences` (androidx.security-crypto 1.1.0-alpha06, per
  DES-MODERNIZATION-004) is inherently disk- and crypto-heavy: `MasterKey.Builder.build()`
  generates or retrieves an AES-256-GCM key from the Android Keystore (hardware-backed when
  StrongBox or TEE is available), and `EncryptedSharedPreferences.create()` loads a Tink
  keyset and opens the backing file. These operations have unbounded latency on the main
  thread — Keystore access on some devices involves IPC to the `keystore` daemon.
- The two `commit()` calls in `migrateFromPlaintext()` (lines 251, 254-258) are
  synchronous-write by design per CNTR-MODERNIZATION-003 (durability barrier). They must
  remain `commit()` (not `apply()`); the fix must ensure they run on a thread where
  blocking I/O is acceptable.
- The violation is low priority today: `penaltyFlashScreen()` is the only active penalty
  (no `penaltyDeath()`, no `penaltyDropBox()`), so the effect is a screen flash and a
  logcat warning. The ANR risk is real but unobserved in practice on the test device.
- U-015 established the precedent that background work in this project belongs in
  `CoroutineWorker` / `WorkManager` workers. A coroutine-on-`Dispatchers.IO` dispatch from
  `App.onCreate()` is the lightest-weight option consistent with that precedent.
- If `detectDiskReads()` (currently commented out in `setupStrictMode()`) is re-enabled in
  future, additional violations will surface from the `encrypted.contains(MIGRATION_COMPLETE_KEY)`
  check on the main thread (the fast-path idempotency probe at line 207). The fix design
  should anticipate this.
- `Preferences.migrate()` is also called (or was previously called) from other entry points;
  confirm no other call sites exist that could re-introduce a main-thread migration path
  after the fix.

## Supporting Documentation

- U-011: `EncryptedPrefsSecretStore` and `SecretStore` interface (introduced the affected class)
- U-012: One-time plaintext-to-encrypted credential migration (introduced the `App.onCreate`
  call site and `migrateFromPlaintext()` logic being fixed)
- CNTR-MODERNIZATION-003: SecretStore contract — migration semantics, durability barrier,
  rollback-safety invariant
- DES-MODERNIZATION-004: EncryptedPrefsSecretStore design — Option-A ordering rationale,
  androidx.security-crypto version pin

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes
