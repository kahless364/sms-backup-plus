---
artifact_type: gate-result
story_id: "U-071"
gate: plan-review
gate_id: "plan-review"
checkpoint: plan-gate
verdict: FAIL
iteration: 1
agent: "Solution Architect (separate instance, plan-review)"
timestamp: "2026-11-21T14:32:47Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md"
base_commit: "9aa6b76c5752c4d9641392e3a91d08102c13f777"
blockers: 5
observations: 11
---

# Gate Report: gate-result — U-071

## Gate: plan-review @ plan-gate

## Verdict: FAIL

## Summary

I reviewed iteration 1 of the plan (1,846 lines) against the story, design DES-MODERNIZATION-014 (S2 table, D1, D3, D5, D7, N-6, D19), U-067's mutation log, U-060's proof script, the research, and the orchestrator's supplement. The supplement was committed during the review (`c9b3954d`). I did not take the plan's numbers on trust.

**Base.** HEAD moved twice during the review. It went to `c9b3954d` (the supplement) and then to `9aa6b76c` (two bug-story documents). `git diff --quiet c9b3954d 9aa6b76c` gives 0 over the build files, `app/src/main`, `plan.md` and the research folder. Since the planning HEAD `7c5e6073`, the build paths are unchanged. The only drift is `code-location.md` (`@@ -28 +28 @@`, U-068 I7), which is what the plan predicts. U-069 had not yet changed `app/build.gradle` in the working tree (`git diff HEAD --stat` was empty), so I stood in for its two lines. Line `:579` became a new note, and line `:586` became `minimum = 0.90 // MEASURED …` (ruling 47).

**What I re-derived, all by script on `git show HEAD:` copies in my scratch folder:**
- **Old texts.** I applied every edit of section 3 in increment order, I2 to I14: 39 edits plus the E7.3 anchor. Each old text occurs exactly once at its moment, and `cache@v4` twice. That includes the comment edits of I14 on lines already changed by E7.1, E8 and E12.
- **Line counts.** Every numstat the plan states is right:
  - `build.gradle 1 1` at I2 to I5;
  - `app/build.gradle` `4 4` (I4), `1 1` (I5), `2 2` (I6), `4 4` (I7) and `1 1` (I8 to I12);
  - `ci.yml 8 7`, with exactly the six hunks L11 lists;
  - at I14, `9 9`, `8 8` and `3 3`, with 34 changed lines that all contain `//`.
- **Versions.** All 19 strings equal the design's D1 rows (`:364-383`) and the story's table character for character. The five Hilt lines are identical. Stdlib equals the plugin. JaCoCo is 0.8.16, and the I4 check gives `1` and `4`.
- **End state.**
  - The AC-16 sweep finds nothing in the build files.
  - The AC-22 (b) grep finds only the four lines of L10.
  - The AC-22 (a) grep gives `1`, `4`, `14`, `5`.
  - The (vii), (viii) and (ix) final checks give the expected empty, `35/24/35/1602` and empty results.
  - The sweep at HEAD equals L9.
- **CI YAML.** The edited `ci.yml` parses with PyYAML 6.0.2 and prints exactly the I13 step 5 values (`steps: 7`).
- **Wrapper block.** The `gradlew` block is at `:117-127` (LF, 100755, `core.filemode=false`) and its SHA-256 is `45364f3d…ea760`. I extracted `gradlew_block.py` verbatim from the plan. It passes on a synthetic generated copy with E1 applied: the result is byte-equal to HEAD's `gradlew`, with numstat `12 0`. It fails when one byte elsewhere is changed.
- **Mutations.** The six texts and the expected failing sets equal `sprint-023/U-067/evidence/mutation-log.md:18-75`.
- **`vm_proof.py`.** Its lines marked `# U-060` equal U-060 `plan.md:654-667`.
- **`cov_floors.py`.** Its regex reads 18 rules, and on the U-069 stand-in `worker` reads 0.90.

**Result.** The design conformance, the edit texts, the counts and the D5 protocol are sound. There are five blockers:
- one commit list names a file no step writes (the U-068 B-4 pattern again);
- the CM(1) staging check will most likely fail, because the 9.9.x Unix script template is unchanged;
- three binding rulings that postdate the plan are not carried, and each would make the developer stop where the ruling says continue, or write mislabelled evidence: SC-P1, ruling 47, and the supplement's form.

## Findings

### Blockers

