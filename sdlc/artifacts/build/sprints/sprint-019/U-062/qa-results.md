---
artifact_type: qa-results
story_id: "U-062"
gate_id: "qa-validation"
checkpoint: "quality-gate"
verdict: "PASS"                        # PASS | FAIL
agent: "QA Analyst"
timestamp: "2026-11-17"
ac_total: 22                           # AC-1..AC-16 plus IC-1..IC-6
ac_passed: 22                          # 16 MET (IC-2 on recorded evidence), 6 MET WITH DEVIATION (each recorded below)
ac_failed: 0
tests_run: 0                           # no test was run by QA: the story changes no product file (IC-1 empty); IC-2 rests on the recorded I4 gate run, see IC-2
tests_passed: 0
---

# QA Validation: U-062

## Verdict: PASS

## Summary

U-062 delivers a written feasibility analysis (REQ-MODERNIZATION-019 Part A) and changes no product code. I evaluated the working tree on 2026-11-17, because everything after commit `1e65fa7e` is uncommitted under the product owner's standing rule. All 16 acceptance criteria and 6 integration criteria are met. 16 are met outright (IC-2 on recorded evidence, not re-run). 6 are met with a deviation that is recorded and judged below: AC-2, AC-11, AC-14 (a whitespace edit), AC-15, IC-4 and IC-5.

What I re-derived myself:
- The heading order and every row count the story fixes.
- The five predicates, row T2 and the single `Recommendation:` line.
- The citation mechanics over all 582 rows.
- Every GitHub citation (389 rows), checked against the git objects at the cited SHA in the read-only scratch clones, with lazy fetching disabled.
- 27 web opens: 15 in the first pass (the cap I was then given), and on the coordinator's later instruction the 12 Section 1 registry and web probes that had rested only on the implementer's opens. There is also a semantic sample of 90 citation IDs (69 table rows).
- Six or more substantive conclusions, tested against the pinned upstream source and this repository.
- IC-1, IC-3, IC-4, IC-5 and IC-6 by command.
- The two at-acceptance edits, compared word for word with Record 3.

What rests on recorded evidence:
- The IC-2 gate run. It was not re-run because the product-path diff is empty. I read the surviving I4 build outputs.
- 128 citations in Sections 1, 3, 4, 6 and 7 that I neither web-opened nor resolved in the clones. I checked them against local copies of the cited artifacts or captures in the implementer's scratch folder (121 POM, jar and sources-jar copies, plus 7 captures: C-019, C-020, C-024 and the AAR metadata of C-216 to C-219). I did not re-fetch them. All other citations in those sections (143) were opened by me on the web (26) or read as git objects at the cited SHA (117).

One fact given to me was wrong. HEAD `1e65fa7e` does not hold the I0 to I4 state. It holds the analysis skeleton (151 lines, 21 `PENDING` tokens) and the log up to `I1 START` (`git show HEAD:<analysis> | wc -l` = 151). In practice, the whole analysis text, Records 1 to 3 and the at-acceptance edits are uncommitted.

## Acceptance Criteria Results

> **Rule**: Every AC marked PASS must cite at least one `file:line` reference in the Evidence column.

How verified: **R** = re-derived by QA on 2026-11-17 (command or reading stated). **E** = rests on recorded evidence (named). `A` = `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`. `S` = the story file `sdlc/artifacts/build/sprints/sprint-019/U-062-thunderbird-mail-libraries-feasibility-analysis.md`. `L` = `sdlc/artifacts/build/sprints/sprint-019/U-062/implementation-log.md`.

