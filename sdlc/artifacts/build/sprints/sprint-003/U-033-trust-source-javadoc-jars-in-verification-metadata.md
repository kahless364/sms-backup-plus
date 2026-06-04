---
type: bug
status: done
artifact_type: user-story
priority: medium
complexity: low
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
id: U-033
title: Trust source/javadoc jars in Gradle dependency verification metadata
pipeline: ''
domain: modernization
requirement_source: bug:BUG-001
sprint: '000003'
---

# U-033: Trust source/javadoc jars in Gradle dependency verification metadata

## Story

As a developer working in Android Studio or any IDE that resolves Gradle source jars on sync,
I want Gradle dependency verification to permit `-sources.jar` and `-javadoc.jar` resolution,
so that IDE sync and source navigation complete without verification failures — while
production-artifact verification stays fully enabled.

## Acceptance Criteria

- [ ] AC-1: A `<trusted-artifacts>` block is added inside the `<configuration>` element of
  `gradle/verification-metadata.xml` containing exactly two `<trust>` rules:
  one with `file=".*-sources\.jar" regex="true"` and one with `file=".*-javadoc\.jar"
  regex="true"`. The rules MUST be scoped exclusively to those two classifiers. The regexes
  MUST NOT broaden to `.*\.jar` or any pattern that matches production artifact names.
  The `<trusted-artifacts>` element MUST be placed as a child of `<configuration>` after
  the existing `<verify-metadata>` and `<verify-signatures>` elements, consistent with the
  `dependency-verification-1.3.xsd` schema declared in line 2 of the file.

- [ ] AC-2: `<verify-metadata>true</verify-metadata>` remains `true` and all 478 existing
  production-artifact `<component>` entries and their SHA-256 checksums are preserved
  verbatim. A `git diff gradle/verification-metadata.xml` shows only additive lines
  (the new `<trusted-artifacts>` block); no existing `<component>`, `<artifact>`, or
  `<sha256>` element is removed or modified. Verified by:
  ```
  grep "verify-metadata" gradle/verification-metadata.xml
  # expected: <verify-metadata>true</verify-metadata>
  ```
  and by inspecting the diff for deletions.

- [ ] AC-3: A Gradle invocation that resolves `-sources.jar` artifacts completes with
  **zero dependency-verification failures**. The `build/reports/dependency-verification/`
  directory is either absent or the report it contains lists no failing artifacts.
  The 143 `-sources.jar` artifacts that previously failed (representative set includes
  `kotlin-compiler-embeddable-1.9.25-sources.jar`, `guava-32.0.1-jre-sources.jar`,
  `hilt-android-gradle-plugin-2.51.1-sources.jar`, and `com.android.tools.*-sources.jar`)
  must all pass. Accepted verification methods: (a) open the project in Android Studio and
  perform a full Gradle sync with "Download Sources and Documentation" enabled; (b) trigger
  a detachedConfiguration source-resolution task directly, such as:
  ```
  ./gradlew :app:dependencies --configuration debugRuntimeClasspath
  ```
  followed by an explicit source-download invocation in the IDE; or (c) any Gradle task
  invocation that exercises the same `detachedConfiguration`-based source-resolution path
  described in BUG-001 Steps to Reproduce.

- [ ] AC-4: The three production CLI gates pass with BUILD SUCCESSFUL and with no
  dependency-verification errors in their output:
  ```
  ./gradlew :app:assembleDebug
  ./gradlew :app:testDebugUnitTest
  ./gradlew :app:jacocoTestCoverageVerification
  ```

- [ ] AC-5: The fix is applied to the single root `gradle/verification-metadata.xml` file
  only. No per-module verification files are created under `:app`, `:metadata`, or
  `:k9mail-vendored`. The root file governs all three modules (the only module registration
  is in `settings.gradle:1`; no per-module `gradle/` directories contain a
  `verification-metadata.xml`). The `:k9mail-vendored` `detachedConfiguration` source-resolution
  path — which BUG-001 identifies as a separate failure surface — is confirmed passing under
  the single root-level trust rule (no per-module override required).

