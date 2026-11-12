---
artifact_type: qa-results
story_id: "U-061"
gate_id: "qa-validation"
checkpoint: "quality-gate"
verdict: "PASS"                        # PASS | FAIL
agent: "QA Analyst"
timestamp: "2026-11-12"
ac_total: 26                           # AC-1..AC-12 plus IC-1..IC-14
ac_passed: 26
ac_failed: 0
tests_run: 682
tests_passed: 680                      # 682 run, 0 failures, 0 errors, 2 skipped (@Ignore, pre-existing)
---

# QA Validation: U-061

## Verdict: PASS

## Summary

I re-derived almost every criterion myself at HEAD `7dfd651b`. That covers three debug gate runs, a release build with R8 forced to re-execute, my own baseline build of `e0c51d42` in a scratch copy, four manifest diffs, the R8 mapping, kept-name and configuration checks, the dependency trees, the verification-metadata writer and the source greps. All 12 ACs and 14 ICs are met. Six of them are met with a recorded deviation that the product owner or orchestrator accepted (IC-1, IC-2, IC-4, IC-8 (2) and (4), IC-11), and each one is classified below. IC-11 (device), the IC-3 isolated run and the per-increment gate history (IC-2) rest on committed evidence, as stated per criterion. QA summary evidence: `sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/qa-evidence-summary.txt`.

The working tree had uncommitted changes under `sdlc/` only, both before and after QA. Product paths match HEAD (`git status --short -- . ':!sdlc'` is empty). QA wrote only this file and the `qa-` evidence summary.

## Acceptance Criteria Results

> **Rule**: Every AC marked PASS must cite at least one `file:line` reference in the Evidence column.

