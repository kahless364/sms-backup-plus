---
artifact_type: gate-result
gate: contract-verification
gate_id: "contract-verification"
checkpoint: plan-gate
story_id: "U-069"
verdict: PASS
iteration: 1
agent: "Solution Architect (separate instance)"
timestamp: "2026-11-21T12:23:26Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md"
base_commit: "4300ffa98370b77dcc4dd8d12142079aada8313a"
contracts_checked:
  - CNTR-MODERNIZATION-004 (v3; port, INV-1 to INV-4, INV-4 note, Validation Rules 1 to 8, Versioning, Breaking change policy, Backward compatibility, content-trigger example, Dependencies, Notes)
  - CNTR-MODERNIZATION-005 (BACKUP broadcast; the plan does not touch the receiver or the manifest)
---

# Gate Report: gate-result — U-069

## Gate: contract-verification @ plan-gate

## Verdict: PASS

The plan makes the content-trigger worker a consumer of the `BackupScheduler` port (`scheduleIncoming()`), deletes the second builder of `INCOMING` work, and leaves the port, the adapter, the receiver, the contract and every design text unchanged. After the change every enqueue path for `INCOMING` work is the one method `WorkManagerScheduler.scheduleIncoming()`. Each clause below is tied to a test in the plan. No blocker. Four observations, none of which changes the verdict; O-1 should be dealt with by the orchestrator before the build starts.

Reads: the plan in full (`plan.md:1-1660`, sections 0 to 12 read; the test text of 3.2 read to `:852` and the helpers), the contract in full, DES-MODERNIZATION-005 (`:255-365`, `:425-440`, `:490-497`), DES-MODERNIZATION-008 (grep of every `@HiltWorker` and worker mention, `:200-215`), the story (frontmatter, AC-8, AC-9, AC-12, AC-13, IC-1, IC-2, IC-4), BUG-023 headings through the story, the U-068 gate report and the U-068 plan sentences that bear on U-069. Source read with `git show 4300ffa9:<path>`: `BackupTriggerWorker.kt` (`:48-79`), `WorkManagerScheduler.kt` (`:56-64`, `:126-160`, `:205-262`, the `INCOMING` hits), `SchedulerModule.kt` (in full), `BackupScheduler.java` (signatures), `SmsBroadcastReceiver.java` (`:60`, `:100-101`). Scope grep at `4300ffa9`: `git grep -n "INCOMING"` over `app/src/main` finds exactly two enqueue sites of the `INCOMING` unique work, `WorkManagerScheduler.kt:154` and `BackupTriggerWorker.kt:66-71` (the latter is what the plan deletes); the other hits are the enum, preferences keys and call-log types. `git grep -n "BackupTriggerWorker"` over `app/src` and `k9mail-vendored` finds the declaration (`BackupTriggerWorker.kt:48`), the scheduler's import, class literal and comments (`WorkManagerScheduler.kt:30`, `:61`, `:63`, `:212`, `:241`), and the worker's own log line; no code constructs the worker. Scope verified via grep.

## Findings

### 1. Contract clause by clause, against the plan's end state

