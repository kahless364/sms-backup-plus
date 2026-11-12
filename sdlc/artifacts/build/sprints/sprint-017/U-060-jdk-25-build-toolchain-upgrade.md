---
type: story
status: done
artifact_type: user-story
priority: medium
complexity: high
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-060
sprint: '017'
title: Run the build on JDK 25 (Gradle 9.x / AGP 9.x toolchain upgrade) while keeping the Java 17 bytecode target
pipeline: ''
domain: build
requirement_source: bug:BUG-017
updated_at: '2026-11-12T16:27:53.792Z'
resolution: done
updated_date: 2026-11-12
---

# U-060: Run the build on JDK 25 (Gradle 9.x / AGP 9.x toolchain upgrade) while keeping the Java 17 bytecode target

## Story
As a developer of SMS Backup+ (and the product owner who asked to "update the app to use JDK 25"),
I want the Gradle build, compilers, and unit-test runtime to run on JDK 25 with all three project gates green,
So that code changes can be built and tested again on current developer machines (where Gradle already runs on JDK 25 and fails with "Unsupported class file major version 69"), on a supported toolchain combination, without weakening dependency verification or silently shrinking the coverage and test gates.

Scope statement (from BUG-017, binding): "use JDK 25" means the JVM that runs Gradle, javac, kotlinc and the unit tests. The app's bytecode target (`sourceCompatibility`/`targetCompatibility`/`jvmTarget` = 17; vendored module = Java 8) is NOT raised by this story. Compile `compileSdk`/`targetSdk` 35 and app runtime behavior are unchanged.

## Acceptance Criteria