How verified: **R** = re-run by QA at HEAD; **E** = committed evidence (file named).

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1 | PASS (R) | `ls app/src/main/java/com/zegoggles/smssync/activity/donation/` gives "No such file or directory". `grep -rniE "billing\|donat" app/src/main app/build.gradle` returns 0 results (rc 1). The same pattern with `git grep` at HEAD also returns 0. All 25 `strings.xml` files have 0 matches, including the 22 that had matches at `e0c51d42` (3 to 11 each). The stated `GooglePlayServices.java` "exception" matches neither term, so the literal result is zero, as the story's scope concern anticipates. `app/build.gradle:43` (reworded comment) contains neither term. |
| AC-2 | PASS (R + E for device) | Items 2-6 are removed: manifest (diff removes only `.activity.donation.DonationActivity`, previously next to `AndroidManifest.xml:128`); `preferences.xml` has no `key="donate"` (grep rc 1, file now ends at line 318); `MainSettings.java:67-72` `onResume()` = `super.onResume(); updateAutoBackupPreferences(); addPreferenceListener(ENABLE_AUTO_BACKUP.key);`; `Keys.DONATE` gone (`Preferences.java:100-101` now `IMAP_SETTINGS`, `BACKUP_SETTINGS_SCREEN`); `Consts.java:34-35` ends at `CALLLOG_PROVIDER` / `}`. Every `android:fragment` in `res/xml` resolves to an existing class (6/6, QA script). Device: `evidence/I8-device.txt` (no Donate node, last title "Advanced settings" = final root `PreferenceScreen` `preferences.xml:223`, `fatal_count=0`). |
| AC-3 | PASS (R) | I rebuilt the merged and packaged manifests for both variants and diffed them against `evidence/before-*`. I confirmed those baselines are genuine by rebuilding `e0c51d42` myself (merged identical, packaged identical after `norm()`). Merged: 0 added / 49 removed in each variant. Packaged: 0 added / 44 removed in each variant. Every removed line belongs to the 11 AC-3 elements and nothing else. `INTERNET` and `ACCESS_NETWORK_STATE` stay (`AndroidManifest.xml:56`, `:61`, count 1 each in both merged manifests). |
| AC-4 | PASS (R) | `:app:dependencies` for `debugRuntimeClasspath` and `releaseRuntimeClasspath` (exit 0) contains 0 `com.android.billingclient`. A dexdump class listing of the debug APK gives 9140 classes, 0 under `com/android/billingclient/` and 0 under `com/zegoggles/smssync/activity/donation/`. `unzip -l` shows 0 matching entries. `apkanalyzer` is not installed in this SDK (no `cmdline-tools`), so dexdump is the class-level listing (deviation 3). The dependency line was removed from the `app/build.gradle` dependency block, near line 131. |
| AC-5 | PASS (R) | `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` (plus `jacocoTestReport`) exit 0 three times at HEAD, once with `clean`. The daemon JVM is `java.version=25.0.5` (init-script line), declared by `gradle/gradle-daemon-jvm.properties`. |
| AC-6 | PASS (R) | The only deleted test files are `DonationActivityTest` (10 `@Test` at `e0c51d42`) and `SkuTest` (5). Every removed assertion line in the test diff comes from those two files. `EnrollmentDialogThemeTest`: 3 `@Test` (`:111`, `:178`, `:221`), import `:9` → `ThemeActivity`, call sites `:118`, `:180`, `:245` = `HostActivity activity = Robolectric.buildActivity(HostActivity.class).create().get();`, nested `public static class HostActivity extends ThemeActivity {}` at `:305`. Both corrected passages (`:114-117`, `:165-168`) cite `ThemeActivity.onCreate()` and say "not from the manifest". That matches `ThemeActivity.java:17-23` (`setTheme(new Preferences(this).getAppTheme())` before `super.onCreate`). No assertion changed and no `@Ignore` was added. |
| AC-7 | PASS (R) | Fresh JUnit XML (written through the init script): `files=73 tests=682 failures=0 errors=0 skipped=2`. The skipped tests are `AppTest.shouldGetVersionName` and `AuthPreferencesTest.u012_robolectricSmoke_encryptedPrefsSecretStore_migrateFromPlaintext`. `git grep` `@Test` count: HEAD 682, `e0c51d42` 697, a drop of exactly 15. Before and after counts are recorded in `implementation-log.md:66`, `:211`, `:371`. |
| AC-8 | PASS (R) | In the build.gradle diff, the JaCoCo block edits are only these: the deleted donation rule and its comment line (`minimum = 0.30`), the Rule 5 note (`app/build.gradle:411`), and one added root-rule comment at `app/build.gradle:399` ("58.6% (17/29)"). The `minimum`/`maximum` lists differ only by the removed `0.30`: 19 → 18 `minimum =` lines, and every other value is unchanged. Root value re-measured by QA: 17/29 = 58.6% ≥ 0.50 (`app/build.gradle:402-406`). The five other values are in the log (`implementation-log.md:347-349`, `:399`) and in the story notes. QA measured the same figures (table under Test Results). |
| AC-9 | PASS (R) | `gradle/verification-metadata.xml` is unchanged from `e0c51d42` to HEAD, and the working tree equals HEAD. `verify-metadata` is `true` and `verify-signatures` is `false` (`gradle/verification-metadata.xml:4-5`). The stale `billing` entry at `:931` remains, which AC-9 allows. The product diff adds no `dependency-verification`/`lenient` line, and no such flag exists in `gradle.properties`, `gradlew` or the build files. I re-ran the writer in a scratch copy of HEAD: exit 0, and the diff is empty after `--strip-trailing-cr`. |
| AC-10 | PASS (R, files) | Done through CR-004 (`sdlc/artifacts/build/change-records/CR-004-req-010-superseded-by-req-018.md:2` `status: implemented`). REQ-MODERNIZATION-010 has `status: superseded`, `change_records: [CR-004]`, and a notes line saying it is superseded by REQ-MODERNIZATION-018, with decision date 2026-11-12, "remove the donation flow" and "AC-1..AC-4 are not applicable". `git diff HEAD` on that file touches frontmatter only, so the body is unedited. REQ-MODERNIZATION-018 frontmatter `change_records` lists CR-003 and CR-004 (lines 14-16). DES-MODERNIZATION-010 has `status: superseded`, `change_records: [CR-004]`, and a SUPERSEDED blockquote naming DES-MODERNIZATION-013 under its H1. These edits are **uncommitted** working-tree changes (see Issues). |
| AC-11 | PASS (R) | `CHANGES:6` adds a two-space bullet under the unreleased `== 1.6.0` heading (`CHANGES:1`). The historical lines, now 24/121/139/169, are unchanged. In `README.md`, the donation paragraph and the "Google Play billing service" bullet are removed, and the "never be a pro / paid version" sentence stays at `README.md:40-41`. The `code-location.md` billing bullet is removed. `git diff --stat e0c51d42..HEAD -- metadata` is empty. |
| AC-12 | PASS (R) | At `e0c51d42`, `Keys.DONATE` was used only at `MainSettings.java:24` (import) and `:172` (`findPreference(DONATE.key)`, a read). The key was declared only on a plain `<Preference>` with an intent (`preferences.xml:320`), which never persists a value. No code iterates `Keys.values()`. `Preferences.reset()` (`Preferences.java:254-259`) is unchanged and removes only the two SMS-default keys. The `app/lint-baseline.xml` diff deletes exactly one `<issue>` (billing 2.1.0 GradleDependency, 11 lines) and no other entry. |

