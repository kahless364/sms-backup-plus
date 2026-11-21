---
artifact_type: review-code
story_id: "U-068"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-11-21"
blockers: 0
warnings: 0
---

# Code Review: U-068 (raise minSdk to 24 and remove what the floor makes dead)

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | Met. The reworded text states only what CNTR-MODERNIZATION-004 v3 states; no port signature, `@Nullable` or invariant changed; CNTR-MODERNIZATION-005 untouched (test added only). |
| Behaviour at API 24 and above | Unchanged. All seven removed sites were conditions that are constant at the floor; the code that remains is what ran at 24+ before. |
| Test coverage | Met. Three tests added (753 to 756), five retargeted, none deleted, no `@Ignore` or `Assume` added. Seven mutations, each caught by the test it was written for. |
| Code quality | Good. Minimal, mechanical diff; findings are Low or Info only. |

## Verdict: PASS

No Critical, High or Medium finding. Nothing must be fixed in this cycle. Two Low findings and several Info items are listed with their disposition.

## What I read and ran

- Read from the commits, not the working tree: `git diff 4300ffa9 0d490997` over `app`, `build.gradle`, `CHANGES`, `DEVELOPMENT.md`, `.github` and `sdlc/artifacts/engagement`; `git show` of each of the seven increment commits with `--stat`; the new test class and the two retargeted test files whole; `SmsReceiver.java`, `SmsDefaultRoleHelper.kt` and `BackupBroadcastReceiver.java` at the last commit; the whole story, `implementation-log.md`, `evidence/mutation-log.md`, `I4-apk-diffs.txt`, `I5-build-diffs.txt`, `I7-final-checks.txt`, `I7-scan.txt`.
- Ran only read-only `git`, `git grep` and a few shell reads: the authoritative `@Test` count at both ends (753 and 756), the AC-7 and AC-8 greps at the last commit, the lint-baseline removal listing and recount, the must-not-change `--stat`, `git diff --name-only`. No Gradle, no external `diff` or `cmp`, no edits to source or tests.
- Plan, plan-review, contract-verification and depth-check were consulted for the mutation table, the commit table (plan lines 1237-1243) and the U-069 byte-identity rule (L18/L20), not re-read end to end.

## 1. The seven removed sites (REQ-015 AC-6, D4 table S)

Each reduction was checked as a constant-fold at API 24 and above (`SDK_INT` is at least 24, so every `>= ICE_CREAM_SANDWICH`, `>= KITKAT`, `>= N` is true and every `< N` is false).

| Site | Result | Remaining code vs. what ran at 24+ |
|------|--------|------------------------------------|
| S-1 `Dialogs.java:169` | Correct | `scrollPosition > 0 && ABOUT_HTML.equals(url)`; the `Build` import is removed and `Build` has no other use in the file (grep of the last commit: no hit). The two-line `&&` chain became one line, same predicate. |
| S-2 `CalendarAccessorPost40.java` | Correct | `@TargetApi` and imports `TargetApi`, `Build` gone; no other use. Class name kept. |
| S-3/S-4 `SmsReceiver.java:45-50` | Correct | `if (SDK_INT >= Q) { role check } else { getPackageName().equals(Sms.getDefaultSmsPackage(context)) }`. The API 24-28 BUG-009 branch is kept; the dead `else { return false; }` is gone. `Build` and `Sms` imports are still used. |
| S-5 `MainActivity.java` | Correct | `@TargetApi(KITKAT)` and its import gone; `Build` stays (`:241`, `:515`). |
| S-6 `SmsDefaultRoleHelper.kt:74-97` | Correct | The outer `if (SDK_INT >= KITKAT)` and the pre-KitKat `else` are gone; the body is the old body de-indented one level. I read the whole hunk: the `isSmsBackupDefaultSmsApp` call, the `>= Q` branch, the legacy pre-Q path (`getDefaultSmsPackage`, `setSmsDefaultPackage`, dialog or request, `error_no_sms_default_package` toast) are token-for-token the old inner text. Imports (`Build`, `Telephony`, `Log`, `TextUtils`, `Toast`) all still used. |
| S-7 `WorkManagerScheduler.kt` | Correct | The `SDK_INT < N` branch (log plus `return null`) is gone and the `android.os.Build` import with it; no other `Build` use in the file. Return type stays `ScheduledJob?` because the encryption-degraded path (`:219-222`) still returns null; the U-054 test is untouched. |

