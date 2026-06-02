---
artifact_type: security-review
story_id: U-003
verdict: PASS
agent: Developer
timestamp: "2026-06-02T17:00:00Z"
---

# Security Review: U-003 — SDK 35 Uplift

## Security Impact Assessment

### PendingIntent FLAG_IMMUTABLE (POSITIVE)

Both PendingIntent construction sites now carry `FLAG_IMMUTABLE`:

- `AlarmManagerDriver.java:127` — `PendingIntent.getService(ctx, 0, intent, FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)`
- `ServiceBase.java:204` — `PendingIntent.getActivity(..., FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE)`

Both are send-only triggers that carry no mutable payload (a backup-type action string; a MainActivity navigation intent). `FLAG_IMMUTABLE` is semantically correct AND a security improvement: it prevents a holder of the PendingIntent from modifying the Intent's extras or action before delivery, closing a PendingIntent-tampering attack surface.

### POST_NOTIFICATIONS (NEUTRAL — standard API)

The `requestPostNotificationsIfNeeded()` method:
- Requests only `POST_NOTIFICATIONS`, which is the standard runtime permission for notification posting
- Is gated on `Build.VERSION_CODES.TIRAMISU` — no-ops on API ≤ 32
- Does not grant or consume any sensitive capability beyond notification display
- Does not introduce new activity for result, implicit broadcast, or data leakage surface

### FOREGROUND_SERVICE_DATA_SYNC (NEUTRAL — required for API 34 compliance)

Adding `android:foregroundServiceType="dataSync"` to both services:
- Satisfies the mandatory platform requirement for starting a foreground service on API 34+
- Does NOT expand what the service can do — it constrains it (the OS can validate the declared type matches the operation)
- The `FOREGROUND_SERVICE_DATA_SYNC` permission is a normal protection-level permission

### JCenter Removal (POSITIVE — supply chain security)

Removing `jcenter()` and `https://jcenter.bintray.com`:
- Eliminates dependency on a sunset CDN that could theoretically serve tampered artifacts
- Build now resolves from `google()`, `mavenCentral()`, `jitpack.io`, and the scijava mirror only
- `mavenLocal()` is retained for the firebase-jobdispatcher local M2 build — this is a developer machine artifact, not a CDN dependency

### BackupBroadcastReceiver (NO CHANGE — CNTR-005 preserved)

`android:exported="true"` and no `android:permission` on BackupBroadcastReceiver is an intentional design decision per CNTR-MODERNIZATION-005. The user-gate (`isAllow3rdPartyIntegration()`, default false) provides the primary access control. Any sender can trigger the broadcast, but only the opt-in user can have it take effect. This is the documented, published contract.

## No Regressions Found

All security-relevant changes are positive or neutral. No new attack surfaces, no new permissions granted to the app beyond what is required for API conformance.
