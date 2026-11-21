---
artifact_type: qa-results
story_id: "U-064"
gate_id: "qa-validation"
checkpoint: "quality-gate"
verdict: "PASS"                        # PASS | FAIL
agent: "QA Analyst"
timestamp: "2026-11-21T03:32:44Z"
ac_total: 15                           # AC-1..AC-3 plus IC-1..IC-12
ac_passed: 15                          # 13 MET, 2 MET WITH DEVIATION (IC-8, IC-12; each named and judged below)
ac_failed: 0
tests_run: 713                         # my full gate run of record on the committed tree (JUnit XML of that run only)
tests_passed: 711                      # 0 failures, 0 errors, 2 skipped (the two baseline skips)
---

# QA Validation: U-064

## Verdict: PASS

## Summary

U-064 adds three tests (one new class, two methods appended to the SNI test class) and changes no production, vendored, build or dependency file. I validated the committed tree at `b2c2dfd8` (HEAD of `sdlc/modernization-plan`, equal to `origin/sdlc/modernization-plan`) against its parent `bd1d5398`. All 3 acceptance criteria and 12 integrity criteria are met: 13 outright, 2 with a deviation that I name and judge harmless. No criterion is unmet.

What I derived myself:
- **Full gate set on the committed tree, with JUnit XML from my own run.** Exit 0, daemon JVM 25.0.5.1; 78 result files, 713 tests, 0 failures, 0 errors, 2 skipped (XML timestamps 03:27:34Z to 03:28:34Z, my run). The three new tests passed, and so did the 12 old SNI tests, the 8 pinned tests, the 4 pinned create-socket tests and the 5 JDK-provider tests.
- **The literal commit-based forms** of IC-2, IC-4 and IC-8 over `bd1d5398..HEAD`.
- **A mutation spot check (M-N1), re-applied by a guarded script, run, and restored, with hash proof.** The named test failed with the named message. Production then recompiled, and a post-restore run passed 13 of 13.
- **Evidence honesty.** Clock order across all evidence files and the mutation-log entries. Counts consistent. Count-only hygiene searches using names read from the environment.

## Acceptance and Integrity Criteria Results

