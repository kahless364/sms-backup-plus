---
artifact_type: review-security
story_id: "U-062"
verdict: "PASS"
iteration: 3
agent: "Security Reviewer"
timestamp: "2026-11-13T19:50:00Z"
blockers: 0
warnings: 7
---

# Security Review: U-062

## Review Summary

U-062 is a no-code feasibility analysis, so this review covers three things: whether the security claims in the analysis (Sections 6, 7, 9, 12) are true when read against this repository and against upstream at the pin, how the research was carried out, and the hygiene of what the story added to the tree. I found no Critical or High issue. Every claim I re-opened in Section 7 holds. The four protections are supported through the public `TrustedSocketFactory` interface with app-owned code, and I found no hidden fork and no upstream path that creates a socket without that factory. I also found no secret, e-mail address, machine name, user name or user-profile path in any committed or modified file. The warnings below are gaps in what the analysis tells the product owner. None of them changes a Section 7 status.

## Verdict: PASS

Basis: no Critical or High finding; no Section 7 status is wrong; no hygiene hit; the research stayed inside the experiment rules (details below). Seven Medium/Low warnings, all non-blocking, all classified.

## Method and evidence base

- Read the story file (AC-8, AC-9, AC-11, experiment rules, IC-1 to IC-6), `plan.md` (Decisions, Environment and Conventions, Recommendation Logic, "What Each Reviewer Checks"), all `implementation-log.md` entries, and analysis Sections 1, 3, 6, 7, 9, 12 and the Experiment log in full. I skimmed the rest.
- Security-relevant REQ artifacts: the story's governing requirement is REQ-MODERNIZATION-019 Part A. The TLS contract in play is CNTR-MODERNIZATION-001, which I read at lines 80-102 and compared with the analysis's measurement. Nothing in the analysis violates REQ-MODERNIZATION-002's protections, because no code is changed.
- Upstream was read from the read-only clones at the pin (`<scratch>/tb` at `ac1f0e85f63c1b600ac443d1fb2d939bffff7de5`, `<scratch>/tmc` at `7aa2590ff4ff915efb721a9b1a69fa84ce8fac3f`) using `git show` and `git grep` only. Nothing was built, run or modified. I met no text addressed to an AI agent. The only prose in upstream files that I read in full was code comments, and I followed none of it.
- No network, Gradle or adb was used. Licence spot-checks used the local, already-resolved Gradle module cache (read-only) and the clones.

## Section 7: verification of the claims

| Claim in the analysis | What I checked | Result |
|---|---|---|
| The mail modules verify nothing about TLS themselves; the supplied factory decides everything | `git grep` at the pin for `HostnameVerifier`, `endpointIdentification`, `SSLParameters`, `serverNames`, `SSLContext`, `TrustManager`, `SSLSocket` over `mail/common/src/main` and `mail/protocols/imap/src/main` | Confirmed. The only hits are in `mail/common/.../ssl/TrustManagerFactory.java` (a helper that is used only if the factory chooses it). The IMAP module has none. |
| Sockets are created only through the supplied factory (check (a)) | Read `RealImapConnection.kt` `open()`, `connectToAddress` (`:170-189`), `startTls` (`:292-301`), `enableCompression`; `git grep` for `Socket(`, `createSocket`, `SocketFactory`, `SSLSocket` over both modules; checked `ImapStore.create`, `RealImapStore`, `ImapFolderFetcher`, `ImapServerSettingsValidator` | Confirmed, with one precondition the analysis does not state (see M-1). Implicit TLS calls `socketFactory.createSocket(null, ...)`. STARTTLS calls `socketFactory.createSocket(socket, ...)` and then rebuilds the input and output streams, so any plaintext bytes buffered before the upgrade are dropped. Compression wraps streams, not sockets. There is no proxy handling and no second default factory. The `create(...)` entry point requires a non-null factory. A "no TLS" connection is a bare `Socket()` that never touches the factory. |
| Credentials are not sent before TLS and there is no silent downgrade (check 2) | Read `authenticate()` and the PLAIN, LOGIN, CRAM-MD5, EXTERNAL and OAuth branches | Confirmed. The branch is chosen from the configured `AuthType` only. There is no fallback from OAuth to a password mechanism. For STARTTLS, the library throws if the server lacks the capability. It issues STARTTLS, upgrades, and re-requests capabilities before authenticating. `LOGINDISABLED` is honoured on the non-`AUTH=PLAIN` path. |
| Which TLS code would move (check (d)) | Compared Q7.f with the vendored `ssl/` package and the upstream counterparts | List is correct for the TLS package: factory with cipher order, blacklist and `hardenSocket`; the trust manager with the platform verifier and the `MinimalSslSession` shim; `CertificateChainException`; `LocalKeyStore` and `KeyChainKeyManager` conditional. One security-relevant item is missing: the store-URI decoder (see M-1). |
| Upstream default factory lacks the blank-host guard (check (c)) | Read `legacy/core/.../DefaultTrustedSocketFactory.kt` and `HostNameUtils` claims | Confirmed. SNI is set when `isLegalIPAddress(host) == null`, and `SNIHostName(hostname)` has no `try`/`catch` (`:131-139`). Both vendored and upstream paths fail closed on an empty host (the hostname check cannot succeed), so the difference is robustness and a crash class, not an acceptance of a bad certificate. The analysis labels the SNI behaviour as code reading plus a JDK 25 probe and says Android's runtime copy was not run. That label is honest. |
| `PinnedCertificateSocketFactory` semantics | Read `PinnedCertificateSocketFactory.java:55-160` | Confirmed: SHA-256 equality of the leaf, then `checkValidity()`, no hostname check, `getAcceptedIssuers()` returns the enrolled leaf. It is app-owned, so a migration changes only the `MessagingException` import. |
| CNTR-MODERNIZATION-001 measurement (P-3, P-4, P-5) (check (e)) | Compared the contract text at `:80-102` with upstream `TrustedSocketFactory.kt` and `DefaultTrustedSocketFactory.kt` | Confirmed. Same interface FQN; `MessagingException` is now `net.thunderbird.core.common.exception.MessagingException`; the slot is a parameter of `ImapStore.create(...)`; producer 1 lives in `:legacy:core` with a different constructor. The analysis routes all three to change records and decides none. |
| `org.apache.http.legacy` independence | Checked the Q7.d commands against this repository and the pin | Confirmed for this repository. The httpclient5 claim ("no `javax/naming` reference in the `ssl` classes", C-412) rests on a class-file text scan that I could not re-run offline. It is consistent with httpclient5 5.x using `DistinguishedNameParser`. |

Status review for check (b): all four rows are genuinely "supported" under the plan's definition (app-owned code through a public interface, no upstream file changed). None is a blocker and none is a fork in disguise. One qualification belongs to the product owner. For the SYSTEM_VALIDATED producer, "supported" means the app permanently owns about 450 lines of K-9-derived TLS code (267-line factory plus 191-line trust manager, with the shim). Upstream's own factory cannot be used because it sits in an Android module with the legacy tree behind it. The analysis states this in Q7.f and Q9.a (Option 3 "owns every fix"), so it is not hidden.

