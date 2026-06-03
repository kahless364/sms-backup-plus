---
artifact_type: plan
story_id: "U-016"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-03"
contracts_verified:
  - CNTR-MODERNIZATION-004
risks_identified: 2
---

# Implementation Plan: U-016 — Durable Restore Checkpoint

## Story Overview

Promote `RestoreConfig.currentRestoredItem` to a durable store so `RestoreWorker` can resume
after process death. The checkpoint is written after each confirmed provider insert and before
the loop index advances. The existing `smsExists()`/`callLogExists()` dedup guards remain active
as the idempotency backstop for the crash window.

## Acceptance Criteria Mapping

| AC | Implementation Step | Notes |
|----|-------------------|-------|
| AC-1 Resume after kill | `executeRestoreWithValues` reads checkpoint on entry, starts from checkpoint+1 | InMemory + SharedPrefs stores |
| AC-2 Write ordering | `insertSmsValues`: insert → write checkpoint → interceptor → loop advances | Load-bearing invariant |
| AC-3 Zero duplicates | smsExists() dedup guard active for re-encountered items | No code change needed |
| AC-4 Fault-injection N=20 K=7 | `ac4_faultInjection_N20_K7_countEqualsN_noDuplicates` test | CrashAfterK + FakeSmsContentProvider |
| AC-5 Resume from checkpoint | `executeRestoreWithValues` reads checkpoint, overrides startIndex=0 | Test pre-writes checkpoint value |
| AC-6 Clear on SUCCEEDED | `checkpointStore.clear(uniqueWorkName)` called before `Result.success()` | |
| AC-7 Retain on CANCELLED | clear() NOT called on crash/cancellation path | SimulatedCrashException exits before clear |

## Implementation Steps

1. **RestoreCheckpointStore.kt** — port interface (no Android types); three suspending methods: read/write/clear
2. **SharedPreferencesCheckpointStore.kt** — production adapter; synchronous `commit()` for crash safety
3. **RestoreInsertInterceptor.kt** — fault-injection seam; NoOp (production) + CrashAfterK (test)
4. **RestoreWorker.kt** — modified constructor to accept checkpointStore + insertInterceptor; added `executeRestoreWithValues` test path; checkpoint read on entry, write after insert, clear on SUCCEEDED; RestoreWorkerFactory + TestableRestoreWorkerFactory inner classes
5. **RestoreWorkerCheckpointTest.kt** — 15 new tests covering AC-1..AC-7 + helpers + SharedPrefs round-trips
6. **RestoreWorkerTest.kt** — updated to use TestableRestoreWorkerFactory and RestoreWorkerFactory in setUp

## Files Modified/Created

| File | Action | Reason |
|------|--------|--------|
| `RestoreCheckpointStore.kt` | Created | Port interface (IC-1) |
| `SharedPreferencesCheckpointStore.kt` | Created | Production adapter (IC-1) |
| `RestoreInsertInterceptor.kt` | Created | Fault-injection seam (IC-4) |
| `RestoreWorker.kt` | Modified | Add checkpoint integration, factory classes |
| `RestoreWorkerCheckpointTest.kt` | Created | 15 fault-injection tests (AC-4) |
| `RestoreWorkerTest.kt` | Modified | Factory injection for worker construction |

## Contracts Verification

CNTR-MODERNIZATION-004 §RestoreSchedulerConfig: `checkpointKey` field used as the unique work name
for checkpoint keying. `scheduleRestore` passes `config.uniqueName` as `KEY_UNIQUE_WORK_NAME`
input data (deferred to matching WorkManagerScheduler update; default `RESTORE_WORK_NAME` used
when not set).

CNTR-MODERNIZATION-004 §Validation Rules rule 6: `scheduleRestore` MUST persist checkpoint after
each successful insert and before cursor advance. Implemented in `insertSmsValues()` with ordering:
insert → `checkpointStore.write()` → `insertInterceptor.afterInsert()` → loop advance.

CNTR-MODERNIZATION-004 §Error Handling: "Process death is not an error." Implemented: checkpoint
survives process death (SharedPreferences); re-execution reads checkpoint and resumes.

## Risks & Mitigations

1. **Risk**: Robolectric 4.12 has no working SMS shadow provider. **Mitigation**: `FakeSmsContentProvider` registered via `ShadowContentResolver.registerProviderInternal` — provides real insert/query semantics for the dedup key.
2. **Risk**: Hilt (U-024) not yet wired. **Mitigation**: `RestoreWorkerFactory` + `TestableRestoreWorkerFactory` inner classes provide manual DI until Hilt lands. TODO U-024 documented.

## Test Strategy

- `RestoreWorkerCheckpointTest`: 15 tests using `executeRestoreWithValues` + `FakeSmsContentProvider`
- `InMemoryCheckpointStore` shared across worker instances in a test, simulating durable persistence
- `CrashAfterK(K)` interceptor throws `SimulatedCrashException` after K inserts
- `FakeSmsContentProvider` implements smsExists() dedup key (date+address+type)
- AC-4 canonical test: N=20, K=7 → assert final count=20, no duplicates