| # | Criterion | Check I ran | Observed | Result |
|---|-----------|-------------|----------|--------|
| AC-1 | Pinned factory rejects expired enrolled cert at a real handshake | Read `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryExpiryTest.java:103-125`; my gate XML; M-N1 re-applied | Factory `new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(), RIGHT_HOST_NAME, enrolled)` with `enrolled = certificate(EXPIRED_HOST_CERT)` (`:105`, `:111-112`). Server `start(IMPLICIT_TLS, serverContext(EXPIRED_HOST_CERT, EXPIRED_HOST_KEY_PKCS8, TRUSTED_CA_CERT))` (`:109-110`). Client from `createSocket(null, RIGHT_HOST_NAME, server.port(), null)`, connected via `LoopbackTlsTestServer.connect` (`:113`), then `startHandshake()` (`:116`). (i) `catch (SSLException e)` (`:118`); a different exception type escapes and fails the test. (ii) direct `getCause()` `isInstanceOf(CertificateExpiredException.class)` (`:119-121`). (iii) the cause message `doesNotContain("Pinned certificate mismatch")` (`:79`, `:122-123`). The explicit `fail("the handshake must be rejected, but it completed")` is at `:117`. The seam `getTrustManagerForTesting()` is not used (grep: 0 in the file). It passed in my gate. Under M-N1 it failed with exactly that message (see Spot Check). | MET |
| AC-2 | `setSniViaSSLParameters` survives names `SNIHostName` rejects | Read `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java:340-373` | Method `setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI` (`:341`) loops over `{"bad_host.example", "host.example.", "-x.example"}` (`:342`). It uses a fresh `mock(SSLSocket.class)` per name, with `getSSLParameters()` returning `new SSLParameters()`. (i) `isIpOrBlankHostname` called by reflection `isFalse()` (`:343-351`). (ii) the call at `:356`, with any `RuntimeException` collected into `escaped` and asserted empty at `:369-372`. (iii) `verify(mockSocket, never()).setSSLParameters(any())` (`:362`). Messages: (i) names the host; the `verify` wrapper names the host; the final assertion lists all names plus `escaped` entries of the form `name -> exception`. So "every message names the host name" holds. It passed in my gate. The developer's M-N2 record shows all three names escaping (`evidence/mutations/M-N2-message.txt`). | MET |
| AC-3 | `createSocket` survives a rejected name end to end | Read `DefaultTrustedSocketFactorySniTest.java:375-389`; production `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:175-177`, `:191-197` | `new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()).createSocket(null, "bad_host.example", 993, null)` (`:381-383`). `assertThat(socket).isInstanceOf(SSLSocket.class)` (`:385`). `socket.close()` in `finally` (`:386-387`). Null base socket, so the socket is unconnected (`:170`). Path: `createSocket` then `setSniHost` (`:177`), then the `SDK_INT >= N` branch (`:192-197`), then `setSniViaSSLParameters`. It passed in my gate. The developer's M-N2 record shows `IllegalArgumentException: Contains non-LDH ASCII characters`, which proves the branch is reached and the test is not vacuous. | MET |
| IC-1 | Baseline evidence before any edit | Read `evidence/I0-baseline.txt`, `I0-gates.txt`, `I0-junit-totals.txt`; `git grep ... bd1d5398` count; `git rev-parse` of both blobs at `de03e99b`, `bd1d5398`, HEAD | Baseline written 02:58:48Z (file time 02:58:50Z): count 710, `prod-clean-exit=0`, blobs `8a0fbb64...` and `d5439271...`, gate `exit=0`, totals `files=77 tests=710 failures=0 errors=0 skipped=2`. My count at `bd1d5398` is 710. The baseline names `de03e99b` because the sprint-plan commit `bd1d5398` was made later. `git diff --stat de03e99b..bd1d5398` touches only `sdlc/` files (8 files), and both production blobs are identical at all three commits, so the baseline is valid for `bd1d5398`. | MET |
| IC-2 | Files-touched rule | `git diff -M --name-status bd1d5398..HEAD`; `git diff --stat bd1d5398..HEAD -- app/src/main k9mail-vendored build.gradle app/build.gradle settings.gradle gradle`; `git diff --quiet bd1d5398..HEAD -- gradle/verification-metadata.xml`; SNI diff | 30 paths. Every path is either `app/src/test/...` (2) or the story or its `sprint-021/U-064/` workspace (filtering them out leaves 0 lines). The story shows as `R098` from `sdlc/artifacts/stories/` (frontmatter status, sprint and phase lines, plus the "As built" note). App-test paths: `A PinnedCertificateSocketFactoryExpiryTest.java`, `M DefaultTrustedSocketFactorySniTest.java`. SNI file: numstat `87 0`, 1 hunk (`@@ -301,6 +301,93 @@`), 0 removed lines, 0 `@Ignore` added. Fixtures and helper diff: 0 files. Production, build and gradle stat: empty. `vm-exit=0`. No dependency added. | MET |
| IC-3 | Gates green before and after, no mutation present | Evidence `I0-gates.txt`, `I6-gates.txt`; my own gate run (see My Gate Run) | Developer: I0 exit 0 and I6 exit 0, both on JDK 25.0.5.1. I6 ran after the last restore: test XML at 03:08-03:09Z, M-N2 restored at 03:05:43Z. Mine: exit 0, JDK 25.0.5.1, no `MSYS_NO_PATHCONV`, timeout 600000 ms, production verified clean right before. | MET |
| IC-4 | Count 710 to 713; 713 run, 0 failures, 2 skipped | Authoritative count at `bd1d5398` and HEAD; my JUnit XML | `count-base=710`, `count-head=713`. The added `@Test` lines are the three new methods only (SNI file now 14 `@Test`, new class 1). XML: 713 run, 0 failures, 0 errors, 2 skipped (`AppTest.shouldGetVersionName`, `AuthPreferencesTest.u012_robolectricSmoke_encryptedPrefsSecretStore_migrateFromPlaintext`, the baseline pair). No IC-11 skip. DP-1 option A, so 713, not 714. | MET |
| IC-5 | No coverage floor change; name contains `Test`; no helper | IC-2 stat (empty for `app/build.gradle`); `git diff --name-status --diff-filter=A bd1d5398..HEAD -- app/src/test` | The only added app-test file is `PinnedCertificateSocketFactoryExpiryTest.java`, whose name contains `Test`. `app/build.gradle` is unchanged. My gate's `jacocoTestCoverageVerification` passed. No helper class (the only helper is the private `causeChain` method, `PinnedCertificateSocketFactoryExpiryTest.java:127-140`). | MET |
| IC-6 | Determinism | Literal IC-6 grep; the same pattern over added lines; `evidence/I5-*`; my runs | Repo-wide literal form: 2 hits, both in `mail/transport/K9MailTransportCreateOpenTest.java` (existing comment text, not touched). Added lines: 0. The connect goes through `LoopbackTlsTestServer.connect` (`PinnedCertificateSocketFactoryExpiryTest.java:113`). There is no sleep or poll. Socket and server are closed in `@After` (`:84-96`), the AC-3 socket in `finally`. Developer's three `--rerun` runs were identical (`I5-checks.txt` `diff12-exit=0`, `diff13-exit=0`). Independently, both classes passed in each of my four suite-level runs (03:21Z, 03:23Z, 03:25Z, plus the post-restore pinned run). | MET |
| IC-7 | Two logged mutations by protocol | Read `evidence/mutation-log.md` and `evidence/mutations/*`; file times; my re-application of M-N1 | M-N1: APPLYING 03:03:27Z, hash-before = base, clean checks, edit exactly as IC-7, 13 cases 12 pass 1 fail (the AC-1 test, the named message), restored 03:04:34Z with hash, empty porcelain and empty stat. Case files 03:04:32-33Z, inside the window. M-N2: APPLYING 03:04:42Z, the M-A1c edit, 19 cases 17 pass 2 fail (exactly AC-2 and AC-3, `IllegalArgumentException`; all three names escaped), restored 03:05:43Z. Case files 03:05:42Z, inside the window. Both compiled; neither was a compile error or a timeout. No `APPLYING` without `restored`. My re-application of M-N1 reproduced the result exactly. | MET |
| IC-8 | Evidence hygiene | Literal IC-8 grep on `git diff bd1d5398..HEAD`; count-only searches with names read from the environment | One hit outside the reserved names: `sdlc/artifacts/build/sprints/sprint-021/U-064/review-code.md:51` quotes the path pattern `C:\Users\<name>\.claude\plugins\...` with a literal placeholder. The story's own IC-8 line 83 is unchanged context and does not appear in the diff. Environment user name and host name: 0 in the diff, 0 in tracked workspace files, 0 in the story, 0 in the new test. E-mail pattern in evidence: 0. `Users\|AppData\|.gradle caches` in evidence: 0. The untracked `compiled-pipeline.json` holds the user name and is excluded (`.git/info/exclude:37`, `git ls-files` 0). | MET WITH DEVIATION |
| IC-9 | No global state | `setDefaultHostnameVerifier\|defaultTrustManager` over the added lines | 0 hits. The only reflection is `getDeclaredMethod("isIpOrBlankHostname", String.class)` (`DefaultTrustedSocketFactorySniTest.java:343-345`). | MET |
| IC-10 | Existing tests untouched and passing | SNI diff (0 deletions, 1 hunk); duplicate method-name check; my XML per class | No removed line, so the 12 SNI methods and their comments are byte-identical. Duplicate `public void` names in the SNI file: 0. The pinned tests (8) and pinned create-socket tests (4) are not in the diff. My XML: SNI 14/14, pinned 8/8, create-socket 4/4, all passed. | MET |
| IC-11 | Defect rule | Diff for `@Ignore`; my run | Not triggered: the new tests pass on unmutated code, 0 `@Ignore` added, no bug filed, no production edit. | MET |
| IC-12 | One commit limited to the story's paths; clock timestamps; counts stated once | `git log`, `git diff --name-only bd1d5398..HEAD`, timestamps; story text | One commit `b2c2dfd8` (2026-11-20T21:19:34-06:00 = 03:19:34Z), pushed (HEAD = origin), limited to the story's paths (IC-2). Product-owner permission: recorded in my delegation ("Commit and push (Recommended)", 2026-11-20); I cannot observe it in the tree and take it as delegated fact. Clock order holds throughout (see Evidence Honesty). Counts: the developer's addition refers to IC-4 by reference. But the story text itself repeats 710 and 713 outside IC-1 and IC-4: the orchestrator's DP-1 note at `:191` and the original Notes at `:202`. | MET WITH DEVIATION |

