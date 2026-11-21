---
artifact_type: gate-result
gate: contract-verification
gate_id: "contract-verification"
checkpoint: plan-gate
story_id: "U-075"
verdict: FAIL
iteration: 1
agent: "Solution Architect (separate instance)"
timestamp: "2026-11-21T17:33:35Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-024/U-075/plan.md"
base_commit: "acc50c94c1c635685164518731ab1b217dca76f2"
contracts_checked:
  - CNTR-MODERNIZATION-004 (HEAD v3 and working-tree v4 under CR-017; INV-1 to INV-4, Rules 1 to 8, Error Handling, SchedulerState)
  - CNTR-MODERNIZATION-005 (BACKUP broadcast; failure-surfacing sentence)
  - CNTR-MODERNIZATION-006 (sync-state repository; producer and progress rules)
  - CNTR-MODERNIZATION-001, -002, -003, -007, -008 (searched; no statement on foreground, notification or worker results)
designs_checked:
  - DES-MODERNIZATION-001, -005, -007, -008, -012, -013, -014
requirements_checked:
  - REQ-MODERNIZATION-001, -005, -013, -015, -016, -017; EPIC-MODERNIZATION-001
---

# Gate Report: contract-verification — U-075 (plan iteration 1)

Verdict: FAIL

The plan's code design changes no contract invariant, rule or interface, and the planner has nothing to fix for this
gate. The FAIL comes from one blocker for the orchestrator. The story claims that no contract or design text says
automatic backups run in the foreground. That claim is not true: one cell of an approved design (DES-MODERNIZATION-014,
row 16-1) says so, and it becomes false when the plan is built. CR-017's search (section E-3) missed the cell, so the
story inherited the gap. A one-cell text edit fixes it. The plan needs no change for B-1.

## How this was checked

- Plan: read sections 0 to 3.3, 8, 10 to 13 and the Verdict in full; other sections were searched (IC-1 greps, D23).
  Story and BUG-029 "Interactions" read in full.
- Contracts, designs and requirements were searched at `HEAD` (`acc50c94`; `git diff --stat 3770b9a4 acc50c94` is empty
  for `app`, `sdlc/artifacts/design`, `sdlc/artifacts/requirements` and the change records) and in the working tree. The
  search used `git grep -n -i` with `foreground|setForeground|ForegroundInfo|notification|dataSync|FOREGROUND_SERVICE|POST_NOTIFICATIONS`.
  I then read each hit in context.
- Note on scope: the brief lists CNTR-001 to -006, but `sdlc/artifacts/design/contracts/` holds eight contracts (-007 mail
  transport, -008 contacts port). All eight were searched.
- Code: `git show fd70325a:<path>` and the post-U-070 blobs `ba667c43…` (`BackupWorker.kt`) and `dc84c4f5…`
  (`RestoreWorker.kt`), copied into the session scratch folder. I read no working-tree file under `app/`.
- **HEAD and working tree compared.** The working-tree edits (CR-017; `git diff HEAD` on CNTR-004, DES-005, DES-008,
  DES-014) do not touch any foreground, notification or worker-result sentence. They only move line numbers:
  - CNTR-004 Rule 8: `:280` at HEAD, `:320` in the working tree.
  - Rule 7: `:279` → `:321`.
  - Error Handling: `:284` → `:326` and `:287` → `:329`.
  - DES-005: `:54` → `:55` and `:129-131` → `:130-132`.
  - DES-008: `:186` → `:187`.
  - DES-014: `+7` in the region cited.

  The text of every sentence in the table below is the same in both, so the difference does not matter for this story.
  The one consequence for the process is O-6.

## Blockers

### B-1 (orchestrator): an approved design states that scheduled long work runs as a `dataSync` foreground service

- **Where:** `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md`,
  Android 16 behaviour-change table, row 16-1 (HEAD `:1275`, working tree `:1282`). Its "Applies? Code evidence" cell
  reads: "WorkManager runs on JobScheduler; long work runs as a dataSync foreground service". Its disposition cell reads:
  "A-NC: AC-11 forced run; REQ-016 AC-13 scheduled run".
