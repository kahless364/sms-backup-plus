---
artifact_type: gate-result
gate: expert-depth-check
gate_id: "expert-depth-check"
checkpoint: plan-gate
pipeline: story-build
story_id: "U-070"
artifact_under_review: sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md
artifact_reviewed: sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md
producing_agent: amp/agents/solution-architect
verdict: PASS
severity: warning
agent: "Solution Architect"
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
iteration: 2
timestamp: "2026-11-21T12:27:53Z"
produced_at: 2026-11-21T12:27:53Z
recommendations_count: 0
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
    verdict: satisfied
    deficit_present: false
---

# Gate Report: expert-depth-check — U-070

## Gate: expert-depth-check @ plan-gate

## Verdict: PASS (severity warning, observe-only), no recommendations

## History

- **Iteration 1** (2026-11-21T11:27:06Z, plan at `36e715e7`): PASS with 2 recommendations. Criterion 3: collision,
  privacy, `apply()` and cumulative write cost were not quantified. Criterion 8: five edge cases were absent and R6 was
  "Unknown".
- **Iteration 2** (this report): the plan is the working-tree revision (`iteration: 2`, `base_commit` `2aad7d70`,
  4,094 lines). HEAD is `c8e98a7f`.

## Summary

Every item of both iteration-1 recommendations is taken up, in the place the developer reads. In most places the text
is close to the wording the report proposed:

- **Collision figures.** 2.5 (plan.md:405-415) computes the per-row chance k·5.4·10⁻²⁰, the stress case 2.7·10⁻¹⁶ and
  the birthday bound 6.8·10⁻¹³. A 32-bit digest is weighed and rejected (2.1 row 18).
- **Privacy statement.** 2.5 (plan.md:419-432) separates what a holder of the file *can* learn (dates in clear,
  short-code recovery in under a second, confirmation of a guess) from what they *cannot*. It gives the "hours late"
  mitigation and is honest that a code that is not withheld may still be valid.
- **Key stretching.** Weighed in 2.5 (plan.md:433-444) and 2.1 row 19. The soundness is judged under Criterion 3.
- **`apply()`.** 2.1 row 17 states the deciding reason: the duplicate window grows to every queued row.
- **Cumulative write cost.** 2.6 (plan.md:469-483) does more than iteration 1 asked. It adds the realistic bound, that
  pruning keeps r at the 6-hour neighbourhood (about 31 MB over a 20,000-SMS first backlog), next to the quadratic
  burst case (15.5 MB and 388 MB).
- **Edge cases.** 2.9 (plan.md:598-613) adds all five, plus a sixth found by the planner: a concurrent prune between
  the two reads.
- **R6.** Resolved from the code (L23, plan.md:161; 2.7 "Concurrency", plan.md:526-541). The lock's per-process scope
  is argued from `static final` and one class loader.
- **The C1 label.** "A consumer's checkpoint committed atomically with its deduplication state" (Kleppmann ch. 11)
  replaces "Transactional Outbox". The plan also says why the restore's write-ahead entry is the one Outbox-like part
  (plan.md:194-200).
- **Timestamp.** It is now `12:09:01Z`, before this review's clock (12:27:53Z), and so consistent with `date -u`.

Two design changes since iteration 1 were judged. Both are sound and grounded:
- **(1) The UTF-16 code-unit digest.**
  - The injectivity argument (plan.md:392-404) is correct in both steps. Step (i): length prefixes make the canonical
    text a prefix-decodable encoding of the triple. Step (ii): two bytes per `char` is total and one-to-one, unpaired
    surrogates included.
  - The defect it closes is real and demonstrated. Under UTF-8, `"\uD800"`, `"\uD801"` and `"?"` share a digest (L21).
  - It is pinned by W2's new vectors and by M-L24.
  - The P1 code (plan.md:845-864) implements exactly the stated form: `c >> 8`, then the low byte.
- **(2) The second record read** (2.4, plan.md:358-384; P5b, plan.md:1288-1336).
  - **The happens-before argument is sound.** The restore commits the entry before it starts the insert (program
    order). A query that returns R observes the completed insert. The backup's second read comes after the queries
    return. The entry is therefore visible:
    - `commit()` applies its change to the in-memory map under the `SharedPreferencesImpl` lock before it writes;
    - `getString` takes the same lock;
    - both threads hold the same cached instance from `getDefaultSharedPreferences`.
  - **The test bites.** B26 (plan.md:2611-2640) injects the restore's two steps at the start of each query, so it
    runs after the first read and before the second. M-L26 (the iteration-1 single read) therefore fails it, and D23
    covers the case where it does not.
  - **The anti-pattern is named.** It is "Check-then-act across a concurrent writer" (plan.md:233-235).

