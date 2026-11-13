---
artifact_type: gate-result
story_id: "U-062"
gate_id: "contract-verification"
gate: contract-verification
checkpoint: plan-gate
verdict: PASS
agent: "Solution Architect (separate instance)"
timestamp: "2026-11-12"
iteration: 3
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-019/U-062/plan.md"
base_commit: "c8f04558dff63ab24ed31b3e3e19ee99c5cb8afc"
contracts_checked:
  - CNTR-MODERNIZATION-001
  - CNTR-MODERNIZATION-002
  - CNTR-MODERNIZATION-003
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-005
  - CNTR-MODERNIZATION-006
  - CNTR-MODERNIZATION-007
  - CNTR-MODERNIZATION-008
blockers: 0
observations: 5
---

# Gate Report: gate-result — U-062

## Gate: contract-verification @ plan-gate

## Verdict: PASS

## Summary

The work still crosses no approved contract. Both iteration-2 blockers are resolved.

**B1 (port coverage).** The analysis can now say, for each port operation, whether CNTR-MODERNIZATION-007 can
be preserved:
- Q5.p and the Section 5 Port preservation sub-table cover every implemented `MailTransport` member, plus the
  constructor and `BinaryTempFileBody`.
- The exception-mapping sub-table has exactly one row for each of the contract's five translation rows.

**B2 (deciding a contract change).** No path lets the analysis decide a contract change:
- Predicate P never selects a recommendation row.
- Each P item goes to the product owner with a named change-record route against CNTR-007 (and CNTR-001 where
  the socket factory changes).
- Section 11 rows assume both contracts unchanged.
- The executor creates no change-record file.

**Other checks.**
- Every port name, signature, value type, exception type and line reference the plan cites matches the
  contract and the code.
- CNTR-001 is measured correctly in Q7.
- The process-contract points hold.

**Residual items.** Five non-blocking observations remain, including how to apply plan-review's R1 and R2. All
can go to the implementer as notes.

## Method

- Read the whole revised plan from disk (963 lines, `iteration: 3`). Read in full: Verified Facts
  (`plan.md:60-70`), the per-turn PCR rule (`:210-229`), the Section 5/11/12 skeleton rows (`:279-286`), Q5 to
  Q7 (`:495-595`), Recommendation Logic (`:639-686`), Acceptance Sequencing (`:700-735`) and Contracts
  Verification (`:840-862`).
- Read plan-review iteration 3 (`plan-review.md`, PASS), with R1 and R2 in full (`:177-194`).
- Re-checked every CNTR-007 and CNTR-001 line reference against the contract files read in iteration 2.
  Re-checked every `MailTransport.java`, `K9MailTransport.java`, `BackupImapStore.java` and engine line reference
  against the sources at HEAD `c8f04558`, which is unchanged since iteration 2.
- Read-only access only. Scope verified via grep.

## Iteration-2 Disposition

| Item | Iteration-2 finding | Iteration-3 change (plan location) | Disposition |
|------|---------------------|------------------------------------|-------------|
| B1 | No per-port-operation preservation assessment | Section 5 Port preservation sub-table, 9 rows (`plan.md:279`). Exception-mapping sub-table, 5 rows (`:279`). Q5.p items 1-9 (`:521-564`). Row counts in the I2 and I4 end checks (`:386-388`, `:401-403`). AC-7 extra check (`:752`) | **Resolved** (Check 1) |
| B2 | No change-record route; Section 11 could plan a port change | Predicate P, non-selecting (`:641`, `:653-666`). P items listed as PO decisions (`:675-676`). Section 11 assumes no contract change and labels rows "conditional on CR (P-n)" (`:285`, `:636-637`). Old wording removed. CR timing set (`:720-722`). Contracts Verification restated (`:851-855`) | **Resolved** (Check 1) |
| O1 | CNTR-007 text lags the code | Q5.p drift list (`:521-526`); Section 12 "Follow-ups" (`:286`); routed to the Artifact Librarian outside the story (`:722-724`, `:857-860`) | Addressed |
| O2 | CNTR-001 not named | Q7 CNTR-001 measurement (`:585-593`); AC-9 check (`:754`); Contracts Verification (`:849-850`) | Addressed (Check 3) |
| O3 | Search criteria and `max` trimming | UID SEARCH test covers all four criteria and the trimming (`:508`) | Addressed |
| O4 | `MessageComparator` line reference | Now `:765-776`, `compare` `:769-775` (`:64`, `:509`); verified | Addressed |
| O5 | Unbacked Section 5 inventory claim | Replaced by Q5.p (`:847-848`) | Addressed |
| O6 | Per-turn Phase Completion Report | Rule at `:220-229` | Addressed (Check 4) |
| O7 | Timing of the D4.7 log entry | Logged when the reviews launch without QA (`:705-707`) | Addressed |
| O8 | DES-009 stale contract id | Section 12 Follow-ups; routed to the Librarian (`:722-724`, `:861`) | Addressed |

