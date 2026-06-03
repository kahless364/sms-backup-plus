---
artifact_type: security-review
story_id: "U-017"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-03"
---

# Security Review: U-017

## Summary

This story removes a supply-chain dependency (`firebase-jobdispatcher:0.8.6` hosted on `maven.scijava.org`) and replaces it with an androidx.work dependency already in the dependency tree. The security posture improves.

## Supply Chain

- **maven.scijava.org removed**: This was the only non-standard repository serving `firebase-jobdispatcher:0.8.6`. The artifact was a 2018 library with no recent security updates and hosted on an academic mirror. Removing it eliminates the supply-chain exposure.
- **firebase-jobdispatcher:0.8.6 removed**: Unmaintained 2018 Firebase library. No longer in the classpath.
- **androidx.work:work-runtime-ktx:2.9.1 retained**: Google-published, maintained, SDK 35-compatible.

## Broadcast Receiver Security

- `BackupBroadcastReceiver` remains `android:exported="true"` (required for third-party callers per CNTR-MODERNIZATION-005). No `android:permission` added (correct — adding a permission would break existing Tasker/automation integrations without any security benefit, since the user-controlled `third_party_integration` gate already governs the behavior).
- `SmsJobService` removed from manifest — no longer exported under `com.firebase.jobdispatcher.ACTION_EXECUTE`. This was a de-facto internal intent-filter but its removal is a minor attack surface reduction.

## No Credential Changes

No credentials, secrets, or authentication flows are modified. The scheduler cutover is purely structural.

## Verdict: PASS

Supply-chain risk of the `maven.scijava.org` mirror eliminated. No new security concerns introduced.
