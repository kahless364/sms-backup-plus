---
artifact_type: gate-result
story_id: "U-075"
gate: plan-review
gate_id: "plan-review"
checkpoint: plan-gate
verdict: PASS
iteration: 2
agent: "Solution Architect (separate instance, plan-review)"
timestamp: "2026-11-21T18:25:52Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-024/U-075/plan.md"
base_commit: "f731e70cc676094de5afa83fe6d7bfd24be10583"
blockers: 0
observations: 5
superseded_iterations:
  - iteration: 1
    verdict: FAIL
    timestamp: "2026-11-21T17:41:58Z"
    base_commit: "acc50c94c1c635685164518731ab1b217dca76f2"
    blockers: 6
    observations: 9
---

# Gate Report: gate-result — U-075

## Gate: plan-review @ plan-gate

Verdict: PASS (iteration 2)

## Iteration 2 — Summary

**Scope.** Ruling 73 (Record 23) sets what I re-checked:
- my blockers B-1 to B-6;
- the planner's self-test table (section 14);
- everything iteration 2 added or rewrote, reviewed in full: section 9, the four orchestrator tools, rows D32 to D35, the release-dex check and the changed restore test file.

For the parts stated as unchanged, I checked the claim by hash and by comparing the text with iteration 1 (committed in `acc50c94`), rather than re-deriving everything. The rules were as before:
- no read of `app/` or `app/build` in the working tree;
- no Gradle, build, test, device or network command;
- no `diff` or `cmp`;
- scratch work only outside the repository.

`HEAD` is `f731e70c`, and `git diff --stat acc50c94 HEAD -- app` is empty.

**Result: all six blockers are closed, and I found no new blocker.** The five observations below can only cause a false stop or are matters of precision. Under ruling 73 they go to the plan's stop-and-amend route.

**The unchanged parts, checked:**
- **Production edits.** E-B1, E-B2 and E-R1, extracted by marker from the iteration-2 text, each occur once in `ba667c43…` and `dc84c4f5…`. The results are the same blobs as in iteration 1: `3f2f0c7678fab5a7ddf5d36581f2df32cd19a643` and `9be01b15210508393e750136ff127cc7eed3a968`.
- **The helper and the backup test file.** `WorkerForeground.kt` is `0782f85f…` and T1 is `3e3ea62b…`, as before.
- **Unchanged tables.** The rows of the mutation table (`| M-… |`) and the section 5.2 red-run text are byte-identical to iteration 1 (equal SHA-256 of the extracted lines). Of the `| F… |`/`| R… |` lines, only the risk-table rows R2 and R10 changed, and R15 is new. The test table 5.1 is unchanged.

## Iteration 2 — Blockers re-checked

**B-1 (blobs): closed.** I took the first fenced block after each file's marker line, wrote it LF, and hashed it with `git hash-object --no-filters` and with a SHA-1 of `blob <n>\0<data>`. The two methods agree. All nine files equal the plan's values:

| File | Blob | Lines |
|------|------|-------|
| `WorkerForeground.kt` | `0782f85f…` | 65 |
| T1 `BackupWorkerForegroundTest.kt` | `3e3ea62b…` | 517, 13 `@Test` |
| T2 `RestoreWorkerForegroundTest.kt` | `e69175c6…` | 174, 5 `@Test` |
| `oldtext_check.py` | `8d1158ec…` | 92 |
| `dex_refs.py` | `4f72cc2f…` | 19 |
| `ui_bounds.py` | `d034222c…` | 25 |
| `notif_check.py` | `0a7a889c…` | 29 |
| `dt_secs.py` | `75b37f0d…` | 13 |
| `ac12_run.py` | `6dc06eee…` | 66 |

The F-34 variants (one `@Config` line each) are `0145ee38…` and `9207950d…`, as stated. No stale iteration-1 value (`c7f765f0`, `c513ea18`, `7b5687be`) remains anywhere in the plan. I0 step 1 and I1 step 2 carry the new values.

**B-2 (section 9 against SC-1, SC-2, SC-3, SC-9): closed.**
- **SC-9.** (i) passes on `watermark=1` or more, that is, the line `BackupWorker: watermark advanced for SMS to <date>` on the run's thread between its start and its result (post-U-070 `BackupWorker.kt:439`, written only after `appendMessages` returned). There is no mailbox check.
- **SC-1.** Screenshots are dropped. The UI dump stays in `OW` and is deleted at the end. No outcome rests on either. The notification evidence is `notif_check.py` over `dumpsys notification`, and the extract goes through `sanitize.py`.
- **SC-3.** (v) is "Not run: needs a system setting".
- **SC-2.** (vi) is now the orchestrator's:
  - `Pixel_9_Pro` is stopped with `emu kill` first;
  - `Pixel_API_24` is selected by `emu avd name`; `Pixel_10_Pro` is never sent anything;
  - install-time verification lines are recorded;
  - a run passes only if it starts, with no `VerifyError` or `NoClassDefFoundError`.

  On the missing-credentials dialog, the plan now matches `MainActivity.java:391-402` at `fd70325a`. `performAction` opens `MISSING_CREDENTIALS` when `isLoginInformationSet()` is false, and `FIRST_SYNC` (or the confirmation) otherwise, so no worker runs. The plan records "no start within 60 s" as "(vi) not run: needs the owner to enter the app password", not a failure, and presses nothing in a dialog. A start line implies the helper has already run, because E-B1 puts the helper before the "starting backup" line. So a (vi) pass does observe the class loading it is about. Iteration 1's "MailException — retrying" sentence is gone.

