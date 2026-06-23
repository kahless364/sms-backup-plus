---
artifact_type: qa-results
story_id: "U-050"
verdict: PASS
agent: "QA Analyst"
timestamp: "2026-06-23"
ac_total: 5
ac_passed: 4
ac_failed: 1
tests_run: 642
tests_passed: 642
---

# QA Validation: U-050

## Verdict: FAIL

Four of five ACs pass with strong evidence: the worker engine object graph is injected via
`WorkerEngineFactory` (no hand-`new` in the production `doWork` path), the BUG-010 watermark
invariant and cooperative-cancellation/cursor-cleanup are preserved verbatim, `MainActivity`
SMS-role logic is decomposed into `SmsDefaultRoleHelper` (U-034 dialog theming + BUG-009 Q+
round-trip preserved), and the `WorkManagerScheduler.observe()` stub comment and the
"second adapter alongside LegacyScheduler" class KDoc are removed.

AC-2 FAILS on one explicit, verifiable clause: the story requires "the `TODO U-022` tag is
gone," but `MainActivity.java:127` still carries `// TODO U-022/MU-007: replace manual factory
with @HiltViewModel.` — a now-stale marker (the manual factory `MainViewModelFactory` is in
fact deleted and the Hilt `@HiltViewModel` path is in use, so the comment is misleading dead
documentation). The implementation log claimed AC-2 was satisfied and cited a wrong line
(`158-159`); the actual obtain-site is line 152 and the TODO at 127 was never removed. Per
the partial-clause-coverage rule, AC-2 is FAIL, so the story verdict is FAIL.

This is a one-line documentation fix (delete the stale `TODO U-022/MU-007` comment at
`MainActivity.java:127`); the functional substance of AC-2 is otherwise met.

## Acceptance Criteria Results

> Every PASS cites a file:line reference. ACs with an unmet verifiable clause are FAIL.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1 | PASS | `MainActivity.startRestore/requestDefaultSmsPackageChange/restoreDefaultSmsProvider/checkDefaultSmsApp` now delegate to `SmsDefaultRoleHelper` (`MainActivity.java:418-426`; helper `SmsDefaultRoleHelper.kt:73,110,133,152`). SMS-role negotiation block (a distinct concern) extracted to a focused collaborator following the `*FlowHelper` pattern. |
| AC-2 | **FAIL** | `MainViewModelFactory` removed: `ls` → "No such file or directory" (only comment refs remain). Hilt `@HiltViewModel` path in use: `MainViewModel.kt:51` `@HiltViewModel`, `MainActivity` `@AndroidEntryPoint` (`:105`), obtained via Hilt default factory `new ViewModelProvider(this).get(MainViewModel.class)` (`MainActivity.java:152`) with no manual factory. **BUT the AC clause "the `TODO U-022` tag is gone" is NOT met**: `MainActivity.java:127` still contains `// TODO U-022/MU-007: replace manual factory with @HiltViewModel.` (stale — the factory is already gone). Verifiable clause unmet → FAIL. |
| AC-3 | PASS | `BackupWorker.kt:240-243` uses `engineFactory.createPersonLookup()`, `createMessageConverter(personLookup, contactAccessor)`, `createCalendarSyncerIfEnabled(personLookup)`, `createTokenRefresher()`; `contactAccessor` is an injected `@AssistedInject` field (`BackupWorker.kt:110`, `ContactAccessor.java:38` `@Inject` ctor). No `new MessageConverter/PersonLookup/TokenRefresher/CalendarSyncer/ContactAccessor` in the production run path. The two `ContactAccessor()` hits at `BackupWorker.kt:547,582` are inside the test-only `TestableBackupWorkerFactory(WithTransport)` seams, not production. `CalendarSyncer` factory-built with documented rationale (`WorkerEngineFactory.kt:104-118`: runtime `Long calendarId` + legacy static `CalendarAccessor.Get.instance`). |
| AC-4 | PASS | `WorkManagerScheduler.observe()` "until U-020 lands" comment removed; KDoc now states the method is retained for API contract with no production caller (`WorkManagerScheduler.kt:383-394`). Class KDoc no longer says "second adapter alongside LegacyScheduler" — now "sole production BackupScheduler implementation (U-017)" (`WorkManagerScheduler.kt:64-65`). (AC scopes LegacyScheduler-KDoc removal to WorkManagerScheduler specifically; residual LegacyScheduler refs in BackupScheduler.java/SchedulerObservable.java/SchedulerState.java are historical contract documentation, explicitly retained in the log — out of AC-4's stated scope. The `App.java:224` Configuration.Provider branch was DEFERRED with rationale in the log; the original "reflective-factory fallback" is already gone, leaving only a null-guard for the test path.) |
| AC-5 | PASS (build-green verified by user) | Integrated tree: `assembleDebug` + `testDebugUnitTest` (642) + jacoco all PASS. `MainActivityRestoreTest` BUG-009 tests pass (`:75,175`). U-034 themed-dialog path preserved via `DialogDelegate.showSmsDefaultPackageChangeDialog()` → `showDialog(SMS_DEFAULT_PACKAGE_CHANGE)` (`MainActivity.java:422`). `WorkerEngineFactoryTest.kt` (4 tests) added. |

## Integration Path Verification

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `WorkerEngineFactory` (@Singleton) | WorkManager run | `@AssistedInject` ctor of `BackupWorker` (`:112`) / `RestoreWorker` (`:134`) → `HiltWorkerFactory` (registered `App.getWorkManagerConfiguration():162`) → factory methods called in `doWork` (`BackupWorker.kt:240-243`) | yes |
| `SmsDefaultRoleHelper` | MainActivity user actions | `MainActivity.startRestore():421` and 3 other delegations → helper static methods | yes |
| Injected `ContactAccessor` | worker run | `@AssistedInject` field → used in `BackupWorker`/`RestoreWorker` engine build | yes |

## Behavioral Contract Verification

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| BUG-010 watermark | Watermark advances ONLY to `confirmedMaxDate` returned by `appendMessages`, after confirmed append; exception before `setMaxSyncedDate` leaves it unchanged; per-batch durable checkpoint | `BackupWorker.kt:344-356` (open→append→setMaxSyncedDate(confirmedMaxDate)); KDoc `:289-297,339-343` | yes |
| Cooperative cancellation (U-050 preserve) | `ensureActive()` retained | `BackupWorker.kt:325` `coroutineContext.ensureActive()` | yes |
| Cursor cleanup (U-050 preserve) | `finally { cursors?.close() }` retained | `BackupWorker.kt:276-277` | yes |
| BUG-009/U-041 Q+ round-trip | Q+ always requests role regardless of legacy default value | `SmsDefaultRoleHelper.kt:77-83,111-117` | yes |
| U-034 themed dialog | SMS_DEFAULT_PACKAGE_CHANGE dialog uses AppCompat themed path via delegate | `MainActivity.java:422` → `showDialog(...)` | yes |
| `@HiltViewModel` injection unchanged | state/events collection semantics preserved | `MainViewModel.kt:61-64` (state/events delegate to repository) | yes |

## Capabilities Inventory Verification

| Capability | Status in Log | Verified in Code | File:Line | Result |
|------------|---------------|------------------|-----------|--------|
| PersonLookup per-run | RETAINED (factory) | yes | `WorkerEngineFactory.kt:70-71` | PASS |
| MessageConverter per-run userEmail | RETAINED (factory) | yes | `WorkerEngineFactory.kt:80-87` | PASS |
| TokenRefresher per-run clientId | RETAINED (factory) | yes | `WorkerEngineFactory.kt:93-98` | PASS |
| ContactAccessor | RETAINED (direct inject) | yes | `BackupWorker.kt:110`; `ContactAccessor.java:38` | PASS |
| CalendarSyncer | RETAINED (hand-built, documented) | yes | `WorkerEngineFactory.kt:118-126` | PASS |
| SMS-role negotiation | RETAINED (extracted to helper) | yes | `SmsDefaultRoleHelper.kt:73-157` | PASS |
| MainViewModelFactory | INTENTIONALLY REMOVED | N/A | file deleted (ls confirms); Hilt `@HiltViewModel` path | Valid justification: YES |

## Requirement Scope Coverage

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| assessment AR-003 | Decompose MainActivity into collaborators | yes | `SmsDefaultRoleHelper.kt` (4 methods extracted) |
| assessment AR-003 | Replace MainViewModelFactory with Hilt ViewModel obtain | partial — factory removed + Hilt path used, but stale `TODO U-022` not removed (AC-2 clause) | `MainActivity.java:127` (stale TODO) |
| assessment AR-004 | Inject worker engine object graph | yes | `WorkerEngineFactory.kt`; `BackupWorker.kt:240-243` |
| assessment AR-005 | Remove stale `observe()` stub + LegacyScheduler KDoc in WorkManagerScheduler | yes | `WorkManagerScheduler.kt:64-65,383-394` |
| assessment AR-005 | Clean App.java Configuration.Provider test-env branch | DEFERRED (tracked in log "Deferred" section; reflective-factory already gone, null-guard retained) | `App.java:232-244` |

## Test Results

Authoritative `@Test` count = 642 (confirmed; +4 from `WorkerEngineFactoryTest.kt`). `MainActivityRestoreTest`, `BackupWorkerWatermarkTest`, `RestoreWorkerCheckpointTest` pass per the integrated build.

## Regression Results

BUG-010 watermark, ensureActive cancellation, cursor cleanup, BUG-009 Q+ round-trip, and U-034 dialog theming all preserved. No functional regression detected. The sole defect is the stale `TODO U-022/MU-007` documentation marker at `MainActivity.java:127`, which AC-2 explicitly requires removed.

## Issues Found

1. **AC-2 clause unmet (FAIL driver)** — `MainActivity.java:127` retains `// TODO U-022/MU-007: replace manual factory with @HiltViewModel.`. AC-2 requires "the `TODO U-022` tag is gone." The marker is now stale/misleading (the manual factory is deleted; the Hilt path is live). Remediation: delete the comment at line 127. One-line fix.
2. **Minor (non-failing) note** — stale `TODO U-022/MU-007` markers also remain at `StatusPreference.java:78` and `FlowCollectHelper.kt:17`. These are outside AC-2's MainActivity scope but are the same class of stale seam AR-005 targets; recommend cleaning in the same fix for consistency.

## Phase Completion Report
---
story_id: "U-050"
phase: "validation"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-012/U-050/qa-results.md"
story_status: "validation-failed"
current_build_phase: "validation"
ac_passed: 4
ac_total: 5
errors:
  - "AC-2: stale 'TODO U-022/MU-007' comment remains at MainActivity.java:127; AC requires the TODO U-022 tag be removed. One-line documentation fix."
---

---
## Remediation verified (post-review)
AC-2's sole failing clause — the stale `TODO U-022/MU-007` markers — are removed (`MainActivity.java:127`, `StatusPreference.java:78`, `FlowCollectHelper.kt:17`; repo-wide `TODO U-022` count now 0). The orphaned double KDoc on `WorkManagerScheduler` was also collapsed. Build green (643 tests). **Verdict updated FAIL → PASS.**
