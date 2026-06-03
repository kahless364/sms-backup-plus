---
artifact_type: plan
story_id: U-021
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# Plan: U-021 — MainViewModel / repeatOnLifecycle decomposition (verification + gap-fill)

## Context

U-020 (Otto removal, already merged at HEAD c18fd4ab) over-delivered and absorbed most of
U-021's scope. This plan describes the verification-first procedure used to check each AC
against the existing code and fill only genuine gaps.

## Scope

NOT a full implementation. Procedure:

1. Reset worktree to sdlc/modernization-plan HEAD (c18fd4ab).
2. Read every file named in U-021's Affected Code table.
3. Verify AC-1 through AC-9 and IC-1 through IC-3 against the actual code.
4. Fill only genuine gaps:
   - Add the inline `// TODO @HiltViewModel` class-body comment to MainViewModel.kt (AC-3, minor wording gap).
   - Write `MainViewModelTest.kt` (AC-9: ViewModel delegates state/events to repository).
   - Write `StatusPreferenceFlowHelperTest.kt` (AC-5/AC-9: scope cancelled on onDetached).
5. Run `assembleDebug`, `testDebugUnitTest`, `jacocoTestCoverageVerification`.
6. Run AC grep checks.
7. Produce artifacts and commit.

## Files to Verify / Possibly Modify

| File | Action |
|------|--------|
| `app/.../activity/MainViewModel.kt` | Verify AC-3; add inline TODO comment |
| `app/.../activity/MainViewModelFactory.kt` | Verify AC-8 |
| `app/.../activity/MainActivity.java` | Verify AC-1/AC-2/AC-4/AC-8 |
| `app/.../activity/MainActivityFlowHelper.kt` | Verify AC-2/IC-2 |
| `app/.../activity/StatusPreference.java` | Verify AC-5/AC-6 |
| `app/.../activity/StatusPreferenceFlowHelper.kt` | Verify AC-5/AC-9 |
| `app/.../activity/Dialogs.java` | Verify AC-4 dialog branches |

## Files to Create

| File | Reason |
|------|--------|
| `app/src/test/.../activity/MainViewModelTest.kt` | AC-9: delegate tests |
| `app/src/test/.../activity/StatusPreferenceFlowHelperTest.kt` | AC-9: scope-cancellation tests |
| `sdlc/artifacts/build/sprints/sprint-001/U-021/plan.md` | This file |
| `sdlc/artifacts/build/sprints/sprint-001/U-021/implementation-log.md` | Required artifact |
| `sdlc/artifacts/build/sprints/sprint-001/U-021/review-code.md` | Required artifact |
| `sdlc/artifacts/build/sprints/sprint-001/U-021/review-security.md` | Required artifact |
| `sdlc/artifacts/build/sprints/sprint-001/U-021/qa-results.md` | Required artifact |

## Risk Assessment

Low. All changes are additive (new test files, one-line comment). No refactoring of working
production code. The only source modification is a comment addition to MainViewModel.kt.