## Findings

### Check 1: Are B1 and B2 resolved?

**Port preservation rows compared with the contract and the code.** The plan's Verified Facts list the port
lines at `plan.md:65`.

| Row | Code (verified) | CNTR-007 clause | Q5.p coverage | OK? |
|-----|-----------------|-----------------|---------------|-----|
| `checkSettings` | `MailTransport.java:50`, `throws MailException, RequiresLoginException` | `:97-100` | items 1-3 | Yes |
| `openFolder` | `:62` | `:102-107` | items 1-3, item 4 (`closePooledConnections`, `K9MailTransport.java:556-559`), item 3(c) | Yes. Placement per R1 (O-A) |
| `appendMessages` | `:87`, returns `long` | `:109-113` (`void`; drift noted, `:524`) | items 1-2; APPEND capability test | Yes |
| `getMessages` | `:102` | `:115-120` | items 1-2; UID SEARCH test (`:508`), including `getUid()` `:230` | Yes. Verdict rule per R2 (O-B) |
| `fetch` | `:117` | `:122-127`; `FetchSpec` `:157-160` | item 6 (`ENVELOPE_DATE` → `Item.ENVELOPE` if `Item.DATE` is absent; `BODY` → `Item.BODY`) | Yes |
| `closeFolders` | `:128`, no throw | `:129-131`, `:365-367` | item 5 (adapter catch at `K9MailTransport.java:445-454`, verified) | Yes |
| `importMessageBody` | `:149`, no `throws` | Not in contract (drift, `:526`) | item 7 | Yes. Swallowed-exception set: see O-C |
| Public constructor | `K9MailTransport.java:117-125`, `throws MailException` | Clause C-1 `:242-271` (plan cites `:242-244`, the clause heading and signature) | item 8 | Yes |
| `BinaryTempFileBody` | `K9MailTransport.java:129` | `:255`, Rule 5 `:356` | item 8 | Yes |

- **Port members.** All seven implemented port members are covered. With the two construction-side rows, that
  makes the nine rows.
- **Exception-mapping sub-table.** It has 5 rows, which matches the contract's five translation rows exactly
  (`CNTR-007:220-224`):

  | Contract row | Discriminator |
  |--------------|---------------|
  | IMAP-prefix text | Q5.p 3(b) |
  | `AuthenticationFailedException` | item 2, FQN |
  | `XOAuth2AuthenticationFailedException` | 3(a), `getStatus()`; engine consumers `BackupWorker.kt:442`, `RestoreWorker.kt:655` (verified) |
  | Generic `MessagingException` | item 2 |
  | Any other throwable (backstop) | item 2 |

  `RequiresLoginException` is reached through the `AuthenticationFailedException` row, as in the code
  (`K9MailTransport.java:161-162`). The count is right.
- **Allowed verdicts.** The verdict set is closed: "preserved", "preserved with adapter change" or "needs
  contract change (CR)" (`plan.md:562-564`). The table must not propose any interface. This keeps Part B design
  out of Part A.

