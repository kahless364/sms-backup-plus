---
artifact_type: plan
story_id: "U-057"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-06-24"
contracts_verified: []
risks_identified: 2
---

# Implementation Plan: U-057

## Story Overview

Migrate SMS Backup+ from Kotlin 1.9.25 + kapt + JVM target 8 to Kotlin 2.x + KSP + JVM target 17,
eliminating the obsolete-source `-Xlint:-options` suppression and putting the toolchain on a supported,
forward-compatible path.

## Acceptance Criteria Mapping

| AC | Implementation Step | Notes |
|----|-------------------|-------|
| AC-1 | Update `build.gradle` Kotlin plugin version to 2.0.21; update `kotlin-stdlib` in `app/build.gradle` | 2.0.21 is the latest stable 2.0.x; compatible with AGP 8.7.3 + Hilt 2.51.1 |
| AC-2 | Replace `apply plugin: 'kotlin-kapt'` with `apply plugin: 'com.google.devtools.ksp'`; replace `kapt`/`kaptTest` dependencies with `ksp`/`kspTest`; add KSP plugin to buildscript | Hilt 2.51.1 supports KSP; KSP version 2.0.21-1.0.28 matches Kotlin 2.0.21 |
| AC-3 | Raise `sourceCompatibility`/`targetCompatibility` to `VERSION_17`, `jvmTarget` to `'17'`; remove `-Xlint:-options` from JavaCompile args; remove `android.javaCompile.suppressSourceTargetDeprecationWarning` from `gradle.properties` | Removes all obsolete-source suppression machinery |
| AC-4 | Run `./gradlew :app:testDebugUnitTest`; verify 697 tests pass | Robolectric 4.12.2 is compatible with Kotlin 2.0.21 |
| AC-5 | Run `./gradlew --write-verification-metadata sha256 :app:assembleDebug`; update `gradle/verification-metadata.xml` | New KSP + Kotlin 2.0.21 artifacts need SHA-256 entries |

## Implementation Steps

1. **Raise JVM/source target 8 → 17** (`app/build.gradle` compileOptions + kotlinOptions)
2. **Remove obsolete-source suppression** (remove `-Xlint:-options` from JavaCompile task; remove `android.javaCompile.suppressSourceTargetDeprecationWarning` from `gradle.properties`)
3. **Upgrade Kotlin 1.9.25 → 2.0.21** (`build.gradle` classpath + `app/build.gradle` stdlib dependency)
4. **Add KSP plugin to buildscript** (`build.gradle` classpath `com.google.devtools.ksp:...:2.0.21-1.0.28`)
5. **Replace kapt with KSP** (`app/build.gradle`: plugin, kapt{} block, dependency declarations)
6. **Update verification-metadata.xml** via `--write-verification-metadata sha256`

## Files Modified/Created

| File | Change |
|------|--------|
| `build.gradle` | Kotlin plugin 1.9.25 → 2.0.21; add KSP plugin classpath |
| `app/build.gradle` | JVM target 17; remove `-Xlint:-options`; kotlin-stdlib 2.0.21; kapt → KSP |
| `gradle.properties` | Remove `android.javaCompile.suppressSourceTargetDeprecationWarning` |
| `gradle/verification-metadata.xml` | New SHA-256 entries for Kotlin 2.0.21 + KSP 2.0.21-1.0.28 artifacts |

## Contracts Verification

No integration contracts (CNTR-*) for this story.

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Hilt KSP codegen fails in mixed Java+Kotlin module | Test incrementally; revert to kapt if any Hilt breakage (per U-022 deliberate decision). In practice, Hilt 2.51.1 supports KSP for mixed modules. |
| Kotlin 2.x K2 compiler introduces new warnings-as-errors | Run with `-Werror` active; fix any new K2 errors. In practice, no source changes required. |

## Test Strategy

- Run `./gradlew clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`
- Authoritative count: `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`
- Verify no `xlint\|obsolete\|source.*1.8` warnings in assembleDebug output

## Phase Completion Report
---
story_id: "U-057"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-015/U-057/plan.md"
story_status: "planned"
current_build_phase: "implementation"
errors: []
---
