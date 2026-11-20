---
artifact_type: gate-result
story_id: "U-063"
gate: plan-review
gate_id: plan-review
checkpoint: plan-gate
verdict: PASS
iteration: 3
agent: "Solution Architect (separate-instance plan reviewer)"
timestamp: "2026-11-17"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-020/U-063/plan.md (iteration 3)"
blockers: 0
warnings: 1
notes: 4
---

# Gate Report: gate-result — U-063 (iteration 3)

## Gate: plan-review @ plan-gate

## Verdict: PASS

## Summary

The iteration-3 plan resolves the last blocker, C1. Every key the probe prints now has an exact name and
value form. A normal, fully successful probe output reaches only continue rows and the record-only row
R0, and never D13. The three failure outcomes I walked each reach a stop. A precedence rule ("stop wins")
covers every overlap. Warnings C2 to C5 are resolved without new defects. The Count Reference table and the
Criterion Status Rule are unchanged from iteration 2. One small wording inconsistency remains (warning
D-W1). The Developer can apply it in I0b without re-planning, and it cannot make the implementation
wrong, unsafe or unverifiable.

## C1 Verification

### Key list, counted independently

From the probe output table (plan lines 474-485):

| Block | Keys | Count |
|-------|------|-------|
| P1 | `sdk_int`, `java_version`, `providers`, six `prov_*` | 9 |
| P2 | `fixtures_load` | 1 |
| P3 | `hv_default_class`, `hv_right_on_right`, `hv_right_on_other` | 3 |
| P4 | `sni_ctor_eight_group`, `sni_ctor_1to8` | 2 |
| P5 | `sni_obs_dns`, `sni_obs_ipv6`, `sni_obs_pinned_implicit` | 3 |
| P6 | 9 lists | 26 |
| P6 (cont.) | 17 booleans: 4 `f_bound_*`, 6 `tls10_*` and `tls11_*`, `sslv3_supported`, `sslv2hello_supported`, `cipher_order_differs`, `protocols_differ`, `supported_minus_sslv3_equals_default_set`, 2 `*_order_matches_reflected` | (in 26) |
| P7 | 4 booleans | 4 |
| P8 | `tmf_default_tm_class`, `tmf_field_rw`, `localkeystore_null`, `p8_restore_ok` | 4 |
| P9 | 8 `hs_<path>_<fixture>`, `p9_restore_ok` | 9 |
| P10 | `pin_accept`, `pin_reject` | 2 |
| **Total** | | **63** |

