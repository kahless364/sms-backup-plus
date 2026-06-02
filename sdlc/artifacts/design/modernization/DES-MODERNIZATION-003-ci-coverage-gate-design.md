---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-003
related_stories: []
related_design_docs: []
integration_contracts: []
change_records: []
type: ''
id: DES-MODERNIZATION-003
title: ''
domain: modernization
---

# DES-MODERNIZATION-003: CI Pipeline and Coverage Safety Net (Gate G2)

## Overview

This design realizes REQ-MODERNIZATION-003 (MU-002 — "Test-Harness & Coverage Gate").
It establishes the automated regression substrate — a GitHub Actions pipeline, a JaCoCo
coverage fitness function, a test-toolchain uplift, supply-chain verification metadata,
and two named characterization tests — that every Phase-2 substrate swap depends on. It
is a **process-and-build** design: it changes `app/build.gradle`, the root `build.gradle`,
adds `.github/workflows/ci.yml` and `gradle/verification-metadata.xml`, and backfills two
test classes. It introduces **no new production runtime component and no cross-component
runtime boundary**. The design covers three areas the requirement gates on: (1) the CI +
coverage architecture, (2) how that gate integrates with the SDK raise (DES-001) and the
trust-all removal (DES-002), and (3) the validation evidence that proves the gate works.

> **System-architecture · Integration-design · Design-validation** sections below map
> one-to-one to the `applicable_prompts` in this artifact's frontmatter. They trace to
> REQ-MODERNIZATION-003 AC-1 through AC-9.

## Context

The repository has **no `.github/workflows/` directory** (verified this session — only a
stale `.travis.yml` running `./gradlew build` against `android-29` exists, and a dead
Travis badge in the README). There is **no JaCoCo plugin** declared anywhere in the build
(verified: `grep jacoco *.gradle` returns nothing). The release variant has
`minifyEnabled false` (`app/build.gradle:33`, verified), so R8 never runs in development
or CI and R8-induced regressions are undetectable until a manual release build is cut. The
test toolchain is pinned to versions that block the SDK raise and carry a CVE
(`app/build.gradle:65-68`, verified: `junit:4.12`, `robolectric:4.3.1`, `truth:0.39`,
`mockito-all:1.10.17`). No `gradle/verification-metadata.xml` exists, and the dependency
graph resolves through `maven.scijava.org` and a raw JitPack SHA without integrity checks
(`build.gradle:24,27`, verified).

Every Phase-2 migration unit (MU-005 WorkManager, MU-006 Otto→Flow, MU-007 Hilt, MU-008
Mail ACL) is a branch-by-abstraction maneuver against a codebase of **unmeasured**
coverage (test:prod LOC ratio 0.33:1 per gaps.md OPS-002). Without a coverage gate and two
specific pinning tests, each swap is a refactor against unknown behavior. REQ-MODERNIZATION-003
names this the **Gate G2** governance constraint: no Phase-2 PR may merge to `master` until
the pipeline, the coverage gate, and the two characterization tests are present, enforced,
and green.

A **first-class design tension** surfaced by reading the source must be recorded here,
because it governs the order in which DES-002 and DES-003 work can land. `AuthPreferences.migrate()`
(`preferences/AuthPreferences.java:276-288`, verified) currently does this:

```java
void migrate() {
    if (useXOAuth()) { return; }
    if ("+ssl".equals(getServerProtocol()) || "+tls".equals(getServerProtocol())) {
        preferences.edit()
            .putBoolean(SERVER_TRUST_ALL_CERTIFICATES, true)   // line 284 — silent downgrade
            .putString(SERVER_PROTOCOL, getServerProtocol()+"+")
            .commit();
    }
}
```

The current code **sets `SERVER_TRUST_ALL_CERTIFICATES = true`** for legacy `+ssl`/`+tls`
users. REQ-MODERNIZATION-003 AC-4 specifies a characterization test that asserts
`isTrustAllCertificates()` returns **`false`** after migration. These do not agree on the
current codebase. AC-4 is therefore **not** a pin of present behavior — it is a pin of the
**post-DES-002 target** behavior. This design resolves the contradiction explicitly in
Integration Design below (the AC-4 test is authored red against current code and goes green
only when DES-002 rewrites `migrate()`), so an implementer does not silently "fix" the test
to match the insecure status quo. This is the single most important reading-derived finding
in this design and it is load-bearing for the DES-002 sequencing constraint.