| # | Contract clause (file:line) | What the plan does (plan:line) | Result |
|---|-----------------------------|--------------------------------|--------|
| 1 | INV-1 single flight, REPLACE, one non-terminal item per unique name (contract `:240`) | The worker no longer enqueues; `scheduleIncoming()` (`WorkManagerScheduler.kt:152-154`, `ExistingWorkPolicy.REPLACE`) is the only `INCOMING` enqueue. Tests `ac8a`, `ac8b`, `ac8c` (plan `:627-658`): exactly one non-finished entry, the survivor's id differs from the first, the first is absent or `CANCELLED`. Mutant M-T6 (KEEP) killed by all three | Conforms |
| 2 | Single flight across the trigger and the receiver paths (contract `:245`, `:387-389`) | `ac8b` (trigger then `scheduler.scheduleIncoming()`) and `ac8c` (reverse). The receiver is `SmsBroadcastReceiver.java:60` `getScheduler(context).scheduleIncoming()`, the same port call, so the test's direct call stands for it. The real receiver is not run (see O-2) | Conforms |
| 3 | INV-2 network per type: `UNMETERED` if `isWifiOnly()`, else `CONNECTED`; charging never (contract `:241`) | `ac1` (Wi-Fi only on, `UNMETERED`), `ac2` (off, `CONNECTED`) (plan `:514-540`), and both `ac3` tests pin the value of the network type next to the equality (`:812`). Charging is not set in `scheduleIncoming()` (`WorkManagerScheduler.kt:145-150`) and not in the worker. Before the change the worker set no constraint (`BackupTriggerWorker.kt:61-64`), which is the Rule 2 breach the bug records | Conforms (breach removed) |
| 4 | INV-3 exponential 30 s, cap 300 s at the worker (contract `:242`) | `ac3` pair: `backoffPolicy == EXPONENTIAL`, `backoffDelayDuration == BACKOFF_INITIAL_SECS * 1000` (plan `:814-816`), read from the `WorkSpec`; equal to the `scheduleIncoming()` request. The 300 s cap lives in `BackupWorker`, unchanged (plan 8.1 item INV-3) and the trigger's `INCOMING` work runs that same worker | Conforms |
| 5 | INV-4 two-stage debounce: no backup on the raw change; delayed unique `INCOMING`, delay = `getIncomingTimeoutSecs()`, REPLACE (contract `:243`, Rule 5 `:277`) | W3 (plan `:385-397`): `doWork()` logs and calls `scheduler.scheduleIncoming()`, returns success; it starts no backup. `ac7` (plan `:608-621`): one entry, state `ENQUEUED`, `runAttemptCount` 0, `initialDelayMillis` 60 000. `ac3` pins 60 000 too. M-T5, M-T7 | Conforms |
| 6 | The encryption-degraded gate (U-054 SE-002, `WorkManagerScheduler.kt:135-138`; contract Rule 2 for every enqueue path) | Now on the trigger path because the trigger path runs the gated method. `ac4` (no `INCOMING` entry, success), `ac9_encryptionDegraded` (trigger line, then the scheduler's `blocked` line). M-T2, M-T8 | Conforms |
| 7 | Auto backup off (`WorkManagerScheduler.kt:138-143`; part of what Rule 2 covers by "every enqueue path") | `ac5` (stub changed after registration, no entry, success). M-T3 | Conforms |
| 8 | Delay at or below 0 declined (same gate) | `ac6_incomingDelayZero`, `ac6_incomingDelayNegative` (plan `:582-602`). M-T4 | Conforms |
| 9 | Rule 1: no Android scheduler type crosses the port | The worker takes `private val scheduler: BackupScheduler` (W2, plan `:350`), the interface, and the port is unchanged. `ic1` asserts the constructor's parameter types in order `Context, WorkerParameters, BackupScheduler, Preferences` (plan `:733-737`). `androidx.work` types appear only in the worker, which is an adapter-side class, not in the port | Conforms |
| 10 | Rule 2: INV-1 to INV-4 hold for every enqueue path, asserted by the DES-005 harness (`WorkManagerTestInitHelper`, `getWorkInfosForUniqueWork`, `WorkSpec.constraints`/`backoffPolicy`/`backoffDelayDuration`) (contract `:274`) | The new test class uses exactly that harness (plan `:480-508`, `:779-838`). Row 1 to 8 above cover each invariant on the trigger path | Conforms |
| 11 | Rule 5: the triggered worker MUST NOT back up on the raw change and MUST enqueue the delayed backup (contract `:277`) | Rows 5 and 8. When the setting declines (delay at or below 0) nothing is enqueued, as on every other path; that is the existing gate, not a Rule 5 change | Conforms |
| 12 | Rules 3, 4, 6, 7, 8 (`scheduleImmediate`, `scheduleBootup`, restore, `cancel`, `scheduleManual`) | No method of those is touched; `WorkManagerScheduler.kt` shows an empty diff (plan `:169-171`, `:895`; I6 step 4) | Not applicable |
| 13 | Single flight: the periodic `CONTENT_TRIGGER` registration (`UPDATE`, `WorkManagerScheduler.kt:249-253`; INV-1 allows REPLACE/UPDATE for periodic work) | Not touched; each test first calls `registerContentTrigger()` and asserts non-null | Conforms (unchanged) |

All of rows 3 to 8 are shown on the trigger path by a test that runs the real `WorkManagerScheduler` over the WorkManager test harness, not by a mock `verify`. The plan argues the reason (2.0) and I agree: a mock would pass with a second builder left beside the call.

### 2. The port, its signatures, and what the worker injects

| Item | Evidence | Result |
|------|----------|--------|
| Port and every signature unchanged | `BackupScheduler.java` (`:56` `ScheduledJob scheduleIncoming()` … `:142`) is in the plan's empty-diff list (plan `:895`, 3.4; story AC-13 (c)); no edit of it in any step; no change to `BackupType`, `SchedulerState`, `ScheduledJob` | Conforms |
| Worker consumes the port, not the adapter | W2 injects `BackupScheduler` (plan `:350`). The contract requires it: Consumers "depend on the port, never on a concrete mechanism" (contract `:43`), "rewired to depend on the injected `BackupScheduler` (Hilt-provided per DES-008)" (`:46`), Dependencies row DES-008 "owns the `@HiltWorker`/`HiltWorkerFactory` wiring and the `BackupScheduler` binding … states only the injection points it requires" (`:400`). DES-005 `:363` says the binding is provided by a Hilt module. Injecting `WorkManagerScheduler` would break that and would bind the worker to the debug-only `CompositeScheduler` swap that the contract describes (`:42`, `:400`) | Conforms. The interface is the right type |
| The binding exists | `SchedulerModule.kt` at `4300ffa9`: `@Binds @Singleton abstract fun bindBackupScheduler(impl: WorkManagerScheduler): BackupScheduler` in `SingletonComponent`; `WorkManagerScheduler` has an `@Inject` constructor and does not inject the worker, so no cycle (plan L16) | Verified against the source |
| The test passes the concrete adapter into the port-typed parameter | The test field is `WorkManagerScheduler` (plan `:477`) and the factory hands it to the `BackupScheduler` parameter (`:752`), as `HiltWorkerFactory` hands over the injected singleton. A test choice, not a contract matter | Conforms |

### 3. The sentences that become true with no text change

| Sentence | Location | Plan's end state | Result |
|----------|----------|------------------|--------|
| "On trigger, the worker does NOT back up directly — it calls scheduler.scheduleIncoming(), enqueuing a delayed one-off" | contract `:384-385` | W3 calls `scheduler.scheduleIncoming()`; AC-1 to AC-3 and AC-7 show the request | True after the change; not edited (plan 3.4 row 1) |
| INV-4 realization: "the triggered worker enqueues the delayed `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)` as unique work `INCOMING` (REPLACE)" | contract `:243` | Same call; `ac7`, `ac8*` | True |
| INV-4 note: the receiver's call "enqueues … under the same unique name … as the content trigger's follow-up, so INV-1 collapses the two at enqueue" | contract `:245` | Both paths use the one method (grep above); `ac8b`, `ac8c` | True |
| Rule 2 for every enqueue path | contract `:274` | Rows 3 to 10 of section 1 | True |
| Rule 5 | contract `:277` | Row 11 | True |
| "same debounce, same unique name INCOMING; INV-1 (REPLACE) collapses it with the content trigger's follow-up" | contract `:387-389` | `ac8b`, `ac8c` | True |
| DES-005 "The triggered worker enqueues the delayed incoming-backup work (preserving the `getIncomingTimeoutSecs()` debounce) — it does not back up on the raw content change" | DES-005 `:320` | W3 | True |
| DES-005 "`scheduleIncoming()` | delayed one-off, delay = `getIncomingTimeoutSecs()`, network per `isWifiOnly()`" | DES-005 `:434` | The trigger path now has that network rule | True |
| DES-005 Risks row "Content-URI debounce regresses … INV-4 test asserts the debounce delay" | DES-005 `:495` | See section 7 | True once `ac7` exists |
| DES-005 `:135` "it calls `scheduleIncoming()`" | DES-005 `:135` | Describes the legacy `SmsJobService` (`onStartJob`); true as written regardless | Not affected |
| The worker's class comment "Stage 2: this worker enqueues the delayed incoming backup via scheduleIncoming()" | `BackupTriggerWorker.kt:39` | Kept byte for byte by W2 (plan `:336`, `:354`) | True after the change |

The scheduler's own KDoc sentences at `WorkManagerScheduler.kt:61` and `:212-213` ("the triggered [BackupTriggerWorker] enqueues a delayed …") stay true: the worker enqueues through the port. One precision point: `:61` reads "enqueues a delayed [BackupWorker]" and `:212-213` reads "enqueues a delayed / incoming backup", so the plan's and the story's phrase "both say `enqueues a delayed [BackupWorker]`" is exact for `:61` only (O-4).

### 4. Contract and design edits; the example

| Check | Evidence | Result |
|-------|----------|--------|
| The plan edits no contract and no design | 3.4 (plan `:885-895`), 8.1 header ("read, not edited"), 8.4, and the 4.9 file list (3 tracked files: worker, `app/build.gradle`, the new test) with a close-time check that `git diff --name-only <start>..HEAD` lists nothing under `sdlc/artifacts/design` or `sdlc/artifacts/build/change-records` (I6 step 4, rows D29, D30). `git status --porcelain` for `sdlc/artifacts/design` and `sdlc/artifacts/requirements` is empty in this checkout | Conforms |
| The example's network constraint and backoff on the trigger registration are left to the later change record, and the plan says so | Plan 3.4 row 3 (`:891`): "Known divergence, kept … Bug ruling 2, Record 15 row 15, SC-1 ruling: the later change record's"; hand-over item 2 (`:1398-1400`); 8.4 (`:1492`). The registration code (`WorkManagerScheduler.kt:233-254`) has only the content-URI constraints, no network constraint, no backoff, `UPDATE`, and the plan touches none of it | Conforms |
| Hand-over item 2 is accurate | CR-016 is the highest record in `sdlc/artifacts/build/change-records/` (listing: CR-014, CR-015, CR-016), so CR-017 is the next free number; the example is at contract `:372-376` (network `:374`, backoff `:375`, `REPLACE` `:376`) | Accurate |

### 5. Does anything need a contract version step?

No. The plan changes no port signature, no `BackupType` value, no `SchedulerState` variant, no invariant and no Validation Rule, so under the Breaking change policy (contract `:266`) nothing is breaking for consumers. For the behavioural contract, the story brings one path into line with INV-2 and the Rule 2 gate; it loses neither the content trigger nor its debounce, which is the stated behavioural-break test. The visible change (Wi-Fi only on: the trigger path's backup now waits for an unmetered network; auto backup off: the trigger path no longer enqueues) is the contract's own rule being applied to a path that had missed it; the plan states it (2.4 items 2 and 4, R10, hand-over item 5). The contract needs no new version step, and the example's correction is already routed to a change record (section 4), not made here. Result: Conforms.

### 6. Interaction with story U-068 (runs first)

| What U-069 relies on | U-068 plan (iteration 2) | Result |
|----------------------|--------------------------|--------|
| The two scheduler sentences stay byte-identical (U-069 L20; AC-13 (b)) | U-068 hand-over item 6 (`U-068/plan.md:1434-1437`) and L18 (`:134`): E5.13 ends at old `:60`, E5.14 at old `:210`; old `:61` and `:212-213` are kept; the edits reword `:58-60`, `:208-210`, `:256` only | Consistent. U-069's `:60` and `:211-212` are the old `:61` and `:212` after the 1-import and 7-line removal: the simulated post-U-068 scheduler is 481 lines, 489 minus 8 (plan L20), and KDoc rewording is line-neutral, so the arithmetic holds |
| The scheduler test `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` stays unchanged | U-068 hand-over item 6 says it "must stay unchanged in U-069 and S4"; the U-069 plan does not edit `WorkManagerSchedulerTest.kt` (3.4, 2.3, AC-13 (e)) | Consistent |
| Where the U-068 branch deletion lands relative to the new test | The new test calls `registerContentTrigger()` at SDK 29 and asserts non-null. With the branch present (before U-068 I5) or deleted, SDK 29 is at or above 24, so it holds either way; U-069 starts after U-068 closes (D2) | Consistent |
| `app/build.gradle` Rule 15 stays at `:575-588` (`:579` note, `:586` floor) | U-068 edits `:24`, `:153`, `:160` in place (plan L20) | Consistent; U-069's `oldtext_check.py start` and row D4 catch any drift |
| The worker is not edited by U-068 | U-068 empty-diff list includes `BackupTriggerWorker.kt` (U-068 gate report finding 4) | Consistent |
| The pre-24 branch and its old test name occur 0 times at U-069's start | Plan L19 and I0 step 2 check it; row D4 stops if one is present | Consistent |

### 7. DES-MODERNIZATION-005 Risks row: "INV-4 test asserts the debounce delay"

The row (`:495`): "Content-URI debounce regresses (backs up on every SMS) | Reproduce the two-stage trigger → delayed-follow-up; INV-4 test asserts the debounce delay."

`ac7_debounce_incomingWorkWaitsEnqueuedForTheIncomingDelay` meets it: it runs the trigger worker, then asserts one `INCOMING` entry, state `ENQUEUED` (the backup has not run on the raw change), `runAttemptCount == 0`, and `initialDelayMillis == 60 000` for `incomingTimeoutSecs` 60. The `ac3` pair asserts the same 60 000 against the equal-pair check. Both assertions are on the request the trigger worker leaves, which is the "delayed follow-up" the row asks for. The clock is not advanced, so the test shows the delay length by the request's field, not by elapsed time; that is what "asserts the debounce delay" can mean in this harness, and the story and plan say so. Weakness for the record: the test's delay (60) equals the `Defaults.INCOMING_TIMEOUT_SECONDS` value (plan L15), so a hard-coded 60 in `scheduleIncoming()` would pass `ac7` and `ac3`; `ac9_allowedState` sets 75 but asserts only the log line. This is not a defect of this plan (the scheduler's own `ac5_scheduleIncoming_withTimeout_isEnqueued` is the existing test of the delay), but see O-3. Result: Meets the row.

### 8. Hand-over items that name contracts and designs

| Item | Claim | Check | Result |
|------|-------|-------|--------|
| 2 | Change record for CNTR-004 `:372-376`, number CR-017 if free | Highest record CR-016; lines match | Accurate |
| 7 | DES-MODERNIZATION-008 `:113` lists `BackupWorker` and `RestoreWorker` as the `@HiltWorker` workers; `BackupTriggerWorker` joins them | `DES-MODERNIZATION-008-hilt-di-design.md:113` is the table row "`@HiltWorker` (assisted, per-work) | `BackupWorker`, `RestoreWorker`" | Accurate; incomplete, see O-3 |
| 7 | `sprint-017/U-060/contract-verification.md:49` is historical | Not re-read; no gate depends on it | Not verified here |
| 8.1 | Contract line numbers (`:240-243`, `:245`, `:266-267`, `:273`, `:274`, `:277`, `:384-385`, `:372-376`) | All match the contract text | Accurate |
| 3.4 | DES-005 `:133-138`, `:135`, `:320`, `:434`, `:495` | Each read; `:135` is the legacy `SmsJobService` note inside the `:128-138` block; `:320` the trigger bullet; `:434` the `scheduleIncoming()` row; `:495` the Risks row | Accurate |

## Blockers

None.

## Observations

**O-1 (Info; action before the build). `integration_contracts: []` in the story frontmatter.** The story's worker becomes a consumer of the `BackupScheduler` port, so it crosses a component boundary and the contract gate wants `integration_contracts` populated. The story cites CNTR-MODERNIZATION-004 in `related_items` (`:33`), in its Source, Technical Context and AC text, and the contract is `approved` and was read here in full, so the reference exists and the gate's substance (approved contract, no deviation) is met. The plan raises this as information (8.1 last paragraph). I treat it as frontmatter hygiene, not a blocker. The orchestrator should add `CNTR-MODERNIZATION-004` to `integration_contracts` in the story before the build, so the lifecycle record matches the text. If the orchestrator prefers to leave it, record the ruling in the story.

**O-2 (Info). The receiver path is stood in for by a direct call.** `ac8b` and `ac8c` call `scheduler.scheduleIncoming()`, not `SmsBroadcastReceiver`. That is faithful for INV-1 (`SmsBroadcastReceiver.java:60` makes exactly that call), and the receiver stays unchanged and covered by `SmsBroadcastReceiverTest`. No action.

**O-3 (Info; for the orchestrator's design note, no edit now). The Hilt worker lists are wider than hand-over item 7 says.** Besides DES-MODERNIZATION-008 `:113`, the same two-worker statement appears at DES-MODERNIZATION-008 `:204-206` ("`@HiltWorker` for the CoroutineWorkers"), at DES-MODERNIZATION-005 `:261-262` (component table) and `:357` ("`BackupWorker` and `RestoreWorker` are annotated `@HiltWorker`"). The trigger worker is a plain `Worker`, not a `CoroutineWorker`, which those texts do not cover. None is false (they list the two workers they name and make no "only" claim), so nothing is violated; a later amendment should list all three workers and name the trigger worker's type. Separately, the delay in `ac7` equals the default (section 7); a later test could set 75 and assert `initialDelayMillis` 75 000 on the trigger path. The plan's `ac9_allowedState` already uses 75 for the log; extending that one test to assert the delay would close the gap at no cost. Neither is required.

**O-4 (Info). Imprecise wording in the plan and the story about the scheduler KDoc.** Plan 2.1 and L20 describe both `:60` and `:211-212` as "enqueues a delayed [BackupWorker]"; at `4300ffa9` the first (`:61`) says that and the second (`:212-213`) says "enqueues a delayed incoming backup". Both stay true and both are untouched, so the plan's reliance holds. Likewise plan 3.4 row 3 and the story AC-12 say the registration has "no constraint": it has the content-URI trigger constraints and no network constraint and no backoff. The later change record should word it as "no network constraint, no backoff". And of the example's three differences, `REPLACE` for the registration is not a divergence under INV-1 (periodic work may use REPLACE or UPDATE), so the change record need only correct the network constraint and the backoff.

## Phase Completion Report
---
story_id: "U-069"
phase: "plan-gate/contract-verification"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-069/contract-verification.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
