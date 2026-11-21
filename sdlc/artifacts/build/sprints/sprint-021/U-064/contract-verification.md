---
artifact_type: gate-result
gate: contract-verification
gate_id: "contract-verification"
checkpoint: plan-gate
story_id: "U-064"
verdict: PASS
iteration: 2
agent: "Solution Architect (separate instance, contract verification)"
timestamp: "2026-11-21T02:55:40Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-021/U-064/plan.md"
base_commit: "de03e99b51a04d402b64cbd5c7f0d7d19e90f2a9"
contracts_checked:
  - CNTR-MODERNIZATION-001
change_records_read:
  - CR-007
  - CR-008
  - CR-009
  - CR-012
  - CR-013
blockers: 0
warnings: 0
observations: 2
---

# Gate Report: gate-result — U-064

## Gate: contract-verification @ plan-gate

## Verdict: PASS

## Summary

The revised plan (`iteration: 2`, section 0) keeps everything iteration 1 cleared. The contract claims in section 8, every interface call in the test code, and both "After the CR-009 move" texts are unchanged and still correct. Each iteration-1 observation is handled as recorded in section 0, and section 8 now says so in substance.

The one test-code change is the message of AC-2's final assertion. It adds a "names tried" list built with the fully qualified `java.util.Arrays.toString(names)`. It changes no assertion and touches nothing the contract governs.

The new and changed decision-table rows are S1, S3, S6, S13, S14 and S15. None of them contradicts CNTR-MODERNIZATION-001 or the target text of an approved change record, and none edits a contract or a change record.

Base state was confirmed this session. HEAD is `de03e99b…`. `git status --porcelain` shows nothing under `app`, `k9mail-vendored` or the contracts folder. The only change under the change-records folder is the uncommitted `CR-013` modification, which was already present when this gate began in iteration 1. The contract still has no SNI text, so neither CR-012 nor CR-013 has been applied.

## Findings

### 1. The iteration-1 PASS still holds

| Item | Iteration 2 | Result |
|---|---|---|
| Contract claims (section 8): `:96` "SHA-256 fingerprint equality + validity-window check"; `:197` "SHA-256 mismatch or outside validity window" and "Connection fails closed"; no SNI text in the contract | Same claims, same lines; re-checked against the contract file | Holds |
| AC-1 test code (plan I2, `:253-343`) | Byte-for-byte the iteration-1 code: public constructor `(Context, String, X509Certificate)`, `createSocket(null, RIGHT_HOST_NAME, server.port(), null)` outside the `try`, rejection expected from `startHandshake()`, direct cause `CertificateExpiredException`, no "Pinned certificate mismatch" text | Holds |
| AC-2 and AC-3 test code (plan I3, `:409-495`) | Same calls: `setSniViaSSLParameters` (public static), `isIpOrBlankHostname` by reflection (package-private static), `new DefaultTrustedSocketFactory(Context).createSocket(null, "bad_host.example", 993, null)`; only the final message changed (section 3 below) | Holds |
| Helper use (`LoopbackTlsTestServer.start/port/connect/close`, `Mode.IMPLICIT_TLS`, `TlsTestFixtures.certificate/serverContext` and constants) | Unchanged; signatures as verified in iteration 1 (`LoopbackTlsTestServer.java:58-63`, `:93`, `:122`, `:147`, `:167`; `TlsTestFixtures.java:101`, `:280`, `:300`, `:329`, `:338`, `:354-355`) | Holds |
| "After the CR-009 move" text | Class Javadoc bullet (plan `:376`, word for word from `PinnedCertificateSocketFactoryCreateSocketTest.java:96-98`), and the SNI comment block (plan `:435-437`) | Holds |
| No contract edit or change record needed | Section 6 out-of-scope list unchanged; row S1's new clause says "the contract and the change record are not edited" | Holds |

### 2. How the iteration-1 observations were handled

| Obs. | Iteration-1 point | Handling in iteration 2 | Assessment |
|---|---|---|---|
| O1 | S5 was quoted as CR-012's candidate wording | Section 8 (`:822-826`) quotes S5 word for word from CR-013 `:317`. It attributes "the factory skips SNI and the connection continues" to CR-012 `:292`, which I checked this session, and notes that CR-013 kept it as S5 (CR-013 `:361`) | Resolved |
| O2 | CR-012's held status was not stated, and CR-012 and CR-009 were missing from Artifacts Consulted | Section 8 (`:809-816`) says CR-012 is approved and held (Record 7; CR-013 `:46`), that the contract still carries the v1 text, that AC-1 follows CR-012's target, and that reviewers read the v1 wording as a known erratum. Artifacts Consulted now lists CR-012 (`:867`) and CR-009 (`:868`). The CR-009 row says honestly that the planner named it but did not re-read it | Resolved. The CR-009 row is accurate disclosure, and this gate read CR-009 in full |
| O3 | If the S1 fallback is needed, it disproves CR-012's premise | Row S1 (`:712`) adds: report it in the hand-off as a finding against CR-012's target text, which assumes the direct cause is the `CertificateException` (CR-012 `:247`), for the orchestrator to route; the contract and the change record are not edited. Section 8 `:816-818` says the same | Resolved. The routing respects the "no edit before the combined revision" rules (CR-012 `:327`, CR-007 `:219-220`) |
| O4 | AC-2 (iii) is stricter than S5 and binds U-065 | Section 8 (`:832-839`) records it, citing the existing BUG-015 observable (`DefaultTrustedSocketFactorySniTest.java:180`, `:195`, `:209`, `:224`, `:238`) and U-065 `:221`. The test is unchanged | Resolved as a recorded constraint. Passing the note to U-065's planner is the orchestrator's job; no contract or change-record action is involved |
| O5 | Gaps in S5 coverage (layered shape, server name at `createSocket` level); CR-013 terminology slip | Left as informational. Section 8 `:829-830` now names the gap and points to CR-013 Scope Concern D | Accepted |
| O6 | A renamed guard fails at runtime under reflection | Left as informational; already covered by the "After the CR-009 move" rule | Accepted |

