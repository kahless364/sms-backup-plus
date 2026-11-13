---
artifact_type: plan
story_id: "U-062"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-12"
iteration: 3
base_commit: "c8f04558dff63ab24ed31b3e3e19ee99c5cb8afc"
contracts_verified:
  - CNTR-MODERNIZATION-007   # measured read-only; not crossed
  - CNTR-MODERNIZATION-001   # measured read-only; not crossed
risks_identified: 11
---

# Implementation Plan: U-062 — Thunderbird Mail Libraries Feasibility Analysis (REQ-MODERNIZATION-019 Part A)

## Revision History

| Iter | Change |
|------|--------|
| 1 | Initial plan |
| 2 | Plan-review iteration 1 (`plan-review.md`) addressed. B1: decision table rebuilt as four mutually exclusive rows (Recommendation Logic). B2: the third capability test now checks the ENVELOPE superset (Q5). B3: IC-3 runs in regex mode with a positive control and a fail-closed guard. B4: D4.6 and I5 step 4 cover the at-acceptance reference edits. O1-O15 applied where marked |
| 3 | Contract-verification iteration 1 (`contract-verification.md`) addressed. B1: Port preservation and exception-mapping sub-tables in Section 5, filled by Q5.p. B2: predicate P (non-selecting) with the CR route against CNTR-007/-001; Section 11 rows labelled "conditional on CR". Observations applied: CNTR-001 measurement in Q7; the four search criteria and `max` trimming; per-turn Phase Completion Report; D4.7 logged at review launch; drift and stale id routed as follow-ups. Plan-review iteration 2 N1-N7 applied: three `Item.DATE` sites; (f) invocation and log entry; disjoint IC-3 control lines; Q4 grep classification plus `java.nio.file`; Q8 class regex; D2 wording; retro item names the three story locations |

## Story Overview

U-062 is analysis only. The deliverable is one cited feasibility analysis at
`sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` (called
**the analysis** below). It ends in exactly one recommendation: `go`, `go-with-conditions` or `no-go`.
No product file, build file or dependency changes. The executor is a Developer-role agent (sonnet) with a
web page fetch tool and a Git Bash shell (git, Gradle, JDK 25, Android SDK). Each increment runs in its own
orchestrator turn and is logged step by step. The story closes only after the product owner's written
acceptance (AC-15), which no agent can give.

This plan does not decide the outcome. It tells the executor where the evidence probably is, marking every
upstream fact as **to re-verify**. It also fixes the format the reviewers check mechanically.

## Verified Facts at Planning (HEAD `c8f04558`; product files identical to `7dfd651b`)

| Fact | Evidence |
|------|----------|
| Modules | `settings.gradle:1` `include ':app', ':metadata', ':k9mail-vendored'` |
| JDK 25 daemon | `gradle/gradle-daemon-jvm.properties:2` `toolchainVersion=25` |
| Gradle 9.5.1 | `gradle/wrapper/gradle-wrapper.properties:4` |
| AGP 9.0.1 / KGP 2.3.21 | `build.gradle:10`, `build.gradle:14` |
| compileSdk 35 / minSdk 21 / targetSdk 35 | `app/build.gradle:21`, `:24`, `:25` |
| App bytecode 17 | `app/build.gradle:90-91` (Java), `:107` (Kotlin `JVM_17`) |
| Release minified | `app/build.gradle:53` |
| Coroutines 1.7.3 | `app/build.gradle:118-119` |
| Vendored module: compileSdk 35, minSdk 21, Java 8 | `k9mail-vendored/build.gradle:52`, `:54`, `:76-77` |
| Vendored module deps | mime4j 0.8.11 `:100,:102`; commons-io 2.16.1 `:107`; jzlib 1.0.7 `:109`; jutf7 1.0.0 `:111`; androidx.annotation 1.7.1 `:114`; the bump history is in the header comments at `:16-19` |
| NOTICE origin and eviction | `k9mail-vendored/NOTICE:4-8` (fork, SHA `eaf68902…`, branch `sms-backup-plus-1.5.11`); `:25-30` (JitPack 404, U-027); `:20-21` list mime4j 0.7.2 / commons-io 2.4 as the versions **at capture** (story Scope Concern) |
| 14 k9-referencing files outside `mail/transport/` | `grep -rln "com\.fsck\.k9" app/src/main/java \| grep -v /mail/transport/` returns the 14 files of REQ-019's table |
| Headers | `mail/Headers.java:8-22` holds 13 `X-smssync-*` constants; `:25-26` hold `References` and `Message-ID` (15 in all) |
| SNI tests | `DefaultTrustedSocketFactorySniTest.java` has 10 `@Test` methods (lines 48, 70, 98, 133, 151, 171, 187, 202, 216, 231). It tests the vendored `com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory` (import at `:14`) |
| Vendored SNI guard | `k9mail-vendored/.../ssl/DefaultTrustedSocketFactory.java:208-251` (null/blank at `:217`, IPv6 `:221`, IPv4 `:225`, `SNIHostName` at `:246`) |
| Vendored hostname verifier | `k9mail-vendored/.../ssl/TrustManagerFactory.java:135-160` (`HttpsURLConnection.getDefaultHostnameVerifier()` and `MinimalSslSession`) |
| Pinning | `app/.../mail/PinnedCertificateSocketFactory.java:55` (`implements TrustedSocketFactory`), `:75` (`createSocket` with four arguments) |
| **Live** transport path | `mail/transport/K9MailTransport.java`: `BackupImapStoreDelegate extends ImapStore` `:414`; `BackupFolder extends ImapFolder` `:696`; `ImapSearcher` + `executeSimpleCommand` `:716-719`; `(HEADER %s "%s")` `:752`; `FetchProfile.Item.DATE` `:730-731` and `:258`; `Item.BODY` `:256`, `:299`. `mail/BackupImapStore.java` still exists (static `isValidUri`/`isValidImapFolder` used by `MailModule.kt:80`, `AdvancedSettings.java:533`). The executor confirms which path is live before citing it |
| Internal date set by the app | `mail/HeaderGenerator.java:56` `message.setInternalDate(sentDate)`; vendored `store/imap/ImapFolder.java:1177` `APPEND %s (%s) "%s" {%d}` |
| Header-only fetch lists (vendored) | `ImapFolder.java:686-689` ENVELOPE (`INTERNALDATE`, `RFC822.SIZE`, a `HEADER.FIELDS` list that includes `date` but no `X-smssync-*`); `:708-709` `DATE` gives `BODY.PEEK[HEADER.FIELDS (date)]` |
| `Item.DATE` sites (all three) | `K9MailTransport.java:731` (live, in `BackupFolder.getMessages` `max` trimming `:727-744`: DATE fetch, then sort, then reverse); `:258` (port `fetch(…, FetchSpec.ENVELOPE_DATE)`; production code never calls it, only tests do); `mail/BackupImapStore.java:181` (legacy class; only its statics are live). Every production use ends in `MessageComparator` (`K9MailTransport.java:765-776`, `compare` `:769-775`), which reads `getSentDate()` (the `Date` header) |
| Port as implemented | `mail/transport/MailTransport.java`: `checkSettings` `:50`; `openFolder` `:62`; `appendMessages` returning `long` `:87`; `getMessages` `:102`; `fetch` `:117`; `closeFolders` `:128`; `importMessageBody` `:149`. The throwing methods also declare `RequiresLoginException` |
| Adapter exception translation | `K9MailTransport.java:159-162` and `:176-179` (`XOAuth2AuthenticationFailedException` → `XOAuth2FailedException(e.getStatus(), e)`; `AuthenticationFailedException` → `RequiresLoginException`); `"Unable to get IMAP prefix"` text match at `:164,181,212,238,266,395` (vendored source of the text `store/imap/ImapFolder.java:102`); vendored `XOAuth2AuthenticationFailedException.java:7-10` (constructor takes an `org.json` `JSONObject`; thrown at `ImapConnection.java:400`) |
| Engine consumers of port semantics | `XOAuth2FailedException.status == 400` → token refresh: `service/BackupWorker.kt:442`, `service/RestoreWorker.kt:655`. `closeFolders()` in `finally`, relying on no-throw: `BackupWorker.kt:377`, `RestoreWorker.kt:277` |
| Other adapter behaviours the port relies on | `BinaryTempFileBody.setTempDirectory` `K9MailTransport.java:129`; `closeFolders` swallows per-folder errors `:445-454`; BUG-013 fresh-connection retry `forceFreshConnection` → `closePooledConnections()` `:556-559` (vendored `ImapStore.java:362`); folder-not-ready text match `NONEXISTENT` / `did not find message count` `:652-654`; UID → handle `msg.getUid()` `:230`; `importMessageBody` → `converter.getDataType`/`messageToContentValues` `:304-305`, errors returned as `MessageImportResult.failure` `:307-316`; search query `:749-758` (`HEADER X-SMSSYNC-DATATYPE`, `UNDELETED`, `SENTSINCE`, `FLAGGED`) |
| Contracts measured (read-only) | CNTR-MODERNIZATION-007: interface `:82-132`, exception-translation table `:209-224` (rows `:220-224`), Clause C-1 `:242-244`, `BinaryTempFileBody` preservation `:255`, breaking-change policy `:331-341`, Validation Rules 1-3 `:345-353`, Rule 5 `:356`, `closeFolders` no-throw `:365-367`. CNTR-MODERNIZATION-001: carried type and signature `:84-86`, permitted producers `:93-96`, breaking change on the carried type `:284-285` |
| Release APK name | depends on `keystoreProperties` (`app/build.gradle:55`), so use the glob `app-release*.apk` |
| Recipient setters used | `mail/MessageGenerator.java:99,102,144,146,207` (`setRecipient`/`setRecipients`) |
| No Gmail extension use | `grep -rn "X-GM\|XLIST" app/src/main/java k9mail-vendored/src/main/java`: no match |
| Test baseline | the story's `git grep … @Test` count = **682** at HEAD |
| JUnit XML tooling (tracked, reusable read-only) | `sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/tools/junit-xml.init.gradle`, `…/junit_totals.py` |
| dexdump available | Android SDK `build-tools/36.0.0` (used in sprint-018) |
| EPIC success criteria 5 and 9 (AC-16) | `EPIC-MODERNIZATION-006…md:92-97` and `:107-109` |
| AC-6 owning lines | REQ-015 minSdk 23 `:34`, `:113`; JDK 25 `:189`; JVM target 17 `:157`; "latest stable at planning" rule `:84-86` (snapshot Gradle/AGP/Kotlin `:52-54`, "not a target" `:85-86`). REQ-016 rule `:35-36`, coroutines snapshot `:62`. REQ-017 names no level: `:19-20`, `:31-36` |
| Research input (planning-time read) | 290 newline-terminated lines (`wc -l`; the story says 291), SHA-256 `f2b9610bedc2ca322ef496a1f45ec50b974dc6103a0e63b30ed320ea704aece9`. No IC-3 match for e-mail, hostname, user name or user-profile path. Line 124 contains the repo's absolute path. That is not an IC-3 pattern, so IC-5 keeps it verbatim |

## Decisions

**D1. Upstream pin rule.** Pin `thunderbird/thunderbird-android` at the research input's commit
`ac1f0e85f63c1b600ac443d1fb2d939bffff7de5`, and `thunderbird/thunderbird-mobile-components` at
`7aa2590ff4ff915efb721a9b1a69fa84ce8fac3f`. One condition applies: at I1 each must be an ancestor of its
repository's current `main` (`git merge-base --is-ancestor <pin> origin/main`). If that fails (history rewritten),
pin the then-current `main` head instead and record why. Reasons for keeping the research pin: it is the same day's
commit, it makes the input's line numbers usable as search hints, and AC-2 already demands a fresh primary-source row
for every carried claim, so a moving pin buys nothing and costs a full re-anchor. The analysis records, once at the
top: the pin SHA, its commit date and subject, the rule applied, the `main` head SHA observed at I1, and a
**drift list**. The drift list is `git log --format='%h %ad %s' --date=short <pin>..origin/main -- <in-scope paths>`
(in-scope paths: `mail/`, `core/common/`, `legacy/logging/`, `feature/account/api/`, `feature/mail/folder/api/`,
`core/architecture/api/`, `feature/mail/account/api/`, `build-plugin/`, `gradle/`, `settings.gradle.kts`,
`legacy/core/src/main/java/com/fsck/k9/helper/`, `legacy/common/`). An empty drift list says the finding set is
current. A non-empty one is listed but not chased (pin once).

