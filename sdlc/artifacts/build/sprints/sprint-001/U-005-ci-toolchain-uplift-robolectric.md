---
type: story
status: done
sprint: '000001'
artifact_type: user-story
priority: high
complexity: low
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-003
  - REQ-MODERNIZATION-001
design_docs:
  - DES-MODERNIZATION-003
integration_contracts: []
dependencies:
  - U-002
  - U-004
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-005
title: Upgrade test toolchain in the SDK-35 PR (Robolectric 4.12.x, JUnit 4.13.2, Mockito-core 5.x, Truth 1.4.x) and generate verification metadata
pipeline: ''
domain: modernization
requirement_source: authored
updated_at: '2026-06-02T19:33:42.543Z'
resolution: done
---

# U-005: Upgrade test toolchain in the SDK-35 PR (Robolectric 4.12.x, JUnit 4.13.2, Mockito-core 5.x, Truth 1.4.x) and generate verification metadata

## Story

As a contributor building the modernized SDK-35 branch,
I want the test toolchain upgraded from its 2015–2019 pinned versions to current stable releases in the same pull request that raises targetSdkVersion to 35,
So that the Robolectric suite remains runnable after the SDK raise (Robolectric 4.3.1 hard-caps at SDK 29), the JUnit CVE-2020-15250 advisory is resolved, the mockito-all uber-jar conflict is eliminated, and every resolved dependency is covered by a cryptographic integrity check before any Phase-2 substrate work begins.

## Acceptance Criteria

- [ ] AC-1: **Given** `app/build.gradle` before this story, **when** this story lands, **then** the Robolectric dependency at line 66 reads `org.robolectric:robolectric:4.12.x` where `x` is the highest stable patch release available at implementation time (4.12.0 at minimum); no other version of `robolectric` appears in `app/build.gradle` or any transitively applied `.gradle` file, verifiable via `./gradlew app:dependencies --configuration testRuntimeClasspath | grep robolectric`.

- [ ] AC-2: **Given** `app/build.gradle` before this story, **when** this story lands, **then** the JUnit dependency at line 65 reads `junit:junit:4.13.2`; no `junit:junit:4.12` or earlier version appears in the resolved `testRuntimeClasspath`, verifiable via `./gradlew app:dependencies --configuration testRuntimeClasspath | grep "junit:junit"`.

- [ ] AC-3: **Given** `app/build.gradle` before this story declares `org.mockito:mockito-all:1.10.17` at line 68, **when** this story lands, **then** (a) the `mockito-all` declaration is replaced with `org.mockito:mockito-core:5.x` where `x` is the highest stable minor release available at implementation time; (b) `mockito-all` does not appear in the resolved `testRuntimeClasspath`, verifiable via `./gradlew app:dependencies --configuration testRuntimeClasspath | grep mockito` producing no line containing `mockito-all`; and (c) the `mockito-core` version in the resolved graph is 5.0.0 or higher.

- [ ] AC-4: **Given** `app/build.gradle` before this story declares `com.google.truth:truth:0.39` at line 67, **when** this story lands, **then** the Truth dependency reads `com.google.truth:truth:1.4.x` where `x` is the highest stable patch release available at implementation time (1.4.0 at minimum), verifiable via `./gradlew app:dependencies --configuration testRuntimeClasspath | grep "truth:truth"`.

