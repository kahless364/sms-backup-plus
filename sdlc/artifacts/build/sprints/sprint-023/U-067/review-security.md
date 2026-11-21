---
artifact_type: review-security
story_id: "U-067"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-11-21"
blockers: 0
warnings: 3
---

# Security Review: U-067

## Review Summary

Story U-067 changes three loop checks (`BackupWorker.kt:332`, `RestoreWorker.kt:311`, `:399`) from the worker's inherited, job-less `coroutineContext` to `currentCoroutineContext()`, so a cancelled run now stops at the next pass. It also widens `backupCursors` (`BackupWorker.kt:305-306`) to `internal` with `@VisibleForTesting(otherwise = PRIVATE)`. I read the commit diff and, from the commit (not the working tree), both workers, the checkpoint store, the cursor class, the scheduler call sites, the three new or changed test files and the workspace evidence. No blocking issue was found. Stopping a cancelled run earlier is the safer direction: it removes a window that the unfixed code left open (a run replaced by a new enqueue kept appending or inserting alongside its successor). The write-ordering invariants that protect the watermark and the checkpoint are untouched and cannot be split by a cancellation, because nothing between them can suspend. Three Low findings and several Info items remain; none must be fixed in this cycle.

Verified against the committed code, not the implementation log:

- The diff in `app/src/main` is 13 lines across two files: three call replacements, three import swaps, and the visibility change. No other production line moved.
- No `NonCancellable`, `withContext`, `delay` or `yield` sits between an external write and its bookkeeping (see I-1 and I-2).

## Verdict: PASS

## Findings

### Blockers

None (no Critical or High finding).

### Warnings

Classification follows `knowledge/standards/review-finding-classification.md`.

- **W-1 (Low). A thrown check is swallowed by the restore catches and reported as `illegal_state` with a "possible memory" error log.** `kotlinx.coroutines.CancellationException` is `java.util.concurrent.CancellationException`, a subclass of `IllegalStateException`. So the check at `RestoreWorker.kt:311` is caught at `:348` (`Log.e(TAG, "RestoreWorker: IllegalStateException (possible memory)", e)`, then `Result.failure(KEY_FAILURE_REASON to "illegal_state")`), the check at `:399` at `:433-434`, and in the IMAP path the outer `catch (e: IllegalStateException)` at `:272-273` is a second net. In the backup path the check passes the typed catches in `fetchAndBackupItems` and reaches `catch (e: Exception)` at `BackupWorker.kt:168`, which logs `Log.e(TAG, "BackupWorker: unrecoverable error", e)` and returns `Result.failure(KEY_FAILURE_REASON to (e.message ?: "unknown"))`. What is logged and stored: the exception's own message and stack, and the cancellation's cause if one was supplied; nothing from message bodies, addresses, credentials or tokens. A stopped worker's `Result` is discarded by WorkManager (item 4 below), so the mislabel has no scheduling effect. The cost is diagnostic: a normal user cancel is logged at error level as a memory problem. The plan names this (L15, section 7 item 4) and proposes a follow-up bug. — Classification: defer-justified — the follow-up bug for the swallowed `CancellationException` is already recorded in the plan and implementation log; no confidentiality or integrity impact.

- **W-2 (Low). A cancelled IMAP-restore run no longer refreshes conversation threads, so rows it inserted can sit outside their conversation until a later run inserts something.** `updateAllThreadsIfAnySmsRestored(ctx)` runs only after the loop completes (`RestoreWorker.kt:270`, `:331`, `:416`). Before the fix a cancelled run always reached it, because the loop never stopped. After the fix a cancelled run skips it (the `IllegalStateException` catch returns first). Data are not lost or duplicated: the rows and the checkpoint are consistent; the next run resumes at checkpoint plus one and, if it inserts anything, refreshes threads. It is a newly reachable state, not a new write. Integrity of stored data is unaffected; only the display grouping of the already inserted rows is stale. — Classification: defer-justified — display-only and self-healing on the next insert; belongs with the cancel-handling follow-up (W-1), not with this change.

- **W-3 (Low). Production stop path is unverified.** The implementation log declares one claim as UNVERIFIED under an escape hatch: that WorkManager's own stop reaches the loop through the same job. The justification names the missing infrastructure (a running WorkManager with a real executor and a real stop, which is a device or emulator run excluded by the delegation) and why no narrower probe exists (the tests call the worker directly, not through `startWork()`). I judge it adequate, not vague. The other runtime claim (the loop stops at the next pass) has an adequate in-process probe: real loops under Robolectric, cancelled from inside the fixture callback with no timing, red run failing four cases with `expected 2 but was 4` and similar, six logged mutations detected, three uncancelled controls passing. — Classification: defer-justified — disclosed, bounded, and the owner's device check (IC-4) is explicitly out of the delegation.

### Info

