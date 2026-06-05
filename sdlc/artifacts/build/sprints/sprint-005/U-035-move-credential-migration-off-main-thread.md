---
type: bug
status: done
artifact_type: user-story
priority: low
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies:
  - U-012
  - U-015
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-035
title: Move plaintext-to-encrypted credential migration off the main thread
pipeline: ''
domain: modernization
requirement_source: bug:BUG-003
sprint: '000005'
---

# U-035: Move plaintext-to-encrypted credential migration off the main thread

## Story

As a user launching the app,
I want the one-time credential migration to run without blocking the main thread,
so that startup stays responsive (no StrictMode disk violation / ANR risk) while credentials remain correct and available.

## Acceptance Criteria

- [ ] AC-1: Given the app is launching for the first time after upgrade (migration not yet complete — `__secretstore_migration_complete__` absent from the encrypted store and `encrypted == null` in `EncryptedPrefsSecretStore`), when `App.onCreate()` executes, then `preferences.migrate()` does NOT run synchronously on the main thread; the blocking disk/crypto I/O chain (`App.onCreate` -> `Preferences.migrate` -> `AuthPreferences.migrate` -> `EncryptedPrefsSecretStore.migrateFromPlaintext` -> `getEncrypted`) no longer originates from the main thread.

- [ ] AC-2: Given the app has launched and migration has not yet completed, when `App.onCreate()` finishes and the main thread is inspected via logcat with `StrictMode` tag filtering, then no `StrictMode` disk-write violation with a stack trace rooted in `EncryptedPrefsSecretStore.migrateFromPlaintext`, `getEncrypted`, or `App.onCreate` appears; the `penaltyFlashScreen()` screen flash attributable to the migration is absent.

- [ ] AC-3: Given the off-thread migration runs to completion, when both disk-write operations in `migrateFromPlaintext()` execute, then BOTH `commit()` calls run on the background thread — the `commit()` at `EncryptedPrefsSecretStore.java:251` (encrypted write + `MIGRATION_COMPLETE_KEY` marker) AND the `commit()` at lines 254-258 (plaintext `login_password`/`oauth2_token`/`oauth2_refresh_token` clear); a fix that relocates only `getEncrypted()` off-thread while leaving either `commit()` on the main thread is NOT compliant; `commit()` must NOT be replaced with `apply()` (the durability barrier per CNTR-MODERNIZATION-003 depends on synchronous commit semantics — the background thread blocks, not the main thread).

- [ ] AC-4: Given migration runs off the main thread with a completion gate (SA option A: off-thread dispatch + gate mechanism such as `CompletableDeferred<Unit>`, `CountDownLatch`, or `Mutex`), when a credential consumer (e.g., a scheduled `BackupWorker` from U-015) first calls `AuthPreferences.getPassword()`, `getOAuth2Token()`, or `getOAuthRefreshToken()`, then the consumer blocks on the gate until migration is complete before reading credentials, guaranteeing it does not observe a mid-migration or pre-migration state; the race-safety argument and the chosen gate mechanism must be documented in code comments co-located with the gate declaration; credentials are correct and available at first read.

- [ ] AC-5: Given the migration ran to completion, when `EncryptedPrefsSecretStore.contains(MIGRATION_COMPLETE_KEY)` is checked, then it returns `true`; the idempotency short-circuit at `EncryptedPrefsSecretStore.java:207` (`encrypted != null && encrypted.contains(MIGRATION_COMPLETE_KEY)`) fires on the second launch and the migration body is not re-entered (confirmed by absence of the "migration complete" log line on second launch); the U-012 rollback-safe four-step ordering is preserved exactly — (1) buffer plaintext into `pw`/`at`/`rt` from the raw `SharedPreferences` handle BEFORE `getEncrypted()` is called (Option-A ordering), (2) open encrypted store via `getEncrypted()`, (3) write values + marker + `commit()`, (4) clear plaintext + `commit()` ONLY after step-3 `commit()` returns; if interrupted before the marker is written, re-launch completes migration without credential loss.

- [ ] AC-6: Given the app is used by an account with OAuth2 authentication (XOAuth), when the migration runs off-thread and the user relaunches after upgrade, then `oauth2_token` and `oauth2_refresh_token` are migrated before the first OAuth2 credential read; the user is NOT forced to re-authenticate (not logged out); the `secretStore.migrateFromPlaintext()` call at `AuthPreferences.java:344` continues to execute before the `useXOAuth()` early-return at line 346, regardless of dispatch strategy.