## Deviations Judged

- **IC-8, one placeholder path in `review-code.md:51`.** The literal grep reports the code reviewer's description of W1: the text `C:\Users\<name>\.claude\plugins\...`. It is a pattern with the placeholder `<name>`, not a path of any machine. My environment-name searches find 0 user-name and 0 host-name hits in every committed file. Judgement: no personal data is disclosed, so the intent of IC-8 holds. The literal "shows nothing" does not. Harmless. A future reviewer could write "a user-profile path" instead of quoting the shape.
- **IC-12, counts repeated in the story text.** "Each count appears once in the story" is not literally true. The figures 710 and 713 also appear in the product owner's DP-1 note (recorded by the orchestrator, `:191`) and in the story's original Notes (`:202`). All occurrences agree with IC-4 and with the measured values. The developer's own note refers to IC-4. Judgement: no inconsistency exists, so the rule's purpose (no drifting counts) is served. Harmless.
- **Base naming (not a criterion deviation, recorded for clarity).** The implementation log, the mutation log and both reviews name `de03e99b` as their base. The commit-based base is `bd1d5398`. I proved the two are equivalent for this story: the commits between them touch only `sdlc/`, and both production blobs and the test tree are identical at both.

## My Gate Run

- **Gate run of record (2026-11-21T03:25:37Z to 03:28:42Z):** `./gradlew --init-script <scratch>/junit-xml.init.gradle :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease --rerun-tasks`. It ran in the foreground in a shell without `MSYS_NO_PATHCONV`, with timeout 600000 ms; the tool moved it to the background at 180 s, and I waited for the completion notice without polling and without starting another Gradle run. Results: `exit=0`, `BUILD SUCCESSFUL in 3m 4s`, `U063-DAEMON-JVM: java.version=25.0.5.1`, all four tasks executed. The init script is a read-only copy of `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/junit-xml.init.gradle`. It only sets `reports.junitXml.required = true`.
- **JUnit XML of that run:** `files=78 tests=713 failures=0 errors=0 skipped=2`, timestamps 03:27:34Z to 03:28:34Z.
  - `PinnedCertificateSocketFactoryExpiryTest` 1/1. `handshake_expiredEnrolledCertificate_isRejectedAsExpired` passed.
  - `DefaultTrustedSocketFactorySniTest` 14/14, including `setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI` and `createSocket_nameRejectedBySniHostName_doesNotThrow`.
  - `PinnedCertificateSocketFactoryTest` 8/8, `PinnedCertificateSocketFactoryCreateSocketTest` 4/4, `DefaultTrustedSocketFactoryJdkProviderTest` 5/5.
