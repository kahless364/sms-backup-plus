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
iteration: 1
timestamp: "2026-11-21T11:27:06Z"
produced_at: 2026-11-21T11:27:06Z
recommendations_count: 2
criterion_results:
  - criterion: "Pattern grounding"
    verdict: satisfied
    deficit_present: false
  - criterion: "Anti-pattern recognition"
    verdict: satisfied
    deficit_present: false
  - criterion: "Trade-off articulation"
    verdict: satisfied_with_recommendation
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

# Gate Report: expert-depth-check — U-070

## Gate: expert-depth-check @ plan-gate

## Verdict: PASS (severity warning, observe-only), 2 recommendations

## Summary

The plan is iteration 1. It was committed at `36e715e7`, which is HEAD. Its `base_commit` is `c9770c30`. Between the two,
`app/src/main` differs only in U-067's two worker files (`git diff --stat c9770c30 HEAD -- app/src/main`: 7 insertions,
6 deletions in `BackupWorker.kt` and `RestoreWorker.kt`), and the plan's section 2.10 expects exactly that.

What this review read:
- **In full:** sections 1, 2.0 to 2.10, 3.1 to 3.5, 4.8, 5.0, I0 to I8's header, 6, 7, 8, 9, 10, 11, 12 and 13.
- **Test samples:** B7, B8, B9 to B12, B13, B14 (plan.md:2009-2114), S8 and S9 (plan.md:2765-2790), and the start of
  S10/S11's descriptor parsing.
- **Read-only checks:** `git grep` at HEAD for `enqueueUniqueWork` / `ExistingWorkPolicy` and for `android:process`.
  These were used for the Criterion 8 recommendation.

The design is grounded and tested to a standard well above the usual:
- **Grounding.** It names the patterns: allowed lateness in the Dataflow sense, Idempotent Receiver, at-least-once
  delivery plus deduplication, the write-ahead rule, and the Fake / Spy test doubles. It applies each to a named site.
- **Atomicity.** The claim rests on the disassembled `SharedPreferencesImpl` (L14), not on folklore. The plan says
  which part was not verified (API 36/37).
- **Rejected options.** There are 16 rows, most of them with a criterion-level reason.
- **Process death.** The failure-window table covers ten points, and says which of them can be tested.
- **Proving "exactly once".** The deterministic multi-run tests check row id, Message-ID and triple for duplicates
  (B7). The death-after-confirm test pins the duplicate to exactly one message (B13). 21 mutation ids (32 runs) have
  predicted failures.

Two areas fall short of the quantified, senior-level standard the brief sets:
- **Criterion 3.** Several trade-offs are stated but not computed: digest length and collision probability, what the
  digest gives an attacker and what it costs to guess, `commit()` against `apply()`, and the cumulative write cost of
  rewriting the record on every commit.
- **Criterion 8.** Some edge cases from the brief are absent: clock moved backwards, time zones, dual SIM, network
  re-delivery, and updates to non-key columns. Risk R6 is left as "Unknown" when a grep answers it.

Neither gap changes the design.

## Findings

### Criterion 1 — Pattern grounding

**Verdict:** satisfied

**Evidence present:** Section 2.0 (plan.md:105-143) names each pattern with its source and maps it onto this
mechanism:
- **Watermarks and allowed lateness** (Dataflow §2.3; *Streaming Systems* ch. 2, 3). The SMS watermark is a heuristic
  watermark, a withheld SMS is late data, W is the allowed-lateness horizon, and the record is the per-window state
  that the horizon lets the code garbage-collect (plan.md:110-114). This is the right lens for the failure of a
  high-watermark incremental sync when data arrives late. It goes further than the bug's own framing.
- **Idempotent Receiver** (EIP ch. 10), applied on the sender side because IMAP APPEND is not idempotent. The key is
  the content, not `_id`, with the reason given (plan.md:115-118).
- **At-least-once delivery plus deduplication equals effectively once**, with the duplicate window bounded to one
  in-flight append (plan.md:119-123).
- **The ARIES write-ahead rule**, applied to restore with the direction of the harmless failure argued
  (plan.md:124-127).
- **Meszaros test doubles**, each mapped to a class (plan.md:128-132).
- **The Feathers seam**: a mocked builder returns null and the old path runs (plan.md:141-143).

**Evidence missing:** (none at a level that affects the verdict). One accuracy note, which needs no plan change. "The
single-store form of the Transactional Outbox" (plan.md:121-123) is a loose fit:
- Richardson's Outbox records the *outgoing message* in the same transaction as the state change, *before* it is sent,
  and a relay sends it later.