**B-3 (no pass without the observed run): closed.** I read every outcome rule of section 9 for a path to "pass" without the run it is about:
- **(iii)** needs `start` not `none`, `result=SUCCESS` and `moved≥1` for the MANUAL run found after `-T $D3`. A second inconclusive result is "not run".
- **(i)** needs a start at least 58 s after `HOME_T` (`dt_secs.py`), the top-activity and process lines not naming the app, `result=SUCCESS`, `moved=0`, `watermark≥1` and `backedUp`. Its "no id 1 notification" condition counts only when `iii-notif.txt` parsed one of the app's records in the same session, so an unparseable format cannot produce "no record".
- **(ii)** needs the `bootup` line and a start at most 900 s after `BOOT_T`.
- **The planner's case.** An unfixed build's refused run writes no start line. It is now caught by `defect>0`, which is checked first in (i) step 5 and in (ii) "Fail first". I confirmed it on the real log: INCOMING `--after '11-21 09:46:38.000'` gives `start=none … defect=4`.
- **(iv)** is assessed only over observed off-screen runs and is "not run" with none.
- **`ac12_run.py`** refuses an empty `--pids` (`pids=missing`, exit 1). The `PIDS` derivation gives an empty list for an empty log and reply, and `4401` for a constructed `Start proc` line plus a CRLF `pidof` reply.

**B-4 (device paths): closed.** All three `adb` commands with `/sdcard` in section 9 carry `MSYS_NO_PATHCONV=1` on that command only: the dump, the `exec-out cat` and the `rm`. No other command in section 9 has a device path (`grep` of the section). It is never set for Gradle.

**B-5 (waits): closed.**
- The `timeout` values in section 9 are 60, 120 (twice), 240, 300 (three times), 450 and 540 s. None exceeds 540 s, and every call is a foreground call at 600,000 ms.
- **(i) cannot miss a start.** The two 450 s waits carry the same `-T "$D1"`, and logcat replays the buffer from that time, so the second wait finds a start that fell between the two calls.
- **No double-count.** The second wait runs only after `exit=124`, and `-m 1` ends each wait at its first match. The waits only gate the captures; every outcome is read once from the single `logcat -d` dump in step 6, through `ac12_run.py`.
- **(ii)'s waits** have no `-T` after a cold boot. They replay the whole buffer, so they cannot miss either.
- **The (ii) window still covers 900 s.** After the boot wait and the 120 s `bootup` wait, the two 450 s waits start at most about 120 s after `BOOT_T`. The 900 s rule is applied by `dt_secs.py` against `BOOT_T`.

**B-6 (rows): closed.** Each new row covers the cases I listed:
- **D32:** hashes, `PROD_DIFF`, an open `APPLYING` entry, porcelain at I6 step 0 or 5. G-F is void; restore; report.
- **D33:** `test-tracked-unchanged-exit`, `ignore-exit`, the 13/5 counts. On the extracted tests `grep -nE '@Ignore|Assume'` gives exit 1, and the counts are 13 and 5.
- **D34:** toolchain, `--version`, a running Gradle JVM, `@Ignore` lines.
- **D35:** I1 and I4 porcelain, `main-clean-exit`, `oldtext_check.py fixed` at I4.

Citations: D34 in I0 steps 2 and 4; D35 in I1 step 2 and I4 step 1; D32 in I6 steps 0 and 5; D33 in I6 step 5. My iteration-1 observations are also taken in:
- O-1 is in D29;
- O-2 is the new I6 step 0. On U-069's committed log its anchored counts are 12 and 12, while the bare `APPLYING` count is 13 (the header's recovery note), which is why the plan anchors them.
- O-5: the `awk` entry reader returns the whole M-T1 entry (8 lines) on that log.

## Iteration 2 — New and rewritten parts