This matches the plan's figure (line 632). The two conditional key kinds (lines 647-650) are also
consistent between the block table, the decision rows (D3a, D3b, D10b, D10e', D13(a)) and the
completeness table (lines 625-626).

The completeness table (lines 595-630) has a row for every one of the 63 keys. I found no key in the
completeness table that the block table does not print, and no printed key that the completeness table
omits. Appendix A contains no probe source, only `probe_extract.py`, which selects lines by the
`U063-PROBE ` prefix and is therefore unaffected by the renaming.

### Walk 1: a fully successful probe

The expected environment is JDK 25 tests, API 29, the JDK stub verifier, and SunJSSE or Conscrypt:
- `sdk_int=29`, `java_version=25…` → D1a.
- `fixtures_load=OK` → D2a.
- Both `hv_*` false → D3a.
- Both `sni_ctor_*` `IAE` → D4a.
- `sni_obs_dns=[imap.right.test]` → D5a.
- `sni_obs_ipv6=[]` → D5a2.
- `sni_obs_pinned_implicit=[]` → D5e.
- F `[]` or non-empty → D6 or D6'.
- All `f_bound_*` true → D6b.
- `cipher_order_differs=true` → D7a'.
- `protocols_differ=true` and `supported_minus_sslv3_equals_default_set=false` → D7b'.
- TLS 1.0/1.1 disabled by default → D7c'.
- Hardened equals supported → D7e'.
- Both `*_order_matches_reflected` true → D7d'.
- P7 all true → D8a.
- P8 working → D9a.
- Both restore flags true → D9r'.
- P9 as expected → D10a.
- Swap check rejected with marker → D10e'.
- P10 as expected → D11a.
- All `ms` ≤ 10000.
- Everything else → R0.

Every key reaches a continue row or R0. D13 is not reached.

### Walk 2: failures

| Failure | Rows matched | Result |
|---------|--------------|--------|
| A control fails with a certificate rejection (`hs_implicit_right=REJECTED direct_cause=…CertificateChainException marker=false`) | D10f only. D10a fails on that key. D10b needs a negotiation error. D10c, D10d and D10g concern the six rejection keys | One stop (D10f) |
| The provider reorders the hardened lists (`cipher_order_matches_reflected=false`) | D7d. D7d' does not match | One stop (D7d) |
| A key is missing (for example `p9_restore_ok`) | D9r' needs it `true` and D9r needs it `false`, so neither matches. D13(a) matches | One stop (D13) |

### New stop rows do not fire on an expected environment

- **D1b** (`sdk_int` ≠ 29 or `java_version` not starting with `25`). `robolectric.properties:8` pins
  `sdk=29`. CLAUDE.md states that unit tests run on JDK 25, through `gradle/gradle-daemon-jvm.properties`.
  The probe prints the test JVM's own version, so `25.0.x` passes. If the test JVM were not 25, the
  project's rules would already be broken, and a stop is the right answer.
- **D7e** (hardened legacy protocols differ from supported). DTSF `:107`, `:116` and `:182-189` set the
  enabled protocols to supported-minus-SSLv3, so on unmodified code `tls1x_hardened = tls1x_supported`.
- **D9r** (restore flags). Fires only if the probe itself fails to restore global state. That is the
  correct stop.
- **D10h** (`ERROR` from a non-SSL throwable). The successful path prints only `OK` or `REJECTED`.

### Precedence

The rule (lines 511-521) lists every overlap I identified in iteration 2: D3a/D3b with D3c, D4a/D4b with
D4c, D6/D6' with D6''. It adds the D7c, D10a, D11a and D12 combinations. Two stop rows that match together
are both reported, which is harmless.

## C2 to C5

| Item | Plan lines | Result |
|------|-----------|--------|
| C2 RECOVER-I6 case (a') | 999-1004, 1013-1014 | Resolved. A clean tree with the last entry restored, or no entry yet, now continues instead of stopping. Case (c) still catches every unexpected state |
| C3 leftover throwaway suite | resume rule 6 (371-378); Notes item 8 (1804-1808) | Resolved. Every turn after I7.4 starts, and anyone before a gate, sweeps `U063OrderedSuite*Test.java`. They then re-check the untracked list against the I5 manifest and count C = 708 |
| C4 short-form annotations | rule 310-318; I7.1 check 1107-1111 | Resolved. The grep only matches an at-sign directly followed by a dotted name. Ordinary annotations with a parenthesis, and Javadoc tags followed by a space, do not match. The rare false positive goes through an explained-in-log path |
| C5 I7 opening sentence | 1096-1099 | Resolved. It now restricts "no change" to U-063 product files, and names the throwaway suites and the Option B timing |

## Spot-checks Outside C1 to C5

- **Count Reference** (lines 194-214). Unchanged: 26 tests, 5 new files, count C 682 → 708, 35
  mutations, 5 throwaway files.
- **Criterion Status Rule** (lines 216-236). Unchanged. `NOTED-AC16` is still limited to the second
  AC-16 test (lines 232, 968, 1054-1055, 1086, 1467).
- **IC-8 patterns.** No new text in the revision contains the e-mail, profile-path or application-data
  patterns. The C4 rule describes the qualified form in words rather than writing it out.

## Findings (iteration 3)

### Blockers

None.

### Warning

**D-W1. A generic `restore_ok` key name remains in the probe prose.** Plan line 464 still says each block
that replaces global state "prints `restore_ok=<bool>`". Line 468 and the block table say the probe prints
exactly the listed keys "and no others", with the restore flags named `p8_restore_ok` and
`p9_restore_ok`. A Developer who follows line 464 literally would print an unlisted key, and leave the
two listed ones missing, which is a D13(a) stop.
**Suggested fix (Developer, no re-planning needed):** treat line 464 as referring to `p8_restore_ok` (P8)
and `p9_restore_ok` (P9). Those are the only blocks that replace global state; P3 only reads the verifier
and P10 changes nothing global. Record that reading in the I0b log.

### Notes

**N1. An extra key is not a D13 case.** D13 lists a missing key, an unhandled `EXCEPTION`, a malformed
value and contradictions. An extra, unlisted key printed by mistake is not among them. It cannot change a
decision, because no row reads it, but the Developer should log and remove it.

**N2. A D7b stop is plausible under Conscrypt.** If Robolectric's Conscrypt mode is active and Conscrypt
enables TLS 1.0 and 1.1 by default, `supported_minus_sslv3_equals_default_set` may be `true`. D7b then
stops, correctly: M-D11 would then be invisible to the first AC-16 test. The orchestrator should expect
that this ruling may be needed after I0b. It is an environment fact, not a plan defect.

**N3. Variant keys come from a re-run.** The probe is one run, and D10b is decided from its output. The
`*_server_variant_<n>` keys therefore come from a re-run of the edited probe within I0b. That is implicit
in D10b and unchanged since iteration 1. No further rule is needed.

**N4. This report is in the commit.** It is committed under Notes item 7. It was written to contain none
of the three IC-8 patterns.

## History of Earlier Iterations

**Iteration 1 (FAIL; 5 blockers, 11 warnings).**
- B1: the probe table lacked branches and a default.
- B2: `UNDETECTABLE-HERE` could hide an unmet criterion.
- B3: the IC-8 working-tree scan skipped committed workspace files.
- B4: the IC-9 order evidence relied on coarse timestamps.
- B5: count errors ("4 new files", "all four controls").
- W1-W11: F bounds; resume and recovery gaps; Group E host; AC-13 mutation; literal forms after commit;
  `$WORK` location; unbounded `accept()`; null leak guard; unbuffered read; raw environment file;
  product-owner information.

**Iteration 2 (FAIL; 1 blocker, 4 warnings).** B2 to B5 and W1 to W11 were resolved. B1's new catch-all
D13 fired on normal outcomes (C1). The warnings were:
- C2: the RECOVER-I6 clean-and-restored case;
- C3: a leftover throwaway suite;
- C4: qualified annotations;
- C5: the I7 opening sentence.

**Iteration 3 (this report): PASS.** C1 to C5 are resolved. One warning (D-W1) and four notes remain, each
with a fix the Developer or orchestrator can apply.

## Phase Completion Report
---
story_id: "U-063"
phase: "plan-gate/plan-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-020/U-063/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---

## Amendment A1 review (2026-11-17)

The orchestrator asked for this review of the post-probe "Amendment A1" (plan lines 26, 30 and 46-336,
plus the edits marked "(A1)"). It is not a gate iteration. The gate result above (PASS, iteration 3)
stands. Inputs read in this session:
- plan.md, A1 and every "(A1)" edit;
- `evidence/I0-probe.txt` (all 65 lines);
- `evidence/I0c-diag.txt` (all 327 lines);
- `implementation-log.md`, sections I0a to I0c;
- the story's Implementation Notes;
- `contract-verification.md` and `expert-depth-check.md`;
- the Robolectric 4.15.1 jars in the local Gradle cache, read with the JDK's `javap` (nothing run, nothing
  downloaded).

**A1 verdict: PASS.** A1 is consistent with the evidence. Its central claim, about the Robolectric
sandbox key, holds against the jar. No finding makes the implementation wrong, unsafe or unverifiable.
Six warnings (A-W1 to A-W6) carry concrete text changes that the Developer or orchestrator can apply at I2b or I7
without re-planning. The plan as a whole keeps its PASS.

### Independent verification

**Robolectric sandbox key (A1.e item 1).** `javap -p` on `org.robolectric.internal.SandboxManager$SandboxKey`
(robolectric-4.15.1.jar) shows exactly five fields: `sdk`, `instrumentationConfiguration`,
`resourcesMode`, `looperMode`, `graphicsMode`. `SandboxManager.getAndroidSandbox` builds the key from
those five and caches sandboxes in a `LinkedHashMap` keyed on it. The SQLite mode is passed in but is not
part of the key. **The Conscrypt mode is not part of the key**, so the planner's claim is confirmed. A
class with only `@ConscryptMode(OFF)` would share a class loader, and with it
`DefaultTrustedSocketFactory`'s static lists, with the Conscrypt-mode classes.

**Conscrypt is toggled per test, not per sandbox.** `javap -c` on
`org.robolectric.android.internal.AndroidTestEnvironment.setUpApplicationState` shows the following
order on every test:
1. `Security.removeProvider` (twice);
2. only in mode ON, `Security.insertProviderAt(new OpenSSLProvider(), 1)` and
   `HttpsURLConnection.setDefaultHostnameVerifier(...)`;
3. `Security.addProvider(new BouncyCastleProvider())` if absent.

So an OFF class in a JVM where ON classes ran earlier does see no Conscrypt provider. A1-G1 checks
exactly that. This also explains the probe's `hv_default_class=…AndroidTestEnvironment$1` (`I0-probe.txt:16`),
and why an OFF class sees the JDK stub verifier only if no ON test ran before it (`I0c-diag.txt:6`).

**Looper mode in 4.15.1.**
- `javap -v` on the annotations jar: `LooperMode.Mode.LEGACY` carries `Deprecated: true`.
- `INSTRUMENTATION_TEST` and `PAUSED` are not deprecated.
- LEGACY is still implemented (`LooperModeConfigurer`) and works on API 29.

**No existing test sets a looper, graphics, resources or SQLite mode** (grep over `app/src/test`: 0 hits).
The `@Config(sdk = …)` uses are 21, 23 and 29, so SDK-21 and SDK-23 sandboxes already exist in this
suite. A second API-29 sandbox is therefore not new territory for memory or start-up.

**The A1.a outcome table against the evidence, row by row.**

| A1.a row | Evidence checked | Result |
|----------|------------------|--------|
| D1a, D2a, D4a, D6'+D6b, D7a', D7c, D7d', D7e', D8a, D9a, D9r', D10a (rejections), R0 | `I0-probe.txt:2-4,6-8` (six rejections, `direct_cause=CertificateChainException`, marker true only for `other`), `:13-15`, `:19-22`, `:26-55`, `:56-57` | Correct |
| D3b | `:16-18` | Correct. The verifier is Robolectric's, installed per ON test (see above) |
| D13(d) and D11, closed by V5 | `:1,5,10` (`byte=-1 server=FAILED`); root cause `I0c-diag.txt:144-145,254,256`; V5 `:287-297` (three controls `byte=42 server=OK`, all six rejections and `pin_reject` unchanged) | Correct |
| D10b variants | `I0c-diag.txt:41-78` | Correct, apart from wording: variant 2 was **skipped**, not tried (`:53`). It did not "fail the condition" (note A-N1) |
| D5b | `:23`; `I0c-diag.txt:89-91,258-261` | Correct. The row also lists `sni_obs_ipv6` and `sni_obs_pinned_implicit`, whose `null` values match the continue rows D5a2 and D5e, not a stop. The next row's label "D5c (pinned)" should read D5e. Wording only (A-N1) |
| D7b | `:49`; M-D13 still visible through order (`:28` versus `:27`) | Correct |
| Probe-key naming | `:9,22` | Correct |
| Wire facts | `I0c-diag.txt:262-285`, `:302-325` | Correct, and correctly not turned into a test (A1.i) |
| Extra keys | `:12` `negotiated_protocols`, `:65` `jdk_tls_disabled_algorithms` | These are the two record-only keys the orchestrator accepted (story notes, expert-depth item). A1.a cites them without naming them as the accepted extras (A-N1) |

Nothing is recorded as decided that the evidence does not support.

### Answers to the review questions

**1. A1.a.** Supported by the evidence (table above). Three wording slips are listed as note A-N1.

**2. V5 (A1.b): sufficient and safe.**
- Every wait is bounded: `accept()`, the accepted socket's read timeout and `awaitOutcome()` all use
  `TIMEOUT_MILLIS`.
- The server thread is a daemon.
- The client is never reconfigured.
- V5 changes only the peer-host string the **server** engine is given. It does not change the chain the
  server presents or anything the client's trust manager sees.
- A rejection cannot be turned into a pass by V5, because the accept or reject decision is made by the
  client's `SecureX509TrustManager` or `PinnedX509TrustManager`. Evidence: the rejection keys are
  identical before and after V5.
- A control cannot pass falsely. It requires `byte=42`, the byte the server writes only after its own
  `startHandshake()` returns, read through the client's TLS stream. That is real application data over a
  completed handshake.
- The required one-sentence Javadoc is specific and true (`I0c-diag.txt:254`).

**3. The JDK-provider class and its isolation.**
- **The sandbox claim** is verified (above).
- **Is `@LooperMode(LEGACY)` supported, and does it matter?** It is supported on API 29 and deprecated.
  The deprecation produces a `javac` deprecation note on the test sources, not a failure: `-Werror`
  applies only to non-test `JavaCompile` tasks (`app/build.gradle:190-205`).
- **Effect on what the tests do:** none. The tests create sockets and read parameters, with no Looper
  and no handshake.
- **One real side effect:** in LEGACY mode, work that the app's `Application` (Hilt, WorkManager
  initialisation in `App.onCreate`) posts to the main looper runs at once instead of being paused. A
  start-up failure in that mode would be loud, not a false pass, but the plan has no named branch for it
  (warning A-W3).
