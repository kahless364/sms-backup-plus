---
artifact_type: gate-result
story_id: "U-062"
gate: plan-review
gate_id: "plan-review"
checkpoint: "plan-gate"
verdict: PASS
iteration: 3
agent: "Solution Architect (separate_instance reviewer)"
timestamp: "2026-11-12"
reviewed_artifact: "sdlc/artifacts/build/sprints/sprint-019/U-062/plan.md"
base_commit: "c8f04558dff63ab24ed31b3e3e19ee99c5cb8afc"
blocking_findings: 0
non_blocking_observations: 6
---

# Gate Report: gate-result — U-062

## Gate: plan-review @ plan-gate

## Verdict: PASS

## Summary

I re-read the whole of plan.md (963 lines, iteration 3) from disk, and the contract-verification result that
prompted this iteration.

- **Additions are sound.** The Port preservation and exception-mapping sub-tables (Q5.p), predicate P, the
  CNTR-001 measurement and the Section 11 "conditional on CR" labels keep AC-1 (twelve numbered sections) and
  AC-7 (eight operation rows) literally satisfied.
- **Decision table still holds.** Re-enumerated with P and S as annotations, T1-T4 is total and has no
  conflicting rows.
- **Citations are correct.** I opened every new Verified Facts row and every new file:line (41 locations
  across `MailTransport.java`, `K9MailTransport.java`, `BackupWorker.kt`, `RestoreWorker.kt`, the vendored
  IMAP sources, CNTR-007, CNTR-001, REQ-019 and DES-009). All are correct.
- **Q5.p is executable and stays in scope.** It states facts and verdicts only, and its closing paragraph
  forbids interface proposals.
- **Iteration-2 findings are resolved.** N1-N7 are all resolved. I tested the widened Q4 grep and Q8 regex
  against this repository's own sources. Both now do what they claim, with small residual misses. Those
  misses, and four other points, are observations below.
- **Still followable.** The plan remains followable at 963 lines: every increment step names the Question
  Guide entries it depends on.

## Iteration-1 disposition

| ID | Iteration-1 finding | Status |
|----|---------------------|--------|
| B1 | The decision table could not reach `go`; R5 conflicted with R7 | Resolved in iteration 2; still resolved (see re-enumeration below) |
| B2 | The third-gap test did not decide the gap | Resolved in iteration 2; still resolved (l.509) |
| B3 | The IC-3 `-F` path check missed single-backslash paths; the guard failed silently | Resolved in iteration 2; still resolved (l.773-798) |
| B4 | The at-acceptance reference edits fell outside IC-1/IC-4 | Resolved in iteration 2; still resolved (D4.6 l.149-162, step 4 l.717-723) |
| O1-O15 | Observations | All applied in iteration 2; still present |

## Iteration-2 disposition (N1-N7)

| ID | Iteration-2 finding | Status | Evidence in plan.md (iteration 3) |
|----|---------------------|--------|-----------------------------------|
| N1 | Name all three `Item.DATE` sites | **Resolved** | l.64 and l.509 test (ii) name `:731` (live), `:258` (port; test-only callers) and `BackupImapStore.java:181` (legacy). They state that a hit at the last two is not a refutation, and that `ENVELOPE_DATE` must map to ENVELOPE (CNTR-007 `:159`, verified) |
| N2 | (f) missing from the Experiment log; no invocation or JDK | **Resolved** | l.114-121 (scratch `settings.gradle`/`build.gradle`; `env -u … GRADLE_USER_HOME=$S/gh <repo>/gradlew -p $S/f --no-daemon dependencies`; JDK and Gradle recorded); l.288 (Experiment log covers (a)-(f)) |
| N3 | Control lines must be disjoint by category | **Resolved** | l.782-784, with a placeholder profile name. With disjoint lines, `grep -c` yields 4 (paths), 1 (e-mail) and 2 (hostname and user name); confirmed |
| N4 | The Q4 grep flags Kotlin stdlib calls; `java.nio.file` missing | **Resolved** | l.479-484: classify by language and receiver; `java.nio.file` added. Residual: R4 |
| N5 | The Q8 regex missed `final`, Java `enum` and `fun interface` | **Resolved** | l.609, with a 5-file sample check recorded. Tested on this repo (263 main source files): it matches a top-level declaration in 259 files; of the four misses, two are `package-info.java` (no class) and one is a Kotlin `annotation class`. Residual: R3 |
| N6 | Stale D2 wording | **Resolved** | l.100 (no `log -S`); l.102 (the table now also turns on gating platform conflicts) |
| N7 | The retro item should name the story locations | **Resolved** | l.160-162 (lines 57, 99, 102, 106-115) |

