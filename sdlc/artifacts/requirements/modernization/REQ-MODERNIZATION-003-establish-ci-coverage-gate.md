---
id: REQ-MODERNIZATION-003
title: "Establish CI Pipeline and Coverage Safety Net"
domain: modernization
status: approved
type: constraint
artifact_type: requirement
epic: EPIC-MODERNIZATION-001
priority: high
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-001
  - REQ-MODERNIZATION-002
related_stories: []
related_design_docs: []
traces_to: []
change_records: []
notes: >
  Traceability: OPS-001/PROC-001 (no CI), OPS-002/PROC-002 (coverage unmeasured),
  TECH-004/DEP-004 (test toolchain); MU-002; source assessment 20260529-modernization.
  Finding 3 of phase0-shippable-secure-context.md.
---

# REQ-MODERNIZATION-003 — Establish CI Pipeline and Coverage Safety Net

## Description

The project must establish a GitHub Actions CI pipeline that runs `./gradlew test lint jacocoTestReport assembleRelease` on every pull request, a JaCoCo coverage gate that fails the build when line or branch coverage on `service/`, `mail/`, or `auth/` falls below 70%, and characterization tests that pin `AuthPreferences.migrate()` trust-all behavior and `BackupJobs` retry (30/300 exponential backoff) and network-constraint semantics before any substrate refactor begins. This safety net — Gate G2 — is a governance constraint: no Phase-2 substrate work (WorkManager, Otto→Flow, k-9 ACL, Hilt DI) may merge to `master` until all conditions of this requirement are satisfied and a green CI run is on record.

## Context

The repository has no `.github/workflows/` directory — the only CI artefact is a stale Travis badge in the README. No automated build, test, lint, or coverage run is triggered on pull requests. The 35-class Robolectric suite (36 files) provides good package breadth but its coverage is entirely unmeasured: no JaCoCo plugin is declared in `app/build.gradle`, and the test-to-production LOC ratio is 0.33:1 (3,550 test LOC against 10,635 production LOC), below the 0.5–1.0 range typical of a well-covered Android engine.

Specific structural blind spots compound the general gap:

- `activity/MainActivity.java` (499 LOC, highest-complexity file by branch-token proxy) has no behavioral test class. Debt item TD-001.
- `auth/OAuth2Client.java` has no behavioral tests at all. Debt item TD-003.
- `service/SmsBackupServiceTest.java:212` contains an empty `// TODO` test body — a test method that registers as "present" but asserts nothing.
- `preferences/AuthPreferences.migrate()` (lines 276–288) performs a silent trust-all security downgrade for legacy `+ssl`/`+tls` IMAP users on upgrade, and has no characterization test pinning its behavior before the TLS hardening work in REQ-MODERNIZATION-002 is applied.
- `service/BackupJobs` retry semantics (30 s base / 300 s maximum exponential backoff) and network-constraint wiring have no pinning test before the WorkManager scheduler migration.
- `minifyEnabled false` at `app/build.gradle:33` means release builds assembled in development and CI never exercise R8 code-shrinking, leaving R8-induced regressions permanently undetectable until an explicit release build is cut.

The test toolchain is blocked on three fronts. `robolectric:4.3.1` (`app/build.gradle:66`) cannot execute tests against any SDK level above 29; this is a hard co-requisite of the SDK raise in REQ-MODERNIZATION-001 — both must move in the same build sweep or the test suite becomes unrunnable immediately after the SDK bump. `junit:4.12` (`app/build.gradle:65`) carries CVE-2020-15250, a security advisory against a test-framework class reachable from malicious test input in a shared CI environment. `mockito-all:1.10.17` (`app/build.gradle:68`) is a 2015 all-dependencies uber-jar that conflicts with `mockito-core:5.x` and blocks the Mockito upgrade needed for Kotlin coroutine interoperability in later phase tests.

No `gradle/verification-metadata.xml` exists. The dependency graph includes `maven.scijava.org` (a scientific-computing mirror, not a Google endpoint, serving `firebase-jobdispatcher:0.8.6`) and a raw JitPack SHA pin for the k-9 IMAP library; both are resolved without cryptographic integrity verification. Supply-chain tampering against either host is currently undetectable.

