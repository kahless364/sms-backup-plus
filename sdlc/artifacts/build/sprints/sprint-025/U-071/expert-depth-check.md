---
artifact_type: gate-result
gate: expert-depth-check
gate_id: "expert-depth-check"
checkpoint: plan-gate
pipeline: story-build
story_id: "U-071"
artifact_under_review: sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md
artifact_reviewed: sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md
producing_agent: amp/agents/solution-architect
verdict: PASS
severity: warning
agent: "Solution Architect"
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
iteration: 2
timestamp: "2026-11-21T15:55:34Z"
produced_at: 2026-11-21T15:55:34Z
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
    verdict: satisfied
    deficit_present: false
  - criterion: "NFR and quality-attribute coverage"
    verdict: satisfied
    deficit_present: false
  - criterion: "Tooling evidence"
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "Sequencing rigor"
    verdict: satisfied
    deficit_present: false
  - criterion: "Anti-AI-slop check"
    verdict: satisfied_with_recommendation
    deficit_present: false
---

# Gate Report: expert-depth-check — U-071 (iteration 2)

## Gate: expert-depth-check @ plan-gate

## Verdict: PASS (severity warning, observe-only), 2 recommendations

## History

- **Iteration 1** (2026-11-21T14:19:35Z; plan iteration 1 at `ba8eb9d2`): PASS with 2 recommendations
  (Criterion 3: four trade-offs; Criterion 8: six edge cases). Kept below, marked superseded.
- **Iteration 2** (this report). It covers plan iteration 2: the working-tree `plan.md`, 2390 lines, frontmatter
  `iteration: 2`, timestamp `2026-11-21T15:17:24Z`. At the time of review HEAD is `0c99149e` and the plan is
  modified, not yet committed. Line numbers below refer to that working-tree file.

## Iteration 1 recommendations: taken up or not

Every item was taken up.

| Iteration-1 item | Status | Where in plan iteration 2 |
|------------------|--------|---------------------------|
| C3.1: increments, not one raise | taken up | 2.5 row, plan.md:419: all four elements, including the two intermediate pairings and the D26/D35 guards |
| C3.2: no version catalog | taken up | plan.md:420 |
| C3.3: what additions-only verification does not protect | taken up | plan.md:421: trust on first use, signatures off, the two `<trust regex>` lines (now named as the `-javadoc.jar` and `-sources.jar` exemptions), same-host `.sha512`. Signature verification is a further rejected alternative |
| C3.4: caches off on comparison runs | taken up | 2.5 row, plan.md:422. `NC` flags on G, ENF, UTD, KC, TR, L and DEP (plan.md:1261-1271). I0 check, plan.md:1492-1493. D94 (plan.md:2044). WP keeps the design's exact command and gains the flags only under D94 |
| C8.1: Kotlin compile daemon | taken up | the `jps` greps match `gradle\|KotlinCompileDaemon` (plan.md:1286, :1453, :1490); 2.8 bullet, plan.md:490-493; D92 (plan.md:2042) |
| C8.2: incremental check | taken up | UTD after G(2) and G(5), with four tasks expected `UP-TO-DATE` (plan.md:1267); 2.7 row, plan.md:455 |
| C8.3: AGP 9.1 to 9.5 behaviour changes | taken up | Q-13 to Q-17 (plan.md:359-363); explicitly set defaults listed (plan.md:494-497); D44 classifies an I3 difference against those entries (plan.md:1994) |
| C8.4: JaCoCo filters | taken up | R19 (plan.md:2161); 2.9 I12 cell, plan.md:532; D40 (plan.md:1990); 2.8 bullet, plan.md:498-500, citing Q-11's "no filter change listed" |
| C8.5: timeout | taken up | 4.L (plan.md:1274-1289); D93 (plan.md:2043); R21 (plan.md:2163); section 7 note (plan.md:2066-2070) |
| C8.6: ordering-constraint table | taken up | 2.2 item 6, plan.md:315-331: nine edges with source, order and the row that fires, plus the closing sentence |

## Iteration 2 summary

The new material is at expert depth. The R8 comparison (2.10, plan.md:545-593; `r8_check.py`, plan.md:1080-1207)
works at six levels: kept classes, manifest closure, member seeds and usage within the contract prefixes,
contract-bearing rules whatever their source, allowed sections per increment, and header normalisation. That goes
further than most teams go before an AGP raise. It was tested on U-068's real outputs (L26). The UTD and 4.L
additions are sound in design.

Two criteria still carry a recommendation.
- **Criterion 6.** R8 and packaging characterisation still lacks:
  - which R8 actually ran;
  - the release dex's class and method counts;
  - the dex format version;
  - the baseline-profile assets;
  - a stated reason why the resource shrinker is out of the comparison.
- **Criterion 8.** Two new binding facts are not yet carried into the plan (ruling 58 and the BUG-029 story landing
  before `<start>`), and one table row contradicts the iteration-2 procedure.

## Iteration 2 findings

### Criterion 1 — Pattern grounding

**Verdict:** satisfied

**Evidence present:** Section 2.0 is unchanged (plan.md:181-208), with the seven principles each applied as in
iteration 1. The new 2.10 extends the fitness-function principle to R8 with named, executable checks (R8-1 to R8-6).

**Evidence missing:** (none) — **Passage:** (N/A) — **Recommendation / Remediation:** (none)

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied

**Evidence present:**
- **Comparison on raw text that differs for irrelevant reasons.** The plan names the iteration-1 flaw: raw-text
  equality of `configuration.txt` "stops at I1 on the Gradle version in every header" (2.5 row R8, plan.md:418;
  L26). It replaces it with normalisation.
- **A check too narrow to see the contract.** Class names in four prefixes "cannot see Tink's protobuf fields,
  worker constructors or manifest classes outside the prefixes" (plan.md:418).

**Evidence missing:** (none) — **Passage:** (N/A) — **Recommendation / Remediation:** (none)

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied

**Evidence present:** 2.5 (plan.md:410-426) now carries the four rows iteration 1 asked for (see the table above),
plus a new R8 row (plan.md:418) with all four elements. A senior reviewer would raise no remaining trade-off.

