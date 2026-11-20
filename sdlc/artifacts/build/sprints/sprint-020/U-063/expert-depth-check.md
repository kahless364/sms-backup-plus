---
artifact_type: gate-result
gate: expert-depth-check
gate_id: "expert-depth-check"
checkpoint: plan-gate
pipeline: story-build
story_id: "U-063"
artifact_under_review: "sdlc/artifacts/build/sprints/sprint-020/U-063/plan.md"
producing_agent: amp/agents/solution-architect
verdict: FAIL
severity: warning
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
agent: "Solution Architect (separate instance, expert-depth reviewer)"
iteration: 3
timestamp: "2026-11-17"
produced_at: "2026-11-17T20:44:05Z"
recommendations_count: 0
criterion_results:
  - criterion: "Pattern grounding"
    verdict: deficient
    deficit_present: true
  - criterion: "Anti-pattern recognition"
    verdict: deficient
    deficit_present: true
  - criterion: "Trade-off articulation"
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "Citation depth"
    verdict: deficient
    deficit_present: true
  - criterion: "NFR and quality-attribute coverage"
    verdict: satisfied
    deficit_present: false
  - criterion: "Tooling evidence"
    verdict: satisfied
    deficit_present: false
  - criterion: "Sequencing rigor"
    verdict: satisfied
    deficit_present: false
  - criterion: "Anti-AI-slop check"
    verdict: satisfied_with_recommendation
    deficit_present: false
---

# Gate Report: gate-result — U-063 (iteration 3)

## Gate: expert-depth-check @ plan-gate

## Verdict: FAIL (severity: warning — recorded, does not block implementation)

## Summary

The plan is analytically deep, specific to the code, and largely correct. I re-read the vendored `ssl/`
classes, `PinnedCertificateSocketFactory.java`, `app/build.gradle` and `robolectric.properties`. Every line
reference I checked matches the base commit. The probe and decision table, the fixture design and the
mutation protocol are at or above what a senior TLS-testing engineer would write. The gate still fails
on the rubric's literal terms. The plan names no canonical pattern, no canonical anti-pattern and no
canonical source anywhere, and it does not say that none apply. So Criteria 1, 2 and 4 are deficient,
even though the plan applies those patterns in substance throughout. Each deficit can be fixed by the
orchestrator or Developer adding a short section (see the remediations). No re-planning is needed. Under
Criterion 8 I also list the additions a TLS expert would make. The two most important are below.

1. **Enabled is not the same as negotiable.** The plan never records the JDK's
   `jdk.tls.disabledAlgorithms`. On a recent JDK this can make TLS 1.0 and 1.1 enabled on the socket but
   impossible to negotiate. That matters for the DP-2 bug draft and the D7c reading.
2. **Session resumption.** The plan relies, without saying so, on a fresh client `SSLContext` per
   `createSocket` call. A resumed session would bypass `checkServerTrusted`, so this is the assumption
   that makes the rejection tests meaningful after the CR-009 move.

These do not repeat plan-review D-W1 or contract-verification W1 and W2. Where they touch W2 (assertions
tied to the vendored trust manager), the angle here is different: which assertions overspecify behaviour
rather than which seams must be retargeted.

## Verification of plan claims against the code (this session)

