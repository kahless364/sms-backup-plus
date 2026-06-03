---
artifact_type: review-code
story_id: "U-018"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# Code Review: U-018

## Summary

All changes are deletions of provably-dead code or implementation of a well-bounded UI lifecycle
method. The scope is proportionate and the changes are correct.

## CalendarAccessor.java

- Dead `if/else` branch removed cleanly. `Build` import removed. Signature preserved.
- The `try/catch` removal is cosmetic and safe (`new CalendarAccessorPost40(resolver)` throws no
  checked exception).
- The live `CalendarAccessor` interface (lines 10-37) is byte-for-byte unchanged.

## DataType.java

- The unconditional `new String[]{ READ_CONTACTS, READ_CALL_LOG }` is the correct runtime behavior
  at any API level >= 16. At minSdk 21 this is always the right value.
- `Build` import removal is correct (no other usage in the file).

## TokenRefresher.java

- The 6-arg `accountManager.getAuthToken(account, AUTH_TOKEN_TYPE, null, true, null, null)` is the
  only correct form at API 21+ (the 5-arg form with boolean `notifyAuthFailure` was deprecated in
  API 14 and removed from practical use).
- All other methods (`refreshOAuth2Token`, `refreshUsingAccountManager`, `invalidateToken`,
  `refreshUsingOAuth2Client`) are unchanged.

## StatusPreference.java

- `IconKind` enum is clean and maps 1:1 to the four static drawables.
- Tracking fields are package-private (needed for same-package test access) and initialised in the
  constructor to `idleColor` / `IconKind.IDLE`, matching the default `idle()` state.
- Every point that calls `setTextColor` and `setImageDrawable` now also updates the tracking fields
  (verified by inspection: `setViewAttributes`, `finishedBackup`, `finishedRestore`, `idle`).
- `onSaveInstanceState` correctly guards null views (`statusLabel == null ? null : ...`) since the
  method can be called before `onBindViewHolder`.
- `onRestoreInstanceState` correctly delegates to super when the state is not a `SavedState` (handles
  the initial case where `Preference` calls this with a non-SavedState on first creation).
- `onBindViewHolder` applies `restoredState` before `App.register(this)`, which is correct (restored
  state should be visible before any incoming events).
- `SavedState` `writeToParcel`/constructor symmetry is correct; null strings are handled
  (write `null`, read back as `null`).

## StatusPreferenceTest.java

- Tests use `AbsSavedState.EMPTY_STATE` as the super-state (correct; avoids the API-30-only
  `Parcelable.EMPTY_STATE`).
- Parcel obtain/recycle is balanced.
- Tests verify all 7 `SavedState` fields round-trip correctly.
- Tests verify `restoredState` is set after `onRestoreInstanceState` and null after no-op calls.
- The "idle not applied over restored values" contract is tested by asserting `restoredState != null`
  after restore (if `restoredState` is non-null, `onBindViewHolder` will call `applyRestoredState`,
  not `idle()`).

## Findings

No issues. All changes are correct, minimal, and scope-bounded per DES-MODERNIZATION-006.