- **A cleaner lever?** No download- or build-file-free alternative is clearly cleaner:
  - another SDK changes `Build.VERSION.SDK_INT`, and below 24 `setSniHost` leaves the code path under
    test (`DefaultTrustedSocketFactory.java:192`);
  - `@Config(instrumentedPackages=…)` would instrument the class under test;
  - graphics or resources modes are heavier.
  
  `@LooperMode(INSTRUMENTATION_TEST)` is a non-deprecated value that also changes the key, and its
  start-up behaviour is closer to the suite's default PAUSED mode. It runs the test body on a separate
  test thread, which these tests do not care about. Either value is acceptable (A-W3).
- **Is it honest?** Yes, if it is explained. The plan says so in A1.c ("only to give the class its own
  sandbox"), but the Javadoc list in A1.c does not require the explanation (warning A-W1).
- **The guards.**
  - A1-G1 and A1-G2 are deterministic, because sandbox separation is by key. They are mutation-proof as
    the plan argues: no listed mutation adds or removes DHE suites or `SSLv2Hello`.
  - A1-G3 depends on what else ran in that JVM. Gradle hands classes to the four forks dynamically, so if
    a present or future Conscrypt-mode test creates a SunJSSE socket, A1-G3 could fire in some runs and
    not others. It fails with its own name, never as a false pass. Today no test does this: U-063's
    Conscrypt-mode classes get `SSLContext` "TLS" (Conscrypt), and `KeyManagerFactory` SunX509 does not
    initialise `NamedGroup`. Record it as a known tripwire (note A-N3).
  - The reverse direction (the JDK class initialising the JDK TLS classes cleanly with Conscrypt off,
    then Conscrypt tests running) is harmless. The forward ordered suite (Handshake, Hardening, then the
    JDK class) exercises exactly ON, then OFF, in one JVM.
  - **Gap:** the `@Before` order. If A1-G1 runs first, its reflective read of `ENABLED_CIPHERS`
    initialises `DefaultTrustedSocketFactory`. Its static block catches only `Exception` (`:108`), so a
    poisoned `NamedGroup` surfaces as an unnamed `ExceptionInInitializerError` instead of the A1-G3
    message (warning A-W2).
- **The suite mixing two sandboxes (IC-9).** Each child keeps its own `RobolectricTestRunner`, which
  obtains its sandbox from the shared `SandboxManager` by key. A JUnit 4 `Suite` with children in two
  sandboxes therefore runs them correctly, in declaration order, in one JVM.

**4. The tests in the new class.**
- On JDK 25 the SunJSSE facts hold:
  - supported `[TLSv1.3, TLSv1.2, TLSv1.1, TLSv1, SSLv3, SSLv2Hello]` and enabled `[TLSv1.3, TLSv1.2]`
    (`I0c-diag.txt:7-8`);
  - the factory's socket enables `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3, SSLv2Hello]` (`:15`), which equals
    supported minus SSLv3, with the recorded order. `jdk.tls.disabledAlgorithms` does not filter
    `setEnabledProtocols` (`:15` shows TLS 1.0 and 1.1 enabled), and the tests assert enabled, not
    negotiable.
- Both preconditions fail **as test failures with the named message**, not as assumptions that skip.
  On a JDK where supported equals enabled (A1-P1), or where TLS 1.0 or 1.1 is no longer supported or is
  enabled by default (A1-P2), the result is a red test with a clear message, never a silent pass.
- **M-D11 is killed by both JDK tests.** With `getEnabledProtocols`, the list becomes `[TLSv1.2,
  TLSv1.3]`, so the set check and the TLS 1.0/1.1 check both fail. M-D2, M-D5, M-D8, M-D12 and M-D13 also
  fail the first JDK test, as A1.g says.
- **AC-3 and AC-4 are moved, not duplicated.** I2 adds only AC-1 and AC-2 (plan line 1067 overrides the
  old bullets). AC-3 and AC-4 live only in the JDK class.
- **The I2b pre-check** follows the I6 protocol: write-ahead `M-A4-precheck`, Edit, TR, `git checkout` of
  one file, hash check. It is excluded from the 35.
- Both branches are complete, with the right counts:
  - main: 4 tests, 688 at I2b, 710 final;
  - fallback: 2 tests, 686, 708, with M-A3 and M-A4 not applicable and M-A1 expecting AC-1 only.
- Two recovery details do not fit I2b (warning A-W4).

**5. Counts, re-derived.**

| Count | Main | Fallback |
|-------|------|----------|
| Tests | 2 + 4 + 9 + 8 + 5 = **28**; 682 + 28 = **710** | 2 + 2 + 9 + 8 + 5 = **26**; **708** |
| Per increment | 684 / 688 / 697 / 705 / 710 | 684 / 686 / 695 / 703 / 708 |
| Option B | 711 tests, 5 skipped (two legacy tests ignored, one ignored desired-behaviour test added) | 709 tests, 5 skipped |
| I7.3 repeat run | 12 + 9 + 8 + 5 + 4 = **38** | **36** |
| Mutations | **35** | **33** |
| Gradle runs in I6 | 35 + 6 + 3 = **44** | **42** |
| Suite children | **7** | 7 |
| Turns | **12** | 12 |
| New files | **6** | 6 |

All of these match A1.f. Stale numbers remain in places that drive no step, plus three operational
commands that rely on an "add the JDK class" instruction rather than carrying it (warning A-W5).

**6. Mutation table.**
- M-D11 is killed by name (both JDK AC-16 tests) in a targeted run that includes the JDK class (A1.g
  class runs).
- The only accepted non-detection is the default-provider second AC-16 test (`NOTED-AC16`), which is the
  story's own exception. A M-D11 that fails no JDK test is `NOT DETECTED` (A1.g, last paragraph).
- Every mutation keeps a single home sub-turn.
- M-A3 and M-A4 are handled in both branches.

**7. Carried-forward items (A1.h).** They are binding inside the increments:
- I3 (plan lines 1161-1163) carries A1-G2, A1.h item 4 (DP-2 draft content, including the SunJSSE
  finding and the `jdk.tls.disabledAlgorithms` caveat) and RFC 8996.
- I4 (lines 1241-1242) carries A1-G2 and A1.h items 1 to 3: the fresh TLS context per call, TLS 1.3
  only, RFC 5280 and RFC 6125 section 6, and "After the CR-009 move".
- I5 (line 1270) carries items 1 to 3.
- The JDK class carries them through A1.c's Javadoc list.
- The no-mock trust-manager rule is item 6.

This matches the story's accepted list exactly (contract-verification W2 as the orchestrator narrowed
it; N6; the expert-depth additions). Not carried, consistent with the orchestrator's rulings rather than
an A1 defect: contract-verification W2's Group A section-comment paragraph and its "if upstream's trust
manager is adopted" clause; the expert-depth tagging of assertions as behavioural or
implementation-coupled; and the second-connection test suggestion for Part B (note A-N2). The I2 text does
not repeat A1.h item 3's RFC 6066 citation for the SNI section comment (warning A-W6).