- The upload commit here is written *after* the send. It is closer to committing a consumer's checkpoint together with
  its deduplication state (Kleppmann, *Designing Data-Intensive Applications*, ch. 11, "Fault Tolerance", the
  "Idempotence" and "Atomic commit revisited" parts).
- Only the restore's write-ahead entry is Outbox-like.

The design conclusion (one atomic write, not two stores) is unaffected.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied

**Evidence present:** "Anti-patterns named and avoided" (plan.md:145-156) names six, each with its consequence:
- moving the watermark backwards, which re-uploads the last W on every run;
- bounded-by-count state that evicts live entries (an LRU cap), which is a loss path;
- a Bloom filter for "already sent", where a false positive is a silent loss;
- two stores for one invariant;
- logging message content, including a pre-existing debug VERBOSE leak (L15), handed on in section 11 item 3;
- testing a subclass of a Robolectric-shadowed class (`CursorWrapper`, L12).

The other anti-patterns in the brief are handled where the design makes the choice:
- **Keying on the provider `_id`:** rejected in 2.1 row 5 and pinned by M-L7.
- **Unbounded deduplication sets:** avoided by pruning at `watermark - W` (plan.md:604-610), which Z1 asserts over
  200 runs.
- **Wall-clock assumptions:** avoided by measuring W against the watermark, not the time the backup runs
  (plan.md:244-245, :479-482, and the test note at :1883).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied_with_recommendation

**Evidence present:**
- **Section 2.1** (plan.md:170-187) has 16 options with gained, gives way, and the decision. Most rejections cite the
  criterion they would break:
  - row 3, server search: needs a port change, adds a round trip per row (forbidden by AC-21), and an outbox-to-sent
    row would be appended twice;
  - row 4, an SQLite table: not atomic with the watermark;
  - row 8, an HMAC key: either useless or breaks AC-17;
  - row 10, a separate file: two commits.
- **Cost of the choice** is stated at plan.md:189-191.
- **The window length** is derived as 3 h + 3 h, with both halves openly unmeasured and a revisit path
  (plan.md:241-249).
- **Size** is quantified at 31 bytes per entry, about 3.1 KB normally and 155 KB at 5,000 entries (plan.md:313-321).

**Evidence missing:** four trade-offs the brief asks to be quantified are stated only qualitatively, or not at all:
1. **Collision probability** is given as a formula, "about n²/2⁶⁵ … negligible for any n that occurs"
   (plan.md:282-284). It is not computed for the realistic number of entries. The digest-length trade is argued in one
   direction only: "a longer digest buys nothing measurable" (plan.md:287). A shorter one is not considered.
2. **Privacy residual** (plan.md:291-295). The plan states the 10⁶-guess recovery of a 6-digit code, which is right.
   It does not state:
   - the attacker's cost;
   - what else the file reveals: the dates are stored in clear, so the exact time and number of every SMS in the last
     6 h are readable. "No address and no text are stored" (plan.md:280) is true but is not the whole disclosure;
   - what the attacker cannot learn.

   Its closing "No stronger protection is possible without breaking AC-17" is an absolute claim. A real alternative is
   not evaluated: key stretching, a slow hash, which needs no key and so does not conflict with AC-17.
3. **`commit()` against `apply()`.** Section 2.7 explains what `commit()` does (plan.md:349-360), and B13 asserts
   `applies == 0` (plan.md:2095). `apply()` is never weighed as a rejected alternative. The decisive reason is
   unstated: `apply()` would widen AC-10's duplicate window from one in-flight append to every row whose write is
   still queued.
4. **Cumulative write cost.** R5 gives the size per commit (plan.md:3382). The whole file is rewritten on every row's
   commit, so a run that uploads n rows while the record holds r entries writes about 31·(n·r + n²/2) bytes. The
   per-commit figure hides that this is quadratic.

**Passage:** 2.1 table (plan.md:170-187); 2.5 Collisions and Residual risk (plan.md:281-295); 2.7 (plan.md:349-360);
2.6 Size and R5 (plan.md:313-321, :3382).

**Recommendation / Remediation:** add the following to 2.5, 2.1 and 2.6. Text can be used as written.

