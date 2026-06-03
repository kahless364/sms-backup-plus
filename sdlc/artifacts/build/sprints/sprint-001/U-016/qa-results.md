---
artifact_type: qa-results
story_id: "U-016"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-03"
tests_run: 583
tests_passed: 583
tests_failed: 0
tests_skipped: 2
coverage_gate: PASS
build_gate: PASS
---

# QA Results: U-016 — Durable Restore Checkpoint

## Gates

| Gate | Command | Result |
|------|---------|--------|
| Unit tests | `./gradlew :app:testDebugUnitTest` | PASS (583 run, 0 failed, 2 skipped) |
| Coverage | `./gradlew :app:jacocoTestCoverageVerification` | PASS (≥70% service*, mail*, auth*) |
| Debug build | `./gradlew :app:assembleDebug` | PASS |

## Fault-Injection Test Coverage (AC-4 canonical + supporting)

| Test | AC | Description | Result |
|------|----|----|--------|
| `ac4_faultInjection_N20_K7_countEqualsN_noDuplicates` | AC-4 | N=20, K=7, assert count=20, no dupes, SUCCEEDED | PASS |
| `ac1_resumeAfterCrash_10messages_crash5_finalCount10` | AC-1 | N=10, K=5, resume, assert 10 rows | PASS |
| `ac2_checkpointWrittenAfterInsert_beforeLoopAdvance` | AC-2 | Ordering: insert → write → interceptor | PASS |
| `ac3_zeroDuplicates_crashWindowAfterInsertBeforeCheckpoint` | AC-3 | Crash before write, dedup prevents double-insert | PASS |
| `ac5_resumesFromCheckpoint_notFromZero` | AC-5 | Pre-written checkpoint=5, starts from index 6 | PASS |
| `ac6_checkpointClearedOnSucceeded` | AC-6 | clear() called, read returns NO_CHECKPOINT | PASS |
| `ac7_checkpointRetainedOnInterruption_notClearedOnCrash` | AC-7 | checkpoint=3 retained after 4-insert crash | PASS |
| `idempotency_secondRunNoDuplicates` | IC | Second run: 0 new rows, dedup active | PASS |

## Test Infrastructure Honesty Note

The AC-4 fault-injection test is genuine: it uses `RestoreWorker.executeRestoreWithValues()` (the
internal test seam) with a `FakeSmsContentProvider` registered via Robolectric's
`ShadowContentResolver.registerProviderInternal`. The fake provider implements insert/query with
the same date+address+type dedup semantics as the real `smsExists()` guard. `CrashAfterK(K)` throws
`SimulatedCrashException` after K confirmed inserts; the second worker invocation reads the
checkpoint and resumes correctly.

Limitation: the IMAP path (`doWork()` → `executeRestore()` → `runImapRestoreLoop()`) is not
exercised end-to-end in unit tests (requires a live IMAP store). The checkpoint logic is fully
exercised via the `executeRestoreWithValues` path which shares all the same checkpoint store
read/write/clear calls. The IMAP path is exercised structurally by the `doWork()` invocation tests
in `RestoreWorkerTest` (INV-3 cap, construction, WorkManager enqueue/cancel).

## New Tests Added (U-016 only)

**RestoreWorkerCheckpointTest.kt** (15 tests):
1. `ac4_faultInjection_N20_K7_countEqualsN_noDuplicates`
2. `ac1_resumeAfterCrash_10messages_crash5_finalCount10`
3. `ac2_checkpointWrittenAfterInsert_beforeLoopAdvance`
4. `ac3_zeroDuplicates_crashWindowAfterInsertBeforeCheckpoint`
5. `ac5_resumesFromCheckpoint_notFromZero`
6. `ac6_checkpointClearedOnSucceeded`
7. `ac7_checkpointRetainedOnInterruption_notClearedOnCrash`
8. `idempotency_secondRunNoDuplicates`
9. `inMemoryStore_read_returnsNoCheckpointWhenEmpty`
10. `inMemoryStore_write_thenRead_returnsValue`
11. `inMemoryStore_clear_resetsToNoCheckpoint`
12. `crashAfterK_doesNotThrowBefore_K`
13. `crashAfterK_throwsOnKthCall`
14. `sharedPrefsStore_write_thenRead_roundTrip`
15. `sharedPrefsStore_clear_resetsToNoCheckpoint`

**RestoreWorkerTest.kt** (3 new tests):
16. `u016_checkpointStore_isInjectedAndAccessible`
17. `u016_insertInterceptor_isInjectedAndAccessible`
18. `u016_checkpointKeyConstant_isStable`