- **Declared deviation from "run the gate set once": three suite-level runs, not one.**
  1. My first plain gate run (03:21:12Z to 03:22:36Z) exited 0. But `build.gradle:29-30` sets `reports.junitXml.required = false`, so it wrote no XML. The XML files on disk were the developer's (timestamps 03:08-03:09Z), and I refused to use them.
  2. I deleted those stale XMLs from the ignored `app/build` directory (`.gitignore:21`). I then ran `:app:testDebugUnitTest --rerun` (exit 0, 03:23:37Z to 03:25:09Z), which confirmed that no XML appears without the init script.
  3. The run of record above, with XML switched on.

  All three were green, so the extra runs strengthen the determinism reading (IC-6) and change no verdict. No mutation was present during any of them: production was verified clean right before.

## Mutation Spot Check (story verification step 7, IC-7)

M-N1 was re-applied by a script, not an editor: `<scratch>/apply_mn1.py`, outside the repository. It did the following:
- **Refusal guards.** `git diff --quiet HEAD -- app/src/main k9mail-vendored` exit 0, empty porcelain, and `git hash-object` = `git rev-parse HEAD:<file>` = `8a0fbb64fb171624591fbebafb818555f28efd58`. The script refuses to continue if any of these fails.
- **Substitution.** Exactly one occurrence of `enrolledCert.checkValidity();`, required to be on line 149. It was replaced by `if (false) { enrolledCert.checkValidity(); }` (the IC-7 text), in binary mode so line endings were preserved.

