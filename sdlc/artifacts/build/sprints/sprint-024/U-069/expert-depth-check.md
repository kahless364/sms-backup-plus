---
artifact_type: gate-result
gate: expert-depth-check
gate_id: "expert-depth-check"
checkpoint: plan-gate
pipeline: story-build
story_id: "U-069"
artifact_under_review: sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md
artifact_reviewed: sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md
producing_agent: amp/agents/solution-architect
verdict: PASS
severity: warning
agent: "Solution Architect"
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
iteration: 1
timestamp: "2026-11-21T12:24:14Z"
produced_at: 2026-11-21T12:24:14Z
recommendations_count: 2
criterion_results:
  - criterion: "Pattern grounding"
    verdict: satisfied
    deficit_present: false
  - criterion: "Anti-pattern recognition"
    verdict: satisfied
    deficit_present: false
  - criterion: "Trade-off articulation"
    verdict: satisfied
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
    verdict: satisfied_with_recommendation
    deficit_present: false
---

# Gate Report: expert-depth-check — U-069

## Gate: expert-depth-check @ plan-gate

## Verdict: PASS (severity warning, observe-only), 2 recommendations

## Summary

The plan is iteration 1 (plan.md:9), planned at `4300ffa9` (plan.md:10). I am a separate reviewer instance, and I
judged it as a senior Android engineer who knows WorkManager and Hilt.

Its strongest feature is that it describes the Hilt and WorkManager mechanism from the library code itself, not from
memory:
- **How the factory finds the worker.** `HiltWorkerFactory.createWorker` does a map lookup, then `Provider.get()`,
  then `create`. The plan takes this, and the annotation retentions, from `javap` on hilt-work 1.2.0 and dagger 2.59.2
  (L8, plan.md:73).
- **What KSP generates.** It reads hilt-compiler 1.2.0's `WorkerStep` and `WorkerElement`, and the existing
  `RestoreWorker` output, including the `@StringKey` map binding and the `newMapBuilder(n)` line in the component
  (L6, L7, plan.md:71-72).
- **The release keep rules.** It reads both consumer `proguard.txt` files (L9, plan.md:74).
- **`REPLACE` versus `KEEP`.** It reads `EnqueueRunnable` in both 2.9.1 and 2.12.0 (L12, plan.md:77).

The plan also states the limits of its own evidence:
- what the custom-factory harness proves and does not prove about the production factory (2.6, plan.md:249-258);
- which check detects which wiring failure, including that IC-4 detects neither annotation removal
  (2.5, plan.md:232-247).

Two recommendations remain, and neither blocks:
- **Criterion 4.** One literature anchor is wrong. Assertion Roulette and Fragile Test are Meszaros ch. 16, not
  ch. 15. Two principles the brief expects are named without a source: Ports and Adapters, and the debounce.
- **Criterion 8.** The edge-case list leaves out three cases a WorkManager reviewer would ask about:
  - a trigger while the incoming backup is `RUNNING`, where `REPLACE` cancels a running backup;
  - process death inside `doWork()`;
  - what the periodic, content-URI-triggered `CONTENT_TRIGGER` does between periods.

Each of these is behaviour the story leaves unchanged, so none is a defect in the fix.

What this review checked, read-only:
- **The plan.** Read in full, in parts: sections 0-3, 4.0, I0-I7, 7, 8, 9, 10, 11 and 12. The 4.8 mutation table and
  the section 6 decision rows were read by heading and the rows the criteria depend on.
- **Source at `4300ffa9`, by `git show`.** `BackupWorker.kt:100-118` (the `@HiltWorker` / `@AssistedInject` form the
  plan copies), `App.java:128-168` (the `HiltWorkerFactory` field, the existing `App.getScheduler`, the configuration),
  `WorkManagerScheduler.kt:128-160` (`scheduleIncoming()`) and `:195-280` (the `CONTENT_TRIGGER` registration, a
  `PeriodicWorkRequest` with `addContentUriTrigger`, enqueued with `ExistingPeriodicWorkPolicy.UPDATE`). Every line
  the plan cites from these ranges matches.