The consequence is architectural: every Phase-2 substrate swap (WorkManager, Otto-to-Flow, EncryptedSharedPreferences, Mail ACL) is a refactor against a codebase of unknown coverage with no automated regression signal. A behavioral change introduced during a branch-by-abstraction maneuver will not be detected until manual testing — if it is detected at all. This requirement is the governance gate for all Phase-2 work. The label "Gate G2" in the modernization roadmap reflects that no Phase-2 migration unit may be authorized until every acceptance criterion below is met and a green CI run is on record.

## Acceptance Criteria

1. **Given** a pull request is opened or updated against the `master` branch, **when** the GitHub Actions runner processes the PR event, **then** a workflow defined at `.github/workflows/ci.yml` runs and executes `./gradlew test lint jacocoTestReport assembleRelease` (or an equivalent chained task invocation that exercises all four phases without skipping any); the workflow is registered as a required status check on the `master` branch protection rule; and merge is blocked until this check passes.

2. **Given** the CI workflow executes `assembleRelease`, **when** the release APK or AAB is assembled, **then** the release build variant in `app/build.gradle` has `minifyEnabled true`, so R8 code-shrinking and class-name rewriting execute on every CI run. The current value of `false` at line 33 must be changed as part of this requirement; any R8 `keep` rule gaps or reflection-based breakages surfaced by enabling R8 must be resolved within this requirement and not deferred to a later phase.

3. **Given** JaCoCo is configured in `app/build.gradle` with a `jacocoTestReport` task, **when** `./gradlew jacocoTestReport` completes successfully, **then** a `jacocoTestCoverageVerification` task (or an equivalent enforcement block within the `jacocoTestReport` configuration) applies a coverage rule that: (a) requires at least 70% line coverage on the package `com/zegoggles/smssync/service/`; (b) requires at least 70% line coverage on the package `com/zegoggles/smssync/mail/`; (c) requires at least 70% line coverage on the package `com/zegoggles/smssync/auth/`; and (d) causes the Gradle build to exit non-zero if any of the three packages individually falls below 70%. A PR that adds production code to any of those packages without corresponding tests, dropping that package's coverage below 70%, must fail CI automatically and be blocked from merging.

4. **Given** a characterization-test requirement for `AuthPreferences.migrate()`, **when** the test suite runs against the current codebase — before any change to `preferences/AuthPreferences.java` or `mail/AllTrustedSocketFactory.java` is merged — **then** all of the following independent `@Test` methods exist in `app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java` and pass: (a) a test that calls `migrate()` on a preference state where the stored IMAP server URI contained `+ssl` or `+tls` and asserts that `AuthPreferences.isTrustAllCertificates()` (or the equivalent backing preference key `SERVER_TRUST_ALL_CERTIFICATES`) returns `false` after migration; (b) a test that calls `migrate()` on a clean preference state with no prior trust-all setting and asserts the same `false` result; and (c) a test that calls `migrate()` on a preference state where `SERVER_TRUST_ALL_CERTIFICATES` was already explicitly `true` and asserts the value is preserved as `true` (the test documents that user-explicit trust-all is a separate concern from the silent-downgrade path). These three tests must be passing and present in the test suite as a gate condition before any pull request implementing REQ-MODERNIZATION-002 is merged to `master`.

5. **Given** a characterization-test requirement for `BackupJobs` retry and constraint behavior, **when** the test suite runs against the current codebase — before any change to `service/BackupJobs.java`, `service/AlarmManagerDriver.java`, or `service/SmsJobService.java` is merged — **then** all of the following independent `@Test` methods exist in `app/src/test/java/com/zegoggles/smssync/service/BackupJobsTest.java` (or a new characterization-test class in the same package if `BackupJobsTest.java` does not exist) and pass: (a) a test that asserts the retry base interval is 30 seconds, matching the constant at `BackupJobs.java` (`RETRY_POLICY_EXPONENTIAL` base); (b) a test that asserts the retry maximum interval is 300 seconds, matching the maximum at the same location; (c) a test that verifies a backup job scheduled with `ON_UNMETERED_NETWORK` is not dispatched while only a metered network is available; and (d) a test that verifies a backup job scheduled with `ON_ANY_NETWORK` is dispatched when a metered network is available. Each of the four must be an independent `@Test` method that fails independently. These four tests must be passing and present in the test suite before any MU-005 (WorkManager migration) pull request is merged to `master`.

