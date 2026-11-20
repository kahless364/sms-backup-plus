---
artifact_type: implementation-log
story_id: "U-063"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-11-17"
files_changed: 0
files_created: 0
tests_added: 0
tests_passing: 682
---

# Implementation Log: U-063

Tests-only story. No product, vendored, build or dependency file is changed. Placeholders: `<work>` (the work folder, kept outside the repository), `<repo>`.
Runtime Claims: none. The deliverable is the tests themselves. Their runtime evidence is the per-test case files of increments I2b to I5 (`evidence/I2b-jdk-cases.txt`, `evidence/I3-hardening-cases.txt`, `evidence/I4-handshake-cases.txt`, `evidence/I5-pinned-cases.txt`) and the mutation log (`evidence/mutation-log.md`). The one limit is device behaviour, which these desktop-JVM tests cannot show.
Contract Adherence: no `integration_contracts` in the story; CNTR-MODERNIZATION-001 is read-only context.

## Rulings in force

1. Work folder `$WORK` is `<work>`: a `u063-work` sub-folder of the session scratch folder (plan note 10 / W6 fallback), chosen by the orchestrator. Consequence: it does not survive a new session, so a lost session between I0b and I1 means redoing I0b; snapshots are lost on a new session. Nothing is written outside `<work>`; its absolute path is never written into the repository.
2. The plan's generic `restore_ok` in the probe block list means the two keys `p8_restore_ok` and `p9_restore_ok` (relevant to I0b).
3. Two extra record-only probe keys for I0b: one recording the JDK security property `jdk.tls.disabledAlgorithms`, one recording the protocol each handshake negotiated. They select no branch and are not an unexpected-output stop.
4. No commit, stash, reset, clean, restore or broad checkout by the Developer. Other uncommitted work in the tree is not U-063's and is left alone: the story file, the sprint-020 files, a one-line change in `sdlc/artifacts/build/change-records/CR-009-*.md`, `sdlc/.skill-delegation-audit.log`, `sdlc/.skill-context.json`.
5. **Plan amendment A1 (2026-11-17)** is in force (plan section "Amendment A1" and the edits marked "(A1)"); 12 turns (I2b added); new `DefaultTrustedSocketFactoryJdkProviderTest` (28 new tests, 710 at the end, fallback 26 and 708); `LoopbackTlsTestServer` follows V5 (A1.b); no host-name verifier swap in the Conscrypt-mode classes (D3b). I0c is an authorised diagnostic outside the plan; its additions are accepted as facts and no further additions are to be made. `evidence/tools/diag_extract.py` stays as evidence tooling.
6. **Plan-review A1 warnings, binding as written (where the plan text and a warning differ, the warning wins):**
   - **A-W1:** the JDK-provider class Javadoc explains that `@LooperMode(LEGACY)` is there only to give the class its own Robolectric sandbox (sandboxes are keyed on looper mode but not on Conscrypt mode), and that guards A1-G1 to A1-G3 check this every run (I2b).
   - **A-W2:** in the JDK class `@Before`, guard A1-G3 runs first (one unconnected default socket, catching `NoClassDefFoundError` and `ExceptionInInitializerError`), then the provider check of A1-G1, then the reflective `ENABLED_CIPHERS` part of A1-G1, with `ExceptionInInitializerError` around that read failing as "A1-G3 (via factory static init)" (I2b).
   - **A-W3:** a failure of application start-up under the chosen looper mode at I2b is a stop and report (remedies for the orchestrator: `INSTRUMENTATION_TEST` or a plain `Application` through `@Config(application = ...)`); record the expected `javac` deprecation note for LEGACY (I2b).
   - **A-W4:** for a lost I2b pre-check, RECOVER-I6 step 4 compares against the I2 END manifest plus the single JDK-class path, step 5 is skipped, and the resume rule deletes the class and redoes I2b; Notes item 8's "dispatch nothing else" extends to a session lost during the I2b pre-check (I2b, recovery).
   - **A-W5:** the I7 commands are written with all six classes and the numbers 12 and 38 (I7.1 IC-8 list, I7.3 IC-6 command, I7.6 `find`, I7.4(c)); stale descriptive numbers corrected (I7).
   - **A-W6:** the I2 section comment cites RFC 6066 section 3 (I2).
   - Notes A-N3 (A1-G3 is a permanent tripwire; the JDK class Javadoc says a failure points at the other test) and A-N4 (A1-G1 failure message prints the provider and the first entries of `ENABLED_CIPHERS`) are also to be applied at I2b.
   None of A-W1 to A-W6 changes I1; for I1 the relevant text is A1.b (server), A1.h item 1 (the "After the CR-009 move" paragraph, on both helpers) and the fixtures as recorded in `evidence/I0-fixtures-summary.txt`.

## I0a START 2026-11-17T20:48Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I0a.1 Start check and folders

- `git rev-parse HEAD` = `395000956bb46cb2acc6d66e62d4b5ef933a2d90` (equals BASE).
- `git diff --quiet BASE HEAD -- $PROD`: exit 0 (pass).
- `git status --porcelain --untracked-files=all -- $PP`: empty (pass).
- Created `<work>/logs`, `<work>/snapshots`, `<work>/gen` (write test inside `<work>/logs` succeeded; W6 not triggered). Created `evidence/` and `evidence/tools/` in the workspace.
- `junit_totals.py` and `jacoco_pkgs.py` copied byte-for-byte from the sprint-018 U-061 tools.

### I0a.2 Tools and counts

- Appendix A tools written with the Write tool under `evidence/tools/`: `junit-xml.init.gradle`, `junit_cases.py`, `probe_extract.py`, `sanitize.py`, `gen-fixtures.sh`, `ExtractFixtures.java`; `junit_totals.py` and `jacoco_pkgs.py` copied from U-061. (`python -I -X utf8` works.)
- Counts (`evidence/I0-count.txt`): CLAUDE.md `HEAD` form = 682; working-tree count C = 682. Both as expected, so the targets stay 682 and 708.

### I0a.3 Full baseline gate (IC-3), clean

- Command: `rm -rf app/build/test-results/testDebugUnitTest; ./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease` (no `MSYS_NO_PATHCONV`, no `JAVA_HOME`). Raw log in `<work>/logs/I0-gates.log`.
- Result: `BUILD SUCCESSFUL in 2m 40s`, `156 actionable tasks: 148 executed, 8 up-to-date`, `exit=0`. Daemon JVM line: `U063-DAEMON-JVM: java.version=25.0.5.0.3`. Sanitised summary: `evidence/I0-gates.txt`.
- Totals measured from the XML (`evidence/I0-junit-totals.txt`): `files=73 tests=682 failures=0 errors=0 skipped=2`. Matches the expected 682 / 0 / 0 / 2.
- Per-class baseline for the I7.4 ordered-suite check (`evidence/I0-suite-children-baseline.txt`): `BackupImapStoreTest cases=11`, `K9MailTransportTest cases=44` (files=2 cases=55 pass=55).

### I0a.4 Coverage (IC-1, IC-5)

`evidence/I0-jacoco-packages.txt` (from `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`):
- `com.zegoggles.smssync.mail 531/743 71.5%`
- `com.zegoggles.smssync.mail.transport 241/287 84.0%`

These are the baseline figures that later increments must not drop below. `jacocoTestCoverageVerification` passed in the baseline gate.

### I0a.5 Dependency trees (IC-1, IC-11)

`evidence/I0-deps-debugUnitTestCompileClasspath.txt` (408 lines), `evidence/I0-deps-debugUnitTestRuntimeClasspath.txt` (489 lines); search result in `evidence/I0-deps-check.txt`:
- Runtime tree: `org.conscrypt:conscrypt-openjdk-uber:2.5.2` (line 160) and `org.bouncycastle:bcprov-jdk18on:1.80` (line 233), both under Robolectric.
- **Differs from the plan's expectation ("hits in the runtime tree only"):** the compile tree also lists `org.bouncycastle:bcprov-jdk18on:1.80` (line 363), as a transitive entry directly under Robolectric (`app/build.gradle` has no bouncycastle line; grep count 0). No conscrypt entry in the compile tree. Recorded as the plan says; not a stop condition. The new tests import nothing from `org.conscrypt` or `org.bouncycastle` (to be checked at I7).

### I0a.6 IC-12

`evidence/I0-ic12-grep.txt`: `lines=0` (expected 0).

### I0a.7 Environment (W10)

`evidence/I0-env.txt` (sanitised): JDK `25.0.5.0.3` LTS, Gradle 9.5.1, Launcher JVM 25.0.5.0.3, Daemon JVM "Compatible with Java 25, any vendor" from `gradle/gradle-daemon-jvm.properties`.

### I0a.8 Closing checks

- `git rev-parse HEAD` still `395000956bb46cb2acc6d66e62d4b5ef933a2d90`.
- `git diff --quiet BASE -- $PROD`: exit 0 (production paths clean against the start commit).
- `git status --porcelain --untracked-files=all -- $PP`: empty. `evidence/I0a-manifest.txt` written (no U-063 product files yet).
- Hygiene scan over `evidence/` and this log (e-mail pattern, user-profile path, application-data folder name, user name, host name): 0 hits.
- No source, build, vendored or dependency file touched; no commit, add, stash, reset, clean or checkout run.

### Carried to I0b

- `$WORK` is `<work>`; `<work>/gen` is empty and ready for `gen-fixtures.sh`. `$JDK25` is derived with the plan's `cygpath` line at I0b.
- `restore_ok` means `p8_restore_ok` and `p9_restore_ok` (ruling 2).
- Two extra record-only probe keys (ruling 3): `jdk_tls_disabled_algorithms` (value of the security property `jdk.tls.disabledAlgorithms`) and `negotiated_protocols` (the protocol each P9/P10 handshake negotiated; the existing `protocol=<p>` field already carries it per handshake, the extra key summarises it). They select no branch and are no unexpected-output stop.
- Coverage floors to hold: mail 531/743, mail.transport 241/287. Suite-children baseline: BackupImapStoreTest 11, K9MailTransportTest 44.

## I0a END

```yaml
handoff:
  increment: "I0a"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease", summary: "evidence/I0-gates.txt", exit: 0, junit: "files=73 tests=682 failures=0 errors=0 skipped=2"}
  count_worktree: 682
  count_head: 682
  product_paths: []            # git status --porcelain --untracked-files=all -- $PP is empty
  manifest: "evidence/I0a-manifest.txt"
  escalations: []
  decisions_needed: []
  bug_requests: []
  recovered: false
  notes:
    - "compile classpath also lists bcprov-jdk18on:1.80 (transitive via Robolectric); recorded, not a stop"
    - "coverage baseline: mail 531/743 71.5%, mail.transport 241/287 84.0%"
    - "suite-children baseline: BackupImapStoreTest 11, K9MailTransportTest 44"

## I0b START 2026-11-17T20:54:38Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I0b.1 Start check

- HEAD equals BASE; `git diff --quiet BASE -- $PROD` exit 0; product porcelain empty (equals the I0a manifest). `<work>` survived from I0a (`gen/` empty, `logs/`, `snapshots/`).

### I0b.2 Fixture generation (DP-1, IC-13)

- `$JDK25` = the `java.home` of the `java` on PATH (JDK 25.0.5.0.3, `bin/keytool.exe` present).
- `bash evidence/tools/gen-fixtures.sh <work>/gen`: 24 sequential keytool processes, exit 0, printed `U063-GEN done`; the raw log (`<work>/logs/I0-gen.log`) contains no "error", "exception" or "illegal" line (count 0). No keytool option was rejected.
- `java evidence/tools/ExtractFixtures.java <work>/gen`: exit 0; 10 constants written to `<work>/gen/constants.txt` (never enters the repository), 6 provenance lines to `summary.txt`, sanitised into `evidence/I0-fixtures-summary.txt`.
- IC-13 validity check (from `evidence/I0-fixtures-summary.txt`):
  - `trusted-ca`: 2019-12-01 to 2119-11-07: PASS. `untrusted-ca`: same: PASS.
  - `right-host`, `other-host`, `untrusted-host`: 2020-01-01 to 2107-08-13: PASS (start before 2025-01-01, end after 2100-01-01).
  - `expired-host`: 2020-01-01 to 2020-01-31: PASS (ends before 2025-01-01).
  - Subjects/SAN as planned; `right-host`/`untrusted-host`/`expired-host` SAN `imap.right.test`, `other-host` SAN `imap.other.test`.

### I0b.3 Probe (throwaway), run, and clean-up

- Fixtures source assembled from `<work>/gen/constants.txt` plus a head and tail with loader helpers (certificate, privateKey, serverContext, trustManagerFor, chain). Two throwaway files under `app/src/test/java/com/zegoggles/smssync/mail/ssl/`: `U063ProbeFixturesTest.java` and `U063ProbeTest.java` (one test method per block P1 to P8, and one combined P9+P10 method; 10 test methods).
- First compile failed on `Provider.getVersionStr()` (not in the compile-time API view); changed to `getVersion()`. Second run: `BUILD SUCCESSFUL in 32s`, `exit=0`, one XML file.
- Run: `./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.U063ProbeTest"`; extracted with `probe_extract.py` (`probe_lines=65`), sanitised to `evidence/I0-probe.txt` (sanitiser wrote 66 lines, no refusal). 65 lines = the 63 unconditional keys plus the two accepted record-only keys (`jdk_tls_disabled_algorithms`, `negotiated_protocols`). A script check found no required key missing, no `EXCEPTION` value, no `ms` above 10000, no `TIMEOUT`, and no variant key. `hs_implicit_right_no_verifier_swap` is absent, as D3b requires.
- Clean-up: both probe sources removed with `rm`. Check that no stale probe class remains under `app/build`: after the `rm`, `find app/build -iname '*U063*'` still found 5 classes in `app/build/intermediates/javac/...`, 5 in `app/build/intermediates/classes/debugUnitTest/transformDebugUnitTestClassesWithAsm/dirs/...` (the directory the test run uses), the probe XML, and 5 javac transaction backups. I ran `:app:compileDebugUnitTestJavaWithJavac` (javac copies removed) and `:app:transformDebugUnitTestClassesWithAsm` (ASM copies removed), deleted `app/build/test-results/testDebugUnitTest`, and deleted the 5 named javac backup files in `app/build/tmp/.../stash-dir/`. Final `find app/build -iname '*U063*'` = 0 files.
- Tree: `git status --porcelain --untracked-files=all -- $PP` is empty; `git diff --quiet BASE -- $PROD` exit 0; count C = 682, `HEAD` count = 682. `evidence/I0b-manifest.txt` written.
- Plan deviations (minor): `U063ProbeFixturesTest` has no `@RunWith` (the plan lists both probe files as Robolectric-run; a Robolectric runner on a class with no test methods would add an initialization-error failure, and Gradle only runs classes with test annotations; the class name still contains "Test"). Blocks P9 and P10 are one method so that `negotiated_protocols` is a single line. The probe source was not kept (the plan does not ask for it); a diagnostic re-run needs it rewritten.
- Key material: `<work>/gen` held the keytool PKCS12 stores (both CA private keys and the four leaf keys), CSRs and certificates. Deleted now: all `*.p12` and `*.csr` (this removes both CA private keys). Check: `ls <work>/gen` afterwards lists `constants.txt`, `summary.txt`, `FixturesHead.txt`, `FixturesTail.txt` and the six `*.crt` files (public); no `.p12` file. `constants.txt` still holds the four leaf private keys (throwaway, test-only, needed by I1); the plan deletes `<work>/gen` at I1 step 5. The store password `u063-test-only` is a test-only literal, used only in these throwaway stores and the throwaway probe file; it is not reused from anything real. Nothing from `<work>/gen` entered the repository (`evidence/` holds only the sanitised summary and probe output).

### I0b.4 Probe output and decision-table application (IC-11)

Full sanitised output: `evidence/I0-probe.txt`. Environment facts (R0): provider list `Conscrypt:1.0, SUN, SunRsaSign, SunEC, SunJSSE, SunJCE, SunJGSS, SunSASL, XMLDSig, SunPCSC, JdkLDAP, JdkSASL, SunMSCAPI, SunPKCS11, BC:1.8`; `SSLContext("TLS")`, `TrustManagerFactory("X509")`, `CertificateFactory("X.509")` and `KeyFactory("RSA")` all come from Conscrypt; `KeyManagerFactory` from SunJSSE; `KeyStore` default from SUN. Default host-name verifier class `org.robolectric.android.internal.AndroidTestEnvironment$1`.

| Key or block | Value measured | Row matched |
|---|---|---|
| `sdk_int`, `java_version` | `29`, `25.0.5.0.3` | D1a (continue) |
| `providers`, 6 `prov_*`, `hv_default_class`, 8 of the P6 lists, `sslv3_supported`=false, `sslv2hello_supported`=false, `tls10_supported`=true, `tls11_supported`=true, `jdk_tls_disabled_algorithms`, `negotiated_protocols` | see probe file | R0 / record-only |
| `fixtures_load` | `OK` | D2a (continue) |
| `hv_right_on_right`, `hv_right_on_other` | `true`, `false` | D3b (continue, no swap); `hs_implicit_right_no_verifier_swap` absent, so D10e' |
| `sni_ctor_eight_group`, `sni_ctor_1to8` | both `IAE Contains non-LDH ASCII characters` | D4a (expected) |
| `sni_obs_dns` | `null` | **D5b: STOP** |
| `sni_obs_ipv6` | `null` | D5a2 (expected) |
| `sni_obs_pinned_implicit` | `null` | D5e (expected) |
| `fixed_suites_F` | `[TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384, TLS_CHACHA20_POLY1305_SHA256]` | D6' (continue); content R0 |
| four `f_bound_*` | all `true` | D6b (continue) |
| `cipher_order_differs` | `true` | D7a' (continue) |
| `protocols_differ`, `supported_minus_sslv3_equals_default_set` | `true`, `true` | **D7b: STOP** (second operand is `true`) |
| `tls10_default`, `tls11_default` | `true`, `true` | D7c (continue), overridden by D7b (precedence: stop wins) |
| `tls10_hardened`=`tls10_supported`, `tls11_hardened`=`tls11_supported` | equal | D7e' (continue) |
| `cipher_order_matches_reflected`, `protocol_order_matches_reflected` | `true`, `true` | D7d' (continue) |
| P7 four booleans | all `true` | D8a (continue) |
| `tmf_field_rw`, `localkeystore_null`, `tmf_default_tm_class` | `OK`, `true`, `org.conscrypt.TrustManagerImpl` | D9a (continue) |
| `p8_restore_ok`, `p9_restore_ok` | `true`, `true` | D9r' (continue) |
| `hs_implicit_right` | `OK protocol=TLSv1.3 suite=TLS_AES_128_GCM_SHA256 byte=-1 server=FAILED:javax.net.ssl.SSLHandshakeException ms=42` | **D13(d): STOP** (control `OK` with `server` other than `OK`) |
| `hs_starttls_right` | `OK protocol=TLSv1.3 suite=TLS_AES_128_GCM_SHA256 byte=-1 server=FAILED:javax.net.ssl.SSLHandshakeException ms=8` | **D13(d): STOP** |
| six rejection keys | all `REJECTED`, `direct_cause=com.fsck.k9.mail.CertificateChainException`; `marker=true` for both `other`, `marker=false` for the four `untrusted`/`expired`; server `FAILED:javax.net.ssl.SSLHandshakeException`; `ms` 7 to 14 | the rejection part of D10a holds; none of D10c, D10d, D10g, D10h applies |
| `pin_accept` | `OK protocol=TLSv1.3 suite=TLS_AES_128_GCM_SHA256 byte=-1 server=FAILED:javax.net.ssl.SSLHandshakeException ms=8` | **D11: STOP** (`pin_accept` OK with `server` other than `OK`) |
| `pin_reject` | `REJECTED ... direct_cause=java.security.cert.CertificateException marker=true ... ms=7` | the reject part of D11a holds |
| every `ms`; every `server` field | max 42; no `TIMEOUT` | no D12 |
| D10b, D10f | no control is `REJECTED` | not applicable (no server variants tried) |
| D13(a) to (c) | no key missing, no `EXCEPTION`, every value in form | not applicable (D13(d) fires as above) |

