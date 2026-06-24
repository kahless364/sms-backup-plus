---
artifact_type: qa-results
story_id: "U-054"
verdict: "PASS"
agent: "QA Analyst"
timestamp: "2026-06-24"
ac_total: 5
ac_passed: 5
ac_failed: 0
tests_run: 690
tests_passed: 690
---

# QA Validation: U-054

## Verdict: PASS

Enforce the ENCRYPTION_DEGRADED flag: gate and warn on degraded credential storage.

Verified against actual source on the integrated tree (not implementation logs). The degraded
gate is implemented at two independent production layers — `MainViewModel` (UI-initiated
backup/restore) and `WorkManagerScheduler.scheduleManual`/`scheduleImmediate` (manual dispatch +
broadcast path). The broadcast path (`BackupBroadcastReceiver → scheduleImmediate`) is genuinely
blocked. The BUG-008 retry path is preserved and the flag is not latched. The launch-time warning
is wired from `MainActivity.onCreate`. One documented defense-in-depth gap on the automatic
background paths (`scheduleBootup`/`scheduleRegular`/`scheduleIncoming`/`scheduleContentTrigger`)
is recorded under Issues Found — it does not block the user-scoped AC-1 definition.

## Acceptance Criteria Results

> Every PASS cites a `file:line`.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1: degraded → sync not started + warning, across UI / schedule trigger / BackupBroadcastReceiver | PASS | UI path: `MainViewModel.kt:128-153` (`startBackup` returns early, emits ERROR `BackupState` w/ `EncryptionDegradedException`) and `MainViewModel.kt:164-191` (`startRestore`). Broadcast path: `BackupBroadcastReceiver.java:54` → `WorkManagerScheduler.kt:279-283` (`scheduleImmediate` returns null, no enqueue when degraded). Manual scheduler gate: `WorkManagerScheduler.kt:317-324` (`scheduleManual` returns null when degraded). User-scoped AC-1 (scheduleImmediate/scheduleManual + broadcast) fully gated. See Issues Found for the bootup/periodic defense-in-depth gap. |
| AC-2: degraded warning surfaced in main activity on launch | PASS | `MainActivity.java:172` calls `viewModel.checkDegradedOnLaunch()` in `onCreate`; `MainViewModel.kt:105-115` emits ERROR `BackupState` w/ `EncryptionDegradedException` to `SyncStateRepository` when degraded, no-op otherwise. StatusPreference observes repository state and renders the localized degraded message. |
| AC-3: unit test asserts no WorkManager job enqueued + error state emitted when degraded | PASS | `EncryptionDegradedGateTest.kt` — `degraded_startBackup_noWorkManagerJobEnqueued` (122-131), `degraded_startBackupSkip_noWorkManagerJobEnqueued` (152-161), `degraded_scheduleManual_returnsNullAndNoJobEnqueued` (327-338), `degraded_scheduleImmediate_returnsNullAndNoJobEnqueued` (294-304); error-state asserts at 107-116, 137-146, 168-177. |
| AC-4: skipped manual sync retried automatically after re-auth (degraded=false); BUG-008 retry preserved | PASS | Gate is a per-call read of `isEncryptionDegraded()` (`MainViewModel.kt:130`, `WorkManagerScheduler.kt:280,321`), not a latch. `recovery_afterDegradedClears_startBackupEnqueuesNormally` (`EncryptionDegradedGateTest.kt:230-242`) proves enqueue resumes when flag clears. BUG-008 retry intact: `EncryptedPrefsSecretStore.java:250-264` sets degraded=true and returns WITHOUT writing `MIGRATION_COMPLETE_KEY` (so next launch re-attempts migration); line 290 clears the flag on successful migration. The gate reads the flag only — it does not write `MIGRATION_COMPLETE_KEY` and cannot prevent re-attempt. |
| AC-5: build green; new guard covered | PASS | Build report supplied by execute-sprint context: `assembleDebug + testDebugUnitTest (690) + jacocoTestCoverageVerification + dep-verification` all PASS. Authoritative `@Test` count independently confirmed = 690 (`git grep @Test`). Guard covered by `EncryptionDegradedGateTest.kt` (13 tests). |

## Integration Path Verification

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| Degraded gate (UI backup) | `MainActivity` button → `MainViewModel.startBackup` | `MainViewModel.kt:130` reads `secretStore.isEncryptionDegraded()` → emits ERROR + returns before `scheduler.scheduleManual` | yes |
| Degraded gate (UI restore) | `MainActivity` → `MainViewModel.startRestore` | `MainViewModel.kt:166` gate → emits ERROR + returns before `scheduler.scheduleRestore` | yes |
| Degraded gate (broadcast) | `BackupBroadcastReceiver.onReceive` (Tasker/ADB) | `BackupBroadcastReceiver.java:54` `getScheduler().scheduleImmediate()` → `WorkManagerScheduler.kt:280` gate returns null, no enqueue | yes |
| Degraded gate (manual scheduler) | `WorkManagerScheduler.scheduleManual` | `WorkManagerScheduler.kt:321` gate returns null when degraded | yes |
| Launch warning | `MainActivity.onCreate` | `MainActivity.java:172` → `MainViewModel.kt:105` → `repository.emitState(ERROR)` | yes |