6. **Given** that `app/src/test/java/com/zegoggles/smssync/service/SmsBackupServiceTest.java` line 212 contains an empty `// TODO` test body, **when** this requirement is implemented, **then** that test method is replaced with an assertion-bearing body that exercises a named, meaningful behavior of `SmsBackupService` (the specific behavior must be documented in a comment in the test); or, if the path is untestable in isolation at this time, the method is replaced with a `@Ignore` annotation carrying a comment that states the blocking reason and a reference to the story or ticket that will address it. Under no circumstance does the `@Test`-annotated method remain with an empty body or a body consisting solely of comments and no assertions or explicit `@Ignore`. The CI workflow must not contain any mechanism that suppresses test-failure reporting for empty test methods.

7. **Given** the test toolchain upgrade scope, **when** `app/build.gradle` is updated as part of this requirement, **then** all of the following hold simultaneously after the change lands: (a) `robolectric` is at version `4.12.x` or higher (the exact minor version is the highest stable release available at implementation time); (b) `junit:junit` is at version `4.13.2`, resolving CVE-2020-15250; (c) the `mockito-all` dependency declaration is removed and replaced with `mockito-core:5.x` (the `mockito-all` uber-jar must not appear in the resolved `testRuntimeClasspath` dependency tree, verifiable via `./gradlew app:dependencies --configuration testRuntimeClasspath | grep mockito`); (d) `truth` is at version `1.4.x` or higher; and (e) all 35 existing Robolectric test classes pass without changes to their test logic (runner-configuration changes such as `@Config` annotations are permitted). All four dependency changes must land in the same pull request as the Robolectric upgrade, which in turn must land in the same pull request as the SDK raise in REQ-MODERNIZATION-001.