## Findings

### Criterion 1 — Pattern grounding

**Verdict:** satisfied

**Evidence present:** Section 2.0 (plan.md:103-134) names seven principles. Each is applied to a specific site in this
story:
- **DRY and Duplicated Code.** Hunt and Thomas ch. 2, "The Evils of Duplication"; Fowler, *Refactoring* 2nd ed.,
  ch. 3. They are applied to the defect itself: "two builders of the same `INCOMING` request that drifted apart". The
  AC-3 equality test is named as the fitness function that keeps the second builder deleted (plan.md:107-110). This
  is the single-source-of-truth framing the brief asks for.
- **Dependency Inversion and Constructor Injection.** Martin ch. 11; Seemann and van Deursen ch. 4. The worker
  depends on the `BackupScheduler` port, not on `WorkManagerScheduler` (plan.md:111-116).
- **Published versus public interface.** Fowler, IEEE Software 2002. It explains why the fully qualified class name
  is frozen: it is persisted in every device's `WorkSpec` and is the `HiltWorkerFactory` map key (plan.md:122-124).
  The brief asks for this, and it is the right principle.
- **Logs as an operational interface.** Nygard ch. 10, "Transparency". It explains why the trigger line is kept byte
  for byte (plan.md:125-127).
- **Others.** A sociable unit test (Fowler; Freeman and Pryce ch. 8), mutation analysis (DeMillo, Lipton and Sayward,
  1978) and the coverage ratchet (Ford, Parsons and Kua ch. 2).

The debounce is framed as the contract's two-stage trigger, INV-4: stage 1 is the content-URI change and stage 2 the
delayed enqueue (plan.md:189-190 in the kept comment, and AC-7). Its preservation is tested by `ac7_debounce_…`.

**Evidence missing:** (none at criterion level). The hexagonal vocabulary ("port") is used without naming Ports and
Adapters. That is a citation-depth point and is taken up under Criterion 4.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied

**Evidence present:** The plan names the anti-patterns in the current code and in the rejected options:
- **Duplicated Code.** This is the defect itself (plan.md:107-110).
- **Service Locator** (Seemann and van Deursen ch. 5). This is rejected option A2, `App.getScheduler(…)`. The static
  accessor exists at `4300ffa9` (`App.java:143-146`), so the temptation is real, not hypothetical (plan.md:114-116).
- **Test Logic in Production.** Named as the cost of the seam that A2 would then need (plan.md:116).
- **Assertion Roulette.** Answered by labelling every assertion message (plan.md:130-131).
- **Fragile Test.** Named for the internal `WorkSpec` accessor and contained in one helper, verified in both
  WorkManager versions (plan.md:205).
- **Per-call `Preferences(applicationContext)` construction.** The plan ties it to U-022 / ARCH-002 as the reason not
  to keep it (plan.md:202).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied

