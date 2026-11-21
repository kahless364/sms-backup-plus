---
artifact_type: gate-result
story_id: "U-064"
gate: plan-review
gate_id: "plan-review"
checkpoint: plan-gate
verdict: PASS
iteration: 2
agent: "Solution Architect (separate instance, plan-review)"
timestamp: "2026-11-21T02:56:51Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-021/U-064/plan.md"
base_commit: "de03e99b51a04d402b64cbd5c7f0d7d19e90f2a9"
blockers: 0
observations: 3
---

# Gate Report: gate-result — U-064

## Gate: plan-review @ plan-gate

## Verdict: PASS

## Summary

I re-read the revised plan (iteration 2) as a whole and re-checked it against the code at `de03e99b`. Blocker B-1 is fixed. The new step I8 writes no output inside a scanned folder. It records file names, counts and exit codes only. It covers `I6-checks.txt` and, through a final check, its own result file `I8-scan.txt`. It searches for the bare user name and host name without writing either value anywhere. I reproduced its exit codes in a scratch folder outside the repository. Observations O-1 to O-7 are all handled. Nothing I cleared in iteration 1 has regressed. A developer can carry out this plan and end with AC-1 to AC-3 and IC-1 to IC-12 met and proven. Three informational notes remain; none blocks.

## Iteration 1 findings and their resolution

| Finding (iteration 1) | Resolution in iteration 2 | Status |
|-----------------------|---------------------------|--------|
| B-1 (blocking): the IC-8 scan wrote inside the folder it scanned (grep exit 2), never covered `I6-checks.txt`, and with `-n` copied hit text into evidence | The scan is removed from I6 (plan.md:621) and becomes a new last step, I8 (plan.md:649-706). All raw output goes to `$WORK/logs`. Only `grep -l` or `-c` is used. Every file in the workspace, `$NEWT` and the SNI diff (kept in the work folder) are scanned. Exit codes are stated (1 clean, 0 hit, 2 error). The sanitised `I8-scan.txt` gets a final count-only check that prints to the terminal only. Exit 2 has its own row, S14; a hit has row S15 | Resolved; verified by simulation (below) |
| O-1 (medium): no search for the bare user name or host name | I8 reads `USERNAME`/`USER` and `COMPUTERNAME`/`hostname` into a shell variable and searches with `grep -rliF -e "$v"`, recording only `exit=` and `files=`. The story file is checked with `grep -ciF` (count only). A value shorter than 3 characters is reported as `value-unavailable` and routed to S14 | Resolved |
| O-2 (low): two output folders were never created | I0 step 1 runs `mkdir -p "$WORK/logs" "$M"` (plan.md:187-188) | Resolved |
| O-3 (low): the final AC-2 assertion message named no host | The message now ends with `"names tried: " + java.util.Arrays.toString(names) + "; escaped"` (plan.md:475-478). The M-N2 expectation (plan.md:563-565) and S7 (plan.md:718) now take the evidence from Truth's "but was" value, not from the "names tried" part, which always lists every name | Resolved; compiles (a single `String` argument to `assertWithMessage`) |
| O-4 (low): three foreseeable outcomes had no row | New row S13 (plan.md:724): a mutation failure that does not match the expectation is logged `MISMATCH (not counted as DETECTED)`, the file is restored, and the developer stops. S3 now also covers assertion (i) failing inside the sandbox (plan.md:714) | Resolved |
| O-5 (low): S3 let AC-3 swap its fixed host name | S3 now treats a swap in AC-3 as a recorded deviation, reported in the hand-off for the orchestrator to accept or reject before review | Resolved |
| O-6 (low): the hand-off restated the counts | I7 (plan.md:627-634) points to IC-4 and the evidence files and does not restate the numbers | Resolved |
| O-7 (info): S6 repeat during a mutation; the story's own IC-8 line matches its pattern | S6 names the repeat as a deviation from IC-7(d) (plan.md:717). I8 records the matching story lines by number only and expects exactly one, the IC-8 line (plan.md:679-680, 697-701) | Resolved |

## Findings

### Blockers

None.

### B-1 re-check, command by command (plan.md:662-694)

The I8 commands, run with `$W`, `$NEWT` and the work folder replaced by scratch stand-ins, gave the results stated in the plan:

