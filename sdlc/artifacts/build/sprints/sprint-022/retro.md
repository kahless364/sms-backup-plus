---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-022
started: 2026-11-21
completed: 2026-11-21
---

# Retro: sprint-022 — an allow-list of TLS cipher suites for both socket factories (U-066)

## Business Summary

**Objective.** Close finding F-1 (Low) of the U-065 security review (`sdlc/artifacts/build/sprints/sprint-021/U-065/review-security.md`):
both TLS socket factories still enabled weak cipher suites (CBC, SHA-1, static RSA key exchange, DHE and others), and
when the cipher list could not be worked out in advance the platform's suites were left as they were. Only the
forward-secret suites with authenticated encryption stay enabled: the three TLS 1.3 suites, six ECDHE suites for TLS 1.2,
and the renegotiation signalling value. This runs before the migration's code move (CR-009). The target and the order
are the product owner's decisions of 2026-11-21 (Record 11 answer 2, Record 12 in the appendix "Product owner decision"
of `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`).

**Outcome.** U-066 is `done` and sprint-022 is `completed` (`sdlc/artifacts/build/sprints/sprint-022/sprint-plan.md`).

- QA: 13 of 13 acceptance criteria and 3 of 3 integration criteria met, five with deviations that were ruled before the
  build (`sdlc/artifacts/build/sprints/sprint-022/U-066/qa-results.md`).
- The authoritative test count went from 732 to 746 (14 added, three renamed, none removed), 0 failures, 2 skipped as
  before. QA's own gate run gave the same figures, and JaCoCo and the release build passed.
- The emulator check on Android 16 passed after one test SMS (`evidence/IC1-emulator.txt`): the app connects to Gmail
  and completes a backup of one message, with none of the four error kinds in the log.
- Security review PASS, no Critical, High or Medium finding, three Low findings deferred: W-1 (the
  certificate-enrollment socket in `PinCertificateEnrollmentFlow.java` is not hardened), W-2 (on Conscrypt an empty
  suite list still completes at TLS 1.3 with its three TLS 1.3 suites, so "empty list" does not mean "refused"), W-3
  (the shared arrays are still mutable, U-065 F-4). Finding F-1 of the U-065 review is closed for every IMAP, SMTP and
  POP3 socket (`U-066/review-security.md`).
- Code review PASS with no blocker and no warning (`U-066/review-code.md`).

**Key Deliverables.**

- Commit `1f8ebe3b` (U-066): `DefaultTrustedSocketFactory` enables only the ten names of `ALLOWED_CIPHERS`, taken from
  the socket's default-enabled suites by the new `ciphersToEnable(SSLSocket)` and in the allowed list's order; the
  failed-initialisation branch computes the suites from the socket; `ORDERED_KNOWN_CIPHERS` and `BLACKLISTED_CIPHERS` are
  gone. `PinnedCertificateSocketFactory` has one changed Javadoc line (a ruled exception; its compiled classes are
  identical before and after) and reaches the same step as before. 14 tests added and 12 changed, including loopback
  handshakes showing that a server offering only a pruned suite is refused (ECDHE-CBC-SHA1, and RSA key exchange with
  GCM), on both factories and both shapes.
- Mutation evidence: 15 counted mutation runs and 2 guard checks by the developer, 2 more by QA, all detected. One
  further run (the first of M-X5 (ii)) is logged as not counted (see Issues Encountered).
- Records and planning before the build (commit `b9fd172f`): CR-014 Amendment 1, the allow-list wording that the story's
  behaviour must make true. Close-out records: a dated note in CR-009 on what U-066 changed (commit `eda0aba7`).
- Work done in the same checkout while the story ran: DES-MODERNIZATION-014, REQ-MODERNIZATION-020, CR-015 (commit
  `3af4ce32`) and CR-016 (commit `f0d0d17e`), and stories BUG-023, U-067 and U-068 (not committed when this retro was written).

**Decisions Made.**

- Orchestrator, 2026-11-21, on the Requirements Analyst's recommendation, under Record 12: DP-1 option A (forward secrecy
  with authenticated encryption only), DP-2 allow-list, DP-3 compute the cipher list from the socket, DP-4 as written
  (the two ChaCha20 TLS 1.2 suites stay). All four are the recommended options, so no criterion changed (story,
  Implementation Notes, rulings table).
- Orchestrator, before planning: the cipher measurement on two emulators (see Process Observations) was the evidence for
  S0-3 and IC-3, and showed the cost of option A for the one server in use is nil.
- Orchestrator, on the plan review and the contract verification: a comment-only edit of the pinned factory's Javadoc is
  allowed (scope concern P-1), so AC-11 (a) reads "one file changes in code, one in a comment".
- Orchestrator, during the emulator check: one test SMS sent through the emulator console after a first run with nothing
  to back up, under the product owner's standing statement that adding SMS on the emulator is safe (as in sprint 021).