**Evidence missing:** (none). One stale row is a consistency defect, not a missing trade-off; it is under
Criterion 8, item 3.

**Passage:** (N/A) — **Recommendation / Remediation:** (none)

### Criterion 4 — Citation depth

**Verdict:** satisfied

**Evidence present:**
- **Supplied facts.** Each is labelled as a table value, a checksum or a stated fact, with address and fetch date
  (2.2, plan.md:280-284; 2.3, plan.md:341-353). Nothing is called verbatim that is not.
- **Request URLs.** Q-13 to Q-18 each give a versioned release-notes URL (plan.md:359-364).
- **Ordering edges.** Each edge in the constraint table cites its C-row or Q-row (plan.md:318-328).

**Evidence missing:** (none) — **Passage:** (N/A) — **Recommendation / Remediation:** (none)

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** Section 12 still evaluates all eight ISO/IEC 25010 characteristics by name. Performance
efficiency now states 71 + 5 = 76 runs and the backgrounding of long runs (plan.md:2283).

**Evidence missing:** (none) — **Passage:** (N/A) — **Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied_with_recommendation

**Evidence present:** For R8 the plan uses every output the release build writes:
- `mapping.txt`, both as the identity set (four prefixes, and all classes) and through the closure check;
- `configuration.txt`, normalised by section;
- `seeds.txt` and `usage.txt`, restricted to the contract prefixes;
- the merged and the packaged release manifests;
- the backup descriptor resolved through `aapt2 dump resources`.

These are EV-B (plan.md:1316-1349) and CMP (plan.md:1357-1373). The tool was tested on U-068's files: 28 sections
and 28 keys, the I0 and I5 files equal after normalisation, 27 manifest classes, 482 seeds (L26, plan.md:146). Row
D98 covers an AGP that stops writing `seeds.txt` or `usage.txt`.

**Judged as an experienced Android build engineer would:** this is beyond common practice. Five cheap checks that
such an engineer would also make are missing:
1. **Which R8 ran.** `mapping.txt` begins with `# compiler: R8`, `# compiler_version: …` and `# min_api: …`. The
   plan counts the file's lines (plan.md:1330) but never records this header. At I3 the R8 version is the one fact
   that attributes any shrinking difference. `min_api: 24` re-confirms the minSdk from inside R8.
2. **Release dex size.** Only the number of dex files is recorded (`dex=`, plan.md:1329). The class, method and
   field counts of the release `classes.dex` are the standard signal for "R8 shrank or kept differently". 2.7 lists
   exactly that as "cannot show" (plan.md:453). The counts would turn part of it into "recorded" at no cost. The
   dex format version (`dex\n037`, `038`, `039`) should also be recorded: D8 and R8 choose it from `min_api`.
3. **Baseline profiles.** `androidx.profileinstaller.` is one of the contract prefixes (plan.md:558), so the app
   ships ProfileInstaller. AGP merges library baseline profiles into `assets/dexopt/baseline.prof` and
   `baseline.profm` and rewrites them against R8's mapping. The entry-list comparison catches presence or absence,
   but nothing records their content. Expect that content to change whenever R8 renames classes, so record it
   rather than gate on it.
4. **Resource shrinker.** `app/build.gradle` has no `shrinkResources` (this review: 0 hits), so there is no
   `resources.txt` and the resource shrinker plays no part. The plan should say so in one line. Otherwise a reader
   will ask why the shrinker's output is not compared, and an AGP 9.x default-shrinking change would have no stated
   check.
5. **Merged manifest.** This one is already covered: both merged manifests and both packaged manifests are compared,
   with invariant greps (plan.md:1320-1323, :1369).

**Passage:** EV-B, plan.md:1328-1333; 2.7 R8 row, plan.md:453; 2.10, plan.md:545-593.

**Recommendation / Remediation:**
- **(should)** Append to the `I<k>-apk.txt` block in EV-B:
  `grep -E '^# (compiler|compiler_version|min_api):' $MAP/mapping.txt`
  and
  `echo "mapping-classes=$(grep -cE '^[^ #].* -> .*:$' $MAP/mapping.txt)"`.
  Add to 2.9 a row "R8 `compiler_version` | = | = | changes (recorded) | = | …", and `min_api: 24` everywhere.
- **(could)** Add release-dex counts with `dexdump` from `build-tools/36.0.0` (check it is present, as was done for
  `aapt2`):
  `unzip -p app/build/outputs/apk/release/app-release-unsigned.apk classes.dex > $WORK/rel-classes.dex; "$SDK/build-tools/36.0.0/dexdump" -f $WORK/rel-classes.dex | grep -E "^(magic|class_defs_size|method_ids_size|field_ids_size)"`.
  Record the result, do not gate on it, and classify any change at I3 to I6.
- **(could)** Record `size` and `sha256` of `assets/dexopt/baseline.prof` and `baseline.profm` from the release APK
  when the entry list contains them. Record only; they change whenever R8's mapping changes.
- **(should)** Add to 2.7 or 2.10: "No `shrinkResources`: no resource-shrinker output exists; resources are covered
  by the APK entry lists and the backup-descriptor dump."

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:**
- **Ordering table** (plan.md:315-331).
- **Session plan** with per-session run counts, resume VERs and boundary reasons (plan.md:2051-2070).
- **UTD** placed after EV-B, so the tree is unchanged and the run comes before the I5 mutations (plan.md:1267).
- **Q-13 to Q-17** timed "before I3" (plan.md:338), consistent with the coordinator's statement.

**Run count (76) — proportionate.** The total is 71 runs in the increments plus five resume VERs. The cost is driven
by the per-dependency D5 cycle: WP of about 14 minutes, ENF, then G of about 10 minutes, at 11 increments. The design
and the D5 protocol bind that cycle. The extra runs since iteration 1 each buy something:
- the second DEP per increment (plan-review O-3);
- UTD at I2 and I5;
- resume VERs that prove the wrapper version.

