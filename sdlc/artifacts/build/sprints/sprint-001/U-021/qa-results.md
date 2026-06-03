---
artifact_type: qa-results
story_id: U-021
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# QA Results: U-021

## AC Grep Verification

**AC-1 / AC-6 — No `@Subscribe` annotations in MainActivity or StatusPreference:**

```
$ grep -n "^[[:space:]]*@Subscribe" app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java
(zero lines, exit 0)

$ grep -n "^[[:space:]]*@Subscribe" app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java
(zero lines, exit 0)
```

Result: PASS — zero actual `@Subscribe` annotations in either file (comments referencing
the removed annotations are not annotations).

**AC-7 — No `App.register`/`App.unregister` calls targeting MainActivity or StatusPreference:**

```
$ grep -rn "App\.register\|App\.unregister" app/src/main/java/com/zegoggles/smssync/activity/
```

Output: Only comment lines in DialogsFlowHelper.kt, MainActivityFlowHelper.kt,
StatusPreferenceFlowHelper.kt, and fragments. Zero actual calls in MainActivity.java
or StatusPreference.java.

Result: PASS.

## Build Gate Results

| Gate | Command | Result |
|------|---------|--------|
| Compile | `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL |
| Unit tests | `./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL (272 test methods, 0 failures) |
| Coverage | `./gradlew :app:jacocoTestCoverageVerification` | BUILD SUCCESSFUL (≥70% gate passed) |

## New Tests

| File | Tests | Coverage target |
|------|-------|-----------------|
| `MainViewModelTest.kt` | 9 | AC-3, AC-9: MainViewModel delegates state/events to repository |
| `StatusPreferenceFlowHelperTest.kt` | 4 | AC-5, AC-9: scope cancelled in onDetached (no leak) |

## Behavioral Verification

### AC-4 handler behaviors (read verification)

| Former @Subscribe | New handler | Location | Behavioral match |
|------------------|-------------|----------|-----------------|
| `restoreStateChanged` (L265) | `onRestoreStateChanged()` | `MainActivity.java:288-291` | `isSmsBackupDefaultSmsApp` + `restoreDefaultSmsProvider` path preserved |
| `backupStateChanged` (L271) | `onBackupStateChanged()` | `MainActivity.java:294-300` | `MANUAL/SKIP` + `isPermissionException` → `requestPermissions` path preserved |
| `onOAuth2Callback` (L280) | `onSyncEvent` OAuth2Callback branch | `MainActivity.java:306-319` | `event.valid()` check + `setOauth2Token` + `AccountAdded` event preserved |
| `onConnect` (L289) | `onSyncEvent` AccountConnectionChanged branch | `MainActivity.java:320-329` | Start `AccountManagerAuthActivity` preserved |
| `handleFallbackAuth` (L298) | `onSyncEvent` FallbackAuth branch | `MainActivity.java:330-336` | `startActivityForResult(fallbackAuthIntent, REQUEST_WEB_AUTH)` preserved |
| `themeChangedEvent` (L306) | `onSyncEvent` ThemeChanged branch | `MainActivity.java:337-339` | `recreate()` call preserved |
| `performAction` (L331) | `onSyncEvent` PerformActionRequested branch → `performAction()` | `MainActivity.java:340-343`, `368-380` | `confirm` flag read; CONFIRM_ACTION/FIRST_SYNC/direct-execute branching preserved |
| `doPerform` (L345) | `doPerform()` private method | `MainActivity.java:383-393` | `startBackup`/`startRestore` dispatch preserved |
| (8th: backupStateChanged @ L271 is one handler, also verified above) | | | |

### AC-5 StatusPreference verification (read verification)

| Requirement | Location | Status |
|-------------|----------|--------|
| `App.register(this)` removed | `StatusPreference.java:161` — comment only | PASS |
| `App.unregister(this)` removed | `StatusPreference.java:119` — comment only | PASS |
| `isServiceWorking()` absent | Not present anywhere in file | PASS |
| `isServiceIdle()` absent | Not present anywhere in file | PASS |
| State check via repository | `L264-265`: `getState().getValue().isRunning()` + instanceof `BackupState` | PASS |
| `Cancel(Origin.USER)` used | `L282`: `new SyncEvent.Cancel(USER)` (backup); `L307`: same (restore) | PASS |
| Return value checked | `L283`: `if (!emitted) Log.w(...)`; `L308`: same | PASS |
| Scope cancelled in `onDetached` | `L120-122`: `StatusPreferenceFlowHelper.cancelScope(collectionScope)` | PASS |

## Regression Check

All 272 test methods in the test suite pass. No test that was passing before this story
is now failing. The single source file change (one comment line in `MainViewModel.kt`)
has no behavioral effect.

## Story Status

U-021: **done** (all ACs met, tests green, coverage gate passed, commit created).
