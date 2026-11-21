---
artifact_type: plan
story_id: "U-065"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-21T04:31:10Z"
iteration: 2
base_commit: "b2c2dfd8d7f0f5de8627575e160215b24b7cc5d9"
contracts_verified:
  - CNTR-MODERNIZATION-001
risks_identified: 12
---

# Implementation Plan: U-065 — TLS 1.2 Floor for Both Factories; the Pinned Factory Gets the Default Factory's Cipher and Server-Name Handling

Story: `sdlc/artifacts/build/sprints/sprint-021/U-065-tls12-floor-both-factories-and-pinned-factory-parity.md`.
Base commit `b2c2dfd8d7f0f5de8627575e160215b24b7cc5d9` on `sdlc/modernization-plan` (U-064 is in it). At
planning time `git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle` printed
nothing; the working tree holds SDLC documents only. Records 8 and 9 are in `6cc49ab4` and `bd1d5398`, both
ancestors of HEAD. The product owner's decisions in the story are binding and are not re-opened here.

Two production files change, one test-only helper (`LoopbackTlsTestServer`) gets an additive option, four
test classes change, 19 test methods are added and 7 are changed (renamed). The developer works from this
plan alone.

## 0. Changes in Iteration 2

Iteration 1 went through three gates (results in this folder): `plan-review` FAIL (B-1, O-1 to O-10),
`contract-verification` PASS (O1 to O10), `expert-depth-check` PASS at warning severity (two
recommendations). What the gates cleared is kept: production edits D1, D2, D4 to D6 and P1 to P4 are
unchanged in code (D3 and D5 change in comment text only, see EDC-2); the restore protocol; the counts
(19 added, 7 changed, 713 to 732, 68 cases in the seven classes, 14 mutation entries, 21 Gradle runs).

Every mutation row of I7 was re-derived against the planned test code, naming the assertion that fails and
its failure type. The plain-Java parts were re-checked with a second throwaway `jshell` run on JDK 25.0.5.1
outside the repository (section 1.2): an untouched layered socket enables `[TLSv1.3, TLSv1.2]` (set equal to
the allow-list); M-P2's restored protocols keep the order `[TLSv1.3, TLSv1.2]` through the round trip; M-P3's
widened list is accepted; M-P6 (i)'s swap survives the round trip; the old list of M-P1 and the H3 case (i)
result; M-D11r gives the same `[TLSv1.2, TLSv1.3]` on SunJSSE. The only expected failing set that changed
is M-P7's (J4 removed); every row now also names the first failing assertion, its type, and the tests that
must keep passing where a reader could expect otherwise.

| Finding | Change |
|---------|--------|
| plan-review B-1 | Fix (a): J4 removed from the M-P7 row and from the three other statements (J-doc mutation list, J4 Javadoc, I4 "passing by design" note). Under M-P7 the JDK-provider detector is J7 (order check of `assertSameHandling` on the layered sockets). J4's own detectors are M-P3 and M-P1/M-P8. No count changes |
| plan-review O-1 | The I7 table states the expected failure type of every failing test (assertion, or a named exception class, as `junit_cases.py` prints it). Row D30 stops only on a failing set or a type that differs from the row |
| plan-review O-2 | Row D6 is now a stop after I2 and before I3: the developer reports the probe line and waits; the orchestrator's reading decides between D6-a (write the tests with the exact widened catch given in D6, report AC-8 (b) for that version as met with the observed type, accepted on a date) and D6-b (no tests; report AC-8 (b) for that version as not met, with the probe line). Counts follow D3 or D4 respectively |
| plan-review O-3, O-4 | IC-1 step 1: list devices first; if more than one device is attached, or the one attached emulator's AVD name is not `Pixel_9_Pro`, stop and ask the owner; no command is sent to any other device. The date command is `adb -s <serial> shell "date '+%m-%d %H:%M:%S.000'"` |
| plan-review O-5 | The guard code in both classes is now left byte-identical (H-guard and the Handshake guard edit are withdrawn). The class Javadoc and the Implementation Notes record that the `SSLv2Hello` clause can no longer fire and the DHE clause alone carries the guard, the second option AC-6 (e) names; AC-6 (d)'s "guards pass unchanged" then holds literally. Allowed hunk ranges updated |
| plan-review O-6 | Probe: the cipher suites are reversed before the round trip, so `c_roundtrip_cipher_order_kept` shows that a reordered list survives |
| plan-review O-7 | I10's Implementation Notes list names the `ALLOWED_PROTOCOLS` trade (a future TLS version needs an entry) |
| plan-review O-8 | I5 also stores both blobs with `git hash-object -w` (no tracked change, no commit); the recovery note restores from the object store if the pristine copy is lost. `cp` stays the primary restore |
| plan-review O-9 | `-89,0` corrected to `-90,0`; I1 says four replaced lines; "five test classes" corrected |
| plan-review O-10 | Left as it is: the story's second branch of AC-4 (b) asks only for "no exception"; a layered "no name" check for hosts where the provider also sets none would pass with or without the factory's step and adds no detection. D13 keeps the stronger alternative for the case where the provider sets nothing |
| contract-verification O1, O2, O3, O6, O7 | Added to the hand-off list (section 4, I10 "Items for the orchestrator"); nothing in the records is edited. O7 also corrects 8.1: the change is backward-compatible only by elimination |
| contract-verification O4 | "Silently" removed from 2.2 and R5: CR-009's zero-references check (Fix Order step 5, Fix Checklist) would catch the import if it is run. The suggested CR-009 checklist line, test row T11 and T5 rewording are in the hand-off list |
| contract-verification O5 | 2.1 and I10 record that in the initialiser-failure branch `createSocket` may now throw an unchecked exception (fail closed) |
| contract-verification O8 | 2.2 corrected: only `setSniViaSSLParameters` is documented as public for testing; `setSniHost` is public and has a vendored caller |
| contract-verification O9, O10 | O9: the contract-verification gate read the contract in full; section 12 says so. O10: nothing to do |
| contract-verification (citation) | CR-013 alternatives cited as `:389-411` |
| expert-depth-check Criterion 2 (EDC-1) | 2.2 names the copy alternative as Duplicated Code (Fowler, *Refactoring* 2nd ed., ch. 3) and the encapsulation cost of widening `hardenSocket`; 2.1 names the pre-existing null branch as CWE-636 Not Failing Securely ("Failing Open"), fixed for protocols and deliberately left for cipher suites |
| expert-depth-check Criterion 4 (EDC-2) | RFC 8996 sections 4 and 5; NIST SP 800-52 Rev. 2 section 3.1; the Android `javax.net.ssl.SSLSocket` reference protocol table for the minSdk 21 claim (L6). D3 now keeps the existing comment's facts (TLS 1.1 and 1.2 supported from API 16, enabled by default from API 20) and D5's Javadoc cites them |

## 1. Assumption Ledger

`verified` = read or run in this planning session, with the location. `needs_check` = a developer step with
a decision row for each outcome. Nothing material is left `assumed`.

| # | Claim | Status | Evidence |
|---|-------|--------|----------|
| L1 | HEAD `b2c2dfd8…`, branch `sdlc/modernization-plan`; authoritative `@Test` count 713; Records 8 and 9 committed in `6cc49ab4` and `bd1d5398`, both ancestors of HEAD (`git merge-base --is-ancestor`) | verified | `git rev-parse HEAD`; CLAUDE.md count command; `git log -S'### Record 8'` / `-S'### Record 9'` on the feasibility analysis |
| L2 | Base blobs, equal at HEAD and in the working tree: pinned factory `8a0fbb64fb171624591fbebafb818555f28efd58`, default factory `d543927166b3c5e1da0aaa8ab584072a43bf7521`, `LoopbackTlsTestServer.java` `4eafda53d64522a3a1f579d4a2a5656714571af9` | verified | `git rev-parse HEAD:<path>` and `git hash-object <path>` |
| L3 | Default factory: `ENABLED_CIPHERS`/`ENABLED_PROTOCOLS` `protected static final` (`:34-35`); cipher lists `:37-62`, `:64-81`; `ORDERED_KNOWN_PROTOCOLS` `:83-85`; `BLACKLISTED_PROTOCOLS` `:87-89`; static initialiser `:91-118` (protocols from `getSupportedProtocols()` `:107`, null branch `:108-117`); `reorder` `protected static` `:124-148`; `createSocket` `:152-180` (calls `hardenSocket` `:176`, then `setSniHost` `:177`); `hardenSocket` `private static` `:182-189` (applies nothing for a null list); `setSniHost` `public static` `:191-205`; `isIpOrBlankHostname` package-private `:216-226`; `setSniViaSSLParameters` `public static` `:238-254` (round trip `:244-247`, catch `:248-253`); `setHostnameViaReflection` `:256-266` | verified | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java`, read in full |
| L4 | Pinned factory: `createSocket` `:74-86` (fresh `PinnedX509TrustManager` and `SSLContext` per call `:77-80`; returns the provider socket `:81-85`); imports `:18-33` (no `SSLSocket`, no default-factory import); fingerprint comparison `:141`; validity check `:149`; `getAcceptedIssuers` `:152-157` | verified | `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java`, read in full |
| L5 | App construction sites: `K9MailTransport.java:145` (pinned), `:148` (default); vendored `Transport.java:26`, `RemoteStore.java:52`, `:56`; the only other caller of a default-factory static is `WebDavSocketFactory.java:67` (`setSniHost`, unchanged); `hardenSocket` has no caller outside its class | verified | `git grep` over `app` and `k9mail-vendored` (scope verified via grep) |
| L6 | Robolectric `sdk=29` (so `setSniHost` takes the `setSniViaSSLParameters` branch); `minSdkVersion 21`. Every supported Android version supports TLS 1.2 and enables it by default: the Android developer reference for `javax.net.ssl.SSLSocket` (protocol table) gives TLSv1.1 and TLSv1.2 as supported from API level 16 and enabled by default from API level 20, which the existing comment at `DefaultTrustedSocketFactory.java:102-106` also records | verified | `app/src/test/resources/robolectric.properties:8`; `app/build.gradle:24`; Android `SSLSocket` reference; `:102-106` |
| L7 | JUnit XML is off for every `Test` task (`build.gradle:30`); `maxParallelForks = 4`, `forkEvery = 100` (`:27-28`). The U-063 init script `junit-xml.init.gradle` turns XML on without a tracked edit and prints `U063-DAEMON-JVM` | verified | `build.gradle:25-31`; `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/junit-xml.init.gradle` |
| L8 | JaCoCo `mail*` floor LINE 0.70 (`app/build.gradle:368-374`); the vendored module is not in the class roots (U-064 plan L20) | verified | `app/build.gradle:340-460` |
| L9 | Mockito 5.18.0 and Truth 1.4.4 are test dependencies | verified | `app/build.gradle:161`, `:163` |
| L10 | Test classes at HEAD: Hardening 9 tests (`:178`, `:208`, `:229`, `:244`, `:255`, `:270`, `:335`, `:392`, `:421`), guard A1-G2 `:153-173`, BUG-019 comment `:376-377`; JDK class 5 tests (`:172`, `:191`, `:211`, `:270`, `:311`), guards `:120-166`; Handshake 8 tests (`:206-270`), guard `:366-389`; SNI 14 tests (U-064's two at `:340`, `:379`); pinned create-socket 4 tests (`:123`, `:140`, `:160`, `:180`); Expiry 1 (`:103`); `PinnedCertificateSocketFactoryTest` 8 | verified | each file read in full (the last from U-064's verified count, file unchanged since) |
| L11 | `LoopbackTlsTestServer`: constructor `:79-87`, `start` `:93-95`, `serve` `:97-119` (wraps the accepted plain socket `:107-108`, `setUseClientMode(false)` `:109`, handshake `:110`); no protocol option. `TlsTestFixtures.serverContext` `:354-365`, `trustManagerFor` `:371-387` | verified | both files read |
| L12 | Plain JDK 25.0.5.1 (SunJSSE): an unconnected socket reports server names `null`; a **layered** socket created with host `imap.right.test` already reports that host as server name before any factory step; layered with `host.example.` reports `host.example` (the provider strips the dot); layered with `192.168.1.10`, `::1`, `fe80::1`, the eight-group literal, `bad_host.example`, `-x.example`, `""`, `"   "` and `null` reports none and does not throw; `setEnabledProtocols(new String[0])` is accepted (empty list); after `setEnabledProtocols({TLSv1.2, TLSv1.3})` and a cipher reorder, the `getSSLParameters`/`setSSLParameters` round trip keeps both; supported `[TLSv1.3, TLSv1.2, TLSv1.1, TLSv1, SSLv3, SSLv2Hello]`, default enabled `[TLSv1.3, TLSv1.2]`; the default suites contain `TLS_DHE_` suites and both `TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384` and `TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384`; `jdk.tls.disabledAlgorithms` lists `TLSv1`, `TLSv1.1` | verified (outside Robolectric) | throwaway `jshell` script in the planner's session scratch folder, nothing written to the repository; output in section 1.1. Inside the Robolectric sandbox: needs_check N4 |
| L13 | Conscrypt under Robolectric (U-063): supported = default enabled = `[TLSv1, TLSv1.1, TLSv1.2, TLSv1.3]`; no server names reported; every handshake so far at TLS 1.3; no blocked suite in the defaults; the factory's cipher order differs from the provider's | verified (by reading evidence) | `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/I0-probe.txt:12`, `:23-27`, `:33`, `:47` |
| L14 | `PinnedCertificateSocketFactoryExpiryTest` relies on a fresh context per `createSocket` and a TLS 1.3 handshake; the design keeps `:77-80` byte-identical and TLS 1.3 enabled | verified | Expiry test `:53-55`, `:104-125`; section 3 |
| L15 | Line endings in the working tree: both production files and the SNI test are CRLF; Hardening, JDK, Handshake, create-socket test and `LoopbackTlsTestServer` are LF | verified | `git ls-files --eol` |
| L16 | `compiled-pipeline.json` in this workspace is excluded locally (`.git/info/exclude:38`); the U-065 story file has 0 matches of the hygiene pattern | verified | `grep -n` of the exclude file; count-only `grep -cEi` of the story |
| L17 | Debug APK path is `app/build/outputs/apk/debug/app-debug.apk`; `versionCode 1602` | verified | `ls app/build/outputs/apk/debug/`; `app/build.gradle:26` |
| L18 | Log lines for IC-1: `BackupWorker: starting backup (%d messages)` `BackupWorker.kt:313`, `BackupWorker: backup complete, backedUp=` `:369`, `BackupWorker: unrecoverable error` `:169`; tag `SMSBackup+` (`App.java:93`); the connection is opened only when items > 0 (`:252-255`) | verified | `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` |
| N1 | The desktop Conscrypt server, limited to `TLSv1.1` (and separately `TLSv1`), completes a handshake with a client that enables those versions | needs_check | Probe I2 (keys `c_hs_*`); rows D3 to D7 |
| N2 | Conscrypt `setEnabledProtocols(new String[0])`: accepted or throws | needs_check | Probe I2 (`c_empty_protocols`); row D9 (record only) |
| N3 | Under Conscrypt the `getSSLParameters`/`setSSLParameters` round trip keeps a `{TLSv1.2, TLSv1.3}` list and the cipher order | needs_check | Probe I2 (`c_roundtrip_*`); row D10. Also shown by the changed tests (AC-4 (d)) |
| N4 | Inside the JDK-provider Robolectric sandbox the layered behaviour of L12 holds (the provider sets the layered name itself; a null layered host is accepted) | needs_check | Probe I2 (keys `j_*`); rows D12 to D16 |
| N5 | The Gradle daemon runs JDK 25 | needs_check | I0 (`U063-DAEMON-JVM` line); row D33 |
| N6 | The debug build installs over the emulator's app without losing data (same signing key) | needs_check | IC-1 section 9 (orchestrator); stop conditions there |

### 1.1 Throwaway JDK check run while planning (output, verbatim; plain `jshell`, JDK 25.0.5.1)

```
provider=SunJSSE
unconnected.serverNames=null
unconnected.supported=[TLSv1.3, TLSv1.2, TLSv1.1, TLSv1, SSLv3, SSLv2Hello]
unconnected.enabled=[TLSv1.3, TLSv1.2]
layered[imap.right.test] names=[type=host_name (0), value=imap.right.test]
layered[192.168.1.10] names=null
layered[::1] names=null
layered[fe80::1] names=null
layered[2001:0db8:0000:0000:0000:0000:0000:0001] names=null
layered[bad_host.example] names=null
layered[host.example.] names=[type=host_name (0), value=host.example]
layered[-x.example] names=null
layered[] names=null
layered[   ] names=null
layered[null] names=null
emptyProtocols.accepted enabled=[]
roundtrip.protocols=[TLSv1.2, TLSv1.3]
roundtrip.ciphersOrderKept=true
roundtrip.serverNames=[type=host_name (0), value=imap.right.test]
jdk.supported.has.SSLv2Hello=true
jdk.defaultEnabled.ciphers.hasDhe=true
jdk.defaultEnabled.ciphers.has.ECDHE_RSA_AES256GCM=true
jdk.defaultEnabled.ciphers.has.ECDHE_ECDSA_AES256GCM=true
java.version=25.0.5.1
```

Consequence, decided here and confirmed in the sandbox by I2: on the layered JDK socket the provider sets
the host as server name by itself (and normalises `host.example.`), so the layered shape cannot tell the
factory's step from the provider's. AC-4 (b) takes its second branch: for the layered shape only "no
exception and an `SSLSocket` is returned" is asserted. AC-4 (c): a null layered host is accepted by this
provider, so it is asserted (no exception) on the layered shape too.

### 1.2 Second throwaway JDK check (iteration 2; plain `jshell`, JDK 25.0.5.1; the factory steps re-typed)

The script re-implements `reorder` and the allow-list filter exactly as D5 writes them and applies the
planned mutation edits to plain SunJSSE sockets. Output, verbatim:

```
factory.protocols=[TLSv1.2, TLSv1.3]
MP1.oldList=[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3, SSLv2Hello]
MD11r.fromEnabled=[TLSv1.2, TLSv1.3]
MP1.H3i=[TLSv1.2, TLSv1.1, TLSv1, SSLv2Hello, TLSv1.3]
MP7.layeredUntouched=[TLSv1.3, TLSv1.2] setEqualsAllowed=true
MP2.pinnedOrder=[TLSv1.3, TLSv1.2] factoryOrder=[TLSv1.2, TLSv1.3]
MP3.enabled=[TLSv1.3, TLSv1.2, TLSv1.1, TLSv1, SSLv2Hello]
MP6i.first2=[TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384, TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384]
MS5 bad_host.example IAE
MS5 host.example. IAE
MS5 -x.example IAE
java.version=25.0.5.1
```

What it settles for I7: under M-P7 the pinned layered JDK socket passes J4 (set equal) and fails J7 (order
`[TLSv1.3, TLSv1.2]` against `[TLSv1.2, TLSv1.3]`); under M-P2 the pinned direct JDK socket passes J3 and J4
(sets) and fails J7 (order); M-P3's list is accepted and contains the old names; M-P6 (i)'s swap survives the
server-name round trip; M-D11r gives the same list on SunJSSE (so only H3 case (iii) can detect it); the
three S5 names raise the unchecked `IllegalArgumentException` once the catch is gone.

Planning-session disclosure: read-only `git`/`grep`/`sed`/`ls` commands, two `jshell` runs in the scratch
folder, `date -u`. No Gradle call of any kind.

## 2. Design (decided)

No new architectural pattern. The change applies a single shared policy step (Fowler, *Refactoring*, 2nd
ed., "Extract Function", with the extracted step made callable), positive validation by an allow-list
(OWASP Input Validation Cheat Sheet, "allow-list validation"), and fail-safe defaults (Saltzer and
Schroeder, "The Protection of Information in Computer Systems", design principle (b)). Context: RFC 8996
sections 4 and 5 (TLS 1.0 and TLS 1.1 MUST NOT be used), RFC 6066 section 3 (server name), NIST SP 800-52
Rev. 2 section 3.1 (Protocol Version Support: TLS 1.2 required, TLS 1.3 support), and the Android
`javax.net.ssl.SSLSocket` reference protocol table for what each API level supports (L6).

### 2.1 Where the TLS 1.2 floor lives (default factory)

**Decision: a separate final filter step after `reorder`, driven by a new allow-list constant, in a new
`protected static String[] protocolsToEnable(SSLSocket socket)` that reads the socket's *supported*
protocols.** The static initialiser calls it once; `hardenSocket` calls it again only when the static list
is null.

`protocolsToEnable(s)` = `reorder(s.getSupportedProtocols(), ORDERED_KNOWN_PROTOCOLS, BLACKLISTED_PROTOCOLS)`
filtered to the names in `ALLOWED_PROTOCOLS = {"TLSv1.2", "TLSv1.3"}`, keeping the order. Result on both
test providers: `[TLSv1.2, TLSv1.3]`.

Why this meets AC-1:
- (a) the allow-list admits none of the old four, whatever the provider lists or enables.
- (b), (d) the input is the supported list, so TLS 1.2 and 1.3 are enabled if and only if supported, also
  where the provider leaves one off by default (Record 8 read literally, Scope Concern 3); nothing
  unsupported can be enabled (the filter only removes).
- (d) the step takes a socket, so a test can feed a stand-in socket whose supported and default-enabled
  lists differ: the injected-list test exists, and it detects "built from the enabled list" (M-D11r).
- Unknown names (Scope Concern 7): an allow-list never enables a name it does not know, including a
  future version; enabling TLS 1.4 one day needs one entry in `ALLOWED_PROTOCOLS`. That is the stated
  trade, recorded for the product owner. It reverses the "append unknown items" stance (`:143-145`) for
  protocols only; ciphers keep it.

Constants: `ORDERED_KNOWN_PROTOCOLS` and `BLACKLISTED_PROTOCOLS` are **unchanged** (the SSLv3 block stays,
AC-9 (d)). Therefore `protocolConstants_matchRecordedBaseline`, `reorder_removesEveryBlockedItem` and
`reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder` are **unchanged** (AC-6 (c)). The new constant
gets its own snapshot assertion in the new stand-in test.

Alternatives rejected: (i) adding `TLSv1`, `TLSv1.1`, `SSLv2Hello` to `BLACKLISTED_PROTOCOLS`: a deny-list
lets an unknown old name through (for example a provider-specific `SSLv2`), and it changes the snapshot and
the `reorder` tests (AC-6 (c) prefers unchanged); (ii) building from the enabled list: fails AC-1 under
Conscrypt (the provider enables TLS 1.0 and 1.1 itself) and contradicts "where the platform supports
them"; (iii) a numeric version parse (`TLSv1.N`, N >= 2): admits names whose meaning is unknown and adds
parsing code to a security step for no present benefit.

**AC-1 (e), the fail-open branch.** Today's branch (the exception is logged, both lists stay null,
`hardenSocket` applies nothing, `:108-117`, `:182-189`) is CWE-636 Not Failing Securely ("Failing Open").
Decision: in that branch the floor still applies, computed from the socket being hardened
(`protocolsToEnable(sock)`), which fixes CWE-636 for protocols; the cipher suites deliberately stay as the
platform enabled them (unchanged behaviour; Record 8 concerns protocols, and both factories behave alike
there, so Record 9's parity holds). The connection is not failed on purpose; it fails only if the platform
supports neither TLS 1.2 nor 1.3 (empty list), which no Android version this app supports does (minSdk 21,
L6). One new consequence (contract-verification O5): if, in that branch, the platform throws from
`getSupportedProtocols()` or refuses an empty list (probe N2), an unchecked exception now leaves
`createSocket` itself instead of the handshake. That fails closed and is covered by the contract's lead-in
that errors cross the boundary as exceptions (CR-012 `:198`); the Implementation Notes record it with the
N2 result. How it is
tested: the initialiser cannot be made to fail (it runs once per class loader; the lists are `final`, and
JDK 25 refuses reflective writes to static finals), so the code of the branch is a three-argument
`hardenSocket(SSLSocket, String[] ciphers, String[] protocols)` that the one-argument form calls with the
static lists, and a test calls it with `null, null` on a stand-in socket (mutation M-F1). Implementation
Notes record exactly this: the branch's code is tested; the class-initialiser failure itself is not
forced, and why.

Empty list (story item "What `setEnabledProtocols` does with an empty list"): JDK accepts it (L12) and no
handshake can then succeed; Conscrypt is probed (N2). Either way the connection fails closed; the Javadoc
states both possibilities.

### 2.2 How the pinned factory gets the same handling

**Decision: a call into the default factory's existing step, made callable.** The pinned `createSocket`
calls `DefaultTrustedSocketFactory.hardenSocket(sslSocket)` and then
`DefaultTrustedSocketFactory.setSniHost(socketFactory, sslSocket, host)`, on both shapes, after creating
the socket from its own pinned `SSLContext`. The one visibility change: `hardenSocket(SSLSocket)` goes from
`private static` to `public static`. `setSniHost` is already `public static`. The new
`protocolsToEnable` and the three-argument `hardenSocket` are `protected static` like `reorder` (tests reach
them by reflection, as they already reach `reorder`).

Why the visibility change is safe in vendored code: the method is static and stateless; it only narrows
and reorders what the platform offers on a socket the caller already holds, using lists derived from the
platform; it reads and changes no trust manager, key manager or host-name decision; a caller cannot
weaken another caller's socket with it. The vendored module is in-tree code that is not re-synced from
upstream (U-027), and the same file already carries the BUG-015 fix and public statics:
`setSniViaSSLParameters`, documented as "Extracted as a public static method to allow unit testing"
(`:230-231`), and `setSniHost` (`:191`), which the vendored `WebDavSocketFactory.java:67` calls. The cost
accepted: widening a private method of a vendored module to `public` weakens encapsulation (Bloch,
*Effective Java*, 3rd ed., Item 15 "Minimize the accessibility of classes and members"): the method becomes
part of the class's exported surface for a second caller in another package, and the move must keep or
retarget it. This is a pre-move bridge; once both classes share a package after CR-009, the shared step can
become package-private again.

Why not a copy in the app: a copy is the Duplicated Code smell (Fowler, *Refactoring*, 2nd ed., ch. 3), the
smell the chosen extraction removes: about 60 duplicated lines (two lists of 24 and 16 names, `reorder`, the floor)
in the trust path, drift by construction, its own list and `reorder` tests or a reflection comparison
(AC-3 (c)), and a second static initialiser that the JDK class's sandbox must also isolate. BUG-020's
Suggested Approach advised "before the move, share can only mean copy" because it wanted to keep
`hardenSocket` private and do the fix after CR-009; Record 9 moved the fix before the move. Opening one
static method in a file this story edits anyway costs less than a copy and removes drift for the shared
step: AC-5 then guards the two call sites, which is where a one-factory drift can still happen.

CR-009 consequence (Scope Concern 6): at the move, the pinned factory's two calls must be retargeted to
the moved class. If the vendored class is deleted, compilation forces it. If the move copies the class and
leaves the vendored one, CR-009's zero-references check (Fix Order step 5, `:309`; Fix Checklist, `:333`:
no reference to `com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory` under `app/src`) catches P1's import,
if it is run; and the parity
tests (`createSocket_*_appliesDefaultFactoryProtocolAndCipherHandling`,
`pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider`) compare against the moved
default factory after the test retarget and fail on the first divergent edit. The orchestrator's one-line
CR-009 checklist addition is recommended in section 8.

Trust is untouched: `:77-80` stay byte-identical (fresh `PinnedX509TrustManager` and `SSLContext` per
call; M-E5's anchor). No host-name verification is added; the server name affects only what the server may
choose to present, and a different certificate still fails the pin closed.

### 2.3 Order of the steps on the socket (AC-4 (d))

Both factories: create the socket, then `hardenSocket` (cipher suites, then protocols), then `setSniHost`.
The server-name step reads `getSSLParameters()` (which carries the protocols and suites already set) and
writes them back with the name, so it cannot undo the earlier settings; L12 shows the round trip keeps
both on the JDK provider, N3 probes Conscrypt, and the changed tests assert the floor and the cipher order
on the same socket after the step. The reverse order would let `hardenSocket` run after a name was set,
which the JDK keeps but which is not shown for every provider; the default factory's order is kept.

Server-name step: nothing changes in `setSniHost`, `isIpOrBlankHostname` or `setSniViaSSLParameters`. In
particular `setSniViaSSLParameters` still calls `setSSLParameters` only when a name is set, so U-064's
`setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI` (its "never called" assertion,
the public static and the package-private guard read by reflection) passes unchanged with no retarget.

### 2.4 Exact production code

Apply with the Edit tool, in this order. Every `old_string` is unique in its file. Both files are CRLF in
the working tree (L15); keep the line endings.

**D1** (`DefaultTrustedSocketFactory.java`, insert the allow-list after the blocked-protocol list).
`old_string`:
```java
    protected static final String[] BLACKLISTED_PROTOCOLS = {
            "SSLv3"
    };
