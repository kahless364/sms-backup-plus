---
artifact_type: gate-result
story_id: "U-066"
gate: plan-review
gate_id: "plan-review"
checkpoint: plan-gate
verdict: PASS
iteration: 1
agent: "Solution Architect (separate instance, plan-review)"
timestamp: "2026-11-21T07:53:21Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-022/U-066/plan.md"
base_commit: "b9fd172fbc397ed43c451379a2c41a9cae1fc605"
blockers: 0
observations: 9
---

# Gate Report: gate-result — U-066

## Gate: plan-review @ plan-gate

## Verdict: PASS

## Summary

I read the story and the plan in full and checked the plan against the code at `b9fd172f`. I did not take the plan's line numbers or quotations on trust.

**Production code.** I applied E1 to E7, word for word, to a scratch copy of `DefaultTrustedSocketFactory.java` outside the repository.
- It compiles with `javac --release 8 -Xlint:all -Werror` against minimal stubs, and also with the app's `--release 17 -Werror -Xlint:unchecked`.
- Doclint gives the same 17 notices as HEAD, so the plan adds none.
- The file stays uniformly CRLF.
- The diff touches no protected range.
- A scratch harness on the build JDK (25.0.5.1) gave the results below.

**Mutation rows.** Every row's `old_string` is unique against the planned file, and every mutant compiles.
- I ran a plain-Java copy of H4, H7, H8 and H9 against HEAD, the plan and each mutant. The Truth assertions were copied verbatim; a recording stand-in replaced Mockito.
- The pass/fail set and the first failing message match the plan's I4 table and its M-X rows exactly.
- The socket-level rows (H5, H6, J1 to J3, J7, C1, C2, HR1, HR2, HL, the K and P handshake tests) I walked by hand through the planned test code and the recorded provider facts. Each named test can fail, and none that must fail is missing.