### Integration Criteria

| IC | Status | Evidence |
|----|--------|----------|
| IC-1 | PASS, met with recorded deviation (E, validated by R) | `evidence/before-merged-{debug,release}.xml`, `before-manifest-{debug,release}.txt`, `before-deps-*`, `before-junit-totals.txt`, `before-jacoco-packages.txt`, `before-test-count.txt`, `kept-before.txt`, `before-configuration-rules.txt`, `before-mapping-prefix-count.txt`. Deviation (log I0 deviation 1, evidence hygiene): the raw `mapping.txt`, `configuration.txt` and JaCoCo XML were kept outside the repo because they contain machine paths, and only derived files were committed. The literal wording ("release mapping.txt and configuration.txt" under evidence) is therefore not met. The substance is: my independent `e0c51d42` build reproduces every committed baseline (merged, packaged, kept names, prefix count 965, dependency tree). |
| IC-2 | PASS, met with recorded deviation (E + R) | Commit order and file sets verified with `git diff-tree`: I1 = 1 test file; I2 = manifest, MainSettings, Preferences, preferences.xml; I3 = 5 deletions plus Consts; I4 = 22 strings; I5 = build.gradle and lint-baseline; I6 = build.gradle; I7 = CHANGES and README (code-location under `sdlc/`); I8 = build.gradle (1 comment line); I9 = proguard-rules.pro (approved). Per-increment gates: `evidence/I1..I7-gates.log`, `final-gates.log`, `I9-gates.log`, each exit 0 (derived logs; the task list is not shown in the derived files). Deviation 9: the final gate ran on the identical tree before the I8 commit, not after it. Read literally, "followed by the final gate run" is not met. I agree the intent is met, because the tree was identical, and my own three gate runs at HEAD close the gap. |
| IC-3 | PASS (E + R) | Isolated 3/3 after I1: `evidence/I1-isolated-junit-totals.txt` (`tests=3 failures=0`). `HostActivity` exists only as `EnrollmentDialogThemeTest$HostActivity.class` under test outputs (`evidence/I1-hostactivity-checks.txt`). QA at HEAD: `grep HostActivity app/src/main` rc 1, absent from the debug dex and from the JaCoCo XML, and `EnrollmentDialogThemeTest` 3/3 in the full run. Nested declaration at `EnrollmentDialogThemeTest.java:305`. |
| IC-4 | PASS, met with recorded deviation (R) | I re-ran the writer myself in a `git archive HEAD` scratch copy: exit 0 and an empty diff (no additions, no removals). This matches `evidence/I8-ic4-writer.diff`. Deviation 8: a scratch copy instead of a scratch branch, which is equivalent and avoids touching the repo. |
| IC-5 | PASS (R) | `:app:dependencies` at HEAD compared with `evidence/before-deps-*`, which I validated against my own baseline resolution. Both variants go from 95 to 83 modules, with exactly the 12 expected removed, 0 added and 0 version drift. No `constraints {}` was added (build.gradle diff). |
| IC-6 | PASS (R) | Four diffs, each deletion-only, using the exact `norm()` for the packaged dumps (aapt2 36.0.0). The removed elements are the AC-3 set in all four. My after-state is identical to `evidence/after-merged-*.xml` and `after-manifest-*.txt`. |
| IC-7 | PASS (R) | Checked in both after-merged and after-packaged manifests. `package` and `versionCode` 1602 are unchanged. `BackupBroadcastReceiver` (`AndroidManifest.xml:171`) has `exported="true"`, no `android:permission`, and the `com.zegoggles.smssync.BACKUP` action. `SystemForegroundService` keeps `foregroundServiceType` 0x1. `InitializationProvider` count is 0. `networkSecurityConfig` and `fullBackupContent` are present (`AndroidManifest.xml:96-97`). `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` appears twice. |
| IC-8 | PASS, (2) and (4) met with recorded/approved deviation (R) | Run on a forced R8 re-execution at HEAD. (1) Prefix grep on `mapping.txt` gives 0 (baseline 965). (2) dexdump on the debug APK finds 0 billingclient and 0 donation classes. `apkanalyzer` is not available (deviation 3); dexdump is a strictly finer listing. (3) `comm -23` prints exactly `com.zegoggles.smssync.activity.donation.DonationActivity`, and `comm -13` is empty. (4) Not met as literally worded: the `configuration.txt` diff contains one **added** line (the approved `-dontwarn com.google.errorprone.annotations.**`, `app/proguard-rules.pro:62`). Its removed lines are consumer rules of the removed libraries plus AAPT keeps, not literally "removals listed in AC-3". Read per DES-013 D6 ("removals listed above": AAPT keeps plus the billing, gms and datatransport consumer-rule sections), my own section-aware comparison gives 63 removed, all attributed, 0 unattributed, and 1 added, which is exactly the approved rule. Classification: met with approved deviation (PO-approved I9 rule) and D6 reading. I agree with that reading. |
| IC-9 | PASS (not triggered) (R) | No rule breached in three QA runs. No floor was lowered, no exclusion was added, and no helper was renamed (the `app/build.gradle` diff shows only the permitted edits). |
| IC-10 | PASS (R) | `grep -rn "DonationActivity\|Consts.Billing\|Keys.DONATE\|DONATE" app/src` returns 0. At `e0c51d42` the only `Keys.DONATE` uses were `MainSettings.java:24` and `:172`. |
| IC-11 | PASS, met with approved waiver (E only) | `evidence/I8-device.txt`: `adb install -r` succeeded; the uiautomator titles end with "Advanced settings" (the final root `PreferenceScreen`, `preferences.xml:223`); 0 `donat` nodes; `fatal_count=0`. The precondition "third-party integration off" was **not** met (setting ON). The product owner waived it (`implementation-log.md:383`). No emulator was online during QA (`adb devices` empty), so I ran no device query. |
| IC-12 | PASS (R) | Re-baselined: `git grep` count at `e0c51d42` = 697 and at HEAD = 682. No other story changed the count in between. |
| IC-13 | PASS (R) | `grep -rn "purchase errors" app/src/main/res` returns 0. The strings diff (minimal algorithm) removes 18 `<!-- purchase errors -->` and 18 `<!-- donation related -->` comments. |
| IC-14 | PASS (R, files) | (a) REQ-MODERNIZATION-010 lists `change_records: [CR-004]` and states the supersession by REQ-MODERNIZATION-018 with decision date 2026-11-12. (b) REQ-MODERNIZATION-018 frontmatter lines 14-16 list CR-003 and CR-004. (c) DES-MODERNIZATION-010 carries a SUPERSEDED annotation naming DES-MODERNIZATION-013. Both superseded artifacts have `status: superseded`. All of this is uncommitted, see Issues. |

