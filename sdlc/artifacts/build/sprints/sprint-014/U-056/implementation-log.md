---
artifact_type: implementation-log
story_id: U-056
verdict: PASS
agent: Developer
timestamp: "2026-06-23"
files_changed: 2
files_created: 2
tests_added: 3
tests_passing: 669
---

# Implementation Log: U-056

## Summary

Implemented SE-004 (no-cleartext network security config at the manifest layer) and SE-001 UX copy update (third-party integration opt-in now surfaces the public-broadcast risk). BackupBroadcastReceiver is untouched per CNTR-MODERNIZATION-005. Build and all tests pass.

## Files Created

### `app/src/main/res/xml/network_security_config.xml`

New file. Contains:
```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
</network-security-config>
```
No domain-specific overrides needed — all app traffic is already TLS (IMAP over port 993, OAuth2 over HTTPS). This provides OS-layer defence-in-depth beyond the existing transport enforcement in `PinnedCertificateSocketFactory`.

### `app/src/test/java/com/zegoggles/smssync/NetworkSecurityConfigTest.java`

Plain JUnit (no Robolectric) with 3 `@Test` methods:
- `networkSecurityConfigXml_shouldExistAndDisableCleartext` — parses source XML, asserts `cleartextTrafficPermitted="false"` on `<base-config>`.
- `androidManifest_shouldReferenceNetworkSecurityConfigOnApplicationOnly` — parses manifest, confirms attribute on `<application>` only, not on any `<receiver>`/`<service>`/`<activity>`.
- `androidManifest_backupBroadcastReceiverShouldBeUnchanged` — confirms `exported="true"` and no `android:permission` on BackupBroadcastReceiver (CNTR-MODERNIZATION-005 guard).

## Files Modified

### `app/src/main/AndroidManifest.xml`

Added `android:networkSecurityConfig="@xml/network_security_config"` to the `<application>` element (line 97), alongside the existing `android:fullBackupContent`. Attribute appears on `<application>` only. BackupBroadcastReceiver (lines 163-170) is unchanged — `exported="true"`, no `android:permission`, no config attribute.

### `app/src/main/res/values/strings.xml`

Updated `ui_third_party_integration_desc` (line 263) from:
> "Allow other apps to trigger backups via broadcast intents"

to:
> "Allow external apps (e.g. Tasker, MacroDroid, ADB) to trigger backups via a public broadcast. Any app on the device can send this broadcast when enabled."

`ui_third_party_integration_label` unchanged. Default value `false` (Preferences.java:179) unchanged.

## Acceptance Criteria Verification

| AC | Status | Evidence |
|----|--------|----------|
| AC-1: XML created with `cleartextTrafficPermitted="false"`, manifest references it on `<application>` | PASS | File created; manifest line 97; `aapt2 dump xmltree` shows `A: cleartextTrafficPermitted=false` |
| AC-2: Attribute only on `<application>`; BackupBroadcastReceiver unchanged | PASS | `grep -n "networkSecurityConfig"` returns only line 97; receiver lines 163-170 untouched |
| AC-3: Summary text updated to mention "external apps" and public broadcast risk | PASS | strings.xml line 263 updated |
| AC-4: Build green | PASS | `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` — BUILD SUCCESSFUL |
| AC-5: `cleartextTrafficPermitted` confirmed false in built APK | PASS | `aapt2 dump xmltree app-debug.apk --file res/xml/network_security_config.xml` → `A: cleartextTrafficPermitted=false`; also confirmed by `NetworkSecurityConfigTest` |

## Cleartext Usage Audit

Confirmed no legitimate cleartext usage:
- IMAP: port 993 TLS (BackupImapStore / K9MailTransport — all existing tests pass)
- OAuth2: HTTPS only (Google OAuth2 endpoints)
- No HTTP endpoints, no STARTTLS downgrade paths needing cleartext at the OS layer
- Existing `PinnedCertificateSocketFactory` / `TrustManagerFactory` enforce TLS at the transport level; the new config adds OS-layer enforcement

## Contract Adherence

No `integration_contracts` listed in the story frontmatter. Adherence to CNTR-MODERNIZATION-005 (hard-preserve BackupBroadcastReceiver public broadcast contract) is confirmed:
- `AndroidManifest.xml:163-170` — receiver unchanged
- `NetworkSecurityConfigTest.androidManifest_backupBroadcastReceiverShouldBeUnchanged` asserts `exported="true"` and absence of `android:permission`

## Build Result

```
BUILD SUCCESSFUL in 4m 18s
69 actionable tasks
```

## Test Results

Authoritative @Test count: **669** (baseline 666 + 3 new tests in NetworkSecurityConfigTest.java)

All tests passing.

## Integration Path

`network_security_config.xml` is referenced from `AndroidManifest.xml <application>` element. Android platform reads this at install time and enforces it for all network calls from the app process. No code changes are needed in the app itself — the config is declarative and enforced by the OS networking stack (HTTPS, IMAPS, etc.).
