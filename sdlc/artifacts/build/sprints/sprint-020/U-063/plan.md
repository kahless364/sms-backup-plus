---
artifact_type: plan
story_id: "U-063"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-17"
iteration: 3
base_commit: "395000956bb46cb2acc6d66e62d4b5ef933a2d90"
contracts_verified:
  - CNTR-MODERNIZATION-001
risks_identified: 16
---

# Implementation Plan: U-063 — Characterisation Tests for the Two TLS Socket Factories

## Revision History

| Iteration | Change |
|-----------|--------|
| 1 | Initial plan. Plan-review returned FAIL (`plan-review.md`) with blockers B1 to B5 and warnings W1 to W11. |
| 2 | **B1:** the probe decision table gains rows D4c, D5d, D10f, D10g and a catch-all row D13. D10a and D10b name the three controls. **B2:** a new rule ([Criterion Status Rule](#criterion-status-rule-ic-7)): a mutation that no test detects leaves its criterion unmet. Outside the one exception the story allows (the second AC-16 test), that is a stop-and-report, not a recorded deviation. D7a, D7b and D7d are now stop-and-report branches, and the verdict `UNDETECTABLE-HERE` is gone. **B3:** the I7 IC-8 scan covers `plan.md`, `implementation-log.md` and the whole evidence folder including `tools/`, and hand-written log lines follow the evidence rules. Deviation 2 is corrected to match. **B4:** IC-9 order is forced by a throwaway ordered JUnit 4 `Suite`, run once forward and once reversed and deleted after each run (option (i)). The timestamp claim, the serial-fork init script and `--order` are removed. Deviation 9 is rewritten. **B5:** "4 new files" becomes 5, the controls are named, and every count was re-checked against [Count Reference](#count-reference). **W1:** bounds on F. **W2:** resume rule for I2; a clean-tree mutation-recovery case; the named procedure [RECOVER-I6](#recover-i6--recovery-turn-after-a-lost-i6-session). **W3:** every Group E factory uses `imap.right.test`. **W4:** new mutation M-D15 for AC-13 (35 mutations). **W5:** literal-form closure and the story's self-matching pattern line (orchestrator notes). **W6:** a refused write outside the repository stops I0a. **W7:** `accept()` has a timeout. **W8:** null check in the leak guard. **W9:** the greeting read is unbuffered. **W10:** `I0-env.txt` goes through the sanitiser. **W11:** orchestrator note. |

| 3 | Plan-review iteration 2 confirmed B2-B5 and W1-W11 and raised C1 to C5. Only those were changed. **C1:** the probe now prints an exact key list (P1-P10, 63 unconditional keys plus 2 kinds of conditional key, each with fixed value forms). The decision table has a continue row for every expected outcome, one record-only row R0, a stated precedence rule (stop wins, with the overlapping pairs listed), and a narrowed D13. A completeness check (probe key → rows) shows no key and no enumerated value without a row. New stop rows: D1b, D7e, D9r and D10h. **C2:** RECOVER-I6 gains case (a'), "nothing to recover". **C3:** resume rule 6 (throwaway-suite sweep) and Notes item 8 extended to I7.4. **C4:** short-form annotation rule and an I7.1 qualified-annotation check. **C5:** the I7 opening sentence is corrected. |
| A1 (post-probe amendment, not a gate iteration; frontmatter stays `iteration: 3`) | Applies the I0b probe, the I0c diagnostic, the orchestrator's V5 ruling (deviation 13) and the product owner's "Separate class, JDK provider" decision (deviation 14), all of 2026-11-17. New section [Amendment A1](#amendment-a1--after-the-environment-probe-2026-11-17): the I0 outcome table; `LoopbackTlsTestServer` under V5; the new `DefaultTrustedSocketFactoryJdkProviderTest` (AC-3, AC-4 and two AC-16 tests, `@ConscryptMode(OFF)` plus `@LooperMode(LEGACY)` for its own sandbox); the I2b pre-check with both branches; the sandbox, JVM-wide initialisation and isolation guards A1-G1 to A1-G3; the counts (710, or 708 in the fallback); the mutation class runs; the carried-forward Javadoc and DP-2 instructions; the increment list (12 turns). Edits elsewhere are marked "(A1)". |

## Story Overview

The story adds tests only: 26 new `@Test` methods in five groups (A to E). (A1) After the probe it is 28 tests in six classes, or 26 in the SNI fallback ([Amendment A1](#amendment-a1--after-the-environment-probe-2026-11-17), A1.f). They record how
`com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory` (with its trust manager
`com.fsck.k9.mail.ssl.TrustManagerFactory`) and `com.zegoggles.smssync.mail.PinnedCertificateSocketFactory`
behave today, before the default factory is moved into app-owned code (CR-009, Suggested Approach:
"characterisation tests first"). No production code, vendored code, build file or dependency changes.

The plan settles what the story left open. It decides the probe and its decision table, how the
handshakes reach the code, the fixture method, global-state handling, the mutation protocol, DP-2
handling, the increments, the verification map, evidence hygiene, the contract reading and the risks.
It also adapts the story's increment and commit shape to two constraints: no commits per increment, and
endpoint security software that kills shells that start many processes.

Every line reference below was re-read at base commit `39500095` (HEAD at planning time). The product
paths (`app`, `k9mail-vendored`, build files, `gradle/`) have no local changes at planning time (checked
with `git status --porcelain`).

## Amendment A1 — after the environment probe (2026-11-17)

**Status.** This is a post-probe amendment, not a gate iteration: the plan stays at `iteration: 3`.
It applies three inputs:
- the I0b probe (`evidence/I0-probe.txt`, 65 lines);
- the diagnostic turn I0c (`evidence/I0c-diag.txt`, runs 1 to 3);
- the orchestrator's rulings and the product owner's decision of 2026-11-17 (story, Implementation
  Notes).

Every other part of the plan that A1 touches is marked "(A1)". Where they differ, A1 governs. The
Criterion Status Rule is unchanged.

### A1.a — I0 outcome (IC-11 record)

Provider facts (`I0-probe.txt`):
- The TLS provider is Conscrypt (`prov_sslcontext_tls=Conscrypt`, `:59`).
- `sdk_int=29` and `java_version=25.0.5.0.3` (`:56-57`).
- Supported protocols equal default-enabled protocols, `[TLSv1, TLSv1.1, TLSv1.2, TLSv1.3]` (`:26-27`).
- The hardened list is `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3]` (`:28`).
- `jdk.tls.disabledAlgorithms` lists `SSLv3, TLSv1, TLSv1.1, …` (`:65`).
- Every negotiated protocol was TLS 1.3 (`:12`).

| Probe row(s) matched | Evidence | Decision | What it changes |
|----------------------|----------|----------|-----------------|
| D1a, D2a, D4a, D6' with D6b, D7a', D7c, D7d', D7e', D8a, D9a, D9r', D10a for the rejections, R0 | `I0-probe.txt:2-4,6-9,13-15,19-22,26-55` | Continue as planned | F = the three TLS 1.3 suites (`:34`), so AC-15 uses deviation 7. D7c (TLS 1.0/1.1 enabled by the provider default, `:40,43`) means the default-provider second AC-16 test is `NOTED-AC16` for M-D11 and M-D12 |
| D3b | `:16-18`: Robolectric's verifier accepts the right host and rejects the other | No verifier swap in the handshake classes. The verifier leak guard stays | I4: `@Before` swaps only the trust manager |
| D13(d) and D11: the controls show `server=FAILED` and `byte=-1` | `:1,5,10`; root cause `I0c-diag.txt:144-145,254,256` | **Closed by V5** (orchestrator ruling, deviation 13; A1.b). Under V5 all three controls are `OK byte=42 server=OK` and every rejection keeps its direct cause and marker (`I0c-diag.txt:287-297`) | `LoopbackTlsTestServer` per A1.b |
| D10b variants 1-4 | `I0c-diag.txt:41-78` | Variants 1, 2 and 4 fail the condition. Variant 3 (SunJSSE server beside Conscrypt) fails on a JVM-wide JDK initialisation error (`:54-65`, `:250-252`) | Not used |
| D5b: `sni_obs_dns=null`, and likewise `sni_obs_ipv6` and `sni_obs_pinned_implicit` | `I0-probe.txt:23-25`; `I0c-diag.txt:89-91,258-285` (null before and after the handshake) | **Handled by the new JDK-provider class** (product owner decision, deviation 14; A1.c). AC-3 and AC-4 move there. Under Conscrypt they have no observable | I2 loses AC-3/AC-4; new increment I2b |
| D7b: `supported_minus_sslv3_equals_default_set=true` | `I0-probe.txt:49` | **Handled by the new class.** M-D11 (`:107`) is killed there. M-D13 stays detectable with the default provider (`protocols_differ=true`, `:48`) | AC-16 gains two tests in the new class |
| D5c (pinned) | `I0-probe.txt:25`: `null`, which is the D5e "empty" value | AC-19 as written: the value is `null` | none |
| Probe-key naming | orchestrator ruling D-W1 | `restore_ok` means the two keys `p8_restore_ok` and `p9_restore_ok` (both `true`, `:9,22`). **Closed** | none |
| DP-1 | fixtures from `keytool` | Not triggered | none |

**Wire facts (I0c run 2 N and run 3 N2; story notes).** Under Conscrypt:
- the layered shape sends the host given to `createSocket` (`I0c-diag.txt:264,319`);
- an IPv6 literal on the layered shape sends no name (`:266,322`);
- the implicit shape sends the connect address's host name (`:262,304,307`);
- the pinned factory sends the same names as the default factory (`:271,273,313,316,325`).

So under Conscrypt the default factory's `setSniHost` has no observable effect. **A1.i:** no wire-level
(ClientHello capture) test is added by this story. These facts stay in the story notes and go to a
follow-up. What a device's TLS stack sends is not shown.

### A1.b — `LoopbackTlsTestServer` under V5 (deviation 13)

This replaces the I1 server description in both shapes:
1. **Listening socket.** Both modes bind a plain `ServerSocket` with `bindPlainLoopback()`: port 0,
   backlog 1, `InetAddress.getLoopbackAddress()`, `setSoTimeout(TIMEOUT_MILLIS)`. The I1
   `createServerSocket` from an `SSLServerSocketFactory` is no longer used.
2. **Server task** (one daemon thread, as in I1):
   - `accept()` (bounded by the timeout above), then `setSoTimeout(TIMEOUT_MILLIS)` on the accepted
     socket;
   - **STARTTLS only:** write `GREETING + "\r\n"` and flush;
   - **both modes:** wrap with
     `serverContext.getSocketFactory().createSocket(plain, InetAddress.getLoopbackAddress().getHostAddress(), plain.getPort(), true)`,
     cast to `SSLSocket`, `setUseClientMode(false)`, then `startHandshake()`. The implicit mode wraps
     immediately after `accept()`, without the greeting;
   - write one byte `PAYLOAD` (`0x2A`), flush, close.
3. **Outcome.** The task returns `"OK"`, or `"FAILED:" + e.getClass().getName()` for any `Throwable`. It
   never throws. `awaitOutcome()` returns the value within `TIMEOUT_MILLIS`; a timeout is reported as
   `TIMEOUT` by the test, not hidden.
4. **Client untouched.** The client side is never reconfigured: no client protocols, ciphers, parameters
   or host change. The D10b variants list is closed: V5 is the only adopted variant.
5. **Javadoc (one sentence, required):** "The server always wraps an accepted plain socket and passes the
   loopback address string as the peer host, because Conscrypt's server engine otherwise reflects into
   `java.net.InetAddress` internals, which the JDK's module system refuses (`java.base` does not open
   `java.net` to unnamed modules)." No absolute or machine path appears in it.
6. Under V5 no source contains `"127.0.0.1"`; the address string comes from the call above (IC-6
   unchanged).

### A1.c — The new class: `DefaultTrustedSocketFactoryJdkProviderTest` (deviation 14)

- **File:** `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`;
  package `com.zegoggles.smssync.mail.ssl`. The name contains `Test` (JaCoCo filter).
- **Annotations (short forms, C4):**
  - `@RunWith(RobolectricTestRunner.class)`;
  - `@ConscryptMode(OFF)` with `import org.robolectric.annotation.ConscryptMode;` and
    `import static org.robolectric.annotation.ConscryptMode.Mode.OFF;`;
  - `@LooperMode(LEGACY)` with `import org.robolectric.annotation.LooperMode;` and
    `import static org.robolectric.annotation.LooperMode.Mode.LEGACY;`.

  `@LooperMode(LEGACY)` is there only to give the class its own sandbox (A1.e). The class uses no
  `Looper`. This is the only class in the story allowed to use either annotation; the I7.1 grep checks
  that.
- **No handshake in this class** (A1.e). Every socket is unconnected, or layered over a plain loopback
  connection without a handshake. Each is closed in `finally`.
- **Shared helpers:** reflective `constant(name)` as in I3; `defaultSocket()` from a fresh
  `SSLContext.getInstance("TLS")` (SunJSSE here), `init(null, null, null)`, unconnected;
  `hardenedSocket(host)` = `new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()).createSocket(null, host, 993, null)`.

| Test | AC | Observable | Exact assertions | Mutations that must fail it |
|------|----|-----------|------------------|-----------------------------|
| `createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset` | AC-3 (moved from I2) | `hardenedSocket("2001:0db8:0000:0000:0000:0000:0000:0001").getSSLParameters().getServerNames()` | the call does not throw; the result is an `SSLSocket`; server names are `null` or empty | M-A1 (the exception escapes `createSocket`); M-A3 (a name appears) |
| `createSocket_dnsHost_setsServerNameOnRealSocket` | AC-4 (moved from I2) | `hardenedSocket("imap.right.test").getSSLParameters().getServerNames()` | exactly one entry, an `SNIHostName` whose `getAsciiName()` equals `imap.right.test` | M-A3 (wrong name), M-A4 (no name) |
| `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder_jdkProvider` | AC-16 (`:107` clause) | `hardenedSocket("imap.right.test").getEnabledProtocols()` against `defaultSocket().getSupportedProtocols()` | **Precondition, failing with message "A1-P1":** the default socket's supported set is not equal to its enabled set (otherwise this class cannot tell supported from enabled). Then: `SSLv3` is not enabled; the enabled set equals supported minus `SSLv3`; among those present, `TLSv1.2` precedes `TLSv1.1` precedes `TLSv1`, and all three precede any other protocol. I0c observed `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3, SSLv2Hello]` (`I0c-diag.txt:15,19`) | **M-D11** (the set shrinks to the enabled set); M-D2 (SSLv3 becomes enabled; SunJSSE supports it, `:7`); M-D5; M-D8; M-D13 |
| `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff` | AC-16 (second test, JDK form; DP-2) | the same sockets | **Precondition "A1-P2":** for each of `TLSv1` and `TLSv1.1`, the default socket lists it as supported and **not** enabled (`I0c-diag.txt:10-13`). Then each is enabled on the hardened socket | **M-D11**; M-D12 |

**Decisions under A1.c.**
- **AC-3 and AC-4 move to the new class and are not duplicated.** Under Conscrypt the observable is
  `null` before and after the handshake whatever the factory sets (`I0-probe.txt:23-24`;
  `I0c-diag.txt:89-91,258-261`). There AC-3 would pass vacuously and AC-4 cannot pass, so a copy in the
  default class has no value. AC-1 and AC-2 (mock and reflection; provider-independent) stay in
  `DefaultTrustedSocketFactorySniTest`.
- **AC-16.**
  - M-D11 is killed by the new class's first AC-16 test, and also by its second.
  - The default-provider AC-16 tests in `DefaultTrustedSocketFactoryHardeningTest` stay: the first
    kills M-D13 and M-D5 under Conscrypt. The second is `NOTED-AC16` for M-D11 and M-D12 (D7c,
    Criterion Status Rule, rule 3).
  - The second AC-16 test also runs in the new class: under SunJSSE it is no longer indistinguishable
    from the provider. The factory enables TLS 1.0 and 1.1 that the provider leaves off, which is the
    DP-2 finding in its clearest form.
- **Javadoc of the new class (AC-22 plus A1.h)** states:
  - why the class exists (deviation 14);
  - that it runs on the JDK provider, which is neither the app's runtime provider nor a device's;
  - that "enabled" is not "negotiable": the JDK's `jdk.tls.disabledAlgorithms` (I0b: `SSLv3, TLSv1,
    TLSv1.1, …`) may still refuse TLS 1.0 and 1.1 at handshake, so the test asserts only what the
    factory enables (RFC 8996 deprecates TLS 1.0 and 1.1);
  - RFC 6066 section 3 for the SNI rules;
  - the "After the CR-009 move" paragraph (A1.h);
  - that the class makes no handshake.

### A1.d — Pre-check inside I2b, with both branches

I2b creates the new class and runs a pre-check in the same turn, before the class is counted as done:
1. TR(`com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest`, `I2b-jdk`). It must
   give 4 cases, all PASS, with the guards of A1.e passing.
2. **SNI pre-check, under the I6 mutation protocol.** Write-ahead entry `M-A4-precheck` in
   `mutation-log.md`; apply M-A4 (delete `:177`); TR the class; restore and hash-check. Expected: AC-4
   FAILS and AC-3 PASSES.
3. **Branch "observable works"** (step 1 all PASS, and step 2 shows AC-4 failing): keep all 4 tests. The
   counts are the main column of the [A1 count table](#a1f--counts).
4. **Branch "observable does not work"** (AC-4 fails unmutated, AC-3 sees a name, or M-A4 does not make
   AC-4 fail). This is the product owner's fallback:
   - delete the two AC-3/AC-4 methods from the class, so it keeps the 2 AC-16 tests;
   - record in the hand-off, under `decisions_needed` as information, "AC-3 and AC-4 dropped by the
     product owner's fallback (2026-11-17); gap: no unit test observes the factory's SNI setting"; the
     orchestrator writes this into the story notes;
   - mark M-A3 and M-A4 "not applicable (AC dropped)" in the mutation log; M-A1 then expects AC-1 only;
   - the counts are the fallback column of the A1 count table.

   This branch does not stop the story.
5. Gate G, end check, manifest, snapshot, log, hand-off.

### A1.e — What the provider switch touches

1. **Sandbox and static lists (established from the Robolectric 4.15.1 jar).**
   `org/robolectric/internal/SandboxManager$SandboxKey` is built from instrumentation configuration,
   SDK, resources mode, looper mode and graphics mode. **The Conscrypt mode is not part of the key.** A
   `@ConscryptMode(OFF)` class alone would therefore share a class loader, and with it
   `DefaultTrustedSocketFactory`'s static `ENABLED_*` lists, with the Conscrypt-mode classes in the same
   JVM. Whichever ran first would fix the lists for both:
   - Conscrypt first: M-D11 cannot be detected in the new class;
   - SunJSSE first: the Conscrypt classes would get `SSLv2Hello` and DHE suites, which a Conscrypt
     socket rejects.

   No test in the suite uses `@LooperMode` or `@GraphicsMode` (grep: 0). `@LooperMode(LEGACY)` therefore
   gives the new class a sandbox of its own, still on API 29 with the same cached android-all jar, so no
   download is needed. In it the static lists are computed under SunJSSE on first use, and they never
   reach the Conscrypt sandbox.

   This is a **checked assumption**: Robolectric honours the key as read, and LEGACY works on API 29.
   Two isolation guards check it on every run:
   - **A1-G1**, in the new class's `@Before`: `SSLContext.getInstance("TLS").getProvider().getName()`
     equals `SunJSSE`, no registered provider is named `Conscrypt`, and the reflected `ENABLED_CIPHERS`
     contains at least one `TLS_DHE_` suite. SunJSSE's defaults have DHE suites (`I0c-diag.txt:14`);
     Conscrypt's have none (`I0-probe.txt:30`).
   - **A1-G2**, added to the `@Before` of `DefaultTrustedSocketFactoryHardeningTest` and
     `DefaultTrustedSocketFactoryHandshakeTest` (A1): the reflected `ENABLED_PROTOCOLS` does not contain
     `SSLv2Hello`, and `ENABLED_CIPHERS` has no `TLS_DHE_` suite. That would mean SunJSSE-computed lists
     reached the Conscrypt sandbox.

   Both fail with a message naming A1-G1 or A1-G2. **Branch:** a guard failure at I2b, I3, I4 or I7 is a
   **stop and report**; the isolation needs another mechanism, which the orchestrator rules on. Neither
   guard is affected by any listed mutation: no mutation adds or removes DHE suites or `SSLv2Hello`.
2. **JVM-wide JDK TLS initialisation.** The JDK's own TLS classes are global to the JVM. If any
   Conscrypt-mode code creates a SunJSSE socket first, their initialisation fails for the rest of that
   JVM (`I0c-diag.txt:250-252`: `NamedGroup$SupportedGroups`). With Conscrypt off, it succeeds
   (`I0c-diag.txt:15-20,239-245`). Variant 3's failure is therefore not relevant to the new class, **on
   the condition that no Conscrypt-mode code in the same JVM touched SunJSSE sockets first**.
   - U-063's own Conscrypt-mode tests never request the SunJSSE provider (all use
     `SSLContext.getInstance("TLS")`). Whether any existing test does is not known.
   - **Guard A1-G3**, in the new class's `@Before`: create one unconnected `defaultSocket()` and close
     it. Catch `NoClassDefFoundError` or `ExceptionInInitializerError` and fail with "A1-G3: the JDK TLS
     classes were initialised under Conscrypt earlier in this JVM".
   - Branch: stop and report.
   - In the reverse direction, the new class initialising those classes cleanly cannot harm Conscrypt
     classes.
3. **Host-name verifier.** The JDK stub (`I0c-diag.txt:6`) is irrelevant because the class makes no
   handshake. **Handshakes are forbidden in the new class.** I7.1 greps it for `startHandshake` (0).
4. **`jdk.tls.disabledAlgorithms`.** The tests assert what is enabled, never what is negotiable; the
   Javadoc says so (A1.c).
5. **Harness timeouts seen with Conscrypt off** (`I0c-diag.txt:240,244`, `server=TIMEOUT`): irrelevant,
   since there is no handshake here. Recorded.
6. **Global state.** The class sets no global state itself. Robolectric installs and removes Conscrypt
   per test according to the mode. The trust-manager and verifier rules of I4 do not apply here.

### A1.f — Counts

| Item | Main (observable works) | Fallback (A1.d step 4) |
|------|-------------------------|------------------------|
| New product files | **6** (adds `DefaultTrustedSocketFactoryJdkProviderTest`) | 6 |
| Modified product files | 1 (SNI test: AC-1, AC-2 only) | 1 |
| New `@Test` | **28**: SNI 2, JDK-provider 4, Hardening 9, Handshake 8, Pinned 5 | **26**: JDK-provider 2 |
| Count C after I0a, I0b, I0c, I1, I2, I2b, I3, I4, I5 | 682, 682, 682, 682, 684, 688, 697, 705, **710** | …, 684, 686, 695, 703, **708** |
| Skipped | 2 | 2 |
| DP-2 Option B | **711**, skipped **5**: both legacy characterisation tests `@Ignore`d (Hardening and JDK-provider), plus one ignored desired-behaviour test in the JDK-provider class | **709**, skipped **5** |
| Cases in the I7.3 repeat run | **38**: SNI 12, Hardening 9, Handshake 8, Pinned 5, JDK-provider 4 | 36 |
| Mutations | **35**, same ids; class runs change (A1.g); plus one pre-check run `M-A4-precheck`, not counted as a mutation | **33** (M-A3 and M-A4 not applicable) |
| Gradle runs in I6 | 35 mutation runs + 6 baseline runs (one invocation per sub-turn start and end, listing all of that sub-turn's classes) + 3 gates = **44** | 33 + 6 + 3 = 42 |
| Ordered-suite children (IC-9) | 7: Handshake, Hardening, **JDK-provider**, Pinned createSocket, SNI, K9MailTransportTest, BackupImapStoreTest. Expected cases 8 + 9 + 4 + 5 + 12 + the two I0a baselines | the same with JDK-provider 2 |
| Developer turns | 12: I0a, I0b (I0c done outside the plan), I1, I2, **I2b**, I3, I4, I5, I6a, I6b, I6c, I7 | 12 |

### A1.g — Mutation table changes

The class runs below replace the "Class runs" lines of I6a, I6b and I6c (A1). Every run is a single
Gradle invocation with one `--tests` filter per class:
- **Group A mutations** (M-A1, M-A1g, M-A1c, M-A2a, M-A2b, M-A3, M-A4) run `DefaultTrustedSocketFactorySniTest`
  **and** `DefaultTrustedSocketFactoryJdkProviderTest`.
- **Group D mutations** (M-D1..M-D15, with M-D6a and M-D6b) run `DefaultTrustedSocketFactoryHardeningTest`
  **and** `DefaultTrustedSocketFactoryJdkProviderTest`.
- Groups B, C and E are unchanged.

| ID | Expected failures after A1 |
|----|----------------------------|
| M-A1 | AC-1 (SNI test), AC-3 (JDK-provider class; the `IllegalArgumentException` escapes `createSocket`) |
| M-A3 | AC-3 and AC-4 (JDK-provider class) |
| M-A4 | AC-4 (JDK-provider class) |
| M-D2 | AC-11, AC-14 (Hardening); AC-16 first JDK test (SSLv3 enabled under SunJSSE) |
| M-D5 | AC-12, AC-14, AC-16 (Hardening); AC-16 first JDK test |
| M-D8 | AC-15, AC-16 (Hardening); AC-16 first JDK test |
| **M-D11** | **AC-16: both JDK-provider tests** (primary). The default-provider second AC-16 test is `NOTED-AC16` (D7c); the default-provider first AC-16 test is not expected to fail (D7b) |
| M-D12 | AC-14 (Hardening); AC-16 second JDK test. The default-provider second test is `NOTED-AC16` |
| M-D13 | AC-16 first test (Hardening; the order changes, `I0-probe.txt:48`); AC-16 first JDK test |

All other rows are as in I6. **Nothing may be recorded as undetectable** except the default-provider
second AC-16 test under D7c (`NOTED-AC16`). M-D11 not failing a JDK-provider AC-16 test is
`NOT DETECTED` (Criterion Status Rule, rule 2).

### A1.h — Carried-forward instructions (binding)

1. **Every new test class's Javadoc**, including the JDK-provider class and the helpers where relevant,
   has the paragraph "**After the CR-009 move**": a missing field, method or class means the test's
   seam needs retargeting to the moved class; a failed behavioural assertion is a regression, and the
   test is never edited to pass. (Orchestrator ruling, contract-verification W2.)
2. **Handshake classes** (`…HandshakeTest`, `PinnedCertificateSocketFactoryCreateSocketTest`):
   - the "cannot prove" Javadoc states that the rejection tests rely on a fresh TLS context per
     `createSocket` call (`DefaultTrustedSocketFactory.java:165-166`; a resumed session would skip the
     certificate check);
   - it states that the handshakes ran at TLS 1.3 under Conscrypt (`I0-probe.txt:12`), so nothing is
     shown about TLS 1.2 or earlier;
   - it cites RFC 5280 (certificate path validation) and RFC 6125 section 6 (host-name checks).
3. **RFC citations:**
   - RFC 6066 section 3 in the SNI-related Javadoc (SNI test additions, JDK-provider class);
   - RFC 8996 in both AC-16 legacy-protocol tests and the DP-2 bug draft.
4. **DP-2 bug draft at I3** must include:
   - the SunJSSE finding (I2b's second AC-16 test and `I0c-diag.txt:7-8,15`: the factory enables TLS
     1.0 and 1.1 where the JDK provider leaves them off);
   - the Conscrypt fact that supported equals enabled;
   - the caveat that `jdk.tls.disabledAlgorithms` may still refuse them at negotiation on a desktop JDK,
     while a device's provider behaviour is not shown.
5. **Probe key naming:** `p8_restore_ok` and `p9_restore_ok` (closed).
6. **The I4 trust-manager substitute** writes only the test-CA trust manager into the vendored field,
   never a mock or an accept-all manager (contract-verification N6).

### A1.j — Increment list after A1

- **Done:** I0a, I0b, I0c (diagnostic, outside the plan).
- **I1:** fixtures and `LoopbackTlsTestServer` under V5 (A1.b). The helper smoke test must show the
  three controls `OK byte=42 server=OK`.
- **I2:** Group A in the SNI test, **AC-1 and AC-2 only** (2 tests; count 684).
- **I2b (new turn, A1):** the JDK-provider class, its guards and the A1.d pre-check (count 688, or 686
  in the fallback). It comes after Group A and before Group D because it carries Group A's AC-3/AC-4.
  Its AC-16 tests need nothing from I3: their baselines are literal or live.
- **I3, I4, I5, I6a, I6b, I6c, I7:** as planned, with the (A1) edits.
- The turn count rises from 11 to 12. Resume rules 1-6 still hold. For I2b the interrupted-increment
  rule is "delete the new class, then redo I2b". If the pre-check mutation was applied, run
  RECOVER-I6 first, because it uses the I6 protocol.

### A1.k — Verification mapping (replaces the rows named)

| AC / IC | Increment | Command or reading | Evidence |
|---------|-----------|--------------------|----------|
| AC-1, AC-2 | I2, I6a | TR SNI test (12 PASS); additions-only check; M-A1, M-A2a, M-A2b | `I2-sni-cases.txt`, `mutation-log.md` |
| AC-3, AC-4 | I2b, I6a | TR JDK-provider class; A1.d pre-check; M-A1, M-A3, M-A4. Fallback: dropped, with the gap recorded | `I2b-jdk-cases.txt`, `mutation-log.md` (`M-A4-precheck`), story notes |
| AC-16 | I3, I2b, I6a, I6b | TR Hardening (2 default-provider tests) and TR JDK-provider (2 tests); M-D2, M-D5, M-D8, M-D11, M-D12, M-D13; `NOTED-AC16` for the default second test | `I3-hardening-cases.txt`, `I2b-jdk-cases.txt`, `mutation-log.md` |
| IC-2 | I7 | as before, with **6** new files | `I7-ic2.txt` |
| IC-4 | every increment, I7 | count C per A1.f (710, or 708 in the fallback; Option B 711 or 709) | `I7-count.txt` |
| IC-9 | I7 | ordered suites with 7 children (A1.f); guards A1-G1 to A1-G3; leak guards; identity asserts | `I7-ic9-*.txt` |
| IC-11 | I0b, I0c | A1.a table | `I0-probe.txt`, `I0c-diag.txt`, this section |

## Assumption Ledger

```yaml
assumptions:
  verified:
    - claim: "HEAD is 395000956bb46cb2acc6d66e62d4b5ef933a2d90 and the product pathspec has no local change"
      evidence: "git rev-parse HEAD; git status --porcelain --untracked-files=all -- app k9mail-vendored build.gradle settings.gradle gradle (empty)"
    - claim: "Authoritative @Test count at HEAD is 682"
      evidence: "git grep -h \"@Test\" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c \"@Test\" -> 682"
    - claim: "IC-12 search returns 0 lines today"
      evidence: "git grep -nE \"LocalKeyStore|setKeyStoreLocation|addCertificate\" -- app/src/main | wc -l -> 0"
    - claim: "DefaultTrustedSocketFactory line map: lists :34-89 (24 known ciphers :38-61, 16 blocked :65-80, protocols :83-85, SSLv3 :87-89); static probe :91-118 (getSupportedProtocols :107, ENABLED_PROTOCOLS :116); reorder :124-148 (null guard :129, removal loop :129-133, unknown appended :145); createSocket :152-180 (trust manager :155, layered branch :172, hardenSocket :176, setSniHost :177); hardenSocket :182-189 (:184, :187); setSniHost :191-205 (N branch :192, call :197); isIpOrBlankHostname :216-226 (colon :221-223, IPv4 regex :225); setSniViaSSLParameters :238-254 (guard :239-243, try :245-253, catch :248)"
      evidence: "k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java (read in full)"
    - claim: "TrustManagerFactory: private static non-final defaultTrustManager :33; checkServerTrusted :68-94 (system validation :76, host name :77, local key store :91, CertificateChainException :92); per host:port cache :39-61 whose instances read the static field on every call; static init with getInstance(\"X509\") :102-123 (:106); verifyHostname :142-150 asks HttpsURLConnection.getDefaultHostnameVerifier() (:144) and throws SSLException with 'was not verified against the server certificate' (:147-148); MinimalSslSession.getPeerCertificates :164-166"
      evidence: "k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java (read in full)"
    - claim: "LocalKeyStore is inert in the test JVM: the constructor's upgradeKeyStoreFile throws because the location is unset (:188-191), the catch at :46 logs, setKeyStoreFile is never reached, mKeyStore stays null, isValidCertificate returns false (:146-150)"
      evidence: "k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/LocalKeyStore.java:42-55,146-150,178-191"
    - claim: "CertificateChainException extends CertificateException and is in package com.fsck.k9.mail (file .../k9/mail/CertificateChainException.java:11), not ssl/"
      evidence: "k9mail-vendored/src/main/java/com/fsck/k9/mail/CertificateChainException.java:1,11"
    - claim: "ImapConnection: implicit TLS createSocket(null, host, port, alias) :209; STARTTLS createSocket(socket, host, port, alias) :315; handleSslException maps an SSLException whose direct cause is a CertificateException to CertificateValidationException :146-152; first read after connect :119 and after STARTTLS :324; open() resolves the host by DNS :186; connectToAddress is package-private :189; startTLS is private :308; adjustDNSCacheTTL changes java.security properties :171-183"
      evidence: "k9mail-vendored/src/main/java/com/fsck/k9/mail/store/imap/ImapConnection.java:90-330"
    - claim: "CertificateValidationException extends MessagingException"
      evidence: "k9mail-vendored/src/main/java/com/fsck/k9/mail/CertificateValidationException.java:10"
    - claim: "PinnedCertificateSocketFactory: host argument not stored :67-72; createSocket :75-86 builds a plain SSLContext with PinnedX509TrustManager (:77) and neither hardens nor sets SNI; fingerprint check :141-145 with message 'Pinned certificate mismatch'; enrolled validity check :149"
      evidence: "app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java (read in full)"
    - claim: "DefaultTrustedSocketFactorySniTest has 10 @Test at :47,:69,:97,:132,:150,:170,:186,:201,:215,:230; the 'full' IPv6 case passes fe80::1 (:207); reflective helper :250-260. PinnedCertificateSocketFactoryTest has 8 @Test and never calls createSocket"
      evidence: "both files read in full"
    - claim: "No existing test changes JSSE global state or Robolectric's Conscrypt mode, and none touches DefaultTrustedSocketFactory or TrustManagerFactory from a static initialiser or @BeforeClass"
      evidence: "grep over app/src/test for ConscryptMode, Security.insertProviderAt/addProvider/removeProvider/setProperty, setDefaultHostnameVerifier, setDefaultSSLSocketFactory, SSLContext.setDefault, System.setProperty, static.*(DefaultTrustedSocketFactory|TrustManagerFactory), @BeforeClass: 0 relevant hits"
    - claim: "Test configuration: Robolectric 4.15.1, JUnit 4.13.2, Truth 1.4.4, mockito-core 5.18.0 (app/build.gradle:159-163); testOptions only includeAndroidResources (:94-101); JaCoCo filter **/*Test*.* (:237) applied to the ASM root (:328); mail rule com.zegoggles.smssync.mail* LINE 0.70 (:368-376); class roots are app-only (:326-336), so com.fsck.k9.mail.ssl.* is in no rule"
      evidence: "app/build.gradle (read in full)"
    - claim: "Root build.gradle: maxParallelForks 4, forkEvery 100 (:26-28); JUnit XML disabled (:30)"
      evidence: "build.gradle:25-31"
    - claim: "Robolectric runs API 29"
      evidence: "app/src/test/resources/robolectric.properties:8"
    - claim: "Robolectric 4.15.1 ships the ConscryptMode annotation and its AndroidTestEnvironment references org.conscrypt.OpenSSLProvider, Security.insertProviderAt, Security.addProvider, Security.removeProvider and BouncyCastleProvider"
      evidence: "unzip -l of annotations-4.15.1.jar (ConscryptMode, ConscryptMode$Mode) and of robolectric-4.15.1.jar (AndroidTestEnvironment, plugins/ConscryptModeConfigurer); string table of AndroidTestEnvironment.class"
    - claim: "The test runtime classpath carries org.conscrypt:conscrypt-openjdk-uber:2.5.2 and org.bouncycastle:bcprov-jdk18on:1.80 through Robolectric"
      evidence: "sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/qa/qa-deps-debugUnitTestRuntime.txt:160,233 (sprint-017 tree; re-taken at I0)"
    - claim: "The android-all jar used for API 29 (android-all-instrumented 10-robolectric-5803371-i7) has no javax/net entries, so javax.net.ssl.HttpsURLConnection and SNIHostName come from the JDK"
      evidence: "unzip -l of that jar in the local Maven repository: 0 entries under javax/net/"
    - claim: "A JDK 25 is the java on PATH and its home has keytool and the source-file launcher"
      evidence: "java -version (25.0.5); java.home/bin lists keytool.exe and jshell.exe"
    - claim: "JUnit XML can be enabled without a tracked edit (U-061 init script); gate runs take about 1-2.5 minutes"
      evidence: "sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/tools/junit-xml.init.gradle; BUILD SUCCESSFUL times in that evidence folder (16 s to 2 m 37 s)"
    - claim: "core.autocrlf=true; the five target files are LF in the index and CRLF in the working tree; the vendored module compiles at source level 1.8"
      evidence: "git config core.autocrlf; git ls-files --eol; k9mail-vendored/build.gradle:76"
    - claim: "The @Ignore precedents are AuthPreferencesTest.java:1079 and AppTest.java:13"
      evidence: "grep -n @Ignore app/src/test"
  assumed:
    - claim: "Gradle runs the test classes of one fork one after another (no in-JVM parallelism)"
      reason: "No parallel JUnit configuration exists in any build file. IC-9 does not rest on it: the I7 ordered-suite runs force order inside one JVM, and the @After identity assertions hold in any order"
    - claim: "A JUnit 4 Suite class filtered with --tests runs all its child classes, each under its own RobolectricTestRunner, in declaration order"
      reason: "JUnit 4 Suite semantics. Checked at I7.4 by counting every child's cases in the XML; a shortfall is a stop condition (no fallback is taken silently)"
    - claim: "keytool (JDK 25) accepts -startdate on -genkeypair and -gencert and the -ext forms bc, ku, eku, san used in Appendix A"
      reason: "Documented keytool options. Executed in I0b; a keytool error is a stop condition"
    - claim: "Each mutation in the mutation table compiles (Java 8 syntax for the vendored module, -Werror for the app module)"
      reason: "Written to compile; a COMPILE_FAIL run is recorded as an invalid mutation and re-edited, never counted as detected"
  needs_check:   # every item is a probe key in I0b, with a branch in the decision table
    - claim: "Which provider SSLContext.getInstance(\"TLS\") returns under Robolectric 4.15.1 (Conscrypt inserted first by ConscryptMode ON, or SunJSSE)"
      blocker: "The ConscryptMode default is not readable from the jar's string table. Probe P1"
    - claim: "HttpsURLConnection.getDefaultHostnameVerifier() is the JDK stub that returns false for every host"
      blocker: "JDK behaviour as read from the JDK source from memory, not run here. Probe P3"
    - claim: "A loopback handshake completes between the hardened client socket and a test SSLServerSocket, on both call shapes"
      blocker: "Provider protocol filtering and, under SunJSSE, SSLv2Hello in the hardened list. Probe P9"
    - claim: "A certificate rejection surfaces as an SSLException whose direct cause is the CertificateChainException"
      blocker: "Provider wrapping is not visible from this repository. Probe P9"
    - claim: "SNIHostName rejects an eight-group IPv6 literal"
      blocker: "Expected (colon is not an LDH character; the analysis saw the same rule reject a blank host). Probe P4. Both outcomes keep AC-1 and AC-3 meaningful (see decision table)"
    - claim: "getSSLParameters().getServerNames() on the real unconnected socket reports the name set by setSniHost"
      blocker: "Provider-specific. Probe P5; AC-4 depends on it"
    - claim: "Conscrypt prepends its fixed TLS 1.3 suites to getEnabledCipherSuites(), which would make AC-15 (ii)/(iii) false as literally written"
      blocker: "Recalled from Conscrypt's source, not run. Probe P6 key fixed_suites_F"
    - claim: "The provider keeps the order of setEnabledProtocols/setEnabledCipherSuites; TLSv1 and TLSv1.1 are supported but not enabled by default; the provider's default cipher order differs from the reordered list"
      blocker: "Decides whether mutations M-D8, M-D9, M-D11 and M-D13 are distinguishable from provider behaviour. Probe P6"
    - claim: "A server-mode layered socket (STARTTLS server side) works with the provider"
      blocker: "Probe P9 starttls cases"
```

## Deviations From the Story's Literal Text

The orchestrator records acceptance of these in the story's Implementation Notes so that Code Review and
QA do not flag the literal mismatch.

1. **No commit per increment.** The story says "each committed separately". Commits are made only by the
   orchestrator, and only when the product owner asks. The plan therefore does not depend on commits. Each
   increment is verifiable from the uncommitted tree with a manifest of blob hashes (see
   [Working Without Commits](#working-without-commits)). If the product owner asks for commits, the
   orchestrator may commit at any increment boundary, path-limited, and nothing in the plan changes.
2. **IC-2, IC-4, IC-6 and IC-8 in an uncommitted tree.** Those checks are written against
   `<base>..HEAD` or `HEAD`, which show nothing until a commit exists. The plan uses working-tree forms
   that cover the same files:
   - IC-2: `git diff <base>` plus the untracked list;
   - counts and greps: `git grep --untracked`;
   - IC-8: a scan of every file the story will commit. That is the SNI-test diff, the 6 new sources (A1),
     `plan.md`, `implementation-log.md` and the whole evidence folder, `tools/` included (I7.1).

   These forms are evidence for the Developer's turns only. **The literal IC-2, IC-4 and IC-8 can close
   only after a commit.** The orchestrator asks the product owner for a path-limited commit of U-063's
   files before QA, and QA re-runs the literal forms (see
   [Notes for the Orchestrator](#notes-for-the-orchestrator-and-downstream-gates), item 7). The expected
   `HEAD` count before any commit stays 682.
3. **I0 is split into I0a (baseline) and I0b (fixtures and probe). I6 is split into I6a, I6b and I6c.**
   This keeps each Developer turn short enough to resume after a lost session.
4. **The test certificates are generated in I0b, not I1.** The probe must handshake with the real
   fixtures (IC-11). I1 creates `TlsTestFixtures.java` from the same generated material, so what was probed
   is what is committed.
5. **IC-6 uses `--rerun`, not `--rerun-tasks`.** `--rerun` re-executes the test task only. It gives the
   same evidence for the three repeat runs without recompiling every module, and it starts far fewer
   processes (see the endpoint-security constraint).
6. **The per-increment gate also runs `:app:jacocoTestCoverageVerification`.** It costs seconds and catches
   a coverage problem in the increment that causes it.
7. **AC-15 (ii) and (iii) are evaluated without the provider-fixed suites F, but only if the probe finds
   any.** F is the set of suites the provider always reports and the caller cannot configure; it is
   measured live in the test. Conscrypt is expected to prepend its TLS 1.3 suites. Under SunJSSE F is
   expected to be empty and the assertions are literal. This keeps the story's rule that no test asserts
   a property the code does not have, and it does not weaken any listed mutation (see
   [Group D design](#i3--group-d-hardening-9-tests--turn-5)). **F is bounded** (W1), so the order checks
   cannot pass on an empty remainder. The test fails, naming D6', if any of these holds:
   - F shares a name with `KNOWN_CIPHERS_BASELINE`;
   - F shares a name with the `ENABLED_CIPHERS` entries the provider accepted;
   - `hardened − F` holds fewer than 2 known ciphers;
   - no baseline cipher is in the default list (a clear message, not an index error).
8. **The substitute trust manager uses algorithm `"X509"`**, the name the production static initialiser
   uses (`TrustManagerFactory.java:106`), instead of `"PKIX"` (E4). In SunJSSE `X509` is an alias of
   `PKIX`. Using the production name keeps the substitute from the same provider that production would
   get. The probe records which provider that is.
9. **IC-9's two orderings are forced by a throwaway ordered JUnit 4 `Suite`, not by `--tests`.** A
   `--tests` list does not set execution order, but a `@RunWith(Suite.class)` class runs its children in
   declaration order in one JVM. I7.4 runs two such suites:
   - forward: the new classes first, then the SNI test, then two classes that build the default factory;
   - reversed.

   Each suite is created, run alone and deleted before the next step. It is never committed and never
   present at a gate or a count (rules in I7.4). Order evidence is the suite declaration plus a per-child
   case count from the XML. JUnit XML timestamps are not used, because they are too coarse to show order.
   The `@After` identity assertions and the `@Before` leak guards remain the proof that holds in any order.
   The default full run is the third piece of evidence.
10. **Bugs are filed by the orchestrator.** The Developer drafts the bug text (AC-23) in the log and the
    hand-off. The orchestrator runs `/amp:create-bug` and records the id in the story's Implementation
    Notes. The Developer then writes the id into any `@Ignore` reason.
11. **Fixture format.** The fixtures are DER certificates and PKCS#8 private keys as Base64 text,
    extracted once from `keytool`-generated PKCS12 files by a throwaway JDK program. E6 allows this
    ("or exported certificates and PKCS8 keys"). It avoids parsing a PKCS12 blob at test time, which
    depends on whichever provider is first for `KeyStore.PKCS12`.
12. **DP-2 Option B counts.** The story says Option B raises the skipped total by 2 but gives no `@Test`
    total. This plan reads Option B as one extra (ignored) test, so the end count is 709 with 4
    skipped. The orchestrator confirms this reading if Option B is chosen. (A1) Superseded by A1.f:
    711 with 5 skipped (fallback 709 with 5), because there are now two legacy characterisation tests.
13. **(A1) V5 loopback server** (orchestrator ruling, 2026-11-17). Both shapes wrap an accepted plain
    socket with the loopback address string as host. The client is never changed. See A1.b.
14. **(A1) JDK-provider class** (product owner decision, 2026-11-17). AC-3, AC-4 and the `:107` clause
    of AC-16 run in `DefaultTrustedSocketFactoryJdkProviderTest` under `@ConscryptMode(OFF)` (and
    `@LooperMode(LEGACY)` for sandbox isolation). This lifts the rule against `@ConscryptMode` for
    that class only. See A1.c to A1.e.

## Count Reference

(A1) Rows marked (A1) are superseded by A1.f, which also gives the SNI fallback column. Every count in this plan is taken from this table. If a step's number differs from this table, the table
wins and the step is a typo to report.

| Item | Count | Detail |
|------|-------|--------|
| New product files (A1: **6**, adds `DefaultTrustedSocketFactoryJdkProviderTest`) | **5** | `TlsTestFixtures`, `LoopbackTlsTestServer`, `DefaultTrustedSocketFactoryHardeningTest`, `DefaultTrustedSocketFactoryHandshakeTest` (all `…/mail/ssl/`), `PinnedCertificateSocketFactoryCreateSocketTest` (`…/mail/`) |
| Modified product files | **1** | `DefaultTrustedSocketFactorySniTest` (additions only) |
| New `@Test` (A1: **28**: SNI 2, JDK-provider 4, D 9, B+C 8, E 5; fallback 26) | **26** | A 4 (SNI test), D 9 (Hardening), B+C 8 (Handshake), E 5 (Pinned createSocket) |
| Count C after each increment (A1: 682 through I1, then 684 (I2), 688 (I2b), 697 (I3), 705 (I4), **710** (I5); fallback 686, 695, 703, 708) | 682, 682, 682, 686, 695, 703, 708 | after I0a, I0b, I1, I2, I3, I4, I5 (708 also through I6 and I7; Option B: 709) |
| Skipped | 2 | Option B: 4 |
| Cases in the I7.3 repeat run (A1: **38**, adds JDK-provider 4 and SNI 12; fallback 36) | **36** | SNI 14 (10 + 4), Hardening 9, Handshake 8, Pinned createSocket 5 |
| Existing pinned test | 8 | unchanged |
| Handshake **controls** | **3** | implicit-right (P9, AC-5), STARTTLS-right (P9, AC-7), `pin_accept` (P10, AC-20) |
| Handshake rejection cases | 7 | P9: {implicit, STARTTLS} × {other, untrusted, expired} = 6; P10: `pin_reject` = 1 |
| Fixtures | 6 certificates, 4 private keys, 10 constants | CAs: `trusted-ca`, `untrusted-ca` (certificates only); leaves: right, other, untrusted, expired |
| keytool processes in I0b | 24 | 2 per CA, 5 per leaf |
| Mutations | **35** | A 7 (including by-design M-A1g and M-A1c), D 16 (M-D1..M-D15 with M-D6a and M-D6b), B 5, C 2, E 5 |
| Throwaway files | 5, at most 2 at a time | I0b: `U063ProbeTest`, `U063ProbeFixturesTest`; I1: `U063HelperSmokeTest`; I7.4: `U063OrderedSuiteForwardTest`, then `U063OrderedSuiteReverseTest` |
| Developer turns (A1: 12, adds I2b) | 11 | I0a, I0b, I1, I2, I3, I4, I5, I6a, I6b, I6c, I7 (plus RECOVER-I6 only if needed) |

## Criterion Status Rule (IC-7)

This rule applies before any run and is not argued afterwards.

1. **Mutation verdict.** Every mutation in I6 gets exactly one verdict:
   - `DETECTED`: at least one test of an AC that names it fails;
   - `NOT DETECTED`: no test fails;
   - `BY-DESIGN`: only M-A1g and M-A1c, the single-layer halves that AC-1 itself says do not break AC-1;
   - `COMPILE_FAIL`: invalid; re-edit it and re-run; never counted.
2. **Unmet criterion.** If a mutation that an AC names in its "Breaks when" is `NOT DETECTED`, that AC is
   **unmet**, whatever the reason, including "the provider cannot show it". The Developer writes it under
   `escalations` and `decisions_needed`, stops after the current mutation's restore, and does nothing
   further until the orchestrator rules. It is never recorded as a deviation and never relabelled.
3. **The one exception the story allows.** AC-16 lets the story "report that in its notes" when the
   provider's default already enables TLS 1.0 and 1.1. If D7c holds and M-D11 or M-D12 is not detected by
   the second AC-16 test (`createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem`), that
   is recorded as `NOTED-AC16` in the mutation log and in the notes. M-D11 must still be detected by the
   first AC-16 test, and M-D12 by AC-14. If either is not, rule 2 applies.
4. **Known in advance.** If the probe already shows that a named mutation cannot be detected (D7a, D7b),
   the Developer does not start Group D. It stops at I0b with `decisions_needed` (see the decision
   table).

## Working Without Commits

The working tree is the only copy of the work until the orchestrator commits. Three mechanisms protect it
and make each increment checkable.

1. **Manifest.** Each increment ends by writing `evidence/I<k>-manifest.txt`. It holds the verbatim
   output of `git status --porcelain --untracked-files=all -- $PP`, plus one line per U-063 product file:
   `git hash-object <path>` followed by the path. The next increment's start check recomputes both and
   compares them with the last manifest. Any difference means stop and report.
2. **Product-clean check.** `git diff --quiet $BASE -- $PROD` and
   `git status --porcelain -- app/src/main k9mail-vendored` (empty) run at the start and end of every
   increment, before every gate and after every mutation restore.
3. **Snapshots.** At the end of each increment, the U-063 test files are copied to
   `$WORK/snapshots/I<k>/`, outside the repository. This guards against a mistaken `git checkout`. The
   snapshots are deleted at I7 after the final gate.

**Git commands the Developer may run:** `status`, `diff`, `grep`, `ls-files`, `hash-object`, `rev-parse`,
`show <BASE>:<path>`, and `git checkout -- <exactly one production file named in the mutation log>`. The
Developer **never** runs `commit`, `add`, `stash`, `reset`, `restore`, `clean`, `checkout` with a
directory or `.`, `branch`, or `worktree`. A `git checkout -- app`, `-- .` or `-- app/src/test/...` would
silently destroy the uncommitted test files (the `DefaultTrustedSocketFactorySniTest.java` change is
tracked and uncommitted).

## Environment and Conventions (every Developer turn)

```bash
# run from the repository root, using its absolute path in the tool call's cd
unset MSYS_NO_PATHCONV            # NEVER set for ./gradlew (CLAUDE.md)
W=sdlc/artifacts/build/sprints/sprint-020/U-063; E=$W/evidence; T=$E/tools
BASE=395000956bb46cb2acc6d66e62d4b5ef933a2d90
PP="app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat"   # porcelain pathspec
PROD="app/src/main k9mail-vendored build.gradle app/build.gradle settings.gradle gradle gradle.properties"   # must never change
WORK=../u063-work                 # outside the repository; created at I0a by the Developer; deleted at I7
INIT=$T/junit-xml.init.gradle
R=app/build/test-results/testDebugUnitTest
S=app/src/test/java/com/zegoggles/smssync/mail
JDK25=$(cygpath -u "$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.home = //p' | tr -d '\r')")   # I0b only
```

- **JDK.** Gradle chooses JDK 25 through `gradle/gradle-daemon-jvm.properties`. Every gate log carries the
  `U063-DAEMON-JVM: java.version=25…` line printed by the init script. Do not export `JAVA_HOME`.
- **Tools** are created with the **Write tool** (U-061 found that a bash heredoc collapsed `\\`). Their
  full source is in [Appendix A](#appendix-a--tools-create-verbatim-with-the-write-tool). Run Python as
  `python -I -X utf8 <script>`.
- **Raw logs** go to `$WORK/logs/` and never into the repository. Every command output that becomes
  evidence is first written to `$WORK/logs/` and then copied into `$E` **only** through `sanitize.py`.
  No command redirects straight into `$E`, with two exceptions, neither of which can carry a machine path:
  - the tools' own summaries of build reports (`junit_totals.py`, `junit_cases.py`, `jacoco_pkgs.py`),
    which print only class names and numbers;
  - `git` and `grep` output over repository-relative paths or over files already in `$E`.
  The I7.1 scan re-checks all of them.
  Hand-written text in `implementation-log.md` and `mutation-log.md` follows the same rules: no e-mail
  address, host name, user name or user-profile path, and only workspace-relative paths. Quoted log
  lines are copied from the sanitised evidence files, never from raw logs (see
  [Evidence Layout and Hygiene](#evidence-layout-and-hygiene-ic-8)).
- **Gate G (per increment):**
  ```bash
  rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification > $WORK/logs/I<k>-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I<k>-gates.log
  python -I -X utf8 $T/sanitize.py $WORK/logs/I<k>-gates.log $E/I<k>-gates.txt --grep '^(BUILD |FAILURE|> Task :app:(test|jacoco)|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'
  python -I -X utf8 $T/junit_totals.py $R > $E/I<k>-junit-totals.txt
  ```
- **Targeted run TR(class, name):**
  ```bash
  rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:testDebugUnitTest --tests "<fully qualified class>" > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log
  python -I -X utf8 $T/junit_cases.py $R <SimpleClassName> > $E/<name>-cases.txt
  ```
- **Working-tree count C:**
  `git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`.
  The CLAUDE.md `HEAD` form is also recorded and stays 682 until a commit exists.
- **Text rule for new sources.** The string `@Test` appears only as an annotation, never in a comment or
  Javadoc, because the count is a text grep. Do not write `*/` patterns such as the JaCoCo glob inside
  Javadoc.
- **Short-form annotations only (C4).** This applies to the 6 new sources (A1), the SNI-test additions and the
  throwaway suites. Every annotation is written in its imported short form: `@Test`, `@Before`,
  `@After`, `@RunWith`, `@Ignore`, `@SuiteClasses`, and so on, each with an `import` line. Never use the
  package-qualified form (an at-sign followed directly by a dotted package name, such as the JUnit or
  Robolectric package before the annotation name). A qualified form would:
  - match the IC-8 e-mail pattern;
  - for the test annotation, escape the `@Test` text count, because the package would stand between the
    at-sign and `Test`.
  I7.1 checks this.
- **Shell discipline (endpoint security, 2026-11-13).** Every command runs in the foreground. There are no
  background or detached shells, no `&`, no polling or `sleep` loops, no helper agents for shell work, and
  no command that starts hundreds of processes. Every Gradle call is given the tool's maximum timeout
  (600000 ms). If a shell call fails with "bash.exe … not found", or a process is killed for no visible
  reason, **stop and report**: do not retry in another shell and do not work around it.
- **If a Gradle call exceeds the tool's time limit:**
  1. Do not re-launch it in the background.
  2. Run `./gradlew --stop` once (one process) so no orphaned build holds the lock.
  3. Re-run the same work split into one task per call, each in the foreground: `:app:assembleDebug`, then
     `:app:testDebugUnitTest`, then `:app:jacocoTestCoverageVerification`, then (I0a/I7) `:app:assembleRelease`.
     Up-to-date tasks finish quickly, so the split reaches the same state.
  4. If a single task still exceeds the limit, stop and report with the tail of its raw log, sanitised.

## Hand-off (end of every turn)

The Developer stops after each increment, appends to `$W/implementation-log.md`, and returns:

```yaml
handoff:
  increment: "I3"
  head: "<git rev-parse HEAD>"
  gate: {command: "<exact>", summary: "evidence/I3-gates.txt", exit: 0, junit: "files=<n> tests=697 failures=0 errors=0 skipped=2"}   # (A1) I3 value
  count_worktree: 697          # count C (A1)
  count_head: 682              # CLAUDE.md form; changes only after a commit
  product_paths:               # verbatim porcelain for $PP
    - "?? app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java"
    - "..."
  manifest: "evidence/I3-manifest.txt"
  escalations: []              # stop conditions hit
  decisions_needed: []         # DP-1, DP-2, AC-24 split, probe branches
  bug_requests: []             # draft texts for /amp:create-bug (orchestrator files them)
  recovered: false             # true only on a RECOVER-I6 hand-off
```

**Resuming after a lost session.** `implementation-log.md` alone is enough to resume:
- Each increment section begins with `## I<k> START <UTC time> HEAD=<sha>` and ends with
  `## I<k> END` followed by the hand-off block.
- A START with no END means the increment was interrupted. The new session:
  1. Runs the product-clean check. If a production file is dirty, see the mutation recovery rule in I6.
  2. Compares the porcelain with the last END manifest.
  3. Returns the tree to the last END manifest:
     - delete the interrupted increment's new untracked files (`rm`, explicit paths);
     - **if the interrupted increment is I2**, first restore the SNI test with its single allowed checkout,
       `git checkout -- app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java`.
       I2 inserts into that tracked file with the Edit tool, so a redo on top of a half-done I2 would
       duplicate the import and the methods. Confirm afterwards that its `git hash-object` equals the
       base blob;
     - re-compare with the last END manifest. Only when they match, redo the interrupted increment from its
       first step. Increments other than I2 create their files whole, so recreating them is safe.
  4. Writes new evidence names with a `-retry<n>` suffix.
  5. **If the interrupted increment is any part of I6**, it runs [RECOVER-I6](#recover-i6--recovery-turn-after-a-lost-i6-session)
     first, as its own turn.
  6. **Throwaway-suite sweep (C3).** From the moment I7 step 4 has started, the first action of **every**
     later turn, before any Gradle run, is:
     1. `ls $S/U063OrderedSuite*Test.java 2>/dev/null`;
     2. if a file is listed, `rm` exactly that file and record `suite-sweep: removed <name>` in the log;
     3. confirm that `git status --porcelain --untracked-files=all -- $PP` equals the I5 manifest and that
        count C is 710 (Option B: 711; SNI fallback 708 or 709) (A1). Any other difference is a stop.
     A leftover suite would make any gate run its six child classes twice and inflate the totals. The
     same sweep is required of anyone else before a gate (Notes, item 8).

## Implementation Steps

### I0a — Baseline evidence (no source change) — Turn 1

1. **Start check.**
   - `git rev-parse HEAD` must be `$BASE`, or a descendant of it that leaves `$PROD` unchanged.
   - `git diff --quiet $BASE HEAD -- $PROD` must pass.
   - `git status --porcelain --untracked-files=all -- $PP` must be empty.
   Otherwise stop. Create `$E/`, `$T/`, `$WORK/logs/`, `$WORK/snapshots/` and `$WORK/gen/`. Write all
   Appendix A tools into `$T/`.
   - **W6:** `$WORK` is outside the repository. If creating or writing to it is refused (by the permission
     sandbox or otherwise), **stop and report at this step**. Do not move it under `app/build` or the
     root `build/` (`clean` at I0a and I7 would delete it while it is in use), and do not invent another
     location. The orchestrator chooses the fallback. Its recommended fallback is a `u063-work`
     sub-folder of the Developer's own harness scratch folder, accepting two consequences:
     - that folder does not survive a new session, so a lost session between I0b and I1 means redoing
       I0b;
     - snapshots are lost on a new session.
     The chosen path is recorded in the log only as `$WORK=<orchestrator-chosen scratch folder>`, never
     as an absolute path.
2. **Counts (IC-1, IC-4).** Write to `$E/I0-count.txt`: the CLAUDE.md `HEAD` count (expected 682) and
   count C (expected 682). If either differs from 682, the targets become I0 + 26 (re-baseline). Do not
   reuse 682 and 708.
3. **Full baseline gate (IC-3), clean.**
   `rm -rf $R; ./gradlew -I "$INIT" --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease > $WORK/logs/I0-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I0-gates.log`.
   It must exit 0. Then sanitise into `$E/I0-gates.txt` (filter as gate G) and write the totals to
   `$E/I0-junit-totals.txt`. Expected: `tests=682 failures=0 errors=0 skipped=2`. Also write
   `python -I -X utf8 $T/junit_cases.py $R K9MailTransportTest BackupImapStoreTest > $E/I0-suite-children-baseline.txt`.
   This records the per-class case counts that the I7.4 ordered suites must reproduce.
4. **Coverage (IC-1, IC-5).**
   `python -I -X utf8 $T/jacoco_pkgs.py app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml > $E/I0-jacoco-packages.txt`.
   Record the `com.zegoggles.smssync.mail` and `com.zegoggles.smssync.mail.transport` lines in the log.
5. **Dependency trees (IC-1, IC-11).** For each `C` in `debugUnitTestCompileClasspath` and
   `debugUnitTestRuntimeClasspath`:
   `./gradlew -q :app:dependencies --configuration C > $WORK/logs/I0-deps-C.log 2>&1; python -I -X utf8 $T/sanitize.py $WORK/logs/I0-deps-C.log $E/I0-deps-C.txt`.
   Then run `grep -nE "org\.(bouncycastle|conscrypt)" $E/I0-deps-*.txt > $E/I0-deps-check.txt`.
   Expected: hits in the runtime tree only. If the compile tree has a hit, record it. New tests still
   import nothing from `org.conscrypt` or `org.bouncycastle` (checked at I7).
6. **IC-12:** `git grep -nE "LocalKeyStore|setKeyStoreLocation|addCertificate" -- app/src/main > $E/I0-ic12-grep.txt; echo "lines=$(wc -l < $E/I0-ic12-grep.txt)" >> $E/I0-ic12-grep.txt`
   (expected `lines=0`).
7. **Environment line (W10):** `{ java -version 2>&1 | head -1; ./gradlew --version 2>&1 | grep -E "^(Gradle|Daemon JVM|Launcher JVM)"; } > $WORK/logs/I0-env.log`,
   then `python -I -X utf8 $T/sanitize.py $WORK/logs/I0-env.log $E/I0-env.txt`. Nothing is written to
   `$E` before it is sanitised.
8. Write the I0a manifest (porcelain empty) and log entry, then hand off.

### I0b — Fixtures, probe, decisions (no committed source) — Turn 2

1. **Start check** as in [Working Without Commits](#working-without-commits).
2. **Generate the fixtures (DP-1, IC-13).** Confirm that `"$JDK25/bin/keytool"` exists and that
   `java -version` reports 25. Run `JDK25="$JDK25" bash $T/gen-fixtures.sh $WORK/gen > $WORK/logs/I0-gen.log 2>&1; echo "exit=$?" >> $WORK/logs/I0-gen.log`.
   This is 24 sequential keytool processes. It must exit 0 and print `U063-GEN done`. Then run
   `java $T/ExtractFixtures.java $WORK/gen > $WORK/gen/constants.txt 2> $WORK/gen/summary.txt`. It must
   exit 0. The program fails if any chain is not exactly [leaf, CA] or a key is not PKCS#8.
   - `summary.txt` holds the public certificate data: subject, issuer, SAN, notBefore and notAfter (UTC),
     SHA-256 fingerprint. Sanitise it into `$E/I0-fixtures-summary.txt`.
   - Check against IC-13: the valid leaves and the CAs start on or before 2025-01-01 and end after
     2100-01-01; `expired-host` ends before 2025-01-01. Record PASS or FAIL per certificate in the log.
   - `constants.txt` holds private keys and **never** enters the evidence folder.

   **Fixture layout (decided):**

   | Fixture | Key | Subject / SAN | Signed by | Validity | Used by |
   |---------|-----|---------------|-----------|----------|---------|
   | `trusted-ca` | RSA 2048, SHA256withRSA, basicConstraints CA (critical, pathlen 0), keyUsage keyCertSign+cRLSign | `CN=U-063 Test Trusted CA, O=U-063 Test Only` | self | 2019-12-01 + 36500 days (about 2119-11) | trust anchor for B and C; issuer of the server chains |
   | `untrusted-ca` | as above | `CN=U-063 Test Untrusted CA, O=U-063 Test Only` | self | as above | issuer of `untrusted-host`; never installed as an anchor |
   | `right-host` | RSA 2048 | `CN=imap.right.test`, SAN `dNSName=imap.right.test`, KU digitalSignature+keyEncipherment, EKU serverAuth, not a CA | `trusted-ca` | 2020-01-01 + 32000 days (about 2107-08) | AC-5, AC-7, AC-21 (presented) |
   | `other-host` | RSA 2048 | SAN `imap.other.test` | `trusted-ca` | as `right-host` | AC-6, AC-8; enrolled leaf for AC-20/21 |
   | `untrusted-host` | RSA 2048 | SAN `imap.right.test` | `untrusted-ca` | as `right-host` | AC-9 |
   | `expired-host` | RSA 2048 | SAN `imap.right.test` | `trusted-ca` | 2020-01-01 + 30 days (ends 2020-01-31) | AC-10 |

   The dates are fixed by `-startdate`, so they do not depend on the day of generation. The expired leaf
   lies inside the CA's window, so expiry is its only defect. A valid leaf cannot become not-yet-valid or
   expired on any test machine before 2107. RSA 2048 with SHA-256 is accepted by both SunJSSE and
   Conscrypt and by the JDK's certificate-path algorithm constraints. The password `u063-test-only` exists
   only in the throwaway PKCS12 files and as the in-memory key password in `TlsTestFixtures`. It is
   labelled test-only there.

3. **Write the throwaway probe** (untracked; deleted at step 6). Two files, both `@RunWith(RobolectricTestRunner.class)`:
   - `$S/ssl/U063ProbeFixturesTest.java`: the `constants.txt` content wrapped in
     `public final class U063ProbeFixturesTest`, with loader helpers identical to those specified for
     `TlsTestFixtures` in I1.
   - `$S/ssl/U063ProbeTest.java`: one test method per probe block P1 to P10 below.
   Every method catches `Throwable`, prints `U063-PROBE <key>=<value>` lines to `System.out`, and never
   fails, so one problem cannot hide the others. Every block that replaces global state restores it in
   `finally` and prints `restore_ok=<bool>`. Probe sockets use the same loopback, port-0, 10-second-bound
   rules as the real tests (IC-6).

   **Probe output: the exact keys and value forms (iteration 3).** The probe prints exactly these keys,
   one `U063-PROBE <key>=<value>` line each, and no others. The decision table and its completeness
   check below are written against this list.
   - Any key whose code throws prints `EXCEPTION <class>: <message>` instead of its value.
   - Booleans print `true` or `false`.
   - Lists print `null`, `[]` or `[a, b, …]` in the object's own order.

   | Block | Keys (exact names) and value forms |
   |-------|------------------------------------|
   | P1 environment | `sdk_int` (integer); `java_version` (string); `providers` (ordered `name:version` list). Then the provider name of each lookup: `prov_sslcontext_tls` (`SSLContext.getInstance("TLS")`), `prov_tmf_x509` (`javax.net.ssl.TrustManagerFactory.getInstance("X509")`), `prov_kmf_default` (`KeyManagerFactory.getInstance(getDefaultAlgorithm())`), `prov_keystore_default` (`KeyStore.getInstance(KeyStore.getDefaultType())`), `prov_certfactory_x509` (`CertificateFactory.getInstance("X.509")`), `prov_keyfactory_rsa` (`KeyFactory.getInstance("RSA")`) |
   | P2 fixtures | `fixtures_load` = `OK` once every certificate and key loads, a server `SSLContext` is built per identity (in-memory `KeyStore` of the default type, `KeyManagerFactory` default algorithm), and the substitute trust manager is built from `trusted-ca`; otherwise `EXCEPTION …` |
   | P3 host-name verifier | `hv_default_class` (class name of `HttpsURLConnection.getDefaultHostnameVerifier()`). `hv_right_on_right` and `hv_right_on_other` = the boolean result of `verify("imap.right.test", s)`, where `s` is a probe-local `SSLSession` stub returning the `right-host` / `other-host` chain |
   | P4 SNIHostName | `sni_ctor_eight_group` (`new SNIHostName("2001:0db8:0000:0000:0000:0000:0000:0001")`) and `sni_ctor_1to8` (`"1:2:3:4:5:6:7:8"`). Each is one of: `ACCEPTED`; `IAE <message>` for an `IllegalArgumentException`; `OTHER <class> <message>` for any other throwable. This is the one block where a thrown exception is a value, not `EXCEPTION` |
   | P5 SNI observable | `getSSLParameters().getServerNames()` on unconnected sockets, as a list of host-name strings: `sni_obs_dns` (default factory, `imap.right.test`); `sni_obs_ipv6` (default factory, eight-group literal); `sni_obs_pinned_implicit` (pinned factory built with `imap.right.test`) |
   | P6 Group D facts | **Lists**, for a fresh `SSLContext("TLS").init(null,null,null)` unconnected socket ("default") and the hardened socket: `default_supported_protocols`, `default_enabled_protocols`, `hardened_enabled_protocols`, `reflected_enabled_protocols` (`ENABLED_PROTOCOLS`), `default_enabled_ciphers`, `hardened_enabled_ciphers`, `reflected_enabled_ciphers` (`ENABLED_CIPHERS`), `blocked_in_default` (literal blocked names found in the default list), `fixed_suites_F` (method in I3). **Booleans:** `f_bound_known_disjoint` (F shares no name with the known or blocked baselines); `f_bound_no_with` (no F name contains `_WITH_`); `f_bound_remainder_ge2` (`hardened − F` holds at least 2 baseline known ciphers); `f_bound_baseline_in_default` (some baseline cipher is in the default list); `tls10_supported`, `tls10_default`, `tls10_hardened` and the same three for `tls11` (`TLSv1` / `TLSv1.1` in the supported, default-enabled and hardened-enabled lists); `sslv3_supported`; `sslv2hello_supported`; `cipher_order_differs` (`hardened − F` differs in order from `default − F`); `protocols_differ` (hardened enabled protocols differ in order from default enabled); `supported_minus_sslv3_equals_default_set` (the set of supported protocols minus `SSLv3` equals the default enabled set); `cipher_order_matches_reflected` (`hardened − F` equals `ENABLED_CIPHERS − F` in order); `protocol_order_matches_reflected` (hardened enabled protocols equal `ENABLED_PROTOCOLS` in order) |
   | P7 layered | Single-threaded plain loopback (bind with timeout, connect, accept). Booleans: `layered_equals_implicit_ciphers`, `layered_equals_implicit_protocols`, `pinned_implicit_equals_default`, `pinned_layered_equals_default` |
   | P8 static state | `tmf_default_tm_class` (class name, or `null` if the field is null); `tmf_field_rw` = `OK` (reflective get, set to a probe value, restore, identity verified) or `FAIL <reason>`; `localkeystore_null` (boolean: `LocalKeyStore.getInstance()` field `mKeyStore` is null); `p8_restore_ok` (boolean) |
   | P9 handshakes | Eight keys `hs_<path>_<fixture>`, with path in {`implicit`, `starttls`} and fixture in {`right`, `other`, `untrusted`, `expired`}. They run with the trust-manager substitute, plus the SAN verifier if D3a holds. Each value is one of three forms: `OK protocol=<p> suite=<s> byte=<b> server=<outcome> ms=<n>`; `REJECTED top=<class> direct_cause=<class> chain=<classes> marker=<bool> server=<outcome> ms=<n>` for an `SSLException` (`marker` = some message in the cause chain contains `was not verified against the server certificate`); `ERROR <class> ms=<n>` for any other throwable. `<outcome>` is the server's `awaitOutcome()` (`OK`, `FAILED:<class>`) or `TIMEOUT`. Also printed: `hs_implicit_right_no_verifier_swap` (same forms; **only under D3a**); `hs_<path>_<fixture>_server_variant_<n>` (same forms; **only while D10b tries variants**); `p9_restore_ok` (boolean) |
   | P10 pinned handshakes | The factory is built with `imap.right.test` and the enrolled `other-host` leaf; the client asks for `imap.right.test`. `pin_accept`: the server presents `other-host`. `pin_reject`: the server presents `right-host`. Both use the P9 forms, except that `marker` means some message in the cause chain contains `Pinned certificate mismatch`. Also `pin_accept_server_variant_<n>` and `pin_reject_server_variant_<n>` (**only while D10b tries variants**) |

4. **Run it:** TR(`com.zegoggles.smssync.mail.ssl.U063ProbeTest`, `I0-probe-run`). Then run
   `python -I -X utf8 $T/probe_extract.py $R > $WORK/logs/I0-probe.raw; python -I -X utf8 $T/sanitize.py $WORK/logs/I0-probe.raw $E/I0-probe.txt`.
   The cipher lists are public provider data. No key material is printed.
5. **Apply the decision table** below and record every outcome, with the probe key that decided it, under
   "I0 probe decisions" in the log. The orchestrator copies these into the story's Implementation Notes
   (IC-11).
6. **Delete the probe.** `rm $S/ssl/U063ProbeTest.java $S/ssl/U063ProbeFixturesTest.java`. Then
   `git status --porcelain --untracked-files=all -- $PP` must be empty, and count C must be 682.
7. Write the manifest and log, then hand off. The hand-off lists every branch taken. If AC-24, DP-1 or
   any "stop and report" branch fires, the hand-off carries it under `decisions_needed` and the Developer
   does nothing further.

#### Probe decision table

**How to read it (iteration 3).**
- **Every key matches a row.** Each key the probe prints (the list above) is matched against the rows
  below. The [completeness check](#completeness-check-probe-key--rows) shows that every key, and every
  enumerated value of every key, has at least one row.
- **Three kinds of row.**
  - *Continue* rows (marked **Continue** or **Expected**) let the work go on as written. Several can
    apply at once.
  - The single *record-only* row R0 records information and selects no branch.
  - *Stop* rows (marked **Stop and report**) end I0b. The Developer writes every matching stop row under
    `decisions_needed` and does nothing further.
- **Precedence: if a continue or record-only row and a stop row both match the same output, the stop row
  wins.** The pairs where this can happen are:
  - D3a or D3b with D3c;
  - D4a or D4b with D4c;
  - D6 or D6' with D6'';
  - D7c with D7b or D7e;
  - D10a, D10e' or D11a with D12;
  - D10a with D10c, D10d, D10f, D10g or D10h;
  - D11a with D10f or D10h;
  - D1a with D13 when a value is `EXCEPTION`.
  Continue rows never override one another, because each one continues.
- **D13 is narrow.** It fires only for:
  - a key that is missing from the output, apart from the two conditional keys under their stated
    conditions;
  - a value printed as `EXCEPTION …`;
  - a value outside the forms listed for that key;
  - one of the contradictory combinations listed in D13.
  A successful probe matches only continue rows and R0, and never reaches D13.

**Terms.** The three **controls** are implicit-right (`hs_implicit_right`, AC-5), STARTTLS-right
(`hs_starttls_right`, AC-7) and `pin_accept` (AC-20). A `REJECTED` value is a **certificate rejection** if
its `direct_cause` is a `CertificateException` (or a subclass) or its `marker` is `true`. Any other
`REJECTED` value is a **negotiation error**, for example no common protocol or cipher, or a
`handshake_failure` alert without a certificate cause.

| # | Probe outcome (keys) | Action | ACs affected |
|---|----------------------|--------|--------------|
| R0 | **Record only.** P1 `providers` and the six `prov_*` keys. P3 `hv_default_class`. P6 the nine lists (`default_supported_protocols`, `default_enabled_protocols`, `hardened_enabled_protocols`, `reflected_enabled_protocols`, `default_enabled_ciphers`, `hardened_enabled_ciphers`, `reflected_enabled_ciphers`, `blocked_in_default`, `fixed_suites_F` (its *content*; its emptiness is D6/D6')), and `sslv3_supported`, `sslv2hello_supported`, `tls10_supported`, `tls11_supported`. The `protocol`, `suite` and `byte` fields of P9/P10 `OK` values, and the `top`, `chain` and `server` fields of rejection values. | Record in the log. No decision. These feed the Javadoc (D1 note), the DP-2 bug draft and the mutation notes. Any of them becomes a stop only through D13 (an `EXCEPTION` value or a value outside its form) or through the rows that name it explicitly (D7e, D9, D10a, D12) | none |
| D1a | P1 `sdk_int` = `29` and `java_version` starts with `25` | **Continue.** The provider named in `prov_sslcontext_tls` is recorded (R0). Every Group D expectation is computed from the live provider, and each class's Javadoc names the provider observed at I0 as information, not as an assertion | none |
| D1b | P1 `sdk_int` ≠ `29`, or `java_version` does not start with `25` | **Stop and report.** E1 relies on API 29 (`robolectric.properties:8`); the project's build and test JDK is 25 (CLAUDE.md) | all |
| D2a | P2 `fixtures_load` = `OK` | **Continue** | none |
| D2 | P2 `fixtures_load` is `EXCEPTION …` | **Stop and report: DP-1 and AC-24.** No dependency is added and no build file is touched. The product owner decides | B, C, AC-20, AC-21 |
| D3a | P3 `hv_right_on_right` = `false` and `hv_right_on_other` = `false` (the JDK stub) | **Expected.** Install the test-owned verifier in Group B/C (E3). Record what this cannot prove (Android's real verifier). `hs_implicit_right_no_verifier_swap` must then be printed (D10e') | AC-5..AC-10 |
| D3b | P3 `hv_right_on_right` = `true` and `hv_right_on_other` = `false` | **Continue, with no swap.** The real verifier is the subject of the tests. The leak guard for the verifier stays. `hs_implicit_right_no_verifier_swap` must then be absent | AC-5..AC-10 |
| D3c | P3 `hv_right_on_other` = `true`; or either `hv_*` key is `EXCEPTION …` | **Stop and report.** A verifier that accepts a mismatch would make AC-6 and AC-8 meaningless; a throwing one is unknown ground. (`EXCEPTION` here is handled by D3c, not D13) | B |
| D4a | P4 both `sni_ctor_*` = `IAE …` | **Expected.** AC-1's "Breaks when" holds as written | AC-1, AC-3 |
| D4b | P4 at least one `sni_ctor_*` = `ACCEPTED`, and neither is `OTHER …` | **Continue.** With both layers removed, `setSSLParameters` is then called, so AC-1's `never()` check still fails, and AC-3 sees a server name. Record that the mutation fails through that observable instead of through an escaping exception | AC-1, AC-3 |
| D4c | P4 either `sni_ctor_*` = `OTHER …` | **Stop and report.** The safety net at `:248` catches only `IllegalArgumentException`, so in this JVM the guard alone keeps the literal from escaping. AC-1's two-layer reading would then be wrong, and the orchestrator must rule (it may also be an AC-23 candidate) | AC-1, AC-3 |
| D5a | P5 `sni_obs_dns` = `[imap.right.test]` | **Expected.** AC-4 as written | AC-3, AC-4 |
| D5b | P5 `sni_obs_dns` = `null`, `[]`, or any other list | **Stop and report.** The observable does not report the name `setSniHost` set, so AC-3 would be vacuous and AC-4 cannot pass | AC-3, AC-4 |
| D5a2 | P5 `sni_obs_ipv6` = `null` or `[]` | **Expected.** AC-3 as written | AC-3 |
| D5d | P5 `sni_obs_ipv6` is a non-empty list | **Stop and report at once.** AC-3 would fail. A server name for an IP literal is either a candidate defect (AC-23; file nothing yourself) or provider behaviour that the orchestrator must rule on | AC-3 |
| D5e | P5 `sni_obs_pinned_implicit` = `null` or `[]` | **Expected.** AC-19 as written | AC-19 |
| D5c | P5 `sni_obs_pinned_implicit` is a non-empty list | **Stop and report.** AC-19 as written cannot pass (a name added by the provider) | AC-19 |
| D6 | P6 `fixed_suites_F` = `[]` (or `null`) | **Continue.** AC-15 assertions literal | AC-15 |
| D6' | P6 `fixed_suites_F` is a non-empty list | **Continue.** AC-15 (ii)/(iii) are evaluated on the hardened list with F removed (deviation 7). F is computed live in the test. (i) stays a full-set comparison | AC-15 |
| D6b | P6 all four `f_bound_*` = `true` | **Continue** (F is bounded; W1) | AC-15 |
| D6'' | P6 any `f_bound_*` = `false` | **Stop and report.** The AC-15 order checks would be empty or meaningless | AC-15 |
| D7a' | P6 `cipher_order_differs` = `true` | **Continue.** M-D8 and M-D9 are detectable through the socket | AC-15 |
| D7a | P6 `cipher_order_differs` = `false` | **Stop and report.** M-D8 and M-D9 could not be detected, so AC-15 would be unmet in this environment ([Criterion Status Rule](#criterion-status-rule-ic-7), rule 4) | AC-15 |
| D7b' | P6 `protocols_differ` = `true` and `supported_minus_sslv3_equals_default_set` = `false` | **Continue.** M-D13 and M-D11 are detectable by the first AC-16 test | AC-16 |
| D7b | P6 `protocols_differ` = `false`, or `supported_minus_sslv3_equals_default_set` = `true` | **Stop and report.** M-D13 or M-D11 could not be detected by the first AC-16 test, so AC-16 would be unmet (rule 4) | AC-16 |
| D7c | P6 `tls10_default` = `true` and `tls11_default` = `true` | **Continue: this is the one exception the story allows.** The second AC-16 test cannot tell the factory from the provider. It is still written, and this is reported in the notes (rule 3) | AC-16 |
| D7c' | P6 `tls10_default` = `false` or `tls11_default` = `false` | **Continue.** The second AC-16 test can tell the factory from the provider for the protocol(s) the default disables | AC-16 |
| D7e' | P6 `tls10_hardened` = `tls10_supported` and `tls11_hardened` = `tls11_supported` | **Continue.** This is the behaviour the code shows (`:107`, `:116`) and the second AC-16 test asserts | AC-16 |
| D7e | P6 `tls10_hardened` ≠ `tls10_supported`, or `tls11_hardened` ≠ `tls11_supported` | **Stop and report.** It contradicts the code reading, and the second AC-16 test would fail on unmodified code | AC-16 |
| D7d' | P6 `cipher_order_matches_reflected` = `true` and `protocol_order_matches_reflected` = `true` | **Continue.** The socket shows the factory's order | AC-15, AC-16 |
| D7d | P6 either `*_order_matches_reflected` = `false` (the provider reorders) | **Stop and report.** The socket cannot show the factory's order, so AC-15 (ii)/(iii) and the AC-16 order check would measure something other than what the story says. The orchestrator rules; the plan does not move the assertions on its own | AC-15, AC-16 |
| D8a | P7 all four booleans = `true` | **Continue.** AC-17 and AC-18 as written | AC-17, AC-18 |
| D8 | P7 any of the four = `false` | **Stop and report** for the affected AC only (AC-17 or AC-18), with the arrays recorded | AC-17, AC-18 |
| D9a | P8 `tmf_field_rw` = `OK`, `localkeystore_null` = `true`, and `tmf_default_tm_class` ≠ `null` | **Continue.** E4 and E5 hold | B, C |
| D9 | P8 `tmf_field_rw` = `FAIL …`, or `localkeystore_null` = `false`, or `tmf_default_tm_class` = `null` | **Stop and report.** E4 or E5 does not hold, so no trust substitution is possible without a production change (AC-24) | B, C |
| D9r' | P8 `p8_restore_ok` = `true` and P9 `p9_restore_ok` = `true` | **Continue** | none |
| D9r | `p8_restore_ok` = `false` or `p9_restore_ok` = `false` | **Stop and report.** The probe left global state changed, so later probe values in that JVM cannot be trusted | all |
| D10a | `hs_implicit_right` and `hs_starttls_right` = `OK … server=OK`. All six other `hs_*` keys = `REJECTED` with `direct_cause` = `com.fsck.k9.mail.CertificateChainException`: `marker=true` for `hs_implicit_other` and `hs_starttls_other`, `marker=false` for the four `untrusted`/`expired` keys | **Expected.** Groups B and C as written | AC-5..AC-10 |
| D10b | A control (`hs_implicit_right`, `hs_starttls_right` or `pin_accept`) = `REJECTED` with a **negotiation error** | Try the **allowed server-side variants**, one at a time, each printed as `<key>_server_variant_<n>` for all 8 `hs_*` keys and both `pin_*` keys: (1) set the server socket's enabled protocols to all it supports; (2) under SunJSSE, also enable `SSLv2Hello` on the server; (3) build the server `SSLContext` from the other installed JSSE provider (`getInstance("TLS", "<name>")`); (4) for the STARTTLS server side, pass the loopback address string from `InetAddress.getLoopbackAddress().getHostAddress()` instead of `null` as the layered host. If a variant makes all three controls match D10a/D11a without changing any rejection outcome, **continue** with that variant adopted in `LoopbackTlsTestServer` and recorded as a deviation. **Never** change the client socket's protocols, ciphers or parameters, add retries or sleeps, set system properties, or use `@ConscryptMode`. If no variant works, **stop and report: AC-24 fires** | B, C, AC-20, AC-21 |
| D10c | Any of the six P9 rejection keys = `REJECTED` whose `direct_cause` is not a `CertificateException` | **Stop and report** before Group B/C. The story's central assertion (the `handleSslException` precondition, `ImapConnection.java:146-152`) does not hold in this JVM. That is an environment fact the orchestrator must rule on, not something to assert around | AC-6, AC-8, AC-9, AC-10 |
| D10d | Any of the six P9 rejection keys = `OK …` (accepted) | **Stop and report at once.** This is a candidate security defect (AC-23); file nothing yourself | B, C |
| D10e' | D3a holds and `hs_implicit_right_no_verifier_swap` = `REJECTED … marker=true`; **or** D3b holds and that key is absent | **Continue.** E3 is confirmed (or not applicable) | B |
| D10e | D3a holds and `hs_implicit_right_no_verifier_swap` is anything other than `REJECTED … marker=true` | **Stop and report.** It contradicts E3 | B |
| D10f | A control = `REJECTED` with a **certificate rejection** | **Stop and report.** The likely causes are a fixture defect (key usage, extended key usage, path length, dates) or a trust-manager or verifier substitution that did not take effect. Do not regenerate the fixtures or change the substitution on your own: report the probe keys, and the orchestrator decides whether I0b is redone | AC-5, AC-7, AC-20 |
| D10g | A P9 rejection key = `REJECTED` whose `direct_cause` is a `CertificateException` but **not** `com.fsck.k9.mail.CertificateChainException`; or `hs_*_other` with `marker=false`; or `hs_*_untrusted`/`hs_*_expired` with `marker=true` | **Stop and report.** The rejection did not come through `SecureX509TrustManager` (`TrustManagerFactory.java:92`), or came from the wrong check, so AC-6, AC-8, AC-9 or AC-10 cannot be asserted as written | AC-6, AC-8, AC-9, AC-10 |
| D10h | Any `hs_*` or `pin_*` key = `ERROR …` (a non-SSL throwable such as a connect failure or a read timeout) | **Stop and report.** The loopback mechanics, not TLS, failed | all handshake ACs |
| D11a | `pin_accept` = `OK … server=OK`, and `pin_reject` = `REJECTED` with `direct_cause` = `java.security.cert.CertificateException` (or a subclass) and `marker=true` | **Expected.** AC-20 and AC-21 as written | AC-20, AC-21 |
| D11 | `pin_accept` = `OK` with `server` ≠ `OK`; or `pin_reject` = `OK …`; or `pin_reject` = `REJECTED` whose `direct_cause` is not a `CertificateException` or whose `marker=false` (failures of `pin_accept` itself are D10b or D10f) | **Stop and report** for AC-20/AC-21 (part of the AC-24 split) | AC-20, AC-21 |
| D12 | Any `hs_*` or `pin_*` value with `ms` > 10000, or `server=TIMEOUT` | **Stop and report** (a hang risk the bounds did not catch) | all handshake ACs |
| D13 | **Only these.** (a) A key of the completeness table is missing from the output. The exceptions are `hs_implicit_right_no_verifier_swap`, which must be absent exactly under D3b, and the `*_server_variant_*` keys, which appear only under D10b. A variant key printed without D10b is a contradiction under (d). (b) A value printed as `EXCEPTION …` for a key whose own rows do not handle it (D3c does). (c) A value outside the forms listed for its key in the probe output table. (d) A contradictory combination: a control `OK` with `server` ≠ `OK` (P9 controls; `pin_accept` is D11); `hs_implicit_right_no_verifier_swap` present under D3b; a P6 list key whose content contradicts its own boolean (for example `fixed_suites_F` = `[]` with `f_bound_known_disjoint` = `false`) | **Stop and report** under `decisions_needed` with the probe keys. Do nothing further | as reported |

#### Completeness check: probe key → rows

Performed against the probe output table above (iteration 3). For every key, every value it can take
has a row. Keys with free-form values are covered by R0 for any well-formed value and by D13 for
`EXCEPTION` or a malformed value.

| Key | Values it can take | Rows that match |
|-----|--------------------|-----------------|
| `sdk_int` | `29` / other | D1a / D1b; `EXCEPTION`: D13 |
| `java_version` | starts `25` / other | D1a / D1b; `EXCEPTION`: D13 |
| `providers`, `prov_sslcontext_tls`, `prov_tmf_x509`, `prov_kmf_default`, `prov_keystore_default`, `prov_certfactory_x509`, `prov_keyfactory_rsa` (7) | any name or list | R0; `EXCEPTION`: D13 |
| `fixtures_load` | `OK` / `EXCEPTION` | D2a / D2 |
| `hv_default_class` | any class name | R0; `EXCEPTION`: D13 |
| `hv_right_on_right` × `hv_right_on_other` | (`false`,`false`) / (`true`,`false`) / (any, `true`) / either `EXCEPTION` | D3a / D3b / D3c / D3c (all 9 combinations covered) |
| `sni_ctor_eight_group`, `sni_ctor_1to8` | both `IAE` / some `ACCEPTED` and no `OTHER` / any `OTHER` | D4a / D4b / D4c (all 9 combinations covered) |
| `sni_obs_dns` | `[imap.right.test]` / `null`, `[]` or another list | D5a / D5b |
| `sni_obs_ipv6` | `null` or `[]` / non-empty | D5a2 / D5d |
| `sni_obs_pinned_implicit` | `null` or `[]` / non-empty | D5e / D5c |
| 8 P6 lists (all but `fixed_suites_F`) | any list | R0 |
| `fixed_suites_F` | `null` or `[]` / non-empty | D6 / D6' (content: R0) |
| `f_bound_known_disjoint`, `f_bound_no_with`, `f_bound_remainder_ge2`, `f_bound_baseline_in_default` | all `true` / any `false` | D6b / D6'' |
| `tls10_supported`, `tls11_supported` | `true` / `false` | R0, and D7e'/D7e via the comparison |
| `tls10_default` × `tls11_default` | both `true` / either `false` | D7c / D7c' |
| `tls10_hardened`, `tls11_hardened` | equal to `*_supported` / not equal | D7e' / D7e |
| `sslv3_supported`, `sslv2hello_supported` | `true` / `false` | R0 |
| `cipher_order_differs` | `true` / `false` | D7a' / D7a |
| `protocols_differ` × `supported_minus_sslv3_equals_default_set` | (`true`,`false`) / any other of the 4 | D7b' / D7b |
| `cipher_order_matches_reflected` × `protocol_order_matches_reflected` | both `true` / either `false` | D7d' / D7d |
| `layered_equals_implicit_ciphers`, `layered_equals_implicit_protocols`, `pinned_implicit_equals_default`, `pinned_layered_equals_default` | all `true` / any `false` | D8a / D8 |
| `tmf_default_tm_class` | a class name / `null` | R0 (and D9a) / D9 |
| `tmf_field_rw` | `OK` / `FAIL` | D9a / D9 |
| `localkeystore_null` | `true` / `false` | D9a / D9 |
| `p8_restore_ok`, `p9_restore_ok` | both `true` / either `false` | D9r' / D9r |
| `hs_implicit_right`, `hs_starttls_right` | `OK server=OK` / `OK server≠OK` / `REJECTED` negotiation / `REJECTED` certificate / `ERROR` | D10a / D13(d) or D12 for `TIMEOUT` / D10b / D10f / D10h |
| `hs_implicit_other`, `hs_starttls_other` | `REJECTED` chain-exception and `marker=true` / other `direct_cause` / `marker=false` / `OK` / `ERROR` | D10a / D10c or D10g / D10g / D10d / D10h |
| `hs_{implicit,starttls}_{untrusted,expired}` (4) | `REJECTED` chain-exception and `marker=false` / other `direct_cause` / `marker=true` / `OK` / `ERROR` | D10a / D10c or D10g / D10g / D10d / D10h |
| `hs_implicit_right_no_verifier_swap` | (D3a) `REJECTED marker=true` / (D3a) anything else / (D3b) absent / (D3b) present | D10e' / D10e / D10e' / D13(d) |
| `hs_*_server_variant_<n>`, `pin_*_server_variant_<n>` | any value, only under D10b | D10b (the variant is adopted only if its values meet D10a/D11a) |
| `pin_accept` | `OK server=OK` / `OK server≠OK` / `REJECTED` negotiation / `REJECTED` certificate / `ERROR` | D11a / D11 / D10b / D10f / D10h |
| `pin_reject` | `REJECTED`, `CertificateException` and `marker=true` / other `REJECTED` / `OK` / `ERROR` | D11a / D11 / D11 / D10h |
| every `ms` field | ≤ 10000 / > 10000 | (no stop) / D12 |
| every `server` field of a rejection | any `<outcome>` | R0 (`TIMEOUT`: D12) |

**Result:** the probe prints **63 unconditional keys**:

| Block | Keys |
|-------|------|
| P1 | 9 |
| P2 | 1 |
| P3 | 3 |
| P4 | 2 |
| P5 | 3 |
| P6 | 26 (9 lists, 17 booleans) |
| P7 | 4 |
| P8 | 4 |
| P9 | 9 (8 `hs_*` and `p9_restore_ok`) |
| P10 | 2 |

It also prints two kinds of conditional key:
- `hs_implicit_right_no_verifier_swap`, only under D3a;
- `<key>_server_variant_<n>` for the 8 `hs_*` and 2 `pin_*` keys, at most 4 variants each, only under
  D10b.

Every key appears in the table above. No enumerated value of any key lacks a row. Every multi-key
condition (D3, D4, D7b, D7c, D7d, D8, D9, D9r, D10a, D11a) is checked against all its combinations. The
keys that select no branch are exactly those in R0.

**When AC-24 fires (D2, D9, D10b without a working variant, or the orchestrator's ruling on D10c, D10f, D10g or D11).**
The Developer stops at the end of I0b and reports. If the orchestrator confirms the split:
- The story keeps Groups A and D and AC-18/AC-19, as I2, I3 and a reduced I5 with 3 tests.
- I1 shrinks to nothing, since no fixtures or server are needed and none is committed. The `$WORK/gen`
  material is deleted.
- I4 is dropped. I6 covers only the kept groups.
- Expected count: 682 + 4 + 9 + 3 = 698.
- Groups B and C and AC-20/AC-21 go to a second story, to be written by the Requirements Analyst through
  the orchestrator.

**What is known now and what only the probe can tell.** Known from reading:
- The android-all jar has no `javax.net` classes, so `HttpsURLConnection` and `SNIHostName` are the JDK's.
- Robolectric 4.15.1 can insert Conscrypt as the first provider (`ConscryptMode`) and ships
  Conscrypt 2.5.2 and BouncyCastle on the test runtime.
- No existing test changes the provider mode or JSSE global state.
- The static `ENABLED_*` lists are therefore computed under the same provider that later creates the
  sockets.

Unknown until the probe:
- whether Conscrypt is actually active (the mode's default is not visible in the jar);
- whether the JDK verifier stub is what `TrustManagerFactory` sees;
- whether the hardened lists negotiate (Conscrypt should; SunJSSE has the `SSLv2Hello` question);
- the exact exception wrapping;
- whether Conscrypt prepends fixed TLS 1.3 suites.

### I1 — Fixtures and the loopback server helper (no `@Test`) — Turn 3

1. Start check (I0b manifest: empty porcelain). `$WORK/gen/constants.txt` and `summary.txt` must exist.
   If the folder was lost, redo I0b (new keys, new probe run). Never mix material from two generations.
2. Create `$S/ssl/TlsTestFixtures.java` (package `com.zegoggles.smssync.mail.ssl`, `public final`,
   private constructor). It contains:
   - **Javadoc (AC-22, IC-13).** It says the class is test-only and the keys are throwaway, generated
     offline for U-063 and never used anywhere else. It gives the exact keytool command lines from
     `gen-fixtures.sh`, with `<gen-dir>` in place of the path, and the ExtractFixtures step. It lists
     every subject, SAN, notBefore and notAfter, and SHA-256 fingerprint, copied from `summary.txt`. It
     states that the password literal is test-only.
   - **The Base64 constants from `constants.txt`, byte for byte:** `TRUSTED_CA_CERT`,
     `UNTRUSTED_CA_CERT`, and `<ID>_CERT` plus `<ID>_KEY_PKCS8` for `RIGHT_HOST`, `OTHER_HOST`,
     `UNTRUSTED_HOST` and `EXPIRED_HOST`. Also the host names `RIGHT_HOST_NAME = "imap.right.test"` and
     `OTHER_HOST_NAME = "imap.other.test"`.
   - **Helpers.**
     - `certificate(String b64)`: `CertificateFactory.getInstance("X.509")`.
     - `privateKey(String b64)`: `KeyFactory.getInstance("RSA")` with a `PKCS8EncodedKeySpec`.
     - `serverContext(String leafCertB64, String keyB64, String issuerCertB64)`: a `KeyStore` of
       `KeyStore.getDefaultType()`, `load(null, null)`, one key entry with chain [leaf, issuer] and
       password `TEST_ONLY_KEY_PASSWORD`; `KeyManagerFactory` with the default algorithm; then
       `SSLContext.getInstance("TLS").init(kms, null, null)`. If D10b adopted a server variant, apply it
       here or in the server.
     - `trustManagerFor(String... anchorCertB64)`: an in-memory `KeyStore` with certificate entries only;
       `javax.net.ssl.TrustManagerFactory.getInstance("X509")`, `init(ks)`; return the first
       `X509TrustManager` (deviation 8).
     - `chain(String... certB64)`: returns `X509Certificate[]`.

   **Self-check:**
   `grep -c "public static final String .*_\(CERT\|KEY_PKCS8\)" $S/ssl/TlsTestFixtures.java` gives 10.
   `diff <(grep -o '"[A-Za-z0-9+/=]*"' $WORK/gen/constants.txt) <(grep -o '"[A-Za-z0-9+/=]*"' $S/ssl/TlsTestFixtures.java | head -n $(grep -c '"' $WORK/gen/constants.txt))`
   is empty. If the class has other quoted strings before the constants, place the constants first or
   compare by name.
3. Create `$S/ssl/LoopbackTlsTestServer.java` (`public final`, `AutoCloseable`). The Javadoc says it is
   test-only (AC-22). **(A1) The listening socket and the server task follow V5 (A1.b) in both modes;
   A1.b overrides the IMPLICIT_TLS and STARTTLS lines below, and the Javadoc carries A1.b's one-sentence
   reason.** API:
   - `enum Mode { IMPLICIT_TLS, STARTTLS }`; `TIMEOUT_MILLIS = 10_000`; `PAYLOAD = 0x2A`;
     `GREETING = "* OK U-063 loopback test server"`.
   - `static LoopbackTlsTestServer start(Mode, SSLContext serverContext)`.
     - IMPLICIT_TLS binds `serverContext.getServerSocketFactory().createServerSocket(0, 1, InetAddress.getLoopbackAddress())`.
     - STARTTLS binds `new ServerSocket(0, 1, InetAddress.getLoopbackAddress())`.
     - `setSoTimeout(TIMEOUT_MILLIS)`.
     - One single-thread executor whose thread factory makes a **daemon** thread named
       `u063-loopback-test-server`. It submits one task. The task accepts once and sets the read timeout.
       In STARTTLS mode it writes `GREETING + "\r\n"`, then layers
       `serverContext.getSocketFactory().createSocket(plain, null, plain.getPort(), true)` (or the D10b
       variant) with `setUseClientMode(false)`. It calls `startHandshake()`, writes `PAYLOAD`, flushes and
       closes. It returns `"OK"`, or `"FAILED:" + e.getClass().getName()` for any `Throwable`. The task
       never throws.
   - `int port()`.
   - `String awaitOutcome()`: `future.get(TIMEOUT_MILLIS, MILLISECONDS)`.
   - `close()`: closes the accepted and server sockets, then `executor.shutdownNow()`. Idempotent; never
     throws.
   - Client helpers:
     - `static void connect(Socket s, int port)` connects to
       `new InetSocketAddress(InetAddress.getLoopbackAddress(), port)` with `TIMEOUT_MILLIS` and sets the
       read timeout.
     - `static Socket connectPlain(int port)`.
     - `static ServerSocket bindPlainLoopback()` returns `new ServerSocket(0, 1, InetAddress.getLoopbackAddress())`
       with `setSoTimeout(TIMEOUT_MILLIS)` already set. AC-17 and AC-18 use it, so their single-threaded
       `accept()` is bounded too (W7, IC-6).
     - `static String readLine(Socket s)` is **unbuffered** (W9). It reads one byte at a time from the raw
       `s.getInputStream()` until `\n`, at most 256 bytes, and fails on end of stream. It uses no
       `BufferedReader`, `BufferedInputStream` or `Scanner`, because a buffered read could consume the
       server's first TLS bytes that the layered socket must see. The Javadoc says so, so that a later
       edit keeps it.
   - No `Thread.sleep`, polling, `localhost`, `"127.0.0.1"`, `getByName` or host-name `InetSocketAddress`
     (IC-6).
4. **Helper smoke test (throwaway).** Write `$S/ssl/U063HelperSmokeTest.java` with three methods that
   print and never fail. Using `TlsTestFixtures` and `LoopbackTlsTestServer`, it runs:
   - the implicit `right-host` control,
   - the STARTTLS `other-host` rejection, with the substitute trust manager and verifier installed and
     restored in `finally`,
   - the pinned acceptance, with the factory built with host `imap.right.test`.
   Run TR(`…ssl.U063HelperSmokeTest`, `I1-smoke`), then
   `python -I -X utf8 $T/probe_extract.py $R > $WORK/logs/I1-smoke.raw; python -I -X utf8 $T/sanitize.py $WORK/logs/I1-smoke.raw $E/I1-smoke.txt`.
   The outcomes must match P9/P10. Copy the smoke source to `$T/U063HelperSmokeTest.java.txt` (it holds
   no key material). Then `rm` the smoke test.
5. **Delete the generated material (IC-13: no private key outside the fixtures):** `rm -rf $WORK/gen`.
6. Gate G. Count C = 682 (no `@Test` added). Run the end check. The porcelain must be exactly:
   `?? app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java` and
   `?? app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java`. Then manifest, snapshot,
   log, hand-off.

### I2 — Group A, SNI for an eight-group IPv6 literal (A1: 2 tests, AC-1 and AC-2) — Turn 4

(A1) AC-3 and AC-4 are **not** added to this file. They are created in the JDK-provider class at I2b
(A1.c). In step 1 add no `RuntimeEnvironment` import, because AC-1 and AC-2 do not need it. In step 2
skip the AC-3 and AC-4 bullets. The section comment names AC-1 and AC-2 and points to the JDK-provider
class for AC-3 and AC-4.

File: `$S/ssl/DefaultTrustedSocketFactorySniTest.java` (CRLF in the working tree; additions only).

1. Add one import line, `import org.robolectric.RuntimeEnvironment;`, after line 5. Use fully qualified
   names for everything else, as the file's own helper does.
2. Insert a new block **between the end of `setSniViaSSLParameters_blankHostname_doesNotThrowAndSkipsSNI`
   (line 239) and the helper section comment (line 241)**. It starts with a section comment that states
   what Group A can prove, what it cannot prove, and the mutation that breaks each test (AC-22). Then add:
   - `setSniViaSSLParameters_ipv6EightGroupLiteral_doesNotThrowAndSkipsSNI` (AC-1): mock `SSLSocket` with
     `getSSLParameters()` returning a new `SSLParameters`; call with
     `2001:0db8:0000:0000:0000:0000:0000:0001`; `verify(mockSocket, never()).setSSLParameters(any())`.
   - `isIpOrBlankHostname_ipv6EightGroupLiteral_isTrue` (AC-2): reflect
     `getDeclaredMethod("isIpOrBlankHostname", String.class)`, `setAccessible(true)`; assert `true` for
     the literal and for `1:2:3:4:5:6:7:8`.
   - `createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset` (AC-3): build
     `new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication())`, then
     `createSocket(null, literal, 993, null)`. Assert `isInstanceOf(SSLSocket.class)` and that
     `getSSLParameters().getServerNames()` is null or empty. Close the socket in `finally`. The socket is
     never connected.
   - `createSocket_dnsHost_setsServerNameOnRealSocket` (AC-4): with `imap.right.test`, assert exactly one
     server name, an `SNIHostName` whose `getAsciiName()` is `imap.right.test`. Close in `finally`.
3. **Additions-only check (IC-2, IC-10):**
   `git diff -U0 -- $S/ssl/DefaultTrustedSocketFactorySniTest.java | grep -E '^-[^-]' | wc -l` gives 0.
   `git diff --numstat -- <file>` shows 0 deleted lines.
4. TR(`com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactorySniTest`, `I2-sni`): 12 PASS
   (10 existing + 2) (A1).
5. Gate G: tests = 684, failures 0, skipped 2. Count C = 684 (A1). End check: the I1 lines plus
   ` M app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java`.
   Manifest, snapshot, log, hand-off.

### I2b — JDK-provider class (A1) — new turn, between I2 and I3

(A1) Create `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`
exactly as A1.c specifies, with guards A1-G1 and A1-G3 in `@Before` (A1.e). Run the A1.d pre-check and
take its branch. Then run gate G: count 688 (fallback 686), skipped 2. The end check adds
`?? app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`.
Manifest, snapshot, log, hand-off. A guard failure is a stop and report (A1.e).

### I3 — Group D, hardening (9 tests) — Turn 5

New file `$S/ssl/DefaultTrustedSocketFactoryHardeningTest.java`, `@RunWith(RobolectricTestRunner.class)`.
Its Javadoc covers AC-22: what it proves, the "cannot prove" list (device provider, wire, `null`
branches), and the mutations. It also names the provider observed at I0 as information.

**Shared helpers in the class.**
- Reflective `static String[] constant(String name)`.
- `static String[] reorder(String[] in, String[] known, String[] blocked)` via
  `getDeclaredMethod("reorder", String[].class, String[].class, String[].class)`.
- `defaultSocket()` from a fresh `SSLContext.getInstance("TLS")`, `init(null, null, null)`,
  `getSocketFactory().createSocket()`.
- `hardenedSocket()` from `new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()).createSocket(null, "imap.right.test", 993, null)`.
- `providerFixedSuites()`: take a fresh default socket. Pick the first entry of the literal
  `KNOWN_CIPHERS_BASELINE` that is in its enabled list, call `s0`. If there is none, fail with the
  message "no baseline cipher in the provider default list (D6'')". `setEnabledCipherSuites(new String[]{s0})`,
  read back the enabled suites, remove `s0`. The rest is F. Under SunJSSE F is expected to be empty.
- **F bounds (W1), asserted in the AC-15 test before (ii) and (iii), each with a failure message naming D6'':**
  - F has no name in common with `KNOWN_CIPHERS_BASELINE` or `BLOCKED_CIPHERS_BASELINE`;
  - no name in F contains `_WITH_`. The suites a provider may fix are TLS 1.3 suites such as
    `TLS_AES_128_GCM_SHA256` and signalling values such as `TLS_EMPTY_RENEGOTIATION_INFO_SCSV`, none of
    which has `_WITH_`. Every configurable TLS 1.2-or-earlier suite, which is what the reflected
    `ENABLED_CIPHERS` order is about, does have it. So F cannot swallow any suite the factory actually
    orders. This is the operational form of "F is disjoint from the `ENABLED_CIPHERS` entries the
    provider accepted";
  - `hardened − F` holds at least 2 names from `KNOWN_CIPHERS_BASELINE`, so (ii) and (iii) can never pass
    on an empty or single-element remainder.
  A provider that ignored `setEnabledCipherSuites` would make F "everything except `s0`". These bounds
  turn that into a failure, not an empty pass.
- Every socket is closed in `finally`.
- Literal baselines: `KNOWN_CIPHERS_BASELINE` (the 24 names of `:38-61`, in order),
  `BLOCKED_CIPHERS_BASELINE` (the 16 names of `:65-80`), `KNOWN_PROTOCOLS_BASELINE = {"TLSv1.2", "TLSv1.1", "TLSv1"}`,
  `BLOCKED_PROTOCOLS_BASELINE = {"SSLv3"}`.

| Test | AC | Assertions |
|------|----|-----------|
| `reorder_removesEveryBlockedItem` | 11 | Input: the 16 literal blocked names, `TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384`, `TLS_RSA_WITH_AES_128_CBC_SHA` and `TLS_U063_UNKNOWN_SUITE`, with the reflected `ORDERED_KNOWN_CIPHERS` and `BLACKLISTED_CIPHERS`. No literal blocked name is in the result, and the 3 others are. Then `{SSLv3, TLSv1.2, X}` with the reflected protocol constants: `SSLv3` is absent, `TLSv1.2` and `X` are present |
| `reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder` | 12 | Input `[K24, U1, K1, K20, U2, K2]`, where Kn is the n-th literal known cipher. K1 and K2 are adjacent in the list, so a swap of `:38`/`:39` is visible. Expect `[K1, K2, K20, K24, U1, U2]`. Protocols `{TLSv1, X, TLSv1.2, Y, TLSv1.1}` give `TLSv1.2, TLSv1.1, TLSv1, X, Y` |
| `reorder_withNullBlocklistAndEmptyInput_behavesAsRecorded` | 13 | `reorder({"A","B"}, known, null)` gives `{"A","B"}`; `reorder({}, known, blocked)` gives `{}` |
| `cipherConstants_matchRecordedBaseline` | 14 | `ORDERED_KNOWN_CIPHERS` equals the 24-name literal in order; `BLACKLISTED_CIPHERS` equals the 16-name literal in order |
| `protocolConstants_matchRecordedBaseline` | 14 | `ORDERED_KNOWN_PROTOCOLS` = `{TLSv1.2, TLSv1.1, TLSv1}`; `BLACKLISTED_PROTOCOLS` = `{SSLv3}` |
| `createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` | 15 | (i) `set(hardened)` equals `set(default)` minus the literal blocked names. (ii) The known ciphers present in `hardened − F` appear in literal order. (iii) In `hardened − F`, every known cipher precedes every unknown one. F is empty unless D6' holds, and is bounded as above (W1). D7d is a stop at I0b, so the order assertions are always on the socket |
| `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder` | 16 | `SSLv3` is not enabled. `set(enabled)` = `set(supported) − {SSLv3}`. Among the protocols present, `TLSv1.2` comes before `TLSv1.1`, which comes before `TLSv1`, and all three come before any other protocol |
| `createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem` | 16 | For each of `TLSv1` and `TLSv1.1`: enabled if and only if it is in the provider's supported list. The comment names DP-2 and the bug id once it exists |
| `createSocket_layeredOverPlainSocket_appliesSameHardening` | 17 | Single-threaded: `LoopbackTlsTestServer.bindPlainLoopback()` (read timeout set, so `accept()` is bounded), `connectPlain`, `accept()` (the backlog completes the connect). Then `createSocket(plain, "imap.right.test", port, null)`. Its enabled cipher suites and enabled protocols are `Arrays.equals` to the implicit hardened socket's. No handshake. Close all in `finally` |

1. TR(`…ssl.DefaultTrustedSocketFactoryHardeningTest`, `I3-hardening`): 9 PASS.
2. **DP-2 draft (AC-23).** Record the observed `tls10_*`/`tls11_*` facts and the second AC-16 test's
   result in the log under "Bug draft: legacy protocols". Use the template in [DP-2](#dp-2-tls-10-and-11-stay-enabled).
   Put the draft in `bug_requests` in this hand-off, so the orchestrator can file it early. Under
   Option A (the default) the test stays passing.
3. Gate G: 697 tests, skipped 2. Count C = 697 (A1; fallback 695).
   (A1) The class's `@Before` carries isolation guard A1-G2 (A1.e). The DP-2 draft follows A1.h item 4.
   The Javadoc carries the "After the CR-009 move" paragraph and RFC 8996 at the legacy-protocol test
   (A1.h). End check (+ `?? …/ssl/DefaultTrustedSocketFactoryHardeningTest.java`).
   Manifest, snapshot, log, hand-off.

### I4 — Groups B and C, handshakes (8 tests) — Turn 6

New file `$S/ssl/DefaultTrustedSocketFactoryHandshakeTest.java`, `@RunWith(RobolectricTestRunner.class)`.
Its Javadoc covers AC-22, using the B and C rows of the story's "can and cannot prove" table. It states
plainly that the trust anchor is a test CA (E4) and, under D3a, that host-name semantics come from a
test-owned verifier (E3).

**How the tests reach the code under test (decided).** The tests call
`DefaultTrustedSocketFactory.createSocket` directly, in the two call shapes of `ImapConnection`:
- implicit TLS: `createSocket(null, host, port, null)`, then `connect` and `startHandshake()`, as at
  `:209` with the handshake of `:119`;
- STARTTLS: read the greeting on a plain socket, then `createSocket(plainSocket, host, port, null)` and
  `startHandshake()`, as at `:315` with the handshake of `:324`.

They do **not** drive `ImapConnection`. That is out of the story's scope (Out of scope, bullet 4), and it
is also impractical:
- `open()` resolves the host by DNS (`:186`), which the reserved `.test` names cannot pass without
  machine-level changes.
- `startTLS` is private (`:308`).
- `adjustDNSCacheTTL` changes JVM-wide security properties (`:171-183`).

What the direct calls cannot show is that `ImapConnection` itself calls the factory on both paths (T6,
out of scope). The `handleSslException` mapping is covered through its precondition only: an
`SSLException` whose direct cause is a `CertificateException`.

**Global state (IC-9), in `@Before` and `@After`.**
- `@Before` captures `originalTm` from `TrustManagerFactory.class.getDeclaredField("defaultTrustManager")`
  (reading it initialises the class), and captures `originalVerifier`.
- **Leak guards:**
  - Fail with the message "TrustManagerFactory.defaultTrustManager is null: production static init
    failed; contradicts probe P8 (stop and report)" if `originalTm == null` (W8). Do the same if
    `originalVerifier == null`. Neither guard dereferences a null.
  - Fail if any of `originalTm.getAcceptedIssuers()` has a subject containing `U-063`. A leaked
    substitute trusts only the U-063 CA.
  - Fail if `originalVerifier.getClass().getName()` contains `SanDnsHostnameTestVerifier`.
- It then sets the field to `TlsTestFixtures.trustManagerFor(TRUSTED_CA_CERT)`. Under D3a it calls
  `HttpsURLConnection.setDefaultHostnameVerifier(new SanDnsHostnameTestVerifier())`.
- `@After`, in `finally` blocks:
  - close the client sockets and `server.close()`;
  - set the field back to `originalTm` and the verifier back to `originalVerifier`;
  - assert that `field.get(null) == originalTm` and `HttpsURLConnection.getDefaultHostnameVerifier() == originalVerifier`
    (identity).
- `SanDnsHostnameTestVerifier` is a nested static class. It returns `true` only when a `dNSName` (type 2)
  SAN entry of `session.getPeerCertificates()[0]` equals the host, compared case-insensitively with
  `Locale.ROOT`. There is no wildcard handling, no CN fallback and no IP entry. An empty array or any
  exception gives `false`.

**Assertion helper `assertRejectedByCertificateCheck(SSLException e, boolean hostnameMarkerExpected)`:**
- `e.getCause()` is an instance of `CertificateException`, and of `com.fsck.k9.mail.CertificateChainException`.
- Walk the cause chain (at most 20 steps): a message containing `was not verified against the server certificate`
  is present if and only if `hostnameMarkerExpected`.
- Nothing below `CertificateException` is asserted (story, Test mechanics).
- Each rejection test is `try { client.startHandshake(); fail(...); } catch (SSLException e) { … }`, so
  any other `IOException` fails the test.

| Test | AC | Server mode / fixture | Expected |
|------|----|-----------------------|----------|
| `implicitTls_rightHost_handshakeCompletes` | 5 | IMPLICIT / `right-host` | handshake completes; `getInputStream().read() == PAYLOAD`; `awaitOutcome()` is `OK` |
| `implicitTls_otherHost_rejectedByHostnameCheck` | 6 | IMPLICIT / `other-host` | rejection, marker present |
| `startTls_rightHost_handshakeCompletes` | 7 | STARTTLS / `right-host` | `readLine` = greeting, then handshake completes, byte read, `OK` |
| `startTls_otherHost_rejectedByHostnameCheck` | 8 | STARTTLS / `other-host` | rejection, marker present |
| `implicitTls_untrustedChain_rejected` | 9 | IMPLICIT / `untrusted-host` | rejection, marker absent |
| `startTls_untrustedChain_rejected` | 9 | STARTTLS / `untrusted-host` | rejection, marker absent |
| `implicitTls_expiredChain_rejected` | 10 | IMPLICIT / `expired-host` | rejection, marker absent |
| `startTls_expiredChain_rejected` | 10 | STARTTLS / `expired-host` | rejection, marker absent |

No timeout annotation or JUnit `Timeout` rule is used. Both run the body on another thread, which breaks
Robolectric's main-thread model. The bounds are the socket and `Future` timeouts.

1. TR(`…ssl.DefaultTrustedSocketFactoryHandshakeTest`, `I4-handshake`): 8 PASS.
2. **IC-6 greps on the new files so far:**
   `git grep --untracked -nE "Thread\.sleep|getByName|getAllByName|\"localhost\"|\"127\.0\.0\.1\"|new InetSocketAddress\(\"" -- $S > $E/I4-ic6-grep.txt`
   must be empty.
3. Gate G: 705 tests, skipped 2. Count C = 705 (A1; fallback 703).
   (A1) D3b holds, so no verifier is swapped: `@Before` sets only the trust manager, and the verifier
   leak guard stays. `@Before` also carries isolation guard A1-G2. The Javadoc carries A1.h items 1 to 3
   (fresh TLS context per call, TLS 1.3 only, RFC 5280 and RFC 6125 section 6, the "After the CR-009
   move" paragraph). Controls expect `awaitOutcome()` = `OK` under V5. End check (+ `?? …/ssl/DefaultTrustedSocketFactoryHandshakeTest.java`).
   Manifest, snapshot, log, hand-off.

### I5 — Group E, the pinned factory (5 tests) — Turn 7

New file `$S/PinnedCertificateSocketFactoryCreateSocketTest.java` (package `com.zegoggles.smssync.mail`),
`@RunWith(RobolectricTestRunner.class)`. Its Javadoc covers AC-22, using row E and Q7.g. It never calls
`getTrustManagerForTesting()` (IC-10). It changes no global state, so it needs no leak guard.
`@After` closes the server and sockets.

**Every `PinnedCertificateSocketFactory` in this class is constructed with host `"imap.right.test"`**
(the host the client asks for), whatever certificate is enrolled (W3). The constructor host is unused
today (`:67-72`). Mutation M-E3 compares that host with the presented certificate's SAN. A factory
built with `"imap.other.test"` would hide M-E3 and produce a spurious finding.

| Test | AC | Set-up | Assertions |
|------|----|--------|-----------|
| `createSocket_implicit_leavesProviderDefaultsUntouched` | 18 | factory `(app, "imap.right.test", certificate(OTHER_HOST_CERT))`; `createSocket(null, "imap.right.test", 993, null)` | cipher suites and protocols `Arrays.equals` to a fresh default socket's |
| `createSocket_layered_leavesProviderDefaultsUntouched` | 18 | same factory; single-threaded plain loopback with `bindPlainLoopback()` as in AC-17 | same |
| `createSocket_implicit_setsNoServerNames` | 19 | as the first test, unconnected | `getServerNames()` is null or empty. Measured before any connect. The layered path is deliberately not asserted |
| `handshake_enrolledCertificateForDifferentHostName_isAccepted` | 20 | factory `(app, "imap.right.test", certificate(OTHER_HOST_CERT))`; IMPLICIT server with `other-host`; client asks `imap.right.test`; no verifier installed | handshake completes, byte read, `OK` |
| `handshake_certificateOtherThanEnrolled_isRejected` | 21 | factory `(app, "imap.right.test", certificate(OTHER_HOST_CERT))`; server presents `right-host` | `SSLException` whose direct cause is a `CertificateException` with message containing `Pinned certificate mismatch` |

1. TR(`com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest`, `I5-pinned`): 5 PASS.
   Also TR(`…mail.PinnedCertificateSocketFactoryTest`, `I5-pinned-existing`): 8 PASS, and the file is
   unchanged (`git diff --quiet -- <file>`).
2. Gate G: **710** tests, failures 0, skipped 2. Count C = 710 (A1; fallback 708). The Javadoc carries
   A1.h items 1 to 3. End check (+ `?? app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java`).
   Manifest, snapshot, log, hand-off.

### I6 — Mutation evidence (IC-7) — Turns 8, 9 and 10

**Protocol (applies to every mutation, no exceptions).**

1. Before a mutation:
   - the product-clean check passes;
   - `git hash-object <file>` equals `git rev-parse $BASE:<file>` (with autocrlf, `hash-object` applies the
     clean filter, so an unmodified CRLF file hashes to the base blob);
   - the Developer appends to `$E/mutation-log.md`:
     `### M-xx APPLYING <UTC time>`, then `file: <path>`, then `edit: <exact edit from the table>` (write-ahead).
2. Apply the single edit with the **Edit tool**: no `sed`, no scripts, one file, one mutation.
3. TR(the affected class, `mutations/M-xx`). Before it, `mkdir -p $E/mutations`.
4. Record in the log, appending each item:
   - `run_exit`;
   - the class's `FAIL`/`ERROR` test names with their failure type (from `junit_cases.py`);
   - `expected: <AC tests>`;
   - `verdict`, one of those defined in the [Criterion Status Rule](#criterion-status-rule-ic-7): `DETECTED`,
     `NOT DETECTED`, `BY-DESIGN` (M-A1g and M-A1c only) or `COMPILE_FAIL`. `NOTED-AC16` is an additional
     note for the second AC-16 test only, under rule 3.
   - A `NOT DETECTED` verdict marks the named AC **unmet**. After step 5 (restore) the Developer stops
     the turn and hands off with the AC under `escalations` and `decisions_needed` (rule 2). It is never
     recorded as a deviation.
5. Restore: `git checkout -- <that one file>`. Then `git hash-object <file>` must equal the base blob, and
   `git status --porcelain -- app/src/main k9mail-vendored` must be empty. Append
   `restored: <UTC time> hash-ok porcelain-empty`.
6. Only then start the next mutation. **No mutation is ever committed. No gate runs while a mutation is
   applied.**

**Recovery after an interrupted session:** always through [RECOVER-I6](#recover-i6--recovery-turn-after-a-lost-i6-session),
as its own turn, before anything else touches the tree.

#### RECOVER-I6 — recovery turn after a lost I6 session

**Trigger.** Any lost session during I6a, I6b or I6c: a START with no END, or a mutation-log entry with
`APPLYING` and no `restored` line. Until this turn has finished, **no other agent and no gate may act on
the tree**. That covers Code Review, QA, a gate run, a commit and another Developer increment. The
orchestrator enforces this (Notes, item 8).

**Steps** (one turn; no Gradle run until step 5):
1. Read the last entry of `$E/mutation-log.md` and note its file `F` (or none).
2. For each of the three mutable files (DTSF, TMF, PCSF), compare `git hash-object <file>` with
   `git rev-parse $BASE:<file>`. Run the product-clean check (`git diff --quiet $BASE -- $PROD` and
   `git status --porcelain -- app/src/main k9mail-vendored`). Write all of these to
   `$E/RECOVER-I6-<n>.txt` (repository-relative paths and hashes only).
3. Decide:
   - **(a) Clean, all three hashes equal the base blobs, and the last entry has no `restored` line.** The
     session died after the restore and before the log line, or before the edit. Append
     `restored: <UTC time> recovered-clean`, mark the entry `INTERRUPTED`, and go to step 4.
   - **(a') Clean, all three hashes equal the base blobs, and the last entry already has its `restored`
     line (or the log has no mutation entry yet).** The session died between mutations, or during a
     sub-turn's baseline run or closing gate. **Nothing to recover:** append
     `recovery: <UTC time> nothing-to-recover` and go to step 4. The next turn continues with the next
     mutation of the sub-turn, or repeats the sub-turn's closing baseline and gate if the loss came after
     the last mutation (C2).
   - **(b) Exactly `F` is dirty (its hash differs; everything else is clean).** Run
     `git checkout -- F`, re-check its hash equals the base blob and the porcelain is empty, append
     `restored: <UTC time> recovered-dirty`, mark `INTERRUPTED`, and go to step 4.
   - **(c) Anything else.** That is a dirty file other than `F`, a dirty file when the log names no
     mutation, a hash mismatch after (b), or a change outside the three files. **Stop and report**,
     restore nothing, and hand off with `escalations`.
4. Compare the U-063 test files with the last END manifest. Any difference is a stop.
5. Run gate G on the recovered tree; it must give 710, failures 0 (A1; Option B 711 with 5 skipped; SNI
   fallback 708 or 709).
6. Hand off with `recovered: true`. After (a) or (b), the next turn redoes the interrupted mutation from
   step 1 of the protocol, and then continues the sub-turn. After (a'), it continues as stated there.

**Each sub-turn starts with an unmutated TR of each class it will mutate (baseline), all passing.** (A1)
The baseline is one Gradle invocation listing all of that sub-turn's classes (A1.f). It
also ends with that baseline TR again, plus gate G, to show the restored tree is green.

`DTSF` = `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`;
`TMF` = `…/ssl/TrustManagerFactory.java`; `PCSF` = `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`.
Edits are written against base line numbers. Locate each by its text and use the number only to confirm.

#### I6a — Group A (Sni test) and Group D part 1 (Hardening test) — Turn 8

| ID | File | Exact edit | Expected failures |
|----|------|-----------|-------------------|
| M-A1 | DTSF | Delete `:239-243` (guard), and replace `:245-253` (try/catch) with its two body lines `params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));` `socket.setSSLParameters(params);` | AC-1, AC-3 (existing IP/blank tests also fail; record but do not count) |
| M-A1g | DTSF | Delete `:239-243` only | BY-DESIGN: AC-1 is expected to pass (the catch rescues). Record what fails |
| M-A1c | DTSF | Replace `:245-253` with the two body lines only | BY-DESIGN: AC-1 is expected to pass (the guard stops it) |
| M-A2a | DTSF | Delete `:221-223` | AC-2 |
| M-A2b | DTSF | `:221` `hostname.indexOf(':') >= 0` becomes `hostname.contains("::")` | AC-2 |
| M-A3 | DTSF | `:197` `setSniViaSSLParameters(socket, hostname);` becomes `setSniViaSSLParameters(socket, "mutant.invalid.test");` | AC-3, AC-4 |
| M-A4 | DTSF | Delete `:177` `setSniHost(socketFactory, sslSocket, host);` | AC-4 |
| M-D1 | DTSF | Delete `:79` `"SSL_RSA_WITH_RC4_128_SHA",` | AC-11, AC-14 |
| M-D2 | DTSF | `:88` delete `"SSLv3"` (array becomes empty) | AC-11, AC-14; AC-16 only if `sslv3_supported` |
| M-D3 | DTSF | Delete `:129-133` (blocked-item removal) | AC-11 |
| M-D4 | DTSF | Swap `:38` and `:39` | AC-12, AC-14 |
| M-D5 | DTSF | `:84` becomes `"TLSv1.1", "TLSv1.2", "TLSv1"` | AC-12, AC-14, AC-16 |
| M-D6a | DTSF | `:145` `result.addAll(unknown);` becomes `result.addAll(0, unknown);` | AC-12 |
| M-D6b | DTSF | Delete `:145` | AC-12 (and AC-15/AC-16 set checks) |
| M-D7 | DTSF | `:129` `if (blacklisted != null) {` becomes `if (true) {` | AC-13 (null-blocklist clause) |
| M-D15 | DTSF | Insert `if (enabled.length == 0) { return null; }` as the first statement of `reorder`, before `:125` (W4) | AC-13 ("empty input is mishandled" clause). The static initialiser only calls `reorder` with non-empty provider arrays, so the other tests are not expected to move |

Class runs (A1, A1.g): Group A edits run `…ssl.DefaultTrustedSocketFactorySniTest` and
`…ssl.DefaultTrustedSocketFactoryJdkProviderTest` in one invocation. Group D edits run
`…ssl.DefaultTrustedSocketFactoryHardeningTest` and `…ssl.DefaultTrustedSocketFactoryJdkProviderTest`
in one invocation. The expected failures of M-A1, M-A3, M-A4, M-D2 and M-D5 are as in A1.g.

#### I6b — Group D part 2 and Groups B and C (Handshake test) — Turn 9

| ID | File | Exact edit | Expected failures |
|----|------|-----------|-------------------|
| M-D8 | DTSF | Delete `:176` `hardenSocket(sslSocket);` | AC-15, AC-16 |
| M-D9 | DTSF | Delete `:184` `sock.setEnabledCipherSuites(ENABLED_CIPHERS);` | AC-15 |
| M-D10 | DTSF | `:176` becomes `if (socket == null) hardenSocket(sslSocket);` | AC-17 |
| M-D11 | DTSF | `:107` `sock.getSupportedProtocols()` becomes `sock.getEnabledProtocols()` | AC-16, first test (second test: `NOTED-AC16` allowed under D7c, rule 3) |
| M-D12 | DTSF | `:88` becomes `"SSLv3", "TLSv1"` | AC-14; AC-16 second test if TLSv1 is supported (`NOTED-AC16` allowed under D7c) |
| M-D13 | DTSF | Delete `:187` `sock.setEnabledProtocols(ENABLED_PROTOCOLS);` | AC-16 |
| M-D14 | DTSF | `:61` `"TLS_RSA_WITH_AES_128_CBC_SHA",` becomes `"TLS_RSA_WITH_AES_128_CBC_SHA256",` | AC-14 |
| M-B1 | TMF | Delete `:77` `verifyHostname(mHost, chain);` | AC-6, AC-8 |
| M-B2 | DTSF | `:155` becomes `TrustManager[] trustManagers = new TrustManager[] { new javax.net.ssl.X509TrustManager() { public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) { } public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) { } public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; } } };` | AC-6, AC-8, AC-9 (2), AC-10 (2) |
| M-B3 | TMF | `:77` becomes `verifyHostname(mHost + ".mutant", chain);` | AC-5, AC-7 |
| M-B4 | TMF | `:165` `return chain;` becomes `return new Certificate[0];` | AC-5, AC-7 |
| M-B5 | DTSF | `:172` becomes `trustedSocket = socket;` | AC-7, AC-8, AC-9b, AC-10b (`ClassCastException` at `:175`) |
| M-C1 | TMF | Delete `:76` `defaultTrustManager.checkServerTrusted(chain, authType);` | AC-9 (2), AC-10 (2) |
| M-C2 | TMF | `:91` becomes `if (false) {` | AC-6, AC-8, AC-9 (2), AC-10 (2) (silent fallback accepts) |

Class runs (A1, A1.g): D edits run the Hardening test and the JDK-provider class in one invocation
(the expected failures of M-D8, M-D11, M-D12 and M-D13 are as in A1.g); B and C edits run
`…ssl.DefaultTrustedSocketFactoryHandshakeTest`.

#### I6c — Group E, summary and the bug hand-off — Turn 10

| ID | File | Exact edit | Expected failures |
|----|------|-----------|-------------------|
| M-E1 | PCSF | Replace `:81-85` with `Socket s = (socket == null) ? socketFactory.createSocket() : socketFactory.createSocket(socket, host, port, true);` `((javax.net.ssl.SSLSocket) s).setEnabledProtocols(new String[] {"TLSv1.2"});` `return s;` | AC-18 (both) |
| M-E2 | PCSF | As M-E1, but the middle line becomes `javax.net.ssl.SSLParameters p = ((javax.net.ssl.SSLSocket) s).getSSLParameters(); p.setServerNames(java.util.Collections.<javax.net.ssl.SNIServerName>singletonList(new javax.net.ssl.SNIHostName(host))); ((javax.net.ssl.SSLSocket) s).setSSLParameters(p);` | AC-19 |
| M-E3 | PCSF | Add the field `private final String host;` after `:57`, and `this.host = host;` after `:71`. After `:145`, add `if (!String.valueOf(presented.getSubjectAlternativeNames()).contains(host)) { throw new CertificateException("U-063 mutant host check"); }` | AC-20 |
| M-E4 | PCSF | `:141` `if (!Arrays.equals(` becomes `if (Arrays.equals(` | AC-20, AC-21 |
| M-E5 | PCSF | `:77` becomes an accept-all `TrustManager[]` exactly as M-B2 | AC-21 |

Class runs: `com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest`. PCSF is in the
app module, whose main javac has `-Werror -Xlint:unchecked`. The edits are written to compile without
warnings. Report a `COMPILE_FAIL` as described above.

**Close-out of I6c.**
1. Write a summary table at the top of `mutation-log.md` with one row per mutation: ID, ACs, verdict,
   failing tests. Add one row per AC listing the mutations that cover it and whether each is DETECTED.
   An AC is "demonstrated" only if every mutation it names is `DETECTED` (the `NOTED-AC16` exception
   aside). A `NOT DETECTED` row would already have stopped the turn (Criterion Status Rule, rule 2). It
   is listed here as unmet, never as met. Do not change a test to make it pass.
2. Post-I6 check (IC-7, IC-2): `git diff --stat $BASE -- $PROD` empty, and
   `git status --porcelain -- app/src/main k9mail-vendored` empty, saved to `$E/I6-clean.txt`.
3. `bug_requests`: the DP-2 draft (if not already filed at I3), plus any defect found under AC-23.
4. Gate G (710; A1). Manifest, log, hand-off.

### I7 — Final verification and gates — Turn 11

Order: checks that need no build first, then the repeat and order runs, then the clean final gate,
then the evidence read from that gate. After step 1 no U-063 product file may change. The throwaway
suites of step 4 are created and deleted as specified there, and the Option B delta, if any, lands
before step 1. If a check fails, stop and escalate; do not edit to make it pass.

1. **Hygiene and scope greps (IC-6, IC-8, IC-12, imports):** write to `$E/I7-greps.txt`.
   - IC-6:
     `git grep --untracked -nE "Thread\.sleep|getByName|getAllByName|\"localhost\"|\"127\.0\.0\.1\"|new InetSocketAddress\(\"" -- $S`
     must give 0 hits in the U-063 files.
   - Also 0 hits for:
     `git grep --untracked -nE "org\.(conscrypt|bouncycastle)|ConscryptMode|System\.setProperty|SSLContext\.setDefault|setDefaultSSLSocketFactory|Security\.(add|insert|remove)Provider|@Test\(timeout|Timeout" -- <the 6 U-063 files>`.
     (A1) The only allowed hits are `ConscryptMode` and the annotation import lines in
     `DefaultTrustedSocketFactoryJdkProviderTest.java`. `LooperMode` may appear only there too.
     `git grep --untracked -n "startHandshake" -- $S/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`
     gives 0 (A1.e item 3).
   - **Qualified annotations (C4):** `git grep --untracked -nE "@[A-Za-z_][A-Za-z0-9_]*\.[A-Za-z]" -- <the 6 U-063 files (A1)> | wc -l`
     gives 0. Also `git diff -U0 $BASE -- $S/ssl/DefaultTrustedSocketFactorySniTest.java | grep -E "^\+" | grep -cE "@[A-Za-z_][A-Za-z0-9_]*\.[A-Za-z]"`
     gives 0. Any hit is a package-qualified annotation (short-form rule). This pattern also catches Javadoc
     tags followed by a dotted name, such as a link tag with a qualified class. Write those with the
     simple class name, or explain the line in the log and confirm it is not an annotation.
   - IC-12:
     `git grep -nE "LocalKeyStore|setKeyStoreLocation|addCertificate" -- app/src/main | wc -l` gives 0.
   - **IC-8 over every file the story will commit (B3).** The scan covers:
     - the SNI-test diff;
     - the **6** new sources (A1: also `$S/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java` in the `cat` list);
     - `$W/plan.md`;
     - `$W/implementation-log.md`;
     - **all** of `$E`, `tools/` included.

     Nothing is excluded. Run
     `{ git diff $BASE -- $S/ssl/DefaultTrustedSocketFactorySniTest.java; cat $S/ssl/TlsTestFixtures.java $S/ssl/LoopbackTlsTestServer.java $S/ssl/DefaultTrustedSocketFactoryHardeningTest.java $S/ssl/DefaultTrustedSocketFactoryHandshakeTest.java $S/PinnedCertificateSocketFactoryCreateSocketTest.java $W/plan.md $W/implementation-log.md; find $E -type f -exec cat {} +; } > $WORK/logs/I7-ic8-stream.txt`,
     then
     `grep -nEi -e "@[a-z0-9-]+\.[a-z]{2,}" -e "Users[/\\\\]" -e "App""Data" $WORK/logs/I7-ic8-stream.txt | grep -vE "imap\.(right|other)\.test" | wc -l`.
     It must print 0. The tools are written so that their own pattern text does not match: `sanitize.py`
     builds the folder name from parts, and the `Users[` and `@[` forms are not matches. A non-zero
     result is investigated, never filtered away. Then `grep -ciF "$(hostname)" $WORK/logs/I7-ic8-stream.txt`
     and `grep -ciF "$USERNAME" $WORK/logs/I7-ic8-stream.txt` must both print 0. Only the three counts
     are written to `$E/I7-greps.txt`, never the values. The story file is not in this scan: its own
     IC-8 text contains the folder-name pattern and is a known, benign match for the literal form (Notes,
     item 7).
2. **IC-2 (working-tree form):** write to `$E/I7-ic2.txt`.
   - `git diff --name-only $BASE -- . ':(exclude)sdlc'` lists exactly
     `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java`.
   - `git ls-files --others --exclude-standard -- $PP` lists exactly the **6** new files (A1) of the
     [Count Reference](#count-reference), and nothing else. A leftover throwaway suite, probe or smoke
     file fails this check.
   - `git diff --stat $BASE -- $PROD` is empty, and so is `git diff --stat $BASE -- gradle/verification-metadata.xml`.
   - The additions-only check of I2.3 still gives 0.
   - `git diff --quiet $BASE -- $S/PinnedCertificateSocketFactoryTest.java` passes.
   - List the U-063 workspace files: `git status --porcelain --untracked-files=all -- $W`.
3. **IC-6 determinism:** three runs of
   `rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:testDebugUnitTest --rerun --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactorySniTest" --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryHardeningTest" --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryHandshakeTest" --tests "com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest"`,
   each followed by `junit_cases.py $R` into `$E/I7-ic6-run<n>-cases.txt`. Then
   `diff $E/I7-ic6-run1-cases.txt $E/I7-ic6-run2-cases.txt` and run 1 against run 3, both empty
   (`$E/I7-ic6-diff.txt`). (A1) Add `--tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest"`
   to the command. Each run gives 38 cases (fallback 36), all PASS.
4. **IC-9 orders: two throwaway ordered suites (B4, option (i)).** The order is forced by JUnit 4
   `Suite`, which runs its children in declaration order, in one JVM, each child under its own
   `@RunWith(RobolectricTestRunner.class)`. JUnit XML timestamps are not used.

   **Rules for the throwaway suites:**
   - Only one suite file exists at a time.
   - It lives at `$S/U063OrderedSuiteForwardTest.java` or `$S/U063OrderedSuiteReverseTest.java` (package
     `com.zegoggles.smssync.mail`; the name contains `Test`).
   - It contains only `import org.junit.runners.Suite;`, `import org.junit.runners.Suite.SuiteClasses;`,
     `@RunWith(Suite.class)`, `@SuiteClasses({...})` and an empty public class body. Use the imported
     short form `@SuiteClasses`: the qualified annotation form (the `Suite` class name, a dot, then
     `SuiteClasses`) matches the IC-8 e-mail pattern and would fail the I7.1 scan of the evidence copy.
     It holds **no `@Test` text**, so count C cannot change while it exists.
   - It is never present at a gate (step 5 runs after both are deleted), at a count, or at the end check.
   - It is never committed. The I7.2 untracked-file check, which runs before step 4, cannot see it. The
     re-check in step 4(d) proves it is gone.

   **(a) Forward suite.** Write `U063OrderedSuiteForwardTest` with `@SuiteClasses` in this order:
   1. `com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryHandshakeTest`
   2. `com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryHardeningTest`
   3. `com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryJdkProviderTest` (A1: placed between
      Conscrypt-mode classes so that guards A1-G1 to A1-G3 are exercised across modes in one JVM)
   4. `com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest`
   5. `com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactorySniTest`
   6. `com.zegoggles.smssync.mail.transport.K9MailTransportTest`
   7. `com.zegoggles.smssync.mail.BackupImapStoreTest`

   This puts the new classes first, the SNI test after them, then two existing classes that build the
   default factory. Run
   `rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:testDebugUnitTest --rerun --tests "com.zegoggles.smssync.mail.U063OrderedSuiteForwardTest" > $WORK/logs/I7-ic9-forward.log 2>&1; echo "exit=$?" >> $WORK/logs/I7-ic9-forward.log`,
   then `python -I -X utf8 $T/junit_cases.py $R > $E/I7-ic9-forward-cases.txt`. Then
   `cp $S/U063OrderedSuiteForwardTest.java $T/U063OrderedSuiteForwardTest.java.txt` (the declared order
   is the evidence) and `rm $S/U063OrderedSuiteForwardTest.java`.

   **(b) Reverse suite.** The same, with the seven classes (A1) in the opposite order (BackupImapStoreTest
   first, Handshake last). File `U063OrderedSuiteReverseTest`; evidence `I7-ic9-reverse-cases.txt` and
   `$T/U063OrderedSuiteReverseTest.java.txt`; then `rm`.

   **(c) Pass condition, each run.**
   - Exit 0.
   - Every case PASS.
   - The per-class case counts are Handshake 8, Hardening 9, Pinned createSocket 5, SNI 14, and
     K9MailTransportTest and BackupImapStoreTest equal to their counts in `$E/I0-suite-children-baseline.txt`.
     (A1) JDK-provider 4 (fallback 2). The total equals 38 (fallback 36) plus those two counts.
     Read the SNI count as 12 (A1).

   A missing class or a short count means the suite did not run its children as assumed (assumption
   ledger). That is **stop and report**: do not fall back silently to weaker evidence.

   **(d) After both runs.**
   - `git status --porcelain --untracked-files=all -- $PP` equals the I5 manifest (no suite file left).
   - Count C is 710 (Option B: 711; fallback 708 or 709) (A1).
   - Record both in `$E/I7-ic9.txt`.

   **(e) The default full run is the final gate** (step 5).

   Together, the forced orders, the `@Before` leak guards and the `@After` identity assertions are the
   IC-9 evidence. The identity assertions are the part that holds in any order.
5. **Final gate (IC-3), clean:**
   `rm -rf $R; ./gradlew -I "$INIT" --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease > $WORK/logs/I7-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I7-gates.log`.
   It must exit 0 and show `U063-DAEMON-JVM: java.version=25`. Sanitise into `$E/I7-gates.txt`.
   `junit_totals.py` gives `tests=710 failures=0 errors=0 skipped=2` (A1; Option B: 711 and 5; fallback
   708, or 709 and 5).
   `junit_cases.py $R` lists the skipped tests in `$E/I7-skipped.txt`: the 2 baseline ones, plus any
   `@Ignore`d test with its bug id.
6. **Coverage (IC-5):** `jacoco_pkgs.py` gives `$E/I7-jacoco-packages.txt`. The `mail` and
   `mail.transport` LINE figures are not below I0 (`mail` is expected to rise through
   `PinnedCertificateSocketFactory.createSocket`).
   `grep -cE "TlsTestFixtures|LoopbackTlsTestServer|HandshakeTest|HardeningTest|CreateSocketTest" app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`
   gives 0. List the compiled U-063 test classes with
   `find app/build/intermediates -name "*.class" | grep -E "TlsTestFixtures|LoopbackTlsTestServer|DefaultTrustedSocketFactory(Hardening|Handshake)Test|PinnedCertificateSocketFactoryCreateSocketTest" | sed 's#.*/##'`
   and confirm that every name contains `Test`. Write all of this to `$E/I7-ic5.txt`.
   `git diff --quiet $BASE -- app/build.gradle` passes (no floor changed).
7. **Counts (IC-4):** write to `$E/I7-count.txt`: count C = 710 (Option B: 711), and the `HEAD` form =
   682 (until committed). State the arithmetic (A1): 682 + 2 (A, SNI) + 4 (JDK-provider) + 9 (D) +
   8 (B and C) + 5 (E) = 710. Fallback: 682 + 2 + 2 + 9 + 8 + 5 = 708.
8. **Close-out.**
   - Complete `implementation-log.md` with:
     - the per-increment table (HEAD, files, gate exit, totals, counts);
     - the probe decisions;
     - the mutation summary;
     - DP-1 (not taken) and the DP-2 outcome;
     - the bug ids from the orchestrator;
     - all deviations;
     - the I0 and I7 coverage figures.
   - **Re-run the full I7.1 IC-8 scan** as the last check after the log and every evidence file are
     final. Text written after step 1 is then scanned too. All three counts must be 0, and they are
     recorded in the log's last lines (the log is not scanned again after those count lines, which hold
     only numbers).
   - Remove `$WORK` (`rm -rf "$WORK"`). It was created by the Developer and holds raw logs, snapshots and
     the IC-8 stream only; `gen/` was already removed at I1.
   - Run the end check (I5 porcelain), then hand off.
   - The orchestrator commits only if the product owner asks. Before QA, the orchestrator asks the
     product owner for a path-limited commit of U-063's files (Notes, item 7), and QA then re-runs the
     literal `HEAD` forms of IC-2, IC-4 and IC-8.

## DP-2 (TLS 1.0 and 1.1 stay enabled)

**Option A (default, planned).**
- The second AC-16 test asserts what the code does: TLSv1/TLSv1.1 are enabled if and only if the provider
  supports them. It passes.
- The Developer drafts the bug at I3 (hand-off `bug_requests`) and the orchestrator files it with
  `/amp:create-bug`. Its id goes into the story's Implementation Notes and into a one-line comment above
  the test.
- Draft template:
  - Title: "DefaultTrustedSocketFactory enables TLS 1.0 and TLS 1.1 (and any other provider-supported
    protocol except SSLv3)".
  - Evidence: `DefaultTrustedSocketFactory.java:107` (supported, not enabled, protocols), `:116`, and
    `:87-89` (only SSLv3 is blocked). The test name. The probe keys `tls10_*`, `tls11_*`, plus
    `sslv2hello_*` if the provider lists it.
  - Context: the device provider may disable these by default, but the factory re-enables them; the
    unit-test JVM's provider is not the device's.
  - Severity: left to the Security Reviewer. Not fixed by U-063.
- Counts: 710, skipped 2 (A1; fallback 708).

**Option B (only if the product owner chooses it, with the Security Reviewer's judgement).**
- Mark `createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem` with
  `@Ignore("<reason>, <bug id>")`.
- Add `createSocket_legacyProtocols_areNotEnabled_desired`, which asserts that TLSv1 and TLSv1.1 are not
  enabled, also `@Ignore("<reason>, <bug id>")`. It is written so that it would pass once the bug is fixed.
- This is done as a delta in the turn after the decision arrives, or in I7 before step 1 if it arrives
  late. Re-run TR for the Hardening test and gate G.
- Counts (A1): 711, skipped 5 (fallback 709, skipped 5). Both legacy characterisation tests (Hardening
  and JDK-provider) are `@Ignore`d, and the one desired-behaviour test is added, ignored, in the
  JDK-provider class, where it can tell the factory from the provider.
- The mutations M-D11 and M-D12 for that test are then recorded as not applicable (the test is skipped).
  AC-16's first test still covers them.

The safety judgement is the Security Reviewer's. This plan makes no judgement about whether the
behaviour is safe.

## Global and Static State (IC-9, IC-6)

| State | Scope | Touched by | Set-up and restore | Leak detection |
|-------|-------|------------|--------------------|----------------|
| `TrustManagerFactory.defaultTrustManager` (private static, non-final, `:33`) | per Robolectric sandbox class loader (app and vendored classes are loaded by the sandbox) | Handshake test only | `@Before` capture and set; `@After` restore in `finally`, identity assert | `@Before` fails if the current value trusts a `U-063` subject |
| `SecureX509TrustManager` cache (`:39-61`) | sandbox | every `createSocket` | not restored. It holds no trust decision: instances read the static field on every call (`:76`), and ports differ per test | n/a |
| `HttpsURLConnection` default verifier | JVM-wide (`javax.net.ssl` comes from the JDK's loader, not the sandbox) | Handshake test only (D3a) | as above | `@Before` fails if the current verifier class is the U-063 one |
| `DefaultTrustedSocketFactory.ENABLED_*` (static final) | sandbox | read only | none | n/a |
| `LocalKeyStore` (static location, singleton) | sandbox | read only (probe P8) | never set (IC-12) | IC-12 grep |
| Security providers, `SSLContext` default, `HttpsURLConnection` default socket factory, system properties (`javax.net.ssl.*`, `jdk.tls.*`) | JVM | **never** by U-063 (Robolectric manages Conscrypt itself) | none | I7.1 grep |
| (A1) Conscrypt mode and sandboxes | the Conscrypt mode is per test (Robolectric); the sandbox key has no Conscrypt mode | JDK-provider class | its own sandbox through `@LooperMode(LEGACY)` (A1.e); no global state set by the class | guards A1-G1 (new class), A1-G2 (Hardening, Handshake), A1-G3 (JVM-wide JDK TLS initialisation) |
| Threads and sockets | JVM | server helper and clients | daemon threads; `close()` closes the sockets and `shutdownNow()` stops the executor; clients are closed in `finally` or `@After` | IC-6 repeat runs |

**Under four parallel JVMs:**
- each fork has its own JVM-wide and sandbox state;
- inside one fork, the test classes run one after another. This is assumed; IC-9 does not depend on it,
  because the I7.4 suites force the order in one JVM and the identity assertions hold in any order;
- a restore in `@After` is therefore enough.
`forkEvery 100` only restarts JVMs. The DefaultTrustedSocketFactory static lists are computed once per
sandbox, under whichever provider Robolectric installs for every test (no test changes the mode). U-063
tests never trigger class initialisation from a static initialiser or `@BeforeClass`, so the lists are
computed under the same provider as the sockets.

## Verification Mapping

| AC / IC | Increment | Command or reading | Evidence (under `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/`) |
|---------|-----------|--------------------|-----------------|
| AC-1..AC-4 (A1: superseded by the A1.k rows for AC-1/AC-2 and AC-3/AC-4) | I2, I6a | TR Sni test (14 PASS); additions-only check; M-A1..M-A4 | `I2-sni-cases.txt`, `mutation-log.md`, `mutations/M-A*` |
| AC-5..AC-8 | I4, I6b | TR Handshake test; M-B1..M-B5 | `I4-handshake-cases.txt`, `mutation-log.md` |
| AC-9, AC-10 | I4, I6b | as above; M-B2, M-C1, M-C2 | same |
| AC-11..AC-17 (A1: for AC-16 see the A1.k row) | I3, I6a, I6b | TR Hardening test; M-D1..M-D15 (with M-D6a, M-D6b); probe P6/P7 for distinguishability | `I3-hardening-cases.txt`, `I0-probe.txt`, `mutation-log.md` |
| AC-18..AC-21 | I5, I6c | TR Pinned createSocket test; existing pinned test unchanged; M-E1..M-E5 | `I5-pinned-cases.txt`, `I5-pinned-existing-cases.txt`, `mutation-log.md` |
| AC-22 | I1-I5 | Code Review reads each class Javadoc (three statements; test-only and throwaway labels) | the source files |
| AC-23 | I3, I6c | bug drafts in hand-off; ids in Implementation Notes; `@Ignore` reasons | `implementation-log.md`, `I7-skipped.txt` |
| AC-24 | I0b | decision table; report if fired | `I0-probe.txt`, log "I0 probe decisions" |
| IC-1 | I0a | counts, gate, jacoco, deps trees | `I0-count.txt`, `I0-gates.txt`, `I0-junit-totals.txt`, `I0-jacoco-packages.txt`, `I0-deps-*.txt` |
| IC-2 (A1: 6 new files, A1.k) | I2, I7 | working-tree forms (deviation 2) | `I7-ic2.txt` |
| IC-3 | I0a, I7 | full gate set, clean | `I0-gates.txt`, `I7-gates.txt` |
| IC-4 (A1: counts per A1.f, A1.k) | every increment, I7 | count C and the `HEAD` form; JUnit totals | `I<k>-junit-totals.txt`, `I7-count.txt` |
| IC-5 | I0a, I7 | jacoco packages; class-name check; build file unchanged | `I0-`/`I7-jacoco-packages.txt`, `I7-ic5.txt` |
| IC-6 | I4, I7 | greps; three `--rerun` runs identical | `I4-ic6-grep.txt`, `I7-greps.txt`, `I7-ic6-*.txt` |
| IC-7 | I6a-c | protocol above | `mutation-log.md`, `mutations/`, `I6-clean.txt` |
| IC-8 | I7 | hygiene grep, host and user counts; sanitiser on every evidence file | `I7-greps.txt` |
| IC-9 (A1: 7-child suites and guards A1-G1 to A1-G3, A1.k) | I4 (guards), I7 | leak guards; `@After` identity asserts; forward and reverse throwaway ordered suites with per-class case counts; default full run | `I7-ic9-forward-cases.txt`, `I7-ic9-reverse-cases.txt`, `I7-ic9.txt`, `tools/U063OrderedSuite*.java.txt`, `I7-gates.txt` |
| IC-10 | I2, I5, I7 | additions-only diff; existing pinned test unchanged and passing | `I2-sni-cases.txt`, `I5-pinned-existing-cases.txt`, `I7-ic2.txt` |
| IC-11 | I0a, I0b | probe and deps check | `I0-probe.txt`, `I0-deps-check.txt`, log |
| IC-12 | I0a, I7 | grep | `I0-ic12-grep.txt`, `I7-greps.txt` |
| IC-13 | I0b, I1 | fixture summary; Javadoc provenance; `$WORK/gen` deleted | `I0-fixtures-summary.txt`, `TlsTestFixtures.java` |

## Evidence Layout and Hygiene (IC-8)

```
sdlc/artifacts/build/sprints/sprint-020/U-063/
  plan.md, implementation-log.md
  evidence/
    tools/         junit-xml.init.gradle, junit_totals.py, junit_cases.py,
                   jacoco_pkgs.py, probe_extract.py, sanitize.py, gen-fixtures.sh, ExtractFixtures.java,
                   U063HelperSmokeTest.java.txt, U063OrderedSuiteForwardTest.java.txt,
                   U063OrderedSuiteReverseTest.java.txt
    RECOVER-I6-<n>.txt   only if a recovery turn ran
    I0-*.txt       baseline, deps, env, fixtures summary (public certificate data only), probe output
    I<k>-*.txt     per-increment gate summary, totals, cases, manifest
    mutation-log.md, mutations/M-*-cases.txt, I6-clean.txt
    I7-*.txt       final checks
```

**Rules.**
- Text only. No private key, no `constants.txt`, no PKCS12 file and no raw Gradle log enters the
  repository.
- Raw logs stay in `$WORK/logs` and are deleted at I7.
- Every evidence file made from tool or Gradle output goes through `sanitize.py`:
  - It replaces the repository root, the user's home directory, the user name and the machine host name
    with `<repo>`, `<home>`, `<user>` and `<host>`.
  - It then refuses to write the file (exit 1, printing only line numbers) if a user-profile path, the
    Windows application-data folder name or an e-mail-like token remains.
  - A refused file is reduced further with `--grep`, or the hit is explained in the log. It is never
    hand-edited.
- Certificate subjects are generic (`O=U-063 Test Only`). No e-mail attribute appears in any `-dname`.
- Gradle gate evidence is reduced to the summary lines (task lines, `BUILD`, failures, the JVM line,
  `exit=`).
- **Hand-written text** (`implementation-log.md`, `mutation-log.md`, hand-off text copied into the log)
  follows the same rules as sanitised evidence:
  - no e-mail address, host name, user name or user-profile path;
  - workspace-relative or repository-relative paths only;
  - anything quoted from a Gradle or tool output is copied from the sanitised evidence file, never from a
    raw log.
- The IC-8 scan runs at I7.1 and again as the last close-out check (I7.8). It covers every file the story
  will commit: the 6 new sources (A1), the SNI-test diff, `plan.md`, `implementation-log.md` and all of
  `evidence/` **including `tools/`**. Nothing is excluded. The tools are written so that their own
  pattern text does not match: `sanitize.py` builds the folder name from parts, and the `Users[` and `@[`
  forms are not matches. This plan file was itself checked with the same three patterns and the
  host/user counts at the end of iteration 2 (0 hits).

## Contracts Verification

U-063 reads CNTR-MODERNIZATION-001 (`status: approved`, v1) and changes nothing in it. The story has
`integration_contracts: []` and adds no boundary. The tests as specified contradict no guarantee of the
contract. They differ from its wording in the places below, which are recorded, not fixed (CR-007 and
CR-009 own the combined revision; Record 6 adds the SNI statement).

| Contract text | What the tests show | Handling |
|---------------|---------------------|----------|
| `:191` and `:196`: the `CertificateException` is "surfaced from `createSocket(...)`" (and `:191` adds "at TLS-handshake time") | `createSocket` returns an unconnected or layered socket. The rejection surfaces at the handshake: `startHandshake()` here, the first read in `ImapConnection` (`:119`, `:324`). | **SC-1**: recorded in the story and in Implementation Notes; the wording belongs to the combined revision. Not a guarantee conflict, because the connection still fails closed |
| `:196`: "wrapped per k-9 as `MessagingException`" | The tests assert the precondition only: an `SSLException` whose direct cause is a `CertificateException`. That is what `handleSslException` (`:146-152`) turns into `CertificateValidationException`, a `MessagingException` (`CertificateValidationException.java:10`) | Consistent; the mapping itself is not run (out of scope) |
| `:95`: "chain validation against the system CA store" | The anchor in the tests is a test CA (E4) | A limit of the test, stated in the Javadoc; no conflict |
| `:96` and Invariant 4 (`:181-184`): pinning is for "this `host:port`", and scoping is the producer's job | AC-20 shows that the pinned trust manager ignores the requested host name | Consistent: scoping is by construction of the factory per host. `:288-289` classes "adding hostname-match on top of fingerprint" as a backward-compatible tightening. So the story's phrase "the contract owner must approve" is stronger than the contract; the test would still fail and must be changed knowingly. Recorded |
| SNI and cipher/protocol hardening | Not stated anywhere in the contract (CR-009 guarantee table) | The tests are the only carrier until the revision (Record 6) |
| DP-2 finding | The contract says nothing about protocol versions | No contract conflict; it is an AC-23 safety question for the Security Reviewer |

## Requirements and Standards Conformance

- **Requirements.** No approved REQ in the domain is of `type: standard` (frontmatter scan: 0). The
  relevant approved requirements are:
  - REQ-MODERNIZATION-019 AC-B8 (`:109`): a host-name-mismatch test and a pinned accept/reject test.
    This story supplies them for today's factory, as characterisation.
  - REQ-MODERNIZATION-002 AC-2 and AC-3 (`:100-110`): no production trust manager with an empty body;
    validated TLS by default. Unaffected. The test code adds no accept-all trust manager. The accept-all
    edits exist only as uncommitted mutations, reverted and checked.
  - REQ-MODERNIZATION-003: coverage floors are not changed, and its "characterisation test precedes the
    change" rule (`:77`) is the pattern this story follows for the later move.
  REQ-MODERNIZATION-010 is `superseded` and is not relevant.
- **Design.** No approved DES document governs these tests: DES-MODERNIZATION-002 has no host-name, SNI
  or cipher text (grep: 0). The plan follows the story's Technical Context E1-E10, all re-verified above,
  with deviations 7 and 8.

## Files Modified/Created

| # | File | Increment | Action |
|---|------|-----------|--------|
| 1 | `app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java` | I1 | new (no test) |
| 2 | `app/src/test/java/com/zegoggles/smssync/mail/ssl/LoopbackTlsTestServer.java` | I1 | new (no test) |
| 3 | `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java` | I2 | additions only (+4 tests, +1 import, +1 comment block) |
| 4 | `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java` | I3 | new (9) |
| 5 | `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHandshakeTest.java` | I4 | new (8) |
| 6 | `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java` | I5 | new (5) |
| 7 | `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java` (A1) | I2b | new (4; fallback 2) |

(A1) Row 3: the SNI test now gets +2 tests (AC-1, AC-2) and no new import.

**Workspace files:** `implementation-log.md` and `evidence/**`.

**Throwaway files, created and deleted within one increment, never left in the tree:**
- `U063ProbeTest.java` and `U063ProbeFixturesTest.java` (I0b);
- `U063HelperSmokeTest.java` (I1);
- `U063OrderedSuiteForwardTest.java`, then `U063OrderedSuiteReverseTest.java` (I7.4, one at a time,
  neither containing `@Test` text);
- `$WORK/**` (outside the repository; `gen/` deleted at I1, everything else at I7).

**Never touched:** everything under `app/src/main`, `k9mail-vendored/` (except the reverted I6
mutations), `build.gradle`, `app/build.gradle`, `settings.gradle`, `gradle/` (including
`verification-metadata.xml`), `PinnedCertificateSocketFactoryTest.java`, the story, CR-009 and the
contract.

## Stop Conditions

At any of these, halt, write `ESCALATION: …` in the log and hand off.

1. A start check fails: porcelain or manifest mismatch, product not clean, or `$BASE` diff not empty.
2. A probe row that says stop: D1b, D2, D3c, D4c, D5b, D5c, D5d, D6'', D7a, D7b, D7d, D7e, D8, D9,
   D9r, D10b without a working variant, D10c, D10d, D10e, D10f, D10g, D10h, D11, D12 or D13. Stop rows
   win over continue rows (precedence rule).
3. A gate fails, or a TR of a new class is not all PASS. Fix only inside the increment's own files. Never
   weaken an assertion to pass; an assertion the probe showed to be unachievable goes back to the
   orchestrator.
4. The count or totals differ from the increment's expectation.
5. A mutation is `NOT DETECTED` (its AC is unmet: Criterion Status Rule, rule 2), or a restore check
   fails, or a production file other than the current mutation's is dirty.
6. A coverage figure drops below I0 or the coverage verification fails.
7. A hygiene check refuses a file and the hit cannot be explained.
8. "bash.exe … not found", an unexplained process kill, or a Gradle call that exceeds the limit even when
   split.
9. Any need for a new dependency, a build-file change, a production change or a contract edit (DP-1,
   AC-24).
10. A refused write to `$WORK` at I0a (W6); a throwaway suite that does not run every child with the
    expected case count (I7.4); a throwaway file still present at a gate, count or end check; any
    RECOVER-I6 outcome (c).
11. (A1) Isolation guard A1-G1, A1-G2 or A1-G3 fails (A1.e); a pre-check outcome outside the two
    branches of A1.d.

## Rollback

- **Within an increment:** `rm` that increment's new untracked files. For I2, restore the SNI test from
  the I1 manifest state. Because it is a tracked file with only U-063 additions, use
  `git checkout -- app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java`.
  That is the only test-file checkout allowed, and it is used only in I2's own rollback and in the
  resume rule for an interrupted I2. Then re-verify the previous manifest.
- **Mutations:** `git checkout -- <the one file>` per the protocol.
- **Whole story, before any commit:** delete the new files and restore the SNI test as above. Nothing in
  production, the build or persisted data changes, so there is nothing else to undo.
- **After a commit:** `git revert`.

## Risks & Mitigations

| # | Risk | Detection | Response |
|---|------|-----------|----------|
| R1 | Desktop-JVM provider ≠ device provider. The tests can never prove the device's suites and protocols, its SNI at connect time, its system CA store or Android's real verifier | Stated in each class's Javadoc (AC-22); probe records the provider | Accepted limit. An on-device check is outside this story (story, "What a desktop-JVM unit test cannot show") |
| R2 | The loopback handshake fails with the hardened client lists (protocol filtering; `SSLv2Hello` under SunJSSE) | Probe P9 | Allowed server-side variants (D10b), else AC-24 split |
| R3 | Exception wrapping differs from the `handleSslException` precondition | Probe P9 `direct_cause` | D10c: stop for a ruling |
| R4 | A host-name test passes for the wrong reason (the JDK stub verifier rejects everything) | AC-5/AC-7 controls; P9 `hs_implicit_right_no_verifier_swap` | Verifier swap (D3a); M-B3, M-B4 prove the controls depend on the real path |
| R5 | Global state leaks between tests (trust manager, verifier) | Leak guards in `@Before`; identity asserts in `@After` | Restore in `finally`; IC-9 forward and reverse ordered suites |
| R6 | Flakiness from ports or timing; a hang | IC-6 three identical runs; 10-second bounds | Port 0 on loopback; no sleeps or retries. If flaky: stop and report, never add retries |
| R7 | Conscrypt's fixed TLS 1.3 suites make AC-15 false as literally written | P6 `fixed_suites_F` | Live F subtraction (deviation 7) |
| R8 | A mutation cannot be told apart from provider behaviour (M-D8, M-D9, M-D11, M-D13, M-D2 on the socket) | P6 booleans (D7a, D7b, D7d), known before any Group D test is written | Stop and report at I0b: the AC would be unmet in this environment (Criterion Status Rule, rule 4). The only exception is the second AC-16 test under D7c (`NOTED-AC16`). Nothing is relabelled or claimed |
| R9 | A mutation is left in place or committed | Write-ahead log, hash check after each restore, product-clean checks, I7 IC-2 | One file per mutation; restore by exact path; no gate while mutated |
| R10 | Uncommitted test work is lost (wrong `git checkout`, lost session) | Manifest compare at every start | Restricted git command list; snapshots in `$WORK`; idempotent increments |
| R11 | Test-count drift: `@Test` in comments; untracked files missed by the `HEAD` grep; another story lands | Count C every increment; `HEAD` form recorded separately | Text rule; re-baseline at I0 |
| R12 | Coverage gate | Gate G every increment; I0/I7 packages | The vendored classes are in no rule; `mail` can only rise; helper names contain `Test` |
| R13 | Robolectric start-up cost of three new classes | Gate durations in the gate summaries | Expected seconds; the sandbox is shared across classes. No mitigation needed, and classes are not merged to save time |
| R14 | Endpoint security kills a shell | Tool error text | Foreground only; 24 keytool and about 70 Gradle client invocations in total (48 of them in I6: 35 mutation runs, 10 baseline runs, 3 gates), spread over 11 turns; stop and report |
| R15 | Stale JUnit XML or JaCoCo data read as a result | XML file count in totals | `rm -rf $R` before every counting run; `clean` at I0a and I7; mutations never followed by a gate without a restored tree |
| R16 | Private test keys outside the fixtures or personal data in evidence | IC-8 grep; `$WORK/gen` deleted at I1 | Sanitiser on every evidence file; keys only in `TlsTestFixtures` |

## Quality Attribute Coverage (ISO/IEC 25010)

- **Functional Suitability.** The tests record today's behaviour of the four protections named by CR-009
  (host-name verification, chain validation, the SNI guard, cipher and protocol hardening) and of the
  pinned factory. Each claim is tied to a mutation that must fail it. Production behaviour is unchanged.
- **Performance Efficiency.** No production effect. Test-suite cost is 26 tests and 3 new Robolectric
  classes. Handshakes run on loopback with 10-second upper bounds, and their duration is recorded in the
  gate summaries. No target is set.
- **Compatibility.** No API, SDK or dependency change. The tests are provider-agnostic: expectations come
  from the live provider. `gradle/verification-metadata.xml` is untouched.
- **Usability.** Not applicable to this story's scope (no user-visible change).
- **Reliability.** The tests are deterministic by design: no network or DNS, port 0, bounded blocking
  calls, daemon threads, sockets closed, global state restored with identity checks. This is proven by
  three identical runs and by forward and reverse ordered-suite runs in one JVM.
- **Security.** This is the point of the story. It gives the first handshake-level proof that a wrong-host,
  untrusted or expired chain fails closed. Every protection has mutation evidence for the Security
  Reviewer. DP-2 surfaces a protocol-version question without judging it. Test keys are throwaway and
  confined to one fixture class. Evidence is sanitised.
- **Maintainability.** These tests are the oracle for the Part B move. Reflection targets are named in
  one place per class, so the move retargets them with the class (story, Test mechanics). Fixture
  provenance is in the Javadoc. Helpers follow the JaCoCo naming rule.
- **Portability.** The tests run on JDK 25 under Robolectric API 29 on any OS. The fixtures do not expire
  before 2107. No machine path is embedded. Device portability of the conclusions is explicitly not
  claimed (R1).

## Test Strategy

- **Characterisation:** 26 tests. The handshake tests use real loopback TLS. The configuration tests use
  real unconnected sockets. The list tests use reflection.
- **Effectiveness:** one mutation per "Breaks when" clause (35 mutations: A 7, D 16, B 5, C 2, E 5,
  including the 2 by-design pair members M-A1g and M-A1c), each run against its class, with results and restores logged.
- **Regression:** gate G after every increment. The full gate with `clean` at I0a and I7. The 10 existing
  SNI tests and 8 existing pinned tests run unchanged.
- **Environment facts:** a throwaway probe settles 10 blocks of unknowns before any committed test.
- **Determinism and isolation:** IC-6 (three runs) and IC-9 (leak guards, identity asserts, forward and reverse ordered suites).
- **Not covered (stated limits):** device providers, Android's verifier and CA store, wire contents,
  `ImapConnection` end to end, pre-API-24 branches, client certificates (story, "cannot show" list).

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-063 story | sdlc/artifacts/build/sprints/sprint-020/U-063-tls-socket-factory-characterisation-tests.md | Specification (AC-1..24, IC-1..13, DP, SC, E1-E10) |
| Sprint-020 plan | sdlc/artifacts/build/sprints/sprint-020/sprint-plan.md | Scope, contract gate, DP notes |
| CR-009 | sdlc/artifacts/build/change-records/CR-009-cntr-001-default-trusted-socket-factory-producer.md | Source; T1-T10; Suggested Approach |
| Feasibility analysis (U-062) | sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md | Q7.c, Q7.f, Q7.g, Q7.i; Records 5 and 6 |
| CNTR-MODERNIZATION-001 | sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md | Producer table, Invariant, Error Handling, Versioning |
| REQ-MODERNIZATION-001..019 (status scan) | sdlc/artifacts/requirements/modernization/ | All approved except 010 (superseded); none of `type: standard` |
| REQ-MODERNIZATION-002, -003, -019 | sdlc/artifacts/requirements/modernization/ | AC-2/AC-3; coverage gate and characterisation precedent; AC-B8 |
| DES-MODERNIZATION-002 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-002-transport-security-hardening-design.md | Checked for host-name/SNI/cipher text (none) |
| U-061 plan, log, tools | sdlc/artifacts/build/sprints/sprint-018/U-061/ | House structure; init script; totals and coverage tools; gate durations |
| U-060 dependency tree | sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/qa/qa-deps-debugUnitTestRuntime.txt | Conscrypt and BouncyCastle on the test runtime |
| Plan and gate templates | plugin cache `templates/build-artifacts/{build-plan,gate-report-template}.md` | Frontmatter and sections |
| Project rules | .claude/CLAUDE.md | JDK 25, gates, `MSYS_NO_PATHCONV`, count command |
| Source under test | k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/{DefaultTrustedSocketFactory,TrustManagerFactory,LocalKeyStore}.java; k9mail-vendored/src/main/java/com/fsck/k9/mail/{CertificateChainException,CertificateValidationException}.java; k9mail-vendored/src/main/java/com/fsck/k9/mail/store/imap/ImapConnection.java (:90-330); app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java | Every line reference |
| Existing tests | app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java; app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryTest.java; grep over app/src/test | Not to change; patterns; global-state usage |
| Build | app/build.gradle; build.gradle; k9mail-vendored/build.gradle; gradle/gradle-daemon-jvm.properties; app/src/test/resources/robolectric.properties | Gates, test config, JaCoCo, forks, source levels |
| Local jars (inspection only) | Robolectric 4.15.1 `annotations` and `robolectric` jars; android-all-instrumented 10 (API 29) | `ConscryptMode`; provider insertion; absence of `javax.net` |

## Appendix A — Tools (create verbatim with the Write tool)

`junit_totals.py` and `jacoco_pkgs.py` are byte copies of the U-061 tools
(`sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/tools/`), which were run against real inputs in
sprint-018. Copy them with `cp`. The others are new. They are small and fail closed.

`$T/junit-xml.init.gradle`
```groovy
// U-063: enable JUnit XML for Test tasks without editing tracked build files (U-060/U-061 method).
gradle.projectsEvaluated {
    gradle.rootProject.allprojects { p ->
        p.tasks.withType(Test).configureEach { t -> t.reports.junitXml.required = true }
    }
}
println "U063-DAEMON-JVM: java.version=${System.getProperty('java.version')}"
```

(The iteration-1 `serial-fork.init.gradle` and the `--order` option of `junit_cases.py` are withdrawn:
IC-9 order now comes from the throwaway ordered suites of I7.4, not from timestamps.)

`$T/junit_cases.py`
```python
"""List JUnit XML test cases. usage: python -I junit_cases.py <results-dir> [class-substring ...]
Prints '<class>#<name> PASS|FAIL|ERROR|SKIP [type]' sorted, then one 'CLASS <name> cases=<n>' line per
XML file (used by the I7.4 per-child count check), then totals. Exit 1 if no XML file matched."""
import glob, os, sys
import xml.etree.ElementTree as ET
filters = sys.argv[2:]
rows, suites = [], []
for p in sorted(glob.glob(os.path.join(sys.argv[1], 'TEST-*.xml'))):
    root = ET.parse(p).getroot()
    name = root.get('name', '')
    if filters and not any(f in name for f in filters):
        continue
    suites.append((name, sum(1 for _ in root.iter('testcase'))))
    for tc in root.iter('testcase'):
        status, kind = 'PASS', ''
        for tag, label in (('failure', 'FAIL'), ('error', 'ERROR'), ('skipped', 'SKIP')):
            el = tc.find(tag)
            if el is not None:
                status, kind = label, (el.get('type') or '')
                break
        rows.append(f"{tc.get('classname')}#{tc.get('name')} {status} {kind}".rstrip())
for r in sorted(rows):
    print(r)
for name, n in sorted(suites):
    print(f'CLASS {name} cases={n}')
count = {k: sum(1 for r in rows if f' {k}' in r) for k in ('PASS', 'FAIL', 'ERROR', 'SKIP')}
print(f"files={len(suites)} cases={len(rows)} " + ' '.join(f'{k.lower()}={v}' for k, v in count.items()))
sys.exit(0 if suites else 1)
```

`$T/probe_extract.py`
```python
"""Print the U063-PROBE lines from JUnit XML system-out. usage: python -I probe_extract.py <results-dir>"""
import glob, os, sys
import xml.etree.ElementTree as ET
n = 0
for p in sorted(glob.glob(os.path.join(sys.argv[1], 'TEST-*U063*.xml'))):
    for so in ET.parse(p).getroot().iter('system-out'):
        for line in (so.text or '').splitlines():
            if line.startswith('U063-PROBE '):
                print(line)
                n += 1
print(f'probe_lines={n}')
sys.exit(0 if n else 1)
```

`$T/sanitize.py`
```python
"""U-063 IC-8: copy text to evidence with machine details replaced; fail closed.
usage (from the repository root): python -I -X utf8 sanitize.py <in> <out> [--grep REGEX]
Replaces the repository root, the home directory, the user name and the host name with tokens. Refuses to
write (exit 1, prints line numbers only) if a profile path, the application-data folder name or an
e-mail-like token remains. The folder-name pattern is assembled from parts so this file does not match it."""
import os, re, socket, sys
src, dst = sys.argv[1], sys.argv[2]
keep = re.compile(sys.argv[sys.argv.index('--grep') + 1]) if '--grep' in sys.argv else None

def forms(path):
    if not path:
        return []
    fwd = path.replace('\\', '/')
    out = {path, fwd, fwd.replace('/', '\\')}
    m = re.match(r'^([A-Za-z]):/(.*)$', fwd)
    if m:
        out.add('/' + m.group(1).lower() + '/' + m.group(2))
    return sorted(out, key=len, reverse=True)

subs = [(f, '<repo>') for f in forms(os.getcwd())] + [(f, '<home>') for f in forms(os.path.expanduser('~'))]
tokens = [(t, r) for t, r in ((os.environ.get('USERNAME') or os.environ.get('USER') or '', '<user>'),
                              (socket.gethostname(), '<host>'),
                              (os.environ.get('COMPUTERNAME') or '', '<host>')) if len(t) >= 3]
bad = re.compile('Users[/\\\\]|' + 'App' + 'Data' + r'|[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+\.[A-Za-z]{2,}', re.I)
lines, hits = [], []
for i, line in enumerate(open(src, encoding='utf-8', errors='replace'), 1):
    if keep and not keep.search(line):
        continue
    for a, b in subs:
        line = re.sub(re.escape(a), b, line, flags=re.I)
    for t, r in tokens:
        line = re.sub(re.escape(t), r, line, flags=re.I)
    if bad.search(line):
        hits.append(i)
    lines.append(line)
if hits:
    print('REFUSED: residual personal data on input lines', hits[:50])
    sys.exit(1)
with open(dst, 'w', encoding='utf-8', newline='\n') as f:
    f.writelines(lines)
print(f'written {len(lines)} lines')
```

`$T/gen-fixtures.sh` (run as `JDK25="$JDK25" bash $T/gen-fixtures.sh $WORK/gen`)
```bash
#!/usr/bin/env bash
# U-063 throwaway test-fixture generator. JDK keytool only. Test-only material; throwaway keys.
# The store password is a test-only literal.
set -euo pipefail
G="$1"; mkdir -p "$G"
KT="$JDK25/bin/keytool"
P='u063-test-only'
KEY=(-keyalg RSA -keysize 2048 -sigalg SHA256withRSA -storetype PKCS12 -storepass "$P" -keypass "$P")
ST=(-storetype PKCS12 -storepass "$P")
ca() {   # alias dname
  "$KT" -genkeypair -alias "$1" -dname "$2" "${KEY[@]}" -keystore "$G/$1.p12" \
        -startdate "2019/12/01 00:00:00" -validity 36500 \
        -ext bc:c=ca:true,pathlen:0 -ext ku:c=keyCertSign,cRLSign
  "$KT" -exportcert -alias "$1" -keystore "$G/$1.p12" "${ST[@]}" -rfc -file "$G/$1.crt"
}
leaf() { # id host issuer startdate validity-days
  "$KT" -genkeypair -alias "$1" -dname "CN=$2, O=U-063 Test Only" "${KEY[@]}" -keystore "$G/$1.p12" \
        -startdate "$4" -validity "$5"
  "$KT" -certreq -alias "$1" -keystore "$G/$1.p12" "${ST[@]}" -file "$G/$1.csr"
  "$KT" -gencert -alias "$3" -keystore "$G/$3.p12" "${ST[@]}" -sigalg SHA256withRSA \
        -infile "$G/$1.csr" -outfile "$G/$1.crt" -rfc -startdate "$4" -validity "$5" \
        -ext san=dns:"$2" -ext ku:c=digitalSignature,keyEncipherment -ext eku=serverAuth -ext bc=ca:false
  "$KT" -importcert -alias "$3" -file "$G/$3.crt" -keystore "$G/$1.p12" "${ST[@]}" -noprompt
  "$KT" -importcert -alias "$1" -file "$G/$1.crt" -keystore "$G/$1.p12" "${ST[@]}" -noprompt
}
ca   trusted-ca     "CN=U-063 Test Trusted CA, O=U-063 Test Only"
ca   untrusted-ca   "CN=U-063 Test Untrusted CA, O=U-063 Test Only"
leaf right-host     imap.right.test trusted-ca   "2020/01/01 00:00:00" 32000
leaf other-host     imap.other.test trusted-ca   "2020/01/01 00:00:00" 32000
leaf untrusted-host imap.right.test untrusted-ca "2020/01/01 00:00:00" 32000
leaf expired-host   imap.right.test trusted-ca   "2020/01/01 00:00:00" 30
echo "U063-GEN done"
```

`$T/ExtractFixtures.java` (run as `java $T/ExtractFixtures.java $WORK/gen > $WORK/gen/constants.txt 2> $WORK/gen/summary.txt`)
```java
// U-063 throwaway: print Base64 constants (stdout) and public provenance (stderr) for the fixtures.
// JDK only. Fails if a chain is not [leaf, issuer] or a key is not PKCS#8. Never committed with keys.
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class ExtractFixtures {
    static final char[] PW = "u063-test-only".toCharArray();   // test-only literal

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        for (String ca : new String[] {"trusted-ca", "untrusted-ca"}) {
            X509Certificate c = (X509Certificate) load(dir, ca).getCertificate(ca);
            emit(constName(ca) + "_CERT", c.getEncoded());
            summary(ca, c);
        }
        String[][] ids = {{"right-host", "trusted-ca"}, {"other-host", "trusted-ca"},
                          {"untrusted-host", "untrusted-ca"}, {"expired-host", "trusted-ca"}};
        for (String[] id : ids) {
            KeyStore ks = load(dir, id[0]);
            Certificate[] chain = ks.getCertificateChain(id[0]);
            if (chain == null || chain.length != 2) {
                throw new IllegalStateException(id[0] + ": chain length " + (chain == null ? 0 : chain.length));
            }
            X509Certificate leaf = (X509Certificate) chain[0];
            X509Certificate issuer = (X509Certificate) chain[1];
            X509Certificate expectedIssuer = (X509Certificate) load(dir, id[1]).getCertificate(id[1]);
            if (!issuer.equals(expectedIssuer)) {
                throw new IllegalStateException(id[0] + ": unexpected issuer");
            }
            PrivateKey key = (PrivateKey) ks.getKey(id[0], PW);
            if (!"PKCS#8".equals(key.getFormat())) {
                throw new IllegalStateException(id[0] + ": key format " + key.getFormat());
            }
            emit(constName(id[0]) + "_CERT", leaf.getEncoded());
            emit(constName(id[0]) + "_KEY_PKCS8", key.getEncoded());
            summary(id[0], leaf);
        }
    }

    static KeyStore load(File dir, String name) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(new File(dir, name + ".p12"))) {
            ks.load(in, PW);
        }
        return ks;
    }

    static String constName(String id) {
        return id.toUpperCase(java.util.Locale.ROOT).replace('-', '_');
    }

    static void emit(String name, byte[] der) {
        String b = Base64.getEncoder().encodeToString(der);
        System.out.println("    public static final String " + name + " =");
        for (int i = 0; i < b.length(); i += 64) {
            String part = b.substring(i, Math.min(b.length(), i + 64));
            System.out.println("            \"" + part + "\"" + (i + 64 < b.length() ? " +" : ";"));
        }
    }

    static void summary(String id, X509Certificate c) throws Exception {
        byte[] fp = MessageDigest.getInstance("SHA-256").digest(c.getEncoded());
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < fp.length; i++) {
            hex.append(String.format("%02X", fp[i])).append(i + 1 < fp.length ? ":" : "");
        }
        System.err.println(" * " + id + ": subject=" + c.getSubjectX500Principal().getName()
                + "; issuer=" + c.getIssuerX500Principal().getName()
                + "; SAN=" + c.getSubjectAlternativeNames()
                + "; notBefore=" + c.getNotBefore().toInstant()
                + "; notAfter=" + c.getNotAfter().toInstant()
                + "; SHA-256=" + hex);
    }
}
```

## Notes for the Orchestrator and Downstream Gates

1. Record acceptance of deviations 1-12 in the story's Implementation Notes before the Developer starts.
   Tell Code Review and QA about deviations 2, 5, 7 and 9 in particular.
2. Copy the I0 probe decisions (IC-11) and the DP-1 and DP-2 outcomes into the story's Implementation
   Notes after I0b and I3.
3. File the DP-2 bug from the I3 hand-off with `/amp:create-bug` and return its id to the Developer for
   the test comment (and the `@Ignore` reason under Option B).
4. Put DP-2 to the Security Reviewer and the product owner. Option A stands until they decide.
5. If AC-24 fires, decide the split with the product owner and dispatch the Requirements Analyst for the
   second story. The Developer continues with the reduced increment list only after that decision.
6. SC-1 (contract wording) and the AC-20 note on `:288-289` go to the combined CNTR-MODERNIZATION-001
   revision (CR-007, CR-008, CR-009). SC-2..SC-6 stay as raised in the story.
7. **Literal IC-2, IC-4 and IC-8 close only after a commit (W5).** The Developer's working-tree forms
   (deviation 2) are evidence, but the story must not be reported done on them alone.
   - Before QA, ask the product owner for a **path-limited** commit of U-063's files only:
     - the 6 product files of the Count Reference;
     - `sdlc/artifacts/build/sprints/sprint-020/U-063/` (plan, plan-review, log, evidence);
     - the story file.
     Commit it separately from the CR-009 edit, the skill-delegation log and the sprint files, which are
     already dirty in the tree. A range that includes them fails the literal IC-2 ("only `app/src/test/`
     and the story's own workspace files").
   - QA then re-runs the literal IC-2 (`git diff --name-only <base>..HEAD`), IC-4 (the CLAUDE.md `HEAD`
     count, 710 (A1; fallback 708)) and IC-8 (`git diff <base>..HEAD | grep …`) over that range.
   - **Known, benign IC-8 match:** the story file's own IC-8 line (story line 138) contains the
     folder-name pattern, so a range that includes the story matches itself there. QA records that one
     line as explained and checks that no other line matches.
   - If the product owner declines a commit, record that the literal forms remain open and that the
     working-tree forms stand in for them. That is a deviation needing the product owner's
     acknowledgement.
8. **Lost session during I6 (W2).** If a Developer session is lost in I6a, I6b or I6c, dispatch nothing
   else against the tree until a Developer turn has run [RECOVER-I6](#recover-i6--recovery-turn-after-a-lost-i6-session)
   and handed off with `recovered: true`. Nothing else covers Code Review, QA, a gate, a commit or
   another increment. A production or vendored file may still carry a mutation until that turn has
   checked the hashes. **The same holds for a session lost after I7 step 4 has started (C3):** before
   anyone runs any gate (Code Review, QA, the orchestrator), check for `U063OrderedSuite*Test.java`
   under `app/src/test/java/com/zegoggles/smssync/mail/` and remove it. Then confirm that the
   untracked-file list equals the I5 manifest and the working-tree `@Test` count is 710 (A1; fallback 708; 711 or 709 under
   Option B). The next Developer turn does this as its first action (resume rule 6).
9. **Tell the product owner (W11)**, for information, no decision needed:
   - deviation 1: no commit per increment, because commits are the orchestrator's on request;
   - deviation 10: bugs are filed by the orchestrator, not the Developer.
   Both change the story's own wording.
10. **W6 fallback.** If I0a stops because `$WORK` (`../u063-work`) cannot be written, choose the fallback
    location (recommended: a `u063-work` sub-folder of the Developer's harness scratch folder, with the
    session-loss consequences stated in I0a step 1). Re-dispatch I0a with it.

## Verdict: PASS

The story can be implemented as specified, within its files-touched rule and without a dependency. Every
open question the story left to planning is decided:
- the probe, with a branch for each outcome and the AC-24 trigger;
- the call shapes, and why `ImapConnection` is not driven;
- JDK-only offline fixtures with fixed dates;
- trust substitution through the static field, with leak-guarded restore;
- a mutation protocol that cannot leave an edit behind;
- DP-2 under both options;
- 11 resumable turns with gates, counts and stop conditions;
- a full AC/IC verification map;
- evidence hygiene;
- the contract reading;
- the risks.

The unknowns that remain (provider, verifier stub, negotiation, exception wrapping, fixed suites) are
environment facts that only running code can settle. Each is a probe key with a pre-decided response, so
none of them blocks planning.

Iteration 2 resolves all five plan-review blockers:
- B1: every probe outcome now has a row, with a catch-all D13.
- B2: an undetected mutation leaves its criterion unmet and stops the Developer, with only the story's
  AC-16 exception.
- B3: the IC-8 scan covers every committed file, `tools/` included.
- B4: IC-9 order is forced by throwaway ordered suites that are deleted after use.
- B5: counts come from one reference table.
All eleven warnings are folded in (W11 as an orchestrator note).

Iteration 3 resolves C1. Every printed probe key has a row, a successful probe matches only continue
rows and R0, stop rows take precedence, and D13 is narrowed to missing, malformed or contradictory
output. C2 to C5 are fixed locally.

## Phase Completion Report
---
story_id: "U-063"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-020/U-063/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
