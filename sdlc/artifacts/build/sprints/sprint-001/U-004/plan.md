---
artifact_type: plan
story_id: "U-004"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-02"
---

# Plan: U-004 — GitHub Actions CI + JaCoCo 70% Gate + R8

## Scope

This story introduces three changes against the current SDK-29 / AGP 4.1.3 toolchain:

1. **`.github/workflows/ci.yml`** — New GitHub Actions workflow running the four-phase
   Gradle invocation on `pull_request` and `push` to `master`.

2. **`app/build.gradle`** — Apply `jacoco` plugin; add `jacocoTestReport` and
   `jacocoTestCoverageVerification` tasks with three independent per-package 70% LINE
   coverage rules; change `minifyEnabled false` → `true` on the release build type;
   add `testCoverageEnabled true` on debug.

3. **`app/proguard-rules.pro`** — Append keep rules for Otto `@Subscribe`/`@Produce`
   reflection, `SmsJobService` firebase-jobdispatcher entry point, and
   `AuthMode`/`DataType` enum `valueOf` paths.

## Files to Modify

| File | Change |
|------|--------|
| `.github/workflows/ci.yml` | Create: CI workflow |
| `app/build.gradle` | Apply jacoco plugin; add JaCoCo tasks; minifyEnabled true; testCoverageEnabled |
| `app/proguard-rules.pro` | Append R8 keep rules |

## Files NOT in Scope

- Test toolchain upgrades (Robolectric, JUnit, Mockito, Truth) — U-005
- Characterization tests for AuthPreferences/BackupJobs — U-006
- gradle/verification-metadata.xml — cannot be generated in this env (JDK 22, needs JDK 11)

## Implementation Approach

### CI Workflow

- Trigger: `pull_request` → `master` and `push` → `master`
- Runner: `ubuntu-22.04` (pinned — not `ubuntu-latest`)
- JDK: 11 (Temurin) via `actions/setup-java@v4`
- Cache: `~/.gradle/wrapper` and `~/.gradle/caches` keyed on gradle file hashes
- Gradle invocation: `./gradlew test lint jacocoTestReport assembleRelease`
  (Note: `--verify` omitted — `gradle/verification-metadata.xml` does not exist yet;
  must be generated and added in a follow-up JDK-11 environment before `--verify` is added)
- No `continue-on-error: true` or `|| true` shell escapes
- Job name `build` under workflow name `CI` → required-status-check string is `CI / build`

### JaCoCo Configuration

- Plugin: `apply plugin: 'jacoco'` with `toolVersion = "0.8.7"`
- `testCoverageEnabled true` on debug build type (enables `.exec` instrumentation)
- `jacocoTestReport` task: depends on `testDebugUnitTest`, produces HTML/XML reports,
  `finalizedBy 'jacocoTestCoverageVerification'`
- `jacocoTestCoverageVerification` task: three independent `rule {}` blocks:
  - `com/zegoggles/smssync/service/**` — LINE COVEREDRATIO >= 0.70
  - `com/zegoggles/smssync/mail/**` — LINE COVEREDRATIO >= 0.70
  - `com/zegoggles/smssync/auth/**` — LINE COVEREDRATIO >= 0.70
- Glob `**` used to include sub-packages (`service/state/`, `service/exception/`)
  per the story's Technical Notes

### R8 Keep Rules

Three rule groups appended to `app/proguard-rules.pro`:

1. Otto `@Subscribe`/`@Produce` — `-keep @com.squareup.otto.Subscribe class * { *; }` +
   `-keepclassmembers` for annotated methods
2. `SmsJobService` — `-keep public class com.zegoggles.smssync.service.SmsJobService { *; }`
3. Enum `valueOf`/`values()` — `-keepclassmembers enum com.zegoggles.smssync.** { ... }`

## Constraints

- Must work against AGP 4.1.3 / SDK 29 — no upgrades
- Cannot run Gradle locally (JDK 22 available; AGP 4.1.3 requires JDK 11)
- Gradle config verified syntactically; runtime behaviour verified on GitHub Actions

## Verification Plan

| Check | Method | Runnable locally? |
|-------|--------|-------------------|
| ci.yml is valid YAML | Node.js structural parse | YES |
| ci.yml has no tabs | Node.js check | YES |
| ci.yml correct triggers/runner/cache/no-escape | Node.js regex checks | YES |
| app/build.gradle syntax | Static inspection | YES |
| JaCoCo tasks register without error | `./gradlew tasks` | NO (JDK 22) |
| Coverage gate fails below threshold | `./gradlew jacocoTestReport` | NO (JDK 22) |
| R8 assembleRelease succeeds | `./gradlew assembleRelease` | NO (JDK 22) |
| GitHub required-status-check registration | GitHub repo settings / `gh api` | NO (needs remote) |