```
`new_string`:
```java
    protected static final String[] BLACKLISTED_PROTOCOLS = {
            "SSLv3"
    };

    /**
     * The only protocol versions this factory ever enables, where the platform supports them
     * (BUG-019, BUG-020; product owner decision Record 8 of 2026-11-20: nothing older than
     * TLS 1.2). A name not in this list is never enabled, including a protocol name the factory
     * does not know, so a later TLS version needs an entry here before it is used.
     */
    protected static final String[] ALLOWED_PROTOCOLS = {
            "TLSv1.2", "TLSv1.3"
    };
```

**D2** (static initialiser, first local). `old_string`: `        String[] supportedProtocols = null;`
`new_string`: `        String[] enabledProtocols = null;`

**D3** (static initialiser, the protocol query; the existing comment's facts on API levels are kept
word for word, only a sentence is added). `old_string`:
```java
             * only enabled by default on API levels 20+.
             */
            supportedProtocols = sock.getSupportedProtocols();
```
`new_string`:
```java
             * only enabled by default on API levels 20+. The list is then
             * limited to TLS 1.2 and newer (U-065): see protocolsToEnable.
             */
            enabledProtocols = protocolsToEnable(sock);
```

**D4** (static initialiser, the assignment). `old_string`:
```java
        ENABLED_PROTOCOLS = (supportedProtocols == null) ? null :
                reorder(supportedProtocols, ORDERED_KNOWN_PROTOCOLS, BLACKLISTED_PROTOCOLS);
```
`new_string`: `        ENABLED_PROTOCOLS = enabledProtocols;`

**D5** (insert `protocolsToEnable` after `reorder`). `old_string`:
```java
        return result.toArray(new String[result.size()]);
    }

    private Context context;
```
`new_string`:
```java
        return result.toArray(new String[result.size()]);
    }

    /**
     * The protocols this factory enables on {@code socket}: the protocols the platform supports
     * (not only those it enables by default), with the blocked protocols removed and the known
     * ones first ({@link #reorder}), and then only those in {@link #ALLOWED_PROTOCOLS}, so that
     * nothing older than TLS 1.2 is ever enabled (Record 8). TLS 1.2 and 1.3 are enabled wherever
     * the platform supports them, also where it leaves one off by default. If the platform
     * supports neither, the result is empty: the socket then cannot complete a handshake (or the
     * platform refuses the empty list), so the connection fails instead of falling back to an
     * older version. Every Android version this app supports (API 21 and later) supports TLS 1.2
     * and enables it by default (supported from API 16, enabled by default from API 20, as the
     * comment in the static initialiser and the SSLSocket reference documentation record).
     */
    protected static String[] protocolsToEnable(SSLSocket socket) {
        String[] ordered = reorder(socket.getSupportedProtocols(), ORDERED_KNOWN_PROTOCOLS,
                BLACKLISTED_PROTOCOLS);
        List<String> enabled = new ArrayList<String>();
        for (String protocol : ordered) {
            for (String allowed : ALLOWED_PROTOCOLS) {
                if (allowed.equals(protocol)) {
                    enabled.add(protocol);
                    break;
                }
            }
        }
        return enabled.toArray(new String[enabled.size()]);
    }

    private Context context;
```

**D6** (replace `hardenSocket`). `old_string`:
```java
    private static void hardenSocket(SSLSocket sock) {
        if (ENABLED_CIPHERS != null) {
            sock.setEnabledCipherSuites(ENABLED_CIPHERS);
        }
        if (ENABLED_PROTOCOLS != null) {
            sock.setEnabledProtocols(ENABLED_PROTOCOLS);
        }
    }
```
`new_string`:
```java
    /**
     * Applies this factory's cipher-suite and protocol handling to {@code sock}. Public so that
     * {@code com.zegoggles.smssync.mail.PinnedCertificateSocketFactory} applies exactly the same
     * handling (BUG-020; product owner decision Record 9 of 2026-11-20, "Same as default"). It only
     * narrows and orders what the platform offers on the socket; it touches no trust manager and no
     * host-name decision.
     */
    public static void hardenSocket(SSLSocket sock) {
        hardenSocket(sock, ENABLED_CIPHERS, ENABLED_PROTOCOLS);
    }

    /**
     * Applies the given lists. A null {@code ciphers} leaves the platform's cipher suites. A null
     * {@code protocols} (the lists could not be computed at class initialisation) does not leave
     * the platform's protocols: the list is computed from {@code sock} itself, so the TLS 1.2
     * floor still applies (Record 8).
     */
    protected static void hardenSocket(SSLSocket sock, String[] ciphers, String[] protocols) {
        if (ciphers != null) {
            sock.setEnabledCipherSuites(ciphers);
        }
        sock.setEnabledProtocols(protocols != null ? protocols : protocolsToEnable(sock));
    }
```

No other region of the vendored file changes: the cipher lists, the SSLv3 block, `createSocket` (`:152-180`)
and the server-name methods (`:191-266`) are untouched (AC-9 (d)).

**P1** (`PinnedCertificateSocketFactory.java`, imports). `old_string`:
```java
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
```
`new_string`:
```java
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
```

**P2** (imports). `old_string`:
```java
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
```
`new_string`:
```java
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
```

**P3** (class Javadoc). `old_string`:
```java
 * DES-MODERNIZATION-002 and CNTR-MODERNIZATION-001.
 *
 * <p>Constructor:
```
`new_string`:
```java
 * DES-MODERNIZATION-002 and CNTR-MODERNIZATION-001.
 *
 * <p>Protocols, cipher suites and server name: every socket this factory returns, direct or
 * layered, gets exactly the default factory's handling, by calling the same two steps of
 * {@code DefaultTrustedSocketFactory} in the same order: {@code hardenSocket} (only TLS 1.2 and
 * newer, where the platform supports them; the same cipher-suite order and blocked list), then
 * {@code setSniHost} (the host as server name for a DNS name; none for an IP address or a blank
 * host; none, without failure, for a name the platform rejects). Decided by the product owner on
 * 2026-11-20 (Records 8 and 9; BUG-019, BUG-020). These steps do not touch the trust manager: the
 * pin is the only trust decision, and no host-name verification is added.
 *
 * <p>Constructor:
```

**P4** (`createSocket` body; `:77-80` stay as they are). `old_string`:
```java
        if (socket == null) {
            return socketFactory.createSocket();
        } else {
            return socketFactory.createSocket(socket, host, port, true);
        }
```
`new_string`:
```java
        SSLSocket sslSocket;
        if (socket == null) {
            sslSocket = (SSLSocket) socketFactory.createSocket();
        } else {
            sslSocket = (SSLSocket) socketFactory.createSocket(socket, host, port, true);
        }
        // The default factory's protocol, cipher-suite and server-name handling, in its order
        // (BUG-019, BUG-020; product owner decisions Records 8 and 9 of 2026-11-20). Neither call
        // reads or changes the trust manager installed above.
        DefaultTrustedSocketFactory.hardenSocket(sslSocket);
        DefaultTrustedSocketFactory.setSniHost(socketFactory, sslSocket, host);
        return sslSocket;
```

Constructor and `createSocket` signatures are unchanged (AC-9 (b), IC-2). Callers of the changed
visibility: `DefaultTrustedSocketFactory.createSocket` (`:176`, unchanged) and the pinned factory; nothing
else (L5).

## 3. Test Design

Patterns: characterisation tests changed by declaration (Feathers, *Working Effectively with Legacy
Code*, ch. 13); a Test Stub for `SSLSocket` (Meszaros, *xUnit Test Patterns*, ch. 23) as the seam for the
protocol step (Feathers ch. 4, object seam); a real loopback server as a fixture; Guard Assertions for
preconditions that stop a test from passing on provider defaults (Meszaros ch. 24); mutation testing as the
adequacy check of each test. Smells guarded against: Erratic Test (bounded waits, loopback address object
only, three runs compared), Assertion Roulette (every message names the shape, host, provider or
protocol), Fragile Test (assertions on exception class and protocol names, not provider message text),
Test Code Duplication (accepted in private helpers per class, as U-063 did; one copied cipher-rule helper
so that AC-15 stays byte-identical, stated below).

Names used below. **Old four** = `TLSv1`, `TLSv1.1`, `SSLv3`, `SSLv2Hello`. Changed tests keep their
place in the file. "[R]" marks the refusal tests whose writing depends on probe I2 (rows D3 to D7).

### 3.1 Test impact table (every row of the story's table)

| Class and test (HEAD name) | Effect | New name / reason |
|----------------------------|--------|-------------------|
| Hardening `createSocket_legacyProtocols_remainEnabled_whenProviderSupportsThem` (`:392`) | Changed (BUG-019 AC-2; AC-1 (a)-(c)) | `createSocket_legacyProtocols_notEnabled_althoughProviderEnablesThemByDefault`; old name false. BUG-019 comment kept and reworded (3.3) |
| JDK `createSocket_legacyProtocols_enabledByFactory_whereJdkProviderLeavesThemOff` (`:270`) | Changed (BUG-019 AC-2); A1-P2 kept | `createSocket_legacyProtocols_notEnabledByFactory_whereJdkProviderSupportsThem` |
| Hardening `createSocket_enabledProtocols_areProviderSupportedMinusSslv3_inRecordedOrder` (`:335`) | Changed (BUG-019 AC-4): set is now supported ∩ {1.2, 1.3}; order TLS 1.2 first | `createSocket_enabledProtocols_areProviderSupportedTls12AndNewer_inRecordedOrder` |
| JDK `…_inRecordedOrder_jdkProvider` (`:211`) | Changed (BUG-019 AC-4); A1-P1 kept | `createSocket_enabledProtocols_areProviderSupportedTls12AndNewer_inRecordedOrder_jdkProvider` |
| Hardening `protocolConstants_matchRecordedBaseline` (`:255`) | Unchanged (constants unchanged, 2.1) | — |
| Hardening `reorder_removesEveryBlockedItem` (`:178`) | Unchanged (SSLv3 still blocked) | — |
| Hardening `reorder_ordersKnownItemsFirst_thenUnknownInOriginalOrder` (`:208`) | Unchanged (floor is a separate step) | — |
| Hardening `createSocket_layeredOverPlainSocket_appliesSameHardening` (`:421`) | Unchanged; the absolute layered floor is in the changed legacy test | — |
| Hardening cipher rule, cipher constants, null blocklist (`:270`, `:244`, `:229`) | Unchanged (cipher handling unchanged) | — |
| Hardening and Handshake `checkIsolationGuardA1G2` (`:153-173`; `:366-389`) | Unchanged in code and passing (AC-6 (d)); AC-6 (e) by its second option: the class Javadoc and Implementation Notes record that the `SSLv2Hello` clause can no longer fire and the DHE clause alone carries the guard | 3.3, 3.5 |
| Pinned create-socket `createSocket_implicit_leavesProviderDefaultsUntouched` (`:123`) | Changed (BUG-020 AC-3; AC-2, AC-3 (a), AC-5) | `createSocket_implicit_appliesDefaultFactoryProtocolAndCipherHandling` |
| Pinned create-socket `createSocket_layered_leavesProviderDefaultsUntouched` (`:140`) | Changed (same) | `createSocket_layered_appliesDefaultFactoryProtocolAndCipherHandling` |
| JDK `pinnedFactory_createSocket_implicit_setsNoServerNames_jdkProvider` (`:311`) | Changed (BUG-020 AC-3; AC-4 (a), (d)) | `pinnedFactory_createSocket_implicit_setsDnsHostAsOnlyServerName_jdkProvider` |
| JDK `createSocket_dnsHost_setsServerNameOnRealSocket`, `createSocket_ipv6EightGroupLiteralHost_leavesServerNamesUnset` | Unchanged | — |
| Pinned create-socket `handshake_enrolledCertificateForDifferentHostName_isAccepted`, `handshake_certificateOtherThanEnrolled_isRejected` | Unchanged (BUG-020 AC-6) | — |
| `PinnedCertificateSocketFactoryTest` (8), `DefaultTrustedSocketFactoryHandshakeTest` (8 existing), `DefaultTrustedSocketFactorySniTest` (14, U-064's two included), `PinnedCertificateSocketFactoryExpiryTest` (1) | Unchanged | — |
| `K9MailTransportTest`, `BackupImapStoreTest` (type checks) | Unchanged | — |

New tests (19): Hardening 3, JDK 4, Handshake 6 [4 of them R], pinned create-socket 6 [4 of them R]:

| Class | New test | Criteria |
|-------|----------|----------|
| Hardening | `protocolsToEnable_givenSupportedList_keepsOnlySupportedTls12AndNewer` | AC-1 (d); `ALLOWED_PROTOCOLS` snapshot; Scope Concerns 3, 5, 7 |
| Hardening | `hardenSocket_withNullLists_stillAppliesTheProtocolFloorFromTheSocket` | AC-1 (e) |
| Hardening | `pinnedFactory_createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst` | AC-3 (b) |
| JDK | `pinnedFactory_createSocket_enablesExactlyTls12AndTls13_bothShapes_jdkProvider` | AC-2 (a), (c) |
| JDK | `pinnedFactory_createSocket_ipLiteralAndBlankHosts_setNoServerName_jdkProvider` | AC-4 S2, S3, S4, (b), (c) |
| JDK | `pinnedFactory_createSocket_nameRejectedBySniHostName_doesNotThrowAndSetsNoServerName_jdkProvider` | AC-4 S5 (the call-through test) |
| JDK | `pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider` | AC-5, AC-3 (a) (JDK), S6 |
| Handshake | `implicitTls_serverLimitedToTls12_handshakeCompletesAtTls12`, `startTls_serverLimitedToTls12_handshakeCompletesAtTls12` | AC-8 (a) |
| Handshake | `implicitTls_serverLimitedToTls11_handshakeRefused`, `startTls_serverLimitedToTls11_handshakeRefused`, `implicitTls_serverLimitedToTls10_handshakeRefused`, `startTls_serverLimitedToTls10_handshakeRefused` [R] | AC-8 (b) |
| Pinned create-socket | `handshake_implicitTls_serverLimitedToTls12_completesAtTls12`, `handshake_startTls_serverLimitedToTls12_completesAtTls12` | AC-8 (a) |
| Pinned create-socket | `handshake_implicitTls_serverLimitedToTls11_isRefused`, `handshake_startTls_serverLimitedToTls11_isRefused`, `handshake_implicitTls_serverLimitedToTls10_isRefused`, `handshake_startTls_serverLimitedToTls10_isRefused` [R] | AC-8 (b) |

Count (stated once): the count at the start of this story is 713; with all 19 written it is **732**. For
each version (TLS 1.1, TLS 1.0) that probe I2 rules out under rows D4 or D6-b (decision rows, not the production edit D6), 4 refusal tests are not written
and the end count is 4 lower (728 or 724). No test is removed.

Per-class totals after the change (used by every run below): Hardening 12, JDK 9, Handshake 14 (10 or 12 if
reduced), SNI 14, pinned create-socket 10 (6 or 8 if reduced), Expiry 1, `PinnedCertificateSocketFactoryTest`
8; the seven classes together 68 (60 or 64 if reduced).

### 3.2 Shared test conventions

- Text rules: the string `@Test` only as an annotation; annotations in short imported form; no `*/`
  inside a comment; no `localhost`, `127.0.0.1`, `getByName`, `Thread.sleep`; every wait bounded by
  `LoopbackTlsTestServer.TIMEOUT_MILLIS`.
- Every class keeps its own private helpers (no new helper class; `TlsTestFixtures.java` is not edited).
- Diff discipline: no hunk may touch a line range of an unchanged test method. Allowed HEAD ranges for
  hunks are listed per file; I3 step 8 checks them.

### 3.3 `DefaultTrustedSocketFactoryHardeningTest.java` (LF)

Allowed HEAD ranges for hunks: imports `:3-26`; class Javadoc `:28-91`; after `:147` (constants);
`:330-414` (the two changed tests and the BUG-019 comment); after `:448` (insertion); after `:517`
(helpers, before `closeQuietly`). The guard `:149-173`, the unchanged methods `:175-328` and `:416-448`, and
`:518-535` must show no hunk.

**H-imp** (imports). Replace `import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;` with:
```java
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.zegoggles.smssync.mail.PinnedCertificateSocketFactory;
```
and replace `import org.junit.runner.RunWith;` with:
```java
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
```
and replace `import static org.junit.Assert.fail;` with:
```java
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
```

**H-doc** (class Javadoc, `:28-91`, replaced whole; comment-only):
```java
/**
 * Characterisation tests for the cipher and protocol hardening of {@link DefaultTrustedSocketFactory}
 * (story U-063, Group D, acceptance criteria AC-11 to AC-17; protocol tests changed and tests added by
 * story U-065), run under Robolectric's default TLS provider. At the environment probe (I0b) that
 * provider was Conscrypt.
 *
 * <p><b>What the code does, and what these tests pin down.</b> The factory computes two static lists
 * once per class loader: the provider's default enabled cipher suites with 16 blocked names removed
 * and 24 known names placed first in a fixed order, and the provider's <em>supported</em> protocols
 * with SSLv3 removed, TLS 1.2, 1.1, 1.0 placed first in that order, and then only TLS 1.2 and TLS 1.3
 * kept. The last step is the protocol floor of U-065 (BUG-019; product owner decision Record 8 of
 * 2026-11-20: no protocol older than TLS 1.2, for either factory). Every socket it creates gets both
 * lists applied; if the lists could not be computed at class initialisation, the floor is computed
 * from the socket itself. The tests check the pure reordering function, the four constant lists and
 * the allowed-protocol list against literal copies kept here (snapshot tests, so that any edit to a
 * list is a conscious, reviewed change), the protocol step fed given supported lists through a
 * stand-in socket, the lists on a real implicit-TLS socket, and the same lists on a socket layered
 * over a plain connection. Since U-065 the pinned-certificate factory's cipher suites are checked here
 * against the same recorded rule (BUG-020; Record 9, "Same as default"). Expectations come from the
 * live provider at test time plus those literal baselines; no list of provider suites is hard-coded.
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>The lists here are those of a desktop test provider, not of a device. What a device's
 *       platform enables or reorders is not shown.
 *   <li>"Enabled" is not "negotiable": nothing here connects to a server, so nothing shows which
 *       suite or protocol a handshake would settle on. No handshake is run in this class; the
 *       handshake classes show TLS 1.2 negotiated and older versions refused.
 *   <li>The array of enabled protocols is not a preference order (SC-5). Since U-065 it holds TLS 1.2
 *       and then TLS 1.3 where both are supported; the TLS stack does not read the array as a
 *       preference list, and this is not claimed to be a defect. The order is recorded as it is.
 *   <li>The provider fixes some suites in front of whatever the caller sets (under Conscrypt, the
 *       three TLS 1.3 suites). The order checks of AC-15 therefore run on the list with those
 *       provider-fixed suites removed (plan deviation 7), within bounds that are asserted so that the
 *       checks cannot pass on an empty or tiny remainder.
 *   <li>The class initialiser's failure branch (provider query failed, both static lists null) cannot
 *       be made to run: the initialiser runs once per class loader and the lists are final. The code
 *       of that branch is called directly with null lists (U-065 AC-1 (e)).
 *   <li>Under this provider the supported protocols equal the default enabled ones, so whether the
 *       protocol list is built from the supported or the enabled list is shown only by the stand-in
 *       socket test (U-065 AC-1 (d)).
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063 and U-065 mutation logs).
 * <ul>
 *   <li>AC-11: a name removed from the blocked-cipher list (M-D1), the blocked-protocol list
 *       emptied (M-D2), or the removal loop in {@code reorder} deleted (M-D3).
 *   <li>AC-12: two entries of a known list swapped (M-D4, M-D5), or the unknown items put first or
 *       dropped (M-D6a, M-D6b).
 *   <li>AC-13: the null-blocklist guard removed (M-D7), or empty input mishandled (M-D15).
 *   <li>AC-14, ciphers: any entry of the known or blocked cipher list changed (M-D1, M-D4, M-D14).
 *       AC-14, protocols: any entry of the known or blocked protocol list changed (M-D2, M-D5, M-D12).
 *   <li>AC-15: the hardening call removed from {@code createSocket} (M-D8), the cipher part of
 *       {@code hardenSocket} removed (M-D9), or the unknown items dropped (M-D6b).
 *   <li>AC-16, both tests as changed by U-065: the TLS 1.2 floor removed (M-P1/M-P8), or the old
 *       protocol list given back to the default factory alone (M-P6 (ii)).
 *   <li>AC-17: hardening applied only when no plain socket is passed in (M-D10).
 *   <li>U-065 stand-in socket test: the floor removed (M-P1/M-P8); the list built from the enabled
 *       instead of the supported protocols (M-D11r).
 *   <li>U-065 null-lists test: the floor not applied when the lists are null (M-F1).
 *   <li>U-065 pinned cipher test: the pinned factory's cipher handling removed (M-P4), or one entry of
 *       its order swapped (M-P6 (i)).
 * </ul>
 * The U-063 mutations ran against the code before U-065 and are not repeated.
 *
 * <p><b>Isolation guard A1-G2.</b> The before-method fails, naming A1-G2, if the factory's static
 * cipher list was computed under the JDK provider (it would contain DHE suites, which this class's
 * provider never produces). That would mean the JDK-provider class's Robolectric sandbox was reused
 * here. The guard also has an SSLv2Hello clause on the protocol list; since U-065 the factory never
 * enables SSLv2Hello under either provider, so that clause can no longer fire and the DHE clause alone
 * carries the guard (U-065 AC-6 (e); the guard code is left unchanged).
 *
 * <p><b>After the CR-009 move:</b> a missing field, method or class means this test's seam needs
 * retargeting to the moved class; a failed behavioural assertion is a regression, and the test is
 * never edited to pass.
 */
