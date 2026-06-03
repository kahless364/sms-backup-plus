# SMS Backup+ — Development Setup

## Prerequisites

### JDK 17 (Required)

**Minimum JDK version: 17**

Android Gradle Plugin (AGP) 8.x, used in this project, **requires JDK 17 at Gradle
configuration time**. If the Gradle daemon starts with JDK 11 or earlier, the build
will fail immediately with:

```
Android Gradle plugin requires Java 17 to run. You are currently using Java <version>.
```

This failure occurs before any task runs, regardless of `compileOptions.sourceCompatibility`.

#### Installing JDK 17

Choose one of the following options:

1. **Android Studio bundled JDK** (recommended for Android developers)
   Android Studio Hedgehog (2023.1.1) and later ship with JDK 17.
   Set `org.gradle.java.home` in `gradle.properties` to point to the Studio JDK:
   ```
   # macOS example
   org.gradle.java.home=/Applications/Android Studio.app/Contents/jbr/Contents/Home
   # Windows example
   org.gradle.java.home=C:/Program Files/Android/Android Studio/jbr
   ```

2. **Adoptium OpenJDK 17** (for CI or standalone installs)
   Download from https://adoptium.net/temurin/releases/?version=17

3. **SDKMAN** (Linux/macOS)
   ```bash
   sdk install java 17.0.9-tem
   sdk use java 17.0.9-tem
   ```

4. **Homebrew** (macOS)
   ```bash
   brew install openjdk@17
   ```

#### Pinning the JDK in gradle.properties

The project pins JDK 17 via `org.gradle.java.home` in `gradle.properties`.
This path is set to the JBR 17 installation used during development. Override
it locally if your JDK 17 is at a different path by adding a line to your
local `gradle.properties` (do not commit local overrides).

### Android SDK

- **compileSdkVersion**: 29 (held at S2; raised to 35 at S3)
- **targetSdkVersion**: 29
- **minSdkVersion**: 21

Install Android SDK platform 29 via Android Studio SDK Manager or `sdkmanager`:
```bash
sdkmanager "platforms;android-29" "build-tools;29.0.2"
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build (unsigned unless keystore.properties is present)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run lint (with baseline — new violations are errors)
./gradlew lint
```

## AGP Upgrade History

| Stage | AGP | Gradle | JDK | SDK |
|-------|-----|--------|-----|-----|
| S1 (U-001) | 7.4.2 | 7.5.1 | 11+ | compileSdk 29 |
| **S2 (U-002)** | **8.7.3** | **8.14.1** | **17 (required)** | **compileSdk 29** |
| S3 (U-003, planned) | 8.7.3 | 8.14.1 | 17 | compileSdk 35 |

AGP 8 (S2 and beyond) will **not configure** if Gradle is started with JDK < 17.