| Plan claim | Checked against | Result |
|---|---|---|
| DTSF lists `:37-89`; static probe `:91-118` (`:107` supported protocols, `:116`); `reorder` `:124-148` (`:129` null guard, `:145` unknown appended); `createSocket` `:152-180` (`:155`, `:172`, `:176`, `:177`); `hardenSocket` `:182-189`; `isIpOrBlankHostname` `:216-226` (`:221-223`, `:225`); `setSniViaSSLParameters` `:238-254` (guard `:239-243`, try `:245-253`, catch of `IllegalArgumentException` only at `:248`) | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` | All correct. A new `SSLContext` is built on every `createSocket` call (`:165-166`), which is relevant to the session-resumption point below |
| TMF `defaultTrustManager` private static non-final `:33`; `checkServerTrusted` `:68-94` (`:76`, `:77`, `:91`, `:92`); `getInstance("X509")` `:106`; verifier `:144`; marker text `:147-148`; `getPeerCertificates` `:164-166` (M-B4 edits `:165`) | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/TrustManagerFactory.java` | All correct |
| PCSF host not stored `:67-72`; `createSocket` `:75-86` with no hardening or SNI; fingerprint check `:141-145`, message "Pinned certificate mismatch"; `checkValidity` `:149`; field `:57` | `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` | All correct |
| JUnit 4.13.2, Robolectric 4.15.1, Truth 1.4.4, mockito-core 5.18.0 at `:159-163`; filter `**/*Test*.*` at `:237`; mail rule `com.zegoggles.smssync.mail*` LINE 0.70 at `:368-376`; `-Werror -Xlint:unchecked` | `app/build.gradle:159-163`, `:237`, `:370-374`, `:203` | Correct |
| Robolectric runs API 29 | `app/src/test/resources/robolectric.properties:8` | Correct |

---

### Criterion 1 — Pattern grounding

**Verdict:** deficient

**Evidence present:** The plan makes several test-architecture decisions and applies well-known test
patterns in substance:
- characterisation tests that pin today's behaviour before a move (Story Overview: "Suggested Approach:
  'characterisation tests first'");
- a reflective write into the private static `TrustManagerFactory.defaultTrustManager` as the trust-anchor
  substitution point (I4 "Global state", P8);
- per-test fresh sockets and servers, with a precomputed certificate fixture class (`TlsTestFixtures`,
  Deviation 11);
- a custom assertion helper (`assertRejectedByCertificateCheck`, I4);
- `@Before` leak guards (I4);
- a test-owned host-name verifier (`SanDnsHostnameTestVerifier`, I4);
- hand-applied mutation analysis (I6).

The plan's only use of the word "pattern" in this sense is in Requirements and Standards Conformance
(line 1389): REQ-MODERNIZATION-003's rule "is the pattern this story follows".

**Evidence missing:** No pattern is named from a canonical catalogue, and the plan does not say why
naming one would not help. The rubric's FAIL condition applies directly: "the subject clearly involves
canonical patterns but they are not named." Characterisation testing and seams are the defining
vocabulary of this exact kind of work. The doctrine's own Modernization row asks for "characterization
tests per Feathers (Chapter 2)" and "seams per Feathers (Chapter 4)".

**Passage:** Story Overview (lines 27-39); I4 "How the tests reach the code under test" and "Global
state" (lines 856-894); Deviation 11 (lines 186-189); Test Strategy (lines 1502-1513).

**Recommendation / Remediation:** Add a "Test Patterns Applied" table to the story's Implementation
Notes, or as a short addendum section under Test Strategy at Developer turn I1 (no re-planning). Name
each pattern, where it is applied, and why it was chosen:

| Pattern (catalogue, chapter) | Where applied in the plan | Why this one |
|---|---|---|
| Characterization Test (Feathers, *Working Effectively with Legacy Code*, Ch. 13) | All 26 tests; the Criterion Status Rule | The oracle is today's behaviour, not a specification. That is why the second AC-16 test asserts behaviour the DP-2 bug may later call wrong |
| Seam and enabling point (Feathers Ch. 4, "The Seam Model") | The static field `defaultTrustManager` (seam); the `@Before` reflective write (enabling point) | The only seam available without a production change (Stop Condition 9) |
| Introduce Static Setter (Feathers Ch. 25), done by reflection instead | I4 set-up | The refactoring cannot be applied because production may not change. Reflection gives the same enabling point with no production edit, at the cost of coupling to a private name (see Criterion 2) |
| Back Door Manipulation (Meszaros, *xUnit Test Patterns*, Ch. 18) | P8 and the I4 field write | State is set through a back door because the front door (the system key store) cannot be configured in the JVM |
| Fresh Fixture, with a Prebuilt Fixture for certificates (Meszaros Ch. 18 and Ch. 20) | Loopback server and sockets per test; `TlsTestFixtures` generated once, offline | Fresh sockets avoid Interacting Tests. Prebuilt certificates avoid generating keys at test time, which would depend on the provider |
| Guard Assertion and Custom Assertion (Meszaros Ch. 21) | `@Before` leak guards; `assertRejectedByCertificateCheck` | The guards turn a leaked global into a precise failure. The custom assertion keeps the `handleSslException` precondition in one place |
| Fake Object (Meszaros Ch. 23) | `SanDnsHostnameTestVerifier` | A working, simplified stand-in for Android's verifier. It is stricter, so it cannot widen trust |
| Mutation analysis (DeMillo, Lipton and Sayward, "Hints on Test Data Selection", IEEE Computer 11(4), 1978) | I6, 35 mutants | Shows each test is sensitive to the behaviour it claims. Mutants are applied by hand because PIT would be a new dependency (DP-1) |