## Integration Path Verification

> **Rule**: Any new function, class, or component with no verified call path from a production entry point is a BLOCK finding.

This is a removal story. It adds no production component. The only new class is test-only, and its "entry point" is the test runner. The checks here prove that no surviving production path still reaches removed code.

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `EnrollmentDialogThemeTest.HostActivity` (test-only, new) | JUnit/Robolectric | `EnrollmentDialogThemeTest.java:118`, `:180`, `:245` → `Robolectric.buildActivity(HostActivity.class).create()` → `ThemeActivity.onCreate()` (`ThemeActivity.java:17-23`). Deliberately unregistered: `grep HostActivity app/src/main` rc 1, absent from the APK dex. | yes |
| Settings screen (`MainSettings`, `preferences.xml`) | `MainActivity` → preference fragment | `MainSettings.onResume()` (`MainSettings.java:67-72`) no longer calls a donation check. The root `PreferenceScreen` children are StatusPreference, Server, Backup, `enable_auto_sync`, AutoBackupSettings, AdvancedSettings$Main (`preferences.xml:223`), and every fragment class exists. No path leads to a missing class. | yes |
| Removed `DonationActivity` / billing client | (formerly the settings intent) | No remaining reference: IC-10 grep 0; manifest, merged and packaged entries removed; dex 0 classes; R8 kept names lose exactly `DonationActivity`. | yes (absence verified) |
| `App.gcmAvailable` / `GooglePlayServices` (retained, affected by `<queries>` removal) | `App` startup | Unchanged code (`git diff` empty). It loses its declared package visibility of `com.android.vending` and `com.google.android.gms`, which the PO accepted. On the API 36 image both packages are force-queryable (`evidence/I0-queries-visibility.txt`, `I8-device.txt`). | yes (unchanged; accepted behaviour) |