Possible redundancy and possible gaps (could):
- **ENF looks redundant.** ENF(k) is `:app:assembleDebug` (plan.md:1266). The G(k) that follows runs the same task
  with verification enforced, so ENF adds a check only if the design's D5 step binds it as a separate run. If it is
  bound, the plan should say so; if not, about 11 runs of 2 to 3 minutes could go.
- **D75 has no detector at I8 to I11 (or I12).** D75 (plan.md:2025) covers a test-classpath change at a test-only
  increment, but no DEP runs there. Either name the detector (the I<k>-vm-added groups, VMP step 4) or add one
  `--configuration debugUnitTestRuntimeClasspath` run at each of I8 to I11.

Neither changes the verdict.

**Evidence missing:** (none) — **Passage:** (N/A) — **Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied_with_recommendation

**Evidence present:** None of the nine doctrine anti-patterns appears. The new rows are exact (D92 to D99,
plan.md:2042-2049). 4.L is concrete.

**4.L / D93 judged:**
- **Sound in design.** The command writes its own `exit=` line, so the result never depends on the tool's output.
  While a run is backgrounded nothing else starts and nothing is polled. A missing `exit=` line defines a cut-off.
  After a cut-off the developer checks for a surviving Gradle JVM, then stops. The re-run comes only in a later
  session, and only once. That protects against the one real hazard: an orphaned Gradle JVM still writing
  `verification-metadata.xml` or `app/build` after the shell was killed. VMP step 1 restores the file from HEAD
  before any re-run.
- **A wording gap.** 4.R's recovery bullet identifies a cut-off as "its log has no `exit=` line" (plan.md:1464 ff.),
  but a new session has a new scratch folder and the old raw log is gone (plan.md:1438-1440). The next session can
  only find the cut-off through the D93 entry in `implementation-log.md`.

**Evidence missing:**
1. **Ruling 58 (binding, 2026-11-21) is not in the plan.** D42 (plan.md:1992) still says a `seeds-core` difference
   from regenerated code "is still a stop: the orchestrator decides". SC-P6 (plan.md:2137) still asks for the ruling.
   2.9's R8-3 row (plan.md:524) and 2.10's R8-3 (plan.md:557-559) still say "hard stop". R20 (plan.md:2162) still
   reads "Medium / Medium (delay)" for the stop.
   - Two of the ruling's conditions are not checkable as the plan stands:
     - "nothing under … a worker constructor changes" has no grep;
     - the pattern list has no stated match rule (simple class name or substring).
   - The Hilt pattern list (`Hilt_*`, `*_Factory`, `*_HiltModule`, `*_AssistedFactory*`, `*_MembersInjector`,
     `hilt_aggregated_deps.*`) omits other classes Hilt generates: `Dagger*` components, `*_HiltComponents*`,
     `*_GeneratedInjector`, `*_ComponentTreeDeps`. A change there stops safely rather than being accepted, so the
     gap costs only an avoidable stop.
2. **The BUG-029 story is not anticipated.** The coordinator states that BUG-029 (a backup started while the app is
   off screen fails at `setForeground` on Android 16) is fixed in its own story before U-071 is built. The plan
   does not account for it anywhere:
   - The planning state (plan.md:28-38), D2 (plan.md:1952) and I0 step 1's expected commit list name U-068, U-069,
     U-070 and U-074 only.
   - The fix will most likely touch `BackupWorker.kt`, the file of mutation anchor M-1 (plan.md:635 in iteration 1;
     4.M), and may add cases to the NEW3 classes or change the `SystemForegroundService` manifest entry behind the
     `fgs=1` invariant.
   - The I0 checks would catch the drift (D5, D6, D41), but as a stop, not as an expected start state.
3. **A stale row contradicts the procedure.** 2.5's last row (plan.md:426), "One `:app:dependencies` report per DEP
   step … Two `--configuration` runs [not chosen] … Half the runs", says the opposite of iteration 2's DEP. DEP now
   runs two `--configuration` reports (plan.md:79, :1271).

**Passage:** D42 (plan.md:1992); SC-P6 (plan.md:2137); R20 (plan.md:2162); 2.9 R8-3 row (plan.md:524); 2.10 R8-3
(plan.md:557-562); D2 (plan.md:1952); planning state (plan.md:28-38); 2.5 last row (plan.md:426); 4.R cut-off bullet
(plan.md:1464 ff.).

**Recommendation / Remediation:**
- **(should) Carry ruling 58 into the plan.**
  - Rewrite D42's last sentence: "A `seeds-core` difference is accepted and recorded (ruling 58) when every changed
    line names a class matching, by simple class name, the Hilt/Dagger patterns of SC-P6 at I4 and I6, or the Kotlin
    compiler-generated member patterns at I5 only; each line is listed in the log with the pattern it matched; R8-1,
    R8-2 and R8-4 hold; and `grep -E '^com\.google\.crypto\.tink\.|^com\.zegoggles\.smssync\.(service\.BackupWorker|service\.RestoreWorker|worker\.BackupTriggerWorker): .*<init>'`
    over the changed lines finds nothing. Anything else stops."
  - Mark SC-P6 "ruled (ruling 58)".
  - Change the 2.9 R8-3 cells at I4, I5 and I6 to "= `<p>`, or accepted under ruling 58".
  - Lower R20's likelihood or impact accordingly.
  - The orchestrator confirms the exact `<init>` line form in `seeds.txt` at I0. Adding `Dagger*`,
    `*_HiltComponents*`, `*_GeneratedInjector` and `*_ComponentTreeDeps` to the list is a *could* and needs the
    orchestrator's agreement, because the ruling names "the plan's patterns".
- **(should) Add the BUG-029 story** (its id once created) to:
  - the planning state;
  - D2 (`done` required);
  - I0 step 1's expected commits and SC-9's record;
  - I0 step 13's classification of later NEW3 cases.

  Add one sentence: "if it changes `BackupWorker.kt` at or near `:332`, `oldtext_check.py` (D6) re-derives anchor
  M-1; if it changes the manifest, the invariant line's expected values are re-derived from I0 before I1, and D41
  compares against I0."
