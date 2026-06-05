---
artifact_type: qa-results
story_id: U-047
verdict: PASS
agent: "QA Analyst"
timestamp: "2026-06-05T16:20:00Z"
ac_total: 4
ac_passed: 4
ac_failed: 0
tests_run: 6
tests_passed: 6
---

# QA Validation: U-047 — Guard SNIHostName against IP-literal/empty IMAP host (fix BUG-015)

## Verdict: PASS

Verified against the real integrated main tree at `C:/Code/Android/sms-backup-plus`. Read the
current on-disk state of `DefaultTrustedSocketFactory.java` (lines 191-254) and the full
`DefaultTrustedSocketFactorySniTest.java` (296 lines), traced the production call chain
`createTrustedSocket → setSniHost → setSniViaSSLParameters`, and ran the SNI test class via
`:app:testDebugUnitTest --tests ...DefaultTrustedSocketFactorySniTest` (BUILD SUCCESSFUL, exit 0).
All four ACs are met with file:line evidence. The change is confined to the vendored module and
test (git diff stat: only `DefaultTrustedSocketFactory.java`, the SNI test, plus build artifacts);
no `app/src/main` or `service.*` files changed. The IP path is unit-tested only — expected per the
story (no IP IMAP server for a device test); the DNS path was on-device validated in U-045.

## Acceptance Criteria Results

> Rule: Every AC marked PASS cites at least one `file:line` reference.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1 | PASS | `setSniViaSSLParameters` now guards before constructing `SNIHostName`. `DefaultTrustedSocketFactory.java:238-243` returns early (skips SNI, DEBUG log) when `isIpOrBlankHostname` is true; `:216-226` detects null/blank (`hostname.trim().isEmpty()`), IPv6 (`indexOf(':') >= 0`), and IPv4 (regex `^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$`). `:248-253` adds a try/catch safety net so any residual `IllegalArgumentException` from `SNIHostName` is swallowed (DEBUG) rather than thrown. Verified by no-throw tests for IPv4/IPv6/empty/blank at `DefaultTrustedSocketFactorySniTest.java:171-239`, each asserting `setSSLParameters` is `never()` called. The path that throws (calling code in `setSniHost`) still proceeds: `setSniViaSSLParameters` returns normally so the TLS connection continues. |
| AC-2 | PASS | DNS host behavior unchanged: `DefaultTrustedSocketFactory.java:244-247` sets `params.setServerNames(Collections.singletonList(new SNIHostName(hostname)))` then `socket.setSSLParameters(params)` — identical to pre-fix logic, now only reached for valid DNS names. Confirmed by `setSniViaSSLParameters_dnsHostname_setsSNI` (`...SniTest.java:151-164`) and the retained Gmail test `setSniViaSSLParameters_setsSSLParametersWithCorrectSNIHostName` (`...SniTest.java:47-64`) which asserts a single `SNIHostName` entry equal to `imap.gmail.com`. Cert validation / hardening (`hardenSocket` `:182-189`) and the SDK-version branching in `setSniHost` (`:191-205`) are untouched by the diff. |
| AC-3 | PASS | Six tests added for U-047 (git diff confirms exactly these new `@Test` methods): (a) DNS → SNI set `...SniTest.java:151-164`; (b) IPv4 literal `192.168.1.10` no throw + `setSSLParameters` never called `:171-181`; (c) IPv6 compressed `::1` `:187-196` and full `fe80::1` `:202-210`; (d) empty `""` `:216-225` and blank `"   "` `:231-239`. Each negative case asserts `verify(mockSocket, never()).setSSLParameters(any())` and the DNS case asserts the `SNIHostName` ascii name — they assert the correct behavior, not just no-throw. All 6 executed in a BUILD SUCCESSFUL run. |
| AC-4 | PASS | Change confined to vendored module: `git diff --stat 402a15aa HEAD` lists only `k9mail-vendored/.../DefaultTrustedSocketFactory.java`, `app/src/test/.../DefaultTrustedSocketFactorySniTest.java`, plus `plan.md`/`implementation-log.md`. Filtered diff over `app/src/main` and `**/service` is empty — no transport/`service.*` change. Build green: `:app:testDebugUnitTest --tests DefaultTrustedSocketFactorySniTest` → BUILD SUCCESSFUL, exit 0 (parent build reports 672 tests). BUG-012 preserved: both new log calls in the SNI path use `Log.d` (`:241`, `:251`); the reflective fallback remains `Log.d` (`:263`). The only `Log.e` in the file (`:109`) is in the unrelated cipher-enumeration path and was not touched. |

