---
artifact_type: review-code
story_id: "sprint-001..005-consolidated"
verdict: "FAIL"
agent: "Code Reviewer"
timestamp: "2026-06-04"
blockers: 3
warnings: 6
---

# Code Review: SMS Backup+ Modernization — Sprint 001–005 Consolidated

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PARTIAL — ACL invariant holds in service.*, breaks in RestoreWorker.kt imports |
| Test coverage | Not evaluated (advisory review; test files not enumerated) |
| Code quality | Generally high; architecture coherent except for the dual-repository split |

## Verdict: FAIL

Three blockers found. Two are correctness bugs that silently break user-visible behaviour;
one is a resource leak that manifests on the abnormal service lifecycle path.

---

## Findings

### Blockers

#### B-1 — CRITICAL: Dual SyncStateRepository instances — MainActivity state callbacks never fire

**Severity:** Critical correctness bug  
**Files:** `App.java:160`, `di/EventModule.kt:37`, `activity/MainActivity.java:155`

**Issue:**  
`App.onCreate()` creates a manual static singleton:
```java
syncStateRepositoryInstance = new FlowSyncStateRepository();   // App.java:160
```
All service-side emitters (`SmsBackupService.moveToState()`, `SmsRestoreService.postError()`)
write to this instance via `App.syncStateRepository()`.

`MainActivity` obtains `MainViewModel` via Hilt:
```java
viewModel = new ViewModelProvider(this).get(MainViewModel.class);  // MainActivity.java:155
```
Because `MainActivity` is `@AndroidEntryPoint`, Hilt supplies `MainViewModel` with the
**Hilt-managed** `SyncStateRepository` produced by `EventModule.provideSyncStateRepository()`,
which creates a second, entirely independent `FlowSyncStateRepository` instance.

`MainActivityFlowHelper.startCollection()` collects from `viewModel.state` and `viewModel.events`
— which are backed by the Hilt instance. Service emits to the static instance. The two flows
are never connected.

**Consequence:**
- `MainActivity.onRestoreStateChanged()` is never called → `restoreDefaultSmsProvider()` never
  fires after restore → the user's default SMS app is not restored. This is a silent functional
  regression for every restore on Android KitKat+.
- `MainActivity.onBackupStateChanged()` is never called → `ActivityCompat.requestPermissions()`
  after a permission exception is never triggered from the activity.
- `MainActivity.onSyncEvent()` is never called → OAuth2 callback, account-added, theme-change,
  perform-action and fallback-auth events from `App.syncStateRepository()` are silently dropped
  by the Hilt-side flow.

**Fix:** Choose one canonical instance. The simplest correct fix is to inject
`SyncStateRepository` into `App` as an `@Inject` field (Hilt supports field injection on
`@HiltAndroidApp` classes) and assign it to `syncStateRepositoryInstance` in `onCreate()`,
replacing the `new FlowSyncStateRepository()` call. `EventModule` then becomes the single
source of truth and both the static accessor and the Hilt-injected `MainViewModel` share the
same instance. The `TODO U-022/MU-007` comments throughout `App.java` already flag this
cleanup; it was not completed.

---

#### B-2 — HIGH: `observeForever` observer leaked on abnormal service-destroy path

**Severity:** Resource leak, potential memory leak and spurious callbacks  
**Files:** `SmsBackupService.java:311–313`, `SmsRestoreService.java:270–272`

**Issue:**  
Both services register a LiveData observer with `observeForever`:
```java
WorkManager.getInstance(getApplicationContext())
    .getWorkInfosForUniqueWorkLiveData(uniqueWorkName)
    .observeForever(workInfoObserver);    // SmsBackupService.java:237, SmsRestoreService.java:202
```
When the worker reaches a terminal state, `tearDownObserverAndCollector(uniqueWorkName)` is
called with the work name and correctly calls `removeObserver(workInfoObserver)`.

However, when the service is destroyed without the worker reaching a terminal state first (OS
kill, crash, or process death), `onDestroy()` calls `tearDownObserverAndCollector(null)`. With
`uniqueWorkName == null`, this branch executes:
```java
} else if (workInfoObserver != null) {
    // uniqueWorkName not available (onDestroy path) — just null the reference
    workInfoObserver = null;    // SmsBackupService.java:312
}
```
The LiveData observer is **never removed** from the `WorkInfosForUniqueWorkLiveData` LiveData.
The observer holds a reference to the service instance (via the anonymous inner class and its
closures). WorkManager's internal `LiveData` may keep the observer alive indefinitely (for the
process lifetime), preventing GC of the service and all its fields.

