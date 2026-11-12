---
artifact_type: implementation-log
story_id: "U-061"
verdict: ""                        # set at I8 close-out; I0 is baseline only
agent: "Developer"
timestamp: "2026-11-12"
files_changed: 0
files_created: 0
tests_added: 0
tests_passing: 697
---

# Implementation Log: U-061

## Summary

Story U-061 removes the donation flow and the Play Billing dependency. It runs as nine increments (I0-I8), one per
Developer turn, with the orchestrator committing between turns. This log is appended to at the end of each turn.
Status: **I0 complete** (baseline only, no tracked product file changed). I1-I8 pending.

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
(emitted at I8)
