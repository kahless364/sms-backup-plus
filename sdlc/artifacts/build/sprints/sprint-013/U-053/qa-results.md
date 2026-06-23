---
artifact_type: qa-results
story_id: "U-053"
verdict: "PASS"
agent: "QA Analyst"
timestamp: "2026-06-23"
ac_total: 5
ac_passed: 5
ac_failed: 0
tests_run: 669
tests_passed: 669
---

# QA Validation: U-053 — Cover the real BackupWorker execution body via a testable seam

## Verdict: PASS

Independently verified against the actual `BackupWorker.kt` production code, the rewritten
`BackupWorkerWatermarkTest.kt`, a `grep` for the deleted helper, and the JaCoCo coverage report.
The watermark-gating logic is a single production seam, the test invokes it on a real worker, the
old local copy is gone, and the BUG-010 invariant survives. All five ACs pass.

> Minor documentation drift (non-blocking): the implementation log cites the seam at
> `BackupWorker.kt:379-422` / call site `:344`. The actual code has the seam at `:404-424` and the
> call site at `:346`. The seam exists, is wired, and is tested — the line numbers in the log are
> off by a few lines (likely from edits after the log was written). No functional impact.

## Acceptance Criteria Results

> Rule: Every AC marked PASS cites at least one `file:line` reference.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1: named seam encapsulates watermark-gating; single implementation, no duplication | PASS | `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:404-424` — `internal fun appendBatchAndUpdateWatermark(...)` encapsulates the 3-step BUG-010 invariant (openFolder `:416` → appendMessages `:417` → setMaxSyncedDate `:422`). It is the only implementation: `grep "processWatermarkLoop" app/src/` → 0 results (exit 1). No test-side copy exists. |
| AC-2: `BackupWorkerWatermarkTest` invokes the seam on a production `BackupWorker` | PASS | `BackupWorkerWatermarkTest.kt:87-90` builds a real `BackupWorker` via `TestableBackupWorkerFactoryWithTransport`; all 6 tests call `worker.appendBatchAndUpdateWatermark(...)` (`:113,144,173,209,241/247,292/301`). BUG-010 invariant asserted: watermark unchanged on openFolder/append/login failure, advances exactly to confirmed date on success. |
| AC-3: local `processWatermarkLoop` deleted; grep returns zero | PASS | `grep -rn "processWatermarkLoop" app/src/` → no matches (exit 1). Confirmed absent from both main and test sources. |
| AC-4: all assertions pass with same semantics; BUG-010 not weakened | PASS | `BackupWorkerWatermarkTest.kt` retains 6 tests across 3 BUG-010 categories: (a) failure→unchanged (`:108,137,166`), (b) success→exact confirmed date (`:205,231`), (c) partial→batch-1 date only (`:278`). All assert against production seam. Gate run green (669 tests pass). |
| AC-5: build green; extracted seam covered by updated test | PASS | `./gradlew :app:jacocoTestReport :app:jacocoTestCoverageVerification` → BUILD SUCCESSFUL. Seam is part of `BackupWorker` (55.8% LINE in JaCoCo XML); `service*` package = 75.6% ≥ 0.70. |

## Integration Path Verification

> Rule: Any new component with no verified call path from a production entry point is a BLOCK finding.

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `BackupWorker.appendBatchAndUpdateWatermark` | `BackupWorker.doWork()` | `doWork` → `executeBackup` → `fetchAndBackupItems` → `backupCursors` (`BackupWorker.kt:305`) → `appendBatchAndUpdateWatermark(...)` (`:346`) | yes |
| `appendBatchAndUpdateWatermark` (test reachability) | `BackupWorkerWatermarkTest` | `TestableBackupWorkerFactoryWithTransport` → real `BackupWorker` → `worker.appendBatchAndUpdateWatermark(...)` (`BackupWorkerWatermarkTest.kt:113` etc.) | yes |

No dead code: the seam is both a production call site (`:346`) and a directly-tested entry point.

## Behavioral Contract Verification

> BUG-010 invariant is the load-bearing contract; cooperative cancellation and finally-cleanup must
> survive the extraction.

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| BUG-010 watermark invariant | `setMaxSyncedDate` fed only by `appendMessages` return; throw exits before write | `BackupWorker.kt:416-422` — `confirmedMaxDate` assigned from `appendMessages` return; `setMaxSyncedDate` only reachable after both calls return | yes |
| Cooperative cancellation preserved | `ensureActive()` survives extraction | `BackupWorker.kt:331` `coroutineContext.ensureActive()` retained in `backupCursors` loop | yes |
| Finally cursor/folder cleanup preserved | `closeFolders()` in `finally` | `BackupWorker.kt:375-378` `finally { transport.closeFolders() }` retained | yes |
| Calendar sync per CALLLOG batch preserved | `calendarSyncer.syncCalendar(result)` | `BackupWorker.kt:349-351` retained after the seam call | yes |
| `MailTransportFactory` test-seam usable from updated test | fault injection via stub transport | `BackupWorkerWatermarkTest.kt:355-396` `StubMailTransport` injected via `TestableBackupWorkerFactoryWithTransport` | yes |

## Requirement Scope Coverage

> Source: assessment 20260623-post-migration-assessment#TE-003. No REQ-*/DES-* docs referenced.

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| TE-003 | Extract watermark-gating loop into named, injectable seam | yes | `BackupWorker.kt:404-424` |
| TE-003 | Test invokes production seam, not a copy | yes | `BackupWorkerWatermarkTest.kt:113` etc. |
| TE-003 | Delete local `processWatermarkLoop` | yes | grep → 0 results |
| TE-003 | BUG-010 invariant preserved (success/failure/partial) | yes | 6 tests across 3 categories, all green |

## Test Results

- `BackupWorkerWatermarkTest`: 6 `@Test` methods, all invoking the production seam, all green.
- `grep -rn "processWatermarkLoop" app/src/` → 0 results (exit 1).
- Authoritative total `@Test` count: 669 (unchanged by U-053 — tests rewritten in body, not added).
- `./gradlew :app:jacocoTestReport :app:jacocoTestCoverageVerification` → BUILD SUCCESSFUL.

## Regression Results

No regressions. The extraction is a pure refactor: `backupCursors` retains the same 3 steps (now
delegated), exception propagation, `closeFolders()` finally, `ensureActive()` cancellation, calendar
sync, and progress emission. All 669 tests pass.

## Phase Completion Report
---
story_id: "U-053"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-013/U-053/qa-results.md"
story_status: "validated"
current_build_phase: "validation"
ac_passed: 5
ac_total: 5
errors: []
---
