---
artifact_type: plan
story_id: "U-075"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-21T18:01:03Z"
iteration: 2
base_commit: "acc50c94c1c635685164518731ab1b217dca76f2"   # PLAN_HEAD of iteration 2 (documents only since fd70325a; iteration 1: 3770b9a4); code read at fd70325a and the two post-U-070 blobs; the start commit is read at I0 (the commit that closes U-070)
contracts_verified:
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-005
  - CNTR-MODERNIZATION-006
risks_identified: 15
---

# Implementation Plan: U-075 — Automatic Backups Run as Ordinary Background Work; the Foreground Is Asked Only for Work the User Started, and a Refusal Is Absorbed (BUG-029, Ruling 60)

Story: `sdlc/artifacts/build/sprints/sprint-024/U-075-automatic-backup-runs-when-the-app-is-off-screen.md` (binding).
Source bug: `sdlc/artifacts/stories/BUG-029-background-backup-fails-at-foreground-start.md` (Suggested Approach option (c),
"Exactly what is caught", "Interactions", "Not verified"). Rulings: Record 21 ruling 60 (approach and place), Record 22
ruling 66 (Skip is user-started) and ruling 68 (PO-K and PO-L not asked now; the fix builds "no"), Record 18 (no external
`diff` or `cmp`). Form: `sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md`.

**Planning state.** Planned at `PLAN_HEAD` `3770b9a4` on `sdlc/modernization-plan`. `git diff --stat fd70325a 3770b9a4 -- app`
is empty, so every code fact below is read at `fd70325a` with `git show` (the commit that closed U-069). Story U-070 is
being built in this checkout now; the working tree under `app/` and `app/build` was not read. The post-U-070 text of the
two workers was produced in a scratch folder by applying U-070's own edits P6a, P6b, P6c and P7 (U-070 plan sections 3.6
and 3.7, extracted from the plan by line range) to the `fd70325a` blobs (`BackupWorker.kt` `eb535f5c…`, `RestoreWorker.kt`
`ffa9c57e…`): each U-070 old text occurred exactly once; the simulated post-U-070 blobs are `ba667c43…` and `dc84c4f5…`.
No Gradle, build, test, device or network was run.

**The start commit is not fixed by this plan.** `<start>` is `HEAD` at I0: the commit that closes U-070 (status `done`).
`C0` (the authoritative count), the Gradle totals and the coverage figures are measured there. Every expected total is
written as "start + N".

**Size.** Two tracked production files change, both inside the first statements of `doWork()` and in no region U-070
edits: `BackupWorker.kt` (two Edits, `--numstat` `10 6`) and `RestoreWorker.kt` (one Edit, `5 5`). One production file is
new: `service/WorkerForeground.kt` (65 lines, one `internal suspend` function and one private API-31 holder object). Two
test files are new: `BackupWorkerForegroundTest.kt` (13 tests in two classes) and `RestoreWorkerForegroundTest.kt` (5
tests): **18 tests**. No build, manifest, scheduler, contract or design file changes. **12 mutation runs**: the story's
M-F1a to M-F6, one more of this plan's (M-F7, the restore asks for nothing), one informational class-loading probe (M-X1)
and the three U-070 re-runs (M-L11, M-L20, M-L22). **16 Gradle runs**: two full gates (I0 and I6, the two AC-9 asks for)
and 14 targeted runs (I2 1, I3 1, I5 12) whose `--tests` lists leave out U-070's 5.5-minute `SmsBackupRecordSizeTest`.
**One developer session** is enough (section 4.10 gives the safe stopping points). The developer commits nothing.

**Coordinator's facts of 2026-11-21 (U-070's first build session), taken in.** (1) After U-070 a full gate takes about 9
to 10 minutes and usually passes the 10-minute foreground limit, so the harness moves it to the background: every Gradle
call writes its own `exit=` line into its log, the developer waits for the completion notice, never polls and never starts
a second Gradle, and a log without `exit=` is a cut-off run and a stop (row D29); the intermediate measurement gate of
earlier drafts (G-M) is dropped, coverage is read at I6 only. (2) The start count is expected to be 821 (771 at `fd70325a`
plus U-070's 50) if nothing else lands; it is still recorded at I0 with its commit, not assumed. `jacoco_pkgs.py` prints
dotted package names (`com.zegoggles.smssync.service`). (3) U-070's P6 and P7 were applied exactly as its plan gives them;
its session-1 blobs `BackupWorker.kt` `ba667c4365993121fa91520a813ba84cdc5ae8d5` and `RestoreWorker.kt`
`dc84c4f50f9c49740ddf49cad2f4ac4a39f5f281` equal this plan's simulated post-U-070 blobs (L20), which confirms the
simulation. (4) The U-063 init script is not needed: the build writes JUnit XML itself (L24).

**Compile check.** The helper (section 3.3), its three non-trivial mutants (M-F2, M-F3, M-X1) and both test files
(section 3.4) were compiled in a scratch folder with the cached `kotlin-compiler-embeddable` 2.3.21 (the build's Kotlin),
without Gradle and without network: the helper and the mutants against the SDK's `android-35/android.jar`, the cached
`work-runtime-2.9.1` classes, `kotlin-stdlib` 2.3.21, `kotlinx-coroutines-core-jvm` 1.7.3 and `androidx.annotation`
1.8.1; the tests additionally against `work-testing-2.9.1`, `preference-1.1.0`, Robolectric 4.15.1's jars, JUnit 4.13.2,
Hamcrest 1.3, Truth 1.4.4, Guava 33.0.0 with `failureaccess` 1.0.2, and **stubs** of the app classes they name, written
from the signatures read at `fd70325a` and from U-070's fixture text (its plan 4.1). All exit 0. The stubs are mine, not
Gradle's: the first real compile is the developer's I2 run (row D12).

## Iteration 2 (after the plan gates, 2026-11-21)

Iteration 1's gates: plan-review **FAIL** (B-1 to B-6, O-1 to O-9), contract-verification **FAIL** (B-1 is the
orchestrator's; observations O-1 to O-8), expert-depth-check **PASS** with two "should" recommendations (the three reports
in this workspace, read in full). Binding since then: the rulings on SC-1 to SC-9 in the story's Implementation Notes
(Record 23), the orchestrator's ruling on contract O-6 / row D23, and ruling 69 (self-test every command that can be
tested without Gradle, a device or the working tree under `app/`; section 14). `PLAN_HEAD` is now `acc50c94`; `git diff
--stat 3770b9a4 acc50c94 -- app` is empty. The design, the exact edits (3.1 to 3.3), T1, the red/green split, P-1's
facts and all twelve failing sets stand as plan-review verified them. Nothing is declined without a reason.