**B-1: `I1-gradle-version.txt` is in the I1 commit list, but no step writes it.**
- **Where:** `plan.md:1436` (I1 list: `$W/evidence/I1-{wrapper-run1.txt,gradle-version-run1.txt,wrapper-run2.txt,gradle-version.txt,wrapper.txt}`) and `plan.md:1157` (I1 step 7: `VER(I1-gradle-version)`).
- **What:** VER writes only `$WORK/logs/<name>.log`. Steps 2, 4 and 5 each name their sanitised `$E/…` file (`:1141`, `:1150-1151`, `:1152`). Step 7 does not, unlike I0 step 6 (`:1114`) and I14 step 2 (`:1332`). So CM(1) step 2 (`ls`) fails, and row D87 stops the run.
- **Fix:** in `plan.md:1157`, after "no loopback error", add: "Sanitised (whole file) to `$E/I1-gradle-version.txt`."

**B-2: CM(1) will most likely fail because `gradlew` stages nothing.**
- **Where:** `plan.md:1015` (CM step 5, staged list must equal the list), `plan.md:1436` (I1 list contains `gradlew`) and `plan.md:1170-1174` (I1 step 8, "the `gradlew` numstat recorded (template changes plus `12` block lines …)").
- **What I found:** I compared the start-script templates inside the cached distributions, read-only.
  - `org/gradle/api/internal/plugins/unixStartScript.txt` in `gradle-plugins-application-9.5.1.jar` and `-9.9.1.jar` is byte-identical: both 11385 bytes, SHA-256 prefix `81a2c1ec4cad61c1`.
  - The Windows template differs (`8e9b1e00…` and `c9184040…`).
- **What it means:** 9.9.2 is a patch of 9.9. After W2 and E1, `gradlew` will then be byte-equal to HEAD's blob. That is the same thing I showed on the synthetic copy. `git add -- gradlew` stages nothing, so `I1-staged.txt` lacks `gradlew`, and `staged-equal-exit` is 1. No row covers this, so constraint 14 stops the run.
  - `gradlew.bat` will change.
  - I1 step 3 already expects equality after W1, for the same reason.
- **Fix:** do both of the following.
  1. In I1 step 6 (`:1153-1156`), add: `git diff --quiet HEAD -- gradlew; echo "gradlew-unchanged-exit=$?"`. `0` is the expected case when the 9.9.2 template equals 9.5.1's. The block proof is then the AC-6 (b) evidence, and the log records "the 9.9.2 generator writes the same `gradlew` as 9.5.1; block present, file equal to `<start>`".
  2. In CM step 5 (`:1015`), build the expected list from the listed files that differ from HEAD or are untracked: `for f in <list>; do git diff --quiet HEAD -- "$f" 2>/dev/null && git ls-files --error-unmatch -- "$f" >/dev/null 2>&1 || echo "$f"; done | LC_ALL=C sort`. Compare that with the staged list, and record each listed file that was unchanged. In the I1 row of 4.9 (`:1436`), write "`gradlew` (only if changed, CM step 5)". In I1 step 8 (`:1172-1173`), replace the parenthesis with "(expected empty if the 9.9.2 template is unchanged; otherwise template changes only; the block proof is the arbiter)".

**B-3: ruling SC-P1 on `configuration.txt` is not carried, so the plan still stops on every difference.**
- **The ruling** (story `:367`, Record 19): a difference is accepted and recorded when two things hold:
  - the kept-name set equals the previous increment's;
  - every changed rule comes from the raised component's own shipped rules, with the source of each named.

  Any other difference stops the run.
- **Where the plan contradicts it:**
  - D43 (`plan.md:1518`): "Stop and report … (strict reading; SC-P1 may give a standing ruling)".
  - The 2.9 row (`:355`), plus CMP's expected result (`:983-987`): "every `exit=0` except the cells 2.9 allows". CMP compares every file against `I0` as well as `<p>` (`:971`). So a difference accepted at I3 reappears as `configuration.txt vs I0 exit=1` at every later increment, and the plan stops again each time.
  - Other stale passages: the 2.5 row (`:275`), N7 (`:100`), the SC-P1 row of 9.2 (`:1637`), R2 (`:1648`), H-2 (`:1591-1592`) and session B (`:1577`, "likely SC-P1 stop").
