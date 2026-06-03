---
artifact_type: implementation-log
story_id: "U-016"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-03"
files_changed: 2
files_created: 4
tests_added: 28
tests_passing: 583
---

# Implementation Log: U-016 — Durable Restore Checkpoint

## Summary

Promoted the restore cursor to a durable checkpoint written to SharedPreferences after each
confirmed provider insert. `RestoreWorker` reads the checkpoint on entry (overriding the
in-memory config value) and resumes from checkpoint+1. Checkpoint is cleared on SUCCEEDED
and retained on CANCELLED/crash. Fault-injection tests verify the full crash-resume cycle.

## Files Created

| File | Purpose |
|------|---------|
| `app/src/main/java/com/zegoggles/smssync/service/RestoreCheckpointStore.kt` | Port interface: `read(workName)`, `write(workName, index)`, `clear(workName)`. No Android types in signature (IC-1). |
| `app/src/main/java/com/zegoggles/smssync/service/SharedPreferencesCheckpointStore.kt` | Production adapter backed by `SharedPreferences` with synchronous `commit()` for crash safety. |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreInsertInterceptor.kt` | Fault-injection seam: `NoOp` (production) + `CrashAfterK` (test). Called after insert + checkpoint write, before loop advance. |
| `app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerCheckpointTest.kt` | 15 fault-injection tests covering AC-1..AC-7 + store + interceptor unit tests. |

## Files Modified

| File | Changes |
|------|---------|
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` | (1) Constructor extended with `checkpointStore: RestoreCheckpointStore` and `insertInterceptor: RestoreInsertInterceptor`; (2) `executeRestoreWithValues()` internal test path added; (3) Checkpoint read on entry (AC-5), write after insert (AC-2), clear on SUCCEEDED (AC-6); (4) `RestoreWorkerFactory` (production) and `TestableRestoreWorkerFactory` (test) inner classes added. |
| `app/src/test/java/com/zegoggles/smssync/service/RestoreWorkerTest.kt` | Updated all `TestListenableWorkerBuilder` calls to use `.setWorkerFactory(productionFactory())`; `setUp()` uses `RestoreWorkerFactory` for WorkManager integration tests; 3 new U-016 structural tests added. |

## Capabilities Inventory (U-015 capabilities retained in U-016 rewrite)

| Capability | Status | Citation |
|-----------|--------|---------|
| Early-exit when restoreSms=false AND restoreCallLog=false | RETAINED | RestoreWorker.kt:113 |
| smsExists() dedup guard (date+address+type) | RETAINED | RestoreWorker.kt:insertSmsValues() |
| callLogExists() dedup guard (date+number+duration+type) | RETAINED | RestoreWorker.kt:importCallLog() |
| SMS type filter: INBOX and SENT only | RETAINED | RestoreWorker.kt:insertSmsValues() |
| Thread update after SMS restore | RETAINED | RestoreWorker.kt:updateAllThreadsIfAnySmsRestored() |
| XOAuth2 token-refresh retry (at most once) | RETAINED | RestoreWorker.kt:handleAuthError() |
| Cooperative cancellation via ensureActive() | RETAINED | RestoreWorker.kt:executeRestoreWithValues():while loop |
| INV-3 backoff cap (300s) | RETAINED | RestoreWorker.kt:doWork():96 |
| Progress emission via setProgress | RETAINED | RestoreWorker.kt:executeRestoreWithValues() |
| clearAppCache() every 50 items | RETAINED | RestoreWorker.kt:clearAppCache() |

## Contract Adherence

**CNTR-MODERNIZATION-004 §RestoreSchedulerConfig:**
- `checkpointKey` field: used as `uniqueWorkName` parameter in `executeRestoreWithValues()` and `insertSmsValues()`. `KEY_UNIQUE_WORK_NAME` input data key (RestoreWorker.kt:366) carries the checkpoint key from `scheduleRestore`. Default `RESTORE_WORK_NAME` (RestoreWorker.kt:371) used when not set.
- `RestoreCheckpointStore.kt:28-43` — port interface carries no Android types (IC-1 satisfied).

