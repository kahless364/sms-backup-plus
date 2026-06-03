---
artifact_type: plan
story_id: "U-018"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# Plan: U-018

## Approach

Low-risk deletion-and-fix unit. All changes are deletions of provably-dead code or implementation
of already-stubbed lifecycle methods.

## Steps

1. **Rebase worktree onto upstream/sdlc/modernization-plan** — satisfies U-003/U-006 preconditions
   (minSdkVersion 21, lint-baseline committed, coverage gate green).

2. **Delete `CalendarAccessorPre40.java`** — `rm` the file after confirming zero other references.

3. **Collapse `CalendarAccessor.Get.instance`** — remove `sdkVersion` local, `Build` import,
   `try/catch`, and dead `if/else` branch; leave unconditional `new CalendarAccessorPost40(resolver)`.

4. **Collapse `DataType.java` JELLY_BEAN guard** — replace ternary with `new String[]{ READ_CONTACTS,
   READ_CALL_LOG }` unconditionally; remove `import android.os.Build`.

5. **Collapse `TokenRefresher.java` SDK_INT >= 14 guard** — remove `getAuthToken` conditional
   dispatch; delete `getAuthTokenPreApi14` and `getAuthTokenApi14` methods; remove `TargetApi`,
   `Build`, `ICE_CREAM_SANDWICH` imports.

6. **Update `lint-baseline.xml`** — remove 3 now-stale `ObsoleteSdkInt` entries for the deleted code.

7. **Implement `StatusPreference` SavedState** — add `IconKind` enum, tracking fields, update helpers,
   implement `onSaveInstanceState`/`onRestoreInstanceState`, update `onBindViewHolder`, add
   `SavedState extends Preference.BaseSavedState` inner class.

8. **Write `StatusPreferenceTest`** — 10 Robolectric tests for Parcel round-trip and lifecycle.

9. **Verify** — `testDebugUnitTest` green, `jacocoTestCoverageVerification` green,
   `assembleDebug` green, grep gates satisfied.
