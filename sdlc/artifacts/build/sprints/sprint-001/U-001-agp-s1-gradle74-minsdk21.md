---
type: story
status: done
sprint: "000001"
artifact_type: user-story
priority: critical
complexity: medium
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-001
design_docs:
  - DES-MODERNIZATION-001
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags:
  - build-toolchain
  - agp-migration
  - stage-s1
gate_additions: []
id: U-001
title: 'AGP S1: Bump AGP 4.1.3 to 7.4.x, Gradle wrapper to 7.5.x, and raise minSdkVersion to 21 with a committed lint baseline'
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-001: AGP S1 — Bump AGP 4.1.3 to 7.4.x, Gradle wrapper to 7.5.x, and raise minSdkVersion to 21 with a committed lint baseline

## Story

As a maintainer shipping updates to users through Google Play,
I want the project to compile successfully under AGP 7.4.x with Gradle 7.5.x and minSdkVersion 21, with all AGP-7 DSL breakages resolved and a lint baseline capturing pre-existing warnings,
so that the build toolchain clears the first independently-verifiable stage of the three-stage AGP migration, unblocking Stage S2 without conflating toolchain failures with behavioral failures.

## Acceptance Criteria

- [ ] AC-1: `build.gradle` classpath declares `com.android.tools.build:gradle:7.4.*` (where `*` is the latest 7.4.x patch at time of implementation). Verified by `grep "com.android.tools.build:gradle" build.gradle` returning only a 7.4.x coordinate; no 4.x coordinate remains in any build file.
- [ ] AC-2: `gradle/wrapper/gradle-wrapper.properties` `distributionUrl` references a Gradle 7.5.x distribution (minimum `7.5.0`, or latest 7.5 patch). Verified by static inspection of `distributionUrl`; no value below 7.3.3 may remain (AGP 7.4 requires Gradle >= 7.3.3; 7.5.x provides headroom for S2).
- [ ] AC-3: `app/build.gradle` declares `minSdkVersion 21` (or the equivalent `minSdk 21` in the Kotlin DSL). Verified by `grep "minSdk" app/build.gradle` returning only `21`; no reference to `14` or any value below `21` remains in any build file.
- [ ] AC-4: compileSdkVersion and targetSdkVersion remain at `29` in this commit. Verified by `grep "compileSdkVersion\|targetSdkVersion" app/build.gradle` returning only `29`; raising the SDK level is explicitly out of scope for S1.
- [ ] AC-5: All AGP-7 DSL migration points are resolved such that `./gradlew assembleRelease` exits 0 on a clean checkout. The three required DSL changes are:
  - `lintOptions { }` block renamed to `lint { }` in `app/build.gradle` (AGP 7.0+ removed the `lintOptions` DSL element).
  - `package` attribute in `AndroidManifest.xml` removed (or confirmed absent) and replaced by `namespace` in `app/build.gradle` (AGP 7.3+ requires `namespace`; AGP 7.4 emits a hard error if `package` attribute remains in the manifest when `namespace` is set in the build file).
  - Any removed variant-API usage (e.g., `variant.getMergedFlavor()`, `applicationVariants.all { variant -> ... }` with removed members) is resolved to the AGP 7.4 stable variant API. Verified by CI log showing `assembleRelease` exit code 0 with no `Deprecated Gradle features` build errors.
