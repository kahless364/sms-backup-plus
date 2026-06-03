---
artifact_type: plan
story_id: U-020
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# Plan: U-020 — Otto Bus Removal

## Approach

Branch-by-abstraction Step 2 (U-019 was Step 1 Facade).

1. **FlowSyncStateRepository** — new live implementation replacing the Otto-delegating
   `DefaultSyncStateRepository`. Backed by `MutableStateFlow<SyncState>` (sticky) and
   `MutableSharedFlow<SyncEvent>(replay=0, extraBufferCapacity=1, DROP_OLDEST)`.

2. **App.java** — swap to `FlowSyncStateRepository`, delete `Bus bus`, delete static
   `register/unregister/post` helpers, migrate `autoBackupSettingsChanged` to
   Application-scoped coroutine via `FlowCollectHelper`.

3. **ServiceBase** — remove `App.register(this)` / `App.unregister(this)`.

4. **SmsBackupService / SmsRestoreService** — delete static `service` fields and
   `isServiceWorking()` / `isServiceIdle()` methods; remove `@Produce` / `@Subscribe`;
   replace `App.post(state)` with `App.syncStateRepository().emitState(state)`.

5. **BackupTask / RestoreTask** — remove `App.register` / `App.unregister`;
   replace `@Subscribe canceled()` with `BackupCancelCollector` Kotlin flow-first
   subscription; replace `App.post(state)` with `emitState + direct service callback`.

6. **SmsJobService** — remove `App.register/unregister`; replace `@Subscribe backupStateChanged`
   with `SmsJobServiceFlowHelper` StateFlow observation; replace `App.post(new CancelEvent(SYSTEM))`
   with `tryEmitEvent(new SyncEvent.Cancel(SYSTEM))`.

7. **UI layer** (MainActivity, StatusPreference, Dialogs, AdvancedSettings, MainSettings,
   OAuth2WebAuthActivity) — add `MainViewModel`, replace `App.register/unregister` with
   `repeatOnLifecycle` / scoped-coroutine collection; replace `App.post(event)` with
   `syncStateRepository().tryEmitEvent(SyncEvent.*)`.

8. **Receivers / Tasks** (PackageReplacedReceiver, OAuth2CallbackTask, RedirectReceiverActivity)
   — replace `App.post(...)` with `tryEmitEvent`.

9. **Delete** `DefaultSyncStateRepository.kt`, `CancelEvent.java`, 8 `activity/events/*.java`
   POJOs. `PerformAction.java` retained (its `Actions` enum referenced by `SyncEvent.PerformActionRequested`).

10. **Build** — remove `com.squareup:otto:1.3.8`; add `lifecycle-runtime-ktx:2.6.2` + `lifecycle-viewmodel-ktx:2.6.2`.

11. **Tests** — add `SyncStateRepositoryTest.kt` (6 tests); update existing tests.
