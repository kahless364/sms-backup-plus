---
type: story
status: planned
sprint: "000001"
artifact_type: user-story
priority: high
complexity: high
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-005
design_docs:
  - DES-MODERNIZATION-005
integration_contracts:
  - CNTR-MODERNIZATION-004
dependencies:
  - U-015
change_records: []
platforms:
  - android
tags:
  - workmanager
  - restore
  - durability
  - fault-tolerance
gate_additions: []
id: U-016
title: Durable Restore Checkpoint — Resumable Restore Worker with Fault-Injection Verification
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-016: Durable Restore Checkpoint — Resumable Restore Worker with Fault-Injection Verification

> One-line note: promotes `RestoreConfig.currentRestoredItem` to a durable store written after each successful provider insert and before cursor advance, enabling `RestoreWorker` to resume from the next uncommitted item after process kill, with fault-injection test seeding N messages, crashing after K inserts before checkpoint advance, and asserting final row count == N with zero duplicates.

## Story

As a user who initiates a large SMS or call-log restore on a device where the system may kill the app mid-operation,
I want the restore worker to automatically resume from where it left off after being restarted rather than starting over from the beginning,
so that I do not end up with duplicate messages in my inbox and the full restore completes correctly regardless of mid-operation process death.

## Acceptance Criteria

1. **Resumable after process kill.** Given a `RestoreWorker` `OneTimeWorkRequest` is in a non-terminal state when the process is killed, when WorkManager re-executes the worker, then the worker reads the persisted checkpoint value (the `currentRestoredItem` cursor index stored in the durable store after the last successful provider insert) and begins the restore loop at `currentRestoredItem + 1`, skipping all previously committed items, and proceeds to complete the full restore to a `SUCCEEDED` terminal state.

2. **Checkpoint written at the correct point in the write-commit sequence.** Given the restore loop calls `resolver.insert` for item at index `i`, when the insert returns a non-null `Uri` confirming a committed row, then the durable store is updated with `currentRestoredItem = i` before the loop variable is advanced to `i + 1`; no checkpoint write occurs for items that are skipped by the existing `smsExists` / `callLogExists` dedup guards (i.e., skipped items do not advance the checkpoint).

3. **Zero duplicate messages across the crash window.** Given a process kill occurs in the interval after `resolver.insert` for item `i` succeeds but before the checkpoint write persists (the non-zero crash window), when the worker re-executes and processes item `i` again, then the existing `smsExists` guard (`RestoreTask.java:259,308–327`, keyed on `date + address + type`) and `callLogExists` guard (`RestoreTask.java:280,288–306`, keyed on `date + number + duration + type`) prevent a duplicate row from being inserted, and the final row count in the SMS and call-log providers equals exactly the number of unique messages in the restore set.

4. **Fault-injection test: N messages, crash after K, assert count == N, no duplicates.** Given an instrumented test that:
   (a) seeds the IMAP restore source with exactly N distinct SMS messages (N >= 20);
   (b) injects a fault (test-controlled exception or cooperative crash signal) after exactly K successful `resolver.insert` calls (1 <= K < N) but before the checkpoint for item K is written to the durable store;
   (c) simulates process death by re-initializing the test `WorkManager` instance (`WorkManagerTestInitHelper`);
   (d) allows the re-executed `RestoreWorker` to run to completion;
   then the SMS provider contains exactly N rows (verified via `ContentResolver.query`), no item appears twice (verified by querying on `date + address + type` uniqueness), and the worker's terminal `WorkInfo.State` is `SUCCEEDED`.

5. **Worker does not restart from index zero after a checkpoint-carrying resume.** Given the durable store holds a checkpoint value of `K` (meaning items 0 through K were previously committed), when the worker re-executes, then the restore loop initializes `currentRestoredItem = K + 1` (read from the durable store, not from `RestoreConfig.currentRestoredItem` which starts at 0), confirming the checkpoint store — not the in-memory config — is the authoritative resume cursor.

6. **Checkpoint is cleared on terminal SUCCEEDED.** Given the restore worker completes all items and transitions to `SUCCEEDED`, then the durable checkpoint entry for this restore's unique work name is removed or reset to 0, so that a fresh invocation of `scheduleRestore(config)` for the same work name starts from item 0 and does not skip any messages.

