---
artifact_type: implementation-log
story_id: U-020
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
files_changed: 33
files_created: 13
tests_added: 6
tests_passing: 591
---

# Implementation Log: U-020

## Summary

Otto event bus fully removed. `FlowSyncStateRepository` replaces `DefaultSyncStateRepository`.
All 28+ @Subscribe/@Produce handler sites migrated to StateFlow/SharedFlow collection.
`App.bus`, `App.register`, `App.unregister`, `App.post` deleted. `CancelEvent.java`,
`DefaultSyncStateRepository.kt`, and 8 `activity/events/*.java` POJOs deleted.
`com.squareup:otto:1.3.8` removed from `app/build.gradle`.

**Build gate**: `assembleDebug` PASS. `testDebugUnitTest` (591 tests) PASS.
`jacocoTestCoverageVerification` (≥70%) PASS.
**Zero** `com.squareup.otto` references in production source.

## Worktree Context

Worktree branch: `worktree-agent-af9676c8d81523570`
Base: `sdlc/modernization-plan` HEAD (commit 32500558 — Merge U-016, includes Wave 1-8 + U-019 facade)
Commit: `ffae43c5`

## Handler Sites Migrated (28+ sites across 12 files)

| # | File | Site | Migration |
|---|------|------|-----------|
| 1 | `App.java` | `@Subscribe autoBackupSettingsChanged` | → `FlowCollectHelper.collectAutoBackupSettings()` App-scoped coroutine |
| 2 | `App.java` | `App.register(this)` in `onCreate` | → Deleted |
| 3 | `App.java` | `register()` static helper | → Deleted (AC-6) |
| 4 | `App.java` | `unregister()` static helper | → Deleted (AC-6) |
| 5 | `App.java` | `post()` static helper | → Deleted (AC-6) |
| 6 | `ServiceBase.java` | `App.register(this)` in `onCreate` | → Deleted (AC-7) |
| 7 | `ServiceBase.java` | `App.unregister(this)` in `onDestroy` | → Deleted (AC-7) |
| 8 | `SmsBackupService.java` | `@Produce produceLastState()` | → Deleted; StateFlow.value provides sticky semantics (AC-8) |
| 9 | `SmsBackupService.java` | `@Subscribe backupStateChanged(BackupState)` | → Made public non-@Subscribe; called directly (AC-8) |
| 10 | `SmsBackupService.java` | `SmsRestoreService.isServiceIdle()` at :101 | → `repository.state.value` instanceof check (AC-9) |
| 11 | `SmsBackupService.java` | static `service` field + `isServiceWorking()` | → Deleted (AC-8a,b) |
| 12 | `SmsRestoreService.java` | `@Produce produceLastState()` | → Deleted; StateFlow.value provides sticky semantics (AC-9) |
| 13 | `SmsRestoreService.java` | `@Subscribe restoreStateChanged(RestoreState)` | → Made public non-@Subscribe; called directly (AC-9) |
| 14 | `SmsRestoreService.java` | static `service` field + `isServiceIdle()` | → Deleted (AC-9a,b) |
| 15 | `SmsRestoreService.java` | `App.post(state.transition(ERROR,e))` | → `repository.emitState()` (IC-3) |
| 16 | `BackupTask.java` | `App.register(this)` in `onPreExecute` | → Deleted |
| 17 | `BackupTask.java` | `@Subscribe canceled(CancelEvent)` | → `BackupCancelCollector.collect()` in `doInBackground` |
| 18 | `BackupTask.java` | `App.unregister(this)` in `onPostExecute/onCancelled` | → Deleted |
| 19 | `BackupTask.java` | `App.post(state)` in `post()` | → `repository.emitState(state)` + `service.backupStateChanged(state)` (IC-3) |
| 20 | `RestoreTask.java` | `App.register(this)` in `onPreExecute` | → Deleted |
| 21 | `RestoreTask.java` | `@Subscribe canceled(CancelEvent)` | → `BackupCancelCollector.collectForRestore()` in `doInBackground` |
| 22 | `RestoreTask.java` | `App.unregister(this)` in `onPostExecute/onCancelled` | → Deleted |
| 23 | `RestoreTask.java` | `App.post(changed)` in `post()` | → `repository.emitState(changed)` + `service.restoreStateChanged(changed)` (IC-3) |
| 24 | `SmsJobService.java` | `App.register(this)` in `onCreate` | → `SmsJobServiceFlowHelper.observeBackupState()` |
| 25 | `SmsJobService.java` | `@Subscribe backupStateChanged(BackupState)` | → `onBackupStateChanged()` called by SmsJobServiceFlowHelper |
| 26 | `SmsJobService.java` | `App.unregister(this)` in `onDestroy` | → `stateObservationJob.cancel()` |
| 27 | `SmsJobService.java` | `App.post(new CancelEvent(SYSTEM))` | → `repository.tryEmitEvent(new SyncEvent.Cancel(SYSTEM))` (AC-17) |
| 28 | `MainActivity.java` | `App.register(this)` in `onStart` | → `MainActivityFlowHelper.startCollection()` via `repeatOnLifecycle(STARTED)` (AC-14b) |
| 29 | `MainActivity.java` | `App.unregister(this)` in `onStop` | → Lifecycle handles cleanup |
| 30 | `MainActivity.java` | `@Subscribe restoreStateChanged` | → `onRestoreStateChanged()` called by MainActivityFlowHelper |
| 31 | `MainActivity.java` | `@Subscribe backupStateChanged` | → `onBackupStateChanged()` called by MainActivityFlowHelper |
| 32 | `MainActivity.java` | `@Subscribe onOAuth2Callback` | → `onSyncEvent()` SyncEvent.OAuth2Callback branch (AC-14) |
| 33 | `MainActivity.java` | `@Subscribe onConnect` | → `onSyncEvent()` SyncEvent.AccountConnectionChanged branch |
| 34 | `MainActivity.java` | `@Subscribe handleFallbackAuth` | → `onSyncEvent()` SyncEvent.FallbackAuth branch |
| 35 | `MainActivity.java` | `@Subscribe themeChangedEvent` | → `onSyncEvent()` SyncEvent.ThemeChanged branch |
| 36 | `MainActivity.java` | `@Subscribe performAction` | → `onSyncEvent()` SyncEvent.PerformActionRequested branch (AC-14) |
| 37 | `MainActivity.java` | `@Subscribe doPerform(Actions)` | → private `doPerform()` called from performAction handler |
| 38 | `MainActivity.java` | `App.post(new AccountAddedEvent())` × 2 | → `repository.tryEmitEvent(SyncEvent.AccountAdded.INSTANCE)` (AC-14d) |
| 39 | `StatusPreference.java` | `App.register(this)` in `onBindViewHolder` | → `StatusPreferenceFlowHelper.startCollection()` (AC-15) |
| 40 | `StatusPreference.java` | `App.unregister(this)` in `onDetached` | → `StatusPreferenceFlowHelper.cancelScope()` (AC-15) |
| 41 | `StatusPreference.java` | `@Subscribe restoreStateChanged` | → `restoreStateChanged()` called by StatusPreferenceFlowHelper |
| 42 | `StatusPreference.java` | `@Subscribe backupStateChanged` | → `backupStateChanged()` called by StatusPreferenceFlowHelper |
| 43 | `StatusPreference.java` | `@Subscribe onMissingPermissions` | → `onMissingPermissions()` called by StatusPreferenceFlowHelper |
| 44 | `StatusPreference.java` | `App.post(new PerformAction(...))` × 2 | → `repository.tryEmitEvent(new SyncEvent.PerformActionRequested(...))` (AC-15c) |
| 45 | `StatusPreference.java` | `App.post(new CancelEvent())` × 2 | → `repository.tryEmitEvent(new SyncEvent.Cancel(USER))` (AC-15c) |
| 46 | `Dialogs.java` | `App.register(this)` in `OAuth2AccessTokenProgress.onAttach` | → `DialogsFlowHelper.collectOAuth2Callback()` |
| 47 | `Dialogs.java` | `@Subscribe onOAuth2Callback` in `OAuth2AccessTokenProgress` | → `onOAuth2Callback()` called by DialogsFlowHelper |
| 48 | `Dialogs.java` | `App.unregister(this)` in `OAuth2AccessTokenProgress.onDetach` | → `collectionJob.cancel()` |
| 49 | `Dialogs.java` | `App.post(BackupSkip/Backup)` in `FirstSync` | → `repository.tryEmitEvent(new SyncEvent.PerformActionRequested(...))` |
| 50 | `Dialogs.java` | `App.post(new SettingsResetEvent())` in `Reset` | → `repository.tryEmitEvent(SyncEvent.SettingsReset.INSTANCE)` |
| 51 | `Dialogs.java` | `App.post(new AccountRemovedEvent())` in `Disconnect` | → `repository.tryEmitEvent(SyncEvent.AccountRemoved.INSTANCE)` |
| 52 | `Dialogs.java` | `App.post(new FallbackAuthEvent(false))` in `AccountManagerTokenError` | → `repository.tryEmitEvent(SyncEvent.FallbackAuth.INSTANCE)` |
| 53 | `Dialogs.java` | `App.post(new PerformAction(...))` in `ConfirmAction` | → `repository.tryEmitEvent(new SyncEvent.PerformActionRequested(...))` |
| 54 | `AdvancedSettings.java` | `App.register(this)` in `Main.onStart` | → `AdvancedSettingsFlowHelper.startCollection()` |
| 55 | `AdvancedSettings.java` | `App.unregister(this)` in `Main.onStop` | → `flowCollectionJob.cancel()` |
| 56 | `AdvancedSettings.java` | `@Subscribe onAccountAdded` | → `onAccountAdded()` called by AdvancedSettingsFlowHelper |
| 57 | `AdvancedSettings.java` | `@Subscribe onAccountRemoved` | → `onAccountRemoved()` called by AdvancedSettingsFlowHelper |
| 58 | `AdvancedSettings.java` | `@Subscribe onSettingsReset` | → `onSettingsReset()` called by AdvancedSettingsFlowHelper |
| 59 | `AdvancedSettings.java` | `App.post(new AccountConnectionChangedEvent(...))` | → `repository.tryEmitEvent(SyncEvent.AccountConnectionChanged.INSTANCE)` |
| 60 | `AdvancedSettings.java` | `App.post(new AutoBackupSettingsChangedEvent())` | → `repository.tryEmitEvent(SyncEvent.AutoBackupSettingsChanged.INSTANCE)` |
| 61 | `MainSettings.java` | `App.register(this)` in `onStart` | → `MainSettingsFlowHelper.startCollection()` |
| 62 | `MainSettings.java` | `App.unregister(this)` in `onDestroy` | → `flowCollectionJob.cancel()` |
| 63 | `MainSettings.java` | `@Subscribe onAccountAdded` | → `onAccountAdded()` called by MainSettingsFlowHelper |
| 64 | `MainSettings.java` | `@Subscribe onAccountRemoved` | → `onAccountRemoved()` called by MainSettingsFlowHelper |
| 65 | `MainSettings.java` | `@Subscribe onAutoBackupSettingsChanged` | → `onAutoBackupSettingsChanged()` called by MainSettingsFlowHelper |
| 66 | `MainSettings.java` | `@Subscribe onSettingsReset` | → `onSettingsReset()` called by MainSettingsFlowHelper |
| 67 | `OAuth2WebAuthActivity.java` | `App.register(this)` in `onCreate` | → `OAuth2WebAuthFlowHelper.collectBrowserAuthResult()` |
| 68 | `OAuth2WebAuthActivity.java` | `@Subscribe onBrowserAuthResult(RedirectReceiverActivity.BrowserAuthResult)` | → `onBrowserAuthResult(SyncEvent.BrowserAuthResult)` via OAuth2WebAuthFlowHelper |
| 69 | `OAuth2WebAuthActivity.java` | `App.unregister(this)` in `onDestroy` | → Lifecycle handles cleanup |
| 70 | `SMSBackupPreferenceFragment.java` | `App.post(event)` in `addPreferenceListener` | → `repository.tryEmitEvent(event)` |
| 71 | `PackageReplacedReceiver.java` | `App.post(new AutoBackupSettingsChangedEvent())` | → `repository.tryEmitEvent(SyncEvent.AutoBackupSettingsChanged.INSTANCE)` (AC-17) |
| 72 | `RedirectReceiverActivity.java` | `App.post(new BrowserAuthResult(code, error))` | → `repository.tryEmitEvent(new SyncEvent.BrowserAuthResult(code, error))` (AC-17) |
| 73 | `OAuth2CallbackTask.java` | `App.post(new OAuth2CallbackEvent(token))` | → `repository.tryEmitEvent(new SyncEvent.OAuth2Callback(event))` (AC-17) |

