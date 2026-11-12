---
type: story
status: in-progress
artifact_type: user-story
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements:
  - REQ-MODERNIZATION-018
design_docs:
  - DES-MODERNIZATION-013
integration_contracts: []
dependencies: []
sprint: '018'
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-061
title: Remove the donation flow and the Play Billing dependency
pipeline: ''
domain: modernization
alignment_audit: passed
updated_at: '2026-11-12T18:43:43.103Z'
current_build_phase: implementation
---

# U-061: Remove the Donation Flow and the Play Billing Dependency

## Story
As the product owner and sole user of this private, unpublished SMS Backup+ build,
I want the in-app donation flow and the `com.android.billingclient:billing:7.1.1` dependency deleted completely,
So that the app no longer carries a feature I do not want, a Play Billing library that is two major versions behind (latest is 9.1.0 as of 2026-11-12), or the manifest permission, `<queries>` element and library components that come with it, while backup, restore, scheduling and settings behavior (other than the donation entry point) stays exactly as it is.

Product owner decision (verbatim): "remove the donation flow". This story supersedes the intent of REQ-MODERNIZATION-010 (upgrade Play Billing to 7.x).

## Scope and Delivery Shape

One story, ordered increments I0-I8 (DES-MODERNIZATION-013 D1), committed in this order. Each increment must pass `:app:assembleDebug :app:testDebugUnitTest` before the next starts. The JaCoCo gate runs at I0, I6 and I8.

| Inc | Change |
|-----|--------|
| I0 | Capture baseline evidence before any edit (see IC-1). No edit. |
| I1 | Re-host `EnrollmentDialogThemeTest` (D2). `DonationActivity` still exists, so the test source set stops depending on it before it is deleted. |
| I2 | Remove entry points: the `<Preference key="donate">` block (preferences.xml lines 319-327), `MainSettings` lines 14, 15, 24, 75 and 163-182, `Keys.DONATE` (Preferences.java:101), manifest line 129. |
| I3 | Delete `app/src/main/.../activity/donation/`, `app/src/test/.../activity/donation/` and `Consts.Billing` (Consts.java lines 36-50) together. |
| I4 | Remove the donation strings and both comments from all 22 `strings.xml` files in one commit, by `name` attribute (`warningsAsErrors true`). |
| I5 | Remove the dependency (build.gradle line 132). Reword the BuildConfig comment (line 43) to name only `App.java`. Delete the stale `lint-baseline.xml` entry (lines 114-123). |
| I6 | Retire the JaCoCo rule (D3). |
| I7 | Docs (D7). |
| I8 | After-evidence and equivalence checks: D5 manifest diffs, then the D6 checks and the Design Validation on-device check. No edit except the root-rule JaCoCo comment value (AC-8). |

## Acceptance Criteria

Numbered AC-1 to AC-12 are carried from REQ-MODERNIZATION-018 (as amended by CR-003) with their numbers. IC-n criteria follow the design.

- [ ] AC-1: The directory `app/src/main/java/com/zegoggles/smssync/activity/donation/` does not exist, and `grep -rniE "billing|donat" app/src/main app/build.gradle` returns zero results. The one expected exception is `app/src/main/java/com/zegoggles/smssync/compat/GooglePlayServices.java`, whose `com.android.vending` package-name constant matches neither term. Because the grep covers all strings files, AC-1 also proves that items 3-9 of the inventory are removed from every locale.
- [ ] AC-2: Items 2-6 of the inventory are removed. After removal, `MainSettings.onResume()` no longer calls any donation-status check, and `preferences.xml` contains no `key="donate"`. The settings screen shows no entry point that leads to a missing screen, and no `Preference` references a class that does not exist.
- [ ] AC-3: For both the debug and the release build, a fresh manifest differs from the pre-change manifest only by the removal of the following, and nothing else is added, removed or changed. The check uses the merged manifest (`./gradlew :app:processDebugManifest` and `:app:processReleaseManifest`) and the packaged manifest (`aapt2 dump xmltree --file AndroidManifest.xml`, with `(line=N)` stripped and app resource IDs (`@0x7f[0-9a-f]{6}`) normalised before diffing; renumbered app resource IDs are not a difference, because the merged-manifest diff checks those references by name):
  - the `com.android.vending.BILLING` permission
  - the whole `<queries>` element (both billing intents)
  - the `com.google.android.play.billingclient.version` meta-data
  - the `ProxyBillingActivity` and `ProxyBillingActivityV2` activities
  - the `.activity.donation.DonationActivity` activity
  - from billing's transitive libraries, which nothing else in the app uses: `com.google.android.gms.common.api.GoogleApiActivity`; the `com.google.android.gms.version` meta-data; `com.google.android.datatransport.runtime.backends.TransportBackendDiscovery` (with its `backend:` meta-data); `com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService`; `com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver`

  `INTERNET` and `ACCESS_NETWORK_STATE` remain, because the app declares both itself.