**Verdict.** No finding would stop the developer from meeting a criterion, make the evidence untrustworthy or leave production code wrong. Nine observations follow. The two worth fixing in the developer brief are O-1 (the Hardening constants block duplicates two existing lines) and O-2 (no saved copy of the pinned factory for M-X5 (i)'s restore). On the plan's Scope Concern P-1, I recommend asking for a ruling now (O-9).

## Findings

### Blockers

None.

### What I checked and found sound

**Production design (plan 2.1 to 2.7).** Results from the scratch harness, on the planned class:

| Check | Result |
|-------|--------|
| Static `ENABLED_CIPHERS` under SunJSSE | L-A |
| Static `ENABLED_PROTOCOLS` | `[TLSv1.2, TLSv1.3]` |
| Step fed the 18 recorded Conscrypt defaults (`I0-probe.txt:30`) | L-A, in the ruled order |
| Step fed the 31 SunJSSE defaults (equal to `I0c-diag.txt:14`) | L-A |
| Step fed the union, forward and reversed | L-A |
| Defaults with no allowed name | `[]` |
| The four-names-by-default case | exactly the three allowed names, in allowed order |
| What the step reads | only `getEnabledCipherSuites` (`getSupportedCipherSuites` is never called) |
| `hardenSocket(s, null, null)` | sets L-A once, then `[TLSv1.2, TLSv1.3]` |
| Same, no allowed default | sets `[]`, then the floor |
| Same, platform query throws | the exception escapes with nothing set (fail closed, C4) |

- **Static initialisation order is right.** `ALLOWED_CIPHERS` is declared textually before the `static {}` block, so it is initialised before the block reads it.
- **Only TLS 1.2 or newer, and only listed suites.** Every path that sets suites goes through the allow filter: the static list, the null branch, the public `hardenSocket` and the pinned call. The protocol line and `protocolsToEnable` are untouched. The server-name round trip is guarded by J1 and probe key D6.
- **The removals are complete.** `git grep` shows that the only readers of `ORDERED_KNOWN_CIPHERS` and `BLACKLISTED_CIPHERS` outside the production file are the six reflective reads in Hardening (`:212`, `:242`, `:257`, `:262`, `:272`, `:274`). The plan retargets all six (3.3, H1 to H4).
- **No subclass exists** (`extends DefaultTrustedSocketFactory`: no match).
- **`reorder` is byte-identical** apart from its comment, and still serves `protocolsToEnable`.
- **The public surface is unchanged.**
  - Reflection on the compiled class: the public methods are the same.
  - Protected members: `ALLOWED_CIPHERS` and `ciphersToEnable` are added; the two removed arrays go.
- **IC-2.** The 30 occurrences in 6 files are confirmed.

**Tests (plan 3.3 to 3.7).** I checked every symbol the new and changed code uses against the files. The helpers it relies on exist with matching signatures:
- `stubSocket`, `Layered`, `closeQuietly`, `firstEntries`, `describeChain`, `connectClient`, `connectPinned` and `LoopbackTlsTestServer.PAYLOAD`;
- `TlsTestFixtures.serverContext(String, String, String)` and `trustManagerFor(String...)`;
- the four-argument server option's constructor and lambda.

Further points:
- **Imports.** Imports removed (`any`, `never`) have no other user. `java.util.List` is added where it is needed.
- **Counts.** Tests per class at HEAD: 12, 9, 14, 10, 14, 1 and 8, a total of 68. With 14 added the seven classes hold 82. The authoritative count is 732. Fourteen added gives 746, or 742 or 738 when a suite is ruled out. The counts are consistent throughout the plan.
- **H5 and H6 cannot pass on provider defaults.** The Conscrypt preconditions are not met by an L-A list, and Conscrypt's own 18 names are not L-A.
- **Provider-fixed suites.** With `ALLOWED_CIPHERS_BASELINE`, `providerFixedSuites` picks `TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384` and still yields exactly the three TLS 1.3 suites (`I0-probe.txt:34`).
- **The JDK guard.** It keeps A1-G3 and the provider check `:159-169`. The DHE clause, `firstEntries` and `CIPHER_ENTRIES_IN_MESSAGES` have no other user and go cleanly.
- **The Hardening and Handshake guards.** They become live-provider checks. Those can fire falsely only if Conscrypt renames itself, which would be a loud stop.
- **When a guard stays silent.** After the change both providers compute identical static lists: L-A and `[TLSv1.2, TLSv1.3]` (I recomputed the protocol list for both providers' supported sets). A guard can therefore stay silent only where nothing provider-dependent is cached.
- **G-1 and G-2 do fire.**
  - Under G-1 both HARD and HSK run with Conscrypt off. The live provider is then SunJSSE, whether Robolectric applies the mode per test or per sandbox, so the new clause fails in every case. In the Handshake class this happens before the trust-manager substitution, so the after-method has nothing to restore.
  - G-2 leaves the JDK class on its own LEGACY sandbox with Conscrypt on, so A1-G1 fails.
- **U-064 and U-065 tests.** The only ones changed are those the story's own criteria change: H6 and the null-lists test (AC-7 (a), AC-4), J1 (binding point 5) and the four TLS 1.2 tests through their helpers (AC-5 (a)). The SNI, Expiry, `PinnedCertificateSocketFactoryTest` and `TlsTestFixtures` files are checked as untouched in I3.

**Mutation table (plan I7).** Highlights of the walk:
- **M-X1 (a).** J1 fails on the new literal only; its U-065 comparison passes because both factories share the mutant list. K4, K5, P4 and P5 fail through `fail(...)`; K6, K7, P6 and P7 pass.
- **M-X1 (b), (e).** SunJSSE's defaults lack the name, so the static list stays L-A, as I measured on the mutant. J1 to J3 rightly pass.
- **M-X2.** H4 and H8 pass, as the constant is unchanged. H7 (i) and H9 (a) fail, as I measured.
- **M-X5 (i).**
  - J1 first fails on "AC-4 (d): cipher suites…", because its protocol `containsExactly` compares sets.
  - J7 first fails on "direct socket for host … enabled protocols in order", because SunJSSE leaves an untouched socket at `[TLSv1.3, TLSv1.2]`.
  - C1 and C2 first fail on `assertProtocolFloor` "TLSv1 must not be enabled", after their precondition passes.
- **M-X5 (ii).**
  - J7, C1 and C2 fail on their non-vacuity preconditions.
  - JR1 fails on "TLSv1.2 must come first"; JR2 passes.
  - HL passes, because both sockets are unhardened.
- **M-X6.**
  - C2 fails on its precondition; C1 passes.
  - HL fails on cipher order; J7 fails on "layered … enabled cipher suites in order".
  - K5 and K7 fail; K4 and K6 pass.
- **M-X7.** H9 fails with `WantedButNotInvoked` (an `AssertionError` subclass); nothing else fails.
- **M-X8.** The J1 U-065 comparison passes and the literal fails; J7 passes.
- **M-X9.** Only H7 (iii) and H9 (b) fail, as I measured. Both providers support all ten (key D19 decides).

**M-X4 (i) and AC-9.** The plan says in advance (2.5) that removing the TLS 1.3 names cannot fail the Conscrypt exact-list or handshake tests, because the provider reports them whatever is set (`I0-probe.txt:31-34`). That is right. The mutation is still detected by J1 to J3 (where nothing is provider-fixed), H4, H7, H8 (its TLS 1.3 clause, which I ran) and H9. AC-9's rule is that "a mutation no test detects is a finding", and that rule is not triggered. The story's own M-X4 note already concedes the handshake side. I judge AC-9 met, with a declared deviation that I10 hands to QA.

M-X1 (b) and (e) are **not** deviations. The story's table already scopes (b) to Conscrypt and (e) to AC-3 (b) and (c) (see O-5).

**Probe and decision table (I2, section 5).**
- Every I2 key has a continue row and a stop row: D3 to D19, with D40 as the residual stop. D11 to D15 cover the AC-6 control both ways.
- The decisions D5, D7, D9, D12, D14 and D19 are settled before I3. No row continues where the story says stop: D8 is S0-4's stop rule.
- D40 cannot stop a probe line that matched its expected value.

**Restore protocol (I5, I7).** The plan keeps:
- saved copies and object-store blobs for the production file and the three heavily mutated test files;
- the production-diff hash, a write-ahead entry and a `restored` line per entry;
- a recovery note for a lost session;
- D35's check for a leftover probe file before the final gate.

No gate runs while an entry lacks `restored`.

**Commands.**
- `MSYS_NO_PATHCONV` is unset for Gradle. Every Gradle call is in the foreground with a 600000 ms timeout; U-065's full gate took 1 min 22 s to 2 min 35 s.
- `rm -rf $R` runs before every run, and the read-only init script is used. XML is off in `build.gradle:30`.
- There are no heredocs and no background shells.
- I11 writes its outputs to the work folder, outside the folders it scans.
- Every `git status` and diff check is path-limited (`app`, `k9mail-vendored`, the build files, change-records, contracts, requirements). The other uncommitted work in the checkout (the audit log, `.skill-context.json`, `research/`, DES-MODERNIZATION-014 under `design/modernization`) cannot stop the developer.
- Today the hygiene pattern returns exit 1 over the workspace, with `compiled-pipeline.json` excluded locally.

**IC-1 (section 9).**
- `adb devices -l` comes first.
- The stop rules cover no device, several devices, and an AVD name that is not `Pixel_9_Pro`.
- The `date` format is passed as one quoted argument, and the step checks that `T` is not empty.
- The app is never uninstalled, never installed with `-d` and its data is never cleared.
- The single test SMS is allowed under the product owner's standing statement, with one retry and a "not conclusive" verdict otherwise.

### Observations (non-blocking)

**O-1 (medium): the Hardening constants block duplicates two declarations that follow the region.**
- **Where:** plan 3.3, "Constants (`:118-164`) → replace with" (`plan.md:437`), and the note at `plan.md:537-539`.
- **What:** the replacement block ends with `KNOWN_PROTOCOLS_BASELINE` and `BLOCKED_PROTOCOLS_BASELINE`. Both already exist at Hardening `:166-167`, after the stated region, which ends at `};` on `:164` (`:165` is blank). If the developer replaces exactly `:118-164`, the class declares both fields twice and does not compile. The parenthetical note says these lines "are part of the region", which contradicts the stated range.
- **Effect:** I4 fails to compile. D23 (`plan.md:1927`) allows a fix to the test source and one repeat, so the developer recovers at the cost of one Gradle run, and the I4 evidence then carries a logged compile failure.
- **Fix:** state the region as `:118-167`, or drop the two protocol lines from the block.

**O-2 (medium): M-X5 (i) has no saved copy to restore from.**
- **Where:** I5 saves only `$DTSF` (`plan.md:1738`) and I6 saves only `$HARD $HSK $JDKT` (`plan.md:1747`). Yet I7 step 5 says "Restore at once by `cp` from the pristine copy, whatever the result" (`plan.md:1776`). M-X5 (i) edits `$PIN` (`plan.md:1804`).
- **What:** the recovery note names `git checkout -- $PIN` (`plan.md:1759-1760`), and the clean check compares `$PIN` with `PIN_BLOB`. A careful developer will use that, but the normal-path instruction points to a copy that does not exist.
- **Fix:** add `cp $PIN "$WORK/pristine/"` to I5, or state in step 5 that M-X5 (i) is restored with `git checkout -- $PIN`.

**O-3 (low): M-X5 (ii) narrows the story's "AC-2 tests (exact list…) for each" without declaring it.**
- **Where:** the M-X5 (ii) row (`plan.md:1805`); story AC-9, M-X5.
- **What:** when only the default factory's call is removed, the pinned exact-list tests (H6, J3) cannot fail. The plan correctly lists them as passing and lists the default factory's exact lists (H5, J2) and the parity tests as failing. But sections 2.5, I10 and the verdict declare only the M-X4 (i) narrowing.
- **Fix:** add M-X5 (ii) to I10's declared deviations, so that QA reads that row the same way.

**O-4 (low): M-X2's reason for K1, K2, P1 and P2 passing assumes the server honours the client's order.**
- **Where:** `plan.md:1801`, "the first TLS 1.2 suite offered is still AEAD".
- **What:** this holds under outcome O. If O is false, the server's own preference decides. With an RSA key and the default Conscrypt server list, that is still an ECDHE-RSA AEAD suite, so the row very probably still matches. D33 stops on a mismatch, so nothing passes silently.
- **Fix:** write "{O or PDF}" next to the four tests.

**O-5 (low): the verdict paragraph describes a deviation that is not one.**
- **Where:** `plan.md:2211-2212`.
- **What:** it says the story's table "expects more than can fail" for "M-X1 (b) and (e) on the JDK". In fact the story's M-X1 row asks for the exact-list tests only "under the provider that offers it" ((b) is Conscrypt), and for (e) only AC-3 (b) and (c). The plan's rows (`plan.md:1797`, `:1800`) match the story.
- **Fix:** a wording correction only.

**O-6 (info): two small text slips.**
- H1 has two uses of `BLOCKED_CIPHERS_BASELINE` (Hardening `:206`, `:214`), not "three times" (`plan.md:575`).
- E6's `old_string` is described as "the three lines" but lists four (`plan.md:262`). The four together are unique, as I checked.
- Neither changes the result of an Edit.

**O-7 (info): I9 gives no expected output for its unscoped `app k9mail-vendored` status line.**
- **Where:** `plan.md:1833`.
- **What:** the expected output is six ` M` lines: the production file, four test classes and `LoopbackTlsTestServer`. It can be derived but is not written.
- **Fix:** write it, so that a leftover probe file (`??`) is read as a D35 case, not judged by eye.

**O-8 (info): IC-1 step 1 has no branch for exactly one physical device and no emulator.**
- **Where:** `plan.md:2088-2095`. This is the same gap as U-065 plan-review O2-4.
- **What:** nothing is sent in that case, so it is safe.
- **Fix:** one line, "otherwise stop and ask the owner".

**O-9 (recommendation): Scope Concern P-1, the stale pinned Javadoc. Ask for a ruling now.**
- **Where:** plan section 7, item 1 (`plan.md:2002-2007`); `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:57`.
- **What:** after this story the class Javadoc will say "the same cipher-suite order and blocked list". There is no blocked list any more. `git grep` confirms this is the only stale comment in `app/src/main` and `k9mail-vendored/src/main`.
- **Why I recommend the ruling now:** this Javadoc is the in-code statement of the pinned factory's security behaviour. A reader auditing the pin path would infer a deny-list that no longer exists, for as long as the CR-009 move takes to land, and its date is not fixed. The fix is one phrase, with no behavioural risk, and it can be checked mechanically:
  - the hunk lies inside the Javadoc `:54-62`;
  - `createSocket` is untouched, so story AC-9's M-E4 and M-E5 re-run trigger does not fire;
  - the parity tests C1, C2 and J7 are unaffected.
- **What it costs:** AC-2 (c) and AC-11 (a) forbid any edit to that file. A comment-only edit needs the orchestrator to amend both. AC-2 (c) exists to keep pinned-side logic out, and a Javadoc edit adds none.
- **Recommendation:** the orchestrator rules before I5 whether a comment-only edit is allowed. The suggested text is the plan's own: "the same allowed cipher suites in the same order". If it is allowed, the plan needs three small additions:
  - an exact edit;
  - in I5, the expectation that `$PIN` shows one Javadoc-only hunk;
  - in I9, `$PIN` dropped from `untouched-exit`, plus a check that the `PinnedCertificateSocketFactory` class files from the I6 and I9 builds are byte-identical to those of the I0 build, or an equivalent comment-only proof.
- **If the ruling is declined:** the plan's carry-over to CR-009's checklist is acceptable. It is recorded, owned and dated. This does not justify a FAIL.

## Check of Amendment A1 (2026-11-21)

A scoped check, not a new gate iteration, at 2026-11-21T08:13:50Z. It covers `plan.md` with "Amendment A1 (after the plan gates, 2026-11-21)" at `plan.md:39-66`, against HEAD `b9fd172f`. Methods as before:
- scratch copies outside the repository;
- `javac` from JDK 25.0.5.1;
- a plain-Java copy of the affected tests (Truth 1.4.4 assertions copied verbatim, with a recording stand-in in place of Mockito).

No Gradle was run, and nothing under `app/` or `k9mail-vendored/` changed. **The verdict stays PASS:** the amendment breaks nothing. One optional text correction is given under point 6.

**1. Observations O-1 to O-9: each handled as intended.**
- **O-1.** The constants region is now `:118-167` (`plan.md:562`), and the block ends with the two protocol lines (`plan.md:659-660`). Applied literally, the replacement declares `KNOWN_PROTOCOLS_BASELINE` and `BLOCKED_PROTOCOLS_BASELINE` once each: HEAD `:165` is blank and `:166-167` are exactly those two lines, and the block replaces them by themselves. The re-check of the other regions (`plan.md:667-674`) agrees with the files.
- **O-2.** I5 copies and stores both production files and records `DTSF_NEW` and `PIN_NEW` (`plan.md:1889-1894`). The clean check, the porcelain reading, the restore by `cp` and the recovery note all name both files (`plan.md:1916-1934`). The M-X5 (i) row now restores from the post-E8 copy (`plan.md:1970`). The rule "`git checkout` is never used for a production file" is right: it would discard E1 to E8.
- **O-3.** M-X5 (ii) is declared beside M-X4 (i) (2.5 item 4, `plan.md:281-285`), and its wording is correct: H6 and J3 cannot fail; H5, J2, C1, C2, J7, J1 and the refusal tests detect it.
- **O-4.** The M-X2 row gives the reason "{O or PDF}" (`plan.md:1967`).
- **O-5.** The verdict no longer calls M-X1 (b) and (e) a deviation.
- **O-6.** H1 now says "two times, `:206` and `:214`" (`plan.md:711`), and E6 says "the four lines" (`plan.md:359`).
- **O-7.** I9 lists the seven expected ` M` lines (`plan.md:2018-2029`). They are in `git status` path order, and no `??` line is allowed (D35).
- **O-8.** IC-1 step 1 covers exactly one physical device (`plan.md:2315-2316`): send nothing, stop and ask the owner.
- **O-9.** Taken up by the ruling; see point 2.

**2. E8 and its proof: sound.**
- **The edit.** The `old_string` (`plan.md:416`) is byte-for-byte the whole of HEAD `PinnedCertificateSocketFactory.java:57` and occurs once. The ruling is recorded in the story (`U-066-…md:329`).
- **The result.** Applied to a scratch copy, it gives one hunk, `@@ -57 +57 @@`, whose two lines both start with ` * `. The file stays uniformly CRLF.
- **The new text is true after the story.** "the same allowed cipher suites, in the same order": the pinned factory reaches the allowed list only through the shared public `hardenSocket(SSLSocket)` (`:101`). It names no constant.
- **The class files cannot differ for another reason.** I compiled the HEAD and the E8 source, each against the HEAD and against the planned default factory, under `-g`, `-g:none` and `-g:source,lines`:
  - within each flag set, all combinations gave byte-identical `PinnedCertificateSocketFactory.class` and `PinnedCertificateSocketFactory$PinnedX509TrustManager.class`;
  - the factory change does not leak into the pinned class files, because the class reaches it only through two method references whose signatures are unchanged;
  - no line moves, and javac embeds no source hash (only the file name in `SourceFile`).
- **The build's own class files match.** The debug and release class files already under `app/build/intermediates/javac/…` are byte-identical to my scratch `-g` compile of the HEAD source, so AGP's javac output for this class is reproducible and identical across the two variants.
- **JaCoCo** runs as an on-the-fly agent (`app/build.gradle:247-255`) and does not rewrite these files.
- **No false stop.** The I0-against-I9 comparison of the four files (`plan.md:1543`, `:2007`) cannot stop the developer falsely. A rebuild after the default factory changed, the debug/release difference, line numbers or a source hash cannot make the files differ. Comparing per variant, as planned, is sound.
- **The rest.**
  - D41 (`plan.md:2141`) covers every failure of the proof with a stop and does not invent the ruling's fallback.
  - The file counts agree: two production files at I5 and I7, seven status lines and two `name-only` files at I9, and `$PIN` dropped from I9's `untouched-exit`.
  - `PROD_DIFF` now includes the E8 hunk at every place it is compared.
  - Constraint 9 (`plan.md:2377`) allows only E8 and M-X5 (i).
  - `createSocket` (`:85-104`) is untouched, so M-E4 and M-E5 need no re-run.

**3. H9 (c) and M-X10: correct.**
- **H9 (c)** (`plan.md:889-894`) compiles. It calls `hardenSocket(stub, new String[0], {TLSv1.2, TLSv1.3})` and asserts that exactly one `setEnabledCipherSuites` call carried an empty array. On the planned code it passes.
- **M-X10's edit** (`plan.md:1974`) is unique and compiles.
- **Under M-X10:** H9 (a) and (b) pass, then (c) fails at `verify(emptyList)…` with `WantedButNotInvoked`, an `AssertionError` subclass (A). H4, H7 and H8 pass, as the row says. No socket test reaches the skipped branch, because the static list holds ten names on both providers.
- **Other rows undisturbed.** I walked H9 through every mutant: M-X1 (a) to (e), M-X2, M-X4 (i), M-X4 (ii), M-X5 (ii), M-X6, M-X7, M-X8, M-X9 and M-X10. Each H9 outcome and first failing assertion is as in iteration 1, and under M-X7 case (a) still fails first. Case (c) comes last, so it cannot pre-empt an earlier failure, and it passes wherever H9 passed (M-X5 (ii), M-X6). I4 is unchanged: at HEAD, H9 still fails first at (a).

**4. Counts: consistent everywhere they are stated.**
- Tests: 14 added (8 [R]) and 12 changed (3 renamed); 732 → 746 (or 742 or 738); 82 cases.
- Red run: 17 failures.
- Runs: 15 mutation runs (M-X1 ×5, M-X2, M-X4 ×2, M-X5 ×2, M-X6 to M-X10) plus 2 guard runs, 17 in all.
- Gradle: 22 runs (1 + 1 + 1 + 1 + 17 + 1).
- Checked at `plan.md:35-37`, `:57`, `:63-66`, `:471`, `:1501`, `:1512`, `:1850-1857`, `:1907`, `:1981`, `:1994-1999`, `:2042` and `:2161-2164`. No stale figure remains.

**5. Text-only additions: they changed no code text except E2's Javadoc.**
- Every ` ```java ` block of the amended 2.7 (E1 to E7) appears verbatim in my iteration-1 scratch file once E2's new "Sources: …" sentences are added. The only other blocks are E8's two lines.
- With the amended Javadoc the file compiles under `javac --release 8 -Xlint:all,-options -Werror`.
- All 14 production mutation `old_string`s are still unique, because the new Javadoc quotes no suite name, and all 14 mutants compile.
- Sections 2.0 and 2.1 are prose and tables only.

**6. Nothing accepted in iteration 1 has regressed.**
- I re-ran the planned H4, H7, H8 and H9 against HEAD, the plan and every mutant. They give the same results as before, plus M-X10.
- The production behaviour (L-A on both recorded defaults, the empty case, fail closed) and the public surface are unchanged.
- The probe, the decision rows D1 to D40, the guards, G-1 and G-2, the hygiene and command rules, and IC-1 are unchanged apart from the O-8 branch.

One optional text correction (info, no effect on any step). At `plan.md:174`, "**Undetectable on SunJSSE:** M-X1 (b) and (e)" understates (e), which no provider enables. Exact text: "**Undetectable on SunJSSE:** M-X1 (b), because `TLS_RSA_*` is not in SunJSSE's defaults. **Undetectable at the socket level on both providers:** M-X1 (e), because neither provider enables it, and M-X9, because both support all ten."

## Phase Completion Report
---
story_id: "U-066"
phase: "plan-gate/plan-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-022/U-066/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