**Total handler sites migrated: 73** (covering all 12 Otto-importing files plus receivers/tasks)

## Files Modified

- `app/build.gradle` — remove `com.squareup:otto:1.3.8`; add `lifecycle-runtime-ktx:2.6.2` + `lifecycle-viewmodel-ktx:2.6.2` (AC-1)
- `app/src/main/java/com/zegoggles/smssync/App.java` — swap to FlowSyncStateRepository; delete Bus/register/unregister/post; migrate autoBackupSettingsChanged to coroutine (AC-6)
- `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` — remove App.register/unregister (AC-7)
- `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` — delete static field + isServiceWorking; remove @Produce/@Subscribe (AC-8)
- `app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` — delete static field + isServiceIdle; remove @Produce/@Subscribe; fix service→this refs (AC-9)
- `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java` — remove register/unregister; add BackupCancelCollector call; replace App.post
- `app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java` — same pattern as BackupTask
- `app/src/main/java/com/zegoggles/smssync/service/SmsJobService.java` — replace App.register with SmsJobServiceFlowHelper; replace @Subscribe + CancelEvent post
- `app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java` — add MainViewModel; replace onStart/onStop register/unregister with MainActivityFlowHelper; migrate 7 @Subscribe handlers (AC-14)
- `app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java` — replace register/unregister with StatusPreferenceFlowHelper; migrate 3 @Subscribe + 4 App.post (AC-15)
- `app/src/main/java/com/zegoggles/smssync/activity/Dialogs.java` — replace @Subscribe + App.register/unregister in OAuth2AccessTokenProgress; migrate 5 App.post calls
- `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettings.java` — replace App.register/unregister with AdvancedSettingsFlowHelper; migrate 3 @Subscribe + 2 App.post
- `app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java` — replace App.register/unregister with MainSettingsFlowHelper; migrate 4 @Subscribe
- `app/src/main/java/com/zegoggles/smssync/activity/fragments/SMSBackupPreferenceFragment.java` — replace App.post with tryEmitEvent; change signature from Object to SyncEvent
- `app/src/main/java/com/zegoggles/smssync/activity/auth/OAuth2WebAuthActivity.java` — replace App.register/@Subscribe/App.unregister with OAuth2WebAuthFlowHelper
- `app/src/main/java/com/zegoggles/smssync/activity/auth/RedirectReceiverActivity.java` — delete inner BrowserAuthResult class; replace App.post with tryEmitEvent(SyncEvent.BrowserAuthResult)
- `app/src/main/java/com/zegoggles/smssync/receiver/PackageReplacedReceiver.java` — replace App.post with tryEmitEvent
- `app/src/main/java/com/zegoggles/smssync/tasks/OAuth2CallbackTask.java` — replace App.post with tryEmitEvent(SyncEvent.OAuth2Callback)
- `app/src/main/java/com/zegoggles/smssync/service/state/SyncEvent.kt` — add SyncEvent.BrowserAuthResult variant
- `gradle/verification-metadata.xml` — regenerated to include lifecycle 2.6.2 checksums
- `app/src/test/java/com/zegoggles/smssync/service/CancelEventTest.java` — migrate from CancelEvent to SyncEvent.Cancel
- `app/src/test/java/com/zegoggles/smssync/service/SmsJobServiceTest.java` — rename backupStateChanged → onBackupStateChanged
- `app/src/test/java/com/zegoggles/smssync/service/state/DefaultSyncStateRepositoryTest.kt` — update to FlowSyncStateRepository

