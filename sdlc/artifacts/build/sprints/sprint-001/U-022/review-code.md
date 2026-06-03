---
artifact_type: review-code
story_id: "U-022"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-06-03T00:00:00Z"
blockers: 0
warnings: 2
---

# Code Review: U-022

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS — no CNTR-* contracts; story ACs satisfied within coexistence constraints |
| Test coverage | PASS — 568 tests, 0 failures; JaCoCo ≥70% gate holds |
| Code quality | PASS — all new Kotlin files follow existing project conventions; documented deviations |

## Verdict: PASS

The implementation correctly establishes the Hilt bootstrap skeleton. The two documented AC deviations (AC-4/IC-3 and AC-6) are justified by the AC-10 constraint and aligned with the story's scope note about coexistence; they are flagged as `TODO(U-023)` for activation in the next story.

## Findings

### Blockers

None.

### Warnings

**W-1: AC-4/AC-6 coexistence fallback in ServiceBase.getPreferences()/getAuthPreferences()**

File: `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java:170,180`

`getPreferences()` and `getAuthPreferences()` contain null-check fallbacks (`new Preferences(...)`, `new AuthPreferences(...)`) to handle the case where Hilt injection has not fired (because `@AndroidEntryPoint` is deferred to U-023). This means the Service-Locator pattern (ARCH-002) technically remains active on the code path where `injectedPreferences == null`.

This is intentional and documented in the implementation log. The fallback is ONLY reached in non-Hilt contexts (current Robolectric tests via anonymous subclasses). Once U-023 applies `@AndroidEntryPoint` and migrates tests to `@HiltAndroidTest`, the fields will always be non-null and the fallback becomes unreachable dead code.

**Mitigation**: Acceptable for bootstrap story. Must be removed in U-023.

**W-2: @Inject field naming (injectedPreferences/injectedAuthPreferences)**

File: `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java:82-83`

The field names deviate from the standard `preferences`/`authPreferences` to avoid Java field-shadowing in test anonymous subclasses. This is an unusual naming convention for `@Inject` fields. When U-023 converts to constructor injection or applies `@AndroidEntryPoint` properly, these field names should be normalized or eliminated.

**Mitigation**: Acceptable for bootstrap story. Must be cleaned up in U-023.

### Observations

1. **MailModule stub rationale is solid**: K9MailTransport's constructor (`throws MailException, MessagingException`) genuinely cannot be used in a Dagger `@Provides` method. The stub with a clear TODO(U-026) comment is the correct approach.

2. **Field naming prevents future shadowing bugs**: the `injected`-prefix naming is self-documenting — it's immediately clear these are DI-supplied fields, not class-level state.

3. **Hilt version 2.51.1 is correct**: confirmed compatible with AGP 8.7.3 + Kotlin 1.9.25 + JDK 17 kapt.

4. **kapt choice is correct**: mixed Java/Kotlin source in the `app` module; kapt is the documented safe path for mixed-source Hilt modules.

5. **AC-8 verified**: deliberate breakage of `PreferencesModule.providePreferences` causes `BUILD FAILED` at `hiltJavaCompileDebug` with Dagger "cannot be provided without..." error — confirms compile-time graph verification property.

6. **Seven modules present** in `di/` package: IoDispatcher.kt, DispatcherModule.kt, PreferencesModule.kt, SchedulerModule.kt, EventModule.kt, SecretModule.kt, MailModule.kt (stub), ContactsModule.kt — all have `@Module @InstallIn(SingletonComponent::class)`.

7. **Generated components verified**: `Hilt_App.java` and `DaggerApp_HiltComponents_SingletonC.java` exist in `build/generated/hilt/component_sources/debug/`.

## Phase Completion Report
---
story_id: "U-022"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-001/U-022/review-code.md"
story_status: "done"
current_build_phase: "code-review"
blockers: 0
warnings: 2
errors: []
---