**D2. Experiments.**

| # | Experiment | Decision | Reason |
|---|-----------|----------|--------|
| a | Partial clone (`--filter=blob:none`) of thunderbird-android at the pin, in scratch | **Run** (I1) | Line-accurate citations, `git grep`, history (ahead/behind counts, fork-only commit list, drift list) and no API rate limits. Nothing is executed |
| b | Read upstream build configuration (convention plugins, `ProjectConfig.kt`, version catalogue, wrapper, `settings.gradle.kts`) for Section 4 | **Run** (I2, from the clone) | AC-6 needs cited values, not a build |
| c | Build upstream `:mail:*` modules (in its tree or extracted) | **Do not run** | It would run untrusted build logic: AGP 9.5, isolated projects, about 1-2 GB of downloads. No AC requires it (AC-7's "yes"/"via shim" accept code citations, and AC-10 accepts an estimate). It cannot change the recommendation's class: the decision table below turns on capability gaps (Sections 5 and 7), licences (Section 3) and gating platform conflicts (Section 4), all of which come from reading. Its question ("buildable outside the upstream tree, at what edit cost") becomes a numbered Part B condition with its own test if the outcome is go-with-conditions. It is recorded in the Experiment log as "not run" with this reason |
| d | Throwaway copy of this repo compiled against upstream modules (size/method delta) | **Do not run** | It depends on (c), which is not run, and would mean porting 14+10 files. AC-10 permits an estimate. The estimate method is fixed in Q8 below. It is recorded as "not run" |
| e | Release baseline (`:app:assembleRelease`, APK bytes, dex method/class counts) in the **real tree** | **Run** (I0, re-run at I4) | Required by AC-10. It changes no tracked file. This is proven by an unchanged `git status --porcelain` and an empty `git diff --stat <start> -- <product paths>` before and after |

If a reviewer or the product owner later asks for (c) or (d), they run under the experiment rules in a new rework
increment. The stop rule applies.

Not building upstream departs from the story's Estimation Guidance, which expects small build and probe experiments.
It is allowed by AC-10 (estimate), AC-9 (code reference) and REQ-019 question 8. The analysis says so in plain words
in Section 8 and in the Experiment log: "Upstream was not built; reason: …". I5 tells the product owner the same.
The unbuilt state must never appear as "unknown" anywhere, which would fail AC-1. Its consequence appears only as a
cost range in Sections 9 and 11 or as a numbered condition.
**Optional (f), allowed by rule 3:** a scratch-only Gradle project at `$S/f` that declares only the **third-party**
coordinates from Section 2. It runs no upstream build logic and gives the exact resolved closure, POM licences and jar
sizes for Q3 and Q8. Run it if the POM walk in Q3 exceeds about 30 artifacts. Otherwise skip it and record that it was
skipped. The `settings.gradle` and `build.gradle` it needs are written by the executor in scratch; they are never
copied into the repo. Invocation:
`env -u GH_TOKEN -u GITHUB_TOKEN GRADLE_USER_HOME=$S/gh <repo>/gradlew -p $S/f --no-daemon dependencies --configuration runtimeClasspath`.
The scratch project has no daemon-JVM criteria file, so the JDK is whichever JDK launches Gradle. Record
`java -version` and the Gradle version line from the output. The Experiment log has an (f) entry either way.

**D3. No evidence directory.** Raw outputs (Gradle logs, `mapping.txt`, dex files, probe headers) stay in scratch.
Only derived numbers and the exact commands (with `<scratch>`/`<repo>` placeholders) go into the analysis and
`implementation-log.md`. This keeps IC-4's delta minimal and keeps machine paths out of tracked files.

**D4. Story-text discrepancies (recorded interpretations, for review and QA).**
1. Affected Code names the story at `sdlc/artifacts/stories/U-062-…`. The real file is
   `sdlc/artifacts/build/sprints/sprint-019/U-062-thunderbird-mail-libraries-feasibility-analysis.md`. Edit the real
   file (Implementation Notes only).
2. IC-4's literal list leaves out the pipeline's own workspace files (`sdlc/artifacts/build/sprints/sprint-019/U-062/*`:
   plan, plan-review, contract-verification, expert-depth-check, implementation-log, review-code, review-security,
   qa-results) and framework bookkeeping (`sdlc/.skill-*`, `sdlc/.engine/`, audit logs). IC-1 allows "workflow
   bookkeeping the pipeline itself writes under `sdlc/`". The IC-4 record lists these by name as that class. Any
   other new path fails IC-4.
3. The story says the input is 291 lines; `wc -l` gives 290. Identity is proven by SHA-256, not by line count. The
   analysis records the measured count and the discrepancy.
4. `git status --porcelain` collapses untracked directories (`?? sdlc/artifacts/build/sprints/sprint-019/`). The
   story's command is recorded as written. `--untracked-files=all` is also recorded, because it is the only form that
   shows files inside those directories.
5. AC-6 targets for Kotlin, AGP and Gradle (REQ-015) and for coroutines (REQ-016) are a **rule** ("latest stable at
   the implementing story's planning date") with a dated snapshot, not a fixed number. The target cell quotes the rule
   line and the snapshot line. "No conflict" is allowed only if the upstream requirement is met by the snapshot value.
   compileSdk (REQ-017) names no level at all, so it must read "not specified in REQ-MODERNIZATION-017" and gets the
   verdict "conflict (unresolved: target not specified)", with resolution options and a product owner decision noted.
   Under the decision table, a Section 4 conflict excludes `go` only if it is **gating**: the analysis shows Part B
   cannot start until it is resolved, under every viable consumption mode. The compileSdk "not specified" verdict on
   its own is never gating. It is listed in Section 12 as a "PO decision needed" item.
6. **At-acceptance edits outside the implementer's diff.** Story line 57 requires reference edits at acceptance:
   REQ-MODERNIZATION-019 and EPIC-MODERNIZATION-006 gain the analysis path, and for `no-go` REQ-019's
   `change_records` lists the CRs (AC-16). IC-1, IC-4 and Affected Code do not list those two files. Interpretation:
   these edits, and the `no-go` change-record files, are made by the Requirements Analyst or Artifact Librarian
   after the decision record. They are outside the implementer's diff. QA evaluates IC-1 and IC-4 for the
   implementer's work against the pre-acceptance state (the I4 records), plus exactly this enumerated
   post-acceptance set:
   `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-019-replace-vendored-k9-with-thunderbird-mail-libraries.md`
   (reference and `change_records` fields only),
   `sdlc/artifacts/requirements/modernization/EPIC-MODERNIZATION-006-dependency-currency-latest-stable.md`
   (reference only), and `sdlc/artifacts/build/change-records/CR-*.md` (`no-go` only). Any other change to those
   files, or any other path, fails IC-4. This is a story-text inconsistency: flag it for the sprint retro. Story
   line 57 (reference edits at acceptance) disagrees with line 99 (IC-1) and line 102 (IC-4), and with the Affected
   Code section at lines 106-115.
7. **QA timing deviates from the compiled pipeline.** `compiled-pipeline.json` puts `qa-validation` in the review
   parallel group with no `depends_on`. This plan defers QA until after the AC-15 record (Acceptance Sequencing).
   The orchestrator records the deviation in the implementation log (an orchestrator entry) **when it launches code
   and security review without QA** (Acceptance Sequencing step 2), and in its sprint records. Otherwise a mechanical run would launch QA early, and an AC-15 FAIL would trigger
   `on_any_fail → blocked`.

## Environment and Conventions (executor)

```bash
unset MSYS_NO_PATHCONV                     # never set for ./gradlew
cd <repo>                                  # the repo root, given by the orchestrator; never written into files
A=sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md
IN=sdlc/artifacts/build/feasibility/U-062/inputs/thunderbird-mail-libraries-research-2026-11-12.md
W=sdlc/artifacts/build/sprints/sprint-019/U-062; LOG=$W/implementation-log.md
T=sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/tools   # read-only reuse
START=<start commit recorded at I0>; PIN=ac1f0e85f63c1b600ac443d1fb2d939bffff7de5
S=<scratch>   # outside the repo root; supplied per turn by the orchestrator in Git Bash POSIX form
TB=$S/tb      # the thunderbird-android clone
SDK=$(sed -n 's/^sdk\.dir=//p' local.properties | sed 's/\\:/:/g; s/\\\\/\//g')   # read, never written
DEXDUMP="$SDK/build-tools/36.0.0/dexdump"
```

- **Scratch guard:** first `mkdir -p "$S/logs"`, then
  `case "$(cd "$S" && pwd -P)/" in "$(pwd -P)/"*|/) echo ABORT;; esac`. It must not print ABORT. Never write the
  value of `$S`, `$SDK` or the repo path into any file. Write `<scratch>`, `<sdk>` and `<repo>`.
- **No credentials in experiments:** run git network commands as `env -u GH_TOKEN -u GITHUB_TOKEN git -c credential.helper= …`.
  Straight after cloning, run `git -C $TB config credential.helper ""` so the later lazy blob fetches (`checkout`,
  `show`, `grep`) also run without credentials.
  Use HTTPS URLs only. An SSH-form remote (user `git` at the GitHub host) also matches the IC-3 e-mail pattern.
- **History output hygiene:** always use `--format='%h %ad %s' --date=short` (or `%H`). Never use `git log` default
  output, `%ae` or `%an <%ae>`, which would capture author e-mails (IC-3).
- **Do not:** run `./gradlew` with `--write-verification-metadata`; edit any product or build file; `git add`, `commit`,
  `stash`, `checkout` or `reset` in the repo (the orchestrator commits, path-limited); create files other than the
  analysis, the input copy, `implementation-log.md` and the story's Implementation Notes.
- **Gradle gate idiom:** `rm -rf app/build/test-results/testDebugUnitTest; ./gradlew -I $T/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease > $S/logs/<inc>-gates.log 2>&1; echo "exit=$?"`,
  then `python -I $T/junit_totals.py app/build/test-results/testDebugUnitTest`. The init script's `U061-DAEMON-JVM`
  line is the JDK evidence.
- **Web tool:** prefer `raw.githubusercontent.com/<owner>/<repo>/<sha>/<path>` and the local clone over rendered
  GitHub pages, which may truncate. Record HTTP status for registry probes (use `curl -sS -o /dev/null -w '%{http_code}' -I <url>`
  or the web tool; state which).

## Logging Protocol (resumable)

`implementation-log.md` is appended **after every numbered step, not at the end of an increment**:
`### I<n>.<k> <title>`, then the commands (with placeholders), the result lines, and `status: done|failed|inconclusive`.
Each increment opens with `## I<n> START <UTC timestamp>` (inputs listed) and closes with
`## I<n> END` (checks with results, files touched, and a line `NEXT: <exact next step>`).
**Resume rule:** at the start of every turn, read the log's last `NEXT:` line and the last step entry. If an
increment has START but no END, resume at the first step without `status: done`. Re-run that increment's end checks
before closing it. Never re-create the input copy if its hashes already verify. Never re-clone if `git -C $TB rev-parse HEAD` equals the pin.

**Return to the orchestrator at each increment end** (≤ 15 lines): increment, status, end-check results, the analysis
sections now complete, citation count (rows = distinct IDs), open contradictions with the research input, `NEXT:`
line, and any question for the orchestrator. The orchestrator may run I1 as two turns (I1a: clone, D1, I1.2, Q1;
I1b: Q2-Q3) and I2 as two turns (I2a: Q4-Q5 and the Port preservation tables; I2b: Q6-Q8). Each turn has its own
START/END.

**Per-turn Phase Completion Report (CNTR-PIPELINE-006).** Every executor turn ends with the summary above, then a
PCR in which:
- `artifact_path` is `sdlc/artifacts/build/sprints/sprint-019/U-062/implementation-log.md`;
- for I0-I3 (including I1a/b and I2a/b): `verdict: "PASS"`, `story_status: "in-progress"`,
  `current_build_phase: "implementation"`, and the `NEXT:` line copied into `notes`;
- only the I4 turn sets `current_build_phase` to the quality gate (`"quality-gate"`).

A step logged `failed` or `inconclusive` under the stop rule still gives `PASS` for the turn, provided the increment
reaches its END. `FAIL` is reserved for an increment that cannot reach END. `ERROR` is for unreadable inputs (for
example, the research input missing).

## Analysis Skeleton (create at I0; fill per increment)

Headings are exact. `grep -nE '^## ' $A` must show this order and nothing else at level 2:

```
---
artifact_type: feasibility-analysis
story_id: U-062
requirement: REQ-MODERNIZATION-019 Part A
upstream_pin: <40-hex>          # thunderbird/thunderbird-android
components_pin: <40-hex>        # thunderbird/thunderbird-mobile-components
start_commit: <40-hex>
---
# Feasibility Analysis: Replacing :k9mail-vendored with Thunderbird for Android Mail Libraries
Upstream pin: thunderbird/thunderbird-android @ <40-hex> (<date>, "<subject>"); rule: <D1 text>; main head at I1: <sha>; drift: <list|none>
## Baseline
## 1. Consumability
## 2. Modules and dependency graph
## 3. Licence
## 4. Platform requirements
## 5. API mapping
## 6. Authentication
## 7. Custom trust, pinning and SNI
## 8. Size and method count
## 9. Fallback options
## 10. Separability
## 11. Sizing and split
## 12. Recommendation
## Citations
## Experiment log
## Product owner decision
```

The Baseline section is unnumbered (IC-6). It is a table: start commit, Gradle, AGP, Kotlin, JDK, bytecode (app 17,
vendored 8), minSdk, compileSdk/targetSdk, test count (682 run / 2 skipped; "682, not REQ-019's 697: sprint-018 U-061
deleted 15 donation tests"), each with `file:line`. It also holds the I0 records: porcelain (both forms), untracked
list, input hashes, line counts and redactions (IC-5). No level-3 heading may start with a number (`### 1.` would read
as a numbered section). Use `### Q1.a …`-style subheadings. Temporary text is the single token `PENDING`. It must be
gone at I4 (`grep -c PENDING $A` = 0).

**Required tables (fill, do not design):**

| Section | Table(s) and columns |
|---------|----------------------|
| 1 | Registry probes: `Registry \| URL probed \| Control probe URL and status \| Retrieved \| Result (coordinates+version or HTTP status) \| Citation`, exactly 5 rows (Maven Central, Google Maven, Mozilla Maven, JitPack, GitHub Packages). Found artifacts: `Coordinate \| Version \| Signed (.asc present) \| Stable or snapshot \| Mail module? \| Citation`. Then: "Is any mail module published as a Maven artifact: yes/no". Then provenance verdict (correct / incorrect / partly correct) with ≥1 quoted primary-source sentence; continuation repository; ahead/behind with compare URL and merge base |
| 2 | Per-file: `# \| File (14 rows, REQ-019 order) \| k9 types used \| Replacement module (Gradle path) \| Module kind (JVM/KMP/Android) \| Replacement type(s) and package at pin \| Notes (moved / removed / now interface / internal)`. Recount line: the command, the result, and the difference from 14. Per proposed module: `Module \| Direct deps (version) \| Transitive deps (version, via) \| Citation`. Legacy four: `Library \| Vendored version \| Upstream version at pin \| Replaced or retained \| Citation` |
| 3 | `Component \| Version \| Form (source module / binary) \| SPDX \| Identifier source (LICENSE opened / POM only / POM + licence text opened) \| Apache-2.0 compatible (yes/no) \| Obligations \| Citation`. Every module and transitive dependency from Section 2 gets a row. Then the NOTICE/attribution obligations list replacing `k9mail-vendored/NOTICE`, the MPL-2.0 file-level obligations, and the NOTICE-defect finding |
| 4 | Rows: minSdk, compileSdk, Kotlin, kotlinx-coroutines, AGP, JVM target, JDK to build, Gradle. Columns: `Item \| Upstream at pin (cited; state the consumption mode it applies to) \| Target (owning REQ file:line) \| App now (file:line) \| Verdict \| Resolution options / PO decision needed` |
| 5 | Operations: `Operation \| App call today (file:line) \| Replacement call at pin (cited) \| Behavioural difference \| Java-callable without coroutines (yes/no/via shim + evidence) \| Achievable without forking (yes / needs fork of <files>)`. Exactly 8 rows: connect/login; folder create/select; APPEND with flags and internal date; UID SEARCH by header; FETCH BODY.PEEK (headers; full body); X-GM-EXT-1/labels; MIME build (four labelled sub-cells: multipart / text bodies / base64 attachments / address and header encoding); MIME parse. Headers sub-table (15 rows): `Constant \| Header \| Headers.java line \| Writable at pin \| Readable after full-body fetch \| Readable by a header-only fetch (vendored vs pin)`. Then the **Port preservation** subsection (`### Q5.p Port preservation (CNTR-MODERNIZATION-007)`). It is a sub-table of Section 5, not a ninth AC-7 row and not a numbered section. Columns: `Port member (MailTransport.java line) \| CNTR-007 clause (line) \| Upstream backing call at pin (cited) \| Throwables upstream can raise (FQN) \| Maps to (MailException / TemporaryImapException / XOAuth2FailedException / RequiresLoginException / none) \| Verdict (preserved / preserved with adapter change / needs contract change (CR)) \| Notes (contract-text drift)`. 9 rows: `checkSettings`, `openFolder`, `appendMessages`, `getMessages`, `fetch`, `closeFolders`, `importMessageBody`, the public constructor (Clause C-1), and `BinaryTempFileBody` (Rule 5). Then the exception-mapping sub-table, one row per CNTR-007 translation row (`:220-224`, 5 rows): `CNTR-007 row \| k-9 throwable today \| Upstream throwable and FQN at pin (cited) \| Discriminator needed (subtype / getStatus() / message text) \| Discriminator present at pin (yes/no, cited) \| Verdict` |
| 6 | `Mechanism \| Implemented (yes/no) \| How selected \| Citation` (PLAIN, LOGIN, CRAM-MD5, EXTERNAL, XOAUTH2, OAUTHBEARER, others found). Then token provider: interface module vs implementation module, and what the app supplies. Then the Google OAuth consequence in ≤ 2 sentences |
| 7 | `Protection \| Extension point \| Code reference or PoC \| Status (supported / needs fork of <files> / blocker)`, 4 rows. The pinning row also states the CNTR-MODERNIZATION-001 measurement (Q7). SNI case table (10 tests): `Test method (line) \| Input \| Vendored result \| Upstream result at pin \| Basis (test run / code reading)`. The pinning row states store semantics ("reject everything else" vs "user-accepted exception") |
| 8 | `Measure \| Baseline at start commit \| Delta \| Measured or estimate \| Method/tool`. Verification-metadata: `Coordinate \| Version \| Present in gradle/verification-metadata.xml (y/n)`, with the new-entry count |
| 9 | `Option \| Cost (files, work items, counted) \| Supply-chain risk (with U-027 precedent) \| Security-patch owner \| Citation`, ≥ 3 rows (newer upstream snapshot; fork; stay and record). If Section 1 found a published mail artifact: "not required" and why |
| 10 | `Goal \| Separable / inseparable \| Evidence (cited)`, 2 rows (RCS; Google OAuth sign-in) |
| 11 | Sizing counts table (files, test classes, tooling, live-account run, Kotlin/coroutine adoption). Breakdown: `# \| Working title \| One-line scope \| Depends on (row # / REQ-015 / REQ-016 / CR (P-n))`, acyclic, prose rows only. Every row assumes CNTR-MODERNIZATION-007 (and -001) unchanged. A row that would need a contract change is labelled "conditional on CR (P-n)" and contains no interface design |
| 12 | Predicates B/F/G/S/P with the findings that set them, the selected row T1-T4 (plan interpretations labelled), "PO decision needed" items (including each P item with clause and CR route), option comparison, conditions (numbered, each with a test) or the no-go decision record, then a "Follow-ups" list (the pre-existing CNTR-007 text drift and DES-009 stale contract id, routed by the orchestrator to the Artifact Librarian; and any maintainer enquiry). Then the separability restatement, then the AC-16 line, then the last line `Recommendation: <value>` |
| Citations | `\| ID \| Claim \| URL \| Commit/tag/version \| Retrieved \| Status \|`, one row per ID, no `\|` inside cells |
| Experiment log | One entry per experiment (a)-(f) plus the IC-3, IC-1/IC-4 and gate commands: scratch placeholder, upstream commit, Gradle/JDK, commands, exit code, result. Entries (c) and (d) say "not run" with the D2 reason. Entry (f) says "run" (with Gradle and JDK versions) or "skipped" with the reason |
| Product owner decision | Orchestrator-only. The executor writes the heading and one line: `Pending: to be recorded by the orchestrator or Artifact Librarian (AC-15).` |

## Citation Mechanics

- **ID format:** `C-001` upward, allocated in writing order. Gaps are allowed. Never renumber. Add the row **in the
  same edit** that first uses the ID. Remove a row when its last use is removed.
- **URL forms allowed:** `https://github.com/<owner>/<repo>/blob/<40-hex>/<path>#L<n>[-L<m>]`;
  `…/tree/<tag>/<path>`; `…/commit/<40-hex>`; `…/compare/<40-hex>...<owner>:<repo>:<40-hex>`;
  registry/POM/licence URLs (version in the Commit/tag/version column); dated web pages such as blog posts and PR or
  issue pages (column = "web page, published YYYY-MM-DD" or "PR #n, state at retrieval"). **Never** `/blob/main/`,
  `/tree/main/` or any branch name. Never the research input or anything under `feasibility/U-062/inputs/`.
- **Line confirmation:** read lines at the pin from the clone with `git -C $TB show $PIN:<path> | sed -n '<n>,<m>p'`
  or `git -C $TB grep -n '<pattern>' $PIN -- <path>`. This works whatever is checked out. Then build the permalink.
  Status: `grep-confirmed` (located by `git grep` at the pin), `read` (read at the pin in the clone), or
  `opened` (fetched through the web tool: the raw URL at the same SHA showing the cited line text, or the
  registry/web page itself).
- **Web-open sample (executor):** every citation in Sections 1, 3, 4, 6 and 7 (the sections QA opens in full), every
  Section 5 citation on the APPEND, UID SEARCH and FETCH rows and in the Port preservation and exception-mapping
  sub-tables, and every non-GitHub URL. Elsewhere, at least every fifth
  ID in order. Mark each `opened`.
- **Retrieval date:** the UTC date of the read, `YYYY-MM-DD`. Clone reads use the clone date.
- **Mechanical check (run at every increment end; record the outputs):**

```bash
# body = whole document minus the Citations section (Experiment log and PO decision included)
body(){ awk '/^## Citations$/{c=1;next} /^## Experiment log$/{c=0} !c' "$A"; }
cit(){ sed -n '/^## Citations$/,/^## Experiment log$/p' "$A"; }
body | grep -oE '\bC-[0-9]{3}\b' | sort -u > $S/used.txt
cit | grep -oE '^\| C-[0-9]{3} \|' | grep -oE 'C-[0-9]{3}' | sort > $S/rows.txt
uniq -d $S/rows.txt; diff $S/used.txt $S/rows.txt            # both must print nothing
cit | grep -E '^\| C-[0-9]{3} \|' | awk -F'|' 'NF!=8{print "COLS "$2;next}{for(i=2;i<=7;i++)if($i~/^ *$/)print "EMPTY "$2}'
grep -nE '(github\.com/[^ )|]*/(blob|tree)|raw\.githubusercontent\.com/[^/ ]+/[^/ ]+)/(main|master|beta|release|develop)(/|#|$)' "$A"   # nothing
grep -oE 'github\.com/[^ |)]*/(blob|commit)/[^/# |)]+' "$A" | grep -vE '/(blob|commit)/[0-9a-f]{40}$'          # nothing (full SHAs only)
cit | grep -E '^\| C-[0-9]{3} \|' | awk -F'|' 'match($4,/\/(blob|commit)\/[0-9a-f]{40}/){s=substr($4,RSTART,RLENGTH);sub(/.*\//,"",s);if(index($5,s)==0)print "SHA-MISMATCH "$2}'   # nothing
grep -nE 'inputs/thunderbird|thunderbird-mail-libs-research' <(cit)                           # nothing
wc -l < $S/rows.txt                                          # report = number of citations
```

## Implementation Steps

### I0 — Baseline, input copy, skeleton (Turn 1; no product edit)

1. **I0.1 Start state.** `git rev-parse HEAD` gives START. Record verbatim in the log and the Baseline section:
   `git status --porcelain`, `git status --porcelain --untracked-files=all`, `git ls-files --others --exclude-standard`.
   The paths are repo-relative, so no machine strings appear.
2. **I0.2 Input copy (IC-5).** The orchestrator passes the source's absolute path in the turn prompt. Refer to it in
   files only as "the orchestrator's session scratchpad, `thunderbird-mail-libs-research.md`".
   Hash with `sha256sum < "<src>" | cut -d' ' -f1`. Reading from stdin keeps the path out of the output, so the
   comparison is a plain string match. If the hash differs from the planning hash `f2b9610b…aece9`, stop and ask
   (the input changed).
   Then `mkdir -p "$(dirname $IN)"; { printf '%s\n' 'UNREVIEWED INPUT - not a citation source'; cat "<src>"; } > $IN`
   (shell, not the Write tool, to keep bytes exact).
   Checks: `head -1 $IN` equals the banner; `tail -n +2 $IN | sha256sum | cut -d' ' -f1` equals the source hash;
   `wc -l < file` for both (expect 290 and 291). Run the IC-3 grep on `$IN`. The planning pre-check found no match, so the expected redaction
   list is "none". If any match appears, replace only the matched text with `[REDACTED-IC3]`, list line and category
   (never the value), and record that the identity check becomes "identical except the listed lines" (`diff` output
   with the value masked). Record SHA-256 of source and copy.
3. **I0.3 Baseline facts.** Re-run the Verified Facts greps at START and record value plus `file:line`. A difference
   from this plan's table is recorded and the measured value wins.
4. **I0.4 Gates and size baseline (D2-e).** Run the gate idiom (`I0`). Then:
   APK: `ls app/build/outputs/apk/release/app-release*.apk` (exactly one; record its name), then bytes with `wc -c <`.
   Dex: `unzip -q -o <that apk> 'classes*.dex' -d $S/dex0`. For each dex, `"$DEXDUMP" -f <dex> | grep -E 'method_ids_size|class_defs_size'`
   (`$DEXDUMP` from the Environment block, recorded as `<sdk>/build-tools/36.0.0/dexdump`) and sum. If `-f` does not print those fields, that is
   attempt 1. Attempt 2: count `Class descriptor` lines (U-061 recipe) and method entries in `dexdump` output.
   Attempt 3: the `classes*.dex` header bytes, read with `od` at offsets 0x58 (method_ids_size) and 0x60 (class_defs_size).
   Then the stop rule. Vendored footprint: copy `app/build/outputs/mapping/release/mapping.txt` to `$S` and count original
   classes per prefix: `grep -cE '^com\.fsck\.k9\.[^ ]+ -> '`, the same for `org\.apache\.james\.mime4j`,
   `org\.apache\.commons\.io`, `com\.jcraft\.jzlib`, `com\.beetstra`, plus member lines under those classes (methods
   retained). Totals: `junit_totals.py` must give tests=682 skipped=2 failures=0 errors=0, and the `@Test` `git grep` = 682.
   Afterwards, `git status --porcelain` must equal I0.1 and
   `git diff --stat $START -- app k9mail-vendored metadata gradle settings.gradle build.gradle gradle.properties gradlew gradlew.bat`
   must be empty. Record these two results as the "baseline changes no tracked file" proof.
5. **I0.5 Skeleton.** Create `$A` with the skeleton, the header placeholders, the filled Baseline section and the
   `Pending` line under "Product owner decision".
6. **End checks:** banner and hash; gates exit 0 with 682/2; porcelain unchanged except `$A`/`$IN` (collapsed
   under `?? sdlc/artifacts/build/feasibility/`) and `$LOG`; IC-3 grep over `$A $IN $LOG` returns zero. Log END.

### I1 — Pin, clone, Q1-Q3 (Turn 2, or I1a: I1.1, I1.2, Q1 / I1b: Q2-Q3)

1. **I1.1 Clone (D2-a).** Scratch guard. `env -u GH_TOKEN -u GITHUB_TOKEN git -c credential.helper= -c core.longpaths=true clone --filter=blob:none --no-checkout https://github.com/thunderbird/thunderbird-android.git $TB`,
   then `git -C $TB config credential.helper ""`, then `git -C $TB checkout --detach $PIN`. Check with `git -C $TB rev-parse HEAD`. Apply D1 (ancestor check, main
   head, drift list). The components repository is small: a second partial clone at its pin, or raw URLs at its SHA.
   Record the clone date. Three distinct failed approaches (for example partial clone, then `--depth`-limited fetch of
   the pin SHA, then raw-URL-only reading) make the experiment "inconclusive" under rule 5. Raw-URL reading still
   satisfies AC-2.
2. **I1.2 Fork position (AC-3).** `git -C $TB fetch https://github.com/jberkel/k-9.git sms-backup-plus-1.5.11`
   (fallback: fetch the SHA `eaf689025ec28d55e3b62f5ea3b1f185f410ee6b` directly). Confirm the SHA exists, then
   `git -C $TB rev-list --left-right --count $PIN...eaf68902…` (left = behind, right = ahead), `merge-base`, and
   `log --format='%h %ad %s' --date=short <merge-base>..eaf68902…` (fork-only commits). The cited compare URL is
   `…/compare/$PIN...jberkel:k-9:eaf689025ec28d55e3b62f5ea3b1f185f410ee6b`, opened through the web tool. Local counts
   are authoritative. A difference from GitHub's page or from the input's "11 ahead / 15,032 behind" (measured
   against a different `main`) is explained.
3. **I1.3 Q1, Q2, Q3** per the question guide. End checks: citation check; Sections 1-3 web-open sample done; IC-3
   grep; porcelain unchanged. Log END.

### I2 — Q4-Q8 (Turn 3, or I2a/I2b)

Q4 from the clone's build configuration (D2-b). Q5 (including the Port preservation and exception-mapping
sub-tables, Q5.p), Q6 and Q7 from the clone and the repo. Q8 from the I0 numbers plus the estimate method. End checks
as at I1, plus row counts: 8 operations, 15 headers, 9 port rows, 5 exception-mapping rows. The Section 5 decisive
rows and the port sub-tables are web-opened.

### I3 — Q9-Q12 (Turn 4)

Sections 9-11 (Section 11 rows are labelled "conditional on CR (P-n)" where P applies). Then apply the decision table
in Section 12, mechanically and visibly, and list each P item with its CR route. If the result is `no-go`, write
the AC-16 decision record and the **text** of the two change records (no files). End checks as at I1, plus the AC-14
greps.

### I4 — Consistency, gates, hygiene (Turn 5)

1. Whole-document pass. `PENDING` count 0. Heading order check. Every number that appears twice agrees (the 14
   files, 15 headers, 10 SNI tests, 9 port rows and 5 exception-mapping rows, 682 tests, ahead/behind counts, module
   and dependency counts between Sections 2, 3 and 8; every "needs contract change (CR)" verdict has a P item in
   Section 12, and every P item has a source row). Each arithmetic result is recomputed and the recomputation recorded in the log.
2. Complete the web-open sample. Final citation check.
3. End gates (IC-2) with the gate idiom (`I4`). Expect exit 0 and 682/2. Record APK bytes again; they must equal I0,
   since no product change was made. A difference is investigated, not explained away.
4. IC-1, IC-4 and IC-3 commands exactly as in the verification table. Results go into the Experiment log and the
   story's Implementation Notes (start commit; pointers to the porcelain and untracked lists in the analysis Baseline;
   pin; input hashes; Experiment log pointer; IC-3 zero-match result). Log END with `NEXT: orchestrator — reviews, then I5`.

### I5 — Acceptance (orchestrator, product owner, then RA/Librarian; no executor)

See Acceptance Sequencing, steps 3-7.

## Question Guide (all upstream facts: to re-verify at the pin)

Paths are upstream-relative unless prefixed `<repo>`. "Notes §n" points into the research input as a search hint
only.

**Q1 Consumability.** Sources: the upstream build files of the mail modules and the convention plugin
`build-plugin/src/main/kotlin/thunderbird.library.jvm.gradle.kts`. Look for the absence of `maven-publish`, `signing`,
group and version (absence is shown by `git grep -nE 'maven-publish|signing|publishing' $PIN -- mail build-plugin`
with its output). Registry probes, each with a **control probe** of a group known to exist, so that a 404 means
absence and not "listing unsupported":

| Registry | Probe | Control |
|----------|-------|---------|
| Maven Central | `https://repo1.maven.org/maven2/` + `com/fsck/k9/`, `app/k9mail/`, `net/thunderbird/` (and its children) | `…/org/apache/james/apache-mime4j-core/` |
| Google Maven | `https://dl.google.com/dl/android/maven2/master-index.xml`, searched for `thunderbird\|k9\|fsck`; group-index for any hit | presence of `androidx.annotation` in the same index |
| Mozilla Maven | `https://maven.mozilla.org/maven2/` + the same group paths, and `maven-metadata.xml` for candidate GAs | a known Mozilla group (for example `org/mozilla/components/`), status recorded |
| JitPack | the **builds API** `https://jitpack.io/api/builds/com.github.thunderbird/thunderbird-android` (also `com.github.k9mail/k-9`, `com.github.jberkel/k-9`). Do not request artifact URLs, which trigger third-party builds | — |
| GitHub Packages | `https://github.com/orgs/thunderbird/packages?ecosystem=maven` (public page) and `https://api.github.com/orgs/thunderbird/packages?package_type=maven` (record 401/403 as the result) | — |

The `net/thunderbird/components/*` artifacts (notes §2.3) are **found, non-mail** artifacts. Report their
signing (`.asc`) and stable/snapshot status anyway, because they are transitive (Section 2).
Provenance: quote at least one sentence from the 2022 announcements (notes §1.1-1.2 give the URLs) and give the
verdict. Continuation repository: GitHub redirect and repository ID (`https://api.github.com/repos/k9mail/k-9` →
`full_name`). Pitfall: the Maven Central search index is unreliable (notes §2.9); do not use it as evidence of absence.

**Q2 Modules.** Sources: `settings.gradle.kts`, `mail/common/build.gradle.kts`, `mail/protocols/imap/build.gradle.kts`,
the convention plugins (`thunderbird.library.jvm|kmp.gradle.kts`), `gradle/libs.versions.toml` (versions and bundles),
and each project dependency's build file (`core/common`, `legacy/logging`, `feature/account/api`,
`feature/mail/folder/api`, `core/architecture/api`, `feature/mail/account/api`). Third-party transitive dependencies
come from each artifact's `.module` file (preferred) or POM on the registry, with scope noted. Locate each old type
with `git -C $TB ls-files | grep -E '/<Type>\.(kt|java)$'`. Moved packages (notes §3.2: `Flag`, `MessagingException`)
and removed types (`Folder`, `StoreConfig`, `NetworkType`, `ImapSearcher`) each need a citation of the new file, or
the `ls-files` command and its empty result for absence. Pitfalls: convention bundles add dependencies that do not
appear in the module's own build file (koin, coroutines, datetime, TMC components); BOM-managed versions need the BOM
cited. Recount command for AC-4: the story's grep piped through `grep -v '/mail/transport/'` (expected 14).

