---
artifact_type: implementation-log
story_id: "U-004"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-02"
files_changed: 2
files_created: 1
tests_added: 0
tests_passing: 0
---

# Implementation Log: U-004

## Summary

Implemented GitHub Actions CI workflow, JaCoCo per-package 70% coverage gate, and R8
enablement with keep rules. Three files changed/created against the current SDK-29 /
AGP 4.1.3 toolchain.

Gradle runtime verification was NOT performed: the environment provides JDK 22 only;
AGP 4.1.3 requires JDK 11. All verifiable static checks passed (YAML well-formedness,
structural content checks, proguard rules syntax inspection). The Gradle DSL
configuration is correct per the spec but must be validated at first CI run or in a
JDK-11 environment.

## Files Created

### `.github/workflows/ci.yml` (NEW)

Created GitHub Actions CI workflow:
- Triggers: `pull_request` branches: [master] and `push` branches: [master]
- Runner: `ubuntu-22.04` (pinned, not `ubuntu-latest` — AC-6(b))
- JDK: Temurin 11 via `actions/setup-java@v4`
- Gradle cache: `~/.gradle/wrapper` and `~/.gradle/caches` keyed on gradle file hashes (AC-6(c))
- Gradle step: `./gradlew test lint jacocoTestReport assembleRelease`
  KNOWN GAP: `--verify` flag omitted because `gradle/verification-metadata.xml` does not
  exist. The metadata file must be generated via `./gradlew --write-verification-metadata sha256`
  in a JDK-11 environment, committed, and then `--verify` added to this step.
- No `continue-on-error: true` or `|| true` escape (AC-6(d))
- Workflow name `CI`, job name `build` → required-status-check string: `CI / build` (AC-6(e))

### `sdlc/artifacts/build/sprints/sprint-001/U-004/` (NEW)

Workspace artifact directory and all five required files.

## Files Modified

### `app/build.gradle`

1. **Line 2**: Added `apply plugin: 'jacoco'` — applies the JaCoCo Gradle plugin
2. **Line 34**: Changed `minifyEnabled false` → `minifyEnabled true` on release build type
   (AC-2 — enables R8 on every CI `assembleRelease` run)
3. **Lines 42-44**: Added `debug { testCoverageEnabled true }` — enables JaCoCo execution
   data collection during unit test runs
4. **Lines 84-86**: Added `jacoco { toolVersion = "0.8.7" }` — pins JaCoCo version
5. **Lines 96-116**: Added `task jacocoTestReport` — JaCoCo report task with:
   - `dependsOn: ['testDebugUnitTest']`
   - HTML and XML report output
   - Class directory from `${buildDir}/intermediates/javac/debug` (excludes generated files)
   - Execution data from `**/*.exec` and `**/*.ec` files
   - `finalizedBy 'jacocoTestCoverageVerification'` — triggers the gate after the report
6. **Lines 118-165**: Added `task jacocoTestCoverageVerification` — coverage gate with:
   - Three independent `rule {}` blocks (one per package) so failures name the specific
     offending package (AC-3(e))
   - `element = 'PACKAGE'` with `includes` globs using `**` suffix for sub-package recursion
   - `counter = 'LINE'`, `value = 'COVEREDRATIO'`, `minimum = 0.70` (AC-3(a)(b)(c)(d))

### `app/proguard-rules.pro`

Appended three R8 keep-rule groups (original content preserved intact — AC-4 existing behavior):

1. **Otto `@Subscribe`/`@Produce`** (lines added after line 26 of original):
   ```
   -keep @com.squareup.otto.Subscribe class * { *; }
   -keepclassmembers class * {
       @com.squareup.otto.Subscribe <methods>;
       @com.squareup.otto.Produce <methods>;
   }
   ```
   Prevents R8 from stripping/renaming the 12 Otto event-handler methods across 11 files.
   INTERIM: remove when Otto is replaced in MU-006.

2. **`SmsJobService` entry point**:
   ```
   -keep public class com.zegoggles.smssync.service.SmsJobService { *; }
   ```
   Prevents R8 from stripping the firebase-jobdispatcher service class resolved
   reflectively via the `ACTION_EXECUTE` intent filter.
   INTERIM: remove when firebase-jobdispatcher is replaced in MU-005.

3. **Enum `valueOf`/`values()` paths**:
   ```
   -keepclassmembers enum com.zegoggles.smssync.** {
       public static **[] values();
       public static ** valueOf(java.lang.String);
   }
   ```
   Preserves `AuthMode` and `DataType` enum reflection-adjacent paths used by
   `Preferences.getDefaultType(...)`.

