---
artifact_type: review-code
story_id: "U-042"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-06-05"
blockers: 0
warnings: 2
---

# Code Review: U-042

## Review Summary

| Aspect | Status |
|--------|--------|
| Data-loss invariant (watermark only on confirmed append) | PASS — compiler-enforced |
| Contract compliance (CNTR-MODERNIZATION-007) | PASS with observation (see below) |
| Test coverage — AC-3 regression cases | PASS — all 6 watermark + 2 adapter tests |
| Code quality | PASS |
| Integration trace | PASS |

## Verdict: PASS

The fix correctly addresses BUG-010. The "watermark only on confirmed append" invariant is now language-enforced: `setMaxSyncedDate` is unreachable unless `appendMessages` returns normally, which it does if and only if the k-9 layer did not throw. Per-batch durability is preserved. Partial-success recovery is correct. All AC-3 regression scenarios are covered by tests that actually write to real `DataTypePreferences` (via Robolectric SharedPreferences), so the assertions on watermark state are authoritative.

## Findings

### Blockers

None.

### Warnings

**W-1 — CNTR-MODERNIZATION-007 §Interface defines `appendMessages` as `void`; U-042 changes it to `long` without a formal contract revision.**

`CNTR-MODERNIZATION-007` §"Interface: `MailTransport`" (line 109) specifies:

```
- appendMessages(folder: BackupFolderHandle, result: ConversionResult): void
```

The U-042 implementation changes this to `long`. The plan.md notes `contracts_verified: [CNTR-MODERNIZATION-007]` and correctly observes that `long` is a Java primitive satisfying the ACL no-k9-types invariant (Validation Rule 1). The fix is architecturally sound and the contract's *intent* — "no k-9 types cross the boundary" — is preserved.

However, the contract document itself still specifies `void` at line 109 and in the Example Payload at line 384 (`transport.appendMessages(folder, result);`). The contract has not been updated in-place or versioned to reflect the new `long` return type. This creates a documentation drift risk: future implementers of a second `MailTransport` (e.g. for testing or a mock) reading the contract would declare `void` and break the build.

Recommended action: update CNTR-MODERNIZATION-007 §Interface (line 109) to `appendMessages(...): long` and the Example Payload (line 384) to `long confirmedMaxDate = transport.appendMessages(folder, result);`, and bump the contract to v3 with a one-line changelog entry.

**W-2 — `processWatermarkLoop` in `BackupWorkerWatermarkTest` is a copy of the production invariant, not a direct invocation of `backupCursors`.**

The test helper (lines 294–303 of `BackupWorkerWatermarkTest.kt`) replicates the three-line gating logic verbatim rather than calling `BackupWorker.backupCursors()` directly. The implementation log correctly explains why (Hilt `@AssistedInject` + kapt generics failure). This is a reasonable pragmatic tradeoff and the test accurately captures the invariant. The risk is that if the production gating sequence in `backupCursors` is later refactored (e.g., the `openFolder` and `appendMessages` calls are wrapped in a helper), the tests continue to pass without catching the regression.

Mitigation already present in the test's Javadoc: "Changes to `BackupWorker.backupCursors` that violate this invariant would need to also update these tests." This is documented but relies on developer discipline. No code change required, but worth noting as a maintenance concern.

### Observations

**O-1 — Cancellation safety is correct but untested.**

`ensureActive()` (line 315 of `BackupWorker.kt`) fires at the top of each loop iteration — before `openFolder` and `appendMessages`. A `CancellationException` thrown by `ensureActive()` propagates unhandled (it is a `RuntimeException` subtype, not caught by any handler in `backupCursors`), so `setMaxSyncedDate` is never reached. If the coroutine is cancelled *during* a blocking `appendMessages` call, the k-9 layer does not know about Kotlin coroutines; the thread blocks until the k-9 call returns or throws a `MessagingException`. `CancellationException` is only raised at Kotlin suspension points, of which there are none inside the blocking `appendMessages` call. Result: cancellation during `appendMessages` waits for the k-9 call to complete; if the k-9 call throws, `MessagingException` is translated to `MailException` and the watermark is not advanced (correct); if the k-9 call returns normally, `confirmedMaxDate` is set and `setMaxSyncedDate` is called before the next `ensureActive()` check (also correct — the append did succeed). This is the right behavior and is consistent with AC-2 (per-batch durability). No fix required; noted for completeness.

**O-2 — Empty-result guard in `backupCursors` ensures `appendMessages` is not called with zero messages.**

Lines 321–350 of `BackupWorker.kt` guard `openFolder` + `appendMessages` behind `if (!result.isEmpty)`. So the "empty result returns -1" case tested in `K9MailTransportTest.appendMessages_emptyResult_returnsDefaultMaxDate` is a defensive test; it cannot occur in production without a logic error. The test is still good to have as a contract boundary check.

