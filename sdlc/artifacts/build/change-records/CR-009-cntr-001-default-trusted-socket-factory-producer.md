---
status: approved
severity: medium
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-019
  - REQ-MODERNIZATION-002
  - REQ-MODERNIZATION-003
design_docs:
  - CNTR-MODERNIZATION-001
  - DES-MODERNIZATION-002
related_stories:
  - U-062
  - U-063
  - U-064
tags:
  - change-record
  - cntr-modernization-001
  - tls
  - thunderbird-migration
  - p-5
id: CR-009
title: 'CNTR-MODERNIZATION-001: producer 1 (DefaultTrustedSocketFactory) does not exist under that name in the Thunderbird mail modules; the default factory becomes app-owned code (P-5)'
type: DCR
domain: change-records
summary: 'P-5: CNTR-MODERNIZATION-001 names producer 1 as DefaultTrustedSocketFactory(context); that class does not exist under that name in upstream''s mail modules (it left the mail library in 2022), so the producer becomes app-owned code. Found by the accepted feasibility analysis (U-062).'
updated_at: '2026-11-17T18:01:48.651Z'
---

# CR-009: CNTR-MODERNIZATION-001 — producer 1 becomes an app-owned default factory (P-5)

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-009 |
| **Type** | DCR (Design Correction) |
| **Severity** | Medium |
| **Status** | Open |
| **Reported By** | Requirements Analyst agent, for the product owner (forced change record approved as a batch, Record 4 of the U-062 analysis) |
| **Date Reported** | 2026-11-17 |
| **Source** | Sprint work: U-062 feasibility analysis, item P-5 (analysis Q7.a, Q7.b, Q7.f, Q12.d PO-7, Q12.e condition 10) |

Sibling records against the same contract: CR-007 (P-3, the `createSocket` exception class) and CR-008 (P-4, the slot). All three are applied together as one contract revision (see Timing).

---