7. **No checkpoint regression on CANCELLED.** Given a `cancel(RESTORE)` structured-cancellation is issued against the running `RestoreWorker`, when the worker unwinds at the next suspension point and the work reaches `CANCELLED` state, then the durable checkpoint retains the last written value so that a subsequent re-schedule can resume from the same position if desired; the checkpoint is not cleared on cancellation.

### Integration Criteria

- [ ] IC-1: The durable checkpoint store (e.g., `RestoreCheckpointStore`) is declared as a port in the core module with no Android `SharedPreferences`, `DataStore`, or `WorkManager` type in its signature; the production adapter in the app module uses `WorkManager`'s `Data` output or `androidx.datastore` under the hood.
- [ ] IC-2: `RestoreWorker.doWork()` calls `checkpointStore.read(uniqueWorkName)` before the restore loop begins and calls `checkpointStore.write(uniqueWorkName, i)` inside the loop immediately after a confirmed `resolver.insert` and before advancing the loop index, matching the ordering specified in AC-2.
- [ ] IC-3: `RestoreWorker` is annotated `@HiltWorker` with an `@AssistedInject` constructor; the `RestoreCheckpointStore` port is an injected collaborator supplied by Hilt (per DES-MODERNIZATION-005 §Hilt worker construction and DES-MODERNIZATION-008).
- [ ] IC-4: The fault-injection seam (the mechanism that triggers the simulated crash after K inserts) is a test-only collaborator injected via the same Hilt test module used for the invariant tests, not a production-visible flag or static.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java` | `AsyncTask`; `currentRestoredItem` is the in-memory loop counter at line 100, advanced at line 119; no persistence across process death | Replaced by `RestoreWorker` (`CoroutineWorker`); checkpoint is read from durable store on entry, written to durable store after each confirmed insert, cleared on `SUCCEEDED` |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreConfig.java` | Holds `currentRestoredItem` (line 11) as the in-process resume cursor; `retryWithStore(currentItem, ...)` (line 31) passes it for OAuth retry within a single process lifetime | `RestoreConfig` continues to carry restore parameters; its `currentRestoredItem` is superseded at worker entry by the durable store's value so the worker resumes correctly after external restarts (OAuth retry path within a single execution continues to use the in-memory value) |
| `app/src/main/java/com/zegoggles/smssync/service/` (new file) `RestoreWorker.kt` | Does not exist | New `@HiltWorker CoroutineWorker`; owns the durable-checkpoint read/write loop; calls the core restore use-case extracted by U-015; emits progress via `setProgress` |
| `core/` (new file) `RestoreCheckpointStore.kt` (port) | Does not exist | Interface: `suspend fun read(workName: String): Int` and `suspend fun write(workName: String, index: Int)` and `suspend fun clear(workName: String)`; no Android types in signature |
| `app/src/main/java/.../` (new file) restore checkpoint store adapter | Does not exist | Production impl of `RestoreCheckpointStore`; persistence mechanism (DataStore or WorkManager `setProgressAsync` + output `Data`) decided at implementation; injected by Hilt |
| `app/src/test/.../RestoreWorkerTest.kt` (new file) | Does not exist | Instrumented test implementing AC-4 fault-injection scenario: seeds N messages, crashes after K inserts before checkpoint write, re-executes worker, asserts row count == N and no duplicates |

## Existing Behavior to Preserve

- `smsExists` gate (`RestoreTask.java:259,308–327`): insert is only performed when `!smsExists(values)`, where `smsExists` queries `date + address + type`. This guard MUST remain active and unmodified in the `RestoreWorker` path — it is the last-line duplicate defense for the crash window.
- `callLogExists` gate (`RestoreTask.java:280,288–306`): insert is only performed when `!callLogExists(values)`, where `callLogExists` queries `date + number + duration + type`. Same preservation requirement.
- SMS type filter (`RestoreTask.java:256–259`): only `MESSAGE_TYPE_INBOX` and `MESSAGE_TYPE_SENT` messages are inserted. This filter must be preserved in the `RestoreWorker` path.
- `clearCache()` periodic call (every 50 items, `RestoreTask.java:124–126`): kept in the `RestoreWorker` loop to prevent SD card overflow.
- Thread update after SMS restore (`RestoreTask.java:129`): `updateAllThreadsIfAnySmsRestored()` is called after the loop; must be called from `RestoreWorker` in the same position.
- OAuth retry via `retryWithStore(currentRestoredItem, ...)` (`RestoreConfig.java:31`, used at `RestoreTask.java:165`): within a single worker execution the in-memory `currentRestoredItem` is used for OAuth retries exactly as today; this retry is an intra-execution concern and is unaffected by the durable checkpoint.
- `isCancelled()` loop guard: replaced by cooperative coroutine cancellation (`isActive` or `ensureActive()` at the top of the loop body); structured cancellation must unwind the loop at the next suspension point.

