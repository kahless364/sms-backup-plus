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
traces_to:
  - sdlc/artifacts/requirements/modernization/EPIC-MODERNIZATION-006-dependency-currency-latest-stable.md
  - sdlc/artifacts/build/sprints/sprint-017/retro.md
change_records: []
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
| 7 | Strings `donate`, `donate_summary`, `ui_dialog_donate_message`, `ui_donation_success_message`, `ui_donation_failure_message`, `donation_error_iab_unavailable`, `donation_error_unavailable`, `donation_error_canceled`, `donation_error_already_owned`, `donation_unspecified_error`, and the `<!-- donation related -->` and `<!-- purchase errors -->` comments, in the default file and 21 locale files (22 files; per-file counts of matching lines range from 3 to 10) | `app/src/main/res/values/strings.xml` (lines 338-351) and `app/src/main/res/values-{ca,da,de,fr,gl,gr,hu,it,ko,nl,pl,pt-rPT,ru,sk,sq,sr,sv,tr,uk,zh-rCN,zh-rTW}/strings.xml` |
| 8 | Dependency `implementation 'com.android.billingclient:billing:7.1.1'` and the `BuildConfig` comment naming `DonationActivity` | `app/build.gradle` (lines 132 and 43) |
| 9 | JaCoCo rule `includes = ['com.zegoggles.smssync.activity.donation*']`, floor 0.30 (measured 33.5%, 79/236), and the `activity.donation=33.5%` note in the Rule 5 comment | `app/build.gradle` (lines 411 and 435-444) |
| 10 | Tests: `DonationActivityTest` (10 `@Test`), `SkuTest` (5 `@Test`) | `app/src/test/java/com/zegoggles/smssync/activity/donation/` |
| 11 | `EnrollmentDialogThemeTest` (3 `@Test`) uses `DonationActivity` as its AppCompat host activity at lines 117, 179 and 244. It is not a donation test and must be kept, so it needs a replacement host. | `app/src/test/java/com/zegoggles/smssync/activity/fragments/EnrollmentDialogThemeTest.java` |
| 12 | Lint baseline entry (GradleDependency for billing 2.1.0, stale) | `app/lint-baseline.xml` (lines 114-123) |
| 13 | Verification metadata for `billing` 7.1.1 (`.aar` and `.pom`) | `gradle/verification-metadata.xml` (line 931) |
| 14 | Docs: the donation paragraph and the "Google Play billing service" permission bullet | `README.md` (lines 43-44 and 241) |
| 15 | Store listing text mentioning donations (publishing artifact) | `metadata/play/text/{en,nl,da}/full_description.txt` (en: lines 10, 12) |
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

This merged manifest is a local build artifact, so the merged-manifest check in AC-3 must be re-run on a fresh build.

Persisted donation state: none was found. The `donate` preference is an intent-only `Preference` with no
stored value. Donation status is queried live from Play through `queryPurchasesAsync` and is never written
to `SharedPreferences`. Searches of `app/src/main/java/com/zegoggles/smssync/preferences` found no code that
reads or writes a donation value. Users who previously donated therefore have no local state to migrate,
and their purchases stay on Google's side.

## Acceptance Criteria

1. The directory `app/src/main/java/com/zegoggles/smssync/activity/donation/` does not exist, and `grep -rniE "billing|donat" app/src/main app/build.gradle` returns zero results. The one expected exception is `app/src/main/java/com/zegoggles/smssync/compat/GooglePlayServices.java`, whose `com.android.vending` package-name constant matches neither term. Because the grep covers all strings files, AC-1 also proves that items 3-9 of the inventory are removed from every locale.
2. Items 2-6 of the inventory are removed. After removal, `MainSettings.onResume()` no longer calls any donation-status check, and `preferences.xml` contains no `key="donate"`. The settings screen shows no entry point that leads to a missing screen, and no `Preference` references a class that does not exist.
3. A fresh merged manifest (`./gradlew :app:processDebugManifest`) differs from the pre-change merged manifest only by the removal of the following, and nothing else is added, removed or changed:
   - the `com.android.vending.BILLING` permission
   - the two billing `<queries>` intents
   - the `com.google.android.play.billingclient.version` meta-data
   - the `ProxyBillingActivity` and `ProxyBillingActivityV2` activities
   - the `.activity.donation.DonationActivity` activity