- **Fix:**
  - D43: "`configuration.txt` differs from `<p>`. Accept and record, under ruling SC-P1 (Record 19), when `kept-names.txt vs <p>` is `exit=0` and every added or removed rule lies in a section whose source file belongs to this increment's raised component. That is AGP's default file at I3, or the shipped consumer rules of that library or plugin at I4 to I6. Each rule and its source section go into the log. Any other difference stops."
  - 2.9 `:355`: put "= `<p>`, or accepted under SC-P1" in every column.
  - CMP expected (`:983-987`): add "`configuration.txt vs I0` may differ by the differences already accepted under SC-P1 at earlier increments, listed in the log. Its comparison against `<p>` decides."
  - Mark the 9.2 row (`:1637`) "Ruled (Record 19 ruling 48)", and reword R2, H-2 and session B to match.

**B-4: ruling 47 (`worker` floor 0.90) is not carried, so D60 stops a passing gate.**
- **Where:** D60 (`plan.md:1535`): "`worker` below 0.95 … Stop … (L17: zero spare lines)". The 2.6 table (`:288`: floor 0.95, spare **0**, "any single line fails the gate").
- **What:** with 16 lines at 0.90, `⌈0.90·16⌉ = 15`, so there is one spare line (story `:370`). At 15/16 = 0.9375, the gate and `cov_floors.py` both pass, because the floor is read from the file. D60 still orders a stop. The plan gives two contradictory stop rules for the same package.
- **Stale passages:** L17 (`:85`), H-7 (`:1606`), H-9 (`:1614-1616`), R3 (`:1649`) and the Verdict bullet (`:1831`).
- **Fix:**
  - D60: "`worker` below its configured floor (0.90 after U-069, ruling 47) → D38. A covered-count drop of one line within the floor (16/16 to 15/16) is recorded and explained from the per-class lines; it is not a stop."
  - 2.6 row: "`worker` (0.90, U-069, ruling 47) | 16/16 | **1** | … one more uncovered line fails the gate".
  - L17: "0.90 (ruling 47; U-069 plan iteration 3 wrote 0.95, corrected)".
  - Delete H-9, which ruling 47 has settled. Change "zero spare lines" to "one spare line" in H-7, R3 and the Verdict.

**B-5: the supplement's form (ruling of 2026-11-21) is not carried, so the plan still demands verbatim quotations.**
- **The ruling:** the supplement gives table values and checksums exactly, but prose only as "the stated fact, the address and the fetch date" (`2026-11-21-u071-quote-supplement.md:7-15`, story `:371`).
- **Where the plan contradicts it:**
  - 2.2 (`plan.md:190-191`: "quoting verbatim and never paraphrasing"; `:201-206`: "the orchestrator's verbatim quotation from the supplement (Q-3 to Q-9)").
  - 2.3 (`:224`, and the Q-3 to Q-10 "verbatim …" cells at `:231-238`).
  - D1 (`:1476`: stop if `$SUPP` lacks any of Q-1 to Q-9). Q-3, Q-5, Q-6, Q-7 and Q-9 are summaries.
- **What happens:** a strict developer reading either stops at D1, or writes a `compatibility-record.md` that labels summaries "verbatim". That is wrong AC-3 evidence.
- **Fix:**
  - 2.2 `:190-191`: "quoting table values and checksums exactly and prose as the supplement states it (orchestrator ruling of 2026-11-21: fact, address, fetch date), each labelled as such; nothing paraphrased beyond the supplement's own wording".
  - In the quotation table (`:201-206`), replace "verbatim quotation" with "the supplement's item (exact for tables and checksums; stated fact for prose)".
  - D1: "`$SUPP` missing, or lacking an item for any of Q-1 to Q-9 (an item in the supplement's ruled form counts)".
  - Also record these notes in the D1a table and the record:
    - Q-4's statement that no Android page names 9.5.2 (G5 holds);
    - Q-9's finding that the Mockito notes are silent and Byte Buddy's notes carry the Java 25 statement at 1.17.5. That satisfies D76's "supported"; I8 (`:1286`) should quote that item.
    - Q-12 now confirms P1; mark P1 (`:116`) "verified (Q-12)".

### Observations (non-blocking)

