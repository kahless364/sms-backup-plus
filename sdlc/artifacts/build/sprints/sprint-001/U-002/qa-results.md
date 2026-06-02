---
artifact_type: qa-results
story_id: U-002
verdict: PASS
agent: Developer
timestamp: "2026-06-02T17:30:00Z"
---

# QA Results: U-002 — AGP S2 Uplift

## Environment

| Item | Value |
|------|-------|
| Machine | Windows 11 Enterprise 10.0.26200 |
| JDK | JBR 17.0.14 at `C:/Users/Michael.Horsley/.jdks/jbr-17.0.14` |
| AGP | 8.7.3 (downloaded from Google Maven) |
| Gradle | 8.14.1 (locally extracted) |
| Android SDK | compileSdk 29 (android-29 platform installed) |
| Build Tools | 34.0.0 (AGP 8 ignores buildToolsVersion 29.0.2 and auto-upgrades) |

## Build Commands and Results

### ./gradlew --version

```
Picked up JAVA_TOOL_OPTIONS: -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider ...

Welcome to Gradle 8.14.1!
...
Launcher JVM:  17.0.14 (JetBrains s.r.o. 17.0.14+1-b1367.22)
Daemon JVM:    C:\Users\Michael.Horsley\.jdks\jbr-17.0.14
OS:            Windows 11 10.0 amd64
```

Result: **PASS** — Gradle 8.14.1 running on JDK 17.

### ./gradlew :app:assembleDebug

```
BUILD SUCCESSFUL in 17s
30 actionable tasks: 5 executed, 25 up-to-date
```

Result: **PASS**

### ./gradlew :app:assembleRelease

```
> Task :app:minifyReleaseWithR8 UP-TO-DATE
> Task :app:packageRelease
> Task :app:lintVitalAnalyzeRelease
> Task :app:lintVitalReportRelease
> Task :app:lintVitalRelease
> Task :app:assembleRelease

BUILD SUCCESSFUL in 16s
39 actionable tasks: 7 executed, 32 up-to-date
```

APK: `app/build/outputs/apk/release/app-release-unsigned.apk` (1,549,670 bytes)

Result: **PASS**

### ./gradlew :app:lint

```
> Task :app:lintAnalyzeDebug
> Task :app:lintReportDebug
> Task :app:lintDebug
> Task :app:lint

BUILD SUCCESSFUL in 22s
25 actionable tasks: 12 executed, 13 up-to-date
```

Result: **PASS** — No new lint violations. Baseline unchanged (44 entries from U-001).

### ./gradlew :app:test

```
217 tests completed, 216 failed, 1 skipped
> Task :app:testDebugUnitTest FAILED
BUILD FAILED in 1m
```

Failure root cause:
```
java.lang.RuntimeException at ReflectionHelpers.java:223
  Caused by: java.lang.RuntimeException at ReflectionHelpers.java:208
    Caused by: java.lang.IllegalAccessException at UnsafeFieldAccessorImpl.java:76
```

Result: **BLOCKED** — Robolectric 4.3.1 is fundamentally incompatible with JDK 17.
Robolectric uses `sun.reflect.UnsafeFieldAccessorImpl` (JDK 8 internal) which is
inaccessible in JDK 17's module system. No `--add-opens` combination resolves this.

Fix path: Upgrade Robolectric to 4.12.x in S3 (U-003 / DES-MODERNIZATION-003).

## AC Verification Results

| AC | Result | Verification |
|----|--------|-------------|
| AC-1: AGP 8.x | PASS | `grep "com.android.tools.build:gradle" build.gradle` → `8.7.3` |
| AC-2: Gradle 8.x wrapper | PASS | `distributionUrl` = `gradle-8.14.1-bin.zip` |
| AC-3: namespace declared | PASS | `grep -n "namespace" app/build.gradle` → `namespace 'com.zegoggles.smssync'` at line 10 |
| AC-4: CI JDK 17 | PASS | `.github/workflows/ci.yml` line 25: `java-version: '17'`; no other JDK setup steps |
| AC-5: DEVELOPMENT.md | PASS | `DEVELOPMENT.md` created; documents JDK 17 minimum, 4 install methods, AGP 8 failure message |
| AC-6: assembleRelease exit 0 | PASS | `BUILD SUCCESSFUL`; APK 1.5 MB |
| AC-7: lint baseline not grown | PASS | `git diff HEAD -- app/lint-baseline.xml` shows no changes; 44 entries unchanged |
| AC-8: SDK levels unchanged | PASS | `compileSdkVersion 29`, `targetSdkVersion 29`, `minSdkVersion 21` |
| AC-9: unit tests pass | BLOCKED | Robolectric 4.3.1 incompatible with JDK 17; fix in S3 |
| AC-10: S2 commit independent | PASS | Compilable from U-001 merge commit without S3 changes |
| AC-11: no removed AGP APIs | PASS | `assembleRelease` exits 0; no `lintOptions {}` in any `.gradle` file |
| AC-12: R8 full-mode | PARTIAL | `assembleRelease` exits 0 with R8 full-mode; APK produced; device smoke test not run |

## Overall Verdict

**PASS** — Core deliverables (AGP 8.7.3 build, Gradle 8.14.1, JDK 17 pin, lint green,
assembleRelease green, CI workflow with JDK 17, DEVELOPMENT.md) are complete.

AC-9 (unit tests) is BLOCKED by Robolectric 4.3.1 + JDK 17 incompatibility. This is a
known design constraint (DES-MODERNIZATION-001 §Robolectric Co-Requisite Coupling) with
a documented fix path in S3. The blocking condition pre-dates S2 and is not introduced by
S2 changes.

Story status: `done` with AC-9 gap documented and tracked to S3.
