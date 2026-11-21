---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-015
  - REQ-MODERNIZATION-016
  - REQ-MODERNIZATION-017
related_stories:
  - BUG-021
  - BUG-022
related_design_docs:
  - DES-MODERNIZATION-004
  - DES-MODERNIZATION-005
  - DES-MODERNIZATION-006
  - DES-MODERNIZATION-013
integration_contracts:
  - CNTR-MODERNIZATION-001
  - CNTR-MODERNIZATION-003
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-005
  - CNTR-MODERNIZATION-006
  - CNTR-MODERNIZATION-008
change_records:
  - CR-009
  - CR-010
  - CR-011
  - CR-013
  - CR-014
  - CR-015
  - CR-016
  - CR-017
type: ''
id: DES-MODERNIZATION-014
title: 'Toolchain, Runtime Libraries and Platform Level 37: One Sequenced Upgrade Plan (REQ-MODERNIZATION-015, -016, -017)'
domain: modernization
updated_at: '2026-11-21T08:45:43.427Z'
---

# DES-MODERNIZATION-014: Toolchain, Runtime Libraries and Platform Level 37

> Note added 2026-11-21 (references only; no decision of this design changes). The change records this
> design calls P-1, P-2 and P-3 exist. P-1 is CR-015 (CNTR-MODERNIZATION-004 and DES-MODERNIZATION-005: the
> pre-API-24 content-trigger fallback). P-2 and P-3 are one record, CR-016 (requirements wording), as this
> design allows when the record is raised before S1. Throughout, read "change record P-1" as CR-015 and
> "change record P-2" or "P-3" as CR-016. Once a record is applied (status `implemented`), every condition
> this design places on it is met. That includes "Conditional on P-3" in the traceability table, "until it
> rules" in D8 item 5, and the remediations of FD-VAL-002 and FD-VAL-003. After CR-016 is applied,
> REQ-MODERNIZATION-015 AC-13, REQ-MODERNIZATION-016 AC-6 and REQ-MODERNIZATION-017 AC-15 state the practice
> this design already follows: the count at the start of each story, and the configured floors.

> Note added 2026-11-21 by CR-017 (no decision of this design changes). The SMS-received receiver
> (`SmsBroadcastReceiver`) is not delivered `SMS_RECEIVED` and is to be removed by its own story (product owner's
> decision, decision appendix Record 20, ruling 56). Run R and its Design Validation row read only the
> `BackupTriggerWorker` line. The behaviour-change row 37-5, the "Integration Contracts" argument for CR-015
> and the D14 file list name the receiver as it was when they were written; they are history and are not edited.

## Overview

This design covers the first three requirements of EPIC-MODERNIZATION-006. REQ-MODERNIZATION-015
moves the build toolchain and test tooling to latest stable and raises minSdk to 24.
REQ-MODERNIZATION-016 moves the runtime libraries to latest stable. REQ-MODERNIZATION-017 raises
compileSdk and targetSdk to 37 (Android 17), with a behaviour-change review and an on-device
validation. All three must be complete before any REQ-MODERNIZATION-019 Part B row compiles
Thunderbird code (epic Sequencing; analysis decision Record 4).

The three requirements are designed together because their versions constrain one another:

- Robolectric 4.18 cannot emulate any SDK below 24, so it needs no test configured below 24.
- WorkManager 2.12 needs minSdk 24.
- Kotlin 2.4 needs Hilt 2.60.
- compileSdk 37 needs AGP 9.1.1 or newer.
- targetSdk 37 brings a platform behaviour that this app's backup model is not built for.

The design makes 19 decisions, D1 to D19, and splits the work into **five stories, S1 to S5**, run in
order. Each story ships on its own with every gate green, and each is made of ordered increments
that compile. The headline findings:

1. **One finding blocks only the targetSdk raise (S5).** On Android 17, an app targeting 37 cannot
   see an SMS that contains a one-time code for three hours. The backup selects
   `date > watermark`, and the watermark only moves forward. A withheld message that keeps its
   original date is therefore skipped for ever once any later message has been backed up (D14).
   **Orchestrator ruling (decision appendix Record 13, row 4, made under Record 12): fix first, then
   targetSdk 37.** S5 measures the gap first, with a positive control. BUG-021 has been raised; it
   covers the WebOTP all-targets case and the target-37 case. If the measurement confirms the gap,
   BUG-021 is designed and built before S5-I4. The completeness rule it implements is
   REQ-MODERNIZATION-020. compileSdk 37 does not wait for any of this. The product owner is
   informed, not asked.
2. **No version set at stable versions sits inside every vendor's published range.** The research's
   claim that Gradle 9.7.0 + AGP 9.3.1 + Kotlin 2.4.21 does so is corrected in D3. The procedure
   and the pre-specified fallbacks are in D3.
3. **The machine already has an API 37 system image and an API 37 virtual device.** That device is
   `Pixel_10_Pro`, which is the emulator never to touch, by standing instruction of the product
   owner (Record 14, row 2). There is no API 24 image (D16). Both device needs therefore go to the
   product owner.
4. **A latent defect outside these requirements turned up, F-1.** The three cooperative-cancellation
   checks in the workers do nothing. It has been raised as BUG-022, which is fixed as its own small
   story after U-066 and before S2 (Record 14, row 11; D2). Its tests then serve as the stop rule
   in S2-I5, S4-C and S4-W (D10).
5. **Three change records are mandatory preconditions** (Record 14, row 9):
   - P-1, on CNTR-MODERNIZATION-004 and DES-MODERNIZATION-005, before S1;
   - P-2, requirements wording, before S1;
   - P-3, requirements wording, before S4.

   They are in D2.

The design was written against HEAD `fba733e5` on branch `sdlc/modernization-plan` (2026-11-21).
At that HEAD the test sources hold 732 `@Test` methods, 2 of them `@Ignore`d. The audit revision is
checked against HEAD `b9fd172f`, which adds only CR-014 Amendment 1 (see "In-flight work"). The
gates are
`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease`.

## Context

### What exists today (verified in the working tree)

| Item | Value | Location |
|---|---|---|
| Modules | `:app`, `:metadata`, `:k9mail-vendored` | `settings.gradle:1` |
| Gradle wrapper | 9.5.1 with `distributionSha256Sum` | `gradle/wrapper/gradle-wrapper.properties:3-4` |
| Build JDK | 25 (daemon JVM criteria) | `gradle/gradle-daemon-jvm.properties:2` |
| AGP / KGP / KSP / Hilt plugin | 9.0.1 / 2.3.21 / 2.3.6 / 2.59.2 | `build.gradle:10`, `:14`, `:17`, `:21` |
| Test reports | HTML and JUnit XML switched off for every `Test` task | `build.gradle:29-30` |
| Platform levels | compileSdk 35, minSdk 21, targetSdk 35 | `app/build.gradle:21`, `:24`, `:25` |
| Vendored module levels | compileSdk 35, minSdk 21, Java 8 | `k9mail-vendored/build.gradle:52`, `:54`, `:76-77` |
| Bytecode target `:app` | 17 (Java and Kotlin) | `app/build.gradle:90-91`, `:107` |
| JaCoCo | 0.8.14, 18 per-package LINE rules, class-root guard | `app/build.gradle:211-229`, `:340-589` |
| Robolectric default SDK | `sdk=29`; its comment says it matches compileSdk, which is stale | `app/src/test/resources/robolectric.properties:1-8` |
| JVM flags for tests | none; comment says Robolectric 4.15 needs none | `app/build.gradle:592-594` |
| Dependency verification | 803 components, 1,460 artifact entries, `verify-metadata` true, `verify-signatures` false, two trusted-artifact regexes (javadoc, sources) | `gradle/verification-metadata.xml:4-9` (counted with `grep -c`) |
| Lint | `warningsAsErrors true`, baseline with 38 issues (10 `ObsoleteSdkInt`, 1 `ExportedReceiver`, 5 `GradleDependency`, others), produced by lint 8.7.3 | `app/build.gradle:65-77`, `app/lint-baseline.xml`, `app/lint.xml` |
| R8 | release `minifyEnabled true`, `proguard-android-optimize.txt` plus `-dontoptimize` | `app/build.gradle:53-54`, `app/proguard-rules.pro:57` |
| Windows `gradlew` block | hand-maintained `JAVA_TOOL_OPTIONS` export under cygwin/msys only | `gradlew:117-127` |
| CI | runs only on pull requests to, and pushes to, `master`; checkout@v4, setup-java@v4 (temurin 25), build-tools 36.0.0, cache@v4 | `.github/workflows/ci.yml:3-9`, `:17`, `:22-25`, `:28-30`, `:33`, `:41` |
| Native code | 0 `.so` files in the debug and release APKs built on 2026-11-20 | `unzip -l` of `app/build/outputs/apk/*/*.apk` |

### Where the requirement texts have been overtaken

The requirement tables are a 2026-11-12 snapshot. The research of 2026-11-21
(`sdlc/artifacts/build/research/2026-11-21-latest-stable-versions.md`) is newer and governs "latest
stable at planning time" in this design. Where they differ, the research wins:

- Gradle 9.9.1 → **9.9.2**
- AGP 9.5.1 → **9.5.2**
- KSP 2.3.13 → **2.3.14**, because 2.3.13 now carries "Defect in release"
- Robolectric 4.17 → **4.18.1**
- Lifecycle 2.11.0 → **2.11.1**

All from research §1.

Other facts have moved since the requirements were written:

- **Test-count baseline.** REQ-015 AC-13, REQ-016 Context and REQ-017 AC-15 quote 697 as the
  sprint-017 baseline. Since then, sprint-018 deleted the donation tests and U-063 to U-065 added
  TLS tests. The count at HEAD is 732. Each story records the count at its own start and accounts
  for every difference by test name, as story U-066 already does (its AC-12). This design does not
  reinterpret the approved "697" wording. The requirements-wording change record (precondition P-2)
  rules on it before S1 is planned.
- **CI action versions.** REQ-015 Description puts them in scope; the research does not cover them.
  They were read on 2026-11-21 from the release pages and are in D1.

### Product-owner decisions this design is bound by

These come from the "Product owner decision" appendix of
`sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`:

- **Record 3:** the full migration goes ahead (Part B).
- **Record 4:** a fork of Thunderbird consumed as Android variants (mode M2), so minSdk 24 and
  compileSdk 37. targetSdk goes to 37: "Raise to 37 (Recommended)".
- **Record 5, row 8:** "all my devices are 11 or newer".
- **Record 11, item 3:** "3 start there".
- **Record 12:** the goal is "get the app completed". Routine choices are the orchestrator's, on the
  recommendation of the agent that owns the question. These still go to the product owner:
  - scope changes and changes to a recorded decision;
  - anything that costs compatibility for a device or server in use;
  - work under the owner's accounts outside this repository;
  - anything involving a password.

  Commits and pushes go only to `sdlc/modernization-plan`. There is no merge to `master` and no
  force-push.
- **Record 14, standing instructions for device work:**
  - row 1: adding or deleting SMS on the emulator, and adding or removing the SMS folder in the mail
    account, are safe;
  - row 2: `Pixel_10_Pro` is never started and never touched;
  - row 3: only the owner enters passwords;
  - row 4: system settings on an emulator are the owner's to change.

  Record 14 notes what these do not cover, and is therefore still asked: creating new virtual
  devices, downloading images or tools, and backups from a device the owner has not configured
  (PO-2, PO-3).
- **Authorisation for `Pixel_9_Pro` (2026-11-20):** the owner answered "Start it for me" for story
  U-065's emulator check. That allows the orchestrator to start `Pixel_9_Pro` and to install a debug
  build over the configured app there. What goes beyond this and Record 14 is listed under PO-3.

### Orchestrator rulings that bind this design (made under Record 12)

- **PO-1 (Record 13, row 4):** fix first, then targetSdk 37. The fix is required only if the S5
  measurement confirms the gap. The ruling keeps Record 4 and costs no device in use, so the product
  owner is informed, not asked.
- **PO-6 (Record 13, row 5):** not applicable. The only mail server in use is Gmail (Record 5,
  row 1), so row 37-6 is NA.
- **PO-5:** replacing `security-crypto` stays a recommendation. The orchestrator puts it to the
  owner; it gates no story here.
- **P-1 to P-3 (Record 14, row 9):** the three change records are mandatory preconditions of the
  stories they precede (D2).
- **BUG-021 (Record 14, row 10):** its decision-point defaults apply, including a bounded look-back
  of 6 hours.
- **BUG-022 (Record 14, row 11):** fixed as its own small story, after U-066 closes and before the
  toolchain story S2.

### In-flight work this design must not collide with

Story U-066 (sprint-022, `status: in-progress`) prunes the weak cipher suites in
`k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`. Its AC-11(e)
requires that no build file and no verification metadata change while it runs. Its tests derive
expectations from the TLS provider that Robolectric installs, and S2 changes Robolectric.
**S1 starts only after U-066 is closed.**

HEAD `b9fd172f` adds CR-014 Amendment 1. Under it, the cipher allow-list is derived from the
platform's default-enabled suites. Under Robolectric, "the platform" is the Conscrypt provider that
Robolectric brings, so S2-I7 (Robolectric 4.18.1) can change the input that U-066's exact-list tests
filter. The handling:

- U-066's exact-list tests must pass unchanged at S2-I7.
- If the provider's default-enabled set changes so that a filtered result differs, that is a
  test-environment change, not a factory regression. It is recorded with both lists.
- The applier check that CR-014 sets for the migration's contract revision (CR-014, "At application
  time the applier ... confirms points 1 to 6 held") then re-confirms against the Robolectric
  version in use at that time.
- Tests are never loosened to pass.

## Assumption Ledger

Every row marked `assumed` or `needs_check` is either verified before a story relies on it, or it is
an explicit first step of a story with a stop rule. That step is named in the last column.

| # | Statement | State | Evidence or where it is settled |
|---|---|---|---|
| A1 | Version lines and values in the build files are as tabled in D1 | verified | `build.gradle:10,14,17,21`; `app/build.gradle:21-25,117-187,212`; `k9mail-vendored/build.gradle:52,54,100-114`; `gradle/wrapper/gradle-wrapper.properties:4`; `gradle/gradle-daemon-jvm.properties:2` |
| A2 | Exactly 7 SDK-level sites at level 24 or lower and 12 at 25 or higher exist in `app/src/main` (19 lines, import lines excluded) | verified | `grep -rnE "SDK_INT\|VERSION_CODES\|TargetApi\|RequiresApi" app/src/main`; agrees with CR-011 Verification Evidence item 9 |
| A3 | Exactly 5 tests run below SDK 24 (4 at LOLLIPOP, 1 at `sdk = [23]`) | verified | `SmsReceiverTest.java:27,32,37,42`; `WorkManagerSchedulerTest.kt:378` |
| A4 | The authoritative test count is 732, with 2 `@Ignore` | verified | project command; `AppTest.java:13`, `AuthPreferencesTest.java:1079` |
| A5 | A message whose provider `date` is at or below the SMS watermark is never selected again | verified (code) | `BackupQueryBuilder.java:103-116`, `BackupWorker.kt:403-423`, `ConversionResult.java:19-30`, `mail/HeaderGenerator.java:67` (sets `X-smssync-date` from the provider `date`). The watermark also moves forward on restore (`RestoreWorker.kt:463-467`) and on the skip path (`BackupWorker.kt:198-217`) |
| A6 | On Android 17 at target 37 a withheld OTP SMS keeps its receipt-time `date` when it becomes visible | needs_check | S5-I1 measurement M0–M7 with a positive control (M1a) and a stop rule (D14) |
| A7 | `coroutineContext.ensureActive()` at `BackupWorker.kt:331`, `RestoreWorker.kt:311`, `:399` binds to `CoroutineWorker`'s deprecated `coroutineContext: CoroutineDispatcher` and so has no Job to check | verified | U-060 compiler warnings ("'val coroutineContext: CoroutineDispatcher' is deprecated", same three lines, `sdlc/artifacts/build/sprints/sprint-017/U-060/implementation-log.md` §1e); kotlinx.coroutines `CoroutineContext.ensureActive()` does nothing without a Job |
| A8 | An API 37.0 system image (`google_apis_playstore_ps16k`, x86_64) and platform `android-37.0` revision 2 are installed. AVD `Pixel_10_Pro` targets android-37.0. No API 24 image exists. `cmdline-tools` (sdkmanager, avdmanager) are not installed. Build Tools 36.0.0 and 36.1.0 are installed, 37.0.0 is not | verified | listing of the SDK folder named in `local.properties`; `emulator -list-avds`; AVD `.ini` files |
| A9 | `Pixel_10_Pro` is the emulator never to touch | verified | Standing instruction of the product owner, recorded in the decision appendix, Record 14, row 2; consistent with U-060 Step 10 and `U-060/evidence/qa/qa-ac5-device.txt:2` |
| A10 | Platform certificate transparency applies to the default factory's trust path | assumed | Code: `TrustManagerFactory.java:76`, `:106-107` uses the platform trust manager with the two-argument check. Whether CT runs on that call is not documented in the research. The disposition does not depend on it (D15) |
| A11 | ECH is not used on the app's raw `SSLSocket`s | assumed | Research §4.5: "when the networking library supports it". Observed by the REQ-017 AC-9 handshake at 37 (D15) |
| A12 | No REQ-016 library forces compileSdk above 35 | verified (research) | Research §2.3: "The directly declared latest-stable set above does not force androidx.core:core above 1.12.0". Moot anyway: S3 (compileSdk 37) precedes S4 |
| A13 | AGP 9.5.2 accepts the `compileSdkVersion` / `minSdkVersion` / `targetSdkVersion` DSL names | needs_check | S2-I3 build. If forced to `compileSdk` and the like, the change is recorded and the AC-2 / AC-5 greps are adapted (D12) |
| A14 | Robolectric 4.18.1 still offers `@LooperMode(LEGACY)` and `@ConscryptMode(OFF)`, used by `DefaultTrustedSocketFactoryJdkProviderTest.java:130-131` | needs_check | S2-I7. The class's three guards fail by name if isolation breaks (U-063 deviation 14) |
| A15 | Robolectric 4.18.1 runs at SDK 24 and 29 on JDK 25 without `--add-opens` | needs_check | S2-I7 procedure (D7) |
| A16 | Whether WorkManager 2.9.1 can open a database that 2.12.0 has migrated | unknown | Not verified, by the audit either. S4 planning reads WorkManager's database migration and downgrade settings before PO-3's cost is stated. Until then rollback is described as possibly needing an app-data clear (D19) |
| A17 | The research's "Gradle 9.7.0 + AGP 9.3.1 + KGP 2.4.21 sits inside every published range" holds | contradicted | The Gradle 9.7.x page, as read for U-060, lists Kotlin "through 2.4.20-Beta1" (`U-060/implementation-log.md` §1d). That log does not show the page's own fetch date; the planner's sources were fetched 2026-11-11. Corrected in D3 |
| A18 | The CI runner image `ubuntu-22.04` is still offered | needs_check | S2-I13 re-reads GitHub's runner-image list. If retired, the change is recorded |
| A19 | No dependency coordinate is a `-SNAPSHOT` | verified | `grep SNAPSHOT` over the four build files: no match |
| A20 | WorkManager's `setProgress` is a suspension point at which a cancelled worker stops | not verifiable here | The library source is not in this repository. The loops call it on every pass (`BackupWorker.kt:361`, `RestoreWorker.kt:321`, `:406`). F-1's bound rests on it (D10; BUG-022) |
| A21 | `androidx.test:monitor` (pulled by Robolectric 4.18.1 at 1.8.0) has a `minCompileSdk` no higher than 35 | not verified | If it is higher, S3 moves to just after S2-I3 (D2, N-6 rule) |
| A22 | The rule IDs of the 39 pre-existing lint errors | not verified | U-060 recorded only counts and the first finding (`ContactAccessorTest`, `CheckResult`). S1-I0 records the rule IDs |

## System Architecture

### Architectural drivers

| # | Driver | Source | Requirement or goal | Impact on the design |
|---|---|---|---|---|
| AD-1 | Latest stable at planning time, recorded | REQ-015 AC-1, REQ-016 AC-1, epic SC-1 | Every in-scope component equals the newest non-pre-release version on its planning date | D1 table and version-movement rule; every story re-resolves its own rows |
| AD-2 | Mutual compatibility outside documented ranges | REQ-015 AC-2 to AC-4, research §3.2 | No set at stable versions is inside every vendor range | D3 procedure; increments ordered so each failure points at one component |
| AD-3 | minSdk 24 floor | REQ-015 AC-5 to AC-10, CR-011, Record 4 | Floor set by the Thunderbird Android variants | S1 first; enables Robolectric 4.18 and WorkManager 2.12 |
| AD-4 | Platform level 37 with on-device proof | REQ-017, Record 4, epic SC-3 | Unit tests cannot prove a platform raise (BUG-014, BUG-016 history) | compileSdk and targetSdk split (S3, S5); device plan D16 |
| AD-5 | Gates never weakened | REQ-015 AC-11/12/14, REQ-016 AC-4/6, REQ-017 AC-15, epic SC-8 | Verification enforced, floors not lowered, JDK 25 | D5 writer protocol; D8 floors; D18 gates |
| AD-6 | Attribution of every regression | REQ-015 Constraints "Increments", REQ-016 Constraints | One version bump per verified increment | Increment ladders in D2 |
| AD-7 | Backup completeness preserved | REQ-017 Constraints "Behavior preserved", REQ-015 Constraints | Identical backup and restore results | D14 gates targetSdk 37 on the OTP question |
| AD-8 | Part B readiness | epic Sequencing, CR-011 | Nothing here may make the retained mail libraries or the vendored-code move harder | D4b leaves the vendored module alone; D3 F4 flags a Kotlin fallback as a Part B consequence |

### The system being changed

This is a build-system and platform design, not a feature design. The parts it touches are four
layers of the existing app and its build:

```
 +---------------------------------------------------------------+
 | L4 Platform contract    compileSdk / targetSdk / minSdk,      |
 |                         manifest, platform behaviours (S1,S3,S5)|
 +---------------------------------------------------------------+
 | L3 Runtime libraries    WorkManager, Lifecycle, coroutines,   |
 |                         preference, annotation, core-role,    |
 |                         security-crypto (S4)                  |
 +---------------------------------------------------------------+
 | L2 Test tooling         Robolectric, Mockito, Truth, GreenMail,|
 |                         JUnit, auto-service, JaCoCo (S2)      |
 +---------------------------------------------------------------+
 | L1 Build toolchain      Gradle, AGP, KGP, KSP, Hilt plugin,   |
 |                         JDK 25, Build Tools, CI (S2)          |
 +---------------------------------------------------------------+
   Cross-cutting fitness functions: dependency verification,
   JaCoCo floors, manifest invariants, lint, R8 keep set, greps
```

Dependency direction for change: L4 depends on L1, because compileSdk 37 needs AGP 9.1.1 or newer
(research C5). L2 depends on L4's floor, because Robolectric 4.18 cannot run a test configured
below SDK 24 (C15), and S1 removes those tests together with the floor raise. L3 depends
on L4's floor, because WorkManager 2.12 needs minSdk 24 (C18). That dependency graph sets the
story order in D2.

Module structure is unchanged. `:app` stays the single application module. `:k9mail-vendored`
stays until REQ-019 Part B deletes it. `:metadata` is untouched. No package moves. No production
class is added, except one test class (`BackupBroadcastReceiverTest`, D8).

Patterns this design leans on, and why:

- **Fitness functions** (Ford, Parsons, Kua, *Building Evolutionary Architectures*, Ch. 2). The
  gates in D18 are executable architecture checks, each tied to a quality attribute:
  - dependency-verification additions-only proof (security, supply chain);
  - JaCoCo floors (maintainability);
  - merged-manifest invariants for `SystemForegroundService`, `InitializationProvider` and
    `BackupBroadcastReceiver` (reliability, CNTR-005 compatibility);
  - no `.so` in the APK (portability, 16 KB pages);
  - the AC-6 and AC-7 greps (maintainability).
