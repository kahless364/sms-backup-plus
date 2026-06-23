---
artifact_type: implementation-log
story_id: "U-053"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-23"
files_changed: 2
files_created: 1
tests_added: 0
tests_passing: 643
---

# Implementation Log: U-053 — Cover the real BackupWorker execution body via a testable seam

## Summary

Extracted the BUG-010 watermark-gating logic from the inline body of `BackupWorker.backupCursors`
into a named `internal` method `appendBatchAndUpdateWatermark`. Rewrote `BackupWorkerWatermarkTest`
to call that production seam directly via a real `BackupWorker` instance (built with
`TestableBackupWorkerFactoryWithTransport`). The local `processWatermarkLoop` copy was deleted.
All 6 BUG-010 regression tests continue to pass against production code. Build and JaCoCo
coverage gate both passed.

## Seam Extracted

**Method:** `BackupWorker.appendBatchAndUpdateWatermark`
**Location:** `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:402-422`
**Visibility:** `internal` + `@VisibleForTesting(otherwise = PRIVATE)`

The method encapsulates the three-step BUG-010 invariant:
1. `transport.openFolder(type, dataTypePreferences)` — throws on failure; watermark never touched
2. `transport.appendMessages(folder, result)` — throws on failure; watermark never touched
3. `dataTypePreferences.setMaxSyncedDate(type, confirmedMaxDate)` — only reached on confirmed append

`backupCursors` (the production loop at line 344) delegates to `appendBatchAndUpdateWatermark` for
each non-empty batch, preserving all prior behavior including cooperative cancellation
(`ensureActive()` at line 329), calendar sync (line 347), progress emission (line 358), and
`transport.closeFolders()` in the `finally` block (line 375).

## How BackupWorkerWatermarkTest Now Exercises Production Code

Tests build a real `BackupWorker` via:
```kotlin
TestListenableWorkerBuilder<BackupWorker>(context)
    .setWorkerFactory(BackupWorker.TestableBackupWorkerFactoryWithTransport(transport))
    .build() as BackupWorker
```
and then call:
```kotlin
worker.appendBatchAndUpdateWatermark(transport, type, result, dataTypePreferences)
```
directly. This invokes the production seam rather than a local copy. Any regression in
`appendBatchAndUpdateWatermark` will be caught by these tests.

## Files Modified

| File | Change |
|------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` | Added imports for `ConversionResult` and `DataTypePreferences`; extracted `appendBatchAndUpdateWatermark` seam (lines 379-422); updated `backupCursors` to call the seam at line 344 (replacing the 6 inline lines with one call). |
| `app/src/test/java/com/zegoggles/smssync/service/BackupWorkerWatermarkTest.kt` | Deleted `processWatermarkLoop` local helper; added `buildWorker(StubMailTransport)` helper; rewrote all 6 test methods to call `worker.appendBatchAndUpdateWatermark(...)` against a real `BackupWorker` instance. |

## Files Created

| File | Purpose |
|------|---------|
| `sdlc/artifacts/build/sprints/sprint-013/U-053/implementation-log.md` | This file |

## BUG-010 Cases Retained

All 3 categories of BUG-010 assertions survive, now against production code:

| Test | Case | What is asserted |
|------|------|-----------------|
| `ac3a_openFolderFails_watermarkUnchanged` | failure | openFolder throws → watermark stays at -1 |
| `ac3a_appendMessagesFails_watermarkUnchanged` | failure | appendMessages throws → watermark stays at -1 |
| `ac3a_loginFails_watermarkUnchanged` | failure | RequiresLoginException from openFolder → watermark stays at -1 |
| `ac3b_successfulBatch_watermarkAdvancesToConfirmedDate` | success | appendMessages returns SMS_DATE_1 → watermark = SMS_DATE_1 exactly |
| `ac3b_twoSuccessfulBatches_watermarkAdvancesToSecondDate` | success | two successful calls → watermark = SMS_DATE_2 (second batch) |
| `ac3c_partialSuccess_watermarkEqualsBatch1MaxDate` | partial | batch 1 ok, batch 2 throws → watermark = SMS_DATE_1 only |

## Integration Path

`appendBatchAndUpdateWatermark` is:
- **Called from** `backupCursors` at `BackupWorker.kt:344` — reachable via `fetchAndBackupItems` → `executeBackup` → `doWork()`
- **Directly tested by** `BackupWorkerWatermarkTest` via `worker.appendBatchAndUpdateWatermark(...)` (the production seam)

No dead code introduced — the seam is both a production call site (via `backupCursors`) and a test entry point.

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|---------|
| AC-1: Named seam encapsulates watermark-gating; no duplication | PASS | `appendBatchAndUpdateWatermark` at BackupWorker.kt:379-422; `processWatermarkLoop` deleted |
| AC-2: `BackupWorkerWatermarkTest` invokes the seam on production `BackupWorker` | PASS | All 6 tests call `worker.appendBatchAndUpdateWatermark(...)` on a real worker |
| AC-3: `processWatermarkLoop` deleted; grep returns zero results | PASS | `grep -r "processWatermarkLoop" app/src/test/` → 0 results |
| AC-4: All assertions pass with same semantics; BUG-010 invariant not weakened | PASS | 6/6 tests pass; same watermark assertions |
| AC-5: Build green; extracted seam covered by updated test | PASS | `BUILD SUCCESSFUL`; seam covered by 6 tests |

## Test Results

- **@Test count (HEAD before commit):** 643
- **@Test count (working tree):** 643 (same — 6 watermark tests unchanged in count, rewritten in body)
- **Build:** `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` → `BUILD SUCCESSFUL in 2m 44s`
- **processWatermarkLoop grep:** 0 results in `app/src/test/`

## Regression Results

No regressions. The extracted seam is a pure refactor of the existing inline block:
- `backupCursors` behavior unchanged (same 3 steps, same exception propagation)
- `closeFolders()` in `finally` block preserved
- `ensureActive()` cooperative cancellation in the loop preserved
- `calendarSyncer.syncCalendar(result)` per CALLLOG batch preserved
- Progress emission (`setProgress`) preserved

## Notes

- `appendBatchAndUpdateWatermark` is `internal` (Kotlin module-scoped), accessible from test
  sources in the same Gradle module without reflection. The `@VisibleForTesting(otherwise = PRIVATE)`
  annotation documents the intent: it exists at `internal` visibility solely for test access.
- The `BackupWorker.TestableBackupWorkerFactoryWithTransport` seam (from U-042) was reused to
  construct the real worker with a `StubMailTransport` injected. No new test infrastructure needed.
- U-052 (GreenMail integration / remove BackupWorker jacoco exclusion) is unaffected — jacoco
  config was not touched per the story's explicit instruction.
