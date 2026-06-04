---
type: bug
status: ready
artifact_type: bug
severity: medium
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-033
platforms: []
tags: []
id: BUG-001
title: Gradle dependency verification fails for all -sources.jar artifacts on IDE/source resolution
domain: build
origin: qa
---

# BUG-001: Gradle dependency verification fails for all -sources.jar artifacts on IDE/source resolution

## Description

Gradle strict dependency verification is enabled for this project via `gradle/verification-metadata.xml`
(`<verify-metadata>true</verify-metadata>`). The verification metadata was generated to cover production
build artifacts (`.aar`, `.jar`, `.module`, `.pom`) and contains entries for all 479 declared components.
However, the file contains no checksum entries for `-sources.jar` (or `-javadoc.jar`) classifier artifacts,
and it contains no `<trusted-artifacts>` block that would exempt source jars from checksum verification.

When any Gradle invocation resolves source jars — IDE sync, Android Studio "Download Sources",
or any task that creates a `detachedConfiguration` to fetch sources — Gradle applies the same
strict verification rules to those jars. Because there are no matching entries in the metadata,
every source jar fails verification. The observed failure count is 143 artifacts, all of the
form `*-sources.jar`, reported in `build/reports/dependency-verification/`.

Production CLI builds (`./gradlew :app:assembleDebug`, `:app:testDebugUnitTest`,
`:app:jacocoTestCoverageVerification`) are unaffected because those task graphs never resolve
source jars. The defect surfaces exclusively during IDE-driven source resolution.

The `gradle/verification-metadata.xml` file was introduced by U-027 and subsequently updated
by U-020 and U-022 as dependencies changed. None of those increments added source-jar coverage.

## Steps to Reproduce

1. Open the project in Android Studio (or any IDE that triggers Gradle source resolution on sync).
2. Perform a Gradle sync, or explicitly trigger "Download Sources" / "Download Sources and Documentation"
   for any dependency via the IDE.
3. Observe that Gradle resolves `*-sources.jar` artifacts via a `detachedConfiguration`
   (e.g., `:app:detachedConfiguration3`, `:k9mail-vendored:detachedConfiguration3`).
4. Gradle looks up each resolved source jar in `gradle/verification-metadata.xml`.
5. No checksum entry exists for any `*-sources.jar` artifact; no `<trusted-artifacts>` exemption
   applies. Verification fails for all 143 source jars.
6. The IDE sync aborts and reports dependency verification failures. The verification report is
   written to `build/reports/dependency-verification/`.

## Expected Behavior

Resolving source jars during IDE sync or a "Download Sources" invocation succeeds with no
dependency-verification failure. All 143 (or more, as dependencies evolve) `-sources.jar`
artifacts are either trusted via a scoped `<trusted-artifacts>` rule or have their checksums
present in `gradle/verification-metadata.xml`. Android Studio sync completes normally and
developers can navigate to dependency sources.

## Actual Behavior

Gradle aborts resolution and reports: **"143 artifacts failed verification"**. Every failing
artifact is a `-sources.jar`. Representative examples from the failure set:

- `kotlin-compiler-embeddable-1.9.25-sources.jar`
- `guava-32.0.1-jre-sources.jar`
- `hilt-android-gradle-plugin-2.51.1-sources.jar`
- `com.android.tools.*-sources.jar` (multiple)

The full failure list is written to `build/reports/dependency-verification/`. IDE sync cannot
complete; developers cannot navigate to dependency sources from within the IDE.

## Evidence

**`gradle/verification-metadata.xml`, lines 1–6 — configuration block:**

```xml
<verification-metadata ...>
   <configuration>
      <verify-metadata>true</verify-metadata>
      <verify-signatures>false</verify-signatures>
   </configuration>
```

`verify-metadata` is `true` — strict verification is active for all resolved artifacts.

**No `-sources.jar` entries exist in the file.** A search for `sources.jar` in
`gradle/verification-metadata.xml` returns zero matches. Every one of the 479 `<component>`
entries covers only production artifact classifiers (`.aar`, `.jar`, `.module`, `.pom`).
No `-sources.jar` artifact name appears anywhere in the file.

**No `<trusted-artifacts>` block exists in the file.** A search for `trusted-artifacts`
in `gradle/verification-metadata.xml` returns zero matches. There is no exemption mechanism
in place that would allow unverified source jars to pass.

**Production CLI builds pass.** `./gradlew :app:assembleDebug`, `:app:testDebugUnitTest`,
and `:app:jacocoTestCoverageVerification` all succeed because those task graphs do not
resolve `-sources.jar` artifacts. The verification infrastructure is functioning correctly
for its intended scope; the gap is exclusively in source-jar coverage.