## Verification Steps

1. **AC-1 — Resume after process kill:** Using `WorkManagerTestInitHelper`, enqueue a `RestoreWorker` for a 10-message restore seeded in a fake IMAP store; allow the worker to commit 5 rows; call `WorkManagerTestInitHelper.getTestDriver().setAllConstraintsMet(false)` to simulate process death; re-initialize the test WorkManager; allow the re-queued worker to run; assert via `ContentResolver.query(Consts.SMS_PROVIDER)` that exactly 10 rows are present and `WorkInfo.State` is `SUCCEEDED`.

2. **AC-2 — Checkpoint write ordering:** Using a test-double `RestoreCheckpointStore` that records the order of `write()` and `clear()` calls relative to `resolver.insert()` invocations, run a 3-message restore and assert that each `write(name, i)` call is recorded after the corresponding `insert` call for item `i` and before the loop variable increments to `i + 1` (observable via call ordering in the test double's call log).

3. **AC-3 — Zero duplicates across crash window:** Seed 5 messages; inject fault to kill after insert of item 2 but before `write(name, 2)`; restart the worker; assert `ContentResolver.query(Consts.SMS_PROVIDER).count == 5`; assert no row shares `date + address + type` with any other row (verified by querying each unique combination).

4. **AC-4 — Fault-injection test (canonical):** Run the full instrumented test described in AC-4: N = 20, K = 7. Assert final SMS provider row count == 20 with no `date + address + type` duplicates and `WorkInfo.State == SUCCEEDED`. This test must be included in the CI gate defined by Gate G2 (DES-MODERNIZATION-003) and must be green before the WorkManager binding flip (per REQ-MODERNIZATION-005 constraints).

5. **AC-5 — Resume from checkpoint, not from config:** Seed 10 messages; write checkpoint value 5 to the durable store under the restore's unique work name before the worker executes; run the worker; assert via test-double `RestoreCheckpointStore` that `read(uniqueWorkName)` was called exactly once on entry, returning 5, and that the first insert attempted is for item index 6, not item 0.

6. **AC-6 — Checkpoint cleared on SUCCEEDED:** Run a full 5-message restore to `SUCCEEDED`; assert `checkpointStore.read(uniqueWorkName)` returns 0 (or the sentinel "not set" value) after the worker reaches terminal state.

7. **AC-7 — Checkpoint retained on CANCEL:** Seed 10 messages; run the worker; issue structured cancellation after 4 commits; assert the checkpoint store still holds value 3 (last confirmed commit index) after the worker reaches `CANCELLED`.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (app module) | `RestoreWorker` implementation, checkpoint store adapter, `@HiltWorker` wiring, fault-injection test | Developer |
| Android (core module) | `RestoreCheckpointStore` port declaration, restore use-case hook for checkpoint injection (handed off from U-015) | Developer |

## Technical Notes

**Checkpoint write ordering is the load-bearing invariant.** The crash window between `resolver.insert` completing and the checkpoint write persisting is non-zero. The correct sequence inside the loop body is: (1) call `smsExists` / `callLogExists` guard; (2) if guard passes, call `resolver.insert`; (3) if insert returns non-null `Uri`, call `checkpointStore.write(uniqueWorkName, i)` as a `suspend` call (awaited, not fire-and-forget); (4) advance `i`. Steps 3 and 4 must not be reordered. The existing dedup guards cover the crash window so there is no duplicate even if step 3 does not complete before the crash — but the checkpoint must be written before the index advances to avoid re-fetching messages the worker has already moved past.

**Durable store technology choice.** `WorkManager` `setProgressAsync` + `Data` is observable for UI but is not truly durable across process death in all WorkManager versions; `androidx.datastore` (or `SharedPreferences` with synchronous commits as a fallback) behind the `RestoreCheckpointStore` port is the preferred production backing. The port design isolates this choice from the worker and from the core use-case. The adapter owns the persistence; the port surface is three suspending functions: `read`, `write`, `clear`.

**`currentRestoredItem` supersession on re-execution.** On every `doWork()` entry the worker calls `checkpointStore.read(uniqueWorkName)` and uses that value as the initial `currentRestoredItem`, overriding the `RestoreConfig` field. The `RestoreConfig` field retains its existing role for the intra-execution OAuth retry path (`retryWithStore`, `RestoreConfig.java:31`) — that path operates within a single execution where the in-memory cursor is authoritative. The durable store is authoritative only across executions.

**Fault-injection seam.** The test fault is injected via a test-only implementation of a `RestoreInsertInterceptor` (or equivalent suspending hook) that is a constructor parameter of the core restore use-case, supplied from the Hilt test module. The hook is a no-op in the production binding and throws (or suspends indefinitely then cancels) after exactly K calls in the test binding. No static field, no build-type flag.

**`smsExists` and `callLogExists` must remain the active dedup guards.** The checkpoint does not replace these guards; it reduces the number of items that need dedup re-checking on resume. Both must fire unconditionally for every item in the restored-but-not-yet-checkpointed window. Removing or weakening either guard to "optimize" the checkpoint path is a correctness defect.

**Dependency on U-015.** This story requires the core restore use-case extracted by U-015 (the `RestoreWorker` is a thin adapter over that use-case). The checkpoint-write hook must be an injectable collaborator inside the core restore loop, not added to `RestoreTask` itself. This story cannot begin until U-015 is complete and the core restore use-case exposes the post-insert hook point.

**WorkManager re-execution guarantee.** A `OneTimeWorkRequest` that has not reached a terminal state (`SUCCEEDED`, `FAILED`, or `CANCELLED`) will be re-executed by WorkManager after process death or reboot. No manual restart machinery is needed in the worker. The checkpoint is the only mechanism required to make the re-execution semantically correct (resumption vs. restart).

## Estimation

**High** — the checkpoint store port + adapter (new component), the write-ordering discipline inside the restore loop, the Hilt wiring of the fault-injection seam, and the full instrumented fault-injection test (AC-4) each carry meaningful complexity. The correctness of the checkpoint-vs-cursor ordering must be code-reviewed separately from functional behavior. Integration with the core restore use-case from U-015 adds a hard sequencing dependency.

## Supporting Documentation

- REQ-MODERNIZATION-005 §Acceptance Criteria AC-6 — "durable restore checkpoint is active post-cutover (resumable after process kill; no duplicate messages)"
- DES-MODERNIZATION-005 §System Architecture — "Durable restore checkpoint via WorkManager state" — checkpoint ordering, re-execution guarantee, and dedup guard verification
- DES-MODERNIZATION-005 §Design Validation — "Resumable restore, no duplicates (AC-6)" — canonical fault-injection test spec (N messages, K-row crash before checkpoint advance)
- DES-MODERNIZATION-005 §Integration Design — "Constraint + backoff configuration" — `RestoreSchedulerConfig.checkpointKey` semantics
- CNTR-MODERNIZATION-004 §RestoreSchedulerConfig — `checkpointKey` field; §Validation Rules rule 6 — "Restore is resumable and idempotent"
- CNTR-MODERNIZATION-004 §Error Handling — "Process death is not an error"
- Source: `RestoreTask.java:97–155` (restore loop and cursor), `RestoreTask.java:259,280,288–327` (dedup guards), `RestoreConfig.java:11,31` (`currentRestoredItem`, `retryWithStore`)

## Integration Contract References

- CNTR-MODERNIZATION-004 §RestoreSchedulerConfig — `checkpointKey` field is the durable cursor key this story promotes to a persistent store
- CNTR-MODERNIZATION-004 §Validation Rules rule 6 — binding clause: "scheduleRestore MUST persist the checkpoint after each successful insert and before cursor advance; the preserved smsExists/callLogExists write-seam guards enforce no-duplicate-rows"
- CNTR-MODERNIZATION-004 §Error Handling — "Process death is not an error; restore resumes from the durable checkpoint"

## Notes