**8. Consistency.**
- The increment order (I2, I2b, I3), 12 turns, the I2b resume rule, and 7 suite children with both
  sandboxes are consistent across A1, the Count Reference, I7.2, I7.4 and the verification map.
- The I7.1 greps allow `ConscryptMode` and `LooperMode` only in the JDK class and forbid `startHandshake`
  there.
- The qualified-annotation grep does not match the short forms `@ConscryptMode(OFF)` and
  `@LooperMode(LEGACY)`, nor the static import lines, which carry no at-sign.
- Residue: A-W4 and A-W5.

**9. Contracts: no change to the conclusions of `contract-verification.md`, and no re-run is needed.**
- CNTR-MODERNIZATION-001 binds the production producers. A1 changes no production code and no producer
  behaviour, and it adds no trust manager. The JDK class makes no handshake and sets no global state.
- The second provider exists only in test code, through Robolectric's own per-test switch.
- The moved AC-3 and AC-4 characterise the same producer method (`setSniHost`), which the contract does
  not state (Record 6).
- V5 is server-side test harness.
- One item is relevant to a future contract statement rather than to this gate. The wire facts (on the
  implicit shape, Conscrypt sends the connect address's name whatever `setSniHost` set) bear on Record
  6's planned SNI statement. A1.i routes them to a follow-up. The orchestrator should make sure that
  follow-up reaches the author of the CNTR-MODERNIZATION-001 revision (note A-N2).

**10. Left to improvisation.** Four places: the `@Before` guard order (A-W2), application start-up under
LEGACY (A-W3), RECOVER-I6 at I2b (A-W4), and the I7 commands that must be extended by hand (A-W5). Each
has a concrete fix below.

### Blockers

None.

### Warnings (changes required; Developer or orchestrator, no re-planning)

**A-W1. Javadoc must explain the looper-mode annotation.** Plan lines 158-166 (the A1.c Javadoc list)
and 128-129. Add a required item. Suggested text: "`@LooperMode(LEGACY)` is here only to give this class
its own Robolectric sandbox. Robolectric 4.15.1 keys sandboxes on looper mode but not on Conscrypt mode,
so without it the factory's static cipher and protocol lists would be shared with the Conscrypt-mode
classes and computed under whichever provider ran first. Guards A1-G1 to A1-G3 check this on every run."
If the I2b turn chooses `INSTRUMENTATION_TEST` instead (A-W3), the text changes accordingly.

**A-W2. Fix the order of the guards in the JDK class's `@Before`.** Plan lines 209-231 and 1104. Require
A1-G3 (one unconnected `defaultSocket()`, catching `NoClassDefFoundError` and
`ExceptionInInitializerError`) to run **first**, before anything touches `DefaultTrustedSocketFactory`.
After it run the provider check of A1-G1, and only then the reflective `ENABLED_CIPHERS` part of A1-G1.
Also catch `ExceptionInInitializerError` around that reflective read and fail with "A1-G3 (via factory
static init)". The factory's static block catches only `Exception`
(`DefaultTrustedSocketFactory.java:108`), so a poisoned JDK TLS class would otherwise surface unnamed.