- **2.5 Collisions, replace the second and third bullets with:**

  > "A loss needs a late row whose `date:digest16` equals a recorded entry, so only entries with the same millisecond
  > compete. For k such entries the chance per late row is k/2⁶⁴ ≈ k·5.4·10⁻²⁰. In normal use k ≤ 1, so the chance is
  > about 5·10⁻²⁰ per late row and about 5·10⁻¹⁵ over 10⁵ late rows. Even if all 5,000 entries of the stress case
  > shared one millisecond, it would be 2.7·10⁻¹⁶. Ignoring the date entirely, the birthday bound over 5,000 entries
  > is 5,000²/2⁶⁵ ≈ 6.8·10⁻¹³. A 32-bit digest (8 hex characters) would still give k·2.3·10⁻¹⁰ and save 8 of the 31
  > bytes (26 %). It is not chosen, because the saving is small next to file size and the loss probability rises by a
  > factor of 2³²."

- **2.5 Residual risk, replace the last sentence with:**

  > "What a holder of the file **can** learn:
  > (a) the exact time, to the millisecond, and the number of SMS received or sent in the last 6 h;
  > (b) the full text of a message whose address and template can be guessed. For a 6-digit code that is 10⁶ SHA-256
  > evaluations, well under a second on one CPU core;
  > (c) confirmation of any guessed (address, body) pair, such as 'Ok' to a known number.
  >
  > What it **cannot** learn:
  > - any message text with real entropy;
  > - an address on its own (a guess needs address and body together);
  > - anything older than W once pruned.
  >
  > On Android 17 a withheld one-time code is recorded only after it becomes visible, which is at least 3 h after
  > receipt, so such codes are normally expired. Codes that are not withheld are recorded within minutes.
  >
  > Key stretching (PBKDF2-HMAC-SHA256, RFC 8018 §5.2) at 10⁴ iterations costs about 10 ms per entry on the phone. A
  > run reads tens of window rows, which is under 1 s; at 1,000 rows in 6 h it is about 10 s per run. It raises an
  > attacker's cost for one guess hypothesis to 10¹⁰ hashes, which a GPU does in seconds to minutes. It is rejected:
  > the residual exposure is bounded to 6 h, and the same texts are in the owner's mailbox.
  >
  > A key would add protection only if it does not travel with the file. A key that does not travel breaks AC-17
  > (row 8)."

- **2.1, new row 17:**

  > "`apply()` instead of `commit()` | the worker thread does not wait for fsync | a write that is queued but not yet
  > on disk when the process dies is lost, so every row appended since the last flushed write is uploaded again: the
  > duplicate window grows from one in-flight append (AC-10) to every queued row | Rejected (B13 asserts
  > `applies == 0`)."

- **2.6 Size, add:**

  > "The whole file is rewritten on every row's commit. A run uploading n rows over a record of r entries writes about
  > 31·(n·r + n²/2) bytes: 15.5 MB for n = 1,000 starting from empty, and 388 MB for n = 5,000. Normal use (n and r in
  > the tens) is a few tens of KB. The per-run limit bounds n. The stress case is accepted unmeasured (SC-8). The fsync
  > per commit exists today, so the added cost is bytes and XML serialisation only."

### Criterion 4 — Citation depth

**Verdict:** satisfied

**Evidence present:**
- **Literature.** Nearly every citation is anchored:
  - Dataflow paper, *PVLDB* 8(12), §2.3;
  - *Streaming Systems*, ch. 2 and 3;
  - EIP ch. 10, "Idempotent Receiver";
  - Richardson ch. 3.3.7;
  - ARIES, *ACM TODS* 17(1), §1;
  - Meszaros ch. 23;
  - Feathers ch. 4, "The Seam Model";
  - DeMillo, Lipton and Sayward, *IEEE Computer* 11(4);
  - RFC 2104.

  Only "Gamma et al., 'Decorator'" lacks a chapter, and it is anchored by its catalogue pattern name. That is well
  above 80 %.
- **Framework and library behaviour** is anchored to disassembly offsets: `ShadowContentResolver` `:387-420` and
  `:228-265`, and `SharedPreferencesImpl` `:144-155`, `:997-1095` and `:1164` (L11, L14).
- **Project facts** are anchored to file and line (L1 to L20).
- **Unverified claims are labelled.** L14 is "verified (Android 10); plausible, not verified (API 36/37)" (plan.md:84),
  and R8 carries that limit into the risks (plan.md:3385).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** Section 13 (plan.md:3487-3498) evaluates all eight ISO/IEC 25010 characteristics by name, with
