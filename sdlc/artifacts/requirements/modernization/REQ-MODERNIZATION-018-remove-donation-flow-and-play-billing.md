---
status: approved
type: functional
artifact_type: requirement
related_requirements:
  - REQ-MODERNIZATION-010
  - REQ-MODERNIZATION-003
related_stories: []
related_design_docs:
  - DES-MODERNIZATION-010
  - DES-MODERNIZATION-013
traces_to:
  - EPIC-MODERNIZATION-006
change_records:
  - CR-003
  - CR-004
id: REQ-MODERNIZATION-018
title: Remove the Donation Flow and the Play Billing Dependency
domain: modernization
epic: EPIC-MODERNIZATION-006
priority: medium
notes: 'Supersedes the intent of REQ-MODERNIZATION-010. Product owner decision (verbatim): ''remove the donation flow''. Epic sequencing note: this requirement should land before the platform-level (compileSdk/targetSdk) requirement.'
updated_at: '2026-11-12T16:49:41.783Z'
---

# Remove the Donation Flow and the Play Billing Dependency

## Description

Delete the in-app donation flow completely, including `com.android.billingclient:billing:7.1.1`,
instead of upgrading Play Billing. As of 2026-11-12 the latest Play Billing release is 9.1.0, so
7.1.1 is two major versions behind. The product owner's decision, verbatim, is "remove the donation
flow". This requirement supersedes the intent of REQ-MODERNIZATION-010 (upgrade Play Billing to 7.x).
Backup, restore, scheduling, and settings behavior other than the donation entry point must not change.

## Context

The app has no paid tier; billing exists only for optional donations. The repo was read on
2026-11-12 and the donation flow consists of the items below. The list is the verified inventory, and
the grep in AC-1 is the completeness check.

