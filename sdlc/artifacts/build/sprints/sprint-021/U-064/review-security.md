---
artifact_type: gate-result
gate: security-review
gate_id: "security-review"
checkpoint: quality-gate
story_id: "U-064"
verdict: PASS
iteration: 1
agent: "Security Reviewer"
timestamp: "2026-11-21T03:14:19Z"
base_commit: "de03e99b51a04d402b64cbd5c7f0d7d19e90f2a9"
blockers: 0
warnings: 0
---

# Security Review: U-064

## Summary

U-064 adds three tests and changes no production file. I confirmed that against disk: `git diff --quiet HEAD -- app/src/main k9mail-vendored` exits 0, and `git hash-object` of `PinnedCertificateSocketFactory.java` (`8a0fbb64...`) and `DefaultTrustedSocketFactory.java` (`d5439271...`) equals `git rev-parse HEAD:<path>` for both. AC-1 closes W-1 of the U-063 security review. The test drives the trust manager that `createSocket` installs, and the only thing that can reject it is `enrolledCert.checkValidity()`. AC-2 and AC-3 put the `catch (IllegalArgumentException)` under test. Both mutations were recorded as failing in the expected way. I found no Critical or High finding, no personal data in any file to be committed, and no new key material. The residual risks are Low or Info and are listed below.

## Verdict: PASS

Basis: production unchanged (verified by hash); AC-1 reaches the installed trust manager and fails only through the validity step (verified from code and from the M-N1 record); AC-2 and AC-3 fail for the right reason under M-N2; the mutation record is consistent with the clock, the file times and the compiled output; evidence hygiene is clean (my own search, count-only); `compiled-pipeline.json` is ignored.

## Findings

No Critical, High or Medium findings.

### F-1 (Low). AC-1 would not notice an end-date-only check; the not-yet-valid case is unprotected (DP-1 residual risk)

Location: `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:149`; test `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryExpiryTest.java:104-125`.

What: the test presents one certificate, `EXPIRED_HOST_CERT`, which ended in January 2020. Any replacement of `enrolledCert.checkValidity()` that still rejects an end date in the past passes the test. Examples: an explicit `getNotAfter().before(now)` test; `checkValidity(date)` with a fixed past date that is after January 2020; a check that tests only the end date. Only the removal or neutralisation of the call is caught (M-N1). A certificate that is not yet valid is not covered, because DP-1 ("leave out", product owner, 2026-11-20) means no future-dated fixture exists (every fixture in `TlsTestFixtures` starts on or before 2020-01-01).

Residual risk, plainly: if the CR-009 move rewrote the call as an end-date-only comparison, the pinned factory would accept an enrolled certificate before its start date and every test would stay green. Rating Low. Reasons: the single call handles both windows and a verbatim move keeps it; the certificate is one the user enrolled themselves on a private, unpublished build; an attacker would still need its private key to present it, so a not-yet-valid window is a hygiene control and not what stops an impostor (the fingerprint comparison does that and is covered by the existing tests); and the Javadoc of the new class (`:45-47`) states the omission honestly. The mutation M-N1 detects the realistic regression (deletion or an early return).

Recommendation: if the pinned code is ever rewritten rather than moved verbatim, add the future-dated case then (new key and certificate, offline, with provenance recorded).
Classification: defer-justified — the product owner decided "leave out" on DP-1 and the story says so; a fixture would add key material to the repository for a regression that a verbatim move does not produce.

### F-2 (Info). AC-1 cannot tell "check the enrolled certificate" from "check the presented certificate"

Location: `PinnedCertificateSocketFactory.java:134-149`; the test enrols and presents the same bytes.

What: with equal fingerprints the two certificates are byte-identical, so a change of `enrolledCert.checkValidity()` to `presented.checkValidity()` is behaviour-preserving on the accept path and cannot be told apart by any handshake. If the fingerprint comparison itself were removed, the existing rejection test (`handshake_certificateOtherThanEnrolled_isRejected`) and AC-1 assertion (ii) catch it. The same reasoning covers moving the validity call before the fingerprint step: both checks still run, the outcome is identical. A swallowed exception (try/catch that logs) is caught, because the handshake would complete. Nothing "after trust is granted" can exist: returning from `checkServerTrusted` is the grant, so a check moved after it would be dead code.
Recommendation: none.
Classification: defer-justified — equivalent behaviour, nothing to protect against.