### Criterion 2 — Anti-pattern recognition

**Verdict:** deficient

**Evidence present:** The plan clearly understands and mitigates several hazards:
- tests that pass for the wrong reason (R4, D3a and D10e', M-B3 and M-B4);
- leaked global state (R5, the leak guards, the `@After` identity asserts, the ordered suites);
- flakiness (R6: no sleeps, no retries, port 0, bounded blocking calls);
- the provider masking a mutation (R8, D7a, D7b, D7d);
- the plan deliberately does not use the existing production test hook `getTrustManagerForTesting()`
  (I5, IC-10).

**Evidence missing:** None of these is named as the canonical anti-pattern it guards against. The plan
has no statement that an anti-pattern analysis was done. Two anti-patterns are visibly present in the
subject and are not identified as such:
1. **Global mutable state.** The trust anchor of every default-factory connection is a private static
   non-final field (`TrustManagerFactory.java:33`), read on every call (`:65`, `:76`, `:97`). The
   host-name verifier is the JVM-wide `HttpsURLConnection` default (`:144`). The tests have to reach
   into both. This is the source of the Interacting Tests and Erratic Test risk (Meszaros Ch. 16, Erratic
   Test, cause "Interacting Tests"). The plan mitigates it well but does not name it. It also does not
   record it as a structural finding for the CR-009 move, which is when it could be removed (for
   example by constructor injection of the trust anchor, Meszaros Ch. 26 "Dependency Injection").
2. **Test Logic in Production** (Meszaros Ch. 15). `PinnedCertificateSocketFactory.getTrustManagerForTesting()`
   (`:94-96`) returns a new trust manager, not the one `createSocket` installs (`:77`). So the existing
   tests checked an object production never uses. Contract-verification N4 notes the gap that AC-21
   closes, but neither document names the smell. The plan's choice to bypass it is the correct response
   and deserves to be stated as such.

The planned tests also carry two anti-pattern risks that the plan does not call out:
3. **Fragile Test, cause "Overspecified Software"** (Meszaros Ch. 16):
   - The AC-6 and AC-8 versus AC-9 and AC-10 discriminator is the English text `was not verified against
     the server certificate` (`TrustManagerFactory.java:147-148`). The assertion helper also requires
     the class `com.fsck.k9.mail.CertificateChainException`.
   - The AC-16 order check asserts protocol array order. The story's SC-5 itself says JSSE does not read
     that order as a preference, and M-D5 exists only to detect it. A reordering of the protocol array has
     no runtime effect, yet it fails a test.
   - Contract-verification W2 covers the seams that must be retargeted. The point here is different:
     which assertions encode behaviour, and which encode the wording of today's implementation.
4. **Mystery Guest / Obscure Test** (Meszaros Ch. 15). Ten Base64 blobs in a separate class decide every
   handshake outcome. The plan mitigates this with Javadoc provenance and fingerprints (I1). That is
   good, but it should be named, so that reviewers know why the provenance block is mandatory.

**Passage:** Risks & Mitigations (lines 1456-1475); Global and Static State (lines 1271-1291); I4 assertion
helper (lines 896-902); Group D table row for `createSocket_enabledProtocols_…` (line 837); I5 preamble
(lines 927-930).

**Recommendation / Remediation:** Add an "Anti-pattern analysis" paragraph to the story's Implementation
Notes (orchestrator, before I4), naming items 1 to 4 above with their Meszaros chapters, and:
- Record item 1 as a design input for the CR-009 Part B story: inject the trust anchor and the verifier
  into the moved factory, so that the reflective Back Door goes away. Use a change-record addendum, not
  a bug: nothing is wrong at run time.
- For item 3, have the Developer tag each assertion in the class Javadoc (AC-22) as either
  **behavioural** (must hold after the move) or **implementation-coupled** (may be re-expressed in the
  move, with the reviewer's agreement).
  - Behavioural: the control completes; the rejection is an `SSLException` with a direct
    `CertificateException` cause; wrong-host can be told apart from a bad chain; the blocked and known
    sets; the cipher order.
  - Implementation-coupled: the marker text; the `CertificateChainException` class; protocol array
    order; reflected constant names.
  - This classification complements contract-verification W2's "After the CR-009 move" paragraph, and
    the two can be one Javadoc block.

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied_with_recommendation

**Evidence present:** Most material decisions carry all four elements:
- **Not driving `ImapConnection`** (lines 856-872):
  - gained: direct, DNS-free calls in both shapes;
  - gives way: "What the direct calls cannot show is that `ImapConnection` itself calls the factory";
  - alternative: driving `open()`;
  - why not: DNS at `:186`, private `startTLS` at `:308`, JVM-wide security properties at `:171-183`.
- **Ordered suites versus `--tests` or JUnit XML timestamps** (Deviation 9): the alternative and the
  reason ("too coarse to show order") are both stated.
- **Algorithm `X509` versus `PKIX`** (Deviation 8): gain, alternative and reason are stated.
- **F subtraction** (Deviation 7 and its W1 bounds): what is given up, and how the bounds stop it from
  weakening a mutation.
- **No JUnit timeout rule** (line 915): the reason is Robolectric's main-thread model.
- **`--rerun` versus `--rerun-tasks`** (Deviation 5).
- **The W6 fallback**: its two consequences are stated.

**Evidence missing:** Two decisions are stated as directives without the "gives way" or the "alternative
not chosen":
1. **Trust-anchor substitution by reflective write into the private static field** (I4). This is the
   most consequential test-architecture choice. The plan inherits it from story E4 and does not weigh it
   against the alternatives a JSSE engineer would consider:
   - setting the `javax.net.ssl.trustStore` system property before `TrustManagerFactory` is first
     initialised in the sandbox. The plan bans system properties (I7.1 grep) but never says why: the
     property is JVM-wide, it is read once and cached per JVM, and it depends on class-initialisation
     order across forks;
   - registering a test `Security` provider for `TrustManagerFactory.X509`. This is rejected implicitly by
     the provider-mutation ban, because Robolectric manages Conscrypt itself.

   The cost of the chosen route is also unstated: it couples the tests to a private field name, which is
   W2's issue, and it bypasses the class's own static initialiser.
2. **Fixture format: DER plus PKCS#8 as Base64 constants** (Deviation 11). The gain and the alternative
   are stated. The cost is not: the fixtures are opaque in review (see Mystery Guest above), and the plan
   depends on `keytool -startdate` and the `-ext` forms, which are "assumed" in the Assumption Ledger.

**Passage:** I4 "Global state" (lines 874-894); Deviation 11 (lines 186-189).

**Recommendation / Remediation:** Add two trade-off paragraphs to the story's Implementation Notes,
each covering gained, gives way, alternative, and why not:
- one for the reflective static-field substitution versus a `javax.net.ssl.trustStore` system property
  versus a test provider;
- one for the fixture format, naming the review-opacity cost and its mitigation, the Javadoc provenance
  block.

No plan change is needed.

### Criterion 4 — Citation depth

**Verdict:** deficient

**Evidence present:** Internal citations are exemplary and anchored to the line: contract `:191-196`,
`ImapConnection.java:146-152`, `REQ-MODERNIZATION-019 AC-B8 (:109)`, and every source location in the
Assumption Ledger. The plan makes no canonical-source citation at all, either from the test-design
literature or from TLS standards.

**Evidence missing:** The plan does not state that canonical sources are not applicable, and it plainly
could have cited them. The rubric allows PASS only for anchored citations, or for an explicit "none
applicable" statement, so neither condition is met. For a TLS characterisation story the missing anchors
are domain-material, not decoration:
- **SNI and IP literals:** RFC 6066 §3 ("Literal IPv4 and IPv6 addresses are not permitted in
  'HostName'"). Group A's whole premise rests on it, and the production Javadoc at
  `DefaultTrustedSocketFactory.java:209-210` and `:233` already cites it. The plan does not.
- **Host-name verification semantics:** RFC 6125 §6 (dNSName matching, no CN fallback when a SAN is
  present) and RFC 2818 §3.1. These justify the test verifier's "type-2 SAN only, no wildcard, no CN
  fallback" design as stricter than the real verifier (I4).
- **Certificate path and validity:** RFC 5280 §6.1 (path validation), §4.1.2.5 (validity) and
  §4.2.1.9/§4.2.1.3/§4.2.1.12 (basicConstraints, keyUsage, extKeyUsage). These ground the fixture table's
  extension choices and D10f's "likely causes" list.
- **DP-2:** RFC 8996 ("Deprecating TLS 1.0 and TLS 1.1", BCP 195 update), §§4-5, MUST NOT negotiate.
  Without it the bug draft's severity framing has no normative anchor. The plan leaves severity to the
  Security Reviewer, which is right, but the draft should carry the citation.
- **Cipher blocklist and ordering:** RFC 9325 (BCP 195) §4.1-4.2, for the "known first, unknown last"
  ordering and the RC4 and export bans (RFC 7465 for RC4).
- **Test design:** Feathers Ch. 13 and Ch. 4; Meszaros Ch. 15, 16, 18, 21 and 23 (see Criteria 1 and 2).

**Passage:** whole plan. Most acutely: Group A (I2, lines 764-793); DP-2 (lines 1238-1269); fixture table
(lines 441-455); I4 verifier (lines 891-894).

**Recommendation / Remediation:**
- **Developer, at I2, I3 and I4, inside the AC-22 Javadoc.** Anchor each "what this proves" sentence to
  the clause it characterises: RFC 6066 §3 for Group A; RFC 6125 §6.4.4 for the test verifier's no-CN
  rule; RFC 5280 §6.1 for Group C.
- **Developer, at I3.** Add "Normative context: RFC 8996 §4 and §5" to the DP-2 bug-draft template.
- **Orchestrator.** Paste the Criterion 1 table, which carries the chapter anchors, into the
  Implementation Notes.

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** "Quality Attribute Coverage (ISO/IEC 25010)" (lines 1477-1500) covers all eight
characteristics by name, each with a story-specific finding:
- **Functional Suitability:** each claim is tied to a mutation.
- **Performance Efficiency:** 10-second bounds; durations recorded; "No target is set".
- **Compatibility:** provider-agnostic expectations; `verification-metadata.xml` untouched.
- **Usability:** explicitly not applicable.
- **Reliability:** determinism, proved by three runs plus the ordered suites.
- **Security:** fail-closed proof at handshake level; DP-2; throwaway keys.
- **Maintainability:** the tests are the oracle for Part B.
- **Portability:** JDK 25 / API 29 on any OS; fixtures valid to 2107; device portability explicitly not
  claimed.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied

**Evidence present:** Deterministic signals are cited, and they drive decisions:
- the authoritative `@Test` count (682), from the CLAUDE.md command;
- the JaCoCo rule structure (`app/build.gradle:237`, `:326-336`, `:368-376`), used to conclude that
  `com.fsck.k9.mail.ssl.*` is in no rule and that the `mail` floor can only rise (R12);
- dependency-tree evidence of Conscrypt 2.5.2 and BouncyCastle 1.80 on the test runtime, re-taken at I0a;
- jar-content inspection: no `javax/net` in android-all API 29, and `ConscryptMode` present;
- gate durations from U-061 evidence (R14);
- mutation analysis, which is itself the effectiveness metric;
- coverage deltas I0 to I7 (IC-5).

The I0a step re-takes every baseline rather than trusting earlier sprints.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:**
- **Dependencies are explicit and enforced.** Each increment's start check compares the tree with the
  previous increment's manifest (Working Without Commits).
- **Ordering rationale is stated:**
  - fixtures are generated in I0b, before I1, because "the probe must handshake with the real fixtures"
    (Deviation 4);
  - the probe settles distinguishability before any Group D test is written (Criterion Status Rule,
    rule 4);
  - I6 mutations run only after all 26 tests exist;
  - I7 runs non-build checks, then repeat and order runs, then the clean gate, then the evidence read
    from that gate (lines 1096-1099).
- **Parallelism is ruled out with a stated reason:** foreground only and no helper agents, because of the
  endpoint-security constraint (Shell discipline, R14).
- **The AC-24 split re-sequences explicitly:** I1 shrinks, I4 is dropped, and the count becomes 698.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied_with_recommendation

**Evidence present:** None of the nine doctrine anti-patterns appears at a level that degrades analytical
value.
- **Recommendations are specific and executable.** Every mutation is an exact edit with expected
  failures. Every probe key has a value form and a decision row.
- **Hedging is replaced by measurement.** Each unknown is a probe key with a pre-decided response
  (needs_check list; decision table).
- **No out-of-scope deferral is unjustified.** The device limits trace to the story's "What a desktop-JVM
  unit test cannot show" list. Not driving `ImapConnection` is argued from code (`:186`, `:308`,
  `:171-183`).
- **The TLS reasoning is genuinely expert:**
  - the `direct_cause` precondition of `handleSslException`;
  - the expired leaf placed inside its CA's window, so expiry is its only defect;
  - the unbuffered greeting read, so the layered socket sees the server's first TLS bytes (W9);
  - the F bounds based on the `_WITH_` naming rule for configurable suites;
  - the stricter-than-real fake verifier;
  - the M-E3 host-choice reasoning (W3).

A senior architect would not call this surface-level. The absence of canonical vocabulary is a grounding
defect (Criteria 1, 2 and 4), not slop.

**Evidence missing:** Not slop, but depth a JVM and Android TLS-testing specialist would add:
1. **Enabled versus negotiable.**
   - Probe block P6 and rows D7c and D7e read only socket lists. Recent JDKs list TLSv1, TLSv1.1 and SSLv3
     (and, depending on release, static-RSA and static-ECDH key-exchange suites) in the
     `jdk.tls.disabledAlgorithms` security property. Under SunJSSE a protocol can then be *enabled* on the
     socket and still be impossible to negotiate.
   - So the DP-2 draft's evidence ("the factory re-enables them") is stronger on the desktop JVM than
     what can actually happen there, and D7c may be read as "the provider enables TLS 1.0 by default"
     when it cannot negotiate it.
   - The plan does not record the property.
2. **The session-resumption assumption.**
   - The rejection tests mean "the trust manager rejected this chain" only because no session is resumed.
     An abbreviated handshake does not call `checkServerTrusted`.
   - Today this holds by construction: `createSocket` builds a new `SSLContext` per call
     (`DefaultTrustedSocketFactory.java:165-166`, `PinnedCertificateSocketFactory.java:78-79`), and each
     test uses a fresh port-0 server.
   - A natural Part B refactor, caching one `SSLContext` per factory, would keep the tests green while
     weakening that guarantee. On loopback, two tests can also reuse a port number after release, so the
     same `host:port` session-cache key is possible.
   - The plan does not state the assumption. Nor does the story's "cannot prove" list.
3. **Which TLS version the handshakes run at.**
   - The `OK` value records `protocol=` (R0), but no row or test notes the consequence. The controls and
     rejections almost certainly run at TLS 1.3, because the hardened list appends `TLSv1.3` as unknown
     (SC-5) and JSSE ignores array order.
   - So Groups B and C characterise the TLS 1.3 certificate path only. The TLS 1.2 path, which a device
     server may still choose, is not exercised.
   - That is a legitimate limit, but it belongs in the AC-22 "cannot prove" text. It is not in the story's
     list.
4. **What to cut.**
   - Nothing material. The 63-key probe is large, but every key feeds a row.
   - The one candidate is the protocol-order assertion in AC-16 and its mutation M-D5 (see Criterion 2,
     item 3). It characterises an order JSSE does not use as a preference. The story mandates it, so it
     stays, but it should be tagged implementation-coupled, not deleted.

**Passage:** Probe block P6 and P9 (lines 481, 484); D7c and D7e rows (lines 563-566); DP-2 draft
template (lines 1246-1254); R1 (line 1460); Test Strategy "Not covered" (lines 1512-1513).

**Recommendation / Remediation:** Each is a Developer action within the current plan's structure, no
re-planning:
- (1) In P1, add the record-only key `jdk_tls_disabled_algorithms`
  (`java.security.Security.getProperty("jdk.tls.disabledAlgorithms")`; read only, never set). Route it to
  R0, and handle `EXCEPTION` through D13, as for the other P1 keys. Quote it in the DP-2 draft with the
  sentence "on this JVM, enabled ≠ negotiable for these protocols".
  - Adding a key changes the key count from 63 to 64. The orchestrator should record that as an
    accepted deviation so that D13(c) does not treat the new key as a malformed extra.
- (2) Add one sentence to the Handshake and Pinned test Javadoc: "Each test uses a fresh client and server
  `SSLContext`, so no TLS session is resumed. A resumed session would skip `checkServerTrusted`. If the
  moved factory caches an `SSLContext`, add a test that a rejection still occurs on a second connection
  to the same `host:port`." Copy it to the CR-009 Part B notes.
- (3) Add to the Group B and C "cannot prove" Javadoc: "Handshakes ran at `<protocol from I0 probe>`.
  The other TLS version's certificate path is not exercised."

## Overall Verdict

**Verdict:** FAIL

**Rationale:** The plan's analysis is strong. It is code-verified to the line and has a complete decision
table for every environmental unknown. Its mutation protocol cannot leave an edit behind and treats an
undetected mutant as an unmet criterion rather than a relabelled one. Its ISO 25010 coverage, tooling
evidence and sequencing are all at expert level. It fails this gate on three grounding criteria:
- **Criterion 1 (Pattern grounding):** characterisation testing, seams, back-door fixture manipulation,
  guard and custom assertions, and the fake verifier are applied but never named against Feathers or
  Meszaros.
- **Criterion 2 (Anti-pattern recognition):** the global-mutable-state trust anchor, Test Logic in
  Production in the pinned factory, and the Overspecified-Software risk of the marker text and
  protocol-order assertions are mitigated or avoided but never named. There is also no statement that an
  anti-pattern analysis was done.
- **Criterion 4 (Citation depth):** there are no canonical citations at all, not even RFC 6066 §3, which
  Group A rests on, or RFC 8996 for DP-2, and no statement that none apply.

All three can be fixed by text added to the story's Implementation Notes and the AC-22 Javadoc, as set
out above. Implementation can proceed: this gate's severity is warning. Under Criterion 8, the
enabled-versus-negotiable probe key and the session-resumption statement are the two additions with real
technical value for the CR-009 move.

**Iteration:** 3

**Rework routing (FAIL only):** Routes to amp/agents/solution-architect at step `planning` for iteration
4. Cap is 3 reworks (4 total evaluations). On cap exhaustion, escalates via AskUserQuestion. Because this
gate's severity is warning and every remediation above is additive text that the orchestrator or
Developer can apply without changing the plan's steps, the orchestrator may instead record this verdict
and carry the remediations into the story's Implementation Notes.

## Phase Completion Report
---
story_id: "U-063"
phase: "plan-gate/expert-depth-check"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-020/U-063/expert-depth-check.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
