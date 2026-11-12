---
status: approved
type: functional
artifact_type: requirement
related_requirements:
  - REQ-MODERNIZATION-002
  - REQ-MODERNIZATION-009
  - REQ-MODERNIZATION-012
  - REQ-MODERNIZATION-015
  - REQ-MODERNIZATION-016
related_stories: []
related_design_docs:
  - DES-MODERNIZATION-009
traces_to:
  - EPIC-MODERNIZATION-006
change_records: []
id: REQ-MODERNIZATION-019
title: Replace the Vendored K-9 Mail Library with Thunderbird for Android Mail Libraries (Gated Feasibility Analysis, Then Migration)
domain: modernization
epic: EPIC-MODERNIZATION-006
priority: high
updated_at: '2026-11-12T16:49:50.944Z'
---

# REQ-MODERNIZATION-019: Replace the Vendored K-9 Mail Library with Thunderbird for Android Mail Libraries

## Description

Replace the in-tree `:k9mail-vendored` module with Thunderbird for Android's current mail libraries, behind the existing `MailTransport` anti-corruption layer (ACL), without changing what the app backs up or how it restores it. The work is structured in two gated parts. **Part A** is a written feasibility analysis that must be accepted before any design or build story for the migration exists. **Part B** is the migration itself and is conditional on Part A ending in "go" or "go-with-conditions". The product owner's direction (verbatim): "include the thunderbird migration in scope". This requirement implements requirement 5 of EPIC-MODERNIZATION-006.

## Context (verified repo facts, 2026-11-12)

- `settings.gradle` line 1 is `include ':app', ':metadata', ':k9mail-vendored'`.
- `k9mail-vendored/NOTICE` records the origin: fork `https://github.com/jberkel/k-9`, commit `eaf689025ec28d55e3b62f5ea3b1f185f410ee6b`, branch `sms-backup-plus-1.5.11`, captured 2026-06-03 by U-027 after the JitPack artifact was evicted (HTTP 404). Upstream licence is Apache-2.0 (K-9 Mail Developers; The Android Open Source Project), the same licence family as this app (`COPYING`, `README.md` line 477).
- `k9mail-vendored/build.gradle`: `sourceCompatibility`/`targetCompatibility` Java 8; `minSdkVersion 21`; `compileSdkVersion 35`; dependencies `apache-mime4j-core` and `-dom` 0.8.11 (`api`), `commons-io` 2.16.1 (`api`), `jzlib` 1.0.7 and `jutf7` 1.0.0 (`implementation`), `androidx.annotation` 1.7.1 (`api`). The `webdav/` package is excluded from compilation (U-058). **Documentation defect found:** `k9mail-vendored/NOTICE` still states mime4j 0.7.2 and commons-io 2.4, but `build.gradle` uses 0.8.11 and 2.16.1; the NOTICE must be reconciled by Part B and the discrepancy reported in Part A.
- `com.fsck.k9` is referenced in 14 files under `app/src/main/java` outside `mail/transport/` (the grep also finds 9 files inside `mail/transport/`, which is the permitted adapter zone). `grep -rn "com\.fsck\.k9\|com\.android\.email" app/src/main/java/com/zegoggles/smssync/service/` returns zero matches, so the REQ-MODERNIZATION-012 engine seal holds; the leak is in `mail/` and three root-level files. Classification:

| File (under `app/src/main/java/com/zegoggles/smssync/`) | k9 types used | Class |
|---|---|---|
| `mail/MessageGenerator.java` | Address, BodyPart, Message, MessagingException, MimeMessage, MimeMultipart, TextBody, `MimeMessageHelper.setBody` | Message/MIME model |
| `mail/MessageConverter.java` | Address, Flag, Message, MessagingException, MimeUtility | Message/MIME model |
| `mail/Attachment.java` | Body, MessagingException, `filter.Base64OutputStream`, MimeBodyPart, MimeHeader | Message/MIME model |
| `mail/MmsSupport.java` | Address, BodyPart, MessagingException, MimeBodyPart, TextBody | Message/MIME model |
| `mail/HeaderGenerator.java` | Message, MessagingException | Message/MIME model |
| `mail/Headers.java` | Message | Message/MIME model |
| `mail/ConversionResult.java` | Message | Message/MIME model |
| `mail/PersonRecord.java` | Address | Message/MIME model |
| `mail/BackupImapStore.java` | FetchProfile, Folder, Folder.FolderType, Message, MessagingException, `ssl.TrustedSocketFactory`, ImapFolder, ImapMessage, ImapResponse, ImapSearcher, ImapStore | Transport |
| `mail/BackupStoreConfig.java` | `store.StoreConfig`, NetworkType | Transport (config) |
| `mail/PinnedCertificateSocketFactory.java` | MessagingException, `ssl.TrustedSocketFactory` | Transport (TLS) |
| `App.java` | `K9MailLib` (bootstrap) | Transport (library init) |
| `preferences/AuthPreferences.java` | AuthType | Transport (auth config) |
| `di/MailModule.kt` | MessagingException | Transport (exception handling) |