- [ ] AC-6: No production source file, test file, `build.gradle`, or `build.gradle.kts` file
  is modified. `git diff --name-only` shows exactly one changed file:
  `gradle/verification-metadata.xml`.

### Integration Criteria

Not applicable. This story creates no new components and introduces no cross-component
integration boundaries. All changes are confined to a single build-configuration file.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `gradle/verification-metadata.xml` | `<configuration>` block contains only `<verify-metadata>true</verify-metadata>` and `<verify-signatures>false</verify-signatures>`; no `<trusted-artifacts>` block exists; 478 `<component>` entries cover only `.aar`, `.jar`, `.module`, and `.pom` artifacts; no `-sources.jar` or `-javadoc.jar` artifact name appears anywhere in the file | Add a `<trusted-artifacts>` block as the third child of `<configuration>`, containing two `<trust>` elements trusting `.*-sources\.jar` and `.*-javadoc\.jar` by regex; all existing content is preserved verbatim |

## Existing Behavior to Preserve

- `<verify-metadata>true</verify-metadata>` MUST remain `true`. Disabling verification
  globally is not an acceptable resolution and would be a regression against the U-027
  supply-chain guarantee.
- All 478 existing `<component>` entries and their SHA-256 checksums MUST be preserved
  intact. No checksum may be removed or altered.
- The three CLI build/test/coverage gates (`assembleDebug`, `testDebugUnitTest`,
  `jacocoTestCoverageVerification`) MUST continue to pass after the change.
- The trust rule MUST NOT be broadened beyond `-sources.jar` and `-javadoc.jar` classifiers.
  A rule matching `.*\.jar` would exempt ordinary production `.jar` artifacts from
  verification and silently undo the supply-chain guarantee established by U-027.

## Verification Steps

1. **AC-6 first — confirm the diff is config-only:**
   ```
   git diff --name-only
   ```
   Confirm the only file listed is `gradle/verification-metadata.xml`. If any other file
   appears, the change scope is incorrect.

2. **AC-2 — verify-metadata flag still enabled:**
   ```
   grep "verify-metadata" gradle/verification-metadata.xml
   ```
   Confirm output is exactly `<verify-metadata>true</verify-metadata>`.

3. **AC-1 — trusted-artifacts block is present and narrowly scoped:**
   ```
   grep -A4 "trusted-artifacts" gradle/verification-metadata.xml
   ```
   Confirm a `<trusted-artifacts>` block is present. Confirm the block contains
   `<trust file=".*-sources\.jar" regex="true"/>` and `<trust file=".*-javadoc\.jar" regex="true"/>`.
   Confirm no other `<trust>` element is present. Confirm there is no `.*\.jar` pattern.

4. **AC-2 — no existing checksums removed:**
   ```
   git diff gradle/verification-metadata.xml | grep "^-" | grep -v "^---"
   ```
   Confirm zero deletion lines appear (only addition lines for the `<trusted-artifacts>` block).

5. **AC-3 — source resolution succeeds:**
   Open the project in Android Studio. Trigger a full Gradle sync with source download enabled
   (File > Sync Project with Gradle Files, or right-click a dependency > Download Sources and
   Documentation). Confirm sync completes without error. Confirm
   `build/reports/dependency-verification/` is absent or its report lists zero failures.
   Alternatively, run:
   ```
   ./gradlew :app:dependencies --configuration debugRuntimeClasspath
   ```
   and then trigger source download via the IDE on any dependency in the displayed tree.

6. **AC-4 — production CLI gates still pass:**
   ```
   ./gradlew :app:assembleDebug
   ./gradlew :app:testDebugUnitTest
   ./gradlew :app:jacocoTestCoverageVerification
   ```
   Confirm all three commands exit with code 0 and no dependency-verification errors appear
   in their output.

