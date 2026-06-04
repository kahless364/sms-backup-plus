---
artifact_type: plan
story_id: U-032
verdict: PASS
agent: Developer
timestamp: "2026-06-04T00:00:00Z"
---

# Plan: U-032 — Complete Hilt Service Injection

## Objective

Complete Hilt DI migration for `SmsBackupService` and `SmsRestoreService` by applying
`@AndroidEntryPoint`, removing the null-check coexistence shims from `ServiceBase`, renaming
the `injected*` fields, fixing the latent CalendarSyncer `@Inject` binding error, and
migrating the Robolectric tests to be compatible with `@AndroidEntryPoint`.

## Prerequisites Verified

- U-031 merged: `git log --oneline | grep U-031` confirms commit 907c10c8
- `BackupTask`/`RestoreTask` deleted (AC-5/AC-6): grep-zero on getBackupTask/getRestoreTask in production source
- CalendarSyncer `@Inject` NOT removed by U-031 (coordination gap) — this story fixes it

## Implementation Steps

### Step 1: Fix CalendarSyncer (critical correction — AC-7)

Per orchestrator directive, U-031 did NOT remove the vestigial `@Inject` annotation from
`CalendarSyncer`. Without fixing this first, applying `@AndroidEntryPoint` to the services
would trigger a Dagger `[Dagger/MissingBinding] long cannot be provided` compile error
(CalendarSyncer's constructor has an unbound `long calendarId` parameter).

Action: Remove `@Inject` annotation and `import javax.inject.Inject` from CalendarSyncer.java.
Update javadoc to document the removal.

### Step 2: Apply @AndroidEntryPoint (AC-1, AC-2)

Add `@AndroidEntryPoint` annotation and matching import to:
- `SmsBackupService.java`
- `SmsRestoreService.java`

Remove TODO(U-023) deferral comments. Update class-level javadoc.

### Step 3: Remove coexistence shims from ServiceBase (AC-3, AC-4, AC-8)

In `ServiceBase.java`:
- Rename `@Inject Preferences injectedPreferences` → `@Inject Preferences preferences`
- Rename `@Inject AuthPreferences injectedAuthPreferences` → `@Inject AuthPreferences authPreferences`
- Simplify `getAuthPreferences()` to `return authPreferences;`
- Simplify `getPreferences()` to `return preferences;`

### Step 4: Migrate Robolectric tests (AC-9, AC-10)

**Chosen approach: Option A — Override onCreate() in anonymous subclasses to bypass Hilt injection**

Rationale: Option A is lower ceremony. Rather than introducing HiltTestApplication configuration
(Option B), override `onCreate()` in the test anonymous subclass to NOT call `super.onCreate()`,
which bypasses the Hilt-generated `Hilt_SmsBackupService.onCreate()` injection call. Overrides
for `getPreferences()`, `getAuthPreferences()`, and `getScheduler()` return mocks directly.

For `SmsBackupServiceTest.java`:
- Add `@Override public void onCreate() {}` (no super) in the anonymous subclass
- Rename mock fields `preferences`/`authPreferences` → `mockPreferences`/`mockAuthPreferences`
  to prevent field-shadowing of the now package-visible `ServiceBase` fields

For `SmsRestoreServiceTest.java` — two patterns to address:
- `charService` (characterization tests): change `setupService(SmsRestoreService.class)` to
  `buildService(SmsRestoreService.class).get()` — avoids `onCreate()` entirely
- Mock-based tests (anonymous subclass): add `@Override public void onCreate() {}` (no super),
  rename mock fields to `mockPreferences`/`mockAuthPreferences`

### Step 5: Fix JaCoCo classDirectories (build.gradle)

`@AndroidEntryPoint` causes Hilt to ASM-transform service classes at test runtime via the
`transformDebugUnitTestClassesWithAsm` task. JaCoCo exec data is from the transformed classes.
The existing config pointing only at pre-ASM javac dirs causes "classes do not match" warnings
and false-low (39%) coverage for the service package.

Fix: Use combined classDirectories in both `jacocoTestReport` and `jacocoTestCoverageVerification`:
- ASM-transformed dir: for correct hash matching of service/App classes
- javac dir: for mail/auth/other packages, with service/App classes excluded to prevent duplicates
- kotlin-classes dir: for Kotlin compiled sources

### Step 6: Verify all gates pass

- `./gradlew :app:hiltJavaCompileDebug` — zero Dagger errors
- `./gradlew :app:assembleDebug` — successful APK build
- `./gradlew :app:testDebugUnitTest` — 588 tests, zero failures
- `./gradlew :app:jacocoTestCoverageVerification` — BUILD SUCCESSFUL, all packages >= 70%