**Q3 Licence.** Each source module is covered by the upstream `LICENSE`/`NOTICE` (cite both at the pin). Each binary
comes from its POM `<licenses>`, then the licence text where the POM is vague. jzlib's POM says only "BSD": open the
licence URL it names and identify the variant. Without that, the row reads "POM only, not opened" and **cannot be
"yes"**, which would block `go`. `httpcore5`, `httpcore5-h2` and `slf4j-api` were never opened by the notes (§8.2);
open them. MPL-2.0 (TMC `outcome`, `logging:core`): cite the MPL-2.0 text sections for file-level obligations
(§3.1-3.3, including the Larger Work clause) and give the compatibility verdict with that clause cited. The
private-app status affects when obligations trigger (distribution), not the compatibility verdict; say so.
Depth rules:
- **Parent POMs.** Where `<licenses>` is inherited (mime4j, commons-io, httpclient5 → `org.apache` parents), cite the
  POM that actually declares it.
- **Full transitive closure.** Recurse to closure, scope noted. Examples: okio → okio-jvm; koin-core and
  kotlinx-serialization-json → their `-core`/`-jvm` artifacts; httpclient5 → httpcore5, httpcore5-h2, slf4j-api.
- **Android variant.** If Section 4 keeps Android-variant consumption as a live mode, add the KMP `androidMain`
  dependencies (core-ktx, koin-android, coroutines-android).