### Carried over from BUG-017
- [ ] AC-1: Given Gradle runs on JDK 25 (confirmed by `./gradlew --version` showing a JDK 25 launcher/daemon JVM), when `./gradlew :app:assembleDebug` is run from a clean Git Bash shell (no `MSYS_NO_PATHCONV`), then it exits 0 and the output contains no "Unsupported class file major version 69".
- [ ] AC-2: Given the change is complete, when `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` is run on JDK 25, then all three tasks succeed, and the unit-test count (authoritative command: `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`) is greater than or equal to the baseline recorded before the first change commit, with no test newly `@Ignore`d/disabled/deleted to achieve green.
- [ ] AC-3: Given dependency verification is enforced, then `gradle/verification-metadata.xml` is still present and enforcing; no `--dependency-verification=off`, no blanket `verify-metadata="false"`, and no verification-disabling property appears in any build file, `gradle.properties`, CI file, or documented command; and every changed or added dependency/plugin has checksums recorded in that file.
- [ ] AC-4: Given the per-package JaCoCo LINE thresholds in `app/build.gradle`, then no threshold value is lower than at the pre-change commit (all remain >= 70% per package as currently configured), and no package is removed from, or added to an exclude list of, the verification rules to make the gate pass.
- [ ] AC-5: Given the debug APK produced by the fixed build, when it is installed (`adb install -r app/build/outputs/apk/debug/app-debug.apk`) and the main activity is launched on an API 36 emulator, then it launches without crash (no `FATAL EXCEPTION` for the app process in `adb logcat`).
- [ ] AC-6: Given the build documentation, then the "Local Build & Device Notes" JDK instruction in `.claude/CLAUDE.md` and the build-environment notes in `sdlc/artifacts/engagement/code-location.md` describe the JDK Gradle actually uses, and explicitly state how a developer's user-global `org.gradle.java.home` interacts with `JAVA_HOME` (the user-global property overrides `JAVA_HOME`). Ownership note: `.claude/CLAUDE.md` is Engine-Librarian-owned; the Developer must route that edit through the Engine Librarian rather than editing it directly.
- [ ] AC-7: Given a fresh clone on a machine with no special `~/.gradle/gradle.properties`, then the required JDK is declared in a repository-tracked way (e.g., a daemon-JVM criteria file) OR the required user-global setting is documented in the repository; the build is not dependent on an undocumented machine-specific setting. Whichever mechanism is chosen is shown to work with `org.gradle.java.installations.auto-detect=false` set, or the documentation states the limitation.
- [ ] AC-8: Given `.github/workflows/ci.yml`, then it is either updated to a JDK/Gradle combination consistent with this change (including the Android SDK Build Tools version the chosen AGP requires), or a recorded decision (in the story's Implementation Notes or an engagement doc) states why CI intentionally stays on JDK 17; the stale comment "AGP 8.x requires JDK 17" is corrected either way.

### Integration Criteria
- [ ] IC-1: Given any plugin/library version bump in `build.gradle` or `app/build.gradle`, then it is reflected at every use site and the set is mutually compatible: Hilt Gradle plugin, `hilt-android`, `hilt-compiler`, `hilt-android-testing` are identical versions; `androidx.hilt` artifacts are compatible with that Hilt; KSP matches the chosen Kotlin Gradle plugin; `kotlin-stdlib` matches the Kotlin version.
- [ ] IC-2: Given `compileOptions`/`jvmTarget` values, then `app/build.gradle` and `k9mail-vendored/build.gradle` remain mutually compatible (app = 17; vendored module = Java 8 unless planning proves JDK 25 `javac` rejects it, in which case the change is recorded as a Scope Concern, not silently made).
- [ ] IC-3: Given the debug and release variants, all call sites of build-script constructs modified by the DSL migration (e.g., `kotlinOptions`, `kotlin-android` plugin application, `buildDir` usages) are updated consistently across `build.gradle`, `app/build.gradle`, `k9mail-vendored/build.gradle`, and `metadata/build.gradle`.

### Added for the risks named in BUG-017 Suggested Approach
- [ ] AC-9 (coverage-gate integrity): Given the JaCoCo report is produced before the change (baseline) and after, then the number of distinct classes measured (e.g., count of `<class>` elements, per package, in the JaCoCo XML report) after the change is >= the baseline for every package, and the baseline and after figures are both recorded in the Implementation Notes. A passing `jacocoTestCoverageVerification` alone is not accepted as evidence. If class-directory paths in `app/build.gradle` (`intermediates/classes/debugUnitTest/...`, `intermediates/javac/debug/...`, `tmp/kotlin-classes/debug`) changed under the new AGP/KGP, the `fileTree`/classDirectories inputs are updated so no input resolves to an empty set.
- [ ] AC-10 (test-runtime on JDK 25): Given unit tests execute on JDK 25, then Robolectric, Mockito/ByteBuddy, and the JaCoCo agent are at versions that support a Java 25 runtime, and `:app:testDebugUnitTest` shows zero failures and zero errors attributable to class-file-version, ASM, ByteBuddy, or JaCoCo-instrumentation incompatibility. The Robolectric SDK set still covers every SDK level the tests configure.
- [ ] AC-11 (supported combination): Given Gradle on JDK 25 requires Gradle 9.1.0 or later and Gradle 9.x is tested only with AGP 9.x, then the chosen Gradle, AGP, and Kotlin Gradle plugin versions form a combination documented as supported by the official Gradle compatibility page and AGP release notes (AGP's minimum Gradle, AGP's minimum KGP, JDK 25 support), with the sources cited in Implementation Notes. An AGP 8.x-on-Gradle-9 combination is not accepted unless a recorded Scope Concern explains why.
- [ ] AC-12 (bytecode target unchanged): Given the finished change, then `compileOptions` source/target compatibility and Kotlin `jvmTarget` remain 17 for `:app` (expressed in whatever DSL the chosen AGP requires), the vendored module remains Java 8 source/target, and a verification step (e.g., `javap -v` on a compiled app class) shows class-file major version 61 for app classes.
- [ ] AC-13 (verification metadata provenance): Given `gradle/verification-metadata.xml` changed, then new entries were produced by Gradle's metadata-writing mode (not hand-edited), the resulting change is reviewable as a diff, no existing checksum for an unchanged artifact was altered, and `./gradlew :app:assembleDebug` passes with verification enforced (no write mode) afterwards.
- [ ] AC-14 (wrapper hardening): Given the Gradle wrapper is upgraded, then `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`, and `gradle-wrapper.properties` were regenerated via `./gradlew wrapper` (not by editing the URL only), and `distributionSha256Sum` is set to the official checksum of the chosen distribution.
- [ ] AC-15 (minimal version choice): Given the chosen Gradle/AGP pair, then it is the lowest pair satisfying AC-11 and the JDK 25 requirement (not automatically the latest), and the Implementation Notes record the rationale; moving to latest versions is out of scope.
- [ ] AC-16 (Gradle 9 deprecations): Given `./gradlew :app:assembleDebug --warning-mode all` is run once, then every construct that Gradle 9.x/AGP 9.x treats as an error is fixed in this story, and remaining deprecation warnings are listed in the Implementation Notes as recorded debt (not silently ignored).
- [ ] AC-17 (opt-outs are debt): Given AGP 9 opt-out flags (`android.builtInKotlin=false`, `android.newDsl=false`, or similar), then none is introduced unless each is listed in the Implementation Notes with the reason and the AGP version at which it will be removed; the kapt fallback is not relied on unless proven to work under the chosen AGP.
- [ ] AC-18 (daemon flags revalidated): Given the `org.gradle.jvmargs` entries in `gradle.properties` (`-Djava.nio.channels.spi.SelectorProvider=...`, `-Djdk.net.unixdomain.tmpdir=...`), then each is verified on JDK 25 and either retained or removed with a stated reason in the Implementation Notes; JDK 17 comments in `gradle.properties`, `build.gradle`, and `app/build.gradle` are updated to match reality.
- [ ] AC-19 (unchanged defaults): Given the change, then `compileSdk`/`targetSdk` 35, `android.nonTransitiveRClass=true`, `android.nonFinalResIds=true` (U-059), and removal of jetifier remain in effect, and no `org.gradle.java.home` is added to the repository `gradle.properties`.

## Affected Code
<!-- Paths are repo-relative. -->

| File | Current Behavior | Change |
|------|-----------------|--------|
| `gradle/wrapper/gradle-wrapper.properties`, `gradle-wrapper.jar`, `gradlew`, `gradlew.bat` | Gradle 8.14.1 | Gradle 9.x (>= 9.1.0), regenerated with checksum |
| `build.gradle` | AGP 8.7.3, KGP 2.0.21, KSP 2.0.21-1.0.28, Hilt plugin 2.51.1; `buildDir` in `clean` | AGP 9.x, KGP >= 2.2.10, matching KSP/Hilt; DSL fixes |
| `app/build.gradle` | `kotlin-android`, `kotlinOptions`, JaCoCo 0.8.12, Robolectric 4.12.2, Mockito 5.14.2, Hilt 2.51.1, hard-coded JaCoCo class paths, `-Werror` | DSL migration; test-tool bumps for Java 25; class-dir path review; comments |
| `k9mail-vendored/build.gradle` | Java 8 source/target | Verify with JDK 25 `javac` and `-Werror` scope; keep at 8 |
| `metadata/build.gradle`, `settings.gradle` | `task name(type:)` syntax; no toolchain config | Fix only if error under Gradle 9 / AGP 9 |
| `gradle/verification-metadata.xml` | 546 components, enforced | Add checksums via Gradle writer |
| `gradle.properties` | JVM args, JDK 17 comments | Revalidate on JDK 25; update comments |
| `gradle/gradle-daemon-jvm.properties` (candidate, new) | n/a | Optional repository-tracked JDK declaration for AC-7 |
| `.github/workflows/ci.yml` | temurin 17 | Update or record decision (AC-8) |
| `.claude/CLAUDE.md` (Engine-Librarian-owned) | JAVA_HOME = JBR 17 instruction | Update via Engine Librarian (AC-6) |
| `sdlc/artifacts/engagement/code-location.md` | Build notes | Update for actual JDK and `org.gradle.java.home` precedence |

## Existing Behavior to Preserve
- Dependency verification enforced via `gradle/verification-metadata.xml`.
- Per-package JaCoCo LINE >= 70% floors not lowered; coverage measures at least the same classes.
- All currently passing unit tests still pass; test count does not decrease.
- `compileSdk`/`targetSdk` 35; app runtime behavior unchanged; debug APK installs and launches on API 36 emulator.
- U-059 defaults (`nonTransitiveRClass`, `nonFinalResIds`, no jetifier) remain.
- Bytecode targets: app 17, vendored module Java 8.
- Local `adb` guidance (`MSYS_NO_PATHCONV=1` only in adb-only shells, never for Gradle).

## Verification Steps
1. (Baseline, before any change) Record `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` and, from a build on a working JDK (e.g., JBR 17 via local `org.gradle.java.home` containment), the per-package class counts from `app/build/reports/jacoco/**/*.xml`. Save both in Implementation Notes.
2. (AC-1) In Git Bash without `MSYS_NO_PATHCONV`, run `./gradlew --version` and confirm a JDK 25 JVM; run `./gradlew :app:assembleDebug`; confirm exit 0 and no "major version 69".
3. (AC-2, AC-10) Run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`; confirm success; re-run the `@Test` count and compare with baseline; inspect test XML results for zero failures/errors.
4. (AC-9) Compare post-change JaCoCo XML class counts per package with the baseline; confirm >=. Confirm each directory in the JaCoCo `classDirectories` inputs exists and is non-empty after a build.
5. (AC-3, AC-13) Grep build files, `gradle.properties`, `.github/workflows/ci.yml`, and docs for `dependency-verification`, `verify-metadata`; review the `git diff` of `gradle/verification-metadata.xml` for additions only; run the build without write mode.
6. (AC-4) `git diff` `app/build.gradle` and confirm no jacoco minimum decreased and no package dropped from the rules.
7. (AC-5) Install and launch the debug APK on an API 36 emulator; scan `adb logcat` for `FATAL EXCEPTION`.
8. (AC-6, AC-7) Read `.claude/CLAUDE.md` and `sdlc/artifacts/engagement/code-location.md`; confirm they match step 2's JDK and describe `org.gradle.java.home` precedence. Simulate a fresh clone (temporarily without the user-global JDK properties, or per the documented mechanism) and confirm the declared JDK is honored.
9. (AC-8) Read `.github/workflows/ci.yml`: JDK, Gradle, and SDK Build Tools consistent, or decision recorded.
10. (IC-1..IC-3, AC-11, AC-12, AC-14..AC-19) Review version table in Implementation Notes against cited official compatibility sources; run `javap -v` on a compiled app class to confirm major version 61; confirm wrapper files regenerated and `distributionSha256Sum` present; review the `--warning-mode all` record and opt-out list.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android / Gradle build | Wrapper, plugin stack, test tooling, DSL migration, verification metadata, CI, build docs | Developer |

## Technical Context
- Class file major version 69 = Java 25. Running Gradle on Java 25 requires Gradle 9.1.0 or later (BUG-017, verified against Gradle's compatibility page 2026-11-11). Gradle 9.x is documented as tested with AGP 9.x only; AGP 9.0 needs Gradle >= 9.1.0, JDK >= 17, KGP >= 2.2.10, and SDK Build Tools >= 36.0.0.
- The Solution Architect decides the concrete versions at planning. Constraints the choice must satisfy: (1) Gradle >= 9.1.0; (2) an AGP/Gradle pair documented as supported (AC-11); (3) KGP, KSP, Hilt, androidx.hilt, kotlin-stdlib mutually compatible (IC-1); (4) Robolectric, Mockito/ByteBuddy and JaCoCo each support a Java 25 runtime (AC-10); (5) the lowest satisfying pair (AC-15); (6) every new artifact has checksums (AC-3, AC-13).
- BUG-017's recommendation (advisory) is the AGP 9.x option, with an optional timeboxed AGP 8.x probe only as fallback. Gradle/AGP/KGP/KSP/Hilt changes are one atomic change set: no intermediate state builds on JDK 25.
- Highest-risk silent failure: hard-coded JaCoCo class directories in `app/build.gradle` can resolve to empty file trees after an AGP/KGP change (AC-9).
- `app/build.gradle` sets `-Werror` for the app's JavaCompile; planning must confirm it does not reach the Java 8 vendored compile (an "obsolete source/target" warning would fail the build).
- A `java { toolchain }` pinned to 17 was considered in BUG-017 and not recommended (contradicts the request, needs machine-specific changes or foojay).
- The user-global `~/.gradle/gradle.properties` (outside the repo, `org.gradle.java.home` = Android Studio JBR, JDK 25.0.4) overrides `JAVA_HOME`; it cannot be changed by a commit.
- Do not run Gradle with `MSYS_NO_PATHCONV=1`. Last successful build: 2026-06-24 (commit 38845a34).
- Scope Concern (carried from BUG-017, not resolved here): whether "use JDK 25" should also raise the compile target is a planning decision; this story is scoped to runtime only (target stays 17).
- Out of scope (optional separate items): raising the bytecode target above 17; moving to latest AGP/Gradle; cleaning non-error deprecation warnings.

## Supporting Documentation
- Source bug: BUG-017 (`sdlc/artifacts/stories/BUG-017-build-fails-on-jdk-25.md`)
- Related story: U-057 (Java 17 target raise / toolchain currency), U-059 (AGP-8 defaults)
- `.claude/CLAUDE.md` "Local Build & Device Notes (Windows / Git Bash)"
- Official: Gradle compatibility page; AGP release notes for the chosen AGP line (cite in Implementation Notes)

## Integration Contract References
None.

## Estimation Guidance
Complexity: high (frontmatter label raised from medium to match BUG-017's own assessment that it is likely higher). Drivers: atomic multi-plugin major upgrade (Gradle 9 + AGP 9 + KGP + KSP + Hilt), Kotlin DSL migration, test-tooling bumps on a Java 25 runtime, silent-failure risk in the JaCoCo gate, large verification-metadata regeneration, docs and CI. If planning finds it cannot fit one sprint, report back to the orchestrator for a split rather than shrinking the ACs; note that the parts are not separately buildable on JDK 25.

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes
- `.claude/CLAUDE.md` and `sdlc/config.yaml` are Engine-Librarian-owned; edits route through that agent.
- Optional containment while this story is in progress (not part of the fix): locally point `org.gradle.java.home` at `C:/Users/Michael.Horsley/.jdks/jbr-17.0.14` to build the baseline in Verification Step 1.
