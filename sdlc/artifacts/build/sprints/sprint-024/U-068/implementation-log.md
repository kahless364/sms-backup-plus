---
artifact_type: implementation-log
story_id: "U-068"
gate_id: "implementation"
checkpoint: "implementation"
verdict: "FAIL"
agent: "Developer"
timestamp: "2026-11-21T12:45:00Z"
iteration: 1
base_commit: "4300ffa98370b77dcc4dd8d12142079aada8313a"
files_changed: 14
files_created: 1
tests_added: 3
tests_passing: 756
---

# Implementation Log: U-068 - Raise minSdk to 24 and remove what the floor makes dead

Plan: `sdlc/artifacts/build/sprints/sprint-024/U-068/plan.md` (iteration 2), followed step by step. Evidence is under `sdlc/artifacts/build/sprints/sprint-024/U-068/evidence/`. Raw logs, saved JaCoCo XML copies, pristine copies and commit messages were kept in a work folder outside the repository. One local commit per increment, explicit paths, nothing pushed. No emulator or device was used (I8 is the orchestrator's).

**Status: INCOMPLETE. The run stopped inside increment I5 (before its commit) because endpoint security quarantined `diff.exe`; see "Stopped here" below. `verdict: FAIL` in the frontmatter means "phase not finished", not a failed gate: every gate that was run is green. Sections for I6, I7, Contract Adherence, Capabilities Inventory and Runtime Claims are not written yet.**

`START` = `4300ffa98370b77dcc4dd8d12142079aada8313a` (subject "Sprint-023 closed: U-067 done, BUG-022 closed; U-068 plan gates passed"), branch `sdlc/modernization-plan`. `C0` = **753** (authoritative command at `START`).

## Start conditions (SC-1 to SC-6)

| SC | Check and result | Evidence |
|----|------------------|----------|
| SC-1 | U-066 `status: done`, U-067 `status: done`. `git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat CHANGES DEVELOPMENT.md .github sdlc/artifacts/engagement` printed nothing (`porcelain-end` preceded by no line). | `evidence/I0-start.txt` |
| SC-2 | CR-015 and CR-016 `status: implemented`; `3af4ce32` and `f0d0d17e` are ancestors of HEAD (`cr-ancestor-exit=0`). | `evidence/I0-start.txt` |
| SC-3 | `START` and `C0` recorded above. `@Ignore` on code lines: 2 (`AppTest.java:13`, `AuthPreferencesTest.java:1079`). | `evidence/I0-start.txt` |
| SC-4 | `jps -l` listed no Gradle JVM (`jps-gradle-end` preceded by no line); the delegation states that nobody else runs Gradle in this checkout. | `evidence/I0-start.txt` |
| SC-5 | U-067 is done (its fix is `c12a2788`, inside `START`); BUG-023 is U-069, not started. | `evidence/I0-start.txt` |
| SC-6 | No device or emulator was touched by this run. | - |

All 19 files of plan table 2.1: HEAD blob equals working-tree hash equals planning blob (19 of 19, `evidence/I0-start.txt`). `plan-head-ancestor-exit=0`. Commits between the plan head `c12a2788` and START: `36e715e7` and `4300ffa9`, documents only (`moved-files-end` preceded by nothing). Rows D1 to D5: none fired.

## Interruption (endpoint security) and the substituted comparison commands

- **What happened:** during I5, after the I5 gate and most of its checks, the endpoint-security product (SentinelOne) logged "Malware detected" for the Git for Windows `usr/bin/diff.exe` (event 06:36:29 local time, about 12:36 UTC) and quarantined it. The next `diff` call failed with "command not found". I stopped, ran no further shell command, recorded the state in this log with the Edit tool, and reported.
- **Owner's decision (2026-11-21, relayed by the orchestrator):** builds continue using Git's built-in comparison in place of `diff.exe`; nothing is restored from quarantine and no exclusion is added. I called neither `diff` nor `cmp` after the stop and installed, copied or downloaded no comparison tool.
- **Deviations: comparison tool.** Every `diff` of the plan after the stop is `git -c core.autocrlf=false diff --no-index --no-color -- A B` (header lines cut with `tail -n +5`; the exit code is printed as `git-diff-no-index-exit=0|1`, 0 equal, 1 differ). Process substitutions (`<(...)`) are replaced by scratch files in the work folder, because `--no-index` needs real paths. Output is unified (`-`/`+` lines) instead of `<`/`>`; where the plan counts lines the pattern is `^-` / `^+` excluding the `---`/`+++` headers. Adapted commands and expected values are given in the entries below where they are used.
- The comparisons made before the stop (I1 `xml-check`, I4 manifest and APK diffs, the first part of I5-bytecode) used the external `diff` and were not repeated; their outputs are in the evidence files as produced.

## Steps

### I0 - Baseline at START (2026-11-21T11:27:07Z to 11:42Z; no tracked file changed)

- **Gate G-I0** (`clean`, `assembleDebug`, `testDebugUnitTest`, `jacocoTestReport`, `jacocoTestCoverageVerification`, `assembleRelease`, with the read-only init script for the XML): `exit=0`, `BUILD SUCCESSFUL in 10m 51s`, JDK `25.0.5.1`; `files=79 tests=753 failures=0 errors=0 skipped=2`. Gradle 9.5.1, Daemon JVM "Compatible with Java 25 ... (from gradle/gradle-daemon-jvm.properties)". Evidence `I0-gates.txt`, `I0-junit-totals.txt`, `I0-gradle-version.txt`, `I0-cases.txt`. Rows D6, D7: none fired (the Gradle total 753 equals `C0`).
- **Per-package coverage at START** (`I0-jacoco-packages.txt`, `I0-jacoco-inventory.json`; 23 packages): `receiver` 29/58 = 50.0 % (spare 0), `compat` 16/73 = 21.9 % (spare 1), `scheduler` 140/188 = 74.5 %, `activity` 199/1011, `calendar` 37/50. These equal the plan's L9 figures for `receiver`, `compat`, `scheduler` and `calendar`; `activity` is 199 (plan 189, an expectation only; floor 0.10, spare 97). Rows D8, D9: not fired. (`jacocoTestCoverageVerification` passed.)
- **Build outputs** (`I0-merged-*.xml`, `I0-manifest-*.txt`, `I0-kept-names.txt`, `I0-configuration.txt`, `I0-apk-entries-*.txt`, `I0-apk.txt`, `I0-javap.txt`): debug APK 542 entries, `so=0`, 17 dex; release APK 525 entries, `so=0`, 1 dex; 48 kept names; all nine reflection-sensitive classes self-mapped (1 each); class-file major version 61 for the app classes (`App`, `BackupWorker`) and 52 for the vendored `Address`. Row D13: not fired (`.so` count 0).
- **Greps at START** (`I0-greps.txt`): AC-7 grep 24 lines (5 imports); AC-8 grep 29; AC-4 (e) grep 6; AC-11 (f) sweep 9; `maxSdkVersion` in the manifest 2 (`:41`, `:50`); `minSdk 21|SDK 21` in `app/build.gradle` 2 (`:153`, `:160`). All equal the plan.
- **Lint baseline count** (`I0-lint-baseline-count.txt`): 37 by `grep -c '^ *<issue *$'`, 38 by a plain `<issue` match. The design's 38 counts the root `<issues` tag; the story's 37 and its counting command govern (Scope Concern 2, ruled; no design note added). Row D11: not fired.
- **Lint at START** (`I0-lint.txt`, `I0-lint-ids.txt`): `exit=1` (pre-existing red, D17: recorded, not fixed, not gated). "Lint found 39 errors (and 22 errors filtered by baseline)"; "15 errors/warnings were listed in the baseline file but not found in the project" (U0 = **15**; the plan expected 16 from the AGP 9.0.1 record of U-060, an expectation only). Rule-ID table (closes design assumption A22): UnusedResources 18, LogTagMismatch 4, UseRequiresApi 4, CheckResult 2, CustomX509TrustManager 2, ObsoleteSdkInt 2, TrustAllX509TrustManager 2, UseKtx 2, AndroidGradlePluginVersion 1, StaticFieldLeak 1, UnusedAttribute 1 (all Error: 39), plus LintBaseline 1 and LintBaselineFixed 1 (Hint). Row D10: not fired (the XML report was readable).

### I1 - JUnit XML on (AC-2; evidence `I1-*`)

- E1: `build.gradle:30` now reads `reports.junitXml.required = true`; line 29 unchanged. `git diff -U0 -- build.gradle` shows exactly one `-` and one `+` line.
- Gate G3 (no init script): `exit=0`, `BUILD SUCCESSFUL in 4m 25s`. The build's own XML: 79 files (`ls $R/TEST-*.xml`); totals `tests=753 failures=0 errors=0 skipped=2`, equal to I0's init-script totals (`totals-diff-exit=0`, `I1-xml-check.txt`). Rows D14, D15: not fired.
- **Must hold (S1-I1):** "gates green; XML totals equal I0" - held.
- Commit: see "I1 commit" at the top of the I2 entry.

### I2 - `BackupBroadcastReceiverTest` first (AC-3, IC-2; evidence `I2-*`, `mutation-log.md`, `mutations/M-B1-*`, `M-B2-*`)

I1 commit: `533e9f51b5f880cdad05c81880c401b130352b95` ("U-068 S1-I1: switch on JUnit XML test reports").

- Pre-check: `git diff --quiet HEAD -- app/src/main` exit 0. New file `app/src/test/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiverTest.java` written exactly as plan 3.2. No production file changed.
- First targeted run (`I2-first-run.txt`): `exit=0`, two PASS (`onReceive_integrationOff_neverSchedules`, `onReceive_integrationOn_schedulesImmediateOnce`), `cases=2`. Row D16: not fired.
- Gate G3: `exit=0`, `BUILD SUCCESSFUL in 3m 35s`; `files=80 tests=755 failures=0 errors=0 skipped=2` = C0 + 2. IC-2: `TEST-com.zegoggles.smssync.receiver.BackupBroadcastReceiverTest.xml` has `tests="2" skipped="0" failures="0" errors="0"` (`I2-checks.txt`).
- Coverage after I2 (`I2-jacoco-packages.txt`, `I2-jacoco-lines.txt`): `receiver` **39/58 = 67.2 %** (was 29/58 = 50.0 %), delta 10, **spare lines = 39 - 29 = 10**. Every `BackupBroadcastReceiver` line has `ci>0` except `:68` (`return App.getScheduler(context)`, the line the test overrides). `compat` 16/73 and `scheduler` 140/188 unchanged. Row D18: not fired.
- Mutations (plan 4.8; write-ahead entry, one Edit, one targeted run, restore by copying the pristine file, hash and clean check):
  - **M-B1** (opt-in gate removed, `if (true || ...)`): exactly `onReceive_integrationOff_neverSchedules` failed (`NoInteractionsWanted`, type A); the "on" test passed. Matches the plan row. Restored, hash equals HEAD blob, `app/src/main` diff empty.
  - **M-B2** (`if (false) getScheduler(context).scheduleImmediate();`): exactly `onReceive_integrationOn_schedulesImmediateOnce` failed (`WantedButNotInvoked`, type A); the "off" test passed. Matches the plan row. Restored the same way.
  - Row D17: not fired. After both: `git diff --quiet HEAD -- app/src/main` exit 0; `git hash-object` of `BackupBroadcastReceiver.java` equals `b038d7fd...` (its blob at START).
- Working-tree count with `--untracked`: 755 (C0 + 2); `git status --porcelain -- app` lists only the new test file.
- **Must hold (S1-I2):** "`receiver` coverage above 50 % with headroom; count + 2" - held (67.2 %, 10 spare, 755).

### I3 - The five tests retargeted, minSdk still 21 (AC-4; evidence `I3-*`, `mutation-log.md`, `mutations/M-R1-*`, `M-R2-*`, `M-W1-*`, `M-W2-*`)

I2 commit: `2aad7d709d1f90e510d96369ed64d26b4e9d80d0` ("U-068 S1-I2: unit test for the BACKUP broadcast receiver's opt-in guard").

- **SDK 24 jar (ruling P-1, row D19 (a)):** before any run the cache held no `7.*` folder (`I3-sdk24-jar.txt`); both build-file checks were 0 (`build-files-uncommitted-exit=0`, `build-files-vs-start-exit=0`). The first SDK 24 run (one class, `SmsReceiverTest`) let Robolectric's own resolver fetch it; **no download failure, no retry**. Coordinates `org.robolectric:android-all-instrumented:7.0.0_r1-robolectric-r1-i7`: jar 88,212,080 bytes, SHA-512 `0776aacf...a9ad9a9` equal to its `.jar.sha512` file (match); pom 1,403 bytes, SHA-512 `1e5b3481...a926784` equal to its `.pom.sha512` file (match); both checksum files 128 bytes (full hashes in `I3-sdk24-jar.txt`). The two build-file checks were 0 again after the run (no build file, `robolectric.properties` or verification-metadata change). The SHA-512 check shows the transfer is intact, not who published the file. Nothing was fetched by hand.
- Edits (plan 3.3, 3.4): `SmsReceiverTest.java` (comment rewritten, four tests renamed and retargeted to `N` and `Q`, bodies and assertions unchanged); `WorkManagerSchedulerTest.kt` T1 (`:34`), T2 (`:296-298`), T3 (`:371-384`). `git diff HEAD` of the Kotlin test shows exactly three hunks (`@@ -31,7 +31,7 @@`, `@@ -293,9 +293,9 @@`, `@@ -369,18 +369,29 @@`). The `:537-538` comment (BUG-023) is not in the diff.
- First run (`I3-first-run.txt`): `exit=0`, four PASS with the new names, `cases=4`. Targeted run (`I3-targeted.txt`): `exit=0`, 29 cases, 0 fail; `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger PASS`; class case counts unchanged (`SmsReceiverTest` 4, `WorkManagerSchedulerTest` 25). Rows D20, D21, D22: not fired.
- Gate G3: `exit=0`, `BUILD SUCCESSFUL in 3m 37s`; `files=80 tests=755 failures=0 errors=0 skipped=2` (count unchanged by I3: C0 + 2).
- Coverage after I3 (`I3-jacoco-packages.txt`): `receiver` 39/58 (unchanged), `compat` 16/73 (unchanged), **`scheduler` 138/188 = 73.4 %** (140 to 138: the two lines only the SDK 23 test ran, as predicted; N9), above 0.60. `activity` 189/1011; the figure alternates between 199 and 189 from run to run (I0 199, I1 189, I2 199, I3 189), so it is test-scheduling dependent and not caused by this story; it is far above its 0.10 floor. Row D23: not fired.
- Mutations (plan 4.8): all four matched their rows. **M-R1** (role branch throws): the two `_q` cases failed (IllegalStateException "U-068 M-R1", type X), the two `_belowQ` cases passed. **M-R2** (package branch throws): the two `_belowQ` cases failed, the two `_q` cases passed. **M-W1** (SMS trigger not for descendants): `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` and `inv4_contentTrigger_api29_smsUriPresent_withDescendants` failed ("expected to be true"), the other 23 passed. **M-W2** (`< N` moved to `<= N`, the dead branch on the floor): exactly `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` failed ("expected not to be: null"), the other 24 passed. Each restored, hashes equal HEAD blobs, `app/src/main` diff empty (`I3-checks.txt`). Row D17: not fired.
- Checks (`I3-checks.txt`): `grep "LOLLIPOP\|sdk = [2x"` over `app/src/test` exit 1 (no match); the AC-8 grep over the service and compat test folders exit 1; no `@Ignore` or `Assume` in the two files; `git diff --stat HEAD -- app/src/test` two files; working-tree count 755.
- **Rename table (AC-4, "listed by name"; none deleted, none added by this increment):**

| Old name | New name | Old `@Config` | New |
|----------|----------|---------------|-----|
| `SmsReceiverTest.testOnReceivePreKitKat` | `testOnReceive_belowQ` | `LOLLIPOP` | `N` |
| `SmsReceiverTest.testOnReceiveKitKat` | `testOnReceive_q` | `LOLLIPOP` | `Q` |
| `SmsReceiverTest.testIsSmsBackupDefaultSmsAppPreKitKat` | `testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage` | `LOLLIPOP` | `N` |
| `SmsReceiverTest.testIsSmsBackupDefaultSmsAppKitKat` | `testIsSmsBackupDefaultSmsApp_q_usesRoleManager` | `LOLLIPOP` | `Q` |
| `WorkManagerSchedulerTest.inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` | `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` | `[23]` | `[24]` |

- **Must hold (S1-I3):** "green on 4.15.1; count unchanged" - held.

### I4 - The floor and the two permissions (AC-5, AC-6; evidence `I4-*`)

I3 commit: `853483e295b890871a3ce313fffc9d7c4a8e4c4f` ("U-068 S1-I3: retarget the five tests below SDK 24 to API 24 and Q").

- Edits E4.1 to E4.4 (plan 3.5): `app/build.gradle` `:24` `minSdkVersion 24`, the comments at `:153` and `:160`; `AndroidManifest.xml` `:38-51` removed (fourteen lines, one hunk `@@ -38,14 +37,0 @@`, 0 added lines).
- Gate G4 (G3 + `assembleRelease`, `lintVitalRelease` inside it): `exit=0`, `BUILD SUCCESSFUL in 7m 6s`; `files=80 tests=755 failures=0 errors=0 skipped=2`; the five retargeted tests PASS again at minSdk 24 (`I4-cases.txt`, AC-4 (f)). Rows D30, D31: not fired.
- Checks (`I4-checks.txt`): `app/build.gradle` 21/24/25/26 = 35 / `minSdkVersion 24` / 35 / 1602; `grep "minSdk 21\|SDK 21" app/build.gradle` exit 1; the comments-stripped diff against HEAD is line 24 only; 6 changed lines (3 pairs); `grep maxSdkVersion` in the manifest exit 1; `k9mail-vendored/build.gradle:54` still `minSdkVersion 21` (D4b); `git grep "USE_CREDENTIALS\|WRITE_EXTERNAL_STORAGE" -- app/src k9mail-vendored/src` lists only `TokenRefresher.java:97` (a comment, left as history). Invariants in the packaged manifests: `SystemForegroundService` `foregroundServiceType=0x1` (dataSync) in both APKs; no `InitializationProvider` anywhere; `BackupBroadcastReceiver` `exported=true` with action `com.zegoggles.smssync.BACKUP`; the `SmsBroadcastReceiver` entry is untouched (no hunk at or after the old `:146`).
- **Manifest comparison I0 vs I4** (`I4-manifest-diffs.txt`), merged (debug and release) and packaged (both, normalised): `minSdkVersion` 21 -> 24; one deletion hunk with exactly the two comment blocks, the two `uses-permission ... maxSdkVersion` elements and one blank line; and the **declared difference** `android:extractNativeLibs` "true" -> "false" (N3, row D24, ruling P-2: `so=0` in both APKs before and after, so nothing is extracted and there is no runtime effect; no build file was changed to suppress it). Nothing else differs (row D25: not fired).
- **APK comparison I0 vs I4** (`I4-apk-diffs.txt`, rows D26 to D29): kept-name set (48) and `configuration.txt` identical; `so=0` both; class-file majors 61/61/52 unchanged. Declared differences (ruling P-4, row D28): debug `classes*.dex` 17 -> 23 (release stays 1); nine `res/` entries removed from each APK, all base-folder duplicates of `-v21/-v22/-v23` variants (qualifier-normalised debug list differs only by the items below). **Three differences the plan did not name, each explained by the floor** (flagged for the orchestrator; I judged them explained, not undeclared-and-unexplained, so I did not stop): (a) debug APK has no `META-INF/CERT.RSA`, `CERT.SF`, `MANIFEST.MF` - AGP stops writing the v1 JAR signature at minSdk 24 or higher; `apksigner verify -v` of the I4 debug APK: Verifies, v1 false, v2 true (the unsigned release APK never had them); (b) release `META-INF/services/r4.b0` -> `q4.b0` and `w4.t` -> `v4.t` - the two kotlinx.coroutines service interfaces (`CoroutineExceptionHandler`, `MainDispatcherFactory`) under shifted R8 obfuscated names (the R8 output changed with the min-api: `mapping.txt` 95,470 -> 91,897 lines; kept names and configuration unchanged); (c) release `res/u01.xml` gone - the shortened path of the collapsed `layout-v22` duplicate (nine removed `res/` entries in each APK).
- Bytecode "before" copies for I5 saved (six classes, work folder).
- **Must hold (S1-I4):** "merged-manifest diff shows only `minSdkVersion="24"` and the two removals" - held, with the declared `extractNativeLibs` difference (D24).

### I5 - The seven dead sites and the stale text (AC-7, AC-8, AC-9) (evidence `I5-*`; interrupted once, then completed)

I4 commit: `c8e98a7f98e5c28593bddbf207c6d19ef5f1ed52` ("U-068 S1-I4: minSdk 24; drop the two permissions capped below API 24").

**Stop (row D50, constraint 3):** after the I5 gate and most of its checks, the endpoint-security product (SentinelOne, event "Malware detected", file `diff.exe` of the Git for Windows `usr/bin` folder) quarantined `diff.exe`; `/usr/bin/diff.exe` is gone and every `diff` call now fails with "command not found". The same product has already killed or quarantined Git tools on this machine. I did not restore the file, did not substitute another tool for the missing comparison, and ran no further shell command after confirming the event. Everything below was produced before the stop.

Done in I5:
- **Step 1a, test C-1 (ruling P-5):** `app/src/main` equalled HEAD (`main-clean-exit=0`). C1.a (imports) and C1.b (`testEnableThenDisable_togglesReceiverComponent`) applied exactly as plan 6.1; first run `exit=0`, five PASS, `cases=5`, 5 lines with `@Test` (`I5-c1-first-run.txt`). **M-C1** (`COMPONENT_ENABLED_STATE_DISABLED` -> `..._ENABLED` in `disable`): exactly `testEnableThenDisable_togglesReceiverComponent` failed (Truth "expected: 2 / but was : 1", type A), the four retargeted cases passed; restored, hash equals the HEAD blob `30b78e48...`, `app/src/main` diff empty (`mutation-log.md`, `mutations/M-C1-*`). Rows D35b, D17: not fired.
- **Step 2, E5.1 to E5.18** applied exactly as plan 3.6 (Dialogs, CalendarAccessorPost40 x3, SmsReceiver x3, MainActivity x2, SmsDefaultRoleHelper.kt, WorkManagerScheduler.kt x5, BackupScheduler.java, App.java). Edit order, not plan order, was used for the edits within a file; every `old_string` matched exactly once (no row D4).
- **Step 3, gate G4:** `exit=0`, `BUILD SUCCESSFUL in 8m 10s`; `files=80 tests=756 failures=0 errors=0 skipped=2` = C0 + 3. No compile error (row D34: not fired). Coverage (`I5-jacoco-packages.txt`): **`compat` 18/71 = 25.4 %, 3 spare lines** (as planned, with C-1: `disable`'s three lines covered), `receiver` 39/58 = 67.2 %, **`scheduler` 137/185 = 74.1 %** (as planned), `calendar` 37/50, `activity` 198/1008 (floor 0.10). Class inventory equals I0 package by package (`packages equal: True`, no class-count difference; `I5-checks.txt`). The five retargeted tests, the API 24 test, the three INV-4 API 29 tests, the U-054 test and the five `MainActivityRestoreTest` cases PASS (`I5-cases.txt`). Rows D35, D36: not fired.
- **AC-4 (g)** (`I5-jacoco-lines.txt`): in `SmsReceiver.java` the role-manager line (`RoleManager roleManager = ...`, ci=5) and the package line (`return context.getPackageName().equals(Sms.getDefaultSmsPackage(context));`, ci=6) are both covered. The only tests between API 24 and 28 are the two `N` cases (the API 24 scheduler test never calls `SmsReceiver`), so the package line is the `N` tests' doing; M-R1 and M-R2 (I3) showed which pair runs which branch. Row D38: not fired.
- **Step 4, greps** (`I5-greps.txt`): AC-7 grep 14 lines, 12 without imports, exactly the plan's list (`App.java` O x2; `MainActivity.java` Q, TIRAMISU; `SmsDefaultRoleHelper.kt` Q x3; `ThemeActivity.java` 26 x2; `SmsReceiver.java` Q; `BackupWorker.kt` Q; `RestoreWorker.kt` Q; the two remaining imports are `RequiresApi` in `App.java` and `TargetApi` in `ThemeActivity.java`); AC-8 grep exactly one line, `compat/package-info.java:2`; no `TargetApi` left in the three files and no word `Build` left in `Dialogs.java`, `CalendarAccessorPost40.java`, `WorkManagerScheduler.kt` (AC-7 (b)); IC-1 caller list printed (callers `MainActivity.java` `:242`, `:312`, `:427`, `SmsDefaultRoleHelper.kt` `:74`, `:149`, `SmsReceiver.onReceive` `:35`, `App.java:382`, the tests; all compile unchanged). Rows D32, D33: not fired.
- **Step 5 checks** (`I5-checks.txt`): `git diff --name-only HEAD -- app/src/main` lists the eight files (nine with I4's manifest); `git diff -w -U0` of `SmsDefaultRoleHelper.kt` has exactly the five expected `-` lines and no `+` line; the must-not-change files (`SmsBroadcastReceiver.java`, `BackupBroadcastReceiver.java`, `BackupTriggerWorker.kt`, the two workers, `k9mail-vendored`, `robolectric.properties`) show an empty diff; `git diff --stat HEAD -- app/src/test` is one file (`SmsReceiverTest.java`, 15 insertions); working-tree count 756 = C0 + 3 (`--untracked`).
- **Step 7, bytecode** (`I5-bytecode.txt`): the normalised `javap -c -p` of `CalendarAccessorPost40` and `MainActivity` is identical (S-2, S-5); `SmsReceiver` loses only the `SDK_INT`/19 compare and the `iconst_0; ireturn`; `Dialogs$About$1` only the `SDK_INT`/14 compare; `SmsDefaultRoleHelper` only the `SDK_INT`/19 compare and the pre-KitKat call; `WorkManagerScheduler` only the `SDK_INT`/24 compare with its `Log.d` and `return null`, and the final log constant loses " (API 24+)". In each, the remaining instructions are the same with shifted offsets (as plan 5.4). Row D37: not fired. N11: not confirmed (no R8 release note was read).

**Resumed (2026-11-21T13:03Z) after the owner's decision (see "Interruption"):** the tree was verified against this record before continuing (`git diff --stat HEAD -- app`: eight production files and `SmsReceiverTest.java`, 46 insertions, 55 deletions; `BackupBroadcastReceiver.java` still equals its blob; no change since the gate), so the I5 gate was not re-run.
- **Step 6, `I5-build-diffs.txt` (I4 vs I5, via `git diff --no-index`):** merged manifests (debug, release) and packaged manifests (normalised) identical; kept names identical; `configuration.txt` identical; APK entry lists identical (raw and qualifier-normalised, both APKs); javap majors identical. Only `I5-apk.txt` differs: debug size 6,408,587 -> 6,459,683, release size 1,937,824 -> 1,937,712, both sha256, and `mapping-lines` 91,897 -> 91,896 (one line fewer in the R8 mapping, from the removed log constant); entries, dex counts (23 and 1) and `so=0` are the same. Rows D26 to D29: not fired.
- **Must hold (S1-I5):** "AC-6 grep passes; coverage floors hold (scheduler measured)" - held (AC-7 grep as listed, `scheduler` 137/185 = 74.1 %, `compat` 18/71, every package at or above its floor because the gate's `jacocoTestCoverageVerification` passed).

<!-- LOG-ENTRIES-END -->