**Failure report location:** `build/reports/dependency-verification/`

## Acceptance Criteria

- [ ] AC-1: Resolving source jars via IDE sync, "Download Sources", or any
  sources-resolving Gradle invocation (e.g., `--download-sources`) completes with
  **zero dependency-verification failures**. The `build/reports/dependency-verification/`
  report contains no failing artifacts.

- [ ] AC-2: Production CLI builds pass without modification and dependency verification
  remains **enabled** (`<verify-metadata>true</verify-metadata>` in
  `gradle/verification-metadata.xml`). The three gates — `:app:assembleDebug`,
  `:app:testDebugUnitTest`, `:app:jacocoTestCoverageVerification` — all succeed.
  The supply-chain guarantee from U-027 is not weakened for production artifacts.

- [ ] AC-3: The fix is **configuration-only** — changes are limited to
  `gradle/verification-metadata.xml`. No production source files, test files, or
  `build.gradle` / `build.gradle.kts` files are modified.

## Impact Analysis

`change_records.layers` is not configured in `sdlc/config.yaml`; impact analysis is manual.

**Files in scope for the fix:**

| File | Role | Change |
|------|------|--------|
| `gradle/verification-metadata.xml` | Gradle dependency verification metadata | Add `<trusted-artifacts>` block or add `-sources.jar` checksum entries |

**Downstream impact:**

- **Developers using Android Studio** — blocked from completing IDE sync and navigating
  to dependency sources until fixed. This affects every developer who opens the project
  in an IDE with source resolution enabled. Severity is medium (CLI workflow is unaffected;
  no production functionality is impaired).
- **CI/CLI builds** — unaffected. All Gradle tasks that run in CI do not resolve source jars.
  No CI change is required.
- **No app source or test files are affected.** The defect and its fix are entirely within
  the Gradle build configuration layer.
- **U-027 supply-chain guarantee** — the fix must preserve `verify-metadata=true` and must
  not remove any existing checksum entries. Blanket disabling of verification would be a
  regression against U-027's acceptance criteria.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `gradle/verification-metadata.xml` | Contains 479 component entries covering `.aar`/`.jar`/`.module`/`.pom` artifacts only; no `-sources.jar` entries; no `<trusted-artifacts>` block; `verify-metadata=true` | Add a `<trusted-artifacts>` block trusting `.*-sources\.jar` and `.*-javadoc\.jar` regexes (preferred), or regenerate including source-jar checksums |

## Existing Behavior to Preserve

- `<verify-metadata>true</verify-metadata>` MUST remain `true` in `gradle/verification-metadata.xml`
  after the fix. Disabling verification wholesale is not an acceptable resolution.
- All 479 existing `<component>` entries and their SHA-256 checksums MUST be preserved intact.
  No existing checksum may be removed or altered.
- The three CLI build/test/coverage gates (`assembleDebug`, `testDebugUnitTest`,
  `jacocoTestCoverageVerification`) MUST continue to pass after the fix.
- Dependency verification MUST remain scoped to the project's actual dependency graph.
  The fix must not introduce a global trust rule that bypasses verification for all artifacts.

## Verification Steps

1. **AC-1 — Source resolution succeeds:**
   Open the project in Android Studio and trigger a full Gradle sync (or run
   `./gradlew :app:dependencies --configuration debugRuntimeClasspath --download-sources`
   if supported). Confirm the sync completes without error. Confirm
   `build/reports/dependency-verification/` contains no failing artifacts (directory
   is absent or the report lists zero failures).

2. **AC-2 — Production CLI gates still green:**
   Run the following three commands in sequence and confirm all three exit with code 0:
   ```
   ./gradlew :app:assembleDebug
   ./gradlew :app:testDebugUnitTest
   ./gradlew :app:jacocoTestCoverageVerification
   ```
   Confirm no dependency-verification errors appear in any of the three outputs.

3. **AC-2 (verify-metadata still enabled) — Grep the config:**
   ```
   grep "verify-metadata" gradle/verification-metadata.xml
   ```
   Confirm output is `<verify-metadata>true</verify-metadata>`. Any other value is a
   verification failure.

4. **AC-3 — Config-only change:**
   Run `git diff --name-only` (or inspect the PR diff). Confirm the only modified file
   is `gradle/verification-metadata.xml`. Any modification outside that file fails this criterion.

5. **Sources trust scope — Grep the fix:**
   ```
   grep "trusted-artifacts\|sources" gradle/verification-metadata.xml
   ```
   Confirm a `<trusted-artifacts>` block (or equivalent source-jar checksum entries) is
   present and scoped appropriately (e.g., regex `.*-sources\.jar`, `.*-javadoc\.jar`).
   Confirm the rule does not apply to non-source, non-javadoc artifacts.