**Wiring of P.**
- P is set only by a "needs contract change (CR)" row or by a CNTR-001 difference (`:653-654`).
- P never selects a row T1-T4. B, F and G alone select (`:641-642`; AC-14 check `:759`).
- Every P item is a "PO decision needed" entry naming the clause, the finding and the change-record route
  (`:657-662`).
- In `go-with-conditions`, each P item is also a numbered condition tested by "CR-nnn approved" (`:663-664`).
- In `go`, the affected Section 11 rows cannot start before the CR is approved (`:664-665`).
- The CR is raised by the Requirements Analyst or Artifact Librarian after U-062's QA and before any Part B
  design. It stays outside U-062's diff and its IC-4 set (`:720-722`).
- Section 11 rows assume both contracts unchanged and contain no interface design (`:636-637`).
- The I4 consistency check requires each "needs contract change (CR)" verdict to have a P item, and each P item
  to have a source row (`:401-403`).

No path leaves the change decision with the analysis. This matches REQ-MODERNIZATION-019 `:118` and the
CNTR-007 breaking-change policy (`:331-341`).

### Check 2: Names, signatures and lines

Every port reference in the plan was checked character by character against the contract and the code, and all
match:

| Reference | Checked against |
|-----------|-----------------|
| `checkSettings` `:50`, `openFolder` `:62`, `appendMessages` `:87` (returns `long`), `getMessages` `:102`, `fetch` `:117`, `closeFolders` `:128`, `importMessageBody` `:149` (`plan.md:65`) | `MailTransport.java` |
| `XOAuth2AuthenticationFailedException` → `XOAuth2FailedException(e.getStatus(), e)` at `K9MailTransport.java:159-162`, `:176-179`; the prefix text match at `:164,181,212,238,266,395` (`:395` is `translateMessagingException`) | `K9MailTransport.java` |
| Vendored `ImapFolder.java:102`, `ImapConnection.java:400`, `XOAuth2AuthenticationFailedException.java:7-10`, `ImapStore.java:362` | Vendored sources |
| `FetchSpec.ENVELOPE_DATE` (CNTR-007 `:159`) and `FetchSpec.BODY` | `FetchSpec.java:33`, `:40` |
| `MailException`, `TemporaryImapException`, `XOAuth2FailedException`, `RequiresLoginException` | Exact class names |
| `Item.DATE` sites `K9MailTransport.java:731`, `:258`, `BackupImapStore.java:181` | `BackupImapStore.java:181` is `fp.add(FetchProfile.Item.DATE);` |
| CNTR-007 `:82-132`, `:209-224`, `:220-224`, `:242-244`, `:255`, `:331-341`, `:336-337`, `:345-349`, `:345-353`, `:356`, `:365-367` | CNTR-007 |
| CNTR-001 `:84-86`, `:93-96`, `:284-285` | CNTR-001 |

One citation refinement is under O-D.

### Check 3: CNTR-MODERNIZATION-001 in Q7

Q7 measures the right things:
- **The carried type and its signature** (`plan.md:585-590`):
  - the type `com.fsck.k9.mail.ssl.TrustedSocketFactory`;
  - the method `createSocket(Socket, String, int, String) throws NoSuchAlgorithmException,
    KeyManagementException, MessagingException, IOException`, which matches `CNTR-001:84-86` and the vendored
    interface `TrustedSocketFactory.java:10-11`;
  - the check includes the package of `MessagingException`, which matters because Q2 expects that package to
    have moved.
- **The store slot.** Whether the upstream store factory or constructor still accepts the factory (`:591`, tied
  to Q5.p item 8).
- **The producers.** Whether both permitted producers can still be supplied (`:592`). These are
  `DefaultTrustedSocketFactory` and the app-owned `PinnedCertificateSocketFactory` (`CNTR-001:93-96`; app
  `PinnedCertificateSocketFactory.java:55`, `:75`, verified).
