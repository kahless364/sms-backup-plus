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
iteration: 1
timestamp: "2026-11-21T14:19:35Z"
produced_at: 2026-11-21T14:19:35Z
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

# Gate Report: expert-depth-check — U-071

## Gate: expert-depth-check @ plan-gate

## Verdict: PASS (severity warning, observe-only), 2 recommendations

## History

- **Iteration 1** (this report). The plan is at HEAD `ba8eb9d2` (plan iteration 1, `base_commit` `7c5e6073`). It was
  read from the committed blob (`git show HEAD:…`) because a developer is working in the checkout.

## Summary

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

## Findings

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

## Overall Verdict

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
