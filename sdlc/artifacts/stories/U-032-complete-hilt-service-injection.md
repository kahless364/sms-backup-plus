---
type: story
status: ready
artifact_type: user-story
priority: medium
complexity: medium
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-014
design_docs:
  - DES-MODERNIZATION-012
integration_contracts: []
dependencies:
  - U-031
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-032
title: 'Complete Hilt service injection: @AndroidEntryPoint + remove coexistence shims'
pipeline: ''
domain: modernization
alignment_audit: passed
---

# U-032: Complete Hilt service injection: @AndroidEntryPoint + remove coexistence shims

## Story

As a maintainer,
I want the services fully Hilt-injected with `@AndroidEntryPoint` and the manual-construction coexistence shims removed,
so that there is a single construction mechanism and no injected-vs-manual divergence across the service layer.

## Acceptance Criteria

- [ ] AC-1: Given U-031 has merged (factory deletion + CalendarSyncer `@Inject` removal + services retained), when `app/src/main/java/com/zegoggles/smssync/service/` is grepped for classes that `extend ServiceBase`, then every such class (`SmsBackupService`, `SmsRestoreService`) carries `@dagger.hilt.android.AndroidEntryPoint` in the same file. Verified by: `grep -rn "extends ServiceBase\|extends Service" app/src/main/java/` lists only files that also contain `@AndroidEntryPoint`.

- [ ] AC-2: Given `@AndroidEntryPoint` has been applied to both service classes, when `./gradlew :app:kaptDebugKotlin` (or `kaptDebugJava`) is run, then the build completes with zero Dagger processor errors — no `[Dagger/MissingBinding]`, `[Dagger/DuplicateBindings]`, or `[Dagger/IncompatiblyScopedBindings]` appear in the build output, confirming the Hilt component graph resolves all injection points on the annotated services.

- [ ] AC-3: Given the `@AndroidEntryPoint` annotation is live and Hilt member injection fires before `onCreate()`, when `grep -n "injectedPreferences != null\|injectedAuthPreferences != null" app/src/main/java/` is run, then the output is empty (zero matches). The null-check conditional branches are removed from `ServiceBase.getPreferences()` and `ServiceBase.getAuthPreferences()`.

- [ ] AC-4: Given the null-check fallbacks are removed, when `grep -n "new Preferences(getApplicationContext\|new AuthPreferences(this)" app/src/main/java/com/zegoggles/smssync/service/` is run, then the output is empty (zero matches). No on-demand manual construction of `Preferences` or `AuthPreferences` remains in the `service/` package.

- [ ] AC-5 (verification gate — deletion performed by U-031): Given U-031 has merged, when `grep -n "getBackupTask\|getRestoreTask" app/src/main/java/` is run against production source, then the output is empty (zero matches). This confirms U-031 completed the factory deletion; this story does not perform the deletion and must not attempt it.

- [ ] AC-6 (verification gate — deletion performed by U-031): Given U-031 has merged, when `grep -rn "new com.zegoggles.smssync.service.BackupItemsFetcher\|new com.zegoggles.smssync.service.BackupQueryBuilder\|new com.zegoggles.smssync.mail.PersonLookup\|new com.zegoggles.smssync.contacts.ContactAccessor\|new com.zegoggles.smssync.mail.MessageConverter\|new com.zegoggles.smssync.auth.OAuth2Client\|new com.zegoggles.smssync.auth.TokenRefresher\|new com.zegoggles.smssync.service.CalendarSyncer" app/src/main/java/com/zegoggles/smssync/service/` is run, then the output is empty (zero matches). This confirms no fully-qualified manual-construction site from the former factory methods remains.

- [ ] AC-7 (CalendarSyncer MissingBinding — verification gate): Given U-031 removed the vestigial `@Inject` annotation from `CalendarSyncer`'s constructor (`CalendarSyncer.java:32`), when `./gradlew :app:kaptDebugKotlin` runs after `@AndroidEntryPoint` is applied (AC-2), then no `[Dagger/MissingBinding] long cannot be provided` error appears. The compiler gate (AC-2) is the combined verification; this AC makes the CalendarSyncer precondition explicit for code review.