Additionally, if the service is subsequently restarted and calls `observeForever` again with the
same LiveData, two observers are registered for the same work — resulting in duplicate callbacks
and double-calls to `backupStateChanged()`/`restoreStateChanged()`.

**Fix:** The `onDestroy` path must call `removeObserver` even without the unique-work-name.
Store the `LiveData` reference alongside `workInfoObserver` so `onDestroy` can remove it:

```java
// In service fields:
@Nullable private LiveData<List<WorkInfo>> workInfoLiveData;

// In registerBackupWorkInfoObserver / registerRestoreWorkInfoObserver:
workInfoLiveData = WorkManager.getInstance(getApplicationContext())
    .getWorkInfosForUniqueWorkLiveData(uniqueWorkName);
workInfoLiveData.observeForever(workInfoObserver);

// In tearDownObserverAndCollector, replace the null-uniqueWorkName branch:
if (workInfoObserver != null) {
    if (workInfoLiveData != null) {
        workInfoLiveData.removeObserver(workInfoObserver);
        workInfoLiveData = null;
    }
    workInfoObserver = null;
}
```

---

#### B-3 — MEDIUM: Dead k-9 / transport imports in `RestoreWorker.kt` violate ACL invariant

**Severity:** ACL contract violation, dead code  
**File:** `RestoreWorker.kt:37–48`

**Issue:**  
`RestoreWorker.kt` contains the following imports that are never used in executable code:

```kotlin
import com.zegoggles.smssync.mail.BackupImapStore    // line 37
import com.zegoggles.smssync.mail.PinnedCertStore    // line 41
import com.zegoggles.smssync.mail.TlsTrustPolicy     // line 42
import com.zegoggles.smssync.mail.transport.K9MailTransport  // line 44
import com.zegoggles.smssync.mail.transport.MailTransportConfig // line 48
```

No usage of these symbols appears in any executable expression in `RestoreWorker.kt` — they
appear only in comments/KDoc. `BackupImapStore` and `K9MailTransport` are k-9 adapter types
that CNTR-MODERNIZATION-007 explicitly excludes from `service.*` code. The presence of these
imports in `RestoreWorker.kt` violates the stated ACL invariant even if they are unused, and
risks accidental re-use during future development.

`BackupWorker.kt` does NOT have these imports — the issue is specific to `RestoreWorker.kt`
(stale imports from a partial migration, not removed after refactoring).

**Fix:** Remove all five dead imports from `RestoreWorker.kt`. The Kotlin compiler should emit
"unused import" warnings for these; if `-Werror` equivalent is active for Kotlin, this may
already be a build warning.

---

### Warnings

#### W-1 — Coexistence shim: `App.scheduler` not yet wired via Hilt

**File:** `App.java:125`, `App.java:235`

`scheduler` is constructed manually in `App.onCreate()` and exposed via the static accessor
`App.getScheduler(Context)`. `SchedulerModule` provides `BackupScheduler` to the Hilt graph,
but services access it via `App.getScheduler(this)` (not via `@Inject`). This means the Hilt
binding is unused in practice. The `TODO U-022` comment acknowledges the gap. Not a correctness
bug (both resolve to `WorkManagerScheduler`), but creates two allocation paths and makes the
Hilt binding untested.

#### W-2 — `FlowCollectHelper` coroutine scope leaks on application process lifetime

**File:** `FlowCollectHelper.kt:31`, `App.java:264`

`collectAutoBackupSettings` creates a new `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`
and returns a `Job`. The returned `Job` is not retained by `App.java` — it is discarded:
```java
FlowCollectHelper.collectAutoBackupSettings(syncStateRepositoryInstance, new Runnable() { ... });
```
The scope lives for the process lifetime (intentional for application-scoped collection), but
because `App` never retains the `Job`, it cannot cancel it in a test teardown. The `TODO`
comment acknowledges this. Low production risk; higher test risk (scope survives across tests
in Robolectric). The fix (`ProcessLifecycleOwner.get().lifecycleScope`) is already noted.

#### W-3 — `MainSettingsFlowHelper` scope not scoped to `onStop` lifecycle — mismatched with `onStart`

**File:** `MainSettings.java:53–58`, `MainSettings.java:62–68`

`flowCollectionJob` is started in `onStart()` but cancelled in `onDestroy()`. This means the
flow continues collecting while the fragment is stopped (invisible to the user), receiving events
that will attempt to update preference UI. For events that mutate preference summaries (e.g.
`onAutoBackupSettingsChanged`), this is harmless but wasteful. The pairing should be
`onStart`/`onStop` (or use `repeatOnLifecycle(STARTED)` as `MainActivityFlowHelper` does).