- **The routing.** Any difference sets P against CNTR-001 under its breaking-change rule (`:284-285`), which is
  the correct routing.

The upstream default factory lives in an Android app-core module (`plan.md:574`). That means item 3 is a real
question: the app may need a new dependency on that module, or may need to supply its own `SYSTEM_VALIDATED`
producer. Either answer is a finding about how the producer is supplied, not a contract change, unless the
carried type differs.

### Check 4: Process contracts

- **Closing bookend** (`compiled-pipeline.json` `closing.obligations`): unchanged from iteration 2.

  | Artifact | Status |
  |----------|--------|
  | `plan.md` | Exists |
  | `plan-review.md` | Exists (iteration 3, PASS) |
  | `contract-verification.md` | This file |
  | `expert-depth-check.md` | Next gate; warning severity |
  | `implementation-log.md` | Created at I0 |
  | `review-code.md`, `review-security.md` | Acceptance Sequencing step 2 |
  | `qa-results.md` | Step 6, after an `accepted` record |

  All exist at close. If the product owner does not accept, the story stays open by design.
- **Per-turn reports** (`plan.md:220-229`):
  - Every executor turn returns a PCR whose `artifact_path` is the delegation block's `artifact_destination`
    (`implementation-log.md`).
  - I0-I3, including the split turns, return `PASS` / `in-progress` / `implementation` with `NEXT:` in `notes`.
  - Only I4 sets `quality-gate`.
  - `FAIL` is reserved for an increment that cannot reach END; `ERROR` for unreadable inputs.

  This is compatible with CNTR-PIPELINE-006's routing (PASS advances, FAIL blocks).
- **D4.7.** The entry is written when the reviews launch without QA (`:705-707`).
- **Write paths.** The executor's write set is unchanged:
  - the analysis and input copy under `sdlc/artifacts/build/feasibility/U-062/**`;
  - `implementation-log.md` in the workspace;
  - the story's Implementation Notes.

  Gate runs write only gitignored build output. P-item CRs and drift follow-ups are owned by the Requirements
  Analyst, Librarian or orchestrator after QA, outside U-062's diff (`:720-724`).

### Check 5: New blockers

None. The additions are measurement and routing text only. They add no write path, no executed upstream code
and no product edit.

### Plan-review R1 and R2

**R1 is right, and the fix should be extended (O-A).** The `NONEXISTENT` / `did not find message count` text
(`K9MailTransport.java:652-654`) drives the BUG-011/013 retry. That retry is adapter behaviour, not a CNTR-007
translation row. Putting it in the exception-mapping sub-table would make that table six rows and fail the
"5 rows" end check. The implementer should record Q5.p 3(c), together with item 4 (`closePooledConnections`), in
the Notes cell of the `openFolder` port row.