## Files Created

- `app/src/main/java/com/zegoggles/smssync/FlowCollectHelper.kt` — App-scoped coroutine helper for autoBackupSettingsChanged
- `app/src/main/java/com/zegoggles/smssync/activity/MainViewModel.kt` — ViewModel exposing state/events to MainActivity (AC-14, AC-18)
- `app/src/main/java/com/zegoggles/smssync/activity/MainViewModelFactory.kt` — manual DI ViewModelProvider.Factory (AC-18, TODO U-022)
- `app/src/main/java/com/zegoggles/smssync/activity/MainActivityFlowHelper.kt` — lifecycleScope + repeatOnLifecycle flow collection for MainActivity
- `app/src/main/java/com/zegoggles/smssync/activity/StatusPreferenceFlowHelper.kt` — CoroutineScope-scoped flow collection for StatusPreference
- `app/src/main/java/com/zegoggles/smssync/activity/DialogsFlowHelper.kt` — OAuth2Callback flow collection for OAuth2AccessTokenProgress
- `app/src/main/java/com/zegoggles/smssync/activity/auth/OAuth2WebAuthFlowHelper.kt` — BrowserAuthResult flow collection for OAuth2WebAuthActivity
- `app/src/main/java/com/zegoggles/smssync/activity/fragments/AdvancedSettingsFlowHelper.kt` — account/settings event collection for AdvancedSettings.Main
- `app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettingsFlowHelper.kt` — account/settings event collection for MainSettings
- `app/src/main/java/com/zegoggles/smssync/service/BackupCancelCollector.kt` — flow.first() Cancel event collector for BackupTask/RestoreTask
- `app/src/main/java/com/zegoggles/smssync/service/SmsJobServiceFlowHelper.kt` — BackupState StateFlow observation for SmsJobService
- `app/src/main/java/com/zegoggles/smssync/service/state/FlowSyncStateRepository.kt` — live Flow-backed SyncStateRepository implementation (replaces DefaultSyncStateRepository)
- `app/src/test/java/com/zegoggles/smssync/service/state/SyncStateRepositoryTest.kt` — 6 unit tests (AC-4, AC-5, AC-10, AC-16)