**O-3 — `TestableBackupWorkerFactoryWithTransport` inner class is production bytecode but test-only.**

The class is declared inside `BackupWorker.kt` (production source), not in a test directory. This adds test infrastructure to the production APK. The pre-existing `TestableBackupWorkerFactory` follows the same pattern, so this is consistent. The implementation log notes it was kept as "useful infra." No functional impact; minor packaging concern.

**O-4 — Adapter test uses real `K9MailTransport` with a live IMAP URI but only mocks `ImapFolder.appendMessages`.**

`K9MailTransportTest.appendMessages_success_returnsConfirmedMaxDate` constructs a real `K9MailTransport` via the public constructor (with a valid-format but non-connectable URI) and creates a `BackupFolderHandle` wrapping a Mockito `ImapFolder`. The test verifies that `folder.folder.appendMessages(messages)` is delegated to the mock and the return value is `result.getMaxDate()`. This is a correct integration test at the adapter boundary.

**O-5 — The `SmsBackupServiceTest` WorkManager initialization fix is sound and self-sufficient.**

The added `WorkManagerTestInitHelper.initializeTestWorkManager(appContext, config)` in `@Before` is idempotent (the method is documented as a no-op on re-initialization for the same context). The fix correctly addresses the test-order flakiness without side effects on other test classes that initialize WorkManager themselves.

## Detailed Analysis

### Data-Loss Invariant Verification

The BUG-010 root cause was: `BackupWorker.kt` called `setMaxSyncedDate(cursor.type, result.maxDate)` immediately after `transport.appendMessages(folder, result)` without checking whether the append actually succeeded. Since the old `appendMessages` was `void`, a silent k-9 failure (e.g., `MessagingException` swallowed inside the k-9 layer, or `folder.NONEXISTENT` returning normally) would cause the call to return normally and the watermark to advance to `result.maxDate` — a real SMS timestamp, not wall-clock time.

**The fix (verified in code):**

1. `MailTransport.java:87` — `appendMessages` now returns `long`. The Javadoc at lines 72–88 precisely documents the confirmed-append contract: "If the append fails, this method throws and never returns a date, so the caller's watermark is never updated."

2. `K9MailTransport.java:197–217` — The adapter calls `folder.folder.appendMessages(messages)` and then `return result.getMaxDate()`. The `return` is AFTER the k-9 call, inside the `try` block. If `folder.folder.appendMessages(messages)` throws any `MessagingException` subtype, control jumps to one of the four catch blocks, all of which `throw` an app-owned exception — never returning a `long`. The date comes from `result.getMaxDate()`, which is set during message conversion (from SMS `DATE` column → `Headers.DATE` header → `ConversionResult.add()` → `ConversionResult.maxDate`). Not wall-clock.

3. `BackupWorker.kt:334–345` — The call site:
   ```kotlin
   val folder: BackupFolderHandle = transport.openFolder(cursor.type, preferences.dataTypePreferences)
   val confirmedMaxDate: Long = transport.appendMessages(folder, result)
   // ... calendar sync (does not touch watermark) ...
   preferences.dataTypePreferences.setMaxSyncedDate(cursor.type, confirmedMaxDate)
   ```
   If `openFolder` throws: `confirmedMaxDate` is never declared; `setMaxSyncedDate` is unreachable.
   If `appendMessages` throws: `confirmedMaxDate` is never assigned; `setMaxSyncedDate` is unreachable.
   The exception propagates out of `backupCursors` (caught by `fetchAndBackupItems` handlers at lines 252–268). The `finally` block at line 369 calls `transport.closeFolders()` (non-throwing) and the watermark is left unchanged. **AC-1 verified.**

4. **Partial success (AC-2):** Per-batch watermark writes were already present; U-042 preserves this. After batch N succeeds, `setMaxSyncedDate` is called with the confirmed date. If batch N+1 fails, the exception propagates but batch N's watermark is already persisted (SharedPreferences `.commit()` is synchronous). The next backup sees `date > batchN_maxDate` and re-attempts batch N+1. **AC-2 verified.**

### AC-3 Regression Test Adequacy

| Scenario | Test | Observation |
|----------|------|-------------|
| AC-3(a): `openFolder` throws `MailException` | `ac3a_openFolderFails_watermarkUnchanged` | Throws before `appendMessages` is called; watermark = -1 asserted on real SharedPrefs |
| AC-3(a): `appendMessages` throws `MailException` | `ac3a_appendMessagesFails_watermarkUnchanged` | `appendMessagesBehavior` lambda throws; watermark = -1 |
| AC-3(a): `openFolder` throws `RequiresLoginException` | `ac3a_loginFails_watermarkUnchanged` | Login failure path; watermark = -1 |
| AC-3(b): successful batch → confirmed date | `ac3b_successfulBatch_watermarkAdvancesToConfirmedDate` | `confirmedDateToReturn = SMS_DATE_1`; asserts watermark = `SMS_DATE_1` exactly |
| AC-3(b): two batches → second date | `ac3b_twoSuccessfulBatches_watermarkAdvancesToSecondDate` | Counter-based stub; asserts watermark = `SMS_DATE_2` |
| AC-3(c): partial success | `ac3c_partialSuccess_watermarkEqualsBatch1MaxDate` | Batch 1 returns `SMS_DATE_1`, batch 2 throws; asserts watermark = `SMS_DATE_1` AND != `SMS_DATE_2` |

