---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - component-design
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-012
  - REQ-MODERNIZATION-013
  - REQ-MODERNIZATION-014
related_stories: []
related_design_docs: []
integration_contracts:
  - CNTR-MODERNIZATION-007
  - CNTR-MODERNIZATION-004
change_records: []
type: pattern
id: DES-MODERNIZATION-012
title: Modernization Debt Closure — ACL Purity, Legacy-Path Removal, Hilt Completion
domain: modernization
---

# DES-MODERNIZATION-012: Modernization Debt Closure — ACL Purity, Legacy-Path Removal, Hilt Completion

## Overview

This is one combined design covering three tightly-coupled refactoring requirements that
close the bounded technical debt deferred at the end of sprint-001 (the SMS Backup+
modernization sprint, 29/29 stories complete):

- **REQ-MODERNIZATION-012** — Seal the `MailTransport` ACL exception boundary so that
  **zero** `com.fsck.k9.*` type references (import, FQN catch, FQN throws, FQN local type)
  remain in any `service.*` class. The two surviving FQN-catch residuals are
  `BackupTask.java:308` (catching `com.fsck.k9.mail.MessagingException` from
  `MessageConverter.convertMessages()`) and `ServiceBase.java:173` (catching it from the
  `new K9MailTransport(...)` call).
- **REQ-MODERNIZATION-013** — Delete the legacy `AsyncTask` execution path (`BackupTask`,
  `RestoreTask`, their tests, and their `jacocoFileFilter` exclusions), after migrating the
  still-live manual backup/restore dispatch (`MainActivity` → `SmsBackupService` /
  `SmsRestoreService` → `getBackupTask()`/`getRestoreTask()`) onto the WorkManager
  (`BackupWorker`/`RestoreWorker`) path that has been the sole *scheduled* production engine
  since U-017.
- **REQ-MODERNIZATION-014** — Apply `@AndroidEntryPoint` to the surviving services, remove
  the `ServiceBase` null-check coexistence shims and the `getBackupTask()`/`getRestoreTask()`
  factory methods, fix the latent `CalendarSyncer` Dagger `MissingBinding`, and migrate the
  Robolectric service tests off the anonymous-subclass / `setupService` patterns that are
  incompatible with the Hilt service lifecycle.

All three touch the same files (`ServiceBase`, `SmsBackupService`, `SmsRestoreService`,
`BackupTask`, `RestoreTask`) and **must be sequenced**. The heart of this design is the
explicit ordering and the per-shared-file coordination (`## Sequencing & Dependencies`).

This is internal refactoring with **no user-facing behavior change** (EPIC-MODERNIZATION-005
Success Criterion 10). The non-trivial behavior-preservation points — foreground-service
promotion, the `FULL_WAKE_LOCK` on restore, the `MailException.getCause()` chain that
`State.getDetailedErrorMessage()` reads, and cooperative cancellation — are flagged
explicitly throughout.

## Context

### What sprint-001 left open (verified this session)

Sprint-001 reached the architectural target state but documented three bounded residuals
(EPIC-MODERNIZATION-005 §Description). Each is verified against current source below; every
claim in this design about existing code was read this session, not inherited.

**1. The ACL has two FQN-catch holes in `service.*` (REQ-012).** U-026 removed every
`import com.fsck.k9.*` line from `service.*`, satisfying the *import-only* grep, but left two
fully-qualified-name catch clauses that keep the engine compile-coupled to k-9:

- `BackupTask.java:305-310` (verified): `result = converter.convertMessages(...)` is wrapped
  in `try { ... } catch (com.fsck.k9.mail.MessagingException e) { throw new MailException(e); }`.
  The leak source is `MessageConverter.convertMessages(Cursor, DataType)` whose signature is
  `throws MessagingException` (`MessageConverter.java:120-121`, verified).
- `ServiceBase.java:170-176` (verified): `return new K9MailTransport(getApplicationContext(),
  config);` is wrapped in `catch (com.fsck.k9.mail.MessagingException e) { throw new
  MailException(e); }`. The leak source is `K9MailTransport`'s public constructor signature
  `public K9MailTransport(Context, MailTransportConfig) throws MailException, MessagingException`
  (`K9MailTransport.java:117-118`, verified).

A grep for `com.fsck.k9` over `service/` does **not** return only two lines. Re-verified this
session — `grep -rn "com\.fsck\.k9" .../service/` returns **20 matches: 2 compile-coupling FQN
catches** (`BackupTask.java:308`, `ServiceBase.java:173`) **plus 18 comment/javadoc occurrences**.
REQ-012 AC-1 demands the grep produce **zero** output lines and explicitly names "javadoc
`{@code}` blocks that would couple at compile time"; AC-2 demands zero across the whole `service/`
tree including `service/state/`. **Fixing only the two catches FAILS AC-1/AC-2 literally** — the
18 comment/javadoc residuals must also be scrubbed (see the dedicated REQ-012 grep-to-zero
decision below). The full enumeration of the 18 (re-greped this session):

| File | Lines | Nature | Disposition |
|------|-------|--------|-------------|
| `BackupTask.java` | 8, 51, 286 | "all com.fsck.k9.* imports removed" / "No `{@code com.fsck.k9.*}`…" annotations | Vanish — `BackupTask.java` is **deleted by REQ-013** |
| `RestoreTask.java` | 14, 55, 134 | same annotation pattern | Vanish — `RestoreTask.java` is **deleted by REQ-013** |
| `BackupWorker.kt` | 55 | "No `{@code com.fsck.k9.*}` import remains in this…" | **Survives — must be reworded by REQ-012** |
| `RestoreWorker.kt` | 30, 66 | "all com.fsck.k9.* imports removed" / "No `{@code com.fsck.k9.*}`…" | **Survives — must be reworded by REQ-012** |
| `SmsBackupService.java` | 27, 78 | "import com.fsck.k9.mail.MessagingException removed" / "`{@code com.fsck.k9.mail.MessagingException}` import removed" | **Survives — must be reworded by REQ-012** |
| `SmsRestoreService.java` | 11, 12, 41, 42 | "com.fsck.k9.mail.MessagingException import removed" / BinaryTempFileBody note | **Survives — must be reworded by REQ-012** |
| `State.java` (`service/state/`) | 6, 7, 8 | "U-026 AC-7: com.fsck.k9.mail.*Exception import removed" | **Survives — must be reworded by REQ-012** |

So **12 of the 18 comment/javadoc residuals live in files that survive REQ-013** (1 in
`BackupWorker.kt`, 2 in `RestoreWorker.kt`, 2 in `SmsBackupService.java`, 4 in
`SmsRestoreService.java`, 3 in `State.java`) and therefore **must be reworded by REQ-012 to reach
grep-zero**; the other **6** (3 in `BackupTask.java`, 3 in `RestoreTask.java`) vanish when those
files are deleted in REQ-013. (The 2 **code** catches are `BackupTask.java:308` — also gone with
the REQ-013 deletion — and `ServiceBase.java:173`, deleted by the REQ-012 ctor-relocation fix.)
The `MailTransport` port interface itself (`MailTransport.java`) is already
clean — its method signatures and `throws` clauses are app-owned only (verified). CNTR-MODERNIZATION-007
Validation Rule 1 demands `com.fsck.k9` in `service/` → **0**; REQ-012 tightens the U-026
import-only criterion to *all reference forms, including comments and javadoc*.

**2. The `AsyncTask` path is duplicated and partially live (REQ-013).** `BackupWorker.kt`
and `RestoreWorker.kt` are `@HiltWorker` `CoroutineWorker`s and are the sole *scheduled*
execution path (U-017 flipped `WorkManagerScheduler` to be the only `BackupScheduler`
binding; `WorkManagerScheduler.kt` verified). But `BackupTask`/`RestoreTask` are **not fully
dead**: `MainActivity.startBackup()` (`MainActivity.java:405-407`) calls
`startService(new Intent(this, SmsBackupService.class).setAction(backupType.name()))` and
`MainActivity.startRestore()` (`MainActivity.java:409-434`) calls
`startService(new Intent(this, SmsRestoreService.class))`. Those services dispatch to the
AsyncTasks via `SmsBackupService.getBackupTask().execute(...)` (`SmsBackupService.java:145`)
and `SmsRestoreService.getRestoreTask().execute(config)` (`SmsRestoreService.java:115`). The
same IMAP backup/restore logic now exists in both the AsyncTask classes and the Workers
(both rewired onto `MailTransport` in U-026) — a live divergence risk. The four
`jacocoFileFilter` exclusions for `BackupTask`/`RestoreTask` are at `app/build.gradle:229-232`
(verified); the workers' exclusions at `:219-222` are **out of scope** (REQ-013 Note).

**3. Two-mechanism construction; `@AndroidEntryPoint` deferred (REQ-014).** `ServiceBase`
declares `@Inject Preferences injectedPreferences` / `@Inject AuthPreferences
injectedAuthPreferences` (`:84-85`) but `getPreferences()`/`getAuthPreferences()`
(`:186-200`) still guard them with `!= null ? injected : new ...()` fallbacks because
`@AndroidEntryPoint` was never applied — it breaks `SmsBackupServiceTest`'s anonymous-subclass
pattern. `SmsBackupService.getBackupTask()` (`:202-240`) and
`SmsRestoreService.getRestoreTask()` (`:129-143`) hand-build the entire task collaborator
graph with fully-qualified `new` expressions. The Hilt graph owns the workers and the
`App`-level singletons; the services build their task graphs by hand.

### The latent Dagger `MissingBinding` (verified — addressed by this design)

REQ-014 flags a latent risk. Confirmed this session: `CalendarSyncer`
(`CalendarSyncer.java:32-41`) has an `@Inject` constructor whose second parameter is a raw
`long calendarId`. Its javadoc claims `calendarId` is "supplied as `@Named("calendarId")
long` in EngineModule" — but **there is no `EngineModule`** (Glob `**/EngineModule*` → no
files) and **no Hilt binding for `long` or `@Named("calendarId") long`** exists anywhere in
`di/` (grep over `di/` for `calendarId`/`@Provides`/`@Binds` → no match; verified
`PreferencesModule.kt` provides only `Preferences`/`AuthPreferences`/`DataTypePreferences`).
This compiles **today only because the Hilt graph is never asked to build `BackupTask`** —
its `@Inject` constructor takes `Lazy<CalendarSyncer>`, but `BackupTask` is hand-constructed
in `SmsBackupService.getBackupTask()` (the `Lazy` is supplied as a hand-written lambda at
`SmsBackupService.java:230-237`, which calls `new CalendarSyncer(...)` directly, bypassing
Dagger). The moment `BackupTask` is asked of the graph — or the moment the deletion of
`getBackupTask()` removes the hand-wired lambda without `BackupTask` also being gone — Dagger
would attempt to resolve `Lazy<CalendarSyncer>` → `CalendarSyncer` → unbound `long`, producing
`[Dagger/MissingBinding] long cannot be provided` and failing AC-2 of REQ-014. The sequencing
below resolves this by **deleting `BackupTask` (and thus `CalendarSyncer`'s only `@Inject`
consumer) in REQ-013 before `@AndroidEntryPoint` is applied in REQ-014**, so the broken
`CalendarSyncer` `@Inject` constructor never enters a live graph.

## System Architecture

### Target end-state

The architectural target (EPIC-MODERNIZATION-005 §Description, modernization plan) is three
invariants, each closed by one requirement:

1. **Zero k-9 in the engine.** `grep -rn "com\.fsck\.k9" app/src/main/java/.../service/` →
   0, for every reference form. All k-9 transport/MIME types live only in `mail.transport.*`
   (the `K9MailTransport` adapter) and the Preserved-Core converters in `mail.*` (the
   documented bounded residual, DES-MODERNIZATION-009 ADR-009-B). (REQ-012)
2. **A single execution path.** `BackupWorker`/`RestoreWorker` (`CoroutineWorker`) are the
   only backup/restore engine. `BackupTask`/`RestoreTask` (`AsyncTask`) are gone; no second
   copy of the IMAP loop survives. (REQ-013)
3. **A single construction source of truth.** Every service collaborator comes from the Hilt
   `SingletonComponent` graph (member injection on `@AndroidEntryPoint` services, assisted
   injection on `@HiltWorker` workers). No `getBackupTask()`/`getRestoreTask()` factories,
   no `ServiceBase` null-check fallbacks. (REQ-014)

### Before / after — backup & restore dispatch flow

**Before (current, verified):** two dispatch surfaces feed two engines.

```
SCHEDULED  (BroadcastReceiver / BootReceiver / SmsBroadcastReceiver / content-trigger)
   → App.getScheduler(ctx)  [WorkManagerScheduler]
       → scheduleImmediate/Incoming/Regular/Bootup → BackupWorker  (CoroutineWorker, LIVE)
       → scheduleRestore(RestoreSchedulerConfig)   → RestoreWorker (CoroutineWorker, LIVE)