The apply, the run and the restore ran in one shell command, so the restore ran whatever Gradle did:

| Step | Time (clock) | Observed |
|------|--------------|----------|
| apply | 03:29:21Z | `hash-before 8a0fbb64...`, `APPLIED M-N1 at line 149 hash-after 9560d5e8...`, diff `-            enrolledCert.checkValidity();` / `+            if (false) { enrolledCert.checkValidity(); }` |
| run | 03:29:22Z | `./gradlew --init-script <scratch>/junit-xml.init.gradle :app:testDebugUnitTest --tests ...ExpiryTest --tests ...CreateSocketTest --tests ...PinnedCertificateSocketFactoryTest`; `:app:compileDebugJavaWithJavac` ran (compiled); `13 tests completed, 1 failed`; `gradle-exit=1` |
| failure | | `PinnedCertificateSocketFactoryExpiryTest#handshake_expiredEnrolledCertificate_isRejectedAsExpired`, type `java.lang.AssertionError`, message `java.lang.AssertionError: the handshake must be rejected, but it completed`; CreateSocket 4/4 and existing pinned 8/8 passed |
| restore | 03:30:05Z | `git checkout -- app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` |
| proof | 03:30:05Z | `hash-object=8a0fbb64fb171624591fbebafb818555f28efd58 head-blob=8a0fbb64fb171624591fbebafb818555f28efd58`; `porcelain=[]`; `stat=[]`; `diff-quiet-exit=0` |
| post-restore run | 03:31:37Z to 03:32:23Z | clean check first; same three classes; `:app:compileDebugJavaWithJavac` re-ran on the restored source (so no class compiled from the mutation stays in the debug build output); 13/13 pass, exit 0 |

Result: **DETECTED, reproduced exactly** as the developer's log states. The full gate set never ran while the mutation was applied. The vendored file was never touched (its blob `d543927166b3c5e1da0aaa8ab584072a43bf7521` equals HEAD).

## Literal Integrity Checks (commit-based)

| Command | Output |
|---------|--------|
| `git diff --name-only bd1d5398..HEAD \| grep -vE "^app/src/test/\|^sdlc/artifacts/build/sprints/sprint-021/U-064"` | (empty); 30 paths in total |
| `git diff --stat bd1d5398..HEAD -- app/src/main k9mail-vendored build.gradle app/build.gradle settings.gradle gradle` | (empty) |
| `git diff --quiet bd1d5398..HEAD -- gradle/verification-metadata.xml; echo $?` | `0` |
| `git grep -h "@Test" bd1d5398 -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' \| grep -c "@Test"` | `710` |
| `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' \| grep -c "@Test"` | `713` |
| `git diff --numstat bd1d5398..HEAD -- app/src/test` | `141 0 ...ExpiryTest.java`, `87 0 ...DefaultTrustedSocketFactorySniTest.java` |
| SNI diff hunks / removed lines | `1` / `0` |
| `git diff bd1d5398..HEAD \| grep -nEi "@[a-z0-9-]+\.[a-z]{2,}\|Users[/\\\\]\|AppData"` | one line, the `review-code.md:51` placeholder (IC-8 deviation) |
| `git rev-parse HEAD origin/sdlc/modernization-plan` | both `b2c2dfd8d7f0f5de8627575e160215b24b7cc5d9` |
| `git status --porcelain` (end) | `?? sdlc/.skill-context.json` only (pipeline marker), before this file was written |

## Evidence Honesty

