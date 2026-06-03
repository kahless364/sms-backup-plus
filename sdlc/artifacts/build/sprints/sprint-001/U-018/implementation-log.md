---
artifact_type: implementation-log
story_id: "U-018"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
files_changed: 5
files_created: 1
tests_added: 10
tests_passing: 330
---

# Implementation Log: U-018

## Summary

Deleted `CalendarAccessorPre40.java` (125-line dead pre-API-14 calendar shim), collapsed the dead
`if (sdkVersion < ICE_CREAM_SANDWICH)` factory branch in `CalendarAccessor.java`, removed the
always-true `SDK_INT >= JELLY_BEAN` guard in `DataType.java` and the `SDK_INT >= 14` guard in
`TokenRefresher.java` (including the now-unnecessary `getAuthTokenPreApi14` dead method and its
`@TargetApi`/`@SuppressWarnings("deprecation")` scaffolding), verified `minSdkVersion 21` (already
set by U-001), implemented `StatusPreference` instance-state save/restore via
`SavedState extends Preference.BaseSavedState`, and added 10 Robolectric tests covering the full
`Parcelable` round-trip and the `onRestoreInstanceState` to `restoredState` to `onBindViewHolder`
lifecycle contract.

## Pre-Implementation Audit

The worktree was initially at `master` (pre-modernization). Rebased onto
`upstream/sdlc/modernization-plan` (HEAD `a50d6b2`, U-007 merged) to satisfy the U-003 / U-006
preconditions. Post-rebase state:

- `app/build.gradle`: `minSdkVersion 21`, `compileSdkVersion 35`, `targetSdkVersion 35` — confirms
  the DES-MODERNIZATION-001 precondition is satisfied before any deletion in this unit.
- `CalendarAccessorPre40.java`: present; only referenced from `CalendarAccessor.java:50`
  (grep confirmed — zero other production or test references).

### Files Read Before Implementation

| File | Why |
|------|-----|
| `U-018-dead-code-minsdk-cleanup.md` | Story ACs, affected-file table, verification steps |
| `DES-MODERNIZATION-006-dead-code-minsdk-cleanup-design.md` | Design rationale, SavedState pattern, scope boundaries |
| `REQ-MODERNIZATION-006-dead-code-and-minsdk-cleanup.md` | Governing requirement, constraints |
| `CalendarAccessor.java` | Dead factory branch to collapse; interface to preserve |
| `CalendarAccessorPre40.java` | Deletion target (full read, 125 lines) |
| `CalendarAccessorPost40.java` | Surviving implementation (must be byte-for-byte unchanged) |
| `StatusPreference.java` | Stubbed save/restore; onBindViewHolder; helper methods |
| `DataType.java` | JELLY_BEAN ternary to collapse |
| `TokenRefresher.java` | `SDK_INT >= 14` guard and dead pre-14 methods to remove |
| `app/build.gradle` | minSdkVersion confirmation |
| `lint-baseline.xml` | Identified 3 ObsoleteSdkInt entries for now-deleted code |
| `CalendarAccessorPost40Test.java` | Surviving calendar test — must remain green |
| `CalendarSyncerTest.java` | Interface-level mock test — must remain green |

### Audit Evidence — CalendarAccessorPre40 Deadness

```
grep -rn "CalendarAccessorPre40" app/src/ returns:
  CalendarAccessor.java:50 (in the dead < ICE_CREAM_SANDWICH branch)
  CalendarAccessorPre40.java:32,37 (own definition)
```

Zero test files, zero other production files reference the symbol.

## Files Modified