## Integration Path Verification

> Rule: Any new function with no verified call path from a production entry point is a finding.

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `setSniViaSSLParameters(SSLSocket, String)` | `TrustedSocketFactory.createTrustedSocket` (the `createSocket` override, `DefaultTrustedSocketFactory.java:177`) | `createSocket → setSniHost (:177→:191) → setSniViaSSLParameters (:197→:238)` for API 24+ (the modern/default path) | yes |
| `isIpOrBlankHostname(String)` (new, package-private static) | same as above, via `setSniViaSSLParameters` | `setSniViaSSLParameters:239` calls `isIpOrBlankHostname` as the first guard | yes — reachable, not dead code; exercised by 5 negative + 1 positive test |

## Behavioral Contract Verification

> Rule: For each interface contract referenced, every clause must match. Partial is FAIL.

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| SNI-setting contract (RFC 6066 §3 — no SNI for IP literals/empty) | DNS host → SNI set | `DefaultTrustedSocketFactory.java:244-247` | yes |
| same | IPv4 literal → SNI skipped, no throw | `:225` regex + `:239-243` early return | yes |
| same | IPv6 literal → SNI skipped, no throw | `:221-223` colon check + `:239-243` | yes |
| same | empty/blank host → SNI skipped, no throw | `:217-219` null/trim-empty check + `:239-243` | yes |
| same | malformed-but-not-IP host → no crash | `:248-253` try/catch safety net (DEBUG, no rethrow) | yes |
| BUG-012 log-noise contract | SNI/reflective paths log at DEBUG, never ERROR | `:241`, `:251`, `:263` all `Log.d`; no `Log.e` in SNI path | yes |
| Gmail/TLS-hardening preservation | hardening, cert path, SDK branching unchanged | `hardenSocket :182-189`, `setSniHost :191-205` not in diff | yes |

## Requirement Scope Coverage

> Rule: For each referenced REQ/DES doc, list scope items. Missing scope = FAIL.

Story frontmatter lists no `requirements`/`design_docs`; source is `bug:BUG-015`. Referenced
supporting context: RFC 6066 §3 and the existing-behavior-to-preserve list (REQ-MODERNIZATION-002 /
CNTR-MODERNIZATION-001 TLS hardening — preserve only).

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| BUG-015 | No `IllegalArgumentException` for IP-literal/empty host; SNI skipped, TLS proceeds | yes | `DefaultTrustedSocketFactory.java:216-226`, `:238-253`; tests `...SniTest.java:171-239` |
| RFC 6066 §3 | SNI not sent for IP literals (IPv4 + IPv6) or blank | yes | `:221-225` (IPv6 colon, IPv4 regex), `:217-219` (blank) |
| REQ-MODERNIZATION-002 / CNTR-MODERNIZATION-001 | TLS hardening / cert validation / pinned-cert unchanged (preserve) | yes | `hardenSocket :182-189` and `setSniHost :191-205` untouched (not in diff); no `app/src/main`/`service` change |

## Test Results

Ran `./gradlew :app:testDebugUnitTest --tests "com.zegoggles.smssync.mail.ssl.DefaultTrustedSocketFactorySniTest" --console=plain` → `:app:testDebugUnitTest` executed, BUILD SUCCESSFUL in 1m13s, EXIT=0. With `--tests` filtering to this class, any failing test would fail the task; it did not. The class contains 8 `@Test` methods total, of which 6 are the U-047 additions (DNS, IPv4, IPv6 compressed, IPv6 full, empty, blank) plus 2 retained U-045 SNI/reflective-fallback tests. Per-class JUnit XML is not emitted in this project's binary results layout; the BUILD SUCCESSFUL outcome with exit 0 is the authoritative pass signal. Parent build separately verified at 672 tests.

## Regression Results

- DNS / Gmail SNI path: unchanged logic (`:244-247`) and still covered by the retained `imap.gmail.com` test (`...SniTest.java:47-64`) — PASS.
- Reflective fallback (BUG-012): still `Log.d`, still no-throw — retained tests at `...SniTest.java:98-140` unchanged and still present — PASS.
- No `app/src/main` or `service.*` change (filtered diff empty) — no transport regression surface introduced.

## Phase Completion Report
---
story_id: "U-047"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-011/U-047/qa-results.md"
story_status: "validated"
current_build_phase: "validation"
ac_passed: 4
ac_total: 4
errors: []
---