Eight files use the message/MIME model, six use transport. REQ-MODERNIZATION-012 AC-8/Constraints carve out `App.java` and `AuthPreferences.java` as documented residuals of REQ-MODERNIZATION-009; this requirement removes those carve-outs.
- The security changes in the vendored copy that must survive: TLS hardening (REQ-MODERNIZATION-002) in `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/` (`DefaultTrustedSocketFactory`, `TrustManagerFactory` with `HttpsURLConnection.getDefaultHostnameVerifier()` plus a `MinimalSslSession` shim, `LocalKeyStore`, `KeyChainKeyManager`); certificate pinning in `app/.../mail/PinnedCertificateSocketFactory.java`; SNI via `SSLParameters`/`SNIHostName` with an IP-literal and blank-hostname guard (BUG-012, BUG-015; tests in `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java`); removal of `org.apache.http.legacy` (U-058).
- Operations the app drives through the library (verified in code): `UID SEARCH` (`BackupImapStore.java`, `K9MailTransport.java`), `BODY.PEEK[HEADER.FIELDS ...]` and `BODY.PEEK[]` fetches and `INTERNALDATE` handling (`ImapFolder.java`), APPEND with flags and internal date (`ImapFolder.java` ~line 1174-1178), XOAUTH2 (`ImapConnection.java` ~line 358), and the 13 `X-smssync-*` headers defined in `mail/Headers.java`. A grep for `X-GM` found no match in `app/src/main/java` or the vendored IMAP package, so Gmail extension use appears to be limited to folder-as-label naming; Part A must confirm this.
- Baseline: 697 unit tests, 2 skipped, including the GreenMail IMAP integration tests (`app/src/test/java/com/zegoggles/smssync/mail/transport/BackupImapStoreDelegateIntegrationTest.java`); build on JDK 25; dependency verification enforced; `mail*` and `mail.transport` JaCoCo rules at 0.70.
- Unverified background (product owner recollection, to be tested by Part A, not assumed): "Thunderbird bought the old K-9 app and its libraries."

## Requirements

### Part A: Feasibility Analysis (gate)

A single written analysis artifact, reviewed and accepted before any design or build story for Part B is created. Every factual claim about Thunderbird must cite a primary source (repository URL and commit or tag, published POM, or licence file) with the retrieval date; a claim without a citation is rejected. The analysis must answer:

1. **Consumability.** Are the libraries published as Maven artifacts (coordinates, repository, versions, signing, whether a stable non-snapshot release exists), or only buildable from the thunderbird-android source tree? Is the "Thunderbird acquired K-9" recollection correct, and which repository is the continuation of the code this app vendors?
2. **Modules and dependency graph.** Which modules would replace the 8 message/MIME-model and 6 transport usages tabled above, and what does each depend on transitively (replacing commons-io, mime4j, jzlib, jutf7, or not)?
3. **Licence.** Per module and transitive dependency: licence identifier, compatibility with this app's Apache-2.0 licence, and the NOTICE/attribution obligations that `k9mail-vendored/NOTICE` would be replaced by.
4. **Platform requirements.** minSdk, Kotlin, coroutines, AGP, and JVM-target requirements, and any conflict with REQ-MODERNIZATION-015 (minSdk 23) and REQ-MODERNIZATION-016 (runtime libraries, including coroutines), with a statement of the resolution.
5. **API mapping.** For each operation: connect/login; folder create/select; APPEND with flags and internal date; UID SEARCH by header; FETCH BODY.PEEK of headers and full body; X-GM-EXT-1/label handling (confirming or refuting the "folder naming only" finding above); MIME build (multipart, text bodies, base64 attachments, address and header encoding) and parse. State the replacement call, the behavioural difference, and whether the library is Java-callable from this Java code base or forces Kotlin and coroutine adoption.
6. **Authentication.** Which of PLAIN/LOGIN, XOAUTH2, and OAUTHBEARER are supported, and what this means for the later Google OAuth goal.
7. **Custom trust, pinning, and SNI.** Exactly how a custom socket factory or trust manager, the pinned-certificate store, hostname verification, and SNI with the IP-literal guard can be supplied; if they cannot, which code must be forked.
8. **Size and method count.** Release-APK size and method-count delta, measured on `assembleRelease` or estimated with the method stated.
9. **Fallback options if not consumable:** vendor a newer snapshot, depend on a fork, or stay on `:k9mail-vendored` and record the decision, each with cost, supply-chain risk (the U-027 eviction precedent), and security-patch ownership.
10. **Separability.** An explicit statement of whether RCS support and Google OAuth sign-in are separable from this migration or inseparable, with evidence; if inseparable, the dependency is recorded for the product owner's decision (per EPIC-MODERNIZATION-006 Out of Scope).
11. **Sizing and split.** Whether Part B fits one sprint or must become its own epic, with a proposed story breakdown and ordering relative to REQ-MODERNIZATION-015/016.
12. **Recommendation.** One of **go**, **go-with-conditions** (conditions enumerated and testable), or **no-go** (with the decision recorded and `:k9mail-vendored` retained).