- **(should) Fix or delete the stale DEP row.** Replace 2.5's last row with: "Two DEP reports per step
  (`--configuration`, as U-060) | each report parses one configuration exactly (plan-review O-3) | one more short run
  per DEP | one plain `:app:dependencies` | its mixed output was the O-3 finding".
- **(could)** In 4.R's cut-off bullet, replace "its log has no `exit=` line" with "the implementation log records
  D93 for it (the raw log is gone with the old scratch folder)".

## Iteration 2 Overall Verdict

**Verdict:** PASS

**Rationale:** Iteration 2 takes up every iteration-1 recommendation, each at a cited line. The new material holds
up at expert depth:
- a six-level R8 comparison tested on real outputs;
- caches neutralised by command-line flags, with no file edit;
- Kotlin-daemon and Windows-lock handling;
- an up-to-date check aimed at the KSP2 recompilation history;
- AGP 9.1 to 9.5 behaviour-change sourcing;
- an ordering-constraint table;
- a sound procedure for backgrounded and cut-off runs.

Two criteria carry recommendations, within the PASS limit of two:
- **Criterion 6, R8 characterisation** (should: record `mapping.txt`'s `compiler_version` and `min_api`, and state
  that the resource shrinker plays no part; could: release-dex counts and format version via `dexdump`, and
  baseline-profile hashes).
- **Criterion 8, consistency with new binding facts** (should: carry ruling 58 into D42, SC-P6, 2.9, 2.10 and R20 in
  a checkable form; anticipate the BUG-029 story at I0; fix the stale DEP row in 2.5; could: the 4.R cut-off
  wording).

