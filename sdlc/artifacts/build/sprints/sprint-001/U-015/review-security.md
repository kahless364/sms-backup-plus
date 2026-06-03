---
artifact_type: security-review
story_id: "U-015"
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# Security Review: U-015

## Summary

No new security issues introduced. The workers use the same TLS trust policy
(PinnedCertStore + TlsTrustPolicy) as the existing ServiceBase.getBackupImapStore(),
ensuring the pinned-cert flow from U-008/U-009 is preserved.

## Findings

### TLS trust policy

- [PASS] buildImapStore() mirrors ServiceBase.getBackupImapStore() exactly: resolves
  TlsTrustPolicy from PinnedCertStore before constructing the socket factory; never
  uses DefaultTrustedSocketFactory unconditionally
- [PASS] TlsTrustPolicy.PINNED_CERTIFICATE path uses PinnedCertificateSocketFactory
- [PASS] No trust-all downgrade (AllTrustedSocketFactory removed in U-007)

### Credentials

- [PASS] No hardcoded secrets or credentials in worker files
- [PASS] OAuth2 token refresh uses existing TokenRefresher (same as BackupTask/RestoreTask)

### Content provider access

- [PASS] smsExists() and callLogExists() use parameterized queries (? placeholders) — no
  SQL injection risk
- [PASS] SMS insert gated on type filter (INBOX/SENT only) — avoids re-sending

### Otto removed (no event bus security concern)

- [PASS] No Otto App.post — progress data flows through WorkManager setProgress/WorkInfo,
  not a broadcast bus accessible to third parties
