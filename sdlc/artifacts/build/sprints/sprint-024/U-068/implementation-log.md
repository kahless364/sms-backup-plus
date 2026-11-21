---
artifact_type: implementation-log
story_id: "U-068"
gate_id: "implementation"
checkpoint: "implementation"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-11-21T11:42:09Z"
iteration: 1
base_commit: "4300ffa98370b77dcc4dd8d12142079aada8313a"
files_changed: 0
files_created: 0
tests_added: 3
tests_passing: 0
---

# Implementation Log: U-068 - Raise minSdk to 24 and remove what the floor makes dead

Plan: `sdlc/artifacts/build/sprints/sprint-024/U-068/plan.md` (iteration 2), followed step by step. Evidence is under `sdlc/artifacts/build/sprints/sprint-024/U-068/evidence/`. Raw logs, saved JaCoCo XML copies, pristine copies and commit messages were kept in a work folder outside the repository. One local commit per increment, explicit paths, nothing pushed. No emulator or device was used (I8 is the orchestrator's).

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

<!-- LOG-ENTRIES-END -->