## Files Deleted

- `app/src/main/java/com/zegoggles/smssync/service/state/DefaultSyncStateRepository.kt` — Otto-delegating facade replaced by FlowSyncStateRepository (AC-3)
- `app/src/main/java/com/zegoggles/smssync/service/CancelEvent.java` — absorbed into SyncEvent.Cancel (AC-10)
- `app/src/main/java/com/zegoggles/smssync/activity/events/AccountAddedEvent.java` — absorbed into SyncEvent.AccountAdded object
- `app/src/main/java/com/zegoggles/smssync/activity/events/AccountConnectionChangedEvent.java` — absorbed into SyncEvent.AccountConnectionChanged object (payload-less per U-019 design)
- `app/src/main/java/com/zegoggles/smssync/activity/events/AccountRemovedEvent.java` — absorbed into SyncEvent.AccountRemoved object
- `app/src/main/java/com/zegoggles/smssync/activity/events/AutoBackupSettingsChangedEvent.java` — absorbed into SyncEvent.AutoBackupSettingsChanged object
- `app/src/main/java/com/zegoggles/smssync/activity/events/FallbackAuthEvent.java` — absorbed into SyncEvent.FallbackAuth object (payload-less per U-019 design)
- `app/src/main/java/com/zegoggles/smssync/activity/events/MissingPermissionsEvent.java` — absorbed into SyncEvent.MissingPermissions (AC-12)
- `app/src/main/java/com/zegoggles/smssync/activity/events/SettingsResetEvent.java` — absorbed into SyncEvent.SettingsReset object
- `app/src/main/java/com/zegoggles/smssync/activity/events/ThemeChangedEvent.java` — absorbed into SyncEvent.ThemeChanged object

