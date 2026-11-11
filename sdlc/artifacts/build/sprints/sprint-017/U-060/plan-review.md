---
artifact_type: gate-result
story_id: "U-060"
gate_id: "plan-review"
gate: plan-review
checkpoint: "plan-gate"
verdict: "FAIL"
agent: "Solution Architect (separate instance, plan-review gate)"
timestamp: "2026-11-11"
iteration: 2
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md (revision 2, delta vs 2334fc6d)"
blockers: 1
warnings: 6
---

# Gate Report: gate-result — U-060

## Gate: plan-review @ plan-gate (iteration 2 of 3)

## Verdict: FAIL

## Summary

Revision 2 resolves all three iteration-1 blockers (B-1, B-2, B-3) and all seven observations (N-1 to N-7), with evidence I re-checked independently.
- **Gradle 9.5.1 is accepted.** The move from 9.3.1 is well-argued: it is stronger on the two sources AC-11 actually names. It also introduces no compatibility problem that 9.3.1 did not have.
- **The rollback is now safe.**
- **Branch M is executable without further decisions.**

One defect remains. Trigger **T3** (Robolectric 4.15.1 fails on JDK 25) raises the app's minSdk to 23, and nothing technical forces that. It would drop Android 5.0/5.1 users for a test-tooling reason. That overreaches the product-owner approval, which was granted as permission rather than mandate, with Android 5.x to be kept where keeping it costs nothing risky. The fix is a small, local edit to the T3 row and M.1/M.2/M.10.

## Iteration 1 findings — disposition

| Finding | Status | Evidence checked this iteration |
|---|---|---|
| B-1 misquoted Gradle compatibility citations | **Resolved** | Verbatim quotes match what I fetched for 9.1.0, 9.2.1, 9.3.0, 9.3.1, 9.4.0, 9.4.1, 9.5.0 and 9.5.1. I did not independently re-fetch the 9.6 and 9.7 rows in SC-5; they do not bear on the decision. SC-5 records the reading of AC-11 and the residual risk. |
| B-2 repo-wide `git restore -- .` | **Resolved** | See "Rollback" below. |
| B-3 product-owner minSdk approval | **Resolved, with one overreach** | Branch M is fully specified. T3 is new blocker R2-B1. |
| N-1 Fallback B2 was a no-op | Resolved (dropped) | `hilt-compiler-1.3.0.pom` uses `room-compiler-processing` 2.6.0, the same as 1.2.0, so forcing a newer one targeted the wrong component. |
| N-2 mixed androidx.hilt versions | Resolved (not adopted) | — |
| N-3 bootstrap checksum | Resolved | Step 1 sets `distributionSha256Sum`. |
| N-4 HEAD mismatch | Resolved | `git diff --stat b09bda5d HEAD -- . ':!sdlc'` is empty today, so the Step 0.1 guard works as written. |
| N-5 CI daemon JVM | Resolved (R14) | See observation O-3 on the containment syntax. |
| N-6 AC-6 wording | Resolved | 12.1 and 12.2 are written from the observed M2/M4 results. |
| N-7 `-proc` | Resolved | Added to the B-javac watch list. |

## Findings

### Blocking (must be fixed in plan.md before implementation)

**R2-B1: Trigger T3 raises minSdk without technical necessity, which overreaches the product-owner approval.**

The T3 row (Branch M table) reads: Robolectric 4.15.1 → 4.16.1, and "the app's minSdk is raised as well, so that the Robolectric-testable floor equals the app's floor". The stated reason is a test-coverage preference, not a constraint.

Why minSdk 23 is not needed under T3:
- Robolectric 4.16 dropping SDK 21/22 means only that *tests* cannot run at 21/22. The four `SmsReceiverTest` tests can be retargeted to `M` exactly as in M.2 while the app keeps minSdk 21.
- Every other Robolectric test runs at 29 (`robolectric.properties` `sdk=29`), at Q (`MainActivityRestoreTest`), or at 23 (`WorkManagerSchedulerTest`). None needs SDK 21/22.
- The plan itself shows that API 21 and API 23 take the same `SmsReceiver` branch (`>= KITKAT && < Q`). Moving those tests to 23 therefore loses no meaningful coverage.