- **Replacement NOTICE obligations.** Apache-2.0 §4(d): the upstream `NOTICE` text plus the `NOTICE` file of each
  Apache-licensed binary that ships one (at least commons-io, mime4j, httpclient5/httpcore5). Read these from the
  source tag or the jar's `META-INF/` (a jar read in scratch, never executed).
The NOTICE-defect finding: cite `<repo>/k9mail-vendored/NOTICE:20-21` against `<repo>/k9mail-vendored/build.gradle:16-19` and classify it as a
historical record or an error. Do not edit it.

**Q4 Platform.** Sources (notes §4): `build-plugin/plugin/src/main/kotlin/net/thunderbird/gradle/plugin/ProjectConfig.kt`
(minSdk, compileSdk, JVM), `gradle/libs.versions.toml` (Kotlin, coroutines, AGP, Gradle catalogue entry),
`gradle/wrapper/gradle-wrapper.properties`, `.java-version` and the JDK check in `settings.gradle.kts`, and the
components repository's `ProjectConfig.kt` for the published artifacts. Every upstream cell says which consumption mode
it applies to. The `:mail:*` modules are plain JVM (no minSdk); their KMP dependencies have Android targets at the
upstream minSdk. Whether that floor binds depends on source-vendoring versus binary consumption: state both and do
not resolve it.
"No minSdk" does not mean "runs at API 23". JVM modules get no Android lint, so check what Java APIs they call:
`git -C $TB grep -nE 'java\.time|java\.nio\.file|java\.util\.Optional|java\.util\.stream|(List|Map|Set)\.of\(|\.repeat\(|\.isBlank\(|\.strip\(' $PIN -- mail/common/src/main mail/protocols/imap/src/main`
plus the `commonMain`/`jvmMain` sets of the required KMP modules. Classify each hit by file language and receiver
before assigning an API level. In `.kt` files, `.repeat(`, `.isBlank(` and `listOf`-style calls on Kotlin types are
Kotlin stdlib and need no API level. JDK-only members (for example `String.strip()`, `java.nio.file.*` at API 26,
`java.time` at API 26) count. Report each counted hit with its API level, or whether core-library desugaring covers it,
in the minSdk row's source-vendoring mode.
minSdk verdict rule: "conflict" if any viable consumption mode conflicts, naming the mode. compileSdk: a conflict
reaches consumers through AAR `minCompileSdk` metadata only for Android-variant binaries; say which mode the row
assumes. Targets and app values come from Verified Facts. Apply D4.5, and mark each conflict "gating" or
"non-gating" with a one-line reason (Recommendation Logic). Every conflict lists options (for example
raise to 24 / keep 23 and vendor JVM-only sources / no-go) and "PO decision needed". Do not change minSdk 23.
Pitfall: Kotlin metadata direction. A 2.3.x compiler reading 2.4.x binaries is a conflict while the app stays on
2.3.21; REQ-015 moves it to latest stable (snapshot 2.4.21). Cite `<repo>/build.gradle` comments for the Hilt/2.4
coupling if used.