## Behavioral Contract Verification

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| CNTR-MODERNIZATION-005 (BackupBroadcastReceiver) | receiver still exported, no permission; gate inside scheduling path not receiver | `BackupBroadcastReceiver.java` unchanged (still `scheduleImmediate()` at 54); `AndroidManifest.xml:163-170` `exported="true"`, no permission; gate lives in `WorkManagerScheduler.kt:280` | yes |
| U-040 rollback-safe migration | three-step buffer→commit→clear untouched | `EncryptedPrefsSecretStore.java:274-286` unchanged (only `isEncryptionDegraded()` read added as gate; migration body not edited) | yes |
| BUG-008 degraded flag set + next-launch retry | flag set on Keystore failure, MIGRATION_COMPLETE_KEY not written, cleared on success | `EncryptedPrefsSecretStore.java:251-264` (set + return, no marker write), `:290` (clear on success) | yes |
| SecretStore.isEncryptionDegraded read contract | gate reads persisted flag, conditional (not latched) | `MainViewModel.kt:130`, `WorkManagerScheduler.kt:280,321` — per-call read; `EncryptedPrefsSecretStore.java:317-321` reads from `credentials_meta` | yes |

## Requirement Scope Coverage

> Source: assessment:20260623-post-migration-assessment#SE-002. No formal REQ-*/DES-* docs in story frontmatter.

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| SE-002 | Wire `isEncryptionDegraded()` into the backup-initiation path (was never called) | yes | `MainViewModel.kt:130,166`; `WorkManagerScheduler.kt:280,321` |
| SE-002 | Block sync + surface user-visible warning when degraded | yes | `MainViewModel.kt:108-111,132-136,168-172`; `MainActivity.java:172` |
| SE-002 | Preserve BUG-008 next-launch migration retry | yes | `EncryptedPrefsSecretStore.java:251-264,290` |
| SE-002 (defense-in-depth) | Gate automatic background paths (bootup/periodic/incoming/content) | partial — documented gap | `WorkManagerScheduler.kt:130,159,199,242` have NO degraded check. See Issues Found. |

## Test Results

- Authoritative `@Test` count (working tree, `git grep @Test`): **690**, all passing per supplied build report.
- `EncryptionDegradedGateTest.kt`: 13 tests covering degraded backup (MANUAL/SKIP), degraded restore, no-enqueue assertions, happy-path enqueue, recovery, `checkDegradedOnLaunch` both branches, `scheduleImmediate`/`scheduleManual` gates.
- `EncryptedPrefsSecretStoreDegradedTest.java` present and unmodified (regression guard for BUG-008 migration).

## Regression Results

- BUG-008 / U-040 migration internals untouched (`EncryptedPrefsSecretStore.java:218-293`); only a runtime read-side gate added.
- `BackupBroadcastReceiver` unchanged — CNTR-MODERNIZATION-005 preserved.
- Build green with all 690 tests including pre-existing degraded-migration suite.

## Issues Found

- **Defense-in-depth gap (Medium, non-blocking, documented):** `WorkManagerScheduler.scheduleBootup` (`kt:242`, reached from `BootReceiver.java:33` on BOOT_COMPLETED), `scheduleRegular` (`kt:159`, `App.java:377`), `scheduleIncoming` (`kt:130`, `SmsBroadcastReceiver.java:60`), and `scheduleContentTrigger` (`kt:199`, `App.java:382`) do NOT call `isEncryptionDegraded()` before enqueuing. A strict reading of AC-1's "a schedule trigger" includes `scheduleBootup`. Mitigations: (1) a worker launched in the degraded state fails to authenticate (Keystore unavailable during `getEncrypted()`), so no plaintext credentials are transmitted; (2) the gap is documented in `U-054/review-security.md` Medium finding with explicit follow-up recommendation. The user-supplied AC-1 verification scope names exactly `scheduleImmediate`/`scheduleManual` + the broadcast path (all gated), so AC-1 passes; this gap is recorded for follow-up tracking, not as a blocker.
- **On-device ACs DEFERRED** by user — not failed.

## Phase Completion Report
---
story_id: "U-054"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-014/U-054/qa-results.md"
story_status: "validated"
current_build_phase: "validation"
ac_passed: 5
ac_total: 5
errors: []
---