- [ ] AC-5: **Given** all four dependency changes in AC-1 through AC-4 are applied, **when** `./gradlew test` is executed against the updated build, **then** all 35 Robolectric test classes pass; no test class is deleted or disabled to achieve a green result; `@Config` annotation adjustments on individual test classes (for example, changing `sdk` values or adding explicit `manifest` paths required by Robolectric 4.12.x's stricter manifest resolution) are the only permissible changes to test source files; the logical assertions in every existing test method remain identical to those in the pre-change version.

- [ ] AC-6: **Given** this story lands in the same pull request as U-003 (the targetSdkVersion 35 raise and jcenter removal), **when** that combined PR is merged, **then** no intermediate commit on the `master` branch exists where `targetSdkVersion` exceeds 29 and `robolectric` is still at version `4.3.1`; `./gradlew test` must be green on every commit in the PR that is used as a merge base or that advances `master`.

- [ ] AC-7: **Given** all dependency changes and the SDK raise are applied in the combined PR, **when** `./gradlew --write-verification-metadata sha256` is executed against the final resolved dependency graph (after all changes in both U-003 and U-005 are applied, including jcenter removal from U-003), **then** the file `gradle/verification-metadata.xml` is created or fully regenerated and committed; it contains sha256 checksum entries for every resolved dependency in the graph including the JitPack-hosted k-9 IMAP library and the `maven.scijava.org`-hosted `firebase-jobdispatcher:0.8.6`; and the CI workflow (introduced by U-004) invokes Gradle with `--dependency-verification strict` (or the equivalent `--verify` flag accepted by the Gradle version in use) so that any checksum mismatch causes the build to exit non-zero.

- [ ] AC-8: **Given** `gradle/verification-metadata.xml` is committed, **when** any future pull request changes a dependency version or adds a new dependency to `app/build.gradle` or the root `build.gradle`, **then** the PR description or a comment in the commit that changes the dependency includes the output of `./gradlew --write-verification-metadata sha256` re-run on the updated graph; the updated `gradle/verification-metadata.xml` is committed in the same commit as the version change. This AC is a documented maintenance obligation, not a CI-enforced check at merge time, but its absence from a dependency-changing PR is a reviewer-blocking finding.

## Integration Criteria

No new production components are introduced by this story. Integration criteria are not applicable.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/build.gradle` | Line 65: `junit:junit:4.12`; line 66: `org.robolectric:robolectric:4.3.1`; line 67: `com.google.truth:truth:0.39`; line 68: `org.mockito:mockito-all:1.10.17` | Lines 65–68 updated to target versions; no other logic changes |
| `gradle/verification-metadata.xml` | Does not exist | Generated via `--write-verification-metadata sha256` after the combined U-003+U-005 dependency graph is finalized |
| `app/src/test/java/com/zegoggles/smssync/**/*.java` (up to 35 files) | Robolectric `@Config` annotations targeting SDK 29 or relying on 4.3.1 manifest-resolution behavior | `@Config` annotations adjusted where Robolectric 4.12.x requires explicit `sdk` or `manifest` values; test logic (assertions, setup, mocked interactions) unchanged |
| `app/proguard-rules.pro` | Existing keep rules | No change required by this story (R8 keep-rule work is scoped to U-004); included here because the combined PR touches `app/build.gradle` and reviewers must confirm this file is not inadvertently reverted |

## Existing Behavior to Preserve

- All 35 Robolectric test classes must continue to pass; no class may be deleted, renamed, or have its test methods removed.
- No change to production source files (`app/src/main/`) is within this story's scope; any production-source change required to make tests compile after the upgrade is a blocker that must be resolved without altering production behavior.
- The `testInstrumentationRunner` declaration and any `android.testOptions` blocks in `app/build.gradle` must be preserved unchanged.
- The `mockStatic` / `mockConstruction` style of mocking used with the existing Mockito version must continue to work; if `mockito-core:5.x` requires test-source adjustments for static or constructor mocking (beyond the scope of `@Config` annotations), those changes are permissible only if the underlying assertion semantics are identical.

## Verification Steps

1. Check out the combined U-003 + U-005 PR branch. Run `./gradlew app:dependencies --configuration testRuntimeClasspath | grep -E "robolectric|junit|mockito|truth"`. Confirm: `robolectric:4.12.` (not `4.3.1`), `junit:junit:4.13.2` (not `4.12`), `mockito-core:5.` with no `mockito-all`, `truth:1.4.` (not `0.39`).
2. Run `./gradlew app:dependencies --configuration testRuntimeClasspath | grep mockito`. Confirm no output line contains `mockito-all`.
3. Run `./gradlew test`. Confirm BUILD SUCCESSFUL with 35 test classes reported. Confirm the test output shows no classes skipped or disabled relative to the pre-change baseline.
4. Inspect `app/src/test/java/com/zegoggles/smssync/` and compare against the pre-change version. Confirm the only differences in test source files are `@Config` annotation parameter changes; no test method bodies differ.
5. Confirm `gradle/verification-metadata.xml` exists at the repo root under `gradle/`. Open the file and confirm it contains `<component>` entries that include at least one entry for the JitPack k-9 artifact and one for `firebase-jobdispatcher`. Confirm every entry has a `<sha256 value="..."/>` element with a 64-character hex string.
6. Review `.github/workflows/ci.yml` (introduced by U-004) and confirm the `./gradlew` invocation includes `--dependency-verification strict` or `--verify`; trigger a CI run on the combined PR and confirm the "verification metadata" step does not produce a failure (no tampered checksum in the baseline run).
7. On the PR's commit list, confirm there is no commit where both conditions hold simultaneously: `targetSdkVersion` > 29 in `app/build.gradle` AND `robolectric:4.3.1` in `app/build.gradle`.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / Gradle | All changes: `app/build.gradle` dependency lines 65–68, `gradle/verification-metadata.xml` generation, any `@Config` annotation adjustments in test sources | Developer |

## Technical Notes

### Co-requisite coupling with U-003 — this story cannot land independently

Robolectric `4.3.1` (`app/build.gradle:66`) executes tests only against SDK 29 and below. U-003 raises `targetSdkVersion` to 35. The instant that raise lands on `master` with Robolectric still at `4.3.1`, `./gradlew test` fails for every test class that does not carry an explicit `@Config(sdk = 29)` override. There is no valid intermediate state. U-005 and U-003 must be delivered as a single pull request. The dependency edge in this story's frontmatter records U-003 as a co-requisite, not a predecessor.

### Exact version targets (as of authoring; implementer must confirm latest stable at implementation time)

| Artifact | Current (verified) | Target floor | Preferred (highest stable) |
|---|---|---|---|
| `org.robolectric:robolectric` | `4.3.1` | `4.12.0` | Highest `4.12.x` stable |
| `junit:junit` | `4.12` | `4.13.2` | `4.13.2` (CVE-2020-15250 fix; no `4.14` stable exists as of authoring) |
| `org.mockito:mockito-core` | `mockito-all:1.10.17` (uber-jar) | `5.0.0` | Highest `5.x` stable |
| `com.google.truth:truth` | `0.39` | `1.4.0` | Highest `1.4.x` stable |

### Robolectric 4.12.x @Config migration notes

Robolectric 4.x series progressively tightened manifest and SDK resolution. `AppTest.java` already uses `@Config(sdk = 18)` (verified), confirming the pattern is established in this suite. Known adjustment areas:
- Classes that relied on implicit SDK resolution at 4.3.1's default (SDK 16 or the `targetSdk`) may need an explicit `@Config(sdk = 29)` or a higher value now that `targetSdkVersion = 35`.
- If any test class used `@Config(constants = BuildConfig.class)`, that attribute was removed in Robolectric 4.x; replace with `@Config(application = Application.class)` or remove.
- `@RunWith(RobolectricTestRunner.class)` does not change.
- No test logic (assertions, `verify()`, `given()`) may change.

### Supply-chain verification timing

`gradle/verification-metadata.xml` must be generated after the full U-003 + U-005 dependency graph is finalized — including jcenter removal (U-003) — because removing jcenter changes which repository resolves several artifacts, which changes their binary contents and therefore their sha256 checksums. Generating the metadata before jcenter removal and then removing jcenter in a subsequent commit produces stale checksums and will cause `--verify` to fail on CI.

### mockito-all removal: potential classpath conflict

`mockito-all:1.10.17` bundles its own copies of ASM, Objenesis, and ByteBuddy at old versions. `mockito-core:5.x` pulls those as separate `compile`-scope dependencies at current versions. If any other test dependency in the graph also pulls those transitively, Gradle's standard resolution will pick the higher version. The implementer must run `./gradlew app:dependencies --configuration testRuntimeClasspath` after the change and confirm no version-conflict warning appears for `asm`, `objenesis`, or `byte-buddy`.

## Supporting Documentation

- `REQ-MODERNIZATION-003#acceptance-criteria` — AC-7 (toolchain uplift), AC-8 (verification metadata), AC-7e (same-PR constraint with SDK raise)
- `REQ-MODERNIZATION-003#constraints` — Robolectric co-requisite with REQ-MODERNIZATION-001
- `DES-MODERNIZATION-003#test-toolchain-uplift-ac-7--co-requisite-with-the-sdk-raise` — exact version table and same-PR rationale
- `DES-MODERNIZATION-003#supply-chain-verification-metadata-ac-8` — timing constraint for metadata generation relative to jcenter removal
- `DES-MODERNIZATION-003#design-validation` — AC-7 and AC-8 rows in validation table

## Integration Contract References

None. This story introduces no new cross-component runtime boundary and no data exchange contract.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Draft — toolchain-uplift scope narrowed to `app/build.gradle:65–68` dependency lines and `gradle/verification-metadata.xml` generation; co-requisite coupling with U-003 (SDK-35 raise) is the single most load-bearing constraint and is recorded in both frontmatter dependencies and Technical Notes; not finalized pending review of the combined U-003/U-005 PR scope boundary.