**Q5 API mapping.** For each row, read the app call site (Verified Facts) and the upstream declaration at the pin
(`mail/protocols/imap/src/main/java/com/fsck/k9/mail/store/imap/ImapFolder.kt`, `ImapStore.kt`, `RealImapFolder.kt`,
`RealImapStore.kt`, `UidSearchCommandBuilder.java`, `mail/common/.../FetchProfile.java`, `Message.java`,
`MimeMessage.java`, `MimeMessageHelper.java`, `internet/AddressHeaderBuilder.kt`). Java-callability: "no" needs the
signature (a `suspend` modifier, `internal` visibility, an interface where the app subclasses a class); "yes" needs the
public declaration; "via shim" names the shim (for example a Kotlin `runBlocking` wrapper) and cites the signature it
wraps. Kotlin `internal` reachable through name mangling counts as **forking/unsupported**, not "yes". Any
"needs fork" verdict must list the non-fork alternatives considered, each with its correctness cost measured against
AC-B4/B5 and EPIC success criterion 9. "Needs fork" is a reasoned conclusion, never a default. The three decisive
capabilities in story P4 (story Technical Context), each confirmed or refuted on both sides:

| Capability | Vendored side (cite) | Upstream test at the pin | Verdict rule |
|------------|----------------------|--------------------------|--------------|
| APPEND with explicit internal date | `<repo>/k9mail-vendored/.../imap/ImapFolder.java:1177`; app sets the date at `HeaderGenerator.java:56`; fork commit "Set internal date on APPEND" from I1.2's fork-only list (commit permalink). That list already shows the change never reached upstream, so no history pickaxe is run | `git grep -n 'APPEND' $PIN -- mail/protocols/imap/src/main`; read the command format and the `appendMessages` signature; `git grep -n internalDate $PIN -- mail/protocols/imap/src/main`; confirm no public API sends a raw command (the member list from the next row) | Refuted if the format carries a date-time argument fed from `internalDate`. Otherwise "needs fork of <files>", with the non-fork option "accept server-assigned internal date" and its cost (restore ordering, the AC-B4/B5 internal-date comparison, EPIC SC9) |
| UID SEARCH on an arbitrary header | `K9MailTransport.java:716-719`; the query at `:749-758` has four criteria: `HEADER X-SMSSYNC-DATATYPE "<type>"`, `UNDELETED`, `SENTSINCE <date>` and `FLAGGED`. The `max` trimming at `:727-744` does a DATE fetch, sorts by `getSentDate()`, then reverses. Vendored `ImapSearcher.java` and `executeSimpleCommand` visibility in vendored `ImapFolder.java` | List every member of the `ImapFolder`/`ImapStore` interfaces (read whole files); `git grep -nE 'HEADER|UNDELETED|SENTSINCE|FLAGGED|executeSimpleCommand|ImapSearcher' $PIN -- mail/protocols/imap/src/main`; visibility of `UidSearchCommandBuilder` and `executeSimpleCommand`; the flag terms the public `search(...)` emits; the upstream equivalent of `ImapMessage.getUid()` (used for handles, `:230`) | Refuted if any public, non-`internal` API accepts all four criteria (or arbitrary criteria or a raw command). Otherwise "needs fork". Each non-fork option is costed, and for each the analysis states how `since` (`SENTSINCE`), `flagged` (`FLAGGED`), deleted-message exclusion (`UNDELETED`) and the `max` trimming would be honoured. Options: the public `TEXT` search plus a client-side header, date and flag filter (needs a body or header fetch per candidate; superset results); per-datatype folders without a header search (changes the folder layout, EPIC SC9) |
| Header-only fetch (story P4 item 3: "`X-smssync-*` fetch") | Verified at planning: the vendored header-only fetches also omit `X-smssync-*` (ENVELOPE `ImapFolder.java:686-689`; DATE `:708-709`). The app reads `X-smssync-*` only after a **full-body** fetch (`K9MailTransport.java:256,299`, then `Headers.java:31`). The fork-only item actually used is `FetchProfile.Item.DATE`. It appears at three sites: `K9MailTransport.java:731` (live); `:258` (port `fetch(…, ENVELOPE_DATE)`, called only by tests); `BackupImapStore.java:181` (legacy). Every production use ends in `MessageComparator` → `getSentDate()` (`K9MailTransport.java:765-776`) | (i) Read the ENVELOPE `HEADER.FIELDS` list at the pin and check it contains `date`. (ii) Confirm at `<repo>` that all three `Item.DATE` sites end in `MessageComparator` via `getSentDate()`. A grep hit at `:258` or in `BackupImapStore.java` is not a refutation: `:258` is the CNTR-007 port path with no production caller, and `BackupImapStore` sorts the same way. Still, a migration must map the port's `FetchSpec.ENVELOPE_DATE` (CNTR-007 `:159`) to ENVELOPE. (iii) Confirm that upstream `MimeMessage.getSentDate()` parses the `Date` header (cite) | If (i)-(iii) hold: "achievable without forking: yes, ENVELOPE superset". The difference cell quantifies the extra fields fetched per 100-UID window (the other header fields, `INTERNALDATE`, `RFC822.SIZE`). Otherwise: "needs fork of <files>". Either way, record that story P4 item 3 mis-states the gap. The research notes already identify `Item.DATE` (notes §1 consequence and the §5 FETCH row), so the contradiction is with the story's summary, not the notes. A full-body fetch is **not** the substitute to evaluate |

Headers recount (AC-7): `grep -cE '"X-smssync-' app/src/main/java/com/zegoggles/smssync/mail/Headers.java` (expect 13),
and `grep -nE '"(References|Message-ID)"' app/src/main/java/com/zegoggles/smssync/mail/Headers.java` (expect 2).

MIME build sub-cells: multipart (`MimeMultipart`, `MimeMessageHelper.setBody`, boundary), text bodies (`TextBody`,
transfer encoding), base64 attachments (`Base64OutputStream`, `MimeBodyPart`), address and header encoding
(`setRecipient(s)` absence: `git grep -n 'setRecipient' $PIN -- mail/common/src/main`, `AddressHeaderBuilder`,
`MimeHeaderEncoder`). X-GM row: re-run the repo grep (Verified Facts) and `git grep -nE 'X-GM|XLIST|gmail' $PIN -- mail`,
stating the scope. Library bootstrap (`K9MailLib`, the `Log.logger` `lateinit` from notes §5) goes in the
connect/login row's difference cell.

**Q5.p Port preservation (CNTR-MODERNIZATION-007).** Rows are keyed to the code (`MailTransport.java` lines in
Verified Facts). Each row cites its CNTR-007 clause and records any contract-text drift in Notes. Known drift, to
report and not fix:
- `appendMessages` returns `long` (code `:87`) where the contract says `void`;
- the methods also throw `RequiresLoginException`, which is not a `MailException`;
- `importMessageBody`/`MessageImportResult` exist in code but not in the contract.

For each row:
1. **Backing call.** Cite the upstream declaration (store settings check; folder exists/create/open; append;
   search plus UID retrieval; fetch with a fetch profile; folder close). Record any `suspend` modifier or `internal`
   visibility, and whether a Java adapter can keep the port synchronous. If it can, the verdict is "preserved with
   adapter change", and the blocking moves inside the adapter.
2. **Throwables.** List what upstream declares or can throw, with FQNs at the pin (`MessagingException` and
   `AuthenticationFailedException` are expected to have moved package; Q2). Map each to an app-owned type.
3. **Discriminators** (exception-mapping sub-table):
   - (a) whether an XOAUTH2-specific failure type exists at the pin and exposes an HTTP-style status. The engine
     refreshes the token on `status == 400` (`BackupWorker.kt:442`, `RestoreWorker.kt:655`). If no status exists,
     state whether the adapter could derive one (cite the source it would parse) or that
     `XOAuth2FailedException.getStatus()` cannot be preserved;
   - (b) `git -C $TB grep -n 'Unable to get IMAP prefix' $PIN -- mail` (the `TemporaryImapException` discriminator);
   - (c) `git -C $TB grep -niE 'NONEXISTENT|did not find message count' $PIN -- mail/protocols/imap` (the BUG-011/013
     folder-open retry, `K9MailTransport.java:652-654`).
   A discriminator that is missing turns a typed error into the `MailException` backstop, which CNTR-007 `:336-337`
   calls a contract defect. The verdict is then "needs contract change (CR)" unless an adapter change restores it
   (state which).
4. **`openFolder` retry.** Cite the upstream equivalent of `ImapStore.closePooledConnections()` (used at
   `K9MailTransport.java:556-559`) and its visibility.
5. **`closeFolders`.** Cite the upstream folder `close()` signature and whether it throws. The adapter's
   `catch (Exception)` (`:445-454`) keeps the no-throw clause (`CNTR-007:365-367`) either way; record that.
6. **`fetch`.** `FetchSpec.ENVELOPE_DATE` maps to upstream `FetchProfile.Item.ENVELOPE` if `Item.DATE` is absent;
   `FetchSpec.BODY` maps to `Item.BODY`. The port is unchanged; only the adapter mapping changes.