MANUAL  (MainActivity UI button)
   → startBackup(MANUAL|SKIP)  → startService(SmsBackupService, action=type)
        → SmsBackupService.handleIntent → backup() → getBackupTask().execute(BackupConfig)
            → BackupTask  (AsyncTask, LIVE via this path only)         ── DUPLICATE LOGIC
   → startRestore()            → startService(SmsRestoreService)
        → SmsRestoreService.handleIntent → getRestoreTask().execute(RestoreConfig)
            → RestoreTask (AsyncTask, LIVE via this path only)         ── DUPLICATE LOGIC
```

**After (target):** one dispatch port, one engine. The services are retained (they still
own foreground promotion + lock lifecycle in the manual path) but **delegate to the
`BackupScheduler` port** instead of constructing AsyncTasks.

```
SCHEDULED  (unchanged)
   → WorkManagerScheduler → BackupWorker / RestoreWorker  (CoroutineWorker, sole engine)

MANUAL  (MainActivity UI button — unchanged call into the service)
   → startBackup(MANUAL|SKIP)  → startService(SmsBackupService, action=type)
        → SmsBackupService.handleIntent → backup()
            → getScheduler().scheduleImmediate()      ── enqueues BackupWorker (single engine)
   → startRestore()            → startService(SmsRestoreService)
        → SmsRestoreService.handleIntent
            → getScheduler().scheduleRestore(cfg)      ── enqueues RestoreWorker (single engine)
   (services retain @AndroidEntryPoint Hilt injection; BackupTask/RestoreTask deleted)
```

This design selects **Option (a) from REQ-013 AC-1**: the services delegate to
`WorkManagerScheduler`, rather than rewriting `MainActivity` to bypass the services
(Option (b)). Rationale, with the behavior-preservation analysis, is in `## Integration
Design` and `## Trade-offs`. The `MainActivity` `startService(...)` call sites are **not**
changed (REQ-013 §"Scope ... conditionally" keeps `MainActivity` migration out of scope; the
services and their `foregroundServiceType="dataSync"` manifest entries are retained).

### How the three requirements move the system

| Req | Layer touched | Net structural change |
|-----|---------------|------------------------|
| 012 | `mail.transport.*`, `mail.MessageConverter` (producer side); `BackupTask`/`ServiceBase` catch sites (consumer side) | Translation moves *below* the port: `K9MailTransport` ctor declares only `throws MailException`; converter access wrapped in `mail.transport`. The two FQN catches are deleted. |
| 013 | `MainActivity`-fed services dispatch; `BackupTask`/`RestoreTask` + tests; `BackupCancelCollector`; `jacocoFileFilter` | Manual dispatch delegates to the scheduler; AsyncTask classes and their cancel collector deleted; coverage exclusions removed. |
| 014 | `ServiceBase`, `SmsBackupService`, `SmsRestoreService`, service tests, `CalendarSyncer` | `@AndroidEntryPoint` applied; shims removed; `CalendarSyncer` binding fixed (or its `@Inject` removed); tests migrated to constructor injection. |

## Component Design

Each change below is an architecture decision in Context / Decision / Consequences form,
keyed to the requirement and the verified source location.

### REQ-012 — `MessageConverter` exception wrapping (primary leak)

- **Context.** `MessageConverter.convertMessages(Cursor, DataType)` declares `throws
  com.fsck.k9.mail.MessagingException` (`:120-121`). This forces `BackupTask.java:306-310` to
  catch the k-9 FQN. `MessageConverter` is in `mail.*` (a permitted k-9 zone), but its public
  checked-throws escapes into `service.*` callers. Note both engines call it: `BackupTask`
  (the FQN-catch) **and** `BackupWorker.kt:310` (`val result = converter.convertMessages(...)`,
  inside a Kotlin function whose enclosing `try` catches `MailException`). Scope verified via
  grep: `convertMessages` is called at `BackupTask.java:307` and `BackupWorker.kt:310`; after
  REQ-013 deletes `BackupTask`, only the worker call site remains — but REQ-012 lands **first**
  (per sequencing), so both must compile under the new signature.
- **Decision.** Apply **REQ-012 AC-6 option (a)**: change `convertMessages()` to translate the
  `MessagingException` internally and re-throw an **app-owned** exception, so its public
  signature declares `throws MailException` (or a new `MessageConversionException extends
  MailException`). Internally the method keeps using k-9 `Message`/`Flag` (permitted in
  `mail.*`); only the *thrown* type changes. The original `MessagingException` is preserved as
  the cause (`new MailException(e)` — `MailException(Throwable)` exists, `MailException.java:44`).
  - **Backward-compatibility constraint (verified):** `convertMessages` is also called inside
    `mail.transport.K9MailTransport.importMessageBody()` only *indirectly* — actually
    `importMessageBody` calls `converter.getDataType(message)` and
    `converter.messageToContentValues(message)` (`K9MailTransport.java:287-288`), **not**
    `convertMessages`. So changing `convertMessages`'s thrown type does not affect
    `importMessageBody`. The only `convertMessages` callers are `BackupTask` and `BackupWorker`
    (grep-confirmed). Both catch `MailException` already at an outer level, so option (a) is
    compile-safe. `MailException` does **not** need to be added to the `service.*` import set of
    `BackupWorker` (it already imports it, `BackupWorker.kt:34`).
  - The design **prefers option (a) over option (b)** (a `mail.transport` wrapper method): a
    wrapper would require `BackupWorker`/`BackupTask` to route the converter call through the
    transport, complicating the loop and the `BackupConfig` for no benefit, since the converter
    is already an injected collaborator, not a transport concern.
- **Consequences.** `BackupTask.java:306-310`'s inner `try/catch (com.fsck.k9...)` collapses to
  a direct `result = converter.convertMessages(...)` (the outer `catch (MailException e)` at
  `:175` already handles it). `BackupWorker.kt:310` is unchanged at the call site; the
  `MailException` it can now throw is already caught by the enclosing `try` (`:259`). One k-9
  FQN reference in `service.*` is eliminated. **`MessageConverterTest` test-update scope
  (concrete, AC-9/AC-10):** the direct callers of `convertMessages` in test source are
  `MessageConverterTest.java:121,125,143,147,165,169` (six call sites across the result-mapping
  tests) — re-greped this session. **None of these assert that `MessagingException` is thrown**;
  they assert on the returned `ConversionResult`, and the method's checked `throws` only appears
  on the test methods' own `throws` clauses. Therefore the option-(a) signature change requires
  **only** updating those test-method `throws` clauses (`throws MessagingException` →
  `throws MailException`); there is **no result-assertion change**. A new negative test that the
  converter throws `MailException` carrying a `MessagingException` cause would be additive
  coverage, not a required edit to the six existing sites. (`BackupTaskTest` also mocks
  `convertMessages` at `:117,128,153,166,180,214,237`, but `BackupTaskTest` is **deleted** by
  REQ-013, so those mocks are moot.)
- **AC-5 auth-escalation safety (re-verified this session).** Option (a)'s wrapping of every
  `MessagingException` from `convertMessages` as `MailException` **cannot** swallow an
  auth-failure type, because `convertMessages` (`MessageConverter.java:120-131`) performs **no
  IMAP/transport operation** — it calls only `messageGenerator.messageForDataType(...)` and
  `m.setFlag(...)`. No `XOAuth2AuthenticationFailedException`/`AuthenticationFailedException` can
  originate inside `convertMessages`; auth failures arise exclusively in the `K9MailTransport`
  adapter methods, which catch and translate them **before** any generic catch (full trace under
  the `K9MailTransport` section). Thus the converter's throws-type narrowing does not intersect
  the auth-escalation path.

### REQ-012 — `K9MailTransport` constructor translation (secondary leak)

