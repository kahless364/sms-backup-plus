---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-020
started: 2026-11-17
completed: 2026-11-20
---

# Retro: sprint-020 — Characterisation tests for the two TLS socket factories (U-063)

## Business Summary

**Objective.** Record, as tests, how the app's two TLS socket factories behave today, so that the
planned move of about 458 lines of TLS code into app ownership (change record CR-009, part of the
Thunderbird migration) can be checked against a baseline. The product owner's instruction was
"yes. add tests". One story, U-063, in one wave. Tests only: no production, vendored, build or
dependency change.

**Outcome.** U-063 is `done` and sprint-020 is `completed`. Final QA: 37 of 37 criteria met (AC-1 to
AC-24, IC-1 to IC-13), 20 outright and 17 with a recorded deviation.

- 28 new unit tests in six new test files and one modified one: 682 tests before, 710 after, 0
  failures, 2 skipped (the two that were skipped before).
- What they pin: a wrong host name, an untrusted chain and an expired chain are rejected on both
  connection shapes, with accepted controls; the cipher blocklist and order and the protocol list;
  the SNI rules; the pinned factory accepting only the enrolled certificate, whatever the host name.
- Each group was shown to fail when its protection is removed: 39 logged temporary edits to
  production code, every one restored and verified. QA repeated two of them with the same result.
- The three production TLS files equal the start commit's blobs; no build file changed.
- Mail package line coverage rose from 71.5% to 72.4%.
- BUG-019 filed (medium): the default factory enables TLS 1.0 and 1.1 wherever the provider
  supports them. On the JDK's provider, which leaves them off, the factory turns them on.
- Seven change records for the migration (CR-005 to CR-011) and two product-owner decision records
  were raised and committed just before this sprint started; they are not part of it.

**Key Deliverables.** Commit `5a5321d4` (164 files, story scope only):
`app/src/test/java/com/zegoggles/smssync/mail/ssl/` — `DefaultTrustedSocketFactoryHardeningTest`,
`DefaultTrustedSocketFactoryHandshakeTest`, `DefaultTrustedSocketFactoryJdkProviderTest`,
`TlsTestFixtures`, `LoopbackTlsTestServer`, and two tests added to
`DefaultTrustedSocketFactorySniTest`; `app/src/test/java/com/zegoggles/smssync/mail/` —
`PinnedCertificateSocketFactoryCreateSocketTest`. Story workspace `U-063/`: plan (three iterations
and amendment A1), three plan-gate results, implementation log, mutation log, evidence, code review,
security review, QA results. `sdlc/artifacts/stories/BUG-019-factory-enables-tls10-and-tls11.md`.

**Decisions Made.**
- Product owner, 2026-11-17: the three checks that cannot be observed with the default test TLS
  provider go into one extra class on the JDK's provider ("Separate class, JDK provider
  (Recommended)").
- Product owner, 2026-11-20: the pinned factory's "sets no server name" test moves to that class
  too ("Move it to the JDK-provider class (Recommended)").
- Product owner, 2026-11-20: a commit limited to the story's files before QA ("Commit and push
  (Recommended)").