- **What:** after the plan's change (E-B1, plan 3.1), no automatic backup (`INCOMING`, `REGULAR`, `BROADCAST_INTENT`, or
  untagged) asks for the foreground. Those are exactly the long, JobScheduler-run backups that row 16-1 is about. Only
  `MANUAL`, `SKIP` and restores ask, and a refused request runs without the service. The cell then describes behaviour
  the app no longer has. It is also the premise of the row's quota assessment: from this story on, automatic backups
  meet the job quota as ordinary jobs (the BUG-025 subject).
- **Why it blocks:** the story's Integration Contract References and CR-017 E-3 (working tree `:252-253`: "No document
  states that automatic backups run in the foreground or show a notification, so U-075 makes no contract or design text
  false") both rest on a search that did not cover this cell. Under this gate's rule, a statement in an approved design
  that becomes false needs a change record before the build. The story cannot fix it itself: its AC-8 forbids any change
  under `sdlc/artifacts/design` and `sdlc/artifacts/build/change-records`.
- **Exact fix (proposed).** Replace the cell text with:
  "WorkManager runs on JobScheduler. Automatic backups (incoming, regular, after a restart, BROADCAST_INTENT) run as
  ordinary jobs and are subject to the job quota. Only a backup or restore the user starts asks for a dataSync foreground
  service, and a refused start continues as an ordinary job (U-075, BUG-029)."
  - Leave the disposition cell as it is: the REQ-016 AC-13 scheduled run is still the right check, and now more so.
  - The cheapest vehicle is CR-017 Amendment 1, which is still being written and already carries DES-014 edits E-5 and E-6.
    Add it as E-7, and add one row to E-3's table that records the cell and its correction. The record is insert-only,
    so add the row in the amendment rather than editing `:252`.
  - Alternatively, a separate small non-breaking record could carry it together with BUG-029's optional DES-005
    clarification (story H-2).
  - **Land it before U-075's Start commit** so that U-075's AC-8 diff of `sdlc/artifacts/design` stays empty (see O-6).
- **Owner:** orchestrator. The planner has nothing to change. Plan 8.1 only claims CNTR-004, DES-005 and the REQs it
  names, and those claims are correct. The story's sentence "Any contract or design edit (Integration Contract
  References below)" can be corrected at close-out to point at the record.

## Statement table (item 1)

"After" means after the plan's change: automatic types never ask for the foreground; `MANUAL`, `SKIP` and every
restore ask through `setForegroundOrContinue`, which continues when the request is refused. Line numbers are HEAD /
working tree where they differ.