| File | Change | AC |
|------|--------|----|
| `app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessor.java` | Removed `import android.os.Build`. Removed `sdkVersion` local, `try/catch`, dead `if (sdkVersion < ICE_CREAM_SANDWICH)` branch. Factory collapses to null-check + unconditional `new CalendarAccessorPost40(resolver)`. Interface (lines 10-37) untouched. `Get.instance(ContentResolver)` signature preserved. | AC-1 |
| `app/src/main/java/com/zegoggles/smssync/mail/DataType.java` | Removed `import android.os.Build`. Replaced JELLY_BEAN ternary with unconditional `new String[]{ READ_CONTACTS, READ_CALL_LOG }`. | AC-3 |
| `app/src/main/java/com/zegoggles/smssync/auth/TokenRefresher.java` | Removed imports for `TargetApi`, `Build`, `ICE_CREAM_SANDWICH`. Replaced `getAuthToken(Account)` with the unconditional 6-arg `AccountManager.getAuthToken(...)` body. Deleted `getAuthTokenPreApi14` and `getAuthTokenApi14` helper methods. | AC-3 |
| `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` | Added `IconKind` enum (IDLE/DONE/ERROR/SYNCING). Added package-private tracking fields `currentStatusColor`, `currentIconKind`, `restoredState`. Updated constructor to initialise tracking fields. Updated `setViewAttributes`, `finishedBackup`, `finishedRestore`, `idle` helpers to set tracking fields. Replaced TODO stubs with full `onSaveInstanceState()` and `onRestoreInstanceState(Parcelable)`. Updated `onBindViewHolder` to call `applyRestoredState(restoredState)` instead of `idle()` when `restoredState != null`. Added `SavedState extends Preference.BaseSavedState` inner class with 7 fields, `writeToParcel`, and `CREATOR`. | AC-5, AC-6 |
| `app/lint-baseline.xml` | Removed 3 now-stale `ObsoleteSdkInt` entries: `DataType.java:21` (JELLY_BEAN ternary), `TokenRefresher.java:82` (SDK_INT >= 14 guard), `TokenRefresher.java:100` (@TargetApi(ICE_CREAM_SANDWICH) annotation). | AC-3 |

## Files Deleted

| File | Reason | AC |
|------|--------|----|
| `app/src/main/java/com/zegoggles/smssync/calendar/CalendarAccessorPre40.java` | Pre-API-14 calendar shim; only reference was the dead < ICE_CREAM_SANDWICH factory branch; unreachable at minSdk 21. | AC-1, AC-7 |

## Files Created

| File | Purpose | AC |
|------|---------|-----|
| `app/src/test/java/com/zegoggles/smssync/activity/StatusPreferenceTest.java` | 10 Robolectric tests: 3 Parcel round-trip tests, 2 `onSaveInstanceState` tests, 3 `onRestoreInstanceState` tests, 2 full save/parcel/restore round-trip tests | AC-9 |

## Capabilities Inventory

### CalendarAccessorPre40.java (deleted — all capabilities INTENTIONALLY REMOVED)

| Capability | Disposition |
|-----------|-------------|
| Pre-API-14 calendar event insertion | INTENTIONALLY REMOVED — AC-1; unreachable at minSdk 21 |
| Pre-API-14 calendar list query | INTENTIONALLY REMOVED — AC-1; unreachable at minSdk 21 |
| `enableSync` returning false | INTENTIONALLY REMOVED — AC-1; unreachable at minSdk 21 |

### CalendarAccessor.Get.instance (modified)

| Capability | Disposition |
|-----------|-------------|
| `< ICE_CREAM_SANDWICH` dispatch to `CalendarAccessorPre40` | INTENTIONALLY REMOVED — AC-1 |
| Null-check singleton cache | RETAINED — `CalendarAccessor.java:45-47` |
| `Get.instance(ContentResolver)` public signature | RETAINED — `CalendarAccessor.java:44` |
| Return `CalendarAccessorPost40` on API 21+ | RETAINED (now unconditional) — `CalendarAccessor.java:46` |

### DataType.java (modified)

| Capability | Disposition |
|-----------|-------------|
| CALLLOG requires `{ READ_CONTACTS, READ_CALL_LOG }` on API 16+ | RETAINED as unconditional — `DataType.java:20` |
| Dead else-branch `{ READ_CONTACTS }` for API < 16 | INTENTIONALLY REMOVED — AC-3 |

### TokenRefresher.java (modified)