Under T3 alone, then, the cost of keeping Android 5.x is a documented test-floor gap (API 21–22 not emulated). That is not a risky product change, and the product owner asked for exactly that trade-off.

The other two triggers are legitimate:
- **T1:** `hilt-work` 1.3.0/1.4.0 declare `minSdkVersion="23"`, so manifest merge fails at 21.
- **T2:** Hilt 2.60 itself declares "minSDK for Hilt is now 23".

Required change to plan.md:
1. **T3 alone** becomes: Robolectric 4.16.1, plus the M.2 retarget of the four `@Config` lines to `M`. **minSdk stays 21.** The comment text says the tests run at 23 because Robolectric 4.16 cannot emulate 21/22, not because minSdk was raised. No `CHANGES` entry and no M.7/M.8 minSdk documentation.
   - Record in Implementation Notes: "API 21–22 not Robolectric-testable from U-060; app still supports them".
   - Story amendment is needed only for AC-10 (configured SDK set {23, 29}) and the AC-2 note that retargeting is not disabling. It is not needed for the runtime-behavior scope statement.
2. Raise minSdk under T3 only if Robolectric 4.16.1 **demonstrably refuses** to run with a manifest minSdk of 21. Save the error to evidence; that makes the raise technically forced.
3. If T3 combines with T1 or T2, the full Branch M applies as written.
4. Update the T3 row, M.1 ("T3 only"), M.2 comment text, M.10, R8, and the "ACs touched if Branch M triggers" block to match.

### Non-blocking observations

- **O-1: Assessment of Gradle 9.5.1 (accepted).** The checksums match services.gradle.org: bin zip `bafc141b619ad6350fd975fc903156dd5c151998cc8b058e8c1044ab5f7b031f`, wrapper jar `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`.
  - **Quotes verified:** the 9.5.x page reads "9.0 through 9.2.0-alpha05" and Kotlin "2.0.0 through 2.3.20". The upgrade guide confirms "The embedded Kotlin has been upgraded from 2.3.10 to Kotlin 2.3.20", the 9.4 system-property-priority change, and the reworked Windows start script.
  - **Consistency:** the dependent values are updated in Steps 1, 3.1, 5, 7.4, 8.1 and 12.1, the AC table, and the Files table. A grep shows no leftover 9.3.1 checksum, and every remaining "9.3.1" mention is rationale or revision log.
  - **Set coherence:** the set is internally consistent and defensible:
    - AGP 9.0.x publishes only *minimums* (Gradle ≥ 9.1.0), not a maximum, and Gradle 9.5 lists AGP 9.0 as tested, so there is no AGP-side ceiling problem.
    - The Hilt plugin states only "Gradle 9.1+".
    - KSP 2.3.6 predates Gradle 9.5, but I found no KSP or Dagger issues mentioning Gradle 9.5 (GitHub search).
  - **New exposure:** the one exposure 9.3.1 did not have is KGP 2.3.21, which sits two minors above the Kotlin table's "fully supported" Gradle maximum (R16). It is correctly disclosed.