## Behavioral Contract Verification

> **Rule**: For each interface contract (CNTR-*) referenced by the story, every clause must match the implementation.

The story has `integration_contracts: []`. DES-013 asserts that the CNTR-MODERNIZATION-001..005 surfaces in the manifest and the R8 keep set stay unchanged, and I verified each one:

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| CNTR-MODERNIZATION-005 | Exported `BACKUP` receiver, no permission, action `com.zegoggles.smssync.BACKUP` | `AndroidManifest.xml:171`. The after-merged and packaged manifests (both variants) show exported=true and no permission attribute. The diff does not touch the element. | yes |
| CNTR-MODERNIZATION-004 | `InitializationProvider` absent; `SystemForegroundService` type dataSync (0x1); worker class names kept | `InitializationProvider` count 0 in all after manifests; `foregroundServiceType` 0x1. R8 kept names: only `DonationActivity` lost (`comm -23`), so `androidx.work` and app worker names are all kept. | yes |
| CNTR-MODERNIZATION-001/002 | `networkSecurityConfig` on `<application>` | `AndroidManifest.xml:97`, present in all four after manifests | yes |
| CNTR-MODERNIZATION-003 | `fullBackupContent`; Tink keep rules | `AndroidManifest.xml:96`. The configuration-pair diff removes no rule from the app or Tink sections. It adds only the `-dontwarn` for Error Prone annotations (`proguard-rules.pro:62`), which Tink references at compile time only. | yes |
| Preferences persistence (AC-12) | No key migrated, written or deleted; `reset()` unchanged | `Preferences.java:254-259` unchanged; the only `Preferences.java` diff line is the removed enum constant | yes |
| Dependency surface (IC-5) | Surviving modules keep their versions | 0 drift in both variants (QA re-run) | yes |

## Requirement Scope Coverage