- Product owner, 2026-11-17: BUG-018 was offered for the sprint and not selected ("U-063 only
  (Recommended)").
- Orchestrator rulings, all recorded in the story: a diagnostic turn outside the plan after the
  environment probe stopped on four rows; the corrected loopback server set-up; compiling
  replacements for two mutations that did not compile; the developer's raw-log folder moved into
  the session scratch folder; the plan's twelve departures from the story's literal text accepted.
- The TLS 1.0 and 1.1 behaviour is recorded as a passing test and a bug, not fixed here. Keep or
  drop is the product owner's decision under BUG-019.

## Process Observations

**Process Compliance.** The chain ran: create-stories (from CR-009) → create-sprint → execute-story
(planning, three plan gates, fourteen Developer turns, code review, security review, one fix-in-cycle
remediation with diff-scoped re-checks, QA) → closing bookend. create-bug ran inside the story for
BUG-019. All required artifacts are present with passing verdicts, apart from the advisory
expert-depth-check.

**Deviations.**
- Run with `execute-story` directly, not `execute-sprint`, as in the last three sprints.
- Nothing was committed per increment; commits wait for the product owner's word. One story-scoped
  commit was made before QA; the sprint files, BUG-019 and the QA results were still uncommitted
  when this retro was written.
- The plan was amended after its gates had closed (amendment A1), because the environment probe
  contradicted its assumptions. The amendment was reviewed as a delta by the plan-review reviewer;
  contract-verification was not re-run, on that reviewer's finding that it was unaffected.
- A later product-owner decision (the AC-19 move) was applied by orchestrator ruling without a
  second plan amendment.
- One diagnostic Developer turn (I0c) was outside the plan.
- create-bug ran in the middle of the story by pausing the execute-story context by hand, as in
  sprint-018.
- The orchestrator made small edits itself: "As built" notes and Review Findings in the story, and
  a wording correction in BUG-019.
- The lifecycle engine set the story to `done` and the sprint to `completed` when a passing
  `qa-results.md` was written, before the closing bookend ran. Reported in sprint-019 as well.
- QA had no editing tool and applied its two spot-check mutations with a one-line `sed`.
- Developer turns whose final Gradle run exceeded three minutes were moved to the background by
  the tool; the agents waited without polling.
- The plugin was updated from 1.7.2 to 1.9.0 between sessions. The story kept using the 1.7.2
  tools it was compiled with; the hooks ran from the newer version. No fault was seen.
- U-063's `pipeline` field is empty, as on every story here.

**Gate Results.**
- plan.md: PASS at iteration 3.
- plan-review: FAIL, FAIL, PASS. Iteration 1: gaps in the probe decision table; an undetectable
  mutation could hide an unmet criterion; the hygiene scan skipped committed files; the
  test-ordering evidence could not work; count errors. Iteration 2: the new catch-all row would
  have stopped every successful probe.
- contract-verification: PASS. expert-depth-check: FAIL at warning severity (no named patterns,
  pitfalls or published sources).
- Amendment A1 review: PASS, six warnings applied as binding.
- review-code: PASS (0 blockers; 3 items fixed in cycle; 2 warnings deferred).
- review-security: PASS (0 blockers; 2 items fixed in cycle; 7 warnings deferred).
- qa-results: PASS, 37 of 37.

**Issues Encountered.**
- The plan's central assumption was wrong and only the probe showed it: under Robolectric the TLS
  provider is Conscrypt, where the client socket reports no server name whatever is set, and the
  supported and enabled protocols are the same list. Three criteria could not be observed there.
- Robolectric does not separate class loaders by Conscrypt mode, so a class on the JDK provider
  would have shared the factory's once-per-class-loader lists with the others. Found by the planner
  reading the framework's classes; solved with a second annotation and three guards, which leaves
  a permanent tripwire in the suite.
- The loopback test server failed on a JDK module-access restriction when not given the peer's
  address. It looked like a TLS failure until the diagnostic captured the server's exception.
- Two planned mutations did not compile (deleting the only statement that throws leaves an illegal
  `catch`), and one was undetectable under the default provider. None of the three had been caught
  by the plan gates.
- The endpoint security product flagged the Git shell again on 2026-11-17, about two hours after
  the session's last command; the cause is not known. The session before that had been stopped for
  a machine shutdown part-way through the mutation phase, at a clean point.
- The implementation log's times for the last increment were written by hand and are later than
  the files they describe. The story's Verification Steps and two Affected Code rows still carry
  figures from before the amendment.
- The release APK's hash changes with every commit because the build embeds the git revision, so
  "byte-identical" holds only when compared at one commit.
- A reviewer's shell command hung on a heredoc and was left running until the orchestrator stopped
  it.
- `compiled-pipeline.json` again contains a user-profile path and was kept out of git by a local
  exclude.
- Coverage figures for the `activity` and `preferences` packages change between runs with no code
  change (noted since sprint-018).
- The story took about 76 Gradle runs and three planning iterations for 28 tests.

**Not verified** (no criterion requires these): anything on a device; any handshake below TLS 1.3;
the real platform trust store; what Gmail's IMAP endpoint accepts; 37 of the 39 mutations were not
repeated by QA.

**Recommendations.**
1. Before the CR-009 move: add a test that the pinned factory rejects an expired enrolled
   certificate and a test for the SNI `catch` on its own, each with its mutation; file a bug for the
   pinned factory's missing cipher and protocol hardening; take the BUG-019 decision after measuring
   what a device enables and what Gmail's endpoint accepts; route the contract wording SC-1 to a
   change-record addendum.
2. Probe the environment before planning, not as the first increment, when a story's criteria
   depend on what a test provider does. A one-turn spike would have saved two planning iterations
   and an amendment.
3. For mutation plans: check that each edit compiles when the plan is written, and for each
   "undetectable by design" entry say which layer is then unprotected.
4. When a decision moves a test or changes a count, update every count-bearing line of the story in
   the same turn.
5. Take log timestamps from the clock. Give Gradle calls an explicit timeout above three minutes.
6. Compare APKs at one commit, or ignore the embedded revision entry.
7. Give QA an editing tool for spot-check mutations, or script the substitution with a hash check.
8. Consider a separate Gradle test task for the JDK-provider class, which would make its isolation
   structural. That needs a build change and a decision.
9. Tidy BUG-018 into a repair story before it is scheduled; it has now been passed over twice.
10. Report to the AMP maintainers, in addition to earlier items: no defined way to amend a plan
    after its gates close; `compiled-pipeline.json` embedding an absolute plugin path.