### F-3 (Info). Fail-open on SNI when `SNIHostName` rejects the name: acceptable, and what it depends on

Location: `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:238-254`, called from `:177`; tests `DefaultTrustedSocketFactorySniTest.java` (added methods, lines 304-389 of the working file).

What: when the catch fires, no server name is sent. That is fail-open in the sense that a feature is skipped, but it is not a trust decision. Certificate and host-name verification do not read the SNI value. The trust manager is built from the `host` argument before the socket exists (`DefaultTrustedSocketFactory.java:154`, `TrustManagerFactory.get(host, port)`), and `SecureX509TrustManager.checkServerTrusted` runs `defaultTrustManager.checkServerTrusted(chain, authType)` and `verifyHostname(mHost, chain)` (`TrustManagerFactory.java:75-77`) against `mHost`. A skipped SNI therefore changes only which certificate a name-routed server may choose to present. If it presents a default certificate for another name, host-name verification fails closed.

Acceptable, on these conditions, all true today: (1) the host name is user configuration and not attacker-controlled input; (2) the verifier host comes from the `createSocket` argument, never from the socket's SNI state; (3) the catch handles only `IllegalArgumentException` from the SNI construction and from `setSSLParameters`, and does not wrap the trust path. The cost of a rejected name is functional (a virtual-host server may serve the wrong certificate and the connection fails), not a weakening. The tests do not assert condition (2); it is read from the code. A reviewer of any later change to `setSniHost` should re-check it, and the U-065 review is the next one.
Recommendation: none for this story.
Classification: defer-justified — a statement of why the catch is safe; no code or test change.

### F-4 (Info). The write-ahead order of the mutation log cannot be proved from the final file

Location: `sdlc/artifacts/build/sprints/sprint-021/U-064/evidence/mutation-log.md`.

What: each entry is written as one block, with the expected result stated before the observed result and the verdict and `restored` lines after. The file as it now stands cannot show that the block was written before the edit. The record is consistent with a write-ahead entry: `expected:` lines read as predictions and match in detail what was then observed; the entry times are from the clock (`03:03:27Z`, `03:04:42Z`); the evidence files for M-N1 were written at 21:04:32-33 local (03:04:32Z, which is between the entry and the `restored` time of 03:04:34Z; local time on this machine is UTC minus 6, checked with `date`); and those for M-N2 at 03:05:42Z, one second before its `restored` time 03:05:43Z. I treat it as sound. Not a gap in the evidence's trustworthiness.
Recommendation: none.
Classification: defer-justified — a limit of any after-the-fact review of a write-ahead log, no better evidence is obtainable.

### F-5 (Info). Assertion (iii) of AC-1 is redundant with (ii)

Location: `PinnedCertificateSocketFactoryExpiryTest.java:122-123`.

What: a fingerprint mismatch throws a plain `CertificateException`, which is not a `CertificateExpiredException`, so assertion (ii) already fails in that case. (iii) is harmless belt-and-braces and documents intent.
Recommendation: none.
Classification: defer-justified — harmless; editing it would only create churn.

## Judgement on the six questions

### 1. Does AC-1 close W-1?

Yes, for the realistic regression (deletion, bypass or neutralisation of the call).

