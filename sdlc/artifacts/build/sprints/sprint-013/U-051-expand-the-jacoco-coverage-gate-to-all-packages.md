---
type: story
status: done
artifact_type: user-story
priority: high
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
id: U-051
title: Expand the jacoco coverage gate to all packages
pipeline: ''
domain: modernization
requirement_source: assessment:20260623-post-migration-assessment#TE-001
sprint: '000013'
updated_at: '2026-06-23T22:23:33.411Z'
resolution: done
---

# U-051: Expand the jacoco coverage gate to all packages

## Story
As a maintainer of the SMS Backup+ codebase, I want `jacocoTestCoverageVerification` to enforce a minimum LINE coverage threshold on all ~13 source packages (not just `service*`, `mail*`, `auth*`), so that untested code in `activity`, `preferences`, `calendar`, `contacts`, `di`, `receiver`, `scheduler`, `compat`, `utils`, and `worker` becomes visible and enforced rather than silently ungated.

## Source
Derived from assessment 20260623-post-migration-assessment, finding TE-001. See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/testing.md`.

## Acceptance Criteria
- [ ] AC-1: The `jacocoTestCoverageVerification` task in `app/build.gradle` includes explicit `limit` entries for every source package under `com.zegoggles.smssync.*`; no package is ungated; the three previously-gated packages (`service*`, `mail*`, `auth*`) retain their existing ≥ 70% LINE threshold.
- [ ] AC-2: Newly-gated packages (`activity*`, `preferences*`, `calendar*`, `contacts*`, `di*`, `receiver*`, `scheduler*`, `compat*`, `utils*`, `worker*`) are added with a floor of LINE ≥ 50%, making their current coverage visible and enforced without immediately failing the build on low-covered packages.
- [ ] AC-3: The `jacocoFileFilter` exclusions for `BackupImapStoreDelegate*`, `BackupWorker*`, and `RestoreWorker*` are NOT removed by this story — they remain in place because IMAP integration coverage (U-052) is a prerequisite for removing them safely.
- [ ] AC-4: `./gradlew :app:jacocoTestCoverageVerification` passes (green) on the current test suite without adding any new tests; any package that currently falls below the new 50% floor for newly-gated packages must have its threshold set to its actual current coverage or just below, with a comment marking it for uplift.
- [ ] AC-5: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` — the gate passes, the existing ≥70% packages still pass, and the CI step in `.github/workflows/ci.yml` is updated to run `jacocoTestCoverageVerification` explicitly (not just `jacocoTestReport`).

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/build.gradle` (`jacocoTestCoverageVerification` block, ~lines 321-361) | Enforces LINE ≥ 70% on `service*`, `mail*`, `auth*` only; ~10 packages have no `limit` entry | Add `limit` entries for all ~13 packages; set newly-gated packages to LINE ≥ 50% (or actual floor) |
| `.github/workflows/ci.yml` | Runs `jacocoTestReport`; gate task not explicitly invoked | Add `jacocoTestCoverageVerification` as an explicit CI step after `jacocoTestReport` |

## Existing Behavior to Preserve
- Existing ≥ 70% LINE thresholds for `service*`, `mail*`, `auth*` must not be reduced.
- The `jacocoFileFilter` exclusions for `BackupImapStoreDelegate*`, `BackupWorker*`, `RestoreWorker*` must remain unchanged — removing them is U-052's job after IMAP integration tests exist.
- The documented false-green fix (noted in `app/build.gradle:~321-361`) that prevents Jacoco from reporting green on zero-covered classes must not be disturbed.

## Verification Steps
1. Run `./gradlew :app:jacocoTestReport` followed by `./gradlew :app:jacocoTestCoverageVerification` — the verification task must exit 0.
2. Open `app/build/reports/jacoco/testDebugUnitTest/html/index.html` — confirm all ~13 packages appear; confirm no package listed in the report is absent from a `limit` entry in `build.gradle`.
3. Introduce a deliberate coverage regression in a newly-gated package (e.g. delete one test method temporarily) and re-run the gate — confirm it fails; revert.
4. Push to a branch and confirm CI runs `jacocoTestCoverageVerification` as an explicit step in `.github/workflows/ci.yml` output.

## Technical Context
- TE-001 root cause: the gate was written when only three packages had meaningful test coverage; the other ~10 were excluded by omission rather than by design. Setting a 50% floor (rather than 70%) for newly-gated packages avoids immediately failing CI on code that has never been gated, while making it impossible to add new uncovered code without the gate catching it.
- The correct pattern for a package that currently has, say, 35% coverage is to set its threshold to 35% (or 30%) with a `// TODO: raise to 70% after U-052` comment — not to exclude it. This makes the real number visible without breaking the build.
- U-059 (flip AGP-8 defaults) also touches `build.gradle` and CI; coordinate to avoid a merge conflict on the `jacocoTestCoverageVerification` block.

## Notes
- Sprint B (Test Visibility & Coverage). Do this story first within the sprint, before U-052 and U-053, because it makes untested code visible without requiring any new tests.
- BT-006 (CI coverage step) is merged into AC-5 of this story — adding the explicit CI step and widening the gate are one coherent change.