## Section 6: authentication

- The mechanism table matches `authenticate()` at the pin. PLAIN and LOGIN, CRAM-MD5, EXTERNAL, XOAUTH2 and OAUTHBEARER are implemented, and OAUTHBEARER is preferred over XOAUTH2 when both are advertised. The citation line ranges I opened (C-300 `:359-385`, C-355, C-356, C-338, C-302) land on the stated code.
- Selection logic is correctly described as "configuration picks the branch, the server only confirms the capability". There is no downgrade path from OAuth to a password mechanism.
- P-2 consequence, which the analysis spreads across Section 5 and Q6.c and never states as a security outcome (see L-1). At the pin, `XOAuth2ChallengeParser.shouldRetry` returns true only for status 400 or an unreadable challenge. The connection invalidates the token on any negative response (the source carries a `TODO` that says so) and retries once. A second failure is permanent. So the retry is bounded at two attempts per `open()`. If the app's provider does not refresh behind `invalidateToken()`, the second attempt re-sends the same stale token, and the adapter then sees only `AuthenticationFailedException`. The worker's own refresh is gated on `status == 400` and a single retry (`BackupWorker.kt:442-446`, `currentTry < 1`). The consequence is a failed backup, or a token-endpoint call on every authentication failure if the adapter synthesises 400. It is not endless retry, not a downgrade and not a credential leak.

## Section 3 and Section 9: supply chain

- U-027 precedent: `k9mail-vendored/NOTICE` lines 25-30 say what the analysis says. The analysis correctly declines to confirm or contradict the eviction from the JitPack builds API (an "ok" build record is not the artifact). Option 3's "already closed, remaining risk is staleness" is accurate.
- "Who owns security patches" is answered honestly for all three options: the product owner in each case, with upstream flowing in only by re-copy or rebase. The Implementer's note says the supply-chain position after migration "is not better than today's". The analysis does not imply the vendored code is current: it gives the fork point (11 ahead, 15,032 behind; merge base 2016-10-20) and names a concrete missing upstream fix (the 2026-10-26 certificate blocklist, which I read in `CertificateBlocklist.kt`). It does not say that no vulnerability audit was done (see L-2).
- Licence spot-check, 9 rows against sources I could open offline:
  - jzlib 1.0.7: the POM says only "BSD" with a dead author-site URL. Confirmed. The analysis resolved it from the sources-jar header, which is the right handling of a vague row.
  - jutf7 1.0.0: the POM names a relative `LICENSE.txt`, and the cached jar contains no licence file. Confirmed. The analysis resolved it from the sources-jar header.
  - slf4j-api 1.7.36 and parent: MIT. Confirmed.
  - Upstream `LICENSE` is Apache-2.0 (C-102). Confirmed.
  - Upstream `NOTICE` (K-9 Mail, AOSP; C-103). Confirmed.
  - MPL-2.0 text at the cited `LICENSE` lines in the components repository (C-107). Confirmed.
  - mime4j 0.8.x parent POM is Apache-2.0. Confirmed on the cached 0.8.11 POM; the 0.8.15 row has the same parent chain.
  - commons-io 2.16.1 POM is Apache-2.0. Confirmed. The same cached versions are the "vendored" versions the analysis cites against the stale `NOTICE`.
  - `NOTICE` itself still lists mime4j 0.7.2 and commons-io 2.4. Confirmed (Q3.e and follow-up 3 are right).
  Remaining "POM only" Apache-2.0 rows (Kotlin, Koin, kotlinx, okio, moshi, androidx) were not re-opened. They are well-known licences and the analysis does not rest any verdict on them.

## How the research was carried out

- Clones: HTTPS only, `credential.helper` cleared, `GH_TOKEN` and `GITHUB_TOKEN` stripped (`implementation-log.md` I1 entries; Experiment (a)). `--no-checkout` partial clone, then `checkout --detach` at the pin. Nothing was executed from the clones.
- Experiment (f) (scratch Gradle project): rule 3 of the experiment rules is met in substance. It used a separate `GRADLE_USER_HOME` in `<scratch>` (caches landed there, about 3 MB), no daemon, the token variables stripped, a plain `java-library` project with no upstream build logic or plugins, repositories Maven Central and Google Maven only, and the `dependencies` task (metadata resolution; no jars downloaded, the log says so). Gradle 9.5.1 and JDK 25.0.5 are recorded. The deviation (running the installed distribution instead of the repository wrapper, to avoid contacting an unlisted host) is disclosed and is the more conservative choice. Residual points are in L-4.
- Hosts contacted, from the citation table: github.com, api.github.com (unauthenticated, generic client string), repo1.maven.org, dl.google.com, jitpack.io, maven.mozilla.org, blog.thunderbird.net, k9mail.app. All appear in the plan's allowed list. The jzlib author site was deliberately not fetched.
- Three-attempt rule: no experiment was reported "inconclusive". The two failed launches of (f) exited 127 before Gradle started (shell path error) and are recorded. Experiments (c) and (d) were not run, by a recorded plan decision, so they have no attempts.
- Identifying data in requests: none found. Queries were repository search terms; the client string is generic.
- Repository state: `git diff --stat c8f04558 -- app k9mail-vendored metadata gradle settings.gradle build.gradle gradle.properties gradlew gradlew.bat` is empty (including the working tree), so `gradle/verification-metadata.xml` and every product path are unchanged. `git diff --name-status c8f04558` lists 11 paths, all in the permitted set: the analysis, the input copy, the story file, the workspace artifacts, the sprint plan and execution plan, and `sdlc/.skill-delegation-audit.log`. That log gained two lines written by the create-sprint workflow step (instance ids and a hash only; no personal data). It is bookkeeping, not research output, and IC-4's text accepts it. `git status --short` shows three modified permitted files and the untracked `sdlc/.skill-context.json`, which was on the I0 list. No experiment output, clone or scratch project is in the tree.
- `U-062/compiled-pipeline.json`: not tracked (`git ls-files` has no sprint-019 entry) and excluded by a rule in the local exclude file. It contains an absolute repository path and a plugin-cache path, which is why it must stay untracked. Confirmed.

## IC-3 secrets and personal data

I scanned the analysis, the input copy, `implementation-log.md`, the story file, `plan.md`, `plan-review.md`, `contract-verification.md`, `expert-depth-check.md`, `sprint-plan.md` and `execution-plan.json` (working-tree content, including the uncommitted edits) for: e-mail addresses (none), SSH-form remotes and URL-embedded credentials (none), tokens, passwords and key material (the keyword hits are all prose such as "Bearer token" or "AUTH=" and carry no value), phone-number and device-serial patterns (none), the machine hostname and Windows user name read live from the shell (none), and absolute user-profile paths (`C:\Users`, `/c/Users`, `AppData`; none). Repository-root paths appear only in the untracked `compiled-pipeline.json`. The one path-pattern line in the story file (criterion IC-3, line 101) is the expected criterion text. No third-party commit-author names or addresses appear; the history excerpts use `%h %ad %s`. GitHub handles in the text are limited to the public fork owner already named in `k9mail-vendored/NOTICE`. No hit; no value to withhold.