4. `./gradlew :app:dependencies --configuration debugRuntimeClasspath` and `releaseRuntimeClasspath` list no `com.android.billingclient` coordinate. An `unzip -l` or `apkanalyzer dex packages` listing of the debug APK contains no `com/android/billingclient` classes and no `com.zegoggles.smssync.activity.donation` classes.
5. On JDK 25, `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0.
6. Tests deleted are exactly `DonationActivityTest` (10 `@Test`) and `SkuTest` (5 `@Test`), 15 in total. `EnrollmentDialogThemeTest` keeps all 3 `@Test` methods with their assertions unchanged, and only its host activity changes from `DonationActivity` to an AppCompat activity that remains in the app. No other test is deleted, ignored or weakened.
7. The new test baseline is 682 run (697 minus 15) with 2 skipped (the `@Ignore`d `AppTest.shouldGetVersionName` and the `@Ignore`d AndroidKeyStore test in `AuthPreferencesTest`; neither is a donation test). Confirm the run count with the Gradle test report and cross-check with the CLAUDE.md `git grep` command: 15 fewer `@Test` than the pre-change `HEAD`. Record the before and after counts in the story's implementation log.
8. JaCoCo retirement:
   - The `activity.donation*` rule (floor 0.30) is deleted together with its subject package, and the `activity.donation=33.5%` note is removed from the Rule 5 comment.
   - No other `limit`/`minimum` value in `app/build.gradle` changes. The only legitimate edits to the JaCoCo block are the deletion of the donation rule and its comment.
   - The `com.zegoggles.smssync` root-package rule (floor 0.50, last measured 60.0%, 18/30) must be re-measured after `Consts.Billing` is removed, because `Consts` sits in that package. The new measured value is recorded in the rule's comment and must still be at least 50%. If it is below 50% the floor is not lowered. Instead, coverage is added until the rule passes, and the shortfall is escalated.
   - The `activity` (0.10), `activity.fragments` (0.10), and `receiver` (0.50) rules are re-measured and must still pass without their floors being edited. `MainSettings` is in `activity.fragments`, where removing `checkUserDonationStatus()` reduces both covered and total lines.
9. Dependency verification stays enforced: no `verification-mode`, `<verify-metadata>`, or `<verify-signatures>` setting in `gradle/verification-metadata.xml` is relaxed, and no `--dependency-verification=off` or `lenient` flag is added anywhere. Policy for removed artifacts: the `billing` 7.1.1 entries (line 931) and any now-unreferenced transitive entries may remain, because stale entries do not weaken verification. They are removed only if `./gradlew --write-verification-metadata sha256 help` run in a branch produces that removal with no additions. Hand-editing checksums is prohibited. The build passes with the entries either kept or removed.
10. REQ-MODERNIZATION-010 is recorded as superseded through a change record, not silently edited. A `change_records` entry on REQ-MODERNIZATION-010 states that its intent is superseded by REQ-MODERNIZATION-018 (decision date 2026-11-12, product owner decision "remove the donation flow"). Its acceptance criteria 1-4 are marked not applicable. REQ-MODERNIZATION-018 carries the reciprocal reference, and `DES-MODERNIZATION-010` is annotated as superseded.
11. `CHANGES` has a new entry under the unreleased 1.6.0 heading, in the existing bullet style, stating that the donation feature and the in-app billing dependency were removed. `README.md` no longer contains the donation paragraph (lines 43-44) or the billing permission bullet (line 241), and the sentence at lines 40-41 about there never being a paid version is retained. `metadata/play/text/{en,nl,da}/full_description.txt` are updated to drop the donation wording. If the product owner reserves store-listing text as a publishing decision, the exact files and lines are handed over unchanged to the owner and the handover is recorded. `sdlc/artifacts/engagement/code-location.md` line 37 is updated.
12. Previously donated users: no `SharedPreferences` key is migrated, written or deleted. The enum constant `Keys.DONATE` is removed, and no stored value exists for it (see Context), so no cleanup code or schema version change is required. A unit test or reviewed code evidence confirms that `Preferences` never persisted `donate`. The removal does not clear user preferences, and `Preferences.reset()` behavior is unchanged. The `app/lint-baseline.xml` billing entry (inventory item 12) is deleted, with no other baseline entry edited.

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
- Not verified: the merged release manifest, since only the debug merged manifest exists locally. AC-3 is applied to both variants.