- It reaches the installed trust manager: the socket comes from `new PinnedCertificateSocketFactory(...).createSocket(null, RIGHT_HOST_NAME, server.port(), null)` (test `:111-112`), which builds `new PinnedX509TrustManager()` at `PinnedCertificateSocketFactory.java:77` and installs it in a fresh `SSLContext` (`:78-80`). The seam `getTrustManagerForTesting()` is not used.
- The handshake fails because of the validity step and nothing else: the fixture is the same expired leaf both enrolled and presented (`certificate(EXPIRED_HOST_CERT)` at test `:105`, `serverContext(EXPIRED_HOST_CERT, EXPIRED_HOST_KEY_PKCS8, TRUSTED_CA_CERT)` at `:109-110`), so step 2 passes (fingerprints equal), and the pinned trust manager does no chain or host-name check, so there is no other rejection path in `checkServerTrusted` (`:126-150`). Assertion (ii) requires the direct cause to be `CertificateExpiredException`, which only `checkValidity()` produces; the plain `CertificateException`s at `:129` and `:142` do not satisfy it.
- M-N1 proves the converse. `evidence/mutations/M-N1-cases.txt` and `M-N1-message.txt` record exactly one failure in 13, `handshake_expiredEnrolledCertificate_isRejectedAsExpired`, `java.lang.AssertionError: the handshake must be rejected, but it completed`. The 12 other tests pass, and the run compiled. So with the call neutralised the handshake does complete: the server, the key, the protocol and the client path are all otherwise fine, which rules out an unrelated failure also being present in the unmutated run. The failing line is the `fail()` at test `:117`, not a timeout or compile error.
- Weakenings it would not notice: see F-1 (end-date-only) and F-2 (presented vs enrolled, reorder). Not-yet-valid: F-1, rated Low.
- One environmental dependency: the precondition at `:106-107` asserts the machine clock is after January 2020, with a message; a wrong machine clock fails visibly, not silently.

### 2. Do AC-2 and AC-3 protect the safety net?

Yes. AC-2 asserts for each of `bad_host.example`, `host.example.`, `-x.example` that the guard returns false (so the name reaches `SNIHostName`), that `setSniViaSSLParameters` throws nothing, and that `setSSLParameters` is never called. AC-3 proves the same property end to end through `createSocket` with an unconnected socket (no connect, no resolution). M-N2 (the try/catch removed, guard kept) turned exactly those two red and left the 12 existing SNI and 5 JDK-provider tests green. The recorded failures show `IllegalArgumentException: Contains non-LDH ASCII characters` for the underscore name and a server-name message for the others, so the three names do reach the constructor; AC-3 fails with `IllegalArgumentException`, which also shows the Robolectric run takes the `SDK_INT >= N` branch (`robolectric.properties` has `sdk=29`) and AC-3 is not passing vacuously. In the compiled output both `IllegalArgumentException` and the "Skipping SNI" string are present again after the restore. The security angle of the catch is in F-3. A limit: AC-2 and AC-3 show which names the desktop JDK rejects; Android's own `SNIHostName` may reject a different set (the story says so).

### 3. Mutation hygiene

- Each of M-N1 and M-N2 has a block with file, hash-before equal to the base blob, clean check (`git diff --quiet` exit 0, empty porcelain), the single edit, the class set, the expectation, the result and a `restored` line with the hash and an empty porcelain and stat. Each ran once (one failing run each). No mutation entry says `APPLYING` without a `restored` line.
- Restore proof by blob hash: I re-hashed both files now; both equal the base blobs and `git diff --quiet HEAD -- app/src/main k9mail-vendored` exits 0.
- Nothing present when a gate ran: the I0 gate ran at about 02:58Z, before the first mutation (03:03:27Z); the I6 gate ran after the last restore (03:05:43Z). Both gates are logged with exit 0 and no mutated task in between except `testDebugUnitTest` runs inside the mutation windows.
- Leak into build output: the compiled debug `PinnedX509TrustManager.class` was rebuilt at 03:05:24Z (after the M-N1 restore at 03:04:34Z) and contains the `checkValidity` reference; the compiled `DefaultTrustedSocketFactory.class` (debug 03:06:14Z, after the M-N2 restore at 03:05:43Z) contains `IllegalArgumentException` and the "Skipping SNI" text. The release classes date from 17:50-17:51 local on 2026-11-20, before the story's first mutation, so no mutated source reached `assembleRelease` output. The mutation runs used only `testDebugUnitTest`. I did not recompute the APK hashes; I read the build output times.
- Leak into a commit: nothing is committed or staged by the story (`git status` shows the two test paths and sdlc files only; HEAD is still `de03e99b`). The Gradle user-level build cache could hold mutated outputs keyed by input; they are inert. That is the same limit as U-063.
- Could a mutation leak? Only through a lost session or an unreverted file; the recovery note in the log forbids any action before a hash comparison. No such interruption happened.

