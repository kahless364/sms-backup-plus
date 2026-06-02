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
| Language | Java (source/target Java 8) |
| Build system | Gradle (wrapper: `./gradlew`) |
| Android Gradle Plugin | `com.android.application` |
| compileSdk / targetSdk | 29 |
| minSdk | 14 (tested Android 4.x ICS – 10.x Q) |
| Build tools | 29.0.2 |
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
| Install to device | `adb install app/build/outputs/apk/app-debug.apk` |
| Run unit tests | `./gradlew test` |
| Lint | `./gradlew lint` (lint warnings are treated as errors) |

> Note: `JavaCompile` tasks use `-Werror -Xlint:unchecked -Xlint:deprecation` for
> non-test code, so compiler warnings fail the build.

## Build Signing

Release signing reads from a `keystore.properties` file at the repo root (not
committed). If absent, the release build is unsigned.

## Notable Constraints

- As of June 2019 Google API policy changes, XOAuth2 can no longer be used to save
  messages into Gmail; an IMAP "app password" is required. XOAuth2 is still used for
  contact-name matching and calendar writes.
- MMS restore is not supported (backup only).