## Findings

### Criterion 1 — Pattern grounding

**Verdict:** satisfied

**Evidence present:** Section 2.0 (plan.md:177-221) keeps all of iteration 1's grounded patterns, each applied to a
named site: Dataflow allowed lateness, Idempotent Receiver, ARIES write-ahead, the Meszaros doubles, the Feathers seam,
and the DeMillo mutant classes, now including M-L22 to M-L26. The checkpoint label is corrected (plan.md:194-200).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied

**Evidence present:** The six anti-patterns of iteration 1 remain. Two are added, each tied to the defect it caused and
to the change that removes it:
- **Check-then-act across a concurrent writer** (plan.md:233-235): the record read before the queries;
- **Charset-encoding before hashing** (plan.md:236-237): malformed characters are replaced, so the encoding is not
  one-to-one.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied

**Evidence present:** 2.1 now has 20 rows (plan.md:254-275), each with gained, gives way and decision. Rows 17 to 20
carry the reason and the test or mutation that pins the choice:
- row 17, `apply()`: B13, M-L12 (b);
- row 18, the 32-bit digest: the 2³² factor;
- row 19, key stretching: 2.5;
- row 20, a single read: B26, M-L26.

Every quantity iteration 1 asked for is computed: 2.5 for collisions and the cost of a guess, 2.6 for cumulative
writes.

**Is the key-stretching rejection sound and honestly argued?** Yes.
- **Option and cost.** The candidate is named precisely: PBKDF2-HMAC-SHA256, RFC 8018 §5.2, with a public salt and so
  no key, which keeps AC-17. The phone's cost is stated, about 10 ms per entry at 10⁴ iterations. The plan is candid
  that its own code computes each window row's entry twice (plan.md:436-438; `entryOf(window)` at plan.md:1297 and
  :1316).
- **Gain.** It is stated without inflation: 10⁶ to 10¹⁰ hash evaluations, "seconds to minutes on one GPU". The plan
  says outright that "the outcome for the case that matters, a short code, does not change".

That is the correct conclusion. A 6-digit code has about 20 bits of entropy, and no work factor a phone can afford on
every row of every run turns 2²⁰ guesses into a real barrier. The plan does not claim otherwise. The rejection rests on
three stated grounds: the cost is paid on every run, the gain is only a delay, and the exposure is bounded to 6 hours of
history that is also in the mailbox. A secret key is handled separately and correctly (row 8, plan.md:443-444). The
absolute claim of iteration 1, "no stronger protection is possible", is gone.

**Evidence missing:** (none at a level that affects the verdict). Two optional strengtheners need no plan change:
- **Memory-hard functions are not mentioned.** scrypt and Argon2 are not in the Android framework at minSdk 24. They
  would need a library, which section 12 item 9 forbids. One clause would close that alternative.
- **The general argument.** Raising the iteration count until it matters, for example 10⁶, would cost about 1 s per
  row on the phone. That strengthens "the outcome does not change" from an observation about 10⁴ iterations into a
  general argument.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 4 — Citation depth

**Verdict:** satisfied

**Evidence present:**
- **Literature.** The iteration-1 anchors stay. New ones are anchored too: Kleppmann *DDIA* ch. 11, "Fault Tolerance",
  the "Idempotence" and "Atomic commit revisited" parts; RFC 8018 §5.2; Richardson ch. 3.3.7, now cited for what the
  Outbox *is*.
- **New code facts are anchored to file and line:**
  - L23: `WorkManagerScheduler.kt:154`, `:289`, `:324`, `:365`, `:400`, and the empty `android:process` grep;
  - L21: the UTF-8 substitution, demonstrated in Python and Java 25;
  - L24: the scratch `javac --release 17 -Werror` compile of P1 to P5, and the 32 Java mutants.