> **Note: what stories U-063, U-064 and U-065 made stale in this record, and what the move must now also do.** Added 2026-11-21 by the Requirements Analyst for the orchestrator, after stories U-064 and U-065; the text below this note is kept as approved. Each point was checked against this record and against the code at commit `12b584ab`.
>
> **Rows that no longer describe the code** (named by their text; this record's line numbers are as approved):
> - The Evidence row "Hardening and SNI" (`:85`) cites `hardenSocket` at `:182-189` of the vendored file. It is now `public static` at `:226-241`, with a new step `protocolsToEnable` at `:172-185` and an allow-list `ALLOWED_PROTOCOLS` at `:97-99`; the row says nothing of the protocol floor.
> - The rows "Producer 2 (unchanged)" (`:87`) and, in the guarantee check, "No (producer 2 is unchanged)" (`:234`). The pinned factory now imports the default factory's class and calls its two steps, `hardenSocket` then `setSniHost` (`app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:20`, `:101-102`).
> - The sentence "no test asserts cipher or protocol hardening" (`:206`) and the guarantee row's "the 10 tests" (`:233`). Tests for the hardening exist (U-063, U-065).
> - Test list T1: "the 10 `DefaultTrustedSocketFactorySniTest` cases" is now 14 (12 after U-063, plus the 2 U-064 added for a name the platform rejects). T2 (an eight-group IPv6 literal, "Missing") exists. T3 and T4 (host-name mismatch; untrusted and expired chain, "Missing") exist in `DefaultTrustedSocketFactoryHandshakeTest` since U-063, for both call shapes. T5 ("Missing") exists and now also covers a protocol floor: suggested wording, "`hardenSocket`, `protocolsToEnable`, `reorder`: blocked and ordered cipher suites; protocols limited to the allowed list from the platform's supported list. Exist (U-063, U-065)". T6 (the factory is called on both connection paths) is still missing.
>
> **New test row T11 (suggested):** "Pinned and default parity: the protocols (set and order), the cipher suites (order) and the server names of the two factories' sockets are equal, per shape and per provider (`PinnedCertificateSocketFactoryCreateSocketTest.createSocket_implicit_appliesDefaultFactoryProtocolAndCipherHandling` and `createSocket_layered_...`; `DefaultTrustedSocketFactoryJdkProviderTest.pinnedAndDefaultFactories_sameProtocolsCiphersAndServerNames_jdkProvider`). Exist; retarget."
>
> **New line for the move's checklist (suggested):** "`PinnedCertificateSocketFactory.createSocket` calls the default factory's `hardenSocket` and `setSniHost` (U-065, commit `12b584ab`). Retarget both calls and the import to the app-owned default factory in the same change. The shared step may become package-private once both classes share a package. The check in the Fix Order, `grep -rn "com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory" app/src` returns 0, catches a leftover import if it is run."
>
> **The baseline the move must keep green is the tests as U-063, U-064 and U-065 left them, not the table above:** in `app/src/test/java/com/zegoggles/smssync/mail/`, `ssl/DefaultTrustedSocketFactoryHardeningTest`, `ssl/DefaultTrustedSocketFactoryJdkProviderTest`, `ssl/DefaultTrustedSocketFactoryHandshakeTest`, `ssl/DefaultTrustedSocketFactorySniTest`, `PinnedCertificateSocketFactoryCreateSocketTest`, `PinnedCertificateSocketFactoryExpiryTest` and `PinnedCertificateSocketFactoryTest` (732 tests at `12b584ab`, 2 skipped). Four of these classes reach `ALLOWED_PROTOCOLS`, `protocolsToEnable` and the three-argument `hardenSocket` by reflection, so a member missing after the move means the tests are retargeted, not that they are dropped. A failed behavioural assertion after the move is a regression, not a reason to edit the test.
>
> **What must hold after the move, besides what this record says:** the server-name behaviour of both producers (CR-013, statements S1 to S6) and the protocol floor and cipher-suite handling of both producers (CR-014, statements V1 to V5, C1 to C3, B1 to B3). A default factory re-implemented from upstream instead of moved from the vendored class could drop them while the moved tests stay green only if those tests are not retargeted; see CR-014, Interactions and Scope Concern E, for the loopback test (T6, one case more) that survives the swap.

## Finding

### Description

CNTR-MODERNIZATION-001 allows exactly two socket factories to cross the TLS boundary. Producer 1, for the default `SYSTEM_VALIDATED` policy, is written as `new DefaultTrustedSocketFactory(context)` and identified as `com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory` (k-9). That class is part of the vendored library today (`k9mail-vendored`). Upstream moved it out of its mail library on 2022-04-28 [C-472]. At the pinned commit the nearest counterpart is `com.fsck.k9.helper.DefaultTrustedSocketFactory(context, trustManagerFactory)` in the Android module `:legacy:core` [C-345], wired by upstream's dependency injection [C-351]. That module is not one the app will consume (analysis Section 2, Q7.f).

So after the migration the name in the contract points at nothing the build contains. The default factory, with its protections, has to be code the app owns: moved from `k9mail-vendored` into `app/src/main`. The contract's statements about producer 1 (its class, its constructor, its location) are therefore wrong after the migration, while the statements about what it must do are still required.

Why this one is more than a rename. The mail modules verify nothing about TLS themselves; hostname verification, SNI and cipher hardening are all decided inside whichever factory the app passes (analysis Section 7 opening paragraph). Upstream's own default factory is **not** equivalent to the app's: for an empty or blank host name it has no guard, so an unchecked `IllegalArgumentException` leaves `createSocket` where the vendored factory skips SNI quietly (analysis Q7.c, 2 of 10 cases differ; 2 have no counterpart). The app's guard exists because of BUG-015 (U-047). Taking upstream's factory would drop it without anything failing, because no test connects. This is why the analysis marks the protection "supported" only as **app-owned** code moved from the vendored module (Q7.a, rows 1 and 3).

Nothing is wrong today. The contract describes the live vendored code correctly for this item. This is a forward-looking design correction.

### Source of Truth

- **Accepted feasibility analysis**, `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`: Section 7 Q7.a (lines 611-620), Q7.b "Producer 1" row (line 633) and the P-5 row (line 643), Q7.c (lines 647-664), Q7.f (lines 692-702), Q7.g (line 708), Q7.i (lines 726-734); Section 12 Q12.d row PO-7 (line 1336), Q12.e conditions 10, 12 and 17 (lines 1358, 1360, 1365), Q12.f items 5 and 8 (lines 1373, 1376). Product owner decisions: Record 3, and Record 4 for the blocklist (line 2179) and the batch approval (line 2180).
- **Upstream at the pin** (commit `ac1f0e85f63c1b600ac443d1fb2d939bffff7de5`), as the analysis cites it: `DefaultTrustedSocketFactory.kt` lines 24-62 [C-345]; Koin wiring [C-351]; the commit "Move `DefaultTrustedSocketFactory`", 2022-04-28 [C-472]; `TrustManagerFactory.java` [C-336, C-338]; `CertificateBlocklist.kt` [C-344]. I did not re-read upstream.
- **The contract being corrected**: `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`, status `approved`, version v1.

### Evidence

Contract text, quoted from `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-001-tls-socket-factory.md`:

```
:64   │     SYSTEM_VALIDATED   → new DefaultTrustedSocketFactory(context)
:93   | Policy (`TlsTrustPolicy`) | Concrete factory handed across | Embedded `X509TrustManager` behavior |
:95   | `SYSTEM_VALIDATED` (default) | `com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory` (k-9; imported `BackupImapStore.java:29`) | Full Android `TrustManager` chain validation against the system CA store. `checkServerTrusted()` throws `CertificateException` on any untrusted/expired/hostname-mismatched chain. |
:275      : new DefaultTrustedSocketFactory(context);
```
(`:94` is the table's separator row.) Further lines that name the same class, found by this record's search: `:153` (`//   returns DefaultTrustedSocketFactory for SYSTEM_VALIDATED,`), `:173-174` (Invariant 2: "exactly one of `DefaultTrustedSocketFactory` (system validation) or `PinnedCertificateSocketFactory`"), `:196` (error table), `:243` (scenario A, `: new DefaultTrustedSocketFactory(getApplicationContext());`).

Live code, read this session:

| What | File and line | Text |
|------|---------------|------|
| The only production construction of producer 1 | `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java:148` (in `buildSocketFactory`, `:135-149`) | `return new DefaultTrustedSocketFactory(context);` with the import at `:31` (`com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory`) |
| The class | `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java:33`, constructor `:120`, `createSocket` `:152-180` | `public class DefaultTrustedSocketFactory implements TrustedSocketFactory {` ... `public DefaultTrustedSocketFactory(Context context) {` |
| Trust manager it installs | `.../ssl/TrustManagerFactory.java:68-94` (`checkServerTrusted`: system validation `:76`, hostname verification `:77`, local key store only after a failure `:91`) | `defaultTrustManager.checkServerTrusted(chain, authType); verifyHostname(mHost, chain); return;` |
| Hardening and SNI | `.../ssl/DefaultTrustedSocketFactory.java:182-189` (`hardenSocket`), `:191-205` (`setSniHost`), `:216-226` (`isIpOrBlankHostname`), `:238-254` (`setSniViaSSLParameters`) | Skips SNI for blank hosts, for any host containing `:`, and for four-part IPv4 literals |
| The library's user-accepted-certificate store is inert | `.../ssl/LocalKeyStore.java:25` (`setKeyStoreLocation`) and repository search below | No call to `setKeyStoreLocation`, `addCertificate` or `LocalKeyStore` anywhere in `app/src`; `LocalKeyStore` is used only at `TrustManagerFactory.java:91,104` inside the vendored module |
| Producer 2 (unchanged) | `app/src/main/java/com/zegoggles/smssync/mail/PinnedCertificateSocketFactory.java:55`, `:67`, `:126-150` | App-owned; fingerprint equality then validity window; does not verify the host name |

Upstream side, quoted from the accepted analysis (lines 633 and 643; not re-measured here):

> The default factory is `com.fsck.k9.helper.DefaultTrustedSocketFactory(context: Context?, trustManagerFactory: TrustManagerFactory)` in `:legacy:core` [C-345], wired by Koin there [C-351]; that module is not in the proposed set.

> P-5 ... The named class `com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory(context)` does not exist at the pin; upstream's counterpart is in `:legacy:core` with a different constructor, so the producer would be an app-owned class.

---

## Triage

### Classification

- **Type:** DCR (Design Correction). The contract names a producer class that will not exist; the source of truth (upstream at the pin, as measured by the accepted analysis) shows the producer must be app-owned code.
  - Not a DEF: the implementation matches the contract today (`K9MailTransport.java:148`).
  - Not an RCR: the requirement that the default factory validate the chain, verify the host name and set SNI correctly does not change; only who owns the code does. REQ-MODERNIZATION-002 AC-3 (`:110`) already allows "`DefaultTrustedSocketFactory` or an equivalent that delegates chain validation to the Android `TrustManager` framework", and REQ-MODERNIZATION-019 AC-B8 already asks for the hostname-mismatch test.
- **Root:** Design and contracts layer. The contract named a library class and its constructor as part of a security boundary, so a library change that is outside the app's control invalidated it.
- **Layers Affected:** 5. Design and contracts (the text change); application code, tests, the vendored library and the build gates (the coverage gate in `app/build.gradle`) when the migration lands. The text change by this record touches 1 layer.
- **Severity Justification:** Medium (isolated impact, non-blocking). Nothing is blocked: no migration story exists, U-062 is done and sprint-019 is closed. It is the largest of the three records and carries the only real security risk of the batch (an ownership change for a security-critical class, a protection that has no test, an SNI guard that is not in the contract), but that risk is realised only if Part B ignores it, and the record's own change is one contract. It is not high because no open story is affected. The "5+ files" criterion for critical is read as files this record changes (one), not the migration footprint; if the product owner reads it by footprint (listed below, more than 5 files), the record would still not be critical because nothing is blocked. Raising it to high is a defensible alternative reading and is the product owner's call.

---

## Impact Analysis

> `change_records.layers` is not configured in `sdlc/config.yaml`. The layers below are the ones given for this batch.

Checklist run: `knowledge/checklists/impact-analysis-checklist.md`. No `[TBC]` placeholders are involved.

Ground truth (layer 1, read-only): upstream at the pin: `DefaultTrustedSocketFactory.kt:24-62` [C-345] in `:legacy:core`. Correct value: producer 1 does not exist as `com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory(context)` at the pin; the app owns the default factory.

### Searches run

| # | Search | Path | Result |
|---|--------|------|--------|
| 1 | `DefaultTrustedSocketFactory` and `new DefaultTrustedSocketFactory` | `app/src/main` | Construction: `K9MailTransport.java:148` only. Mentions in code or javadoc: `K9MailTransport.java:31,78,103`; `BackupImapStore.java:64`; `PinnedCertificateSocketFactory.java:61`; `TlsTrustPolicy.java:32`; `MailTransportConfig.java:38` |
| 2 | `new DefaultTrustedSocketFactory(` | `app/src/test` | 29 constructions in 5 files: `BackupImapStoreTest` 8, `K9MailTransportPortMethodsTest` 14, `K9MailTransportTest` 5, `K9MailTransportCreateOpenTest` 1, `BackupImapStoreDelegateIntegrationTest` 1 |
| 3 | `DefaultTrustedSocketFactory` | `app/src/test/.../mail/ssl/DefaultTrustedSocketFactorySniTest.java` | 10 `@Test` methods; 8 call the vendored static `setSniViaSSLParameters` (`:53,75,156,177,193,207,222,236`), 2 reach the private reflective method (`:252-255`) |
| 4 | `DefaultTrustedSocketFactory\|com.fsck.k9.mail.ssl` | `k9mail-vendored/src/main` | The class (`ssl/DefaultTrustedSocketFactory.java`), its interface, and library-internal default producers that the app never calls: `store/RemoteStore.java:10,52,56`, `Transport.java:6,26`, `store/webdav/WebDavSocketFactory.java:3,67` (webdav is excluded from the source set, analysis Q7.d) |
| 5 | `hostname.?(mismatch\|verif)\|wrong.?host\|HostnameVerifier\|TrustManagerFactory\|SecureX509\|CertificateValidationException\|CertificateChainException` | `app/src` | 1 match, a comment in `app/src/main/res/xml/network_security_config.xml:8`; 0 lines in `app/src/test` |
| 6 | `expired\|untrusted\|self.?signed\|SSLHandshake\|CertPath` | `app/src/test/.../mail` | 3 matches, none an assertion: two test certificate fixtures (`BackupImapStoreTest.java:25`, `K9MailTransportTest.java:36`) and an authentication message (`K9MailTransportPortMethodsTest.java:368`) |
| 7 | `ENABLED_CIPHERS\|ENABLED_PROTOCOLS\|hardenSocket\|setEnabledCipherSuites\|setEnabledProtocols\|reorder(` | `app/src/test` | 2 matches, both no-op overrides in a fake socket (`DefaultTrustedSocketFactorySniTest.java:274,277`) |
| 8 | `setKeyStoreLocation`; `addCertificate\|LocalKeyStore` | `**/src/main/**`; `app/src` | `setKeyStoreLocation`: 3 matches, all inside `LocalKeyStore.java` (`:25,48,67`). `addCertificate` or `LocalKeyStore` in `app/src`: 0 |
| 9 | `DefaultTrustedSocketFactory\|TrustedSocketFactory\|CNTR-MODERNIZATION-001\|CNTR-001` | `sdlc/artifacts/design`, `sdlc/artifacts/requirements` | Design restating producer 1: `DES-MODERNIZATION-002:137,156,167,347,392,433`; `DES-MODERNIZATION-009:300`; `CNTR-MODERNIZATION-002:297`. Requirements: `REQ-MODERNIZATION-002:110` (compatible, says "or an equivalent"); `:45`, `:204` quote the removed trust-all code (history) |
| 10 | `smssync.mail` rules in `app/build.gradle` | `app/build.gradle:340-376` | PACKAGE rule `com.zegoggles.smssync.mail*`, LINE, minimum 0.70 (`:368-376`); comment `:367`: "MEASURED (U-051): mail=71.5%, mail.transport=88.8%" |
| 11 | `TrustedSocketFactory\|ssl`; and `com.fsck.k9.mail.ssl` in `*.pro`, `*.gradle`, `*.kts`, `*.xml` | `app/build.gradle`; repository | 0 build or shrinker rules name the class (other hits are evidence files under `sdlc`) |

### Layer Impact Matrix

| # | Layer | Affected? | Files and lines | Occurrences | Action Needed |
|---|-------|-----------|-----------------|-------------|---------------|
| 1 | Ground truth: upstream at the pin (external) | — | `DefaultTrustedSocketFactory.kt:24-62` [C-345]; `LegacyCoreModule.kt:63-66` [C-351] | — | Ground truth (read-only) |
| 2 | Requirements | No text change | `REQ-MODERNIZATION-002:110` already says "or an equivalent"; `REQ-MODERNIZATION-019` AC-B8 (`:109`) asks for the tests listed below; `REQ-MODERNIZATION-003` (coverage gate) is affected in practice (see build gates) | 0 text changes | None. REQ-003's gate constrains where the moved code can go |
| 3 | Design and contracts | **Yes (root)** | `CNTR-MODERNIZATION-001`: `:64`, `:95` (and `:93` header), `:275` (analysis-cited); `:153`, `:173-174`, `:196`, `:243` (found by this record). Restating design: `DES-MODERNIZATION-002:137-138,156,167,347,392,433`; `DES-MODERNIZATION-009:300`; `CNTR-MODERNIZATION-002:297` | 8 contract lines; 8 lines in other documents | Contract: reword at apply time. DES-002, DES-009 and CNTR-002 are approved records of the as-built design: leave unedited and let the contract changelog state that v2 supersedes their producer-1 wording (Solution Architect to confirm) |
| 4 | Stories and sprints | No in-progress work | Closed records only (U-008, U-010, U-045, U-047, BUG-012, BUG-015 and others) | 0 open | None |
| 5 | Application code | Yes, at migration | `mail/transport/K9MailTransport.java:31` (import), `:148` (construction); javadoc mentions `K9MailTransport.java:78,103`, `BackupImapStore.java:64`, `PinnedCertificateSocketFactory.java:61`, `TlsTrustPolicy.java:32`, `mail/transport/MailTransportConfig.java:38`. New app-owned code: the default factory and its trust manager moved from the vendored module | 1 construction site; 5 javadoc mentions | Part B migration stories (move the code, repoint the import). Not done by this record |
| 6 | Tests | Yes, at migration, and new tests are required | 29 constructions in 5 files (search 2); 3 `isInstanceOf(DefaultTrustedSocketFactory.class)` assertions (`BackupImapStoreTest.java:91,111`, `K9MailTransportTest.java:75-76`; the `systemValidated_policy_creates_DefaultTrustedSocketFactory` test is at `K9MailTransportTest.java:68`); `DefaultTrustedSocketFactorySniTest.java:14` (import) and the 10 tests | 6 files | Rename-only retarget for the existing tests; add the new tests in Proposed Change |
| 7 | Vendored library | Yes, source of the moved code | `ssl/DefaultTrustedSocketFactory.java` (267 lines), `ssl/TrustManagerFactory.java` (191 lines, with the `MinimalSslSession` shim), `ssl/CertificateChainException.java`; `ssl/LocalKeyStore.java` (198) and `ssl/KeyChainKeyManager.java` (215) only if kept (both inert or unused in the app, analysis Q7.f). Line counts from the analysis (Q7.f), not re-counted | 3 to 5 files, 458 lines minimum | Part B decides the exact set. The rest of the module leaves |
| 8 | Build gates | Yes (found by this record) | `app/build.gradle:368-376`: LINE ≥ 0.70 per package under `com.zegoggles.smssync.mail*`; `:346-351` (a comment on the `service*` rule, which uses the same pattern form as the `mail*` rule) records that each matching sub-package is evaluated separately with its own limit; I read this from the build file and did not run the gate; `:367`: `mail` measured 71.5% at U-051 (headroom 1.5 points; not re-measured by me); no `ssl` exclusion in `jacocoFileFilter` (`:232-245`) | 1 rule | Moving about 458 lines of mostly un-exercised code into any package under `mail*` (or a new `mail.ssl` package, checked on its own) will fail `jacocoTestCoverageVerification` unless the new tests below land with it. Do not place the code outside `mail*` to avoid the gate: that would hide the code from the repository's own quality control |
| 9 | Documentation | No | `README.md`: 0 matches; engagement folder: no match | 0 | None |
| | **Total** | | | | 1 contract (8 lines) at apply time; 1 production construction site, 6 test files, 3 to 5 moved source files and 1 build gate at migration |

### Pre-existing drift relevant to reading this record (reported, not fixed here)

- The contract places resolution in `ServiceBase.getBackupImapStore()` (`:18`, `:56`, `:150-152`, `:226-227`). No `ServiceBase` file exists under `app/src`; the live resolution is `K9MailTransport.buildSocketFactory` (`K9MailTransport.java:135-149`), fed by `di/MailModule.kt:78-98`. The contract's `resolveSocketFactory(Context, String, int)` (`:152`) has no match in `app/src/main`. The combined revision should say where the default factory is built; this record only says who owns the class.

---

## Proposed Change

Wording changes to apply to `CNTR-MODERNIZATION-001` when the timing condition is met. Nothing is applied by this record. The class name and package of the app-owned default factory, and any constructor argument beyond the application `Context`, are fixed by the Part B design; the placeholder `<app-owned default factory>` below stands for that name and is replaced when the revision is applied.

### Line `:64` (diagram; analysis-cited)

Current text:
```
        │     SYSTEM_VALIDATED   → new DefaultTrustedSocketFactory(context)
```
Proposed text:
```
        │     SYSTEM_VALIDATED   → new <app-owned default factory>(context)
```

### Lines `:93-95` (producer table; analysis-cited)

Current text (row `:95`; `:93` is the header and is unchanged):
```
| `SYSTEM_VALIDATED` (default) | `com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory` (k-9; imported `BackupImapStore.java:29`) | Full Android `TrustManager` chain validation against the system CA store. `checkServerTrusted()` throws `CertificateException` on any untrusted/expired/hostname-mismatched chain. |
```
Proposed text:
```
| `SYSTEM_VALIDATED` (default) | `<app-owned default factory>` (`com.zegoggles.smssync...`; owned by this repository, moved from the vendored `com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory`; not supplied by the library) | Full Android `TrustManager` chain validation against the system CA store. `checkServerTrusted()` throws `CertificateException` on any untrusted/expired/hostname-mismatched chain. |
```
Only the second cell changes. The third cell is carried over word for word. The package prefix is completed by the Part B design.

### Line `:275` (scenario C; analysis-cited)

Current text:
```
    : new DefaultTrustedSocketFactory(context);
```
Proposed text:
```
    : new <app-owned default factory>(context);
```
Line `:276`, in the same code block, is CR-008's.

### Lines found by this record's search (not in the analysis's list)

`:153` and `:243` (handoff comment and scenario A): replace `DefaultTrustedSocketFactory` with `<app-owned default factory>`. `:173-174` (Invariant 2): replace "exactly one of `DefaultTrustedSocketFactory` (system validation)" with "exactly one of the app-owned default factory (system validation)"; the rest of the sentence, including "never `AllTrustedSocketFactory`", is unchanged. `:196` (error table, first cell): replace the possessive `DefaultTrustedSocketFactory`'s TrustManager" with "the default factory's TrustManager"; the exception class in that row is CR-007's.

### Who owns the default factory after the change

- **Owner:** this repository's app module, in the transport-security policy layer next to `PinnedCertificateSocketFactory` (which is already app-owned, `PinnedCertificateSocketFactory.java:55`). It is not owned by the fork, by upstream or by the library.
- **Consequences of that ownership.** Rebasing the fork, or an upstream security fix in the mail code, does not reach this class; Record 4 says the fork is rebased "on demand" and that nothing detects such fixes automatically. Cipher and protocol lists, the SNI guard and the hostname-verification path become this repository's to maintain and review. A change to the class needs the same review and tests as the pinned factory.
- **What moves** (analysis Q7.f, not re-counted): at minimum `DefaultTrustedSocketFactory.java` (267 lines) and a trust manager that does system validation plus host-name verification, either the vendored `TrustManagerFactory.java` (191 lines, with the `MinimalSslSession` shim and `CertificateChainException`) or upstream's public `TrustManagerFactory` from `:mail:common`. Which one is a Part B design choice; the analysis notes the two verify host names with different implementations (the platform verifier against httpclient5's `DefaultHostnameVerifier`) and that whether they accept the same certificates was not tested (Q7.a, row 1). `LocalKeyStore` and `KeyChainKeyManager` move only if kept.

### Tests that must exist for the app-owned default factory

The gap, named: **no hostname-mismatch test exists in the app today** (search 5: 0 lines in `app/src/test`; analysis Q7.i and Q12.f item 8). Nothing in the suite fails if a certificate for the wrong host is accepted. Two related gaps found by this record's searches: no test rejects an untrusted or expired chain under `SYSTEM_VALIDATED` (search 6), and no test asserts cipher or protocol hardening (search 7). The existing SNI tests call static helpers on a mock socket; none opens a connection or performs a handshake.

| # | Test | Proves | State today |
|---|------|--------|-------------|
| T1 | The 10 `DefaultTrustedSocketFactorySniTest` cases against the moved class | SNI set for DNS names (3 cases); not set for IPv4, abbreviated IPv6 (`::1`, `fe80::1`), empty and blank hosts, with no exception (5 cases); reflective fallback (2 cases) | Exist; move with the class (import rename only). If the minSdk change of CR-011 makes the pre-API-24 branches unreachable, whether to delete them and the 2 reflective cases is a Part B choice |
| T2 | A true eight-group IPv6 literal | No SNI for a full IPv6 literal | Missing. The case named `..._ipv6FullLiteral_...` passes `fe80::1` (`DefaultTrustedSocketFactorySniTest.java:207`); analysis Q12.f item 5 |
| T3 | Hostname mismatch, implicit TLS and STARTTLS | A loopback TLS server presents a certificate valid for a different host: the connection is refused with a certificate-validation exception; a certificate for the right host from the same test authority is accepted (control) | **Missing.** Analysis condition 12(a); REQ-MODERNIZATION-019 AC-B8 |
| T4 | Untrusted and expired chain | The chain fails closed under `SYSTEM_VALIDATED` | Missing |
| T5 | Cipher and protocol hardening | `hardenSocket` / `reorder` apply the disallow and ordering lists to the enabled suites and protocols | Missing (search 7) |
| T6 | The factory is called on both connection paths | A recording factory sees `createSocket` for implicit TLS and again after STARTTLS; unknown or missing scheme and missing credentials open no socket | Missing. Analysis conditions 12 and 17; shared with CR-008 |
| T7 | Producer selection | `SYSTEM_VALIDATED` yields the app-owned default factory, `PINNED_CERTIFICATE` yields `PinnedCertificateSocketFactory` | Exist (`K9MailTransportTest.java:68-76`, `:88`); retarget the class name |
| T8 | Blocklist, only if wired | A chain containing a listed public-key hash is rejected before system validation; a chain without one passes; the list's maintenance is tested as the security review (L-A6) asks | Missing; depends on the design |
| T9 | Host-name verifier equivalence, only if upstream's trust manager is chosen | Wildcard, common-name fallback and IP-address entries give the same accept and reject results as the vendored verifier | Missing; the analysis says this was not tested |
| T10 | The library key store stays inert | `grep -rn "LocalKeyStore\|setKeyStoreLocation\|addCertificate" app/src/main` returns 0 unless a contract revision allows it | Holds today (search 8) |

T3 to T6 are also what the repository's coverage gate will demand of the moved code (search 10). T3 to T5 describe the live vendored factory too: they can be written against it before the migration as characterization tests, which is the analysis's own recommendation ("a test-hardening follow-up now", Q12.f item 8). That is test work, not a contract or code change under this record, and it is not created here.

### Where the certificate-blocklist decision touches these lines

Record 4 took upstream's certificate blocklist; how it is wired with the app-owned trust code is a design question, not decided. It touches this record at `:95`, third cell ("Full Android `TrustManager` chain validation against the system CA store"), and at Invariant 2 (`:173-176`): upstream's trust manager rejects a chain containing a listed public-key hash before it calls system validation [C-338, C-344, analysis Q7.g]. A blocklist can only make the default factory refuse more; it cannot turn it into a trust-all or weaken the pinned path. Open design questions this record does not settle and does not write into the contract: whether the blocklist applies to the default factory only or also to `PINNED_CERTIFICATE`; which exception a listed chain raises and whether it is a `CertificateException` (the contract's row `:196` depends on that); and whether the contract states the blocklist at all. The wording of `:95` in this record leaves room for a sentence to be added.

### Guarantee check: what each reworded line carries, and that it is unchanged

| Guarantee | Where stated in the contract | Carried by the lines reworded here? | Result |
|-----------|------------------------------|-------------------------------------|--------|
| Certificate validation | `:95` third cell; Invariant 2 `:173-176` | Yes: `:95`, `:173-174` | Unchanged. The third cell is copied word for word. The moved vendored code keeps system validation first (`TrustManagerFactory.java:76`); upstream's trust manager does it second after the blocklist (blocklist note above) |
| Hostname verification | `:95` third cell ("hostname-mismatched chain") | Yes: `:95` | Unchanged in the contract. In code it stays unchanged only if the moved vendored trust manager is used; a different verifier needs T9. T3 is the missing proof either way |
| SNI, including no SNI for IP literals and blank hosts | **Not stated anywhere in the contract** (0 matches for `SNI`, `server name`, `IP literal`). Lives in code `DefaultTrustedSocketFactory.java:191-254` and the 10 tests | No line of the contract carries it, so no reworded line can weaken it. It is the guarantee most at risk in this migration | Unchanged by the rewording. The ownership rule (the factory stays app-owned and moves with its tests T1, T2) is what keeps it. See Scope Concern |
| Pinning and user-trusted certificates | `:96`, `:181-184`, `:215-216` | No (producer 2 is unchanged) | Unchanged. For the default factory: the library's user-accepted-certificate store stays inert (search 8, T10); activating it would add a second user-trust path beside the enrolment flow, against Validation Rule 4 (`:215-216`) |
| No silent fallback | `:200-202`, Invariant 2 | Indirectly | Unchanged. The moved `createSocket` (`DefaultTrustedSocketFactory.java:152-180`) has no catch-and-downgrade; the app-owned copy must not gain one |

**Scope Concern (raised, not decided; for the Solution Architect and product owner):** the contract does not state the SNI behaviour (SNI for DNS names; none for IPv4 and IPv6 literals and for empty or blank hosts, without throwing). Since this record puts exactly that code under the app's ownership, the combined revision is the natural place to state it in the `SYSTEM_VALIDATED` row. That would be an addition to the contract, not a rewording, so it is not written into the proposed text above and needs the product owner's agreement. A candidate sentence: "The factory sets SNI for DNS host names and does not set SNI for IP-address literals or empty or blank host names, and does not throw for them."

---

## Timing

- **When to apply:** with the story or stories that make it true, in **one** revision of CNTR-MODERNIZATION-001 (v1 to v2) together with CR-007 and CR-008. The trigger for this record's lines is the Part B story that removes `K9MailTransport.java:31` and `:148`'s dependence on the vendored class by putting the app-owned default factory in place. If the Part B design moves the factory into the app before the library swap (a rename-only step while the vendored library is still live), that story is the trigger, and the revision is still applied once, at the last of the three code changes. Part B design is not blocked by waiting: it cites the approved CR-009 as the target form (analysis condition 10 asks for "CR-nnn approved", not "applied").
- **Why not now:** the contract is `approved` and describes the vendored class as the live producer; sprint contract verifications compare it with code. TLS fixes were made in the vendored class in sprints 10 and 11 (U-045, U-047, BUG-012, BUG-015) and another is possible before the migration. Writing the app-owned name now would make the contract false for that period. The CR-002 precedent (contract amended before the code) does not apply, for the reason given in CR-007.
- **Must not happen before then:**
  - Any edit to `CNTR-MODERNIZATION-001` text.
  - Switching `K9MailTransport.java:148` to any other factory, or taking upstream's `DefaultTrustedSocketFactory` as a shortcut (it lacks the blank-host guard).
  - Deleting or editing `DefaultTrustedSocketFactorySniTest` before the moved class and its tests exist.
  - Moving the factory code into a package outside `com.zegoggles.smssync.mail*` to avoid the coverage gate.
  - Supplying a key-store directory for the library's `LocalKeyStore`, or any call to `addCertificate`.
  - Marking this record implemented on approval.
- **Together or separately:** together. Lines `:275` and `:276` are in one code block (this record and CR-008); `:95` sits in the table whose lead-in carries CR-007's carried-type block. A separate pass leaves scenario C and the producer table half rewritten. If the Part B design splits the code changes across stories, apply the revision at the last of them and have each earlier story name the deviation from the contract text.
- **The test follow-up is not subject to this timing.** T3 to T5 can be written against the live factory now and would then serve as the oracle for the move; the analysis routes the hostname-mismatch test to "a test-hardening follow-up now" (Q12.f item 8). It is outside this record.

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only — refined during story planning.

**What changes.** The app takes ownership of a security-critical component: system validation, host-name verification, the SNI guard and cipher hardening. The contract rewording is sound. The risk is in the move, because no test today makes a TLS connection (record searches 5-7: no host-name-mismatch, untrusted-chain or hardening test). This is the record in the batch most likely to hide a regression. DCR: agreed. Medium is acceptable; High is defensible on risk, and either reading leads to the same next steps.

**Recommended approach: characterisation tests first** (Feathers, *Working Effectively with Legacy Code*, Ch. 13). T2 (a full eight-group IPv6 literal), T3 (host-name mismatch on both paths), T4 (untrusted or expired chain) and T5 (hardening) can be written now against the live vendored factory. They then serve as the oracle for the move. In the whole batch, this is the only work that warrants a **story of its own before Part B**. It can run in parallel with REQ-015, REQ-016 and REQ-017. The analysis already routes it as "a test-hardening follow-up now" (Q12.f item 8); the product owner decides when to schedule it.

**Placement conflicts with CR-005.** The record puts the factory "next to `PinnedCertificateSocketFactory`" under `com.zegoggles.smssync.mail*`. The factory implements `com.fsck.k9.mail.ssl.TrustedSocketFactory`, so CR-005's Rule 1 and AC-B3 require it under `mail/transport/`. The pinned factory (today in `com.zegoggles.smssync.mail`, library imports at lines 19-20) must move there as well. I recommend a dedicated sub-package, for example `mail.transport.tls`. The coverage rule `com.zegoggles.smssync.mail*` (`app/build.gradle:368-376`) checks each sub-package on its own at LINE ≥ 0.70, so the gate forces T3 to T6 to land with the move. Inside `mail.transport` (88.8% at U-051) the code could hide behind other well-covered classes. Trade-off: the move cannot merge without its tests, which is the point.

**Which trust manager.** Moving the vendored `TrustManagerFactory` keeps the platform host-name verifier introduced by U-058 and makes T9 unnecessary. Upstream's verifier (httpclient5) needs T9 plus an argument that the two are equivalent. I recommend the vendored one unless the way the blocklist is wired argues otherwise.

**Effect of CR-011.** minSdk 24 leaves the vendored factory's pre-Android 7.0 SNI path dead: `DefaultTrustedSocketFactory.java:192` tests `SDK_INT >= N`, and `:198` is the reflective fallback. Drop both in the move, together with the two reflective SNI tests. The move then carries less code, and less uncovered code under the gate. It also makes the move depend on REQ-015.

**Decisions before design (product owner, with security review).**
1. Should the contract state the SNI behaviour (the Scope Concern)? I recommend yes. It is the guarantee most at risk, and no contract line carries it today.
2. Blocklist scope and exception class. The pinned path accepts only one enrolled certificate, so the blocklist adds most on the default path. Whether it also applies to the pinned path is a security-review call.

**Ownership cost.** Record 4 keeps the fork rebased only on demand, and nothing detects upstream TLS fixes automatically. Upstream fixes in `:legacy:core`'s factory or `:mail:common`'s `ssl` package must therefore be watched by hand for this class.

**Story shape and order.** No story is needed for the text. The move is part of the Part B TLS story (analysis Section 11, rows 5-6) and comes after the characterisation story. The text goes into the combined CNTR-MODERNIZATION-001 v2.

---

## Cascade Analysis

**Root Layer:** Design and contracts (CNTR-MODERNIZATION-001 v1)

**Root Defect:** The contract names a library class and constructor as the default producer inside a security boundary. The library removed that class from its mail modules years before the migration, so the producer must become app-owned code, and the protections the contract expects of it (host-name verification, SNI guard, hardening) have to be owned, tested and gated by the app.

**Cascade Direction:** Mid-stream down (contract, then one construction site, the moved code and its tests, and the coverage gate; restating design documents need no edit)

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Design and contracts (CNTR-001) | Other design documents | Yes, as restated history | DES-002 `:137-138,156,167,347,392,433`; DES-009 `:300`; CNTR-002 `:297`. Approved as-built records; not edited |
| 2 | Design and contracts | Requirements | No text change | REQ-002 `:110` says "or an equivalent" |
| 3 | Design and contracts | Application code | Yes, at migration | `K9MailTransport.java:31,148` |
| 4 | Application code | Tests | Yes, at migration | 29 constructions in 5 files, 3 type assertions, 10 SNI tests |
| 5 | Application code / tests | Build gates | Yes, at migration | `app/build.gradle:368-376` per-package LINE ≥ 0.70, `mail` at 71.5% when measured at U-051 |
| 6 | Application code | Vendored library | Yes, source of the move | `ssl/DefaultTrustedSocketFactory.java`, `ssl/TrustManagerFactory.java`, `ssl/CertificateChainException.java` |
| 7 | Design and contracts | Documentation | No | `README.md` and engagement folder: no match |

### Fix Order

1. [ ] Design and contracts: apply the CR-009 wording in the combined CNTR-MODERNIZATION-001 revision, ROOT FIX
2. [ ] Application code: move the default factory and its trust manager into the app; repoint `K9MailTransport.java:31,148` (Part B story), CASCADE FIX
3. [ ] Tests: T1 and T7 retarget; add T2 to T6 (and T8, T9 as the design requires); keep T10 as a gate, CASCADE FIX
4. [ ] Build gates: confirm `jacocoTestCoverageVerification` passes with the moved code in place, CASCADE FIX
5. [ ] Verification: `grep -rn "com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory" app/src` returns 0; the contract names no library class as producer 1; `grep -rn "org.apache.http" app/src/main` returns 0 (analysis condition 12)

### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| None | — | — | No open story. Hold nothing |

---

## Plan

- **Execution Window:** Backlog. Approval now (a precondition of Part B design); text applied with the Part B migration story, in the combined revision.
- **Assigned To:** TBD
- **Estimated Effort:** About 1 hour for the contract text; the moved code and tests T2 to T6 are the Part B migration stories' own work
- **Sprint:** None scheduled

### Fix Checklist

- [ ] Design and contracts: reword `CNTR-MODERNIZATION-001` `:64`, `:95`, `:153`, `:173-174`, `:196`, `:243`, `:275` as in Proposed Change, in the combined revision
- [ ] Design and contracts: decide the Scope Concern (state the SNI behaviour in the contract) and the blocklist wording; changelog line that v2 supersedes the producer-1 wording in DES-002, DES-009 and CNTR-002; set `change_records` frontmatter (Artifact Librarian)
- [ ] Application code: move the default factory and trust manager; repoint the import and construction (Part B story)
- [ ] Tests: T1, T7 retarget; T2 to T6 new (Part B stories); T8, T9 as the design requires
- [ ] Build gates: `./gradlew :app:jacocoTestCoverageVerification` passes with the moved code
- [ ] Verification: 0 remaining references to the vendored `DefaultTrustedSocketFactory` in the contract and in `app/src`

## Next Steps

CR filed and queued for sprint planning. Run `/amp:create-stories --from-cr CR-009` when ready to schedule.

Read this with the batch decision: CR-009 is a precondition of Part B design (analysis condition 10, row 5 of Section 11), not a story to schedule on its own. The moved code, tests T2 to T6 and the coverage-gate check are part of the Part B migration stories; the contract text is applied in the combined revision with CR-007 and CR-008. Approval of the record is the condition; do not create a stand-alone repair story for it. The Scope Concern about stating SNI in the contract, and the blocklist wording, need the Solution Architect and product owner before the combined revision is written.

---

## Execution

### Changes Made

| File | Change | Layer |
|------|--------|-------|
| *(none yet)* | Pending the timing above | — |

### Verification Evidence

*(Pending)*

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| *(none yet)* | | | | |

---

## Closure

- **Root Cause Category:** Contract coupled to a library-owned class (a normative producer named by the vendored library's class and constructor), with a security property (SNI guard) carried only by code and tests and not by the contract.
- **Prevention Recommendation:** For each security property a boundary contract depends on (validation, host-name verification, SNI, pinning, hardening), state the property in the contract and name the test that proves it; name library classes only in a separate binding block. A property that lives only in code and tests is lost without a signal when the code changes owner.
- **Closed By:** (pending)
- **Date Closed:** (pending)