**Not deleted**: `activity/events/PerformAction.java` — its `Actions` enum is still imported by `SyncEvent.PerformActionRequested`. Retained per AC-13 grep-before-delete discipline; no live constructor calls remain in production source.

## Contract Adherence

CNTR-MODERNIZATION-006 compliance:

| Contract Item | Implementation | File:Line |
|---|---|---|
| `state: StateFlow<SyncState>` | `MutableStateFlow<SyncState>(BackupState())` | `FlowSyncStateRepository.kt:27-30` |
| `events: SharedFlow<SyncEvent>` | `MutableSharedFlow(replay=0, extraBufferCapacity=1, DROP_OLDEST)` | `FlowSyncStateRepository.kt:34-40` |
| `emitState(newState)` non-suspending | `_state.value = newState` | `FlowSyncStateRepository.kt:51-53` |
| `suspend emitEvent(event)` | `_events.emit(event)` | `FlowSyncStateRepository.kt:58-61` |
| `tryEmitEvent(event): Boolean` returns value, not swallowed | logged when false | `FlowSyncStateRepository.kt:67-73` |
| Cancel.Origin USER/SYSTEM, mayInterruptIfRunning() | `data class Cancel(val origin: Origin = Origin.USER)` | `SyncEvent.kt:34-37` |
| One-shot non-replay (AC-5) | `replay = 0` | `FlowSyncStateRepository.kt:35` |
| Initial state INITIAL | `BackupState()` seed | `FlowSyncStateRepository.kt:27` |
| App-scoped singleton (IC-1) | `App.syncStateRepository()` accessor | `App.java:140-148` |

