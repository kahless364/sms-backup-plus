---
type: bug
status: planned
sprint: '000006'
artifact_type: user-story
priority: medium
complexity: low
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-038
title: Remove dead mail-transport imports from RestoreWorker.kt (fix BUG-006)
pipeline: ''
domain: modernization
requirement_source: bug:BUG-006
---

# U-038: Remove dead mail-transport imports from RestoreWorker.kt

## Story
As a maintainer, I want `RestoreWorker.kt` to import only what it uses, so that no mail adapter/transport types leak into the `service.*` engine layer (ACL purity) and there's no dead-import regression risk.

## Source
Fixes **BUG-006** (medium) — see `sdlc/artifacts/stories/BUG-006-restoreworker-dead-k9-transport-imports-acl.md`.

## Acceptance Criteria
1. AC-1: The five unused imports (`BackupImapStore`, `PinnedCertStore`, `TlsTrustPolicy`, `K9MailTransport`, `MailTransportConfig`) are removed from `service/RestoreWorker.kt` (~:37-48).
2. AC-2: `grep` for those types in `RestoreWorker.kt` returns no genuine usages; the file compiles unchanged in behavior.
3. AC-3: Build green (assembleDebug, testDebugUnitTest, jacoco ≥70%); no behavior change.

## Technical Notes
- File: `service/RestoreWorker.kt:37-48`. Pure cleanup; verify each import is truly unused (KDoc mentions don't count) before removing. Reinforces CNTR-MODERNIZATION-007 (no adapter types in `service.*`).

## Estimation Guidance
Low (trivial).