```

**H-const** (after `private static final String[] BLOCKED_PROTOCOLS_BASELINE = {"SSLv3"};`), add:
```java
    /** The allowed protocols of the factory, in order (snapshot; U-065, Record 8). */
    private static final String[] ALLOWED_PROTOCOLS_BASELINE = {"TLSv1.2", "TLSv1.3"};

    /** Protocol versions older than TLS 1.2 that must never be enabled (Record 8). */
    private static final String[] OLD_PROTOCOLS = {"TLSv1", "TLSv1.1", "SSLv3", "SSLv2Hello"};
```

**Guard A1-G2 (AC-6 (e)): no code edit (iteration 2, plan-review O-5).** `checkIsolationGuardA1G2`
(`:149-173`) stays byte-identical, so AC-6 (d)'s "the guards of the other two classes pass unchanged" holds
literally. AC-6 (e) is met by its second option, recorded in the class Javadoc above and in the
Implementation Notes: after U-065 the factory never enables `SSLv2Hello` under either provider, so the
`SSLv2Hello` clause can no longer fire; the DHE clause beside it still fires when the lists were computed
under the JDK provider (its default suites contain `TLS_DHE_` suites, L12, and `reorder` keeps them), as the
JDK class's own A1-G1 relies on, so the guard still stops a run that reuses a sandbox. The method's own
Javadoc ("SSLv2Hello and DHE suites appear only under the JDK provider") stays: it remains true of the
providers, and the class Javadoc gives the new reading.

**H1** (replace `:330-374`, Javadoc and method of the supported-minus-SSLv3 test):
```java
    /**
     * AC-16 (first test), as changed by U-065 (AC-1 (b), BUG-019 AC-4): SSLv3 is not enabled; the
     * enabled protocols are exactly the provider's supported protocols that are TLS 1.2 or newer;
     * TLS 1.2 comes before every other enabled protocol (SC-5: an order of the array, not a
     * preference). Implicit shape; the layered shape is in the second test.
     */
    @Test
    public void createSocket_enabledProtocols_areProviderSupportedTls12AndNewer_inRecordedOrder()
            throws Exception {
        SSLSocket plain = defaultSocket();
        Set<String> supported;
        try {
            supported = new HashSet<>(Arrays.asList(plain.getSupportedProtocols()));
        } finally {
            closeQuietly(plain);
        }
        assertWithMessage("precondition: the provider supports TLS 1.2; supported=" + supported)
                .that(supported).contains("TLSv1.2");
        SSLSocket socket = hardenedSocket();
        List<String> enabled;
        try {
            enabled = Arrays.asList(socket.getEnabledProtocols());
        } finally {
            closeQuietly(socket);
        }

        assertThat(enabled).doesNotContain("SSLv3");
        Set<String> expected = new HashSet<>(supported);
        expected.retainAll(Arrays.asList(ALLOWED_PROTOCOLS_BASELINE));
        assertWithMessage("enabled protocols must be the supported protocols that are TLS 1.2 or newer;"
                + " supported=" + supported).that(new HashSet<>(enabled)).isEqualTo(expected);
        assertThat(enabled).containsNoDuplicates();
        assertWithMessage("TLSv1.2 must come first in " + enabled).that(enabled.get(0)).isEqualTo("TLSv1.2");
    }
```

**H2** (replace `:376-414`, the BUG-019 comment, Javadoc and method; AC-6 (b) rewording):
```java
    // DP-2 (decision point: TLS 1.0 and 1.1). Bug id: BUG-019, decided by the product owner on
    // 2026-11-20 (Record 8: no protocol older than TLS 1.2 is enabled, by either factory). Since
    // U-065 this test asserts the decided behaviour; until then it recorded that the two stayed enabled.
    /**
     * AC-16 (second test), as changed by U-065 (AC-1 (a), (b), (c); BUG-019 AC-1, AC-2): none of
     * TLSv1, TLSv1.1, SSLv3 and SSLv2Hello is enabled on the factory's socket, direct or layered, and
     * TLS 1.2 and 1.3 are each enabled if and only if the provider supports them. RFC 8996 deprecates
     * TLS 1.0 and 1.1.
     *
     * <p>Precondition (AC-1 (c)): this provider (Conscrypt) enables TLS 1.0 and 1.1 on its own default
     * socket, so their absence on the factory's sockets can only be the factory's doing. "Enabled" is
     * not "negotiable"; the handshake class shows what is negotiated.
     */
    @Test
    public void createSocket_legacyProtocols_notEnabled_althoughProviderEnablesThemByDefault() throws Exception {
        SSLSocket plain = defaultSocket();
        List<String> supported;
        List<String> providerEnabled;
        try {
            supported = Arrays.asList(plain.getSupportedProtocols());
            providerEnabled = Arrays.asList(plain.getEnabledProtocols());
        } finally {
            closeQuietly(plain);
        }
        for (String name : Arrays.asList("TLSv1", "TLSv1.1")) {
            assertWithMessage("precondition AC-1 (c): the provider's default socket must enable " + name
                    + ", so that its absence is the factory's doing; default enabled=" + providerEnabled)
                    .that(providerEnabled).contains(name);
        }

        SSLSocket direct = hardenedSocket();
        try {
            assertProtocolFloor("default factory, direct socket", supported,
                    Arrays.asList(direct.getEnabledProtocols()));
        } finally {
            closeQuietly(direct);
        }
        try (Layered layered = new Layered(
                new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()), HOST)) {
            assertProtocolFloor("default factory, layered socket", supported,
                    Arrays.asList(layered.socket.getEnabledProtocols()));
        }
    }
```

**H3, H4, H5** (insert after the closing brace of `createSocket_layeredOverPlainSocket_appliesSameHardening`,
`:448`, before the Helpers banner):
```java
    /**
     * U-065 AC-1 (d): the protocol step, fed a given supported list through a stand-in socket, keeps
     * exactly the supported protocols that are TLS 1.2 or newer, whatever the stand-in enables by
     * default. Case (iii) is a provider that supports TLS 1.2 but leaves it off by default: TLS 1.2 is
     * still enabled ("stay enabled where the platform supports them", Record 8, read literally). Case
     * (iv) shows that an unknown name is never enabled and that a platform with nothing at or above
     * TLS 1.2 gets an empty list (story Scope Concern 7). The allowed list is checked against its
     * literal copy. Mutations: the floor removed (M-P1/M-P8); the list built from the enabled instead
     * of the supported protocols (M-D11r, which only case (iii) can tell).
     */
    @Test
    public void protocolsToEnable_givenSupportedList_keepsOnlySupportedTls12AndNewer() throws Exception {
        assertThat(Arrays.asList(constant("ALLOWED_PROTOCOLS")))
                .containsExactlyElementsIn(ALLOWED_PROTOCOLS_BASELINE).inOrder();
        String[] everything = {"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};

        assertWithMessage("(i) every old name supported and enabled")
                .that(Arrays.asList(protocolsToEnable(stubSocket(everything, everything))))
                .containsExactly("TLSv1.2", "TLSv1.3").inOrder();
        assertWithMessage("(ii) only TLS 1.2 supported")
                .that(Arrays.asList(protocolsToEnable(
                        stubSocket(new String[] {"TLSv1.2"}, new String[] {"TLSv1.2"}))))
                .containsExactly("TLSv1.2");
        assertWithMessage("(iii) TLS 1.2 supported but off by default")
                .that(Arrays.asList(protocolsToEnable(stubSocket(
                        new String[] {"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"},
                        new String[] {"TLSv1.3"}))))
                .containsExactly("TLSv1.2", "TLSv1.3").inOrder();
        assertWithMessage("(iv) nothing at or above TLS 1.2, and an unknown name")
                .that(Arrays.asList(protocolsToEnable(stubSocket(
                        new String[] {"SSLv3", "TLSv1", "TLSv1.1", "U065_UNKNOWN_PROTOCOL"},
                        new String[] {"TLSv1", "TLSv1.1", "U065_UNKNOWN_PROTOCOL"}))))
                .isEmpty();
    }

    /**
     * U-065 AC-1 (e): the branch taken when the provider query failed at class initialisation (both
     * static lists null). The class initialiser cannot be made to fail here: it runs once per class
     * loader and the lists are final. So the code of that branch is called directly, with both lists
     * null, on a stand-in socket that supports and enables every old version: the protocol floor is
     * still applied, computed from the socket itself, and the cipher suites are left as they are.
     * Mutation: the fallback removed (M-F1, the behaviour before U-065: nothing applied).
     */
    @Test
    public void hardenSocket_withNullLists_stillAppliesTheProtocolFloorFromTheSocket() throws Exception {
        String[] everything = {"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"};
        SSLSocket socket = stubSocket(everything, everything);

        hardenSocket(socket, null, null);

        ArgumentCaptor<String[]> protocols = ArgumentCaptor.forClass(String[].class);
        verify(socket).setEnabledProtocols(protocols.capture());
        assertThat(Arrays.asList(protocols.getValue())).containsExactly("TLSv1.2", "TLSv1.3").inOrder();
        verify(socket, never()).setEnabledCipherSuites(any());
    }

    /**
     * U-065 AC-3 (b) (BUG-020, Record 9): the pinned-certificate factory's direct socket passes the
     * same three checks as AC-15 above, against the literal baselines, so that the two factories cannot
     * be wrong the same way. Mutations: the pinned cipher handling removed (M-P4); one entry of the
     * cipher order swapped in the pinned factory only (M-P6 (i)).
     */
    @Test
    public void pinnedFactory_createSocket_enabledCipherSuites_areProviderDefaultsMinusBlockedWithKnownFirst()
            throws Exception {
        List<String> defaults = enabledCiphersOf(defaultSocket());
        List<String> pinned = enabledCiphersOf(pinnedSocket());
        assertCipherRule("pinned direct socket", defaults, pinned);
    }
```

**H-help** (insert after the closing brace of `providerFixedSuites()` at `:517`, before `closeQuietly`):
```java
    /** Calls the protected static protocolsToEnable of the factory by reflection (U-065). */
    private static String[] protocolsToEnable(SSLSocket socket) throws Exception {
        Method method = DefaultTrustedSocketFactory.class.getDeclaredMethod("protocolsToEnable",
                SSLSocket.class);
        method.setAccessible(true);
        return (String[]) method.invoke(null, socket);
    }

    /** Calls the protected static three-argument hardenSocket of the factory by reflection (U-065). */
    private static void hardenSocket(SSLSocket socket, String[] ciphers, String[] protocols)
            throws Exception {
        Method method = DefaultTrustedSocketFactory.class.getDeclaredMethod("hardenSocket",
                SSLSocket.class, String[].class, String[].class);
        method.setAccessible(true);
        method.invoke(null, socket, ciphers, protocols);
    }

    /** A stand-in socket that reports the given supported and default-enabled protocols. */
    private static SSLSocket stubSocket(String[] supported, String[] enabled) {
        SSLSocket socket = mock(SSLSocket.class);
        when(socket.getSupportedProtocols()).thenReturn(supported.clone());
        when(socket.getEnabledProtocols()).thenReturn(enabled.clone());
        return socket;
    }

    /** The pinned-certificate factory's implicit-TLS socket, unconnected (the other-host leaf enrolled). */
    private static SSLSocket pinnedSocket() throws Exception {
        return (SSLSocket) new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(), HOST,
                TlsTestFixtures.certificate(TlsTestFixtures.OTHER_HOST_CERT))
                .createSocket(null, HOST, 993, null);
    }

    /** AC-1 (a) and (b) for one socket: none of the old four; TLS 1.2 and 1.3 each enabled iff supported. */
    private static void assertProtocolFloor(String label, List<String> supported, List<String> enabled) {
        for (String old : OLD_PROTOCOLS) {
            assertWithMessage(label + ": " + old + " must not be enabled; enabled=" + enabled)
                    .that(enabled).doesNotContain(old);
        }
        for (String modern : ALLOWED_PROTOCOLS_BASELINE) {
            assertWithMessage(label + ": " + modern + " enabled must equal " + modern + " supported;"
                    + " supported=" + supported + ", enabled=" + enabled)
                    .that(enabled.contains(modern)).isEqualTo(supported.contains(modern));
        }
        Set<String> expected = new HashSet<>(supported);
        expected.retainAll(Arrays.asList(ALLOWED_PROTOCOLS_BASELINE));
        assertWithMessage(label + ": the enabled set must be the supported protocols that are TLS 1.2 or"
                + " newer").that(new HashSet<>(enabled)).isEqualTo(expected);
    }

    /**
     * The three checks of AC-15 for a hardened cipher list against the provider defaults, with the
     * same bounds on the provider-fixed suites F. A copy of AC-15's body, kept separate so that AC-15
     * stays as U-063 wrote it (U-065 AC-3 (b)).
     */
    private static void assertCipherRule(String label, List<String> defaults, List<String> hardened)
            throws Exception {
        Set<String> expectedSet = new HashSet<>(defaults);
        expectedSet.removeAll(Arrays.asList(BLOCKED_CIPHERS_BASELINE));
        assertWithMessage(label + ": (i) the set must equal the provider defaults minus the blocked names")
                .that(new HashSet<>(hardened)).isEqualTo(expectedSet);

        List<String> fixed = providerFixedSuites();
        for (String name : fixed) {
            assertWithMessage(label + ": D6'': provider-fixed suite " + name + " is a baseline known or"
                    + " blocked suite; F=" + fixed)
                    .that(Arrays.asList(KNOWN_CIPHERS_BASELINE).contains(name)
                            || Arrays.asList(BLOCKED_CIPHERS_BASELINE).contains(name)).isFalse();
            assertWithMessage(label + ": D6'': provider-fixed suite " + name + " contains _WITH_; F="
                    + fixed).that(name.contains("_WITH_")).isFalse();
        }
        List<String> remainder = new ArrayList<>(hardened);
        remainder.removeAll(fixed);
        List<String> knownInRemainder = new ArrayList<>();
        for (String name : KNOWN_CIPHERS_BASELINE) {
            if (remainder.contains(name)) {
                knownInRemainder.add(name);
            }
        }
        assertWithMessage(label + ": D6'': fewer than 2 known ciphers remain; remainder=" + remainder)
                .that(knownInRemainder.size()).isAtLeast(2);
        assertWithMessage(label + ": D6'': no unknown suite remains; remainder=" + remainder)
                .that(remainder.size() - knownInRemainder.size()).isAtLeast(1);

        List<String> actualKnownOrder = new ArrayList<>();
        for (String name : remainder) {
            if (Arrays.asList(KNOWN_CIPHERS_BASELINE).contains(name)) {
                actualKnownOrder.add(name);
            }
        }
        assertWithMessage(label + ": (ii) the known suites must appear in the literal order")
                .that(actualKnownOrder).containsExactlyElementsIn(knownInRemainder).inOrder();

        int lastKnown = -1;
        int firstUnknown = Integer.MAX_VALUE;
        for (int i = 0; i < remainder.size(); i++) {
            if (Arrays.asList(KNOWN_CIPHERS_BASELINE).contains(remainder.get(i))) {
                lastKnown = i;
            } else if (firstUnknown == Integer.MAX_VALUE) {
                firstUnknown = i;
            }
        }
        assertWithMessage(label + ": (iii) a known cipher follows an unknown one in " + remainder)
                .that(lastKnown).isLessThan(firstUnknown);
    }

    /** A socket a factory layered over a fresh plain loopback connection, with the plain sockets it needs. */
    private static final class Layered implements java.io.Closeable {
        private final ServerSocket listener;
        private final Socket client;
        private final Socket accepted;
        private final SSLSocket socket;

        Layered(TrustedSocketFactory factory, String host) throws Exception {
            ServerSocket l = LoopbackTlsTestServer.bindPlainLoopback();
            Socket c = null;
            Socket a = null;
            SSLSocket s;
            try {
                c = LoopbackTlsTestServer.connectPlain(l.getLocalPort());
                a = l.accept();
                s = (SSLSocket) factory.createSocket(c, host, l.getLocalPort(), null);
            } catch (Exception | Error e) {
                closeQuietly(c);
                closeQuietly(a);
                closeQuietly(l);
                throw e;
            }
            listener = l;
            client = c;
            accepted = a;
            socket = s;
        }

        @Override
        public void close() {
            closeQuietly(socket);
            closeQuietly(client);
            closeQuietly(accepted);
            closeQuietly(listener);
        }
    }
```

### 3.4 `DefaultTrustedSocketFactoryJdkProviderTest.java` (LF)

Allowed HEAD ranges for hunks: imports `:3-35`; class Javadoc `:37-105`; after `:113` (constants);
`:205-324` (the three changed tests); after `:324` (insertion); after `:348` (helpers inserted after
`hardenedSocket`). Unchanged: guards `:115-166`, `:168-203`, `:326-348` existing helpers, `:350-365`.

**J-imp**: replace `import com.zegoggles.smssync.mail.PinnedCertificateSocketFactory;` with
```java
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.zegoggles.smssync.mail.PinnedCertificateSocketFactory;
```
and replace `import java.lang.reflect.Field;` with
```java
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
```

**J-doc** (class Javadoc `:37-105`, replaced whole; comment-only). Paragraphs "Why this class exists"
(`:45-53`), "The looper-mode annotation" (`:55-61`), "Guard A1-G3" (`:98-100`) and "After the CR-009 move"
(`:102-104`) are copied unchanged; the rest becomes:
```java
/**
 * Characterisation tests for {@link DefaultTrustedSocketFactory} that run on the JDK's own TLS
 * provider (SunJSSE) instead of the Conscrypt provider that Robolectric installs by default
 * (story U-063, acceptance criteria AC-3, AC-4 and the supported-protocols clause of AC-16; changed and
 * extended by story U-065). It also holds the pinned factory's checks that need this provider: under
 * Conscrypt the server-name observable reads nothing whatever is set, so mutation M-E2 was not detected
 * there (product owner decision of 2026-11-20), and only this provider supports old protocols while
 * leaving them off by default.
 *
 * <p><b>Why this class exists.</b> [copy :45-53 unchanged]
 *
 * <p><b>The looper-mode annotation.</b> [copy :55-61 unchanged]
 *
 * <p><b>What these tests prove.</b> With the JDK provider: the default factory's socket for a DNS host
 * carries exactly that host as its one server name; the socket for an eight-group IPv6 literal carries
 * none and creating it does not throw (RFC 6066 section 3: the server name list holds DNS names, never IP
 * literals). Since U-065 (BUG-019; product owner decision Record 8 of 2026-11-20) the default factory
 * enables exactly the provider's supported protocols that are TLS 1.2 or newer, TLS 1.2 first, and none of
 * TLS 1.0, TLS 1.1, SSLv3 and SSLv2Hello on the direct or the layered socket, although this provider
 * supports all four (until U-065 it enabled TLS 1.0, 1.1 and SSLv2Hello here; RFC 8996 deprecates TLS 1.0
 * and 1.1). For the pinned-certificate factory (BUG-020; Record 9, "Same as default"): its sockets enable
 * exactly TLS 1.2 and 1.3 on both shapes, so it gains no protocol this provider leaves off; its direct
 * socket for a DNS host carries that host as its only server name, with the floor and the default
 * factory's cipher order still in place after that step; for an IPv4-form host, a host with a colon, a
 * null, empty or blank host, and a name SNIHostName rejects it sets no server name and does not throw
 * (statements S2 to S5 of CR-013); and its sockets match the default factory's in protocols, cipher suites
 * and server names (U-065 AC-5).
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>This provider is neither the app's runtime provider nor a device's. Nothing here shows what
 *       a device's TLS stack sends or enables.
 *   <li>"Enabled" is not "negotiable". The JDK security property for disabled algorithms lists
 *       TLS 1.0 and 1.1 and refuses them in a handshake whatever the client enables, so the tests
 *       assert only what the factory enables; the handshake classes show what is negotiated.
 *   <li>No handshake is run in this class: every socket here is unconnected, or layered over a plain
 *       connection that never carries TLS. Rejection of certificates, host names and resumed sessions
 *       is the business of the handshake classes.
 *   <li>On the layered shape this provider sets the host as server name by itself (U-065 probe), so
 *       the factories' server-name step cannot be observed there; for that shape only the absence of an
 *       exception is asserted, and the statement rests on reading the code (CR-013 Scope Concern D).
 *   <li>Whether the protocol list is built from the supported or the enabled protocols is no longer
 *       visible here (both give TLS 1.2 and 1.3 on this provider); the stand-in socket test of the
 *       Hardening class covers it.
 *   <li>The wire-level contents of a client hello are not examined.
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063 and U-065 mutation logs).
 * <ul>
 *   <li>AC-3: the guard and the safety net in {@code setSniViaSSLParameters} both removed (M-A1,
 *       the exception escapes {@code createSocket}); {@code setSniHost} reporting a name for an IP
 *       literal (M-A3).
 *   <li>AC-4: {@code setSniHost} made a no-op or deleted from {@code createSocket} (M-A4, and U-065's
 *       M-P6 (iii)); the host replaced by another name (M-A3).
 *   <li>AC-16, both tests as changed by U-065: the TLS 1.2 floor removed (M-P1/M-P8), or the old list
 *       given back to the default factory alone (M-P6 (ii)).
 *   <li>AC-19 as changed by U-065 (pinned, DNS host): the pinned server-name step removed (M-P5); the
 *       pinned cipher handling removed (M-P4) or one of its entries swapped (M-P6 (i)); the pinned list
 *       widened (M-P3); the floor removed (M-P1/M-P8).
 *   <li>U-065 pinned exact-protocol test: M-P3, M-P1/M-P8. It compares sets, so it does not detect a
 *       layered socket left untouched (M-P7): this provider already enables exactly TLS 1.3 and 1.2 there;
 *       the parity test detects M-P7 by order.
 *   <li>U-065 pinned rejected-name test: the catch in {@code setSniViaSSLParameters} removed (M-S5).
 *   <li>U-065 parity test: each of M-P2 to M-P7 and M-S5, in particular the three one-factory drifts
 *       M-P6 (i) to (iii).
 * </ul>
 *
 * <p><b>Guard A1-G3 is a permanent tripwire.</b> [copy :98-100 unchanged]
 *
 * <p><b>After the CR-009 move:</b> [copy :102-104 unchanged]
 */