Observations for the orchestrator (record-only, not decisions):
- In the three control cases the client completed the handshake at TLS 1.3 but read end-of-stream (`byte=-1`), and the server task reported `FAILED:javax.net.ssl.SSLHandshakeException` instead of writing the payload. The server's exception message was not captured (the probe prints only the plan's keys), and I have not tested why. The server side is built from the Conscrypt `SSLContext` (`prov_sslcontext_tls`) with SunJSSE key managers. Cause unverified.
- The provider is Conscrypt (inserted first by Robolectric), so `jdk.tls.disabledAlgorithms` (which lists `SSLv3, TLSv1, TLSv1.1, ...`) does not govern it: Conscrypt enables `TLSv1` and `TLSv1.1` by default. `default_enabled_protocols` = `[TLSv1, TLSv1.1, TLSv1.2, TLSv1.3]`, `hardened_enabled_protocols` = `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3]` (TLSv1.3 is appended last as an "unknown" protocol).
- `cipher_order_differs` and `protocols_differ` were computed as plain ordered-list inequality. Restricted to the names present in both lists they also differ, and the cipher and protocol sets are equal, so both readings give `true`; the choice changes no row.
- The F suites are exactly the three TLS 1.3 suites, at the front of the provider's list.
- `blocked_in_default` is `[]`: Conscrypt's default already omits all 16 blocked names.

### I0b.5 Result: stop rows fired

Stop rows that match: **D5b** (`sni_obs_dns` = `null`), **D7b** (`supported_minus_sslv3_equals_default_set` = `true`), **D11** (`pin_accept` OK with `server` = `FAILED:...`), **D13(d)** (`hs_implicit_right` and `hs_starttls_right`: control OK with `server` other than OK). D7c matches but is overridden by D7b.