Remaining level comparisons: the AC-7 grep over `app/src/main` at the last commit returns 14 lines, 12 without imports, each naming a level of 25 or higher (`App.java` 246 and 350; `MainActivity.java` 241 and 515; `SmsDefaultRoleHelper.kt` 76, 106, 130; `ThemeActivity.java` 20 and 26; `SmsReceiver.java` 45; `BackupWorker.kt` 534; `RestoreWorker.kt` 707) and two imports (`App.java:35`, `ThemeActivity.java:3`). No comparison against level 24 remains. The 12 sites at 25+ have no hunk.

Empty diffs `4300ffa9..0d490997`: `SmsBroadcastReceiver.java`, `BackupBroadcastReceiver.java`, `BackupTriggerWorker.kt`, `BackupWorker.kt`, `RestoreWorker.kt` (the whole `receiver`, `worker` and `service` folders under `app/src/main/java/com/zegoggles/smssync` show nothing), `k9mail-vendored/`, `app/lint.xml`, `app/proguard-rules.pro`, `gradle/`, `gradle.properties`, `settings.gradle`, `.github/workflows/`, `app/src/test/resources/`. `git diff --name-only ... -- app/src/main` lists exactly the nine files of AC-9.

## 2. Manifest and build files

- `AndroidManifest.xml`: one hunk, 14 lines removed, none added (`@@ -35,20 +35,6 @@` at the context level): the `USE_CREDENTIALS` block (two comment lines, element, blank) and the `WRITE_EXTERNAL_STORAGE` block (five comment lines, group comment, element, blank). The blank line before `<!-- Allows the app to create network sockets` is preserved, so there is no double blank. No `maxSdkVersion` remains. No hunk near the `SmsBroadcastReceiver` entry, `SystemForegroundService` or `BackupBroadcastReceiver`.
- `app/build.gradle`: exactly three changed lines: `minSdkVersion 24`, the WorkManager comment, and the Robolectric trailing comment. `compileSdkVersion`, `targetSdkVersion`, `versionCode` and every JaCoCo rule are context only. The Robolectric comment ("ASM 9.8 reads Java 25 class files. U-068: no test is configured below the minSdk of 24.") is true: no `@Config` below 24 remains in `app/src/test` and the default is `sdk=29`; `k9mail-vendored/src/test` has no `@Config` at all. `grep "minSdk 21\|SDK 21"` on the file is empty.
- Root `build.gradle`: one line, `reports.junitXml.required = true`; the `html` line is context. Nothing else in any build file changed.

## 3. Tests

**`BackupBroadcastReceiverTest` (2 tests).** Real `Preferences` over the default shared preferences, key `third_party_integration`, set with `commit()`; the test also asserts the precondition (`isAllow3rdPartyIntegration()` true or false) before acting, so a changed default would fail loudly rather than pass vacuously. The `getScheduler(Context)` seam is overridden in an anonymous subclass of the production receiver. "On": `times(1)` plus `verifyNoMoreInteractions`; "off": `verifyNoInteractions`. Isolation: Robolectric rebuilds the application and shared preferences per test, and the "on" test's write is not visible to the "off" test (the "off" test's own precondition assertion would catch a leak). M-B1 (`true ||` in the gate) fails exactly the "off" test (`NoInteractionsWanted`); M-B2 (call guarded by `if (false)`) fails exactly the "on" test (`WantedButNotInvoked`). Each is the one test I derive from reading the code, and each has a restore line with a hash equal to the HEAD blob. `receiver` 29/58 to 39/58, ten lines of headroom.