## Checks requested for iteration 3

**(1) AC-1, AC-7, and re-enumeration of T1-T4.**

- **AC-1.** The skeleton is unchanged (l.246-261). The port material sits under `### Q5.p …`, a level-3
  heading starting with "Q" (allowed by l.267-268), so `grep -nE '^## [0-9]+\. '` still gives 12.
- **AC-7.** The skeleton states that Q5.p is "a sub-table of Section 5, not a ninth AC-7 row and not a
  numbered section" (l.279). The AC-7 verification row keeps "8 rows" and adds the 9 + 5 sub-table check as
  extra (l.752).
- **Predicates.** Only B, F and G select a row (l.641-642).

  | (B, F, G) | Row |
  |-----------|-----|
  | (1, *, *), 4 states | T1 |
  | (0, 1, *), 2 states | T2 |
  | (0, 0, 1), 1 state | T3 |
  | (0, 0, 0), 1 state | T4 |

  That is 8 states, each in exactly one row. S and P are annotations that never select a row.

- **P against each row (l.663-666).** I checked P in all four rows, plus every combination of P with S:
  - In `go-with-conditions` (T1, T2, T3, or T4 by choice), each P item is also a numbered condition with the
    test "CR-nnn approved".
  - In `go` (T4 only), each P item stays a "PO decision needed" item, and Section 12 says the affected
    Section 11 rows wait for the CR.
  - In `no-go`, P items are recorded for completeness.
  - No combination gives two rules that conflict, and none forces a value. `go` stays reachable in T4 with P
    set, which is consistent with REQ-019 `:118`: a port change is decided by a change record, not by the
    analysis.
  - The AC-14 consistency rules are unaffected, because F still carries the Section 3 and Section 5 triggers
    and B the Section 7 blocker.

**(2) New Verified Facts rows and file:line citations. All 41 opened; all correct.**

- **`MailTransport.java`** `:50, :62, :87` (returns `long`), `:102, :117, :128, :149`.
- **`K9MailTransport.java`:**
  - `:129` `BinaryTempFileBody.setTempDirectory`;
  - `:159-162` and `:176-179` (`XOAuth2AuthenticationFailedException` → `XOAuth2FailedException(e.getStatus(), e)`;
    `AuthenticationFailedException` → `RequiresLoginException`);
  - the `"Unable to get IMAP prefix"` match at `:164, 181, 212, 238, 266, 395`;
  - `:230` `msg.getUid()`;
  - `:304-305` converter calls, `:307-316` failure returns;
  - `:445-454` `closeFolders` swallow;
  - `:556-559` `forceFreshConnection` → `closePooledConnections()`;
  - `:652-654` `NONEXISTENT` / `did not find message count`;
  - `:727-744` `max` trimming through `reverse`;
  - `:749-758` `buildSearchQuery`;
  - `:765-776` `MessageComparator`, with `compare` at `:769-775`.
- **Engine:** `BackupWorker.kt:442` and `RestoreWorker.kt:655` (`e.status == 400`); `BackupWorker.kt:377` and
  `RestoreWorker.kt:277` (`transport.closeFolders()`).
- **Vendored IMAP:** `ImapFolder.java:102` (throws the prefix text); `XOAuth2AuthenticationFailedException.java:7-10`
  (constructor taking `JSONObject`); `ImapConnection.java:400` (throw site); `ImapStore.java:362`
  (`closePooledConnections`).
- **CNTR-007:**
  - `:82` interface; `:109` `appendMessages … : void`; `:159` `ENVELOPE_DATE == Item.DATE`;
  - `:209-224`, with exactly 5 translation rows at `:220-224`;
  - `:242-244` Clause C-1; `:255` `BinaryTempFileBody`;
  - `:331-341` breaking-change policy; `:336-337` "contract defect";
  - `:345-353` Validation Rules 1-3; `:356` Rule 5; `:365-367` `closeFolders` no-throw.