| Command (I8) | Stated result | Observed in the scratch run |
|--------------|---------------|-----------------------------|
| `git diff HEAD -- $SNIT > $L/I8-sni.diff` | exit 0 | `git diff` without `--exit-code` exits 0 whatever it prints; the file goes to the work folder, which is not scanned |
| `grep -rlEi --exclude=compiled-pipeline.json "$P" $W $NEWT $L/I8-sni.diff > $L/...` | clean: exit 1, files 0 | `pattern-scan exit=1 files=0` |
| the same, with a hit in an evidence file named like `I6-checks.txt` | exit 0, files above 0 | `exit=0 files=1`; a hit only in `compiled-pipeline.json` is excluded (`exit=1`) |
| `grep -rliF ... -e "$v"` (user name, host name) | clean: exit 1, files 0 | `exit=1 files=0`; with the user name planted in a file, `exit=0 files=1`. The value appeared in no output |
| any `grep` with a missing input path | exit 2, row S14 | `exit=2`; the error text lands in `I8-scan.log` in the work folder, not in the repository |
| story lines (`grep -nEi "$P" $S \| cut -d: -f1`) and `grep -n 'IC-8: (evidence hygiene)'` | one line number, the same in both | both give 83 on the current story |

- **No output inside a scanned folder.** The output files are `$L/I8-pattern-files.log`, `$L/I8-<label>-files.log` and `$L/I8-scan.log`. They sit in the work folder, and the only work-folder file that is scanned is `I8-sni.diff`. So the iteration-1 exit-2 condition cannot arise.
- **The recording files are covered.** `I6-checks.txt` and every other evidence file lie under `$W` and are scanned by name. `I8-scan.txt` is written once, through `sanitize.py`. It is then checked with `grep -cEi "$P"` and `grep -ciF -e "$v"`, and those results go to the terminal and the hand-off only (plan.md:690-694).
- **The bare names are never written.** Each value exists only in a shell variable. `-l` prints file names and `-c` prints counts. S15 has the developer read context with the Read tool, so the value appears only in tool output (plan.md:726).
- **Current workspace.** A read-only run of the same pattern and bare-name scans gives no match: `contract-verification.md`, `expert-depth-check.md`, `plan-review.md` (iteration 1) and `plan.md` all return exit 1, and the evidence folder is still empty. This gate result contains no such text either.

### Regression check (nothing cleared in iteration 1 broke)

- **AC-1 class** (plan.md:253-343): unchanged from iteration 1. Its imports, the members it uses (all public: `TlsTestFixtures.java:97, 101, 280, 300, 329, 338, 354`; `LoopbackTlsTestServer.java:93, 122, 147, 167`), its assertions and `causeChain` are all the same.
- **SNI insertion** (plan.md:389-405): the `old_string` (`DefaultTrustedSocketFactorySniTest.java:301-305`) and the additions-only shape are unchanged. The only change in the added block is the AC-2 final message. The I3 checks still expect 0 deletions, 1 hunk and 14 `@Test` annotations.
- **Mutations** (plan.md:544-567): the M-N1 text against `PinnedCertificateSocketFactory.java:149`, the M-N2 text against `DefaultTrustedSocketFactory.java:245-253` and the expected results (13/12/1 and 19/17/2) are unchanged. The base blobs are still `8a0fbb64...` and `d5439271...`; `git status --short` shows nothing under `app`, `k9mail-vendored`, the build files or `gradle`.
- **Decision table**: S13 to S15 are new. S3 and S6 are widened. S1 gains a hand-off note for the contract-verification gate: CR-012's premise is at `:247`, and I checked that the line says what the plan cites. None of these changes contradicts IC-11. Every defect path still stops, and the developer edits no production code.
- **Counts**: these agree with each other and with iteration 1: 710 to 713, per-run 13, 19 and 15, mutation runs 12/1 and 17/2, and 9 Gradle invocations. The story's count is no longer restated in the hand-off.
- **Changes made for the other gates**: the pattern paragraph that now opens section 3 (plan.md:172-177), the test-smell paragraph in section 6, and the expanded sections 8 and 9 are prose. They change no command, expectation or file list.

### Observations (non-blocking)

**O2-1 (info): a trailing space in one I8 output.** `story-pattern-lines` prints its numbers joined by `tr`, so a single match gives `83 ` with a trailing space, while `story-ic8-line` prints `83`. The plan says to compare them as numbers, so nothing breaks, but a check that compares the strings would differ. Optional fix: `| paste -sd' '` in place of `tr`.

**O2-2 (info): an I8 error can leave the work-folder path in the I8 log.** If a `grep` fails on `I8-sni.diff`, the error text in `I8-scan.log` carries the work folder's absolute path. `sanitize.py` then either replaces it or refuses to write `I8-scan.txt` (it fails closed), and S14 already says to fix and re-run. Nothing can leak; this is noted only so the developer is not surprised.

**O2-3 (info): S15 does not say whether to continue.** When a match is in another agent's gate file or in `plan.md`, the developer "reports to the orchestrator" but is not told whether to continue or stop. Stopping and handing off is the safe reading, and it matches S12.

## Phase Completion Report
---
story_id: "U-064"
phase: "plan-gate/plan-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-021/U-064/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
