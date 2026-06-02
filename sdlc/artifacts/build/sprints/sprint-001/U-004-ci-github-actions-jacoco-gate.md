---
type: story
status: done
sprint: "000001"
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirement_source: authored
requirements:
  - REQ-MODERNIZATION-003
design_docs:
  - DES-MODERNIZATION-003
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-004
title: Add GitHub Actions CI workflow with JaCoCo per-package coverage gate and R8 enablement
pipeline: ''
domain: modernization
---

# U-004: Add GitHub Actions CI workflow with JaCoCo per-package coverage gate and R8 enablement

## Story

As a contributor opening a pull request against `master`,
I want every PR to automatically run the full test suite, Android Lint, JaCoCo coverage verification, and a release APK assembly with R8 enabled — and for that workflow to be a required status check that blocks merge on any failure —
so that behavioral regressions, coverage drops below 70% in the engine packages, and R8-induced shrinking breakages are detected before they reach `master`, establishing Gate G2 as the automated governance prerequisite for all Phase-2 substrate work.

## Acceptance Criteria

- [ ] AC-1: Given a pull request is opened or updated against `master`, when the GitHub Actions runner processes the event, then `.github/workflows/ci.yml` exists in the repository and runs the following four phases in a single chained Gradle invocation that prevents any phase from being skipped: `./gradlew test lint jacocoTestReport assembleRelease`; the `--verify` flag (or Gradle's equivalent dependency-verification option) is appended to the invocation so that a checksum mismatch in `gradle/verification-metadata.xml` causes the command to exit non-zero; the workflow is registered as a required status check on the `master` branch-protection rule via GitHub repository settings (verifiable via `gh api repos/:owner/:repo/branches/master/protection`); and merge is blocked until the check passes.

- [ ] AC-2: Given the CI workflow executes `assembleRelease`, when the release APK or AAB is assembled, then `app/build.gradle` line 33 reads `minifyEnabled true` on the release build variant (changed from the current `false`); the `./gradlew assembleRelease` task in CI completes successfully with R8 invoked (observable in the task log as the R8 shrinking step executing); and the full unit test suite (`./gradlew test`) continues to pass in the same CI run, confirming that R8-enabled assembly does not break the test-time classpath.

- [ ] AC-3: Given JaCoCo is applied and the coverage verification rule is configured in `app/build.gradle`, when `./gradlew jacocoTestReport` completes, then: (a) a `jacocoTestCoverageVerification` task (or an equivalent `violationRules` block `finalizedBy`-linked to `jacocoTestReport`) evaluates line-coverage independently for each of the three gated packages; (b) the rule for `com/zegoggles/smssync/service/**` requires `LINE` `COVEREDRATIO >= 0.70` at `element = PACKAGE` and the glob is verified at implementation time to include sub-packages `service/state/` and `service/exception/` (confirmed present in the source tree); (c) the rule for `com/zegoggles/smssync/mail/**` requires `LINE` `COVEREDRATIO >= 0.70` at `element = PACKAGE`; (d) the rule for `com/zegoggles/smssync/auth/**` requires `LINE` `COVEREDRATIO >= 0.70` at `element = PACKAGE`; (e) each rule is configured as an independent `rule {}` block so that a breach in one package fails the task and names that package in the error output, without being masked by higher coverage in the other two; (f) the Gradle build exits non-zero when any of the three packages individually falls below 70%, and a PR that drops any gated package below 70% by adding production code without corresponding tests fails CI automatically and is blocked from merging.

- [ ] AC-4: Given that `app/build.gradle` currently declares `minifyEnabled false` and has no R8 keep rules for Otto's reflection-driven `@Subscribe`/`@Produce` dispatch or `firebase-jobdispatcher`'s reflective `SmsJobService` resolution, when R8 is enabled via AC-2 and `./gradlew assembleRelease` runs in CI, then `app/proguard-rules.pro` contains keep rules sufficient to prevent R8 from stripping: (a) all methods annotated with `com.squareup.otto.Subscribe` or `com.squareup.otto.Produce` (the 12 import sites across 11 files in the current source tree); (b) the `com.firebase.jobdispatcher.SmsJobService` class and its `onStartJob`/`onStopJob` entry points resolved reflectively via the `ACTION_EXECUTE` intent filter; and (c) any `AuthMode` or `DataType` enum valueOf paths exercised by `Preferences.getDefaultType(...)`; and the `assembleRelease` Gradle task exits zero with these keep rules in place, demonstrating that no R8 keep-rule gap blocks the release build.

- [ ] AC-5: Given the CI workflow defined in AC-1 runs on a pull request that introduces new production code in `app/src/main/java/com/zegoggles/smssync/service/`, `app/src/main/java/com/zegoggles/smssync/mail/`, or `app/src/main/java/com/zegoggles/smssync/auth/` without a corresponding increase in test coverage, when `./gradlew jacocoTestReport` executes and any of the three gated packages drops below 70% LINE coverage, then the CI workflow exits with a non-zero status code and the GitHub required-status-check blocks the PR from merging; this behavior is verified by the implementer during development by temporarily dropping a test and confirming CI fails naming the specific package that breached the threshold.

- [ ] AC-6: Given the CI workflow YAML at `.github/workflows/ci.yml`, when the file is inspected, then: (a) it runs on the `pull_request` event targeting `master` and also on `push` to `master` (so that the Gate G2 milestone green run on `master` is recorded); (b) it specifies a concrete, pinned version of the `ubuntu` (or equivalent) runner (e.g., `ubuntu-22.04`, not `ubuntu-latest`) to prevent silent runner environment drift; (c) it caches the Gradle wrapper and the `~/.gradle/caches` and `~/.gradle/wrapper` directories keyed on a hash of `**/*.gradle*` and `gradle/wrapper/gradle-wrapper.properties` to prevent redundant downloads; (d) it does not contain any `continue-on-error: true` directive or `|| true` shell escape that would suppress a non-zero exit from the Gradle invocation; and (e) the workflow name and job name identify this check unambiguously (e.g., `name: CI` with `jobs.build`) so it can be referenced by the branch-protection required-status-check setting by exact string.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `.github/workflows/ci.yml` | Does not exist; no GitHub Actions CI | Create: runs `./gradlew test lint jacocoTestReport assembleRelease --verify` on `pull_request` and `push` to `master` |
| `app/build.gradle` | `minifyEnabled false` (line 33); no JaCoCo plugin; no `jacocoTestReport` or `jacocoTestCoverageVerification` task | Add `apply plugin: 'jacoco'`; change `minifyEnabled false` → `true`; add `jacocoTestReport` task with `finalizedBy jacocoTestCoverageVerification`; add three independent per-package coverage rules |
| `app/proguard-rules.pro` | Minimal keep rules; no entries for Otto `@Subscribe`/`@Produce` reflection or firebase-jobdispatcher's reflective `SmsJobService` resolution | Add keep rules for Otto annotation-driven dispatch (12 call sites), `SmsJobService` entry points, and `AuthMode`/`DataType` enum valueOf paths |

## Existing Behavior to Preserve

- All 35 existing Robolectric test classes (`app/src/test/java/com/zegoggles/smssync/**`) must continue to pass without changes to their test logic; `@Config`-level annotation changes are permitted.
- `warningsAsErrors true` in the existing lint configuration (`app/build.gradle:41`) must remain in effect; the lint phase must not be relaxed by removing this flag. If the SDK-29 toolchain surfaces new lint warnings under the R8-enabled build, a `lint-baseline.xml` is the managed escape valve, not removal of `-Werror`-equivalent behavior.
- The `./gradlew assembleDebug` path and debug build variant are not modified by this story; only the release variant's `minifyEnabled` setting is changed.
- The existing `app/proguard-rules.pro` file's current content must be preserved; new keep rules are appended, not substituted.

## Verification Steps

1. **AC-1 — Workflow triggers and required status check:** Open a draft PR against `master` (any trivial one-line comment change). Observe the GitHub Actions tab; confirm the `CI / build` check (or equivalent name matching the workflow's `name:` and `jobs:` keys) appears and runs. After the pipeline implementation is complete, visit repository Settings > Branches > Branch protection rules for `master` and confirm the CI workflow job is listed as a required status check with "Require status checks to pass before merging" enabled. Run `gh api repos/:owner/:repo/branches/master/protection` and confirm the check name appears under `required_status_checks.contexts`.

2. **AC-2 — R8 enabled on release:** Run `./gradlew assembleRelease` locally. Confirm the build log contains R8 task output (look for lines containing `R8` or `ProGuard` in the task execution trace). Inspect `app/build.gradle` line 33 and confirm the value reads `minifyEnabled true`. Run `./gradlew test` in the same environment and confirm all tests pass.

3. **AC-3 — JaCoCo per-package gate enforces 70% independently:** Run `./gradlew jacocoTestReport` on a clean checkout. Confirm the task exits zero (all three packages currently meet 70%; if any do not, the story's first step is backfill until they do — see Technical Notes). Temporarily rename or comment out a test class in `app/src/test/java/com/zegoggles/smssync/service/` to artificially drop `service/` coverage below 70%. Re-run `./gradlew jacocoTestReport`. Confirm the build exits non-zero and the error output names `com/zegoggles/smssync/service/**` as the offending package. Restore the test class and confirm the build returns to zero.

4. **AC-4 — R8 keep rules resolve Otto and jobdispatcher reflection:** Run `./gradlew assembleRelease`. Confirm the build exits zero. Inspect the release APK using `apktool d` or Android Studio's APK Analyzer and verify that at least one `@Subscribe`-annotated method (e.g., `SmsBackupService.onEvent(...)`) is present in the shrunken output and has not been renamed or removed. Confirm that `SmsJobService` is present in the manifest and its class is retained in the APK. Confirm `AuthMode` and `DataType` enums are not stripped.

5. **AC-5 — Gate blocks PRs that drop coverage below threshold:** In a feature branch, add a new production method to a class in `com/zegoggles/smssync/mail/` with no corresponding test. Open a PR to `master`. Confirm the CI workflow runs and the check fails with a JaCoCo coverage violation naming `mail/`. Confirm GitHub shows the PR as blocked from merging.

6. **AC-6 — Workflow structural properties:** Inspect `.github/workflows/ci.yml` directly. Confirm `on: pull_request` targets `master` and `on: push` targets `master`. Confirm the runner is pinned (not `ubuntu-latest`). Confirm `cache` steps are present for `~/.gradle/caches` and `~/.gradle/wrapper`. Confirm no `continue-on-error: true` or `|| true` escape exists in the Gradle step. Confirm the job name string matches the name registered as the required status check in the branch-protection setting.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / Gradle | `app/build.gradle` JaCoCo plugin + `jacocoTestCoverageVerification` rules + `minifyEnabled true` + R8 keep rules in `app/proguard-rules.pro` | Developer |
| GitHub Actions | `.github/workflows/ci.yml` creation; branch-protection required-status-check configuration via `gh api` or repository settings | Developer |

## Technical Notes

### Workflow design

The four Gradle phases must be chained in a single `./gradlew` invocation to prevent any phase from being skipped via Gradle task-avoidance or a `|| true` shell escape:

```
./gradlew test lint jacocoTestReport assembleRelease --verify
```

`jacocoTestReport` is placed before `assembleRelease` so that a coverage gate breach fails the build before the (slower) R8 assembly step runs, giving fast feedback. The `--verify` flag triggers Gradle dependency verification against `gradle/verification-metadata.xml`; if this file does not yet exist when this story lands, it is created as part of this story (see REQ-MODERNIZATION-003 AC-8) by running `./gradlew --write-verification-metadata sha256` after all this story's build-file changes are applied.

### JaCoCo per-package gate

Three independent `rule {}` blocks inside a `JacocoCoverageVerification` task, each using `element = 'PACKAGE'` with an `includes` glob and `counter = 'LINE'` at `minimum = 0.70`:

```groovy
apply plugin: 'jacoco'

tasks.register('jacocoTestCoverageVerification', JacocoCoverageVerification) {
    dependsOn 'testDebugUnitTest'
    executionData fileTree(buildDir).include('**/*.exec')
    violationRules {
        rule {
            element = 'PACKAGE'
            includes = ['com/zegoggles/smssync/service/**']
            limit { counter = 'LINE'; value = 'COVEREDRATIO'; minimum = 0.70 }
        }
        rule {
            element = 'PACKAGE'
            includes = ['com/zegoggles/smssync/mail/**']
            limit { counter = 'LINE'; value = 'COVEREDRATIO'; minimum = 0.70 }
        }
        rule {
            element = 'PACKAGE'
            includes = ['com/zegoggles/smssync/auth/**']
            limit { counter = 'LINE'; value = 'COVEREDRATIO'; minimum = 0.70 }
        }
    }
}

tasks.named('jacocoTestReport') {
    finalizedBy 'jacocoTestCoverageVerification'
}
```

**Critical implementation note:** The `includes` glob syntax for `element = 'PACKAGE'` must be validated against the JaCoCo version resolved by AGP 4.1.3 at implementation time. JaCoCo uses `/`-separated class-path globs (not `.`-separated package names) for `PACKAGE` element includes. The form `com/zegoggles/smssync/service/**` must be confirmed to recursively include `service/state/` and `service/exception/` sub-packages (both verified to exist in the source tree). If the JaCoCo version resolved by AGP 4.1.3 does not support recursive `**` globs for `PACKAGE` element includes, the implementer must list each sub-package explicitly to avoid a silent false-pass. Verify by running `./gradlew jacocoTestReport` and inspecting the generated HTML report under `app/build/reports/jacoco/` to confirm `state/` and `exception/` classes appear in the `service/` coverage totals.

**Baseline-coverage risk (first run):** Coverage for all three gated packages is currently unmeasured. The story's first executable step is to run `./gradlew testDebugUnitTest jacocoTestReport` (without the verification rule initially) and read the per-package line-coverage numbers from `app/build/reports/jacoco/jacocoTestReport/html/`. If any gated package is below 70%, the story owns backfill of characterization tests until it clears — noting that the named characterization tests for `AuthPreferences.migrate()` and `BackupJobs` (REQ-003 AC-4, AC-5) are scoped to U-006, but any additional backfill needed to reach 70% on `service/`, `mail/`, or `auth/` is in-scope for this story. The 70% threshold is the floor the gate must reach before Gate G2 is declared, not an assumption that it is already met.

### R8 keep rules

Enabling `minifyEnabled true` exposes three reflection surfaces in the current codebase that R8 will strip without explicit keep rules:

1. **Otto event bus** — Otto dispatches events by reflecting on methods annotated `@com.squareup.otto.Subscribe` and `@com.squareup.otto.Produce`. There are 12 import sites across 11 files. Without a keep rule, R8 will rename or remove these methods. Required keep rule:
   ```
   -keep @com.squareup.otto.Subscribe class * { *; }
   -keepclassmembers class * {
       @com.squareup.otto.Subscribe <methods>;
       @com.squareup.otto.Produce <methods>;
   }
   ```

2. **firebase-jobdispatcher / SmsJobService** — `firebase-jobdispatcher:0.8.6` resolves `SmsJobService` reflectively via the `ACTION_EXECUTE` intent filter. R8 will strip the class or rename it unless it is kept. Required keep rule:
   ```
   -keep public class com.zegoggles.smssync.service.SmsJobService { *; }
   ```

3. **AuthMode / DataType enum valueOf paths** — `Preferences.getDefaultType(...)` uses reflection-adjacent `valueOf` paths. Required keep rule:
   ```
   -keepclassmembers enum com.zegoggles.smssync.** {
       public static **[] values();
       public static ** valueOf(java.lang.String);
   }
   ```

All three keep rules are interim: Otto and firebase-jobdispatcher are removed by later migration units (MU-006 and MU-005 respectively), at which point the corresponding keep rules are also removed. The interim cost is accepted per REQ-MODERNIZATION-003 Constraints ("R8 breakages are in-scope for this requirement and must not be deferred").

### Branch-protection configuration

The CI workflow existing as a file is necessary but not sufficient for AC-1. The workflow must also be registered as a required status check. This is a GitHub repository settings change, not a file in the tree. The implementer must:

1. Navigate to repository Settings > Branches > Branch protection rules > `master`, or
2. Use `gh api --method PUT repos/:owner/:repo/branches/master/protection` with a JSON body that includes the workflow's job name under `required_status_checks.contexts`.

The exact string to use in `required_status_checks.contexts` is the `name` value of the workflow job (e.g., `"CI / build"` if the workflow is named `CI` and the job is named `build`). This must match exactly or the required-status-check registration will not bind to the workflow run.

### Scope boundary with related stories

This story (U-004) is scoped to: CI workflow, JaCoCo per-package gate, R8 enablement, and R8 keep-rule resolution. It explicitly excludes:

- Test-toolchain version upgrades (Robolectric, JUnit, Mockito, Truth) — scoped to U-005, which is a co-requisite of the SDK raise and must land in the same PR as the SDK bump.
- `AuthPreferences.migrate()` and `BackupJobs` characterization tests (AC-4 and AC-5 of REQ-003) — scoped to U-006.
- `SmsBackupServiceTest` empty-body remediation (AC-6 of REQ-003) — scoped to U-006.
- Supply-chain verification metadata generation (`gradle/verification-metadata.xml`) — if not already present, this story creates it as a by-product of finalizing the build-file changes; U-005 regenerates it after the toolchain sweep.

U-004 is parallel-eligible from day 1 and independent of U-001 (AGP/Gradle upgrade), U-002, U-003, and U-005. It can land against the current SDK-29 / AGP 4.1.3 toolchain. Gate G2 (REQ-003 AC-9) is declared achieved only when U-004, U-005, and U-006 have all merged to `master` and a single green CI run on `master` is on record.

## Supporting Documentation

- REQ-MODERNIZATION-003 — Establish CI Pipeline and Coverage Safety Net (AC-1, AC-2, AC-3, AC-8, AC-9)
- DES-MODERNIZATION-003 — CI Pipeline and Coverage Safety Net (Gate G2): §CI Pipeline as a Required Status Check, §Coverage Fitness Function — Per-Package 70%, §R8 On, §Supply-Chain Verification Metadata

## Integration Contract References

No CNTR-\* integration contracts are produced or consumed by this story. This design introduces no new cross-component runtime boundary. The behavioral contracts it establishes (the JaCoCo fitness function as an executable architectural constraint) are enforced by CI configuration, not by a runtime interface.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Draft; not finalized. The single load-bearing open item for review is the JaCoCo `element = 'PACKAGE'` glob semantics for sub-packages (`service/state/`, `service/exception/`): the implementer must verify at implementation time that the `**` wildcard in `com/zegoggles/smssync/service/**` recursively includes sub-packages under the JaCoCo version resolved by AGP 4.1.3, and must not assume a false-pass is a pass.