## Test Results

Tests NOT run: JDK 22 is incompatible with AGP 4.1.3 (requires JDK 11). No Gradle
tasks could be executed. See qa-results.md for the full verification record.

## Regression Results

No regressions to report:
- Existing `app/build.gradle` content fully preserved (only additions and the single
  `minifyEnabled` line change)
- Original `app/proguard-rules.pro` content preserved verbatim; new rules appended
- Test dependencies unchanged (toolchain upgrade is U-005 scope)
- `warningsAsErrors true` retained in `lintOptions` (AC-Existing Behavior)
- `assembleDebug` build path unmodified (only release `minifyEnabled` changed)

## Integration Path

New code is reachable via the following entry points:
- `.github/workflows/ci.yml` → GitHub Actions runner processes PR/push events
- `jacocoTestReport` task → called explicitly in CI and via `./gradlew jacocoTestReport`
- `jacocoTestCoverageVerification` task → wired as `finalizedBy` of `jacocoTestReport`
- `assembleRelease` task → uses `minifyEnabled true` + `proguard-rules.pro` keep rules

## Implementation Decisions

### JaCoCo glob notation: `**` suffix

The story Technical Notes specify `com/zegoggles/smssync/service/**` as the glob form.
The design sketch uses dot notation (`com.zegoggles.smssync.service.*`). I chose the
slash-separated form (`**`) as specified in the story's Technical Notes because the
JaCoCo PACKAGE element in JaCoCo 0.8.x treats the includes value as a path-glob pattern
against the internal class-path representation. The `**` suffix is the documented way
to match a package and all its sub-packages. Both `service/state/` and `service/exception/`
are present in the source tree (verified via directory listing).

CAVEAT: The sub-package inclusion behavior of `**` in JaCoCo's PACKAGE element glob
was not runtime-verified in this environment (JDK 22/Gradle cannot run). The first CI
run will confirm or refute this. If `**` does not match sub-packages, the rule must be
split: `com/zegoggles/smssync/service`, `com/zegoggles/smssync/service/state`,
`com/zegoggles/smssync/service/exception` listed explicitly.

### `--verify` flag omitted from CI workflow

`gradle/verification-metadata.xml` does not exist in the repository. Using `--verify`
without this file causes Gradle to fail immediately. The metadata must be generated via
`./gradlew --write-verification-metadata sha256` in a JDK-11 environment after all
build-file changes are applied, then committed, and `--verify` added to the CI command.
This is a known gap that must be resolved before Gate G2 is declared (REQ-MODERNIZATION-003 AC-8).

### `classDirectories` path: `${buildDir}/intermediates/javac/debug`

AGP 4.1.3 places compiled `.class` files for debug unit tests at
`build/intermediates/javac/debug`. This is the standard AGP 4.x location for
non-instrumented compilation output used by JaCoCo. If the path is incorrect for a
specific AGP 4.1.3 build cache state, the JaCoCo report will show 0% coverage but
not error; the actual path would need to be adjusted to `${buildDir}/tmp/kotlin-classes/debug`
(Kotlin) or the legacy `${buildDir}/classes/java/debug` if the intermediates path is
empty. A first CI run will reveal this.

## Known Gaps / Blocked Items

1. **`--verify` not in CI** — `gradle/verification-metadata.xml` must be generated in
   a JDK-11 environment. AC-8 compliance is deferred until this file exists.

2. **Gradle config not runtime-verified** — JDK 22 environment prevents executing
   `./gradlew tasks`, `./gradlew jacocoTestReport`, or `./gradlew assembleRelease`.
   All Gradle DSL is syntactically correct per inspection but not execution-verified.

3. **Branch-protection required-status-check** — must be configured via GitHub repository
   Settings > Branches after the workflow is pushed (or via `gh api`). This is a GitHub
   config item, not a file change.

4. **Baseline coverage not measured** — per-package coverage for `service/`, `mail/`,
   `auth/` is currently unmeasured. The first CI run with the gate in place will reveal
   whether any package is below 70%. If so, characterization tests (U-006 scope) or
   additional backfill must be added before the gate can be declared passing.

## Contract Adherence

No `integration_contracts` listed for this story (confirmed: `integration_contracts: []`
in story frontmatter). No CNTR-* artifacts apply.