### Part B: Migration (conditional on Part A acceptance with go / go-with-conditions)

- Remove `:k9mail-vendored` from `settings.gradle` and delete `k9mail-vendored/`; absent from the resolved runtime classpath: commons-io, apache-mime4j, jzlib, jutf7 (unless Part A records a specific retained exception).
- No `com.fsck.k9` type (or the Thunderbird library's equivalent) appears outside `com.zegoggles.smssync.mail.transport`; message/MIME-model usages are moved behind app-owned types or into the adapter.
- Behaviour is preserved: backed-up messages are byte-comparable before and after (headers including all `X-smssync-*` headers, MIME structure, transfer encodings, flags, internal date, folder/label naming); restore reads messages written by both the old and the new stack; the TLS, pinning, and SNI protections are preserved with tests; no new ERROR-level log output.

## Acceptance Criteria

### Part A

- **AC-A1**: The analysis exists at a workspace path recorded in the story that produces it, and contains a numbered section answering each of questions 1-12 above. A question answered "unknown" is a failed criterion unless it states the experiment attempted and why it was inconclusive.
- **AC-A2**: Every factual statement about Thunderbird libraries (publication, module names, licence, minSdk/Kotlin/coroutine/AGP requirements, authentication mechanisms, API) carries a citation with URL, commit/tag/version, and retrieval date; a reviewer can open each citation.
- **AC-A3**: Question 3 lists, for every module and transitive dependency proposed, an SPDX licence identifier and a yes/no Apache-2.0 compatibility verdict; any non-"yes" item blocks a "go" recommendation.
- **AC-A4**: Question 4 states the exact minSdk, Kotlin, coroutines, and AGP versions required, in a table beside the versions that REQ-MODERNIZATION-015 and -016 will set, with an explicit "conflict / no conflict" verdict per row.
- **AC-A5**: Question 5 contains a table with one row for each of the seven operations listed (connect/login, folder create/select, APPEND with flags and internal date, UID SEARCH by header, FETCH BODY.PEEK, X-GM-EXT-1/labels, MIME build/parse); no row is blank.
- **AC-A6**: Question 7 names, for each of the four protections (TLS hostname verification, certificate pinning, SNI with IP-literal guard, `org.apache.http.legacy` independence), the extension point and a proof-of-concept or code reference; a protection with no supported path is listed as a blocker.
- **AC-A7**: The analysis ends with exactly one of the three recommendation values and a statement of RCS and OAuth separability (separable / inseparable, with evidence).
- **AC-A8**: The product owner accepts the analysis in writing (recorded in the artifact or the sprint record); no design or build story for Part B exists before that date. Verifiable by comparing artifact creation dates.
- **AC-A9**: If the recommendation is **no-go**, the decision is recorded (including supply-chain mitigation for `:k9mail-vendored`), Part B is marked not applicable, and EPIC-MODERNIZATION-006 success criteria 5 and 9 are revised by change record.

### Part B (applies only after AC-A8 and a go / go-with-conditions recommendation)

- **AC-B1**: `grep -n "k9mail-vendored" settings.gradle app/build.gradle` returns zero matches and the `k9mail-vendored/` directory does not exist.
- **AC-B2**: `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` shows none of `commons-io`, `org.apache.james:apache-mime4j-*`, `com.jcraft:jzlib`, `com.beetstra.jutf7` unless Part A recorded each as a retained exception.
- **AC-B3**: `grep -rln "com\.fsck\.k9" app/src/main/java` lists only files under `app/src/main/java/com/zegoggles/smssync/mail/transport/` (or, if Part A shows the package name changed, the equivalent new package string appears only there). This also removes the `App.java`, `AuthPreferences.java`, and `MailModule.kt` references.
- **AC-B4**: A golden-master test builds identical SMS, MMS (with attachments), and call-log inputs through the pre-migration code (fixtures captured from the commit before migration) and the new code, and asserts the serialised RFC 5322 bytes are equal, except for fields explicitly allow-listed in the test with a stated reason. Compared fields include every header in `mail/Headers.java`, boundary-independent MIME structure, `Content-Transfer-Encoding`, flags (`\Seen`), internal date, and folder/label name.
- **AC-B5**: A GreenMail test appends a message through the new stack and reads it back with unchanged headers, flags, and internal date; the existing GreenMail integration tests pass without assertion changes.
- **AC-B6**: A restore test reads at least one fixture message written by the old stack (committed under `app/src/test/resources`) and one written by the new stack, and produces identical restored SMS/MMS/call-log content for both.
- **AC-B7**: A manual run against a live Gmail account (credentials supplied by the product owner, not stored in the repository) backs up at least 10 SMS and 2 MMS messages and restores them to a device or emulator; the evidence (message counts, a header diff of one message from each stack) is attached to the story. If no account is available, the story is blocked, not closed.
- **AC-B8**: Tests preserve the TLS protections: a test rejects a certificate whose hostname does not match, a test accepts a pinned certificate and rejects a different one, `DefaultTrustedSocketFactorySniTest`-equivalent cases pass (SNI set for a DNS name; not set for an IPv4 literal, an IPv6 literal, and a blank host), and `grep -rn "org.apache.http" app/src/main` returns zero matches.
- **AC-B9**: `./gradlew :app:testDebugUnitTest` has zero failures; the skipped count is no more than 2; the executed count is at least 697 minus tests deleted with the vendored module, each deletion listed with its replacement.
- **AC-B10**: `./gradlew :app:jacocoTestCoverageVerification` passes with no per-package floor lowered (`mail*` and `mail.transport` remain at 0.70); `gradle/verification-metadata.xml` contains an entry for every new artifact and dependency verification is not disabled.
- **AC-B11**: `./gradlew :app:assembleDebug` succeeds on JDK 25; a logcat capture of one full backup and restore on the test device contains no `E/` entries tagged by this app that are absent from the pre-migration capture.
- **AC-B12**: A NOTICE/attribution file replaces `k9mail-vendored/NOTICE` and satisfies the obligations identified in AC-A3.

## Constraints

- JDK 25 build, enforced dependency verification, and no lowered JaCoCo floor are binding (EPIC-MODERNIZATION-006 Out of Scope).
- The `MailTransport` port contract (REQ-MODERNIZATION-012 AC-8) is changed only if Part A shows it cannot be preserved; any change goes through a change record.
- RCS and Google OAuth are Out of Scope unless AC-A7 records them as inseparable, in which case the product owner decides before design.
- Part A requires web and repository access; the author of this requirement had none, so no statement about Thunderbird is made as fact here.

## Scope Concern

Part B may exceed a single sprint (14 files plus tests, golden-master tooling, a live-account run, and a possible Kotlin/coroutine adoption) and may need to become its own epic once Part A reports (Part A question 11). This is flagged, not acted on; EPIC-MODERNIZATION-006 keeps requirement 5 in scope as approved.

## Dependencies and Sequencing

Part A can run in parallel with REQ-MODERNIZATION-015/016 but its findings on Kotlin/coroutines must be reconciled with them. Part B starts only after AC-A8 and, if Part A so concludes, after the toolchain versions it needs are in place.

## Traceability

EPIC-MODERNIZATION-006; REQ-MODERNIZATION-002 (TLS), -009 and -012 (ACL), DES-MODERNIZATION-009; `sdlc/artifacts/build/sprints/sprint-017/retro.md`.