**Evidence missing:** (none at a level that affects the verdict). The 2.4 race argument says that
`SharedPreferencesImpl` "updates its in-memory map and serves `getString` under one lock (L14)". L14 was read for the
file-write path, not for `commitToMemory` and `getString`. The claim is true of the framework, and it is the one fact
the race argument rests on. When the developer next touches L14, it should cite those two methods in the same
disassembly.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** Section 13 still evaluates all eight ISO/IEC 25010 characteristics by name. Iteration 2's
changes fall inside them:
- **Security / confidentiality:** the 2.5 disclosure statement;
- **Performance Efficiency / resource utilisation:** 2.6's cumulative write cost;
- **Reliability:** 2.7 row 11 and the concurrency resolution.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:** Iteration 1's signals stay: `javap`, JaCoCo, the greps and the Python vectors. Iteration 2 adds
stronger deterministic evidence than most plans carry (plan.md:41-50, L24):
- the plan's production Java was extracted by script and compiled with `-Werror -Xlint:unchecked`;
- each of the 32 Java mutants was shown to have a unique anchor and to compile;
- the digest vectors were computed by two independent implementations.

The limit is stated: Kotlin test text was not compiled.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:**
- The I0 to I11 order and its reasons are unchanged.
- The new 5.1, "Sessions and hand-over", adds two sessions with named safe stopping points: after I1b, after I7,
  between I8 entries, and before I9.
- The resume step S2-0 rebuilds the pristine copies from the stored blobs before any mutation runs. This turns an
  implicit hazard, a lost session in the middle of a mutation, into an explicit dependency.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied

**Evidence present:**
- **2.9 edge cases.** The new entries each state the outcome and whether it is a duplicate or a loss. They are not a
  list of words:
  - clock moved back by d against W;
  - time zones, with the one zone-dependent header named;
  - dual SIM reduced to the SC-6 case;
  - network re-delivery, with the reason no de-duplication is attempted;
  - updates to non-key columns, against key columns;
  - the concurrent prune.
- **R6 and 2.7 "Concurrency".** These replace "Unknown" with a code-derived result: the overlap is possible, one
  process, one lock, worst case one duplicate per row both workers select, never a loss. The out-of-scope part, mutual
  exclusion, is justified by naming the contract it would change.
- **The two new design changes** each come with a proof sketch, a test and a mutation.

None of the nine doctrine anti-patterns is present.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

## Notes for the developer and the plan-review gate (outside the eight criteria; no depth effect)

1. **The per-run limit under a concurrent prune.** 2.4 (plan.md:381-382) says the late plus kept forward rows "never
   exceed `max`", except for the concurrent-prune case in 2.9. 2.9 gives only that case's duplicate, not its effect on
   the limit.
   - **How the limit is exceeded.** In that case the second read lacks pruned entries that the first read held. Late
     rows can then outnumber `lateBefore`, while `remaining` was computed from `lateBefore` (P5b, plan.md:1300 and
     :1318). The SMS cursor can therefore exceed `max` by the number of rows pruned in between.
   - **The effect is contained.** At HEAD (`c8e98a7f`), `BulkFetcher.fetch` clamps with `Math.max(max - count, 0)` and
     breaks at 0, so MMS and call log are not made unlimited.
   - **Fix options.** Either add one sentence to 2.9 stating the bounded overshoot, or cap step 7 at
     `late.size() < max - remaining`. The cap keeps the exact limit and defers those rows, which are duplicates in
     either case, to the next run.
2. **Line drift since `base_commit`.** `git diff 2aad7d70 HEAD -- app/src/main` shows only `AndroidManifest.xml`, with
   14 deleted lines (U-068's two `maxSdkVersion` permissions). `allowBackup` and `fullBackupContent` are now at `:81-82`,
   not L9's `:95-96`.
   - No edit anchors there, and S10/S11 parse the descriptor, not line numbers. L9's conclusion holds.
   - I0's re-read covers the drift.

## Overall Verdict

**Verdict:** PASS

**Rationale:** Iteration 2 closes both iteration-1 recommendations completely, and both new design changes are
correct:
- **Criterion 3:** the collision probability, the attacker's cost, the clear-date disclosure, `apply()`, a shorter
  digest and the cumulative write cost are all computed. The key-stretching rejection is honestly argued: the cost is
  stated, the gain is not inflated, and the plan says plainly that it changes nothing for a 6-digit code.
- **Criterion 8:** every listed edge case is stated with its outcome. R6 is resolved from the scheduler and the
  manifest.
- **The UTF-16 code-unit digest** has a correct two-step injectivity argument and closes a demonstrated UTF-8
  substitution collision.
- **The second record read** closes the restore race by a sound happens-before chain, which B26 pins with M-L26.

The other six criteria still hold and are strengthened by the new anchors and compile evidence. All eight are
satisfied, with no recommendations.

**Iteration:** 2

**Rework routing (FAIL only):** Not applicable (PASS).

## Phase Completion Report
---
story_id: "U-070"
phase: "plan-gate/expert-depth-check"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-070/expert-depth-check.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