- **CNTR-001:** `:84-86` carried type and signature; `:93-96` producers table (header at `:93`, pinned
  producer at `:96`); `:284-285` breaking change on the carried type.
- **Requirements and design:** REQ-019 `:118` (the port changes only if Part A shows it must); DES-009 `:395`
  (stale `CNTR-MODERNIZATION-009` id).

**(3) Q5.p executability and scope (l.521-564).**

- **Executable.** Each of the nine numbered items names either the upstream thing to cite or a literal
  `git -C $TB grep` command, and maps to a table column. The three verdict values are closed (l.562).
- **No design.** The closing sentence forbids proposing "new interface, signature or type". Item 3(a)'s "could
  the adapter derive one (cite the source it would parse)" is a factual feasibility question, not a design.
- **Known drift.** Pre-existing CNTR-007 drift is reported, not fixed (l.522-526, l.857-865).
- Two placement ambiguities could let a literal agent mis-fill a row (R1, R2). Both are caught by the I2 row
  counts or the code review, so neither is a blocker.

**(4) Q4 grep and Q8 regex.**

- **Q4.** Run against this repo's sources, the grep's only hit is `EnumSet.of(` (`State.java:78`), a false
  positive the classification step (l.480-484) discards. It does what it claims (residual: R4).
- **Q8.** See N5 above. It now covers `final`, `static`, Java `enum`, `fun interface`, `record` and
  `@interface`, and checks in `git grep -c` form at `HEAD` (one per top-level file, nested classes correctly
  excluded). It still misses Kotlin `annotation`/`value`/`inline` and KMP `expect`/`actual` declarations
  (R3), which matter for the upstream KMP modules. Q8 is a labelled estimate, and the 5-file sample check
  should surface the gap, so this is an observation.

**(5) N1-N7.** All seven resolved (table above).

**(6) Internal consistency and followability.**

- **Cross-references match.** The skeleton (l.279, l.285-286), the Question Guide (Q5.p, Q7, Q11), the
  Recommendation Logic (P), the increment steps, the verification table, the reviewer checklists and the
  acceptance sequencing all agree:
  - I2 names Q5.p and the 9 + 5 row counts (l.386-389);
  - I3 names the "conditional on CR (P-n)" labels and the P items (l.393-394);
  - I4 cross-checks every CR verdict against a P item (l.402-403);
  - the AC-7, AC-9, AC-13 and AC-14 rows carry the additions (l.752-759);
  - CR, SR and QA each have the new checks (l.806-808, l.814-817, l.823);
  - acceptance step 4 orders P-item CRs after U-062's QA and outside its IC-4 set (l.720-722), consistent
    with l.662.
- **Step-to-guide pointers.** Each increment points to its Question Guide entries: I1.3 → Q1-Q3; I2 → Q4,
  Q5 + Q5.p, Q6-Q8; I3 → Q9-Q11 + Recommendation Logic.
- **One pointer is weak.** The I0 and I1-I4 end checks say "IC-3 grep" rather than "the IC-3 procedure"
  (R5). An executor could run a bare grep without the positive control.

## Findings

### BLOCKERS

None.

### OBSERVATIONS (hand to the implementer as notes)

**R1. Where Q5.p item 3(c) belongs.** Item 3 is headed "Discriminators (exception-mapping sub-table)", but its
part (c), the `NONEXISTENT` / `did not find message count` folder-open retry text, corresponds to no CNTR-007
translation row. The sub-table has exactly 5 rows (`:220-224`).

*Fix:* record 3(c), with item 4 (`closePooledConnections`), in the `openFolder` row's Notes. Do not add a sixth
exception-mapping row. The I2 end check "5 exception-mapping rows" (l.388) would otherwise fail.

**R2. Port verdict when preservation depends on a forked capability.** Take `getMessages`: if upstream has no
arbitrary-header search, the Section 5 UID SEARCH row is "needs fork". The plan does not say which of the
three port verdicts the matching port row takes.

