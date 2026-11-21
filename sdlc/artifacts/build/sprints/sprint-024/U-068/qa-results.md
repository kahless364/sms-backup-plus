---
artifact_type: qa-results
story_id: "U-068"
verdict: "PASS"
agent: "QA Analyst"
timestamp: "2026-11-21T14:00:41Z"
ac_total: 20
ac_passed: 19
ac_failed: 0
tests_run: 756
tests_passed: 754
---

# QA Validation: U-068

Story: `sdlc/artifacts/build/sprints/sprint-024/U-068-minsdk-24-floor.md` (AC-1 to AC-16, IC-1 to IC-4, the Sites table, and the binding rulings in its Implementation Notes: before planning, on P-1 to P-5, and the iteration-2 plan-review corrections). Change under test: the seven commits `533e9f51` (S1-I1), `2aad7d70` (I2), `853483e2` (I3), `c8e98a7f` (I4), `9aa84266` (I5), `7c5e6073` (I6) and `0d490997` (I7), on top of the start commit `4300ffa9`. `git log 4300ffa9..HEAD -- app gradle k9mail-vendored build.gradle settings.gradle CHANGES DEVELOPMENT.md .github sdlc/artifacts/engagement/code-location.md` lists only those seven, so the commits between them are documents only. HEAD `677ad6c2` is a documents-only commit; `git diff --quiet HEAD -- app k9mail-vendored build.gradle` exited 0 before my gate. My filtered evidence is in `sdlc/artifacts/build/sprints/sprint-024/U-068/evidence/qa/`: `qa-gate.txt`, `qa-checks.txt`, `qa-build-outputs.txt` and `qa-mutations.txt`. All four were copied through `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/sanitize.py`, which exited 0 for each. Raw logs are kept outside the repository. I followed Record 18: I did not call the external `diff` or `cmp`; every comparison used `git diff`, `git diff --no-index` or `sha256sum`. No tool disappeared and no process was killed during this validation.

Line numbers are at `0d490997`. Paths under `app/src/main/java/com/zegoggles/smssync/` are written from `smssync/`. WMS = `smssync/scheduler/WorkManagerScheduler.kt`, WMST = `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt`, SRT = `app/src/test/java/com/zegoggles/smssync/compat/SmsReceiverTest.java`, BBRT = `app/src/test/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiverTest.java`.

## Summary

I ran the full gate once myself: exit 0, 756 tests, 0 failures, 0 errors, 2 skipped, JaCoCo passed and `assembleRelease` passed. I built my own JaCoCo report from that run's data. All 18 rules hold: `receiver` is 39/58, `compat` 18/71 and `scheduler` 137/185.

I re-derived the merged and packaged manifests from my own build. They differ from the I0 baseline only in the floor (`minSdkVersion` 24), the two removed permissions and the declared `extractNativeLibs` change. The manifest invariants hold. I checked the three APK differences the plan did not name on my own APKs, and each one is caused by the floor.

I ran every grep the story specifies at `0d490997`, plus the count rule and the empty-diff and file-list checks. I read every production and test hunk. I re-applied three mutations of my own: M-B1 and M-R2 by the plan's literal text, and M-W2, the boundary mutant, in its HEAD-equivalent form. Each failing set equals the plan's row, and each file was restored to its committed blob.

IC-3's evidence (`evidence/IC3-emulator.txt`) is present, and it meets the story's IC-3 wording. The installed APK is byte for byte the debug output that my gate found up to date against the HEAD tree. IC-4 is "not applicable until PO-A is decided", as ruled. Every criterion is met. Four are met with deviations that were declared or ruled before the build (AC-6, AC-10, AC-13, AC-14). The verdict is PASS.

## Verdict: PASS

## Gate and counts (run by QA, 2026-11-21)

| Item | Result | Evidence |
|---|---|---|
| Command | `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease`. I deleted `app/build/test-results` first. `env \| grep -c MSYS_NO_PATHCONV` printed 0. The run was in the foreground with a 600000 ms timeout; the tool moved it to the background, and I waited for the completion notice without polling. No other Gradle ran meanwhile | `evidence/qa/qa-gate.txt` |
| JDK | `./gradlew --version`: Gradle 9.5.1; Launcher JVM 25.0.5.1; Daemon JVM "Compatible with Java 25 ... (from gradle/gradle-daemon-jvm.properties)" | same |
| Result | `EXIT=0`, `BUILD SUCCESSFUL in 5m 33s`, 153 tasks (5 executed, 148 up to date). `testDebugUnitTest`, `jacocoTestCoverageVerification` and `assembleRelease` ran. The compile and `packageDebug` tasks were UP-TO-DATE, because the tree equals the developer's I7 build | same |
| JUnit totals | The build wrote its own XML (S1-I1): 80 files, `tests=756 failures=0 errors=0 skipped=2`, so 754 passed | same |
| Skipped | `AppTest#shouldGetVersionName` and `AuthPreferencesTest#u012_robolectricSmoke_encryptedPrefsSecretStore_migrateFromPlaintext`: the two `@Ignore`d tests that predate the story. The start figure was also 2 (`evidence/I0-junit-totals.txt`, `I1-xml-check.txt`) | same |
| Authoritative count | `git grep -h "@Test" <commit> -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' \| grep -c "@Test"`: **753** at `4300ffa9`, **756** at `0d490997` | `qa-gate.txt`, `qa-checks.txt` |
| Delta | +3. The test diff (3 files, +114 / −22) adds 8 `@Test` lines and removes 5. The 5 removed are replaced in place by the 5 retargeted tests; the other 3 are new: `BackupBroadcastReceiverTest.onReceive_integrationOn_schedulesImmediateOnce`, `BackupBroadcastReceiverTest.onReceive_integrationOff_neverSchedules` and `SmsReceiverTest.testEnableThenDisable_togglesReceiverComponent` (C-1, ruling P-5). None was deleted | `qa-checks.txt` |
| Gradle total against the count | Equal: 756 = 756, skipped tests included. There is no difference to explain | `qa-gate.txt` |
| Classes the story touches | `BackupBroadcastReceiverTest` 2/2, `SmsReceiverTest` 5/5, `WorkManagerSchedulerTest` 25/25, `MainActivityRestoreTest` 5/5 | `qa-gate.txt` |

