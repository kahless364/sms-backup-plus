---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-021
started: 2026-11-20
completed: 2026-11-20
---

# Retro: sprint-021 — TLS baseline gap tests (U-064) and the TLS 1.2 floor fix (U-065)

## Business Summary

**Objective.** Close the two gaps the previous sprint left in the TLS baseline tests (U-064), then fix
BUG-019 and BUG-020 (U-065): neither of the app's two TLS socket factories enables a protocol older than
TLS 1.2, and the pinned-certificate factory treats cipher suites and the server name the way the default
factory does. Both before the migration's code move (CR-009). The order and the target behaviour are the
product owner's decisions (Records 8 and 9 in the appendix "Product owner decision" of
`sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`).

**Outcome.** Both stories are `done` and sprint-021 is `completed`.

- U-064 (tests only): QA 15 of 15 criteria met. The authoritative test count went from 710 to 713. No
  production change.
- U-065 (production change): QA 13 of 13 criteria met, two with a judged deviation. The count went from
  713 to 732, 0 failures, 2 skipped (the baseline pair). The emulator check on Android 16 passed: after
  the change the app still connects to Gmail and completes a backup.
- BUG-019: all five criteria satisfied. BUG-020: satisfied except its AC-5 (the contract statement), which
  waits for the combined contract revision.

**Key Deliverables.**

- Commit `b2c2dfd8` (U-064): `PinnedCertificateSocketFactoryExpiryTest` (a real handshake with an expired
  enrolled certificate is rejected by the validity check) and two methods added to
  `DefaultTrustedSocketFactorySniTest` (a host name the platform's `SNIHostName` rejects causes no
  failure). Two logged mutations, both detected.
- Commit `12b584ab` (U-065): `DefaultTrustedSocketFactory` only ever enables TLS 1.2 and 1.3, where the
  platform supports them, also when its lists could not be computed at class initialisation;
  `PinnedCertificateSocketFactory` calls the default factory's two steps (`hardenSocket`, then
  `setSniHost`) on both socket shapes. 19 tests added and 7 changed, including handshake tests against a
  loopback server limited to TLS 1.2, 1.1 and 1.0. 14 logged mutations, all detected.
- Records and planning (commit `bd1d5398`): Record 9, story U-065, the sprint plan. Change record CR-013
  (commit `de03e99b`) was raised just before the sprint.
- Close-out commits: `fd239e96` (U-064) and the commit that carries this retro (U-065).

**Decisions Made.**

- Product owner, 2026-11-20, "Same as default" (Record 9): the pinned factory gets the default factory's
  cipher-suite handling and server-name rule. With Record 8 this settles BUG-020 AC-1 and picks
  alternative (b) of CR-013's pinned clause.
- Product owner, 2026-11-20: permission for the story-scoped commit and push before each QA run
  ("Commit and push (Recommended)", twice), and to start the `Pixel_9_Pro` emulator for the check
  ("Start it for me").
- Design, taken in U-065's plan and checked by three plan gates: the floor is an allow-list
  (`ALLOWED_PROTOCOLS = TLSv1.2, TLSv1.3`) applied to what the platform supports; the pinned factory
  calls into the default factory instead of holding a copy, at the cost of one method made public in the
  vendored class; the old fail-open branch now applies the floor per socket.
- Orchestrator, during the emulator check: after a first run with nothing to back up, one test SMS was
  sent to the emulator under the product owner's standing statement that adding SMS messages there is
  safe. QA recorded this as a deviation from the story's wording and judged it acceptable.

## Process Observations

**Process Compliance.** The chain ran as the framework defines it: create-cr (CR-013) → create-stories
(U-065, from BUG-019 with BUG-020) → create-sprint → execute-story twice (U-064, then U-065), each with
the Story Build Manager's plan, planning, the three plan gates, implementation, code review and security
review in parallel, a story-scoped commit, QA, and the closing bookend. All expected phase artifacts are
present with passing verdicts.

**Deviations.**

- Both plans needed a second iteration. U-064: the evidence-hygiene scan wrote into the folder it
  scanned. U-065: one mutation row expected a test to fail that could not fail. Both were found by the
  plan review before a developer ran anything.
- Gate agents were resumed for iteration 2 instead of being started fresh. Each was still a separate
  instance from the planner; each re-read the whole revised plan.
- U-064 code review W1 (a user-profile path in the untracked `compiled-pipeline.json`) was resolved in
  cycle without a Developer dispatch, because the fix was the local exclude entry, not an edit.
- U-065's mutation runs restored from saved copies verified by hash instead of `git checkout`, because
  the production change was uncommitted while they ran. The plan declared it; the plan review, and QA
  with two literal-protocol spot checks after the commit, accepted it.
- The emulator was started by the orchestrator, where the plan said to ask the owner to start it. The
  owner was asked and chose "Start it for me".
- The plugin now has a newer version installed beside the one this engagement uses; the skills' text came
  from the newer version and the engine tools from the older one. No fault was seen. In the newer text
  QA runs in the same parallel group as the reviews; this sprint kept the older order (reviews, commit,
  then QA), which the stories' commit-based criteria need.

**Gate Results.**

| Story | plan-review | contract-verification | expert-depth-check | code-review | security-review | qa-validation |
|-------|-------------|-----------------------|--------------------|-------------|-----------------|---------------|
| U-064 | FAIL, then PASS | PASS, PASS | FAIL (warning only), then PASS | PASS | PASS | PASS |
| U-065 | FAIL, then PASS | PASS, PASS | PASS, PASS | PASS | PASS | PASS |

**Issues Encountered.**

- JUnit XML is switched off in the build. A plain Gradle run leaves the XML of an earlier run on disk, so
  totals read from it belong to someone else's run. U-064's QA ran the gates three times before settling
  on the read-only init script from U-063's evidence tools; U-065's plan and QA used it from the start.
  After U-065's QA the XML on disk is from a mutation run.
- The planner for U-064 ran `./gradlew --version` once against its instructions. No task ran and nothing
  was written; it was declared.
- No endpoint-security block occurred in this sprint.

**Not verified** (no criterion requires these): the app's own sockets on a device after the change; any
Android 11-era device (the measurement in BUG-019 shows that is where the change removes TLS 1.0 and
1.1); the pinned-certificate path on a device (no host is enrolled); a handshake from a socket with an
empty protocol list; a downgrade forced by a third party.

**Recommendations.**

- Put the JUnit XML rule into every QA and review brief until the build itself writes XML: totals only
  from a run made with the init script, with the results folder deleted first.
- When a plan contains a mutation table, have the planner derive each row against the planned test code
  and state the first failing assertion and the failure type. U-065's iteration 2 did this and the
  developer then met no stop row.
- Keep the evidence-hygiene scan as the last step, writing outside the scanned folders, as both plans now
  do.
- For an emulator check that needs something to back up, decide in the story who provides it and how, so
  the run does not depend on a standing statement.
- Open items for the product owner, none blocking: a change record for stating the protocol floor and the
  cipher handling in CNTR-MODERNIZATION-001, or a dated note saying why not (BUG-020 AC-5); whether
  BUG-020 closes with AC-5 carried by that record; whether a story should prune the cipher lists (U-065
  security finding F-1, Low); the note CR-013 needs when alternative (b) is applied; the lines CR-009
  needs now that the pinned factory calls the default factory's steps.
