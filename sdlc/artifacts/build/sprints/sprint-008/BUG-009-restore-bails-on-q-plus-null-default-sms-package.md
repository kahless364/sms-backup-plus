---
type: bug
status: done
artifact_type: bug
severity: high
priority: high
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-041
  - U-009
  - U-021
platforms: []
tags: []
id: BUG-009
title: Restore bails on Android Q+ when legacy getDefaultSmsPackage() returns null
domain: build
origin: code-review
resolved_by: U-041
sprint: '000008'
---

# BUG-009: Restore bails on Q+ when legacy getDefaultSmsPackage() returns null

## Description
On Android Q+ (API 29+, the entire supported range at targetSdk 35), `MainActivity.startRestore()` captures the current default SMS app via the **legacy** `Telephony.Sms.getDefaultSmsPackage()` before requesting the SMS role. When that returns null/empty (observed on the emulator: `RoleManager` reports Google Messages holds `ROLE_SMS`, but `settings secure sms_default_application` and `getDefaultSmsPackage()` are null), the code takes the "no default package — running on tablet?" branch and shows a Toast, **never requesting the SMS role** → restore cannot become the default SMS app → restore cannot write SMS and does not proceed.

## Steps to Reproduce
1. On an emulator/device where `getDefaultSmsPackage()` returns null (RoleManager-managed default, legacy secure setting unset), with SMS Backup+ NOT the default SMS app.
2. Tap Restore.
3. Observe logcat: `restore` → `default SMS package: null`; then nothing — no role-request dialog, no RestoreWorker. (User sees the "no default package" toast.)

## Expected Behavior
On Q+, restore requests the SMS role via `RoleManager.createRequestRoleIntent(ROLE_SMS)` regardless of the legacy `getDefaultSmsPackage()` value; after restore, the role is released (SmsReceiver disabled). The legacy package capture is only needed on pre-Q to switch back.

## Actual Behavior
`startRestore()` gates the entire flow on `getDefaultSmsPackage()` being non-empty; null → bails with `error_no_sms_default_package` toast; role never requested; restore never runs.

## Evidence
- `activity/MainActivity.java:411-435` (`startRestore`): line 418 `Sms.getDefaultSmsPackage(this)`; line 420 `if (!TextUtils.isEmpty(defaultSmsPackage))` gate; line 427-429 the "tablet?" bail.
- `activity/MainActivity.java:469-481` (`requestDefaultSmsPackageChange`): already uses `RoleManager.createRequestRoleIntent(ROLE_SMS)` on Q+ — but is only reached when defaultSmsPackage is non-empty.
- Live logcat (emulator-5554, API 37): `restore` / `default SMS package: null`; role-holder query: `cmd role get-role-holders android.app.role.SMS` = com.google.android.apps.messaging; `settings get secure sms_default_application` = null.

## Acceptance Criteria
- [ ] AC-1: On Q+, tapping Restore requests the SMS role (RoleManager) even when `getDefaultSmsPackage()` is null/empty; the role-request dialog appears.
- [ ] AC-2: After restore completes, the role is released (SmsReceiver disabled / default restored) as today; pre-Q behavior (capture + ACTION_CHANGE_DEFAULT switch-back) preserved.
- [ ] AC-3: The "no default package — running on tablet?" toast only fires in genuinely unsupported cases (e.g., device without telephony), not merely because the legacy API returned null on a phone with a RoleManager-managed default.
- [ ] AC-4: On-device: restore proceeds to write SMS after the user grants the role; build green.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `activity/MainActivity.java:411-435` | gates restore on legacy getDefaultSmsPackage() non-empty | on Q+, proceed to RoleManager request regardless; only capture legacy package pre-Q |

## Existing Behavior to Preserve
- Pre-Q: capture current default + ACTION_CHANGE_DEFAULT switch-back (restoreDefaultSmsProvider).
- Q+: release role after restore (SmsReceiver.disable). The U-009 restore-default-provider flow and the role round-trip.

## Verification Steps
1. On a Q+ device/emulator with getDefaultSmsPackage()==null, tap Restore → role dialog appears → grant → restore runs.
2. Confirm the previous default is restored / role released afterward.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- Discovered during live testing. The legacy `getDefaultSmsPackage()` is unreliable on RoleManager-managed devices; on Q+ the role system is authoritative. Fix decouples the Q+ role request from the legacy capture. Likely present in pre-modernization code; MainActivity was restructured in U-021 but this gating logic predates/persisted.

## Supporting Documentation
- Live diagnosis 2026-06-05; consolidated review context.

## Notes
Logged from live testing; fix deferred per user. High — blocks restore on Q+ when the legacy default-SMS API returns null.