#### W-4 — Deprecated bridge overload `BackupConfig.retryWithStore` left in production code

**File:** `BackupConfig.java:62–65`

The `@Deprecated retryWithStore(MailTransport)` bridge method was added during U-026 migration
for call-site compatibility. Its Javadoc says "kept for call-site migration compatibility within
U-026." U-026 is complete; `BackupWorker.kt` already calls `retryWithTransport`. The deprecated
overload is dead code and should be removed to avoid confusion.

#### W-5 — `MailModule.provideMailTransportFactory` references `BackupImapStore.isValidUri` from `service`-adjacent code

**File:** `di/MailModule.kt:80`

`MailModule` calls `BackupImapStore.isValidUri(uri)` for URI validation. `BackupImapStore` is
in `mail.*` (not `service.*`) so this is not an ACL violation, but it creates a dependency on
the legacy `BackupImapStore` class from within the DI module. `K9MailTransport.isValidUri(uri)`
performs the same check and is the post-ACL-migration canonical form. Using it would allow
`BackupImapStore` to eventually be deleted; using `BackupImapStore.isValidUri` pins the legacy
class in place.

#### W-6 — `BackupTriggerWorker` constructs `Preferences` directly, bypassing Hilt

**File:** `worker/BackupTriggerWorker.kt:54`

`BackupTriggerWorker` is a plain `Worker` (not `@HiltWorker`) and calls
`Preferences(applicationContext)` directly in `doWork()`. This is not a Hilt worker, so it
cannot receive `@Inject` dependencies. It is not annotated `@AndroidEntryPoint` either.
This means `BackupTriggerWorker` will bypass the Hilt graph's `@Singleton` `Preferences`
instance and construct a fresh one per trigger. For a stateless reader of `SharedPreferences`
this is functionally harmless but inconsistent with the singleton contract established by
`PreferencesModule`. The fix is to annotate `BackupTriggerWorker` with `@HiltWorker` and
`@AssistedInject`, consistent with `BackupWorker` and `RestoreWorker`.

---

### Observations

#### O-1 — ACL boundary is correct except for RestoreWorker dead imports

The `service.*` layer contains no live k-9 imports. All k-9 types are correctly bounded to
`mail.*`, `mail.transport.*`, and `App.java` (K9MailLib). `BackupWorker.kt` imports are clean.
`RestoreWorker.kt` has five dead k-9/transport imports (B-3 above) but no live k-9 usage in
executable code.

#### O-2 — CredentialMigrationGate is correctly implemented

`CredentialMigrationGate` (U-035/BUG-003): the `prepare() → submit() → signalComplete()`
ordering guarantee is correct (happens-before via `submit()` and volatile `migrationComplete`).
The `awaitIfNeeded()` main-thread guard is correct. The executor is shut down inside the task's
`finally` block — it does not linger. The `resetForTest()` method is package-private.
Implementation is clean with no identified correctness issues.

#### O-3 — `observeForever` on main thread is correct

`SmsBackupService.registerBackupWorkInfoObserver()` notes that `observeForever` requires the
main thread and that `handleIntent()` is called on the main thread (via `onStartCommand()`).
This is correct — no threading violation here.

#### O-4 — Worker backoff cap (INV-3) is correctly split between scheduler and worker

`WorkManagerScheduler` sets `BackoffPolicy.EXPONENTIAL` with 30 s initial delay and documents
that the 300 s cap cannot be enforced via WorkManager API. `BackupWorker` and `RestoreWorker`
both implement the cap at the worker layer by checking `runAttemptCount` and returning
`Result.failure()` when the effective delay would exceed `BACKOFF_MAX_SECS`. The arithmetic
`BACKOFF_INITIAL_SECS shl runAttempt` is correct for attempt tracking. Implementation is clean.

#### O-5 — RestoreWorker checkpoint write ordering is correct

The U-016 durable checkpoint write-before-advance ordering (insert → write checkpoint → call
interceptor → advance loop) is correctly implemented in both `insertSmsValues` and
`importCallLogValues`. The checkpoint is written after a confirmed `insert()` returns a non-null
URI, using `checkpointStore.write()` (synchronous `SharedPreferences.commit()`) before
`insertInterceptor.afterInsert()`. The invariant is maintained.

#### O-6 — `FlowSyncStateRepository` DROP_OLDEST policy is a reasonable trade-off