**Evidence present:** Section 2.3 (plan.md:197-208) gives, for all eight decisions, what is gained, what gives way,
what was not chosen and why. The brief names four alternatives, and the A1 row rejects each one:
- **A2, a static accessor:** a Service Locator.
- **A3, a Hilt `@EntryPoint`:** "a new pattern with the same test problem".
- **A4, a shared builder function:** it "leaves two gate sites".
- **A5, `CoroutineWorker`:** "adds nothing". Section 2.1 (plan.md:140-142) gives the reason: androidx.hilt accepts
  any `ListenableWorker` (L6, from `WorkerStep`'s `isAssignableFrom`), `scheduleIncoming()` is synchronous, and
  `Worker.doWork()` already runs on WorkManager's executor.

The plan's smaller choices also carry their trade-offs:
- `Result.success()` on a declined enqueue: no retry storm, and the periodic trigger is never marked failed. The cost
  is that the result cannot tell declined from enqueued; the log tells it.
- The second `Preferences` read for the log value, with its staleness risk carried to R14.
- Writing the red run before the fix, with a two-argument factory line rather than a reflective factory.
- The two-line Rule 15 change, raised as P-1 with a default and a fallback row.

**Evidence missing:** (none at criterion level). A note for the plan's owner, which needs no change: the A2 reason "a
hidden dependency that is null under Robolectric" (plan.md:115) is not sourced. Robolectric runs the manifest
application's `onCreate`, and whether Hilt's injection then populates `App.scheduler` in this test set-up was not
shown. The Service Locator reason carries the rejection on its own, so the conclusion holds either way.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 4 — Citation depth

**Verdict:** satisfied_with_recommendation

**Evidence present:**
- **Literature.** Every literature citation carries a chapter, an article and year, or a bliki entry
  (plan.md:107-134). The plan is open that the chapters are "cited as commonly given; I did not re-read them in this
  session" (plan.md:105).
- **Toolchain and library claims.** These are anchored more strongly than any book could anchor them: to the artifact
  and member read with `javap` or the sources jar, and for 2.12.0 to file and line (L6 to L14, plan.md:71-79; L11
  gives `WorkInfo.kt:35-39`, `EnqueueRunnable.kt:224-249` and others).
- **Project claims.** Anchored to line ranges at a named commit throughout.

Well over 80% of citations are anchored, so the criterion is met.

**Evidence missing:**
1. **One anchor is wrong.** Meszaros, *xUnit Test Patterns*, puts Test Logic in Production in ch. 15, "Code Smells".
   But **Assertion Roulette** and **Fragile Test** are behaviour smells, in **ch. 16, "Behavior Smells"**. The plan
   cites ch. 15 for both (plan.md:131 and plan.md:205).
2. **Two principles are used without a source.**
   - **Ports and Adapters.** The plan's whole dependency argument is "the worker depends on the `BackupScheduler`
     port" (plan.md:113), but the style is never named. The canonical source is Cockburn, "Hexagonal Architecture
     (Ports and Adapters)", 2005.
   - **The debounce.** It appears only as the project's INV-4. It is a two-stage, trailing-edge debounce with
     coalescing by unique-work `REPLACE`, and it is never tied to a named pattern.
3. **No vendor documentation is cited for the WorkManager policy semantics** the tests rely on (L12, AC-8). Reading the
   library code is stronger evidence, but a reviewer verifying intent wants the documented contract next to it.

**Passage:** Section 2.0 (plan.md:111-116, :130-131); section 2.3, row "Backoff and input read from the internal
`WorkSpec`" (plan.md:205); L12 (plan.md:77).

**Recommendation / Remediation:**
- **Correct the Meszaros anchors.** In plan.md:131 write "the **Assertion Roulette** smell (Meszaros, ch. 16,
  'Behavior Smells')". In plan.md:205 write "(a **Fragile Test** risk, Meszaros ch. 16)". Test Logic in Production at
  plan.md:116 stays ch. 15.
- **Add to the Dependency Inversion bullet in 2.0:** "The port and its WorkManager adapter are the **Ports and
  Adapters** style (Cockburn, 'Hexagonal Architecture', 2005): `BackupScheduler` is the driven port,
  `WorkManagerScheduler` its adapter, and after this story both incoming triggers (the receiver and the content-URI
  worker) are primary actors that reach the rule only through the port."
- **Add one sentence on the debounce:** "INV-4 is a trailing-edge debounce. Stage 1 is the content-URI change; stage 2
  is a delayed one-time request whose unique name and `REPLACE` policy coalesce a burst of changes into one backup
  that runs `incomingTimeoutSecs` after the last of them."
