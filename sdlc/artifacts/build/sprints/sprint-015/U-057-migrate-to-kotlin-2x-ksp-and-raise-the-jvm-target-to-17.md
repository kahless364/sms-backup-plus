---
type: story
status: done
artifact_type: user-story
priority: medium
complexity: high
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-057
title: Migrate to Kotlin 2.x + KSP and raise the JVM target to 17
pipeline: ''
domain: modernization
resolution: done
requirement_source: assessment:20260623-post-migration-assessment#BT-001
sprint: '000015'
---

# U-057: Migrate to Kotlin 2.x + KSP and raise the JVM target to 17

## Story
As a maintainer of the SMS Backup+ codebase, I want the Kotlin compiler upgraded from 1.9.25 to 2.x, kapt replaced with KSP for annotation processing, and the JVM/source target raised from 8 to 17, so that the toolchain is on a supported, forward-compatible path, Hilt annotation processing uses the production-path processor (KSP), and the `-Xlint:-options` obsolete-source suppression can be removed.

## Source
Derived from assessment 20260623-post-migration-assessment, finding BT-001. See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/build-toolchain.md`.

## Acceptance Criteria
- [ ] AC-1: `app/build.gradle` and the root `build.gradle` (or `libs.versions.toml` if using version catalogs) are updated to declare Kotlin 2.x (minimum `2.0.0`, or the latest stable 2.x release at implementation time); all Kotlin Gradle plugin references are updated consistently; no `1.9.x` version string remains.
- [ ] AC-2: All `apply plugin: 'kotlin-kapt'` / `kapt(...)` declarations are replaced with `com.google.devtools.ksp` plugin and `ksp(...)` dependency declarations; the Hilt KSP processor (`com.google.dagger:hilt-compiler` via KSP) is used in place of the kapt processor; `gradle.properties:30` no longer contains kapt-specific flags.
- [ ] AC-3: `app/build.gradle:184` (and any companion `jvmTarget`/`sourceCompatibility`/`targetCompatibility` settings) are updated to `JavaVersion.VERSION_17` / `jvmTarget = "17"`; the `-Xlint:-options` suppression in `app/build.gradle:185` (or equivalent) is removed; `./gradlew :app:assembleDebug` produces no obsolete-source warnings.
- [ ] AC-4: All 672+ unit tests pass: `./gradlew :app:testDebugUnitTest` exits 0 with the authoritative test count (`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`) equal to or greater than the pre-migration count; Robolectric is compatible with the new Kotlin/KSP combination.
- [ ] AC-5: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` — full gate passes; `gradle/verification-metadata.xml` is updated for all new/changed artifact SHA-256s.

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/build.gradle:7` | Kotlin plugin version `1.9.25` declared | Update to Kotlin 2.x stable |
| `app/build.gradle:90,112` | `apply plugin: 'kotlin-kapt'`; `kapt(...)` dependency declarations | Replace with KSP plugin and `ksp(...)` declarations |
| `app/build.gradle:184` | `jvmTarget = "1.8"` (or equivalent JavaVersion.VERSION_8) | Raise to `JavaVersion.VERSION_17` / `jvmTarget = "17"` |
| `app/build.gradle:185` | `-Xlint:-options` compiler flag suppressing obsolete-source warning | Remove this flag |
| `gradle.properties:30` | kapt-specific properties (e.g. `kapt.use.worker.api`, `kapt.incremental.apt`) | Remove kapt properties; add KSP equivalents if needed |
| `gradle/verification-metadata.xml` | SHA-256 entries for `kotlin-stdlib:1.9.x`, kapt artifacts | Update for Kotlin 2.x and KSP artifacts |

## Existing Behavior to Preserve
- Hilt DI graph: the KSP Hilt processor must generate the same component/module code as kapt; all Hilt injection points (workers, activities, ViewModels) must continue to compile and inject correctly.
- Robolectric test suite: all 672+ tests must pass; Robolectric's Kotlin version compatibility must be verified (check Robolectric compatibility matrix for Kotlin 2.x before committing to a version).
- `warningsAsErrors true` (`app/build.gradle:65`) and `-Werror -Xlint:unchecked` (`:185`) must remain active; the only removal is the `-Xlint:-options` suppression.
- Supply-chain verification: `gradle/verification-metadata.xml` with `verify-metadata=true` must be updated; build must pass with metadata verification active.

## Verification Steps
1. Run `./gradlew :app:assembleDebug 2>&1 | grep -i "xlint\|obsolete\|source.*1.8"` — expect zero matches (no obsolete-source warning, no kapt references).
2. Run `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` — confirm count is ≥ 672.
3. Run `./gradlew :app:testDebugUnitTest` — all tests pass.
4. Run `./gradlew :app:jacocoTestCoverageVerification` — gate passes.
5. Run `./gradlew :app:assembleRelease` — R8 release build succeeds (Kotlin 2.x IR backend + KSP + R8 compatibility check).
6. Run `./gradlew --write-verification-metadata sha256 :app:assembleDebug` and confirm `gradle/verification-metadata.xml` is updated; subsequent build with verification passes.

## Technical Context
- BT-001 root cause: Kotlin 1.9.x + kapt is not a defect — it works — but kapt is now maintenance-mode (Google has announced KSP as the successor); Kotlin 2.x includes the new K2 compiler with improved incremental compilation; the JVM-8 target is kept only via `-Xlint:-options` suppression (the JDK 17 build environment flags it as obsolete).
- KSP migration for Hilt: replace `classpath 'com.google.dagger:hilt-android-gradle-plugin:...'` (unchanged) and `kapt("com.google.dagger:hilt-compiler:...")` with `ksp("com.google.dagger:hilt-compiler:...")`. Hilt supports KSP from Hilt 2.48+.
- Kotlin 2.x K2 compiler introduces stricter smart-cast and null-safety rules; expect some source changes to fix new warnings/errors promoted under `-Werror`.
- This is the largest-effort story in Sprint D; plan for a full day of toolchain debugging. Run the migration incrementally: first raise Kotlin version (K2 compat check), then swap kapt → KSP, then raise JVM target.

## Notes
- Sprint D (Toolchain Currency). Large effort (L); schedule last or as a dedicated spike.
- Hilt/Robolectric compatibility matrix must be checked against the chosen Kotlin 2.x version before starting. Document the chosen versions in the commit message.
- If Robolectric does not yet support Kotlin 2.x at implementation time, pin the highest Kotlin version that is Robolectric-compatible and document the constraint.
