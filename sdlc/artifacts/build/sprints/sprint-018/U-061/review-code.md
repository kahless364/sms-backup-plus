---
artifact_type: review-code
story_id: "U-061"
verdict: "PASS"                        # PASS | FAIL
agent: "Code Reviewer"
timestamp: "2026-11-12"
blockers: 0
warnings: 0
---

# Code Review: U-061

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS. No CNTR applies (`integration_contracts: []`). The manifest and R8 surfaces the design asserts unchanged are unchanged in the diff: the `BACKUP` receiver, `networkSecurityConfig` and the other manifest entries are untouched, and the only manifest hunk is the `DonationActivity` line. |
| Test coverage | PASS. `EnrollmentDialogThemeTest` keeps 3 `@Test` with unchanged assertions. Exactly the two donation test classes are deleted (`DonationActivityTest`, `SkuTest`). No test is `@Ignore`d or weakened. |
| Code quality | PASS. The change is pure deletion plus one test re-host and one approved ProGuard line. I found no orphaned import, string, layout or rule. |

Reviewed range: `git diff e0c51d42..7dfd651b -- . ':!sdlc'` (38 files, +62 / -1246). I read the changed files, the surrounding files (`ThemeActivity.java`, `MainSettings.java`, `preferences.xml`, `proguard-rules.pro`), the story, `plan.md` scope and the implementation log. Gradle was not run, as instructed. Test counts, coverage figures and manifest diffs below come from the committed evidence files, not from a re-run by me.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

None.

### Observations

1. `app/proguard-rules.pro:59-62` (I9): `-dontwarn com.google.errorprone.annotations.**` is the narrowest *conventional* rule and its comment is accurate.
   - I did not rely on the comment. The pre-change R8 configuration (`evidence/before-configuration-rules.txt:86`) already contained this exact line. `evidence/I9-r8-config-pairs.txt` attributes it to `play-services-basement-18.4.0/proguard.txt`, which `evidence/I5-drift-release.txt` shows was removed.
   - The only remaining dependency that references Error Prone annotations is `tink-android:1.8.0` (`evidence/after-deps-releaseRuntime.txt:275`). No `error_prone_annotations` artifact is on the runtime classpath in either the before or the after tree, so "compile-time-only" is correct.
   - The wildcard is a package wildcard over an annotations-only package, and R8 named four classes in that package. Enumerating the four classes would be marginally narrower but brittle on a Tink bump. The wildcard also reproduces the previously effective behaviour exactly, which is the right bar for a no-behaviour-change story.
   - The comment avoids the retired terms ("in-app purchase library"). That is a deliberate coordinator choice and does not make it misleading.
   - Classification: defer-justified — the rule matches the previously effective R8 behaviour and a tighter rule trades no real safety for added upgrade brittleness.

2. `app/proguard-rules.pro:20-37`: the Otto `@Subscribe`/`@Produce` rule, the `SmsJobService` keep and the `com.firebase.jobdispatcher.**` keep are old. They are not billing or donation rules and are outside this story's scope.
   - `app/build.gradle` no longer declares the jobdispatcher dependency (U-017). `SmsJobService` appears only in the manifest.
   - The billing and donation ProGuard surface is clean: no keep rule names a billing or donation class (`grep -ci "billing|donat"` on the file is 0).
   - Classification: defer-justified — pre-existing dead rules unrelated to this story, and removing them needs a release-tested change of their own.

3. `gradle/verification-metadata.xml` is byte-unchanged over the range (`git diff --stat e0c51d42..7dfd651b -- gradle metadata` is empty). The stale `billing:7.1.1` component (line 931) and the transitive datatransport, gms and firebase-encoders entries remain.
   - Leaving them is harmless. Gradle verifies only artifacts it resolves; an unused `<component>` entry cannot admit anything because no resolved artifact matches it. `verify-metadata` and `verify-signatures` are untouched.
   - The IC-4 writer run (`evidence/I8-ic4-writer.diff`) is empty after `--strip-trailing-cr`, i.e. Gradle itself would not prune them.
   - The only cost is a few hundred lines of dead trust data. A later prune should be done by the writer, never by hand.
   - Classification: defer-justified — AC-9 expressly permits the stale entries and a hand edit of checksum metadata is prohibited.

4. `app/build.gradle:43` comment now reads "app uses BuildConfig.DEBUG in App.java and auth/OAuth2Client.java". The story asked for a comment naming only `App.java`. I checked both claims: `App.java:91` and `auth/OAuth2Client.java:7,183` use `BuildConfig`. The comment is accurate and slightly more complete than the story asked for; this is the accepted deviation 1.
   - The JaCoCo edits are exactly as AC-8 permits. The donation rule and its comment lines are deleted and the Rule 5 `activity.donation=33.5%` note is removed. The only other edit is one added Rule 4 comment line (`app/build.gradle:399`). No `limit` or `minimum` value changed.
   - No exclusion or config in `app/build.gradle` still refers to the removed package.
   - Classification: defer-justified — informational; the comment is correct and already accepted.

