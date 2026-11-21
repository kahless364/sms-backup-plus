---
artifact_type: plan
story_id: "U-064"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-21T02:54:05Z"
iteration: 2
base_commit: "de03e99b51a04d402b64cbd5c7f0d7d19e90f2a9"
contracts_verified:
  - CNTR-MODERNIZATION-001
risks_identified: 7
---

# Implementation Plan: U-064 — Two Gap Tests for the TLS Baseline (Pinned Expiry Check, SNI Safety Net)

Story: `sdlc/artifacts/build/sprints/sprint-021/U-064-tls-baseline-gap-tests-pinned-expiry-and-sni-catch.md`.
Tests only. Three new test methods (one new class, two methods added to an existing class), two logged
temporary mutations, no fixture change. DP-1 was decided by the product owner on 2026-11-20 ("leave out",
option A), so IC-4 reads 710 to 713. Base commit `de03e99b51a04d402b64cbd5c7f0d7d19e90f2a9` on
`sdlc/modernization-plan`; at planning time the working tree had SDLC documents changed only (production,
vendored, build and test paths clean: `git status --porcelain -- app k9mail-vendored build.gradle
settings.gradle gradle` printed nothing).

## 0. Changes in Iteration 2

Iteration 1 went through three gates (results in this folder): `plan-review` FAIL (B-1, O-1 to O-7),
`contract-verification` PASS (O1 to O6), `expert-depth-check` FAIL at warning severity (Criterion 5;
recommendations for Criteria 1 and 2). What the gates cleared is unchanged: the AC-1 class, the insertion
point and `old_string` of I3, the AC-3 method, both mutation edits and their expected results, the
write-ahead protocol, and the environment rules. The only test-code change is the message of AC-2's final
assertion (O-3).

| Finding | Change |
|---------|--------|
| plan-review B-1 | The IC-8 scan is no longer part of I6. It is a new last step, I8, run after every evidence file and the implementation log are final. Its raw output goes to the work folder (outside the scanned folders), it prints file names and counts only (`grep -l`, never `-n`), it covers every file of the workspace including `I6-checks.txt` and its own summary, each command has a stated exit code (0 hit, 1 clean, 2 error), and exit 2 has its own decision row (S14). Only exit codes and counts are copied into `evidence/I8-scan.txt`, through `sanitize.py`, and that file is then scanned once more, count-only |
| plan-review O-1 | I8 adds count-only scans for the bare user name and machine host name. The values are read at run time from the environment (`USERNAME`, else `USER`; `COMPUTERNAME`, else the output of `hostname`), passed to `grep -F` as shell variables and never printed or written; only `files=<n>` and the exit code are recorded. Expected 0 files, exit 1 |
| plan-review O-2 | I0 step 1 now starts with `mkdir -p "$WORK/logs" "$M"` |
| plan-review O-3 | AC-2's final assertion message now lists the names tried (`java.util.Arrays.toString(names)`) |
| plan-review O-4 | New row S13: a mutation run whose failure does not match the expected test, type or text (including a timeout, an `SSLException` with another cause under M-N1, another failure type under M-N2) is logged `does not match`, restored, not counted as DETECTED, and the developer stops. Row S3 now also covers AC-2 assertion (i) failing inside the sandbox |
| plan-review O-5 | Row S3: replacing `bad_host.example` in AC-3 is a recorded deviation from the story's literal AC-3, reported in the hand-off for the orchestrator to accept; the AC-2 replacement stays within IC-7's permission |
| plan-review O-6 | I7: the hand-off text for the story's Implementation Notes refers to IC-4 and to `evidence/I6-junit-totals.txt` instead of restating the counts |
| plan-review O-7 | Row S6: a repeat during a mutation run is named in that mutation's entry as a deviation from IC-7(d) "one targeted run". I8 checks the story file too (line numbers and counts only) and states that the IC-8 criterion's own line is the one expected self-match of the literal pattern, for QA's information |
| contract-verification O1 | Section 8 quotes S5 from CR-013 `:317`; the "skips SNI" phrase is attributed to CR-012's candidate clause (`:292`) |
| contract-verification O2 | Section 8 says CR-012 is approved and held for the combined revision and that AC-1 follows CR-012's target text; CR-009 and CR-012 are added to Artifacts Consulted |
| contract-verification O3 | Row S1: if the fallback is used, the developer also reports it in the hand-off as a finding against CR-012's premise (direct cause is the `CertificateException`), for the orchestrator to route |
| contract-verification O4 | Section 8 records that AC-2 (iii) ("`setSSLParameters` never called") is stricter than S5 and binds U-065's design of the round trip; no change to the test (it is the existing BUG-015 observable) |
| contract-verification O5, O6 | Informational; left as they are. O5's gap (layered shape, server name at `createSocket` level) is CR-013 Scope Concern D's; O6 is already covered by the "After the CR-009 move" rule |
| expert-depth-check Criterion 5 | Section 9 now covers all eight ISO/IEC 25010 characteristics, with the four added ones marked as evaluated or not applicable |
| expert-depth-check Criteria 1, 2 | Section 3 opens with a statement that no architectural pattern applies and names the test patterns used; section 6 names the test smells guarded against |

## 1. Assumption Ledger

Status values: `verified` (read or run in this planning session, with the location), `assumed` (not
verified; consequence stated), `needs_check` (becomes a developer step with a stop rule). Nothing material
is left `assumed`.