- **Add the documented contract to L12:** Android Developers, "Managing work", section "Unique Work" (the
  `ExistingWorkPolicy` values `REPLACE`, `KEEP`, `APPEND`, `APPEND_OR_REPLACE`), and for the registration's `UPDATE`,
  `ExistingPeriodicWorkPolicy.UPDATE` in the WorkManager API reference.

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** Section 11 (plan.md:1586-1597) evaluates all eight ISO/IEC 25010 characteristics by name, with
sub-characteristics. Usability is marked not applicable, with the reason (no UI) and the user-visible effect.
Highlights:
- **Compatibility:** the persisted `CONTENT_TRIGGER` resolves through the unchanged name and key, and the 2.12.0
  accessors are verified.
- **Security:** the U-054 gate now covers the trigger path.
- **Reliability:** single flight, and success on decline.
- **Portability:** the R8 keep rules.

**Evidence missing:** (none at criterion level). The Reliability row says nothing about `REPLACE` cancelling a
*running* backup. That is taken up as an edge case under Criterion 8.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:** Deterministic signals are cited and interpreted:
- **Library classes and resources:** `javap` output from hilt-compiler, hilt-work, dagger, work-runtime,
  work-testing and shadows-framework, and the two `proguard.txt` files (L6 to L14).
- **The existing KSP and Hilt build output:** the `RestoreWorker` generated classes and the component's map builder
  (L7).
- **Coverage:** the `worker` package's measured JaCoCo baseline, 0/16 lines in 2 classes (U-061 I9, U-063), and the
  filter analysis. That analysis predicts that the generated `_Factory` and `_AssistedFactory_Impl` will be counted and
  will pull the ratio to about 0.35 (L17, N6, plan.md:82, :94). The floor is then set from measurement, not from that
  estimate (I4 step 3, E-G2).
- **Hashes and expected greps:** blob hashes, `--numstat` and the hunk header, all derived by simulation on scratch
  copies of the post-U-068 text (L19, L20).
- **Expected grep outputs** for AC-10, AC-12 and IC-2, by file and line (I0 step 5, I6 step 4).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:** Increments I0 to I7 (plan.md:1062-1247) are chained by explicit data dependencies:
- I2's red run uses I1's two-argument test, so the red evidence exists before production code moves. The rationale is
  the 2.3 row "Red run before the fix".
- I3 step 4 records the pristine worker hashes `HW` and `SW` that the I5 mutation protocol restores to.
- I6 step 1 takes the floor values from I4 step 3. The section titles state the reason: "Measurement gate (floor
  unchanged)", then "The floor".
- I5 copies the scheduler's pristine file and hashes before its first scheduler mutant.
- IC-3's precondition is "no mutation entry lacks `restored`" (plan.md:1498-1499).

The start state depends on U-068 closing first, and that dependency is gated by I0 step 2's checks: the ancestor
check, the U-068 commits and status, and `oldtext_check.py start`.

**Evidence missing:** (none at criterion level). One note: the plan does not say in so many words that no step can run
in parallel. The constraints make that true: Gradle runs foreground only, there is one working tree, and the mutation
protocol edits it in place. A one-line statement would close the point, but the gate does not require it here.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied_with_recommendation

**Evidence present:** None of the nine doctrine anti-patterns degrades the plan.

The work a senior reviewer usually has to ask for is already there:
- **The production runtime path, step by step.** From `CONTENT_TRIGGER` through `HiltWorkerFactory.createWorker`, the
  map lookup and the assisted factory to `scheduleIncoming()` (plan.md:162-167).
- **The fallback when the map has no entry.** WorkManager's reflective default, which finds no
  `(Context, WorkerParameters)` constructor and logs "Could not instantiate …". The text was taken from the
  `WorkerFactory` constant pool (L22), and IC-3 searches for it (plan.md:224-226).
- **What the custom-factory harness cannot prove.** The plan states that `TestListenableWorkerBuilder` with a test
  factory proves the rules on the trigger path but not that the production factory builds the worker. It closes the
  gap in three layers:
  - `ic1_…` reflects on the generated `_AssistedFactory` and `_HiltModule.bind` and its `@StringKey` (RUNTIME
    retention, L8);
  - EV-G greps both components for the map entry;
  - IC-3 runs on a device.
  It also names what remains unproven (2.5, 2.6).