The could-items under Criterion 7 (whether ENF is redundant with G; D75's missing detector at I8 to I11) do not
affect the verdict. None of the recommendations changes the build order or a gate.

**Iteration:** 2

**Rework routing (FAIL only):** (not applicable; PASS)

---

## Iteration 1 record (superseded by iteration 2 above)

The text below is the iteration-1 report as written at 2026-11-21T14:19:35Z against plan iteration 1 (`ba8eb9d2`).
Its frontmatter values were iteration 1, PASS, recommendations_count 2, with Criteria 3 and 8
`satisfied_with_recommendation`. Its line numbers refer to plan iteration 1.

### Iteration 1 — Gate Report: expert-depth-check — U-071

#### Iteration 1 — Verdict: PASS (severity warning, observe-only), 2 recommendations

#### Iteration 1 — History

- **Iteration 1** (this report). The plan is at HEAD `ba8eb9d2` (plan iteration 1, `base_commit` `7c5e6073`). It was
  read from the committed blob (`git show HEAD:…`) because a developer is working in the checkout.

#### Iteration 1 — Summary

This plan reads like the work of a senior build engineer. Its strongest parts:
- **Assumption ledger (1).** Every claim the plan depends on is marked verified, needs_check or "plausible, not
  verified". Each needs_check has a decision row.
- **Simulated edits (L11).** Every `old_string` was simulated on the start-state blobs.
- **Wrapper analysis (L5, 2.5 row 1, 3.1).** The tracked jar's hash shows that the `wrapper` task writes the jar of
  the Gradle that runs it. So the task has to run twice, and the Windows block has to be re-applied and proven after
  each run.
- **Comparison limits (2.7).** A table states what each before/after comparison can and cannot show.
- **Expected changes (2.9).** A table states which comparison may change at which increment.
- **Measured coverage margin (L17, 2.6).** The `worker` package has zero spare lines: ⌈0.95·16⌉ = 16. The plan
  derives this from measurements, not from a guess.

Two recommendations remain:
- **Criterion 3.** Four alternatives a reviewer will ask about are not in the trade-off table:
  - a single raise of every version at once;
  - a version catalog;
  - what "additions only" verification gives up and does not protect;
  - the state of Gradle's build cache and configuration cache in the comparison runs.
- **Criterion 8.** Six specific edge cases or risks are missing (listed under Criterion 8):
  - the Kotlin compile daemon on Windows;
  - incremental builds, which no clean run exercises;
  - the AGP 9.1 to 9.4 behaviour changes;
  - JaCoCo's Kotlin filters at I12;
  - the 600 000 ms tool timeout against the plan's own run estimates;
  - an explicit table of the ordering constraints.

None of these changes the build order or a gate.

One note outside the criteria: the plan's frontmatter `timestamp` is `2026-11-21T14:30:00Z` (plan.md:8). That is
later than this review's `date -u` reading (14:19:35Z), so it was typed rather than read from the clock. It should
come from `date -u` (the plan requires the same of the developer at plan.md:1019).

This review checked, read-only:
- **The gate prompt and doctrine.** Read in full.
- **The plan.** Sections 0 to 2.9, 3.1 to 3.4, 4.0 (tools, Gradle calls, EV, CMP, CK, VMP, CM), 4.P, 4.R, I0 to I14,
  4.M, 4.9 and 5 to 14 were read in full. The embedded Python tool bodies after `gradlew_block.py` were sampled.
- **The design.** The S2 table and D3 of DES-MODERNIZATION-014 (`:554-575`, `:701-760`).
- **The committed build files.** `gradle.properties` in full. `app/build.gradle:38-115` and `:192-203`. The
  `build.gradle` plugin lines. The `-dontoptimize` line of `app/proguard-rules.pro`.
- **The research file.** The Gradle 9.9.2 row, which supports the plan's "dependency-verification fix" claim at
  plan.md:1756.

#### Iteration 1 — Findings (criterion headings below are iteration 1's)

### Criterion 1 — Pattern grounding

**Verdict:** satisfied

**Evidence present:** Section 2.0 (plan.md:125-152) names seven principles. Each is tied to a specific mechanism of
this story:
- **One change per increment, each integrated and green** (Humble/Farley ch. 1, ch. 3). Tied to `git bisect` landing
  on one bump and to newest-first revert (plan.md:129-133). The plan also states the method's known weakness: testing
  one factor at a time hides interactions. It names the one interaction that matters: Kotlin 2.4 needs Hilt 2.60, so
  I4 comes before I5.
- **Fitness functions as gates** (Ford/Parsons/Kua ch. 2). Tied to the per-package floors, the manifest invariants,
  the R8 kept-name set, the class-file majors, the KSP list and the verification proof (plan.md:134-137).
- **Characterisation tests as the oracle, never the variable** (Feathers ch. 13). Tied to "no test edited",
  Scope Concern 4 and U-066's exact-list tests (plan.md:138-140).
- **Mutation analysis** (DeMillo/Lipton/Sayward 1978). Tied to U-067's six mutants under Kotlin 2.4
  (plan.md:141-143, 4.M).
- **Write-ahead logging** (ARIES, ACM TODS 1992). Tied to the `APPLYING` / `restored` lines that 4.R counts
  (plan.md:144-146, :1044).
- **Monotone ratchet for supply-chain pinning.** Tied to the additions-only proof (plan.md:147-149).
- **An evidence hierarchy for compatibility.** Primary sources bind and secondary sources check (plan.md:150-152).

None of these is name-dropping. Each one drives a step, a tool or a decision row.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied

**Evidence present:** The plan finds these anti-patterns in the current state and in the planned change, each from
explicit analysis:
- **Hand edits to a generated file.** The hand-maintained block in the generated `gradlew` is rated "Certain if
  forgotten / High" (R6, plan.md:1652) and is proven byte for byte (3.1, `gradlew_block.py`). The U-060 bootstrap was
  rejected because it is "a hand edit of a generated file" (2.5 row 1, plan.md:271).
- **Stale build outputs measured as if current.** The U-060 lesson of a stale Kotlin directory. This is the reason
  for `clean` in every gate (2.5 row 2, plan.md:272) and for the class-root check at I3 (D55).
- **Unevidenced causal claims.** "JaCoCo counting" may not be given as a cause without per-class and per-line
  evidence (plan.md:295-297).
- **A brittle gate.** A 0.95 floor over 16 lines tolerates no lost line. The plan corrects U-069's rationale on this
  (H-9, plan.md:1614-1616).
- **Noisy metrics treated as signal.** The `activity` coverage alternates between 199 and 189 and is declared known
  noise (plan.md:292).
- **Tag-pinned CI actions.** SHA pinning is recommended and kept out of scope (H-8, plan.md:1612-1613).
- **Stale documentation.** `DEVELOPMENT.md:49` still says "pins JDK 17" (L22, H-8).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied_with_recommendation

**Evidence present:**
- **Section 2.5 (plan.md:267-279).** Nine decisions, each with all four elements: gained, gives way, not chosen, and
  why.
  - The wrapper row (plan.md:271) is exemplary. It rejects running the task once because L5 shows that one run leaves
    the 9.5.1 jar, and it rejects the U-060 bootstrap because AC-6 (a) forbids hand edits.
  - The `clean`-per-gate row states its cost (about 3 minutes per gate) and the alternative it rejects.
  - The strict `configuration.txt` row admits that the plan will probably stop at I3 to I6, and turns that into
    ruling request SC-P1.
- **One change per increment (plan.md:129-133).** The plan gives this its gain (bisect and revert) and its weakness
  (hidden interactions).

**Evidence missing:** These alternatives are not set out as trade-offs:
- **A single raise as the rejected alternative.** It is not a row. Its costs are not stated: 14 commits and about
  63 runs, plus intermediate states that nobody ships. Gradle 9.9.2 with AGP 9.0.1, and AGP 9.5.2 with Kotlin 2.3.21,
  are themselves pairings the vendors have not jointly tested.
- **Version catalogs.** There is no mention of `gradle/libs.versions.toml` anywhere in the plan. A reviewer will ask
  why a story that touches every version line does not move them into a catalog.
- **Additions-only versus wholesale regeneration of the verification file.** The plan forbids wholesale regeneration
  (plan.md:1008, constraint 9) but does not state what the ratchet gives up, or what checksum-only verification does
  not protect:
  - `verify-signatures` is `false` (L8), so the writer records whatever the repository serves during the pass. That
    is trust on first use: it protects against later substitution, not against a bad first fetch.
  - The two `<trust … regex="true"/>` lines exempt whatever they match.
  - The `android-all` `.sha512` files come from the same server as the jars, so they give integrity in transit only.

  Section 2.7 (plan.md:311) gets close but names none of these mechanisms.
- **Build cache and configuration cache.** Nothing in the plan states their state. The committed `gradle.properties`
  sets neither `org.gradle.caching` nor `org.gradle.configuration-cache`, so both are off by default. But
  `.claude/CLAUDE.md` warns that a user-global `gradle.properties` overrides the project's. `G(k)` (plan.md:906)
  passes neither `--no-build-cache` nor `--no-configuration-cache`; only `KC(k)` passes `--no-build-cache`
  (plan.md:910). `clean` does not stop `FROM-CACHE` outputs if a user-global file has turned the build cache on.

**Passage:** Section 2.5 (plan.md:267-279); section 2.7, verification row (plan.md:311); 4.0, Gradle calls
(plan.md:905-914).

**Recommendation / Remediation:** Add four rows to 2.5:
1. **Increments, not one raise.**
   - Gained: every failure has one suspect, and the verification additions can be attributed to a group.
   - Gives way: 14 commits, about 63 runs, six sessions, and two intermediate pairings outside the joint vendor
     tables (each guarded by D26 and D35).
   - Not chosen: one raise of all versions.
   - Why: with twelve suspects per failure, bisect is useless and the D5 group review cannot attribute an artifact.
2. **Versions stay inline; no version catalog.**
   - Gained: the exact-text edits stay valid (L11, `oldtext_check.py`), and the AC-16 sweep and AC-23's file set apply
     unchanged.
   - Gives way: version tokens stay spread across two build files and their comments.
   - Not chosen: `gradle/libs.versions.toml`.
   - Why: moving to a catalog is a refactoring with its own characterisation (resolved classpaths equal, before and
     after). It would be a second change in every increment. It belongs after S4.
3. **Additions-only verification.**
   - Gives way: 9.0.1-era AGP and 2.3.x Kotlin entries stay in the file. The file grows by several hundred entries,
     and a later clean-up needs its own story.
   - Does not protect: checksum-only mode is trust on first use; the two `<trust regex>` lines exempt what they match;
     same-origin `.sha512` files give integrity in transit only.
   - Not chosen: wholesale regeneration.
   - Why: it would make a removed or altered checksum invisible to the set proof, and that proof is what detects
     tampering (INT-001).
4. **Caches off for every comparison run.**
   - Add `--no-build-cache --no-configuration-cache` to `G(k)`, `WP(k)` and `ENF(k)`. These are command-line flags;
     no file is edited.
   - Add one I0 line that records the effective state without printing a path, for example
     `./gradlew --console=plain help --info | grep -iE 'build cache|configuration cache' | head`.
   - Gained: no comparison depends on a developer's user-global settings.
   - Gives way: nothing, since both caches are already off at project level.

### Criterion 4 — Citation depth

**Verdict:** satisfied

**Evidence present:**
- **Literature.** Every citation is anchored: Humble/Farley ch. 1 and ch. 3, Ford/Parsons/Kua ch. 2, Feathers ch. 13,
  DeMillo et al. *IEEE Computer* 1978, and Mohan et al. *ACM TODS* 1992. The plan says plainly that these were not
  re-read in the session (plan.md:127).
- **Vendor sources.** Each version row cites the exact Maven Central or Google Maven metadata path and a versioned
  release-notes URL (2.1, plan.md:161-181).
- **Compatibility claims.** Each points to a research row (C1 to C21). Where the research only paraphrases, the plan
  asks for a verbatim quotation at a versioned URL (Q-1 to Q-12, plan.md:227-240), naming the reason for each.
- **The research triple.** The plan explains why "the newest triple inside every published range" is wrong, citing
  the Gradle 9.7 page as read for U-060 (plan.md:197-200). That is a correction with a source, not an appeal to
  authority.
- **Gradle's verification chapter.** Cited by chapter name, "Verifying dependencies" (plan.md:147).
- **The 9.9.2 "dependency-verification fix" (plan.md:1756).** This review traced it to the research's Gradle row,
  which quotes the 9.9.2 release notes ("Transitive snapshot dependencies skip dependency verification").

On the question of what still depends on the pending quotations, the plan answers it per item:
- Q-1 and Q-2 are needed for I1 (rows D18 and D22).
- Q-3 to Q-8 are needed for the compatibility record (row D13). D13 stops before I1 if a constraint no longer holds,
  for example AGP 9.5's minimum Gradle being above 9.9.2.
- Q-9 is needed at I8 (D76) and Q-10 at I13 (D83).
- P1 is not relied on (plan.md:116).

So no decision rests on an unsupplied quote without a stop row.

**Evidence missing:** (none at a level that affects the verdict). The supply-chain mechanisms Criterion 3 asks for
should be cited when they are added: Gradle user guide, "Verifying dependencies", the sections on trusting artifacts
and on verifying signatures.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** Section 12 (plan.md:1747-1758) evaluates all eight ISO/IEC 25010 characteristics by name, each
with something specific to this story:
- **Functional suitability:** the case list equal at every increment, the F-1 rule, six mutations, the exact-list TLS
  tests, and the CNTR-005 and BUG-016 manifest invariants.
- **Performance efficiency:** split into runtime (R8 input and kept names) and build (63 runs, `android-all`
  downloads, the size of the verification file, the SDK 29 sandbox).
- **Compatibility:** G1 to G6 with their detectors; IC-4 shows the build installs over the configured app.
- **Usability:** not applicable, with the reason.
- **Reliability:** revert newest-first, write-ahead lines, the resume protocol.
- **Security:** the additions-only proof, the pinned distribution, the wrapper jar compared with its published hash,
  `android-all` checked against SHA-512, and evidence hygiene.
- **Maintainability:** one version per commit; the compatibility record separates an expected warning from a defect.
- **Portability:** JDK 25 kept, the Windows block proven, bytecode targets 17 and 8 unchanged. IC-6 is explicitly not
  claimed.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:** The plan's conclusions rest on deterministic signals read in the session:
- **Unchanged build files (L1).** Blob hashes at `c8e98a7f` and at HEAD.
- **Line endings (L2).** `git ls-files --eol`.
- **The Windows block (L4).** Its SHA-256 and line range.
- **The jar (L5).** Its hash, matching the official 9.5.1 value. This is the basis of the run-twice decision.
- **Verification counts (L8).** 803 components, 1460 artifacts, 3 trust lines.
- **The edit simulation (L11).** It yields numstat, hunk and grep predictions for every increment, and the I14
  comment-only check: 34 changed lines, each containing `//`.
- **Local caches (L12, L13).** Which artifacts must be downloaded, and the cached POM and AAR metadata.
- **Coverage margins (L14, L15, L17).** From U-068's measured reports, through `cov_floors.py` with exact fractions.

The signals are interpreted, not just listed:
- L17's zero spare lines becomes rows D38 and D60 and correction H-9.
- L5 becomes the decision to run the `wrapper` task twice.
- L12 says which increments download through the build's own path.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:**
- **Increment order.** The order is bound by the story, which takes it from the design's S2 table and its order
  rationale. The plan follows it and encodes every dependency the design names:
  - I1 before I3: I1 step 9 says "AGP is still 9.0.1 (C2: Gradle 9.9 tests AGP 9.0)".
  - KSP may move into I5: the pre-specified reorder D54 (plan.md:1187-1188, :1529), with I5 applying E2 "if I2 moved"
    (plan.md:1224).
  - I4 before I5: the one-way coupling between Hilt and Kotlin (plan.md:132-133).
  - JaCoCo last among the build changes, compared with I11 (plan.md:1294).
  - The N-6 pause into U-072 (D74).
- **Within each increment.** A fixed order: VMP, then G, then EV-T, EV-B, CMP, CK, then CM.
- **What must not overlap.** No gate, commit or count runs while a probe or mutation is in place (plan.md:1030,
  :1408). No two Gradle runs run at once (plan.md:905).
- **What may overlap.** Q-10 may arrive during the build, before I13 (plan.md:224-225).
- **Sessions (section 7, plan.md:1568-1586).** Each session boundary has its own reason. Session C, for example, "must
  not end with an `APPLYING` entry". The resume protocol 4.R (plan.md:1035-1054) states its expected values and
  recovery steps.

The plan restates only one of the ordering constraints in its own text and relies on the design for the rest.
Criterion 8, item 6 asks for the full table. That gap concerns reading the plan on its own; the order itself is
correct.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied_with_recommendation

**Evidence present:** None of the nine doctrine anti-patterns appears:
- **No generic recommendation.** Every edit is given as exact text, and every check has an expected value.
- **No maximalist hedging.** Uncertainty sits in the ledger as needs_check or "plausible, not verified" items, each
  with a row.
- **No out-of-scope deferral without a reason.** Section 9.4 gives the scope, and H-8 hands follow-ups on.
- **No tooling substitution.** Each measurement drives a decision.

Edge-case coverage is broad and specific (2.8, plan.md:316-345):
- the generator changing the `gradlew` template;
- the wrapper dropping `distributionSha256Sum`;
- `updateDaemonJvm` under 9.9.2;
- the verification file being re-serialised;
- `lintVitalRelease` failing inside `assembleRelease`;
- AGP moving the Kotlin class directory;
- the A13 DSL names;
- the Risk 9 `@param:` targets;
- `android-all` downloads;
- `minCompileSdk` above 35;
- test-classpath drift of S4 libraries;
- YAML 1.1 reading `on:` as a boolean;
- mixed line endings in `ci.yml`.

Other parts of the brief are covered too:
- **Daemon criteria file across the wrapper change.** D24 and the Daemon JVM line in VER.
- **Gradle daemons.** L23 notes `org.gradle.daemon=false`.
- **Stale outputs.** `clean` in every gate run.
- **Kotlin warnings.** Recorded as debt by diffing `w:` lines between I4 and I5. That is correct, because the build
  sets no `allWarningsAsErrors` (`app/build.gradle:106-109`).
- **javac `-Werror`.** D34 and SC-P4.
- **KSP2-only behaviour.** E14.3.
- **Hilt aggregation and transform.** `hiltJavaCompileDebug` and `transformDebugClassesWithAsm` at I4.
- **R8.** `configuration.txt` compared strictly, the kept-name set, `-dontoptimize`, and a "cannot show" cell for
  non-kept code.
- **Robolectric.** The legacy looper and Conscrypt modes (A14), `android-all` against SHA-512, and the provider
  probe.
- **JaCoCo.** Isolated at I12, with the class inventory and per-class lines.

**Evidence missing:** Six points a senior Android build engineer would raise:
1. **The Kotlin compile daemon on Windows.**
   - `org.gradle.daemon=false` (L23) stops only Gradle daemons. KGP runs the Kotlin compiler in its own daemon by
     default, and that daemon outlives the Gradle run.
   - At I5 a 2.3.21 Kotlin daemon can stay alive beside the 2.4.21 one. During the mutation runs the daemon persists
     between targeted runs.
   - On Windows it can hold file locks under `app/build`, so `clean` or `rm -rf $R` can fail with "Unable to delete
     file".
   - The JVM checks at plan.md:1076 and :1045 use `jps -l | grep -i gradle`, which does not match
     `org.jetbrains.kotlin.daemon.KotlinCompileDaemon`. Nothing says what to do when a delete fails.
2. **Incremental builds are never exercised.**
   - Every `G(k)` runs `clean`, so up-to-date checks and incremental KSP and Kotlin compilation are never run on the
     new toolchain.
   - This matters here in particular: the comment E14.3 replaces records that KSP 2.3.6 was chosen for "the Windows
     KSP2 recompilation fix" (plan.md:506).
   - Section 2.7 has no "cannot show" cell for incremental or up-to-date correctness.
3. **AGP 9.1 to 9.4 behaviour changes.**
   - Q-4 asks only for AGP 9.5's minimums and API statement (plan.md:232). The raise from 9.0.1 to 9.5.2 crosses four
     intermediate minors. Their "Behavior changes" sections are where changed defaults are announced: `buildFeatures`
     defaults, `android.*` property defaults, lint, unit-test options.
   - Section 2.9 lets the I3 APK-entry and coverage cells differ only if "explained by AGP only", but names no source
     for the explanation.
   - The plan also does not say that the build sets `buildFeatures { buildConfig true }`, `android.nonTransitiveRClass`
     and `android.nonFinalResIds` explicitly. So only defaults the build does not set can move.
4. **JaCoCo's Kotlin filters at I12.**
   - Section 2.9 requires each package's `t` at I12 to equal I11 (plan.md:361), and D40 stops otherwise.
   - JaCoCo releases often add or change Kotlin bytecode filters, which changes `t` while the class inventory stays
     equal.
   - The risk table (plan.md:1645-1664) has no row for this. The plan also does not say whether a change in `t` with
     an equal inventory counts as AC-14 (which names the inventory) or as a stop.
5. **The tool timeout against the plan's own estimates.**
   - Constraint 1 sets every Gradle call to the 600 000 ms (10-minute) tool timeout (plan.md:1713). Section 7
     estimates G at "about 9 to 11 min" and WP at "12 to 16 min" (plan.md:1583).
   - So every writer pass, and some gate runs, will outlast the foreground timeout. The first run on a new Gradle
     version is longer still, because Gradle's per-version transform and buildscript caches start cold (G(1), and
     WP(2) after the AGP download).
   - The plan relies on the harness moving the run to the background (2.8, D53). It has no row for the other outcome,
     where the run is killed at the timeout. In that case 4.R's rule ("an interrupted writer pass is … run again from
     the start", plan.md:1051-1052) would repeat a pass that can never finish in time.
