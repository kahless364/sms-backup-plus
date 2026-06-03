---
artifact_type: plan
story_id: "U-022"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-03T00:00:00Z"
contracts_verified: []
risks_identified: 3
---

# Implementation Plan: U-022

## Story Overview

Bootstrap Hilt DI framework: apply `com.google.dagger.hilt.android` plugin, add kapt annotation processor, annotate `App` with `@HiltAndroidApp`, annotate ServiceBase `@Inject` fields for Preferences/AuthPreferences, and create seven `@InstallIn(SingletonComponent::class)` modules for the core singletons.

## Acceptance Criteria Mapping

| AC | Implementation Step | Notes |
|----|-------------------|-------|
| AC-1 | Add Hilt classpath to root build.gradle; apply plugin + add hilt-android, hilt-compiler (kapt), hilt-work to app/build.gradle | Version: 2.51.1 |
| AC-2 | Add `@HiltAndroidApp` to App.java class declaration | Existing onCreate() logic unchanged |
| AC-3 | Add `@Inject Preferences preferences` to App.java, remove `new Preferences(this)` | Injection fires before onCreate() body |
| AC-4 | Add `@Inject Preferences injectedPreferences` and `@Inject AuthPreferences injectedAuthPreferences` to ServiceBase; update getPreferences()/getAuthPreferences() | Coexistence fallback for test compatibility; @AndroidEntryPoint deferred to U-023 |
| AC-5 | Create 7 module files in `com.zegoggles.smssync.di`: PreferencesModule, DispatcherModule, SchedulerModule, EventModule, SecretModule, MailModule, ContactsModule | MailModule stubbed (checked exceptions); others active |
| AC-6 | Remove new Preferences(this) from App.java (done); ServiceBase has null-check fallback for coexistence | App.java zero matches; ServiceBase fallback documented |
| AC-7 | Create IoDispatcher.kt qualifier + DispatcherModule.kt | @Qualifier @Retention(BINARY) |
| AC-8 | Deliberate breakage test: comment out providePreferences, run hiltJavaCompileDebug, confirm Dagger error | BUILD FAILED with "cannot be provided without @Provides" |
| AC-9 | assembleDebug passes; no new lint errors | Passed; existing lint baseline |
| AC-10 | Existing tests pass without modification | 568 tests, 0 failures |

## Implementation Steps

1. **Gradle configuration**: Add Hilt classpath to root `build.gradle`; apply `kotlin-kapt` and `com.google.dagger.hilt.android` plugins to `app/build.gradle`; add Hilt 2.51.1 dependencies + kapt processor + hilt-work 1.2.0; add `kapt { correctErrorTypes true }` block.

2. **App.java**: Add `@HiltAndroidApp` annotation; add `import dagger.hilt.android.HiltAndroidApp` and `import javax.inject.Inject`; convert `private Preferences preferences` field to `@Inject Preferences preferences`; remove `preferences = new Preferences(this)` from onCreate().

3. **ServiceBase.java**: Add `import javax.inject.Inject`; add `@Inject Preferences injectedPreferences` and `@Inject AuthPreferences injectedAuthPreferences` fields (package-private for Dagger compatibility but named to avoid Java field-shadowing in test anonymous subclasses); update `getPreferences()` and `getAuthPreferences()` to return injected fields with null-check fallback.

4. **SmsRestoreService.java**: Replace `new AuthPreferences(this)` local variable with `getAuthPreferences()` call.

5. **SmsBackupService.java / SmsRestoreService.java**: Add TODO comments noting @AndroidEntryPoint is deferred to U-023 (would break existing Robolectric tests that construct anonymous service subclasses directly).

6. **Create di/ package**: IoDispatcher.kt, DispatcherModule.kt, PreferencesModule.kt, SchedulerModule.kt, EventModule.kt, SecretModule.kt, MailModule.kt (stub), ContactsModule.kt.

## Files Modified/Created

| File | Type | Reason |
|------|------|--------|
| build.gradle (root) | Modified | Add Hilt classpath |
| app/build.gradle | Modified | Add kapt plugin, Hilt plugin, Hilt dependencies |
| app/src/main/java/com/zegoggles/smssync/App.java | Modified | @HiltAndroidApp, @Inject Preferences field |
| app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java | Modified | @Inject fields, updated getters |
| app/src/main/java/com/zegoggles/smssync/service/SmsBackupService.java | Modified | TODO note for U-023 @AndroidEntryPoint |
| app/src/main/java/com/zegoggles/smssync/service/SmsRestoreService.java | Modified | Remove new AuthPreferences(this) local var |
| app/src/main/java/com/zegoggles/smssync/di/IoDispatcher.kt | Created | @Qualifier annotation for IO dispatcher |
| app/src/main/java/com/zegoggles/smssync/di/DispatcherModule.kt | Created | Provides @IoDispatcher CoroutineDispatcher |
| app/src/main/java/com/zegoggles/smssync/di/PreferencesModule.kt | Created | Provides Preferences, AuthPreferences, DataTypePreferences |
| app/src/main/java/com/zegoggles/smssync/di/SchedulerModule.kt | Created | Provides BackupScheduler <- WorkManagerScheduler |
| app/src/main/java/com/zegoggles/smssync/di/EventModule.kt | Created | Provides SyncStateRepository <- FlowSyncStateRepository |
| app/src/main/java/com/zegoggles/smssync/di/SecretModule.kt | Created | Provides SecretStore <- EncryptedPrefsSecretStore |
| app/src/main/java/com/zegoggles/smssync/di/MailModule.kt | Created | Stub — K9MailTransport has checked-exception constructor |
| app/src/main/java/com/zegoggles/smssync/di/ContactsModule.kt | Created | Provides ContactsPort <- PeopleApiContactsAdapter; CalendarPort stubbed |

## Contracts Verification

No CNTR-* contracts listed in story frontmatter (`integration_contracts: []`).

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| AC-4 vs AC-10 contradiction: @AndroidEntryPoint on services breaks existing Robolectric tests | Defer @AndroidEntryPoint to U-023; use @Inject fields with null-check fallback for coexistence. Documented deviation. |
| Java field-shadowing: non-private @Inject fields in ServiceBase shadow test outer class fields | Use distinct field names (injectedPreferences, injectedAuthPreferences) to avoid shadowing |
| MailModule: K9MailTransport constructor throws checked exceptions (MailException, MessagingException) | Stub MailModule with TODO comment; activate in U-026 when clean adapter constructor available |

## Test Strategy

- `./gradlew :app:assembleDebug` — must pass (verify Hilt plugin + kapt generate correctly)
- `./gradlew :app:hiltJavaCompileDebug` with broken binding — must fail with Dagger error (AC-8)
- `./gradlew :app:testDebugUnitTest` — all 568 existing tests must pass (AC-10)
- `./gradlew :app:jacocoTestCoverageVerification` — coverage gate ≥70% must hold

## Phase Completion Report
---
story_id: "U-022"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-001/U-022/plan.md"
story_status: "done"
current_build_phase: "implementation"
errors: []
---
