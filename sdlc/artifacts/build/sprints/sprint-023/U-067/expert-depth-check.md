---
artifact_type: gate-result
gate: expert-depth-check
gate_id: "expert-depth-check"
checkpoint: plan-gate
pipeline: story-build
story_id: "U-067"
artifact_under_review: sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md
artifact_reviewed: sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md
producing_agent: amp/agents/solution-architect
verdict: FAIL
severity: warning
agent: "Solution Architect"
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
iteration: 1
timestamp: "2026-11-21T10:07:03Z"
produced_at: 2026-11-21T10:07:03Z
recommendations_count: 0
criterion_results:
  - criterion: "Pattern grounding"
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "Anti-pattern recognition"
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "Trade-off articulation"
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "Citation depth"
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "NFR and quality-attribute coverage"
    verdict: satisfied
    deficit_present: false
  - criterion: "Tooling evidence"
    verdict: satisfied
    deficit_present: false
  - criterion: "Sequencing rigor"
    verdict: satisfied
    deficit_present: false
  - criterion: "Anti-AI-slop check"
    verdict: satisfied
    deficit_present: false
---

# Gate Report: expert-depth-check — U-067

## Gate: expert-depth-check @ plan-gate

## Verdict: FAIL (severity warning, observe-only)

## Summary

On the engineering, this is a strong plan. Its main claim is that the inherited `CoroutineWorker.coroutineContext`
member, typed `CoroutineDispatcher`, shadows the imported `kotlin.coroutines.coroutineContext`, so
`dispatcher[Job]` is null and `ensureActive()` returns at once. The plan shows this from the compiled
work-runtime 2.9.1 and kotlinx-coroutines 1.7.3 classes, and from the compiler's own deprecation warning
(plan.md:54). It does not just assert it. The test harness is deterministic: no time, a single-thread event loop,
and a child job the test holds. The plan says why `LAZY` is used and why `UNDISPATCHED` is used only for AC-4
(plan.md:119-132). It also shows why the tests can tell the fix apart under the harness, namely the completed
future fast path of `setProgress` and `setForeground` (plan.md:55).