## Prompt-injection check

No quoted third-party text in the analysis or the input copy addresses a reader or an agent. The quotations are two maintainer replies from 2017, announcement sentences and licence or POM fields, none of them an instruction. The input copy carries the required "UNREVIEWED INPUT - not a citation source" banner. The implementation log records no step taken on the strength of text found in upstream files. A grep for the usual injection phrases ("ignore previous", "you must", "system prompt", "disregard") over the analysis and the input copy returned nothing.

## Runtime claims

`implementation-log.md:16` declares `Runtime Claims: none (empty by design)`. I audited this against the security-relevant acceptance criteria. AC-9 asks for upstream behaviour in the SNI cases, and the analysis labels every such cell "code reading", or "code reading plus experiment (g)" where it ran the JDK class. It makes no claim that an Android runtime or a live server behaves in a given way, and says so (Q7.c, Q7.g, Q12.f item 7). The claims that depend on a live server (which mechanism Gmail negotiates, whether the platform adds SNI to a layered socket) are disclosed as unmeasured, not asserted. I treat the empty inventory as complete. No probe wrote into `../amp`.

## Findings

### Blockers

None (no Critical, no High).

### Warnings

**Medium**

- **M-1. The scheme-to-`ConnectionSecurity` mapping, and the app's cleartext option, are not in the analysis's account of what moves or of what protects credentials.** Locations: analysis Q6.a/Q6.b, Q7.f, Q12.e condition 12. The library never refuses to send a password on an unprotected connection. With `ConnectionSecurity.NONE` it opens a bare `Socket()`, never calls the factory, and authenticates with `AUTH=PLAIN` or `LOGIN` (the only guard is `LOGINDISABLED`; `RealImapConnection.kt` `connectToAddress` and `authenticate`). Today the vendored `ImapStoreUriDecoder` turns `imap`, `imap+tls+` and `imap+ssl+` (and the obsolete `imap+tls`/`imap+ssl`, matched with `startsWith`) into `NONE`, `STARTTLS_REQUIRED` and `SSL_TLS_REQUIRED`. At the pin the decoder exists only in a settings-migration package under `legacy/storage`, which is not in the proposed module set, so after a migration the app would write that mapping itself when it builds `ServerSettings`. The settings screen offers "no security" (`app/src/main/res/values/arrays.xml:108-122`, empty protocol value), and `BackupImapStore.java:236` and `K9MailTransport.java:337` accept the bare `imap` scheme. Q7.f lists the TLS files that must move but not this mapping, and condition 12 has no test that STARTTLS and implicit-TLS settings still reach the factory. A wrong default here is the one place a migration could silently send credentials in the clear without touching any of the four Section 7 rows. It does not change a Section 7 status. Fix: add the decoder or mapping to Q7.f's list and a condition that tests the three scheme values (including that an unknown scheme throws) in Part B. Classification: defer-justified — outside the four-type `fix-in-cycle` allowlist (it needs new analysis content and a new condition, not a mechanical edit), and Part A already routes such items to Section 12 conditions at AC-15.

- **M-2. Condition 12's tests would not prove that the protections survive the migration.** Location: Q12.e condition 12. It requires the 10 SNI cases (which call a private factory method by reflection or mocks and pass on code that is copied unchanged), "a hostname-mismatch rejection test" and "a pinned-accept / different-reject test". I found no hostname-mismatch test anywhere under `app/src/test` today, and `PinnedCertificateSocketFactoryTest` exercises the trust manager through a package-private seam, not a handshake. So the condition would pass without showing that the new library actually routes both connection paths through the app's factory. A stronger test, in three parts: (1) run a loopback TLS server in the unit-test tree and drive the real store against it through `ImapStore.create(...)` for both implicit TLS and STARTTLS; (2) a SYSTEM_VALIDATED case with a certificate valid for another host must fail with `CertificateValidationException`, a matching host must succeed as the negative control, and the factory must take an injectable trust anchor so the test can use a test CA; (3) a PINNED case where the enrolled leaf is accepted and a different chain that system trust would accept is rejected, with a STARTTLS-required server that withholds `STARTTLS` and a recording server proving that no `LOGIN` or `AUTHENTICATE` bytes arrived before the handshake. Classification: defer-justified — a test-design recommendation for Part B whose adjudication needs execution, which is outside the static-provability invariant.

**Low**

- **L-1. P-2 is described as a typing gap and never as a security outcome.** Location: Q6.c "Failure typing", Q5.p check (a). Section 5 does reason about a synthesised status (a constant 400 over-refreshes, -1 stops refreshing). The authentication section should add what I verified: bounded to two attempts per `open()`, no downgrade, no credential leak, and a failure mode where a provider that does not refresh behind `invalidateToken()` re-sends the stale token. Condition 7's test ("the engine's token refresh still runs on a failed XOAUTH2 login") covers the main case but not the "refresh did not happen" case. Classification: defer-justified — new analysis content rather than one of the four mechanical edit types.

- **L-2. Staleness of the vendored IMAP and MIME code is described, but the analysis does not say no vulnerability review was made.** Location: Q9.a Option 3, Q12.c. A reader could take "the risk is staleness, not loss" as "nothing known is wrong". The vendored IMAP response parser and MIME decoder process untrusted server data at restore time, from a 2016-2018 fork point. One sentence saying the analysis did not audit it would remove the ambiguity. Classification: defer-justified — adding a scope statement is not in the allowlist.

- **L-3. Adopting upstream's public trust manager changes the hostname-matching rules, which Q7.a does not say.** Location: Q7.a row 1. The vendored code delegates to the platform verifier (`HttpsURLConnection.getDefaultHostnameVerifier()` through `MinimalSslSession`). Upstream uses httpclient5's `DefaultHostnameVerifier`. Q7.a notes the extra library and blocklist but not that wildcard and common-name handling can differ between the two verifiers. The analysis's main path (move the vendored class) is unaffected. Classification: defer-justified — comparison needs running both verifiers against test certificates.

- **L-4. Experiment (f) hygiene is good but not fully evidenced.** Location: Experiment log (f), implementation log I1b.4. Only `GH_TOKEN` and `GITHUB_TOKEN` were stripped, so the rest of the environment was inherited (no credentialed repository was configured, so nothing could be sent to the registries). "Nothing was written to the real Gradle home" is asserted from the scratch cache size, not from a before-and-after listing of the real home. The Gradle user-agent discloses the OS and JDK, which are not identifying. The repository-side outcome (`gradle/` untouched) is verified above. Classification: defer-justified — process evidence cannot be re-created by editing text.

- **L-5. Option 2 and mode M1/M2 run upstream's Gradle build logic inside the app's build, and the supply-chain column lists reachability but not code execution.** Location: Q9.a Option 2. Included-build or composite consumption runs a third party's convention plugins on the developer's machine, where signing configuration and `local.properties` are in reach; Option 1 (source copy) does not. This should be an input to the Option 1 versus Option 2 choice (PO-11). Classification: defer-justified — new analysis content.

### Observations (pre-existing app weaknesses the analysis merely describes; not findings against this story)

