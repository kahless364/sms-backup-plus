---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-018
started: 2026-11-12
completed: 2026-11-12
---

# Retro: sprint-018 — Remove the donation flow and Play Billing (U-061)

## Business Summary

**Objective.** Remove the in-app donation feature and the Google Play Billing dependency from the
app. The product owner's instruction was "remove the donation flow"; the app is a private build
("I don't own the old app store listing. this is just going to be a private app for me"), so a
donation screen that sells through someone else's store listing has no purpose. This is the first
build story under EPIC-MODERNIZATION-006 (dependency currency) and implements REQ-MODERNIZATION-018
as amended by CR-003. One story, U-061, in one wave.

**Outcome.** U-061 is `done` and sprint-018 is `completed`. Final QA: 26 of 26 criteria met (AC-1
to AC-12 and IC-1 to IC-14), six of them with a recorded deviation or waiver. QA re-derived almost
everything at commit `7dfd651b`; only the device check, one isolated test run and the per-increment
gate history rest on committed evidence.

- The donation screen, its settings entry, its 10 strings in 22 languages and its two test classes
  are gone. 38 product files changed: 62 lines added, 1246 removed.
- `com.android.billingclient:billing:7.1.1` and its 11 transitive modules (Google Play services,
  datatransport, Firebase encoders) are no longer in the app: 95 runtime modules before, 83 after,
  none added, no version changed on the rest.
- The packaged manifests (debug and release) differ by removals only: the donation activity, two
  billing proxy activities, the `com.android.vending.BILLING` permission, the `<queries>` entries
  and the Play services / datatransport components that came with billing.
- Tests: 697 before, 682 after (exactly the 15 donation tests). 0 failures, 2 skipped. Debug and
  release builds pass; every coverage floor is met without lowering one.
- REQ-MODERNIZATION-010 ("upgrade Play Billing to 7.x") and its design DES-MODERNIZATION-010 are now
  `superseded`, recorded by CR-004. They are the first superseded artifacts in this workspace.

**Key Deliverables.** Ten commits `e0c51d42..7dfd651b` (I0 baseline evidence, I1 to I9 product
changes): `app/src/main/AndroidManifest.xml`, `MainSettings.java`, `Preferences.java`,
`Consts.java`, `res/xml/preferences.xml`, 22 `strings.xml` files, five deleted source and test
files under `activity/donation/`, `EnrollmentDialogThemeTest.java`, `app/build.gradle`,
`app/lint-baseline.xml`, `app/proguard-rules.pro`, `README.md`, `CHANGES`,
`sdlc/artifacts/engagement/code-location.md`. Story workspace `U-061/`: plan, three plan-gate
results, implementation log, code review, security review, QA results and 97 evidence files.
Change records CR-003 (requirement scope) and CR-004 (supersession).