- **Characterisation tests as a safety net** (Feathers, *Working Effectively with Legacy Code*,
  Ch. 13). The U-063 to U-065 TLS tests and the BUG-010 watermark tests pin behaviour that the
  toolchain move must not change. No test is loosened to pass. A test that fails after a version
  bump is either a regression or a provider difference, and is investigated as such.
- **Small batches** (Humble and Farley, *Continuous Delivery*, Ch. 1, "deliver small changes"). One
  component per increment, each committed and green, so `git bisect` across a story points at one
  version.
- **Ports and adapters** (Cockburn's Hexagonal Architecture). CNTR-MODERNIZATION-003 makes the
  credential store a port, so the deprecated `security-crypto` adapter can be replaced later
  without touching callers. D11 uses that to keep the replacement out of scope.
- **Remove Dead Code** (Fowler, *Refactoring*, 2nd ed.). It applies to the 7 pre-24 sites in D4.
  The precedent is DES-MODERNIZATION-006 (Dead-Code Removal and minSdk Cleanup), which did the same
  for the minSdk 21 raise. Its retarget of `SmsReceiverTest` to LOLLIPOP is the comment S1-I3
  rewrites.

Anti-patterns found while reading the code, each handled by a decision:

- **Lava flow / dead code.** Seven guards for levels the app will no longer run on (D4).
- **Documentation rot.** These comments no longer describe the code:
  - `robolectric.properties:1-7` says sdk 29 matches compileSdk;
  - `app/build.gradle:592-594` says no flags are needed, a statement made for Robolectric 4.15;
  - `app/build.gradle:153` says "minSdk 21";
  - `CHANGES` says "Android minSdk now 14".

  D4 and D6 correct them.
- **Name shadowing producing a silent no-op.** The worker loops call `coroutineContext.ensureActive()`.
  It resolves to the inherited deprecated property `CoroutineWorker.coroutineContext`, a
  dispatcher, not the imported `kotlin.coroutines.coroutineContext`. Cancellation checks therefore
  do nothing (F-1, D10).
- **Monotonic watermark over late-arriving data.** An incremental extractor that keeps only a
  high-water mark loses "straggler" records that arrive with old timestamps. Kleppmann,
  *Designing Data-Intensive Applications*, Ch. 11, "Reasoning About Time", covers this. Android
  17's OTP delay turns it into a real case (D14).

### D1 — Target version of every component in scope

**Context.** REQ-015 AC-1 and REQ-016 AC-1 require the build files to equal the latest stable
version, re-resolved on the story's planning date, with the date and source recorded. The research
of 2026-11-21 is the planning-time record for this design.

**Decision.** The targets are those in the table below. Sources are given in full or with the
research's abbreviations: MC is `https://repo1.maven.org/maven2/<group path>/<artifact>/maven-metadata.xml`,
GM is `https://dl.google.com/dl/android/maven2/<group path>/<artifact>/maven-metadata.xml`. Each
story repeats the resolution for its own rows on its planning date (D1a).

| Component | Today (file:line) | Target | Story | Source (read 2026-11-21) | Why not newer, or note |
|---|---|---|---|---|---|
| Gradle wrapper | 9.5.1 (`gradle/wrapper/gradle-wrapper.properties:4`) | 9.9.2 | S2 | https://services.gradle.org/versions/current ; https://docs.gradle.org/9.9.2/release-notes.html | Newest. Its fix "Transitive snapshot dependencies skip dependency verification" is a security fix; this build has no snapshots (A19) |
| AGP `com.android.tools.build:gradle` | 9.0.1 (`build.gradle:10`) | 9.5.2 | S2 | GM `com/android/tools/build/gradle` ; https://developer.android.com/build/releases/agp-9-5-0-release-notes | Newest stable (9.6.0-alpha05 excluded). 9.5.2 has no published notes (research §6.1); fallback F1 is 9.5.1 |
| Kotlin Gradle plugin | 2.3.21 (`build.gradle:14`) | 2.4.21 | S2 | MC `org/jetbrains/kotlin/kotlin-gradle-plugin` ; https://kotlinlang.org/docs/releases.html | 2.5.0 is RC only |
| `kotlin-stdlib` | 2.3.21 (`app/build.gradle:117`) | 2.4.21 | S2 | MC `org/jetbrains/kotlin/kotlin-stdlib` | Must equal the plugin |
| KSP plugin | 2.3.6 (`build.gradle:17`) | 2.3.14 | S2 | MC `com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin` ; https://github.com/google/ksp/releases/tag/2.3.14 | Newest. 2.3.13 is "Defect in release" (issue 3286) and is never used |
| Hilt Gradle plugin | 2.59.2 (`build.gradle:21`) | 2.60.1 | S2 | MC `com/google/dagger/hilt-android-gradle-plugin` ; https://github.com/google/dagger/releases/tag/dagger-2.60.1 | Newest |
| `hilt-android`, `hilt-compiler` (ksp, kspTest), `hilt-android-testing` | 2.59.2 (`app/build.gradle:182`, `:183`, `:187`, `:186`) | 2.60.1 | S2 | MC `com/google/dagger/...` | All five Hilt coordinates identical (U-060 IC-1) |
| `androidx.hilt:hilt-work`, `androidx.hilt:hilt-compiler` | 1.2.0 (`app/build.gradle:184`, `:185`) | 1.4.0 | S2 | GM `androidx/hilt/hilt-work` ; https://developer.android.com/jetpack/androidx/releases/hilt | Newest; needs minSdk 23 (C14), met by S1 |
| JaCoCo | 0.8.14 (`app/build.gradle:212`) | 0.8.16 | S2 | MC `org/jacoco/org.jacoco.core` ; https://www.jacoco.org/jacoco/trunk/doc/changes.html | 0.8.17 exists only as a snapshot |
| SDK Build Tools | none declared (AGP default); CI installs 36.0.0 (`.github/workflows/ci.yml:30`) | 36.0.0 (AGP 9.5 default) | S2 | https://developer.android.com/build/releases/agp-9-5-0-release-notes ; https://dl.google.com/android/repository/repository2-3.xml | 37.0.0 is the newest stable package. AGP 9.5's "minimum and default is 36.0.0" (research C4) is what REQ-015 Description asks for ("the SDK Build Tools version that the chosen AGP requires"). 37.0.0 would add a `buildToolsVersion` line and a download, and no requirement needs it |
| JDK (build) | 25 (`gradle/gradle-daemon-jvm.properties:2`) | 25, unchanged | — | https://api.adoptium.net/v3/info/available_releases | REQ-015 "JDK 25 stays"; the epic forbids moving off it. 27 is a non-LTS feature release |
| `actions/checkout` | v4 (`.github/workflows/ci.yml:17`) | v7 (7.0.1) | S2 | https://github.com/actions/checkout/releases | Newest. The page shows no year on the release date; 2026 inferred |
| `actions/setup-java` | v4 (`.github/workflows/ci.yml:22`) | v6 (6.3.0) | S2 | https://github.com/actions/setup-java/releases | Newest. 6.3.0 "no longer registers the Java problem matcher by default", which does not matter for this workflow |
| `actions/cache` | v4 (`.github/workflows/ci.yml:33`, `:41`) | v6 (6.1.0) | S2 | https://github.com/actions/cache/releases | Newest. The major-tag pin style of today is kept |
| `junit:junit` | 4.13.2 (`app/build.gradle:159`) | 4.13.2, unchanged | S2 | MC `junit/junit` | Current (REQ-016 AC-3 finding: maintained line has no newer release) |
| Robolectric | 4.15.1 (`app/build.gradle:160`) | 4.18.1 | S2 | MC `org/robolectric/robolectric` ; https://robolectric.org/compatibility_table/ | Newest; SDK range 24–37 |
| Mockito `mockito-core` | 5.18.0 (`app/build.gradle:163`) | 5.24.0 | S2 | MC `org/mockito/mockito-core` | Newest (ByteBuddy 1.17.7) |
| Truth | 1.4.4 (`app/build.gradle:161`) | 1.4.5 | S2 | MC `com/google/truth/truth` | Not named in REQ-015's table. Included as test tooling because the product owner's goal is "the latest of everything it uses" (REQ-015 Context) and no constraint argues against it |
| GreenMail `greenmail-junit4` | 2.1.3 (`app/build.gradle:173`) | 2.1.15 | S2 | MC `com/icegreen/greenmail-junit4` | Newest |
| auto-service | 1.0 (`app/build.gradle:166`) | 1.1.1 | S2 | MC `com/google/auto/service/auto-service` | Newest |
| `kotlinx-coroutines-core`, `-android` | 1.7.3 (`app/build.gradle:118`, `:119`) | 1.11.0 | S4 | MC `org/jetbrains/kotlinx/...` ; https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.11.0 | Newest; built with Kotlin 2.2.20, usable with 2.4.21 (C21) |
| `kotlinx-coroutines-test` | 1.7.3 (`app/build.gradle:150`) | 1.11.0 | S4 | same | Moves with coroutines |
| `lifecycle-runtime-ktx`, `lifecycle-viewmodel-ktx` | 2.6.2 (`app/build.gradle:122`, `:123`) | 2.11.1 | S4 | GM `androidx/lifecycle/...` ; https://developer.android.com/jetpack/androidx/releases/lifecycle | Newer than REQ-016's 2.11.0 snapshot (research §1). 2.12.0-alpha06 excluded |
| `androidx.annotation:annotation` | 1.1.0 (`app/build.gradle:134`), 1.7.1 (`k9mail-vendored/build.gradle:114`) | 1.11.0 (both) | S4 | GM `androidx/annotation/annotation` | Newest |
| `androidx.preference:preference` | 1.1.0 (`app/build.gradle:135`) | 1.2.1 | S4 | GM `androidx/preference/preference` | Newest; library is "in maintenance mode" |
| `androidx.core:core-role` | 1.0.0-beta01 (`app/build.gradle:136`) | 1.1.0 | S4 | GM `androidx/core/core-role` | Removes the last pre-release (REQ-016 AC-2) |
| `androidx.security:security-crypto` | 1.1.0 (`app/build.gradle:147`) | 1.1.0, unchanged | S4 | GM `androidx/security/security-crypto` ; https://developer.android.com/jetpack/androidx/releases/security | Newest stable; all APIs deprecated (D11) |
| `androidx.work:work-runtime-ktx`, `work-testing` | 2.9.1 (`app/build.gradle:155`, `:156`) | 2.12.0 | S4 | GM `androidx/work/...` ; https://developer.android.com/jetpack/androidx/releases/work | Newest; needs minSdk 24 (C18) |
| minSdk | 21 (`app/build.gradle:24`) | 24 | S1 | CR-011, Record 4 | Floor set by the Thunderbird Android variants |
| compileSdk | 35 (`app/build.gradle:21`) | 37 | S3 | research §4.1 | 37.1 and 37.2 exist; REQ-017 names 37 |
| targetSdk | 35 (`app/build.gradle:25`) | 37 | S5 | Record 4, REQ-017 | Gated by D14 |
| `:k9mail-vendored` minSdk / compileSdk | 21 / 35 (`k9mail-vendored/build.gradle:54`, `:52`) | unchanged | — | D4b | The module is deleted in Part B |
| Robolectric default SDK | 29 (`robolectric.properties:8`) | 29, comment corrected | S3 | D6 | The reason is recorded under REQ-017 AC-13 |

On the classpath but not named in any requirement table, and **not overridden**:

| Library | Version | How it arrives | Why not overridden |
|---|---|---|---|
| `com.google.crypto.tink:tink-android` | 1.8.0 (newest 1.24.0) | `security-crypto` | Forcing Tink under a deprecated wrapper changes the cipher library beneath stored credentials, outside the design that owns them (D11) |
| `androidx.core:core` | 1.12.0 | WorkManager 2.12.0 (research §2.3) | Not declared; 1.19.1 would need compileSdk 37 and AGP 9.1.0. Part B decides when Thunderbird brings it |
| `org.conscrypt:conscrypt-openjdk-uber`, `org.bouncycastle:bcprov-jdk18on` | via Robolectric | test runtime | They follow Robolectric; the TLS tests read the live provider (U-063 E2) |
| ByteBuddy | 1.17.7 | Mockito 5.24.0 | Follows Mockito |

The four retained mail libraries (commons-io, apache-mime4j core and dom, jzlib, jutf7) are out of
scope (CR-010). **Open item for the migration design, not here:** Maven Central's newest jzlib is
1.1.3 (2013-10-04), while Thunderbird `main` pins 1.0.7 (research §1, finding 4). Epic SC-1 (Record
5, row 6: "yes") then asks for 1.1.3 at Part B planning.

**Alternatives rejected.**
- *Lowest compatible set* (U-060 AC-15). That was the right call for a repair. REQ-015 now asks for
  latest stable.
- *Target the 2026-11-12 snapshot tables.* The requirements say the tables are "a snapshot, not a
  target".
- *Override transitive versions (Tink, androidx.core) to "latest".* That changes code nobody
  declared, without a requirement or a design that owns it.

**Consequences.** Every row has one owning story, counted from the table:

| Story | Rows | Of which unchanged |
|---|---|---|
| S2 | 19 | JUnit |
| S4 | 8 | `security-crypto` |
| S3 | 2 | compileSdk, Robolectric default |
| S1 | 1 | — |
| S5 | 1 | — |

The JDK and vendored-module rows have no owner because they do not change.

JUnit belongs to S2 as test tooling. REQ-016 AC-3 traces JUnit's status to S4, so S4 records its
planning-date version and maintenance finding without re-owning the row.

### D1a — Rule for a version that moves between this design and the story

**Decision.**

1. Each story re-resolves only its own rows, from the same sources, on its planning date, and
   records date, version and URL (REQ-015 AC-1, REQ-016 AC-1).
2. **Same major and minor, newer patch:** take it. Re-check only that component's rows in the
   research §3.1 constraint table, and record.
3. **Newer minor or major:** take it if every constraint row it takes part in still holds by the
   vendor's own statement, re-fetched. Otherwise it becomes the D3 case. The story does not quietly
   keep this design's version.
4. Never a pre-release:
   - Kotlin 2.5.0-RC
   - AGP 9.6.0-alpha
   - Lifecycle 2.12.0-alpha
   - androidx.core 1.20.0-alpha
   - AndroidX Test 1.8.0-alpha
   - JaCoCo 0.8.17-SNAPSHOT
5. Never a release its vendor marks defective. KSP 2.3.13 is the precedent.
6. A later story does not re-bump a component owned by an earlier, finished story. If the component
   has moved since, it is recorded as a follow-up, which keeps each story's attribution clean. Epic
   SC-1 judges each component at its own story's planning date.

**Rejected.**
- *Freeze the design's versions.* That contradicts AC-1.
- *Re-bump everything in every story.* That destroys attribution (AD-6).

### D2 — Five stories, their order, and their increments

**Context.** The constraints set a partial order:

- WorkManager 2.12 needs minSdk 24 (C18).
- Robolectric 4.18 cannot emulate any SDK below 24, so it needs no test configured below 24; the
  five sub-24 tests must go first (C15).
- compileSdk 37 needs AGP 9.1.1 or newer (C5).
- Kotlin 2.4 needs Hilt 2.60 (C11).
- Hilt 2.60 needs AGP 9 (C10).
- AGP 9.5 needs Gradle 9.6.0 or newer (C3).
- targetSdk 37 needs the D14 measurement. If the gap is confirmed, it also needs the BUG-021 fix
  (Record 13, row 4) and an API 37 device (D16).

**Decision.**

```
U-066 (in progress, sprint-022) ──must close before──▶ BUG-022 story, S1
P-1, P-2 (change records) ────────must precede──────▶ S1
BUG-022 (own small story; Record 14 row 11) ──before──▶ S2
S1 minSdk 24 floor ─────────▶ S2 toolchain + test tooling ─────────▶ S3 compileSdk 37
                                                                       │
                       target-37 part of S5-I1 (scratch build, after S3, before S4 is planned)
                                                                       │
BUG-022 ──▶ BUG-021 fix (after BUG-022, before S4, or strictly serial with S4)
                                       │
                                       ▼
P-3 (change record) ──must precede──▶ S4 runtime libraries ◀────────────┘
                                       │
                                       ▼
                                     S5 targetSdk 37 + behaviour review + API 37 device run
                                       ▲
  BUG-021 landed (unless S5-I1 is "measured not affected") ──┤ before S5-I4
  API 37 device (PO-2), appends (PO-3) ──────────────────────┘
```

**Mandatory preconditions.** The orchestrator raises these change records. None is optional
(Record 14, row 9).

| ID | Change record | Covers | Must precede |
|---|---|---|---|
| P-1 | CNTR-MODERNIZATION-004 and DES-MODERNIZATION-005: the pre-API-24 content-trigger fallback | The argument and line list in "Integration Contracts" | S1 |
| P-2 | Requirements wording, part 1 | REQ-015 AC-13's "697 plus added minus deleted" against a start of 732; the AC-9 items on REQ-MODERNIZATION-001 and -006 (their literal minSdk 21); REQ-MODERNIZATION-001's literal "35" (AC-1 at line 47, AC-29 at line 134; REQ-017 Notes say it should carry a change record) | S1 |
| P-3 | Requirements wording, part 2 (may be the same record as P-2, if raised before S1) | REQ-016 AC-6 and epic success criterion 8, "others at or above 70%", against the 13 configured floors below 0.70 | S4 |

BUG-021 has been raised for the OTP backup gap. It covers both the WebOTP all-targets case and the
target-37 case (D14). If S5-I1 confirms the gap, BUG-021 is designed and built before S5-I4.

**Placement of BUG-022 and BUG-021.** Both change `service/BackupWorker.kt`.

- **BUG-022** is its own small story (Record 14, row 11).
  - It lands after U-066 closes, because U-066 AC-11(a) allows no other `app/src/main` change while
    it runs.
  - It lands before S2, because S2-I5's Kotlin upgrade already touches how the three call sites
    resolve (BUG-022 Suggested Approach, "Timing").
  - It may land before or after S1; the two are independent.
  - Once BUG-022 is done, its fix removes the warnings the old F-1 stop rule watched. The rule is
    therefore restated as BUG-022 gives it (D10), and applies in S2-I5, S4-C and S4-W.
- **BUG-021** lands after BUG-022 and before S4 (BUG-021 Suggested Approach §7).
  - The fix story can be built and unit-tested now, without an emulator. Only its device criterion
    (AC-12), the confirmation of the bound, and the targetSdk 37 change wait for the measurement.
  - The orchestrator may therefore schedule the BUG-021 fix before the measurement, as its own story
    after BUG-022.
  - If the measurement waits, the target-37 part of S5-I1 runs after S3, on the scratch target-37
    build, before S4 is planned.
  - If it cannot run then (PO-2 or PO-3 not yet granted) and BUG-021 has not been scheduled, S4 and
    BUG-021 run strictly one after the other, with BUG-021 rebased onto S4's WorkManager and
    coroutines changes.
  - S5-I4 waits for BUG-021 either way, if the gap is confirmed.

#### S1 — minSdk 24 floor (REQ-015 AC-5 to AC-10; gates AC-11 to AC-14; first part of AC-17)

Preconditions: U-066 closed; change records P-1 and P-2 exist.

Runs on today's known-good toolchain (Gradle 9.5.1, AGP 9.0.1, Robolectric 4.15.1). Robolectric
4.15.1 runs SDK 21 to 35 (C15), so the retargeted tests are green before and after the floor moves.

| Increment | Content | Must hold after it |
|---|---|---|
| S1-I0 | Baseline evidence, no change. Captures: <br>• the four gates; <br>• the authoritative count; <br>• JUnit XML totals (scratch init script, as U-060 Step 0.5); <br>• JaCoCo per-package line ratios and class inventory (U-060 AC-9 method); <br>• merged manifests (debug, release); <br>• packaged manifests (`aapt2 dump xmltree`, U-060 C-1); <br>• R8 kept-name set (U-060 C-2); <br>• `:app:lintDebug` rule-ID counts and baseline entry count (38); <br>• `.so` count; <br>• `javap` majors | Nothing changed |
| S1-I1 | `reports.junitXml.required = true` in `build.gradle:30` (D9) | Gates green; XML totals equal I0 |
| S1-I2 | New `app/src/test/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiverTest.java`: <br>• integration on: `scheduleImmediate()` called once; <br>• integration off: never called, the "ignored" branch. <br>Uses the protected `getScheduler` seam (`BackupBroadcastReceiver.java:67-68`) and real `Preferences` under Robolectric (D8) | `receiver` coverage above 50% with headroom; count +2 |
| S1-I3 | Retarget the 5 sub-24 tests (D4, table T). The rewritten `SmsReceiverTest` comment contains neither "LOLLIPOP" nor `sdk = [2x`, so REQ-015 AC-7's grep stays at zero | Green on 4.15.1; count unchanged |
| S1-I4 | `minSdkVersion 24` (`app/build.gradle:24`). Comments `:153` and `:160` corrected. The two manifest permissions whose `maxSdkVersion` is below 24 are removed (D4a) | Merged-manifest diff shows only `minSdkVersion="24"` and the two removals |
| S1-I5 | Remove the 7 dead sites (D4, table S); update KDoc `WorkManagerScheduler.kt:59`, `:207-210` | AC-6 grep passes; coverage floors hold (scheduler measured, D8) |
| S1-I6 | Prune `app/lint-baseline.xml` by the D17 method: regenerate on a scratch copy, diff, and keep only removals | Entry count before and after, with each removed entry (AC-8); `app/lint.xml` untouched |
| S1-I7 | Documentation: <br>• `CHANGES` entry (AC-10; it also replaces the stale "Android minSdk now 14" line); <br>• `sdlc/artifacts/engagement/code-location.md` minSdk row; <br>• `.github/ISSUE_TEMPLATE/500_bug_report.yml` device list (CR-011 layer 8). <br>The AC-9 evidence is change record P-2, raised before S1 | AC-9 and AC-10 evidence |
| S1-I8 | Launch smoke on API 24 (if granted, D16) and API 36 (`Pixel_9_Pro`) | No `FATAL EXCEPTION`; the full AC-17 run is in S2 |

`gradle/verification-metadata.xml` must not change in S1: no dependency changes. A `git diff` that
is not empty is a stop.

#### S2 — Build toolchain and test tooling to latest stable (REQ-015 AC-1 to AC-4, AC-11 to AC-18)

Preconditions: BUG-022 done (Record 14, row 11). Its cancellation tests are the stop rule in S2-I5.

Every increment changes one component. Each is committed with its own verification entries (D5)
and all gates green.

| Increment | Change | Order rationale |
|---|---|---|
| S2-I0 | Baseline as S1-I0. AC-1 re-resolution record. AC-2 and AC-3 support reading (D3) | — |
| S2-I1 | Gradle 9.9.2: `./gradlew wrapper --gradle-version 9.9.2 --distribution-type bin --gradle-distribution-sha256-sum <official sha>`. Then re-apply the `gradlew` LOCAL ADDITION block (`gradlew:117-127`, U-060 capability regression) and check that `gradlew.bat` changes only by the generator | AGP 9.5 needs Gradle 9.6.0 or newer (C3); Gradle 9.9 is tested with AGP 9.0 (C2), so 9.0.1 keeps building |
| S2-I2 | KSP 2.3.14 (on Kotlin 2.3.21) | KSP is independent of the Kotlin version since 2.3.0 (C9). An older KSP under a newer AGP is the riskier pairing (2.3.10 fixed R-class resolution with AGP 9 built-in Kotlin). **Pre-specified reorder:** if 2.3.14 cannot run on KGP 2.3.21, the KSP bump moves into S2-I5, recorded, not a fallback |
| S2-I3 | AGP 9.5.2. Check: JaCoCo class roots after `clean` (`app/build.gradle:217-221`, U-060 B5 stale-directory lesson); C-1 manifests; C-2 R8 keep set; `receiver` coverage (AGP unit-test manifest) | Needs Gradle 9.6 or newer (I1). Must precede compileSdk 37 (C5) |
| S2-I4 | Hilt 2.60.1, all five coordinates. KSP generated-file list diffed against I0 (U-060 B1 method) | Hilt 2.60 is built with Kotlin 2.3.21 (C11), so it runs on today's Kotlin. That is what **separates** Kotlin 2.4 from Hilt 2.60: the coupling goes one way only (Kotlin 2.4 needs Hilt 2.60, not the reverse), so REQ-015's "if they cannot be separated" clause does not apply |
| S2-I5 | Kotlin 2.4.21 (plugin and stdlib). Record Kotlin `w:` lines against I4 (debt). **F-1 stop rule (D10):** the cancellation tests added by BUG-022 pass unmodified, and no Kotlin warning names `coroutineContext` in either worker file; otherwise stop and report | Needs Hilt 2.60 (I4) |
| S2-I6 | androidx.hilt 1.4.0 | minSdk 23 met (S1). Separate from I4 so a WorkManager-factory regression points at it |
| S2-I7 | Robolectric 4.18.1, plus: <br>• the D7 JVM-flag check; <br>• the `@LooperMode(LEGACY)` / `@ConscryptMode(OFF)` check (A14); <br>• a re-run of the TLS characterisation classes with their provider probes; <br>• U-066's exact-list tests unchanged (CR-014 Amendment 1 handling, "In-flight work") | Needs S1 (no test below SDK 24) |
| S2-I8 to S2-I11 | Mockito 5.24.0; Truth 1.4.5; GreenMail 2.1.15; auto-service 1.1.1, one increment each | Low risk; separate for attribution |
| S2-I12 | JaCoCo 0.8.16, with the class inventory compared to I11 | Last among build changes, so a coverage delta from JaCoCo itself is isolated |
| S2-I13 | CI workflow: action versions (D1), comments, runner image check (A18). Build Tools stays 36.0.0 (AC-16 records AGP 9.5's minimum 36.0.0) | — |
| S2-I14 | Documentation: <br>• `code-location.md` tech-stack rows; <br>• `app/build.gradle` and `build.gradle` version comments. <br>`.claude/CLAUDE.md` changes only if a gate text changes, and then through the Engine Librarian (U-060 AC-6 precedent) | — |
| S2-I15 | Device run AC-17 (API 24 and a higher level, D16). First CI run AC-18, if the product owner permits a pull request (open item PO-4) | — |

#### S3 — compileSdk 37 (REQ-017 AC-2 compileSdk half, AC-13, AC-14, AC-15, AC-16 for this raise)

| Increment | Change |
|---|---|
| S3-I0 | Baseline, including lint rule-ID counts at compileSdk 35 on AGP 9.5.2 |
| S3-I1 | `compileSdkVersion 37`. Platform `android-37.0` revision 2 is installed (A8). No targetSdk change |
| S3-I2 | Lint at compileSdk 37 and the baseline refresh under REQ-017 AC-14 option (a) (D17) |
| S3-I3 | `robolectric.properties` comment corrected; AC-13 record (D6); the one-off SDK 37 probe run on a scratch branch, not committed (D6) |
| S3-I4 | CI installs `platforms;android-37.0` |

Expected manifest difference: `android:compileSdkVersion` and `compileSdkVersionCodename` in the
packaged manifest. Nothing else.

**Pre-specified move (N-6).** S3 needs only AGP 9.1.1 or newer. Any S2 increment after S2-I3 may
fail because compileSdk 35 is too low: an AAR-metadata check, a missing platform API, or any other
failure whose cause is the compileSdk level (for example `androidx.test:monitor`, A21). Then S3-I0,
S3-I1, S3-I2 and S3-I4 run immediately after S2-I3, and the remaining S2 increments follow.

S3-I3 (the Robolectric 37 record and probe) stays after S2-I7, because it needs Robolectric 4.18.1.
The move is recorded, not treated as a fallback.

#### S4 — Runtime libraries (REQ-016 AC-1 to AC-13)

Increments run in the order R, A, P, C, L0, L, W (D10):

| Increment | Change |
|---|---|
| R | core-role 1.1.0 |
| A | annotation 1.11.0, in `:app` and `:k9mail-vendored` |
| P | preference 1.2.1 |
| C | coroutines core, android and test 1.11.0 |
| L0 | New test `MainActivityFlowCollectionStopTest` on Lifecycle 2.6.2 (no version change) |
| L | Lifecycle 2.11.1 |
| W | WorkManager and work-testing 2.12.0 |

Each increment records three things: the merged-manifest diff against the previous increment, the
resolved `debugRuntimeClasspath` and `releaseRuntimeClasspath` diff (every transitive version change
listed, DES-013 D4a style), and the gates. Increments C and W also carry F-1's stop rule (D10).

Preconditions: change record P-3; BUG-022 done; BUG-021 either landed or explicitly scheduled after S4 under D2's strict-serial rule.

**REQ-016 AC-10 evidence, decided.** A new Robolectric test,
`app/src/test/java/com/zegoggles/smssync/activity/MainActivityFlowCollectionStopTest.kt`, is added
as increment S4-L0 before L.

It sits in package `com.zegoggles.smssync.activity`, so it can verify the package-private callbacks
(`MainActivity.java:312`, `:318`, `:329`). The setup:

- it mocks `MainActivity` with Mockito 5 (inline mock maker);
- it stubs `getLifecycle()` to a `LifecycleRegistry` the test owns;
- it builds `MainViewModel` as `MainViewModelTest:55` does, over `FlowSyncStateRepository`;
- it calls `MainActivityFlowHelper.startCollection` (`MainActivityFlowHelper.kt:20`);
- it idles the main looper after each step.

It asserts that:
1. at STARTED, an emitted event reaches `onSyncEvent`;
2. after ON_STOP (CREATED), a new event does not;
3. after STARTED again, a new event does;
4. after DESTROYED, the registry's `coroutineScope` job is cancelled.

Events (`MainViewModel.events`, a `SharedFlow` with replay 0, `MainViewModel.kt:67`) are used, not
the state flow, which replays.

If `MainActivity` cannot be mocked, the same test runs through `OAuth2WebAuthFlowHelper` in package
`com.zegoggles.smssync.activity.auth`, and the story records why.

A device check was considered and not chosen as the evidence. Stopping and collecting cannot be
observed from `adb` text without adding logging. The test is deterministic, and it is counted in
the story's test-count record.

The story ends with the device checks AC-9, AC-11, AC-12 and AC-13 on `Pixel_9_Pro` (D16).

#### S5 — targetSdk 37, behaviour-change review, API 37 device validation (REQ-017 AC-1, AC-2 complete, AC-3 to AC-12, AC-15, AC-16)

Preconditions:
- an API 37 device (PO-2);
- the appends from that device (PO-3);
- the D14 measurement (S5-I1);
- if the measurement confirms the gap, BUG-021 designed and built before S5-I4 (Record 13, row 4).

| Increment | Content |
|---|---|
| S5-I0 | AC-1 record (D12) |
| S5-I1 | The Android 17 measurement (D14, its single owner): Run A with counting control M0b and positive control M1a; Run T; Run R. Outcomes per D14. Anything but "measured not affected" means S5-I4 waits for BUG-021 |
| S5-I2 | Behaviour-change table finalised from D13 with the measured results |
| S5-I3 | Fixes for rows dispositioned "applicable and fixed". None is expected from code reading; any found on the device co-land with I4 (AC-5) |
| S5-I4 | `targetSdkVersion 37`, one commit, including every fix from I3 |
| S5-I5 | Device validation AC-6 to AC-12 on the API 37 device, plus a regression run on `Pixel_9_Pro` |
| S5-I6 | Release-manifest diff (AC-16); lint at target 37 (AC-14) |

**Why five, and not fewer.**
1. *S1 folded into S2.* That makes a U-060-sized change and also mixes behaviour-affecting code
   deletions with tool changes. U-060's first explanation of a coverage shift was wrong for exactly
   that kind of reason. S1 is also a prerequisite of S2-I7 and can be proven on a known-good
   toolchain.
2. *S3 folded into S5.* compileSdk 37 would then wait on the OTP measurement and fix, and on device permission,
   though it changes no runtime behaviour and is a Part B prerequisite.
3. *S4 folded into S2.* That joins two requirements with different device checks and doubles the
   blast radius of one story.

**Why five, and not more.** One story per library family would repeat the baseline, the
verification pass and, above all, the device runs on the owner's emulators. Each device run needs
permission and the owner's configured emulator. Increments give the same attribution at a fraction
of the cost.

**Parallelism.** None on code: every story edits `app/build.gradle` and
`gradle/verification-metadata.xml`, so they run strictly in series. Four things can run in
parallel:

- the product-owner questions PO-2 to PO-4, which can be asked now;
- the change records P-1 to P-3;
- the creation of the API 37 AVD;
- the target-35 part of S5-I1's measurement (the WebOTP all-targets case), which needs no code.

**Consequences.** Five stories, roughly 45 increments. The complexity, in the U-060 sense, is:

| Story | Complexity |
|---|---|
| S1 | medium |
| S2 | high |
| S3 | low |
| S4 | medium |
| S5 | high (device-bound) |

### D3 — Procedure for a component outside a vendor's documented compatibility range (REQ-015 AC-3, AC-4)

**Context.** Research §3.2 point 2 finds that the newest set is outside Kotlin's fully supported
range. KGP 2.4.21 fully supports Gradle up to 9.7.0 and AGP up to 9.3.1 (C7). The research then
proposes Gradle 9.7.0 + AGP 9.3.1 + KGP 2.4.21 as "the newest triple that sits inside every
published range". **That is not correct on the evidence in this repository.** The Gradle 9.7.x
compatibility page, as fetched for U-060 on 2026-11-11, lists Kotlin only "through 2.4.20-Beta1"
and AGP "9.0 through 9.4.0-alpha03" (`U-060/implementation-log.md` §1d). So 2.4.21 sits beyond what
that page names. The conclusion is the same as U-060's SC-5: **no Gradle/AGP/KGP triple at stable
versions is inside all three vendors' ranges.**

This design's reading is the supported one. Where research §3.2 point 2 says otherwise, read it with
two caveats: U-060's log does not show the fetch date of the Gradle 9.7 page, and the planner's
sources were fetched on 2026-11-11.

**Decision.**

1. **Try first:** the D1 set. Record the support reading verbatim with URLs (AC-2). Starting
   citations are research C1 to C21; the story re-fetches each.
2. **Reading applied (AC-3, the SC-5 method).**
   - *Primary, binding:* the Gradle compatibility page of the chosen Gradle version, and the AGP
     release notes (minimum Gradle, minimum JDK, Build Tools, maximum API).
   - *Secondary, checks:* the Kotlin Gradle plugin table, the KSP and Dagger release notes, and the
     Robolectric compatibility table.

   The residual gaps, each with its detection method:

   | Gap | What the sources say | Detection |
   |---|---|---|
   | G1 | The Gradle 9.9 page tests AGP "9.0 through 9.5.0-alpha10" (C2); AGP 9.5.2 is beyond that pre-release bound | Build plus `--warning-mode all`; any AGP-originated error |
   | G2 | KGP 2.4.21 is fully supported only up to Gradle 9.7.0 and AGP 9.3.1 (C7) | KGP-originated warnings (debt) or errors (trigger) |
   | G3 | KSP 2.3.14 is tested against AGP base 9.3.0-alpha01 (C9) | KSP tasks pass, and the KSP generated-file list equals the S2-I0 list (U-060: 49 files plus the four Hilt LazyMapKey additions) |
   | G4 | The Hilt Gradle plugin 2.60.1 has no statement for AGP 9.5 or Gradle 9.9 (§3.2 point 4) | `hiltJavaCompileDebug`, the ASM transform, Hilt tests, R8 C-2 |
   | G5 | AGP 9.5.2 has no release notes (§6.1) | Build; F1 |
   | G6 | Robolectric 4.18.1 on JDK 25 without flags is not re-tested (§6.7) | D7 |

3. **Evidence that decides.** Success requires all of these:
   - the four gates green;
   - no error-class item in `--warning-mode all`;
   - a complete KSP file list;
   - a JaCoCo class inventory at or above the baseline for every package;
   - C-1 manifests equal and the C-2 R8 keep set equal.

   A warning is debt, recorded, never a trigger. A **trigger** is an error raised from one of these
   components, quoted verbatim:
   - KGP (`org.jetbrains.kotlin.gradle.*`)
   - KSP (`com.google.devtools.ksp.*`)
   - the Hilt plugin (`dagger.hilt.android.plugin.*`)
   - AGP (`com.android.build.*`)

   **javac `-Werror` rule (applies to D3, D10, D12).** `:app` compiles Java with
   `-Werror -Xlint:unchecked` (`app/build.gradle:203`). The comment at `:192-199` records that a
   past SDK raise broke the build this way, and removal warnings are on by default. So any javac
   warning that a new toolchain, library or SDK level produces fails `:app`. The rule:
   - record the warning verbatim;
   - fix it at the call site;
   - never remove `-Werror`, and never add an `-Xlint:-...` suppression.

   Such a warning is a defect to fix in the increment. It is not a fallback trigger, unless the fix
   is impossible without a version change.
4. **Documented fallback set.** These are pre-specified. Change one component at a time, and re-run
   that increment in full, including D5.

   | Fallback | When | Change | Why it is valid |
   |---|---|---|---|
   | F1 | AGP-originated error, or a 9.5.2-only regression | AGP 9.5.2 → 9.5.1 | 9.5.1 has notes; Thunderbird `main` builds on it (research §5) |
   | F2 | KGP- or KSP-originated error that names a Gradle or AGP API, version or behaviour | Gradle 9.9.2 → 9.7.0 and AGP → 9.3.1 | Inside Kotlin's full range; AGP 9.3 needs Gradle 9.5.0 or newer and supports API 37 (agp-9-3-0 notes, research §3.2); Hilt needs AGP 9 (C10). Checksum from services.gradle.org; the Gradle 9.7 compatibility page is re-read. **Part B consequence:** AGP 9.3.1 is below Thunderbird `main`'s 9.5.1. If the fork is built in place, one build cannot hold both. Surface it to the product owner, as F4 is |
   | F3 | KSP-originated error | KSP 2.3.14 → 2.3.12 | Thunderbird tag 25.0 uses it; 2.3.12 sets min AGP 8.12. Never 2.3.13 |
   | F4 | Hilt 2.60.1 or KSP rejects Kotlin 2.4 metadata | Kotlin 2.4.21 → 2.3.21, Hilt 2.60.1 kept | **Part B consequence:** Thunderbird `main` uses Kotlin 2.4.21, so the migration's readiness is affected. Surface it to the product owner, not only for acknowledgement |
   | F5 | Robolectric 4.18.1 cannot run the suite on JDK 25 even with the documented flags | Robolectric 4.17 | Supports SDK 23 to 37 (C15) |

   Outside the set, and never taken: a JDK change (epic Out of Scope), any version below today's,
   any pre-release.
5. **Who decides.**
   - The Solution Architect pre-specifies F1 to F5 here and in the story plan.
   - The Developer applies a fallback when its trigger is met, records the failing error verbatim
     and the version fallen back to (AC-4).
   - The orchestrator flags it to the product owner for acknowledgement (AC-4). The acknowledgement
     does not block continuing, because a pre-specified fallback is a routine choice under Record
     12. The story cannot close until the flag is recorded.

   A failure that F1 to F5 do not cover stops the story. The Solution Architect amends the plan. If
   the amendment would go below today's version or touch the JDK, it is the product owner's
   decision.

**Rejected.**
- *Run on F2 from the start, to sit inside Kotlin's range.* AC-1 asks for latest stable, and F2 is
  still outside the Gradle page's literal listing.
- *Treat warnings as triggers.* Vendors say explicitly that newer Gradle and AGP "might encounter
  deprecation warnings" (C7), so every bump would fall back for no defect.

**Consequences.** The reading and the gaps G1 to G6 become the AC-3 record. A future upgrade can
then tell an expected warning from a regression.

### D4 — minSdk 24: build, manifest, dead branches, tests

**Context.** REQ-015 AC-5 to AC-8; CR-011 Impact Analysis.

**Decision, build.** `app/build.gradle:24` becomes `minSdkVersion 24`. These comments are
corrected:

- `app/build.gradle:153`: "2.9.1 is compatible with compileSdk 35 + minSdk 21";
- `app/build.gradle:160`: "4.16+ drops SDK 21/22".

The merged manifest is checked for `android:minSdkVersion="24"` (AC-5) in
`app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml`.

**Decision, dead branches (table S).** Seven sites, each with the code that remains:

| # | Site | After |
|---|---|---|
| S-1 | `activity/Dialogs.java:170-174`, condition `... && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH` | `if (scrollPosition > 0 && ABOUT_HTML.equals(url)) { view.setScrollY(...); }`; the `android.os.Build` import (`:25`) goes, since it has no other use |
| S-2 | `calendar/CalendarAccessorPost40.java:23` `@TargetApi(ICE_CREAM_SANDWICH)` | Annotation deleted; imports `android.annotation.TargetApi` (`:3`) and `android.os.Build` (`:10`) go. The class keeps its name: renaming is a refactor without requirement |
| S-3 | `compat/SmsReceiver.java:31` `@TargetApi(KITKAT)` | Annotation and its import (`:3`) deleted |
| S-4 | `compat/SmsReceiver.java:46-55`: `if (>= Q) {role} else if (>= KITKAT) {getDefaultSmsPackage} else {return false}` | `if (SDK_INT >= Q) { role check } else { return context.getPackageName().equals(Sms.getDefaultSmsPackage(context)); }`. **The API 24–28 branch stays**: it is the pre-Q path of BUG-009 |
| S-5 | `activity/MainActivity.java:425` `@TargetApi(KITKAT)` on `startRestore()` | Annotation and the `android.annotation.TargetApi` import (`:19`) deleted. `Build` stays (`:242`, `:517`) |
| S-6 | `activity/SmsDefaultRoleHelper.kt:74-102`: outer `if (SDK_INT >= KITKAT) { ... } else { // Pre-KitKat ... startViewModelRestore() }` | The outer `if` and the pre-KitKat `else` go; the body stays as it is: `if (isSmsBackupDefaultSmsApp) start ... else if (SDK_INT >= Q) {...} else { legacy getDefaultSmsPackage path }`. **The inner pre-Q branch stays** (API 24–28, BUG-009) |
| S-7 | `scheduler/WorkManagerScheduler.kt:224-230`: `if (SDK_INT < N) { Log.d(...); return null }` | Deleted. The return type stays `ScheduledJob?`, because the encryption-degraded path (`:220-223`) still returns null. `import android.os.Build` (`:20`) goes, since it has no other use. KDoc `:59` and `:207-210` lose the "below API 24" text |

The 12 sites at level 25 or above stay:

- Q at `MainActivity.java:242`, `SmsDefaultRoleHelper.kt:77`, `:111`, `:135`, `SmsReceiver.java:47`,
  `BackupWorker.kt:533`, `RestoreWorker.kt:707`;
- TIRAMISU at `MainActivity.java:517`;
- O at `App.java:246`, `:350`;
- 26 at `ThemeActivity.java:20`, `:26`.

AC-6's grep after S1-I5 returns exactly these 12 lines.

**Decision, tests (table T).** Every test is retargeted. None is deleted, so the count is unchanged:

| Old test (line) | New name | New `@Config` | Assertion |
|---|---|---|---|
| `SmsReceiverTest.testOnReceivePreKitKat` (`:27`) | `testOnReceive_belowQ` | `sdk = Build.VERSION_CODES.N` | Unchanged (no throw) |
| `SmsReceiverTest.testOnReceiveKitKat` (`:32`) | `testOnReceive_q` | `sdk = Build.VERSION_CODES.Q` | Unchanged |
| `SmsReceiverTest.testIsSmsBackupDefaultSmsAppPreKitKat` (`:37`) | `testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage` | `N` | `isFalse()` (covers the pre-Q branch of S-4) |
| `SmsReceiverTest.testIsSmsBackupDefaultSmsAppKitKat` (`:42`) | `testIsSmsBackupDefaultSmsApp_q_usesRoleManager` | `Q` | `isFalse()` (role not held) |
| `WorkManagerSchedulerTest.inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` (`:378-384`) | `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` | `sdk = [24]` | Result non-null; unique work `CONTENT_TRIGGER_UNIQUE_NAME` enqueued with the SMS URI trigger (the boundary that now matters) |

The comment at `SmsReceiverTest.java:23-25` is rewritten. Before the change, the four SmsReceiver
tests were pairwise identical: all ran at 21 and all took the KITKAT branch. After it, they cover
both surviving branches.

AC-7's grep `grep -rn "LOLLIPOP\|sdk = \[\?2[0-3]" app/src/test` returns zero. The rename list
above is the "listed by name" record.

**D4a — Manifest permissions dead at minSdk 24.** Two declarations are removed, because their
`maxSdkVersion` is below 24 and so no supported device ever applies them:

- `app/src/main/AndroidManifest.xml:40-41`: `USE_CREDENTIALS`, `maxSdkVersion="22"`;
- `app/src/main/AndroidManifest.xml:49-50`: `WRITE_EXTERNAL_STORAGE`, `maxSdkVersion="18"`.

They are outside AC-6's grep (CR-011 called that out), and removing them is behaviour-neutral on
every installable device. Rejected: *keep them.* They are dead declarations of the same kind AC-6
removes from code, and the S1-I4 merged-manifest diff explains them.

**D4b — The vendored module stays as it is.** `k9mail-vendored/build.gradle:54` (minSdk 21) and the
vendored SDK guards stay unchanged:

- `DefaultTrustedSocketFactory.java:195`, `:244`, `:250`;
- `KeyChainKeyManager.java:35`, `:105`.

Reasons:

1. Runtime behaviour is identical. Those branches cannot run on a device of API 24 or above.
2. CR-009 moves `DefaultTrustedSocketFactory` into app-owned code at the migration, and the move is
   the natural place to drop them (CR-011 Suggested Approach, "Cross-record").
3. CR-013 states the SNI behaviour for "API level 24 and later", which already covers every level
   the app will run on (CR-013 lines 456-466).
4. Two existing tests in `DefaultTrustedSocketFactorySniTest` call `setHostnameViaReflection`
   directly (U-063 E1). Deleting the reflective path breaks them, for no runtime benefit.
5. U-066 forbids edits to `setSniHost` while it runs (U-066 AC-11c).

A library minSdk below the app's is accepted by the manifest merger. Rejected: *raise the vendored
minSdk to 24 and delete its guards now.* That touches characterised TLS code twice, here and in
CR-009's move.

**User-visible consequence.** Android 5.0, 5.1 and 6.0 (API 21 to 23) can no longer install
updates. All the owner's devices are Android 11 or newer (Record 5, row 8), so none is dropped. The
`CHANGES` entry states it (AC-10).

### D5 — Dependency verification for every story

**Context.** REQ-015 AC-11, REQ-016 AC-4, REQ-017 AC-15. The precedents are U-060 plan Step 6
(pristine restore, one writer pass, additions-only proof, enforced re-run) and DES-013 D4 (stale
entries kept, never hand-deleted, no wholesale regeneration). U-060 committed once. Here each
increment is committed, so each increment needs its own entries, or its commit does not build with
verification on.

**Decision, per increment that changes a dependency or plugin.**

1. Explore freely in writer mode while making the change work.
2. Before committing, discard the exploratory output with a single-path restore:
   `git restore --source=HEAD --worktree -- gradle/verification-metadata.xml`. HEAD is the previous
   increment's commit. Failed attempts, for example a fallback version tried and abandoned, leave no
   entries behind.
3. Run **one** writer pass over every configuration the gates and CI resolve. This is the U-060
   command, unchanged:
   `./gradlew --write-verification-metadata sha256 --continue clean help test lint jacocoTestReport assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`.
   The `--continue` covers the pre-existing `lintDebug` failure until S3 makes lint green.
4. Run the additions-only proof with the U-060 script, run with `python -I` from the scratchpad. It
   passes only if all three hold:
   - the `<configuration>` block is unchanged: `verify-metadata` true, `verify-signatures` false, the
     same two trusted-artifact regexes;
   - zero entries are removed or altered;
   - more than zero entries are added.
5. Review the added components. Each must belong to the increment's version change or its
   transitive closure, and anything else is flagged in the implementation log. Expected large groups:
   AGP's tooling and lint artifacts in S2-I3, Kotlin in S2-I5, Robolectric in S2-I7.
6. **Enforced re-run** without writer mode: `./gradlew :app:assembleDebug`, then the full gates.

**Stale entries are kept.** A checksum only constrains an artifact that resolves (DES-013 D4).
Deleting entries by hand is a hand edit (AC-11), and regenerating the file wholesale rewrites more
than 800 components unreviewably. The file grows. A whole-file regeneration review may be proposed
after Part B as its own decision.

**The arbiter.** The writer of Gradle 9.9.2 may re-serialise the file. The arbiter is the semantic
proof in step 4, not a text diff. The text diff is still reviewed.

**Outside Gradle verification, recorded and not changed:**

- Robolectric's `android-all` jars, which Robolectric resolves at test time. S1 adds SDK 24's jar,
  and S2-I7 may re-download them under new names. This has been the posture since U-005.
- The wrapper distribution, covered by `distributionSha256Sum`.
- Android SDK packages.
- GitHub Actions.

Every writer pass runs on the owner's machine and trusted network, as in U-060.

**Rejected.**
- *One writer pass per story.* Intermediate commits would then not build.
- *Writer mode left on in a commit.*
- *`verify-metadata` relaxation, or a `<trust>` entry for new groups.* AC-11 forbids both.

**Consequences.** S1 and S3 are expected to add no or few entries. S3 may add lint models for
compileSdk 37; if so, the same protocol applies. S2 and S4 add the bulk.

### D6 — Robolectric SDK levels for the test runs

**Context.** Today, `sdk=29` is the default (`robolectric.properties:8`):

- 10 classes pin `@Config(sdk = [29])` (or `{29}`);
- `MainActivityRestoreTest` pins Q five times;
- 5 tests sit below 24 (A3).

Robolectric 4.18.1 runs 24 to 37; SDK 36 and 37 need Java 21 or newer (C15, C16). REQ-017 AC-13
allows either moving to the new level, or recording the highest supported level and the reason the
tests stay below targetSdk.

**Decision.** Keep `sdk=29` as the default. Add explicit SDK 24 (`N`) and Q coverage only where a
branch depends on the level (table T). Rewrite the comment at `robolectric.properties:1-7`: the
level is deliberate, compileSdk and targetSdk are 37, Robolectric 4.18.1 supports 24 to 37, and the
reason is recorded in S3 under AC-13.

**Reason, with the cost of moving.**

1. The 732 expectations were authored and characterised at 29. U-063 found that the TLS provider
   under Robolectric is Conscrypt, and that "the Robolectric Android 10 jar contains no
   `javax/net` entries" (U-063 E2 and Implementation Notes). A move to 37 would not change the TLS
   provider, but it would change `SDK_INT` (every Q-or-later branch is still taken) and every shadow
   at once, across all classes. That is an unattributable change in a story that should change
   nothing.
2. Platform behaviour at 37 is proven on the device (REQ-017 Rationale), not in Robolectric.
3. Each new SDK level costs an `android-all` download outside verification, and per-sandbox memory
   with `maxParallelForks = 4`, `forkEvery = 100` (`build.gradle:27-28`).

**Measured instead of assumed.** S3-I3 runs a one-off probe on a scratch branch, with sdk set to 37
and not committed, and records pass and fail counts and the failing classes. This is also the
evidence Part B needs, because Thunderbird's build adds
`--add-opens=java.base/jdk.internal.access=ALL-UNNAMED` "when emulating SDK 37" (research §3.2
point 5). Moving the default level later is then a follow-up decision made on data.

**Rejected.**
- *Default to 37 now.* The cost above, with no requirement that needs it.
- *Default to 24 (the floor).* The same churn, and it moves coverage of the Q-or-later paths into
  explicit pins.

**Consequences.** The cost to the 732 tests is zero beyond the 5 retargets. The TLS tests' provider
expectations are unaffected by D6; they are affected only by S2-I7's Robolectric bump, see D7.

### D7 — JVM flags for Robolectric 4.18.1, and Mockito/ByteBuddy on JDK 25

**Context.** robolectric.org's getting-started page says that on Java 17 and later "the JVM requires
--add-opens flags" (C16). The project comment at `app/build.gradle:592-594` says no flags are needed,
but that was checked against 4.15.1 in U-060.

**Decision.**

1. At S2-I7, run the full suite with no flags.
2. Search the Gradle test output and the JUnit XML `system-err` for these, and record the counts:
   - `InaccessibleObjectException`
   - `does not "opens"`
   - `IllegalAccessError`
   - `module java.base`
   - the names of the three isolation guards in `DefaultTrustedSocketFactoryJdkProviderTest`
3. **Zero hits** means no flags. The comment is replaced with a dated statement for 4.18.1 at SDK 24
   and 29 on JDK 25.
4. **Any hit** means adding exactly the `--add-opens` that the error names, in
   `android.testOptions.unitTests.all { jvmArgs ... }`, quoting the error and the robolectric.org
   sentence. Never a blanket list copied from elsewhere.
5. Run the TLS classes in this increment with their environment probes (U-063 IC-11). If the
   Conscrypt version that Robolectric brings changes what the provider reports, the characterisation
   tests already derive their expectations from the live provider. A failure is investigated, and
   tests are not loosened (U-063 AC-23 defect rule). U-066's allow-list tests are exact by design and
   must pass unchanged.

**Mockito 5.24.0 / ByteBuddy 1.17.7.** ByteBuddy has supported Java 25 since 1.17.5, according to
the project comment at `app/build.gradle:163`; this is re-checked in the S2-I8 release notes.
Mockito's inline mock maker attaches a Java agent at run time. Under JEP 451 ("Prepare to Disallow
the Dynamic Loading of Agents") JDK 21 and later print a warning, and a future JDK will refuse.
Decision: record the warning as debt, and change nothing now. The documented remedy, running Mockito
as `-javaagent`, becomes a follow-up when a JDK in scope refuses dynamic loading.

**Rejected.** *Pre-emptively copying Thunderbird's flags.* Nobody here has measured that they are
needed, and they widen module access for every test JVM.

### D8 — JaCoCo floors and coverage headroom

**Context.** REQ-015 AC-14, REQ-016 AC-6, epic SC-8. There are 18 rules in the `violationRules` block (`app/build.gradle:340-589`):

| Package rule | Floor |
|---|---|
| `service*`, `mail*`, `auth*` | 0.70 |
| root `com.zegoggles.smssync` | 0.50 |
| `activity` | 0.10 |
| `activity.auth*` | 0.00 |
| `activity.events*` | 0.00 |
| `activity.fragments*` | 0.10 |
| `calendar*` | 0.70 |
| `compat*` | 0.20 |
| `contacts*` | 0.70 |
| `di*` | 0.03 |
| `preferences*` | 0.65 |
| `receiver*` | 0.50 |
| `scheduler*` | 0.60 |
| `tasks*` | 0.00 |
| `utils*` | 0.10 |
| `worker*` | 0.00 |

`receiver` sat at exactly 29/58 = 50.0% after U-060, with zero headroom. The cause: AGP 9's
unit-test manifest drops the app's own components (U-060 rework §1a).

**Decision.**

1. No `minimum`, `includes` or `excludes` changes, ever. A breach is fixed with tests or a
   manifest correction (AC-14).
2. **Headroom before risk.** S1-I2 adds `BackupBroadcastReceiverTest` before any toolchain change.
   `BackupBroadcastReceiver` has "0 of 11 lines covered" (sprint-017 retro). The test also covers
   CNTR-005's opt-in gate at unit level, which until now had device-only evidence for one branch.
3. Per increment: per-package covered and total lines and the class inventory, compared to the
   previous increment. After any AGP or KGP change, run `clean` first: U-060 found a stale Kotlin
   class directory that would have satisfied the guard on stale classes. The class-root guard
   (`app/build.gradle:217-229`) stays, and its paths are updated if AGP 9.5 moves them.
4. **Known thin margins.**
   - `scheduler`: S-7 deletes the three lines the SDK 23 test covered. The retargeted test covers
     lines already covered by INV-4(a). The net change is measured at S1-I5, with stop and add-tests
     if it falls below 0.60.
   - `compat`: S-4 deletes the uncovered `else { return false; }`, so the ratio rises.
5. **REQ-016 AC-6 and epic success criterion 8.** Both say "others at or above 70%". That
   contradicts the configured floors: 13 of the 18 rules sit between 0.00 and 0.65. This design does
   **not** reinterpret approved criteria. The wording goes to change record P-3, which must rule
   before S4 is planned (finding FD-VAL-002). Until it rules, the gate that runs is the configured
   one, and no floor is lowered or raised by this work.

**Rejected.** *Adding the test only if the floor breaks.* With zero headroom, any toolchain-caused
shift fails the gate in the middle of a story and forces the investigation at the worst moment.

### D9 — JUnit XML switched on

**Context.** `build.gradle:29-30` turns both reports off. QA has used a scratch init script for
totals in every toolchain story (U-060 plan Step 0.5, its QA, U-063 IC-4). REQ-015 AC-13 and
REQ-016 AC-5 need failures and skipped counts.

**Decision.** In S1-I1, set `reports.junitXml.required = true` at `build.gradle:30`. HTML stays off.
Totals then come from `app/build/test-results/testDebugUnitTest/TEST-*.xml`: the sums of `tests`,
`failures`, `errors` and `skipped`.

**Rejected.**
- *Keep the init script.* Every agent re-writes it, and evidence from different scripts is not
  comparable.
- *Turn HTML on.* It is larger and adds nothing for gates.

**Consequences.** There is a small set of XML files per run. CI could upload them later. The build
outputs are otherwise unchanged.

### D10 — Runtime libraries: order, code-touching changes, proof of no regression

**Context.** REQ-016. The release notes were read on 2026-11-21 (URLs in D1). The app's use of the
libraries was found by grepping imports in `app/src/main` and `app/src/test`.

**Decision, order R → A → P → C → L → W.** Lowest risk goes first. Coroutines precede WorkManager
because WorkManager 2.12.0 pulls coroutines 1.9.0 and Lifecycle 2.6.2 transitively (research §2.3).
Upgrading coroutines first stops the WorkManager increment from moving coroutines silently.

| Library | What the notes say that touches this app | App's use (evidence) | Proof of no regression |
|---|---|---|---|
| core-role 1.0.0-beta01 → 1.1.0 | Since 1.0.0-beta01 the only item is 1.1.0-alpha01 "Added role name for system gallery". 1.0.0 restates `RoleManagerCompat` | `RoleManagerCompat.ROLE_SMS` (`SmsReceiver.java:28`, `SmsDefaultRoleHelper.kt:26`) | `MainActivityRestoreTest` (5 tests at Q); AC-9 device role flow |
| annotation 1.1.0 / 1.7.1 → 1.11.0 | JVM jar, no SDK floor | Annotations only. The vendored module compiles at Java 8 source and target and must read the newer class files | Both modules compile; `javap` majors unchanged (61 app, 52 vendored, AC-15) |
| preference 1.1.0 → 1.2.1 | 1.1.1: depends on Fragment 1.2.4. 1.2.0: Kotlin nullability (source-breaking for Kotlin callers); `PreferenceFragmentCompat` "now looks for implementations of the `OnPreferenceStartFragmentCallback`" (parent fragments first). 1.2.1: depends on Activity 1.5.1. The library is in maintenance mode | Java callers only (no Kotlin file imports `androidx.preference`). `MainActivity` implements `OnPreferenceStartFragmentCallback` and `OnPreferenceStartScreenCallback` (`MainActivity.java:106-109`). No parent fragment implements them, so lookup still ends at the activity | `preferences*` tests (floor 0.65); AC-11 device run of every preference screen; classpath diff records the fragment and activity versions |
| coroutines 1.7.3 → 1.11.0 | 1.8.0: `runTest` default timeout 60 s; "On Android, ensure that `Dispatchers.Main != Dispatchers.Main.immediate`"; `SharedFlow.collect` registers as subscriber before its first suspension. 1.9.0 and 1.11.0 advanced deprecation levels in the test API. 1.11.0 deprecated `CoroutineDispatcher` as a context key and fixed `SharedFlow` simultaneous cancellation and an R8 `shareIn`/`stateIn` issue | Tests use only `runTest` (4 imports) and `UnconfinedTestDispatcher` (2). No `runBlockingTest`, `TestCoroutineDispatcher`, `TestCoroutineScope` or `setMain` | All `state` and `SyncStateRepository` tests (CNTR-006); `MainViewModelTest` (BUG-005); any test changed for an API change is listed with its replacement (AC-5) |
| Lifecycle 2.6.2 → 2.11.1 | 2.7.0: keep rules for `LifecycleObserver` removed. 2.8.0: "`lifecycle-runtime-ktx` is now empty, with all APIs being moved into `lifecycle-runtime`" (same for `-viewmodel-ktx`). 2.9.0: "`Lifecycle.DESTROYED` state is terminal". 2.10.0: minSdk 23 | `lifecycleScope`, `repeatOnLifecycle`, `viewModelScope`, `ViewModel`, `LifecycleOwner` imports only; no reflective `LifecycleObserver` | Keep the `-ktx` coordinates REQ-016 names; they still resolve and pull the base artifacts. `MainViewModelTest` no-leak tests (AC-10); R8 C-2 keep set unchanged |
| WorkManager 2.9.1 → 2.12.0 (with work-testing) | 2.10.0: "compiled with SDK 35", adds `Configuration.workerCoroutineContext`. 2.10.5: "Fix foreground service stopping when there are pending commands". 2.11.0: minSdk 23. 2.11.2: periodic work failing with an uncaught exception is rescheduled again. 2.12.0: minSdk 24; `Configuration.Builder` accepts a worker-execution listener. **Nothing stated** about the `SystemForegroundService` manifest entry, `InitializationProvider`, `ForegroundInfo` or content-URI triggers | `CoroutineWorker`, `setForeground(ForegroundInfo(..., FOREGROUND_SERVICE_TYPE_DATA_SYNC))` (`BackupWorker.kt:533-537`), `PeriodicWorkRequest` with `addContentUriTrigger` (`WorkManagerScheduler.kt:232-253`), `Configuration` and `HiltWorkerFactory` in `App.java`, manifest override (`AndroidManifest.xml:140-143`) and provider removal (`:246-249`) | AC-7: merged-manifest invariant: `SystemForegroundService` still `foregroundServiceType="dataSync"`, `InitializationProvider` still absent, every other difference justified. AC-8: `WorkManagerSchedulerTest` and the worker tests unmodified. AC-13: device scheduled run |

**F-1 (finding, outside these requirements).** The three cancellation checks
`coroutineContext.ensureActive()` (`BackupWorker.kt:331`, `RestoreWorker.kt:311`, `:399`) resolve
to the inherited, deprecated `CoroutineWorker.coroutineContext: CoroutineDispatcher`. U-060's
compiler warnings name exactly these lines (A7). A dispatcher context holds no `Job`, so the checks
never throw. The backup and restore loops therefore do not stop at those points when the work is
cancelled. `BackupWorkerTest.kt:27-31` and `:231` assert only WorkManager's state, so no test
catches this.

**The bound.** Every loop pass also calls `setProgress`:

- `BackupWorker.kt:361`, once per pass, and one pass of `backupCursors` handles **one message**,
  not a batch (BUG-022 Suggested Approach, its correction: `BackupCursors.next()` moves one row);
- `RestoreWorker.kt:321` and `:406`, once per item.

`setProgress` is a suspension point, so a cancelled worker should stop there. That bounds the
overrun to at most one more message (backup) or one more item (restore) after cancellation.

- *Verifiable in this repository:* the loop structure, and that `setProgress` is called on every
  pass.
- *Not verifiable here:* that WorkManager's `setProgress` suspension is cancellable (A20). The
  library source is not in the repository.

Decisions on F-1:

- **Not fixed in these stories.** It is BUG-022, fixed as its own small story after U-066 and before
  S2 (Record 14, row 11; D2 placement).
- **Stop rule, restated as BUG-022 gives it, without line numbers.** In S2-I5, S4-C and S4-W:
  - the cancellation tests added by BUG-022 pass unmodified; and
  - no Kotlin warning names `coroutineContext` in `BackupWorker.kt` or `RestoreWorker.kt`.

  Otherwise stop and report. The first part checks the behaviour; the second catches the old
  binding coming back. The three increments are those that can change the binding:
  - S2-I5: Kotlin 2.4;
  - S4-C: coroutines 1.11.0, which deprecates a `CoroutineDispatcher` as a context key;
  - S4-W: WorkManager 2.12.0.

**`-Werror`.** The D3 javac rule applies to every S4 increment.

**Database.** WorkManager keeps its own internal database. Whether 2.9.1 can open one that 2.12.0
has migrated is unknown (A16). Rollback is covered in D19.

**Rejected.** *WorkManager first, as REQ-016's table lists it.* It would move coroutines and
Lifecycle transitively inside the riskiest increment.

### D11 — `security-crypto`: what "latest stable" means here

**Context.** REQ-016 AC-3 asks for the maintenance-status finding, and its Notes say "do not widen
this requirement". The app's credential store (`preferences/EncryptedPrefsSecretStore.java:7-8`,
`di/SecretModule.kt`) uses `EncryptedSharedPreferences` and `MasterKey`, per DES-MODERNIZATION-004
§Cryptographic Construction.

**Finding.** 1.1.0 (2025-07-30) is the newest stable release and the version in the build. "Deprecated all APIs in favour of existing platform APIs
and direct use of Android Keystore" appears in the 1.1.0-alpha07 and 1.1.0-beta01 entries. The
1.1.0 entry itself only announces the release. The notes name no replacement class and no
migration, and do not say whether existing data stays readable.

**Decision.**
- `security-crypto` stays at 1.1.0. AC-3 records "unchanged" with the finding.
- `tink-android` 1.8.0 is not overridden (D1).
- In scope: AC-12's data-at-rest check across S4. Credentials written by the pre-upgrade build must
  read without re-entry after `adb install -r`.
- Ordinary deprecation shows only as a javac "Note", because `-Xlint:deprecation` is not enabled
  (`app/build.gradle:203`). Deprecation *for removal* warns by default and fails `:app` under
  `-Werror`, so the D3 javac rule applies.

**Recommendation for the product owner (PO-5), not designed in.** Raise a requirement to replace the
`EncryptedSharedPreferences` adapter with a Keystore-direct adapter behind the existing `SecretStore`
port.

CNTR-MODERNIZATION-003 calls a producer swap backward-compatible (`:133-136`), but the swap is more
than that clause suggests. The same contract mandates the library's own construction: stored keys
"MUST be encrypted with **AES-256-SIV**" (`:145`). A Keystore-direct producer therefore needs a
contract revision of that clause, as well as a one-time, rollback-safe re-encryption migration
(DES-004 §One-Time Migration is the pattern).

DES-004 foresaw this: "*Reconsider only* if Jetpack Security is later deprecated without a successor"
(`DES-MODERNIZATION-004-encrypted-credential-store-design.md:208-209`). Here the release notes point
to platform APIs and the Keystore rather than to a successor library.

Nothing breaks today, so it is not urgent, and the orchestrator puts it to the owner. It gates no
story here.

**Rejected.**
- *Replace it now.* Out of scope, and a data-at-rest migration would sit inside a version-bump story.
- *Force a newer Tink.* That changes the cipher implementation under stored ciphertext with no
  owning design.

### D12 — Platform level: compileSdk 37 first, targetSdk 37 last; SDK packages; AC-1 evidence

**Decision, split.** compileSdk 37 is S3, a build-only change with no runtime effect, which
satisfies every library's `minCompileSdk` and the Thunderbird prerequisite. targetSdk 37 is S5. This
also follows the co-landing rule (REQ-017 AC-5): no commit raises targetSdk while a known crash
condition remains unfixed.

**SDK packages.**

| Need | Package | On this machine | How obtained |
|---|---|---|---|
| Compile at 37 | `platforms;android-37.0` revision 2 (newest stable per research §4.2) | installed (A8) | Nothing to download |
| Build Tools | `build-tools;36.0.0` | installed | Nothing to download |
| CI | `platforms;android-37.0`, `build-tools;36.0.0` | runner | `sdkmanager` step in `ci.yml` (S3-I4) |
| API 37 device | `system-images;android-37.0;google_apis_playstore_ps16k;x86_64` | installed; its only AVD is `Pixel_10_Pro` | New AVD from the installed image: no image download. Needs the owner's permission (PO-2) |
| API 24 device | an API 24 x86_64 system image (about 1 GB) | **not installed** | Download through the official SDK Manager. `cmdline-tools` are not installed (A8), so that is Android Studio's SDK Manager, or installing the cmdline-tools package, which is itself a download. Needs the owner's permission (PO-2) |

**DSL.** Keep `compileSdkVersion 37` and `targetSdkVersion 37`, so the AC-2 grep works literally.
If AGP 9.5 forces the newer `compileSdk` form, or resolves 37 to a platform other than
`android-37.0`, the forced change is recorded with its error and the grep is adapted (A13).
compileSdk minor levels 37.1 and 37.2 are not used: REQ-017 names 37.

**`-Werror`.** The D3 javac rule applies to S3-I1 and S5-I4. A newly deprecated-for-removal platform
API at 37 is fixed at its call site, never suppressed.

**AC-1 evidence and the stale pages.** S5-I0 records these sources on developer.android.com, each
with the date consulted:

- **Qualifying:** "Android 17 is here", 16 June 2026
  (https://developer.android.com/blog/posts/android-17-is-here).
- **Qualifying:** the GSI release page, "Android 17 (stable release)"
  (https://developer.android.com/topic/generic-system-image/releases).
- **Recorded as stale and not used:** the versions index (https://developer.android.com/about/versions).
- **Recorded as stale and not used:** the SDK platform release notes, which stop at Android 16
  (https://developer.android.com/tools/releases/platforms).
- **Recorded as stale and not used:** the SDK setup page, which still says "Android Cinnamon Bun
  Preview" (https://developer.android.com/about/versions/17/setup-sdk).
- **Supporting, not developer.android.com:** the AOSP table (https://source.android.com/docs/setup/reference/build-numbers),
  "Android17, 17, API level 37".

Rule: AC-1 is met when a cited developer.android.com source page shows 37 as stable and that page
does not label it preview. The stale pages are recorded with what they say. If at S5 planning no
developer.android.com page shows 37 as stable, or a higher level has become stable, the story stops
and asks the product owner (AC-1). The locally installed platform is never cited as evidence (AC-1).

### D13 — Behaviour-change review for levels 36 and 37 (REQ-017 AC-3, AC-4)

**Decision.** S5 adopts this table, completed with device and measurement results. Each row has
exactly one disposition:

- NA: not applicable, with the reason;
- A-NC: applicable, no code change, verified on device;
- A-F: applicable and fixed;
- A-D: applicable and deferred, with the follow-up artifact ID.

The sources are the four research pages, §4.3 to §4.6, with their URLs. The pages were last
updated on:

| Page | Last updated |
|---|---|
| behavior-changes-16 | 2026-10-28 |
| 16 behavior-changes-all | 2026-10-28 |
| behavior-changes-17 | 2026-11-11 |
| 17 behavior-changes-all | 2026-11-09 |

The S5 story re-reads each page for rows added after those dates.

The Google Play target-API note (research §4.7) does not apply: the app is private and
unpublished.

| # | Change (page, anchor) | Applies? Code evidence | Disposition and action |
|---|---|---|---|
| 36-1 | Edge-to-edge opt-out removed (behavior-changes-16 `#edge-to-edge`) | App already draws edge-to-edge and applies insets: `WindowInsetsUtil.applyEdgeToEdgeInsets` (`MainActivity.java:138`, `WindowInsetsUtil.java:86-106`, BUG-014). No `windowOptOutEdgeToEdgeEnforcement` in resources (grep) | `MainActivity` and its preference screens: A-NC, AC-7 per screen |
| 36-1a | Edge-to-edge for `OAuth2WebAuthActivity` | No `setContentView`. It opens the browser with `ACTION_VIEW` and finishes (`OAuth2WebAuthActivity.java:24`, `:37`, `:47`), so it draws no content of its own | NA |
| 36-1b | Edge-to-edge for `RedirectReceiverActivity` | No `setContentView`. It reads the redirect and finishes (`RedirectReceiverActivity.java:22-45`) | NA |
| 36-1c | Edge-to-edge for `AccountManagerAuthActivity` | No `setContentView`. It shows framework dialogs and finishes (`AccountManagerAuthActivity.java:90-170`); a dialog window gets the platform's own inset handling | A-NC. Evidence: if reachable from the account settings on DV-2, a record that no dialog control is occluded; if not reachable, "not reached" with the reason, recorded as the evidence for this disposition |
| 36-1d | Edge-to-edge for `ComposeSmsActivity` | Empty class (`compat/ComposeSmsActivity.java`); it exists only for the default-SMS-app role declaration | NA |
| 36-2 | Predictive back on by default (`#predictive-back`) | No `onBackPressed` override, no `KEYCODE_BACK`. `onSupportNavigateUp` *calls* `onBackPressed()` (`MainActivity.java:196-197`), which dispatches to `OnBackPressedDispatcher`. The fragment back stack is used (`addToBackStack`, `MainActivity` `showFragment`; `OnBackStackChangedListener` `:109`) | A-NC: AC-7 back gesture and button per screen. If a screen fails to pop, A-F, co-landed |
| 36-3 | Elegant font APIs (`#elegant-text-height`) | No `elegantTextHeight` (grep) | NA |
| 36-4 | Fixed-rate work scheduling (`#schedule-at-fixed-rate`) | No `scheduleAtFixedRate` (grep) | NA |
| 36-5 | Adaptive layouts (`#adaptive-layouts`) | No `screenOrientation` or `resizeableActivity` in the manifest | NA |
| 36-6 | Health permissions; Bluetooth bond items; MediaStore lockdown; GPU syscall filtering; app-owned photos | None used | NA |
| 36-7 | Safer intents (`#safer-intents`, opt-in) | Exported components: `MainActivity`, `RedirectReceiverActivity`, `BackupBroadcastReceiver`, `SmsReceiver`, `MmsReceiver`, `ComposeSmsActivity`, `HeadlessSmsSendService` (`AndroidManifest.xml:99-226`). The app has not opted in | NA (opt-in). CNTR-005 checked by AC-12 anyway |
| 36-8 | Local network permission, opt-in at 36 | — | NA at 36; see 37-6 |
| 16-1 | JobScheduler quota (behavior-changes-all `#job-quota-opt`) | WorkManager runs on JobScheduler. Today every backup asks for a `dataSync` foreground service, and a start the system refuses (off screen, Android 12 and later) fails the run (BUG-029). From story U-075, automatic backups (incoming, regular, after a restart, the `BACKUP` broadcast) run as ordinary jobs under the job quota, and only work the user starts (manual backup, Skip, restore) asks for the foreground; a refused start continues without it (CR-017, Amendment 2) | A-NC: AC-11 forced run; REQ-016 AC-13 scheduled run. Affects any Android 16 device already, whatever the target |
| 16-1a | Abandoned empty jobs stop reason (`#abandoned-job-stop-reason`) | A new stop reason for jobs whose `JobParameters` were garbage-collected. The app holds no `JobParameters`; WorkManager owns the jobs | NA (informational; a WorkManager-internal concern) |
| 16-1b | Predictive back on 3-button navigation (`#three-button-predictive-back`) | Same code as 36-2 | A-NC: AC-7 records back with both gesture and 3-button navigation on DV-2 |
| 16-1c | Improved bond-loss handling (`#improved-bond-loss-handling`) | No Bluetooth use (grep `Bluetooth`: none in `app/src/main`) | NA |
| 16-2 | Ordered broadcast priority scope (`#ordered-broadcast-priority`) | No `android:priority` on the SMS receivers (`AndroidManifest.xml:146-153`, `:181-197`) | NA |
| 16-3 | 16 KB page-size compatibility mode | No native code: 0 `.so` in the debug and release APKs; no `jniLibs` | NA. A fitness check per story: `unzip -l <apk> \| grep -c '\.so$'` = 0. The installed API 37 image is a 16 KB (`ps16k`) image, so S5 also runs on 16 KB pages |
| 16-4 | Intent redirection hardening (`#intent-redirect-attacks`) | `RedirectReceiverActivity` reads `code`/`error` and finishes; it starts no activity from the received intent (`RedirectReceiverActivity.java:22-45`) | NA |
| 16-5 | ART internals, disruptive a11y announcements, themed icons, virtual-device and companion items | No reflection on ART internals; no `announceForAccessibility` (grep) | NA |
| 37-1 | Lock-free `MessageQueue` (`#lock-free-messagequeue`) | No reflection on `MessageQueue` in `app/src/main`. Robolectric runs on the JVM, not ART | NA |
| 37-2 | Static final fields unmodifiable (`#static-final-fields`) | No `setAccessible` or `getDeclaredField` in `app/src/main` or vendored main. U-063 tests replace a non-final static field on the JVM (U-063 E4) | NA |
| 37-3 | ECH enabled (`#ech-by-default`) | D15 | A-NC (expected), observed by AC-9 |
| 37-4 | Certificate transparency by default (`#ct-default`) | D15 | A-NC (expected), observed by AC-9 |
| 37-5 | OTP protection for standard SMS (`#sms-otp-protection`) | D14. Backup queries the SMS provider (`BackupQueryBuilder.java:103-116`); `SmsBroadcastReceiver` listens to `SMS_RECEIVED` (`AndroidManifest.xml:146-153`) | **Applicable.** A-F by BUG-021, landed before S5-I4, unless S5-I1's outcome for this row is 'measured not affected' (then NA, under the D14 stop rule). Every other outcome, including 'not run', means S5-I4 waits for BUG-021. |
| 37-6 | Local network permission required (`#local-network-protection-permission`) | IMAP to a host on the local network would need `ACCESS_LOCAL_NETWORK`, which is not declared | NA: the only mail server in use is Gmail, a public host (Record 5, row 1; orchestrator ruling PO-6). If the owner ever uses a mail server on the local network, the app must declare and request `ACCESS_LOCAL_NETWORK`; until then a connection to a LAN address fails at targetSdk 37 |
| 37-6a | Safer native dynamic code loading (`#safer-dcl`) | No native code (row 16-3) and no dynamic code loading: no `System.loadLibrary`, `DexClassLoader` or `loadLibrary` in `app/src/main` (grep) | NA |
| 37-7 | Activity security / BAL via `IntentSender` (`#activity-security`) | The notification `PendingIntent` is user-tapped (`BackupWorker.kt:519-521`). The role request is a foreground `startActivityForResult` | NA. AC-10 exercises the role flow |
| 37-8 | Restricted PII fields in CP2 data view (`#restrict-pii-fields-cp2-data-view`) | Projections: `GroupMembership.CONTACT_ID`, `RAW_CONTACT_ID`, `GROUP_ROW_ID` (`ContactAccessor.java:77-86`); `Email.DATA` (`PersonLookup.java`, `getPrimaryEmail`); `Groups._ID`, `TITLE`, `SUMMARY_COUNT` (`ContactAccessor.java:52-57`); `PhoneLookup._ID`, `DISPLAY_NAME`. No `ACCOUNT_NAME`, `ACCOUNT_TYPE` or `ACCOUNT_TYPE_AND_DATA_SET` | NA (CNTR-008 unaffected) |
| 37-9 | Strict SQL checks in CP2 without `READ_CONTACTS` (`#enforce-strict-sql-checks`) | Queries run with `READ_CONTACTS`; without it they already fail with `SecurityException` (documented `@throws`). Selections are plain `col = ?` | NA. AC-8 contacts grant and deny paths |
| 37-10 | Memory-limit widget; IME physical-keyboard a11y; hiding passwords from physical devices; `setContentCaptureEnabled`; background audio; large-screen opt-out removal; RFCOMM | None used (grep for `setContentCaptureEnabled`, audio APIs: none) | NA |
| 17-1 | App memory limits (`#app-memory-limits`) | Large MMS attachments are converted in memory | A-NC: watch `ApplicationExitInfo` reason `REASON_OTHER` "MemoryLimiter:AnonSwap" in logcat during AC-9 |
| 17-2 | SMS OTP for WebOTP format, all targets (`#sms-otp-all-apps`) | As 37-5, for any target, on Android 17 devices | **Applicable today**, whatever the target. NA if its own run is 'measured not affected'. Otherwise A-F by BUG-021 once it lands; until then A-D, BUG-021. |
| 17-3 | `usesCleartextTraffic` deprecation plan | App uses `networkSecurityConfig` (`AndroidManifest.xml:97`), not `usesCleartextTraffic` | NA |
| 17-4 | Implicit URI grants restricted from Android 18; per-app keystore limits; cross-profile loopback; IME rotation; touchpad; Bluetooth re-pairing | One Keystore master key; no `ACTION_SEND` with URIs | NA |
| AC4-a | Foreground-service types (BUG-016) | Override at `AndroidManifest.xml:140-143`; `FOREGROUND_SERVICE_DATA_SYNC` declared (`:81`); none of the 36 or 37 pages lists an FGS change (research §4.5 note) | A-NC: merged-manifest invariant (S4 AC-7, S5 AC-16); AC-9 starts the foreground worker |
| AC4-b | Runtime notification permission | `POST_NOTIFICATIONS` declared (`:83`); requested on TIRAMISU and later (`MainActivity.java:517`) | A-NC: AC-8 grant and deny |
| AC4-c | Exact alarms and background starts | No `AlarmManager` (comment `App.java:256`; `BackupScheduler.java:27`); WorkManager only | NA for exact alarms. A-NC for background FGS start from WorkManager (AC-11, REQ-016 AC-13) |
| AC4-d | Broadcast receiver export | `BackupBroadcastReceiver` exported with no permission (`:171-178`, CNTR-005 frozen; lint baseline `ExportedReceiver` entry is pre-existing) | A-NC: AC-12 both branches |
| AC4-e | SMS and default-SMS role | Role hand-over for restore (`SmsDefaultRoleHelper.kt`); restore inserts as the default app, which is exempt from OTP filtering | A-NC: AC-10. OTP behaviour is row 37-5 |

### D14 — The SMS one-time-code delay and the backup watermark

**What the code does, verified.**

- The SMS backup query is `date > ? AND type <> ?`. The argument is `getMaxSyncedDate(SMS)`, sorted
  by `date` with an optional `LIMIT` (`BackupQueryBuilder.java:60-63`, `:103-116`).
- After every confirmed IMAP append, the watermark is set to the batch's confirmed maximum
  (`BackupWorker.kt:416-422`). That maximum is the largest `X-smssync-date` header
  (`ConversionResult.java:19-30`). The header is set from the provider's `date` column in
  `mail/HeaderGenerator.java:67`. `MessageGenerator.java:109` reads the same column, but only for
  the message's sent date.
- Two other paths move the SMS watermark forward:
  - **Restore.** Each inserted inbox or sent SMS raises the watermark to its `date` when that is
    higher (`RestoreWorker.kt:463-467`).
  - **Skip.** The SKIP backup type sets the watermark to the newest provider `date` without
    uploading anything (`BackupWorker.kt:198-217`).
- There is no look-back window and no record of which rows were backed up. The watermark only
  moves back if the user clears the sync data (`DataTypePreferences.clearLastSyncData`, `:79`).

**Answer.** Yes, on the code: a message that becomes visible to the app three hours late *with its
original date* is never backed up, if a later SMS was backed up in the meantime. "Later" means any
incoming or sent SMS with a later `date`; drafts are excluded. On a phone in normal use that is the
likely case, not an edge case. The incoming-SMS debounce (CNTR-004 INV-4) usually backs up the next
message within minutes. If nothing else is backed up during the window, the withheld message is
picked up by the next backup after release, because its `date` is still above the old watermark.
MMS has its own watermark and is not named by the OTP pages (research §4.5: "standard SMS
messages").

The `SMS_RECEIVED` broadcast is also withheld for such a message; the app has no receiver for it that is delivered (the SMS-received receiver is to be removed by its own story (ruling 56), CR-017), so the content trigger is the only trigger concerned.
That delays a trigger; it does not lose data. Restore's own provider reads are unaffected: during
restore the app holds the default-SMS role, which is exempt. Restore does move the SMS watermark
(above); BUG-021 handles that.

**What is not known, and must be measured.** Research §4.5 and §4.6 say messages "do not become
available until three hours after receipt" and "SMS provider database queries are filtered". They
do not say whether the released row keeps its original `date`. Nothing beyond the cited text is
assumed here (A6).

**Measurement S5-I1: the single owner of the Android 17 measurement.** This section is the one
complete statement of what is measured, how, and what each result means (Requirements Reviewer's
audit of REQ-MODERNIZATION-020, accepted by the orchestrator). REQ-MODERNIZATION-020 and BUG-021
are to point to it and not restate it. Until the orchestrator updates them (Notes, last paragraph),
this section governs where they differ. Known today: BUG-021's Steps to Reproduce (b) and AC-12,
whose positive control expects `(1 messages)`, where M1a here expects `nothing to backup`. It folds in:
- BUG-021's corrected device procedure (its Steps to Reproduce (b));
- the Design Auditor's stop rule;
- the REQ-MODERNIZATION-020 review's points.

It runs on the new API 37 AVD, driven by QA or the orchestrator only, with foreground commands and
text evidence only.

**Preconditions.**
- **Device.** The new API 37 emulator, created with the owner's permission (PO-2). Never
  `Pixel_10_Pro` (Record 14, row 2).
- **Appends.** The owner's permission for the appends from that device (PO-3).
- **Account.** The owner's account is configured before M0a, with the password entered by the
  owner (PO-2(d); Record 14, row 3).
- **Auto backup OFF during every counted run.** The SMS receiver is enabled, and the content trigger
  scheduled, only when auto backup is on (`App.java:258`, `:376-383`). With auto backup off, no
  backup starts except the manual ones the procedure runs, so each count belongs to one known run.
- **The SMS watermark already exists.** One backup has already run (M0a), so the first-backup
  dialog is not shown at M1a.
- **One data type.** Only SMS backup is enabled; MMS and call-log backup are off. These are app
  settings, restored afterwards; no system setting is changed (Record 14, row 4).
- **Account needed.** Without an account, a manual backup cannot start: `MainActivity.performAction`
  shows the missing-credentials dialog instead (`MainActivity.java:393`, `:401-402`). There is no
  no-account variant. Without an account, the measurement is "not run".
**How the evidence is read.**
- **Which app is being measured.** The filtering is applied per calling app. So the measurement is
  the **app's own count**, from the log line "BackupWorker: starting backup (%d messages)"
  (`BackupWorker.kt:313`). A count of 0 appears as "BackupWorker: nothing to backup"
  (`BackupWorker.kt:262`).
- **What the shell can show.** `adb -s <serial> shell content query --uri content://sms --projection _id:address:date:type:body`
  runs as the shell, not the app. It answers only the date question, and only if the shell sees the
  row at all. It never stands in for the app's view.
- **Builds.** The target-35 build is the debug build of the branch. The target-37 build is made from
  an **uncommitted scratch change** of `targetSdkVersion`. A committed one would put targetSdk 37 in
  history before the fixes, which breaks REQ-017 AC-5's co-landing rule.

**Cases.** One run per row:
- row 37-5: the scratch target-37 build with a standard one-time-code message,
  `"Your verification code is 123456"`;
- row 17-2: the target-35 build with a WebOTP-format message, whose exact text is taken from the
  platform's WebOTP documentation and recorded. That is the first step for this row. If the format
  cannot be established, row 17-2 is recorded as unmeasured (BUG-021, Scope Concern 4).

**Run A, the gap run (auto backup OFF).**

| Step | Action | Observed |
|---|---|---|
| M0 | Install the build under test; confirm the preconditions. Record device API, build target and time | — |
| M0a | **Preliminary completed backup.** Run one manual backup. When the FIRST_SYNC dialog appears (`MainActivity.java:396-397`), answer "back up", never Skip | A completed backup; the SMS watermark exists; later backups are not first backups |
| M0b | **Counting control** (BUG-021's corrected procedure, step 1), before the code message is sent. Send two plain messages; manual backup | `BackupWorker: starting backup (2 messages)` (`BackupWorker.kt:313`). The count line reflects plain messages, so a later count can be read. If it does not read 2, stop: the counting method is invalid and the run is "not run" |
| M1 | At T0, send the message under test with `adb -s <serial> emu sms send 5550001 "<text>"` | Time T0 |
| M1a | **Positive control**, at T0 + 1 min: manual backup | The app's count excludes the code message: `nothing to backup`. A count that includes it (the app sees the code message) is the inconclusive case below |
| M2 | Shell content query | Whether the shell lists the row; its `address` and `date` (date question only) |
| M3 | At T0 + 2 min, send plain message B; manual backup | `starting backup (1 messages)`, and `BackupWorker: watermark advanced for SMS to <t>` (`BackupWorker.kt:423`) with t = date(B), later than the code message's date at T0 |
| M4 | At T0 + 3 h + 10 min, with the emulator clock never changed, repeat M2 | Whether the row is listed; its `address` and `date` now |
| M5 | Manual backup | `starting backup (1 messages)` (picked up) or `nothing to backup` |

**Run T, release time (auto backup OFF).** It runs when Run A ends in `nothing to backup` at M5.
On the unfixed build, Run A cannot show when the message became visible: once the watermark is past
it, the app's count stays 0 whether the message is still hidden or already skipped.
1. Repeat M0 to M1a with a fresh message of the same form, and send no message B, so the watermark
   stays below the code message.
2. From T0 + 3 h + 10 min, run a manual backup every 30 minutes up to T0 + 6 h.
3. Record the first time the app's count includes the message, or "not visible by T0 + 6 h".

This is the only evidence of the delay's length and jitter, which BUG-021's 6-hour look-back bound
(Record 14, row 10) relies on.

**Run R, trigger at release (auto backup ON).** A second run of M1 with auto backup on, in which
only the `BackupTriggerWorker` log lines are read (the SMS-received receiver is not delivered the broadcast and is to be removed by its own story (ruling 56), CR-017): around T0, and around
the release time found by Run T or after T0 + 3 h. No count is read in this run, because automatic
backups move the watermark on their own. If Run R is not run, "trigger at release" is recorded as
**not measured**.

**Appends.** The test SMS are injected into the emulator, and the backups at M0a, M0b, M1a, M3, M5
and in Run T and Run R append them to the owner's mailbox. Adding and deleting emulator SMS, and
adding or removing the SMS folder in the mail account, are within the owner's standing instructions
(decision appendix, Record 14, row 1). The appends from a newly created device are asked under PO-3,
and the device itself under PO-2.

**Stop rule.** Each row is decided by its own run:
- row 37-5 by the scratch target-37 build with a standard one-time-code message;
- row 17-2 by the target-35 build with a WebOTP-format message.

A run counts only if all of these hold:
- (i) before M1 the device has completed one backup, with the FIRST_SYNC dialog
  (`MainActivity.java:396-397`) answered "back up", never Skip, so M1a is not a first backup;
- (ii) M1a passes: the app's count excludes the code message. A count of 0 appears as
  "BackupWorker: nothing to backup" (`BackupWorker.kt:262`);
- (iii) at M3 the line "BackupWorker: watermark advanced for SMS to <t>" (`BackupWorker.kt:423`)
  shows t = date(B), later than the code message's date at T0.

A run also needs the counting control (M0b) to pass and auto backup to be off.

The outcomes, and what each means for sequencing:

| Outcome | When | Meaning | Sequencing |
|---|---|---|---|
| **Measured not affected** | (i) to (iii) hold, and at M5 the app logs `starting backup (1 messages)` and `BackupWorker: watermark advanced for SMS to <t>` (`BackupWorker.kt:423`), with t equal to the code row's `date` as the shell lists it at M4. If the shell does not list the row, t is later than date(B), and every code message from an earlier run or case is accounted for (backed up, or recorded as not visible by T0 + 6 h). | — | The targetSdk 37 change (S5-I4) does **not** wait for REQ-MODERNIZATION-020's fix (BUG-021). Row 37-5 or 17-2 is NA, with the run as evidence |
| **Gap confirmed** | (i) to (iii) hold, M5 logs `nothing to backup`, and Run T shows the message visible to the app at or before M5's time | The watermark skipped it | S5-I4 waits for BUG-021. Once BUG-021 has landed, the row is A-F by BUG-021, with this outcome recorded. |
| **Gap not ruled out** | (i) to (iii) hold, M5 logs `nothing to backup`, and Run T shows it later than M5's time, never by T0 + 6 h, or was not run | On the unfixed build `nothing to backup` fits both a skip by the watermark and a delay longer than the check time | S5-I4 waits for BUG-021. Once BUG-021 has landed, the row is A-F by BUG-021, with this outcome recorded. |
| **Inconclusive: app sees the code message** | M1a's count includes it | The platform did not treat this text as a one-time code, or the app is not filtered; never "not affected". May be repeated once with another message form, and the record says which form each run used | S5-I4 waits for BUG-021. Once BUG-021 has landed, the row is A-F by BUG-021, with this outcome recorded. |
| **Inconclusive: other** | Anything else, including no account, a failed counting control, a failed or missing M3 append, or an unknown WebOTP format | — | S5-I4 waits for BUG-021. Once BUG-021 has landed, the row is A-F by BUG-021, with this outcome recorded. |
| **Not run** | The measurement was not run | — | S5-I4 waits for BUG-021. Once BUG-021 has landed, the row is A-F by BUG-021, with this outcome recorded. |

**Limits the record must state.**
- The shell is not the app.
- An emulator may differ from a device.
- Each case is one run.
- The WebOTP text is taken from the platform documentation.
- The trigger observation is "not measured" if Run R was not run.
- Run T's release time is taken to apply to Run A; the record says so.

**After the fix.** The device check of the fixed build belongs to the fix story (BUG-021 AC-12), not
to S5-I1. On the fixed build, repeat Run A to M4. Then:
- the first manual backup after release logs `BackupWorker: backup complete, backedUp=1`
  (`BackupWorker.kt:369`);
- the next two manual backups log `nothing to backup` or `backup complete, backedUp=0`.

**Operator practicalities.**
- **Starting a backup from adb.** `BackupBroadcastReceiver` is exported with action
  `com.zegoggles.smssync.BACKUP` (`AndroidManifest.xml:171-178`). When the app's third-party
  integration setting is on, it schedules an immediate backup through the same `BackupWorker` path
  (`BackupBroadcastReceiver.java:49-55`; the backup type only affects the status display,
  `BackupType.java:37-39`). Every backup after M0a is therefore started with
  `adb -s <serial> shell am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync`.
  - The integration setting is an app setting: it is turned on for the measurement and restored
    afterwards.
  - M0a itself is started by a tap, because the first-backup dialog is UI-only. The tap target is
    located by a screenshot, as the U-065 emulator check did. Screenshots are not kept as evidence.
- **Device clock.** At every step, record `adb -s <serial> shell date`.
- **No sleep.** The host must not sleep during a run.
- **Duration.** One case takes about 12 hours of emulator time for Runs A, T and R, and both cases
  about 24 hours. The operator cannot wait in the foreground, so needs scheduled re-entry at each
  timed step.
- **Auto backup really off.** After auto backup is turned off, confirm with
  `adb -s <serial> shell dumpsys jobscheduler` that no job for `com.zegoggles.smssync` is waiting.

**Classification and decision.**
1. **A requirement gap.** REQ-017 Constraints say backup results are "identical for the user before
   and after, apart from platform-mandated UI changes". Permanently missing messages are neither, so
   REQ-017 cannot be met at target 37 without a fix. **Orchestrator ruling (Record 13, row 4, made
   under Record 12): fix first, then targetSdk 37**, conditional on S5-I1 confirming the gap. It
   keeps Record 4 and costs no device in use. The product owner is informed, not asked.
2. **The bug: BUG-021** (`sdlc/artifacts/stories/BUG-021-delayed-otp-sms-skipped-by-backup-watermark.md`).
   - It covers the WebOTP-format case (17-2), which applies at every target level on Android 17
     devices, and the target-37 case (37-5).
   - If S5-I1 confirms the gap, BUG-021 is designed and built before S5-I4.
   - The completeness rule it implements is REQ-MODERNIZATION-020 (BUG-021, DP-4).
3. **Not designed here.** These belong to BUG-021's Suggested Approach and its decision points DP-1
   to DP-5, under REQ-MODERNIZATION-020:
   - the mechanism and its key;
   - the look-back bound (a bounded look-back of 6 hours under the decision-point defaults, Record
     14, row 10);
   - the handling of restore, Skip, first run and upgrade.

   This design imposes only two constraints. The fix lands before S5-I4, and it neither changes the
   `MailTransport` port nor depends on the mail library that Part B replaces (BUG-021 AC-11).

**For the product owner's information.** Does any of your devices run Android 17? If so, the
all-targets WebOTP case may already affect you today, whatever this work does.

### D15 — TLS: certificate transparency, ECH, local network at target 37

**CT (37-4).**
- *Default path.* `DefaultTrustedSocketFactory` uses `TrustManagerFactory.get(host, port)`
  (`DefaultTrustedSocketFactory.java:189-192`). `SecureX509TrustManager` delegates to the platform
  trust manager with the two-argument, host-less `checkServerTrusted(chain, authType)`
  (`TrustManagerFactory.java:68-76`). That platform trust manager comes from
  `javax.net.ssl.TrustManagerFactory.getInstance("X509").init((KeyStore) null)` (`:106-107`), and
  the app declares a network security config (`AndroidManifest.xml:97`). Whether the platform's CT
  check runs on that host-less call is **not documented in the sources read** (A10).
- *Pinned path.* `PinnedCertificateSocketFactory` initialises its `SSLContext` with only its own
  `PinnedX509TrustManager` (`PinnedCertificateSocketFactory.java:88-90`). That trust manager compares
  fingerprints and checks validity, and never calls the platform trust manager (`:135-190`).
  Platform CT therefore cannot apply there.
- *Effect.* The owner's server presents publicly logged certificates, so the outcome is the same
  under either reading. Disposition A-NC, observed by AC-9's handshake at target 37. Residual risk,
  accepted and recorded: a server on the default path with an unlogged public certificate would now
  be refused.

No contract change: CNTR-MODERNIZATION-001 states the factories' trust decisions. CT is platform
policy beneath them, and CR-014 already scopes what the contract will state.

**ECH (37-3).** The research says ECH is used "when the networking library supports it" and is
controlled by `<domainEncryption>` in the network security config. The app's sockets come from
`SSLContext.getInstance("TLS")` and the platform provider, and the app resolves hosts through
`InetAddress`, not a resolver that fetches HTTPS records. ECH is expected not to engage (A11).
Observed by AC-9. If a handshake fails with an ECH-attributable error, the `<domainEncryption>`
control in `app/src/main/res/xml/network_security_config.xml` is the fix, co-landed (A-F).

**Local network (37-6).** NA by orchestrator ruling PO-6: the only server in use is Gmail. If a LAN
server were ever used at target 37 without the permission, the connection would fail as a
connection error. The app maps connection errors to "retry or fail" (`BackupWorker.kt`
`MailException`), so the result is a failed backup, not a crash.

### D16 — On-device validation plan

**Constraints, from earlier device work and the brief:**

- Only the orchestrator or QA drives an emulator.
- `Pixel_10_Pro` is the emulator never to touch, by standing instruction of the product owner
  (Record 14, row 2; A9). It is never started, listed for use, or written to.
- Agents do not change an emulator's system settings (Record 14, row 4). App settings, such as the
  backup data types and the integration toggle, are restored afterwards.
- **Authorisation for `Pixel_9_Pro` (2026-11-20, "Start it for me"):**
  - the orchestrator may start it, and install a debug build over the configured app;
  - adding or deleting SMS messages on the emulator, and adding or removing the SMS folder in the
    mail account, are safe (Record 14, row 1).

  Anything beyond that is PO-3. That includes manual backups that append to the account's other
  folders, the restore's default-SMS-role hand-over, changing the third-party-integration setting,
  and any step whose rollback could need an app-data clear.
- Nobody but the owner handles the account password (Record 14, row 3).
- A debug build goes over the configured app with `adb install -r`. Never uninstall, never
  `pm clear` on a configured emulator.
- Text evidence only: no screenshots, no account names. Host names and user paths are redacted
  (sprint-017 retro, item 4).
- Third-party integration and any other preference touched are restored afterwards (REQ-017).
- `MSYS_NO_PATHCONV=1` only in adb shells, never for Gradle.
- The serial is found with `adb devices` first and confirmed with `adb -s <serial> emu avd name`.
  Every command then uses `-s <serial>`.
- On a new device (DV-2, DV-3), the first-run permission prompts (REQ-017 AC-8) are recorded
  **before** the owner configures the account.
- Emulators are shut down after the run.

**Devices.**

| ID | Device | State | Used for |
|---|---|---|---|
| DV-1 | `Pixel_9_Pro`, API 36 | App and Gmail configured (Record 7, item 4); start and install-over authorised | S1 smoke; S2 AC-17 "higher level"; S4 AC-9, AC-11, AC-12, AC-13 (install-over keeps the data; PO-3 for what goes beyond the authorisation); S5 regression at 36 |
| DV-2 | New AVD on the installed `android-37.0` `ps16k` image | To be created (PO-2b); the owner enters the app password (PO-2d) | S5 AC-6 to AC-12; D14 measurement |
| DV-3 | New AVD on an API 24 image | Image to be downloaded (PO-2a); the owner enters the app password (PO-2e), because REQ-015 AC-17 needs a manual backup and restore to finish at API 24 | S1 smoke; S2 AC-17 at the floor |
| DV-4 | `Pixel_4_2`, API 30 (Android 11) | Exists | Recommended extra S2 smoke at the owner's oldest real-device level |
| — | `Pixel_4` (29), `Pixel_7` (33) | Exist | Fallbacks |

**What is observed.** Per acceptance criterion, as the traceability table lists it. Examples:

- the `am start -W` status;
- `pidof`;
- `logcat` greps for `FATAL EXCEPTION`, `IllegalArgumentException`, `SecurityException`,
  `ForegroundServiceStartNotAllowedException` and Hilt initialisation errors;
- the BackupWorker log lines;
- `dumpsys jobscheduler` for the job ID;
- `cmd jobscheduler run -f com.zegoggles.smssync <id>` for AC-11;
- `am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync` with integration on and off
  for AC-12;
- per screen, on `MainActivity` and each preference screen, an insets and back record for AC-7.

**Fallbacks if permission is not granted, and what they prove.**
- **No API 24 device.** The retargeted Robolectric tests at SDK 24 prove that the pre-Q branches
  execute under a simulated 24. The API 29 emulator proves the lowest real level available.
  REQ-015 AC-17's API 24 part is recorded "not reached", and the product owner acknowledges it.
- **No API 37 device.** A target-37 build on DV-1 (Android 16) proves install, launch and the
  target-36 behaviours (36-1, 36-2, AC-8, AC-11, AC-12 at 36). It proves none of the Android 17
  behaviours (37-3 to 37-6, 17-1, 17-2). REQ-017 AC-6 to AC-12 at level 37 are recorded "not
  reached". REQ-017's Rationale makes device proof a gate, so targetSdk 37 must not be reported as
  done. The owner decides.

### D17 — Lint

**Decision.**
- **S1: prune.** Lint's "listed in the baseline file but not found" message gives only a count, so it
  is not the oracle on its own. **Method:**
  1. Regenerate the baseline on a scratch copy of the tree.
  2. Diff the scratch baseline against `app/lint-baseline.xml`.
  3. Apply only the removals to the real file; no addition is ever applied.
  4. Confirm that lint's "not found" count falls to zero, or by exactly the number of removed
     entries.

  Expected stale entries:
  - the 10 `ObsoleteSdkInt` entries: `CalendarAccessorPost40` (S-2), `Dialogs` (S-1),
    `SmsReceiver` ×2 (S-3, S-4), `MainActivity` ×2, and the already-deleted `ServiceBase.java` ×2
    and `SmsRestoreService.java` ×2. Site S-6 (`SmsDefaultRoleHelper.kt:74`) has no baseline entry
    of its own: its check moved out of `MainActivity` (U-050), and the `MainActivity` entry is the
    stale one;
  - `Deprecated`, for the deleted `BackupJobs.java`.

  Record the count before (38) and after, with each removed entry (AC-8). `app/lint.xml` gets no
  addition.
- **S1 and S2: record, don't gate.** `:app:lintDebug` was red before JDK 25 (39 errors / 22 filtered
  at AGP 9.0.1, U-060 Step 6). Their rule IDs are not recorded anywhere (A22), so S1-I0 records
  them. That they are all test-source findings is **not** established; only the first,
  `ContactAccessorTest` / `CheckResult`, is known. Each story records the rule-ID counts before and after, and names any
  new rule ID. For REQ-015 AC-18 the pre-existing failure is "recorded ... with its failing rule IDs
  and a follow-up item", and the follow-up is S3.
- **S3: REQ-017 AC-14 option (a).** At compileSdk 37, regenerate the baseline with AGP's baseline
  task (`updateLintBaseline` in AGP 8 and later; S3 confirms the name on 9.5). List every newly
  baselined issue category with its count. Findings in `NewApi`, `ExportedReceiver` or
  `ForegroundServiceType` are fixed, not baselined. The one existing `ExportedReceiver` entry,
  `BackupBroadcastReceiver`, is mandated by CNTR-005 and is not new. Expected new categories include
  `OldTargetApi` (targetSdk is still 35 while compileSdk is 37) and `GradleDependency` (the S4
  libraries are not yet bumped); both are listed, not hidden. Result: `lintDebug` green, and CI's
  `lint` step green from then on.
- **S5:** lint at target 37. New findings follow the same three-category rule, and the delta is
  listed.

**Rejected.**
- *Option (b), lint left red.* New regressions stay hidden among 39 old errors, and CI stays red for
  ever.
- *Fixing all pre-existing errors here.* Their rule IDs are not yet known (A22), and fixing
  unrelated findings is scope creep. S1-I0 records them; the owner may ask for a cleanup story.

### D18 — Gates and evidence for every story

| Gate or check | S1 | S2 | S3 | S4 | S5 | Command or method |
|---|---|---|---|---|---|---|
| G1 debug build | ✓ | ✓ | ✓ | ✓ | ✓ | `./gradlew :app:assembleDebug` (Git Bash without `MSYS_NO_PATHCONV`, timeout above 3 minutes) |
| G2 unit tests | ✓ | ✓ | ✓ | ✓ | ✓ | `./gradlew :app:testDebugUnitTest`; XML totals: failures 0, errors 0, skipped ≤ 2 (D9) |
| G3 coverage | ✓ | ✓ | ✓ | ✓ | ✓ | `./gradlew :app:jacocoTestCoverageVerification`; per-package ratios and class inventory against the previous increment (D8) |
| G4 release build with R8 | ✓ | ✓ | ✓ | ✓ | ✓ | `./gradlew :app:assembleRelease`; R8 kept-name set and `configuration.txt` diff (U-060 C-2) |
| Test count | ✓ | ✓ | ✓ | ✓ | ✓ | `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' \| grep -c "@Test"` at start and end; every difference by name |
| Verification | — (no diff) | ✓ | ✓ if any | ✓ | — | D5 protocol and proof |
| Manifests | ✓ | ✓ | ✓ | ✓ | ✓ | Merged (debug, release) and packaged (`aapt2 dump xmltree`) diffs. Invariants: `SystemForegroundService` `foregroundServiceType="dataSync"`; no `InitializationProvider`; `BackupBroadcastReceiver` exported with action `com.zegoggles.smssync.BACKUP` |
| APK contents | ✓ | ✓ | ✓ | ✓ | ✓ | `.so` count = 0; entry-list diff explained |
| Bytecode | — | ✓ | — | ✓ | — | `javap -v` majors: 61 app (Java and Kotlin), 52 vendored (AC-15) |
| Greps | AC-6, AC-7 | REQ-016 AC-2 style "no pre-release" over the build files | AC-2 | REQ-016 AC-2 | AC-2 | As written in the ACs |
| Lint | record + prune | record | refresh (a) | record | delta | D17 |
| Warnings | — | `--warning-mode all` once per increment, recorded | ✓ | ✓ | ✓ | — |
| Emulator | smoke | AC-17 | optional smoke | AC-9, AC-11, AC-12, AC-13 | AC-6 to AC-12 | D16 |

"Done" for a story means:

- every row above is green or recorded as allowed;
- every acceptance criterion it owns has the evidence the traceability table names;
- the review, security and QA verdicts pass;
- any AC-4 fallback is flagged.

### D19 — Rollback model

**Decision.** Every increment is a commit on `sdlc/modernization-plan`. Rolling back a story means
`git revert` of its commits, newest first, pushed normally: no history rewrite, no force-push
(Record 12). Verification entries leave with their commit.

Per story:

| Story | Rollback |
|---|---|
| S1 | Revert. Devices: reinstall the previous debug APK with `adb install -r`, same `versionCode` 1602 |
| S2 | Revert. Re-apply nothing: the `gradlew` block is restored with the reverted wrapper commit |
| S3 | Revert the single compileSdk commit |
| S4 | Revert. **Device caveat, unverified (A16):** WorkManager keeps an internal database. Whether 2.9.1 can open one that 2.12.0 has migrated is not known. S4 planning reads WorkManager's database migration and downgrade settings first. Only if a downgrade fails does rolling S4 back on a device need an app-data clear, which loses the stored credentials (only the owner re-enters them). PO-3 states the cost as it is then known: "possible", or "certain", or "none" |
| S5 | Revert the targetSdk commit (and its co-landed fixes) |

The release build is not distributed: the app is private and unpublished, so a rollback concerns
the owner's emulators and devices only.

### Cross-cutting concerns

The prompt's generic cross-cutting concerns are each judged for this build design.

- **Logging and observability.** The "observability" of this work is its evidence: implementation
  logs written after every step (sprint-017 retro, recommendation 5); command outputs saved under
  the story's `evidence/`; app logs read through `adb logcat` with tag `SMSBackup+`.
- **Error handling.** Every unknown has a stop rule: A6, A9, A13 to A16, D3 triggers, D10's F-1
  rule, D14's rule. A stop produces an implementation-log entry quoting the error verbatim, not a
  workaround.
- **Authentication and authorisation.** The only credential in play is the owner's mail-account
  app password on the emulators. Agents never enter, read or log it (D16). No build credential
  changes: release signing via `keystore.properties` (`app/build.gradle:13-17`) is untouched.
- **Caching.** Gradle caches and Robolectric's `android-all` cache, which sits outside verification
  (D5). There is no runtime caching change.
- **Configuration management.**
  - `gradle.properties` jvmargs (`:20`) are unchanged; U-060 verified them on JDK 25.
  - The daemon criteria file is unchanged (REQ-015 AC-12).
  - `gradlew`'s Windows block is re-applied after regeneration (S2-I1).
  - No `org.gradle.java.home` goes into the repository.

### Quality attributes (ISO/IEC 25010)

| Characteristic | How this design addresses it |
|---|---|
| Functional suitability | Backup completeness is protected by D14 (target 37 gated). Restore role flow (AC-10); preference screens (AC-11) |
| Performance efficiency | Runtime: no expected change. WorkManager 2.10.5 fixes an FGS-stopping bug. Build and test: new SDK jars and a larger verification file; Robolectric default kept at 29 to avoid per-sandbox cost (D6) |
| Compatibility | Drops API 21 to 23 (no device in use). Co-existence with the future Thunderbird variants (minSdk 24, compileSdk 37). Android 17 platform rules (D13) |
| Usability | Edge-to-edge and predictive back verified per screen (36-1, 36-2) |
| Reliability | FGS invariant (BUG-016) kept by manifest fitness check; scheduled-run checks; F-1 cancellation defect surfaced; WorkManager DB rollback caveat (D19) |
| Security | Verification stays enforced with additions-only proof (D5). Gradle 9.9.2's verification fix. CT and ECH analysed (D15). `security-crypto` deprecation surfaced (D11). Local-network limitation (37-6) |
| Maintainability | One component per increment (AD-6); dead code removed (D4); stale comments corrected; lint made green (D17); JUnit XML (D9) |
| Portability | JDK 25 kept; Windows `gradlew` block preserved; CI Linux runner check (A18); 16 KB page images exercised (16-3) |

## Integration Design

### Integration inventory

| # | External system | Purpose | Type | Auth | Critical? | Stories |
|---|---|---|---|---|---|---|
| INT-001 | Maven Central, Google Maven (`google()`, `mavenCentral()`, `maven.google.com`; no Gradle Plugin Portal is configured) | Resolve plugins (buildscript classpath) and libraries | HTTPS artifact repositories (`build.gradle:2-5`, `:33-45`) | None | Yes (build) | S2, S3, S4 |
| INT-002 | services.gradle.org | Gradle distribution and checksums | HTTPS | None | Yes (S2) | S2 |
| INT-003 | Android SDK repository (via the SDK Manager) | Platform 37 (installed), system images, Build Tools | HTTPS manifest `repository2-3.xml` | None | Yes (devices) | S3, S5, S1/S2 devices |
| INT-004 | Robolectric `android-all` resolver | SDK jars at test time | HTTPS to Maven Central, outside Gradle verification | None | Yes (tests) | S1 (SDK 24), S2-I7 |
| INT-005 | GitHub Actions | First CI run | `pull_request` / `push` to `master` (`ci.yml:3-9`) | Owner's GitHub account | No (gate is local) | S2-I15, S3-I4 |
| INT-006 | Android emulators via `adb` | Device validation | Local | Debug signing | Yes (REQ-017) | All |
| INT-007 | The owner's IMAP server (Gmail) during device runs | AC-9 LOGIN and append; AC-12 credentials | IMAPS via the app's TLS factories (CNTR-001) | App password, held by the app and entered only by the owner | Yes (REQ-016 AC-13, REQ-017 AC-9) | S4, S5 |
| INT-008 | Android platform providers (SMS, contacts, JobScheduler, RoleManager) | The app's own runtime integrations under new platform rules | Content providers and system services | Runtime permissions and roles | Yes | S5 (D13, D14) |

### Data flow and data boundaries

- **INT-001 to INT-004.** Outbound: coordinates and versions only. Inbound: artifacts, whose
  checksums are recorded by Gradle's writer and enforced afterwards (D5). Nothing from the
  repository or the owner leaves the machine.
- **INT-005.** Outbound: the repository contents of the branch, already pushed under Record 12.
  The PR itself needs the owner (PO-4).
- **INT-007.** Outbound: SMS, MMS and call-log content of the *emulator*. For DV-1 that is whatever
  the owner placed there; for DV-2 the injected test messages. Never anything from a physical
  device. The password never leaves the app's encrypted store. Evidence records counts and status
  lines, never message bodies of real messages, never account names.
- **INT-008.** Read only for backup. Restore writes to the emulator's SMS provider while holding the
  role.

### Error handling, fallback and testing per integration

| # | Failure | Detection | Handling | Fallback | How it is tested |
|---|---|---|---|---|---|
| INT-001 | Artifact missing, checksum mismatch, network error | Gradle failure; verification failure naming the artifact | Retry on network errors. A checksum mismatch on an *unchanged* artifact is a **stop**: possible tampering, never re-written | D3 F1 to F5 for incompatibility only | Enforced re-run after every writer pass (D5 step 6) |
| INT-002 | Wrong distribution | `distributionSha256Sum` mismatch | Stop | F2's Gradle 9.7.0 has its own official checksum | `./gradlew --version` after S2-I1 |
| INT-003 | Package unavailable or not permitted | SDK Manager error, or the owner declines | Record | D16 fallbacks | Listing of the SDK folder (`platforms/`, `build-tools/`, `system-images/`) before and after, as for A8; `emulator -list-avds` for a new device |
| INT-004 | `android-all` download fails | Robolectric "unable to resolve" | Retry once; record | None: a test environment fault, never a reason to skip tests | S1-I3 first SDK 24 run |
| INT-005 | CI not triggered or red | Workflow result | Record each step's result (AC-18); a pre-existing lint failure is fixed in S3 (D17) | Local CI-parity run `./gradlew test lint jacocoTestReport assembleRelease --continue` (U-060 Step 8.2) | YAML parse check of `ci.yml`, plus the local CI-parity run on every S2 and S3 increment that touches `ci.yml` |
| INT-006 | Emulator boot or install failure | `adb` exit status | Retry boot; never wipe a configured AVD | Other AVDs (D16) | `adb devices` then `emu avd name` (identity), `adb install -r` result line, `am start -W` status and `pidof` before any acceptance step |
| INT-007 | LOGIN fails after the upgrade | Worker failure reason `auth_failed`; no credential re-prompt expected | **Stop**: AC-12 failed. The owner re-enters, never an agent | AC-9 records "not reached" if no account | `EncryptionDegradedGateTest`, `AuthPreferencesTest` (unit level) |
| INT-008 | Platform withholds or filters data | D14 measurement | Per Record 13, row 4 | targetSdk stays 35 until BUG-021 lands, unless the measurement shows "not affected"; restore is unaffected (role-holder exemption) | D14 M0 to M7 with the M1a positive control |

There is no circuit breaker. These are human-supervised build-time integrations, and the
fail-and-stop behaviour above is the right degradation for a supply-chain boundary: a silent retry
past a verification failure would be the vulnerability.

### Security considerations

- **Supply chain.** Verification enforced, additions only, reviewed groups (D5). Wrapper checksum
  pinned. Gradle 9.9.2's verification fix. CI actions move to their current majors. Pinning by
  commit SHA would be stronger. It is not required by REQ-015 and is recorded as a recommendation,
  not done.
- **Credentials.** No new secret. The emulator account password stays the owner's (D16).
  `keystore.properties` untouched.
- **Data in transit.** TLS 1.2 floor and cipher handling unchanged (U-065, CNTR-001 with CR-014).
  CT and ECH (D15).
- **Evidence hygiene.** Text only; no screenshots, account names, host names or user paths
  (sprint-017 retro). The AC-1 record cites public URLs only.

## Integration Contracts

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status | Touched by | Revision needed? |
|---|---|---|---|---|---|---|---|
| TLS socket factories | `DefaultTrustedSocketFactory`, `PinnedCertificateSocketFactory` | mail transport | service | CNTR-MODERNIZATION-001 | approved | S2-I7 (test provider only), S5 (platform CT and ECH, D15) | **No.** Production TLS code is not edited (D4b). CT and ECH are platform policy beneath the contract's trust statements. CR-007, -008, -009, -012, -013, -014 stay pending for the migration's combined revision, unaffected |
| Pinned-certificate enrolment | enrolment UI | pinned factory | ui/service | CNTR-MODERNIZATION-002 | approved | none | No |
| Secret store | `EncryptedPrefsSecretStore` | `AuthPreferences` | service | CNTR-MODERNIZATION-003 | approved | S4 (`security-crypto` unchanged; AC-12 data-at-rest) | **No.** The contract calls library bumps and producer swaps backward-compatible. The replacement recommendation (D11) would use that clause |
| Backup scheduler | `WorkManagerScheduler` | receivers, `App`, `MainViewModel` | service | CNTR-MODERNIZATION-004 | approved | S1 (S-7 deletes the pre-24 branch; table T retargets its test), S4 (WorkManager 2.12) | **Yes: change record P-1, a mandatory precondition of S1.** Not editorial; see "CNTR-MODERNIZATION-004 and DES-MODERNIZATION-005" below |
| Public `BACKUP` broadcast | external apps | `BackupBroadcastReceiver` | event | CNTR-MODERNIZATION-005 | approved | S1-I2 (new unit test), S5 (AC-12 both branches) | No (frozen; verified) |
| Sync state repository | workers | UI | service | CNTR-MODERNIZATION-006 | approved | S4 (coroutines 1.11, `SharedFlow`/`StateFlow` fixes) | No; tests unchanged |
| Mail transport ACL | `K9MailTransport` | `service.*` | service | CNTR-MODERNIZATION-007 | approved | none (toolchain only) | No |
| Contacts port | `ContactAccessor` | backup | service | CNTR-MODERNIZATION-008 | approved | S5 (CP2 rules 37-8, 37-9: not affected) | No |

### CNTR-MODERNIZATION-004 and DES-MODERNIZATION-005: the pre-API-24 content-trigger fallback (P-1)

**Why it is not editorial.**
- The contract's Versioning section (CNTR-004 `:260`) lists "losing the content-trigger pre-24
  fallback or its debounce" as a break of the behavioural contract, which "MUST be gated by the
  INV-1..4 tests".
- DES-MODERNIZATION-005's Design Validation (`:387`) requires an "API ≤23" assertion of the
  broadcast fallback. S1's retarget of
  `WorkManagerSchedulerTest.inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` removes that
  assertion.

**Every line carrying the pre-24 text:**

| Artifact | Lines |
|---|---|
| CNTR-MODERNIZATION-004 | `:38` (producer row, "version-branches the content-URI trigger (`SDK_INT >= 24`)"); `:64`; `:170`; `:240` (INV-4 row); `:260` (Versioning); `:381-383` |
| DES-MODERNIZATION-005 | `:238` (AC-3(d) row); `:324-329` ("API 14–23 ... Retain the existing broadcast fallback"); `:387` (INV-4 validation row) |

**The argument for the change record to rule on: no fallback is lost at minSdk 24.**
1. The removed branch (`WorkManagerScheduler.kt:224-230`) runs only when `SDK_INT < 24`. At
   minSdk 24 no installable device reaches it. Removing it changes no behaviour on any supported
   level.
2. The fallback itself is not code in `scheduleContentTrigger`: it is the `SMS_RECEIVED` path,
   `SmsBroadcastReceiver.incomingSMS` → `scheduler.scheduleIncoming()`
   (`SmsBroadcastReceiver.java:58-60`, manifest `:146-153`). That path is untouched and still runs
   on **every** level, with the same debounce (`initialDelay = incomingTimeoutSecs`).
3. INV-4's obligation, "an SMS content change MUST trigger backup ... preserving the two-stage
   debounce", therefore holds on every supported level. It is met by the content-URI trigger, which
   the retargeted test `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` asserts at the new
   floor, and by the unchanged broadcast path, which `SmsBroadcastReceiverTest` covers.
4. What the record must change:
   - remove the API ≤23 clauses as obsolete scope, not as a lost capability;
   - replace DES-005's "API ≤23" validation row with the API-24 boundary assertion;
   - state in CNTR-004 `:260` that the minSdk 24 floor retires the pre-24 obligation, so the
     Versioning rule is not read as broken.

**Ruling.** The change record rules on this before S1 is planned (precondition P-1). If it rules
that the fallback must stay asserted, S1 keeps an SMS_RECEIVED-path test as the INV-4 fallback
evidence instead.

### Contracts Needed (pre-sprint gate)

None. This design adds no new component boundary.

- [x] CNTR needed: none new. The one revision is change record P-1 on CNTR-MODERNIZATION-004 and
  DES-MODERNIZATION-005, a mandatory precondition of S1.

## Design Validation

### Requirements inventory (the truth set)

| Requirement | Acceptance criteria | Count |
|---|---|---|
| REQ-MODERNIZATION-015 | AC-1 to AC-18 | 18 |
| REQ-MODERNIZATION-016 | AC-1 to AC-13 | 13 |
| REQ-MODERNIZATION-017 | AC-1 to AC-16 | 16 |
| **Total** | | **47** |

Constraints also checked:

- JDK 25 stays;
- verification enforced;
- no floor lowered;
- one bump per increment;
- behaviour preserved;
- `BackupBroadcastReceiver` stays exported;
- minSdk 24 sequencing.

### Traceability: every acceptance criterion to its story and evidence

| AC | Story | Evidence that satisfies it |
|---|---|---|
| 015 AC-1 | S2 | Re-resolution table (date, URL) equal to the build files (D1, D1a) |
| 015 AC-2 | S2 | Verbatim support quotes with URLs (D3 step 1) |
| 015 AC-3 | S2 | Reading and gaps G1 to G6 with detection (D3 step 2) |
| 015 AC-4 | S2 | Only if triggered: verbatim error, version fallen back to, PO flag (D3 steps 4 and 5) |
| 015 AC-5 | S1 | `app/build.gradle:24` and merged manifest `android:minSdkVersion="24"`; comment `:153` corrected (D4) |
| 015 AC-6 | S1 | AC-6 grep returns the 12 lines of D4 only |
| 015 AC-7 | S1 | AC-7 grep returns zero; rename table T |
| 015 AC-8 | S1 | Baseline count 38 → N, each removed entry listed; `app/lint.xml` diff empty (D17) |
| 015 AC-9 | S1 (orchestrator) | Change record P-2, covering REQ-MODERNIZATION-001 and -006, raised before S1 |
| 015 AC-10 | S1 | `CHANGES` entry; location named (`CHANGES`) |
| 015 AC-11 | S2 (and S1: no diff) | D5 proof per increment; `git diff` shows no `<trust>` or `verify-metadata` change |
| 015 AC-12 | S1, S2 | G1 to G3 on JDK 25; `gradle-daemon-jvm.properties` diff empty |
| 015 AC-13 | S1, S2 | XML totals; count at start, plus additions (2 in S1), minus deletions (none), by name. The "697" baseline wording is ruled by P-2 before S1 |
| 015 AC-14 | S1, S2 | No `minimum` diff; per-package ratios; `receiver` above 50% (D8) |
| 015 AC-15 | S2 | `javap` 61 and 52; any forced change recorded |
| 015 AC-16 | S2 | `ci.yml` diff; AGP 9.5 minimum Build Tools 36.0.0 cited |
| 015 AC-17 | S2 (S1 smoke) | DV-3 (API 24) and DV-1 or DV-4 runs: launch, manual backup and restore, logcat excerpt. **Conditional on PO-2 (a, e) and PO-3** |
| 015 AC-18 | S2 | First CI run, each step's result. **Conditional on PO-4**; otherwise local CI-parity run and "not reached" |
| 016 AC-1 | S4 | Re-resolution table per coordinate |
| 016 AC-2 | S4 | Grep returns zero dependency lines (core-role leaves beta) |
| 016 AC-3 | S4 | `security-crypto` (deprecated, unchanged), JUnit 4.13.2 (unchanged), jutf7 1.0.0 (unchanged, only version published) |
| 016 AC-4 | S4 | D5 proof per increment; G1 enforced |
| 016 AC-5 | S4 | XML totals; any test rewritten for a coroutines-test change listed with its replacement |
| 016 AC-6 | S4 | G3 with no floor lowered; the "others at or above 70%" wording is ruled by P-3 before S4 (FD-VAL-002). **Conditional on P-3** |
| 016 AC-7 | S4 | Merged manifest before and after; `SystemForegroundService` `dataSync`; every difference justified |
| 016 AC-8 | S4 | `WorkManagerSchedulerTest` and the worker tests unmodified (diff empty) or justified per API change |
| 016 AC-9 | S4 | DV-1 role request with granted and denied outcomes. **Conditional on PO-3** |
| 016 AC-10 | S4 | Both halves: <br>• "still stop collecting when the activity is stopped": new `LifecycleCollectionStopTest` (S4-L0), passing on Lifecycle 2.6.2 and 2.11.1; <br>• "no-leak": the `MainViewModelTest` BUG-005 tests pass |
| 016 AC-11 | S4 | DV-1 preference screens open, show and persist values. **Conditional on PO-3** |
| 016 AC-12 | S4 | DV-1 install-over: LOGIN succeeds with no re-entry. **Conditional on PO-3** |
| 016 AC-13 | S4 | DV-1 manual backup, restore, and one scheduled run; logcat greps. **Conditional on PO-3** |
| 017 AC-1 | S5 | D12 record (qualifying and stale pages, dates) |
| 017 AC-2 | S3 (compile), S5 (target) | Grep shows 37 and 37 at S5 end. **The target half is conditional on the measurement and, if confirmed, BUG-021** |
| 017 AC-3 | S5 | D13 table completed; every row dispositioned with URL. Row 37-5 needs S5-I1 and, if the gap is confirmed, the OTP fix. **Conditional on PO-2 and the measurement** |
| 017 AC-4 | S5 | Rows AC4-a to AC4-e plus 36-1 (with 36-1a to 36-1d), 36-2, 37-5. **Conditional as AC-3** |
| 017 AC-5 | S5 | Commit history: fixes in the S5-I4 commit or earlier; the target-37 measurement build is never committed. **Conditional on the OTP fix landing first, if required** |
| 017 AC-6 | S5 | DV-2 launch and each preference screen. **Conditional on PO-2** |
| 017 AC-7 | S5 | DV-2 per-screen insets and back record. **Conditional on PO-2** |
| 017 AC-8 | S5 | DV-2 SMS, contacts, call-log and notification prompts, grant and deny. **Conditional on PO-2** |
| 017 AC-9 | S5 | DV-2 foreground worker, notification, LOGIN, append. **Conditional on PO-2 (b, d) and PO-3** |
| 017 AC-10 | S5 | DV-2 role hand-over and hand-back. **Conditional on PO-2** |
| 017 AC-11 | S5 | DV-2 `cmd jobscheduler run` and worker log. **Conditional on PO-2** |
| 017 AC-12 | S5 | DV-2 broadcast with integration on and off, setting restored. **Conditional on PO-2** |
| 017 AC-13 | S3 | Robolectric 4.18.1 supports 37; default stays 29 with the recorded reason; no `@Config` below 24 (D6) |
| 017 AC-14 | S3, S5 | Option (a) record (D17). **The S5 part is conditional as AC-2's target half** |
| 017 AC-15 | S3, S5 | G1 to G3; count; floors; verification. **The S5 part is conditional as AC-2's target half** |
| 017 AC-16 | S3, S5 | Release manifest diffs explained. **The S5 part is conditional as AC-2's target half** |

### Validation scores

The prompt's data-model, API and component categories have no subject in a build and platform
design. They were consciously judged not applicable, not skipped.

| Category | Score | Basis |
|---|---|---|
| AC traceability (designed) | 100% (47/47) | Every AC has a story and named evidence above |
| AC satisfiable without a condition | **51.1% (24/47)**, conditional | 23 ACs carry a condition (see the note below the table) |
| Integration coverage | 100% (8/8) | INT-003, INT-005 and INT-006 have test entries and INT-008 a fallback (added in the first audit revision; before it, 4/8) |
| Decision coverage of the brief's 10 topics | 100% (10/10) | Topic → decision: 1 → D1, D1a; 2 → D2; 3 → D3; 4 → D4, D4a, D4b; 5 → D5; 6 → D6 to D9; 7 → D10, D11; 8 → D12 to D16; 9 → D18; 10 → D19 and Risks |
| Consistency | Not scored | No internal contradiction is known after this revision: D1 is the only source of versions, and D14 is the only statement of the measurement. Cross-document differences are recorded in D14 S5-I1, which governs where they differ:
<br>• BUG-021's device procedure and its AC-12 expect `(1 messages)` at the positive control, where M1a expects `nothing to backup`;
<br>• REQ-MODERNIZATION-020's earlier draft read a count of 2 during the delay as "not affected", where D14 reads it as inconclusive. That draft was renumbered on 2026-11-21: its device criterion no longer contains the measurement and points to S5-I1.
<br>A percentage would overstate what was checked |

The 23 conditional ACs, by condition. Each AC is counted once.

| Condition | Count | ACs |
|---|---|---|
| Owner permission (PO-2, PO-3, PO-4) | 13 | 015 AC-17, AC-18; 016 AC-9, AC-11, AC-12, AC-13; 017 AC-6 to AC-12 |
| The OTP measurement and, if confirmed, BUG-021 (Record 13, row 4) | 7 | 017 AC-2 (target half), AC-3, AC-4, AC-5, and the S5 parts of AC-14, AC-15, AC-16 |
| Change record P-2 | 2 | 015 AC-9, AC-13 |
| Change record P-3 | 1 | 016 AC-6 |

**Verdict: CONDITIONAL.**
- *S1 to S4:* ready for implementation planning once these hold:
  - U-066 is closed;
  - preconditions P-1 to P-3 exist (P-1 and P-2 before S1, P-3 before S4);
  - BUG-022 is done before S2;
  - BUG-021 has landed before S4, or is explicitly scheduled after S4 under D2's strict-serial rule.
- *S5:* conditional on PO-2, PO-3 and the D14 measurement. If the gap is confirmed, S5 is also
  conditional on BUG-021.
- There are 0 critical findings. One HIGH finding (FD-VAL-001) stays open by design, and it gates
  S5-I4 only.

By the prompt's criteria, a HIGH finding rules out an unconditional PASS, so none is claimed.

### Findings

**FD-VAL-001: targetSdk 37 conflicts with "behaviour preserved"** (HIGH, Consistency).
- Evidence: REQ-017 Constraints; D14 (code verified, platform measurement pending).
- Remediation: ruling PO-1 (Record 13, row 4: fix first). BUG-021 has been raised; S5-I4 waits for
  the measurement and, if the gap is confirmed, for BUG-021.
- Effort: M for the fix.

**FD-VAL-002: REQ-016 AC-6 and epic success criterion 8 contradict the configured floors**
(MEDIUM, Consistency).
- Evidence: REQ-016 AC-6 and EPIC-MODERNIZATION-006 SC-8, "others at or above 70%", against the
  `violationRules` block `app/build.gradle:340-589`, where 13 of the 18 rules are below 0.70.
- Remediation: mandatory change record P-3, before S4. The design does not reinterpret the
  criteria.
- Effort: XS.

**FD-VAL-003: the 697 baseline in REQ-015 AC-13 and REQ-017 AC-15 is stale** (LOW, Consistency).
- Evidence: 732 at HEAD (A4).
- Remediation: mandatory change record P-2, before S1. Until it rules, stories record the count at
  start and every difference by name (as U-066).
- Effort: XS.

**FD-VAL-004: the research overstates the fallback triple** (MEDIUM, Consistency).
- Evidence: A17.
- Remediation: corrected in D3. F2 is described as "inside Kotlin's range", not "inside every
  range".
- Effort: XS.

**FD-VAL-005: device facts differ from the brief** (MEDIUM, Completeness).
- Evidence: A8. An API 37 AVD exists (`Pixel_10_Pro`), which is the emulator never to touch (A9).
  No API 24 image; no cmdline-tools.
- Remediation: PO-2.
- Effort: S.

**FD-VAL-006: latent no-op cancellation checks, F-1** (MEDIUM, outside scope).
- Evidence: A7.
- Remediation: BUG-022 raised, placed per D2 (after U-066, before S2); its tests are the stop rule
  in S2-I5, S4-C and S4-W.
- Effort: S.

**FD-VAL-007: REQ-015 AC-18 needs a pull request to `master`** (LOW, Completeness).
- Evidence: `ci.yml:3-9`; Record 12 limits.
- Remediation: PO-4.
- Effort: XS.

**FD-VAL-008: `CHANGES` carries a stale "Android minSdk now 14" line** (LOW).
- Evidence: `CHANGES` line 3.
- Remediation: replaced by the AC-10 entry in S1-I7.
- Effort: XS.

**FD-VAL-009: the CNTR-MODERNIZATION-004 change was mislabelled "editorial"** (MEDIUM,
Consistency).
- Evidence: CNTR-004 `:260` and DES-005 `:387` (see Integration Contracts).
- Remediation: mandatory change record P-1, before S1, with the argument written above.
- Effort: S.

### Remediation plan

| Finding | Action | Priority | Blocking? |
|---|---|---|---|
| FD-VAL-001 | Ruling PO-1 recorded (Record 13, row 4); BUG-021 raised | P0 | Blocks S5-I4 only |
| FD-VAL-009 | Raise P-1 | P0 | Blocks S1 |
| FD-VAL-003 | Raise P-2 | P0 | Blocks S1 |
| FD-VAL-002 | Raise P-3 | P1 | Blocks S4 |
| FD-VAL-005 | Ask PO-2 | P0 | Blocks S2 AC-17 (API 24 part), S5 device ACs |
| FD-VAL-007 | Ask PO-4 | P1 | Blocks AC-18 only |
| FD-VAL-006 | BUG-022 raised, placed per D2 | P1 | Before S2 |
| FD-VAL-004, -008 | Done in this design or S1 | P2 | No |

## Risks

Ranked by probability × impact.

| Rank | Risk | P / I | Mitigation | Detection |
|---|---|---|---|---|
| 1 | OTP SMS permanently missing from backups after targetSdk 37 (D14) | **Conditional:** High if any of the owner's devices runs Android 17, otherwise Low until one does / High (data completeness) | S5-I4 gated on the measurement and, if confirmed, BUG-021 (Record 13, row 4); one bug for both cases | D14 measurement, under its stop rule |
| 2 | Device validation impossible without the owner's permission (no API 24 image; the only API 37 AVD is the protected `Pixel_10_Pro`) | High / High (REQ-017 cannot close) | PO-2 asked now; D16 fallbacks state what they prove | — |
| 3 | The toolchain set fails outside the vendor ranges (G1 to G5) | Medium / High (effort) | Increment order isolates the component; F1 to F5 pre-specified | Gates, `--warning-mode all`, KSP file list |
| 4 | WorkManager 2.12 changes its manifest entry or foreground behaviour (the BUG-016 class); the database rollback may be irreversible on a device (unverified, A16) | Medium / High | Manifest invariant check; device run on DV-1 within the authorisation plus PO-3; rollback caveat D19 | AC-7 diff; AC-13 run |
| 5 | Coverage floor breach (receiver zero headroom; scheduler margin; the AGP 9.5 unit-test manifest) | Medium / Medium | `BackupBroadcastReceiverTest` first (S1-I2); per-increment measurement | G3 |
| 6 | Robolectric 4.18.1 changes the TLS test provider, `LooperMode` isolation, or needs JVM flags | Medium / Medium | D7 procedure; U-063 guards; tests never loosened | S2-I7 |
| 7 | The verification writer hides an unexpected artifact | Low / High (supply chain) | D5 single-path restore, one pass, additions-only proof, group review | D5 step 4 and 5 |
| 8 | `gradlew` Windows block lost on regeneration | Certain if forgotten / High (no build) | Checklist item in S2-I1 | "Unable to establish loopback connection" |
| 9 | Kotlin 2.4 annotation-target default turns the four U-060 warnings (for example `WorkManagerScheduler.kt:81`) into a Hilt qualifier issue | Low / Medium | Use explicit `@param:` site targets if forced; recorded as a forced change | S2-I5 compile, Hilt tests |
| 10 | Cancellation behaviour shifts when Kotlin, coroutines or WorkManager change the binding (F-1) | Low / Medium | BUG-022 lands before S2; its tests are the stop rule in S2-I5, S4-C, S4-W | BUG-022's cancellation tests; no Kotlin warning naming `coroutineContext` in either worker |
| 11 | Collision with U-066 | Low if sequenced / Medium | S1 and BUG-022 wait for U-066 to close | Sprint status |
| 12 | CI never runs (PO-4 declined, or the runner image is retired) | Medium / Low | Local CI-parity run; A18 check | — |
| 13 | A javac warning from a new toolchain, library or SDK fails `:app` under `-Werror` | Medium / Low | D3 rule: fix at the call site, never suppress | G1 |

## Open items for the product owner

Only real ones are listed: each changes scope or a recorded decision, costs compatibility on a
device or server in use, or needs the owner's machine, accounts or password.

1. **PO-1, targetSdk 37 and one-time-code messages (D14). A recorded ruling, not a question.**
   The orchestrator ruled on 2026-11-21 (decision appendix, Record 13, row 4, made under Record
   12): **fix first, then targetSdk 37**.
   - The targetSdk 37 change waits for the fix unless the S5 measurement shows the app picks such a
     message up after release.
   - BUG-021 has been raised. It covers the WebOTP all-targets case and the target-37 case, and
     implements REQ-MODERNIZATION-020.
   - The ruling keeps Record 4 and costs no device in use, so you are informed, not asked.

   *For your information:*
   - Does any of your devices run Android 17? If so, the WebOTP case may already leave such
     messages out of your backups today, whatever this work does.
   - BUG-021's decision-point defaults apply unless you overrule them (Record 14, row 10). The
     defaults are a bounded look-back of 6 hours; SMS only; a message hidden at Skip time is backed
     up; a duplicate is preferred to a loss.
2. **PO-2, emulators and passwords on your machine (D16).**
   - (a) May an API 24 system image be downloaded (about 1 GB) and a new virtual device created?
   - (b) May a new API 37 virtual device be created from the API 37 image already installed? No
     image download is needed, but creation needs Android Studio's Device Manager, or a
     cmdline-tools download.
   - (c) No question: `Pixel_10_Pro` is never touched, per your standing instruction (Record 14,
     row 2).
   - (d) On the new API 37 device, will you enter the mail-account app password yourself, so that
     the LOGIN, append and one-time-code measurement can run? Only you handle the password.
   - (e) The same on the new API 24 device: REQ-015 AC-17 needs a manual backup and restore to
     finish at API 24.
3. **PO-3, `Pixel_9_Pro` runs beyond your 2026-11-20 authorisation (S4, S5).**
   - *Already authorised:* starting `Pixel_9_Pro` and installing a debug build over the configured
     app ("Start it for me"); adding or deleting SMS on the emulator, and adding or removing the SMS
     folder in your mail account (Record 14, row 1).
   - *Asked now:*
     - manual and scheduled backups that append that emulator's messages to your account;
     - restore, which hands the default-SMS role over and back on the emulator;
     - toggling and then restoring the third-party-integration setting (REQ-017 AC-12);
     - the same kinds of run on the new API 37 and API 24 devices, including the measurement's
       appends at M0a, M0b, M1a, M3, M5 and in Runs T and R (D14).

     If S4 must be rolled back on a device, an app-data clear (and so re-entering the password) may
     be needed. Whether it is needed is checked before this question is put (A16).
4. **PO-4, first CI run (REQ-015 AC-18).** CI runs only for pull requests to `master`. May a pull
   request be *opened*, not merged, from `sdlc/modernization-plan` to `master` to trigger it? If
   not, AC-18 is recorded "not reached", with a local CI-parity run.
5. **PO-5, `security-crypto` is deprecated (D11). A recommendation, put to you by the
   orchestrator; it gates no story here.** Should a requirement be raised to replace the credential
   store's encryption library with direct Android Keystore use, behind the existing port? This
   needs a contract revision (CNTR-MODERNIZATION-003 mandates AES-256-SIV) and a re-encryption
   migration. It is not urgent; nothing breaks today.
6. **Local network (37-6), information only.** Ruled not applicable: the only server in use is
   Gmail. If you ever back up to a mail server on your local network, the app will need the new
   local-network permission at targetSdk 37.

**For the orchestrator. These are mandatory, not optional:**

| Item | Must precede |
|---|---|
| P-1: change record on CNTR-MODERNIZATION-004 and DES-MODERNIZATION-005 (pre-API-24 content-trigger fallback) | S1 planning |
| P-2: requirements-wording change record covering REQ-015 AC-13 "697"; the AC-9 items on REQ-MODERNIZATION-001 and -006; REQ-MODERNIZATION-001's literal "35" (AC-1, AC-29) | S1 planning |
| P-3: requirements-wording change record on REQ-016 AC-6 and epic SC-8 (coverage floors); may be part of P-2 | S4 planning |
| The OTP bug (WebOTP all-targets case and target-37 case in one): raised as BUG-021 (`sdlc/artifacts/stories/BUG-021-delayed-otp-sms-skipped-by-backup-watermark.md`) | After BUG-022. Before S4 if scheduled then, otherwise strictly serial with S4 (D2). In every case before S5-I4, unless S5-I1 is 'measured not affected'. |
| The F-1 bug: raised as BUG-022 (`sdlc/artifacts/stories/BUG-022-worker-cancellation-checks-do-nothing.md`) | After U-066 closes, before S2 (Record 14, row 11). |
| The jzlib currency item, carried into the migration design (D1) | Part B design |
| AC-4 acknowledgements, and the F2/F4 Part B flags | Only if a fallback fires |

## Trade-offs

1. **Newest versions against vendor-supported ranges.** Newest is chosen (AC-1). It is paid for by
   gaps G1 to G6 and a pre-specified fallback ladder. The alternative, F2 from the start, is still
   outside a literal reading and fails AC-1.
2. **compileSdk and targetSdk split (S3 and S5).** One more story, in exchange for decoupling a
   build-only change from a decision- and device-bound one. Rejected: one REQ-017 story, which would
   hold Part B prerequisites hostage to the OTP measurement and fix.
3. **Robolectric default kept at 29.** Fewer unattributed test changes and less download, in
   exchange for Robolectric not exercising the target level. Device runs carry that proof, and the
   S3 probe measures the cost of moving later.
4. **Lint baseline refresh (S3) rather than leaving lint red.** It hides 39 pre-existing errors in a
   reviewed baseline, with categories listed and three categories forbidden, in exchange for a green
   CI lint step that catches new regressions.
5. **Vendored module untouched.** Some dead code lives on until CR-009's move, in exchange for not
   touching characterised TLS code twice.
6. **Increment commits with per-increment verification.** More writer passes, in exchange for
   bisectable history (AD-6).

## Artifacts Consulted

| Artifact | Path | Purpose |
|---|---|---|
| REQ-MODERNIZATION-015 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-015-toolchain-and-test-tooling-latest-stable-minsdk-23.md` | Toolchain, test tooling, minSdk 24 (all ACs, amendment notes) |
| REQ-MODERNIZATION-016 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-016-runtime-libraries-latest-stable.md` | Runtime libraries (all ACs, notes) |
| REQ-MODERNIZATION-017 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-017-platform-level-current-stable-api.md` | Platform level 37 (all ACs, amendment) |
| EPIC-MODERNIZATION-006 | `sdlc/artifacts/requirements/modernization/EPIC-MODERNIZATION-006-dependency-currency-latest-stable.md` | Scope, success criteria, sequencing |
| CR-011 | `sdlc/artifacts/build/change-records/CR-011-req-015-minsdk-24-for-thunderbird-android-variants.md` | minSdk 24 impact, dead sites, "Not verified" |
| CR-010 | `sdlc/artifacts/build/change-records/CR-010-retained-mail-libraries-exception.md` | Retained mail libraries (out of scope here) |
| CR-009, CR-013, CR-014 | `sdlc/artifacts/build/change-records/CR-009-...`, `CR-013-...`, `CR-014-...` | TLS factory move and contract statements (D4b, D15) |
| Feasibility analysis, Records 3 to 12 | `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` (lines 2122-2285) | Product-owner decisions |
| Research 2026-11-21 | `sdlc/artifacts/build/research/2026-11-21-latest-stable-versions.md` | Versions, compatibility matrix, platform changes |
| DES-MODERNIZATION-013 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-013-donation-flow-removal-design.md` | D4 and D4a verification precedent |
| DES-MODERNIZATION-004 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-004-encrypted-credential-store-design.md` | Credential store and its reconsider trigger |
| DES-MODERNIZATION-005 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (`:238`, `:324-329`, `:387`) | Pre-24 fallback obligations (P-1) |
| DES-MODERNIZATION-006 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-006-dead-code-minsdk-cleanup-design.md` | Precedent for the minSdk dead-code removal |
| CR-014 Amendment 1 (HEAD `b9fd172f`) | `sdlc/artifacts/build/change-records/CR-014-cntr-001-state-protocol-floor-and-cipher-handling.md` | Allow-list from default-enabled suites; effect on S2-I7 |
| REQ-MODERNIZATION-001 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-001-restore-play-store-eligibility.md` (lines 47, 134) | Literal "35" for P-2 |
| BUG-021 | `sdlc/artifacts/stories/BUG-021-delayed-otp-sms-skipped-by-backup-watermark.md` (Steps to Reproduce (b); Suggested Approach §7; DP-1 to DP-5; AC-11, AC-12; Scope Concern 4) | The fix D14 defers to; the procedure folded into S5-I1 |
| BUG-022 | `sdlc/artifacts/stories/BUG-022-worker-cancellation-checks-do-nothing.md` (Suggested Approach: the one-message correction, timing, restated stop rule) | F-1, D2 placement, D10 stop rule |
| REQ-MODERNIZATION-020 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-020-backup-completeness-for-late-visible-messages.md` (draft; its device criterion) | The completeness rule BUG-021 implements; points to S5-I1 |
| Decision appendix, Records 13 and 14 | `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` (Record 13 rows 4, 5; Record 14 rows 1 to 4, 9 to 11) | Rulings and standing instructions cited throughout |
| Source checked in the second revision | `App.java` (`:258`, `:376-383`), `service/BackupWorker.kt` (`:262`, `:369`, `:423`), `activity/MainActivity.java` (`:312`, `:318`, `:329`, `:396-397`), `activity/MainActivityFlowHelper.kt` (`:20`), `activity/MainViewModel.kt` (`:67`); test `activity/MainViewModelTest.kt` (`:55`) | NB-1, NS-4, REQ-020 review |
| Source added in the revision | `activity/MainActivity.java` (`:393-402`), `mail/HeaderGenerator.java` (`:64-67`), `service/RestoreWorker.kt` (`:463-467`, `:321`, `:406`), `activity/auth/OAuth2WebAuthFlowHelper.kt`, `activity/MainActivityFlowHelper.kt`, `activity/auth/*Activity.java`, `compat/ComposeSmsActivity.java` (under `app/src/main/java/com/zegoggles/smssync/`) | B-1, SF-1, SF-3, SF-5, SF-6 |
| U-060 story and workspace | `sdlc/artifacts/build/sprints/sprint-017/U-060-jdk-25-build-toolchain-upgrade.md`; `sprint-017/U-060/implementation-log.md`; `U-060/plan.md` (Step 6); `sprint-017/retro.md` | Toolchain precedent and traps |
| U-063 story | `sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md` | TLS test provider notes |
| U-066 story | `sdlc/artifacts/build/sprints/sprint-022/U-066-prune-weak-tls-cipher-suites.md` | In-flight collision (AC-11) |
| CNTR-MODERNIZATION-001 to -008 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-00{1..8}-*.md` | Contract impact (status lines; CNTR-003 versioning; CNTR-004 INV-4) |
| Build files | `build.gradle`, `app/build.gradle`, `k9mail-vendored/build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/gradle-daemon-jvm.properties`, `gradlew` | Current versions and settings |
| Verification metadata | `gradle/verification-metadata.xml` | Configuration block, counts |
| Manifest, lint, R8 | `app/src/main/AndroidManifest.xml`, `app/lint.xml`, `app/lint-baseline.xml`, `app/proguard-rules.pro`, `app/src/main/res/xml/network_security_config.xml` | Platform surface |
| CI | `.github/workflows/ci.yml` | Triggers, actions, Build Tools |
| Robolectric config | `app/src/test/resources/robolectric.properties` | Default SDK |
| Source, SDK sites | `activity/Dialogs.java`, `activity/MainActivity.java`, `activity/SmsDefaultRoleHelper.kt`, `calendar/CalendarAccessorPost40.java`, `compat/SmsReceiver.java`, `scheduler/WorkManagerScheduler.kt`, `App.java`, `activity/ThemeActivity.java` (under `app/src/main/java/com/zegoggles/smssync/`) | D4 |
| Source, backup path | `service/BackupWorker.kt`, `service/BackupItemsFetcher.java`, `service/BackupQueryBuilder.java`, `preferences/DataTypePreferences.java`, `mail/ConversionResult.java`, `mail/MessageGenerator.java`, `mail/Headers.java`, `receiver/SmsBroadcastReceiver.java`, `receiver/BackupBroadcastReceiver.java` | D14, D8 |
| Source, TLS | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`, `.../TrustManagerFactory.java`, `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` | D15, D4b |
| Source, contacts | `contacts/ContactAccessor.java`, `mail/PersonLookup.java` | 37-8, 37-9 |
| Tests | `compat/SmsReceiverTest.java`, `service/WorkManagerSchedulerTest.kt`, `service/BackupWorkerTest.kt`, `mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java` (under `app/src/test/java/com/zegoggles/smssync/`) | D4 table T, D7, F-1 |
| Release notes (fetched 2026-11-21) | WorkManager, Lifecycle, kotlinx.coroutines `CHANGES.md`, Preference, Core (core-role), Security pages; GitHub releases of actions/checkout, setup-java, cache | D10, D11, D1 |
| Local environment (read-only) | SDK folder listing, AVD `.ini` files, `emulator -list-avds`, built APK listings | A8, 16-3 |

## Notes

- Paths written in this design are repository-relative. Line numbers were read at HEAD `fba733e5`
  on 2026-11-21. The revision was checked at `b9fd172f`, which changes only CR-014. Each story
  re-reads the lines before editing.
- Story IDs are not assigned here. S1 to S5 are working names for `/amp:create-stories`, which takes
  its acceptance criteria from the decisions above.
- Nothing in this design was built or run: no Gradle command, no emulator.

### Audit response (Design Auditor, first audit; revision of 2026-11-21)

| Finding | Action taken | Where |
|---|---|---|
| B-1 | Verified `MainActivity.java:393`, `:401-402` (missing-credentials dialog). Added positive control M1a; the shell content query is limited to the date question; the no-account fallback is withdrawn; "measured not affected" now requires M1a plus an app-side count; an inconclusive run or no account leaves 37-5 applicable and S5-I4 waits | D14 "Conditions for every run", table, stop rule; S5 preconditions; row 37-5 |
| SF-1 | Verified. Citation corrected to `HeaderGenerator.java:67`; added the restore (`RestoreWorker.kt:463-467`) and skip (`BackupWorker.kt:198-217`) watermark paths; option (a) must ledger restored `_id`s and respect a skip floor | A5; D14 "What the code does", option (a) |
| SF-2 | Target-37 measurement build is an uncommitted scratch change; MMS and call-log backup off; PO-1 timing per the ruling | D14; S5 preconditions; D2 diagram and preconditions; Overview item 1 |
| SF-3 | Verified `setProgress` at `BackupWorker.kt:361`, `RestoreWorker.kt:321`, `:406`. Bound stated (one batch or one item); its cancellability marked not verifiable here (A20); stop rule in S2-I5, S4-C, S4-W | D10 F-1; S2-I5; S4; Overview item 4; Risk 10; FD-VAL-006 |
| SF-4 | No reinterpretation. AC-13 "697" goes to P-2 before S1; REQ-016 AC-6 and SC-8 go to P-3 before S4; SC-8 cited | Context; D8 item 5; traceability; FD-VAL-002, -003; orchestrator table |
| SF-5 | Decided: a Robolectric test `LifecycleCollectionStopTest` (S4-L0) drives a `LifecycleRegistry` owner to stopped through `OAuth2WebAuthFlowHelper`; no new dependency. A device check was rejected as not observable from text | D2 S4; traceability 016 AC-10 |
| SF-6 | Added rows 16-1a (`#abandoned-job-stop-reason`), 16-1b (`#three-button-predictive-back`), 16-1c (`#improved-bond-loss-handling`), 37-6a (`#safer-dcl`); 36-1a to 36-1d for the four other activities (verified: none sets a content view; `ComposeSmsActivity` is empty); 37-6 is NA per the ruling | D13 |
| SF-7 | Verified CNTR-004 `:38`, `:64`, `:170`, `:240`, `:260`, `:381-383` and DES-005 `:238`, `:324-329`, `:387`. Relabelled as non-editorial; the argument is written; the record is precondition P-1 of S1; DES-005 is cited in the body | Integration Contracts; D2 preconditions; FD-VAL-009 |
| SF-8 | Verified REQ-001 AC-1 (line 47) and AC-29 (line 134). Added to P-2 | D2 preconditions; orchestrator table |
| SF-9 | Added PO-2(e), the password on the API 24 device; the `Pixel_9_Pro` install-overs folded into PO-3 with the named authorisation | Context; D16; DV-1 and DV-3 rows; PO-2, PO-3 |
| SF-10 | Verified `app/build.gradle:192-203`. Added the `-Werror` rule (record verbatim, fix at the call site, never remove `-Werror` or suppress) | D3 step 3; D10; D12; D11; Risk 13 |
| SF-11 | Added the F2 Part B flag (AGP 9.3.1 is below Thunderbird `main`'s 9.5.1) | D3 F2 row |
| SF-12 | Row counts recomputed from the table (S2 19, S4 8, S3 2, S1 1, S5 1); S4 records JUnit's status without owning it | D1 Consequences |
| SF-13 | Verdict is now CONDITIONAL; integration coverage restored to 8/8 by adding the missing entries (it was 4/8); the conditional count is 17/47, including the 3 ACs gated by ruling PO-1 and the 1 by P-3; JaCoCo ranges unified on `:340-589` | Validation scores; INT-003, -005, -006, -008 rows; D8; FD-VAL-002 |
| SF-14 | Prune method named (scratch regeneration, diff, removals only, count confirmation); noted that S-6 has no entry of its own and the stale entry is `MainActivity`'s | D17; S1-I6 |
| N-1 | Annotated that this design's Kotlin-range reading is the supported one, and that U-060's log shows no fetch date for the Gradle 9.7 page | D3 Context; A17 |
| N-2 | DES-005 and DES-006 are now cited in the body (Integration Contracts; Patterns); CR-009 added to `change_records` | Frontmatter; System Architecture; Integration Contracts |
| N-3 | Removed the Gradle Plugin Portal; the repositories named are `google()`, `mavenCentral()`, `maven.google.com` | INT-001 |
| N-4 | Quote corrected to "deprecated without a successor" (`:208-209`); PO-5 now mentions CNTR-003's AES-256-SIV mandate | D11; PO-5 |
| N-5 | HEAD `b9fd172f` noted; the effect of CR-014 Amendment 1 on S2-I7 and U-066's exact-list tests stated; U-066 must close before S1 | Overview; Context "In-flight work"; S2-I7; Notes |
| N-6 | Pre-specified moving S3 to just after S2-I3 if a test-tooling dependency needs a compileSdk above 35 (A21) | D2 S3 |
| N-7 | Reworded to "Robolectric 4.18 needs no test below SDK 24" | Overview; System Architecture; D2 Context; S2-I7 |
| N-8 | Page dates added; one line that the Google Play note does not apply to a private app | D13 |
| N-9 | Rollback cost softened to "possible", and checked before PO-3 states it | A16; D10; D19 S4 row; PO-3; Risk 4 |
| N-10 | "Test-source" claim withdrawn; rule IDs recorded at S1-I0 (A22); `OldTargetApi` and `GradleDependency` expected in S3 | D17 |
| N-11 | The rewritten `SmsReceiverTest` comment contains neither "LOLLIPOP" nor `sdk = [2x` | S1-I3 |
| N-12 | Serial found with `adb devices`, then confirmed with `emu avd name`; first-run prompts recorded before the owner configures the account | D16 constraints |
| N-13 | Risk 1's likelihood ranked conditionally on an Android 17 device in use | Risks |
| Unverifiable items | One line each: `setProgress` cancellability (A20), WorkManager downgrade (A16), `androidx.test:monitor` minimum compileSdk (A21), lint rule IDs (A22) | Assumption Ledger |

Nothing in the first audit was kept against. One addition beyond the audit: FD-VAL-009 records the
CNTR-004 relabelling as its own finding, so that the remediation table shows P-1 blocking S1.

**Second audit (Design Auditor), and the REQ-MODERNIZATION-020 review.** The SF-1 row above is
superseded by NS-1: the mechanism detail it added was removed.

| Finding | Action taken | Where |
|---|---|---|
| NB-1 | Auditor's stop rule applied as sent, after verifying `MainActivity.java:396-397` (FIRST_SYNC), `BackupWorker.kt:262` ("nothing to backup") and `:423` (watermark line). `address` added to the shell projection; preliminary completed backup M0a added before M1; the 17-2 WebOTP-format first step kept. One procedure, stated once in D14 | D14 S5-I1 |
| NS-1 | Classification items 2 and 3 replaced with the auditor's text. The restore sentence replaced. Mechanism detail (ledger, `_id` key, restore ids, Skip floor, "at least three hours", `X-smssync-id` search) removed from D14 and PO-1 | D14; PO-1 |
| NS-2 | Rows 37-5 and 17-2 replaced with the auditor's dispositions | D13 |
| NS-3 | Placement paragraph added, adjusted to Record 14 row 11: BUG-022 before S2, not S4. BUG-022's one-message correction applied to D10. Its restated stop rule (BUG-022 tests pass unmodified; no Kotlin warning naming `coroutineContext` in either worker) applied in S2-I5, S4-C, S4-W, without line numbers. The orchestrator may schedule the BUG-021 fix before the measurement, as its own story after BUG-022. Diagram and FD-VAL-001/-006 remediation updated | D2; D10; S2-I5; FD-VAL; Risks 10, 11 |
| NS-4 | Auditor's test text applied after verifying `MainActivity.java:312`, `:318`, `:329`, `MainActivityFlowHelper.startCollection` (`:20`), `MainViewModel.events` (`:67`, a `SharedFlow`) and `MainViewModelTest:55`. L0 added to S4's order table | D2 S4 |
| NS-5 | D14 append sentence replaced as given; PO-3 added to S5's preconditions; Record 14 cited at every standing-instruction and `Pixel_10_Pro` mention | D14; S5; D16; Context; PO-2, PO-3; A9 |
| NS-6 | S1's preconditions stated in S1; P-1 to P-3 in the Overview; "BUG-021 has been raised"; remediation rows reworded | Overview item 5; D2; Remediation plan |
| NS-7 | Rulings cited to Record 13 rows 4 and 5 (made under Record 12), `Pixel_10_Pro` to Record 14 row 2, P-1 to P-3 to Record 14 row 9 | Throughout |
| NS-8 | D17's "Rejected" bullet replaced as given | D17 |
| N-a | Recounted: 23 conditional ACs, 24/47 (51.1%) without a condition, now including 017 AC-2 (target half) and the S5 parts of AC-14 to AC-16, and P-2's two ACs. Consistency no longer scored as 100%; the one cross-document difference is named | Validation scores |
| N-b | The S3 move triggers on any failure caused by a too-low compileSdk; S3-I3 stays after S2-I7 | D2 S3 |
| N-c | "Four things"; the bug removed from the parallel list; Risk 13 moved to the end; FD-VAL-009 moved after FD-VAL-008 | D2 Parallelism; Risks; Findings |
| N-d | `related_stories` now lists BUG-021 and BUG-022: the schema types the field as an array of story ids, and bug files are story-type artifacts in `sdlc/artifacts/stories`. REQ-MODERNIZATION-020 cited in the body. `type` left empty: the schema's only non-empty value is `pattern`, which does not apply to a design of this kind | Frontmatter; D14; Artifacts Consulted |
| N-g | Row 36-1c: A-NC, with "not reached" recorded as the evidence when the dialog cannot be reached | D13 |
| N-h | PO-1's information note says BUG-021's decision-point defaults apply unless the owner overrules them (Record 14, row 10) | PO-1 |
| REQ-020 review: single owner | S5-I1 is the one complete measurement; REQ-MODERNIZATION-020 and BUG-021 point to it | D14 |
| REQ-020 review: preconditions | Emulator under PO-2; auto backup OFF during counts (verified `App.java:258`, `:376-383`); the watermark exists (M0a) | D14 Preconditions |
| REQ-020 review: counting control | M0b, before the code message is sent | D14 Run A |
| REQ-020 review: count of 2 | Inconclusive, never "not affected"; may be repeated once with another form, and the form is recorded | D14 outcomes |
| REQ-020 review: "nothing to back up" after release | "Gap not ruled out" unless Run T shows visibility by M5's time (then "gap confirmed"); S5-I4 waits either way. Run T checks every 30 minutes up to T0 + 6 h and records the time it appears | D14 Run T, outcomes |
| REQ-020 review: trigger at release | Run R with auto backup ON, reading only the `BackupTriggerWorker` lines (CR-017); "not measured" if not run | D14 Run R |
| REQ-020 review: limits | Shell is not the app; emulator not device; one run; WebOTP text from the documentation; trigger "not measured" if Run R absent | D14 Limits |
| REQ-020 review: outcomes and sequencing | Only "picked up after release with a passing positive control" lets S5-I4 go without the fix; any other outcome or "not run" waits. Fixed-build check belongs to the fix story: `backup complete, backedUp=1` (`BackupWorker.kt:369`), then two `nothing to backup` | D14 outcomes table; "After the fix" |
| REQ-020 review: citation | "Ruling PO-1 (Record 12)" replaced by Record 13, row 4 | D14; PO-1; INT-008; FD-VAL-001 |

**One addition beyond both audits.** Run T exists because, on the unfixed build, the watermark hides
the release from the app once message B has been backed up. The reviewer's "repeat every 30
minutes" can only observe the release time in a run without message B. This is stated in D14.

**Kept against a finding: none.**

**Third audit (Design Auditor: APPROVE).**

| Finding | Action taken | Where |
|---|---|---|
| SF3-1 | "Measured not affected" When-cell replaced with the given text; Limits gained "Run T's release time is taken to apply to Run A; the record says so" | D14 outcomes; Limits |
| SF3-2 | Sequencing cells of "Gap not ruled out", both "Inconclusive" rows and "Not run" replaced with the given text. The same text was also applied to "Gap confirmed", so that every non-"not affected" outcome gives one answer. Rows 37-5 and 17-2 replaced as given | D14; D13 |
| SF3-3 | S2 precondition added; S4 precondition line replaced; orchestrator-table rows for BUG-022 and BUG-021 replaced as given | D2 S2, S4; Open items |
| SF3-4 | The "point to it" sentence replaced as given. Consistency row cites the differences by content, not criterion number. The REQ-020 entry in Artifacts Consulted cites "its device criterion". `backedUp=0` accepted beside `nothing to backup` in "After the fix" | D14; Validation scores; Artifacts Consulted |
| N3-a | Operator practicalities added. Verified: `BackupBroadcastReceiver` is exported with action `com.zegoggles.smssync.BACKUP` (`AndroidManifest.xml:171-178`) and, with third-party integration on, schedules an immediate backup through the same worker (`BackupBroadcastReceiver.java:49-55`). So later backups are started by `adb shell am broadcast`; M0a still needs a tap, because of the first-backup dialog. Also added: device clock at every step, host must not sleep, about 12 h per case and 24 h for both, scheduled re-entry | D14 Operator practicalities |
| N3-b | `dumpsys jobscheduler` check after auto backup is turned off | D14 Operator practicalities |
| N3-c | Verdict readiness line names U-066 closed, BUG-022 before S2, and BUG-021's placement | Validation scores, Verdict |
| N3-d | BUG-021's arrow drawn to S4 in the D2 diagram | D2 |

**For the orchestrator.** BUG-021's Steps to Reproduce (b) and AC-12 should point to D14 S5-I1
rather than restate it; their positive control expects `(1 messages)`, where S5-I1's M1a expects
`nothing to backup`. REQ-MODERNIZATION-020's device criterion already points to S5-I1, per its
renumbering of 2026-11-21.