- [ ] AC-6: A `lint-baseline.xml` file is committed to the repository root (or `app/lint-baseline.xml` as required by the `lint { baseline }` configuration in `app/build.gradle`). The baseline is generated from the S1 build state — it captures all pre-existing lint warnings present before this migration and must not be generated from a post-migration clean slate. Verified by presence of the file in the commit tree and by `./gradlew lint` exiting 0 with the baseline active.
- [ ] AC-7: `./gradlew lint` exits 0 with the baseline active (pre-existing warnings are suppressed by the baseline; no new warnings introduced by the AGP-7 migration are present). Verified by CI lint report.
- [ ] AC-8: The `ExportedReceiver` lint category does not produce zero-baseline entries that would require a `tools:ignore` suppression added during this stage. If AGP 7.4's manifest merger emits `ExportedReceiver` errors for the six intent-filtered receivers (which currently lack `android:exported`), those receivers must have explicit `android:exported` attributes added in this commit to satisfy the hard manifest-merger constraint rather than being suppressed in the baseline. Verified by `./gradlew assembleRelease` exiting 0 and by `grep "ExportedReceiver" app/lint-baseline.xml` returning no output (no suppression in baseline).

  > **Note on receiver `exported` scope:** DES-MODERNIZATION-001 §Manifest Conformance documents that absence of `android:exported` on an intent-filtered receiver is a **hard manifest-merger error under AGP 7+**, not merely a lint warning. If the AGP 7.4 merger enforces this as a build error in S1, all six receivers must be addressed here with their correct values: `SmsBroadcastReceiver=false`, `BootReceiver=false`, `PackageReplacedReceiver=false`, `BackupBroadcastReceiver=true`, `.compat.SmsReceiver=true`, `.compat.MmsReceiver=true`. The developer must attempt `assembleRelease` after the AGP bump and inspect whether the merger errors; if it does, the receiver fix is pulled forward to this stage. The full semantic specification for each receiver's exported value is owned by U-003 (S3); if pulled forward here, U-003 must not re-apply those changes. This criterion is therefore conditional: it is satisfied if (a) the merger does not error and the baseline contains no `ExportedReceiver` suppressions, OR (b) the merger errors and all six receivers are corrected with their U-003-specified values.

- [ ] AC-9: `./gradlew assembleRelease` succeeds and the resulting APK/AAB is produced. Verified by presence of a non-zero-size output artifact under `app/build/outputs/`.
- [ ] AC-10: This commit is independently compilable — a developer checking out only this commit (without S2 or S3) can run `./gradlew assembleRelease` and get exit 0. Verified by PR commit history showing S1 as a discrete, self-contained commit and by a clean build on that commit alone.
- [ ] AC-11: `build.gradle` does not reference `jcenter()` or `jcenter.bintray.com` as a result of this stage's changes. The S1 commit must not introduce any new JCenter references; existing ones (from pre-migration) remain intact at this stage as they are removed in S3. Verified by diff review confirming no new JCenter entries added.

### Integration Criteria

Not applicable. This story introduces no new runtime components, no new public interfaces, and no new cross-component boundaries. It is a build-toolchain configuration change only.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `build.gradle` | Declares AGP `com.android.tools.build:gradle:4.1.3` in the `buildscript` classpath | Bump to `com.android.tools.build:gradle:7.4.x` |
| `gradle/wrapper/gradle-wrapper.properties` | `distributionUrl` points to `gradle-7.2-bin.zip` | Change to `gradle-7.5.x-bin.zip` (7.5.0 or latest 7.5 patch) |
| `app/build.gradle` | Declares `minSdkVersion 14`; uses `lintOptions { }` DSL block; no `namespace` declaration | Change `minSdkVersion` to `21`; rename `lintOptions { }` to `lint { }`; add `namespace "com.zegoggles.smssync"` and verify `package` attribute is absent from `AndroidManifest.xml` |
| `app/src/main/AndroidManifest.xml` | May contain `package` attribute at root `<manifest>` element; six intent-filtered receivers lack `android:exported` (these become a hard merger error under AGP 7+ and may need to be resolved here — see AC-8) | Remove `package` attribute if present (namespace moves to `app/build.gradle`); conditionally add `android:exported` to all six intent-filtered receivers if the AGP 7.4 merger treats absence as a hard error |
| `app/lint-baseline.xml` (new file) | Does not exist | Created by `./gradlew lint --baseline` from the S1 build state; captures pre-existing warnings |

## Existing Behavior to Preserve

