---
artifact_type: review-security
story_id: "U-048"
verdict: "PASS"
agent: "Security Reviewer"
timestamp: "2026-06-23"
blockers: 0
warnings: 0
---

# Security Review: U-048 — Complete Hilt DI Cutover (@Binds/@Provides)

## Review Summary

U-048 converts three DI modules from manual `@Provides`-new patterns to `@Binds` abstract
modules and adds `@Inject` constructors to `FlowSyncStateRepository`, `WorkManagerScheduler`,
and `PeopleApiContactsAdapter`. The net security surface change is zero: no new exported
components, no credential-handling changes, no TLS changes, no new logging of sensitive data,
and the singleton-instance invariant for `SyncStateRepository` is strengthened (BUG-004
structurally fixed). This story is security-neutral.

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

None.

### Low (informational)

**Static bridge `App.syncStateRepository()` retained** — `App.java` continues to expose a
static `syncStateRepositoryInstance` field and accessor. This is a known transitional pattern
documented in the story. It does not expose any credential or sensitive data; the repository
holds `BackupState`/`RestoreState` progress objects and sync events, not credentials. The
bridge is slated for removal once all legacy callers migrate to `@Inject`. No action required
for this story.

---

## Security Checklist

- [x] No hardcoded credentials or secrets — `@Inject` constructors accept only `Context` and
      `Preferences`; no secrets cross the DI boundary at construction time.
- [x] Input validation on all user inputs — no user input paths introduced or changed.
- [x] Output encoding prevents injection attacks — no output encoding surface changed.
- [x] Authentication tokens handled securely — `AuthPreferences`/`EncryptedPrefsSecretStore`
      are not touched by this story; `OAuth2Client`, `TokenRefresher`, and credential storage
      are unchanged.
- [x] Authorization checks on all protected resources — `BackupBroadcastReceiver` and all
      broadcast receivers are unchanged. No new exported components introduced.
- [x] Sensitive data encrypted at rest and in transit — no change to encryption or TLS paths.
- [x] Error messages don't leak internal details — no new logging of sensitive values;
      `debugSensitive()` remains `false` in `App.java`.
- [x] Dependencies have no known critical vulnerabilities — no dependency changes in this story.

## Verification Notes

**Files read in full:** `EventModule.kt`, `SchedulerModule.kt`, `ContactsModule.kt`,
`App.java`, `WorkManagerScheduler.kt` (constructor section), `FlowSyncStateRepository.kt`
(constructor section), `PeopleApiContactsAdapter.java` (constructor section).

**Specific verification against implementation log claims:**

1. *"No `new FlowSyncStateRepository()`, `new WorkManagerScheduler(`, or
   `new PeopleApiContactsAdapter()` in production source files"* — verified by reading all three
   `@Binds` modules: they contain no manual construction. `App.java` line 178 assigns
   `syncStateRepositoryInstance = syncStateRepository` from the Hilt-injected field, not from
   `new FlowSyncStateRepository()`.

2. *"EventModule no longer calls `App.syncStateRepository()`"* — verified: `EventModule.kt`
   contains only `@Binds @Singleton abstract fun bindSyncStateRepository(impl:
   FlowSyncStateRepository): SyncStateRepository`. No call to `App.syncStateRepository()`.

3. *"Hilt `@Singleton` scope guarantees exactly one instance"* — verified at the module level.
   All three `@Binds` methods carry `@Singleton` annotation under `SingletonComponent`.

4. *"No parallel construction paths"* — `App.java` `getWorkManagerConfiguration()` uses
   `hiltWorkerFactory` (Hilt-injected). The `if (hiltWorkerFactory != null)` guard is a
   test-path defensive null-check, not a parallel production factory. Not a security concern.

No credential logging, no new exported components, no TLS changes. No security findings.
