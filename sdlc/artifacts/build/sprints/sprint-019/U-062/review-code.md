---
artifact_type: review-code
story_id: "U-062"
iteration: 3
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-11-13"
blockers: 0
warnings: 2
---

# Code Review: U-062

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS. Twelve sections with the exact titles, three appendices in order, one `Recommendation:` line, "AC-16 not applicable." before it. No product, build, contract or requirement file changed. |
| Test coverage | Not applicable (no code). Gate claims (682 run / 2 skipped, release APK 1995017 bytes, 23904 method ids) are internally consistent between Baseline, Experiment (e) and the story Implementation Notes; I did not run Gradle, as instructed. |
| Code quality (document quality) | Good. Arithmetic recomputes; the decisive Section 5 and Section 7 verdicts match the pinned upstream source. One omitted behavioural difference (read timeout), one counting-rule inconsistency, and a recommendation whose tie-break cannot discriminate. None changes a predicate or the selected row. |

## Verdict: PASS

No finding changes a predicate value, the selected row (T2), or a decisive Section 5 / Section 7 verdict. Three warnings and eight observations follow.

## Scope and method

Files read in full: the story, plan.md (Recommendation Logic, What Each Reviewer Checks), the analysis (all 1,605 lines), the Implementation Notes, and the log's closing entries. I opened the cited lines in this repository and in the read-only upstream clones (`<scratch>/tb` at the thunderbird-android pin, `<scratch>/tmc` for the components repo). Nothing in the clones was built, executed or modified. No text addressed to an AI agent was found in any upstream file I read. Nothing was fetched from the network.

## Findings

### Blockers

None.

### Warnings

- **W1. Section 12, Q12.b box, Q12.c "Trade-off" and the Implementer's note: the tie-break that selects `go-with-conditions` cannot discriminate, and the document never states the substantive reason for the value.** The rule is "choose `go-with-conditions` if every F-setting row and every P item has a numbered condition with a test, else `no-go`". The same author writes the conditions, so the rule is satisfied by construction for any set of findings; it cannot produce `no-go` unless the author declines to write a condition. The document labels the rule correctly as the implementer's own (box at top of Q12.b, the paragraph after the T1-T4 table, and the Implementer's note all say so, and AC-14's "not presupposed" requirement is met). What is missing is the substantive "why": AC-14 asks the analysis to explain why it recommends `go-with-conditions` despite predicate F. Q12.c's trade-off ends with "Why `go-with-conditions` rather than `no-go` is Q12.b's rule", which is procedural, and note item (4) says the author "did not use" the product-owner statements to select the value. So the reader gets reservations (1), (2), (3) that point toward `no-go`, and a procedure that points the other way. The presentation is fair to the product owner in that both outcomes and all reservations are on the page; it is thin as an explanation. Plan check: the plan says the option comparison "picks the value among the permitted ones"; Q12.c compares but explicitly does not pick. This is a documented deviation (log deviation 18), not a violation of a plan rule that has a criterion. — Classification: defer-justified — adding a substantive rationale or changing the value is a content change outside the four-type allowlist and is the product owner's call at AC-15.

- **W2. Section 5 (Q5.a rows 1 and 2, Q5.p) and Section 9 Q9.a: a behavioural difference is missing, the read timeout, and the "2 to 3 patched files" count is therefore possibly 3 to 4.** The vendored fork changed `SOCKET_READ_TIMEOUT` from 60000 to 120000 ms (fork commit `013948a758` "Increae read timeout", listed in Q1.e; current value `k9mail-vendored/src/main/java/com/fsck/k9/mail/store/RemoteStore.java:21`). At the pin the value is 60000 (`mail/common/.../NetworkTimeouts.kt:5`), it reaches `RealImapConnection` only as a constructor default (`RealImapConnection.kt:53`), and `RealImapStore.createImapConnection` (`RealImapStore.kt:303-309`) does not pass a timeout, so `ImapStore.create(...)` offers no way to set it; `RealImapConnection` is `internal`. Q9.a(c) dismisses this commit with six others as "not carried or assessed here (publishing, retry and timeout, ...)", by subject line only. The effect is a halved read timeout on APPEND and FETCH of MMS with large attachments over a mobile link, with no row in Section 5's "behavioural difference" column, no P item and no condition. The other half of that commit family, "Retry socket connections", is covered at the pin (`RealImapConnection.kt:154-166` loops over all addresses), so only the timeout is a real gap. This does not change F (already true through rows 3 and 4) or the selected row. It does mean the cost counts in Q9.a, Q11.a and Q12.c understate the fork or behaviour-change surface by one item, and condition 14's live run would only catch it by accident. — Classification: defer-justified — adding a Section 5 difference and a condition is a finding change, not an allowlist-type edit; it can be absorbed in Part B row 2 (consumption proof) if the product owner proceeds.

- **W3. Section 8 Q8.e versus Q8.c item 5: verification-entry counts include components that REQ-MODERNIZATION-015 and -016 would add anyway, while the size estimates exclude exactly those components.** Q8.c item 5 excludes kotlin-stdlib, kotlinx-coroutines-core, androidx.annotation and JetBrains annotations from the byte and method deltas because "their version changes belong to REQ-MODERNIZATION-015 and -016". Q8.e then counts as new migration entries `kotlin-bom 2.4.21`, `kotlin-stdlib` 2.4.21 and its jdk7/jdk8 variants, `annotations` 26.1.0, `androidx.annotation` and `annotation-jvm` 1.11.0, and `kotlinx-coroutines-bom/core/core-jvm` 1.11.0 (the "n" rows of the Q8.e table; I confirmed these are absent from `gradle/verification-metadata.xml` at the start commit, 803 components and 1,460 artifact entries as stated). Condition 1 makes REQ-015/016 precede Part B, so when Part B runs those entries are already present. The figures 80 / 112 / 208 to 256 are correct as "against the start commit" and I recomputed their internal sums (24+34+54 = 112; 18+21+41 = 80; 112 + 96..144 = 208..256; 112 - 80 = 32 for the four omitted families), but the marginal cost is overstated by roughly ten components, and the document presents the counts as "counted", not "estimate". — Classification: defer-justified — changing the counts or adding a "net of REQ-015/016" column alters stated numbers, which the typo allowlist excludes; the ranges already carry condition 2's "measure first" caveat.

### Observations