| Finding | Change, and where |
|---------|-------------------|
| plan-review **B-1** (`ui_bounds.py` blob) | Every embedded file's blob is now derived **from the plan text itself**: a scratch script takes the first fenced block after each file's marker line, writes it LF and hashes it (`git hash-object --no-filters` and a SHA-1 of `blob <n>\0` agree); section 14.1 gives method and values. Iteration 1's `c7f765f0…` was the hash of a scratch copy whose text had drifted from the printed text. `ui_bounds.py` also changed (O-6 below) and has the new blob of 3.5; T2 changed (O-3); the other six values are unchanged |
| plan-review **B-2** (section 9 against SC-1, SC-2, SC-3, SC-9) | Section 9 rewritten: (i) passes on the confirmed-append line `BackupWorker: watermark advanced for SMS to <date>` and `backup complete, backedUp=` (SC-9; no mailbox check); no outcome rests on a screenshot or UI dump, and none is taken into the repository: the notification evidence is `notif_check.py` over `dumpsys notification` (SC-1; screenshots dropped altogether); (v) is "Not run: needs a system setting" (SC-3); (vi) is the orchestrator's, selects `Pixel_API_24` by serial and `emu avd name` after `Pixel_9_Pro` is stopped, records install-time verification, and records "not run: needs the owner to enter the app password" when the missing-credentials dialog prevents the run (`MainActivity.java:391-402`; iteration 1's "MailException — retrying" sentence was wrong and is deleted) (SC-2). Section 10 item 7 no longer asks for rulings |
| plan-review **B-3** ((iv) on no observed run) | (iv) is assessed only over the off-screen runs observed ((i)'s INCOMING if it met the off-screen rule, (ii)'s REGULAR if it started); with none: "(iv) not run: no off-screen run observed". `ac12_run.py` refuses an empty process-id list, so no count is taken from a log that was not read (self-test 14.7) |
| plan-review **B-4** (`/sdcard` path) | `MSYS_NO_PATHCONV=1` on each of the three `adb` commands with a device path, on that command only; tested: without it Git Bash turns `/sdcard/u075-ui.xml` into `C:/Program Files/Git/sdcard/u075-ui.xml`, with it the path is passed unchanged, and no other section 9 argument is rewritten (14.6). `exec-out uiautomator dump /dev/tty` was considered and not used (it appends a status line to the XML; `ui_bounds.py` then prints `xml=unreadable`, 14.2) |
| plan-review **B-5** (waits above the ceiling) | No `timeout` above 540 s; the 15-minute windows of (i) and (ii) are two waits of 450 s anchored at the same `-T`; the boot wait is 540 s, repeated once; the captures that follow a start are chained to each wait (section 9, shell rules; `timeout` exit 124 and `&&` behaviour tested in 14.8) |
| plan-review **B-6** (rows missing) | New rows D32 (final-check hashes or porcelain differ after G-F: G-F void, restore, report), D33 (tracked tests changed, `@Ignore`, counts), D34 (JDK, a running Gradle JVM, the Daemon JVM line), D35 (I1/I4 porcelain, `main-clean-exit`, `oldtext_check.py fixed` at I4); each cited in its step's **Rows:** line |
| plan-review O-1 (cut-off run inside I5) | D29: inside an I5 entry, protocol steps 5 and 6 (restore, clean check, `restored` line) come first, then the stop |
| plan-review O-2 (no clean check before G-F) | New I6 step 0: the 4.8 clean check and no open `APPLYING` entry |
| plan-review O-3 (AC-2 privacy for the restore) | R2 also asserts that no log entry carries `SmsFixture.OWNER`. T2's blob changes to `e69175c6…` (174 lines); recompiled against the stubs (exit 0); red/green split and every failing set unchanged (no injection writes the account name) |
| plan-review O-4 (file count) | 4.9 recounted from the list: 53 with the three new tools |
| plan-review O-5 (`grep -A14`) | I0 step 3 reads whole entries with `awk` up to the next `### ` and translates U-070's B/S ids through its plan's table 4.8 (tested on U-069's committed log, 14.10) |
| plan-review O-6 (button label in capitals) | `ui_bounds.py` compares the label without regard to case and handles unreadable XML (14.2) |
| plan-review O-7 (snapshot load) | (ii) starts the emulator with `-no-snapshot-load -no-snapshot-save`, and "no bootup line" is recorded "(ii) not run" |
| plan-review O-8 (second inconclusive (iii)) | Recorded "(iii) not run: notification not captured", never a pass |
| plan-review O-9 (fixtures at SDK 24) | Taken as stated: rows D11 (c), (d) cover it; noted in 8.3 R2 |
| contract-verification **B-1** (DES-014 row 16-1; the orchestrator's) | 8.1 corrects the story's claim; section 10 item 9 carries **X-3**, the close-out edit of CR-017 Amendment 2 (E-7), for the orchestrator |
| Orchestrator's ruling on contract O-6 / D23 | I0 step 2 records each uncommitted path by others under `sdlc/artifacts/design`, `sdlc/artifacts/requirements`, `sdlc/artifacts/build/change-records` with its `sha256sum`; I6 step 5 shows each unchanged, or committed in between with the orchestrator's subject; D23 rewritten; tested in a scratch repository (14.17) |
| contract-verification O-1 (user cancel of a refused manual run) | Read in 2.9.1: `Processor.stopAndCancelWork` adds the id to the cancelled set, calls `cleanUpWorkerUnsafe` (which removes it from the foreground map) and interrupts it, so a user's cancel still stops a refused run (new L27; 2.5 item 4) |
| contract-verification O-2 (the warning names the class) | Section 9 gives the two exact greps (the app's `W SMSBackup+` warning = refused and continued; the `E WM-WorkerWrapper` line = the defect), tested on today's real log (14.7); hand-over item 10 for U-071 IC-4 and U-073 |
| contract-verification O-3 to O-5, O-7 | The orchestrator's (CNTR-005 wording, REQ-016 wording, U-072's REQ-017 row, U-074's dependency); listed in section 10 item 9; nothing in the plan changes |
| contract-verification O-8 (IC-1 wording) | Already explicit (I3 step 3: the four comment lines and the one call); unchanged |
| depth-check **should 1** (release dex) | I6 step 4 maps the two class names through `mapping.txt` and runs the dex reference listing over the **release** dex as well; D25 extended to the release build (pipeline tested on the D8 output with a mapping file, 14.9); L22 says what the release check measures |
| depth-check **should 2** (platform consequences, device evidence) | 2.6 item 3 replaced (ordinary-job stops, Android 11's limit, the unbounded run size, the interim BUG-024 ERROR line, Doze and lost constraints); item 8 corrected (the second refusal point is already in 2.9.1, new L29); items 10 (no `getForegroundInfo` override needed; the Android 15 `BOOT_COMPLETED` rule is moot) and 11 (default-SMS role, not read); R10 revised; section 12 Reliability; section 9: the `POST_NOTIFICATIONS` precondition, the process, service and job state at the INCOMING start, the "stopped by the system" outcome, `Unable to start foreground service` in the counts, "What it does not prove" extended; hand-over item 6 extended |
| depth-check, "which scheduler started the work" | Partly declined, with the reason: WorkManager's "onStartJob for" and "Starting work for" are debug lines and the app's WorkManager configuration logs from INFO (`App.java:162-165`; L29), so they never reach logcat; the scheduler dump's lines for the app are recorded instead and no outcome depends on them |
| depth-check "could" items | Taken: the F13 sentence (2.1 (b)), "not reached: <reason line>" in (ii), the default-SMS role note (2.6 item 11). Declined: a retry case in F1 and a cancellation-on-the-pending-future variant of F12. Reason: the order "helper before the backoff check" is fixed by the edit text and the hunk check of I3, and the pending-future cancel branch is WorkManager's own code read in L5 that ends in line 48's rethrow; adding tests now would reopen the twelve failing sets plan-review verified, for no criterion |
| Ruling 69 | New section 14: every command and tool that can run without Gradle, a device or `app/` was run on input written in scratch, with the real log lines of today's observation where formats matter. Found and fixed while testing: on today's **unfixed** build a refused INCOMING run writes no start line, so (i) and (ii) now count `defect` above 0 as a fail before any "held by quota" outcome; `ls` on this machine is aliased to `ls -F` and appends `/` to directory names, so the cache listing uses `command ls -1`; `sha256sum` marks paths with `*`, so paths are cut with `sed -E 's/^[0-9a-f]{64} \*?//'`; Python writes CRLF to a redirected stdout, so tool output is read field-wise and `\r` is stripped where a value is passed on |

Totals after iteration 2: **18 tests**, **12 mutation runs** (8 counted), **16 Gradle runs**, one session; six evidence
tools in 3.5 (`oldtext_check.py` and `dex_refs.py` for the developer; `ui_bounds.py`, `notif_check.py`, `dt_secs.py` and
`ac12_run.py` for the orchestrator), all written by the developer at I0 so that they are committed with the story.

## 0. Binding Inputs

| Source | What binds this plan |
|--------|----------------------|
| Story AC-1 to AC-12, IC-1 to IC-3, P-1 to P-3, start conditions, binding constraints | As written. Where this plan is stricter (the control test F2, the two future-shape tests F4 and F5, the API 24 test F13, injection M-F7, the probe M-X1, the dex check), section 8.2 says so |
| Ruling 60 (Record 21) | Foreground only for user-started work (manual backup, Skip, restore); automatic backups run without it; one helper catches only the refusal, logs one warning without message content, lets a cancellation through; the story goes after U-070 and before U-074, U-071 to U-073 and the scheduler stories |
| Ruling 66 (Record 22) | Skip is user-started and asks for the foreground |
| Ruling 68 (Record 22); PO-K, PO-L (Record 21) | Not asked now; the fix builds "no notification for automatic backups" and asks for no battery exemption |
| Record 18 | No external `diff` or `cmp`; `git diff --no-index` or `sha256sum`; modest shell activity; stop on a vanished tool or killed process |
| REQ-MODERNIZATION-015 AC-13 as amended by CR-016 | The count rule (AC-10) |
| REQ-MODERNIZATION-013 AC-8 | A documented decision whether foreground promotion is kept through `setForeground()`: this plan records it (section 2.1): kept for `MANUAL`, `SKIP` and every restore; dropped for automatic types |

## 1. Assumption Ledger

`verified` = read or run read-only in this session, with where. `needs_check` = a developer step with a decision row.
`plausible, not verified` = not read. Line numbers are at `fd70325a` unless marked "post-U-070" (from the simulation).

| # | Claim | Status | Evidence |
|---|-------|--------|----------|
| L1 | `BackupWorker.doWork()` starts at `:118`; `:119-122` U-049 comment; `:123` `setForeground(createBackupForegroundInfo())`; backoff cap `:125-137`; `val ctx` `:139`; comment `:140-141`; `val backupType = inferBackupType()` `:142`; starting line `:144`; `return try {` `:146`; `MailException` → `Result.retry()` `:164-167`; broad catch `:168-171`; end `:172`. U-070 edits nothing above `:206` (P6a at `:206-211`, P6b `:356-357`, P6c `:422-423`), so these lines are the same at `<start>` | verified | `git show fd70325a:…/BackupWorker.kt`; U-070 plan 3.6; simulation |
| L2 | `RestoreWorker.doWork()` starts at `:143`; `:144-147` comment; `:148` `setForeground(createRestoreForegroundInfo())`; backoff `:150-158`; starting line `:163`; `try` `:165`; `transport` from the factory `:167`; `MailException` → `Result.retry()` `:194-197`. U-070's only edit (P7) is at `:461`, so these lines are unchanged at `<start>` | verified | file read; U-070 plan 3.7 |
| L3 | `inferBackupType()` (`:501-507`) returns the first tag that names a `BackupType` other than `UNKNOWN`, else `BROADCAST_INTENT`; `BackupType.isBackground()` is `this != MANUAL && this != SKIP` (`BackupType.java:37-39`); enum `BROADCAST_INTENT, INCOMING, REGULAR, UNKNOWN, MANUAL, SKIP` (`:7-12`). Production tags: the worker's class name plus the type (`WorkManagerScheduler.kt:149`, `:189`, `:277`, `:312`, `:353`) | verified | files read |
| L4 | `createBackupForegroundInfo()` (`:516-539`): `App.CHANNEL_ID`, id `BACKUP_NOTIFICATION_ID` = 1 (`:545`), `FOREGROUND_SERVICE_TYPE_DATA_SYNC` from API 29; `createRestoreForegroundInfo()` (`RestoreWorker.kt:689-712`), id `RESTORE_NOTIFICATION_ID` = 2 (`:718`). `App.CHANNEL_ID = "sms_backup_plus"` (`App.java:95`). Title text `R.string.status_backup` = "Backing up" (`strings.xml:24`), not "Backup" as the story says (section 8.2, SC-8) | verified | files read |
| L5 | WorkManager 2.9.1 `ListenableWorker.setForegroundAsync(ForegroundInfo)` calls `getForegroundUpdater().setForegroundAsync(ctx, id, info)` directly (offsets 0-21, no handler). `CoroutineWorker.setForeground`: if the future `isDone`, `get()`, and an `ExecutionException` is unwrapped to its cause (exception table `25-35 → 35 ExecutionException`); otherwise `ListenableFutureKt$await$2$1` resumes the continuation with `getCause()`, and cancels it when the throwable is a `java.util.concurrent.CancellationException`. An exception thrown by the updater itself leaves `setForeground` at offset 2, outside the table: unchanged | verified | `javap -c -p` on `work-runtime-2.9.1.aar` classes (local Gradle cache) |
| L6 | Production updater `WorkForegroundUpdater`: returns a `SettableFuture`, runs its task on the task thread (`executeOnTaskThread`); the task throws `IllegalStateException("Calls to setForegroundAsync() must complete before a ListenableWorker signals completion of work by returning an instance of Result.")` for finished work, calls `ForegroundProcessor.startForeground` (offset 70) and then posts the notification by `startService` (offset 96); any `Throwable` (`0-109 → 112`) goes to `setException` (offset 118). So the refusal reaches `doWork` as `ForegroundServiceStartNotAllowedException` itself, normally through the pending-future branch of L5, and no notification is posted when the start is refused | verified | `javap -c -p -constants` on `WorkForegroundUpdater` and `$1` |
| L7 | work-testing 2.9.1: `TestListenableWorkerBuilder` has public `setForegroundUpdater(ForegroundUpdater)`, `setTags(List<String>)`, `setRunAttemptCount(int)`; `build()` passes the updater into `WorkerParameters`; the default `TestForegroundUpdater` (public) returns a `SettableFuture` set to `null` at once. `ForegroundUpdater` has one method, `ListenableFuture<Void> setForegroundAsync(Context, UUID, ForegroundInfo)` | verified | `javap -p`, `-c` on `work-testing-2.9.1.aar` and `work-runtime-2.9.1.aar` |
| L8 | `ListenableWorker.Result.Retry.equals` compares classes; `Success` carries output data | verified | `javap -c` |
| L9 | `ListenableWorker.setProgressAsync` goes to `getProgressUpdater().updateProgress(…)`, which is independent of the foreground updater | verified | `javap -c` |
| L10 | `Processor` 2.9.1: `cleanUpWorkerUnsafe(id)` removes the work from `mForegroundWorkMap`, `mEnqueuedWorkMap` and `mWorkRuns` and calls `stopForegroundService()` when the foreground map is empty; `onExecuted` calls it when the worker ends. `stopWork` logs "Ignored stopWork. WorkerWrapper … is in foreground" for foreground-mapped work (BUG-029 L4) | verified | `javap -c -p -constants` on `Processor` |
| L11 | `android.app.ForegroundServiceStartNotAllowedException` (API 31) extends `ServiceStartNotAllowedException` (abstract, extends `IllegalStateException`) and has a public `(String)` constructor; `ForegroundServiceTypeException` (abstract, API 34) extends `ServiceStartNotAllowedException`; `InvalidForegroundServiceTypeException` and `MissingForegroundServiceTypeException` extend it and have public `(String)` constructors. All five classes are in the cached `android-all-instrumented` jars for API 34 and 35; none in API 24's | verified | `javap -p` on the API 34 jar's classes; `unzip -l` of the 34, 35, 24 and 29 jars (local Maven cache) |
| L12 | **P-1, Robolectric.** 4.15.1's `DefaultSdkProvider.populateSdks` knows SDK 21 to 35 (no 36). The artifact is `org.robolectric:android-all-instrumented:<androidVersion>-robolectric-<robolectricVersion>-i7` (pre-instrumented jars on by default, `robolectric.usePreinstrumentedJars`). SDK 34 = `14-robolectric-10818077-i7`, 35 = `15-robolectric-12650502-i7`; 31 = `12-robolectric-7732740`, 32 = `12.1-robolectric-8229987`, 33 = `13-robolectric-9030017`. Required Java: 8 for 21-28, 9 for 29-33, 17 for 34-35 ("Android SDK %d requires Java %d"); the test JVM is JDK 25, so 34 and 35 are allowed | verified | `javap -c -p -constants` on `DefaultSdkProvider` and `$DefaultSdk` in `robolectric-4.15.1.jar` (local Gradle cache) |
| L13 | **P-1, cache.** The local Maven cache holds `android-all-instrumented` i7 jars for 21, 23, 24, 29, 34, 35 and 36 (the last for a newer Robolectric), each with a `.sha512` file; the SHA-512 of the 24, 29, 34 and 35 jars equals the file beside it. **No i7 jar for 31, 32 or 33** (33 exists as i6 only). The Gradle cache has no `android-all-instrumented` folder (Robolectric fetches with its own resolver into the Maven cache). `MavenArtifactFetcher` returns at once ("Found %s in local maven repository") when the jar file exists, so a run at 34 or 35 needs no download. So the story's middle fallback (31 to 33 only) is **not available offline** | verified | `Glob` of the two cache folders; `sha512sum`; `javap -c` on `MavenArtifactFetcher` |
| L14 | The project pins `sdk=29` (`app/src/test/resources/robolectric.properties:8`); per class at `fd70325a`: `sdk = [29]` 11 times, `sdk = Build.VERSION_CODES.Q` 7, `N` 2, `[24]` 1; no class above 29. A class-level `@Config(sdk = [35])` overrides the global pin | verified | `git grep` |
| L15 | Robolectric 4.15.1 `ShadowLog`: `getLogs()`, `getLogsForTag(String)` (Guava `ImmutableList<LogItem>`), `clear()`; `LogItem` public final `type`, `tag`, `msg`, `throwable` | verified | `javap -p` on `shadows-framework-4.15.1.jar` |
| L16 | U-070's fixtures as planned (its plan 4.1): `ProviderDouble(kind)` with `add(date, address, body, type = SmsFixture.INBOX, …)` and `beforeQuery: ((String) -> Unit)?`; `ContactsProviderDouble` with `contacts` (address → (contact id, raw id)), answering `phone_lookup/<address>` and `data/emails`; `RecordingTransport` with `appended`, `count(call)`, `appendMessages` returning the result's max date; `LateSmsHarness(context)` registering the SMS double, setting the IMAP user to `SmsFixture.OWNER` ("backup-owner"), initialising the test WorkManager, with `sms`, `preferences`, `dataTypePreferences`, `registerContacts()`, `watermark()`; `SmsFixture.D = 1_700_000_000_000L`. The names as **built** are checked at I0 by text (`oldtext_check.py start`, the `FX-*` ids; P-3) | verified (plan text); needs_check (as built) | U-070 plan `:1517-1974`; I0 step 2; row D4 |
| L17 | `InMemoryCheckpointStore` is an `open class` in `RestoreWorkerCheckpointTest.kt:580`; `RestoreInsertInterceptor.NoOp` is an `object` (`RestoreInsertInterceptor.kt:42`); `TestableRestoreWorkerFactory(checkpointStore, interceptor)` (`RestoreWorker.kt:760-786`) | verified | files read |
| L18 | `PersonLookup.java:76` writes "Looked up unknown address: <address>" at verbose level when `LOCAL_LOGV` (= `BuildConfig.DEBUG`, `App.java:92`) and the contact is unknown. It is the existing line of ruling 39 (Record 18), outside this story. The tests register the sender as a known contact so the branch is not taken (section 2.3) | verified | files read |
| L19 | The helper compiles; the only class that names `ForegroundServiceStartNotAllowedException` is `ForegroundRefusal` (3 constant-pool hits; 0 in `WorkerForegroundKt` and its continuation class). Its catch type is `java.lang.IllegalStateException` (API 1). Line table: `WorkerForegroundKt` lines 39, 44 to 51, 53 (10 lines); `ForegroundRefusal` lines 62 and 64 (2 lines). Dexed with build-tools 36.0.0 D8 at `--min-api 24`: `dexdump -d` lists only `Lcom/zegoggles/smssync/service/ForegroundRefusal;` as naming the type. WorkManager 2.9.1 itself names it in `androidx/work/impl/foreground/SystemForegroundService$Api31Impl` | verified | scratch compile; `javap -v -l`; D8 and `dexdump` from build-tools 36.0.0 (not AGP's own D8) |
| L20 | The text simulation (`fd70325a` + U-070 P6/P7 + this plan's E-B1, E-B2, E-R1): every old text once at the simulated start; after the edits `git diff --no-index --numstat` `10 6` (BackupWorker) and `5 5` (RestoreWorker); `-U0` hunks `@@ -119,5 +119,10 @@`, `@@ -142 +146,0 @@` and `@@ -144,5 +144,5 @@`; `doWork()` spans `:118-176` after the edit; post-U-070 BackupWorker 649 lines, after U-075 653; RestoreWorker 791 both. Simulated blobs after U-075: `3f2f0c7678fab5a7ddf5d36581f2df32cd19a643` and `9be01b15210508393e750136ff127cc7eed3a968`. U-071's anchor M-1 (`currentCoroutineContext().ensureActive()`, 16 spaces) at post-U-070 `:339`, after U-075 `:343`; U-070's anchors M-L11, M-L20, M-L22 once each before and after | verified (simulation) | scratch script; `git diff --no-index`; `git hash-object --no-filters` |
| L21 | `oldtext_check.py` (3.5) gives `mismatches=0` in `start` mode on the simulated start tree (with a CRLF copy of BackupWorker.kt, the U-070 fixture text, `RestoreWorkerCheckpointTest.kt` and `app/proguard-rules.pro` at `fd70325a`) and in `fixed` mode on the simulated fixed tree | verified (run) | scratch tree |
| L22 | Release: `app/proguard-rules.pro:57` `-dontoptimize` (no inlining, no class merging); no `-assumenosideeffects` rule under `app/` (`git grep` prints nothing); the helper is called directly, never by reflection or name; WorkManager's consumer rules keep the workers (U-069 L9). So no keep rule is needed. No `-dontobfuscate` under `app/`, so release class names are renamed; what `-dontoptimize` does in AGP 9's R8 is expected, not read: the release dex check of I6 step 4 measures it (iteration 2) | verified (the file); measured at I6 (the effect) | `git grep`; U-069 L9 |
| L23 | JaCoCo: Rule 1 `com.zegoggles.smssync.service*`, LINE, `minimum = 0.70` (`app/build.gradle:358-365`); measured `service` 674/822 = 82.0 % at U-069's I0, I4, I6 (`sprint-024/U-069/evidence/I0-jacoco-packages.txt:16`, `I6-…:16`). U-070 adds lines to `service`; its figure is measured at I0 here | verified | file read; evidence read |
| L24 | `app/build.gradle:21`, `:24-25`: compileSdk 35, minSdk 24, targetSdk 35; `:155-156` work 2.9.1; `:160` Robolectric 4.15.1; `:161` Truth 1.4.4; `:135` preference 1.1.0; root `build.gradle:29-30` `reports.html.required = false`, `reports.junitXml.required = true`; `gradle/gradle-daemon-jvm.properties` `toolchainVersion=25` | verified | files read |
| L25 | Guava's nullness annotations type `Futures.immediateVoidFuture()` as `ListenableFuture<Void?>`, which does not override `ListenableFuture<Void>`; so the counting updater delegates to `TestForegroundUpdater` (compile result in the scratch folder) | verified | scratch compile |
| L26 | Committed tools reused read-only: `sprint-020/U-063/evidence/tools/{sanitize.py,junit_totals.py,junit_cases.py,jacoco_pkgs.py}`, `sprint-024/U-069/evidence/tools/jacoco_classes.py`, `sprint-024/U-068/evidence/tools/sdk_dir.py` | verified | `git ls-tree fd70325a`; docstrings read |
| L27 | (Iteration 2, contract O-1.) WorkManager 2.9.1 `Processor.stopAndCancelWork(id, reason)` logs "Processor cancelling", adds the id to `mCancelledIds`, calls `cleanUpWorkerUnsafe(id)` (which removes the work from `mForegroundWorkMap`) and then `interrupt(id, wrapper, reason)`. So a user's cancel (CNTR-004 Rule 7, `cancelUniqueWork`) stops a foreground-mapped run, a refused one included; only `stopWork` (a system stop) ignores foreground-mapped work (L10) | verified | `javap -c -p -constants` on `Processor` |
| L28 | (Iteration 2, depth check.) `CoroutineWorker.getForegroundInfo` default (`getForegroundInfo$suspendImpl`) throws with the text "Not implemented"; no request uses `setExpedited` (`git grep -n setExpedited fd70325a -- app/src/main` prints nothing, exit 1); WorkManager calls `getForegroundInfoAsync` below API 31 only for expedited work (BUG-029 L8). `Defaults.MAX_ITEMS_PER_SYNC = -1` (`Defaults.java:23`): an automatic run is not bounded in size | verified | `javap`; `git grep` |
| L29 | (Iteration 2.) `androidx/work/impl/foreground/SystemForegroundService$Api31Impl.startForeground` in **2.9.1** calls `Service.startForeground(int, Notification, int)` and logs "Unable to start foreground service" at warning on a refusal or `SecurityException` (two handlers): WorkManager's own second refusal point exists already in 2.9.1 (iteration 1's 2.6 item 8 attributed it to 2.12.0, wrongly). The app's WorkManager `Configuration` sets no minimum logging level (`App.java:162-165`), so WorkManager logs from INFO: its debug lines "onStartJob for", "onStopJob for" (`SystemJobService`) and "Starting work for" (`GreedyScheduler`) do not reach logcat, while "Moving WorkSpec … to the foreground" and "Worker result …" (INFO) do, as today's log shows | verified | `javap -c -p -constants` on the three classes; `App.java` read |
| L30 | (Iteration 2, ruling 69.) Every embedded file was extracted from the plan text and hashed; every tool and every parsing, grep, hash and compare line of sections 4 and 9 that can run without Gradle, a device or `app/` was run on scratch input; the results are section 14 | verified (run) | section 14 |
| N1 | The two test files compile on Gradle's real unit-test classpath (with the real app classes and U-070's fixtures as built) | needs_check | I2; row D12 |
| N2 | Robolectric runs the SDK 35 classes offline: no download, the app starts in the sandbox, U-070's fixtures work at 35 (the control F2 and the characterisation tests pass) | needs_check | I2; rows D10, D26 |
| N3 | The API 24 class loads and runs at SDK 24 (it names no class above API 24; L19-style check of the test classes in the scratch compile: `BackupWorkerForegroundApi24Test`, `ForegroundCallLog`, the updaters, `ForegroundTestLog` and `BackupWorkerForegroundTestKt` name none of the five exceptions) | needs_check | I2; row D11 |
| N4 | JaCoCo reports `WorkerForegroundKt` and `ForegroundRefusal` with `missed=0`, and `service` does not fall | needs_check | I4, I6; row D16 |
| N5 | AGP's own D8 and R8 keep the API-31 reference in `ForegroundRefusal` (or a synthetic class named after it) | needs_check | I6 step 4; row D25 |
| P1 | With `-ea` (Gradle's default for tests), kotlinx-coroutines stack-trace recovery may copy an exception when a suspended coroutine resumes with it; the copy has the same class. The helper decides by class, and the tests compare class and message, never identity | plausible, not verified | kotlinx-coroutines 1.7.3 behaviour as commonly documented; the tests do not depend on it |
| P2 | work-testing 2.12.0 (story U-073) keeps the public `TestForegroundUpdater` and `setForegroundUpdater` | plausible, not verified | not in the cache; hand-over item 6 |
| P3 | ART on API 24 to 30 treats an unresolvable class in `instance-of` as a soft verification failure of that class only, and never loads `ForegroundRefusal` because the `SDK_INT` test comes first | plausible, not verified (ART) | the design does not rely on it: L19 shows the reference is not in the helper's class; optional device step (vi), SC-2 |

## 2. Design (decided)

### 2.0 Principles applied

Book chapters are cited as commonly given; I did not re-read them in this session.

- **Catch what you can handle, and only that** (Bloch, *Effective Java*, 3rd ed., Item 77 "Don't ignore exceptions" and
  Item 73 "Throw exceptions appropriate to the abstraction"; Nygard, *Release It!*, 2nd ed., ch. 4, "Stability
  Antipatterns"). The worker can handle exactly one condition, the platform's refusal to promote it: the work itself can
  still run. Every other exception at that call means a defect (a manifest regression, a missing permission, WorkManager
  misuse) or a cancellation, and must stay loud or keep its meaning.
- **Cooperative cancellation is not an error** (kotlinx.coroutines guide, "Cancellation and timeouts": a
  `CancellationException` must be rethrown; Goetz et al., *Java Concurrency in Practice*, ch. 7). `java.util.concurrent.
  CancellationException` extends `IllegalStateException`, so a catch of `IllegalStateException` must rethrow it first.
- **Isolate references to newer platform APIs in their own class** (the AndroidX `ApiNNImpl` pattern, used by WorkManager
  itself in `SystemForegroundService$Api31Impl`, L19; Android Developers, "Class verification failures", cited as the
  vendor names it, not fetched). The guard is the `SDK_INT` test; the isolation keeps the reference out of the caller's
  class so that its verification never meets an unresolvable type.
- **Least privilege for the foreground** (the legacy service promoted only manual backups, BUG-029 Root Cause Analysis;
  Google's foreground-service guidance that a foreground service is for user-noticeable work). Automatic work does not ask;
  work the user started asks and degrades gracefully (Nygard, ch. 5, "Fail Fast" for defects versus "Steady State" and
  graceful degradation for environmental limits).
- **Logs as an operational interface** (Nygard, ch. 10, "Transparency"): one warning per refused run, fixed prefix, the
  exception's class only; no message content, address, account or folder (privacy by design, GDPR Art. 25 as a principle;
  this app is private and unpublished).
- **Test doubles that the test owns, at the seam the library offers** (Meszaros, *xUnit Test Patterns*, ch. 11 "Using Test
  Doubles", Test Stub and Test Spy; Freeman and Pryce, *GOOS*, ch. 8). WorkManager's public `ForegroundUpdater` is the seam
  (L7); no production seam is added (story constraint).
- **Characterisation first, then failing first, then mutation analysis** (Feathers, *Working Effectively with Legacy Code*,
  ch. 13; Beck, *TDD by Example*, part I; DeMillo, Lipton and Sayward, 1978). Assertion messages name what is checked,
  against Assertion Roulette (Meszaros, ch. 16).

### 2.1 The fix, line by line (the decision REQ-MODERNIZATION-013 AC-8 asks for)

**Decision.** Foreground promotion through `setForeground()` is **kept** for `MANUAL`, `SKIP` and every restore, and
**dropped** for `INCOMING`, `REGULAR`, `BROADCAST_INTENT` and a run without a type tag. A refused promotion is not a failure:
the run continues as ordinary background work.

`BackupWorker.doWork()` after E-B1 and E-B2 (section 3.1):
1. `val backupType = inferBackupType()`, moved up from post-U-070 `:142` (it reads only the tags; no side effect).
2. `if (!backupType.isBackground()) { setForegroundOrContinue(createBackupForegroundInfo(), TAG, "BackupWorker") }`. The
   Boolean result is not used: nothing in the run depends on whether the notification is shown.
3. The backoff check, `val ctx`, the starting line (text unchanged) and the `try`, in today's order.

`RestoreWorker.doWork()` after E-R1: `setForegroundOrContinue(createRestoreForegroundInfo(), TAG, "RestoreWorker")` in
place of the old call, always (a restore is always started from the screen). Nothing else changes in either worker.

The helper (`service/WorkerForeground.kt`, full text in 3.3; the line numbers below are that file's):

| Line | Text | Why, for the question an expert would ask |
|------|------|--------------------------------------------|
| 39-43 | `internal suspend fun CoroutineWorker.setForegroundOrContinue(info, tag, site): Boolean` | An extension on `CoroutineWorker` reaches the worker's own `setForeground` without changing either worker's constructor or adding a field (IC-1). `internal`: visible to the two workers and to tests in the module only. The name is checked against U-074's planned `service/WorkerCancellation.kt` (`rethrowIfCancelled`, U-074 story `:64`): different file, function and facade class (`WorkerForegroundKt` vs `WorkerCancellationKt`); no name in this plan occurs anywhere under `app/` at `fd70325a` or in U-070's plan (`git grep`, `grep`) |
| 44-46 | `try { setForeground(info); return true }` | The only call of `setForeground(` in `app/src/main` after the story (IC-1). The refusal arrives here unwrapped in all three shapes WorkManager can deliver it (L5, L6) |
| 47 | `catch (e: IllegalStateException)` | The refusal's own class is **not** named in a `catch` clause: a catch type is part of the method's exception table, so on API 24 to 30 the helper's class would carry a reference to a class that does not exist (L11). `IllegalStateException` is the nearest API 1 supertype (L11). The catch is not broad in effect: lines 48 to 53 rethrow everything but the refusal |
| 48 | `if (e is CancellationException) throw e` | **First**, because `java.util.concurrent.CancellationException` (the class kotlinx's `CancellationException` aliases) is an `IllegalStateException`: a cancellation while `setForeground` is suspended (L5's `cancel` branch), or an updater that is cancelled, must end the run as cancelled (U-067, U-074). With today's shape the rethrow at line 53 would also do it; line 48 is kept so that the rule survives a reordering (story: "an equivalent mutation, known in advance"; not in the injection table) |
| 49 | `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ForegroundRefusal.isRefusal(e))` | The `SDK_INT` test comes first and short-circuits: on API 24 to 30 `ForegroundRefusal` is never touched, so it is never loaded, initialised or verified (P3). `ForegroundRefusal` is a separate class (line 62-65) whose single method holds the one `instance-of` of the API-31 class (L19). The `&&` order is what M-X1 probes |
| 50 | `Log.w(tag, "$site: foreground start not allowed; continuing without the foreground (${e.javaClass.simpleName})")` | The story's binding prefix with the site; the class name only. No throwable attached: the platform's message names the package and WorkManager's service only (BUG-029 log :20), but a one-line warning is enough to identify the event and keeps logcat free of stack traces for a run that succeeds. `Log.w` survives release builds (no `-assumenosideeffects`, L22) |
| 51 | `return false` | The run continues as ordinary background work |
| 53 | `throw e` | Everything else propagates unchanged: the library's own `IllegalStateException` (L6), `InvalidForegroundServiceTypeException` and `MissingForegroundServiceTypeException` (siblings under `ServiceStartNotAllowedException`, so the parent is not tested, L11; a BUG-016-type manifest regression stays loud), and any other `IllegalStateException`. `SecurityException` and every `Error` never enter the catch |
| 62-65 | `@RequiresApi(Build.VERSION_CODES.S) private object ForegroundRefusal { fun isRefusal(e) = e is ForegroundServiceStartNotAllowedException }` | `@RequiresApi` documents the contract for lint (`NewApi` is satisfied by the guard at line 49). `private` top-level: not visible outside the file. With `-dontoptimize` R8 neither inlines nor merges it (L22) |

**Why `IllegalStateException` is not caught broadly.** Swallowing every `IllegalStateException` would hide WorkManager's
"Calls to setForegroundAsync() must complete…" error (a worker bug), Android 14's type exceptions (a manifest regression, the
BUG-016 class of defect) and a cancellation (U-074's subject). M-F2 is the injection that shows the tests catch exactly
that widening.

**How the API 24 to 30 safety is proven** (SC-2). (a) Bytecode: the helper's class has no reference to the API-31 class
(L19), checked again by the developer in the real debug **and release** dex (I6 step 4; R8 runs only in the release
build). (b) JVM: test F13 runs the helper at SDK 24, where the class does not exist in the sandbox (L11), and an
`IllegalStateException` must arrive as itself. F13 proves only that the helper loads and rethrows on a HotSpot JVM in a
sandbox without the class; it is not evidence about ART's verifier. (c) M-X1 removes the `SDK_INT` test and records whether
F13 then fails with `NoClassDefFoundError` (informational: whether Robolectric's sandbox resolves a missing `android.*` class
through its parent class loader was not read). (d) ART: device step (vi) on the `Pixel_API_24` emulator (section 9), run by
the orchestrator: install-time verification is always read; the helper itself runs there only if the app has login
information, otherwise the step is recorded "not run: needs the owner to enter the app password".

### 2.2 Alternatives considered (not chosen)

| Option | Why not |
|--------|---------|
| `catch (e: ForegroundServiceStartNotAllowedException)` before a general catch | Puts the API-31 class into the helper's exception table (2.1, line 47) |
| Catch `ServiceStartNotAllowedException` | Swallows the Android 14 type exceptions (M-F3 shows the tests detect it) |
| Catch every `IllegalStateException` and log it | Swallows cancellation and defects (M-F2) |
| Keep promoting every type and absorb the refusal (BUG-029 option (a)) | Leaves refused automatic runs in WorkManager's foreground map, where system stops are ignored (L10, BUG-029 L4); PO-K builds "no" (ruling 68) |
| Expedited work (option (b)) | Not possible for delayed or periodic work (BUG-029 L7); rejected by ruling 60 |
| Decide by process importance | A guess at the platform's rule (BUG-029 option (d)) |
| Inline `e is ForegroundServiceStartNotAllowedException` in the helper behind the `SDK_INT` test | Works on most ART versions (P3) but leaves the unresolvable reference in the helper's class; the holder object costs two lines and removes the question |
| A production seam to make the refusal testable at SDK 29 | Forbidden by the story; P-1 shows SDK 35 runs offline (L12, L13) |

### 2.3 P-1: the test SDK level, and the test design that follows

**Result.** SDK **35** is the refusal level: it is the compile and target SDK (L24), the highest level Robolectric 4.15.1
knows (L12), its pre-instrumented jar is in the local Maven cache with a matching SHA-512 (L13), it contains all five
exception classes (L11), and JDK 25 meets its Java 17 requirement (L12). SDK **34** is the fallback (same facts) through row
D10 (F-34: two Edits of the class annotations; blobs in 3.4). **No level of 31 to 33 can run offline** (no i7 jar, L13): the
story's middle fallback does not exist on this machine, so if 35 and 34 both fail, the story's last rule applies: stop and
report before the build (row D10). Nothing may be downloaded: the I0 and I6 listings of the cache folder must be equal
(row D26).

**Design.**
- `BackupWorkerForegroundTest` and `RestoreWorkerForegroundTest` run at `@Config(sdk = [35])`; `BackupWorkerForegroundApi24Test`
  at `@Config(sdk = [24])` (cached, L13). The API 24 class lives in the same file but names no class above API 24, and the
  shared top-level declarations name none either (N3; JVM verification of a class that constructs an API-31 exception would
  need that class, which the SDK 24 sandbox does not have).
- The workers are built with `TestListenableWorkerBuilder`, a test-local `WorkerFactory` (the constructors are unchanged,
  IC-1), the tags production uses (class name plus type, L3), and the updater the test chooses (L7).
- AC-1 and AC-2 reuse U-070's `LateSmsHarness`, `ProviderDouble`, `ContactsProviderDouble`, `RecordingTransport` and
  `SmsFixture` as built, unedited (story Terms). Fixture values are not personal: sender `U075SENDER` (alphanumeric, so no
  hexadecimal id or decimal date can contain it), body `u075 body kiwi`, folders `u075-sms-folder` and `u075-call-folder`
  (set through the `imap_folder` and `imap_folder_calllog` preferences, `DataType.java:16-18`, `:66-67`), account
  `backup-owner` (`SmsFixture.OWNER`). The sender is registered as a known contact so that the existing verbose line of
  L18 is not taken; AC-2's check then runs literally over every `ShadowLog` entry under every tag (SC-4).
- AC-3 and AC-5 use a transport factory that records each `create()` and throws `MailException`, the form of
  `BackupWorkerIntegrationTest.doWork_mailExceptionFromTransportFactory_returnsRetry` (`:131-163`): today's mapping gives
  `Result.retry()` (L1).
- AC-4 records, in one ordered list, the foreground request (the counting updater), each `create()` (the factory) and each
  SMS provider query (`ProviderDouble.beforeQuery`), so "before the transport and before any provider query" is an order
  assertion, not an inference.
- AC-6 uses a test-local factory over the real `RestoreWorker` constructor with `InMemoryCheckpointStore` and
  `RestoreInsertInterceptor.NoOp` (L17) and the same recording, failing transport factory; the expected result is
  `Result.retry()` (L2).
- `ShadowLog.clear()` in every `@Before` and between loop cases.

### 2.4 How a test makes `setForeground` refuse, and why the tests are honest (item 3)

Production delivers the refusal through a future that its task fails on the task thread (L6), normally after `doWork` has
suspended in `setForeground` (L5's pending branch). The story's **throwing updater** throws from `setForegroundAsync`
itself, which leaves `setForeground` before any future exists (L5). All three shapes reach `doWork` as
`ForegroundServiceStartNotAllowedException` itself (L5). The tests therefore cover all three:

| Shape | Test | WorkManager code it passes through |
|-------|------|------------------------------------|
| Throw from the updater (the story's binding definition) | F1, F3, F7, R2 | `ListenableWorker.setForegroundAsync` → straight out of `setForeground` |
| Future already failed | F4 (Guava `Futures.immediateFailedFuture`) | `isDone` → `get()` → `ExecutionException` unwrapped (exception table `25-35`) |
| Future failed after `doWork` suspended (production's usual branch) | F5 (Guava `SettableFuture`, failed after `async(start = UNDISPATCHED)` shows the run suspended before `create()`) | `ListenableFutureKt$await$2$1` resumes with the cause |

Guava is on the unit-test compile classpath (Truth 1.4.4's POM, compile scope; Guava 33's POM brings `failureaccess`,
which `SettableFuture`'s supertype needs, L25 and the scratch compile). The tests compare the class and the message of what
`doWork` throws, never identity (P1).

### 2.5 P-2 / AC-7: what a refused manual backup or restore does (recorded reading)

Read in WorkManager 2.9.1 (L5, L6, L9, L10), `MainViewModel.kt` and `StatusPreference.java` at `fd70325a`:
1. **The run continues and its result is applied.** The helper returns `false`; `doWork` returns its own `Result`
   (`Success`, `retry()` on `MailException`, or a failure reason). `CoroutineWorker` sets it on the worker's future and
   `WorkerWrapper` resolves the work by that result (BUG-029 Description item 5). When the worker ends, `Processor.onExecuted`
   removes the work from the foreground map and the other maps (`cleanUpWorkerUnsafe`, L10). **The work cannot stay
   `RUNNING` after `doWork` returns** on that account: the stop condition of AC-7 does not occur.
2. **No progress notification is shown for it.** The refusal is thrown inside `startForeground` (offset 70) before
   `WorkForegroundUpdater` posts the notification (offset 96), and the exception skips the rest of the task (L6).
3. **Its progress still reaches the status screen.** `setProgress` goes through the progress updater, independent of the
   foreground (L9). `MainViewModel.observeBackupWork` collects `getWorkInfosForUniqueWorkFlow` and maps a `RUNNING` work's
   progress to a `BackupState` (`MainViewModel.kt:198-215`, `:245-268`); `StatusPreference.backupStateChanged` suppresses
   only background types except errors (`StatusPreference.java:234`); `MANUAL` and `SKIP` are not background types (L3).
   The restore path is the same (`MainViewModel.kt:222-239`, `:285-305`).
4. **WorkManager keeps the work in its foreground map until it ends.** `Processor.startForeground` moved it there before the
   start was attempted, so a JobScheduler stop is ignored ("Ignored stopWork…", L10) and the `ProcessorForegroundLck` wake
   lock is held until the map is empty (BUG-029 L3). This now concerns only a refused manual backup, Skip or restore. A
   user's cancel still stops such a run: `stopAndCancelWork` removes it from the foreground map and interrupts it (L27).
5. **Not verified** (stays as BUG-029 Not verified 5): what the platform does with the process when JobScheduler stops the
   job while such a run goes on (freezing, a kill), and how 2.9.1 recovers a `RUNNING` work spec after a process death. The
   watermark (BUG-010) and the restore checkpoint (U-016) make either case lossless.

The developer copies this reading into the implementation log for the story's Implementation Notes (I7).

### 2.6 Edge cases (behaviour after the fix)

1. A manual backup's retry (30 s backoff) that starts after the user left: refused, one warning, the run continues (the
   main user-started beneficiary).
2. A manual backup or restore at attempt 4 or more: the helper runs first (and may warn), then the backoff cap returns
   `failure("backoff_cap_exceeded")`, as today.
3. An automatic backup on any Android version no longer enters the foreground, so it runs as an ordinary job and the
   system's stops are honoured (BUG-029 L6): the job time limit (on Android 12 and later when the system is busy, BUG-029
   L5, read; on Android 11 and earlier at 10 minutes whatever the load, recalled, not verified: on the owner's Android 11
   phone a long automatic run changes from foreground to ordinary work and is now cut at 10 minutes), Doze or another
   device-state change, a lost network constraint, and on Android 16 the job runtime quota (recalled, not verified). Before
   this story the foreground map ignored such stops (L10). With `MAX_ITEMS_PER_SYNC` at its default `-1` (L28) an automatic
   run after a long gap is not bounded in size. A stopped run is rescheduled and continues above the watermark (BUG-010),
   so nothing confirmed is lost. **Until U-074** the stop reaches `doWork`'s broad catch (`BackupWorker.kt:168-171`): one
   ERROR line "BackupWorker: unrecoverable error" with the job's `CancellationException`, and a `Result.failure` that
   WorkManager ignores for interrupted work; section 9 reads that pair as "stopped by the system", not as a fail, and tells
   it from a real error by the next line (`ac12_run.py`'s `unrecoverable-cancel`, 14.7). When a job **starts** (Doze
   windows, standby bucket, quota) is decided by JobScheduler before `doWork` and does not change.
4. Cancellation while `setForeground` is suspended: kotlinx cancels the continuation; the helper rethrows (line 48).
5. A refusal that arrives after the job was cancelled (a race): the helper returns `false` and the run reaches the next
   cooperative check; that is U-074's subject (hand-over item 2).
6. A missing `FOREGROUND_SERVICE` permission (`SecurityException`) or a manifest type regression (Android 14 type
   exceptions) propagates and fails the run loudly, as today.
7. Android 15's six-hour `dataSync` budget used up: whether the next start throws the refusal is BUG-029 Not verified 4; if
   it does, the run continues without the notification, which is the right outcome.
8. WorkManager 2.9.1 already has (and 2.12.0 keeps) a second refusal point inside
   `SystemForegroundService$Api31Impl.startForeground`, which catches the refusal and `SecurityException` and logs only
   "Unable to start foreground service" (L29). If it is ever reached, the worker is not told and continues, as with the
   caught case but without the app's warning; section 9 counts that line (`svc-refusal`).
9. The warning is written at most once per run (one call per run).
10. `getForegroundInfo()` is not overridden and needs no override: no request is expedited, WorkManager calls it below API
    31 only for expedited work, and 2.9.1's default throws "Not implemented" (L28). A later story that expedites a request
    must add it (hand-over item 6). Android 15's rule that a `BOOT_COMPLETED` receiver may not start a `dataSync` service is
    moot here: the boot backup is `REGULAR` and never asks, and WorkManager starts its service from a job, not from the
    receiver (BUG-029 Not verified 3).
11. Whether holding the default-SMS role exempts a restore from the background-start rule was not read; the helper is
    correct either way.

### 2.7 Interactions (exact)

- **U-074** (`service/WorkerCancellation.kt`, `rethrowIfCancelled`; its IC-1 searches `catch (e: IllegalStateException)` over
  the workers and the classes they call). It will find `WorkerForeground.kt:47` `    } catch (e: IllegalStateException) {`.
  Status for its table: **left on purpose**: its first statement rethrows a `CancellationException` (line 48); it continues
  only on `ForegroundServiceStartNotAllowedException` from API 31 (line 49-51); everything else is rethrown (line 53). It needs
  no `rethrowIfCancelled`. Edge case 5 is for U-074 to decide. (IC-2; hand-over item 2.)
- **U-071** (mutation anchor M-1 in `BackupWorker.kt`, the loop check `                currentCoroutineContext().ensureActive()`).
  This story changes `BackupWorker.kt` only at new lines `:119-128` (old `:119-123`) and removes old `:142` (`-U0` hunks
  `@@ -119,5 +119,10 @@` and `@@ -142 +146,0 @@`), and `RestoreWorker.kt` only at `:144-148` (`@@ -144,5 +144,5 @@`). M-1
  moves down by 4 lines (simulated `:339` → `:343`; the simulated start blob equals U-070's built blob, coordinator's fact
3) and stays unique (L20). U-071's I0 drift check (its row D6, text counts)
  reads it unchanged. U-071's plan already names this story and the new file (its plan `:39-46`).
- **U-073** (WorkManager 2.12.0): the tests use `TestForegroundUpdater` and `setForegroundUpdater` (P2); the bug read
  L1 to L4 in 2.12.0 (`CoroutineWorker.kt:105-117`). Hand-over item 6.
- **The BUG-026/BUG-027 scheduler story and BUG-025's story**: no shared file; this story changes no scheduling (AC-8).
- **U-070**: its fixtures are reused unedited; its `BackupWorker.kt` mutations M-L11, M-L20, M-L22 are re-run (I5).
- **Known stale comments** (existing text, not edited; AC-8 forbids editing existing tests and lines outside `doWork`):
  `BackupWorkerIntegrationTest.kt:40` ("Foreground notification setup (always runs in doWork)") and `:76` ("required for
  setForeground() in doWork") become partly stale for automatic types; the KDoc of `createBackupForegroundInfo()`
  (`BackupWorker.kt:510` at `fd70325a`, post-U-075 `:529`, "used by setForeground() at the start of doWork()") is now true
  for user-started runs only. `RestoreWorker.kt:683`, `RestoreWorkerIntegrationTest.kt:91`, `MainActivity.java:510` and
  `AndroidManifest.xml:116-118` stay true. Hand-over item 3.

## 3. Exact Changes

Paths: `$S` = `app/src/main/java/com/zegoggles/smssync`, `$TS` = `app/src/test/java/com/zegoggles/smssync`. Every Edit is
made with the Edit tool, matched by text; each `old_string` occurs exactly once at `<start>` (I0 `oldtext_check.py start`),
in a region U-070 does not edit (L1, L2). Working-tree line endings of the two workers are CRLF (`core.autocrlf true`,
U-069 L2); the Edit tool keeps them; `git hash-object` gives the LF blob.

### 3.1 `$S/service/BackupWorker.kt` (two Edits, I3 step 2)

**E-B1** (old `:119-123`; not edited by U-070). `old_string`:
```
        // U-049 AC-2: Set foreground notification early in doWork() so the backup shows
        // a foreground-service notification on Android 12+ without ANR risk.
        // Uses the same notification channel (App.CHANNEL_ID) and content that
        // SmsBackupService.notifyAboutBackup() previously provided.
        setForeground(createBackupForegroundInfo())
```
`new_string`:
```kotlin
        // U-075 (BUG-029): the type is inferred first, and only a run the user started from the
        // screen (MANUAL, SKIP) asks for the foreground, as the legacy SmsBackupService did.
        // Android 12+ refuses a foreground-service start while the app is in the background, so
        // automatic backups (INCOMING, REGULAR, BROADCAST_INTENT) run as ordinary background work
        // without the notification, and a refused user-started run continues without it
        // (WorkerForeground.kt). The notification itself is unchanged (App.CHANNEL_ID, id 1, dataSync).
        val backupType = inferBackupType()
        if (!backupType.isBackground()) {
            setForegroundOrContinue(createBackupForegroundInfo(), TAG, "BackupWorker")
        }
```

**E-B2** (old `:141-142`; not edited by U-070; unique before and after E-B1). `old_string`:
```
        // no more manual construction here.
        val backupType = inferBackupType()
```
`new_string`:
```
        // no more manual construction here.
```

### 3.2 `$S/service/RestoreWorker.kt` (one Edit, I3 step 2)

**E-R1** (old `:144-148`; not edited by U-070). `old_string`:
```
        // U-049 AC-2: Set foreground notification early in doWork() so the restore shows
        // a foreground-service notification on Android 12+ without ANR risk.
        // Uses the same notification channel (App.CHANNEL_ID) and content that
        // SmsRestoreService.restoreStateChanged() previously provided via startForeground().
        setForeground(createRestoreForegroundInfo())
```
`new_string`:
```kotlin
        // U-075 (BUG-029): a restore is always started from the screen, so it always asks for the
        // foreground (the notification is unchanged: App.CHANNEL_ID, id 2, dataSync). Android 12+
        // refuses the start while the app is in the background, for example on a retry after the
        // user left; the restore then continues without the notification (WorkerForeground.kt).
        setForegroundOrContinue(createRestoreForegroundInfo(), TAG, "RestoreWorker")
```

### 3.3 New file `$S/service/WorkerForeground.kt` (Write tool, I3 step 1; blob `0782f85f1b3815790ac21c87b454fe1f972a34c1`, 65 lines, LF)

```kotlin
/*
 * Copyright (c) 2024 SMS Backup+
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zegoggles.smssync.service

import android.app.ForegroundServiceStartNotAllowedException
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import java.util.concurrent.CancellationException

/**
 * U-075 (BUG-029): asks WorkManager to run this worker as a foreground service with [info].
 * Returns true when the request was accepted. When Android refuses the start because the app
 * is in the background (Android 12 and later, ForegroundServiceStartNotAllowedException), it
 * writes one warning with [tag], naming [site] and the exception's class only, and returns
 * false: the run continues as ordinary background work, without the notification.
 *
 * Only that refusal is absorbed. A cancellation (java.util.concurrent.CancellationException,
 * itself an IllegalStateException) is rethrown first. The Android 14 foreground-service type
 * errors (siblings of the refusal under ServiceStartNotAllowedException, which is therefore
 * not caught; a manifest regression such as BUG-016), WorkManager's own
 * IllegalStateException, SecurityException and every Error propagate unchanged.
 */
internal suspend fun CoroutineWorker.setForegroundOrContinue(
    info: ForegroundInfo,
    tag: String,
    site: String
): Boolean {
    try {
        setForeground(info)
        return true
    } catch (e: IllegalStateException) {
        if (e is CancellationException) throw e
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ForegroundRefusal.isRefusal(e)) {
            Log.w(tag, "$site: foreground start not allowed; continuing without the foreground (${e.javaClass.simpleName})")
            return false
        }
        throw e
    }
}

/**
 * The only reference to an API 31 class. It is kept out of the function above so that on
 * API 24 to 30, where ForegroundServiceStartNotAllowedException does not exist, this class is
 * never loaded or verified: the SDK_INT test is the only way in.
 */
@RequiresApi(Build.VERSION_CODES.S)
private object ForegroundRefusal {
    fun isRefusal(e: IllegalStateException): Boolean = e is ForegroundServiceStartNotAllowedException
}
```

The final warning text (story Terms; the prefix is binding): `BackupWorker: foreground start not allowed; continuing without
the foreground (ForegroundServiceStartNotAllowedException)`, and the same with `RestoreWorker:`.

### 3.4 New test files (Write tool, I1 step 1)

**T1** `$TS/service/BackupWorkerForegroundTest.kt` — blob `3e3ea62b56e25158bb73f993f8a5f2f44b3825b6` (517 lines, 13 lines
containing `@Test`, LF); under F-34 (row D10) blob `0145ee38147424dd78928d1722955fec245e1911`.

```kotlin
package com.zegoggles.smssync.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.InvalidForegroundServiceTypeException
import android.app.MissingForegroundServiceTypeException
import android.content.Context
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.ForegroundInfo
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestForegroundUpdater
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.zegoggles.smssync.App
import com.zegoggles.smssync.contacts.ContactAccessor
import com.zegoggles.smssync.di.MailTransportFactory
import com.zegoggles.smssync.mail.DataType
import com.zegoggles.smssync.mail.transport.MailException
import com.zegoggles.smssync.mail.transport.MailTransport
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.Preferences
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.util.UUID
import java.util.concurrent.CancellationException

/*
 * U-075 (BUG-029): how BackupWorker.doWork asks for the foreground, and what it does when the
 * request is refused. The refusal (android.app.ForegroundServiceStartNotAllowedException) exists
 * from API 31 and the foreground-service type exceptions from API 34, so the main class runs at
 * SDK 35 (plan P-1). The declarations at the top level of this file name no class above API 24:
 * the API 24 class at the end of the file loads them in a sandbox where those classes do not exist.
 */

/** U-075: the order of what a run did: foreground requests, transport creations, provider queries. */
internal class ForegroundCallLog {
    val events = mutableListOf<String>()
    val infos = mutableListOf<ForegroundInfo>()
    val foregroundCalls: Int get() = infos.size

    fun record(info: ForegroundInfo) {
        events.add(FOREGROUND)
        infos.add(info)
    }

    companion object {
        const val FOREGROUND = "foreground"
        const val CREATE = "create"
        const val QUERY = "query"
    }
}

/** U-075: records each request and completes it at once, as the builder's default updater does. */
internal class CountingForegroundUpdater(private val calls: ForegroundCallLog) : ForegroundUpdater {
    private val delegate = TestForegroundUpdater()

    override fun setForegroundAsync(context: Context, id: UUID, foregroundInfo: ForegroundInfo): ListenableFuture<Void> {
        calls.record(foregroundInfo)
        return delegate.setForegroundAsync(context, id, foregroundInfo)
    }
}

/** U-075: records the request, then throws [error]'s result from the call itself. */
internal class ThrowingForegroundUpdater(
    private val calls: ForegroundCallLog,
    private val error: () -> Throwable
) : ForegroundUpdater {
    override fun setForegroundAsync(context: Context, id: UUID, foregroundInfo: ForegroundInfo): ListenableFuture<Void> {
        calls.record(foregroundInfo)
        throw error()
    }
}

/** U-075: records the request and returns a future that has already failed with [error]'s result. */
internal class FailedFutureForegroundUpdater(
    private val calls: ForegroundCallLog,
    private val error: () -> Throwable
) : ForegroundUpdater {
    override fun setForegroundAsync(context: Context, id: UUID, foregroundInfo: ForegroundInfo): ListenableFuture<Void> {
        calls.record(foregroundInfo)
        return Futures.immediateFailedFuture<Void>(error())
    }
}

/** U-075: records the request and returns [future], which the test fails later (WorkManager's own form). */
internal class PendingFutureForegroundUpdater(private val calls: ForegroundCallLog) : ForegroundUpdater {
    val future: SettableFuture<Void> = SettableFuture.create<Void>()

    override fun setForegroundAsync(context: Context, id: UUID, foregroundInfo: ForegroundInfo): ListenableFuture<Void> {
        calls.record(foregroundInfo)
        return future
    }
}

/** U-075: log reading shared by the two worker test files. */
internal object ForegroundTestLog {
    const val TAG = "SMSBackup+"
    const val BACKUP_PREFIX = "BackupWorker: foreground start not allowed; continuing without the foreground"
    const val RESTORE_PREFIX = "RestoreWorker: foreground start not allowed; continuing without the foreground"

    /** Every entry, under any tag and at any level, whose text starts with [prefix]. */
    fun withPrefix(prefix: String): List<ShadowLog.LogItem> =
        ShadowLog.getLogs().filter { it.msg?.startsWith(prefix) == true }

    fun errors(): List<ShadowLog.LogItem> = ShadowLog.getLogsForTag(TAG).filter { it.type == Log.ERROR }

    fun hasDebugLine(text: String): Boolean = ShadowLog.getLogsForTag(TAG).any { it.type == Log.DEBUG && it.msg == text }

    /** The warning of the story's Terms, exactly once, with the refusal's simple class name and no throwable. */
    fun assertRefusalLoggedOnce(prefix: String, simpleClassName: String) {
        val hits = withPrefix(prefix)
        assertWithMessage("entries whose text starts with the refusal warning's prefix").that(hits).hasSize(1)
        val warning = hits.single()
        assertWithMessage("level of the refusal warning").that(warning.type).isEqualTo(Log.WARN)
        assertWithMessage("tag of the refusal warning").that(warning.tag).isEqualTo(TAG)
        assertWithMessage("text of the refusal warning").that(warning.msg).isEqualTo("$prefix ($simpleClassName)")
        assertWithMessage("throwable of the refusal warning").that(warning.throwable).isNull()
        assertWithMessage("entries at Log.ERROR with tag $TAG").that(errors()).isEmpty()
    }
}

/** U-075: one worker built for one run, with the updater and transport factory the test chooses. */
internal fun foregroundTestBackupWorker(
    context: Context,
    tags: List<String>,
    updater: ForegroundUpdater?,
    transportFactory: MailTransportFactory
): BackupWorker {
    val factory = object : WorkerFactory() {
        override fun createWorker(appContext: Context, workerClassName: String,
                                  workerParameters: WorkerParameters): ListenableWorker? {
            if (workerClassName != BackupWorker::class.java.name) return null
            val prefs = Preferences(appContext)
            val authPrefs = AuthPreferences(appContext)
            return BackupWorker(appContext, workerParameters, prefs, authPrefs, transportFactory,
                ContactAccessor(), WorkerEngineFactory(appContext, prefs, authPrefs))
        }
    }
    val builder = TestListenableWorkerBuilder<BackupWorker>(context)
        .setWorkerFactory(factory)
        .setTags(tags)
    if (updater != null) builder.setForegroundUpdater(updater)
    return builder.build()
}

/** U-075: a transport factory that records each create() and then fails, as BackupWorkerIntegrationTest's does. */
internal fun failingTransportFactory(calls: ForegroundCallLog): MailTransportFactory = MailTransportFactory {
    calls.events.add(ForegroundCallLog.CREATE)
    throw MailException("U-075: no transport in this test")
}

/** U-075: a transport factory that records each create() and returns [transport]. */
internal fun recordingTransportFactory(calls: ForegroundCallLog, transport: MailTransport): MailTransportFactory =
    MailTransportFactory {
        calls.events.add(ForegroundCallLog.CREATE)
        transport
    }

/** U-075: the tags WorkManagerScheduler gives a run: the worker's class name and the type. */
internal fun foregroundTestTags(type: String?): List<String> =
    listOfNotNull(BackupWorker::class.java.name, type)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupWorkerForegroundTest {

    private lateinit var context: Context
    private lateinit var harness: LateSmsHarness

    private class AutomaticCase(val label: String, val tag: String?, val logged: String)

    companion object {
        /** Fixture values; none is personal data. The sender is alphanumeric so no id or date can contain it. */
        const val ADDRESS = "U075SENDER"
        const val BODY = "u075 body kiwi"
        const val SMS_FOLDER = "u075-sms-folder"
        const val CALL_FOLDER = "u075-call-folder"
        private val AUTOMATIC_CASES = listOf(
            AutomaticCase("INCOMING", BackupType.INCOMING.name, "INCOMING"),
            AutomaticCase("REGULAR", BackupType.REGULAR.name, "REGULAR"),
            AutomaticCase("BROADCAST_INTENT tag", BackupType.BROADCAST_INTENT.name, "BROADCAST_INTENT"),
            AutomaticCase("no type tag", null, "BROADCAST_INTENT")
        )
    }

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
        ShadowLog.clear()
        harness = LateSmsHarness(context)
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString("imap_folder", SMS_FOLDER)
            .putString("imap_folder_calllog", CALL_FOLDER)
            .commit()
        // A known contact, so that PersonLookup does not take its verbose "unknown address" branch
        // (an existing line outside this story, ruling 39 of the decision appendix).
        harness.registerContacts().contacts[ADDRESS] = 7L to 70L
        harness.sms.add(date = SmsFixture.D, address = ADDRESS, body = BODY)
    }

    // ---------------------------------------------------------------------------------------
    // AC-1, AC-2: a user-started backup whose foreground start is refused still runs
    // ---------------------------------------------------------------------------------------

    @Test
    fun manual_foregroundRefused_continuesAndBacksUp() {
        val calls = ForegroundCallLog()
        val transport = RecordingTransport()
        val updater = ThrowingForegroundUpdater(calls) { ForegroundServiceStartNotAllowedException("test") }

        val result = runBlocking {
            foregroundTestBackupWorker(context, foregroundTestTags(BackupType.MANUAL.name), updater,
                recordingTransportFactory(calls, transport)).doWork()
        }

        assertBackedUpOnce(result, transport)
        ForegroundTestLog.assertRefusalLoggedOnce(ForegroundTestLog.BACKUP_PREFIX, "ForegroundServiceStartNotAllowedException")
        assertWithMessage("the starting line").that(
            ForegroundTestLog.hasDebugLine("BackupWorker.doWork: starting backup, type=MANUAL, attempt=0")).isTrue()
        assertNoFixtureValueLogged()
    }

    /** AC-1 control, and the probe that U-070's fixtures run at this SDK level (plan P-1). */
    @Test
    fun manual_foregroundAllowed_backsUp_control() {
        val calls = ForegroundCallLog()
        val transport = RecordingTransport()

        val result = runBlocking {
            foregroundTestBackupWorker(context, foregroundTestTags(BackupType.MANUAL.name), null,
                recordingTransportFactory(calls, transport)).doWork()
        }

        assertBackedUpOnce(result, transport)
        assertWithMessage("refusal warnings").that(ForegroundTestLog.withPrefix(ForegroundTestLog.BACKUP_PREFIX)).isEmpty()
        assertWithMessage("entries at Log.ERROR").that(ForegroundTestLog.errors()).isEmpty()
        assertNoFixtureValueLogged()
    }

    @Test
    fun skip_foregroundRefused_sameOutcomeAsAllowed() {
        val allowed = runBlocking {
            foregroundTestBackupWorker(context, foregroundTestTags(BackupType.SKIP.name), null,
                recordingTransportFactory(ForegroundCallLog(), RecordingTransport())).doWork()
        }
        val allowedWatermark = harness.watermark()
        assertWithMessage("control: the SMS watermark after an allowed Skip").that(allowedWatermark).isEqualTo(SmsFixture.D)
        harness.dataTypePreferences.setMaxSyncedDate(DataType.SMS, DataType.Defaults.MAX_SYNCED_DATE)
        ShadowLog.clear()

        val calls = ForegroundCallLog()
        val refused = runBlocking {
            foregroundTestBackupWorker(context, foregroundTestTags(BackupType.SKIP.name),
                ThrowingForegroundUpdater(calls) { ForegroundServiceStartNotAllowedException("test") },
                recordingTransportFactory(calls, RecordingTransport())).doWork()
        }

        assertWithMessage("Result of a refused Skip against an allowed one").that(refused).isEqualTo(allowed)
        assertWithMessage("SMS watermark of a refused Skip against an allowed one").that(harness.watermark()).isEqualTo(allowedWatermark)
        ForegroundTestLog.assertRefusalLoggedOnce(ForegroundTestLog.BACKUP_PREFIX, "ForegroundServiceStartNotAllowedException")
        assertWithMessage("the starting line").that(
            ForegroundTestLog.hasDebugLine("BackupWorker.doWork: starting backup, type=SKIP, attempt=0")).isTrue()
        assertNoFixtureValueLogged()
    }

    /** The refusal as WorkManager's own updater delivers it when the future has completed already. */
    @Test
    fun manual_foregroundRefusedThroughFailedFuture_continuesAndBacksUp() {
        val calls = ForegroundCallLog()
        val transport = RecordingTransport()
        val updater = FailedFutureForegroundUpdater(calls) { ForegroundServiceStartNotAllowedException("test") }

        val result = runBlocking {
            foregroundTestBackupWorker(context, foregroundTestTags(BackupType.MANUAL.name), updater,
                recordingTransportFactory(calls, transport)).doWork()
        }

        assertBackedUpOnce(result, transport)
        ForegroundTestLog.assertRefusalLoggedOnce(ForegroundTestLog.BACKUP_PREFIX, "ForegroundServiceStartNotAllowedException")
    }

    /** The refusal as WorkManager's own updater delivers it in production: the future fails after doWork suspended. */
    @Test
    fun manual_foregroundRefusedThroughPendingFuture_continuesAndBacksUp() = runBlocking<Unit> {
        val calls = ForegroundCallLog()
        val transport = RecordingTransport()
        val updater = PendingFutureForegroundUpdater(calls)

        val run = async(start = CoroutineStart.UNDISPATCHED) {
            foregroundTestBackupWorker(context, foregroundTestTags(BackupType.MANUAL.name), updater,
                recordingTransportFactory(calls, transport)).doWork()
        }
        assertWithMessage("doWork() is suspended in its foreground request, before create()").that(
            run.isActive && calls.foregroundCalls == 1 && !calls.events.contains(ForegroundCallLog.CREATE)).isTrue()
        updater.future.setException(ForegroundServiceStartNotAllowedException("test"))
        val result = run.await()

        assertBackedUpOnce(result, transport)
        ForegroundTestLog.assertRefusalLoggedOnce(ForegroundTestLog.BACKUP_PREFIX, "ForegroundServiceStartNotAllowedException")
    }

    // ---------------------------------------------------------------------------------------
    // AC-3: automatic backups do not ask for the foreground
    // ---------------------------------------------------------------------------------------

    @Test
    fun automaticTypes_doNotRequestForeground() {
        for (case in AUTOMATIC_CASES) {
            ShadowLog.clear()
            val calls = ForegroundCallLog()

            val result = runBlocking {
                foregroundTestBackupWorker(context, foregroundTestTags(case.tag), CountingForegroundUpdater(calls),
                    failingTransportFactory(calls)).doWork()
            }

            assertWithMessage("${case.label}: foreground requests").that(calls.foregroundCalls).isEqualTo(0)
            assertWithMessage("${case.label}: transport factory create() calls").that(
                calls.events.count { it == ForegroundCallLog.CREATE }).isEqualTo(1)
            assertWithMessage("${case.label}: the starting line").that(ForegroundTestLog.hasDebugLine(
                "BackupWorker.doWork: starting backup, type=${case.logged}, attempt=0")).isTrue()
            assertWithMessage("${case.label}: Result").that(result).isEqualTo(ListenableWorker.Result.retry())
        }
    }

    @Test
    fun automaticTypes_foregroundRefused_runAsToday() {
        for (case in AUTOMATIC_CASES) {
            ShadowLog.clear()
            val calls = ForegroundCallLog()
            val updater = ThrowingForegroundUpdater(calls) { ForegroundServiceStartNotAllowedException("test") }

            val result = runBlocking {
                foregroundTestBackupWorker(context, foregroundTestTags(case.tag), updater,
                    failingTransportFactory(calls)).doWork()
            }

            assertWithMessage("${case.label}: Result").that(result).isEqualTo(ListenableWorker.Result.retry())
            assertWithMessage("${case.label}: refusal warnings").that(
                ForegroundTestLog.withPrefix(ForegroundTestLog.BACKUP_PREFIX)).isEmpty()
            assertWithMessage("${case.label}: foreground requests").that(calls.foregroundCalls).isEqualTo(0)
        }
    }

    // ---------------------------------------------------------------------------------------
    // AC-4: a user-started backup still asks once, first, with today's notification
    // ---------------------------------------------------------------------------------------

    @Test
    fun userStartedTypes_requestForegroundOnce() {
        for (type in listOf(BackupType.MANUAL, BackupType.SKIP)) {
            val calls = ForegroundCallLog()
            harness.sms.beforeQuery = { calls.events.add(ForegroundCallLog.QUERY) }

            val result = runBlocking {
                foregroundTestBackupWorker(context, foregroundTestTags(type.name), CountingForegroundUpdater(calls),
                    recordingTransportFactory(calls, RecordingTransport())).doWork()
            }

            assertWithMessage("$type: Result").that(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertWithMessage("$type: foreground requests").that(calls.foregroundCalls).isEqualTo(1)
            assertWithMessage("$type: first event of the run").that(calls.events.first()).isEqualTo(ForegroundCallLog.FOREGROUND)
            assertWithMessage("$type: create() after the request").that(calls.events.indexOf(ForegroundCallLog.CREATE)).isGreaterThan(0)
            assertWithMessage("$type: provider queries made").that(calls.events.count { it == ForegroundCallLog.QUERY }).isAtLeast(1)
            assertWithMessage("$type: first provider query after the request").that(
                calls.events.indexOf(ForegroundCallLog.QUERY)).isGreaterThan(0)
            val info = calls.infos.single()
            assertWithMessage("$type: notification id").that(info.notificationId).isEqualTo(1)
            assertWithMessage("$type: notification channel").that(info.notification.channelId).isEqualTo(App.CHANNEL_ID)
            assertWithMessage("$type: foreground-service type").that(info.foregroundServiceType).isEqualTo(
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }
    }

    // ---------------------------------------------------------------------------------------
    // AC-5: nothing but the refusal is swallowed
    // ---------------------------------------------------------------------------------------

    @Test
    fun foreground_otherIllegalState_propagates() {
        assertForegroundErrorPropagates(context, harness, "IllegalStateException") { IllegalStateException("x") }
    }

    @Test
    fun foreground_typeException_propagates() {
        assertForegroundErrorPropagates(context, harness, "InvalidForegroundServiceTypeException") {
            InvalidForegroundServiceTypeException("x")
        }
        assertForegroundErrorPropagates(context, harness, "MissingForegroundServiceTypeException") {
            MissingForegroundServiceTypeException("x")
        }
    }

    @Test
    fun foreground_securityException_propagates() {
        assertForegroundErrorPropagates(context, harness, "SecurityException") { SecurityException("x") }
    }

    @Test
    fun foreground_cancelled_endsCancelled() = runBlocking<Unit> {
        val calls = ForegroundCallLog()
        harness.sms.beforeQuery = { calls.events.add(ForegroundCallLog.QUERY) }
        lateinit var workJob: Job
        var thrown: Throwable? = null
        var result: ListenableWorker.Result? = null
        val worker = foregroundTestBackupWorker(context, foregroundTestTags(BackupType.MANUAL.name),
            ThrowingForegroundUpdater(calls) {
                workJob.cancel()
                CancellationException("test")
            },
            failingTransportFactory(calls))

        workJob = launch(start = CoroutineStart.LAZY) {
            try {
                result = worker.doWork()
            } catch (t: Throwable) {
                thrown = t
                throw t
            }
        }
        workJob.join()

        assertWithMessage("the job is cancelled").that(workJob.isCancelled).isTrue()
        assertWithMessage("doWork() ends by throwing a CancellationException").that(thrown).isInstanceOf(
            CancellationException::class.java)
        assertWithMessage("no Result is returned").that(result).isNull()
        assertWithMessage("refusal warnings").that(ForegroundTestLog.withPrefix(ForegroundTestLog.BACKUP_PREFIX)).isEmpty()
        assertWithMessage("what the run did (no create(), no provider query)").that(calls.events).containsExactly(
            ForegroundCallLog.FOREGROUND)
    }

    // ---------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------

    private fun assertBackedUpOnce(result: ListenableWorker.Result, transport: RecordingTransport) {
        assertWithMessage("Result").that(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
        assertWithMessage("backed_up in the output").that(
            (result as ListenableWorker.Result.Success).outputData.getInt(BackupWorker.PROGRESS_KEY_BACKED_UP, -1)).isEqualTo(1)
        assertWithMessage("appendMessages calls").that(transport.count("appendMessages")).isEqualTo(1)
        assertWithMessage("appended messages").that(transport.appended).hasSize(1)
        assertWithMessage("SMS watermark (the date the append confirmed)").that(harness.watermark()).isEqualTo(SmsFixture.D)
    }

    /** AC-2: no entry under any tag carries the row's body or address, the account name or a folder name. */
    private fun assertNoFixtureValueLogged() {
        val values = listOf(BODY, ADDRESS, SmsFixture.OWNER, SMS_FOLDER, CALL_FOLDER)
        for (item in ShadowLog.getLogs()) {
            val text = (item.msg ?: "") + " " + (item.throwable?.message ?: "")
            for (value in values) {
                assertWithMessage("a log entry with tag ${item.tag} carries a fixture value of the row, the account or a folder")
                    .that(text.contains(value)).isFalse()
            }
        }
    }
}

/**
 * U-075 (AC-5 (a) and the class-loading rule of the helper): under ART on API 24 to 30 the refusal's
 * class does not exist. At SDK 24 an IllegalStateException from the foreground request must reach the
 * caller as itself, not as a NoClassDefFoundError. This class names no class above API 24.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class BackupWorkerForegroundApi24Test {

    @Test
    fun manual_atApi24_otherIllegalState_propagatesWithoutLinkageError() {
        @Suppress("DEPRECATION")
        val context: Context = RuntimeEnvironment.application
        ShadowLog.clear()
        val harness = LateSmsHarness(context)
        assertForegroundErrorPropagates(context, harness, "IllegalStateException at API 24") { IllegalStateException("x") }
    }
}

/** U-075 (AC-5): a MANUAL run whose foreground request throws [error]'s result throws it, logs no warning and does nothing else. */
internal fun assertForegroundErrorPropagates(
    context: Context,
    harness: LateSmsHarness,
    label: String,
    error: () -> Throwable
) {
    ShadowLog.clear()
    val calls = ForegroundCallLog()
    harness.sms.beforeQuery = { calls.events.add(ForegroundCallLog.QUERY) }
    var expected: Throwable? = null
    val worker = foregroundTestBackupWorker(context, foregroundTestTags(BackupType.MANUAL.name),
        ThrowingForegroundUpdater(calls) { error().also { expected = it } }, failingTransportFactory(calls))

    val thrown = runCatching { runBlocking { worker.doWork() } }.exceptionOrNull()

    assertWithMessage("$label: doWork() throws").that(thrown).isNotNull()
    assertWithMessage("$label: class of what doWork() throws").that(thrown!!.javaClass).isEqualTo(expected!!.javaClass)
    assertWithMessage("$label: message of what doWork() throws").that(thrown.message).isEqualTo("x")
    assertWithMessage("$label: refusal warnings").that(ForegroundTestLog.withPrefix(ForegroundTestLog.BACKUP_PREFIX)).isEmpty()
    assertWithMessage("$label: what the run did (no create(), no provider query)").that(calls.events).containsExactly(
        ForegroundCallLog.FOREGROUND)
}
```

**T2** `$TS/service/RestoreWorkerForegroundTest.kt` — blob `e69175c646a728c0e4e4cf9bc3b7f08239ff4e31` (174 lines, 5 lines
containing `@Test`, LF; iteration 2: R2's account-name check, plan-review O-3); under F-34 blob
`9207950d18c7be23f0c178e001640b503aaa04e0`.

```kotlin
package com.zegoggles.smssync.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.InvalidForegroundServiceTypeException
import android.app.MissingForegroundServiceTypeException
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.work.Configuration
import androidx.work.ForegroundUpdater
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertWithMessage
import com.zegoggles.smssync.App
import com.zegoggles.smssync.contacts.ContactAccessor
import com.zegoggles.smssync.di.MailTransportFactory
import com.zegoggles.smssync.preferences.AuthPreferences
import com.zegoggles.smssync.preferences.Preferences
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors

/**
 * U-075 (BUG-029, AC-6): RestoreWorker.doWork asks for the foreground on every run, through the same
 * helper as the backup, and continues when the request is refused. The transport factory records
 * each create() and fails with MailException, which today's mapping turns into Result.retry().
 * SDK 35 for the refusal and the type exceptions (plan P-1).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RestoreWorkerForegroundTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        @Suppress("DEPRECATION")
        context = RuntimeEnvironment.application
        ShadowLog.clear()
        AuthPreferences(context).setImapUser(SmsFixture.OWNER)
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(Executors.newSingleThreadExecutor())
            .setWorkerFactory(RestoreWorker.TestableRestoreWorkerFactory(InMemoryCheckpointStore(), RestoreInsertInterceptor.NoOp))
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun restore_requestsForegroundOnce() {
        val calls = ForegroundCallLog()

        val result = runBlocking { restoreWorker(CountingForegroundUpdater(calls), failingTransportFactory(calls)).doWork() }

        assertWithMessage("Result (MailException from create())").that(result).isEqualTo(ListenableWorker.Result.retry())
        assertWithMessage("what the run did, in order").that(calls.events).containsExactly(
            ForegroundCallLog.FOREGROUND, ForegroundCallLog.CREATE).inOrder()
        val info = calls.infos.single()
        assertWithMessage("notification id").that(info.notificationId).isEqualTo(2)
        assertWithMessage("notification channel").that(info.notification.channelId).isEqualTo(App.CHANNEL_ID)
        assertWithMessage("foreground-service type").that(info.foregroundServiceType).isEqualTo(
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    @Test
    fun restore_foregroundRefused_continues() {
        val calls = ForegroundCallLog()
        val updater = ThrowingForegroundUpdater(calls) { ForegroundServiceStartNotAllowedException("test") }

        val result = runBlocking { restoreWorker(updater, failingTransportFactory(calls)).doWork() }

        assertWithMessage("Result (MailException from create())").that(result).isEqualTo(ListenableWorker.Result.retry())
        assertWithMessage("what the run did, in order").that(calls.events).containsExactly(
            ForegroundCallLog.FOREGROUND, ForegroundCallLog.CREATE).inOrder()
        assertWithMessage("the starting line").that(
            ForegroundTestLog.hasDebugLine("RestoreWorker.doWork: starting restore, attempt=0")).isTrue()
        ForegroundTestLog.assertRefusalLoggedOnce(ForegroundTestLog.RESTORE_PREFIX, "ForegroundServiceStartNotAllowedException")
        assertWithMessage("log entries carrying the configured account name").that(
            ShadowLog.getLogs().filter { (it.msg ?: "").contains(SmsFixture.OWNER) }).isEmpty()
    }

    @Test
    fun restore_foreground_otherIllegalState_propagates() {
        assertRestoreForegroundErrorPropagates("IllegalStateException") { IllegalStateException("x") }
    }

    @Test
    fun restore_foreground_typeException_propagates() {
        assertRestoreForegroundErrorPropagates("InvalidForegroundServiceTypeException") {
            InvalidForegroundServiceTypeException("x")
        }
        assertRestoreForegroundErrorPropagates("MissingForegroundServiceTypeException") {
            MissingForegroundServiceTypeException("x")
        }
    }

    @Test
    fun restore_foreground_cancelled_endsCancelled() = runBlocking<Unit> {
        val calls = ForegroundCallLog()
        lateinit var workJob: Job
        var thrown: Throwable? = null
        var result: ListenableWorker.Result? = null
        val worker = restoreWorker(
            ThrowingForegroundUpdater(calls) {
                workJob.cancel()
                CancellationException("test")
            },
            failingTransportFactory(calls))

        workJob = launch(start = CoroutineStart.LAZY) {
            try {
                result = worker.doWork()
            } catch (t: Throwable) {
                thrown = t
                throw t
            }
        }
        workJob.join()

        assertWithMessage("the job is cancelled").that(workJob.isCancelled).isTrue()
        assertWithMessage("doWork() ends by throwing a CancellationException").that(thrown).isInstanceOf(
            CancellationException::class.java)
        assertWithMessage("no Result is returned").that(result).isNull()
        assertWithMessage("refusal warnings").that(ForegroundTestLog.withPrefix(ForegroundTestLog.RESTORE_PREFIX)).isEmpty()
        assertWithMessage("what the run did (no create())").that(calls.events).containsExactly(ForegroundCallLog.FOREGROUND)
    }

    private fun assertRestoreForegroundErrorPropagates(label: String, error: () -> Throwable) {
        ShadowLog.clear()
        val calls = ForegroundCallLog()
        var expected: Throwable? = null
        val worker = restoreWorker(ThrowingForegroundUpdater(calls) { error().also { expected = it } },
            failingTransportFactory(calls))

        val thrown = runCatching { runBlocking { worker.doWork() } }.exceptionOrNull()

        assertWithMessage("$label: doWork() throws").that(thrown).isNotNull()
        assertWithMessage("$label: class of what doWork() throws").that(thrown!!.javaClass).isEqualTo(expected!!.javaClass)
        assertWithMessage("$label: message of what doWork() throws").that(thrown.message).isEqualTo("x")
        assertWithMessage("$label: refusal warnings").that(ForegroundTestLog.withPrefix(ForegroundTestLog.RESTORE_PREFIX)).isEmpty()
        assertWithMessage("$label: what the run did (no create())").that(calls.events).containsExactly(
            ForegroundCallLog.FOREGROUND)
    }

    private fun restoreWorker(updater: ForegroundUpdater, transportFactory: MailTransportFactory): RestoreWorker {
        val factory = object : WorkerFactory() {
            override fun createWorker(appContext: Context, workerClassName: String,
                                      workerParameters: WorkerParameters): ListenableWorker? {
                if (workerClassName != RestoreWorker::class.java.name) return null
                val prefs = Preferences(appContext)
                val authPrefs = AuthPreferences(appContext)
                return RestoreWorker(appContext, workerParameters, prefs, authPrefs, transportFactory,
                    InMemoryCheckpointStore(), RestoreInsertInterceptor.NoOp, ContactAccessor(),
                    WorkerEngineFactory(appContext, prefs, authPrefs))
            }
        }
        return TestListenableWorkerBuilder<RestoreWorker>(context)
            .setWorkerFactory(factory)
            .setForegroundUpdater(updater)
            .build()
    }
}
```

**F-34 (row D10 only).** In each of the two files, `old_string` `@Config(sdk = [35])` → `new_string` `@Config(sdk = [34])`
(one occurrence per file; the comments that say "SDK 35" stay). The resulting blobs are given above.

### 3.5 Helper tools (Write tool, I0 step 1, into `$TL` = `sdlc/artifacts/build/sprints/sprint-024/U-075/evidence/tools/`)

`oldtext_check.py` — blob `8d1158ecda4c0e9bde9962b46132dc33829c1324` (run on the simulated trees, L21):
```python
"""U-075: check that every text this story's edits, mutations and test fixtures rely on occurs as expected.
usage (from the repository root): python -I -X utf8 oldtext_check.py start|fixed
start: before any edit (the workers and U-070's fixtures as at <start>). fixed: after E-B1, E-B2, E-R1 and the new
helper file. Files are read with universal newlines, so CRLF and LF working trees give the same result. Prints one
line per check and a RESULT line; exit 1 if any count differs from the expected one."""
import os
import sys
S = 'app/src/main/java/com/zegoggles/smssync/service/'
T = 'app/src/test/java/com/zegoggles/smssync/service/'
BW, RW, WF = S + 'BackupWorker.kt', S + 'RestoreWorker.kt', S + 'WorkerForeground.kt'
FX, CK, PG = T + 'LateVisibleSmsFixtures.kt', T + 'RestoreWorkerCheckpointTest.kt', 'app/proguard-rules.pro'
CALL_B = '            setForegroundOrContinue(createBackupForegroundInfo(), TAG, "BackupWorker")\n'
CALL_R = '        setForegroundOrContinue(createRestoreForegroundInfo(), TAG, "RestoreWorker")\n'
LOGW = ('            Log.w(tag, "$site: foreground start not allowed; continuing without the foreground '
        '(${e.javaClass.simpleName})")\n')
SHARED = [
    ('START-B', BW, '        Log.d(TAG, "BackupWorker.doWork: starting backup, type=$backupType, attempt=$runAttempt")\n', 1),
    ('START-R', RW, '        Log.d(TAG, "RestoreWorker.doWork: starting restore, attempt=$runAttempt")\n', 1),
    ('RETRY-B', BW, '        } catch (e: MailException) {\n            // U-026: MailException replaces MessagingException\n'
                    '            Log.w(TAG, "BackupWorker: MailException — retrying", e)\n            Result.retry()\n', 1),
    ('RETRY-R', RW, '        } catch (e: MailException) {\n            // U-026: MailException replaces MessagingException\n'
                    '            Log.w(TAG, "RestoreWorker: MailException — retrying", e)\n            Result.retry()\n', 1),
    ('CTOR-B', BW, 'class BackupWorker @AssistedInject constructor(\n    @Assisted context: Context,\n'
                   '    @Assisted params: WorkerParameters,\n    private val preferences: Preferences,\n'
                   '    private val authPreferences: AuthPreferences,\n    private val mailTransportFactory: MailTransportFactory,\n', 1),
    ('FACTORY-R', RW, '    class TestableRestoreWorkerFactory(\n        private val checkpointStore: RestoreCheckpointStore,\n'
                      '        private val interceptor: RestoreInsertInterceptor\n    ) : WorkerFactory() {\n', 1),
    ('M-L11', BW, '        val confirmedMaxDate: Long = transport.appendMessages(folder, result)\n', 1),
    ('M-L20', BW, '                    if (cursor.type == DataType.SMS) {\n', 1),
    ('M-L22', BW, '                if (type == DataType.SMS) {\n', 1),
    ('U071-M-1', BW, '                currentCoroutineContext().ensureActive()\n', 1),
    ('FX-provider', FX, 'internal class ProviderDouble(private val kind: DataType) : ContentProvider() {\n', 1),
    ('FX-beforeQuery', FX, '    var beforeQuery: ((String) -> Unit)? = null\n', 1),
    ('FX-add', FX, '    fun add(date: Long, address: String?, body: String?, type: Int = SmsFixture.INBOX,\n', 1),
    ('FX-contacts', FX, 'internal class ContactsProviderDouble : ContentProvider() {\n', 1),
    ('FX-contacts-map', FX, '    val contacts = mutableMapOf<String, Pair<Long, Long>>()\n', 1),
    ('FX-transport', FX, 'internal class RecordingTransport : MailTransport {\n', 1),
    ('FX-appended', FX, '    val appended = mutableListOf<AppendedMessage>()\n', 1),
    ('FX-count', FX, '    fun count(call: String) = calls.count { it == call }\n', 1),
    ('FX-harness', FX, 'internal class LateSmsHarness(val context: Context) {\n', 1),
    ('FX-sms', FX, '    val sms = ProviderDouble(DataType.SMS)\n', 1),
    ('FX-dtp', FX, '    val dataTypePreferences: DataTypePreferences get() = preferences.dataTypePreferences\n', 1),
    ('FX-registerContacts', FX, '    fun registerContacts(): ContactsProviderDouble =\n', 1),
    ('FX-watermark', FX, '    fun watermark(): Long = dataTypePreferences.getMaxSyncedDate(DataType.SMS)\n', 1),
    ('FX-D', FX, '    const val D = 1_700_000_000_000L\n', 1),
    ('FX-OWNER', FX, '    const val OWNER = "backup-owner"\n', 1),
    ('CKPT', CK, 'open class InMemoryCheckpointStore : RestoreCheckpointStore {\n', 1),
    ('R8-dontoptimize', PG, '-dontoptimize\n', 1),
]
START = SHARED + [
    ('E-B1', BW, '        // U-049 AC-2: Set foreground notification early in doWork() so the backup shows\n'
                 '        // a foreground-service notification on Android 12+ without ANR risk.\n'
                 '        // Uses the same notification channel (App.CHANNEL_ID) and content that\n'
                 '        // SmsBackupService.notifyAboutBackup() previously provided.\n'
                 '        setForeground(createBackupForegroundInfo())\n', 1),
    ('E-B2', BW, '        // no more manual construction here.\n        val backupType = inferBackupType()\n', 1),
    ('E-R1', RW, '        // U-049 AC-2: Set foreground notification early in doWork() so the restore shows\n'
                 '        // a foreground-service notification on Android 12+ without ANR risk.\n'
                 '        // Uses the same notification channel (App.CHANNEL_ID) and content that\n'
                 '        // SmsRestoreService.restoreStateChanged() previously provided via startForeground().\n'
                 '        setForeground(createRestoreForegroundInfo())\n', 1),
    ('NO-HELPER-B', BW, 'setForegroundOrContinue', 0),
    ('NO-HELPER-R', RW, 'setForegroundOrContinue', 0),
]
FIXED = SHARED + [
    ('M-F1a/M-F5', BW, CALL_B, 1),
    ('M-F4', BW, '        if (!backupType.isBackground()) {\n' + CALL_B + '        }\n', 1),
    ('M-F1b', RW, CALL_R, 1),
    ('M-F2', WF, '        if (e is CancellationException) throw e\n'
                 '        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ForegroundRefusal.isRefusal(e)) {\n'
                 + LOGW + '            return false\n        }\n        throw e\n', 1),
    ('M-F3', WF, '    fun isRefusal(e: IllegalStateException): Boolean = e is ForegroundServiceStartNotAllowedException\n', 1),
    ('M-F6', WF, LOGW + '            return false\n', 1),
    ('M-X1', WF, '        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ForegroundRefusal.isRefusal(e)) {\n', 1),
    ('ONE-INFER', BW, '        val backupType = inferBackupType()\n', 1),
    ('OLD-CALL-B', BW, 'setForeground(createBackupForegroundInfo())', 0),
    ('OLD-CALL-R', RW, 'setForeground(createRestoreForegroundInfo())', 0),
    ('ONE-CALL-WF', WF, 'setForeground(info)', 1),
]
mode = sys.argv[1]
checks = START if mode == 'start' else FIXED
bad = 0
for cid, path, text, want in checks:
    n = open(path, encoding='utf-8').read().count(text)
    bad += n != want
    print(f'{cid} {path} occurrences={n} expected={want}' + ('' if n == want else ' MISMATCH'))
if mode == 'start':
    exists = os.path.exists(WF)
    bad += exists
    print(f'NO-FILE {WF} exists={exists} expected=False' + (' MISMATCH' if exists else ''))
print(f'RESULT mode={mode} mismatches={bad}')
sys.exit(1 if bad else 0)
```
M-F1b and M-F7 share an old text, as do M-F1a and M-F5; each is applied alone.

`dex_refs.py` — blob `4f72cc2f8c27e869e0c41a25afc0d58376b5f1e7` (smoke-tested on a synthetic listing and on the D8 output
of L19):
```python
"""U-075: list the classes of a dex disassembly that name a type.
usage: <dexdump> -d <classes.dex> | python -I -X utf8 dex_refs.py <type descriptor, e.g. Landroid/app/Foo;>
Prints 'CLASS <descriptor> lines=<n>' for each class whose disassembly names the type (instructions and catch
lists), then 'TOTAL classes=<k>'. Exit 0 always; the caller compares the output with the plan."""
import re
import sys
want = sys.argv[1]
head = re.compile(r"\s*Class descriptor\s*:\s*'([^']+)'")
current, counts = None, {}
for line in sys.stdin:
    m = head.match(line)
    if m:
        current = m.group(1)
        continue
    if current is not None and want in line:
        counts[current] = counts.get(current, 0) + 1
for name in sorted(counts):
    print(f'CLASS {name} lines={counts[name]}')
print(f'TOTAL classes={len(counts)}')
```

`ui_bounds.py` (for the orchestrator, section 9) — blob `d034222c4d13a79ead881a9948e3e491201b988d` (iteration 2: unreadable XML handled, the label
compared without regard to case, plan-review O-6; self-test 14.2):
```python
"""U-075 device step (iii): the centre of the app's backup button, from a uiautomator dump.
usage: python -I -X utf8 ui_bounds.py <dump.xml>
Prints 'X Y enabled=<true|false> label-is-backup=<True|False>' for the one node whose resource-id is
com.zegoggles.smssync:id/sync_button, and exit 0 (the label is compared without regard to case). Otherwise one line
'xml=unreadable', 'nodes=<n>' or 'bounds=unreadable' and exit 1. It never prints text from the dump, which can
contain the account's address."""
import re
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
except (ET.ParseError, OSError):
    print('xml=unreadable')
    sys.exit(1)
nodes = [n for n in root.iter('node') if n.get('resource-id') == 'com.zegoggles.smssync:id/sync_button']
if len(nodes) != 1:
    print(f'nodes={len(nodes)}')
    sys.exit(1)
m = re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', nodes[0].get('bounds', ''))
if not m:
    print('bounds=unreadable')
    sys.exit(1)
x1, y1, x2, y2 = map(int, m.groups())
print(f"{(x1 + x2) // 2} {(y1 + y2) // 2} enabled={nodes[0].get('enabled')} "
      f"label-is-backup={(nodes[0].get('text') or '').strip().lower() == 'backup'}")
```

`notif_check.py` (for the orchestrator, section 9; new in iteration 2, SC-1) — blob `0a7a889c505ac18f5e056cc49e4ea17e50390815` (self-test 14.3):
```python
"""U-075 device steps (iii) and (i): the app's records in a 'dumpsys notification --noredact' dump.
usage: python -I -X utf8 notif_check.py <dump.txt>
A record starts at a line containing 'NotificationRecord(' and ends before the next such line; it is the app's when
that line contains 'pkg=com.zegoggles.smssync '. Prints one line and exits 0:
'dump=ok records=<n> id1=<n> id1-channel=<True|False> id1-title-backing-up=<True|False>'
(id1: the app's records with id=1; id1-channel: one of them names channel=sms_backup_plus; id1-title-backing-up: one
of them carries 'android.title=String (Backing up)'). Prints 'dump=unreadable' and exits 1 when the file is empty or
has neither 'Notification Manager state' nor 'NotificationRecord(' in it. Prints no other text from the dump."""
import re
import sys
try:
    text = open(sys.argv[1], encoding='utf-8', errors='replace').read()
except OSError:
    text = ''
if not text.strip() or ('Notification Manager state' not in text and 'NotificationRecord(' not in text):
    print('dump=unreadable')
    sys.exit(1)
blocks, current = [], None
for line in text.splitlines():
    if 'NotificationRecord(' in line:
        current = [line]
        blocks.append(current)
    elif current is not None:
        current.append(line)
app = [b for b in blocks if 'pkg=com.zegoggles.smssync ' in b[0]]
id1 = [b for b in app if re.search(r'\bid=1\b', b[0])]
channel = any('channel=sms_backup_plus' in '\n'.join(b) for b in id1)
title = any('android.title=String (Backing up)' in '\n'.join(b) for b in id1)
print(f'dump=ok records={len(app)} id1={len(id1)} id1-channel={channel} id1-title-backing-up={title}')
```
The record format (`NotificationRecord(0x…: pkg=<package> user=… id=<n> …`, `Notification(channel=<id> …`, extras
`android.title=String (<text>)`) is recalled from AOSP's `NotificationRecord.dump`, not read on this emulator: a different
format makes the tool print `records=0`, which in (iii) is a false stop only, and in (i) is guarded by the sanity marker
and by (iii) having parsed a record of the same dump format first (section 9, (i) Pass).

`dt_secs.py` (for the orchestrator, section 9; new in iteration 2) — blob `75b37f0d80268bda4b982d5e414880e1788c8fbd` (self-test 14.4):
```python
"""U-075 device step (i): seconds from device time A to device time B ('MM-DD HH:MM:SS.mmm', logcat's form).
usage: python -I -X utf8 dt_secs.py '<A>' '<B>'
Prints 'seconds=<B-A with 3 decimals>' and exits 0; prints 'time=unreadable' and exits 1 if either value has another
form. Both times are taken in the same year; a step that crosses New Year is read by hand."""
import re
import sys
from datetime import datetime
form = re.compile(r'^\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}$')
if len(sys.argv) != 3 or not all(form.match(v.strip()) for v in sys.argv[1:3]):
    print('time=unreadable')
    sys.exit(1)
a, b = (datetime.strptime('2000-' + v.strip(), '%Y-%m-%d %H:%M:%S.%f') for v in sys.argv[1:3])
print(f'seconds={(b - a).total_seconds():.3f}')
```
`ac12_run.py` (for the orchestrator, section 9; new in iteration 2, plan-review B-3, contract-verification O-2, depth
check) — blob `6dc06eeed6d56463b3b9708621092a8e80a04500` (self-test 14.7, on today's real app log and on lines written in its format):
```python
"""U-075 device check (AC-12): read one backup run of a given type from a 'logcat -v threadtime' file.
usage: python -I -X utf8 ac12_run.py <logcat.txt> <TYPE> --pids <pid>[,<pid>...] [--after '<MM-DD HH:MM:SS.mmm>']
Only lines of the given process ids are read. The run is the first 'BackupWorker.doWork: starting backup, type=<TYPE>'
line (at or after --after); its result is the first later 'Worker result <R> for Work [ id=<id>, tags={ ... } ]' line
whose tags hold the worker's class name and <TYPE>. Prints one line of key=value fields and exits 0; exits 1 with
'pids=missing' when no process id is given. Fields: start, tid, id, result, moved (WM-Processor 'Moving WorkSpec (<id>)
to the foreground' lines anywhere), watermark ('watermark advanced for SMS' lines of the run's thread between start and
result), backedUp, nothing, unrecoverable and unrecoverable-cancel (an 'unrecoverable error' line whose next line of
the same thread names a CancellationException), and over the whole file: refused-warn (the app's own warning that it
continued without the foreground), defect (a WM-WorkerWrapper error line naming ForegroundServiceStartNotAllowedException)
and svc-refusal ('Unable to start foreground service'). It prints no message text."""
import re
import sys
args = sys.argv[1:]
def opt(name):
    if name in args:
        i = args.index(name)
        value = args[i + 1] if i + 1 < len(args) else ''
        del args[i:i + 2]
        return value
    return None
pids = {p for p in (opt('--pids') or '').split(',') if p.strip().isdigit()}
after = opt('--after') or ''
if not pids or len(args) != 2:
    print('pids=missing' if not pids else 'usage=wrong')
    sys.exit(1)
path, kind = args
line_re = re.compile(r'^(\d\d-\d\d \d\d:\d\d:\d\d\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+(.*?): (.*)$')
rows = []
for raw in open(path, encoding='utf-8', errors='replace'):
    m = line_re.match(raw.rstrip('\r\n'))
    if m and m.group(2) in pids:
        rows.append(m.groups())
start = next((i for i, r in enumerate(rows) if r[4] == 'SMSBackup+' and r[3] == 'D' and r[0] >= after
              and r[5].startswith(f'BackupWorker.doWork: starting backup, type={kind}, attempt=')), None)
f = {'type': kind, 'start': 'none', 'tid': 'none', 'id': 'none', 'result': 'none', 'moved': 0, 'watermark': 0,
     'backedUp': 'none', 'nothing': 0, 'unrecoverable': 0, 'unrecoverable-cancel': 0}
result_re = re.compile(r'^Worker result (\w+) for Work \[ id=([0-9a-f-]+), tags=\{ (.*) \} \]$')
if start is not None:
    f['start'], f['tid'] = rows[start][0].replace(' ', '_'), rows[start][2]
    end = len(rows)
    for i in range(start + 1, len(rows)):
        m = result_re.match(rows[i][5]) if rows[i][4] == 'WM-WorkerWrapper' else None
        if m and {'com.zegoggles.smssync.service.BackupWorker', kind} <= set(t.strip() for t in m.group(3).split(',')):
            f['result'], f['id'], end = m.group(1), m.group(2), i
            break
    run = [r for r in rows[start:end + 1] if r[2] == f['tid'] and r[4] == 'SMSBackup+']
    for j, r in enumerate(run):
        if r[5].startswith('BackupWorker: watermark advanced for SMS to '):
            f['watermark'] += 1
        if r[5].startswith('BackupWorker: backup complete, backedUp='):
            f['backedUp'] = r[5].split('=', 1)[1].strip()
        if r[5] == 'BackupWorker: nothing to backup':
            f['nothing'] += 1
        if r[5] == 'BackupWorker: unrecoverable error':
            f['unrecoverable'] += 1
            if j + 1 < len(run) and 'CancellationException' in run[j + 1][5]:
                f['unrecoverable-cancel'] += 1
    if f['id'] != 'none':
        f['moved'] = sum(1 for r in rows if r[4] == 'WM-Processor' and r[5] == f"Moving WorkSpec ({f['id']}) to the foreground")
f['refused-warn'] = sum(1 for r in rows if r[4] == 'SMSBackup+' and r[3] == 'W'
                        and 'foreground start not allowed; continuing without the foreground' in r[5])
f['defect'] = sum(1 for r in rows if r[4] == 'WM-WorkerWrapper' and r[3] == 'E'
                  and 'ForegroundServiceStartNotAllowedException' in r[5])
f['svc-refusal'] = sum(1 for r in rows if 'Unable to start foreground service' in r[5])
print(' '.join(f'{k}={v}' for k, v in f.items()))
```
Known limits (self-test 14.7): the window of a run is the start line's thread between its start and its result, which
is how `BackupWorker` logs (one coroutine thread per run in today's log); a run without a result line (a system stop) is
read to the end of the file.

The four orchestrator tools print to the console; Python on this machine writes CRLF to a redirected stdout, so every
consumer in section 9 reads their output with `read -r` into the first fields only, `grep` or `grep -o`, and strips
`\r` where a value is passed on (`tr -d '\r'`), never by byte comparison (self-test 14.5).
(`sync_button` is the status screen's backup button, `status.xml:78`, label `ui_sync_button_label_idle` = "Backup",
`strings.xml:11`, found by `StatusPreference.java:139`.)

### 3.6 Not changed

`WorkManagerScheduler.kt`, `AndroidManifest.xml` (the `dataSync` override `:126-129` stays, still needed for user-started
runs), `createBackupForegroundInfo()`, `createRestoreForegroundInfo()`, both workers' constructors and test factories,
every catch of both workers, `app/build.gradle` (no floor, no dependency), `robolectric.properties`, every existing test,
U-070's fixtures, every contract, design and change record.

## 4. Implementation Steps

### 4.0 Environment and conventions (every step)

```bash
# From the repository root. Never set MSYS_NO_PATHCONV for Gradle.
unset MSYS_NO_PATHCONV
W=sdlc/artifacts/build/sprints/sprint-024/U-075; E=$W/evidence; M=$E/mutations; TL=$E/tools
T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools      # read-only: sanitize.py, junit_totals.py, junit_cases.py, jacoco_pkgs.py
U69T=sdlc/artifacts/build/sprints/sprint-024/U-069/evidence/tools   # read-only: jacoco_classes.py
U68T=sdlc/artifacts/build/sprints/sprint-024/U-068/evidence/tools   # read-only: sdk_dir.py
R=app/build/test-results/testDebugUnitTest
JX=app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
S=app/src/main/java/com/zegoggles/smssync; TS=app/src/test/java/com/zegoggles/smssync
BW=$S/service/BackupWorker.kt; RW=$S/service/RestoreWorker.kt; WF=$S/service/WorkerForeground.kt
BFTF=$TS/service/BackupWorkerForegroundTest.kt; RFTF=$TS/service/RestoreWorkerForegroundTest.kt
BFT=com.zegoggles.smssync.service.BackupWorkerForegroundTest
B24=com.zegoggles.smssync.service.BackupWorkerForegroundApi24Test
RFT=com.zegoggles.smssync.service.RestoreWorkerForegroundTest
NEW5="com.zegoggles.smssync.service.LateVisibleSmsRegressionTest com.zegoggles.smssync.service.LateVisibleSmsBackupTest com.zegoggles.smssync.service.LateVisibleSmsSkipResetRestoreTest com.zegoggles.smssync.service.BackupQueryBuilderWindowTest com.zegoggles.smssync.preferences.SmsBackupWindowTest"
PLAN_HEAD=3770b9a4fb832053e7df6b05bff704bc25dab76e
SDK="$(python -I -X utf8 $U68T/sdk_dir.py)"; BT="$SDK/build-tools/36.0.0"
AAI="$HOME/.m2/repository/org/robolectric/android-all-instrumented"   # the local Maven cache; never written into a repository file
WORK=<a u075-work folder inside the developer's own session scratch folder>   # raw logs, pristine copies, dex extracts
START=<the commit recorded in I0 step 2>
```
Shell state does not persist; each call sets what it needs. `WORK`, `SDK`, `AAI` are never written into a repository file
(evidence says "the work folder", "the SDK", "the local Maven cache").

**Gradle calls.** Foreground, tool timeout 600000 ms on every call, output to the work folder, exit appended by the same
command line. A full gate takes about 9 to 10 minutes after U-070 (its `SmsBackupRecordSizeTest` alone about 5.5 minutes),
so the harness will usually move G0 and G-F to the background: wait for the completion notice, never poll, start no other
command meanwhile, then read the log; a log whose last line is not `exit=<n>` is a cut-off run (row D29).
- **G0** (I0) and **G-F** (I6), the only two full gates: `rm -rf $R; ./gradlew --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease :app:jacocoTestReport > $WORK/logs/I<k>-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I<k>-gates.log`
- **TR(name, classes…)** (every other run; its class lists never include `SmsBackupRecordSizeTest`): `rm -rf $R; ./gradlew --console=plain :app:testDebugUnitTest --tests "<fqcn>" [--tests "<fqcn>" …] > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log`
- **FG3** = `--tests "$BFT" --tests "$B24" --tests "$RFT"`.

`:app:jacocoTestReport` is reporting only (QA asked for it in the gate); the story's four gate tasks are in every gate.

**Filters for `sanitize.py`** (`python -I -X utf8 $T/sanitize.py <in> <out> --grep '<regex>'`):
- gate filter **G**: `'^(BUILD |FAILURE|> Task :app:(assemble|test|jacoco|lintVital|compile)|exit=)|tests completed|FAILED$|^e: |error:'`

**EV(k)** (after a gate; k = 0, 6; `jacoco_pkgs.py` prints dotted names, so its `service` line is
`com.zegoggles.smssync.service <covered>/<total> <percent>%`):
```bash
python -I -X utf8 $T/sanitize.py $WORK/logs/I<k>-gates.log $E/I<k>-gates.txt --grep '<G>'
python -I -X utf8 $T/junit_totals.py $R > $E/I<k>-junit-totals.txt
python -I -X utf8 $T/jacoco_pkgs.py $JX > $E/I<k>-jacoco-packages.txt
python -I -X utf8 $U69T/jacoco_classes.py $JX com/zegoggles/smssync/service > $E/I<k>-service-classes.txt
```
At I6 also `python -I -X utf8 $T/junit_cases.py $R BackupWorkerForeground RestoreWorkerForeground > $E/I<k>-cases.txt`.

**Timestamps** come from `date -u +%Y-%m-%dT%H:%M:%SZ`, run at that moment.

### I0 — Start state and baseline (no tracked file changes)

1. Create `$WORK/logs`, `$WORK/pristine`, `$E`, `$M`, `$TL`; write the six tools of 3.5 with the Write tool;
   `git hash-object $TL/oldtext_check.py $TL/dex_refs.py $TL/ui_bounds.py $TL/notif_check.py $TL/dt_secs.py $TL/ac12_run.py`
   → `8d1158ecda4c0e9bde9962b46132dc33829c1324`, `4f72cc2f8c27e869e0c41a25afc0d58376b5f1e7`,
   `d034222c4d13a79ead881a9948e3e491201b988d`, `0a7a889c505ac18f5e056cc49e4ea17e50390815`,
   `75b37f0d80268bda4b982d5e414880e1788c8fbd`, `6dc06eeed6d56463b3b9708621092a8e80a04500` (each derived from the plan text,
   14.1; row D13).
2. **Start state** (one call into `$WORK/logs/I0-start.log`, sanitised to `$E/I0-start.txt`), each command echoed first:
   ```bash
   date -u +%Y-%m-%dT%H:%M:%SZ; git rev-parse HEAD; git rev-parse --abbrev-ref HEAD
   git merge-base --is-ancestor $PLAN_HEAD HEAD; echo "plan-head-ancestor-exit=$?"
   git log --oneline $PLAN_HEAD..HEAD
   git log --oneline $PLAN_HEAD..HEAD -- app
   grep -n '^status' sdlc/artifacts/build/sprints/sprint-024/U-069-*.md sdlc/artifacts/build/sprints/sprint-024/U-070-*.md
   git status --porcelain --untracked-files=all -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat; echo "porcelain-end"
   for p in $BW $RW; do echo "$(git rev-parse HEAD:$p) $(git hash-object $p) $p"; done
   python -I -X utf8 $TL/oldtext_check.py start; echo "oldtext-exit=$?"
   grep -n -F "                currentCoroutineContext().ensureActive()" $BW
   git grep -n "setForeground(" HEAD -- app/src/main
   git grep -n assumenosideeffects HEAD -- app; echo "assumenosideeffects-exit=$?"
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   git grep -nE '^\s*@Ignore(\s|\(|$)' HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt'
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   command ls -1 "$AAI" > $WORK/logs/aai-before.txt; cat $WORK/logs/aai-before.txt; grep -cE '^(7\.0\.0_r1-robolectric-r1-i7|14-robolectric-10818077-i7|15-robolectric-12650502-i7)/?$' $WORK/logs/aai-before.txt
   "$(command ls -d "/c/Program Files/Java/"jdk-25* | tail -1)/bin/jps.exe" -l | grep -iE 'gradle'; echo "jps-gradle-end"
   git status --porcelain --untracked-files=all -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records | cut -c4- | while read -r p; do sha256sum "$p"; done > $WORK/logs/others-I0.txt; cat $WORK/logs/others-I0.txt
   ```
   Expected: `plan-head-ancestor-exit=0`; the `-- app` log shows U-070's code commits only; both stories `status: done`;
   nothing before `porcelain-end`; for each worker the two hashes are equal; the blobs are expected to be `ba667c43…` and
   `dc84c4f5…` (U-070 built exactly as planned; any other value is row D5, not a stop when the text check passes);
   `RESULT mode=start mismatches=0`, `oldtext-exit=0` (every anchor once, U-070's fixture names as built, row D4); the M-1
   line expected at `:339` (record the number; U-071 reads it); `setForeground(` 6 lines: `AndroidManifest.xml:118`,
   `MainActivity.java:510`, `BackupWorker.kt:123` and `:525`, `RestoreWorker.kt:148` and `:687` (post-U-070 numbers from
   the simulation; the same texts at other numbers are recorded, row D30); `assumenosideeffects-exit=1` and no line (AC-2);
   `C0` as printed with the commit id (821 expected if only U-070 landed after `fd70325a`; any other value is recorded
   and explained, not a stop, row D22); the `@Ignore` lines (two at `fd70325a`: `AppTest.java:13`,
   `AuthPreferencesTest.java:1079`); `toolchainVersion=25`; the cache listing contains `7.0.0_r1-robolectric-r1-i7`,
   `14-robolectric-10818077-i7` and `15-robolectric-12650502-i7`, the count `3` (`ls` on this machine is aliased to
   `ls -F`, hence `command ls -1` and the optional `/`; row D9); no Gradle JVM before `jps-gradle-end` (row D34);
   `others-I0.txt`: one `sha256sum` line per uncommitted path of someone else under the three document folders, or none
   (the orchestrator's ruling on contract O-6: recorded, not a stop; CR-017 is expected committed before `<start>`).
   Record `START=$(git rev-parse HEAD)` and `C0` at the top of the log. **Rows:** D1 to D9, D23, D30, D34.
3. **U-070's recorded mutation sets** into `$WORK/logs/I0-u070.log`, sanitised to `$E/I0-u070-mutations.txt`:
   `for id in M-L11 M-L20 M-L22; do awk -v id="$id" '$0 ~ "^### " id " " {on=1; print; next} /^### / {on=0} on' sdlc/artifacts/build/sprints/sprint-024/U-070/evidence/mutation-log.md; done`
   (whole entries, up to the next `### ` heading; plan-review O-5; tested on U-069's log, 14.10). Read each entry's failed
   set and translate U-070's ids (B10, S3, …) to test names through U-070's plan table 4.8. Planned values (U-070 plan I8): M-L11 fails `ac9_appendFails_nothingSaved_rowsUploadedByTheNextBackup`,
   `ac9_cancelledDuringTheAppend_nothingSaved_rowsUploadedByTheNextBackup`, `ac10_deathAfterTheServerConfirmed_onlyThatMessageTwice_oneCommitPerRowCarriesBoth`
   (`LateVisibleSmsBackupTest`); M-L20 fails `ac15_recordOnlyWritesKeepTheFirstRun_emptyFirstRunWritesTheSentinel`,
   `ac21_rowsTheConverterRejects_recordedAsExamined_lateSmsNotStarved_noLogin`; M-L22 fails
   `ac12_skipWithoutSms_leavesTheSmsWatermarkAndRecordUntouched` (`LateVisibleSmsSkipResetRestoreTest`). **The recorded sets
   govern** (story AC-11: "exactly the failing set U-070 recorded"); a difference from the planned values is recorded. Row D20.
4. `./gradlew --version > $WORK/logs/I0-gradle-version.log 2>&1`, sanitised to `$E/I0-gradle-version.txt` (the Daemon JVM
   line cites `gradle/gradle-daemon-jvm.properties`, Java 25). **Row:** D34.
5. **Gate G0** (expect it in the background, 4.0), then EV(0). Expected `exit=0` as the log's last line; `failures=0 errors=0`;
   `tests=T0`, `skipped=S0` (recorded); the `com.zegoggles.smssync.service` line of `I0-jacoco-packages.txt` recorded as
   `C_s/T_s` (no expected number; 674/822 at `fd70325a`, U-070 adds lines). **Rows:** D7, D8, D29.
6. Create `$W/implementation-log.md`: story, plan path, `START`, `C0`, `T0`, `S0`, `C_s/T_s`, the `@Ignore` lines, the I0
   results by reference to the evidence files, P-3's mapping (fixture names as built = as planned, or the differences
   under row D4), and the note that U-069's and U-070's tests are inside `C0`.

### I1 — The two test files (no production change)

1. Write `$BFTF` and `$RFTF` (3.4) with the Write tool.
2. Checks into `$WORK/logs/I1-checks.log`, sanitised to `$E/I1-checks.txt`: `git hash-object $BFTF $RFTF` →
   `3e3ea62b56e25158bb73f993f8a5f2f44b3825b6`, `e69175c646a728c0e4e4cf9bc3b7f08239ff4e31`; `grep -c "@Test" $BFTF $RFTF` → 13, 5;
   `git status --porcelain --untracked-files=all -- app` → exactly `?? $BFTF` and `?? $RFTF`;
   `git diff --quiet HEAD -- app/src/main; echo "main-clean-exit=$?"` → 0. **Rows:** D13, D35.

### I2 — Characterisation and red run on the unchanged workers (P-1 probe)

1. TR(`I2-red`, FG3). Expected `exit=1`, the classes compiled (N1; row D12).
2. `python -I -X utf8 $T/junit_cases.py $R BackupWorkerForeground RestoreWorkerForeground > $E/I2-red-cases.txt`;
   `for c in $BFT $B24 $RFT; do grep -o 'message="[^"]*"' $R/TEST-$c.xml; done > $WORK/logs/I2-red-msg.log; python -I -X utf8 $T/sanitize.py $WORK/logs/I2-red-msg.log $E/I2-red-messages.txt`.
   Expected: the table of 5.2, `cases=18 pass=11 fail=7 error=0`. **Rows:** D10, D11.
3. `command ls -1 "$AAI" > $WORK/logs/aai-I2.txt; git diff --no-index --quiet $WORK/logs/aai-before.txt $WORK/logs/aai-I2.txt; echo "aai-equal-exit=$?"`
   → `aai-equal-exit=0` (no download; both files are written by the shell). Row D26.
4. Append to the implementation log the red output (failing tests, their failure types and first message lines) for the
   story's Implementation Notes ("Red at Start"), and P-1's outcome (refusal level 35, or 34 under F-34).

### I3 — The fix and the green run

1. Write `$WF` (3.3) with the Write tool.
2. Edits E-B1, E-B2 (3.1) and E-R1 (3.2) with the Edit tool (Read the lines first).
3. Checks into `$WORK/logs/I3-checks.log`, sanitised to `$E/I3-checks.txt`, each echoed:
   ```bash
   python -I -X utf8 $TL/oldtext_check.py fixed; echo "oldtext-exit=$?"
   git hash-object $WF $BW $RW
   git diff --numstat HEAD -- $BW $RW
   git diff -U0 HEAD -- $BW | grep '^@@'; git diff -U0 HEAD -- $RW | grep '^@@'
   git status --porcelain --untracked-files=all -- app/src/main
   git grep -n --untracked "setForeground(" -- app/src/main
   git grep -n --untracked "setForegroundOrContinue" -- app/src/main
   tr -cd '\r' < $WF | wc -c
   ```
   Expected: `RESULT mode=fixed mismatches=0`; `$WF` blob `0782f85f1b3815790ac21c87b454fe1f972a34c1`; `$BW` and `$RW` blobs
   `3f2f0c7678fab5a7ddf5d36581f2df32cd19a643` and `9be01b15210508393e750136ff127cc7eed3a968` if the start blobs were the
   simulated ones (row D5 otherwise); numstat `10	6	…BackupWorker.kt` and `5	5	…RestoreWorker.kt`; hunks
   `@@ -119,5 +119,10 @@ class BackupWorker @AssistedInject constructor(`, `@@ -142 +146,0 @@ class BackupWorker @AssistedInject constructor(`
   and `@@ -144,5 +144,5 @@ class RestoreWorker @AssistedInject constructor(` (every changed line inside `doWork()`, which
   spans `:118-176` after the edit; AC-8); porcelain exactly ` M $BW`, ` M $RW`, `?? $WF`; `setForeground(` 5 lines:
   `AndroidManifest.xml:118`, `MainActivity.java:510`, `BackupWorker.kt:529`, `RestoreWorker.kt:687` (the four comments of
   2.7) and `WorkerForeground.kt:45` (the only call, IC-1); `setForegroundOrContinue` 3 lines: `BackupWorker.kt:127`,
   `RestoreWorker.kt:148`, `WorkerForeground.kt:39` (IC-1); the CR count of `$WF` recorded (0 expected; the recovery note
   uses it). **Rows:** D15, D30.
4. TR(`I3-green`, FG3). Expected `exit=0`; `junit_cases.py` (into `$E/I3-green-cases.txt`) `cases=18 pass=18`. Row D14.

### I4 — Pristine copies (no Gradle)

No measurement gate here (coordinator's fact 1): the full suite and coverage are read once more at I6, before which the
mutation runs only use the three new classes and U-070's NEW5 classes. A coverage fall found at I6 is still a stop (D16).
1. `git status --porcelain --untracked-files=all -- app` is exactly the five paths of I6 step 5; `oldtext_check.py fixed`
   still `mismatches=0`. **Row:** D35.
2. Pristine copies and hashes into `$WORK/logs/I4-pristine.log`, sanitised to `$E/I4-pristine.txt`:
   `cp $BW $RW $WF $WORK/pristine/`; `sha256sum $BW $RW $WF` (`SB`, `SR`, `SF`); `git hash-object -w $BW $RW $WF` (`HB`,
   `HR`, `HF`; written to the object store for recovery); `git diff HEAD -- app/src/main | git hash-object --stdin`
   (`PROD_DIFF`, tracked files only). Record all seven values in the implementation log.

### I5 — Failure injections (protocol 4.8; table 4.8)

The twelve entries in table order, one at a time. After the last: every entry has its `restored` line.

### I6 — Final gate, release checks and the final checks

0. **Clean check before the gate** (plan-review O-2): the 4.8 clean check (`sha256sum $BW $RW $WF` = `SB SR SF`,
   `PROD_DIFF` unchanged, porcelain of `app/src/main` exactly the three paths) and
   `grep -cE '^### [^ ]+ APPLYING ' $E/mutation-log.md` equal to `grep -cE '^(- )?restored: ' $E/mutation-log.md` (12 and
   12; the header's recovery note also contains the word "APPLYING" and `restored:` lines may be list items, hence the
   anchored forms, tested on U-069's committed log, 14.10). **Rows:** D17, D32.
1. **Gate G-F** (with `clean`; expect it in the background, 4.0), then EV(6). Expected `exit=0` as the log's last line;
   `tests = T0 + 18`, failures 0, errors 0, skipped not above `S0`; in `I6-service-classes.txt`
   `CLASS com.zegoggles.smssync.service.WorkerForegroundKt covered=10 missed=0` and
   `CLASS com.zegoggles.smssync.service.ForegroundRefusal covered=2 missed=0` (L19; other totals with `missed=0` are row
   D16 (b)); the `com.zegoggles.smssync.service` ratio not below `C_s/T_s` (expected about +13 covered and +13 total
   lines: the helper's 12 and one net executable line in `doWork`); every floor passes. **Rows:** D16, D21, D29.
2. `command ls -1 "$AAI" > $WORK/logs/aai-I6.txt; git diff --no-index --quiet $WORK/logs/aai-before.txt $WORK/logs/aai-I6.txt; echo "aai-equal-exit=$?"`
   → `aai-equal-exit=0`. Row D26.
3. **Count (AC-10)**: `git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`
   (working-tree form, stated as such) → `C0 + 18`. Row D22.
4. **Release and dex checks** into `$WORK/logs/I6-release.log`, sanitised to `$E/I6-release.txt`:
   ```bash
   ls app/build/outputs/apk/release/ app/build/outputs/apk/debug/
   grep -n -E '^com\.zegoggles\.smssync\.service\.(WorkerForegroundKt|ForegroundRefusal) -> ' app/build/outputs/mapping/release/mapping.txt
   for v in debug release; do rm -rf "$WORK/dex-$v"; mkdir -p "$WORK/dex-$v"; unzip -o -q app/build/outputs/apk/$v/*.apk 'classes*.dex' -d "$WORK/dex-$v"; done
   for d in "$WORK"/dex-debug/classes*.dex; do echo "== debug $(basename "$d")"; "$BT/dexdump.exe" -d "$d" | python -I -X utf8 $TL/dex_refs.py 'Landroid/app/ForegroundServiceStartNotAllowedException;'; done
   MAP=app/build/outputs/mapping/release/mapping.txt
   FR=$(grep -E '^com\.zegoggles\.smssync\.service\.ForegroundRefusal -> ' $MAP | sed -E 's/.* -> (.*):$/\1/' | tr -d '\r'); WK=$(grep -E '^com\.zegoggles\.smssync\.service\.WorkerForegroundKt -> ' $MAP | sed -E 's/.* -> (.*):$/\1/' | tr -d '\r')
   FRD="L${FR//.//};"; WKD="L${WK//.//};"; echo "release names: ForegroundRefusal=$FRD WorkerForegroundKt=$WKD"
   for d in "$WORK"/dex-release/classes*.dex; do "$BT/dexdump.exe" -d "$d" | python -I -X utf8 $TL/dex_refs.py 'Landroid/app/ForegroundServiceStartNotAllowedException;'; done > "$WORK/logs/release-refs.txt"; cat "$WORK/logs/release-refs.txt"
   echo "holder-lines=$(grep -cF "CLASS $FRD " "$WORK/logs/release-refs.txt") caller-lines=$(grep -cF "CLASS $WKD " "$WORK/logs/release-refs.txt")"
   ```
   Expected: APK names `app-release-unsigned.apk` (or `app-release.apk`) and `app-debug.apk`; two mapping lines (the names
   right of `->` are recorded, renamed or not: no keep rule is needed, L22); in the debug dex files the classes under
   `Lcom/zegoggles/` that name the type are exactly `Lcom/zegoggles/smssync/service/ForegroundRefusal;` (or a synthetic
   class whose name begins with it, from D8's API outlining); library classes such as
   `Landroidx/work/impl/foreground/SystemForegroundService$Api31Impl;` are recorded as library. In the **release** dex
   (R8 runs only there; obfuscation is on, so names come from `mapping.txt`; depth check, should 1): `holder-lines=1` and
   `caller-lines=0`, and every other `CLASS` line is a library class or a synthetic class R8 derived from the holder
   (pipeline tested on the D8 output with a mapping file, 14.9). **Rows:** D24, D25.
5. **Final checks** into `$WORK/logs/I6-final.log`, sanitised to `$E/I6-final-checks.txt`, each echoed:
   ```bash
   python -I -X utf8 $TL/oldtext_check.py fixed; echo "oldtext-exit=$?"
   git hash-object $BW $RW $WF; sha256sum $BW $RW $WF
   git status --porcelain --untracked-files=all -- app
   git diff --name-only $START -- app/src/main
   git diff --quiet $START -- app/src/test; echo "test-tracked-unchanged-exit=$?"
   git diff --name-only $START -- build.gradle app/build.gradle k9mail-vendored/build.gradle settings.gradle gradle
   git log --oneline $START..HEAD -- app k9mail-vendored build.gradle settings.gradle gradle
   git status --porcelain --untracked-files=all -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records | cut -c4- | while read -r p; do sha256sum "$p"; done > $WORK/logs/others-I6.txt
   grep -vxF -f $WORK/logs/others-I0.txt $WORK/logs/others-I6.txt; echo "docs-new-or-changed-exit=$?"
   git log --format='%h %s' $START..HEAD -- sdlc/artifacts/design sdlc/artifacts/requirements sdlc/artifacts/build/change-records
   git grep -n --untracked "setForeground(" -- app/src/main
   git grep -n --untracked "setForegroundOrContinue" -- app/src/main
   git grep -n assumenosideeffects -- app; echo "assumenosideeffects-exit=$?"
   grep -nE '@Ignore|Assume' $BFTF $RFTF; echo "ignore-exit=$?"
   grep -c "@Test" $BFTF $RFTF
   ```
   Expected: `mismatches=0`; the blobs and hashes equal `HB`, `HR`, `HF` and `SB`, `SR`, `SF`; porcelain exactly ` M $BW`,
   ` M $RW`, `?? $WF`, `?? $BFTF`, `?? $RFTF`; `git diff --name-only $START -- app/src/main` lists `$BW` and `$RW` (the new
   `$WF` is untracked until the orchestrator's commit; with that commit in place of the working tree it lists the three
   files, hand-over item 1); `test-tracked-unchanged-exit=0` (added files only, AC-8); the build-file list is empty and no
   code commit after `$START` (row D23); `docs-new-or-changed-exit=1` with no line (every uncommitted document path of I6
   was already uncommitted with the same hash at I0; a path committed in between appears in the `git log` lines with the
   orchestrator's subject, which is recorded; the orchestrator's ruling on contract O-6, tested in 14.17); the two `git
   grep` outputs as in I3 step 3; `assumenosideeffects-exit=1`; `ignore-exit=1`; 13 and 5. **Rows:** D23, D30, D31, D32,
   D33.

### I7 — Implementation log, hygiene scan, hand-off

1. Implementation log: every step's result by reference; the counts (5.3); the "For the story's Implementation Notes" block
   with: `START` and counts; P-1's outcome (refusal level, how it was established offline: L12, L13, the I2 probe and the
   equal cache listings); P-2's reading (copy 2.5); P-3's mapping; the red output of I2; IC-2's line for U-074 (2.7); the
   stale comments (2.7); BUG-029 Not verified 5 as still not verified; M-X1's result.
2. **Hygiene scan** (last), `$WORK/logs/I7-scan.log`, sanitised to `$E/I7-scan.txt`:
   `grep -rnE '[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+\.[A-Za-z]{2,}|Users[/\\]|AppData' $W/implementation-log.md $E; echo "scan-exit=$?"`
   → `scan-exit=1`, no line. Row D27.
3. Hand-off: the 4.9 list, the APK path `app/build/outputs/apk/debug/app-debug.apk` with its size and `sha256sum` (for
   section 9), and "no mutation entry lacks `restored`".

### 4.8 Mutation protocol (write-ahead; restore proven by hash) and table

`$E/mutation-log.md` header: "Protocol: U-075 plan 4.8 (U-074 AC-15 form: production change uncommitted; each changed
production file saved and its `sha256sum` recorded first (I4 step 2); a write-ahead entry; one Edit; one targeted run;
restore by copying the saved file; the hash shown equal to the saved one; `git status --porcelain -- app/src/main` listing
only this story's files; never present at a gate, a review or a commit). Repository-relative paths."; `START`, `SB`, `SR`,
`SF`, `HB`, `HR`, `HF`, `PROD_DIFF`. **Recovery note** (in the header): "If a session is lost while an entry says
`APPLYING` without a `restored` line, nothing else runs until the file of that entry is restored only by `cp` from its
pristine copy, or, if that copy is lost: `$BW`, `$RW` by `git cat-file --filters --path=<path> <HB|HR> > <path>`; `$WF`
by `git cat-file blob <HF> > $WF` (LF; use the `--filters` form if I3 recorded a CR count above 0); then `sha256sum` equals
`SB`, `SR` or `SF`. Never `git checkout` or `git restore` (they would remove the fix)."

**Clean check** (before and after each entry): `sha256sum $BW $RW $WF` = `SB SR SF`; `git diff HEAD -- app/src/main | git
hash-object --stdin` = `PROD_DIFF`; `git status --porcelain --untracked-files=all -- app/src/main` = ` M $BW`, ` M $RW`,
`?? $WF` exactly.

**Each entry:**
1. Clean check (row D17 on failure).
2. Write-ahead `### <id> APPLYING <date -u>`: file, its `sha256sum`, the exact edit, the classes run, the expected
   failing set (copied from the table) and "every other case passes".
3. One Edit (Read the lines first).
4. TR(`<id>`, classes). Then `python -I -X utf8 $T/junit_cases.py $R BackupWorkerForeground RestoreWorkerForeground LateVisibleSms BackupQueryBuilderWindow SmsBackupWindow > $M/<id>-cases.txt`
   and `for x in $R/TEST-*.xml; do grep -o 'message="[^"]*"' "$x"; done > $WORK/logs/<id>-msg.log; python -I -X utf8 $T/sanitize.py $WORK/logs/<id>-msg.log $M/<id>-message.txt`.
5. Restore at once: `cp $WORK/pristine/<file> <path>`, whatever the result.
6. Clean check; append `run_exit`, failed ids, passed count, `expected: matches | does not match` (as sets),
   `verdict: detected | undetected | invalid (does not compile) | informational`, and `restored: <date -u> sha256-ok diff-hash-ok porcelain-ok`.

Failure types: **A** = an `AssertionError` (Truth's); **X** = an exception from the code under test (a failing test; type
recorded). Test ids: section 5.1. "FG3" = the 18 cases of `BackupWorkerForegroundTest`, `BackupWorkerForegroundApi24Test`,
`RestoreWorkerForegroundTest`.

| Id | File | Exact edit (`old_string` → `new_string`) | Classes | Complete failing set (every other case of the classes run passes) |
|----|------|-------------------------------------------|---------|----------------------------------------------------------------------|
| M-F1a | `$BW` | `            setForegroundOrContinue(createBackupForegroundInfo(), TAG, "BackupWorker")` → `            setForeground(createBackupForegroundInfo())` | FG3 | F1, F3, F4, F5 (X, the refusal): 4 |
| M-F1b | `$RW` | `        setForegroundOrContinue(createRestoreForegroundInfo(), TAG, "RestoreWorker")` → `        setForeground(createRestoreForegroundInfo())` | FG3 | R2 (X): 1 |
| M-F2 | `$WF` | the six lines from `        if (e is CancellationException) throw e` through `        throw e` → two lines `        Log.w(tag, "$site: foreground start not allowed; continuing without the foreground (${e.javaClass.simpleName})")` / `        return false` (continue on every `IllegalStateException`; the refusal test and the cancellation rethrow both go, which is what makes AC-5 (d) detectable) | FG3 | F9, F10, F12, F13, R3, R4, R5 (A: "doWork() throws" or "the job is cancelled"/"what the run did"): 7 |
| M-F3 | `$WF` | `    fun isRefusal(e: IllegalStateException): Boolean = e is ForegroundServiceStartNotAllowedException` → `    fun isRefusal(e: IllegalStateException): Boolean = e is android.app.ServiceStartNotAllowedException` | FG3 | F10, R4 (A, "doWork() throws" for the first type exception): 2 |
| M-F4 | `$BW` | the three lines `        if (!backupType.isBackground()) {` / `            setForegroundOrContinue(createBackupForegroundInfo(), TAG, "BackupWorker")` / `        }` → `        setForegroundOrContinue(createBackupForegroundInfo(), TAG, "BackupWorker")` | FG3 | F6 ("INCOMING: foreground requests"), F7 ("INCOMING: refusal warnings"): 2 A |
| M-F5 | `$BW` | `            setForegroundOrContinue(createBackupForegroundInfo(), TAG, "BackupWorker")` → `            // M-F5: no foreground request` | FG3 | F1, F3, F4, F5 (no warning), F8 ("MANUAL: foreground requests"), F9, F10, F11, F12, F13 (nothing thrown; F12 "the job is cancelled"; F5 may fail at its first assertion): 10 |
| M-F6 | `$WF` | the two lines `            Log.w(tag, "$site: foreground start not allowed; continuing without the foreground (${e.javaClass.simpleName})")` / `            return false` → `            return false` | FG3 | F1, F3, F4, F5, R2 ("entries whose text starts with the refusal warning's prefix"): 5 A |
| M-F7 | `$RW` | `        setForegroundOrContinue(createRestoreForegroundInfo(), TAG, "RestoreWorker")` → `        // M-F7: no foreground request` (this plan's addition, so that every restore test is detected by one injection) | FG3 | R1, R2 ("what the run did, in order"), R3, R4, R5 (nothing thrown; R5 "the job is cancelled"): 5 A |
| M-X1 | `$WF` | `        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ForegroundRefusal.isRefusal(e)) {` → `        if (ForegroundRefusal.isRefusal(e)) {` (informational, not counted) | FG3 | Either F13 alone (X or A: `NoClassDefFoundError` for the refusal's class, because the SDK 24 sandbox has no such class) or none (the sandbox resolved the class elsewhere). Both are recorded; neither is a stop (row D19). Every SDK 35 case passes |
| M-L11 | `$BW` | U-070's row: `        val confirmedMaxDate: Long = transport.appendMessages(folder, result)` → the two lines `        dataTypePreferences.setMaxSyncedDateAfterAppend(type, DataType.Defaults.MAX_SYNCED_DATE, result.mapList)` / `        val confirmedMaxDate: Long = transport.appendMessages(folder, result)` | FG3 + NEW5 | U-070's recorded set (I0 step 3; planned: the three `ac9`/`ac10` tests of `LateVisibleSmsBackupTest`); FG3 all pass (the final call still sets the watermark to the confirmed date) |
| M-L20 | `$BW` | U-070's row: `                    if (cursor.type == DataType.SMS) {` → `                    if (false) {` | FG3 + NEW5 | U-070's recorded set (planned: `ac15_…`, `ac21_…`); FG3 all pass (the branch is not reached: the conversion succeeds) |
| M-L22 | `$BW` | U-070's row: `                if (type == DataType.SMS) {` → `                if (type == DataType.SMS \|\| type == DataType.MMS) {` | FG3 + NEW5 | U-070's recorded set (planned: `ac12_skipWithoutSms_…`); FG3 all pass (F3 compares two Skips under the same code) |

`\|\|` is Kotlin's `||`, escaped for this table. Classes "FG3 + NEW5" add `--tests` for each class of `$NEW5`. If U-070's
built text of M-L11, M-L20 or M-L22 differs from its plan, the text in U-070's `mutation-log.md` is used and recorded
(row D20). Every mutant except the U-070 ones was derived against the test text of 3.4 and the production text of 3.1 to
3.3; the helper's mutants M-F2, M-F3, M-X1 compile (scratch compile); M-F1a, M-F1b, M-F4, M-F5, M-F6, M-F7 replace one
statement by a valid statement or comment. **Counted detections: M-F1a to M-F7 (8).** If a counted mutant does not compile
(row D18 (c)), its compiling replacement is: M-F5 `            if (false) setForegroundOrContinue(createBackupForegroundInfo(), TAG, "BackupWorker")`;
M-F7 `        if (false) setForegroundOrContinue(createRestoreForegroundInfo(), TAG, "RestoreWorker")`; M-F4
`        if (true) {` in place of its first line only; the others have none (stop).

### 4.9 Files for the orchestrator's commit (exact; the developer commits nothing)

`$W` = `sdlc/artifacts/build/sprints/sprint-024/U-075`.
- Code: `$S/service/BackupWorker.kt`, `$S/service/RestoreWorker.kt`, `$S/service/WorkerForeground.kt`,
  `$TS/service/BackupWorkerForegroundTest.kt`, `$TS/service/RestoreWorkerForegroundTest.kt`.
- Log: `$W/implementation-log.md`. Tools: `$W/evidence/tools/{oldtext_check.py,dex_refs.py,ui_bounds.py,notif_check.py,dt_secs.py,ac12_run.py}`.
- I0: `$W/evidence/I0-{start.txt,u070-mutations.txt,gradle-version.txt,gates.txt,junit-totals.txt,jacoco-packages.txt,service-classes.txt}`.
- I1: `$W/evidence/I1-checks.txt`. I2: `$W/evidence/I2-{red-cases.txt,red-messages.txt}`. I3: `$W/evidence/I3-{checks.txt,green-cases.txt}`.
- I4: `$W/evidence/I4-pristine.txt`.
- I5: `$W/evidence/mutation-log.md`; `$W/evidence/mutations/<id>-{cases,message}.txt` for the twelve ids (24 files).
- I6: `$W/evidence/I6-{gates.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,service-classes.txt,release.txt,final-checks.txt}`.
- I7: `$W/evidence/I7-scan.txt`.
- AC-12 (orchestrator, section 9): `$W/evidence/AC12-emulator.txt` and `$W/evidence/AC12-{iii,i,ii}-log.txt`.

5 code files and 53 workspace files from the developer (1 log, 6 tools, 7 + 1 + 2 + 2 + 1 + 25 + 7 + 1 = 46 evidence files,
the mutation log counted in the 25; plan-review O-4 recounted). Never staged for this story: the story file, the sprint plan, other stories' workspaces
(U-070's, U-071's), `compiled-pipeline.json`, `sdlc/.skill-context.json`, any design, contract or change record (CR-017 is
the orchestrator's own work in progress).

### 4.10 Sessions and hand-over

**One session** is planned (16 Gradle runs: two full gates of about 10 minutes each, likely in the background, and 14
targeted runs; U-069 ran 18 in one session). Safe stopping points, each followed in the next session by the resume checks
below (never while a Gradle run is in flight):
- after I0 (baseline frozen; next step I1);
- after I2 (red evidence frozen; no production edit; next step I3);
- after I4 (fix in place, the new classes green, pristine copies and blobs stored; next step the first I5 entry);
- between two I5 entries (each finished entry has its `restored` line);
- before I6 (every entry closed).

**Resume checks** (no Gradle): `git rev-parse HEAD` and `git log --oneline $START..HEAD` (a code commit by anyone is row
D23); `git status --porcelain --untracked-files=all -- app` as the stopping point leaves it; after I4: `sha256sum` of the
three files = `SB SR SF` and `PROD_DIFF` unchanged; `mutation-log.md` has no `APPLYING` entry without `restored` (the two
anchored counts of I6 step 0 are equal); rebuild
`$WORK/pristine/` in the new session's work folder from `HB`, `HR`, `HF` as the recovery note says and compare by
`sha256sum` (never `cmp`). The hand-over block in the implementation log gives: `Session <n> ends at <date -u>`, the last
finished step, the next step, `START`, `C0`, `T0`, `S0`, `SB SR SF`, `HB HR HF`, `PROD_DIFF`.

## 5. Tests and Counts

### 5.1 The 18 added tests

| # | Test (class) | Criterion | Red at Start | Detected by |
|---|--------------|-----------|--------------|-------------|
| F1 | `manual_foregroundRefused_continuesAndBacksUp` (BFT) | AC-1, AC-2 | FAIL, X `ForegroundServiceStartNotAllowedException` | M-F1a, M-F5, M-F6 |
| F2 | `manual_foregroundAllowed_backsUp_control` (BFT) | AC-1 control, AC-2 privacy, P-1 probe | PASS | none by design (a control: it passes under every injection) |
| F3 | `skip_foregroundRefused_sameOutcomeAsAllowed` (BFT) | AC-1, AC-2 | FAIL, X (after the allowed half passes) | M-F1a, M-F5, M-F6 |
| F4 | `manual_foregroundRefusedThroughFailedFuture_continuesAndBacksUp` (BFT) | AC-1, AC-2; item 3 | FAIL, X | M-F1a, M-F5, M-F6 |
| F5 | `manual_foregroundRefusedThroughPendingFuture_continuesAndBacksUp` (BFT) | AC-1, AC-2; item 3 | FAIL, X | M-F1a, M-F5, M-F6 |
| F6 | `automaticTypes_doNotRequestForeground` (BFT) | AC-3 (a) | FAIL, A "INCOMING: foreground requests" (1, not 0) | M-F4 |
| F7 | `automaticTypes_foregroundRefused_runAsToday` (BFT) | AC-3 (b) | FAIL, X (INCOMING case) | M-F4 |
| F8 | `userStartedTypes_requestForegroundOnce` (BFT) | AC-4 | PASS | M-F5 |
| F9 | `foreground_otherIllegalState_propagates` (BFT) | AC-5 (a) | PASS | M-F2, M-F5 |
| F10 | `foreground_typeException_propagates` (BFT) | AC-5 (b) | PASS | M-F2, M-F3, M-F5 |
| F11 | `foreground_securityException_propagates` (BFT) | AC-5 (c) | PASS | M-F5 |
| F12 | `foreground_cancelled_endsCancelled` (BFT) | AC-5 (d) | PASS | M-F2, M-F5 |
| F13 | `manual_atApi24_otherIllegalState_propagatesWithoutLinkageError` (B24) | AC-5 (a) at API 24; class loading | PASS | M-F2, M-F5 (M-X1 informational) |
| R1 | `restore_requestsForegroundOnce` (RFT) | AC-6 | PASS | M-F7 |
| R2 | `restore_foregroundRefused_continues` (RFT) | AC-6, AC-2 | FAIL, X | M-F1b, M-F6, M-F7 |
| R3 | `restore_foreground_otherIllegalState_propagates` (RFT) | AC-6 (AC-5 (a)) | PASS | M-F2, M-F7 |
| R4 | `restore_foreground_typeException_propagates` (RFT) | AC-6 (AC-5 (b)) | PASS | M-F2, M-F3, M-F7 |
| R5 | `restore_foreground_cancelled_endsCancelled` (RFT) | AC-6 (AC-5 (d)) | PASS | M-F2, M-F7 |

### 5.2 Red-run expectation (I2)

`cases=18 pass=11 fail=7 error=0`. Failing: F1, F3, F4, F5, F7, R2 with type `android.app.ForegroundServiceStartNotAllowedException`
(today `doWork` throws the refusal at `BackupWorker.kt:123` and `RestoreWorker.kt:148`, outside their `try`), and F6 with
Truth's assertion "INCOMING: foreground requests" (today 1 call). Passing: F2 and F8 to F13 and R1, R3 to R5. The passes at
SDK 35 are P-1's probe: the sandbox, U-070's fixtures (F2, F8) and the type exceptions (F10, R4) work offline at 35.

### 5.3 Counts (REQ-MODERNIZATION-015 AC-13 as amended by CR-016; AC-10)

- Start: `C0` by the authoritative command at `<start>` with its commit id (I0 step 2). It includes U-069's and U-070's
  tests (not this story's).
- Added: the 18 tests of 5.1, by name. Deleted: none. Changed: none (`git diff --quiet $START -- app/src/test` exit 0).
- End: `C0 + 18` by the working-tree form with `--untracked` (I6 step 3), stated as such; after the orchestrator's commit
  the same command with that commit in place of `HEAD` (hand-over item 1).
- Gradle: `tests = T0 + 18`; failures 0; errors 0; skipped not above `S0` (and not above 2). Tests from another change
  after `<start>` are listed by name with their commit and not counted (row D22).

## 6. Criterion-to-Step Map

| Criterion | Where it is met | Evidence |
|-----------|-----------------|----------|
| AC-1 | F1, F3 (and F4, F5); I2 red, I3 green; M-F1a, M-F5 | `I2-red-cases.txt`, `I3-green-cases.txt`, mutation log |
| AC-2 | F1, F3, F4, F5, R2 (warning once, level, tag, text, no throwable, no ERROR; F1, F2, F3 privacy over all entries); M-F6; I0/I6 `assumenosideeffects` | same; `I0-start.txt`, `I6-final-checks.txt` |
| AC-3 | F6, F7 (four cases each); M-F4 | same |
| AC-4 | F8; I3 hunks show no line of `createBackupForegroundInfo()` changed; M-F5 | `I3-checks.txt` |
| AC-5 | F9 to F13; M-F2, M-F3, M-F5 | mutation log |
| AC-6 | R1 to R5; M-F1b, M-F7 | mutation log |
| AC-7 | Section 2.5 (recorded reading; stop condition not met), copied to the implementation log; device step (v) not run (SC-3) | implementation log |
| AC-8 | I3 step 3 and I6 step 5 (file list, hunks, porcelain, tracked tests unchanged, build/design list empty) | `I3-checks.txt`, `I6-final-checks.txt` |
| AC-9 | G0, G-F exit 0 (the two full gates); JaCoCo per class and package at I6; no floor change | `I0-/I6-gates.txt`, `I0-/I6-service-classes.txt` |
| AC-10 | 5.3; I0 step 2, I6 step 3 | `I0-start.txt`, `I6-final-checks.txt` |
| AC-11 | I5 (table 4.8) | `mutation-log.md`, `mutations/` |
| AC-12 | Section 9 (orchestrator) | `AC12-*.txt` |
| IC-1 | I3 step 3 and I6 step 5 greps (one call, three helper lines); constructors and factories unchanged (`oldtext_check.py` `CTOR-B`, `FACTORY-R`) | same |
| IC-2 | Section 2.7 line for U-074; implementation log | implementation log |
| IC-3 | F8, R1 (doWork reached through the builder); device step (iii) from `scheduleManual` | same; `AC12-emulator.txt` |
| P-1 | 2.3, L12, L13; I2 probe; cache listings equal (I2, I6) | `I2-red-cases.txt`, logs |
| P-2 | 2.5 | implementation log |
| P-3 | I0 `oldtext_check.py start` (`FX-*`, anchors); I0 step 3 | `I0-start.txt`, `I0-u070-mutations.txt` |

## 7. Decision Table

| Row | Observation | What the developer does |
|-----|-------------|-------------------------|
| D1 | HEAD is not a descendant of `$PLAN_HEAD` | Stop and report; nothing has been edited |
| D2 | U-070 or U-069 is not `done`, or no U-070 code commit after `$PLAN_HEAD` | Stop before any edit (start condition 1) |
| D3 | Porcelain shows a change under the checked paths at I0 | Stop; never stash, restore or commit someone else's change |
| D4 | `oldtext_check.py start` reports a mismatch (an anchor not once, a U-070 fixture name different, a helper name already present); or the Edit tool reports an `old_string` not found or not unique | Stop and report "plan text no longer matches <file>" with the tool output and `git log --oneline $PLAN_HEAD..HEAD -- <file>`. The developer never adapts an anchor or a test: the SA writes a re-anchoring amendment from the start-state text |
| D5 | A worker's start blob is not `ba667c43…` / `dc84c4f5…` (U-070's session-1 blobs, equal to the simulation) while the text check passes | Record the blob (U-070 changed the file again after session 1, for example in a review fix); the fixed blobs of I3 are then not predicted; the numstat, hunk and text checks still apply |
| D6 | (unused) | — |
| D7 | G0 fails, or failures or errors above 0 | Stop and report (start condition 2) |
| D8 | `S0` above 2 | Record as pre-existing; AC-9 compares with `S0` |
| D9 | The cache listing lacks the 35, 34 or 24 i7 jar | Without 24: stop (F13 cannot run offline). Without 35 but with 34: apply F-34 at I1 and say so. Without both: stop and report (no level of 31 or more offline, P-1) |
| D10 | I2: the SDK 35 classes do not run as 5.2 says | (a) A message names a missing SDK jar, a download ("Failed to fetch maven artifact", a URL), or the cache listing changed: stop and report; never allow a download. (b) Every case of `$BFT` and `$RFT` errors with the same exception from the sandbox or the application's start-up (not from the test body) while `$B24` passes, or only F2 and F8 fail inside U-070's fixtures: apply F-34 (two Edits; blobs `0145ee38…`, `9207950d…`), run I2 once more. If SDK 34 shows the same: stop and report (31 to 33 are not cached; the orchestrator decides, the story's last P-1 rule) |
| D11 | The red run differs from 5.2 in another way | (a) A listed FAIL passes: stop (the test does not detect the defect). (b) A listed PASS fails: stop and report with the message. If it is F2 failing at "a log entry with tag … carries a fixture value", an existing line writes a fixture value: report its tag and which value class (body, address, account, folder) without copying the value; the orchestrator rules (ruling 39 precedent). (c) F13 errors with `NoClassDefFoundError` or `VerifyError` at class load: stop and report (N3). (d) A failure of another type (for example `ContactsProviderDouble: unexpected query`): stop and report |
| D12 | A test file does not compile | Stop and report the compiler message (the files were compiled against stubs only); the developer does not change the test |
| D13 | A tool or test blob differs from the plan after the Write | Compare with 3.4/3.5 and correct until the blob matches; never run a variant |
| D14 | The green run (I3) has a failure | Stop and report; never change an assertion to fit |
| D15 | `oldtext_check.py fixed` mismatch, numstat or hunks differ, or the porcelain is not the three paths | Re-read E-B1, E-B2, E-R1 and the helper against the file and correct until they match; if the plan's text is the cause, stop and report |
| D16 | JaCoCo at I6 | (a) `WorkerForegroundKt` or `ForegroundRefusal` with `missed` above 0, or `service` below `C_s/T_s`, or any floor failing: stop and report (AC-9; no floor is changed). (b) Totals other than 10 and 2 with `missed=0`: record (a different line table); not a stop |
| D17 | A mutation clean check fails | Stop; apply the recovery note; never run a mutation on a dirty tree |
| D18 | A counted mutation's result | (a) An expected-failing case passes: undetected; record, stop and report (AC-11 unmet). (b) Extra failing cases: record and explain each from the mutant; stop if one cannot be explained. (c) It does not compile: log `invalid (does not compile)` with the message; apply its listed replacement once (4.8), or stop if none is listed |
| D19 | M-X1's result | Record which of the two outcomes occurred; not a stop. If F13 still passes, record that the class-loading proof on API 24 to 30 rests on the bytecode (L19, I6 dex check) and the optional device step (vi) |
| D20 | U-070's re-runs | The set observed over NEW5 must equal U-070's recorded set (I0 step 3) and FG3 must all pass. A different NEW5 set: stop and report. An FG3 failure: record and explain; stop if unexplained. U-070's built anchor text differs from its plan: use the text in its mutation log and record it |
| D21 | G-F fails | Stop and report |
| D22 | End count not `C0 + 18`, Gradle tests not `T0 + 18`, or skipped above `S0` | Record and explain; a test added by another change after `<start>`: list it with its commit id, not as this story's |
| D23 | A build file listed, or a commit after `$START` touching `app`, `k9mail-vendored`, `build.gradle`, `settings.gradle` or `gradle`; or `docs-new-or-changed-exit` is 0 (a document path under `sdlc/artifacts/design`, `requirements` or `build/change-records` that is new or changed since I0 and uncommitted) | Build file or code commit: stop and report. Documents (the orchestrator's ruling on contract O-6): a path already uncommitted at I0 with the same hash at I6, or committed in between (its commit in the `git log` lines with the orchestrator's subject), is recorded and is not a stop; a path new or changed since I0 and still uncommitted: stop and report its path (it may be another agent's edit; the developer never touches it and the orchestrator decides). The developer edits no document in these folders |
| D24 | The release APK or the mapping lines are not as I6 step 4 says | Another APK name: use it and record it. A mapping line missing: stop and report (the class would be absent from the release build) |
| D25 | In the **debug** dex a class under `Lcom/zegoggles/` other than `ForegroundRefusal` (or a synthetic class named after it) names the refusal's type; or in the **release** dex `caller-lines` is above 0, `holder-lines` is not 1, or another app class names the type (R8 merged, inlined or outlined the holder) | Record the class and report it in the hand-off; not a stop (the `SDK_INT` test still guards execution, P3). The orchestrator then runs device step (vi) on the build concerned (for a release finding with a release APK, or records why it cannot) |
| D26 | A cache listing differs from `aai-before.txt` | Stop and report (something was downloaded) |
| D27 | The hygiene scan has a line | Fix the file through `sanitize.py`, re-scan; never hand off with a hit |
| D28 | "bash.exe … not found", or a process killed without a visible reason | Stop and report (endpoint security); do not work around it |
| D29 | The tool moves a Gradle run to the background (expected for G0 and G-F); or a Gradle log's last line is not `exit=<n>` | Background: wait for its completion notice; never poll; start nothing else, Gradle above all, meanwhile. A log without `exit=`: the run was cut off. **Inside an I5 entry** (plan-review O-1) first finish protocol steps 5 and 6 (restore from the pristine copy, clean check, `restored` line), then stop and report. Everywhere: never rerun on a guess, never kill a daemon |
| D30 | A `setForeground(` or `setForegroundOrContinue` grep differs from I0 step 2 or I3 step 3 | Same texts at other line numbers: record. Another call of `setForeground(` in `app/src/main`, or a fourth `setForegroundOrContinue` line: stop and report |
| D31 | `assumenosideeffects` prints a line | Stop and report (AC-2: the warning would not survive release builds) |
| D32 | I6 step 0 or step 5: a hash of `$BW`, `$RW`, `$WF` differs from `SB SR SF` / `HB HR HF`, `PROD_DIFF` differs, an `APPLYING` entry has no `restored` line, or the porcelain is not the five paths | Stop. G-F (if it ran) is void: the tested tree is not the gated one. Apply the recovery note of 4.8 to restore `SB SR SF`, record what was found, report; never hand off (AC-11: a mutation is never present at a gate) |
| D33 | I6 step 5: `test-tracked-unchanged-exit` is not 0 (an existing test changed), `ignore-exit` is not 1 (`@Ignore` or `Assume` in a new test), or the `@Test` counts are not 13 and 5 | Stop and report (AC-8, AC-9); do not edit to fit |
| D34 | I0: `toolchainVersion` is not 25, `./gradlew --version` does not name the criteria file and Java 25, a Gradle JVM is listed before `jps-gradle-end`, or the `@Ignore` lines differ from the two of `fd70325a` | Stop before any Gradle call and report. A running Gradle JVM: start nothing (another build may run in this checkout; D29's rule). A wrong JDK: AC-9's start condition. Other `@Ignore` lines: record them as pre-existing and report (not this story's) |
| D35 | I1 step 2 or I4 step 1: the porcelain is not the stated paths, `main-clean-exit` is not 0, or `oldtext_check.py fixed` is not `mismatches=0` at I4 | Stop and report; do not edit (a change by someone else, or a step not applied as written) |

## 8. Contracts, Standards, Risks, Scope Concerns

### 8.1 Contracts and design (approved; read, not edited)

- **CNTR-MODERNIZATION-004** (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`, approved; v3 at
  HEAD, Validation Rule 8 at `:280` "the manual notification/foreground semantics"; the working tree carries the
  orchestrator's uncommitted v4 under CR-017, where Rule 8 is at `:320`). The story keeps the manual semantics and changes
  no scheduling; a refused promotion no longer fails the run, which meets the non-retryable-failure rule better. CR-017
  (working tree, `:247-249`) records that Rule 8, DES-MODERNIZATION-005 `:54`/`:129-131` and REQ-016 `:49-53` stay true
  after U-075. No contract change is needed; `integration_contracts: []` in the story is consistent with that.
- **DES-MODERNIZATION-014** row 16-1 (Android 16 behaviour-change table; HEAD `:1275`): "long work runs as a dataSync
  foreground service". **Correction (iteration 2, contract-verification B-1 and the orchestrator's ruling):** the story's
  claim (Integration Contract References) and CR-017 E-3's that no contract or design text states that automatic backups
  run in the foreground are not true: this cell does, and it becomes false when this story is built. It is corrected by
  CR-017 Amendment 2 (edit E-7), with the close-out edit X-3 that the orchestrator applies when this story closes (section
  10 item 9). This story still edits no design text (AC-8). CNTR-MODERNIZATION-005 `:213` ("notification/state channels")
  stays true through the state channel, and CNTR-MODERNIZATION-006 is silent on the foreground (contract-verification
  statement table rows 7 and 8); both added to `contracts_verified`.
- **DES-MODERNIZATION-005** (approved; `:54` "owns worker lifecycle plus foreground promotion", `:129-131` "where a
  user-visible notification is required it uses `setForeground`"; at `:55`, `:130-132` in the working tree): both hold.
  The bug's optional clarification is the orchestrator's (hand-over item 4).
- **REQ-MODERNIZATION-013 AC-8**: decision recorded in 2.1. **REQ-MODERNIZATION-005** ("preserving all current scheduling
  behavior"): AC-8. **REQ-MODERNIZATION-015 AC-13 / CR-016**: 5.3. No REQ with `type: standard` exists (U-069 plan 12, `git
  grep`; not re-run).

### 8.2 Scope Concerns (ruled: Record 23, the story's Implementation Notes; the plan follows the rulings)

Iteration 1's recommendations are kept below for the record; the ruled text governs, and section 9 follows it (SC-1:
no screenshot or UI dump in the evidence, no outcome on one; SC-2: the orchestrator's run on `Pixel_API_24` where it can
be done without an account and a password, else "not run: needs the owner to enter the app password"; SC-3: "not run:
needs a system setting"; SC-4 to SC-8: accepted as recommended; SC-9: no mailbox check, the confirmed-append line is the
evidence).

| # | Concern | Recommended ruling (iteration 1) |
|---|---------|----------------------------------|
| SC-1 | AC-12 asks for the screenshots in the story's evidence folder, but the status screen shows the owner's e-mail address and a shade screenshot may show part of it | Screenshots and UI dumps stay in the orchestrator's work folder outside the repository; the evidence records each screenshot's SHA-256, what it shows, and the sanitised `dumpsys notification` lines for the package (section 9) |
| SC-2 | The class-loading proof on ART 7.0 is not in AC-12 | Allow optional step (vi) on `Pixel_API_24` (install, launch, one manual backup, logcat for `VerifyError`/`NoClassDefFoundError`); informational; required only under row D25 |
| SC-3 | AC-12 (v) needs airplane mode, a device setting the same criterion forbids changing | Not run unless the owner approves it explicitly; AC-7 rests on 2.5 and F4/F5 |
| SC-4 | AC-2's literal privacy check would fail on the existing line `PersonLookup.java:76` for an unknown sender (ruling 39's bug) | Accept the fixture's known contact; the existing line stays ruling 39's bug |
| SC-5 | The story's middle P-1 fallback (31 to 33) is not available offline (no cached i7 jar) | Accept; 35 then 34, else stop, as the story's last rule says |
| SC-6 | The plan is stricter than the story: F2, F4, F5, F13, M-F7, M-X1, the dex check | Accept |
| SC-7 | Shared test helpers live at the top level of `BackupWorkerForegroundTest.kt`, which also holds the API 24 class (still two new test files) | Accept |
| SC-8 | The notification's title is "Backing up" (`strings.xml:24`), not "Backup" as AC-12 and AC-4 say | Read the story's "Backup notification" as the notification with id 1 on `sms_backup_plus`, title "Backing up"; corrected at close-out |
| SC-9 | AC-12 (i) asks the orchestrator to check the mail account's SMS folder, which needs the owner's mailbox | Ask the owner to confirm; otherwise record the confirmed-append line `watermark advanced for SMS to <date>` (BUG-010: written only after the server confirmed the append) as the evidence, stated as such |

### 8.3 Risks

| # | Risk | Likelihood / impact | Mitigation |
|---|------|--------------------|------------|
| R1 | Test files do not compile on the real classpath (stubs used) | Low / medium | Scratch compile with the real library jars; D12 stops for an amendment |
| R2 | Robolectric at 35 behaves differently for the app (start-up, providers); U-070's fixtures run at SDK 24 for the first time in F13 (plan-review O-9) | Low-medium / medium | I2 probe (F2, F8, R1, F13); F-34; D10, D11 (c), (d) |
| R3 | U-070 built its fixtures or anchors differently from its plan | Medium / low | `oldtext_check.py start` (`FX-*`); D4 amendment |
| R4 | A download by Robolectric | Low / high (policy) | Local jars verified (L13); cache listing compared (D26) |
| R5 | ART on API 24-30 meets the API-31 type | Very low / high | Holder class (L19); F13; M-X1; dex check; optional (vi) |
| R6 | An existing log line carries a fixture value | Low / low | F2 at I2 shows it before the fix; D11 (b) |
| R7 | Stack-trace recovery copies the exception | Medium / none | The helper and the tests use class, not identity (P1) |
| R8 | `service` coverage falls | Very low / medium | All 12 new lines covered by design; D16 stops |
| R9 | A refused manual run is not stoppable by the system (L10) | Rare / low | Accepted trade (ruling 60); BUG-029 Not verified 5 stays |
| R10 | Automatic backups now meet the system's stops as ordinary jobs: the time limit (on Android 11 and earlier at 10 minutes whatever the load, recalled), Doze, a lost constraint, quota; run size not bounded (`MAX_ITEMS_PER_SYNC = -1`) | Medium on Android 11 for large runs / low | Watermark; reschedule; the next run continues above the watermark; U-074 removes the interim ERROR line (2.6 item 3) |
| R11 | Device step (i) starts within a platform grace period after Home | Medium / low (a false pass) | The 58 s rule and the top-activity check (section 9) |
| R12 | Device step (ii) held by job quota | High / none | "Not reached", not a pass (story) |
| R13 | `TestForegroundUpdater` changes in work-testing 2.12.0 | Low / low | Hand-over item 6 to U-073 |
| R15 | A `dumpsys` output format on the emulator differs from the recalled one (`dumpsys notification` records, process and service lines) | Medium / low (a false stop or a "not determined") | `notif_check.py` refuses a dump without its marker; (i)'s "no record" counts only when (iii) parsed a record in the same session; process and service lines are recorded raw and decide no pass on their own; tools tested on constructed samples (14.3, 14.19) |
| R14 | A full gate passes the foreground limit and is cut off or overlapped by a second Gradle | High (moved) / medium | Two full gates only; `exit=` written by the command itself; wait for the notice; D29 |

### 8.4 Out of scope (each ruled or recorded)

BUG-025's delay, BUG-026/027's schedule, removing the SMS receiver, U-074's cancellation reporting, a battery-optimisation
prompt (PO-L), a notification for automatic backups (PO-K), any scheduling, manifest, build, contract or design edit.

## 9. AC-12 — Emulator Check (for the orchestrator; the developer does not run it)

Iteration 2: rewritten to the rulings in the story's Implementation Notes (Record 23, SC-1 to SC-9) and to plan-review
B-2 to B-5, O-7, O-8, contract-verification O-2 and the depth check's section 9 recommendations. Every parsing step uses
a tool of 3.5 that was run on sample input (section 14).

**Preconditions.** The developer's hand-off names `app/build/outputs/apk/debug/app-debug.apk` with its size and SHA-256
(from G-F), and no mutation entry lacks `restored`. Conditions: only the AVDs `Pixel_9_Pro` and, for (vi), `Pixel_API_24`
are used; `Pixel_10_Pro` is never started or touched; nothing but the app's own state changes (installing it, opening and
leaving it, test SMS through the emulator console, a cold start of the emulator); no device setting, permission, battery
exemption or standby-bucket change (`dumpsys`, `getprop` and `am get-standby-bucket` are read-only and allowed); the app
is never uninstalled and its data never cleared; nobody but the owner handles a password. **Evidence (SC-1):** no
screenshot and no UI dump is taken into the repository, and no outcome rests on one; the evidence is the app's log lines
and the sanitised notification lines of `dumpsys notification`, read by the tools of 3.5. Raw files stay in the
orchestrator's work folder `OW` outside the repository; the UI dump is deleted from `OW` at the end.

**Shell rules.** `ADB="$SDK/platform-tools/adb"`; `TL=sdlc/artifacts/build/sprints/sprint-024/U-075/evidence/tools`;
`T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools`. **`MSYS_NO_PATHCONV=1` is put on each `adb` command that
carries a device path (`/sdcard/…`), on that command only, and never on Gradle**; without it Git Bash rewrites the path
into a Windows path before `adb` sees it (self-test 14.6). No other command below has a device path. **No `timeout`
value exceeds 540 s** and every call is a foreground tool call with timeout 600000 ms; a window longer than 540 s is two
waits of 450 s anchored at the same `-T "<device time>"`, so the second wait also finds a match the first one missed by
a second (`-T` prints the buffer from that time, `-m 1` stops at the first match). Device time:
`"$ADB" -s $SER shell "date '+%m-%d %H:%M:%S.000'" | tr -d '\r'` (written below as `$(dt)`, typed out in each call);
a wait on the device: `"$ADB" -s $SER shell sleep <n>`. App process ids for the log tools, after each log dump `<raw>`:
`PIDS=$({ grep -oE 'Start proc [0-9]+:com\.zegoggles\.smssync/' "<raw>" | grep -oE '[0-9]+'; "$ADB" -s $SER shell pidof com.zegoggles.smssync | tr -d '\r' | tr ' ' '\n'; } | grep -E '^[0-9]+$' | sort -u | paste -sd,)`
(`ac12_run.py` refuses an empty list: no step can pass on a log it did not read).

1. **Device, by AVD name.** `"$ADB" devices | grep -E '^emulator-[0-9]+[[:space:]]+device$' | cut -f1 | while read -r s; do echo "$s $("$ADB" -s "$s" emu avd name | head -1 | tr -d '\r')"; done`.
   `SER` is the one serial whose name is `Pixel_9_Pro` (for (vi): `Pixel_API_24`). None: ask the owner to start that
   AVD. A line `Pixel_10_Pro`: never send anything to that serial. A physical device or two serials with the wanted
   name: send nothing, ask the owner. Every later command uses `-s $SER` only.
2. `"$ADB" -s $SER shell getprop ro.build.version.sdk` → `36`;
   `"$ADB" -s $SER shell dumpsys package com.zegoggles.smssync | grep -E "versionCode|lastUpdateTime|android\.permission\.POST_NOTIFICATIONS: granted="`.
3. `sha256sum app/build/outputs/apk/debug/app-debug.apk` equals the hand-off; `"$ADB" -s $SER install -r app/build/outputs/apk/debug/app-debug.apk`
   → `Success`. Any other result: stop, "not run". Repeat step 2's line.
4. **Set-up, read only.** `"$ADB" -s $SER shell dumpsys deviceidle whitelist | grep -c com.zegoggles.smssync` → `0`;
   `"$ADB" -s $SER shell am get-standby-bucket com.zegoggles.smssync` (recorded). `POST_NOTIFICATIONS: granted=true` from
   step 2 is the precondition for (iii)'s notification condition; with `granted=false` (iii) uses the service record
   instead (below), and the record says so. Auto backup on, the 1-minute incoming delay and the regular schedule are read
   from the log lines of the steps (`enqueued INCOMING with delay=60s`), not from the settings screen.

**(iii) Manual backup, on screen** (first).
1. `D3=$(dt)`; `"$ADB" -s $SER shell am start -n com.zegoggles.smssync/.activity.MainActivity`; `"$ADB" -s $SER shell sleep 3`.
2. One test SMS so that the run has work: `"$ADB" -s $SER emu sms send 5550100 "U-075 iii"`; then
   `timeout 120 "$ADB" -s $SER logcat -T "$D3" -m 1 -e 'BackupTriggerWorker\.doWork: content-URI trigger fired' > "$OW/iii-trigger.txt"; echo "exit=$?"`
   (exit 0 expected within about 11 s; 124 is recorded and the step continues).
3. Button: `MSYS_NO_PATHCONV=1 "$ADB" -s $SER shell uiautomator dump /sdcard/u075-ui.xml`;
   `MSYS_NO_PATHCONV=1 "$ADB" -s $SER exec-out cat /sdcard/u075-ui.xml > "$OW/u075-ui.xml"`;
   `MSYS_NO_PATHCONV=1 "$ADB" -s $SER shell rm /sdcard/u075-ui.xml`;
   `python -I -X utf8 $TL/ui_bounds.py "$OW/u075-ui.xml" > "$OW/ui.txt"; echo "exit=$?"; read -r X Y REST < "$OW/ui.txt"; echo "X=$X Y=$Y"; grep -c 'enabled=true label-is-backup=True' "$OW/ui.txt"`
   → `exit=0`, two integers, `1`. Anything else: "(iii) not run: button not found" (no coordinate is guessed).
   (`exec-out uiautomator dump /dev/tty` was not used: it appends a status line to the XML.)
4. Tap, wait for the start, then read the notification and the service at once, in one command line:
   `"$ADB" -s $SER shell input tap $X $Y && timeout 60 "$ADB" -s $SER logcat -T "$D3" -m 1 -e 'BackupWorker\.doWork: starting backup, type=MANUAL' > "$OW/iii-start.txt" && "$ADB" -s $SER shell dumpsys notification --noredact > "$OW/iii-notification.txt" && "$ADB" -s $SER shell dumpsys activity services com.zegoggles.smssync > "$OW/iii-services.txt"; echo "exit=$?"`.
   A start wait of 124: a dialog (missing credentials, first sync, confirmation; `MainActivity.java:391-402`) or no tap;
   record "(iii) not run: no MANUAL start within 60 s of the tap"; press nothing in a dialog.
5. `timeout 300 "$ADB" -s $SER logcat -T "$D3" -m 1 -e 'Worker result [A-Z]+ for Work .*MANUAL' > "$OW/iii-result.txt"; echo "exit=$?"`;
   then the trigger's delayed INCOMING, which runs with the app still on screen:
   `timeout 240 "$ADB" -s $SER logcat -T "$D3" -m 1 -e 'Worker result [A-Z]+ for Work .*INCOMING' > "$OW/iii-incoming.txt"; echo "exit=$?"`.
6. `"$ADB" -s $SER logcat -d -v threadtime -T "$D3" > "$OW/iii-logcat.txt"`; `PIDS` from it;
   `python -I -X utf8 $TL/ac12_run.py "$OW/iii-logcat.txt" MANUAL --pids "$PIDS" > "$OW/iii-run.txt"`;
   `python -I -X utf8 $TL/ac12_run.py "$OW/iii-logcat.txt" INCOMING --pids "$PIDS" --after "$D3" > "$OW/iii-run-incoming.txt"`;
   `python -I -X utf8 $TL/notif_check.py "$OW/iii-notification.txt" > "$OW/iii-notif.txt"`;
   `grep -E "SystemForegroundService|isForeground=|foregroundId=" "$OW/iii-services.txt" > "$OW/iii-service-lines.txt"`.
- **Pass:** `iii-run.txt` has `start` not `none`, `result=SUCCESS`, `moved=1` (or more), `refused-warn=0`, `defect=0`;
  and either `iii-notif.txt` = `dump=ok records=… id1=1 id1-channel=True id1-title-backing-up=True` (title "Backing up",
  SC-8), or, when `POST_NOTIFICATIONS` is not granted, `iii-service-lines.txt` holds `isForeground=true` and
  `foregroundId=1` for `SystemForegroundService` (the service-record format is recalled, not read on this emulator).
  Recorded besides: `iii-run-incoming.txt` `moved=0` (an automatic run does not ask, even on screen).
- **Not conclusive:** the run line passes but the notification was not captured (`id1=0`: the run ended before the
  dump). Repeat steps 1 to 6 once; a second "not conclusive" is recorded "(iii) not run: notification not captured"
  (plan-review O-8), never a pass.
- **Fail:** `moved=0` for the MANUAL work, `refused-warn` or `defect` above 0, or a result other than SUCCESS.

**(i) Incoming backup, off screen.**
1. `D1=$(dt)`; `"$ADB" -s $SER shell am start -n com.zegoggles.smssync/.activity.MainActivity`; `"$ADB" -s $SER shell sleep 3`;
   `"$ADB" -s $SER shell input keyevent KEYCODE_HOME`; `HOME_T=$(dt)`;
   `"$ADB" -s $SER shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" > "$OW/i-top-home.txt"`
   (the launcher, not `com.zegoggles.smssync`).
2. `"$ADB" -s $SER emu sms send 5550100 "U-075 test"`; `SMS_T=$(dt)`.
3. Wait for the start, then record the process at once (two waits of at most 450 s, the second only after `exit=124`):
   `timeout 450 "$ADB" -s $SER logcat -T "$D1" -m 1 -e 'BackupWorker\.doWork: starting backup, type=INCOMING' > "$OW/i-start.txt" && { "$ADB" -s $SER shell dumpsys activity processes com.zegoggles.smssync > "$OW/i-proc.txt"; "$ADB" -s $SER shell dumpsys activity services com.zegoggles.smssync > "$OW/i-services.txt"; "$ADB" -s $SER shell dumpsys notification --noredact > "$OW/i-notification.txt"; "$ADB" -s $SER shell dumpsys jobscheduler > "$OW/i-jobs-at-start.txt"; "$ADB" -s $SER shell dumpsys activity activities | grep -E "topResumedActivity|mResumedActivity" > "$OW/i-top-start.txt"; }; echo "exit=$?"`.
4. `timeout 300 "$ADB" -s $SER logcat -T "$D1" -m 1 -e 'Worker result [A-Z]+ for Work .*INCOMING' > "$OW/i-result.txt"; echo "exit=$?"`.
5. Both start waits ended 124 (no start within 15 minutes of `D1`): first run step 6. **A refused run of an unfixed build
   writes no start line** (BUG-029; today's log, self-test 14.7: `start=none`, `defect=2`), so `defect` above 0 in
   `i-run.txt` is a **fail** here, before any other reading. Otherwise `"$ADB" -s $SER shell dumpsys jobscheduler > "$OW/i-jobs.txt"`:
   the app's job lines with `WITHIN_QUOTA` unsatisfied give "(i) not run: held by the system's job quota"; another
   reason gives "(i) not reached: <the reason line>". Repeat (i) once from step 1.
6. `"$ADB" -s $SER logcat -d -v threadtime -T "$D1" > "$OW/i-logcat.txt"`; `PIDS` from it;
   `python -I -X utf8 $TL/ac12_run.py "$OW/i-logcat.txt" INCOMING --pids "$PIDS" --after "$HOME_T" > "$OW/i-run.txt"`;
   `ST=$(grep -oE 'start=[^ ]+' "$OW/i-run.txt" | cut -d= -f2 | tr '_' ' ' | tr -d '\r'); python -I -X utf8 $TL/dt_secs.py "$HOME_T" "$ST" > "$OW/i-secs.txt"; echo "exit=$?"`
   (with `start=none` the tool prints `time=unreadable` and the step has no start);
   `python -I -X utf8 $TL/notif_check.py "$OW/i-notification.txt" > "$OW/i-notif.txt"`;
   `grep -E "procState|ProcState|curAdj|setAdj" "$OW/i-proc.txt" > "$OW/i-proc-lines.txt"`;
   `grep -E "SystemForegroundService|isForeground=|foregroundId=" "$OW/i-services.txt" > "$OW/i-service-lines.txt"`;
   `grep -E "com\.zegoggles\.smssync" "$OW/i-jobs-at-start.txt" > "$OW/i-job-lines.txt"`.
- **Off-screen rule.** The step counts only if `i-secs.txt` gives at least `seconds=58.000` (the interval of today's
  observed refusal; a shorter one may fall inside a platform grace period after leaving the screen, BUG-029 Not verified
  2), `i-top-home.txt` and `i-top-start.txt` do not name `com.zegoggles.smssync`, and `i-proc-lines.txt` does not name
  `TOP` (the format of the process lines is recalled, not read; they are recorded as the process state at the start).
  Expected: the trigger about 11 s after the SMS plus the 60 s delay, so about 70 s or more after Home. Shorter: "not
  conclusive: started within 58 s of Home"; repeat once.
- **Pass:** `i-run.txt` has `start` not `none`, `result=SUCCESS`, `moved=0`, `watermark=1` or more (the confirmed-append
  line `BackupWorker: watermark advanced for SMS to <date>` between that work's start and its result, SC-9), `backedUp=1`
  (more if other SMS were pending; recorded), `refused-warn=0`, `defect=0`, `svc-refusal=0`; `i-notif.txt` =
  `dump=ok records=… id1=0 …` (no record with id 1 for the app), with `iii-notif.txt` of the same session having parsed a
  record of the app (so the dump format is the one the tool reads); `i-service-lines.txt` has no `isForeground=true`.
- **Stopped by the system** (not a pass, not a fail): `unrecoverable-cancel=1` or more and `result=none`. Until U-074 a
  system stop of an ordinary job reaches `doWork`'s broad catch, which writes "BackupWorker: unrecoverable error" with
  the job's `CancellationException` and returns a result WorkManager ignores (2.6 item 3). Recorded with the lines;
  repeat (i) once.
- **Fail:** `moved` above 0, `refused-warn` above 0 (an automatic run asked), `defect` above 0, `result=FAILURE`, or
  `unrecoverable` above `unrecoverable-cancel` (an error that is not a cancellation).
- Which scheduler started the work: WorkManager's own lines for it ("onStartJob for", "Starting work for") are debug
  lines, and the app's WorkManager configuration logs from INFO (`App.java:162-165`, no `setMinimumLoggingLevel`; L29), so
  they are not in the log. `i-job-lines.txt` (the app's lines in the scheduler dump at the start) is recorded instead;
  the record says "JobScheduler" if it shows the app's `SystemJobService` job as running, else "not determined". No
  outcome depends on it.

**(ii) Boot backup, the app never opened.**
1. `"$ADB" -s $SER emu kill`; start the emulator with the tool's background mode (never `&`):
   `"$SDK/emulator/emulator" -avd Pixel_9_Pro -no-snapshot-load -no-snapshot-save`. Select `SER` again (step 1 of the
   preconditions; the serial may change).
2. `timeout 540 "$ADB" -s $SER wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 2; done'; echo "exit=$?"`
   (124: repeat once); `BOOT_T=$(dt)`. Do not open the app, do not touch the screen.
3. `timeout 120 "$ADB" -s $SER logcat -m 1 -e 'scheduleBootup: enqueued bootup REGULAR with 60s delay' > "$OW/ii-bootup.txt"; echo "exit=$?"`
   (the log since boot). 124: "(ii) not run: no bootup line (a resumed snapshot or auto backup off)" (plan-review O-7).
4. Two waits of at most 450 s for the start, as in (i) step 3 without `-T` and with `type=REGULAR`, the dumps into
   `ii-proc.txt`, `ii-services.txt`, `ii-jobs-at-start.txt`; then
   `timeout 300 "$ADB" -s $SER logcat -m 1 -e 'Worker result [A-Z]+ for Work .*REGULAR' > "$OW/ii-result.txt"; echo "exit=$?"`.
5. `"$ADB" -s $SER logcat -d -v threadtime > "$OW/ii-logcat.txt"`; `PIDS` from it;
   `python -I -X utf8 $TL/ac12_run.py "$OW/ii-logcat.txt" REGULAR --pids "$PIDS" > "$OW/ii-run.txt"`;
   `ST=$(grep -oE 'start=[^ ]+' "$OW/ii-run.txt" | cut -d= -f2 | tr '_' ' ' | tr -d '\r'); python -I -X utf8 $TL/dt_secs.py "$BOOT_T" "$ST" > "$OW/ii-secs.txt"; echo "exit=$?"`. No start: `"$ADB" -s $SER shell dumpsys jobscheduler > "$OW/ii-jobs.txt"`
   and `"$ADB" -s $SER shell am get-standby-bucket com.zegoggles.smssync`.
- **Pass:** the bootup line; a start at most 900 s after `BOOT_T`; `result=SUCCESS`, `moved=0`, `refused-warn=0`,
  `defect=0`, `svc-refusal=0`.
- **Fail first:** with no start, `defect` above 0 in `ii-run.txt` (a refused REGULAR run of an unfixed build writes no start
  line; today's Run 1) or a `Worker result FAILURE … REGULAR` line in `ii-result.txt` is a fail, before "not reached".
- **Not reached:** no start in the window and the app's job lines show `WITHIN_QUOTA` unsatisfied: "not reached: held
  by the system's job quota (BUG-025)", with that line (expected: today's cold boot left the app in bucket 50 with its jobs
  held until it was opened). Another reason: "not reached: <the reason line>". Neither is a pass or a fail; the QA record
  states that the boot case rests on (i) and AC-3.
- **Stopped by the system:** as in (i). **Fail:** as in (i), for the REGULAR work.

**(iv) No refusal off screen.** Assessed only over the off-screen runs that were observed: (i)'s INCOMING run if it met
the off-screen rule, and (ii)'s REGULAR run if it started (plan-review B-3). **Pass:** at least one such run, and in each
of their `ac12_run.py` lines `refused-warn=0`, `defect=0`, `svc-refusal=0` (the last is WorkManager's own refusal point
in `SystemForegroundService$Api31Impl`, L29). With none: "(iv) not run: no off-screen run observed". A count above 0:
**fail**.

**Telling the two refusal lines apart** (contract-verification O-2; also for U-071's IC-4 and U-073's device checks):
- refused and continued, the app's own warning: `grep -E ' W SMSBackup\+: (BackupWorker|RestoreWorker): foreground start not allowed; continuing without the foreground \(ForegroundServiceStartNotAllowedException\)' <log>`;
- the defect (a refusal that escaped a worker, as in BUG-029): `grep -E ' E WM-WorkerWrapper: .*ForegroundServiceStartNotAllowedException' <log>`.
A bare search for the class name matches both. `ac12_run.py`'s `refused-warn` and `defect` count exactly these two
(self-test 14.7).

**(v)** Not run: needs a system setting (SC-3).

**(vi) API 24 (SC-2).** After (ii): `"$ADB" -s $SER emu kill` (the `Pixel_9_Pro` serial); start `Pixel_API_24` in the
background (`"$SDK/emulator/emulator" -avd Pixel_API_24 -no-snapshot-save`); select its serial by name (step 1);
`getprop ro.build.version.sdk` → `24`; `D6=$(dt)`; `install -r` the same APK (a first install if the app is not there:
the app's own state). Then:
1. Install-time verification: `"$ADB" -s $SER logcat -d -v threadtime -T "$D6" > "$OW/vi-install.txt"`;
   `grep -E "ForegroundRefusal|WorkerForegroundKt|VerifyError|failed verification" "$OW/vi-install.txt"` (recorded; a hard
   verification failure naming `WorkerForegroundKt` is a fail; a soft one naming only `ForegroundRefusal` is recorded,
   P3).
2. Open the app and press Backup by (iii) steps 3 and 4 with `type=MANUAL`. The helper runs only if the app has login
   information (`MainActivity.java:391-402`: otherwise the missing-credentials dialog opens and no worker runs). No
   start within 60 s: "(vi) not run: needs the owner to enter the app password"; not a failure; the proof stays the unit
   test F13 and the dex checks (SC-2). A start: read the log as in (iii) step 6 for the result and for
   `VerifyError|NoClassDefFoundError` from the app's process; **pass** with neither.
3. `"$ADB" -s $SER emu kill` for `Pixel_API_24` at the end.

**Record.** `$E/AC12-emulator.txt`, written in `OW` first and copied through `python -I -X utf8 $T/sanitize.py <in> <out>`
(it refuses e-mail-like text): UTC clock time; AVD names and SDK levels; the APK SHA-256 against the hand-off; the
install results and package lines; `POST_NOTIFICATIONS`; that the test SMS were sent and when (device time); `HOME_T`,
the INCOMING start and `i-secs.txt`; the top-activity, process, service and job lines; each step's `ac12_run.py` and
`notif_check.py` line; the job lines of a held step; (iv)'s assessment; each step's outcome. The log extracts
`$E/AC12-{iii,i,ii}-log.txt` are `python -I -X utf8 $T/sanitize.py <raw> <out> --grep 'SMSBackup\+|WM-WorkerWrapper|WM-Processor|WM-SystemFgDispatcher|WM-SystemFgService|WM-SystemJobService|WM-GreedyScheduler|ForegroundServiceStartNotAllowedException|Unable to start foreground service'`
over the raw files after keeping only the lines of `PIDS` (`grep -E "^[0-9]{2}-[0-9]{2} [0-9:.]+ +(${PIDS//,/|}) "`).
The notification extract is `grep -E 'NotificationRecord\(.*pkg=com\.zegoggles\.smssync |android\.title=String \(Backing up\)'`
over each dump, through `sanitize.py`. Nothing raw enters the repository.

**What it proves:** on Android 16 a manual backup started on screen still runs in the foreground with today's
notification; an incoming backup that starts off screen runs as ordinary background work and succeeds; no automatic run
asks for the foreground. **What it does not prove:** a refused manual run on a device ((v) not run); Android 11 to 15,
in particular the Android 11 change of automatic runs from foreground to ordinary jobs (2.6 item 3); a physical device and
the owner's phones, including OEM background restrictions that can hold or stop jobs whatever this fix does; the stop of
a long automatic run (time limit, Doze, quota), which no step provokes; the boot backup when it is held by quota; that a
user's cancel stops a refused manual run on a device (read in 2.9.1, L27, not observed); a run on ART below API 31 if
(vi) cannot start the helper.

## 10. Items for the Orchestrator (hand-over)

1. **Commit after the reviews** (the developer commits nothing): the 4.9 list with explicit paths. Then re-run with the
   commit: `git diff --name-only <start> <commit> -- app/src/main` (three files, AC-8), `git grep -n "setForeground(" <commit> -- app/src/main`
   and `git grep -n "setForegroundOrContinue" <commit> -- app/src/main` (IC-1), and the count with the commit (AC-10).
2. **U-074 (IC-2, H-1):** carry into U-074's plan, IC-1 table: `WorkerForeground.kt:47` `catch (e: IllegalStateException)` —
   left on purpose (cancellation rethrown first; continues only on the refusal from API 31; else rethrown). Also 2.6 item
   5 (a refusal racing a cancellation) for U-074 to decide.
3. **Stale comments** (2.7): `BackupWorkerIntegrationTest.kt:40`, `:76`; the KDoc of `createBackupForegroundInfo()`. A later
   story may correct them; this story may not.
4. **Design clarification (H-2, optional):** DES-MODERNIZATION-005 `:129-131` and a Risks row, as BUG-029 proposes; CR-017
   already records that the current text stays true.
5. **Close-out corrections:** SC-8 ("Backing up"); SC-5 (P-1's middle fallback is not available offline).
6. **U-073 (H-3):** WorkManager ignores scheduler stops for foreground-mapped work even after a refused start (now only
   user-started runs); the new tests use work-testing's `TestForegroundUpdater` and `setForegroundUpdater` (P2), to be
   confirmed in 2.12.0's work-testing; 2.9.1's `SystemForegroundService` has no `onTimeout` (Android 15's six-hour
   `dataSync` budget; recalled that 2.10+ handles it), which after this story concerns only long user-started runs. For
   any later story: a request made expedited needs a `getForegroundInfo()` override, or the run fails below API 31
   (2.6 item 10, L28).
7. **Rulings:** SC-1 to SC-9 are ruled (Record 23); section 9 follows them. Nothing is asked.
8. **Ruling 61** ends when AC-12 has run: from then a device step may start a backup off screen.
9. **Documents (contract-verification):** X-3, the close-out edit of CR-017 Amendment 2 (E-7, DES-MODERNIZATION-014 row
   16-1), applied by the orchestrator when this story closes (B-1). The observations that are the orchestrator's: O-3
   (CNTR-005 `:213` wording, optional), O-4 (REQ-016 `:49-51` wording, optional), O-5 (U-072: REQ-017 AC-4's
   background-start row cites BUG-029/U-075 as applicable and fixed), O-7 (U-074's `dependencies` gain U-075).
10. **Later device checks (U-071 IC-4, U-073, the scheduler story):** a search for `ForegroundServiceStartNotAllowedException`
    now also matches the app's own warning. Use the two greps of section 9 ("Telling the two refusal lines apart"): the
    `W SMSBackup+ … continuing without the foreground` line is "refused and continued" (a finding only during an on-screen
    manual run), the `E WM-WorkerWrapper … ForegroundServiceStartNotAllowedException` line is the defect.

## 11. Constraints for the Developer (binding)

1. Gradle in the foreground, tool timeout 600000 ms on every call, never with `MSYS_NO_PATHCONV=1` set. No adb, no
   emulator, no device.
2. If the tool moves a run to the background, wait for its completion notice. Never poll; start nothing else meanwhile.
3. No background or detached shells, no `&`, no `sleep` or polling loops, no helper agents for shell work, no heredocs for
   editing (Write and Edit tools only).
4. On "bash.exe … not found", or a process killed without a visible reason: stop and report. Do not work around it;
   restore nothing from quarantine; add no exclusion.
5. **Do not call the external `diff` or `cmp`** (Record 18). Compare with `git diff`, `git diff --no-index`, `sha256sum`
   or `git hash-object`. Keep shell activity modest (the loops of I6 run over a few dex files).
6. Raw logs, pristine copies and dex extracts stay in the work folder. Only sanitised evidence enters `$W`. Never write an
   e-mail address, host name, user name or user-profile path into a repository file.
7. Timestamps from `date -u +%Y-%m-%dT%H:%M:%SZ`.
8. **Commit nothing.** Never stage, stash, reset, amend, rebase or push.
9. Production edits only as 3.1 to 3.3, plus the logged mutations, each restored and proven by hash and porcelain. Test
   files only as 3.4 (F-34 only under row D10). No edit to any existing test, to U-070's fixtures, or to the story file.
10. No dependency, Gradle, build, ProGuard, manifest, `robolectric.properties` or verification-metadata change; no floor
    change; no `@Ignore`, no `Assume`; no download (D26).
11. No new file under `app/` other than the three of 4.9. No test text contains `@Test` except the 18 annotations.
12. Do not touch other work in the checkout: `sdlc/.skill-context.json`, other stories' workspaces, the design and contract
    files and CR-017 (the orchestrator's uncommitted work), this workspace's plan and plan-gate files.

## 12. Quality Attribute Coverage (ISO/IEC 25010)

| Characteristic | Finding |
|----------------|---------|
| Functional Suitability | Correctness: automatic backups no longer fail off screen (AC-3, device (i)); user-started runs keep the notification (AC-4, device (iii)) and survive a refusal (AC-1, AC-6); only the refusal is absorbed (AC-5). Completeness: every automatic type and the untagged case are tested; all three delivery shapes of the refusal are tested (2.4) |
| Performance Efficiency | Time behaviour: one tag scan moved earlier; no new I/O. Resource use: automatic runs no longer hold a foreground service, its wake lock or Android 15's six-hour `dataSync` budget (BUG-029 L3, L9) |
| Compatibility | Co-existence: WorkManager's public updater seam, no constructor or signature change (IC-1); the manifest override stays for user-started runs. Interoperability with WorkManager 2.12.0 per BUG-029 L1-L4 (U-073) |
| Usability | Automatic backups run silently, as before the scheduler migration (PO-K default); a manual backup still shows "Backing up"; a refused manual run shows progress on the status screen without the notification (2.5) |
| Reliability | Fault tolerance: an environmental refusal degrades to background work instead of failing; defects stay loud. Recoverability: a stopped automatic run is rescheduled and resumes above the watermark; the cost is that automatic runs are now stoppable by the time limit (10 minutes on Android 11, recalled), Doze, lost constraints and quota, with no size bound on a run, and until U-074 such a stop writes one misleading ERROR line (2.6 item 3). Availability of automatic backup on Android 12+ off screen is restored. A user's cancel still stops a refused manual run (L27) |
| Security | No new permission or exported component; the warning carries no message content, address, account or folder (AC-2, tested over every log entry); `Log.w` survives release (no `-assumenosideeffects`) |
| Maintainability | Modifiability: one helper, two call sites (IC-1); the API-31 reference isolated in one two-line class. Testability: 18 tests through the library's own seam; every test but the control detected by an injection. Analysability: one warning line with a fixed prefix |
| Portability | Adaptability: identical behaviour of automatic backups from API 24 to 36; the API-31 class is never loaded below 31 (L19, F13, dex check). Installability: no manifest or build change |

## 13. Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-075 story | `sdlc/artifacts/build/sprints/sprint-024/U-075-automatic-backup-runs-when-the-app-is-off-screen.md` | Read in full: AC-1 to AC-12, IC-1 to IC-3, P-1 to P-3, constraints |
| BUG-029 | `sdlc/artifacts/stories/BUG-029-background-backup-fails-at-foreground-start.md` | Read in full |
| Observation | `sdlc/artifacts/build/research/2026-11-21-background-backup-and-boot-observation.md` | Read in full |
| Decision appendix | `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` | Records 18 and 21 in full; Record 22 rulings 64 to 68 (HEAD) |
| U-069 plan | `sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md` | Form: sections 0 to 4.9, 6, 9 to 13, Verdict; its evidence `I0-`/`I6-jacoco-packages.txt`, `IC3-emulator.txt` |
| U-070 plan | `sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md` | 3.6, 3.7 (applied in the simulation), 4.1 (fixtures), 4.8 (test table), 5.0, 5.1, I8 (M-L11, M-L20, M-L22) |
| U-074 story | `sdlc/artifacts/build/sprints/sprint-025/U-074-cancelled-run-is-not-reported-as-a-failure.md` | `WorkerCancellation.kt`, IC-1, AC-15, AC-17 (grep) |
| U-071 plan | `sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md` (working tree) | `:36-48`, M-1 anchor (`:997`), row D6 |
| CNTR-MODERNIZATION-004 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` | Status; Rule 8 at HEAD `:280` and working tree `:320`; "foreground"/"notification" search |
| DES-MODERNIZATION-005 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` | Status; `:54-55`, `:129-132` |
| CR-017 (working tree, uncommitted) | `sdlc/artifacts/build/change-records/CR-017-cntr-004-scheduler-never-boot-example-and-receiver.md` | `:235-249` (statements that stay true after U-075) |
| REQ-MODERNIZATION-013, -005, -015 | `sdlc/artifacts/requirements/modernization/` | Cited through the story and the bug; file names listed |
| Source at `fd70325a` | `service/BackupWorker.kt` (full), `service/RestoreWorker.kt` (`:1-240`, `:680-787`), `service/BackupType.java`, `service/BackupCursors.java` (`:30-45`), `di/MailModule.kt` (`:108-120`), `mail/transport/MailException.java` (grep), `service/WorkerEngineFactory.kt` (grep), `service/RestoreInsertInterceptor.kt` (grep), `mail/PersonLookup.java` (`:40-110`), `mail/DataType.java` (`:14-24`, grep), `preferences/DataTypePreferences.java` (`:45-62`), `preferences/Preferences.java` (grep), `App.java` (grep, `:170-260`), `activity/MainViewModel.kt` (`:196-268`, grep), `activity/StatusPreference.java` (`:220-245`, `:139`), `activity/MainActivity.java` (`:506-512`), `AndroidManifest.xml` (`:112-130`), `scheduler/WorkManagerScheduler.kt` (grep), `res/layout/status.xml`, `res/values/strings.xml` (grep) | The code changed and its neighbours |
| Tests at `fd70325a` | `service/BackupWorkerIntegrationTest.kt` (`:1-175`), `service/BackupWorkerLoopCancellationTest.kt` (full), `service/RestoreWorkerIntegrationTest.kt` (`:60-110`, `:470-505`), `service/RestoreWorkerCheckpointTest.kt` (`:575-600`), `src/test/resources/robolectric.properties`; `@Config` and `ShadowLog` greps | Harness, fixtures, names |
| Build files at `fd70325a` | `app/build.gradle` (`:15-120`, `:130-200`, rules), root `build.gradle` (grep), `gradle/gradle-daemon-jvm.properties`, `app/proguard-rules.pro` (grep) | SDKs, versions, floors, R8 posture |
| Library artifacts (local caches; `javap`, `unzip -l`, `sha512sum`) | `work-runtime-2.9.1.aar`, `work-testing-2.9.1.aar`, `robolectric-4.15.1.jar`, `plugins-maven-dependency-resolver-4.15.1.jar`, `shadows-framework-4.15.1.jar`; `android-all-instrumented` 7.0.0, 10, 14, 15 (i7) in the local Maven cache; Guava 33.0.0 and Truth 1.4.4 POMs | L5 to L15, L25 |
| Scratch compile and dex | `kotlin-compiler-embeddable` 2.3.21 with its runtime jars; build-tools 36.0.0 `d8.jar`, `dexdump`; the SDK's `android-35/android.jar` | L19, N1, mutant compile |
| Evidence tools | U-063 `sanitize.py`, `junit_cases.py`, `junit_totals.py`, `jacoco_pkgs.py`; U-069 `jacoco_classes.py`, `oldtext_check.py` (form); U-068 `sdk_dir.py` | Reused; docstrings read |
| Standards and templates | plugin cache `templates/build-artifacts/build-plan.md`, `knowledge/standards/phase-completion-report.md` (1.9.0) | Frontmatter, report format |
| Iteration 2: gate reports | `sdlc/artifacts/build/sprints/sprint-024/U-075/plan-review.md`, `contract-verification.md`, `expert-depth-check.md` | Read in full; the Iteration 2 table |
| Iteration 2: rulings | the story's Implementation Notes, "Rulings on the plan's scope concerns" (Record 23, SC-1 to SC-9); the coordinator's message (ruling on contract O-6 / D23; ruling 69) | Section 9, D23, section 14 |
| Iteration 2: real log formats | `sdlc/artifacts/build/research/evidence/2026-11-21-background-backup-app-log.txt` (full); `sdlc/artifacts/build/sprints/sprint-024/U-069/evidence/IC3-emulator.txt`; `sdlc/artifacts/build/sprints/sprint-024/U-069/evidence/mutation-log.md`, `I6-jacoco-packages.txt` | Inputs of section 14 |
| Iteration 2: code and library | `activity/MainActivity.java` (`:388-404`, `:505-522`), `App.java` (`:162-168`), `preferences/Defaults.java` (grep) at `fd70325a`; WorkManager 2.9.1 `Processor.stopAndCancelWork`, `CoroutineWorker.getForegroundInfo`, `SystemForegroundService$Api31Impl`, `SystemJobService`, `GreedyScheduler` (`javap`) | L27 to L29 |

## 14. Self-Test of the Plan's Commands (ruling 69)

Run on 2026-11-21 in this session's scratch folder, outside the repository, with no Gradle, no device and no read of the
working tree under `app/`. **Every tool tested is the copy extracted from this plan file** (14.1), not a scratch original.
Inputs marked "constructed" were written in the format recalled for that output (they test the parsing, not the
format); inputs marked "real" are today's committed log lines and evidence. Python here writes CRLF to a redirected
stdout (14.5); `ls` is aliased to `ls -F` (appends `/` to directories); `sha256sum` prints `*` before the path; each is
handled where it matters. In the table, `\|` is the shell's `|` escaped for Markdown; the commands themselves are those
of sections 4 and 9.

| # | Command as it stands in the plan | Input | Expected | Observed |
|---|----------------------------------|-------|----------|----------|
| 14.1 | Blob of every embedded file: the first fenced block after its marker line, lines joined with LF plus a final LF, `git hash-object --no-filters` (and SHA-1 of `blob <n>\0<data>`) | `plan.md` as written | 3.3 `0782f85f…`; T1 `3e3ea62b…` (517 lines, 13 `@Test`); T2 `e69175c6…` (174, 5); `oldtext_check.py` `8d1158ec…`; `dex_refs.py` `4f72cc2f…`; `ui_bounds.py` `d034222c…`; `notif_check.py` `0a7a889c…`; `dt_secs.py` `75b37f0d…`; `ac12_run.py` `6dc06eee…`; F-34 variants `0145ee38…`, `9207950d…` (one line changed each) | All equal; the two hash methods agree; a second extraction after the last edit is byte-equal (`git diff --no-index --quiet` exit 0 for all nine). Iteration 1's `c7f765f0…` (B-1) came from a scratch copy that differed from the printed text |
| 14.1b | Scratch compile of the extracted helper and both test files (Kotlin 2.3.21, stubs of the app classes) | extracted files | exit 0 | exit 0 (T2's new assertion compiles) |
| 14.2 | `python -I -X utf8 $TL/ui_bounds.py "$OW/u075-ui.xml" > "$OW/ui.txt"; echo "exit=$?"; read -r X Y REST < "$OW/ui.txt"; …; grep -c 'enabled=true label-is-backup=True' "$OW/ui.txt"` | constructed dumps: label `Backup` with a second node holding an address; `BACKUP`; no button; two buttons; empty file; XML followed by uiautomator's `/dev/tty` status line | `290 965 enabled=true label-is-backup=True` exit 0 (twice: case-insensitive); `nodes=0`, `nodes=2`, `xml=unreadable`, `xml=unreadable` exit 1; no dump text printed | As expected; `read -r` gives `X=290 Y=965` despite CRLF; the grep count is 1; the address is not printed |
| 14.3 | `python -I -X utf8 $TL/notif_check.py "$OW/i-notification.txt"` | constructed dumps: app record id 1 plus another package's id-1 record with the same title; another package only; empty; `Can't find service: notification`; the app record with CRLF; `pkg=com.zegoggles.smssync.other` | `records=1 id1=1 id1-channel=True id1-title-backing-up=True`; `records=0 id1=0`; `dump=unreadable` exit 1 (twice); as the first with CRLF; `records=0` (the prefix does not count) | As expected; a missing file also gives `dump=unreadable` exit 1. The other package's record is not counted |
| 14.4 | `python -I -X utf8 $TL/dt_secs.py "$HOME_T" "$ST"` | `11-21 09:46:38.000` → `11-21 09:47:36.068` (today's Home press and refusal); `none`; empty; a month boundary | `seconds=58.068`; `time=unreadable` exit 1 (twice); `seconds=70.500` | As expected |
| 14.5 | any tool `> file` | `dt_secs.py` output | bytes end in `\r\n` | `\r\n` (so outputs are read field-wise, `grep`, or with `tr -d '\r'`; no byte comparison of Python output anywhere) |
| 14.6 | `adb` arguments under Git Bash; `MSYS_NO_PATHCONV=1` on the command only | `python.exe` printing its `argv` (a native Windows program like `adb.exe`) with the plan's argument lists | `/sdcard/u075-ui.xml` rewritten without the variable, unchanged with it; no other argument rewritten | Without: `C:/Program Files/Git/sdcard/u075-ui.xml` (dump and `exec-out cat`); with: `/sdcard/u075-ui.xml`; the boot-wait loop, the `date` format, the `-T`/`-e` arguments, `am start -n com.zegoggles.smssync/.activity.MainActivity` and `emu sms send` pass unchanged; the variable is unset afterwards in the shell |
| 14.7 | `python -I -X utf8 $TL/ac12_run.py <logcat> <TYPE> --pids "$PIDS" [--after "$HOME_T"]` | **real** log, pids 3309 and 3332; constructed logs in the same format (pass; pass with CRLF; asked for the foreground; stopped by the system; real error; empty) | Run 2 REGULAR: `result=SUCCESS moved=1 watermark=1 backedUp=1`; Run 1 INCOMING: `moved=1 nothing=1`; the refused INCOMING after Home: `start=none defect=2`; Run 1 REGULAR alone: `start=none defect=2`; constructed: `moved=0 watermark=1 backedUp=1 refused-warn=0` (twice); `moved=1 refused-warn=1`; `result=none unrecoverable=1 unrecoverable-cancel=1`; `result=FAILURE unrecoverable=1 unrecoverable-cancel=0`; `start=none`; another process's warning not counted; empty `--pids` or none: `pids=missing` exit 1 | All as expected. This test found the rule change of iteration 2: an unfixed build's refused run has no start line, so `defect` decides first ((i) step 5, (ii) "Fail first") |
| 14.7b | `PIDS=$({ grep -oE 'Start proc [0-9]+:com\.zegoggles\.smssync/' "<raw>" \| grep -oE '[0-9]+'; … pidof … \| tr -d '\r' \| tr ' ' '\n'; } \| grep -E '^[0-9]+$' \| sort -u \| paste -sd,)` and the extract filter `grep -E "^[0-9]{2}-[0-9]{2} [0-9:.]+ +(${PIDS//,/\|}) "` | constructed `Start proc 4401:com.zegoggles.smssync/u0a190 …` line plus a CRLF `pidof` reply; empty log and empty reply; the real log | `4401`; empty (and `ac12_run.py` then refuses); 7 lines of 4401; 52 app lines of the real log | As expected |
| 14.7c | `ST=$(grep -oE 'start=[^ ]+' "$OW/i-run.txt" \| cut -d= -f2 \| tr '_' ' ' \| tr -d '\r')` | `ac12_run.py` output (CRLF) | `11-21 10:02:11.050`, then `dt_secs.py` gives a number | `ST=[11-21 10:02:11.050]`, `seconds=71.050` |
| 14.8 | `timeout <n> … && <capture>; echo "exit=$?"` | a 5 s sleep under `timeout 2`; a quick command under `timeout 5` | `exit=124` and the chained capture skipped; the capture runs on success | As expected |
| 14.9 | I6 step 4's release pipeline: `FR=$(grep -E '^com\.zegoggles\.smssync\.service\.ForegroundRefusal -> ' $MAP \| sed -E 's/.* -> (.*):$/\1/' \| tr -d '\r')`, the same for `WK`, `FRD="L${FR//.//};"`, `dex_refs.py` over the dex files, `grep -cF "CLASS $FRD "` | constructed mapping (renamed names and a `WorkerForegroundKtX` decoy); an identity mapping with the D8 dex of the helper (build-tools 36.0.0, min-api 24) | `FR=a.b.c WK=a.b.d`, descriptors `La/b/c;`, `La/b/d;` (decoy not matched); `holder-lines=1 caller-lines=0`, one app class | As expected |
| 14.10 | I0 step 3's `awk -v id="$id" '$0 ~ "^### " id " " {on=1; print; next} /^### / {on=0} on' <mutation-log.md>`; I6 step 0's `grep -cE '^### [^ ]+ APPLYING '` and `grep -cE '^(- )?restored: '` | **real** U-069 `mutation-log.md`; the same with an open entry appended | whole entries (M-T1: 8 lines with its failing line; the last entry to the end of the file); 12 and 12; unequal with the open entry | As expected. The first form planned for I6 step 0 (`grep -c "APPLYING"` against `^restored: `) gave 13 and 0, a false stop, and was replaced |
| 14.11 | `command ls -1 "$AAI" > …; grep -cE '^(7\.0\.0_r1-robolectric-r1-i7\|14-robolectric-10818077-i7\|15-robolectric-12650502-i7)/?$' …`; `git diff --no-index --quiet` of two listings | the real local Maven cache folder (read only) | `3`; equal listings exit 0 | `3` and exit 0. Plain `ls` printed `…-i7/` (the alias), which the iteration-1 form did not allow for |
| 14.12 | `grep -E '^com\.zegoggles\.smssync\.service ' I<k>-jacoco-packages.txt` | **real** U-069 `I6-jacoco-packages.txt` | `com.zegoggles.smssync.service 674/822 82.0%` | As expected (dotted names, coordinator's fact 2) |
| 14.13 | I6 step 5 in a scratch git repository: the simulated start tree committed as `<start>`, then the fixed workers, the extracted helper and tests | scratch repository | `mismatches=0`; blobs `3f2f0c76…`, `9be01b15…`, `0782f85f…`; porcelain ` M` ×2, `??` ×3; `--name-only` the two workers; `test-tracked-unchanged-exit=0`; `setForeground(` 3 lines here (the two KDoc lines and `WorkerForeground.kt:45`; the real tree adds the manifest and `MainActivity` comments); `setForegroundOrContinue` 3 lines; `ignore-exit=1`; 13 and 5 | As expected; with an existing test edited, `test-tracked-unchanged-exit=1` (D33) |
| 14.14 | `python -I -X utf8 $TL/oldtext_check.py start` / `fixed` | the simulated start tree (CRLF `BackupWorker.kt`, U-070's fixture text) and fixed tree | `mismatches=0` both | As expected |
| 14.15 | `<dexdump> -d <dex> \| python -I -X utf8 $TL/dex_refs.py 'Landroid/app/ForegroundServiceStartNotAllowedException;'` | the D8 dex of the helper | one class, `Lcom/zegoggles/smssync/service/ForegroundRefusal;` | As expected |
| 14.16 | Hygiene of the plan: `grep -n -i -E` for the user and host names, profile paths and e-mail-like tokens; control characters | `plan.md` | no line; no CR, no control character except the four tabs of the `--numstat` example | As expected (a backslash loss in one scripted edit was found by this check and repaired) |
| 14.17 | I0's and I6's document-folder record: `git status --porcelain --untracked-files=all -- <three folders> \| cut -c4- \| while read -r p; do sha256sum "$p"; done`, then `grep -vxF -f others-I0.txt others-I6.txt; echo "docs-new-or-changed-exit=$?"` | the real repository (read only: no line today); a scratch repository with five cases | real: empty list; A none/none, B unchanged, C committed by the orchestrator in between: exit 1 (no line); D changed again, E a new untracked document: the path printed, exit 0 | As expected; the commit subject appears in the `git log --format='%h %s'` line; an empty pattern file still gives exit 1 on an empty list |
| 14.18 | `"$ADB" devices \| grep -E '^emulator-[0-9]+[[:space:]]+device$' \| cut -f1` | constructed CRLF listing with two emulators, an offline one and a physical device | `emulator-5554`, `emulator-5556` | As expected (no CR in the serials) |
| 14.19 | `grep -E "android\.permission\.POST_NOTIFICATIONS: granted="`; `grep -E "SystemForegroundService\|isForeground=\|foregroundId="`; `grep -E "topResumedActivity\|mResumedActivity" … \| grep -c com.zegoggles.smssync`; `grep -c WITHIN_QUOTA`; `grep -E "com\.zegoggles\.smssync"` on the scheduler dump | constructed (CRLF) package, service, activity and scheduler dumps in the recalled formats | the granted line; the service record and `isForeground=true foregroundId=1`; `0`; `1`; the job line | As expected; the formats themselves are not verified (R15) |
| 14.20 | The two refusal greps of section 9 | **real** log; constructed lines | real log: warning 0, defect 4 (two lines per refusal), bare class name 4; constructed: warnings counted, defect 0 | As expected |

Not testable here, and why: every Gradle step (build, tests, JaCoCo, the APK dex and mapping of the real build), every
`adb` and emulator step against a device, and the real formats of `dumpsys notification`, `dumpsys activity processes`
and `services`, `dumpsys jobscheduler` and `uiautomator dump` on this emulator (constructed samples only; their stop
routes are rows D11 and the "not determined" and "not conclusive" outcomes of section 9). A later finding in those that
can only cause a false stop is handled by the stop-and-amend route.

## Verdict

**PASS.** The story can be built as written, with the ruled design and no decision left to the developer.

- The design is ruling 60's: the type first, the foreground only for `MANUAL`, `SKIP` and restores, one helper that
  rethrows a cancellation first, absorbs only `ForegroundServiceStartNotAllowedException` from API 31 and rethrows the
  rest. The API-31 reference is isolated in a two-line holder class behind the `SDK_INT` test, shown by bytecode and dex
  output to be the only place the type is named (L19), and checked again in the real debug and release dex (I6).
- **P-1 is settled offline:** Robolectric 4.15.1 knows SDK 35 and 34, both pre-instrumented jars are cached with matching
  SHA-512 and contain the five exception classes, JDK 25 is allowed; no jar for 31 to 33 exists (SC-5, ruled).
- **P-2 is settled:** a refused user-started run continues, its result is applied, it shows no notification, its progress
  reaches the status screen, WorkManager keeps it in the foreground map until it ends, and a user's cancel still stops it
  (2.5, L27); the stop condition does not occur.
- **Item 3:** the throwing updater is the story's binding form; two more tests drive WorkManager's own unwrapping branches
  (already-failed and pending future).
- **Iteration 2** fixes plan-review B-1 to B-6 and takes up O-1 to O-9, follows the SC rulings (Record 23) in section 9,
  carries contract-verification's X-3 and the ruling on D23, takes up both depth-check recommendations (one sub-item
  declined with its reason), and adds section 14: every command that can run without Gradle, a device or `app/` was run
  on the plan's own text and on sample or real input; three defects in commands and one gap in a pass rule were found that
  way and fixed.
- 18 tests (7 red at Start, all green after), 12 mutation runs with complete failing sets (8 counted), 16 Gradle runs (two
  full gates), one session.

Nothing is open for the orchestrator before the build (SC-1 to SC-9 ruled). Not verified: the real test compile (N1),
Robolectric at 35 and 24 at run time (N2, N3), JaCoCo's line totals (N4), AGP's own D8/R8 output (N5), ART's handling below
API 31 (P3), work-testing 2.12.0 (P2), the emulator's `dumpsys` formats (R15), the platform job limits marked "recalled"
in 2.6 item 3, and BUG-029 Not verified 5.

## Phase Completion Report
---
story_id: "U-075"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-075/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
notes: "Iteration 2. plan-review B-1 (every embedded blob re-derived from the plan text; ui_bounds.py now d034222c after the O-6 change; T2 e69175c6 after O-3), B-2 (section 9 rewritten to the SC rulings of Record 23: no screenshot or UI dump, notification evidence by notif_check.py over dumpsys, (i) passes on the watermark and backup-complete lines, (v) not run, (vi) on Pixel_API_24 selected by emu avd name with install-time verification and 'not run: needs the owner to enter the app password'), B-3 ((iv) only over observed off-screen runs), B-4 (MSYS_NO_PATHCONV=1 on the three /sdcard commands, tested), B-5 (no timeout above 540 s; 450 s anchored double waits), B-6 (rows D32 to D35) fixed; O-1 to O-9 taken up. Contract: DES-014 row 16-1 correction recorded (X-3 for the orchestrator); D23 per the orchestrator's ruling (others' uncommitted document paths recorded by sha256 at I0, compared at I6). Depth check: release dex through mapping.txt; 2.6 items 3, 8, 10, 11; process, service and job state at the INCOMING start; 'stopped by the system' outcome; scheduler identification partly declined (WorkManager logs from INFO, App.java:162-165). New tools notif_check.py, dt_secs.py, ac12_run.py. Ruling 69: section 14, 20 self-tests on the plan's own text, real logs and constructed samples; found and fixed: the refused-run-without-start rule, the ls -F alias in the cache listing, the APPLYING/restored count, a backslash loss in an edit. 18 tests, 12 mutation runs (8 counted), 16 Gradle runs, one session; design, edits, T1 and the failing sets unchanged."
---