- `./gradlew assembleDebug` and `./gradlew assembleRelease` produce valid APK/AAB artifacts.
- `./gradlew test` continues to execute (Robolectric test suite must not be broken by the AGP bump or minSdk raise; Robolectric 4.3.1 remains at this stage — its upgrade is owned by S3/DES-MODERNIZATION-003).
- Application ID `com.zegoggles.smssync` remains unchanged throughout the namespace migration (the `namespace` value in `app/build.gradle` must match the former `package` attribute exactly).
- Signing configuration in `app/build.gradle` (lines 3–7, 35) is preserved without modification.
- compileSdkVersion and targetSdkVersion remain at `29` — the SDK raise is S3's scope.
- All existing runtime behavior is untouched: the AGP/Gradle bump is a build-system change only; no application source files are modified unless the receiver `exported` pull-forward is triggered (see AC-8).
- The `firebase-jobdispatcher:0.8.6` dependency continues to resolve via the `https://maven.scijava.org/` mirror declared in `build.gradle:26–27`; this mirror must not be removed during the AGP bump even if a developer is also cleaning up other repository entries.

## Verification Steps

1. **AC-1 — AGP version:** Run `grep "com.android.tools.build:gradle" build.gradle`. Confirm the output shows a `7.4.x` coordinate and no `4.x` line exists anywhere in the build files (`grep -r "com.android.tools.build:gradle" .`).
2. **AC-2 — Gradle wrapper version:** Open `gradle/wrapper/gradle-wrapper.properties`. Confirm `distributionUrl` ends in `gradle-7.5.x-bin.zip` where x >= 0.
3. **AC-3 — minSdk:** Run `grep "minSdk" app/build.gradle`. Confirm only `21` appears; no `14` remains.
4. **AC-4 — SDK held at 29:** Run `grep "compileSdkVersion\|targetSdkVersion\|compileSdk\|targetSdk" app/build.gradle`. Confirm all values are `29`.
5. **AC-5 — DSL migration:** On a clean checkout of the S1 commit, run `./gradlew assembleRelease`. Inspect the output for any `ERROR` or `FAILED` entries. Confirm the build exits 0. Specifically verify: (a) no `lintOptions` reference in `app/build.gradle` (`grep "lintOptions" app/build.gradle` returns empty); (b) `lint { }` block exists in `app/build.gradle`; (c) `namespace "com.zegoggles.smssync"` is declared in `app/build.gradle`; (d) `package` attribute is absent from the root `<manifest>` element in `app/src/main/AndroidManifest.xml` (`grep "package=" app/src/main/AndroidManifest.xml` returns empty or only returns non-root-element occurrences).
6. **AC-6 — lint baseline committed:** Run `git show HEAD --stat | grep lint-baseline.xml`. Confirm the file appears in the commit. Alternatively inspect the repo for `app/lint-baseline.xml` or `lint-baseline.xml` at the repo root.
7. **AC-7 — lint passes with baseline:** Run `./gradlew lint`. Confirm exit 0. Inspect the lint report in `app/build/reports/lint-results-*.html` for zero new violations outside the baseline.
8. **AC-8 — no ExportedReceiver in baseline:** Run `grep "ExportedReceiver" app/lint-baseline.xml` (or the baseline path configured in `app/build.gradle`). Confirm no output. If output exists, verify the six receivers have explicit `android:exported` in `AndroidManifest.xml` with the correct values from AC-8's receiver table.
9. **AC-9 — release artifact:** After `./gradlew assembleRelease`, confirm presence of a non-zero-size file under `app/build/outputs/apk/release/` or `app/build/outputs/bundle/release/`.
10. **AC-10 — independent compilability:** Check out the S1 commit in isolation (no S2/S3 commits). Run `./gradlew assembleRelease`. Confirm exit 0. This is the key staging invariant.
11. **AC-11 — no new JCenter:** Run `git diff HEAD~1 HEAD -- build.gradle app/build.gradle settings.gradle gradle/` and confirm no lines beginning with `+` introduce `jcenter` or `jcenter.bintray.com`.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / Gradle | All changes — build file modifications, wrapper bump, DSL migration, lint baseline generation | Developer |

## Technical Context