```
(The bracketed lines are instructions: paste the original lines there, without the brackets.)

**J-const** (after `private static final int CIPHER_ENTRIES_IN_MESSAGES = 5;`):
```java
    /** The allowed protocols of the factory (U-065, Record 8). */
    private static final String[] ALLOWED_PROTOCOLS_BASELINE = {"TLSv1.2", "TLSv1.3"};

    /** Protocol versions older than TLS 1.2 that must never be enabled (Record 8). */
    private static final String[] OLD_PROTOCOLS = {"TLSv1", "TLSv1.1", "SSLv3", "SSLv2Hello"};
```

**J1** (replace `:205-260`):
```java
    /**
     * AC-16 (supported-protocols clause), as changed by U-065 (AC-1 (b), BUG-019 AC-4): the factory
     * enables exactly the provider's supported protocols that are TLS 1.2 or newer, TLS 1.2 first, and
     * not SSLv3. Precondition A1-P1: on this provider the supported set differs from the default enabled
     * set. Since U-065 both sets give the same list here, so this test no longer tells them apart; the
     * stand-in socket test of the Hardening class does (AC-1 (d)).
     */
    @Test
    public void createSocket_enabledProtocols_areProviderSupportedTls12AndNewer_inRecordedOrder_jdkProvider()
            throws Exception {
        SSLSocket plain = defaultSocket();
        Set<String> supported;
        Set<String> defaultEnabled;
        try {
            supported = new HashSet<>(Arrays.asList(plain.getSupportedProtocols()));
            defaultEnabled = new HashSet<>(Arrays.asList(plain.getEnabledProtocols()));
        } finally {
            closeQuietly(plain);
        }
        assertWithMessage("A1-P1: the provider's supported protocols equal its default enabled "
                + "protocols, so this class cannot tell supported from enabled; supported="
                + supported + ", default enabled=" + defaultEnabled)
                .that(supported.equals(defaultEnabled)).isFalse();
        assertWithMessage("precondition: the provider supports TLS 1.2; supported=" + supported)
                .that(supported).contains("TLSv1.2");

        SSLSocket socket = hardenedSocket(DNS_HOST);
        List<String> enabled;
        try {
            enabled = Arrays.asList(socket.getEnabledProtocols());
        } finally {
            closeQuietly(socket);
        }

        assertThat(enabled).doesNotContain("SSLv3");
        Set<String> expected = new HashSet<>(supported);
        expected.retainAll(Arrays.asList(ALLOWED_PROTOCOLS_BASELINE));
        assertWithMessage("enabled protocols must be the supported protocols that are TLS 1.2 or newer")
                .that(new HashSet<>(enabled)).isEqualTo(expected);
        assertThat(enabled).containsNoDuplicates();
        assertWithMessage("TLSv1.2 must come first in " + enabled).that(enabled.get(0)).isEqualTo("TLSv1.2");
    }
```

**J2** (replace `:262-298`):
```java
    /**
     * AC-16 (second test, JDK form; decision point DP-2), as changed by U-065 (AC-1 (a), (b), (c);
     * BUG-019 AC-1, AC-2): the factory enables none of TLSv1, TLSv1.1, SSLv3 and SSLv2Hello, on the
     * direct and on the layered socket, although this provider supports all four; TLS 1.2 and 1.3 are
     * enabled if and only if supported. Precondition A1-P2: TLSv1 and TLSv1.1 are supported and not
     * enabled by default, so the test is made against a provider that supports them. Until U-065 the
     * factory enabled TLS 1.0, 1.1 and SSLv2Hello here (BUG-019); decided by the product owner on
     * 2026-11-20 (Record 8).
     */
    @Test
    public void createSocket_legacyProtocols_notEnabledByFactory_whereJdkProviderSupportsThem()
            throws Exception {
        List<String> legacy = Arrays.asList("TLSv1", "TLSv1.1");
        SSLSocket plain = defaultSocket();
        List<String> supported;
        try {
            supported = Arrays.asList(plain.getSupportedProtocols());
            List<String> defaultEnabled = Arrays.asList(plain.getEnabledProtocols());
            for (String name : legacy) {
                assertWithMessage("A1-P2: " + name + " must be supported and not enabled by default "
                        + "on this provider; supported=" + supported + ", default enabled="
                        + defaultEnabled)
                        .that(supported.contains(name) && !defaultEnabled.contains(name)).isTrue();
            }
        } finally {
            closeQuietly(plain);
        }

        SSLSocket socket = hardenedSocket(DNS_HOST);
        try {
            assertProtocolFloor("default factory, direct socket", supported,
                    Arrays.asList(socket.getEnabledProtocols()));
        } finally {
            closeQuietly(socket);
        }
        try (Layered layered = new Layered(
                new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()), DNS_HOST)) {
            assertProtocolFloor("default factory, layered socket", supported,
                    Arrays.asList(layered.socket.getEnabledProtocols()));
        }
    }
```

**J3** (replace `:300-324`):
```java
    /**
     * AC-19 as changed by U-065 (AC-4 (a), (d); BUG-020 AC-2, AC-3; CR-013 S1 and S6): the pinned
     * factory's direct socket for a DNS host carries exactly one server name, an SNIHostName equal to
     * the host, and on the same socket, after that step, exactly TLS 1.2 and 1.3 and the default
     * factory's cipher suites in the default factory's order. Precondition: an unconnected socket of this
     * provider carries no server name of its own, so the name can only be the factory's (U-063 mutation
     * M-E2-jdk showed that this observable reports what the factory sets). Control: the default factory's
     * createSocket_dnsHost_setsServerNameOnRealSocket. The layered shape is not asserted for the name:
     * on this provider a layered socket already carries the host before any factory step (U-065 probe).
     */
    @Test
    public void pinnedFactory_createSocket_implicit_setsDnsHostAsOnlyServerName_jdkProvider() throws Exception {
        SSLSocket plain = defaultSocket();
        try {
            assertWithMessage("precondition: an unconnected default socket carries no server name")
                    .that(serverNames(plain)).isEmpty();
        } finally {
            closeQuietly(plain);
        }
        SSLSocket socket = pinnedSocket(DNS_HOST);
        SSLSocket reference = null;
        try {
            reference = hardenedSocket(DNS_HOST);
            assertThat(socket).isInstanceOf(SSLSocket.class);
            List<SNIServerName> names = socket.getSSLParameters().getServerNames();
            assertWithMessage("server names on the pinned implicit socket").that(names).isNotNull();
            assertThat(names).hasSize(1);
            assertThat(names.get(0)).isInstanceOf(SNIHostName.class);
            assertThat(((SNIHostName) names.get(0)).getAsciiName()).isEqualTo(DNS_HOST);
            assertWithMessage("AC-4 (d): protocols on the same socket after the server-name step")
                    .that(Arrays.asList(socket.getEnabledProtocols())).containsExactly("TLSv1.2", "TLSv1.3");
            assertWithMessage("AC-4 (d): cipher suites on the same socket after the server-name step,"
                    + " in the default factory's order")
                    .that(Arrays.asList(socket.getEnabledCipherSuites()))
                    .containsExactlyElementsIn(Arrays.asList(reference.getEnabledCipherSuites())).inOrder();
        } finally {
            closeQuietly(socket);
            if (reference != null) {
                closeQuietly(reference);
            }
        }
    }
```

**J4 to J7** (insert after J3's closing brace, before the Helpers banner):
```java
    /**
     * U-065 AC-2 (a), (c) (BUG-020, first refinement of its Suggested Approach): the pinned factory's
     * direct and layered sockets enable exactly TLS 1.2 and TLS 1.3 on this provider, which supports
     * SSLv3, TLS 1.0, TLS 1.1 and SSLv2Hello and leaves them off by default. A pinned factory that took
     * the default factory's old list (every supported protocol except SSLv3) would add TLSv1, TLSv1.1 and
     * SSLv2Hello here and fail. Mutations: M-P3 (that widening), M-P1/M-P8 (the floor removed). It
     * compares sets, so a socket the factory left untouched passes it on this provider (its defaults are
     * TLS 1.3 and 1.2); the parity test below catches that by order (M-P7).
     */
    @Test
    public void pinnedFactory_createSocket_enablesExactlyTls12AndTls13_bothShapes_jdkProvider() throws Exception {
        SSLSocket plain = defaultSocket();
        List<String> supported;
        List<String> defaultEnabled;
        try {
            supported = Arrays.asList(plain.getSupportedProtocols());
            defaultEnabled = Arrays.asList(plain.getEnabledProtocols());
        } finally {
            closeQuietly(plain);
        }
        for (String name : OLD_PROTOCOLS) {
            assertWithMessage("precondition: " + name + " must be supported and not enabled by default on"
                    + " this provider; supported=" + supported + ", default enabled=" + defaultEnabled)
                    .that(supported.contains(name) && !defaultEnabled.contains(name)).isTrue();
        }

        SSLSocket direct = pinnedSocket(DNS_HOST);
        try {
            List<String> enabled = Arrays.asList(direct.getEnabledProtocols());
            assertWithMessage("pinned direct socket").that(enabled).containsExactly("TLSv1.2", "TLSv1.3");
            assertProtocolFloor("pinned direct socket", supported, enabled);
        } finally {
            closeQuietly(direct);
        }
        try (Layered layered = new Layered(pinnedFactory(), DNS_HOST)) {
            List<String> enabled = Arrays.asList(layered.socket.getEnabledProtocols());
            assertWithMessage("pinned layered socket").that(enabled).containsExactly("TLSv1.2", "TLSv1.3");
            assertProtocolFloor("pinned layered socket", supported, enabled);
        }
    }

    /**
     * U-065 AC-4, statements S2, S3 and S4 of CR-013 for the pinned factory: for an IPv4-form host, a
     * host with a colon, and a null, empty or blank host, the direct socket carries no server name and
     * createSocket does not throw; the layered socket is created without an exception (AC-4 (b), (c): on
     * this provider a layered socket may carry a name the provider set itself, so only the absence of an
     * exception is asserted there; this provider accepts a null layered host, U-065 probe). Every host is
     * tried before the assertion, so a failure lists all of them. These hold with or without the step
     * (the shared guard skips them); they fail if the pinned path ever sets a name for them (S6).
     */
    @Test
    public void pinnedFactory_createSocket_ipLiteralAndBlankHosts_setNoServerName_jdkProvider() throws Exception {
        String[] hosts = {"192.168.1.10", "::1", "fe80::1", IPV6_EIGHT_GROUP_LITERAL, null, "", "   "};
        List<String> problems = new ArrayList<>();
        for (String host : hosts) {
            SSLSocket direct;
            try {
                direct = pinnedSocket(host);
            } catch (RuntimeException e) {
                problems.add("direct " + quote(host) + " threw " + e);
                continue;
            }
            try {
                List<String> names = serverNames(direct);
                if (!names.isEmpty()) {
                    problems.add("direct " + quote(host) + " carries server names " + names);
                }
            } finally {
                closeQuietly(direct);
            }
            try (Layered layered = new Layered(pinnedFactory(), host)) {
                // created without an exception; the cast in Layered checks that it is an SSLSocket
            } catch (RuntimeException e) {
                problems.add("layered " + quote(host) + " threw " + e);
            }
        }
        assertWithMessage("S2 to S4 on the pinned factory; hosts tried: " + Arrays.toString(hosts))
                .that(problems).isEmpty();
    }

    /**
     * U-065 AC-4, statement S5 of CR-013 for the pinned factory (the call-through test the U-064 reviews
     * asked for): for each name that passes the IP/blank guard and that SNIHostName rejects, the guard
     * answers false, the pinned factory's direct createSocket throws nothing and its socket carries no
     * server name, and the layered createSocket throws nothing. Only the catch of IllegalArgumentException
     * in DefaultTrustedSocketFactory.setSniViaSSLParameters provides this; the pinned factory reaches it
     * through setSniHost. Mutation: that catch removed (M-S5, the edit of U-064's M-N2).
     */
    @Test
    public void pinnedFactory_createSocket_nameRejectedBySniHostName_doesNotThrowAndSetsNoServerName_jdkProvider()
            throws Exception {
        String[] names = {"bad_host.example", "host.example.", "-x.example"};
        java.lang.reflect.Method guard = DefaultTrustedSocketFactory.class
                .getDeclaredMethod("isIpOrBlankHostname", String.class);
        guard.setAccessible(true);
        List<String> escaped = new ArrayList<>();

        for (String name : names) {
            assertWithMessage("isIpOrBlankHostname(\"" + name + "\") must be false, so the name reaches"
                    + " SNIHostName").that((Boolean) guard.invoke(null, name)).isFalse();
            SSLSocket direct;
            try {
                direct = pinnedSocket(name);
            } catch (RuntimeException e) {
                escaped.add("direct " + name + " -> " + e);
                continue;
            }
            try {
                assertWithMessage("server names on the pinned direct socket for \"" + name + "\"")
                        .that(serverNames(direct)).isEmpty();
            } finally {
                closeQuietly(direct);
            }
            try (Layered layered = new Layered(pinnedFactory(), name)) {
                // created without an exception
            } catch (RuntimeException e) {
                escaped.add("layered " + name + " -> " + e);
            }
        }
        assertWithMessage("the pinned createSocket must not throw for a name SNIHostName rejects; names"
                + " tried: " + Arrays.toString(names) + "; escaped").that(escaped).isEmpty();
    }

    /**
     * U-065 AC-5 and AC-3 (a) (JDK provider): for each host, the pinned factory's direct socket and the
     * default factory's direct socket have the same enabled protocols (as a set and in order), the same
     * enabled cipher suites in the same order, and the same server names; for a DNS host the protocols and
     * cipher suites of the two layered sockets are compared too (their names are not: the provider sets
     * them itself). Precondition: the default factory's cipher order is not the provider's own, so parity
     * cannot hold on provider defaults. A drift of one factory's protocol, cipher or server-name handling
     * fails here (M-P6 (i) to (iii)). It does not prescribe how the code is shared.
     */
    @Test
    public void pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider() throws Exception {
        SSLSocket plain = defaultSocket();
        SSLSocket probe = hardenedSocket(DNS_HOST);
        try {
            assertWithMessage("precondition: the default factory's cipher order must differ from the"
                    + " provider's own")
                    .that(Arrays.asList(probe.getEnabledCipherSuites()))
                    .isNotEqualTo(Arrays.asList(plain.getEnabledCipherSuites()));
        } finally {
            closeQuietly(plain);
            closeQuietly(probe);
        }

        String[] hosts = {DNS_HOST, "192.168.1.10", "::1", "", null, "bad_host.example"};
        for (String host : hosts) {
            SSLSocket pinned = pinnedSocket(host);
            SSLSocket reference = null;
            try {
                reference = hardenedSocket(host);
                assertSameHandling("direct socket for host " + quote(host), reference, pinned);
                assertWithMessage("server names, direct socket for host " + quote(host))
                        .that(serverNames(pinned)).isEqualTo(serverNames(reference));
            } finally {
                closeQuietly(pinned);
                if (reference != null) {
                    closeQuietly(reference);
                }
            }
        }
        try (Layered pinned = new Layered(pinnedFactory(), DNS_HOST);
             Layered reference = new Layered(
                     new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication()), DNS_HOST)) {
            assertSameHandling("layered socket for host " + quote(DNS_HOST), reference.socket, pinned.socket);
        }
    }
```

**J-help** (insert after `hardenedSocket(String host)` at `:348`):
```java
    /** The pinned factory, built with the DNS host; the other-host leaf is enrolled (no handshake here). */
    private static PinnedCertificateSocketFactory pinnedFactory() throws Exception {
        return new PinnedCertificateSocketFactory(RuntimeEnvironment.getApplication(), DNS_HOST,
                certificate(OTHER_HOST_CERT));
    }

    /** The pinned factory's own socket for the given host, unconnected. */
    private static SSLSocket pinnedSocket(String host) throws Exception {
        return (SSLSocket) pinnedFactory().createSocket(null, host, 993, null);
    }

    /** The ASCII server names the socket reports; empty for null or none. */
    private static List<String> serverNames(SSLSocket socket) {
        List<SNIServerName> names = socket.getSSLParameters().getServerNames();
        List<String> out = new ArrayList<>();
        if (names != null) {
            for (SNIServerName name : names) {
                out.add(name instanceof SNIHostName ? ((SNIHostName) name).getAsciiName() : name.toString());
            }
        }
        return out;
    }

    /** AC-1 (a) and (b) for one socket: none of the old four; TLS 1.2 and 1.3 each enabled iff supported. */
    private static void assertProtocolFloor(String label, List<String> supported, List<String> enabled) {
        for (String old : OLD_PROTOCOLS) {
            assertWithMessage(label + ": " + old + " must not be enabled; enabled=" + enabled)
                    .that(enabled).doesNotContain(old);
        }
        for (String modern : ALLOWED_PROTOCOLS_BASELINE) {
            assertWithMessage(label + ": " + modern + " enabled must equal " + modern + " supported;"
                    + " supported=" + supported + ", enabled=" + enabled)
                    .that(enabled.contains(modern)).isEqualTo(supported.contains(modern));
        }
        Set<String> expected = new HashSet<>(supported);
        expected.retainAll(Arrays.asList(ALLOWED_PROTOCOLS_BASELINE));
        assertWithMessage(label + ": the enabled set must be the supported protocols that are TLS 1.2 or"
                + " newer").that(new HashSet<>(enabled)).isEqualTo(expected);
    }

    /** Protocols as a set and in order, and cipher suites in order, of candidate against reference. */
    private static void assertSameHandling(String label, SSLSocket reference, SSLSocket candidate) {
        List<String> expectedProtocols = Arrays.asList(reference.getEnabledProtocols());
        List<String> actualProtocols = Arrays.asList(candidate.getEnabledProtocols());
        assertWithMessage(label + ": enabled protocols as a set")
                .that(new HashSet<>(actualProtocols)).isEqualTo(new HashSet<>(expectedProtocols));
        assertWithMessage(label + ": enabled protocols in order")
                .that(actualProtocols).containsExactlyElementsIn(expectedProtocols).inOrder();
        assertWithMessage(label + ": enabled cipher suites in order")
                .that(Arrays.asList(candidate.getEnabledCipherSuites()))
                .containsExactlyElementsIn(Arrays.asList(reference.getEnabledCipherSuites())).inOrder();
    }

    private static String quote(String host) {
        return host == null ? "null" : "\"" + host + "\"";
    }

    private static void quietClose(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // nothing to do for a test socket
            }
        }
    }

    /** A socket a factory layered over a fresh plain loopback connection, with the plain sockets it needs. */
    private static final class Layered implements java.io.Closeable {
        private final ServerSocket listener;
        private final Socket client;
        private final Socket accepted;
        private final SSLSocket socket;

        Layered(TrustedSocketFactory factory, String host) throws Exception {
            ServerSocket l = LoopbackTlsTestServer.bindPlainLoopback();
            Socket c = null;
            Socket a = null;
            SSLSocket s;
            try {
                c = LoopbackTlsTestServer.connectPlain(l.getLocalPort());
                a = l.accept();
                s = (SSLSocket) factory.createSocket(c, host, l.getLocalPort(), null);
            } catch (Exception | Error e) {
                quietClose(c);
                quietClose(a);
                quietClose(l);
                throw e;
            }
            listener = l;
            client = c;
            accepted = a;
            socket = s;
        }

        @Override
        public void close() {
            quietClose(socket);
            quietClose(client);
            quietClose(accepted);
            quietClose(listener);
        }
    }
```

### 3.5 `DefaultTrustedSocketFactoryHandshakeTest.java` (LF)

Allowed HEAD ranges for hunks: Javadoc lines `:56-73` (one bullet added before `:73`), `:82-83`, after
`:105`, `:116-118`; insertion after `:270`. The eight existing tests, `setUp`, `tearDown`, the scenario
helpers `:272-360` and the guard `:366-411` show no hunk.

- Javadoc, insert before the ` * </ul>` that closes "What they prove" (`:73`):
```java
 *   <li>Protocol floor (U-065 AC-8; BUG-019; Record 8 of 2026-11-20): against a server limited to TLS 1.2
 *       the handshake completes at TLS 1.2 on both call shapes (the first handshakes below TLS 1.3 in these
 *       tests); against a server limited to TLS 1.1 or to TLS 1.0 the client refuses with an
 *       {@link SSLException} that is not a certificate rejection, and the server does not report
 *       {@code OK}. The server presents the right-host chain in all of them, so only the protocol can stop
 *       them.</li>
```
- Replace `:82-83` with:
```java
 *   <li>Every handshake here ran at TLS 1.3 (environment probe), except the U-065 tests whose server is
 *       limited to one version. TLS 1.0 and 1.1 are shown only as refused, under this provider.</li>
```
- Insert after `:105` (the last mutation bullet):
```java
 *   <li>U-065 AC-8: the refusal tests break when the TLS 1.2 floor is removed (M-P1/M-P8) or the old list
 *       is given back to the default factory alone (M-P6 (ii)); the TLS 1.2 tests are controls that keep
 *       passing under those mutations.</li>
```
- Replace `:116-118` with:
```java
 * <p><b>Isolation guard A1-G2.</b> The before-method fails, naming A1-G2, if the factory's static cipher
 * list was computed under the JDK provider (it would contain DHE suites, which this class's provider never
 * produces). That would mean the JDK-provider class's Robolectric sandbox was reused here. The guard also
 * has an SSLv2Hello clause on the protocol list; since U-065 the factory never enables SSLv2Hello under
 * either provider, so that clause can no longer fire and the DHE clause alone carries the guard (U-065
 * AC-6 (e); the guard code is left unchanged).
```
- Guard `:366-389`: **no code edit** (iteration 2, plan-review O-5; same reasoning as 3.3).
- New tests and helpers, inserted after the closing brace of `startTls_expiredChain_rejected` (`:270`):
```java
    /**
     * U-065 AC-8 (a), implicit TLS: against a server limited to TLS 1.2 the handshake completes at TLS 1.2,
     * the client reads the byte the server writes, and the server reports OK. It shows that the TLS 1.2
     * floor does not exclude TLS 1.2.
     */
    @Test
    public void implicitTls_serverLimitedToTls12_handshakeCompletesAtTls12() throws Exception {
        assertCompletesAt(IMPLICIT_TLS, "TLSv1.2");
    }

    /** U-065 AC-8 (a), STARTTLS: as the implicit test, on the layered call. */
    @Test
    public void startTls_serverLimitedToTls12_handshakeCompletesAtTls12() throws Exception {
        assertCompletesAt(STARTTLS, "TLSv1.2");
    }

    /**
     * U-065 AC-8 (b), implicit TLS: against a server limited to TLS 1.1 the client's handshake throws an
     * SSLException that is not a certificate rejection, and the server does not report OK. Control: with
     * the floor removed (M-P1/M-P8) the same handshake completes (U-065 probe and mutation log).
     */
    @Test
    public void implicitTls_serverLimitedToTls11_handshakeRefused() throws Exception {
        assertRefusedByProtocolFloor(IMPLICIT_TLS, "TLSv1.1");
    }

    /** U-065 AC-8 (b), STARTTLS, TLS 1.1. */
    @Test
    public void startTls_serverLimitedToTls11_handshakeRefused() throws Exception {
        assertRefusedByProtocolFloor(STARTTLS, "TLSv1.1");
    }

    /** U-065 AC-8 (b), implicit TLS, TLS 1.0. */
    @Test
    public void implicitTls_serverLimitedToTls10_handshakeRefused() throws Exception {
        assertRefusedByProtocolFloor(IMPLICIT_TLS, "TLSv1");
    }

    /** U-065 AC-8 (b), STARTTLS, TLS 1.0. */
    @Test
    public void startTls_serverLimitedToTls10_handshakeRefused() throws Exception {
        assertRefusedByProtocolFloor(STARTTLS, "TLSv1");
    }

    /** The right-host chain, a server limited to one protocol; the handshake must complete at it. */
    private void assertCompletesAt(LoopbackTlsTestServer.Mode mode, String protocol) throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT), new String[] {protocol});
        SSLSocket ssl = connectClient(mode);

        ssl.startHandshake();
        int data = ssl.getInputStream().read();

        assertWithMessage("the byte the server writes after the handshake").that(data)
                .isEqualTo(LoopbackTlsTestServer.PAYLOAD);
        assertWithMessage("negotiated protocol").that(ssl.getSession().getProtocol()).isEqualTo(protocol);
        assertWithMessage("server outcome").that(server.awaitOutcome()).isEqualTo("OK");
    }

    /** The right-host chain, a server limited to one old protocol; the client must refuse the handshake. */
    private void assertRefusedByProtocolFloor(LoopbackTlsTestServer.Mode mode, String protocol)
            throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT), new String[] {protocol});
        SSLSocket ssl = connectClient(mode);

        try {
            ssl.startHandshake();
            fail("the handshake against a server limited to " + protocol + " must be refused, but it"
                    + " completed at " + ssl.getSession().getProtocol());
        } catch (SSLException e) {
            assertWithMessage("the refusal must not be a certificate rejection; chain=" + describeChain(e))
                    .that(e.getCause() instanceof CertificateException).isFalse();
            assertWithMessage("server outcome against a client that refused " + protocol)
                    .that(server.awaitOutcome()).isNotEqualTo("OK");
        }
    }