> **Rule**: For each referenced REQ-* document, list every scope item.

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| REQ-MODERNIZATION-018 | Inv. 1: donation package | yes | Directory absent (ls), 3 files deleted (I3) |
| REQ-MODERNIZATION-018 | Inv. 2: manifest `DonationActivity` | yes | `AndroidManifest.xml` diff (one line, I2) |
| REQ-MODERNIZATION-018 | Inv. 3: `donate` Preference | yes | `preferences.xml` ends at 318 with no `donate` |
| REQ-MODERNIZATION-018 | Inv. 4: `checkUserDonationStatus`, imports | yes | `MainSettings.java:67-72`; imports removed |
| REQ-MODERNIZATION-018 | Inv. 5: `Keys.DONATE` | yes | `Preferences.java:100-101` |
| REQ-MODERNIZATION-018 | Inv. 6: `Consts.Billing` | yes | `Consts.java:34-35` |
| REQ-MODERNIZATION-018 | Inv. 7: 10 strings + 2 comments × 22 files | yes | Minimal diff: 0 added, 235 removed, all named lines |
| REQ-MODERNIZATION-018 | Inv. 8: dependency + BuildConfig comment | yes | `app/build.gradle:43` (deviation 1: also names `auth/OAuth2Client.java`, which does use `BuildConfig`); dependency line removed |
| REQ-MODERNIZATION-018 | Inv. 9: JaCoCo rule + Rule 5 note | yes | `app/build.gradle:411`; rule deleted |
| REQ-MODERNIZATION-018 | Inv. 10: donation tests | yes | 2 files deleted (15 `@Test`) |
| REQ-MODERNIZATION-018 | Inv. 11: `EnrollmentDialogThemeTest` re-host | yes | `EnrollmentDialogThemeTest.java:305` |
| REQ-MODERNIZATION-018 | Inv. 12: lint-baseline entry | yes | One `<issue>` deleted |
| REQ-MODERNIZATION-018 | Inv. 13: verification metadata | yes (kept, per AC-9) | `gradle/verification-metadata.xml:931` unchanged |
| REQ-MODERNIZATION-018 | Inv. 14: README | yes | `README.md:40-41` kept; paragraph and bullet removed |
| REQ-MODERNIZATION-018 | Inv. 15: withdrawn (metadata/play) | n/a, untouched | `metadata` diff empty |
| REQ-MODERNIZATION-018 | Inv. 16: code-location.md | yes | Bullet removed (I7) |
| REQ-MODERNIZATION-018 | Constraints: JDK 25, no floor change, verification enforced, no behaviour change, keep `ThemeActivity`/`Dialogs.BaseFragment`/`BundleBuilder` | yes | Gate runs; floors diff; `git diff` of `ThemeActivity`, `Dialogs`, `BundleBuilder`, `GooglePlayServices`, `App`, `service/`, `receiver/`, `auth/` is empty; classes present in the dex |
| DES-MODERNIZATION-013 | D1 increments | yes (I9 added, approved) | `git diff-tree` per commit |
| DES-MODERNIZATION-013 | D2 HostActivity | yes | `EnrollmentDialogThemeTest.java:305` |
| DES-MODERNIZATION-013 | D3 JaCoCo | yes | `app/build.gradle:399`, `:411`; QA re-measure |
| DES-MODERNIZATION-013 | D4 / D4a | yes | Metadata unchanged; writer empty; 0 drift |
| DES-MODERNIZATION-013 | D5 manifests | yes | Four deletion-only diffs |
| DES-MODERNIZATION-013 | D6 R8 / APK | yes (with approved rule) | Checks (1)-(4) re-run; `proguard-rules.pro:59-62` |
| DES-MODERNIZATION-013 | D7 docs | yes | `CHANGES:6`; README; code-location |
| DES-MODERNIZATION-013 | D8 supersession | yes (uncommitted) | CR-004; REQ-010, REQ-018, DES-010 frontmatter |

The Context section of REQ-MODERNIZATION-018 notes the app has "no billing rules in `app/proguard-rules.pro`". That is still true: the added rule is an Error Prone `-dontwarn`, and a grep of the file for `billing|donat` returns 0.

## Test Results

- JUnit (fresh XML, QA run 2; run 3 identical): `files=73 tests=682 failures=0 errors=0 skipped=2`. Gradle writes JUnit XML here only through the init script. The XML files already sitting in `app/build/test-results` before QA were stale (older than my run), and I did not use them.
- `git grep` `@Test`: HEAD 682, `e0c51d42` 697.
- JaCoCo LINE coverage (QA runs 1, 2 and 3; run 3 with `clean`) compared with the story table:

| Package | Story I8 / I9 | QA run 1 | QA run 2 | QA run 3 (clean) | Floor | Headroom at the worst QA reading |
|---------|---------------|----------|----------|------------------|-------|-----------------------------|
| root | 17/29 | 17/29 | 17/29 | 17/29 58.6% | 0.50 | +2 lines |
| activity | 199/1011 | 199/1011 | 199/1011 | **189/1011** 18.7% | 0.10 | +88 lines |
| activity.fragments | 58/472 | 58/472 | 58/472 | 58/472 12.3% | 0.10 | +10 lines |
| receiver | 29/58 | 29/58 | 29/58 | 29/58 50.0% | 0.50 | **0 lines** |
| preferences | 313/412 / 314/412 | 314/412 | 314/412 | 314/412 76.2% | 0.65 | +46 lines |
| utils | 30/199 | 30/199 | 30/199 | 30/199 15.1% | 0.10 | +10 lines |

**The 189 vs 199 flip.** I reproduced both values on identical code and diffed the two reports line by line. All 10 lines are in `activity/MainViewModel.kt`. Lines 203-213 are the body of the `observeBackupWork` collector, and lines 246, 255, 256 and 266 are in `mapWorkInfoToBackupState`. Whether the WorkManager `getWorkInfosForUniqueWorkFlow` emits before the test finishes depends on timing, so these lines are sometimes covered and sometimes not. The code predates this story (last changed in U-054) and this story does not touch it. **Flake risk to the gate: none.** The swing is ±1.0 pp against 8.7 pp of headroom.