> **NOTE ON PACKAGE LOCATION (corrects the requirement's prose):** REQ-MODERNIZATION-003
> AC-3(c) gates coverage on package `com/zegoggles/smssync/auth/`, and AC-4's test lives at
> `…/preferences/AuthPreferencesTest.java`. `AuthPreferences.java` is in the **`preferences/`**
> package, **not** `auth/` (verified: `auth/` contains only `OAuth2Client`, `OAuth2Token`,
> `TokenRefresher`, `TokenRefreshException`). The AC-4 characterization test correctly targets
> `preferences/`; the AC-3 70% gate on `auth/` is a **separate** package threshold that the
> AC-4 test does **not** contribute coverage to. This design keeps the two concerns distinct
> so the implementer does not assume the migrate() test lifts `auth/` coverage. See
> Design Validation.

## Design

### CI Pipeline as a Required Status Check (AC-1)

A single workflow at `.github/workflows/ci.yml` runs on `pull_request` (and `push` to
`master` for the Gate-G2 milestone record). It executes the four required tasks in one
chained Gradle invocation so none can be skipped:

```
./gradlew test lint jacocoTestReport assembleRelease --verify
```

- `test` — the full Robolectric unit suite (35 classes).
- `lint` — Android Lint with `warningsAsErrors true` (`app/build.gradle:41`, verified)
  retained; a `lint-baseline.xml` is the managed escape valve during the SDK migration
  (gaps.md OPS-003) so newly surfaced deprecations do not block the gate itself.
- `jacocoTestReport` — produces the coverage report **and** triggers the verification rule
  (see next decision) so a sub-threshold package fails the same task.
- `assembleRelease` — assembles the release variant with R8 enabled (see "R8 On" decision),
  exercising code-shrinking on every PR.
- `--verify` — Gradle dependency verification against `gradle/verification-metadata.xml`
  (AC-8).

The workflow is registered as a **required status check** on the `master` branch-protection
rule; merge is blocked until it passes. Branch protection is a GitHub repository setting,
not a file in the tree — this design records it as a configuration deliverable of the story
(the implementer must set it via repo settings / `gh api`), because AC-1 explicitly requires
it and a workflow that exists but is not "required" does not satisfy the gate.

### Coverage Fitness Function — Per-Package 70% (AC-3) · ADR: coverage-fitness-function-as-gate

**Decision.** Configure JaCoCo in `app/build.gradle` and bind a
`jacocoTestCoverageVerification` rule into the `jacocoTestReport` execution so that
**`jacocoTestReport` itself fails non-zero** when any of the three engine packages
falls below 70% line coverage. The gate is a *fitness function* (Ford/Parsons/Kua): an
executable architectural constraint, evaluated automatically on every change, that fails
the build rather than emitting an advisory report.

**Why this shape.** The requirement (AC-3d) demands the build exit non-zero if **any** of
the three packages **individually** drops below 70%. JaCoCo's `violationRules` supports
exactly this via **per-package `element = "PACKAGE"`** rules with an `includes` filter — one
rule scope per package so each is evaluated independently and a failure names the offending
package. A single aggregate rule across all three would let a high-coverage package mask a
low-coverage one; that is explicitly rejected.

The three gated packages, expressed as JaCoCo class-path globs (note `**` to include
sub-packages such as `service/state/` and `service/exception/` — verified to exist):

| Requirement clause | JaCoCo `includes` glob | Verified package contents |
|--------------------|------------------------|---------------------------|
| AC-3(a) `service/` ≥ 70% | `com/zegoggles/smssync/service/**` | BackupJobs, BackupTask, RestoreTask, SmsBackupService, … + `state/`, `exception/` |
| AC-3(b) `mail/` ≥ 70% | `com/zegoggles/smssync/mail/**` | MessageConverter, MessageGenerator, BackupImapStore, AllTrustedSocketFactory, … |
| AC-3(c) `auth/` ≥ 70% | `com/zegoggles/smssync/auth/**` | OAuth2Client, OAuth2Token, TokenRefresher, TokenRefreshException |

```groovy
// app/build.gradle (sketch — exact JaCoCo wiring is the implementer's; semantics are fixed)
apply plugin: 'jacoco'

tasks.register('jacocoTestCoverageVerification', JacocoCoverageVerification) {
    dependsOn 'testDebugUnitTest'
    executionData fileTree(buildDir).include('**/*.exec')
    ['service', 'mail', 'auth'].each { pkg ->
        violationRules {
            rule {
                element = 'PACKAGE'
                includes = ["com.zegoggles.smssync.${pkg}.*"]   // NB: confirm JaCoCo glob semantics for sub-packages at impl time
                limit { counter = 'LINE'; value = 'COVEREDRATIO'; minimum = 0.70 }
            }
        }
    }
}
tasks.named('jacocoTestReport') { finalizedBy 'jacocoTestCoverageVerification' }
```

> **Implementer caveat (verify, do not assume):** JaCoCo's `element = 'PACKAGE'` with
> `includes` matches package nodes by name; the exact glob form for recursively including
> sub-packages (`service.state`, `service.exception`) must be confirmed against the JaCoCo
> version resolved by AGP at implementation time. The **semantic** requirement is fixed
> (each of the three packages, sub-packages included, evaluated independently, 70% line,
> non-zero on breach); the precise glob string is an implementation detail to verify, not
> invent. This is flagged so the gate does not silently exclude `service/state/` and report
> a false pass.

**Baseline-coverage risk (called out, not hidden):** coverage is currently *unmeasured*.
There is a real risk that one or more of the three packages is **below 70% today**, which
would make `jacocoTestReport` fail on the very first CI run. This is by design — the gate is
honest — but it means the story must, as its first executable step, **measure** current
per-package coverage and, if a package is under 70%, backfill characterization tests until
it clears. AC-3's 70% is the floor the safety net must reach, not an assumption that it is
already met. The two named characterization tests (AC-4, AC-5) are necessary but may not be
sufficient to reach 70% on `service/` and `auth/`; the story owns whatever additional
backfill the measurement reveals.

### R8 On — `minifyEnabled true` (AC-2)

**Decision.** Change `app/build.gradle:33` from `minifyEnabled false` to `minifyEnabled true`
on the `release` variant, so `assembleRelease` runs R8 on every CI run. Any R8 `keep`-rule
gap or reflection-based breakage surfaced by enabling R8 is resolved **within this design's
story** (`app/proguard-rules.pro`), not deferred (AC-2, and Constraints in the requirement).

**Reflection surfaces to audit for keep rules (from source read this session):** Otto uses
reflection on `@Subscribe`/`@Produce` methods (12 import sites across 11 files) — R8 may
strip annotated methods; `firebase-jobdispatcher` reflectively resolves the `SmsJobService`
via the `ACTION_EXECUTE` intent filter; `AuthMode`/`DataType` enums are read via
`Preferences.getDefaultType(...)` reflection-adjacent valueOf paths. These are the high-risk
keep-rule candidates the story must validate the suite against with R8 enabled before Gate G2
is declared. (Otto and jobdispatcher are removed by later MUs, but they are present **now**
and R8 is enabled **now**, so the keep rules are required in the interim.)

### Test-Toolchain Uplift (AC-7) — co-requisite with the SDK raise

**Decision.** In the same pull request as the DES-001 SDK raise, change `app/build.gradle:65-68`:

| Line | Current (verified) | Target (AC-7) | Reason |
|-----:|--------------------|---------------|--------|
| 66 | `org.robolectric:robolectric:4.3.1` | `4.12.x` (highest stable at impl time) | 4.3.1 cannot run tests against SDK > 29 — **hard co-requisite of DES-001** |
| 65 | `junit:junit:4.12` | `junit:junit:4.13.2` | resolves CVE-2020-15250 |
| 68 | `org.mockito:mockito-all:1.10.17` | `org.mockito:mockito-core:5.x` | retire 2015 uber-jar; unblock Mockito 5 / coroutine interop |
| 67 | `com.google.truth:truth:0.39` | `com.google.truth:truth:1.4.x` | currency; Truth API used across the suite |

The `mockito-all` uber-jar must not appear in the resolved `testRuntimeClasspath`
(verifiable: `./gradlew app:dependencies --configuration testRuntimeClasspath | grep mockito`).
All 35 existing test classes must pass **without logic changes** (runner/`@Config`
annotation changes permitted — note `AppTest` already uses `@Config(sdk = 18)`, verified, so
`@Config`-level adjustments are an established pattern in this suite).

### Supply-Chain Verification Metadata (AC-8)

**Decision.** Generate `gradle/verification-metadata.xml` via
`./gradlew --write-verification-metadata sha256` **after** all dependency upgrades in this
design and DES-001 are applied (so the recorded checksums match the final graph). CI passes
`--verify` so a checksum mismatch on any resolved dependency — including the JitPack k-9 SHA
(`build.gradle` jitpack repo, verified) and the scijava-hosted jobdispatcher — exits the
build non-zero. The metadata is regenerated and committed whenever a dependency version
changes (a maintenance obligation recorded in the story's Definition of Done).

### Characterization Tests as Behavioral Contracts (AC-4, AC-5)

These two tests are the behavioral specifications that Phase-2 dependents rely on. They are
detailed under Integration Design (their precedence relationships to DES-002 and DES-005 are
the integration concern). The empty `// TODO` test body in `SmsBackupServiceTest`
(AC-6 — verified: multiple empty `// TODO` `@Test` bodies exist in that file) is replaced
with an assertion-bearing body or an `@Ignore` carrying a blocking reason; no empty
`@Test`-annotated method may remain, and CI must not suppress empty-test reporting.

## Architecture

```
   Pull request ──▶ GitHub Actions runner (.github/workflows/ci.yml)
                         │
                         ▼
        ./gradlew test  lint  jacocoTestReport  assembleRelease  --verify
                │        │          │                  │             │
                ▼        ▼          ▼                  ▼             ▼
        Robolectric   Android   JaCoCo report   R8 (minify=true)  Gradle dep
        35 classes    Lint      + per-package    on release       verification
        (incl. the    (baseline) 70% rule:       variant          (sha256 vs
         AC-4/AC-5    ─────────  service/ mail/  ──────────────   verification-
         pins)                   auth/ → FAIL                     metadata.xml)
                                 non-zero if any
                                 pkg < 70%
                         │
                         ▼
              Required status check on `master` branch protection
                         │
                         ▼
              Gate G2: blocks every Phase-2 PR (MU-005/006/007/008)
```

**Gate G2 milestone (AC-9):** a single green CI run on `master` — observable via a no-op
change to `service/SmsBackupService.java` — in which the workflow passes, the coverage gate
is enforced and passing, and both characterization tests are present and green, constitutes
the achieved gate. Until that run exists on record, no Phase-2 substrate PR may merge.

## Components

| Component | File (path relative to repo root) | Responsibility | Status |
|-----------|-----------------------------------|----------------|--------|
| CI workflow | `.github/workflows/ci.yml` | Run the 4 tasks + `--verify` on every PR; required check | New |
| Coverage fitness function | `app/build.gradle` (JaCoCo block) | Per-package 70% line gate; non-zero on breach | New config |
| R8 enablement | `app/build.gradle:33` (`minifyEnabled true`) + `app/proguard-rules.pro` | Run R8 on `assembleRelease`; keep-rule coverage | Modified |
| Toolchain uplift | `app/build.gradle:65-68` | Robolectric 4.12.x, junit 4.13.2, mockito-core 5.x, truth 1.4.x | Modified |
| Verification metadata | `gradle/verification-metadata.xml` | sha256 integrity of resolved deps | New |
| AuthPreferences characterization | `app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java` | Pin `migrate()` trust-all post-state (AC-4) | Backfill (file exists) |
| BackupJobs characterization | `app/src/test/java/com/zegoggles/smssync/service/BackupJobsTest.java` | Pin retry 30/300 + network constraints (AC-5) | Backfill (file exists) |
| Empty-test remediation | `app/src/test/java/com/zegoggles/smssync/service/SmsBackupServiceTest.java` | Replace empty `// TODO` body / `@Ignore` (AC-6) | Modified |
| Branch protection | GitHub repo setting (not a file) | Make CI a required status check (AC-1) | Config |

## Interfaces

This design adds **no production API**. The "interfaces" it introduces are the two
characterization tests, which act as behavioral contracts on existing production code, and
the CI task contract. They are specified in Integration Design.

---

## Integration Design

### Gate G2 blocks every Phase-2 PR

Gate G2 is the integration point between this design and all of Phase 2. The coupling is
**process-enforced via the CI required-status-check** plus the two characterization tests:

| Phase-2 unit | Design | What G2 protects it against |
|--------------|--------|-----------------------------|
| MU-005 WorkManager | DES-005 | Scheduler contract drift (retry 30/300, network constraints) — pinned by AC-5 |
| MU-006 Otto→Flow | DES-007 | Silent eventing-state regressions — caught by the coverage gate on `service/` |
| MU-007 Hilt DI | DES-008 | Construction-graph wiring regressions — caught by the suite + coverage gate |
| MU-008 Mail ACL | DES-009 | k-9 boundary regressions — caught by `mail/` coverage gate |

No Phase-2 PR may merge until: CI passes (AC-1), the JaCoCo gate is enforced and passing
(AC-3), the AC-4 migrate() test is present and passing, and the AC-5 BackupJobs test is
present and passing (AC-9). This is a hard sequencing edge in the migration-units DAG:
**MU-002 blocks MU-003, MU-004, MU-005, MU-006, MU-008** (migration-units.md §Dependency Matrix,
verified).

### Robolectric uplift is a co-requisite of the SDK raise (DES-001)

This is not a "DES-003-then-DES-001" sequence; it is a **single combined build sweep**.
`robolectric:4.3.1` cannot execute tests against any SDK level above 29 (REQ-MODERNIZATION-003
Context, and migration-units.md MU-001↔MU-002 co-requisite, verified). DES-001 raises
`targetSdkVersion` above 29; the instant it does, the 4.3.1 suite becomes unrunnable. Therefore:

- The Robolectric `4.3.1 → 4.12.x` bump (AC-7a) **must land in the same pull request** as the
  DES-001 SDK raise.
- No intermediate `master` state where `targetSdkVersion > 29` and Robolectric is still
  `4.3.1` may exist (REQ Constraints, verified).
- The other three toolchain bumps (junit, mockito-core, truth) ride in the same PR (AC-7e).
- `verification-metadata.xml` is generated **after** this combined sweep so its checksums
  match the post-raise graph (AC-8).

This is the one place where DES-003 and DES-001 are not independently shippable. The CI
workflow and JaCoCo gate (the rest of DES-003) **are** independently shippable and can land
before the SDK sweep, running against the existing SDK-29 toolchain.

### Characterization test #1 — `AuthPreferences.migrate()` protects DES-002

DES-002 (transport-security hardening) deletes `AllTrustedSocketFactory` and **rewrites
`migrate()`** so it never silently enables trust-all. The AC-4 test is the safety net that
makes that rewrite provably correct. Three independent `@Test` methods in
`preferences/AuthPreferencesTest.java` (which currently has only `testStoreUri` and
`testStoreUriWithXOAuth2`, verified):

1. **Legacy `+ssl`/`+tls` state →** after `migrate()`, `isTrustAllCertificates()` (backing
   key `SERVER_TRUST_ALL_CERTIFICATES`) returns **`false`**.
2. **Clean state, no prior trust-all →** after `migrate()`, returns **`false`**.
3. **Explicit `SERVER_TRUST_ALL_CERTIFICATES = true` →** after `migrate()`, value is
   **preserved as `true`** (user-explicit trust-all is a distinct concern from the silent
   downgrade path).

> **CRITICAL SEQUENCING — read-derived, governs DES-002:** On the **current** codebase,
> `migrate()` sets the flag to `true` for `+ssl`/`+tls` (verified, `AuthPreferences.java:284`).
> So test #1 **fails red against today's code**. AC-4's phrasing ("before any change to
> AuthPreferences.java … these tests must pass") is satisfiable *as written* only for tests
> #2 and #3; test #1 is a **target-state pin that goes green when DES-002 rewrites migrate()**.
> The integration contract between DES-003 and DES-002 is therefore: DES-003 authors all three
> tests; tests #2 and #3 gate the current behavior immediately; **test #1 is the executable
> acceptance criterion for DES-002's migrate() rewrite** and must be green before DES-002
> merges. An implementer must NOT make test #1 green by asserting `true` (matching the insecure
> status quo) — that would invert the security requirement. This contradiction is recorded so
> it is resolved deliberately, not silently. `migrate()` is package-private (`void migrate()`,
> verified) and invoked via `Preferences.migrate()` → `App.migrate()` (verified); the test is in
> the same package so it can call it directly.

`migrate()` (note `AuthMode.XOAUTH` early-return at line 277, verified) means test #1's
fixture must set a non-XOAUTH auth mode and a `+ssl`/`+tls` `server_protocol`, mirroring the
existing `testStoreUri` fixture pattern (`putString("server_protocol", "+ssl+")`, verified).

### Characterization test #2 — `BackupJobs` retry/constraints protects DES-005

DES-005 migrates scheduling to WorkManager, which must preserve the scheduling contract
**line-for-line**. The AC-5 test pins that contract on the **current** `BackupJobs` before
the swap. Four independent `@Test` methods in `service/BackupJobsTest.java` (which currently
tests scheduling/triggers but **not** the retry interval values, verified):

1. Retry **base interval = 30 seconds** — pins `firebaseJobDispatcher.newRetryStrategy(RETRY_POLICY_EXPONENTIAL, 30, 300)` (`BackupJobs.java:205`, verified).
2. Retry **maximum interval = 300 seconds** — same line, the `300` argument.
3. A job scheduled with **`ON_UNMETERED_NETWORK`** is not dispatched on a metered network —
   pins the `isWifiOnly() ? ON_UNMETERED_NETWORK : ON_ANY_NETWORK` branch (`BackupJobs.java:199`, verified).
4. A job scheduled with **`ON_ANY_NETWORK`** is dispatched on a metered network — same branch.

> **Implementer note (verify, do not assume):** the current jobdispatcher API exposes the
> retry strategy and constraints on the built `Job`. The existing `verifyJobScheduled` helper
> reads `job.getConstraints()` and `job.getTrigger()` (verified). Tests #3/#4 should assert
> on `job.getConstraints()` for `ON_UNMETERED_NETWORK`/`ON_ANY_NETWORK` driven by a mocked
> `preferences.isWifiOnly()`; tests #1/#2 assert on the retry strategy exposed by the built
> `Job` (the implementer must confirm jobdispatcher surfaces `getRetryStrategy()` on the
> `Job` — if it does not, the test extracts the strategy via the same builder path the
> production code uses). These four are the behavioral contract DES-005 must keep green
> **before** the WorkManager swap and must map onto WorkManager equivalents
> (`setBackoffCriteria(EXPONENTIAL, 30s)`, `Constraints.setRequiredNetworkType(UNMETERED|CONNECTED)`)
> per migration-units.md MU-005 §contract_preservation (verified).

### Why these two and not others

AC-4 and AC-5 are singled out (over `MainActivity`/`OAuth2Client`, which gaps.md also flags
as untested) because they are the two paths whose **behavior must be preserved by an
imminent Phase-1/Phase-2 swap**: `migrate()` is the exact code DES-002 deletes/rewrites, and
`BackupJobs` is the exact contract DES-005 reimplements. They are the *behavioral contracts*
the dependent designs rely on; the other backfills improve coverage but do not gate a
specific upcoming swap.

---

## Integration Contracts

> **No formal CNTR-\* integration contracts are expected or required for this design.**

This design introduces **no new cross-component runtime boundary**. It adds CI configuration,
build configuration, and test code. Nothing in it produces or consumes data across a component
interface at runtime. Per the Contract Gate model, a CNTR-\* artifact is required only when a
design crosses a component boundary; this one does not.

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| _(none — no runtime cross-component boundary introduced)_ | — | — | — | — | n/a |

**The behavioral contracts in this design are the two characterization tests, not CNTR-\*
artifacts.** They are code-level pins, enforced by CI, that downstream designs depend on:

| Behavioral contract | Specified in | Producer (code under pin) | Consumer (dependent design) |
|---------------------|--------------|---------------------------|-----------------------------|
| `migrate()` never silently enables trust-all (AC-4, test #1) | `preferences/AuthPreferencesTest.java` | `AuthPreferences.migrate()` | **DES-002** (must turn test #1 green) |
| `migrate()` preserves explicit/clean state (AC-4, tests #2,#3) | same | `AuthPreferences.migrate()` | DES-002 (must not regress) |
| Retry 30/300 exponential backoff (AC-5, tests #1,#2) | `service/BackupJobsTest.java` | `BackupJobs.defaultRetryStrategy()` | **DES-005** (must preserve) |
| Network-constraint semantics UNMETERED/ANY (AC-5, tests #3,#4) | same | `BackupJobs.jobConstraints()` | DES-005 (must preserve) |

### Contracts Needed (pre-sprint gate)

- _None._ This design requires no `/amp:create-contracts` run. A story implementing this
  design crosses no undocumented component boundary.

## Trade-offs

- **Per-package JaCoCo rules vs. a single aggregate rule.** Per-package rules cost more
  configuration and produce three independent failure points, but they are required by
  AC-3(d) (any package individually below 70% must fail) and prevent a high-coverage package
  from masking a low one. Aggregate rule rejected.
- **70% as the floor.** A higher bar (e.g., 80%) would be stronger but risks the gate being
  unreachable on the first run given unmeasured current coverage, stalling Gate G2. 70% is
  the requirement's number and is the pragmatic floor that makes the gate *land* and then
  ratchet. Recorded as a deliberate floor, not a ceiling.
- **R8 on now vs. deferring to release.** Enabling R8 in CI surfaces keep-rule gaps early
  (cost: must author keep rules for Otto/jobdispatcher reflection that later MUs will delete).
  Deferring would leave R8 regressions latent until a manual release. AC-2 mandates on-now;
  the interim keep-rule cost is accepted.
- **Lint `warningsAsErrors` retained with a baseline vs. relaxed to warnings.** Keeping
  `-Werror`-style lint with a managed `lint-baseline.xml` preserves the project's existing
  discipline while preventing the SDK migration's new deprecations from blocking the gate.

## Risks

- **First CI run may fail the coverage gate** if a gated package is below 70% today (coverage
  is currently unmeasured). Mitigation: the story's first step is to measure per-package
  coverage and backfill until each clears; AC-4/AC-5 alone may not reach 70%.
- **R8 keep-rule gaps** for Otto `@Subscribe`/`@Produce` reflection and jobdispatcher's
  reflective `SmsJobService` resolution. Mitigation: run the suite with R8 enabled and author
  keep rules before declaring Gate G2 (AC-2).
- **JaCoCo sub-package glob** may silently exclude `service/state/` and `service/exception/`
  if the `includes` pattern is wrong, producing a false pass. Mitigation: verify the resolved
  JaCoCo version's glob semantics at implementation time and assert the report lists the
  sub-package classes.
- **AC-4 test #1 contradiction** with current `migrate()` behavior. Mitigation: the
  contradiction is documented above; test #1 is the DES-002 acceptance pin, authored red,
  and must not be "fixed" to match the insecure status quo.
- **Toolchain co-requisite skew.** If the Robolectric bump lands separately from the SDK
  raise, `master` enters an unrunnable-test state. Mitigation: hard same-PR constraint (AC-7,
  Integration Design).

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| REQ-MODERNIZATION-003 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-003-establish-ci-coverage-gate.md | Source requirement; 9 ACs traced |
| Migration Units (MU-002) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md | MU-002 scope, backfill targets, MU-001↔002 co-requisite, MU-005 contract preservation |
| Gap Analysis | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/gaps.md | OPS-001/OPS-002/OPS-003, SEC-001 migrate() downgrade, DEP-004 toolchain |
| Code Location | sdlc/artifacts/engagement/code-location.md | Tech stack, build/test commands, source layout |
| app/build.gradle | app/build.gradle | VERIFIED: minifyEnabled false (:33), warningsAsErrors (:41), test deps (:65-68), no jacoco |
| build.gradle (root) | build.gradle | VERIFIED: jcenter/bintray/scijava/jitpack repos; AGP 4.1.3; parallel test config |
| AuthPreferences.java | app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java | VERIFIED: migrate() sets trust-all=true (:284); XOAUTH early-return (:277); package=preferences not auth |
| AuthPreferencesTest.java | app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java | VERIFIED: only testStoreUri/testStoreUriWithXOAuth2 exist; no migrate() test |
| BackupJobs.java | app/src/main/java/com/zegoggles/smssync/service/BackupJobs.java | VERIFIED: retry 30/300 (:205); ON_UNMETERED/ON_ANY constraint branch (:199) |
| BackupJobsTest.java | app/src/test/java/com/zegoggles/smssync/service/BackupJobsTest.java | VERIFIED: tests scheduling/triggers; no retry-interval or constraint assertion |
| SmsBackupServiceTest.java | app/src/test/java/com/zegoggles/smssync/service/SmsBackupServiceTest.java | VERIFIED: empty `// TODO` @Test bodies present (AC-6) |
| AppTest.java | app/src/test/java/com/zegoggles/smssync/AppTest.java | VERIFIED: @Config(sdk=18) + @Ignore pattern already in suite |
| .travis.yml | .travis.yml | VERIFIED: stale Travis running ./gradlew build on android-29 (no GH Actions) |
| service package tree | app/src/main/java/com/zegoggles/smssync/service/ | VERIFIED: state/ and exception/ sub-packages exist (JaCoCo glob scope) |
| auth package tree | app/src/main/java/com/zegoggles/smssync/auth/ | VERIFIED: only OAuth2Client/OAuth2Token/TokenRefresher/TokenRefreshException |
| DES-MODERNIZATION-001 (stub) | sdlc/artifacts/design/modernization/DES-MODERNIZATION-001-restore-play-store-eligibility-design.md | VERIFIED: still scaffold; SDK-raise co-requisite partner (not yet authored) |
| DES-MODERNIZATION-002 (stub) | sdlc/artifacts/design/modernization/DES-MODERNIZATION-002-transport-security-hardening-design.md | VERIFIED: still scaffold; consumer of AC-4 test #1 (not yet authored) |

## Design Validation

Each row maps a requirement acceptance criterion to the concrete, inspectable/runnable
evidence that proves it. All evidence is artifact inspection or a deterministic Gradle/CLI
command, matching the requirement's verification note.

| AC | Claim | Validation evidence |
|----|-------|---------------------|
| AC-1 | CI runs all four tasks on every PR; required check on `master` | `.github/workflows/ci.yml` exists and runs `./gradlew test lint jacocoTestReport assembleRelease --verify` on `pull_request`; branch-protection setting shows the workflow as a required status check (repo settings / `gh api repos/:owner/:repo/branches/master/protection`) |
| AC-2 | R8 runs on every CI run | `app/build.gradle:33` reads `minifyEnabled true`; a CI `assembleRelease` log shows R8 task execution; suite green with R8 on |
| AC-3 | Per-package 70% gate fails below threshold | `./gradlew jacocoTestReport` exits non-zero when any of `service/**`, `mail/**`, `auth/**` < 70% line; deliberately drop a test → CI fails naming the package |
| AC-4 | migrate() trust-all pinned (3 tests) | Three `@Test` methods in `preferences/AuthPreferencesTest.java`; tests #2,#3 green now; **test #1 green only after DES-002 rewrite** (documented contradiction) |
| AC-5 | BackupJobs retry/constraints pinned (4 tests) | Four independent `@Test` methods in `service/BackupJobsTest.java` asserting 30s base, 300s max, UNMETERED-not-on-metered, ANY-on-metered |
| AC-6 | No empty test bodies | `SmsBackupServiceTest.java:~204+` empty `// TODO` body replaced with assertions or `@Ignore` + reason; no empty `@Test` remains; CI does not suppress empty-test reporting |
| AC-7 | Toolchain uplift, mockito-all gone | `app/build.gradle:65-68` shows robolectric 4.12.x / junit 4.13.2 / mockito-core 5.x / truth 1.4.x; `./gradlew app:dependencies --configuration testRuntimeClasspath \| grep mockito` shows no `mockito-all`; all 35 classes pass; lands in the DES-001 SDK-raise PR |
| AC-8 | Verification metadata enforced | `gradle/verification-metadata.xml` present; CI `--verify` exits non-zero on a tampered checksum; regenerated on any dependency-version change |
| AC-9 | Gate G2 milestone | One green CI run on `master` (no-op `SmsBackupService.java` change) with workflow passing, coverage gate enforced+passing, AC-4 and AC-5 tests present+passing |

**Validation summary (the four asserted by the prompt):**
1. **CI runs all four tasks on PR** — AC-1 row above; one chained `./gradlew` invocation so none can be skipped.
2. **Gate fails below 70%** — AC-3 row; per-package `JacocoCoverageVerification` rules, non-zero exit, package named on breach.
3. **Two characterization tests green before dependents** — AC-4/AC-5 rows; with the explicit caveat that AC-4 test #1 is DES-002's acceptance pin (green on DES-002 merge), while tests #2,#3 and all of AC-5 gate current behavior immediately.
4. **verification-metadata enforced** — AC-8 row; `--verify` in CI, regenerated post-upgrade.

> **One-line note:** Draft content for system-architecture, integration-design, and design-validation written and verified against live source (build.gradle:33/65-68, AuthPreferences.migrate():284, BackupJobs:199/205) — NOT finalized; the load-bearing open item for review is the AC-4 test-#1 contradiction with current `migrate()` behavior, which makes test #1 a DES-002 acceptance pin rather than a present-state characterization.