Consequences as the plan states them:
- D5b: the observable does not report the name `setSniHost` set, so AC-3 would be vacuous and AC-4 cannot pass (AC-3, AC-4).
- D7b: the first AC-16 test could not detect mutation M-D13 or M-D11 (the provider's default set already equals supported minus SSLv3), so AC-16 would be unmet (Criterion Status Rule, rule 4); the Developer does not start Group D.
- D11: stop for AC-20/AC-21 (part of the AC-24 split).
- D13(d): stop and report under `decisions_needed` with the probe keys, nothing further. It also puts Groups B and C in question, since the controls (AC-5, AC-7) did not behave as D10a expects.

Nothing further was done: no server variant was tried, the probe was not changed or re-run, and I did not start I1.

## I0b END

```yaml
handoff:
  increment: "I0b"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "TR com.zegoggles.smssync.mail.ssl.U063ProbeTest (probe only; no gate G in I0b)", summary: "evidence/I0-probe.txt", exit: 0, junit: "probe run: 10 test methods, 65 probe lines; probe deleted"}
  count_worktree: 682
  count_head: 682
  product_paths: []            # porcelain for $PP empty; probe deleted
  manifest: "evidence/I0b-manifest.txt"
  escalations:
    - "D5b: sni_obs_dns=null (AC-3, AC-4)"
    - "D7b: supported_minus_sslv3_equals_default_set=true; Conscrypt enables TLSv1/TLSv1.1 by default (AC-16; Criterion Status Rule rule 4)"
    - "D11: pin_accept OK with server=FAILED:javax.net.ssl.SSLHandshakeException (AC-20, AC-21)"
    - "D13(d): hs_implicit_right and hs_starttls_right OK with server=FAILED:javax.net.ssl.SSLHandshakeException (AC-5, AC-7)"
  decisions_needed:
    - "Rule on D5b, D7b, D11 and D13(d). Plan consequence for D7b: Group D is not started. D11 and D13(d) may be an AC-24 split."
    - "Whether a diagnostic re-run that captures the server-side exception message (and D10b-style server variants) is permitted."
    - "TLS 1.0 and 1.1 are enabled by default in the unit-test provider (Conscrypt): context for the DP-2 question to the Security Reviewer."
  bug_requests: []
  recovered: false
```

## I0c START 2026-11-17T21:10:29Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

**I0c is an orchestrator-authorised diagnostic outside the plan (recorded as a deviation).** It gathers facts only: it makes no decision and starts no increment. It follows the orchestrator's ruling on the I0b stops (D5b, D7b, D11, D13(d)). Limits: at most three Gradle test runs, throwaway classes under `app/src/test` with "Test" in every name, client sockets only through the two factories and never reconfigured, only the test-CA trust manager in the vendored field (restored), output lines prefixed `U063-DIAG`, evidence in `evidence/I0c-diag.txt`. Compile-only runs (`:app:compileDebugUnitTestJavaWithJavac`) to fix compile errors are not counted as test runs.

### I0c.1 Start check

- Three throwaway classes plus a fixtures copy (`U063DiagFixturesTest`, no test annotation; `U063DiagSupportTest`, no test annotation; `U063DiagTest` with blocks S, V, N; `U063DiagOffTest` with block C and `@ConscryptMode(ConscryptMode.Mode.OFF)`), a new small tool `evidence/tools/diag_extract.py`. One compile-only run: compiled at the first attempt, including the `ConscryptMode` annotation.
- Gradle test run 1 of 3 (`--tests U063DiagTest --tests U063DiagOffTest`): `BUILD SUCCESSFUL in 2m 23s`, exit 0, 195 `U063-DIAG` lines. Results are kept in `evidence/I0c-diag.txt` and are superseded/extended by the run-2 output (see below). Run 1 findings: the Conscrypt-mode server fails in every handshake with `InaccessibleObjectException ... java.net.InetAddress.holder() ... module java.base does not "opens java.net"`; the SunJSSE server context cannot initialise in Conscrypt mode (`NoClassDefFoundError: Could not initialize class sun.security.ssl.NamedGroup$SupportedGroups`); variant 4 made `hs_starttls_right` pass completely; the wire SNI was not obtainable because both server kinds failed.

### I0c.2 Run 2 and run 3

- Run 2 (`BUILD SUCCESSFUL in 55s`, exit 0, 95 lines): added a raw TLS ClientHello capture server (JDK sockets only, parses the `server_name` extension), a stack-frame print for the server failure, a SunJSSE initialisation check, one extra server variant (V5, see below), and in the Conscrypt-OFF class a second set of controls with the test-owned SAN verifier installed and restored (`C2`).
- Run 3 (`BUILD SUCCESSFUL in 24s`, exit 0, 25 lines): only `U063DiagTest.n2_block`: the client connects to a loopback address that carries a host name (`InetAddress.getByAddress(name, loopbackBytes)`, no DNS lookup) to see which name the ClientHello carries.
- Test runs used: 3 of 3 (compile-only runs not counted: three).
- Evidence: `evidence/I0c-diag.txt` (327 lines: the verbatim `U063-DIAG` output of runs 1, 2 and 3 in that order, sanitised, no refusal). Raw logs stay in `<work>/logs`.
- Deviations from the instruction text, all inside the "facts only" rule:
  - Block V: variant 2 was skipped as instructed (the server context is Conscrypt, not SunJSSE). Variants 1, 3, 4 did not meet the condition (see results). I added **V5** (not in the plan's list): the server accepts on a plain socket and immediately wraps it with the loopback address string as the host, with no greeting, for the implicit shape too. The client side is unchanged. V5 meets the condition.
  - Block N: the SunJSSE server context could not be used (it cannot initialise next to Conscrypt) and the Conscrypt server cannot complete a handshake in the implicit shape, so server-side recording by `SNIMatcher` or `ExtendedSSLSession` was not possible. The wire name was recorded by a raw ClientHello parser instead (JDK classes only). Cases g and h (implicit shape with the IPv6 literal) were added.
  - Block C: also ran the seven controls and rejections with the SAN verifier swapped in (C2), because the JDK stub verifier in that mode rejects every host.

### I0c.3 Results

**Block S (why the server fails).** In Conscrypt mode the server task fails in step `handshake` for all three controls, with `InaccessibleObjectException: Unable to make java.net.InetAddress$InetAddressHolder java.net.InetAddress.holder() accessible: module java.base does not "opens java.net" to unnamed module` (wrapped by `SSLHandshakeException`). Origin (run 2 frames): `org.conscrypt.Platform.getOriginalHostNameFromInetAddress` called from `AbstractConscryptSocket.getHostnameOrIP`, `ConscryptEngine.getPeerHost`, `ConscryptEngine.handshake`. The server context provider is Conscrypt, the server socket classes are `org.conscrypt.ConscryptServerSocket` (implicit) or a plain `ServerSocket` wrapped into `org.conscrypt.ConscryptEngineSocket` (STARTTLS); client authentication is neither needed nor wanted; the server enables `[TLSv1, TLSv1.1, TLSv1.2, TLSv1.3]`; after the handshake it writes one byte (0x2A), flushes and closes. On the client the handshake completed (TLSv1.3, `TLS_AES_128_GCM_SHA256`) and the read returned -1. The rejection case (`hs_implicit_other`) fails on the server with the client's `certificate_unknown` alert, which is the expected server-side effect of a client rejection.

**Block V (server variants).** Variant 1 (all supported protocols on the server): no change (controls `byte=-1`, `server=FAILED`). Variant 2: skipped (server context is Conscrypt). Variant 3 (SunJSSE server context): not usable in Conscrypt mode: `ExceptionInInitializerError` in `sun.security.ssl.NamedGroup`, root cause `InvalidParameterException: unhandled field class java.security.spec.ECFieldF2m` thrown by `org.conscrypt.OpenSSLECGroupContext.getInstance` (Conscrypt is the first provider and SunJSSE asks it for EC parameters); the STARTTLS cases there ended in client read timeouts (10 s), cause not determined. Variant 4 (loopback address string as the layered host): `hs_starttls_right` becomes `OK ... byte=42 server=OK`; the implicit controls and `pin_accept` are unaffected (variant 4 applies only to the layered server side), so the condition is not met. **V5 (extra) meets the condition**: all three controls `OK ... byte=42 server=OK`, and every rejection has the same direct cause and marker as in I0b. The plan's four listed variants: none satisfies it; with the extra V5: yes. V5 changes only how the test server accepts; it does not touch the client.

**Block N (what is on the wire, raw ClientHello).** Names carried in the `server_name` extension:

| Case | Client shape and host given to the factory | Wire |
|---|---|---|
| a | default, implicit, `imap.right.test`, connect to the plain loopback address | `localhost` |
| b | default, layered, `imap.right.test` | `imap.right.test` |
| c | default, layered, eight-group IPv6 literal | extension absent |
| d | pinned, implicit, `imap.right.test` | `localhost` |
| e | pinned, layered, `imap.right.test` | `imap.right.test` |
| f | pinned, layered, IPv6 literal | extension absent |
| g | default, implicit, IPv6 literal | `localhost` |
| h | pinned, implicit, IPv6 literal | `localhost` |

The client socket is `org.conscrypt.ConscryptEngineSocket`; its `getSSLParameters().getServerNames()` is `null` before and after the handshake (D5b is a property of the Conscrypt getter, not of the wire). Run 3 (loopback address carrying a host name): in the implicit shape the wire name is the **address's host name**, not the host given to `createSocket`, for the default factory and the pinned factory alike (host `imap.right.test` with address name `other.example.test` gives `other.example.test`; IPv6 literal with address name `imap.right.test` gives `imap.right.test`). In the layered shape the wire name is the host given to `createSocket` (for both factories), and the IPv6 literal gives no extension, regardless of the address name. The pinned factory, which calls no `setSniHost`, produces the same wire names as the default factory in every comparable case.

**Block C (Conscrypt mode OFF, measurement only).** The annotation exists and the class ran. `SSLContext("TLS")` comes from SunJSSE (providers: SUN, SunRsaSign, SunEC, SunJSSE, SunJCE, SunJGSS, SunSASL, XMLDSig, SunPCSC, JdkLDAP, JdkSASL, SunMSCAPI, SunPKCS11, BC). Default supported protocols `[TLSv1.3, TLSv1.2, TLSv1.1, TLSv1, SSLv3, SSLv2Hello]`; default enabled `[TLSv1.3, TLSv1.2]`; the two sets are **not** equal; `TLSv1` and `TLSv1.1` are supported but not enabled by default. The factory's socket and its static `ENABLED_PROTOCOLS` are `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3, SSLv2Hello]`, so the factory enables TLSv1 and TLSv1.1 there (and SSLv2Hello); they differ from the Conscrypt-mode values of I0b (`[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3]`). The default host-name verifier is the JDK stub (`HttpsURLConnection$DefaultHostnameVerifier`), so the unswapped `hs_implicit_right` control is `REJECTED ... marker=true` (host-name check), i.e. this mode would be D3a. With the test SAN verifier installed (C2): `hs_implicit_right` and `hs_starttls_right` and `pin_accept` complete with `byte=42` at TLSv1.3 but the server task reports `ms` about 10 s (`server=OK` for the first, `server=TIMEOUT` for the other two) because the SunJSSE server's `close()` waits for the client's close_notify and my harness awaited the server before closing the client; the rejections have the I0b direct causes and markers (`hs_implicit_other` marker true; `untrusted` and `expired` marker false; `pin_reject` marker true).

### I0c.4 What I could not determine

- Why the SunJSSE-server STARTTLS cases in variant 3 ended in client read timeouts.
- Whether the default factory's `setSniHost` call has any effect on the wire: the pinned factory (no such call) sends the same names, so these runs cannot show a difference.
- Whether the Conscrypt server failure is avoidable on the implicit shape without the V5 server construction (no JVM option may be added by this story).

### I0c.5 Clean-up and state

- Deleted with `rm`: `U063DiagFixturesTest.java`, `U063DiagSupportTest.java`, `U063DiagTest.java`, `U063DiagOffTest.java`. Purged with `:app:compileDebugUnitTestJavaWithJavac` and `:app:transformDebugUnitTestClassesWithAsm`; `app/build/test-results/testDebugUnitTest` removed; 10 javac transaction backups under `app/build/tmp/.../stash-dir/` removed. `find app/build -iname '*U063*'` = 0.
- `git status --porcelain --untracked-files=all -- $PP` empty; `git diff --quiet BASE -- $PROD` exit 0; count C 682, `HEAD` count 682; `evidence/I0c-manifest.txt` written. `<work>/gen` is unchanged since I0b (only `constants.txt`, `summary.txt`, head and tail files and public `*.crt`); no key material was created or written to the repository. New tool: `evidence/tools/diag_extract.py`.
- Hygiene scan (e-mail pattern, user-profile path, application-data folder name, user name, host name) over `evidence/` and this log: 0 hits.

## I0c END

```yaml
handoff:
  increment: "I0c"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "three runs of :app:testDebugUnitTest --tests U063Diag* (diagnostic only; no gate G)", summary: "evidence/I0c-diag.txt", exit: 0, junit: "diagnostic only; classes deleted"}
  count_worktree: 682
  count_head: 682
  product_paths: []
  manifest: "evidence/I0c-manifest.txt"
  escalations: []                 # I0b stops (D5b, D7b, D11, D13(d)) remain open
  decisions_needed:
    - "Rule on D5b, D7b, D11, D13(d) using the I0c facts"
  bug_requests: []
  recovered: false
```

## I1 START 2026-11-17T21:42:05Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I1.1 Start check

- HEAD `395000956bb46cb2acc6d66e62d4b5ef933a2d90` (equals BASE); product-path porcelain empty (equals the I0c manifest); `git diff --quiet BASE -- $PROD` exit 0; `<work>/gen` held `constants.txt` (226 lines), `summary.txt`, head and tail files and the six public `*.crt` files; `find app/build -iname '*U063*'` = 0. `app/src/test/.../ssl/` held only `DefaultTrustedSocketFactorySniTest.java`.

### I1.2 `TlsTestFixtures.java` (397 lines)

- Built from a Javadoc header, the 10 constants taken from `<work>/gen/constants.txt` without change, then the name constants and helpers. Constants block: lines 101 to 326 of the source; compared with `constants.txt` after removing carriage returns: **identical (226 lines)**. `grep -c "public static final String .*_\(CERT\|KEY_PKCS8\)"` gives **10**. (The plan's second self-check, a diff of quoted strings, was replaced by this exact block comparison, which is stricter.) The file has mixed line endings (the generator wrote the constants with the platform line end); git normalises them.
- Contents: `TRUSTED_CA_CERT`, `UNTRUSTED_CA_CERT`, `<ID>_CERT` and `<ID>_KEY_PKCS8` for `RIGHT_HOST`, `OTHER_HOST`, `UNTRUSTED_HOST`, `EXPIRED_HOST`; `RIGHT_HOST_NAME`, `OTHER_HOST_NAME`; `TEST_ONLY_KEY_PASSWORD` (labelled test-only); helpers `certificate`, `privateKey`, `serverContext(leaf, key, issuer)` (in-memory default-type key store, default key manager algorithm, `SSLContext.getInstance("TLS")`), `trustManagerFor(anchors...)` (algorithm `X509`, deviation 8), `chain(...)`. No CA private key exists in the source or anywhere (deleted in I0b).
- Javadoc (AC-22, IC-13, A1.h item 1): test-only and throwaway, generated offline for U-063 and used nowhere else; the exact keytool command lines of `gen-fixtures.sh` with `<gen-dir>` and `<pw>` placeholders and the extraction step; every subject, issuer, SAN, notBefore and notAfter and SHA-256 fingerprint copied from `evidence/I0-fixtures-summary.txt` (validity dates as recorded there); the password literal is declared test-only; an "After the CR-009 move" paragraph (the class references no production class, so nothing to retarget; a failed assertion in a test using it is not edited to pass).

### I1.3 `LoopbackTlsTestServer.java` (220 lines), built to A1.b

- Both modes bind `bindPlainLoopback()` (plain `ServerSocket`, port 0, backlog 1, loopback address object, accept timeout 10 s). One daemon thread named `u063-loopback-test-server`. The task accepts, sets the read timeout, writes `GREETING` + CR LF in STARTTLS only, then in both modes wraps with `serverContext.getSocketFactory().createSocket(plain, InetAddress.getLoopbackAddress().getHostAddress(), plain.getPort(), true)`, `setUseClientMode(false)`, `startHandshake()`, writes `PAYLOAD` (0x2A), flushes, closes. It returns `OK` or `FAILED:<class>`; it never throws.
- `awaitOutcome()` waits at most `TIMEOUT_MILLIS` and **returns the string `TIMEOUT`** if the task has not finished (interpretation of A1.b.3, "reported as TIMEOUT by the test, not hidden": a visible value to assert on, no checked exception); `INTERRUPTED` restores the interrupt flag. `close()` is idempotent and never throws. Client helpers `connect`, `connectPlain`, `bindPlainLoopback`, `readLine` (unbuffered, one byte at a time, at most 256 bytes, fails on end of stream, returns the line without CR LF).
- The A1.b one-sentence reason is in the Javadoc ("The server always wraps an accepted plain socket and passes the loopback address string as the peer host, because Conscrypt's server engine otherwise reflects into `java.net.InetAddress` internals, which the JDK's module system refuses (`java.base` does not open `java.net` to unnamed modules)"). No absolute or machine path. Javadoc also carries the "After the CR-009 move" paragraph, the bounds, and the statement that the client is never configured here.
- Only annotation used: the built-in `@Override` (short form). No `@Test`, no import from `org.conscrypt` or `org.bouncycastle`, no `ConscryptMode` or `LooperMode`.

### I1.4 Helper smoke test (throwaway `U063HelperSmokeTest`)

- Four methods that print and never fail (the plan's three plus the STARTTLS control, so that all three controls of A1.j appear): implicit right-host control, STARTTLS right-host control, STARTTLS other-host rejection, pinned acceptance; trust manager substituted (test CA only) and restored in `finally`, restore printed; no verifier swap (D3b). Two Gradle runs: the first with the three planned methods (`BUILD SUCCESSFUL in 35s`), the second after adding the STARTTLS control (`BUILD SUCCESSFUL in 31s`, exit 0).
- Result (`evidence/I1-smoke.txt`, second run, 7 lines): `smoke_implicit_right=OK protocol=TLSv1.3 byte=42 server=OK`; `smoke_starttls_right=OK protocol=TLSv1.3 byte=42 server=OK`; `smoke_pin_accept=OK protocol=TLSv1.3 byte=42 server=OK`; `smoke_starttls_other=REJECTED ... direct_cause=com.fsck.k9.mail.CertificateChainException ... marker=true server=FAILED:javax.net.ssl.SSLHandshakeException`; all three `*_restore_ok=true`. The three controls are `OK byte=42 server=OK`, as A1.j requires, and the rejection keeps its I0b direct cause and marker.
- Source copied to `evidence/tools/U063HelperSmokeTest.java.txt` (no key material: grep for `BEGIN` or `PRIVATE` = 0), then `U063HelperSmokeTest.java` removed with `rm`.

### I1.5 Generated material removed

`rm -rf <work>/gen` (it held `constants.txt` with the four leaf private keys). `ls <work>` afterwards: `logs`, `snapshots`. No private key exists outside `TlsTestFixtures.java` (IC-13); the CA private keys were removed in I0b.

### I1.6 Gate G

`./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification` (the report task added so the helper classes could be checked in the report); `BUILD SUCCESSFUL in 1m 47s`, exit 0, daemon JVM `25.0.5.0.3`. Summary `evidence/I1-gates.txt`. Totals from this run's XML (`evidence/I1-junit-totals.txt`): `files=73 tests=682 failures=0 errors=0 skipped=2`. Coverage (`evidence/I1-jacoco-packages.txt`): `com.zegoggles.smssync.mail 531/743 71.5%` and `mail.transport 241/287 84.0%`, identical to I0; the two helper classes and the smoke test do not appear in the JaCoCo report (grep for `TlsTestFixtures`, `LoopbackTlsTestServer`, `U063` in `jacocoTestReport.xml` = 0), confirming the name filter excludes them. After the gate: `find app/build -iname '*U063*'` found only the javac backup of the smoke class, which I deleted (final count 0).

### I1.7 End checks

- Product-path porcelain, exactly the two files the plan names: `?? app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java` and `?? app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java`. `git diff --quiet BASE -- $PROD` exit 0. Count C 682, `HEAD` count 682.
- Checks on the two files: IC-6 word grep (sleep, name-lookup calls, host-name literals, host-name socket addresses) 0 hits; imports of `org.conscrypt`, `org.bouncycastle`, `ConscryptMode`, `LooperMode` 0; package-qualified annotations 0; `@Test` text 0; PEM markers 0.
- Manifest `evidence/I1-manifest.txt` (hashes `4e013b43d6384048126e6ff6c580e5da5cd90d4d` for `TlsTestFixtures.java`, `4eafda53d64522a3a1f579d4a2a5656714571af9` for `LoopbackTlsTestServer.java`); snapshot copied to `<work>/snapshots/I1/`.
- Hygiene scan (e-mail pattern, user-profile path, application-data folder name, user name, host name) over the two new sources, `evidence/` including `tools/`, and this log: 0 hits.
- `evidence/tools/diag_extract.py` (I0c) is in the tools folder next to the plan's tools; the I7 scans cover `tools/` for hygiene only, which it passes. A plan check that lists the tools by name would not list it; no check I found would flag it.

### Differences from the plan and A1

- Self-check 2 of I1 replaced by an exact comparison of the constants block (stricter).
- `awaitOutcome()` returns `TIMEOUT` instead of throwing (see I1.3).
- The smoke test has four methods instead of three (adds the STARTTLS right-host control, to show the three controls of A1.j).
- Gate G included `:app:jacocoTestReport` for the report check.
- `<work>/gen` was removed in this turn as the plan's step 5 says; I0b had already removed the CA private keys.

## I1 END

```yaml
handoff:
  increment: "I1"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification", summary: "evidence/I1-gates.txt", exit: 0, junit: "files=73 tests=682 failures=0 errors=0 skipped=2"}
  count_worktree: 682
  count_head: 682
  product_paths:
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java"
  manifest: "evidence/I1-manifest.txt"
  escalations: []
  decisions_needed: []
  bug_requests: []
  recovered: false
```

## I2 START 2026-11-17T21:50:38Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I2.1 Start check

- HEAD equals BASE; `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty; product-path porcelain is exactly the two I1 files (equals `evidence/I1-manifest.txt`); `git hash-object` of `TlsTestFixtures.java` and `LoopbackTlsTestServer.java` equal the I1 manifest hashes; count C 682, `HEAD` count 682; `<work>` holds `logs` and `snapshots`.
- `git hash-object` of `DefaultTrustedSocketFactorySniTest.java` = `7cf73070587db0c2991f5aadcc9ea72d5d60f808` = the blob at BASE (file unmodified; `git ls-files --eol`: index lf, working tree crlf). A restore by the resume rule is therefore not needed.
- Production lines re-read: `isIpOrBlankHostname` at `DefaultTrustedSocketFactory.java:216` (ipv6 branch `:221-223`), guard `:239-243`, catch `:248`; they match the story's citations.

### I2.2 Insertion into `DefaultTrustedSocketFactorySniTest.java` (Edit tool, one edit)

- One insertion, between the end of `setSniViaSSLParameters_blankHostname_doesNotThrowAndSkipsSNI` and the helper section comment: a section comment (what Group A proves, what it cannot prove, the mutation that breaks each of AC-1 and AC-2, RFC 6066 section 3 per A-W6, a pointer to `DefaultTrustedSocketFactoryJdkProviderTest` for AC-3 and AC-4, and the "After the CR-009 move" paragraph) and two tests: `setSniViaSSLParameters_ipv6EightGroupLiteral_doesNotThrowAndSkipsSNI` (AC-1) and `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue` (AC-2).
- No import added (A1 drops the `RuntimeEnvironment` import of the plan's step 1; AC-1 and AC-2 need nothing new). Fully qualified names for `java.lang.reflect.Method` and `org.mockito.ArgumentMatchers.any()` follow the file's own style. The only annotations are `@Test` (existing import).
- Additions-only check: `git diff -U0 | grep -E '^-[^-]' | wc -l` = 0; `git diff --numstat` = 63 added, 0 deleted. Line endings stay CRLF (359 CRLF, 0 bare LF; `git ls-files --eol`: index lf, working tree crlf). Working-tree count C = 684.

### I2.3 Targeted run (TR) of the SNI test

`./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactorySniTest"`: `BUILD SUCCESSFUL in 41s`, exit 0. `evidence/I2-sni-cases.txt`: `files=1 cases=12 pass=12 fail=0 error=0 skip=0` (10 existing + `setSniViaSSLParameters_ipv6EightGroupLiteral_doesNotThrowAndSkipsSNI` + `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue`).

### I2.4 Gate G

`./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification` (report task included as at I1): `BUILD SUCCESSFUL in 3m 23s`, exit 0, daemon JVM `25.0.5.0.3`. Summary `evidence/I2-gates.txt`. Totals from this run's XML (`evidence/I2-junit-totals.txt`): `files=73 tests=684 failures=0 errors=0 skipped=2`. Coverage (`evidence/I2-jacoco-packages.txt`): `com.zegoggles.smssync.mail 531/743 71.5%` and `mail.transport 241/287 84.0%`, identical to I0 and I1. (`com.zegoggles.smssync.preferences` shows 313/412 here against 308/412 at I1: a package this story does not touch and higher, not lower; recorded only because the two files differ.)
- The Bash tool moved this gate to its own background mode after 180 s of foreground run time (a tool behaviour, not a choice of mine: I started it in the foreground and did not poll it). It ran to completion in the one invocation (`exit=0`) and I started no second copy.

### I2.5 End checks

- Product-path porcelain: ` M .../DefaultTrustedSocketFactorySniTest.java`, `?? .../LoopbackTlsTestServer.java`, `?? .../TlsTestFixtures.java` (the I1 lines plus the modified SNI test, as the plan says). `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty.
- Counts: C (working tree) = 684; CLAUDE.md `HEAD` form = 682.
- Additions-only (IC-2, IC-10): 63 lines added, 0 deleted; the ten existing methods are untouched.
- Checks on the diff: no `org.conscrypt` or `org.bouncycastle`; no package-qualified annotation; no import added; the string `@Test` appears in the diff only as the two annotations; IC-6 word grep over `app/src/test/java/com/zegoggles/smssync/mail/ssl` (sleep, name-lookup calls, host-name literals, host-name socket addresses): 0 hits; no throwaway file present; `find app/build -iname '*U063*'` = 0.
- Manifest `evidence/I2-manifest.txt` (SNI test hash `36eedaeef53813bad3519e0c501a4173a77375fa`); snapshot copied to `<work>/snapshots/I2/`.
- Hygiene scan (e-mail pattern, user-profile path, application-data folder name, user name, host name) over `evidence/`, this log and the SNI-test diff: 0 hits.

### Differences from the plan, A1 and the A-W warnings

- Plan I2 step 1 (add the `RuntimeEnvironment` import) is not done: A1 removes it because AC-3 and AC-4 move to the JDK-provider class (I2b). No import is added.
- The section comment also carries the "After the CR-009 move" paragraph (A1.h item 1 asks for it in every new class's Javadoc; the SNI test is an existing class, so this is a small addition beyond the letter of I2, kept in the comment only).
- Gate G included `:app:jacocoTestReport` (as at I1) so the package figures could be read.
- A-W6 applied: the section comment cites RFC 6066 section 3. No other warning applies to I2.

## I2 END

```yaml
handoff:
  increment: "I2"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification", summary: "evidence/I2-gates.txt", exit: 0, junit: "files=73 tests=684 failures=0 errors=0 skipped=2"}
  count_worktree: 684
  count_head: 682
  product_paths:
    - " M app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java"
  manifest: "evidence/I2-manifest.txt"
  escalations: []
  decisions_needed: []
  bug_requests: []
  recovered: false
```

## I2b START 2026-11-17T21:57:44Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I2b.1 Start check

- HEAD equals BASE; `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty; product-path porcelain equals `evidence/I2-manifest.txt` (the modified SNI test and the two untracked helpers); the three hashes equal the I2 manifest; count C 684; no `U063` file under `app/build`; no JDK-provider class yet; `ConscryptMode`/`LooperMode` appear nowhere under `app/src/test`; no `mutation-log.md` yet.
- `DefaultTrustedSocketFactory.java` (DTSF, the one file M-A4 will touch): `git hash-object` = `d543927166b3c5e1da0aaa8ab584072a43bf7521` = `git rev-parse BASE:<path>`.
- Warnings read and applied: A-W1 (Javadoc explains the looper-mode annotation), A-W2 (guard order: A1-G3 first, then the provider check, then the reflective read with `ExceptionInInitializerError` named "A1-G3 (via factory static init)"), A-W3 (record the deprecation note; start-up failure under LEGACY is a stop), A-W4 (recovery adjustments, see I2b.5), A-N3 and A-N4 (tripwire wording; messages print the provider and the first cipher entries).

### I2b.2 Class written (Write tool)

- `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`: `@RunWith(RobolectricTestRunner.class)`, `@ConscryptMode(OFF)`, `@LooperMode(LEGACY)` with the imports `org.robolectric.annotation.ConscryptMode`, `org.robolectric.annotation.LooperMode` and the two static imports of `OFF` and `LEGACY`. Four tests (AC-3, AC-4, AC-16 first and second, JDK forms) and a before-method with guards in the A-W2 order: A1-G3 (one unconnected default socket, catching `NoClassDefFoundError` and `ExceptionInInitializerError`), then the A1-G1 provider check (`SunJSSE`, no registered `Conscrypt`), then the reflective `ENABLED_CIPHERS` read (catching the same two errors and failing as "A1-G3 (via factory static init)") and the `TLS_DHE_` check. Messages print the provider, the registered providers and the first five cipher entries (A-N4). Preconditions A1-P1 and A1-P2 are failing assertions with those names, not assumptions. No handshake, no `RuntimeEnvironment` use beyond the application for the factory.
- Javadoc: why the class exists (deviation 14), the looper-mode explanation (A-W1), JDK provider is neither the runtime nor a device provider, "enabled is not negotiable", RFC 6066 section 3, RFC 8996, per-test mutations, A1-G3 permanent tripwire (A-N3), "After the CR-009 move", and "no handshake".
- Self-checks: working-tree count C = 688; no `startHandshake`, sleep, name-lookup, host-name literal, `org.conscrypt` or `org.bouncycastle`, or key text in the file.

### I2b.3 Pre-check step 1: unmutated targeted run (A1.d step 1)

- `./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest"`: `BUILD SUCCESSFUL in 27s`, exit 0. `evidence/I2b-jdk-cases.txt`: `files=1 cases=4 pass=4 fail=0 error=0 skip=0` (all four PASS). The application started normally under LEGACY looper mode (no A-W3 stop).
- **Guards:** the before-method ran for all four tests and none failed, so A1-G3 (JDK TLS classes usable), A1-G1 provider check (`SunJSSE`, no `Conscrypt` registered) and A1-G1 cipher check (`TLS_DHE_` suite present in the reflected `ENABLED_CIPHERS`) all passed; A1-G2 belongs to the Hardening and Handshake classes (I3, I4). Preconditions A1-P1 and A1-P2 passed (both inside the tests that PASS).
- **javac note (A-W3), expected:** `Note: <the new class> uses or overrides a deprecated API.` and `Note: Recompile with -Xlint:deprecation for details.` (`evidence/I2b-jdk-compile.txt`). It names only this file; the only deprecated API the file uses is the looper-mode value `LEGACY` (I did not rerun with `-Xlint:deprecation` to print the detail). It is a note, not a failure (`-Werror` applies to non-test compilation only). The unrelated Dagger option warning in the same log is old and not from this story.

### I2b.4 Pre-check step 2: M-A4-precheck, write-ahead (2026-11-17T21:59:40Z)

About to apply M-A4 to `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` (hash before `d543927166b3c5e1da0aaa8ab584072a43bf7521` = base blob): delete line 177, `setSniHost(socketFactory, sslSocket, host);`. Entry written to `evidence/mutation-log.md` first (`M-A4-precheck APPLYING`). **If this section has no "restored" line below, the file may be mutated: run nothing else; a recovery turn must compare its hash to the blob above and run `git checkout -- <that file>`.**

### I2b.5 Pre-check step 2: result and restore

- Edit applied with the Edit tool (`git diff --numstat` showed 0 added, 1 deleted). Targeted run of the JDK-provider class only: `BUILD FAILED` (exit 1, expected), `evidence/mutations/M-A4-precheck-cases.txt`: `cases=4 pass=3 fail=1`. FAIL: `createSocket_dnsHost_setsServerNameOnRealSocket` (AC-4, `AssertionErrorWithFacts`). PASS: AC-3 and both AC-16 JDK tests. This is exactly the expected result.
- Restore: `git checkout -- k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` (the only checkout run), exit 0. `git hash-object` = `d543927166b3c5e1da0aaa8ab584072a43bf7521` = base blob; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty; `git diff --quiet BASE -- $PROD` exit 0. Line written to `evidence/mutation-log.md`: `restored: 2026-11-17T22:00:42Z hash-ok porcelain-empty`.
- **Branch applied: "observable works" (A1.d step 3).** Evidence: unmutated, AC-4 reports exactly one `SNIHostName` `imap.right.test` and AC-3 reports no server name for the eight-group literal (both PASS, I2b.3); with M-A4 applied AC-4 fails and AC-3 still passes. The class keeps 4 tests; count C = 688. The fallback (A1.d step 4) is not used.
- Recovery rule for I2b (A-W4), recorded for a lost session: if the session is lost between the write-ahead entry and the `restored` line, nothing else may run until a recovery turn has compared the hash of DTSF with the blob above and, if it differs, run the single-file checkout. RECOVER-I6 is applied to I2b with two changes: step 4 compares the test files against the I2 END manifest plus the single path `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`; step 5 (gate) is skipped, because the resume rule then deletes that class and redoes I2b from the start. Not needed this turn: the session was not lost.

### I2b.6 Gate G (after the restore)

`./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification`, run with an explicit ten-minute tool timeout: `BUILD SUCCESSFUL in 1m 31s`, exit 0, daemon JVM `25.0.5.0.3`, stayed in the foreground (no backgrounding). `git diff --quiet BASE -- $PROD` was exit 0 immediately before the run (no mutated file present). Totals from this run's XML (`evidence/I2b-junit-totals.txt`): `files=74 tests=688 failures=0 errors=0 skipped=2`. Coverage (`evidence/I2b-jacoco-packages.txt`): `com.zegoggles.smssync.mail 531/743 71.5%`, `mail.transport 241/287 84.0%`, unchanged from I0 to I2.

### I2b.7 End checks

- Product-path porcelain: ` M` SNI test, `??` `DefaultTrustedSocketFactoryJdkProviderTest.java`, `??` `LoopbackTlsTestServer.java`, `??` `TlsTestFixtures.java`. `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty. No `U063` file under `app/build`.
- Counts: C = 688; `HEAD` form = 682.
- Checks on the new class: `startHandshake` 0; imports from `org.conscrypt` or `org.bouncycastle` 0; package-qualified annotations 0; IC-6 word grep 0; `ConscryptMode` and `LooperMode` appear only in this class (imports, static imports, and the two annotations); only short-form annotations.
- Manifest `evidence/I2b-manifest.txt` (new class hash `b317ba93074600786018e43fcb4475272da7dcdc`); snapshot in `<work>/snapshots/I2b/`.
- Hygiene scan (e-mail pattern, user-profile path, application-data folder, user name, host name) over `evidence/`, this log and the new class: 0 hits. The javac note in `evidence/I2b-jdk-compile.txt` went through `sanitize.py` (path shown as `<repo>`).

### Differences from the plan, A1 and the warnings

- Nothing beyond the following. The mutation-log entry is also mirrored in this log (I2b.4, I2b.5) so the write-ahead line exists in both places.
- The gate included `:app:jacocoTestReport` (as at I1, I2).
- The javac note is attributed to the looper-mode value by inference (the only deprecated API in the file); `-Xlint:deprecation` was not run.
- Application start-up under LEGACY looper mode worked: no A-W3 stop.

## I2b END

```yaml
handoff:
  increment: "I2b"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification", summary: "evidence/I2b-gates.txt", exit: 0, junit: "files=74 tests=688 failures=0 errors=0 skipped=2"}
  count_worktree: 688
  count_head: 682
  product_paths:
    - " M app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java"
  manifest: "evidence/I2b-manifest.txt"
  escalations: []
  decisions_needed: []
  bug_requests: []
  recovered: false
  notes:
    - "A1.d branch: observable works (AC-3 and AC-4 kept; M-A4-precheck DETECTED: AC-4 failed, AC-3 passed)"
    - "vendored file restored: hash d543927166b3c5e1da0aaa8ab584072a43bf7521 = base blob, porcelain and diff stat empty"

## I3 START 2026-11-17T22:05:48Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I3.1 Start check

- HEAD equals BASE; `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty (both `app/src/main` and `k9mail-vendored`); porcelain equals `evidence/I2b-manifest.txt`; the four test-file hashes equal the I2b manifest; the vendored factory's hash `d543927166b3c5e1da0aaa8ab584072a43bf7521` equals the base blob; count C 688; no `U063` file under `app/build`; no mutation applied in this turn.
- Read for this increment: plan I3 with its (A1) edits, A1.a rows D6', D6b, D7a', D7c, D7d', D7e', D8a, A1.e (A1-G2 belongs here), A1.h (items 1, 3, 4), deviation 7 and its F bounds, story AC-11 to AC-17, SC-3, SC-5, DP-2, the Criterion Status Rule, `evidence/I0-probe.txt` and `evidence/I0c-diag.txt` lines 4-20.

### I3.2 Class written (Write tool)

`app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java`: `@RunWith(RobolectricTestRunner.class)` only (default provider and looper mode); nine tests (AC-11, AC-12, AC-13, AC-14 x2, AC-15, AC-16 x2, AC-17) and a before-method with guard A1-G2. Literal baselines `KNOWN_CIPHERS_BASELINE` (24), `BLOCKED_CIPHERS_BASELINE` (16), `KNOWN_PROTOCOLS_BASELINE`, `BLOCKED_PROTOCOLS_BASELINE`, copied from the production source. Helpers: reflective `constant`, `reorder`; `defaultSocket`, `hardenedSocket`, `providerFixedSuites` (F, live), `closeQuietly`. AC-17 uses `LoopbackTlsTestServer.bindPlainLoopback()` and `connectPlain(...)`, an `accept()` (bounded by the listener's timeout) and no handshake; every socket is closed in `finally`. No import from `org.conscrypt` or `org.bouncycastle`; short-form annotations only; no throwaway files.

### I3.3 Targeted run and guard A1-G2

- `./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryHardeningTest"`: `BUILD SUCCESSFUL in 46s`, exit 0. `evidence/I3-hardening-cases.txt`: `files=1 cases=9 pass=9 fail=0 error=0 skip=0`.
- Guard A1-G2 (before-method of every test in the class): passed for all nine tests, so the factory's static lists in this class's sandbox contain no `SSLv2Hello` and no `TLS_DHE_` suite. In the full gate the JDK-provider class (guards A1-G1, A1-G3) and this class ran in the same JVMs and both passed, so the sandbox separation held in that run too.
- The tests do not print the measured lists (a test that prints would be an unplanned addition), so the lists below are those of the I0b probe, the same provider, platform and unchanged production code (`evidence/I0-probe.txt` lines 26-34); the passing assertions confirm them live (bounds on F, at least 2 known ciphers in the remainder, the exact order checks): provider default enabled ciphers 18 entries; hardened list 18 entries (same set, blocked names none); F = `TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384, TLS_CHACHA20_POLY1305_SHA256` (3 suites); remainder 15 entries, of which 10 are known ciphers in the literal order, then 5 unknown (two ChaCha20 TLS 1.2 suites, two `TLS_RSA_WITH_AES_*_GCM` suites, the renegotiation signalling value); supported protocols `[TLSv1, TLSv1.1, TLSv1.2, TLSv1.3]`; enabled protocols on the implicit and the layered shape `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3]` (equal, probe lines 52-53).

### I3.4 Gate G

`./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification`, explicit ten-minute timeout, foreground: `BUILD SUCCESSFUL in 2m 9s`, exit 0, daemon JVM `25.0.5.0.3`. Totals from this run's XML (`evidence/I3-junit-totals.txt`): `files=75 tests=697 failures=0 errors=0 skipped=2`. Coverage (`evidence/I3-jacoco-packages.txt`): `com.zegoggles.smssync.mail 531/743 71.5%`, `mail.transport 241/287 84.0%`, unchanged. `git diff --quiet BASE -- $PROD` was exit 0 immediately before the gate; no mutation was applied this turn.

### I3.5 End checks

- Product-path porcelain: ` M` SNI test, `??` Hardening test, `??` JDK-provider test, `??` `LoopbackTlsTestServer.java`, `??` `TlsTestFixtures.java`. Production clean against BASE; `app/src/main` and `k9mail-vendored` porcelain empty; no `U063` file under `app/build`.
- Counts: C = 697; `HEAD` form = 682.
- The new class: no `ConscryptMode`, `LooperMode`, `startHandshake`; no `org.conscrypt` or `org.bouncycastle`; IC-6 word grep over the `ssl` test folder: 0 hits; no package-qualified annotation; the only annotations are `@RunWith`, `@Before`, `@Test`.
- Manifest `evidence/I3-manifest.txt` (Hardening class hash `a18c470f4a25946a5421403ad1b2033609073171`); snapshot in `<work>/snapshots/I3/`.
- Hygiene scan (e-mail pattern, user-profile path, application-data folder, user name, host name) over `evidence/`, this log and the new class: 0 hits.

### I3.6 Criterion status (no mutation this turn)

No assertion was false against the unchanged production code, so nothing was stopped, reworded, relaxed or ignored. The second AC-16 test is `NOTED-AC16` under this provider (it passes, cannot tell the factory from the provider, and its Javadoc points to the JDK-provider test that can). Mutation verdicts are I6's. The test carries a comment line "Bug id: pending" for the orchestrator to complete after filing.

### I3.7 Bug draft: legacy protocols (DP-2, Option A, AC-23; A1.h item 4)

**Title:** DefaultTrustedSocketFactory enables TLS 1.0 and TLS 1.1 (and any other protocol the provider supports except SSLv3)

**What the code does.** The factory builds its list of enabled protocols once, at class load, from the protocols the TLS provider supports, not from those it enables by default (`k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` line 107). It removes only SSLv3 (the blocked list, lines 87-89), puts TLS 1.2, 1.1 and 1.0 first in that order, and appends every other supported protocol (lines 116-117, 124-148). `hardenSocket` applies that list to every socket it creates, on both the direct and the layered path (lines 176 and 186-188). So TLS 1.0 and TLS 1.1 are enabled on every socket wherever the provider supports them.

**Evidence from both providers.**
- Under Robolectric's default provider (Conscrypt): the provider supports and enables TLS 1.0, 1.1, 1.2 and 1.3 by default, and the factory's list is `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3]` (`evidence/I0-probe.txt` lines 26-28, 39-44). Because the provider already enables the two old versions, the factory's own part cannot be separated here: building the list from the enabled protocols would give the same result (line 49), so the test `createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem` in `DefaultTrustedSocketFactoryHardeningTest` cannot detect it (status NOTED-AC16).
- Under the JDK's own provider (SunJSSE), selected for one test class: supported protocols `[TLSv1.3, TLSv1.2, TLSv1.1, TLSv1, SSLv3, SSLv2Hello]`, enabled by default only `[TLSv1.3, TLSv1.2]` (`evidence/I0c-diag.txt` lines 7-13). The factory's socket enables `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3, SSLv2Hello]` (line 15), so the factory itself turns TLS 1.0 and 1.1 on (and SSLv2Hello, a legacy hello format, not a protocol version). The test `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff` in `DefaultTrustedSocketFactoryJdkProviderTest` records this and passes. The mutation that would remove the factory's part (M-D11) is run in I6.

**RFC 8996** ("Deprecating TLS 1.0 and TLS 1.1") deprecates both versions and says clients must not negotiate them.

**Caveats.**
- "Enabled" is not "negotiable": the JDK's disabled-algorithms security property lists TLS 1.0 and 1.1 and may still refuse them during a handshake on a desktop JDK. No handshake below TLS 1.3 was run (all test handshakes negotiated TLS 1.3, `evidence/I0-probe.txt` line 12).
- What a device's platform provider enables by default, for each Android API level, was not measured. The factory's behaviour on a device depends on which protocols the device's provider reports as supported.
- The minimum supported API level is moving to 24 (change record CR-011, requirement REQ-015); that does not by itself change which protocols the provider supports.

**Steps to reproduce (test runs).** `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryHardeningTest" --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest"`. Both legacy-protocol tests pass today; they assert the behaviour above, not a desired state.

**Expected** (if the factory followed current guidance): TLS 1.0 and 1.1 not enabled by the factory, whatever the provider supports. **Actual:** they are enabled whenever the provider supports them, including on a provider that leaves them off by default.

**What is not known.** Whether any server this app talks to would negotiate below TLS 1.2 (nothing in this story connects to a real server); whether any user relies on such a server; what the device providers enable by default per API level; whether an on-path attacker could use the enabled versions to force a downgrade (the TLS 1.3 downgrade-protection mechanism applies only when both sides support TLS 1.3).

**Severity suggestion: Medium (security hardening, no exploit path shown).** Reasoning: the factory widens the set of protocol versions the client will accept beyond what a current provider would, on a path that carries mail credentials and content, but acceptance needs a server (or attacker) that offers a legacy version, and this story shows none does. Whether the behaviour is safe, and whether it should change (with the compatibility cost for old servers), is left to the Security Reviewer and the product owner. U-063 does not fix it.

### Differences from the plan and A1

- The measured lists are taken from the I0b probe, not printed by the new tests (see I3.3). Everything the plan asked the tests to assert is asserted live.
- The AC-15 bound "F shares a name with the `ENABLED_CIPHERS` entries the provider accepted" is implemented in the plan's operational form: no name in F contains `_WITH_`, plus F shares no name with the known or blocked baselines.
- Gate G included `:app:jacocoTestReport` (as at I1 to I2b).

## I3 END

```yaml
handoff:
  increment: "I3"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification", summary: "evidence/I3-gates.txt", exit: 0, junit: "files=75 tests=697 failures=0 errors=0 skipped=2"}
  count_worktree: 697
  count_head: 682
  product_paths:
    - " M app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java"
  manifest: "evidence/I3-manifest.txt"
  escalations: []
  decisions_needed: []
  bug_requests:
    - "DP-2 legacy protocols: full draft in implementation-log.md section I3.7"
  recovered: false
```

## I4 START 2026-11-17T22:20:34Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I4.1 Start check

- HEAD equals BASE; `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty; product-path porcelain equals `evidence/I3-manifest.txt` (the modified SNI test and the four untracked files); the five hashes equal the I3 manifest (SNI `36eedaee...`, JDK-provider `b317ba93...`, Hardening `a18c470f...`, server `4eafda53...`, fixtures `4e013b43...`); count C 697, `HEAD` form 682; no `U063` file under `app/build`; `<work>` holds `logs` and `snapshots`.
- Read for this increment: log (all), story notes, AC-5 to AC-10, AC-22, AC-23, IC-1 to IC-13, SC-1 to SC-6, plan A1.a, A1.b, A1.e, A1.h, Count Reference, Criterion Status Rule, Working Without Commits, Environment, Hand-off, I4 (with the A1 edits), Global and Static State, Evidence Layout, Stop Conditions, plan-review A1 section, the helpers, the Hardening and JDK-provider classes, the smoke-test source, the vendored `TrustManagerFactory`, `DefaultTrustedSocketFactory.createSocket`, `CertificateChainException`, and the `ImapConnection` call shapes (`:209` implicit, `:315` STARTTLS).
- Cached per-host trust managers (`TrustManagerFactory.SecureX509TrustManager` map): not restored. Each instance reads the static `defaultTrustManager` on every call (`TrustManagerFactory.java:76`), so a cached instance carries no trust decision of its own (plan, Global and Static State).

### I4.0 Bookkeeping edit (BUG-019)

Comment text only, no code, annotation or assertion change. `DefaultTrustedSocketFactoryHardeningTest.java`: the comment "Bug id: pending" (former lines 372-373) now names BUG-019 (Option A, characterisation). `DefaultTrustedSocketFactoryJdkProviderTest.java`: the Javadoc of `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff` gains "Bug id: BUG-019 (filed; Option A applies ...)". New hashes: Hardening `ed535dc90bfbe1cafb869297f752fccb5478d27b` (was `a18c470f...`), JDK-provider `9fda86c2709b6b4703580895437eb990be71e52b` (was `b317ba93...`). `grep -c pending` on the Hardening class = 0.

### I4.2 Class written (Write tool)

`app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java`: `@RunWith(RobolectricTestRunner.class)` only; annotations used `@RunWith`, `@Before`, `@After`, `@Test` (8). No `ConscryptMode`, `LooperMode`, import from `org.conscrypt` or `org.bouncycastle`.
- Before-method: guard A1-G2 first (as in the Hardening class); then capture of the vendored `defaultTrustManager` field (reflection, reading it initialises the class) and of the default host-name verifier; leak guards with the null checks (trust manager null: "production static init failed; contradicts probe P8 (stop and report)"; verifier null); fail if any accepted issuer subject contains `U-063`; fail if the verifier class name contains `SanDnsHostnameTestVerifier` or starts with this test package (a stricter reading of the plan's verifier guard, since no test verifier class exists under D3b); then the field is set to `TlsTestFixtures.trustManagerFor(TRUSTED_CA_CERT)` (a validating trust manager, never a mock or accept-all). No host-name verifier swap (D3b).
- After-method: client, plain socket and server closed in a try block; in `finally`, the field is set back and the identity of the trust manager and of the default verifier are asserted (`isSameInstanceAs`), both only if the substitution happened. The per-host cached `SecureX509TrustManager` instances are not restored (they read the static field on every call).
- Tests (AC-5 to AC-10): `implicitTls_rightHost_handshakeCompletes`, `implicitTls_otherHost_rejectedByHostnameCheck`, `startTls_rightHost_handshakeCompletes`, `startTls_otherHost_rejectedByHostnameCheck`, `implicitTls_untrustedChain_rejected`, `startTls_untrustedChain_rejected`, `implicitTls_expiredChain_rejected`, `startTls_expiredChain_rejected`. Controls: handshake completes, the client reads `PAYLOAD`, `awaitOutcome()` equals `OK`. Rejections: `startHandshake()` inside try, `fail(...)` if it completes, catch `SSLException` only; the direct cause must be a `CertificateException` and a `CertificateChainException`; the cause chain (at most 20 steps, starting at the `SSLException`) contains "was not verified against the server certificate" if and only if expected (true for the two other-host tests, false for the four others); the server outcome is not asserted in a rejection. The client socket is created by `createSocket` only (implicit: `createSocket(null, host, port, null)` then `connect`; STARTTLS: `connectPlain`, `readLine` greeting, `createSocket(plain, host, port, null)`) and never reconfigured.
- Javadoc: AC-22 and A1.h items 1 to 3: what is proved; what cannot be proved (desktop test provider, handshakes at TLS 1.3 so the TLS 1.2 certificate path is not shown, fresh TLS context per `createSocket` call with the caching caveat, direct calls only with no IMAP class, SC-1, test CA, local key-store fallback not exercised); the mutation per test (M-B1 to M-B5, M-C1, M-C2); RFC 6125 section 6 and RFC 5280; global state; guard A1-G2; time bounds; the "After the CR-009 move" paragraph.

### I4.3 Targeted run and guards

- `./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryHandshakeTest"`: `BUILD SUCCESSFUL in 45s`, exit 0. `evidence/I4-handshake-cases.txt`: `files=1 cases=8 pass=8 fail=0 error=0 skip=0`, first run, no assertion adjusted.
- Guards: the before-method (A1-G2, null checks, trust-manager and verifier leak guards) passed for all eight tests, and so did the identity assertions in the after-method (a failure there would have failed the test). In the gate the other five classes of this story ran in the same JVMs and also passed.
- Durations from the JUnit XML of the targeted run (per test, seconds): `startTls_expiredChain_rejected` 5.89 (the first test of the class; it carries the Robolectric sandbox start-up), then 0.209, 0.17, 0.156, 0.147, 0.161, 0.182, 0.18; class total 7.45. No handshake test needed more than about 0.21 s apart from the sandbox start-up, far below the 10 s bounds. Not newly observed in this turn (the tests print nothing): the negotiated protocol and the exception class chains. They are those of the I0b probe and the I1 smoke test (same provider, unchanged production code): all controls TLSv1.3; the other-host chain `SSLHandshakeException > CertificateChainException > SSLException` (marker present); untrusted and expired `SSLHandshakeException > CertificateChainException > CertificateException > CertPathValidatorException` (marker absent) (`evidence/I0-probe.txt` lines 2-8, 12). The assertions pass live against exactly those shapes.

### I4.4 Gate G

`./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification`, explicit ten-minute tool timeout, foreground (not moved to the background): `BUILD SUCCESSFUL in 1m 26s`, exit 0, daemon JVM `25.0.5.0.3`. `git diff --quiet BASE -- $PROD` was exit 0 immediately before. Totals from this run's XML (`evidence/I4-junit-totals.txt`): `files=76 tests=705 failures=0 errors=0 skipped=2`. Coverage (`evidence/I4-jacoco-packages.txt`): `com.zegoggles.smssync.mail 531/743 71.5%`, `mail.transport 241/287 84.0%`, unchanged from I0; no new class appears in the JaCoCo report (grep for the helper and handshake class names = 0).

### I4.5 End checks

- Product-path porcelain: ` M` SNI test and `??` Handshake, Hardening, JDK-provider, server, fixtures. `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty; no `U063` file under `app/build`.
- Counts: C = 705; `HEAD` form = 682.
- IC-6 word grep over the `ssl` test folder: 0 hits (over the whole `mail` folder the pre-existing `K9MailTransportCreateOpenTest.java` shows two comment lines mentioning the sleep call; not a U-063 file, untouched). No package-qualified annotation; no `org.conscrypt` or `org.bouncycastle`; `@Test` appears 8 times in the new class, all as annotations; no PEM or key text.
- Manifest `evidence/I4-manifest.txt`; snapshot in `<work>/snapshots/I4/`. No mutation was applied this turn.

### Differences from the plan, A1 and the warnings

- The verifier leak guard checks the class name for `SanDnsHostnameTestVerifier` as the plan says and also for the test package prefix (nothing else can be a leftover under D3b).
- The `@After` restore and identity assertions are skipped if the substitution never happened, so a failing before-method guard does not produce a second, misleading failure.
- The negotiated protocol and exception chains are not re-measured by the tests (they print nothing); durations come from the XML. Gate G included `:app:jacocoTestReport` (as at I1 to I3).
- Nothing else differs.

## I4 END

```yaml
handoff:
  increment: "I4"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification", summary: "evidence/I4-gates.txt", exit: 0, junit: "files=76 tests=705 failures=0 errors=0 skipped=2"}
  count_worktree: 705
  count_head: 682
  product_paths:
    - " M app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java"
  manifest: "evidence/I4-manifest.txt"
  escalations: []
  decisions_needed: []
  bug_requests: []
  recovered: false
```

## I5 START 2026-11-17T22:28:33Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I5.1 Start check

- HEAD equals BASE; `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty; porcelain equals `evidence/I4-manifest.txt` (the new I5 class was written after the check and is the only addition); the six hashes equal the I4 manifest; no `U063` file under `app/build`; `PinnedCertificateSocketFactoryTest.java` is unmodified (`git diff --quiet` exit 0, blob `611331b0e650f531f185061b422296894aa50a22`). Coverage before I5 (I4 gate): `com.zegoggles.smssync.mail` 531/743.
- Read for this increment: plan I5 with the A1 edits, A1.a rows D5e, D8a, D11a, A1.e (guard A1-G2 is named for the Hardening and Handshake classes only, so it is not added here), A1.h, AC-18 to AC-22, SC-6, the story notes on SNI, `PinnedCertificateSocketFactory.java` in full, `contract-verification.md` N4, mutations M-E1 to M-E5 and plan-review W3 as folded into the plan (every factory in the class is built with host `imap.right.test`).
- Confirmed from the code: `createSocket` builds its own `PinnedX509TrustManager` (`PinnedCertificateSocketFactory.java:77`), so the vendored `TrustManagerFactory` is not involved and nothing is substituted or restored; the constructor host is unused (`:67-72`).

### I5.2 Class written (Write tool)

`app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java` (package `com.zegoggles.smssync.mail`; reaches the public helpers `LoopbackTlsTestServer` and `TlsTestFixtures` from the `ssl` test package by import). Annotations: `@RunWith`, `@After`, `@Test` (5). No `getTrustManagerForTesting()`, no vendored-`TrustManagerFactory` access, no guard A1-G2 (A1.e places it in the Hardening and Handshake classes only), no `ConscryptMode` or `LooperMode`, nothing from `org.conscrypt` or `org.bouncycastle`. Every factory is built with host `imap.right.test` and the other-host leaf enrolled (W3). The after-method closes the client, the plain sockets, the listener and the server.
- `createSocket_implicit_leavesProviderDefaultsUntouched` (AC-18): `createSocket(null, host, 993, null)`; enabled cipher suites and protocols are equal, element by element and in order, to those of an unconnected socket from a fresh default context.
- `createSocket_layered_leavesProviderDefaultsUntouched` (AC-18): same comparison on a socket layered over a plain loopback connection (`bindPlainLoopback`, `connectPlain`, one bounded `accept`, no handshake).
- `createSocket_implicit_setsNoServerNames` (AC-19): unconnected implicit socket; `getSSLParameters().getServerNames()` is null or empty. Layered shape not asserted. The Javadoc says that under this provider the observable is `null` whatever is set and that the wire capture of I0c found the provider sends a name by itself, equally for the default and the pinned factory; no wire-level test added.
- `handshake_enrolledCertificateForDifferentHostName_isAccepted` (AC-20): IMPLICIT server with the other-host chain, the same leaf enrolled, client asks `imap.right.test`; handshake completes, the byte `0x2A` is read, `awaitOutcome()` is `OK`; no verifier installed.
- `handshake_certificateOtherThanEnrolled_isRejected` (AC-21): server presents the right-host chain; `startHandshake()` in try with `fail(...)`, catch `SSLException` only; the direct cause is a `CertificateException` whose message contains "Pinned certificate mismatch"; the server outcome is not asserted.
- Javadoc (AC-22, A1.h): can prove, cannot prove (desktop provider; TLS 1.3 only; fresh context per call with the caching caveat, both for rejection and acceptance; direct `createSocket` calls, SC-1; AC-19 weakness; no expired enrolled certificate test, SC-6, a follow-up; the pinned path has no cipher or protocol hardening, recorded as present behaviour and not as approved design), M-E1 to M-E5 per test, global-state paragraph, "After the CR-009 move" paragraph, RFC 5280 and RFC 6125 section 6 named as deliberately absent from the pinned path.

### I5.3 Targeted runs

- `./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest"`: `BUILD SUCCESSFUL in 26s`, exit 0, `evidence/I5-pinned-cases.txt`: `files=1 cases=5 pass=5 fail=0 error=0 skip=0`, first run, no assertion adjusted. Per-test times from the XML (seconds): 4.176 (first test, includes sandbox start-up), 0.085, 0.067, 0.062, 0.155.
- The existing test: `--tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryTest"`: `BUILD SUCCESSFUL in 18s`, `evidence/I5-pinned-existing-cases.txt`: `files=1 cases=8 pass=8 fail=0 error=0 skip=0`. `git diff --quiet` on that file: exit 0 (unchanged, not duplicated).
- No case that should be rejected was accepted: AC-21 rejected as required; AC-20 is the designed acceptance.

### I5.4 Gate G

Same command as I4, explicit ten-minute timeout, foreground: `BUILD SUCCESSFUL in 1m 14s`, exit 0, daemon JVM `25.0.5.0.3`; `git diff --quiet BASE -- $PROD` exit 0 immediately before. Totals from this run's XML (`evidence/I5-junit-totals.txt`): `files=77 tests=710 failures=0 errors=0 skipped=2`. Coverage (`evidence/I5-jacoco-packages.txt`): `com.zegoggles.smssync.mail` **531/743 (71.5%) before, 538/743 (72.4%) after**: the covered lines of the package rose by 7 (the pinned factory's `createSocket` and trust-manager code now runs in tests; `PinnedCertificateSocketFactory.java` shows 26 of 29 lines covered in the I5 report; its figure before was not recorded separately and the I4 report was overwritten). `mail.transport` 241/287 (84.0%), unchanged. No floor was lowered and nothing moved down. The new test classes do not appear in the JaCoCo report.

### I5.5 End checks

- Product-path porcelain: the I4 six entries plus `?? .../mail/PinnedCertificateSocketFactoryCreateSocketTest.java` (seven in all: the modified SNI test and six untracked). `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty; no `U063` file under `app/build`.
- Counts: C = 710; `HEAD` form = 682.
- IC-6 word grep over the `ssl` folder and the new class: 0 hits. Forbidden-text grep on the new class (conscrypt, bouncycastle, looper and Conscrypt-mode annotations, the trust-manager test seam, package-qualified annotations, PEM markers): 0. `@Test` text appears 5 times, all annotations.
- Manifest `evidence/I5-manifest.txt` (the reference state for I6 and I7); snapshot in `<work>/snapshots/I5/`. No mutation applied this turn.

### Differences from the plan, A1 and the warnings

- None in substance. The AC-19 Javadoc is longer than the plan's minimum, as the coordinator asked. Gate G included `:app:jacocoTestReport` (as at I1 to I4). The pinned file's own covered-line count before I5 was not captured (only package totals were kept); the package figure is given instead.

## I5 END

```yaml
handoff:
  increment: "I5"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification", summary: "evidence/I5-gates.txt", exit: 0, junit: "files=77 tests=710 failures=0 errors=0 skipped=2"}
  count_worktree: 710
  count_head: 682
  product_paths:
    - " M app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java"
  manifest: "evidence/I5-manifest.txt"
  escalations: []
  decisions_needed: []
  bug_requests: []
  recovered: false
```

## I6a START 2026-11-17T22:34:05Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I6a.1 Start check

- HEAD equals BASE; `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty; product-path porcelain equals `evidence/I5-manifest.txt`; the seven hashes equal the I5 manifest; DTSF hash `d543927166b3c5e1da0aaa8ab584072a43bf7521` equals the base blob; count C 710, `HEAD` form 682; no `U063` file under `app/build`; `<work>` holds `logs`, `snapshots` and a small run script (`tr.sh`, wraps the plan's TR block for several classes).
- I6a mutations (plan I6a table, A1.g): M-A1, M-A1g, M-A1c, M-A2a, M-A2b, M-A3, M-A4 (Sni test + JDK-provider class), M-D1, M-D2, M-D3, M-D4, M-D5, M-D6a, M-D6b, M-D7, M-D15 (Hardening test + JDK-provider class): 16 mutations. Only DTSF is mutated in I6a.

### I6a.2 Baseline at start

`evidence/mutations/I6a-baseline-start-cases.txt`: three classes (Sni, JDK-provider, Hardening) in one invocation, `files=3 cases=25 pass=25 fail=0 error=0 skip=0` (12 + 4 + 9), exit 0.

### I6a.3 Mutations (one line each; details in `evidence/mutation-log.md`)

- M-A1: DETECTED (AC-1 and AC-3 failed, as expected; existing IP/blank tests also failed, not counted). Restored 2026-11-17T22:36:10Z: hash-ok, porcelain empty, diff stat empty.
- M-A1g: BY-DESIGN (guard-only removal: AC-1 and AC-3 pass by design; only the existing IPv4 test failed). Restored 2026-11-17T22:37:12Z: hash-ok, porcelain empty, diff stat empty.
- M-A1c: BY-DESIGN (catch-layer-only removal: nothing failed, AC-1 and AC-3 pass by design (the guard covers it)). Restored 2026-11-17T22:38:06Z: hash-ok, porcelain empty, diff stat empty.
- M-A2a: DETECTED (AC-2 failed, as expected; AC-1 and AC-3 still passed (catch layer)). Restored 2026-11-17T22:38:59Z: hash-ok, porcelain empty, diff stat empty.
- M-A2b: DETECTED (AC-2 failed, as expected (eight-group literal contains no double colon)). Restored 2026-11-17T22:39:51Z: hash-ok, porcelain empty, diff stat empty.
- M-A3: DETECTED (AC-3 and AC-4 failed (JDK-provider class), as expected). Restored 2026-11-17T22:40:39Z: hash-ok, porcelain empty, diff stat empty.
- M-A4: DETECTED (AC-4 failed (JDK-provider class), AC-3 passed, as expected). Restored 2026-11-17T22:41:25Z: hash-ok, porcelain empty, diff stat empty.
- M-D1: DETECTED (AC-11 and AC-14 failed, as expected). Restored 2026-11-17T22:42:10Z: hash-ok, porcelain empty, diff stat empty.
- M-D2: DETECTED (AC-11, AC-14 and the AC-16 first JDK test failed, as expected). Restored 2026-11-17T22:42:57Z: hash-ok, porcelain empty, diff stat empty.
- M-D3: DETECTED (AC-11 failed as expected; the AC-16 first JDK test also failed (SSLv3 no longer removed under SunJSSE)). Restored 2026-11-17T22:43:47Z: hash-ok, porcelain empty, diff stat empty.
- M-D4: DETECTED (AC-12, AC-14 failed as expected; AC-15 also failed). Restored 2026-11-17T22:44:29Z: hash-ok, porcelain empty, diff stat empty.
- M-D5: DETECTED (AC-12, AC-14, AC-16 (Hardening) and the AC-16 first JDK test failed, as expected). Restored 2026-11-17T22:45:12Z: hash-ok, porcelain empty, diff stat empty.
- M-D6a: DETECTED (AC-12 failed as expected; AC-15 and both AC-16 first tests also failed). Restored 2026-11-17T22:45:55Z: hash-ok, porcelain empty, diff stat empty.
- M-D6b: DETECTED (AC-12, AC-15, AC-16 failed as expected; AC-11 and AC-13 also failed as a consequence). Restored 2026-11-17T22:46:38Z: hash-ok, porcelain empty, diff stat empty.
- M-D7: DETECTED (AC-13 failed as expected (null blocklist)). Restored 2026-11-17T22:47:22Z: hash-ok, porcelain empty, diff stat empty.
- M-D15: DETECTED (AC-13 failed as expected (empty input); nothing else moved). Restored 2026-11-17T22:48:05Z: hash-ok, porcelain empty, diff stat empty.

All 16 mutations of I6a are done. None is in place (see I6a.5).

### I6a.4 Baseline at end and Gate G

- End baseline (same three classes, one invocation), `evidence/mutations/I6a-baseline-end-cases.txt`: `files=3 cases=25 pass=25 fail=0 error=0 skip=0`. **The first attempt failed for an environmental reason and was repeated once**: the Sni class reported a class-level error (`classMethod`, `IllegalStateException: Couldn't create lock file ...` for Robolectric's download lock file in the user profile, raised in the Robolectric sandbox set-up before any test of the class ran; the other two classes passed, 13 of 13). The attempt-1 summary is kept in `evidence/mutations/I6a-baseline-end-attempt1-cases.txt`. Production was already restored and verified clean (hash, porcelain, diff stat) when it happened, and no mutation was applied; the same command passed on the second run. Cause not investigated (a lock file in the user profile is shared with whatever else runs Robolectric on this machine). This is one Gradle run more than the plan's count.
- Gate G (same command as I5: `./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification`, explicit ten-minute timeout, foreground), run on the clean tree after `git diff --quiet BASE -- $PROD` exit 0: `BUILD SUCCESSFUL in 1m 33s`, exit 0, daemon JVM `25.0.5.0.3`. Totals from this run's XML (`evidence/I6a-junit-totals.txt`): `files=77 tests=710 failures=0 errors=0 skipped=2`. Coverage (`evidence/I6a-jacoco-packages.txt`): `com.zegoggles.smssync.mail 538/743 72.4%`, `mail.transport 241/287 84.0%`, equal to I5.

### I6a.5 End checks

- Production clean: `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --stat BASE -- app/src/main k9mail-vendored` empty. Only DTSF was mutated this turn. Hashes now versus the base blobs: DTSF `d543927166b3c5e1da0aaa8ab584072a43bf7521` = `d543927166b3c5e1da0aaa8ab584072a43bf7521`; (not mutated, shown for completeness) `TrustManagerFactory.java` `3b448058a393c5455ba9524f4b1a5fe9e2926c88` = `3b448058a393c5455ba9524f4b1a5fe9e2926c88`; `PinnedCertificateSocketFactory.java` `8a0fbb64fb171624591fbebafb818555f28efd58` = `8a0fbb64fb171624591fbebafb818555f28efd58`.
- Product-path porcelain equals `evidence/I5-manifest.txt` (the modified SNI test and six untracked test files); the seven test-file hashes equal the I5 manifest (no test file was changed this turn). `evidence/I6a-manifest.txt` written.
- Counts: C = 710, `HEAD` form = 682. `find app/build -iname '*U063*'` = 0. No U-063 throwaway file under `app/src/test`. No commit, add, stash, reset, restore or clean was run; the only checkout run was `git checkout -- <DTSF>` (16 times, one per mutation, through the work-folder script `restore.sh` that names only that path).
- Hygiene scan (e-mail pattern, user-profile path, application-data folder, user name, host name) over `evidence/mutation-log.md`, `evidence/mutations/`, the `I6a-*` evidence files and this log: 0 hits.
- Gradle runs this turn: 1 start baseline + 16 mutation runs + 2 end baseline (one repeat) + 1 gate = 20 (plan: 19).

### Differences from the plan, A1 and the warnings

- One extra Gradle run (the repeated end baseline, environmental, see I6a.4).
- M-D3 also made the AC-16 first JDK test fail (not in the plan's expected list; the deleted block removes SSLv3 as well, same mechanism as M-D2). M-A1 and M-A1g outcomes, and the extra failures of M-D4, M-D6a, M-D6b, are listed in the mutation log; all were within what the plan's entries allowed or explained by the edit, none contradicted a rule.
- No mutation was NOT DETECTED, invalid or left in place. No test file changed.

## I6a END

```yaml
handoff:
  increment: "I6a"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification", summary: "evidence/I6a-gates.txt", exit: 0, junit: "files=77 tests=710 failures=0 errors=0 skipped=2"}
  count_worktree: 710
  count_head: 682
  product_paths:
    - " M app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java"
  manifest: "evidence/I6a-manifest.txt"
  escalations: []
  decisions_needed: []
  bug_requests: []
  recovered: false
  notes:
    - "mutations done (16, all DTSF): M-A1 DETECTED, M-A1g BY-DESIGN, M-A1c BY-DESIGN, M-A2a DETECTED, M-A2b DETECTED, M-A3 DETECTED, M-A4 DETECTED, M-D1 DETECTED, M-D2 DETECTED, M-D3 DETECTED, M-D4 DETECTED, M-D5 DETECTED, M-D6a DETECTED, M-D6b DETECTED, M-D7 DETECTED, M-D15 DETECTED"
    - "no mutation in place: DTSF hash d543927166b3c5e1da0aaa8ab584072a43bf7521 = base blob; porcelain and diff stat for app/src/main and k9mail-vendored empty"
    - "next: I6b (M-D8 to M-D14 with the Hardening and JDK-provider classes; M-B1 to M-B5, M-C1, M-C2 with the Handshake class); start with the sub-turn baseline"
    - "environmental flake once: Robolectric download lock file error in the end baseline attempt 1; repeated once, passed"
```

## I6b START 2026-11-17T22:53:43Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I6b.1 Start check

- HEAD equals BASE; `git diff --quiet BASE -- $PROD` exit 0; `git status --porcelain -- app/src/main k9mail-vendored` empty; product-path porcelain equals `evidence/I6a-manifest.txt` (= the I5 manifest); the seven test-file hashes equal the I5 manifest; the three mutable production files are at their base blobs (DTSF `d543927166b3c5e1da0aaa8ab584072a43bf7521`, TMF `3b448058a393c5455ba9524f4b1a5fe9e2926c88`, PCSF `8a0fbb64fb171624591fbebafb818555f28efd58`); count C 710, `HEAD` form 682; no `U063` file under `app/build`; the last mutation-log entry (M-D15) has its `restored` line.
- I6b mutations: M-D8, M-D9, M-D10, M-D11, M-D12, M-D13, M-D14 (DTSF; Hardening + JDK-provider classes), M-B1, M-B3, M-B4, M-C1, M-C2 (TMF; Handshake class), M-B2, M-B5 (DTSF; Handshake class): 14 mutations. Plan order is kept (M-D8..M-D14, then M-B1..M-B5, M-C1, M-C2).
- Work-folder helpers changed for this turn: `pre.sh`, `restore.sh` and `post.sh` now take one key from the fixed list DTSF, TMF, PCSF and refuse anything else; `restore.sh` runs `git checkout -- <that one file>` and then verifies all three files against their base blobs, the porcelain and the diff stat; `pre.sh` refuses to write the entry unless the tree is clean and the target hash equals the base blob. Both mutated modules in this turn are in `k9mail-vendored` (no `app/src/main` file is mutated; see I6b.5 on stale classes).

### I6b.2 Baseline at start

`evidence/mutations/I6b-baseline-start-cases.txt`: Handshake, Hardening and JDK-provider classes in one invocation, `files=3 cases=21 pass=21 fail=0 error=0 skip=0` (8 + 9 + 4), exit 0.

### I6b.3 Mutations (one line each; details in `evidence/mutation-log.md`)

- M-D8: DETECTED (AC-15, AC-16 (Hardening and JDK first) failed; AC-16 second JDK also failed). Restored 2026-11-17T22:55:02Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-D9: DETECTED (AC-15 failed as expected). Restored 2026-11-17T22:55:39Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-D10: DETECTED (AC-17 failed as expected). Restored 2026-11-17T22:56:19Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-D11: DETECTED (NOTED-AC16 for the Hardening second AC-16 test) (both JDK AC-16 tests failed; Hardening AC-16 second test passed (NOTED-AC16)). Restored 2026-11-17T22:57:00Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-D12: DETECTED (AC-14 and the AC-16 second JDK test failed as expected; extra: AC-12, AC-16 (Hardening first and second, JDK first) also failed; no NOTED-AC16). Restored 2026-11-17T22:57:47Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-D13: DETECTED (AC-16 first (Hardening and JDK) failed; AC-16 second JDK also failed). Restored 2026-11-17T22:58:30Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-D14: DETECTED (AC-14 failed as expected; AC-15 also failed). Restored 2026-11-17T22:59:11Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-B1: COMPILE_FAIL (invalid, not counted) (did not compile (SSLException catch no longer reachable); needs a ruling on a replacement edit). Restored 2026-11-17T22:59:49Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-B3: DETECTED (AC-5 and AC-7 failed as expected). Restored 2026-11-17T23:00:32Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-B4: DETECTED (AC-5 and AC-7 failed as expected; AC-6 and AC-8 also failed (marker missing)). Restored 2026-11-17T23:01:20Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-C1: COMPILE_FAIL (invalid, not counted) (did not compile (CertificateException catch no longer reachable); needs a ruling on a replacement edit). Restored 2026-11-17T23:01:55Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-C2: DETECTED (six rejection tests failed (they saw the handshake complete); controls passed). Restored 2026-11-17T23:02:53Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-B2: DETECTED (six rejection tests failed (accept-all trust manager); controls passed). Restored 2026-11-17T23:03:49Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-B5: DETECTED (four STARTTLS tests failed with ClassCastException; implicit tests passed). Restored 2026-11-17T23:04:41Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.

All 14 mutations of I6b were run (12 DETECTED, 2 COMPILE_FAIL). None is in place (see I6b.5). Actual order run: M-D8 to M-D14, M-B1, M-B3, M-B4, M-C1, M-C2, M-B2, M-B5 (the TMF mutations were grouped; the plan's order is not a rule).

### I6b.4 NOTED-AC16 and the compile failures

- **NOTED-AC16 applies to M-D11 only.** The default-provider second AC-16 test (`createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem`, Hardening) passed under M-D11, as D7c predicted (Conscrypt supports and enables TLS 1.0 and 1.1 itself). M-D11 was detected by both JDK-provider AC-16 tests, including the first one, as rule 3 requires. For M-D12 the same test did fail, so NOTED-AC16 does not apply there (M-D12 was detected by AC-14 as rule 3 requires).
- **M-B1 and M-C1: COMPILE_FAIL (invalid, not counted).** Deleting `verifyHostname(mHost, chain);` (M-B1) leaves the `catch (SSLException e)` with nothing in the try body that throws it; deleting `defaultTrustManager.checkServerTrusted(chain, authType);` (M-C1) leaves the `catch (CertificateException e)` unreachable in the same way. Both stop `k9mail-vendored:compileDebugJavaWithJavac` (`TrustManagerFactory.java:82` and `:78`: "exception ... is never thrown in body of corresponding try statement"), so no test ran. Both were restored and verified. The plan table gives no replacement edit and none was invented. Both checks are exercised by other mutations in this turn: host-name checking by M-B3 (right host rejected) and M-B4 (empty chain), certificate validation by M-C2 and M-B2 (all six rejection tests fail when validation is bypassed). The two stand open for a ruling: the orchestrator may supply a compiling replacement edit.

### I6b.5 Baseline at end, Gate G, stale classes, end checks

- End baseline (Handshake, Hardening, JDK-provider in one invocation), `evidence/mutations/I6b-baseline-end-cases.txt`: `files=3 cases=21 pass=21 fail=0 error=0 skip=0`. The Robolectric lock-file error did not recur.
- Gate G (same command as I6a; explicit ten-minute timeout, foreground), run after `git diff --quiet BASE -- $PROD` exit 0: `BUILD SUCCESSFUL in 1m 35s`, exit 0, daemon JVM `25.0.5.0.3`. Totals from this run's XML (`evidence/I6b-junit-totals.txt`): `files=77 tests=710 failures=0 errors=0 skipped=2`. Coverage (`evidence/I6b-jacoco-packages.txt`): `mail` 538/743 (72.4%), `mail.transport` 241/287 (84.0%), equal to I5 and I6a.
- **No mutated class remains.** No `app/src/main` file was mutated (both mutated files are in `k9mail-vendored`). Each mutation changed the module's inputs, so Gradle recompiled `k9mail-vendored` on the next run (the compile failures were reported by that task), and the final unmutated gate and end baseline recompiled it back. Check after the gate: a byte search for the string `mutant` (present in the M-B3 and the I6a M-A3 edits) over every file under `k9mail-vendored/build` and `app/build` found no class, jar or dex file; no `DefaultTrustedSocketFactory$*.class` exists (the accept-all anonymous class of M-B2 would be one); `find app/build -iname '*U063*'` = 0. The release class directory of the vendored module dates from the I0a assembleRelease on the clean tree and contains no mutation string.
- Production clean: `git diff --quiet BASE -- $PROD` exit 0; porcelain and diff stat for `app/src/main` and `k9mail-vendored` empty. Hashes now versus base blobs: DTSF `d543927166b3c5e1da0aaa8ab584072a43bf7521` = `d543927166b3c5e1da0aaa8ab584072a43bf7521`; TMF `3b448058a393c5455ba9524f4b1a5fe9e2926c88` = `3b448058a393c5455ba9524f4b1a5fe9e2926c88`; PCSF `8a0fbb64fb171624591fbebafb818555f28efd58` = `8a0fbb64fb171624591fbebafb818555f28efd58`.
- Product-path porcelain equals the I5 manifest; the seven test-file hashes equal it (no test file changed). `evidence/I6b-manifest.txt` written. Counts: C 710, `HEAD` form 682. Mutation log: the pre-check plus 16 (I6a) plus 14 (I6b) = 31 entries, each with its `restored` line.
- Only `git checkout -- <one file>` was run, through `restore.sh` (key DTSF or TMF, 14 times in total, once per mutation). No commit, add, stash, reset, restore or clean.
- Gradle runs this turn: 1 start baseline + 14 mutation runs (two of them stopped at compilation) + 1 end baseline + 1 gate = 17 (plan: 17). No run hung or moved to the background.

### Differences from the plan, A1 and the warnings

- M-B1 and M-C1 did not compile (see I6b.4): not counted, no replacement edit, ruling needed. Everything else ran as the table says.
- Extra detections beyond the table, all explained in the mutation log: M-D8 (also the AC-16 second JDK test), M-D12 (AC-12 and both Hardening AC-16 tests), M-D13 (also the AC-16 second JDK test), M-D14 (also AC-15), M-B4 (also AC-6 and AC-8, because the host-name marker is missing from the exception).
- Mutation order differs from the table order (TMF mutations grouped); helper scripts parameterised (key from a fixed list of three files).

## I6b END

```yaml
handoff:
  increment: "I6b"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification", summary: "evidence/I6b-gates.txt", exit: 0, junit: "files=77 tests=710 failures=0 errors=0 skipped=2"}
  count_worktree: 710
  count_head: 682
  product_paths: [the I5 manifest set: 1 modified SNI test, 6 untracked test files]
  manifest: "evidence/I6b-manifest.txt"
  escalations: []
  decisions_needed:
    - "M-B1 and M-C1 are COMPILE_FAIL (invalid, not counted): deleting the only statement that throws SSLException (M-B1) or CertificateException (M-C1) in the try body makes the catch clause a compile error. Supply a compiling replacement edit, or rule that M-B3/M-B4 (host-name check) and M-C2/M-B2 (chain validation) stand in for them. No test was run for either."
  bug_requests: []
  recovered: false
  notes:
    - "14 mutations run: M-D8, M-D9, M-D10, M-D11, M-D12, M-D13, M-D14, M-B2, M-B3, M-B4, M-B5, M-C2 DETECTED; M-B1 and M-C1 COMPILE_FAIL"
    - "NOTED-AC16 applies to M-D11 only (Hardening second AC-16 test passed; both JDK AC-16 tests detected it)"
    - "no mutation in place: DTSF, TMF, PCSF at base blobs; porcelain and diff stat empty"
    - "next: I6c (PCSF mutations M-E1..M-E5; app/src/main is mutated there, so purge stale app classes) unless the orchestrator rules on M-B1/M-C1 first"
```

## I6c START 2026-11-17T23:09:34Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I6c.1 Start check and ruling in force

- HEAD equals BASE; `git diff --quiet BASE -- $PROD` exit 0; porcelain for `app/src/main k9mail-vendored` empty; product-path porcelain equals `evidence/I6b-manifest.txt` (= the I5 manifest); the seven test-file hashes equal the I5 manifest; DTSF, TMF and PCSF at their base blobs (`d543927166b3c5e1da0aaa8ab584072a43bf7521`, `3b448058a393c5455ba9524f4b1a5fe9e2926c88`, `8a0fbb64fb171624591fbebafb818555f28efd58`); count C 710, `HEAD` form 682; no `U063` file under `app/build`; no throwaway suite file; the last mutation-log entry (M-B5) has its `restored` line.
- **Orchestrator ruling of 2026-11-17 on M-B1 and M-C1** (both stay recorded as COMPILE_FAIL, invalid, not counted): replacements **M-B1r** (TMF, `verifyHostname(mHost, chain);` becomes `if (false) verifyHostname(mHost, chain);`) and **M-C1r** (TMF, `defaultTrustManager.checkServerTrusted(chain, authType);` becomes `if (false) defaultTrustManager.checkServerTrusted(chain, authType);`), both run on `DefaultTrustedSocketFactoryHandshakeTest`. Expected sets: M-B1r fails exactly AC-6 and AC-8 (the two other-host tests); M-C1r fails exactly the four untrusted and expired tests (AC-9, AC-10). Any difference in either set is a stop after restore (Criterion Status Rule).
- I6c mutations: M-B1r, M-C1r (TMF, Handshake class), then M-E1 to M-E5 (PCSF, `com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest`). Helper change: `tr.sh` accepts a fully qualified class name as well as a simple name in the `ssl` package (needed for the `mail` package class).

### I6c.2 Baseline at start

`evidence/mutations/I6c-baseline-start-cases.txt`: Handshake class and `PinnedCertificateSocketFactoryCreateSocketTest` in one invocation, `files=2 cases=13 pass=13 fail=0 error=0 skip=0` (8 + 5), exit 0. (The first launch of the run script failed to parse after a bad edit of the script itself and started no Gradle run; the script was rewritten and the baseline run once. No effect on the tree.)

### I6c.3 Mutations (one line each; details in `evidence/mutation-log.md`)
- M-B1r: DETECTED (replacement for COMPILE_FAIL M-B1) (replacement for M-B1: exactly AC-6 and AC-8 failed). Restored 2026-11-17T23:11:19Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-C1r: DETECTED (replacement for COMPILE_FAIL M-C1) (replacement for M-C1: exactly the four untrusted and expired tests (AC-9, AC-10) failed). Restored 2026-11-17T23:12:06Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-E1: DETECTED (AC-18 (both tests) failed as expected). Restored 2026-11-17T23:13:03Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-E2: NOT DETECTED (AC-19 UNMET: the SNI observable is null under Conscrypt even when M-E2 sets a name; stop and report). Restored 2026-11-17T23:14:06Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.

**STOP: M-E2 is NOT DETECTED.** M-E3, M-E4, M-E5 and the I6 close-out were not run (no further mutation, no gate), as the Criterion Status Rule (rule 2) and the turn's instruction require.

### I6c.4 The stop

- M-E2 (PCSF; adds a real `SNIHostName` to the pinned factory's socket) was applied, the class `PinnedCertificateSocketFactoryCreateSocketTest` ran (`BUILD SUCCESSFUL`, 5 of 5 passed), and **AC-19 (`createSocket_implicit_setsNoServerNames`) did not fail**. By rule 2, AC-19 is **unmet**: its test cannot detect "createSocket sets a server name" under this provider. The reason is on record from the start: the Conscrypt client socket's `getSSLParameters().getServerNames()` returns `null` whatever was set (probe `sni_obs_pinned_implicit` and I0c: null before and after the handshake), and the test's Javadoc says the observable is vacuous here. The NOTED-AC16 exception covers only the second AC-16 test, so it does not apply. It was not relabelled and no test was changed.
- File restored at once and verified: PCSF hash `8a0fbb64fb171624591fbebafb818555f28efd58` = base blob; DTSF `d543927166b3c5e1da0aaa8ab584072a43bf7521` and TMF `3b448058a393c5455ba9524f4b1a5fe9e2926c88` at base; porcelain and diff stat empty.
- Options for the orchestrator (not mine to choose): (a) accept AC-19 as unmet and record why, (b) a wire-level or other observable (I0c found the pinned factory sends the same names as the default one, and a name-bearing observable would be a new test), (c) a ruling that M-E2 does not apply under this provider. The detection of AC-19 would need a test change, which this turn may not make.

### I6c.5 State at the stop

- **Stale mutated classes exist under `app/build`.** No Gradle run followed the M-E2 restore, so the compiled classes of `PinnedCertificateSocketFactory` in the debug javac output and the debug ASM-transform directory still contain the M-E2 code (byte search: `SNIHostName` present; the release directories do not contain it). The SOURCE is restored and verified; Gradle recompiles from it at the next invocation (the debug compile input changed back). The next turn should start by running `:app:compileDebugJavaWithJavac` and `:app:transformDebugClassesWithAsm` and repeating this byte search (expect `SNIHostName` count 0 in all four class copies) before any test run. Nothing was run on the stale classes.
- The M-E1 mutation (PCSF) compiled and ran earlier this turn; its classes were overwritten by M-E2's compile.
- Product-path porcelain equals the I5 manifest; the seven test-file hashes equal it; counts C 710, `HEAD` form 682; `find app/build -iname '*U063*'` = 0. `evidence/I6c-manifest.txt` written.
- Gradle runs this turn: 1 start baseline + M-B1r + M-C1r + M-E1 + M-E2 = 5 (an earlier launch of the run script failed to parse and started no Gradle run). No end baseline and no gate were run: the plan's close-out was not reached.

### Differences from the plan, A1 and the warnings

- The turn stopped at M-E2 (NOT DETECTED), so M-E3 to M-E5 and the I6 close-out (summary table, verdict totals, `I6-clean.txt`, gate) are outstanding.
- M-B1r and M-C1r ran under the orchestrator ruling and matched their expected sets exactly.

## I6c END

```yaml
handoff:
  increment: "I6c"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "not run (turn stopped at a NOT DETECTED mutation)", summary: "n/a", exit: null, junit: "n/a (last gate: I6b, files=77 tests=710 failures=0 errors=0 skipped=2)"}
  count_worktree: 710
  count_head: 682
  product_paths: [the I5 manifest set: 1 modified SNI test, 6 untracked test files]
  manifest: "evidence/I6c-manifest.txt"
  escalations:
    - "AC-19 UNMET: M-E2 NOT DETECTED (createSocket_implicit_setsNoServerNames passes with a real SNIHostName set; the Conscrypt getServerNames observable is null whatever is set). Criterion Status Rule, rule 2."
  decisions_needed:
    - "Rule on AC-19 / M-E2: accept as unmet with the recorded reason, or supply a different observable (needs a test change, which the Developer may not make unprompted), or rule M-E2 not applicable under this provider."
    - "Whether to continue with M-E3, M-E4, M-E5 and the I6 close-out (the stop was after M-E2 only; they are independent of AC-19)."
  bug_requests: []
  recovered: false
  notes:
    - "M-B1r DETECTED (exactly AC-6 and AC-8), M-C1r DETECTED (exactly the four untrusted/expired tests), M-E1 DETECTED (AC-18 both), M-E2 NOT DETECTED"
    - "no mutation in place: DTSF, TMF, PCSF at base blobs; porcelain and diff stat empty"
    - "STALE mutated debug classes of PinnedCertificateSocketFactory (M-E2) under app/build; source is clean; recompile (compileDebugJavaWithJavac, transformDebugClassesWithAsm) before any test run"
```

## I6c INTERRUPTED 2026-11-17T23:14:50Z

Reason: orchestrator stop for a machine shutdown, at the M-E2 stop-and-report point. No Gradle run was in flight and no mutation is applied.

- Completed this turn: M-B1r DETECTED (exactly AC-6 and AC-8), M-C1r DETECTED (exactly the four untrusted and expired tests), M-E1 DETECTED (AC-18, both tests), M-E2 NOT DETECTED (AC-19 unmet).
- NOT yet run: M-E3, M-E4, M-E5.
- I6 close-out NOT done (summary table, verdict totals, `I6-clean.txt`, NOTED-AC16 and BY-DESIGN list, Gradle run count for I6). End-of-turn gate NOT run, no end baseline. (`evidence/I6c-manifest.txt` and the hygiene scan were done before this stop.)
- Production state re-confirmed: DTSF, TMF and PCSF at their base blobs (`d543927166b3c5e1da0aaa8ab584072a43bf7521`, `3b448058a393c5455ba9524f4b1a5fe9e2926c88`, `8a0fbb64fb171624591fbebafb818555f28efd58`); `git diff --quiet BASE -- $PROD` exit 0; porcelain for `app/src/main k9mail-vendored` empty.
- Resume: run RECOVER-I6 (expected outcome (a'), nothing to recover), then recompile (`:app:compileDebugJavaWithJavac`, `:app:transformDebugClassesWithAsm`) because stale M-E2 debug classes sit under `app/build` (source is clean), then continue with the orchestrator's ruling on M-E2, M-E3 to M-E5 and the close-out.

```yaml
handoff:
  increment: "I6c"
  status: "INTERRUPTED"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: "not run"
  count_worktree: 710
  count_head: 682
  escalations: ["AC-19 UNMET: M-E2 NOT DETECTED (Criterion Status Rule, rule 2)"]
  decisions_needed: ["Rule on AC-19 / M-E2: accept as unmet with the recorded reason (Conscrypt getServerNames is null whatever is set), supply a different observable (needs a test change), or rule M-E2 not applicable under this provider", "Continue with M-E3, M-E4, M-E5 and the I6 close-out"]
  recovered: false
  notes: ["no mutation in place", "stale M-E2 debug classes under app/build; recompile before any test run"]
```

## I6c-resume START 2026-11-20T23:02Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

recovered: true (RECOVER-I6 with the A-W4 adjustments, outcome (a'): nothing to recover). Work folder is `<work>` (orchestrator ruling; its helper scripts `pre.sh`, `restore.sh`, `post.sh`, `tr.sh`, `gate.sh`, `gate6b.sh` were read before use).

### I6c-resume.1 Recovery check (no Gradle run before it ended)

- HEAD equals the story's start commit (nothing committed since).
- DTSF `d543927166b3c5e1da0aaa8ab584072a43bf7521`, TMF `3b448058a393c5455ba9524f4b1a5fe9e2926c88`, PCSF `8a0fbb64fb171624591fbebafb818555f28efd58`: each equals its base blob. `git status --porcelain -- app/src/main k9mail-vendored` empty; `git diff --quiet BASE -- app/src/main k9mail-vendored` exit 0.
- Mutation log: 35 `APPLYING` entries and 35 `restored` lines (the I2b pre-check plus 34 mutations); the last entry (M-E2) has its `restored` line. Outcome (a').
- Product-path porcelain equals `evidence/I5-manifest.txt` (the SNI test modified, six untracked test files); the seven hashes equal the manifest; counts C 710, `HEAD` form 682. No `U063` file under `app/build`; no throwaway suite file.
- Recompile so no stale class of the interrupted turn can be used: `./gradlew --console=plain :app:compileDebugJavaWithJavac :app:transformDebugClassesWithAsm`: `BUILD SUCCESSFUL in 28s`, exit 0 (the app compile task executed; the ASM task was up to date). Byte search over all 12 `PinnedCertificateSocketFactory*.class` copies under `app/build` (debug, release, javac and ASM outputs, plus the two test classes): `SNIHostName` and `U-063 mutant` count 0 in each. The orchestrator's earlier recompile had already replaced the stale M-E2 classes.

### I6c-resume.2 AC-19 test moved to the JDK-provider class (product owner decision of 2026-11-20, deviation 16)

- Pre-move copies saved to `<work>/snapshots/pre-ac19-move/`; `git hash-object` of the copies equals the I5 manifest: pinned class `7c8cd6acceef566182edceb954032308eaf0121c`, JDK-provider class `9fda86c2709b6b4703580895437eb990be71e52b`.
- Pinned class (`PinnedCertificateSocketFactoryCreateSocketTest`), Edit tool: removed `createSocket_implicit_setsNoServerNames` with its Javadoc; removed the imports `java.util.List` and `javax.net.ssl.SNIServerName` (unused afterwards); class Javadoc: added a short paragraph saying AC-19 is tested in `DefaultTrustedSocketFactoryJdkProviderTest`, why (default provider reports no server name whatever is set, M-E2 on 2026-11-17), and that the class now holds AC-18, AC-20 and AC-21 (4 tests); removed the AC-19 bullet from the list of what the tests record, the AC-19 "cannot prove" bullet (its content moved to the new test's Javadoc) and the AC-19 mutation bullet. No other test, helper or assertion changed.
- JDK-provider class (`DefaultTrustedSocketFactoryJdkProviderTest`), Edit tool: added `pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider` (builds `new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(), "imap.right.test", certificate(OTHER_HOST_CERT))`, the same enrolled certificate the pinned class enrols; `createSocket(null, "imap.right.test", 993, null)`; asserts `SSLSocket` and `getSSLParameters().getServerNames()` null or empty; socket never connected, closed in `finally`); Javadoc on the test (AC-19, AC-4 as control, M-E2, what it cannot prove); class Javadoc: opening sentence, and M-E2 added to the per-test mutation list; imports `PinnedCertificateSocketFactory` and static `OTHER_HOST_CERT`, `certificate`. Guards, other four tests and annotations untouched. No `org.conscrypt`, `org.bouncycastle` or `startHandshake` in the file.
- Diffs against the pre-move copies (sanitised): `evidence/I6c-ac19-move-pinned.diff.txt` (only removed lines, plus the two removed imports and the added Javadoc paragraph) and `evidence/I6c-ac19-move-jdk.diff.txt` (only added lines: one import, two static imports, Javadoc lines, the new method). Nothing else differs.
- New hashes: pinned class `dac59ccab1322e0f74f2a5d39f27befd71143226` (was `7c8cd6acceef566182edceb954032308eaf0121c`); JDK-provider class `049a115232392ea84f1c518f39cdbcd9e2665dfa` (was `9fda86c2709b6b4703580895437eb990be71e52b`).
- Targeted run (`evidence/mutations/I6c-move-targeted-cases.txt`, one invocation, unmutated tree): pinned class 4 of 4 PASS, JDK-provider class 5 of 5 PASS (the new test passed on the unmutated code), existing `PinnedCertificateSocketFactoryTest` 8 of 8 PASS; `files=3 cases=17 pass=17 fail=0`. Working-tree count stays 710.
### I6c-resume.3 Mutations (one line each; details in `evidence/mutation-log.md`)

Order: M-E2-jdk, M-E3, M-E4, M-E5. Each was written ahead (`pre.sh`), applied with the Edit tool, run in the foreground with the ten-minute timeout (stale XML removed first), restored at once by the single-file checkout (`restore.sh PCSF`), and verified (all three production files at base, porcelain and diff stat empty) before its result lines were written. M-E1 is not re-run (the move left its two AC-18 tests unchanged, see I6c-resume.2).

- M-E2-jdk: DETECTED (re-run of M-E2 after the 2026-11-20 decision; the 2026-11-17 NOT DETECTED entry stays) (AC-19 failed as expected after the move). Restored 2026-11-20T23:06:25Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-E3: DETECTED (AC-20 failed as expected). Restored 2026-11-20T23:07:57Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-E4: DETECTED (AC-20 and AC-21 failed as expected). Restored 2026-11-20T23:09:10Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.
- M-E5: DETECTED (AC-21 failed as expected). Restored 2026-11-20T23:10:05Z: hash-ok (all three production files at base), porcelain empty, diff stat empty.

All four mutations matched the expected sets exactly. No mutation is in place.

### I6c-resume.4 After the PCSF mutations: stale classes, end baseline, Gate G

- Recompile from the restored source: `./gradlew --console=plain :app:compileDebugJavaWithJavac :app:transformDebugClassesWithAsm`, `BUILD SUCCESSFUL in 21s`. Checks: no file under `app/build` or `k9mail-vendored/build` contains `U-063 mutant host check` (M-E3 marker) (0 files); all 8 non-test `PinnedCertificateSocketFactory*.class` copies (debug and release; javac and ASM outputs) contain neither `SNIHostName` nor `mutant`; no class for the M-E5 anonymous trust manager (`PinnedCertificateSocketFactory` followed by a dollar sign and a digit) exists.
- End baseline (JDK-provider class and pinned create-socket class, one invocation), `evidence/mutations/I6c-resume-baseline-end-cases.txt`: `files=2 cases=9 pass=9 fail=0 error=0 skip=0`.
- Gate G (`:app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification`, foreground, ten-minute timeout, run after `git diff --quiet BASE -- PROD` exit 0): `BUILD SUCCESSFUL in 1m 26s`, exit 0. Totals from this run's XML (`evidence/I6c-junit-totals.txt`): `files=77 tests=710 failures=0 errors=0 skipped=2`. Coverage (`evidence/I6c-jacoco-packages.txt`): `com.zegoggles.smssync.mail 538/743 72.4%`, `mail.transport 241/287 84.0%`, equal to I5, I6a and I6b. Environment note: the daemon JVM line of this run reads `25.0.5.1` (earlier runs: `25.0.5.0.3`); the JDK was updated between sessions, still JDK 25 as declared. Not a plan matter.

### I6 close-out (plan "Close-out of I6c")

1. **Summary table** written at the top of `evidence/mutation-log.md` (one row per entry, one row per AC).
2. **Totals by verdict, over the 39 entries of the mutation log** (the I2b pre-check, the 35 planned mutation ids, and M-B1r, M-C1r, M-E2-jdk):
   - DETECTED 34: 30 of the 35 planned ids, the I2b pre-check M-A4-precheck, the replacements M-B1r and M-C1r, and the re-run M-E2-jdk.
   - BY-DESIGN 2: M-A1g, M-A1c.
   - COMPILE_FAIL 2: M-B1, M-C1 (invalid, not counted).
   - NOT DETECTED 1: M-E2 (2026-11-17, default provider).
   - NOTED-AC16: 1 (M-D11, second Hardening AC-16 test only).
   - Planned ids, 35: 30 DETECTED + 2 BY-DESIGN + 2 COMPILE_FAIL + 1 NOT DETECTED. Every planned id has an entry.
3. **Justification under the Criterion Status Rule for every entry that is not DETECTED.**
   - M-A1g, M-A1c: rule 1, BY-DESIGN. AC-1 itself says the single-layer halves do not break it (the guard and the catch each suffice for the eight-group literal). They are recorded as the pair with M-A1, which is DETECTED by AC-1 and AC-3.
   - M-B1, M-C1: rule 1, COMPILE_FAIL (invalid, never counted). Deleting the only statement that can throw leaves an unreachable catch clause. By the orchestrator's ruling of 2026-11-17 (deviation 15) they stay recorded as invalid and are replaced by M-B1r (exactly AC-6 and AC-8 failed) and M-C1r (exactly the four untrusted and expired tests failed), both DETECTED; AC-6, AC-8, AC-9 and AC-10 are also covered by M-B2 and M-C2.
   - M-E2 (2026-11-17): rule 2 applied then: AC-19 was unmet because the default provider's server-name observable reads null whatever is set. It was not relabelled. The product owner decided on 2026-11-20 (deviation 16) to move the test to the JDK-provider class, where AC-4 is the control for the observable; M-E2 re-run there as M-E2-jdk is DETECTED (exactly the moved test failed, the other 8 tests of the two classes passed). The 2026-11-17 entry stays as recorded. AC-19 is therefore demonstrated by the moved test only, and the pinned class no longer claims it.
   - NOTED-AC16 (M-D11): rule 3. D7c predicted that the default-provider second AC-16 test would pass (Conscrypt supports and enables TLS 1.0 and 1.1 itself). M-D11 was detected by both JDK-provider AC-16 tests, including the first, as the rule requires. M-D12 also failed that default-provider test, so NOTED-AC16 does not apply to it.
   - Nothing was left in place, relabelled, or recorded as a deviation to hide a miss. Every AC named by IC-7 is demonstrated (summary table, second table).
4. **Gradle runs for all of I6: 49 test or gate runs, against the plan's 44** (plan count reference: 35 mutation runs + 6 baseline runs + 3 gates). Actual: 38 mutation runs (the 35 planned, of which M-B1 and M-C1 stopped at compilation but still ran Gradle, plus M-B1r, M-C1r, M-E2-jdk) + 8 baseline-type runs (I6a: start 1, end 2 with one repeat; I6b: start 1, end 1; I6c: start 1, the post-move targeted run 1, final end baseline 1) + 3 gates = 49. By sub-turn: I6a 20, I6b 17, I6c before the interruption 5, I6c after it 7. Differences from 44, all +5: three mutation runs for the replacements and the re-run (M-B1r, M-C1r, M-E2-jdk); one repeated end baseline in I6a (Robolectric download lock file, environmental); one targeted run of the three classes after the AC-19 move. The interruption itself cost no test run; the recompiles after it (two compile-only Gradle invocations by me this turn, one by the orchestrator before) are not counted as test runs. No run hung or moved to the background.
5. **`evidence/I6-clean.txt`** written: `git diff --stat BASE` over the production paths empty, `git diff --quiet` exit 0, `git status --porcelain -- app/src/main k9mail-vendored` empty, `gradle/verification-metadata.xml` diff empty, and the three production hashes next to their base blobs.
6. `bug_requests`: none new (BUG-019 was already filed; no further defect found under AC-23).

### I6c-resume.5 End-of-turn checks

- Production files, hash versus base blob (both listed): DTSF `d543927166b3c5e1da0aaa8ab584072a43bf7521` = `d543927166b3c5e1da0aaa8ab584072a43bf7521`; TMF `3b448058a393c5455ba9524f4b1a5fe9e2926c88` = `3b448058a393c5455ba9524f4b1a5fe9e2926c88`; PCSF `8a0fbb64fb171624591fbebafb818555f28efd58` = `8a0fbb64fb171624591fbebafb818555f28efd58`. Porcelain and diff stat for `app/src/main k9mail-vendored` empty.
- Product-path porcelain: the SNI test modified plus the six untracked test files (equal to the I5 manifest). Seven hashes (`evidence/I6c-manifest.txt`): `TlsTestFixtures` `4e013b43d6384048126e6ff6c580e5da5cd90d4d`, `LoopbackTlsTestServer` `4eafda53d64522a3a1f579d4a2a5656714571af9`, SNI test `36eedaeef53813bad3519e0c501a4173a77375fa`, **JDK-provider class `049a115232392ea84f1c518f39cdbcd9e2665dfa` (changed)**, Hardening `ed535dc90bfbe1cafb869297f752fccb5478d27b`, Handshake `22a57393adb7c866677410910a0219ba504ff048`, **pinned create-socket class `dac59ccab1322e0f74f2a5d39f27befd71143226` (changed)**. The five others equal the I5 manifest.
- Counts: C 710, `HEAD` form 682. Per-class cases from the gate's XML: SNI 12, JDK-provider 5, Hardening 9, Handshake 8, pinned create-socket 4, existing pinned 8 (46 in the six classes).
- `evidence/I6c-manifest.txt` rewritten as the reference state for I7; the earlier one kept as `evidence/I6c-manifest-interrupted.txt`. Snapshots of the two changed files in `<work>/snapshots/I6c/` (pre-move copies in `<work>/snapshots/pre-ac19-move/`).
- `find app/build -iname '*U063*'` = 0; no throwaway file under `app/src/test`.
- Git commands run this turn: status, diff, hash-object, rev-parse, grep, ls-files, and `git checkout -- <PCSF>` four times (once per mutation, through `restore.sh PCSF`). No commit, add, stash, reset, restore or clean.
- Hygiene scan (e-mail pattern, user-profile path, application-data folder, user name, host name, work-folder markers) over this log, the mutation log, the new evidence files and the two changed test files: 0 hits.

### Differences from the plan and the turn message

- The AC-19 test moved (deviation 16); M-E2 re-run as M-E2-jdk; the pinned class and the JDK-provider class changed by hand, as the product owner decided.
- The pinned class's Javadoc also lost its AC-19 bullets in the "records" and "mutations" lists and its "cannot prove" bullet (the message asked for a Javadoc update; these bullets would otherwise have described a test the class no longer holds). The "cannot prove" content moved into the new test's Javadoc.
- M-E3 was applied with three Edit calls (three insertions), the plan's text.
- The daemon JVM version string moved from `25.0.5.0.3` to `25.0.5.1` between sessions.
- The Runtime Claims section (empty at I0a) is revisited at I7, as planned.

## I6c-resume END

```yaml
handoff:
  increment: "I6c (resumed; I6 complete)"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification", summary: "evidence/I6c-gates.txt", exit: 0, junit: "files=77 tests=710 failures=0 errors=0 skipped=2"}
  count_worktree: 710
  count_head: 682
  product_paths:
    - " M app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java"
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java"
  manifest: "evidence/I6c-manifest.txt"
  escalations: []
  decisions_needed: []
  bug_requests: []
  recovered: true
  notes:
    - "I6 complete: 39 mutation-log entries, each restored; DETECTED 34, BY-DESIGN 2, COMPILE_FAIL 2 (replaced by M-B1r, M-C1r), NOT DETECTED 1 (M-E2, 2026-11-17, superseded by the move and M-E2-jdk DETECTED); NOTED-AC16 on M-D11 only"
    - "no mutation in place: DTSF d543927166b3c5e1da0aaa8ab584072a43bf7521, TMF 3b448058a393c5455ba9524f4b1a5fe9e2926c88, PCSF 8a0fbb64fb171624591fbebafb818555f28efd58 equal their base blobs; porcelain and diff stat empty; no stale mutated class under app/build"
    - "per-class cases: SNI 12, JDK-provider 5, Hardening 9, Handshake 8, pinned create-socket 4, existing pinned 8"
    - "next: I7 (final verification and gates). The plan's I7 text names JDK-provider 4 and pinned create-socket 5 cases; after the move they are 5 and 4 (the I7.3 repeat run stays 38 cases: SNI 12, JDK 5, Hardening 9, Handshake 8, pinned 4; total 710 unchanged)"
    - "work-folder helpers and snapshots are in <work>; delete at I7"
```

## I7 START 2026-11-20T23:40Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

### I7.0 Start check, suite sweep, APK baseline

- HEAD equals the start commit. Suite sweep (`ls` for `U063OrderedSuite*Test.java` under `app/src/test/java/com/zegoggles/smssync/mail/`): nothing listed. Product-path porcelain equals `evidence/I6c-manifest.txt` and its seven hashes equal it; count C 710, `HEAD` form 682; `git diff --quiet BASE -- app/src/main k9mail-vendored` exit 0; no `U063` file under `app/build`.
- Numbers used (they supersede plan I7 text where it differs): 710 / 682; 28 new tests; 6 new files; SNI 12, JDK-provider 5, Hardening 9, Handshake 8, pinned create-socket 4, existing pinned 8; the I7.3 repeat set is 38 cases; the ordered suite has 7 children; 2 skipped; DP-2 Option A (BUG-019).
- APK baseline: the plan asks for a comparison with the I0 baseline, but I0 recorded no APK size. The clean rebuild of step 5 deletes the old outputs, so before it I recorded the artefacts that the I0a gate left (their timestamps are those of I0a, and only test files have changed since): `evidence/I7-apk-before.txt` (release unsigned APK 1995017 bytes; debug APK 6468693 bytes; SHA-256 values in the file).

### I7.1 Hygiene and scope greps (`evidence/I7-greps.txt`)

- IC-6 words: 0 hits in the six new files and in the SNI additions (the only hits in the whole mail test folder are two comment lines in an existing file, `K9MailTransportCreateOpenTest.java`, not U-063).
- Forbidden-construct grep (the plan's pattern) over the six new files: the allowed `ConscryptMode` lines of the JDK-provider class (import, static import, annotation). **One difference from the plan's "0 hits":** the pattern word `Timeout` also matches `LoopbackTlsTestServer.java` lines 17, 101, 134, 169, 191 (`TimeoutException`, `setSoTimeout`). These are the bounded waits that IC-6 itself requires (socket read timeouts and the `Future.get` timeout); the pattern was written to catch a JUnit timeout rule or `@Test(timeout`, and none is present. I recorded the hits as explained and did not change the file or the pattern. SNI additions: 0 hits.
- `LooperMode` and `ConscryptMode` appear only in `DefaultTrustedSocketFactoryJdkProviderTest.java`. `startHandshake` in that class: 0. Imports from `org.conscrypt` or `org.bouncycastle`: 0 (six files and SNI additions). Qualified annotations: 0 (six files and SNI additions). IC-12: 0. All six new class names contain `Test`. Annotations in the six files are short-form (After 2, Before 3, ConscryptMode 1, LooperMode 1, Override 1, RunWith 4, Test 26). `@Test` appears only as annotation lines: 26 in the six files plus 2 added to the SNI test = 28.
- Evidence tooling: `evidence/tools/U063HelperSmokeTest.java.txt` and `evidence/tools/ExtractFixtures.java` carry no key material (0 hits for PEM markers, `PRIVATE`, base64 key bodies). They sit under `sdlc/`, which no source set references (no mention in the build files), so no compile task sees them. The IC-2 and annotation checks are limited to `app/src/test` and so do not examine them; the IC-8 scan does include them.
- IC-8 scan, first pass (stream: SNI diff, six sources, `plan.md`, `plan-review.md`, `contract-verification.md`, `expert-depth-check.md`, this log, every file under `evidence/` including `tools/` and `mutations/`; the story file excluded): pattern hits 0, host-name hits 0, user-name hits 0. The values are not written anywhere.

### I7.2 IC-2 and IC-10, working-tree forms (`evidence/I7-ic2.txt`)

- `git diff --name-only BASE -- . ':(exclude)sdlc'` lists exactly the SNI test. `git ls-files --others --exclude-standard` over the product pathspec lists exactly the six new files and nothing else. `git diff --stat BASE` over the production paths: empty; over `gradle/verification-metadata.xml`: empty.
- SNI test: 63 lines added, 0 deleted (additions only; the ten existing methods are untouched). `PinnedCertificateSocketFactoryTest.java`: `git diff --quiet BASE` exit 0, hash `611331b0e650f531f185061b422296894aa50a22` equals the blob at the start commit.

### I7.3 IC-6 determinism: three `--rerun` runs (`evidence/I7-ic6-run1-cases.txt` to `run3`, `I7-ic6-diff.txt`)

Command per run: `rm -rf <results>; ./gradlew -I <init> --console=plain :app:testDebugUnitTest --rerun` with one `--tests` filter for each of the five classes (Sni, Hardening, Handshake, JDK-provider, pinned create-socket), explicit ten-minute timeout, foreground. Each run read from its own XML:
- Run 1: exit 0, `BUILD SUCCESSFUL in 30s` (wall 32 s), `files=5 cases=38 pass=38 fail=0 error=0 skip=0`.
- Run 2: exit 0, `BUILD SUCCESSFUL in 29s` (wall 30 s), same 38 of 38.
- Run 3: exit 0, `BUILD SUCCESSFUL in 32s` (wall 33 s), same 38 of 38.
- Per class in each run: Sni 12, Hardening 9, Handshake 8, JDK-provider 5, pinned create-socket 4. `diff` run 1 versus run 2 and run 1 versus run 3: empty. The Robolectric lock-file error did not appear in any run (0 mentions in each raw log). Plan command corrected: the JDK-provider class is written into the command (A-W5).

### I7.4 START 2026-11-20T23:58Z (IC-9 ordered suites). Written before the first suite file is created.

If this section has no "I7.4 done" line below, a throwaway suite file `U063OrderedSuite*Test.java` may exist under `app/src/test/java/com/zegoggles/smssync/mail/`: the first action of whoever follows is the sweep (`ls`, `rm` exactly that file, record `suite-sweep: removed <name>`), then check that the product-path porcelain equals `evidence/I6c-manifest.txt` and count C is 710.

I7.4 done 2026-11-21T00:05Z. Two throwaway suites (`U063OrderedSuiteForwardTest`, `U063OrderedSuiteReverseTest`, package `com.zegoggles.smssync.mail`, short-form `@SuiteClasses`, no `@Test` text, one file at a time, each removed straight after its run; sources kept as `evidence/tools/U063OrderedSuite*Test.java.txt`). Seven children in the plan's order (JDK-provider between Conscrypt-mode classes), forward and reversed; `--rerun`, explicit ten-minute timeout, foreground.
- Forward: exit 0, `BUILD SUCCESSFUL in 40s`, `files=7 cases=93 pass=93 fail=0 error=0 skip=0`.
- Reverse: exit 0, `BUILD SUCCESSFUL in 1m 5s`, same 93 of 93.
- Per class in both: Handshake 8, Hardening 9, JDK-provider 5, pinned create-socket 4, SNI 12, `K9MailTransportTest` 44, `BackupImapStoreTest` 11 (the last two equal `evidence/I0-suite-children-baseline.txt`); total 38 + 55 = 93. No shortfall.
- Plan corrections applied: I7.4(c) written with 12 SNI and 5 and 4 for the JDK-provider and pinned classes; "six child classes" read as seven.
- After both runs (`evidence/I7-ic9.txt`): suite sweep lists nothing; product-path porcelain and all seven hashes equal `evidence/I6c-manifest.txt`; count C 710. The compiled suite classes were still under `app/build` (the second suite's class in two output folders and a javac transaction backup of the first); I purged them by recompiling the unit-test sources (`:app:compileDebugUnitTestJavaWithJavac :app:transformDebugUnitTestClassesWithAsm`, both compile-only) and deleting the two backup files and the test-results folder; `find app/build -iname '*U063*'` = 0.

### I7.5 Final gate, IC-3 (`evidence/I7-gates.txt`, `I7-junit-totals.txt`, `I7-skipped.txt`)

`rm -rf <results>; ./gradlew -I <init> --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease`, run on the clean tree (`git diff --quiet BASE -- PROD` exit 0 first), explicit ten-minute timeout, foreground. The tool moved it to its background mode after 180 s; I started no second copy and did not poll it; it ran to the end in the one invocation. Result: `BUILD SUCCESSFUL in 3m 12s`, `156 actionable tasks: 148 executed, 8 up-to-date` (the same figures as the I0 gate), exit 0, daemon JVM line `U063-DAEMON-JVM: java.version=25.0.5.1`. Totals from this run's XML: `files=77 tests=710 failures=0 errors=0 skipped=2`. Skipped: the two baseline tests (`AppTest#shouldGetVersionName`, `AuthPreferencesTest#u012_robolectricSmoke_encryptedPrefsSecretStore_migrateFromPlaintext`); no U-063 test is ignored (DP-2 Option A).

**APK comparison** (`evidence/I7-apk-before.txt` holds the I0a artefacts, read before the clean): release unsigned APK 1995017 bytes before and 1995017 after, SHA-256 identical (`0b7e6aa7...2b42a`); debug APK 6468693 bytes before and after, SHA-256 identical (`a2a237d6...e4dc`). No difference, as expected for a story that changes only tests. Note: I0 did not record an APK size; the "before" values are the artefacts the I0a gate left, which by timestamp are I0a's.

### I7.6 Coverage, IC-5 (`evidence/I7-ic5.txt`, `I7-jacoco-packages.txt`)

- `com.zegoggles.smssync.mail`: I0 531/743 (71.5%), I7 538/743 (72.4%), not lower. `com.zegoggles.smssync.mail.transport`: 241/287 (84.0%) at both. `jacocoTestCoverageVerification` passed; `app/build.gradle` unchanged.
- **A difference I measured and did not explain away:** all 23 package rows were compared with I0; one other row differs, `com.zegoggles.smssync.activity`: I0 199/1011 (19.7%), I7 189/1011 (18.7%). That package is untouched by this story. The figure alternates between 199 and 189 from run to run across the story with production unchanged (I0 199, I1 189, I2 189, I2b 199, I3 199, I4 199, I5 199, I6a 189, I6b 189, I6c 189, I7 189). The cause was not investigated. The verification task passed. The plan's IC-5 names only the two mail packages.
- The jacoco report contains none of the U-063 helper or test class names; all compiled U-063 test classes have `Test` in their names.

### I7.7 Counts, IC-4 (`evidence/I7-count.txt`)

Working tree 710; `HEAD` form 682 (nothing committed). Arithmetic: 682 + 2 (SNI) + 5 (JDK-provider) + 9 (Hardening) + 8 (Handshake) + 4 (pinned create-socket) = 710; 28 new tests.

### I7 checks against AC-22 to AC-24

- AC-22: each of the six new sources and the SNI section comment carries the "After the CR-009 move" paragraph (6 of 6, SNI 1); "cannot prove" text is present in Hardening, Handshake, JDK-provider and pinned classes and in the SNI section; the helper classes (`TlsTestFixtures`, `LoopbackTlsTestServer`) state that they are test-only and throwaway; mutation lists are in the four test classes and the SNI section; the Handshake and pinned classes state the fresh-TLS-context and TLS 1.3 facts (the Hardening class also mentions TLS 1.3); RFC citations: 6066 (SNI section, JDK-provider), 8996 (Hardening, JDK-provider), 5280 and 6125 (Handshake, pinned); the JDK-provider class explains the looper-mode annotation as a sandbox of its own (A-W1) and the permanent A1-G3 tripwire (A-N3).
- AC-23: BUG-019 is named in the Hardening and JDK-provider legacy-protocol tests; no test is ignored.
- AC-24: did not fire. The two product-owner decisions that took the place of a split (separate JDK-provider class, 2026-11-17; move of the AC-19 test, 2026-11-20) are in the story's Implementation Notes (deviations 14 and 16).

### I7.8 Close-out

- Work folder: see the last lines.
- Plan corrections made as I went (A-W5): the JDK-provider class written into the IC-8 stream, the IC-6 repeat command and the class-name `find`; I7.4(c) uses SNI 12, JDK-provider 5, pinned 4; "six child classes" read as seven. After the AC-19 move the plan's I7 text numbers JDK-provider 4 and pinned 5; I used 5 and 4 as instructed (total 38 unchanged).

### Gate and increment table

| Increment | Gate exit | Totals (tests/fail/err/skip) | Count C |
|---|---|---|---|
| I0a | 0 | 682/0/0/2 | 682 |
| I1 | 0 | 682/0/0/2 | 682 |
| I2 | 0 | 684/0/0/2 | 684 |
| I2b | 0 | 688/0/0/2 | 688 |
| I3 to I5 | 0 | 697, 705, 710 | 697, 705, 710 |
| I6a, I6b, I6c | 0 | 710/0/0/2 | 710 |
| I7 (clean) | 0 | 710/0/0/2 | 710 |

HEAD stayed at the start commit throughout (nothing committed). Coverage: I0 and I7 figures in I7.6. Probe decisions, DP-1 (not taken), DP-2 (Option A, BUG-019) and the deviations are in the sections above and in the story's Implementation Notes.

## Summary for reviewers and QA

**What was built.** Tests only. Six new test files and one modified: `TlsTestFixtures`, `LoopbackTlsTestServer`, `DefaultTrustedSocketFactoryHardeningTest`, `DefaultTrustedSocketFactoryHandshakeTest`, `DefaultTrustedSocketFactoryJdkProviderTest` (package `com.zegoggles.smssync.mail.ssl`), `PinnedCertificateSocketFactoryCreateSocketTest` (package `com.zegoggles.smssync.mail`), and two insertions into `DefaultTrustedSocketFactorySniTest` (63 lines added, none deleted). 28 new tests: SNI 2, JDK-provider 5, Hardening 9, Handshake 8, pinned create-socket 4. Count 682 to 710 (working tree); 2 skipped. No production, build, vendored or dependency file changed; the release APK is byte-identical to the I0 one.

**The 16 deviations** (all in the story's Implementation Notes):
1. No commit per increment; commits happen when the product owner asks.
2. IC-2, IC-4, IC-6 and IC-8 are checked in working-tree forms; the literal forms wait for a commit.
3. I0 split into I0a and I0b, and I6 into I6a, I6b and I6c.
4. Test certificates are generated in I0b so the probe uses the real fixtures.
5. IC-6 uses `--rerun`, not `--rerun-tasks`.
6. Each increment's gate also runs the coverage verification.
7. AC-15 (ii) and (iii) leave out cipher suites the provider always reports, within stated bounds.
8. The substitute trust manager uses the algorithm name the production code uses.
9. IC-9's two orders come from throwaway ordered suites, run forward and reversed and deleted after each run.
10. Bugs are drafted by the Developer and filed by the orchestrator.
11. Fixtures are Base64 certificates and PKCS#8 keys.
12. Under DP-2 Option B the end count would be 709 with 4 skipped (not used; Option A applies).
13. The loopback test server always wraps an accepted plain socket with the loopback address string as host (V5), because the Conscrypt server side otherwise fails on a module-access restriction.
14. The SNI and supported-protocols checks that the default provider cannot show live in a separate class that runs on the JDK's own provider (product owner decision, 2026-11-17).
15. M-B1 and M-C1 did not compile; the compiling replacements M-B1r and M-C1r stand in for them (orchestrator ruling, 2026-11-17).
16. The AC-19 test moved from the pinned class to the JDK-provider class after mutation M-E2 was not detected under the default provider (product owner decision, 2026-11-20).

**What each class proves and cannot prove.**
- SNI test additions (AC-1, AC-2): the factory skips SNI for an eight-group IPv6 literal without throwing, and the helper treats it as an IP; cannot show what a device sends.
- JDK-provider class (AC-3, AC-4, AC-16 x2, AC-19): on the JDK's provider the server-name list is empty for an IPv6 literal, holds exactly the host for a DNS name, and the pinned factory sets none; the factory enables the provider's supported protocols minus SSLv3 in the recorded order and turns TLS 1.0 and 1.1 on where the provider leaves them off. Cannot show a device's stack, negotiation, or the wire.
- Hardening class (AC-11 to AC-17): the factory's cipher and protocol lists and the reorder logic, under the default (Conscrypt) provider, which enables TLS 1.0 and 1.1 itself; cannot tell the factory's part from the provider's for those.
- Handshake class (AC-5 to AC-10): real loopback handshakes at TLS 1.3 accept the right host and reject the wrong host, an untrusted chain and an expired chain, on both implicit and STARTTLS shapes; cannot show TLS 1.2 or older, or session resumption.
- Pinned create-socket class (AC-18, AC-20, AC-21): the pinned socket keeps provider defaults, accepts the enrolled certificate whatever host name was asked, and rejects any other with the "Pinned certificate mismatch" cause; an expired enrolled certificate is not exercised (SC-6).

**Mutations.** 39 log entries, each restored and verified: DETECTED 34, BY-DESIGN 2 (M-A1g, M-A1c), COMPILE_FAIL 2 (M-B1, M-C1, replaced by M-B1r and M-C1r, both DETECTED), NOT DETECTED 1 (M-E2 on 2026-11-17 under the default provider; resolved by the move and the re-run M-E2-jdk, DETECTED). NOTED-AC16 on M-D11 only. Every AC named in IC-7 is demonstrated.

**Open findings.** BUG-019 (the default factory enables TLS 1.0 and 1.1 wherever the provider supports them; the keep-or-drop decision is the product owner's, the safety judgement the Security Reviewer's). The contract wording SC-1 (goes to the combined CNTR-MODERNIZATION-001 revision). SC-2 to SC-6 as raised in the story (SC-6: no test presents an expired enrolled certificate to the pinning trust manager). The pinned factory applies no cipher or protocol hardening (recorded by AC-18 as present behaviour). Wire-level SNI facts: under the default provider the socket reports no server name whatever is set, and a capture showed the provider puts a name on the wire by itself for both factories, so "the factory sets no name" is not "no name is sent". The guard A1-G3 in the JDK-provider class is a permanent tripwire: if it fails, the cause is another test that initialised the JDK TLS classes under Conscrypt earlier in the same JVM. The `Timeout` word in the plan's I7.1 pattern matches the required socket and `Future` timeouts in `LoopbackTlsTestServer` (explained, not a violation). The `activity` package line figure moves between 199 and 189 from run to run, unrelated to this story.

**Literal checks that wait for a commit.** IC-2 (`git diff --name-only <base>..HEAD`), IC-4 (the `HEAD` count, expected 710) and IC-8 (`git diff <base>..HEAD` scan; the story file's own IC-8 line is a known benign match). A path-limited commit of the six new files, the SNI test change, the U-063 workspace and the story file is needed first.

**Gradle runs for the whole story.** About 76 test or gate runs (compile-only runs not counted): I0a 1, I0b 2, I0c 3, I1 3, I2 2, I2b 3, I3 2, I4 2, I5 3, I6 49 (plan 44), I7 6 (three repeat runs, two ordered suites, one clean gate). The I3 to I5 figures are read from the increment sections above.

### I7.9 Work folder

After the final IC-8 scan below, `<work>` is removed with one `rm -rf` of that folder only. It holds helper scripts, raw logs (never in the repository), the snapshots (`I1` to `I6c`, `pre-ac19-move`) and the IC-8 stream. Nothing in the plan asks to keep anything for QA, and everything that depends on them is in `evidence/` in sanitised form. Nothing outside that folder is touched.

## I7 END

```yaml
handoff:
  increment: "I7"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease", summary: "evidence/I7-gates.txt", exit: 0, junit: "files=77 tests=710 failures=0 errors=0 skipped=2"}
  count_worktree: 710
  count_head: 682
  product_paths: [the I6c manifest set: 1 modified SNI test, 6 untracked test files]
  manifest: "evidence/I7-manifest.txt"
  escalations: []
  decisions_needed: []
  bug_requests: []
  recovered: false
  notes:
    - "implementation complete; literal IC-2, IC-4, IC-8 wait for a path-limited commit"
    - "release and debug APKs byte-identical to the I0a ones"
    - "activity package coverage 199 vs 189 alternates run to run, unrelated; mail 538/743, mail.transport 241/287"
    - "plan I7.1 pattern word Timeout matches LoopbackTlsTestServer bounded waits (explained)"
```
IC-8 final scan after the log and all evidence were final: pattern_hits=0 host_hits=0 user_hits=0

## FIX-IN-CYCLE START 2026-11-20T23:48:00Z HEAD=395000956bb46cb2acc6d66e62d4b5ef933a2d90

Micro-remediation of three fix-in-cycle review findings. Nothing committed. No production, build, vendored or other test file touched.

| Item | Finding | File and lines | Change |
|---|---|---|---|
| 1 | review-code W2 | `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java` 267-268 (Javadoc), 303-305 (new check) | New precondition "D6''" after the 2-known-suites check: fails, printing F and the remainder, when `remainder.size() - knownInRemainder.size()` is below 1, so check (iii) can never be empty. No new variable. Javadoc bound sentence extended. |
| 2 | review-code N2 | `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java` 173 | Deleted the condition on the class name that does not exist; only `startsWith("com.zegoggles.smssync.mail.ssl.")` remains. |
| 3 | review-security W-9 | this log, line 16 and this section | Top Runtime Claims line replaced (none; evidence is the I2b to I5 case files and the mutation log; limit is device behaviour). The line near the end saying the section is revisited at I7 is left as history. |

Unified diffs: `evidence/FIX-hardening.diff.txt`, `evidence/FIX-handshake.diff.txt`.

`git hash-object` before and after:

| File | Before | After |
|---|---|---|
| `DefaultTrustedSocketFactoryHardeningTest.java` | `ed535dc90bfbe1cafb869297f752fccb5478d27b` | `7e0344374c66287c400aad581d90cb8b08b4c8d1` |
| `DefaultTrustedSocketFactoryHandshakeTest.java` | `22a57393adb7c866677410910a0219ba504ff048` | `5704390e81a107b11b1f5d7e427182b4c483b48d` |
| `implementation-log.md` | `7c370617a248ea85ecb66eb46704a01549841314` | after this section: see the remediation reply (a file cannot contain its own hash) |

Runs:
- Targeted run of the two classes (`evidence/FIX-targeted-gates.txt`, `evidence/FIX-targeted-cases.txt`): Hardening 9 of 9 pass, Handshake 8 of 8 pass, exit 0.
- Full gate, same command as I7 (init script, `clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease`; `evidence/FIX-gates.txt`): BUILD SUCCESSFUL, exit 0, release build built. The tool moved the run to the background after 180 s; it was not polled or relaunched. XML totals (`evidence/FIX-junit-totals.txt`): files=77 tests=710 failures=0 errors=0 skipped=2.
- Coverage (`evidence/FIX-jacoco-packages.txt`): `mail` 538/743, `mail.transport` 241/287, equal to I7. Only other difference from I7: `preferences` 313/412 against 314/412 (same run-to-run jitter as the `activity` figure; production unchanged).
- End checks: production paths clean against the start commit; working-tree test count 710; no test file added or removed; the only tracked change under `app` is the SNI test (63 insertions, unchanged); manifest `evidence/FIX-manifest.txt` (seven test files, five hashes equal to I7).

## FIX-IN-CYCLE END

```yaml
handoff:
  increment: "FIX-IN-CYCLE"
  head: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
  gate: {command: "./gradlew -I evidence/tools/junit-xml.init.gradle --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease", summary: "evidence/FIX-gates.txt", exit: 0, junit: "files=77 tests=710 failures=0 errors=0 skipped=2"}
  count_worktree: 710
  count_head: 682
  product_paths: [the I7 manifest set: 1 modified SNI test, 6 untracked test files]
  manifest: "evidence/FIX-manifest.txt"
  escalations: []
  decisions_needed: []
  bug_requests: []
  recovered: false
  notes:
    - "W2, N2 and W-9 done; nothing else changed"
    - "full gate run was moved to the background by the tool; not polled, not relaunched"
```