**Why S1 stops at AGP 7.4.x and Gradle 7.5.x (not 8.x):** AGP 8 requires JDK 17 and Gradle 8+. Bundling those constraints into S1 would create a three-way entangled failure surface (DSL breakages + JDK toolchain + new variant API). S1 isolates the 4.x → 7.x DSL break; S2 isolates the 7.x → 8.x JDK/toolchain break; S3 isolates the SDK behavioral raise. Each stage has a single, diagnosable failure domain.

**Why minSdk 21 is raised here (not at S3):** minSdk 14→21 is orthogonal to both the AGP-8 jump and the targetSdk raise. Raising it at S1 isolates pre-21 compat-surface fallout from SDK-raise fallout. The stranded code (`CalendarAccessorPre40`, `CalendarAccessor.Get` branch, `ServiceBase.isConnectedViaWifi_pre_SDK21` at lines 216–222) becomes dead but is not deleted here — that is MU-011's scope (REQ-MODERNIZATION-001 Constraint 3).

**lint-baseline.xml strategy (REQ-MODERNIZATION-001 Constraint 2):** `warningsAsErrors true` (`app/build.gradle:41`) and `-Werror -Xlint:deprecation` (`app/build.gradle:75`) will fail the build the moment a new deprecation surfaces. The baseline captures the pre-existing warning set at S1 so the build does not fail on already-known issues. The policy is strictly shrink-only: each subsequent stage (S2, S3) may only remove baseline entries, never add. A full `-Werror` run with an empty baseline must be restored before REQ-MODERNIZATION-001 is closed.

**Receiver exported pull-forward risk:** DES-MODERNIZATION-001 §Manifest Conformance documents that missing `android:exported` on an intent-filtered receiver is a hard manifest-merger error under AGP 7+. The developer must test `assembleRelease` immediately after the AGP bump and determine whether the merger enforces this as a hard error in 7.4.x or only as a lint warning. If it is a hard error, all six receivers must be resolved in this commit. The correct values are: `SmsBroadcastReceiver=false`, `BootReceiver=false`, `PackageReplacedReceiver=false`, `BackupBroadcastReceiver=true` (public Tasker/automation contract — must NOT be set false), `.compat.SmsReceiver=true` (system `SMS_DELIVER`, must be reachable by the platform), `.compat.MmsReceiver=true` (system `WAP_PUSH_DELIVER`, must be reachable by the platform).

**Key file locations (all paths relative to repo root):**

| File | Lines of interest |
|------|------------------|
| `build.gradle` | `:8` AGP version (currently `4.1.3`); `:4,:21` jcenter (do not touch in S1); `:26–27` scijava mirror (must NOT be removed) |
| `app/build.gradle` | `:14` minSdkVersion (currently `14`); `:41` warningsAsErrors; `:75` -Werror; lint DSL block location |
| `gradle/wrapper/gradle-wrapper.properties` | `distributionUrl` (currently `gradle-7.2-bin.zip`) |
| `app/src/main/AndroidManifest.xml` | Root `<manifest>` element (check for `package` attribute); `:131` SmsBroadcastReceiver; `:139` BootReceiver; `:145` PackageReplacedReceiver; `:151` BackupBroadcastReceiver (has `tools:ignore="ExportedReceiver"`); `:159` .compat.SmsReceiver; `:167` .compat.MmsReceiver |

## Supporting Documentation

- `REQ-MODERNIZATION-001` §SDK and Build Toolchain Uplift (AC-1 through AC-6), §Constraints (Constraints 2, 3, 4)
- `DES-MODERNIZATION-001` §Staged Toolchain Uplift — Three Independently-Buildable AGP Stages (S1 row), §Design (lint-baseline approach), ADR-MOD-001-A

## Integration Contract References

None. This story operates entirely within the Gradle/AGP build toolchain configuration surface and introduces no runtime component boundaries.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Story scope is S1 only (AGP 4.1.3 → 7.4.x, Gradle 7.2 → 7.5.x, minSdk 14 → 21, lint baseline committed, SDK held at 29); assembleRelease must exit 0 on this commit independently before S2 is started.
