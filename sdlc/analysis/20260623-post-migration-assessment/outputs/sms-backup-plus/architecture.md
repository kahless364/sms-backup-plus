# Findings — Architecture & Code Quality (sms-backup-plus)

**Dimension:** Architecture & Code Quality · **Score: 80/100 · Grade: B−**
*Justification:* A genuinely well-executed modernization with an airtight ACL boundary and disciplined coroutine workers, held back by a half-finished Hilt migration and a retained Service+Worker dual-dispatch layer that adds real, removable complexity.

## Strengths
- **ACL boundary is airtight** — zero `com.fsck.k9.*` imports in `service.*`; all k-9 types confined to `K9MailTransport.java`; composition-based `BackupImapStoreDelegate` (`K9MailTransport.java:414`) resolves the checked-exception/port conflict cleanly.
- **Coroutine workers are correct** — cooperative cancellation (`BackupWorker.kt:315` `ensureActive()`), `finally` cursor cleanup, durable checkpoint write-ordering (`RestoreWorker.kt:455-459`); the BUG-010 watermark invariant is **compiler-enforced** via the `appendMessages` return value (`BackupWorker.kt:335-345`).
- **State management is clean** — `FlowSyncStateRepository` uses StateFlow + SharedFlow(replay=0, DROP_OLDEST), logs dropped events (`FlowSyncStateRepository.kt:68-74`); `MainViewModel` is a thin `@HiltViewModel`.
- **First-class test seams** — `MailTransportFactory`, `RestoreInsertInterceptor`, `forceFreshConnection` enable fault injection without touching production paths.

## Weaknesses / Tech Debt
- **AR-001 [High] Hilt migration is half-done** — 4 modules still `@Provides`-return manually-constructed instances (`SchedulerModule.kt:32`, `EventModule.kt:41`, `ContactsModule.kt:32`) each tagged `TODO(U-023)`; `EventModule.provideSyncStateRepository()` returns `App.syncStateRepository()` — a Hilt singleton aliased to a Java static (`App.java:101,239`), the explicit BUG-004 patch. The DI graph and static-singleton world coexist.
- **AR-002 [High] Dual-dispatch layer survives** — `SmsBackupService` (~505 lines) + `SmsRestoreService` (~378) + `ServiceBase` (~311) remain manifest-registered and are the live UI entry point (`MainActivity.java:420 startService(...)`); they no longer execute backups (delegate to `scheduleManual` → worker) but add a second scheduling surface + a LiveData↔Flow bridge (~1,200 lines of transitional glue).
- **AR-003 [Medium] God-class MainActivity (~615 lines)** mixes navigation, SMS-role negotiation, dialog flow, service dispatch; still uses manual `MainViewModelFactory` (`TODO U-022`) despite a Hilt-ready `MainViewModel`.
- **AR-004 [Medium] Per-run object graph hand-built in workers** — `PersonLookup`, `ContactAccessor`, `MessageConverter`, `TokenRefresher`, `CalendarSyncer` are `new`-ed inside `BackupWorker.kt:211-233`, defeating DI for the engine core.
- **AR-005 [Low] Stale seams/docs** — `WorkManagerScheduler.observe()` returns a hardcoded state ("until U-020 lands" — already landed); class KDoc still describes a removed `LegacyScheduler`; `Configuration.Provider` reflective fallback carries test-env branching in production (`App.java:224`).

## Improvement Items
1. Finish the Hilt cutover (U-023): `@Binds` the 4 providers, `@Inject` constructors for scheduler/repo/contacts adapter, delete `App.syncStateRepository()` + the static singleton (removes the BUG-004 aliasing). *(M, high)*
2. Retire the legacy Services: move foreground/notification + WorkInfo-observer into the worker (`setForeground`/`ForegroundInfo`), delete the 3 service classes + manifest entries, have MainActivity call the scheduler directly (~1,200 lines removed). *(M-L, high)*
3. Decompose MainActivity + inject the worker engine graph; replace `MainViewModelFactory` with `by viewModels()`. *(M, med)*
4. Clean stale seams/docs. *(S, low)*