## Acceptance Criteria Results

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1 baseline (S1-I0) | **Satisfied** | Each of (a) to (j) has its own file in `evidence/`: `I0-gates.txt` (exit 0, 10m 51s), `I0-start.txt` (start commit, C0 = 753, `@Ignore` 2), `I0-junit-totals.txt` (753/0/0/2 from the scratch init script), `I0-jacoco-packages.txt` and `I0-jacoco-inventory.json`, `I0-merged-{debug,release}.xml` and `I0-manifest-*.txt`, `I0-kept-names.txt` and `I0-configuration.txt`, `I0-lint.txt` and `I0-lint-ids.txt` (39 errors; rule-ID table), `I0-lint-baseline-count.txt` (37), `I0-apk.txt` (`so=0`), `I0-javap.txt` (61/61/52) and `I0-greps.txt`. The log records I0 at 11:27Z to 11:42Z. The first story commit, `533e9f51`, is dated 05:47 −0600 = 11:47Z, and that commit carries the I0 files. I checked (j) myself: at `4300ffa9` the AC-7 grep gives 24 lines, 5 of them imports (`qa-checks.txt`) |
| AC-2 JUnit XML on (S1-I1) | **Satisfied** | `build.gradle:30` reads `reports.junitXml.required = true`, and `:29` (`reports.html.required = false`) is unchanged. `git diff --numstat 4300ffa9 0d490997 -- build.gradle` = `1 1`, and `533e9f51` contains only `build.gradle` and workspace files. My gate's own XML gives the totals above. `I1-xml-check.txt` shows I1's totals equal to I0's: 753/0/0/2 |
| AC-3 headroom test for `receiver` (S1-I2) | **Satisfied** | BBRT is the only new file under `app/src/test`. It uses the real `Preferences` over the default shared preferences with key `third_party_integration` (BBRT:31, :48-51) and overrides the protected `getScheduler(Context)` seam (BBRT:40-44). `onReceive_integrationOn_schedulesImmediateOnce` (BBRT:47) asserts `verify(scheduler, times(1)).scheduleImmediate()` and `verifyNoMoreInteractions` (:55-56). `onReceive_integrationOff_neverSchedules` (BBRT:59) asserts that the default is off (:60) and `verifyNoInteractions(scheduler)` (:64). IC-2's XML: tests 2, failures 0, errors 0, skipped 0. The developer's M-B1 and M-B2 are in `evidence/mutation-log.md:9-37`, each with a restore line and a clean `app/src/main`. My Q-1 reproduces M-B1. `receiver` was 29/58 at I0 (`I0-jacoco-packages.txt`) and is 39/58 = 67.2% in my run: strictly above 0.50, 10 lines of headroom. `2aad7d70` changes no production file |
| AC-4 the five tests below SDK 24 retargeted (S1-I3) | **Satisfied** | (a) SRT:31 `testOnReceive_belowQ` at `N`, :36 `testOnReceive_q` at `Q`, :41 `testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage` at `N`, :46 `testIsSmsBackupDefaultSmsApp_q_usesRoleManager` at `Q`. The bodies are unchanged (no throw; `isFalse()`). The comment SRT:26-28 says the tests run the two surviving branches, and contains no "LOLLIPOP", no `sdk = [2x` and no "KitKat". (b) WMST:379-380 `@Config(sdk = [24])` on `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger`, with the class still at `[29]`. It asserts `isNotNull()` and the tag `CONTENT_TRIGGER_UNIQUE_NAME` (:383-384), that the unique work exists (:386-389), that a `content://sms` trigger is present (:391-393) and that `isTriggeredForDescendants` is true (:394). (c) WMST:34, :296-298 and :372-376 are reworded: none says "pre-24", calls the broadcast path a fallback, or uses "API 24+" as a condition. The `:537-538` comment (now `:548`) is not in the diff. (d) No test was deleted, no `@Ignore` or `Assume` was added, and the only assertion replaced (`isNull()`) was replaced by stronger ones. (e) `git grep -nE "LOLLIPOP\|sdk = \[?2[0-3]" 0d490997 -- app/src/test` gives no output. (f) They passed at I3 with minSdk still 21 (`I3-first-run.txt`, `I3-targeted.txt`), after I4 (`I4-cases.txt`), after I5 (`I5-cases.txt`) and in my gate. (g) My JaCoCo report for `SmsReceiver.java`: line 45 (`if (SDK_INT >= Q)`) has 2 of 2 branches covered, and lines 46 (RoleManager) and 49 (`getDefaultSmsPackage`) are both executed (`qa-build-outputs.txt`). My Q-2 shows that the `N` pair runs line 49. The three INV-4 tests at API 29 (WMST:308, :332, :356 region) and the U-054 test have no hunk |
| AC-5 the floor (S1-I4) | **Satisfied** | `app/build.gradle:24` `minSdkVersion 24`; `:21` `compileSdkVersion 35`, `:25` `targetSdkVersion 35` and `:26` `versionCode 1602` are unchanged. `git grep -nE "minSdk 21\|SDK 21" 0d490997 -- app/build.gradle` gives no output. `:153` now reads "minSdk 24". `:160` keeps the Robolectric pin reason ("ASM 9.8 reads Java 25 class files") and states that no test is configured below 24. `k9mail-vendored/build.gradle:54` still reads `minSdkVersion 21`. The merged manifests (debug and release) and the packaged manifests (`aapt2 dump xmltree`, build-tools 36.1.0) from my own build report minSdk 24 (`qa-build-outputs.txt`) |
| AC-6 two dead manifest permissions (S1-I4) | **Satisfied with deviation** | `git diff --numstat ... -- app/src/main/AndroidManifest.xml` = `0 14`, one hunk `@@ -38,14 +37,0 @@`. The removed lines are the two comment blocks, the two elements and one blank line after each, so `:37` is now followed by the network-sockets comment. `git grep -n maxSdkVersion 0d490997 -- app/src/main/AndroidManifest.xml` gives no output. I compared my merged manifests with `evidence/I0-merged-*.xml`, and my packaged manifests (line numbers stripped) with `I0-manifest-*.txt`. In both variants the only differences are `minSdkVersion` 21 → 24, the removal of the two `uses-permission` elements with their comments, and **`extractNativeLibs` true → false** (`qa-build-outputs.txt`). The last one is the deviation: declared as row D24 and accepted by ruling P-2. It comes from the floor: AGP's default for minSdk 23 and higher. There is no runtime effect, because `so=0` in both APKs before and after. Invariants, in merged and packaged form: `SystemForegroundService` has `foregroundServiceType="dataSync"` (0x1); `InitializationProvider` occurs 0 times; `BackupBroadcastReceiver` is `exported="true"` with action `com.zegoggles.smssync.BACKUP`; the `SmsBroadcastReceiver` source block has the same sha256 at `4300ffa9` and at `0d490997`. Only `TokenRefresher.java:97`, a comment, names either permission |
| AC-7 the seven dead sites deleted (S1-I5) | **Satisfied** | I read every hunk (`git diff 4300ffa9 0d490997 -- app/src/main`). S-1 `Dialogs.java:169` is `if (scrollPosition > 0 && ABOUT_HTML.equals(url))`, and the `Build` import is gone. S-2 `CalendarAccessorPost40.java:21`: `@TargetApi` and both imports are gone, and the class name is kept. S-3 `SmsReceiver.java`: `@TargetApi(KITKAT)` and its import are gone. S-4 `SmsReceiver.java:45-50` is `if (SDK_INT >= Q) {role} else {return getPackageName().equals(getDefaultSmsPackage)}`. S-5 `MainActivity.java:424`: the annotation and the `TargetApi` import are gone; `Build` stays (:241, :515). S-6 `SmsDefaultRoleHelper.kt:73-98`: `git diff -w` = `0 5`, so the outer `if`, the `else` and the pre-KitKat call are removed and the body is kept verbatim. S-7 WMS:218-222: the `SDK_INT < N` branch and the `Build` import are gone, and the return type stays `ScheduledJob?` because of the degraded path at :219-222. (a) The grep at `0d490997` gives 14 lines, 12 without imports, exactly the story's list: `App.java:246`, `:350`; `MainActivity.java:241`, `:515`; `SmsDefaultRoleHelper.kt:76`, `:106`, `:130`; `ThemeActivity.java:20`, `:26`; `SmsReceiver.java:45`; `BackupWorker.kt:534`; `RestoreWorker.kt:707`. All are level 26 or higher, and none compares against 24. The two remaining imports are `App.java:35` (`RequiresApi`) and `ThemeActivity.java:3` (`TargetApi`). (b) The removed import's simple name occurs 0 times in each edited file, and the gate compiles. (c) The U-054 test, the INV-4 API 29 tests and `MainActivityRestoreTest` (5/5) pass in my gate, and none has a hunk |
| AC-8 stale text (S1-I5) | **Satisfied** | Reworded: WMS:57-59 (KDoc: "on every supported level (minSdk 24; no version branch, CNTR-MODERNIZATION-004 v3)"), WMS:207-209, WMS:248 (log text, " (API 24+)" dropped), `BackupScheduler.java:74-78` (`@return ... null if the adapter does not schedule it (today: when encryption is degraded, U-054)`; `@Nullable` kept at :80) and `App.java:379-380`. The pre-KitKat comment left with S-6. None cites `SmsBroadcastReceiver` or calls the broadcast path an INV-4 fallback. The story's grep at `0d490997` returns exactly one hit, `compat/package-info.java:2`. "via scheduleIncoming()" (`BackupTriggerWorker.kt:39`) and "would ultimately call scheduleIncoming()" (WMST:548) are untouched |
| AC-9 empty diffs, nine files | **Satisfied** | `git diff --stat 4300ffa9 0d490997 --` over `SmsBroadcastReceiver.java`, `BackupBroadcastReceiver.java`, `BackupTriggerWorker.kt`, `BackupWorker.kt`, `RestoreWorker.kt`, `k9mail-vendored/` and `robolectric.properties` is empty. The manifest has one hunk of fourteen deleted lines and no added line. `git diff --name-only 4300ffa9 0d490997 -- app/src/main` lists exactly the nine files (`qa-checks.txt`) |
| AC-10 lint baseline pruned (S1-I6) | **Satisfied with deviation** | (a) `git diff --numstat ... -- app/lint-baseline.xml` = `0 121`. There are 0 added lines and 11 removed `<issue` openings with 11 `</issue>` closings, so whole blocks only, and the file at `0d490997` parses as XML. (b) 37 → 26 by `grep -c '^ *<issue *$'`, as I counted at both commits. (c) The 11 removed are exactly the story's list: `Deprecated` `BackupJobs.java:68`, and ten `ObsoleteSdkInt` (`CalendarAccessorPost40.java:23`, `Dialogs.java:169`, `MainActivity.java:364` and `:368`, `SmsReceiver.java:31` and `:50`, `ServiceBase.java:209` and `:225`, `SmsRestoreService.java:67` and `:165`) (`qa-checks.txt`). (d) From the developer's lint runs, which I read and did not re-run: "listed in the baseline but not found" was 15 at I0 (`I0-lint.txt`) and 19 in the scratch I5 tree, and is 8 after the prune (`I6-lint.txt`). It fell by exactly 11 from the I5 tree, and `I6-baseline-diff.txt` reports `REMOVE=11`. (e) `app/lint.xml` has an empty diff. (f) Errors 39 → 33. The rule IDs that fell are `ObsoleteSdkInt`, `UseRequiresApi` and `UnusedAttribute`. No ID is new (`I6-lint-ids-compare.txt`). Lint is still red, recorded and not gated (D17). (g) The scratch copy was removed and is not in any commit. **Deviation (ruling P-3):** 8 "STALE-OTHER" entries (3 `NonConstantResourceId`, 5 `GradleDependency`) are kept and explained in `I6-baseline-diff.txt`; the floor does not make them stale. The regenerated file itself was not kept (log, Deviations 5); the comparison output stands in for it |
| AC-11 documents (S1-I7) | **Satisfied** | (a) `CHANGES:3` reads "Minimum Android version is now 7.0 (API 24); Android 5.0, 5.1 and 6.0 (API 21 to 23) are no longer supported". It replaces the "minSdk now 14" line under the top section, and has no store, release or rollout word. (b) `code-location.md:28` reads "24 (Android 7.0)". (c) `500_bug_report.yml` has only the 6.0, 5.1 and 5.0 entries removed. (d) I ran the three CR-016 checks myself. `grep -n CR-016` gives REQ-001 :13 and :18, and REQ-006 :17 and :24. "superseded by REQ-MODERNIZATION-015" occurs once in each. `git diff f0d0d17e^ f0d0d17e` of each shows only the `change_records` list and the added note. (e) `DEVELOPMENT.md` numstat `1 1`, `:58` → "24". (f) The sweep gives 7 hits: the six expected, plus `CHANGES:3`, which the story names as intended (`qa-checks.txt`) |
| AC-12 no dependency, toolchain or floor change | **Satisfied** | `git diff --stat 4300ffa9 0d490997 --` over `gradle/verification-metadata.xml`, `gradle/wrapper/`, `gradle-daemon-jvm.properties`, `settings.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, `k9mail-vendored/build.gradle`, `app/lint.xml`, `app/proguard-rules.pro` and `.github/workflows/` is empty. `build.gradle` is `1 1` (AC-2's line). `app/build.gradle` is `3 3`, and I read the three pairs: `:24`, `:153` and `:160`. No dependency, SDK or JaCoCo `minimum`, `value` or `limit` line is among them. Outside `sdlc/artifacts/build/`, the changed files are exactly the allowed list, plus `sdlc/artifacts/stories/BUG-024-*.md`. That file comes from orchestrator commit `4f041c3f`, not from a story commit (each story commit's file list is in `qa-checks.txt`) |
| AC-13 gates | **Satisfied with deviation** | (a) Exit 0 at I0 (`I0-gates.txt`) and after I7 (`I7-gates.txt`, and my gate). My gate ran with no scratch edit or mutation present (`git diff --quiet HEAD -- app k9mail-vendored build.gradle` exit 0). (b) `I1-gates.txt` to `I5-gates.txt` each show `exit=0`; I4 and I5 include `assembleRelease`. (c) My run has 0 failures, 0 errors and 2 skipped (start 2). (d) `so=0` in both of my APKs. (e) The entry lists of my APKs equal `I7-apk-entries-*.txt` (git diff exit 0). Against I0 they differ only by what the floor explains. **Deviation (declared D28 and ruling P-4, plus three differences the plan did not name, judged here):** debug dex 17 → 23; nine `res/` base-folder duplicates of `-v21/-v22/-v23` variants dropped from each APK; and the three below, each of which I checked on my own build and judged caused by the floor (see "The three unplanned APK differences"). None is unexplained |
| AC-14 test count | **Satisfied with deviation** | 753 at `4300ffa9`, 756 at `0d490997`, by my own run of the command. 756 = 753 + 3 − 0. The three added tests are named above. None was deleted. The five retargeted tests are listed by name in `implementation-log.md:91-97`, which I compared with the diff. The commits that are not this story's add or delete no test (only the seven story commits touch `app/`). The Gradle total of 756 agrees. **Deviation (ruling P-5):** "two added" reads "three added" |
| AC-15 coverage floors | **Satisfied** | From my own report, all 18 rules are evaluated mechanically against `app/build.gradle` (`qa-build-outputs.txt`): every package is at or above its floor. `receiver` 39/58 = 67.2% (floor 0.50, 10 spare; I0 29/58); `compat` 18/71 = 25.4% (floor 0.20, 3 spare; I0 16/73); `scheduler` 137/185 = 74.1% (floor 0.60, 26 spare; I0 140/188). I0, I2 and I5 are recorded in `I0-`, `I2-` and `I5-jacoco-packages.txt`, and each matches the log. The class inventory equals I0's (`I5-checks.txt`, `I7-jacoco-inventory.json`); the one new class is a test class. No floor line is in the diff (AC-12) |
| AC-16 order and attribution | **Satisfied** | In topological order the commits are I1 → I2 → I3 → I4 → I5 → I6 → I7, each its own commit on `sdlc/modernization-plan` with the plan's subject. There are 0 merges in `4300ffa9..HEAD`. I listed each commit's files (`qa-checks.txt`), and each contains only its own increment's files and its workspace files. I2 (the test) comes before any production change, I3 before I4, and I4 before I5. The "must hold" checks are recorded under each increment in `implementation-log.md:60, :76, :99, :111, :130, :142, :155`. The branch is on `origin/sdlc/modernization-plan`, the same branch; there was no push to another branch and no merge to `master` |

### Integration Criteria

| IC | Status | Evidence |
|----|--------|----------|
| IC-1 call sites | **Satisfied** | No diff line touches `isSmsBackupDefaultSmsApp(@NonNull Context)` (`SmsReceiver.java:44`), `startRestore(activity, preferences, delegate)` (`SmsDefaultRoleHelper.kt:73`), `ScheduledJob scheduleContentTrigger()` or its `@Nullable` (`BackupScheduler.java:80-81`), or `fun scheduleContentTrigger(): ScheduledJob?` (WMS:218). The callers at `0d490997` are `App.java:382` (return value ignored), `MainActivity.java:242`, `:312`, `:427`, `SmsDefaultRoleHelper.kt:74`, `:149`, `SmsReceiver.java:35`, and the tests. They compile unchanged in my gate |
| IC-2 the new test runs in the build | **Satisfied** | `TEST-com.zegoggles.smssync.receiver.BackupBroadcastReceiverTest.xml` in my gate: `tests="2" failures="0" errors="0" skipped="0"`, cases `onReceive_integrationOff_neverSchedules` and `onReceive_integrationOn_schedulesImmediateOnce`. The class is in the 756 |
| IC-3 launch smoke on the API 36 emulator (orchestrator) | **Satisfied** | `evidence/IC3-emulator.txt`. AVD `Pixel_9_Pro` was confirmed by `emu avd name`, and it was the only device attached. `adb install -r` returned Success with data kept (no uninstall, no clear, no setting changed). Before the install: `versionCode=1602 minSdk=21`; after: `versionCode=1602 minSdk=24`, so the platform reads the new floor. `am start -W -n com.zegoggles.smssync/.activity.MainActivity` gave Status ok, with the activity `MainActivity`. The pid was 4402 before and after the log read, so there was no crash and no restart. The log shows 0 `FATAL EXCEPTION` lines, 0 Hilt or Dagger error lines and 0 `IllegalArgumentException` or `SecurityException` lines from the app's process. The evidence is text only, with no account or host name. The emulator was shut down afterwards. **The APK is the last commit's build:** the installed sha256 `40d46cab...824e` equals the debug APK left by my gate, in which `packageDebug` was UP-TO-DATE against the HEAD tree (app inputs at HEAD = `0d490997`) (`qa-gate.txt`). The file's own "proves / does not prove" lines agree with the story's IC-3 wording. Its extra observation (a pending incoming backup ran) is beyond IC-3 and not judged |
| IC-4 launch smoke on an API 24 device | **Not applicable** (until PO-A is decided) | The orchestrator's ruling before planning (story Implementation Notes): without the product owner's decision PO-A, IC-4 is recorded "not run: needs the product owner's decision PO-A", and QA judges it not applicable, not failed. `IC3-emulator.txt:18` and `implementation-log.md:222, :237` record it that way. What stands instead: the Robolectric tests at SDK 24 (AC-4 (b) and (g); my Q-3 shows that the API 24 test is the only one that sees a version branch at the floor); the merged and packaged manifests at minSdk 24 (AC-5); and the IC-3 run on API 36. Not proven: install, launch, WorkManager and Hilt start-up, and JobScheduler's acceptance of the content-URI trigger on a real Android 7.0 system |

## The three unplanned APK differences (AC-6, AC-13 (e)), judged on my own build

| Difference | What I checked | Explained by the floor? |
|---|---|---|
| (a) Debug APK lost `META-INF/CERT.RSA`, `CERT.SF`, `MANIFEST.MF` | `apksigner verify -v` on my debug APK: Verifies; v1 false; v2 true; v3 false. The I0 list has the three files (`I0-apk-entries-debug.txt`) | **Yes.** AGP signs with v1 (JAR) only when minSdk is below 24, because API 24 is the first level that verifies v2. Every supported level now verifies v2, which the installation on API 36 also exercised. The release APK is unsigned before and after |
| (b) Release `META-INF/services/r4.b0` → `q4.b0`, `w4.t` → `v4.t` | My `mapping.txt`: `kotlinx.coroutines.CoroutineExceptionHandler -> q4.b0`, `kotlinx.coroutines.internal.MainDispatcherFactory -> v4.t`. The files' contents name their implementations (`r4.b`, `r4.a`). My unobfuscated debug APK has the same two services under their real names (`AndroidExceptionPreHandler`, `AndroidDispatcherFactory`). The release has 3 service files, as at I0 | **Yes.** These are the same two service interfaces under new obfuscated names. R8 compiles for the min-api, so its output changes with the floor (mapping 95,470 → 91,896 lines), and the minified names shift. The kept-name set and `configuration.txt` are unchanged (`I4-apk-diffs.txt`; my APK lists equal I7's) |
| (c) Release `res/u01.xml` gone | Shortened `res/` paths: 414 at I0, 413 now. `aapt2 dump resources` of my release APK: `layout/abc_alert_dialog_button_bar_material` has only `()` → `res/hP1.xml` and `(watch)` → `res/GR.xml`, and both names are in the I0 list. In debug, I0 had `layout/`, `layout-v22/` and `layout-watch-v20/` for this resource; now it has `layout/` and `layout-watch-v20/` | **Yes.** At minSdk 24, aapt2 folds the `-v22` variant into the default configuration, because a device can no longer select the old base file. That removes one of the three files of this resource, which in release is the one shortened path that is now missing. The colour entries in the same row of P-4 are the same collapse, and they are not shortened in release |

## Requirement Scope Coverage

The story carries REQ-MODERNIZATION-015 AC-5 to AC-10, the gates AC-11 to AC-14 as they apply to S1, and the first part of AC-17 (as amended by CR-011 and CR-016). I read the requirement's AC-5 to AC-18 (`REQ-MODERNIZATION-015-...md:124-199`).

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| REQ-015 AC-5 | `minSdkVersion 24` and merged manifest 24; stale "minSdk 21" comment corrected | yes | `app/build.gradle:24`, `:153`; my merged debug manifest (`qa-build-outputs.txt`) |
| REQ-015 AC-6 | No SDK comparison or annotation for level ≤ 24 in `app/src/main`, imports not counted | yes | 12 non-import lines, all level 26 or higher (AC-7 (a)). The vendored `DefaultTrustedSocketFactory.java:248` (`>= N`) is outside `app/src/main` and is kept by D4b (story Scope Concern 5) |
| REQ-015 AC-7 | Five tests retargeted or deleted; grep zero; count change listed by name | yes | All five retargeted (AC-4); grep empty; names in AC-14 |
| REQ-015 AC-8 | Every stale entry removed, including `ObsoleteSdkInt`; no `lint.xml` suppression; counts before and after | yes | 37 → 26; `ObsoleteSdkInt` 10 → 0 in the baseline; `lint.xml` diff empty (AC-10) |
| REQ-015 AC-9 | Change records on REQ-001 and REQ-006 | yes (by CR-016, `f0d0d17e`) | The three D-5 checks, re-run by me (AC-11 (d)) |
| REQ-015 AC-10 | Release-notes entry in the project's location, naming 5.0, 5.1, 6.0 | yes | `CHANGES:3`, the top section |
| REQ-015 AC-11 | Verification metadata | yes (no change) | `gradle/verification-metadata.xml` has an empty diff. No artifact changed in Gradle's graph. The SDK 24 `android-all-instrumented` jar is fetched by Robolectric outside Gradle (P-1), with its SHA-512 recorded in `I3-sdk24-jar.txt` |
| REQ-015 AC-12 | Gates on JDK 25 | yes | My gate (above) |
| REQ-015 AC-13 (CR-016) | Count by the authoritative command at start and end commit; named adds and deletes; Gradle total checked | yes | 753 / 756, three named, none deleted, Gradle 756 |
| REQ-015 AC-14 | No floor lowered; every package at or above its floor; `receiver` ≥ 50% | yes | 18 rules OK; `receiver` 67.2% |
| REQ-015 AC-17 (first part only) | Launch on a higher level | yes (API 36, IC-3) | `IC3-emulator.txt`. The API 24 launch and the manual backup and restore are S2-I15 and PO-A (IC-4) |
| REQ-015 AC-15, AC-16, AC-18 | Bytecode targets, CI, first CI run | Not this story's (S2). Bytecode majors are unchanged at 61/61/52 (`I7-javap.txt`), and `.github/workflows/` has an empty diff | — |
| CNTR-MODERNIZATION-004 v3 (CR-015) | INV-4: one mechanism on every supported level; the version boundary asserted at API 24; the port keeps `ScheduledJob?` and `@Nullable` | yes | WMS:218-249; WMST:379-395; `BackupScheduler.java:80`; Q-3 |
| CNTR-MODERNIZATION-005 (frozen) | `BackupBroadcastReceiver` unchanged; a test only | yes | Empty diff (AC-9); manifest entry unchanged (AC-6) |
| DES-MODERNIZATION-005 D-5 (INV-4 row) | The retargeted test's assertions, including descendants | yes | WMST:394; M-W1 in `mutation-log.md:71-85` |

## Integration Path Verification

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `WorkManagerScheduler.scheduleContentTrigger` (S-7) | `App.onCreate` → `rescheduleJobs` when auto backup is on | `App.java:382` `scheduler.scheduleContentTrigger()` → WMS:218 → degraded guard :219-222 → constraints :224-230 → `enqueueUniquePeriodicWork` → `ScheduledJob` :249 | yes (code read; WMST:379 drives WMS:218 directly at SDK 24) |
| `SmsReceiver.isSmsBackupDefaultSmsApp` (S-4) | `MainActivity` (:242, :312), `SmsDefaultRoleHelper` (:74, :149), manifest receiver `SmsReceiver.onReceive` (:35) | → `SmsReceiver.java:45` → role (:46-47) or package (:49) | yes (SRT at `N` and `Q`; JaCoCo branch record) |
| `SmsDefaultRoleHelper.startRestore` (S-6) | `MainActivity.startRestore` (`MainActivity.java:427`) | → `SmsDefaultRoleHelper.kt:74` default-app check → `:76` Q branch, or `:83-97` legacy pre-Q path | yes for Q (`MainActivityRestoreTest`, 5/5); the pre-Q path is reached by code read only (see Behaviour preserved) |
| `Dialogs.About` `onPageFinished` (S-1) | WebView callback of the About dialog | `Dialogs.java:167-171` | yes, code read only |
| `BackupBroadcastReceiverTest` (new test) | `:app:testDebugUnitTest` discovery | 80 XML files in my gate, including this class (2 cases) | yes |
| Manifest | Platform package manager | Merged → packaged `AndroidManifest.xml` in both APKs | yes (aapt2 dumps; the IC-3 install reads `minSdk=24`) |

## Behavioral Contract Verification

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| CNTR-004 v3 INV-4 | Content-URI trigger on every supported level, SMS URI with descendants, call-log URI under its conditions | WMS:224-230 unchanged; no version branch remains | yes |
| CNTR-004 v3 INV-4 boundary | Asserted at API 24 | WMST:379-395; Q-3 fails only this test | yes |
| U-054 degraded path | `scheduleContentTrigger` returns null and enqueues nothing when encryption is degraded | WMS:219-222 unchanged; the U-054 test passes (mine and Q-3) | yes |
| Port `BackupScheduler.scheduleContentTrigger` | `@Nullable ScheduledJob`; javadoc gives the real null reason | `BackupScheduler.java:74-81` | yes |
| BUG-009 pre-Q default-SMS path | `getDefaultSmsPackage` comparison for API 24-28 | `SmsReceiver.java:49`; `SmsDefaultRoleHelper.kt:83-97` verbatim (`git diff -w` 0 added) | yes |
| CNTR-005 BACKUP broadcast | Action, class, filter, export and opt-in guard unchanged | Empty diff of `BackupBroadcastReceiver.java`; merged manifest :199-207 | yes |
| Manifest invariants (BUG-016) | `dataSync` FGS type, no `InitializationProvider`, BACKUP receiver exported | Merged release :166-170, :199-207 | yes |

## Capabilities Inventory Verification

| Capability | Status in Log | Verified in Code | File:Line | Result |
|------------|--------------|-----------------|-----------|--------|
| About-dialog scroll restore | RETAINED | yes | `Dialogs.java:169-171` | PASS |
| `CalendarAccessorPost40` behaviour | RETAINED | yes; annotation only removed (`I5-bytecode.txt`: identical) | `CalendarAccessorPost40.java:21` | PASS |
| `SmsReceiver` `onReceive`, `enable`, `disable`, role check, package check | RETAINED | yes | `SmsReceiver.java:33-50`, `:103` | PASS |
| `SmsDefaultRoleHelper.startRestore` body (default app, Q branch, legacy path, toast) | RETAINED | yes (`git diff -w` 0/5) | `SmsDefaultRoleHelper.kt:73-98` | PASS |
| `MainActivity.startRestore` | RETAINED | yes | `MainActivity.java:424-427` | PASS |
| `scheduleContentTrigger`: degraded null, triggers, unique work, `ScheduledJob` | RETAINED | yes | WMS:218-249 | PASS |
| All other manifest entries | RETAINED | yes (merged diff has only the stated hunks) | `qa-build-outputs.txt` | PASS |
| 26 lint baseline entries | RETAINED | yes (parses; 26) | `app/lint-baseline.xml` | PASS |
| `return false` below KitKat; pre-KitKat `else`; `SDK_INT < N` branch; three `@TargetApi`; Dialogs `>= 14` operand | INTENTIONALLY REMOVED | N/A | N/A | Valid: AC-7 S-1 to S-7 |
| Log suffix " (API 24+)" | INTENTIONALLY REMOVED | N/A | N/A | Valid: AC-8 |
| `USE_CREDENTIALS` (max 22), `WRITE_EXTERNAL_STORAGE` (max 18) | INTENTIONALLY REMOVED | N/A | N/A | Valid: AC-6, D4a |
| 11 stale baseline entries | INTENTIONALLY REMOVED | N/A | N/A | Valid: AC-10 |

## Behaviour preserved for the removed code (task item 5)

| Site | What shows that the API 24+ path is unchanged | Gap |
|---|---|---|
| S-1 `Dialogs` `>= ICE_CREAM_SANDWICH` operand | The developer's bytecode comparison (`I5-bytecode.txt`): only the `SDK_INT`/14 compare is gone, and the remaining instructions are the same with shifted offsets. The operand was always true at 24 | **No test** reaches `Dialogs.About` (`git grep` over `app/src/test` finds no reference). Evidence is the bytecode and the source diff only |
| S-2 `CalendarAccessorPost40` `@TargetApi` | Bytecode identical (`I5-bytecode.txt`); `calendar` tests pass (37/50 unchanged) | None. A compile-time annotation has no runtime effect |
| S-3 `SmsReceiver` `@TargetApi` | Annotation only; SRT 5/5 | None |
| S-4 `isSmsBackupDefaultSmsApp` | `_belowQ` tests at `N` run line 49 (my Q-2 fails exactly them), and `_q` tests at `Q` run the role branch (M-R1). JaCoCo: 2/2 branches of line 45. Bytecode: only the `>= 19` compare and `iconst_0; ireturn` are gone | Both outcomes are `false`, so results alone cannot tell the branches apart; the branch record and the mutations do (ruled as designed, Scope Concern 4) |
| S-5 `MainActivity` `@TargetApi` | Bytecode identical; `MainActivityRestoreTest` 5/5 at Q | None |
| S-6 `SmsDefaultRoleHelper.startRestore` | `git diff -w` = 0 added, 5 removed (pure unwrap); bytecode shows only the `>= 19` compare and the pre-KitKat call gone; the Q branch is covered by `MainActivityRestoreTest` (5/5) | **The pre-Q legacy path (API 24-28) is run by no unit test.** This gap predates the story and was accepted as designed (ruling on Scope Concern 4; backlog). Its preservation rests on the whitespace-insensitive diff and the bytecode |
| S-7 `scheduleContentTrigger` `SDK_INT < N` branch | WMST:379 at SDK 24 now asserts enqueuing; my Q-3 shows it is the only test that sees a branch on the floor; the three INV-4 API 29 tests and U-054 pass; bytecode shows only the `/24` compare, its log and `return null` gone | None at unit level. A real API 24 JobScheduler is IC-4 |

## Test Results

- Full gate: exit 0, `tests=756 failures=0 errors=0 skipped=2` (754 passed). JaCoCo verification passed and `assembleRelease` passed (`evidence/qa/qa-gate.txt`).
- The four classes the story touches or relies on: 2/2, 5/5, 25/25, 5/5.

### QA mutations (`evidence/qa/qa-mutations.txt`)

Each mutation was one single-line `sed` substitution on the working-tree file, followed by one targeted run of the plan's test class and then `git checkout -- <file>`. Before and after each one, `git status --short -- app k9mail-vendored build.gradle` was empty, and the file's `git hash-object` equalled the committed blob.

| Id | Plan row | Mutation | Result | Tests that failed | Restore |
|----|----------|----------|--------|-------------------|---------|
| Q-1 | M-B1 (receiver test; opt-in gate removed), literal | `BackupBroadcastReceiver.java:51` `if (new Preferences(context)...` → `if (true \|\| new Preferences(context)...` | **Detected**: exit 1, 1 of 2 failed | `onReceive_integrationOff_neverSchedules` (`NoInteractionsWanted`); the "on" test passed. Equals the plan row | hash `b038d7fd...` = HEAD blob; status empty |
| Q-2 | M-R2 (package branch throws), literal | `SmsReceiver.java:49` prefixed with `if (true) throw new IllegalStateException("U-068 M-R2");` | **Detected**: exit 1, 2 of 5 failed | `testOnReceive_belowQ`, `testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage` ("U-068 M-R2"); the two `_q` cases and C-1 passed. Equals the plan row | hash `bb0c5be0...` = HEAD blob; status empty |
| Q-3 | M-W2 (the boundary mutant), HEAD-equivalent | M-W2's literal target (`if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {`) was deleted by S-7, so its literal text cannot be applied at HEAD. The equivalent puts the null return back on the floor: WMS:219 `if (secretStore.isEncryptionDegraded()) {` → `if (secretStore.isEncryptionDegraded() \|\| android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.N) {` | **Detected**: exit 1, 1 of 25 failed | `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` ("expected not to be: null"); every API 29 case and U-054 passed. Equals the plan row | hash `0194308c...` = HEAD blob; status empty |

After the last restore, `git diff --quiet HEAD -- app k9mail-vendored build.gradle` exits 0. A post-restore run of the three classes was green (2/5/25), so no class compiled from a mutant remains in `app/build`. Nothing was staged or committed. Note: `app/build/test-results` and the JaCoCo `.exec` now hold that targeted run's data, not the full gate's; my gate's XML and report were captured before the mutations.

## Regression Results

- Every other test class passes in the full gate (0 failures, 0 errors over 80 files).
- Build files, dependency verification, the wrapper and daemon criteria, and the vendored module: empty diff (AC-12).
- The release kept-name set (48), `configuration.txt` and the bytecode majors are unchanged from I0 (`I4-apk-diffs.txt`, `I7-build-diffs.txt`). My APK entry lists equal I7's.
- Hygiene: I scanned the added lines of each of the seven commits for e-mail-like tokens, the user name, the host name and user-profile or application-data paths: 0 hits in each. Whole-file hits are only the upstream copyright headers (`App.java:2`, `Dialogs.java:1-2`, `MainActivity.java:1-2`), which predate the story and are not in its diff. My four evidence files passed the sanitiser's fail-closed check.

## Deviations (all declared or ruled before the build, and accepted)

1. **AC-6 / AC-13 (e), `extractNativeLibs` true → false** (D24, P-2): caused by the floor; no runtime effect at `so=0`.
2. **AC-13 (e), declared APK differences** (D28, P-4): debug dex 17 → 23; nine base `res/` duplicates collapsed in each APK.
3. **AC-13 (e), three differences the plan did not name** (v1 signature files, two renamed service files, one shortened `res/` path): judged above, each caused by the floor. The developer flagged them rather than hiding them; P-4's "an unexplained difference stops the story" does not fire.
4. **AC-10, 8 STALE-OTHER entries kept** (P-3), and the regenerated baseline file not kept (log Deviations 5). The comparison output stands in for it.
5. **AC-14, three added, not two** (P-5).
6. **Comparison tool** (Record 18; log "Deviations: comparison tool"): after `diff.exe` was quarantined, the developer used `git diff --no-index`. The comparisons made before the quarantine (I1 `xml-check`, I4 manifest and APK diffs) were not repeated by the developer. I re-derived the manifest and APK comparisons myself with `git diff --no-index`, and they agree.
7. **My own Q-3** uses the HEAD-equivalent form of M-W2, because the literal line no longer exists.

## Issues Found

- **No defects.**
- **Runtime claims.** The implementation log's `## Runtime Claims` is empty, and that is correct: the story's runtime-behaviour criterion (install and launch on a real platform) is IC-3, and its evidence is embedded with raw observations (`IC3-emulator.txt`). The API 24 device claim (IC-4) is **not made**: it is disclosed as not run under the PO-A ruling, not reported as verified.
- **O-1 (observation).** `WorkManagerSchedulerTest.kt:302` still says "on API 29 (>= 24)" in the KDoc of an unchanged INV-4 API 29 test. It is true, lies outside AC-4 (c)'s ranges, and states no condition. No action is required.
- **O-2 (observation).** The pre-Q legacy branch of `SmsDefaultRoleHelper.startRestore` (API 24-28) and the About-dialog scroll restore have no unit test. Both gaps predate the story. The first was ruled a backlog item (Scope Concern 4).

## Recommendations (not required by any criterion)

1. Before any further device install or coverage reading from this checkout, run the gate again: `app/build/test-results` and the JaCoCo `.exec` now hold my targeted post-restore run.
2. Add a unit test at `@Config(sdk = N)` for `SmsDefaultRoleHelper.startRestore`'s legacy path (the backlog item of Scope Concern 4). With the floor at 24, it is the only supported-level path of restore negotiation with no test.
3. In S2 (Robolectric 4.18), re-run M-W2's HEAD-equivalent and M-R2 as well as the tests: they are the only mutants that pin the API 24 behaviour.
4. Follow-up text sites stay as ruled: `compat/package-info.java:2`, the `WindowInsetsUtil` "API 21-34" comments, `README.md:38` and the issue template's 4.x entries. Update the stale `scheduler` comment figure in `app/build.gradle` (102/164; measured 137/185) at the next story that edits that file.
5. PO-A (an API 24 image for IC-4 / REQ-015 AC-17) is still open for S2-I15.

## Phase Completion Report
---
story_id: "U-068"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-068/qa-results.md"
story_status: "done"
current_build_phase: "validation"
ac_passed: 19
ac_total: 20
errors: []
---