- **The changed restore test (T2, `e69175c6…`).** It differs from iteration 1 by two added lines only, at the end of R2 `restore_foregroundRefused_continues`:
  `assertWithMessage("log entries carrying the configured account name").that(ShadowLog.getLogs().filter { (it.msg ?: "").contains(SmsFixture.OWNER) }).isEmpty()`.
  - **It compiles.** `ShadowLog.getLogs()` is a `List<LogItem>` with a public `msg` field (verified with `javap` in iteration 1), `SmsFixture.OWNER` is U-070's `internal const`, and Truth's `that(Iterable)` gives `isEmpty()`.
  - **The failing set is still complete.** R2 is still red at Start (the refusal escapes `doWork` before this line) and green after. The restore path logs only the starting line, the warning (prefix and class name) and "RestoreWorker: MailException — retrying" (the account name is not in a message). No injection writes the account name, so R2's failing set is unchanged: M-F1b, M-F6 and M-F7 fail it at earlier assertions.
- **`ui_bounds.py`.** I tested it on dumps of my own: `BACKUP` next to an address node, `Cancel`, a disabled button, unreadable bounds, and XML followed by the `/dev/tty` status line. The outputs are: `290 965 enabled=true label-is-backup=True` (the grep count is 1 and `read -r` gives integers despite CRLF); `label-is-backup=False`; `enabled=false`; `bounds=unreadable` exit 1; `xml=unreadable` exit 1. No dump text is printed.
- **`notif_check.py`.** I tested it on my own dumps in AOSP's `NotificationRecord(0x…: pkg=… user=… id=… …)` form:
  - the app's id 1 record next to another package's id 1 record that has the same channel and title: `records=1 id1=1 …True True` (the other package is not counted);
  - the app with `id=10` only: `id1=0`;
  - CRLF: as LF;
  - the app with id 2 "Restoring": `id1=0`;
  - an empty file or a missing file: `dump=unreadable` exit 1;
  - a header only: `records=0`.

  The one case that could falsely read as "no record" is a different real format. (i) guards against it by requiring (iii)'s parse in the same session.
- **`dt_secs.py`.** Today's pair gives `seconds=58.068`; `none` gives `time=unreadable` exit 1; a midnight crossing gives `70.500`; equal times give `0.000`.
- **`ac12_run.py` on the real log** (`sdlc/artifacts/build/research/evidence/2026-11-21-background-backup-app-log.txt`, pids 3309 and 3332; the extract filter keeps 52 of its 55 lines):
  - REGULAR: `result=SUCCESS moved=1 watermark=1 backedUp=1`;
  - first INCOMING: `result=SUCCESS moved=1 nothing=1`;
  - MANUAL: `start=none`;
  - INCOMING after the Home press: `start=none`.

  `defect=4` on the whole file, which agrees with 14.20. 14.7's `defect=2` is per run.

  The two refusal greps of section 9 give `warn=0` and `defect=4`, and the bare class name gives 4. That matches 14.20.
- **The release-dex pipeline of I6 step 4.** I ran it on a constructed mapping (`ForegroundRefusal -> a.b`, `WorkerForegroundKt -> a.ba`, a decoy `WorkerForegroundKtX -> a.bb`) and a constructed dexdump listing:
  - the descriptors are `La/b;` and `La/ba;` (the decoy is not matched), and the result is `holder-lines=1 caller-lines=0`;
  - with the caller made to name the type, `caller-lines=1`;
  - with an empty mapping, the descriptor is `L;` and `holder-lines=0`, which fails the expected value and leads to D24's "mapping line missing: stop".

  The trailing space in `grep -cF "CLASS $FRD "` keeps `La/b;` and `La/ba;` apart.
- **Self-test table (section 14).** I reproduced rows 14.1, 14.2, 14.3, 14.4, 14.7, 14.7b, 14.9, 14.10 and 14.20 myself (nine of twenty) with matching results. I did not re-run 14.1b (the compile), 14.6, 14.8, 14.11 to 14.19.

## Iteration 2 — Observations (ruling 73: false stops or precision; stop-and-amend route)

**O-1. (iv) counts three line forms, not the story's literal "any line containing the class name".** `refused-warn`, `defect` and `svc-refusal` are counted over the app's lines. I found no app-process path that writes the class name outside them:
- the helper's warning;
- WorkerWrapper's error and its stack lines, which carry the same tag and level;
- `SystemForegroundService$Api31Impl`'s "Unable to start foreground service", whose stack lines follow the counted first line.

So this is not a false-pass path with this code. Adding a fourth field (the bare class name over the `PIDS` lines, which 14.20 already computes) would make (iv) the story's criterion word for word.

**O-2. `ac12_run.py` reads a run on the start line's thread only.** In production, `setProgress` can suspend while WorkManager's progress future is pending. The coroutine can then resume on another `Dispatchers.Default` thread, so the `watermark advanced` or `backup complete` line may carry another tid. That gives `watermark=0` or `backedUp=none`, and (i) fails to pass: a false stop. The plan names the limit (3.5, "Known limits"). If it happens, the amend route is to read the run by its work id, or over the app's tids between start and result.

**O-3. (vi)'s "not run" label.** Any non-start within 60 s is recorded "needs the owner to enter the app password". On a first install on API 24 the blocker could also be the first-sync or confirmation dialog, or a runtime-permission prompt. Recording which dialog was on screen (from the UI dump's resource ids, without text) would make the record exact. It is never a pass.

