---
type: story
status: done
artifact_type: user-story
priority: medium
complexity: medium
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
id: U-059
title: Flip AGP-8 R-class defaults, drop enableJetifier and dead jitpack repo, add CI coverage verification
pipeline: ''
domain: modernization
resolution: done
requirement_source: assessment:20260623-post-migration-assessment#BT-002
sprint: '000015'
---

# U-059: Flip AGP-8 R-class defaults, drop enableJetifier and dead jitpack repo, add CI coverage verification

## Story
As a maintainer of the SMS Backup+ codebase, I want `nonTransitiveRClass` and `nonFinalResIds` flipped to `true`, `enableJetifier` removed, the dead `jitpack.io` repository declaration dropped, and `jacocoTestCoverageVerification` added as an explicit CI step, so that the build uses AGP-8 defaults, eliminates legacy Jetifier overhead, removes a resolver entry that points nowhere, and ensures the coverage gate is enforced on every CI run.

## Source
Derived from assessment 20260623-post-migration-assessment, findings BT-002, BT-005, and BT-006 (merged). See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/build-toolchain.md`.

## Acceptance Criteria
- [ ] AC-1: `gradle.properties:22-26` is updated to set `android.nonTransitiveRClass=true` and `android.nonFinalResIds=true`; all R-class references in source files that break under non-transitive R-class (i.e., references to resources from transitive dependencies that must now be fully qualified) are fixed so `./gradlew :app:assembleDebug` compiles clean.
- [ ] AC-2: `gradle.properties:17` (or equivalent location) no longer contains `android.enableJetifier=true`; the line is removed entirely (not commented out); `./gradlew :app:assembleDebug` succeeds without Jetifier, confirming no remaining Android Support Library artifacts require transformation.
- [ ] AC-3: The `jitpack.io` repository entry in `build.gradle:29` (root or `settings.gradle`) is removed; `./gradlew :app:assembleDebug` resolves all dependencies from google, mavenCentral, and the local vendored module without contacting jitpack.
- [ ] AC-4: `.github/workflows/ci.yml` is updated to add an explicit `./gradlew :app:jacocoTestCoverageVerification` step after the existing `jacocoTestReport` step; the CI job fails if the coverage gate fails (exit code propagated correctly); this is in addition to (not replacing) the existing `test lint jacocoTestReport assembleRelease` steps.
- [ ] AC-5: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` — all pass; `gradle/verification-metadata.xml` remains valid (no new unverified artifacts introduced by this change).

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `gradle.properties:17` | `android.enableJetifier=true` | Remove this line |
| `gradle.properties:22-26` | `android.nonTransitiveRClass=false`, `android.nonFinalResIds=false` | Set both to `true` |
| `build.gradle:29` (root) | `maven { url 'https://jitpack.io' }` repository declaration | Remove the jitpack block |
| `app/src/main/java/**/*.java` and `app/src/main/java/**/*.kt` (any files with transitive R references) | May reference R.string/layout/drawable from transitive deps without package qualification | Add fully-qualified R-class imports where broken by `nonTransitiveRClass=true` |
| `.github/workflows/ci.yml` | Runs `test lint jacocoTestReport assembleRelease`; no explicit `jacocoTestCoverageVerification` step | Add explicit `jacocoTestCoverageVerification` step after `jacocoTestReport` |

## Existing Behavior to Preserve
- All existing resource references (`R.string.*`, `R.layout.*`, `R.drawable.*`, etc.) in app source must resolve correctly after enabling `nonTransitiveRClass`; compilation must not regress.
- `nonFinalResIds=true` changes R field finality but must not break any `switch` statement or annotation that requires compile-time constants on R values; convert any such `switch` to `if-else` if needed.
- The supply-chain verification (`gradle/verification-metadata.xml` with `verify-metadata=true`) must remain intact; removing jitpack must not invalidate existing SHA-256 entries.
- CI behavior: the existing test, lint, and release-build steps must be unaffected by the coverage-verification addition; only a gate failure causes the new step to fail CI (it must not produce false failures on currently-passing code).

## Verification Steps
1. Run `./gradlew :app:assembleDebug` after flipping `nonTransitiveRClass=true` — confirm zero R-class compilation errors; if errors exist, fix and re-run before proceeding.
2. Run `./gradlew :app:assembleDebug` with `enableJetifier` removed — confirm no `Support Library` transformation warnings or errors.
3. Run `./gradlew :app:assembleDebug` with jitpack removed — confirm dependency resolution completes without jitpack DNS lookups (network log or Gradle `--info` output shows no `jitpack.io` request).
4. Run `./gradlew :app:testDebugUnitTest` — all tests pass.
5. Run `./gradlew :app:jacocoTestCoverageVerification` — gate passes.
6. Push to a branch; confirm CI run shows `jacocoTestCoverageVerification` as a named step in the Actions log and that it exits 0.

## Technical Context
- BT-002 root cause: `nonTransitiveRClass=false` and `nonFinalResIds=false` are AGP-7 legacy opt-outs deferred during migration. AGP-8 flips both by default; keeping them at `false` means the project will require a larger flag-flip at a future forced upgrade. Flipping now while the app is small is the right time.
- `nonTransitiveRClass=true` means each module only sees its own R values; resource references from transitive dependencies must be accessed via the dep's own package. In practice, this rarely requires more than import fixes.
- BT-005 (dead jitpack): the jitpack repo was added for the original k-9 dependency before vendoring. It now resolves nothing but adds a network round-trip and a potential supply-chain attack surface to every build.
- BT-006 (CI coverage step): `jacocoTestReport` generates the report but does not enforce the gate; only `jacocoTestCoverageVerification` fails the build. Running only `jacocoTestReport` in CI means a coverage regression passes CI silently until the gate is run locally. This is the same CI coverage gap identified in the health report.
- This story should be sequenced after U-051 (which widens the gate to all packages) so the explicit CI step validates the full widened gate, not just the 3-package original.

## Notes
- Sprint D (Toolchain Currency). Small-to-medium effort; mostly config changes with one potential source-fix pass for R-class references.
- BT-002, BT-005, and BT-006 are merged here as a coherent "build-hygiene" changeset; they touch overlapping files (`build.gradle`, `gradle.properties`, CI config) and are cheapest as one PR.
- Coordinate with U-051 on the `jacocoTestCoverageVerification` CI step — U-051 may already add it in Sprint B; if so, AC-4 of this story becomes a no-op (confirm and note in the story's implementation notes).
