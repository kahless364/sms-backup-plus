---
artifact_type: gate-result
gate: contract-verification
gate_id: "contract-verification"
checkpoint: plan-gate
story_id: "U-067"
verdict: PASS
iteration: 1
agent: "Solution Architect (separate instance)"
timestamp: "2026-11-21T10:04:45Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md"
base_commit: "4095db286d52dc4a361abc03de078edfce857b9c"
contracts_checked:
  - CNTR-MODERNIZATION-004 (v3; rule 7, Error Handling, Validation Rule 6)
  - CNTR-MODERNIZATION-006 (producer clause for BackupWorker and RestoreWorker)
  - CNTR-MODERNIZATION-007 (clause C-2 and the closeFolders-in-finally example)
  - CNTR-MODERNIZATION-001, -002, -003, -005, -008 (searched; no clause binds either worker)
---

# Gate Report: gate-result — U-067

## Gate: contract-verification @ plan-gate

## Verdict: PASS

## Summary

The plan edits no contract and needs no version step. It makes the workers conform more closely to CNTR-MODERNIZATION-004 rule 7 (`:279`), and it breaks no clause. The story's "observable behaviour when a check throws" table and the plan's assertions agree with every contract clause that bears on cancellation: the restore checkpoint, the backup watermark, the `finally` cleanup and the `Result` type (which no contract fixes). No clause in the contracts or the designs states what a cancelled worker returns. The plan therefore asserts nothing about it, and that is the right reading. The `internal` widening of `backupCursors` touches no port or public type. No constructor or WorkManager/Hilt-visible signature changes. There are no blockers and four observations, all Low or Info.

Scope verified via grep.
- `BackupWorker|RestoreWorker|cancel|watermark|checkpoint` over `sdlc/artifacts/design/contracts` hits only CNTR-004, -006 and -007 for the workers. CNTR-001, -002, -003, -005 and -008 name neither worker.
- In the worker sources, the check lines are `BackupWorker.kt:331`, `RestoreWorker.kt:311` and `:399`.
- `backupCursors(` has exactly two production callers, `BackupWorker.kt:255` and `:462`.
- `SyncStateRepository|SyncEvent|tryEmit` has 0 hits in either worker, so the workers publish no state event. Nothing the fix changes touches CNTR-006's event surface.

## Findings

### 1. Clause-by-clause check

