---
artifact_type: plan
story_id: "U-068"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-21T11:10:22Z"
iteration: 2
base_commit: "c12a2788b0d1cdd5267529eda30cc7895be60761"   # planning HEAD (iteration 2); every planning blob equals its value at 0d5755c4 (iteration 1)
contracts_verified:
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-005
risks_identified: 12
---

# Implementation Plan: U-068 — Raise minSdk to 24 and Remove What the Floor Makes Dead

Story: `sdlc/artifacts/build/sprints/sprint-024/U-068-minsdk-24-floor.md` (story S1 of DES-MODERNIZATION-014).
Model for structure, conventions and tools: `sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md`. Manifest, R8
and APK evidence recipes: `sdlc/artifacts/build/sprints/sprint-018/U-061/plan.md` (kept-name recipe, `norm()`)
and `sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md` (Step 0.6 inventory).

**Planning state (iteration 2).** Iteration 1 was planned at `0d5755c4`. Iteration 2 is planned at HEAD
`c12a2788` on `sdlc/modernization-plan`. The commits between them are four orchestrator document commits
(`c9770c30`, `e972ab5c`, `fb707d7e`, `d73017de`) and U-067's code commit `c12a2788` (`BackupWorker.kt`,
`RestoreWorker.kt`, worker tests). For all 19 paths of table 2.1, at `c12a2788`, `git status --short` is empty,
`git diff --name-only 0d5755c4 c12a2788` lists none of them, and the HEAD blob, the working-tree hash and the
planning blob are equal. U-067's QA is still running in the checkout and applies temporary mutations to the two
worker files, which this plan does not edit.

**The start commit is not fixed by this plan.** `<start>` is the commit that closes U-067's story (status
`done`), read by the developer at I0 as `HEAD` before the first change. The start count `C0`, the coverage figures
and the lint figures are measured there, and every expectation below that depends on them is a formula in `C0` or
in the I0 measurements, with a decision row. No expectation depends on a fixed count: the authoritative count was
746 at `0d5755c4` and 753 at `c12a2788`, and neither number is used by any step. There are 2 `@Ignore`
annotations on code lines (`AppTest.java:13`, `AuthPreferencesTest.java:1079`).

**Size.** 18 tracked files change: 3 build or documentation files at the root (`build.gradle`, `CHANGES`,
`DEVELOPMENT.md`), `app/build.gradle`, `app/lint-baseline.xml`, 9 files under `app/src/main`, 2 test files,
`sdlc/artifacts/engagement/code-location.md` and `.github/ISSUE_TEMPLATE/500_bug_report.yml`. One test file is
new. **3 tests are added** (the two of `BackupBroadcastReceiverTest` and C-1, ruling P-5), 5 are retargeted and
renamed, none is deleted. There are **7 mutation runs** and about 23 Gradle runs (I0 2, I1 1, I2 4, I3 7, I4 1, I5 3, I6 4, I7 1). 7 local commits, one per
increment, plus fix commits only if a later gate fails.

## Iteration 2 (after the plan gates, 2026-11-21)

Iteration 1's gates: plan-review FAIL (B-1 to B-6, O-1 to O-7), contract-verification PASS (O-1 to O-4),
expert-depth-check PASS with two recommendations and an edge-case list (`plan-review.md`,
`contract-verification.md`, `expert-depth-check.md` in this workspace). The orchestrator ruled P-1 to P-5 (story,
Implementation Notes, "Rulings on the plan's scope concerns"). Each change below is made in place, where the
developer reads it; the table only says where to look. Every blocker was re-verified in this session before it was
fixed (column "Verified").

