---
artifact_type: review-security
story_id: U-001
verdict: PASS
agent: Developer
timestamp: "2026-06-02T16:00:00Z"
---

# Security Review: U-001

## Summary

This story is a build-toolchain configuration change only. No application source code is modified. Security review scope is limited to the manifest changes and lint baseline.

## Findings

### android:exported Receiver Values

The six intent-filtered receivers now have explicit `android:exported` declarations:

| Receiver | exported | Permission guard | Assessment |
|----------|----------|-----------------|------------|
| SmsBroadcastReceiver | false | `BROADCAST_SMS` | Correct — system-only, permission guard present, not exported to third parties |
| BootReceiver | false | none | Correct — BOOT_COMPLETED system broadcast only; not exported |
| PackageReplacedReceiver | false | none | Correct — MY_PACKAGE_REPLACED system broadcast only; not exported |
| BackupBroadcastReceiver | true | none | By design — intentional public API for Tasker/automation. Any app can trigger backup. This is the documented contract. No security regression (was already implicitly exported prior to this change). |
| .compat.SmsReceiver | true | `BROADCAST_SMS` | Correct — system SMS_DELIVER requires exported; permission guard present |
| .compat.MmsReceiver | true | `BROADCAST_WAP_PUSH` | Correct — system WAP_PUSH_DELIVER requires exported; permission guard present |

### BackupBroadcastReceiver No-Permission Export

The `BackupBroadcastReceiver` is intentionally exported without a permission guard. This allows any app to trigger a backup via `com.zegoggles.smssync.BACKUP` broadcast. This is the documented public automation API (Tasker integration). This is NOT a security regression introduced by this change — the receiver was previously in the same effective state (missing exported attr on AGP 4.x resulted in the same exported=true default behavior at runtime per Android pre-12 behavior for receivers with intent-filters). The `tools:ignore="ExportedReceiver"` suppression on the original manifest confirms this was a known, intentional state.

### Lint Baseline Security-Relevant Entries

The lint baseline captures two `UnspecifiedImmutableFlag` entries for PendingIntent construction sites:
- `AlarmManagerDriver.java:127` — `FLAG_UPDATE_CURRENT` only
- `ServiceBase.java:204` — `FLAG_UPDATE_CURRENT` only

These are PRE-EXISTING conditions at targetSdk 29 (not exploitable at current SDK level). Per DES-MODERNIZATION-001, both sites will receive `FLAG_IMMUTABLE` in S3 (co-landed with targetSdk 35). The baseline correctly captures these as known-acceptable at S1; they must be REMOVED from the baseline in S3 when the fix lands.

The `CustomX509TrustManager` entry in the baseline (`AllTrustedSocketFactory.java:42`) is a pre-existing trust-all-TLS vulnerability. This is owned by DES-MODERNIZATION-004 and is captured in the baseline as a known pre-existing condition. It is NOT introduced by this change.

### No New Security Issues Introduced

This commit introduces no new code, no new application functionality, and no new network interfaces. The security surface is unchanged by the AGP version bump.

## Verdict

PASS — No new security issues introduced. Receiver export values are correct and verified against DES-MODERNIZATION-001 §Manifest Conformance specification. Pre-existing security issues (PendingIntent mutability, TLS trust-all) are captured in baseline per design, with their fix stages documented.
