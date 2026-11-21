# Supplement to the version research of 2026-11-21, for story U-071

Made by the orchestrator on 2026-11-21 (the planning date of story U-071) from the vendors' own pages, for the
items Q-1 to Q-12 that the story's plan lists (`sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md`, section 2.3).
It supplements `sdlc/artifacts/build/research/2026-11-21-latest-stable-versions.md`.

**How to read it.** Each item gives the page, the date it was fetched, and what the page states. Checksums and
table cells are data and are given exactly as the page shows them. Statements in running prose are given as the
fact they state, with at most a short excerpt, not as full quotations: the pages are public and each can be read
at its address. Where a page did not show what was asked for, the item says so. Nothing here was taken from memory.

**A ruling that follows from this form (orchestrator, 2026-11-21):** where the story's criterion AC-3 asks for
"verbatim vendor quotes with URLs", this record meets it with exact table values and checksums, and for prose with
the stated fact, the address and the fetch date. Longer verbatim reproduction of the vendors' release notes is not
made in this repository.

All pages were fetched on 2026-11-21.

## Q-1 and Q-2: Gradle 9.9.2 checksums

| File | SHA-256 | Source |
|------|---------|--------|
| `gradle-9.9.2-bin.zip` | `971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b` | `https://downloads.gradle.org/distributions/gradle-9.9.2-bin.zip.sha256` |
| `gradle-9.9.2-wrapper.jar` | `621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8` | `https://downloads.gradle.org/distributions/gradle-9.9.2-wrapper.jar.sha256` |

Both values equal the ones listed for 9.9.2 on `https://gradle.org/release-checksums/` ("Binary-only (-bin) ZIP"
and "Wrapper JAR"). The address `services.gradle.org/distributions/...` redirects to `downloads.gradle.org`; the
first of the two could only be fetched from the second host.

For fallback F2 (Gradle 9.7.0), should it fire:

| File | SHA-256 | Source |
|------|---------|--------|
| `gradle-9.7.0-bin.zip` | `84fbba45c7f4c64abc77460e1c00f541e9f960e3c7ed2538f1ede19eacd873ae` | `https://downloads.gradle.org/distributions/gradle-9.7.0-bin.zip.sha256` |
| `gradle-9.7.0-wrapper.jar` | `7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d` | `https://downloads.gradle.org/distributions/gradle-9.7.0-wrapper.jar.sha256` |

Both also equal the 9.7.0 values on the release-checksums page. The Gradle 9.7 compatibility page was not re-read;
if F2 fires, it is read then.

## Q-3: Gradle 9.9.2 compatibility page

Page: `https://docs.gradle.org/9.9.2/userguide/compatibility.html`.

- Java table, columns "Java version | Support for toolchains | Support for running Gradle": the row for Java 25
  reads `25 | 9.1.0 | 9.1.0 and after`.
- Running Gradle needs a JVM between 17 and 27; JVM 28 and later are not yet supported.
- Kotlin: the page says Gradle is tested with Kotlin 2.0.0 through 2.5.0-Beta1, and that Beta and RC versions may
  or may not work.
- Android: the page says Gradle is tested with the Android Gradle Plugin 9.0 through 9.5.0-alpha10, and that Alpha
  and Beta versions may or may not work.

**What this means for the plan:** Gradle 9.9.2 names Java 25 as supported for running Gradle and for toolchains.
Its tested range for the Android Gradle Plugin ends at a 9.5.0 alpha; 9.5.2 is beyond what the page names as
tested.

## Q-4: Android Gradle Plugin 9.5

Page: `https://developer.android.com/build/releases/about-agp`.

- Compatibility table, columns "Plugin version | Minimum required Gradle version": the row for 9.5 reads
  `9.5 | 9.6.0`.
- The page does not list a version 9.5.2 anywhere. The only 9.5.x string in its compatibility section is the
  example "version 9.5.0".

Page: `https://developer.android.com/build/releases/agp-9-5-0-release-notes`.

- Compatibility table, columns "Minimum version | Default version":

  | | Minimum version | Default version |
  |---|---|---|
  | Gradle | 9.6.0 | 9.6.0 |
  | SDK Build Tools | 36.0.0 | 36.0.0 |
  | JDK | 17 | 17 |