- [ ] AC-4: `./gradlew :app:dependencies --configuration debugRuntimeClasspath` and `releaseRuntimeClasspath` list no `com.android.billingclient` coordinate. An `unzip -l` or `apkanalyzer dex packages` listing of the debug APK contains no `com/android/billingclient` classes and no `com.zegoggles.smssync.activity.donation` classes.
- [ ] AC-5: On JDK 25, `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0.
- [ ] AC-6: Tests deleted are exactly `DonationActivityTest` (10 `@Test`) and `SkuTest` (5 `@Test`), 15 in total. `EnrollmentDialogThemeTest` keeps all 3 `@Test` methods with their assertions unchanged. Only its host activity changes from `DonationActivity` to a test-only subclass of the production `ThemeActivity`, declared as a concrete `public static` nested class of that test (`EnrollmentDialogThemeTest.HostActivity`; `ThemeActivity` is abstract). The import, the local-variable type `DonationActivity` and the `DonationActivity.class` argument on lines 117, 179 and 244, and the javadoc and comment references to `DonationActivity` (lines 114-116 and 164-167) are corrected to match, and both corrected passages state that the AppCompat theme comes from `ThemeActivity.onCreate()`, not from the manifest. No other test is deleted, ignored or weakened.
- [ ] AC-7: The new test baseline is 682 run (697 minus 15) with 2 skipped (the `@Ignore`d `AppTest.shouldGetVersionName` and the `@Ignore`d AndroidKeyStore test in `AuthPreferencesTest`; neither is a donation test). Confirm the run count with the Gradle test report and cross-check with the CLAUDE.md `git grep` command: 15 fewer `@Test` than the pre-change `HEAD`. Record the before and after counts in the story's implementation log.
- [ ] AC-8: JaCoCo retirement:
  - The `activity.donation*` rule (floor 0.30) is deleted together with its subject package, and the `activity.donation=33.5%` note is removed from the Rule 5 comment.
  - No other `limit`/`minimum` value in `app/build.gradle` changes. The only permitted edits to the JaCoCo block are: deleting the donation rule (including its own comment lines) and its Rule 5 note, and updating the root-package rule's comments with the re-measured value (third bullet). Nothing else in the block is edited; other re-measured values go in the implementation log.
  - The `com.zegoggles.smssync` root-package rule (floor 0.50, last measured 60.0%, 18/30) must be re-measured after `Consts.Billing` is removed, because `Consts` sits in that package. The new measured value is recorded in the root-package rule's comments in `app/build.gradle` (the "Rule 4" header comment, about lines 397-399, and the trailing `// MEASURED` comment on its `minimum` line, about line 406; either or both may be updated, and they are the only comment edits besides the Rule 5 note and the donation rule's own comments) and must still be at least 50%. If it is below 50% the floor is not lowered. Instead, coverage is added until the rule passes, and the shortfall is escalated.
  - The `activity` (0.10), `activity.fragments` (0.10), `receiver` (0.50), `preferences` (0.65) and `utils` (0.10) rules are re-measured and must still pass without their floors being edited. `MainSettings` is in `activity.fragments`, where removing `checkUserDonationStatus()` reduces both covered and total lines. `Keys.DONATE` is a covered line in `preferences`. These five re-measured values are recorded in the story's implementation log, not in `app/build.gradle`.