- **O-2: Pre-specify an R16 / KSP fallback instead of a bare stop.** If a KGP or KSP error appears that is specific to Gradle 9.5, the obvious pre-justified fallback is the iteration-1 set: wrapper 9.3.1, with its already verified checksums `b266d5ff…`/`b3a875dd…`. Its compatibility reading is already written up in SC-5. A one-line "fallback: 9.3.1 wrapper, re-run Steps 1/5/6" would avoid a mid-build stall. This is optional.
- **O-3: The R14 containment syntax is uncertain.** `-Dorg.gradle.java.installations.fromEnv=…` on the `./gradlew` line may not be honoured for *daemon* JVM selection, which happens client-side. The robust, documented form is to write the property into the runner's user-home `gradle.properties` in a CI step, e.g. `echo "org.gradle.java.installations.fromEnv=JAVA_HOME_25_X64" >> ~/.gradle/gradle.properties`.
- **O-4: `story_amendment_required: true` is not a field in the standard Phase Completion Report.** The orchestrator must be told to look for it, or the Developer should also state it in prose under `errors`/notes so it cannot be dropped.
- **O-5: M.5 lint-baseline rule is sound.** The allowed diff is header attributes plus `ObsoleteSdkInt` messages, and the baseline has exactly 10 `ObsoleteSdkInt` entries (grep). Everything else goes through R5. One note: the header `version=` will change because of AGP 9's lint even without Branch M. If `updateLintBaseline` is ever run on the main path, the same diff discipline should apply.
- **O-6: The Branch M dependency facts check out.**
  - `hilt-work` 1.2.0, 1.3.0 and 1.4.0 all declare `minCompileSdk=34`, which compileSdk 35 satisfies, so `checkAarMetadata` is safe.
  - 1.3.0/1.4.0 require AGP ≥ 8.1.1.
  - The Dagger 2.60 release notes add no AGP or Gradle requirement beyond minSdk 23. They also show 2.60 "Updated Kotlin version to 2.3.21" (fixes #5177/#5180/#5190), which supports T2's choice of 2.60.1.

### Rollback (item 3 of the iteration-2 brief): sound

- **Path list:** every path in `U060_TRACKED` is tracked (`git ls-files --error-unmatch` on all 15), so `git restore --source=HEAD --staged --worktree -- $U060_TRACKED` will not abort on an unmatched pathspec. `U060_NEW` covers the two new files.
- **Order:** daemons are stopped before the wrapper is restored, which is correct because the 8.14.1 wrapper cannot run on JDK 25.
- **Excluded paths:** `sdlc/**` and `.claude/**` are never passed to restore/checkout, `code-location.md` is reversed only by hand edit, and `CLAUDE.md` goes through the Engine Librarian.
- **Post-check:** the step-4 check `git status --short -- . ':!sdlc' ':!.claude'` prints nothing on today's tree, so it is a usable assertion.
- **Step 6.1:** the restore is a single path, `--worktree` only.

### Branch M executability (item 2 of the iteration-2 brief)

Apart from R2-B1, Branch M can be executed by a Developer without further decisions:
- Triggers T1 and T2 are precisely defined, and B1's pass/fail definition separates R11 from B1.
- The edits are exact line-level changes.
- The pre-specified 1.3.0 → 1.4.0 second step is justified.
- The test-diff rule in Step 7.5 is mechanically checkable, and the docs, CHANGES and release statement are specified.
- Stop conditions are listed.
- The Developer does not edit ACs; the amendment is routed to the Requirements Analyst before code review and QA.

Dropping Fallback B2 is correct (see N-1 disposition).

## Artifacts Consulted

| Artifact | Path | Purpose |
|---|---|---|
| Plan (revision 2) | sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md | Full re-read (862 lines); delta vs `2334fc6d` |
| U-060 story / BUG-017 | sdlc/artifacts/build/sprints/sprint-017/U-060-jdk-25-build-toolchain-upgrade.md; sdlc/artifacts/stories/BUG-017-build-fails-on-jdk-25.md | ACs, scope statement (read in iteration 1; unchanged) |
| Build files | build.gradle, app/build.gradle, gradle.properties, gradle/wrapper/gradle-wrapper.properties, settings.gradle, metadata/build.gradle, k9mail-vendored/build.gradle, .github/workflows/ci.yml | Read in iteration 1; unchanged on disk (`git status` clean outside sdlc) |
| Test config | app/src/test/resources/robolectric.properties; `@Config(sdk…)` grep over app/src/test | T3 analysis |
| Lint baseline | app/lint-baseline.xml | `ObsoleteSdkInt` count (10) |
| Git state | `git status`, `git diff --stat b09bda5d HEAD -- . ':!sdlc'`, `git ls-files --error-unmatch` (15 paths) | Step 0.1 guard, rollback list |
| External | services.gradle.org gradle-9.5.1 bin/wrapper sha256; docs.gradle.org/{9.1.0..9.5.1}/userguide/compatibility.html; docs.gradle.org/9.5.1/userguide/upgrading_version_9.html; hilt-work-{1.2.0,1.3.0,1.4.0}.aar aar-metadata; hilt-compiler-1.3.0.pom; github.com/google/dagger release dagger-2.60; GitHub issue search (google/ksp, google/dagger: "Gradle 9.5") | Spot-checks |
| Template | plugin cache templates/build-artifacts/gate-report-template.md | Report format |

## Phase Completion Report
---
story_id: "U-060"
phase: "plan-gate/plan-review"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-017/U-060/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