7. **`importMessageBody`.** Name the upstream `Message` type that `MessageConverter.getDataType`/
   `messageToContentValues` (`K9MailTransport.java:304-305`) would have to accept, and cross-reference that file's
   Section 2 row. The port signature is unaffected.
8. **Constructor and `BinaryTempFileBody`.** Cite the upstream store factory or constructor parameters and the slot
   for the socket factory (CNTR-007 Clause C-1 `:242-244`; CNTR-001 `:284-285`). Cite the upstream equivalent of
   `BinaryTempFileBody.setTempDirectory` (`K9MailTransport.java:129`; CNTR-007 `:255`, Rule 5 `:356`).
9. **Package name.** If the replacement types live outside `com.fsck.k9`, CNTR-007 Validation Rule 1 (`:345-349`)
   and REQ-019 AC-B3 need the new package string. The port is preserved, but the contract text needs a CR: record
   that as a P item, not as a port break.

Verdict values are only "preserved", "preserved with adapter change" or "needs contract change (CR)", each with its
reason. The table states facts and verdicts. It proposes no new interface, signature or type: that would be Part B
design.

**Q6 Authentication.** `RealImapConnection.kt` `authenticate()` and its helpers, the `AuthType.kt` enum,
`mail/common/.../oauth/OAuth2TokenProvider.kt` (interface), and the implementation's location (`legacy/common/…`,
Android module). The app's current mechanism comes from `<repo>/.../preferences/AuthPreferences.java` and the
XOAUTH2 path in the vendored `ImapConnection.java` (REQ-019 Context cites about line 358). OAUTHBEARER has no
`AuthType` of its own: cite how it is negotiated.

**Q7 Trust, pinning, SNI.** Upstream `mail/common/.../ssl/TrustedSocketFactory.kt`, `TrustManagerFactory.java`
(hostname verifier source), `LocalKeyStore.kt` (semantics), `RealImapConnection.kt` (where `createSocket` is called;
whether the module verifies anything itself), `legacy/core/.../helper/DefaultTrustedSocketFactory.kt` (SNI; it is in an
Android app-core module) and `core/common/.../net/HostNameUtils.kt` (IP-literal test). For each of the 10 SNI tests,
evaluate the upstream guard by hand on the test's input, quoting the regex or condition lines, and mark it "code
reading". Blank and empty strings need explicit reasoning about what `isLegalIPAddress` returns and whether
`SNIHostName("")` would then be attempted. Status semantics (used in Sections 5 and 7):
- **supported**: the app supplies code it owns, including code moved from `:k9mail-vendored` (name the moved files),
  through a public interface at the pin.
- **needs fork of <files>**: a file **at the pin** must be changed or copied.
- **blocker**: no path.

A "needs fork" verdict lists the non-fork alternatives considered (Q5 rule).
**CNTR-MODERNIZATION-001 measurement (pinning row).** CNTR-001 fixes the carried type
`com.fsck.k9.mail.ssl.TrustedSocketFactory` with `createSocket(Socket, String, int, String) throws
NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException` (`CNTR-001:84-86`). Its two
permitted producers are `DefaultTrustedSocketFactory` and `PinnedCertificateSocketFactory` (`:93-96`). State three
things at the pin:
1. the upstream interface's FQN and exact signature, including the `MessagingException` package;
2. whether the upstream store factory or constructor still accepts it (Q5.p item 8);
3. whether both producers can still be supplied.
Any difference in the carried type, its signature or the slot sets P against CNTR-001 (`:284-285`). The
`org.apache.http.legacy` row:
`git grep -n 'org\.apache\.http\.' $PIN -- mail/common/src/main mail/protocols/imap/src/main` (trailing dot; `org.apache.hc.` is httpclient5, which is a different library), plus the POM check.

**Q8 Size.** Baseline: the I0.4 numbers (APK bytes and dex method count). Section 8 opens with: "Upstream was not
built (D2-c/d); the delta below is an estimate. Reason: …". Both deltas are labelled "estimate":
- **Bytes.**
  1. Bytes added = Σ `Content-Length` (HEAD request, or the jar sizes from (f)) of each new or changed runtime jar
     from Section 2, minus the jars removed or replaced.
  2. Retention factor k_b = the current mail stack's retained share at baseline.
  3. The source-module term: upstream `src/main` file and line counts for `:mail:common`, `:mail:protocols:imap` and
     the required KMP modules, set against the vendored module's counts and scaled by the vendored module's measured
     retained footprint.
  4. Δbytes ≈ k_b × (1) + (3).
- **Methods.** From the mapping-based baseline: methods retained per retained class (m) for the current mail stack.
  Estimate the classes added and removed (third-party: class entries in each jar, listed with `unzip -l` in scratch;
  source modules: declared top-level classes, counted with `git grep -cE '^(public |internal |private |protected )?(final |abstract |open |data |sealed |static )*(class|interface|fun interface|object|enum class|enum|record|@interface) [A-Z]'`
  at the pin, after checking the pattern on 5 sample files and recording the check). Δmethods ≈ m × (retained-class delta), using the class retention ratio observed at baseline.