- [ ] AC-9: Dependency verification stays enforced: no `verification-mode`, `<verify-metadata>`, or `<verify-signatures>` setting in `gradle/verification-metadata.xml` is relaxed, and no `--dependency-verification=off` or `lenient` flag is added anywhere. Policy for removed artifacts: the `billing` 7.1.1 entries (line 931) and any now-unreferenced transitive entries may remain, because stale entries do not weaken verification. They are removed only if `./gradlew --write-verification-metadata sha256 help` run in a branch produces that removal with no additions. Hand-editing checksums is prohibited. The build passes with the entries either kept or removed.
- [ ] AC-10: (Owner: Artifact Librarian / Requirements Analyst, not the developer. Must be done before the story closes, DES-MODERNIZATION-013 D8.) REQ-MODERNIZATION-010 is recorded as superseded through a change record, not silently edited. A `change_records` entry on REQ-MODERNIZATION-010 states that its intent is superseded by REQ-MODERNIZATION-018 (decision date 2026-11-12, product owner decision "remove the donation flow"). Its acceptance criteria 1-4 are marked not applicable. REQ-MODERNIZATION-018 carries the reciprocal reference, and `DES-MODERNIZATION-010` is annotated as superseded.
- [ ] AC-11: `CHANGES` has a new entry under the unreleased 1.6.0 heading, in the existing bullet style, stating that the donation feature and the in-app billing dependency were removed. `README.md` no longer contains the donation paragraph (lines 43-44) or the billing permission bullet (line 241), and the sentence at lines 40-41 about there never being a paid version is retained. `sdlc/artifacts/engagement/code-location.md` line 37 is updated. `metadata/play/**` is inherited upstream store text and is out of scope: it is not edited.
- [ ] AC-12: No `SharedPreferences` key is migrated, written or deleted. The enum constant `Keys.DONATE` is removed, and no stored value exists for it (see Context in REQ-MODERNIZATION-018), so no cleanup code or schema version change is required. A unit test or reviewed code evidence confirms that `Preferences` never persisted `donate`. The removal does not clear user preferences, and `Preferences.reset()` behavior is unchanged. The `app/lint-baseline.xml` billing entry (inventory item 12) is deleted, with no other baseline entry edited.

### Integration Criteria

These come from the design (DES-MODERNIZATION-013), not the requirement. No new production component is created. The only new class is the test-only `HostActivity` (IC-3).

