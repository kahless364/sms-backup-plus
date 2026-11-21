# Latest stable versions and platform status, resolved 2026-11-21

Every version below was read on 2026-11-21 from the URL cited beside it. "Published" dates are the
`Last-Modified` header of the artifact's `.pom` on Maven Central or Google Maven unless a release
page is named. "Stable" excludes alpha, beta, RC, milestone, dev, EAP and snapshot versions.
Project locations are repository-relative (`file:line`).

Abbreviations for sources:

- MC = `https://repo1.maven.org/maven2/<group path>/<artifact>/maven-metadata.xml`
- GM = `https://dl.google.com/dl/android/maven2/<group path>/<artifact>/maven-metadata.xml`

---

## 1. What changed since the 2026-11-12 snapshot tables (REQ-MODERNIZATION-015, -016)

| Component | Snapshot 2026-11-12 | Today 2026-11-21 | Published | Note |
|---|---|---|---|---|
| Gradle | 9.9.1 | **9.9.2** | 2026-11-19 | Patch release; fixes four security vulnerabilities, one of them "Transitive snapshot dependencies skip dependency verification" (https://docs.gradle.org/9.9.2/release-notes.html). |
| Android Gradle Plugin | 9.5.1 | **9.5.2** | 2026-11-20 | On Google Maven; the AGP 9.5 release-notes page (last updated 2026-11-05) lists only up to 9.5.1, so no published notes for 9.5.2 yet. |
| KSP | 2.3.13 | **2.3.14** | 2026-11-13 | The 2.3.13 release notes now carry "Defect in release" (regression, issue 3286, concurrent KSP executions); 2.3.14 fixes it. |
| Robolectric | 4.17 | **4.18.1** | 4.18: 2026-11-13; 4.18.1: 2026-11-18 | 4.18 removes SDK 23: supported range is now API 24 to 37. Removes `shadows-httpclient` and `@TextLayoutMode`. |
| androidx.lifecycle | 2.11.0 | **2.11.1** | 2026-11-18 (release page), pom 2026-11-19 | Bug-fix release (`LifecycleRegistry.currentStateFlow`). |

Unchanged since the snapshot (re-verified today): Kotlin 2.4.21, Hilt 2.60.1, androidx.hilt 1.4.0,
Mockito 5.24.0, GreenMail 2.1.15, JaCoCo 0.8.16, auto-service 1.1.1, WorkManager 2.12.0,
kotlinx-coroutines 1.11.0, androidx.preference 1.2.1, androidx.annotation 1.11.0,
androidx.core:core-role 1.1.0, androidx.security:security-crypto 1.1.0, JUnit 4.13.2, jutf7 1.0.0.

Pre-releases now visible (not eligible): Kotlin 2.5.0-RC (2026-11-11), AGP 9.6.0-alpha05,
Lifecycle 2.12.0-alpha06, androidx.core 1.20.0-alpha01, AndroidX Test 1.8.0-alpha04,
JaCoCo 0.8.17 snapshot (2026-11-17).

Findings that correct or sharpen statements in the requirement files:

1. **Robolectric floor is now 24, not 23.** REQ-015 Context says "Robolectric 4.16 or later require
   minSdk 23". True for 4.16 and 4.17; 4.18 and 4.18.1 cannot emulate any SDK below 24
   (release notes; compatibility table). This agrees with the minSdk 24 decision.
2. **WorkManager 2.12.0 itself requires minSdk 24** ("The minSdk has been updated from API 23 to API 24",
   release page; AAR manifest `minSdkVersion="24"`). REQ-016 can only reach 2.12.0 after minSdk 24.
3. **Hilt 2.60 "minSDK is now 23"** is stated in the Dagger 2.60 release notes, but the published
   `hilt-android-2.60.1.aar` manifest still declares `minSdkVersion="14"`. The manifest merger will
   not enforce 23 from that AAR; the statement is a support statement.
4. **jzlib: Maven Central's newest stable is 1.1.3 (2013-10-04), not 1.0.7.** The epic says each
   retained mail library "resolves to its latest stable version at planning time" and records 1.0.7 as
   what Thunderbird declares. Thunderbird `main` still pins 1.0.7. The two statements conflict for jzlib.
5. **Truth 1.4.5** (2025-09-10) is newer than the project's 1.4.4; Truth is not in the REQ-015 table.
6. **Thunderbird**: the 2026-11-17 finding is confirmed for `main` (Gradle 9.7.0, AGP 9.5.1, JDK 21,
   compileSdk 37, minSdk 24). The latest release tag (25.0) differs: AGP 9.2.1, Kotlin 2.4.20, minSdk 23.
7. **androidx.security:security-crypto 1.1.0** is the latest stable and its release notes say
   "Deprecated all APIs in favour of existing platform APIs and direct use of Android Keystore"
   (maintenance-status finding asked for by REQ-016 AC-3).

---

## 2. Version tables

### 2.1 Build toolchain

| Coordinate / tool | In project today (file:line) | Latest stable | Published | Source URL | Notes |
|---|---|---|---|---|---|
| Gradle (wrapper) | 9.5.1 (`gradle/wrapper/gradle-wrapper.properties:4`) | 9.9.2 | 2026-11-19 | https://services.gradle.org/versions/current ; https://gradle.org/releases/ ; https://docs.gradle.org/9.9.2/release-notes.html | No active RC. Previous: 9.9.1 (2026-11-10), 9.9.0 (2026-11-06). Gradle 8 maintenance line: 8.14.8. |
| `com.android.tools.build:gradle` (AGP) | 9.0.1 (`build.gradle:10`) | 9.5.2 | 2026-11-20 | GM `com/android/tools/build/gradle` ; https://developer.android.com/build/releases/gradle-plugin (redirects to `agp-9-5-0-release-notes`) | 9.5.0: 2026-10-28; 9.5.1: 2026-11-05. Newest pre-release 9.6.0-alpha05. Release notes for 9.5.2 not yet on the page. |
| `org.jetbrains.kotlin:kotlin-gradle-plugin` | 2.3.21 (`build.gradle:14`) | 2.4.21 | 2026-10-08 | MC `org/jetbrains/kotlin/kotlin-gradle-plugin` ; https://kotlinlang.org/docs/releases.html ; https://github.com/JetBrains/kotlin/releases | Pre-release line: 2.5.0-RC (2026-11-11), 2.5.0-Beta2. |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.3.21 (`app/build.gradle:117`) | 2.4.21 | 2026-10-08 | MC `org/jetbrains/kotlin/kotlin-stdlib` | Must match KGP. |
| `com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin` | 2.3.6 (`build.gradle:17`) | 2.3.14 | 2026-11-13 | MC `com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin` ; https://github.com/google/ksp/releases/tag/2.3.14 | 2.3.13 (2026-10-30) has a known regression. No 2.4.x KSP line exists. |
| `com.google.dagger:hilt-android-gradle-plugin` | 2.59.2 (`build.gradle:21`) | 2.60.1 | 2026-07-06 | MC `com/google/dagger/hilt-android-gradle-plugin` ; https://github.com/google/dagger/releases/tag/dagger-2.60.1 | 2.60: 2026-06-26. |
| `com.google.dagger:hilt-android` | 2.59.2 (`app/build.gradle:182`) | 2.60.1 | 2026-07-06 | MC `com/google/dagger/hilt-android` | AAR manifest minSdk 14; release notes say minSDK 23. |
| `com.google.dagger:hilt-compiler` (ksp, kspTest) | 2.59.2 (`app/build.gradle:183`, `:187`) | 2.60.1 | 2026-07-06 | MC `com/google/dagger/hilt-compiler` | |
| `androidx.hilt:hilt-work` | 1.2.0 (`app/build.gradle:184`) | 1.4.0 | 2026-07-01 | GM `androidx/hilt/hilt-work` ; https://developer.android.com/jetpack/androidx/releases/hilt | AAR: minSdk 23, minCompileSdk 34, min AGP 8.1.1. Built against hilt-android 2.59. |
| `androidx.hilt:hilt-compiler` (ksp) | 1.2.0 (`app/build.gradle:185`) | 1.4.0 | 2026-07-01 | GM `androidx/hilt/hilt-compiler` | |
| JaCoCo (`org.jacoco:org.jacoco.core` / `.agent`) | 0.8.14 (`app/build.gradle:212`) | 0.8.16 | 2026-10-24 (change history), pom 2026-10-26 | MC `org/jacoco/org.jacoco.core` ; https://www.jacoco.org/jacoco/trunk/doc/changes.html | Officially supports Java 27; ASM 9.11.1. |
| Android SDK Build Tools | not named in Gradle files (AGP default); CI installs `build-tools;36.0.0` (`.github/workflows/ci.yml:30`) | 37.0.0 (also stable: 36.1.0, 36.0.0) | date not verified | https://dl.google.com/android/repository/repository2-3.xml | AGP 9.5 minimum and default is 36.0.0. The Build Tools release-notes page stops at 34.0.0. |
| JDK (Gradle daemon / build) | 25 (`gradle/gradle-daemon-jvm.properties:2`); bytecode 17 for `:app` (`app/build.gradle:90-91`, `:107`), 8 for `:k9mail-vendored` (`k9mail-vendored/build.gradle:76-77`) | JDK 25 is the most recent LTS; newest 25 GA build 25.0.5.1+2. Newest feature release is 27 | not recorded | https://api.adoptium.net/v3/info/available_releases ; https://api.adoptium.net/v3/info/release_names?release_type=ga&version=%5B25,26) | `most_recent_lts: 25`, `most_recent_feature_release: 27`, `tip_version: 28`. |
| Version catalog | `gradle/libs.versions.toml` does not exist | n/a | n/a | n/a | Versions are inline in the three build files. `settings.gradle:1` includes `:app`, `:metadata`, `:k9mail-vendored`. |

### 2.2 Test tooling

| Coordinate | In project today (file:line) | Latest stable | Published | Source URL | Notes |
|---|---|---|---|---|---|
| `junit:junit` | 4.13.2 (`app/build.gradle:159`) | 4.13.2 | 2021-02-13 | MC `junit/junit` ; https://github.com/junit-team/junit4/releases | Current. |
| `org.robolectric:robolectric` | 4.15.1 (`app/build.gradle:160`); `sdk=29` (`app/src/test/resources/robolectric.properties:8`) | 4.18.1 | 2026-11-18 | MC `org/robolectric/robolectric` ; https://github.com/robolectric/robolectric/releases/tag/robolectric-4.18.1 ; https://robolectric.org/compatibility_table/ | API 24 to 37. SDK 36 and 37 need Java 21 or newer. ASM 9.11.1. |
| `org.mockito:mockito-core` | 5.18.0 (`app/build.gradle:163`) | 5.24.0 | 2026-09-23 | MC `org/mockito/mockito-core` ; https://github.com/mockito/mockito/releases | Depends on ByteBuddy 1.17.7. |
| `org.mockito.kotlin:mockito-kotlin` | not used | 6.4.0 | 2026-09-23 | MC `org/mockito/kotlin/mockito-kotlin` ; https://github.com/mockito/mockito-kotlin/releases | For information only. |
| `com.google.truth:truth` | 1.4.4 (`app/build.gradle:161`) | 1.4.5 | 2025-09-10 | MC `com/google/truth/truth` ; https://github.com/google/truth/releases | Not in the REQ-015 table. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.7.3 (`app/build.gradle:150`) | 1.11.0 | 2026-05-07 | MC `org/jetbrains/kotlinx/kotlinx-coroutines-test` ; https://github.com/Kotlin/kotlinx.coroutines/releases | Built with Kotlin 2.2.20. |
| `androidx.work:work-testing` | 2.9.1 (`app/build.gradle:156`) | 2.12.0 | 2026-09-23 | GM `androidx/work/work-testing` ; https://developer.android.com/jetpack/androidx/releases/work | AAR: minSdk 24, minCompileSdk 35, min AGP 8.6.0. |
| `com.google.dagger:hilt-android-testing` | 2.59.2 (`app/build.gradle:186`) | 2.60.1 | 2026-07-06 | MC `com/google/dagger/hilt-android-testing` | |
| `com.icegreen:greenmail-junit4` | 2.1.3 (`app/build.gradle:173`) | 2.1.15 | 2026-10-25 | MC `com/icegreen/greenmail-junit4` ; https://github.com/greenmail-mail-test/greenmail/releases | |
| `com.google.auto.service:auto-service` (testAnnotationProcessor) | 1.0 (`app/build.gradle:166`) | 1.1.1 | 2023-06-12 | MC `com/google/auto/service/auto-service` | |
| AndroidX Test (`androidx.test:runner`, `core`, `rules`, `ext:junit`, `espresso-core`) | no dependency declared; only `testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner'` (`app/build.gradle:28`); there is no `app/src/androidTest` | runner 1.7.0, core 1.7.0, rules 1.7.0, ext.junit 1.3.0, espresso 3.7.0 | 2025-07-30 | GM `androidx/test/runner` etc. ; https://developer.android.com/jetpack/androidx/releases/test | Alphas: 1.8.0-alpha04 / 1.4.0-alpha04 / 3.8.0-alpha04. Robolectric 4.18.1 itself pulls `androidx.test:monitor:1.8.0`. |

### 2.3 Runtime libraries

| Coordinate | In project today (file:line) | Latest stable | Published | Source URL | Notes (AAR manifest minSdk / `minCompileSdk` / min AGP read from the published AAR) |
|---|---|---|---|---|---|
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.3.21 (`app/build.gradle:117`) | 2.4.21 | 2026-10-08 | MC `org/jetbrains/kotlin/kotlin-stdlib` | |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.7.3 (`app/build.gradle:118`) | 1.11.0 | 2026-05-07 | MC `org/jetbrains/kotlinx/kotlinx-coroutines-core` | Compiled against kotlin-stdlib 2.2.20; usable with Kotlin 2.4.21 (newer stdlib). |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.7.3 (`app/build.gradle:119`) | 1.11.0 | 2026-05-07 | MC `org/jetbrains/kotlinx/kotlinx-coroutines-android` | |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.6.2 (`app/build.gradle:122`) | 2.11.1 | 2026-11-18 | GM `androidx/lifecycle/lifecycle-runtime-ktx` ; https://developer.android.com/jetpack/androidx/releases/lifecycle | minSdk 23 / minCompileSdk 34 / AGP 8.1.1. Alpha: 2.12.0-alpha06. |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.6.2 (`app/build.gradle:123`) | 2.11.1 | 2026-11-18 | GM `androidx/lifecycle/lifecycle-viewmodel-ktx` | minSdk 23 / minCompileSdk 34 / AGP 8.1.1. |
| `androidx.annotation:annotation` | 1.1.0 (`app/build.gradle:134`); 1.7.1 (`k9mail-vendored/build.gradle:114`) | 1.11.0 | 2026-09-23 | GM `androidx/annotation/annotation` ; https://developer.android.com/jetpack/androidx/releases/annotation | JVM jar; no SDK floor. |
| `androidx.preference:preference` | 1.1.0 (`app/build.gradle:135`) | 1.2.1 | 2023-07-26 | GM `androidx/preference/preference` ; https://developer.android.com/jetpack/androidx/releases/preference | minSdk 14 / minCompileSdk 31. |
| `androidx.core:core-role` | 1.0.0-beta01 (`app/build.gradle:136`) | 1.1.0 | 2025-03-12 | GM `androidx/core/core-role` ; https://developer.android.com/jetpack/androidx/releases/core | minSdk 14 / minCompileSdk 31. No dependencies. |
| `androidx.security:security-crypto` | 1.1.0 (`app/build.gradle:147`) | 1.1.0 | 2025-07-30 | GM `androidx/security/security-crypto` ; https://developer.android.com/jetpack/androidx/releases/security | minSdk 21 / minCompileSdk 34 / AGP 8.1.1. All APIs deprecated in 1.1.0. Depends on tink-android 1.8.0 (Tink latest stable is 1.24.0, 2026-10-16). |
| `androidx.work:work-runtime-ktx` | 2.9.1 (`app/build.gradle:155`) | 2.12.0 | 2026-09-23 | GM `androidx/work/work-runtime-ktx` ; https://developer.android.com/jetpack/androidx/releases/work | minSdk 24 / minCompileSdk 35 / AGP 8.6.0. Pulls core 1.12.0, room-runtime 2.7.0, lifecycle 2.6.2, coroutines 1.9.0, startup 1.1.1. |
| `com.google.dagger:hilt-android` | 2.59.2 (`app/build.gradle:182`) | 2.60.1 | 2026-07-06 | MC `com/google/dagger/hilt-android` | Manifest minSdk 14; no `aar-metadata.properties`. POM: kotlin-stdlib 2.3.21, activity 1.5.1, fragment 1.5.1, lifecycle 2.5.1. |
| `androidx.hilt:hilt-work` | 1.2.0 (`app/build.gradle:184`) | 1.4.0 | 2026-07-01 | GM `androidx/hilt/hilt-work` | minSdk 23 / minCompileSdk 34 / AGP 8.1.1. |
| `:k9mail-vendored` (in-tree module) | compileSdk 35, minSdk 21 (`k9mail-vendored/build.gradle:52`, `:54`) | n/a | n/a | n/a | To be retired by REQ-019. |
| App platform levels | compileSdk 35, minSdk 21, targetSdk 35 (`app/build.gradle:21`, `:24`, `:25`) | see section 4 | | | |

Transitive AndroidX libraries (not declared by the app; for information, because Thunderbird modules or
future bumps may pull them): `androidx.core:core` 1.19.1 (2026-09-23; minSdk 23, **minCompileSdk 37**,
min AGP 9.1.0), `androidx.activity:activity` 1.13.0 (2026-03-11; minSdk 23, minCompileSdk 36, min AGP
8.9.1), `androidx.fragment:fragment` 1.9.1 (2026-09-23; minSdk 23, minCompileSdk 34),
`androidx.appcompat:appcompat` 1.8.0 (2026-08-12; minSdk 23, minCompileSdk 34),
`androidx.room:room-runtime` 2.8.5 (2026-09-09; minSdk 23, minCompileSdk 34),
`androidx.startup:startup-runtime` 1.2.0 (2024-09-18; minSdk 21, minCompileSdk 34). Source: GM
metadata for each, and the AAR at `https://dl.google.com/dl/android/maven2/<group path>/<artifact>/<version>/<artifact>-<version>.aar`.
The directly declared latest-stable set above does not force `androidx.core:core` above 1.12.0.

### 2.4 Retained mail libraries

| Coordinate | In project today (file:line) | Latest stable | Published | Source URL | Notes |
|---|---|---|---|---|---|
| `commons-io:commons-io` | 2.16.1 (`k9mail-vendored/build.gradle:107`) | 2.22.0 | 2026-04-19 | MC `commons-io/commons-io` ; https://commons.apache.org/proper/commons-io/download_io.cgi | Requires Java 8. Thunderbird `main` and tag 25.0: 2.22.0. Metadata also holds a legacy `20030203.000550` entry, which is not a current version. |
| `org.apache.james:apache-mime4j-core` | 0.8.11 (`k9mail-vendored/build.gradle:100`) | 0.8.15 | 2026-09-03 | MC `org/apache/james/apache-mime4j-core` ; tag `apache-mime4j-project-0.8.15` at https://github.com/apache/james-mime4j/tags | Thunderbird: 0.8.15. The James Mime4J site is stale (last published 2017). |
| `org.apache.james:apache-mime4j-dom` | 0.8.11 (`k9mail-vendored/build.gradle:102`) | 0.8.15 | 2026-09-03 | MC `org/apache/james/apache-mime4j-dom` | Thunderbird: 0.8.15. |
| `com.jcraft:jzlib` | 1.0.7 (`k9mail-vendored/build.gradle:109`) | **1.1.3** | 2013-10-04 | MC `com/jcraft/jzlib` | Versions on Central: 1.0.7, 1.1.0, 1.1.1, 1.1.2, 1.1.3. Thunderbird `main` and tag 25.0 pin **1.0.7** (`gradle/libs.versions.toml`). See finding 4 in section 1. |
| `com.beetstra.jutf7:jutf7` | 1.0.0 (`k9mail-vendored/build.gradle:111`) | 1.0.0 | 2011-08-25 | MC `com/beetstra/jutf7/jutf7` | Only version published. Thunderbird: 1.0.0. |

---

## 3. Compatibility matrix

### 3.1 Constraints, each with its citation

| # | Constraint | Evidence (quoted or read today) | URL |
|---|---|---|---|
| C1 | Gradle 9.9.x runs on JDK 25 | "A JVM version between 17 and 27 is required to execute Gradle." Java 25: toolchains 9.1.0, running Gradle "9.1.0 and after". | https://docs.gradle.org/current/userguide/compatibility.html |
| C2 | Gradle's tested Kotlin and AGP ranges | "Gradle is tested with Kotlin 2.0.0 through 2.5.0-Beta1." "Gradle is tested with Android Gradle Plugin 9.0 through 9.5.0-alpha10." Embedded Kotlin of Gradle 9.9.0: 2.4.21. | same |
| C3 | AGP to Gradle minimums | 9.5: 9.6.0; 9.4: 9.6.0; 9.3: 9.5.0; 9.2: 9.4.1; 9.1: 9.3.1; 9.0: 9.1.0. | https://developer.android.com/build/releases/about-agp |
| C4 | AGP 9.5 other minimums | "The maximum API level that Android Gradle plugin 9.5 supports is API level 37." SDK Build Tools min and default 36.0.0; JDK min 17. | https://developer.android.com/build/releases/agp-9-5-0-release-notes |
| C5 | Minimum AGP for API 37 | Table "Minimum versions of tools for Android API level": 37.0: AGP 9.1.1 (Studio Panda 3 Patch 1); 36.1: 8.13.0; 36: 8.9.1. AGP 9.0's maximum is API 36.1. | https://developer.android.com/build/releases/about-agp ; https://developer.android.com/build/releases/agp-9-0-0-release-notes |
| C6 | AGP built-in Kotlin and KGP | AGP 9.0 has a "runtime dependency on Kotlin Gradle plugin (KGP) 2.2.10 ... if you use a KGP version lower than 2.2.10, Gradle will automatically upgrade"; "built-in Kotlin requires KGP 2.2.10 or higher"; a higher KGP is declared on the buildscript classpath. The POMs of AGP 9.5.1 and 9.5.2 still depend on `kotlin-gradle-plugin:2.2.10`. No upper bound is stated by AGP. | https://developer.android.com/build/releases/agp-9-0-0-release-notes ; https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/9.5.2/gradle-9.5.2.pom |
| C7 | KGP's fully supported Gradle and AGP ranges | KGP 2.4.20 to 2.4.21: Gradle 7.6.3 to 9.7.0, AGP 8.5.2 to 9.3.1. KGP 2.3.20 to 2.3.21: Gradle up to 9.3.0, AGP up to 9.0.0. "You can also use Gradle and AGP versions up to the latest releases, but ... you might encounter deprecation warnings or some new features might not work." | https://kotlinlang.org/docs/gradle-configure-project.html |
| C8 | Kotlin and JDK 25 | Kotlin 2.3.0 added Java 25 support; 2.4.0 added Java 26. | https://kotlinlang.org/docs/whatsnew23.html ; https://kotlinlang.org/docs/whatsnew24.html |
| C9 | KSP to Kotlin pairing | Since 2.3.0: "KSP version is no longer tied to the Kotlin compiler version". 2.3.10: "Sanitize ':' in internal-name module suffix so KSP works with Kotlin 2.4.0 default module names"; "Fix R-class resolution in KSP when AGP 9 built-in Kotlin is enabled". 2.3.12: "Update minimum supported Android Gradle Plugin (AGP) version to 8.12.0". At tag 2.3.14 the embedded analysis API is `aa-kotlin-base = 2.4.20-dev-6138`, built with Kotlin 2.4.0, tested against AGP base 9.3.0-alpha01. | https://github.com/google/ksp/releases ; https://raw.githubusercontent.com/google/ksp/2.3.14/gradle/libs.versions.toml ; https://raw.githubusercontent.com/google/ksp/2.3.14/gradle.properties |
| C10 | Hilt to AGP | 2.59: "if you use the Hilt Gradle Plugin, AGP 9 is now a requirement along with AGP 9's own requirements like Gradle 9.1+". 2.59.1: "Updated minimum required AGP version to 9.0.0". | https://github.com/google/dagger/releases/tag/dagger-2.59 ; `.../dagger-2.59.1` |
| C11 | Hilt to Kotlin metadata | 2.59.2 fails on Kotlin 2.4: "Provided Metadata instance has version 2.4.0, while maximum supported version is 2.3.0" (issues 5177, 5180, 5190). 2.60: "Fixes #5190, #5180, #5177: Updated Kotlin version to 2.3.21". `hilt-android-2.60.1.pom` depends on kotlin-stdlib 2.3.21. | https://github.com/google/dagger/issues/5190 ; https://github.com/google/dagger/releases/tag/dagger-2.60 |
| C12 | Hilt to KSP | "Dagger's KSP support is stable as of Dagger 2.60+ and KSP 2.3.9+." | https://dagger.dev/dev-guide/ksp |
| C13 | Hilt minSdk | 2.60: "Multidex support is removed. minSDK for Hilt is now 23, matching AndroidX". AAR manifest of 2.60.1 still says 14. | https://github.com/google/dagger/releases/tag/dagger-2.60 ; https://repo1.maven.org/maven2/com/google/dagger/hilt-android/2.60.1/hilt-android-2.60.1.aar |
| C14 | androidx.hilt minSdk | hilt-work 1.2.0: minSdk 19; 1.3.0 and 1.4.0: minSdk 23, minCompileSdk 34, min AGP 8.1.1 (AAR manifest and `aar-metadata.properties`). 1.4.0 POM depends on hilt-android 2.59 and work-runtime 2.3.4. | https://dl.google.com/dl/android/maven2/androidx/hilt/hilt-work/1.4.0/hilt-work-1.4.0.aar (and `.pom`) |
| C15 | Robolectric SDK range | 4.18: "Support for SDK 23 has been removed. The minimum supported SDK is now 24, matching AndroidX". Compatibility table: 4.18: 24 to 37 (built with Gradle 9.8.2, AGP 9.5.0); 4.17: 23 to 37; 4.16: 23 to 36; 4.15: 21 to 35. | https://github.com/robolectric/robolectric/releases/tag/robolectric-4.18 ; https://robolectric.org/compatibility_table/ |
| C16 | Robolectric and JDK | Android 16 (36) and Android 17 (37) need Java 21; 34 and 35 need 17; 29 to 33 need 9. `DefaultSdkProvider` at tag 4.18.1 lists N (24) through CINNAMON_BUN (37). "When running tests on Java 17 or higher, the JVM requires --add-opens flags". 4.18: no more "restricted method" warnings on JDK 24+. Sandbox uses ASM 9.11.1. | https://robolectric.org/compatibility_table/ ; https://robolectric.org/getting-started/ ; https://raw.githubusercontent.com/robolectric/robolectric/robolectric-4.18.1/robolectric/src/main/java/org/robolectric/plugins/DefaultSdkProvider.java |
| C17 | JaCoCo class-file support | 0.8.14: "officially supports Java 25". 0.8.15: Java 26. 0.8.16: "officially supports Java 27", experimental Java 28, ASM 9.11.1. | https://www.jacoco.org/jacoco/trunk/doc/changes.html |
| C18 | WorkManager minSdk | 2.12.0: "The minSdk has been updated from API 23 to API 24". AAR: minSdk 24, minCompileSdk 35, min AGP 8.6.0. | https://developer.android.com/jetpack/androidx/releases/work |
| C19 | Lifecycle minSdk | 2.11.1 AARs: minSdk 23, minCompileSdk 34, min AGP 8.1.1. | https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-android/2.11.1/lifecycle-runtime-android-2.11.1.aar |
| C20 | AGP on JDK 25 | AGP 9.5.0-alpha08 fixed "Warning in generateReleaseRFile task when using JDK 25" (issue 553121473). | https://developer.android.com/build/releases/agp-9-5-0-release-notes |
| C21 | Coroutines and Kotlin | 1.11.0: "Kotlin was updated to 2.2.20". | https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.11.0 |

### 3.2 Proposed mutually compatible set

| Component | Proposed | Is it the newest stable? | Reason |
|---|---|---|---|
| JDK (build) | 25 | Newest LTS (27 is the newest feature release) | Project constraint. Gradle 9.9 runs on 17 to 27 (C1); AGP min 17 (C4); Kotlin 2.4 supports 25 (C8); JaCoCo 0.8.16 supports 27 (C17); Robolectric needs 21+ for SDK 36/37 (C16). |
| Gradle | 9.9.2 | Yes | AGP 9.5 needs at least 9.6.0 (C3). |
| AGP | 9.5.2 | Yes | Supports API 37 (C4); at least 9.1.1 is needed for compileSdk 37 (C5). |
| Kotlin Gradle plugin and stdlib | 2.4.21 | Yes (2.5.0 is RC only) | At least 2.2.10 for built-in Kotlin (C6). |
| KSP | 2.3.14 | Yes | Version-independent of Kotlin, works with Kotlin 2.4 and AGP 9 built-in Kotlin (C9); at least 2.3.9 for Dagger (C12). Avoid 2.3.13. |
| Hilt (plugin, runtime, compiler, testing) | 2.60.1 | Yes | Needed for Kotlin 2.4 metadata (C11); needs AGP 9 (C10). Kotlin 2.4 and Hilt 2.60 must move together. |
| androidx.hilt (hilt-work, hilt-compiler) | 1.4.0 | Yes | minSdk 23 (C14), satisfied by minSdk 24. |
| Robolectric | 4.18.1 | Yes | Runs API 24 to 37 (C15); tests pinned at `sdk=29` keep working; SDK 21 and 23 tests must go (REQ-015 AC-7). |
| JaCoCo | 0.8.16 | Yes | Java 25 class files supported since 0.8.14 (C17). |
| SDK Build Tools | 36.0.0 (AGP default) or 37.0.0 | 37.0.0 is the newest stable | AGP 9.5 minimum and default is 36.0.0 (C4). Using 37.0.0 needs an explicit `buildToolsVersion`; nothing fetched today requires it. |
| Mockito / Truth / GreenMail / auto-service / JUnit | 5.24.0 / 1.4.5 / 2.1.15 / 1.1.1 / 4.13.2 | Yes | No constraint found. |
| WorkManager, Lifecycle, coroutines, preference, annotation, core-role, security-crypto | 2.12.0, 2.11.1, 1.11.0, 1.2.1, 1.11.0, 1.1.0, 1.1.0 | Yes | All need at most minSdk 24 and compileSdk 35; WorkManager 2.12.0 needs minSdk 24 (C18). |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 | 37 is the current stable level (section 4) | AGP 9.5 max API 37; Robolectric 4.18.1 max 37. |

Places where the newest stable cannot be used, or where its use rests on a reading rather than a
vendor statement:

1. **No hard block was found**: every stated minimum is met by the set above.
2. **The set is outside Kotlin's "fully supported" range.** For KGP 2.4.21 the Kotlin documentation
   lists Gradle up to 9.7.0 and AGP up to 9.3.1 (C7), and Gradle's page lists AGP as tested through
   9.5.0-alpha10 (C2). Gradle 9.9.2 with AGP 9.5.2 and KGP 2.4.21 is permitted ("You can also use Gradle
   and AGP versions up to the latest releases") but may show deprecation warnings. This is the REQ-015
   AC-3 situation: no triple at the newest stable versions is listed as supported by all three vendors.
   The newest triple that sits inside every published range is **Gradle 9.7.0 + AGP 9.3.1 + KGP 2.4.21**
   (AGP 9.3 needs Gradle 9.5.0 or newer and supports API 37; https://developer.android.com/build/releases/agp-9-3-0-release-notes).
   That is the documented fallback under AC-4. Thunderbird `main` runs Gradle 9.7.0 + AGP 9.5.1 + Kotlin 2.4.21.
3. **Kotlin 2.5** is pre-release (RC). Hilt 2.60.1 is built with Kotlin 2.3.21 and, by the pattern in
   issue 5190 (library built with Kotlin N reads metadata up to N+1), is expected to stop at Kotlin 2.4
   metadata. This is an inference, not a vendor statement.
4. **Hilt Gradle plugin 2.60.1 with AGP 9.5.x and Gradle 9.9.x**: no vendor statement beyond "AGP 9 is
   a requirement". Residual risk; detected by the build.
5. **Robolectric 4.18.1** cannot emulate SDK 21 to 23, and needs Java 21 or newer for SDK 36/37. On
   Java 17+ the project documents `--add-opens` JVM arguments; Thunderbird's build adds
   `--add-opens=java.base/jdk.internal.access=ALL-UNNAMED` "when emulating SDK 37". The current project
   comment (`app/build.gradle:592-594`) says no flags are needed with 4.15.1; that must be re-checked.
6. **androidx.core:core 1.19.1** needs compileSdk 37 and AGP 9.1.0 or newer. Not forced by the app's
   own declarations, but Thunderbird `main` uses it (`androidxCore = "1.19.1"`).
7. **jzlib**: newest stable is 1.1.3 while Thunderbird pins 1.0.7 (section 1, finding 4). A decision is needed.

---

## 4. Android platform

### 4.1 Status of the API levels

| Level | Android version | Status today | Date | Evidence |
|---|---|---|---|---|
| 37 (37.0) | Android 17 (`CINNAMON_BUN`) | **Released stable; current stable platform release** | 2026-06-16 | "Today we're releasing Android 17 ... this moment marks the availability of the source code at the Android Open Source Project (AOSP)", dated 16 Jun 2026: https://developer.android.com/blog/posts/android-17-is-here . AOSP table: "Android17, 17, API level 37, 26Q2"; tag `android-17.0.0_r1`, build CP2A.260605.016: https://source.android.com/docs/setup/reference/build-numbers . GSI page lists "Android 17 (stable release)": https://developer.android.com/topic/generic-system-image/releases . Last beta was Beta 4.1 (2026-06-01): https://developer.android.com/about/versions/17/release-notes |
| 37.1, 37.2 (minor) | Android 17, minor releases 1 and 2 (`CINNAMON_BUN_1`, `CINNAMON_BUN_2`) | SDK platforms 37.1 and 37.2 are in the stable SDK channel. "Android 17 QPR1 (stable release)"; QPR2 and QPR3 are Beta (QPR2 Beta 7.1, 2026-10-16). | QPR1 date not verified | https://developer.android.com/reference/android/os/Build.VERSION_CODES_FULL ; https://developer.android.com/topic/generic-system-image/releases ; https://developer.android.com/about/versions/17/qpr2/release-notes ; https://dl.google.com/android/repository/repository2-3.xml |
| 36 | Android 16 (`BAKLAVA`) | Released stable | 2025-06-10 | "Android 16 is here", 10 June 2025: https://android-developers.googleblog.com/2025/06/android-16-is-here.html . AOSP: "Android16, API level 36, 25Q2", tag `android-16.0.0_r1`. |
| 36.1 | Android 16 QPR2 (`BAKLAVA_1`) | Released stable | 25Q4 | AOSP table "Android16 QPR2, 16, API level 36.1, 25Q4": https://source.android.com/docs/setup/reference/build-numbers |
| 38 / Android 18 | none | No page exists (`/about/versions/18` returns 404). An `android-CANARY` SDK platform exists. | | https://dl.google.com/android/repository/repository2-3.xml |

Caveat for REQ-017 AC-1: `https://developer.android.com/about/versions` (the index) and
`https://developer.android.com/tools/releases/platforms` do not show Android 17 as the newest entry as
fetched today (the platforms page stops at "Android 16 (API level 36), Revision 1 (March 2025)"), and
`https://developer.android.com/about/versions/17/setup-sdk` still calls the SDK package "Android
Cinnamon Bun Preview". The stable status rests on the blog post, the AOSP table and the GSI page above.

### 4.2 SDK Platform and Build Tools revisions (stable channel of the SDK repository)

Source: https://dl.google.com/android/repository/repository2-3.xml (the manifest `sdkmanager` reads).

| Package | Revision |
|---|---|
| `platforms;android-36` | 2 |
| `platforms;android-36.1` | 1 |
| `platforms;android-37.0` | 2 |
| `platforms;android-37.1` | 1 |
| `platforms;android-37.2` | 2 (37.2-beta1 to beta3 are also listed) |
| `build-tools;36.0.0` | 36.0.0 |
| `build-tools;36.1.0` | 36.1.0 |
| `build-tools;37.0.0` | 37.0.0 (rc1, rc2 also listed) |

Release dates of these packages: not verified (the release-notes pages are stale).

### 4.3 Behaviour changes, apps targeting API 36 (Android 16)

Source: https://developer.android.com/about/versions/16/behavior-changes-16 (last updated 2026-10-28).

| Title | Anchor | Relevant note |
|---|---|---|
| Edge to edge opt-out going away | `#edge-to-edge` | **Edge-to-edge**: `windowOptOutEdgeToEdgeEnforcement` is deprecated and disabled at target 36; the app cannot opt out. |
| Migration or opt-out required for predictive back | `#predictive-back` | **Predictive back**: system animations on by default; `onBackPressed` is not called and `KEYCODE_BACK` is not dispatched. |
| Elegant font APIs deprecated and disabled | `#elegant-text-height` | |
| Fixed rate work scheduling optimization | `#schedule-at-fixed-rate` | **Background work**: at most one missed `scheduleAtFixedRate` execution runs when the app returns to a valid lifecycle. |
| Adaptive layouts | `#adaptive-layouts` | Orientation, resizability and aspect-ratio restrictions are ignored on large screens (opt-out available at 36 only). |
| Health and fitness permissions | `#health-fitness-permissions` | **Permissions** (health data only). |
| New intents to handle bond loss and encryption changes | `#new-intents-to-handle-bond-loss` | |
| New way to remove bluetooth bond | `#bond-removal-api` | |
| MediaStore version lockdown | `#mediastore-lockdown` | |
| Safer Intents | `#safer-intents` | Intent resolution checks move to the receiving app (opt-in controls); relevant to exported receivers. |
| GPU syscall filtering | `#gpu-syscall-filtering` | |
| Local Network Permission | `#local-network-permission` | **Networking/permissions**: opt-in at 36; mandatory at 37. |
| App-owned photos | `#owned-photos` | |

### 4.4 Behaviour changes, all apps on Android 16

Source: https://developer.android.com/about/versions/16/behavior-changes-all (last updated 2026-10-28).

| Title | Anchor | Relevant note |
|---|---|---|
| JobScheduler quota optimizations | `#job-quota-opt` | **WorkManager / background work**: regular and expedited job runtime quota now depends on standby bucket and on whether the job started while the app was in a top state. |
| Abandoned empty jobs stop reason | `#abandoned-job-stop-reason` | **Background work**: new stop reason for jobs whose `JobParameters` were garbage collected. |
| Ordered broadcast priority scope no longer global | `#ordered-broadcast-priority` | Receiver priorities only order receivers within the same process (relevant to SMS/MMS receivers with `android:priority`). |
| ART internal changes | `#art-changes` | |
| 16 KB page size compatibility mode | `#16-kb-compatibility-mode` | **16 KB**: apps with 4 KB-aligned native code run in a compatibility mode with a notice. |
| Deprecating disruptive accessibility announcements | `#disruptive-a11y` | |
| Support for 3-button navigation | `#three-button-predictive-back` | **Predictive back** extends to 3-button navigation for migrated apps. |
| Automatic themed app icons | `#themed-app-icons` | |
| Virtual device owner overrides | `#virtual-device-owner-overrides` | |
| Improved security against Intent redirection attacks | `#intent-redirect-attacks` | By-default hardening against intent redirection. |
| Companion apps no longer notified of discovery timeouts | `#companion-device-timeout` | |
| Improved bond loss handling | `#improved-bond-loss-handling` | |

### 4.5 Behaviour changes, apps targeting API 37 (Android 17)

Source: https://developer.android.com/about/versions/17/behavior-changes-17 (last updated 2026-11-11).

| Title | Anchor | Relevant note |
|---|---|---|
| Memory limit widget | `#memory-limit-widget` | |
| New lock-free implementation of MessageQueue | `#lock-free-messagequeue` | May break code or test tools that reflect on `MessageQueue` internals. |
| Static final fields are now unmodifiable | `#static-final-fields` | Reflection on static final fields throws `IllegalAccessException`. |
| Accessibility support of complex IME physical keyboard typing | `#a11y-ime-pk` | |
| ECH (Encrypted Client Hello) enabled | `#ech-by-default` | **TLS**: ECH used for TLS connections when the networking library supports it; configurable via `<domainEncryption>` in the network security config. |
| Local network permission required for apps targeting Android 17 | `#local-network-protection-permission` | **Networking/permissions**: `ACCESS_LOCAL_NETWORK` runtime permission is mandatory for LAN access (matters for an IMAP server on the local network). |
| Hiding passwords from physical devices | `#hide-pwd-kbd` | |
| OTP protection for standard SMS messages | `#sms-otp-protection` | **SMS access**: for most apps targeting 37, SMS containing an OTP "do not become available until three hours after receipt"; `SMS_RECEIVED_ACTION` is withheld and "SMS provider database queries are filtered" during the delay. The default SMS app is exempt. Directly affects when a backup can see such messages. |
| Activity Security | `#activity-security` | Background activity launch hardening extended to `IntentSender`. |
| Enable CT by default | `#ct-default` | **TLS**: certificate transparency enabled by default at target 37. |
| Safer Native DCL-C | `#safer-dcl` | |
| Restrict PII fields in CP2 data view | `#restrict-pii-fields-cp2-data-view` | **Contacts**: `ACCOUNT_NAME`, `ACCOUNT_TYPE`, `ACCOUNT_TYPE_AND_DATA_SET` removed from `ContactsContract.Data`. |
| Enforce strict SQL checks in CP2 | `#enforce-strict-sql-checks` | **Contacts/permissions**: strict query validation on `ContactsContract.Data` without `READ_CONTACTS`. |
| Deprecation of setContentCaptureEnabled | `#deprecate-setcontentcaptureenabled` | |
| Background audio hardening | `#bg-audio` | |
| Platform API changes to ignore orientation, resizability, and aspect ratio constraints on large screens | `#large-screen-ignore-constraints` | The opt-out available at 36 is removed at 37. |
| Consistent BluetoothSocket read() behavior for RFCOMM | `#bluetooth-rfcomm-socket-change` | |

No heading on this page concerns foreground-service types, WorkManager, notifications, the default SMS
role, or 16 KB page size.

### 4.6 Behaviour changes, all apps on Android 17

Source: https://developer.android.com/about/versions/17/behavior-changes-all (last updated 2026-11-09).

| Title | Anchor | Relevant note |
|---|---|---|
| App memory limits | `#app-memory-limits` | **Background work**: per-app memory limits by device RAM; exit reason `REASON_OTHER` with "MemoryLimiter:AnonSwap". |
| SMS OTP protection | `#sms-otp-all-apps` | **SMS access**: the three-hour delay now also covers WebOTP-format messages for apps that are not the intended recipient, for all target levels; broadcast withheld and provider queries filtered. Default SMS app exempt. |
| usesClearTraffic deprecation plan | `#uses-clear-traffic-deprecation` | **Networking**: `usesCleartextTraffic` to be deprecated in a future release; use a network security config (API 24+). |
| Restrict implicit URI grants | `#restrict-implicit-uri-grants` | From Android 18, no automatic URI grants for `ACTION_SEND` and similar. |
| Per-app keystore limits | `#per-app-keystore-limits` | 50,000 keys for apps targeting 37; 200,000 otherwise (relevant to Keystore-backed credential storage only in principle). |
| Block cross profile loopback traffic | `#block-cross-profile-loopback` | **Networking**. |
| Restoring default IME visibility after rotation | `#ime-rotate` | |
| Touchpads deliver relative events by default during pointer capture | `#touchpad-pointer-capture` | |
| Background audio hardening | `#bg-audio` | |
| Autonomous re-pairing initiation for Bluetooth bond losses | `#autonomous-repairing-initiation` | |

### 4.7 Google Play target-API requirement (informational)

Source: https://developer.android.com/google/play/requirements/target-sdk

"Starting August 31 2026: New apps and app updates must target Android 16 (API level 36) or higher";
"Existing apps must target Android 15 (API level 35) or higher to remain available to new users on
devices running Android OS higher than your app's target API level"; extension available "to November
1, 2026". No requirement for API 37 is published yet.

### 4.8 Minimum AGP for compileSdk 37

**AGP 9.1.1** (Android Studio Panda 3 | 2025.3.3 Patch 1), from the table "Minimum versions of tools for
Android API level" at https://developer.android.com/build/releases/about-agp (last updated 2026-11-12).
AGP 9.1.1 was published 2026-04-13 (Google Maven). The project's AGP 9.0.1 has a maximum of API 36.1.
Note: `https://developer.android.com/about/versions/17/setup-sdk` still says "AGP 8.9.0-rc01 or higher";
that text conflicts with the table and looks stale.

---

## 5. Thunderbird for Android (`thunderbird/thunderbird-android`)

Read from `raw.githubusercontent.com` and the GitHub API on 2026-11-21.

| Item | Default branch `main` at `2d881297d9005af4d8035d382f078d07aaa0e079` (2026-11-20) | Latest release tag `THUNDERBIRD_25_0` / `K9MAIL_25_0` (published 2026-11-10; commit `1584e32ddc3e17f9f0ad5c435e11f3f1aa6a1a42`) | File |
|---|---|---|---|
| Gradle (wrapper) | 9.7.0 (`gradle-9.7.0-all.zip`) | 9.7.0 | `gradle/wrapper/gradle-wrapper.properties:4` |
| AGP | 9.5.1 | 9.2.1 | `gradle/libs.versions.toml` (`androidGradlePlugin`) |
| Kotlin | 2.4.21 | 2.4.20 | `gradle/libs.versions.toml` (`kotlinBom`) |
| KSP | 2.3.13 | 2.3.12 | `gradle/libs.versions.toml` (`kotlinKsp`) |
| JDK | 21 (daemon toolchain; `.java-version` = 21) | 21 | `gradle/gradle-daemon-jvm.properties:12` (`toolchainVersion=21`); `.java-version` |
| Java / Kotlin bytecode target | 17 (`JavaVersion.VERSION_17`, `JvmTarget.JVM_17`) | 17 | see next rows |
| compileSdk | 37 | 37 | main: `build-plugin/plugin/src/main/kotlin/net/thunderbird/gradle/plugin/ProjectConfig.kt:18`; tag: `build-plugin/src/main/kotlin/ThunderbirdProjectConfig.kt:11` |
| minSdk | **24** | **23** | main: same file `:14`; tag: `:7` |
| targetSdk | 36 | 36 | main: same file `:17`; tag: `:10` |
| Robolectric | 4.17 | 4.17 | `gradle/libs.versions.toml` |
| commons-io / mime4j / jzlib / jutf7 | 2.22.0 / 0.8.15 / 1.0.7 / 1.0.0 | 2.22.0 / 0.8.15 / 1.0.7 / 1.0.0 | `gradle/libs.versions.toml` |
| androidx.core / work / lifecycle | 1.19.1 / 2.12.0 / 2.10.0 | 1.18.0 / 2.11.2 / 2.10.0 | `gradle/libs.versions.toml` |
| coroutines / okio / okhttp | 1.11.0 / 3.18.2 / 5.5.1 | 1.11.0 / 3.18.2 / 5.5.1 | `gradle/libs.versions.toml` |

Verdict on the 2026-11-17 analysis: **confirmed** for today's `main` (Gradle 9.7.0, AGP 9.5.1, JDK 21,
compileSdk 37, minSdk 24). Additions: targetSdk is 36, not 37; Kotlin is 2.4.21; the latest release tag
still has minSdk 23 and AGP 9.2.1, so minSdk 24 is a `main`-only (beta 26.x) change so far.
Oddity: `gradle/libs.versions.toml` on both refs also holds `gradle = "9.6.1"` with a checksum, which
differs from the wrapper's 9.7.0; what consumes that key was not traced.
The mail modules read (`mail/common/build.gradle.kts`, `mail/protocols/imap/build.gradle.kts`) apply
`ThunderbirdPlugins.Library.jvm` on `main`, and still depend on commons-io, mime4j, jzlib, jutf7 and okio.

Newer pre-release tags: `THUNDERBIRD_26_0b4` (2026-11-17).

---

## 6. Not verified

1. **AGP 9.5.2 release notes**: the artifact is on Google Maven (2026-11-20) but the release-notes page
   lists only up to 9.5.1; its contents are unknown.
2. **Release dates of SDK Platform 36/36.1/37.0/37.1/37.2 revisions and of Build Tools 36.1.0 and
   37.0.0**: the repository manifest has no dates and the release-notes pages are stale.
3. **Mapping of API 37.1 / 37.2 to Android 17 QPR1 / QPR2**, and the QPR1 release date: inferred from
   `VERSION_CODES_FULL` ("minor release 1/2") and the GSI page; no page stating the mapping was fetched.
4. **Android 17 on the two developer.android.com index pages**: the versions index and the SDK Platform
   release-notes page do not show Android 17 as fetched; stable status is taken from the official blog
   post, AOSP and the GSI page.
5. **Hilt Gradle plugin 2.60.1 with AGP 9.5.x / Gradle 9.9.x, and KSP 2.3.14 with AGP 9.5.x**: no vendor
   statement naming these versions; not build-tested (no build was run).
6. **Hilt 2.60.1 and Kotlin 2.5 metadata**: inference only.
7. **Robolectric 4.18.1 on JDK 25 without `--add-opens`**: the project's current claim is for 4.15.1;
   not re-tested.
8. **ByteBuddy 1.17.7 (Mockito 5.24.0) Java 25 support**: not re-checked today; the project comment
   records Java 25 support from ByteBuddy 1.17.5.
9. **AAR `minCompileSdk` of `hilt-android` 2.60.1**: the AAR carries no `aar-metadata.properties`.
10. **Apache James Mime4J official download page**: the site is stale; 0.8.15 is confirmed by Maven
    Central and by the git tag only.
11. **Android 16 QPR2 (API 36.1) exact release date**, and behaviour-change pages for minor releases
    36.1, 37.1 and 37.2: not fetched.
12. **JDK 25.0.5.1 release date**: not recorded.
13. **The purpose of `gradle = "9.6.1"` in Thunderbird's version catalog**: not traced.
14. **Full transitive classpath of the app**: only declared coordinates and the main AndroidX transitives
    were resolved; `gradle/verification-metadata.xml` was not enumerated and no dependency report was run.