- **O1. Q5.a row 5 "via shim" is argued from the `suspend` signature, not compiled.** AC-7 allows a code citation, and the plan's D2 decision records why no build was run. The claim is consistent with upstream (`ImapFolder.kt:47` `suspend fun fetch`, `RealImapFolder.kt:572`). A `runBlocking` wrapper called from Java is plausible but unproven in this app (thread, cancellation and the listener callback also being `suspend`). Condition 15 and Part B row 6 carry it forward. — Classification: defer-justified — a compile proof requires the build experiment the plan deliberately skipped.

- **O2. Q11.d row 10: "`:k9mail-vendored` (116 tracked files)".** `git ls-files k9mail-vendored` returns 121 tracked files; 116 is the count of tracked `.java` files (the other five are `.gitignore`, `LICENSE`, `NOTICE`, `build.gradle`, `AndroidManifest.xml`). The same section's other file and line counts check out (14 and 23 files, 62 import lines in 16 files, 31 distinct names, 17 test files with 49 import lines, 26 tracked `mail` files with 3,528 lines, 102 compiled vendored files). — Classification: defer-justified — the fix changes a number, which the doc/prose typo type excludes.

- **O3. Experiment log entry (f) does not record how the 27 jars (and sources jars) used for the class, method and licence-header counts were obtained.** The entry says the `dependencies` task "resolves metadata only" and sizes come from the registries; the implementation log (I1b, "jars downloaded to `<scratch>/jars/`, listed only, never run") and Q8.b ("bytes parsed; nothing executed") show jars were downloaded and parsed. Story experiment rule 4 asks for commands in the log. The handling described is within rule 3 (nothing executed). — Classification: defer-justified — adding the missing command is new recorded evidence rather than a typo or dangling-reference fix.

- **O4. Condition tests vary in precision.** Conditions 3 to 5, 7, 11 and 12 name a command or a checkable result. Conditions 6 to 10 use "CR-nnn approved", which the plan prescribes for P items. Condition 1's test ("`git grep -n` for the words kotlin and coroutines ... both requirement stories read `done`") names no exact command or expected value; condition 13 reads a pin by grep, which is a fine test but depends on the text still being there; condition 14's test is the presence of attached evidence. All 15 are numbered, owned, and have an observable result, so AC-14's literal rule holds. — Classification: defer-justified — tightening them is Part B planning work and the plan accepts this form.

- **O5. No-go branch is less prepared than the go-with-conditions branch.** AC-16 records are rightly not drafted (outcome is not `no-go`), and T2 permits `no-go`. For the product owner to choose `no-go` at AC-15 the supply-chain mitigation facts (authoritative copy, what detects a lost upstream, what recreates it) are only partly on the page: Q9.a Option 3 says the authoritative copy is this repository and the origin fork is a single third-party fork; nothing says what detects its loss. This asymmetry is mild because AC-16 assigns the records to the Requirements Analyst after acceptance. — Classification: defer-justified — AC-16 places these records after acceptance by design.