- **I-1. Backup: loss and duplication windows (assessment item 1).** `appendBatchAndUpdateWatermark` (`BackupWorker.kt:~405-425`) is a plain non-suspending function: `openFolder`, `appendMessages` (returns the confirmed max date), then `setMaxSyncedDate` at `:423`. A cancellation is observed only at a suspension point or at the check, so it cannot land between a confirmed append and the watermark write. The check at `:332` sits at the top of the pass, before the next batch's conversion, so a stop leaves: batches already appended, watermark equal to the last confirmed append, remaining rows above the watermark and picked up by the next run. There is no append-without-watermark window opened by this change, and no watermark-ahead-of-append window (the watermark is set only from the append's return value). The pre-existing residual (process death or an exception between append and watermark write, giving a duplicate on retry) is unchanged. After the first `setProgress` suspends in production, the loop usually stops one pass earlier (plan 2.4); either way the state is the same. `cursors?.close()` (`:280`) and `transport.closeFolders()` (`:378`) are in `finally` blocks with non-suspending bodies, so they run on cancellation (the tests assert `closeFolders` count 1 on the cancelled path). Calendar sync after a call-log batch (`:~356`) is after the watermark and is simply skipped on a stop; it is re-derived from the next batch.

- **I-2. Restore: insert versus checkpoint (assessment item 1).** `insertSmsValues` and `importCallLogValues` do `contentResolver.insert` (`:461`, `:551`), then `checkpointStore.write` (`:472`, `:556`), then `insertInterceptor.afterInsert`. Although `write` and `afterInsert` are declared `suspend`, the production implementations have no suspension point (`SharedPreferencesCheckpointStore.write` is a synchronous `commit()`; `RestoreInsertInterceptor.NoOp.afterInsert` is empty), so no cancellation can split insert from checkpoint. A stop leaves rows equal to the checkpoint (plus any dedup-skipped items, which write no checkpoint and are skipped again on resume). Resume starts at checkpoint plus one, and `smsExists` (`:~600`) and `callLogExists` dedup guard any overlap, so no row is inserted twice and none is skipped. The checkpoint is not cleared on a cancelled run (the clear is after the loop), which is the intended resume state. Compared with the unfixed code, which ran every row and cleared the checkpoint, the new state is the same kind as a crash mid-restore and is what the checkpoint design exists for. `transport.closeFolders()` (`:277`) is in a `finally`.

- **I-3. Default-SMS role and "restore in progress" flag (assessment item 1).** Neither worker touches the default-SMS-app role or any in-progress flag: those names occur only in `SmsDefaultRoleHelper.kt`, `MainActivity.java`, `Dialogs.java` and `SmsReceiver.java`, not in `service/`. A cancelled run therefore leaves the role and any such state exactly as they were; nothing in the workers must be undone on stop.

- **I-4. Concurrency window closed, not opened (assessment item 1).** Every enqueue uses `ExistingWorkPolicy.REPLACE` (`WorkManagerScheduler.kt:154, 289, 324, 365, 400`), which cancels the running work. Before the fix the cancelled run kept appending or inserting while its replacement started, so two runs could work the same rows at once (a duplicate-append hazard on the server for backup, interleaved checkpoints for restore). After the fix the replaced run stops at its next pass. This is an improvement, not a regression.

- **I-5. Mail connection, credentials, logging on the cancellation path (assessment item 2).** The connection is closed by `transport.closeFolders()` in the `finally` blocks quoted above. The catches that receive the thrown check are `BackupWorker.kt:168` (logs the exception object and stores `e.message` in the failure output data) and `RestoreWorker.kt:273`, `:349`, `:434` (log the exception object). The cancellation exception's message and stack contain job and coroutine names, not credentials, tokens, addresses or bodies. The failure output data carries the message string only. Other error-path logging is pre-existing and unchanged: the verbose lines `Log.v(TAG, "BackupWorker: backing up: $cursor")` (`BackupWorker.kt:335`; the cursor's `toString` prints the data type and the cursor object, not column values) and `RestoreWorker` uid-only lines. No message body or address is written to the log on any path I read.

- **I-6. Widened visibility (assessment item 3).** `internal` is Kotlin module visibility: the module is `:app`, the Gradle project includes only `:app`, `:metadata` and `:k9mail-vendored`, and `:app` is the application module that no other module depends on, so no other module can call `backupCursors`. `internal` members are public in bytecode with a mangled name; outside the module, only reflection or another app on the device loading the class could reach it, and an app on the device cannot load this app's classes (separate process and class loader); reflection is out of scope per the delegation. `BackupWorker` is a `@HiltWorker` that WorkManager creates; it is not a manifest component, and its input is the transport and cursors passed in by `fetchAndBackupItems`, never an intent or untrusted data. The function takes the same arguments as before and only the visibility moved. The annotation form is the right one: `@androidx.annotation.VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)` is the same form already used on `appendBatchAndUpdateWatermark` (`:~395`), declares the intended visibility as private so that lint's `VisibleForTests` check flags production callers outside the declaring class, and the project builds lint with `warningsAsErrors true` (`app/build.gradle`). The two existing production calls (`BackupWorker.kt:255` and `:463`) are inside the declaring class, so they are not flagged. `app/lint-baseline.xml` has no entry for `backupCursors`.

