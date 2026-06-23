---
artifact_type: review-code
story_id: "U-053"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-06-23"
blockers: 0
warnings: 1
---

# Code Review: U-053

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS — BUG-010 invariant preserved verbatim; seam is the single implementation |
| Test coverage | PASS — 6 BUG-010 regression tests now exercise production code, not a copy |
| Code quality | PASS — minimal, focused refactor; no runtime behavior change |

## Verdict: PASS

Story U-053 extracts the three-step confirmed-append / watermark-gating block from `BackupWorker.backupCursors` into a named `internal` method `appendBatchAndUpdateWatermark` (BackupWorker.kt:404-424), and rewrites `BackupWorkerWatermarkTest` to invoke that production seam directly on a real `BackupWorker` instance. The local `processWatermarkLoop` copy is cleanly deleted; `grep -r "processWatermarkLoop" app/src/` returns zero source hits (the only matches are stale `.claude/worktrees/` and `app/build/` artifacts from prior agent runs, which are not source files). All three BUG-010 invariant cases — failure/unchanged, success/confirmed-date, partial/batch-1-date — are correctly asserted against the production code path. No runtime behavior change is detectable: `backupCursors` calls `appendBatchAndUpdateWatermark` at exactly the same point (line 346) with the same arguments that were previously inline; cooperative cancellation (`ensureActive()` at line 331), `calendarSyncer.syncCalendar` (line 349), progress emission (lines 361-366), and `transport.closeFolders()` in the `finally` block (line 377) are all preserved and unchanged in position relative to the extracted call.

## Findings

### Blockers

None.

### Warnings

- **W-1 — `appendBatchAndUpdateWatermark` is not `suspend` but is called from a `suspend` context:** `appendBatchAndUpdateWatermark` is a plain (non-suspend) `internal fun` (`BackupWorker.kt:403-424`). It is invoked from `backupCursors`, which is `suspend`. This is correct for what the method does (it performs blocking synchronous IMAP calls via the `MailTransport` port), but it means any future attempt to add `ensureActive()` or `withContext(...)` inside `appendBatchAndUpdateWatermark` itself would require a signature change. The current design is intentional (the blocking I/O contract lives in `K9MailTransport`) but is not documented on the method. Consider a one-line KDoc note: `// Note: intentionally not suspend; MailTransport port methods are synchronous blocking calls.` This is a maintainability note, not a correctness defect.

- **W-2 — `StubMailTransport` is defined in the test file at package scope (not inside a companion object or separate test-utilities file):** `StubMailTransport` (BackupWorkerWatermarkTest.kt:355-396) is a top-level class in the same file as the test class. This is workable for a small stub but makes the class visible to the entire `service` package in test scope. If additional test files in `service` create their own transport stubs, there is a future risk of name collision. This is low severity given the class is test-only and the package is not large, but moving it to a shared test utilities file would be cleaner. Not a correctness or build issue.

### Observations

- **Seam is the single production implementation — no duplication (AC-1 verified):** The three lines (`openFolder`, `appendMessages`, `setMaxSyncedDate`) appear exactly once in source at BackupWorker.kt:416-422; `backupCursors` at line 346 delegates unconditionally. The diff confirms the 6 inline lines were fully replaced, not supplemented.

- **Integration trace complete (AC-2 verified):** `doWork()` → `executeBackup()` → `fetchAndBackupItems()` → `backupCursors()` → `appendBatchAndUpdateWatermark()`. Each link is verified in the production source. The auth-retry path in `handleAuthError` also calls `backupCursors` (line 462), so the seam is reachable from that path as well.

- **`processWatermarkLoop` deleted from working tree source (AC-3 verified):** `grep -r "processWatermarkLoop" app/src/` returns zero results. The `.claude/worktrees/` and `app/build/` hits are pre-existing stale artifacts from earlier agent invocations and compiled stubs respectively — they are not source files and do not affect the build or test outcomes.

- **BUG-010 three-case coverage (AC-4 verified):**
  - Failure: `ac3a_openFolderFails_watermarkUnchanged`, `ac3a_appendMessagesFails_watermarkUnchanged`, `ac3a_loginFails_watermarkUnchanged` — all catch-and-assert patterns confirm watermark stays at `DataType.Defaults.MAX_SYNCED_DATE` (-1) when the seam throws.
  - Success: `ac3b_successfulBatch_watermarkAdvancesToConfirmedDate` asserts `getMaxSyncedDate(SMS) == SMS_DATE_1` exactly; `ac3b_twoSuccessfulBatches_watermarkAdvancesToSecondDate` confirms cumulative per-batch writes.
  - Partial: `ac3c_partialSuccess_watermarkEqualsBatch1MaxDate` asserts `== SMS_DATE_1` and `!= SMS_DATE_2`, directly modeling the retry-from-checkpoint behavior.