8. **Given** the supply-chain verification gap, **when** this requirement is implemented, **then** a `gradle/verification-metadata.xml` file is present at `gradle/verification-metadata.xml` in the repository, generated via `./gradlew --write-verification-metadata sha256` after all dependency upgrades in this requirement and REQ-MODERNIZATION-001 are applied; the CI workflow passes `--verify` (or Gradle's equivalent dependency-verification flag) so that the build exits non-zero if any resolved dependency's checksum does not match the recorded value; and the verification metadata is regenerated and committed whenever a dependency version changes.

9. **Given** Gate G2 governance, **when** any pull request targets Phase-2 substrate work — MU-005 (WorkManager migration), MU-006 (Otto-to-Flow eventing), MU-007 (Hilt DI formalization), or MU-008 (Mail ACL and k-9 unpin) — **then** all of the following conditions must be verifiably met before that pull request may be merged to `master`: the CI workflow (criterion 1) is passing on the target branch; the JaCoCo coverage gate (criterion 3) is enforced and passing; the `AuthPreferences.migrate()` characterization tests (criterion 4) are present and passing; and the `BackupJobs` retry/constraint characterization tests (criterion 5) are present and passing. A single green CI run on a no-op change to `service/SmsBackupService.java` that satisfies all four of these sub-conditions constitutes the observable Gate G2 milestone. This gate applies retroactively: if REQ-MODERNIZATION-001 or REQ-MODERNIZATION-002 is merged before Gate G2 is achieved, their Phase-2 gating obligation is satisfied when Gate G2 is satisfied, not before.

## Rationale

Every Phase-2 substrate swap in the modernization roadmap is a branch-by-abstraction maneuver: introduce a port, bind the existing implementation as a first adapter, add the new adapter, flip the binding. The safety of each flip depends entirely on having a characterization test suite that runs automatically on every change and a coverage gate that fails the build when behavioral coverage of the affected packages drops. Neither condition holds today. The 35-class Robolectric suite proves the project understands characterization testing as a practice; the missing pieces are measurement (JaCoCo), enforcement (a CI gate that blocks merge), and targeted backfill on the two highest-risk paths. `AuthPreferences.migrate()` is the path that introduced the active MITM exposure documented in REQ-MODERNIZATION-002; a characterization test for it must exist before that exposure is closed, so the closure is verifiably correct rather than optimistically assumed. `BackupJobs` retry and constraint logic is the scheduling contract that must be preserved verbatim by the WorkManager migration; a characterization test for it must exist before that migration begins, so any deviation from the contract is detected as a test failure rather than a field regression. Without these two tests, the Phase-2 swaps are refactors against unknown behavior. This requirement is not a quality nicety; it is the precondition that makes the substrate swaps safe, reversible, and auditable.

## Constraints

- **Robolectric co-requisite (REQ-MODERNIZATION-001):** Robolectric `4.3.1` cannot execute tests against SDK levels above 29. The Robolectric upgrade from `4.3.1` to `4.12.x` is therefore a hard co-requisite of the SDK raise in REQ-MODERNIZATION-001, not a sequenced prerequisite. Both must land in the same pull request. No intermediate state where `targetSdkVersion` exceeds 29 and Robolectric remains at `4.3.1` may be merged to `master`.

- **AuthPreferences characterization test precedes REQ-MODERNIZATION-002:** Criterion 4 (the `migrate()` characterization test) is the behavioral safety net that makes REQ-MODERNIZATION-002 (elimination of the trust-all TLS path) safe to implement. The test must be present and passing on the current codebase — before any line of `AllTrustedSocketFactory.java` or `AuthPreferences.migrate()` is deleted or modified — so that the deletion is proven correct rather than assumed correct.

- **BackupJobs characterization test precedes MU-005:** Criterion 5 (the `BackupJobs` retry/constraint characterization test) must be present and passing before any WorkManager migration pull request is merged. The scheduling contract (30/300 backoff, REPLACE semantics, network constraints) must be machine-verifiable before the scheduler implementation is swapped.

- **R8 breakages are in-scope for this requirement:** Enabling `minifyEnabled true` (criterion 2) may surface missing ProGuard/R8 keep rules or reflection-based class-loading failures. Resolving these is explicitly in-scope for this requirement and must not be deferred. The test suite must pass with R8 enabled before Gate G2 is declared.

- **Gate G2 blocks all Phase-2 work:** Gate G2 (criterion 9) is a process constraint enforced by team agreement and enforced by the CI system, not by a single automated check. The gate is declared achieved only when a CI run on the `master` branch passes all four sub-conditions simultaneously.

## Notes

**Traceability:**

| Finding ID | Gap Title | Source Location |
|------------|-----------|-----------------|
| OPS-001 / PROC-001 | No CI/CD — no automated regression gate | `gaps.md §Process Gaps §OPS-001` |
| OPS-002 / PROC-002 | Coverage unmeasured (0.33:1 LOC) — no fitness function | `gaps.md §Process Gaps §OPS-002` |
| TECH-004 / DEP-004 | Outdated test toolchain blocks SDK bump | `gaps.md §Technology Gaps §DEP-004` |

**Migration unit:** MU-002 (Test-Harness & Coverage Gate) — `migration-units.md §MU-002`.

**Source assessment:** `20260529-modernization`.

**Key file locations (paths relative to repo root):**

| File | Relevant Line(s) | Relevance |
|------|-----------------|-----------|
| `app/build.gradle` | 33 | `minifyEnabled false` — must become `true` (AC 2) |
| `app/build.gradle` | 65 | `junit:4.12` carrying CVE-2020-15250 (AC 7) |
| `app/build.gradle` | 66 | `robolectric:4.3.1` hard-capping SDK ≤ 29 (AC 7) |
| `app/build.gradle` | 67 | `truth:0.39` — upgrade to 1.4.x (AC 7) |
| `app/build.gradle` | 68 | `mockito-all:1.10.17` uber-jar — replace with mockito-core:5.x (AC 7) |
| `app/src/test/…/SmsBackupServiceTest.java` | 212 | Empty `// TODO` test body (AC 6) |
| `app/src/main/…/preferences/AuthPreferences.java` | 276–288 | `migrate()` silent trust-all path — subject of AC 4 characterization test |
| `app/src/main/…/service/BackupJobs.java` | ~188, ~199, ~205 | Retry 30/300, REPLACE semantics, network constraints — subject of AC 5 |

**Verification note:** Every acceptance criterion in this requirement is verifiable by inspection of artefacts (workflow YAML, `app/build.gradle` configuration, test source files, CI run logs) or by running a deterministic Gradle command (`./gradlew dependencies --configuration testRuntimeClasspath`). No acceptance criterion uses qualitative language. Coverage thresholds are expressed as specific numeric percentages against named Java package paths. Retry delay constants are expressed as specific integer values taken from verified source-file locations.
