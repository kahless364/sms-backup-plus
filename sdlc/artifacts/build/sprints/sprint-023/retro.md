---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-023
started: 2026-11-21
completed: 2026-11-21
---

# Retro: sprint-023 — the workers' cancellation checks observe the running job (U-067)

## Business Summary

**Objective.** Close BUG-022 (`sdlc/artifacts/stories/BUG-022-worker-cancellation-checks-do-nothing.md`). Inside the backup
and restore workers the name `coroutineContext` resolved to the worker's own deprecated member, which is a dispatcher and
holds no job. The three checks that were meant to stop a cancelled run at the top of each pass could therefore never throw.
The fix makes them read the running coroutine's context, and each of the three sites now has a test that cancels the work
mid-loop. The order (BUG-022, then BUG-023, then BUG-021, before the runtime-library story) is the product owner's, Record 15 of the
decision appendix of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`
(`sprint-023/sprint-plan.md`, Goals). The sprint was one small story so that the fix could start at once
(`sprint-plan.md`, Notes).

**Outcome.** U-067 is `done`, sprint-023 is `completed` (`sdlc/artifacts/build/sprints/sprint-023/sprint-plan.md`) and
BUG-022 is closed (`sdlc/artifacts/stories/BUG-022-worker-cancellation-checks-do-nothing.md`, status `done`).

- QA: 16 of 16 criteria met, two of them with deviations that were ruled or stated before the build: the cursor-close half
  of AC-1 (c) cannot be seen through the test seam and was judged by reading the code path (ruling P-1), and AC-4's
  checkpoint assertion cannot tell "never written" from "index 0", so the zero-row count carries the criterion
  (`U-067/qa-results.md`, "Deviations").
- The authoritative test count went from 746 to 753 (seven added, none removed), 0 failures, 2 skipped as before. QA's own
  gate run gave the same figures, and JaCoCo and the release build passed (`qa-results.md`, "Gate and counts").
- Four of the seven tests are cancellation tests and fail without the fix (expected 2 but was 4, expected 2 but was 4,
  expected 3 but was 5, expected 0 but was 5); the other three are controls that pass with and without it
  (`U-067/implementation-log.md`, I3).
- Six logged mutations (the old binding and a deleted check, at each of the three sites) were all detected with the failing
  sets the plan predicted; QA re-applied three of them and saw the same sets (`U-067/evidence/mutation-log.md`,
  `qa-results.md`, "QA mutations").
- The Kotlin compiler's three warnings about the shadowed name are gone: 9 warnings before, 6 after, none naming either
  worker (`implementation-log.md`, I7; `qa-results.md`, AC-7).
- Code review PASS with no blocker and no warning (`U-067/review-code.md`). Security review PASS with no Critical, High or
  Medium finding and three Low findings deferred (`U-067/review-security.md`; summary in the story, Review Findings).

**Key Deliverables.**

- Commit `c12a2788` (U-067): the three checks read `currentCoroutineContext().ensureActive()`; the two imports are replaced
  in place; `backupCursors` is `internal` with `@VisibleForTesting` (Route S, ruled before planning). Three call lines, two
  imports and one visibility change in production (`BackupWorker.kt`, `RestoreWorker.kt`); seven tests in one new and two
  existing test classes; the false Javadoc in `BackupWorkerTest.kt` corrected (comment only).
- Plan commit `c9770c30` (plan, Amendment A1 and the three plan-gate reports); close-out commit `4300ffa9`.
- BUG-024, raised from this sprint's reviews and still `draft`
  (`sdlc/artifacts/stories/BUG-024-cancelled-backup-or-restore-reported-as-failure.md`).

**Decisions Made.**

- Orchestrator, before planning: DP-1 in (the Javadoc is corrected) and DP-2 Route S (the seam; no fallback of "record why
  the site is not tested"). The recommended options were taken, so no criterion changed (story, Implementation Notes).
- Orchestrator, on the plan review: P-1, the cursor-close half of AC-1 (c) is declared not observable through the seam and
  QA judges it by reading; the two corrections from the plan (two callers of `backupCursors`; Javadoc at `:27-30`) are
  accepted (story, Implementation Notes).
- Orchestrator: the cancelled-run-returns-`Result.failure` concern stays out of U-067 and is raised as a separate bug.
- Carried forward: the toolchain story (its Kotlin increment) and the runtime-library story (its coroutines and WorkManager
  increments) re-run U-067's six mutations, not only its tests; contract line citations into `BackupWorker.kt` shift by
  one line, so the next contract amendment cites by function name (story, Review Findings).

## Process Observations

**Process Compliance.** The chain ran as the framework defines it for one story: plan, the three plan gates, implementation,
code review and security review, a story-scoped commit, QA and the closing step. All expected phase artifacts are present
with passing verdicts, except the depth check, which failed at warning level only (below). The plan had one iteration plus
Amendment A1 (`U-067/plan.md`, top).

**What went well.**

- The plan read the compiled libraries to establish the mechanism, instead of asserting it. `javap` of work-runtime 2.9.1 and
  kotlinx-coroutines 1.7.3 showed that the inherited member is a dispatcher, that `ensureActive()` returns at once on a
  context without a job, and that under the work-testing harness `setProgress` and `setForeground` return completed
  futures, so they are not cancellation points (`U-067/plan.md`, Assumption Ledger L5 to L7; A1 added L5b for
  `startWork` and `onStopped`; `expert-depth-check.md`, summary). That last fact is why the loop's own check is the only
  place a loop can stop in a unit test, and so why each new test can fail without the fix. The red run confirmed it: exactly the four cancellation
  tests failed, the three controls and the 60 existing cases passed (`implementation-log.md`, I3).
- The developer's run followed the plan with no decision row firing. D1 and D2 were checked and not triggered; D3 to D15 are
  recorded as not used (`implementation-log.md`, I0 to I8). The mutation run needed no retry, which sprint 022's did.
- Reviews read the commit and QA mutated the working tree. The story was committed first (`c12a2788`), and both reviews state
  that they read from the commit and not from the working tree (`review-code.md`, `review-security.md`). QA's three
  mutations were applied to the working tree afterwards, each restored with `git checkout`, with the file hash equal to the
  committed blob and an empty `git status` before and after (`qa-results.md`, "QA mutations"). The reviewers' reading
  and QA's edits could not disturb each other. The sources give the file times of the reviews (05:07 local) and of QA's
  runs (from 05:10), not the times the reviews started, so how far the two overlapped is not established.
- The plan was amended so its end-of-run checks fit a shared checkout. Plan review O-1 found that another planner was
  writing under `sprint-024/` during the build, and that the plan's end-of-run records check would list those files
  (`U-067/plan-review.md`, O-1). Amendment A1 scoped the checks to the story's own paths, `app`, `k9mail-vendored`, the build files and
  the workspace, and added a "no code commit since the start" check (`plan.md`, Amendment A1, row plan-review O-1). The
  developer's end checks passed as scoped; documents were committed in the meantime and no code was
  (`implementation-log.md`, I8). This is the sprint 022 recommendation, taken before the build.
- The reviews found a real follow-up and a hazard for a later story. Both reviews found that a cancelled run is caught by the
  workers' broad catches, logged at error level and returned as `Result.failure`. Writing it up as BUG-024 added that
  WorkManager 2.12.0 treats a cancellation that is not its own stop as a failure (read in the library sources, not run), so
  the runtime-library story must re-run the cancellation tests and mutations
  (`BUG-024-...md`, Technical Context and Not verified, item 3; `review-security.md`, W-1).

**What did not go well, and what to change.**

- The depth check failed at warning level, on text. Four criteria were "satisfied with a recommendation" against a limit of
  two: structured concurrency, cooperative cancellation, mutation testing and the test-double patterns were used and not
  named; the broad catches were not named as a swallowed `CancellationException`; `runTest`, an import alias and `yield()`
  were not weighed; and the claim about WorkManager's stop rested on a ledger row that did not show it
  (`U-067/expert-depth-check.md`, Findings). No edit, test or mutation changed; Amendment A1 added the text. The gate states that none of the findings changes an edit, a test or a
  mutation row. Change: write the named principles, sources, rejected alternatives and edge cases in the
  first iteration. The U-068 plan has a section "2.0 Principles applied" in iteration 1 and its depth check passed with
  two recommendations (`sprint-024/U-068/plan.md`, "Iteration 2" and 2.0); whether it was written with this finding in mind
  is not stated in it.
- Rulings were written into stories as notes after the criteria were written. The alignment audits of the next sprint's
  design stories failed, and for U-073 the first audit's findings were four points where the criteria had not taken up
  the rulings; the second audit failed on three narrower points, among them ruling citations, and a third pass was
  authorised (`sdlc/artifacts/stories/U-073-runtime-libraries-latest-stable.md`, Implementation Notes, last paragraph; the
  commit messages `fb707d7e` and `d73017de` give the pass counts for U-071 to U-073). Change: send each ruling to the
  Requirements Analyst to fold into the criteria before the alignment audit, so the audit compares one text.
- The plan's end-of-run check was written for an empty checkout and had to be amended after the fact (above). Change: the
  plan template scopes every end-of-run check to the story's own file list from the first iteration when the sprint
  record says that other work shares the checkout.

**Gate Results.**

| Story | plan-review | contract-verification | expert-depth-check | code-review | security-review | qa-validation |
|-------|-------------|-----------------------|--------------------|-------------|-----------------|---------------|
| U-067 | PASS (seven observations, all taken into A1) | PASS (four observations) | FAIL (warning only; named principles, sources, edge cases), taken up in A1 | PASS | PASS | PASS |

**Issues Encountered.**

- Planning for sprint 024 ran in the same hours in the same checkout: plans for U-068 and U-070, and story texts with
  alignment audits for U-071 to U-073 (`git log` from `4095db28` to `4300ffa9`). It did not collide with the build. During the
  build the only Gradle runs in the story's workspace were the developer's, and QA's gate run notes that no other Gradle ran
  meanwhile (`qa-results.md`, "Gate and counts"). The developer's HEAD moved from `c9770c30` to `fb707d7e` by document commits only
  (`implementation-log.md`, I8).
- The code review lists Low or Info observations to leave: the tests do not close their cursors in a `finally`, one
  assertion cannot fail on its own, the KDoc above `backupCursors` does not mention the new visibility, and two comments
  say "next suspension point" where "next pass" is exact (story, Review Findings).

**Not verified** (no criterion requires these): that WorkManager's own stop reaches the loop through the same job in
production. The tests call the worker directly, not through `startWork()`, and the caller of `onStopped()` was not read
(`implementation-log.md`, Runtime Claims; security review W-3). No device or emulator was used (IC-4).

**Recommendations.**

- Put the named principles, sources, rejected alternatives and edge cases in the first plan iteration (above).
- Fold each orchestrator ruling into the story's criteria through the analyst before the alignment audit (above).
- Scope the plan's end-of-run checks to the story's own paths when other work shares the checkout (above).
- Open items for the product owner, none blocking: BUG-024 is `draft` and needs its Suggested Approach; whether
  `WorkManagerCancelCollector` is ruled in (BUG-024, Scope Concern SC-2); whether the skipped thread refresh on a cancelled
  restore (SC-1) is worth a change.

**What comes next.** Sprint 024: U-068 (minSdk 24) is being built; U-069 (the BUG-023 fix) and U-070 (the BUG-021 fix)
follow, one at a time; then the BUG-024 fix and the remaining design stories U-071 to U-073, where the runtime-library story
re-runs U-067's six mutations.