- **O6. Section 10 verdicts rest on absence of evidence and are disclosed as such.** "Separable" for RCS and Google sign-in is supported by greps (0 lines in the mail modules and the app's mail path) and by the structural fact that acquisition is app code. Q10.a item 4 and Q10.b item 4 state that Android's RCS access and Google's current requirements were not verified from primary sources. AC-12 is met as written; predicate S does not select a row. — Classification: defer-justified — the open items are outside the mail-library question and are disclosed in the text.

- **O7. Section 7's "supported" status for hostname verification and SNI means the app keeps K-9-derived TLS code in tree.** Q7.f and Q11.a count at least 458 lines (`DefaultTrustedSocketFactory.java` 267 + `TrustManagerFactory.java` 191, plus ten tests) moving into app-owned code. This is consistent with the plan's definition of "supported" (app-owned code, including code moved out of the vendored module) and does not set B or F. It is relevant to EPIC success criterion 5 and to reservation (1) of the Implementer's note, and is correctly stated in Q11.a and Q12.c. — Classification: defer-justified — informational; the document already states it.

- **O8. Cosmetic: sub-section labels in Section 5 run Q5.a, Q5.b, Q5.c, Q5.p, Q5.f; the Baseline reports JDK `25.0.5` while experiment (g) and the I4 re-run record `25.0.5.0.3`.** The second is disclosed in the I4 paragraph of Experiment (e). — Classification: defer-justified — labels are plan-assigned and the JDK difference is already explained in the document.

## Recommendation logic, re-derived

| Predicate | My derivation | Matches doc? |
|-----------|---------------|--------------|
| B | Section 7 Q7.a has four rows, all "supported"; 0 blockers. The "supported" statuses are consistent with the plan's definitions and with upstream: the mail modules do not verify TLS themselves (`TrustManagerFactory.java:20,75` only), the factory is supplied by the caller (`TrustedSocketFactory.kt:16`), and the upstream SNI code has no blank-host guard (`DefaultTrustedSocketFactory.kt:57-58,131-137`). | yes, false |
| F | Section 5 rows 3 (APPEND, format string at `RealImapFolder.kt:1038-1044` has no date-time) and 4 (no public criteria; `UidSearchCommandBuilder.java:51-53` emits only TEXT or the five-field OR; the one HEADER search is `MESSAGE-ID`, `RealImapFolder.kt:1118`) are "needs fork". Section 3: 33 + 72 rows, all "yes" (POM-only counts 21 and 72 recomputed, so 93). | yes, true |
| G | Section 4: conflicts only for minSdk and compileSdk in mode M2; M1 and M3 start without resolving either. Upstream values verified (`ProjectConfig.kt` sdkMin 24, sdkCompile 37, JVM 17; `.java-version` 21; wrapper 9.7.0 against catalogue 9.6.1). | yes, false (plan interpretation labelled) |
| S | Both goals "separable", no inseparable row. | yes, false |
| P | P-1, P-2 (Section 5) and P-3, P-4, P-5 (Section 7); the upstream facts behind P-2 hold (`AuthType.kt` has no status type; the challenge parser is reduced to a boolean at `RealImapConnection.kt:431`; `TrustedSocketFactory.kt` imports the moved `MessagingException`). | yes, true |

Row T2 (not B, F) is selected, and it is robust: B false is the only predicate that could change the row (to T1, whose permitted values are the same two), and G and S do not select. Plain `go` is excluded on F. The mechanical part is correct.

## Is the evidence for `go-with-conditions` at least as strong as for `no-go`?

In my reading, no. The evidence supports `no-go` at least as well as `go-with-conditions`, and on engineering merit alone I would weight it somewhat toward `no-go`. What the document establishes: every route to upstream mail code is source-only; two behaviours the app depends on (APPEND internal date, header search) are absent and need patched upstream files or a mailbox-visible behaviour change; there is a further, unlisted difference (W2); XOAUTH2 failure status is lost (P-2); upstream calls the mail modules legacy and "being gradually replaced"; the work is a ten-row epic with five contract-change items and fifteen conditions; the APK grows by an estimated 8% to 20%; EPIC success criterion 5 cannot be met as written; and the app still owns patches to a copy or fork, so the supply-chain position is no better than today's. What favours migrating is currency (mime4j 0.8.15 and commons-io 2.22.0, which the vendored module could take as version bumps as it did in U-058; the certificate blocklist, which could be ported; OAUTHBEARER negotiation) and the product owner's stated wish that the migration be in scope and that the app run on the latest of everything. That wish is a legitimate reason for `go-with-conditions`, but the document says it did not use it. The recommendation is therefore defensible as a product-owner-preference outcome with honest costs and conditions, not as one the evidence selects. This is an opinion for the product owner; it does not affect the verdict.

## Spot-check record

Repository (this checkout), 30 items opened, all agree with the analysis: the 14-file recount and the 23-file total; 62 import lines (61 non-static), 16 files, 31 distinct names; per-file import counts for 11 files (8, 5, 5, 5, 2, 1, 1, 1, 11, 2, 2); 17 test files with 49 import lines; 13 `X-smssync-*` constants plus `References` and `Message-ID` at `Headers.java:25-26`; the three `Item.DATE` sites (`BackupImapStore.java:181`, `K9MailTransport.java:258`, `:731`); the six "Unable to get IMAP prefix" sites (`K9MailTransport.java:164,181,212,238,266,395`); `XOAuth2FailedException.status == 400` at `BackupWorker.kt:442` and `RestoreWorker.kt:655`; `importMessageBody` catch list (`K9MailTransport.java:307-316`: MessagingException, IOException, IllegalArgumentException); the pinned factory (`PinnedCertificateSocketFactory.java:55,75-76,126`); `buildSocketFactory` (`K9MailTransport.java:132-148`); `buildSearchQuery` (`:749-758`); the vendored APPEND with date (`ImapFolder.java:1174-1177`), ENVELOPE and DATE fetch lists (`:685-689`, `:708-709`); `HeaderGenerator.java:55-56`; the SNI test (10 `@Test`, method lines 48, 70, 98, 133, 151, 171, 187, 202, 216, 231, import at line 14); vendored SNI guard lines 216-255 (`:217`, `:221`, `:225`); `Sanitizer.java:3`, `Attachment.java:13`, `MessageConverter.java:40`; `app/build.gradle:131`; `k9mail-vendored/build.gradle:100-114` and the NOTICE lines 19-21 and 25-27; REQ-015 lines 34, 52-54, 84-86, 113, 157, 189; REQ-016 lines 35-36, 62; REQ-019 lines 84, 104, 106, 108; EPIC-006 lines 92-97, 107-109; `gradle/verification-metadata.xml` (803 components, 1,460 artifacts, three of the coordinates present); the `mail` package (26 files, 3,528 lines) and its test directory (22 files); the sprint story counts behind the Q11.a comparison; `app/build.gradle:366-373`.

Upstream (pin `ac1f0e85...`), 29 items read with `git show` / `git grep`: APPEND format string; UID SEARCH builder content; `search` plumbing; ENVELOPE header list (16 names, contains `date`); `suspend fun fetch` in `ImapFolder.kt:47` and `RealImapFolder.kt:572`; `TrustedSocketFactory` signature and its moved `MessagingException`; `ImapStore.create` (companion, third parameter the factory); `checkSettings` rethrow; `AuthType` (PLAIN, CRAM_MD5, EXTERNAL, XOAUTH2, NONE; no OAUTHBEARER constant); OAUTHBEARER-before-XOAUTH2 selection (`RealImapConnection.kt:313-323`); challenge parser reduced to a retry boolean; absence of any XOAUTH2 failure type; `FetchProfile.Item` without `DATE`; absence of `setRecipient`; `AddressHeaderBuilder` `require(addresses.isNotEmpty())`; `DefaultTrustedSocketFactory.kt` (SNI guard, no try/catch, `SSLCertificateSocketFactory` branch); `RealImapStore` and `RealImapFolder` `internal`; `K9MailLib.setDebugStatus`; `Log` `@Discouraged` object with `lateinit var logger`; `DefaultHostnameVerifier` import; `Flag` in `core/common`; `NetworkTimeouts` 30000 / 60000; the "Legacy Modules" statements (`README.md:43`, `module-organization.md:207-214`, `legacy-module-integration.md:358`); `ProjectConfig.kt` (sdkMin 24, sdkCompile 37, JVM 17); `thunderbird.library.kmp.gradle.kts` (android target plus jvm target); `.java-version` 21; wrapper 9.7.0 and catalogue 9.6.1. All agree with the citations' claims, except the omission in W2.

## Arithmetic recomputed

All of these agree with the document: Q8.b sums (299 classes, 1,608 methods, 125,158 code bytes, 809 / 7,366 original, 1,651,468 jar bytes, retention percentages, 870 third-party methods, 58,788 code bytes); removal (-1,810 method ids = 1,608 + 202; 85,551 to 127,208 bytes); `k_b` 0.0445 to 0.0575; version-change bytes 114,938; added jar bytes M1 5,122,239 and M3 3,635,490 (M1 less 1,486,749 for the four omitted families); added jar classes 2,909 and 2,081; M1 byte range 237,000 to 407,000 and M3 158,000 to 237,000; method-id ranges -1,060 to +5,370 (M1) and -1,280 to +2,930 (M3); class ranges; 27 jar rows; 33 + 72 licence rows; 13 + 2 = 15 headers; 9 + 5 port rows with verdict tallies (2 + 6 + 1 and 2 + 2 + 1); 7 items with conditions (2 + 5); 15 conditions, 12 PO items, 5 P items; the 11 fork-only commits split 2 + 1 + 1 + 7; breakdown acyclic with longest chain 6; condition-to-row and P-to-row mappings (conditions 6 to 10 against Q11.d rows 5, 6, 8, 10) agree.

## Citation mechanics (AC-2)

Re-run by me over the Citations table: 432 rows, C-001 to C-432 contiguous, 432 distinct IDs used in the text outside the table, none unused, none unresolved, no empty column, retrieval dates all `YYYY-MM-DD`, GitHub blob permalinks all pinned to a 40-character hex SHA, no `/blob/main` or branch permalink, statuses within the vocabulary (opened 421, read 5, grep-confirmed 6), no citation targets the research input. A line-by-line read of Sections 5 (Q5.p), 6 and 12 found upstream facts carrying IDs; the uncited statements are cross-references to earlier cited sections. Hygiene (IC-3): no e-mail address, host name, user name or profile path in the analysis, the input copy, the implementation log or the Implementation Notes (the story's own IC-3 text on line 101 is author text). IC-5: the copy's first line is the banner, the copy minus line 1 hashes to the recorded source value, the whole copy to the recorded copy value. IC-1: the product-path diff against the start commit is empty. IC-4: `git diff --name-status <start>` lists 11 paths, all permitted or bookkeeping; no `.gitmodules`; no REQ, DES, CNTR, CR or other U file added.

## Experiment log (rules 1 to 6)

Experiments (a), (b), (e), (f), (g) show placeholders (`<scratch>`, `<repo>`, `<sdk>`), exit codes and results; (c) and (d) are "not run" with a stated reason and no attempts, which AC-1's "unknown" exception does not need because no section answer is "unknown". The git clone commands clear credentials (`env -u GH_TOKEN`, `credential.helper` blank). (f) used the installed Gradle with a separate home and no upstream build logic. (g) is a ten-line JDK class probe containing no upstream code and its results (`IllegalArgumentException` for `::1`, `fe80::1`, empty, blank) are consistent with the Q7.c claims. No proof-of-concept source is quoted in the analysis (8 code fences, all command lines), so rule 6 has nothing to review beyond the commands. See O3 for the one recording gap.

## Patterns Verified
- [x] Follows the plan's skeleton and table shapes (AC-1, AC-3 to AC-14, IC-6)
- [x] Error handling: not applicable; unknowns are stated as "not measured" with the reason, never left blank
- [x] Tests: not applicable (no code); the 10-test SNI mapping is complete and honest about the `fe80::1` mislabel
- [x] No hardcoded values: not applicable
- [x] No unnecessary complexity: the document is long but each section answers its question; the P-item and PO-item lists reconcile

## Integration Verified
- [x] No code introduced; no entry point to trace
- [x] No registries or dispatch maps touched
- [x] No function signatures changed
- [x] No dead code introduced
- [x] The analysis's own cross-references (section numbers, P-n, PO-n, condition numbers, citation IDs, Q11.d row numbers) resolve to what the referring text claims

## Regression Check
Not a replacement or rewrite story. No Capabilities Inventory is required. The analysis's own capability comparison (Q12.c "Capabilities closed / opened", Q5.a, Q5.p) is the content under review; W2 is the one capability difference I found missing.

## Contract Verification
- [x] CNTR-MODERNIZATION-007 and CNTR-MODERNIZATION-001 are read, not changed (both files unmodified per `git diff`).
- [x] The 9 + 5 port and exception-mapping rows agree with Section 5's operation rows and with P-1 / P-2; CNTR-001 measurement rows agree with P-3 / P-4 / P-5.
- [x] Pre-existing contract-text drift is reported, not fixed, and routed to Q12.f.

## Runtime Claims Verified
- [x] The story declares no Runtime Claims inventory and makes no runtime-behaviour acceptance criterion; the log's inventory is empty, which is the valid state for a documentation-only story.
- [x] No embedded probe exists, so no probe design needs scrutiny. The two runs the analysis relies on (the I0 and I4 gate runs; the `SNIHostName` probe) are recorded with commands, exit codes and results.
- [x] No escape-hatch justification is used.
- [x] No probe wrote into `../amp`; none touched the product tree.
- [x] UNVERIFIED (escape-hatch) claims: None. Statements the analysis itself marks as not measured (upstream build on JDK 25, minimum Android API of upstream's third-party libraries, size figures, live Gmail mechanism) are disclosed as not measured, not as verified.

## Phase Completion Report
---
story_id: "U-062"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-019/U-062/review-code.md"
story_status: "in-progress"
current_build_phase: "review"
blockers: 0
warnings: 3
errors: []
---

## Delta re-review after IR1 (2026-11-13)

Scope: only the parts changed by rework increment IR1, re-read from disk (analysis now 1,766 lines). Verdict stays PASS. Predicates B, F, G, S, P and row T2 are unchanged and still correctly derived; no decisive Section 5 or Section 7 verdict changed. No Gradle, no network, no state-changing git; the upstream clones were read only.

### 1. W2 (read timeout): resolved, and correctly stated

- Q5.t follows the primary sources link by link. I re-opened each: vendored `RemoteStore.java:21` (120000), `ImapConnection.java:82-89` and `:229`, `ImapStore.java:371-373`; at the pin `NetworkTimeouts.kt:3-6` (30000 / 60000), `RealImapConnection.kt:46-54` (`internal`, timeouts as constructor defaults), `:190-202` (`soTimeout` applied in `configureSocket`, called at `:90` and again at `:300` after the STARTTLS upgrade), `RealImapStore.kt:25` (`internal open`) and `:302-310` (no timeout passed). The claim "no public way to set it" holds.
- Status "needs fork of `NetworkTimeouts.kt`" is supported. The two non-fork alternatives are fairly stated: accept 60 s (a behaviour change, PO-13) and an app-owned delegating socket whose `setSoTimeout` ignores the value. The wrapper is plausible (the library sets `soTimeout` on the socket object the factory returned) and is labelled as not written, compiled or run.
- The read timeout appears in Q5.a row 1 (differences 7 and 8, the "achievable without forking" cell), rows 3 and 5, the closing paragraph (rows 2, 5, 6, 7, 8 "yes"; row 1 carries one fork-dependent setting), the `appendMessages` and `importMessageBody` port notes plus the verdict paragraph, Q12.a (F row), Q12.b, Q12.d (PO-13), condition 16, and Q9.a. A search shows no remaining "2 to 3 patched files" for the whole patched set (the two "2 to 3" hits are the M2 artifact estimate and the rows-only count in Q9.a (b), both correct). Q5.t itself explains that the range is 2 to 4 files, or 2 to 3 if 60 s is accepted or wrapped.
- Condition 16's test is observable (recorded `getSoTimeout()` after connect and after STARTTLS; live run with the largest MMS). One nuance, N3 below.

### 2. The retry disagreement: the implementer is right, I was wrong

- Fork commit `69eef8239b` removes exactly the pin's loop (`getAllByName`, one `connectToAddress` per address, remember the last `IOException`) and replaces it with a sorted list of `ConnectableAddress` objects, `maxTries = 3`, `initialBackoff = 5000` doubling per try, and a `while (elapsed < socketConnectTimeout)` budget, ordered by last attempt.
- The pin's `connect()` (`RealImapConnection.kt:154-168`) is the pre-image: one try per address with the full 30 s connect timeout. I read the pin's loop and treated it as the fork's feature; it is what the fork replaced. Q5.t's correction, and the choice to record it as a behaviour difference (row 1, difference 8, PO-13) rather than a fork row, are right.

### 3. Seven previously dismissed fork commits: classification checked against the commits

| Commit | Analysis class | My reading of the commit and the pin |
|--------|----------------|--------------------------------------|
| `013948a758` read timeout | absent | Agrees (60 to 120 s; pin 60 s, no setter) |
| `69eef8239b` retry | absent | Agrees (see 2) |
| `605b321574` fail early | partly present | Agrees. The commit throws when username or password is null; the pin's `ServerSettings.username` is non-null and `isMissingCredentials` exists, but `RealImapStore` init checks only `type == "imap"`, so a missing password is not refused |
| `eaf689025e` empty last field | present only outside the proposed modules | Agrees. The fix is in the vendored `ImapStoreUriDecoder`; the pin has the same handling in `legacy/storage` migration12 `ImapStoreUriDecoder.java:89-98`, not in the mail modules |
| `ac837a0156` SDK 7 compat | not applicable | Agrees. The diff replaces `Charset.forName`, `String.isEmpty` and similar calls with API-7-safe forms; nothing there matters at minSdk 21 |
| `75bb293984`, `7e6d627a7a` bintray added then removed | not applicable | Agrees (build file only, 20 lines in then out) |

Tally 2 + 1 + 1 + 3 = 7, consistent with the 11-commit split (2 + 1 + 1 + 7) in Q9.a (c). The Q1.e rework note points to Q9.a (c) and Q5.t.

### 4. W3 (Q8.e overlap): arithmetic holds; one claim of the text is not true

Recomputed: 112 - 23 = 89 (jar 24 - 6 = 18, module 34 - 6 = 28, pom 54 - 11 = 43; components 54 - 11 = 43); 80 - 23 = 57 (12, 15, 30; components 41 - 11 = 30); M2 208 - 23 to 256 - 23 = 185 to 233; 6 + 6 + 11 = 23. The named families give ten components (Kotlin bom, stdlib, jdk7, jdk8; coroutines bom, core, core-jvm; androidx.annotation, annotation-jvm; JetBrains annotations); the eleventh is not named in the text (probably the `kotlin-bom` 2.4.20 parent listed under the 12 parent components). The text says the other sections "now refer to this paragraph for the net ones", but Q9.a, Q11.a and Q12.c still quote 80 / 112 / 208 to 256 with no pointer. W3 is resolved in substance (net figures are given, the basis is explained, and the gross figures remain correct "against the start commit"). The missing pointer and the unnamed eleventh component are N1.

### 5. W1 and the no-go preparation

- The sub-section "If the product owner chooses `no-go`" is marked "prepared text, not in effect" and contains what AC-16 requires. (a) Supply-chain decision record with the three parts: authoritative copy (tracked `k9mail-vendored/`, 121 files, 116 `.java`; I confirmed 5 commits touch it and 2 remotes are configured, and the text states the mirror question is open); what detects a lost upstream (three checks, with a command for the first two); what recreates it (repository history, with the honest note that the origin commit alone gives only the capture state). (b) Change-record text for REQ-MODERNIZATION-019 Part B not applicable; I confirmed `change_records: []` at line 16, the Part B heading at `:80`, AC-B1 to AC-B12 at `:102-113`, and AC-A9 at `:98`. (c) Change-record text for EPIC-MODERNIZATION-006 criteria 5 and 9: I opened the epic and compared; the quoted current text of criterion 5 (`:92-97`) and criterion 9 (`:107-109`) is verbatim, and the epic's in-scope bullet (`:58-61`) and requirement 5 (`:119`) cited as related texts exist. Proposed revisions are labelled as proposals. Story AC-16 is met in content, and is correctly not an in-effect record.
- Evenness: both permitted values now have their records, conditions or preparation on the page. The tie-break itself is unchanged, and the box and the Implementer's note still say the value is the implementer's choice and that the product owner decides. The residual of W1 (the tie-break is satisfied by construction, and the substantive reason for `go-with-conditions` is the product owner's stated wish, which the note says was not used) is unchanged, as it should be: the document states it openly. Disposition: partly resolved (preparation and attribution fixed; the rule remains non-discriminating).
- "Reviewers' readings" reports my reading accurately: evidence supports `no-go` at least as well, somewhat toward `no-go` on engineering merit, `go-with-conditions` defensible as a product-owner-preference outcome, and the W1 point. It does not overstate. The one sentence attributed to the security review ("preserve, not improve") is explicitly marked as the analysis's own summary, not a quotation; the quoted fragment "is not better than today's" is correctly identified as a sentence of the Implementer's note that the security review reports; I confirmed the review file contains that report and the M-1 item. One small imprecision: the sub-section says my review "found the document better prepared for one of the two values than the other (W1 and O5)"; W1 concerned the tie-break and the missing substantive reason, O5 the no-go preparation. The phrase fits O5 and part of W1 and is not misleading.

### 6. Consistency after the edits

- Counts: 454 citation rows, C-001 to C-454 contiguous, 454 distinct IDs in the text, none unused or unresolved, no empty cell, no branch or short-SHA permalink, no research-input citation (my own re-run). Q12.d has PO-1 to PO-13; Q12.e has conditions 1 to 17; five P items; "fifteen", "twelve PO", "seven items" and "All seven" no longer appear. The boxed note, the T2 table row, the tie-break paragraph, Q12.c and the Implementer's note all say "eight items" or "seventeen conditions" consistently, and "All eight items" correctly lists the two F rows, the row-1 setting (condition 16) and P-1 to P-5 (conditions 6 to 10).
- Structure: 8 operation rows, 9 port rows, 5 exception rows, 4 protection rows, headings in order (Baseline, 1 to 12, Citations, Experiment log, Product owner decision). `Recommendation: go-with-conditions` appears once, at line 1208, immediately before `## Citations`, preceded by "AC-16 not applicable." The new sub-sections sit before that line, so AC-14's order holds.
- Predicates: F is still true on rows 3 and 4; the row-1 setting adds a third F-setting item and does not change F, B, G or the row.
- Hygiene: no e-mail address, host name, user name or profile path in the analysis or the implementation log (my own scan, same patterns as before). IC-1: the product-path diff against the start commit is still empty. IC-4: no new REQ, DES, CNTR, CR or U file, no change-record file; `git status` shows the analysis, story, log and `sprint-plan.md` modified and the two review files untracked.
- New repository references, all opened and correct: `AuthPreferences.java:81,247-262,273-275,281-290`; `arrays.xml:108-122` (the empty third value); `ImapStoreUriDecoder.java:49-73` (133 lines; `imap` to NONE, `imap+tls` to STARTTLS, `imap+ssl` to SSL_TLS, else exception); `ImapStoreSettings.java` 52 and `UrlEncodingHelper.java` 28 lines (213 total); `ImapStore.java:79-84,92-102`; `K9MailTransport.java:337-339`; `BackupImapStore.java:236-238`; `ImapConnection.java:293-302,336-347` (PLAIN, LOGIN unless LOGINDISABLED); `PinnedCertificateSocketFactory.java:94`; the test greps (0 lines in `app/src/test`; `k9mail-vendored` has no `src/test`; the GreenMail test uses `imap://PLAIN:...` at lines 89 and 144).
- New upstream citations opened, 17 of 22: C-433, 434, 436, 438, 441, 442, 443, 444, 445, 446, 447, 435 (via the pin's `connect()`), 437 (via `RealImapStore.kt`), plus the fork commits C-448 to C-454 (their full SHAs resolve to the cited commits). All say what the rows claim. Q7.h's description of the pin (the migration decoder in `:legacy:storage` depending on `:legacy:core`, `MissingCapabilityException` for missing STARTTLS, a bare `Socket()` unless implicit TLS) matches the code.

### 7. Observations raised earlier

- O1 ("via shim"): resolved. Row 5 now says the answer is argued from the declaration and that no shim was written, compiled or run; the closing paragraph repeats it.
- O2 (116 versus 121 files): resolved. Q11.d row 10 and the no-go record state "121 tracked files: 116 `.java` files and 5 others", which I recounted.
- O3 (jar download record): partly resolved. The Experiment (f) addition states what the evidence shows and what is inferred (only two token variables were removed from the environment; the real Gradle home was not listed before and after), which is an honest repair of the evidence-quality point. I did not find a recorded command for the jar downloads in the experiment (f) entry itself; the implementation log (I1b) still holds that. The remaining gap is small.
- O4 (condition test precision): partly improved (conditions 7, 12, 16, 17 are concrete and observable); condition 1 was not tightened. Not required.
- O5 (no-go preparation): resolved (see 5).
- O6, O7, O8: unchanged and still acceptable as disclosed.

### 8. New findings

No blockers and no new warnings.

- **N1.** Q8.e says the other sections refer to the net verification counts; Q9.a, Q11.a and Q12.c do not, and the eleventh overlap component is unnamed (see 4). — Classification: defer-justified — a cross-reference or a name is a content addition outside the four allowlist types and does not change any finding.
- **N2.** Q5.t says "Every link agrees with the code review" and later corrects the code review on the retry. Both statements are accurate (the timeout links agree; the retry is the correction), but a reader skimming the first sentence could miss the second. — Classification: defer-justified — wording choice with no effect on any finding.
- **N3.** Condition 16 does not say what value a socket wrapper (alternative (c)) must report from `getSoTimeout()`. If the wrapper ignores `setSoTimeout`, the recorded value could be the platform default, not 120000, and the test as worded would not distinguish a working wrapper from a failing one. — Classification: defer-justified — the wrapper was not built, and the condition is Part B test design.
- **N4.** The "Retry socket connections" difference (difference 8 in row 1) is recorded as a behaviour difference only, with no condition or live-run check; the analysis states this and that nothing in the contract or acceptance criteria depends on it. The effect (one 30 s attempt per address instead of staged 5 s, 10 s, 20 s attempts) matters mostly on flaky networks. — Classification: defer-justified — the product owner can add it at PO-13, and the analysis presents it as an option.

### 9. Reading of the evidence

Unchanged. The rework added more cost and risk (a third patched file for the timeout, the connection-security mapping the app would own, a TLS test gap that exists regardless), and none of it favours migrating. My reading remains that the evidence supports `no-go` at least as well as `go-with-conditions`, with a lean toward `no-go` on engineering merit; `go-with-conditions` rests on the product owner's stated wish that the migration be in scope. The document now presents both values with equal preparation and reports that reading without distortion.

### Dispositions

| Item | Disposition |
|------|-------------|
| W1 | Partly resolved (no-go text prepared, readings reported; the rule remains non-discriminating, openly) |
| W2 | Resolved |
| W3 | Resolved in substance (N1 residual) |
| O1 | Resolved |
| O2 | Resolved |
| O3 | Partly resolved |
| O4 | Partly (condition 1 unchanged) |
| O5 | Resolved |
| O6, O7, O8 | Unchanged, acceptable |

## Phase Completion Report (iteration 2)
---
story_id: "U-062"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-019/U-062/review-code.md"
story_status: "review"
current_build_phase: "review"
blockers: 0
warnings: 1
errors: []
---

## Delta re-review after IR2 (2026-11-17)

Scope: only what IR2 changed (Q9.f and its pointer, Q11.e, the Q12.c extension, PO-14, Q12.f items 10 and 11, the `no-go` item 5, citations C-455 to C-582, the IR2 Experiment-log entry, Record 2). The analysis is now 2,148 lines. **Verdict stays PASS.** No predicate, no row, no Section 5 or Section 7 verdict moved.

### Process disclosure (read this first)

Two of my shell commands exceeded the 120 s limit and were moved to the background by the harness: a whole-history `git log` (it completed with no effect) and a loop of `git show` over blob permalinks in the blob-less scratch clone. The second may have made the clone fetch missing blobs from the network, which the ground rules forbid. I killed it (the processes were mine) and ran every later check with lazy fetching switched off and with per-command timeouts. I changed no file other than this one, and I did not follow any upstream text as an instruction. The coordinator may want to treat this as a rule slip on my side. No shell call failed with "bash.exe not found" and nothing was killed by anything other than me.

### Verification record

- **Pairs of protected parts unchanged.** Against the pre-IR2 copy (`analysis.before-IR2.md` in the scratch folder, found): Sections 1 to 8 and 10 are byte-identical; Q12.a, Q12.b (predicates, box, tie-break, row T2), Q12.e (all 17 conditions), the Implementer's note and "Reviewers' readings" are byte-identical. `Recommendation: go-with-conditions` is the single such line (line 1435), the last non-blank line of Section 12, preceded by "AC-16 not applicable." and followed directly by `## Citations`.
- **Counts re-derived by me (all agree with the text):** non-merge mail-path commits ahead of the merge base and behind the pin for A0 to A4: 107 / 625, 133 / 599, 159 / 573, 183 / 549, 239 / 493, each pair summing to 732; 5.703 is 488 behind; C3 `be7849eb5d` is 295 ahead and `55c54555ff` 344; C4 is 107 behind; 138 tags on this line; Java / Kotlin main files at A3 128 / 6, A4 123 / 14, pin 56 / 112, A2 126 / 5 (A0, A1 and the merge base differ by one from the text only because the text also excludes `WebDavTransport.java`; the reading rule does not say so); the candidate-versus-vendored file splits (100 + 2, 99 + 3, 98 + 4, 97 + 5, 95 + 7 equal 102, and each "both + candidate only" equals the candidate's file total); 26 + 12 = 38 commits and 18 marked F in the table (counted); the first-candidate column gives A0 2, A1 3, A4 4 fixes of the ten and 6, 7, 9 of the 18 fix-like; C4 contains 7 and `be7849eb5d` contains 4 of the ten (by date); 47 + 53 + 2 + 15 + 5 + 32 + 4 + 30 = 188 over 12 upstream files, mapping to 5 edited and 2 new vendored files; 3 verification components and 6 entries for the two library bumps; 582 citation rows contiguous, every ID used, none unresolved, no empty cell, no branch or short-SHA URL (new rows: 67 commit pages, 3 tree pages, 58 blob permalinks, as logged); 14 PO items, 17 conditions, 5 P items. Keyword pass: with the three main-source paths alone I get 25 distinct commits (hostname 7) against the text's 26 (hostname 8); the text also searches the later homes of the TLS factory, whose paths it does not list exactly, and the extra commit is the SNI one (row 32) which my run does not reach. All 25 of mine are rows of the table.
- **Not re-derived:** the diff size lines (`--shortstat`), the hunk-overlap counts of the fork patches, the 42-snapshot Kotlin first-appearance scan, and the `grep` results at A2. I take them as logged.
- **Citations sampled (more than 25 across all three kinds).** Commits: C-455, 456, 457, 464, 468, 469, 476, 485, 487, 488, 489, 483 (subject and date match the claim in every case). Blobs, lines read at the cited commit: C-521 (A0 `ImapStore` constructor with `OAuth2TokenProvider`), C-522 (A0 private `search(ImapSearcher, ...)`), C-524 (A0 APPEND `"APPEND %s (%s) {%d}"`), C-531 (A0 `StrictHostnameVerifier`), C-532 (A0 dependencies: mime4j 0.8.1, commons-io 2.4, jzlib, jutf7, support-annotations, Timber), C-536 (A1 query builder: TEXT or OR SUBJECT / FROM), C-546 (A4 constructor takes `ImapStoreSettings` first), C-551 (A4 compileSdk 29, minSdk 21), C-553 (A4 read timeout 60000), C-555 (`class ImapFolder internal constructor`), plus the A1 `StoreConfig.shouldHideHostname()` and the A0 private `Address` constructor. Tree claim C-576: the A0 and A4 trees hold `ImapSearcher.java`, `Folder.java`, `NetworkType.java`, `StoreConfig.java`, `DefaultTrustedSocketFactory.java` and no `XOAuth2AuthenticationFailedException`. Tags: 5.304, 5.501, 5.700, 5.703 point at the stated commits; A2 and A3 carry no tag. Ancestry: fix 6 (`025502ceb5`) is in A0; fix 9 (`c2eb00b4f6`) is in A1 and not A0; fix 8 (`b241201e88`) is in A4 and not A1. Every one says what its row claims.
- **Per-fix "vendored copy affected?" verdicts, checked against `k9mail-vendored/` here (10 of 10 read):** item 1 `DefaultTrustedSocketFactory.java:113-117` reorders unconditionally (true; "yes in code form", effect unmeasured, worded honestly); item 2 `TrustManagerFactory.java:133-146` platform verifier with a shim (true); item 3 the SNI guard at `:216` (true); item 4 no "blocklist" or "blocked" in `TrustManagerFactory.java` (true); item 5 `ImapResponseParser.java:392` `new String(data, "US-ASCII")` (true); item 6 `MimeUtility.java:1053` unchecked `UnsupportedOperationException` (true; "in another form" is accurate); item 7 `FixedLengthInputStream` has `skip` but no `skipRemaining`, the same loop at `ImapResponseParser.java:372-374` (the verdict "could not tell" is right: the loop spins only if `skip` returns 0 while `available() > 0`, which I also cannot settle by reading); item 8 `ImapConnection.java:281-283` `responses.size() != 2` (true); item 9 `close()` at `:635` has no early return (true); item 10 `sendContinuation` at `:707-716` writes without a catch, its only caller is `ImapFolderPusher.java:722`, and the app has no reference to a pusher (true; "yes in code form, off the app's path" is accurate).
- **This repository's own changes (item 4 of the brief).** `git show --stat` of the four later commits touches exactly the ten listed source files (`ImapStore`, `DefaultTrustedSocketFactory`, `Address`, `Transport`, `EncoderUtil`, `MimeMessage`, `MimeUtility`, `MessageHeaderParser`, `TrustManagerFactory`, `RemoteStore`) plus `build.gradle`. None of the five commits is contained in any candidate by the text's evidence, and the evidence I could check agrees (no `closePooledConnections` upstream-side claim was contradicted; the upstream SNI guard dates from 2024-01-06 [C-487], after every candidate). **But the count "10" is incomplete; see W1.**
- **Hygiene.** No e-mail address, machine name, user name or profile path in the analysis (my scan). IC-1 and IC-4 are unaffected: `git status` shows only the analysis, the story, the log and `sprint-plan.md` modified and the two review files untracked.

### Is the direct answer supported, and are the limits honest?

- **Viability and the answer.** The cost and fit arguments hold up against the clone: from A1 the fork's search hook is gone (the A1 and A4 query builders have no HEADER term), the constructor changes at every candidate, `StoreConfig` and `ImapSearcher` exist until 2020, and from 2020-06-08 `ImapFolder` is a final Kotlin class with an internal constructor. "R-B is the only vendored route that reaches the 2022 to 2026 fixes" is true for the ten read (items 4, 5, 7 and 10 are 2023 to 2026). "R-A reaches 2 to 4 of the ten" and "493 to 625 commits behind" are right. "Short of the pin, still fork-and-patch" is right for A0, A1, A4. The answer does not overstate the unknowns: it says nothing was compiled and no advisory was checked.
- **Limits.** Stated openly and in more than one place: the commit-message search is not an audit ("a fix absent from the list proves nothing"); the 8 unread fix-like commits and about 690 other commits; no compile, no patch application; advisories for mime4j 0.8.11 and commons-io 2.16.1 "not checked in this analysis" (and the one identifier found in messages is an OkHttp patch); the web-open covered only the 128 new rows, and the text says so. The abandoned "hang" pass (it matched "change") is disclosed. This is the honest treatment.

### Findings

**Blockers.** None.

**Warnings.**

- **W1. The count "10 source files of this repository's own changes" is too low as a total, and it is reused in the comparison.** Q9.f 3(c) says "Distinct vendored source files that the five commits changed: 10", and the Section 6 comparison table, Q11.e, the Q12.c extension and PO-14 re-use "10 source files" as the amount to re-apply. The capture commit `132cf5bf` is one of the five, and its own table row says it rewrote imports in 12 files (`k9mail-vendored/build.gradle:30-35`: Address, BoundaryGenerator, Message, Part, MessageExtractor, MessageIdGenerator, MimeBodyPart, MimeHeader, MimeMessage, MimeUtility, TextBody, SmtpTransport). Three of them (Address, MimeMessage, MimeUtility) are among the ten; nine are not, so the distinct total is 19 for A0 to A3 (which use support-annotations) and 10 for A4 (androidx, as the row itself says). The row for A4 in the comparison table does say "minus the import rewrite", so the table is half-consistent, but the headline "10" understates the A0 to A2 re-apply set by nine files (import-only edits). It does not change the viability verdicts or any predicate. Suggested fix: state 19 (10 logic-bearing plus 9 import-only) for A0 to A3 and 10 for A4 in 3(c), the comparison table, Q11.e and PO-14. — Classification: defer-justified — the fix changes a stated number and adds a distinction, which the typo allowlist excludes.

- **W2 (carried, residual of W1 from iteration 1).** Unchanged by IR2 and not expected to change: the tie-break remains non-discriminating, as the document itself reports in "Reviewers' readings". — Classification: defer-justified — a statement the document makes openly; the value is the product owner's call.

**Notes.**

- **N1. Missing blank line merges two paragraphs in Q12.c.** The "**Trade-off.** ..." paragraph is followed directly (next line) by "**Option comparison extended with the two refresh routes of Q9.f ...**" with no blank line between them (line 1309 then line 1310), so a Markdown renderer shows them as one paragraph. The implementer reported fixing the missing blank before Q11.e (correct: that one is fixed and Q9.f has its heading), but not this one. Suggested fix: insert one blank line before line 1310. — Classification: fix-in-cycle (doc-prose-typo) — whitespace only; every meaning-bearing token stays byte-identical, and the analysis file is in the story's Affected Code table.

- **N2. "8 of the 10" slightly overstates what R-B closes.** The eight include item 7 ("could not tell") and item 10 (flawed in code form but off the app's path); only items 1 (effect unmeasured), 4, 5, 6, 8 and 9 are on the app's path with the flaw read in the vendored file. The direct answer, the Q12.c "Capabilities closed" cell and PO-14 (c) say "8 of the 10 fixes" without that qualification, although Q9.f 4 states the 7 / 2 / 1 split correctly. Suggested fix: add "(six with an on-path flaw read here, one off the path, one undetermined)" where "8 of the 10" is used as a gain. — Classification: defer-justified — wording that changes a stated claim, not a typo.

- **N3. "Viable only at A0, A1 and A4" and "newest point at which the subclassing design still exists" are looser than the evidence.** A2, A3 and the tags 5.701 to 5.703 are viable and ruled out on cost (dominated), not impossible, and the app's subclass-plus-`ImapSearcher` design survives until `ImapSearcher` is deleted (2020-04-18) and `StoreConfig` (2020-05-01), a few months after A4, with only a `FolderType` edit needed after 2020-01-09. The text gives these facts in the timeline and in the "ruled out" paragraph, so the body is accurate; only the summary phrases compress them. Suggested fix: "worth costing at" for "viable only at" and "a plateau chosen at" for "the newest point". — Classification: defer-justified — wording choice with no effect on any count or verdict.

- **N4. "5 more files than A2" for A3 (Q9.f 6).** Main-source files are 134 against 130 (4 more); candidate-only files against the vendored copy are 37 against 32 (5 more). The sentence should say "5 more candidate-only files". Separately, the reading rule for the Java / Kotlin counts does not say that `WebDavTransport.java` is excluded as well as the webdav directory (that is why my directory-only count is one higher at the merge base, A0, A1 and A2). — Classification: defer-justified — clarifications of a count's basis; the numbers themselves are right on the text's own definition.

- **N5. The keyword searches name three main-source paths but only describe the TLS-factory paths ("`app/core/...`, `legacy/core/...`").** A reader cannot repeat the 26 exactly (I reached 25 without them). Suggested fix: list the two exact paths. — Classification: defer-justified — a content addition, and the 38-row list is itself the checkable record.

### Items from the brief, short answers

1. Citation support: sampled and supported (see the record above).
2. Counts: five or more re-derived (ahead / behind pairs, Java / Kotlin, 26 + 12 = 38, 18 F, 188 and 5 + 2, 582, 14 / 17 / 5); consistent except W1 and N4.
3. Per-fix verdicts: all ten checked; the two "yes in code form" verdicts and the one "could not tell" are worded correctly.
4. Own changes not in any candidate: supported; the list of ten is right but incomplete as a total (W1).
5. Direct answer: supported and not overstated in substance; two summary phrases are loose (N2, N3).
6. Limits: honest and complete as far as I can see.
7. Protected parts: unchanged and identical; one Recommendation line in place.
8. Insert slips: Q9.f heading and the blank line before Q11.e are fine; one blank line is still missing before the Q12.c extension (N1).
9. Reading of the evidence: unchanged. Q9.f strengthens the lean, now with a better "stay" variant on the table: a cheap partial refresh (R-B, about 190 lines in 5 files plus 2 new, no app change, found by a search that proves nothing about the rest) reaches most of what the large migration would bring for this app, while every route that really tracks upstream still needs forked patches; so, as my own reading and not a recommendation to change the line, the evidence leans toward `no-go` on Part B, with R-B as the natural form of staying.

## Phase Completion Report (iteration 3)
---
story_id: "U-062"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-019/U-062/review-code.md"
story_status: "review"
current_build_phase: "review"
blockers: 0
warnings: 2
errors: []
---