**O-4. Synthetic classes in the release dex.** The expected values allow "a synthetic class R8 derived from the holder", but with obfuscation on, such a class cannot be recognised by name. D25 records and reports rather than stopping, so this is a judgement for the hand-off, not a risk of a false pass. `caller-lines=0` stays the deciding fact.

**O-5. Formats recalled, not read on the emulator.** `notif_check.py`'s record form, the process, service and scheduler dump lines, and uiautomator's dump are tested only on constructed samples (the plan's R15). Every mismatch leads to `records=0`, "not determined", "not conclusive" or "not run", never to a pass. (i)'s notification condition is guarded by (iii)'s parse.

## Iteration 2 — What I ran

- **Embedded files.** A marker-based extractor over the plan text: the first fenced block after each marker, written LF, hashed two ways. Also the three production edits re-applied to the post-U-070 blobs, and the F-34 variants.
- **Comparison with iteration 1.** `git show acc50c94:<plan>` compared with the current plan by SHA-256 of the extracted table rows and section 5.2. `git diff --no-index` was used for the T2 lines and the changed table rows.
- **The tools on my own inputs.** `ui_bounds.py` (5 dumps), `notif_check.py` (7 inputs), `dt_secs.py` (4 pairs).
- **The real log.** `ac12_run.py` (4 runs and the empty `--pids` case), the `PIDS` derivation and the extract filter, and the two refusal greps.
- **The I6 step 4 pipeline** on a constructed mapping and dexdump listing, with a negative case and an empty mapping.
- **The I0 step 3 `awk` and the I6 step 0 counts** on U-069's committed `mutation-log.md`.
- **`grep` scans of section 9** for device paths, `MSYS_NO_PATHCONV`, `timeout` values, and `diff`/`cmp`.

## Iteration 2 — What I could not check

The same as iteration 1:
- the real Gradle compile and run (N1 to N3);
- JaCoCo's line totals (N4);
- the real R8 and D8 output (N5);
- ART's verifier below API 31 (P3);
- the emulator's real `dumpsys` and uiautomator formats (O-5);
- the device steps themselves.

---

## Iteration 1 (superseded by iteration 2 above)

Verdict: FAIL (iteration 1, 2026-11-21T17:41:58Z, base `acc50c94`). Kept as the record; every blocker is closed in iteration 2.

### Iteration 1 — Summary

I reviewed iteration 1 of the plan (2,025 lines). I followed the rules for this run. I read no file under `app/` in the working tree and nothing under `app/build`. I read code only with `git show fd70325a:<path>` and from the two post-U-070 blobs (`ba667c43…` `BackupWorker.kt`, `dc84c4f5…` `RestoreWorker.kt`). I ran no Gradle, build, test, device or network command, and I did not call `diff` or `cmp`. Scratch work stayed in my session scratch folder, outside the repository. `HEAD` is now `acc50c94`, which commits the plan and the SC rulings. `git diff --stat 3770b9a4 HEAD -- app` is empty.

**The design, the production edits and the tests are sound. Every count, blob and failing set I re-derived matches the plan.** The six blockers are in one tool blob, in the decision table, and in the orchestrator's device check (section 9). Section 9 was written before rulings SC-1 to SC-9, and the plan has not been brought into line with them.

What I confirmed:

- **Edits (item 1).** I extracted E-B1, E-B2 and E-R1 from the plan by line range (`:336-340`→`:344-353`, `:358-359`→`:363`, `:370-374`→`:378-382`). Each old text occurs once in the post-U-070 blobs. Applying them gives:
  - blobs `3f2f0c76…` (653 lines) and `9be01b15…` (791 lines);
  - `--numstat` `10 6` and `5 5`;
  - hunks `@@ -119,5 +119,10 @@`, `@@ -142 +146,0 @@` and `@@ -144,5 +144,5 @@`.

  U-071's anchor M-1 is unique and moves from `:339` to `:343`. The new files hash exactly as planned: `WorkerForeground.kt` `0782f85f…` (65 lines), T1 `3e3ea62b…` (517 lines, 13 `@Test`), T2 `c513ea18…` (172 lines, 5), `oldtext_check.py` `8d1158ec…`, `dex_refs.py` `4f72cc2f…`. `ui_bounds.py` does not (B-1).
- **`oldtext_check.py`.** It prints `RESULT mode=start mismatches=0` and `RESULT mode=fixed mismatches=0` on my two simulated trees. The start tree has a CRLF copy of `BackupWorker.kt`, U-070's fixture text from its plan `:1522-1973`, and `RestoreWorkerCheckpointTest.kt` and `proguard-rules.pro` at `fd70325a`.
- **The `git grep` expectations.** `setForeground(` gives 6 lines before the fix and 5 after: the 4 comment lines plus `WorkerForeground.kt:45`. `setForegroundOrContinue` gives `BackupWorker.kt:127`, `RestoreWorker.kt:148` and `WorkerForeground.kt:39`. `git grep -n assumenosideeffects fd70325a -- app` prints nothing (exit 1).
- **The helper (item 2).** It follows the story's "exactly what is caught" and ruling 60:
  - it catches `IllegalStateException` (API 1) and rethrows a `java.util.concurrent.CancellationException` first;
  - the `SDK_INT >= S` test short-circuits before `ForegroundRefusal`, the only class that holds the `instance-of` of the API 31 type;
  - the two Android 14 type exceptions and other `IllegalStateException`s reach `throw e`; `SecurityException` and every `Error` never enter the catch;
  - it writes one `Log.w` with the tag, the site and the simple class name, with no throwable and no message text.

  On ART, an unresolvable type in `instance-of` is at worst a soft verification failure, and it is confined to a class that is never loaded below API 31. The holder argument is sound and conservative.

  F13 really does run the catch path at SDK 24. The updater throws `IllegalStateException("x")`, the helper catches it, the `SDK_INT` test is false, and the helper rethrows. On the JVM, F13 proves the short-circuit, not ART's verifier behaviour. The plan says so (2.1 (a) to (d), M-X1 informational), which matches SC-2: the unit test, the dex check, and an emulator run where possible.
- **Tests (item 3).** I checked the signatures against the cached artifacts with `javap`:
  - work-testing 2.9.1: `TestListenableWorkerBuilder` has `setWorkerFactory`, `setTags(List<String>)` and `setForegroundUpdater(ForegroundUpdater)`; there is a Kotlin factory `TestListenableWorkerBuilderKt.TestListenableWorkerBuilder(Context, …)`; `TestForegroundUpdater()` is public; `WorkManagerTestInitHelper.initializeTestWorkManager(Context, Configuration)` exists.
  - work-runtime 2.9.1: `ForegroundUpdater.setForegroundAsync(Context, UUID, ForegroundInfo)` and `CoroutineWorker.setForeground`.
  - Robolectric 4.15.1: `ShadowLog$LogItem` has public `type`, `tag`, `msg`, `throwable`.
  - App classes: the `BackupWorker` constructor (7 parameters) and the `RestoreWorker` constructor (9 parameters) in the post-U-070 blobs match the tests' factories. `MailTransportFactory` is a `fun interface`. `AuthPreferences.setImapUser` is public. The U-070 fixture API in its plan matches (`add(date, address, body, type, person, id)`, `beforeQuery`, `registerContacts()`, `watermark()`, `SmsFixture.D`, `OWNER`). The folder keys `imap_folder` and `imap_folder_calllog` (`DataType.java:66-67`) are real.
  - Kotlin is not built with warnings as errors (`-Werror` is javac only, `app/build.gradle:203`), so the mutants' unused imports compile.
- **Red at Start and green after.** Reasoning from the code gives the plan's split: 7 red (F1, F3, F4, F5, F7 and R2 with the refusal; F6 with "INCOMING: foreground requests"), 11 green, and all 18 green after the fix. No red test can pass without the fix. In the SKIP path, `create()` comes before `executeSkip` (`BackupWorker.kt:150`, `:187`), and `skipSms` queries the SMS double, so F8's order assertions hold for SKIP too.
- **SDK 35 offline (P-1).** These i7 jars are in the local Maven cache: `android-all-instrumented` `15-robolectric-12650502-i7`, `14-robolectric-10818077-i7` and `7.0.0_r1-robolectric-r1-i7`. There is no i7 jar for 31 to 33. Robolectric 4.15.1's `DefaultSdkProvider` lists 34 = `14`/`10818077` and 35 = `15`/`12650502`, and `DefaultSdk` builds the version `<android>-robolectric-<build>-i7`. The production code does not call `registerReceiver`, so SDK 34+ receiver-flag enforcement does not affect start-up.
- **Failure injections (item 4).** I derived every failing set myself from the test text. All twelve match the plan:
  - M-F1a {F1, F3, F4, F5}; M-F1b {R2};
  - M-F2 {F9, F10, F12, F13, R3, R4, R5}. F11 passes because `SecurityException` is not caught. F12 fails at "doWork() ends by throwing a CancellationException" because the job is cancelled but `doWork` returns `retry()`.
  - M-F3 {F10, R4}; M-F4 {F6, F7};
  - M-F5 {F1, F3, F4, F5, F8, F9, F10, F11, F12, F13};
  - M-F6 {F1, F3, F4, F5, R2}; M-F7 {R1 to R5}.

  Each old text is unique in the fixed tree. U-070's M-L11, M-L20 and M-L22 match U-070's plan I8 (B10/B12/B13, B17/B23, S3, over the same NEW5). U-070's story corrections (M-L8 (a), M-L4 (a)) do not touch them. FG3 passes under all three re-runs.
- **Commands (item 5).** No external `diff` or `cmp` anywhere. Every byte-for-byte comparison is between two shell-written files (`ls` listings) or uses `sha256sum` and `git hash-object` on the tracked files. No Python-written file is compared byte-for-byte. The tools the plan calls exist (`dexdump.exe` in build-tools 36.0.0, `adb`, `emulator`, the AVDs `Pixel_9_Pro` and `Pixel_API_24`).

### Iteration 1 — Blockers

**B-1. `ui_bounds.py` does not match its planned blob.**
- **Where:** 3.5 (`plan.md:1287-1310`); I0 step 1 (`:1377-1378`); row D13.
- **What:** The plan gives blob `c7f765f0af1f0696a7ef86944cbd9251137c5c8c`. The text at `:1288-1309`, written LF exactly as printed (22 lines, no CR), hashes to `c7f3601376dafcfd2443cb755948a0c447f3ddba`. Without the final newline it is `037616f6…`; with CRLF it is `f61df7da…`. None of these is `c7f765f0…`. The other five new files hash exactly as planned.
- **Why it blocks:** I0 step 1 requires `c7f765f0…`. D13 says "correct until the blob matches; never run a variant". The developer can never match it, so I0 cannot finish, and it fails before any baseline is taken.
- **Fix:** Replace the blob in 3.5 (and in I0 step 1's expected list) with `c7f3601376dafcfd2443cb755948a0c447f3ddba`. Alternatively, correct the text to the one whose blob is `c7f765f0…` and say what changed.

**B-2. Section 9 does not follow rulings SC-1, SC-2, SC-3 and SC-9, and step (vi) rests on a false claim.**
- **Where:** section 9, step (iii) Pass (`:1832-1836`), step (i) Pass (`:1859-1863`), (v) (`:1887`), (vi) (`:1889-1893`), Record (`:1895-1904`); 10.7 (`:1927`, "Rulings asked").
- **What:**
  - (a) **SC-9.** (i) Pass requires "the test SMS in the mail account's SMS folder (SC-9)". SC-9 is ruled "not done": the confirmed-append log line is the evidence. That line exists: `BackupWorker: watermark advanced for $type to $confirmedMaxDate`, `Log.d`, post-U-070 `BackupWorker.kt:439`, written only after `appendMessages` returned. The Pass rule does not ask for it.
  - (b) **SC-1.** (iii) Pass accepts the notification "in `iii-notification.txt`, or visible in the screenshot". (i) Pass adds "and none in the screenshot". Screenshots cannot enter the repository, so a pass that rests on one cannot be evidenced. SC-1 says the evidence is the sanitised `dumpsys notification` lines and the app's log lines.
  - (c) **SC-3.** (v) is "Not run (SC-3) unless the owner approves the airplane-mode change". The ruling is "not run", recorded "not run: needs a system setting".
  - (d) **SC-2.** (vi) is "only if the owner agrees", "informational; required only under row D25". The ruling makes the run the orchestrator's whenever it can be done without an account and a password. Otherwise it is recorded "not run: needs the owner to enter the app password". The plan also says that without a mail account a backup "gives MailException — retrying, which still passes the class-loading question". That is not what the code does. `MainActivity.performAction` (`MainActivity.java:391-402` at `fd70325a`) shows the `MISSING_CREDENTIALS` dialog when `isLoginInformationSet()` is false, and `FIRST_SYNC` on a first backup. No worker runs, so the helper is never reached. (vi) also gives no way to pick the `Pixel_API_24` serial: preconditions step 1 stops when two emulators run, and (vi) does not say to stop `Pixel_9_Pro` first or how to select by `emu avd name`.
  - (e) 10.7 still asks for rulings that have been made.
- **Why it blocks:** As written, the orchestrator must either do what a ruling forbids (check the mailbox, ask the owner about airplane mode), pass a step on evidence that cannot be kept, or run a (vi) that cannot reach the code it claims to test.
- **Fix:**
  - (i) Pass: replace the mailbox clause with "the line `BackupWorker: watermark advanced for SMS to <date>` between that work's start line and its `Worker result` line (SC-9; the confirmed-append evidence)".
  - (iii) and (i) Pass: the notification evidence is the `dumpsys notification` record only. Drop "or visible in the screenshot" and "and none in the screenshot". Screenshots stay in `OW` as aids and never decide an outcome. The Record keeps their SHA-256 at most.
  - (v): "Not run: needs a system setting (SC-3)."
  - (vi): run by the orchestrator under SC-2. Select the device by `emu avd name` = `Pixel_API_24`, with `Pixel_9_Pro` stopped first and `Pixel_10_Pro` never started. Install and launch. If login information is not set (the Backup button opens the missing-credentials dialog), record "not run: needs the owner to enter the app password" (not a failure). Delete the "MailException — retrying" sentence.
  - 10.7: replace with "SC-1 to SC-9 ruled (Record 23); section 9 follows them".

**B-3. Step (iv) can pass when no off-screen run was observed.**
- **Where:** section 9 (iv) (`:1883-1885`).
- **What:** (iv) passes when `ForegroundServiceStartNotAllowedException` and `foreground start not allowed` count 0 over `i-logcat.txt` and `ii-logcat.txt`. (i) can end "not run: held by the system's job quota" twice (step 5 and its one repeat). (ii) can end "not reached". In that case both logs hold no off-screen backup, the counts are 0, and (iv) "passes".
- **Why it blocks:** It lets an unobserved run count as a pass, which the story forbids ("never reported as passed").
- **Fix:** "(iv) is assessed only over runs that started off screen and were observed: the INCOMING run of (i) that met the off-screen rule, and the REGULAR run of (ii) if it started. Pass at 0 when at least one such run exists. With none: (iv) 'not run: no off-screen run observed'."

**B-4. The UI dump is read back with an `/sdcard` path that Git Bash rewrites.**
- **Where:** section 9 (iii) step 3 (`:1822`): `"$ADB" -s $SER exec-out cat /sdcard/u075-ui.xml > "$OW/u075-ui.xml"`.
- **What:** This line has an `/sdcard` path but no `MSYS_NO_PATHCONV=1`. The plan's own rule (`:1801-1802`) and CLAUDE.md both require it. Git Bash converts `/sdcard/u075-ui.xml` into a Windows path under the Git install, the device `cat` fails, and the local file is empty. `ui_bounds.py` then dies with an XML parse traceback, which is none of its documented outputs.
- **Why it blocks:** Step (iii) stops with "(iii) not run: button not found" on every run, so AC-12 (iii) and IC-3's device leg cannot be met.
- **Fix:** `MSYS_NO_PATHCONV=1 "$ADB" -s $SER exec-out cat /sdcard/u075-ui.xml > "$OW/u075-ui.xml"`.

**B-5. The 900-second waits cannot run as the plan says.**
- **Where:** section 9 (i) step 3 and (ii) step 3 (`timeout 900 … logcat …`, `:1847`, `:1873`); the rule at `:1804` ("Every `logcat` wait is a foreground call with a tool timeout above its own `timeout`"); also (ii) step 2's `timeout 600`.
- **What:** The tool's foreground ceiling is 600,000 ms, so a 900 s `timeout` cannot sit inside a larger tool timeout, and `timeout 600` cannot either. On this setup a call that long is cut off or moved to the background by the harness, which the plan's own D29 treats as unreliable for Gradle. In (i), the screenshot and `dumpsys notification` steps are chained after the wait with `&&` and are lost if the wait is cut off.
- **Why it blocks:** The 15-minute windows that AC-12 (i) and (ii) require cannot be observed as written. The orchestrator must improvise the wait.
- **Fix:** Split each long wait into calls of at most 540 s, each a foreground tool call with timeout 600,000 ms, anchored at the same `-T "$D1"` (or `$BOOT_T`) so a later call still sees an earlier match. Example: `timeout 450 … logcat -T "$D1" -m 1 -e '<re>'`, repeated once on exit 124. Then decide on the combined result. For (ii) step 2, use `timeout 540`. Alternatively, start them with the tool's background mode, each line writing an `exit=` marker, and wait for the completion notice, never polling. Say which.

**B-6. The decision table has no row for several expected-value mismatches, one of them at the final gate.**
- **Where:** section 4, I6 step 5 (`:1511-1531`, rows given: D23, D30, D31), I0 steps 2 and 4, I1 step 2, I4 step 1; section 7.
- **What:** No row covers:
  - (a) I6 step 5: blobs or `sha256sum` not equal to `HB HR HF` / `SB SR SF`; porcelain not the five paths; `test-tracked-unchanged-exit` not 0; `ignore-exit` not 1; `@Test` counts not 13 and 5.
  - (b) I0 step 2: `toolchainVersion` not 25; a Gradle JVM listed before `jps-gradle-end`; `@Ignore` lines other than the two; and I0 step 4: the Daemon JVM line not citing the criteria file or not Java 25.
  - (c) I1 step 2 and I4 step 1: porcelain not as stated, `main-clean-exit` not 0, or `oldtext_check.py fixed` not 0 at I4. D15 names I3 only.

  (a) is the case where a mutation or a stray edit is present after G-F has run, or an existing test was edited (AC-8, AC-11: "a mutation is never present at a gate").
- **Why it blocks:** At each of these points the developer has to guess. At (a) the plan would otherwise hand off a gate result over code that differs from the tested and mutated code.
- **Fix:** Add rows such as:
  - D32 for (a): "Stop; G-F is void; apply the recovery note of 4.8 to restore `SB SR SF`; report; never hand off."
  - D33 for `test-tracked-unchanged-exit`, `ignore-exit` or the counts: "Stop and report (AC-8, AC-9)."
  - D34 for (b): "Stop before any Gradle call and report (a running Gradle JVM: start nothing, D29's rule; a wrong JDK: AC-9's start condition)."
  - D35 for (c): "Stop and report; do not edit." (or extend D3/D15 to I1 and I4).

  Cite each row in its step's **Rows:** line.

### Iteration 1 — Observations

**O-1. A cut-off targeted run in I5 (D29 against 4.8 step 5).** Protocol step 5 says "Restore at once … whatever the result", but D29 says "stop and report" for a log without `exit=`. State in D29 that inside an I5 entry the restore and the clean check (steps 5 and 6) come first, then the stop. Then no reading leaves a mutation in the tree after a cut-off run.

**O-2. No clean check before G-F.** I6 step 1 starts G-F without the 4.8 clean check. Add an I6 step 0: `sha256sum $BW $RW $WF` = `SB SR SF`, `PROD_DIFF` unchanged, and `mutation-log.md` with no `APPLYING` entry lacking `restored`. The last I5 entry's check covers this in one session, but not after a resume.

**O-3. AC-2's privacy check for the restore.** R2 checks the warning and the absence of `Log.ERROR`, but not that no log entry carries the configured account name (`backup-owner`, set in `setUp`). AC-2's wording ("Checked by … AC-6's refusal test") can be read as asking for it. A one-line loop over `ShadowLog.getLogs()` for `SmsFixture.OWNER` would close the reading. If it is added, update T2's blob.

**O-4. File count in 4.9.** 1 + 3 + (7 + 1 + 2 + 2 + 1 + 25 + 7 + 1) = 50, not 51 (`:1614`).

**O-5. I0 step 3's `grep -A14`.** U-070's entries carry the write-ahead block and then the appended result lines. Fourteen lines may cut off the `failed` line. Read whole entries (up to the next `### `) and translate U-070's B/S ids to test names through its plan's table 4.8.

**O-6. The `ui_bounds.py` label check.** If the button renders in all caps, uiautomator can report `BACKUP`, which gives `label-is-Backup=False`, and step (iii) stops. That is a false stop, not a false pass. A case-insensitive comparison, or the resource id alone, would avoid it.

**O-7. The cold start in (ii).** `-no-snapshot-save` alone does not prevent a quick-boot load. The observation used the same flag and did get `BOOT_COMPLETED`, so this is not a blocker. Add: "if no `bootup` line appears after `sys.boot_completed`, record '(ii) not run: the emulator resumed from a snapshot'".

**O-8. The inconclusive repeat in (iii).** "Not conclusive … repeat steps 1 to 6 once" has no rule for a second inconclusive result. Record it as "(iii) not run: notification not captured", never as a pass.

**O-9. The test file at SDK 24 (N3, D11 (c)/(d)).** U-070's fixtures have only run at SDK 29. F13 is their first use at 24. The plan's stop rows cover a failure, and I see nothing in the fixture text that needs API 25 or later. This is noted only as the most likely place for an I2 surprise.

### Iteration 1 — What I ran

- `git show`, `git cat-file blob`, `git rev-parse`, `git diff --stat`, `git log`, `git grep` at `fd70325a` and on the two post-U-070 blobs.
- A scratch script that extracts the plan's code blocks by line range, writes them LF, hashes them with `git hash-object --no-filters`, applies E-B1, E-B2 and E-R1 to the post-U-070 blobs with occurrence counts, and reports blobs, line counts and the M-1 position. Also `git diff --no-index --numstat` and `-U0` hunk headers on the scratch copies.
- `oldtext_check.py start` and `fixed` on two simulated trees in the scratch folder.
- Variant hashes of `ui_bounds.py` (as is, without the final newline, CRLF).
- `javap` (JDK 25) on `work-testing-2.9.1` and `work-runtime-2.9.1` `classes.jar` (unzipped into scratch), `robolectric-4.15.1.jar` (`DefaultSdkProvider`, `$DefaultSdk`) and `shadows-framework-4.15.1.jar` (`ShadowLog$LogItem`).
- `Glob` of the local Maven cache's `android-all-instrumented` folder and of the Gradle cache's `androidx.work` and `org.robolectric` folders.
- Existence tests (paths not printed) for `dexdump.exe`, `adb`, `emulator`, and the AVD names.

### Iteration 1 — What I could not check

- The real Gradle compile of the two test files against the built U-070 fixtures (N1), and Robolectric's run-time behaviour at SDK 35 and 24 (N2, N3). U-070 is still being built, so its fixtures and anchors as built are checked only by the plan's I0 `oldtext_check.py start`.
- JaCoCo's line totals for the two new classes (N4), and AGP's own D8/R8 output for the dex check (N5).
- ART's verifier on API 24 to 30 (P3), and whether uiautomator reports the button label in capitals (O-6).
- WorkManager's removal of notification id 1 after a manual run (it matters for (i)'s "no record with id=1": a stale record would be a false fail, not a false pass).
- The device steps themselves.