7. **AC-5 — no per-module verification files created:**
   ```
   find . -name "verification-metadata.xml"
   ```
   Confirm only `./gradle/verification-metadata.xml` is listed.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / Gradle | Add `<trusted-artifacts>` block to `gradle/verification-metadata.xml`; verify AC-3 via IDE sync or sources-resolving Gradle invocation; confirm AC-4 via three CLI gates | Developer |

## Technical Context

**Chosen approach: Option A — `<trusted-artifacts>` block.**

BUG-001 documents two viable options for resolving the verification gap. Option A (add a
`<trusted-artifacts>` block) is selected over Option B (regenerate metadata with source-jar
checksums) for two reasons:

1. **Maintenance cost.** A regex trust rule on `-sources.jar` and `-javadoc.jar` suffixes is
   version-agnostic. It matches any artifact version now and in future without further
   maintenance. Option B would add ~143 checksum entries to an already-large file and require
   a metadata regeneration every time any dependency version changes — exactly the maintenance
   burden BUG-001 identifies as a reason to prefer Option A.

2. **Supply-chain trade-off is bounded and acceptable.** Source and javadoc jars are
   developer-convenience artifacts consumed exclusively by the IDE for source navigation and
   inline documentation. They are never compiled into, packaged in, or executed by the shipped
   APK. A tampered source jar cannot alter the runtime binary. The threat model that motivates
   strict checksum verification — a compromised dependency influencing runtime behaviour —
   does not apply to these artifacts. Every production classifier (`.aar`, `.jar`, `.module`,
   `.pom`) remains fully covered by its existing SHA-256 checksum and `verify-metadata=true`.
   The supply-chain guarantee from U-027 is unchanged for anything that ships.

**Narrowness constraint — non-negotiable.** The `file` attribute regex values MUST be
`.*-sources\.jar` and `.*-javadoc\.jar`. They MUST NOT be broadened to `.*\.jar` or any
other pattern that could match production classifiers. The safety of the supply-chain
trade-off above depends entirely on this narrowness.

**XSD element ordering.** The `dependency-verification-1.3.xsd` schema (referenced on
line 2 of `gradle/verification-metadata.xml`) constrains the child-element order within
`<configuration>`. Based on the schema and the Gradle documentation example, the correct
ordering is: `<verify-metadata>`, `<verify-signatures>`, then `<trusted-artifacts>`. The
developer should confirm this against the XSD before writing the change.

**File is project-wide.** `gradle/verification-metadata.xml` is the only verification
metadata file in the project (Glob `**/verification-metadata.xml` returns one result).
All three modules — `:app`, `:metadata`, `:k9mail-vendored` — share this single root-level
file. The `:k9mail-vendored` detachedConfiguration source-resolution path that BUG-001
identifies as a separate failure surface is therefore covered by the same trust rule. No
per-module file is needed.

**Component count note.** BUG-001 cites 479 components; the file currently contains 478
`<component>` elements. The discrepancy is a pre-existing count error in the bug report and
is immaterial to this story — the trust rule is count-agnostic. AC-2 uses "478" as the
authoritative count.

**Reference implementation (illustrative — confirm element ordering against XSD):**
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

## Supporting Documentation

- BUG-001: `sdlc/artifacts/stories/BUG-001-gradle-dep-verification-fails-sources-jars.md`
  — root cause analysis, Option A/B comparison, supply-chain trade-off, and Assumption Ledger
- U-027: Dependency verification enablement (introduced `gradle/verification-metadata.xml`)
- U-020, U-022: Updated verification metadata as the dependency graph changed
- Gradle documentation: [Verifying dependencies — Trusting some specific artifacts](https://docs.gradle.org/current/userguide/dependency_verification.html)

## Integration Contract References

None. This story introduces no new components and no integration boundaries.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Surfaces exclusively during IDE-driven source resolution. CLI workflow (`assembleDebug`,
`testDebugUnitTest`, `jacocoTestCoverageVerification`) is unaffected by the defect and must
remain unaffected by the fix. Detection context: identified during QA review of IDE workflow
as part of U-027 acceptance verification.