| # | File:line | Statement (short) | After the change |
|---|-----------|-------------------|------------------|
| 1 | CNTR-004 `:280` / `:320` (Rule 8) | collapsing MANUAL/SKIP onto BROADCAST_INTENT would lose "the manual notification/foreground semantics" | **True**: MANUAL/SKIP keep the request (AC-4); the type still reaches the worker by tag |
| 2 | CNTR-004 `:284` / `:326` | a failed `doWork()` returns `Result.retry()`; non-retryable failures return `Result.failure()` and surface as `Failed` | **True**, and met better: before, a refusal escaped `doWork` as an exception (neither result); now the run returns its own result |
| 3 | CNTR-004 `:285` / `:327` | "No silent swallowing across the port" | **True**: the refusal is not a failure of the run. The run's real result still crosses the port, and the refusal is logged once at warning |
| 4 | CNTR-004 `:279` / `:321` (Rule 7) | `cancel` is structured: cooperative cancellation, retry cleared | **True**: the helper rethrows `CancellationException` first (3.3 line 48); a cancel during `setForeground` ends the run as cancelled (AC-5 (d)) |
| 5 | CNTR-004 `:287` / `:329` | cancellation is terminal, not failure | **True** (unchanged; U-074's subject) |
| 6 | CNTR-004 SchedulerState, `Running(progress)` "surfaced via WorkManager setProgress" (`:259` WT) | progress through setProgress | **True**: `setProgress` is independent of the foreground updater (plan L9); a refused run still publishes |
| 7 | CNTR-005 `:213` | a failure of the enqueued broadcast backup "is surfaced through the app's own notification/state channels" | **True through the state channel.** After the change a BROADCAST_INTENT run shows no progress notification; its errors still reach the status screen (`StatusPreference.java:234` at `fd70325a` suppresses background types except errors). Since U-049 the foreground notification was an ongoing progress notification, not a failure notification; no `notify(` call exists in `app/src/main` at `fd70325a`. See O-3 |
| 8 | CNTR-006 (whole file; `:347` "getNotificationLabel") | producer and progress rules of the sync-state repository | **Silent** on the foreground; nothing in it changes |
| 9 | CNTR-001, -002, -003, -007, -008 | none | **Silent** |
| 10 | DES-005 `:54` / `:55` | WorkManager "owns worker lifecycle plus foreground promotion" | **True** |
| 11 | DES-005 `:129-131` / `:130-132` | "where a user-visible notification is required it uses `setForeground` / `getForegroundInfo`" | **True**: it is now required only for user-started runs. No worker overrides `getForegroundInfo` (none in `app/src/main` at `fd70325a`), so that half was already loose wording before this story. BUG-029's optional clarification still applies (story H-2) |
| 12 | DES-007 `:272-273` | DES-005 "merges the foreground lifecycle of SmsBackupService/SmsRestoreService into the workers via setForeground" | **True**, and closer to the legacy behaviour: the legacy service promoted only manual backups and restores (BUG-029 Root Cause) |
| 13 | DES-008 `:186-188` / `:187-189` | the workers absorb "the foreground lifecycle" of the services | **True** (same reason as 12) |
| 14 | DES-012 `:652`, `:697-757`, `:791`, `:855-890`, `:933-936` | the manual-path foreground design of REQ-013 (service-as-shell, "setForeground() on the worker is NOT chosen") | **Silent / history.** U-049 already superseded it by moving to `setForeground` in the workers. U-075 does not change that status |
| 15 | DES-012 `:198`, `:407`, `:664-666` | preserve "MANUAL notification/foreground and SKIP handling" | **True** |
| 16 | DES-012 `:220`, `:450`; DES-001 `:39-94`, `:133-143`, `:187`, `:248-253`, `:285`; REQ-001 `:36-37`, `:116-129`, `:141`; EPIC-001 `:74-75`, `:159` | `dataSync` type and permissions on `SmsBackupService`/`SmsRestoreService` | **Silent** (those services were deleted by U-049; history) |
| 17 | DES-013 `:307`; DES-014 `:313` / `:320`, `:1660` / `:1667`, `:1896` / `:1903`; REQ-016 `:101-104` (AC-7) | merged-manifest invariant: `SystemForegroundService` with `foregroundServiceType="dataSync"` (0x1) | **True**: the plan changes no manifest (AC-8), and the override is still needed for MANUAL, SKIP and restore |
| 18 | **DES-014 `:1275` / `:1282` (row 16-1)** | "long work runs as a dataSync foreground service" | **False** for automatic backups: **B-1** |
| 19 | DES-014 `:1298` / `:1305` (AC4-a) | "AC-9 starts the foreground worker" (manual backup) | **True** |
| 20 | DES-014 `:1588` / `:1595`; REQ-016 `:118-121` (AC-13) | device checks grep logcat for `ForegroundServiceStartNotAllowedException`; AC-13: none during a manual backup, manual restore and a scheduled backup | **True** and easier to meet: a scheduled run no longer asks. See O-2 for the new warning text, which contains the class name |
| 21 | DES-014 `:1911` / `:1918`; REQ-017 `:129-132` (AC-9) | "a manual backup starts the WorkManager foreground worker, shows its notification" | **True** (AC-4; device step (iii)) |
| 22 | DES-014 `:2036` / `:2043` (risk 4) | WorkManager 2.12 may change "its manifest entry or foreground behaviour" | **True**; U-075 adds a dependency on how a refusal surfaces in `doWork`, read in 2.12.0 by BUG-029 (L1 to L4), and handed to U-073 (plan hand-over 6) |
| 23 | DES-014 `:1102` / `:1109` | 2.10.5 "Fix foreground service stopping when there are pending commands" | **Silent** (release-note row) |
| 24 | REQ-005 `:41` / `:42` | WorkManager "owns worker lifecycle and foreground promotion" | **True** |
| 25 | REQ-013 `:155-172` (AC-8) | a documented decision whether manual-backup foreground promotion is kept via `setForeground()` | **True**: plan 2.1 records the decision (kept for MANUAL, SKIP and restore; dropped for automatic types) |
| 26 | REQ-016 `:49-51` | "`BackupWorker.kt` and `RestoreWorker.kt` call `setForeground()` with `FOREGROUND_SERVICE_TYPE_DATA_SYNC`" | **True in substance, imprecise in letter**: after the change the literal `setForeground(` call is in `WorkerForeground.kt`, reached from both workers, and from `BackupWorker` only for user-started types. CR-017 E-3 rules it "a description for the library check, not a promise"; I agree it is not a blocker (O-4) |
| 27 | REQ-015 `:216` | "backup, restore, scheduling, and notification behavior is identical before and after" | **Silent**: a constraint on REQ-015's own toolchain change, not on this story |
| 28 | REQ-017 `:104-105` (AC-4) | the review covers "exact-alarm and background-start restrictions affecting auto-backup scheduling" | **Silent** (a review obligation of U-072); U-075 is relevant input for that row (O-5) |
| 29 | EPIC-005 `:123`, `:169`; EPIC-006 `:94`; REQ-018 `:163` | "notification state transitions", "no change in … notification copy" | **Silent** (scoped to those epics' own changes) |

Not in scope but read as context: the code comments `AndroidManifest.xml:116-118`, `MainActivity.java:510`,
`BackupWorker.kt:510` and `RestoreWorker.kt:683` at `fd70325a`. Plan 2.7 handles them correctly as known, partly stale
comments (hand-over 3).

## Item 2: scheduler and event contract invariants

- **INV-1 to INV-4 and Rules 1 to 8:** unchanged. The plan edits only the first statements of the two `doWork`
  functions (E-B1, E-B2, E-R1). These sites do not touch any scheduling part: unique names, policies, constraints,
  backoff, tags, the content trigger or `scheduleManual`. I checked them against the post-U-070 blobs: each `old_string`
  is present once, and the "starting" lines and the `try` follow unchanged. The 300 s backoff cap check still runs on
  every attempt; it now runs after the helper, as it ran after `setForeground` before.
- **Result of a refused or failed run:** before the change a refusal escaped `doWork` as an exception. WorkManager
  recorded FAILURE, or skipped to the next period for periodic work (BUG-029). That is neither the contract's
  `Result.retry()` nor its `Result.failure()` with a reason. After the change, an automatic run is never refused. A
  refused user-started run returns its own result, and the contract's mapping applies to it. Every other exception at
  that call (type exceptions, WorkManager's own `IllegalStateException`, `SecurityException`) still escapes `doWork`
  exactly as today (AC-5). This was already outside the `try` before the change, so the contract's position on it is
  unchanged.
- **Progress events of a run that never entered the foreground:** CNTR-004 requires only that progress surface through
  `setProgress`/`observe()`. CNTR-006's rules govern `emitState` and events, and none depends on the foreground. The
  plan's L9 (the progress updater is independent of the foreground updater) and its reading of `MainViewModel` and
  `StatusPreference` (2.5 item 3) agree with both contracts. The status screen's suppression of background types except
  errors is untouched.
- **P-2 against the contracts:**
  - Items 1 to 3 of plan 2.5 are consistent with CNTR-004 Error Handling and SchedulerState.
  - Item 4 says a refused user-started run stays in WorkManager's foreground map, where a system `stopWork` is ignored
    until it ends. No contract makes a promise about system stops: Rule 7 is about the port's `cancel`. Since U-049,
    every accepted manual run has been in the same map, so U-075 adds no new kind of state. It adds refused runs to an
    existing class, and it removes every automatic run from that class.
  - Neither the plan nor I checked that a user cancel (`cancelUniqueWork`, Rule 7) still interrupts a foreground-mapped
    run in 2.9.1 (see O-1 and "Not verified").
- **Foreground-info obligations of expedited or long-running work:** no contract or design states one. No enqueue path
  uses `setExpedited` (`git grep` at `fd70325a`: none), so WorkManager's requirement that expedited work below API 31
  provide `getForegroundInfo` does not apply. No contract requires `getForegroundInfo` to be overridden.

## Item 3: manifest and permissions

- `FOREGROUND_SERVICE` (`AndroidManifest.xml:65`), `FOREGROUND_SERVICE_DATA_SYNC` (`:67`), `POST_NOTIFICATIONS` (`:69`)
  and the `SystemForegroundService` override with `dataSync` (`:126-129`), all at `fd70325a`, are still used: by MANUAL,
  SKIP and every restore. None becomes unused, and the plan changes none (AC-8).
- No contract, design or requirement requires the foreground type for automatic backups. None mentions Android 14/15
  limits on `dataSync` (no `dataSync` hit mentions a time budget). The only design text that describes automatic or
  scheduled work as a foreground service is B-1's cell.
- Line numbers in the documents are stale but pre-existing, not caused by this story:
  - REQ-016 `:50` cites the override at manifest lines 141-144.
  - DES-014 AC4-a cites `:140-143`, `:81` and `:83`.
  - At `fd70325a` the lines are `:126-129`, `:67` and `:69`.

## Item 4: interfaces other stories consume

- **U-074** (story `sdlc/artifacts/build/sprints/sprint-025/U-074-cancelled-run-is-not-reported-as-a-failure.md`):
  - IC-1 searches `catch (e: Exception)`, `catch (e: IllegalStateException)`, `catch (e: Throwable)` and `runCatching`
    "over the two worker files and the classes they call on the run path". It states that every other catch names
    `MailException`, `XOAuth2FailedException`, `RequiresLoginException`, `SecurityException` or `TokenRefreshException`.
  - After U-075 that sentence becomes inaccurate: the helper's `catch (e: IllegalStateException)` (`WorkerForeground.kt:47`;
    I counted the line in plan 3.3, and it matches) is on the run path.
  - The plan states this correctly: plan 2.7, hand-over 2, and story IC-2/H-1 carry the line into U-074's plan as "left
    on purpose", together with the race of edge case 5.
  - U-074's AC-17 measures from its own Start, so U-075's lines are not counted against it.
  - Gap (O-7): U-074's `dependencies` list U-070, U-071 and U-073 but not U-075.
- **U-071** (`sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md`, searched for `BackupWorker.kt`):
  - M-1 is `16 spaces + currentCoroutineContext().ensureActive()` with expected count 1 in `BackupWorker.kt`
    (oldtext entry `:1059`). In the post-U-070 blob it occurs once, at `:339`.
  - U-075's new text contains no such line, so the count stays 1; the plan's `:343` after the change follows from its
    `+4` shift.
  - U-071's F-1 grep (`:194`, `:1993`) expects 0 `coroutineContext` hits in `WorkerForeground.kt`. The helper text in
    plan 3.3 has none.
  - U-071 already names U-075 and the new file (`:40-46`) and expects U-075 `done` at its I0 (`:1800-1807`).
  - The plan's statements (2.7, L20) are correct.
- **Later CR-017 stories** (BUG-026/BUG-027 scheduler fix, receiver removal, BUG-025 text):
  - No shared file: they change `WorkManagerScheduler.kt`, the receiver and the manifest's receiver entry; U-075 changes
    none of these.
  - CR-017 `:497-498` and BUG-029 "Interactions" place U-075 before the scheduler story's device check, and the plan
    agrees (2.7).
  - After CR-017 the boot path registers `REGULAR` as periodic work. After U-075 that work runs without the foreground.
    No v4 sentence states otherwise.
- **U-073:** the plan hands over the 2.12.0 points (hand-over 6). Correct.

## Item 5: what the plan lists as verified

- Frontmatter `contracts_verified: [CNTR-MODERNIZATION-004]`. Section 8.1 covers CNTR-004, DES-005, REQ-013, REQ-005 and
  REQ-015, and each statement it makes about them is correct. I checked CNTR-004 Rule 8 at HEAD `:280` and working tree
  `:320`, DES-005 at `:55` and `:130-132` in the working tree, and CR-017 `:247-249`.
- The plan does not claim to have searched DES-014, CNTR-005 or CNTR-006, and those are what the story's claim depends
  on. The plan's "No contract change is needed" leans on CR-017 E-3's search, which missed B-1's cell. The plan should
  not be failed for this, because it relays the story and the approved record accurately. At close-out the frontmatter
  can list CNTR-005 and CNTR-006 as checked, both "silent / true" in this report.
- 8.1 "No REQ with `type: standard` exists": re-run now (`git grep -l -E "^type: '?standard'?" -- sdlc/artifacts/requirements`
  prints nothing). True.

## Observations

- **O-1 (planner, optional):** plan 2.5 item 4 and L10 cover a system `stopWork` (ignored for foreground-mapped work).
  They do not say whether a user cancel (`cancelUniqueWork` → `Processor.stopAndCancelWork` in 2.9.1) still interrupts a
  refused-but-continuing manual run. It should, as it does for an accepted foreground run since U-049. A one-line
  `javap` reading in L10 would close the question for CNTR-004 Rule 7. U-074's H-2 test covers it at WorkManager level
  in any case.
- **O-2 (orchestrator, U-071/U-073 device checks):** the helper's warning ends `(ForegroundServiceStartNotAllowedException)`.
  A logcat grep for that class name (DES-014 `:1595`, REQ-016 AC-13, U-075 AC-12 (iv)) will now also match the app's own
  warning line, in addition to a stack trace. That is the intended signal: a refusal during an on-screen manual run is
  still a finding. Device-check writers should read the match as "refused and continued", not as "crashed".
- **O-3 (orchestrator, optional):** CNTR-005 `:213` "notification/state channels" stays true through the state channel
  only. If the clarification of story H-2 is written, it could add "(automatic backups, including BROADCAST_INTENT, show
  no progress notification)" to keep the broadcast contract's wording exact. Not required.
- **O-4:** REQ-016 `:49-51` (row 26) reads well enough after the change. If the B-1 vehicle is opened anyway, a
  precise form is: "`BackupWorker.kt` (for backups the user starts) and `RestoreWorker.kt` ask for the foreground
  through `setForegroundOrContinue` (`WorkerForeground.kt`), which calls `setForeground()` with
  `FOREGROUND_SERVICE_TYPE_DATA_SYNC`".
- **O-5 (U-072):** REQ-017 AC-4's "background-start restrictions affecting auto-backup" row should cite BUG-029/U-075 as
  "applicable and fixed".
- **O-6 (orchestrator / plan row D23):** AC-8 requires `git diff --name-only <start> -- … sdlc/artifacts/design
  sdlc/artifacts/build/change-records` to be **empty**. With CR-017's uncommitted working-tree edits to CNTR-004, DES-001,
  DES-005, DES-008 and DES-014, that command, run against the working tree at U-075's end, will list those files. Plan row
  D23 turns this into "record, not a stop", which reads AC-8 as "this story changes none", but no ruling records that
  reading (it is not in SC-1 to SC-9). Either commit CR-017 (and B-1's edit) before U-075's Start, or rule D23's
  reading. The first is cleaner.
- **O-7 (orchestrator):** U-074's `dependencies` (U-070, U-071, U-073) omit U-075, although ruling 60 orders U-075
  before U-074 and U-074's IC-1 text depends on it. Add U-075 when U-074's plan takes H-1.
- **O-8:** the plan's IC-1 expectations (I3 step 3: five `setForeground(` lines, four of them comments; three
  `setForegroundOrContinue` lines) are right against the blobs. Each post-U-070 worker has two `setForeground(` lines:
  one call and one KDoc. The story's literal IC-1 wording ("shows the call inside the helper only") is met only when the
  comment lines are read as non-calls, which the plan does explicitly.

## Not verified

- WorkManager 2.9.1 `Processor.stopAndCancelWork` behaviour for a foreground-mapped work (O-1). Not read, because
  reading it would need jar extraction beyond this gate's scope.
- The plan's library facts L5 to L13 and L19 (bytecode, caches, dex). I took them as the planner's readings and did not
  re-run them.
- Whether Android 16's job-quota rules treat ordinary jobs differently from jobs running beside a foreground service. B-1
  rests only on the cell's text becoming false, not on that platform detail.
- CR-017 Amendment 1 is not yet applied (its own status row says so). Any sentence it adds later was not checked.
- The working tree under `app/` (U-070's session). By instruction, I read only the blobs named.

## Phase Completion Report

```yaml
story_id: "U-075"
phase: "plan-gate/contract-verification"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-075/contract-verification.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
```