6. **The ordering constraints are not tabulated in the plan.** The brief asks which of Gradle, AGP, Kotlin, KSP and
   Hilt constrains which, and which decisions depend on the quotations still to be supplied. The plan answers this
   item by item (Q-items mapped to rows), but never in one place. A reader of plan.md alone cannot see why I2 comes
   before I3: the design's reason, that "an older KSP under a newer AGP is the riskier pairing", is not restated.

**Passage:** Section 2.7 (plan.md:299-314); section 2.8 (plan.md:316-345); Q-4 (plan.md:232); section 2.9, I12
column (plan.md:361); section 4.0, Gradle calls (plan.md:905-914); I0 step 1 (plan.md:1076); 4.R (plan.md:1045,
:1051-1052); section 7 estimates (plan.md:1583); section 9.3 (plan.md:1643-1664); constraint 1 (plan.md:1713).

**Recommendation / Remediation:** Add these exact texts:
1. **Kotlin daemon.**
   - In I0 step 1 and 4.R, change `"$JDKBIN/jps.exe" -l | grep -i gradle` to
     `"$JDKBIN/jps.exe" -l | grep -iE 'gradle|KotlinCompileDaemon'`. Record each Kotlin daemon (its class name only)
     and do not stop for it.
   - Add a row: "D92 — `clean` or `rm -rf $R` fails with 'Unable to delete file' or a file in use under `app/build`
     (Windows lock, typically a Kotlin compile daemon of the previous KGP): record verbatim; stop and report. Never
     kill a process, never delete by other means, never retry in a loop."
   - Add the matching edge case to 2.8.