- [ ] AC-8 (field rename — required for EPIC SC-6): Given the field-shadowing hazard motivating the `injected`-prefix is resolved by the test migration below, when `grep -rn "injectedPreferences\|injectedAuthPreferences" app/src/main/java/com/zegoggles/smssync/service/` is run, then the output is empty (zero matches). The fields in `ServiceBase` are renamed from `injectedPreferences`/`injectedAuthPreferences` to `preferences`/`authPreferences`, and `getPreferences()`/`getAuthPreferences()` return those fields directly.

- [ ] AC-9 (Robolectric migration — approach declaration): Given the implementing developer has resolved the Robolectric anonymous-subclass blocker, the implementation log for this story explicitly names the chosen approach (Option A: constructor/field-seam refactor, or Option B: `@HiltAndroidTest` + `HiltTestApplication`) and includes a brief rationale for the choice. This is a documentation criterion verified during code review, not by a build command.

- [ ] AC-10 (Robolectric migration — anonymous-subclass removal): Given the test migration is complete, when `grep -n "new SmsBackupService() {" app/src/test/` and `grep -n "new SmsRestoreService() {" app/src/test/` are each run, then both return zero matches. The anonymous-subclass instantiation in `SmsBackupServiceTest.java:68-80` is removed; `SmsRestoreServiceTest.java`'s `Robolectric.setupService(SmsRestoreService.class)` pattern is addressed under whichever migration option is chosen.

- [ ] AC-11: Given the full unit test suite is run after all changes in this story, when `./gradlew :app:testDebugUnitTest` completes, then there are zero test failures. All tests that existed before this story and tested service behavior either continue to pass or are replaced with equivalent tests covering the same behavior; no service behavior test is deleted without a replacement.

- [ ] AC-12: Given the unit tests pass, when `./gradlew :app:jacocoTestCoverageVerification` is run, then the build result is `BUILD SUCCESSFUL` — the JaCoCo per-package LINE coverage (counter `LINE` / `COVEREDRATIO`) for `com.zegoggles.smssync.service*`, `com.zegoggles.smssync.mail*`, and `com.zegoggles.smssync.auth*` each remain at or above 70%.

- [ ] AC-13: Given a debug APK from this branch is installed on a physical device or emulator (API 26+), when a manual backup run is initiated from `MainActivity`, then logcat output at `adb logcat -s SmsSyncPlus` shows no `NullPointerException`, no `IllegalStateException` containing "Hilt component not initialized", and no `IllegalArgumentException` during the backup run.

- [ ] AC-14: Given the same device and APK as AC-13, when a manual restore run is initiated from `MainActivity`, then logcat output at `adb logcat -s SmsSyncPlus` shows no Hilt DI initialization error during the restore run.

### Integration Criteria