## Root Cause Analysis

*(To be completed during planning phase.)*

## Technical Context

Two viable fix options exist for `gradle/verification-metadata.xml`:

**Option A — Add a `<trusted-artifacts>` block (preferred):**
Add a `<trusted-artifacts>` block inside `<configuration>` that trusts artifact names
matching `.*-sources\.jar` and `.*-javadoc\.jar` regexes. Gradle will skip checksum
verification for matching artifact names regardless of group, module, or version.
This approach is low-maintenance: source-jar trust is version-agnostic, so it will not
need to be updated when dependencies change. It is the standard approach for IDE source
resolution in projects with strict dependency verification.

Example (illustrative — exact schema syntax must be confirmed against the
`dependency-verification-1.3.xsd` referenced in line 2 of the file):

```xml
<configuration>
   <verify-metadata>true</verify-metadata>
   <verify-signatures>false</verify-signatures>
   <trusted-artifacts>
      <trust file=".*-sources\.jar" regex="true"/>
      <trust file=".*-javadoc\.jar" regex="true"/>
   </trusted-artifacts>
</configuration>
```

**Option B — Regenerate metadata including source-jar checksums:**
Run `./gradlew --write-verification-metadata sha256 --download-sources` (or equivalent)
to regenerate `gradle/verification-metadata.xml` with SHA-256 checksums for all resolved
source jars added alongside the existing entries. This approach is higher-maintenance:
every dependency version bump requires a corresponding metadata regeneration, and IDE
source resolution in future may resolve new source jars that are not yet in the file.

**Recommendation:** Option A. It eliminates the maintenance burden, is consistent with the
rationale that source jars carry no executable code that could be supply-chain-compromised
in the same way as production bytecode, and is the approach used by the majority of
Android projects that enable strict dependency verification.

**Artifact history:** `gradle/verification-metadata.xml` was introduced by U-027
(dependency verification enablement) and updated by U-020 and U-022 as the dependency
graph changed. None of those updates added source-jar coverage, which is the proximate
cause of this defect.

## Supporting Documentation

