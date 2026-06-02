---
id: REQ-MODERNIZATION-006
title: Dead-Code Removal and minSdk Cleanup
artifact_type: requirement
domain: modernization
status: approved
type: constraint
priority: medium
epic: EPIC-MODERNIZATION-002
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-001
related_stories: []
related_design_docs: []
traces_to: []
change_records: []
notes: |
  Traceability: ARCH-005/STRUCT-005 (gaps), dead-code-candidates.md; migration unit MU-011; source assessment 20260529-modernization.
---

# Dead-Code Removal and minSdk Cleanup

## Description

Remove compatibility shims that are unreachable at the target `minSdk`, implement the
stubbed `StatusPreference` instance-state save/restore, and complete the minSdk-21
cleanup opened by the SDK raise.

## Context

`calendar/CalendarAccessorPre40.java` targets API < 14 and is already unreachable at
the current `minSdkVersion 14`; at the target `minSdk 21` (from REQ-MODERNIZATION-001)
it is provably dead by two API levels. Its only caller is the
`if (sdkVersion < ICE_CREAM_SANDWICH)` branch in `CalendarAccessor.java`, which
collapses once the shim is removed. Separately, `StatusPreference.java:131-139` contains
stubbed `// TODO` instance-state save/restore, causing a visual-state flash on screen
rotation. These are low-risk cleanups best swept opportunistically during/after the SDK
uplift.

## Acceptance Criteria

1. `calendar/CalendarAccessorPre40.java` is deleted; the dead `if (sdkVersion < ICE_CREAM_SANDWICH)`
   branch in `CalendarAccessor.java` is removed and its two call sites updated.
2. `StatusPreference` instance-state save/restore (`StatusPreference.java:131-139`) is
   implemented so UI state survives screen rotation with no visual flash.
3. `minSdkVersion` is set to 21 and any now-dead `Build.VERSION` guards below API 21 are removed.
4. The full test suite passes after removal; no reference to the deleted shim remains
   (verified by grep).
5. No behavioral change to supported devices (API 21+).

## Rationale

Removing provably-dead code reduces the modernization surface area and eliminates a
maintenance distraction; the `StatusPreference` fix closes a small but visible UX defect.
Both are cheap and reduce noise for the larger substrate migrations.

## Constraints

- Depends on REQ-MODERNIZATION-001 establishing `minSdk 21` (the precondition that makes
  the Pre40 shim provably dead).
- Must not remove the live `CalendarAccessor` interface — only the dead `Pre40`
  implementation and its factory branch.

## Notes

Boundary: `calendar/CalendarAccessorPre40.java`, `calendar/CalendarAccessor.java`,
`activity/StatusPreference.java`, `app/build.gradle` (minSdk).
Traceability: ARCH-005/STRUCT-005, dead-code-candidates.md; MU-011; source assessment 20260529-modernization.