| # | Contract clause (file:line) | What the plan does (plan:line / source:line) | Result |
|---|-----------------------------|----------------------------------------------|--------|
| 1 | CNTR-004 Validation Rule 7 (`:279`): "`cancel` is structured. It MUST cooperatively cancel the running coroutine (unwinding at the next suspension point) … not merely poll an `isCancelled()` flag" | The checks become `currentCoroutineContext().ensureActive()` (plan 2.1, E3, E5, E6). Today they bind to the worker's dispatcher member and never throw (`BackupWorker.kt:331`, `RestoreWorker.kt:311`, `:399`). Polling `isStopped` is rejected (plan 2.1). The plan's test "stops at the next pass" is the latest-bound form of the rule: the pass in flight is blocking IMAP work, so no suspension point lies inside it | Conforms. Rule 7 becomes true by the mechanism it names rather than by the `setProgress` route only. See O-3 on the wording "next suspension point" |
| 2 | CNTR-004 `cancel(jobKind)` (`:98-100`): "structured-cancels the running coroutine and clears any scheduled retry" | The scheduler adapter and `cancelUniqueWork` are untouched (IC-2: `StatusPreference.java`, `WorkManagerCancelCollector.kt`, `MainViewModel.kt` listed as unchanged, plan I8 `ic2-end`). Only the worker side of the cancel gets real | Conforms; not affected on the port side |
| 3 | CNTR-004 Error Handling, "Cancellation is terminal, not failure" (`:287`): a structured-cancelled job emits `SchedulerState.Cancelled`, distinct from `Failed` | The thrown check ends in the existing broad catches, which return `Result.failure` (`BackupWorker.kt:168-170`; `RestoreWorker.kt:348-350`, `:433-435`). The plan does not change the catches and does not assert the `Result` type (plan 2.2, "Not asserted"). Plan section 8 says the clause concerns the state consumers observe and that WorkManager keeps the work CANCELLED | Not violated by this change. The same path is taken today when a `setProgress` cancellation throws, so the fix adds no new class of outcome. See O-1 on the limit of what the plan can say about the clause |
| 4 | CNTR-004 Error Handling, worker failure (`:284`): non-retryable failures return `Result.failure()` and surface as `Failed` | Same as row 3. No `Result.retry` or `Result.failure` rule is changed (AC-8 (d); E1 to E6 touch only the call expression and imports) | Not applicable (unchanged) |
| 5 | CNTR-004 Validation Rule 6 (`:278`): persist the checkpoint after each successful insert and before the cursor advances; the dedup guards enforce no duplicate rows | The check sits at the top of the pass, before `insertSmsValues` / `importMessage` (`RestoreWorker.kt:310-313`, `:398-402`). The write order is insert, checkpoint, interceptor (`:461-475`) and is not edited. A stop therefore falls between passes: either a pass is complete with its checkpoint written, or it has not begun. The plan's AC-2 (checkpoint 1) and AC-3 (checkpoint 2, not cleared) assert exactly that | Conforms. The checkpoint is not cleared on a stopped run: `checkpointStore.clear` is at `RestoreWorker.kt:340` and `:425`, after the loops, and is skipped when the check throws |
| 6 | CNTR-004 `scheduleRestore` semantics (`:209-215`): resumable from a durable checkpoint | On resume the loops read the checkpoint and continue at `checkpoint + 1` (`RestoreWorker.kt:300-306`, `:388-394`). A cancelled run leaves the checkpoint at the last confirmed insert, so the resume point is correct | Conforms (strengthened: the stop now happens one pass earlier, never mid-pass) |
| 7 | CNTR-004 `BackupType` / `inferBackupType` and `executeSkip` citations (`:87`, `:131`, `:149`, `:280`: `BackupWorker.kt:161-162`, `:430-436`) | Citations only; no behaviour. E2 adds one line at `:305`, so lines below it shift by one. These citations are already stale at the base commit: `executeSkip` is called at `:187-188` and `inferBackupType` is at `:500` | Not applicable. Already-stale references, not made false by this plan. See O-2 |
| 8 | CNTR-006 producer row (`:44`) and `Cancel(origin)` events (`:180-191`, `:298-300`) | Neither worker publishes a sync event (0 grep hits). The `SyncEvent.Cancel` consumer is not a worker | Not applicable |
| 9 | CNTR-007 Example (`:374-394`): `finally { transport.closeFolders() }` keeps its semantics; clause C-2 (`:298-300`, `:325`): `convertMessages` throws `MailException` and the worker already catches it | `closeFolders()` stays in the `finally` of `backupCursors` (`BackupWorker.kt:375-378`) and of `executeRestore` (`RestoreWorker.kt:275-278`). The plan asserts `closeFolders` exactly once on a stopped run (tests 1 and 3). No catch is edited. The example's `!isCancelled()` is a Java pre-migration illustration, not a clause on the worker | Conforms. The `:374` example is illustrative text and does not bind `BackupWorker` |
| 10 | CNTR-004 `BackupScheduler`, `SchedulerConfig`, `BackupType`, `SchedulerState` surfaces (`:54-232`) and INV-1 to INV-4 (`:238-243`) | No scheduler, config or state type is touched. The plan lists only `BackupWorker.kt`, `RestoreWorker.kt` and tests in the diff (plan I8 `git diff --name-only`) | Not applicable (unchanged) |

### 2. The story's "observable behaviour" table against the contracts