```
No import changes (`SSLException`, `CertificateException`, `IMPLICIT_TLS`, `STARTTLS` and the fixtures are
already imported).

### 3.6 `PinnedCertificateSocketFactoryCreateSocketTest.java` (LF)

Allowed HEAD ranges for hunks: imports `:3-30`; class Javadoc `:32-99`; `:122-154` (the two changed tests);
insertion after `:196`; helpers inserted after `:213`. Unchanged: `:100-120`, `:156-196`, `:202-213`,
`:215-224`.

- Imports: replace `import com.zegoggles.smssync.mail.ssl.LoopbackTlsTestServer;` with
```java
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.zegoggles.smssync.mail.ssl.LoopbackTlsTestServer;
```
  and replace `import java.security.cert.CertificateException;` with
```java
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
```
  No other import changes: `OTHER_HOST_KEY_PKCS8`, `TRUSTED_CA_CERT`, `serverContext`, `fail`,
  `SSLException`, `ServerSocket` and `Socket` are already imported.
- Class Javadoc `:32-99`, replaced whole (comment-only; the AC-20, AC-21, "fresh TLS context", "IMPLICIT
  connection", "Global state" and "After the CR-009 move" texts are copied unchanged where marked):
```java
/**
 * Tests for the sockets that {@link PinnedCertificateSocketFactory#createSocket} creates (story U-063,
 * Group E, acceptance criteria AC-18 to AC-21; changed and extended by story U-065). The existing
 * {@code PinnedCertificateSocketFactoryTest} calls the trust manager directly through its package-private
 * seam and never calls {@code createSocket}; these tests check the trust manager that {@code createSocket}
 * actually installs, by running real handshakes, and the socket it returns. Nothing the existing test
 * covers is repeated, and the seam is not used here.
 *
 * <p>The pinned factory's server-name checks (U-063 AC-19, U-065 AC-4) are in
 * {@code DefaultTrustedSocketFactoryJdkProviderTest}: the default test provider reports no server name
 * whatever is set, as mutation M-E2 showed on 2026-11-17. Its cipher suites are also checked against the
 * recorded rule in {@code DefaultTrustedSocketFactoryHardeningTest} (U-065 AC-3 (b)).
 *
 * <p><b>What the code does, and what these tests record.</b> The pinned factory builds a plain TLS context
 * with its own pinning trust manager and returns an unconnected socket or one layered over a plain socket.
 * Since U-065 it gives every socket the default factory's handling by calling the default factory's
 * {@code hardenSocket} and then its {@code setSniHost}: only TLS 1.2 and TLS 1.3 where the provider supports
 * them, the default factory's cipher-suite order and blocked list, and the host as server name for a DNS
 * name. Decided by the product owner on 2026-11-20 (Record 8: nothing older than TLS 1.2, for both
 * factories; Record 9: cipher suites and server name "Same as default"; BUG-019, BUG-020). Until U-065 the
 * pinned path left the provider's defaults untouched, which the two AC-18 tests recorded. It still does not
 * verify the host name (the host given to its constructor is not stored) and ignores the client-certificate
 * alias. Every factory in this class is built with the host {@code imap.right.test}, whatever certificate is
 * enrolled, so that a mutation which compares that host with the presented certificate can be detected.
 * <ul>
 *   <li>AC-18 as changed by U-065 (two tests; U-065 AC-2, AC-3 (a) and AC-5 under this provider): on both
 *       call shapes none of TLSv1, TLSv1.1, SSLv3 and SSLv2Hello is enabled, TLS 1.2 and 1.3 are enabled if
 *       and only if the provider supports them, and the enabled protocols and cipher suites equal, in order,
 *       those of the default factory's socket of the same shape. Preconditions: the provider's own default
 *       socket enables TLS 1.0 and 1.1, and its cipher order is not the default factory's, so neither
 *       assertion can be met by the provider's defaults.</li>
 *   <li>[copy the AC-20 bullet, :56-60, unchanged]</li>
 *   <li>[copy the AC-21 bullet, :61-63, unchanged]</li>
 *   <li>U-065 AC-8: against a server limited to TLS 1.2 the handshake completes at TLS 1.2 on both shapes;
 *       against a server limited to TLS 1.1 or to TLS 1.0 the client refuses with an {@link SSLException}
 *       that is not a certificate rejection, and the server does not report {@code OK}. The server presents
 *       the enrolled certificate in all of them, so only the protocol can stop them.</li>
 * </ul>
 *
 * <p><b>What they cannot prove.</b>
 * <ul>
 *   <li>The provider is a desktop test provider, not a device's. What a device enables by default is not
 *       shown.</li>
 *   <li>Every handshake here runs at TLS 1.3, except the U-065 tests whose server is limited to one
 *       version. TLS 1.0 and 1.1 are shown only as refused, under this provider; a downgrade forced by a
 *       third party is not examined.</li>
 *   <li>[copy the "fresh TLS context" bullet, :71-74, unchanged]</li>
 *   <li>[copy the "IMAP connection class / SC-1" bullet, :75-76, unchanged]</li>
 *   <li>An expired enrolled certificate is presented by {@code PinnedCertificateSocketFactoryExpiryTest}
 *       (U-064); a not-yet-valid one by no test. Certificate path validation (RFC 5280) and host-name checks
 *       (RFC 6125 section 6) are deliberately absent from the pinned path; pinning compares a SHA-256
 *       fingerprint.</li>
 * </ul>
 *
 * <p><b>Mutations that must break each test</b> (ids as in the U-063 and U-065 mutation logs).
 * <ul>
 *   <li>AC-18 as changed, both tests: the floor removed from the shared step (M-P1/M-P8); the pinned
 *       protocol handling removed (M-P2) or its cipher handling removed (M-P4); one factory drifting
 *       (M-P6 (i), (ii)); the layered test also: the handling applied to the direct shape only (M-P7).</li>
 *   <li>AC-20: a host-name comparison added to the pinning trust manager (M-E3), or the fingerprint
 *       comparison inverted (M-E4).</li>
 *   <li>AC-21: the fingerprint comparison inverted (M-E4), or the pinning trust manager replaced by an
 *       accept-all manager (M-E5); both re-run by U-065.</li>
 *   <li>U-065 AC-8: the TLS 1.2 tests break under M-E4 (the pin then fails); the refusal tests break when
 *       the floor is removed (M-P1/M-P8) or the pinned protocol handling is removed (M-P2), the STARTTLS
 *       ones also under M-P7.</li>
 * </ul>
 *
 * <p><b>Global state.</b> [copy :91-94 unchanged]
 *
 * <p><b>After the CR-009 move:</b> [copy :96-98 unchanged]
 */
```
(The previous sentence "This class therefore holds AC-18, AC-20 and AC-21 (4 tests)" is dropped: it is no
longer true. The superseded SC-6 sentence about the expired certificate is corrected in the same edit.)

- C1 (replace `:122-134`):
```java
    /**
     * U-065 AC-2 (a), (b), AC-3 (a), AC-5 under this provider, implicit shape. Was AC-18 of U-063, which
     * recorded that the pinned path left the provider's defaults untouched (BUG-020); changed by the
     * product owner's decisions of 2026-11-20 (Records 8 and 9).
     */
    @Test
    public void createSocket_implicit_appliesDefaultFactoryProtocolAndCipherHandling() throws Exception {
        SSLSocket reference = defaultSocket();
        SSLSocket defaultFactorySocket = null;
        try {
            List<String> supported = Arrays.asList(reference.getSupportedProtocols());
            assertProviderDefaultsEnableTls10AndTls11(reference);
            client = (SSLSocket) factory().createSocket(null, RIGHT_HOST_NAME, 993, null);
            defaultFactorySocket = (SSLSocket) defaultFactory().createSocket(null, RIGHT_HOST_NAME, 993, null);

            assertCipherOrderIsNotTheProviders(reference, defaultFactorySocket);
            assertProtocolFloor("pinned implicit socket", supported, Arrays.asList(client.getEnabledProtocols()));
            assertSameHandling("implicit", defaultFactorySocket, client);
        } finally {
            closeQuietly(reference);
            closeQuietly(defaultFactorySocket);
        }
    }
```
- C2 (replace `:136-154`):
```java
    /**
     * U-065 AC-2 (a), (b), AC-3 (a), AC-5 under this provider, layered shape: over a plain loopback
     * connection with one accept and no handshake; the default factory's socket for comparison is layered
     * over a second plain connection. Was the layered AC-18 test of U-063.
     */
    @Test
    public void createSocket_layered_appliesDefaultFactoryProtocolAndCipherHandling() throws Exception {
        SSLSocket reference = defaultSocket();
        ServerSocket otherListener = null;
        Socket otherPlain = null;
        Socket otherAccepted = null;
        SSLSocket defaultFactorySocket = null;
        try {
            List<String> supported = Arrays.asList(reference.getSupportedProtocols());
            assertProviderDefaultsEnableTls10AndTls11(reference);
            listener = LoopbackTlsTestServer.bindPlainLoopback();
            plainClient = LoopbackTlsTestServer.connectPlain(listener.getLocalPort());
            plainAccepted = listener.accept();
            client = (SSLSocket) factory().createSocket(plainClient, RIGHT_HOST_NAME, listener.getLocalPort(), null);
            otherListener = LoopbackTlsTestServer.bindPlainLoopback();
            otherPlain = LoopbackTlsTestServer.connectPlain(otherListener.getLocalPort());
            otherAccepted = otherListener.accept();
            defaultFactorySocket = (SSLSocket) defaultFactory()
                    .createSocket(otherPlain, RIGHT_HOST_NAME, otherListener.getLocalPort(), null);

            assertCipherOrderIsNotTheProviders(reference, defaultFactorySocket);
            assertProtocolFloor("pinned layered socket", supported, Arrays.asList(client.getEnabledProtocols()));
            assertSameHandling("layered", defaultFactorySocket, client);
        } finally {
            closeQuietly(reference);
            closeQuietly(defaultFactorySocket);
            closeQuietly(otherPlain);
            closeQuietly(otherAccepted);
            closeQuietly(otherListener);
        }
    }
```
- New handshake tests, inserted after the closing brace of `handshake_certificateOtherThanEnrolled_isRejected`
  (`:196`):
```java
    /** U-065 AC-8 (a), implicit TLS: a server limited to TLS 1.2 presenting the enrolled certificate. */
    @Test
    public void handshake_implicitTls_serverLimitedToTls12_completesAtTls12() throws Exception {
        assertPinnedHandshakeCompletesAt(LoopbackTlsTestServer.Mode.IMPLICIT_TLS, "TLSv1.2");
    }

    /** U-065 AC-8 (a), STARTTLS. */
    @Test
    public void handshake_startTls_serverLimitedToTls12_completesAtTls12() throws Exception {
        assertPinnedHandshakeCompletesAt(LoopbackTlsTestServer.Mode.STARTTLS, "TLSv1.2");
    }

    /** U-065 AC-8 (b), implicit TLS, TLS 1.1. Control: M-P1/M-P8 and M-P2 make it complete. */
    @Test
    public void handshake_implicitTls_serverLimitedToTls11_isRefused() throws Exception {
        assertPinnedHandshakeRefused(LoopbackTlsTestServer.Mode.IMPLICIT_TLS, "TLSv1.1");
    }

    /** U-065 AC-8 (b), STARTTLS, TLS 1.1. */
    @Test
    public void handshake_startTls_serverLimitedToTls11_isRefused() throws Exception {
        assertPinnedHandshakeRefused(LoopbackTlsTestServer.Mode.STARTTLS, "TLSv1.1");
    }

    /** U-065 AC-8 (b), implicit TLS, TLS 1.0. */
    @Test
    public void handshake_implicitTls_serverLimitedToTls10_isRefused() throws Exception {
        assertPinnedHandshakeRefused(LoopbackTlsTestServer.Mode.IMPLICIT_TLS, "TLSv1");
    }

    /** U-065 AC-8 (b), STARTTLS, TLS 1.0. */
    @Test
    public void handshake_startTls_serverLimitedToTls10_isRefused() throws Exception {
        assertPinnedHandshakeRefused(LoopbackTlsTestServer.Mode.STARTTLS, "TLSv1");
    }
```
- Helpers, inserted after the closing brace of `defaultSocket()` (`:213`):
```java
    private static final String[] ALLOWED_PROTOCOLS_BASELINE = {"TLSv1.2", "TLSv1.3"};
    private static final String[] OLD_PROTOCOLS = {"TLSv1", "TLSv1.1", "SSLv3", "SSLv2Hello"};

    private static DefaultTrustedSocketFactory defaultFactory() {
        return new DefaultTrustedSocketFactory(RuntimeEnvironment.getApplication());
    }

    /** Creates the pinned client in the call shape of the IMAP code; the enrolled leaf's server must run. */
    private SSLSocket connectPinned(LoopbackTlsTestServer.Mode mode) throws Exception {
        int port = server.port();
        if (mode == LoopbackTlsTestServer.Mode.IMPLICIT_TLS) {
            client = (SSLSocket) factory().createSocket(null, RIGHT_HOST_NAME, port, null);
            LoopbackTlsTestServer.connect(client, port);
        } else {
            plainClient = LoopbackTlsTestServer.connectPlain(port);
            assertThat(LoopbackTlsTestServer.readLine(plainClient)).isEqualTo(LoopbackTlsTestServer.GREETING);
            client = (SSLSocket) factory().createSocket(plainClient, RIGHT_HOST_NAME, port, null);
        }
        return client;
    }

    private void assertPinnedHandshakeCompletesAt(LoopbackTlsTestServer.Mode mode, String protocol)
            throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(OTHER_HOST_CERT, OTHER_HOST_KEY_PKCS8, TRUSTED_CA_CERT), new String[] {protocol});
        SSLSocket ssl = connectPinned(mode);

        ssl.startHandshake();
        int data = ssl.getInputStream().read();

        assertWithMessage("the byte the server writes after the handshake").that(data)
                .isEqualTo(LoopbackTlsTestServer.PAYLOAD);
        assertWithMessage("negotiated protocol").that(ssl.getSession().getProtocol()).isEqualTo(protocol);
        assertWithMessage("server outcome").that(server.awaitOutcome()).isEqualTo("OK");
    }

    private void assertPinnedHandshakeRefused(LoopbackTlsTestServer.Mode mode, String protocol)
            throws Exception {
        server = LoopbackTlsTestServer.start(mode,
                serverContext(OTHER_HOST_CERT, OTHER_HOST_KEY_PKCS8, TRUSTED_CA_CERT), new String[] {protocol});
        SSLSocket ssl = connectPinned(mode);

        try {
            ssl.startHandshake();
            fail("the handshake against a server limited to " + protocol + " must be refused, but it"
                    + " completed at " + ssl.getSession().getProtocol());
        } catch (SSLException e) {
            assertWithMessage("the refusal must not be a certificate rejection; direct cause="
                    + e.getCause()).that(e.getCause() instanceof CertificateException).isFalse();
            assertWithMessage("server outcome against a client that refused " + protocol)
                    .that(server.awaitOutcome()).isNotEqualTo("OK");
        }
    }

    private static void assertProviderDefaultsEnableTls10AndTls11(SSLSocket reference) {
        List<String> enabled = Arrays.asList(reference.getEnabledProtocols());
        for (String name : Arrays.asList("TLSv1", "TLSv1.1")) {
            assertWithMessage("precondition AC-2 (b): the provider's default socket must enable " + name
                    + ", so that its absence on the pinned socket is the factory's doing; default enabled="
                    + enabled).that(enabled).contains(name);
        }
    }

    private static void assertCipherOrderIsNotTheProviders(SSLSocket reference, SSLSocket defaultFactorySocket) {
        assertWithMessage("precondition AC-3 (a): the default factory's cipher order must differ from the"
                + " provider's own, so that parity cannot hold on the provider's defaults")
                .that(Arrays.asList(defaultFactorySocket.getEnabledCipherSuites()))
                .isNotEqualTo(Arrays.asList(reference.getEnabledCipherSuites()));
    }

    private static void assertProtocolFloor(String label, List<String> supported, List<String> enabled) {
        for (String old : OLD_PROTOCOLS) {
            assertWithMessage(label + ": " + old + " must not be enabled; enabled=" + enabled)
                    .that(enabled).doesNotContain(old);
        }
        for (String modern : ALLOWED_PROTOCOLS_BASELINE) {
            assertWithMessage(label + ": " + modern + " enabled must equal " + modern + " supported;"
                    + " supported=" + supported + ", enabled=" + enabled)
                    .that(enabled.contains(modern)).isEqualTo(supported.contains(modern));
        }
        Set<String> expected = new HashSet<>(supported);
        expected.retainAll(Arrays.asList(ALLOWED_PROTOCOLS_BASELINE));
        assertWithMessage(label + ": the enabled set must be the supported protocols that are TLS 1.2 or"
                + " newer").that(new HashSet<>(enabled)).isEqualTo(expected);
    }

    private static void assertSameHandling(String shape, SSLSocket reference, SSLSocket candidate) {
        List<String> expectedProtocols = Arrays.asList(reference.getEnabledProtocols());
        List<String> actualProtocols = Arrays.asList(candidate.getEnabledProtocols());
        assertWithMessage(shape + ": enabled protocols as a set")
                .that(new HashSet<>(actualProtocols)).isEqualTo(new HashSet<>(expectedProtocols));
        assertWithMessage(shape + ": enabled protocols in order, against the default factory")
                .that(actualProtocols).containsExactlyElementsIn(expectedProtocols).inOrder();
        assertWithMessage(shape + ": enabled cipher suites in order, against the default factory")
                .that(Arrays.asList(candidate.getEnabledCipherSuites()))
                .containsExactlyElementsIn(Arrays.asList(reference.getEnabledCipherSuites())).inOrder();
    }
```

### 3.7 `LoopbackTlsTestServer.java` (LF; test-only, additive; AC-8 condition)

Allowed HEAD ranges: Javadoc insertion before `:39`; `:79-97`; insertion after `:109`. The client helpers
`:163-219` and `close`/`awaitOutcome` are unchanged; the Conscrypt rule of `:39-42` (always wrap an accepted
plain socket with the loopback address string) is kept.

- Javadoc, insert before ` * <p><b>Why it always wraps a plain socket.</b>`:
```java
 * <p><b>Protocol limit (U-065).</b> {@link #start(Mode, SSLContext, String[])} limits the server's TLS socket
 * to the given protocol versions, set after the socket is put in server mode and before its handshake, so that
 * a test can show which versions a client still negotiates. {@link #start(Mode, SSLContext)} leaves the
 * server's protocols as the context enables them, exactly as before.
 *
```
- Replace the constructor `:79-87`:
```java
    private LoopbackTlsTestServer(final Mode mode, final SSLContext serverContext,
                                  final String[] serverProtocols) throws IOException {
        this.serverSocket = bindPlainLoopback();
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "u063-loopback-test-server");
            thread.setDaemon(true);
            return thread;
        });
        this.outcome = executor.submit(() -> serve(mode, serverContext, serverProtocols));
    }