| Finding | Verified | Change, and where |
|---------|----------|-------------------|
| Ruling P-1 (acknowledged) | story Implementation Notes | I3 steps 2 and 3 now proceed under the ruling (cited) and record the coordinates, size and SHA-512 of the jar **and** of the `.pom`, each checked against its `.sha512` file; D19 (a) to (d) rewritten (two failed downloads stop the story; nothing fetched by hand); 7 item 8, 8.2 P-1, R1, Verdict and the report notes no longer ask for an acknowledgement |
| Ruling P-2, P-4 | story | D24, D28: "accepted as declared differences (ruling P-2/P-4); any unexplained difference stops the story"; 8.2 P-2, P-4 marked ruled |
| Ruling P-3 | story | 8.2 P-3 marked ruled; I6 unchanged |
| Ruling P-5, plan-review B-1 | `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml` (read-only): `SmsReceiver.java` `:98` ci 6, `:99` ci 6, `:102` ci 1 (`enable`, covered); `:105`, `:106`, `:109` ci 0 (`disable`); `compat` 16/73 | C-1 is required and lives in **I5, step 1a**, before the I5 code edits (placement and reasons: 6.1). 6.1 rewritten as exact edits C1.a, C1.b and mutation M-C1 (one Edit); 4.8 Order, recovery note and "No mutation runs after M-C1"; 4.9 row I5 gains `SmsReceiverTest.java`, `I5-c1-first-run.txt`, `mutation-log.md`, `mutations/M-C1-*`; 2.2 `compat` after I5 = **18/71 = 25.4 %, 3 spare lines**, formula with Δc1; D9, D35, D35b; counts `C0 + 3` from I5 on (I5, I7, 5.1, 5.3, Size, Verdict); section 7 items 4, 5, 8.2 P-5, section 11 Maintainability |
| plan-review B-2 | `git archive HEAD gradlew` gives 260 CRLF line ends; with `-c core.autocrlf=false`, 0 | I6 step 2 uses `git -c core.autocrlf=false archive`; `I6-scratch.txt` records the CRLF count of the copy's `gradlew` (expected 0) before the first run there; row D39b; L3 no longer cites `grep -c $'\r$'` |
| plan-review B-3 | I1 commits `build.gradle`, so `git diff $START -- build.gradle` is non-empty at I3 | I3 step 2: two checks, uncommitted against `HEAD` and the files I1 does not change against `$START`, each before and after the first SDK 24 run |
| plan-review B-4 | no step wrote `I3-first-run.txt` | I3 step 3 writes it; CM step 2 checks that every listed file exists before `git add`; every commit list re-checked against its steps (I7's `gradle-version.txt` and every other name matched; I5's list gained C-1's files) |
| plan-review B-5 | AC-4 (e) grep at the start: 6 lines; `-U0` gives 6 hunks for the test file; `grep -c '^[-+][^-+]'` misses `- **minSdkVersion**` lines | I0 step 4 "6 lines"; I3 step 4 uses default context, three hunks `@@ -31,7 +31,7 @@`, `@@ -293,9 +293,9 @@`, `@@ -369,18 +369,29 @@`; I7 step 2 uses `--numstat` (`1	1`) and `grep -E '^[-+]- '`. Every other line or hunk count in the plan re-derived: list in the reply and in "Re-derived values" below this table |
| plan-review B-6 | four orchestrator document commits since `0d5755c4` | Constraint 11 takes U-067's constraint-12 wording; I7 step 5 has three scoped checks (all commits recorded; this story's own files via `--grep='^U-068 S1-'`; no code commit by anyone else); I7 step 7's order check filters by the subject; new row D53 |
| plan-review O-1 | — | as ruling P-1 above |
| plan-review O-2 | `git grep -n "USE_CREDENTIALS\|WRITE_EXTERNAL_STORAGE" -- app/src k9mail-vendored/src` at HEAD: the manifest lines and `TokenRefresher.java:97` | I4 step 4 adds the grep; expected after E4.4: only `TokenRefresher.java:97` |
| plan-review O-3 | — | I6 step 7: on a failed `rm`, `./gradlew --stop` in the copy once, retry, else record and report (row D39c); 4.0 comment says "inside the repository's grandparent folder" |
| plan-review O-4 | — | I7: the log entry is written in step 6 before the scan; step 7 is CM(7) only; the post-commit lines go to the hand-off message, not the log |
| plan-review O-5 | the plan's texts contain `@Test` only as annotations | Section 10 items 13 and 14 |
| plan-review O-6 | story IC-3: "the emulator is shut down afterwards" | Section 9 step 9 follows the story: shut down afterwards in every case |
| plan-review O-7 | AC-8 grep at `0d5755c4`/`c12a2788`: 29 hits; cache also holds `-i7` jars for 14, 15, 16 | L5 says 29; L11 lists them; L3 relies on `git ls-files --eol` only |
| contract-verification O-1 | CR-015 "Order", item 5 | Declined as a change: T1 and T2 stay in I3 with the tests they describe. Reason: CR-015 accepts "true by requirement, not yet by build" wording for this interval, and moving the comments to I5 would split one test edit over two commits. Stated in 3.4 |
| contract-verification O-2 | CNTR-003 `:260` | Section 7 item 9 (for the next contract amendment); no edit |
| contract-verification O-3 | CNTR-005 line citations | Section 7 item 9; no edit |
| contract-verification O-4 | — | 2.4 row "Commits" and section 11 Reliability: a hand rollback goes newest-first (reverting I4 while keeping I5 is the one order the contract's non-breaking claim excludes) |
| expert-depth-check criterion 3 | — | New 2.4 "Decisions and trade-offs": commits, guard deletion, baseline method, each gained / gives way / not chosen / why |
| expert-depth-check criterion 4 | — | New 2.5 "What the floor changes in the toolchain (sources)": AGP manifest merger and `useLegacyPackaging`, AAPT2, D8 at API 24, R8's `SDK_INT` assumption (a `needs_check`, N11, confirmed from the R8 release notes or the debug bytecode diff, not stated as fact), lint `ObsoleteSdkInt` and `NewApi`, Robolectric's SDK jar resolution |
| expert-depth-check edge cases | — | 2.5 and 2.6: a device below the floor (`INSTALL_FAILED_OLDER_SDK`, the installed build keeps running); multidex unaffected; `extractNativeLibs` argument rests on the measured `so=0` although `ndk.abiFilters` is set (D24); resource qualifiers come only from library resources; the transient `ObsoleteSdkInt` finding between I4 and I5; what each before/after comparison can and cannot show (2.6). D26 names `$-CC` companions as a known cause |

**Re-derived values (B-5, "every count of lines or hunks").** Checked against the files at `c12a2788`, or against
the edits applied by hand to the text:
- I0 step 4: AC-7 grep 24 lines (5 imports; `BackupWorker.kt`'s Q line is now `:534` after U-067); AC-8 grep **29**
  (was 31); AC-4 (e) **6** (was 5); AC-11 (f) 9; `maxSdkVersion` 2; `minSdk 21\|SDK 21` in `app/build.gradle` 2.
- I0 step 5: 37 and 38. I1 step 2: one `-` and one `+` line (second character a space, so `[^-+]` matches).
- I3 step 4: **3** hunks with default context (6 with `-U0`, no longer used). I3 step 3: `cases=4`.
- I4 step 4: `app/build.gradle` 6 changed lines; comments-stripped diff only line 24; manifest `@@ -38,14 +37,0 @@`,
  0 added lines; the new O-2 grep: 1 line.
- I5: C-1 first run `cases=5`, 5 lines with `@Test` in `SmsReceiverTest.java`; AC-7 grep 14 lines, 12 without
  imports; AC-8 1; `git diff -w -U0` of the role helper 5 `-` lines, 0 `+`; name-only 8 files; `git diff --stat HEAD -- app/src/test` 1 file.
- I6: `0	121`; 26 entries.
- I7: `DEVELOPMENT.md` **`1	1`** by `--numstat` and 2 lines by `grep -E '^[-+]- '` (was a `grep -c` that prints 0);
  template grep 0 lines; CR-016 D-5: 2 lines per file, 1 and 1, 4 diff lines per file, 0 criterion or status lines;
  sweep 7; AC-9 nine files; `app/build.gradle` floor-word count 0.
- Counts: `C0 + 2` after I2, I3, I4; **`C0 + 3`** from I5 on.

## 0. Binding Inputs

| Source | What binds this plan |
|--------|----------------------|
| Story, "Orchestrator rulings before planning" (Record 16) | SC1 (IC-4): not waited for; recorded "not run: needs the product owner's decision PO-A" if PO-A is undecided at the device step. SC2: 37 entries, not 38; no design note. SC3: `DEVELOPMENT.md:58` corrected in S1-I7, minSdk token only; the other leftover sites go to a follow-up. SC4: no test is added for the pre-Q branch of `SmsDefaultRoleHelper.startRestore`; the `SmsReceiver` pair is told apart by JaCoCo branch evidence. SC7: U-068 runs before U-069, never at the same time; U-067 is done before U-068 starts. AC-16: the developer makes one local commit per increment, explicit paths, the plan's subjects, no push, no amend; review fixes are further commits |
| Story, "Bound decisions" | Table S as written; table T as written (retarget, no deletion); `BackupBroadcastReceiverTest` first; the two permissions removed; JUnit XML on, HTML off; Robolectric default stays 29; no Gradle, dependency or verification-metadata change; CR-015's ruling stands; the broadcast path is never cited as INV-4 evidence |
| DES-MODERNIZATION-014 | Section S1 (increments S1-I0 to S1-I8 with their "must hold" checks, `:539-549`), D2 (`:462-530`), D4 with tables S and T, D4a and D4b (`:795-880`), D5 "Outside Gradle verification" (`:920-926`: "S1 adds SDK 24's jar"), D8 (`:1017-1066`), D9 (`:1068-1084`), D17 (`:1606-1648`), D18 (`:1650-1673`), D19 (`:1675-1692`), INT-004 (`:1737`, `:1764`), Risks 2 and 5 (`:2034`, `:2037`), and the dated note under the H1 (`:44-52`: P-1 is CR-015, P-2 and P-3 are CR-016, both `implemented`) |
| REQ-MODERNIZATION-015 | AC-5 to AC-10 (`:124-154`); AC-12 (`:162-164`); AC-13 as amended by CR-016 Amendment 1 (`:165-180`, the A-7 text); AC-14 (`:181-185`) |
| CNTR-MODERNIZATION-004 v3 | INV-4 row and the note "INV-4 at the minSdk 24 floor (v3, CR-015)" (`:243-245`); Versioning (`:251`); Breaking change policy, both bullets (C-8 and C-9 of CR-015, `:266-267`) |
| CR-015 | "What S1's plan must cite" (`:345-357`) and its restatement (`:653-658`); Amendment 1 note on report finding 8 (`:487`: the five points for the S1 author); "Order relative to the code" (`:493-530`). Section 8.1 below cites each point |
| CR-016 | D-5 (`:404-419`, the three AC-9 checks); Amendment 1 A-7 (`:551`, the count rule now in REQ-015 AC-13) |

## 1. Assumption Ledger

`verified` = read or run read-only in this planning session, with its location. `needs_check` = a developer
step with a decision row for each outcome. Nothing material is left assumed without a row. Line numbers are at
`0d5755c4`; for every file this plan edits they are the same at `c12a2788` (equal blobs, iteration 2).

| # | Claim | Status | Evidence |
|---|-------|--------|----------|
| L1 | Iteration 1: HEAD `0d5755c4`; parent `0b7575ff`; that commit changes only three `sdlc` files. Iteration 2: HEAD `c12a2788` (U-067's code commit after four document commits); none of the 19 paths of table 2.1 moved (`git diff --name-only 0d5755c4 c12a2788`, `git status --short`, blobs). Counts (not used by any step): 746 and 753; `@Ignore` on code lines 2; `git status --porcelain -- app k9mail-vendored build.gradle gradle settings.gradle gradle.properties gradlew gradlew.bat` empty | verified | `git rev-parse`, `git log`, `git diff --stat 0b7575ff 0d5755c4`, the CLAUDE.md count command, `git grep -hE '^\s*@Ignore(\s\|\(\|$)' HEAD -- 'app/src/test/**'` |
| L2 | Planning blobs of every file this plan edits (table 2.1) | verified | `git rev-parse HEAD:<path>` |
| L3 | Line endings: the working tree is CRLF and the index LF (`core.autocrlf true`) for every edited file except `app/build.gradle` and `sdlc/artifacts/engagement/code-location.md` (LF both). `git hash-object` normalises, so working-tree hashes compare with index blobs. `gradlew` is LF in index and working tree; `git archive` applies `core.autocrlf` and writes it CRLF unless `-c core.autocrlf=false` is given (I6). `grep -c $'\r$'` is not reliable in this Git Bash (it reports CRLF for LF files) and is not used as evidence | verified | `git ls-files --eol`; byte counts with Python on `git archive` output (260 CRLF with the default, 0 with `-c core.autocrlf=false`) |
| L4 | The AC-7 grep at HEAD prints 24 lines: 5 `import` lines (`App.java:35`, `MainActivity.java:19`, `ThemeActivity.java:3`, `CalendarAccessorPost40.java:3`, `SmsReceiver.java:3`) and 19 code lines, 7 at level 24 or lower (`Dialogs.java:172`, `MainActivity.java:425`, `SmsDefaultRoleHelper.kt:74`, `CalendarAccessorPost40.java:23`, `SmsReceiver.java:31`, `:50`, `WorkManagerScheduler.kt:224`) and 12 at 25 or higher (`App.java:246`, `:350`; `MainActivity.java:242`, `:517`; `SmsDefaultRoleHelper.kt:77`, `:111`, `:135`; `ThemeActivity.java:20`, `:26`; `SmsReceiver.java:47`; `BackupWorker.kt:533`, `:534` at `c12a2788` after U-067; `RestoreWorker.kt:707`) | verified | `grep -rnE "SDK_INT\|VERSION_CODES\|TargetApi\|RequiresApi" app/src/main` (scope verified via grep) |
| L5 | The AC-8 grep at HEAD has 29 hits (17 in `app/src/main/java`, 7 in the `service` test folder, 5 in the `compat` test folder); after the story only `compat/package-info.java:2` is to remain. Every other hit is removed by a planned edit (table 3.6) | verified | the AC-8 grep, run read-only |
| L6 | Each removed import has no other use: `Dialogs.java` `Build` only at `:25`, `:172` (`AlertDialog.Builder` is another class); `CalendarAccessorPost40.java` `Build` `:10`, `:23` and `TargetApi` `:3`, `:23`; `SmsReceiver.java` `TargetApi` `:3`, `:31` (`Build` stays for `:47`); `MainActivity.java` `TargetApi` `:19`, `:425`; `WorkManagerScheduler.kt` `Build` `:20`, `:224` (the other matches are `Builder`) | verified | `grep -n` per file |
| L7 | Manifest `:38-42` and `:43-51` are as the story says; `:37` is blank and `:52` opens the INTERNET comment. In the last recorded merged manifest each of the two permissions appears once (no library contributes them), and the `<application>` element carries `android:extractNativeLibs="true"` | verified | `app/src/main/AndroidManifest.xml` read in full; `sprint-018/U-061/evidence/after-merged-debug.xml:47-64`, `:127` |
| L8 | `build.gradle:29-30`; `app/build.gradle:24`, `:153`, `:160`; 18 rules; `jacocoTestCoverageVerification` writes no report, so per-package figures need `:app:jacocoTestReport` (XML at `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`, `:264-296`) | verified | both files read in full |
| L9 | Latest recorded per-package figures (U-063, sprint 020): `receiver` 29/58, `compat` 16/73, `scheduler` **140/188** (74.5 %), `activity` 189/1011, `calendar` 37/50. The `scheduler` comment in `app/build.gradle:539` (102/164) is stale. U-064 to U-066 changed no file of these packages; U-067 changes only `service` | verified (by reading evidence) | `sprint-020/U-063/evidence/FIX-jacoco-packages.txt` |
| L10 | `BackupBroadcastReceiver.java` has 11 executable lines (`:38`, `:43`, `:45`, `:46`, `:48`, `:51`, `:52`, `:54`, `:56`, `:58`, `:68`); `LOCAL_LOGV` is `DEBUG` = `BuildConfig.DEBUG` (not a compile-time constant), so `:43` has bytecode. The seam is `getScheduler(Context)` `:67-69`. `Preferences(Context)` uses `androidx.preference.PreferenceManager.getDefaultSharedPreferences` (`Preferences.java:22`, `:65-67`); key `third_party_integration` (`:93`), default `false` (`:177-178`) | verified | files read in full |
| L11 | Robolectric: `robolectric.properties:8` `sdk=29`; the local Maven cache holds `android-all-instrumented` jars for 5.0.2 (SDK 21), 6.0.1 (SDK 23) and 10 (SDK 29), each `-i7` (also `-i7` jars for 14, 15 and 16, and older `-i6` ones), each with `.jar.sha512`, `.pom` and `.pom.sha512` beside it, and **none for 7.0 (SDK 24)**; `gradle/verification-metadata.xml` has no `android-all` entry at all; no `robolectric.offline` or `robolectric.dependency.*` setting exists in the build files, `gradle.properties` or `app/src/test/resources` | verified | listing of the cache folder; `grep -n "android-all" gradle/verification-metadata.xml` (no line); `grep` of the settings |
| L12 | Lint baseline: 37 entries by `grep -c '^ *<issue *$'`, 38 by a plain `<issue` match (the root `<issues` tag); header `lint 8.7.3`. The 11 expected stale entries are at lines 169-178 (`Deprecated`, `BackupJobs.java:68`) and 213-321 (the 10 `ObsoleteSdkInt`), each block followed by one blank line (`:179`, `:223`, ... `:322`). Rule-ID totals: ObsoleteSdkInt 10, Range 6, RtlHardcoded 6, GradleDependency 5, NonConstantResourceId 3, one each of DataExtractionRules, Deprecated, ExportedReceiver, IgnoreWithoutReason, MonochromeLauncherIcon, RedundantLabel, SystemPermissionTypo | verified | file read; counted with `grep` and `awk` |
| L13 | At AGP 9.0.1 (U-060): "Lint found 39 errors (and 22 errors filtered by baseline)", and "16 errors/warnings were listed in the baseline file ... but not found in the project". So the "not found" count is already above zero before this story; AGP 9.0.1 itself names `gradlew updateLintBaseline` | verified (by reading evidence) | `sprint-017/U-060/evidence/step8-ci-command.log:228-230`, `:304` |
| L14 | SDK tools: build-tools 36.0.0 has `aapt2.exe` and `dexdump.exe`; platform-tools `adb.exe`; `emulator/emulator.exe`; no `cmdline-tools`; system images 29, 30, 33 to 37 (no 24); AVDs `Pixel_9_Pro`, `Pixel_10_Pro`, `Pixel_4`, `Pixel_4_2`, `Pixel_7`. The SDK folder is read from `local.properties` (`sdk.dir`) by `tools/sdk_dir.py` (section 4.0), tested read-only in the planner's session scratch folder | verified | listings; the helper run without printing the path |
| L15 | JDK tools: `javap.exe` and `jps.exe` are in the JDK 25 `bin` folder under `Program Files/Java/jdk-25.0.5.1`, not on PATH; `java -version` 25.0.5.1; `unzip`, `sha256sum`, `sha512sum`, `diff`, `comm`, `python` on PATH | verified | listings |
| L16 | Release outputs: `app/build/outputs/apk/release/app-release-unsigned.apk`; `app/build/outputs/mapping/release/{mapping.txt,configuration.txt,seeds.txt,usage.txt}`; javac classes under `app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/`; Kotlin classes under `app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes/`; vendored `k9mail-vendored/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/fsck/k9/mail/Address.class`. `:app:assembleRelease` also runs `lintVitalRelease` | verified | listings of the existing build folder (read-only) |
| L17 | CR-016 D-5 checks pass at HEAD: `CR-016` is in `change_records` and in a dated note of both REQ-001 (`:13`, `:18`) and REQ-006 (`:17`, `:24`); `git diff f0d0d17e^ f0d0d17e` of each shows only the `change_records` list and the note (this closes the story's "not verified" item for REQ-006) | verified | `grep -n`; the two diffs read line by line |
| L18 | U-069 AC-13 (b) relies on the `WorkManagerScheduler.kt` sentences at `:61` and `:212-213` ("enqueues a delayed [BackupWorker]"). This plan rewords `:58-60`, `:208-210` and `:256` only, so both sentences stay byte-identical | verified | U-069 story `:102`; the edits in 3.6 |
| L19 | The old test name `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` contains `pre24`, which the AC-8 grep (`pre-?24`) matches. The new text in `WorkManagerSchedulerTest.kt` therefore never quotes the old name; the rename is recorded in the implementation log only. Likewise the old `SmsReceiverTest` names contain "KitKat" | verified | the AC-8 pattern applied to the names |
| L20 | `compiled-pipeline.json` of this workspace is excluded locally (`.git/info/exclude`) | verified | `.git/info/exclude` |
| N1 | Robolectric 4.15.1 on JDK 25 runs `SmsReceiverTest` at `N` and the WorkManager test harness at SDK 24 | needs_check | I3 steps 3 and 4; rows D19 to D21 |
| N2 | The SDK 24 jar Robolectric 4.15.1 asks for is `android-all-instrumented` `7.0.0_r1-robolectric-r1-i7`, fetched by Robolectric's own resolver at test time into the local Maven cache, outside Gradle's dependency verification (design D5, INT-004) | needs_check | I3 step 2; row D19 |
| N3 | AGP 9.0.1 derives `android:extractNativeLibs` in the merged manifest from the floor (legacy packaging is the default below API 23 only), so `"true"` may become `"false"` at minSdk 24 | needs_check | I4 step 5; row D24 |
| N4 | AAPT2 collapses resource version qualifiers at or below the floor, so `res/` entry paths of `-v22`, `-v23` or `-v24` resources may change in the APKs | needs_check | I4 step 6; row D28 |
| N5 | The R8 kept-name set and `configuration.txt` are unchanged by the floor and the removals | needs_check | I4 step 6, I5 step 6; rows D26, D27 |
| N6 | Where `:app:lintDebug` writes a report that `lint_ids.py` can read (XML under `app/build/reports/`, or the intermediate text report) | needs_check | I0 step 6; row D10 |
| N7 | `:app:updateLintBaseline` exists at AGP 9.0.1 and writes the configured baseline file | needs_check | I6 step 3; row D40 |
| N8 | The regenerated baseline lacks exactly the 11 expected entries' keys (id, file, message) | needs_check | I6 step 4; rows D41, D42 |
| N9 | JaCoCo line attribution: S-7 removes 3 lines (`:224`, `:227`, `:229`; the literal concatenation at `:227-228` is one constant), S-4 removes 2 (`:50` covered, `:53` missed), S-6 removes 2 (`:74` covered, `:101` missed) | needs_check | I3 and I5 coverage records; rows D9, D35, D36 |
| N10 | `Pixel_9_Pro` holds the configured app at versionCode 1602 with minSdk 21; `install -r` of the new debug build succeeds | needs_check | section 9; rows in 9 |
| N11 | The R8 bundled with AGP 9.0.1 treats `Build.VERSION.SDK_INT` as at least the min-api and folds comparisons it decides (as if an `-assumevalues` range from min-api were present). Not stated as fact: the developer confirms it from the R8 release notes for that R8 version, or, if no note is found, records "not confirmed" | needs_check | I5 step 7 (2.6); no row depends on the outcome: the equivalence proof rests on the debug bytecode and the tests either way |
| L21 | `SmsReceiver.enable` (`:98`, `:99`, `:102`) is already covered (ci 6, 6, 1) and `disable` (`:105`, `:106`, `:109`) is not (ci 0), in the last JaCoCo report in `app/build`; `compat` there is 16/73. Robolectric 4.15.1's `ShadowApplicationPackageManager` stores and returns a component's enabled state with no manifest check (plan-review, read from the jar with `javap`) | verified (L21 by reading the report read-only; the shadow fact by the reviewer) | `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`; plan-review `:49` |

## 2. Design (decided)

### 2.0 Principles applied

Book chapters are cited as commonly given; I did not re-read them in this session.

- **Dead code at a platform floor: Remove Dead Code** (Fowler, *Refactoring*, 2nd ed., ch. 8). Each of the seven
  sites is a guard whose outcome is fixed for every SDK ≥ 24, so the guard and the branch it never takes are dead.
  The proof that behaviour is unchanged is a case analysis per site over the supported domain s ≥ 24 (2.3),
  backed by tests where they exist and by a normalised bytecode comparison where the change is an annotation
  (I5 step 7).
- **The receiver test: a characterisation test through an object seam** (Feathers, *Working Effectively with
  Legacy Code*, ch. 13 and ch. 4; the technique "Subclass and Override Method", ch. 25). The protected
  `getScheduler(Context)` (`BackupBroadcastReceiver.java:67-69`) is the enabling point; the production class is
  not edited (CNTR-MODERNIZATION-005 is frozen).
- **The retargeted tests: boundary-value analysis** (Myers, *The Art of Software Testing*, ch. 4). The floor, API
  24 (`N`), is the lowest value of the input domain and the boundary for the pre-Q partition; Q (API 29) is the
  boundary of the role partition. Before this story the four `SmsReceiver` tests all ran at 21 and took the same
  branch, a case of **Test Code Duplication** (Meszaros, *xUnit Test Patterns*, ch. 15); after it they cover both
  partitions, and the JaCoCo line evidence plus mutations M-R1 and M-R2 show which test runs which branch.
- **Mutation analysis** (DeMillo, Lipton and Sayward, "Hints on Test Data Selection", IEEE Computer, 1978). Every
  new or retargeted test is shown to fail against the mutant it guards. M-W2 is the boundary mutant: it moves the
  dead branch's condition from `< N` to `<= N`, which is exactly the regression "the content trigger is not
  registered at the floor"; only the new API 24 test catches it.
- **The lint baseline as a ratchet** (Ford, Parsons and Kua, *Building Evolutionary Architectures*, ch. 2,
  fitness functions). Removal-only edits keep the ratchet monotone: no finding can enter the baseline by this
  story. The oracle is a semantic diff against a regenerated baseline (D17), not a text diff, because lint
  rewrites line numbers and messages between versions.
- **Small, attributable increments and revert-based rollback** (Humble and Farley, *Continuous Delivery*, ch. 1
  and ch. 10). One commit per increment, in the ruled order, so `git revert` newest-first is the rollback (D19)
  and every regression points at one commit.

### 2.1 Planning blobs (start-state reference; row D4)

| File | Blob at `0d5755c4` | Lines |
|------|-------------------|-------|
| `build.gradle` | `241bf3d934b7547bee2d153c74f1eba8b0959845` | 53 |
| `app/build.gradle` | `0ab6ef75326d581f1820ba7e991bc388f2c0fbb2` | 594 |
| `app/src/main/AndroidManifest.xml` | `1077c3c112c81cce93cbf38e5bc7bd339a93de81` | 251 |
| `app/src/main/java/com/zegoggles/smssync/App.java` | `204b10178482bf6381f52338cfcc895537cdb418` | 408 |
| `.../activity/Dialogs.java` | `a8fa917371d60911a9b5120a3fefbf8f2f70aa24` | 382 |
| `.../activity/MainActivity.java` | `087fcfe6c9a8e419028d30cbd0b312f29c528cbb` | 571 |
| `.../activity/SmsDefaultRoleHelper.kt` | `1249781fcc1c20f86821280e13364b86310a769c` | 158 |
| `.../calendar/CalendarAccessorPost40.java` | `d6f54c79d1b884addb08a6b1db16f5bbbfb53b88` | 108 |
| `.../compat/SmsReceiver.java` | `30b78e48aefc73fae08ab8f38cf7189bf859abdd` | 110 |
| `.../scheduler/BackupScheduler.java` | `58629d6e65e252a89013b77b5eaf4efd22004055` | 183 |
| `.../scheduler/WorkManagerScheduler.kt` | `0ae361ec16c09a3632eef1aff7c6d184344cf4c7` | 489 |
| `.../receiver/BackupBroadcastReceiver.java` (not edited; mutated in I2) | `b038d7fdb8b50d5cddd32ef1899736c29efc7274` | 70 |
| `app/src/test/java/com/zegoggles/smssync/compat/SmsReceiverTest.java` | `d5488c92ed38e7fab7490eac30e1e3715e0077fc` | 46 |
| `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt` | `ff26b56faec805abafa1c6345857509391268771` | 591 |
| `app/lint-baseline.xml` | `95d9aba05f66ec34cd76c1ab8183c4d16d339b87` | 411 |
| `CHANGES` | `647f172b4e5ddd7526e4649755d385c324fd44c9` | 303 |
| `DEVELOPMENT.md` | `5b38fa7c808a43b322f5e4a0a16fe51e207f4de8` | 89 |
| `sdlc/artifacts/engagement/code-location.md` | `61b417651d4a011309b830098675c7340206af1c` | 119 |
| `.github/ISSUE_TEMPLATE/500_bug_report.yml` | `5f1dafebbc1bf6eb1065c999459f2cf775c295d6` | 153 |

(`.../` is `app/src/main/java/com/zegoggles/smssync/`.)

### 2.2 Coverage arithmetic (re-derived at I0; rows D8, D9, D18, D35, D36)

Let a package have `c` covered of `t` lines at I0. The gate passes when `c/t ≥ f`. "Spare lines" =
`c − ⌈f·t⌉`, the number of covered lines that can be lost before the gate fails.

| Package (floor) | I0 (expected, L9) | After I2 | After I3 | After I5 | Spare after I5 |
|-----------------|-------------------|----------|----------|----------|----------------|
| `receiver` (0.50) | 29/58 = 50.0 %, spare 0 | 29 + 10 = **39/58 = 67.2 %**, spare 10 (`:68` stays missed: the test overrides it) | unchanged | unchanged: no S1 site is in `receiver` | 10 |
| `compat` (0.20) | 16/73 = 21.9 %, spare 1 | unchanged | unchanged (`:47-51` were already run by the LOLLIPOP tests and by `MainActivityRestoreTest` at Q; `:50` ci 3 in the last report, L21) | `:50` (covered) and `:53` (missed) go: 15/71 = 21.1 % without C-1; C-1 covers `disable` (`:105`, `:106`, `:109`; `enable` is already covered, L21): **18/71 = 25.4 %** | **3** with C-1 (⌈0.20·71⌉ = 15); 0 without |
| `scheduler` (0.60) | 140/188 = 74.5 % | unchanged | `:227` and `:229` lose coverage (only the SDK 23 test ran them): 138/188 = 73.4 % | `:224` (covered), `:227`, `:229` go: **137/185 = 74.1 %** | 26 |
| `activity` (0.10) | 189/1011 | unchanged | unchanged | `SmsDefaultRoleHelper.kt:74` (covered) and `:101` (missed) go; `Dialogs.java:172` may or may not carry its own line entry: ≈188/1009 | ≈87 |
| `calendar` (0.70) | 37/50 | unchanged | unchanged | unchanged (an annotation has no line) | 2 |

Formulas used by the decision rows when I0 differs from L9: after I2, `receiver` = `(c0 + Δ)/t0`, where Δ is the
number of `BackupBroadcastReceiver` lines covered after I2 minus before (expected 10); after I5, `compat` =
`(c0' − 1 + Δc1)/(t0' − 2)`, where Δc1 is the number of `disable` lines covered after I5 that were missed before
(expected 3), and `scheduler` = `(c3 − 1)/(t3 − 3)`, with `c0'`, `t0'`, `c3`, `t3` taken from the I3 record.
**Two corrections to the design** follow, both accepted by the orchestrator (ruling P-5): D8 item 4 says the
`compat` ratio rises after S-4; S-4 alone lowers it (21.9 % → 21.1 %, zero spare lines), because S-4 also removes
the covered `else if` line; C-1 (ruling P-5) restores a margin of 3 lines. The `scheduler` margin is wide; the
comment figure the story quotes (102/164) is stale (L9).

### 2.3 Why behaviour on every supported level is unchanged (site by site)

Let s be `Build.VERSION.SDK_INT`, with s ≥ 24 on every installable device after S1-I4.

| Site | Old behaviour as a function of s | New | Equal for s ≥ 24 because | Tests that run the surviving code | Stand-in where no test runs it |
|------|-----------------------------------|-----|--------------------------|-----------------------------------|-------------------------------|
| S-1 `Dialogs.About` `onPageFinished` | restore scroll iff `p > 0 ∧ url = ABOUT ∧ s ≥ 14` | iff `p > 0 ∧ url = ABOUT` | `s ≥ 14` is true; the removed operand is a static field read with no side effect, so short-circuit order changes nothing | none (no test opens the About dialog's WebView) | the boolean identity; the normalised bytecode diff of `Dialogs$About$1.class` shows only the removed compare (I5 step 7) |
| S-2 `@TargetApi` on `CalendarAccessorPost40` | annotation, `RetentionPolicy.CLASS`, read by lint only | none | no runtime effect | the `calendar` tests (37/50 lines) | normalised `javap -c -p` identical (I5 step 7) |
| S-3 `@TargetApi` on `SmsReceiver` | as S-2 | none | as S-2 | `SmsReceiverTest` | as S-2, all methods but `isSmsBackupDefaultSmsApp` |
| S-4 `isSmsBackupDefaultSmsApp` | `s ≥ 29 ? role : (s ≥ 19 ? pkg : false)` | `s ≥ 29 ? role : pkg` | `s ≥ 19` is true | the `Q` pair (role) and the `N` pair (package), shown by M-R1, M-R2 (I3) and the I5 line record (AC-4 (g)); `MainActivityRestoreTest` (Q, five tests) | — |
| S-5 `@TargetApi` on `MainActivity.startRestore` | as S-2 | none | as S-2 | `MainActivityRestoreTest` (Q) | as S-2 |
| S-6 `SmsDefaultRoleHelper.startRestore` | `s ≥ 19 ? body : restore()` | `body` | `s ≥ 19` is true; `body` is byte-identical apart from indentation (`git diff -w`, I5 step 5) | `MainActivityRestoreTest` at Q runs the `isSmsBackupDefaultSmsApp` and Q branches of `body` | the pre-Q branch of `body` (API 24 to 28) has no unit test: pre-existing gap, ruled (SC4), handed over |
| S-7 `scheduleContentTrigger` | degraded → null; else `s < 24` → log, null; else enqueue | degraded → null; else enqueue | `s < 24` is false | the retargeted API 24 test, the three INV-4 tests at API 29, the U-054 degraded test | — |

**A device below the new floor** (not one the owner uses, Record 5): the package manager refuses the update with
`INSTALL_FAILED_OLDER_SDK`, and the build already installed keeps running with its data. The reverse is allowed:
the previous APK (minSdk 21, same versionCode 1602) installs with `install -r` on the API 36 emulator, which is the
device rollback of D19.

### 2.4 Decisions and trade-offs (iteration 2; expert-depth-check criterion 3)

| Decision | Gained | Gives way | Not chosen | Why |
|----------|--------|-----------|------------|-----|
| One local commit per increment (ruled, AC-16) | Each increment can be bisected and reverted on its own; a gate failure points at one increment (Humble and Farley, ch. 10) | Seven commits on a shared branch that the orchestrator also commits to; a D51 fix commit breaks "one increment, one commit"; **a hand rollback must go newest-first**: reverting I4 while keeping I5 would leave a floor of 21 with the branch deleted, the one order CNTR-MODERNIZATION-004 v3's non-breaking claim excludes (contract-verification O-4) | One squashed commit | It gives one review diff and one revert, but no attribution of a failure to an increment, and it would mix the floor (I4) with the deletions (I5), which CR-015 Amendment 1 note 4 keeps apart |
| Delete the guards and annotations (bound by table S) | The AC-7 and AC-8 greps become exact; lint `ObsoleteSdkInt` entries and findings leave; readers no longer reason about unreachable branches | Nothing at runtime on a supported level (2.3); debug builds lose one integer compare per call; in release, R8 may already fold the checks (N11), so the release build may not change at all | (a) keep the guards; (b) turn `@TargetApi` into `@RequiresApi` or `@ChecksSdkIntAtLeast` | Both keep lint findings and the 21-versus-24 ambiguity alive in the code, and (a) contradicts REQ-015 AC-6 directly |
| Lint prune by regenerating on a `git archive` copy, removals only (D17, ruling P-3) | The regenerated file is the oracle; the committed baseline only shrinks (a monotone ratchet); the working tree is never touched by the regeneration | A second Gradle configuration and two lint runs in the copy; one `local.properties` copy; STALE-OTHER entries kept on purpose | (a) committing the regenerated file; (b) regenerating in the real tree and restoring the file afterwards | (a) admits every NOT-ADDED finding (the 39 current errors) into the baseline, breaking the ratchet; (b) risks leaving the real file regenerated if a session is lost, and runs Gradle on a tree that also carries this story's evidence |

### 2.5 What the floor changes in the toolchain (sources; iteration 2, expert-depth-check criterion 4)

Sources are cited as the vendor documents name them; I did not re-read them in this session, so each line that
the evidence must confirm is also a `needs_check` (N3, N4, N11) or is measured by a step.

1. **AGP manifest merger and packaging.** `packaging.jniLibs.useLegacyPackaging` defaults to `false` when minSdk is
   23 or higher (Android Gradle plugin DSL reference, `JniLibsPackaging`; Android Developers, "`<application>`
   element", `android:extractNativeLibs`). The merged manifest then carries `extractNativeLibs="false"` or no
   attribute (N3, D24). The "no effect" argument rests on the **measured** `so=0` of both APKs (EV-B), not on the
   absence of native code in the build: `app/build.gradle:29` sets `ndk.abiFilters 'armeabi-v7a','arm64-v8a'`.
   No other floor-derived manifest attribute is known: `uses-sdk` merging and vector-drawable handling were settled
   at 21; any other would show in the merged-manifest diff and stop the story (D24).
2. **AAPT2.** At link time AAPT2 collapses resource configurations whose version qualifier is at or below
   `--min-sdk-version` (Android Developers, "AAPT2", link options). The app's own `res/` has only
   `drawable-anydpi-v26`, above the floor, so any change comes from merged AndroidX and Material resources (N4,
   D28).
3. **D8 at min-api 24.** Default and static interface methods are kept native from API 24, so D8 no longer
   synthesises `$-CC` companion classes for them; lambdas are still desugared (native only from 26) (Android
   Developers, "Java 8+ language features and APIs"). No `coreLibraryDesugaring` is configured. **Multidex is
   unaffected**: ART has loaded multiple dex files natively since API 21 and the build sets no `multiDexEnabled`;
   only the number of `classes*.dex` files may change (D28).
4. **R8.** Recent R8 versions assume `SDK_INT ≥ min-api` and fold comparisons that this decides. This is a
   `needs_check` (N11): the developer confirms it from the R8 release notes of the R8 bundled with AGP 9.0.1, or
   records "not confirmed". Either way the release-build comparisons are **regression checks only**: they cannot
   show the equivalence of S-1, S-4, S-6 and S-7, because R8 may already have removed those checks from the
   release dex at minSdk 21 (14 and 19) or at I4 (`< N`). The debug bytecode comparison (I5 step 7) and the tests
   carry the equivalence (2.6).
5. **Lint.** `ObsoleteSdkInt` flags `SDK_INT` comparisons, `@TargetApi`/`@RequiresApi` values and `-vNN` resource
   folders at or below minSdk; `NewApi` can only lose findings when the floor rises (Android lint check reference).
   **Transient finding:** between I4 and I5, `ObsoleteSdkInt` gains a finding at `WorkManagerScheduler.kt:224`
   (`< N` becomes always false); I5 removes it. No lint run is planned at I4, so the I0-to-I6 comparison does not
   show it, which is correct (the end state is what AC-10 judges).
6. **Robolectric's SDK jar resolution.** Robolectric resolves its `android-all-instrumented` runtime jars at test
   time with its own Maven fetcher into the local Maven repository, fetching each jar's `.sha512` and its `.pom`
   and `.pom.sha512` alongside; the system properties `robolectric.offline` and `robolectric.dependency.dir` switch
   to pre-fetched jars (Robolectric documentation, "Configuring Robolectric", dependency resolution). Neither
   property is set here (L11), and setting one would be a build change AC-12 forbids. Gradle's dependency
   verification is not involved (design D5, ruling P-1).

### 2.6 What each before/after comparison can and cannot show

| Comparison | Can show | Cannot show |
|------------|----------|-------------|
| Merged and packaged manifests (I0→I4, I4→I5, I0→I7) | Exactly what the floor and the two removals change in what the platform reads; I4→I5 must be empty | Runtime behaviour of any code path |
| R8 kept names and `configuration.txt` | Drift in keep rules and in the names reflection depends on (the nine classes of U-060 C-2) | The equivalence of the removed branches (N11); synthetic classes that are not self-mapped |
| APK entry lists, `.so` and dex counts | Floor effects on packaging (AAPT2, D8); that no native library appears | What the dex files contain |
| Normalised `javap -c -p` of the debug classes (I4→I5) | That S-2, S-3, S-5 change no code, and that S-1, S-4, S-6, S-7 remove exactly the planned compare and branch (5.4) | Behaviour of code no test runs, beyond the identity of the remaining instructions |
| Tests and mutations | That the surviving branches behave as before at 24, 29 and Q, and which test runs which branch (M-R1, M-R2) | The pre-Q branch of `SmsDefaultRoleHelper.startRestore` (no test; SC4) and any level other than 24, 29 and Q |
| IC-3 on API 36 | Install over the configured app, platform `minSdk=24`, a clean start | Anything at API 24 (IC-4) |

## 3. Exact Changes

Every edit uses the Edit tool. Read the lines first and copy `old_string` from the Read output (the files are
CRLF in the working tree; the Edit tool preserves line endings). Paths: `S=app/src/main/java/com/zegoggles/smssync`,
`TS=app/src/test/java/com/zegoggles/smssync`. If an `old_string` is not found exactly once, row D4 applies.

### 3.1 S1-I1 — `build.gradle`

**E1** (`:30`). `old_string`: `        reports.junitXml.required = false` → `new_string`: `        reports.junitXml.required = true`.
Line 29 (`reports.html.required = false`) is not touched.

### 3.2 S1-I2 — new file `$TS/receiver/BackupBroadcastReceiverTest.java` (Write tool, whole file)

```java
package com.zegoggles.smssync.receiver;

import android.content.Context;
import android.content.Intent;
import androidx.preference.PreferenceManager;
import com.zegoggles.smssync.preferences.Preferences;
import com.zegoggles.smssync.scheduler.BackupScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * U-068 (DES-MODERNIZATION-014 D8, increment S1-I2): the public BACKUP broadcast's opt-in guard, both branches
 * (CNTR-MODERNIZATION-005, frozen). The production receiver is used unchanged; the scheduler is supplied
 * through its protected getScheduler(Context) seam. The preference is the real Preferences over the default
 * shared preferences, so the persisted key is part of what is pinned.
 */
@RunWith(RobolectricTestRunner.class)
public class BackupBroadcastReceiverTest {
    /** Persisted key of the third-party integration setting (Preferences.Keys.THIRD_PARTY_INTEGRATION). */
    private static final String THIRD_PARTY_INTEGRATION_KEY = "third_party_integration";

    @Mock BackupScheduler scheduler;
    private Context context;
    private BackupBroadcastReceiver receiver;

    @Before public void before() {
        openMocks(this);
        context = RuntimeEnvironment.getApplication();
        receiver = new BackupBroadcastReceiver() {
            @Override protected BackupScheduler getScheduler(Context context) {
                return scheduler;
            }
        };
    }

    @Test public void onReceive_integrationOn_schedulesImmediateOnce() {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(THIRD_PARTY_INTEGRATION_KEY, true)
                .commit();
        assertThat(new Preferences(context).isAllow3rdPartyIntegration()).isTrue();

        receiver.onReceive(context, new Intent(BackupBroadcastReceiver.BACKUP_ACTION));

        verify(scheduler, times(1)).scheduleImmediate();
        verifyNoMoreInteractions(scheduler);
    }

    @Test public void onReceive_integrationOff_neverSchedules() {
        assertThat(new Preferences(context).isAllow3rdPartyIntegration()).isFalse();

        receiver.onReceive(context, new Intent(BackupBroadcastReceiver.BACKUP_ACTION));

        verifyNoInteractions(scheduler);
    }
}
```

Design notes. The preconditions (`isTrue()`, `isFalse()`) make each test fail loudly if the key or the default
ever drifts, instead of passing for the wrong reason. `verifyNoMoreInteractions` makes "exactly once" strict.
Robolectric creates a fresh application, and so fresh shared preferences, for each test, so the "off" test reads
the real default. The anonymous subclass compiles into the test output, which JaCoCo does not measure. The
working names of AC-3 are kept as the final names.

### 3.3 S1-I3 — `$TS/compat/SmsReceiverTest.java` (one edit, `:23-45`)

`old_string` (lines 23 to 45):
```java
    // U-005 / U-001: minSdk raised to 21; SDK 16 (JELLY_BEAN) and SDK 19 (KITKAT) are below
    // the new minSdk floor and are no longer reachable. Tests updated to use SDK 21 (LOLLIPOP)
    // which is the minimum supported API level after U-001.

    @Test @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void testOnReceivePreKitKat() {
        subject.onReceive(RuntimeEnvironment.application, new Intent());
    }

    @Test @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void testOnReceiveKitKat() {
        subject.onReceive(RuntimeEnvironment.application, new Intent());
    }

    @Test @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void testIsSmsBackupDefaultSmsAppPreKitKat() {
        assertThat(SmsReceiver.isSmsBackupDefaultSmsApp(RuntimeEnvironment.application)).isFalse();
    }

    @Test @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    public void testIsSmsBackupDefaultSmsAppKitKat() {
        assertThat(SmsReceiver.isSmsBackupDefaultSmsApp(RuntimeEnvironment.application)).isFalse();
    }
```
`new_string`:
```java
    // U-068: minSdk raised to 24. isSmsBackupDefaultSmsApp keeps two branches: the role check from
    // Q (API 29) and the default-SMS-package check below Q (API 24 to 28, BUG-009). The tests run
    // each branch: N (API 24, the floor) for the package check and Q for the role check.

    @Test @Config(sdk = Build.VERSION_CODES.N)
    public void testOnReceive_belowQ() {
        subject.onReceive(RuntimeEnvironment.application, new Intent());
    }

    @Test @Config(sdk = Build.VERSION_CODES.Q)
    public void testOnReceive_q() {
        subject.onReceive(RuntimeEnvironment.application, new Intent());
    }

    @Test @Config(sdk = Build.VERSION_CODES.N)
    public void testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage() {
        assertThat(SmsReceiver.isSmsBackupDefaultSmsApp(RuntimeEnvironment.application)).isFalse();
    }

    @Test @Config(sdk = Build.VERSION_CODES.Q)
    public void testIsSmsBackupDefaultSmsApp_q_usesRoleManager() {
        assertThat(SmsReceiver.isSmsBackupDefaultSmsApp(RuntimeEnvironment.application)).isFalse();
    }
```
Bodies and assertions are byte-identical (AC-4 (a), (d)). The new comment has no "LOLLIPOP", no `sdk = [2x` and no
"kitkat" in any case.

### 3.4 S1-I3 — `$TS/service/WorkManagerSchedulerTest.kt` (three edits)

**T1** (`:34`). `old_string`: ` * INV-4 (AC-4): Content-URI trigger on API 24+.` → `new_string`:
` * INV-4 (AC-4): Content-URI trigger on every supported level; the version boundary is asserted at API 24, the minSdk floor (CNTR-MODERNIZATION-004 v3).`

**T2** (`:296-298`). `old_string`, the three lines:
```kotlin
    // INV-4: Content-URI trigger (API 24+) and pre-24 fallback
    // AC-4: addContentUriTrigger with triggerForDescendants=true; call-log conditional
    // Tests run with sdk=29 (API 29 >= 24, so content-URI triggers are available)
```
`new_string`:
```kotlin
    // INV-4: Content-URI trigger, one mechanism on every supported level (CNTR-MODERNIZATION-004 v3, CR-015)
    // AC-4: addContentUriTrigger with triggerForDescendants=true; call-log conditional
    // Tests run at the class level sdk=29; the version boundary is asserted at API 24, the minSdk floor
```

**T3** (`:371-384`). `old_string`, from the line `    /**` above ` * INV-4(d)` through the closing `    }` of the test:
```kotlin
    /**
     * INV-4(d): scheduleContentTrigger() returns null on API < 24 (pre-24 fallback path).
     * Below API 24, content-URI triggers are unavailable; the broadcast fallback
     * (SmsBroadcastReceiver → scheduleIncoming()) covers pre-24 devices.
     * Source: DES-MODERNIZATION-005 §Incoming-SMS trigger; AC-4(b).
     */
    @Test
    @Config(sdk = [23])
    fun inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast() {
        val result = scheduler.scheduleContentTrigger()

        // Below API 24: scheduleContentTrigger returns null; caller uses broadcast fallback.
        assertThat(result).isNull()
    }
```
`new_string`:
```kotlin
    /**
     * INV-4(d), version boundary at the lowest supported level, API 24 (CNTR-MODERNIZATION-004 v3, INV-4;
     * CR-015; DES-MODERNIZATION-005 INV-4 validation row): scheduleContentTrigger() returns a job, unique
     * work CONTENT_TRIGGER is enqueued, and its constraints carry the SMS provider URI trigger with
     * isTriggeredForDescendants=true. Retargeted by U-068 from the former SDK 23 test, which asserted only
     * a null return.
     */
    @Test
    @Config(sdk = [24])
    fun inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger() {
        val result = scheduler.scheduleContentTrigger()

        assertThat(result).isNotNull()
        assertThat(result!!.tag).isEqualTo(WorkManagerScheduler.CONTENT_TRIGGER_UNIQUE_NAME)

        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(WorkManagerScheduler.CONTENT_TRIGGER_UNIQUE_NAME)
            .get()
        assertThat(workInfos).isNotEmpty()

        val smsUri = android.net.Uri.parse("content://sms")
        val smsTrigger = workInfos[0].constraints.contentUriTriggers.find { it.uri == smsUri }
        assertThat(smsTrigger).isNotNull()
        assertThat(smsTrigger!!.isTriggeredForDescendants).isTrue()
    }
```
The assertions are AC-4 (b)'s four (non-null, unique work, SMS URI, descendants) plus the returned tag, which ties
the result to the unique name (stricter, not looser). The method-level `@Config(sdk = [24])` overrides the class's
`[29]`. T1 and T2 describe the post-S1 position ("every supported level") while the I3 commit still has
minSdk 21; they stay in I3 with the test they describe, because CR-015 "Order relative to the code" item 5 accepts
"true by requirement, not yet by build" wording for this interval, and moving them to I5 would split one test edit
over two commits (contract-verification O-1, considered and not taken). Nothing at `:296` lists the INV-4 API 29 tests (`:308`, `:332`, `:356`) or the U-054 test
(`:540-551`, with its comment `:537-538`, BUG-023's); none of them is in an edited range.

### 3.5 S1-I4 — `app/build.gradle` (three edits) and `app/src/main/AndroidManifest.xml` (one edit)

**E4.1** (`:24`). `        minSdkVersion 21` → `        minSdkVersion 24`.

**E4.2** (`:153`). `    // 2.9.1 is compatible with compileSdk 35 + minSdk 21 (AGP/SDK uplift from U-003).` →
`    // 2.9.1 is compatible with compileSdk 35 + minSdk 24 (AGP/SDK uplift from U-003; minSdk 24 from U-068).`

**E4.3** (`:160`, the trailing comment only; the code before `//` is byte-identical).
`    testImplementation 'org.robolectric:robolectric:4.15.1' // U-060: ASM 9.8 reads Java 25 class files; 4.16+ drops SDK 21/22` →
`    testImplementation 'org.robolectric:robolectric:4.15.1' // U-060: ASM 9.8 reads Java 25 class files. U-068: no test is configured below the minSdk of 24.`

Because `:160` is a dependency line that carries its comment, AC-12's "no dependency line changes" is proven on the
code: `app/build.gradle` with every `//` comment stripped differs from `<I3>` only on line 24 (I4 step 4).

**E4.4** (manifest `:38-51`, fourteen lines). `old_string`: from the line `    <!-- Allows the app to request
authentication tokens. Removed in Android 6 -->` through the blank line after
`                     android:maxSdkVersion="18"/>`, plus the next line `    <!-- Allows the app to create network
sockets and use custom network protocols. The browser and` as an anchor. `new_string`: that anchor line only. The
blank line `:37` is then followed directly by the INTERNET comment. Removed: `:38-42` (two comments, the
`USE_CREDENTIALS` element, a blank line) and `:43-51` (the five-line comment, the STORAGE group comment, the
`WRITE_EXTERNAL_STORAGE` element, a blank line). `SmsBroadcastReceiver`'s entry (`:146-153`) and every other line
are untouched. No source file refers to either permission by constant (`TokenRefresher.java:97` names
`USE_CREDENTIALS` in a comment, left as history).

### 3.6 S1-I5 — the seven dead sites and the stale text (8 files under `app/src/main`)

**S-1, `$S/activity/Dialogs.java`.**
- E5.1 (`:25`): `old_string` the two lines `import android.os.Build;` and `import android.os.Bundle;` → `import android.os.Bundle;`.
- E5.2 (`:170-172`): `old_string`
  ```java
                      if (scrollPosition > 0 &&
                          ABOUT_HTML.equals(url) &&
                          Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
  ```
  → `                    if (scrollPosition > 0 && ABOUT_HTML.equals(url)) {` (the form of table S).

**S-2, `$S/calendar/CalendarAccessorPost40.java`.**
- E5.3 (`:3`): `import android.annotation.TargetApi;` + `import android.content.ContentResolver;` → `import android.content.ContentResolver;`.
- E5.4 (`:10`): `import android.os.Build;` + `import android.provider.CalendarContract;` → `import android.provider.CalendarContract;`.
- E5.5 (`:23`): `@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)` + `public class CalendarAccessorPost40 implements CalendarAccessor {` → the second line only. The class keeps its name.

**S-3 and S-4, `$S/compat/SmsReceiver.java`.**
- E5.6 (`:3`): `import android.annotation.TargetApi;` + `import android.app.role.RoleManager;` → `import android.app.role.RoleManager;`.
- E5.7 (`:31`): `@TargetApi(Build.VERSION_CODES.KITKAT)` + `public class SmsReceiver extends BroadcastReceiver {` → the second line only.
- E5.8 (`:50-54`): `old_string`
  ```java
          }  else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
              return context.getPackageName().equals(Sms.getDefaultSmsPackage(context));
          } else {
              return false;
          }
  ```
  (note the two spaces after the first `}`) → 
  ```java
          } else {
              return context.getPackageName().equals(Sms.getDefaultSmsPackage(context));
          }
  ```
  Resulting method (`import android.os.Build` stays for the Q check):
  ```java
      public static boolean isSmsBackupDefaultSmsApp(@NonNull Context context) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              RoleManager roleManager = (RoleManager) context.getSystemService(Context.ROLE_SERVICE);
              return (roleManager != null && roleManager.isRoleHeld(ROLE_SMS));
          } else {
              return context.getPackageName().equals(Sms.getDefaultSmsPackage(context));
          }
      }
  ```

**S-5, `$S/activity/MainActivity.java`.**
- E5.9 (`:19`): `import android.annotation.TargetApi;` + `import android.content.Intent;` → `import android.content.Intent;`.
- E5.10 (`:425`): `    @TargetApi(Build.VERSION_CODES.KITKAT)` + `    private void startRestore() {` → the second line only. `import android.os.Build` stays (`:242`, `:517`).

**S-6, `$S/activity/SmsDefaultRoleHelper.kt`.** E5.11 (`:74-102`): `old_string` is lines 74 to 102 exactly
(the outer `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {` through the `        }` that closes the
pre-KitKat `else`). `new_string` is lines 75 to 98 with four leading spaces removed from each line, nothing else
changed:
```kotlin
        if (isSmsBackupDefaultSmsApp(activity)) {
            delegate.startViewModelRestore()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // BUG-009 / U-041: On Q+, always proceed to requestDefaultSmsPackageChange().
            if (preferences.hasSeenSmsDefaultPackageChangeDialog()) {
                delegate.requestDefaultSmsPackageChange()
            } else {
                delegate.showSmsDefaultPackageChangeDialog()
            }
        } else {
            @Suppress("DEPRECATION")
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(activity)
            Log.d(TAG, "default SMS package: $defaultSmsPackage")
            if (!TextUtils.isEmpty(defaultSmsPackage)) {
                preferences.setSmsDefaultPackage(defaultSmsPackage)
                if (preferences.hasSeenSmsDefaultPackageChangeDialog()) {
                    delegate.requestDefaultSmsPackageChange()
                } else {
                    delegate.showSmsDefaultPackageChangeDialog()
                }
            } else {
                Toast.makeText(activity, R.string.error_no_sms_default_package, Toast.LENGTH_LONG).show()
            }
        }
```
The pre-KitKat comment (`:100`) leaves with the `else`. `import android.os.Build` stays (`:77`, `:111`, `:135`).
Proof that the body is kept as it is: `git diff -w -U0` of the file has exactly five `-` lines and no `+` line
(I5 step 5).

**S-7 and the text, `$S/scheduler/WorkManagerScheduler.kt`.**
- E5.12 (`:20`): `import android.net.Uri` + `import android.os.Build` + `import android.util.Log` → `import android.net.Uri` + `import android.util.Log`.
- E5.13 (KDoc `:58-60`; `:61-63` unchanged, L18): `old_string`
  ```kotlin
   * - **INV-4 content-URI trigger**: API 24+ uses [Constraints.Builder.addContentUriTrigger]
   *   with [triggerForDescendants = true]; below API 24 the broadcast path calls
   *   [scheduleIncoming] instead. The two-stage debounce is preserved: the triggered
  ```
  → 
  ```kotlin
   * - **INV-4 content-URI trigger**: [Constraints.Builder.addContentUriTrigger] with
   *   `triggerForDescendants = true`, on every supported level (minSdk 24; no version branch,
   *   CNTR-MODERNIZATION-004 v3). The two-stage debounce is preserved: the triggered
  ```
- E5.14 (KDoc `:208-210`; `:212-213` unchanged, L18): `old_string`
  ```kotlin
       * API 24+: uses [Constraints.Builder.addContentUriTrigger] with triggerForDescendants=true.
       * Below API 24: the SMS-received broadcast path calls [scheduleIncoming] instead; this
       * method returns null to signal that the API-24+ trigger was not registered.
  ```
  → 
  ```kotlin
       * Uses [Constraints.Builder.addContentUriTrigger] with triggerForDescendants=true. Every
       * supported level (minSdk 24) has content-URI triggers, so there is no version branch
       * (CNTR-MODERNIZATION-004 v3, INV-4).
  ```
- E5.15 (`:222-232`): `old_string`
  ```kotlin
              return null
          }
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
              // Below API 24: content-URI triggers are unavailable.
              // The broadcast fallback (SmsBroadcastReceiver → scheduleIncoming()) covers this path.
              Log.d(TAG, "WorkManagerScheduler.scheduleContentTrigger: API < 24, " +
                      "using broadcast fallback (SmsBroadcastReceiver)")
              return null
          }

          val constraintsBuilder = Constraints.Builder()
  ```
  → 
  ```kotlin
              return null
          }

          val constraintsBuilder = Constraints.Builder()
  ```
  The return type stays `ScheduledJob?`: the encryption-degraded path (`:220-223`) still returns null (U-054).
- E5.16 (`:256`): `        Log.d(TAG, "WorkManagerScheduler.scheduleContentTrigger: enqueued CONTENT_TRIGGER (API 24+)")` →
  `        Log.d(TAG, "WorkManagerScheduler.scheduleContentTrigger: enqueued CONTENT_TRIGGER")`. No test asserts this
  text (`git grep "enqueued CONTENT_TRIGGER" -- app/src/test`: no line).

**Port javadoc, `$S/scheduler/BackupScheduler.java`.** E5.17 (`:74-77`): `old_string`
```java
     * API-level branching (content-URI triggers are only available on API 24+) lives
     * inside the adapter, not in this signature.
     *
     * @return the enqueued job, or {@code null} if the adapter does not support it
```
→
```java
     * Every supported level (minSdk 24) has content-URI triggers, so this call carries no
     * version condition (CNTR-MODERNIZATION-004 v3, INV-4).
     *
     * @return the enqueued job, or {@code null} if the adapter does not schedule it (today:
     *         when encryption is degraded, U-054)
```
`@Nullable` (`:79`) stays (IC-1).

**Comment, `$S/App.java`.** E5.18 (`:379-380`): `old_string`
```java
            // U-017: isUseOldScheduler() guard removed; WorkManagerScheduler handles
            // API-level branching internally (content-URI triggers on API 24+).
```
→
```java
            // U-017: isUseOldScheduler() guard removed. WorkManagerScheduler enqueues the
            // content-URI trigger with no version branch (minSdk 24, CNTR-MODERNIZATION-004 v3).
```

None of the new texts says a level below 24 is supported, calls the broadcast path a fallback or INV-4 evidence,
names `SmsBroadcastReceiver`, or states "API 24+" as a condition; each states only what CNTR-MODERNIZATION-004
v3 states (INV-4 row and note, `:243-245`).

**After S1-I5, the AC-7 grep prints 14 lines**: the 2 remaining `import` lines (`App.java` `RequiresApi`,
`ThemeActivity.java` `TargetApi`) and these 12 code lines, compared by file and text (line numbers move):
`App.java` `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {` and `@RequiresApi(api = Build.VERSION_CODES.O)`;
`MainActivity.java` the `readyToRestore` line (Q) and the TIRAMISU line; `SmsDefaultRoleHelper.kt` three Q lines;
`ThemeActivity.java` `if (VERSION.SDK_INT >= 26) {` and `@TargetApi(26)`; `SmsReceiver.java` one Q line;
`BackupWorker.kt` one Q line; `RestoreWorker.kt` one Q line. No comparison against level 24 itself remains.

**After S1-I5, the AC-8 grep prints exactly one line**: `app/src/main/java/com/zegoggles/smssync/compat/package-info.java:2`.

### 3.7 S1-I6 — `app/lint-baseline.xml` (removals only)

Each REMOVE entry of the I6 diff tool (expected 11) is deleted with one Edit whose `old_string` is the entry's ten
lines (`    <issue` to `    </issue>`) plus the blank line after it; the `file=` and `line=` lines make it unique.
`new_string` is empty (if the tool refuses an empty `new_string`, add the next `    <issue` line to both). At
`0d5755c4` that is lines 169-179 and 213-322: 121 lines, so `git diff --numstat` reads `0	121	app/lint-baseline.xml`.
`app/lint.xml` is not touched.

### 3.8 S1-I7 — documents

- E7.1 `CHANGES:3`: `  * Android minSdk now 14 (4.0, Ice Cream Sandwich)` →
  `  * Minimum Android version is now 7.0 (API 24); Android 5.0, 5.1 and 6.0 (API 21 to 23) are no longer supported`.
  The entry stays under the top section (`== 1.6.0 (xxx) dd-mm-2019`); no store, release or rollout word.
- E7.2 `DEVELOPMENT.md:58`: `- **minSdkVersion**: 21` → `- **minSdkVersion**: 24`. Nothing else in the file.
- E7.3 `sdlc/artifacts/engagement/code-location.md:28`: `| minSdk | 21 (Android 5.0) |` → `| minSdk | 24 (Android 7.0) |`.
- E7.4 `.github/ISSUE_TEMPLATE/500_bug_report.yml:54-56`: `old_string` the four lines `        - 7.0 (Nougat 2016)`,
  `        - 6.0 (Marshmallow 2015)`, `        - 5.1 (Lollipop 2015)`, `        - 5.0 (Lollipop 2014)` →
  `        - 7.0 (Nougat 2016)`. The 4.x entries (`:57-61`) stay (ruled follow-up).

## 4. Implementation Steps

### 4.0 Environment and conventions (every step)

```bash
# From the repository root. Never set MSYS_NO_PATHCONV for Gradle.
unset MSYS_NO_PATHCONV
W=sdlc/artifacts/build/sprints/sprint-024/U-068; E=$W/evidence; M=$E/mutations; TL=$E/tools
T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools     # read-only: sanitize.py, junit_totals.py, junit_cases.py, jacoco_pkgs.py, junit-xml.init.gradle
INIT=$T/junit-xml.init.gradle
R=app/build/test-results/testDebugUnitTest
JX=app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
S=app/src/main/java/com/zegoggles/smssync; TS=app/src/test/java/com/zegoggles/smssync
PLAN_HEAD=c12a2788b0d1cdd5267529eda30cc7895be60761                        # iteration-2 planning HEAD; table 2.1 blobs equal here and at 0d5755c4
SDK="$(python -I -X utf8 $TL/sdk_dir.py)"; BT="$SDK/build-tools/36.0.0"; AAPT2="$BT/aapt2"
JDKBIN="$(ls -d "/c/Program Files/Java/"jdk-25* | tail -1)/bin"
WORK=<a u068-work folder inside the developer's own session scratch folder>   # raw logs, pristine copies, the JaCoCo XML copies, commit messages
SCR="$(cd ../.. && pwd)/u068-lint"                                        # I6 only: a new folder inside the repository's grandparent folder, outside the repository; short path (Windows path length)
START=<the commit recorded in I0 step 1>
```
Shell state does not persist; each call sets what it needs. `SDK` can be set only after I0 step 2 has written
`sdk_dir.py`. `WORK`, `SDK`, `JDKBIN` and `SCR` are never written
into a repository file (evidence names them "the work folder", "the SDK", "the JDK", "the scratch copy").

**Helper tools** (I0 step 2, Write tool, into `$TL/`; QA re-runs them):

`sdk_dir.py`
```python
"""Print the Android SDK folder from local.properties (Java properties escaping), with forward slashes.
usage (from the repository root): python -I -X utf8 sdk_dir.py"""
import re
for line in open('local.properties', encoding='utf-8'):
    line = line.rstrip('\r\n')
    if line.startswith('sdk.dir='):
        print(re.sub(r'\\(.)', r'\1', line[len('sdk.dir='):]).replace('\\', '/'))
        break
else:
    raise SystemExit('sdk.dir not found in local.properties')
```

`jacoco_inventory.py` (U-060 plan Step 0.6, unchanged)
```python
"""usage: python -I -X utf8 jacoco_inventory.py <jacocoTestReport.xml> <out.json>"""
import sys, json, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot(); out = {}
for pkg in root.iter('package'):
    line = next((c for c in pkg.findall('counter') if c.get('type') == 'LINE'), None)
    out[pkg.get('name').replace('/', '.')] = {
        'classes': len(pkg.findall('class')),
        'line_covered': int(line.get('covered')) if line is not None else 0,
        'line_missed': int(line.get('missed')) if line is not None else 0}
json.dump(out, open(sys.argv[2], 'w'), indent=1, sort_keys=True)
```

`jacoco_lines.py`
```python
"""Per-line JaCoCo coverage of one source file, beside the source text.
usage: python -I -X utf8 jacoco_lines.py <jacocoTestReport.xml> <package/path> <SourceFile> <source path>
Prints 'nr mi=<missed instr> ci=<covered instr> mb=.. cb=.. | text' per line JaCoCo lists, then a TOTAL line."""
import sys
import xml.etree.ElementTree as ET
xml, pkg, name, src = sys.argv[1:5]
text = open(src, encoding='utf-8').read().splitlines()
root = ET.parse(xml).getroot()
p = next((p for p in root.iter('package') if p.get('name') == pkg), None)
sf = None if p is None else next((s for s in p.findall('sourcefile') if s.get('name') == name), None)
if sf is None:
    raise SystemExit(f'not found: {pkg}/{name}')
cov = mis = 0
for ln in sf.findall('line'):
    nr, ci = int(ln.get('nr')), int(ln.get('ci'))
    cov += ci > 0; mis += ci == 0
    src_text = text[nr - 1].strip() if nr <= len(text) else '?'
    print(f"{nr} mi={ln.get('mi')} ci={ci} mb={ln.get('mb')} cb={ln.get('cb')} | {src_text}")
print(f'TOTAL {pkg}/{name} covered={cov} missed={mis}')
```

`lint_ids.py`
```python
"""Count lint issues by rule id and severity, from a lint XML report or lint's text report.
usage: python -I -X utf8 lint_ids.py <report.xml | report.txt>
LintBaseline* messages (the 'listed in the baseline file but not found' count) are printed in full."""
import collections, re, sys
import xml.etree.ElementTree as ET
path = sys.argv[1]
c, notes = collections.Counter(), []
if path.endswith('.xml'):
    for i in ET.parse(path).getroot().iter('issue'):
        c[(i.get('id'), i.get('severity'))] += 1
        if (i.get('id') or '').startswith('LintBaseline'):
            notes.append(f"{i.get('id')}: {i.get('message')}")
else:
    pat = re.compile(r': (Error|Warning|Information|Fatal): (.*) \[([A-Za-z0-9_]+)\]\s*$')
    for line in open(path, encoding='utf-8', errors='replace'):
        m = pat.search(line)
        if m:
            c[(m.group(3), m.group(1))] += 1
            if m.group(3).startswith('LintBaseline'):
                notes.append(f'{m.group(3)}: {m.group(2)}')
for (rid, sev), n in sorted(c.items(), key=lambda kv: (-kv[1], kv[0])):
    print(f'{n:5d} {rid} {sev}')
for n in notes:
    print('NOTE', n)
print(f'TOTAL issues={sum(c.values())} ids={len({k[0] for k in c})}')
sys.exit(0 if c else 1)
```

`baseline_diff.py`
```python
"""Compare a lint baseline with a regenerated one (U-068 S1-I6, design D17).
usage: python -I -X utf8 baseline_diff.py <real baseline> <regenerated baseline> <module dir>
Entries match on (id, location file, message); line, column and errorLine are ignored. Each real entry is:
  KEEP         its key is in the regenerated file
  REMOVE       its key is not, and its id is ObsoleteSdkInt or its file no longer exists in the module
  STALE-OTHER  its key is not, for any other reason (kept; explained entry by entry)
Keys only in the regenerated file are NOT-ADDED (never applied). For REMOVE entries the cited code line
(errorLine1) is looked up in the file: site-gone, file-gone or site-present."""
import collections, os, sys
import xml.etree.ElementTree as ET

def entries(path):
    out = []
    for issue in ET.parse(path).getroot().findall('issue'):
        loc = issue.find('location')
        out.append({'id': issue.get('id'), 'message': issue.get('message'),
                    'err': (issue.get('errorLine1') or '').strip(),
                    'file': loc.get('file') if loc is not None else '',
                    'line': loc.get('line') if loc is not None else ''})
    return out

real, regen, module = sys.argv[1:4]
R, G = entries(real), entries(regen)
key = lambda e: (e['id'], e['file'], e['message'])
gcount, rcount = collections.Counter(map(key, G)), collections.Counter(map(key, R))
used, classes = collections.Counter(), collections.Counter()
for e in R:
    k, path = key(e), os.path.join(module, e['file'])
    if used[k] < gcount[k]:
        used[k] += 1; cls, site = 'KEEP', ''
    elif e['id'] == 'ObsoleteSdkInt' or not os.path.exists(path):
        cls = 'REMOVE'
        if not os.path.exists(path):
            site = 'file-gone'
        else:
            site = 'site-present' if e['err'] and e['err'] in open(path, encoding='utf-8', errors='replace').read() else 'site-gone'
    else:
        cls, site = 'STALE-OTHER', ''
    classes[cls] += 1
    print(f"{cls} {e['id']} {e['file']}:{e['line']} {site} | {e['message']}")
for k, n in sorted(gcount.items()):
    if n > rcount.get(k, 0):
        classes['NOT-ADDED'] += n - rcount.get(k, 0)
        print(f"NOT-ADDED x{n - rcount.get(k, 0)} {k[0]} {k[1]} | {k[2]}")
print(f'SUMMARY real={len(R)} regenerated={len(G)} ' + ' '.join(f'{c}={classes[c]}' for c in ('KEEP', 'REMOVE', 'STALE-OTHER', 'NOT-ADDED')))
```

**Gradle calls.** Foreground, tool timeout 600000 ms, console plain, output to the work folder, exit appended:
- **G-I0** (I0 only; JUnit XML by the read-only init script):
  `rm -rf $R; ./gradlew -I "$INIT" --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease > $WORK/logs/I0-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I0-gates.log`
- **G3** (I1, I2, I3; **no init script from I1 on**, the build's own XML):
  `rm -rf $R; ./gradlew --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification > $WORK/logs/I<k>-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I<k>-gates.log`
- **G4** (I4, I5): G3 plus `:app:assembleRelease`. **G4c** (I7): G4 with `clean` first.
- **TR(name, fqcn...)** targeted: `rm -rf $R; ./gradlew --console=plain :app:testDebugUnitTest --tests "<fqcn>" [--tests ...] > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log`
- **L** lint: `./gradlew --console=plain :app:lintDebug > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log` (expected `exit=1`, pre-existing red, D17: recorded, not gated).

`jacocoTestReport` is added to every gate only to produce the XML; it is finalized by the same verification task.

**Filters for sanitize.py** (`python -I -X utf8 $T/sanitize.py <in> <out> --grep '<regex>'`):
- gate filter **G**: `'^(BUILD |FAILURE|> Task :app:(assemble|test|jacoco|lintVital)|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'`
- lint filter **LF**: `'^(BUILD |FAILURE|exit=)|Lint found|listed in the baseline file|First failure|Created baseline|Wrote .*report'`

**Evidence after a gate, procedure EV-T(k)**:
```bash
python -I -X utf8 $T/sanitize.py $WORK/logs/I<k>-gates.log $E/I<k>-gates.txt --grep '<G>'
python -I -X utf8 $T/junit_totals.py $R > $E/I<k>-junit-totals.txt
python -I -X utf8 $T/junit_cases.py $R SmsReceiverTest WorkManagerSchedulerTest BackupBroadcastReceiverTest MainActivityRestoreTest SmsBroadcastReceiverTest > $E/I<k>-cases.txt
cp $JX $WORK/I<k>-jacoco.xml
python -I -X utf8 $T/jacoco_pkgs.py $JX > $E/I<k>-jacoco-packages.txt
python -I -X utf8 $TL/jacoco_inventory.py $JX $E/I<k>-jacoco-inventory.json
for f in receiver:BackupBroadcastReceiver.java compat:SmsReceiver.java scheduler:WorkManagerScheduler.kt activity:SmsDefaultRoleHelper.kt activity:Dialogs.java; do
  p=${f%%:*}; n=${f#*:}; python -I -X utf8 $TL/jacoco_lines.py $JX com/zegoggles/smssync/$p $n $S/$p/$n; done > $E/I<k>-jacoco-lines.txt
```

**Evidence of the build outputs, procedure EV-B(k)** (I0, I4, I5, I7; after a G-I0, G4 or G4c that exited 0):
```bash
MD=app/build/intermediates/merged_manifests
cp $MD/debug/processDebugManifest/AndroidManifest.xml $E/I<k>-merged-debug.xml
cp $MD/release/processReleaseManifest/AndroidManifest.xml $E/I<k>-merged-release.xml
"$AAPT2" dump xmltree --file AndroidManifest.xml app/build/outputs/apk/debug/app-debug.apk > $E/I<k>-manifest-debug.txt
"$AAPT2" dump xmltree --file AndroidManifest.xml app/build/outputs/apk/release/app-release-unsigned.apk > $E/I<k>-manifest-release.txt
MAP=app/build/outputs/mapping/release
grep -oE '^([A-Za-z0-9_.$]+) -> \1:' $MAP/mapping.txt | sed 's/ -> .*//' | grep -E '^(com\.zegoggles\.smssync|com\.fsck\.k9|com\.google\.crypto\.tink|androidx\.work)\.' | LC_ALL=C sort > $E/I<k>-kept-names.txt
python -I -X utf8 $T/sanitize.py $MAP/configuration.txt $E/I<k>-configuration.txt
for a in debug/app-debug release/app-release-unsigned; do unzip -Z1 app/build/outputs/apk/$a.apk | LC_ALL=C sort > $E/I<k>-apk-entries-${a%%/*}.txt; done
{ for a in debug/app-debug release/app-release-unsigned; do f=app/build/outputs/apk/$a.apk
    echo "$a entries=$(unzip -Z1 $f | wc -l) so=$(unzip -Z1 $f | grep -c '\.so$') dex=$(unzip -Z1 $f | grep -c '^classes[0-9]*\.dex$') size=$(stat -c %s $f) sha256=$(sha256sum $f | cut -d' ' -f1)"; done
  echo "mapping-lines=$(wc -l < $MAP/mapping.txt) kept-names=$(wc -l < $E/I<k>-kept-names.txt)"
  for c in App activity.MainActivity receiver.SmsBroadcastReceiver receiver.BootReceiver receiver.PackageReplacedReceiver receiver.BackupBroadcastReceiver service.BackupWorker service.RestoreWorker worker.BackupTriggerWorker; do
    echo "self-mapped com.zegoggles.smssync.$c=$(grep -c "^com\.zegoggles\.smssync\.${c//./\\.} -> com\.zegoggles\.smssync\.${c//./\\.}:" $MAP/mapping.txt)"; done
} > $E/I<k>-apk.txt
{ for c in "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/zegoggles/smssync/App.class" \
           "app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes/com/zegoggles/smssync/service/BackupWorker.class" \
           "k9mail-vendored/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/fsck/k9/mail/Address.class"; do
    echo "$c $("$JDKBIN/javap.exe" -v "$c" | grep 'major version')"; done } > $E/I<k>-javap.txt
```
(The nine self-mapped names are U-060 C-2's reflection-sensitive classes; expected `1` for each.) Expected in `I<k>-apk.txt`: `so=0`
for both APKs; `I<k>-javap.txt`: `major version: 61` for the two app classes and `52` for the vendored one.

**Normaliser for packaged manifests** (U-061 plan, verified there): `norm() { sed -E 's/ ?\(line=[0-9]+\)//g; s/@0x7f[0-9a-f]{6}/@0x7fRESID/g' "$1"; }`.

**Timestamps** come from `date -u +%Y-%m-%dT%H:%M:%SZ`, run at that moment.

**Commit procedure CM(k)** (the one ruled exception to "commit nothing"):
1. `git diff --cached --quiet; echo "index-clean-exit=$?"` must print 0 (nobody else has staged anything). If not, stop (row D51).
2. `ls -- <the explicit file list of the increment, section 4.9> > /dev/null; echo "files-exist-exit=$?"` must print 0 (plan-review B-4: a pathspec that matches nothing aborts `git add`); if not, stop and find which step did not write its file. Then `git add -- <the same list>` (every path written out; never `-A`, `.`, `-u` or a bare directory).
3. `git diff --cached --name-only | LC_ALL=C sort > $WORK/logs/I<k>-staged.txt` and `diff $WORK/logs/I<k>-staged.txt <(printf '%s\n' <the same list> | LC_ALL=C sort); echo "staged-diff-exit=$?"` → 0.
4. Write the message with the Write tool to `$WORK/msg-I<k>.txt`: the subject of 4.9, a blank line, two to four lines of body (what changed, the must-hold result, "Plan: sdlc/artifacts/build/sprints/sprint-024/U-068/plan.md, S1-I<k>"), a blank line, and the attribution trailer the developer's environment prescribes for commits. Then `git commit -F "$WORK/msg-I<k>.txt"`. Never `--amend`, `-a`, `--no-verify`, `push`, `rebase`, `reset`.
5. `git rev-parse HEAD` → recorded at the top of the next increment's log entry ("I<k> commit: <id>"); for I7, in the hand-off.

### I0 — Baseline at the start commit (no tracked file changes)

1. **Start state** (one call, `$WORK/logs/I0-start.log`, sanitised to `$E/I0-start.txt`):
   ```bash
   date -u +%Y-%m-%dT%H:%M:%SZ; git rev-parse HEAD; git rev-parse --abbrev-ref HEAD
   git merge-base --is-ancestor $PLAN_HEAD HEAD; echo "plan-head-ancestor-exit=$?"
   git log --oneline $PLAN_HEAD..HEAD
   git diff --name-only $PLAN_HEAD HEAD -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat CHANGES DEVELOPMENT.md .github sdlc/artifacts/engagement; echo "moved-files-end"
   git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat CHANGES DEVELOPMENT.md .github sdlc/artifacts/engagement; echo "porcelain-end"
   for p in <the 19 paths of table 2.1>; do echo "$(git rev-parse HEAD:$p) $(git hash-object $p) $p"; done
   grep -n '^status' sdlc/artifacts/build/change-records/CR-015-*.md sdlc/artifacts/build/change-records/CR-016-*.md
   grep -n '^status' sdlc/artifacts/build/sprints/sprint-022/U-066-*.md sdlc/artifacts/build/sprints/sprint-023/U-067-*.md
   git log --oneline -1 3af4ce32; git log --oneline -1 f0d0d17e; git merge-base --is-ancestor f0d0d17e HEAD; echo "cr-ancestor-exit=$?"
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   git grep -nE '^\s*@Ignore(\s|\(|$)' HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt'
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   "$JDKBIN/jps.exe" -l | grep -iE 'gradle' ; echo "jps-gradle-end"
   ```
   Expected: `plan-head-ancestor-exit=0`; the commits after `$PLAN_HEAD` (U-067's code commit `c12a2788` is
   `$PLAN_HEAD` itself) are document commits and, at most, U-067 fix or close commits; `moved-files-end` preceded
   by nothing, or only by U-067's files (`BackupWorker.kt`, `RestoreWorker.kt`, test files under
   `app/src/test/java/com/zegoggles/smssync/service/`); nothing before `porcelain-end` (U-067's QA mutations of the
   two worker files must be gone: SC-1); for each path of table 2.1 the two hashes equal each other and equal the
   planning blob; CR-015 and CR-016 `status: implemented`; U-066 and U-067 `status: done` (`<start>` is the commit
   that closes U-067's story); `cr-ancestor-exit=0`; the count `C0` as the command prints it (no expected number:
   nothing in this plan depends on a fixed count); the `@Ignore` lines (2 expected); `toolchainVersion=25`; no
   Gradle JVM listed by `jps`. Record `START=$(git rev-parse HEAD)` and `C0` at the top of the log. **Rows:** D1 to
   D5.
2. Create `$WORK/logs`, `$WORK/pristine`, `$E`, `$M`, `$TL`; write the five helper tools (4.0) with the Write tool;
   `./gradlew --version > $WORK/logs/I0-gradle-version.log 2>&1` sanitised to `$E/I0-gradle-version.txt`
   (expected "Daemon JVM: Compatible with Java 25 ... (from gradle/gradle-daemon-jvm.properties)").
3. **Gate G-I0.** Then EV-T(0) and EV-B(0). Expected: `exit=0`; `U063-DAEMON-JVM: java.version=25...`;
   `I0-junit-totals.txt` `tests=C0 failures=0 errors=0 skipped=2` (`skipped` is "the start" for AC-13 (c));
   per-package figures (2.2) recorded as "I0". **Rows:** D6 to D9, D12, D13.
4. **Greps at `<start>`** into `$E/I0-greps.txt`: the AC-7 grep (24 lines expected, classified 5 / 7 / 12 in the
   file as in L4; U-067 moved `BackupWorker.kt`'s Q line to `:534`); the AC-8 grep (29 lines); the AC-4 (e) grep
   (6 lines: `SmsReceiverTest.java:24` (the comment naming LOLLIPOP), `:27`, `:32`, `:37`, `:42`, and
   `WorkManagerSchedulerTest.kt:378`);
   the AC-11 (f) sweep (9 lines, as the story lists); `grep -n "maxSdkVersion" app/src/main/AndroidManifest.xml`
   (2 lines); `grep -n "minSdk 21\|SDK 21" app/build.gradle` (2 lines: `:153`, `:160`).
5. **Lint baseline count** into `$E/I0-lint-baseline-count.txt`: `grep -c '^ *<issue *$' app/lint-baseline.xml`
   (37) and `grep -c '<issue' app/lint-baseline.xml` (38), with the sentence "the 38 of the design counts the root
   `<issues` tag; the story's 37 governs (Scope Concern 2, ruled)". **Row:** D11.
6. **Lint (L, `I0-lint`).** Sanitise with LF into `$E/I0-lint.txt`. Locate the report:
   `ls app/build/reports/ | grep -i lint; find app/build/intermediates -name 'lint-results-debug*' -path '*lint*'`.
   Run `lint_ids.py` on `app/build/reports/lint-results-debug.xml` if it exists, else on the intermediate text
   report; sanitise into `$E/I0-lint-ids.txt`. Record: the exit (1 expected), "Lint found N errors (and F filtered
   by baseline)", the "not found" count `U0` (16 expected from L13), and the rule-ID table (closes design assumption
   A22). **Row:** D10.
7. Create `$W/implementation-log.md`: header; SC-1 to SC-6 each with its check and result (SC-4: the `jps`
   line and the orchestrator's confirmation); `<start>`, `C0`, the `@Ignore` lines; the I0 results by reference to
   the evidence files; the note that U-067's added tests are inside `C0` (they land before `<start>`).
   `git status --porcelain` afterwards lists only `?? $W/implementation-log.md` and `?? $E/` (AC-1, last sentence),
   plus the checkout's other untracked orchestrator files, which are left alone.

### I1 — JUnit XML on (AC-2)

1. Pre-check: `git status --porcelain -- app k9mail-vendored build.gradle` empty.
2. E1. `git diff -U0 -- build.gradle | grep -E '^[-+][^-+]'` prints exactly
   `-        reports.junitXml.required = false` and `+        reports.junitXml.required = true`.
3. Gate G3 (no init script). EV-T(1). Then `$E/I1-xml-check.txt`:
   `ls $R/TEST-*.xml | wc -l` (above 0); `diff <(sed 's/^files=[0-9]* //' $E/I0-junit-totals.txt) <(sed 's/^files=[0-9]* //' $E/I1-junit-totals.txt); echo "totals-diff-exit=$?"` (0: tests, failures, errors and skipped equal I0; the file count may differ only if forking differs, it is not compared).
   **Must hold (design S1-I1):** "Gates green; XML totals equal I0". **Rows:** D14, D15.
4. Log entry; CM(1).

What changes for evidence from here on: every later total comes from the build's own XML, written by
`:app:testDebugUnitTest` because of E1; the init script is never passed again (so its `U063-DAEMON-JVM` line is not
in later logs; the JDK evidence is `./gradlew --version`, I0 and I7).

### I2 — `BackupBroadcastReceiverTest` first (AC-3, IC-2)

1. Pre-check: `git diff --quiet HEAD -- app/src/main; echo "main-clean-exit=$?"` (0).
2. Write the file of 3.2. TR(`I2-first`, `com.zegoggles.smssync.receiver.BackupBroadcastReceiverTest`); `junit_cases.py $R BackupBroadcastReceiverTest > $E/I2-first-run.txt`. Expected: `exit=0`, two PASS lines with the two names, `cases=2`. This is the green half; the red half is M-B1 and M-B2 (step 4), because the code under test already exists. **Row:** D16.
3. Gate G3. EV-T(2). `$TS/receiver/BackupBroadcastReceiverTest.java` → `TEST-com.zegoggles.smssync.receiver.BackupBroadcastReceiverTest.xml` with `tests="2" failures="0" errors="0" skipped="0"` (IC-2; quote the root element's attributes into `$E/I2-checks.txt` with `grep -o '<testsuite [^>]*>' ... | sed 's/ hostname="[^"]*"//; s/ timestamp="[^"]*"//'`, dropping the host-name attribute). `I2-junit-totals.txt` `tests=C0+2`. `receiver` in `I2-jacoco-packages.txt` = 39/58 (2.2); `I2-jacoco-lines.txt` shows every `BackupBroadcastReceiver` line with `ci>0` except `:68`. Record "spare lines after I2 = covered − ⌈0.50·total⌉" (10 expected). **Must hold (S1-I2):** "`receiver` coverage above 50% with headroom; count +2". **Row:** D18.
4. Mutations M-B1, M-B2 by the protocol of 4.8. After them: `git diff --quiet HEAD -- app/src/main` (exit 0), `git hash-object $S/receiver/BackupBroadcastReceiver.java` = `b038d7fd...` (or its blob at `<start>`).
5. `$E/I2-checks.txt` also gets: `git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` (C0 + 2, working tree, said so) and `git status --porcelain -- app` (exactly `?? app/src/test/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiverTest.java`).
6. Log; CM(2).

### I3 — The five tests retargeted, minSdk still 21 (AC-4)

1. Pre-check as I2 step 1.
2. **SDK 24 jar (N2), under ruling P-1 ("acknowledged", story Implementation Notes, "Rulings on the plan's scope concerns").** Robolectric fetches its `android-all-instrumented` jar for Android 7.0 itself, from Maven Central, during the first SDK 24 run; Gradle's verification metadata covers no such jar today or after; design D5 accepts the download for S1; no build file or verification-metadata entry changes. Before any run, write to `$E/I3-sdk24-jar.txt` (the cache folder written as `<m2>/org/robolectric/android-all-instrumented`): `ls <m2>/org/robolectric/android-all-instrumented/ | grep '^7\.'` (expected: nothing), and the two build-file checks of plan-review B-3:
   ```bash
   git diff --quiet HEAD -- gradle/verification-metadata.xml app/build.gradle build.gradle gradle.properties app/src/test/resources/robolectric.properties; echo "build-files-uncommitted-exit=$?"   # 0
   git diff --quiet $START -- gradle/verification-metadata.xml app/build.gradle gradle.properties app/src/test/resources/robolectric.properties; echo "build-files-vs-start-exit=$?"   # 0 (build.gradle is left out: I1 changed and committed it)
   ```
3. Apply 3.3. TR(`I3-first`, `com.zegoggles.smssync.compat.SmsReceiverTest`) — one class, one fork, so the download happens once. Then `python -I -X utf8 $T/junit_cases.py $R SmsReceiverTest > $E/I3-first-run.txt`. Expected: `exit=0`, four PASS lines with the new names, `cases=4`. Then append to `$E/I3-sdk24-jar.txt`: the coordinates `org.robolectric:android-all-instrumented:<version>` with the folder name now present (expected version `7.0.0_r1-robolectric-r1-i7`); for each of the jar and the `.pom`: its name, its size (`stat -c %s`), its `sha512sum`, the content of its `.sha512` file beside it, and `match` or `MISMATCH`; the names and sizes of the `.jar.sha512` and `.pom.sha512` files; then the two build-file checks of step 2 again (both 0). The SHA-512 check shows the transfer is intact, not who published the file. **Rows:** D19, D20.
4. Apply 3.4 (T1 to T3). `git diff HEAD -- $TS/service/WorkManagerSchedulerTest.kt | grep '^@@'` (default context, plan-review B-5) → exactly three hunks: `@@ -31,7 +31,7 @@`, `@@ -293,9 +293,9 @@` and `@@ -369,18 +369,29 @@` (each may be followed by a function-name suffix after the second `@@`). TR(`I3-targeted`, `...compat.SmsReceiverTest`, `com.zegoggles.smssync.service.WorkManagerSchedulerTest`); `junit_cases.py $R SmsReceiverTest WorkManagerSchedulerTest > $E/I3-targeted.txt`. Expected: `exit=0`; `WorkManagerSchedulerTest#inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger PASS`; every other case of the two classes PASS; class case counts unchanged from `I2-cases.txt`. **Rows:** D20, D21.
5. Gate G3. EV-T(3). Expected: `tests=C0+2` (count unchanged by I3); `scheduler` covered falls by the lines only the SDK 23 test ran (2 expected, N9) and stays ≥ 0.60; `compat` as I2 (or as recorded). **Must hold (S1-I3):** "Green on 4.15.1; count unchanged". **Rows:** D9, D23.
6. Mutations M-R1, M-R2, M-W1, M-W2 (4.8). After them the production clean check as I2 step 4.
7. `$E/I3-checks.txt`: `grep -rn "LOLLIPOP\|sdk = \[\?2[0-3]" app/src/test; echo "ac4e-exit=$?"` (1: no match);
   `grep -rniE "pre-?24|below API 24|API ?< ?24|API 24\+|API-level branching|broadcast fallback|kitkat" app/src/test/java/com/zegoggles/smssync/service app/src/test/java/com/zegoggles/smssync/compat; echo "ac8-tests-exit=$?"` (1);
   `git grep -n "@Ignore\|Assume" -- $TS/compat/SmsReceiverTest.java $TS/service/WorkManagerSchedulerTest.kt; echo "ignore-exit=$?"` (1);
   `git diff --stat HEAD -- app/src/test` (two files); the working-tree count (C0 + 2);
   `git diff HEAD -- $TS/service/WorkManagerSchedulerTest.kt | grep -c 'would ultimately call scheduleIncoming'` (0: the `:537-538` comment, BUG-023's, is not in the diff).
8. Log (including the rename table 5.2 as "listed by name"); CM(3).

### I4 — The floor and the two permissions (AC-5, AC-6)

1. Pre-check: `git status --porcelain -- app` empty.
2. Apply E4.1 to E4.4.
3. Gate G4. EV-T(4). EV-B(4). Expected: `exit=0` (including `lintVitalRelease`); the five retargeted tests PASS again (AC-4 (f)). **Rows:** D30, D31.
4. `$E/I4-checks.txt`:
   ```bash
   grep -n "minSdkVersion\|compileSdkVersion\|targetSdkVersion\|versionCode" app/build.gradle          # 21: 35; 24: minSdkVersion 24; 25: 35; 26: 1602
   grep -n "minSdk 21\|SDK 21" app/build.gradle; echo "sdk21-exit=$?"                                 # 1
   diff <(git show HEAD:app/build.gradle | sed -E 's#[[:space:]]*//.*$##') <(sed -E 's#[[:space:]]*//.*$##' app/build.gradle)   # only line 24
   git diff -U0 HEAD -- app/build.gradle | grep -c '^[-+][^-+]'                                       # 6 (three - and three +)
   grep -n "maxSdkVersion" app/src/main/AndroidManifest.xml; echo "maxsdk-exit=$?"                     # 1
   git diff -U0 HEAD -- app/src/main/AndroidManifest.xml | grep '^@@'                                  # @@ -38,14 +37,0 @@
   git diff HEAD -- app/src/main/AndroidManifest.xml | grep -c '^+[^+]'                                # 0
   grep -n "minSdkVersion" k9mail-vendored/build.gradle                                                 # 54: minSdkVersion 21
   git grep -n "USE_CREDENTIALS\|WRITE_EXTERNAL_STORAGE" -- app/src k9mail-vendored/src                 # only TokenRefresher.java:97 (a comment; AC-6, plan-review O-2)
   ```
5. `$E/I4-manifest-diffs.txt`: `diff $E/I0-merged-debug.xml $E/I4-merged-debug.xml`, the same for release, and
   `diff <(norm $E/I0-manifest-debug.txt) <(norm $E/I4-manifest-debug.txt)` and release. Expected merged diff, both
   variants: `android:minSdkVersion="21"` → `"24"`; one deletion hunk holding exactly the two comment blocks of
   E4.4 (as the merger reprints them), the two `<uses-permission ... maxSdkVersion>` elements and one blank line;
   and, if N3 holds, `android:extractNativeLibs="true"` → `"false"` (row D24). Expected packaged diff:
   `minSdkVersion(0x0101020c)=21` → `=24`, the two `uses-permission` elements (name and `maxSdkVersion`) gone, and
   the same `extractNativeLibs` attribute if present. Then read the invariants in `I4-manifest-*.txt`:
   `SystemForegroundService` with `foregroundServiceType` `0x1` (dataSync); no `InitializationProvider`;
   `BackupBroadcastReceiver` `exported=true` with action `com.zegoggles.smssync.BACKUP`; the
   `SmsBroadcastReceiver` element identical to I0 (`git diff $START -- app/src/main/AndroidManifest.xml` shows no
   hunk at or after the old `:146`). **Must hold (S1-I4):** "Merged-manifest diff shows only
   `minSdkVersion="24"` and the two removals" (with D24 as the declared, explained exception). **Rows:** D24, D25.
6. `$E/I4-apk-diffs.txt`: `diff $E/I0-kept-names.txt $E/I4-kept-names.txt` (empty); `diff $E/I0-configuration.txt $E/I4-configuration.txt` (empty); `diff $E/I0-apk.txt $E/I4-apk.txt` (only size, sha256, the mapping line count and possibly `entries=` and `dex=`, D28; `so=0` both times); the entry lists:
   `diff $E/I0-apk-entries-debug.txt $E/I4-apk-entries-debug.txt` and release; then the qualifier-normalised
   comparison `diff <(sed -E 's#-v(1[0-9]|2[0-4])/#/#' $E/I0-apk-entries-debug.txt | LC_ALL=C sort -u) <(sed -E 's#-v(1[0-9]|2[0-4])/#/#' $E/I4-apk-entries-debug.txt | LC_ALL=C sort -u)`. **Rows:** D26 to D29.
7. **Bytecode "before" copies for I5** (no Gradle): copy into `$WORK/classes-I4/` the class files of 5.4 (Java from the javac folder, Kotlin from the built_in_kotlinc folder).
8. Log; CM(4).

### I5 — The seven dead sites and the stale text (AC-7, AC-8, AC-9)

1. Pre-check: `git status --porcelain -- app` empty.
1a. **Test C-1 first (ruling P-5; 6.1), while `app/src/main` still equals HEAD (the I4 commit).** `git diff --quiet HEAD -- app/src/main; echo "main-clean-exit=$?"` (0). Apply C1.a and C1.b. TR(`I5-c1-first`, `com.zegoggles.smssync.compat.SmsReceiverTest`), then `python -I -X utf8 $T/junit_cases.py $R SmsReceiverTest > $E/I5-c1-first-run.txt`. Expected: `exit=0`, five PASS lines (the four retargeted names and `testEnableThenDisable_togglesReceiverComponent`), `cases=5`; `grep -c "@Test" $TS/compat/SmsReceiverTest.java` is 5. Then run M-C1 by 4.8 (production still equals HEAD, so the protocol's precondition and recovery hold). Only then go to step 2. **Rows:** D35b, D17.
2. Apply E5.1 to E5.18 (3.6).
3. Gate G4. EV-T(5). EV-B(5). Expected: `exit=0`; `tests=C0+3 failures=0 errors=0 skipped=2` (C-1 included); the U-054 test, the three INV-4 API 29 tests, the API 24 test, the four `SmsReceiverTest` cases and the five `MainActivityRestoreTest` cases PASS (AC-7 (c), AC-4 (f)); coverage per 2.2 (`compat` 18/71 expected, with `ci>0` on `SmsReceiver.disable`'s three lines in `I5-jacoco-lines.txt`; `scheduler` ≥ 0.60; class inventory equal to I0 package by package: `python -I -c` comparison of the two JSON files' `classes` fields, written to `$E/I5-checks.txt`). **AC-4 (g)**: in `I5-jacoco-lines.txt` the `SmsReceiver.java` line `RoleManager roleManager = ...` and the line `return context.getPackageName().equals(Sms.getDefaultSmsPackage(context));` both have `ci>0`. The only tests configured at a level from 24 to 28 are the two `N` cases and the API 24 scheduler test, which never calls `SmsReceiver` (census of `sdk =` values in `app/src/test` at `0d5755c4`: `LOLLIPOP` 4, `Q` 5, `[23]` 1, `[29]` 9, `{29}` 1, all others at the default 29), so `ci>0` on the package line is the `N` tests' doing; M-R1 and M-R2 (I3) show which pair runs which branch. **Must hold (S1-I5):** "AC-6 grep passes; coverage floors hold (scheduler measured)". **Rows:** D32 to D38.
4. `$E/I5-greps.txt`: the AC-7 grep (14 lines, 3.6; compare by file and text); `grep -rnE ... app/src/main | grep -v ':import ' | wc -l` (12); the AC-8 grep (one line, 3.6); for each edited file, a search for the removed import's simple name: `grep -n "TargetApi" $S/calendar/CalendarAccessorPost40.java $S/compat/SmsReceiver.java $S/activity/MainActivity.java` (no line), `grep -nw "Build" $S/activity/Dialogs.java $S/calendar/CalendarAccessorPost40.java $S/scheduler/WorkManagerScheduler.kt` (no line; `Builder` does not match `-w Build`) (AC-7 (b)); the IC-1 caller list: `git grep -n "isSmsBackupDefaultSmsApp(\|SmsDefaultRoleHelper.startRestore(\|scheduleContentTrigger()" -- app/src` with each line accounted for in the log (callers `MainActivity.java` the two `isSmsBackupDefaultSmsApp` calls and `startRestore` delegation, `SmsDefaultRoleHelper.kt` two calls, `SmsReceiver.onReceive`, `App.java` `rescheduleJobs`, the test classes; all compile unchanged).
5. `$E/I5-checks.txt` also: `git diff --name-only HEAD -- app/src/main | LC_ALL=C sort` (the eight files of 3.6; with I4's manifest that makes the nine of AC-9); `git diff -w -U0 HEAD -- $S/activity/SmsDefaultRoleHelper.kt | grep -E '^[-+][^-+]'` → five `-` lines whose trimmed texts are, as a multiset, `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {`, `} else {`, `// Pre-KitKat: no SMS default permission required.`, `delegate.startViewModelRestore()`, `}`, and no `+` line; `git diff HEAD --stat -- $S/receiver/SmsBroadcastReceiver.java $S/receiver/BackupBroadcastReceiver.java $S/worker/BackupTriggerWorker.kt $S/service/BackupWorker.kt $S/service/RestoreWorker.kt k9mail-vendored app/src/test/resources/robolectric.properties` (empty); `git diff --stat HEAD -- app/src/test` (one file, `SmsReceiverTest.java`, C-1); the working-tree count `git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` (C0 + 3, said so).
6. `$E/I5-build-diffs.txt`: merged and packaged manifests, kept names, configuration and APK entry lists against I4 (expected: manifests identical; kept names identical; configuration identical; APK entry lists identical or differing only in `classes*.dex` count, row D28).
7. **Bytecode comparison** (`$E/I5-bytecode.txt`), for each class of 5.4: `diff <(jp $WORK/classes-I4/<C>) <(jp <current C>)` where `jp() { "$JDKBIN/javap.exe" -c -p "$1" | sed -E 's/#[0-9]+(,[0-9]+)?//g; s/^Compiled from.*//'; }` (constant-pool indices removed, because removing an annotation renumbers the pool). Expected per 5.4. This debug comparison, with the tests, is the equivalence evidence; the release comparisons of step 6 are regression checks only (2.5 item 4, 2.6). Also record N11 here: the R8 release-note line for the R8 bundled with AGP 9.0.1 that states the `SDK_INT ≥ min-api` assumption (quoted with its R8 version), or "not confirmed". **Row:** D37.
8. Log (with the site table 2.3 by reference); CM(5).

### I6 — Lint baseline pruned (AC-10)

1. Pre-check: `git status --porcelain -- app` empty; `test -e "$SCR"; echo "scr-exists-exit=$?"` → 1 (row D39).
2. **Scratch copy, no build outputs, outside the repository:** `mkdir "$SCR" && git -c core.autocrlf=false archive HEAD | tar -x -C "$SCR" && cp local.properties "$SCR/"`. `git archive` writes the committed tree of the I5 commit only (no `build/` folder, no untracked file, no `.git`); `-c core.autocrlf=false` keeps the index's LF endings, because with this checkout's `core.autocrlf=true` a plain `git archive` writes `gradlew` with CRLF and it cannot start (plan-review B-2). Line endings do not affect lint's matching or `baseline_diff.py`, which parses XML. Record in `$E/I6-scratch.txt`, **before the first Gradle run in the copy**: `python -I -c "import sys;print(open(sys.argv[1],'rb').read().count(b'\r\n'))" "$SCR/gradlew"` (expected `0`; row D39b); the archived commit id, `find "$SCR" -type f | wc -l` against `git ls-tree -r HEAD --name-only | wc -l` (+1 for `local.properties`), `find "$SCR" -maxdepth 2 -name build -type d | wc -l` (0), and `git -C . status --porcelain -- app` (empty: the working tree is untouched). The scratch folder is written as "the scratch copy" in evidence.
3. In the scratch copy, foreground, tool timeout 600000: `(cd "$SCR" && ./gradlew --console=plain :app:lintDebug) > $WORK/logs/I6-scratch-lint.log 2>&1; echo "exit=$?" >> ...` → LF → `$E/I6-scratch-lint.txt`: "not found" count `N5` (the I5 tree with the 37-entry baseline) and the error count; `lint_ids.py` on the scratch report → `$E/I6-scratch-lint-ids.txt`. Then `(cd "$SCR" && ./gradlew --console=plain :app:tasks --all) | grep -i "lintbaseline"` (expect `updateLintBaseline`) and `(cd "$SCR" && ./gradlew --console=plain :app:updateLintBaseline) > $WORK/logs/I6-regenerate.log 2>&1; echo "exit=$?" >> ...` → LF → `$E/I6-regenerate.txt`; `grep -c '^ *<issue *$' "$SCR/app/lint-baseline.xml"` and its header line. **Rows:** D40.
4. `python -I -X utf8 $TL/baseline_diff.py app/lint-baseline.xml "$SCR/app/lint-baseline.xml" app > $WORK/logs/I6-baseline-diff.log` → sanitise → `$E/I6-baseline-diff.txt`. Sanitise the regenerated file itself into `$E/I6-regenerated-baseline.xml` (if `sanitize.py` refuses it, keep it in the work folder and say so). Expected: `REMOVE=11`, each `site-gone` or `file-gone`, exactly the entries of L12; `KEEP` + `STALE-OTHER` = 26; each `STALE-OTHER` explained in the log entry by entry (expected candidates: the five `GradleDependency` entries if lint 9.0.1 no longer reports that check, U-060 recorded none; any entry whose message wording changed between lint 8.7.3 and 9.0.1); `NOT-ADDED` listed, never applied. **Rows:** D41, D42.
5. Apply 3.7 for exactly the REMOVE set. `$E/I6-checks.txt`: `git diff --numstat -- app/lint-baseline.xml` (`0	121`); `git diff -U0 -- app/lint-baseline.xml | grep -c '^+[^+]'` (0); `grep -c '^ *<issue *$' app/lint-baseline.xml` (26); `git diff --quiet -- app/lint.xml; echo "lint-xml-exit=$?"` (0); `git diff -- app/lint-baseline.xml | grep -E '^-.*(id=|file=|line=)'` (the 11 removed ids, files and lines, matching `I6-baseline-diff.txt`). **Row:** D43.
6. **Lint in the real tree** (L, `I6-lint`) → LF → `$E/I6-lint.txt`; `lint_ids.py` → `$E/I6-lint-ids.txt`. Expected: "not found" `N6 = N5 − 11`, or 0 (AC-10 (d)). `$E/I6-lint-ids-compare.txt`: the rule-ID tables of I0 and I6 side by side; every rule ID whose count changed is explained (expected: `ObsoleteSdkInt` errors fall, because S1 removed their sites; `UseRequiresApi` falls from the four `@TargetApi` uses to one, `ThemeActivity`; `UnusedAttribute` may fall if its one finding is an attribute of API 23 or 24); any rule ID that is new after the story is named (row D44). The pre-existing red result is recorded, not fixed and not gated (D17).
7. Remove the scratch copy: `rm -rf -- "$SCR"`; `test -e "$SCR"; echo "scr-removed-exit=$?"` (1). If the removal fails (a Gradle or Kotlin daemon may still hold files there), run `(cd "$SCR" && ./gradlew --stop)` once, retry the `rm`, and if it still fails record it and report (row D39c); never leave the copy silently. It is never committed.
8. **Must hold (S1-I6):** "Entry count before and after, with each removed entry; `app/lint.xml` untouched". Log (the 37-versus-38 note; every removed entry with id, file, line and message; every STALE-OTHER with its reason); CM(6).

### I7 — Documents, the CR-016 checks, final gates and hand-off (AC-11, AC-12 to AC-15)

1. Pre-check: `git status --porcelain -- app CHANGES DEVELOPMENT.md .github sdlc/artifacts/engagement` empty.
2. Apply E7.1 to E7.4. `$E/I7-docs.txt`:
   ```bash
   sed -n 1,5p CHANGES
   grep -n "minSdk" sdlc/artifacts/engagement/code-location.md                        # 28: | minSdk | 24 (Android 7.0) |
   grep -n "minSdkVersion" DEVELOPMENT.md                                              # 58:- **minSdkVersion**: 24
   git diff --numstat -- DEVELOPMENT.md                                                # 1	1	DEVELOPMENT.md
   git diff -U0 -- DEVELOPMENT.md | grep -E '^[-+]- '                                  # -- **minSdkVersion**: 21 and +- **minSdkVersion**: 24 (plan-review B-5)
   grep -n "Marshmallow\|Lollipop" .github/ISSUE_TEMPLATE/500_bug_report.yml; echo "template-exit=$?"   # 1
   # CR-016 D-5, checks 1 to 3 (AC-11 (d)):
   grep -n "CR-016" sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-001-restore-play-store-eligibility.md sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-006-dead-code-and-minsdk-cleanup.md
   grep -c "superseded by REQ-MODERNIZATION-015" sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-001-*.md sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-006-*.md
   git diff f0d0d17e^ f0d0d17e -- sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-001-*.md sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-006-*.md | grep -E '^[-+][^-+]' | cut -c1-120
   git diff f0d0d17e^ f0d0d17e -- sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-001-*.md sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-006-*.md | grep -cE '^[-+](- \[|status:)'   # 0
   # AC-11 (f) sweep:
   git grep -niE "minSdk(Version)?[^0-9]{0,6}21|API (level )?21" -- . ':!sdlc'
   ```
   Expected (L17): two `grep -n` lines per file (the `change_records` item and the note); 1 and 1; the diff lines are,
   per file, exactly four: `-change_records: []`, `+change_records:`, `+  - CR-016`, `+> Amended 2026-11-21 by
   CR-016...` (8 in all); the count is 0. The sweep lists **7** lines: `CHANGES:3` ("API 21 to 23", intended), `WindowInsetsUtil.java:48`,
   `:84`, `WindowInsetsUtilTest.java:31`, `:116`, `k9mail-vendored/build.gradle:54`,
   `DefaultTrustedSocketFactory.java:153`; each with its disposition in the log. **Rows:** D45, D46.
3. Gate G4c (clean). EV-T(7). EV-B(7). `./gradlew --version` → `$E/I7-gradle-version.txt`. **Rows:** D47, D48.
4. `$E/I7-build-diffs.txt`: every EV-B file of I7 against I0 (merged and packaged manifests, kept names,
   configuration, APK entries, `.so`, `javap` majors), each difference attributed to I4 or I5 by the I4 and I5
   records. AC-13 (d): `so=0` for both APKs; (e): entry differences only as D28 allows.
5. `$E/I7-final-checks.txt` (working tree; the I7 commit follows):
   ```bash
   git diff --stat $START -- $S/receiver/SmsBroadcastReceiver.java $S/receiver/BackupBroadcastReceiver.java $S/worker/BackupTriggerWorker.kt $S/service/BackupWorker.kt $S/service/RestoreWorker.kt k9mail-vendored app/src/test/resources/robolectric.properties   # empty (AC-9)
   git diff -U0 $START -- app/src/main/AndroidManifest.xml | grep '^@@'                                  # @@ -38,14 +37,0 @@ only
   git diff --name-only $START -- app/src/main | LC_ALL=C sort                                          # the nine files of AC-9 (no other story changes code during this run: check (iii) below)
   git diff --stat $START -- gradle/verification-metadata.xml gradle/wrapper gradle/gradle-daemon-jvm.properties settings.gradle gradle.properties gradlew gradlew.bat k9mail-vendored/build.gradle app/lint.xml app/proguard-rules.pro .github/workflows   # empty (AC-12)
   git diff -U0 $START -- build.gradle | grep -E '^[-+][^-+]'                                           # the E1 pair only
   git diff -U0 $START -- app/build.gradle | grep -E '^[-+][^-+]'                                       # the three E4 pairs only
   git diff -U0 $START -- app/build.gradle | grep -cE '^[-+].*(minimum|value|limit|counter|includes|excludes)'   # 0 (no floor changed)
   # Scoped to this story (plan-review B-6; U-067 plan I8 A1). The orchestrator may commit documents while the developer runs:
   git log --reverse --format='%h %s' $START..HEAD                                     # (i) every commit, recorded; others are not this story's
   { git log --format= --name-only --grep='^U-068 S1-' $START..HEAD; git diff --name-only HEAD; git ls-files --others --exclude-standard -- app CHANGES DEVELOPMENT.md .github build.gradle sdlc/artifacts/engagement $W; } | sed '/^$/d' | LC_ALL=C sort -u   # (ii) this story's own files: within the AC-12 allowed list
   git log --format='%h %s' --invert-grep --grep='^U-068 S1-' $START..HEAD -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat CHANGES DEVELOPMENT.md .github sdlc/artifacts/engagement; echo "foreign-code-commits-end"   # (iii) nothing before the marker (row D53)
   git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"   # C0 + 3 (working tree, AC-14 alternative form, said so)
   git grep -nE '^\s*@Ignore(\s|\(|$)' -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt'              # the same 2 lines as I0
   ```
6. **Log, then the hygiene scan** (plan-review O-4: the log is final before the scan, so the scan covers it).
   First write the I7 log entry: the counts (5.3), the "must hold" of S1-I7 ("AC-9 and AC-10 evidence": the three
   D-5 checks and the `CHANGES` entry, location `CHANGES`), the hand-off items (section 7) and the last line
   "Hygiene scan: step I7.6, result in `evidence/I7-scan.txt`". Write `$E/I7-scan.txt`'s name into the log now; the
   file itself is written by the scan below. Then the scan (as U-066 I11, workspace `$W`, excluding
   `compiled-pipeline.json`), one call:
   ```bash
   P='@[a-z0-9-]+\.[a-z]{2,}|Users[/\\]|App''Data'
   L=$WORK/logs
   git diff $START -- app/src build.gradle app/build.gradle app/lint-baseline.xml CHANGES DEVELOPMENT.md .github sdlc/artifacts/engagement > $L/I7-code.diff; echo "code-diff-exit=$?"
   {
     grep -rlEi --exclude=compiled-pipeline.json "$P" $W $L/I7-code.diff > $L/I7-pattern-files.log
     echo "pattern-scan exit=$? files=$(wc -l < $L/I7-pattern-files.log)"
     for label in user-name host-name; do
       case $label in user-name) v="${USERNAME:-$USER}" ;; host-name) v="${COMPUTERNAME:-$(hostname)}" ;; esac
       if [ ${#v} -lt 3 ]; then echo "$label-scan value-unavailable"; continue; fi
       grep -rliF --exclude=compiled-pipeline.json -e "$v" $W $L/I7-code.diff > $L/I7-$label-files.log
       echo "$label-scan exit=$? files=$(wc -l < $L/I7-$label-files.log)"
     done
   } > $L/I7-scan.log 2>&1
   cat $L/I7-pattern-files.log $L/I7-user-name-files.log $L/I7-host-name-files.log
   ```
   Expected: every scan `exit=1 files=0`. Sanitise into `$E/I7-scan.txt`. (The code diff contains no copyright
   header: no edited hunk touches a header line.) A hit: fix the file, re-run the scan; no file under `$W` is edited
   after a clean scan except by CM(7). **Row:** D49.
7. CM(7), nothing else. After the commit, into the **hand-off message** (not into any file):
   `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` (C0 + 3, at the
   last commit); the AC-16 order check `git log --reverse --format='%h %s' --grep='^U-068 S1-' $START..HEAD`
   (I1 to I7 in order, plus any fix commit), with every other commit in `$START..HEAD` listed by id and subject as
   not this story's (row D53); the APK path, size and SHA-256 from `I7-apk.txt`.

### I8 — Device checks (the orchestrator's; section 9)

### 4.8 Mutation protocol and table (write-ahead; restore proven by hash)

`$E/mutation-log.md` header: "Protocol: U-068 plan 4.8 (write-ahead entry, one Edit, one targeted run, restore by
copying the pristine file, hash and clean check, `restored` line). Repository-relative paths."; `<start>`; the
HEAD blob of each mutated file. Recovery note: "If a session is lost while an entry says `APPLYING` without a
`restored` line, nothing else runs until the file's `git hash-object` has been compared with `git rev-parse
HEAD:<file>` and, if it differs, the file has been restored from its pristine copy or with
`git restore --source=HEAD --worktree -- <file>` (safe here: in I2, in I3, and in I5 before E5.1, no production
file differs from HEAD)."

For each entry, one at a time: (1) `git diff --quiet HEAD -- app/src/main` exit 0 and the file's hash = HEAD blob;
`cp <file> $WORK/pristine/` once per file; (2) write-ahead `### <id> APPLYING <date -u>` with file, blob, edit,
class, expected; (3) one Edit; (4) TR(`<id>`, class) then `junit_cases.py $R <class> > $M/<id>-cases.txt` and
`grep -o 'message="[^"]*"' $R/TEST-<fqcn>.xml > $WORK/logs/<id>-msg.log; python -I -X utf8 $T/sanitize.py $WORK/logs/<id>-msg.log $M/<id>-message.txt`;
(5) `cp` the pristine copy back, whatever the result; (6) re-check (1) and append `run_exit`, the failing and
passing cases, `expected: matches | does not match`, `verdict`, `restored: <date -u> hash-ok main-clean`. No gate,
review or commit runs while an entry lacks `restored`.

Failure types: **A** = an `AssertionError` or subclass (Truth's, Mockito's `WantedButNotInvoked` and
`NoInteractionsWanted`); **X** = `java.lang.IllegalStateException` thrown by the mutant.

| Id | Inc. | File | Exact edit (`old_string` → `new_string`) | Class run | Expected: failing (all other cases of the class pass) |
|----|------|------|------------------------------------------|-----------|--------------------------------------------------------|
| M-B1 | I2 | `$S/receiver/BackupBroadcastReceiver.java` | `        if (new Preferences(context).isAllow3rdPartyIntegration()) {` → `        if (true \|\| new Preferences(context).isAllow3rdPartyIntegration()) {` (opt-in gate removed) | `...receiver.BackupBroadcastReceiverTest` | `onReceive_integrationOff_neverSchedules` — "No interactions wanted here" (A). Passing: `onReceive_integrationOn_schedulesImmediateOnce` |
| M-B2 | I2 | same | `            getScheduler(context).scheduleImmediate();` → `            if (false) getScheduler(context).scheduleImmediate();` (call removed) | same | `onReceive_integrationOn_schedulesImmediateOnce` — "Wanted but not invoked: scheduler.scheduleImmediate()" (A). Passing: the "off" test |
| M-R1 | I3 | `$S/compat/SmsReceiver.java` | `            RoleManager roleManager = (RoleManager) context.getSystemService(Context.ROLE_SERVICE);` → `            if (true) throw new IllegalStateException("U-068 M-R1"); RoleManager roleManager = (RoleManager) context.getSystemService(Context.ROLE_SERVICE);` | `...compat.SmsReceiverTest` | `testOnReceive_q`, `testIsSmsBackupDefaultSmsApp_q_usesRoleManager` — X "U-068 M-R1". Passing: the two `_belowQ` cases (they never reach the role branch) |
| M-R2 | I3 | same | `            return context.getPackageName().equals(Sms.getDefaultSmsPackage(context));` → `            if (true) throw new IllegalStateException("U-068 M-R2"); return context.getPackageName().equals(Sms.getDefaultSmsPackage(context));` | same | `testOnReceive_belowQ`, `testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage` — X "U-068 M-R2". Passing: the two `_q` cases |
| M-C1 | I5 (step 1a, before E5.1) | `$S/compat/SmsReceiver.java` | `            COMPONENT_ENABLED_STATE_DISABLED,` (12 spaces; inside `disable`, `:107` at `<start>`; unique) → `            COMPONENT_ENABLED_STATE_ENABLED,` | `...compat.SmsReceiverTest` | `testEnableThenDisable_togglesReceiverComponent` — Truth "expected: 2 ... but was: 1" on the second assertion (A). Passing: the four retargeted cases |
| M-W1 | I3 | `$S/scheduler/WorkManagerScheduler.kt` | `            .addContentUriTrigger(Consts.SMS_PROVIDER, true)` → `            .addContentUriTrigger(Consts.SMS_PROVIDER, false)` | `...service.WorkManagerSchedulerTest` | `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` and `inv4_contentTrigger_api29_smsUriPresent_withDescendants` — "expected to be true" on `isTriggeredForDescendants` (A). Passing: every other case (the call-log tests compare URIs only) |
| M-W2 | I3 | same | `        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {` → `        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {` (the dead branch moved onto the floor) | same | `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` — "expected not to be null" (A). Passing: every API 29 case and the U-054 case. This is the mutant that only the boundary test can see |

`\|` is Java's `||`, escaped for this table. Each edit compiles: `if (true)`/`if (false)` are the conditional-
compilation forms javac accepts without a warning, so `-Werror` is not triggered. Order: M-B1, M-B2 (I2); M-R1,
M-R2, M-W1, M-W2 (I3); M-C1 (I5, step 1a, before E5.1). No mutation runs after M-C1: once E5.1 is applied the
production tree differs from HEAD, and the I5 proof is the case analysis of 2.3 with the tests and the bytecode
comparison (row D17 governs a result that does not match). Seven mutation runs in all.

### 4.9 Commits (exact file lists and subjects)

Evidence names are those produced by the steps; `$W` = `sdlc/artifacts/build/sprints/sprint-024/U-068`.

| Commit | Subject | Files (all written out in `git add --`) |
|--------|---------|------------------------------------------|
| I1 | `U-068 S1-I1: switch on JUnit XML test reports` | `build.gradle`; `$W/implementation-log.md`; `$W/evidence/tools/{sdk_dir.py,jacoco_inventory.py,jacoco_lines.py,lint_ids.py,baseline_diff.py}`; `$W/evidence/I0-{start.txt,gradle-version.txt,gates.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,jacoco-inventory.json,jacoco-lines.txt,merged-debug.xml,merged-release.xml,manifest-debug.txt,manifest-release.txt,kept-names.txt,configuration.txt,apk-entries-debug.txt,apk-entries-release.txt,apk.txt,javap.txt,greps.txt,lint-baseline-count.txt,lint.txt,lint-ids.txt}`; `$W/evidence/I1-{gates.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,jacoco-inventory.json,jacoco-lines.txt,xml-check.txt}` |
| I2 | `U-068 S1-I2: unit test for the BACKUP broadcast receiver's opt-in guard` | `app/src/test/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiverTest.java`; `$W/implementation-log.md`; `$W/evidence/I2-{first-run.txt,gates.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,jacoco-inventory.json,jacoco-lines.txt,checks.txt}`; `$W/evidence/mutation-log.md`; `$W/evidence/mutations/{M-B1-cases.txt,M-B1-message.txt,M-B2-cases.txt,M-B2-message.txt}` |
| I3 | `U-068 S1-I3: retarget the five tests below SDK 24 to API 24 and Q` | `app/src/test/java/com/zegoggles/smssync/compat/SmsReceiverTest.java`; `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt`; `$W/implementation-log.md`; `$W/evidence/I3-{sdk24-jar.txt,first-run.txt,targeted.txt,gates.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,jacoco-inventory.json,jacoco-lines.txt,checks.txt}`; `$W/evidence/mutation-log.md`; `$W/evidence/mutations/{M-R1,M-R2,M-W1,M-W2}-{cases,message}.txt` (eight files) |
| I4 | `U-068 S1-I4: minSdk 24; drop the two permissions capped below API 24` | `app/build.gradle`; `app/src/main/AndroidManifest.xml`; `$W/implementation-log.md`; `$W/evidence/I4-{gates.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,jacoco-inventory.json,jacoco-lines.txt,merged-debug.xml,merged-release.xml,manifest-debug.txt,manifest-release.txt,kept-names.txt,configuration.txt,apk-entries-debug.txt,apk-entries-release.txt,apk.txt,javap.txt,checks.txt,manifest-diffs.txt,apk-diffs.txt}` |
| I5 | `U-068 S1-I5: remove the code branches the minSdk 24 floor makes dead` | the eight files of 3.6 (`App.java`, `activity/Dialogs.java`, `activity/MainActivity.java`, `activity/SmsDefaultRoleHelper.kt`, `calendar/CalendarAccessorPost40.java`, `compat/SmsReceiver.java`, `scheduler/BackupScheduler.java`, `scheduler/WorkManagerScheduler.kt`, each under `app/src/main/java/com/zegoggles/smssync/`); `app/src/test/java/com/zegoggles/smssync/compat/SmsReceiverTest.java` (C-1); `$W/implementation-log.md`; `$W/evidence/mutation-log.md`; `$W/evidence/mutations/{M-C1-cases.txt,M-C1-message.txt}`; `$W/evidence/I5-{c1-first-run.txt,gates.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,jacoco-inventory.json,jacoco-lines.txt,merged-debug.xml,merged-release.xml,manifest-debug.txt,manifest-release.txt,kept-names.txt,configuration.txt,apk-entries-debug.txt,apk-entries-release.txt,apk.txt,javap.txt,greps.txt,checks.txt,build-diffs.txt,bytecode.txt}` |
| I6 | `U-068 S1-I6: prune the lint baseline entries made stale` | `app/lint-baseline.xml`; `$W/implementation-log.md`; `$W/evidence/I6-{scratch.txt,scratch-lint.txt,scratch-lint-ids.txt,regenerate.txt,baseline-diff.txt,regenerated-baseline.xml,checks.txt,lint.txt,lint-ids.txt,lint-ids-compare.txt}` (without `regenerated-baseline.xml` if D41's note applies) |
| I7 | `U-068 S1-I7: CHANGES, developer and engagement docs, bug-report template for minSdk 24` | `CHANGES`; `DEVELOPMENT.md`; `sdlc/artifacts/engagement/code-location.md`; `.github/ISSUE_TEMPLATE/500_bug_report.yml`; `$W/implementation-log.md`; `$W/evidence/I7-{docs.txt,gates.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,jacoco-inventory.json,jacoco-lines.txt,merged-debug.xml,merged-release.xml,manifest-debug.txt,manifest-release.txt,kept-names.txt,configuration.txt,apk-entries-debug.txt,apk-entries-release.txt,apk.txt,javap.txt,gradle-version.txt,build-diffs.txt,final-checks.txt,scan.txt}` |
| fix | `U-068 S1-I<k> fix: <what>` | only the files the fix touches, plus the log and its evidence; made when a later gate fails because of an earlier, already committed increment (row D51). No commit is amended, rebased, reset or force-pushed |

The story file, the sprint plan, `plan.md` and the other plan-gate files in `$W`, `compiled-pipeline.json` and the
checkout's other orchestrator files are never staged by the developer.

## 5. Tests, Counts and Bytecode Expectations

### 5.1 Test table

| Test | Class | Kind | `@Config` | What it pins | Red proof | Increment |
|------|-------|------|-----------|--------------|-----------|-----------|
| `onReceive_integrationOn_schedulesImmediateOnce` | `receiver.BackupBroadcastReceiverTest` | new | default (29) | opt-in on → `scheduleImmediate()` exactly once, no other interaction | M-B2 | I2 |
| `onReceive_integrationOff_neverSchedules` | same | new | default (29) | default off → no scheduler interaction | M-B1 | I2 |
| `testOnReceive_belowQ` | `compat.SmsReceiverTest` | retargeted | `N` | `onReceive` at the floor runs the package branch without a throw | M-R2 | I3 |
| `testOnReceive_q` | same | retargeted | `Q` | `onReceive` at Q runs the role branch without a throw | M-R1 | I3 |
| `testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage` | same | retargeted | `N` | package branch → `false` | M-R2; I5 line record | I3 |
| `testIsSmsBackupDefaultSmsApp_q_usesRoleManager` | same | retargeted | `Q` | role branch → `false` (role not held) | M-R1; I5 line record | I3 |
| `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` | `service.WorkManagerSchedulerTest` | retargeted | `[24]` | INV-4 at the floor: job returned with tag `CONTENT_TRIGGER`, unique work enqueued, SMS URI trigger, descendants | M-W2 (boundary), M-W1 | I3 |
| `testEnableThenDisable_togglesReceiverComponent` (C-1, ruling P-5) | `compat.SmsReceiverTest` | new | default (29) | `SmsReceiver.enable` then `disable` set the component state to ENABLED then DISABLED; covers `disable`'s three lines | M-C1 | I5 (step 1a) |
| INV-4 at 29 (`:308`, `:332`, `:356`), U-054 (`:541`), `MainActivityRestoreTest` (five, Q), `SmsBroadcastReceiverTest` (four) | existing | unchanged | as they are | the surviving behaviour (2.3) | — | run in every gate |

### 5.2 Rename record (AC-4, AC-14 "listed by name")

| Old name | New name | Old `@Config` | New |
|----------|----------|---------------|-----|
| `SmsReceiverTest.testOnReceivePreKitKat` | `testOnReceive_belowQ` | `LOLLIPOP` | `N` |
| `SmsReceiverTest.testOnReceiveKitKat` | `testOnReceive_q` | `LOLLIPOP` | `Q` |
| `SmsReceiverTest.testIsSmsBackupDefaultSmsAppPreKitKat` | `testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage` | `LOLLIPOP` | `N` |
| `SmsReceiverTest.testIsSmsBackupDefaultSmsAppKitKat` | `testIsSmsBackupDefaultSmsApp_q_usesRoleManager` | `LOLLIPOP` | `Q` |
| `WorkManagerSchedulerTest.inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` | `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` | `[23]` | `[24]` |

### 5.3 Counts (REQ-015 AC-13 as amended by CR-016 Amendment 1; story AC-14)

- Start: `C0` = the authoritative command at `<start>` (I0 step 1), with the commit id. It includes U-067's tests,
  which land before `<start>`; they are not this story's and need no listing (none lands between `<start>` and the
  end, because the sprint is serial; if one did, row D48).
- Added by this story: three tests, by name: `onReceive_integrationOn_schedulesImmediateOnce` and
  `onReceive_integrationOff_neverSchedules` (I2), and `testEnableThenDisable_togglesReceiverComponent` (C-1, I5;
  ruling P-5: AC-14's "two added" reads "three added"). Deleted: none. Retargeted: the five of 5.2.
- Running count: `C0 + 2` after I2, I3 and I4; `C0 + 3` from I5 on.
- End: `C0 + 3`, taken over the working tree before the I7 commit (`git grep --untracked`, said so) and with the I7
  commit in place of `HEAD` after it (hand-off).
- Gradle total: `tests` in `I7-junit-totals.txt` = `C0 + 3`; failures 0; errors 0; skipped = the I0 value (2) and
  never above 2. Any difference is recorded and explained (row D48).
- Commits by others between `<start>` and the end touch documents only (I7 step 5 (iii)); they add or delete no
  test. If one did, row D48 lists it with its commit id, not as this story's.

### 5.4 Bytecode expectations for I5 step 7 (normalised `javap -c -p`)

| Class file | Expected difference |
|------------|---------------------|
| `calendar/CalendarAccessorPost40.class` | none (S-2 is an annotation, CLASS retention) |
| `activity/MainActivity.class` | none (S-5) |
| `compat/SmsReceiver.class` | only inside `isSmsBackupDefaultSmsApp`: the `SDK_INT`/19 compare and the `iconst_0; ireturn` of `return false` are gone |
| `activity/Dialogs$About$1.class` | only inside `onPageFinished`: the `SDK_INT`/14 compare is gone |
| `activity/SmsDefaultRoleHelper.class` (Kotlin) | only inside `startRestore`: the `SDK_INT`/19 compare and the call of the pre-KitKat `else` are gone |
| `scheduler/WorkManagerScheduler.class` (Kotlin) | only inside `scheduleContentTrigger`: the `SDK_INT`/24 compare, its `Log.d` and `return null` are gone, and the final log constant loses " (API 24+)" |

## 6. Decision Table

| Row | Observation | What the developer does |
|-----|-------------|-------------------------|
| D1 | HEAD is not a descendant of `$PLAN_HEAD` | Stop and report; nothing has been edited |
| D2 | U-066 or U-067 is not `done`; CR-015 or CR-016 is not `implemented`; `f0d0d17e` is not an ancestor | Stop before any edit (SC-1, SC-2, the ruling "U-067 is done before this story starts") |
| D3 | Porcelain shows a change under the checked paths that is not this story's | Stop (SC-1); never stash, restore or commit someone else's change |
| D4 | A file of table 2.1 has a blob other than its planning blob | Expected only if U-067 touched it (it should not; L1 and the U-067 story). For that file: every `old_string` of this plan that targets it must be found exactly once by text; if so, continue and log the new line numbers; if any is missing or occurs twice, stop and report "plan text no longer matches <file>" with the `git log --oneline $PLAN_HEAD..HEAD -- <file>` |
| D5 | `jps` lists a Gradle JVM, or the orchestrator has not confirmed SC-4 | Wait for the orchestrator's go; never kill a process; never run a gate meanwhile |
| D6 | G-I0 fails | Stop: the baseline is not trustworthy. Report with the sanitised log |
| D7 | I0 totals: failures or errors above 0, or skipped above 2 | Stop. If only `tests` differs from `C0`, record and explain (a test counted twice or a `@Test` in a comment); continue only if the explanation is a fact shown in the evidence |
| D8 | `receiver` at I0 is not 29/58 | Use the formula of 2.2 with the measured `c0`, `t0`; the I2 requirement stays "strictly above 0.50" |
| D9 | `scheduler` or `compat` at I0 or I3 differs from 2.2 | Recompute the I3 and I5 predictions with the formulas (C-1 included for `compat`); if the predicted I5 `compat` with C-1 is below 0.20, or the predicted `scheduler` is below 0.60, stop and report before I5 (tests are added only under a ruling; the floor is never edited) |
| D10 | No lint report readable by `lint_ids.py` | Try the other report kind; if none exists, stop and report (A22 cannot be closed). If lint exits 0, record it as unexpected and continue |
| D11 | Baseline count at I0 is not 37 | Record; recompute I6 expectations from the entry list (the REMOVE rule does not depend on the number) |
| D12 | `aapt2` or `dexdump` is missing from build-tools 36.0.0 | Use the highest build-tools folder present (36.1.0), the same one for every increment, and record it |
| D13 | `.so` count at I0 is not 0 | Record; AC-13 (d) then compares with the I0 value |
| D14 | No `TEST-*.xml` after I1's gate | Stop: E1 did not take effect; check that line 30 changed and that the run was not up to date (results deleted first) |
| D15 | I1 totals differ from I0 | Re-run G3 once (results deleted first). Still different: stop and report (E1 is a report switch and cannot change the test set) |
| D16 | A new receiver test fails on its first run | Read the failure. A fixture problem (context, preferences) is fixed in the test only, keeping the two names, the preconditions, `times(1)` and `verifyNoInteractions`. If production behaves otherwise than CNTR-MODERNIZATION-005 says (for example it schedules with the setting off), stop and report a defect; never adjust the test to the behaviour |
| D17 | A mutation result does not match its row | An expected failing case passes: the test does not guard what it claims; stop and report (plan defect). An extra case fails: record it with its message and explain; stop if it cannot be explained. The mutant does not compile: restore, record "invalid mutant", report |
| D18 | `receiver` after I2 not above 0.50, or Δ ≠ 10 | Read `I2-jacoco-lines.txt`; record which lines; stop if not above 0.50 |
| D19 | SDK 24 jar (I3 steps 2-3; ruling P-1, acknowledged) | (a) The first run downloads the jar and its `.pom`, each matching its `.sha512`: record (coordinates, sizes, SHA-512s, checksum files) and continue. (b) The download fails: re-run the same TR once and record both. **Two failed downloads stop the story** (ruling P-1): report INT-004; never skip, `@Ignore` or `Assume`, and **nothing is fetched by hand**. (c) A SHA-512 does not match its `.sha512` file: stop and report; do not delete or re-fetch by hand. (d) Any route needs `robolectric.offline`, `robolectric.dependency.*`, a build-file, `robolectric.properties` or verification-metadata change, or either build-file check of I3 step 2 is not 0 after the run: stop and report (AC-12). (e) The jar is already cached: record its folder name, sizes and `.sha512` matches; continue |
| D20 | Robolectric refuses SDK 24 or fails for a reason other than the download | Stop and report: design constraint C15 ("4.15.1 runs SDK 21 to 35") does not hold here |
| D21 | The API 24 test fails because `scheduleContentTrigger()` returns null or enqueues no SMS trigger at 24 | Stop and report: that is a behaviour fault at the floor (INV-4), not a test problem |
| D22 | A retargeted `SmsReceiverTest` case fails at `N` or `Q` | Read the failure. A Robolectric shadow difference at that level (for example `getDefaultSmsPackage` or `RoleManager` not shadowed): stop and report; never change an assertion, a level or a name to make it pass |
| D23 | `scheduler` at I3 below 0.60 | Stop and report; tests are added only under a ruling; the floor is never edited |
| D24 | Merged-manifest diff has one difference beyond AC-6's prediction: `android:extractNativeLibs` `"true"` → `"false"` (or the attribute gone) | Declared in advance (N3, 2.5 item 1) and **accepted as a declared difference by ruling P-2**: AGP derives it from the floor; the measured `.so` count is 0 in both APKs (although `ndk.abiFilters` is set), so there is nothing to extract and no runtime effect. Record it in both diffs and the log with that explanation; do not change any build file to suppress it. Any difference the floor does not explain: stop and report (ruling P-2) |
| D25 | Packaged-manifest diff (normalised) differs from the merged one in kind | Stop and report |
| D26 | Kept-name set differs from the previous increment | A name that is only a D8/R8 synthetic (for example a `$-CC` interface companion, which D8 stops creating at min-api 24, 2.5 item 3): record and explain; expected not to occur, because companions are not self-mapped. Any other name, in particular one of the nine of U-060 C-2: stop and report |
| D27 | `configuration.txt` differs | Lines that differ only in a path token after sanitising: record. Anything else: stop and report |
| D28 | APK entry lists differ | **Accepted as declared differences by ruling P-4**, each recorded and explained (N4, 2.5 items 2 and 3): `res/` entries of library resources whose qualifier-normalised list (I4 step 6) is equal, or whose difference is only removed duplicates of a normalised path (the app's own `res/` has only `-v26`); a changed number of `classes*.dex` (D8 at min-api 24 keeps interface default and static methods native; multidex itself is unchanged). Any other added or removed entry: stop and report (ruling P-4) |
| D29 | `.so` count changes | Stop and report |
| D30 | `lintVitalRelease` (inside `assembleRelease`) fails at I4 | Stop and report: a fatal lint finding caused by the floor; S1 never suppresses it |
| D31 | Any test fails at I4 | Stop and report (nothing in I4 should change test behaviour) |
| D32 | AC-7 grep after I5 is not the 14 lines of 3.6 | A site of table S still there: complete the edit. A line not in this plan (for example from U-067): stop and report (scope) |
| D33 | AC-8 grep after I5 has a hit other than `package-info.java:2` | In a file and range this plan rewords: fix the text. Elsewhere: stop and report |
| D34 | A compile error after an import removal | The plan's L6 is wrong for that file: restore that import only, record, report in the log; QA decides |
| D35 | `compat` after I5 (C-1 included) below 0.20, or `disable`'s lines not covered | Stop and report (AC-15); never edit the floor |
| D35b | C-1's first run fails (I5 step 1a), for example because Robolectric rejects the component (the AGP 9 unit-test manifest carries none of the app's components; the reviewer read `ShadowApplicationPackageManager` as storing the state with no manifest check) | Stop and report before step 2; never change the assertions to fit; the orchestrator rules on another test |
| D36 | `scheduler` after I5 below 0.60 | Stop and report (AC-15) |
| D37 | A normalised `javap` difference outside 5.4 | Stop and report |
| D38 | AC-4 (g): either `SmsReceiver` branch line has `ci=0` after I5 | Stop and report |
| D39 | `$SCR` already exists | Stop; never delete a folder this story did not create |
| D39b | The copy's `gradlew` has CRLF line ends (I6 step 2) | Remove the copy, re-create it with `git -c core.autocrlf=false archive`, re-check; if still not 0, stop and report |
| D39c | `rm -rf "$SCR"` fails | `(cd "$SCR" && ./gradlew --stop)` once, retry; still failing: record it in the log and report; never leave the copy silently |
| D40 | `updateLintBaseline` is not listed | Method B: `rm "$SCR/app/lint-baseline.xml"` then `(cd "$SCR" && ./gradlew --console=plain :app:lintDebug)`; lint writes the missing baseline and fails the build ("Created baseline file"); record both |
| D41 | REMOVE ≠ the 11 of L12 | An expected entry is KEEP or STALE-OTHER: stop and report (its site was not removed, or lint matches it otherwise). An extra REMOVE: include it only if it is `site-gone` or `file-gone`, explain it in the log, and adjust 3.7's line count. A `site-present` REMOVE: do not remove it; stop and report. If `KEEP` is near 0, the regenerated file's path form differs; stop and report. If `sanitize.py` refuses the regenerated file, keep it in the work folder and commit only the tool output |
| D42 | `N6` ≠ `N5 − |REMOVE|` and ≠ 0 | Stop and report with both lint logs |
| D43 | `git diff app/lint-baseline.xml` shows a `+` line | `git restore --source=HEAD --worktree -- app/lint-baseline.xml` (HEAD is the I5 commit, where the file equals `<start>`) and redo 3.7 |
| D44 | A rule ID appears after the story that I0 did not have | Name it in `I6-lint-ids-compare.txt` and the hand-off; do not fix or baseline it (D17) |
| D45 | The AC-11 (f) sweep lists other than the 7 lines | Explain each; a new false minSdk statement in a file this story may edit: stop and report |
| D46 | A CR-016 D-5 check differs from L17 | Stop and report (SC-2) |
| D47 | G4c fails | If caused by an earlier, committed increment: fix commit (D51). Otherwise stop and report |
| D48 | End count, XML total or skipped differ from 5.3 | Record and explain; a test added or deleted by someone else's commit after `<start>`: list it with its commit id, not as this story's |
| D49 | A hygiene scan hit | Fix the file (redact through `sanitize.py`), re-run the scan; never commit with a hit |
| D50 | "bash.exe … not found" or a process killed without a visible reason | Stop and report (endpoint security); do not work around it |
| D51 | A gate fails after the increment that caused it is committed, or the index was not clean | Make a further commit `U-068 S1-I<k> fix: ...` with explicit paths; never amend, rebase, reset, revert or force-push without the orchestrator. An unclean index before staging: stop and report |
| D52 | The tool moves a Gradle run to the background | Wait for its completion notice; never poll; start nothing else meanwhile |
| D53 | A commit in `$START..HEAD` whose subject does not start with `U-068 S1-` | It touches only documents (no path of I7 step 5 (iii)): record its id and subject; not a stop. It touches code or build paths: stop and report. A changed or new path outside this story's paths that the developer notices is recorded with its path only and is never a reason to stop (U-067 plan, constraint 12) |

### 6.1 Test C-1 (ruling P-5: always applied, in I5 step 1a)

**Placement: I5, step 1a, before the I5 code edits.** Reasons:
1. Ruling P-5 adds C-1 "in the increment the plan assigns it", and the plan assigned it to I5.
2. I5 is where S-4 removes the covered `compat` line. Adding C-1 first in that increment keeps "headroom before
   risk" inside the commit that spends the headroom, and the I5 gate is the first gate that needs it.
3. I3's design "must hold" is "count unchanged" (DES-014 S1-I3). Adding C-1 there would break that check, and I3's
   AC-4 scope is the five retargets. I4 is the floor and the manifest; a test there would mix concerns.
4. In step 1a, `app/src/main` still equals HEAD (the I4 commit), so M-C1 meets 4.8's precondition, and its recovery
   command cannot wipe the E5 edits, because none has been made yet.

AC-16's order is unchanged: one commit per increment, I1 to I7. The I5 commit carries the eight production files
plus `SmsReceiverTest.java` and the C-1 evidence (4.9).

**C1.a** (`$TS/compat/SmsReceiverTest.java`, as left by 3.3). `old_string` is the two lines
`import android.content.Intent;` and `import android.os.Build;`. `new_string` is the five lines
`import android.content.ComponentName;`, `import android.content.Context;`, `import android.content.Intent;`,
`import android.content.pm.PackageManager;`, `import android.os.Build;`. The old text is unique.

**C1.b.** `old_string` is the last test of 3.3, three lines:
`    public void testIsSmsBackupDefaultSmsApp_q_usesRoleManager() {`, its assertion line
`        assertThat(SmsReceiver.isSmsBackupDefaultSmsApp(RuntimeEnvironment.application)).isFalse();`, and `    }`.
`new_string` is the same three lines, then a blank line, then this method exactly as printed. The old text is
unique. The file then has 5 lines containing `@Test`.
```java
    @Test
    public void testEnableThenDisable_togglesReceiverComponent() {
        Context context = RuntimeEnvironment.application;
        ComponentName component = new ComponentName(context, SmsReceiver.class);
        SmsReceiver.enable(context);
        assertThat(context.getPackageManager().getComponentEnabledSetting(component))
                .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        SmsReceiver.disable(context);
        assertThat(context.getPackageManager().getComponentEnabledSetting(component))
                .isEqualTo(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }
```
**Coverage.** C-1 runs `SmsReceiver.enable` and `disable` (`:97-109` at `<start>`). `enable`'s three lines
(`:98`, `:99`, `:102`) are already covered through `SmsDefaultRoleHelper.kt:114`, so C-1 adds `disable`'s three
lines (`:105`, `:106`, `:109`): `compat` after I5 is 18/71 = 25.4 %, with 3 spare lines (2.2, L21). Robolectric
4.15.1's `ShadowApplicationPackageManager` keeps and returns a component's enabled state with no manifest check, as
the reviewer read from the jar with `javap`; if Robolectric nevertheless rejects the component, row D35b applies.

**M-C1** (one Edit; row in 4.8). File `$S/compat/SmsReceiver.java`. `old_string`
`            COMPONENT_ENABLED_STATE_DISABLED,` (12 spaces, inside `disable`, `:107` at `<start>`; unique: the import at
`:22` has no indentation and ends with `;`) → `new_string` `            COMPONENT_ENABLED_STATE_ENABLED,`. Class run
`com.zegoggles.smssync.compat.SmsReceiverTest`. Expected failing: `testEnableThenDisable_togglesReceiverComponent`,
Truth "expected: 2 ... but was: 1" on its second assertion (type A). Passing: the four retargeted cases.

## 7. Items for the Orchestrator (hand-over)

1. **Follow-up sites left by ruling (SC3):** `README.md:38` ("Tested with Android 4.x - 10.x");
   `compat/package-info.java:2` ("Kitkat+", the one AC-8 hit); `utils/WindowInsetsUtil.java:48`, `:84` and
   `WindowInsetsUtilTest.java:31`, `:116` ("API 21-34", true as written); the issue template's 4.x entries
   (`:57-61` at `0d5755c4`, `:54-58` after I7). For a follow-up item.
2. **Pre-existing test gap for the backlog (SC4):** the pre-Q branch of `SmsDefaultRoleHelper.startRestore` (API 24
   to 28: `getDefaultSmsPackage`, `setSmsDefaultPackage`, the dialog and the toast) has no unit test; every
   `MainActivityRestoreTest` case runs at Q. Test C-1 does not cover it.
3. **37 versus 38 (SC2):** the story's count (37) and command stand; the log states the difference; no design note.
4. **Design corrections for a dated note, if the orchestrator wants one** (this story edits no design; both
   accepted by ruling P-5): D8 item 4 says the `compat` ratio rises after S-4; S-4 alone lowers it from 21.9 % to
   21.1 % (it also removes the covered `else if` line), leaving 0 spare lines; with C-1 `compat` ends at 18/71,
   **3 spare lines (0 without C-1)**. The `scheduler` figure in `app/build.gradle:539` (102/164) and quoted in the
   story is stale; the last recorded value is 140/188 (L9). D4b's line numbers for the vendored guards moved with
   U-066 (the story records the new ones).
5. **For S2 (toolchain):** after S1 no test is configured below API 24 (`grep` of AC-4 (e)), so Robolectric 4.16 or
   later is no longer blocked by test levels; the `app/build.gradle:160` comment says so. The `compat` package has
   3 spare lines (0 without C-1) and `receiver` 10 after I2, the margins S2-I7 (Robolectric 4.18.1) starts from.
   The SDK 24 `android-all` jar is in the local Maven cache after I3 (coordinates, sizes and SHA-512s in
   `I3-sdk24-jar.txt`); S2-I7 resolves new names. The `extractNativeLibs` outcome (D24) is recorded for S2's
   manifest diff baseline; N11's outcome (R8 and `SDK_INT`) is recorded in `I5-bytecode.txt`.
6. **For U-069 (BUG-023):** `WorkManagerScheduler.kt` loses 7 lines at the old `:224-230` and one import, so every
   line after `:20` moves; the sentences U-069 AC-13 (b) relies on (old `:61`, `:212-213`) are byte-identical; the
   comment at `WorkManagerSchedulerTest.kt:537-538` is untouched; the retargeted test
   `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` must stay unchanged in U-069 and S4 (REQ-016 AC-8).
7. **For S3:** the lint baseline keeps every entry that is stale for reasons other than the floor (STALE-OTHER, for
   example `GradleDependency`); S3's regenerate (D17 option (a)) is where they go. The rule-ID table of I0 closes
   design assumption A22.
8. **Scope Concerns of this plan** (section 8.2): P-1 to P-5 are ruled (story Implementation Notes, "Rulings on the
   plan's scope concerns"); nothing is open.
9. **For the next contract amendments** (contract-verification O-2, O-3; no edit by this story):
   CNTR-MODERNIZATION-003 `:260` still says "minSdk 21 platform" (history); CNTR-MODERNIZATION-005 cites the
   receiver's manifest registration by line, and E4.4 moves those lines up by 14; re-cite by element name.

## 8. Contracts Verified, Scope Concerns, Risks, Out of Scope

### 8.1 Contracts (approved; read, not edited)

**CNTR-MODERNIZATION-004 v3** (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`). The
plan is built against v3 and CR-015's ruling "not a behavioural break" (CR-015 `:299-307`). Point by point, CR-015
"What S1's plan must cite" (`:345-357`):
1. CR-015 and v3, with the reworded policy lines C-8 (Breaking change policy, first bullet: "losing the
   content-trigger or its debounce on any supported level ... From v3 the pre-24 fallback is not on this list",
   contract `:266`) and C-9 (second bullet: an API-level branch inside the adapter is non-breaking, `:267`) as the
   rule: section 0, 2.3 (S-7), this section.
2. The retargeted test `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` at `@Config(sdk = [24])` with
   non-null result, unique work `CONTENT_TRIGGER_UNIQUE_NAME`, the SMS URI trigger and `triggerForDescendants =
   true` (DES-005 INV-4 row after edit D-5, contract INV-4): 3.4 T3, 5.1, M-W1, M-W2.
3. The sites D4 does not list: `BackupScheduler.java:74-77` (E5.17), `App.java:379-380` (E5.18),
   `WorkManagerScheduler.kt:256` (E5.16), `WorkManagerSchedulerTest.kt:34` (T1), `:296-299` (T2), `:371-376` (T3).
   The "via scheduleIncoming()" comments (`BackupTriggerWorker.kt:39`, `WorkManagerSchedulerTest.kt:537`) are
   BUG-023's and untouched.
4. `scheduleContentTrigger()` keeps `ScheduledJob?`; the port javadoc's `@return` is reworded to the
   encryption-degraded reason (E5.17); `@Nullable` stays.
5. Empty diffs for `SmsBroadcastReceiver.java`, the receiver's manifest entry and `BackupTriggerWorker.kt`: I5 step
   5, I7 step 5, I4 step 5.
6. The plan does not cite the broadcast path as INV-4 evidence: 2.3 lists the content-trigger tests only, and no
   new text in the code names `SmsBroadcastReceiver` (3.6, last paragraph).
Also CR-015 Amendment 1 note on finding 8: (4) S1-I4 before S1-I5, kept; (5) the scheduler tests live under
`service/`, used. No port signature, `BackupType` value, `SchedulerState` variant or invariant changes (v3
Versioning `:251`). INV-1 to INV-3 tests are untouched.

**CNTR-MODERNIZATION-005** (public `BACKUP` broadcast, frozen): only a test is added; `BackupBroadcastReceiver.java`
shows an empty diff (I7 step 5); its manifest entry is unchanged (I4 step 5); both branches of the opt-in guard
are now asserted at unit level (5.1), which until now had device-only evidence for one branch (design D8 item 2).

**CNTR-MODERNIZATION-001** (TLS factories): not touched; `k9mail-vendored/` diff empty (D4b).

No new component boundary is introduced; no CNTR is missing or in draft. Contract gate: PASS.

### 8.2 Scope Concerns of this plan

All five are ruled (story Implementation Notes, "Rulings on the plan's scope concerns", 2026-11-21).

- **P-1, ruled "acknowledged": the SDK 24 `android-all-instrumented` jar is fetched by Robolectric's own resolver,
  outside Gradle's dependency verification.** Gradle's verification metadata covers no such jar, today or after
  (none of the SDK 21, 23 and 29 jars the suite uses is in it, L11); design D5 accepts the download for S1
  ("S1 adds SDK 24's jar", `:922`); no build file or metadata entry changes, so AC-12 holds. The developer records
  the coordinates, sizes and SHA-512s of the jar and its `.pom` and the `.sha512` files they were checked against
  (I3 step 3); two failed downloads stop the story and nothing is fetched by hand (D19). The SHA-512 check shows
  transfer integrity, not origin.
- **P-2, ruled: the merged manifest may change `android:extractNativeLibs`** (N3, D24), accepted as a declared
  difference with its explanation (measured `so=0`); suppressing it would need a build-file change that AC-12
  forbids. An unexplained difference stops the story.
- **P-3, ruled "as planned": the lint prune removes only the 11 entries the floor makes stale** (or that name
  deleted files) and keeps any entry stale for another reason (STALE-OTHER), each explained.
- **P-4, ruled: floor-caused APK differences** (AAPT2 qualifier collapse in library resources, the `classes*.dex`
  count) are accepted as declared differences, each recorded and explained (D28); an unexplained one stops the
  story.
- **P-5, ruled: C-1 is authorised and required.** It is added in I5 step 1a (6.1); the story adds three tests and
  the count from I5 on is `C0 + 3`; `compat` ends at 18/71 with 3 spare lines.

The story's own Scope Concerns: 1, 2, 3, 4 and 7 are ruled and applied (section 0); 5 (the vendored `>= N` guard
stays, D4b) and 6 (no publishing words; the `CHANGES` entry has none) need nothing; 8 (stricter API 24 assertions)
is applied in T3; 9 (criteria count) needs nothing; 10 (operational) is D19, I6 and D24.

### 8.3 Risks

| # | Risk | Likelihood / impact | Mitigation |
|---|------|---------------------|------------|
| R1 | The SDK 24 jar cannot be downloaded (ruling P-1: two failures stop the story) | Low / blocks I3 | D19; retry once; never skip a test; nothing fetched by hand |
| R2 | Robolectric 4.15.1 misbehaves at SDK 24 on JDK 25 (WorkManager harness, SQLite) | Low / blocks I3 | D20, D21; the first SDK 24 run is one class |
| R3 | `compat` falls below 0.20 if the I0 figures differ from L9 | Low / gate red | 2.2 formulas; D9, D35; C-1 pre-written |
| R4 | A merged-manifest or APK difference nobody predicted | Low-medium / blocks I4 | Full EV-B at I0, I4, I5, I7; D24 to D29 classify, everything else stops |
| R5 | The lint regeneration on the scratch copy disagrees with the real tree's matching | Medium / I6 count check fails | Semantic key, site check, `N6 = N5 − 11` cross-check; D41, D42 |
| R6 | A reworded comment presents the broadcast path as INV-4 evidence (CR-015 point 6) | Low / review finding | Exact texts in 3.4 and 3.6; the AC-8 grep; no text names `SmsBroadcastReceiver` |
| R7 | A mutation left in a production file | Low / high | Pristine copies, write-ahead log, hash and clean checks, recovery note; no gate or commit while an entry is open |
| R8 | A commit takes in someone else's file | Low / medium | Explicit `git add -- <list>`, clean-index check, staged-list diff (CM steps 1 to 3) |
| R9 | U-067 moved a line this plan edits | Low / medium | D4 text-match rule; table 2.1 blobs |
| R10 | Endpoint security kills a shell mid-run | Low / medium | Foreground only; D50; recovery note |
| R11 | Evidence leaks a machine path or name (configuration.txt, lint messages, logcat) | Low / medium | `sanitize.py` on every copied text; the hygiene scan; IC-3 counts only |
| R12 | The scratch lint copy is left behind or committed | Low / low | Created outside the repository, removed in I6 step 7 with a check |

### 8.4 Out of scope

Every dependency and toolchain version, `compileSdkVersion`, `targetSdkVersion`, the Robolectric default level
and its comment, `k9mail-vendored/` (minSdk 21 and its guards), the `SMS_RECEIVED` path, `BackupTriggerWorker.kt`
(BUG-023, U-069), `BackupWorker.kt` and `RestoreWorker.kt` (U-067), any design, contract, requirement or change
record, publishing and rollout.

## 9. IC-3 and IC-4 — Device Checks (for the orchestrator; the developer runs none of this)

Preconditions: the hand-off names `app/build/outputs/apk/debug/app-debug.apk` with its size and SHA-256 from
`evidence/I7-apk.txt` (built by the I7 gate from the tree of the last commit), and no mutation entry lacks
`restored`. No command below needs `MSYS_NO_PATHCONV=1` (no device path is passed); never set it for Gradle. Raw
output stays in the orchestrator's work folder outside the repository. `ADB="$SDK/platform-tools/adb"`.

1. `"$ADB" devices -l` first; it addresses no device.
   - No device listed: start `Pixel_9_Pro` (authorised, "Start it for me", 2026-11-20) with
     `"$SDK/emulator/emulator.exe" -avd Pixel_9_Pro` as the orchestrator's own background command (never a
     subagent's), then `"$ADB" devices -l` again. **Never start or touch `Pixel_10_Pro`.**
   - More than one device listed: stop, ask the owner which serial is `Pixel_9_Pro`, send nothing to any device.
   - Exactly one emulator: `"$ADB" -s <serial> emu avd name`; if its first line is not `Pixel_9_Pro` (for example
     `Pixel_10_Pro`), send nothing more and ask the owner.
   - A physical device, or anything else: send nothing, stop, ask the owner.

   Every later command uses that serial with `-s`.
2. `"$ADB" -s <serial> wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 2; done'`
   (one foreground command, tool timeout 600000), then `getprop ro.build.version.sdk` → `36`.
3. Before: `"$ADB" -s <serial> shell dumpsys package com.zegoggles.smssync | grep -E "versionCode|minSdk|lastUpdateTime"`
   (expected `versionCode=1602 minSdk=21 targetSdk=35`) and the requested-permission block
   (`... | sed -n '/requested permissions:/,/install permissions:/p' | grep -cE "USE_CREDENTIALS|WRITE_EXTERNAL_STORAGE"`;
   expected 0 already, because both carry a `maxSdkVersion` below 36). Not installed: stop and report "not run"
   (the authorisation covers installing over the configured app only).
4. `"$ADB" -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk` → `Success`. Any failure (signature
   mismatch, `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, downgrade): stop, report "not run". Never uninstall, never `-d`,
   never `pm clear`, never change a setting, never handle the account password. Repeat step 3's `dumpsys` lines:
   expected `versionCode=1602 minSdk=24 targetSdk=35`, `lastUpdateTime` later than before, and the count 0. The
   `minSdk=24` line is the platform's own reading of the installed package.
5. `T=$("$ADB" -s <serial> shell "date '+%m-%d %H:%M:%S.000'" | tr -d '\r')` (check it is not empty), then
   `"$ADB" -s <serial> shell am start -W -n com.zegoggles.smssync/.activity.MainActivity` → `Status: ok` and
   `Activity: com.zegoggles.smssync/.activity.MainActivity` (a cold start: the install stopped the process).
6. `"$ADB" -s <serial> shell pidof com.zegoggles.smssync` → a pid P. Then
   `"$ADB" -s <serial> logcat -d -v threadtime -T "$T" > <work>/ic3-logcat.txt`, then `pidof` again → the same P (no
   crash and restart). No tap, no dialog answered, no preference changed, so nothing needs restoring.
7. Search `<work>/ic3-logcat.txt`: `FATAL EXCEPTION` lines followed by `Process: com.zegoggles.smssync` (count);
   lines with pid P at level E or F that contain `hilt` or `dagger` (case-insensitive; count); lines with pid P
   containing `IllegalArgumentException` or `SecurityException` (count).
8. Verdict: pass only with `Status: ok`, a pid that is the same before and after the log read, and all three counts
   0. Otherwise "fail" with the matching lines (redacted), or "not run" as above.
9. Shut the emulator down afterwards, as the story's IC-3 says (plan-review O-6): `"$ADB" -s <serial> emu kill`
   (only the `Pixel_9_Pro` serial identified in step 1).
10. Record `evidence/IC3-emulator.txt` through `sanitize.py` (it refuses e-mail-like text): clock time, AVD name,
    SDK level, `dumpsys` lines before and after, install result, `am start -W` output, the pids, the three counts,
    the APK SHA-256 compared with the hand-off, the verdict. Never copy raw logcat or a screenshot into the
    repository.

**What IC-3 proves:** the minSdk 24 build with the two permissions removed installs over the configured app on
Android 16, keeps its data (`install -r`, same versionCode), is read by the platform as `minSdk=24`, and starts
`MainActivity` without a crash, a Hilt error or a permission exception. **What it does not prove:** anything about
API 24; a manual backup or restore (REQ-015 AC-17's full run is S2, and needs PO-3); that the content-trigger work
is registered (auto backup is not toggled).

**IC-4.** If the product owner has not decided PO-A (Record 15) when the device step is reached, record
`evidence/IC4-not-run.txt`: "IC-4 not run: needs the product owner's decision PO-A", then what stands instead (the
SDK 24 Robolectric tests of AC-4, including `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` and the `N`
pair with the I5 line record; the merged and packaged manifests reporting minSdk 24; IC-3's `dumpsys` reading
`minSdk=24` on API 36; the lowest real level available being API 29 `Pixel_4`, not authorised for this story) and
what is not proven (install, launch, WorkManager and Hilt start, and acceptance of the content-URI trigger on a
real Android 7.0). QA judges IC-4 "not applicable until PO-A is decided". Nothing is downloaded and no device is
created. If PO-A is decided for a download, the run is the same as IC-3 on the new device, plus the first-run
permission prompts recorded before any account is configured.

## 10. Constraints for the Developer (binding)

1. Run Gradle in the foreground with the tool timeout set to 600000 ms on every call, and never with
   `MSYS_NO_PATHCONV=1` set.
2. If the tool moves a run to the background by itself, wait for its completion notice. Never poll, and start no
   other command meanwhile.
3. No background or detached shells, no `&`, no `sleep` or polling loops, no helper agents for shell work, no
   heredocs for editing (Write and Edit tools only).
4. On "bash.exe … not found" or a process killed without a visible reason: stop and report (endpoint security). Do
   not work around it.
5. Raw logs, JaCoCo XML copies, pristine copies, class-file copies and commit messages stay in the work folder
   outside the repository. Only filtered evidence (section 4.0) enters `$W`.
6. Take timestamps from `date -u +%Y-%m-%dT%H:%M:%SZ`; never type them.
7. **Commits (the one ruled exception, for this story only):** one local commit per increment I1 to I7 by procedure
   CM, with the explicit file list and subject of 4.9, on `sdlc/modernization-plan`. Never `git add -A`, `.`, `-u`
   or `git commit -a`; never push, amend, rebase, reset, revert or force-push. A failure found after a commit is
   fixed by a further commit (D51). The orchestrator pushes after the reviews.
8. Edit production code only as section 3 says, plus the logged mutations, each restored and proven by hash.
9. No dependency, Gradle, toolchain, wrapper, daemon-criteria or verification-metadata change; no edit to
   `app/lint.xml`, `app/proguard-rules.pro`, `robolectric.properties`, `k9mail-vendored/` or `.github/workflows/`.
   No `@Ignore` or `Assume`; no assertion loosened; no JaCoCo `minimum`, `includes` or `excludes` changed; no
   floor lowered.
10. No device, emulator or adb command (section 9 is the orchestrator's). `Pixel_10_Pro` is never touched.
11. Do not touch the other work in the checkout (plan-review B-6; U-067 plan, constraint 12). That is:
    - `sdlc/.skill-*` (the delegation audit log, `sdlc/.skill-context.json`);
    - `sdlc/artifacts/build/sprints/sprint-024/**` except this story's own files in `$W` (the implementation log and
      `evidence/`); in `$W`, this plan, the plan-gate reports and `compiled-pipeline.json` are not the developer's;
    - `sdlc/artifacts/stories/**` and the decision appendix
      (`sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`);
    - any design, requirement, contract or change record; the story file.

    The orchestrator may commit documents (never code) while you run, and planners and reviewers of other stories
    write under other sprint folders. Changes outside this story's paths (`app`, `k9mail-vendored`, the build files,
    `CHANGES`, `DEVELOPMENT.md`, `.github`, `sdlc/artifacts/engagement`, `$W`) are recorded if noticed and are never
    a reason to stop (I7 step 5, D53). Edit neither the story nor any record; section 7 is the orchestrator's list.
12. No e-mail address, host name, user name or user-profile path in any file written to the repository.
13. No test text may contain the string `@Test` except the annotations (the authoritative count counts lines that
    contain it; every test text in this plan complies).
14. No new file under `app/` (outside the ignored build output `app/build/`) other than
    `app/src/test/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiverTest.java`.

## 11. Quality Attribute Coverage (ISO/IEC 25010)

| Attribute | How this story serves it |
|-----------|--------------------------|
| Functional suitability | Behaviour on every level ≥ 24 is unchanged (2.3, site by site); INV-4 is asserted at the new floor with stricter assertions; CNTR-005's opt-in guard gets both branches asserted; correctness of documentation: every statement about supported levels in the edited files becomes true |
| Performance efficiency | No runtime path gains work; S-1, S-4, S-6, S-7 each remove one integer comparison per call. Build: about 23 Gradle runs; tests add 3 cases and one SDK 24 sandbox (one more `android-all` jar in memory per fork that runs an SDK 24 test, `maxParallelForks = 4`) |
| Compatibility | Co-existence and interoperability: the manifest loses two permissions no installable device applies (D4a); the app installs over the configured app (`install -r`, versionCode 1602, IC-3); the vendored module's minSdk 21 merges under the app's 24 (D4b). Accepted cost: Android 5.0 to 6.0 can no longer install updates; no device in use is affected (Record 5) |
| Usability | Not applicable to this story's scope (no UI change; the `CHANGES` entry is the user-facing note) |
| Reliability | Fault tolerance unchanged (the U-054 degraded path still returns null and is still tested); recoverability: one commit per increment, `git revert` newest-first (a hand rollback too: reverting I4 while keeping I5 is excluded, 2.4), reinstall the previous APK with `install -r` (D19); a device below the floor keeps its installed build (`INSTALL_FAILED_OLDER_SDK` on update, 2.3) |
| Security | Least privilege improves slightly: two permission declarations (`USE_CREDENTIALS`, `WRITE_EXTERNAL_STORAGE`) leave the manifest. Supply chain: one Robolectric test jar and its POM enter the local cache outside Gradle verification, as D5 accepts (ruling P-1), with sizes and SHA-512s recorded against their checksum files; no verification-metadata change |
| Maintainability | Analysability: seven dead branches and their imports leave, stale comments are corrected, ten stale lint entries and one for a deleted file leave the baseline (a monotone ratchet); testability: the receiver seam is exercised; modifiability: the toolchain stories S2 and S4 are unblocked (Robolectric 4.18 and WorkManager 2.12 need 24). The `compat` margin ends at 3 spare lines (0 without C-1, ruling P-5) |
| Portability | Installability: minSdk 24 is read by the platform (IC-3 `dumpsys`); adaptability across levels 24 to 36 rests on Robolectric at 24, 29 and Q and one real level (36); API 24 itself is not run on a device (IC-4) |

## 12. Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-068 story | `sdlc/artifacts/build/sprints/sprint-024/U-068-minsdk-24-floor.md` | Read in full: SC-1 to SC-6, AC-1 to AC-16, IC-1 to IC-4, tables, Verification Steps, Technical Context, Scope Concerns, the binding rulings |
| Sprint-024 plan | `sdlc/artifacts/build/sprints/sprint-024/sprint-plan.md` | Order, the planning-while-U-067-builds note, ruling on commits |
| U-069 story | `sdlc/artifacts/build/sprints/sprint-024/U-069-content-trigger-backup-goes-through-the-scheduler.md` | AC-13 (b), (e); what it reads around (`:102`, `:115-122`, `:184`, `:192-193`) |
| U-067 story | `sdlc/artifacts/build/sprints/sprint-023/U-067-worker-cancellation-checks-use-the-running-coroutine.md` | Its affected files (no overlap), its line-moving edits |
| DES-MODERNIZATION-014 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md` | Note `:44-52`; D2 `:462-552`; D4 `:795-880`; D5 `:920-936`; D6 `:938-979`; D8 `:1017-1066`; D9 `:1068-1084`; D17 `:1606-1648`; D18 `:1650-1673`; D19 `:1675-1692`; INT-004; P-1 section `:1800-1838`; traceability `:1876-1888`; Risks `:2027-2045` |
| DES-MODERNIZATION-005 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` | INV-4 validation row (`:379`), CR-015 note (`:511`) |
| REQ-MODERNIZATION-015 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-015-toolchain-and-test-tooling-latest-stable-minsdk-23.md` | Read in full (AC-1 to AC-18, notes) |
| REQ-MODERNIZATION-001, -006 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-001-restore-play-store-eligibility.md`, `...-006-dead-code-and-minsdk-cleanup.md` | CR-016 D-5 checks (L17); status |
| REQ type standard | `sdlc/artifacts/requirements/` | Searched `^type: standard`: none in the corpus |
| CNTR-MODERNIZATION-004 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` | v3: INV-4 row and note `:243-245`, Versioning `:249-267`, Notes `:409-411` |
| CR-015 | `sdlc/artifacts/build/change-records/CR-015-cntr-004-content-trigger-pre-api-24-fallback-removed.md` | Ruling `:248-320`; Impact `:324-357`; Amendment 1 `:459-489`; Order `:493-530`; Suggested Approach `:640-658`; Scope Concerns `:739-765` |
| CR-016 | `sdlc/artifacts/build/change-records/CR-016-requirements-wording-overtaken-before-des-014-stories.md` | D-5 `:404-419`; Parts A, B `:431-460`; Amendment 1 `:537-588` |
| U-066 plan | `sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md` | The model: sections 0, 1, 4.0, I0, I7 (mutations), I9, I11, 5, 8.3, 9, 10, 11 |
| U-061 plan, evidence | `sdlc/artifacts/build/sprints/sprint-018/U-061/plan.md` (`:250-330`, `:553-555`); `evidence/after-merged-*.xml`, `after-manifest-*.txt` | EV-B recipes, `norm()`, merged-manifest structure (L7) |
| U-060 plan, log, evidence | `sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md` (`:238-300`), `implementation-log.md`, `evidence/step8-ci-command.log`, `lint-ids-*.txt`, `kept-baseline.txt` | Inventory tool, lint figures (L13), kept-name format |
| U-063 tools, evidence | `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/` (all five used files read), `evidence/FIX-jacoco-packages.txt` | Read-only tools; latest per-package figures (L9) |
| Build | `build.gradle`, `app/build.gradle`, `gradle.properties`, `app/src/test/resources/robolectric.properties`, `app/lint.xml`, `gradle/verification-metadata.xml` (searched) | Read in full except the metadata (searched for `android-all`) |
| Manifest | `app/src/main/AndroidManifest.xml` | Read in full |
| Production sources | `App.java` (`:85-95`, `:140-150`, `:240-250`, `:370-390`), `activity/Dialogs.java`, `activity/MainActivity.java` (imports, `:230-250`, `:410-435`, `:517`), `activity/SmsDefaultRoleHelper.kt`, `calendar/CalendarAccessorPost40.java` (`:1-40`), `compat/SmsReceiver.java`, `compat/package-info.java`, `scheduler/WorkManagerScheduler.kt`, `scheduler/BackupScheduler.java` (`:60-90`), `scheduler/ScheduledJob.java`, `receiver/BackupBroadcastReceiver.java`, `preferences/Preferences.java` (`:20-110`, `:170-185`) | Read in full where no range is given |
| Tests | `compat/SmsReceiverTest.java`, `service/WorkManagerSchedulerTest.kt` (`:1-110`, `:280-560`), `receiver/SmsBroadcastReceiverTest.java`, `receiver/BootReceiverTest.java`, `activity/MainActivityRestoreTest.java` (imports, `@Config` lines) | Patterns, harness, the lines kept unchanged |
| Documents | `CHANGES` (`:1-20`), `DEVELOPMENT.md` (`:50-64`), `sdlc/artifacts/engagement/code-location.md` (`:20-34`), `.github/ISSUE_TEMPLATE/500_bug_report.yml` (`:40-70`) | The I7 edits |
| Knowledge | plugin cache `knowledge/standards/expert-exceeding-depth.md`, `truth-and-accuracy.md`, `phase-completion-report.md`, `templates/build-artifacts/build-plan.md` | Quality bar, report format, template |

## Verdict

**PASS (iteration 2).** The story can be built as written, with the orchestrator's rulings P-1 to P-5, at the start
commit that closes U-067's story. Every fact the plan relies on was read at `0d5755c4` and re-verified at
`c12a2788` for every edited file (L1 to L21); every fact that needs Gradle, Robolectric, AGP, R8, lint or a device
is a `needs_check` with its step and decision rows (N1 to N11, D1 to D53). Each acceptance and integration
criterion has its step and evidence (section 13). All six blockers of the plan-review are fixed after being
re-verified, and every observation and recommendation of the three gates is taken up (Iteration 2 table). The plan:
- keeps the ruled order (I1 before any change, I2 headroom before risk, I3 before I4, I4 before I5) and makes
  one local commit per increment with explicit paths, checked to exist before staging;
- gives every edit as exact text, including C-1 (C1.a, C1.b), keeps the two sentences U-069 relies on
  byte-identical, and never writes the old test names that the AC-8 grep would catch;
- proves the new and retargeted tests by seven mutations, including the boundary mutant M-W2 and M-C1, and proves
  the dead-code removal by a site-by-site case analysis, the existing tests and a normalised debug bytecode
  comparison, stating that the release comparisons are regression checks only (2.5, 2.6);
- re-derives the coverage arithmetic from recorded figures (`compat` 18/71 with C-1, 3 spare lines) and every
  expected count of lines or hunks;
- scopes its end-of-story checks to this story's commits and paths while the orchestrator commits documents;
- builds against CNTR-MODERNIZATION-004 v3 and cites every point CR-015 asks of S1 (8.1).

Nothing is open for the orchestrator before implementation. No finding needed a change of the story's scope.

## 13. Criterion-to-Step Map

| Criterion | Step | Evidence |
|-----------|------|----------|
| AC-1 (a)-(j) | I0 steps 1-7 | `I0-*` (gates, totals by init script, JaCoCo, merged and packaged manifests, kept names, configuration, lint and rule IDs, baseline count, `.so`, `javap`, greps) |
| AC-2 | I1 | `I1-xml-check.txt`, `I1-junit-totals.txt` |
| AC-3, IC-2 | I2 | `I2-*`, `mutations/M-B1*`, `M-B2*` |
| AC-4 (a)-(f) | I3; re-run I4, I5 | `I3-*`, `mutations/M-R*`, `M-W*`, `I4-cases.txt`, `I5-cases.txt` |
| AC-4 (g) | I5 step 3 | `I5-jacoco-lines.txt` |
| AC-5, AC-6 | I4 | `I4-checks.txt`, `I4-manifest-diffs.txt`, `I4-manifest-*.txt` |
| AC-7, AC-8, IC-1 | I5 | `I5-greps.txt`, `I5-checks.txt`, `I5-bytecode.txt` |
| AC-9 | I5 step 5, I7 step 5 | `I5-checks.txt`, `I7-final-checks.txt` |
| AC-10 | I6 | `I6-*` |
| AC-11 | I7 step 2 | `I7-docs.txt` |
| AC-12 | I4 step 4, I7 step 5 | `I4-checks.txt`, `I7-final-checks.txt` |
| AC-13 | every gate; I7 steps 3-4 | `I<k>-gates.txt`, `I<k>-junit-totals.txt`, `I7-build-diffs.txt`, `I0/I7-gradle-version.txt` |
| AC-14 (three added, ruling P-5) | I0 step 1, I2 step 5, I5 steps 1a and 5, I7 steps 5, 7 | `I0-start.txt`, `I2-checks.txt`, `I5-c1-first-run.txt`, `I5-checks.txt`, `I7-final-checks.txt`, hand-off |
| AC-15 | I0, I2, I3, I5, I7 | `I<k>-jacoco-packages.txt`, `-inventory.json`, `-lines.txt` (`compat` 18/71 with C-1) |
| AC-16 | CM(1)-CM(7) | `git log --reverse --format='%h %s' --grep='^U-068 S1-' $START..HEAD` (hand-off); the log's "I<k> commit" lines and "must hold" results |
| IC-3, IC-4 | Section 9 (orchestrator) | `IC3-emulator.txt`, `IC4-not-run.txt` |

## Phase Completion Report
---
story_id: "U-068"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-068/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
notes: "Iteration 2: B-1 to B-6, plan-review O-1 to O-7, contract-verification O-1 to O-4 and the depth check's recommendations and edge cases taken up (contract-verification O-1 considered and not taken, with the reason). C-1 is in I5 step 1a; the story adds three tests (count C0 + 3 from I5). P-1 to P-5 ruled; nothing open."
---
