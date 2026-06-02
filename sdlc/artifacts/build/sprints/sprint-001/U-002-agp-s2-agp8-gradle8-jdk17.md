---
type: story
status: planned
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
dependencies:
  - U-001
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-002
title: 'AGP Stage S2: Bump AGP to 8.x, Gradle wrapper to 8.x, and pin JDK 17 toolchain (SDK held at 29)'
pipeline: ''
domain: modernization
---

# U-002: AGP Stage S2 — Bump AGP to 8.x, Gradle wrapper to 8.x, and pin JDK 17 toolchain (SDK held at 29)

## Story

As a maintainer building SMS Backup+ on a clean checkout,
I want the root build file to declare AGP 8.x and the Gradle wrapper to reference a Gradle 8.x distribution with JDK 17 pinned in CI and documented for local development,
So that the build toolchain is proven at its target-state version in isolation before any SDK or behavioral changes are introduced, giving every subsequent stage a stable, independently-bisectable baseline to build on.

## Acceptance Criteria

- [ ] AC-1: `build.gradle` classpath entry declares `com.android.tools.build:gradle:8.*` (any 8.x release that is compatible with the Gradle 8.x wrapper used in this stage). No AGP version below 8.0 appears in any `*.gradle` or `*.gradle.kts` file in the repository. Verified by `grep -r "com.android.tools.build:gradle" .` returning only a line containing `8.`.

- [ ] AC-2: `gradle/wrapper/gradle-wrapper.properties` `distributionUrl` references a Gradle 8.x distribution (e.g., `gradle-8.x.x-bin.zip`). The Gradle version must satisfy AGP 8.x's minimum requirement (Gradle >= 8.0). Verified by static inspection of `gradle/wrapper/gradle-wrapper.properties` confirming the `distributionUrl` line contains `gradle-8.`.

- [ ] AC-3: The `namespace` property is declared in `app/build.gradle` under the `android {}` block, replacing any reliance on the `package` attribute in `AndroidManifest.xml` as the build-time namespace source. Specifically: `android { namespace "com.zegoggles.smssync" }` appears in `app/build.gradle`. Verified by `grep -n "namespace" app/build.gradle` returning a match containing `com.zegoggles.smssync`.

- [ ] AC-4: The CI workflow file (`.github/workflows/` or equivalent) pins the JDK version to 17 for all build and test jobs. The `java-version` (or equivalent toolchain-setup input) is set to `17` and no job in the workflow uses a JDK below 17. Verified by static inspection of every CI workflow file confirming `java-version: '17'` (or `17`) in the JDK setup step.

- [ ] AC-5: A `DEVELOPMENT.md` (or the existing developer setup file at the repo root, if one exists) documents the JDK 17 requirement. The documentation states the minimum JDK version (17), the recommended way to install it (e.g., SDKMAN, Homebrew `openjdk@17`, or the Android Studio bundled JDK), and a note that AGP 8 will fail to configure if a JDK below version 17 is detected. Verified by reading the relevant documentation file and confirming these three elements are present.

- [ ] AC-6: `./gradlew assembleRelease` exits 0 on a clean checkout of this stage's commit with a cleared Gradle cache (run `./gradlew --stop` followed by deletion of `~/.gradle/caches/` before invoking). The build uses only `google()`, `mavenCentral()`, `https://jitpack.io`, and the `https://maven.scijava.org/` mirror (preserved from U-001 for `firebase-jobdispatcher` resolution) as repository sources. Verified by CI log showing exit code 0 with no "Could not resolve" or "BUILD FAILED" output.

- [ ] AC-7: `./gradlew lint` exits 0 with no new lint violations introduced by this stage. The `lint-baseline.xml` committed at U-001 (Stage S1) is not modified to add entries in this commit; if the AGP 8 upgrade causes any new violations not present in the baseline, those violations must be resolved in source — not suppressed by adding them to the baseline. Net baseline entry count after this commit must be less than or equal to the count after U-001's commit. Verified by `git diff HEAD~1 lint-baseline.xml` (or the equivalent against the U-001 commit) showing no lines beginning with `+` that add issue entries.

- [ ] AC-8: `compileSdkVersion` and `targetSdkVersion` remain at 29 after this stage. `minSdkVersion` remains at 21 (set by U-001). No SDK-level value changes in `app/build.gradle` as part of this story. Verified by `grep "compileSdkVersion\|targetSdkVersion\|minSdkVersion" app/build.gradle` returning `compileSdkVersion 29`, `targetSdkVersion 29`, and `minSdkVersion 21` — no other values.

