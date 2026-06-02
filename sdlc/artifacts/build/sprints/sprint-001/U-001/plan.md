---
artifact_type: plan
story_id: U-001
verdict: PASS
agent: Developer
timestamp: "2026-06-02T15:45:00Z"
---

# Plan: U-001 AGP S1 — Bump AGP 4.1.3 to 7.4.x, Gradle 7.5.x, minSdk 21

## Approach

Stage S1 of the three-stage AGP migration defined in DES-MODERNIZATION-001. This stage isolates the 4.x to 7.x DSL break from the JDK/toolchain and SDK behavioral raises.

## Files to Modify

| File | Change |
|------|--------|
| `build.gradle` | Bump AGP classpath `4.1.3` to `7.4.2`; add `mavenLocal()` fallback repo |
| `gradle/wrapper/gradle-wrapper.properties` | Change `distributionUrl` from `gradle-7.2-bin.zip` to `gradle-7.5.1-bin.zip` |
| `app/build.gradle` | Add `namespace`; rename `lintOptions{}` to `lint{}`; add `baseline file('lint-baseline.xml')`; change `minSdkVersion 14` to `minSdkVersion 21` |
| `app/src/main/AndroidManifest.xml` | Remove `package` attribute; add `android:exported` to all 6 intent-filtered receivers |
| `gradle.properties` | Fix `android.jetifier.blacklist` to `android.jetifier.ignorelist`; add NIO selector workaround |
| `gradlew` | Add `DEFAULT_JVM_OPTS` with NIO selector provider workaround for JDK 21 + Windows |

## Files to Create

| File | Rationale |
|------|-----------|
| `app/lint-baseline.xml` | AC-6: capture pre-existing warnings at S1 build state; shrink-only policy through S2/S3 |

## DSL Migration Points (AC-5)

1. `lintOptions{}` renamed to `lint{}` — removed in AGP 7.0
2. `namespace 'com.zegoggles.smssync'` added to `android{}` block — required in AGP 7.3+
3. `package` attribute removed from `AndroidManifest.xml` root `<manifest>` element
4. `android:exported` added to all 6 intent-filtered receivers (AC-8 pull-forward):
   - `SmsBroadcastReceiver` = `false`
   - `BootReceiver` = `false`
   - `PackageReplacedReceiver` = `false`
   - `BackupBroadcastReceiver` = `true` (public Tasker/automation contract)
   - `.compat.SmsReceiver` = `true` (system SMS_DELIVER, must be reachable by platform)
   - `.compat.MmsReceiver` = `true` (system WAP_PUSH_DELIVER, must be reachable by platform)
5. `android.jetifier.blacklist` renamed to `android.jetifier.ignorelist` (AGP 7.0 renamed property)

## Environment Notes

- JDK on PATH: Java 22 (incompatible with AGP 7.4/Gradle 7.5 script compilation)
- JDK 17 found at: `~/.jdks/jbr-17.0.14` (JBR from Android Studio) — used for build
- NIO Selector workaround required: Java 17/21 on Windows uses WEPollSelectorProvider which fails on Unix domain socket path; workaround via `DEFAULT_JVM_OPTS` in `gradlew` and `org.gradle.jvmargs`

## Dependency Resolution

`firebase-jobdispatcher:0.8.6` unavailable from scijava.org (artifact no longer hosted there). Built from source (tag v0.8.6 at googlearchive/firebase-jobdispatcher-android) and published to `~/.m2/repository` as local Maven artifact. `mavenLocal()` added to `allprojects` repositories as first-search fallback in `build.gradle`.