| Criterion (story text, condensed) | Check done | Evidence | Result |
|---|---|---|---|
| **AC-1** Analysis at the fixed path. Exactly 12 numbered sections "1" to "12" with REQ-019's question names. An "unknown" needs a stated experiment. | R: `grep -nE '^## ' A`. `grep -c PENDING A` (0). `grep -niE '\bunknown\b\|inconclusive' A`. `grep -nE '^### [0-9]' A` (0). | Headings in order: A:11 Baseline, A:99 `1. Consumability`, A:151, A:215, A:364, A:417, A:557, A:607, A:736, A:915, A:1154, A:1182, A:1262 `12. Recommendation`, then A:1437 Citations, A:2024 Experiment log, A:2122 Product owner decision. Titles match S:78 exactly. No section answer is "unknown". The hits are an encoding name (A:1069, A:1113) and "compile fit unknown" inside the supplementary Q11.e prose (A:1260). Experiments (c) and (d) are "not run" with reasons (A:2058, A:2061). That is allowed: AC-10 permits an estimate and AC-7 permits code citations. | MET |
| **AC-2** Every factual statement about Thunderbird or third-party libraries carries an ID that resolves to a complete Citations row. Distinct IDs used = rows. No empty column. The input is never a target. QA opens every citation in Sections 1, 3, 4, 6 and 7 plus a sample elsewhere (S:147). | R: my own parser (`<scratch>/u062/qa/citcheck.py`). It found 582 rows, C-001 to C-582 contiguous, no duplicate, 582 distinct IDs in the body, used-minus-rows and rows-minus-used both empty. Every row has 8 pipe fields and no empty cell. No branch-name URL, no short-SHA URL, no SHA mismatch, no reference to `inputs/` or the source file name. R: all 389 GitHub rows (286 blob, 83 commit, 18 tree, 2 compare) resolved with `git cat-file --batch` against the scratch clones (`GIT_NO_LAZY_FETCH=1`): 388 OK, 1 line range one past the end of file (C-556). Semantic sample of 90 citation IDs in 69 table rows (table below). 15 web opens in the first pass. Second pass on 2026-11-17, on the coordinator's instruction: 12 single foreground opens (curl, generic client string, HTTPS) of C-008, C-009, C-010, C-011, C-012, C-014, C-016, C-017, C-021, C-023, C-027 and C-032. All 12 answered as their rows say, and all 12 support the citing sentence (table "Second-pass opens" below). | Rows A:1441 (C-001) to A:2022 (C-582). Sections 1/3/4/6/7 hold 271 IDs. QA checked 117 against git objects, 26 by web open (14 + the 12 above), and 128 against local copies of the cited artifacts or captures (POMs, jars, AAR metadata, API JSON). None now rests only on the implementer's opens. One count in the citing text has moved since retrieval: A:110 says JitPack's control had "95 \"ok\" builds" [C-021], and on 2026-11-17 it shows 148 builds, 126 "ok". The row's claim ("answers 200 with ok builds") and its role as a control still hold. | MET WITH DEVIATION (see Deviations judged, D-a; narrowed) |
| **AC-3** Section 1: 5 registries, each with URL, date and result. A yes/no published answer. Signing and stability. A provenance verdict with a quote. The continuation repository. Ahead/behind with the compare URL. | R: read A:103-146. Web: Maven Central `net/thunderbird/` 200, listing only `components/`. `com/fsck/k9/` 404. Google master index 200, 0 matches for thunderbird/fsck/k9. Mozilla candidate metadata 404. JitPack thunderbird-android 200, 7 builds, all "Error". GitHub Packages maven page "0 packages". k9mail.app page holds both quoted sentences. Repo API redirect to id 1326671. R: `git rev-list --left-right --count <pin>...eaf689025e` = 15032 / 11. `merge-base` = `23bf474d93…`. | A:107-111 (5 rows), A:117-122 (artifacts found, `.asc`, release vs snapshot), A:128 "**no**", A:132-136 (verdict "partly correct", three quotes), A:140 (continuation repository), A:144 (11 ahead / 15032 behind, compare URL, merge base). `k9mail-vendored/NOTICE:5` holds the vendored SHA. | MET |
| **AC-4** Section 2: 14 rows naming module and type. Deps per module with versions. The legacy four replaced or retained. A recount of the 14. | R: row count by parser. `grep -rln "com\.fsck\.k9" app/src/main/java \| grep -v /mail/transport/ \| wc -l` = 14 at the working tree. Product paths equal the start commit, so this is the start-commit count. | A:169-182 (14 rows), A:161 (recount, difference none), A:190-199 (8 modules, direct and transitive deps with versions), A:207-212 (commons-io, mime4j, jzlib, jutf7, each "Retained" with versions). | MET |
| **AC-5** Section 3: SPDX and a yes/no verdict for every module and transitive dependency. A cited source. "POM only" marked. NOTICE obligations. MPL file-level obligations. A non-yes blocks `go`. | R: parser over Q3.a (33 rows) and Q3.b (72 rows). The verdict column holds only "yes". Web: the jzlib POM says only "BSD", which the analysis marks and resolves from the sources-jar header (local jar: three-clause header). slf4j-parent POM: MIT. guava-parent POM: Apache 2.0. kotlin-stdlib POM: Apache-2.0. | A:225-261 (Q3.a, count line A:261), A:269-344 (Q3.b, A:344), A:250 (jzlib row with the "POM says only BSD" note), A:346-348 (MPL 3.1 to 3.4), A:350-359 (NOTICE list), A:363 (NOTICE-defect finding). | MET |
| **AC-6** Section 4 table: 8 rows (minSdk, compileSdk, Kotlin, coroutines, AGP, JVM target, JDK, Gradle). Upstream value cited. Target from the owning REQ by file:line. App value by file:line. A verdict. "Not specified" cannot be "no conflict". Conflicts go to the PO. | R: parser (8 rows, 8 fields each). Opened the cited REQ lines: `REQ-MODERNIZATION-015:34` (minSdk 21 to 23), `:52` Gradle 9.9.1, `:53` AGP 9.5.1, `:54` Kotlin 2.4.21, `:84-86` (latest-stable rule), `:113` (AC-5 minSdk 23), `:157` (bytecode 17/8), `:189` (JDK 25 stays). `REQ-MODERNIZATION-016:35-36`, `:62` (coroutines 1.11.0). `REQ-MODERNIZATION-017:19-20`, `:31-36` (no level named). App lines `app/build.gradle:21,24,25,90-91,107,118-119`, `build.gradle:10,14`, `gradle/wrapper/gradle-wrapper.properties:4`, `gradle/gradle-daemon-jvm.properties:2`. All match. Upstream: C-221 (24/36/37), C-226 (AGP 9.5.1), C-231 (Gradle 9.7.0), C-233 (JDK 21), read at the pin. | A:383-390. compileSdk reads "not specified in REQ-MODERNIZATION-017" with verdict "conflict (unresolved: target not specified)" (A:384). minSdk is a conflict in M2 (A:383). PO-1 and PO-2 are at A:1330-1331. Neither the analysis nor any changed file alters 23 (`git diff --stat c8f04558 -- <REQ-015/016/017>` is empty). | MET |
| **AC-7** Section 5: one row each for the 8 operations. FETCH states each `X-smssync-*` header separately (13 + 2 = 15). X-GM with grep and scope. MIME build in 4 separate parts. No blank cell. Each row has the replacement call, the behavioural difference and "Java-callable: yes/no/via shim" with a code citation. | R: parser found 8 operation rows and 0 empty cells, 15 header rows, 9 port rows and 5 exception rows, all with 0 empty cells. `grep -cE '"X-smssync-' app/src/main/java/com/zegoggles/smssync/mail/Headers.java` = 13. References and Message-ID at `Headers.java:25,26`. The "needs fork" rows were re-tested (see the conclusion checks). | A:425-432 (rows 1-8). A:431 (MIME build with four labelled parts). A:430 (X-GM grep commands and scope, "folder naming only" confirmed). A:473-487 (15 header rows). Java-callable cells cite C-071, C-072, C-249, C-254 and others, checked in the sample. A:434 summary. | MET |
| **AC-8** Section 6: PLAIN, LOGIN, XOAUTH2 and OAUTHBEARER (and others) yes/no, each with a code citation. How selected. Token provider interface vs implementation and what the app supplies. A Google OAuth consequence in at most 2 sentences. | R: read the rows. Checked C-313, C-321, C-324, C-300, C-253 and C-333 at the pin (`authenticate()` `when(settings.authType)`. The XOAUTH2 branch requires a provider and SASL-IR and prefers OAUTHBEARER. `RealOAuth2TokenProvider` is in `legacy/common`). | A:565-571 (7 rows incl. CRAM-MD5, EXTERNAL, others "no"). A:575-579 (selection). A:583-585 (interface vs implementation, what the app supplies). A:601 (two sentences). | MET |
| **AC-9** Section 7: 4 protections, each with an extension point, a code reference and a status. The SNI row covers the 10 test cases. Pinning store semantics stated. A blocker blocks `go`. | R: parser found 4 rows, all "**supported**". Checked C-067, C-355, C-356, C-240, C-338, C-340, C-345 and C-348 at the pin. `git grep` for HostnameVerifier, endpointIdentification, SSLParameters and serverNames over both mail modules finds only `TrustManagerFactory.java:20,75`. `grep -c "@Test" DefaultTrustedSocketFactorySniTest.java` = 10. Inputs read at its lines 53-236. The surviving I4 JUnit XML for that class shows tests=10, failures=0. | A:615-618 (4 rows), A:620 (0 blockers, 0 fork rows), A:624 (store semantics: user-accepted exception vs reject everything else), A:653-662 (10 SNI rows with basis), A:660 (the "full IPv6" test passes `fe80::1`, and the eight-group case is marked code reading only). | MET |
| **AC-10** Section 8: a baseline at the start commit from `assembleRelease` (bytes, dex methods, tool, apkanalyzer note). A delta measured or labelled "estimate". Verification-metadata entries by artifact. | R: read. `wc -c app/build/outputs/apk/release/app-release-unsigned.apk` = 1995017 (the I4 output, dated 2026-11-13 13:34). | A:744-748 (1,995,017 bytes, 23,904 method ids by `dexdump -f`, apkanalyzer note at A:746). A:822-838 (deltas, each labelled **estimate**). A:839-848 (112 / 80 / 208-256, split into jar, module and pom). A:738 ("Upstream was not built"). | MET |
| **AC-11** Section 9: at least the 3 options (newer snapshot, fork, stay), each with counted cost, supply-chain risk citing the U-027 eviction, and the patch owner. | R: parser found 3 option rows. Read the cost cells. | A:923-925 (three options, counted files and entries, U-027 cited via `k9mail-vendored/NOTICE:25-30`, patch owner per row), A:917 (no published artifact, so options are required). The Q9.f extension (A:960-1152) and the Q12.c extension row (A:1315) understate R-A's re-apply count ("10 source files"). It is 19 for A0-A3 when import-only files are counted (code review W1, reproduced by QA from `git show --stat` of the four post-capture commits: 10 files, plus 9 of the 12 import-rewrite files). | MET WITH DEVIATION (D-d) |
| **AC-12** Section 10: separable or inseparable for RCS and for Google OAuth, each with cited evidence. If inseparable, it is recorded for the PO. | R: parser found 2 rows, both "separable". Re-ran `git grep -niwE 'rcs\|rich communication'` at the pin: 0 lines in the in-scope modules, and 2 binary files in the whole repo (C-429 confirmed). | A:1160-1161, A:1163-1180 (evidence, C-429 and others). Q12.g restatement at A:1381-1383. | MET |
| **AC-13** Section 11: one sprint or its own epic, with counted reasoning. A breakdown table with dependencies, acyclic. Prose only, and no file created. | R: parser found 10 rows. Checked every "Depends on" entry: each points to a lower row number or to REQ-015/016/017, so the graph is acyclic. No new story, design or requirement file (IC-4). | A:1186-1210 (counts), A:1237-1239 ("own epic", 6 counted reasons), A:1245-1254 (10 rows), A:1256 (topological order). | MET |
| **AC-14** Section 12 is last. Then only Citations, Experiment log and PO decision, in order. "AC-16 not applicable" before the Recommendation line. Exactly one `Recommendation: <value>` line, last in Section 12. Conditions numbered and tested. Blocking rules respected. | R: `grep -nE '^Recommendation: (go\|go-with-conditions\|no-go)$' A` gives exactly one hit, A:1436. The last non-blank line of A:1262-1436 is A:1436. `AC-16 not applicable.` at A:1434. Level-2 headings after A:1262 are A:1437, A:2024, A:2122, in order. Predicates re-derived: B false (0 blockers, A:620), F true (Section 5 rows 3 and 4 "needs fork", A:427-428), G false (both conflicts M2-only, A:405-408), S false, P true (P-1..P-5). Row T2 permits `go-with-conditions` or `no-go` (plan.md:671). Conditions: 17 rows, 0 empty cells, each with a test and an owner. | A:1270-1274 (predicates), A:1288 (T2), A:1282-1284 (the tie-break is labelled the implementer's), A:1349-1365 (17 conditions), A:1330-1343 (14 PO items), P items at A:641-643 and in Q5.p. The orchestrator inserted one blank line in Q12.c (L:810). QA confirmed the blank line is present before "Option comparison extended". The Recommendation line moved from 1435 (code review) to 1436, which agrees with one inserted line. | MET WITH DEVIATION (D-1, whitespace only) |
| **AC-15** Presented after IC-1..IC-5 pass. The decision is recorded verbatim in the last appendix with the date and the recorder. An agent cannot accept. The story is not `done` without "accepted". No Part B artifact before the acceptance date (`git log --diff-filter=A`). | R: `git log --diff-filter=A --format='%ad %h' --date=short --name-only c8f04558..HEAD` gives only `1e65fa7e` (2026-11-13), and all its added files are U-062 Part A or workflow files. `git log --since=2026-11-01 --diff-filter=A -- sdlc` adds nothing else Part-B-like. Working tree: `git ls-files --others --exclude-standard` and `git diff --name-status c8f04558` show no new REQ, DES, CNTR, CR or U file other than U-062. Records 1 and 2 match the pre-IR1 and pre-IR2 copies in `<scratch>` (diff of non-blank lines: identical). Story status `review` (S:3). | A:2126-2136 (Record 1, 2026-11-13, `changes requested`), A:2138-2149 (Record 2), A:2151-2162 (Record 3, 2026-11-17, `accepted`, verbatim "Accept: full migration", recorder Orchestrator, A:2157-2161). Presentation after the checks: L:589-597, L:805-813, L:815-819. | MET WITH DEVIATION (D-b) |
| **AC-16** If `no-go`, records are needed. Otherwise Section 12 says "AC-16 not applicable" before the Recommendation line. | R: the value is `go-with-conditions` (A:1436). Statement at A:1434. Record 3 says not applicable (A:2160). No `CR-*` file was added (`git diff --name-status c8f04558` lists none). | A:1434, A:2160 | MET |

### Integration Criteria

| Criterion (story text, condensed) | Check done | Evidence | Result |
|---|---|---|---|
| **IC-1** No product or build file changed. `git diff --stat <start> -- app k9mail-vendored metadata gradle settings.gradle build.gradle gradle.properties gradlew gradlew.bat` is empty. Porcelain differs from I0 only by permitted files and pipeline bookkeeping. | R: the command against `c8f04558` gives 0 bytes. The same against `HEAD` gives 0 bytes. Porcelain compared with the I0 record (A:47-51, A:55-66). Every path is classified below. | Path table under "Path classification". No product path appears in `git status`. | MET |
| **IC-2** Gates pass at the end on JDK 25 without `MSYS_NO_PATHCONV`. Tests 682 run, 2 skipped. No test, floor or verification setting changed. | Not re-run by QA. IC-1 shows no product, build or test file changed since the start commit, both committed and in the working tree, so the gate inputs equal those of the I4 run. R: the surviving outputs of that run read with `python -I sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/tools/junit_totals.py app/build/test-results/testDebugUnitTest` give `files=73 tests=682 failures=0 errors=0 skipped=2` (dated 2026-11-13 13:34). APK 1995017 bytes. `git grep -h "@Test" c8f04558 -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' \| grep -c "@Test"` = 682 (same at HEAD). E: `<scratch>/u062/logs/I4-gates.log` shows `U061-DAEMON-JVM: java.version=25.0.5.0.3`, `BUILD SUCCESSFUL in 1m 12s`, and all four gate tasks. `I4-gates.exit` = `exit=0`. The runner script unsets `MSYS_NO_PATHCONV`. | A:2032 (I4 re-run), L:554-559 | MET (rests on recorded evidence; see D-c) |
| **IC-3** No e-mail address, host name, Windows user name or user-profile path in any file the story adds. The check and its result are recorded in the analysis. | R: read `<scratch>/u062/ic3.sh` first. It prints only file:line, never the matched text. Pattern files equal the story's regexes. Run with `IC3S=<scratch>/u062` over the analysis, input copy, story, log, plan, plan-review, contract-verification, expert-depth-check, both reviews, sprint-plan.md, execution-plan.json, REQ-019, EPIC-006 and `.skill-context.json`. Control `paths=4 email=1 host_user=2`, guard silent. E-mail 0, host/user 0, path-pattern 2 lines. Extra QA checks: forward-slash drive profile paths, the 8.3 short-name form, home-dir paths and scratch path fragments gave 0 matches. The added lines of `.skill-delegation-audit.log` gave 0, 0, 0. | The 2 path hits are descriptions of the patterns, not leaks: S:101 (the IC-3 criterion text) and `review-security.md:78` (the reviewer's description of what it scanned). Analysis record at A:2080-2082. | MET |
| **IC-4** The untracked list minus I0, plus the name-status diff, shows only the analysis, input copy, story and (after acceptance, AC-16 only) CRs. No clone, build output, `.gitmodules`, or new REQ/DES/CNTR/U file. Experiment log complete per experiment. | R: commands run (table below). `ls .gitmodules`: no such file. `find` in the repo for `settings.gradle.kts`, `libs.versions.toml`, `RealImapFolder.kt` and `SniProbe.java`: none. `git status --ignored`: only the normal build directories, the pre-existing sprint-017 evidence and `compiled-pipeline.json` (local exclude). Experiment log entries (a)-(g), IC and IR entries at A:2028-2120. | Beyond the literal IC-4 list: the D4.2 workspace and sprint bookkeeping, and the D4.6 reference edits to REQ-019 and EPIC-006. Experiment (f) jar-download command not recorded (A:2072, deviation 28). | MET WITH DEVIATION (D-e) |
| **IC-5** The input is copied, carries the banner, is byte-identical to the source except listed redactions, and both SHA-256 hashes are recorded. | R: `head -1` = `UNREVIEWED INPUT - not a citation source`. `sha256sum` of the copy = `342e149a…cf50e2`. `tail -n +2 \| sha256sum` = `f2b9610b…aece9`. The source file in the orchestrator's scratch (`thunderbird-mail-libs-research.md`) hashes to `f2b9610b…aece9`, with `wc -l` = 290. The copy has 291 lines and 2 mentions of the pin. | A:86-95 (hashes, banner, line counts, "redactions none"). | MET WITH DEVIATION (story text says 291 source lines; measured 290; identity proven by hash, plan D4.3) |
| **IC-6** A Baseline section before Section 1 with start commit, Gradle, AGP, Kotlin, JDK, bytecode, minSdk, compile/targetSdk and test count, with file:line. States 682, not 697. | R: opened every cited line. `settings.gradle:1`, `gradle/wrapper/gradle-wrapper.properties:4` (9.5.1), `build.gradle:10` (9.0.1), `:14` (2.3.21), `gradle/gradle-daemon-jvm.properties:2` (25), `app/build.gradle:21,24,25,53,90-91,107,118-119`, `k9mail-vendored/build.gradle:52,54,76-77,100,102,107,109,111,114`, `k9mail-vendored/NOTICE:4-6`. All values match. | A:11-37, A:13 (682, not 697) | MET |