**Five retargeted tests.** `SmsReceiverTest`: `testOnReceive_belowQ` and `testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage` at `Build.VERSION_CODES.N`; `testOnReceive_q` and `testIsSmsBackupDefaultSmsApp_q_usesRoleManager` at `Q`. Bodies and assertions are character-identical to the old ones (no throw; `isFalse()`); nothing weakened. Because both outcomes are `false`, the branch each pair exercises is proven by mutation, not by result: M-R1 (role branch throws) fails exactly the two `_q` tests and M-R2 (package branch throws) exactly the two `_belowQ` tests; the I5 JaCoCo lines show both branch lines covered. This is the AC-4 (g) evidence and it is sound. The class comment now says there are two surviving branches, covered by `N` and `Q`, with no "LOLLIPOP", no `sdk = [2x`, no "KitKat".

`WorkManagerSchedulerTest.kt` `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` (`@Config(sdk = [24])` on the method; the class stays 29): asserts non-null result, `tag == CONTENT_TRIGGER_UNIQUE_NAME`, a non-empty unique-work list, the `content://sms` trigger present and `isTriggeredForDescendants` true. This is stronger than the old `isNull()` and carries the CR-015 Amendment 1 / DES-005 D-5 addition. M-W1 (SMS trigger not for descendants) fails this test and the API 29 descendants test; M-W2 (`< N` changed to `<= N`, the dead branch put on the floor) fails exactly this test and nothing else. So the API 24 test is the one that catches the boundary mutant, as required. The INV-4 tests at 29 and the U-054 test are not in the diff; the BUG-023 comment at `:537-538` is not in the diff.