| Story table row | Contract bearing | Agreement |
|-----------------|------------------|-----------|
| Backup: no further convert, append, calendar sync, `setProgress`, `STATE_FINISHED`; watermark not advanced for an unappended message; pass in flight completes; `closeFolders` once, `cursors.close()` once | CNTR-004 rule 7 and CNTR-007 `:366` (`finally` semantics). The source agrees: the watermark moves only in `appendBatchAndUpdateWatermark` after a confirmed append (`BackupWorker.kt:403-424`); `closeFolders` at `:377`; `cursors?.close()` at `:280` | Agrees. The plan's declared narrowing (cursor close not observable through the seam) is a test-reach limit, not a contract contradiction |
| Backup: the `CancellationException` passes the catches at `:265-278` and ends in `doWork`'s `catch (e: Exception)` (`:168`), returning `Result.failure` | `:284` and `:287` leave this open. Neither clause fixes the `Result` of a cancelled run | No contradiction. Source verified: none of `XOAuth2FailedException`, `RequiresLoginException`, `MailException`, `SecurityException` is a `CancellationException` |
| Restore IMAP loop: rows and checkpoint kept; no `updateAllThreadsIfAnySmsRestored` (`:331`), no `clear` (`:340`); `closeFolders` once (`:275-278`); `IllegalStateException` catch at `:348` returns `Result.failure("illegal_state")` before the outer catch at `:272` | Rule 6 (checkpoint) and `:287`. Source verified: the inner catch at `:348` returns, so the outer `:272` is not reached | Agrees |
| Restore test-facing loop: as above, no `closeFolders` | Rule 6 | Agrees. Source verified: `executeRestoreWithValues` has no `finally` (`:396-436`) |

No assertion in the plan's tests (plan 3.2) contradicts a clause. The tests assert effects (appends, rows, checkpoint, watermark, `closeFolders`, cursor position) and, for the controls only, `Result.Success`. They assert no `Result` type for a cancelled run, so a later story that rethrows `CancellationException` (the follow-up in plan section 7 item 4) does not have to edit them, and nothing the contracts may later say about it is pre-empted.

### 3. Does any contract need an edit or a version step?

No.
- No clause is changed or contradicted, so CNTR-004 stays v3 and `approved`. Its changelog needs no entry.
- The only references the plan makes to CNTR-004 are read-only (`:279`, `:287`), and both line numbers are correct in the current file.
- The plan's own file list for the contract area is empty: I8 `records-end` checks `git status --porcelain -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records` for nothing (plan I8 step 4).
- The stale `BackupWorker.kt:161-162` / `:430-436` / `:310` citations (row 7) were stale before this story and are not made newly false by it. No version step is needed for them. They are a candidate for the next contract amendment, not for this story.

### 4. The visibility change on `backupCursors` (check 3)

- `backupCursors` is a member of `BackupWorker` (`BackupWorker.kt:305`), not a port. The ports are `BackupScheduler`, `MailTransport` and the others named in the contracts. No contract lists `BackupWorker`'s members as a surface.
- `internal` plus `@VisibleForTesting(otherwise = PRIVATE)` is module-scoped. The same pattern is already applied to `appendBatchAndUpdateWatermark` in the same class (`BackupWorker.kt:403-404`), so the shape is established.
- The two callers, `:255` and `:462`, are inside the class. The plan corrects the story's single-caller wording (plan L4; section 7 item 2). Scope verified by grep: two production callers, no other `backupCursors(` hit in `app/src`.
- No public type is added or changed. The `internal` function mentions `BackupCursors`, `BackupConfig`, `Preferences`, `MessageConverter` and `CalendarSyncer` as parameter types, all of which are existing types; no new contract type is introduced. The plan flags the one compile risk (a package-private Java type, `CalendarSyncer`, in an `internal` signature) as an open check with a stop row (D3, plan L12). That is a build risk, not a contract risk.

Result: touches no contract surface.

### 5. WorkManager and Hilt signatures (check 4; IC-1)

- The `@HiltWorker` / `@AssistedInject` constructor of `BackupWorker` is at `BackupWorker.kt:105-106` and is not in any edit (E1 `:57`, E2 `:305`, E3 `:331`). The `RestoreWorker` constructor is not in E4 to E6 (`:66`, `:311`, `:399`).
- `doWork` and the factory classes are not edited. Hilt reads the constructor, not `backupCursors`, so a visibility change on a member method cannot reach the generated worker factory. Kotlin name mangling of an `internal` member (`backupCursors$app_…`) is not seen by WorkManager, which calls `doWork`. The precedent in the same class is `appendBatchAndUpdateWatermark`.
- The import swap (E1, E4) replaces `kotlin.coroutines.coroutineContext` by `kotlinx.coroutines.currentCoroutineContext` in place, and keeps `kotlinx.coroutines.ensureActive` (`BackupWorker.kt:54`, `RestoreWorker.kt:63`).