5. `EnrollmentDialogThemeTest` (I1) still tests what it tested before. The three call sites change only the host type (`HostActivity` replacing `DonationActivity`). The nested `public static class HostActivity extends ThemeActivity {}` is sound:
   - `ThemeActivity` is abstract with a no-arg constructor (`ThemeActivity.java:12`), so the subclass is instantiable.
   - `ThemeActivity.onCreate()` calls `setTheme(new Preferences(this).getAppTheme())` before `super.onCreate()`. That is exactly the theme path the corrected comments now cite, so the AppCompat theme does not depend on the manifest.
   - Because it is nested, it compiles to `EnrollmentDialogThemeTest$HostActivity` and stays under the `*Test*` JaCoCo filter. `evidence/I6-jacoco-packages.txt` and the log report no `HostActivity` in the JaCoCo XML.
   - The isolated run is 3/3 (`evidence/I1-isolated-junit-totals.txt`).
   - One residual: the new comment says the AGP 9 unit-test manifest "declares no app components and no application theme". That is not provable from the diff. It is explanatory only and the passing test does not depend on it.
   - Classification: defer-justified — the sentence is not provable from the diff alone and no behaviour rests on it.

6. Residual verification gap, known and handled. The release variant was not built by the per-increment gates, so the R8 failure surfaced only at I8. I agree with how it was handled: the failure was escalated, one product-owner-approved line was added as I9, and the I9 gate now builds release.
   - The release APK was never run on a device. The mitigation is strong: kept-name comparison shows exactly one lost kept name (`activity.donation.DonationActivity`), the member-level comparison shows no other loss (`evidence/I9-kept-members.txt`), and the R8 configuration diff is fully attributed (63 removed, 1 added, the approved rule).
   - A related softness: `activity` line coverage read 189/1011 in two runs and 199/1011 in two others on identical product code, and `receiver` sits at exactly 50.0% against a 0.50 floor. Neither is changed by this story, but they are the likeliest source of a flaky `jacocoTestCoverageVerification` later.
   - I recommend adding `:app:assembleRelease` to the standing gate set. That is a process suggestion and not a defect in this diff.
   - Classification: defer-justified — product-owner-accepted limit for a private, unpublished app, and the static evidence for the release surface is complete.

7. Commit hygiene is clean. Each commit message matches its diff, and I9 touches exactly one product file (`app/proguard-rules.pro`).
   - No unrelated change rides along. Non-product files are limited to `sdlc/` evidence and log updates.
   - `gradle/`, `metadata/` and `endpoints.md`/`systems.md` are unchanged. The `endpoints.md:62` and `systems.md:55` follow-up is already recorded.
   - The `CHANGES` bullet is added only under the unreleased block, and the historical lines at 24, 121, 139 and 169 are left alone. The remaining "billing" and "donat" hits in `CHANGES` are those historical lines.
   - Classification: defer-justified — informational confirmation; nothing to remediate.

## Detailed Verification

### 1. Completeness of removal

- A repo-wide case-insensitive grep for `billing|donat|purchase errors|DONATE` over `app/src` (main, test, `res/` including every `values-*`), `app/build.gradle`, `app/proguard-rules.pro`, `app/lint-baseline.xml`, `README.md` and `metadata/` returns hits only in the four historical `CHANGES` lines (24, 121, 139, 169) and the new `CHANGES:6` entry. There is no `app/src/androidTest` directory.
- `*.gradle`, `*.pro`, `*.xml`, `*.md`, `*.yml` and `*.kts` outside `sdlc/`, `metadata/` and build output contain billing or donation text only in `gradle/verification-metadata.xml` (allowed by AC-9).
- The strings removal is exact. I diffed all 22 `strings.xml` files with `--diff-algorithm=minimal`: 0 added and 235 deleted lines, and the deleted lines are only the 10 named strings (`donate`, `donate_summary`, `ui_dialog_donate_message`, `ui_donation_success_message`, `ui_donation_failure_message`, `donation_error_iab_unavailable`, `donation_error_unavailable`, `donation_error_canceled`, `donation_error_already_owned`, `donation_unspecified_error`) plus the two comments. The per-name counts (22 / 21 / 20 / 12 / 17 comments) match files that actually carried each string, so no unrelated string was touched. The raw-diff `+` noise in 14 files is only Myers blank-line alignment, as the log notes.
- No donation-only layout, drawable, menu or style exists. The tree has no resource whose name contains `donat`, and the donation UI built its views in code from the deleted classes. Nothing is orphaned.

### 2. Collateral damage