2. **Incremental check.** After G(2) and after G(5), add one run:
   `./gradlew --console=plain :app:assembleDebug > $WORK/logs/I<k>-uptodate.log 2>&1` (no `clean`). Expected:
   `> Task :app:kspDebugKotlin UP-TO-DATE` and `> Task :app:compileDebugKotlin UP-TO-DATE`, sanitised to
   `$E/I<k>-uptodate.txt`. If either task re-runs, record it as debt; it is not a stop. Add the 2.7 row "Clean gate
   runs | the full build from scratch | incremental or up-to-date correctness (I2 and I5 check it once)". This adds
   two runs of about 2 minutes each.
3. **AGP behaviour changes.**
   - Add Q-4b: "verbatim, the 'Behavior changes' entries and every 'default value changed' entry of the AGP 9.1, 9.2,
     9.3, 9.4 and 9.5 release notes; needed by I3, to source each 'explained by AGP' classification".
   - Add a 2.8 bullet: "Defaults the build sets explicitly (`buildFeatures.buildConfig true`,
     `android.nonTransitiveRClass`, `android.nonFinalResIds`, `minifyEnabled`, the lint block) cannot move; any I3
     difference is classified against Q-4b's entries, and an entry Q-4b does not explain stops (D44)."
4. **JaCoCo filters.**
   - Add risk "R19 — JaCoCo 0.8.16's Kotlin filters change a package's `t` at I12 | Medium / Low (delay) | per-class
     lines against I11, C17's change history | D40".
   - In 2.9's I12 cell, state the reading: "class inventory = I11 (AC-14); a `t` change with an equal inventory is
     recorded with the per-class lines and the JaCoCo change-history entry, and stops only if a package goes `BELOW`
     (D38)". Or, if the stricter reading is intended, list it as SC-P6.
