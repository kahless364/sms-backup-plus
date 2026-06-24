---
artifact_type: implementation-plan
story_id: U-056
verdict: PASS
agent: Solution Architect
timestamp: "2026-06-23"
---

# Implementation Plan: U-056

## Summary

Add a manifest-level `networkSecurityConfig` enforcing `cleartextTrafficPermitted="false"` (SE-004 defense-in-depth) and update the `third_party_integration` preference copy to surface the public-broadcast risk (SE-001 UX-only).

## Approach

### Part 1 — Network Security Config (AC-1, AC-2, AC-4, AC-5)

1. Create `app/src/main/res/xml/network_security_config.xml` with a single `<base-config cleartextTrafficPermitted="false" />`. No domain-specific overrides are needed because all app traffic is already TLS (IMAP/OAuth2).
2. Add `android:networkSecurityConfig="@xml/network_security_config"` to the `<application>` element in `AndroidManifest.xml`. Place it alongside the existing `android:fullBackupContent` attribute.
3. The attribute goes ONLY on `<application>` — not on any `<receiver>`, `<service>`, or `<activity>`. BackupBroadcastReceiver (lines 162-170) is NOT touched.

### Part 2 — Opt-In Copy (AC-3)

4. Update `ui_third_party_integration_desc` in `app/src/main/res/values/strings.xml` to explicitly mention "external apps" and the public broadcast risk (Tasker, MacroDroid, ADB, etc.). Label string is unchanged.

### Part 3 — Test (AC-5)

5. Add `NetworkSecurityConfigTest.java` (plain JUnit — no Robolectric needed) with three tests:
   - `networkSecurityConfigXml_shouldExistAndDisableCleartext` — parses the XML source file and asserts `cleartextTrafficPermitted="false"`.
   - `androidManifest_shouldReferenceNetworkSecurityConfigOnApplicationOnly` — parses the manifest and confirms the attribute is only on `<application>`.
   - `androidManifest_backupBroadcastReceiverShouldBeUnchanged` — confirms BackupBroadcastReceiver has `exported="true"` and no `android:permission`.

## Files to Change

| File | Action |
|------|--------|
| `app/src/main/res/xml/network_security_config.xml` | CREATE |
| `app/src/main/AndroidManifest.xml` | MODIFY — add `android:networkSecurityConfig` on `<application>` |
| `app/src/main/res/values/strings.xml` | MODIFY — update `ui_third_party_integration_desc` |
| `app/src/test/java/com/zegoggles/smssync/NetworkSecurityConfigTest.java` | CREATE |

## Risk

None. Cleartext was never used; the change adds enforcement without behavioral change. The copy update is cosmetic. BackupBroadcastReceiver is untouched.
