---
artifact_type: implementation-log
story_id: "U-064"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-11-21T03:09:48Z"
files_changed: 1
files_created: 1
tests_added: 3
tests_passing: 713
---

# Implementation Log: U-064

Plan: `sdlc/artifacts/build/sprints/sprint-021/U-064/plan.md` (iteration 2). Base commit
`de03e99b51a04d402b64cbd5c7f0d7d19e90f2a9` on `sdlc/modernization-plan`. Tests only; nothing committed, staged
or pushed. "The work folder" below is a folder in the session scratch area, outside the repository; it holds
the raw Gradle logs. Counts are stated by reference (IC-4, IC-12): Gradle totals are in
`evidence/I6-junit-totals.txt`, the authoritative count in `evidence/I6-checks.txt`.

## Capabilities Inventory

Not applicable: no existing code is replaced or rewritten. Production code is edited only by the two
temporary mutations (restored, see I4).

## Summary

- One new test class (`PinnedCertificateSocketFactoryExpiryTest`, AC-1) and two methods added to
  `DefaultTrustedSocketFactorySniTest` (AC-2, AC-3), as one additions-only hunk.
- Both mutations (M-N1, M-N2) compiled and were DETECTED exactly as expected; both files were restored and
  verified by blob hash.
- No decision-table row was needed; no deviation from the plan; no bug filed (IC-11 not triggered).
- DP-1: "leave out" (product owner, 2026-11-20); no not-yet-valid test.

## Per-step record

### I0 (2026-11-21T02:58:48Z, pre-flight; gate after it) - baseline (IC-1) and first gate (IC-3)

- Run: the pre-flight block of plan I0 step 1, then the four-task gate (`:app:assembleDebug`,
  `:app:testDebugUnitTest`, `:app:jacocoTestCoverageVerification`, `:app:assembleRelease`), foreground,
  timeout 600000 ms, in a shell without `MSYS_NO_PATHCONV`.
- Result against expected: HEAD `de03e99b...`, branch `sdlc/modernization-plan`, nothing dirty under `app`,
  `k9mail-vendored` or the build files, `prod-clean-exit=0`, both blobs equal the section 2 values
  (`8a0fbb64...` and `d5439271...`), authoritative count at HEAD in `evidence/I0-baseline.txt`,
  `toolchainVersion=25`. Gate: `exit=0`, daemon JVM 25.0.5.1, totals in `evidence/I0-junit-totals.txt`
  (`failures=0 errors=0 skipped=2`). As expected.
- Evidence: `evidence/I0-baseline.txt`, `evidence/I0-gates.txt`, `evidence/I0-junit-totals.txt`.
- Decision row: none. Deviation: none.

### I1 - first step, part (a)

- Run: `jshell` script from the plan (copy of the guard logic plus `new SNIHostName(...)`), run in the work
  folder, output sanitised.
- Result: identical to the first six lines of plan section 1.1. `bad_host.example`, `host.example.` and
  `-x.example` each: `guard=false`, `SNIHostName` throws `IllegalArgumentException`; `imap.right.test` and
  `192.168.1.10` accepted. JDK 25.0.5.1, the same as the Gradle daemon (row S8 not needed).
- Evidence: `evidence/I1-first-step.txt`. Part (b) is covered by I2. Decision row: none. Deviation: none.

### I2 - AC-1, new class

- Created `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryExpiryTest.java` with
  the code of plan I2 and the Javadoc content listed there. Run: the three pinned classes in one invocation.
- Result: `exit=0`, `files=3 cases=13 pass=13` (1 + 4 + 8). Assertion (ii) held with the direct cause being
  `CertificateExpiredException`, so rows S1 and S2 were not needed (L11 and L12 confirmed).
- Evidence: `evidence/I2-pinned-cases.txt`. Decision row: none. Deviation: none.

### I3 - AC-2 and AC-3 added to `DefaultTrustedSocketFactorySniTest`