| # | Item | Path |
|---|------|------|
| 1 | `DonationActivity`, `DonationListFragment`, `Sku` (the whole package) | `app/src/main/java/com/zegoggles/smssync/activity/donation/` |
| 2 | Manifest activity entry `.activity.donation.DonationActivity` | `app/src/main/AndroidManifest.xml` (line 129) |
| 3 | Settings entry point: `<Preference android:key="donate">` with an intent targeting `DonationActivity` | `app/src/main/res/xml/preferences.xml` (lines 319-327) |
| 4 | Entry-point handler: `checkUserDonationStatus()`, called from `onResume()`, plus the `DonationActivity` imports and the `DONATE` static import | `app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java` |
| 5 | `Keys.DONATE("donate")` enum constant | `app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java` (line 101) |
| 6 | `Consts.Billing` (`DONATION_PREFIX`, `SKU_DONATION_1..3`, `ALL_SKUS`) | `app/src/main/java/com/zegoggles/smssync/Consts.java` (lines 36-50) |
| 7 | Strings `donate`, `donate_summary`, `ui_dialog_donate_message`, `ui_donation_success_message`, `ui_donation_failure_message`, `donation_error_iab_unavailable`, `donation_error_unavailable`, `donation_error_canceled`, `donation_error_already_owned`, `donation_unspecified_error`, and the `<!-- donation related -->` and `<!-- purchase errors -->` comments, in the default file and 21 locale files (22 files; per-file counts of matching lines range from 3 to 12) | `app/src/main/res/values/strings.xml` (lines 338-351) and `app/src/main/res/values-{ca,da,de,fr,gl,gr,hu,it,ko,nl,pl,pt-rPT,ru,sk,sq,sr,sv,tr,uk,zh-rCN,zh-rTW}/strings.xml` |
| 8 | Dependency `implementation 'com.android.billingclient:billing:7.1.1'` and the `BuildConfig` comment naming `DonationActivity` | `app/build.gradle` (lines 132 and 43) |
| 9 | JaCoCo rule `includes = ['com.zegoggles.smssync.activity.donation*']`, floor 0.30 (measured 33.5%, 79/236), and the `activity.donation=33.5%` note in the Rule 5 comment | `app/build.gradle` (lines 411 and 435-444) |
| 10 | Tests: `DonationActivityTest` (10 `@Test`), `SkuTest` (5 `@Test`) | `app/src/test/java/com/zegoggles/smssync/activity/donation/` |
| 11 | `EnrollmentDialogThemeTest` (3 `@Test`) uses `DonationActivity` as its AppCompat host activity at lines 117, 179 and 244. It is not a donation test and must be kept, so it needs a replacement host. | `app/src/test/java/com/zegoggles/smssync/activity/fragments/EnrollmentDialogThemeTest.java` |
| 12 | Lint baseline entry (GradleDependency for billing 2.1.0, stale) | `app/lint-baseline.xml` (lines 114-123) |
| 13 | Verification metadata for `billing` 7.1.1 (`.aar` and `.pom`) | `gradle/verification-metadata.xml` (line 931) |
| 14 | Docs: the donation paragraph and the "Google Play billing service" permission bullet | `README.md` (lines 43-44 and 241) |
| 15 | (Withdrawn by CR-003. Store listing text under metadata/play/** is inherited upstream text for a listing the product owner does not own, and is out of scope and untouched.) | none |
| 16 | Engagement doc line: "`com.android.billingclient:billing` - in-app donations" | `sdlc/artifacts/engagement/code-location.md` (line 37) |

Facts verified as absent:

- There is no `com.android.vending.BILLING` permission in `app/src/main/AndroidManifest.xml`.
- There are no donation-specific layouts, drawables, or menu XML.
- There are no billing rules in `app/proguard-rules.pro`.
- `CHANGES` mentions only historical releases (lines 23, 120, 138, 168) and has no 1.6.0 entry about donations.
- `DEVELOPMENT.md` has no billing or donation mentions.

The library's AAR manifest contributes the following to the merged debug manifest at
`app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml`:

- the `com.android.vending.BILLING` permission (line 115)
- two `<queries>` intents for `com.android.vending.billing.InAppBillingService.BIND` and `com.google.android.apps.play.billingtestcompanion.BillingOverrideService.BIND`
- the `com.google.android.play.billingclient.version` meta-data
- the activities `com.android.billingclient.api.ProxyBillingActivity` and `ProxyBillingActivityV2`

Removing `billing` also removes its transitive libraries (`com.google.android.gms:*`,
`com.google.android.datatransport:*`, `com.google.firebase:firebase-encoders*`), which appear under
`billing:7.1.1` and nowhere else in the U-060 runtime dependency trees. Those libraries contribute five more entries to the
merged manifest in both debug and release builds: the activity
`com.google.android.gms.common.api.GoogleApiActivity`; the `com.google.android.gms.version` meta-data; the service
`com.google.android.datatransport.runtime.backends.TransportBackendDiscovery` with its
`backend:com.google.android.datatransport.cct.CctBackendFactory` meta-data; the service
`com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService`; and the receiver
`com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver`.
The two `<queries>` intents above are the only children of the `<queries>` element, so the whole element is removed.
`INTERNET` and `ACCESS_NETWORK_STATE` stay, because the app declares both itself
(`app/src/main/AndroidManifest.xml` lines 56 and 61). The merged manifest is a local build artifact, so the
check in AC-3 must be re-run on a fresh build. Evidence: `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/`
(`baseline-manifest-{debug,release}.txt`).

Persisted donation state: none was found. The `donate` preference is an intent-only `Preference` with no
stored value. `Keys.DONATE` was never stored, and searches of `app/src/main/java/com/zegoggles/smssync/preferences`
found no code that reads or writes a donation value. `Preferences.reset()` does not touch it.

## Acceptance Criteria

1. The directory `app/src/main/java/com/zegoggles/smssync/activity/donation/` does not exist, and `grep -rniE "billing|donat" app/src/main app/build.gradle` returns zero results. The one expected exception is `app/src/main/java/com/zegoggles/smssync/compat/GooglePlayServices.java`, whose `com.android.vending` package-name constant matches neither term. Because the grep covers all strings files, AC-1 also proves that items 3-9 of the inventory are removed from every locale.
2. Items 2-6 of the inventory are removed. After removal, `MainSettings.onResume()` no longer calls any donation-status check, and `preferences.xml` contains no `key="donate"`. The settings screen shows no entry point that leads to a missing screen, and no `Preference` references a class that does not exist.
3. For both the debug and the release build, a fresh manifest differs from the pre-change manifest
   only by the removal of the following, and nothing else is added, removed or changed. The check uses
   the merged manifest (`./gradlew :app:processDebugManifest` and `:app:processReleaseManifest`) and the
   packaged manifest (`aapt2 dump xmltree --file AndroidManifest.xml`, with `(line=N)` stripped and app resource IDs (`@0x7f[0-9a-f]{6}`) normalised before
   diffing; renumbered app resource IDs are not a difference, because the merged-manifest diff checks those
   references by name):
   - the `com.android.vending.BILLING` permission
   - the whole `<queries>` element (both billing intents)
   - the `com.google.android.play.billingclient.version` meta-data
   - the `ProxyBillingActivity` and `ProxyBillingActivityV2` activities
   - the `.activity.donation.DonationActivity` activity
   - from billing's transitive libraries, which nothing else in the app uses:
     `com.google.android.gms.common.api.GoogleApiActivity`; the `com.google.android.gms.version` meta-data;
     `com.google.android.datatransport.runtime.backends.TransportBackendDiscovery` (with its `backend:` meta-data);
     `com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService`;
     `com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver`

   `INTERNET` and `ACCESS_NETWORK_STATE` remain, because the app declares both itself.
4. `./gradlew :app:dependencies --configuration debugRuntimeClasspath` and `releaseRuntimeClasspath` list no `com.android.billingclient` coordinate. An `unzip -l` or `apkanalyzer dex packages` listing of the debug APK contains no `com/android/billingclient` classes and no `com.zegoggles.smssync.activity.donation` classes.
5. On JDK 25, `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0.
6. Tests deleted are exactly `DonationActivityTest` (10 `@Test`) and `SkuTest` (5 `@Test`), 15 in total. `EnrollmentDialogThemeTest` keeps all 3 `@Test` methods with their assertions unchanged. Only its host
   activity changes from `DonationActivity` to a test-only subclass of the production `ThemeActivity`,
   declared as a concrete `public static` nested class of that test (`EnrollmentDialogThemeTest.HostActivity`;
   `ThemeActivity` is abstract). The import, the local-variable type `DonationActivity` and the
   `DonationActivity.class` argument on lines 117, 179 and 244, and the javadoc and comment references
   to `DonationActivity` (lines 114-116 and 164-167) are corrected to match, and both corrected passages state
   that the AppCompat theme comes from `ThemeActivity.onCreate()`, not from the manifest. No other test is
   deleted, ignored or weakened.
7. The new test baseline is 682 run (697 minus 15) with 2 skipped (the `@Ignore`d `AppTest.shouldGetVersionName` and the `@Ignore`d AndroidKeyStore test in `AuthPreferencesTest`; neither is a donation test). Confirm the run count with the Gradle test report and cross-check with the CLAUDE.md `git grep` command: 15 fewer `@Test` than the pre-change `HEAD`. Record the before and after counts in the story's implementation log.
8. JaCoCo retirement:
   - The `activity.donation*` rule (floor 0.30) is deleted together with its subject package, and the `activity.donation=33.5%` note is removed from the Rule 5 comment.
   - No other `limit`/`minimum` value in `app/build.gradle` changes. The only permitted edits to the JaCoCo block are: deleting the donation rule (including its own comment lines) and its Rule 5 note, and updating the root-package rule's comments with the re-measured value (third bullet). Nothing else in the block is edited; other re-measured values go in the implementation log.
   - The `com.zegoggles.smssync` root-package rule (floor 0.50, last measured 60.0%, 18/30) must be
     re-measured after `Consts.Billing` is removed, because `Consts` sits in that package. The new measured value is
     recorded in the root-package rule's comments in `app/build.gradle` (the "Rule 4" header comment, about lines
     397-399, and the trailing `// MEASURED` comment on its `minimum` line, about line 406; either or both may be
     updated, and they are the only comment edits besides the Rule 5 note and the donation rule's own comments) and must
     still be at least 50%. If it is below 50% the floor is not lowered. Instead, coverage is added until the rule
     passes, and the shortfall is escalated.
   - The `activity` (0.10), `activity.fragments` (0.10), `receiver` (0.50), `preferences` (0.65) and `utils` (0.10)
     rules are re-measured and must still pass without their floors being edited. `MainSettings` is in
     `activity.fragments`, where removing `checkUserDonationStatus()` reduces both covered and total lines.
     `Keys.DONATE` is a covered line in `preferences`. These five re-measured values are recorded in the story's
     implementation log, not in `app/build.gradle`.
9. Dependency verification stays enforced: no `verification-mode`, `<verify-metadata>`, or `<verify-signatures>` setting in `gradle/verification-metadata.xml` is relaxed, and no `--dependency-verification=off` or `lenient` flag is added anywhere. Policy for removed artifacts: the `billing` 7.1.1 entries (line 931) and any now-unreferenced transitive entries may remain, because stale entries do not weaken verification. They are removed only if `./gradlew --write-verification-metadata sha256 help` run in a branch produces that removal with no additions. Hand-editing checksums is prohibited. The build passes with the entries either kept or removed.
10. REQ-MODERNIZATION-010 is recorded as superseded through a change record, not silently edited. A `change_records` entry on REQ-MODERNIZATION-010 states that its intent is superseded by REQ-MODERNIZATION-018 (decision date 2026-11-12, product owner decision "remove the donation flow"). Its acceptance criteria 1-4 are marked not applicable. REQ-MODERNIZATION-018 carries the reciprocal reference, and `DES-MODERNIZATION-010` is annotated as superseded.
11. `CHANGES` has a new entry under the unreleased 1.6.0 heading, in the existing bullet style, stating that the donation feature and the in-app billing dependency were removed. `README.md` no longer contains the
    donation paragraph (lines 43-44) or the billing permission bullet (line 241), and the sentence at lines 40-41
    about there never being a paid version is retained. `sdlc/artifacts/engagement/code-location.md` line 37 is
    updated. `metadata/play/**` is inherited upstream store text and is out of scope: it is not edited.
12. No `SharedPreferences` key is migrated, written or deleted. The enum constant `Keys.DONATE` is removed, and no stored value exists for it (see Context), so no cleanup code or schema version change is required. A unit test or reviewed code evidence confirms that `Preferences` never persisted `donate`. The removal does not clear user preferences, and `Preferences.reset()` behavior is unchanged. The `app/lint-baseline.xml` billing entry (inventory item 12) is deleted, with no other baseline entry edited.

## Rationale

Upgrading carries recurring cost (two major versions behind and still drifting) and a Play Billing policy
surface for a feature the product owner no longer wants. The app never had a paid tier. Removing the flow
also deletes a merged-manifest permission and two `queries` entries, and shrinks the attack and review surface.
Removing it before the compileSdk/targetSdk requirement avoids validating a library that is being deleted.

## Constraints

- Build stays on JDK 25. No per-package JaCoCo floor other than the retired donation rule changes (AC-8).
- Dependency verification stays enforced (AC-9).
- No change to backup, restore, scheduler, notification, or authentication behavior. `Dialogs.BaseFragment`, `ThemeActivity`, and `BundleBuilder` stay, since `BundleBuilder` is still used by `MainActivity` and `AccountManagerAuthActivity`.
- Depends on: nothing. Should precede the platform-level requirement under EPIC-MODERNIZATION-006.

## Notes

- Affected stories: none yet. Stories are created from this requirement, and an NFR linkage is not applicable because the constraints above are expressed as acceptance criteria.
- The 697 figure is the sprint-017 baseline; the 682 target assumes no other test changes land first. Re-baseline from the Gradle report if they do.
- The `@Test` counts above come from a per-file grep and may differ from Gradle's run count if any test is parameterized. AC-7 therefore requires the run count to be confirmed from the Gradle report.
- The release manifest was verified from `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/after-manifest-release.txt`,
  which is equivalent to the debug manifest apart from line numbers and the debug-only `android:debuggable` attribute. AC-3 applies to both variants and the story re-runs it.
- Amended 2026-11-12 by CR-003: AC-3, AC-6, AC-8, AC-11, AC-12, inventory items 7 and 15, and the Context manifest and "Persisted donation state" paragraphs. Product owner decision (verbatim): "I don't own the old app store listing. this is just going to be a private app for me".
