---
artifact_type: gate-result
gate: contract-verification
gate_id: "contract-verification"
checkpoint: plan-gate
story_id: "U-065"
verdict: PASS
iteration: 2
agent: "Solution Architect (separate instance, contract-verification reviewer)"
timestamp: "2026-11-21T04:38:30Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-021/U-065/plan.md"
base_commit: "b2c2dfd8d7f0f5de8627575e160215b24b7cc5d9"
contracts_checked:
  - CNTR-MODERNIZATION-001
  - CR-007
  - CR-008
  - CR-009
  - CR-012
  - CR-013
---

# Gate Report: gate-result — U-065

## Gate: contract-verification @ plan-gate

## Verdict: PASS

## Summary

The PASS from iteration 1 still holds for the revised plan (`iteration: 2`). Its production code is identical to iteration 1, apart from comment text in D3 and D5. The calls the pinned factory and the tests make into the default factory are unchanged and match the real signatures at `b2c2dfd8`. The default factory still satisfies CR-013's S1 to S6, and the pinned factory still makes alternative (b) true as written.

All nine iteration-1 observations (O1 to O9) are handled in the plan. None of the affected records is edited (`git status --porcelain -- sdlc/artifacts/design sdlc/artifacts/build/change-records app k9mail-vendored` is empty). The changes made for the other gates introduce nothing that contradicts the contract or an approved change record: the withdrawn guard edits, the D6 stop with its two outcomes, and the revised IC-1 steps.

Method: I read the plan's section 0, sections 1.2 to 2.4 in full, the revised 3.5 (Handshake class), D6 in the decision table, I10, sections 7 to 9, section 12 and the verdict. The source texts were the same as in iteration 1: the contract, CR-007, CR-008, CR-009, CR-012, CR-013 and both factories at `b2c2dfd8`. I used read-only `git` and `grep` only, ran no Gradle, and edited nothing except this file.

## Findings

### 1. The iteration-1 verdict, re-checked

| Check | Iteration 2 | Result |
|---|---|---|
| Production code identical apart from comments | D1, D2, D4, D6 and P1 to P4 are textually the same as in iteration 1. D3 now keeps the existing comment, adds two sentences, and its code line is still `enabledProtocols = protocolsToEnable(sock);`. The `old_string` "`* only enabled by default on API levels 20+.`" occurs once in the base file (`grep -c` = 1), so the edit is unique. D5's method body is unchanged; only its Javadoc gains one sentence on API levels | Holds |
| Nothing the contract states is changed (carried type and signature `:84-86`; `:89-91`; producer rows `:95`, `:96`; enum; payload; handoff and seam; Invariants 1 to 5; error table and prohibited behaviour; Validation Rules 1 to 5; breaking list; dependencies) | Same edits as iteration 1. The pinned trust code `:77-80` and `:117-168` and the default `createSocket` `:152-180` and `:191-266` are outside every edit | Holds |
| Interface calls | `DefaultTrustedSocketFactory.hardenSocket(SSLSocket)` (made `public static` by D6) and `setSniHost(SSLSocketFactory, SSLSocket, String)` (`:191`, already public) from P4. The tests reach `protocolsToEnable(SSLSocket)`, `hardenSocket(SSLSocket, String[], String[])` and `ALLOWED_PROTOCOLS` by reflection with matching parameter lists. `ALLOWED_PROTOCOLS` is declared before the static initialiser | Holds |
| CR-013 S1 to S6 for the default factory | The server-name code is untouched, and the step order is unchanged (`hardenSocket` before `setSniHost`) | Holds |
| CR-013 alternative (b) for the pinned factory | P4 calls both steps after both branches, using the `host` argument. J3, J5, J6 and J7 are unchanged. Plan 8.1 now cites the alternatives as starting at `:389` | Holds. (b) stays true as written on the layered shape too, because S1 to S6 describe what the factory itself sets and "Not stated" (`:324-326`) covers names the provider adds |

### 2. What changed for the other gates, checked against the contract and the change records

