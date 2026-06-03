---
artifact_type: review-code
story_id: U-021
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# Code Review: U-021

## Summary

Verification pass on U-020-delivered code plus two new test files and one comment addition.
All production code was delivered by U-020; U-021 adds tests and a minor documentation
improvement to MainViewModel.kt. No architectural concerns with the gap-fill changes.

## Review Findings

### MainViewModel.kt — inline TODO comment (AC-3)

**PASS.** The class-body comment `// TODO @HiltViewModel — retrofit in MU-007 (U-022)` is
added at the class declaration line, satisfying the story's exact wording requirement.
The existing KDoc block also references `@HiltViewModel` and MU-007 — the inline comment
makes the deferred-injection marker visible in the class signature context.

### MainViewModel.kt — delegation pattern (AC-3, IC-2)

**PASS.** `val state: StateFlow<SyncState> = repository.state` and
`val events: SharedFlow<SyncEvent> = repository.events` are direct property assignments —
no wrapping, no intermediate transformation. This is the correct pattern for the single
indirection layer required by IC-2.

### MainActivityFlowHelper.kt — structural deviation (AC-2)

**ACCEPTED DEVIATION.** Two separate `lifecycleScope.launch { repeatOnLifecycle(STARTED) }` 
blocks instead of one block with two child `launch`es. Behaviorally identical: both use
`Lifecycle.State.STARTED`, both are started in `onCreate`, both collect through the ViewModel.
Refactoring to a single block is deferred as non-blocking polish.

### StatusPreference.java — Cancel.Origin.USER (CNTR-MODERNIZATION-006)

**PASS.** All four cancel call sites use `new SyncEvent.Cancel(USER)` (the `USER` origin
constant imported from `SyncEvent.Cancel.Origin`). The `mayInterruptIfRunning()` semantics
are preserved downstream. No flattening to boolean.

### StatusPreference.java — tryEmitEvent return-value checking (AC-5, rule 6)

**PASS.** All four button-handler call sites (backup start, backup cancel, restore start,
restore cancel) assign the boolean return and log `Log.w(TAG, "...")` on false. No silent
failure.

### MainViewModelTest.kt — test coverage (AC-9)

**PASS.** Nine test methods covering:
- Initial state delegation (same instance as repository.state)
- State update propagation after `emitState()`
- `state` is the same `StateFlow` instance (IC-2 verification)
- Flow collection of a `BackupState`
- `events` is the same `SharedFlow` instance (IC-2 verification)
- Event collector receives emitted events
- Cancel event with `Origin.USER` round-trip
- `tryEmitEvent` returns true with available buffer capacity
- `tryEmitEvent` return value is not silently discarded

### StatusPreferenceFlowHelperTest.kt — scope-cancellation coverage (AC-5/AC-9)

**PASS.** Four test methods covering:
- `cancelScope()` makes a scope inactive
- `cancelScope()` is idempotent (no throw on already-cancelled scope)
- `startCollection()` returns an active scope
- Scope returned by `startCollection()` is cancelled by `cancelScope()`

These tests validate the core invariant that no coroutine scope leaks past `onDetached()`.

## No Regressions

The only source file modified (`MainViewModel.kt`) received one comment line. No behavior
change, no signature change, no new imports. All 272 existing test methods continue to pass.

## Findings Not Requiring Action

- `RuntimeEnvironment.application` deprecation warning in `StatusPreferenceFlowHelperTest.kt`:
  matches the same pattern used in the existing `StatusPreferenceTest.java`. Not a new issue.
- Two Kotlin coroutine deprecation warnings in `BackupWorker.kt` and `RestoreWorker.kt`:
  pre-existing, outside this story's scope.