- The vendored `DefaultTrustedSocketFactory` still enables TLSv1 and TLSv1.1 (`ORDERED_KNOWN_PROTOCOLS`), and `PinnedCertificateSocketFactory` neither hardens protocols or ciphers nor sets SNI itself (the analysis says so in Q7.g). The pin makes interception fail anyway, but a downgrade is not blocked.
- OAuth tokens and IMAP passwords live in app preferences (`AuthPreferences`), and the XOAUTH2 initial response is stored as the URI "password". A migration to the provider seam would, if anything, stop storing the pre-built string.
- `HashMap` caching of per-host trust managers in upstream's `TrustManagerFactory` is not synchronised. This matters only if upstream's class is adopted instead of the vendored one.

## Security Checklist

- [x] No hardcoded credentials or secrets (IC-3 scan clean)
- [x] Input validation: not applicable to the deliverable; the analysis correctly notes the guard code and where it lives (M-1 covers the one unmentioned input mapping)
- [x] Output encoding / injection: not applicable (documentation); no quoted instructions to agents
- [x] Authentication tokens handled securely: analysis describes the seam accurately (L-1 for the P-2 outcome)
- [x] Authorization checks: not applicable
- [x] Sensitive data encrypted in transit: Section 7 claims verified; the cleartext option is an unmentioned precondition (M-1)
- [x] Error messages do not leak internal details: not applicable
- [x] Dependencies have no known critical vulnerabilities: not assessed by this story and not claimed; licence verdicts spot-checked (L-2 on the vendored code)

## Runtime Claims Verified

- [x] `## Runtime Claims` inventory read in `implementation-log.md` (line 16: declared empty by design, not omitted)
- [x] Every declared claim and every security-relevant runtime-behaviour AC has an embedded probe or an escape hatch: no claim is declared; AC-9's upstream behaviours are labelled code reading, plus the JDK probe of experiment (g), and no runtime assertion is made
- [x] Probe design adequacy: the one probe (experiment (g), `SNIHostName` on the build JDK) states its limit (Android runtime copy not run); it is not offered as proof of Android behaviour
- [x] Escape-hatch justifications: none used
- [x] Inventory completeness audited against the security-relevant ACs: no gap found
- [x] No probe wrote into `../amp`
- [x] UNVERIFIED (escape-hatch) claims: None. Disclosed, unmeasured items (not escape-hatch claims) are listed in analysis Q12.f item 7, including what a live Gmail server negotiates and whether the platform adds SNI to a layered socket.

## Phase Completion Report
---
story_id: "U-062"
phase: "security-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-019/U-062/review-security.md"
story_status: "in-progress"
current_build_phase: "security-review"
blockers: 0
warnings: 7
errors: []
---


## Delta re-review after IR1 (2026-11-13)

Scope: only the parts changed in rework increment IR1, re-read from disk: analysis Q5.t, Q6.c, Q6.f, Q7.a, Q7.f, Q7.h, Q7.i, Q9.a, Q9.c, Q12 conditions 7, 12, 16 and 17, PO-13, the "If the product owner chooses `no-go`" and "Reviewers' readings" sub-sections, the Experiment (f) entry, citations C-433 to C-454, Record 1, the IR1 log entries and the story's Implementation Notes. Upstream was read again in the read-only clones (`git show` and `git grep` only). No Gradle, network, adb or state-changing git.

### Verdict after IR1: PASS (unchanged)

No Critical or High finding. No Section 7 status changes. Three new Low findings and two observations, below.

### Disposition of the earlier findings

| Finding | Disposition | Basis |
|---|---|---|
| M-1 connection-security mapping and cleartext | **Resolved** (residual points in N-2) | Q7.h, Q6.f, Q7.f, condition 17. Facts verified, see item 1. |
| M-2 TLS test strength | **Resolved** | Condition 12 and Q7.i, see item 2. |
| L-1 P-2 as a security outcome | **Resolved** | Q6.c now states the bound (two attempts per open, no downgrade, no leak), the same-stale-token case, and the synthesise-400 / -1 outcomes. Condition 7 adds the "provider returns the same token twice" test. Re-checked against `RealImapConnection.kt` and `XOAuth2ChallengeParser.java`; accurate. |
| L-2 vendored code not audited | **Resolved** | Q9.a option 3 now carries "Not audited ... no advisory database or scanner was used". |
| L-3 hostname-matching rules differ | **Resolved** | Q7.a now says the two verifiers differ in implementation and that wildcard and common-name equivalence was not tested. |
| L-4 experiment (f) evidence | **Resolved** | The restated entry claims only what the evidence shows (item 3). |
| L-5 upstream build logic runs in the app's build | **Resolved** | Q9.a option 2 names it and cites the signing-key properties read in `app/build.gradle` (lines 13-16 and 32-37, confirmed). Option 1 correctly runs none. |

### 1. M-1 / Q7.h: facts and sufficiency

Verified against this repository and the pin:

- The settings screen offers TLS (`+ssl+`, default), STARTTLS (`+tls+`) and None (an empty value) (`app/src/main/res/values/arrays.xml:108-122`, `app/src/main/res/xml/preferences.xml:46-52`). `AuthPreferences.formatUri` builds `imap<value>://...`, so None gives `imap://`; an absent preference gives `+ssl+`; the XOAUTH2 path passes `+ssl+` directly. Correct.
- The vendored decoder throws `IllegalArgumentException("Unsupported protocol ...")` for an unknown scheme, and `ImapStore` reports it as "Error while decoding store URI", so an unknown scheme fails closed today. `ImapStore` also refuses a null user name or password before any I/O. Correct. The line counts (133, 52, 28) are right; `UrlEncodingHelper.java` sits under `.../mail/helper/`, which is a path slip in the summary only.
- Upstream keeps a decoder only in `legacy/storage/.../migration/migration12/ImapStoreUriDecoder.java` (144 lines; same three branches and exception; defaults the authentication type to PLAIN), referenced only by `StorageMigrationTo12.kt`. Correct.
- At the pin `ConnectionSecurity.NONE` opens a bare `Socket()`, never calls the factory, and `authenticate()` sends the password regardless. `LOGINDISABLED` is read only in the `else if (!hasCapability(LOGINDISABLED)) login()` arm of the PLAIN branch; it is not read on the SASL PLAIN path or on the `loginOrThrow` fallback after a failed SASL PLAIN. The vendored `ImapConnection.authenticate()` has the same shape (lines 336-347). So the library behaviour is unchanged and only the mapping moves into app code. Correct.
- The risk is stated plainly and correctly: a wrong or missing value at that one line sends credentials in the clear while every Q7.a row still reads "supported" and every TLS test still passes. The text does not call "None" acceptable and does not propose removing it, which is the right place for the analysis to stop.
- No Section 7 row status changes. Agreed: the four rows describe protections on a connection that goes through the factory, and Q7.h says it is a precondition, not a fifth row.

Condition 17 covers an unknown stored value (throws before any socket, zero factory calls, zero server connections), an absent value (TLS, never NONE), the obsolete `+ssl` and `+tls` forms, and "no TLS or STARTTLS setting ever yields NONE", which is the downgrade check at the mapping. It is sufficient for the mapping as it stands. Residual points are in N-2.