Result: no signature seen by WorkManager or Hilt changes. IC-1 holds.

### 6. What the contracts say a cancelled run returns (check 5)

- **CNTR-004** states two things about cancellation as it reaches consumers: rule 7 (`:279`), that `cancel` structured-cancels the running coroutine and clears any retry; and `:287`, that a structured-cancelled job emits `SchedulerState.Cancelled`, distinct from `Failed`. It also states (`:284`) that a non-retryable failure returns `Result.failure()` and surfaces as `Failed`.
- **No contract or design clause says what `Result` the worker itself returns when its coroutine is cancelled.** DES-MODERNIZATION-005 ADR-005-B (`:216-218`) says cancellation cooperatively cancels the coroutine, which unwinds the suspending loop at the next suspension point. DES-MODERNIZATION-012 (`:430-447`) says the worker's `ensureActive()` observes the interrupt and requires a `WorkInfo` CANCELLED test. DES-MODERNIZATION-014 F-1 (`:1104-1138`) states the bound and the stop rule, and also says nothing about the returned `Result`.
- The plan's hand-over item (plan section 7 item 4, section 8) says the cancelled run ends in the broad catches and returns `Result.failure`, which is the path a `setProgress` cancellation takes today, and that the follow-up bug (rethrow `CancellationException` ahead of each broad catch) is separate. That is consistent with the contracts: it states the code's present behaviour, and it proposes the change that would bring the worker in line with `:287`'s intent. It does not claim the contract is met by the return value.
- Source confirms the described return: `BackupWorker.kt:168-170` (`Result.failure(KEY_FAILURE_REASON)`), `RestoreWorker.kt:348-350` and `:433-435` (`Result.failure("illegal_state")`), with `RestoreWorker.kt:198-201` as the outer catch for the IMAP path.

## Observations

**O-1 (Low; for the follow-up bug, not a defect of this plan). The contract's "terminal, not failure" clause holds in practice only if WorkManager discards the returned `Result` of an already-cancelled run.** Plan section 8 rests this on the bug's reading ("`onStopped()` cancels the result future first"), taken from compiled library classes. I did not re-read the library, and the contract does not state it. The clause is about `SchedulerState`, so this plan does not contradict it. But the hand-over item should cite `:284` and `:287` by line when the follow-up bug is raised, so that its acceptance criteria can be written against the clause, and it should record that the discard is a library behaviour not a contract guarantee. Until then the visible residue is the error-level log line the story names.

**O-2 (Info). Stale line citations in contracts, shifted by one further line after E2.** CNTR-004 cites `BackupWorker.kt:161-162` and `:430-436` (`:87`, `:131`, `:149`, `:280`); CNTR-007 cites `BackupWorker.kt:138`, `:310` and `:34`. At the base commit `executeSkip` is called at `:187-188` and `inferBackupType` is at `:500`. The plan's `internal` annotation line moves everything below `:305` down by one. No contract text becomes false, because the references were already off. Suggest the next contract amendment re-cite by name, not by line. The plan, which does not edit contracts, is right to leave them.

**O-3 (Info). Wording of rule 7.** Rule 7 says "unwinding at the next suspension point". After the fix the loops also stop at the top of the next pass, which is a bound at least as early as the next suspension point that matters here, since a pass is blocking IMAP work. In production `setProgress` may still throw first, so the two never conflict. The plan's tests assert the pass-boundary stop under a harness where `setProgress` cannot cancel (plan L6). That demonstrates the stricter form and does not contradict the rule. No contract change is needed.

**O-4 (Info). `integration_contracts: []` is correct.** The story crosses no component boundary: the seam is `internal` within the app module, and no contract names `backupCursors`. The story's Integration Contract References section already lists CNTR-004 as read-only. The contract gate does not block on this.

## Blockers

None.

## Phase Completion Report
---
story_id: "U-067"
phase: "plan-gate/contract-verification"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-023/U-067/contract-verification.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