- **Mutants with stated outcomes.** Each mutant names the tests that must fail and those that must pass. M-T6 (`KEEP`)
  is detected only because AC-8 (b) and (c) were made stricter, and the plan says so (P-3).
- **Risks with likelihood and impact.** Each risk carries both, with a cited basis, and a concrete mitigation
  (section 8.3, R1 to R14).

**Evidence missing:** Section 2.4 (plan.md:210-230) covers most of the edge cases the brief lists:
- encryption degraded after registration (item 1);
- auto backup switched off between registration and firing (item 2);
- a delay of zero or negative (item 3);
- two triggers inside the delay, or a trigger and the receiver (item 5);
- the first trigger after an upgrade with the trigger work already persisted (item 7);
- `scheduleIncoming()` throwing (item 9).

It leaves out three cases a WorkManager reviewer would raise. All three are behaviour this story leaves unchanged, but
the plan should say so:
1. **The trigger fires while the incoming backup is `RUNNING`.** `REPLACE` applies to unfinished work, which includes
   running work: L12's own reading of `EnqueueRunnable` shows `CancelWorkRunnable.forName` on the existing chain. So an
   SMS that arrives during an incoming backup cancels that backup, which is stopped, and starts a new delayed one.
   This is unchanged from the old worker, which also used `REPLACE`, and from the receiver path. But AC-8 tests only
   the `ENQUEUED` case. The case also meets BUG-024 directly: a cancelled backup is logged and reported as a failure.
   Edge case 5 says only "one `INCOMING` survives and the newest request's delay applies".
2. **Process death inside `doWork()`, before `enqueueUniqueWork` has committed.** Who re-runs the trigger, and when?
   The periodic work is rescheduled when WorkManager next initialises, and the rescheduled job has content-URI
   triggers again, so it may wait for the next content change. The SMS that fired the lost run may therefore wait for
   the next change or the regular backup. The cost of the window is unchanged, but it is not stated.
3. **What the periodic `CONTENT_TRIGGER` does between runs.** The registration is a `PeriodicWorkRequest` with
   `addContentUriTrigger` and the 15-minute minimum interval, enqueued with `ExistingPeriodicWorkPolicy.UPDATE`
   (`WorkManagerScheduler.kt:232-256` at `4300ffa9`). After `Result.success()` the trigger is re-armed for its next
   period. The plan never says whether that period also limits how often stage 1 can fire, which makes stage 1 a
   rate limit as well as a trigger. It also never says what happens to content changes that land while the trigger
   worker is running. Section 2.1's "Runtime path" says only "a content change runs `CONTENT_TRIGGER`". The
   `REPLACE` / `KEEP` analysis is sourced, but the periodic content-trigger behaviour the debounce rests on is not.

**Passage:** Section 2.4 (plan.md:210-230), item 5 in particular; section 2.1 "Runtime path after the fix"
(plan.md:162-167); section 11, Reliability row (plan.md:1594).

**Recommendation / Remediation:** Add three items to 2.4. Each is a statement plus its source, and none needs a new
test.