No criterion is `deficient`. The gate still FAILs on the aggregate rule ("more than two
`satisfied_with_recommendation`"). Criteria 1 to 4 each lack canonical grounding or a stated alternative at a
point where a decision rests on one:
- structured concurrency and cooperative cancellation are never cited to kotlinx.coroutines;
- WorkManager's stop path is attributed to an Assumption Ledger row that does not establish it;
- mutation testing and the test-double patterns are used but not named;
- `runTest`, which is on the test classpath and used elsewhere in the repository, is not considered against
  `runBlocking`.

Each fix is an addition to the text. None changes an edit, a test or a mutation row.

This review checked the following against the tree (read-only):
- **Line references.** `BackupWorker.kt:54`, `:57`, `:72`, `:168`, `:255`, `:265-281`, `:286`, `:305`, `:318`,
  `:324`, `:329`, `:331`, `:361`, `:375-378` and `:462`, and `RestoreWorker.kt:63`, `:66`, `:84`, `:148`,
  `:272`, `:311`, `:348-350`, `:399` and `:433-435`, all match the plan's citations.
- **Base commit.** HEAD is now `0d5755c4`, a descendant of the plan's base `4095db28`. Between the two,
  `git diff --stat 4095db28 HEAD -- app k9mail-vendored` prints nothing, so row D1's "descendant" clause applies.
- **Suspension points.** Neither worker file contains `withContext`, `NonCancellable`, `yield`, `delay` or
  `suspendCancellableCoroutine`.

Paths in this report are repository-relative, as the delegation requires.

## Findings

### Criterion 1 — Pattern grounding

**Verdict:** satisfied_with_recommendation

**Evidence present:** The plan names one canonical technique with a chapter. Feathers, *Working Effectively with
Legacy Code*, ch. 4, "The Seam Model", is the stated basis for the Route S seam (plan.md:92-93). It ties the seam
to the repository's U-053 precedent, `@VisibleForTesting(otherwise = PRIVATE)` (plan.md:89-91). That meets the
rubric's PASS floor of "at least one canonical pattern named with catalog attribution".

**Evidence missing:** Four decisions rest on canonical principles that the plan uses but does not name:
- **Structured concurrency (parent-child cancellation).** The harness's correctness depends on it: "A child
  cancelled with a `CancellationException` does not cancel its parent, and `join()` returns normally"
  (plan.md:119-120). This is the structured-concurrency rule in kotlinx.coroutines. The term never appears in the
  plan's own words. Its only occurrence is inside the quoted contract text at plan.md:989.
- **Cooperative cancellation.** The whole bug is a cooperative-cancellation check that never observes the job.
  The plan grounds this only in CNTR-MODERNIZATION-004 rule 7 (plan.md:84, :989-993), not in the library's
  definition of cooperative cancellation.
- **Mutation testing.** Six mutants are planned in two classes: (a) a reverted binding and (b) a removed check.
  They are AC-6's evidence (plan.md:812-830, :979-983). The technique is never named or attributed. That is
  DeMillo, Lipton and Sayward, "Hints on Test Data Selection: Help for the Practicing Programmer", *IEEE
  Computer* 11(4), 1978, and the competent-programmer and coupling-effect hypotheses behind it.
- **Test doubles.** The counting transports and `CancellingInterceptor` (plan.md:358-369, :467-491, :508-517) are
  Test Spies, and the mocked `MessageConverter` (plan.md:375-385) is a configurable stub. Meszaros, *xUnit Test
  Patterns*, ch. 23, "Test Spy" and "Test Stub", describes both. The `object : MailTransport by stub` wrapper is
  a GoF Decorator built with Kotlin interface delegation.

**Passage:** Section 2.2 (plan.md:102-141). Section 3.2, the helpers of T1, T2b and T3b. Section I6 (plan.md:781-830).

**Recommendation / Remediation:** Add a "Principles applied" paragraph to section 2:
1. The harness relies on structured concurrency. Cite the kotlinx.coroutines guide, "Coroutine exceptions
   handling", section "Cancellation and exceptions": a child cancelled with `CancellationException` does not
   cancel its parent. It also relies on cooperative cancellation. Cite the guide "Cancellation and timeouts",
   section "Cancellation is cooperative": a computation stops only at a check (`ensureActive`, `isActive`,
   `yield`) or at a cancellable suspension.
2. The six I6 entries are mutation-testing mutants (DeMillo/Lipton/Sayward 1978). Class (a) is a "binding" mutant
   specific to this bug. Class (b) is a statement-deletion mutant. Say that a surviving (b) means the test cannot
   observe the check at all, which is the R1 failure.
3. Name the counting transports and the interceptor as Test Spies (Meszaros ch. 23), and the delegating wrapper
   as a Decorator (GoF) built with Kotlin `by` delegation.

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied_with_recommendation

**Evidence present:** The plan identifies the current-state defects concretely:
- **Shadowing of the top-level property by an inherited member.** This is the root cause, shown mechanically
  (plan.md:54), with the warning that the fully qualified form invites an "optimise imports" regression
  (plan.md:83).
- **Polling a flag in place of cooperative cancellation.** Rejected under rule 7 (plan.md:84).
- **Comments that overstate the checks.** "the five comments that overstate the checks today become true"
  (plan.md:183-184).
- **The vacuous-test risk.** R1, D5, and section 7 item 5 (plan.md:925, :979-983, :1012).
- **The broad catches that turn a `CancellationException` into `Result.failure`.** `BackupWorker.kt:168`, and
  `RestoreWorker.kt:348` and `:433`, where `CancellationException` extends `IllegalStateException`
  (plan.md:64, :970-978).

**Evidence missing:** Two of these are canonical anti-patterns, and the plan describes them as facts without
naming them.
- **The broad catches** at `BackupWorker.kt:168` (`catch (e: Exception)`) and `RestoreWorker.kt:348` and `:433`
  (`catch (e: IllegalStateException)`) swallow `CancellationException`. kotlinx.coroutines documents this as
  breaking cancellation. In static-analysis catalogs it is detekt's `TooGenericExceptionCaught` and
  `SwallowedException` (rule set `exceptions`). It is also why `runCatching` is unsafe in coroutines.
- **The shadowing** is a known Kotlin hazard: an implicit-receiver member wins over a top-level import. Here it
  went unseen because the warning was treated as a deprecation notice and not as a binding change.
- **A sixth overstating comment.** `RestoreWorker.kt:369`, "AC-7: checkpoint NOT cleared on CANCELLED
  (cooperative cancellation unwinds before clear)", overstates as well. It contains no `ensureActive`, so the
  plan's grep (L3) did not count it.

**Passage:** Section 7 item 4 (plan.md:970-978). Section 2.1 (plan.md:80-84). L3 (plan.md:52).

**Recommendation / Remediation:**
- In section 7 item 4, name the catches as "swallowed `CancellationException`". Cite the kotlinx.coroutines guide,
  "Cancellation and timeouts", on rethrowing `CancellationException`, and detekt `exceptions` /
  `SwallowedException`. Say that this story leaves them in place on purpose (SC-1), and that they do not affect
  the job state the tests assert.
- Add `RestoreWorker.kt:369` to the list of comments that this story makes true, or to the SC-1 follow-up.

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied_with_recommendation

**Evidence present:** Section 2.1's table (plan.md:80-84) gives "Gained", "Gives way" and "Decision" for the fix.
It names two alternatives, each with a reason it was not taken:
- the fully qualified name, which an import optimiser can shorten back to the shadowed form;
- polling `isStopped`, which the contract forbids and which is untestable.

The seam-annotation placement has a rejected alternative and a reason (plan.md:95-98). So do the harness
choices: `LAZY` against `UNDISPATCHED` (plan.md:121-123), and `UNDISPATCHED` for AC-4, because a `LAZY` job
cancelled before start "would make the test pass on the unfixed code for the wrong reason" (plan.md:128-130). So
does the choice not to assert `Result`, which keeps the tests valid after the SC-1 fix (plan.md:138-141,
:976-978).

**Evidence missing:**
- **Harness.** `runTest` (kotlinx-coroutines-test 1.7.3) is not considered. It is on the test classpath
  (`app/build.gradle:150`) and already used in `MainViewModelTest.kt:18` and `EventModuleSingleInstanceTest.kt:11`.
  The plan chooses `runBlocking` without stating what that costs or why `runTest` lost.
- **Fix.** Two alternatives are not in the table: an import alias (`import kotlin.coroutines.coroutineContext as
  runningContext`), which also escapes the shadowing, and `yield()`, which checks cancellation but adds a
  dispatch.
- **Seam.** The cost of Route S has no gain/cost line. `backupCursors` widens from `private` to `internal`, and
  it has two production callers (plan.md:53). The cost appears only as R9 (plan.md:1020).

**Passage:** Section 2.1 table (plan.md:80-84). Section 2.2 (plan.md:109-126).

**Recommendation / Remediation:** Add these rows to the 2.1 table:
- **Import alias.** "Gained: same behaviour, no new API. Gives way: an alias is as easy to remove in an import
  clean-up as a qualifier, and it hides the reason the alias exists. Not taken."
- **`yield()`.** "Gives way: a dispatch on every pass. Under `runBlocking` it would let other coroutines interleave
  and change the determinism of 2.2. Not taken."

Add a harness paragraph to 2.2: "`runTest` is available (`app/build.gradle:150`), but nothing here needs virtual
time. Its dispatch timeout and leaked-coroutine checks would add failure modes unrelated to the check under test.
The existing worker tests the new tests sit beside use `runBlocking`, so `runBlocking` keeps one style per class.
Not taken."

### Criterion 4 — Citation depth

**Verdict:** satisfied_with_recommendation

**Evidence present:** The only canonical-literature citation, Feathers ch. 4 (plan.md:92), is anchored. The
plan's other citations are anchored too:
- the contract (`CNTR-MODERNIZATION-004` `:279` and `:287`, plan.md:989-990);
- requirements (REQ-MODERNIZATION-015 AC-13, `:165-176`, plan.md:1002);
- prior evidence (`sprint-017/U-060/evidence/final-gates.log:108-110`, plan.md:54).

Library behaviour is anchored in `javap` output of named classes and methods (plan.md:54-56). Of the citations
present, 100% are anchored.

**Evidence missing:** Three load-bearing claims carry no source, or cite one that does not support them:
- **plan.md:106-107** says "Cancelling that coroutine's job is what WorkManager's stop does to the worker's job in
  production (L5: `startWork` runs `doWork` in dispatcher + `job`)". L5 (plan.md:54) establishes the `job`
  field and the getter's type. It does not read `startWork` or the constructor's future listener. This
  equivalence is the premise that makes the unit test stand in for a production stop. The evidence the plan
  points to does not establish it.
- **plan.md:119-120** states the parent-child cancellation rule without a source.
- **plan.md:1069** says the API is "present unchanged in 1.7.3 and 1.11.0". The ledger verified 1.7.3 only (L7,
  plan.md:56).

**Passage:** Section 2.2 (plan.md:104-120). Section 11, Portability row (plan.md:1069).

**Recommendation / Remediation:**
- Add a ledger row L5b. Run `javap -c` on `androidx.work.CoroutineWorker.startWork` and on the constructor's
  `future.addListener` lambda, which cancels `job` when the future is cancelled. Record that `doWork` runs in
  `CoroutineScope(coroutineContext + job)`. Cite the Android developer guide "Threading in CoroutineWorker",
  which says a `CoroutineWorker` handles stops by cancelling its coroutine. Point plan.md:107 at L5b.
- Cite the parent-child rule as in Criterion 1, item 1.
- In the Portability row, either mark 1.11.0 "not verified (expected from the public API's stability)" or drop
  it.

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** Section 11 (plan.md:1058-1069) evaluates all eight ISO/IEC 25010 characteristics by name.
Each has a finding specific to this story:
- **Performance Efficiency:** the inline context read, and one IMAP APPEND fewer when a run is cancelled.
- **Compatibility:** the fix no longer depends on the deprecated member.
- **Reliability:** the `finally` cleanup, watermark gating and checkpoint retention.
- **Maintainability:** the mutation evidence can be audited at each upgrade.

Usability, Security and Portability are marked not applicable, each with a reason.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:**
- **Scope by `git grep`.** Five `coroutineContext` hits and every `ensureActive` hit, line by line (L3,
  plan.md:52). The grep also found the second caller of `backupCursors`, which corrects the story (L4,
  plan.md:53).
- **File identity.** Blob and working-tree hashes (L2).
- **Library behaviour.** `javap -p` and `javap -c` of the compiled libraries (L5 to L7).
- **Compiler baseline.** The three warnings at HEAD from U-060's log. I0 step 3 proves that the AC-7 compile can
  see them before it is used to show they are gone (plan.md:710-713).
- **Test counts.** Per-class `@Test` counts (L18).
- **Mutations.** Six, each with a predicted failing test and failure message (plan.md:812-819).
- **Coverage.** JaCoCo floors are enforced by the gate and not edited (plan.md:842-843). The mutations, not
  coverage, carry the evidence for a one-line change, and that choice is sound.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:** I0 to I10 state their order and dependencies:
- **I0:** "no edit before this step passes" (plan.md:683).
- **I1 then I2:** the seam comes before the tests, because T1 calls `backupCursors` and "I3 compiles the seam"
  (plan.md:719-720).
- **I3, the red run:** "fix absent, seam and tests present" (plan.md:735). It precedes I4 so that each
  cancellation test is shown to fail for the expected reason (D5).
- **I6:** "one at a time", with write-ahead and restore. "No gate, review or commit runs while an entry lacks
  its `restored` line" (plan.md:799-810).
- **I7:** gated on every I6 entry being restored (plan.md:834).
- **I10:** "last step, after every file is final" (plan.md:894).

Parallelism is excluded for stated reasons: the foreground-only and endpoint-security constraints, and mutations
of a shared, uncommitted file (plan.md:1033-1041). The I8 determinism check reuses I5's class filter, so no extra
run is needed (plan.md:844-846).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied

**Evidence present:** A senior Android/Kotlin reviewer would accept the analysis.
- **The mechanism is shown from bytecode.** `CoroutineContext.ensureActive()` compiles to `get(Job.Key)` and
  returns on null, and a dispatcher's key is `ContinuationInterceptor` (plan.md:54).
- **The test-validity premise is shown from bytecode and named as a risk.** Under work-testing 2.9.1 the futures
  are already complete, so `setProgress` and `setForeground` are not cancellation points (L6). The plan gives the
  exact way the tests would go vacuous under a later library and the mutation re-run that would catch it
  (plan.md:979-983).
- **Predicted outcomes.** Section 2.4 predicts, for each test, what it observes on the fixed and on the unfixed
  code, down to the first failing assertion (plan.md:156-164).
- **The cancel is checked as delivered.** `isCancelled` is asserted first, so a test cannot pass because the
  cancel was never delivered (plan.md:135-137).
- **The `Result` question is reasoned through.** A `CancellationException` caught as `IllegalStateException`
  becomes `Result.failure("illegal_state")`. The tests therefore assert effects, not the `Result` type, so the
  SC-1 fix will not need to edit them (plan.md:64, :138-141).

None of the nine doctrine anti-patterns occurs:
- no maximalist hedging;
- no generic recommendation;
- no ungrounded "best practice" claim;
- every out-of-scope item is tied to the story's own list or a ruled scope concern (plan.md:1022-1023);
- the AC-1 (c) narrowing is declared with its cause, not deferred quietly (plan.md:143-152).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

## Edge-case and risk review (requested by the delegation; does not change the criterion counts)

| Edge case | Plan coverage | Assessment |
|-----------|---------------|------------|
| Cancellation between items against inside a suspending call | 2.2 and 2.4: the pass in flight completes and the check at the top of the next pass stops the loop. L6 shows that under the harness no suspending call is a cancellation point. Section 7 item 4 says production can also stop at `setProgress` | Covered for the harness. **Gap:** the plan does not state that in production the in-pass `setProgress` (`BackupWorker.kt:361`; `RestoreWorker.kt:321`, `:406`) is the first cancellable point after a cancel. So production stops one step *earlier* than the tests show: after the watermark or checkpoint write, before the next pass. AC-1 (b) and AC-2 (c) are unaffected, because the write precedes `:361` and `:321`. One sentence in 2.4 would make the harness-to-production mapping exact |
| `NonCancellable` sections | Not mentioned | Neither worker file contains `NonCancellable` or `withContext` (checked in this review). The plan should say so, because a `withContext` on the loop path would be a cancellation point under the harness too, and would make the red run pass (D5) |
| `finally` blocks | 2.3 and Reliability: `finally` runs on any throwable; `closeFolders` is asserted once | **Gap:** the plan does not say that the `finally` bodies (`BackupWorker.kt:280`, `:377`) contain no suspending call. In a cancelled coroutine a suspending call in `finally` throws at once unless it is wrapped in `withContext(NonCancellable)`. Both bodies are non-suspending Java calls (checked in this review), so the claim holds. It should be stated with its evidence |
| What a `CancellationException` does to `Result` | L15, 2.2, section 7 item 4 | Covered |
| Re-entrancy (`handleAuthError` retry, `:462`) | L4 counts the caller. IC-1 is corrected (section 7 item 2) | **Gap:** the plan does not state that a cancellation cannot trigger the retry. The retry is entered only from `catch (e: XOAuth2FailedException)` (`BackupWorker.kt:265-267`), and `CancellationException` is not one. A cancelled first pass therefore never re-enters `backupCursors`. If a cancel lands during the retry's own loop, the same check stops it. This is one line under L15 |
| Test determinism | 2.2 ("No time"), I8 determinism diff | Covered. The cancel is synchronous, from inside the fixture, on `runBlocking`'s event loop |
| Risk quantification | Section 8 risk table (plan.md:1010-1020) | **Gap:** R1 to R9 have mitigations but no likelihood or impact. R1, vacuous tests after a library upgrade, has the highest impact: the regression would pass silently. Its likelihood is tied to the S2-I5/S4-W upgrades. A Low/Med/High likelihood and impact column with one-line grounds would meet the delegation's "risks quantified" bar |

## Overall Verdict

**Verdict:** FAIL

**Rationale:** The plan is technically excellent:
- the root cause is shown from bytecode and the compiler's own warning;
- the harness is deterministic, and each cancellation test has a control that uses the same harness;
- the red run is predicted down to the first failing assertion;
- the mutation design proves AC-6 at each site and anticipates how the tests could become vacuous under later
  libraries.

No criterion is deficient. The gate fails on the aggregate rule, four `satisfied_with_recommendation` verdicts
against a limit of two. All four concern canonical grounding of decisions the plan already makes correctly:
- **Criterion 1:** name structured concurrency, cooperative cancellation, mutation testing and the Test Spy and
  Decorator doubles.
- **Criterion 2:** name the broad catches as swallowed `CancellationException` (detekt `SwallowedException`), and
  add the sixth overstating comment, `RestoreWorker.kt:369`.
- **Criterion 3:** add `runTest`, an import alias and `yield()` as rejected alternatives, and give Route S a
  gain/cost line.
- **Criterion 4:** back the WorkManager-stop equivalence (plan.md:107) with a `javap` read of `startWork` and the
  future listener, or the Android guide. Today it points at L5, which does not show it. Also cite the
  parent-child rule, and qualify the unverified 1.11.0 claim.

The edge-case table adds four one-line statements: no `NonCancellable` or `withContext` on the loop paths;
non-suspending `finally` bodies; no retry on cancellation; production stops at the in-pass `setProgress`. It also
asks for a likelihood and impact column on the risk table. None of these changes an edit, a test, a mutation row
or a decision row. This gate has severity warning, so the verdict is recorded and does not block the story by
itself.

**Iteration:** 1

**Rework routing (FAIL only):** Routes to amp/agents/solution-architect at step `planning` for iteration 2. Cap is
3 reworks (4 total evaluations). On cap exhaustion, escalates via AskUserQuestion.

## Phase Completion Report
---
story_id: "U-067"
phase: "plan-gate/expert-depth-check"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-023/U-067/expert-depth-check.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