- Accepted cost (for the product owner): a TLS 1.2-only server with no ECDHE AEAD suite can no longer be used. None is
  known to be in use. A later cipher suite needs one entry in `ALLOWED_CIPHERS`, as a later protocol needs one in
  `ALLOWED_PROTOCOLS`.

## Process Observations

**Process Compliance.** The chain ran as the framework defines it for one story: create-sprint, then execute-story with the
Story Build Manager's plan, planning, the three plan gates, implementation, code review and security review in parallel,
a story-scoped commit, QA, and the closing bookend. All expected phase artifacts are present with passing verdicts. The plan
had one iteration plus Amendment A1 (taking up the non-blocking findings of the three gates before the developer started).

**What went well.**

- The cipher measurement was made before planning, on two emulators (Android 16, `Pixel_9_Pro`, and Android 11,
  `Pixel_4_2`), by the same read-only program BUG-019 used. Both report the same 18 default suites, all ten allowed names
  among them, and Gmail negotiates TLS 1.3 by default and `TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256` when limited to TLS 1.2
  (story, Implementation Notes, "Measurement for S0-3 and IC-3"). With that table the plan's decision rows (the S0-3
  stop rule, the expected lists, the device effect "eight suites removed, ten kept") had figures to compare against
  instead of assumptions. The limits stay as the story states them: emulator images, not the owner's devices; only
  Gmail; the TLS 1.2 handshake offered the platform's default suites, not only the six.
- Design work ran in parallel with the build in one checkout (DES-MODERNIZATION-014, REQ-MODERNIZATION-020, CR-015,
  CR-016). It worked because only this story's agents ran Gradle and commits were made by explicit path; the sprint plan
  records the arrangement and the file-overlap check (`sprint-plan.md`, "File-Overlap Advisories" and the shared-checkout
  risk). The story's commit `1f8ebe3b` holds no design or change-record file (QA: `git diff --name-only` over the three
  record folders is empty).
- The tests were checked against code that is wrong, not only against the right code: the mutation rows were derived
  against the planned test code (the sprint 021 recommendation), and the developer met one environment stop (below) and no
  unexpected row. QA ran two mutations of its own, one of a kind not in the developer's table (the platform's order
  returned instead of the allowed list's), and both were caught (`U-066/qa-results.md`, "QA mutations").
- Evidence chain: QA checked by blob hash that the committed production and test files equal the blobs the mutation log
  saved, and that the debug APK installed for the emulator check has the SHA-256 recorded in the IC-1 file
  (`qa-results.md`, AC-9 and IC-1).

**What did not go well, and what to change.**

- Review file names. The code and security reviews were written as `code-review.md` and `security-review.md` and committed
  under those names in `1f8ebe3b`. The closing step accepts `review-code.md` and `review-security.md`, so the
  orchestrator renamed both in the close-out (story, Review Findings, "File names"; `git status` shows the renames). The
  brief given to the review agents named the wrong files. Change: the review briefs carry the names the closing step checks
  (`review-code.md`, `review-security.md`), and the brief template is checked against the closing step's list.
- Records written by other agents appear in the developer's "records" check. The plan's `records-end` check
  (`git status --porcelain` over change records, contracts and requirements) was meant to show this story touched none of
  them. With the design work running in the same checkout it listed other agents' files (modified
  `CNTR-MODERNIZATION-004-backup-scheduler.md`, `REQ-MODERNIZATION-005-...md`, `requirements/modernization/_index.md`; new
  `CR-015`, `CR-016`, `REQ-MODERNIZATION-020`). The developer noted this and read it correctly
  (`implementation-log.md`, I9 and Deviations 6), but it was a manual judgement, not a check that passes or fails. Commits
  were held until the developer finished, so the production change stayed uncommitted during the mutation runs, as the
  protocol requires. Change: when work runs in parallel in one checkout, the check that this story touched no record
  compares the story's own file list (`git diff --name-only <start>..<fix commit>`, which QA used for AC-10) and not the
  working tree.
- Developer run length. The developer's baseline run started at 08:15:58Z (`implementation-log.md`, I0); the mutation
  entries run from 08:34:45Z to 09:05:33Z (`evidence/mutation-log.md`); the final gate and scan came after, and the
  orchestrator's emulator run is stamped 09:21Z (`evidence/IC1-emulator.txt`). The plan counted 22 Gradle runs. The
  mutation runs and gates are where the time went; the sources do not split the hour further. The first run of M-X5 (ii)
  failed at class level with a Robolectric `Couldn't create lock file ... .robolectric-download-lock` error before any
  test ran, so the mutation was restored, the run logged as a mismatch and not counted, and repeated once under decision
  row D33. The error was repeated and logged, as the plan required, and it cost one extra Gradle run. No cause was found
  or recorded. Change: none beyond the existing rule; if the lock-file error shows up again, record its cause (a second
  Gradle process holding the folder, or the earlier run still ending) instead of repeating until it passes.