- [ ] IC-1: `SmsBackupService` and `SmsRestoreService` are annotated with `@dagger.hilt.android.AndroidEntryPoint` and their generated Hilt base classes (`Hilt_SmsBackupService`, `Hilt_SmsRestoreService`) appear as intermediate superclasses in the kapt-generated output under `app/build/generated/`.
- [ ] IC-2: `ServiceBase`'s `@Inject`-annotated fields (`preferences`, `authPreferences` after rename) are populated at `onCreate()` entry via Hilt member injection, confirmed by AC-2 compiler clean and AC-13/AC-14 on-device smoke.
- [ ] IC-3: All call sites for `getPreferences()` and `getAuthPreferences()` throughout `service/` continue to compile and behave identically — they now return the renamed `@Inject` fields directly with no null-check branch.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java` | `@Inject Preferences injectedPreferences` / `@Inject AuthPreferences injectedAuthPreferences` fields (`:84-85`); `getPreferences()` (`:198-200`) and `getAuthPreferences()` (`:186-188`) return the injected field when non-null, else construct a new instance on demand (null-check coexistence shim) | Add `@AndroidEntryPoint` to concrete subclasses; remove the null-check branches from both accessors; rename `injectedPreferences`→`preferences` and `injectedAuthPreferences`→`authPreferences`; both methods become direct field returns |
| `app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java` | No `@AndroidEntryPoint`; retains TODO comment noting the deferral; references `getBackupTask()` (already absent after U-031) | Add `@AndroidEntryPoint`; remove TODO comment |
| `app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java` | No `@AndroidEntryPoint`; retains TODO comment noting the deferral; references `getRestoreTask()` (already absent after U-031) | Add `@AndroidEntryPoint`; remove TODO comment |
| `app/src/main/java/com/zegoggles/smssync/service/CalendarSyncer.java` | Vestigial `@Inject` annotation on constructor (`:32`) with unbound `long calendarId` parameter — already removed by U-031 | No change expected here; AC-7 verifies U-031 completed it |
| `app/src/test/java/com/zegoggles/smssync/service/SmsBackupServiceTest.java` | Anonymous-subclass pattern at `:68-80` overriding `getBackupTask()`, `getScheduler()`, `getPreferences()`, `getAuthPreferences()`, `getApplicationContext()`, `getResources()`, `checkPermission()`, `notifyUser()`; `@Mock BackupTask backupTask` field (`:61`) | Migrate off anonymous subclass per AC-9 chosen approach; remove `@Mock BackupTask` (already-deleted class after U-031); update dispatch assertions from `verify(backupTask).execute(...)` to `verify(scheduler).scheduleManual(...)` or equivalent |
| `app/src/test/java/com/zegoggles/smssync/service/SmsRestoreServiceTest.java` | Uses `Robolectric.setupService(SmsRestoreService.class)` at `:25`; `wakeLockType_returnsBrightScreen` test retained | Address `setupService` Hilt incompatibility under chosen approach (Option A or B); retain all existing test assertions unless replaced by equivalents |

## Existing Behavior to Preserve

- Backup and restore operations initiated from `MainActivity` complete without error — same data, same IMAP behaviour, no skipped messages.
- `SmsBackupService.getPreferences()` and `SmsRestoreService.getAuthPreferences()` (and all callers throughout `service/`) return the same singleton instances they returned before — now via direct `@Inject` field return rather than null-check dispatch.
- `SmsRestoreService.wakeLockType()` continues to return a non-zero wake lock type (asserted by `wakeLockType_returnsBrightScreen` test).
- `SmsRestoreService.clearCache()` remains in service `onCreate` — its timing and behaviour are unchanged by this story (it is not in the removed factory code).
- The `TODO(U-023): add @AndroidEntryPoint` comments may be removed; no other comments are modified by this story (the comment/javadoc `com.fsck.k9` scrub is owned by REQ-012/U-030, not this story).
- All 568+ passing tests from the sprint-001 baseline continue to pass (no regressions).

## Verification Steps

1. Confirm U-031 is merged: run `git log --oneline | grep U-031` and verify the merge commit appears before beginning work.
2. AC-5 pre-check: `grep -n "getBackupTask\|getRestoreTask" app/src/main/java/` — must return zero matches before any edits.
3. AC-6 pre-check: run the 8-FQN grep from AC-6 against `service/` — must return zero matches.
4. AC-7 pre-check: confirm `CalendarSyncer.java:32` no longer carries `@Inject` (read the file; grep `@Inject` in `CalendarSyncer.java` — must return zero matches for the constructor-level annotation).
5. Apply `@AndroidEntryPoint` to `SmsBackupService` and `SmsRestoreService`. Run `./gradlew :app:kaptDebugKotlin`; verify zero Dagger errors (AC-2).
6. Remove null-check branches in `ServiceBase.getPreferences()` (`:198-200`) and `getAuthPreferences()` (`:186-188`); rename fields `injectedPreferences`→`preferences` and `injectedAuthPreferences`→`authPreferences` throughout the file. Run `grep -n "injectedPreferences != null\|injectedAuthPreferences != null" app/src/main/java/` — must return zero matches (AC-3). Run `grep -n "new Preferences(getApplicationContext\|new AuthPreferences(this)" app/src/main/java/com/zegoggles/smssync/service/` — must return zero matches (AC-4). Run `grep -rn "injectedPreferences\|injectedAuthPreferences" app/src/main/java/com/zegoggles/smssync/service/` — must return zero matches (AC-8).
7. Migrate `SmsBackupServiceTest.java` off the anonymous-subclass pattern (AC-10): remove `new SmsBackupService() { ... }` block at `:68-80` and `@Mock BackupTask backupTask` field (`:61`). Document the chosen approach (Option A or B) in the implementation log (AC-9). Run `grep -n "new SmsBackupService() {" app/src/test/` — must return zero matches.
8. Address `SmsRestoreServiceTest.java`'s `setupService(SmsRestoreService.class)` pattern under the same chosen approach. Run `grep -n "new SmsRestoreService() {" app/src/test/` — must return zero matches (AC-10).
9. Run `./gradlew :app:testDebugUnitTest` — must complete with zero test failures (AC-11).
10. Run `./gradlew :app:jacocoTestCoverageVerification` — must complete with `BUILD SUCCESSFUL` (AC-12).
11. Run `./gradlew :app:assembleDebug` — must succeed.
12. On-device smoke (AC-13): install the debug APK on an emulator (API 26+). Initiate a manual backup from `MainActivity`. Monitor `adb logcat -s SmsSyncPlus`. Confirm no `NullPointerException`, `IllegalStateException: Hilt component not initialized`, or `IllegalArgumentException` appears in logcat during the backup run.
13. On-device smoke (AC-14): from the same session, initiate a manual restore. Confirm no Hilt DI initialization error in logcat.
14. Code review gate (AC-9): reviewer confirms the implementation log names Option A or Option B and includes rationale.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (app module) | Apply `@AndroidEntryPoint` to services; remove null-check shims in `ServiceBase`; rename `injected*` fields; migrate Robolectric service tests | Developer |

## Technical Context

**Dependency: this story requires U-031 merged first.** U-031 (REQ-013) performs: (1) deletion of `BackupTask`/`RestoreTask` and their tests; (2) deletion of `getBackupTask()`/`getRestoreTask()` factory methods from `SmsBackupService`/`SmsRestoreService`; (3) removal of the vestigial `@Inject` annotation from `CalendarSyncer`'s constructor. If U-031 is not merged, applying `@AndroidEntryPoint` will cause a Dagger `[Dagger/MissingBinding] long cannot be provided` error from `CalendarSyncer`'s unsatisfiable `@Inject` constructor (verified: `CalendarSyncer.java:32-41` — `long calendarId` has no `@Named("calendarId") long` provider anywhere in `di/`; no `EngineModule` exists).

**Null-check shims to remove (ServiceBase.java):**
- `getAuthPreferences()` (`:186-188`): `return injectedAuthPreferences != null ? injectedAuthPreferences : new AuthPreferences(this);` — becomes `return authPreferences;`
- `getPreferences()` (`:198-200`): `return injectedPreferences != null ? injectedPreferences : new Preferences(getApplicationContext());` — becomes `return preferences;`
- Fields at `:84-85`: `@Inject Preferences injectedPreferences` and `@Inject AuthPreferences injectedAuthPreferences` — rename suffix removed.

**Rename is required (not optional):** EPIC-MODERNIZATION-005 Success Criterion 6 has an exit grep: `grep -rn "getBackupTask\|getRestoreTask\|injectedPreferences\|injectedAuthPreferences" service/` → 0. The rename must be applied; leaving the `injected*` names would leave the EPIC exit grep non-zero.

**Robolectric blocker — `SmsBackupServiceTest.java:68-80`:** The anonymous subclass creates `new SmsBackupService() { ... }`. When `@AndroidEntryPoint` is on `SmsBackupService`, the Hilt-generated `Hilt_SmsBackupService` is inserted into the inheritance chain and calls `inject(this)` in `attachBaseContext()`. Under a plain Robolectric `Application` (not `HiltTestApplication`), this throws `IllegalStateException`. Two resolution options per REQ-014 and DES-012:
- **Option A (preferred — lower ceremony):** Keep Robolectric but remove the anonymous-subclass. Replace with Robolectric `buildService(SmsBackupService.class)` and set the `@Inject` fields (`preferences`, `authPreferences`) directly on the service instance via reflection or a package-private test setter before exercising it. Dispatch assertions change from `verify(backupTask).execute(...)` to `verify(scheduler).scheduleManual(...)` (or the equivalent post-U-031 method). Consistent with the U-023 precedent and the `@HiltWorker` constructor-injection direction.
- **Option B (fallback — higher ceremony):** Configure `@Config(application = HiltTestApplication.class)` (or `robolectric.properties`) on the test class, annotate with `@HiltAndroidTest`, declare a `@HiltAndroidRule` in `@Before`, and supply doubles via `@BindValue` / `@UninstallModules`. Requires a custom Robolectric test runner with `HiltTestApplication`.

The implementing developer MUST choose exactly one option, document it in the implementation log (AC-9), and must not introduce a new TODO deferral for the anonymous-subclass issue (REQ-014 Constraint).

**`SmsRestoreServiceTest.java:25`** uses `Robolectric.setupService(SmsRestoreService.class)` rather than an anonymous subclass. `setupService` drives the service lifecycle including `attachBaseContext`, which triggers the same Hilt lifecycle incompatibility. The migration must address both test classes; the same option (A or B) should be applied consistently.

**`@HiltAndroidApp` and `@HiltWorker` are already done:** U-022 applied `@HiltAndroidApp` to `App`; U-024 applied `@HiltWorker` to `BackupWorker`/`RestoreWorker`. Only the two concrete service classes (`SmsBackupService`, `SmsRestoreService`) remain without `@AndroidEntryPoint`.

**Hilt version is fixed at 2.51.1 + kapt** (established in U-022). Do not upgrade Hilt or switch to KSP as part of this story (REQ-014 Constraint).

**`getScheduler()` is out of scope:** `ServiceBase.getScheduler()` delegates to `App.getScheduler(this)` — this is a retained Service-Locator call and is explicitly out of the shim-removal scope for this story (REQ-014 does not require converting it to `@Inject`).

## Supporting Documentation

- REQ-MODERNIZATION-014 — full AC set: AC-1 (`@AndroidEntryPoint`), AC-2 (kapt clean), AC-3/AC-4 (shim removal), AC-5/AC-6 (factory absence — verification-only per design), AC-7/AC-8 (test migration), AC-9/AC-10 (suite + coverage), AC-11/AC-12 (on-device smoke)
- DES-MODERNIZATION-012 §Component Design > REQ-014 — `@AndroidEntryPoint` application, shim removal, factory-removal ownership split, CalendarSyncer binding fix, Robolectric test migration (Option A recommended, Option B fallback)
- DES-MODERNIZATION-012 §Sequencing and Dependencies §Step 3 — ordering rationale: 012 → 013 → 014; U-031 (REQ-013) must merge before this story begins
- DES-MODERNIZATION-012 §REQ-014 shim removal — field rename required for EPIC SC-6 exit grep
- DES-MODERNIZATION-012 §CalendarSyncer MissingBinding analysis — why the `@Inject` removal must precede `@AndroidEntryPoint`

## Integration Contract References

No new contracts are produced or consumed by this story. The `BackupScheduler` contract (CNTR-MODERNIZATION-004) was amended by U-031 to add `scheduleManual(BackupType)`. The `MailTransport` contract (CNTR-MODERNIZATION-007) is unaffected by this story.

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes

The central risk for this story is the Robolectric test migration. The anonymous-subclass pattern in `SmsBackupServiceTest.java:68-80` has blocked `@AndroidEntryPoint` across three prior stories (U-022, U-023, U-024). This story must resolve it — not defer it again. Option A is preferred by DES-012 and aligns with the U-023 `BackupTask` precedent; Option B is the documented fallback. The implementing developer must choose, implement, and document the choice before this story can close.

The factory-method deletion (AC-5/AC-6) and the CalendarSyncer binding fix (AC-7) are verification-only gates in this story — they confirm U-031 completed prerequisite work. If either grep is non-zero at the start of this story, the developer must stop and resolve the U-031 prerequisite before proceeding.