- **Clock order (file modification times, UTC).** `I0-baseline` 02:58:50, then `I0-gates` and `I0-junit-totals` 03:00:30, `I1` 03:00:46, `I2` 03:02:05, `I3-sni-diff-check` 03:02:28, `I3-sni-cases` 03:03:21. Then the M-N1 window (03:03:27 to 03:04:34) holds `M-N1-*` (03:04:32-33), and the M-N2 window (03:04:42 to 03:05:43) holds `M-N2-*` (03:05:42). After that: `I5-run1/2/3` 03:06:33, 03:07:12, 03:07:53; `I5-checks` 03:07:54; `I6-gates` and `I6-junit-totals` 03:09:27; `I6-checks` 03:09:35; `mutation-log.md` 03:09:46 (finalised); implementation log stamped 03:09:48Z; `I8-scan` 03:10:50. Then the code review at 03:11:54Z, the security review at 03:14:19Z, the story update at 03:15:21Z and the commit at 03:19:34Z. Strictly ordered. No mutation window overlaps a gate.
- **Write-ahead.** It cannot be proved from a finished file (security F-4). The file times are consistent with it.
- **Counts consistent.** Story IC-1 and IC-4 (710 to 713), plan (`plan.md:21`, `:61`, `:213`, `:608`), the implementation log (by reference), `I0-baseline` 710, `I0-junit-totals` 710, `I6-checks` 713 and 710, `I6-junit-totals` 713, and my count and XML (710, 713, 713). Case counts agree: 13 = 1 + 4 + 8 and 19 = 14 + 5.
- **Every deviation declared.** The developer declared "four" versus five fully qualified names (implementation log I3). I found no undeclared deviation in the developer's work. The two criterion deviations above are textual, and the base naming is explained.
- **Hygiene.** I ran count-only searches with the user name and host name read from the environment (not written anywhere): 0 in every committed file (diff, tracked workspace files, story, tests). `evidence/I8-scan.txt` agrees (files=0 for each scan).

## Integration Path Verification

| New Component | Expected Entry Point | Dispatch Mechanism | Path Tested | Result |
|---------------|---------------------|--------------------|-------------|--------|
| `PinnedCertificateSocketFactoryExpiryTest` (test class) | `:app:testDebugUnitTest` | Gradle JUnit 4 discovery, `@RunWith(RobolectricTestRunner.class)` (`:76`) | Found and run in my gate (1 case in the XML) | PASS |
| AC-1 production path exercised | `PinnedCertificateSocketFactory.createSocket` (`app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:75-86`) | direct call | `createSocket` installs `new PinnedX509TrustManager()` (`:77`) in a fresh `SSLContext` (`:78-80`); handshake reaches `checkServerTrusted` (`:126-150`); fingerprints equal (`:141-145`); `checkValidity()` (`:149`) throws. Proven by M-N1 flipping it. | PASS |
| AC-2 and AC-3 methods | `:app:testDebugUnitTest` | same | Run in my gate (14 SNI cases) | PASS |
| AC-3 production path exercised | `DefaultTrustedSocketFactory.createSocket` (`k9mail-vendored/.../DefaultTrustedSocketFactory.java:170-179`) | direct call | `:177` `setSniHost`, then `:192-197` (sdk 29), then `setSniViaSSLParameters` `:238-254`; guard `:239` false; `SNIHostName` throws at `:246`; caught at `:248-253`. Proven non-vacuous by M-N2 in the developer's log. | PASS |

No production component is created or modified, so there is no registry or wiring to check.

## Behavioral Contract Verification

