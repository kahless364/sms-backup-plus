---
artifact_type: gate-result
story_id: "U-061"
gate: plan-review
gate_id: "plan-review"
checkpoint: "plan-gate"
verdict: PASS
iteration: 2
agent: "Solution Architect (separate_instance reviewer)"
timestamp: "2026-11-12"
reviewed_artifact: "sdlc/artifacts/build/sprints/sprint-018/U-061/plan.md"
base_commit: "218387187371237b288ca60db390441b6e66da80"
blocking_findings: 0
non_blocking_observations: 6
---

# Gate Report: gate-result — U-061

## Gate: plan-review @ plan-gate

## Verdict: PASS

## Summary

I re-read the whole of plan.md (1112 lines, iteration 2) from disk. Both iteration-1 blockers are resolved:

- **B1:** the I8 comment no longer contains "billing" or "donat", and AC-1, IC-10 and IC-13 are now
  re-checked after the I8 edit.
- **B2:** snapshot-and-replay is replaced by one increment per Developer turn, with an orchestrator commit
  after each turn. The protocol is complete and safe.

All six iteration-1 observations were applied. The hardened scripts fail closed. Cross-references and step
numbering are consistent. One defect is new in this review, though it was already present in iteration 1:
the I8.8 `lenient` grep has no pathspec, so it will very likely stop I8 on a false positive. It fails
loudly, not silently, so it is non-blocking. Fix it before the I8 turn (observation 1).

## Iteration-1 disposition

| ID | Iteration-1 finding | Status | Evidence in plan.md (iteration 2) |
|----|---------------------|--------|-----------------------------------|
| B1 | The I8 comment broke AC-1, and AC-1 was not re-checked after the edit | **Resolved** | I8.1 (l.498) reads `- the nested SKU-constants class was removed from Consts.` (no match under `-i`). I8.2 (l.503-509) runs the AC-1, IC-10 and IC-13 greps after the edit and before the final gate. The mapping row for AC-1 points to `I8-final-greps.txt` |
| B2a/b/c | Replay failed when HEAD had moved; `code-location.md` could be swept into another commit; new files were lost | **Resolved by replacement** | One increment per turn (l.122-223). The orchestrator commits by explicit path. Broad `git add` and `commit -a` are banned for the life of the story. New files are declared as `??` and staged with `git add -A -- <path>`. HEAD movement is now harmless: the I0.1 guard is `git diff --quiet $BASE HEAD -- <product paths>` |
| Obs 1 | Third-party check: move it to I0 and make it fail closed | Applied | I0.5: `pm list`, `run-as ls` rc, `<map` check, ON/OFF/UNKNOWN classification, scratch file deleted. Re-run at I8.10.1 |
| Obs 2 | Script silent-pass risks | Applied | See check (4) below |
| Obs 3 | Scratch copy vs scratch branch | Applied | Deviation 8; logged at I8.11 |
| Obs 4 | I8 snapshot taken before the final gate | Applied (superseded) | I8 order is edit → greps → gate → evidence, with no tracked change after I8.1 |
| Obs 5 | QA should read the content of the supersession change record | Applied | l.738-747. The body-unedited check is limited to the frontmatter |
| Obs 6 | Orchestrator notes for deviation 1 and the D6 reading | Applied | l.749-753 |

## Verification of the coordinator's five questions

**(1) The commit mechanism is complete and safe.**

- **`app/build.gradle` in I5, I6 and I8.** This is handled correctly. The start check (empty product
  porcelain) means the previous increment is committed, so each later increment's porcelain shows a fresh
  ` M app/build.gradle`. Content is confined by each increment's own self-check: the I6 `git diff -U0`
  shows only the Rule 5 edit and 10 deleted lines (in that turn the diff is against the I5 commit), and the
  I8.2 greps run plus the I8.4 root-value equality check. The porcelain checks only paths; the diff checks
  carry the content guarantee, and that split is right.
- **Revert command.** `git restore --source=HEAD --staged --worktree -- <paths>`:
  - It is correct for modified (` M`) and deleted (` D`) tracked files. Restoring a deleted file recreates
    its directory, so the I3 package directories come back. With `core.autocrlf=true` the restored files
    get CRLF, which matches the current working tree.
  - It is not correct when the pathspec includes an untracked IC-9 `??` path. `git restore` aborts on a
    pathspec that matches nothing in the source and restores nothing. The plan's own "then `rm` any `??`
    file" and the "porcelain must be empty" check catch this loudly, so it is not silent. See
    observation 2.
- **No conflict between "the Developer never stages" and "log and evidence in every commit".** The
  orchestrator stages `implementation-log.md` and `evidence/` by explicit path (orchestrator step 3). The
  Developer only writes them. The only git write the Developer may make is the explicit-path restore, and
  only on failure.
- **Orchestrator side.**
  - Steps 1-4 check that the index is empty first, compare the porcelain against both the hand-off and the
    table, stage by explicit path, and re-check the porcelain after the commit.
  - The banned forms cover the sweep risk. I verified that the product-pathspec porcelain is empty at HEAD
    today (`rc=0`, no output).
  - The story file and `sprint-plan.md`, which are already modified, are outside the product pathspec and
    so cannot leak into an increment commit.