| Capability | Disposition |
|-----------|-------------|
| `refreshOAuth2Token`, `refreshUsingAccountManager`, `invalidateToken`, `refreshUsingOAuth2Client` | RETAINED — all present and unchanged |
| 6-arg `AccountManager.getAuthToken` call (API 14+) | RETAINED — `TokenRefresher.java:81-87` (now direct, no dispatch method) |
| Dead 5-arg pre-API-14 `getAuthToken` call | INTENTIONALLY REMOVED — AC-3 |

### StatusPreference.java (modified)

| Capability | Disposition |
|-----------|-------------|
| `idle()` on first bind when no rotation | RETAINED — `StatusPreference.java:136` |
| `backupStateChanged`/`restoreStateChanged` view updates | RETAINED — unchanged |
| `onDetached` bus unregistration | RETAINED |
| All button/icon/progress/label setters | RETAINED |
| `onSaveInstanceState` TODO stub | INTENTIONALLY REMOVED — replaced by AC-5 implementation |
| `onRestoreInstanceState` TODO stub | INTENTIONALLY REMOVED — replaced by AC-5 implementation |
| New: apply snapshot in `onBindViewHolder` instead of `idle()` | ADDED — `StatusPreference.java:127-132` (AC-6) |

## Test Results

| Task | Env | Result |
|------|-----|--------|
| `:app:testDebugUnitTest` | JDK 17 | PASS — 330 tests (320 pre-U-018 + 10 new) |
| `:app:jacocoTestCoverageVerification` | JDK 17 | PASS — service*, mail*, auth* all >= 70% |
| `:app:assembleDebug` | JDK 17 | PASS — APK built successfully |
| `grep -rn "CalendarAccessorPre40" app/src/` | n/a | PASS — zero matches (AC-7) |
| `grep -rn "minSdkVersion" . --include="*.gradle"` | n/a | PASS — only `app/build.gradle:15 = 21` (AC-2) |
| `CalendarAccessorPost40Test` and `CalendarSyncerTest` | JDK 17 | PASS — both included in full run above |

**Note on JDK:** `compileDebugJavaWithJavac` fails with the system JDK 22 (pre-existing issue:
`-Xlint:-options` warning from Java 8 source/target). All tasks were run with JDK 17 as declared in
`gradle.properties`. This is a pre-existing environment configuration issue not introduced by U-018.

## Integration Verification

| Integration point | Verified |
|------------------|---------|
| `CalendarAccessor.Get.instance(ContentResolver)` signature preserved | PASS — `assembleDebug` succeeds; signature at `CalendarAccessor.java:44` unchanged |
| `BackupTask.java:78` call site compiles unchanged | PASS — included in `assembleDebug` |
| `AdvancedSettings.java:314` call site compiles unchanged | PASS — included in `assembleDebug` |
| `CalendarSyncerTest` (interface mock) passes | PASS — full test run |
| `CalendarAccessorPost40Test` (surviving calendar test) passes | PASS — full test run |
| `StatusPreference.SavedState` internal to `StatusPreference` | PASS — `static final class` within the class |
| `StatusPreferenceTest` reachable from Gradle test runner | PASS — 10 tests executed in full run |

## Contract Adherence

No `CNTR-*` artifacts govern this story (`integration_contracts: []` in frontmatter). The story
explicitly states "This story introduces no new cross-component boundary." No contract adherence
section required beyond this confirmation.

## Notes

- The `try/catch` wrapper around the factory construction was also removed (per DES-006 §1:
  "removing it is permitted but not required — it is cosmetic and changes no behavior").
- The tracking fields (`currentStatusColor`, `currentIconKind`, `restoredState`) are package-private
  to enable direct access from `StatusPreferenceTest` in the same package, following the Java
  test-accessibility convention used elsewhere in this codebase.
- The `CalendarAccessorPost40.java:23` `@TargetApi(ICE_CREAM_SANDWICH)` lint-baseline entry was NOT
  removed — that file is byte-for-byte unchanged and the annotation is still present there.
