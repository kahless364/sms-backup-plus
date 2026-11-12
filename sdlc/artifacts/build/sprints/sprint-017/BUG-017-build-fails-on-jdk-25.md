---
type: bug
status: done
artifact_type: bug
severity: high
priority: medium
complexity: medium
parallel_eligible: true
iteration: 1
requirements: []
design_docs: []
integration_contracts: []
dependencies: []
related_items:
  - U-057
  - U-060
platforms: []
tags: []
id: BUG-017
title: 'Gradle build fails on JDK 25: "Unsupported class file major version 69" (Gradle 8.14.1 toolchain not JDK 25 compatible)'
domain: build
origin: qa
updated_at: '2026-11-12T16:31:32.372Z'
resolution: done
resolved_by: U-060
sprint: '017'
---

# BUG-017: Gradle build fails on JDK 25: "Unsupported class file major version 69"

## Description
`./gradlew :app:assembleDebug` fails within ~9 seconds, before any project source is compiled, with a Groovy compiler error while compiling the build scripts (`_BuildScript_`): `BUG! exception in phase 'semantic analysis' ... Unsupported class file major version 69`. Class file major version 69 corresponds to Java 25. The JVM that Gradle actually runs on is JDK 25 (Android Studio's bundled JBR, now openjdk 25.0.4), selected by the user-global `org.gradle.java.home` property in `~/.gradle/gradle.properties`. That property overrides `JAVA_HOME`, so the documented instruction in `.claude/CLAUDE.md` ("set `JAVA_HOME` to the JBR 17") no longer takes effect.

The user's request that surfaced this was "update the app to use JDK 25". The project's pinned build toolchain (Gradle wrapper 8.14.1, AGP 8.7.3, Kotlin Gradle plugin 2.0.21, KSP 2.0.21-1.0.28, Hilt Gradle plugin 2.51.1, JaCoCo 0.8.12, Robolectric 4.12.2, Mockito 5.14.2) was last validated on JDK 17 (U-057) and has not been shown to run on JDK 25. No code change can currently be built or tested on this machine. The last successful build was 2026-06-24 (`app-debug.apk` from commit 38845a34).

## Steps to Reproduce
1. On Windows 11 in Git Bash, check out branch `sdlc/modernization-plan` at commit b09bda5d.
2. Run `export JAVA_HOME="C:/Users/Michael.Horsley/.jdks/jbr-17.0.14"`.
3. Run `./gradlew :app:assembleDebug -q`.
4. Observe: the build fails in ~9 seconds with the output shown under Actual Behavior.

Environment in which this was observed (verified 2026-11-11):
- `C:\Program Files\Android\Android Studio\jbr\bin\java -version` reports openjdk 25.0.4.
- `~/.gradle/gradle.properties` (user-global, not in the repository) sets `org.gradle.java.home=C:\\Program Files\\Android\\Android Studio\\jbr`, `org.gradle.java.installations.auto-detect=false`, and `org.gradle.java.installations.paths=` the same JBR. Its comment still describes that JBR as "a full JDK 21".
- `java` on PATH is Oracle JDK 25.0.5 (`C:\Program Files\Java\jdk-25.0.5`).
- JBR 17.0.14 still exists at `C:/Users/Michael.Horsley/.jdks/jbr-17.0.14` but is not the JVM Gradle uses.

## Expected Behavior
The three project gates pass with Gradle running on JDK 25:
`./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`.
Dependency verification stays enabled, per-package JaCoCo LINE floors in `app/build.gradle` are not lowered, and the resulting debug APK installs and launches on an API 36 emulator.

## Actual Behavior
The build aborts during build-script compilation:

```
FAILURE: Build failed with an exception.
* What went wrong:
BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_' Unsupported class file major version 69
> Unsupported class file major version 69
```

## Evidence
Console output above is the verbatim failure supplied in the reproduction details. Java version outputs are as listed under Steps to Reproduce.
<!-- Attach full `./gradlew :app:assembleDebug --stacktrace` output here if captured during planning. -->

## Acceptance Criteria
- [ ] AC-1: The defect no longer reproduces following the steps above: with Gradle running on JDK 25, `./gradlew :app:assembleDebug` completes successfully and does not emit "Unsupported class file major version 69".
- [ ] AC-2: Existing functionality is not broken by the fix: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` all pass, with the unit-test count not reduced relative to the pre-change baseline (counted per the `git grep` command in `.claude/CLAUDE.md`).
- [ ] AC-3: Dependency verification remains enabled (`gradle/verification-metadata.xml` still enforced; no `--dependency-verification=off` and no blanket `verify-metadata="false"`), and every changed or added dependency has checksums recorded in that file.
- [ ] AC-4: Per-package JaCoCo LINE thresholds in `app/build.gradle` are not lowered (still >= 70% per package as currently configured).
- [ ] AC-5: The debug APK produced by the fixed build installs and launches without crash on an API 36 emulator.
- [ ] AC-6: Build documentation is consistent with the fix: the "Local Build & Device Notes" JDK instruction in `.claude/CLAUDE.md` and any build-environment notes in `sdlc/artifacts/engagement/code-location.md` describe the JDK that Gradle actually uses, and state how a developer's user-global `org.gradle.java.home` interacts with `JAVA_HOME`.
- [ ] AC-7: The project build is not dependent on an undocumented machine-specific setting: either the required JDK is declared in a repository-tracked way, or the required `~/.gradle/gradle.properties` setting is documented in the repo.
- [ ] AC-8: CI configuration (`.github/workflows/ci.yml`) is either updated to a JDK/Gradle combination consistent with the fix, or an explicit recorded decision states why it intentionally stays on JDK 17.

### Integration Criteria (if fix modifies component boundaries)
- [ ] IC-1: Any plugin/library version bump in `build.gradle` or `app/build.gradle` is reflected at all use sites (e.g., Hilt compiler/runtime and androidx.hilt versions remain mutually compatible; KSP version matches the Kotlin version).
- [ ] IC-2: If `compileOptions`/`jvmTarget` values change, `app/build.gradle` and `k9mail-vendored/build.gradle` remain mutually compatible (the vendored module currently targets Java 8 / VERSION_1_8).

## Impact Analysis
No `change_records.layers` are configured in `sdlc/config.yaml`; impact was assessed by grep of the build files and build documentation only. Grep terms: `JAVA_HOME|jdk|JDK|java-version|jvmTarget|JavaVersion|jbr|JBR|gradle-8|toolVersion|distributionUrl`.

| # | Area | Affected? | Files | Evidence / Action Needed |
|---|------|-----------|-------|--------------------------|
| 1 | Gradle wrapper | Likely | `gradle/wrapper/gradle-wrapper.properties` | Line 3: `distributionUrl=...gradle-8.14.1-bin.zip`. Running Gradle on Java 25 is believed to require Gradle 9.1.0+ (needs confirmation against Gradle docs). A wrapper upgrade is a candidate action; changes `gradle-wrapper.jar`/distribution checksums if pinned. |
| 2 | Root build / plugin pins | Likely | `build.gradle` | Lines 7 (AGP 8.7.3), 10 (Kotlin Gradle plugin 2.0.21), 14 (KSP 2.0.21-1.0.28), 18 (Hilt Gradle plugin 2.51.1); line 17 comment ties Hilt 2.51.1 to "JDK 17". Compatibility of each with a newer Gradle and with JDK 25 is unconfirmed. |
| 3 | App module build | Likely | `app/build.gradle` | `compileOptions` 17 (lines 90-91), `jvmTarget = '17'` (96), kotlin-stdlib 2.0.21 (115), Robolectric 4.12.2 (159), mockito-core 5.14.2 (162), hilt 2.51.1 / androidx.hilt 1.2.0 (175-186), jacoco `toolVersion = "0.8.12"` (211), `excludes = ['jdk.internal.*']` (240), JDK 17 comment (575). Test-runtime tools (JaCoCo, Robolectric, Mockito/ByteBuddy) must be able to read/run on Java 25 class files if tests execute on JDK 25. |
| 4 | Vendored module | Possibly | `k9mail-vendored/build.gradle` | Lines 76-77: Java 8 source/target. Newer JDKs have removed support for source/target 8 at some versions; needs checking against JDK 25. |
| 5 | Dependency verification | Yes (if any version changes) | `gradle/verification-metadata.xml` | 546 `<component>` entries (4048 lines). Any version change requires regenerated/added checksums, per the reproduction details. |
| 6 | Gradle properties | Possibly | `gradle.properties` | Line 9: `org.gradle.jvmargs` includes `-Djava.nio.channels.spi.SelectorProvider=...` and `-Djdk.net.unixdomain.tmpdir=...`; behavior on JDK 25 unverified. No `org.gradle.java.home` in repo. Comments on lines 33-36 reference JDK 17. |
| 7 | Settings | Not found | `settings.gradle` | No JDK/toolchain matches in grep; no toolchain resolver or `foojay` configured. |
| 8 | CI | Possibly | `.github/workflows/ci.yml` | Lines 19-26: `actions/setup-java@v4`, `java-version: '17'`, temurin; comments state "AGP 8.x requires JDK 17". Runs `./gradlew test lint jacocoTestReport assembleRelease` plus `:app:jacocoTestCoverageVerification`. |
| 9 | Developer documentation | Yes | `.claude/CLAUDE.md` | Line 31: instructs `JAVA_HOME` = JBR 17; now ineffective on machines with user-global `org.gradle.java.home`. |
| 10 | Engagement docs | Not found | `sdlc/artifacts/engagement/*` | Grep for `JDK|JBR|JAVA_HOME|Gradle 8|gradle-8|AGP|Java 17` returned no matches. `code-location.md` should still be reviewed for build-command accuracy. |
| 11 | User-global config (outside repo) | Yes | `~/.gradle/gradle.properties` | Sets `org.gradle.java.home` to Android Studio JBR (JDK 25.0.4); comment stale ("full JDK 21"). Not tracked in the repo; cannot be fixed by a commit. |

Affected areas: 8+ (cascade analysis required, below). Per the severity multiplier, 7+ layers is a systemic issue (3x), driven largely by the tightly coupled toolchain version pins rather than by code changes.

### Cascade Analysis

**Root Layer:** Build toolchain / environment (Gradle runtime JVM selection plus pinned Gradle/plugin versions).
**Root Defect (as observed, not a root-cause claim):** Gradle 8.14.1's build-script compiler cannot read Java 25 (major 69) class files, and the JVM Gradle runs on is now JDK 25. Root cause is to be established in planning.
**Cascade Direction:** Full downstream within the build: wrapper -> plugins (AGP/Kotlin/KSP/Hilt) -> app dependencies and test tooling -> verification metadata -> CI and docs.

#### Propagation Path

| Step | From | To | Propagated? | Evidence |
|------|------|----|-------------|----------|
| 1 | JVM selection (`~/.gradle/gradle.properties`) | Gradle wrapper 8.14.1 | Yes | Failure occurs at build-script compile on the Java 25 JVM. |
| 2 | Gradle version | AGP 8.7.3 / Kotlin 2.0.21 / KSP / Hilt plugin pins in `build.gradle` | Unconfirmed | A Gradle major upgrade may force plugin upgrades; compatibility to be checked in planning. |
| 3 | Plugins | App deps and test tooling (JaCoCo 0.8.12, Robolectric 4.12.2, Mockito 5.14.2) | Unconfirmed | Test tooling must handle Java 25 class files if tests run on JDK 25. |
| 4 | Any version change | `gradle/verification-metadata.xml` | Yes, if versions change | Dependency verification is enforced. |
| 5 | Local fix | CI (`ci.yml` on JDK 17) and `.claude/CLAUDE.md` | Yes | Local and CI JDK would diverge; docs instruct the now-ineffective JBR 17 setup. |

#### Fix Order (advisory)
1. [ ] Confirm the minimum Gradle version and plugin versions supporting JDK 25 against official docs.
2. [ ] Toolchain pins (wrapper, `build.gradle`, `app/build.gradle`) - ROOT FIX
3. [ ] `gradle/verification-metadata.xml` - CASCADE FIX
4. [ ] `.github/workflows/ci.yml`, `.claude/CLAUDE.md`, `code-location.md` - CASCADE FIX
5. [ ] Verification: all three gates plus emulator launch.

#### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| U-057 (related; JDK 17 / Java 17 target raise) | sprint-015 (complete) | Done | Review: its JDK 17 assumptions are what this bug challenges; no rework unless the target is raised. |
| Any pending code change | n/a | n/a | Hold: cannot be built or tested until this is fixed. |

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->
Advisory only — refined during story planning.

**Verification basis.** Statements marked *[verified]* were checked on 2026-11-11 against the official Gradle compatibility page (https://docs.gradle.org/current/userguide/compatibility.html, rendered for 9.9.1), the AGP/Gradle table (https://developer.android.com/build/releases/about-agp), and the AGP 9.0 release notes (https://developer.android.com/build/releases/agp-9-0-0-release-notes), or by reading the repository build files. Statements marked *[unverified]* come from memory or inference and must be confirmed during planning.

### 1. What the failure actually forces
- Running Gradle on Java 25 requires **Gradle 9.1.0 or later** *[verified: compatibility table row "25 | 9.1.0 | 9.1.0 and after"]*. Gradle 8.14.1 cannot be made to work on JDK 25. Any fix that keeps Gradle on JDK 25 therefore starts with a Gradle **major** upgrade.
- Gradle 9.x states "Gradle is tested with Android Gradle Plugin 9.0 through 9.5.0-alpha10" *[verified]*. **No AGP 8.x line appears on Gradle 9's tested list.** The AGP table only gives a *minimum* Gradle for each AGP line (8.7 needs 8.9 or later; 8.11 to 8.13 need 8.13 or later), not a maximum *[verified]*. So "AGP 8.x on Gradle 9.x" is **not an officially documented combination**. It may work in practice, but nothing confirms it.
- AGP 9.0 needs Gradle 9.1.0 or later, JDK 17 or later, KGP 2.2.10 or later (it auto-upgrades lower KGP), and SDK Build Tools 36.0.0 or later *[verified]*.

### 2. Central scoping decision: AGP 8.x + Gradle 9.x vs. AGP 9.x
| Option | What changes | Pros | Cons / risks |
|---|---|---|---|
| **A. Stay on AGP 8.x, move the wrapper to Gradle 9.x** | Wrapper plus the latest 8.x AGP (at least 8.13 for the Gradle 8.13 floor), with matching KGP/KSP/Hilt | Fewest DSL changes. Keeps `kotlin-android`, `kotlinOptions` and the legacy variant API. | **Unsupported combination** (section 1). The "smallest" move may turn out not to be viable: a failure in AGP internals on Gradle 9 has no supported fix path. The AGP 8.x line is end-of-line, so this option will need to be redone. |
| **B. Move to AGP 9.x + Gradle 9.x (at least the chosen AGP's minimum)** | Wrapper, AGP, KGP (2.2.10 or later), KSP, Hilt plugin and runtime, androidx.hilt, build-script DSL migration | The only **documented, tested** combination for Gradle on JDK 25. Gives the build a supported, current foundation. | Larger change. `android.builtInKotlin` and `android.newDsl` both default to `true` *[verified]*. The legacy variant API is removed *[verified]*. You no longer need to apply `kotlin-android` *[verified]*; whether applying it alongside built-in Kotlin is an error is *[unverified]*. |

**Recommendation: Option B (AGP 9.x).** Planning may optionally run a short, timeboxed Option A probe first, but only as a fallback. Option A's only advantage is a smaller diff, and that advantage disappears if it lands on an undocumented combination that later fails. This repository has few constructs that AGP 9 breaks (grep findings below), so Option B costs less here than it would in a typical Android build.

Pick the **lowest AGP 9.x and Gradle 9.x pair that satisfies the requirements**, not automatically the latest (AGP 9.5.1 / Gradle 9.9.1). Every version bump adds entries to `gradle/verification-metadata.xml` (547 `<component>` entries today). The smallest compatible pair keeps the checksum churn reviewable. Whether to jump to the latest is a separate currency decision.

### 3. "Use JDK 25": runtime only, or bytecode target too
- **Recommendation: Gradle (and javac/kotlinc) run on JDK 25. `sourceCompatibility`/`targetCompatibility`/`jvmTarget` stay at 17.** Raising the target is a separate, optional story at most.
- Trade-off. Raising the target to 25 gives the Android app nothing it can use at runtime: ART does not implement Java 25 platform APIs, and D8/R8 must desugar any newer bytecode or language features for `minSdkVersion 21`. Whether AGP 9.x's D8 accepts major-69 input at all is *[unverified]*. A raised target also adds a new compatibility axis (Robolectric, JaCoCo and ByteBuddy would have to read major-69 *app* classes) for no product benefit. The only gain would be newer Java language features in source.
- `k9mail-vendored/build.gradle` stays at Java 8 source/target (lines 76-77). Whether JDK 25's `javac` still accepts `-source 8`/`-target 8` (possibly with an "obsolete" warning) is *[unverified]* and must be checked. `app/build.gradle:202` sets `-Werror` for the app's JavaCompile. Planning must confirm whether that `-Werror` applies only to `:app` or also to the vendored module. If it reaches a Java 8 compile, an "obsolete source/target" warning would fail the build.

### 4. Architectural considerations found by reading the build files
1. **The JaCoCo gate depends on AGP-internal output paths.** `app/build.gradle` lines 263-270 and 309-316 hard-code `intermediates/classes/debugUnitTest/transformDebugUnitTestClassesWithAsm/dirs`, `intermediates/javac/debug/compileDebugJavaWithJavac/classes` and `tmp/kotlin-classes/debug`. These are AGP/KGP implementation details. Under AGP 9 built-in Kotlin, the Kotlin class output directory is likely to move *[unverified]*. **Highest-impact silent-failure risk:** if a path no longer exists, `fileTree` just returns an empty set, and the coverage report and verification rules can quietly cover fewer classes instead of failing. Planning should require evidence that the set of classes measured before and after the change is equivalent (e.g. the class count in the JaCoCo XML report), not just a green `jacocoTestCoverageVerification`. This protects AC-4's intent, not only its wording.
2. **Kotlin DSL surface.** `app/build.gradle:2` applies `kotlin-android` and lines 95-97 use `kotlinOptions { jvmTarget = '17' }`. Under AGP 9 built-in Kotlin, both must move to the built-in Kotlin configuration: probably `kotlin { compilerOptions { jvmTarget = ... } }`, though the exact DSL under built-in Kotlin is *[unverified]*. Opting out with `android.builtInKotlin=false` / `android.newDsl=false` is a valid **interim** step, but AGP 9.0 notes say the `newDsl` opt-out "will be removed in AGP 10.0". Whether AGP 10 has shipped as of today is *[unverified]*. Treat opt-outs as debt, not as the fix.
3. **Legacy variant API.** Grep of `app/build.gradle`, `k9mail-vendored/build.gradle` and `metadata/build.gradle` found no `applicationVariants`/`libraryVariants` usage. AGP 9's removal of the legacy variant API therefore probably does not affect this repository. Planning should confirm with a full grep that includes `settings.gradle`.
4. **Gradle 9 deprecations already present.** `rootProject.buildDir` (root `build.gradle` `clean` task) and `${buildDir}` (app, 7 sites), the `task name(type: ...)` syntax (root `clean`, app `jacocoTestReport`/`jacocoTestCoverageVerification`, `metadata/build.gradle:1`), and Groovy space-assignment (`namespace '...'`, `compileSdkVersion 35`). Whether any of these became *errors* (not just warnings) in Gradle 9.x is *[unverified]*. Planning should run `--warning-mode all` once and decide whether to fix them now or record them as debt. Any construct that is already an error must be fixed in this bug.
5. **Plugin stack moves as one unit.** KGP, KSP, Hilt Gradle plugin, `hilt-compiler`/`hilt-android`/`hilt-android-testing` (must be identical versions), `androidx.hilt:hilt-work` + `androidx.hilt:hilt-compiler`, and `kotlin-stdlib` (`app/build.gradle:115`) must form a mutually compatible set (IC-1). Which Hilt release first supports AGP 9 and built-in Kotlin, and which KSP release pairs with the chosen KGP (KSP2 versioning appears to have decoupled from KGP from 2.3.x onward), are both *[unverified]* and must be checked against the Dagger/Hilt and KSP release notes.
6. **Test runtime on JDK 25.** With no Java toolchain configured, unit tests run on the JVM Gradle uses, which is JDK 25. Robolectric (ASM instrumentation of `android-all`), Mockito/ByteBuddy (inline mock maker) and the JaCoCo agent (`toolVersion = "0.8.12"`, `excludes = ['jdk.internal.*']`) must each support a **Java 25 runtime**. This applies even though app bytecode stays at 17. The minimum versions supporting Java 25 for each are *[unverified]*. Expect all three to need bumps. Robolectric's supported `compileSdk`/SDK set must also still cover the SDKs that tests configure.
7. **Alternative not recommended: Java toolchain to JDK 17.** A `java { toolchain }` / `kotlin.jvmToolchain(17)` setup could keep compile and test on 17 while Gradle runs on 25. That would avoid item 6. But the user-global `~/.gradle/gradle.properties` sets `org.gradle.java.installations.auto-detect=false`, and its only listed installation is the JDK-25 JBR. A toolchain would therefore need either that machine-specific file to change or a toolchain resolver plugin (foojay) that downloads JDKs during the build. The second option adds a network dependency and a dependency-verification entry. It also contradicts the user's request ("use JDK 25") and AC-7 (no undocumented machine-specific setting).

### 5. Layer cascade (from the Impact Analysis)
The cascade in the Impact Analysis is correct, with one refinement: rows 1-3 are **not independently sequenceable**. Gradle 9 forces AGP 9 (section 1), AGP 9 forces KGP 2.2.10 or later, and KGP forces the KSP, Hilt and stdlib versions. The runtime JDK separately forces the test-tooling versions. Rows 1-6 therefore form a **single atomic change set**: no intermediate state builds on JDK 25. Rows 8-10 (CI, `.claude/CLAUDE.md`, engagement docs) depend on the outcome. Row 11 (`~/.gradle/gradle.properties`) cannot be fixed by a commit. It is best handled by a repository-tracked declaration that serves AC-7. Gradle's daemon-JVM criteria file (`gradle/gradle-daemon-jvm.properties`, with `./gradlew updateDaemonJvm`) is a candidate. Whether it takes precedence over a user-global `org.gradle.java.home`, and how it behaves when auto-detect is off, are *[unverified]*.

CI (`.github/workflows/ci.yml` lines 19-26 pin temurin 17, with the comment "AGP 8.x requires JDK 17"). Gradle 9 and AGP 9 still run on JDK 17 *[verified: Gradle 9 requires JVM 17 to 27; AGP 9.0 needs JDK 17 or later]*, so CI could stay on 17. That would leave CI exercising a different runtime JVM (and different Robolectric/JaCoCo/ByteBuddy code paths) than local builds. **Recommend moving CI to JDK 25 for parity.** If not, record the decision as AC-8 allows.

### 6. One story or several
- **One story for the fix**: wrapper, plugin stack, test tooling, DSL migration, verification metadata, CI and docs. The parts cannot be split into separately buildable stories (section 5). Complexity is likely higher than the current `medium` label suggests; planning should re-estimate.
- **Optional separate items, not part of this bug:**
  - (a) raising the bytecode target above 17 (not recommended, section 3);
  - (b) moving to the latest AGP/Gradle if a lower compatible pair was chosen;
  - (c) cleaning up Gradle-9 deprecation *warnings* that are not errors.
- **Optional pre-step (containment, not a fix):** locally point `org.gradle.java.home` back at `C:/Users/Michael.Horsley/.jdks/jbr-17.0.14`. That unblocks other pending work immediately while this story is in progress, and touches nothing in the repository.

### 7. Constraints and risks for planning
- **Dependency verification:** regenerating `gradle/verification-metadata.xml` on a large version jump is the step most likely to tempt a shortcut. AC-3 forbids disabling verification. The planner should require new entries to be produced by Gradle's metadata-writing mode from a trusted network, and reviewed as a diff, not hand-edited. The Gradle **wrapper** distribution is outside dependency verification. Consider adding `distributionSha256Sum` to `gradle-wrapper.properties` (supply-chain hardening, ISO 25010 Security/Integrity). Also regenerate `gradle-wrapper.jar` and `gradlew` via `./gradlew wrapper`, not by editing the URL only.
- **Coverage-gate integrity:** see section 4.1. This is a silent-failure mode, so a passing gate alone is not evidence.
- **Hilt/KSP codegen:** the `app/build.gradle` comments (lines 3-8) already flag mixed Java+Kotlin Hilt codegen under KSP as a known fragility with a kapt fallback. Under AGP 9 built-in Kotlin, kapt may only be available through `legacy-kapt`, whose processing behavior differs *[unverified; release-notes issue titles only]*. Treat the kapt fallback as **not available** unless planning proves otherwise.
- **`gradle.properties` JVM args:** the `SelectorProvider`/`unixdomain.tmpdir` daemon flags (line 9) are Windows workarounds. Re-validate them on JDK 25. If they are no longer needed, document why rather than deleting them silently.
- **SDK Build Tools 36.0.0 or later** is a new local and CI prerequisite under AGP 9 *[verified]*. CI's Android SDK setup must provide it.
- **Behavior preservation:** `compileSdk`/`targetSdk` 35 and the U-059 R-class flags are not changed by this approach. The AGP 9 changes are build-time only, so the APK smoke test on API 36 (AC-5) is the main runtime safety net.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `gradle/wrapper/gradle-wrapper.properties` | Pins Gradle 8.14.1 | TBD in planning (candidate: newer Gradle supporting JDK 25) |
| `build.gradle` | Pins AGP 8.7.3, Kotlin 2.0.21, KSP 2.0.21-1.0.28, Hilt plugin 2.51.1 | TBD in planning |
| `app/build.gradle` | Java/jvmTarget 17; JaCoCo 0.8.12; Robolectric 4.12.2; Mockito 5.14.2; Hilt 2.51.1 | TBD in planning |
| `k9mail-vendored/build.gradle` | Java 8 source/target | TBD in planning |
| `gradle/verification-metadata.xml` | Checksums for current dependency set | Add checksums for any changed dependency |
| `gradle.properties` | JVM args, JDK 17 comments | TBD in planning |
| `.github/workflows/ci.yml` | JDK 17 temurin | TBD (update or record decision) |
| `.claude/CLAUDE.md` | JAVA_HOME = JBR 17 instruction | Update to match actual build JDK |
| `~/.gradle/gradle.properties` (outside repo) | `org.gradle.java.home` = JBR (JDK 25) | Document; not a repository change |

## Existing Behavior to Preserve
- Dependency verification remains enforced via `gradle/verification-metadata.xml`.
- Per-package JaCoCo LINE >= 70% floors in `app/build.gradle` are not lowered.
- All currently passing unit tests continue to pass; the test count does not decrease.
- `compileSdk`/`targetSdk` 35 and runtime behavior of the app are unchanged; the debug APK installs and launches on an API 36 emulator.
- AGP-8 defaults introduced in U-059 (`android.nonTransitiveRClass=true`, `android.nonFinalResIds=true`, jetifier removed) remain in effect.
- Local `adb` guidance (`MSYS_NO_PATHCONV=1` scoped to adb-only shells; not set for Gradle) remains valid.

## Verification Steps
1. (AC-1) In a clean Git Bash shell without `MSYS_NO_PATHCONV`, confirm the JVM Gradle uses is JDK 25 (`./gradlew --version`, check the "Launcher JVM"/"Daemon JVM" line), then run `./gradlew :app:assembleDebug`. Confirm exit code 0 and no "Unsupported class file major version 69".
2. (AC-2) Run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`. Confirm all succeed. Compare `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"` with the baseline recorded before the change.
3. (AC-3) Inspect that `gradle/verification-metadata.xml` is still present and enforcing; confirm no verification-off flags in build files, `gradle.properties`, CI, or documented commands; confirm the build passes without any flag that disables verification.
4. (AC-4) Diff `app/build.gradle` and confirm no threshold in the jacoco verification rules decreased.
5. (AC-5) Install the debug APK on an API 36 emulator (`adb install -r app/build/outputs/apk/debug/app-debug.apk`), launch the main activity, and confirm no crash in `adb logcat`.
6. (AC-6, AC-7) Read `.claude/CLAUDE.md` and `sdlc/artifacts/engagement/code-location.md`; confirm the JDK guidance matches the JDK reported in step 1 and addresses the `org.gradle.java.home` vs `JAVA_HOME` precedence.
7. (AC-8) Review `.github/workflows/ci.yml` and confirm the JDK and Gradle combination is consistent with the fix, or that a recorded decision exists for keeping JDK 17.

## Root Cause Analysis
<!-- Filled during planning phase -->

## Technical Context
- Class file major version 69 = Java 25.
- Gradle wrapper: `gradle-8.14.1-bin.zip`. Per Gradle's compatibility matrix, running Gradle on Java 25 is believed to require Gradle 9.1.0 or later. This was not re-verified against the Gradle documentation; confirm during planning.
- Toolchain pins: AGP 8.7.3 (`build.gradle:7`), Kotlin Gradle plugin 2.0.21 (`build.gradle:10`), KSP 2.0.21-1.0.28 (`build.gradle:14`), Hilt Gradle plugin 2.51.1 (`build.gradle:18`); app: JaCoCo 0.8.12 (line 211), Robolectric 4.12.2 (159), mockito-core 5.14.2 (162), kotlin-stdlib 2.0.21 (115), Hilt 2.51.1 and androidx.hilt 1.2.0 (181-186), `compileOptions`/`jvmTarget` 17 (89-97), compileSdk/targetSdk 35.
- `gradle/verification-metadata.xml` (4048 lines, 546 components) enforces dependency verification.
- Latest stable versions observed in Maven metadata on 2026-11-11 (informational only; NOT a decision or recommendation): Gradle 9.9.1; AGP 9.5.1; Kotlin 2.4.21; KSP 2.3.13; Hilt 2.60.1; androidx.hilt 1.4.0; Robolectric 4.17; JaCoCo 0.8.16; Mockito 5.24.0; coroutines 1.11.0; GreenMail 2.1.15.
- The user-global `~/.gradle/gradle.properties` `org.gradle.java.home` setting overrides `JAVA_HOME`.
- The user's request, verbatim: "update the app to use JDK 25".
- Observed 2026-11-11 on branch `sdlc/modernization-plan` @ b09bda5d (Windows 11, Git Bash). Last successful build: 2026-06-24.

## Supporting Documentation
- Related story: U-057 (raised Java target to 17; toolchain currency).
- `.claude/CLAUDE.md` section "Local Build & Device Notes (Windows / Git Bash)".

## Implementation Notes
<!-- Added by agents during build -->

## Review Findings
<!-- Summarized from workspace artifacts -->

## Notes
Ambiguity for planning (raised, not resolved here): the request "use JDK 25" could mean (a) run Gradle on JDK 25 only, or (b) also raise `sourceCompatibility`/`jvmTarget` to 25. Expected Behavior above is scoped to (a), as stated in the reproduction details; Scope Concern: whether to also raise the compile target is a planning decision.