- The page states that the maximum API level the Android Gradle plugin 9.5 supports is API level 37.
- Patch releases: the page has a heading for 9.5.1, with no date shown under it. **It has no entry for 9.5.2.**

**What this means for the plan:** Gradle 9.9.2 is above the 9.6.0 minimum. The design's target, 9.5.2, is not
named on either Android page on this date; the version research of the same date took it from the plugin's
published artifacts. Under the design's rule D1a the plan decides whether 9.5.2 stands (it exists as a published
artifact and is the newest non-pre-release) and records that the release notes do not yet describe it; fallback F1
(9.5.1) is the version the release notes do name.

## Q-5: Kotlin Gradle plugin compatibility

Page: `https://kotlinlang.org/docs/gradle-configure-project.html`.

- Table, columns "KGP version | Gradle min and max versions | AGP min and max versions": the row reads
  `2.4.20–2.4.21 | 7.6.3–9.7.0 | 8.5.2–9.3.1`.
- Below the table the page says that Gradle and AGP versions up to the latest releases can also be used, with the
  caution that deprecation warnings may appear or some new features might not work.

**What this means for the plan:** Gradle 9.9.2 and the Android Gradle Plugin 9.5.x are above the maxima Kotlin
2.4.21 documents (9.7.0 and 9.3.1). The page allows newer versions with that caution. This is the condition the
design's fallback F2 exists for.

## Q-6: KSP

Page: `https://github.com/google/ksp/releases/tag/2.3.14`. The release notes list six changes, all fixes or
additions inside KSP: a fix for a regression in which library symbols disappeared during concurrent KSP runs;
support for reading Kotlin context parameters; skipping analysis-session start-up and processor execution in
incremental builds when no file is dirty; and three fixes to annotation arguments, default arguments and type
parameter bounds. The page marks 2.3.14 as the latest release.

Page: `https://github.com/google/ksp/releases/tag/2.3.13`. The release carries a section headed "Defect in
release": it says the release has a regression (issue 3286) that can affect concurrent executions of KSP, and that
a fix is coming. The page shows no pre-release label on 2.3.13.

**What this means for the plan:** 2.3.13 is never used (the design and the plan already exclude it); 2.3.14 is the
release that fixes its regression.

## Q-7: Dagger and Hilt 2.60.1

Page: `https://github.com/google/dagger/releases/tag/dagger-2.60.1`. The release notes list two bug fixes: issue
5203, a regression in 2.60 in which Hilt compiler options (the example given is `fastInit`) failed; and issue
5204, an import error for `@CanIgnoreReturnValue`, fixed by removing the annotation from
`ActivityRetainedComponentBuilder`.

## Q-8: Robolectric

Page: `https://robolectric.org/compatibility_table/`.

- Table, columns "Robolectric version | Android API levels | Gradle version | Android Gradle Plugin version": the
  row reads `4.18 | 24 → 37 | 9.8.2 | 9.5.0`.
- A second table on the page gives the minimum Java version needed to run tests against each Android version; for
  Android 17 (API 37) it gives Java 21.

Page: `https://github.com/robolectric/robolectric/releases/tag/robolectric-4.18.1`. The notes describe 4.18.1 as a
minor release: fixes for data races when several threads link calls to the same shadow method; a fix to
`ShadowServiceManager.waitForService`; content providers are now installed for lazily created applications;
changes for the Robolectric simulator; and two opt-in system properties (`robolectric.bindRealServices`,
`robolectric.installDefaultUncaughtExceptionHandler`).

**What this means for the plan:** the supported SDK range of 4.18 starts at 24, which matches the floor story U-068
set. "Content providers are now installed for lazily created applications" is a behaviour change to watch in the
Robolectric increment.

## Q-9: Mockito 5.24.0 and Byte Buddy

Page: `https://github.com/mockito/mockito/releases`. The entry for v5.24.0 is marked latest (released 23
September); its body is a generated change list of 27 commits. It contains no line about Java 25 or Byte Buddy.