- [ ] AC-9: `./gradlew test` (unit test suite) exits 0 with no test failures on this stage's commit. The test suite must execute without errors under AGP 8 and JDK 17. Verified by CI log confirming all test tasks complete with `BUILD SUCCESSFUL` and zero test failure count in the test report.

- [ ] AC-10: The commit that introduces AGP 8.x and the Gradle 8.x wrapper is independently compilable from U-001's merge commit — i.e., a developer who checks out this story's commit (without any S3 changes) can run `./gradlew assembleRelease` to exit 0. The PR history (or equivalent commit graph) shows a discrete S2 commit that is not entangled with SDK-raise or behavioral conformance changes from U-003. Verified by PR commit history confirming a single identifiable S2 commit and CI green on that commit in isolation.

- [ ] AC-11: No deprecated AGP APIs that were removed in AGP 8.x remain in use in any `*.gradle` file. Specifically: `lintOptions {}` DSL block must not appear (replaced by `lint {}` in AGP 7+, enforced as removed in AGP 8); any use of the removed `flavorDimensions` or variant-API constructs that produce a deprecation error under AGP 8 must be resolved. Verified by `./gradlew assembleRelease` producing zero "deprecated API" build errors and zero "unresolved reference" errors attributable to AGP API removals.

- [ ] AC-12: R8 full-mode behavior (the AGP 8 default) does not cause the release build to fail or produce a binary that crashes on launch. If R8 full-mode causes shrinking or obfuscation issues with any existing dependency (notably `firebase-jobdispatcher` or `otto`), the appropriate `-keep` rules must be added to `app/proguard-rules.pro` as part of this story. Verified by `./gradlew assembleRelease` producing a valid APK and, on an API 29 AVD or physical device, the app launches to its main screen without a `ClassNotFoundException` or `NoSuchMethodException` crash.

## Integration Criteria

Integration criteria are not applicable for this story: no new runtime component, module, or cross-component interface is introduced. This story changes build configuration only.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `build.gradle` | Declares AGP `4.1.3` (after U-001: `7.4.x`) as the classpath dependency at `:8` | Bump classpath to `com.android.tools.build:gradle:8.x.x` (latest stable 8.x compatible with Gradle 8.x) |
| `gradle/wrapper/gradle-wrapper.properties` | `distributionUrl` references `gradle-7.5.x-bin.zip` (after U-001) | Change `distributionUrl` to a Gradle 8.x release (e.g., `gradle-8.6-bin.zip` or latest 8.x) |
| `app/build.gradle` | Uses implicit namespace derived from `package` attribute in `AndroidManifest.xml`; `lintOptions {}` block present (if surviving after U-001) | Add explicit `namespace "com.zegoggles.smssync"` to `android {}` block; migrate any `lintOptions {}` references to `lint {}` if AGP 7.4 migration in U-001 did not already do so |
| `app/proguard-rules.pro` | Existing keep rules for release shrinking | Add `-keep` rules for any class/method broken by R8 full-mode, if needed |
| `lint-baseline.xml` | Established at U-001 with pre-existing violations captured | Entries may only be removed; none may be added |
| `.github/workflows/*.yml` (CI workflow file(s)) | JDK version not pinned to 17 (or pinned to an earlier version) | Set `java-version: '17'` in every build/test job's JDK setup step |
| `DEVELOPMENT.md` (or equivalent developer setup doc) | JDK requirement not documented, or documents an earlier version | Add JDK 17 requirement, installation guidance, and AGP 8 incompatibility note |

## Existing Behavior to Preserve