## Story text, plan and status consistency

- Story status is `review` (S:3), as expected; I did not change it. `current_build_phase` is `validation` (S:26). The story was edited only in its frontmatter (status, `updated_at`, phase) and in Implementation Notes (S:205-257): `git diff HEAD -U0` hunks at S:3, S:25-26 and S:205-257. The Review Findings section (S:259-260) is empty.
- The Implementation Notes agree with the files up to IR2. Counts 582 / 14 / 17 / 5, row T2 and the Recommendation line all match. Two gaps:
  - The Notes do not record Record 3 or the D4.6 edits.
  - The Known-gaps bullet at S:257 still says the Product owner decision appendix "holds the plan's placeholder", which is now false.
  - The earlier "Result" paragraph (S:219) carries superseded counts, but S:225 says the IR1 counts supersede it.
- Record 3 and the log: "Accept: full migration" is the label of an option the orchestrator wrote, which the product owner selected. The record says so (A:2158). The acceptance act is the product owner's. The wording of the verbatim text is not.

## Citation sample (semantic check: does the source say what the citing sentence says)

Method:
- GitHub rows: the object at the cited SHA was read from the scratch clones (`thunderbird-android` at `ac1f0e85…`, `thunderbird-mobile-components` at `7aa2590f…`, which also holds the fetched `jberkel/k-9` fork commits) with `GIT_NO_LAZY_FETCH=1`. Nothing was missing.
- "Web" = one of my 15 single opens on 2026-11-17 (curl, generic client string).
- "Local copy" = a copy of the cited artifact or capture in the implementer's scratch folder, not re-fetched.

