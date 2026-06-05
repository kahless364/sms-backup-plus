---
artifact_type: qa-results
story_id: U-045
verdict: PASS
agent: "QA Analyst"
timestamp: "2026-06-05"
ac_total: 4
ac_passed: 4
ac_failed: 0
tests_run: 4
tests_passed: 4
---

# QA Validation: U-045

## Verdict: PASS

Story U-045 (fix BUG-012) quiets the conscrypt SNI reflective fallback in the vendored
`DefaultTrustedSocketFactory`. Verified directly against the post-merge tree at HEAD: the
production change adds a public-API SNI path (`SSLParameters.setServerNames` /
`SNIHostName`) guarded at API 24+ and downgrades the reflective-fallback catch from
`Log.e(... , e)` (full stack trace) to `Log.d(... + e.getMessage())` (no stack trace, no
ERROR). All four ACs verifiable by static evidence are met; the production diff is confined
to the vendored module; the four unit tests confirm the no-throw and correct-SNI contracts.
The on-device "no `NoSuchMethodException` in logcat on API 37" portion of AC-1/AC-4 is the
orchestrator's separate post-merge device check and is recorded as DEFERRED (not failed).

## Acceptance Criteria Results

> **Rule**: Every AC marked PASS must cite at least one `file:line` reference in the Evidence column.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1: API 35+ IMAP connect no longer logs `NoSuchMethodException` ERROR; SNI set via public `SSLParameters.setServerNames(new SNIHostName(host))` (API 24+); degrades quietly (debug-only, no ERROR/stack trace) where unsettable | PASS (device check DEFERRED) | Public-API SNI path added and guarded API 24+: `DefaultTrustedSocketFactory.java:192` (`if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)`) → `setSniHost` calls `setSniViaSSLParameters` at `:197`, implemented at `:212-216` using `socket.getSSLParameters()` + `params.setServerNames(Collections.singletonList(new SNIHostName(hostname)))` + `socket.setSSLParameters(params)`. Reflective fallback catch downgraded from ERROR to DEBUG with no stack trace: `:221-227` — `Log.d(LOG_TAG, "SSLSocket#setHostname(String) not available (expected on API 35+): " + e.getMessage())` (`catch (Throwable e)`, no `e` argument passed to logger, so no stack trace). Tests confirm no-throw: `DefaultTrustedSocketFactorySniTest.java:94-122` (`reflectiveFallback_withAbsentSetHostnameMethod_doesNotThrow`) invokes the private `setHostnameViaReflection` via reflection against `FakeSSLSocketWithoutSetHostname` and asserts no exception (`:147-157` helper throws AssertionError on any throw). SNIHostName correctness: `DefaultTrustedSocketFactorySniTest.java:44-60`. NOTE: the on-device "no `NoSuchMethodException` in logcat, API 37" sub-clause is the orchestrator's separate post-merge device verification — DEFERRED, not failed. |
| AC-2: TLS still negotiates (API 21..35+); certificate validation and `PinnedCertificateSocketFactory` unaffected; no trust/SNI regression where SNI previously worked | PASS | TLS handshake/cipher/protocol hardening unchanged: `hardenSocket` at `DefaultTrustedSocketFactory.java:182-189` is untouched by the diff (diff only modifies `setSniHost`/`setSniViaSSLParameters`/`setHostnameViaReflection`). Cert/trust construction unchanged: `TrustManagerFactory.get(host, port)` at `:155` and KeyChain client-cert path `:157-163` untouched. Pinned-cert path is independent of the changed code: `PinnedCertificateSocketFactory.createSocket` (`PinnedCertificateSocketFactory.java:75-86`) does NOT call `setSniHost` at all — confirmed by Grep (no `setSniHost` reference in that file), so the SNI change cannot regress pinning. SNI still actively set where supported (API 24+ public-API branch added, `:192-197`), and the legacy API 17-23 and reflective branches are preserved (`:198-204`), so SNI is not silently dropped. |
| AC-3: Change confined to `:k9mail-vendored` `DefaultTrustedSocketFactory`; no `service.*`/transport API changes; vendored module build/tests green | PASS | Production diff for this story touches exactly one production file: `k9mail-vendored/src/main/java/com/fsck/k9/mail/ssl/DefaultTrustedSocketFactory.java` (+29 per `git diff --stat 370e88cf HEAD`); plus one new test in `:app` test sources (`DefaultTrustedSocketFactorySniTest.java`). `setSniHost` public signature unchanged (`:191`), so callers (`ImapConnection`, `WebDavSocketFactory.java:67`, `SmtpTransport`, `Pop3Store`) require no change — no transport/`service.*` modification. (The MainActivity.java / WindowInsetsUtil.java entries in `git diff --stat` belong to the disjoint parallel story U-046, per U-045 Notes, and are outside this story's diff scope.) |
| AC-4: Build green (assembleDebug, testDebugUnitTest, jacoco LINE ≥70%); a unit test covers the fallback path where the legacy reflective method is absent (no exception thrown/logged at error); on-device API 37 backup produces no `NoSuchMethodException` ERROR and succeeds | PASS (device check DEFERRED) | Build/test green verified separately by orchestrator (666 tests post-merge; implementation-log records full chain `assembleDebug testDebugUnitTest jacocoTestCoverageVerification` BUILD SUCCESSFUL). Fallback-with-absent-method unit coverage present and direct: `DefaultTrustedSocketFactorySniTest.java:94-122` and `:128-136` — `FakeSSLSocketWithoutSetHostname` (`:167-192`) has no `setHostname(String)` method, mirroring conscrypt `Java8EngineSocket`; both tests assert no exception propagates and `setSSLParameters` is never called in the fallback branch (`:135` `verify(mockSocket, never()).setSSLParameters(...)`). On-device API 37 logcat sub-clause is the orchestrator's separate post-merge device check — DEFERRED, not failed. |

## Integration Path Verification

> **Rule**: Any new function/class/component with no verified call path from a production entry point is a BLOCK finding.

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `setSniViaSSLParameters` (new, `DefaultTrustedSocketFactory.java:212`) | IMAP/SMTP/POP3/WebDav TLS connect | `ImapConnection.createSocket` (`ImapConnection.java:209/315`) → `DefaultTrustedSocketFactory.createSocket` (`:152`) → `setSniHost` (`:177`) → API 24+ branch (`:192`) → `setSniViaSSLParameters` (`:197` → `:212-216`). Also reached via `WebDavSocketFactory.java:67`, and transport `SmtpTransport.java:218/272` / `Pop3Store.java:324/346` → `createSocket` → `setSniHost`. | yes |
| `setHostnameViaReflection` (modified catch, `:218`) | same TLS connect path | `setSniHost` else branch (`:203`) → `setHostnameViaReflection` (`:218`) for pre-API-17 only; on modern devices the API 24+ branch is taken first so this is not normally hit. Reachable; behavior is no-throw with DEBUG log. | yes |

## Behavioral Contract Verification

> **Rule**: For each interface contract (CNTR-*) referenced by the story, every clause must match. Partial = FAIL.

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| `setSniHost(SSLSocketFactory, SSLSocket, String)` public signature/behavior | Same signature, same callers, SNI set on supported platforms | `DefaultTrustedSocketFactory.java:191` (signature unchanged); API 24+ sets SNI via SSLParameters (`:197`), API 17-23 via SSLCertificateSocketFactory preserved (`:198-201`), pre-17 reflective preserved (`:203`) | yes |
| Reflective fallback no-throw + non-alarming log | Must not throw; must not log at ERROR/stack trace when method absent | `:221-227` catch `Throwable` → `Log.d(..., e.getMessage())` (no `e` passed → no stack trace, DEBUG not ERROR); tests `DefaultTrustedSocketFactorySniTest.java:94-136` confirm no-throw and no `setSSLParameters` call | yes |
| TLS/cert/pinned-cert preservation | hardenSocket, TrustManagerFactory, KeyChain, PinnedCertificateSocketFactory unchanged | `:182-189`, `:155`, `:157-163` untouched; `PinnedCertificateSocketFactory.java:75-86` does not call `setSniHost` | yes |
| Story `integration_contracts` frontmatter | (empty — no CNTR-* referenced) | Story frontmatter `integration_contracts: []` | N/A |

## Requirement Scope Coverage

> **Rule**: For each referenced REQ-* document, list every scope item. Any uncovered scope = FAIL.

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| bug:BUG-012 (requirement_source) | Eliminate `NoSuchMethodException` ERROR log on reflective `setHostname` SNI shim on API 35+ | yes | `DefaultTrustedSocketFactory.java:221-227` (ERROR→DEBUG, no stack trace) + API 24+ public-API SNI path `:192-216` |
| Story "Existing Behavior to Preserve" (REQ-MODERNIZATION-002 / CNTR-MODERNIZATION-001 referenced narratively) | TLS handshake, hostname/cert verification, pinned-cert flow unchanged | yes | `hardenSocket` `:182-189`, `TrustManagerFactory.get` `:155`, `PinnedCertificateSocketFactory.java:75-86` independent of change |
| Story "Existing Behavior to Preserve" | SNI still actually set where it was working (no silent global drop) | yes | API 24+ branch `:192-197` actively sets SNI; legacy branches `:198-204` preserved |
| Story "Existing Behavior to Preserve" | No change to `K9MailTransport`/`service.*`; ACL boundary intact | yes | `git diff --stat` shows no transport/service production file changed for this story; `setSniHost` signature unchanged `:191` |

(Story frontmatter `requirements: []` and `design_docs: []` — no formal REQ-*/DES-* docs attached; scope derived from BUG-012 source and the story's explicit preservation list, all covered.)

## Capabilities Inventory Verification

| Capability | Status in Log | Verified in Code | File:Line | Result |
|------------|--------------|-----------------|-----------|--------|
| TLS SNI via SSLCertificateSocketFactory on API 17–23 | RETAINED | Yes | `DefaultTrustedSocketFactory.java:198-201` | PASS |
| TLS SNI via reflective `setHostname` on pre-API 17 | RETAINED | Yes | `:203` (dispatch) + `:218-228` (impl; only log level changed) | PASS |
| Cipher suite filtering (`hardenSocket`) | RETAINED | Yes | `:182-185` (unchanged) | PASS |
| Protocol filtering (`hardenSocket`) | RETAINED | Yes | `:186-188` (unchanged) | PASS |
| Client certificate / KeyChain support | RETAINED | Yes | `:157-163` (unchanged) | PASS |
| TrustManager construction via `TrustManagerFactory.get` | RETAINED | Yes | `:155` (unchanged) | PASS |

All RETAINED capabilities present and functional in the post-merge file. No INTENTIONALLY REMOVED items. No pre-existing capability silently dropped.

## Test Results

Four targeted unit tests added (`DefaultTrustedSocketFactorySniTest.java`):
1. `setSniViaSSLParameters_setsSSLParametersWithCorrectSNIHostName` (`:43-60`) — asserts `setSSLParameters` called with SSLParameters containing `SNIHostName("imap.gmail.com")`.
2. `setSniViaSSLParameters_setsCorrectHostname_forDifferentHost` (`:65-76`) — distinct hostname propagates correctly (no stale value).
3. `reflectiveFallback_withAbsentSetHostnameMethod_doesNotThrow` (`:93-122`) — private `setHostnameViaReflection` invoked against `FakeSSLSocketWithoutSetHostname`; no exception (helper `:147-157` converts any throw to AssertionError).
4. `reflectiveFallback_withAbsentSetHostnameMethod_doesNotCallSetSSLParameters` (`:128-136`) — fallback branch never calls `setSSLParameters` (`never()` verify).

Build/test execution verified separately by the orchestrator (666 tests post-merge, full Gradle chain green). QA verified test source content directly on disk.

## Regression Results

No regression. The production diff is isolated to `setSniHost` dispatch, the new `setSniViaSSLParameters` helper, and the `setHostnameViaReflection` catch log level. `setSniHost` signature is unchanged, so all callers (`ImapConnection`, `WebDavSocketFactory`, `SmtpTransport`, `Pop3Store`) are unaffected. Cert validation, cipher/protocol hardening, client-cert/KeyChain, and the independent `PinnedCertificateSocketFactory` are untouched.

## Deferred Items

- AC-1 / AC-4 on-device sub-clause: "a backup on API 37 produces no `NoSuchMethodException` ERROR in logcat and still succeeds." This is the orchestrator's separate post-merge device verification (build + device already noted as verified separately). DEFERRED — explicitly tracked here, not failed.

## Issues Found

None. No scope gaps, no partial contracts, no unwired code, no untracked deferrals.

## Phase Completion Report
---
story_id: "U-045"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-010/U-045/qa-results.md"
story_status: "validated"
current_build_phase: "validation"
ac_passed: 4
ac_total: 4
errors: []
---