```
- Replace `:93-97` (`start` and the first line of `serve`):
```java
    public static LoopbackTlsTestServer start(Mode mode, SSLContext serverContext) throws IOException {
        return new LoopbackTlsTestServer(mode, serverContext, null);
    }

    /**
     * Starts a server as {@link #start(Mode, SSLContext)} does, whose TLS socket enables only the given
     * protocol versions (story U-065, AC-8). The array must name at least one protocol; the client is not
     * touched. The caller must {@link #close()} it.
     */
    public static LoopbackTlsTestServer start(Mode mode, SSLContext serverContext, String[] serverProtocols)
            throws IOException {
        if (serverProtocols == null || serverProtocols.length == 0) {
            throw new IllegalArgumentException("serverProtocols must name at least one protocol");
        }
        return new LoopbackTlsTestServer(mode, serverContext, serverProtocols.clone());
    }

    private String serve(Mode mode, SSLContext serverContext, String[] serverProtocols) {
```
- After `            tls.setUseClientMode(false);` (`:109`) insert:
```java
            if (serverProtocols != null) {
                tls.setEnabledProtocols(serverProtocols);
            }
```
A server whose provider rejects the protocol list fails inside `serve` and reports `FAILED:<class>`; the
probe shows it (row D3 to D7).

## 4. Implementation Steps

### 4.0 Environment and conventions (every step)

```bash
# From the repository root. Never set MSYS_NO_PATHCONV for Gradle.
unset MSYS_NO_PATHCONV
W=sdlc/artifacts/build/sprints/sprint-021/U-065; E=$W/evidence; M=$E/mutations
T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools      # U-063 tools, read-only
INIT=$T/junit-xml.init.gradle
R=app/build/test-results/testDebugUnitTest
DTSF=k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java
PIN=app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java
TD=app/src/test/java/com/zegoggles/smssync/mail
LTS=$TD/ssl/LoopbackTlsTestServer.java
HARD=$TD/ssl/DefaultTrustedSocketFactoryHardeningTest.java
JDKT=$TD/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java
HSK=$TD/ssl/DefaultTrustedSocketFactoryHandshakeTest.java
CST=$TD/PinnedCertificateSocketFactoryCreateSocketTest.java
BASE=b2c2dfd8d7f0f5de8627575e160215b24b7cc5d9
PIN_BLOB=8a0fbb64fb171624591fbebafb818555f28efd58
DTSF_BLOB=d543927166b3c5e1da0aaa8ab584072a43bf7521
WORK=<a u065-work folder inside the developer's own session scratch folder>   # raw logs, pristine copies
```
Shell state does not persist; repeat what a call needs. `WORK` is the only absolute path and is never
written into a repository file.

- **Class sets** (fully qualified for `--tests`): HARD-C = `com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactoryHardeningTest`;
  JDK-C = `…ssl.DefaultTrustedSocketFactoryJdkProviderTest`; HSK-C = `…ssl.DefaultTrustedSocketFactoryHandshakeTest`;
  SNI-C = `…ssl.DefaultTrustedSocketFactorySniTest`; CST-C = `com.zegoggles.smssync.mail.PinnedCertificateSocketFactoryCreateSocketTest`;
  EXP-C = `…mail.PinnedCertificateSocketFactoryExpiryTest`; PTT-C = `…mail.PinnedCertificateSocketFactoryTest`.
  **ALL7** = all seven. `junit_cases.py` filters: `DefaultTrustedSocketFactory` (the four `ssl` classes),
  `PinnedCertificateSocketFactory` (the three pinned classes), or single simple names.
- **TR(name, classes…)**, every Gradle call in the foreground with the tool timeout 600000 ms:
  ```bash
  rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:testDebugUnitTest --tests "<fqcn1>" [--tests "<fqcn2>" ...] > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> $WORK/logs/<name>.log
  python -I -X utf8 $T/sanitize.py $WORK/logs/<name>.log $WORK/logs/<name>.san --grep '^(BUILD |FAILURE|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'
  python -I -X utf8 $T/junit_cases.py $R <filter...> > <dest>-cases.txt
  ```
  Failure messages, when a test is expected or found to fail:
  `grep -o 'message="[^"]*"' $R/TEST-<fqcn>.xml > $WORK/logs/<name>-msg.log; python -I -X utf8 $T/sanitize.py $WORK/logs/<name>-msg.log <dest>-message.txt`.
- **Gate filter G:** `--grep '^(BUILD |FAILURE|> Task :app:(assemble|test|jacoco)|U063-DAEMON-JVM|exit=)|tests completed|FAILED$'`.
- **Timestamps** in the logs come from `date -u +%Y-%m-%dT%H:%M:%SZ`, run at that moment.
- **Expected Gradle invocations: 21** — I0 gate (1), I2 probe (1), I4 red run (1), I6 green run (1), I7
  mutations (14), I8 determinism (2), I9 gate (1). Contingencies (rows D30, D34) may add at most one repeat
  each; every repeat is logged.

### I0 — Baseline evidence and the first gate

1. `mkdir -p "$WORK/logs" "$WORK/pristine" "$M" "$E/tools"`. One call into `$WORK/logs/I0-baseline.log`,
   sanitised to `$E/I0-baseline.txt`:
   ```bash
   date -u +%Y-%m-%dT%H:%M:%SZ
   git rev-parse HEAD; git rev-parse --abbrev-ref HEAD
   git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat; echo "porcelain-end"
   git rev-parse HEAD:$PIN HEAD:$DTSF; git hash-object $PIN $DTSF
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   git merge-base --is-ancestor bd1d5398 HEAD; echo "record9-ancestor-exit=$?"
   git log --format='%h %cI' -S'### Record 9' -- sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md
   git log --format='%h %cI' -S'### Record 8' -- sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md
   ```
   Expected: HEAD `b2c2dfd8…`; nothing before `porcelain-end`; all four hashes equal the two base blobs;
   count 713; `toolchainVersion=25`; `record9-ancestor-exit=0`; `bd1d5398 …` and `6cc49ab4 …`.
   **Stop:** row D1.
2. Gate: `rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease > $WORK/logs/I0-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I0-gates.log`,
   then sanitise with filter G into `$E/I0-gates.txt` and `python -I -X utf8 $T/junit_totals.py $R > $E/I0-junit-totals.txt`.
   Expected: `exit=0`, `U063-DAEMON-JVM: java.version=25…`, `tests=713 failures=0 errors=0 skipped=2`.
   **Stop:** rows D1, D33.
3. Create `$W/implementation-log.md` (header, I0 entry with the clock time).

### I1 — The server's protocol option (test-only)

Apply section 3.7 with the Edit tool. No Gradle. Record `git diff --stat HEAD -- $LTS` (one file,
additions plus the four replaced lines: constructor signature `:79` and submit `:86`, `start` body `:94`,
`serve` signature `:97`) in `$E/I1-lts-diff-check.txt`.

### I2 — Probes (one Gradle run; before any other test or production edit)

1. Write two throwaway classes with the Write tool:
   `$TD/ssl/U065ConscryptProbeTest.java` and `$TD/ssl/U065JdkProbeTest.java`:
```java
package com.zegoggles.smssync.mail.ssl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.RIGHT_HOST_KEY_PKCS8;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.TRUSTED_CA_CERT;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.serverContext;
import static com.zegoggles.smssync.mail.ssl.TlsTestFixtures.trustManagerFor;

/** THROWAWAY probe for U-065 step I2 under the default (Conscrypt) provider. Deleted after one run. */
@RunWith(RobolectricTestRunner.class)
public class U065ConscryptProbeTest {

    @Test
    public void probe() throws Exception {
        out("provider", SSLContext.getInstance("TLS").getProvider().getName());
        SSLSocket s = clientSocket(null);
        out("supported", String.join(",", s.getSupportedProtocols()));
        out("default_enabled", String.join(",", s.getEnabledProtocols()));
        s.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
        List<String> reversed = new ArrayList<>(Arrays.asList(s.getEnabledCipherSuites()));
        Collections.reverse(reversed);
        s.setEnabledCipherSuites(reversed.toArray(new String[0]));
        String[] before = s.getEnabledCipherSuites();
        SSLParameters p = s.getSSLParameters();
        p.setServerNames(Collections.singletonList(new SNIHostName("imap.right.test")));
        s.setSSLParameters(p);
        out("roundtrip_protocols", String.join(",", s.getEnabledProtocols()));
        out("roundtrip_cipher_order_kept", String.valueOf(Arrays.equals(before, s.getEnabledCipherSuites())));
        try {
            s.setEnabledProtocols(new String[0]);
            out("empty_protocols", "accepted enabled_count=" + s.getEnabledProtocols().length);
        } catch (Exception e) {
            out("empty_protocols", "threw " + e.getClass().getName());
        }
        s.close();
        for (String version : new String[] {"TLSv1.2", "TLSv1.1", "TLSv1"}) {
            out("hs_" + version + "_client_provider_default", handshake(version, false));
            out("hs_" + version + "_client_floor", handshake(version, true));
        }
    }

    private static String handshake(String serverVersion, boolean floor) {
        LoopbackTlsTestServer server = null;
        SSLSocket client = null;
        try {
            server = LoopbackTlsTestServer.start(LoopbackTlsTestServer.Mode.IMPLICIT_TLS,
                    serverContext(RIGHT_HOST_CERT, RIGHT_HOST_KEY_PKCS8, TRUSTED_CA_CERT),
                    new String[] {serverVersion});
            client = clientSocket(new TrustManager[] {trustManagerFor(TRUSTED_CA_CERT)});
            if (floor) {
                client.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});
            }
            LoopbackTlsTestServer.connect(client, server.port());
            client.startHandshake();
            int b = client.getInputStream().read();
            return "completed protocol=" + client.getSession().getProtocol() + " byte=" + b
                    + " server=" + server.awaitOutcome();
        } catch (Throwable e) {
            return "refused type=" + e.getClass().getName()
                    + " server=" + (server == null ? "none" : server.awaitOutcome());
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException ignored) {
                    // probe
                }
            }
            if (server != null) {
                server.close();
            }
        }
    }

    private static SSLSocket clientSocket(TrustManager[] tms) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, tms, null);
        return (SSLSocket) ctx.getSocketFactory().createSocket();
    }

    private static void out(String key, String value) {
        System.out.println("U065-PROBE c_" + key + "=" + value);
    }
}
```
```java
package com.zegoggles.smssync.mail.ssl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.ConscryptMode;
import org.robolectric.annotation.LooperMode;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

import static org.robolectric.annotation.ConscryptMode.Mode.OFF;
import static org.robolectric.annotation.LooperMode.Mode.LEGACY;

/** THROWAWAY probe for U-065 step I2 under the JDK provider (same annotations as the JDK class). */
@RunWith(RobolectricTestRunner.class)
@ConscryptMode(OFF)
@LooperMode(LEGACY)
public class U065JdkProbeTest {