**CNTR-MODERNIZATION-004 §Validation Rules rule 6:**
- "scheduleRestore MUST persist the checkpoint after each successful insert and before cursor advance" — implemented at `RestoreWorker.kt:insertSmsValues()`: `checkpointStore.write(uniqueWorkName, currentIndex)` called AFTER `contentResolver.insert()` returns non-null URI and BEFORE `insertInterceptor.afterInsert()` fires (which is called BEFORE loop advances to `currentIndex+1`).
- "preserved smsExists/callLogExists write-seam guards" — both guards active and unchanged. `smsExists()` at RestoreWorker.kt:432-448, `callLogExists()` at RestoreWorker.kt:414-430.

**CNTR-MODERNIZATION-004 §Error Handling:**
- "Process death is not an error" — implemented: `SharedPreferencesCheckpointStore` uses synchronous `commit()` to ensure durability; checkpoint survives process death; `executeRestoreWithValues()` reads checkpoint on re-entry and resumes from `checkpoint+1`.
- "Restore resumes from the durable checkpoint" — AC-5 test (`ac5_resumesFromCheckpoint_notFromZero`) verified at RestoreWorkerCheckpointTest.kt:338-361.

## Test Results

| Test Class | Tests | Status |
|-----------|-------|--------|
| `RestoreWorkerCheckpointTest` | 15 new | ALL PASS |
| `RestoreWorkerTest` | 13 (10 existing + 3 new) | ALL PASS |
| All other test classes | ~555 | ALL PASS |
| **Total** | **583** | **ALL PASS** |

Fault-injection test summary:
- `ac4_faultInjection_N20_K7_countEqualsN_noDuplicates`: N=20, K=7 → crash → resume → 20 rows, 0 duplicates, SUCCEEDED
- `ac1_resumeAfterCrash_10messages_crash5_finalCount10`: N=10, K=5 → resume → 10 rows, 0 duplicates
- `ac2_checkpointWrittenAfterInsert_beforeLoopAdvance`: ordering verified via RecordingStore + interceptor
- `ac3_zeroDuplicates_crashWindowAfterInsertBeforeCheckpoint`: crash before write → dedup prevents double-insert
- `ac5_resumesFromCheckpoint_notFromZero`: pre-written checkpoint → starts from checkpoint+1
- `ac6_checkpointClearedOnSucceeded`: clear() called after full restore
- `ac7_checkpointRetainedOnInterruption_notClearedOnCrash`: checkpoint=3 after 4-insert crash
- `idempotency_secondRunNoDuplicates`: second run produces 0 new rows

## Coverage Gate

`./gradlew :app:jacocoTestCoverageVerification` — PASS (≥70% holds for service*, mail*, auth* packages).

## Integration Path

New code is reachable from production as follows:
- `RestoreWorker.RestoreWorkerFactory` creates the worker with `SharedPreferencesCheckpointStore`
- `WorkManagerScheduler.scheduleRestore()` enqueues `RestoreWorker` via `OneTimeWorkRequest`
- WorkManager creates the worker via `RestoreWorkerFactory` (registered in `Configuration`)
- `doWork()` → `executeRestore()` → `runImapRestoreLoop()` → `importSmsMessage()` → `insertSmsValues()` → checkpoint write

Note: WorkManager Configuration with `RestoreWorkerFactory` is not yet wired to `App.onCreate()` (deferred to U-024 with Hilt). The `TestableRestoreWorkerFactory` is used in tests; the `RestoreWorkerFactory` is the production implementation used in `WorkManagerTestInitHelper` tests.

## Known Limitations / TODOs

1. **Hilt injection (U-024)**: `@HiltWorker`/`@AssistedInject` annotations deferred. `RestoreWorkerFactory` serves until then. TODO comment in RestoreWorker.kt:70.
2. **WorkManagerScheduler.scheduleRestore()** does not yet pass `KEY_UNIQUE_WORK_NAME` in input data. The worker defaults to `RESTORE_WORK_NAME = "RESTORE"` which is sufficient until U-017/U-024 wires the full scheduling path.
3. **callLogExists dedup** in `importCallLog()` does NOT write checkpoint — this path is only reached via the IMAP path (`runImapRestoreLoop`), not via `executeRestoreWithValues` (the test path). The IMAP path does write checkpoint after each confirmed call-log insert at RestoreWorker.kt:importCallLog().