sub-characteristics:
- Functional Suitability: completeness, correctness, appropriateness;
- Performance Efficiency: time behaviour, resource utilisation;
- Compatibility: co-existence, interoperability, upgrade;
- Usability: no UI change, two README sentences;
- Reliability: fault tolerance, recoverability, a monotone watermark;
- Security: confidentiality, the residual guessability, the VERBOSE line;
- Maintainability: modifiability, testability, analysability;
- Portability: adaptability, framework APIs only.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:** Each deterministic signal is interpreted and used for a decision:
- **`javap` disassembly of the Robolectric and Android 10 jars** (L11 to L14). The `CursorWrapper` shadow finding
  decides 2.1 row 12. The `SharedPreferencesImpl` rename/fsync path is the basis of the atomicity claim.
- **JaCoCo package ratios** from the last XML report (L17), turned into projected margins of 78.7 % and 80.3 % against
  floors of 70 % and 65 % (plan.md:322-330), and re-measured at I0 and I9.
- **`git grep` census** of every watermark reader and writer (section 9) and of name collisions (L19).
- **Digest test vectors and Message-IDs** computed independently in Python (L21, L22), with D8 forbidding a vector
  from being edited to match.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:** I0 to I11 (plan.md:2980 onward) are strictly ordered, with stated preconditions:
- **I0** is "no edit before this passes", and checks the landing of U-067, U-068 and U-069 (D1).
- **I1 and I1b** write the tests on the start commit and record the AC-6 literal there, before any production edit.
  This is what proves "fails on START".
- **I2 to I5** follow the compile dependencies: the record layer P1/P2, then the queries P3, then the fetch layer and
  worker P4 to P6, then restore P7.
- **I7** takes pristine copies and blob hashes before the I8 mutations. Each mutation has a write-ahead `APPLYING`
  entry and a recovery protocol.

Parallelism is ruled out on purpose: foreground Gradle only, no background shells (section 12 items 1 to 3). A strictly
serial order is therefore correct, not an omission. The decision table (D1 to D21) gives the branch for every observed
outcome.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied_with_recommendation

**Evidence present:**
- **Failure windows by process-death point.** The table (plan.md:336-347) says which windows can and cannot be tested.
  Row 6 is "not unit-testable … API 36/37 assumed".
- **The limits of `commit()` are stated.** A false return leaves memory and disk different, which gives a duplicate,
  never a loss (plan.md:356-358).
- **The upgrade marker is reasoned through.** `stored ≤ 0 || marker` means no write can hide a missing seed
  (plan.md:234-239, :730-732).
- **The per-run limit arithmetic is explained.** The LIMIT is widened by the entries above the watermark (2.3,
  plan.md:251-257).
- **Exactly once is proved with three identities.** B7 (plan.md:2009-2023) checks row id, Message-ID and content
  triple for duplicates across four runs with a late arrival.
- **The accepted duplicate is pinned to one message.** B13 (plan.md:2078-2103) shows the duplicate after a confirmed
  append and death is exactly f1, with one commit that carries both keys.
- **Restore is tested at the death point.** S8 (plan.md:2765-2777) dies between the insert and the watermark raise.
- **Each risk has likelihood and impact** (plan.md:3376-3389).

None of the nine doctrine anti-patterns degrades the analysis.

**Evidence missing:** a senior reviewer in data synchronisation would push back on two things.
1. **Edge cases in the brief that 2.9 (plan.md:390-406) does not state:**
   - the device clock moved *backwards* (2.9 has only the future-dated row, SC-12);
   - time zones and daylight-saving changes;
   - dual SIM (`sub_id` is not in the key);
   - network re-delivery of the same SMS under a new `date`;
   - the default SMS app updating non-key columns (`read`, `seen`, `status`, `thread_id`).

   The design handles each of them, but the plan does not say so. That leaves a reviewer to re-derive the results.
2. **R6 is "Unknown (not examined)"** (plan.md:3383; 2.7 at :361-363). That is a deferral where the code answers the
   question in one grep:
   - At HEAD, `WorkManagerScheduler.kt` enqueues each backup type under its own unique name with
     `ExistingWorkPolicy.REPLACE` (`:154`, `:289`, `:324`, `:365`, `:400`). A manual and an incoming backup can
     therefore run at the same time. REPLACE's cancellation of the same name is also cooperative.
   - No `android:process` appears under `app/src/main`, so both workers share one process and `SMS_RECORD_LOCK`
     covers them.

   The story's Scope Concern 9 left the question open, and the plan could have closed it.

**Passage:** 2.9 Edge cases (plan.md:390-406); 2.7 Concurrency (plan.md:361-363); Risks R6 (plan.md:3383).

**Recommendation / Remediation:**