| ID | Sect. | Kind | How checked | Result |
|---|---|---|---|---|
| C-003 | 1 | blob | lines 4-10 | `java-library`, kotlin.jvm, 3 quality plugins, no publish plugin: supports |
| C-005 | 1 | tree | `git grep -nE 'maven-publish\|signing\|publishing'` over mail, build-plugin | only `SigningExtensions.kt` (APK signing) and a README link. 0 kts/toml files with publishing: supports |
| C-006 | 1 | registry | web: 200 | lists only `components/`: supports |
| C-007 | 1 | registry | web | 404: supports |
| C-013 | 1 | registry | web: 200, 10422 bytes | 0 thunderbird/fsck/k9, `androidx.annotation` present: supports |
| C-015 | 1 | registry | web | 404: supports |
| C-018 | 1 | JitPack API | web: 200 | 7 builds, all "Error": supports |
| C-019, C-020 | 1 | JitPack API | local capture | 27 builds / 5 ok, 19 builds / 11 ok incl. `eaf689025e`: supports (not re-fetched) |
| C-022 | 1 | web page | web: 200 | "0 packages": supports |
| C-024 | 1 | API | local capture | 401 "Requires authentication": supports (not re-fetched) |
| C-025 | 1 | web page | web: 200 | both quoted sentences present. "acquired", "bought" and "purchase" absent: supports |
| C-026 | 1 | web page | web: 200 | "K-9 Mail officially joins the Thunderbird family" present. "bought" once, inside a reader comment: supports A:132 |
| C-028 | 1 | API | web: 200 after 1 redirect | id 1326671, `thunderbird/thunderbird-android`, not archived, `main`, Apache-2.0: supports |
| C-029 | 1 | compare | local `rev-list --left-right --count` | 15032 / 11, merge base `23bf474d…`: supports (API not re-fetched) |
| C-031 | 1 | blob (TMC) | README lines 5-7 | "work in progress and is not yet ready for production use": supports |
| C-034 | 1 | fork commit | object read | subject "Set internal date on APPEND": supports |
| C-051 | 2 | blob | lines 16-29 | `class Address @JvmOverloads constructor(address, personal = null, parse = true)`: supports |
| C-064 | 2 | blob | lines 26-58 | items FLAGS, ENVELOPE, STRUCTURE, BODY_SANE, BODY; no DATE: supports |
| C-072 | 2, 5 | blob | lines 14-52, and `suspend fun fetch(` at `ImapFolder.kt:47` | interface, suspend fetch: supports |
| C-073 | 2 | blob | lines 102-108 | `create(folderType: FolderType = FolderType.REGULAR)`, `suspend fun onFetchResponse`: supports |
| C-077 | 2 | blob | lines 33-34 | `internal class RealImapFolder(`: supports |
| C-078 | 5 | blob | lines 3-8 | 4 members, no timeout: supports |
| C-079 | 2, 11 | blob | lines 31-38 | `@Discouraged … object Log`, `lateinit var logger`: supports |
| C-082 | 2 | tree | `git ls-tree -r` at the pin | 0 files named ImapSearcher, StoreConfig, NetworkType or XOAuth2AuthenticationFailedException: supports |
| C-085 | 3 | POM | web: 200 | licence name only "BSD": supports the "POM says only BSD" note |
| C-090 | 3 | POM | web: 200 | deps httpcore5, httpcore5-h2, slf4j-api (others optional or test). No 4.x httpclient: supports |
| C-102, C-103 | 3 | blob | LICENSE 1-4, NOTICE 1-3 | Apache 2.0 text; the three NOTICE lines quoted at A:355: supports |
| C-107 | 3 | blob (TMC) | LICENSE 160-204 | MPL 3.1, 3.2 (and on): supports |
| C-110 | 3 | sources jar | local copy | `ZStream.java` header, BSD three conditions: supports |
| C-113 | 3 | POM | web: 200 | `Apache-2.0`: supports |
| C-128 | 3 | jar NOTICE | local copy | "Apache James :: Mime4j :: Core, Copyright 2004-2025": supports A:241 |
| C-137 / C-138 | 3 | POM / parent POM | local / web: 200 | slf4j-api has parent slf4j-parent, which declares MIT: supports |
| C-149 / C-150 | 3 | POM / parent POM | local / web: 200 | no `<licenses>`, parent guava-parent 26.0-android, which declares Apache 2.0: supports |
| C-216 to C-219 | 3, 4 | AAR metadata | local extraction (`aar-meta.txt`) | minSdk 23; minCompileSdk 37 / 37 / 36 / 1: supports (implementer's extraction) |
| C-221 | 4 | blob | lines 13-19 | sdkMin 24, sdkTarget 36, sdkCompile 37: supports |
| C-226, C-231, C-233 | 4 | blob | single lines | AGP 9.5.1; `gradle-9.7.0-all`; `.java-version` 21: supports |
| C-241 | 4 | blob (TMC) | lines 15-27 | sdkMin 23, compile 37, JVM 17: supports |
| C-361 | 4 | blob | line 138 | `android-lint = { id = "com.android.lint" … }`: supports |
| C-245 | 5 | blob | lines 26-34 | companion `create(serverSettings, config, trustedSocketFactory, oauthTokenProvider)`: supports |
| C-254 | 5 | blob | lines 62-68 | `search(queryString, requiredFlags, forbiddenFlags, performFullTextSearch)`: supports |
| C-264 | 5 | blob | lines 1022-1044, plus `git grep -n APPEND` | the only format is `"APPEND %s (%s) {%d}"` (`RealImapFolder.kt:1040`), no date-time: supports |
| C-269 | 5 | blob | line 9, plus the builder body | `class UidSearchCommandBuilder {` (package-private). Emits TEXT or OR SUBJECT/FROM/TO/CC/BCC plus 6 flag terms. No HEADER: supports |
| C-273 | 5 | blob | lines 107-110 | `private fun executeSimpleCommand`: supports |
| C-276 | 5 | blob | lines 1113-1127 | only `UID SEARCH HEADER MESSAGE-ID`. `git grep` for SENTSINCE/UNDELETED = 0: supports |
| C-277 | 5 | blob | lines 593-601 | the ENVELOPE field list as quoted: supports |
| C-433, C-434, C-436, C-437 | 5 | blob | cited ranges | `SOCKET_READ_TIMEOUT = 60000`; internal `RealImapConnection` with timeout defaults; `socket?.soTimeout = timeout`; `createImapConnection()` passes no timeout: supports |
| C-253 | 6 | blob | lines 5-40 | interface, `OAUTH2_TIMEOUT: Int = 30000`: supports |
| C-300 | 6 | blob | cited range | `invalidateToken()` then one retry: supports |
| C-313, C-321 | 6 | blob | cited range | `when (settings.authType)`. XOAUTH2 needs a provider and SASL-IR and prefers OAUTHBEARER: supports |
| C-324 | 6 | blob | cited range | `getToken(OAUTH2_TIMEOUT)`, `sensitive = true`: supports |
| C-333 | 6 | blob | lines 19-22 | `RealOAuth2TokenProvider` in `legacy/common`: supports |
| C-067 | 7 | blob | lines 9-17 | `createSocket(socket: Socket?, host: String, port: Int, clientCertificateAlias: String?)`: supports |
| C-240 | 7 | blob | line 20 | `import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier`: supports |
| C-338 | 7 | blob | lines 90-119 | blocklist, then system trust, then hostname verify, then store only on failure: supports |
| C-340 | 7 | blob | lines 111-138 | stored certificate must equal the presented one: supports |
| C-345, C-348 | 7 | blob | cited ranges | `legacy/core` factory `(context, trustManagerFactory)`. `setSniHost` builds `SNIHostName(hostname)` with no try/catch: supports |
| C-355, C-356 | 7 | blob | cited ranges | `socketFactory.createSocket(...)` for implicit TLS and after STARTTLS: supports |
| C-373 | 8, 9 | blob | lines 8-25 | 13 `implementation(plugin(...))` lines (counted): supports |
| C-414 | 9, 11, 12 | blob | lines 207-214 | "mail:* and backend:* modules are legacy … being gradually replaced": supports |
| C-418 | 9 | PR comment | web: the issue-comments API of PR 2895 (not the row's page URL) | 2017-11-05 collaborator comment with the quoted sentence: supports |
| C-423 | 9, 12 | commit | object + `merge-base --is-ancestor` | 2026-10-26 "feat(security): add CertificateBlockList. Update OkHttp", ancestor of the pin: supports |
| C-427 | 9, 12 | history | `git log` counts | 252 / 17 / 6: supports |
| C-429 | 10 | tree | `git grep -niwE` | 0 lines in the modules; whole repo 2 binary files (PNG, woff2): supports |
| C-448, C-449, C-451 | 9 | fork commits | objects read; `--stat` of 013948a758 | "Increae read timeout" (1 file, `RemoteStore.java`), "Retry socket connections", "Fix potential ArrayIndexOutOfBoundsException": supports |
| C-468, C-555 | 9 | commit / blob | object; blob lines 32-36 | at `dc119f9068` (2020-06-08): `class ImapFolder internal constructor(`; parent `be7849eb5d` has `public class ImapFolder`: supports |
| C-476 | 9 | commit | object | 2026-09-17, message "Add suspend to ImapFolder.fetch": supports |
| C-487 | 9 | commit | object | 2024-01-06 "Add check to prevent the use of IP addresses as SNI hostnames": supports |
| C-521, C-536, C-571, C-575 | 9 | blob | cited lines | A0 constructor with `OAuth2TokenProvider`; A1 builder with no HEADER; A0 `class ImapFolder` package-private; A4 `public class ImapFolder`: supports |
| C-556 | 9 | blob | lines 9-30 | the claim is correct (interface plus companion `create`), but the file has 29 lines, so `#L9-L30` runs one line past the end. Minor defect; the page still opens |

Sample result: 90 citation IDs (69 table rows) checked for content. All support their claims. One row (C-556) has a line range that runs one line past the end of the file. No citation that I opened failed to open.

### Second-pass opens (2026-11-17, coordinator's instruction; one at a time, foreground, curl with a generic client string, HTTPS)

| ID | URL opened | Row expects | Observed 2026-11-17 | Supports the citing sentence? |
|---|---|---|---|---|
| C-008 | `https://repo1.maven.org/maven2/app/k9mail/` | 404 | 404 | yes (absence probe answers as recorded) |
| C-009 | `https://repo1.maven.org/maven2/org/apache/james/apache-mime4j-core/` | 200 (control) | 200, version directories listed | yes (an existing group lists) |
| C-010 | `https://repo1.maven.org/maven2/net/thunderbird/components/core/` | 200, logging/outcome/testing with variants | 200: `logging/`, `outcome/` (+ android, iosarm64, iossimulatorarm64, jvm, wasm-js), `testing/` (+ the same variants) | yes |
| C-011 | `https://repo1.maven.org/maven2/net/thunderbird/components/bom/maven-metadata.xml` | release 0.1.0 only, last updated 2026-10-12 | 200: latest = release = 0.1.0, one version, `lastUpdated 20261012134331` | yes |
| C-012 | `https://repo1.maven.org/maven2/net/thunderbird/components/core/outcome-jvm/0.1.0/outcome-jvm-0.1.0.jar.asc` | 200, detached signature | 200, 195 bytes, begins `-----BEGIN PGP SIGNATURE-----` | yes |
| C-014 | `https://dl.google.com/dl/android/maven2/net/thunderbird/` | 404 | 404 | yes |
| C-016 | `https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/maven-metadata.xml` | 200 (control) | 200, groupId `org.mozilla.geckoview`, artifactId `geckoview` | yes |
| C-017 | `https://maven.mozilla.org/maven2/org/mozilla/components/` | 404 for an existing group directory | 404 | yes (directory probes prove nothing on this registry) |
| C-021 | `https://jitpack.io/api/builds/com.github.jitpack/gradle-simple` | 200 with "ok" builds | 200: 148 builds, 126 "ok" | yes for the row's claim. Changed since retrieval: the citing text A:110 says 95 "ok" builds. The count grew, and the control still works |
| C-023 | `https://github.com/orgs/thunderbird/packages` | 200, "23 packages" | 200, "23 packages" | yes |
| C-027 | `https://blog.thunderbird.net/2022/06/faq-thunderbird-mobile-and-k-9-mail/` | page (FAQ; no purchase described) | 200; mentions K-9 and Thunderbird; 0 occurrences of acquired, acquisition, bought, purchase, buy | yes (supports A:132 "the words … do not occur in … the FAQ") |
| C-032 | `https://api.github.com/repos/jberkel/k-9` | 200, fork true, last push 2018-03-13 | 200: `fork` true, `pushed_at` 2018-03-13T21:53:12Z, parent `thunderbird/thunderbird-android` | yes |

## Independent tests of substantive conclusions

1. **No mail module is published (Section 1, A:128).**
   - At the pin, the mail convention plugin applies no publishing plugin (C-003).
   - `git grep` finds no `maven-publish` or `publishing` in any `*.gradle.kts` or `*.toml` (0 files).
   - Maven Central `com/fsck/k9/` is 404, and `net/thunderbird/` lists only `components/`.
   - Google's master index has 0 matches. Mozilla's candidate metadata is 404. JitPack has 0 "ok" builds for thunderbird-android. The GitHub Packages maven page shows "0 packages".
   - **Confirmed.**
2. **APPEND with internal date needs a fork (Section 5 row 3, A:427).**
   - The only APPEND format at the pin is `"APPEND %s (%s) {%d}"` (`RealImapFolder.kt:1040`).
   - `ImapConnection` is `internal interface` and `RealImapFolder` is `internal class`, so the app has no raw-command route.
   - The vendored code sends the date (`k9mail-vendored/.../ImapFolder.java:1174-1177`).
   - **Confirmed.**
3. **UID SEARCH on an arbitrary header needs a fork (row 4, A:428).**
   - `UidSearchCommandBuilder` is package-private and emits only TEXT, the five-field OR, and flag terms.
   - `executeSimpleCommand` is private. `git grep` for SENTSINCE, UNDELETED or `HEADER X` in the IMAP main source = 0. The only HEADER search is MESSAGE-ID.
   - **Confirmed.**
4. **Read timeout: 120 s in the fork, 60 s upstream, no override (Q5.t, A:491-509).**
   - Vendored: `RemoteStore.java:21` `SOCKET_READ_TIMEOUT = 120000`, applied at `ImapConnection.java:229`. Fork commit `013948a758` changes one line of `RemoteStore.java`.
   - Pin: `NetworkTimeouts.kt` `60000`. `RealImapConnection` is internal and takes the timeout as a constructor default. `createImapConnection()` passes none. `RealImapStore` is `internal open class`. `ImapStore.create` has 4 parameters and none is a timeout. `ImapStoreConfig` has 4 members and none is a timeout.
   - **Confirmed.**
5. **All four TLS protections "supported" (Section 7, A:615-618).**
   - The library obtains its socket from the supplied `TrustedSocketFactory` on both the implicit-TLS and the STARTTLS path (C-355, C-356).
   - The interface keeps the same FQN and erased shape (C-067). `PinnedCertificateSocketFactory implements TrustedSocketFactory` with the four-argument `createSocket` (`PinnedCertificateSocketFactory.java:55,75-76`).
   - The vendored guard `isIpOrBlankHostname` (`DefaultTrustedSocketFactory.java:216-226`) is app-ownable code.
   - The mail modules hold no hostname verifier other than `TrustManagerFactory`, and have 0 `org.apache.http.` references at the pin. In this repository, the 31 `org.apache.http.` lines are in the 5 webdav files (excluded from the source set, `k9mail-vendored/build.gradle:64-69`) plus 2 comment lines in `TrustManagerFactory.java`. That matches Q7.d.
   - The SNI test class has 10 `@Test` methods, and the "full IPv6" case passes `fe80::1` (test line 207).
   - **Confirmed, including the disclosed limit that the eight-group literal is code reading only.**
6. **From 2020-06-08 `ImapFolder` is a final Kotlin class that the app cannot subclass (Q9.f, A:987, A:1132).**
   - At `dc119f9068` (author date 2020-06-08): `class ImapFolder internal constructor(`. That is final by Kotlin default, so Java cannot extend it.
   - At its parent `be7849eb5d` (the rename commit, same date) the file still reads `public class ImapFolder`.
   - The app's `class BackupFolder extends ImapFolder` is at `K9MailTransport.java:696`.
   - **Confirmed.**
7. **A "vendored copy affected?" verdict: Q9.f item 5, `10b022ba40` (A:1112).**
   - The upstream diff changes `new String(data, "US-ASCII")` to `"UTF8"` in `ImapResponseParser.java` (1 main file, +1 -1). The commit is an ancestor of the pin.
   - The vendored `ImapResponseParser.java:392` still reads `"US-ASCII"`. The "yes" verdict is **confirmed**.
   - I also read item 8: `ImapConnection.java:281-283` has `responses.size() != 2`. **Confirmed.**
8. **Fork position (Q1.e).**
   - `rev-list --left-right --count` = 15032 behind / 11 ahead, and merge base `23bf474d9389…`. **Confirmed.**
9. **"10 source files of this repository's own changes" (Q9.f 3(c), A:1053).**
   - `git show --stat` of `55370179`, `4616c097`, `e5445d56` and `bdea6f4f` touches exactly the 10 named files (plus `build.gradle`).
   - The capture commit's androidx import rewrite (12 files, 3 overlapping) is not in the total, so the total is 19 for A0-A3. **Code review W1 confirmed; see D-d.**

## Path classification (IC-1, IC-4)

Commands run (2026-11-17, working tree):
- `git status --porcelain`
- `git status --porcelain --untracked-files=all` (same list)
- `git ls-files --others --exclude-standard`
- `git diff --name-status c8f04558`
- `git status --porcelain --ignored`

I0 records: A:47-51, A:55-66, A:70-80.

| Path | State now | Present at I0? | Class | Allowed by |
|---|---|---|---|---|
| `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` | A vs start; ` M` vs HEAD | no | the analysis | Affected Code (S:112) |
| `sdlc/artifacts/build/feasibility/U-062/inputs/thunderbird-mail-libraries-research-2026-11-12.md` | A vs start; clean vs HEAD | no | input copy | Affected Code (S:113) |
| `sdlc/artifacts/build/sprints/sprint-019/U-062-thunderbird-mail-libraries-feasibility-analysis.md` | A vs start; ` M` vs HEAD | yes (untracked) | the story (real path) | Affected Code (S:114) with plan D4.1 |
| `sdlc/artifacts/build/sprints/sprint-019/U-062/implementation-log.md` | A; ` M` | no | pipeline workspace | plan D4.2 |
| `.../U-062/plan.md`, `plan-review.md`, `contract-verification.md`, `expert-depth-check.md` | A; clean | yes | pipeline workspace | plan D4.2 (in the I0 list) |
| `.../U-062/review-code.md`, `review-security.md` | `??` | no | pipeline workspace | plan D4.2 |
| `.../U-062/qa-results.md` | new (this file) | no | pipeline workspace | plan D4.2 |
| `.../U-062/compiled-pipeline.json` | ignored (local exclude) | yes | pipeline bookkeeping, not a story file | plan D4.2 |
| `sdlc/artifacts/build/sprints/sprint-019/sprint-plan.md` | A; ` M` (status `in-progress` to `review`, one row) | yes | sprint bookkeeping | plan D4.2 |
| `sdlc/artifacts/build/sprints/sprint-019/execution-plan.json` | A; clean | yes | sprint bookkeeping | plan D4.2 |
| `sdlc/.skill-delegation-audit.log` | M vs start; clean vs HEAD | yes (` M`) | framework bookkeeping | plan D4.2 |
| `sdlc/.skill-context.json` | `??` | yes | framework bookkeeping, subtracted as in the I0 list | IC-4 subtraction |
| `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-019-…md` | M (+2 lines: a blank line and the paragraph at line 134, under `## Traceability` at line 130; frontmatter unchanged) | no | at-acceptance reference edit | plan D4.6 (story line 57) |
| `sdlc/artifacts/requirements/modernization/EPIC-MODERNIZATION-006-…md` | M (+1 bullet at line 163, under `### Traceability` at line 157) | no | at-acceptance reference edit | plan D4.6 |
| `app/build/`, `build/`, `k9mail-vendored/build/`, `.gradle/`, `local.properties`, `.idea/` | ignored | yes / pre-existing | the real tree's own Gradle outputs from the I0/I4 gate runs | IC-2 requires that run. These are not outputs of an upstream experiment |
| `.github/modernize/`, `sdlc/.engine/`, `sdlc/logs/`, `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/*` | ignored | pre-existing (dated 2026-05 / 2026-06 or earlier sprints) | outside this story | not in the IC-4 scope |

No other path exists. There is no `.gitmodules`, no clone, no scratch Gradle project and no `SniProbe.java` in the tree. No new `REQ-*`, `DES-*`, `CNTR-*`, `CR-*` or `U-*` file exists other than U-062's own. The product-path diff is empty against the start commit and against HEAD.

**At-acceptance edits compared word for word with Record 3 (A:2151-2162).** Both files received the same text: "The Part A feasibility analysis was produced by story U-062 at `<path>`. The product owner accepted it on 2026-11-17 (decision Record 3, "Product owner decision" appendix), and the recorded direction is to proceed with the migration under the analysis's conditions (`go-with-conditions`)." That text matches what Record 3 contains: the date, Record 3, the decision `accepted`, and the direction "Go: the full migration, under the 17 conditions … This agrees with the analysis's `go-with-conditions` line". It says no more than the record. It omits that PO-1 to PO-13 and the P items stay open, which is an omission, not an addition. `change_records`, `status` and `updated_at` are unchanged in both files.

## Deviations judged

| # | Deviation | Effect on criteria | Judgement |
|---|---|---|---|
| D-1 | The orchestrator inserted one blank line in Q12.c (code-review N1) itself, not through the implementer (L:810) | AC-14 (document order and content of Section 12) | Not unmet. The blank line is present. The line positions agree with exactly one inserted line (the Recommendation line moved from 1435 to 1436). Predicates, row, conditions and Recommendation text are unchanged. QA cannot prove byte-for-byte that nothing else changed, because no post-IR2, pre-fix copy exists. Recorded as MET WITH DEVIATION on AC-14. |
| D-2 | IR2's web-open covered only the 128 new rows. The 454 earlier rows were opened in I4 and IR1 (A:2086, A:2104, A:2118). | AC-2 | Not unmet. The pre-IR2 rows point at immutable SHAs or versioned artifacts. QA resolved all 389 GitHub rows locally and re-opened 15 non-GitHub URLs; all matched the recorded status. The IR2 rows carry `Retrieved 2026-11-13` while the IR2 web-open ran on 2026-11-17 (`<scratch>/u062/ir2w/c*.tsv`, 128 OK). The date is the clone-read date: minor, noted. |
| D-3 | IR2 was interrupted on 2026-11-13 by the endpoint security product and resumed 2026-11-17 (L:801, L:806) | none | Not unmet. The end checks of IR2 (L:784-797) are dated after the resume. QA's own re-run of the citation mechanics, the structure counts and IC-1/3/4 shows the state is consistent. |
| D-4 (D-d) | Code review W1: "10 source files of this repository's own changes" in Q9.f is 19 for A0-A3 when the import-only files are counted | AC-11 | Not unmet. AC-11 requires counted costs for the three options of Q9.a (A:923-925), and those counts are right. The error is in the supplementary R-A route (Q9.f, Q11.e, the Q12.c extension, PO-14). The product owner was told the figure 19 before deciding (A:2156). R-A was not chosen (A:2159). Recorded as MET WITH DEVIATION on AC-11, with the miscount carried as an open defect. |
| D-5 | Security review Medium N-4 and Low L-A1 to L-A7 on Q9.f, and N-1 to N-3 from IR1, all deferred | AC-9, AC-11, AC-14 | Not unmet. N-4 and L-A1 to L-A7 concern routes R-A and R-B, which Record 3 did not choose. N-1 to N-3 concern the precision of Q7.h and of the tests in conditions 12 and 17. AC-14 requires each condition to have a stated test, and each does (A:1360, A:1365). Whether the test is strong enough is a Part B design matter. These must be carried into Part B planning. |
| D-6 | Upstream was not built and the app was not compiled against it (plan D2; A:738, A:2058-2062) | AC-7, AC-10 | Not unmet. AC-10 allows an estimate labelled as such, and every delta is labelled. AC-7 allows a code citation for "yes"/"via shim", and the via-shim cell says no shim was compiled (A:429). The story's Estimation Guidance expected small builds, but that is guidance, not a criterion. The product owner was told before acceptance (A:2156). Condition 2 (A:1350) makes the build the first Part B step. |
| D-a | AC-2: QA did not itself open every citation of Sections 1, 3, 4, 6 and 7, as the role table at S:147 asks. The first-pass reason was a 15-open cap. The coordinator then lifted it for the 12 rows that rested only on the implementer's opens. | AC-2 | The counting rule that AC-2 itself sets (S:79) was fully run by QA. After the second pass, of 271 IDs, 143 were opened or read by QA (117 git objects at the cited SHA, 26 web opens), and 128 were checked against local copies of the cited artifacts or captures in the implementer's scratch folder, not re-fetched. No ID rests only on the implementer's record. No checked citation failed. It stays MET WITH DEVIATION, not MET: S:147 asks QA to open every citation, and those 128 were content-checked against copies rather than opened by QA. They are versioned Maven Central and Google Maven artifacts (immutable once published) plus 7 captures of registry or API responses (C-019, C-020, C-024, C-216 to C-219). Closing this fully would need 128 further opens, which were not authorised. |
| D-b | AC-15 evidence is mostly uncommitted. The `git log --diff-filter=A` check can only show that nothing Part-B-like was committed (true; the one commit after the start is dated 2026-11-13 and holds Part A files only). The recorder is named by role ("Orchestrator"), not by a personal name. | AC-15 | Not unmet. The working tree confirms that no Part B artifact exists at all. The records exist with date, decision, verbatim words and recorder role. No agent accepted. The orchestrator recorded the product owner's selection. MET WITH DEVIATION, because the prescribed git-log check is weak while the work is uncommitted. |
| D-c | IC-2 gates not re-run by QA | IC-2 | Sufficient under the story's own text. IC-2 asks that the gates pass "at the end" of the implementation. They did at I4 (recorded and surviving outputs read by QA). Since then no product, build or test path changed (IC-1 empty against start and HEAD). Plan Acceptance Sequencing step 5 re-runs gates only if a tracked non-`sdlc/` file could have changed. Recorded as MET on recorded evidence. |
| D-e | IC-4's literal list omits the pipeline workspace files and the at-acceptance edits. The experiment (f) jar-download command was not recorded (A:2072). | IC-4 | Not unmet. The extra paths are the D4.2 bookkeeping class, which IC-1 allows as "workflow bookkeeping the pipeline itself writes", plus the D4.6 edits that story line 57 requires. Both are plan interpretations of a story-text inconsistency. The missing download command is disclosed, the jars were only listed or parsed, and nothing from them was executed. MET WITH DEVIATION. |

## Behavioral Contract Verification

| Contract | Expected | Actual | Result |
|---|---|---|---|
| CNTR-MODERNIZATION-007 and -001 unchanged (story "Existing Behavior to Preserve") | no edit | `git diff --stat c8f04558 -- sdlc/artifacts/design` is empty (included in the name-status run: no design path listed) | PASS |
| The product behaves as before | no product file changed | IC-1 empty; IC-2 recorded 682/2 | PASS |
| The analysis states contract impacts as P items routed to change records, not decided | P-1..P-5 with clause and CR route | A:641-643, A:1332-1336, conditions 6-10 (A:1354-1358) | PASS |

## Integration Path Verification

No component is created by this story (S:97). The only "integration" is document wiring:
- The analysis is referenced from REQ-019 and EPIC-006 (D4.6 edits, verified above).
- The story Implementation Notes point to the analysis and the log (S:205).
- The log's last `NEXT:` (L:825) points to this QA run.

## Requirement Traceability

| Referenced doc | Scope in doc | Scope implemented | Gap |
|---|---|---|---|
| REQ-MODERNIZATION-019 Part A, questions 1-12 and AC-A1 to AC-A9 | 12 questions, cited facts, licence table, platform table, 7-operation API table, 4 protections, recommendation with separability, PO acceptance, no-go records | Sections 1-12 (A:99-1436), Citations, Experiment log, PO decision (Record 3 accepted). AC-A9 not applicable (`go-with-conditions`). | None for Part A. The open items for Part B are listed below. |
| EPIC-MODERNIZATION-006 (requirement 5; success criteria 5, 6, 9) | the gate before the migration | Q12.c shows criterion 5 cannot be met as written (the four libraries stay; condition 11, PO-10). Criterion 9 holds only through the fork rows or recorded behaviour changes. | Not a story gap. Recorded as PO-10 and conditions 4 and 11 for Part B. |
| DES-MODERNIZATION-009 | context (partly stale) | Q12.f item 2 (A:1370) routes the stale contract id and counts to the Librarian | None |
| CNTR-MODERNIZATION-007 / -001 | context, unchanged | measured at the pin (Q5.p, Q7.b), text drift listed in Q12.f item 1 | None |

## Edge cases checked

- "AC-16 not applicable" appears twice (`grep -c` = 2): once inside prose and once as the line `AC-16 not applicable.` at A:1434. Exactly one standalone line precedes the Recommendation line.
- No numbered level-3 heading exists (`grep -nE '^### [0-9]'` = 0), so no extra "numbered section" reading is possible.
- I checked that IC-3 is not defeated by its own record: the analysis describes the pattern files and never quotes them (A:2080). A whole-story scan reports only S:101.
- Records 1 and 2 were not rewritten after they were made: they are identical to the `analysis.before-IR1.md` and `analysis.before-IR2.md` snapshots in `<scratch>`.

## What was not verified

- 128 citations in Sections 1/3/4/6/7 were checked against local copies or captures of the cited artifacts, not re-fetched by QA (D-a). The 12 that had rested only on the implementer's opens were opened by QA on 2026-11-17 and are verified.
- The R-A/R-B quantities that the code reviewer also did not re-derive: the `--shortstat` sizes, the hunk-overlap counts and the 42-snapshot scan.
- The Section 8 estimates themselves (formula inputs). I checked only their labelling and the baseline.
- The gates were not re-run by QA (D-c).
- Whether only one blank line changed in the orchestrator's N1 fix (D-1). No pre-fix copy exists.

## Issues Found

1. **C-556 line range runs one line past the end of the file** (`#L9-L30`; the file has 29 lines). The claim is correct. A cosmetic citation defect.
2. **W1 miscount** in Q9.f, Q11.e, the Q12.c extension and PO-14: "10 source files" should be 19 for A0-A3. It is open and was disclosed to the product owner. Fix it if R-A is ever reconsidered.
3. **Story Implementation Notes are stale after IR2.** They do not record Record 3 or the D4.6 edits, and S:257 says the appendix holds the placeholder. Review Findings (S:259-260) is empty.
4. **Retrieved dates of C-455 to C-582** show the clone-read date (2026-11-13), not the web-open date (2026-11-17).
5. **The fact supplied to QA about HEAD was inaccurate.** `1e65fa7e` holds the I0 skeleton, not the I0 to I4 state. The analysis, all three records and both at-acceptance edits are uncommitted.
6. **Code reviewer process slip** (review-code.md, IR2 "Process disclosure"). One of its commands may have triggered lazy blob fetches from the network in the scratch clone. The reviewer killed the process. No effect on the deliverable.
7. **Open for Part B** (not U-062 defects):
   - PO-1 to PO-13, including PO-11 (Option 1 or 2), and P-1 to P-5 stay undecided (A:2159).
   - Security N-1 to N-4 and L-A1 to L-A7 and code-review N2 to N5 are deferred.
   - The certificate blocklist decision and the missing hostname-mismatch and TLS-path tests (Q12.f items 8 and 9) apply to the app today, whatever the migration does.

## Recommendations for the retro

1. Align the story's QA citation duty (open every citation of five sections, 271 here) with the QA web-open budget, or name local clone reads and local artifact copies as acceptable "opens".
2. AC-15's `git log --diff-filter=A` check is weak while work stays uncommitted. Commit at acceptance (path-limited, on request), or rephrase the check as a working-tree check.
3. Story template defects seen again: the Affected Code story path, the IC-4 list omitting pipeline files, line 57 vs IC-1/IC-4, and the source line count (291 vs 290). Fix them in the story template, not per story.
4. Fix-in-cycle edits by the orchestrator: allow whitespace-only fixes explicitly and record a before/after hash, or route them to the implementer.
5. Keep "verbatim words" as the product owner's own text where possible. "Accept: full migration" is an option label the orchestrator wrote.
6. Scratch clones used by reviewers should have lazy fetching disabled by configuration, not only by environment variable.
7. Refresh the story's Implementation Notes after each decision record, so the story matches the analysis at QA time.

## Phase Completion Report
---
story_id: "U-062"
phase: "qa-validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-019/U-062/qa-results.md"
story_status: "review"
current_build_phase: "quality-gate"
ac_total: 22
ac_passed: 22
ac_failed: 0
errors: []
---