**O-1 (medium): 4.R compares HEAD, not this story's newest commit.**
- **Where:** 4.R expected result (`plan.md:1047`: "HEAD = the last commit the log names"), with D90.
- **What:** the orchestrator commits documents between sessions. It did so twice during this review. The check then differs and D90 says stop, which contradicts constraint 13 (`:1742-1743`).
- **Fix:** "the newest `U-071 S2-` commit (from the third command) equals the last commit the log names, and `git log --format=%h $LAST..HEAD -- <the story's paths>` is empty; other commits are recorded".

**O-2 (medium): a new session has no work folder, and the plan does not say what is lost.**
- **Where:** 4.R (`plan.md:1037`) and 4.0 (`WORK=… session scratch folder`, `:553`). Each session's scratch folder is new, and `mkdir -p "$WORK/logs" "$WORK/pristine"` exists only in I0 step 2 (`:1088`). The resume call's redirect, and every later `> $WORK/logs/…`, fails with "No such file or directory".
- **Fix:** begin 4.R with `mkdir -p "$WORK/logs" "$WORK/pristine"`, and say what is lost.
  - Only the raw logs, copies and messages of earlier sessions are lost.
  - No later step reads them. I checked CMP, the I5 `w:` comparison, the I12 inventory, the DEP comparisons and the I14 end proof: all read `$E/` files or `git show`.
  - `$WORK/gradlew-start` and `$WORK/gradlew-block.txt` are needed only within I1 and can be recreated from `git show $START:gradlew`.

**O-3 (medium): DEP runs a plain `:app:dependencies`, which no earlier story has run.**
- **Where:** DEP (`plan.md:913`); 2.5 last row (`:279`).
- **What:** U-060's evidence came from `--configuration releaseRuntimeClasspath` (`sprint-017/U-060/evidence/after-deps-releaseRuntime.txt:10`). A plain run resolves every configuration of `:app`, including androidTest and lint classpaths that the writer pass never resolves. With `verify-metadata` true it may end in a verification failure. No row covers DEP(0) failing, so I0 would stop with no decision.
- **Fix:** run two `--configuration` reports (`releaseRuntimeClasspath`, `debugUnitTestRuntimeClasspath`), as U-060 did. That adds about 1 to 2 minutes per DEP. Alternatively, add a row: "DEP fails on a configuration outside the gates → record verbatim, re-run with the two `--configuration` forms; never write metadata for it".

**O-4 (low): I7 steps 5 and 6 are in the wrong order.**
- **Where:** `plan.md:1264-1269`.
- **What:** step 5 (A21) reads "from DEP(7)'s test classpath the resolved `androidx.test:monitor` version", but DEP(7) is step 6.
- **Fix:** swap steps 5 and 6.

**O-5 (low): the later-case allowance in 4.M keys on name prefixes.**
- **Where:** `plan.md:1419-1423`.
- **What:** U-074's restore tests (R1 to R4, R8, C1) reuse U-067's fixtures `cancellableImapRun` and `CancellingInterceptor`. U-074 story `:78-79` and `:132` show this. Their final names are not fixed until U-074 is built. A case that drives the IMAP loop under a name other than `doWork_imapRestore_*` would fail under M-2x and trip D64.
- **Fix:** at I0, classify each later case of the three classes by the loop it drives, from U-074's and U-070's logs and the fixture it calls. Write that table into `mutation-log.md` before M-1a, and let D64 apply to that table.

**O-6 (low): `git restore` writes a CRLF `gradlew`.**
- **Where:** D15 to D18 (`plan.md:1490-1493`) and 4.R's wrapper recovery (`:1052-1054`).
- **What:** under `core.autocrlf=true`, `git restore … gradlew` writes CRLF: `git cat-file --filters --path=gradlew HEAD:gradlew` gives 260 CRLF. Git for Windows' `sh` ignored CR in my test (`len=3`), so I do not count this as a blocker. Still, the restored working file would differ in bytes from the LF file the generator writes.
- **Fix:** use `git -c core.autocrlf=false restore --source=HEAD --worktree -- gradlew`, and restore `gradle/wrapper` and `gradlew.bat` normally.

**O-7 (low): the D7 scan has no baseline.**
- **Where:** I7 step 2 (`plan.md:1252-1256`).
- **What:** nothing shows that the five strings occur 0 times at I0. A pre-existing occurrence in system-err (for example a ByteBuddy or JDK warning) would read as a D7 hit at I7 and add flags.
- **Fix:** run the same loop on `$WORK/logs/I0-gates.log` and `$R` right after G(0). The XML is gone after the next `rm -rf $R`. Write the result into `$E/I0-d7.txt`, and add the file to I0SET (33 files).