**`receiver` at exactly 29/58.** `SmsBroadcastReceiver`, `BootReceiver` and their tests use no threads, executors, handlers or coroutines. All seven known runs read 29/58 (I0, I6, I8, I9 and QA ×3). JaCoCo fails only when the ratio is below the minimum, so 0.500 passes. **It is deterministic, so it will not flake.** But it has zero headroom: any future change that removes a covered receiver line, or adds an uncovered one, fails the gate. The I8 `preferences` reading of 313/412 also shows a 1-line nondeterminism there, but that package has 46 lines of headroom.

## Regression Results

- **Existing Behavior to Preserve:** `git diff e0c51d42..HEAD` is empty for `ThemeActivity`, `Dialogs`, `BundleBuilder`, `GooglePlayServices`, `App`, `service/`, `receiver/` and `auth/`. `MainSettings.onResume()` keeps all three calls (`MainSettings.java:67-72`). `reset()` is unchanged. The IC-7 manifest items are unchanged. `EnrollmentDialogThemeTest` keeps 3 tests. Surviving dependency versions are unchanged. Every floor other than the retired one is unchanged. `verification-metadata.xml` and `metadata/play/**` are unchanged. `app/proguard-rules.pro` changed by exactly the approved I9 lines 58-62.
- **Product scope:** `git diff --stat e0c51d42..HEAD -- . ':!sdlc'` covers 38 files (+62/−1246), all in the Affected Code table plus the approved `app/proguard-rules.pro`.
- **Release build:** `:app:assembleRelease` succeeds at HEAD with R8 forced to re-execute. There is no "Missing class" error, and the APK is 1995017 bytes. This confirms the I9 fix independently. The release APK was not run on a device (out of scope, item 7 of the brief).
- **Debug APK:** 9140 classes. `ThemeActivity`, `Dialogs$BaseFragment`, `BundleBuilder`, `BuildConfig`, `GooglePlayServices` and `MainSettings` are all present.

## Issues Found

None blocks the story. Observations and follow-ups:

1. **AC-10 / IC-14 records are uncommitted.** CR-004 is untracked. The REQ-010, REQ-018 and DES-010 edits are working-tree modifications. They meet the criteria as files on disk, but the orchestrator must commit them before the story closes, or the supersession is lost on any reset.
2. **Gate set gap (process).** The per-increment and final gates did not include `:app:assembleRelease`, so the R8 regression showed up only at I8. I agree with the code reviewer: add `:app:assembleRelease` to the standing gate set for any story that changes dependencies or R8 inputs.
3. **JUnit XML is opt-in.** Without the init script, `testDebugUnitTest` writes only binary results and leaves old XML files in place, which can produce stale counts. Agents should always use the init script, or delete `app/build/test-results/testDebugUnitTest` first.
4. **Coverage nondeterminism in `MainViewModel`** (root cause above). Recommended follow-up: make `MainViewModelTest` deterministic, for example with the WorkManager test initializer and `advanceUntilIdle`/`runCurrent`, so the `activity` figure stops flipping between runs.
5. **`receiver` floor has zero headroom.** This is pre-existing. Add a test for `BackupBroadcastReceiver` (0/11) or `PackageReplacedReceiver` (0/11) before any story touches that package.
6. **Recorded follow-ups, carried forward:** delete the dead "Old backup scheduler" setting, `GooglePlayServices.java` and `App.gcmAvailable` (PO decision). Update `sdlc/artifacts/engagement/endpoints.md:62` and `systems.md:55`, which still describe Play Billing (deviation 7).
7. **Literal-wording exceptions** (all classified above): IC-1 raw files withheld; IC-2 gate-before-commit (deviation 9); IC-4 scratch copy; IC-8 (2) dexdump; IC-8 (4) one added rule plus the D6 reading; IC-11 precondition waived; story's "proguard-rules.pro unchanged" (PO-approved I9).
8. **UNVERIFIED (escape-hatch) claims: none.** The Runtime Claims inventory is declared empty (`implementation-log.md:21-25`). I agree: no AC is crash, concurrency, timing, filesystem or hook behaviour. IC-11 is a functional device check accepted from evidence under the PO waiver. I did not re-run it (no device online, and the brief forbids starting one).

## Phase Completion Report
---
story_id: "U-061"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-018/U-061/qa-results.md"
story_status: "review"
current_build_phase: "validation"
ac_passed: 26
ac_total: 26
errors: []
---