## Test Results

```
./gradlew :app:testDebugUnitTest  — 591 tests PASS
./gradlew :app:jacocoTestCoverageVerification — PASS (≥70% service*, mail*, auth*)
./gradlew :app:assembleDebug — PASS, APK produced
```

New tests added (SyncStateRepositoryTest.kt):
- `lateCollector_receivesLastEmittedState` — AC-4 sticky semantics
- `lateCollector_doesNotReceivePriorEvent` — AC-5 one-shot non-replay
- `cancel_originUser_mayInterruptIfRunning_isFalse` — AC-10
- `cancel_originSystem_mayInterruptIfRunning_isTrue` — AC-10
- `tryEmitEvent_returnValue_isConsumedByCallerNotSwallowed` — AC-16
- `initialState_isBackupState_withIdleState` — AC-3

## Otto Removal Verification

```
grep -rn "com.squareup.otto|com.squareup:otto" app/src/main/java app/build.gradle
→ 0 matches

grep -rn "@Subscribe|@Produce" app/src/main/java
→ 0 live matches (comments only)

grep -rn "App\.register|App\.unregister|App\.post" app/src/main/java
→ 0 live matches (comments only)
```

## Integration Path

New code `FlowSyncStateRepository` is reached from:
1. `App.java:onCreate` → `new FlowSyncStateRepository()` stored in `syncStateRepositoryInstance`
2. All engine code (`SmsBackupService`, `SmsRestoreService`, `BackupTask`, `RestoreTask`) → `App.syncStateRepository().emitState(state)` (IC-3)
3. All UI code (MainActivity, StatusPreference, etc.) → `App.syncStateRepository()` accessor for collection and tryEmitEvent
4. `SmsJobService` → `App.syncStateRepository()` for Cancel emission and BackupState observation
5. Receivers → `App.syncStateRepository().tryEmitEvent()` (AC-17)

## Notes

- Worktree was initially on old master (ef97bfa9) which predates the modernization waves. The worktree was reset to `sdlc/modernization-plan` HEAD (32500558) to get the proper U-019 base with existing `SyncStateRepository`, `SyncEvent`, `SyncState` artifacts.
- `PerformAction.java` retained because `SyncEvent.PerformActionRequested` uses `PerformAction.Actions` per the U-019 contract. Its POJO semantics (the class itself with `action` + `confirm` fields) are entirely dead — no `new PerformAction(...)` calls remain in production code.
- `SyncEvent.AccountConnectionChanged` and `SyncEvent.FallbackAuth` are payload-less objects per U-019 design decisions (POJO payloads `connected: Boolean` and `showDialog: Boolean` were dropped). The two callers that depended on the payload were adapted: `AdvancedSettings` always emits AccountConnectionChanged on checkbox toggle (the toggle direction is implicit); `FallbackAuth(true)` case in `onActivityResult` was inlined to `showDialog(WEB_CONNECT)` directly.
- `lifecycle-runtime-ktx:2.6.2` and `lifecycle-viewmodel-ktx:2.6.2` added as new dependencies; `gradle/verification-metadata.xml` regenerated via `--write-verification-metadata sha256`.