*Fix:* add to Q5.p:
- "A port member whose backing call needs a fork is 'preserved with adapter change'. Its Notes cite the
  Section 5 operation row that carries 'needs fork', so F captures the fork and P does not."
- "Use 'needs contract change (CR)' only when the member's signature or observable semantics (return type,
  thrown app-owned type, a discriminator such as `getStatus()`) cannot be kept."

This stops one fork from being counted twice, once in F and once in P.

**R3. Residual misses in the Q8 class-count regex (l.609).** It misses Kotlin `annotation class` (for example
`di/IoDispatcher.kt:18` in this repo), `value class`/`inline class`, and KMP `expect`/`actual` declarations,
which occur in upstream `commonMain`/`jvmMain`.

*Fix:* extend the modifier group to
`(final |abstract |open |data |sealed |static |annotation |value |inline |expect |actual )*`. Record the
5-file sample check on at least one KMP `commonMain` file.

**R4. Q4 grep false positive (l.479).** `(List|Map|Set)\.of\(` also matches `EnumSet.of(` (API 1), as at
`State.java:78` here. Classification discards it, but anchoring saves work.

*Fix:*
- Use `\b(List|Map|Set)\.of\(`.
- Spell out the KMP pathspecs ("plus the `commonMain`/`jvmMain` sets", l.480), for example
  `core/common/src/commonMain core/common/src/jvmMain legacy/logging/src/commonMain feature/account/api/src/commonMain feature/mail/folder/api/src/commonMain`,
  confirmed with `git -C $TB ls-tree` at the pin.

**R5. End checks should cite the IC-3 procedure, not "IC-3 grep".** I0.2, the I0 end checks, I1.3, I2/I3
("as at I1") and I4 step 4 say "IC-3 grep" (l.342, l.364, l.382). Only the procedure (l.773-798) carries the
positive control and the fail-closed guard.

*Fix:* replace "IC-3 grep" with "the IC-3 procedure (control 4/1/2, then the three greps)" in each.

**R6. Path form for experiment (f)'s Gradle home (l.119).** `GRADLE_USER_HOME=$S/gh` passes a POSIX path to a
native JVM. Git Bash normally converts path-like environment values for native programs, but nothing
confirms it took effect.

*Fix:* use `GRADLE_USER_HOME="$(cygpath -w "$S/gh")"`, or check after the run that `$S/gh` contains
`caches/` and that no stray directory appeared at the drive root. Record the result in the (f) entry, without
the path value.

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| Plan under review (iteration 3) | `sdlc/artifacts/build/sprints/sprint-019/U-062/plan.md` | Subject (read in full) |
| Contract-verification result | `sdlc/artifacts/build/sprints/sprint-019/U-062/contract-verification.md` | Rework drivers (read in full) |
| U-062 story | `sdlc/artifacts/build/sprints/sprint-019/U-062-thunderbird-mail-libraries-feasibility-analysis.md` | AC-1, AC-7, AC-14 wording (read in iteration 1; re-checked against the cited lines) |
| CNTR-MODERNIZATION-007 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md` | Interface, translation table, C-1, rules, policy (cited lines opened) |
| CNTR-MODERNIZATION-001 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md` | Carried type, producers, breaking change |
| REQ-MODERNIZATION-019, DES-MODERNIZATION-009 | `sdlc/artifacts/requirements/modernization/…`, `sdlc/artifacts/design/modernization/…` | `:118`; `:395` |
| Port and engine sources | `app/src/main/java/com/zegoggles/smssync/mail/transport/{MailTransport,K9MailTransport}.java`; `service/{BackupWorker,RestoreWorker}.kt`; `service/state/State.java`; `di/IoDispatcher.kt` | New citations; regex and grep tests |
| Vendored IMAP | `k9mail-vendored/src/main/java/com/fsck/k9/mail/store/imap/{ImapFolder,ImapConnection,ImapStore,XOAuth2AuthenticationFailedException}.java` | New citations |
| Prior plan-review format | `sdlc/artifacts/build/sprints/sprint-018/U-061/plan-review.md` | Format |

## Phase Completion Report
---
story_id: "U-062"
phase: "plan-gate/plan-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-019/U-062/plan-review.md"
story_status: "in-progress"
current_build_phase: "plan-gate"
errors: []
---
