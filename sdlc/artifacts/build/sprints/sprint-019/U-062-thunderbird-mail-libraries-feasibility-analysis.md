---
type: story
status: in-progress
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirements:
  - REQ-MODERNIZATION-019
design_docs:
  - DES-MODERNIZATION-009
integration_contracts: []
dependencies: []
sprint: '019'
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-062
title: Feasibility analysis for replacing the vendored K-9 mail library with Thunderbird for Android mail libraries (REQ-MODERNIZATION-019 Part A)
pipeline: ''
domain: modernization
alignment_audit: passed
updated_at: '2026-11-13T15:17:36.748Z'
current_build_phase: review
---

# U-062: Feasibility Analysis for Replacing the Vendored K-9 Mail Library with Thunderbird for Android Mail Libraries

## Story
As the product owner and sole user of this private, unpublished SMS Backup+ build,
I want a written, cited and tested feasibility analysis of replacing the in-tree `:k9mail-vendored` module with Thunderbird for Android's mail libraries, ending in one explicit recommendation (go, go-with-conditions or no-go),
So that I can decide, on evidence and in writing, whether the migration (REQ-MODERNIZATION-019 Part B) is worth doing before any design or build work for it exists, without the app, its build or its dependencies changing in the meantime.

Product owner statements this story serves (verbatim): "include the thunderbird migration in scope"; "I don't own the old app store listing. this is just going to be a private app for me" (no Play publishing, no other users; dropping old Android versions only has to suit the owner's own devices); "I'm good raising the minsdk version" (2026-11-11, about the JDK 25 story; REQ-MODERNIZATION-015 plans minSdk 23). Long-term goals: current Android, "on the latest of everyhing it uses", later RCS support and current Google auth.

## Scope and Delivery Shape

**Part A only.** This story implements REQ-MODERNIZATION-019 Part A (questions 1-12, AC-A1 to AC-A9). It produces one analysis document and a recommendation. It changes no product code, no build file and adds no dependency to the app. Part B (migration) acceptance criteria are deliberately absent. No Part B design or story may exist before the product owner accepts the analysis in writing (AC-15).

"Implementation" here is research and writing. One story fits one sprint; Part A is not split (see Estimation Guidance).

| Inc | Work |
|-----|------|
| I0 | Record the start commit and the `git status --porcelain` output and the `git ls-files --others --exclude-standard` list (IC-1, IC-4). Copy the research input into the workspace (IC-5). Capture the repo baseline facts the analysis is measured against (Technical Context). No product edit. |
| I1 | Pin the upstream commit. Answer questions 1, 2, 3 (provenance, consumability, modules, licences). |
| I2 | Answer questions 4-8 (platform, API mapping, authentication, TLS/pinning/SNI, size), running the experiments the rules below allow. |
| I3 | Answer questions 9-12 (fallbacks, separability, sizing and split, recommendation). If no-go, prepare the AC-16 records. |
| I4 | Run the end gates and hygiene checks (IC-1 to IC-4). |
| I5 | Present the analysis to the product owner; record the decision (AC-15). The story closes only after that record says "accepted". |

### Where the analysis lives (decision)

Exact path of the analysis: `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`.

This is a story-keyed directory under `sdlc/artifacts/build/`, not the assessment pipeline's `sdlc/analysis/` tree and not a sprint folder (the sprint number is not yet assigned). Once the product owner accepts it, EPIC-MODERNIZATION-006 and REQ-MODERNIZATION-019 reference it by this path (a reference edit made by the Artifact Librarian / Requirements Analyst at acceptance, not by the researcher). Research input copy: `sdlc/artifacts/build/feasibility/U-062/inputs/thunderbird-mail-libraries-research-2026-11-12.md`.

### How a citation is recorded (decision)

Every factual statement about a Thunderbird, K-9 or third-party library carries a citation ID (`C-001`, ...) resolving to a row in a "Citations" table in the Citations appendix (after Section 12) with these columns: ID, claim in one line, URL (a permalink: `.../blob/<full commit SHA>/<path>#L<n>` or `.../tree/<tag>/...`, never `main` or a branch name), commit SHA or tag or artifact version, retrieval date (YYYY-MM-DD), and the verifier's status (opened / read / grep-confirmed). A claim that a reviewer cannot open from its row, or whose row lacks any column, fails AC-2. The analysis pins one upstream commit of `thunderbird/thunderbird-android` for all source citations and states it once at the top; if the pin differs from the research input's commit (`ac1f0e85...`), every carried claim is re-verified at the new pin.

### Experiment rules (decision)

Any experiment (cloning `thunderbird/thunderbird-android`, building a module, a throwaway spike compiling this app's mail code against upstream) obeys all of the following.

1. It happens outside this repository's working tree: in a scratch directory outside the repo root, or in a throwaway copy of the repo made outside it. It is never committed to this branch and never staged.
2. It never edits `settings.gradle`, `app/build.gradle`, `build.gradle`, `gradle/**` or `k9mail-vendored/**` in the real working tree, never uses `includeBuild` or a submodule there, and never runs `--write-verification-metadata` there.
3. Downloaded source is untrusted. It is read and built only in its own directory. Nothing from it is executed except its own Gradle build, run with no credentials or signing keys in the environment and with a separate `GRADLE_USER_HOME` so the real Gradle cache and verification state are untouched. The analysis states which Gradle and JDK were used.
4. Its result is recorded in the analysis with the commands used (paths shown as `<scratch>` and `<repo>`, never as real machine paths), the upstream commit, exit code and the relevant output lines.
5. Stop rule: an experiment that cannot be completed after three distinct, recorded approaches is reported as "inconclusive" with those three attempts. That satisfies AC-1's "unknown" exception; it does not permit silently dropping the question.
6. Any proof-of-concept code that is quoted in the analysis is quoted inline (short excerpt) and is reviewed by the Code Reviewer from the analysis text and the recorded commands; it is not committed as a source file.

## Acceptance Criteria

AC-1 to AC-9 carry REQ-MODERNIZATION-019 AC-A1 to AC-A7 and the twelve questions; AC-15 and AC-16 carry AC-A8 and AC-A9. Each says what is counted, opened or compared.

- [ ] AC-1: (A1) The analysis exists at the path above and contains exactly twelve numbered sections, "1" to "12", titled with REQ-019's question names (Consumability; Modules and dependency graph; Licence; Platform requirements; API mapping; Authentication; Custom trust, pinning and SNI; Size and method count; Fallback options; Separability; Sizing and split; Recommendation). A section whose answer is "unknown" is a failure unless it states the experiment attempted (per rule 5) and why it was inconclusive.
- [ ] AC-2: (A2) Every factual statement about Thunderbird or third-party libraries (publication, module names, licence, minSdk/Kotlin/coroutine/AGP requirements, authentication mechanisms, API) carries a citation ID that resolves to a complete Citations-table row in the format above. The research input file is never a citation target. Every claim carried from it appears in the Citations table with its own fresh primary-source row. Counting rule for the reviewer: the number of distinct citation IDs used in the text equals the number of table rows, and no row has an empty column.
- [ ] AC-3: Section 1 (Consumability and provenance) states, for each of Maven Central, Google Maven, Mozilla Maven, JitPack and GitHub Packages, the URL probed, the retrieval date and the result (found with coordinates and version, or HTTP status), and gives a yes/no answer to "is any mail module published as a Maven artifact", with signing and stable-versus-snapshot status for anything found. It gives a verdict on the recollection "Thunderbird bought the old K-9 app and its libraries" (correct / incorrect / partly correct), quoting at least one primary-source sentence, and names the continuation repository. It states how far the app's vendored commit (`eaf689025ec28d55e3b62f5ea3b1f185f410ee6b`, recorded in `k9mail-vendored/NOTICE`) is ahead of and behind that repository, with the compare URL.
- [ ] AC-4: Section 2 (Modules and dependency graph) has one row for each of the 14 files REQ-019 tables (8 message/MIME-model, 6 transport) naming the replacement module and type. It lists, per proposed module, the direct and transitive dependencies with versions, and states for each of commons-io, apache-mime4j, jzlib and jutf7 whether it is replaced or retained. The analysis recounts the 14 files from `grep -rln "com\.fsck\.k9" app/src/main/java` outside `mail/transport/` and states any difference from 14.
- [ ] AC-5: (A3) Section 3 (Licence) lists, for every module and transitive dependency proposed, an SPDX identifier and a yes/no Apache-2.0 compatibility verdict, with a citation to the licence file or POM. An identifier read only from a POM that says just "BSD" and not opened further is marked as such. It lists the NOTICE/attribution obligations that would replace `k9mail-vendored/NOTICE`. Any non-"yes" row blocks a "go" recommendation (AC-14). MPL-2.0 items are listed with their file-level obligations, not left as a bare identifier.
- [ ] AC-6: (A4) Section 4 (Platform requirements) has a table with rows minSdk, compileSdk, Kotlin, kotlinx-coroutines, AGP, JVM target, JDK to build and Gradle. Columns are: upstream value at the pin (cited); the target set by the requirement that owns that row (cited by requirement file and line): REQ-MODERNIZATION-015 for minSdk, Kotlin, AGP, Gradle, JDK and JVM target; REQ-MODERNIZATION-016 for kotlinx-coroutines; REQ-MODERNIZATION-017 for compileSdk (REQ-015 places compileSdk and targetSdk out of scope); the app's current value (cited by file and line); a verdict "conflict" or "no conflict". A cell for which the owning requirement sets no value says "not specified in REQ-MODERNIZATION-0xx" and cannot receive "no conflict". Any conflict names the resolution options and records the choice as a product owner decision needed. The analysis does not change the approved minSdk 23 or any other target.
- [ ] AC-7: (A5) Section 5 (API mapping) has a table with one row for each of: connect/login; folder create/select; APPEND with flags and internal date; UID SEARCH by header; FETCH BODY.PEEK of headers and of the full body (stating separately whether each of the `X-smssync-*` headers defined in `app/src/main/java/com/zegoggles/smssync/mail/Headers.java` can be written and read; the analysis recounts them, and the expected count is 13 `X-smssync-*` constants, with `References` and `Message-ID` listed separately, 15 constants in all); X-GM-EXT-1/label handling (confirming or refuting "folder naming only", with the grep commands and scope); MIME build, whose cell addresses each of multipart construction, text bodies, base64 attachments, and address and header encoding separately (a single-line answer fails); MIME parse. No cell is blank. Each row gives the replacement call, the behavioural difference from the vendored behaviour, and "Java-callable without coroutines: yes / no / via shim", where "yes" and "via shim" are backed by a code citation or a compile proof under the experiment rules, and "no" by the signature.
- [ ] AC-8: Section 6 (Authentication) lists PLAIN, LOGIN, XOAUTH2 and OAUTHBEARER (and any other mechanism found) as implemented yes/no with a code citation each, states how the mechanism is selected, and states which module supplies the OAuth token provider (interface versus implementation) and what the app would have to supply. It then states the consequence for the later Google OAuth goal in two sentences at most.
- [ ] AC-9: (A6) Section 7 (Custom trust, pinning and SNI) has a table with one row for each of four protections: TLS hostname verification; certificate pinning (`PinnedCertificateSocketFactory`); SNI with the IP-literal and blank-hostname guard; independence from `org.apache.http.legacy`. Each row names the extension point and gives a code reference or a recorded proof of concept, and a status "supported", "needs fork of <named files>" or "blocker". The SNI row covers the guard cases exercised by `DefaultTrustedSocketFactorySniTest` (10 `@Test` methods): DNS name, IPv4 literal, abbreviated IPv6 literal, full IPv6 literal, empty string and blank host, plus the reflective-fallback cases, with the result for each. The two remaining tests (`setsSSLParametersWithCorrectSNIHostName`, `setsCorrectHostname_forDifferentHost`) are further DNS-name cases covered by the DNS-name row (test run, or code reading marked as such). The pinning row states whether the library's own certificate store has "reject everything else" semantics or "user-accepted exception" semantics. Any "blocker" row blocks "go" (AC-14).
- [ ] AC-10: Section 8 (Size and method count) gives a baseline measured at the recorded start commit from `:app:assembleRelease` (release APK byte size and dex method count, tool and command stated; note the sprint-018 finding that `apkanalyzer` can fail on this machine and that `dexdump` was used instead) and a delta. The delta is either measured on an `assembleRelease` of a throwaway copy, or estimated with the method stated (for example, summing proposed artifacts' sizes) and labelled "estimate". Report the new `gradle/verification-metadata.xml` entries that would be needed, counted by artifact.
- [ ] AC-11: Section 9 (Fallback options) covers at least: vendor a newer upstream snapshot; depend on a fork; stay on `:k9mail-vendored` and record the decision. Each has: cost (files and work items counted, not adjectives), supply-chain risk (with explicit reference to the U-027 eviction precedent recorded in `k9mail-vendored/NOTICE`), and who owns security patches. If Section 1 finds a published artifact, the section states "not required" and why.
- [ ] AC-12: (A7, part) Section 10 (Separability) states "separable" or "inseparable" separately for RCS support and for Google OAuth sign-in, each with cited evidence. If either is inseparable, the dependency is recorded for the product owner's decision (EPIC-MODERNIZATION-006 Out of Scope); the analysis does not decide it.
- [ ] AC-13: Section 11 (Sizing and split) states whether Part B fits one sprint or must become its own epic, with the reasoning counted (files, test classes, tooling, live-account run, Kotlin/coroutine adoption). It proposes a story breakdown as a table (working title, one-line scope, dependency on another row or on REQ-MODERNIZATION-015/016) with an acyclic order. These are prose rows only: no story, design or requirement file is created for them (IC-4).
- [ ] AC-14: (A7) Document order is fixed: Section 12 is the last numbered section, and is followed only by the appendices Citations, Experiment log and Product owner decision, in that order. When AC-16 is not applicable, the "AC-16 not applicable" statement comes before the Recommendation line. Section 12 ends with exactly one of the strings `go`, `go-with-conditions` or `no-go` on a line of its own labelled "Recommendation:". For go-with-conditions, each condition is numbered and has a stated test (command or observable result). The recommendation is consistent with the rules: any Section 5 row not achievable without forking, any Section 7 blocker, or any Section 3 non-"yes" row means the analysis does not recommend plain `go` and explains why if it recommends `go-with-conditions`. The recommendation is not presupposed by the story: `no-go` and `go-with-conditions` are equally valid outcomes.
- [ ] AC-15: (A8) The analysis is presented to the product owner after IC-1 to IC-5 pass. The product owner's decision (accepted, rejected, or changes requested, with the verbatim words) is recorded in the last appendix, "Product owner decision" of the analysis by the orchestrator or Artifact Librarian, with date and the name of the recorder. An agent cannot accept it. The story is not `done` without a record saying "accepted". Rejected or changes requested keeps the story open for rework. No Part B artifact exists before the acceptance date: verifiable with `git log --diff-filter=A --format=%ad --name-only` showing no added `sdlc/` file for Part B dated before it (IC-4).
- [ ] AC-16: (A9, conditional) If the recommendation is `no-go`: the analysis contains a decision record covering supply-chain mitigation for `:k9mail-vendored` (at minimum: where the authoritative copy of the vendored source lives, what detects a lost upstream, and what recreates it); a change record marks REQ-MODERNIZATION-019 Part B not applicable; and a change record revises EPIC-MODERNIZATION-006 success criteria 5 and 9 (cited by number and current text). These records are authored by the Requirements Analyst / Artifact Librarian after acceptance, not by the researcher, and are listed in the `change_records` of REQ-MODERNIZATION-019. If the recommendation is not `no-go`, the analysis says "AC-16 not applicable" in Section 12, before the Recommendation line (AC-14).

### Integration Criteria

No component is created. These criteria keep the analysis from changing the product.

- [ ] IC-1: No product or build file changed. At I0 the researcher records the start commit, the full `git status --porcelain` output and the `git ls-files --others --exclude-standard` list (the tree may already hold uncommitted sprint-018 / CR-004 paperwork; a clean tree is not a precondition, and the criterion holds if sprint-018 closing files are committed before I0 and the tree is clean). At the end, with `<start>` that commit, `git diff --stat <start> -- app k9mail-vendored metadata gradle settings.gradle build.gradle gradle.properties gradlew gradlew.bat` is empty, and `git status --porcelain` differs from the I0 output only by the files this story is permitted to create or edit (Affected Code), plus workflow bookkeeping the pipeline itself writes under `sdlc/`.
- [ ] IC-2: The gates still pass at the end, run on JDK 25 in a shell without `MSYS_NO_PATHCONV=1`: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease` exits 0. The unit-test run count equals the `git grep` count of `@Test` at the start commit (682 at HEAD 7dfd651b) and the skipped count is 2. No test, floor or verification setting changed (IC-1 covers the files).
- [ ] IC-3: Evidence hygiene. In every file this story adds, none of the following matches: an e-mail address (`grep -nE '[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}'`; third-party commit-author addresses are not copied either); the machine hostname (`$(hostname)`); the Windows user name (`$USERNAME`); a user-profile path (`C:\\Users`, `/c/Users`, `AppData`, `%USERPROFILE%`). The last two values are read from the shell at check time and must not themselves be written into any file. The check command and its zero-match result are recorded in the analysis.
- [ ] IC-4: Experiments stay out of the tree and Part B stays unwritten. `git ls-files --others --exclude-standard` (compared with the list recorded at I0, which is subtracted) and `git diff --name-status <start>` together show only, beyond what was already present at I0: the analysis, the research input copy, this story file, and (after acceptance, AC-16 only) change records. The criterion holds whether the I0 tree was dirty or clean. No cloned upstream source, build output of an experiment, `.gitmodules`, or any new `REQ-*`, `DES-*`, `CNTR-*` or `U-*` file other than U-062 exists. The analysis's experiment log gives, per experiment: scratch location placeholder, upstream commit, Gradle and JDK versions, commands, exit code, result.
- [ ] IC-5: The research input is copied into the workspace before it is relied on. `sdlc/artifacts/build/feasibility/U-062/inputs/thunderbird-mail-libraries-research-2026-11-12.md` exists, is the orchestrator's unreviewed notes (source file in the orchestrator's session scratchpad, named `thunderbird-mail-libs-research.md`, 291 lines, pinned to upstream commit `ac1f0e85f63c1b600ac443d1fb2d939bffff7de5`), carries a first-line banner "UNREVIEWED INPUT - not a citation source", and is otherwise byte-identical to the source except for redactions required by IC-3, each listed in the analysis. The SHA-256 of the source and of the copy are recorded in the analysis.
- [ ] IC-6: The analysis records, in a section before Section 1, the repo baseline it was measured against (start commit, Gradle, AGP, Kotlin, JDK, bytecode targets, minSdk, compileSdk/targetSdk, test count) with file and line citations, and states which test-count baseline applies (682, not REQ-019's 697: see Technical Context).

## Affected Code

No product file changes. The story may create exactly these files and no others (apart from the AC-16 change records after acceptance):

| File | Current Behavior | Change |
|------|-----------------|--------|
| `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` | Does not exist | Create: the analysis (Sections 1-12, Citations, experiment log, product owner decision) |
| `sdlc/artifacts/build/feasibility/U-062/inputs/thunderbird-mail-libraries-research-2026-11-12.md` | Does not exist | Create: copy of the unreviewed research input (IC-5) |
| `sdlc/artifacts/stories/U-062-thunderbird-mail-libraries-feasibility-analysis.md` | This story | Implementation Notes and Review Findings only |
| `sdlc/artifacts/build/change-records/CR-005-*.md` (and a second record if needed) | Does not exist | AC-16 only, `no-go` only, by Requirements Analyst / Librarian after acceptance |

Read-only context (never edited): `settings.gradle`, `app/build.gradle`, `k9mail-vendored/**`, `app/src/main/java/com/zegoggles/smssync/mail/**`, `gradle/verification-metadata.xml`, REQ-MODERNIZATION-015/016/017/019, EPIC-MODERNIZATION-006, DES-MODERNIZATION-009, CNTR-MODERNIZATION-007.

## Existing Behavior to Preserve

- Everything. Backup, restore, scheduling, TLS hardening (REQ-MODERNIZATION-002), pinning, SNI guard and the `MailTransport` port (CNTR-MODERNIZATION-007, context only) are unchanged.
- `:k9mail-vendored` stays in `settings.gradle` and in the build; dependency verification stays enforced; no JaCoCo floor changes.
- The test baseline stays at 682 run, 2 skipped (IC-2).

## Verification Steps

Run Gradle in a shell without `MSYS_NO_PATHCONV=1`.

1. AC-1: list the headings of the analysis; confirm twelve numbered sections in order; for each "unknown", find the stated experiment.
2. AC-2: count distinct `C-nnn` IDs in the text and rows in the Citations table; confirm equal and no empty column. QA opens a sample (below). Confirm no row cites the research input.
3. AC-3 to AC-13: for each, read the named table or statement and check it against the criterion text (counts, columns, no blank cell, every verdict present).
4. AC-5, AC-9, AC-14: for every row that is not "yes" / "supported", confirm the recommendation respects the blocking rule.
5. AC-6: open the cited lines of REQ-MODERNIZATION-015 (minSdk, Kotlin, AGP, Gradle, JDK, JVM target), -016 (coroutines) and -017 (compileSdk) and the app's build files and confirm each value matches.
6. AC-14: `grep -nE '^Recommendation: (go|go-with-conditions|no-go)$'` returns exactly one line, and no numbered section follows Section 12 (only the Citations, Experiment log and Product owner decision appendices, in that order).
7. AC-15: read the "Product owner decision" section: date, verbatim words, recorder. Check no Part B file predates it.
8. AC-16: if `no-go`, read the records and confirm the EPIC criteria 5 and 9 text is quoted and revised; otherwise confirm the "not applicable" statement.
9. IC-1 to IC-6: run the commands exactly as written and compare with the recorded results.

### What each downstream role checks for an analysis deliverable

| Role | Check |
|------|-------|
| Solution Architect (planning) | The plan covers all twelve sections and the experiment rules, names who produces AC-15/16 records, and does not plan any product edit. |
| Developer-role agent (research and writing) | Produces the files in Affected Code and the recorded commands. Does not commit experiments. |
| Code Reviewer | Reviews any proof-of-concept excerpt and its commands for correctness and for compliance with the experiment rules; confirms no source file was added (IC-1, IC-4). |
| Security Reviewer | Section 7 in full (TLS, pinning, SNI, blank host, hostname verifier choice); the supply-chain reasoning in Sections 1, 3 and 9 (provenance, eviction precedent, licence, patch ownership); IC-3 hygiene; that untrusted source was handled per rule 3. |
| QA Analyst | Opens every citation in Sections 1, 3, 4, 6 and 7 and a random 10 per other section (all, if fewer than 10), recording URL, result and who opened it; checks each per-question criterion; runs IC-1 to IC-6; confirms the story is not marked `done` until AC-15 records acceptance. A citation that cannot be opened fails AC-2. |

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Research and writing (web access required, REQ-019 Constraints) | I0 to I4: analysis, experiments, citations, end gates | Developer |
| Artifacts | AC-15 decision record (orchestrator or Artifact Librarian, from the product owner's written words); AC-16 change records if `no-go` | Orchestrator / Artifact Librarian / Requirements Analyst |

## Technical Context

Verified repo facts the analysis starts from (HEAD 7dfd651b at authoring; re-verify at the start commit):

- `settings.gradle:1`: `include ':app', ':metadata', ':k9mail-vendored'`.
- `gradle/gradle-daemon-jvm.properties:2`: `toolchainVersion=25`. `build.gradle:10`: AGP 9.0.1; `build.gradle:14`: Kotlin Gradle plugin 2.3.21.
- `app/build.gradle:21,24,25`: compileSdk 35, minSdk 21, targetSdk 35; `app/build.gradle:90-91`: Java 17 source/target; `app/build.gradle:118-119`: kotlinx-coroutines 1.7.3; `app/build.gradle:53`: release minification on.
- `k9mail-vendored/build.gradle:52,54,76-77`: compileSdk 35, minSdk 21, Java 8; lines 100-102: mime4j 0.8.11.
- `com.fsck.k9` is referenced in 14 files under `app/src/main/java` outside `mail/transport/` (counted at authoring); REQ-019's table classifies 8 as message/MIME model and 6 as transport.
- Test baseline: `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` returns 682 at HEAD 7dfd651b. REQ-019's "697" is the sprint-017 baseline; sprint-018 (U-061) deleted 15 donation tests, so 682 (2 skipped) applies to this story. If another story lands first, re-baseline from the start commit and record it.
- `:app:assembleRelease` is part of the gate set for anything touching dependencies (sprint-018 retro). This story touches none, but runs it as a precaution and to obtain the size baseline.

Preliminary findings to verify (from unreviewed research, pinned to upstream commit beginning `ac1f0e85`; each is a hypothesis, not a fact, and must be re-verified and cited per AC-2):

| # | Preliminary finding to verify | Tests in |
|---|-------------------------------|----------|
| P1 | K-9 Mail joined Thunderbird (MZLA) in 2022; it was not "bought". The continuation repository is `thunderbird/thunderbird-android`. | AC-3 |
| P2 | No mail-library Maven artifact is published; the libraries are source-only modules in that repository. | AC-3 |
| P3 | `:mail:common` and `:mail:protocols:imap` depend on Kotlin Multiplatform modules needing minSdk 24 and a newer compileSdk than this app uses; upstream describes the code as legacy. | AC-6 |
| P4 | Upstream lacks three things the app's fork has: APPEND with an explicit internal date, UID SEARCH on an arbitrary header, and fetch of the `X-smssync-*` headers. | AC-7 |
| P5 | `ImapFolder.fetch` became a `suspend` function in September 2026; the app's mail code is Java. | AC-7 |
| P6 | Upstream still uses mime4j, commons-io, jzlib and jutf7. | AC-4 |
| P7 | Upstream implements PLAIN, LOGIN, CRAM-MD5, EXTERNAL, XOAUTH2 and OAUTHBEARER. | AC-8 |
| P8 | The TLS extension point `TrustedSocketFactory.createSocket(...)` has the same shape as the vendored one; the default factory with SNI is in an Android module outside the mail modules, and has no blank-host guard. | AC-9 |

P1-P8 make `no-go` or `go-with-conditions` plausible. They do not decide the outcome.

DES-MODERNIZATION-009 is partly out of date (its "19 files / 62 occurrences" and JitPack-pin statements predate the vendoring, and it names a contract `CNTR-MODERNIZATION-009-mailtransport` while the approved port contract is CNTR-MODERNIZATION-007) and may need revising if Part B goes ahead; this story follows REQ-019 and CNTR-007.

Constraints: web and repository access required; JDK 25 build, enforced dependency verification and no lowered JaCoCo floor are binding (EPIC-MODERNIZATION-006); no Play publishing concerns (private app); REQ-MODERNIZATION-015's minSdk 23 is approved and is changed only by the product owner.

### Estimation Guidance

Complexity: medium. One analysis document, about twelve sections, several tens of citations, a few small experiments (build upstream modules, compile a header-search or APPEND probe, measure release size), and a presentation step. Fits one sprint; Part A is not split. Risk concentrates in: upstream not building outside its own tree (hence the stop rule), network and rate limits on the GitHub API, the pin moving while the work is under way (fix the pin at I1), and the acceptance step, which depends on the product owner and may outlast the sprint, so the story stays open rather than closing on delivery.

## Supporting Documentation
- REQ-MODERNIZATION-019: Description, Context, Part A questions 1-12, AC-A1 to AC-A9, Constraints, Scope Concern
- EPIC-MODERNIZATION-006: requirement 5; success criteria 5, 6 and 9
- REQ-MODERNIZATION-002 (TLS), -009 and -012 (ACL), -015 (minSdk 23, toolchain), -016 (runtime libraries), -017 (compileSdk and targetSdk)
- DES-MODERNIZATION-009 (MailTransport anti-corruption layer; k-9 unpin ADR)
- CNTR-MODERNIZATION-007 `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md` (context only; not attached)
- `k9mail-vendored/NOTICE` (origin, U-027 eviction)
- `sdlc/artifacts/build/sprints/sprint-018/retro.md` (assembleRelease in gate set), `sdlc/artifacts/build/sprints/sprint-018/U-061-remove-donation-flow-and-play-billing.md` (test baseline 682)

## Integration Contract References
None. This story creates no interface. CNTR-MODERNIZATION-007 is read for context and asserted unchanged.

## Implementation Notes
<!-- Added by agents during build. Record: start commit; I0 porcelain and untracked-file lists; pin; research-input hashes (IC-5); experiment log pointer; IC-3 check result. -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes
- Dependencies between stories: none. Part A may run in parallel with REQ-MODERNIZATION-015/016/017; its Kotlin and coroutine findings are reconciled with them in Section 4.
- Scope Concern: the story does not close when the analysis is delivered; AC-15 requires the product owner's written acceptance, which no agent can give. If the sprint ends first, the story carries over rather than closing. Tracking acceptance separately would close the sprint sooner, but this story is written the binding way (acceptance required).
- Scope Concern: REQ-019 describes a NOTICE defect (mime4j 0.7.2 and commons-io 2.4). `k9mail-vendored/NOTICE` lines 20-21 list those as the original-equivalent versions and `k9mail-vendored/build.gradle` header comments (lines 16-18) record the later bumps, so the "defect" may be a historical record, not a mistake. The analysis reports what it finds; it does not edit the NOTICE.