- **Add to 2.9:**

  > - "**Device clock moved backwards** by d after a backup. A new SMS is dated by the clock, so it is dated below the
  >   watermark. If d < W it falls inside the window and is uploaded once, which today's code does not do. If d ≥ W it
  >   is skipped, as today. Forward jumps are SC-12."
  > - "**Time zones and daylight saving.** `date` is UTC epoch milliseconds. The key, the window and pruning do not
  >   depend on the zone. Only the `Date` header uses the default zone (L16), and the tests fix it to UTC."
  > - "**Dual SIM.** `sub_id` is not in the key. The same text from the same sender in the same millisecond on both
  >   SIMs gives one entry, which is the SC-6 case. Nothing else changes."
  > - "**Network re-delivery** (the same SMS delivered twice). The provider holds two rows with different `date`s, so
  >   they have two keys and two Message-IDs, and both are uploaded, as today. This is not a target for
  >   deduplication."
  > - "**Updates to non-key columns** (`read`, `seen`, `status`, `thread_id`, `type`). The key is unchanged. A change to
  >   `date`, `address` or `body` changes the key and gives one duplicate, never a loss (as R7)."

- **Replace R6 with:**

  > "Two backup workers overlap (SC-9) | Possible: each backup type has its own unique-work name with REPLACE
  > (`WorkManagerScheduler.kt:154`, `:289`, `:324`, `:365`, `:400` at HEAD), so a manual and an incoming backup can
  > run at once | Low: both can select the same late or forward row before either commits, so it is uploaded twice
  > (as today for forward rows). The watermark takes `max()` and the record a set union under `SMS_RECORD_LOCK`, so no
  > entry or watermark is lost. Both workers run in the one app process (no `android:process`), so the lock covers
  > them | Not designed here; mutual exclusion would need a scheduler change (CNTR-MODERNIZATION-004)."

## Edge-case review (the brief's list)

| Edge case | Plan coverage |
|-----------|---------------|
| Clock changes | Forward: SC-12 (plan.md:398-399). Backward: absent (Criterion 8 recommendation) |
| Time zones | Not stated for the key; implicit in epoch dates, and the test fixes UTC (plan.md:1801-1802) (recommendation) |
| Equal timestamps | 2.9 (plan.md:392-393), SC-6 accepted |
| Edited or re-delivered messages | Type change: AC-8, B8. Address or body normalisation: R7. Re-delivery and non-key updates: absent (recommendation) |
| Dual SIM | Absent (recommendation) |
| Restored phone | AC-17: S10 and S11 parse the real backup descriptor |
| Skip and Reset | 2.3 table; S1 to S6, W5 |
| Two workers overlapping | R6, "Unknown" (recommendation) |
| Process death at each step | 2.7, ten rows; B9 to B13, S8 |
| Very large backlogs and the per-run limit | 2.3 per-run limit; B1 to B3; Z1; the quadratic write cost is not computed (Criterion 3 recommendation) |
| Upgrade from the version before this change | Marker, seed, B18, 2.7 rows 1 and 2 |
| Privacy of a truncated digest | 2.5: the 10⁶ guess is stated; the attacker's cost, the clear-date metadata and key stretching are missing (Criterion 3 recommendation) |

**Consistency note (outside the eight criteria):** the plan's frontmatter `timestamp: "2026-11-21T14:00:00Z"`
(plan.md:8) is later than this review's `date -u` (2026-11-21T11:27:06Z). The plan's own rule is that timestamps come
from `date -u`, so the value was not taken from the clock. It should be corrected when the plan is next revised.

## Overall Verdict

**Verdict:** PASS

**Rationale:** The plan is grounded in the right literature for a late-data problem in a watermark sync:
- Dataflow allowed lateness, Idempotent Receiver, at-least-once delivery plus deduplication, and ARIES write-ahead,
  each applied to a named site.
- The decisive atomicity claim rests on disassembled framework code, with its API 36/37 limit stated.
- 16 alternatives are rejected, mostly against named criteria.
- Exactly once is shown by multi-run tests across three identities, by a death-after-confirm test that pins the
  accepted duplicate to one message, and by 21 mutation ids.

Two recommendations remain, the maximum for a PASS:
- **Criterion 3:** compute the collision probability and the digest-length trade; state the attacker's cost and the
  clear-date disclosure, and weigh key stretching; add `apply()` as a rejected row; give the quadratic commit-write
  cost.
- **Criterion 8:** state the clock-backwards, time-zone, dual-SIM, re-delivery and non-key-update cases; replace R6's
  "Unknown" with the likelihood the scheduler code shows.

**Iteration:** 1

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
