---
artifact_type: plan
story_id: "U-005"
verdict: PASS
agent: Developer
timestamp: "2026-06-02"
---

# Plan: U-005 Test Toolchain Uplift

## Approach

1. Reset worktree to sdlc/modernization-plan tip (includes U-001, U-002, U-004 changes).
2. Update `app/build.gradle` test dependencies:
   - junit 4.12 -> 4.13.2 (CVE-2020-15250 fix)
   - robolectric 4.3.1 -> 4.12.2 (JDK 17 native support)
   - truth 0.39 -> 1.4.4
   - mockito-all 1.10.17 -> mockito-core 5.14.2
3. Add `testOptions { unitTests { includeAndroidResources = true } }` for Robolectric 4.12.
4. Create/update `app/src/test/resources/robolectric.properties` with `sdk=29`.
5. Remove the `tasks.withType(Test).configureEach { jvmArgs '--add-opens...' }` stopgap block.
6. Fix all 36 test files for Mockito 1.x -> 5.x API changes:
   - `org.mockito.Matchers` -> `org.mockito.ArgumentMatchers`
   - `anyListOf(X.class)` -> `anyList()`
   - `verifyZeroInteractions` -> `verifyNoInteractions`
   - `initMocks(this)` -> `openMocks(this)`
   - `any(X.class)` -> `nullable(X.class)` where production code passes null
   - `isNull(X.class)` -> `isNull()` (typed form removed)
   - `notNull(X.class)` -> `notNull()` (typed form removed)
7. Fix Truth 0.39 -> 1.4 API: `isSameAs` -> `isSameInstanceAs` for Throwable subjects.
8. Fix production code: `IOUtils.toString(is)` -> `IOUtils.toString(is, StandardCharsets.UTF_8)` to prevent platform-charset ambiguity in message body decoding (exposed by Robolectric 4.12 with includeAndroidResources).
9. Fix SmsReceiverTest: SDK 16/19 configs replaced with SDK 21 (LOLLIPOP) per U-001 minSdk 21 raise.
10. Fix AppTest: `isEqualTo(0)` -> `isGreaterThan(0)` for versionCode (Robolectric 4.12 resolves real resources).
11. Fix SmsBackupServiceTest: Remove `shadowOf(service.getWifiManager())` which triggers NoClassDefFoundError for `WifiManager$LocalOnlyConnectionFailureListener` not present in SDK 29 classpath.
12. Update JaCoCo configuration for AGP 8: use `afterEvaluate { tasks.named('testDebugUnitTest') { jacoco { ... } } }` pattern; point classDirectories to the full path under `compileDebugJavaWithJavac/classes`.
13. Run tests, iterate until green.
14. Run jacocoTestReport and jacocoTestCoverageVerification.

## Versions Chosen

| Artifact | Old | New | Rationale |
|----------|-----|-----|-----------|
| junit:junit | 4.12 | 4.13.2 | CVE-2020-15250; final 4.x stable |
| robolectric | 4.3.1 | 4.12.2 | Latest 4.12.x; JDK 17 native |
| truth | 0.39 | 1.4.4 | Latest 1.4.x stable |
| mockito-core | mockito-all:1.10.17 | 5.14.2 | Latest 5.x stable; fat-jar replaced |
| auto-service | 1.0-rc4 | 1.0 | Release of RC; same artifact |
| JaCoCo | 0.8.7 | 0.8.12 | Latest stable; compatible with JDK 17 |