| Change | Effect on the contract or a change record | Result |
|---|---|---|
| D3 and D5 comment text (EDC-2), adding the claim that TLS 1.2 is supported from API 16 and enabled by default from API 20 | Javadoc only; it states nothing the contract or CR-013 states or contradicts. CR-013's "API level 24 and later" scope is untouched | No conflict |
| Guard edits withdrawn: the A1-G2 guards in the Hardening and Handshake classes stay byte-identical, and the Javadoc records that the `SSLv2Hello` clause can no longer fire | Test-only. The contract and the change records say nothing about isolation guards. Leaving the guard code unchanged matches the story's AC-6 (d) and (e) | No conflict |
| D6 is now a stop after I2. Under D6-a the refusal helpers would catch `java.io.IOException`, assert it is an `SSLException` or the observed class X, and keep the "not a certificate rejection" assertion. D6-b writes no tests for that version | Test-only, and it changes no production behaviour. In both outcomes the refusal surfaces at the handshake, not from `createSocket`, which matches CR-012 (`:198-199`) and the contract's fail-closed rule (`:196-202`). A non-`SSLException` refusal would belong to CR-012 Scope Concern B's territory (a non-certificate failure that is not wrapped as a `MessagingException`). The candidate error-table row in I10 item 4 should then name the observed class, not only `SSLException` (see O-new-1) | No conflict |
| IC-1 revised: list devices first; stop and ask the owner if there is more than one device or the AVD name differs; quoted `date` command; empty-`T` check | Operational only; no contract or change-record bearing. It does not touch the trust policy or the factory selection | No conflict |
| Mutation rows re-derived (plan-review B-1); D30 type matching; I5 stores blobs with `git hash-object -w` | Developer procedure; no contract bearing. M-E4 and M-E5 (the trust mutations) are kept | No conflict |

### 3. How the iteration-1 observations were handled

| Id | Iteration-1 observation (short) | Resolution in iteration 2 | Status |
|---|---|---|---|
| O1 | No record carries the protocol floor and cipher handling; BUG-020 AC-5 open | I10 "Items for the orchestrator", item 1: both options and the closing question, as given in section 4 below; 8.2 row 1 points there; section 7 keeps AC-5 "NOT satisfied here" | Resolved (hand-off) |
| O2 | CR-013's second pass needs more than the subsection swap | I10 item 2: the dated-note text in full, as below | Resolved (hand-off) |
| O3 | Layered pinned name rests on reading the code | I10 item 2: the suggested sentence for the pinned subsection | Resolved (hand-off) |
| O4 | CR-009 lines; "silently" too strong | 2.2 and R5 reworded (CR-009's zero-references check catches P1's import "if it is run"; R5 likelihood lowered to Low). I10 item 3 holds the checklist line, T11, the T5 rewording and the stale rows `:87`, `:234` | Resolved |
| O5 | Initialiser-failure branch: `createSocket` may throw an unchecked exception | 2.1 states it, citing CR-012 `:198`, and names it CWE-636 fixed for protocols. I10 puts it in the Implementation Notes with the N2 result | Resolved |
| O6 | Protocol refusal is a non-certificate failure; no error-table row | I10 item 4 (candidate row, citing CR-012 `:305`) | Resolved (hand-off) |
| O7 | "Backward-compatible" holds only by elimination | 8.1 reworded accordingly; I10 item 1 asks for a Versioning line | Resolved |
| O8 | "Two public statics made public for testing" inaccurate | 2.2 corrected: only `setSniViaSSLParameters` is documented as public for testing (`:230-231`); `setSniHost` `:191` is public and has a vendored caller | Resolved |
| O9 | The planner read the contract only in part | Section 12 says so and refers to this gate's full reading | Resolved |
| Citation | CR-013 alternatives at `:398-411` | Now `:389-411` (section 12, 8.1) | Resolved |

### 4. Text the orchestrator needs for the product owner (kept in full; no record is edited by this story)