| # | Claim | Status | Evidence |
|---|-------|--------|----------|
| L1 | Authoritative `@Test` count at HEAD is 710 | verified | CLAUDE.md command at `de03e99b`: 710 |
| L2 | Base blobs: pinned factory `8a0fbb64fb171624591fbebafb818555f28efd58`; vendored default factory `d543927166b3c5e1da0aaa8ab584072a43bf7521` (the same blob as U-063's base, so the vendored file has not changed since U-063) | verified | `git rev-parse HEAD:<path>` for both; U-063 `evidence/mutation-log.md:5` |
| L3 | `enrolledCert.checkValidity();` is at line 149, after the fingerprint comparison `:141-145`, outside any try block; the method declares `throws CertificateException` (`:126-127`) | verified | `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:126-150` |
| L4 | The text `enrolledCert.checkValidity();` (with the semicolon) occurs once in that file; the Javadoc at `:107` has it without a semicolon | verified | read of the file, `:107` and `:149` |
| L5 | `createSocket` installs a fresh `PinnedX509TrustManager` in a fresh `SSLContext("TLS")` on every call (`:77-80`); `socket == null` returns an unconnected socket (`:81-82`) | verified | same file `:75-86` |
| L6 | The guard `isIpOrBlankHostname` (`:216-226`), the call site in `setSniViaSSLParameters` (`:239-243`), `getSSLParameters` (`:244`), the try at `:245`, `new SNIHostName` at `:246`, `setSSLParameters` at `:247`, the catch at `:248-253`; `setSniHost` at `:191-205` with the SDK-24 branch at `:192-197`; `createSocket` calls `setSniHost` at `:177` | verified | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` |
| L7 | Robolectric runs at sdk 29, so `setSniHost` takes the `setSniViaSSLParameters` branch | verified | `app/src/test/resources/robolectric.properties:8` (`sdk=29`); `Build.VERSION_CODES.N` = 24 |
| L8 | On JDK 25.0.5.1, `new SNIHostName(...)` throws `IllegalArgumentException` for `bad_host.example` ("Contains non-LDH ASCII characters"), `host.example.` ("cannot have the trailing dot") and `-x.example` ("Has leading or trailing hyphen"), and accepts `imap.right.test` and `192.168.1.10`; a copy of the guard logic returns `false` for the three names | verified | throwaway `jshell` run in the planner's session scratch folder (nothing written to the repository), output in section 1.1. The developer repeats it as evidence (I1) |
| L9 | Substitute names, in case one of the three is accepted inside the Robolectric sandbox: `bad host.example`, `x-.example`, `star*.example`, `a..b.example` are each rejected by `SNIHostName` on the same JDK and pass the guard | verified | second throwaway `jshell` run, section 1.1 |
| L10 | `EXPIRED_HOST_CERT` decodes to `CN=imap.right.test, O=U-063 Test Only`, notAfter `2020-01-31T06:00:00Z`, and `checkValidity()` throws `java.security.cert.CertificateExpiredException` on the plain JDK 25 | verified | throwaway `jshell` decode of the fixture text (read only); fixture Javadoc `TlsTestFixtures.java:85-88`; constant at `:280`, key at `:300` |
| L11 | Under Robolectric the certificate parser and TLS provider may be Conscrypt's; `checkValidity()` still throws `CertificateExpiredException` by the `X509Certificate` contract | needs_check | Developer step I2: the AC-1 run on unmutated code shows it (assertion (ii)). Stop rule: decision table row S1/S2 |
| L12 | The direct cause of the client's `SSLException` is the exception the pinning trust manager threw (not wrapped further) | needs_check | Expected from U-063 AC-21, which asserts the direct cause is the trust manager's `CertificateException` and passes (`PinnedCertificateSocketFactoryCreateSocketTest.java:190-195`). Developer step I2; stop rule: row S1 (story's fallback) |
| L13 | The server built by `serverContext(EXPIRED_HOST_CERT, EXPIRED_HOST_KEY_PKCS8, TRUSTED_CA_CERT)` presents the expired leaf (the default key manager does not refuse to offer it) | verified (indirectly) | U-063 AC-10 rejects that server's chain on the expiry path (U-063 story, Implementation Notes, "Rejections behave as the story expects"); proven again by M-N1 (with the check removed, the handshake must complete) |
| L14 | `LoopbackTlsTestServer` offers `start(Mode, SSLContext)`, `port()`, `connect(Socket, int)` (loopback address object, 10 s bound), `close()` (idempotent, never throws); the server always wraps a plain accepted socket (U-063 deviation 13) | verified | `app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java:93-95`, `:122-124`, `:147-151`, `:167-170`, `:97-119` |
| L15 | The AC-21 test that AC-1 mirrors is at `PinnedCertificateSocketFactoryCreateSocketTest.java:180-196`; its `@After` closes client and server (`:111-120`) | verified | read of the file |
| L16 | `DefaultTrustedSocketFactorySniTest` has 12 test methods; the U-063 Group A block ends with the closing brace at `:302`, `:303` is blank, the helper banner starts at `:304`; the file imports `List`, `SNIHostName`, `SNIServerName`, `SSLParameters`, `SSLSocket`, `DefaultTrustedSocketFactory`, `assertThat`, `mock`, `never`, `verify`, `when`, and already uses fully qualified names (`java.lang.reflect.Method`, `org.mockito.ArgumentMatchers.any()`) | verified | read of the file, `:1-20`, `:276-304` |
| L17 | The existing pinned tests: `PinnedCertificateSocketFactoryTest` 8 methods (its enrolled certificate is valid 2026-06-03 to 2036, `:33-35`), `PinnedCertificateSocketFactoryCreateSocketTest` 4 methods (enrols `OTHER_HOST_CERT`, valid to 2107); none presents an expired enrolled certificate | verified | read of both files |
| L18 | `DefaultTrustedSocketFactoryJdkProviderTest` has 5 methods and isolation guards A1-G1 to A1-G3 in `@Before` | verified | `DefaultTrustedSocketFactoryJdkProviderTest.java:120-166`, five methods at `:172`, `:191`, `:211`, `:270`, `:311` |
| L19 | `TrustManagerFactory.get(host, port)` only caches a per-`host:port` trust manager in a static map and does not parse the host; it does not touch `defaultTrustManager` | verified | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java:38-61`, `:128-130` |
| L20 | The JaCoCo file filter `**/*Test*.*` is at `app/build.gradle:237`; the vendored module is not in the class directories (`:326-336`); the `mail*` floor is LINE 0.70 (`:368-376`) | verified | read of `app/build.gradle:215-464` |
| L21 | Working-tree line endings: the SNI test and both production files are CRLF in the working tree (LF in the index); `git hash-object` applies the clean filter, so the restore check compares correctly (it did in U-063) | verified | `git ls-files --eol`; U-063 mutation log `restored` lines |
| L22 | The U-063 evidence tools (`sanitize.py`, `junit_cases.py`, `junit_totals.py`, `junit-xml.init.gradle`) are committed under `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/` and can be used read-only from there | verified | listing and read of the four files |
| L23 | `sdlc/artifacts/build/sprints/sprint-021/U-064/compiled-pipeline.json` contains a user-profile path and is excluded locally from git | verified | count of the pattern in the file is 1; `git check-ignore -v` names `.git/info/exclude:37` |
| L24 | The Gradle daemon JVM is JDK 25 (the same 25.0.5.1 as the `jshell` used for L8) | needs_check | `gradle/gradle-daemon-jvm.properties` says `toolchainVersion=25`; the exact version line comes from the init script in I0. Stop rule: if it is not 25, stop (row S8) |
| L25 | Sprint ordering: U-065 (which edits both production files) depends on U-064 and runs in wave 2, so no other story changes the base blobs while U-064 runs | verified | `sdlc/artifacts/build/sprints/sprint-021/sprint-plan.md:24-29`, `:56`; `execution-plan.json` (`depends_on: ["U-064"]`) |

### 1.1 Throwaway checks run while planning (output, verbatim)

```
bad_host.example | guard=false | SNIHostName IAE: Contains non-LDH ASCII characters
host.example. | guard=false | SNIHostName IAE: Server name value of host_name cannot have the trailing dot
-x.example | guard=false | SNIHostName IAE: Has leading or trailing hyphen
imap.right.test | guard=false | SNIHostName ACCEPTED
192.168.1.10 | guard=true | SNIHostName ACCEPTED
java.version=25.0.5.1

bad host.example | guard=false | SNIHostName IAE: Contains non-LDH ASCII characters
x-.example | guard=false | SNIHostName IAE: Has leading or trailing hyphen
star*.example | guard=false | SNIHostName IAE: Contains non-LDH ASCII characters
a..b.example | guard=false | SNIHostName IAE: Empty label is not a legal name

subject=CN=imap.right.test, O=U-063 Test Only notAfter=2020-01-31T06:00:00Z
checkValidity threw java.security.cert.CertificateExpiredException
```

The `IPv4 accepted` line confirms the story's finding that the comment at
`DefaultTrustedSocketFactorySniTest.java:168` is stale for IPv4 (SC-1): only the guard stops an IPv4 literal.

Planning-session disclosure: besides the read-only commands and the three `jshell` runs, one command in
this session invoked `./gradlew --version` with its output discarded (an oversight in a batch of read-only
checks; no task ran, nothing was built, nothing was written to the repository). No other Gradle call was made.

## 2. Environment and Conventions (every developer step)

```bash
# Run from the repository root (absolute path in the tool call's cd). Never set MSYS_NO_PATHCONV for Gradle.
unset MSYS_NO_PATHCONV
W=sdlc/artifacts/build/sprints/sprint-021/U-064; E=$W/evidence; M=$E/mutations
T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools        # U-063 tools, used read-only
INIT=$T/junit-xml.init.gradle                                         # enables JUnit XML, prints the daemon JVM line
R=app/build/test-results/testDebugUnitTest
PIN=app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
DTSF=k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
PIN_BLOB=8a0fbb64fb171624591fbebafb818555f28efd58
DTSF_BLOB=d543927166b3c5e1da0aaa8ab584072a43bf7521
BASE=de03e99b51a04d402b64cbd5c7f0d7d19e90f2a9
NEWT=app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryExpiryTest.java
SNIT=app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java
WORK=<a u064-work folder inside the developer's own session scratch folder>   # raw logs only; never in the repository
```

Shell variables do not persist between tool calls; repeat the block (or the variables a step needs) in each
call. `WORK` is the only absolute path; it is never written into any repository file.

- **Raw logs** go to `$WORK/logs/`. Evidence files in `$E` are produced only by: `sanitize.py` (for Gradle
  logs and anything else that may carry a machine path), `junit_cases.py` / `junit_totals.py` (print class
  names and numbers only), and `git` / `grep` over repository-relative paths. Python runs as
  `python -I -X utf8 <script>`, from the repository root (the sanitiser replaces the current directory with
  `<repo>`, the home directory with `<home>`, and the user and host names with tokens, and refuses to write
  if a profile path, the application-data folder name or an e-mail-like token remains).
- **Gradle gate filter (G):** `--grep '^(BUILD |FAILURE|> Task :app:(assemble|test|jacoco)|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'`.
  The `U063-DAEMON-JVM` label comes from the reused init script; it is expected, not a typo.
- **Targeted run TR(name, classes...):**
  ```bash
  rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:testDebugUnitTest --tests "<fqcn1>" [--tests "<fqcn2>" ...] > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log
  python -I -X utf8 $T/sanitize.py $WORK/logs/<name>.log $WORK/logs/<name>.san --grep '^(BUILD |FAILURE|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'
  python -I -X utf8 $T/junit_cases.py $R <SimpleName1> [<SimpleName2> ...] > $E/<name>-cases.txt
  ```
  (`<name>.san` is kept in `$WORK`; it is quoted into the implementation log only through the sanitised copy.)
- **Failure messages** (only when a test is expected or found to fail):
  `grep -o 'message="[^"]*"' $R/TEST-<fqcn>.xml > $WORK/logs/<name>-msg.log; python -I -X utf8 $T/sanitize.py $WORK/logs/<name>-msg.log $M/<name>-message.txt`.
- **Timeouts and shells.** Every Gradle call is a foreground call with the tool's timeout set to 600000 ms
  (well above three minutes; U-063's full gate took 3 min 12 s). If the tool moves a long run to the
  background by itself, wait for its completion notice; do not poll, do not start another command meanwhile.
  No `&`, no background or detached shells, no `sleep` or polling loops, no helper agents for shell work, no
  heredocs for editing (use the Write and Edit tools). If a call fails with "bash.exe ... not found" or a
  process is killed for no visible reason, **stop and report** (endpoint security); do not retry in another
  shell. If a Gradle call exceeds the tool limit: do not relaunch it in the background; run `./gradlew --stop`
  once; re-run the same work one task per call in the foreground; if one task alone still exceeds the
  limit, stop and report with the sanitised tail of its log.
- **Timestamps** in `implementation-log.md` and `mutation-log.md` are pasted from `date -u +%Y-%m-%dT%H:%M:%SZ`
  run at that moment, never typed.
- **Text rules for the new and added test code.** The string `@Test` appears only as an annotation, never in
  a comment (the count is a text grep). Annotations in short imported form only (`@Test`, `@After`,
  `@RunWith`); never an at-sign followed directly by a dotted package name (it would match the IC-8 e-mail
  pattern). Do not write the JaCoCo glob or any other `*/` sequence inside a Javadoc (it closes the comment).
  No `localhost`, `127.0.0.1`, `getByName`, `Thread.sleep` in code or comments (IC-6 grep).
- **Commit nothing.** The orchestrator commits, with the product owner's permission, before QA (IC-12).

## 3. Implementation Steps

No architectural pattern is introduced; the story adds tests and changes no production design. The tests
apply characterisation testing and the reflection seam for the package-private guard (Feathers, *Working
Effectively with Legacy Code*, ch. 13 "I Need to Make a Change, but I Don't Know What Tests to Write" and
ch. 4 "The Seam Model"), a Test Double of the Mock Object kind for `SSLSocket` (Meszaros, *xUnit Test
Patterns*, ch. 23 "Test Double Patterns"), a real loopback server as a test fixture for the handshake
(the U-063 helper), and mutation testing as the adequacy check of each test (M-N1, M-N2).

Expected Gradle invocations: 9 (I0 gate, I2, I3, M-N1, M-N2, three I5 runs, I6 gate), plus at most one
repeat for a Robolectric lock-file error (row S6).

### I0 — Baseline evidence (IC-1) and the first gate (IC-3)

Files touched: `$E/I0-baseline.txt`, `$E/I0-gates.txt`, `$E/I0-junit-totals.txt`, `$W/implementation-log.md`
(created with its header and the I0 entry).

1. Create the output folders first (neither exists yet; `$E` exists and is empty):
   `mkdir -p "$WORK/logs" "$M"`. Then the pre-flight, one call, written to `$WORK/logs/I0-baseline.log`
   and sanitised to `$E/I0-baseline.txt`:
   ```bash
   date -u +%Y-%m-%dT%H:%M:%SZ
   git rev-parse HEAD; git rev-parse --abbrev-ref HEAD
   git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat; echo "porcelain-end"
   git diff --quiet HEAD -- app/src/main k9mail-vendored; echo "prod-clean-exit=$?"
   git rev-parse HEAD:$PIN HEAD:$DTSF
   git hash-object $PIN $DTSF
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   ```
   Expected: HEAD `de03e99b...` (or a later commit that changes no file under `app`, `k9mail-vendored` or the
   build files, which the developer records), branch `sdlc/modernization-plan`, nothing between the
   porcelain command and `porcelain-end`, `prod-clean-exit=0`, both `rev-parse` and both `hash-object`
   values equal `PIN_BLOB` and `DTSF_BLOB`, count 710, `toolchainVersion=25`.
   **Stop rule:** any production or test path dirty, or a hash differing from section 2 → stop and report
   (another change landed; IC-1 says re-baseline, which the orchestrator decides). A different count with
   clean production → record it, re-baseline IC-4 as count + 3, and report it in the hand-off.
2. Gate (IC-3), one call, timeout 600000:
   ```bash
   rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease > $WORK/logs/I0-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I0-gates.log
   python -I -X utf8 $T/sanitize.py $WORK/logs/I0-gates.log $E/I0-gates.txt --grep '<filter G>'
   python -I -X utf8 $T/junit_totals.py $R > $E/I0-junit-totals.txt
   ```
   Expected: `exit=0`, `U063-DAEMON-JVM: java.version=25...`, `tests=710 failures=0 errors=0 skipped=2`.
   **Stop rule:** exit not 0, or totals different → stop and report with the sanitised log (nothing has been
   edited yet, so the cause is environmental or a prior change). A daemon JVM other than 25 → row S8.

### I1 — Developer's first step (story "Developer's first step", part (a))

Files touched: `$WORK/sni-check.jsh` (outside the repository, written with the Write tool), `$E/I1-first-step.txt`.

1. Write `$WORK/sni-check.jsh` with exactly this content (it copies the guard's logic from `DTSF:216-226`):
   ```java
   boolean guard(String h) {
       if (h == null || h.trim().isEmpty()) return true;
       if (h.indexOf(':') >= 0) return true;
       return h.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
   }
   for (String h : new String[] {"bad_host.example", "host.example.", "-x.example", "imap.right.test", "192.168.1.10"}) {
       String r;
       try { new javax.net.ssl.SNIHostName(h); r = "ACCEPTED"; }
       catch (IllegalArgumentException e) { r = "IAE: " + e.getMessage(); }
       System.out.println(h + " | guard=" + guard(h) + " | SNIHostName " + r);
   }
   System.out.println("java.version=" + System.getProperty("java.version"));
   /exit
   ```
2. Run it from `$WORK` (foreground, timeout 120000): `jshell -q sni-check.jsh > logs/I1-first-step.log 2>&1`,
   then from the repository root sanitise it to `$E/I1-first-step.txt`.
   Expected: identical to the first six lines of section 1.1.
   **Stop rule:** a name ACCEPTED or `guard=true` for one of the three → row S3 (do not write AC-2 with it).
   `jshell` missing → record "not run", continue: I3's assertion (iii) on unmutated code and M-N2 prove the
   same inside the sandbox.
3. Part (b) of the first step (`checkValidity()` under Robolectric) is not run separately: the I2 run of
   AC-1 on unmutated code is that check, and its assertion (ii) is what would fail.

### I2 — AC-1: new class `PinnedCertificateSocketFactoryExpiryTest`

Files touched: `$NEWT` (new, Write tool), `$E/I2-pinned-cases.txt`.

Structure, mirroring `PinnedCertificateSocketFactoryCreateSocketTest` (`:1-31` imports, `:100-120` class
header and `@After`, `:180-196` the rejected-handshake test):

```java
package com.zegoggles.smssync.mail;

import com.zegoggles.smssync.mail.ssl.LoopbackTlsTestServer;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.EXPIRED_HOST_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.EXPIRED_HOST_KEY_PKCS8;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_NAME;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.TRUSTED_CA_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.certificate;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.serverContext;
import static org.junit.Assert.fail;

/** <class Javadoc, content below> */
@RunWith(RobolectricTestRunner.class)
public class PinnedCertificateSocketFactoryExpiryTest {

    private static final String PIN_MARKER = "Pinned certificate mismatch";   // as CreateSocketTest:103

    private LoopbackTlsTestServer server;
    private SSLSocket client;

    @After
    public void tearDown() {
        if (client != null) {
            try {
                client.close();
            } catch (IOException ignored) {
                // nothing to do for a test socket
            }
        }
        if (server != null) {
            server.close();
        }
    }

    /** <method Javadoc: AC-1; breaks when M-N1 is applied; the server's outcome is not asserted> */
    @Test
    public void handshake_expiredEnrolledCertificate_isRejectedAsExpired() throws Exception {
        X509Certificate enrolled = certificate(EXPIRED_HOST_CERT);
        assertWithMessage("precondition: the enrolled fixture's end date lies before the machine clock")
                .that(enrolled.getNotAfter().before(new Date())).isTrue();

        server = LoopbackTlsTestServer.start(LoopbackTlsTestServer.Mode.IMPLICIT_TLS,
                serverContext(EXPIRED_HOST_CERT, EXPIRED_HOST_KEY_PKCS8, TRUSTED_CA_CERT));
        client = (SSLSocket) new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(),
                RIGHT_HOST_NAME, enrolled).createSocket(null, RIGHT_HOST_NAME, server.port(), null);
        LoopbackTlsTestServer.connect(client, server.port());

        try {
            client.startHandshake();
            fail("the handshake must be rejected, but it completed");
        } catch (SSLException e) {
            Throwable cause = e.getCause();
            assertWithMessage("direct cause of the SSLException; cause chain: " + causeChain(e))
                    .that(cause).isInstanceOf(CertificateExpiredException.class);
            assertWithMessage("message of the direct cause")
                    .that(String.valueOf(cause.getMessage())).doesNotContain(PIN_MARKER);
        }
    }

    /** Class names of the cause chain, outermost first, at most 10 entries (for the failure message only). */
    private static String causeChain(Throwable t) {
        StringBuilder out = new StringBuilder();
        Throwable current = t;
        for (int i = 0; i < 10 && current != null; i++) {
            if (i > 0) {
                out.append(" <- ");
            }
            out.append(current.getClass().getName());
            Throwable next = current.getCause();
            current = (next == current) ? null : next;
        }
        return out.toString();
    }
}
```

Where each assertion comes from:

| Assertion | Story source | Code it observes |
|-----------|--------------|------------------|
| precondition (fixture end date before the clock) | T3 ("expired for every date after 2020-01-31"); makes a wrong machine clock fail with its own message instead of looking like a regression. Not one of the AC's three assertions; it adds no test | `TlsTestFixtures.java:85-88` |
| `fail("the handshake must be rejected, but it completed")` | AC-1 last two sentences; the M-N1 expected failure text | `PinnedCertificateSocketFactory.java:149` removed → handshake completes |
| (i) `SSLException` thrown | AC-1 (i); `fail` throws `AssertionError`, which the `catch (SSLException)` does not catch | the client handshake |
| (ii) direct cause is `CertificateExpiredException` | AC-1 (ii); same shape as AC-21 (`CreateSocketTest:191-193`) | `checkValidity()` at `:149` |
| (iii) cause message lacks `Pinned certificate mismatch` | AC-1 (iii); shows the fingerprint step `:141-145` passed | `:142-144` message text |

The factory is built with `RIGHT_HOST_NAME`, the host the client asks for, and the enrolled certificate is
`certificate(EXPIRED_HOST_CERT)` held in a local (the same value AC-1 names inline); the server presents the
same leaf, so the SHA-256 fingerprints are equal. `getTrustManagerForTesting()` is not used.

Class Javadoc (plain words, as U-063's classes; no `@Test` text, no `*/` inside):
- What it proves: through the trust manager that `createSocket` installs (`:77`), a server presenting
  exactly the enrolled leaf passes the fingerprint step and is rejected by the validity step because the
  enrolled certificate is expired (contract CNTR-MODERNIZATION-001, producer table and error table,
  "validity-window check"; RFC 5280 section 4.1.2.5).
- What it cannot prove: a device's TLS provider; a not-yet-valid certificate (DP-1, left out by the product
  owner on 2026-11-20); the device clock (the check reads the clock at connection time); chain and host-name
  checks, which the pinned path deliberately omits; anything below TLS 1.3 (U-063 environment probe). It
  relies on a fresh TLS context per `createSocket` call (a resumed session would skip the trust manager).
- Mutation that must break it: M-N1 (the `checkValidity()` call disabled), with the failure "the handshake
  must be rejected, but it completed"; also a fingerprint comparison inverted (assertion (ii) or (iii)).
- Record correction: the sentence in the `PinnedCertificateSocketFactoryCreateSocketTest` Javadoc saying
  that no test presents an expired enrolled certificate (scope concern SC-6 of U-063) is superseded by this
  class; it is left unedited under this story's additions-only rule (U-064 SC-2).
- Global state: none changed; no host-name verifier installed; the vendored default trust manager is not
  touched; every wait bounded at 10 seconds by the helper; no sleeps; loopback address object only.
- The "After the CR-009 move" paragraph, verbatim from `CreateSocketTest:96-98`.

Run TR(`I2-pinned`, the three pinned classes in one invocation):
`--tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryExpiryTest" --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest" --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryTest"`;
`junit_cases.py $R PinnedCertificateSocketFactory`.
Expected: `exit=0`; `files=3 cases=13 pass=13` (1 + 4 + 8). This run is also the unmutated baseline for
M-N1. **Stop rule:** AC-1 fails → extract its message (section 2) and apply rows S1, S2 or S5; a compile
error → fix the test source only; any of the 12 existing tests fails → row S9.

### I3 — AC-2 and AC-3: two methods added to `DefaultTrustedSocketFactorySniTest`

Files touched: `$SNIT` (Edit tool, one edit), `$E/I3-sni-cases.txt`, `$E/I3-sni-diff-check.txt`.

**Where.** One insertion between the closing brace of `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue`
(`:302`) and the helper banner (`:304`). Edit tool `old_string` (unique in the file):

```
        assertThat((Boolean) m.invoke(null, "1:2:3:4:5:6:7:8")).isTrue();
    }

    // -----------------------------------------------------------------------
    // Helper: call the private setHostnameViaReflection via reflection for testing
```

`new_string` = the same five lines with the new block inserted after the blank line, i.e. the first two
lines, a blank line, the new comment block and two methods, a blank line, then the two banner lines. No
import line is changed: the added code uses fully qualified names for the four types the file does not
import (`org.robolectric.RuntimeEnvironment`, `com.google.common.truth.Truth`, `java.util.ArrayList`,
`java.net.Socket`), as the file already does for `java.lang.reflect.Method` and
`org.mockito.ArgumentMatchers`. The diff is then one hunk of added lines only.

Added block:

```java
    // -----------------------------------------------------------------------
    // U-064 (AC-2, AC-3): the safety net for a host name that SNIHostName rejects
    //
    // What these two tests can prove: a host name that passes the IP/blank guard
    // (isIpOrBlankHostname) but that the JDK's SNIHostName rejects (an underscore, a
    // trailing dot, a leading hyphen) neither throws from setSniViaSSLParameters nor from
    // createSocket, and no server name is set for it. Only the catch of
    // IllegalArgumentException in setSniViaSSLParameters provides this (fix for BUG-015).
    // The IP-literal and blank tests above protect the guard; these protect the catch.
    // Both are provider-independent: the first uses a Mockito SSLSocket and reflection,
    // the second an unconnected socket (no network, no name resolution).
    //
    // What they cannot prove: which names Android's own SNIHostName rejects (the desktop
    // JDK's rules apply here); what a device sends on the wire for such a name.
    //
    // Mutations that must break them (U-064 mutation log):
    //   M-N2, the try/catch replaced by its two body lines with the guard kept: the
    //        IllegalArgumentException then escapes, and both tests fail for every name.
    //   AC-2 assertion (i) also breaks when the guard is widened to cover these names,
    //        which forces a decision about where the protection now lives.
    //
    // The comment on the IPv4 test above says SNIHostName rejects IP literals; it does
    // so for IPv6 but not for IPv4, which only the guard stops (U-063 mutation M-A1g).
    // That comment is left as it is (U-064 scope concern SC-1).
    //
    // After the CR-009 move: a missing field, method or class means this test's seam
    // needs retargeting to the moved class; a failed behavioural assertion is a
    // regression, and the test is never edited to pass.
    // -----------------------------------------------------------------------

    /**
     * AC-2: for each name that passes the guard and that SNIHostName rejects, the guard
     * answers false, setSniViaSSLParameters throws nothing, and setSSLParameters is never
     * called. All names are tried before the no-throw assertion, so a failure lists every
     * name for which an exception escaped.
     */
    @Test
    public void setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI() throws Exception {
        String[] names = {"bad_host.example", "host.example.", "-x.example"};
        java.lang.reflect.Method guard = DefaultTrustedSocketFactory.class
                .getDeclaredMethod("isIpOrBlankHostname", String.class);
        guard.setAccessible(true);
        List<String> escaped = new java.util.ArrayList<>();

        for (String name : names) {
            com.google.common.truth.Truth.assertWithMessage(
                    "isIpOrBlankHostname(\"" + name + "\") must be false, so the name reaches SNIHostName")
                    .that((Boolean) guard.invoke(null, name)).isFalse();

            SSLSocket mockSocket = mock(SSLSocket.class);
            when(mockSocket.getSSLParameters()).thenReturn(new SSLParameters());
            try {
                DefaultTrustedSocketFactory.setSniViaSSLParameters(mockSocket, name);
            } catch (RuntimeException e) {
                escaped.add(name + " -> " + e);
                continue;
            }
            try {
                verify(mockSocket, never()).setSSLParameters(org.mockito.ArgumentMatchers.any());
            } catch (AssertionError e) {
                throw new AssertionError("setSSLParameters was called for \"" + name
                        + "\", so SNIHostName accepted it here", e);
            }
        }

        com.google.common.truth.Truth.assertWithMessage(
                "setSniViaSSLParameters must not throw for a name SNIHostName rejects; names tried: "
                + java.util.Arrays.toString(names) + "; escaped")
                .that(escaped).isEmpty();
    }

    /**
     * AC-3: createSocket for a name that SNIHostName rejects returns an SSLSocket and
     * throws nothing. The socket is never connected.
     */
    @Test
    public void createSocket_nameRejectedBySniHostName_doesNotThrow() throws Exception {
        java.net.Socket socket = new DefaultTrustedSocketFactory(
                org.robolectric.RuntimeEnvironment.getApplication())
                .createSocket(null, "bad_host.example", 993, null);
        try {
            assertThat(socket).isInstanceOf(SSLSocket.class);
        } finally {
            socket.close();
        }
    }
```

How each AC clause maps: AC-2 (i) is the guard assertion (reflection as `:295-301`); (ii) is the
`escaped` list (the exception is caught per name so the M-N2 failure lists all three names, which is the
story's "the test fails for each name" made observable in one message); (iii) is the `verify ... never()`
per name with a message naming the host; every message names the host. AC-3 is the second method; the
socket is created outside the `try` so that a throwing `createSocket` fails the test with the escaping
exception itself, and the `finally` closes it otherwise. A fresh mock and a fresh `SSLParameters` per name.

After the edit, one call writing `$E/I3-sni-diff-check.txt` (git output is repository-relative):
```bash
git diff --numstat HEAD -- $SNIT                          # expected: "<n> 0 <path>" (0 deletions)
git diff HEAD -- $SNIT | grep -c '^-[^-]'                 # expected: 0
git diff HEAD -- $SNIT | grep -c '^@@'                    # expected: 1 (one hunk)
git grep --untracked -h "@Test" -- $SNIT | grep -c "@Test"   # expected: 14
```
Then TR(`I3-sni`, `--tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactorySniTest" --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest"`),
`junit_cases.py $R DefaultTrustedSocketFactorySniTest DefaultTrustedSocketFactoryJdkProviderTest`.
Expected: `exit=0`; `files=2 cases=19 pass=19` (14 + 5). This run is also the unmutated baseline for M-N2,
and it shows the JDK-provider guards A1-G1 to A1-G3 still pass now that the SNI class calls `createSocket`.
**Stop rule:** deletions not 0 or more than one hunk → undo with `git checkout -- $SNIT` and redo the edit
(line endings: the file is CRLF in the working tree; if the Edit tool cannot keep them, report rather than
convert the file); AC-2 fails at (iii) → row S3; AC-2 fails at (ii) or AC-3 throws on unmutated code →
row S5; a JDK-provider guard fails → row S10.

### I4 — Mutations M-N1 and M-N2 (IC-7), write-ahead protocol

Files touched: `$E/mutation-log.md` (created at the first entry, with the header below),
`$M/M-N1-cases.txt`, `$M/M-N1-message.txt`, `$M/M-N2-cases.txt`, `$M/M-N2-message.txt`; the two production
files are edited temporarily and restored within the same step.

Log header (first lines of `mutation-log.md`): title; "Protocol: U-064 plan section I4 (write-ahead entry,
one edit with the Edit tool, one targeted run, single-file checkout, hash and porcelain check, `restored`
line). Paths are repository-relative."; base commit; the two files and base blobs; the recovery note: "If a
session is lost while an entry says `APPLYING` and has no `restored` line, nothing else runs (no gate, no
review, no commit) until the file's hash has been compared with its base blob and, if it differs, the file
has been restored with `git checkout -- <file>`." Format of each entry as U-063's entries
(`sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/mutation-log.md:126-137`).

For each mutation, in this order, one at a time:

1. **(b) clean check**, one call: `git diff --quiet HEAD -- app/src/main k9mail-vendored; echo $?` (must be 0),
   `git status --porcelain -- app/src/main k9mail-vendored` (empty), `git hash-object <file>` (equals its
   base blob). Stop if any differs.
2. **(a) write-ahead entry**: run `date -u +%Y-%m-%dT%H:%M:%SZ`; append to `mutation-log.md`:
   `### <id> APPLYING <timestamp>`, `file:`, `hash-before:` (with the clean-check result), `edit:`,
   `class run:`, `expected:`. The entry is written before the edit.
3. **(c) one edit with the Edit tool** (Read the lines first, so the `old_string` is copied exactly):
   - **M-N1**, `$PIN`: `old_string` = `            enrolledCert.checkValidity();` (12 spaces; unique, L4),
     `new_string` = `            if (false) { enrolledCert.checkValidity(); }`. Compiles: no try or catch is
     stranded, the method still declares `throws CertificateException`, and `if (false)` is the
     conditional-compilation form Java allows.
   - **M-N2**, `$DTSF`: `old_string` = lines 245 to 253 exactly as read (`        try {` through the closing
     `        }` of the catch; the comment and log lines contain em dashes, so copy them from the Read
     output, do not retype), `new_string` = the two lines
     `        params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));` and
     `        socket.setSSLParameters(params);`. Line 244 and the guard stay. Same edit as U-063 M-A1c.
     Compiles: `IllegalArgumentException` is unchecked and no other catch references it.
4. **(d) one targeted run** (TR, section 2), timeout 600000:
   - M-N1: the three pinned classes (as I2), `junit_cases.py $R PinnedCertificateSocketFactory > $M/M-N1-cases.txt`,
     failure message of the Expiry class into `$M/M-N1-message.txt`.
     **Expected:** `cases=13 pass=12 fail=1`; the failing test is
     `handshake_expiredEnrolledCertificate_isRejectedAsExpired`, type `java.lang.AssertionError`, message
     "the handshake must be rejected, but it completed". Verdict DETECTED.
   - M-N2: SNI and JDK-provider classes (as I3), `junit_cases.py $R DefaultTrustedSocketFactorySniTest DefaultTrustedSocketFactoryJdkProviderTest > $M/M-N2-cases.txt`,
     failure messages of the SNI class into `$M/M-N2-message.txt`.
     **Expected:** `cases=19 pass=17 fail=2`; failing: `setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI`
     (type `com.google.common.truth.AssertionErrorWithFacts`; the `escaped` value in the message, Truth's
     "but was" part, lists all three names, each as `<name> -> java.lang.IllegalArgumentException: ...`;
     the "names tried" part lists the three names whatever happens, so it is not the evidence) and `createSocket_nameRejectedBySniHostName_doesNotThrow`
     (type `java.lang.IllegalArgumentException`). The 12 existing SNI tests and the 5 JDK-provider tests
     pass, as under M-A1c. Verdict DETECTED.
5. **(e) restore at once**, whatever the result: `git checkout -- <file>`.
6. **(f) verify and log**: `git hash-object <file>` equals the base blob; `git status --porcelain -- app/src/main k9mail-vendored`
   empty; `git diff --stat HEAD -- app/src/main k9mail-vendored` empty. Run `date -u ...`; append
   `run_exit:`, `cases:` (path and the summary line), `failed:`, `passed:`, `expected: ... matches` or
   `does not match`, `verdict:`, and `restored: <timestamp> hash-ok porcelain-empty (...)`.

**(g)** No gate, review or commit runs while an entry lacks its `restored` line. M-N2 starts only after
M-N1's `restored` line is written. At the end, add a two-row summary table (id, AC, failing tests / of,
verdict) at the top of the log, under the header.

### I5 — Determinism (IC-6) and the static checks (IC-6, IC-9)

Files touched: `$E/I5-run1-cases.txt`, `$E/I5-run2-cases.txt`, `$E/I5-run3-cases.txt`, `$E/I5-checks.txt`.

1. Pre-check: production clean and both hashes equal the base blobs (as I4 step 1).
2. Three consecutive runs, each its own foreground call, timeout 600000:
   `rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryExpiryTest" --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactorySniTest" --rerun > $WORK/logs/I5-run<n>.log 2>&1; echo "exit=$?" >> $WORK/logs/I5-run<n>.log`
   then `junit_cases.py $R PinnedCertificateSocketFactoryExpiryTest DefaultTrustedSocketFactorySniTest > $E/I5-run<n>-cases.txt`.
   Expected each: `exit=0`, `files=2 cases=15 pass=15`. (`--rerun`, not `--rerun-tasks`, as U-063 deviation 5.)
3. One call writing `$E/I5-checks.txt`:
   ```bash
   diff $E/I5-run1-cases.txt $E/I5-run2-cases.txt; echo "diff12-exit=$?"
   diff $E/I5-run1-cases.txt $E/I5-run3-cases.txt; echo "diff13-exit=$?"
   # IC-6 grep over the two touched files (new file is untracked, so --untracked)
   git grep --untracked -nE 'Thread\.sleep|getByName|getAllByName|"localhost"|"127\.0\.0\.1"|new InetSocketAddress\("' -- $NEWT $SNIT; echo "ic6-grep-exit=$?"
   # IC-9
   git grep --untracked -nE 'setDefaultHostnameVerifier|defaultTrustManager' -- $NEWT $SNIT; echo "ic9-grep-exit=$?"
   ```
   Expected: both diff exits 0; both grep exits 1 (no match). The story's repository-wide IC-6 form
   (`-- app/src/test/java/com/zegoggles/smssync/mail`) is also run and recorded; any hit must lie in a U-063
   file, not in the two touched files (U-063 accepted its helper's bounded socket waits).
   **Stop rule:** a diff exit not 0 → row S9 (non-determinism); a grep hit in a touched file → fix the test
   source and re-run I5 from step 2.

### I6 — Final gates (IC-3), counts and file checks (IC-2, IC-4, IC-5, IC-8)

Files touched: `$E/I6-gates.txt`, `$E/I6-junit-totals.txt`, `$E/I6-checks.txt`.

1. Pre-check as I4 step 1 (no mutation present; both `restored` lines exist).
2. Gate, same command as I0 step 2, into `$WORK/logs/I6-gates.log` → `$E/I6-gates.txt`, `$E/I6-junit-totals.txt`.
   Expected: `exit=0`, `tests=713 failures=0 errors=0 skipped=2` (plus one skipped per IC-11 `@Ignore`, row S5).
3. One call writing `$E/I6-checks.txt`:
   ```bash
   git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"   # 713 (working-tree form)
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"          # 710 until the commit
   git status --porcelain                                    # only $SNIT (M), $NEWT (??) and sdlc/ paths
   git status --porcelain -- app/src/test                    # exactly the two test paths
   git diff --stat HEAD -- app/src/main k9mail-vendored build.gradle app/build.gradle settings.gradle gradle   # empty
   git diff --quiet HEAD -- gradle/verification-metadata.xml; echo "vm-exit=$?"                              # 0
   git diff --numstat HEAD -- $SNIT                          # "<n> 0 ..."
   git ls-files --others --exclude-standard -- app/src/test  # only $NEWT (IC-5: the only new class, name contains Test)
   git hash-object $PIN $DTSF                                # the base blobs
   ```
4. The IC-8 scan is not run here; it is step I8, after every file is final.

### I7 — Implementation log and hand-off text

`$W/implementation-log.md` (created at I0, appended at each step): per step, the clock timestamp, the
command summary (repository-relative paths, `$WORK` named only as "the work folder"), the result line copied
from the sanitised evidence file, and any decision-table row used. Close with: the counts as "see IC-4;
Gradle totals in `evidence/I6-junit-totals.txt`, authoritative count in `evidence/I6-checks.txt`" (the
numbers are not restated, IC-12), the mutation log location and the two verdicts (IC-7), the I1 result, any
bug id (IC-11), the DP-1 outcome ("leave out", no not-yet-valid test), any recorded deviation (rows S1, S3,
S6), a "Runtime claims" line listing only what the evidence files show, and a last line "IC-8 scan: step I8,
result in `evidence/I8-scan.txt`". This entry is the last write to the log before I8. The developer does not
edit the story: the hand-off returns the text the orchestrator places in the story's Implementation Notes,
written with the same count rule (refer to IC-4, do not restate). If I8 then finds a hit, the developer fixes
the file that carries it, re-runs I8 in full, and reports both runs in the hand-off (not in the log).

Evidence produced (all text, all under `$W`): `implementation-log.md`; `evidence/I0-baseline.txt`,
`I0-gates.txt`, `I0-junit-totals.txt`, `I1-first-step.txt`, `I2-pinned-cases.txt`, `I3-sni-cases.txt`,
`I3-sni-diff-check.txt`, `mutation-log.md`, `mutations/M-N1-cases.txt`, `mutations/M-N1-message.txt`,
`mutations/M-N2-cases.txt`, `mutations/M-N2-message.txt`, `I5-run1-cases.txt`, `I5-run2-cases.txt`,
`I5-run3-cases.txt`, `I5-checks.txt`, `I6-gates.txt`, `I6-junit-totals.txt`, `I6-checks.txt`, and last
`I8-scan.txt`. How each is filtered: Gradle logs and `jshell` output through `sanitize.py` (with filter G
for gate logs); XML failure messages through `sanitize.py`; case and total summaries straight from
`junit_cases.py` / `junit_totals.py` (class names and numbers only); `git`/`grep` output straight
(repository-relative paths); `I8-scan.txt` through `sanitize.py`, holding labels, exit codes, counts and
repository-relative file names only. Hand-written log text never quotes `ls -l` lines or any other output
that shows a file owner, a machine name or an absolute path.

### I8 — IC-8 evidence-hygiene scan (last step; after the log and every evidence file are final)

Files touched: `$WORK/logs/I8-*.log` (outside the repository), then `$E/I8-scan.txt` (one write, through
`sanitize.py`). Nothing else is written. The scan reads; its output never goes into a scanned folder, it
prints file names and counts only (`grep -l`, never `-n`, never matched text), and the bare user and host
names are read from the environment at run time and never printed or written.

Scanned set F: every file under `$W` (the workspace, recursively, which includes `plan.md`, the gate
results, `implementation-log.md`, `evidence/` with `mutation-log.md`, `mutations/` and `I6-checks.txt`),
except `compiled-pipeline.json` (it holds a plugin path, is excluded locally from git, L23, and is not part
of the story's commit); the new class `$NEWT`; and the added lines of the SNI test, as a diff written to the
work folder. One call:

```bash
P='@[a-z0-9-]+\.[a-z]{2,}|Users[/\\]|App''Data'   # the IC-8 pattern; the folder name is split so this plan does not match itself
L=$WORK/logs
git diff HEAD -- $SNIT > $L/I8-sni.diff; echo "sni-diff-exit=$?"                       # expected 0
{
  grep -rlEi --exclude=compiled-pipeline.json "$P" $W $NEWT $L/I8-sni.diff > $L/I8-pattern-files.log
  echo "pattern-scan exit=$? files=$(wc -l < $L/I8-pattern-files.log)"                  # expected exit=1 files=0
  for label in user-name host-name; do
    case $label in
      user-name) v="${USERNAME:-$USER}" ;;
      host-name) v="${COMPUTERNAME:-$(hostname)}" ;;
    esac
    if [ ${#v} -lt 3 ]; then echo "$label-scan value-unavailable"; continue; fi
    grep -rliF --exclude=compiled-pipeline.json -e "$v" $W $NEWT $L/I8-sni.diff > $L/I8-$label-files.log
    echo "$label-scan exit=$? files=$(wc -l < $L/I8-$label-files.log)"                   # expected exit=1 files=0
  done
  S=sdlc/artifacts/build/sprints/sprint-021/U-064-tls-baseline-gap-tests-pinned-expiry-and-sni-catch.md
  echo "story-pattern-lines=$(grep -nEi "$P" $S | cut -d: -f1 | tr '\n' ' ')"           # expected: one line number
  echo "story-ic8-line=$(grep -n 'IC-8: (evidence hygiene)' $S | cut -d: -f1)"         # expected: the same number
  for label in user-name host-name; do
    case $label in user-name) v="${USERNAME:-$USER}" ;; host-name) v="${COMPUTERNAME:-$(hostname)}" ;; esac
    [ ${#v} -lt 3 ] && continue
    echo "story-$label-count=$(grep -ciF -e "$v" $S)"                                     # expected 0
  done
} > $L/I8-scan.log 2>&1
cat $L/I8-pattern-files.log $L/I8-user-name-files.log $L/I8-host-name-files.log      # file names only; expected empty
```

Then, if every expectation holds:
`python -I -X utf8 $T/sanitize.py $L/I8-scan.log $E/I8-scan.txt` and a final count-only check of that file,
printed to the terminal only and reported in the hand-off (not written anywhere):
`grep -cEi "$P" $E/I8-scan.txt` (expected 0, exit 1) and, for each of the two values as above,
`grep -ciF -e "$v" $E/I8-scan.txt` (expected 0, exit 1).

Expected exit codes, for every `grep` here: 1 = no match (the clean result), 0 = a match (a finding),
2 = an error (a missing path, an unreadable file, or an output file inside the scanned set; never a clean
result). For the story file: exactly one line matches the literal pattern, and it is the IC-8 criterion's
own line (line 83 at iteration 2 of this plan), because that line spells out the folder name it forbids.
This self-match is expected and is noted for QA, whose literal IC-8 form over `git diff <base>..HEAD` would
show that line if the story's IC-8 text is ever edited in the story's commit; it is not a hygiene finding.

**Stop rules:** any exit 0 or `files` above 0 → row S15; any exit 2, or `value-unavailable` → row S14;
`story-pattern-lines` with more than one number, or different from `story-ic8-line`, or a story bare-name
count above 0 → report it to the orchestrator (the story is the orchestrator's file; the developer does not
edit it). If a hit is fixed, I8 is run again in full and `I8-scan.txt` is written again from the new run.

## 4. Decision Table (foreseeable surprises)

| Row | Observation | What the developer does |
|-----|-------------|-------------------------|
| S1 | I2: handshake rejected, but the direct cause is not `CertificateExpiredException`, and a `CertificateExpiredException` appears deeper in the cause chain (the message lists the chain) | The story's fallback: change assertion (ii) to "the nearest `CertificateException` in the cause chain is a `CertificateExpiredException`" (walk the chain, bounded at 10), keep (iii) on that exception, and add one Javadoc sentence saying why (the provider wraps the trust manager's exception). Record in the log. Re-run I2. Not an IC-11 case: production behaves as required. Also report it in the hand-off as a finding against CR-012's target text, which assumes the `SSLException`'s direct cause is the `CertificateException` (CR-012 `:247`), for the orchestrator to route; the contract and the change record are not edited |
| S2 | I2: handshake rejected, but no `CertificateExpiredException` anywhere in the chain (for example the cause carries `Pinned certificate mismatch`, or the precondition fails) | Precondition failed → the machine clock is wrong: stop and report. Mismatch message → the server did not present the enrolled leaf, or the fingerprint step failed: stop and report with the sanitised message; do not change the fixtures or production |
| S3 | A chosen name does not reach the catch: `SNIHostName` accepts it (I1 shows ACCEPTED, or AC-2 fails at (iii) on unmutated code with "SNIHostName accepted it here"), or the guard stops it (I1 shows `guard=true`, or AC-2 fails at assertion (i) inside the sandbox on unmutated code) | Replace that name in AC-2 with the first substitute from L9 that I1's script (extended with the substitute) shows rejected and `guard=false`: `bad host.example`, then `x-.example`, `star*.example`, `a..b.example`. Record the replacement in the log and, under M-N2, in the mutation log (IC-7 last bullet permits this for AC-2). If no substitute works, drop the name; AC-2 needs at least one name — if none remains, stop and report. If the affected name is `bad_host.example`, AC-3 would need the same replacement; the story fixes that name for AC-3 and gives no permission to change it, so the replacement in AC-3 is a recorded deviation: make it, record it in the log, and report it in the hand-off for the orchestrator to accept or reject before review |
| S4 | A mutation does not compile | Restore at once (step 5), log the entry as `COMPILE_FAIL (invalid, not counted)` with its `restored` line, then apply the compiling replacement as a new entry with its own write-ahead line: M-N1r = the same `if (false)` form (if it was M-N1 itself that failed, stop and report: the analysis in I4 step 3 was wrong); M-N2r = keep the try and make the catch body `throw e;` |
| S5 | A new test fails on unmutated production code in a way that shows a production defect (the pinned factory accepts the expired certificate; `createSocket` or `setSniViaSSLParameters` throws for a rejected name with the catch in place) — IC-11 | Stop. No production edit. Record the result in the implementation log and the hand-off; the orchestrator files the bug through `/amp:create-bug`. After the bug id exists, and only on the orchestrator's instruction, mark that test `@Ignore("<reason>, <bug id>")` as `AuthPreferencesTest.java:1079` does; IC-4 is then read with one more skipped |
| S6 | Robolectric class-level error such as `IllegalStateException: Couldn't create lock file` before any test of a class runs (seen once in U-063, `implementation-log.md:742`) | Repeat the same command once. Keep both summaries (`<name>-attempt1-cases.txt` and `<name>-cases.txt`) and log both. A second failure → stop and report. If it happens during a mutation run, the mutation stays applied only until the repeat finishes; restore and log as usual, and name the repeat in that mutation's entry as a deviation from IC-7(d) ("one targeted run"), with both summaries cited |
| S7 | A mutation is not detected (M-N1: AC-1 passes; M-N2: either new test passes, or a name is missing from the `escaped` list in the AC-2 failure message) | Restore and log the verdict `NOT DETECTED` honestly. Missing name under M-N2 → row S3 for that name, then re-run M-N2 as a new entry. Otherwise the criterion is unmet: stop and report; do not weaken or rewrite the expectation |
| S8 | Daemon JVM not 25, `jshell` and daemon versions differ, or `./gradlew` fails with "Could not find or load main class" | Not 25 → stop and report. Versions differ but both 25 → record, continue (in-sandbox proof is I3 and M-N2). Main-class error → check `MSYS_NO_PATHCONV` is unset and re-run once |
| S9 | An existing test fails, or a determinism diff is not empty | Stop and report with the sanitised summaries; do not edit existing tests (IC-10) |
| S10 | A JDK-provider guard (A1-G1 to A1-G3) fails after the SNI class gained a `createSocket` call | Stop and report: the guard is a tripwire for sandbox sharing (`DefaultTrustedSocketFactoryJdkProviderTest.java:98-100`); do not change either class to make it pass |
| S11 | `sanitize.py` refuses a file | Narrow the filter or extract fewer lines; never edit the raw log or the evidence file by hand to get past it |
| S12 | Anything else not foreseen here, including a shell failure or a process killed without a visible reason | Restore any applied mutation first (if its entry lacks `restored`), then stop and report |
| S13 | A mutation run fails, but not as expected: under M-N1 the AC-1 test fails for another reason (a socket timeout, an `SSLException` whose cause is not the expected one, a failure other than "the handshake must be rejected, but it completed"); under M-N2 a failure type or message differs from I4's expectation, or a test other than the two new ones fails, even if both new tests fail | Restore at once and verify (I4 steps 5 and 6). Log `expected: does not match`, record the actual failing tests, types and the sanitised message, and the verdict `MISMATCH (not counted as DETECTED)`. Then stop and report; do not re-run, re-word the expectation, or change the test (IC-7: "The failure must be that assertion, not a compile error and not a timeout") |
| S14 | I8: a `grep` exits 2, or a bare-name value is unavailable (`value-unavailable`) | Exit 2 is an error, not a clean result: read the error from `$WORK/logs/I8-scan.log` (a missing path or an output file inside the scanned set are the usual causes), correct the command, and re-run I8 in full. A value that cannot be read from the environment means that half of IC-8 cannot be checked: write nothing for it, and report it in the hand-off as "not checked" for the orchestrator |
| S15 | I8: a pattern or bare-name match (exit 0, `files` above 0) | Open each listed file with the Read tool to see the context (the value then appears only in the tool output, never in a file). Remove the text from the file that carries it: in a hand-written file by editing it; in an evidence file by regenerating it from its raw log through `sanitize.py` (never by hand-editing evidence). Then re-run I8 in full. A match in a gate-result file written by another agent, or in `plan.md`, is reported to the orchestrator, not edited by the developer |

## 5. Criterion-to-Step Map

| Criterion | Step(s) | How it is shown | Evidence |
|-----------|---------|-----------------|----------|
| AC-1 | I2, I4 (M-N1), I5, I6 | passes unmutated; fails under M-N1 with the named message; source read (enrolled and presented are both `EXPIRED_HOST_CERT`; `createSocket`, not the seam) | `I2-pinned-cases.txt`, `mutation-log.md`, `mutations/M-N1-*.txt`, `I5-run*-cases.txt` |
| AC-2 | I1, I3, I4 (M-N2), I5 | guard false for each name; passes unmutated; fails under M-N2 with all three names listed | `I1-first-step.txt`, `I3-sni-cases.txt`, `mutations/M-N2-*.txt` |
| AC-3 | I3, I4 (M-N2), I5 | passes unmutated; fails under M-N2 with `IllegalArgumentException` | `I3-sni-cases.txt`, `mutations/M-N2-cases.txt` |
| IC-1 | I0 | count 710, gate totals 710/0/2, both blob hashes | `I0-baseline.txt`, `I0-gates.txt`, `I0-junit-totals.txt` |
| IC-2 | I3, I6 | working-tree forms (porcelain, numstat, diff stat empty, verification metadata unchanged); literal `<base>..HEAD` forms by QA after the IC-12 commit | `I3-sni-diff-check.txt`, `I6-checks.txt` |
| IC-3 | I0, I6 | four-task gate exit 0 before and after, JDK 25, no mutation present | `I0-gates.txt`, `I6-gates.txt` |
| IC-4 | I0, I6 | 710 → 713 working tree; Gradle 713/0/0/2; HEAD form by QA after commit | `I0-baseline.txt`, `I6-checks.txt`, `I6-junit-totals.txt` |
| IC-5 | I6 | `app/build.gradle` unchanged; one new class, name contains `Test`; no helper | `I6-checks.txt` |
| IC-6 | I2, I5 | loopback helper only; greps; three identical runs | `I5-checks.txt`, `I5-run*-cases.txt` |
| IC-7 | I4 | two entries with write-ahead, result, `restored` and hash lines | `mutation-log.md`, `mutations/` |
| IC-8 | all, I8 | sanitised evidence; I8 scan of the whole workspace (every evidence file, the logs, `I6-checks.txt`), the new class and the SNI diff, by pattern and by the bare user and host names, file names and counts only; story file by line number and count; literal `<base>..HEAD` form by QA after the commit | `I8-scan.txt` |
| IC-9 | I2, I3, I5 | no verifier, no `defaultTrustManager`; factory calls only | `I5-checks.txt` |
| IC-10 | I2, I3, I6 | 12 + 8 + 4 existing tests pass; SNI diff additions only | `I2-pinned-cases.txt`, `I3-sni-cases.txt`, `I3-sni-diff-check.txt`, `I6-junit-totals.txt` |
| IC-11 | any | row S5 | `implementation-log.md` (only if triggered) |
| IC-12 | I7, orchestrator | nothing committed by the developer; clock timestamps; counts stated once | `implementation-log.md`, `mutation-log.md` |

## 6. Risks and Out of Scope

| # | Risk | Likelihood / impact | Mitigation |
|---|------|---------------------|------------|
| R1 | Provider wraps the trust manager's exception (direct cause differs) | Low (AC-21 shows no wrapping) / low | Row S1, the story's fallback |
| R2 | A host name is accepted inside the sandbox though rejected on the plain JDK | Very low (Robolectric does not replace `javax.net.ssl`; U-063 M-A1 saw `SNIHostName` reject `::1` in the sandbox) / low | Assertion (iii) on unmutated code catches it; row S3 with four verified substitutes |
| R3 | A mutation is left in place by a lost session | Low / high | Write-ahead entry, recovery note, (g) rule, I5 and I6 pre-checks |
| R4 | The SNI class's new `createSocket` call disturbs the JDK-provider class's sandbox isolation | Very low (the Hardening and Handshake classes already create factory sockets under Conscrypt in the same run) / medium | I3 and M-N2 run both classes in one invocation; the guards would fail by name (row S10) |
| R5 | Evidence leaks a machine path or name | Low / medium | Sanitiser only, I8 scan (pattern and bare names, count-only), `compiled-pipeline.json` excluded from git |
| R6 | Robolectric lock-file flake | Low / low | Row S6 |
| R7 | Endpoint security kills the shell mid-run | Low / medium | Foreground only, stop and report, mutation recovery rule |

Test smells guarded against (Meszaros, *xUnit Test Patterns*, ch. 16 "Behavior Smells" and ch. 15 "Code
Smells"): Erratic Test, in its Interacting Tests and Nondeterministic Test forms (three identical I5 runs;
no sleeps, no name resolution, loopback address object only, every wait bounded; the SNI and JDK-provider
classes run in one invocation with guards A1-G1 to A1-G3 as a tripwire, row S10); Assertion Roulette (every
AC-2 message names the host or the names tried; AC-1's message carries the cause chain); Fragile Test
(AC-1 asserts the exception class and the absence of the mismatch text, not the provider's message wording);
Test Code Duplication (AC-1 sits in its own class instead of a new helper in the existing pinned class, and
nothing of the 12 existing pinned tests is repeated). A mutation that would leave a test green is treated as
an unmet criterion, not hidden (row S7). The production code under test is not changed, so no production
anti-pattern is in scope.

Out of scope: SC-1 (the stale IPv4 comment at `DefaultTrustedSocketFactorySniTest.java:168`) and SC-2 (the
stale sentence at `PinnedCertificateSocketFactoryCreateSocketTest.java:77-79`) are not edited; the new code's
comments point to them. No production file is edited at any time except the two temporary, logged mutations
M-N1 and M-N2. No edit to `TlsTestFixtures.java`, `LoopbackTlsTestServer.java`, any existing test method,
any build file, `gradle/verification-metadata.xml`, CNTR-MODERNIZATION-001, any change record (CR-009,
CR-012, CR-013) or the U-063 artifacts (its tools are run, not changed). No not-yet-valid test (DP-1, option
A). BUG-019 and BUG-020 belong to U-065.

## 7. Constraints for the Developer (binding)

1. Gradle runs in the foreground, with the tool's timeout set to 600000 ms on every call, and never with
   `MSYS_NO_PATHCONV=1` set.
2. If the tool moves a long run to the background by itself, wait for it to finish; do not poll it and do
   not start another command.
3. No background or detached shells, no `&`, no polling or `sleep` loops, no helper agents for shell work,
   no heredocs for editing.
4. If a shell call fails with "bash.exe ... not found", or a process is killed for no visible reason, stop
   and report (endpoint security); do not work around it.
5. Timestamps in the logs come from `date -u +%Y-%m-%dT%H:%M:%SZ`, never typed.
6. No production file is edited except M-N1 and M-N2, each restored within its own step.
7. Existing test methods and comments are not edited; the SNI file change is one additions-only hunk.
8. Raw logs stay in the work folder in the session scratch folder; nothing enters the repository unless
   it went through the filters of section 2.
9. Commit nothing. The orchestrator commits the story's own paths, with the product owner's permission,
   before QA (IC-12).

## 8. Contracts Verified

CNTR-MODERNIZATION-001 (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`,
`status: approved`) is read as context; this story neither edits nor produces it.

- **What AC-1 relies on.** Producer table, `PINNED_CERTIFICATE` row (`:96`): the pinned factory's
  `checkServerTrusted()` performs "SHA-256 fingerprint equality + validity-window check" and throws
  `CertificateException` on mismatch. Error table (`:197`): the pinned trust manager throws
  `CertificateException` for a "SHA-256 mismatch or outside validity window", and the connection fails
  closed. AC-1 tests the "outside validity window" half through the real handshake path; its assertion (iii)
  shows the fingerprint half passed. The test is consistent with DES-MODERNIZATION-002 (`:178`, `:206`,
  "Validity window (not-before / not-after / expiry)") and REQ-MODERNIZATION-002 (no accept-all path). The
  contract's v1 wording says errors are thrown from `createSocket(...)` at handshake time (`:191-192`,
  `:196`). CR-012 (`sdlc/artifacts/build/change-records/CR-012-cntr-001-rejection-surfaces-at-handshake.md`,
  approved) corrects that wording, but it is held for the combined contract revision (Record 7, "Hold for
  the migration"; CR-013 `:46`), so the contract still carries the v1 text. AC-1 follows CR-012's target
  text: `createSocket` returns normally, the rejection is raised by `startHandshake()` as an `SSLException`
  whose direct cause is the `CertificateException` (here its subclass `CertificateExpiredException`), as
  U-063's AC-21 already does. Reviewers and QA should read the v1 wording as a known erratum, not as a
  mismatch with the test. CR-012 keeps the pinned error row `:197` as it is because that rejection "has a
  `CertificateException` direct cause" (CR-012 `:247`); if row S1's fallback is ever needed, that premise is
  wrong, and the hand-off reports it as a finding against CR-012.
- **What AC-2 and AC-3 rely on.** Nothing in the contract. CNTR-MODERNIZATION-001 states no SNI behaviour,
  and in particular nothing about a host name the platform rejects. CR-013
  (`sdlc/artifacts/build/change-records/CR-013-cntr-001-state-sni-behaviour.md`, approved, applied at the
  combined revision) proposes statement S5 (`:317`): "If `host` passes the checks of S2 to S4 but the
  platform's server-name class rejects it as malformed, the factory itself sets no server name,
  `createSocket(...)` does not throw because of it, and returns the socket so that the connection proceeds."
  (The shorter phrase "the factory skips SNI and the connection continues" is CR-012's candidate clause,
  `:292`, which CR-013 kept as S5.) CR-013 names these tests as the only protection of S5 (`:378`). Clause by
  clause: "passes the checks" is AC-2 (i); "rejects it" is the I1 precondition and M-N2; "sets no server
  name" is AC-2 (iii) at helper level; "does not throw" is AC-3 (and AC-2 (ii) at helper level); "returns the
  socket" is AC-3's `SSLSocket` assertion. Not shown: the layered socket shape and "no server name" at the
  `createSocket` level (not observable under the default provider); that gap is CR-013's Scope Concern D.
  The two tests pin present behaviour of the vendored code (`DTSF:248-253`, BUG-015), not a contract clause.
- **A constraint passed on to U-065.** AC-2 (iii) observes "`setSSLParameters` is never called", which is
  stricter than S5's "sets no server name"; it is the observable the existing BUG-015 tests already use
  (`DefaultTrustedSocketFactorySniTest.java:180`, `:195`, `:209`, `:224`, `:238`). U-065 plans work on the
  same `getSSLParameters` / `setSSLParameters` round trip (U-065 story `:221`) and must keep U-064's tests
  passing unchanged. If U-065 makes `setSniViaSSLParameters` call `setSSLParameters` for another reason (for
  example protocols) with no server name, AC-2 (iii) fails although S5 still holds, and its message ("so
  SNIHostName accepted it here") would then point the wrong way. U-065's planner should be told; the test is
  not changed here.
- No new boundary is introduced; `integration_contracts: []` is correct for a tests-only story. Contract
  gate: no block.

## 9. Quality Attribute Coverage (ISO/IEC 25010)

All eight characteristics were evaluated; those that do not apply are marked so.

| Attribute | How this story serves it |
|-----------|--------------------------|
| Functional suitability (correctness, completeness) | AC-1 pins a required behaviour of CNTR-MODERNIZATION-001 (`:96`, `:197`); AC-2 and AC-3 pin CR-013's S5 as the code behaves today. M-N1 and M-N2 show each test discriminates. Completeness is deliberately reduced by DP-1 (no not-yet-valid case) |
| Performance efficiency (time behaviour) | Not applicable to production (no production change). Test cost: one loopback handshake and two in-memory tests; every wait bounded at 10 s by `LoopbackTlsTestServer`; no sleeps |
| Compatibility (co-existence, interoperability) | Co-existence: the SNI class's new `createSocket` call runs in the same test JVM as `DefaultTrustedSocketFactoryJdkProviderTest`; checked by running both in one invocation with guards A1-G1 to A1-G3 (R4, row S10). Interoperability with a real IMAP server is not shown |
| Usability | Not applicable: no user-facing surface changes. Indirectly, AC-3 protects a user who types a malformed host name from a failure at socket creation |
| Security (integrity, authenticity) | Locks in fail-closed rejection of an expired pinned certificate before the CR-009 move (U-063 security review W-1) |
| Reliability (fault tolerance) | Locks in the SNI safety net so a malformed user-entered host name cannot crash socket creation (BUG-015) |
| Maintainability (testability, modifiability) | Each protection is now covered on its own and proven by a mutation; the move can be checked against it |
| Maintainability (analysability) | Failure messages name the host and the cause chain; Javadoc states what each test can and cannot prove |
| Portability | Not improved: desktop JVM and test providers only; device behaviour remains unshown (Known limits) |

## 10. Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-064 story | `sdlc/artifacts/build/sprints/sprint-021/U-064-tls-baseline-gap-tests-pinned-expiry-and-sni-catch.md` | Scope, AC-1 to AC-3, IC-1 to IC-12, DP-1 decision |
| Sprint 021 plan and execution plan | `sdlc/artifacts/build/sprints/sprint-021/sprint-plan.md`, `execution-plan.json` | Ordering against U-065 (L25) |
| CNTR-MODERNIZATION-001 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md` | Lines 96 and 197 (section 8) |
| CR-013 | `sdlc/artifacts/build/change-records/CR-013-cntr-001-state-sni-behaviour.md` | SNI statement S5 (`:317`), hold note (`:46`), header and lines 145-170, 378 |
| CR-012 | `sdlc/artifacts/build/change-records/CR-012-cntr-001-rejection-surfaces-at-handshake.md` | Approved, held; target text for where a rejection surfaces (`:247`, `:292`) (iteration 2) |
| CR-009 | `sdlc/artifacts/build/change-records/CR-009-cntr-001-default-trusted-socket-factory-producer.md` | Named only: the move these tests protect; its content was not re-read (the contract-verification gate read it in full) |
| U-065 story | `sdlc/artifacts/build/sprints/sprint-021/U-065-tls12-floor-both-factories-and-pinned-factory-parity.md` | `:219-223`, `:250-254`: the round trip and the interaction with U-064 (iteration 2) |
| Gate results of iteration 1 | `sdlc/artifacts/build/sprints/sprint-021/U-064/plan-review.md`, `contract-verification.md`, `expert-depth-check.md` | Findings addressed in section 0 (read in full) |
| REQ-MODERNIZATION-002 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-002-eliminate-transport-security-mitm.md` | Searched for validity, expiry and SNI clauses (approved) |
| DES-MODERNIZATION-002 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-002-transport-security-hardening-design.md` | Searched for the validity-window clause (approved; `:178`, `:206`) |
| Pinned factory | `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` | Read in full (M-N1 target) |
| Default factory (vendored) | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` | Read in full (M-N2 target) |
| Vendored trust-manager factory | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java` | `:25-61`, `:128-148` (AC-3 path, IC-9) |
| Pinned create-socket test | `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java` | Read in full (AC-1 model, SC-2) |
| Pinned trust-manager test | `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryTest.java` | Read in full (IC-10, no overlap) |
| SNI test | `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java` | Read in full (insertion point, conventions) |
| JDK-provider test | `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java` | Read in full (guards, 5 tests) |
| Fixtures | `app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java` | `:1-95` header, constants list, `:325-397` helpers |
| Loopback server | `app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java` | Read in full |
| Robolectric config | `app/src/test/resources/robolectric.properties` | `sdk=29` |
| Build file | `app/build.gradle` | `:215-464` JaCoCo block |
| U-063 plan | `sdlc/artifacts/build/sprints/sprint-020/U-063/plan.md` | Frontmatter, revision history, conventions `:560-640` (shape only) |
| U-063 mutation log | `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/mutation-log.md` | `:1-150` protocol, M-A1, M-A1g, M-A1c |
| U-063 evidence tools | `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/` (`sanitize.py`, `junit_cases.py`, `junit_totals.py`, `junit-xml.init.gradle`) | Reused read-only |
| U-063 evidence samples | `.../U-063/evidence/mutations/M-A1c-cases.txt`, `I7-junit-totals.txt`, `I7-gates.txt`, `I7-count.txt`, `mutations/I6a-baseline-end-attempt1-cases.txt` | Expected output forms |
| U-063 story | `sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md` | Implementation Notes `:285-340` (environment, deviations 13 to 16) |
| U-063 implementation log | `sdlc/artifacts/build/sprints/sprint-020/U-063/implementation-log.md` | `:742`, `:785` (lock-file flake) |
| Sprint 020 retro | `sdlc/artifacts/build/sprints/sprint-020/retro.md` | Lessons (compile-checked mutations, clock timestamps, timeouts, commit before QA) |

Not read in full: the other approved REQ and DES artifacts of the `modernization` domain (24 approved REQs).
The story has `requirements: []` and `design_docs: []`, no REQ of `type: standard` exists in the corpus (a
search found none), and the two transport-security artifacts above were searched for the relevant clauses.

## Verdict

**PASS.** The story's facts hold against the code at `de03e99b` (L2 to L7, L14 to L20), the one unverified
environment fact of the story's first step (a) is now verified on the build JDK (L8, L9), and (b) is covered
by I2 with stop rules. Both mutations compile as written and are single-file; the insertion point keeps the
12 existing SNI methods byte-identical; no fixture, helper, build or production change is needed. The two
remaining `needs_check` items (L11/L12, L24) are developer steps with stop rules (rows S1, S2, S8).
Iteration 2 resolves plan-review B-1 by moving the IC-8 scan to a final step I8 whose output stays outside
the scanned folders, prints names and counts only, covers every evidence file and the bare user and host
names, and has stated exit codes with a row for an error exit. The other gate findings are handled as
listed in section 0. The I8 commands' exit-code behaviour was tried in a scratch folder outside the
repository, and a read-only run of the pattern and bare-name scans over the workspace as it is now gave no
match.
