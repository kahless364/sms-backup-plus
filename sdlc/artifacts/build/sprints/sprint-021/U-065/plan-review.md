---
artifact_type: gate-result
story_id: "U-065"
gate: plan-review
gate_id: "plan-review"
checkpoint: plan-gate
verdict: PASS
iteration: 2
agent: "Solution Architect (separate instance, plan-review)"
timestamp: "2026-11-21T04:41:20Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-021/U-065/plan.md"
base_commit: "b2c2dfd8d7f0f5de8627575e160215b24b7cc5d9"
blockers: 0
observations: 5
---

# Gate Report: gate-result — U-065

## Gate: plan-review @ plan-gate

## Verdict: PASS

## Summary

I re-read the revised plan (iteration 2) as a whole and checked it again against the code at `b2c2dfd8`.

- **B-1 is fixed.** J4 is no longer expected to fail under M-P7. Under M-P7, J7 fails on the order of the layered protocols. I confirmed in a scratch run that an untouched layered SunJSSE socket enables `[TLSv1.3, TLSv1.2]`.
- **The mutation table is right.** I checked each of the 14 rows against the planned test and production code: the failing tests, the first assertion that fails, the failure type, and the "not failing" lists. Every row is correct. D30 now stops only on a difference from the row.
- **The production edits still compile and behave as before.** I applied D1 to D6 and P1 to P4 again, word for word from the plan, to scratch copies. The only changes from iteration 1 are the two comment changes in D3 and D5. Both files still compile with `-Werror -Xlint:unchecked`, and SunJSSE gives the same results as in iteration 1.
- **Nothing accepted in iteration 1 has regressed:** restore protocol, counts, commands, evidence hygiene.

Five observations remain. None blocks. The most useful one, O2-1, is a one-line clarification of the I3 hunk check.

## Iteration 1 findings and their resolution

| Finding (iteration 1) | Resolution in iteration 2 | Status |
|-----------------------|---------------------------|--------|
| B-1 (blocking): M-P7 expected J4 to fail | J4 removed from the M-P7 row (plan.md:2417), the J-doc (plan.md:1083-1085), the J4 Javadoc (plan.md:1251-1253) and I4 (plan.md:2328-2329). M-P7 is detected by C2, the two pinned STARTTLS refusal tests and J7 ("layered … enabled protocols in order") | Resolved; checked against the code and by a scratch run |
| O-1 (medium): D30 against failure types that are exceptions | Failure-type key added (plan.md:2393-2400). Every row states the type. D30 (plan.md:2583) treats a match in test and type as never a mismatch, and accepts any `SSLException` subclass where `SSLHandshakeException` is named | Resolved |
| O-2 (medium): D6 continued without a decision | D6 is now a stop after I2 and before I3 (plan.md:2559), with D6-a (tests written with a widened catch, AC-8 (b) met as accepted) and D6-b (as D4, AC-8 (b) unmet). I3 waits for the reading (plan.md:2305) | Resolved; see O2-3 for a narrow edge |
| O-3 (medium): IC-1 could touch `Pixel_10_Pro` | Step 1 (plan.md:2731-2736): `adb devices -l` first. With more than one device: stop and ask the owner, and send nothing. `emu avd name` only when exactly one emulator is listed | Resolved |
| O-4 (medium): `date` quoting | Step 4 (plan.md:2742-2744): `adb -s <serial> shell "date '+%m-%d %H:%M:%S.000'" \| tr -d '\r'`, then a check that `T` is not empty | Resolved |
| O-5 (low): guard edit against AC-6 (d) | Both guard edits withdrawn. The guard code is byte-identical. AC-6 (e) is met by its second option, in the class Javadoc and the Implementation Notes (plan.md:688-696, 1558). The allowed hunk ranges now forbid the guard lines | Resolved |
| O-6 (low): probe N3 was weak | The suites are reversed before the round trip (plan.md:2121-2124) | Resolved |
| O-7 (low): trade not in the hand-off | I10 names the `ALLOWED_PROTOCOLS` trade for the product owner (plan.md:2474-2475) | Resolved |
| O-8 (low): durability of the restore copy | I5 adds `git hash-object -w` (plan.md:2347-2349). The recovery note restores from the object store if the saved copy is lost (plan.md:2369-2373) | Resolved |
| O-9 (info): text inaccuracies | `-90,0` (plan.md:2352; my scratch diff agrees), four replaced lines (plan.md:2078), four test classes (plan.md:24) | Resolved; one leftover, O2-5 |
| O-10 (info): stronger layered check | Left as it is, with a sound reason: such a check passes with or without the factory's step, so it detects nothing. D13 keeps the stronger form for the other probe branch (plan.md:56) | Accepted |