**BUG-020 AC-5 and the missing record.** No approved change record states protocol versions or cipher suites for either factory. CR-013 `:332` ("They are decided and stated separately") and CR-012 `:300` ("is a further record then") point at a record that does not exist. Record 9 gives no reason not to state them; its rationale reads "one set of rules to test and to state in the contract". One question, two options:
1. *Raise one change record now (recommended).* A DCR against CNTR-MODERNIZATION-001, a sibling of CR-013, applied in the same combined revision after CR-013. It states for both factories:
   - no protocol older than TLS 1.2 is enabled;
   - TLS 1.2 and TLS 1.3 are enabled where the platform supports them;
   - a protocol name not in the allowed list is never enabled;
   - the cipher suites are the platform's enabled suites with the blocked list removed and the known suites first, in the recorded order;
   - the pinned factory applies exactly the default factory's handling.

   It also adds a Validation Rule like CR-013's rule 6, and a Versioning line saying whether such a change is a contract revision (today the contract's backward-compatible list does not name it; the change is backward-compatible only by elimination). It may also carry the candidate error-table row: a server limited to an old protocol makes the handshake fail with a non-certificate exception, which per CR-012 Scope Concern B leaves `ImapConnection.open()` as an `IOException`. If D6-a is taken, the row should name the observed class.
2. *Do not state them in the contract.* A dated note to Record 9 (or a new record) gives the reason, which meets AC-5's "or the AC-1 record says why it is not". CR-013 `:332` is then reworded at the revision.

Separately: whether BUG-020 may close with AC-5 open (pointing to the chosen option and to CR-013 (b)) or stays open until the revision is the pipeline's and the product owner's call.

**CR-013, suggested dated note (orchestrator to add; not done here):** "At the second pass with alternative (b): extend rule 6 to both producers; replace the Edit 4 pinned clause ('the pinned producer's server-name handling is not yet stated'); name the U-065 pinned tests (`pinnedFactory_createSocket_implicit_setsDnsHostAsOnlyServerName_jdkProvider`, `pinnedFactory_createSocket_ipLiteralAndBlankHosts_setNoServerName_jdkProvider`, `pinnedFactory_createSocket_nameRejectedBySniHostName_doesNotThrowAndSetsNoServerName_jdkProvider`, `pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider`); alternative (a) no longer describes the code (U-065)." Suggested sentence for the pinned subsection: "For the socket layered over a connected socket, the statements rest on reading the code: the test providers set a server name on that socket themselves, so no test can tell the factory's step from the provider's (CR-013 Scope Concern D; U-065 probe)."

**CR-009, suggested additions (orchestrator to add; not done here):**
- Checklist line: "`PinnedCertificateSocketFactory.createSocket` calls `DefaultTrustedSocketFactory.hardenSocket` and `setSniHost` (U-065, commit `<fix>`); retarget both calls and the import to the app-owned default factory in the same change; the shared step may become package-private once both classes share a package."
- Test row T11: "Pinned/default parity — protocols (set and order), cipher suites (order) and server names of the two factories' sockets are equal per shape and provider (U-065 `createSocket_*_appliesDefaultFactoryProtocolAndCipherHandling`, `pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider`) — Exist; retarget."
- T5 reworded: "`hardenSocket`, `protocolsToEnable`, `reorder`: blocked and ordered cipher suites; protocols limited to `ALLOWED_PROTOCOLS` from the supported list — Exist (U-063, U-065)."
- Note that rows `:87` and `:234` ("producer 2 unchanged") no longer describe the code: the pinned factory now depends on the default factory's class for its protocol, cipher and server-name handling.

### 5. New observations (non-blocking)

| Id | Severity | Observation | Suggested action |
|---|---|---|---|
| O-new-1 | Low | If D6-a is taken, the refusal reaches the client as a class X that is not an `SSLException`. I10 item 4's wording ("a non-certificate `SSLException`") would then be incomplete for that version | When forwarding item 4, the orchestrator uses the observed class from the D6 reading |
| O-new-2 | Info | D5's Javadoc now says every supported Android version "enables [TLS 1.2] by default". This is outside the contract and CR-013, which make no claim about API levels below 24 beyond "not stated" | None |

## Phase Completion Report
---
story_id: "U-065"
phase: "plan-gate/contract-verification"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-021/U-065/contract-verification.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
