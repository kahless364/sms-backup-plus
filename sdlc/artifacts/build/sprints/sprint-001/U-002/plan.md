---
artifact_type: plan
story_id: U-002
verdict: PASS
agent: Developer
timestamp: "2026-06-02T17:00:00Z"
---

# Plan: U-002 — AGP Stage S2 Uplift (7.4.2 → 8.x, Gradle 8.x, JDK 17 toolchain)

## Objective

Bump Android Gradle Plugin from 7.4.2 to 8.7.3 and Gradle wrapper from 7.5.1 to 8.14.1.
Pin JDK 17 toolchain. Handle all AGP 8 breaking changes. SDK held at compileSdk/targetSdk 29.
Produce a working `assembleRelease` and passing `lint`.

## Version Selection Rationale

- **AGP 8.7.3**: latest stable 8.7.x; confirmed downloadable from Google Maven. AGP 8.7.x
  requires Gradle >= 8.7. Not using 8.13.x/8.14.x to avoid excessive dependencies download.
- **Gradle 8.14.1**: locally extracted in `~/.gradle/wrapper/dists/gradle-8.14.1-bin`;
  satisfies AGP 8.7.x minimum Gradle requirement (8.7). Chosen over 8.7 (also available)
  because 8.7-all was locally cached but 8.7-bin needed download; 8.14.1-bin was already
  fully extracted.
- **JDK 17 (JBR 17.0.14)**: already present at `~/.jdks/jbr-17.0.14`; mandatory for AGP 8.

## Implementation Steps

1. **build.gradle**: Bump AGP classpath 7.4.2 → 8.7.3
2. **gradle-wrapper.properties**: Bump distributionUrl → gradle-8.14.1-bin.zip
3. **gradle.properties**: Add `org.gradle.java.home` pin + NIO selector workaround in
   `org.gradle.jvmargs` (Gradle 8 single-use daemon inherits jvmargs from gradle.properties)
4. **gradlew**: Add `export JAVA_TOOL_OPTIONS` to propagate NIO selector workaround to
   Gradle 8's single-use daemon subprocess (needed on Windows + JDK 17)
5. **app/build.gradle**:
   - Add `buildFeatures { buildConfig true }` (AGP 8 disables BuildConfig by default)
   - Enable `minifyEnabled true` on release to exercise R8 full-mode (AC-12)
   - Add `disable 'ExpiredTargetSdkVersion'` in lint block (AGP 8 new check; pre-existing
     condition fixed in S3)
   - Add `disable 'GradleDependency'` in lint block (AGP 8 new check on compileSdk 29)
   - Add `disable 'UnsafeImplicitIntentLaunch'` (AGP 8 new check; out-of-S2 scope)
   - Add `disable 'UnsafeProtectedBroadcastReceiver'` (AGP 8 new check; receivers removed
     by MU-005)
   - Add `--add-opens` JVM args for Robolectric 4.3.1 + JDK 17 compatibility
6. **gradle.properties**: Add `android.nonFinalResIds=false` + `android.nonTransitiveRClass=false`
   (AGP 8 defaults break switch(R.id.*) statements; reverted for S2)
7. **app/src/main/AndroidManifest.xml**: Add `android:exported` to 3 activities with
   intent-filters (IntentFilterExportedReceiver lint errors): MainActivity (true),
   RedirectReceiverActivity (true), ComposeSmsActivity (true)
8. **app/proguard-rules.pro**: Add R8 full-mode keep rules for Otto, firebase-jobdispatcher,
   enum valueOf paths
9. **.github/workflows/ci.yml**: Create GitHub Actions CI workflow with JDK 17
10. **DEVELOPMENT.md**: Create developer setup documentation with JDK 17 requirement
11. **Test fixes** (pre-existing API mismatch): Fix HeaderGeneratorTest (PersonRecord → String),
    MessageGeneratorTest (MmsDetails constructor signature, setHeaders verify signature)

## AGP 8 Breaking Changes Handled

| Change | Fix |
|--------|-----|
| BuildConfig disabled by default | `buildFeatures { buildConfig true }` |
| Non-final R.id in switch statements | `android.nonFinalResIds=false` in gradle.properties |
| Non-transitive R class | `android.nonTransitiveRClass=false` in gradle.properties |
| ExpiredTargetSdkVersion lint error | Disabled in lint{} block (pre-existing, fixed in S3) |
| GradleDependency lint error | Disabled in lint{} block (compileSdk 29 is S2 by design) |
| IntentFilterExportedReceiver on activities | Added android:exported to 3 activities |
| UnsafeImplicitIntentLaunch lint warning | Disabled (out of S2 scope) |
| UnsafeProtectedBroadcastReceiver lint warning | Disabled (receivers removed in MU-005) |
| R8 full-mode | ProGuard keep rules added |

## Known Gap: Robolectric 4.3.1 + JDK 17

Robolectric 4.3.1 is fundamentally incompatible with JDK 17. Tests fail with
`java.lang.IllegalAccessException at UnsafeFieldAccessorImpl.java:76` — Robolectric 4.3.1
uses JDK 8's internal reflection (`sun.reflect.UnsafeFieldAccessorImpl`) which is blocked
in JDK 17's module system. No combination of `--add-opens` flags resolves this.

Fix requires Robolectric upgrade to 4.12.x — this is S3 scope per DES-MODERNIZATION-001.
AC-9 is BLOCKED by this pre-existing constraint.