- **`@VisibleForTesting(otherwise = PRIVATE)` annotation is correct:** The annotation documents intent (would be `private` if tests did not need direct access) and is consistent with the Kotlin `internal` visibility used to grant same-module access without reflection.

- **`setUp()` watermark reset is correct:** All three data types (SMS, MMS, CALLLOG) are reset to -1 (`DataType.Defaults.MAX_SYNCED_DATE`) before each test. All BUG-010 assertions operate on `DataType.SMS` only, so the MMS epoch-scaling quirk in `DataTypePreferences.getMaxSyncedDate` (which applies `* 1000L` when `maxSynced > 0`) is not in play for any test assertion. The reset is correct and complete.

- **`StubMailTransport.appendMessages` fallback chain is sound:** When neither `appendMessagesBehaviorFn` nor `confirmedDateToReturn` is set, it falls back to `result?.getMaxDate() ?: MAX_SYNCED_DATE` — which mirrors what a correct `K9MailTransport.appendMessages` implementation would return. This means the "success" path in the stub is behaviorally correct by default.

- **No impact on `BackupWorkerIntegrationTest`:** `BackupWorkerIntegrationTest` already calls `worker.appendBatchAndUpdateWatermark(...)` directly (lines 235, 258, 309) — these tests are unaffected and continue to confirm reachability of the seam from integration-constructed workers.

## Patterns Verified

- [x] Follows existing code patterns — `internal` + `@VisibleForTesting(otherwise = PRIVATE)` matches prior seam convention in this codebase (e.g., `TestableBackupWorkerFactoryWithTransport`)
- [x] Error handling is appropriate — exceptions from `openFolder` and `appendMessages` propagate naturally; `setMaxSyncedDate` is unreachable unless both return normally
- [x] Tests cover new functionality — 6 BUG-010 tests now exercise production seam; 3 additional tests in `BackupWorkerIntegrationTest` confirm reachability
- [x] No hardcoded values that should be configurable — SMS_DATE_1 and SMS_DATE_2 are test-only epoch constants with clear documentation
- [x] No unnecessary complexity — extraction is minimal (3 lines → named method); no new classes, no new interfaces

## Integration Verified

- [x] New code is reachable from production entry points — traced: `doWork()` → `executeBackup()` → `fetchAndBackupItems()` → `backupCursors()` (line 346) → `appendBatchAndUpdateWatermark()`
- [x] Registries/dispatch maps updated — N/A; no new registry entries needed
- [x] Function signatures match at all call sites — single call site at BackupWorker.kt:346; arguments `(transport, cursor.type, result, preferences.dataTypePreferences)` match method signature exactly
- [x] No dead code introduced — seam is called from `backupCursors` (production) and directly from both test classes (test)
- [x] Integration path documented in implementation-log.md — yes, documented at "Integration Path" section

## Regression Check

- [x] No files deleted or replaced (this is an extraction-and-update, not a rewrite); Capabilities Inventory not required
- [x] BUG-010 invariant preserved — the exact same three-step sequence (`openFolder`, `appendMessages`, `setMaxSyncedDate`) is in production code; only the scope boundary changed
- [x] Cooperative cancellation (`ensureActive()` at BackupWorker.kt:331) — preserved; still in `backupCursors` loop before the `appendBatchAndUpdateWatermark` call
- [x] `finally { transport.closeFolders() }` at BackupWorker.kt:376 — preserved and covers the extracted call
- [x] `calendarSyncer.syncCalendar(result)` at line 349 — preserved immediately after the `appendBatchAndUpdateWatermark` call

## Contract Verification

- [x] `MailTransport.appendMessages` return contract (confirmed max-date, not wall-clock) — unchanged; `appendBatchAndUpdateWatermark` feeds the return value directly to `setMaxSyncedDate`
- [x] `DataTypePreferences.setMaxSyncedDate` call site — unchanged semantics; called with `(type, confirmedMaxDate)` as before
- [x] No external API surface changed — `appendBatchAndUpdateWatermark` is `internal`, not part of any public or cross-module API