Page: `https://github.com/raphw/byte-buddy/blob/master/release-notes.md`. Under version 1.17.5 (30 March 2025) the
notes say ASM was updated to 9.8 to support Java 25. A later entry, version 1.18.9 (1 June 2026), says use of
`Unsafe` is disabled by default when Java 25 or newer is found.

**What this means for the plan:** Java 25 support entered Byte Buddy at 1.17.5, so 1.17.7 has it. Which Byte Buddy
version Mockito 5.24.0 resolves to is read from the build's own dependency report in the Mockito increment, not
from these pages.

## Q-10: GitHub-hosted runners and actions

Page: `https://github.com/actions/runner-images`. The "Available Images" table has a row for Ubuntu 22.04 with the
label `ubuntu-22.04` and no deprecation note. (The only deprecation note on the page is for macOS 14.
`ubuntu-latest` is an alias of the Ubuntu 24.04 row.)

Newest release marked "Latest" on each releases page (the pages show day and month; the year is not shown in the
fetched text):

| Action | Latest | Shown as released |
|--------|--------|-------------------|
| `actions/checkout` | v7.0.1 | 20 July |
| `actions/setup-java` | v6.3.0 | 14 November |
| `actions/cache` | v6.1.0 | 26 June |

## Q-11 (optional): JaCoCo 0.8.16

Page: `https://www.jacoco.org/jacoco/trunk/doc/changes.html`. The section is headed "Release 0.8.16 (2026/10/24)".
New features: official support for Java 27; experimental support for Java 28 class files; and a change for
instrumenting classes loaded by the platform class loader. Non-functional: JaCoCo now depends on ASM 9.11.1.

## Q-12 (optional): the Gradle wrapper task

Page: `https://docs.gradle.org/9.9.2/userguide/gradle_wrapper.html`. In the section on upgrading the wrapper, the
guide says that for all wrapper files to be completely up to date the `wrapper` task must be run a second time,
and that a single run updates `gradle-wrapper.properties` only.

**What this means for the plan:** it confirms the plan's two-run procedure for the wrapper increment.

## Q-13 to Q-17: what the Android Gradle Plugin release notes 9.1 to 9.5 say changed

Added on 2026-11-21 for the plan's iteration 2 (items Q-13 to Q-17), in the same form as the items above. Pages:
`https://developer.android.com/build/releases/agp-9-1-0-release-notes`, and the same address with `9-2-0`, `9-3-0`,
`9-4-0` and `9-5-0`. All five were fetched in full. **None of the five pages has a section headed "Behavior changes"
or "Breaking changes".** What they have instead is listed per version.

Compatibility tables (columns "Minimum version | Default version"), and the highest API level each page names:

| Plugin | Gradle | SDK Build Tools | NDK (default) | JDK | Kotlin Gradle plugin row | Highest API level named |
|--------|--------|-----------------|---------------|-----|--------------------------|-------------------------|
| 9.1 | 9.3.1 / 9.3.1 | 36.0.0 / 36.0.0 | 28.2.13676358 | 17 / 17 | 2.2.10 / 2.2.10 | 37.0 (stated for 9.1.1) |
| 9.2 | 9.4.1 / 9.4.1 | 36.0.0 / 36.0.0 | 28.2.13676358 | 17 / 17 | 2.2.10 / 2.2.10 | 37.0 |
| 9.3 | 9.5.0 / 9.5.0 | 36.0.0 / 36.0.0 | 28.2.13676358 | 17 / 17 | no such row | 37 |
| 9.4 | 9.6.0 / 9.6.0 | 36.0.0 / 36.0.0 | 28.2.13676358 | 17 / 17 | no such row | 37 |
| 9.5 | 9.6.0 / 9.6.0 | 36.0.0 / 36.0.0 | 28.2.13676358 | 17 / 17 | no such row | 37 |

Changes each page describes:

| Item | Plugin | Section on the page | What the page states |
|------|--------|---------------------|----------------------|
| Q-13 | 9.1 | "R8 changes" | (1) When compiling to DEX, R8 now repackages classes into the unnamed (default) package by default, as if `-repackageclasses` had been given; the new rule `-dontrepackage` turns this off. (2) `-maximumremovedandroidloglevel` accepts level names in capitals as well as numbers |
| Q-14 | 9.2 | "R8 changes" | (1) Wildcard `-keepattributes` patterns no longer match the three `RuntimeInvisible...Annotations` attributes; they are kept only when named in full (`RuntimeInvisibleAnnotations`, `RuntimeInvisibleParameterAnnotations`, `RuntimeInvisibleTypeAnnotations`). A rule such as `-keepattributes *Annotation*` no longer keeps them. (2) Member-name patterns in keep rules can be negated. Also on the page, marked experimental and off unless `android.experimental.reportAggregationSupport=true`: combined coverage and test reports |
| Q-15 | 9.3 | feature sections | (1) A new task `:app:analyzeReleaseR8Config` writes the R8 configuration analyzer report on its own; a release build with R8 still writes it too. (2) A new `optimization` block per build type; with it, turning optimisation on turns on code optimisation and optimised resource shrinking, and the default Android keep-rules file need not be named. The page says the earlier DSL is still supported. (3) Keep rules may be placed in `src/<variant>/keepRules/` with the suffix `.keep` |
| Q-16 | 9.4 | feature sections | (1) A module can opt out of the new Variant API with `android.newDsl.optOut=<module path>` until plugin 10 makes it mandatory. (2) A mismatch of flavour dimensions between an app and a dynamic-feature module is a warning from 9.4 (`android.enforceDynamicFeatureVariantMatching=true` makes it an error) |
| Q-17 | 9.5 | feature section | Experimental automated backup-and-restore tests on connected devices (`testOptions.backupTests`), off unless `android.experimental.testSuiteSupport=true` and `android.experimental.enableBackupTest=true` |

Patch releases the pages name: 9.2.1 (one fixed issue, a `ClassNotFoundException` for `com.android.tools.r8.RecordTag`
after upgrading); 9.3.1 (no public issues fixed), 9.3.2 (a lint crash on JDK 17), 9.3.3 (a longer list, among them
that the new `optimization { enable = true }` block did not set `isMinifyEnabled` on the variant object); 9.5.1 (no
public issues fixed). No dates are shown. The 9.1 and 9.4 pages name no patch release. No page names 9.5.2 (as Q-4).

None of the five pages has an item on: lint defaults or the lint baseline format, unit-test defaults, KSP or kapt,
built-in Kotlin support, the Java toolchain beyond the JDK 17 row, the manifest merger, `compileSdk`/`targetSdk`
handling, or BuildConfig and R-class generation.

**What this means for the plan (increment I3, the plugin raise from 9.0.1):**

- **Repackaging (9.1) is the change most likely to show in the R8 comparison.** Classes that R8 is free to rename
  move to the unnamed package, so the mapping file and the list of renamed classes will differ from the previous
  increment although no keep rule changed. Kept names are not affected by repackaging. A difference of this kind at
  I3 is explained by Q-13; a kept class, a manifest class or a seeds line that changes is not.
- **The `-keepattributes` change (9.2) can remove runtime-invisible annotations** that a wildcard rule kept before.
  Whether the app's own rule file or a library's shipped rules use such a wildcard is read from the build's merged
  configuration at I3.
- **Nothing on the pages changes a default the build relies on for lint, unit tests, coverage or the manifest.** The
  build uses the earlier DSL (`minifyEnabled`, `proguardFiles`), which the 9.3 page says is still supported.
- The Gradle minimum for 9.5 (9.6.0) is below the plan's Gradle 9.9.2.

## Not found, or limits of this record

- The five release-notes pages 9.1 to 9.5 have no "Behavior changes" section; the table above lists what they do
  describe (Q-13 to Q-17). Item Q-18 (Gradle's upgrade guide, 9.6 to 9.9) was not fetched.

- The Android pages do not describe plugin version 9.5.2 (see Q-4).
- The year of the three action releases is not shown in the fetched text.
- The Mockito 5.24.0 entry says nothing about Java 25; the Byte Buddy notes do.
- Prose is summarised, not quoted in full (see "How to read it").
