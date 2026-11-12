---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-018
related_stories: []
related_design_docs:
  - DES-MODERNIZATION-010
integration_contracts: []
change_records:
  - CR-003
type: ''
id: DES-MODERNIZATION-013
title: Donation Flow and Play Billing Removal
domain: modernization
updated_at: '2026-11-12T17:51:55.994Z'
---

# DES-MODERNIZATION-013: Donation Flow and Play Billing Removal

## Overview

This design covers how the in-app donation flow and `com.android.billingclient:billing:7.1.1` are
deleted under REQ-MODERNIZATION-018. The product owner's decision, verbatim, is "remove the donation
flow". The work is a deletion, so "architecture" here means two things: what the app looks like after
the deletion, and the order of changes that keeps `:app` and its unit-test source set compiling and
gated at every step. The design makes nine decisions (D1–D8, plus D4a). Each one names the
alternatives it rejects.

All facts below were re-checked against the working tree at HEAD `d931fde8` (Gradle 9.5.1, AGP 9.0.1,
Kotlin 2.3.21, JDK 25). They come from greps, from the local build outputs dated 2026-11-12
(`app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`,
`app/build/test-results/testDebugUnitTest/`,
`app/build/intermediates/packaged_manifests/debugUnitTest/...`), and from the U-060 evidence in
`sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/`.

## Context

The REQ-MODERNIZATION-018 inventory (16 items) is confirmed: `grep -rniE "billing|donat" app/src/main
app/build.gradle` returns exactly the expected files. Re-verification also found five facts that the
original requirement did not state (CR-003 has since added facts 1, 4 and 5 to it), and they shape the
design:

1. **Billing's transitive closure leaves too, and that changes the manifest.** In the U-060
   `after-deps-releaseRuntime.txt` (and the debug tree), every `com.google.android.gms:*` (5 modules),
   `com.google.android.datatransport:*` (3) and `com.google.firebase:firebase-encoders*` (3) node sits
   under `billing:7.1.1` and nowhere else. These libraries add their own entries to the packaged
   manifest (`after-manifest-debug.txt`): `com.google.android.gms.common.api.GoogleApiActivity`, the
   `com.google.android.gms.version` meta-data, and the datatransport `TransportBackendDiscovery` service
   (with its `backend:...CctBackendFactory` meta-data), `JobInfoSchedulerService` and
   `AlarmManagerSchedulerBroadcastReceiver`. During design this was raised as Scope Concern SC-1,
   because AC-3 as originally worded ("nothing else is … removed") could not pass. CR-003 resolved it
   on 2026-11-12: AC-3 now lists these five removals explicitly.
2. **Robolectric's host activity gets its theme from code, not from the manifest.** The AGP 9
   unit-test manifest (`processDebugUnitTestManifest`) declares no app components and no
   `android:theme` on `<application>`. `DonationActivity` is not declared in it, yet
   `EnrollmentDialogThemeTest` passes (3/3, 0 failures). The AppCompat theme comes from
   `ThemeActivity.onCreate()`, which calls `setTheme(new Preferences(this).getAppTheme())`. The test's
   javadoc says the theme comes from the manifest. That was true before AGP 9 and is stale now.
3. **Some test classes count as production coverage.** The JaCoCo ASM class root
   (`transformDebugUnitTestClassesWithAsm/dirs`) contains test classes. The only filter is
   `**/*Test*.*`. Helpers without "Test" in their name are counted in production packages: for
   example, `preferences/InMemorySecretStore` contributes 24/24 covered lines to the `preferences`
   rule. A new test-only helper class is therefore a coverage decision, not just a test decision.