- [ ] IC-1: (D1 I0) Before any edit, the following baseline evidence is captured under `sdlc/artifacts/build/sprints/<sprint>/U-061/evidence/` (naming per D5): merged debug and release manifests from `./gradlew :app:processDebugManifest :app:processReleaseManifest`, copied from `app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml` and `app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml` as `before-merged-{debug|release}.xml` (confirm the release path exists with `ls`); packaged debug and release manifests from `aapt2 dump xmltree --file AndroidManifest.xml` as `before-manifest-{debug|release}.txt`; release `mapping.txt` and `configuration.txt`; the `debugRuntimeClasspath` and `releaseRuntimeClasspath` dependency trees; Gradle test totals; the JaCoCo XML; and the CLAUDE.md `git grep` `@Test` count. Evidence is text only.
- [ ] IC-2: (D1) Each increment I1 to I7 is committed separately in order, and `:app:assembleDebug :app:testDebugUnitTest` passes after each. `:app:jacocoTestCoverageVerification` is run at I0, I6 and I8. The single I8 edit (the root-rule JaCoCo comment in `app/build.gradle`, AC-8) is its own commit, followed by the final gate run (`:app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`).
- [ ] IC-3: (D1 I1, risk R4) After I1 and before I2, `EnrollmentDialogThemeTest` is run in isolation and passes 3/3 with `HostActivity` as host. `HostActivity` is `public static class HostActivity extends ThemeActivity {}` nested in the test, is not registered anywhere (manifest or otherwise), and is not a top-level helper class, so it stays under the JaCoCo `**/*Test*.*` filter. Each of the three call sites reads `HostActivity activity = Robolectric.buildActivity(HostActivity.class).create().get()`, and `{@link DonationActivity}` becomes `{@link HostActivity}`. No assertion changes.
- [ ] IC-4: (D4) `gradle/verification-metadata.xml` is left unchanged and its `<configuration>` block (`verify-metadata=true`, `verify-signatures=false`) is not touched. `./gradlew --write-verification-metadata sha256 help` is run once on a scratch branch purely as evidence. Its result is accepted only if it is a removal with no additions. The result is recorded in the implementation log.
- [ ] IC-5: (D4a) At I5, the resolved `debugRuntimeClasspath` and `releaseRuntimeClasspath` trees are diffed against the I0 trees. If any module that remains on the classpath resolves to a different version, stop and escalate before continuing. A `constraints {}` pin is an option to put to the product owner and is not pre-approved.
- [ ] IC-6: (D5) At I8, the merged and packaged manifests are rebuilt for both variants and compared against their own I0 baselines (four diffs). Packaged-manifest normalisation: `norm() { sed -E 's/ ?\(line=[0-9]+\)//g; s/@0x7f[0-9a-f]{6}/@0x7fRESID/g' "$1"; }`, then `diff <(norm before-manifest-debug.txt) <(norm after-manifest-debug.txt)` and the same for release, using `aapt2` from build-tools 36.0.0 (`$LOCALAPPDATA/Android/Sdk/build-tools/36.0.0/aapt2`). Merged diffs are plain `diff before-merged-{variant}.xml after-merged-{variant}.xml`. Every diff consists only of removed lines, and removed lines belonging to one element count as that element. Any other line is a failure to explain or escalate, never to suppress.
- [ ] IC-7: (D5, unchanged assertions) In the after manifests the following are unchanged: `package` and `versionCode=1602`; `BackupBroadcastReceiver` with exported=true, no permission, and the `com.zegoggles.smssync.BACKUP` action (CNTR-MODERNIZATION-005); `SystemForegroundService` with `foregroundServiceType=0x1`; no `InitializationProvider`; `networkSecurityConfig` and `fullBackupContent` on `<application>`; both `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` entries.
- [ ] IC-8: (D6, release mapping check) At I8, on the release build: (1) in the release `mapping.txt`, `grep -cE '^(com\.android\.billingclient|com\.android\.vending\.billing|com\.google\.android\.(gms|datatransport)|com\.google\.firebase\.encoders|com\.zegoggles\.smssync\.activity\.donation)\.'` returns 0 (U-060 had 98 `com.android.billingclient` entries); (2) `apkanalyzer dex packages --defined-only` on the debug APK lists no `com.android.billingclient` or `activity.donation` package; (3) the U-060 C-2 kept-name check `comm -23 kept-before.txt kept-after.txt` prints exactly one line, `com.zegoggles.smssync.activity.donation.DonationActivity`; (4) every line in the `configuration.txt` diff is one of the removals listed in AC-3. Any other kept name that disappears is a release regression: stop and escalate.
- [ ] IC-9: (D3 breach routing) If any remaining JaCoCo rule falls below its floor, a characterization test for real behavior in that package is added until the rule passes and the shortfall is escalated in the implementation log. Lowering a floor, adding an exclusion, or renaming a helper so the `*Test*` filter catches it is forbidden.
- [ ] IC-10: (D2 call-site audit) After I2 and I3, `grep -rn "DonationActivity\|Consts.Billing\|Keys.DONATE\|DONATE" app/src` finds no reference to a removed symbol. `Keys.DONATE` is shown by reviewed evidence to have been used only at `MainSettings` (lines 24 and 172 before removal).
- [ ] IC-11: (Design Validation, on-device check) Install the debug APK on the API 36 emulator AVD `Pixel_9_Pro` with `adb install -r`, and open Settings. Text evidence only: the `uiautomator dump` output contains no `Donate` title; the last visible entry is the final `PreferenceScreen`; `logcat` shows 0 `FATAL EXCEPTION` across launch, Settings and back. Third-party integration is confirmed off before the run (sprint-017 broadcast incident).
- [ ] IC-12: (D1 count rule) If another story lands first and changes the test count, re-baseline from the I0 Gradle report and `git grep` count instead of reusing 697.
- [ ] IC-13: (AC-1 proof gap, inventory item 7) After I4, `grep -rn "purchase errors" app/src/main/res` returns zero results. The `<!-- purchase errors -->` comment (present in 18 strings files, e.g. `app/src/main/res/values/strings.xml:346`, `app/src/main/res/values-uk/strings.xml:224`) matches neither "billing" nor "donat", so the AC-1 grep cannot detect it if left behind.
- [ ] IC-14: (D8 supersession records; performed by the Artifact Librarian / Requirements Analyst, not the developer; the orchestrator dispatches it in sprint-018; complete before the story closes) (a) A supersession change record exists and is listed in REQ-MODERNIZATION-010's `change_records`, stating that its intent is superseded by REQ-MODERNIZATION-018 (decision date 2026-11-12). (b) The same change record id is listed in REQ-MODERNIZATION-018's `change_records` alongside CR-003. (c) DES-MODERNIZATION-010 carries a superseded annotation that names DES-MODERNIZATION-013.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/activity/donation/` (`DonationActivity`, `DonationListFragment`, `Sku`) | Donation UI and Play Billing client | Delete directory (I3) |
| `app/src/test/java/com/zegoggles/smssync/activity/donation/` (`DonationActivityTest` 10 `@Test`, `SkuTest` 5 `@Test`) | Tests the donation package | Delete directory (I3) |
| `app/src/test/java/com/zegoggles/smssync/activity/fragments/EnrollmentDialogThemeTest.java` | Uses `DonationActivity` as AppCompat host at lines 117, 179, 244; import at line 9; stale comments at 114-116 and 164-167 | Add nested `HostActivity extends ThemeActivity`; swap import, type and class argument; fix comments (I1) |
| `app/src/main/AndroidManifest.xml` | Declares `.activity.donation.DonationActivity` (line 129) | Remove entry (I2) |
| `app/src/main/res/xml/preferences.xml` | `<Preference android:key="donate">` with intent to `DonationActivity` (lines 319-327) | Remove block (I2) |
| `app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java` | `checkUserDonationStatus()` called from `onResume()`; `DonationActivity` imports and `DONATE` static import | Remove; `onResume()` becomes `super.onResume(); updateAutoBackupPreferences(); addPreferenceListener(ENABLE_AUTO_BACKUP.key);` (I2) |
| `app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java` | `Keys.DONATE("donate")` (line 101) | Remove constant (I2). `reset()` unchanged |
| `app/src/main/java/com/zegoggles/smssync/Consts.java` | `Consts.Billing` (`DONATION_PREFIX`, `SKU_DONATION_1..3`, `ALL_SKUS`), lines 36-50 | Remove (I3) |
| `app/src/main/res/values/strings.xml` and `app/src/main/res/values-{ca,da,de,fr,gl,gr,hu,it,ko,nl,pl,pt-rPT,ru,sk,sq,sr,sv,tr,uk,zh-rCN,zh-rTW}/strings.xml` (22 files) | Strings `donate`, `donate_summary`, `ui_dialog_donate_message`, `ui_donation_success_message`, `ui_donation_failure_message`, `donation_error_iab_unavailable`, `donation_error_unavailable`, `donation_error_canceled`, `donation_error_already_owned`, `donation_unspecified_error`, plus the `<!-- donation related -->` and `<!-- purchase errors -->` comments (default file lines 338-351; lines to remove per file range from 3 to 12; in the default file that is 10 strings plus 2 comments. This is a count of lines to remove, not of `grep -iE "billing|donat"` matches, which is 11 in the default file because `<!-- purchase errors -->` matches neither term) | Remove all, by `name` attribute, in one commit (I4) |
| `app/build.gradle` | `implementation 'com.android.billingclient:billing:7.1.1'` (line 132); BuildConfig comment naming `DonationActivity` (line 43); JaCoCo rule `includes = ['com.zegoggles.smssync.activity.donation*']`, floor 0.30 (lines 411, 435-444) | Remove dependency and reword comment (I5); delete rule, its comments and Rule 5 note; update root-rule comments with re-measured value (I6, I8) |
| `app/lint-baseline.xml` | GradleDependency entry for billing 2.1.0, stale (lines 114-123) | Delete entry only (I5) |
| `gradle/verification-metadata.xml` | `billing` 7.1.1 `.aar` and `.pom` (line 931) plus billing-only transitives | Unchanged (D4) |
| `README.md` | Donation paragraph (lines 43-44), billing permission bullet (line 241) | Delete both; keep lines 40-41 (I7) |
| `CHANGES` | Unreleased 1.6.0 heading | Add one `  * ` bullet; do not edit lines 23, 120, 138, 168 (I7) |
| `sdlc/artifacts/engagement/code-location.md` | Line 37 billing bullet | Delete (I7) |
| `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-010-upgrade-play-billing-to-v7.md`, `sdlc/artifacts/design/modernization/DES-MODERNIZATION-010-play-billing-v7-design.md` | Approved, describe the superseded upgrade | Supersession change record and annotation (AC-10, Librarian/Analyst) |

## Existing Behavior to Preserve

- Backup, restore, scheduler, notification and authentication behavior is unchanged.
- `ThemeActivity`, `Dialogs.BaseFragment` and `BundleBuilder` stay (`BundleBuilder` is used by `MainActivity` and `AccountManagerAuthActivity`). `BuildConfig` stays (used by `App.java`). `compat/GooglePlayServices.java` stays.
- `MainSettings.onResume()` still calls `super.onResume()`, `updateAutoBackupPreferences()` and `addPreferenceListener(ENABLE_AUTO_BACKUP.key)`.
- `Preferences.reset()` behavior is unchanged. No `SharedPreferences` key is migrated, written or deleted.
- Manifest items listed in IC-7 are unchanged, including the exported `BACKUP` receiver (CNTR-MODERNIZATION-005), `networkSecurityConfig`, `fullBackupContent`, and `INTERNET` and `ACCESS_NETWORK_STATE` (declared by the app at `app/src/main/AndroidManifest.xml` lines 56 and 61).
- `EnrollmentDialogThemeTest` keeps its 3 `@Test` methods and assertions.
- Every surviving dependency module resolves to the same version as at I0 (IC-5).
- Every JaCoCo `limit`/`minimum` value outside the retired rule is unchanged.
- `gradle/verification-metadata.xml`, `metadata/play/**`, and `app/proguard-rules.pro` are unchanged.

## Verification Steps

Run Gradle in a shell without `MSYS_NO_PATHCONV=1` set. Set `JAVA_HOME` per CLAUDE.md first (the daemon JVM is declared by `gradle/gradle-daemon-jvm.properties`).

1. AC-1: `ls app/src/main/java/com/zegoggles/smssync/activity/donation/` fails with not found. `grep -rniE "billing|donat" app/src/main app/build.gradle` returns zero results. Check it also returns nothing in each of the 22 strings files. Also run `grep -rn "purchase errors" app/src/main/res` and confirm zero results (IC-13; this comment is invisible to the AC-1 grep).
2. AC-2: `grep -n 'key="donate"' app/src/main/res/xml/preferences.xml` returns nothing. Read `MainSettings.onResume()` and confirm no donation-status call. Grep `app/src/main` for `DonationActivity` and confirm zero hits. Confirm the on-device check (IC-11).
3. AC-3: Perform IC-1 at I0 and IC-6 at I8. Confirm the four diffs show only removals from the AC-3 list and IC-7 items are unchanged.
4. AC-4: Run `./gradlew :app:dependencies --configuration debugRuntimeClasspath` and `releaseRuntimeClasspath`, and grep for `com.android.billingclient` (zero). Run `unzip -l` or `apkanalyzer dex packages` on the debug APK and grep for `com/android/billingclient` and `com.zegoggles.smssync.activity.donation` (zero).
5. AC-5: Run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` and confirm exit code 0.
6. AC-6: Diff `EnrollmentDialogThemeTest.java` against HEAD and confirm only the host class, import, type, class argument and the comments at lines 114-116 and 164-167 change, with both corrected passages naming `ThemeActivity.onCreate()`. Confirm the two deleted test files and no other deleted, `@Ignore`d or weakened test.
7. AC-7: Read run and skipped totals from `app/build/test-results/testDebugUnitTest/` (682 run, 2 skipped). Run `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` before and after, and confirm a drop of exactly 15. Record both in the implementation log.
8. AC-8: Diff `app/build.gradle` and confirm the only JaCoCo-block edits are the donation rule with its comment lines, the `activity.donation=33.5%` Rule 5 note, and the root-rule comments. Read per-package figures from `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`. Confirm the root value is at least 50%, and record the five other values in the implementation log.
9. AC-9: `git diff gradle/verification-metadata.xml` is empty (or removal-only if produced by the writer per IC-4). Grep the repo diff for `dependency-verification=off` and `lenient` (zero).
10. AC-10 (and IC-14): Confirm `change_records` on REQ-MODERNIZATION-010 states the supersession (2026-11-12, "remove the donation flow") and marks AC-1 to AC-4 not applicable, that REQ-MODERNIZATION-018 carries the reciprocal reference, and that DES-MODERNIZATION-010 has the superseded annotation. Check all three by grep: (a) `grep -n "change_records" -A4 sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-010-*.md` shows the supersession change record id; (b) `grep -n "change_records" -A4 sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-018-*.md` shows the same id alongside `CR-003`; (c) `grep -n "DES-MODERNIZATION-013" sdlc/artifacts/design/modernization/DES-MODERNIZATION-010-*.md` finds the superseded annotation in its frontmatter or notes.
11. AC-11: Diff `README.md`, `CHANGES` and `sdlc/artifacts/engagement/code-location.md`. Confirm lines 40-41 of README are still present. Confirm `git diff --stat -- metadata/play` is empty.
12. AC-12: Review `Preferences.java` and confirm `Keys.DONATE` was referenced only at `MainSettings` and that `reset()` is unchanged. Confirm only the billing entry of `app/lint-baseline.xml` is deleted.
13. IC-1 to IC-12: check the implementation log and evidence folder for each item.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java, Gradle) | I0 to I8: code, resource, build, test and doc deletions; evidence capture | Developer |
| Artifacts | AC-10 and IC-14 supersession records on REQ-MODERNIZATION-010, REQ-MODERNIZATION-018 and DES-MODERNIZATION-010. The Developer pipeline will not do this: the orchestrator must dispatch this artifact work in sprint-018 | Artifact Librarian / Requirements Analyst |

## Technical Context

- The app is private and unpublished. Product owner, verbatim: "I don't own the old app store listing. this is just going to be a private app for me". `metadata/play/**` is untouched and out of scope.
- Build is Gradle 9.5.1 / AGP 9.0.1 / Kotlin 2.3.21 on JDK 25 declared by `gradle/gradle-daemon-jvm.properties`. Gates per CLAUDE.md: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`.
- Never run `./gradlew` with `MSYS_NO_PATHCONV=1` set. Scope that variable to adb-only shells.
- Dependency verification stays enforced and `gradle/verification-metadata.xml` is left unchanged (D4). Stale entries to be left: `billing` 7.1.1 (line 931) and 11 billing-only transitives (datatransport lines 1859-1875, gms lines 1883-1915, firebase-encoders lines 2530-2546).
- No JaCoCo floor is lowered (REQ-MODERNIZATION-003).
- Removing `billing` removes its transitive libraries (`com.google.android.gms:*`, `com.google.android.datatransport:*`, `com.google.firebase:firebase-encoders*`), which sit under `billing:7.1.1` and nowhere else in the U-060 trees. Hence the five extra manifest removals in AC-3.
- `EnrollmentDialogThemeTest` gets its AppCompat theme from `ThemeActivity.onCreate()` (`setTheme(new Preferences(this).getAppTheme())`), not from the manifest. The AGP 9 unit-test manifest declares no app components, and Robolectric 4.15.1 launches undeclared activities. Rejected hosts: `AccountManagerAuthActivity`, `OAuth2WebAuthActivity`, `MainActivity` (needs Hilt rule), a plain `AppCompatActivity` (no AppCompat theme).
- JaCoCo counts non-`*Test*` test classes as production coverage. A nested class compiles to `EnrollmentDialogThemeTest$HostActivity.class` and is filtered. A top-level helper would not be.
- The on-device check uses text evidence only (no screenshots, no account names) on the API 36 emulator AVD `Pixel_9_Pro`, installing with `adb install -r` and never uninstalling or clearing app data. adb screenshots are not used. `adb` commands with absolute Unix paths need `MSYS_NO_PATHCONV=1` in their own shell.
- Predicted JaCoCo values in DES-013 D3 are estimates. Only the I8 re-measurement is binding.
- Rollback is `git revert` of the story commits. Nothing is persisted or migrated.
- Scope Concern: AC-1 describes `GooglePlayServices.java` as an expected exception while stating it matches neither term. If it does not match, it is not an exception. Carry out AC-1 as written (zero results) and note any match in the implementation log.
- Scope Concern: DES-013 body passages describing the pre-amendment requirement (Context facts 1 and 5, D2 closing paragraph, R3, Notes SC-1 to SC-3) are historical. The amended REQ-MODERNIZATION-018 is authoritative. For example, the requirement says per-file string-line counts range from 3 to 12 and requires the merged-manifest diff for both variants.
- Scope Concern: REQ-MODERNIZATION-018 AC-1's sentence "AC-1 also proves that items 3-9 of the inventory are removed from every locale" overclaims for the `<!-- purchase errors -->` comment (inventory item 7), which matches neither "billing" nor "donat". IC-13 closes the gap with its own grep. The requirement wording will be corrected in a follow-up change record; AC-1's text in this story stays identical to the requirement.
- Scope Concern: line numbers in ACs are at HEAD d931fde8 and shift as increments land. Locate by symbol where they drift.

### Estimation Guidance

Complexity: medium. The code change is deletion with one small test re-host, but it touches 38 files (33 edited: 22 strings files, manifest, `preferences.xml`, `MainSettings`, `Preferences`, `Consts`, `build.gradle`, `lint-baseline.xml`, `EnrollmentDialogThemeTest`, `README.md`, `CHANGES`, `code-location.md`; 5 deleted: `DonationActivity`, `DonationListFragment`, `Sku`, `DonationActivityTest`, `SkuTest`) and carries heavy evidence work (four manifest diffs, release mapping check, dependency-tree diff, six JaCoCo re-measurements, on-device check). Fits one sprint. Risk concentrates in the `receiver` rule (50.0%, zero headroom), surviving-module version drift (IC-5) and any unexplained manifest diff line.

## Supporting Documentation
- REQ-MODERNIZATION-018 (as amended by CR-003): Description, Context (inventory items 1-16), AC-1 to AC-12, Constraints, Notes
- REQ-MODERNIZATION-010 (superseded intent), REQ-MODERNIZATION-003 (JaCoCo floors)
- DES-MODERNIZATION-013: D1 (increments), D2 (HostActivity), D3 (JaCoCo), D4 and D4a (verification, drift guard), D5 (manifest equivalence), D6 (release/R8), D7 (docs), D8 (supersession), Design Validation (test-count arithmetic, on-device check, rollback)
- DES-MODERNIZATION-010 (superseded design)
- CR-003 (`sdlc/artifacts/build/change-records/CR-003-req-018-manifest-scope-private-app.md`)
- Evidence: `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/` (`baseline-manifest-{debug,release}.txt`, `after-manifest-{debug,release}.txt`, dependency trees, `after-configuration.txt`, `kept-baseline.txt`, `kept-after.txt`). Note: U-060's pre-change kept-name file is `kept-baseline.txt`; `kept-before.txt` in IC-8 denotes U-061's own I0 capture of the same list, not a U-060 file
- Related contracts, asserted unchanged: CNTR-MODERNIZATION-001 to 005

## Integration Contract References
None. DES-MODERNIZATION-013 states that no interface is introduced or altered and no approved contract mentions billing or donation. CNTR-MODERNIZATION-001/002/003/004/005 surfaces in the manifest and R8 keep set are asserted unchanged by IC-6, IC-7 and IC-8.

## Implementation Notes
<!-- Added by agents during build. Record: before/after test counts (AC-7); the five re-measured JaCoCo values (AC-8); the IC-4 scratch-run result; IC-5 tree diff result. -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes
- Dependencies between stories: none. REQ-MODERNIZATION-018 should land before the platform-level (compileSdk/targetSdk) requirement under EPIC-MODERNIZATION-006.
- The 697 figure is the sprint-017 baseline. The 682 target assumes no other test changes land first (IC-12).
- Sibling wording follow-up (EPIC-MODERNIZATION-006, REQ-MODERNIZATION-015 AC-10, REQ-MODERNIZATION-017) is tracked separately under CR-003 and is not part of this story.