## Findings

### Blockers

None.

### Mutation table, row by row (plan.md:2407-2422)

I walked each row through the planned test code. For the Conscrypt facts I used U-063 `I0-probe.txt`. For SunJSSE I used the iteration-1 scratch runs and the plan's section 1.2. Each row's failing set is complete and each "not failing" list is right.

- **M-P1/M-P8.** These fail as stated: H1 (set), H2 and J2 (`TLSv1` direct), H3 case (i), H4 (`containsExactly` after a successful `verify`), J1, J3 (protocols after the names pass), J4, C1 and C2 (`assertProtocolFloor` after both preconditions pass), and the refusal tests through `fail(...)`. J7 passes because both factories are equal.
- **M-P2.** C1, C2 and the pinned refusal tests fail. Under SunJSSE only J7 fails, on order. J3 and J4 compare sets and pass.
- **M-P3.** J3, J4 and J7 fail (J7 on the protocol set).
- **M-P4 and M-P6 (i).** C1 and C2 fail on cipher order. Their protocol checks pass first. H5 fails at check (ii). Under Conscrypt, the provider's known-suite order differs from the literal order, and the swapped pair is present (`hardened_enabled_ciphers`). J3 and J7 fail.
- **M-P5.** J3 fails at `isNotNull`: an unconnected SunJSSE socket reports `null`. J7 fails on server names.
- **M-P6 (ii).** C1 and C2 fail on the protocol set in `assertSameHandling`. H1, H2, J1, J2, J7 and the Handshake refusal tests fail. H3, H4, J3, J4 and the layered-equals-implicit test pass.
- **M-P6 (iii).** `createSocket_dnsHost_setsServerNameOnRealSocket` fails at `isNotNull`, and J7 fails.
- **M-P7.** Covered under B-1 above.
- **M-F1.** H4 fails with `WantedButNotInvoked`, which is an `AssertionError` subclass.
- **M-D11r.** Only H3 case (iii) fails; cases (i) and (ii) pass first.
- **M-S5.** J6 fails on an assertion. J7 errors with `IllegalArgumentException` at `pinnedSocket("bad_host.example")`. The two SNI tests fail as in U-064's M-N2.
- **M-E4.** `handshake_enrolledCertificateForDifferentHostName_isAccepted` and the two pinned TLS 1.2 tests fail with `SSLHandshakeException`, as in U-063's M-E4 (`mutation-log.md:586-587`). `handshake_certificateOtherThanEnrolled_isRejected` fails through `fail()`. The Expiry test fails on its cause assertion.
- **M-E5.** `handshake_certificateOtherThanEnrolled_isRejected` and the Expiry test fail through `fail()`.

### Observations (non-blocking)

**O2-1 (medium): one hunk the plan's own edit produces sits on the edge of a "no hunk" range.**

- **Where:** plan.md:573-576. The helpers are inserted "after `:517`", and `:518-535` "must show no hunk".
- **Evidence:** I inserted the plan's exact blocks (H-const, H3 to H5, H-help) into a scratch copy of the Hardening test and ran `git diff --no-index -U0`. The helper insertion shows as `@@ -518,0 …`, which means after the blank line 518. It changes no old line, so it is outside the range by the same reading I5 states for `-90,0`. But a literal check of "518-535 must show no hunk" could send the developer into D17, which says undo and redo, and the result would be identical.

  Every other insertion lands outside its forbidden range:

  | File | Insertions | Forbidden ranges, for comparison |
  |------|------------|----------------------------------|
  | Hardening | `-148,0`, `-449,0` | guard 149-173; 416-448 |
  | JDK | `-325,0`, `-349,0` | 326-348; 350-365 |
  | CST | `-197,0`, `-214,0` | 156-196; 215-224 |
  | Handshake | `-271,0` | 272-360 |

- **Fix (one line, can go in the developer brief):** "the helper insertion shows as `-518,0`, after the blank line 518, which is outside", as I5 does for D1.

**O2-2 (low): the M-S5 message description is slightly wrong.**

The M-S5 row says J6's escaped list names all three names "direct and layered". In J6, a direct exception is followed by `continue` (plan.md:1349-1351), so only the three direct entries appear. Test and type (A) are correct, so D30 is not affected. Only the recorded message differs.