> 11. **A trigger while the incoming backup is running:** `REPLACE` cancels unfinished work, running included (L12:
> `EnqueueRunnable` → `CancelWorkRunnable.forName`), so the running `BackupWorker` is stopped and a new delayed
> request replaces it. This is unchanged by this story: the old worker and the receiver both used `REPLACE`. The
> stopped run is reported as BUG-024 describes. AC-8 covers the `ENQUEUED` case only; the `RUNNING` case is BUG-024's,
> and it is recorded for it.
>
> 12. **Process death inside `doWork()`:** if the process dies before `enqueueUniqueWork` commits, no `INCOMING` work
> exists. The `CONTENT_TRIGGER` run is rescheduled at WorkManager's next initialisation and may wait for the next
> content change, so the message is backed up by the next trigger or by the regular backup. This is unchanged: the
> old worker had the same window.
>
> 13. **The periodic content trigger:** `CONTENT_TRIGGER` is a `PeriodicWorkRequest` with content-URI triggers at the
> 15-minute minimum interval, registered with `UPDATE`. Read `SystemJobInfoConverter` in work-runtime 2.9.1 (local
> cache, `javap -c`) for how a periodic spec with content-URI triggers is turned into a `JobInfo` (minimum latency
> versus content-trigger delays), and state whether stage 1 can fire more than once per period. If it cannot, the
> effective debounce is the longer of the period and `incomingTimeoutSecs`. Unchanged by this story; recorded for
> DES-005.

Add to the Reliability row of section 11: "`REPLACE` also stops a running incoming backup (2.4 item 11; BUG-024)."

## Edge-case review (the brief's list)

| Edge case | Plan coverage |
|-----------|---------------|
| Trigger while a previous incoming backup is pending | 2.4 item 5; AC-8 (a) to (c) with the id check (P-3) |
| Trigger while a previous incoming backup is running | **Not stated**: Criterion 8, item 11 |
| Auto backup switched off between registration and firing | 2.4 item 2; `ac5_…` changes the stub after `registerContentTrigger()`; PO-1 stays open |
| App upgraded with trigger work already enqueued | 2.0 (published interface), 2.1 runtime path, 2.4 item 7, `ic1_…`, IC-3 (`install -r`, data kept) |
| Process death between the trigger and the enqueue | **Not stated**: Criterion 8, item 12 |
| Encryption-degraded state changing | 2.4 item 1; `ac4_…`, second AC-9 test, M-T2 |
| Delay of zero (and negative) | 2.4 item 3; two `ac6_…` tests; M-T4, with N9 for `setInitialDelay(-1)` |
| `Worker` versus `CoroutineWorker` | 2.1 and 2.3 (A5) |
| `REPLACE` versus `KEEP` versus `UPDATE` | `REPLACE` / `KEEP` sourced (L12) and mutated (M-T6); `UPDATE` noted only as the registration's divergence from the CNTR-004 example (3.4). The periodic semantics are Criterion 8, item 13 |
| Constraints, initial delay and backoff on a `OneTimeWorkRequest` | AC-1 to AC-3 and AC-7 read them from `WorkInfo` and the `WorkSpec`; `DEFAULT_BACKOFF_DELAY_MILLIS = 30000` (L10); the harness leaves delayed, constrained work unrun (L13) |
| R8 | L9 (two keep rules), 2.5, IC-4 (mapping, dex, key string), R2 |
| What a custom-factory test does not prove | 2.5, 2.6, `ic1_…`, EV-G, IC-3 |

## Overall Verdict

**Verdict:** PASS

**Rationale:** The plan explains the Hilt-worker mechanism and the WorkManager unique-work policy from the library code
it will run against, and checks its claims against two library versions. Its test design is honest about what the
custom-factory harness cannot prove, and it closes that gap with three detectors in layers. Every decision carries a
four-part trade-off, and every risk carries likelihood and impact with a basis. Six criteria are satisfied and two are
satisfied with a recommendation, which is within the gate's limit of two:
- **Criterion 4:** correct the Meszaros chapter for Assertion Roulette and Fragile Test (ch. 16); name Ports and
  Adapters (Cockburn 2005) and the trailing-edge debounce; add the documented unique-work contract next to L12.
- **Criterion 8:** add three unchanged-behaviour edge cases: a trigger during a running backup, which ties to
  BUG-024; process death inside `doWork()`; and the periodic content-trigger semantics the debounce rests on.

**Iteration:** 1

**Rework routing (FAIL only):** Not applicable (PASS).

## Phase Completion Report
---
story_id: "U-069"
phase: "plan-gate/expert-depth-check"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-069/expert-depth-check.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