**A-W3. Add a named branch for application start-up under the chosen looper mode, and record the
deprecation.** Plan lines 125-129 and 170-188.
- LEGACY is `@Deprecated` in 4.15.1. Record the expected `javac` deprecation note so it is not mistaken
  for a new warning.
- In LEGACY mode, `App.onCreate`'s Hilt and WorkManager start-up runs its posted main-looper work
  eagerly. If the I2b TR fails during application creation, that is a **stop and report**, not a fix in
  the turn. Two remedies are open to the orchestrator: `@LooperMode(INSTRUMENTATION_TEST)` (not
  deprecated, also a distinct key, start-up like the default PAUSED mode), or a plain `Application` for
  this one class through `@Config(application = …)`, which is not part of the sandbox key. Either way the
  guards still prove the isolation.

**A-W4. RECOVER-I6 and Notes item 8 do not fit I2b.** Plan lines 321-323, RECOVER-I6 (about 990-1014),
and Notes item 8 (about 2147-2152).
- When RECOVER-I6 runs for a lost I2b pre-check, step 4 ("compare the U-063 test files with the last END
  manifest") always finds the half-built JDK class and stops.
- Step 5 expects 710 or 708, where the right figure at I2b is 684 before the class or 688 after.

**Change:** for I2b, RECOVER-I6 step 4 compares against the I2 END manifest plus the single path
`…/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`. Step 5 is skipped, because the resume rule then
deletes the class and redoes I2b from the start. Extend Notes item 8's "dispatch nothing else" to a
session lost during the I2b pre-check, because a vendored file may carry M-A4 until recovery.

**A-W5. Stale or hand-patched numbers and commands.**
- Operational (the Developer must edit the command, so state it in the command):
  - the I7.1 IC-8 `cat` list (line 1453) lacks the JDK class (line 1447 says to add it);
  - the I7.3 IC-6 command (line 1474) lacks it (line 1477 says to add it);
  - the I7.6 class-name `find` (line 1549) omits it (it contains `Test`, so the check holds, but the
    list should be complete);
  - I7.4(c) (lines 1520-1523) still says "SNI 14" and then corrects itself to 12.

  Write the six classes, 12 and 38 into the commands and text themselves.
- Descriptive only:
  - line 676: "six child classes", now seven;
  - R14 (line 1818): "48 of them in I6 … 10 baseline runs … 11 turns", now 44, 6 and 12;
  - Quality Attribute Coverage (line 1827): "26 tests and 3 new Robolectric classes", now 28 and 4;
  - Test Strategy (line 1849): "26 tests", now 28.

  Correct them so that the Count Reference rule ("the table wins") is not needed.

**A-W6. RFC 6066 in the SNI-test section comment.** Plan line 1067-1070 (I2) sends AC-3 and AC-4 to the
JDK class, but it does not repeat A1.h item 3's requirement that the SNI-test additions cite RFC 6066
section 3. Add one clause to I2 step 2's section-comment instruction.

### Notes

**A-N1. Wording slips in A1.a.**
- Variant 2 was skipped, not tried.
- The D5b row should say that `sni_obs_ipv6` and `sni_obs_pinned_implicit` match the continue rows D5a2
  and D5e.
- "D5c (pinned)" should read "D5e".
- The two extra probe lines (`:12`, `:65`) are the accepted record-only keys.

None of these changes a decision.

**A-N2. Items to route outside this story.**
- The wire facts, to the CNTR-MODERNIZATION-001 revision's SNI statement (Record 6).
- The not-carried expert-depth suggestions, already recorded by the orchestrator as Implementation
  Notes material: assertion tagging, the second-connection test for a cached `SSLContext` after the move,
  and the full contract-verification W2 wording for the upstream-trust-manager case.

**A-N3. A1-G3 is a permanent tripwire.** Any future Conscrypt-mode test that creates a SunJSSE socket
would make the JDK class fail with the A1-G3 message, possibly only in some runs, depending on which fork
it lands in. The Javadoc should say that this failure points at the other test, not at the factory.

**A-N4. A1-G1's DHE marker.** It depends on SunJSSE's default suites including `TLS_DHE_` (true on JDK
25, `I0c-diag.txt:14`). A future JDK that drops DHE from its defaults would fail A1-G1 with an isolation
message that misnames the cause. The failure is loud, and the message should also print the provider and
the first entries of `ENABLED_CIPHERS` so the cause is clear.

### Effect on the gate result

None. The frontmatter (`verdict: PASS`, `iteration: 3`) stands. A1 is sound, and warnings A-W1 to A-W6
are text and order changes inside I2b and I7. They should be applied before or during I2b (A-W1 to A-W4)
and before I7 (A-W5); A-W6 at I2.

## Phase Completion Report (A1 review)
---
story_id: "U-063"
phase: "plan-gate/plan-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-020/U-063/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