- U-027: Dependency verification enablement (introduced `gradle/verification-metadata.xml`)
- U-020, U-022: Updated verification metadata as dependencies changed
- Gradle documentation: [Verifying dependencies](https://docs.gradle.org/current/userguide/dependency_verification.html)
  — specifically the "Trusting some specific artifacts" section for `<trusted-artifacts>` syntax

## Implementation Notes

*(Added by agents during build.)*

## Review Findings

*(Summarized from workspace artifacts.)*

## Notes

Detection context: surfaced during QA review of IDE workflow as part of U-027 acceptance
verification. CLI builds were confirmed passing across sprint-001 and sprint-002 before
this defect was identified.

## Suggested Approach

Advisory only — refined during story planning. The recommendation below is not a plan; it
records no tasks, acceptance criteria, or verification steps.

### Assumption Ledger

Every claim below was verified against `gradle/verification-metadata.xml` and the Gradle
dependency-verification documentation in this session. Verification method noted per row.

| # | Assumption | Verified? | Evidence / Method |
|---|------------|-----------|-------------------|
| A1 | `verify-metadata` is `true` and verification is strict | Confirmed | `gradle/verification-metadata.xml:4` — `<verify-metadata>true</verify-metadata>` |
| A2 | No `<trusted-artifacts>` block and no `*-sources.jar` / `*-javadoc.jar` entries exist | Confirmed | Grep for `trusted-artifacts\|sources\.jar\|javadoc\.jar` over the file returned 0 matches |
| A3 | The file uses schema `dependency-verification-1.3.xsd` | Confirmed | `gradle/verification-metadata.xml:2` schemaLocation |
| A4 | There is exactly one verification-metadata file (root-level, project-wide) | Confirmed | Glob `**/verification-metadata.xml` returned only `gradle/verification-metadata.xml` — no per-module copies |
| A5 | `:k9mail-vendored` is a real module sharing the same root verification file | Confirmed | `settings.gradle:1` — `include ':app', ':metadata', ':k9mail-vendored'`; only one metadata file exists (A4) |
| A6 | Component count cited as "479" in the bug | Discrepancy noted | Grep `<component ` returns 478 occurrences. The exact count is immaterial to the fix (the trust rule is count-agnostic); planning should confirm the true number but it does not change the recommendation |
| A7 | `<trust file="..." regex="true"/>` placed under `<configuration><trusted-artifacts>` is the Gradle-native mechanism for classifier-scoped trust | Confirmed (documentation) | Gradle "Verifying dependencies" guide, "Trusting some specific artifacts" — `<trusted-artifacts>` is a child of `<configuration>` (matches the bug's Technical Context example, lines 226–233) |

### Recommendation: Option A (scoped `<trusted-artifacts>` block)

Of the two options the bug documents, **Option A — add a `<trusted-artifacts>` block trusting
`.*-sources\.jar` and `.*-javadoc\.jar` by regex — is the stronger choice.** Reasoning:

- **Sources and javadoc are developer-convenience artifacts, not part of the production
  runtime or supply chain.** They are read by the IDE for navigation and inline docs; they are
  never compiled into, packaged with, or executed by the shipped app. Trusting them by
  classifier therefore does not weaken the guarantee U-027 established for the code that
  actually runs.
- **Trusting by classifier is version-agnostic.** A regex on the `-sources.jar` /
  `-javadoc.jar` suffix matches regardless of group, module, or version, so the rule does not
  churn as dependency versions move. Option B (regenerating with `--download-sources`) bloats
  the already-large file — it would add a sources checksum for every one of the ~478 resolvable
  components — and forces a metadata regeneration on **every** version bump, plus whenever the
  IDE resolves a source jar not yet captured. That is exactly the recurring maintenance cost
  the bug's Technical Context flags, and it is avoidable.

The Gradle-native mechanism is a `<trust>` element with `regex="true"` placed inside a
`<trusted-artifacts>` block, which itself sits inside the existing `<configuration>` element
(alongside the current `<verify-metadata>` and `<verify-signatures>` entries):

```
<configuration>
   ... existing verify-metadata / verify-signatures ...
   <trusted-artifacts>
      <trust file=".*-sources\.jar" regex="true"/>
      <trust file=".*-javadoc\.jar" regex="true"/>
   </trusted-artifacts>
</configuration>
```

Exact element ordering and attribute names should be confirmed against
`dependency-verification-1.3.xsd` during planning, since the XSD constrains child ordering
within `<configuration>`.

### Supply-chain trade-off

Trusting `*-sources.jar` / `*-javadoc.jar` by regex means those specific classifier artifacts
are **no longer checksum-verified**. This is an acceptable, bounded relaxation:

- The trusted artifacts are never executed or packaged — sources are consumed by the IDE only,
  and javadoc by IDE tooltips. A tampered source jar cannot influence the built or shipped
  binary, so the threat model that motivates checksum verification (a compromised dependency
  altering runtime behaviour) does not apply to them.
- Every production classifier — `.aar`, `.jar`, `.module`, `.pom` — remains fully covered by
  its existing checksum and by `verify-metadata=true`. The supply-chain guarantee for the
  runtime is unchanged.

**The regex must be anchored to the sources/javadoc classifiers only.** Trust `.*-sources\.jar`
and `.*-javadoc\.jar` — do **not** broaden to `.*\.jar` or `.*-.jar`, which would exempt
ordinary production `.jar` artifacts from verification and silently undo U-027's guarantee for
real bytecode. The narrowness of the regex is the property that makes the trade-off safe.

### One story vs. multiple

This is a **single, config-only, low-risk story.** The entire change lives in one file
(`gradle/verification-metadata.xml`), introduces no cross-component boundary, touches no
production or test source, and needs no contract. There is no integration wiring to own and no
reason to split it.

### Constraints and risks for planning to carry forward

- **Keep `verify-metadata=true`.** The fix adds an exemption scoped to two classifiers; it
  must not disable verification globally.
- **Do not touch the existing production checksums.** The change is purely additive — insert the
  `<trusted-artifacts>` block; leave all existing `<component>` entries and their SHA-256 values
  intact.
- **Project-wide coverage is already satisfied by file location.** There is exactly one
  verification file, the root `gradle/verification-metadata.xml` (verified via Glob: A4), and it
  governs all three modules — `:app`, `:metadata`, `:k9mail-vendored` (`settings.gradle:1`: A5).
  The `:k9mail-vendored` detachedConfiguration source-resolution path that the bug calls out is
  therefore covered by the same root-level trust rule; no per-module verification file exists or
  needs to be created. Planning should treat "project-wide" as the default and confirm no
  module overrides this.
- **Verification surface for planning to design around:** a sources-resolving sync (IDE sync /
  "Download Sources") should pass with zero verification failures, **and** the three CLI gates
  (`:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:jacocoTestCoverageVerification`) must
  stay green and free of verification errors after the change.
- **Component-count note:** the bug cites 479 components; the file currently shows 478
  `<component>` elements (A6). This does not affect the recommendation, but planning should not
  treat "479" as a load-bearing constant.