- `MainSettings.java`: the imports left behind are all still used (`Preference` at lines 35, 37, 103; `App.` at 50, 51, 52, 116; `Bundle`, `TextUtils`, `ArrayList`, `List`, `DataType`, `AuthPreferences`). The removed `android.util.Log` and `static App.TAG` imports have no remaining use in the file (no `TAG` or `Log.` hit), so those deletions are correct and safe. `onResume()` matches the story's target shape.
- `preferences.xml`: the `donate` block was the last child of the root `PreferenceScreen`. After removal the root closes immediately after the inner `AdvancedSettings` screen. No empty `PreferenceCategory` results. The only `android:dependency` attributes (lines 119 and 288) point at `backup_calllog_sync_calendar_enabled` and `app_log`, neither related to donate.
- `Preferences.java`: `DONATE("donate")` is removed and `reset()` is untouched. Nothing else references `Keys.DONATE`, and no grep hit remains (IC-10 holds).
- `Consts.java`: after removal the file still ends with `CALLLOG_PROVIDER` and the closing brace. Its imports (`Uri`, `CallLog`) remain used.

### 4. `app/build.gradle`

- Dependency line removed with no remaining reference to the `billingclient` coordinate. The JaCoCo and comment edits are as described in observation 4.

### 5. Docs

- README: only the donation paragraph (three lines including the trailing blank) and the billing permission bullet are removed. The "never a pro / paid version" sentence is retained.
- `CHANGES`: one new bullet in the existing two-space style under the unreleased block.
- `code-location.md`: only the billing bullet is removed. The neighbouring `firebase-jobdispatcher` bullet is also stale (pre-existing, outside this story's scope).

### Dependency and R8 surface

- The 12 modules removed from the classpath are exactly the expected set (`evidence/I5-drift-{debug,release}.txt`: no version drift, none added).
- The manifest diffs are deletion-only against the AC-3 list (`evidence/I8-manifest-diffs.txt`, `evidence/I9-manifest-diffs.txt`).
- Release mapping prefix count is 0 (I0 baseline: 965).

## Patterns Verified

- [x] Follows existing code patterns (pure deletion; surviving code untouched)
- [x] Error handling is appropriate (no new error paths; the removed `try/catch` in `MainSettings` went with its method)
- [x] Tests cover new functionality (no new production code; `EnrollmentDialogThemeTest` re-hosted with 3 of 3 `@Test` and unchanged assertions)
- [x] No hardcoded values that should be configurable
- [x] No unnecessary complexity (the one new class is a one-line test-only nested host)

## Integration Verified

- [x] New code is reachable from production entry points. The only new class is the test-only `HostActivity`, reached from `Robolectric.buildActivity(HostActivity.class)` at three call sites. No new production component exists.
- [x] Registries and dispatch maps updated. The manifest `DonationActivity` entry and the `preferences.xml` entry are removed. `HostActivity` is deliberately unregistered (IC-3) and `grep HostActivity app/src/main` is empty.
- [x] Function signatures match at all call sites. No signature changed; `checkUserDonationStatus()` had one caller, also removed.
- [x] No dead code introduced
- [x] Integration path documented in `implementation-log.md` (increment table and per-increment sections)

## Regression Check (removal story)

- [x] Capabilities preserved. Story "Existing Behavior to Preserve" items verified: `MainSettings.onResume()` keeps `super.onResume()`, `updateAutoBackupPreferences()` and `addPreferenceListener(ENABLE_AUTO_BACKUP.key)`; `ThemeActivity`, `Dialogs.BaseFragment`, `BundleBuilder`, `BuildConfig` and `GooglePlayServices.java` are untouched; `Preferences.reset()` is unchanged.
- [x] The deleted capabilities are all donation or billing, which the product-owner decision "remove the donation flow" (REQ-MODERNIZATION-018) explicitly covers.
- [x] The Old backup scheduler setting and `GooglePlayServices.java` are intentionally retained (follow-up story, product-owner decision recorded).

## Contract Verification

- [x] Consumers of the modified interfaces are identified: `Keys.DONATE` (one consumer, removed), `Consts.Billing` (donation classes only, removed) and the `donate*` string names (the removed preference and the removed activity only).
- [x] No contract mismatch. Nothing in the diff renames a field or event.

## Runtime Claims Verified

- [x] `## Runtime Claims` inventory read in `implementation-log.md`: declared empty on purpose ("None. ... Declared empty on purpose"), so it was declared, not omitted.
- [x] There are no declared claims, so no probe or escape-hatch justification is required.
- [x] Inventory completeness audited against the story's runtime-behavior ACs. IC-11 (on-device check, text evidence, no `FATAL EXCEPTION`) is a functional check of an application launch, not one of the five runtime-claim classes (crash or interrupt resilience, concurrency, timing or expiry, git or filesystem mechanics, hook firing). The AC-2 "no entry point leads to a missing screen" behaviour is covered both statically (`preferences.xml` has no `key="donate"`) and by the device evidence. I accept the empty inventory. This audit is read-only and I initiated no probe.
- [x] No probe wrote into `../amp`. The log's scratch builds and the IC-4 writer run used scratch copies outside the repository, and nothing refers to a sibling plugin-source repository.
- [x] UNVERIFIED (escape-hatch) claims: None.

## Phase Completion Report
---
story_id: "U-061"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-018/U-061/review-code.md"
story_status: "in-progress"
current_build_phase: "review"
blockers: 0
warnings: 0
errors: []
---