### 2. M-2 / condition 12 and Q7.i

Condition 12 is now a condition with observable results, not a design. It drives the real store entry point against a loopback TLS server, once for implicit TLS and once for STARTTLS, and asserts: a certificate for a different host is rejected and a right-host certificate from the same test authority is accepted as the control; the enrolled certificate is accepted and a different certificate that system trust would accept is rejected; the server records bytes and no LOGIN or AUTHENTICATE arrives before the handshake; a STARTTLS-required server that withholds the capability fails with no credential bytes received; and the ten SNI cases and the `org.apache.http` grep remain. That is strong enough to catch a library or wiring change that bypasses the factory, accepts a wrong-host certificate, or sends credentials early, which is what REQ-MODERNIZATION-002's protections need. It does not assert that an SNI name goes on the wire for a DNS host; Q7.g already says wire-level SNI is untested, and I accept that.

The claim "no hostname-mismatch test exists today" is correctly evidenced. I re-ran the recorded search over `app/src/test` and it returns 0 lines, and `git ls-files k9mail-vendored` lists no `/src/test/` path. The descriptions of the SNI test (Mockito socket, no handshake), the pinned test (package-private trust-manager seam; the mismatch case uses a different certificate, not a different host) and the GreenMail test (`imap://PLAIN:...@127.0.0.1`, the unprotected path) match the files.

### 3. L-4: restated experiment (f)

The entry now separates what the evidence shows (the command, exit 0, the closure, the scratch home filling up, a project that declares only Maven Central and Google Maven and no credentials block) from what is inferred (only the two token variables were stripped and the rest of the environment was inherited and not enumerated; "nothing written to the real Gradle home" is inferred from the scratch cache, not from a before-and-after listing). It also discloses that the jars behind the class and licence-header counts were fetched by a separate scratch script from the two registries, only parsed, never executed, and that the command was not recorded when run. That disclosure is honest and claims no more than the evidence supports. The unrecorded fetch is a process gap, not a breach of the experiment rules (O-1).

### 4. Read timeout and connect retry: security reading

