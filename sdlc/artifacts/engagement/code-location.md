# Code Location

## Repository

| Field | Value |
|-------|-------|
| Type | Existing codebase (single-repo) |
| Path | `.` (this repository — `C:/Code/Android/sms-backup-plus`) |
| Application ID | `com.zegoggles.smssync` |
| Upstream | https://github.com/jberkel/sms-backup-plus |
| License | Apache 2.0 (see `COPYING` / `NOTICE`) |

## What It Is

SMS Backup+ is an Android app that backs up SMS, MMS, and call logs to Gmail/IMAP
over the network, with restore and Google Calendar integration. This is a fork of
the defunct Google Code "SMS Backup" project.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Platform | Android (native) |
| Language | Java + Kotlin; app bytecode Java 17 (class-file major 61); vendored k-9 module (`k9mail-vendored`) Java 8 (major 52) |
| Build system | Gradle 9.5.1 (wrapper: `./gradlew`), running on **JDK 25** (see Build Environment) |
| Android Gradle Plugin | 9.0.1 (`com.android.application`, built-in Kotlin); Kotlin 2.3.21, KSP 2.3.6, Hilt 2.59.2 |
| compileSdk / targetSdk | 35 |
| minSdk | 21 (Android 5.0) |
| Build tools | 36.0.0+ (AGP 9.0 minimum) |
| ABIs | `armeabi-v7a`, `arm64-v8a` |
| Version | 1.6.0-BETA2 (versionCode 1602) |

### Key Dependencies

- `com.squareup:otto` — event bus
- `com.github.jberkel.k-9:k9mail-library` — IMAP/email handling
- `com.android.billingclient:billing` — in-app donations
- `com.firebase:firebase-jobdispatcher` — background job scheduling
- `androidx.annotation`, `androidx.preference`, `androidx.core:core-role`

### Test Stack

- JUnit 4
- Robolectric (Android unit testing)
- Google Truth (assertions)
- Mockito

## Source Layout

```
.
├── app/                      # Main Android application module
│   ├── build.gradle          # App module build config
│   └── src/
│       ├── main/
│       │   ├── java/com/...   # Application source (com.zegoggles.smssync)
│       │   ├── res/           # Resources + many localized values-* dirs
│       │   └── assets/
│       └── test/             # Robolectric / JUnit tests (where present)
├── metadata/                 # Store listing / Fastlane metadata module
├── gradle/                   # Gradle wrapper files
├── build.gradle              # Root build config
├── settings.gradle           # Module inclusion
├── gradle.properties
├── local.properties          # SDK location (not committed)
├── BUGS.md                   # Known issues
├── CHANGES                    # Changelog
└── README.md
```

## Build / Test / Run

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew assembleDebug` |
| Install to device | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| Run unit tests | `./gradlew :app:testDebugUnitTest` (CI runs `./gradlew test lint jacocoTestReport assembleRelease`) |
| Lint | `./gradlew lint` (lint warnings are treated as errors) |
| The three gates | `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` (JaCoCo enforces per-package LINE floors) |

> Note: `:app` `JavaCompile` tasks (non-test) use `-Werror -Xlint:unchecked`, so compiler
> warnings fail the build. The `-Werror` does not reach `:k9mail-vendored`.
> Known baseline state (U-060): `:app:lintDebug` fails both before and after the JDK 25 upgrade.

## Build Environment (JDK, Gradle properties)

1. **JDK:** Gradle, javac, kotlinc and the unit tests run on **JDK 25**, declared in the repository by
   `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=25`, no download URLs). App bytecode stays at
   Java 17; the vendored module at Java 8.
2. **Precedence (observed, U-060 matrix M2):** the daemon JVM criteria file takes precedence over
   `org.gradle.java.home` (even a user-global one in `~/.gradle/gradle.properties`), which in turn
   overrides `JAVA_HOME`. With the criteria file present a `JAVA_HOME`/`org.gradle.java.home` pointing at
   JDK 17 does not change the daemon JVM (it stays JDK 25). For `org.gradle.java.home` itself:
   command line > `~/.gradle/gradle.properties` > project `gradle.properties`. Without the criteria file a
   user-global `org.gradle.java.home` overrides `JAVA_HOME`.
3. **`org.gradle.java.installations.auto-detect=false`:** Gradle then only considers JDKs listed in
   `org.gradle.java.installations.paths` (and the launcher JVM). Observed (M3): listing a JDK 25 there works.
   Observed (M4): with no paths and a JDK 25 launcher (`java` on PATH) the build still works because the launcher
   JVM satisfies the criteria; if the launcher is not JDK 25, a JDK 25 must be listed in `paths`.
4. **jvmargs:** a user-global `org.gradle.jvmargs` replaces the repository's `org.gradle.jvmargs`.
   The repository's Windows NIO flags (`SelectorProvider`, `jdk.net.unixdomain.tmpdir`) are also exported
   by `gradlew` via `JAVA_TOOL_OPTIONS` (needed on Windows: without it JDK 25 fails with "Unable to
   establish loopback connection"). **Re-apply that block in `gradlew` after any `./gradlew wrapper` regeneration.**
5. **Verify:** `./gradlew --version` - the "Daemon JVM" line must cite `gradle/gradle-daemon-jvm.properties`.
6. **Baseline for baseline-vs-after comparisons on JDK 17:** `-Dorg.gradle.java.home=<jdk17>` on the command line
   (highest precedence) is for old pre-U-060 commits only.

## Build Signing

Release signing reads from a `keystore.properties` file at the repo root (not
committed). If absent, the release build is unsigned.

## Notable Constraints

- As of June 2019 Google API policy changes, XOAuth2 can no longer be used to save
  messages into Gmail; an IMAP "app password" is required. XOAuth2 is still used for
  contact-name matching and calendar writes.
- MMS restore is not supported (backup only).
