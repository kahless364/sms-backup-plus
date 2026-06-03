---
artifact_type: review-security
story_id: "U-016"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-03"
findings: 1
blockers: 0
---

# Security Review: U-016 — Durable Restore Checkpoint

## Summary

Security surface is narrow: the checkpoint store persists an integer index (cursor offset) to
SharedPreferences. No secrets, no user content, no PII in the checkpoint value. No new permissions
required. No network access added.

## Findings

### Finding 1 — Advisory: SharedPreferences not encrypted

**File:** `SharedPreferencesCheckpointStore.kt`  
**Severity:** Advisory (not a defect)  
**Finding:** The checkpoint store uses unencrypted `SharedPreferences`. The persisted value is an
integer cursor index (0, 1, 2, ...) — it contains no PII, no credentials, and no message content.
There is no threat model justification for encrypting this value. The SMS content itself is not
stored; only the count of successfully-processed items.  
**Action:** None required. If the threat model changes (e.g., the checkpoint is extended to store
partial message content), this should be revisited.

## Checklist

- [x] No credentials or secrets written to checkpoint store
- [x] No PII written to checkpoint store (index is an integer)
- [x] No new permissions added to AndroidManifest
- [x] No new network operations added
- [x] `SharedPreferences.MODE_PRIVATE` used (not world-readable)
- [x] Fault-injection seam (`RestoreInsertInterceptor`) is test-only (no production flag/static)
- [x] `SimulatedCrashException` cannot be triggered from production paths
- [x] No hard-coded secrets, API keys, or tokens