**Decisions Made.**
- Play Store visibility: removing billing also removed the manifest `<queries>` entries that let
  the app see the Play Store and Play services. The product owner accepted this ("option 1, accept
  it and delete the dead setting later"). On the API 36 emulator nothing observable changed, because
  both packages are visible to every app there.
- Release build: R8 failed after the removal because the removed `play-services-basement` had been
  supplying `-dontwarn com.google.errorprone.annotations.**`, which Tink still needs. The product
  owner approved adding that one rule to `app/proguard-rules.pro` ("Add the one-line rule"). This
  restores a rule that was already in effect; no keep rule changed.
- The on-device check ran with the app's third-party-integration setting on. The product owner
  waived that precondition ("Waive it"); no broadcast was sent and no backup ran.
- `gradle/verification-metadata.xml` was left unchanged. The stale billing checksums are inert
  (Gradle checks only what it resolves) and hand-editing checksum metadata is not allowed.
- Status of the superseded artifacts: the story required only the change-record entries and an
  annotation. The architect's advice was to also move both to `superseded`, since a misleading
  `approved` status was the defect; that was done through the Artifact Librarian.

## Process Observations

**Process Compliance.** The full chain ran: create-requirements (epic and five requirements) →
create-design (DES-MODERNIZATION-013) → create-cr (CR-003) → create-stories → create-sprint →
execute-story (planning, three plan gates, ten implementation increments, code review, security
review, QA) → create-cr (CR-004) → closing bookend. All required artifacts are present with passing
verdicts, after one rework at the plan gate.

**Deviations.**
- The story was run with `execute-story` directly, not `execute-sprint` (single story, no worktree
  isolation), as in sprint-017.
- Implementation ran as one increment per Developer turn, with the orchestrator committing each
  increment by explicit path. The plan's original snapshot-and-replay scheme was found unworkable
  at plan review.
- The create-cr skill was run in the middle of the story (between implementation and review) to
  produce CR-004, by pausing the execute-story context. Only one skill context can be active at a
  time, so the story context file was removed and re-written by hand around it.
- In CR-004 the Requirements Analyst wrote the `## Suggested Approach` section, which the skill
  assigns to the Solution Architect. The architect added an advisory subsection to it instead of a
  second section.
- The expert-depth-check FAIL (warning severity) was recorded and not sent back for another planning
  iteration.
- The security reviewer's completion report used a non-canonical status value (`in-review`); it was
  sent back for that one field and re-issued.
- U-061's `pipeline` frontmatter field is empty, as on every story in this project; the project
  default (`standard-story`) was used.

**Gate Results.**
- plan.md: PASS (two planning iterations).
- plan-review: FAIL, PASS. Iteration 1: a comment the plan itself added would have tripped the AC-1
  grep, and the snapshot-and-replay commit scheme could not work.
- contract-verification: PASS. expert-depth-check: FAIL at warning severity.
- implementation-log: PASS after increment I9.
- review-code: PASS (0 blockers, 0 warnings). review-security: PASS (4 Low, all deferred). No
  fix-in-cycle items.
- qa-results: PASS, 26 of 26.

**Issues Encountered.**
- Gate gap: the per-increment gates built only the debug variant. The R8 failure caused by removing
  the billing dependency in I5 was not seen until the release build in I8, three increments later.
- Requirement defects found after approval: REQ-MODERNIZATION-018 AC-3 omitted five manifest entries
  that billing brought in transitively, and its packaged-manifest comparison could not pass as
  written because resource ids are renumbered when resources are removed. Both were corrected by
  CR-003 before the story was written.
- The story's coverage audit failed once because the design's supersession decision (D8) had no
  criterion; IC-14 was added.
- Coverage nondeterminism: the `activity` package reads 189 or 199 covered lines on identical code.
  QA traced all ten lines to `MainViewModel.kt`, covered only when a WorkManager update arrives
  before the test ends. The gate cannot fail on it (the worst reading is far above the floor), but
  the figure is not stable. `receiver` remains at exactly its 50% floor with no headroom.
- JUnit totals: without the project's init script Gradle writes no JUnit XML, and stale XML from an
  earlier run was sitting in `app/build`. QA noticed before relying on it.
- The Developer agent deleted `*.py` files in the shared session scratch folder while tidying its own
  scripts. Nothing of the orchestrator's was lost, but the folder is shared.
- Supersession was deferred to a story criterion instead of being recorded when REQ-018 was
  created, which is why a change record had to be written mid-story.
- Framework (amp 1.7.2, Windows), unchanged from sprint-017: `compile-pipeline.mjs` prints nothing
  on Windows and needs the launcher workaround; `CLAUDE_PLUGIN_ROOT` is unset in shell commands.

**Not verified** (no criterion requires these): the release APK on a device; the first CI run on the
current toolchain.

**Recommendations.**
1. Add `:app:assembleRelease` to the standing gate set (CLAUDE.md "Gates" line and story templates)
   for any story that touches dependencies, the manifest or R8 rules.
2. Follow-up stories: delete the dead "Old backup scheduler" setting with `GooglePlayServices.java`
   and `App.gcmAvailable`; make `MainViewModelTest` deterministic; add a `BackupBroadcastReceiver`
   test to give `receiver` headroom (already recommended in sprint-017); remove the dead Otto and
   job-dispatcher keep rules from `app/proguard-rules.pro`.
3. Documentation follow-up: `sdlc/artifacts/engagement/endpoints.md:62` and `systems.md:55` still
   describe Play Billing. Fold this into the pending change record for the sibling artifacts
   (EPIC-MODERNIZATION-006 publishing wording, REQ-MODERNIZATION-015 AC-10, REQ-MODERNIZATION-017).
4. When a new requirement supersedes an old one, write the supersession change record in the same
   step.
5. When a requirement's criteria include a manifest or dependency list, derive the list from a
   build of the current tree before approval, not from the source manifest alone.
6. Give each agent its own scratch subfolder, and tell agents not to delete files they did not
   create.
7. Report to the AMP maintainers, in addition to the sprint-017 items: there is no defined way to
   run a second skill (such as create-cr) inside a running story.
