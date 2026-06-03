---
artifact_type: qa-results
story_id: "U-018"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# QA Results: U-018

## Acceptance Criteria Verification

| AC | Description | Verdict | Evidence |
|----|-------------|---------|----------|
| AC-1 | `CalendarAccessorPre40.java` deleted; dead factory branch removed; call sites compile | PASS | File deleted; `CalendarAccessor.java` factory collapses to null-check + unconditional `Post40`; `assembleDebug` succeeds |
| AC-2 | `minSdkVersion 21` in `defaultConfig`; no `14` in any build file | PASS | `grep -rn "minSdkVersion" --include="*.gradle"` returns only `app/build.gradle:15 minSdkVersion 21` |
| AC-3 | `DataType.java` JELLY_BEAN guard collapsed; `TokenRefresher.java` SDK_INT >= 14 guard collapsed | PASS | `DataType.java:20` unconditional `new String[]{ READ_CONTACTS, READ_CALL_LOG }`; `TokenRefresher.java` `getAuthToken` direct call, no dispatch methods |
| AC-4 | KITKAT guards in `SmsRestoreService.java`, `MainActivity.java`, `SmsReceiver.java` untouched; `AuthMode.XOAUTH` untouched; `CalendarAccessor` interface untouched | PASS | Grep confirms lines 67/165 (`SmsRestoreService`), lines 364/368 (`MainActivity`), line 50 (`SmsReceiver`) unchanged; `AuthMode.java:6` `XOAUTH` present; interface methods `enableSync`/`addEntry`/`getCalendars` unchanged |
| AC-5 | `StatusPreference.SavedState` inner class implements all required fields and `Parcelable.Creator` | PASS | `SavedState` at `StatusPreference.java` with `statusText`, `statusColor`, `detailsText`, `progress`, `max`, `indeterminate`, `iconKind`; `CREATOR` present |
| AC-6 | `onBindViewHolder` applies `restoredState` instead of `idle()` when `restoredState != null` | PASS | `onBindViewHolder:127-132` checks `restoredState != null` → `applyRestoredState(restoredState); restoredState = null`, else `idle()` |
| AC-7 | `grep -rn "CalendarAccessorPre40" app/src` returns zero matches | PASS | Zero matches confirmed |
| AC-8 | Full Robolectric test suite passes; `CalendarAccessorPost40Test` and `CalendarSyncerTest` both pass | PASS | `:app:testDebugUnitTest` BUILD SUCCESSFUL (330 tests) |
| AC-9 | `StatusPreferenceTest` Robolectric test class added covering Parcel round-trip and restored-state lifecycle | PASS | 10 test methods in `StatusPreferenceTest.java`; all pass |

## Verification Commands

```
# AC-7 — grep gate
grep -rn "CalendarAccessorPre40" app/src/
# Returns: (no output) — PASS

# AC-2 — minSdkVersion gate
grep -rn "minSdkVersion" . --include="*.gradle"
# Returns: app/build.gradle:15: minSdkVersion 21 — PASS

# AC-8 — full test suite
JAVA_HOME=".jdks/jbr-17.0.14" ./gradlew :app:testDebugUnitTest
# Returns: BUILD SUCCESSFUL (330 tests) — PASS

# Coverage gate
JAVA_HOME=".jdks/jbr-17.0.14" ./gradlew :app:jacocoTestCoverageVerification
# Returns: BUILD SUCCESSFUL — PASS

# assembleDebug
JAVA_HOME=".jdks/jbr-17.0.14" ./gradlew :app:assembleDebug
# Returns: BUILD SUCCESSFUL — PASS
```