| Contract | Expected Behavior | Actual Behavior | Result |
|----------|-------------------|-----------------|--------|
| CNTR-MODERNIZATION-001 pinned producer: validity-window check (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md:96`, error table `:197`) | Certificate outside its window: fail closed | AC-1: handshake fails, direct cause `CertificateExpiredException`; with the check disabled the handshake completes (M-N1). End-of-window only; start-of-window not covered (DP-1). | PASS (scope as decided) |
| BUG-015 safety net: name passing the guard but rejected by `SNIHostName` | No exception, no SNI set | AC-2: no throw, `setSSLParameters` never called, for 3 names; AC-3: `createSocket` returns an `SSLSocket` | PASS |
| Production behaviour unchanged | Both blobs equal at `bd1d5398` and HEAD | `8a0fbb64...` and `d5439271...` at both; working tree equal after my spot check | PASS |

## Requirement Traceability

| Referenced Doc | Scope in Doc | Scope Implemented | Gap |
|----------------|--------------|-------------------|-----|
| Story gap 1 (U-063 security W-1, SC-6) | A test that fails if `enrolledCert.checkValidity()` is removed | AC-1, detected by M-N1 (developer's log and my re-run) | None. The not-yet-valid case was left out by product-owner decision DP-1 ("leave out", 2026-11-20). |
| Story gap 2 (U-063 code-review W1, M-A1c) | A test that fails if the `catch (IllegalArgumentException)` is removed with the guard kept | AC-2 and AC-3, detected by M-N2 (developer's log) | None |
| CR-009 "tests that must exist" (`requirement_source: cr:CR-009`) | The baseline must protect the validity check and the SNI catch before the move | Both are now protected | None for this story; the move itself is CR-009's |
| CNTR-MODERNIZATION-001 `:96`, `:197` | Validity-window check, fail closed | End-date case tested | Start-date case open (DP-1, security F-1, Low) |

The story lists no `requirements`, `design_docs` or `integration_contracts` in its frontmatter. The contract is read-only context.

## Review Artifacts

- `review-code.md`: W1 `Classification: resolved in cycle` (`:54`), with a resolution line (`:55`). N1 to N5 are `defer-justified`.
- `review-security.md`: F-1 to F-5 are all `defer-justified`. No warnings.
- `grep -lE "Classification: *fix-in-cycle"` over the workspace `.md` files: 0 files. No open fix-in-cycle item.
- Plan review, contract verification and expert-depth check are all PASS.

## Edge Cases Tested

- **Machine clock before 2020-01-31.** AC-1 has a labelled precondition (`PinnedCertificateSocketFactoryExpiryTest.java:106-107`), so this fails with a message rather than silently.
- **Handshake failing for another reason** (connection, protocol, fixture). Assertion (ii) requires `CertificateExpiredException`, and M-N1 shows the handshake otherwise completes, so an unrelated failure cannot pass the test.
- **A name accepted by `SNIHostName`.** Assertion (iii) of AC-2 would fail with "so SNIHostName accepted it here". `evidence/I1-first-step.txt` shows all three names are rejected on JDK 25.0.5.1 and `imap.right.test` is accepted.
- **A widened guard.** AC-2 (i) fails by design.
- **`createSocket` throwing.** AC-3 fails with the exception. `socket` is assigned before `try`, so `finally` never sees null.

## What Remains Open (do not block this story)

**For U-065 (changes both factories: TLS 1.2 floor, pinned-factory parity):**
- AC-2 depends on three seams: the public static `setSniViaSSLParameters(SSLSocket, String)`, the package-private `isIpOrBlankHostname` read by reflection, and "no `setSSLParameters` call". If U-065 moves, renames or shares them, the test must be retargeted by a declared change, never edited to pass.
- If the pinned factory gains SNI handling, its catch needs its own rejected-name test and mutation (the U-065 story's row S5). AC-2 and AC-3 only cover the default factory's copy.
- AC-1 relies on a fresh `SSLContext` per pinned `createSocket` (`PinnedCertificateSocketFactory.java:77-80`). A cached context would let a resumed session skip the trust manager without turning AC-1 red.
- The TLS 1.2 floor must leave TLS 1.3 negotiable on the loopback server, or AC-1 fails before the trust manager is reached (its failure message would show this).
- The U-063 tests that assume the pinned factory has no hardening (AC-18 and the JDK-provider pinned test) are the ones U-065 must change by declaration. The U-064 tests assume no such thing.

**For the CR-009 move:**
- Retarget the seams above. A missing member is a retarget; a failed behavioural assertion is a regression (as both class comments say).
- The not-yet-valid case is unprotected (DP-1, security F-1, Low). If the pinned code is rewritten rather than moved verbatim, add a future-dated fixture then.
- AC-1 assertion (iii) and the "inverted fingerprint" Javadoc claim are true by reading but not mutation-tested (code review N1).
- Stale comments that were deliberately left alone: `DefaultTrustedSocketFactorySniTest.java:168` (IPv4 is not rejected by `SNIHostName`; SC-1) and `PinnedCertificateSocketFactoryCreateSocketTest.java:77-79` (superseded by AC-1; SC-2).
- Not verified anywhere: Android's own `SNIHostName` rules, device providers, and the device clock.

## Issues Found

- None blocking.
- Process note: `build.gradle:29-30` disables JUnit XML, so a plain gate run leaves stale XML from an earlier run in `app/build/test-results`. Any agent reading totals from that directory without the U-063 init script reads someone else's run. Worth one line in the sprint retro.
- Textual deviations IC-8 and IC-12: judged harmless above.