**C-1 `SmsReceiverTest.testEnableThenDisable_togglesReceiverComponent`** (ruling P-5). Reads the component state through `PackageManager.getComponentEnabledSetting` after `enable` and after `disable`, asserting `ENABLED` then `DISABLED`. It proves what it claims (the two static methods change the component's state) and is not coverage padding without an assertion. M-C1 (`disable` writes `ENABLED`) fails exactly this test on the second assertion (`expected 2 but was 1`). Isolation is the Robolectric default (class default SDK 29, fresh package manager per test). `compat` ends 18/71 with 3 spare lines.

No `@Ignore` or `Assume` is added (`git diff` of `app/src/test`: no such `+` line; the six `@Ignore` text hits at the last commit are the same pre-existing ones, 2 on code).

## 4. Reworded comments, KDoc and log text

- `WorkManagerScheduler.kt` class KDoc (`:57-60`): "`addContentUriTrigger` with `triggerForDescendants = true`, on every supported level (minSdk 24; no version branch, CNTR-MODERNIZATION-004 v3)". True against the code (`:225`, `:229` have no branch) and against v3 INV-4. The broken KDoc link `[triggerForDescendants = true]` became code formatting; harmless improvement.
- Method KDoc (`:207-209`) likewise. The log text drops " (API 24+)"; no test asserts the string (bytecode evidence shows only that constant changed).
- `BackupScheduler.java:74-78`: "no version condition ... INV-4"; `@return` now says null "if the adapter does not schedule it (today: when encryption is degraded, U-054)". True: the degraded path is the only null return. `@Nullable` kept.
- `App.java:379-380`: "enqueues the content-URI trigger with no version branch (minSdk 24, CNTR-MODERNIZATION-004 v3)". True.
- No new sentence cites `SmsBroadcastReceiver` as INV-4 evidence or calls the broadcast path a fallback. The AC-8 grep at the last commit returns the one allowed hit, `compat/package-info.java:2`. The `scheduleIncoming()` comments (BUG-023) are untouched.
- **U-069 byte-identity.** The two sentences the U-069 plan names are the line "[BackupTriggerWorker] enqueues a delayed [BackupWorker] rather than backing up directly." (old `:61`) and the lines "the triggered [BackupTriggerWorker] enqueues a delayed / incoming backup rather than backing up directly." (old `:212-213`). Both appear only as unchanged context lines in the diff. The sentence that holds the first one starts on the preceding line, which this story rewrote, but the clause "The two-stage debounce is preserved: the triggered" is carried over verbatim, now on the line that begins with the new v3 citation. U-069 quotes post-U-068 `:60`, which is the unchanged line, so its anchor holds.

## 5. Lint baseline

`git diff --numstat` is `0 121`. The removed `id/file/line` triples are exactly the 11 expected: `Deprecated` `BackupJobs.java:68`; `ObsoleteSdkInt` `CalendarAccessorPost40.java:23`, `Dialogs.java:169`, `MainActivity.java:364` and `:368`, `SmsReceiver.java:31` and `:50`, `ServiceBase.java:209` and `:225`, `SmsRestoreService.java:67` and `:165`. Recount by `grep -c '^ *<issue *$'`: 37 at the start commit, 26 at the last. `app/lint.xml` empty diff. The 8 entries that regeneration at lint 9.0.1 would not reproduce (three `NonConstantResourceId`, five `GradleDependency`) were kept under ruling P-3; the "not found" count falls 19 to 8 = 19 - 11, which agrees. Scratch copy not committed (`rm -rf` recorded).

## 6. Documents

- `CHANGES:3`: "Minimum Android version is now 7.0 (API 24); Android 5.0, 5.1 and 6.0 (API 21 to 23) are no longer supported". Replaces the stale ICS line; no store, release or rollout wording.
- `DEVELOPMENT.md`: one line, `:58` `24`; the stale compile/target 29 lines are context and ruled out of scope.
- `sdlc/artifacts/engagement/code-location.md:28`: `24 (Android 7.0)`; the only change in that file.
- `.github/ISSUE_TEMPLATE/500_bug_report.yml`: the three lines for 6.0, 5.1, 5.0 removed; the 4.x entries stay by ruling. No publishing wording anywhere.

## 7. The three package-content differences the plan did not name

| # | Difference | My judgement |
|---|-----------|--------------|
| (a) | Debug APK has no `META-INF/CERT.RSA`, `CERT.SF`, `MANIFEST.MF` | **Explained, and I agree.** AGP enables the v1 (JAR) signature only while the minSdk is below 24, because the v2 scheme exists from API 24. The evidence is a direct check: `apksigner verify -v` on the I4 debug APK reports verifies, v1 false, v2 true. The release APK is unsigned in both builds, so it never had these entries. Consequence for the owner: none on a device at 24 or above; the debug build is now v2-only, which the API 36 install in IC-3 accepts. |
| (b) | Release `META-INF/services/r4.b0` to `q4.b0` and `w4.t` to `v4.t` | **Explained as a mechanism; the identity of the two files is inferred, not shown.** R8 rewrites service-file names to the obfuscated interface names, and a min-api change alters desugaring and the class set, which shifts obfuscated name assignment; the mapping file shrinking (95,470 to 91,897 lines) while kept names (48) and `configuration.txt` are identical supports that. The claim that the two are `CoroutineExceptionHandler` and `MainDispatcherFactory` rests on the debug APK carrying exactly those two kotlinx entries and the release APK carrying exactly two non-charset service entries; no `mapping.txt` lookup is recorded. See L-1. |
| (c) | Release `res/u01.xml` gone | **Explained by count and by AAPT2 behaviour, identification inferred.** At minSdk 24, AAPT2 collapses a `-v21/-v22/-v23` variant into the base path and drops the base duplicate. The debug list shows nine `res/` entries removed (`color-v21` x1, `color` x7, `layout-v22` x1) and the qualifier-normalised debug diff is then empty for `res/`; the release diff has nine removed too, with `u01.xml` standing in for the single `layout-v22` item, because release resource paths are shortened. The shortened names in the release list are not sequential (`u0.xml`, `u01.xml`, `u2.xml`), which is why removing one does not rename the others. The nine-for-nine match is good evidence; a resource-path map was not saved. See L-1. |

None is an unexplained difference in the sense of the P-2/P-4 ruling (nothing that would change behaviour at 24+ and nothing that is not a floor effect). The three declared ones (`extractNativeLibs` true to false with `so=0`; debug dex 17 to 23 at D8 min-api 24; nine collapsed `res/` entries) are recorded and consistent with AGP behaviour.

## 8. Consistency of the log with the evidence and of the commits with AC-16

- Counts: `git grep` at `4300ffa9` gives 753, at `0d490997` gives 756; added by name are the two receiver tests and C-1; Gradle totals `files=80 tests=756 failures=0 errors=0 skipped=2` (I7), start 79 files and 753; skipped 2 at both ends.
- Coverage (I7 evidence): `receiver` 39/58, `compat` 18/71, `scheduler` 137/185; start figures 29/58, 16/73, 140/188; every rule satisfied (the verification task passed in the final gate). The `scheduler` comment figure in `app/build.gradle` (102/164) is stale; that is a comment outside this story (the log hands it over).
- Lint 37 to 26 (verified above). Seven mutations in `mutation-log.md`: M-B1, M-B2, M-R1, M-R2, M-W1, M-W2, M-C1; each has a write-ahead entry, a result, a restore line with a hash equal to the HEAD blob.
- Commits: `git log --grep='^U-068 S1-'` gives I1 to I7 in order; subjects equal the plan's table (lines 1237-1243). Each `--stat` is: I1 `build.gradle` plus evidence plus log; I2 the new test plus evidence; I3 the two test files plus evidence; I4 `app/build.gradle` plus the manifest plus evidence; I5 eight production files plus `SmsReceiverTest.java` (C-1) plus evidence; I6 `app/lint-baseline.xml` plus evidence; I7 `CHANGES`, `DEVELOPMENT.md`, the issue template, `code-location.md` plus evidence. Nothing outside the story. I2 precedes every other code change; I3 precedes I4; I4 precedes I5. The interleaved commits are documents-only commits by the orchestrator. One thing a reader of `git diff --name-only 4300ffa9 0d490997` sees that is not this story's: `sdlc/artifacts/stories/BUG-024-...md`, from an orchestrator commit.
- Runtime Claims: the log declares the inventory empty with a reason. The story makes no runtime-behaviour acceptance criterion other than the device checks IC-3 and IC-4, which the story assigns to the orchestrator (IC-4 "not run: needs the product owner's decision"); that is consistent. No probe is claimed, so none can have written into the sibling repository.

## 9. The substituted comparison commands after the interruption

The plan's `diff A B` was replaced by `git -c core.autocrlf=false diff --no-index --no-color -- A B | tail -n +5`, with the exit status printed as `git-diff-no-index-exit=N`. They prove the same thing, for these reasons: (1) `git diff --no-index` exits 1 when the files differ and 0 when they are byte-identical, the same contract as `diff`; (2) the inputs are small text extracts (manifests, entry lists, kept names, configuration), for which a unified line comparison is what `diff` was doing; `core.autocrlf=false` prevents any conversion, and no `--strip-trailing-cr` was involved in the plan; (3) the header cut of four lines is correct for the plain two-path form; (4) the check is falsifiable in the evidence itself: `I5-build-diffs.txt` shows exit 1 with real `-`/`+` lines for the APK summary (size, hash and mapping-line count differ) while the other nine comparisons print exit 0, and `I7-build-diffs.txt` shows the I0-to-I7 differences with exit 1. Two caveats, both small: the comparisons made before the quarantine used the external tool and were not repeated, which is stated in the log; and the exit code in the pipeline must be captured from the git command and not from `tail` (the evidence prints it from git, since it shows 1 where lines follow). I judge the substitution equivalent. I also did not call `diff` or `cmp` myself.

## Findings

| ID | Severity | Where | Finding | Fix in this cycle? |
|----|----------|-------|---------|--------------------|
| L-1 | Low | `sdlc/artifacts/build/sprints/sprint-024/U-068/evidence/I4-apk-diffs.txt:93-100` | Explanations (b) and (c) of the unnamed package differences are plausible and consistent with R8 and AAPT2 behaviour, but the identity of the two renamed service files and of `u01.xml` is inferred from positions and counts, not from a recorded `mapping.txt` or resource-path lookup. Not a blocker: either way the difference is a name or path, with kept names and configuration identical and no `.so`. | No. QA may confirm with a read-only lookup in the release `mapping.txt` if it wants closure. |
| L-2 | Low | `app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt:34` | The reworded class-header line is about 150 characters, much longer than its neighbours; style only. | No. Defer: no behaviour, and U-069 edits this file next. |
| I-1 | Info | `BackupBroadcastReceiverTest.java` | The action guard (`BACKUP_ACTION.equals(intent.getAction())`) has no test for a different action. AC-3 asks for two tests (opt-in on and off) and the story forbids more; the new tests also rely on the guard being true. | No. Backlog candidate. |
| I-2 | Info | `SmsReceiverTest.java` | The `>= Q` threshold itself has no boundary mutant test (a test at 28 and one at 29 would). Not required by the story; pre-existing. | No. |
| I-3 | Info | `DEVELOPMENT.md:56-57`, `README.md:38`, `compat/package-info.java:2`, `WindowInsetsUtil*` comments, issue template 4.x entries | Stale statements about levels left by ruling (Scope Concern 3). | No. Follow-up already named. |
| I-4 | Info | `app/build.gradle` (JaCoCo comment for `scheduler`) | The comment figure 102/164 is stale (measured 137/185). The story forbids touching anything else in the file. | No. Handed over in the log. |
| I-5 | Info | `implementation-log.md` frontmatter | `files_changed: 20` counts differently from the nine-main-file and AC-12 views; the log text is consistent with git. | No. |

## Patterns Verified
- [x] Follows existing code patterns
- [x] Error handling unchanged (the degraded path still returns null with no work enqueued)
- [x] Tests cover new and changed functionality, each shown to fail under its mutation
- [x] No hardcoded values that should be configurable
- [x] No unnecessary complexity (the diff is deletions, de-indentation, comments and one build line)

## Integration Verified
- [x] No new production component; changed code is reached from `App.rescheduleJobs`, `MainActivity`, `SmsDefaultRoleHelper` and the manifest `SmsReceiver`, unchanged callers
- [x] Function signatures unchanged (`isSmsBackupDefaultSmsApp(Context)`, `startRestore(...)`, `scheduleContentTrigger(): ScheduledJob?`); no caller edited
- [x] No registry or dispatch map touched; the new test class is run by the unit-test task (80 files at the final gate)
- [x] No dead code introduced; remaining comparisons are all at 25 or higher

## Regression Check (replacement / removal)
- [x] Capabilities Inventory present in the log
- [x] Every RETAINED item found in the new code (S-4 package branch `SmsReceiver.java:49`; S-6 body `SmsDefaultRoleHelper.kt:74-97`; scheduler body)
- [x] Every INTENTIONALLY REMOVED item cites a story AC (S-1 to S-7, AC-6, AC-8)
- [x] Deleted lines in the diff are all accounted for in the inventory

## Contract Verification
- [x] CNTR-MODERNIZATION-004 v3: no signature, `@Nullable`, `BackupType` or invariant changed; INV-4's boundary assertion is at API 24; the broadcast path is described nowhere as the fallback
- [x] CNTR-MODERNIZATION-005: `BackupBroadcastReceiver.java` and its manifest entry have an empty diff; only a test is added
- [x] Manifest invariants (`SystemForegroundService` dataSync, no `InitializationProvider`, exported `BackupBroadcastReceiver`) untouched by the manifest hunk

## Runtime Claims Verified
- [x] Inventory read: empty by declaration, with the reason stated; consistent with the story (device checks belong to the orchestrator)
- [x] No embedded probe exists, so no probe can have written to the sibling repository
- [x] UNVERIFIED (escape-hatch) claims: None