- **IC-2 intent.** Each increment I1-I8 is its own commit, made after its gate on the same tree, so IC-2
  holds. I8 deviates from IC-2's literal ordering ("commit, followed by the final gate run"): the plan runs
  the gate first and then commits the identical tree. That is equivalent. QA should be told.

**(2) No stale references.** A grep for `snapshot|replay|cumulative|sha256|.patch|SHA256SUMS|apply --cached|commit-tree`
finds only these:

- l.27 and l.124: statements that snapshot-and-replay was dropped.
- l.1096: the verdict narrative.
- I8.9 (l.582-589): the IC-4 scratch-copy patch and Gradle's own `--write-verification-metadata sha256`
  argument. That is legitimate, and it is not replay.

**(3) The final I8 tree satisfies every AC and IC, and the evidence is taken on that tree.**

- After I8.1 no tracked file may change (l.491). I8.2 asserts AC-1, IC-10 and IC-13 on the final tree.
- I8.3 is the final gate.
- I8.4 builds incrementally on the same tree, with no `clean` and no edit. It adds release, report and
  manifest outputs and cross-checks that the root JaCoCo value equals the I8.1 comment.
- I8.5-I8.10 consume only those outputs. I8.10 installs the debug APK the final gate built.
- I8.9 builds from `git archive HEAD` plus `git diff HEAD -- app`, which is exactly the final tree. I
  confirmed that neither `C:/Code` nor `C:/` is a git repository, so `git apply` there runs outside any
  repo, as intended.
- AC-8: Rule 4 is updated, and no `minimum` changes. AC-11 is satisfied by I7.
- The end check at I8.11 expects only ` M app/build.gradle`.

**(4) The hardened scripts fail closed.**

| Script | Fail-closed behaviour |
|--------|-----------------------|
| `junit_totals.py` | Exits 1 when no XML files are found |
| `jacoco_pkgs.py` | Exits 1 when no packages are found |
| `deps_drift.py` | Exits 1 when either input is empty |
| `r8_config_diff.py` | Exits 1 on empty input, zero removed lines, any added line, any unattributed removal, or any of the 7 AC-3 AAPT keeps not removed |
| `ui_titles.py` | Requires at least one title and 0 `donat` nodes; `--expect-last` requires an exact match |
| logcat check | Distinguishes an rc failure or empty output (`LOGCAT FAILED`) from `fatal_count` |
| Manifest diffs | Fail when a diff is empty |
| Mapping and dex counts | Require non-zero I0 counts and a non-empty after-file |

`strip_strings.py` already aborted on any per-file mismatch. The `junit-xml.init.gradle` mechanism matches
U-060 QA's proven script.

**(5) Nothing else broken.** I spot-checked these references and all are consistent:

- I0.5.1 and I0.5.3 (cited from I8.10.1)
- I8.2 and I8.1/I8.4 (stop condition 11)
- I8.6 and I8.7(3) (contract table)
- I8.8 (AC-7 and AC-9 rows)
- deviations 1-8 (orchestrator notes, IC-4 row)
- R1-R10 (`risks_identified: 10`)
- the per-increment file counts, which total 33 edited plus 5 deleted = 38

Line references from iteration 1 still hold at HEAD `21838718` (no product path changed).

## Non-blocking observations

1. **(Fix before the I8 turn.)** I8.8 runs `git diff "$BASE" | grep -nE 'dependency-verification=off|lenient'`
   with no pathspec.
   - By I8, the orchestrator will have committed `plan.md` (which contains this very regex at l.574 and
     "lenient" elsewhere), `plan-review.md`, and the story (AC-9 text at l.78 and l.147).
   - The grep will hit, and I8's rule "if any later check fails, stop and escalate" will halt the final
     turn on a false positive.
   - Fix: limit it to the product paths, for example
     `git diff "$BASE" -- app build.gradle settings.gradle gradle.properties gradle .github | grep -nE …`.
     This is a one-line edit the orchestrator can make before dispatching I8. If it is left unfixed, it
     fails loudly and can be adjudicated then.
2. **Failure procedure and Rollback.** Pass `git restore` only the tracked ` M` and ` D` paths from the
   increment's list, then `rm` the `??` paths. Including an untracked IC-9 path makes `restore` abort and
   restore nothing. This is only reachable in I6 with IC-9 triggered, and it fails loudly.
3. **Failure hand-off.** The plan does not say what the orchestrator does with the log and evidence of a
   reverted increment. Commit them path-limited, or leave them for the retry turn. Either works; state
   which.
4. **I8 ordering vs IC-2's literal text.** The final gate runs before the I8 commit, on the identical tree.
   The orchestrator should note this equivalence for QA, alongside the deviation notes.
5. **I5 escalation path.** If `deps_drift.py` exits 1, the next turn's start check fails until the
   orchestrator commits or reverts I5. This is correct, but the orchestrator should know that a product
   owner decision must come first.
6. **Evidence volume.** The I0 and I8 commits will carry `mapping.txt`, `configuration.txt` and the JaCoCo
   XML (several MB of text). This is acceptable under "evidence is text only". It is noted so the size is
   not a surprise in review diffs.

## Phase Completion Report
---
story_id: "U-061"
phase: "plan-gate/plan-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-018/U-061/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