Extension: the generic rule at `plan.md:543-545` says a missing discriminator gives "needs contract change (CR)"
unless an adapter change restores it. That rule must not apply to 3(c). No CNTR-007 clause covers the retry, so
losing the text can never set P. The `openFolder` verdict is "preserved with adapter change": the adapter
re-keys the retry on upstream's error type or text, cited, or uses an equivalent detection. If no detection is
possible, the Notes record it as a behaviour-preservation risk for Part B (REQ-019 Part B "behaviour is
preserved"), with an explicit statement that it is not a contract item.

**R2 is right (O-B).** If a port member's backing capability needs a fork, the member is "preserved with adapter
change". Its Notes cite the Section 5 operation row that carries "needs fork", so the fork is counted once, in F.
The same applies to a non-fork alternative that the adapter can absorb, for example a client-side filter that
returns exactly the `HEADER`/`UNDELETED`/`SENTSINCE`/`FLAGGED` result set and the `max` trimming. "Needs contract
change (CR)" is used only when something visible at the port cannot be kept: a signature, a return type, a thrown
app-owned type, or a discriminator such as `getStatus()`. An example would be the result set seen by
`getMessages` callers having to change. The implementer should apply both of plan-review's sentences verbatim as
the Q5.p verdict rule.

### Non-blocking observations (notes for the implementer)

**O-A — R1 placement plus the 3(c) verdict rule.** As above: the Q5.p 3(c) finding goes in the `openFolder`
row's Notes. A missing folder-not-ready text never sets P. The verdict is "preserved with adapter change", or a
Part B behaviour risk if it cannot be detected.

**O-B — R2 verdict rule.** As above: apply plan-review's two sentences verbatim, with this extension: a non-fork
alternative the adapter fully absorbs is also "preserved with adapter change".

**O-C — `importMessageBody`'s swallowed-exception set.**
- Today the method has no `throws` clause. It catches exactly `MessagingException`, `IOException` and
  `IllegalArgumentException` and returns `MessageImportResult.failure` (`K9MailTransport.java:307-316`).
- In this row's "Throwables" cell, list any upstream exception from fetch or parse that falls outside those three
  types, for example Kotlin-specific runtime exceptions or a relocated `MessagingException`. Such an exception
  would escape the port as an unchecked throw instead of becoming a `failure` result.
- The verdict is "preserved with adapter change" (widen the adapter catch), with the types named.

**O-D — Citation for the backstop exception row.**
- The fifth contract row cites `BackupImapStore.java:129-133` for the `IllegalArgumentException` re-wrap
  (`CNTR-007:224`).
- The live re-wrap is at `K9MailTransport.java:596-600`.
- The exception-mapping row should cite both: the contract's reference and the live site.

**O-E — The DES-009 stale contract id and the CNTR-007 text drift remain open.** This is pre-existing and
correctly routed outside U-062. The orchestrator should confirm at story close that the Librarian follow-up was
actually filed.

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-062 plan (iteration 3) | sdlc/artifacts/build/sprints/sprint-019/U-062/plan.md | Artifact under review (changed sections read in full; grep over all 963 lines) |
| U-062 plan review (iteration 3) | sdlc/artifacts/build/sprints/sprint-019/U-062/plan-review.md | Verdict; R1, R2 |
| U-062 story | sdlc/artifacts/build/sprints/sprint-019/U-062-thunderbird-mail-libraries-feasibility-analysis.md | ACs, Affected Code (read in iteration 2; unchanged) |
| Compiled pipeline | sdlc/artifacts/build/sprints/sprint-019/U-062/compiled-pipeline.json | Closing obligations |
| REQ-MODERNIZATION-019 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-019-replace-vendored-k9-with-thunderbird-mail-libraries.md | Constraints `:118` |
| CNTR-MODERNIZATION-007 | sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md | Port, translation table, C-1, rules |
| CNTR-MODERNIZATION-001 | sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md | Carried type, producers, breaking change |
| CNTR-MODERNIZATION-002..006, 008 | sdlc/artifacts/design/contracts/ | Status and subject |
| Port sources | app/src/main/java/com/zegoggles/smssync/mail/transport/{MailTransport,K9MailTransport,FetchSpec,MailException,TemporaryImapException,XOAuth2FailedException,MailTransportConfig,MessageImportResult}.java | Line and signature verification |
| Legacy store | app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java | `:181` |
| Engine consumers | app/src/main/java/com/zegoggles/smssync/service/{BackupWorker.kt,RestoreWorker.kt} | `status == 400`, `closeFolders` in `finally` |
| Vendored k-9 | k9mail-vendored/src/main/java/com/fsck/k9/mail/{store/imap/*,ssl/TrustedSocketFactory.java} | Discriminators, pool reset, factory interface |
| Pinning factory | app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java | `:55`, `:75` |
| Templates/standards | plugin cache templates/build-artifacts/gate-report-template.md; knowledge/standards/phase-completion-report.md | Format, PCR schema |

## Phase Completion Report
---
story_id: "U-062"
phase: "plan-gate/contract-verification"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-019/U-062/contract-verification.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