- [ ] AC-7: Given the unit test suite is executed with `./gradlew testDebugUnitTest`, when all tests complete, then: (a) the suite passes green (report the final passing test count; baseline is approximately 595 tests); (b) new or updated tests cover the off-thread migration path, the idempotency invariant, the not-logged-out invariant, and the completion-gate ordering (migrate `InMemorySecretStore` or an equivalent test seam if `EncryptedPrefsSecretStore` requires Keystore hardware); (c) `jacocoTestCoverageVerification` passes with per-package LINE coverage >= 70% for all packages modified by this story.

- [ ] AC-8 (build gate): Given the fix is applied, when `./gradlew assembleDebug` is executed, then the build completes green with no compilation errors or new lint warnings; no production behavior changes beyond the threading dispatch (the migration still performs exactly the steps U-012 specified, in the same order, with the same durability semantics).

### Verification Note (AC-2 / on-device confirmation)

StrictMode-violation-absence for the migration path is best confirmed on-device or via Robolectric with a full `App.onCreate()` execution path (API 24+ device or emulator, first-run-after-upgrade state). The threading change plus passing unit tests are the primary device-free evidence. An on-device or Robolectric integration check is recommended before marking this story done but is not a mandatory CI gate given the low priority.

### Integration Criteria

- [ ] IC-1: If the `preferences.migrate()` call site in `App.java:163` is changed or removed, all callers of `Preferences.migrate()` across the codebase are updated consistently and the migration still fires on every app launch until the `MIGRATION_COMPLETE_KEY` marker is present.
- [ ] IC-2: If the migration is dispatched via a `WorkManager` one-shot worker, the worker's task ID is unique and does not collide with any existing task ID registered in `WorkManagerScheduler`; the worker uses `enqueueUniqueWork` with `ExistingWorkPolicy.KEEP` policy.
- [ ] IC-3: The completion gate mechanism (however implemented) is accessible from all credential-access call sites that may execute before migration finishes (at minimum: `AuthPreferences.getPassword()`, `getOAuth2Token()`, `getOAuthRefreshToken()`); no credential read path bypasses the gate.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/App.java` | `preferences.migrate()` called synchronously on the main thread at line 163 in `onCreate()`, after `setupStrictMode()` has armed `detectDiskWrites()` at line 152 | Dispatch migration off the main thread; introduce completion gate so first credential access awaits migration; update or replace line 163 call site |
| `app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java` | `migrate()` at line 294-295 delegates synchronously to `new AuthPreferences(context).migrate()` | May require threading/dispatch coordination if call site responsibility moves; no change to migration logic |
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | `migrate()` at line 333 calls `secretStore.migrateFromPlaintext()` synchronously; `migrateFromPlaintext()` sits before the `useXOAuth()` early-return at line 346 | No change to migration logic or call ordering; change is in dispatch context only; gate wiring may be added if credential reads are gated here |
| `app/src/main/java/com/zegoggles/smssync/preferences/EncryptedPrefsSecretStore.java` | `migrateFromPlaintext()` (lines 196-261) and `getEncrypted()` (lines 83-108) perform blocking Keystore IPC + disk I/O; both `commit()` calls (lines 251, 254-258) are synchronous disk writes on the calling thread | No change to internal logic or `commit()` semantics; the entire method body must be called from a background thread after the fix |

## Existing Behavior to Preserve

- **U-012 idempotency**: The migration runs exactly once. The `__secretstore_migration_complete__` marker in the encrypted store gates re-entry via the fast-path check at `EncryptedPrefsSecretStore.java:207` (`encrypted != null && encrypted.contains(MIGRATION_COMPLETE_KEY)`) and the post-open check at line 239. Both checks must remain intact regardless of dispatch strategy.
- **U-012 rollback safety**: The four-step sequence — (1) buffer plaintext from raw `SharedPreferences` handle into local variables, (2) open encrypted store via `getEncrypted()`, (3) write values + marker + `commit()`, (4) clear plaintext only after step-3 `commit()` returns — must not be reordered. Step 4's `commit()` must not execute if step 3's `commit()` fails.
- **U-012 Option-A ordering**: The raw `SharedPreferences("credentials", MODE_PRIVATE)` handle must be read into memory (`pw`/`at`/`rt`) BEFORE `getEncrypted()` constructs `EncryptedSharedPreferences` over the same backing file. This ordering is currently enforced at `EncryptedPrefsSecretStore.java:215-230` and must not be broken.
- **U-012 OAuth2 not-logged-out**: `oauth2_token` and `oauth2_refresh_token` are migrated before the first OAuth2 credential read. The `secretStore.migrateFromPlaintext()` call at `AuthPreferences.java:344` occurs before the `useXOAuth()` early-return at line 346; this must remain true after any deferral.
- **U-011 / U-007 security posture**: Credentials remain encrypted at rest in `EncryptedSharedPreferences` (AES-256-GCM). No fix path may read or write plaintext credentials outside the migration sequence.
- **U-012 backup exclusion invariant**: The `credentials.xml` backing file name remains `"credentials"` so the existing `backup_descriptor.xml` exclusion rule continues to match; do not rename the backing file.
- **Durability semantics**: Both `commit()` calls in `migrateFromPlaintext()` must remain `commit()`, not `apply()`. CNTR-MODERNIZATION-003 requires synchronous durability on the calling thread. The fix moves the calling thread to a background worker — it does not change the commit semantics.
- **`detectDiskReads()` robustness**: The idempotency probe `encrypted.contains(MIGRATION_COMPLETE_KEY)` (line 207) and the `getEncrypted()` keyset load perform disk reads. The fix must be robust to `detectDiskReads()` being re-enabled in `setupStrictMode()` in future; no new disk read must originate from the main thread via the migration path.

## Verification Steps

1. Install a build with the fix on a fresh emulator or device (API 24+) that has not previously run the app after upgrade (migration not yet complete: `__secretstore_migration_complete__` key absent, `encrypted` field null). Run `adb logcat -s StrictMode` before launch.
2. Launch the app. Confirm: no `StrictMode` disk-write violation with a stack trace rooted in `EncryptedPrefsSecretStore.migrateFromPlaintext`, `getEncrypted`, or `App.onCreate` appears in logcat. (AC-1, AC-2)
3. Wait for the migration to complete (logcat: `"EncryptedPrefsSecretStore: plaintext-to-encrypted credential migration complete."`). Confirm via `adb shell` or test instrumentation that `__secretstore_migration_complete__` is present in the encrypted store (`EncryptedPrefsSecretStore.contains(MIGRATION_COMPLETE_KEY)` returns true). (AC-5)
4. Confirm no plaintext credential values (`login_password`, `oauth2_token`, `oauth2_refresh_token`) remain in the raw `SharedPreferences("credentials")` store after migration completes. (AC-5)
5. Kill the app immediately after launch but before logcat reports migration complete (interrupt mid-migration). Relaunch. Confirm: migration re-runs and completes on second launch; no credential values are lost. (AC-5 rollback safety)
6. With an existing OAuth2-authenticated account, upgrade and relaunch. Confirm: the user is not prompted to re-authenticate; the backup flow proceeds normally after migration completes; `oauth2_token` and `oauth2_refresh_token` are readable and correct. (AC-6)
7. Run `./gradlew testDebugUnitTest`. Confirm: all tests pass; count is at or above baseline (~595); `jacocoTestCoverageVerification` passes (LINE >= 70% per package). (AC-7)
8. Run `./gradlew assembleDebug`. Confirm: build is green with no compilation errors. (AC-8)
9. Relaunch the app a second time (migration already complete). Confirm: no `StrictMode` violation and no "migration complete" log line on second launch (idempotency short-circuit fires). (AC-5)
10. Review the completion gate code co-location: confirm the gate declaration includes an inline comment documenting the race-safety argument and the chosen mechanism. (AC-4)

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java/Kotlin) | Off-thread dispatch of `preferences.migrate()`, completion gate wiring, credential-read gating, unit tests | Developer |

## Technical Context

**Root violation path (confirmed from source):**
- `App.java:163` — `preferences.migrate()` called synchronously after `setupStrictMode()` at line 152 has armed `detectDiskWrites()` + `penaltyFlashScreen()` (lines 328-335; no `penaltyDeath()`, no `detectDiskReads()`).
- `Preferences.java:294-295` — delegates to `new AuthPreferences(context).migrate()`.
- `AuthPreferences.java:344` — calls `secretStore.migrateFromPlaintext()`.
- `EncryptedPrefsSecretStore.java:83-108` (`getEncrypted()`) — `MasterKey.Builder` + `EncryptedSharedPreferences.create()`: Keystore IPC (hardware-backed AES-256-GCM key generation or retrieval), Tink keyset load, backing-file disk I/O. Latency is unbounded on the main thread; Keystore access on some devices involves IPC to the `keystore` daemon.
- `EncryptedPrefsSecretStore.java:251` — first `commit()`: encrypted credential write + `MIGRATION_COMPLETE_KEY` marker (synchronous disk write, durability barrier).
- `EncryptedPrefsSecretStore.java:254-258` — second `commit()`: plaintext credential clear (synchronous disk write, rollback-safe final step). Both `commit()` calls are on the main thread today.

**Recommended approach (SA option A — off-thread + completion gate):**
Dispatch `preferences.migrate()` from `App.onCreate()` to a background context (e.g., `Dispatchers.IO` coroutine, consistent with U-015 `CoroutineWorker`/`WorkManager` patterns). Introduce a completion gate — a `CompletableDeferred<Unit>`, `CountDownLatch(1)`, or equivalent — that is signaled when `migrateFromPlaintext()` returns. Gate the first call to each credential-read method (`AuthPreferences.getPassword()`, `getOAuth2Token()`, `getOAuthRefreshToken()`) on the gate so that an early credential consumer (e.g., a `BackupWorker` scheduled within the same launch) does not read mid-migration or pre-migration state. Option A is recommended over lazy-deferral (option B) because `BackupWorker` (U-015) may need credentials early in its run and a startup-time gate provides the strongest correctness guarantee without re-introducing a main-thread blocking path.

**Both `commit()` calls must move — moving only `getEncrypted()` is incomplete:**
A fix that relocates only the `EncryptedSharedPreferences` construction off-thread but leaves either `commit()` on the main thread still violates the StrictMode disk-write policy. Both the encrypted write (line 251) and the plaintext clear (lines 254-258) are synchronous disk writes; the entire `migrateFromPlaintext()` body must execute on a background thread.

**`commit()` must remain `commit()` — not `apply()`:**
The durability barrier is intentional per CNTR-MODERNIZATION-003. The rollback-safe ordering depends on step-3's `commit()` returning synchronously before step 4 executes. The correct remedy is to run the whole sequence on a thread where blocking I/O is acceptable — not to degrade to `apply()` semantics.

**Keystore-IPC cost:**
`MasterKey.Builder.build()` on first call involves IPC to the Android Keystore daemon (TEE or StrongBox hardware). This is the primary source of unbounded latency on first-run-after-upgrade. Subsequent launches hit the `encrypted != null` fast-path at line 207 and skip the migration body entirely.

**U-015 background patterns:**
U-015 established `CoroutineWorker`/`WorkManager` as the project's background work patterns. A coroutine dispatched to `Dispatchers.IO` from `App.onCreate()` is the lightest-weight implementation consistent with that precedent. A full `WorkManager` one-shot worker is also viable (IC-2 governs task-ID uniqueness in that case).

**Early-consumer trade-off:**
`BackupWorker` may be scheduled and reach its first credential read within seconds of launch on a device that had a pending sync. The completion gate must be in place before `BackupWorker` can read credentials — not just before the next user-initiated action. Option A's eager dispatch + gate satisfies this; lazy first-access deferral (option B) risks a race between concurrent credential readers at startup.

**Priority framing:**
This is LOW priority. Today `penaltyFlashScreen()` is the only active StrictMode penalty — no `penaltyDeath()`, so the violation is logged-and-flashed but not fatal. The ANR risk is real but unobserved in practice on the CI test device. This story must not block any higher-priority sprint work.

## Supporting Documentation

- BUG-003: `sdlc/artifacts/stories/BUG-003-credential-migration-main-thread-io.md` — source bug with evidence, impact analysis, SA advisory, and verification steps
- U-011: `EncryptedPrefsSecretStore` and `SecretStore` interface (introduced the affected class)
- U-012: One-time plaintext-to-encrypted credential migration (introduced `App.onCreate` call site and `migrateFromPlaintext()` logic)
- U-015: Background `CoroutineWorker`/`WorkManager` patterns (background dispatch precedent)
- CNTR-MODERNIZATION-003: SecretStore contract — migration semantics, durability barrier, rollback-safety invariant

## Integration Contract References

- CNTR-MODERNIZATION-003 — durability barrier: `commit()` semantics for both migration commits; rollback-safe four-step ordering invariant; this story must not weaken any contract clause

## Estimation Guidance

Low-medium. The threading dispatch itself is a small change (a few lines at the `App.onCreate` call site). The correctness risk is concentrated in the completion gate: the gate must guarantee that every credential-read path awaits migration without deadlocking, and must handle the case where migration was already complete on a prior launch (gate signals immediately, no suspend). Test seaming for `EncryptedPrefsSecretStore` (which requires a real Keystore provider) is the secondary complexity driver. Allow additional time to verify race-safety reasoning is documented and reviewed.

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes
