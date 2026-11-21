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
iteration: 2
timestamp: "2026-11-21T13:17:42Z"
produced_at: 2026-11-21T13:17:42Z
recommendations_count: 1
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
    verdict: satisfied
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

## Verdict: PASS (severity warning, observe-only), 1 recommendation

## History

- **Iteration 1** (2026-11-21T12:24:14Z): PASS with 2 recommendations:
  - Criterion 4: the Meszaros chapter was wrong; Ports and Adapters and the debounce had no source; the unique-work
    contract was not cited.
  - Criterion 8: three unchanged-behaviour edge cases were missing (a running backup, process death, the periodic
    trigger).
- **Iteration 2** (this report): both recommendations taken up. One narrower recommendation remains on Criterion 8.
  It concerns how the periodic-trigger finding is stated, and it comes with an observation for the orchestrator.

## Summary

Iteration 2 makes every change iteration 1 asked for, and each one is accurate.

**Criterion 4 changes:**
- **Meszaros chapters.** Assertion Roulette is now ch. 16 (plan.md:177) and Fragile Test ch. 16 (plan.md:253). Test
  Logic in Production stays ch. 15 (plan.md:154).
- **Ports and Adapters.** It is named, with Cockburn 2005, and applied to this code (plan.md:155-158).
- **The debounce.** INV-4 is named as a trailing-edge debounce, with `REPLACE` doing the coalescing (plan.md:159-162).
- **The vendor contract.** It is cited beside L12 and marked "not fetched in this session" (plan.md:112).

**Criterion 8 changes:** Section 2.4 gains three items:
- item 11, a trigger during a running backup (plan.md:280-285);
- item 12, process death inside `doWork()` (plan.md:286-289);
- item 13, the periodic trigger (plan.md:290-296).

The Reliability row now names the first two (plan.md:1725). BUG-024 is handed over as a case for its own fix (section
7 item 12, plan.md:1550-1551).

**The other changes, judged:**
- **The coverage premise is corrected, and on good evidence.** L24 (plan.md:124) reads today's JaCoCo XML. The
  generated `BackupWorker_*` and `RestoreWorker_*` classes there have no counter. `_Factory` and
  `_AssistedFactory_Impl` carry `@DaggerGenerated` with CLASS retention, and they do have `LineNumberTable`
  attributes, so JaCoCo filters their lines rather than finding none. JaCoCo 0.8.14's `AnnotationGeneratedFilter`
  holds the constant `Generated`. JaCoCo documents that it filters classes and methods annotated with an annotation
  whose simple name contains "Generated" and whose retention is CLASS or RUNTIME, which matches the evidence.
  - Iteration 1's "about 0.35" is withdrawn explicitly (N6, plan.md:132).
  - The 0.95-under-1.00 floor is a recorded ruling (Record 18 ruling 46, L26). Its trade-off is stated: one uncovered
    worker line could slip under the floor, but the tests and mutants still see it (plan.md:257).
  - The cost to future work is handed over: any later story that adds code to `worker` must cover its own new lines
    (plan.md:1525-1526).
- **M-T12 closes a real gap.** AC-7's 60 s equals the default (L15), so a delay hard-coded to 60 would have survived
  every test. `ac9_allowedState_…` now asserts 75 000 ms, and M-T12 must fail only that test, while `ac7` and both
  `ac3` pass (plan.md:238, :240, :1385). This is the right way to keep a test from passing by coincidence with a
  default value.

I checked against source at `4300ffa9`, read-only:
- **The receiver path.** `SmsBroadcastReceiver.java:56-62` calls `scheduleIncoming()`. The manifest
  (`AndroidManifest.xml:146-153`) declares the receiver `exported="false"` with `BROADCAST_SMS`. The story records that
  it is not delivered `SMS_RECEIVED` on Android 11 and newer (story `:68`, PO-3).
- **The `CONTENT_TRIGGER` registration.** `WorkManagerScheduler.kt:232-256`.

## Findings

### Criterion 1 — Pattern grounding

**Verdict:** satisfied

**Evidence present:** Section 2.0 (plan.md:141-182) keeps every iteration-1 principle and adds two:
- **Ports and Adapters** (Cockburn, 2005). `BackupScheduler` is the driven port and `WorkManagerScheduler` its
  adapter, and both incoming triggers are "primary actors that reach the incoming rule only through the port"
  (plan.md:155-158).
- **A trailing-edge debounce** (plan.md:159-162). The plan links it to the tests that pin it: AC-7 for the delay and
  AC-8 for the coalescing.

The ratchet bullet now states why the floor sits at 0.95 under 1.00 (plan.md:179-182).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied

**Evidence present:** Unchanged from iteration 1, and still accurate:
- Duplicated Code: the defect;
- Service Locator: rejected A2;
- Test Logic in Production;
- Assertion Roulette and Fragile Test, now correctly anchored;
- per-call `Preferences` construction (ARCH-002).

Iteration 2 also treats a passing-by-coincidence test as a defect: AC-7's 60 s equals the default, and M-T12 is added
for that reason (plan.md:238).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied

**Evidence present:** Section 2.3 (plan.md:245-257) gives all four elements for every row. The rows changed in
iteration 2:
- **The A1 row** now says what gives way: four generated classes join `worker` but report no lines.
- **A new floor row**, "Floor 0.95 under a measured 1.00":
  - gained: robustness to a JaCoCo line-counting change in U-071;
  - gives way: one uncovered line could pass the floor;
  - not chosen: a floor of 1.00;
  - why: ruled.
- **The P-1 and P-2 rows** now cite their ruling (Record 18 ruling 45).

**Evidence missing:** (none at criterion level). Iteration 1's note still applies: "null under Robolectric"
(plan.md:153) is unsourced. The Service Locator reason carries the rejection without it.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 4 — Citation depth

**Verdict:** satisfied

**Evidence present:** Every iteration-1 correction is made:
- Meszaros ch. 16, "Behavior Smells", for Assertion Roulette (plan.md:177) and Fragile Test (plan.md:253). Ch. 15,
  "Code Smells", stays for Test Logic in Production (plan.md:154).
- Cockburn, "Hexagonal Architecture (Ports and Adapters)", 2005 (plan.md:155).
- The documented unique-work contract (Android Developers, "Managing work", "Unique Work"; `ExistingPeriodicWorkPolicy`
  in the API reference) next to the code reading in L12. It is marked honestly as cited by name and not fetched
  (plan.md:112).

The new claims are anchored to the artifact and member read:
- L24: the JaCoCo XML, `javap -v` on the generated class files, and `javap -c -constants` on `org.jacoco.core-0.8.14`;
- L25: `javap -c -p` on `SystemJobInfoConverter` in `work-runtime-2.9.1`.

The one platform claim that was not read, how JobScheduler treats a content change that arrives before the minimum
latency has elapsed, is marked "plausible, not verified" (plan.md:293-294).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** Section 11 (plan.md:1717-1728) still evaluates all eight ISO/IEC 25010 characteristics.
Reliability now covers the running-backup stop and the process-death window (plan.md:1725).

**Evidence missing:** (none at criterion level). The periodic-trigger latency of 2.4 item 13 is a Performance
Efficiency (time behaviour) fact visible to the user. It is not in that row. The Criterion 8 recommendation carries
it.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:** Iteration 2 adds three measured signals and corrects a conclusion with them:
- **The JaCoCo XML read class by class** (L24, plan.md:124). It replaces iteration 1's inferred "about 0.35" with an
  expected 1.0000. The `LineNumberTable` check separates "filtered" from "no lines", which is the distinction the
  conclusion rests on.
- **`javap` on `SystemJobInfoConverter`** (L25). It shows no `setPeriodic` call, a `setMinimumLatency` computed from
  `calculateNextRunTime()`, and `addTriggerContentUri` with the update and maximum delays.
- **Expected numbers re-derived.** The test blobs were re-derived after the 75 s assertion (L19).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:** The I0 to I7 chain is unchanged. M-T12 is placed last among the scheduler mutants, after the
scheduler's pristine copy is taken (plan.md:1262-1264). It shares its text-check entry with M-T5, as `M-T5/M-T12`
(plan.md:1006), so the protocol's "exactly once" precondition covers it. The floor still follows the measurement:
I6 step 1 uses I4 step 3, and D19 (a) and (b) handle a measurement other than 1.00.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied_with_recommendation

**Evidence present:** Items 11 and 12 (plan.md:280-289) are correct and go beyond what iteration 1 asked:
- **Item 11** ties the running-backup stop to U-067, which made the stop real, and to BUG-024. It says AC-8 covers
  only `ENQUEUED`.
- **Item 12** states the process-death window and who recovers it.

Item 13 (plan.md:290-296) rests on L25, read from the class that does the conversion, and is right in substance:
- **The mechanism is correct.** WorkManager does not hand JobScheduler a periodic job. Each period is a one-shot
  `JobInfo` with a minimum latency up to the next period and the content-URI triggers. JobScheduler runs a job only
  when all its constraints hold. So the trigger worker can start at most once per period, and only after a content
  change. "Stage 1 can fire at most once per 15-minute period" is therefore a correct reading.
- **Its consequence is correct.** "the effective delay after a burst is up to the rest of the period plus
  `incomingTimeoutSecs`". The plan hands this to DES-005 (section 7 item 7, plan.md:1533-1535).
- **The unverified part is labelled.** The plan states openly that the platform's handling of a change inside the
  window is not verified.

**Evidence missing:** Three points:
1. **The user effect is not spelled out, and the reason it matters is in another section.** Item 13 speaks of "a
   burst" and "stage 1". It does not tell the reader the practical effect, and it does not connect that effect to the
   fact that makes it matter. On Android 11 and newer the SMS receiver is not delivered (story `:68`, PO-3; R1 at
   plan.md:1594 says "the receiver is not delivered"). So the content trigger is the **only** automatic path for an
   incoming message on the owner's devices. The plan's own facts give this sequence with the default 60 s delay:
   - The first SMS after a quiet spell fires the trigger and is backed up about 60 s later.
   - A second SMS a few minutes later cannot start the trigger until that 15-minute period ends. It is backed up up
     to roughly 15 minutes plus 60 s after the first trigger.
   - Several messages in the window are coalesced into that one later backup.
   This is a real limit on how soon an incoming message is backed up. It applies on top of Doze and app-standby
   deferral, which the plan does not need to cover. It is a pre-existing product behaviour, unchanged by this story.
2. **The anchor of the period is asserted, not read.** L25 says the next job waits "at least until its next period
   (15 minutes after the last run)". The latency comes from `calculateNextRunTime()`, which uses the spec's
   `lastEnqueueTime` and period count. Which time a finished periodic run writes back is set by WorkManager's
   periodic reset in `WorkerWrapper`, and the plan did not read it. Two readings fit what was read:
   - If the run writes its finish time, the next trigger is at least 15 minutes after the last run.
   - If it keeps the period grid, a trigger after a long quiet gap can run with zero latency, and the limit is "at
     most once per period window", not "15 minutes after each run".
   The worst-case bound of roughly 15 minutes plus the delay holds either way. Only the wording "15 minutes after the
   last run" depends on which reading is right.
3. **One claim in item 13 is stated without a label.** "Changes that land while the trigger worker runs are seen by
   the next scheduled job" (plan.md:295-296) carries no source and no `plausible, not verified` label, unlike the
   sentence before it. Whether JobScheduler carries the changes seen during a run over to the next scheduled `JobInfo`
   is platform behaviour, and the plan did not read it.

**Passage:** Section 2.4 item 13 (plan.md:290-296); L25 (plan.md:125); section 7 item 7 (plan.md:1533-1535);
section 11, Performance Efficiency row.

**Recommendation / Remediation:** These are plan-text changes only. No step, test or count changes.
- **Append to 2.4 item 13:**
  > For the user: on Android 11 and newer the SMS receiver is not delivered (story `:68`, PO-3), so this trigger is the
  > only automatic path for an incoming message. The first message after a quiet spell is backed up about
  > `incomingTimeoutSecs` (60 s by default) after it arrives. A further message within that 15-minute period waits for
  > the period to end: it is backed up up to about 15 minutes plus `incomingTimeoutSecs` after the first trigger, and
  > several such messages are backed up together. This is a pre-existing product behaviour, not this story's defect.
  > It is recorded for the owner with PO-3, because restoring a working SMS-received path, or registering the trigger
  > differently, would change it.
