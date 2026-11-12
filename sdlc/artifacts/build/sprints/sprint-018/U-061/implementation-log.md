---
artifact_type: implementation-log
story_id: "U-061"
verdict: "PASS"                    # I9 fixed the release (R8) build; see I8 escalation and I9
agent: "Developer"
timestamp: "2026-11-12"
files_changed: 39
files_created: 0
tests_added: 0
tests_passing: 682
---

# Implementation Log: U-061

## Summary

Story U-061 removes the donation flow and the Play Billing dependency. It runs as nine increments (I0-I8), one per
Developer turn, with the orchestrator committing between turns. This log is appended to at the end of each turn.
Status: I0-I9 complete. I8 found that the release (R8) build failed after the billing removal; the product owner approved a one-line `-dontwarn` rule in `app/proguard-rules.pro` (I9), after which the debug and release builds, 682 tests and all gates are green. Verdict PASS (AC-10 and IC-14 remain the orchestrator's).

## Runtime Claims

None. This story makes no runtime-provable claim of the five classes (crash/interrupt resilience, concurrency,
timing/expiry, git/filesystem mechanics, hook firing). The on-device check (IC-11) is a text-only functional
check, not a runtime-claim probe. Declared empty on purpose.

## Contract Adherence

`integration_contracts: []`. No contract applies. CNTR-MODERNIZATION-001..005 surfaces are asserted unchanged at I8
(IC-6, IC-7, IC-8).

## Increment table

| Inc | HEAD at start | Files (product) | Gate exit | Totals | Proposed message |
|-----|---------------|-----------------|-----------|--------|------------------|
| I0 | e0c51d421659ab3bf3deff7662e7201da6c4fe62 | none | 0 | tests=697 failures=0 errors=0 skipped=2 | U-061 I0: baseline evidence (no product change) |
| I1 | 8866f805b861a153b57cc88e9c1cd050f66a9414 | EnrollmentDialogThemeTest.java | 0 | tests=697 failures=0 errors=0 skipped=2 (isolated 3/3) | U-061 I1: re-host EnrollmentDialogThemeTest on a nested ThemeActivity subclass |
| I2 | 3257a1491e46fa3d33e00a0162157d6a3322c68a | AndroidManifest.xml, MainSettings.java, Preferences.java, preferences.xml | 0 | tests=697 failures=0 errors=0 skipped=2 | U-061 I2: remove donation entry points (settings, MainSettings, Keys.DONATE, manifest) |
| I3 | 5cb305f839e88c8c876f702d0cf84df4cc4c2a81 | Consts.java (M); 3 donation main classes, 2 donation test classes (D) | 0 | tests=682 failures=0 errors=0 skipped=2 | U-061 I3: delete the donation package, its tests and Consts.Billing |
| I4 | 68d06796459673cfbb7fdeadcb9d99af914b1326 | 22 strings.xml files (M) | 0 | tests=682 failures=0 errors=0 skipped=2 | U-061 I4: remove donation strings from all 22 strings.xml files |
| I5 | 9639f5c7df464bcc2f646809642aa36d719c5264 | app/build.gradle, app/lint-baseline.xml (M) | 0 | tests=682 failures=0 errors=0 skipped=2 | U-061 I5: remove the Play Billing dependency and its stale lint-baseline entry |
| I6 | 8cffa46fd99293b943096bbdf154798a1d549bbb | app/build.gradle (M) | 0 | tests=682 failures=0 errors=0 skipped=2 | U-061 I6: retire the activity.donation JaCoCo rule |
| I7 | 5b3c5907d9a6d6200a280d02c32c51e457bacbb7 | CHANGES, README.md, code-location.md (M) | 0 | tests=682 failures=0 errors=0 skipped=2 | U-061 I7: update README, CHANGES and code-location for the donation removal |
| I8 | 151763a44ef0bcabef9c0895332a0e2d6d573c0b | app/build.gradle (M, one comment line) | final gate 0; release R8 build FAILED | tests=682 failures=0 errors=0 skipped=2 | U-061 I8: record the re-measured root-package coverage |
| I9 | 463f860fd03f46476c303a8e3062b9af0903000a | app/proguard-rules.pro (M, approved deviation) | 0 | tests=682 failures=0 errors=0 skipped=2 | U-061 I9: restore the Error Prone dontwarn rule that left with play-services-basement (release R8 build) |

Evidence folder: `sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/` (referred to below as `evidence/`).

## I0 - Baseline capture and device precondition

### Start check and base

- `git rev-parse HEAD` = `e0c51d421659ab3bf3deff7662e7201da6c4fe62`. The plan's BASE `21838718...` is an ancestor; the
  orchestrator instructed that HEAD `e0c51d42` (plan and gate results, `sdlc/**` only) is treated as base wherever
  the plan says BASE. Recorded here.
- `git status --porcelain --untracked-files=all -- app README.md CHANGES sdlc/artifacts/engagement/code-location.md metadata gradle`
  was empty.
- `git diff --quiet 218387187371237b288ca60db390441b6e66da80 HEAD -- app build.gradle gradle README.md CHANGES sdlc/artifacts/engagement/code-location.md`
  returned 0 (no code changed under the plan).
- Helper tools written with the Write tool (no heredoc) to `evidence/tools/`: `junit-xml.init.gradle`,
  `junit_totals.py`, `jacoco_pkgs.py`, `deps_drift.py`, `r8_config_diff.py`, `strip_strings.py`, `ui_titles.py`
  (source verbatim from plan Appendix A).

### I0.2 @Test count (AC-7, IC-12)

`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` = **697**
(`evidence/before-test-count.txt`). Matches the plan. Target after I3 is 682.

### I0.3 Baseline gate (also the I0 gate)

`rm -rf app/build/test-results/testDebugUnitTest; ./gradlew -I <init> --console=plain clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification`
-> BUILD SUCCESSFUL in 2m 37s, **exit=0**. The three gates are green. The init script printed
`U061-DAEMON-JVM: java.version=25.0.5` (AC-5 daemon evidence). Then `:app:processDebugManifest :app:processReleaseManifest`
(up to date) exit 0.

Deviation (evidence hygiene): the raw Gradle log contains machine paths, so it is not kept. `evidence/before-gates-summary.txt`
holds the derived lines (JVM line, BUILD SUCCESSFUL, task list, exit codes). The raw log was deleted.

### I0.4 Baseline evidence

| Item | Result | File |
|------|--------|------|
| JUnit totals | `files=75 tests=697 failures=0 errors=0 skipped=2` (as expected) | `evidence/before-junit-totals.txt` |
| JaCoCo per package | table below | `evidence/before-jacoco-packages.txt` (raw XML kept outside the repo) |
| Merged manifests | debug 449 lines, release 448 lines | `evidence/before-merged-{debug,release}.xml` (release path exists) |
| Packaged manifests (aapt2 36.0.0) | debug 355 lines, release 354 lines, rc 0 | `evidence/before-manifest-{debug,release}.txt` |
| Release APK name | `app-release-unsigned.apk` (as expected) | `evidence/before-apk-sizes.txt` |
| Release mapping | `mapping.txt`, `configuration.txt`, `mapping.prt`, `seeds.txt`, `usage.txt` present | raw kept outside the repo |
| Kept names | 49 names; contains `com.zegoggles.smssync.activity.donation.DonationActivity` (1 match) | `evidence/kept-before.txt` |
| Mapping prefix count | **965** (matches the plan's "about 965"). `com.android.billingclient` entries alone: 1644 lines match the substring (informational; the U-060 figure of 98 was an entry count of another kind) | `evidence/before-mapping-prefix-count.txt` |
| R8 configuration rules | 245 sorted-unique non-comment lines, no machine paths | `evidence/before-configuration-rules.txt` (raw `configuration.txt` kept outside the repo for the I8 `r8_config_diff.py` run) |
| Dependency trees | debug 389 lines, release 389 lines; each contains `com.android.billingclient` (1 line) | `evidence/before-deps-{debug,release}Runtime.txt` |
| apkanalyzer attempt | failed as expected: `NoClassDefFoundError: javax/xml/bind/annotation/XmlSchema`, rc 1 (deviation 3) | `evidence/before-apkanalyzer-attempt.txt` |
| Dex classes (dexdump 36.0.0) | 11061 classes; `com/android/billingclient/` = **190**, `com/zegoggles/smssync/activity/donation/` = **16** (both as expected) | `evidence/before-dex-classes.txt`, `evidence/before-dex-counts.txt` |
| APK sizes (informational) | debug 7234375 bytes, release unsigned 2398685 bytes | `evidence/before-apk-sizes.txt` |

Kept-name recipe: `grep -oE '^([A-Za-z0-9_.$]+) -> \1:' mapping | sed 's/ -> .*//' | grep -E '^(com\.zegoggles\.smssync|com\.fsck\.k9|com\.google\.crypto\.tink|androidx\.work)\.' | LC_ALL=C sort`.

JaCoCo LINE coverage by package (baseline), `evidence/before-jacoco-packages.txt`:

| Package | covered/total | % |
|---------|---------------|---|
| com.zegoggles.smssync (root, floor 0.50) | 18/30 | 60.0% |
| activity (floor 0.10) | 189/1011 | 18.7% |
| activity.donation (floor 0.30, to retire) | 79/236 | 33.5% |
| activity.fragments (floor 0.10) | 58/483 | 12.0% |
| preferences (floor 0.65) | 315/413 | 76.3% |
| receiver (floor 0.50) | 29/58 | 50.0% |
| utils (floor 0.10) | 30/199 | 15.1% |

Observation (not a deviation): the plan's assumption ledger listed `activity` as 199/1011 = 19.7%; the fresh
measurement is 189/1011 = 18.7%. Total lines are identical; the 10-line difference in covered lines is a local
coverage variance. It does not affect any floor (0.10) and I6 and I8 re-measure against this I0 value. All other
packages equal the plan's figures. The `receiver` rule is at exactly 50.0% with zero headroom (R2).

### I0.5 Device precondition (IC-11) and Play Store visibility (contract-verification check 1)

Emulator: AVD `Pixel_9_Pro` was not running; the Developer started it, confirmed the AVD name by
`emu avd name`, and later shut it down with `emu kill`. No other emulator touched. No install, no uninstall, no
data clear, no setting changed, no broadcast sent, no screenshots, no backup or restore triggered.

- `getprop ro.build.version.sdk` = 36. App installed (1 package match); installed build `versionCode=1602`
  `versionName=1.6.0-BETA2` `targetSdk=35` (the confirmed installed build; version info only).
- `run-as ... ls shared_prefs` rc=0, no `run-as:` error; `com.zegoggles.smssync_preferences.xml` listed: yes;
  `cat` rc=0, file contains `<map`. Matched attribute: `name="third_party_integration" value="true"`.
- **State: ON.** The temporary prefs copy was deleted. File: `evidence/I0-device-precondition.txt`.
- **ESCALATION (non-blocking): IC-11 precondition: third-party integration is ON on Pixel_9_Pro; the Developer will not
  change it.** Product-owner information: the setting gates only the inbound `com.zegoggles.smssync.BACKUP` broadcast
  (`Preferences.java:179`); IC-11 sends no broadcast and only opens Settings, so a waiver is low-risk. The story
  still requires it to be off, so the product owner switches it off in the app or waives the precondition before I8.
  The build continues meanwhile.
- `dumpsys package queries`, baseline (installed build, with `<queries>` still in its manifest), recorded in
  `evidence/I0-queries-visibility.txt` (only this fact, no other package data):
  - `com.zegoggles.smssync` can see `com.android.vending`: **no** per-app entry; effective visibility via the global
    `forceQueryable` list: **yes**.
  - `com.zegoggles.smssync` can see `com.google.android.gms`: **no** per-app entry; effective visibility via the
    global `forceQueryable` list: **yes**.
  - Reading: on this API 36 image both packages are in the system's forceQueryable set, visible to every app. At I8
    the check is repeated; the comparison to make is the per-app entries (none at baseline) plus the forceQueryable
    membership, so losing the `<queries>` element is not expected to change the result on this image.

### Deviation / handling notes for I0

1. Raw Gradle log, JaCoCo XML, mapping.txt and configuration.txt are not under `evidence/` (hygiene rule). Derived
   results are. Raw copies sit in a scratch folder outside the repository (session scratchpad, `u061/`); the
   I8 turn reuses `before-mapping.txt` and `before-configuration.txt` from there for `r8_config_diff.py`, and the
   scratch folder is deleted at the end of I8. If it has disappeared by I8, the baseline would have to be rebuilt
   from the I0 commit, so the orchestrator should note this dependency.
2. `before-configuration-rules.txt` (derived list) is an addition to the plan's file list, so that the I0 R8 baseline is
   still visible in the repository.

### End check

`git status --porcelain --untracked-files=all -- app README.md CHANGES sdlc/artifacts/engagement/code-location.md metadata gradle`
is empty (I0 expected list: none).

## I1 - Re-host EnrollmentDialogThemeTest (IC-3, AC-6)

- Start: HEAD `8866f805b861a153b57cc88e9c1cd050f66a9414` (I0 commit). Product-pathspec porcelain empty.
- Edited only `app/src/test/java/com/zegoggles/smssync/activity/fragments/EnrollmentDialogThemeTest.java` (LF, kept LF):
  import `DonationActivity` -> `ThemeActivity`; comment block and javadoc rewritten (both now say the AppCompat theme
  comes from `ThemeActivity.onCreate()`, not the manifest; `{@link DonationActivity}` -> `{@link HostActivity}`);
  three host lines -> `HostActivity activity = Robolectric.buildActivity(HostActivity.class).create().get();`;
  added `public static class HostActivity extends ThemeActivity {}` (nested, with the plan's javadoc, no `*/` glob).
  No assertion changed. numstat 25 added / 11 deleted.
- Self-check: `grep -c DonationActivity` = 0; `grep -c @Test` = 3.
- IC-3 isolated run (`--tests ...EnrollmentDialogThemeTest`): exit 0, `files=1 tests=3 failures=0 errors=0 skipped=0`
  (`evidence/I1-isolated-junit-totals.txt`, `evidence/I1-isolated-theme-test.log`, derived). `HostActivity.class` exists under
  test-class directories only; `grep -rn HostActivity app/src/main` empty (`evidence/I1-hostactivity-checks.txt`).
- Gate G (`:app:assembleDebug :app:testDebugUnitTest`): exit 0, `files=75 tests=697 failures=0 errors=0 skipped=2`
  (`evidence/I1-gates.log`, `evidence/I1-junit-totals.txt`; derived, raw logs not kept for path hygiene).
- End check: porcelain = ` M app/src/test/java/com/zegoggles/smssync/activity/fragments/EnrollmentDialogThemeTest.java` (matches the I1 list).
- Proposed message: `U-061 I1: re-host EnrollmentDialogThemeTest on a nested ThemeActivity subclass`.
- Note: git warns that LF will be replaced by CRLF (core.autocrlf); this is the plan's known LF-file state, not a change.

## I2 - Remove the entry points (AC-2, AC-12)

- Start: HEAD `3257a1491e46fa3d33e00a0162157d6a3322c68a` (I1 commit); product-pathspec porcelain empty.
- Line endings: all four files are CRLF in the working tree (LF in the index). Edits were made byte-preserving with a
  scratch script that deletes exact line ranges after asserting each line's content, so CRLF is kept and the diff is
  deletions only (`git diff --numstat`: manifest 0/1, MainSettings 0/26, Preferences 0/1, preferences.xml 0/10).
- `preferences.xml`: deleted lines 318-327 (blank line plus the `donate` Preference block); `</PreferenceScreen>` now follows
  the inner `</PreferenceScreen>` directly.
- `MainSettings.java`: deleted lines 5 (`Log` import), 14-15 (DonationActivity imports), 22 (`App.TAG` static import, deviation 2),
  24 (`Keys.DONATE` import), 75 (`checkUserDonationStatus();` call) and 163-182 (blank line plus the method). `onResume()` is now
  `super.onResume();`, blank line, `updateAutoBackupPreferences();`, `addPreferenceListener(ENABLE_AUTO_BACKUP.key);`.
  `Preference`, `ArrayList`, `List` imports kept.
- `Preferences.java`: deleted line 101 `DONATE("donate"),`. `reset()` untouched (removes only `SMS_DEFAULT_PACKAGE_CHANGE_SEEN`
  and `SMS_DEFAULT_PACKAGE`).
- `AndroidManifest.xml`: deleted line 129 (`<activity android:name=".activity.donation.DonationActivity"/>`) only.
- Checks (`evidence/I2-checks.txt`): `grep 'key="donate"' preferences.xml` empty; `grep DONATE|DonationActivity` over app/src/main
  java+xml excluding `/activity/donation/` empty.
- AC-12 reviewed-code evidence: the pre-I2 `git grep` of `"donate"|DONATE` in app/src/main shows `Keys.DONATE` only at
  `Preferences.java:101` (definition) and `MainSettings.java:24` (import) and `:172` (`findPreference(DONATE.key)`, a read);
  `preferences.xml:320` has the XML key. The broader pattern also lists DonationActivity's `DONATED`/`NOT_DONATED` enum constants
  (a different symbol, deleted in I3) and the `donate` string resources (I4). No `put*`/`edit()`/`remove` use of the key exists
  (grep rc=1), so nothing was ever persisted for it. No key is migrated, written or deleted.
- Gate G: exit 0, `files=75 tests=697 failures=0 errors=0 skipped=2` (`evidence/I2-gates.log`, `evidence/I2-junit-totals.txt`;
  derived). The donation tests still run against the undeclared `DonationActivity`.
- End check: porcelain = the four ` M` paths above (matches the I2 expected list, 4).
- Proposed message: `U-061 I2: remove donation entry points (settings, MainSettings, Keys.DONATE, manifest)`.

## I3 - Delete the donation code, its tests and Consts.Billing (AC-6, AC-7, IC-10)

- Start: HEAD `5cb305f839e88c8c876f702d0cf84df4cc4c2a81` (I2 commit); product-pathspec porcelain empty.
- Deleted (`rm -r`): `app/src/main/java/com/zegoggles/smssync/activity/donation/{DonationActivity,DonationListFragment,Sku}.java` and
  `app/src/test/java/com/zegoggles/smssync/activity/donation/{DonationActivityTest,SkuTest}.java` (the two directories are gone).
- `Consts.java`: deleted lines 35-50 (blank line plus `Billing` class) with a byte-preserving, assertion-guarded line-range script;
  line endings preserved; numstat 0 added / 16 deleted; the file ends `CALLLOG_PROVIDER ...;` then `}`.
- IC-10: `grep -rn "DonationActivity\|Consts.Billing\|Keys.DONATE\|DONATE" app/src` gives 0 results (`evidence/I3-ic10-grep.txt`).
- Gate G: exit 0, **`files=73 tests=682 failures=0 errors=0 skipped=2`** (`evidence/I3-gates.log`, `evidence/I3-junit-totals.txt`; derived).
  697 - 15 = 682; 2 files fewer (75 -> 73). Working-tree `@Test` count (`grep -rh "@Test" app/src/test ... | grep -c`) = **682**
  (`evidence/I3-worktree-test-count.txt`), equal to I0 minus 15 (AC-7). The HEAD-based `git grep` is re-run by QA after the I8 commit.
- End check: porcelain = ` M Consts.java` plus five ` D` paths (matches the I3 expected list, 6).
- Proposed message: `U-061 I3: delete the donation package, its tests and Consts.Billing`.

## I4 - Strings, all 22 files, by name (AC-1 item 7, IC-13)

- Start: HEAD `68d06796459673cfbb7fdeadcb9d99af914b1326` (I3 commit); product-pathspec porcelain empty.
- Dry run (`evidence/I4-strings-dryrun.txt`): every folder `OK` (25 folders, 0 MISMATCH); last line `files=25 expected_total=235 ok=True`.
  Every listed line is one of the 10 named `<string>` lines or one of the two exact comments (the script selects only those).
- Apply (`evidence/I4-strings-apply.txt`): `WRITTEN`. The script works on raw bytes, so each file's line endings and the
  `values-zh-rTW` BOM (`357 273 277`, confirmed) are preserved. `values-cs`, `values-es`, `values-nb-rNO` are untouched.
- **Totals (this run): files changed 22; lines deleted 235; lines added 0** (`git diff --diff-algorithm=minimal --numstat`).
- Observation (not a defect): git's default (Myers) diff of 14 of these files shows `2 added / +2 deleted` blank lines
  (default numstat total 28 added / 263 deleted, net 235) because the kept blank lines between the removed strings are aligned
  differently. The file bytes are the same either way; `minimal`, `patience` and `histogram` all give 0 added / 235 deleted.
  The plan's step-3 pure-deletion proofs were therefore run with `--diff-algorithm=minimal` and give: added lines 0;
  removed lines outside the 10 names and 2 comments 0 (`evidence/I4-strings-proof.txt`). The orchestrator's check of the
  committed diff should use the same option (or `git diff --numstat -w`-free equivalent) to see pure deletions.
- Proofs (`evidence/I4-strings-proof.txt`): `grep -rniE "billing|donat" app/src/main/res` = 0 (AC-1-scoped strings grep);
  `grep -rn "purchase errors" app/src/main/res` = 0 (IC-13).
- Gate G: exit 0, `files=73 tests=682 failures=0 errors=0 skipped=2` (`evidence/I4-gates.log`, `evidence/I4-junit-totals.txt`; derived).
- End check: porcelain = 22 ` M` paths (`values/strings.xml` and the 21 locale files; matches the I4 expected list, 22).
- Proposed message: `U-061 I4: remove donation strings from all 22 strings.xml files`.

## I5 - Dependency, BuildConfig comment, lint baseline (AC-4, IC-5, AC-12)

- Start: HEAD `9639f5c7df464bcc2f646809642aa36d719c5264` (I4 commit); product-pathspec porcelain empty.
- `app/build.gradle` (LF, kept): line 43 comment now `// app uses BuildConfig.DEBUG in App.java and auth/OAuth2Client.java.`
  (deviation 1, accepted by the orchestrator); deleted line 132 `implementation 'com.android.billingclient:billing:7.1.1' // U-028 ...`.
  numstat 1 added / 2 deleted (the reworded comment is the 1 added line). No `dependency-verification` flag anywhere.
- `app/lint-baseline.xml` (CRLF, kept): deleted lines 114-124 (billing 2.1.0 `GradleDependency` issue plus the blank after it).
  numstat `0 11`; `grep -c billing app/lint-baseline.xml` = 0 (AC-12).
- **IC-5 / D4a drift guard** (`deps_drift.py`, `evidence/I5-drift-{debug,release}.txt`), both variants **exit 0**:
  - before=95 modules, after=83, common=83.
  - **Modules removed (12, exactly the expected set):** `com.android.billingclient:billing`;
    `com.google.android.datatransport:{transport-api,transport-backend-cct,transport-runtime}`;
    `com.google.android.gms:{play-services-base,play-services-basement,play-services-location,play-services-places-placereport,play-services-tasks}`;
    `com.google.firebase:{firebase-encoders,firebase-encoders-json,firebase-encoders-proto}`. Unexpected removals: none; expected but still present: none.
  - **Version drift of any surviving module: none. Modules added: none.** No pin or `constraints {}` added.
  - `grep -c com.android.billingclient` over `evidence/I5-deps-{debug,release}Runtime.txt` = 0 (AC-4 part 1).
- Gate G: exit 0, `files=73 tests=682 failures=0 errors=0 skipped=2` (`evidence/I5-gates.log`, `evidence/I5-junit-totals.txt`; derived).
- `gradle/verification-metadata.xml` untouched (D4).
- End check: porcelain = ` M app/build.gradle`, ` M app/lint-baseline.xml` (matches the I5 expected list, 2).
- Proposed message: `U-061 I5: remove the Play Billing dependency and its stale lint-baseline entry`.

## I6 - Retire the JaCoCo rule (AC-8, IC-9) and the binding coverage read

- Start: HEAD `8cffa46fd99293b943096bbdf154798a1d549bbb` (I5 commit); product-pathspec porcelain empty.
- `app/build.gradle` (LF, kept): Rule 5 comment `// MEASURED: activity=15.4%, activity.auth=0%, activity.donation=33.5%,` ->
  `// MEASURED: activity=15.4%, activity.auth=0%,`; deleted the donation rule with its comment (10 lines, from
  `// activity.donation: DonationActivity coverage.` through its closing `}`). numstat 1 added / 11 deleted. The only
  `minimum` line changed is `- minimum = 0.30 ... activity.donation` (self-check passed).
- Gate (`clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification`): **exit 0**,
  `files=73 tests=682 failures=0 errors=0 skipped=2` (`evidence/I6-gates.log`, `evidence/I6-junit-totals.txt`; derived).
  `activity.donation` package is absent from the report; `grep -c HostActivity` on the JaCoCo XML = 0.
- Re-measured LINE coverage (`evidence/I6-jacoco-packages.txt`; raw XML kept outside the repo), against the I0 baseline and floor:

| Package | I0 baseline | I6 measured | Floor | Result |
|---------|-------------|-------------|-------|--------|
| root `com.zegoggles.smssync` | 18/30 60.0% | **17/29 58.6%** | 0.50 | pass (prediction 58.6% confirmed) |
| activity | 189/1011 18.7% | 189/1011 18.7% | 0.10 | pass |
| activity.fragments | 58/483 12.0% | 58/472 12.3% | 0.10 | pass |
| receiver | 29/58 50.0% | 29/58 50.0% | 0.50 | pass (zero headroom, unchanged) |
| preferences | 315/413 76.3% | 314/412 76.2% | 0.65 | pass |
| utils | 30/199 15.1% | 30/199 15.1% | 0.10 | pass |

  No floor breached, so the IC-9 route was not needed; no test was added. The I8.1 root-rule comment will use
  `58.6% (17/29)` (to be re-confirmed at I8.4).
- **AC-1 first full assertion** (`evidence/I6-ac1-grep.txt`): `ls app/src/main/java/com/zegoggles/smssync/activity/donation/` fails (no such directory);
  `grep -rniE "billing|donat" app/src/main app/build.gradle` = **0 results**.
- End check: porcelain = ` M app/build.gradle` (matches the I6 expected list; no IC-9 test).
- Proposed message: `U-061 I6: retire the activity.donation JaCoCo rule`.

## I7 - Docs (AC-11, D7)

- Start: HEAD `5b3c5907d9a6d6200a280d02c32c51e457bacbb7` (I6 commit); product-pathspec porcelain empty.
- Byte-preserving, assertion-guarded edits (existing line endings kept: CHANGES and README.md CRLF in the working tree, code-location.md LF).
- `README.md`: deleted the `* Google Play billing service - used for in-app donations` bullet (was line 241), then the donation paragraph
  (lines 43-45: two text lines plus the blank). The sentence "SMS Backup+ is available for free ... there will never be a pro / paid version."
  (lines 40-41) is retained. numstat 0 added / 4 deleted; `grep -ciE 'billing|donat' README.md` = 0.
- `CHANGES`: inserted after `  * Updated Swedish localization (#890)` (unreleased 1.6.0 block) exactly:
  `  * Removed the in-app donation feature and the in-app billing (Play Billing) dependency`. Lines 23, 120, 138, 168 untouched. numstat 1 / 0.
- `sdlc/artifacts/engagement/code-location.md`: deleted line 37 (the `com.android.billingclient:billing` in-app donations bullet). numstat 0 / 1.
- `git diff --stat -- metadata/play sdlc/artifacts/engagement/endpoints.md sdlc/artifacts/engagement/systems.md` is empty
  (`evidence/I7-checks.txt`; `endpoints.md:62` and `systems.md:55` still describe Play Billing: deviation 7, not edited).
- Gate G: exit 0, `files=73 tests=682 failures=0 errors=0 skipped=2` (`evidence/I7-gates.log`, `evidence/I7-junit-totals.txt`; derived).
- End check: porcelain = ` M CHANGES`, ` M README.md`, ` M sdlc/artifacts/engagement/code-location.md` (matches the I7 expected list, 3).
- Proposed message: `U-061 I7: update README, CHANGES and code-location for the donation removal`.

## I8 - Single edit, final verification, final gate, after-evidence

- Start: HEAD `151763a44ef0bcabef9c0895332a0e2d6d573c0b` (I7 commit); porcelain empty. The scratch folder with the raw I0 files was intact, so no rebuild was needed.
- **I8.1 edit** (`app/build.gradle`, LF): inserted after the `U-060 re-measured: 60.0% (18/30)` line:
  `        // U-061 re-measured: 58.6% (17/29) - the nested SKU-constants class was removed from Consts.` (no "billing"/"donat"). 1 line added; `minimum = 0.50` untouched.
- **I8.2 greps** (`evidence/I8-final-greps.txt`, run after the edit): AC-1 donation dir absent and `grep -rniE "billing|donat" app/src/main app/build.gradle` = 0; IC-10 = 0; IC-13 = 0.
- **I8.3 final gate** (`clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`): **exit 0**, JDK line `U061-DAEMON-JVM: java.version=25.0.5`,
  `files=73 tests=682 failures=0 errors=0 skipped=2` (`evidence/final-gates.log`, `evidence/final-junit-totals.txt`; derived). AC-5 met.
- **I8.4 after-evidence build** (`:app:assembleRelease :app:jacocoTestReport :app:processDebugManifest :app:processReleaseManifest`): **exit 1.
  `:app:minifyReleaseWithR8` FAILED** (`evidence/after-evidence-build.log`).

### ESCALATION: release R8 failure caused by removing the billing dependency

- R8 reports `Missing class com.google.errorprone.annotations.{CanIgnoreReturnValue,CheckReturnValue,Immutable,RestrictedApi}` referenced from
  `com.google.crypto.tink.*` (tink-android) and stops: "Missing classes detected while running R8".
- Cause (verified): the I0 R8 `configuration.txt` line 360, `-dontwarn com.google.errorprone.annotations.**`, came from the consumer ProGuard rules of
  `play-services-basement-18.4.0`, one of the 12 modules this story removes. Without it, tink's references to the annotations (not on the runtime classpath) are fatal. `app/proguard-rules.pro` has no errorprone rule.
- Why earlier gates did not catch it: the per-increment gates and the final gate build no release variant; only the I0 baseline did (assembleRelease). The debug build is unaffected.
- Impact: `assembleRelease` fails on the repository tree, so the IC-6 release packaged-manifest diff, IC-8 (mapping, kept names, R8 config) and the files
  `after-mapping`, `after-configuration`, `kept-after`, `after-manifest-release` could NOT be produced for the repository tree. Nothing was edited to make it pass
  (plan: after I8.1 no tracked file may change; the story lists `app/proguard-rules.pro` as unchanged).
- Candidate remedy (verified in a scratch copy outside the repo, then deleted): appending one line `-dontwarn com.google.errorprone.annotations.**` to `app/proguard-rules.pro`
  makes `assembleRelease` succeed. Informational results from that scratch build (`evidence/scratch-remedy-*`): the release packaged manifest diff vs I0 is 4 deletion-only hunks with no `>` line;
  mapping prefix count 0 (I0: 965); kept-name `comm -23` is exactly `com.zegoggles.smssync.activity.donation.DonationActivity` and `comm -13` is empty; `r8_config_diff.py` exit 0
  (49 removals, all attributed, 0 added; the identical `-dontwarn` text now also exists in proguard-rules.pro, so the set-based diff does not list it as removed).
  This needs a product-owner / orchestrator decision (it edits `app/proguard-rules.pro`, which the story lists as unchanged, in a new increment) and is NOT applied.
- Another option the orchestrator may consider, not tried: a compileOnly `error_prone_annotations` dependency.

### Manifest equivalence (IC-6, AC-3) - `evidence/I8-manifest-diffs.txt`
- Merged debug and merged release: each non-empty, every hunk a deletion, 0 lines start with `>`. The removed lines group exactly into the AC-3 elements
  (BILLING permission, `<queries>` with both intents, billingclient.version meta-data, ProxyBillingActivity, ProxyBillingActivityV2, DonationActivity, GoogleApiActivity,
  gms.version meta-data, TransportBackendDiscovery with backend meta-data, JobInfoSchedulerService, AlarmManagerSchedulerBroadcastReceiver). Annotated per hunk in the file.
- Packaged debug (normalised with the plan's `norm()`): 4 deletion-only hunks, same elements, 0 `>` lines.
- Packaged release: **not produced** (R8 failure). Only the scratch-copy informational result exists.
- IC-7 (`evidence/I8-ic7.txt`), packaged DEBUG manifest: package and versionCode 1602 unchanged; `BackupBroadcastReceiver` exported=true, no permission attribute, action `com.zegoggles.smssync.BACKUP`;
  `SystemForegroundService` foregroundServiceType=0x1; no InitializationProvider; networkSecurityConfig and fullBackupContent present; `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` x2; INTERNET and ACCESS_NETWORK_STATE present.
  The merged release manifest was checked by name for the same items. The packaged RELEASE IC-7 check is not done.

### Release and APK checks (IC-8, AC-4) - `evidence/I8-ic8.txt`
- Debug dex: billingclient 0 (I0: 190), activity/donation 0 (I0: 16), total 9140 classes (I0: 11061). Dependency trees: 0 billingclient; drift guard exit 0 in both variants.
- Mapping, kept names and `r8_config_diff.py` on the repository tree: not produced (see escalation). The optional member-level kept-name comparison (contract-verification check 2) was not done for the same reason.

### Other checks - `evidence/I8-checks.txt`
- `git diff --stat HEAD -- gradle/verification-metadata.xml metadata/play app/proguard-rules.pro` is empty; no `dependency-verification=off|lenient` in the product/build diff.
- AC-7: working-tree `@Test` count 682 = 697 - 15; JUnit 682/0/0/2.
- AC-8 re-measure (final tree, `evidence/after-jacoco-packages.txt`): root 17/29 58.6% (floor 0.50); activity 199/1011 19.7% (0.10); activity.fragments 58/472 12.3% (0.10);
  receiver 29/58 50.0% (0.50, zero headroom); preferences 313/412 76.0% (0.65); utils 30/199 15.1% (0.10). The activity figure was 189 in the I0 and I6 runs and 199 in this run
  (it varies by 10 lines between runs; always above its floor). The root value equals the one written in the I8.1 comment.

### IC-4 verification-metadata writer (scratch copy, deviation 8) - `evidence/I8-ic4-writer.{log,diff}`
- Scratch copy outside the repo built from `git archive HEAD` (LF tree) plus the uncommitted I8 diff; `./gradlew --write-verification-metadata sha256 help` exit 0.
  Classification: **empty** (the diff with `--strip-trailing-cr` is empty; the writer emits CRLF while the committed blob is LF, so the raw byte diff differs only in line endings). No additions, no removals;
  the stale billing entries remain, which AC-9 allows. The repository file was never changed. Scratch copy and patch deleted.

### On-device check (IC-11, AC-2) - `evidence/I8-device.txt`, AVD `Pixel_9_Pro` API 36
- Third-party integration re-read (read-only): **still ON** (`third_party_integration=true`), unchanged by the Developer. IC-11 evidence was gathered with the setting ON.
  **PO WAIVER PENDING: third-party integration setting was ON during the IC-11 device check.** No broadcast was sent; no backup or restore was triggered.
- `adb install -r` of the debug APK: Success (versionCode 1602, 1.6.0-BETA2). No uninstall, no data clear, no setting change, no screenshots, no account data recorded.
- `am start -W` Status ok (COLD launch). `ui_titles.py`: titles `IMAP settings, Backup settings, Auto backup, Auto backup settings, Advanced settings`, 0 `donat` nodes, PASS (top and bottom dumps).
- **Plan correction (not an implementation defect):** the plan asked for last title `Auto backup settings`. The final PreferenceScreen in `preferences.xml` (line 222, `AdvancedSettings$Main`) is **Advanced settings**,
  which is also the last title shown, so the check was run with `--expect-last "Advanced settings"` and passes. No Donate entry exists. The first bottom-dump run with the plan's literal string failed (exit 1) and was replaced by the corrected run.
- logcat since before launch (launch, Settings, Auto backup settings screen, back twice): `fatal_count=0`; lines containing "Google Play Services": **0**.
- `use_old_scheduler` row on the Auto backup settings screen (navigation only, nothing changed): title "Old backup scheduler", summary text "Force use of old backup scheduler", enabled=true.
  No I0 baseline of this row or of the Google Play Services line count was taken (the instruction asked for them at I8 only), so no before/after delta exists for them.
- Play Store visibility (accepted by the product owner; not escalated; `<queries>` not re-added): after install, `dumpsys package queries` has no per-app entry for `com.zegoggles.smssync` -> `com.android.vending` or `com.google.android.gms` (no),
  and both are in the global forceQueryable list (yes), exactly as at I0. Nothing observable changed on this image.
- The emulator was started by the Developer and shut down afterwards (`emu kill`); the temporary UI dump on the device was deleted.

### Close-out
- AC-7 counts: before 697 (`evidence/before-test-count.txt`), after 682 (working tree and JUnit). QA re-runs the HEAD-based `git grep` after the I8 commit.
- IC-5: no drift, 12 modules removed (I5). IC-4: empty (above). Deviation 8 applied (scratch copy, not scratch branch). Deviation 9 applied: the final gate ran on the identical tree before the commit.
- Scratch folders deleted: the session scratchpad `u061/` and helper scripts, the IC-4 scratch copy and its patch file; Gradle daemons stopped.
- End check: porcelain = ` M app/build.gradle` only (matches the I8 expected list).
- Proposed message: `U-061 I8: record the re-measured root-package coverage`.
- Not verified: IC-6 packaged release diff; IC-8 (1), (3), (4) on the repository tree; IC-7 on the packaged release manifest; the optional kept-name member comparison; AC-10 and IC-14 (not Developer work).

## Product-owner decisions received after I8 (recorded at I9)

1. **Release R8 failure:** the product owner chose "Add the one-line rule (Recommended)" and approved an extra step in this story, a change to `app/proguard-rules.pro`.
   This is an approved deviation from the story's "unchanged" statement for that file and from the plan's file list. The third comment line was worded by the orchestrator as
   "it left with the in-app purchase library." (instead of naming the retired library), to keep the file free of the retired terms.
2. **Third-party integration setting:** the product owner chose "Waive it (Recommended)". The IC-11 on-device evidence, gathered with the setting ON, is accepted. The waiver is recorded here; the device check was not repeated and the setting was not changed.
   This closes `PO WAIVER PENDING` from I8.

## I9 - Approved release-build fix and release-side evidence

- Start: HEAD `463f860fd03f46476c303a8e3062b9af0903000a` (I8 commit); product-pathspec porcelain empty.
- Edit (`app/proguard-rules.pro`, the only product path): appended after `-dontoptimize`, with one blank line first:
  ```
  # Tink references compile-time-only Error Prone
  # annotations. play-services-basement used to
  # supply this rule; it left with the in-app purchase library.
  -dontwarn com.google.errorprone.annotations.**
  ```
  numstat 5 added / 0 deleted. Line endings: the file has mixed endings in the working tree (53 CRLF, 4 LF-only; the committed blob is LF) and its last line ends in LF, so the appended lines use LF to match the adjacent line. `grep -ci "billing|donat"` on the file = 0.
- **Gate** (`clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification`): **exit 0**, `U061-DAEMON-JVM: java.version=25.0.5`,
  `files=73 tests=682 failures=0 errors=0 skipped=2` (`evidence/I9-gates.log`, `evidence/I9-junit-totals.txt`; derived). The release build now succeeds (`app-release-unsigned.apk` 1995017 bytes, I0: 2398685).
- JaCoCo on this tree (`evidence/I9-jacoco-packages.txt`): root 17/29 58.6%; activity 199/1011 19.7%; activity.fragments 58/472 12.3%; receiver 29/58 50.0%; preferences 314/412 76.2%; utils 30/199 15.1%. All floors met; root equals the value in the I8 comment.
- **Baseline rebuilt (item 7):** the raw I0 files had been deleted with the scratch folder at the end of I8, so the baseline release `mapping.txt` and `configuration.txt` were rebuilt from the pre-change code
  (`git archive e0c51d42`, LF tree) in a scratch copy outside the repo, where the release build works because billing is still present. Verified equal to the original I0 derived evidence:
  kept names identical to `kept-before.txt`, configuration rules identical to `before-configuration-rules.txt`, prefix count 965 (`evidence/I9-baseline-rebuild.txt`). The scratch copy was deleted afterwards.
- **IC-6 (AC-3), release-side now produced** (`evidence/I9-manifest-diffs.txt`, `norm()` from the plan): merged debug, merged release, packaged debug and **packaged release** are each non-empty, every hunk a deletion, 0 lines start with `>`.
  Packaged release hunks 68-76, 122-123, 217-236, 324-336 remove exactly: BILLING permission and `<queries>` with both intents; DonationActivity; billingclient.version meta-data, ProxyBillingActivity, ProxyBillingActivityV2, GoogleApiActivity, gms.version meta-data;
  TransportBackendDiscovery with its `backend:` meta-data, JobInfoSchedulerService, AlarmManagerSchedulerBroadcastReceiver. The merged and debug hunks are identical to the I8 diffs.
  Debug evidence from I8 stands (and was re-run on this tree anyway): `buildTypes.debug` in `app/build.gradle` has no `minifyEnabled` or `proguardFiles`, so `proguard-rules.pro` does not affect the debug variant (checked at `app/build.gradle` lines 47-64: only `release` sets `minifyEnabled true` and `proguardFiles`).
- **IC-7 on the packaged release manifest** (`evidence/I9-ic7-release.txt`): package unchanged, versionCode 1602; `BackupBroadcastReceiver` exported=true, no permission attribute, action `com.zegoggles.smssync.BACKUP`; `SystemForegroundService` foregroundServiceType=0x1; 0 InitializationProvider; networkSecurityConfig and fullBackupContent present; `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` x2; INTERNET and ACCESS_NETWORK_STATE present.
- **IC-8** (`evidence/I9-ic8.txt`): (1) release mapping prefix count 0 (I0 965; `com.android.billingclient` substring 0); (2) debug dex billingclient 0 and activity/donation 0 (I0 190 and 16; 9140 classes);
  (3) `comm -23 kept-before kept-after` prints exactly `com.zegoggles.smssync.activity.donation.DonationActivity`, `comm -13` prints nothing;
  (4) R8 configuration per D6: new source-aware tool `evidence/tools/r8_config_pairs.py` compares (rule, contributing section) pairs: 63 removed, all attributed (removed-library sections or AAPT keeps for the 7 AC-3 components),
  exactly **1 added: `-dontwarn com.google.errorprone.annotations.**` from `proguard-rules.pro`**, the approved rule, and the same rule text is shown removed from the `play-services-basement` section; any other added pair fails closed. Exit 0 (`evidence/I9-r8-config-pairs.txt`).
  The original `r8_config_diff.py` (set-based, 0 added because the rule text still exists) also exits 0 (`evidence/I9-r8-config-diff.txt`).
- **Optional member-level kept-name comparison** (`evidence/tools/kept_members.py`, `evidence/I9-kept-members.txt`): app-prefix classes 1419 -> 1400. Lost classes: `Consts$Billing`, all `activity.donation.*` classes (DonationActivity and its inner classes, DonationListFragment and inner classes, Sku and inner classes),
  `MainSettings$2` and `MainSettings$3` (anonymous classes of the removed donation-status check), and one R8-synthetic class (`AbstractFuture$$ExternalSyntheticBUOutline0`, replaced by synthetic `App$$ExternalSyntheticApiModelOutline0`, an R8 outlining artefact). In surviving classes 1 member line differs: `StatusPreference$SavedState.<init>` whose signature text embeds an obfuscated type name (`h4` vs `v2`), not a kept-member change. No other kept member lost.
- Dependency trees: 0 billingclient in both variants; drift guard exit 0 in both variants (`evidence/after-deps-*`).
- **Greps** (`evidence/I9-final-greps.txt`): AC-1 donation dir absent and grep over `app/src/main app/build.gradle` = 0; IC-10 = 0; IC-13 = 0; extra: `app/proguard-rules.pro` = 0.
- AC-9 / metadata: `git diff --stat HEAD -- gradle/verification-metadata.xml metadata/play` is empty; no `dependency-verification=off|lenient` in the `app` diff.
- Cleanup: scratch folders (session scratchpad `u061/`, baseline scratch copy) deleted. No emulator was used in I9.
- Housekeeping disclosure: at the end of I8 my cleanup command `rm "<scratchpad>/"*.py` also removed any `.py` files that other work had left in the shared session scratchpad directory (it was a shared folder, not only my `u061/` subfolder). I only created `qvis.py`, `uipref.py`, `i2edit.py`, `i3edit.py`, `i5edit.py`, `i6edit.py`, `i7edit.py`, `merge.py` there. If the orchestrator kept `.py` helpers in that directory, they were deleted by me and must be re-created.
- End check: porcelain = ` M app/proguard-rules.pro` only (matches the I9 expected list).
- Proposed message: `U-061 I9: restore the Error Prone dontwarn rule that left with play-services-basement (release R8 build)`.

### Final AC / IC table (whole story)

| Item | Result |
|------|--------|
| AC-1 | PASS (dir absent; grep 0 in `app/src/main` and `app/build.gradle`; `I9-final-greps.txt`) |
| AC-2 | PASS (no `key="donate"`; `onResume` has no donation call; device: no Donate entry, no crash, `I8-device.txt`) |
| AC-3 | PASS (all four manifest diffs pure deletions of exactly the AC-3 set; `I8-` debug/merged and `I9-manifest-diffs.txt`) |
| AC-4 | PASS (trees 0 billingclient; debug dex 0; release mapping prefix count 0) |
| AC-5 | PASS (`final-gates.log` and `I9-gates.log` exit 0 on JDK 25; I9 gate also builds release) |
| AC-6 | PASS (two test classes deleted, 15 `@Test`; `EnrollmentDialogThemeTest` keeps 3 tests with nested `HostActivity`) |
| AC-7 | PASS (697 before, 682 after, 2 skipped; QA re-runs the HEAD `git grep`) |
| AC-8 | PASS (donation rule and Rule 5 note removed; only comment edits otherwise; root 58.6% (17/29); five other values recorded) |
| AC-9 | PASS (metadata unchanged; writer run empty; no relaxing flag) |
| AC-10 | not Developer work - pending |
| AC-11 | PASS (CHANGES entry, README paragraph and bullet removed, code-location line removed, `metadata/play` untouched) |
| AC-12 | PASS (`Keys.DONATE` read-only usage; `reset()` unchanged; lint-baseline billing entry deleted, `0 11`) |
| IC-1 | PASS (baseline evidence; raw logs and files withheld per hygiene) |
| IC-2 | PASS (one increment per turn with a gate each; the plan's I0-I8 plus the approved I9) |
| IC-3 | PASS (isolated 3/3) |
| IC-4 | PASS (writer run empty, scratch copy, deviation 8) |
| IC-5 | PASS (no drift; 12 modules removed) |
| IC-6 | PASS (four diffs, pure deletions) |
| IC-7 | PASS (debug at I8, release at I9, merged release by name) |
| IC-8 | PASS (1)-(4) at I9, read per D6, with the approved `-dontwarn` rule accounted for explicitly |
| IC-9 | not triggered |
| IC-10 | PASS (0) |
| IC-11 | PASS with the product owner's waiver for the setting being ON |
| IC-12 | PASS (697 re-baselined from I0; target 682) |
| IC-13 | PASS (0) |
| IC-14 | not Developer work - pending |

Not verified: AC-10 and IC-14 (not Developer work). The release APK was not run on a device (stated limit in the plan).

## Product-owner decisions carried forward (from the coordinator, recorded at I1)

1. **Play Store visibility:** the product owner ACCEPTS the app losing its declared visibility of the Play Store and will
   delete the dead "Old backup scheduler" setting in a follow-up story (verbatim: "option 1, accept it and delete the dead
   setting later"). At I8: record before/after visibility and the `use_old_scheduler` summary text and enabled state as
   planned; a loss is expected and accepted, so it is not escalated and `<queries>` is never re-added. Also note at I8
   whether anything observable actually changed (I0 found both packages already in the system-wide forceQueryable list on
   this image).
2. **Third-party integration** is still ON on the emulator; it remains a product-owner action or waiver before the I8
   device check. No Developer action.
3. **Scratch fallback for I8:** if the scratch folder holding the raw I0 mapping/configuration/JaCoCo files is gone, rebuild
   them from the code as of e0c51d42 (parent of the I0 commit 8866f805) in a scratch copy outside the repo, and say so here.

## Notes

Appended per increment. See the table above.

## Phase Completion Report
See the final message of the I9 turn (verdict PASS).