- The public `com.zegoggles.smssync.BACKUP` broadcast contract received by `BackupBroadcastReceiver` must remain triggerable — the build must produce a functional APK, not just exit 0.
- All existing unit tests must continue to pass (`./gradlew test` green).
- `compileSdkVersion 29` and `targetSdkVersion 29` are unchanged — no SDK-level behavioral enforcement changes in this stage.
- `minSdkVersion 21` (set by U-001) is unchanged.
- The scijava Maven mirror (`https://maven.scijava.org/content/repositories/public`) must remain in the repository declarations so that `com.firebase:firebase-jobdispatcher:0.8.6` continues to resolve. This mirror is removed in U-017 (WorkManager migration), not here.
- `jcenter()` and `https://jcenter.bintray.com` must not be re-introduced (they were absent from U-001's output and must stay absent).
- The `warningsAsErrors true` and `-Werror -Xlint:deprecation` flags in `app/build.gradle` remain active; the lint-baseline shrink-only contract is enforced.

## Verification Steps

1. **AC-1 — AGP version:** Run `grep -r "com.android.tools.build:gradle" .` from the repo root. Confirm every matching line contains `8.` with no line containing `4.1`, `7.4`, or any other pre-8 version.

2. **AC-2 — Gradle wrapper:** Open `gradle/wrapper/gradle-wrapper.properties`. Read the `distributionUrl` line. Confirm it contains `gradle-8.` (e.g., `gradle-8.6-bin.zip`).

3. **AC-3 — namespace declaration:** Run `grep -n "namespace" app/build.gradle`. Confirm at least one line returns containing `"com.zegoggles.smssync"`. Confirm that `app/src/main/AndroidManifest.xml` still contains `package="com.zegoggles.smssync"` as a manifest attribute (the attribute is still valid at runtime, though the build-time namespace now comes from `build.gradle`; this preserves backward compatibility).

4. **AC-4 — CI JDK pin:** Open every file under `.github/workflows/`. For each file, locate the step that sets up Java (typically `actions/setup-java`). Confirm `java-version` is set to `'17'` or `17`. Confirm no workflow step sets up a JDK below 17.

5. **AC-5 — Developer documentation:** Open `DEVELOPMENT.md` (or the equivalent developer setup document at the repo root). Confirm the document states JDK 17 as the minimum, provides at least one installation method, and warns that AGP 8 will fail on JDK < 17.

6. **AC-6 — Clean assembleRelease:** On a CI runner (or locally): run `./gradlew --stop`, delete `~/.gradle/caches/`, then run `./gradlew assembleRelease --no-build-cache`. Confirm the final line of output is `BUILD SUCCESSFUL` and the exit code is 0. Confirm no "Could not resolve" lines appear in the output.

7. **AC-7 — Lint baseline not grown:** Run `git diff <U-001-merge-sha> HEAD -- lint-baseline.xml`. Confirm there are no added lines (lines beginning with `+` that introduce new issue entries). If `lint-baseline.xml` has changed, every change must be a removal (lines beginning with `-`). Then run `./gradlew lint` and confirm exit 0.

8. **AC-8 — SDK levels unchanged:** Run `grep "compileSdkVersion\|targetSdkVersion\|minSdkVersion" app/build.gradle`. Confirm the output contains exactly `compileSdkVersion 29`, `targetSdkVersion 29`, and `minSdkVersion 21` with no other values.

9. **AC-9 — Unit tests pass:** Run `./gradlew test`. Confirm `BUILD SUCCESSFUL` and that the test report (at `app/build/reports/tests/testDebugUnitTest/index.html` or equivalent) shows 0 failures and 0 errors.

10. **AC-10 — S2 commit independence:** Check out the specific S2 commit (before U-003 changes). Run `./gradlew assembleRelease` from that commit. Confirm `BUILD SUCCESSFUL`. Confirm `git log --oneline` shows a discrete S2 commit not entangled with SDK-raise changes.

11. **AC-11 — No removed AGP APIs in use:** Run `./gradlew assembleRelease 2>&1 | grep -i "deprecated\|lintOptions\|unresolved"`. Confirm no output. Alternatively, confirm by static inspection that `lintOptions {}` does not appear in any `*.gradle` file.

12. **AC-12 — R8 full-mode launch:** After `./gradlew assembleRelease` (AC-6 passed), install the resulting APK on an API 29 AVD (`adb install app/build/outputs/apk/release/app-release.apk`) and launch the app (`adb shell am start -n com.zegoggles.smssync/.activity.MainActivity`). Confirm the app reaches its main screen without a fatal exception in logcat (`adb logcat *:E | grep -v "^---"` should show no `FATAL EXCEPTION` lines for the app process within 5 seconds of launch).

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (single-module Gradle project) | All build file changes, CI workflow update, proguard rules, developer documentation | Developer |

## Technical Context

**Why S2 holds SDK at 29.** DES-MODERNIZATION-001 (ADR-MOD-001-A) stages the toolchain upgrade ahead of the behavioral raise so that toolchain failures are unambiguously separable from behavioral failures. AGP 4.1.3 (pre-U-001) cannot build `compileSdk 35`; neither can AGP 7.4.x without JDK 17. By proving AGP 8 + Gradle 8 + JDK 17 at SDK 29, any breakage at S2 is a toolchain regression with no behavioral entanglement. S3 (U-003) then raises the SDK and co-lands all conformance fixes against an already-validated toolchain.

**AGP 8 mandatory `namespace`.** AGP 8 removes the ability to use `package` in `AndroidManifest.xml` as the sole build-time namespace source. The `namespace` property must be explicit in `app/build.gradle`'s `android {}` block. The value is `"com.zegoggles.smssync"` — confirmed by `app/src/main/AndroidManifest.xml` package attribute. Failure to set this produces: `Namespace not specified. Specify a namespace in the module's build file`. The `package` attribute in the manifest is retained for runtime component resolution but is no longer authoritative for the build namespace.

**AGP 8 `lintOptions {}` → `lint {}` DSL.** AGP 7.0 deprecated `lintOptions {}` in favor of `lint {}`; AGP 8 removes it as a hard error. U-001 (AGP 7.4) should have migrated this, but if not, this story must complete the migration. The lint DSL shape is otherwise identical; it is a block-rename only.

**JDK 17 requirement.** AGP 8.x requires JDK 17 at configuration time (not just compile time). If the Gradle daemon is started with JDK 11 or earlier, `./gradlew` will fail before any task runs with: `Android Gradle plugin requires Java 17 to run. You are currently using Java <version>`. The CI JDK pin in AC-4 prevents this failure in CI. AC-5's developer documentation prevents it locally. The recommended JDK source is the Android Studio bundled JDK (Studio Hedgehog or later ships JDK 17) or OpenJDK 17 from Adoptium.

**R8 full-mode (AGP 8 default).** AGP 7.x defaulted to R8 compatibility mode (which behaves like ProGuard). AGP 8 defaults to R8 full-mode, which performs more aggressive class inlining, interface merging, and member removal. This can break reflective access patterns used by older libraries. The two libraries most likely affected in this codebase are `com.firebase:firebase-jobdispatcher:0.8.6` (uses reflection for job class instantiation) and `com.squareup:otto:1.3.8` (uses reflection to find `@Subscribe`/`@Produce` annotated methods at runtime). If R8 full-mode strips these, the release APK will crash at runtime even though the build exits 0. AC-12 verifies this via a launch smoke test. If issues are found, the fix is `-keep class com.firebase.jobdispatcher.** { *; }` and `-keep class com.squareup.otto.** { *; }` in `app/proguard-rules.pro`, plus keeping subscriber methods that are accessed only via reflection.

**Gradle 8 configuration cache compatibility (advisory).** Gradle 8 stabilizes the configuration cache. If `build.gradle` uses any task configuration that is configuration-cache-incompatible (e.g., accessing `project` at execution time), Gradle 8 may emit warnings. These are warnings, not errors, unless `--configuration-cache-problems=fail` is set; that flag is not set in this project, so they do not block AC-6. However, any such warning that causes a baseline entry addition violates AC-7 and must be resolved.

**Gradle wrapper integrity.** The `gradle/wrapper/gradle-wrapper.jar` binary should not be changed manually. The correct way to update the wrapper is `./gradlew wrapper --gradle-version 8.x.x --distribution-type bin`, which regenerates both `gradle-wrapper.properties` and replaces the jar. Alternatively, update `distributionUrl` in `gradle-wrapper.properties` directly and commit; the jar version is checked against the distribution at first run. Either approach is acceptable; the PR reviewer should confirm `gradle-wrapper.jar` is not a spurious binary diff if the update method was manual.

**Dependency on U-001.** This story begins from U-001's green state: AGP 7.4.x, Gradle 7.5.x, `minSdkVersion 21`, `compileSdkVersion 29`, `lint-baseline.xml` committed. Any deviation from U-001's expected output state (e.g., `lintOptions {}` not yet migrated) must be addressed in this story before AC-6 can pass.

## Supporting Documentation

- REQ-MODERNIZATION-001 §SDK and Build Toolchain Uplift (AC-3, AC-4, AC-6)
- REQ-MODERNIZATION-001 §Constraint 4 (staged AGP upgrade path — three independently-compilable commits)
- REQ-MODERNIZATION-001 §Constraint 2 (warningsAsErrors brittleness and lint-baseline shrink-only policy)
- DES-MODERNIZATION-001 §Staged Toolchain Uplift — Stage S2 row (AGP 8.x, Gradle 8.x, JDK 17, SDK held at 29)
- DES-MODERNIZATION-001 §ADR-MOD-001-A (stage ordering rationale)
- DES-MODERNIZATION-001 §Risks (AGP 8 requires JDK 17 — CI/dev toolchain mismatch at S2)

## Integration Contract References

None. This story introduces no new cross-component runtime interface. See DES-MODERNIZATION-001 §Integration Contracts for the formal rationale.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Estimation: medium. The mechanical work (version bumps, namespace declaration, JDK pin) is small; the uncertainty lies in R8 full-mode compatibility with `firebase-jobdispatcher` and `otto` (AC-12), which may require iterative ProGuard rule authoring and launch-smoke validation. JDK 17 CI wiring and developer documentation are straightforward but must not be skipped — a green CI run on an underpinned JDK is not a valid AC-4 pass.