**O-8 (low): the SC-2 merged-manifest check has no command at I0.**
- **Where:** D3 (`plan.md:1478`) lists "merged manifest". No I0 step greps it, and CMP starts at I1.
- **Fix:** in I0 step 6, add `grep -c 'android:minSdkVersion="24"' $E/I0-merged-debug.xml` (expected `1`).

**O-9 (low): E14.1 states a KGP minimum for AGP 9.5 that no source gives.**
- **Where:** `plan.md:504`, "AGP 9.5 needs Gradle >= 9.6.0, JDK >= 17, KGP >= 2.2.10 (built-in Kotlin)".
- **What:** C6 states 2.2.10 for AGP 9.0. Q-4's 9.5 table has rows for Gradle, Build Tools and JDK only.
- **Fix:** "AGP 9.5 needs Gradle >= 9.6.0 and JDK >= 17; AGP 9 built-in Kotlin needs KGP >= 2.2.10". The 34-line comment check is unaffected.

**O-10 (info): source of the Q-1 checksum.**
- AC-6 says the checksum is "taken from services.gradle.org". The supplement fetched Q-1 and Q-2 from `downloads.gradle.org`, after a redirect (`:23-28`).
- I1's log entry should cite the supplement's URLs and the redirect note.

**O-11 (info): Robolectric 4.18.1 installs content providers for lazily created applications.**
- Q-8 reports this behaviour change.
- I7's D37 report text should name it as a candidate cause, if an application-context test changes.
- The manifest has no `InitializationProvider`, so the effect is expected to be nil.

### Checked and sound (no finding)

- **D5 per increment.**
  - The writer command (`:908`) equals design `:899` exactly.
  - The order is: pristine restore, one pass, proof, group review, enforced `:app:assembleDebug`, then the full gates.
  - "Additions only" is proven by the U-060 set comparison of `(group, name, version, artifact, hash type, value)`. Every old checksum tuple must survive, the configuration element must serialise identically, and `trust=3` is checked separately. That set comparison is sound for "no checksum removed or altered, no trust added", and the numstat is only the text review.
  - A missing entry after the pass stops the run (D32), and a mismatch on an unchanged artifact stops it (D33).
- **F1 to F5 and the stop rule.** F1 to F5 equal design `:766-770`, and the stricter stop-and-report ruling is in 2.4 and D35. Two pre-specified moves are covered:
  - the KSP reorder (D54) and N-6 (D74), both as the design words them (`:565`, `:590-596`);
  - the F2 inputs (the 9.7.0 checksums), which are now in the supplement (`:30-38`).
- **I1 wrapper.**
  - Q-12 confirms the two-run claim.
  - Step 3's expectation, equal to HEAD after W1, is right.
  - The tool proves "block back, insertion only" against a copy taken at once after the generator.
  - Mode 100755 holds, because `core.filemode=false`, and CM checks `--summary`.
- **The other commit lists.** Every name in STD (21), VM (4), I0SET (32) and the I2 to I14 lists is written by a step. Each list has a clean-index check and an `ls` check before `git add`. Every path is explicit. There is no push and no amend. A failure after a commit gets a fix commit (D91), and the commits follow AC-24's order.
- **Coverage map.** Every AC and IC has a step and evidence. IC-5 and IC-6 are recorded "not run". The IC-4 procedure in 10.1 equals story IC-4, and its preference key `third_party_integration` exists (`Preferences.java:93`). Q-1 to Q-10 are each used by a named step, and no step needs an unlisted input.
- **Developer constraints.** The constraints in section 11 cover every item of the brief, apart from O-1's scoping.

### Not reached

- I did not run the plan's own `oldtext_check.py`. My sequential simulation covers its edit entries, and it is stricter, because it checks each text in order rather than only at the start. I did not check the M and R9 anchors against the post-U-074 files, which do not exist yet. I0's run of the tool is the check for those.
- I did not exercise `deps_resolved.py`, `scan.py` or `jacoco_class_lines.py` on real inputs.
- I did not check the 9.9.2 template itself, only 9.9.1's (B-2).
- I did not open the U-063 probe precedent or the U-070 plan.
- Decision rows D8 to D12 and D75 to D85 were read, not traced against every story clause.

## Phase Completion Report
---
story_id: "U-071"
phase: "plan-gate/plan-review"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-025/U-071/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