`FlowSyncStateRepository` uses `BufferOverflow.DROP_OLDEST` with `extraBufferCapacity=1`. Under
heavy burst emission (unlikely in this app), events could be dropped. The contract requires that
`tryEmitEvent` return value is logged when false. This is correctly implemented. The policy is
appropriate for this use case.

#### O-7 — `SmsRestoreService` wakelock pairing is correct on normal path

`acquireLocks()` is called before enqueue (line 133) and `releaseLocks()` is called in the
terminal state handler `restoreStateChanged()` (line 335). On the abnormal path (`scheduleRestore`
returns null), `releaseLocks()` is called at line 157 before `postError`. The normal-path
pairing is correct. The abnormal-path pairing is also correct.

#### O-8 — `OAuth2AccessTokenProgress` dialog correctly cancels its collection job

`Dialogs.OAuth2AccessTokenProgress` starts its `collectionJob` in `onAttach()` and cancels it
in `onDetach()` (lines 265–281). The `DialogsFlowHelper` creates a `SupervisorJob` scope that is
only referenced via the returned `Job` — cancelling the Job cancels the scope. This is correct.

#### O-9 — Remaining AsyncTask usage is scoped to non-service code

Two `AsyncTask` usages survive in `tasks/OAuth2CallbackTask.java` and
`fragments/PinCertificateEnrollmentFlow.java`. Neither is in the backup/restore service path
(U-031 deleted `BackupTask` and `RestoreTask`). `package-info.java` in `tasks/` notes the
deprecation. These are pre-existing; their removal is tracked in future milestones.

#### O-10 — `MainViewModelFactory` is orphaned dead code

`MainViewModelFactory.kt` exists and constructs `MainViewModel` manually. `MainActivity` now
obtains `MainViewModel` via `new ViewModelProvider(this).get(MainViewModel.class)` using Hilt's
factory (U-024). `MainViewModelFactory` is never instantiated. It should be deleted.

---

## Patterns Verified

- [x] Follows existing code patterns — broadly yes, with exceptions noted in B-1/W-1
- [x] Error handling is appropriate — exception translation chain (K9→ACL) is complete and correct
- [x] Tests cover new functionality — not evaluated (advisory review scope)
- [x] No hardcoded values that should be configurable — BACKOFF_INITIAL_SECS/MAX_SECS are constants
- [ ] No unnecessary complexity — dual-repository pattern (B-1) adds hidden complexity

## Integration Verified

- [x] New code is reachable from production entry points — BackupWorker/RestoreWorker registered via HiltWorkerFactory
- [x] Registries/dispatch maps updated for new implementations — HiltWorkerFactory, @HiltWorker annotations present
- [x] Function signatures match at all call sites — verified for MailTransport ACL, BackupConfig, RestoreConfig
- [ ] No dead code introduced — RestoreWorker.kt dead imports (B-3), MainViewModelFactory.kt orphaned (O-10)
- [x] Integration path documented in implementation-log.md — N/A (advisory review, no single implementation log)

## Regression Check

Not applicable to consolidated advisory review (no single implementation-log.md).

## Contract Verification

- [ ] All consumers of modified interfaces identified — see B-1: MainViewModel subscribers do not receive service state
- [ ] Field names, event names, data shapes agree between producer and consumer — N/A for this finding
- [ ] No contract mismatches between backend and frontend — B-1 is a contract mismatch between service emitter and UI consumer

---

## Prioritized Fix List

| Priority | ID  | File(s) | Description |
|----------|-----|---------|-------------|
| P0 | B-1 | App.java:160, di/EventModule.kt:37, MainActivity.java:155 | Dual SyncStateRepository — service state never reaches MainActivity; restoreDefaultSmsProvider never fires |
| P0 | B-2 | SmsBackupService.java:311, SmsRestoreService.java:270 | observeForever observer not removed on onDestroy path |
| P1 | B-3 | RestoreWorker.kt:37–48 | Dead k-9 / transport imports violate ACL invariant |
| P2 | W-6 | worker/BackupTriggerWorker.kt:54 | Not a @HiltWorker; creates non-singleton Preferences |
| P2 | W-3 | MainSettings.java:53,62 | Flow collection mismatched lifecycle (onStart/onDestroy should be onStart/onStop) |
| P3 | W-4 | BackupConfig.java:62 | Dead @Deprecated retryWithStore bridge overload |
| P3 | O-10 | activity/MainViewModelFactory.kt | Orphaned dead class, never instantiated |
| P4 | W-1 | App.java:125,235 | App.scheduler not wired via Hilt — duplicate allocation path |
| P4 | W-5 | di/MailModule.kt:80 | Use K9MailTransport.isValidUri instead of BackupImapStore.isValidUri |