- One Edit with the `old_string` of plan I3; the new block is inserted between the end of
  `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue` and the helper banner. No import changed (the added code
  uses fully qualified names; the plan's prose says four, the code has five, with `java.util.Arrays`).
- Diff check (`evidence/I3-sni-diff-check.txt`): numstat `87 0` (no deletions), no removed line, one hunk,
  14 `@Test` annotations in the file. The working-tree file keeps its CRLF line endings.
- Run: SNI and JDK-provider classes in one invocation: `exit=0`, `files=2 cases=19 pass=19` (14 + 5); the
  JDK-provider isolation guards still pass (row S10 not needed). Evidence: `evidence/I3-sni-cases.txt`.
- Decision row: none. Deviation: none.

### I4 - mutations M-N1 and M-N2 (IC-7)

- Full entries, with write-ahead lines, results and `restored` lines: `evidence/mutation-log.md`; case lists
  and sanitised failure messages in `evidence/mutations/`.
- M-N1 (2026-11-21T03:03:27Z): `enrolledCert.checkValidity();` replaced by `if (false) { ... }`; compiled.
  Run once (three pinned classes): 13 cases, 12 pass, 1 fail. The failure is
  `handshake_expiredEnrolledCertificate_isRejectedAsExpired`, `java.lang.AssertionError`, "the handshake must
  be rejected, but it completed". Restored 2026-11-21T03:04:34Z; blob equals `8a0fbb64...`. DETECTED.
- M-N2 (2026-11-21T03:04:42Z): try/catch replaced by its two body lines, guard kept; compiled. Run once (SNI
  and JDK-provider classes): 19 cases, 17 pass, 2 fail. Failing: the AC-2 test (Truth failure; `escaped`
  lists all three names with `IllegalArgumentException`) and the AC-3 test (`IllegalArgumentException`). The
  12 existing SNI tests and 5 JDK-provider tests pass. Restored 2026-11-21T03:05:43Z; blob equals
  `d5439271...`. DETECTED.
- Each mutation was preceded by the clean check (`git diff --quiet HEAD -- app/src/main k9mail-vendored`
  exit 0, empty porcelain, blob equal to base), applied with one Edit, run once, restored with a single-file
  `git checkout -- <file>`, and verified. Neither was present when any gate ran.
- Decision rows: none (S4, S7, S13 not triggered). Deviation: none.

### I5 - determinism (IC-6) and static checks (IC-6, IC-9)

- Pre-check: production clean, both blobs equal the base values (the end of I4).
- Three consecutive runs of the Expiry class and the SNI class with `--rerun`, each a foreground call: each
  `exit=0`, `files=2 cases=15 pass=15`; `diff` of run 1 against runs 2 and 3 exit 0 (identical).
- Greps over the two touched files: IC-6 pattern exit 1 (no match); IC-9 pattern exit 1 (no match).
  The repository-wide IC-6 form returns two hits, both in `K9MailTransportCreateOpenTest.java` (comment text
  about an interrupted flag, an existing file, not touched by this story); none in the new or added code.
- Evidence: `evidence/I5-run1-cases.txt`, `I5-run2-cases.txt`, `I5-run3-cases.txt`, `evidence/I5-checks.txt`.
- Decision row: none. Deviation: none.

### I6 - final gates (IC-3) and file checks (IC-2, IC-4, IC-5)

- Pre-check: no mutation present, both `restored` lines exist, production clean.
- Gate, same four tasks: `exit=0`, JDK 25.0.5.1; totals in `evidence/I6-junit-totals.txt`
  (`failures=0 errors=0 skipped=2`, skipped unchanged from I0).
- Checks in `evidence/I6-checks.txt`: working-tree count is HEAD count plus 3 (IC-4); porcelain under
  `app/src/test` lists exactly the two test paths; the stat of production, vendored and build files is empty;
  the verification metadata is unchanged (`vm-exit=0`); the SNI numstat has no deletions; the only untracked
  file under `app/src/test` is the new class, whose name contains `Test` (IC-5); the two production blobs
  equal the base values.
- Decision row: none. Deviation: none.

### I7 - this log and the hand-off text

- The hand-off text for the story's Implementation Notes is in the final reply. Under the existing
  product-owner decision note in the story, a short "As built" note pointing to this log was added (the only
  story edit; the frontmatter is untouched).

### I8 - IC-8 scan

- Last step; result in `evidence/I8-scan.txt` (see the final reply for the counts).

## Files created

| Path | Why |
|------|-----|
| `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryExpiryTest.java` | AC-1 |
| `sdlc/artifacts/build/sprints/sprint-021/U-064/implementation-log.md` | this log |
| `sdlc/artifacts/build/sprints/sprint-021/U-064/evidence/` (all files listed per step above, plus `mutation-log.md` and `mutations/`) | evidence |

## Files modified

| Path | Why |
|------|-----|
| `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java` | AC-2, AC-3 (87 lines added, none removed, one hunk) |
| `sdlc/artifacts/build/sprints/sprint-021/U-064-tls-baseline-gap-tests-pinned-expiry-and-sni-catch.md` | "As built" note under Implementation Notes only |

Production files `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` and
`k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` were edited
temporarily (M-N1, M-N2) and are byte-identical to the base again (blobs in `evidence/I6-checks.txt`).

## Criterion-to-evidence table

| Criterion | Result | Evidence |
|-----------|--------|----------|
| AC-1 | Met: passes unmutated; fails under M-N1 with the named message; enrolled and presented certificate are both `EXPIRED_HOST_CERT`; client made through `createSocket`, not the seam | `evidence/I2-pinned-cases.txt`, `evidence/mutation-log.md`, `evidence/mutations/M-N1-*.txt`, `evidence/I5-run*-cases.txt` |
| AC-2 | Met: guard false for the three names (I1 and the test's own assertion); passes unmutated; fails under M-N2 with all three names in `escaped` | `evidence/I1-first-step.txt`, `evidence/I3-sni-cases.txt`, `evidence/mutations/M-N2-*.txt` |
| AC-3 | Met: passes unmutated; fails under M-N2 with `IllegalArgumentException` | `evidence/I3-sni-cases.txt`, `evidence/mutations/M-N2-cases.txt` |
| IC-1 | Met | `evidence/I0-baseline.txt`, `evidence/I0-gates.txt`, `evidence/I0-junit-totals.txt` |
| IC-2 | Met in working-tree form (the literal `base..HEAD` form runs after the IC-12 commit) | `evidence/I3-sni-diff-check.txt`, `evidence/I6-checks.txt` |
| IC-3 | Met: gate exit 0 before any edit and at the end, JDK 25, no mutation present | `evidence/I0-gates.txt`, `evidence/I6-gates.txt` |
| IC-4 | Met: HEAD count plus 3 in the working tree; Gradle totals as stated by reference | `evidence/I6-checks.txt`, `evidence/I6-junit-totals.txt` |
| IC-5 | Met: no build file edited; one new class, name contains `Test`; no helper | `evidence/I6-checks.txt` |
| IC-6 | Met: greps clean in the touched files; three identical runs | `evidence/I5-checks.txt`, `evidence/I5-run*-cases.txt` |
| IC-7 | Met: two entries with write-ahead, result, `restored` and hash lines; both DETECTED | `evidence/mutation-log.md`, `evidence/mutations/` |
| IC-8 | See I8 | `evidence/I8-scan.txt` |
| IC-9 | Met: no verifier, no `defaultTrustManager` in the added code | `evidence/I5-checks.txt` |
| IC-10 | Met: the 12 SNI, 8 pinned and 4 pinned create-socket tests pass unedited; SNI diff has no deletions | `evidence/I2-pinned-cases.txt`, `evidence/I3-sni-cases.txt`, `evidence/I3-sni-diff-check.txt`, `evidence/I6-junit-totals.txt` |
| IC-11 | Not triggered; no bug filed | this log |
| IC-12 | Pending: the orchestrator commits the story's own paths before QA; nothing committed here | n/a |

## Integration verification

The new tests are discovered by Gradle's JUnit runner (`:app:testDebugUnitTest`) and ran in the full gate
(I6). No production class, registry or signature is added or changed, so there is no call site to check.

## Contract adherence

The story lists no `integration_contracts`. CNTR-MODERNIZATION-001 was read as context (plan section 8); the
tests neither edit nor produce it.

## Runtime Claims

Empty. The story makes no runtime-provable claim of the crash, concurrency, timing, filesystem or hook
classes. The test behaviour is shown by the executed gate and the two mutation runs above.

## Final state of the tree

`git status --short` (production, build and fixture paths clean) lists, among the SDLC documents that were
already uncommitted at hand-over: `M app/src/test/.../ssl/DefaultTrustedSocketFactorySniTest.java` and
`?? app/src/test/.../PinnedCertificateSocketFactoryExpiryTest.java`; full listing in
`evidence/I6-checks.txt` (taken before this log and the story note were written). Production blobs at the end:
`8a0fbb64fb171624591fbebafb818555f28efd58` and `d543927166b3c5e1da0aaa8ab584072a43bf7521`, equal to the base.

## Runtime claims shown by the evidence

The evidence shows: the new tests pass on unmutated code (three identical runs), and fail under the two named
mutations with the expected failure texts.

IC-8 scan: step I8, result in `evidence/I8-scan.txt`.