    @Test
    public void probe() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null);
        out("provider", ctx.getProvider().getName());
        SSLSocket u = (SSLSocket) ctx.getSocketFactory().createSocket();
        out("unconnected_names", names(u));
        u.close();
        String[] hosts = {"imap.right.test", "192.168.1.10", "::1", "fe80::1",
                "2001:0db8:0000:0000:0000:0000:0000:0001", "bad_host.example", "host.example.",
                "-x.example", "", "   ", null};
        for (int i = 0; i < hosts.length; i++) {
            out("layered_" + i, "host=" + (hosts[i] == null ? "null" : "'" + hosts[i] + "'")
                    + " " + layered(ctx, hosts[i]));
        }
        SSLSocket e = (SSLSocket) ctx.getSocketFactory().createSocket();
        try {
            e.setEnabledProtocols(new String[0]);
            out("empty_protocols", "accepted enabled_count=" + e.getEnabledProtocols().length);
        } catch (Exception ex) {
            out("empty_protocols", "threw " + ex.getClass().getName());
        }
        e.close();
    }

    private static String layered(SSLContext ctx, String host) {
        try (ServerSocket listener = LoopbackTlsTestServer.bindPlainLoopback();
             Socket plain = LoopbackTlsTestServer.connectPlain(listener.getLocalPort());
             Socket accepted = listener.accept()) {
            SSLSocket s = (SSLSocket) ctx.getSocketFactory().createSocket(plain, host, listener.getLocalPort(), true);
            String result = "names=" + names(s);
            s.close();
            return result;
        } catch (Throwable t) {
            return "threw " + t.getClass().getName();
        }
    }

    private static String names(SSLSocket s) {
        List<SNIServerName> n = s.getSSLParameters().getServerNames();
        if (n == null || n.isEmpty()) {
            return "none";
        }
        List<String> out = new ArrayList<>();
        for (SNIServerName name : n) {
            out.add(name instanceof SNIHostName ? ((SNIHostName) name).getAsciiName() : name.toString());
        }
        return String.join(",", out);
    }

    private static void out(String key, String value) {
        System.out.println("U065-PROBE j_" + key + "=" + value);
    }
}
```
   (Values are bracket-free on purpose so the extraction below cannot cut them.)
2. One run: TR(`I2-probe`, `--tests "com.zegoggles.smssync.mail.ssl.U065ConscryptProbeTest" --tests "com.zegoggles.smssync.mail.ssl.U065JdkProbeTest"`),
   cases into `$E/I2-probe-cases.txt` (filter `U065`). Then
   `grep -ho 'U065-PROBE [^]<]*' $R/TEST-com.zegoggles.smssync.mail.ssl.U065*ProbeTest.xml > $WORK/logs/I2-probe.log`
   and `python -I -X utf8 $T/sanitize.py $WORK/logs/I2-probe.log $E/I2-probe.txt`.
3. Copy each probe source to `$E/tools/<Name>.java.txt` (`cp`), then delete both from `$TD/ssl/` (`rm`), and
   record in `$E/I2-cleanup.txt`: `git status --porcelain -- app/src/test` (expected: only ` M $LTS`) and
   `ls $TD/ssl | grep -c U065` (expected `0`).
4. Expected probe lines (the decision rows D2 to D16 cover every other value):
```
U065-PROBE c_provider=Conscrypt
U065-PROBE c_supported=TLSv1,TLSv1.1,TLSv1.2,TLSv1.3
U065-PROBE c_default_enabled=TLSv1,TLSv1.1,TLSv1.2,TLSv1.3
U065-PROBE c_roundtrip_protocols=TLSv1.2,TLSv1.3
U065-PROBE c_roundtrip_cipher_order_kept=true
U065-PROBE c_empty_protocols=<accepted enabled_count=0 | threw <class>>            (record only)
U065-PROBE c_hs_TLSv1.2_client_provider_default=completed protocol=TLSv1.2 byte=42 server=OK
U065-PROBE c_hs_TLSv1.2_client_floor=completed protocol=TLSv1.2 byte=42 server=OK
U065-PROBE c_hs_TLSv1.1_client_provider_default=completed protocol=TLSv1.1 byte=42 server=OK      (N1)
U065-PROBE c_hs_TLSv1.1_client_floor=refused type=<an SSLException subclass> server=FAILED:<class>
U065-PROBE c_hs_TLSv1_client_provider_default=completed protocol=TLSv1 byte=42 server=OK          (N1)
U065-PROBE c_hs_TLSv1_client_floor=refused type=<an SSLException subclass> server=FAILED:<class>
U065-PROBE j_provider=SunJSSE
U065-PROBE j_unconnected_names=none
U065-PROBE j_layered_0=host='imap.right.test' names=imap.right.test
U065-PROBE j_layered_1..5=… names=none            (IPv4, ::1, fe80::1, eight-group, bad_host.example)
U065-PROBE j_layered_6=host='host.example.' names=host.example
U065-PROBE j_layered_7..10=… names=none           (-x.example, empty, blank, null)
U065-PROBE j_empty_protocols=accepted enabled_count=0
```
   "SSLException subclass" means `javax.net.ssl.SSLException`, `SSLHandshakeException` or
   `SSLProtocolException`. Log the decision row applied for each key in the implementation log.

### I3 — Tests (all test edits; no production edit yet)

I3 starts only when every I2 key has a row that lets it continue (a D6 stop must have its reading). Apply
sections 3.3, 3.4, 3.5, 3.6 with the Edit tool, in that order, leaving out any [R] test that I2 ruled
out (rows D4, D6-b; remove only those methods, keep the helpers; under D6-a apply its catch change). Then one call writing
`$E/I3-test-diff-check.txt`:
```bash
for f in $HARD $JDKT $HSK $CST $LTS; do echo "== $f"; git diff -U0 HEAD -- $f | grep '^@@'; done
git diff --stat HEAD -- app/src/test
git diff --quiet HEAD -- $TD/ssl/DefaultTrustedSocketFactorySniTest.java $TD/PinnedCertificateSocketFactoryExpiryTest.java $TD/PinnedCertificateSocketFactoryTest.java $TD/ssl/TlsTestFixtures.java; echo "untouched-exit=$?"
git grep -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
git grep -n "@Ignore" -- $TD/ssl $TD/Pinned*.java; echo "ignore-exit=$?"
git grep -n "BUG-019" -- $HARD
git diff --quiet HEAD -- app/src/main k9mail-vendored; echo "prod-clean-exit=$?"
```
Expected: every hunk's old-line range inside the allowed ranges of 3.3 to 3.7; `untouched-exit=0`; working
tree count 732 (or as reduced); `ignore-exit=1`; the BUG-019 line above the changed legacy test;
`prod-clean-exit=0`. **Stop:** row D17.

### I4 — Red run (tests against the unchanged production code)

TR(`I4-red`, ALL7), cases into `$E/I4-red-cases.txt` (filters `DefaultTrustedSocketFactory PinnedCertificateSocketFactory`).
Expected: compiles; 68 cases (or as reduced); **failing exactly** (failure or error):
Hardening H1, H2, H3 (error: no field `ALLOWED_PROTOCOLS`), H4 (error: no three-argument `hardenSocket`), H5;
JDK J1, J2, J3, J7; Handshake the four refusal tests [R]; pinned C1, C2 and the four refusal tests [R]
(19, or fewer by 4 per dropped version). **Passing by design** (stated now): J4 (this provider already
enables exactly TLS 1.2 and 1.3 by default and J4 compares sets; protected by M-P3 and M-P1/M-P8), J5 (the guard skips those hosts;
S6 guard), J6 (no step, no exception; protected by M-S5), the four TLS 1.2 handshake tests (controls), and
every unchanged test. **Stop:** rows D18 to D21.

### I5 — Production change

1. Apply D1 to D6 to `$DTSF` and P1 to P4 to `$PIN` (section 2.4). Nothing else in `app/src/main` or
   `k9mail-vendored`.
2. One call writing `$E/I5-production.txt`:
   ```bash
   git diff --name-only HEAD -- app/src/main k9mail-vendored
   git diff -U0 HEAD -- $DTSF | grep '^@@'
   git diff -U0 HEAD -- $PIN | grep '^@@'
   git hash-object $DTSF $PIN
   git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin
   git grep -n "hardenSocket(" -- app/src/main k9mail-vendored/src/main
   ```
   Then `git diff HEAD -- app/src/main k9mail-vendored > $E/I5-production.diff.txt`, copy both files to
   `$WORK/pristine/` (`cp $DTSF $PIN "$WORK/pristine/"`), and store both blobs in the object store as a
   second, durable copy: `git hash-object -w $DTSF $PIN` (it writes objects only: no tracked change, no
   index change, no commit; the printed ids equal `DTSF_NEW` and `PIN_NEW`). Record the two new blobs as
   `DTSF_NEW` and `PIN_NEW`, and the diff hash as `PROD_DIFF`, in the implementation log; they are the restore
   targets. Expected: exactly the two files; no `$DTSF` hunk whose old range intersects `:37-81`, `:83-89`
   (the D1 insertion shows as `-90,0`, after the blank line 90, which is outside), `:152-180`, `:191-266`;
   `hardenSocket(` appears only in `$DTSF`
   (`createSocket` call, the two definitions, the one-argument body) and in `$PIN`. **Stop:** row D22.

### I6 — Green run

TR(`I6-green`, ALL7), cases into `$E/I6-green-cases.txt`. Expected: `exit=0`, 68 cases (or as reduced),
all pass, including U-064's three tests and the three JDK guards. This run is the unmutated baseline of every
mutation and determinism run 1. **Stop:** rows D23 to D26.

### I7 — Mutations (write-ahead and restore protocol)

`$E/mutation-log.md` header: title; "Protocol: U-065 plan section I7 (write-ahead entry, one Edit, one
targeted run, restore by copying the pristine file, hash and porcelain check, `restored` line).
Repository-relative paths."; base commit; the base and the I5 blobs; the recovery note: "If a session is
lost while an entry says `APPLYING` and has no `restored` line, nothing else runs (no gate, no review, no
commit) until the file's hash has been compared with its I5 blob and, if it differs, the file has been
restored from the pristine copy; if that copy is lost, from the object store with
`git cat-file blob <DTSF_NEW or PIN_NEW> > <path>` (it writes LF line endings; with `core.autocrlf=true` and
the files stored as LF, `git hash-object` and the diff hash still match, and the next Gradle compile reads
the file as before); if both are lost, by re-applying section 2.4 of the plan to the base file and checking
the hash."

**Deviation from AC-7's literal protocol (declared here):** the production change is uncommitted while the
mutations run, so (1) the restore copies the pristine file back (`cp "$WORK/pristine/<file>" <path>`)
instead of `git checkout` (which would discard the change), and (2) "empty
`git status --porcelain -- app/src/main k9mail-vendored`" becomes "porcelain lists exactly ` M` for the two
production files, and `git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin` equals
`PROD_DIFF`". The hashes compared are the I5 blobs.

For each mutation, one at a time: (a) clean check (both `hash-object` values equal `DTSF_NEW`/`PIN_NEW`,
porcelain as above, diff hash equals `PROD_DIFF`); (b) write-ahead entry `### <id> APPLYING <clock>`, file,
hash-before, edit, class run, expected; (c) one Edit (Read the lines first; copy `old_string` from the Read
output); (d) one TR, cases into `$M/<id>-cases.txt`, failure messages into `$M/<id>-message.txt`;
(e) restore at once by `cp` from the pristine copy, whatever the result; (f) verify as (a) and append
`run_exit`, `cases`, `failed`, `passed`, `expected: matches | does not match`, `verdict`, and
`restored: <clock> hash-ok porcelain-two-files diff-hash-ok`. No gate, review or commit runs while an entry
lacks its `restored` line. The table names tests by the short names of section 3 (H1 = the changed
supported-protocols test, H2 = the changed legacy test, H3/H4/H5, J1 to J7, C1/C2, the handshake tests by
name).

**Failure types** (iteration 2, plan-review O-1), as `junit_cases.py` prints them after `FAIL`/`ERROR`:
**A** = an assertion failure, i.e. any `java.lang.AssertionError` or subclass (Truth's
`com.google.common.truth.AssertionErrorWithFacts` or `ComparisonFailureWithFacts`, JUnit's `fail(...)` as
`java.lang.AssertionError`, Mockito's verification errors). A named class (for example
`java.lang.IllegalArgumentException`) means that exception escapes the test (`ERROR`); for
`javax.net.ssl.SSLHandshakeException` any `javax.net.ssl.SSLException` subclass matches (the provider
chooses the subclass). Each entry also names the assertion that fails first. [R] entries apply only to the
versions whose refusal tests were written (rows D3, D6-a).

Re-derivation (iteration 2): each row below was walked against the test code of section 3 and, for the
plain-Java behaviour on SunJSSE, against section 1.2. Under Conscrypt the facts used are L13 (supported =
default enabled = TLS 1.0 to 1.3; the factory's cipher order differs from the provider's; no blocked suite
in the defaults).

| Id | File | Exact edit (`old_string` → `new_string`) | Classes run | Expected failing (all others pass): test — first failing assertion (type) |
|----|------|-------------------------------------------|-------------|------------------------------------|
| M-P1/M-P8 | DTSF | `        return enabled.toArray(new String[enabled.size()]);` → `        return ordered;` (the floor removed from the shared step: the old supported-minus-SSLv3 list, for both factories) | HARD-C, JDK-C, HSK-C, CST-C | H1 — set equality, list `[TLSv1.2, TLSv1.1, TLSv1, TLSv1.3]` (A); H2 — "TLSv1 must not be enabled", direct (A); H3 — case (i), result `[TLSv1.2, TLSv1.1, TLSv1, SSLv2Hello, TLSv1.3]` (A); H4 — captured list `containsExactly` (A); J1 — set equality (A); J2 — "TLSv1 must not be enabled", direct (A); J3 — AC-4 (d) protocols `containsExactly` (A); J4 — direct `containsExactly` (A); C1, C2 — `assertProtocolFloor` "TLSv1 must not be enabled" (A); the 4 Handshake and the 4 pinned refusal tests [R] — `fail("… must be refused, but it completed at …")` (A, `java.lang.AssertionError`): the M-P8 control. Not failing: J7 (both factories equal), H5, J5, J6, the TLS 1.2 tests |
| M-P2 | PIN | `        DefaultTrustedSocketFactory.hardenSocket(sslSocket);` → `        String[] m = sslSocket.getEnabledProtocols(); DefaultTrustedSocketFactory.hardenSocket(sslSocket); sslSocket.setEnabledProtocols(m);` | CST-C, JDK-C | C1, C2 — `assertProtocolFloor` "TLSv1 must not be enabled" (A); the 4 pinned refusal tests [R] — `fail(…)` (A); J7 — "direct socket for host \"imap.right.test\": enabled protocols in order", `[TLSv1.3, TLSv1.2]` against `[TLSv1.2, TLSv1.3]` (A). Not failing: J3, J4 (sets equal on SunJSSE, section 1.2) |
| M-P3 | PIN | `        DefaultTrustedSocketFactory.hardenSocket(sslSocket);` → `        DefaultTrustedSocketFactory.hardenSocket(sslSocket); { java.util.List<String> m = new java.util.ArrayList<>(java.util.Arrays.asList(sslSocket.getSupportedProtocols())); m.remove("SSLv3"); sslSocket.setEnabledProtocols(m.toArray(new String[0])); }` | JDK-C | J3 — AC-4 (d) protocols `containsExactly` (A); J4 — "pinned direct socket" `containsExactly` (A); J7 — "enabled protocols as a set", DNS host (A) |
| M-P4 | PIN | `        DefaultTrustedSocketFactory.hardenSocket(sslSocket);` → `        String[] m = sslSocket.getEnabledCipherSuites(); DefaultTrustedSocketFactory.hardenSocket(sslSocket); sslSocket.setEnabledCipherSuites(m);` | CST-C, HARD-C, JDK-C | C1, C2 — "enabled cipher suites in order, against the default factory" (A); H5 — "(ii) the known suites must appear in the literal order" (A); J3 — AC-4 (d) cipher suites in order (A); J7 — "enabled cipher suites in order", DNS host (A) |
| M-P5 | PIN | `        DefaultTrustedSocketFactory.setSniHost(socketFactory, sslSocket, host);` → `        if (false) DefaultTrustedSocketFactory.setSniHost(socketFactory, sslSocket, host);` | JDK-C | J3 — "server names on the pinned implicit socket" `isNotNull` (A); J7 — "server names, direct socket for host \"imap.right.test\"" (A). Not failing: J5, J6 (no step, no name, no exception) |
| M-P6 (i) | PIN | `        DefaultTrustedSocketFactory.hardenSocket(sslSocket);` → `        DefaultTrustedSocketFactory.hardenSocket(sslSocket); { java.util.List<String> m = new java.util.ArrayList<>(java.util.Arrays.asList(sslSocket.getEnabledCipherSuites())); java.util.Collections.swap(m, m.indexOf("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"), m.indexOf("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384")); sslSocket.setEnabledCipherSuites(m.toArray(new String[0])); }` (cipher order drifts in the pinned factory only) | CST-C, HARD-C, JDK-C | C1, C2 — cipher suites in order (A); H5 — (ii) literal order (A); J3 — AC-4 (d) cipher suites in order (A); J7 — cipher suites in order (A) |
| M-P6 (ii) | DTSF | `        hardenSocket(sslSocket);` → `        hardenSocket(sslSocket); sslSocket.setEnabledProtocols(reorder(sslSocket.getSupportedProtocols(), ORDERED_KNOWN_PROTOCOLS, BLACKLISTED_PROTOCOLS));` (the floor removed from the default factory only) | CST-C, HARD-C, JDK-C, HSK-C | C1, C2 — "enabled protocols as a set" in `assertSameHandling` (A; the pinned floor itself passes); H1 — set equality (A); H2 — "TLSv1 must not be enabled", direct (A); J1 — set equality (A); J2 — "TLSv1 must not be enabled", direct (A); J7 — "enabled protocols as a set", DNS host (A); the 4 Handshake refusal tests [R] — `fail(…)` (A). Not failing: H3, H4 (they call the step directly), the layered-equals-implicit test, J3, J4 |
| M-P6 (iii) | DTSF | `        setSniHost(socketFactory, sslSocket, host);` → `        if (false) setSniHost(socketFactory, sslSocket, host);` (the server-name step removed from the default factory only) | JDK-C | `createSocket_dnsHost_setsServerNameOnRealSocket` — `assertThat(names).isNotNull()` (A); J7 — server names, DNS host (A) |
| M-P7 | PIN | the two lines `        DefaultTrustedSocketFactory.hardenSocket(sslSocket);` and `        DefaultTrustedSocketFactory.setSniHost(socketFactory, sslSocket, host);` (one `old_string`) → the same two lines each prefixed `if (socket == null) ` | CST-C, JDK-C | C2 — `assertProtocolFloor` "TLSv1 must not be enabled", layered (A); the 2 pinned STARTTLS refusal tests [R] — `fail(…)` (A); J7 — "layered socket for host \"imap.right.test\": enabled protocols in order", `[TLSv1.3, TLSv1.2]` (A; the set check passes, section 1.2). Not failing (B-1): J4 (sets), J5, J6, C1, the pinned STARTTLS TLS 1.2 test |
| M-F1 | DTSF | `        sock.setEnabledProtocols(protocols != null ? protocols : protocolsToEnable(sock));` → `        if (protocols != null) sock.setEnabledProtocols(protocols);` | HARD-C | H4 — `verify(socket).setEnabledProtocols(…)` (A, `org.mockito.exceptions.verification.WantedButNotInvoked`) |
| M-D11r | DTSF | `        String[] ordered = reorder(socket.getSupportedProtocols(), ORDERED_KNOWN_PROTOCOLS,` → `        String[] ordered = reorder(socket.getEnabledProtocols(), ORDERED_KNOWN_PROTOCOLS,` (U-063's M-D11 on the new code) | HARD-C, JDK-C | H3 only — case (iii), result `[TLSv1.3]` (A). The log records that no socket-level test detects it (section 1.2: same list on SunJSSE; Conscrypt's lists are equal) (AC-7 last bullet) |
| M-S5 | DTSF | the `try { … } catch (IllegalArgumentException e) { … }` block of `setSniViaSSLParameters` (9 lines, copied from the Read output; they contain em dashes) → its two body lines `        params.setServerNames(Collections.singletonList(new SNIHostName(hostname)));` and `        socket.setSSLParameters(params);` (U-064's M-N2) | JDK-C, SNI-C | J6 — "the pinned createSocket must not throw …; escaped" lists all three names, direct and layered (A); J7 — `pinnedSocket("bad_host.example")` throws (ERROR, `java.lang.IllegalArgumentException`); SNI `setSniViaSSLParameters_nameRejectedBySniHostName_doesNotThrowAndSkipsSNI` — escaped list (A); SNI `createSocket_nameRejectedBySniHostName_doesNotThrow` (ERROR, `java.lang.IllegalArgumentException`, as U-064's M-N2). Not failing: J5, J3, the other 12 SNI tests |
| M-E4 | PIN | `            if (!Arrays.equals(presentedFingerprint, enrolledFingerprint)) {` → `            if (Arrays.equals(presentedFingerprint, enrolledFingerprint)) {` | CST-C, EXP-C | `handshake_enrolledCertificateForDifferentHostName_isAccepted` (ERROR, `javax.net.ssl.SSLHandshakeException`, as U-063's M-E4); `handshake_certificateOtherThanEnrolled_isRejected` — `fail("the handshake must be rejected, but it completed")` (A); the 2 pinned TLS 1.2 tests (ERROR, `javax.net.ssl.SSLHandshakeException`); the Expiry test — direct cause `isInstanceOf(CertificateExpiredException)` (A). Not failing: C1, C2, the refusal tests (they stop at the protocol, before any certificate) |
| M-E5 | PIN | `        TrustManager[] trustManagers = new TrustManager[]{ new PinnedX509TrustManager() };` → an accept-all: `        TrustManager[] trustManagers = new TrustManager[]{ new javax.net.ssl.X509TrustManager() { public void checkClientTrusted(X509Certificate[] c, String a) { } public void checkServerTrusted(X509Certificate[] c, String a) { } public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; } } };` (as U-063's M-E5) | CST-C, EXP-C | `handshake_certificateOtherThanEnrolled_isRejected` — `fail(…)` (A); the Expiry test — `fail("the handshake must be rejected, but it completed")` (A) |

Order: M-P1/M-P8, M-P2, M-P3, M-P4, M-P5, M-P6 (i), (ii), (iii), M-P7, M-F1, M-D11r, M-S5, M-E4, M-E5 (14
runs). M-P1 and M-P8 are one entry: under the shared design the floor is one step, so removing it removes it
from both factories; the entry lists both ids and both sets of tests. The one-factory forms of "floor
removed" are M-P2 (pinned; its refusal tests fail) and M-P6 (ii) (default; its refusal tests fail). Every
edit compiles as written: no `try`/`catch` is stranded; `if (false)` is the conditional-compilation form;
`m` is a new local in a new block or statement; `reorder` and the constants are visible inside the class.
At the end, add a summary table (id, criterion, failing / of, verdict) under the header.

### I8 — Determinism

Pre-check as I7 (a). Two runs, each its own foreground call:
`rm -rf $R; ./gradlew -I "$INIT" --console=plain :app:testDebugUnitTest <ALL7 --tests> --rerun > $WORK/logs/I8-run<n>.log 2>&1; echo "exit=$?" >> $WORK/logs/I8-run<n>.log`,
cases into `$E/I8-run<n>-cases.txt`. Then `$E/I8-checks.txt`:
`diff $E/I6-green-cases.txt $E/I8-run2-cases.txt; echo "diff-2-exit=$?"`, the same for run 3, and the grep
`git grep -nE 'Thread\.sleep|getByName|getAllByName|"localhost"|"127\.0\.0\.1"|new InetSocketAddress\("' -- $HARD $JDKT $HSK $CST $LTS; echo "grep-exit=$?"`
(the existing `LoopbackTlsTestServer.connect` line uses the loopback address object, not a string, so the
pattern does not match it). Expected: both diff exits 0, `grep-exit=1`. **Stop:** row D31.

### I9 — Final gate, counts and file checks

1. Pre-check as I7 (a); every mutation entry has its `restored` line.
2. Gate, same command as I0 step 2, into `$E/I9-gates.txt` and `$E/I9-junit-totals.txt`. Expected:
   `exit=0`, `tests=732 failures=0 errors=0 skipped=2` (732 or as reduced).
3. `$E/I9-checks.txt`:
   ```bash
   git grep -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"     # 732 (working tree)
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"   # 713 until the commit
   git status --porcelain -- app k9mail-vendored
   git diff --name-only HEAD -- app/src/main k9mail-vendored                  # the two production files
   git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin    # equals PROD_DIFF
   git diff --stat HEAD -- build.gradle app/build.gradle k9mail-vendored/build.gradle settings.gradle gradle   # empty
   git diff --quiet HEAD -- gradle/verification-metadata.xml; echo "vm-exit=$?"                         # 0
   git diff --stat HEAD -- app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java $TD/ssl/TlsTestFixtures.java   # empty
   git grep -n "new PinnedCertificateSocketFactory\|new DefaultTrustedSocketFactory" -- app/src/main k9mail-vendored/src/main
   git grep -n "@Ignore" -- $TD/ssl $TD/Pinned*.java; echo "ignore-exit=$?"                              # 1
   ls -l app/build/outputs/apk/debug/app-debug.apk | awk '{print $5, $6, $7, $8, $9}'                  # size, date, path only
   sha256sum app/build/outputs/apk/debug/app-debug.apk
   ```
   (The `ls` line prints no owner column.) The construction sites must be the five of L5.

### I10 — Implementation log and hand-off

Per step: clock time, command summary (repository-relative; `$WORK` named only as "the work folder"),
the result line from the evidence file, and any decision row used. Close with: counts by reference
("see AC-10; Gradle totals in `evidence/I9-junit-totals.txt`, working-tree count in `evidence/I9-checks.txt`";
the numbers are not restated), the tests added by name and the 7 renames (old → new), the mutation log
location and verdicts, the probe outcome per row, each declared deviation (I7), and the text for the story's
Implementation Notes: AC-1 (d) and (e) as in section 2.1, including the CWE-636 fix for protocols, the
ciphers deliberately left at platform defaults in that branch, and the new case where `createSocket` itself
may throw an unchecked exception in that branch (fail closed; with the N2 probe result); the
`ALLOWED_PROTOCOLS` trade (an unknown protocol name, including a future TLS version, is never enabled; a
later version needs one entry in that list; story Scope Concern 7, for the product owner); AC-4 (b) and (c)
as in section 1.1 with the I2 keys; AC-6 per row of table 3.1, including that the `SSLv2Hello` clause of
both A1-G2 guards can no longer fire, the DHE clause alone carries them, and the guard code is unchanged and
passes (AC-6 (d), (e)); AC-8 conditions, with the D3/D4/D6-a/D6-b outcome and its reporting per version;
AC-11 (a) to (d) as in section 7. The hand-off names the debug APK
`app/build/outputs/apk/debug/app-debug.apk` with its size and SHA-256 from `I9-checks.txt`, built by the I9
gate from the final tree. A "Runtime Claims" line lists only what evidence files show. Last line: "Hygiene
scan: step I11, result in `evidence/I11-scan.txt`". The developer does not edit the story.

**Items for the orchestrator** (copied into the hand-off; the developer edits none of these records; the
orchestrator takes them to the product owner as it decides):
1. BUG-020 AC-5 and the missing record (contract-verification O1, O7): one question with two options:
   (a) raise one change record against CNTR-MODERNIZATION-001, a sibling of CR-013 applied in the same
   combined revision, stating for both producers that no protocol older than TLS 1.2 is enabled, TLS 1.2
   and 1.3 are enabled where the platform supports them, a protocol name not in the allowed list is never
   enabled, the cipher suites are the platform's enabled suites minus the blocked list with the known suites
   first in the recorded order, and that the pinned producer applies exactly the default producer's handling;
   with a validation rule and a Versioning line saying whether such a change is a contract revision (the
   contract's backward-compatible list does not name it today; the change is backward-compatible only by
   elimination); or (b) a dated note to Record 9 (or a new record) giving the reason not to state them, which
   meets AC-5's "or the record says why not", with CR-013 `:332` reworded at the revision. Whether BUG-020
   closes with AC-5 open stays the pipeline's and the product owner's call.
2. CR-013, second pass with alternative (b) (contract-verification O2, O3), suggested dated note: "extend
   rule 6 to both producers; replace the Edit 4 pinned clause ('not yet stated'); name the U-065 pinned tests
   (`pinnedFactory_createSocket_implicit_setsDnsHostAsOnlyServerName_jdkProvider`,
   `pinnedFactory_createSocket_ipLiteralAndBlankHosts_setNoServerName_jdkProvider`,
   `pinnedFactory_createSocket_nameRejectedBySniHostName_doesNotThrowAndSetsNoServerName_jdkProvider`,
   `pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider`); alternative (a) no longer
   describes the code (U-065)", and for the pinned subsection: "For the socket layered over a connected
   socket, the statements rest on reading the code: the test providers set a server name on that socket
   themselves, so no test can tell the factory's step from the provider's (CR-013 Scope Concern D; U-065
   probe)."
3. CR-009 (contract-verification O4; story Scope Concern 6), suggested additions: checklist line
   "`PinnedCertificateSocketFactory.createSocket` calls `DefaultTrustedSocketFactory.hardenSocket` and
   `setSniHost` (U-065, commit `<fix>`); retarget both calls and the import to the app-owned default factory
   in the same change; the shared step may become package-private once both classes share a package"; test
   row T11 "Pinned/default parity: protocols (set and order), cipher suites (order) and server names of the
   two factories' sockets are equal per shape and provider (U-065
   `createSocket_*_appliesDefaultFactoryProtocolAndCipherHandling`,
   `pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider`); exist; retarget"; T5 reworded
   "`hardenSocket`, `protocolsToEnable`, `reorder`: blocked and ordered cipher suites; protocols limited to
   `ALLOWED_PROTOCOLS` from the supported list; exist (U-063, U-065)"; and rows `:87` and `:234` ("producer 2
   unchanged") no longer describe the code.
4. The error table (contract-verification O6): a server limited to an old protocol makes the handshake fail
   with a non-certificate `SSLException`, which per CR-012 Scope Concern B (`:305`) leaves
   `ImapConnection.open()` as an `IOException`; a candidate row for the record of item 1 or the combined
   revision.
5. The AC-8 (b) reading, if row D6 was reached (it is taken before I3).

### I11 — Evidence-hygiene scan (last step, after every file is final)

As U-064's I8, over: every file under `$W` except `compiled-pipeline.json` (excluded locally, L16), and the
code diff `git diff HEAD -- app/src k9mail-vendored > $WORK/logs/I11-code.diff`. One call:
```bash
P='@[a-z0-9-]+\.[a-z]{2,}|Users[/\\]|App''Data'
L=$WORK/logs
git diff HEAD -- app/src k9mail-vendored > $L/I11-code.diff; echo "code-diff-exit=$?"
{
  grep -rlEi --exclude=compiled-pipeline.json "$P" $W $L/I11-code.diff > $L/I11-pattern-files.log
  echo "pattern-scan exit=$? files=$(wc -l < $L/I11-pattern-files.log)"
  for label in user-name host-name; do
    case $label in user-name) v="${USERNAME:-$USER}" ;; host-name) v="${COMPUTERNAME:-$(hostname)}" ;; esac
    if [ ${#v} -lt 3 ]; then echo "$label-scan value-unavailable"; continue; fi
    grep -rliF --exclude=compiled-pipeline.json -e "$v" $W $L/I11-code.diff > $L/I11-$label-files.log
    echo "$label-scan exit=$? files=$(wc -l < $L/I11-$label-files.log)"
  done
  S=sdlc/artifacts/build/sprints/sprint-021/U-065-tls12-floor-both-factories-and-pinned-factory-parity.md
  echo "story-pattern-count=$(grep -cEi "$P" $S)"
} > $L/I11-scan.log 2>&1
cat $L/I11-pattern-files.log $L/I11-user-name-files.log $L/I11-host-name-files.log
```
Expected: every scan `exit=1 files=0`; `story-pattern-count=0`. Then sanitise `$L/I11-scan.log` into
`$E/I11-scan.txt` and run the count-only checks of that file in the terminal only (pattern 0; each name 0).
Exit codes: 1 clean, 0 hit, 2 error. **Stop:** rows D35, D36.

## 5. Decision Table

| Row | Observation | What the developer does |
|-----|-------------|-------------------------|
| D1 | I0: HEAD differs, a production/test/build path is dirty, a hash differs, count is not 713, toolchain not 25, a Record commit missing, or the gate fails | Stop and report with the sanitised evidence (nothing has been edited). A later HEAD that changes nothing under `app`, `k9mail-vendored` or the build files: record it, re-baseline, continue |
| D2 | I2: either `c_hs_TLSv1.2_*` is not `completed protocol=TLSv1.2 byte=42 server=OK` | Compare `$LTS` with section 3.7; if a typing error is found, fix it and repeat I2 once (logged). Otherwise stop and report: AC-8 (a) cannot be shown |
| D3 | I2, per version V in {TLSv1.1, TLSv1}: `c_hs_V_client_provider_default=completed protocol=V …server=OK` and `c_hs_V_client_floor=refused type=<SSLException subclass> server=FAILED:…` | Continue: write the four refusal tests for V; M-P1/M-P8, M-P2, M-P6 (ii), M-P7 expect them as listed |
| D4 | I2, per V: `c_hs_V_client_provider_default` is not `completed protocol=V` (the desktop Conscrypt server cannot serve V, or the client did not negotiate it) | Do not write the four refusal tests for V. Record AC-8 (b) for V as "not demonstrable under Conscrypt" with the probe line as evidence; AC-1 and AC-2 stand alone for "refused" (story AC-8 condition). Reduce every expected count by 4. Continue |
| D5 | I2, per V: `c_hs_V_client_floor=completed …` (a client limited to TLS 1.2 and 1.3 completed a handshake with a V-only server) | Stop and report: the floor would not hold at handshake under Conscrypt, a finding against the design |
| D6 | I2, per V: the floor case is refused, but `type=` is not an SSLException subclass (for example `java.net.SocketException`) | **Stop after I2, before I3** (no test is written yet). Report the probe line (the observed class X and the server outcome) to the orchestrator and wait for its reading; record the reading with its date in the implementation log. Then exactly one of: **D6-a** (the orchestrator accepts X for AC-8 (b)): write the four refusal tests for V as in 3.5 and 3.6 and, in both refusal helpers (`assertRefusedByProtocolFloor`, `assertPinnedHandshakeRefused`), replace `} catch (SSLException e) {` with `} catch (java.io.IOException e) {` and add as the first statement of the catch block `assertWithMessage("refusal type for " + protocol + ": " + e.getClass().getName()).that(e instanceof SSLException \|\| e instanceof X).isTrue();` with X the fully qualified class from the probe line (`\|\|` is Java's `||`, escaped for this table); counts and mutation expectations as D3; AC-8 (b) for V is reported "met; refusal raised as X instead of an `SSLException`, accepted by the orchestrator on <date>". **D6-b** (not accepted): proceed as D4 (no tests for V, counts reduced by 4); AC-8 (b) for V is reported "not met as worded: the client refuses, but raises X (probe line), not an `SSLException`", the criterion marked unmet for V, and AC-1/AC-2 still show the floor. If both versions hit D6, one reading covers both |
| D7 | I2, per V: the floor case is refused with an SSLException subclass but `server=OK` | Stop and report (contradictory outcome) |
| D8 | I2: `c_provider` is not `Conscrypt`, or `c_supported`/`c_default_enabled` differ from the expected lines | Stop and report: the environment changed; the preconditions of H2, C1, C2 would fail |
| D9 | I2: `c_empty_protocols` or `j_empty_protocols`, either value | Record only; the production Javadoc covers both; no change |
| D10 | I2: `c_roundtrip_protocols` is not `TLSv1.2,TLSv1.3` or `c_roundtrip_cipher_order_kept` is `false` | Stop and report: the server-name step could undo the floor or order under Conscrypt (AC-4 (d)); the step order needs a design decision |
| D11 | I2: `j_provider` is not `SunJSSE`, or a `j_` line is missing, or the JDK probe class errors (for example `NoClassDefFoundError`) | Repeat I2 once with the JDK probe class alone (logged). Same result: stop and report (sandbox isolation) |
| D12 | I2: `j_layered_0 … names=imap.right.test` | Planned branch of AC-4 (b): layered shape asserted only for "no exception, `SSLSocket` returned"; record the reason in Implementation Notes. Continue |
| D13 | I2: `j_layered_0 … names=none` (the provider adds nothing on the layered socket) | AC-4 (b) first branch applies: in J5 and J6 replace the layered `// created without an exception` body with `assertWithMessage("layered " + quote(host) + " server names").that(serverNames(layered.socket)).isEmpty();` (J6: `name` for `host`); in J3 add after the AC-4 (d) assertions a block `try (Layered layered = new Layered(pinnedFactory(), DNS_HOST)) { assertThat(serverNames(layered.socket)).containsExactly(DNS_HOST); }`; in J7 compare `serverNames` of the two layered sockets too. Record. Continue |
| D14 | I2: the null-host layered line (`j_layered_10`) is `threw …` | AC-4 (c) provider case: remove `null` from J5's layered attempt only (wrap the `Layered` block in `if (host != null)`); keep it on the direct shape; record as provider behaviour. Continue |
| D15 | I2: a `j_layered_1..9` line (other than D14) is `threw …` | Same as D14 for that host. Continue |
| D16 | I2: `j_unconnected_names` is not `none`, or `j_layered_6` is not `names=host.example` | `unconnected` not none: stop and report (J3's precondition and the S2 to S5 observable fail). `layered_6` different: record only (informational) |
| D17 | I3: a hunk outside the allowed ranges, `untouched-exit` not 0, an `@Ignore`, the BUG-019 line missing, or production dirty | Undo that file (`git checkout -- <test file>`, test files only) and redo its edits; production dirty: stop and report |
| D18 | I4: a changed test (H1, H2, J1, J2, J3, C1, C2) passes against the unchanged production code | Stop and report: it does not test the change; never weaken or reword it to fail |
| D19 | I4: a new test not in the "passing by design" list passes, or one in that list fails | Stop and report with the sanitised message |
| D20 | I4: an unchanged test fails, or a compile error | Unchanged test: stop and report. Compile error: compare the test source with section 3 character by character, fix the test source only, repeat I4 once (logged) |
| D21 | I4: matches the expectation | Continue |
| D22 | I5: another file changed, a hunk inside a protected range, or an unexpected `hardenSocket(` caller | Undo the deviation to match section 2.4 exactly; if the plan text itself does not compile, stop and report the compiler message (no improvised production code) |
| D23 | I6: a new or changed test fails | Compare with the plan; a typing error in a test is fixed and I6 repeated once. If test and production match the plan, stop and report (a production defect or a plan error); no production edit beyond section 2.4 |
| D24 | I6: an unchanged test fails (including U-064's three) | Stop and report (AC-6 (d)); no test edit. U-064's seams are unchanged by design, so no retarget is planned |
| D25 | I6 or later: guard A1-G1, A1-G2 or A1-G3 fails | Stop and report: a sandbox was shared; do not change annotations or guards |
| D26 | I6: all pass | Continue |
| D27 | A mutation edit's `old_string` is not found or not unique | Re-read the file; if the file matches section 2.4 and the text still differs, stop and report (plan error) |
| D28 | A mutation does not compile | Restore at once, log `COMPILE_FAIL (invalid, not counted)` with its `restored` line, and apply a compiling replacement as a new entry `<id>r`: replace `if (false) stmt;` forms by deleting the statement, and the inline blocks by an equivalent with explicit types; if none compiles, stop and report |
| D29 | A mutation is not detected (every expected test passes) | Restore, log `NOT DETECTED`; the criterion that named it is unmet: stop and report. Exception declared in advance: a [R] test that D4/D6-b removed is not expected |
| D30 | A mutation run differs from its row: an expected test passes, a test not in the row fails, a timeout, or a failing test's type is not the one the row states (an **A** entry matches any `AssertionError` subclass; a named exception matches that class, and for `SSLHandshakeException` any `SSLException` subclass). A failure that matches the row in test and type is never a mismatch, whatever its class (a compile error is D28) | Restore, log `MISMATCH (not counted)` with the actual set, types and messages; stop and report. A Robolectric class-level error such as `Couldn't create lock file` before any test: repeat the run once with the mutation still applied, keep both summaries, name the repeat as a deviation in the entry |
| D31 | I8: a diff is not empty, or the grep hits | Stop and report (non-determinism or a forbidden construct) |
| D32 | I9: a test passes in the targeted runs but fails in the full gate (sandbox reuse, guards, an ordering effect with `forkEvery`) | Check first that no `U065*Probe` file remains under `app/src/test`; if one does, delete it and repeat the gate once. Otherwise stop and report with the sanitised message; do not add annotations or reorder classes |
| D33 | I0/I9: daemon JVM not 25, or "Could not find or load main class" | Not 25: stop. Main-class error: confirm `MSYS_NO_PATHCONV` is unset and repeat once |
| D34 | I9: a JaCoCo floor is missed, totals differ from the expected count, or skipped is above 2 | Stop and report with the gate output; no floor, limit or build edit |
| D35 | I11: a scan hit (`exit=0`, files above 0) | Read the listed file with the Read tool; remove the text from a hand-written file, or regenerate an evidence file from its raw log through `sanitize.py`; re-run I11 in full. A hit in `plan.md` or a gate file of another agent: report, do not edit |
| D36 | I11: a `grep` exit 2 or `value-unavailable` | Correct the command and re-run I11; an unavailable value is reported as "not checked" |
| D37 | `sanitize.py` refuses a file | Narrow the filter; never edit raw logs or evidence by hand |
| D38 | "bash.exe … not found", a process killed without a visible reason, or a session lost | Stop and report (endpoint security). If a mutation entry lacks `restored`, the next turn first restores from the pristine copy and checks the hashes |
| D39 | Anything else not foreseen here, outside a probe that met its expected line | Restore any applied mutation, then stop and report |

## 6. Criterion-to-Step Map

| Criterion | Steps | Shown by | Evidence |
|-----------|-------|----------|----------|
| AC-1 (a)-(c) | I3, I4, I6, I7 | H1, H2 (Conscrypt, both shapes, precondition), J1, J2 (JDK, both shapes, A1-P2); M-P1/M-P8, M-P6 (ii) | `I4-red-cases.txt`, `I6-green-cases.txt`, `mutation-log.md` |
| AC-1 (d) | I3, I7 | H3 (injected lists, case (iii)); M-D11r detected by H3 only | `mutations/M-D11r-*.txt` |
| AC-1 (e) | I3, I7 | H4; M-F1; Implementation Notes (section 2.1) | `mutations/M-F1-*.txt` |
| AC-2 | I3, I6, I7 | C1, C2 (Conscrypt, preconditions), J4 (JDK exact set); M-P2, M-P3, M-P7 | as above |
| AC-3 | I3, I6, I7 | (a) C1, C2, J7; (b) H5; (c) existing `reorder_removesEveryBlockedItem`, `cipherConstants_matchRecordedBaseline` + M-P4 | `mutations/M-P4-*.txt` |
| AC-4 | I2, I3, I6, I7 | S1/S6 J3 (+ (d)), S2-S4 J5, S5 J6; (b), (c) probe keys `j_layered_*`; M-P5, M-S5 | `I2-probe.txt`, `mutations/M-P5-*`, `M-S5-*` |
| AC-5 | I3, I7 | C1, C2, J7; M-P6 (i), (ii), (iii) | `mutations/M-P6*` |
| AC-6 | I3, I4, I6, I10 | table 3.1; diff checks; `@Ignore` grep; BUG-019 grep | `I3-test-diff-check.txt`, implementation log |
| AC-7 | I7 | 14 entries, each with write-ahead, failing names, `restored` | `mutation-log.md`, `mutations/` |
| AC-8 | I1, I2, I3, I7 | TLS 1.2 tests (4); refusal tests (8 [R]); control M-P1/M-P8 (and M-P2, M-P6 (ii)) | `I2-probe.txt`, `mutation-log.md` |
| AC-9 | I5, I9 | name list, protected-range hunks, unchanged constructors and `K9MailTransport.java`, unchanged trust tests, empty build-file diff | `I5-production.txt`, `I9-checks.txt` |
| AC-10 | I0, I9 | four-task gate before and after; counts 713 → 732 | `I0-*`, `I9-*` |
| AC-11 | I0, I10 | Record commits and ancestry; section 7 table | `I0-baseline.txt`, implementation log |
| IC-1 | orchestrator | section 9 | `IC1-emulator.txt` (orchestrator) |
| IC-2 | I5, I9 | full gate compiles every caller; construction-site grep; the only visibility change has two callers | `I5-production.txt`, `I9-checks.txt` |

## 7. Bug Criteria (AC-11 (c)) and Records (AC-11 (a), (b), (d))

| Bug criterion | Satisfied by | Status |
|---------------|--------------|--------|
| BUG-019 AC-1 | AC-1: H2, J2 (both providers, both shapes, preconditions) | Satisfied here |
| BUG-019 AC-2 | AC-1 (c), AC-6 (a), (b): H2 and J2 changed, not deleted or ignored, BUG-019 comment kept and reworded; fails under M-P1/M-P8 | Satisfied here |
| BUG-019 AC-3 | AC-1 (a), (b), AC-9 (d), AC-10: TLS 1.2/1.3 iff supported, SSLv3 off, cipher lists and server-name code untouched, gates | Satisfied here |
| BUG-019 AC-4 | AC-6, table 3.1 | Satisfied here |
| BUG-019 AC-5 | Record 8 (`6cc49ab4`, ancestor of the fix) | Exists; recorded |
| BUG-020 AC-1 | Records 8 and 9 (`bd1d5398`), `SSLv2Hello` dropped by Record 8 | Exists; recorded |
| BUG-020 AC-2 | AC-2, AC-3 (Conscrypt), AC-4 (JDK) | Satisfied here |
| BUG-020 AC-3 | C1, C2, J3 changed; not satisfied by provider defaults (preconditions); M-P2 (protocols), M-P4 (ciphers, run on its own), M-P5 (server name) | Satisfied here |
| BUG-020 AC-4 | none | Does not apply |
| BUG-020 AC-5 | none | NOT satisfied here: waits for the combined contract revision, CR-013 alternative (b), and the record recommended in Scope Concern 1 |
| BUG-020 AC-6 | AC-20, AC-21 and the 8 trust tests unchanged; M-E4, M-E5 re-run | Satisfied here |
| BUG-020 AC-7 | AC-10 | Satisfied here |
| BUG-020 IC-1 | AC-9 (b), IC-2 | Satisfied here |

AC-11 (a): decided by the product owner on 2026-11-20: protocols (Record 8: nothing older than TLS 1.2 for
both factories, including `SSLv2Hello`; accepted cost: a server offering nothing newer than TLS 1.1 can no
longer be used), cipher suites and server name for the pinned factory (Record 9: "Same as default"); both
records are in commits that are ancestors of the fix commit (I0 shows the `git log -S` lines). (b)
`SSLv2Hello` does not stay. (d) No edit to BUG-019, BUG-020, the contract or any change record; status
changes are the pipeline's.

## 8. Contracts Verified, Scope Concerns, Risks, Out of Scope

### 8.1 CNTR-MODERNIZATION-001 (approved; read, not edited)

- What it says about each producer today: the producer table (`:93-96`) and Invariant 2 (`:173-176`)
  state trust behaviour only: system chain validation for `DefaultTrustedSocketFactory`; SHA-256
  fingerprint equality plus validity window for `PinnedCertificateSocketFactory`; never trust-all. Versioning
  (`:279-291`) lists "tightening `PinnedCertificateSocketFactory` validation" as backward-compatible.
- This story changes behaviour the contract does not state (enabled protocols, cipher suites, server name
  of both producers) and nothing it does state: trust stays as written (`:77-80` and the trust manager
  unchanged; AC-20, AC-21, the Expiry test and the 8 trust tests pass unchanged; M-E4, M-E5 re-run). No new
  boundary, no type or signature change; `integration_contracts: []` is correct. Contract gate: no block.
  In the contract's Versioning terms the change is backward-compatible only by elimination: it hits none
  of the four listed breaking changes (no type, name, invariant or consumer duty changes), but the
  backward-compatible list (`:288-291`) names no protocol, cipher or server-name change for either
  producer; a Versioning line for it belongs to the record of hand-off item 1 (I10).
- CR-013 (approved, held): statements S1 to S6 (`:311-318`) describe the default factory; its server-name
  code is untouched, so they stay true. After this story the pinned factory satisfies them on the direct
  shape, shown by J3 (S1, S6), J5 (S2 to S4), J6 (S5), J7 (S6 by parity). Alternative (b) (`:402-411`;
  the alternatives start at `:389`)
  becomes applicable because its code and tests now exist; it is still applied only in the combined
  revision. Its "for both sockets" holds for the layered shape by reading the code (both factories call
  the same `setSniHost` on both shapes); no test observes it, because the JDK provider sets the layered
  name itself (I2). That should be stated when (b) is applied (Scope Concern 9). "On API level 24 and
  later" matches the tested path (`sdk=29`).
- CR-012 (approved, held): unaffected; rejections still surface at the handshake.
- CR-009 (approved, not applied): rows T5 (`:214`) and the evidence row `:85` become stale (a floor now
  applies; two factories share the step; the vendored lines move); the move's baseline is the tests as this
  story leaves them.

### 8.2 The story's Scope Concerns

| # | Answer |
|---|--------|
| 1 | Left for the product owner: a change record for the protocol floor and cipher handling in the contract is recommended by the story; this plan raises none (no CR is raised by this story); the two options are I10 item 1 |
| 2 | Left for the product owner and the pipeline: BUG-020 closes with AC-5 open; section 7 shows it |
| 3 | Dealt with: the literal reading is implemented and tested (H3 case (iii)); no provider observed has the case |
| 4 | Dealt with: the null branch applies the floor from the socket; tested by H4 and M-F1; ciphers stay platform defaults there (unchanged; parity holds). Making that branch fail the connection would be a further decision for the product owner |
| 5 | Dealt with: H3 and M-D11r restore the detection |
| 6 | Left for the orchestrator: CR-009 additions (checklist line, test row T11, T5 rewording), text in I10 "Items for the orchestrator", item 3 |
| 7 | Dealt with and recorded: allow-list; unknown names never enabled; a future TLS version needs one entry in `ALLOWED_PROTOCOLS` (a trade the owner may want to know) |
| 8 | By reference: the U-065 security review examines the result; its five first checks are mapped in 8.3 |
| 9 | Dealt with by evidence: I2 shows the provider sets the layered name itself; the layered statement rests on reading the code |
| 10 | Kept as written (IC-1, section 9) |

### 8.3 Points handed over from U-064

| Point | How the plan deals with it |
|-------|----------------------------|
| U-064's AC-2 seams (public static `setSniViaSSLParameters`, package-private guard by reflection, "never calls `setSSLParameters`") | Not moved, renamed or widened; `setSniViaSSLParameters` unchanged (2.3); all U-064 tests in I6 and I9 |
| The pinned safety net needs its own call-through test and mutation | J6 plus M-S5 |
| Expiry test needs a fresh context per call and TLS 1.3 still negotiated | `:77-80` byte-identical; TLS 1.3 in `ALLOWED_PROTOCOLS`; Expiry test in I6, I9, M-E4, M-E5 |
| JUnit XML off in the build | Every run uses `-I "$INIT"` and `rm -rf $R` first |
| Security review first checks: (1) pinned `createSocket` still installs `PinnedX509TrustManager`, nothing from hardening touches trust | M-E4 and M-E5 fail AC-21 and the Expiry test; P4 leaves `:77-80` unchanged; hardening reads only the socket |
| (2) the SNI host does not change what is verified | `handshake_enrolledCertificateForDifferentHostName_isAccepted` unchanged (a name is now set and the other-host certificate is still accepted by the pin); no verifier added |
| (3) the floor keeps the loopback server's protocol, and a 1.2 floor is enforced by a test with a mutation | TLS 1.3 and 1.2 handshakes pass; refusal tests with M-P1/M-P8, M-P2, M-P6 (ii) |
| (4) the pinned rejected-name test fails when its catch is removed | J6 under M-S5 |
| (5) hygiene, hash checks, no `@Ignore` added | I11; I7 hash protocol; `@Ignore` greps in I3 and I9 |

### 8.4 Risks

| # | Risk | Likelihood / impact | Mitigation |
|---|------|---------------------|------------|
| R1 | Desktop Conscrypt cannot serve TLS 1.1 or 1.0 | Low / AC-8 (b) not demonstrable | Probe I2, rows D3 to D7 |
| R2 | The server-name round trip undoes the floor or order | Very low (L12, default factory already does it) / high | Probe N3, J3 and C1/C2 assert after the step; row D10 |
| R3 | A mutation left in place while the production change is uncommitted | Low / high | Pristine copies, I5 blobs and diff hash, write-ahead, recovery note |
| R4 | The public `hardenSocket` is misused later | Low / low (it can only narrow a caller's own socket) | Javadoc states its scope; IC-2 grep |
| R5 | The pinned factory keeps calling the vendored step after a CR-009 move that copies the class and keeps the vendored one | Low (CR-009's zero-references check catches P1's import if it is run) / medium | That check; parity tests; CR-009 checklist line (I10 item 3) |
| R6 | Sandbox reuse or ordering in the full run | Low / medium | Guards A1-G1 to A1-G3, A1-G2 via DHE; row D32 |
| R7 | A future TLS version is not enabled without a code change | Certain in the long term / low | Recorded trade (Scope Concern 7) |
| R8 | A platform supporting nothing at TLS 1.2 or above gets an empty list | Not reachable at minSdk 21 / connection fails closed | Javadoc; probe N2 |
| R9 | Layered server names not observable | Certain (I2) / low | AC-4 (b) branch; reading of the code stated |
| R10 | Unintended edits to unchanged tests in five heavily edited classes | Medium / medium | Allowed hunk ranges, I3 diff check, row D17 |
| R11 | Endpoint security kills the shell mid-run | Low / medium | Foreground only, row D38, recovery note |
| R12 | Evidence leaks a machine path or name (logcat in IC-1 may contain the account) | Low / medium | `sanitize.py`, I11, IC-1 records filtered lines only |

### 8.5 Out of scope

The contract and every change record (no edit, no new record); CR-009's move; trust and host-name checks;
any user interface; `TlsTestFixtures.java`, `K9MailTransport.java`, the SNI, Expiry and
`PinnedCertificateSocketFactoryTest` classes (unchanged); `WebDavSocketFactory` (calls `setSniHost`, applies
no hardening of its own; not used by this app's code path and not in either bug); a not-yet-valid pinned
certificate; any build or dependency file; the bugs' status.

## 9. IC-1 — Emulator Check (for the orchestrator or QA; the developer does not run it)

Preconditions: the developer's hand-off names `app/build/outputs/apk/debug/app-debug.apk` with size and
SHA-256 from `evidence/I9-checks.txt`, built by the I9 gate from the final tree, and no mutation entry lacks
`restored`. Run in the foreground; `MSYS_NO_PATHCONV=1` only for an adb command with an absolute device path
(none below needs it); never for Gradle. Raw output goes to the orchestrator's work folder outside the
repository.

1. `adb devices -l` first; it addresses no device. If it lists no device: ask the owner to start the
   `Pixel_9_Pro` emulator (never start one, never start or touch `Pixel_10_Pro`). If it lists more than one
   device (emulator or physical): stop and ask the owner which serial is the `Pixel_9_Pro`, and send no
   command to any device until the owner answers; never query the others. If it lists exactly one emulator:
   `adb -s <serial> emu avd name` (first line). If that is not `Pixel_9_Pro`: stop, send nothing more, and ask
   the owner. Every later command uses only that serial with `-s`.
2. `adb -s <serial> shell getprop ro.build.version.sdk` → `36`.
   `adb -s <serial> shell dumpsys package com.zegoggles.smssync | grep -E "versionCode|lastUpdateTime"` (before).
3. `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk` → `Success`. Any failure (for
   example `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, a signing mismatch, or a version downgrade): stop, report
   "not run"; never uninstall, never `-d`, never clear data. Repeat the `dumpsys` line (after).
4. `T=$(adb -s <serial> shell "date '+%m-%d %H:%M:%S.000'" | tr -d '\r')` (the whole command in one
   quoted argument, so the device shell passes the format to `date` as one word; `tr` drops the carriage
   return); check that `T` is not empty before step 5. Start a backup: launch with
   `adb -s <serial> shell monkey -p com.zegoggles.smssync -c android.intent.category.LAUNCHER 1`, then the
   owner taps the backup action (or the orchestrator taps it after a screenshot,
   `adb -s <serial> exec-out screencap -p > <work>/ic1-screen.png`, with `adb -s <serial> shell input tap <x> <y>`).
   If the app asks for the account password or a sign-in: stop, report "not run" (only the owner handles it).
   No system setting is changed.
5. Wait in the foreground (tool timeout 600000):
   `adb -s <serial> logcat -T "$T" -m 1 -e 'BackupWorker: (backup complete, backedUp=|unrecoverable error)'`;
   then `adb -s <serial> logcat -d -v threadtime -T "$T" > <work>/ic1-logcat.txt` and
   `adb -s <serial> shell pidof com.zegoggles.smssync`.
6. Search `<work>/ic1-logcat.txt`: `BackupWorker: starting backup (` (read N), `BackupWorker: backup complete, backedUp=`,
   `FATAL EXCEPTION` (count only those followed by `Process: com.zegoggles.smssync`), `SSLHandshakeException`,
   `SSLProtocolException`, `BackupWorker: unrecoverable error` (count only lines with the app's pid or tag
   `SMSBackup+`).
7. Verdict: pass only with the start line (N ≥ 1), the completion line, and zero of the four error kinds from
   the app's process. N = 0 or no start line: "not conclusive: nothing to back up" (BackupWorker opens no
   connection then, `:252-255`); the owner decides; nobody invents data or changes a setting.
8. Record `sdlc/artifacts/build/sprints/sprint-021/U-065/evidence/IC1-emulator.txt` through `sanitize.py`
   (it refuses e-mail-like text): clock time, AVD name, SDK level, install result, versionCode before and after,
   the matching start and completion lines, the four error counts, the APK SHA-256 compared with the hand-off,
   the verdict. Never copy raw logcat into the repository.

What it proves and does not prove: as the story's IC-1 states (the default factory still connects to Gmail on
Android 16; nothing about the removed versions, Android 11-era devices, the pinned factory or the owner's
devices).

## 10. Constraints for the Developer (binding)

1. Gradle in the foreground with the tool timeout set to 600000 ms on every call; never with
   `MSYS_NO_PATHCONV=1` set.
2. If the tool moves a run to the background by itself, wait for its completion notice; never poll; start no
   other command meanwhile.
3. No background or detached shells, no `&`, no `sleep` or polling loops, no helper agents for shell work, no
   heredocs for editing (Write and Edit tools only).
4. On "bash.exe … not found" or a process killed without a visible reason: stop and report (endpoint
   security); do not work around it.
5. Raw logs and the pristine copies stay in the work folder outside the repository; only filtered evidence
   (section 4.0 filters) enters `$W`.
6. Timestamps from `date -u +%Y-%m-%dT%H:%M:%SZ`, never typed.
7. Commit nothing; the orchestrator commits with the product owner's permission.
8. Production edits only as section 2.4, plus the logged mutations, each restored and proven by blob hash and
   diff hash.
9. No dependency, Gradle, build or verification-metadata change; no edit to `TlsTestFixtures.java`, the SNI,
   Expiry or `PinnedCertificateSocketFactoryTest` classes; no `@Ignore`.
10. The probe classes exist only during I2 and are deleted before I3; no other new file under `app/`.

## 11. Quality Attribute Coverage (ISO/IEC 25010)

| Attribute | How this story serves it |
|-----------|--------------------------|
| Functional suitability | Records 8 and 9 implemented and pinned by tests on both providers and shapes; each aspect proven by a mutation |
| Performance efficiency | One static computation as before; the pinned path adds two cheap calls per socket and triggers the default class's static initialiser once. Tests: loopback only, every wait bounded at 10 s |
| Compatibility | Interoperability: TLS 1.2 handshake shown; Gmail accepts TLS 1.2/1.3 (BUG-019 measurements); accepted cost for TLS 1.1-only servers. Co-existence: the JDK-provider sandbox guards |
| Usability | Not applicable (no UI); indirectly, a malformed host on the pinned path does not crash socket creation (J6) |
| Security (confidentiality, integrity, authenticity) | One protocol minimum for the password-carrying connection on both trust paths; allow-list fails safe for unknown names; trust path untouched (M-E4, M-E5) |
| Reliability | The fail-open branch now applies the floor; the SNI safety net covered on the pinned path |
| Maintainability | One shared step removes drift; parity tests guard the two call sites; Javadoc states what each test can and cannot prove; CR-009 retarget named |
| Portability | Desktop providers only; Android 16 shown by IC-1; Android 11 to 15 behaviour inferred, not measured |

## 12. Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-065 story | `sdlc/artifacts/build/sprints/sprint-021/U-065-tls12-floor-both-factories-and-pinned-factory-parity.md` | Read in full: AC-1 to AC-11, IC-1, IC-2, tables, Scope Concerns |
| BUG-019 | `sdlc/artifacts/stories/BUG-019-factory-enables-tls10-and-tls11.md` | `:100-234` (measurements, ACs, impact, Suggested Approach) |
| BUG-020 | `sdlc/artifacts/stories/BUG-020-pinned-factory-no-tls-hardening.md` | Read in full (Suggested Approach on widening and sharing) |
| CNTR-MODERNIZATION-001 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md` | `:1-12`, `:88-100`, `:170-178`, `:276-292` by the planner; read in full by the contract-verification gate, whose results are applied here |
| Gate results, iteration 1 | `sdlc/artifacts/build/sprints/sprint-021/U-065/plan-review.md`, `contract-verification.md`, `expert-depth-check.md` | Read in full; section 0 |
| CR-013 | `sdlc/artifacts/build/change-records/CR-013-cntr-001-state-sni-behaviour.md` | S1-S6 `:311-318`, coverage `:374-379`, alternatives `:389-411`, Scope Concern D `:458` |
| CR-009, CR-012 | `sdlc/artifacts/build/change-records/` | CR-009 `:82-85`, `:128`, `:214-267`; status lines of both |
| Feasibility analysis | `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` | Record 8 `:2227`, Record 9 `:2239`, commits via `git log -S` |
| REQ-MODERNIZATION-002, -019; DES-MODERNIZATION-002 | `sdlc/artifacts/requirements/modernization/`, `sdlc/artifacts/design/modernization/` | Searched for protocol, cipher, SNI clauses: none conflicts; REQ-019 `:56`, `:109` require the hardening and SNI cases to survive the migration (this story strengthens them). No REQ of `type: standard` exists |
| U-063 story, evidence, tools | `sdlc/artifacts/build/sprints/sprint-020/U-063-…md`, `…/U-063/evidence/I0-probe.txt`, `I0c-diag.txt:1-30`, `mutation-log.md` (header, M-D10, M-D11, M-E4, M-E5), `tools/` | Provider facts, protocol, tool usage |
| Sprint 020 retro | `sdlc/artifacts/build/sprints/sprint-020/retro.md` | `:62-160` lessons (probe first, compile-checked mutations, clock, timeouts) |
| U-064 workspace | `sdlc/artifacts/build/sprints/sprint-021/U-064/plan.md`, `contract-verification.md`, `review-code.md`, `review-security.md`, `qa-results.md`, `evidence/` | Model plan; hand-over points (8.3) |
| Sprint 021 plan | `sdlc/artifacts/build/sprints/sprint-021/sprint-plan.md` | Ordering, contract gate note |
| Default factory | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` | Read in full |
| Pinned factory | `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java` | Read in full |
| Construction site | `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java` | `:125-164` |
| Tests | `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactoryHardeningTest.java`, `…JdkProviderTest.java`, `…HandshakeTest.java`, `…SniTest.java`, `LoopbackTlsTestServer.java`, `app/src/test/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactoryCreateSocketTest.java`, `…ExpiryTest.java` | Read in full |
| Fixtures | `app/src/test/java/com/zegoggles/smssync/mail/ssl/TlsTestFixtures.java` | `:1-80`, `:325-397` |
| Build | `build.gradle` `:1-50`; `app/build.gradle` `:20-110`, `:150-175`, `:208-460`; `app/src/test/resources/robolectric.properties` | Test config, JaCoCo, SDK levels |
| App log lines | `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` `:169`, `:248-256`, `:313`, `:369` | IC-1 |

## Verdict

**PASS.** The story's facts hold against the code at `b2c2dfd8` (L2 to L11). The design meets every
criterion as stated: the floor is an allow-list step on the supported list (AC-1 (a), (b), (d)), the
fail-open branch applies it and is tested (AC-1 (e)), the pinned factory calls the same two steps in the same
order after one visibility change (AC-2 to AC-5), U-064's seams stay as they are, and nothing the contract
states changes. Every production and mutation edit is given as exact text and compiles by construction. The
JDK layered server-name question was settled on the build JDK during planning (L12) and is re-confirmed in
the sandbox by the first probe; the open environment questions (N1 to N6) are probe or gate steps with a row
for each outcome. Iteration 2 resolves plan-review B-1 (J4 is no longer expected to fail under M-P7; every
mutation row was re-derived against the test code and section 1.2, with failure types, and D30 stops only
on a difference from the row), and applies the observations of all three gates as listed in section 0.

## Phase Completion Report
---
story_id: "U-065"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-021/U-065/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