### 4. Key material and secrets

The new class and the SNI diff use only existing fixtures (`EXPIRED_HOST_CERT`, `EXPIRED_HOST_KEY_PKCS8`, `TRUSTED_CA_CERT`, `RIGHT_HOST_NAME`) by static import; no PEM header, no Base64 key block, no keystore file, no password, token or secret word in the new test (count 0 in each case), and none in the workspace files. Names are `imap.right.test` (reserved `.test` name), `bad_host.example`, `host.example.`, `-x.example` (reserved `.example`). No network access and no resolution: the client connects through `LoopbackTlsTestServer.connect(client, server.port())` with a port chosen by the operating system; my search of the new class and the added lines for `getByName`, `getAllByName`, `localhost`, `127.0.0.1`, `Thread.sleep` and `new InetSocketAddress("` found nothing. AC-3 creates an unconnected socket. The test touches no JVM-global state (no verifier, no `defaultTrustManager`).

### 5. Evidence hygiene (my own search, count only)

I searched the new test file, the added lines of the SNI diff, and the 25 other files of the story's workspace (implementation log, plan, reviews, all evidence and mutation files) with the user name and machine host name read from the environment at run time and not written anywhere. Results: e-mail pattern 0; user name 0; host name 0; user-profile path patterns 0; private-key or certificate markers 0. The one line the story's own scan reports (`I8-scan.txt`, the story file's IC-8 description at line 83) is the criterion text itself and holds no personal data. `compiled-pipeline.json` is excluded from the count by design and `git check-ignore -v` shows it ignored by `.git/info/exclude:37`; `git status --ignored` shows it as `!!`. The workspace folder as a whole is untracked and nothing in it matches.

### 6. Forward look: U-065

What in these tests protects the U-065 change:

- `handshake_expiredEnrolledCertificate_isRejectedAsExpired` fails if U-065's rework of the pinned `createSocket` replaces or bypasses the pinned trust manager (for example by routing the pinned socket through the default factory's trust path: the cause would no longer be `CertificateExpiredException`), or if the context handling stops installing the pinned manager. It also guards the accept/reject behaviour of the validity step if the manager is edited in the same change.
- AC-2/AC-3 protect the vendored `setSniViaSSLParameters` and `createSocket` of the default factory. If U-065 makes the pinned `createSocket` call the same static helper, AC-2 protects that helper; AC-3 only exercises the default factory.

What they do not protect:

