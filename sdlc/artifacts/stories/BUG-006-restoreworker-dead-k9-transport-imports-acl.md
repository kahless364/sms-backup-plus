---
type: bug
status: done
artifact_type: bug
severity: medium
priority: medium
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-038
platforms: []
tags: []
id: BUG-006
title: RestoreWorker.kt has dead mail-transport/adapter imports (ACL-purity nit)
domain: build
origin: code-review
---

# BUG-006: Dead mail-transport/adapter imports in RestoreWorker.kt

## Description
`RestoreWorker.kt` imports five mail adapter/transport types never used in any executable expression: `BackupImapStore`, `PinnedCertStore`, `TlsTrustPolicy`, `K9MailTransport`, `MailTransportConfig`. Dead imports — a maintainability/ACL-purity issue (adapter types referenced from a `service.*` engine file) and a future-regression risk. `BackupWorker.kt` has none. (These are app-owned `com.zegoggles.smssync.mail.*` types, not `com.fsck.k9.*`, so U-030's `com.fsck.k9` grep did not flag them.)

## Steps to Reproduce
1. `grep -nE "import .*(BackupImapStore|PinnedCertStore|TlsTrustPolicy|K9MailTransport|MailTransportConfig)" app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt`
2. Observe imports exist; the file body uses none (KDoc mentions don't count).

## Expected Behavior
`RestoreWorker.kt` imports only types it uses; no adapter/transport types in the engine file.

## Actual Behavior
Five unused imports present (~`RestoreWorker.kt:37-48`).

## Evidence
- `service/RestoreWorker.kt:37-48`.
- Consolidated code review: `sdlc/artifacts/build/reviews/consolidated-2026-06/code-review.md` finding B-3.

## Acceptance Criteria
- [ ] AC-1: The five unused imports are removed from `RestoreWorker.kt`.
- [ ] AC-2: `grep` for those types in `RestoreWorker.kt` returns zero genuine usages.
- [ ] AC-3: Build green; no behavior change.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `service/RestoreWorker.kt:37-48` | 5 unused adapter/transport imports | remove them |

## Existing Behavior to Preserve
- RestoreWorker runtime behavior unchanged (imports are dead).

## Verification Steps
1. Remove imports; build; grep confirms absence.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- Trivial cleanup; reinforces CNTR-MODERNIZATION-007 ACL boundary (no adapter types in `service.*`).

## Supporting Documentation
- `sdlc/artifacts/build/reviews/consolidated-2026-06/code-review.md` (B-3)

## Notes
Lowest-effort of the three code findings; bundled with the BUG-004/BUG-005 fix.