**O2-3 (low): D6-a only works when the observed class X is an `IOException`.**

D6-a widens the catch to `java.io.IOException` and asserts `e instanceof SSLException || e instanceof X`. If X is not a subclass of `IOException` (for example a `RuntimeException`), the catch does not catch it, and `e instanceof X` does not compile (incompatible class types). The probe catches every `Throwable`, so any class can appear. D6 is reached only after a stop, and the orchestrator's reading comes first, so this cannot pass silently. Optional fix: add "D6-a applies only if X is an `IOException`; otherwise D6-b".

**O2-4 (info): IC-1 step 1 has no branch for one physical device only.**

Step 1 covers no device, more than one device, and exactly one emulator. It does not cover exactly one physical device and no emulator. Nothing is sent in that case, so the gap is safe. Stating "stop and ask the owner" would close it.

**O2-5 (info): one leftover count.** R10 (plan.md:2711) still says "five heavily edited classes". It should be four, plus the helper.

### Checks that passed or did not regress

- **Production code (section 2.4).**
  - Every `old_string` is unique.
  - Compared with iteration 1, the code is identical. Only two things differ:
    - D3 keeps the existing API-level comment and adds one sentence. This produces the hunk `-105`, inside the static initialiser and outside every protected range.
    - D5 has three more Javadoc lines.
  - Hunks: `-90,0`, `-93`, `-105`, `-107`, `-116,2`, `-149,0`, `-182,6`, `-188,0`. None touches `:37-81`, `:83-89`, `:152-180` or `:191-266`.
  - The scratch build compiles with `-Werror -Xlint:unchecked`.
  - On SunJSSE, the static list is `[TLSv1.2, TLSv1.3]`. Pinned and default sockets agree on protocols and cipher order for every planned host. The S1 to S5 names hold on both shapes, and `hardenSocket(sock, null, null)` gives `[TLSv1.2, TLSv1.3]`.
  - The trust lines `:77-80` are untouched.
- **Other gates' changes (sections 2, 8, 10, 12, I10 items).**
  - These are prose and hand-off text. The developer edits no record: the I10 items are for the orchestrator.
  - The note from contract-verification O5 (in the initialiser-failure branch, `createSocket` may now throw an unchecked exception) adds a statement only. It changes no step.
- **Tests and guards.**
  - The test code is unchanged from iteration 1 apart from comments and the withdrawn guard edits.
  - The A1-G2 guards are byte-identical, and the Hardening and Handshake hunk ranges now protect them.
- **Probe.**
  - The reversal before the round trip compiles: `ArrayList` and `List` were added to the imports.
  - Every key still has a row in each direction.
  - D6 is a stop before I3. AC-4 (b) still takes the story's second branch, with D13 as the pre-written alternative.
- **Restore protocol.**
  - Primary restore by `cp`.
  - Durable copy with `git hash-object -w`. This writes objects only, with no tracked change or commit.
  - Fallback by re-applying section 2.4.
  - Checks by blob hash, diff hash and two-file porcelain.
  - Pre-checks in I8 and I9, and no mutation after I9.
  - Restoring from the object store gives LF line endings. With `core.autocrlf=true` and the files stored as `i/lf`, the blob hash and the diff hash still match.
- **Counts.** 713 at HEAD (authoritative command, re-run); 19 tests added and 7 changed, giving 732 (728 or 724 if a version is dropped); 68 cases in the seven classes; 14 mutation entries; 21 Gradle runs. These agree throughout.
- **Commands.**
  - `MSYS_NO_PATHCONV` is unset for Gradle, and every Gradle call runs in the foreground with a 600000 ms timeout.
  - `rm -rf $R` runs before every run, and the read-only U-063 init script is used.
  - No background shells, no polling, no heredocs for editing.
  - IC-1 now addresses only the agreed serial.
- **Hygiene.**
  - Raw logs stay in the work folder.
  - I11 writes nothing inside the folder it scans.
  - Today the workspace, including the other two gates' results, returns pattern exit 1. The user-name and host-name scans return exit 1, with `compiled-pipeline.json` excluded.
  - Nothing under `app/`, `k9mail-vendored/` or the build files is modified, and HEAD is still `b2c2dfd8`.

## Phase Completion Report
---
story_id: "U-065"
phase: "plan-gate/plan-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-021/U-065/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