- A copy of the SNI or hardening code inside the pinned class (or a new app class): the catch in the copy has no test from U-064. The U-065 story already plans this as row S5 (`bad_host.example`, `host.example.`, `-x.example` on the pinned `createSocket`). The review should confirm it has its own mutation.
- Anything below TLS 1.3, a floor of TLS 1.2, cipher order or blocklist on the pinned factory: the handshakes negotiate TLS 1.3 and the new tests assert nothing about enabled protocols or suites. That is U-065's own coverage.
- Session resumption or a cached context (the new class's Javadoc says it relies on a fresh context per `createSocket`; a cache would not turn AC-1 red).
- A not-yet-valid certificate and an end-date-only rewrite (F-1).

First things for the U-065 security review: (1) read the pinned `createSocket` after the edit and confirm the trust manager installed is still `PinnedX509TrustManager` and nothing from the hardening code touches the trust path; (2) confirm the host used for SNI on the pinned socket does not change what is verified (the pinned path verifies no host name by design, so SNI must not become a reason to add or remove a check); (3) confirm the protocol floor cannot accidentally disable the only protocol the loopback test server negotiates, and that a floor of 1.2 is enforced by a test with a mutation; (4) confirm the pinned rejected-name test (S5) exists and fails when its catch is removed; (5) re-run the hygiene and hash checks, and check that no `@Ignore` was added to U-063 or U-064 tests to make the change pass.

## Security Checklist

- [x] No hardcoded credentials or secrets (none added)
- [x] Input validation on all user inputs (not applicable to tests; production unchanged)
- [x] Output encoding prevents injection attacks (not applicable)
- [x] Authentication tokens handled securely (not touched)
- [x] Authorization checks on all protected resources (not touched)
- [x] Sensitive data encrypted at rest and in transit (the validity check of the pinned path is now pinned by a test; see F-1)
- [x] Error messages do not leak internal details (not touched)
- [x] Dependencies have no known critical vulnerabilities (none added; build files and `gradle/verification-metadata.xml` unchanged, `vm-exit=0` in `evidence/I6-checks.txt`)

## Runtime Claims Verified

- [x] `## Runtime Claims` inventory read in `implementation-log.md`: declared empty ("The story makes no runtime-provable claim of the crash, concurrency, timing, filesystem or hook classes."). The declaration is present, not omitted.
- [x] Every security-relevant runtime-behaviour AC has a real probe: AC-1 is a real loopback TLS handshake through `createSocket`, with raw per-test output (`evidence/I2-pinned-cases.txt`, three identical runs in `evidence/I5-run*-cases.txt`) and the M-N1 failure; AC-2 and AC-3 are no-exception properties shown by unmutated passes and by the M-N2 failures (`evidence/mutations/M-N2-*.txt`). I judged the empty inventory complete: no AC claims a crash, concurrency, timing or filesystem behaviour.
- [x] Probe designs are adequate: topology (real socket and `SSLContext` on both sides), failure mode (a real expired certificate presented and rejected), precondition (fingerprint equality by construction, clock precondition in the test, the converse shown by M-N1 where the handshake completes), falsifiability (two detected mutations).
- [x] No escape-hatch entries.
- [x] Inventory completeness audited against the security-relevant runtime-behaviour ACs: no gap. This is a read-only audit; I started no probe and ran no Gradle.
- [x] No probe wrote into `../amp`. This project has no sibling plugin repository; the mutations touched two in-repo production files and were restored.
- UNVERIFIED (escape-hatch) claims: None. Outside any probe, as stated by the story: Android's own `SNIHostName` rules, device provider behaviour, the device clock.

## What I verified myself

| # | Check | Result |
|---|-------|--------|
| 1 | `git diff --quiet HEAD -- app/src/main k9mail-vendored` | exit 0 |
| 2 | `git hash-object` vs `git rev-parse HEAD:<path>` for the two mutated files | both pairs equal (`8a0fbb64...`, `d5439271...`) |
| 3 | HEAD | `de03e99b51a04d402b64cbd5c7f0d7d19e90f2a9`, equal to the base commit |
| 4 | Read in full: the new test class; the pinned factory `:60-169`; `DefaultTrustedSocketFactory.java:150-265`; `TrustManagerFactory.java:40-160`; the SNI diff (87 added, 0 removed, one hunk); the story, the implementation log, the mutation log, `I6-checks.txt`, `I8-scan.txt`, both mutation message files, `M-N1-cases.txt` | read |
| 5 | Test count: tracked files give 712 (710 + 2 added methods); the untracked new class holds 1 `@Test`; total 713 as IC-4 states | consistent |
| 6 | Compiled output times and contents (debug and release) against mutation and restore times | consistent, see section 3 |
| 7 | Hygiene searches, count-only, environment-derived names | all zero, see section 5 |
| 8 | `git check-ignore -v` on `compiled-pipeline.json` | ignored via `.git/info/exclude` |
| 9 | Gradle | not run; the evidence and hashes settled every question |

## Recommendations

1. Accept the story. Commit only its own paths (the new test class, the modified SNI test, the story and its workspace without `compiled-pipeline.json`) per IC-12.
2. Carry F-1 and the U-065 first-look list (section 6) into the U-065 review.
3. No fix-in-cycle item is raised; every finding is Low or Info and defer-justified.