4. **Store listing is out of scope.** `metadata/play/**` is inherited upstream store text; this is a
   private, unpublished app (product owner, 2026-11-12: "I don't own the old app store listing. this
   is just going to be a private app for me"), so these files are untouched and out of scope.
5. **Counts.** HEAD has 697 `@Test` (CLAUDE.md `git grep` command). The local Gradle XML shows 697
   run and 2 skipped. Per file: `DonationActivityTest` 10 (Gradle: 10), `SkuTest` 5 (Gradle: 5),
   `EnrollmentDialogThemeTest` 3. The default `strings.xml` has 12 matching lines (10 strings plus 2
   comments). Per-locale counts range from 3 (`values-it`) to 12. This matches REQ-MODERNIZATION-018
   inventory item 7 as amended by CR-003 ("3 to 12"). The original text said "3 to 10".

## System Architecture

### Post-removal shape

The app loses one leaf feature and nothing else. The settings screen (`MainSettings`, root
`PreferenceScreen` in `app/src/main/res/xml/preferences.xml`) ends at the last `PreferenceScreen`
(`</PreferenceScreen>` before line 319). `MainSettings.onResume()` becomes `super.onResume();
updateAutoBackupPreferences(); addPreferenceListener(ENABLE_AUTO_BACKUP.key);`, so the only network or
IPC call made from settings resume is gone. `ThemeActivity`, `Dialogs.BaseFragment`, `BundleBuilder`
(used by `MainActivity` and `AccountManagerAuthActivity`), `BuildConfig` (still used by `App.java`) and
`compat/GooglePlayServices.java` stay. That last file holds package-name string constants only and has
no gms import. A grep of `app/src/main` and `k9mail-vendored/src` for `com.google.android.gms`
imports and `datatransport` finds nothing else, so no app code depends on the transitives that leave.
`Preferences.Keys.DONATE` was an intent-only key and was never persisted. `Preferences.reset()`
(lines 255-259) does not touch it, so no preference migration is needed (REQ-MODERNIZATION-018
AC-12).

### D1 — One story, ordered increments I0–I8, each compiling

**Decision.** Deliver this as **one story**, with the increments below committed in order. Each
increment must pass `:app:assembleDebug :app:testDebugUnitTest` before the next one starts. The
JaCoCo gate runs at I0, I6 and I8.

| Inc | Change | Why it compiles at this point |
|-----|--------|-------------------------------|
| I0 | Capture baseline evidence before any edit: merged debug and release manifests (`:app:processDebugManifest`, `:app:processReleaseManifest`; copy the outputs under `app/build/intermediates/merged_manifests/`, see D5), packaged debug and release manifests (`aapt2` dumps), release `mapping.txt` and `configuration.txt`, both runtime dependency trees, Gradle test totals, JaCoCo XML, `git grep` `@Test` count | No edit |
| I1 | Re-host `EnrollmentDialogThemeTest` (D2) | `DonationActivity` still exists. The test source set stops depending on it **before** it is deleted |
| I2 | Remove the entry points: the `<Preference key="donate">` block (preferences.xml lines 319-327), `MainSettings` lines 14, 15, 24, 75 and 164-182 (the method; also drop the blank line 163 before it), `Keys.DONATE` (Preferences.java:101), manifest line 129 | Nothing outside `activity/donation/` references these symbols any more (grep shows `MainSettings` is the only importer) |
| I3 | Delete `app/src/main/.../activity/donation/`, `app/src/test/.../activity/donation/` and `Consts.Billing` (Consts.java lines 36-50) **together** | The tests reference the main classes, and `Consts.Billing` is referenced only by `DonationActivity` (lines 46-47) |
| I4 | Remove the donation strings and both comments from all **22** `strings.xml` files in one commit, by `name` attribute | `warningsAsErrors true`. Removing them from only some locale files would leave translations with no default string. After I3, nothing references them |
| I5 | Remove the dependency (build.gradle line 132). Reword the BuildConfig comment (line 43) to name only `App.java`. Delete the stale `lint-baseline.xml` entry (lines 114-123) | The billing API has no compile-time users after I3 |
| I6 | Retire the JaCoCo rule (D3) | A PACKAGE rule whose `includes` match nothing passes trivially. The order is for review clarity only |
| I7 | Docs (D7) | Not compiled |
| I8 | After-evidence and equivalence checks. D5: rebuild the merged and packaged manifests for both variants and diff each one against its I0 copy. Then D6, the JaCoCo re-measurement (D3), and the on-device check in Design Validation | Exactly one edit is allowed: the measured value goes into the root-package rule's comment(s) in `app/build.gradle` (D3). Nothing else changes |

**Rationale.** The order follows the dependency direction, deleting consumers before what they
consume: tests, then entry points, then code, then resources, then library, then gate, then docs.
With that order no increment leaves a dangling reference. There is a single release-visible outcome
(the permission and component removal), so splitting it would ship an intermediate APK that still
declares `com.android.vending.BILLING` while no code uses it.

**Rejected.** *Two stories (feature deletion, then dependency removal).* The second story would repeat
the full gate run, the manifest diff and the R8 diff for a one-line change, and it would leave the
intermediate state described above. *A single unordered commit.* It cannot be bisected, and the
EnrollmentDialogThemeTest re-host would hide inside a large deletion diff.

### D2 — Replacement host for `EnrollmentDialogThemeTest`: a test-only nested `ThemeActivity` subclass

**Decision.** Add `public static class HostActivity extends ThemeActivity {}` as a nested class of
`EnrollmentDialogThemeTest`. It is concrete, because `ThemeActivity` is abstract, and it is
`public static`, so Robolectric can instantiate it through its implicit public no-arg constructor.
Replace the three call sites (lines 117, 179 and 244) with
`HostActivity activity = Robolectric.buildActivity(HostActivity.class).create().get()`. On each of
those lines, both the `DonationActivity.class` argument and the local variable type
`DonationActivity activity` change to `HostActivity`. The
`{@link DonationActivity}` reference at line 164 changes to `{@link HostActivity}`, so the diff reads
as a host swap and nothing more. Remove the import at line 9. Change no assertion. Correct the javadoc and comments at lines 114-116 and 164-167: the AppCompat theme comes
from `ThemeActivity.onCreate()`, not from the manifest (Context fact 2). Do not register the activity
anywhere.

**Rationale.**
- What the test needs is "an Activity whose theme is AppCompat". In production that guarantee comes
  from `ThemeActivity`. Subclassing it exercises the real theming code, which is 16 of 17 lines
  covered today, and keeps `activity` package coverage where it is.
- No manifest registration is needed or possible. AGP 9 builds the unit-test manifest without the
  app's components, and `DonationActivity` passes today without being declared there. That is direct
  evidence that Robolectric 4.15.1 launches undeclared activities.
- A nested class compiles to `EnrollmentDialogThemeTest$HostActivity.class`, which matches the
  `**/*Test*.*` JaCoCo filter. A top-level helper named, say, `ThemedHostActivity` would be counted as
  production code in its package (Context fact 3), which would quietly move a floor.
- The change stays inside one file, as REQ-MODERNIZATION-018 AC-6 wants.

**Rejected.**
- *`AccountManagerAuthActivity`.* Its `onCreate` requests GET_ACCOUNTS or runs `checkAccounts()`, so
  it may finish or start other UI. That couples a theme test to auth-flow side effects and covers
  lines in `activity.auth`.
- *`OAuth2WebAuthActivity`.* Its `onCreate` fires `ACTION_VIEW` with a null URI and starts a flow
  collector.
- *`MainActivity`.* It is `@AndroidEntryPoint`, so it needs `HiltAndroidRule` and
  `HiltTestApplication`, which changes the test's structure.
- *A plain `AppCompatActivity` subclass.* It has no AppCompat theme under the AGP 9 unit-test
  manifest, so the test would break or pass for the wrong reason.

History: during design, this decision conflicted with the original AC-6 wording ("an AppCompat activity
that remains in the app"), and it was raised as SC-2. CR-003 resolved it on 2026-11-12. AC-6 now
specifies a test-only subclass of the production `ThemeActivity`, declared as a concrete
`public static` nested class `EnrollmentDialogThemeTest.HostActivity`.

### D3 — JaCoCo: retire one rule, re-measure six, lower nothing

**Decision.**
- Delete the `activity.donation*` rule (build.gradle lines 435-444).
- Delete `activity.donation=33.5%,` from the Rule 5 comment (line 411).
- Leave every `minimum` value as it is.
- After I8, write the newly measured root-package value into the root rule's comments only, as
  REQ-MODERNIZATION-018 AC-8 allows. That rule has two comments, and either or both may be updated:
  the Rule 4 header comment (build.gradle lines 397-399) and the trailing comment on its `minimum`
  line (line 406, `// MEASURED 52.9% — TODO: raise to 70%`). The `0.50` value itself does not change.
  No other rule's comment in `app/build.gradle` changes.
- Record the other re-measured values (`activity`, `activity.fragments`, `receiver`, `preferences`,
  `utils`) in the story's implementation log, not in `app/build.gradle`.

| Rule (floor) | Today (local report) | What removal does | Predicted |
|--------------|---------------------|-------------------|-----------|
| `com.zegoggles.smssync` (0.50) | 18/30 = 60.0% | `Consts$Billing` has 1 line, and it is covered (line 45, `ALL_SKUS` initializer). JaCoCo filters the private constructor | 17/29 = **58.6%** |
| `activity.fragments*` (0.10) | 58/483 = 12.0% | Every `MainSettings` line is uncovered (0/75). Lines 75 and 164-182, plus the anonymous listener class, are removed as **missed** lines only | rises (≈58/470 ≈ 12.3%) |
| `activity` (0.10) | 199/1011 = 19.7% | `ThemeActivity` stays covered through `HostActivity` (D2) | ≈ unchanged |
| `preferences*` (0.65) | 315/413 = 76.3% | `Keys$DONATE` takes 1 covered line | 314/412 = 76.2% |
| `receiver*` (0.50) | 29/58 = 50.0% (no headroom) | No donation code or test touches `receiver` | unchanged, still zero headroom |
| `utils*` (0.10) | 30/199 = 15.1% | `BundleBuilder` is 0% covered today | unchanged |

**Rationale.** Per-test coverage attribution is not available, so the "Predicted" column is an
estimate. The binding check is the I8 re-measurement. `preferences` and `utils` are in the list because
`Keys.DONATE` and `BundleBuilder` sit in those packages. The design proposed adding them (SC-3), and
CR-003 added them to REQ-MODERNIZATION-018 AC-8 on 2026-11-12.

**Breach routing.** If any remaining rule falls below its floor, add a characterization test for
real behaviour in that package until the rule passes, and escalate the shortfall in the
implementation log. Lowering a floor, adding an exclusion, or renaming a helper so the `*Test*`
filter catches it are all forbidden (REQ-MODERNIZATION-018 AC-8, REQ-MODERNIZATION-003).

**Rejected.** *Keeping the rule at `0.00` as a placeholder.* That is a dead gate for a package that
does not exist.

## Integration Design

The removal crosses four external surfaces: Gradle dependency resolution and verification, the
merged manifest, R8, and repository docs. Each one gets a before/after proof.

### D4 — Dependency verification: keep stale entries; never hand-edit or regenerate the whole file

**Decision.** Leave `gradle/verification-metadata.xml` unchanged. The entries this change makes stale
are `billing` 7.1.1 (line 931) and the 11 billing-only transitives (datatransport at lines 1859-1875,
gms at lines 1883-1915, firebase-encoders at lines 2530-2546). Run
`./gradlew --write-verification-metadata sha256 help` once, on a scratch branch, purely as evidence.
Accept its result only if it is a removal with no additions, which REQ-MODERNIZATION-018 AC-9 allows.
Do not touch the `<configuration>` block (`verify-metadata=true`, `verify-signatures=false`).

**Rationale.** Gradle's writer merges into the existing file. Project precedent is the U-060
`plan.md` line 539: "the writer adds entries and never removes them". The scratch run is expected to
produce no diff, which would make the pruning path in AC-9 unreachable. Stale checksums do not weaken
verification, because a checksum only constrains an artifact that actually resolves.

**Rejected.**
- *Hand-deleting the 12 components.* AC-9 forbids hand-editing checksums.
- *Deleting the file and regenerating it.* That rewrites all 803 components, cannot be reviewed as
  "removals only", and risks the `trusted-artifacts` block.

### D4a — Surviving-module version drift is a stop condition (guard)

**Decision.** At I5, diff the resolved `debugRuntimeClasspath` and `releaseRuntimeClasspath` trees
against the I0 trees. Treat as a stop condition any module that remains on the classpath but now
resolves to a different version. No drift is expected. In the U-060 tree every androidx version that
billing requests is lower than the version that resolves (activity 1.2.3→1.5.1, core 1.8.0→1.9.0,
savedstate 1.2.0→1.2.1, fragment 1.0.0→1.5.1, collection 1.0.0→1.4.2), so billing raises nothing and
removing it should lower nothing. If drift does appear, **stop and escalate first**. A
`constraints {}` pin at the previous resolved version is one option to put to the product owner. It is
not pre-approved, because it is a build edit outside REQ-MODERNIZATION-018.

**Rationale.** REQ-MODERNIZATION-018 forbids behaviour changes outside donations. The diff is a cheap
guard that proves the evidence above still holds at build time.

**Rejected.** *Accepting drift because "it still builds".*

### D5 — Manifest equivalence: merged and packaged, debug and release

There are four diffs: merged debug, merged release, packaged debug and packaged release. Each one is
compared against its own I0 baseline.

**1. Merged manifests (symbolic names; the authoritative check on references).** Run
`./gradlew :app:processDebugManifest :app:processReleaseManifest` at I0 and again at I8. Copy these
files into the evidence folder as `{before|after}-merged-{debug|release}.xml`:

- `app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml`
  (verified on disk on AGP 9.0.1, dated 2026-11-12 10:23)
- `app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml`
  (path by analogy; no release build exists locally, so confirm it with `ls` at I0)

Use these files, not the sibling `merged_manifest/<variant>/process<Variant>MainManifest/` output,
because AC-3 names `processDebugManifest`. Diff with a plain
`diff before-merged-debug.xml after-merged-debug.xml`, and the same for release. Resource references
appear here by name (`@drawable/ic_sms_backup`, `@xml/backup_descriptor`), so any changed reference
would show up in this diff.

**2. Packaged manifests (what ships).** Use U-060 `contract-verification.md` C-1 with build-tools
36.0.0, and normalise two things before diffing:

```
AAPT2="$LOCALAPPDATA/Android/Sdk/build-tools/36.0.0/aapt2"
"$AAPT2" dump xmltree --file AndroidManifest.xml <apk> > {before|after}-manifest-{debug|release}.txt
norm() { sed -E 's/ ?\(line=[0-9]+\)//g; s/@0x7f[0-9a-f]{6}/@0x7fRESID/g' "$1"; }
diff <(norm before-manifest-debug.txt) <(norm after-manifest-debug.txt)
diff <(norm before-manifest-release.txt) <(norm after-manifest-release.txt)
```

- **Strip `(line=N)`.** Removing `DonationActivity` shifts every later source line number.
- **Replace app resource IDs (`@0x7f` + 6 hex digits).** `aapt2` prints app resource references as
  numeric IDs. Removing `play-services-base` removes its 19 `common_*` drawables. These sort ahead of
  `ic_sms_backup` (`app/build/intermediates/runtime_symbol_list/debug/processDebugResources/R.txt`
  lines 766-784 and 791), so the icon's ID renumbers. For example, `android:icon(0x01010002)=@0x7f080077`
  (U-060 `after-manifest-debug.txt` line 85) changes in both variants. Removed strings renumber the
  same way. A renumbered app ID is not a difference, because the merged diff already checks every
  reference by name. Framework IDs (`0x0101…`) are not normalised.

The debug and release packaged manifests differ from each other by `android:debuggable=true`, which
appears in debug only. That does not matter here, because each variant is diffed only against its own
baseline.

**Pass condition (all four diffs).** Every diff consists **only** of removed lines. Removed lines
that belong to one element count as that element, so the merged diff removes the whole `<queries>`
element. The removed elements are exactly these:

- From billing: the `com.android.vending.BILLING` uses-permission; `<queries>` with both
  `…InAppBillingService.BIND` and `…BillingOverrideService.BIND` intents; the
  `com.google.android.play.billingclient.version` meta-data; `ProxyBillingActivity`;
  `ProxyBillingActivityV2`.
- From the app: `.activity.donation.DonationActivity`.
- From billing-only transitives (Context fact 1): `GoogleApiActivity`; the
  `com.google.android.gms.version` meta-data; `TransportBackendDiscovery` with its `backend:` meta-data;
  `JobInfoSchedulerService`; `AlarmManagerSchedulerBroadcastReceiver`.

The transport libraries also contribute `INTERNET` and `ACCESS_NETWORK_STATE`, but the app declares
both itself (`app/src/main/AndroidManifest.xml` lines 56 and 61), so those permissions stay.

**Also assert, unchanged:**

- `package` and `versionCode=1602`
- `BackupBroadcastReceiver` with exported=true, no permission, and the `com.zegoggles.smssync.BACKUP`
  action (CNTR-MODERNIZATION-005)
- `SystemForegroundService` with `foregroundServiceType=0x1`
- no `InitializationProvider`
- `networkSecurityConfig` and `fullBackupContent` on `<application>`
- both `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` entries

Any other line is a failure. Explain it or escalate it; never suppress it.

### D6 — Release build: R8 and APK content

**Decision.** `app/proguard-rules.pro` has no billing rules (verified), so the project's own rules do
not change. The keep set shrinks only through rules that come from libraries and AAPT. The U-060
`after-configuration.txt` shows these at lines 16-21 and 27 (AAPT `-keep` for the two proxy activities,
the three datatransport components, `GoogleApiActivity` and `DonationActivity`), and in the
billing-7.1.1, gms and datatransport consumer-rule sections.

**Checks at I8:**

1. In the release `mapping.txt`, `grep -cE '^(com\.android\.billingclient|com\.android\.vending\.billing|com\.google\.android\.(gms|datatransport)|com\.google\.firebase\.encoders|com\.zegoggles\.smssync\.activity\.donation)\.'`
   must return 0. U-060 had 98 `com.android.billingclient` entries.
2. `apkanalyzer dex packages --defined-only` on the **debug** APK must list no `com.android.billingclient`
   or `…activity.donation` package. Use the debug APK because release names are obfuscated (`t1.a`);
   for release, check 1 is the authoritative check.
3. Run the U-060 C-2 kept-name check: `comm -23 kept-before.txt kept-after.txt` must print exactly one
   line, `com.zegoggles.smssync.activity.donation.DonationActivity`. It is in the U-060
   `kept-after.txt`. The C-2 prefix filter excludes billing and gms names, so they cannot appear.
4. Every line in the `configuration.txt` diff must be one of the removals listed above.

Any other kept name that disappears is a release regression. Stop and escalate it.

### D7 — Docs: engineering edits only

| File | Classification | Action in the story |
|------|----------------|---------------------|
| `README.md` lines 43-44 (donation paragraph), 241 (billing permission bullet) | Engineering. Both statements become false once the code is removed | Delete them. Keep lines 40-41 ("never be a pro / paid version") |
| `CHANGES` | Engineering | Add one bullet under `== 1.6.0 (xxx) dd-mm-2019` in the two-space `  * ` style, saying the in-app donation feature and the Play Billing dependency were removed. Do not edit lines 23, 120, 138 or 168 (they are history) |
| `sdlc/artifacts/engagement/code-location.md` line 37 | Engineering | Delete the billing bullet |

`metadata/play/**` is inherited upstream store text; this is a private, unpublished app, so these
files are untouched and out of scope.

### D8 — Supersession records (no edits made by this design)

- **REQ-MODERNIZATION-010** gets a `change_records` entry: intent superseded by REQ-MODERNIZATION-018;
  decision date 2026-11-12; product owner decision "remove the donation flow"; AC-1..4 not applicable.
  Its status follows the artifact state machine. Its text is not edited.
- **REQ-MODERNIZATION-018** gets the reciprocal change record. Its `related_requirements` already
  lists REQ-MODERNIZATION-010.
- **DES-MODERNIZATION-010** (status `approved`) gets a superseded annotation pointing to
  DES-MODERNIZATION-013.

These are artifact-lifecycle edits made through the Artifact Librarian or Requirements Analyst. They
are tracked as story AC-10 and must be done before the story closes. This document lists
DES-MODERNIZATION-010 in `related_design_docs`.

## Integration Contracts

This removal introduces **no new interface**, and it removes no approved contracted interface.

**Evidence.** `grep -niE "billing|donat|DonationActivity" sdlc/artifacts/design/contracts/*.md`
returns zero matches across CNTR-MODERNIZATION-001..008, which are all `status: approved`.
DES-MODERNIZATION-010 (Integration Contracts section, lines 396-410) classified billing as a leaf
integration with no CNTR. The only inbound edge, `MainSettings` → `DonationActivity`, was an
intra-module static call, and this design deletes it.

Several contracted surfaces live in the packaged manifest and the R8 keep set:

- CNTR-MODERNIZATION-005 (exported `BACKUP` receiver)
- CNTR-MODERNIZATION-004 (worker class-name identity and the `InitializationProvider` removal)
- CNTR-MODERNIZATION-001/002 (`networkSecurityConfig`)
- CNTR-MODERNIZATION-003 (`fullBackupContent`, Tink keep rules)

The removal regenerates both the manifest and the keep set, so D5 and D6 assert these surfaces
explicitly as unchanged.

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| None introduced or altered | — | — | — | — | n/a |

### Contracts Needed (pre-sprint gate)

- [x] None. The contract gate is satisfied, and the story's `integration_contracts` may stay empty.

## Design Validation

### Requirement traceability (REQ-MODERNIZATION-018)

| AC | Satisfied by | Proof |
|----|--------------|-------|
| 1 | I2–I5 | Directory absent. The grep returns 0 results (`GooglePlayServices.java` matches neither term) |
| 2 | I2 | `onResume()` body as in Post-removal shape. `grep 'key="donate"'` returns 0. On-device check below |
| 3 | D1 I0 and I8, D5 | Merged-manifest diffs (`processDebugManifest`, `processReleaseManifest`; symbolic names) plus normalised packaged `aapt2` diffs (`(line=N)` stripped, `@0x7f…` IDs replaced), debug and release, each against its own I0 baseline. The removal list is the one in amended AC-3 (CR-003; formerly SC-1) |
| 4 | D4a, D6 | `dependencies` for both runtime classpaths shows no `com.android.billingclient`. Debug dex listing |
| 5 | I8 | `:app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` exits 0 with the daemon on JDK 25 (`gradle/gradle-daemon-jvm.properties`, U-060) |
| 6 | D2 | The `EnrollmentDialogThemeTest` diff changes only these: the added nested `HostActivity`; at lines 117, 179 and 244, the local-variable type and the `.class` argument (`DonationActivity` → `HostActivity`); the removed import; the `{@link}` at line 164 and the theme comments. No assertion changes |
| 7 | below | 682 run / 2 skipped |
| 8 | D3 | Rule diff. The root value goes in the Rule 4 header comment and/or the trailing `// MEASURED` comment on its `minimum` line, with `0.50` unchanged. The other five re-measured values go in the implementation log |
| 9 | D4 | `verification-metadata.xml` unchanged (or writer-removal-only) |
| 10 | D8 | Change records exist |
| 11 | D7 | Diff of `README.md`, `CHANGES` and `code-location.md`. Amended AC-11 has no store-listing clause (CR-003) |
| 12 | Post-removal shape | Code evidence: in `MainSettings`, `Keys.DONATE` appears only in the static import (line 24) and in `findPreference(DONATE.key)` inside `checkUserDonationStatus()` (line 172). Both are removed in I2. Nothing persists it, and `reset()` does not touch it |

### Test-count arithmetic

The arithmetic is 697 − (10 + 5) = **682 run, 2 skipped**. Both 697 figures are verified (`git grep`
count at HEAD, and the sum of Gradle XML results). The skipped pair is
`AppTest.shouldGetVersionName` and the `AuthPreferencesTest` AndroidKeyStore test. `HostActivity` adds
no `@Test`. Cross-check: the CLAUDE.md `git grep` count drops by exactly 15. If any other story merges
first, recompute from the I0 baseline. Do not reuse 697.

### On-device check

Install the debug APK on the API 36 emulator used in U-060 and open Settings. Text evidence only (the
sprint-017 evidence-hygiene rule):

- `uiautomator dump` output contains no `Donate` title.
- The last visible entry is the final `PreferenceScreen`.
- `logcat` shows 0 `FATAL EXCEPTION` across launch → Settings → back.

Check that third-party integration is off before the run, because of the sprint-017 broadcast
incident.

### Rollback

Rollback is `git revert` of the story commits. The removal persists nothing and migrates nothing
(AC-12), and verification metadata is unchanged (D4). A revert therefore restores the prior APK
exactly, with no user-data consequence.

### Design-audit self-check

| Prompt topic | Status |
|--------------|--------|
| Layers, data model, API, caching, deployment topology | Not applicable. Nothing is added; the only layer affected (presentation) is covered in Post-removal shape |
| Error handling | Not applicable. The only error path removed is `checkUserDonationStatus`'s `catch (Exception)` log |
| Security | Net reduction in attack surface: one permission, two package-visibility queries, six library components (two billing proxies, `GoogleApiActivity`, three datatransport), and one IPC client fewer |

## Trade-offs

- **One story instead of two.** The diff is larger, but it gives a single coherent APK delta and
  avoids two evidence cycles (D1).
- **A test-only host instead of a production host.** It buys test isolation and stable coverage. AC-6
  originally asked for a production host; it was amended by CR-003 to allow this (D2, SC-2).
- **Stale verification entries kept.** The file is slightly untidy, but the supply-chain change stays
  "zero changes" and reviewable (D4).
- **Store text left untouched.** `metadata/play/**` keeps inherited upstream wording that no longer
  matches the app. That costs nothing, because the app is private and unpublished (D7).

## Risks

| # | Risk | Likelihood / impact | Mitigation |
|---|------|--------------------|------------|
| R1 | A transitive downgrade changes androidx behaviour or the manifest | Low (billing raises no resolved version) / Med | D4a diff; stop and escalate |
| R2 | The `receiver` rule is at 50.0% with zero headroom | Low (no donation lines there) / gate fail | D3 re-measure; add a test rather than lower the floor |
| R3 | A release-only manifest difference goes unnoticed | Low / Med | Amended AC-3 (CR-003) and D5 require both the merged and the packaged release diffs. History: the original REQ note said only the debug merged manifest existed locally; the U-060 release dump has since been used as reference evidence |
| R4 | Robolectric fails to launch the undeclared `HostActivity` | Very low (`DonationActivity` already runs undeclared) / test fail | I1 runs the test class in isolation before I2 |
| R5 | Inherited `metadata/play/**` text still mentions donations | Certain / none (private, unpublished app) | Out of scope (D7) |

## Notes

### Scope Concerns (history; resolved by CR-003)

These concerns were raised during design, where the original REQ-MODERNIZATION-018 wording conflicted
with the repository or with the design. All three were resolved on 2026-11-12 by CR-003
(`sdlc/artifacts/build/change-records/CR-003-req-018-manifest-scope-private-app.md`). The record of
each concern is kept below, together with what the requirement now says.

- **SC-1 (AC-3).**
  - *Raised:* AC-3 ("nothing else is … removed") could not pass, because billing's transitive libraries
    also contribute manifest entries.
  - *Now:* AC-3 lists these removals: the `com.android.vending.BILLING` permission; the whole
    `<queries>` element; the `billingclient.version` meta-data; both proxy activities;
    `DonationActivity`; and the five transitive entries (`GoogleApiActivity`, the
    `com.google.android.gms.version` meta-data, `TransportBackendDiscovery` with its `backend:`
    meta-data, `JobInfoSchedulerService`, `AlarmManagerSchedulerBroadcastReceiver`). It applies to both
    the merged and the packaged manifests, debug and release, with `(line=N)` stripped and `@0x7f…` IDs
    normalised (D5). `INTERNET` and `ACCESS_NETWORK_STATE` remain.
- **SC-2 (AC-6).**
  - *Raised:* the original wording required "an AppCompat activity that remains in the app", but D2
    chose a test-only host.
  - *Now:* AC-6 specifies a test-only subclass of `ThemeActivity`, declared as the concrete
    `public static` nested class `EnrollmentDialogThemeTest.HostActivity`. The
    `AccountManagerAuthActivity` fallback is no longer needed.
- **SC-3 (AC-8, AC-11, AC-12, inventory).**
  - *Raised:*
    - AC-8's re-measure list omitted `preferences` and `utils`.
    - Item 15 and AC-11 covered only three store-listing locales.
    - AC-12 included wording about users who had previously donated.
  - *Now:*
    - AC-8 includes `preferences` and `utils`.
    - The product owner's decision that this is a private app removed inventory item 15 and the
      store-listing clause from AC-11. `metadata/play/**` is out of scope (D7).
    - AC-12 and the "Persisted donation state" context keep only the engineering facts. No
      `SharedPreferences` key is persisted, migrated or cleared, and `Keys.DONATE` is removed.
    - Inventory item 7's per-file range reads "3 to 12".

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| REQ-MODERNIZATION-018 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-018-remove-donation-flow-and-play-billing.md | Governing requirement |
| REQ-MODERNIZATION-010 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-010-upgrade-play-billing-to-v7.md | Superseded requirement (frontmatter) |
| DES-MODERNIZATION-010 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-010-play-billing-v7-design.md | Superseded design; Integration Contracts section |
| CNTR-MODERNIZATION-001..008 | sdlc/artifacts/design/contracts/ | Status and grep for billing/donation |
| sprint-017 retro, U-060 qa-results, contract-verification, plan, evidence | sdlc/artifacts/build/sprints/sprint-017/ | AGP 9 unit-test manifest; C-1/C-2 methods; manifests, mapping, configuration, dependency trees |
| Source | app/src/main/AndroidManifest.xml, app/build.gradle, app/src/main/res/xml/preferences.xml, app/src/main/res/values*/strings.xml, app/src/main/java/com/zegoggles/smssync/{Consts.java, activity/ThemeActivity.java, activity/fragments/MainSettings.java, preferences/Preferences.java, activity/auth/*.java, activity/MainActivity.java, activity/donation/*.java} | Inventory and integration points |
| Tests | app/src/test/java/com/zegoggles/smssync/activity/fragments/EnrollmentDialogThemeTest.java, app/src/test/java/com/zegoggles/smssync/activity/donation/*.java | Host usage, counts |
| Build/config | gradle/verification-metadata.xml, app/lint-baseline.xml, app/proguard-rules.pro, .github/workflows/ci.yml | Verification, lint and R8 posture |
| Docs | README.md, CHANGES, metadata/play/text/*/full_description.txt, sdlc/artifacts/engagement/code-location.md | D7 |
| Local build outputs | app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml, app/build/test-results/testDebugUnitTest/, app/build/intermediates/packaged_manifests/debugUnitTest/ | Coverage numbers, counts, unit-test manifest |
