---
type: story
status: done
artifact_type: user-story
priority: low
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-056
title: Add a network security config (no cleartext) and clarify the 3rd-party-integration opt-in copy
pipeline: ''
domain: modernization
resolution: done
requirement_source: assessment:20260623-post-migration-assessment#SE-004
sprint: '000014'
---

# U-056: Add a network security config (no cleartext) and clarify the 3rd-party-integration opt-in copy

## Story
As a maintainer of the SMS Backup+ codebase, I want a manifest-level `networkSecurityConfig` enforcing `cleartextTrafficPermitted="false"` and the `third_party_integration` preference label updated to surface the external-trigger risk, so that the app's no-cleartext policy is enforced at the platform layer rather than only in transport code, and users understand what enabling the broadcast receiver does.

## Source
Derived from assessment 20260623-post-migration-assessment, findings SE-004 and SE-001 (UX copy only). See `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/security.md`.

## Acceptance Criteria
- [ ] AC-1: A new XML file `app/src/main/res/xml/network_security_config.xml` is created with `<network-security-config><base-config cleartextTrafficPermitted="false" /></network-security-config>`; `AndroidManifest.xml` references it via `android:networkSecurityConfig="@xml/network_security_config"` on the `<application>` element.
- [ ] AC-2: The `android:networkSecurityConfig` attribute is absent from any `<receiver>`, `<service>`, or `<activity>` element — it is set only at the `<application>` level; `BackupBroadcastReceiver` manifest entry (`AndroidManifest.xml:163-167`) is NOT modified in any way (no `exported`, `permission`, or config attribute added or changed).
- [ ] AC-3: The `strings.xml` entry for the `third_party_integration` preference label and/or summary is updated to explicitly state that enabling this setting allows any external app (Tasker, MacroDroid, ADB, etc.) to trigger a backup; the summary must mention "external apps" or equivalent plain-language phrasing, distinct from the current copy.
- [ ] AC-4: Build green: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` passes; lint does not produce a new `NetworkSecurityConfig` warning.
- [ ] AC-5: A unit or instrumentation assertion (or lint check) confirms that `cleartextTrafficPermitted` is `false` in the built APK's network security config; alternatively, `aapt2 dump xmltree` on the built APK shows the config attribute set.

## Affected Code
| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/AndroidManifest.xml` (`<application>` element) | No `android:networkSecurityConfig` attribute | Add `android:networkSecurityConfig="@xml/network_security_config"` |
| `app/src/main/res/xml/network_security_config.xml` | Does not exist | Create with `cleartextTrafficPermitted="false"` base config |
| `app/src/main/res/values/strings.xml` (third_party_integration entries) | Current label/summary does not surface external-app trigger risk | Update summary to state that external apps can trigger backups when this is enabled |

## Existing Behavior to Preserve
- `BackupBroadcastReceiver` (`AndroidManifest.xml:163-167`) must remain exactly as-is: `exported="true"`, no `android:permission` attribute — this is mandated by `CNTR-MODERNIZATION-005` (status: approved, HARD-PRESERVE). Do not add any permission, change `exported`, or reference the network security config from the receiver element.
- The `third_party_integration` preference default value of `false` (`Preferences.java:179`) must not change; this story changes only the displayed copy, not the default or the opt-in mechanism.
- All existing IMAP/OAuth2 connections use TLS already; the `cleartextTrafficPermitted="false"` config must not break any existing network call (confirm by building and running the existing test suite).

## Verification Steps
1. Build the debug APK: `./gradlew :app:assembleDebug`.
2. Run `aapt2 dump xmltree app/build/outputs/apk/debug/app-debug.apk --file res/xml/network_security_config.xml` — confirm `cleartextTrafficPermitted` is `false`.
3. Run `grep -r "networkSecurityConfig" app/src/main/AndroidManifest.xml` — confirm it appears only on the `<application>` element.
4. Run `grep -r "android:permission\|exported" app/src/main/AndroidManifest.xml | grep -i "backup.*receiver\|broadcastreceiver"` — confirm `BackupBroadcastReceiver` entry is unchanged from before this story.
5. Inspect the updated `strings.xml` entry for `third_party_integration` — confirm the summary text references "external apps" or equivalent.
6. Run `./gradlew :app:testDebugUnitTest :app:jacocoTestCoverageVerification` — all pass.

## Technical Context
- SE-004 root cause: the app enforces TLS-only via `TrustManagerFactory`/`StrictHostnameVerifier`/`PinnedCertificateSocketFactory` in transport code, but does not declare a manifest-level `networkSecurityConfig`. Adding one provides a defense-in-depth OS-layer guarantee that clears text traffic from any code path (including third-party libraries) is blocked, even if a bug bypasses the transport-layer enforcement.
- SE-001 UX-only re-scope: adding any `android:permission` or `exported="false"` to `BackupBroadcastReceiver` is explicitly PROHIBITED by `CNTR-MODERNIZATION-005`. The only contract-compatible improvement is clarifying the opt-in copy. This story implements that copy-only change.
- The `network_security_config.xml` base config format is the correct approach for `targetSdk 35`; domain-specific overrides are not needed because all app traffic already goes over IMAP/OAuth2 TLS.

## Notes
- Sprint C (Security Hardening). Low effort (S). Can be done in parallel with U-054 and U-055.
- Do NOT add any `android:permission` attribute to `BackupBroadcastReceiver` — this is a hard prohibition from `CNTR-MODERNIZATION-005` and would break the public broadcast API. This story scope is config + copy only.