All six tests use a real `DataTypePreferences` backed by Robolectric `SharedPreferences`, so watermark assertions are not mocked — they read actual persisted state. The setup resets watermarks to -1 in `@Before`. This is a strong test design.

**Adapter-level tests (K9MailTransportTest):**
- `appendMessages_success_returnsConfirmedMaxDate`: Uses real `K9MailTransport` constructor + mocked `ImapFolder.appendMessages`; asserts return value == message DATE header value (not wall-clock).
- `appendMessages_emptyResult_returnsDefaultMaxDate`: Confirms empty result → -1 (DataType.Defaults.MAX_SYNCED_DATE).

**Missing test:** There is no test for the XOAuth2 failure path from `appendMessages` (i.e., `openFolder` succeeds but `appendMessages` throws `XOAuth2FailedException`). However, the existing `appendMessages_xOAuth2Exception_translatesTo_XOAuth2FailedException` test in `K9MailTransportTest.java` (lines 551–574) covers this exception-translation row, and the XOAuth2 case flows through the same "exception thrown → confirmedMaxDate never assigned → watermark not advanced" path as the MailException cases. Not a blocker, but a watermark-test gap for the XOAuth2 path specifically. The security review (`review-security.md`) may already have noted this.

### Integration Trace

**Entry point → new code path:**
`WorkManager` → `BackupWorker.doWork()` (line 98) → `executeBackup()` (line 137) → `fetchAndBackupItems()` (line 164) → `backupCursors()` (line 242) → `transport.appendMessages(folder, result)` (line 335) → `K9MailTransport.appendMessages()` (line 197).

This path is intact and verified by reading both the caller chain and the called code. No dead-code paths introduced.

**Registry/DI:** `K9MailTransport` is the sole `MailTransport` implementation. `BackupWorker` receives a `MailTransportFactory` via `@AssistedInject`. The `MailTransportFactory { transport }` lambda in `TestableBackupWorkerFactoryWithTransport` correctly injects the stub for tests.

**Arity/signature consistency:** The `appendMessages` signature is `long appendMessages(BackupFolderHandle, ConversionResult) throws MailException, RequiresLoginException` in the interface (line 87–88), the adapter (line 197–198), the stub (line 351), and every test call site. No arity mismatch found. No other production code calls `appendMessages` (confirmed by grep: only `BackupWorker.kt` and `K9MailTransport.java` in `src/main`).

## Patterns Verified

- [x] Follows existing code patterns (exception translation order: XOAuth2 → AuthFailed → string-match → generic backstop; per-batch watermark write; `finally { closeFolders() }`)
- [x] Error handling is appropriate (confirmed-append contract is compiler-enforced; cancellation safety correct)
- [x] Tests cover new functionality (8 new tests; all AC-3 scenarios addressed)
- [x] No hardcoded values that should be configurable (`SMS_DATE_1/2` are test-only constants)
- [x] No unnecessary complexity (3-line gating change; clean separation of confirmed date from pre-computed date)

## Integration Verified

- [x] New code is reachable from production entry points (WorkManager → doWork → backupCursors → appendMessages)
- [x] No new registries or dispatch maps required (single implementation; DI unchanged)
- [x] Function signatures match at all call sites (verified by grep — only two production call sites; both updated)
- [x] No dead code introduced (TestableBackupWorkerFactoryWithTransport is test infrastructure)
- [x] Integration path documented in implementation-log.md

## Regression Check

Not a replacement/rewrite story — no Capabilities Inventory required. The change is additive (return type broadened from `void` to `long`).

## Contract Verification

- [x] CNTR-MODERNIZATION-007 ACL invariant (no k-9 types cross the boundary) — `long` is a Java primitive; satisfied
- [ ] CNTR-MODERNIZATION-007 §Interface `appendMessages` signature — formally still specifies `void`; implementation now uses `long` (Warning W-1)
- [x] Sole k-9 importer remains `K9MailTransport.java` — verified by grep
- [x] Exception translation table exhaustive — all four k-9 exception categories handled in `appendMessages` catch blocks

## Phase Completion Report
---
story_id: "U-042"
phase: "code-review"
verdict: "PASS"
artifact_path: "C:/Code/Android/sms-backup-plus/sdlc/artifacts/build/sprints/sprint-008/U-042/review-code.md"
story_status: "in-progress"
current_build_phase: "qa"
blockers: 0
warnings: 2
errors: []
---