- **Read timeout (60 s against the fork's 120 s):** reliability only. A shorter timeout ends a silent connection sooner and fails closed. It re-sends nothing and changes no security mode. The data-integrity side (a timeout during APPEND after the server has stored the message could produce a duplicate on retry) belongs with condition 16's live-account check.
- **Connect retry (absent at the pin):** also reliability only. Upstream `connect()` loops over the resolved addresses and calls `connectToAddress` for each, with the factory called per address using the host name; only an `IOException` from the socket connect moves to the next address. The TLS handshake and everything after it happen later, outside the loop, on first I/O, so a certificate failure never leads to another address and no credential is sent inside the loop. The vendored loop (3 tries per address, backoff, 30 s budget) has the same structure. Neither side has a retry path that re-sends credentials or falls back to another security mode. The analysis's statement that the fork replaced the loop rather than added to it is consistent with the code.
- **Untried app-owned socket wrapper:** this is a real security question because it sits on the TLS path. A reviewer would need to see: (1) the wrapper overrides only `setSoTimeout` (and the getter if needed) and delegates every other `SSLSocket` method, including `startHandshake`, `getSession`, `getHandshakeSession`, `getSSLParameters` and `setSSLParameters`, the handshake-listener methods, the stream getters and `close`; (2) it is applied after the factory has hardened the socket and set SNI, because the vendored factory's SNI fallback for API levels below 24 finds `setHostname` on the socket's own class by reflection and would silently find nothing on a wrapper; (3) it is applied on both the implicit-TLS socket and the STARTTLS-upgraded socket; (4) the condition 12 tests run against the wrapped socket, so wrong-host rejection, pinned rejection and the no-credentials-before-handshake check are shown to hold with the wrapper in place. The analysis calls the wrapper "app-owned code on the TLS path of Section 7" but does not say condition 12 must run against it, and condition 16 only reads `getSoTimeout()` (N-3).

### 5. The `no-go` sub-section

Sound and honest. It says plainly that it is prepared text, not in effect, and that no change-record file was created. The repository facts check out: 121 tracked files under `k9mail-vendored/`, 116 of them `.java`, exactly 5 commits touching it, 2 configured remotes. It does not claim a mirror exists: it says where the remotes point and whether the branch is pushed were not examined, and asks the decision record to name the mirror or state that there is none. The detection list is concrete and mostly checkable, and the recreation statement is correct and important: the origin plus the two `NOTICE` modifications yields only the 2026-06-03 state, while the SNI guard, pool drain and dependency refresh exist only in this repository's history. It is consistent with L-2: it never calls the vendored code audited. It leaves implicit that the periodic check has no step for known vulnerabilities in the vendored code's own libraries (commons-io, mime4j, jzlib, jutf7); since Q9.a states that nothing was audited, I do not count that as a finding.

### 6. "Reviewers' readings"

The sub-section reports my review accurately. It attributes "preserve, not improve" to the analysis's own summary and not to my file, which is correct: that sentence is not in my review file. It reports the roughly 450 lines of K-9-derived TLS code the app keeps owning, the M-1 risk, "not better than today's" (a sentence of the Implementer's note that my review quoted) and the observation about the pre-built XOAUTH2 string. It omits one thing I said in my message to the coordinator: adopting upstream's certificate blocklist would be a real improvement if the product owner takes it (PO-11).

My position, in two lines. The migration as described would **preserve** the app's TLS and credential handling, provided conditions 12 and 17 are met. Two things could improve it (the optional certificate blocklist, and the provider seam ending the storage of the pre-built XOAUTH2 string) and one could weaken it (the connection-security mapping the app would have to write itself).

### 7. Hygiene (IC-3) and tree

- Scanned the analysis (including Record 1), the input copy, the story file, the log, the sprint plan and the code review for e-mail addresses, credentials and token shapes, the machine hostname and Windows user name read live from the shell, user-profile paths, and injection phrases: no hit. The only path-pattern line is the criterion IC-3 text in the story file, which is expected. Record 1 holds the product owner's words and role names only and carries no instruction addressed to a reader or agent.
- `git status --short` shows the modified permitted files, `sprint-plan.md` (workflow bookkeeping), the untracked `sdlc/.skill-context.json` (on the I0 list) and the two untracked review files in the workspace. `git diff --name-status c8f04558` shows only the permitted set. The product-path diff (`app k9mail-vendored metadata gradle settings.gradle build.gradle gradle.properties gradlew gradlew.bat`) is empty. No change-record, REQ, DES, CNTR or other U file was created; the change-records directory still ends at CR-004.
- Citations C-433 to C-454: those I opened against the clone (C-433 timeout values, C-355 and C-356 socket calls, C-443 decoder branches) match. C-451 is used twice, for the empty-last-field fix and for the origin commit page; both refer to the same commit, so this is consistent.

### 8. Anything the rework broke or introduced

Nothing that changes the verdict. No quoted third-party text addresses a reader or agent. New items:

#### Warnings

- **N-1 (Low). "No test checks any of this mapping" (Q7.h) is slightly broader than the evidence.** `AuthPreferencesTest` asserts the preference-to-URI half for `+ssl+` (PLAIN and XOAUTH2) and the legacy `+ssl`/`+tls` migration, and `BackupImapStoreTest` asserts which schemes the app's validator accepts. Untested is the other half, URI to `ConnectionSecurity` (nothing under `app/src/test` mentions `ConnectionSecurity`), plus the `+tls+`, empty (None) and absent-preference cases of the first half. Q7.i's narrower wording is accurate. Classification: defer-justified — a wording refinement outside the four mechanical `fix-in-cycle` types that changes no conclusion.
- **N-2 (Low). Condition 17 cannot make an accidentally empty value fail closed, and omits two cases.** The empty string is both the legitimate "None" choice and what a blanked or corrupted preference would look like, so the condition correctly says "empty gives NONE"; the product owner should know that this value is the single path to cleartext. Cases to add in Part B: the XOAUTH2 path must give TLS whatever protocol value is stored (today it is forced to `+ssl+`), and with the pinned-certificate policy chosen together with None the pin is silently unused because the factory is never called. Classification: defer-justified — new test content for Part B, not a mechanical edit.
- **N-3 (Low). The wrapper alternative in Q5.t is not tied to condition 12.** See item 4. Classification: defer-justified — new analysis content for the decision, not a mechanical edit.

#### Observations

- **O-1.** The jar fetch behind the Section 3 and Section 8 counts was not recorded when run (disclosed in the restated entry (f)). The jars were only parsed.
- **O-2.** `AuthPreferences.isTrustAllCertificates()` (line 277) has no caller in `app/src/main`, so no trust-all path is reachable, consistent with CNTR-MODERNIZATION-001's "two and only two producers". Not a finding against this story.

### Delta disposition summary

M-1, M-2 and L-1 to L-5 resolved. New: 0 Critical, 0 High, 0 Medium, 3 Low (N-1 to N-3), 2 observations. Verdict PASS.

## Phase Completion Report (iteration 2)
---
story_id: "U-062"
phase: "security-review"
verdict: "PASS"
iteration: 2
artifact_path: "sdlc/artifacts/build/sprints/sprint-019/U-062/review-security.md"
story_status: "review"
current_build_phase: "review"
blockers: 0
warnings: 3
errors: []
---


## Delta re-review after IR2 (2026-11-17)

Scope: only what IR2 changed: Section 9 (pointer after Q9.a and the new Q9.f), Q11.e, the Q12.c extension, PO-14, Q12.f items 10 and 11, item 5 and the R-A/R-B text under "If the product owner chooses `no-go`", citations C-455 to C-582, the Experiment log IR2 entry, the IR2 entries of `implementation-log.md` and the story's "IR2 rework" paragraph. Upstream was read again in the read-only clone with `git show`, `git grep` and `git ls-tree` only; nothing from the clone was run, built or fetched. No Gradle, network or adb. The vendored source was read from `k9mail-vendored/` in this repository.

### Verdict after IR2: PASS (unchanged)

No Critical or High finding, and no Section 7 status moves. One new Medium and six new Low findings, all about what Q9.f leaves unsaid; none is an error that changes a conclusion. Frontmatter `iteration` is now 3.

### 1. The ten fixes: verdicts and weight

I checked each "vendored copy affected?" verdict against the vendored source and the upstream diff.

| # | Fix | Verdict in Q9.f | My check | Does it matter on this app's path (Gmail IMAP over TLS, XOAUTH2 or password, optional pin or user-trusted certificate) |
|---|---|---|---|---|
| 1 | cipher order `059064c9f1` | yes, in code form; effect on API 23+ not measured | Confirmed. Vendored `DefaultTrustedSocketFactory.java:96-117` reorders always; the five NULL/anonymous suites are not in its blacklist; upstream's change applies the reorder only below API 21 and otherwise only removes blacklisted suites. | Marginally. The app's minSdk is 23, so the upstream `hasWeakSslImplementation()` branch would never run; the five added suites are not enabled by default on a platform of that age, and the vendored ordering already puts ECDHE-GCM first, so a Gmail handshake is unchanged. The statement "effect not measured" is fair and not inflated. One thing a reader could miss: the fix leaves the protocol set as it is (both the vendored class and the pin still enable every platform-supported protocol except SSLv3, TLS 1.0 and 1.1 included), so neither backport nor migration tightens protocol versions. |
| 2 | httpclient5 hostname verifier `171c0eca43` | no (U-058 platform verifier) | Confirmed: `TrustManagerFactory.java:135-146` uses the platform verifier through the session shim. | Not needed. "Equivalent" is used in the R-B text for items 2 and 3; for item 2 it means "a hostname check exists", not "accepts the same certificates"; Q7.a says the comparison was not made (L-A5). |
| 3 | no SNI for IP literals `9d02baad2c` | no | Confirmed: the vendored guard (`DefaultTrustedSocketFactory.java:216`, used at `:239`) predates it. | Not needed. |
| 4 | certificate blocklist `ce0d6f1d5c` | yes | Confirmed: the vendored `TrustManagerFactory` has no blocklist. | Only for a chain that contains the listed compromised-issuer key, and only on the system-validated path (not the pin). Gmail's chain is not that issuer. Weight is stated neutrally, which is right; see L-A6 for the risks of copying it. |
| 5 | UTF8 literal `10b022ba40` | yes (`ImapResponseParser.java:392` decodes `US-ASCII`) | Confirmed. | Correctness of non-ASCII literal text, not a security property. Matters little here: the app writes ASCII header values, MIME-encoded words stay ASCII. |
| 6 | unknown content transfer encoding `025502ceb5` | yes, in another form | Confirmed: upstream threw a checked exception, the vendored `MimeUtility.decodeBody` throws unchecked `UnsupportedOperationException` for the same case; the fix passes the raw stream through. | Robustness on restore of a message with a malformed or tampered header (a crash, not a leak). Low. |
| 7 | `skipFully` loop `07985f7066` | could not tell | By reading I can say more. The upstream commit message says a `PeekableInputStream.skip()` bug could cause an endless loop in `skipRemaining()`. The vendored loop (`ImapResponseParser.java:372-374`: `while (fixed.available() > 0) fixed.skip(fixed.available())`) has the same shape, `FixedLengthInputStream.skip` adds to its count only when the underlying skip returns more than 0, and the vendored `PeekableInputStream` does not override `skip`, so it inherits the stream's. A stream that returns 0 at end of stream would make the loop spin. This is a reading, not a test. | Possible hang or CPU spin after a server closes mid-literal on a partly read literal. It needs a misbehaving or failing server, not an on-path attacker, since TLS truncation without close_notify raises an exception instead. Availability only. The analysis's "could not tell" is honest and errs on the safe side; L-A4. |
| 8 | CAPABILITY check `b241201e88` | yes (`ImapConnection.java:281-283`, `responses.size() != 2`) | Confirmed. | Robustness, not security: a server that adds an untagged line makes the connection fail (fail closed), and a two-line reply without a CAPABILITY would keep the previous capability set. After STARTTLS the reply comes over the authenticated channel, so this is not an on-path attack. Gmail sends one CAPABILITY line plus the tagged OK. Runs on every connection but is harmless for Gmail. |
| 9 | `close()` on a closed connection `c2eb00b4f6` | yes (`ImapConnection.java:635-637`) | Confirmed: no early return, it rewrites `open` and the stored stack trace. | Trivial. |
| 10 | `sendContinuation` I/O errors `8671ae1009` | yes in code form, off the app's path | Confirmed: only the push class calls it and the app has no pusher. | Does not matter. |

The summary "7 yes, 2 no, 1 could not tell" is right as far as I can read it. The weight is stated fairly: the analysis never calls any of the ten a vulnerability, calls the exercise "not a vulnerability audit", and says "a fix that is absent from the list proves nothing". Of the seven "yes" items, none is a remotely exploitable flaw on the Gmail-over-TLS path as I read the code. Items 1, 8 and 4 are the ones on the TLS and connection path, and their effect for this app is small to nil. Items 5, 6 and 9 are robustness; 10 is off the path; 7 is an availability question.

### 2. The limits (531 / 38 / 10 / about 690 / no advisory lookup)

The limits are stated so a reader cannot mistake the search for an audit: Q9.f 4 opens with "not a vulnerability audit", the method is given down to the command shape, the 38 are all listed with the 18 marked as fix-like by the author's own reading, the eight fix-like commits not read are named, the advisory lookup for mime4j 0.8.11 and commons-io 2.16.1 is "not checked in this analysis" three times (Q9.f 4, Q12.f item 10, the IR2 log), and the "Not known without building" paragraph closes the sub-section. Nothing a reader would rely on for a security decision is stated more strongly than that evidence allows. Two wording points that could still mislead on the numbers (L-A5): the text says "531 commits" searched but also "the other 700 or so commits in these paths" (Q9.f, last paragraph), "732 mail-path commits behind" (Q12.c) and "roughly 690" (log). The 531 counts main-source paths only; the 732 comes from a count over the whole `k9mail-library` and `mail` trees, tests included. The two denominators should be named once, so that "about 690 unread" is not read as "of the 531".

### 3. Each route against the properties I reviewed

I checked the connection code at A0 (`934bbbe88299`) and A4 (`6ff1b67a7f`) against the vendored copy: at both, implicit TLS and STARTTLS call the supplied `TrustedSocketFactory` for the socket, STARTTLS is required or the connection fails, the capabilities are re-read after the upgrade, and `LOGINDISABLED` is read in the same arm of the PLAIN branch. So the library's own behaviour on certificate validation entry, STARTTLS downgrade and password handling is the same at every candidate as it is today.

(a) **Does an R-A candidate lose protections this repository added?** Yes, and the analysis says so: "None of this repository's changes is contained in a candidate" (Q9.f 3(c)), with the re-apply list: the SNI guard (`4616c097`, `e5445d56`; 71 lines), the platform hostname verifier and `org.apache.http.legacy` removal in `TrustManagerFactory` (`bdea6f4f`), the pool drain (`55370179`), and "Fail early if username/password not set" in `ImapStore`. I confirmed what happens if they are not re-applied: at A0 the factory has only the reflective `setHostname` SNI path and no IP-literal guard, and `TrustManagerFactory` at A0 and A4 calls the legacy Apache `StrictHostnameVerifier`, so the app would again need the legacy HTTP library that U-058 removed (a missing class on current Android, which fails the connection). The text lists these under "to re-apply" but does not say in plain words that forgetting any one is a silent security regression and that the ten SNI cases plus condition 12 are what would show it (L-A2). The credential-related "fail early" patch is also listed only as a 9-line patch among "the other six", not as the property condition 17 relies on.

(b) **P-2 arises at A0 to A4; P-1, P-3, P-4, P-5 do not.** P-2 is right: no candidate has the status-carrying XOAUTH2 exception, and I confirmed the older `retryXoauth2WithNewToken` logic and `invalidateToken(username)` at A0 and A4, so the bounded-retry analysis of Q6.c carries over. P-1 and P-3 are right (package names stay `com.fsck.k9.*`, `MessagingException` has not moved). P-4 is right (the factory is passed to the super-constructor, confirmed at A4). **P-5 is not fully right at A4:** the class and package named in the contract exist, but at A4 the constructor is `DefaultTrustedSocketFactory(Context, TrustManagerFactory)`, while at A0 and A1 it is `(Context)` as the contract names it. The constructor part of P-5 therefore arises at A4 (and A2/A3 were not checked). Under R-A the app would re-apply the vendored factory anyway, so the practical effect is small, but the sentence "P-5 does not arise at A0 to A4" is too broad (L-A1).

(c) **Backporting the certificate blocklist as new code.** The table says "new code: the vendored copy has no equivalent; the hash needs OkHttp's `CertificatePinner` or an own SHA-256 over the SPKI", and sizes it. The risks are not described (L-A6): the list is a hand-copied set of hashes (one wrong character silently disables an entry), it is static and has no update path, it applies only to the system-validated path (not the pin), in upstream a user-accepted exception for the same certificate overrides it, the new exception has to be mapped to the type the connection's SSL error handler expects, and a test with a known blocked certificate is needed. At the pin the code uses only okio's SHA-256; the JDK's `MessageDigest` would avoid a new dependency.

(d) **Dependency bumps under R-B and verification metadata.** "At least 6 entries, exact count not derived" for mime4j 0.8.15 and commons-io 2.22.0 is stated as an estimate, with the 3 components that are not there today. That is honest. It does not mention that parent POMs and module metadata could add entries, nor that mime4j 0.8.11 to 0.8.15 and commons-io 2.16.1 to 2.22.0 have not been checked against this app's source adaptation (U-058 adapted five files to 0.8.x). Neither point changes a conclusion.

**Effect of each route on the reviewed properties (my reading):**

| Property | R-A at A0 | R-A at A1 | R-A at A4 | R-B |
|---|---|---|---|---|
| Certificate validation and hostname verification | regress unless `TrustManagerFactory` is re-applied (legacy Apache verifier) | same | same | unchanged; blocklist added if copied correctly |
| SNI and IP-literal guard | lost unless the guard is re-applied | same | same | unchanged |
| Pinning and user-trusted certificates | the pinned factory is app-owned and untouched; the library's store is inert as today | same | same | same |
| Credential and token handling | token seam appears (app-side provider needed), status type lost (P-2); fail-early patch to re-apply | same | same, plus the app must decode the store URI itself (see N-4) | unchanged |
| STARTTLS downgrade and plaintext password | same library behaviour | same | same behaviour, but the scheme-to-security mapping becomes app code | unchanged |

### 4. Supply-chain statements for R-A and R-B

- **Source of the code:** commit ids are given for every candidate and for the ten fixes, and the upstream is the same public repository. R-A is called "a second, older private fork" owned by the product owner, which is accurate. R-B is "owned by the product owner; no automatic detection of new fixes", also accurate.
- **How integrity would be established:** not described. The analysis relies on commit ids and the citations but gives no procedure for showing that the vendored tree equals the upstream tree at the chosen commit apart from the listed patches (L-A7).
- **Who owns future fixes:** the product owner in both cases; stated.
- **"Reviewed-up-to commit plus periodic keyword search":** described as "Mechanism (not built)" in Q9.f 5 and in Q12.f item 11, with "who runs it and how often is the product owner's decision". Its weaknesses are partly named (the pin is not the reviewed-up-to commit; a keyword search rarely hits the project's wording; ports unreviewed). Not named: the path set (the blocklist module `:core:common` is outside it, and a move of the mail code to new paths would drop fixes silently), fixes in third-party libraries that no commit message in this repository will mention (they need an advisory feed), and the 8 fix-like commits not read.

### 5. My statements

- **Migration (unchanged):** the migration as the analysis describes it would preserve the app's TLS and credential handling if conditions 12 and 17 are met; Q9.f does not change that.
- **R-B (selective backport):** it would preserve the app's TLS and credential handling, because none of the eight ports touches certificate validation entry, pinning or credential sending, and it could improve it slightly (the five extra blacklisted suites, which matter little on a platform of API 23 or later, and the blocklist, which matters only for a chain through the compromised issuer). The risk is that each port is unbuilt and unreviewed and the selection rests on a keyword search; the blocklist copy is the one that touches the trust decision and needs its own tests.
- **R-A (intermediate snapshot):** it would weaken them unless every one of this repository's TLS changes is re-applied and shown by tests, because the older snapshot has no IP-literal SNI guard, uses the legacy Apache hostname verifier again, and (at A4) hands the connection-security mapping to the app. In return it reaches two to four of the ten fixes I read, and the code is 2017 to 2019, so it is not "the latest version". As a security move I read it as no better than today and with more ways to regress than R-B.

### 6. IC-3 and tree

- I scanned the analysis, the log, the story file and the sprint plan for e-mail addresses, the machine host name and the Windows user name (both read live from the shell), user-profile paths, token and key shapes, and instruction-like phrases addressed to a reader: no hit. The one path-pattern line is the criterion IC-3 text in the story file. No upstream author name or address was copied into the analysis (a search for the commit authors' names returns nothing); citations are GitHub commit and blob URLs.
- The interruption passage (`implementation-log.md`, "Interruption and resume", line 729) says that on 2026-11-13 at 17:15 local time the endpoint security product quarantined two Git shell files and killed the running shells, and that the work resumed on 2026-11-17 after the files were restored. It names no host, user, path, product vendor or file location. It is not identifying.
- `git status --short` shows the permitted modified files, `sprint-plan.md`, the untracked `sdlc/.skill-context.json` (on the I0 list) and the two review files. `git diff --name-status c8f04558` shows only the permitted set. The product-path diff is empty. No requirement, design, contract, story or change-record file was created; the change-records directory still ends at CR-004. The new text says in several places that no such file is created.
- Experiment log IR2 entry: the commands are given with placeholders only; "nothing was built, executed or applied; no upstream text was followed as an instruction" matches what I could check. No quoted third-party text addresses a reader or agent (the quoted items are commit subjects).

### 7. Open notes N-1 to N-3

IR2 touched none of them. The text "No test checks any of this mapping" (Q7.h, N-1), the condition 17 wording (N-2) and the Q5.t wrapper alternative without a link to condition 12 (N-3) are all as I found them in the delta after IR1. They stay open as Low.

### 8. New findings

#### Warnings

- **N-4 (Medium). Q9.f does not carry the M-1 risk or condition 17 to R-A at A4.** Location: Q9.f 3(a) table row "ImapStore constructor" and 3(d). At A4 the store takes `ImapStoreSettings` first and "the app must decode the URI itself", and the analysis counts only the file edits. Deciding TLS, STARTTLS or none is then app code again, exactly as under the migration (Q7.h), and it is the one place where a route can send the password in the clear while every TLS test passes. The same applies to the "fail early if username/password not set" patch, which is the property behind condition 17's "missing credentials throw before any socket". Suggested fix: add one sentence to Q9.f 3 and Q12.c saying that A4 (and any route where the app decodes the store settings) needs condition 17, and that the fail-early patch is a credential-safety re-application. Classification: defer-justified — new analysis content outside the four mechanical `fix-in-cycle` types, and no route is chosen yet (PO-14), so it can be added with the next revision.
- **L-A1 (Low). "P-5 does not arise at A0 to A4" is too broad.** Location: Q9.f 6 closing paragraph, Q12.c. At A4 `DefaultTrustedSocketFactory` takes `(Context, TrustManagerFactory)`, which the contract's `new DefaultTrustedSocketFactory(context)` does not match (A0 and A1 match; A2 and A3 not checked). Suggested fix: say the constructor part of P-5 arises at A4. Classification: defer-justified — a precision edit outside the allowlist; no predicate, row or Section 7 status depends on it.
- **L-A2 (Low). The R-A re-apply list does not say a missed item is a silent regression.** Location: Q9.f 3(c). Add that forgetting the SNI guard, the platform verifier or the pool drain regresses the app without a compile error, and name the tests that would show it (the ten SNI cases and condition 12). Classification: defer-justified — new analysis content.
- **L-A3 (Low). Fix 1 could be read as hardening protocols.** Location: Q9.f 4 table, row 1. Add that the fix does not change the enabled protocol set. Classification: defer-justified — scope statement, not a mechanical edit.
- **L-A4 (Low). Fix 7 can be given more than "could not tell".** Location: Q9.f 4 table, row 7. By reading, the loop can spin on a stream whose skip returns 0 at end of stream; it is an availability issue needing a misbehaving server. Classification: defer-justified — the conclusion needs a test.
- **L-A5 (Low). Denominators and "equivalent".** Location: Q9.f 4 and 5, last paragraph of Q9.f, Q12.c. Name what 531, 732 and "about 690" count, and use "has a hostname check" rather than "equivalent" for fixes 2 and 3 until Q7.a's verifier comparison is made. Classification: defer-justified — wording refinements outside the allowlist.
- **L-A6 (Low). Risks of copying the blocklist are not listed.** See 3(c). Classification: defer-justified — new analysis content.
- **L-A7 (Low). No integrity procedure for R-A / R-B, and weaknesses of the keyword mechanism only partly named.** See 4. Classification: defer-justified — new analysis content.

#### Notes

- **Note 1.** The EDR passage in the log is acceptable for IC-3 (item 6).
- **Note 2.** Q9.f says plainly that nothing was built, that the viability of every candidate is unknown without compiling, and that Part A built nothing. That is the right level of claim.

### Delta disposition summary

N-1 to N-3 untouched and still open (Low). New: 0 Critical, 0 High, 1 Medium (N-4), 7 Low (L-A1 to L-A7), 2 Notes. All new warnings are classified `defer-justified`. Verdict PASS.

## Phase Completion Report (iteration 3)
---
story_id: "U-062"
phase: "security-review"
verdict: "PASS"
iteration: 3
artifact_path: "sdlc/artifacts/build/sprints/sprint-019/U-062/review-security.md"
story_status: "review"
current_build_phase: "review"
blockers: 0
warnings: 11
errors: []
---