- **Qualify L25's parenthesis.** Change it to "(the next period boundary from `calculateNextRunTime()`; whether a
  finished run re-anchors it to its finish time is set by `WorkerWrapper`'s periodic reset, not read: plausible, not
  verified)". Or read that method in 2.9.1 with `javap -c` and state the anchor.
- **Label the last sentence of item 13** "plausible, not verified" as well.
- **Add to section 11, Performance Efficiency:** "Time behaviour (pre-existing): an incoming message after the first
  in a 15-minute period waits up to the period's end plus `incomingTimeoutSecs` (2.4 item 13)."

## Observation for the orchestrator (pre-existing product behaviour, not this story's defect)

How soon an incoming SMS is backed up automatically is limited by the content trigger's 15-minute period. The evidence:
- **Registration.** `WorkManagerScheduler.kt:240-253` at `4300ffa9` registers `CONTENT_TRIGGER` as a
  `PeriodicWorkRequest` at `PERIODIC_MIN_INTERVAL_SECS` with `addContentUriTrigger(Consts.SMS_PROVIDER, true)`.
- **Conversion.** WorkManager 2.9.1's `SystemJobInfoConverter` turns each period into a one-shot `JobInfo` with
  `setMinimumLatency` up to the next period plus the content-URI triggers (plan L25, `javap`). So the trigger worker
  runs at most once per period, and only after a content change.
- **No second path.** The SMS-received receiver is not delivered on Android 11 and newer (story `:68`, PO-3).
  `scheduleIncoming()` also uses `REPLACE` with a 60 s default delay.

**The consequence:** the first message after a quiet spell is backed up about a minute after it arrives. Further
messages within that period wait until the period ends, up to about 16 minutes with the defaults, and are backed up
together. It is unchanged by U-069 and was the same before it.

It belongs with PO-3 (restore or remove the receiver) and DES-005's debounce description. Either of these would bound
the latency by `incomingTimeoutSecs` instead:
- a working SMS-received path;
- a one-time content-URI-triggered request that is re-registered after each run, instead of a periodic one.

Each is a product decision. Neither is in scope here.

## Edge-case review (iteration 1's list, re-checked)

| Edge case | Iteration 2 coverage |
|-----------|----------------------|
| Trigger while the incoming backup is pending | 2.4 item 5; AC-8 (a) to (c) with the id check |
| Trigger while the incoming backup is running | 2.4 item 11: `REPLACE` stops it; real since U-067; reported as a failure until BUG-024; handed to BUG-024 (section 7 item 12) |
| Auto backup off between registration and firing | 2.4 item 2; `ac5_…` |
| App upgraded with trigger work already persisted | 2.0, 2.1, 2.4 item 7, `ic1_…`, IC-3 |
| Process death inside `doWork()` | 2.4 item 12; section 11 Reliability |
| Encryption-degraded state changing | 2.4 item 1; `ac4_…`, second AC-9 test, M-T2 |
| Delay zero or negative | 2.4 item 3; two `ac6_…` tests; M-T4 |
| A delay hard-coded to the default | `ac9_allowedState_…` at 75 s; M-T12 (new) |
| The periodic content trigger | 2.4 item 13 from L25: correct in mechanism. The user effect and the anchor wording are the recommendation |

## Overall Verdict

**Verdict:** PASS

**Rationale:** Iteration 2 takes up both iteration-1 recommendations accurately. It corrects the coverage premise
from the measured JaCoCo report and the class files, instead of defending the earlier estimate. It adds M-T12 to
remove a test that passed because its value equalled the default. Seven criteria are satisfied, and Criterion 8 is
satisfied with one recommendation. That recommendation asks for three things:
- state 2.4 item 13's effect in user terms, and say it is the only automatic incoming path on Android 11 and newer;
- mark the period anchor in L25, and the during-run sentence in item 13, as not verified;
- add the latency to the Performance Efficiency row.

The same fact is raised above as an observation for the orchestrator: a pre-existing product behaviour that belongs
with PO-3 and DES-005.

**Iteration:** 2

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