- **I-7. Denial of service and resource use (assessment item 4).** A stopped CoroutineWorker's returned `Result` is discarded by WorkManager: for a user or `REPLACE` cancel the work ends as cancelled, and for a system stop (constraints, quota, timeout) WorkManager reschedules by its own rules regardless of what the worker returned. So the `Result.failure` from W-1 cannot cause a retry loop, and `Result.retry()` is never returned on this path (the typed `MailException` catches are not reached by a cancellation). The backoff cap at `BackupWorker.kt:~130-140` and `RestoreWorker.kt:~125-130` (300 seconds, returning failure beyond it) is untouched. Stopping earlier shortens the time a stopped worker holds the connection and the CPU; it does not extend it. Neither worker takes its own wake lock; the foreground notification is set by `setForeground` at `BackupWorker.kt:123` and is owned by WorkManager, which removes it when the work stops.

- **I-8. Test-only material (assessment item 5).** I scanned the three test files added or changed (`BackupWorkerLoopCancellationTest.kt`, `RestoreWorkerCheckpointTest.kt`, `RestoreWorkerIntegrationTest.kt`, plus the one-line `BackupWorkerTest.kt` change) and the whole workspace evidence folder for e-mail patterns, user and profile names, host names and mail-provider names. The only address is the existing reserved-domain fixture `test@test.local` in `RestoreWorkerIntegrationTest.kt` (not added by this diff). Fixtures are `MatrixCursor` rows with `_id` and `date` columns, in-memory fake providers and a mocked converter; no real number, address, body or key. The workspace's own scan output (`evidence/I10-scan.txt`) reports zero files for the pattern, user-name and host-name scans, which matches mine.

- **I-9. Earlier open findings (assessment item 6).** None is affected. The U-065 and U-066 findings concern the TLS socket policy and the enrollment probe; this change touches neither the socket factories nor any trust or credential code. Nothing is made worse.

## Residual Risks

- A cancelled run is logged and returned as a failure with a misleading label until the follow-up bug for the swallowed `CancellationException` is done (W-1).
- A cancelled restore leaves inserted rows without a thread refresh until a later run inserts something (W-2).
- The production stop path (WorkManager's own stop reaching the loop) is not measured here; the in-process tests prove the loop honours a cancelled job, not that WorkManager cancels it (W-3).
- A duplicate append from a crash between append and watermark write, and the possibility of a changed server-side message order between runs shifting restore indexes, are pre-existing and not changed by this story.

### Security Checklist

- [x] No hardcoded credentials or secrets (diff, tests and evidence scanned)
- [x] Input validation: no new input; the visibility change adds no external entry point
- [x] Output encoding: not applicable to this change
- [x] Authentication/authorization: no credential, token or login code changed; the connection is closed in `finally`
- [x] Data exposure: nothing new is logged; cancellation logs carry exception text and stack only
- [x] Error handling (no information leakage): no new message; existing catches reused (W-1 is mislabelling, not leakage)
- [x] Dependencies: no dependency changed

### Runtime Claims Verified

- [x] `## Runtime Claims` inventory read in the implementation log (declared with two probed claims and one escape-hatch claim, not omitted)
- [x] Each declared claim and each security-relevant runtime-behaviour AC has an embedded probe with raw-output file names and stated design, or an escape-hatch justification
- [x] Probe design adequate: topology (real loops, in-process, a real coroutine job, not WorkManager), failure mode (cancel from inside the fixture callback, no timing), precondition (controls without cancel pass in every run), falsifiability (red run with the fix absent fails four cases; six mutants detected). Stated limit: not WorkManager's own stop
- [x] Escape-hatch justification names the missing infrastructure (a running WorkManager with a real stop, a device or emulator) and why no narrower probe exists (tests call the worker directly, not `startWork()`); not vague
- [x] Inventory completeness audited against the story's security-relevant runtime ACs: no gap
- [x] No probe wrote into the sibling plugin repository (the probes are unit tests and logged mutations in the app tree; no path outside the repository is named)
- [x] UNVERIFIED (escape-hatch) claims: In production, WorkManager's own stop cancels the loop through the same job (W-3)

## Phase Completion Report
---
story_id: "U-067"
phase: "security-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-023/U-067/review-security.md"
story_status: "in-review"
current_build_phase: "security-review"
blockers: 0
warnings: 3
errors: []
---
