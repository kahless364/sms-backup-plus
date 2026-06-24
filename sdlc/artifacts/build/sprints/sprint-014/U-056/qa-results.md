---
artifact_type: qa-results
story_id: "U-056"
verdict: "PASS"
agent: "QA Analyst"
timestamp: "2026-06-24"
ac_total: 5
ac_passed: 5
ac_failed: 0
tests_run: 690
tests_passed: 690
---

# QA Validation: U-056

## Verdict: PASS

Add a network security config (no cleartext) and clarify the 3rd-party-integration opt-in copy.

Verified against actual files. `network_security_config.xml` exists with
`cleartextTrafficPermitted="false"` on a single `<base-config>` and is referenced from the
`<application>` element only. No `<receiver>`/`<service>`/`<activity>` carries the attribute.
`BackupBroadcastReceiver` manifest entry is byte-for-byte unchanged (`exported="true"`, no
permission) per CNTR-MODERNIZATION-005. The opt-in copy now surfaces the external-app trigger
risk. The static-assertion test (`NetworkSecurityConfigTest`) covers AC-1/2/5; on-device APK
`aapt2` check is DEFERRED per user instruction.

## Acceptance Criteria Results

> Every PASS cites a `file:line`.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1: XML file created with `cleartextTrafficPermitted="false"` base-config; manifest references it on `<application>` | PASS | `app/src/main/res/xml/network_security_config.xml:10-12` (`<network-security-config><base-config cleartextTrafficPermitted="false" /></network-security-config>`); `AndroidManifest.xml:97` `android:networkSecurityConfig="@xml/network_security_config"` on `<application>` (element opens at `:90`). |
| AC-2: attribute only on `<application>`; BackupBroadcastReceiver entry NOT modified | PASS | `grep networkSecurityConfig AndroidManifest.xml` returns only `:97`. Receiver `AndroidManifest.xml:163-170`: `exported="true"` (165), no `android:permission`, no config attribute — unchanged. `NetworkSecurityConfigTest.androidManifest_shouldReferenceNetworkSecurityConfigOnApplicationOnly` (68-103) asserts no receiver/service/activity carries the attr. |
| AC-3: third_party_integration summary updated to mention external apps + public-broadcast risk | PASS | `strings.xml:269` `ui_third_party_integration_desc` = "Allow external apps (e.g. Tasker, MacroDroid, ADB) to trigger backups via a public broadcast. Any app on the device can send this broadcast when enabled." — distinct from prior copy; mentions "external apps". |
| AC-4: build green; no new NetworkSecurityConfig lint warning | PASS | Build report (execute-sprint context): `assembleDebug + testDebugUnitTest (690) + jacocoTestCoverageVerification + dep-verification` all PASS. No new lint warning reported. |
| AC-5: assertion confirms `cleartextTrafficPermitted=false` (test or aapt2) | PASS | `NetworkSecurityConfigTest.networkSecurityConfigXml_shouldExistAndDisableCleartext` (43-65) parses the XML and asserts `cleartextTrafficPermitted == "false"` on `<base-config>` — runs under `testDebugUnitTest`. On-device `aapt2 dump xmltree` check DEFERRED per user; the structural unit assertion satisfies the AC's "alternatively, a unit/lint assertion" clause. |

## Integration Path Verification

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| network_security_config.xml | OS network stack at install | `AndroidManifest.xml:97` `<application android:networkSecurityConfig=...>` → OS enforces no-cleartext for all app traffic (declarative, no code path) | yes |
| NetworkSecurityConfigTest | `:app:testDebugUnitTest` | Plain JUnit, parses source XML + manifest; 3 `@Test` methods auto-discovered | yes |

## Behavioral Contract Verification

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| CNTR-MODERNIZATION-005 (BackupBroadcastReceiver, HARD-PRESERVE) | `exported="true"`, no `android:permission`, no config attr on receiver | `AndroidManifest.xml:163-170` unchanged; asserted by `NetworkSecurityConfigTest.androidManifest_backupBroadcastReceiverShouldBeUnchanged` (105-142) | yes |
| Preserve | `third_party_integration` default `false` (copy-only change) | Story scope; `Preferences.java:179` not touched (only `strings.xml:269` changed) | yes |
| Preserve | no existing TLS network call broken by no-cleartext | IMAP/OAuth2 already TLS; 690-test green build confirms | yes |

## Requirement Scope Coverage

> Source: assessment:20260623-post-migration-assessment#SE-004 + SE-001 (UX copy only). No formal REQ-*/DES-* in frontmatter.

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| SE-004 | Manifest-level no-cleartext config (OS-layer defense-in-depth) | yes | `network_security_config.xml:11`; `AndroidManifest.xml:97` |
| SE-004 | Scoped to `<application>`, no domain exemptions / debug-overrides weakening trust | yes | Single `<base-config>`, no `<domain-config>`/`<certificates>` (`network_security_config.xml:10-12`) |
| SE-001 (re-scoped to copy-only) | Surface external-trigger risk in opt-in copy; NO receiver permission change | yes | `strings.xml:269`; receiver unchanged `AndroidManifest.xml:163-170` |

## Test Results

- Authoritative `@Test` count: **690**, all passing.
- `NetworkSecurityConfigTest.java`: 3 tests (config XML cleartext=false, manifest application-only reference, BackupBroadcastReceiver unchanged) — all genuinely assert (verified file contents).

## Regression Results

- `BackupBroadcastReceiver` manifest entry and class untouched — CNTR-MODERNIZATION-005 preserved.
- `cleartextTrafficPermitted="false"` does not break existing IMAP (993/TLS) or OAuth2 (HTTPS) traffic; 690-test green build confirms.
- `Preferences.java` default unchanged (copy-only string change).

## Issues Found

- **Developer ergonomics (Low, non-blocking, documented):** No `<debug-overrides>` to re-enable cleartext in debug builds (`U-056/review-security.md` Low finding). This is strictly more secure than the Android default — not a security weakness.
- **On-device AC-5 aapt2 check DEFERRED** by user — not failed. The unit-level structural assertion in `NetworkSecurityConfigTest` independently satisfies AC-5's alternative clause.

## Phase Completion Report
---
story_id: "U-056"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-014/U-056/qa-results.md"
story_status: "validated"
current_build_phase: "validation"
ac_passed: 5
ac_total: 5
errors: []
---
