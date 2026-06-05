---
artifact_type: plan
story_id: "U-038"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-04"
contracts_verified: []
risks_identified: 0
---

# Implementation Plan: U-038

## Story Overview
`RestoreWorker.kt` imports five mail adapter/transport types that are never used in any
executable expression: `BackupImapStore`, `PinnedCertStore`, `TlsTrustPolicy`, `K9MailTransport`,
`MailTransportConfig`. These are dead imports — ACL-purity violation (adapter types in
`service.*`) and regression risk.

## Acceptance Criteria Mapping

| AC | Implementation Step | Notes |
|----|-------------------|-------|
| AC-1 | Remove the 5 imports from RestoreWorker.kt:37-48 | Verified each is unused before removing |
| AC-2 | grep confirms no genuine usages | Confirmed — KDoc mentions do not count |
| AC-3 | Build green; no behavior change | assembleDebug + testDebugUnitTest + jacoco green |

## Implementation Steps

1. Verify each of the 5 imports is unused: search RestoreWorker.kt body for each type name in executable code.
2. Remove all 5 imports.
3. Add comment documenting the cleanup.

## Verification of Unused Status

- `BackupImapStore`: appears in KDoc comment ("Rewired from BackupImapStore (k-9 type)...") but not in any executable expression. Previously the field type for `buildImapStore` (deleted by U-026). Not used.
- `PinnedCertStore`: not referenced in any field, local, method call, or type annotation in the file body.
- `TlsTrustPolicy`: not referenced in any executable expression.
- `K9MailTransport`: referenced in KDoc ("uses [K9MailTransport.importMessageBody]") but not in any executable expression (the actual call uses the interface type `MailTransport`). Not used.
- `MailTransportConfig`: not referenced in any executable expression.

## Phase Completion Report
---
story_id: "U-038"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-006/U-038/plan.md"
story_status: "done"
current_build_phase: "done"
errors: []
---