### 3. The test-code change: AC-2's final assertion message

The iteration-2 text (plan `:475-478`):
```java
com.google.common.truth.Truth.assertWithMessage(
        "setSniViaSSLParameters must not throw for a name SNIHostName rejects; names tried: "
        + java.util.Arrays.toString(names) + "; escaped")
        .that(escaped).isEmpty();
```
- **Interface.** The plan calls `java.util.Arrays.toString(Object[])` on a `String[]`. It is fully qualified, so no import line changes, and the plan's one-hunk, additions-only rule holds. `Truth.assertWithMessage(String)` and `.that(List).isEmpty()` are unchanged.
- **Contract.** The message is diagnostic text only. The assertion is still "`escaped` is empty", which is AC-2 (ii) and the "`createSocket(...)` does not throw because of it" clause of S5 at helper level. No contract or change-record text is affected.
- **Mutation evidence.** I4's M-N2 expectation (`:562-565`) now says the evidence is the `escaped` value ("but was" part), not the "names tried" part, which always lists all three names. That is the correct reading. It keeps the M-N2 detection tied to escaped `IllegalArgumentException`s, which is what S5 is about.

### 4. New and changed decision-table rows

| Row | Change | Against the contract and approved change records |
|---|---|---|
| S1 | Adds reporting against CR-012's premise; no edits | Consistent (see O3) |
| S3 | Also covers AC-2 (i) failing in the sandbox. Replacing `bad_host.example` in AC-3 becomes a recorded deviation for the orchestrator to accept | No contradiction. CR-013 S5 names no host names. Its "Not stated" list (`:334`) says S5 covers whatever names the platform rejects, so a substitute name pins the same statement. The deviation is from the story's literal AC-3, not from a contract, and the plan routes it correctly |
| S6 | A repeat during a mutation run is named as a deviation from IC-7(d) | Process only; no contract bearing |
| S13 | A mutation failure that does not match the expectation is `MISMATCH`, not DETECTED; stop | Strengthens the evidence that AC-1 and AC-2/AC-3 detect the contract-relevant removals (`:149`, `:248-253`). No contract bearing beyond that |
| S14, S15 | IC-8 scan errors and hits (I8) | Evidence hygiene; no contract bearing. S15 sends hits in other agents' gate files and in `plan.md` to the orchestrator, so this file is not edited by the developer |

None of these rows asserts, relaxes or redefines a contract obligation. None permits an edit to CNTR-MODERNIZATION-001 or to CR-007, CR-008, CR-009, CR-012 or CR-013.

### Observations (non-blocking)

| # | Severity | Observation | Suggested handling |
|---|---|---|---|
| N1 | Info | Plan I3 prose (`:402-405`) says the added code fully qualifies "the four types the file does not import" and lists `RuntimeEnvironment`, `Truth`, `ArrayList`, `Socket`. The iteration-2 message adds a fifth, `java.util.Arrays`. The code is correct and compiles; only the count in the prose is stale | Fix the prose if the plan is revised for another reason; no revision needed for this alone |
| N2 | Info | Section 0 says the only test-code change is AC-2's message (O-3). I confirmed this by comparing the I2 and I3 code blocks with the iteration-1 text this gate reviewed | None |

## Artifacts consulted (this iteration)

| Artifact | Path | Purpose |
|---|---|---|
| U-064 plan, iteration 2 | `sdlc/artifacts/build/sprints/sprint-021/U-064/plan.md` | Section 0, I2 and I3 code, I4 M-N2 expectation, decision table section 4, section 8, Artifacts Consulted |
| CNTR-MODERNIZATION-001 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md` | Re-check of `:96`, `:191-197`; confirmed unapplied (no SNI text) |
| CR-012 | `sdlc/artifacts/build/change-records/CR-012-cntr-001-rejection-surfaces-at-handshake.md` | `:247`, `:292` (re-checked) |
| CR-013 | `sdlc/artifacts/build/change-records/CR-013-cntr-001-state-sni-behaviour.md` | `:46`, `:317`, `:334`, `:361`, `:378` |
| CR-007, CR-008, CR-009 | `sdlc/artifacts/build/change-records/` | Read in full in iteration 1; their timing rules re-applied here |
| Code and helpers | `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`, `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`, `app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java`, `LoopbackTlsTestServer.java`, `DefaultTrustedSocketFactorySniTest.java`, `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java` | Read in iteration 1. They are unchanged at the base commit (no working-tree changes), so the interface table stands |

Commands run (read-only): `git rev-parse HEAD`, `git status --porcelain` on the production, vendored, contracts and change-records paths, `sed -n` on CR-012 line 292, `date -u`, and content searches with the Grep tool. Gradle was not run. Nothing was edited except this file.

## Phase Completion Report
---
story_id: "U-064"
phase: "plan-gate/contract-verification"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-021/U-064/contract-verification.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