Give a low/high range for each and show every input number. Section 8 never says "unknown". The residual uncertainty
goes into Sections 9 and 11 as a cost range or as a numbered condition ("measure on `assembleRelease` in the first
Part B story").
Verification metadata: for each coordinate,
`grep -c '<component group="G" name="A" version="V"' gradle/verification-metadata.xml`. A count of 0 is a new entry.
Report the count by artifact, and separately any Gradle plugin artifacts the consumption mode would add.

**Q9 Fallbacks.** Count files and work items: vendored files to patch; upstream files to copy; the fork-only commits
from I1.2 to re-apply; KMP modules to replace. The U-027 eviction comes from `<repo>/k9mail-vendored/NOTICE:25-30`.
Patch ownership per option. Upstream cadence and "legacy" classification (notes §9.2-9.3, `docs/architecture/…`) are
supply-chain evidence; cite at the pin.
**Support policy:** look for an explicit upstream statement on third-party reuse or API stability of the `mail:*`
modules. Run `git -C $TB grep -niE 'third.party|reuse|api stability|semver|as a library|publish' $PIN -- README.md CONTRIBUTING.md docs`,
read the ADR titles, and search GitHub issues (`repo:thunderbird/thunderbird-android "as a library"`, `"mail library"`,
`publish maven`). Cite what is found, or record "none found" with every search listed. A maintainer enquiry can be a
Part B condition; it is not sent in Part A.

**Q10 Separability.** RCS: what layer RCS data would enter (Android providers and converters versus the IMAP library);
cite app files and the absence of any coupling in the library API. Google OAuth: the `OAuth2TokenProvider` seam (Q6)
versus the app's own token acquisition. Evidence only. If either is "inseparable", record it for the product owner;
do not decide.

**Q11 Sizing.** Counts: `git ls-files app/src/main/java/com/zegoggles/smssync/mail | wc -l`, the test classes under
`app/src/test/.../mail`, the GreenMail tests, golden-master tooling (AC-B4), the live-account run (AC-B7) and the
coroutine/Kotlin adoption from Q5. Table rows only; no design and no files (IC-4). Order the rows relative to
REQ-015/016. Every row assumes CNTR-MODERNIZATION-007 and -001 unchanged. A row that needs a contract change is
labelled "conditional on CR (P-n)", depends on that CR's approval, and contains no interface design.

## Recommendation Logic (Section 12; apply mechanically, show each row)

Compute five predicates from the sections. B, F and G select **exactly one** row; S and P are annotations that never
select a row. The rows are mutually exclusive and cover every state of B, F and G.

- **B** (blocker): any Section 7 row has status "blocker".
- **F** (fork or licence): any of these holds:
  - a Section 3 row's verdict is not "yes" (including "POM only, not opened"); source: AC-14;
  - a Section 5 row is "needs fork of …" or reachable only through `internal`/mangled members; source: AC-14;
  - a Section 7 row is "needs fork of …"; this is a *plan interpretation*: a plain `go` cannot need a fork.
- **G** (gating conflict): any Section 4 conflict is marked **gating**, meaning Part B cannot start until it is
  resolved, under every viable consumption mode (plan interpretation). The compileSdk "not specified" verdict on its
  own is never gating (D4.5).
- **S** (separability): any Section 10 row is "inseparable". S is recorded for the PO and never selects a row.
- **P** (port): any Port preservation or exception-mapping row says "needs contract change (CR)", or the Section 7
  CNTR-001 measurement shows a change to the carried type, signature or slot. This includes Rule 1 package-string-only
  items (Q5.p item 9). P never selects a row and never decides the change: under REQ-019 Constraints
  (`REQ-MODERNIZATION-019…md:118`) and CNTR-007's breaking-change policy (`:331-341`), a port change is decided only by
  change record. Each P item (P-1, P-2, …) is listed in Section 12 as "PO decision needed", naming:
  - the affected clause (CNTR-007 or CNTR-001 line);
  - the finding;
  - the route: a change record against CNTR-MODERNIZATION-007 (and CNTR-MODERNIZATION-001 when the carried
    `TrustedSocketFactory` type or slot changes, `CNTR-001:284-285`), raised through the change-record skill by the
    Requirements Analyst or Artifact Librarian after acceptance (after U-062's QA) and before any Part B design.
  The executor creates no CR file. In a `go-with-conditions` outcome, each P item is also a numbered condition whose
  test is "CR-nnn approved". In a `go` outcome (T4 only), P items stay "PO decision needed" items, and Section 12 states
  that Part B cannot start the affected Section 11 rows before the CR is approved. In a `no-go` outcome they are
  recorded for completeness.

| Row | State | Permitted values | Source |
|-----|-------|------------------|--------|
| T1 | B | `go-with-conditions` only if every blocker has a numbered removal condition with a test; otherwise `no-go` | AC-14 / AC-9 ("not plain go"). The "otherwise no-go" part is a *plan interpretation* |
| T2 | not B, F | `go-with-conditions` or `no-go` | AC-14 (with the Section 7 fork clause a plan interpretation) |
| T3 | not B, not F, G | `go-with-conditions` (each gating conflict is a numbered condition) or `no-go` | Plan interpretation |
| T4 | not B, not F, not G | `go`, `go-with-conditions` or `no-go` | AC-14 |

In every row, non-gating Section 4 conflicts (including compileSdk "not specified"), any S finding and every P item are
listed in Section 12 as "PO decision needed" items, with their options (P items also with their CR route). Non-gating
conflicts and S are not conditions. Section 12 shows the five predicates with the findings that set them, names the selected row, and labels each plan-interpretation
clause as such so the product owner can see where the plan goes beyond AC-14. Where `go` is excluded, Section 12
states on which predicate.

**In every outcome**, Section 12 holds the option comparison, which picks the value among the permitted ones. The
options are `migrate` (per conditions, if any) and `stay on :k9mail-vendored` (Section 9). For each: counted cost,
supply-chain risk, security-patch owner, capability gaps closed or opened, and whether EPIC-MODERNIZATION-006 success
criteria 5 and 9 could be met. It states the trade-off: what is gained, what gives way, the alternative not chosen and
why. Conditions are numbered, each with a command or observable result as its test, and each is owned by a Part B story
row or a PO decision. For `go` or `go-with-conditions`, the line `AC-16 not applicable.` comes before the Recommendation
line.

**`no-go` branch (AC-16):** Section 12 contains a decision record: retain `:k9mail-vendored`; the authoritative copy
of the vendored source (this repo's tracked `k9mail-vendored/` at a named commit, plus where else it is mirrored, if
anywhere); what detects a lost upstream (for example a stated periodic check of the continuation repository and of
the vendored origin fork); what recreates it (from this repo's history; the NOTICE recipe). It also contains the
**text** of the two change records: (1) REQ-MODERNIZATION-019 Part B not applicable; (2) EPIC-MODERNIZATION-006
success criteria 5 and 9, each quoting the current text (`EPIC…:92-97`, `:107-109`) and the proposed revision. The
executor creates **no** change-record file. After acceptance, the Requirements Analyst or Artifact Librarian creates
them through the change-record skill and lists them in REQ-019 `change_records`.

The last line of Section 12 is exactly `Recommendation: go`, `Recommendation: go-with-conditions` or
`Recommendation: no-go`.

## Acceptance Sequencing (AC-15) and QA Timing

1. I0-I4 executor turns, each logged.
2. Code review and security review run in parallel on the analysis. They can check everything except AC-15 and the
   AC-16 records. **When it launches them without QA, the orchestrator writes the D4.7 deviation entry** ("QA deferred
   until after the AC-15 record; compiled pipeline runs it in the review group") to `implementation-log.md`. Findings
   lead to rework increments (`IR1`, …, logged like the others) and re-review of the changed sections.
3. **I5 (orchestrator):** once IC-1..IC-5 hold (I4 records, confirmed by the reviews), present the analysis to the
   product owner (summary, Section 12, open PO decisions from Sections 4, 5 (P items) and 10). Record in "Product owner decision":
   date, decision (`accepted` / `rejected` / `changes requested`), the **verbatim words**, the recorder (role:
   orchestrator or Artifact Librarian), and, separately, any direction on Part B. Accepting the analysis is distinct
   from agreeing with its recommendation; record both if both are given. Append further decisions below earlier ones;
   never overwrite.
   After appending, the orchestrator re-runs the IC-3 check over the analysis, because the verbatim words come from
   outside the pipeline. The presentation includes the P items (port and contract changes needing CRs) among the open
   PO decisions.
4. **At-acceptance edits (D4.6; only after an `accepted` record):** the Requirements Analyst or Artifact Librarian adds
   the analysis path to REQ-MODERNIZATION-019 and EPIC-MODERNIZATION-006 (story line 57). For `no-go` they also create
   the AC-16 change records through the change-record skill and list them in REQ-019 `change_records`. These are
   outside the implementer's diff. QA checks them against the D4.6 enumerated set. P-item CRs (against CNTR-007 or
   CNTR-001) are raised through the same skill after U-062's QA has completed and before any Part B design. They are
   Part B preconditions, so they are not part of U-062's diff or its IC-4 set. Separately, the orchestrator routes the pre-existing CNTR-007 text drift and DES-009's stale
   contract id (see Contracts Verification) to the Artifact Librarian. Those are outside this story.
5. On `changes requested`: rework increment(s) (`IR<n>`) on the named sections. Before re-presenting, re-run the I4
   end checks (citation check, IC-1, IC-3, IC-4; gates only if a tracked non-`sdlc/` file could have changed, which
   should never happen) and refresh the story's Implementation Notes, since AC-15 requires IC-1..IC-5 to hold before
   presentation. Then re-review, re-present, and append the new record. On `rejected`: the story stays open, and the
   orchestrator asks the PO what rework or disposition they want. Rework rounds count against quality-gate
   `max_iterations: 3`. At the limit, the orchestrator escalates to the product owner and does not close the story.
6. Only after an `accepted` record and step 4's edits, run QA. **QA is not run before the record (D4.7):** under the
   pipeline's closing rules (`on_any_fail → blocked`), a QA FAIL on AC-15 would push a waiting story into rework. While
   waiting, the story is `review` with `current_build_phase: quality-gate`, and the reviews' verdicts stand. If the
   sprint ends first, it carries over (sprint plan Notes). There is no deadlock: the PO record depends on the reviews,
   never on QA. The story cannot close without the record, because closing requires `qa-results.md`.
7. The orchestrator commits path-limited (`git add` of the analysis, the input copy, the story and the U-062 workspace
   files by name) after I4 and after the decision record. That makes the AC-15 `git log --diff-filter=A` check
   meaningful. At I5 it also tells the product owner that upstream was not built (D2), departing from the story's
   Estimation Guidance, so the acceptance is informed.

## Acceptance Criteria Mapping (verification table)

E = executor self-check, CR = code review, SR = security review, QA. Inc = where produced or checked.

| AC/IC | Check that proves it | Who | Inc |
|-------|----------------------|-----|-----|
| AC-1 | `grep -nE '^## [0-9]+\. ' $A` gives 12 lines, titles exact, in order; `grep -c PENDING $A` = 0; every "unknown"/"inconclusive" has an Experiment-log entry with 3 attempts | E, CR, QA | I0, I4 |
| AC-2 | Citation mechanical check (all outputs empty or matching); no row cites the input; sample opened | E, CR, QA (opens Sections 1, 3, 4, 6, 7 in full plus 10 random per other section) | every inc |
| AC-3 | Section 1 has 5 registry rows with URL, date and result, plus controls; yes/no line; signing and stability; verdict with quote; continuation repository; ahead/behind, merge base, compare URL (open it) | E, SR, QA | I1 |
| AC-4 | 14 rows; recount command and result; per-module dependency lists with versions; 4 legacy-library rows | E, CR, QA | I1 |
| AC-5 | One row per Section 2 component (cross-count); SPDX; yes/no; source column; MPL obligations; NOTICE list | E, SR, QA | I1 |
| AC-6 | 8 rows by 6 columns; open each REQ and app `file:line` (Verified Facts); D4.5 applied; "not specified" cells not "no conflict" | E, CR, QA | I2 |
| AC-7 | 8 rows, no blank cell, MIME build has 4 sub-cells; the 15-row header sub-table (13 + 2) with the Q5 recount commands and results; X-GM grep and scope; each yes/via shim/no backed as required; each "needs fork" lists non-fork alternatives. Additional check (the AC-7 row count is unchanged): the Port preservation sub-table has 9 rows and the exception-mapping sub-table 5 rows, with no blank cell, each verdict one of the three values, and contract drift noted | E, CR, SR (XOAUTH2 and auth rows), QA | I2 |
| AC-8 | Mechanism rows each with a citation; selection; interface versus implementation; ≤ 2-sentence consequence | E, QA | I2 |
| AC-9 | 4 rows with status; SNI table 10/10 with basis; pinning semantics stated; the pinning row's CNTR-001 measurement (FQN, signature, slot, both producers) | E, SR, QA | I2 |
| AC-10 | Baseline bytes and method count with tool and command (dexdump; apkanalyzer note); delta labelled "estimate" with method; verification-metadata count by artifact | E, CR (arithmetic), QA | I0, I2 |
| AC-11 | ≥ 3 options with counted cost, U-027 reference, patch owner | E, SR, QA | I3 |
| AC-12 | 2 rows, separable/inseparable, cited | E, QA | I3 |
| AC-13 | Sprint/epic statement with counts; breakdown acyclic (CR checks the dependency column forms a DAG); rows assume CNTR-007/-001 unchanged, and any row needing a change is labelled "conditional on CR (P-n)" with no interface design; no file created | E, CR, QA | I3 |
| AC-14 | `grep -nE '^Recommendation: (go\|go-with-conditions\|no-go)$' $A` gives exactly 1 line, the last non-blank line of Section 12; level-2 headings after Section 12 are exactly Citations, Experiment log, Product owner decision; predicates B/F/G/S/P shown, exactly one row T1-T4 selected (by B/F/G only), the value within that row's permitted set, plan-interpretation clauses labelled, every P item listed with its clause and CR route (and as a "CR-nnn approved" condition when `go-with-conditions`), the option comparison present; conditions numbered with tests; "AC-16 not applicable." precedes the Recommendation line when not no-go | E, CR, QA | I3, I4 |
| AC-15 | Appendix: date, decision, verbatim words, recorder; story not `done` without "accepted"; `git log --diff-filter=A --format=%ad --name-only` shows no Part B `sdlc/` file added before the acceptance date | Orchestrator; QA | I5 |
| AC-16 | If no-go: decision record with the 3 elements; CR files exist after acceptance, quote EPIC SC5/SC9, are listed in REQ-019 `change_records`. Else "AC-16 not applicable." | E (record text), RA/Librarian (files), QA | I3, after I5 |
| IC-1 | `git diff --stat $START -- app k9mail-vendored metadata gradle settings.gradle build.gradle gradle.properties gradlew gradlew.bat` empty; `git status --porcelain` (both forms) compared with I0, the delta limited to Affected Code plus D4.2 bookkeeping (QA also allows the D4.6 post-acceptance set) | E, CR, QA | I0, I4, after rework |
| IC-2 | Gate idiom exit 0 on JDK 25 (init-script JVM line), no `MSYS_NO_PATHCONV`; totals tests=682 skipped=2 failures=0 errors=0; `@Test` git-grep at START = 682 | E, QA | I0, I4 |
| IC-3 | See the IC-3 procedure below the table. Expected: 0 matches on the target files and 4 + 1 + 2 matches on the positive control | E, SR, QA; orchestrator after the decision record | every inc, I4, after rework, after I5 record |
| IC-4 | `git ls-files --others --exclude-standard` minus the I0 list, plus `git diff --name-status $START`: only the analysis, the input copy, the story and the D4.2 bookkeeping. QA, after acceptance, also allows the D4.6 enumerated set (the REQ-019 and EPIC-006 reference fields; CR files for `no-go` only). No `.gitmodules`, no clone or build output, no new `REQ-*`/`DES-*`/`CNTR-*`/`U-*` (`git ls-files --others --exclude-standard \| grep -E '/(REQ\|DES\|CNTR\|U)-[A-Z0-9-]+'` beyond U-062). Experiment log complete per experiment | E, CR, QA | I4, after rework |
| IC-5 | Banner line; `tail -n +2` hash = source hash (or diff limited to listed redactions); both hashes recorded | E, SR, QA | I0 |
| IC-6 | Baseline section before Section 1, every value with `file:line`, "682, not 697" statement | E, QA | I0 |

Note: inside the tables above, `\|` is Markdown escaping. When running a command copied from a table cell, use a
plain `|` (for example `grep -nE '^Recommendation: (go|go-with-conditions|no-go)$' "$A"`). The code blocks outside
tables are literal.

**IC-3 procedure.** Pattern files live only in scratch and are never quoted in a tracked file, because a recorded
literal would match itself.

- `$S/ic3-email.re`: the e-mail regex from story IC-3.
- `$S/ic3-paths.re`: the four user-profile patterns from story IC-3, copied **unchanged in their regex-escaped
  form**. Use them with `grep -E` (regex mode), where the story's doubled backslash matches a real single backslash.
- `$S/ic3-control.txt`: a positive control written in scratch with one synthetic line per path pattern (a drive-letter
  profile path with single backslashes, its POSIX `/c/` form, a line containing the local-application-data folder
  name, and the percent-variable), one synthetic e-mail address, and one line each containing the current hostname
  and user name. It is never copied anywhere. The lines are **disjoint by category**: each path line matches exactly
  one path pattern and contains no e-mail text, hostname or user name (use a placeholder profile name such as `x`).
  The e-mail line contains no path, hostname or user name. The hostname and user-name lines contain nothing else.

```bash
F="<every file in the IC-4 delta list>"
if [ -z "$(hostname)" ] || [ -z "$USERNAME" ]; then echo IC3-GUARD-FAIL; else
  echo "control: paths=$(grep -ciE -f $S/ic3-paths.re $S/ic3-control.txt) email=$(grep -cE -f $S/ic3-email.re $S/ic3-control.txt) host_user=$(grep -ciF -e "$(hostname)" -e "$USERNAME" $S/ic3-control.txt)"   # expect 4 1 2
  grep -nE  -f $S/ic3-email.re $F; grep -niE -f $S/ic3-paths.re $F; grep -niF -e "$(hostname)" -e "$USERNAME" $F
  # all three greps must print nothing
fi
```

A control count other than 4/1/2, or `IC3-GUARD-FAIL`, means the check is invalid. Fix it and re-run; never record
"0 matches" from an invalid run. The analysis records this command (with `<scratch>`), the control counts and
"0 matches". It describes the pattern files and never quotes them. A false positive on a short hostname is recorded
by file and line number only, never by value.

## What Each Reviewer Checks

- **Code Reviewer:** internal consistency (counts agree across sections); arithmetic recomputed (ahead/behind, size
  estimate, metadata count, sizing counts); every required table complete with the exact columns and row counts; the
  citation mechanical check re-run, plus spot-opens; the recommendation follows the predicates and exactly one row of
  T1-T4, with the trade-off stated and plan interpretations labelled; each "needs fork" lists its non-fork
  alternatives; Section 8 has both deltas labelled "estimate" and no "unknown"; the Port preservation and
  exception-mapping sub-tables are complete (9 + 5 rows) and consistent with the Section 5 operation rows and with P in
  Section 12, and they state facts and verdicts only (no proposed interface); the Section 11 breakdown is acyclic and
  prose-only, with CR-dependent rows labelled; quoted excerpts (none expected, since D2 runs no PoC) are correct and
  under the experiment rules; IC-1/IC-4 (no source file added, no experiment residue); no Part B design content beyond
  Section 11 rows. Review evidence: `review-code.md`.
- **Security Reviewer:** Section 7 in full (hostname verification source and the "library verifies nothing itself"
  consequence; pinning semantics versus the app's reject-all pin; the SNI blank/empty/IP cases; `org.apache.http.legacy`
  versus `org.apache.hc`; the CNTR-001 carried-type measurement); the exception-mapping sub-table's authentication
  rows, in particular the XOAUTH2 row (`XOAuth2FailedException.status == 400` → token refresh) and the
  `AuthenticationFailedException` → `RequiresLoginException` row, because a lost discriminator changes
  re-authentication behaviour; supply-chain reasoning in Sections 1, 3 and 9 (provenance, unsigned or snapshot
  dependencies, MPL obligations, the U-027 precedent, patch ownership); IC-3 re-run; rule 3 compliance
  (credential-free clone with `credential.helper` cleared, no upstream build logic executed; if (f) ran, its separate
  `GRADLE_USER_HOME`, stripped token variables and recorded Gradle/JDK; confirm from the Experiment log). Review
  evidence: `review-security.md`.
- **QA Analyst (after the AC-15 record):** every AC/IC per the table; opens all citations in Sections 1, 3, 4, 6 and 7
  and in Section 5's Port preservation and exception-mapping sub-tables, and 10 random per other section (all, if
  fewer), recording URL, result and opener; a citation that does not open
  fails AC-2; IC-1..IC-6 commands re-run; the story is not `done` without "accepted".

## Files Modified/Created

| File | Action | By |
|------|--------|----|
| `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` | Create (I0), fill I1-I4 | Executor; "Product owner decision" by orchestrator/Librarian |
| `sdlc/artifacts/build/feasibility/U-062/inputs/thunderbird-mail-libraries-research-2026-11-12.md` | Create (I0) | Executor |
| `sdlc/artifacts/build/sprints/sprint-019/U-062/implementation-log.md` | Create (I0), append | Executor |
| `sdlc/artifacts/build/sprints/sprint-019/U-062-thunderbird-mail-libraries-feasibility-analysis.md` | Implementation Notes (I4), Review Findings | Executor / orchestrator |
| `sdlc/artifacts/build/change-records/CR-005-*.md` (+1) | no-go only, after acceptance | Requirements Analyst / Artifact Librarian |
| REQ-MODERNIZATION-019, EPIC-MODERNIZATION-006 (reference fields only) | After acceptance (D4.6) | Requirements Analyst / Artifact Librarian |

No product, build, Gradle or `k9mail-vendored` file is touched. Read-only: everything else.

## Contracts Verification

`integration_contracts: []`. The story creates and edits no interface; no contract is crossed by the work. Two
approved contracts are **measured** (read-only):

- **CNTR-MODERNIZATION-007** (`MailTransport`). The interface is at `:82-132`, the producer clauses at `:68-73`, the
  exception-translation table at `:209-224`, Clause C-1 at `:242-244`, the breaking-change policy at `:331-341`, and
  Validation Rules at `:345-356`. Q5.p measures every implemented port member and every translation row against
  upstream at the pin (Port preservation and exception-mapping sub-tables).
- **CNTR-MODERNIZATION-001** (TLS socket-factory handoff: carried type `:84-86`, producers `:93-96`, breaking change
  `:284-285`). The Section 7 pinning row measures it (Q7).

REQ-019 Constraints (`:118`) make preserving the port the default: a change happens only if Part A shows it cannot be
preserved, and then through a change record. The analysis therefore records any such finding as predicate P, a
"PO decision needed" item naming the CR route against CNTR-007 (and CNTR-001), and never decides it. Section 11 rows
assume both contracts unchanged and label CR-dependent rows. The executor creates no CR.

Pre-existing drift, reported by the analysis (Port preservation Notes and a "Follow-ups" list in Section 12) and not
fixed by U-062:
- (i) CNTR-007's text lags the code: `appendMessages` returns `long` (`MailTransport.java:87`) where the contract
  says `void` (`CNTR-007:109`); the methods also throw `RequiresLoginException`; `importMessageBody`/
  `MessageImportResult` are not in the contract.
- (ii) DES-MODERNIZATION-009 names a stale contract id (`CNTR-MODERNIZATION-009-mailtransport`, `DES-009:395`) and
  stale counts ("19 files / 62 occurrences").

The orchestrator routes (i) and (ii) to the Artifact Librarian separately, outside this story. ADR-009-A's
"replace-coordinate preferred" ordering (`DES-009:196-226`) is the yardstick that Sections 1 and 9 report against.

## Risks & Mitigations

| # | Risk | Mitigation |
|---|------|------------|
| 1 | Upstream `main` moves during the work | D1: pin once and record the drift list |
| 2 | Web tool truncates or refuses large pages | Raw URLs at the SHA; local clone; registry HEAD requests |
| 3 | GitHub API rate limits (60/h unauthenticated) | Clone for all git facts; the API only for repository ID, packages and redirect |
| 4 | A finding contradicts the research input or the story's summary of it (story P4 item 3 is already known to mis-state the gap) | The primary source wins. Record the contradiction against whichever is wrong (input §n, or story P4) in the log and the analysis |
| 5 | Answering from memory | AC-2: no ID, no claim; the reviewer counting rule; the input is never cited |
| 6 | Scope creep into Part B design, including deciding a port change | Section 11 table rows only, CR-dependent rows labelled; the Port preservation table gives verdicts, not interfaces; port changes are routed via P and change records; code review flags design prose |
| 7 | IC-3 self-match or leakage (the pattern literal, SSH clone URL, `git log` e-mails, scratch paths) | Pattern files in scratch, HTTPS, `--format`, placeholders, empty-variable guard |
| 8 | Partial clone is slow or fails on Windows long paths | `core.longpaths`; stop rule with raw-URL reading as a valid fallback |
| 9 | `dexdump -f` does not print header counts | The 3-attempt ladder in I0.4 |
| 10 | Session interruption loses work | The step-level log and resume rule; the analysis written incrementally |
| 11 | PO decision outlasts the sprint | Story stays `review`; carry-over; QA deferred (no deadlock) |

## Quality Attribute Coverage (ISO/IEC 25010)

The deliverable is a document, so these characteristics apply to what the analysis must evaluate for Part B and to
the analysis's own fitness.
- **Functional Suitability:** Section 5 (capability completeness: APPEND date, header search, date-only fetch), with
  AC-B4/B5 behaviour preservation as the yardstick.
- **Performance Efficiency:** the cost of an ENVELOPE fetch versus a date-only fetch (Q5 row 5); APK size and method
  count (Section 8).
- **Compatibility:** Section 4 (minSdk, Kotlin metadata, coroutines), coexistence with Hilt/Kotlin 2.4 coupling, and
  CNTR-007 port stability.
- **Usability:** not applicable (no UI change); the analysis's own readability is served by the fixed skeleton.
- **Reliability:** `suspend` adoption on worker threads, and the `Log.logger` `lateinit` bootstrap failure mode (Q5).
- **Security:** Section 7 (hostname verification, pinning semantics, the SNI guard), supply chain (Sections 1, 3, 9),
  and evidence hygiene (IC-3).
- **Maintainability:** upstream "legacy" classification, churn and `@Discouraged` markers, and fork re-application cost
  (Section 9).
- **Portability:** the build portability of upstream modules outside their tree (D2-c becomes a condition), and JDK 25
  as the build JDK.

## Test Strategy

There is no runtime behaviour change and no test is added, changed or deleted. Build verification is the end gate set
only (IC-2, run at I0 for the baseline and at I4): `./gradlew :app:assembleDebug :app:testDebugUnitTest
:app:jacocoTestCoverageVerification :app:assembleRelease` with exit 0, 682 run, 2 skipped. The analysis is verified by
the mechanical checks above (heading, citation, recommendation and hygiene greps) and by opening citations. SNI and
TLS results for upstream are code-reading results, labelled as such. No proof-of-concept is compiled (D2).

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-062 story | `sdlc/artifacts/build/sprints/sprint-019/U-062-thunderbird-mail-libraries-feasibility-analysis.md` | ACs, ICs, rules |
| Sprint plan | `sdlc/artifacts/build/sprints/sprint-019/sprint-plan.md` | Sequencing, acceptance note |
| Compiled pipeline | `sdlc/artifacts/build/sprints/sprint-019/U-062/compiled-pipeline.json` | Gates; QA in the review group; closing rules |
| REQ-MODERNIZATION-019 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-019-replace-vendored-k9-with-thunderbird-mail-libraries.md` | Part A questions, AC-A1..A9 |
| EPIC-MODERNIZATION-006 | `sdlc/artifacts/requirements/modernization/EPIC-MODERNIZATION-006-dependency-currency-latest-stable.md` | SC5 `:92-97`, SC9 `:107-109`, Out of Scope |
| REQ-MODERNIZATION-015/016/017 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-01{5,6,7}-*.md` | AC-6 targets (lines in Verified Facts) |
| DES-MODERNIZATION-009 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-009-mail-acl-k9-unpin-design.md` | ACL, ADR-009-A/B |
| CNTR-MODERNIZATION-007 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md` | Port, exception mapping, C-1, rules, breaking-change policy (measured) |
| CNTR-MODERNIZATION-001 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md` | Carried type, producers, breaking change (`:84-86`, `:93-96`, `:284-285`) |
| Gate results | `sdlc/artifacts/build/sprints/sprint-019/U-062/{plan-review,contract-verification}.md` | Rework inputs |
| Port and engine sources | `mail/transport/{MailTransport,K9MailTransport,FetchSpec}.java`; `service/{BackupWorker,RestoreWorker}.kt`; vendored `store/imap/{ImapConnection,ImapStore,XOAuth2AuthenticationFailedException}.java` | Verified Facts (port rows) |
| U-061 plan, log, QA | `sdlc/artifacts/build/sprints/sprint-018/U-061/{plan,implementation-log,qa-results}.md` | Init script, dexdump, totals method |
| Research input | the orchestrator's session scratchpad, `thunderbird-mail-libs-research.md` (read in full) | Hints only |
| Source | `settings.gradle`, `build.gradle`, `app/build.gradle`, `gradle/gradle-daemon-jvm.properties`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/verification-metadata.xml` (format), `k9mail-vendored/{build.gradle,NOTICE}`, vendored `store/imap/ImapFolder.java`, `ssl/DefaultTrustedSocketFactory.java`, `ssl/TrustManagerFactory.java`, `FetchProfile.java`; app `mail/{Headers,BackupImapStore,HeaderGenerator,MessageGenerator,PinnedCertificateSocketFactory}.java`, `mail/transport/K9MailTransport.java`; `DefaultTrustedSocketFactorySniTest.java` | Verified Facts |
| Templates/standards | plugin cache `templates/build-artifacts/{build-plan,gate-report-template}.md`, `knowledge/standards/{expert-exceeding-depth,phase-completion-report}.md` | Format |

## Verdict: PASS

The story is plannable as written. All twelve questions, the experiment rules, the AC-15/16 ownership and the
no-product-edit constraint are covered. Every repo fact used was verified at `c8f04558`. Upstream facts are directed
for verification, not asserted. The story-text discrepancies and the QA-timing deviation (D4.1-D4.7) have recorded
interpretations. D4.6 is to be raised at the sprint retro. Iteration 2 resolves plan-review B1-B4:
- the decision table is total and mutually exclusive, and does not pre-decide;
- the third capability test is decisive (ENVELOPE superset);
- IC-3 has a positive control and a fail-closed guard;
- the at-acceptance edits are an enumerated, owned set.

Iteration 3 resolves contract-verification B1-B2:
- every implemented `MailTransport` member, the constructor, `BinaryTempFileBody` and every CNTR-007 translation row
  get a cited preservation verdict (Q5.p);
- a "cannot be preserved" finding becomes predicate P, a non-selecting "PO decision needed" item routed to a change
  record against CNTR-007 or CNTR-001, never decided by the analysis;
- Section 11 rows assume the contracts unchanged.

The recommendation rows T1-T4 remain total and mutually exclusive over B, F and G.

Story P4 item 3 mis-states the gap as an "X-smssync fetch" gap. It is the `FetchProfile.Item.DATE` gap, which the
research notes already identify. The executor confirms this at the pin.

## Phase Completion Report
---
story_id: "U-062"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-019/U-062/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
