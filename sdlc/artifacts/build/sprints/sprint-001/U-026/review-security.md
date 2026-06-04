---
artifact_type: review-security
story_id: "U-026"
verdict: PASS
agent: Developer
timestamp: "2026-06-04"
---

# Security Review: U-026 — Engine Rewire to MailTransport

## Summary

No new security surface introduced. U-026 is a pure ACL refactor — it moves existing code
from k-9 types to app-owned types without adding new capabilities or changing trust boundaries.

## Security Assessment

| Area | Finding | Risk |
|------|---------|------|
| k-9 type confinement | `MailMessageHandle.message` and `BackupFolderHandle.folder` are package-private; `service.*` engine cannot access raw k-9 objects | NONE — boundary enforced |
| Credential handling | `AuthPreferences.getStoreUri()` is called from the same code paths as before; no new credential access | NONE |
| TLS policy | `MailTransportConfig.tlsPolicy` + pinned cert handling unchanged; `buildSocketFactory()` maps to same factory types | NONE |
| `BinaryTempFileBody.setTempDirectory()` relocation | Now in K9MailTransport constructor; same timing (before first body fetch). No permission or privilege change | NONE |
| Hilt `MailTransportFactory` singleton | Factory is a no-state lambda capturing `AuthPreferences` (existing credential store). No secret material stored in the factory itself | NONE |
| Exception wrapping | k-9 exceptions wrapped in `MailException` preserve the original cause chain (`getCause()`). No credential information in exception messages beyond what existed before | NONE |
| `MailTransportTestFactories` in test code | Test-only file; not compiled into production APK | NONE |

## No Security Issues Found.