- **Context.** `K9MailTransport`'s public constructor declares `throws MailException,
  MessagingException` (`:117-118`); the `MessagingException` arises from
  `new BackupImapStoreDelegate(...)` (`:120`, the delegate ctor is `throws MessagingException`,
  `:401-402`). This forces `ServiceBase.java:171-175` to catch the k-9 FQN. The same wrap
  already exists redundantly in `MailModule.kt:93-97` (the `MailTransportFactory` lambda the
  workers use). Scope verified via grep: `new K9MailTransport(` appears at `ServiceBase.java:172`
  and `MailModule.kt:94`; the package-private test ctor `K9MailTransport(BackupImapStoreDelegate,
  TrustedSocketFactory)` (`:93`) is used by `MailTransportTestFactories` and **must not change**
  (REQ-012 Constraint).
- **Decision.** Move the `MessagingException` catch **inside** the `K9MailTransport(Context,
  MailTransportConfig)` constructor body. Wrap the `new BackupImapStoreDelegate(...)` call in
  `try { ... } catch (MessagingException e) { throw new MailException(e); }` and narrow the
  public constructor signature to `throws MailException` only. The `MessagingException` import
  stays in `K9MailTransport.java` (a permitted zone). This keeps translation in `mail.transport`
  where k-9 is allowed (REQ-012 AC-7).
- **Consequences.** `ServiceBase.java:171-176` collapses to `return new
  K9MailTransport(getApplicationContext(), config);` — the only `throws` it must declare is
  `MailException` (already on `getMailTransport()`), and the FQN catch is deleted (REQ-012 AC-7,
  the second `service.*` k-9 reference eliminated). `MailModule.kt:93-97`'s redundant wrap can be
  simplified to a plain `K9MailTransport(context, config)` call (the lambda already throws
  `MailException`); this is an optional tidy, not required by an AC — the design notes it but
  does not mandate it to avoid widening scope. The cause chain is preserved end-to-end:
  `MailException.getCause()` returns the original `MessagingException`, which
  `State.getDetailedErrorMessage()` reads via `exception.getCause().toString()`
  (`State.java:59`, verified) for the "underlying=" suffix. **Behavior-preservation flag:** the
  `BinaryTempFileBody.setTempDirectory(...)` call at `K9MailTransport.java:125` must remain
  inside the constructor (after the delegate is built); it is unaffected by the catch relocation.
- **AC-5 auth-escalation translation trace (verified this session).** REQ-012 AC-5 requires that
  an XOAuth2 / `AuthenticationFailed` failure continues to surface as the **app-owned** auth type
  (`XOAuth2FailedException` / `RequiresLoginException`), preserving retry/re-login semantics — it
  must **not** be flattened to a generic `MailException`. The translation is **already** local to
  `mail.transport`, in each `K9MailTransport` adapter method, and is **unaffected** by REQ-012's
  ctor/converter changes:
  - `checkSettings()` (`:152-158`): `catch (XOAuth2AuthenticationFailedException e) → throw new
    XOAuth2FailedException(e.getStatus(), e)`, then `catch (AuthenticationFailedException e) →
    throw new RequiresLoginException()`. The XOAuth2 catch precedes the parent
    `AuthenticationFailedException` catch (subtype-first ordering), which precedes any generic
    `MessagingException`/`MailException` handling.
  - The message/header/body/fetch adapter methods (`:169-247`) repeat the identical subtype-first
    pattern (XOAuth2 → `XOAuth2FailedException`; `AuthenticationFailedException` →
    `RequiresLoginException`), and `translateAuthException()` (`:364-376`) centralizes the same
    mapping.
  - **Type lattice (verified):** `XOAuth2FailedException extends MailException`
    (`XOAuth2FailedException.java:28`); `RequiresLoginException` is app-owned
    (`service.exception.RequiresLoginException`, imported by both `MailTransport` and
    `K9MailTransport`). The port's `throws MailException, RequiresLoginException`
    (`MailTransport.java`) lets the more specific auth types propagate without being widened.
  - **Why REQ-012 cannot regress AC-5:** (1) the ctor relocation only wraps the
    `MessagingException` from `new BackupImapStoreDelegate(...)` — store construction, **not** an
    auth handshake — so no auth subtype is in scope there; (2) the converter throws-type change
    affects only `convertMessages`, which performs no transport call and cannot raise an auth
    subtype (see Component Design). The auth catches at `:155-158/:172-175/:190-193/:216-219/
    :244-247` are byte-unchanged. **AC-5 is preserved by leaving the adapter auth catches
    untouched; existing auth-path tests pass unchanged.**

### REQ-012 — `MailTransport` port immutability

- **Context / Decision.** REQ-012 AC-8 and CNTR-MODERNIZATION-007 forbid any change to
  `MailTransport.java`. Verified: no port method needs to change — both leaks are *below* the
  port (converter collaborator and adapter constructor), not on a port method. The design adds
  **no** method, changes **no** `throws`, removes **no** method.
- **Consequences.** AC-8 is satisfied structurally: `MailTransport.java` is not edited. The ACL
  invariant becomes unconditionally true (CNTR-MODERNIZATION-007 Validation Rule 1).

### REQ-012 — Comment/javadoc `com.fsck.k9` scrub (to reach the literal grep-zero)

- **Context.** AC-1's verification command is `grep -rn "com\.fsck\.k9" .../service/` → **0
  output lines**, and AC-1 explicitly calls out "javadoc `{@code}` blocks that would couple at
  compile time." Deleting the two FQN catches removes only the 2 *code* matches; **18
  comment/javadoc matches remain** (enumerated in `## Context`). Six of those vanish with the
  REQ-013 deletion of `BackupTask`/`RestoreTask` (3 each); **twelve survive** in `BackupWorker.kt:55`,
  `RestoreWorker.kt:30,66`, `SmsBackupService.java:27,78`, `SmsRestoreService.java:11,12,41,42`,
  and `State.java:6,7,8`.
- **Decision.** REQ-012 **must reword the 12 surviving comment/javadoc occurrences** of the literal
  string `com.fsck.k9` so the grep reaches zero. These are all U-026 "import removed" / "no
  `{@code com.fsck.k9.*}` import remains" annotations — historical breadcrumbs, not load-bearing.
  Reword them to drop the literal FQN (e.g. "k-9 `MessagingException` import removed (U-026)" or
  "no k-9 transport/MIME import remains"), or delete the now-stale annotation outright. **This is a
  REQ-012 obligation, not REQ-013** — REQ-013's deletions only clear the 6 in the doomed task
  files; the 12 in surviving files are AC-1/AC-2 scope for REQ-012 and would otherwise leave the
  grep non-zero after REQ-012 lands. (Interpretation note: this design reads AC-1's grep
  literally — comments included — because AC-1's own text names `{@code}` javadoc. If the
  requirement author intends the grep to exclude comments, that must be raised as an
  AC-interpretation clarification against REQ-012 **before** sprint-planning; absent that
  clarification, the scrub is mandatory.)
- **Consequences.** After REQ-012 the 2 **code** catches are gone (`BackupTask.java:308` via the
  converter fix; `ServiceBase.java:173` via the ctor relocation) and the **12 surviving comment
  residuals are reworded**, so `grep -rn "com\.fsck\.k9" .../service/` returns **0 across every
  surviving file** including `service/state/State.java`. The **6** comment residuals in
  `BackupTask`/`RestoreTask` are moot once REQ-013 deletes those two files (as is the
  `BackupTask:308` catch, redundantly). Net: grep-zero holds after REQ-012 alone for the surviving
  tree, and remains zero after REQ-013's deletions. The Design Validation grep criterion (below) is
  annotated to make "zero **including comments/javadoc**" explicit.

### REQ-013 — Manual dispatch migration (the unblocking step)

- **Context.** `BackupTask`/`RestoreTask` deletion is *blocked* until no production path
  constructs/executes them (REQ-013 AC-1). The live constructors are
  `SmsBackupService.getBackupTask().execute(...)` (`SmsBackupService.java:145`, inside
  `backup()`) and `SmsRestoreService.getRestoreTask().execute(config)`
  (`SmsRestoreService.java:115`, inside `handleIntent()`). Also live: `BackupCancelCollector.kt`
  (a `service.*` Kotlin file) holds `collect(repo, task: BackupTask)` and `collectForRestore(repo,
  task: RestoreTask)` (`:25,36`), invoked from `BackupTask.doInBackground` (`:123`) and
  `RestoreTask.doInBackground` (`:108`).
- **Decision.** Rewrite the two service entry points to delegate to the injected
  `BackupScheduler` (Option (a)):
  - `SmsBackupService.backup(BackupType)` keeps its pre-flight checks (permissions, credentials,
    enabled-types, the `MailException` catch for invalid URI, and `moveToState(...)` error
    reporting — all preserved verbatim) but replaces `getBackupTask().execute(getBackupConfig(...,
    getMailTransport()))` with a call to `getScheduler().scheduleImmediate()` for a MANUAL
    trigger. The `SKIP` type must still be honored — `scheduleImmediate()` enqueues a
    `BROADCAST_INTENT`-tagged `BackupWorker` with `Constraints.NONE`; the worker infers type via
    `inferBackupType()` from tags (`BackupWorker.kt:430-436`). **Behavior-preservation flag
    (non-trivial):** see `## Integration Design` — MANUAL vs BROADCAST_INTENT differ in the
    notification/foreground path and SKIP handling; the design specifies a scheduler seam that
    preserves the manual semantics rather than silently collapsing MANUAL→BROADCAST_INTENT.
  - `SmsRestoreService.handleIntent()` keeps its `canWriteToSmsProvider()` guard and
    `SmsProviderNotWritableException` path, then replaces `getRestoreTask().execute(config)` with
    `getScheduler().scheduleRestore(new RestoreSchedulerConfig(RestoreWorker.RESTORE_WORK_NAME,
    RestoreWorker.RESTORE_WORK_NAME))` (the durable-checkpoint key, U-016). The `getMailTransport()`
    construction in the service is removed for restore (the worker builds its own transport via
    the injected `MailTransportFactory`, `RestoreWorker.kt:146`).
  - **`SmsRestoreService.clearCache()` decision (REQ-013 Notes, verified this session).**
    `SmsRestoreService` runs `asyncClearCache()` (`:152-159`) on `onCreate`, which spawns a thread
    calling `clearCache()` (`:161-174`) to delete `body*` temp files from the cache dir; the worker
    path has its own `RestoreWorker.clearAppCache()`. **Decision: `clearCache()` STAYS in
    `SmsRestoreService` (the service is retained as the foreground/lifecycle shell).** The service
    still receives the `startService(...)` intent and runs `onCreate` → `asyncClearCache()` exactly
    as today **before** it enqueues the worker, so the pre-restore temp-file purge timing is
    **byte-for-byte preserved** — it does not move to the worker, and `RestoreWorker.clearAppCache()`
    is left untouched (no worker behavior change, per EPIC). This satisfies the REQ-013 Notes
    requirement to confirm clearCache equivalence: there is **no change** to clearCache; it remains
    a service-onCreate responsibility. (`asyncClearCache`/`clearCache` are not in the
    `getRestoreTask()` body, so the factory deletion does not touch them.)
  - `getBackupTask()` / `getRestoreTask()` are deleted (also satisfies REQ-014 AC-5/AC-6 — the
    two requirements agree this method is gone).
  - `BackupCancelCollector.kt` is **deleted** — its only consumers are `BackupTask`/`RestoreTask`.
    Cancellation in the worker model is cooperative (`ensureActive()`, `BackupWorker.kt:303`,
    `RestoreWorker.kt`) and externally driven by `WorkManager.cancelUniqueWork(...)`
    (`WorkManagerScheduler.cancelAll`, verified). **Cancel-path call site (located this session,
    R-4):** the UI cancel emitter is `StatusPreference.java` — `onBackup` cancel at `:279-283` and
    `onRestore` cancel at `:304-308` both call `tryEmitEvent(new SyncEvent.Cancel(USER))`
    (`StatusPreference.java:282/307`). Today that `SyncEvent.Cancel` is consumed **only** by
    `BackupCancelCollector` (`:29-31`/`:40-42`), which calls `task.onCancelRequested(...)`. When
    `BackupCancelCollector` and the tasks are deleted, **that Cancel event has no consumer** — so
    cancelling a manual run would silently no-op. **Required REQ-013 work (not a generic
    "verify"):** the cancel path must be rewired to reach
    `WorkManager.cancelUniqueWork(<uniqueName>)`. The clean shape: route `StatusPreference`'s
    cancel to the injected `BackupScheduler` so it calls `cancelUniqueWork` for the manual
    backup/restore unique-work names (the same names enqueued by the dispatch migration —
    `BackupType.MANUAL.name()`/`RESTORE_WORK_NAME`), and the worker's cooperative `ensureActive()`
    observes the interrupt. Because the unique-work name carries `BackupType` under the
    `scheduleManual` seam (below), the cancel call can target it precisely. A cancel test
    (manual run → `Cancel(USER)` → `WorkInfo` reaches `CANCELLED` → service `stopForeground/
    stopSelf`) is required (ties to R-2's `WorkInfo` observer).
- **Consequences.** After this step, grep for `new BackupTask`/`new RestoreTask`/`.execute(`/
  `getBackupTask`/`getRestoreTask` over `app/src/main/java/` → 0 (REQ-013 AC-1). The services and
  their manifest `foregroundServiceType="dataSync"` are retained (REQ-013 AC-6). The
  `getMailTransport()` seam in `ServiceBase` is **still used by backup** only if MANUAL keeps a
  service-side transport — but since both engines now enqueue workers, `getMailTransport()`
  becomes **unused by the services**; the design notes it may be removed once both dispatch
  paths delegate (it is the seam that holds the second REQ-012 FQN catch, so REQ-012 must seal it
  before it is potentially removed — handled by 012→013 ordering). The design **retains**
  `getMailTransport()` to avoid widening REQ-013 scope, but flags it as now-dead for a future
  cleanup; if the implementing story removes it, the REQ-012 AC-7 fix is moot for that method but
  still required for the `K9MailTransport` signature (which the workers' factory depends on).

### REQ-013 — `BackupTask.java` / `RestoreTask.java` deletion

- **Context / Decision.** Once AC-1 holds, delete `BackupTask.java`, `RestoreTask.java`, and the
  test-only classes `BackupTaskTest.java`, `RestoreTaskTest.java` (verified present:
  `app/src/test/java/.../service/BackupTaskTest.java`, `RestoreTaskTest.java`). Verify no
  residual references: grep `import.*BackupTask`/`import.*RestoreTask`/`extends AsyncTask` over
  `app/src/main/java/` → 0 (REQ-013 AC-4). Note `SmsBackupServiceTest.java` currently declares
  `@Mock BackupTask backupTask` and overrides `getBackupTask()` — those lines are removed as part
  of the REQ-014 test migration (the two requirements' test edits are coordinated).
- **Consequences.** The duplicate IMAP loop is gone; the divergence risk is closed. The
  `CalendarSyncer` `@Inject` constructor loses its only consumer (`Lazy<CalendarSyncer>` in
  `BackupTask`) — see the binding fix below.

### REQ-013 — `jacocoFileFilter` exclusion removal

- **Context.** `app/build.gradle:229-232` excludes `**/BackupTask.class`,
  `**/BackupTask$*.class`, `**/RestoreTask.class`, `**/RestoreTask$*.class` from the JaCoCo
  class set. The gate is per-package LINE ≥ 70% on `com.zegoggles.smssync.service*`,
  `mail*`, `auth*` (verified `:304-343`; `service*` matches `service` + `service.state` +
  `service.exception`).
- **Decision.** Remove exactly those four lines (REQ-013 AC-9). Leave the
  `BackupWorker`/`RestoreWorker` exclusions (`:219-222`) untouched (REQ-013 Note — worker
  coverage is deferred to instrumented tests).
- **Consequences.** Because the *classes* are deleted simultaneously, they contribute zero
  lines (covered or uncovered) to the gate — removing their exclusions cannot lower the ratio
  (REQ-013 AC-10 reasoning). **Risk flag:** deleting `BackupTaskTest`/`RestoreTaskTest` removes
  tests that currently cover *other* `service.*` lines they touch transitively (e.g.
  `BackupConfig`, `BackupCursors`, `BulkFetcher`). The implementing story must run
  `jacocoTestCoverageVerification` and, if `service*` dips below 70%, backfill characterization
  tests on the surviving classes (the same remedy U-006/U-017 used). This is the single highest
  coverage risk in the epic — see `## Risks`.

### REQ-014 — `@AndroidEntryPoint` application

- **Context / Decision.** Add `@dagger.hilt.android.AndroidEntryPoint` to `SmsBackupService` and
  `SmsRestoreService` (the only concrete `ServiceBase` subclasses; grep `extends ServiceBase`
  confirms exactly these two plus the abstract base). This inserts `Hilt_SmsBackupService` /
  `Hilt_SmsRestoreService` into the inheritance chain, firing member injection on
  `ServiceBase`'s `@Inject` fields before `onCreate()` (REQ-014 AC-1). Remove the
  `TODO(U-023): add @AndroidEntryPoint` comments. The Hilt version is fixed at 2.51.1 + kapt
  (REQ-014 Constraint; do not change).
- **Consequences.** `kaptDebugKotlin` must produce zero Dagger errors (REQ-014 AC-2). This is
  the step that would surface the `CalendarSyncer` `MissingBinding` **if `BackupTask` were still
  present** — which is why 013 (deletion) precedes 014 (annotation). With `BackupTask` already
  deleted, the graph never resolves `Lazy<CalendarSyncer>`.

### REQ-014 — `ServiceBase` shim removal

- **Context / Decision.** With `@AndroidEntryPoint` live, the null branches in
  `getPreferences()`/`getAuthPreferences()` (`ServiceBase.java:186-200`) are unreachable. Replace
  the bodies with direct returns: `return injectedPreferences;` / `return injectedAuthPreferences;`.
  Optionally rename `injectedPreferences`→`preferences` and `injectedAuthPreferences`→
  `authPreferences` (REQ-014 AC-3 note) — the field-shadowing hazard that motivated the prefix is
  gone once the anonymous-subclass tests are migrated. The design **recommends** the rename for
  EPIC Success Criterion 6 (which greps for `injectedPreferences`/`injectedAuthPreferences` → 0),
  so the rename is **required** to satisfy the epic's exit grep, not merely optional.
- **Consequences.** grep `injectedPreferences != null`/`injectedAuthPreferences != null` → 0
  (AC-3); grep `new Preferences(getApplicationContext`/`new AuthPreferences(this)` over
  `service/` → 0 (AC-4); EPIC SC-6 grep for the `injected*` field names → 0. Note
  `RestoreTask.java:89` reads `service.getPreferences()` in its constructor — but `RestoreTask`
  is deleted in 013, so this is moot by the time 014 runs (another reason for the ordering).

### REQ-014 — `getBackupTask()` / `getRestoreTask()` factory removal

- **Context / Decision.** Already removed in REQ-013's dispatch migration (the methods'
  *callers* are gone, and the methods themselves are deleted there). REQ-014 AC-5/AC-6 verify the
  end state: grep `getBackupTask`/`getRestoreTask` over `app/src/main/java/` → 0, and grep the
  fully-qualified `new com.zegoggles.smssync.service.BackupItemsFetcher` / `BackupQueryBuilder` /
  `mail.PersonLookup` / `contacts.ContactAccessor` / `mail.MessageConverter` / `auth.OAuth2Client`
  / `auth.TokenRefresher` / `service.CalendarSyncer` over `service/` → 0. These FQN `new`
  expressions are exactly the bodies of `getBackupTask()` (`SmsBackupService.java:210-239`) and
  `getRestoreTask()` (`SmsRestoreService.java:130-142`), verified.
- **Consequences.** The hand-wired construction graph is gone; construction has a single source
  of truth.
- **INTENTIONAL DIVERGENCE from EPIC/REQ-014 ownership (flagged for story authoring).**
  EPIC-MODERNIZATION-005 §Sequencing Constraint states "REQ-014 deletes `getBackupTask()` and
  `getRestoreTask()` as part of removing the coexistence shims," and REQ-014 AC-5 frames the
  grep-zero as REQ-014 work. **This design instead deletes the two factory methods in REQ-013, and
  reduces REQ-014 AC-5/AC-6 to verification-only.** Rationale: REQ-013 deletes the factories'
  **only callers** (`SmsBackupService.backup()` / `SmsRestoreService.handleIntent()`) **and** the
  `BackupTask`/`RestoreTask` classes the factory bodies `new`-construct; leaving the now-dead
  factories in place across the REQ-013→REQ-014 window would mean a method whose body references
  deleted classes (`new BackupTask(...)`) — a compile break. So the factory deletion must travel
  with REQ-013. **Unambiguous assignment for the sprint planner:**
  - **REQ-013 performs the factory deletion** (its callers and the constructed classes are deleted
    there).
  - **REQ-014 AC-5/AC-6 become a verification-only gate** (`grep getBackupTask/getRestoreTask` and
    the 8 FQN-`new` strings over `service/` → 0).
  Story authors MUST NOT double-assign the deletion to REQ-014 nor leave it unassigned; this
  divergence from the EPIC/REQ-014 text is deliberate and is also recorded in `## Integration
  Contracts` / `## Sequencing & Dependencies`.

### REQ-014 — `CalendarSyncer` binding fix (latent `MissingBinding`)

- **Context.** `CalendarSyncer`'s `@Inject` constructor (`CalendarSyncer.java:32-41`) takes a raw
  `long calendarId` with no Hilt binding (verified above). The `BackupWorker` path does **not**
  use Dagger to build `CalendarSyncer` — it `new CalendarSyncer(..., preferences.callLogCalendarId
  .toLong(), ...)` directly (`BackupWorker.kt:220-227`), and `SmsBackupService.getBackupTask()`
  does the same via the hand-written `Lazy` lambda. So the `@Inject` annotation on `CalendarSyncer`
  is currently **vestigial and unsatisfiable**.
- **Decision.** Since REQ-013 deletes `BackupTask` (the only `@Inject`-mediated consumer of
  `CalendarSyncer` via `Lazy<CalendarSyncer>`), the cleanest fix is to **remove the `@Inject`
  annotation from `CalendarSyncer`'s constructor** (it is never built by the graph; the worker
  and any test construct it directly). This eliminates the unsatisfiable injection point entirely
  and guarantees `kaptDebugKotlin` stays clean (REQ-014 AC-2). The alternative — adding a
  `@Named("calendarId") long` provider to a new `EngineModule` — is rejected: it would bind a
  *mutable preference value* (`preferences.getCallLogCalendarId()`) as an application singleton,
  which is incorrect (the calendar id can change at runtime) and adds a module for a type the
  graph never needs to build. Removing the vestigial `@Inject` is correct and minimal.
- **Consequences.** No Dagger `MissingBinding` is possible after `@AndroidEntryPoint` is applied.
  `CalendarSyncerTest` (which constructs `CalendarSyncer` directly with mocks, per the javadoc at
  `:28-30`) is unaffected — it never relied on Dagger. **Verification:** the implementing story
  must confirm no `@Provides`/`@Binds` anywhere asks for `CalendarSyncer` (grep → 0) before
  removing the annotation.

### REQ-014 — Robolectric test migration (the central blocker)

- **Context.** Two test classes block `@AndroidEntryPoint` (verified):
  - `SmsBackupServiceTest.java:68-80` creates `new SmsBackupService() { ... }` — an anonymous
    subclass overriding `getPreferences()`, `getAuthPreferences()`, `getBackupTask()`,
    `getScheduler()`, `getApplicationContext()`, `getResources()`, `checkPermission()`,
    `notifyUser()` to return mocks. Under `@AndroidEntryPoint`, `Hilt_SmsBackupService` calls
    `inject(this)` in `attachBaseContext()`, which throws `IllegalStateException` under a plain
    Robolectric `Application` (not `HiltTestApplication`).
  - `SmsRestoreServiceTest.java:25` uses `setupService(SmsRestoreService.class)` — same Hilt
    lifecycle incompatibility; it has no anonymous subclass but Robolectric's `setupService`
    drives the service lifecycle including `attachBaseContext`.
- **Decision.** Apply **REQ-014 Option A (constructor-injection refactor)** — the preferred,
  lower-ceremony approach, consistent with the U-023 `BackupTask` precedent and the long-term
  `@HiltWorker` direction. Concretely:
  - The services' *collaborators that tests need to mock* — `Preferences`, `AuthPreferences`,
    `BackupScheduler` — are obtained through `protected` accessors (`getPreferences()`,
    `getAuthPreferences()`, `getScheduler()`) that return Hilt-injected fields in production.
    Because `getBackupTask()`/`getRestoreTask()` are deleted (013), the *only* remaining
    test-override surface is preferences/authPreferences/scheduler — which after shim removal are
    direct field returns. To keep tests constructable **without** the Hilt lifecycle, the design
    retains the `protected` accessor seam: tests override `getScheduler()`/`getPreferences()`/
    `getAuthPreferences()` but **do not** instantiate the service via `new ServiceSubclass() {}`
    with Hilt active. The migration replaces the anonymous-subclass-of-a-`@AndroidEntryPoint`-
    service with a **test double that does not trigger `inject(this)`**: construct the service
    via Robolectric `buildService(...)` with the injected fields set reflectively, or refactor
    the testable logic (the dispatch decision) into a package-private method that takes its
    collaborators as parameters and is unit-tested directly without service-lifecycle.
  - REQ-014 AC-8 requires `new SmsBackupService() {` and `new SmsRestoreService() {` to **not**
    appear in test source. The migration therefore **must remove the anonymous subclass** in
    `SmsBackupServiceTest`. The recommended shape: extract `backup(BackupType)`'s dispatch
    decision (the `isWorking()`/`restoreIdle` gate + `scheduleImmediate()` call) so it can be
    exercised against a real `SmsBackupService` whose `getScheduler()`/`getPreferences()`/
    `getAuthPreferences()` return mocks set via the `@Inject` fields (Robolectric can build a
    `@AndroidEntryPoint` service when the application is `HiltTestApplication`; if the story
    prefers to avoid that, it sets the injected fields directly on a `buildService` instance and
    never calls the Hilt-injecting lifecycle). The implementing story MUST document the exact
    mechanism in its implementation log (REQ-014 AC-7).
  - **Decision is Option A, but with an explicit fallback gate:** if, during implementation, the
    constructor/field-seam approach cannot satisfy AC-8 without the Hilt lifecycle (e.g.
    Robolectric `buildService` still routes through `attachBaseContext` → `inject`), the story
    falls back to **Option B (`@HiltAndroidTest` + `HiltTestApplication` via
    `robolectric.properties` + `@BindValue` mocks)**. The story must pick exactly one and state
    it (AC-7); this design's recommendation is A, fallback B, and forbids re-deferring the
    blocker (REQ-014 Constraint).
- **Consequences.** `SmsBackupServiceTest`/`SmsRestoreServiceTest` no longer use anonymous
  subclasses (AC-8); the full suite stays green (AC-9, regression floor of the sprint-001 baseline
  568 passing / 2 skipped per EPIC SC-7). The existing service test *assertions* (state machine,
  scheduling, error notification) are preserved or replaced by equivalents — the dispatch tests
  change from "verify `backupTask.execute(...)`" to "verify `scheduler.scheduleImmediate()` /
  `scheduler.scheduleRestore(...)`", reflecting the 013 dispatch migration. **No service behavior
  test may be deleted without an equivalent replacement** (REQ-014 AC-9).

## Integration Design

### MailTransport boundary purity (REQ-012)

After REQ-012, the ACL invariant (CNTR-MODERNIZATION-007 Validation Rule 1) holds
unconditionally: no `com.fsck.k9.*` reference in any syntactic position in `service.*`. The
translation lives entirely in `mail.transport.*` (`K9MailTransport` ctor + adapter methods) and
`mail.MessageConverter` (whose internal k-9 usage is a permitted, documented residual,
DES-MODERNIZATION-009 ADR-009-B). The port interface `MailTransport.java` is byte-unchanged
(AC-8). The cause chain `MailException(MessagingException)` → `getCause()` → `State` "underlying="
suffix is preserved at every translation site (`MessageConverter`, `K9MailTransport` ctor,
`K9MailTransport` adapter methods).

### MainActivity → service → WorkManagerScheduler manual-trigger path (REQ-013)

The integration point is `SmsBackupService.backup()` / `SmsRestoreService.handleIntent()` →
`getScheduler()` (`BackupScheduler` port, `App.getScheduler(this)`, `SmsBackupService.java:380-382`).
`MainActivity`'s `startService(...)` calls are unchanged (out of scope per REQ-013). The
**non-trivial behavior-preservation analysis** of swapping AsyncTask-execute for scheduler-enqueue:

| Concern | AsyncTask path (current) | Worker path (target) | Resolution |
|---------|--------------------------|----------------------|------------|
| **MANUAL vs BROADCAST_INTENT type** | `backup(MANUAL)` builds `BackupConfig` with `MANUAL`; worker not involved | `scheduleImmediate()` tags `BROADCAST_INTENT` + `Constraints.NONE`; `BackupWorker.inferBackupType()` returns `BROADCAST_INTENT` | **Behavioral difference flagged.** MANUAL drives manual notification/foreground (`SmsBackupService.notifyAboutBackup`, `:319-326`) and unconditional user-notify on error (`shouldNotifyUser`, `:314-317`). The design requires the scheduler seam to **preserve MANUAL semantics** — see decision below. |
| **SKIP backup** | `backup(SKIP)` → `BackupTask.skip()` marks max-synced-date without IMAP | `BackupWorker.executeSkip()` (`:172-192`) preserves this; but `scheduleImmediate()` always tags `BROADCAST_INTENT`, losing the SKIP signal | The scheduler call for a SKIP trigger must carry the type. |
| **Foreground promotion** | `SmsBackupService.notifyAboutBackup` → `startForeground(BACKUP_ID, ...)` for MANUAL | `BackupWorker` does **not** call `setForeground()` today (`BackupWorker.kt:150-153` comment) | REQ-013 AC-8 documented decision — see below. |
| **Wakelock / wifi-lock** | `ServiceBase.acquireLocks()` (PARTIAL wake + wifi lock) around the task | WorkManager holds its own wakelock for `CoroutineWorker`; service no longer holds the task | REQ-013 AC-8 documented decision — see below. |

- **Decision — preserve MANUAL/SKIP via the scheduler, not `scheduleImmediate()` collapse.** The
  design requires the manual backup dispatch to enqueue a `BackupWorker` **tagged with the actual
  `BackupType`** (`MANUAL.name()` or `SKIP.name()`), so `BackupWorker.inferBackupType()` resolves
  the correct type and `executeSkip()`/notification semantics are preserved. The current
  `BackupScheduler` port has `scheduleImmediate()` (hard-codes `BROADCAST_INTENT`,
  `WorkManagerScheduler.kt:270-282`) — which is **insufficient** for MANUAL/SKIP. The implementing
  story must either (i) add a port operation `scheduleManual(BackupType)` to `BackupScheduler` +
  `WorkManagerScheduler` (a contract change to CNTR-MODERNIZATION-004 — see `## Integration
  Contracts`), or (ii) reuse `scheduleImmediate()` and accept that manual backups run as
  `BROADCAST_INTENT` (a behavior change in notification/foreground, which **violates** EPIC SC-10
  "no behavior change" and is therefore **not acceptable**). **The design mandates option (i):** a
  type-carrying manual-enqueue seam, to preserve MANUAL notification/foreground and SKIP handling.
  This is the most consequential integration decision in REQ-013 and is the reason a contract
  touchpoint is raised below.
- **Restore.** `SmsRestoreService.handleIntent()` → `getScheduler().scheduleRestore(new
  RestoreSchedulerConfig(RESTORE_WORK_NAME, RESTORE_WORK_NAME))`. `scheduleRestore` already exists
  and enqueues the real `RestoreWorker` with `Constraints.NONE` (`WorkManagerScheduler.kt:295-307`,
  verified) — no contract change needed for restore. The `RestoreWorker` reads its checkpoint key
  from `KEY_UNIQUE_WORK_NAME` (`RestoreWorker.kt:215,687`); the config's `uniqueName` becomes the
  work name and must match.

### Foreground-service / wakelock handling in the worker model (REQ-013 AC-8)

- **Decision (documented per AC-8).** When manual backup/restore is enqueued as a
  `CoroutineWorker`:
  - **Wakelock:** the `ServiceBase.acquireLocks()` PARTIAL wake lock + wifi lock are
    **intentionally not reproduced** in the worker. WorkManager holds a `PARTIAL_WAKE_LOCK`
    internally for a running `CoroutineWorker` (documented WorkManager behavior, referenced in
    `BackupWorker.kt:150-153`). This is behaviorally equivalent for the *backup* path (which used
    a PARTIAL wake lock, `ServiceBase.wakeLockType():220`).
  - **Restore `FULL_WAKE_LOCK`:** `SmsRestoreService.wakeLockType()` returns `FULL_WAKE_LOCK` on
    API 19+ (`SmsRestoreService.java:203-212`, verified) "to keep the screen on so the user can
    switch back the SMS app afterwards." WorkManager only guarantees a PARTIAL lock — it does
    **not** keep the screen on. **This is a genuine behavior difference for restore.** The design
    requires the implementing story to **document this in the AC-8 decision** and choose:
    (a) accept the screen no longer staying on during a worker-driven manual restore (a UX change,
    arguably acceptable since restore is rare and user-initiated), or (b) have `SmsRestoreService`
    acquire the `FULL_WAKE_LOCK` *itself* around the worker enqueue + observation window
    (preserving the screen-on behavior while the engine moves to the worker). The design
    **recommends (b)** to honor EPIC SC-10, but flags it as a point requiring an explicit AC-8
    decision and a reviewer sign-off. (Note: `SmsRestoreServiceTest.wakeLockType_returnsBrightScreen`
    asserts the wake-lock type is non-zero — that test is retained.)
  - **Foreground promotion — GROUND-TRUTH (re-verified this session, corrects a prior
    over-statement).** Manual backup currently promotes to foreground
    (`startForeground(BACKUP_ID, notification)`) and tears down (`stopForeground(true)` +
    `stopSelf()`) **inside the public methods `backupStateChanged()` (`SmsBackupService.java:259-280`)
    and `restoreStateChanged()` (`SmsRestoreService.java:178-195`)**. Critically, **these methods are
    NOT StateFlow observers.** Verified call-graph:
    - `backupStateChanged(state)` is invoked by a **direct method call** from `BackupTask.post()`
      (`BackupTask.java:273` — `service.backupStateChanged(state)`) and from the service's own
      `moveToState()` (`SmsBackupService.java:243`, used only on the pre-flight error path).
    - `restoreStateChanged(state)` is invoked by a **direct method call** from `RestoreTask`
      (`RestoreTask.java:328`) and from the service's own `postError()` (`SmsRestoreService.java:148`).
    - **There is NO `syncStateRepository().getState()` collector in either service** (grepped:
      the services only *write* via `emitState`, they never `collect`). The `startForeground`/
      `stopForeground`/`stopSelf` driver's **only** running-state caller today is the AsyncTask's
      direct `*StateChanged()` call.
    - `BackupWorker`/`RestoreWorker` emit progress **only** via `setProgress(workDataOf(...))`
      (`BackupWorker.kt:292,298,337`; `RestoreWorker.kt:218,222,307,392`). **The workers never call
      `App.syncStateRepository().emitState(...)`** (grep-confirmed — `emitState` callers in
      `service/` are `BackupTask:271`, `RestoreTask:326`, `SmsBackupService:246`,
      `SmsRestoreService:149` only; no worker).

    **Consequence:** when REQ-013 deletes `BackupTask`/`RestoreTask`, the running-state
    `startForeground`/`stopForeground`/`stopSelf` transitions lose their **only** driver. Neither
    a worker→repository emit nor a service-side StateFlow observer exists to replace it. Therefore
    establishing a **worker → foreground/stop signalling path is a REQUIRED, ordered PREREQUISITE
    of the REQ-013 dispatch migration — not a post-hoc "verify."** The dispatch rewrite
    (`backup()`/`handleIntent()` → scheduler) **must not be merged** until one of the two options
    below is implemented and the manual foreground promotion + teardown + notification are proven
    preserved.

    - **Decision — Option (b1): the service owns a `WorkInfo` observer that bridges worker
      `setProgress` state into the existing `*StateChanged()` foreground/stop driver (REQUIRED).**
      Concretely: `SmsBackupService.backup()` / `SmsRestoreService.handleIntent()`, immediately
      after enqueuing the worker, register a `WorkManager.getInstance(ctx)
      .getWorkInfoByIdLiveData(workId)` (or `getWorkInfosForUniqueWorkLiveData(uniqueName)`)
      observer that, on each `WorkInfo` emission, maps the worker's `progressData`
      (`PROGRESS_KEY_STATE`) / terminal `state` into a `BackupState`/`RestoreState` and calls the
      existing `backupStateChanged(state)` / `restoreStateChanged(state)` method — which already
      performs `startForeground` on running and `stopForeground(true)`/`stopSelf()` on terminal.
      This keeps the worker byte-unchanged (honors the EPIC "no worker behavior change"
      constraint) and reuses the verbatim foreground/teardown/notification logic. The observer
      must be torn down on `stopSelf()`. **This is the linchpin of behavior preservation and the
      single highest-effort task in REQ-013** — the service becomes a thin foreground/lifecycle
      shell that *observes* the worker via `WorkInfo`, because no engine→repository channel exists
      for the worker.
    - **Alternative — Option (b2): bridge the worker into `syncStateRepository` first.** Add a
      worker→`App.syncStateRepository().emitState(...)` emit (mirroring `BackupTask.post()`) AND a
      service-side `StateFlow` collector that calls `*StateChanged()`. **Rejected as primary**
      because it requires *changing worker behavior* (adding `emitState` to the worker), which the
      EPIC carves out of scope, and it adds a second engine→UI channel alongside the existing
      `setProgress` one. It is recorded only as a fallback if `WorkInfo` LiveData proves
      unworkable under the service lifecycle; choosing it requires an explicit AC-8 decision and a
      reviewer waiver of the worker-scope constraint.
    - **`setForeground()` on the worker is NOT chosen** (would change worker behavior — out of
      scope per EPIC), so the foreground guarantee is preserved by the service-as-shell model
      (Option b1), not by the worker.

> **Behavior-preservation hazard R-2 (now a hard prerequisite, not a risk to "verify"):** because
> the worker emits **only** `setProgress(workDataOf(...))` (`BackupWorker.kt:337`) and the service
> has **no** `syncStateRepository` collector, deleting the AsyncTask removes the only caller of the
> `startForeground`/`stopForeground`/`stopSelf` driver. The REQ-013 story **must implement Option
> (b1)** (a service-side `WorkInfo` observer that re-invokes `*StateChanged()`) as a precondition
> of the dispatch rewrite. There is no existing bridge to "verify" — one must be built. This is the
> deepest behavior-preservation work in the epic and is gated in `## Risks` (R-2) and ordered as a
> blocking prerequisite in `## Sequencing & Dependencies`.

### Hilt graph integration (REQ-014)

`@AndroidEntryPoint` on the two services makes the `ServiceBase` `@Inject` fields
(`Preferences`, `AuthPreferences`) live; both are `@Singleton`-provided by `PreferencesModule`
(verified: `providePreferences`, `provideAuthPreferences`). `getScheduler()` remains
`App.getScheduler(this)` (a Service-Locator call) — REQ-014 AC-5 (EPIC SC-6) targets the
`getBackupTask`/`getRestoreTask`/`injected*` shims, **not** `getScheduler()`; the design does not
require converting `getScheduler()` to `@Inject` (it is out of the named shim set and changing it
would widen scope). The `CalendarSyncer` `@Inject` removal keeps `kaptDebugKotlin` clean.

## Design Validation

Each design decision maps to specific ACs. Verification commands are taken verbatim from the
requirements' Verification Method sections. "scope verified via grep" markers indicate scope
claims confirmed this session.

| AC | Requirement | Design decision | Verification command |
|----|-------------|-----------------|----------------------|
| 012 AC-1/AC-2 | Zero k-9 in `service/` (all forms, **including comments/javadoc**) | (1) Converter wrap (app-owned throws) + `K9MailTransport` ctor catch relocation delete both FQN **code** catches; (2) **reword the 12 surviving comment/javadoc `com.fsck.k9` residuals** in `BackupWorker.kt`, `RestoreWorker.kt`, `SmsBackupService.java`, `SmsRestoreService.java`, `State.java` (the other 6 vanish when REQ-013 deletes `BackupTask`/`RestoreTask`) | `grep -rn "com\.fsck\.k9" app/src/main/java/com/zegoggles/smssync/service/` → **0 output lines, comments and javadoc `{@code}` included** (currently 20: 2 code + 18 comment) |
| 012 AC-3/AC-7 | Compile clean; `ServiceBase.getMailTransport()` no k-9 | `K9MailTransport(Context,Config)` declares only `throws MailException` | `./gradlew :app:compileDebugJavaWithJavac` zero errors |
| 012 AC-4 | Cause chain preserved | `new MailException(e)` at every translation site; `MailException.getCause()` == original | Unit test: `assertThat(caught.getCause()).isInstanceOf(MessagingException.class)` |
| 012 AC-5 | Auth escalation preserved | No change to `XOAuth2FailedException`/`RequiresLoginException` translation (already in `K9MailTransport` adapter methods + `checkSettings`) | Existing auth-path tests pass unchanged |
| 012 AC-6 | `convertMessages` no longer throws k-9 into engine | Option (a): `convertMessages` translates internally, throws `MailException` | grep `service/` k-9 → 0; `MessageConverter` callers compile |
| 012 AC-8 | Port immutable | `MailTransport.java` not edited | diff `MailTransport.java` vs U-025 close → no signature change |
| 012 AC-9/AC-10 | Suite green ≥568; coverage ≥70% | Add converter/ctor translation tests | `:app:testDebugUnitTest` 0 failures; `:app:jacocoTestReportDebug` ≥70% |
| 013 AC-1 | No live AsyncTask construction | Dispatch migrated to scheduler; collector deleted | grep `new BackupTask`/`new RestoreTask`/`.execute(`/`getBackupTask`/`getRestoreTask` over `app/src/main/java/` → 0 (scope verified via grep) |
| 013 AC-2/AC-3/AC-4 | Classes + tests deleted; no refs | Delete 4 files; verify imports gone | `find app/src -name "BackupTask.java" -o -name "RestoreTask.java"` → 0; same for tests; grep `import.*BackupTask`/`extends AsyncTask` → 0 |
| 013 AC-5/AC-6/AC-7 | Factories gone; services + manifest retained; delegate to scheduler | Delete `getBackupTask`/`getRestoreTask`; rewrite `backup()`/`handleIntent()` | grep `getBackupTask`/`getRestoreTask` → 0; services present; `foregroundServiceType="dataSync"` unchanged |
| 013 AC-8 | Foreground/wakelock decision documented **+ worker→foreground signalling path built** | `## Integration Design` Option (b1): service registers a `WorkManager.getWorkInfo*LiveData` observer (built as Step 2a prerequisite) that re-invokes `*StateChanged()` to drive `startForeground`/`stopForeground`/`stopSelf`; restore `FULL_WAKE_LOCK` decision | Manual backup/restore on-device (AC-13/AC-14): foreground notification appears while running and is dismissed (`stopForeground`/`stopSelf`) on completion; implementation-log documents the `WorkInfo`-observer mechanism and the restore-wakelock choice |
| 013 AC-9/AC-10 | jacoco exclusions removed; gate holds | Remove 4 `jacocoFileFilter` lines (`:229-232`) | grep those 4 globs in `app/build.gradle` → 0; `:app:jacocoTestCoverageVerification` BUILD SUCCESSFUL |
| 013 AC-11/AC-12 | Suite + assemble green | — | `:app:testDebugUnitTest` 0 failures; `:app:assembleDebug` success |
| 013 AC-13/AC-14/AC-15 | On-device smoke | Manual backup/restore enqueue workers; scheduled REGULAR completes | logcat `WorkManagerScheduler` + worker UUID within 5s; `BackupState(FINISHED_BACKUP)` |
| 014 AC-1 | `@AndroidEntryPoint` on services | Annotate both services | `grep -rn "extends ServiceBase\|extends Service"` lists only `@AndroidEntryPoint` classes |
| 014 AC-2 | Dagger graph resolves | Remove vestigial `CalendarSyncer @Inject` | `:app:kaptDebugKotlin` zero `[Dagger/MissingBinding]` etc. |
| 014 AC-3/AC-4 | Null-check shims gone | Direct field returns; rename `injected*`→`preferences`/`authPreferences` | grep `injectedPreferences != null`/`injectedAuthPreferences != null` → 0; grep `new Preferences(getApplicationContext`/`new AuthPreferences(this)` in `service/` → 0 |
| 014 AC-5/AC-6 | Factory + FQN-construction gone | Deleted in 013; verified here | grep `getBackupTask`/`getRestoreTask` → 0; grep the 8 FQN `new ...` strings in `service/` → 0 |
| 014 AC-7/AC-8 | Test migration documented; no anon subclass | Option A (fallback B); document choice | impl-log names Option A/B; grep `new SmsBackupService() {`/`new SmsRestoreService() {` in `app/src/test/` → 0 |
| 014 AC-9/AC-10 | Suite green; coverage ≥70% | Migrate service tests; backfill if needed | `:app:testDebugUnitTest` 0 failures; `:app:jacocoTestCoverageVerification` BUILD SUCCESSFUL |
| 014 AC-11/AC-12 | On-device DI smoke | — | logcat `SmsSyncPlus` no NPE/ISE/IAE during manual backup/restore |
| EPIC SC-6 | All `injected*` field names gone | Rename in `ServiceBase` | `grep -rn "getBackupTask\|getRestoreTask\|injectedPreferences\|injectedAuthPreferences" service/` → 0 |

## Integration Contracts

> The three requirements introduce no new *external* boundary, but REQ-013's manual-dispatch
> migration **touches two existing contracts**.

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| `MailTransport` port (converter throws-type + `K9MailTransport` ctor signature narrowing) | `K9MailTransport` / `MessageConverter` (`mail.*`) | `BackupWorker`, `RestoreWorker`, `ServiceBase` (`service.*`) | service | CNTR-MODERNIZATION-007 | approved |
| `BackupScheduler` port — manual backup enqueue carrying `BackupType` (MANUAL/SKIP) | `WorkManagerScheduler` | `SmsBackupService`, `SmsRestoreService` | service | CNTR-MODERNIZATION-004 | approved (amend) |

**CNTR-MODERNIZATION-007 (MailTransport).** REQ-012 narrows `K9MailTransport`'s public
constructor from `throws MailException, MessagingException` to `throws MailException`, and
changes `MessageConverter.convertMessages()` to throw an app-owned type. Neither is a change to
the `MailTransport` *interface* (AC-8 forbids that, and the contract's port signatures are
unchanged). The contract's exception-translation table and "no k-9 crosses the port" invariant
are *strengthened*, not broken — REQ-012 makes CNTR-MODERNIZATION-007 Validation Rule 1
unconditionally true. **No contract revision is required**; the changes are *below* the port and
consistent with the contract's intent. (If the Artifact Librarian prefers to record the
constructor-signature narrowing as a versioned note, it is a non-breaking clarification.)

**CNTR-MODERNIZATION-004 (BackupScheduler).** REQ-013's manual-dispatch migration needs a
**type-carrying manual-enqueue seam** to preserve MANUAL/SKIP semantics (see `## Integration
Design`). The existing port has `scheduleImmediate()` (hard-codes `BROADCAST_INTENT`). Adding a
`scheduleManual(BackupType)` operation (or an overload that accepts a `BackupType`) is a
**new port operation** — a breaking addition to CNTR-MODERNIZATION-004's "nine operations"
clause. **This requires a contract amendment before the REQ-013 story is sprint-planned.**

### Contracts Needed (pre-sprint gate)

- [ ] **CNTR amendment needed (CNTR-MODERNIZATION-004):** add a manual-backup enqueue operation
      that carries `BackupType` (MANUAL/SKIP) so the manual UI path preserves notification,
      foreground, and SKIP semantics when delegating to `WorkManagerScheduler`. Run
      `/amp:create-contracts` (or amend the existing CNTR) before the REQ-013 story is
      sprint-planned. **Without this, REQ-013 cannot preserve manual-backup behavior (EPIC SC-10)
      and the only alternative — collapsing MANUAL→BROADCAST_INTENT — is a prohibited behavior
      change.**
- [ ] **CNTR clarification (optional, CNTR-MODERNIZATION-007):** record the `K9MailTransport`
      public-constructor signature narrowing (`throws MailException, MessagingException` →
      `throws MailException`) as a non-breaking note. Not a gate; consumers are unaffected.

### Story-authoring divergences to honor (not contract gates, but binding on the sprint planner)

- **Factory deletion ownership:** EPIC/REQ-014 text assigns `getBackupTask()`/`getRestoreTask()`
  deletion to REQ-014, but this design assigns it to **REQ-013** (its callers + the constructed
  AsyncTask classes are deleted there; leaving dead factories referencing deleted classes would
  not compile). **REQ-013 deletes the factories; REQ-014 AC-5/AC-6 is verification-only.** Story
  authors must encode the deletion in the REQ-013 story's ACs and reduce REQ-014 AC-5/AC-6 to a
  grep-zero gate. (See `## Component Design > REQ-014 factory removal`.)
- **Comment-scrub ownership:** REQ-012 (not REQ-013) owns rewording the 12 surviving
  comment/javadoc `com.fsck.k9` residuals to reach the AC-1/AC-2 grep-zero. (See `## Component
  Design > REQ-012 comment/javadoc scrub`.)
- **Worker→foreground signalling path:** REQ-013 owns building the service-side `WorkInfo`
  observer (Option b1) as a **blocking prerequisite** of the dispatch rewrite (Step 2a). It is not
  a "verify" task. (See `## Integration Design` foreground decision and R-2.)

## Sequencing & Dependencies

This is the most important section: the three requirements share `ServiceBase`,
`SmsBackupService`, `SmsRestoreService`, `BackupTask`, `RestoreTask`. The order is **012 → 013 →
014**, matching EPIC-MODERNIZATION-005 §Sequencing Constraint and verified against the actual
coupling at each shared file.

### Ordered plan with rationale

**Step 1 — REQ-012 (ACL seal).** Purely additive boundary work inside `mail.transport.*` and
`mail.MessageConverter`, plus deletion of two FQN catches in `BackupTask`/`ServiceBase`. It does
**not** restructure the services or the dispatch path. Doing it first means the `K9MailTransport`
constructor signature (`throws MailException` only) and the app-owned `convertMessages` throws are
in place before any later step relies on them. Critically, REQ-012 seals the
`ServiceBase.getMailTransport()` seam (deletes its FQN catch) **before** REQ-013 potentially
renders that seam dead — so the seam is correct whether or not it survives.

**Step 2 — REQ-013 (legacy-path removal).** This step has an **internal ordered prerequisite**
that must precede the deletions:

   - **Step 2a (BLOCKING PREREQUISITE) — build the worker→foreground/stop signalling path.**
     Before any AsyncTask is deleted, the service must gain a `WorkManager.getWorkInfo*LiveData`
     observer (Option b1, `## Integration Design`) that re-invokes the existing
     `backupStateChanged()`/`restoreStateChanged()` driver from `WorkInfo` progress/terminal
     state. Rationale (re-verified this session): the `startForeground`/`stopForeground`/`stopSelf`
     driver is called **only** by the AsyncTask's direct `*StateChanged()` call
     (`BackupTask.java:273`/`RestoreTask.java:328`); the workers emit only `setProgress`; no
     service-side StateFlow collector exists. Deleting the AsyncTask **before** this observer
     exists would silently drop manual foreground promotion and the `stopSelf()` teardown. This is
     not a post-hoc verification — the bridge does not exist and must be implemented first.
   - **Step 2b — migrate manual dispatch** (`backup()`/`handleIntent()` → scheduler), now that the
     service can drive foreground from `WorkInfo`.
   - **Step 2c — delete** `BackupTask`/`RestoreTask` + tests + `BackupCancelCollector` + the four
     `jacocoFileFilter` lines, and delete `getBackupTask()`/`getRestoreTask()`.

   Step 2c removes `CalendarSyncer`'s only `@Inject`-mediated consumer (`Lazy<CalendarSyncer>` in
   `BackupTask`), which is the precondition for REQ-014's `@AndroidEntryPoint` to compile cleanly.
   **REQ-013 must merge before REQ-014's shim-removal phase begins** (EPIC §Sequencing; verified
   necessity via the `CalendarSyncer` MissingBinding analysis).