5. **Timeout.**
   - Add to 7: "WP (12 to 16 min) and the first G on each new Gradle version exceed the 600 000 ms foreground timeout;
     they are expected to be moved to the background (D53)."
   - Add a row: "D93 — a Gradle run is terminated at the tool timeout rather than moved to the background: stop and
     report; the run is not repeated."
   - Add a matching exception to 4.R: "An interrupted writer pass is re-run once; a second interruption is D93."
6. **Ordering-constraint table.** In 2.2, add a table with the columns *edge*, *source*, *increment order it
   decides*, *if the quote does not confirm it*:

   | Edge | Source | Order it decides | If the quote does not confirm it |
   |------|--------|------------------|----------------------------------|
   | AGP 9.5 needs Gradle ≥ 9.6.0 | C3, Q-4 | I1 before I3 | D13 stop before I1 |
   | Gradle 9.9 tests AGP 9.0 | C2, Q-3 | I1 can build on AGP 9.0.1 | D26 detects it |
   | KSP is independent of the Kotlin version since 2.3.0 | C9, Q-6 | I2 before I5 | D54 reorder |
   | An older KSP under a newer AGP is the riskier pairing | the design's S2 table | I2 before I3 | — |
   | Hilt 2.60 is built with Kotlin 2.3.21 | C11, Q-7 | I4 before I5 | D59, D35 |
   | Kotlin 2.4 metadata needs Hilt 2.60 | C10 to C13, Q-7 | I5 after I4 | F4 |
   | No test runs below SDK 24 | S1 (done) | I7 after S1 | — |

   Follow it with: "Every edge has a build-time detector, so the pending quotations change the compatibility record
   and the D13 stop, never the order."

#### Iteration 1 — Overall Verdict (superseded)

**Verdict:** PASS

**Rationale:** The plan is grounded and specific throughout:
- seven principles, each applied to a concrete step;
- a 25-row verified ledger, plus needs_check items each with a decision row;
- every edit simulated on the start-state blobs;
- a wrapper procedure derived from the tracked jar's hash;
- an explicit table of what each comparison can and cannot show;
- coverage margins taken from measurements;
- a 91-row decision table.

The upgrade order follows the design's constraints, and no ordering decision rests on an unsupplied quotation
without a stop row. Two criteria carry recommendations:
- **Criterion 3.** The trade-off table lacks four alternatives: a single raise, a version catalog, what checksum-only
  additions-only verification gives up, and the state of the build and configuration caches.
- **Criterion 8.** Six edge cases or risks are missing: the Kotlin compile daemon's Windows file locks, the untested
  incremental path, the AGP 9.1 to 9.4 behaviour changes, JaCoCo's Kotlin filters at I12, the 600 000 ms timeout
  against WP's 12 to 16 minutes, and a table of the ordering constraints.

Each recommendation gives the exact text to add. None changes the build order, a gate or a file outside the story's
allowed set. The count of recommendations (2) is within the PASS limit.

**Iteration:** 1

**Rework routing (FAIL only):** (not applicable; PASS)
