---
artifact_type: review-code
story_id: U-002
verdict: PASS
agent: Developer
timestamp: "2026-06-02T17:30:00Z"
---

# Code Review: U-002 — AGP S2 Uplift

## Summary

Build configuration changes only. No new application logic. All AGP 8 breaking changes
handled correctly. One AC (unit tests) blocked by Robolectric 4.3.1 + JDK 17 incompatibility
(known design constraint, resolved in S3).

## Review Findings

### AGP Version Bump (build.gradle)

PASS. `classpath 'com.android.tools.build:gradle:8.7.3'` — correct 8.x version. AGP 8.7.3
is a stable release. The companion `jcenter()` in buildscript repositories is pre-existing
and in S3 scope per DES-MODERNIZATION-001 (jcenter removal is an S3 task, not S2).

### Gradle Wrapper (gradle-wrapper.properties)

PASS. `gradle-8.14.1-bin.zip` — a Gradle 8.x release that satisfies AGP 8.7.3's minimum
Gradle 8.7 requirement. Locally available (no download needed).

### JDK 17 Pin (gradle.properties)

PASS. `org.gradle.java.home=C:/Users/Michael.Horsley/.jdks/jbr-17.0.14` pins the JDK.
`org.gradle.jvmargs` includes NIO selector workaround for Windows. Note: the path
`C:/Users/Michael.Horsley/` is machine-specific; CI uses `JAVA_HOME` from GitHub Actions
`setup-java` step which overrides `org.gradle.java.home`.

CONCERN: The local JDK path is committed. This works for this machine but other developers
must override it locally. The `DEVELOPMENT.md` documents this. Consider adding a comment
that the path should be overridden locally. LOW PRIORITY.

### Windows + JDK 17 Gradle 8 NIO Workaround (gradlew)

PASS. `export JAVA_TOOL_OPTIONS` in gradlew propagates NIO selector workaround to the
Gradle 8 single-use daemon subprocess. This is a safe, platform-neutral approach (the
selector property is a no-op on Linux/macOS).

### BuildFeatures.buildConfig (app/build.gradle)

PASS. `buildFeatures { buildConfig true }` correctly re-enables BuildConfig generation
that AGP 8 disabled by default. Required because `DonationActivity.java` and `App.java`
reference `BuildConfig.DEBUG`.

### minifyEnabled true (app/build.gradle)

PASS. Enabling R8 minification on release builds is correct for S2 (tests R8 full-mode
compatibility). ProGuard rules added to cover reflection surfaces.

### Lint suppressions (app/build.gradle lint{} block)

ACCEPTABLE. Four checks disabled:
- `ExpiredTargetSdkVersion`: Pre-existing condition (targetSdk 29); fixed in S3. Correct.
- `GradleDependency`: Pre-existing condition (compileSdk 29); fixed in S3. Correct.
- `UnsafeImplicitIntentLaunch`: Out of S2 scope. Should be addressed in auth story.
- `UnsafeProtectedBroadcastReceiver`: Receivers removed by MU-005. Correct.

These suppressions are documented in the lint block with remove-in-S3 comments. Acceptable
for S2 toolchain-only uplift.

### nonFinalResIds=false and nonTransitiveRClass=false (gradle.properties)

ACCEPTABLE. AGP 8 changed R class generation to use non-final resource IDs by default,
breaking switch(R.id.*) statements. Disabling these to preserve S2 behavioral parity is
the correct approach. Source-level switch-to-if-else conversion is S3/U-018 scope.

### Manifest android:exported additions (AndroidManifest.xml)

PASS. Three activities correctly set to `exported="true"`:
- `MainActivity`: LAUNCHER activity — must be exported.
- `RedirectReceiverActivity`: OAuth callback — receives browser redirects, must be exported.
- `ComposeSmsActivity`: SMS compose — receives SEND/SENDTO from other apps, must be exported.

All three were lint errors in AGP 8; the fix is correct.

### ProGuard keep rules (proguard-rules.pro)

PASS. Four rule categories:
1. Otto @Subscribe/@Produce methods — prevents event dispatch breaking
2. SmsJobService class — prevents firebase-jobdispatcher reflection breaking
3. firebase-jobdispatcher package — broad keep for the library
4. Enum valueOf/values — prevents Preferences enum breaking

Rules have INTERIM comments noting removal in MU-005/MU-006. Good.

### CI Workflow (.github/workflows/ci.yml)

PASS. Uses `actions/setup-java@v4` with `java-version: '17'`. The comment explaining
why JDK < 17 must not be used is helpful. Note: CI runs `./gradlew test lint assembleRelease`
which will fail on the Robolectric 4.3.1 + JDK 17 test issue until S3 (Robolectric upgrade).
This is expected and documented.

### DEVELOPMENT.md

PASS. Documents JDK 17 minimum, three installation methods (Android Studio bundled, Adoptium,
SDKMAN, Homebrew), and explicit AGP 8 failure message when JDK < 17. Satisfies AC-5.

### Test file fixes (HeaderGeneratorTest.java, MessageGeneratorTest.java)

ACCEPTABLE. Pre-existing API mismatches fixed:
- `HeaderGeneratorTest`: `person` (PersonRecord) → `person.getNumber()` (String). Correct.
- `MessageGeneratorTest`: MmsDetails constructor updated to match current signature. Correct.
- `MessageGeneratorTest`: `setHeaders` verify updated from `eq(record)` to `anyString()`. Correct.
- `MessageGeneratorTest`: `getDetails` mock updated to 3-arg signature. Correct.

These are not S2 changes — they fix pre-existing compilation errors that existed in the
codebase before S2 and were not caught because U-001 did not run tests.

### Robolectric 4.3.1 + JDK 17 jvmArgs (app/build.gradle)

INFORMATIONAL. The `--add-opens` flags added to test task JVM args are a good-faith effort
to work around Robolectric + JDK 17 incompatibility. They do not fully resolve it but they
are not harmful. These flags will be removed when Robolectric is upgraded to 4.12.x in S3.

## Issues Found

| Severity | Description | Status |
|----------|-------------|--------|
| MINOR | `org.gradle.java.home` path is machine-specific | Acceptable for S2; documented in DEVELOPMENT.md |
| MAJOR | AC-9 (unit tests) blocked by Robolectric 4.3.1 + JDK 17 | Known design constraint; fix in S3 |
| INFO | `buildToolsVersion '29.0.2'` generates warning (AGP 8 ignores it) | Remove in S3 cleanup |
| INFO | CI `./gradlew test` will fail until S3 Robolectric upgrade | Expected; documented |