**Step 3 — REQ-014 (Hilt completion).** With the AsyncTask classes and factories gone, apply
`@AndroidEntryPoint`, remove the `ServiceBase` null-check shims (and rename `injected*` fields),
remove the vestigial `CalendarSyncer @Inject`, and migrate the service tests. `@AndroidEntryPoint`
+ `kaptDebugKotlin` now resolves cleanly because no broken `@Inject` (`CalendarSyncer`'s `long`)
is in any live graph path.

### Per-shared-file coordination

| Shared file | REQ-012 touch | REQ-013 touch | REQ-014 touch | Coordination |
|-------------|---------------|---------------|---------------|--------------|
| `ServiceBase.java` | Delete FQN catch in `getMailTransport()` (`:171-176`); narrow to `throws MailException` only (relies on K9MailTransport ctor change) | none (seam may become dead but is retained) | Remove null-check shims in `getPreferences()`/`getAuthPreferences()` (`:186-200`); rename `injected*` fields (`:84-85`) | 012 edits the catch block; 014 edits the accessor methods + fields. **Disjoint regions** — but both modify the same file; sequence 012→014 (013 doesn't touch it). If parallel stories, 012 merges first. |
| `BackupTask.java` | Delete inner FQN catch (`:306-310`) | **Delete entire file** | none (gone) | 012's edit to `BackupTask` is **moot if 013 deletes it first** — but 012 lands first, so 012 edits then 013 deletes. The 012 edit to `BackupTask` is still required for the *interim* compile (between 012 merge and 013 merge). |
| `RestoreTask.java` | none (its k-9 catch was already removed in U-026; verified no `com.fsck.k9` in `RestoreTask.java`) | **Delete entire file** | none | Only 013 touches it. |
| `SmsBackupService.java` | Reword comment k-9 residuals (`:27,78`) | **Add `WorkInfo` observer (Step 2a) that re-invokes `backupStateChanged()`**; rewrite `backup()` to `scheduler.scheduleManual(...)`; delete `getBackupTask()` (`:202-240`); remove `getMailTransport()` use | Add `@AndroidEntryPoint`; (factory already gone) | 012 reword + 013 dispatch/observer/factory + 014 annotation. Sequence 012→013→014; the `WorkInfo` observer is the Step 2a prerequisite before the dispatch rewrite. |
| `SmsRestoreService.java` | Reword comment k-9 residuals (`:11,12,41,42`) | **Add `WorkInfo` observer (Step 2a) that re-invokes `restoreStateChanged()`**; rewrite `handleIntent()` to `scheduler.scheduleRestore(...)`; delete `getRestoreTask()` (`:129-143`); remove `getMailTransport()` use; **clearCache() retained in onCreate**; restore-wakelock decision | Add `@AndroidEntryPoint` | Same as above. clearCache stays a service responsibility. |
| `BackupCancelCollector.kt` | none | **Delete** (only consumers are the deleted tasks) | none | Only 013. |
| `CalendarSyncer.java` | none | (its `Lazy` consumer is deleted with `BackupTask`) | Remove vestigial `@Inject` ctor annotation | 013 removes the consumer; 014 removes the annotation. Sequence 013→014. |
| `app/build.gradle` | none | Remove 4 `jacocoFileFilter` lines (`:229-232`) | none | Only 013. |
| `service/state/State.java` | Reword the 3 comment k-9 residuals (`:6,7,8` — the "U-026 AC-7 …import removed" annotations) to reach AC-2 grep-zero across `service/state/`; **no code change** (cause-chain `getCause()` at `:59` is untouched) | none | none | Only 012 (comment reword). |
| `BackupWorker.kt` / `RestoreWorker.kt` | Reword surviving comment k-9 residuals (`BackupWorker.kt:55`; `RestoreWorker.kt:30,66`); **no engine/code change** | none (worker behavior unchanged — EPIC constraint) | none | Only 012 (comment reword). The `setProgress` emission these workers use is the source the 013 `WorkInfo` observer reads — unchanged. |
| `SmsBackupServiceTest.java` | none | Remove `@Mock BackupTask`, `getBackupTask()` override | Replace anonymous subclass (Option A) | 013 removes the BackupTask mock; 014 removes the anonymous subclass. Coordinate as one test-migration edit if 013+014 are one story; if separate, 013 leaves the test compiling against the deleted-factory by removing only the `getBackupTask()` override, and 014 finishes the Hilt migration. |

**If REQ-013 and REQ-014 are separate stories:** REQ-013 must merge (deletion + dispatch
migration + factory removal) **before** REQ-014 begins, per EPIC §Sequencing. The
`SmsBackupServiceTest` edit straddles both — the cleanest split is to do the entire test
migration in REQ-014 and have REQ-013 leave the test in a compiling state (it can still reference
the service; only the `getBackupTask()` override and `@Mock BackupTask` must go when the factory
is deleted). The design **recommends a single combined story (or sprint) for 013+014** given the
tight test coupling; 012 can be a separate, earlier story.

## Trade-offs

| Decision | Chosen | Alternative rejected | Why |
|----------|--------|----------------------|-----|
| REQ-013 dispatch migration | Services delegate to `BackupScheduler` (Option a) | Rewrite `MainActivity` to enqueue directly, bypass services (Option b) | Option (a) preserves the foreground/lifecycle scaffolding that REQ-014 needs and that the manual path requires; Option (b) would prematurely strip the services (REQ-013 §"Scope conditionally" keeps `MainActivity`/services in place). REQ-013 AC-1 explicitly prefers (a). |
| REQ-012 converter fix | Option (a): `convertMessages` throws app-owned `MailException` | Option (b): `mail.transport` wrapper method | (a) is local, touches one signature, and both callers already catch `MailException`; (b) would route the converter through the transport, complicating the loop for no benefit (the converter is an injected collaborator, not a transport concern). |
| Manual-backup type carrying | New `scheduleManual(BackupType)` seam on `BackupScheduler` (contract amend) | Reuse `scheduleImmediate()` (collapse MANUAL/SKIP → BROADCAST_INTENT) | Reuse changes notification/foreground/SKIP behavior — a prohibited behavior change (EPIC SC-10). The type-carrying seam preserves manual semantics; the cost is a contract amendment. |
| Foreground/wakelock in worker model | Service stays a thin foreground/lifecycle shell that **observes the worker via `WorkManager.getWorkInfo*LiveData`** (Option b1) and re-invokes the existing `*StateChanged()` foreground driver | Add `setForeground()` to the workers; OR bridge worker→`syncStateRepository` + service collector (Option b2) | EPIC carves worker changes out of scope and REQ-013 forbids changing worker behavior; keeping promotion in the service preserves behavior without touching the workers. **No worker→repository or service-side StateFlow bridge exists today** — the service-side `WorkInfo` observer must be **built** (it is a blocking prerequisite of the dispatch rewrite, not a "verify"). See R-2. |
| `CalendarSyncer` MissingBinding | Remove the vestigial `@Inject` annotation | Add `@Named("calendarId") long` provider in a new `EngineModule` | The graph never builds `CalendarSyncer` (worker + tests construct it directly); binding a mutable preference as a singleton `long` is semantically wrong. Removing the unsatisfiable `@Inject` is minimal and correct. |
| Robolectric migration | Option A (constructor/field seam), fallback Option B | Option B (`@HiltAndroidTest`) as primary | A is lower-ceremony, matches the U-023 precedent and the `@HiltWorker` direction; B is the documented fallback if A cannot satisfy AC-8 without the Hilt lifecycle. |

## Risks

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|------------|--------|------------|
| R-1 | Deleting `BackupTaskTest`/`RestoreTaskTest` drops coverage of surviving `service.*` classes they transitively exercise, breaching the 70% `service*` gate after the exclusion removal | Medium | High | REQ-013 AC-10 gate run; backfill characterization tests on `BackupConfig`/`BackupCursors`/`BulkFetcher`/surviving service code (the U-006/U-017 remedy). Run `jacocoTestCoverageVerification` before merge. |
| R-2 | Manual foreground/stop behavior not preserved: the worker emits state **only** via `setProgress(WorkData)` (`BackupWorker.kt:337`), **never** `emitState()`; and `backupStateChanged`/`restoreStateChanged` are **direct-call methods, not StateFlow observers** — driven today solely by the AsyncTask (`BackupTask.java:273`/`RestoreTask.java:328`). Deleting the AsyncTask removes the **only** caller of `startForeground`/`stopForeground`/`stopSelf`. **No bridge exists to "verify" — one must be built.** | High (certain absent mitigation) | High | **Blocking prerequisite, not a verify:** REQ-013 MUST implement Option (b1) — a service-side `WorkManager.getWorkInfo*LiveData` observer (registered in `backup()`/`handleIntent()` after enqueue) that maps `WorkInfo` progress/terminal state into a `BackupState`/`RestoreState` and re-invokes the existing `backupStateChanged()`/`restoreStateChanged()` driver, then tears down on `stopSelf()`. The dispatch rewrite **must not merge** until manual foreground promotion + teardown + notification are proven preserved (test + on-device smoke AC-13/AC-14). Option (b2) (worker→`emitState` + service collector) is the only fallback and requires a worker-scope waiver. |
| R-3 | Restore `FULL_WAKE_LOCK` (screen-on) lost when restore moves to a worker (WorkManager holds only PARTIAL) | Medium | Medium | AC-8 documented decision: either accept the UX change or have `SmsRestoreService` hold the `FULL_WAKE_LOCK` around the enqueue+observe window (recommended). Reviewer sign-off required. |
| R-4 | Cancellation regression: the UI cancel emitter is `StatusPreference.java:279-283` (backup) / `:304-308` (restore) → `tryEmitEvent(SyncEvent.Cancel(USER))`, consumed **today only** by `BackupCancelCollector` → `task.onCancelRequested()`. Deleting the collector + tasks leaves `Cancel(USER)` with **no consumer** — a manual run becomes uncancellable from the UI. | Medium | Medium | **REQ-013 must rewire** `StatusPreference`'s cancel to `WorkManager.cancelUniqueWork(<uniqueName>)` via the injected `BackupScheduler` (targeting the `BackupType.MANUAL.name()` / `RESTORE_WORK_NAME` unique-work names enqueued by the dispatch migration). Cooperative `ensureActive()` in the workers handles the interrupt; the R-2 `WorkInfo` observer drives the resulting `stopForeground/stopSelf`. Add a cancel regression test. |
| R-5 | `@AndroidEntryPoint` surfaces a Dagger error other than `CalendarSyncer` (e.g. an unbound type reachable from a service `@Inject` field) | Low | Medium | `kaptDebugKotlin` is the gate (AC-2); the only `@Inject` fields on `ServiceBase` are `Preferences`/`AuthPreferences`, both `@Singleton`-provided (verified). The `CalendarSyncer` `long` is the only known unbound type and is removed. |
| R-6 | Contract amendment (CNTR-MODERNIZATION-004 `scheduleManual`) not done before sprint-planning REQ-013 → manual semantics silently collapse | Medium | High | Pre-sprint contract gate (see `## Integration Contracts`). The REQ-013 story is **blocked** until the amendment is approved. |
| R-7 | Sequencing inverted (014 before 013) → `@AndroidEntryPoint` triggers `CalendarSyncer` MissingBinding while `BackupTask` is still in the graph path | Low | High | Hard ordering 012→013→014 documented; 013 merges before 014's shim phase (EPIC §Sequencing). |

## Notes

- **Source verified this session** (read in full): `BackupTask.java`, `RestoreTask.java`,
  `ServiceBase.java`, `SmsBackupService.java`, `SmsRestoreService.java`, `BackupWorker.kt`,
  `RestoreWorker.kt`, `WorkManagerScheduler.kt`, `BackupScheduler.java`, `MailModule.kt`
  (contains `MailTransportFactory`), `K9MailTransport.java`, `MailTransport.java`,
  `MailException.java`, `MessageConverter.java` (relevant methods), `CalendarSyncer.java`,
  `BackupCancelCollector.kt`, `SmsBackupServiceTest.java`, `SmsRestoreServiceTest.java`,
  `State.java` (error-message/cause-chain region), `app/build.gradle` (jacoco region),
  `MainActivity.java` (dispatch region), `PreferencesModule.kt`. Scope claims marked "scope
  verified via grep" were confirmed by ripgrep this session (no `EngineModule`; no `calendarId`
  binding; `convertMessages` callers = `BackupTask` + `BackupWorker`; `new K9MailTransport(`
  sites = `ServiceBase` + `MailModule`).
- **No user-facing change** (EPIC SC-10): the design's behavior-preservation hazards are R-2
  (the manual foreground/stop signalling path — which **does not exist today** and must be **built**
  as a service-side `WorkInfo` observer, Step 2a, a blocking prerequisite of the dispatch rewrite),
  R-4 (the manual cancel path — `StatusPreference` `Cancel(USER)` must be rewired to
  `WorkManager.cancelUniqueWork`, as its `BackupCancelCollector` consumer is deleted), and R-3
  (restore screen-on `FULL_WAKE_LOCK`). R-2 and R-4 are **build tasks**, not verifications; all
  three carry explicit decisions and reviewer/test gates. None changes data, OAuth, scheduling, or
  the `com.zegoggles.smssync.BACKUP` broadcast contract. `SmsRestoreService.clearCache()` is
  retained unchanged in service `onCreate` (REQ-013 Notes).
- **Style/precedent:** this design follows DES-MODERNIZATION-009 (ACL) and the
  DES-MODERNIZATION-008 Hilt-coexistence approach; the `@Inject`/`Lazy<CalendarSyncer>` history
  is from U-023, and the worker model is U-015/U-017/U-024.

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| EPIC-MODERNIZATION-005 | sdlc/artifacts/requirements/modernization/EPIC-MODERNIZATION-005-modernization-debt-closure.md | Parent epic; scope, success criteria, sequencing constraint |
| REQ-MODERNIZATION-012 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-012-seal-mailtransport-acl-exception-boundary.md | ACL-seal requirement (AC-1..10) |
| REQ-MODERNIZATION-013 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-013-remove-legacy-asynctask-execution-path.md | Legacy-path removal requirement (AC-1..15) |
| REQ-MODERNIZATION-014 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-014-complete-hilt-service-injection.md | Hilt completion requirement (AC-1..12) |
| DES-MODERNIZATION-009 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-009-mail-acl-k9-unpin-design.md | ACL design precedent; ADR-009-B residual scoping; style |
| CNTR-MODERNIZATION-007 | sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md | MailTransport port contract (REQ-012 binding); immutability + cause-chain rules |
| BackupTask.java | app/src/main/java/com/zegoggles/smssync/service/BackupTask.java | FQN catch at :306-310; `Lazy<CalendarSyncer>`; AsyncTask to delete |
| RestoreTask.java | app/src/main/java/com/zegoggles/smssync/service/RestoreTask.java | AsyncTask to delete; no k-9 catch remains |
| ServiceBase.java | app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java | FQN catch at :171-176; null-check shims :186-200; @Inject fields :84-85; locks |
| SmsBackupService.java | app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java | `backup()` dispatch :130-159; `getBackupTask()` :202-240; foreground/notify; getScheduler |
| SmsRestoreService.java | app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java | `handleIntent()` :87-121; `getRestoreTask()` :129-143; FULL_WAKE_LOCK :203-212; clearCache |
| BackupWorker.kt | app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt | Live engine; convertMessages call :310; inferBackupType; setProgress; wakelock comment |
| RestoreWorker.kt | app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt | Live engine; importMessageBody; checkpoint key; ensureActive |
| WorkManagerScheduler.kt | app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt | scheduleImmediate (BROADCAST_INTENT) :270-282; scheduleRestore :295-307 |
| BackupScheduler.java | app/src/main/java/com/zegoggles/smssync/scheduler/BackupScheduler.java | Port (CNTR-MODERNIZATION-004); nine operations; manual seam absent |
| MailModule.kt | app/src/main/java/com/zegoggles/smssync/di/MailModule.kt | MailTransportFactory; redundant K9MailTransport ctor wrap :93-97 |
| K9MailTransport.java | app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java | public ctor throws MessagingException :117-118; translation adapter; test ctor :93 |
| MailTransport.java | app/src/main/java/com/zegoggles/smssync/mail/transport/MailTransport.java | Port interface (must not change — AC-8) |
| MailException.java | app/src/main/java/com/zegoggles/smssync/mail/transport/MailException.java | `MailException(Throwable)` cause-preserving ctor :44 |
| MessageConverter.java | app/src/main/java/com/zegoggles/smssync/mail/MessageConverter.java | `convertMessages` throws MessagingException :120-121 (primary leak) |
| CalendarSyncer.java | app/src/main/java/com/zegoggles/smssync/service/CalendarSyncer.java | Unbound `long calendarId` @Inject ctor :32-41 (latent MissingBinding) |
| PreferencesModule.kt | app/src/main/java/com/zegoggles/smssync/di/PreferencesModule.kt | Confirms no calendarId/EngineModule binding; @Singleton Preferences/AuthPreferences |
| BackupCancelCollector.kt | app/src/main/java/com/zegoggles/smssync/service/BackupCancelCollector.kt | service.* file referencing BackupTask/RestoreTask — delete in 013 |
| SmsBackupServiceTest.java | app/src/test/java/com/zegoggles/smssync/service/SmsBackupServiceTest.java | Anonymous-subclass pattern :68-80; @Mock BackupTask (Robolectric blocker) |
| SmsRestoreServiceTest.java | app/src/test/java/com/zegoggles/smssync/service/SmsRestoreServiceTest.java | setupService pattern :25; wakeLockType test |
| State.java | app/src/main/java/com/zegoggles/smssync/service/state/State.java | getDetailedErrorMessage reads getCause() :59 (cause-chain consumer) |
| MainActivity.java | app/src/main/java/com/zegoggles/smssync/activity/MainActivity.java | startBackup/startRestore startService dispatch :405-434 |
| app/build.gradle | app/build.gradle | jacocoFileFilter exclusions :229-232; per-package 70% gate :304-343 |
| StatusPreference.java | app/src/main/java/com/zegoggles/smssync/activity/StatusPreference.java | UI cancel emitter: onBackup `Cancel(USER)` :279-283, onRestore :304-308 (R-4 call site, re-verified) |
| K9MailTransport.java (auth catches) | app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java | AC-5 auth translation: XOAuth2/AuthenticationFailed catches :152-247; translateAuthException :364-376 |
| XOAuth2FailedException.java | app/src/main/java/com/zegoggles/smssync/mail/transport/XOAuth2FailedException.java | `extends MailException` :28 — AC-5 type-lattice confirmation |
| MessageConverterTest.java | app/src/test/java/com/zegoggles/smssync/mail/MessageConverterTest.java | convertMessages direct callers :121,125,143,147,165,169 (no thrown-type assertion) — AC-9/AC-10 test scope |
| BackupTaskTest.java | app/src/test/java/com/zegoggles/smssync/service/BackupTaskTest.java | Mocks convertMessages :117-237 — deleted by REQ-013 (mocks moot) |