- Two applied change records failed their first independent check. CR-015 (the scheduler contract v3) and CR-016 (the
  requirements wording) were each checked after the edits were applied, and each check returned FAIL on text the record
  had not covered: four texts for CR-015, and for CR-016 a must-fix (F-1, the test-count rule did not say where the end count
  is taken) and two should-fix findings. Each record then needed an Amendment 1 and passed after it
  (`sdlc/artifacts/build/change-records/evidence/CR-015-applied-text-check.md` and `CR-016-applied-text-check.md`, "Verdict
  after Amendment 1: PASS"). CR-014 had been amended the same way earlier. Checking the applied text paid off. The pattern
  behind the findings is that each record listed the sentences it knew it changed, not every sentence the change makes
  false. Change: when drafting a change record, search the contract, design and requirement text for each term and
  statement the change affects (as the applied-text checks did) and list every hit, before applying.
- The test-count rule was ambiguous about where the end count is taken. On this story the authoritative command with `HEAD`
  stays at the start value (732) until the commit, so the developer wrote "the authoritative HEAD count stays 732 until the
  commit" next to the figures (`implementation-log.md`, AC-12 row). CR-016 Amendment 1 (A-7) now says the end count is
  taken with the story's last commit in place of `HEAD`, or, before the commit, over the working tree with
  `git grep --untracked`. The rule was clarified by this story's own run, not by a plan review.

**Gate Results.**

| Story | plan-review | contract-verification | expert-depth-check | code-review | security-review | qa-validation |
|-------|-------------|-----------------------|--------------------|-------------|-----------------|---------------|
| U-066 | PASS (and PASS on the check of Amendment A1) | PASS | FAIL (warning only; principles not named, RFC sections missing), taken up in A1 | PASS | PASS | PASS |

**Issues Encountered.**

- The Robolectric lock-file error described above (M-X5 (ii), first run).
- The emulator run on a debuggable build produced the observation behind BUG-023. In the log from 03:20:05 to 03:21:25
  (device clock), the test SMS started the content trigger and its delayed incoming backup, and the same window has no line
  containing `onReceive(` and none containing `scheduleIncoming`, although verbose lines of the app's tag are present
  (`evidence/IC1-emulator.txt`, "side observation"). One emulator and one message, so the reading is that the SMS receiver
  is not delivered the broadcast and the content trigger is the only incoming path on this run. The story
  `sdlc/artifacts/stories/BUG-023-content-trigger-backup-skips-scheduling-checks.md` (status `ready`) records the defect that
  follows: the backup started by the content trigger is enqueued without the network, encryption-degraded and auto-backup
  rules that `scheduleIncoming()` applies. Its section "Observation, with its limits" states the receiver observation
  as not established, and a separate defect. IC-1's own criteria do not cover this; it was found because the log of the
  emulator run was read for more than the four error counts.
- The `compiled-pipeline.json` of the story workspace holds a machine path (security review I-8). It is a framework-written
  file, not authored for the story; it is not a secret.

**Not verified** (no criterion requires these): the suite negotiated by the app's own socket on a device, and that the
removed suites are off on a device (the security review asks for a log line of `getEnabledCipherSuites()` and the negotiated
suite, temporary and uncommitted, as "What IC-1 must observe"); Android versions between 11 and 16, and releases before
API 29; the pinned-certificate path on a device (no host is enrolled); a TLS 1.2 handshake to Gmail offering only the six
allowed names (the measurement offered the platform's defaults); refusal of pruned suites on Android's own Conscrypt (the
refusal tests run on the desktop Conscrypt test provider, and not at all under the JDK provider); the order precondition
of AC-1 (a) in the strong form (QA observation O-1).

**Recommendations.**

- Name the review files `review-code.md` and `review-security.md` in the code-review and security-review briefs.
- For parallel work in one checkout, base the "this story touched no record" check on the story's own file list, and
  keep the other conditions that made it work: only the story's agents run Gradle, commits by explicit path, no mutation
  present when a commit is made.
- When a change record is drafted, search for every sentence the change makes false and list each, so the applied-text check
  has nothing new to find.
- Record the cause of an environment error such as the Robolectric lock-file one when it first appears.
- Open items for the product owner, none blocking: whether to add a story for the on-device observation of the enabled and
  negotiated suites (security review, QA recommendation 1); whether W-1 (the enrollment probe socket) gets the factory's
  handling; CR-014 Amendment 1 is applied with the combined revision of CNTR-MODERNIZATION-001 at the migration, with W-3
  and the order precondition (O-1) taken up at the CR-009 move.

**What comes next.** Story U-067 (the fix for BUG-022), then U-068 (minSdk 24, story S1 of DES-MODERNIZATION-014), then
the fixes for BUG-023 and BUG-021 before the runtime-library story.
