---
artifact_type: plan
story_id: "U-060"
gate_id: "planning"
checkpoint: "plan-gate"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-11"
contracts_verified: []
risks_identified: 16
---

# Implementation Plan: U-060 — Run the build on JDK 25 (Gradle 9.x / AGP 9.x) keeping the Java 17 bytecode target

## Verdict: PASS

**Revision 2.** This revision answers plan-review iteration 1 (`sdlc/artifacts/build/sprints/sprint-017/U-060/plan-review.md`, FAIL, B-1..B-3, N-1..N-7). It is a targeted edit of the plan committed at `2334fc6d`. The Revision Log at the end lists every change.

The story can be planned and implemented as written. Every version decision below is backed by an official source that was fetched or re-fetched on 2026-11-11. Three items could not be proven without building. Each is a numbered Developer checkpoint, and each has a defined outcome:
- androidx.hilt 1.2.0 under KSP2: Checkpoint B1. If it fails, the **pre-approved minSdk-23 branch (Branch M)** is taken.
- The AGP 9 built-in-Kotlin class-output path: Checkpoint B5.
- `--system` propagation into `hiltJavaCompileDebug`: Checkpoint B-javac.

Scope Concerns for the orchestrator (none of them blocks implementation):
- **SC-1:** corrects the story's premise.
- **SC-2:** records the product-owner minSdk decision and the branch it unlocks.
- **SC-5:** records how "documented as supported" (AC-11) is read, given that no stable triple is listed by all three vendors.

**Version change in this revision:** the Gradle wrapper target moves from **9.3.1 to 9.5.1**. The plan reviewer judged 9.3.1 defensible, but the corrected compatibility data reverses that conclusion. AC-11 names two sources, the Gradle compatibility page and the AGP release notes. Gradle 9.5.x is the lowest line on which both of them list this exact stack (AGP 9.0, Kotlin 2.3.20 line, JDK 25) **at stable versions**. On 9.3.x, the Gradle page lists AGP 9.0 and Kotlin 2.3 only as pre-releases, which "may or may not work". The full argument is in SC-5. Everything else in the version set is unchanged.

---

## Story Overview

Gradle on this machine runs on JDK 25: the user-global `org.gradle.java.home` points at the Android Studio JBR 25.0.4. The pinned toolchain (Gradle 8.14.1, AGP 8.7.3, KGP 2.0.21) cannot compile build scripts on class-file major version 69. The story moves Gradle, javac, kotlinc and the unit-test JVM to JDK 25 as one atomic change. App bytecode stays at 17 and the vendored module stays at 8. compileSdk/targetSdk 35, dependency verification, JaCoCo floors and the test count must all be preserved.

**Why it is one change set:**
- Gradle 9 is required to run on JDK 25, so AGP 9 is required (per AC-11), which forces KGP ≥ 2.2.10.
- Built-in Kotlin then forces KSP ≥ 2.3.1, which is KSP2-only. KSP2 in turn forces the Hilt and androidx.hilt choices.
- Separately, the JDK 25 test runtime forces the Robolectric, Mockito/ByteBuddy and JaCoCo versions.

No intermediate state builds on JDK 25.

---

## Version Decisions (the compatible set)

All sources were fetched on 2026-11-11. "Lowest" means the lowest documented-compatible **minor line**, at the latest **patch** of that line. Patch releases are bug-fix-only within a line, and the vendors' own tables state compatibility per line (AC-15 rationale). Quotes from vendor pages below are verbatim.

| Component | From | **To** | Why this version (and why not lower or higher) | Source |
|---|---|---|---|---|
| Gradle wrapper | 8.14.1 | **9.5.1** (bin; `distributionSha256Sum=bafc141b619ad6350fd975fc903156dd5c151998cc8b058e8c1044ab5f7b031f`) | JDK 25 needs Gradle ≥ 9.1.0. The Java table reads "25 / 9.1.0 / 9.1.0 and after". The versioned compatibility pages say, verbatim (each page adds "Alpha and beta versions may or may not work" for AGP and "Beta and RC versions may or may not work" for Kotlin):<br>• 9.1.0: "Gradle is tested with Android Gradle Plugin 8.4 through 8.12" / "Kotlin 2.0.0 through 2.2.20-Beta2"<br>• 9.2.1: AGP "8.4 through 8.13" / Kotlin "2.0.0 through 2.2.20"<br>• 9.3.0, 9.3.1: AGP "**8.13 through 9.0.0-beta02**" / Kotlin "**2.0.0 through 2.3.0-RC**"<br>• 9.4.0, 9.4.1: AGP "8.13 through 9.1.0-alpha04" / Kotlin "2.0.0 through 2.3.20-Beta1"<br>• **9.5.0, 9.5.1: AGP "9.0 through 9.2.0-alpha05" / Kotlin "2.0.0 through 2.3.20"**<br>So **9.5.x is the first Gradle line whose page lists stable AGP 9.0 and stable Kotlin 2.3 as tested**. Its range "9.0 through 9.2.0-alpha05" literally contains AGP 9.0.1. Gradle 9.5 also *embeds* Kotlin 2.3.20 ("The embedded Kotlin has been upgraded from 2.3.10 to Kotlin 2.3.20", upgrading_version_9). 9.5.1 is the latest 9.5 patch. 9.6.x and later are not needed (AC-15). The choice over 9.3.1 is argued in SC-5. | docs.gradle.org/{9.1.0,9.2.1,9.3.0,9.3.1,9.4.0,9.4.1,9.5.0,9.5.1}/userguide/compatibility.html; docs.gradle.org/9.5.1/userguide/upgrading_version_9.html; services.gradle.org/distributions/gradle-9.5.1-bin.zip.sha256; wrapper jar sha256 `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7` (gradle-9.5.1-wrapper.jar.sha256) |
| AGP | 8.7.3 | **9.0.1** | The lowest AGP 9 line. AC-11 requires AGP 9; see SC-1 for the AGP 8.13 alternative. AGP 9.0 requires Gradle ≥ 9.1.0, JDK ≥ 17, KGP ≥ 2.2.10 and Build Tools ≥ 36.0.0, and supports max API 36.1. 9.0.1 is the latest 9.0 patch (dl.google.com metadata: `9.0.0 9.0.1`). It is listed in the Gradle 9.5.x tested range. | developer.android.com/build/releases/agp-9-0-0-release-notes ("Compatibility" table); dl.google.com/android/maven2/com/android/tools/build/gradle/maven-metadata.xml |
| Kotlin Gradle plugin | 2.0.21 | **2.3.21** | AGP 9.0 needs ≥ 2.2.10. The Gradle 9.5 page lists Kotlin "through 2.3.20". The Kotlin table groups **"2.3.20–2.3.21"** as one row (Gradle 7.6.3–9.3.0, AGP 8.2.2–9.0.0), so the vendor itself states that 2.3.21 has the same compatibility as 2.3.20. 2.3.21 is the bug-fix patch of that line and contains a JVM-relevant compiler revert ("KT-85405 Postpone/Revert `DontIgnoreUpperBoundViolatedOnImplicitArguments`"). **KGP 2.4.x is deliberately excluded:** Hilt 2.59.2 fails on Kotlin 2.4 metadata ("Provided Metadata instance has version 2.4.0, while maximum supported version is 2.3.0", google/dagger #5177/#5190). The fix is in Dagger 2.60, which needs minSdk 23. Kotlin 2.3.0 adds "Kotlin/JVM: Support for Java 25". KSP 2.3.x is built against KGP 2.3.0. | kotlinlang.org/docs/gradle-configure-project.html (KGP table); github.com/JetBrains/kotlin/releases/tag/v2.3.21; kotlinlang.org/docs/whatsnew23.html; github.com/google/dagger/issues/5177, /5190 |
| kotlin-stdlib | 2.0.21 | **2.3.21** | IC-1: matches KGP. | — |
| KSP | 2.0.21-1.0.28 | **2.3.6** | "Added support for AGP 9.0 and built-in Kotlin" first appears in **KSP 2.3.1**; 2.2.x does not have it. 2.3.6 is chosen over 2.3.2 because it "Fixed an issue where module recompilation would fail on Windows environments when KSP2 was enabled (#2774)" and "Improved the detection of built-in Kotlin" (#2772). Both matter on this Windows build. KSP 2.3.x is KSP2-only (KSP1 deprecated in 2.3.0), and 2.3.6 is built against `kotlinBaseVersion=2.3.0` / `agpBaseVersion=9.0.0`. No KSP release from 2.3.7 to 2.3.13 mentions a Gradle 9.4/9.5 incompatibility (release-note scan). | github.com/google/ksp/releases (2.3.0, 2.3.1, 2.3.2, 2.3.6, 2.3.7–2.3.13); raw.githubusercontent.com/google/ksp/2.3.6/gradle.properties |
| Hilt (plugin, `hilt-android`, `hilt-compiler`, `hilt-android-testing`; all identical) | 2.51.1 | **2.59.2** | **Dagger 2.59** is the first release with AGP 9 support in the Hilt Gradle plugin, and it *requires* AGP 9 ("along with AGP 9's own requirements like Gradle 9.1+"). 2.59.1 fixed jetifier-on-AGP-9. 2.59.2 replaced the "android-classes" transform and fixed slow incremental builds from HiltSyncTask on AGP 9. Its kotlin-metadata-jvm reads metadata up to **2.3.0** (#5177 error text), which covers Kotlin 2.3.21. **2.60 is excluded at minSdk 21: "Multidex support is removed. minSDK for Hilt is now 23, matching AndroidX"**. hilt-android 2.59.2's AAR manifest says minSdkVersion 14. 2.60.1 is the pre-specified version *only* inside Branch M when R11 triggers. | github.com/google/dagger/releases (2.58–2.60.1); repo1 POMs/AAR for 2.59.2 |
| androidx.hilt (`hilt-work`, `hilt-compiler`) | 1.2.0 | **1.2.0 (kept), conditional on Checkpoint B1** | 1.3.0 is the line documented for KSP2 ("Updates the library and annotation processor to target Kotlin 2.0 to support newer Kotlin toolchain including KSP2", 1.3.0-alpha01). **`hilt-work-1.3.0.aar` declares `minSdkVersion="23"`**, and 1.4.0 does too (AAR manifests checked). 1.2.0 (room-compiler-processing 2.6.0, KSP API 1.9.22, kotlin-stdlib 1.8.22) is kept to preserve minSdk 21 and must pass Checkpoint B1. **If B1 fails, Branch M (minSdk 23 + androidx.hilt 1.3.0) is the pre-approved outcome** (SC-2). | developer.android.com/jetpack/androidx/releases/hilt; hilt-work-{1.3.0,1.4.0}.aar `AndroidManifest.xml`; hilt-compiler-{1.2.0,1.3.0,1.4.0}.pom |
| JaCoCo | 0.8.12 | **0.8.14** | "JaCoCo now officially supports Java 25" first appears in 0.8.14. 0.8.13 had only experimental support. | jacoco.org/jacoco/trunk/doc/changes.html |
| Robolectric | 4.12.2 | **4.15.1** | **4.16 "removes support for Android L (SDK 21 and 22)"**, and `SmsReceiverTest` configures `sdk = Build.VERSION_CODES.LOLLIPOP` (4 uses). 4.15.x is therefore the newest line that covers the configured SDK set {21, 23, 29}, which satisfies AC-10. 4.15 moved to **ASM 9.8**, the first ASM with `Opcodes.V25`. 4.12.2 uses ASM 9.7 (Java 23 max). 4.16.1 is the pre-specified version *only* inside Branch M when R8 triggers. | github.com/robolectric/robolectric/releases (4.15, 4.15.1, 4.16); sandbox-4.15.1.pom (asm 9.8); asm.ow2.io/versions.html; repo1 maven-metadata |
| Mockito (`mockito-core`) | 5.14.2 | **5.18.0** | The lowest Mockito that bundles **ByteBuddy 1.17.5** ("Update ASM to version 9.8 to support Java 25"). 5.14.2 bundles 1.15.4 (Java 24). 5.15.2–5.17.0 bundle 1.15.11. | mockito-core-{5.14.2..5.20.0}.pom; raphw/byte-buddy release-notes.md |
| SDK Build Tools | (AGP default) | **36.0.0** (AGP 9.0 minimum and default) | Installed locally (`Sdk/build-tools/36.0.0`) and on the GitHub `ubuntu-22.04` image ("36.0.0 36.1.0"). | AGP 9.0 notes; actions/runner-images Ubuntu2204-Readme.md |
| Daemon JDK declaration | none | **`gradle/gradle-daemon-jvm.properties` with `toolchainVersion=25`** | "If the Daemon JVM criteria configuration is provided, it takes precedence over JAVA_HOME and org.gradle.java.home." (9.5.1 docs). This fixes the root cause (an undeclared, user-global JDK) in a repository-tracked way (AC-7). `updateDaemonJvm --jvm-version=…` and the `toolchainPlatforms = []` rule are unchanged in 9.5.1. | docs.gradle.org/9.5.1/userguide/gradle_daemon.html |
| CI JDK | temurin 17 | **temurin 25** | Gives parity: the same Robolectric, ByteBuddy and JaCoCo code paths as local builds. The runner image ships `JAVA_HOME_25_X64`, and `actions/setup-java@v4` accepts `java-version: '25'`. | runner-images readme; setup-java README |

**Unchanged on purpose:** coroutines 1.7.3, lifecycle 2.6.2, work 2.9.1, truth, junit 4.13.2, greenmail 2.1.3, `auto-service`, billing, security-crypto, and the k9mail-vendored dependencies. None of them is forced by the toolchain, and moving them would widen the verification diff (AC-15 spirit).

**Gradle 9.4/9.5 upgrade-guide items checked against this build** (docs.gradle.org/9.5.1/userguide/upgrading_version_9.html):
- **"System property priority for Wrapper execution has changed" (9.4).** The order is now strictly command line > `~/.gradle/gradle.properties` > project `gradle.properties`; previously "properties defined in gradle.properties files could unexpectedly override those passed via the command line". This makes the precedence documented for AC-6 true on the chosen Gradle.
- **"The Windows start script has been reworked" (9.5).** It affects the newly tracked `gradlew.bat` content only. Git Bash uses `gradlew`.
- **"Deprecation of methods taking Closure on Test tasks" (9.4).** Deprecations only, so they are AC-16 debt if reported.
- **Not applicable:** precompiled settings plugins, armored key rings (`verify-signatures` is false), and lockfiles. None is used.

---

## Scope Concerns (for orchestrator / product owner)

- **SC-1 (premise correction, non-blocking).** The story's Technical Context and AC-11 say "Gradle 9.x is tested only with AGP 9.x". That is only true of Gradle **9.5 and later**; the versioned pages are quoted verbatim in the Version Decisions table:
  - Gradle 9.1.0 is "tested with Android Gradle Plugin 8.4 through 8.12".
  - 9.2.1 is tested with "8.4 through 8.13".
  - 9.3.x is tested with "8.13 through 9.0.0-beta02", and 9.4.x with "8.13 through 9.1.0-alpha04".

  A literally documented AGP-8 option exists on the Gradle page: **Option A** = Gradle 9.2.1 + AGP 8.13.x + KGP 2.2.20 (9.2.1: "Kotlin 2.0.0 through 2.2.20"). Its KGP row (2.2.20–2.2.21: Gradle ≤ 8.14, AGP ≤ 8.11.1) does not cover it, so it has the same Kotlin-table gap as the selected set. Option A could keep KSP1 (KSP 2.2.x) and avoid the androidx.hilt/KSP2 question. **This plan still selects AGP 9.0.1, as AC-11 requires by default:**
  - AC-11 accepts AGP-8-on-Gradle-9 only with a reason. The one advantage of Option A, avoiding the androidx.hilt/KSP2 risk, now has a product-owner-approved resolution (SC-2), so the reason no longer exists.
  - Option A leans on KSP1, which KSP deprecated in 2.3.0.
  - 8.13 is the final 8.x line, and Hilt ≥ 2.59 requires AGP 9. AGP 9's notes say its opt-outs are removed in AGP 10. Option A would have to be redone soon.

  Record this correction in Implementation Notes. No AC change is required.

- **SC-2 (product-owner decision recorded; pre-approved contingency, non-blocking).** Product-owner input, relayed by the orchestrator during plan review, verbatim: **"I'm good raising the minsdk version"**. The plan applies it as the reviewer recommended (option a): **permission, not a mandate.** Android 5.0/5.1 support is kept wherever keeping it costs nothing risky:
  1. **Checkpoint B1** tries androidx.hilt 1.2.0 under KSP2 (cheap, fails loudly at build time).
  2. If B1 fails (R1), or if R11 or R8 triggers in a way only a minSdk raise fixes, the Developer **takes Branch M** (minSdk 23 + androidx.hilt 1.3.0; fully specified below). There is **no stop-and-escalate**, because the decision exists.
  3. **No intermediate "Fallback B2" is attempted.** Iteration 1's B2 forced a newer `androidx.room:room-compiler-processing` onto the processor path of androidx.hilt-compiler 1.2.0. It is dropped for three reasons:
     - (i) The reviewer showed that `resolutionStrategy.force` on the declarable `ksp*` buckets does not reach the resolved `ksp…ProcessorClasspath` configurations, so B2 as written was a no-op (N-1).
     - (ii) A working version, a `constraints { … strictly … }` block, would still aim at the wrong component. **androidx.hilt-compiler 1.3.0, the line documented for KSP2, ships the *same* `room-compiler-processing 2.6.0` as 1.2.0** (hilt-compiler-1.3.0.pom). Its KSP2 change was "target Kotlin 2.0". Forcing XProcessing 2.7+ under 1.2.0 is therefore not what upstream did, and no vendor documents that combination.
     - (iii) It is the same category as the mixed-version idea the reviewer rejected in N-2. A minSdk raise that is documented, pre-approved and fully specified is better-supported evidence for AC-11/IC-1 than an undocumented processor-classpath override.

  If Branch M triggers, the story's "app runtime behavior unchanged" scope statement and "Existing Behavior to Preserve" list need a **story amendment** by the Requirements Analyst (see Branch M, M.10). The Developer does not edit the story's ACs.

- **SC-3 (pre-existing defect found during planning).** On this machine, HEAD **fails dependency verification on JDK 17 too**. `./gradlew help -Dorg.gradle.java.home=<JBR17>` reports "2 artifacts failed verification: junit-bom-5.9.2.module, junit-bom-5.9.3.module (configuration 'classpath')". The metadata holds only the `.pom` checksums for those two versions.
  - This does not change the plan: the regeneration in Step 6 covers it if those modules are still in the graph.
  - It does affect how the baseline is captured (Step 0.4), and it must be recorded in Implementation Notes as a pre-existing gap.

- **SC-4 (informational).** The user-global `~/.gradle/gradle.properties` sets its own `org.gradle.jvmargs`. User-home properties take precedence over project properties, so **on this machine the repository's `org.gradle.jvmargs` (the AC-18 flags) are never applied**.
  - AC-18 revalidation must therefore use a scratch Gradle user home (Step 9).
  - This interaction belongs in the AC-6 documentation.

- **SC-5 (AC-11 / AC-15 interpretation; non-blocking, product-owner acknowledgement advisable).**
  - **Fact.** No Gradle/AGP/KGP triple is listed **at stable versions by all three vendors at once**, on any Gradle line from 9.1 to 9.7. Each vendor's tested ranges trail the others' releases:

    | Gradle line | AGP on the Gradle page | Kotlin on the Gradle page |
    |---|---|---|
    | 9.3.x | stable AGP 9.0 absent ("through 9.0.0-beta02") | "through 2.3.0-RC" |
    | 9.5.x | "9.0 through 9.2.0-alpha05" | "through 2.3.20" |
    | 9.6.x | "9.0 through 9.3.0-alpha06" | "through 2.4.0-RC" |
    | 9.7.x | "9.0 through 9.4.0-alpha03" | "through 2.4.20-Beta1" |

    The KGP table works the other way: 2.3.20–2.3.21 covers Gradle ≤ 9.3.0, 2.4.0–2.4.10 covers Gradle ≤ 9.5.0, and 2.4.20–2.4.21 covers Gradle ≤ 9.7.0.
  - **Reading applied.** AC-11 asks for a combination "documented as supported by **the official Gradle compatibility page and AGP release notes**". The plan treats those two sources as binding and requires a **literal** match in both:
    - The Gradle page must list the chosen AGP and Kotlin **stable** lines as tested, and must list JDK 25 for running Gradle.
    - The AGP notes' minimums must be met: Gradle ≥ 9.1.0, KGP ≥ 2.2.10, JDK ≥ 17, Build Tools ≥ 36.0.0.

    The Kotlin table is not named by AC-11. It is used as a **secondary** check. Above its "fully supported" maximum it states: "You can also use Gradle and AGP versions up to the latest releases, but if you do, keep in mind that you might encounter deprecation warnings or some new features might not work".
  - **Result.**
    - **Gradle 9.5.1 + AGP 9.0.1 + KGP 2.3.21** satisfies both AC-11 sources literally. AGP 9.0.1 falls inside "9.0 through 9.2.0-alpha05". Kotlin 2.3.21 falls in the "2.3.20–2.3.21" row that the Kotlin vendor itself groups, and that row is the line Gradle 9.5 lists "through 2.3.20" and embeds. JDK 25 is "9.1.0 and after".
    - 9.3.1 would satisfy the Gradle page only by reading "tested with … 9.0.0-beta02" as covering 9.0.x stable, and "2.3.0-RC" as covering 2.3.x. The same page says of such pre-releases that they "may or may not work". That is a weaker claim than "documented as supported".
    - **AC-15:** under this reading the lowest pair that satisfies AC-11 is Gradle 9.5.x / AGP 9.0.x, at the latest patch 9.5.1. 9.4.x lists AGP 9.0 only up to "9.1.0-alpha04" as a range that starts at 8.13, and Kotlin "2.3.20-Beta1", so it has the same pre-release-only problem as 9.3.x.
    - **The lowest Gradle (9.1.0) is not chosen** because its page tests AGP only to 8.12.
  - **Why not a 2.4-based triple** (e.g. Gradle 9.6/9.7 + KGP 2.4.20, which the KGP table covers up to Gradle 9.7.0): the Gradle pages list Kotlin 2.4 only as RC/Beta. More importantly, Hilt 2.59.2 cannot read Kotlin 2.4 metadata (#5177/#5190), so it would force Dagger 2.60, and with it minSdk 23, for no compatibility gain.
  - **Residual risk (R16).** KGP 2.3.21 runs on Gradle 9.5.1, which is 2 minors above the KGP table's "fully supported" Gradle maximum (9.3.0). The Kotlin vendor's stated consequence is "deprecation warnings or some new features might not work". This is mitigated in three ways:
    - Gradle's own CI tests Kotlin 2.3.20 on 9.5.
    - Gradle 9.5 embeds Kotlin 2.3.20.
    - This project uses only the built-in-Kotlin JVM path (no KMP, no newer KGP features).

    Detection: Checkpoint A/B and `--warning-mode all` (AC-16). Any KGP-originated *error* is a stop condition (Step 3.2 (ii)); warnings are debt.
  - **Decision boundary.** This is a planner interpretation, and the iteration-1 reviewer considered it a planner correction. Product-owner acknowledgement of SC-5 is advisable because AC-11 is worded strictly. It does not block implementation. Copy SC-5 verbatim into Implementation Notes as the AC-11/AC-15 evidence.

The story fits one sprint item; no split is needed.

---

## Acceptance Criteria Mapping

| AC | Implementation step(s) | Evidence the Developer records |
|----|-------------------|-------|
| AC-1 | Steps 1–5, 10 | `./gradlew --version` output (Gradle 9.5.1; Launcher and Daemon JVM 25, "from gradle/gradle-daemon-jvm.properties"); `assembleDebug` exit 0; grep shows no "major version 69" |
| AC-2 | Step 0.2/0.5, Step 7 | `@Test` count ≥ 697 (baseline confirmed at HEAD during planning); JUnit-XML totals (via init script) with failures=0, errors=0, executed ≥ baseline; the test-source diff rule of Step 7.5 (empty, or exactly the Branch M retarget) |
| AC-3 | Step 6, Step 11 | grep for `dependency-verification\|verify-metadata` across build files, properties, CI and docs; enforced run green; additions-only script output |
| AC-4 | Step 3.4, Step 11 | `git diff HEAD -- app/build.gradle` shows every `minimum =` and `includes =` line unchanged; no new `excludes` entries in `jacocoFileFilter` |
| AC-5 | Step 10 | adb install/launch transcript; `pidof`; `logcat` grep shows no `FATAL EXCEPTION`; screenshot |
| AC-6 | Step 12 | `.claude/CLAUDE.md` edit routed through the Engine Librarian; `code-location.md` "Build Environment" section (wording conditional on M2/M4) |
| AC-7 | Step 4, Step 9 | criteria file; scratch-user-home matrix (4 runs) proving precedence and auto-detect=false behavior |
| AC-8 | Step 8 | `ci.yml` diff (JDK 25, Build Tools 36.0.0 step, corrected comment) |
| IC-1 | Step 2; Branch M.4 if triggered | Version table in Implementation Notes; `:app:dependencies` excerpt showing a single Hilt version and stdlib 2.3.21 |
| IC-2 | Step 2.3, Step 7.3 | vendored module still `VERSION_1_8`; `javap` major 52; `-Werror` scope proof |
| IC-3 | Step 2 | grep shows zero `kotlin-android`, `kotlinOptions`, `buildDir` left in the 4 build files |
| AC-9 | Step 0.6, Step 7.2 | per-package `<class>` counts, baseline vs after (script output), class-root guard |
| AC-10 | Step 2.2, Step 7 | versions table; test totals; no ASM/ByteBuddy/JaCoCo/class-version errors in the test log; configured SDK set {21, 23, 29} (or {23, 29} under Branch M) all within the Robolectric version's range |
| AC-11 | Version Decisions table, SC-1, SC-5 | verbatim citations and the SC-5 reading copied into Implementation Notes |
| AC-12 | Step 7.3 | `javap -v` major 61 (one Java and one Kotlin app class), 52 (vendored) |
| AC-13 | Step 6 | writer-mode provenance, additions-only script, enforced re-run |
| AC-14 | Step 1, Step 5 | `./gradlew wrapper ...` transcript; `distributionSha256Sum`; wrapper jar sha256 = official |
| AC-15 | Version Decisions table, SC-5 | rationale copied into Implementation Notes |
| AC-16 | Step 7.4 | `--warning-mode all` log; error-class items fixed; the rest listed as debt |
| AC-17 | Step 2 (no opt-outs) | statement "no AGP opt-out flags introduced" plus grep evidence |
| AC-18 | Step 9 | scratch-user-home run with repo jvmargs on JDK 25; retain/remove decision per flag; comments updated |
| AC-19 | Step 2.5, Step 11 | grep: `compileSdkVersion 35`, `targetSdkVersion 35`, both R flags present, no `enableJetifier`, no `org.gradle.java.home` in repo. AC-19 does not cover minSdk, so Branch M does not affect it |

**ACs touched if Branch M triggers:**
- The story's scope statement ("app runtime behavior … unchanged") and the "Existing Behavior to Preserve" bullet both need the amendment in M.10.
- AC-2: the 4 tests are retargeted, not disabled or deleted, so the count stays 697. The evidence rule changes as in Step 7.5.
- AC-10: the configured SDK set becomes {23, 29}.
- AC-15: the androidx.hilt rationale changes.
- IC-1: androidx.hilt 1.3.0 must be shown to be compatible with Hilt 2.59.2 (M.4).

AC-5 and AC-19 are unaffected.

---

## Implementation Steps

Shell rules for every step: use Git Bash with **no `MSYS_NO_PATHCONV`** for Gradle, and run `unset MSYS_NO_PATHCONV` first. Use the repo root `C:/Code/Android/sms-backup-plus`. Keep every evidence file under `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/`, except scratch copies, which go to the session scratchpad or `C:/Code/u060-*`.

### Shared definitions: rollback path lists (used by Step 3, Branch M, Checkpoint C)

These two lists are the **only** paths any rollback command in this plan may touch. They never include `sdlc/**` or `.claude/**`. Pipeline state, including the tracked story file `sdlc/artifacts/build/sprints/sprint-017/U-060-jdk-25-build-toolchain-upgrade.md` with its frontmatter and Implementation Notes, and the evidence directory, must survive every rollback.

```bash
# Tracked files this plan may modify (main path + Branch M + Step 7.4 conditional edits):
U060_TRACKED="build.gradle settings.gradle gradle.properties gradlew \
  gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.jar \
  gradle/verification-metadata.xml \
  app/build.gradle app/proguard-rules.pro app/lint-baseline.xml \
  app/src/test/java/com/zegoggles/smssync/compat/SmsReceiverTest.java \
  k9mail-vendored/build.gradle metadata/build.gradle \
  .github/workflows/ci.yml CHANGES"
# New (untracked) files this plan may create:
U060_NEW="gradle/gradle-daemon-jvm.properties gradlew.bat"
```

**Rollback procedure (at any checkpoint).** Nothing is committed, so a rollback is:
1. Stop daemons **while the wrapper still points at 9.5.1**. The restored 8.14.1 wrapper cannot run reliably on JDK 25. Run `./gradlew --stop`.
2. Run `git restore --source=HEAD --staged --worktree -- $U060_TRACKED`. These are explicit paths only; **never `-- .`**.
3. Run `rm -f -- $U060_NEW`.
4. Run `git status --short -- . ':!sdlc' ':!.claude'` and confirm the output is empty. `sdlc/**` and `.claude/**` are excluded on purpose and must still show their pipeline edits.
5. `sdlc/artifacts/engagement/code-location.md` is under `sdlc/**`, so it is **never restored by command**. It is edited only in Step 12, after Checkpoint C. If a full rollback is required after Step 12, reverse the Step 12.1 edits by hand with the Edit tool, using `git diff HEAD -- sdlc/artifacts/engagement/code-location.md` as the reference. Never use `git restore` or `git checkout` on it.
6. The `.claude/CLAUDE.md` change (Step 12.2) belongs to the Engine Librarian. A rollback requests its reversal from the Engine Librarian; the Developer never touches it.

The baseline tree at `C:/Code/u060-baseline` is unaffected. To keep partial work instead, use `git stash push -m u060-wip -- $U060_TRACKED`, with the same explicit list and never a bare `git stash`. Untracked `U060_NEW` files are not stashed by that command; copy them to the scratchpad first.

### Step 0 — Baseline capture BEFORE any change (AC-2, AC-9, SC-3)

0.1 **Record the starting point.**
- Run `git rev-parse HEAD` and record the value as the baseline commit in Implementation Notes. At planning time HEAD is **`2334fc6d`**. That commit and its parent `fabc2880` add only `sdlc/**` files on top of `b09bda5d`. **Do not stop on a HEAD mismatch.**
- Instead, prove that no build input changed since the last build-relevant commit: `git diff --stat b09bda5d HEAD -- . ':!sdlc'` must be **empty**. If it is not empty, stop and report: build inputs moved under the plan.
- Then run `git status --short -- build.gradle app k9mail-vendored metadata gradle gradle.properties settings.gradle gradlew .github CHANGES` and confirm it is clean.

0.2 **Test count.** Run `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`. Planning measured **697**; record the value you get.

0.3 **Isolated baseline tree.** Leave the working tree alone and do not change git state. Use a short path to stay clear of Windows MAX_PATH:
```bash
mkdir -p /c/Code/u060-baseline && git -C /c/Code/Android/sms-backup-plus archive HEAD | tar -x -C /c/Code/u060-baseline
cp /c/Code/Android/sms-backup-plus/local.properties /c/Code/u060-baseline/
```

0.4 **Run on JDK 17 without touching `~/.gradle/gradle.properties`.** Command-line system properties have the highest precedence (docs.gradle.org build_environment, "Command line > Gradle properties"). **Planning verified this works on this machine with the baseline's Gradle 8.14.1:** `./gradlew --version -Dorg.gradle.java.home=C:/Users/Michael.Horsley/.jdks/jbr-17.0.14` printed `Daemon JVM: C:\Users\Michael.Horsley\.jdks\jbr-17.0.14 (from org.gradle.java.home)`.
```bash
cd /c/Code/u060-baseline
export JAVA_HOME="C:/Users/Michael.Horsley/.jdks/jbr-17.0.14"
J17=-Dorg.gradle.java.home=C:/Users/Michael.Horsley/.jdks/jbr-17.0.14
./gradlew --version $J17 | tee <evidence>/baseline-gradle-version.txt     # proof of JDK
```
Because of SC-3, first close the pre-existing verification gap **in the baseline copy only**, using Gradle's writer. Verification then stays enforced for the measured run. The checksums do not influence compiled classes or coverage.
```bash
./gradlew $J17 --write-verification-metadata sha256 --continue help :app:assembleDebug :app:testDebugUnitTest lint assembleRelease
```
Save `git diff --no-index <repo>/gradle/verification-metadata.xml gradle/verification-metadata.xml` as `evidence/baseline-preexisting-verification-gap.diff`. It documents SC-3.

0.5 **Measured baseline run (verification enforced).** Write this init script to the scratchpad, not the repo. The root `build.gradle` disables JUnit XML, so the script re-enables it for measurement:
```groovy
// <scratch>/junitxml.init.gradle
gradle.projectsEvaluated { g ->
    g.rootProject.allprojects { p -> p.tasks.withType(Test).configureEach { reports.junitXml.required = true } }
}
```
```bash
./gradlew $J17 -I <scratch>/junitxml.init.gradle clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification
./gradlew $J17 lint assembleRelease        # CI-parity outcome (record PASS/FAIL; pre-existing failures are not regressions)
javap -v app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/zegoggles/smssync/App.class | grep major   # expect 61
```
Also copy the baseline's generated KSP file list for the B1 comparison: `find app/build/generated/ksp -type f | sed 's#.*/generated/ksp/##' | sort > <evidence>/baseline-ksp-files.txt`.

0.6 **Baseline coverage inventory (AC-9).** Save the script below to scratch, run it with `python -I`, and write its JSON output to evidence:
```python
# jacoco_inventory.py  <jacocoTestReport.xml> <out.json>
import sys, json, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot(); out = {}
for pkg in root.iter('package'):
    line = next((c for c in pkg.findall('counter') if c.get('type') == 'LINE'), None)
    out[pkg.get('name').replace('/', '.')] = {
        'classes': len(pkg.findall('class')),
        'line_covered': int(line.get('covered')) if line is not None else 0,
        'line_missed': int(line.get('missed')) if line is not None else 0}
json.dump(out, open(sys.argv[2], 'w'), indent=1, sort_keys=True)
```
The input is `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`. Also record the `.class` counts per class root:
```bash
for d in intermediates/classes/debugUnitTest/transformDebugUnitTestClassesWithAsm/dirs intermediates/javac/debug/compileDebugJavaWithJavac/classes tmp/kotlin-classes/debug; do
  echo "$(find app/build/$d -name '*.class' | wc -l) $d"; done | tee <evidence>/baseline-class-roots.txt
```
Also save the JUnit-XML totals: sum the `tests`, `failures`, `errors` and `skipped` attributes over `app/build/test-results/testDebugUnitTest/*.xml`.

**Checkpoint 0 (stop if not met):** the baseline gates passed on JDK 17, and the inventory JSON, class-root counts, test totals and KSP file list are saved. If the baseline gates fail on JDK 17 for a reason other than SC-3, stop and report, because the baseline is not trustworthy.

### Step 1 — Bootstrap the wrapper URL and checksum (temporary; regenerated in Step 5)

The old Gradle cannot run the `wrapper` task: it fails on JDK 25, and on JDK 17 the root project cannot configure because of SC-3. In `gradle/wrapper/gradle-wrapper.properties`, make exactly these two edits so that the **first** 9.5.1 download is already checksum-verified (N-3):
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip
distributionSha256Sum=bafc141b619ad6350fd975fc903156dd5c151998cc8b058e8c1044ab5f7b031f
```
The checksum is the official value from `services.gradle.org/distributions/gradle-9.5.1-bin.zip.sha256`, confirmed during planning. If the wrapper reports a checksum mismatch, stop: do not delete the line or retry with another URL. AC-14's "regenerated via `./gradlew wrapper`" is met in Step 5, which executes on 9.5.1.

### Step 2 — Atomic build-script change (IC-1, IC-3, AC-10, AC-12, AC-17, AC-19)

**2.1 `build.gradle` (root)**
- classpath: `com.android.tools.build:gradle:9.0.1`, `org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21`, `com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6`, `com.google.dagger:hilt-android-gradle-plugin:2.59.2`. Keep the KGP classpath line: the AGP 9 docs say to declare it in order to use a KGP higher than AGP's runtime 2.2.10.
- Rewrite the comments on lines 8–17 to the new versions, with sources (no stale "AGP 8.7.3 / JDK 17").
- `clean` task: change `delete rootProject.buildDir` to `delete rootProject.layout.buildDirectory`. The `task clean(type: Delete)` syntax may stay unless `--warning-mode all` reports it as an error.
- Add the configuration needed for AC-7 generation. URLs are not generated, so no toolchain-download repository and no foojay are needed:
```groovy
// U-060 (AC-7): daemon JVM criteria are generated without download URLs (no toolchain repository configured).
tasks.named('updateDaemonJvm') { toolchainPlatforms = [] }
```
- Leave the `allprojects { tasks.withType(Test) … }` block functionally unchanged. `url "…"` space-assignment is deprecation debt (AC-16).

**2.2 `app/build.gradle`**
- **Delete** `apply plugin: 'kotlin-android'` (line 2). AGP 9 built-in Kotlin fails with "The 'org.jetbrains.kotlin.android' plugin is no longer required…" (migrate-to-built-in-kotlin guide).
- **Replace** `android { kotlinOptions { jvmTarget = '17' } }` (lines 94–97) with a top-level block outside `android {}`. Keep it explicit, even though the guide says built-in Kotlin defaults jvmTarget to `compileOptions.targetCompatibility`, because AC-12 wants it stated:
```groovy
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}
```
- `compileOptions` stays `JavaVersion.VERSION_17` / `VERSION_17` (AC-12).
- **Change `getDefaultProguardFile('proguard-android.txt')` to `getDefaultProguardFile('proguard-android-optimize.txt')`.** AGP 9 flips `android.r8.proguardAndroidTxt.disallowed` to true. To **preserve current release semantics** (the app was built with `-dontoptimize`), append to `app/proguard-rules.pro`:
  ```
  # U-060: AGP 9 disallows proguard-android.txt; keep pre-AGP-9 release behaviour (no R8 optimisation).
  # Enabling optimisation is a separate, release-tested change (recorded debt).
  -dontoptimize
  ```
  The AGP 9 notes recommend exactly this ("explicitly specify -dontoptimize in a custom proguardFile … alongside using proguard-android-optimize.txt"). Do **not** use the `android.r8.proguardAndroidTxt.disallowed=false` opt-out (AC-17).
- Dependencies:
  - `kotlin-stdlib:2.3.21`, `robolectric:4.15.1`, `mockito-core:5.18.0`.
  - `hilt-android`, `hilt-compiler` (both `ksp` and `kspTest`) and `hilt-android-testing` all at `2.59.2`.
  - androidx.hilt stays `1.2.0`, subject to Checkpoint B1 and Branch M.
- `jacoco { toolVersion = "0.8.14" }`.
- Replace all 7 `${buildDir}` uses with `layout.buildDirectory` forms (scope verified via grep: lines 238, 263, 265, 270, 274, 309, 311, 316, 319). Factor the three class roots into **one shared list** used by both JaCoCo tasks, so the two copies cannot drift (IC-3), and add a fail-fast guard (AC-9). Do not use `project` at execution time (Gradle 9 deprecation):
```groovy
// U-060 (AC-9): single source of truth for JaCoCo class roots; paths are AGP/KGP internals and
// may move between plugin versions — the guard turns a silent empty fileTree into a build failure.
def jacocoClassRootPaths = [
    asm   : 'intermediates/classes/debugUnitTest/transformDebugUnitTestClassesWithAsm/dirs', // confirm in Step 3.3
    javac : 'intermediates/javac/debug/compileDebugJavaWithJavac/classes',                  // confirm in Step 3.3
    kotlin: '<confirmed built-in Kotlin output dir>',                                        // see Step 3.3
]
def jacocoClassRootFiles = jacocoClassRootPaths.collectEntries { k, v -> [k, layout.buildDirectory.dir(v).get().asFile] }
def assertJacocoClassRoots = { ->
    jacocoClassRootFiles.each { k, File f ->
        boolean any = false
        if (f.isDirectory()) f.eachFileRecurse(groovy.io.FileType.FILES) { if (it.name.endsWith('.class')) any = true }
        if (!any) throw new GradleException("U-060 JaCoCo guard: class root '${k}' missing or empty: ${f}")
    }
}
```
  Wire the shared list into both JaCoCo tasks:
  - Use `jacocoClassRootFiles.asm`, `.javac` and `.kotlin` in the three `fileTree(dir: …)` entries, keeping **exactly** the same `excludes` lists as today.
  - Add `doFirst { assertJacocoClassRoots() }` to both `jacocoTestReport` and `jacocoTestCoverageVerification`.
  - Change `destinationFile = file("${buildDir}/jacoco/testDebugUnitTest.exec")` and both `executionData` paths to `layout.buildDirectory.file('jacoco/testDebugUnitTest.exec')`. The `.get().asFile` form is fine for `destinationFile`, and `files(provider)` works for `executionData`.
- **Leave unchanged** (unless a checkpoint or branch below names them):
  - `-Werror -Xlint:unchecked` (unless Checkpoint B-javac triggers).
  - Every `violationRules` rule, `jacocoFileFilter`, `excludes = ['jdk.internal.*']` and `includeNoLocationClasses`.
  - `testOptions`, `lint {}` and `packaging {}`.
  - `compileSdkVersion 35`, `targetSdkVersion 35`, and `minSdkVersion 21` (Branch M only).

  Planning confirmed with javap of `gradle-api-9.0.1.jar` / `gradle-9.0.1.jar` that these methods all still exist in AGP 9.0.1: `compileSdkVersion(int)`, `minSdkVersion(int)`, `targetSdkVersion(int)`, `Packaging.exclude(String)` (deprecated), `LintImpl.disable(String...)`, `NdkOptions.abiFilters(String...)` and `BuildType.proguardFiles(Object...)`. Expect deprecation warnings, not errors (AC-16 debt).
- Update stale comments to match reality (AC-18): lines 3–8, 85–88, 94, 101–103, 114, 174–178, 199–201, 249, 483 ("generated by kapt" becomes KSP), and 574–576.

**2.3 `k9mail-vendored/build.gradle` (IC-2)**
- **No functional change.** `VERSION_1_8` stays, and `minSdkVersion 21` (line 54) stays even under Branch M. A library minSdk below the app's is valid for manifest merge.
- Planning proved that JDK 25 `javac -source 8 -target 8` exits 0 with "[options] source value 8 is obsolete…" warnings. It fails only with `-Werror` (scratch test with JBR 25.0.4).
- `-Werror` reaches only `:app`. It is set by `tasks.withType(JavaCompile)` inside `app/build.gradle`, which is project-scoped (scope verified via grep: `-Werror` appears only at `app/build.gradle:202`).
- The obsolete-option warning is recorded as AC-16 debt. Do not re-add `android.javaCompile.suppressSourceTargetDeprecationWarning`; it is cosmetic and would be a new flag.
- Optionally update the "AGP 8" wording on comment lines 31 and 113. This is not required.

**2.4 `metadata/build.gradle`, `settings.gradle`:** no change unless Step 7.4 reports an *error*. Gradle 9 removes no construct used there: `task` syntax and `buildDir` are deprecated, not removed (upgrading_major_version_9 page). The plain `metadata` project gets `:metadata:lint` from the root `lint` in CI. Its Groovy-4 behavior is checked in Step 8.3. Both files are in `U060_TRACKED` in case Step 7.4 forces an edit.

**2.5 `gradle.properties` (AC-18, AC-19)**
- Keep `android.useAndroidX`, `android.nonTransitiveRClass=true` and `android.nonFinalResIds=true`. AGP 9 does not list them among removed or enforced properties.
- Do not add `android.builtInKotlin`, `android.newDsl` or any `org.gradle.java.home`.
- Update the comments on lines 23 and 31–36. "AGP 8 defaults" becomes "defaults since AGP 8, unchanged in AGP 9", and the JDK 17 text is reworded to describe JDK 25. Note that the vendored Java 8 module now *does* emit the obsolete-source warning (without -Werror).
- `org.gradle.jvmargs`: decide in Step 9.

**Explicitly not done (AC-17):** no `android.builtInKotlin=false`, no `android.newDsl=false`, no `android.r8.*` opt-outs, no kapt, no `legacy-kapt`, no `java { toolchain }`, and no foojay.

### Step 3 — First run in writer mode, then checkpoints A–B (de-risk the unknowns early)

New artifacts are not yet in the metadata, so the first compile attempts run in Gradle's writer mode. Writer output is **discarded and regenerated cleanly** in Step 6, so exploratory iterations, including a Branch M switch, leave no stale entries behind.

3.1 Run `./gradlew --version`. Expect `Gradle 9.5.1`, Launcher JVM 25 and Daemon JVM 25. For now the daemon is the JBR, via the user-global `org.gradle.java.home`.

3.2 **Checkpoint A (configuration).** Run `./gradlew --write-verification-metadata sha256 help --warning-mode all`. Any *configuration error* must be fixed here. Classify each failure as one of:
- (i) a construct that Gradle 9 or AGP 9 treats as an error: fix it (AC-16).
- (ii) a plugin incompatibility, including any KGP-on-Gradle-9.5 error (R16): check the version table, and **stop** if a version change is needed that this plan does not pre-specify.

3.3 **Checkpoint B (compile and codegen).** Run `./gradlew --write-verification-metadata sha256 :app:assembleDebug :app:compileDebugUnitTestKotlin --continue`. Then work through B1–B5 and B-javac.

- **B1, androidx.hilt 1.2.0 under KSP2 (SC-2).** **PASS** requires all of the following:
  - (a) `:app:kspDebugKotlin`, `:app:kspDebugUnitTestKotlin`, `:app:hiltJavaCompileDebug` and `:app:compileDebugJavaWithJavac` succeed.
  - (b) Generated code exists for each `@HiltWorker`: `find app/build/generated/ksp -name "*BackupWorker*" -o -name "*RestoreWorker*"`. Expect `*_AssistedFactory*` and `*_HiltModule*` sources.
  - (c) The generated file-name set equals the baseline's: `find app/build/generated/ksp -type f | sed 's#.*/generated/ksp/##' | sort | diff - <evidence>/baseline-ksp-files.txt`. Allowed differences are only paths that differ in a build-tool-internal directory segment; list each one.
  - (d) The full unit-test run in Step 7 passes. This is the runtime proof that `HiltWorkerFactory` wiring works.

  **B1 FAIL** means one of:
  - an error from the androidx.hilt processor, e.g. stack frames in `androidx.hilt.*` or `androidx.room.compiler.processing.*` in the `ksp*` task output;
  - missing `*_AssistedFactory` / `*_HiltModule` for either worker;
  - a test failure traceable to `HiltWorkerFactory` / assisted-inject wiring.

  On B1 FAIL, save the failing log excerpt to `evidence/b1-failure.log`, then **take Branch M**. There is no intermediate fallback and no escalation (SC-2).

  Failures with other origins are not B1:
  - Dagger's own `[Hilt] Provided Metadata instance has version …` is R11; see Branch M triggers.
  - Generic Kotlin compile errors are handled by B4 or by normal troubleshooting.

  Do not take Branch M for them.
- **B-javac, `--system` propagation and javac 25 option warnings** (formerly "B2-javac"; renamed because Fallback B2 no longer exists). Planning proved that javac 25 with `-source 17 -target 17` and no system-modules location emits "[options] location of system modules is not set…", which fails under `-Werror`. AGP passes `--system` (JdkImageTransform) to `compileDebugJavaWithJavac`. However, `hiltJavaCompileDebug` is a Hilt-owned JavaCompile that also receives `-Werror` (its name has no "test").
  - **Also watch for JDK 23+ javac's annotation-processing default change (`-proc`)** (N-7). Gradle always passes an explicit `-processorpath`, so no impact is expected. But a `-proc`-related note or warning under `-Werror` belongs to this checkpoint.

  If either task fails with an `[options]` warning:
  - Primary fix: confirm the version set is as planned.
  - Fallback: add `'-Xlint:-options'` to the app's non-test `compilerArgs`. It suppresses **only** command-line-option warnings, not type-safety warnings. Record it in Implementation Notes as a justified, scoped change that reverses part of U-057.
- **B3, JdkImageTransform/jlink on JDK 25.** If `:app:compileDebugJavaWithJavac` fails in the jlink transform, **stop** and report. No supported workaround fits this plan, and an AGP minor bump would require re-justifying AC-15. Branch M does not fix this.
- **B4, Kotlin sources under `src/*/java`.** Built-in Kotlin must compile the 31 main and 15 test `.kt` files that live in `java/` directories. Check that `BackupWorker.class` and `SyncStateRepository`-family classes exist in the Kotlin output.
  - **Fallback:** `android { sourceSets { main { kotlin.srcDirs += 'src/main/java' }; test { kotlin.srcDirs += 'src/test/java' } } }`. This is the only supported way under built-in Kotlin, per the migrate guide.
- **B5, locate the class roots (AC-9).** List every directory that holds project classes:
  ```bash
  find app/build -type d -path "*/com/zegoggles/smssync" | sed 's#/com/zegoggles/smssync$##' | while read d; do echo "$(find "$d" -name '*.class' | wc -l) $d"; done
  ```
  Planning found an AGP 9.0.1 internal artifact type `BUILT_IN_KOTLINC` (`InternalArtifactType$BUILT_IN_KOTLINC`). The Kotlin output is therefore **expected to move** from `tmp/kotlin-classes/debug` to an `intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes`-style path. This is unverified until the build runs.
  - Put the observed paths into `jacocoClassRootPaths` (Step 2.2).
  - Confirm that the ASM path and the javac path still exist.

**Checkpoint B outcome:** either B1 passes, or Branch M has been applied and its M.1–M.5 checks pass. Rollback, if ever needed, follows the shared **Rollback procedure** above (explicit path list only).

### Branch M — minSdk 23 contingency (PRE-APPROVED by the product owner; execute without further decisions)

**Authority:** the product owner said "I'm good raising the minsdk version" (relayed by the orchestrator, recorded in plan-review iteration 1 and SC-2). This branch is the approved outcome. It is **not** an escalation point.

**Triggers** (any one is sufficient):

| Trigger | Origin | What the branch adds beyond M.1–M.9 |
|---|---|---|
| **T1** | B1 FAIL (R1) | nothing extra |
| **T2** | R11: Dagger `[Hilt] Provided Metadata instance has version …` or another Hilt 2.59.2 failure that the release notes tie to a fix in 2.60 | Hilt **2.59.2 → 2.60.1** at all 5 use sites: root `hilt-android-gradle-plugin`, `hilt-android`, `ksp` `hilt-compiler`, `kspTest` `hilt-compiler`, `hilt-android-testing`. 2.60.1 is the latest 2.60 patch and needs minSdk 23 ("minSDK for Hilt is now 23"). The app sets no Hilt-plugin flags (grep: no `hilt {` block, no `dagger.hilt.*` property), so 2.60's "error if a user sets a plugin-controlled flag" does not apply. The app uses no multidex (grep), so "Multidex support is removed" does not apply. |
| **T3** | R8: Robolectric 4.15.1 fails on JDK 25 *after* the documented `--add-opens` flags (Step 7.1) | Robolectric **4.15.1 → 4.16.1** (latest 4.16 patch; supports SDK 23–36; "you need to use JDK 21 if running tests with SDK 36 target", which JDK 25 satisfies). The app's minSdk is raised as well, so that the Robolectric-testable floor equals the app's floor: 4.16 can no longer emulate API 21–22. |

**M.1 `app/build.gradle`.** These are exact edits, and nothing else changes.
- Line 26: `minSdkVersion 21` becomes `minSdkVersion 23`.
- Line 183: `implementation 'androidx.hilt:hilt-work:1.2.0'` becomes `…:1.3.0`.
- Line 184: `ksp 'androidx.hilt:hilt-compiler:1.2.0'` becomes `…:1.3.0`.
- Line 152 comment: "compatible with compileSdk 35 + minSdk 21" becomes "+ minSdk 23 (U-060 Branch M)".
- Comment block 174–179: add one line, `// U-060: androidx.hilt 1.3.0 (KSP2-documented line; hilt-work requires minSdk 23) — minSdk raised under PO approval.`
- T2 only: the Hilt 2.60.1 edits listed in the trigger table.
- T3 only: `robolectric:4.16.1`.

  **Why 1.3.0, not 1.4.0:** 1.3.0 is the lowest line documented for KSP2 (AC-15). Its POM depends on `hilt-android 2.54`, `hilt-common 1.3.0` (→ `hilt-core 2.54`), `work-runtime 2.3.4`, `annotation 1.8.1` and `jspecify 1.0.0`. All of them resolve upward to, or coexist with, this app's versions: Hilt 2.59.2, work 2.9.1.

  **Pre-specified second step:** if B1 still fails with androidx.hilt 1.3.0, try **1.4.0** at both sites. 1.4.0 uses `room-compiler-processing 2.8.4` and requires "KGP version to be at least 2.2.0", which is satisfied; its hilt-work also declares minSdk 23. If 1.4.0 also fails, **stop** (see "Stop conditions").

**M.2 `app/src/test/java/com/zegoggles/smssync/compat/SmsReceiverTest.java`.** These are the only test-source edits in the whole story.
- Lines 27, 32, 37, 42: `@Config(sdk = Build.VERSION_CODES.LOLLIPOP)` becomes `@Config(sdk = Build.VERSION_CODES.M)`. Robolectric will not run a test configured below the manifest minSdk, and 4.16 has no SDK 21/22 at all.
- Lines 23–25, the comment, are replaced with:
  ```java
  // U-005 / U-001 raised minSdk to 21; U-060 (Branch M, PO-approved) raised it to 23 because
  // androidx.hilt 1.3.0 (KSP2) requires it. Tests run at SDK 23 (M), the new minimum; API 23 still
  // takes the KITKAT..P branch of SmsReceiver.isSmsBackupDefaultSmsApp (compat/SmsReceiver.java:47-50).
  ```
- Method names and bodies are unchanged. Nothing is deleted, `@Ignore`d or disabled. **The `@Test` count stays 697.**

  Semantics are preserved. `isSmsBackupDefaultSmsApp` branches on `>= Q` and then `>= KITKAT`, and API 21 and API 23 both take the `KITKAT` branch. `onReceive` has no SDK branch below Q.
- `WorkManagerSchedulerTest.kt:378` is already `@Config(sdk = [23])` and is unaffected. The configured SDK set becomes {23, 29}, inside Robolectric 4.15.1 (and 4.16.1) support (AC-10).

**M.3 Manifest / `tools:overrideLibrary`.** **No manifest change.** `tools:overrideLibrary` must **not** be added: at minSdk 23 every dependency's minSdk is ≤ the app's (hilt-work 1.3.0 = 23, hilt-android 14, vendored module 21), so there is nothing to override. Grep confirmed that no `tools:overrideLibrary` or `<uses-sdk>` exists in `app/src` or `k9mail-vendored/src` today. **Verify:** `processDebugMainManifest` succeeds, and the merged manifest under `app/build/intermediates/merged_manifest/debug/` declares `android:minSdkVersion="23"`. Use `find app/build/intermediates -path "*merged_manifest*debug*AndroidManifest.xml" | xargs grep -o 'minSdkVersion="[0-9]*"'`.

**M.4 IC-1 proof (single Hilt version).** Save to evidence:
- `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep -E "dagger:hilt|androidx.hilt|androidx.work:work-runtime"`. It must show `com.google.dagger:hilt-android:2.54 -> 2.59.2` (or `-> 2.60.1` under T2), `androidx.hilt:hilt-work:1.3.0`, and `androidx.work:work-runtime:2.3.4 -> 2.9.1`.
- `./gradlew :app:dependencies | grep -iE "^ksp.*ProcessorClasspath"` lists the KSP processor configurations.
- `./gradlew :app:dependencies --configuration <the debug one>` must show `androidx.hilt:hilt-compiler:1.3.0` and `com.google.dagger:hilt-compiler:2.59.2` (or `2.60.1`).

**M.5 Lint (`warningsAsErrors true`, `lint-baseline.xml`).**
- Raising minSdk changes the text of existing `ObsoleteSdkInt` findings. Lint embeds the minSdk in the message, e.g. "Unnecessary; SDK_INT is always >= 21", and 10 such entries are baselined.
- Grep found **no new** checks that become obsolete: there is no `SDK_INT` comparison against 22/23 in `app/src/main`, and no `-v21`/`-v22`/`-v23` resource folders, only `drawable-anydpi-v26`.
- If `lint` (Step 8.2) fails **only** with `ObsoleteSdkInt` issues at locations already in the baseline:
  1. Run `./gradlew :app:updateLintBaseline`.
  2. Run `git diff -U0 HEAD -- app/lint-baseline.xml`. The only allowed differences are the header attributes (`by=`, `name=`, `version=`) and `ObsoleteSdkInt` `message=` / `errorLine` lines.
  3. Save that diff to evidence.
- Any other issue id in the diff goes to the R5 procedure: do not keep the regenerated baseline. Restore it from HEAD with `git restore --source=HEAD --worktree -- app/lint-baseline.xml`, list the findings, and escalate.
- Do **not** fix obsolete checks in production source. That would be a source change outside this story.

**M.6 Verification metadata.** Branch M is applied at Checkpoint B (T1, T2) or Step 7 (T3), and both come **before** the Step 6 clean regeneration is final:
- **T1/T2:** Step 6 runs after the branch, so its single writer pass records the new artifacts.
- **T3:** the branch is applied after Step 6, so **re-run Step 6 completely**: pristine restore, one writer pass, and the additions-only proof.

Step 6.4's expected-additions review must then include:
- `androidx.hilt:{hilt-work,hilt-common,hilt-compiler}:1.3.0` (or 1.4.0);
- POM/module-only entries for `com.google.dagger:{hilt-android,hilt-core}:2.54`, `androidx.work:work-runtime:2.3.4`, `androidx.annotation:annotation:1.8.1` and `org.jspecify:jspecify:1.0.0`;
- `com.google.devtools.ksp:symbol-processing-api:2.0.10-1.0.24`;
- T2 only: `com.google.dagger:*:2.60.1`;
- T3 only: `org.robolectric:*:4.16.1` plus transitive dependencies.

The 1.2.0 entries stay in the file: the writer adds entries and never removes them, and the additions-only rule requires that.

**M.7 Documentation.**
- **`CHANGES`:** the top block, `== 1.6.0 (xxx) dd-mm-2019`, is the unreleased version. Its line 3, `  * Android minSdk now 14 (4.0, Ice Cream Sandwich)`, is already stale: the U-005/U-001 raise to 21 was never logged. **Replace that line** with `  * Android minSdk now 23 (6.0, Marshmallow)`, keeping the existing two-space-asterisk style. This records the net change for the unreleased 1.6.0 and avoids two contradictory minSdk lines in one block.
- **`sdlc/artifacts/engagement/code-location.md`:** the Tech Stack row `| minSdk | 14 (tested Android 4.x ICS – 10.x Q) |` (line 28) becomes `| minSdk | 23 (Android 6.0) — raised from 21 by U-060 Branch M (PO-approved; androidx.hilt 1.3.0 / KSP2) |`. Without Branch M, Step 12.1 sets it to `21 (Android 5.0)`.
- **`README.md` line 38** ("Tested with Android 4.x (Ice Cream Sandwich) - 10.x. (Q)") is already stale at minSdk 21. **Do not edit it in this story.** Record it as follow-up in Implementation Notes ("README supported-version line stale since U-001; now 6.0+").
- **`metadata/` (Play / F-Droid listings):** grep found no Android-version claims (`android [0-9].`, `minsdk`, `lollipop`, `5.0`), so there is nothing to change. Record the grep as evidence.

**M.8 Release impact statement.** Record verbatim in Implementation Notes:
> "U-060 raised minSdk from 21 to 23 (Android 6.0). Devices on API 21–22 (Android 5.0/5.1) will no longer be offered updates by Google Play or F-Droid; existing installs keep working on their current version. No data migration, permission change, or behavior change is involved for API 23+ devices; targetSdk/compileSdk remain 35."

**M.9 Device check.** AC-5 is unchanged: launch on the API 36 emulator. **Additionally**, check for an API 23 system image (`"$LOCALAPPDATA/Android/Sdk/emulator/emulator" -list-avds` and `ls "$LOCALAPPDATA/Android/Sdk/system-images/android-23" 2>/dev/null`). If one exists, repeat the Step 10 launch on it and record the result. If none exists, record "API 23 launch not performed (no image installed)". Do not download images as part of this story.

**M.10 Recording and story amendment.**
- **Implementation Notes**, under a heading "Branch M (minSdk 23) — triggered", record:
  - trigger(s) T1/T2/T3, with the `evidence/b1-failure.log` excerpt or the equivalent;
  - the product-owner quote;
  - the M.1 version changes;
  - the M.2 diff;
  - M.3/M.4/M.5 evidence;
  - the M.7 doc edits;
  - the M.8 statement;
  - the M.9 result.
- In the Developer's Phase Completion Report, set `story_amendment_required: true`, with the reason "Branch M triggered: minSdk 21 → 23".
- The **Developer does not edit the story's ACs**. The **orchestrator** routes the amendment to the **Requirements Analyst** before code review/QA. The amendment must:
  - (a) amend the Story's scope statement ("app runtime behavior unchanged") and the "Existing Behavior to Preserve" bullet to state the minSdk-23 exception and the product-owner approval;
  - (b) add an AC, e.g. AC-20: "Given Checkpoint B1/R11/R8 triggered Branch M, then `minSdkVersion` is 23, the androidx.hilt artifacts are 1.3.0 (or 1.4.0), the 4 `SmsReceiverTest` `@Config` lines are retargeted LOLLIPOP→M with the `@Test` count unchanged, and `CHANGES` and `code-location.md` record the raise";
  - (c) note under AC-2 that a retarget is not a disable;
  - (d) note under AC-10 that the configured SDK set is {23, 29}.

  AC-19 needs no change, because it does not cover minSdk.

**Stop conditions that Branch M does not fix** (stop and report; roll back via the shared procedure if asked):
- androidx.hilt 1.3.0 **and** 1.4.0 both fail B1. The remaining option, a hand-written `WorkerFactory` replacing `HiltWorkerFactory`, is a source change across BackupWorker, RestoreWorker, CheckpointModule and App, and needs its own story.
- B3 (jlink).
- A JaCoCo floor miss caused by counting changes (Step 7.2).
- New non-`ObsoleteSdkInt` lint findings (R5).
- Robolectric 4.16.1 still failing on JDK 25 (T3 path).
- Any KGP/Gradle-9.5 *error* (R16).

### Step 4 — Repository JDK declaration (AC-7)

Run `./gradlew updateDaemonJvm --jvm-version=25`. Expect `gradle/gradle-daemon-jvm.properties` with `toolchainVersion=25`, no `toolchainUrl.*` lines, and the "generated by updateDaemonJvm" header. Re-run `./gradlew --version` and expect the Daemon JVM line to cite the criteria file. If Gradle labels the feature incubating, record that in Implementation Notes.

### Step 5 — Wrapper regeneration (AC-14)

This runs on Gradle 9.5.1 because of the Step 1 bootstrap. Verification is still in writer mode until Step 6; alternatively, run this step after Step 6.
```bash
./gradlew wrapper --gradle-version 9.5.1 --distribution-type bin \
  --gradle-distribution-sha256-sum bafc141b619ad6350fd975fc903156dd5c151998cc8b058e8c1044ab5f7b031f
sha256sum gradle/wrapper/gradle-wrapper.jar   # must equal 497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7 (official gradle-9.5.1-wrapper.jar.sha256)
git diff --summary -- gradlew gradle/wrapper   # gradlew must keep mode 100755
```
**Note:** `gradlew.bat` is **not tracked today**: `git ls-files` returns nothing for it. The wrapper task creates it (with Gradle 9.5's reworked Windows script template), and AC-14 names it, so add it as a new tracked file. Do not run `./gradlew wrapper` a second time without the checksum flag. Confirm that `distributionSha256Sum` remains in the properties file.

### Step 6 — Clean verification-metadata regeneration (AC-3, AC-13)

1. Run `git show HEAD:gradle/verification-metadata.xml > <scratch>/vm-head.xml`, then `git restore --source=HEAD --worktree -- gradle/verification-metadata.xml`. This single-path restore discards the exploratory writer output from Step 3.
2. Run one writer pass over every configuration that CI and the gates touch, from this trusted network:
   `./gradlew --write-verification-metadata sha256 --continue clean help test lint jacocoTestReport assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`
3. Run the additions-only proof. Save the script below to scratch and run it with `python -I`:
```python
import sys, xml.etree.ElementTree as ET
NS = '{https://schema.gradle.org/dependency-verification}'
def load(p):
    r = ET.parse(p).getroot(); s = set()
    for c in r.iter(NS + 'component'):
        for a in c.findall(NS + 'artifact'):
            for h in a: s.add((c.get('group'), c.get('name'), c.get('version'), a.get('name'), h.tag[len(NS):], h.get('value')))
    return s, ET.tostring(r.find(NS + 'configuration'))
old, oc = load(sys.argv[1]); new, nc = load(sys.argv[2])
print('configuration block unchanged:', oc == nc)
print('removed-or-altered entries:', len(old - new)); [print('  -', e) for e in sorted(old - new)]
print('added entries:', len(new - old))
```
   **Pass condition:** the configuration block is unchanged (`verify-metadata` true, `verify-signatures` false, the same two trusted-artifact regexes), 0 entries are removed or altered, and added > 0.
4. Review the added components. Each must belong to one of:
   - the version changes in this plan, or their transitive dependencies;
   - the Branch M additions listed in M.6, if Branch M triggered;
   - the SC-3 junit-bom `.module` gap.

   Flag anything unexpected in Implementation Notes.
5. **Enforced re-run (no writer):** `./gradlew :app:assembleDebug` must pass (AC-13). The full gate run in Step 7 then proves completeness for all gate configurations.

Note on scope: Robolectric downloads `android-all-instrumented` jars at test runtime through its own resolver, into `~/.m2`. They are outside Gradle verification both before and after this change, so this is not a regression. Record it as an observation.

### Step 7 — Gates on JDK 25 (AC-1, AC-2, AC-4, AC-9, AC-10, AC-12, AC-16)

7.1 Run:
- `./gradlew --version > evidence/after-gradle-version.txt`
- `./gradlew -I <scratch>/junitxml.init.gradle clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification 2>&1 | tee evidence/after-gates.log`
- `./gradlew :app:jacocoTestReport`

The first gate command must exit 0, and `grep -c "major version 69" evidence/after-gates.log` must be 0. Also grep the log for `UnsupportedClassVersion|Unsupported class file|ByteBuddy|IllegalArgumentException: Unsupported|jacoco.*(Error|Exception)|InaccessibleObjectException` and record the hits (expected: none).
- If Robolectric raises `InaccessibleObjectException`, add the specific `--add-opens` jvmArgs to `testDebugUnitTest`. This is Robolectric's documented JDK ≥ 17 configuration, not an opt-out. Record it. If Robolectric 4.15.1 still fails on JDK 25 after that, apply **Branch M, trigger T3**.
- Mockito's "self-attaching … inline-mock-maker" and JDK dynamic-agent warnings are expected on JDK 21+. Record them as debt; they do not fail the build.

7.2 **AC-9 comparison.** Run `jacoco_inventory.py` on the new XML and compare the result with the baseline JSON. **Pass:** for every baseline package, `after.classes >= baseline.classes`, and no baseline package is missing. Record both tables and the class-root counts (the Step 0.6 command with the new paths).

The JaCoCo 0.8.13/0.8.14 Kotlin filters (inline functions, elvis/safe-call) can legitimately change *line totals* for Kotlin packages. If a package's LINE ratio falls below its floor **only** because of counting changes, **do not lower the floor** (AC-4); stop and escalate. Watch `di` especially: it measured 3.1% (1/32) against a 0.03 floor, and the package has 9 `.kt` files.

7.3 **AC-12 / IC-2 bytecode.** Run `javap -v` on:
- `App.class` in the javac root: expect `major version: 61`.
- one Kotlin class (for example `service/BackupWorker.class`) in the Kotlin root: expect 61.
- `k9mail-vendored/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/fsck/k9/mail/Address.class`: expect 52.

7.4 **AC-16.** Run `./gradlew :app:assembleDebug --warning-mode all 2>&1 | tee evidence/warning-mode-all.log`. Fix anything reported as an error or as "will fail". List every remaining deprecation in Implementation Notes. Expected debt includes:
- Groovy space-assignment (`namespace '…'`, `minifyEnabled true`, `url "…"`)
- `packaging.exclude`
- `task` syntax
- Gradle 9.4 "methods taking Closure on Test tasks"
- the vendored "source/target value 8 is obsolete"
- Mockito self-attach
- any AGP or KGP deprecations surfaced on Gradle 9.5 (R16)

7.5 Re-count `@Test`: it must be ≥ 697, and under Branch M it must be exactly 697. Compare the JUnit-XML totals with the baseline: executed ≥ baseline, failures = errors = 0, and skipped ≤ baseline. **Test-source diff rule:**
- **Without Branch M:** `git diff --stat HEAD -- app/src/test` must be **empty**.
- **With Branch M:** `git diff --numstat HEAD -- app/src/test` must list **exactly one file**, `app/src/test/java/com/zegoggles/smssync/compat/SmsReceiverTest.java`. Then run:
  ```bash
  git diff -U0 HEAD -- app/src/test | grep -E '^[-+][^-+]' \
    | grep -vE '^[-+]\s*@Test @Config\(sdk = Build\.VERSION_CODES\.(LOLLIPOP|M)\)$' \
    | grep -vE '^[-+]\s*//'
  ```
  This must print **nothing**: only the 4 `@Config` lines and the comment lines 23–25 changed. Also run `git diff HEAD -- app/src/test | grep -cE '^\+.*VERSION_CODES\.M\)'`, which must print `4`.

**Checkpoint C:** all of 7.1–7.5 hold. Otherwise apply Branch M if a trigger applies, or roll back (shared procedure), or stop on a listed stop condition.

### Step 8 — CI parity and `ci.yml` (AC-8, REQ-MODERNIZATION-003 AC-1/AC-2)

8.1 Edit `.github/workflows/ci.yml`:
- Rename the step to `Set up JDK 25`, with `java-version: '25'` and `distribution: 'temurin'`. `actions/setup-java@v4` supports it; leave the action version alone.
- Replace the stale comment (lines 19–21) with: "Gradle 9.5 / AGP 9.0 run on JDK 17+; this project builds and tests on JDK 25, declared in `gradle/gradle-daemon-jvm.properties`. AGP 9.0 requires SDK Build Tools ≥ 36.0.0."
- Add a deterministic step before the Gradle run: `"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "build-tools;36.0.0"`. The image ships 36.0.0 today; the step guards against image drift.
- Leave the Gradle commands (`test lint jacocoTestReport assembleRelease` and `:app:jacocoTestCoverageVerification`) and the caches as they are. The containment for R14 is pre-written but **not** applied unless the first PR run fails.

8.2 Run the CI command locally: `./gradlew test lint jacocoTestReport assembleRelease`, then `./gradlew :app:jacocoTestCoverageVerification`. Compare the result with the baseline Step 0.5 `lint`/`assembleRelease` outcome.
- **New lint findings from AGP 9's lint** must not be silenced by regenerating `lint-baseline.xml` or by adding `disable` entries without review. List them in Implementation Notes and escalate as a Scope Concern if they fail `warningsAsErrors`. The only pre-authorised baseline update is Branch M's `ObsoleteSdkInt` rule (M.5).
- `assembleRelease` must succeed with `minifyEnabled true` (REQ-MODERNIZATION-003 AC-2).
- Note the AGP 9 behavior change `android.onlyEnableUnitTestForTheTestedBuildType=true`: `./gradlew test` no longer runs `testReleaseUnitTest`. Record that CI's `test` now runs only the debug unit tests. This is accepted AGP 9 default behavior, not an opt-out. The local gate already used `testDebugUnitTest`.

8.3 Check that `:metadata:lint` (Groovy 4) behaves the same as in the baseline.

CI cannot be executed from this machine without a push. Record the YAML diff as evidence, and note that the first CI run on the PR is the remaining verification (R14).

### Step 9 — AC-7 fresh-clone matrix and AC-18 jvmargs (scratch Gradle user homes; no user-global edits)

Use `-g <dir>` (`--gradle-user-home`) to simulate machines. Each fresh home also re-downloads and **verifies the wrapper zip against `distributionSha256Sum`**, which is additional AC-14 evidence. Run `./gradlew -g <home> --version` for each case, plus `help` for M1:

| Run | `<home>/gradle.properties` | Expected |
|---|---|---|
| M1 | none (fresh clone) | Daemon JVM = a detected JDK 25 (Oracle 25.0.5 or JBR), "from gradle/gradle-daemon-jvm.properties"; `help` succeeds. **This run also applies the repo's `org.gradle.jvmargs`** (SC-4), which validates AC-18 |
| M2 | `org.gradle.java.home=C:/Users/Michael.Horsley/.jdks/jbr-17.0.14` | Daemon JVM is still 25. This proves the criteria take precedence over `org.gradle.java.home` |
| M3 | `org.gradle.java.installations.auto-detect=false` + `org.gradle.java.installations.paths=C:/Program Files/Java/jdk-25.0.5` | Daemon JVM = 25.0.5 |
| M4 | `org.gradle.java.installations.auto-detect=false`, no paths | Record the actual behavior (an error naming the unmet criteria is expected). This is the **documented limitation** for AC-7: with auto-detect off, a JDK 25 must be listed in `org.gradle.java.installations.paths` |

The current machine configuration (auto-detect=false, paths = Android Studio JBR 25) is effectively M3 with the JBR, and Step 7 already proves it.

**AC-18 decision rule per flag:** if M1 starts and builds `help` with `-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider` and `-Djdk.net.unixdomain.tmpdir=/nonexistent` in place, **retain** both. Planning verified that `sun.nio.ch.WindowsSelectorProvider` still exists in JDK 25.0.4 (`javap --module java.base`).
- Before writing the stated reason, look up why the flags were added: `git log -S"WindowsSelectorProvider" --oneline -- gradle.properties`.
- Remove a flag only if it demonstrably breaks the daemon on JDK 25, and record the reason.
- Update the line 9 comment to describe what each flag does.

### Step 10 — On-device smoke test (AC-5)

```bash
"$LOCALAPPDATA/Android/Sdk/emulator/emulator" -avd Pixel_9_Pro -no-snapshot-load   # run in background (AVD verified: target=android-36)
adb wait-for-device; until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 5; done
SERIAL=$(adb devices | awk 'NR>1 && /emulator-/{print $1; exit}')   # serials change between boots — never hard-code
adb -s $SERIAL install -r app/build/outputs/apk/debug/app-debug.apk
adb -s $SERIAL logcat -c
adb -s $SERIAL shell am start -W -n com.zegoggles.smssync/.activity.MainActivity
sleep 10; adb -s $SERIAL shell pidof com.zegoggles.smssync          # must print a PID
adb -s $SERIAL logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime: Process: com.zegoggles.smssync" ; echo "fatal-grep-exit=$?"   # expect exit 1 (no match)
adb -s $SERIAL exec-out screencap -p > sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/ac5-launch.png
```
Per `.claude/CLAUDE.md`, set `MSYS_NO_PATHCONV=1` only in an adb-only shell, and only if you use a `/sdcard/...` path. Never set it in a Gradle shell. Under Branch M, also perform M.9.

### Step 11 — Static compliance sweep (AC-3, AC-4, AC-17, AC-19, IC-3)

Run each of the following and paste the output into evidence:
- `grep -rnE "dependency-verification|verify-metadata" build.gradle app/build.gradle k9mail-vendored/build.gradle metadata/build.gradle settings.gradle gradle.properties .github/workflows/ci.yml .claude/CLAUDE.md sdlc/artifacts/engagement/code-location.md`. The only allowed hit is `<verify-metadata>true` in the XML, which is not among these paths.
- `grep -nE "kotlin-android|kotlinOptions|buildDir" build.gradle app/build.gradle k9mail-vendored/build.gradle metadata/build.gradle` → expect 0 code hits. Comment mentions must be reworded.
- `grep -nE "builtInKotlin|newDsl|proguardAndroidTxt|org.gradle.java.home|enableJetifier" gradle.properties build.gradle app/build.gradle` → expect 0.
- `git diff HEAD -- app/build.gradle | grep -E "^[-+].*(minimum|includes|excludes)"` → only the expected `buildDir` → `layout.buildDirectory` rewrites. No `minimum` or `includes` lines change.
- `grep -nE "compileSdkVersion 35|targetSdkVersion 35|minSdkVersion" app/build.gradle` → 35/35, and minSdk 21 (or 23 under Branch M). Also `grep -nE "nonTransitiveRClass=true|nonFinalResIds=true" gradle.properties`.

### Step 12 — Documentation (AC-6)

12.1 **`sdlc/artifacts/engagement/code-location.md` (Developer edits directly).**
- Update the build-environment rows of Tech Stack:
  - Language: Java + Kotlin; app bytecode Java 17; vendored k-9 module Java 8
  - Gradle 9.5.1; AGP 9.0.1; Kotlin 2.3.21
  - compileSdk/targetSdk 35; minSdk row per M.7 (21 without Branch M, 23 with it)
  - Build tools 36.0.0+
- Fix the Build/Test/Run table: debug APK path `app/build/outputs/apk/debug/app-debug.apk`, and the three gates.
- Fix the stale `-Xlint:deprecation` note.
- Add a **"Build Environment"** section. **Write (b) and (c) from the observed M2/M4 results, not from this plan's expectation** (N-6). If M2 or M4 behaves differently from the table in Step 9, the text states the observed behavior. It covers:
  - (a) Gradle, javac, kotlinc and unit tests run on **JDK 25**, declared by `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=25`).
  - (b) Precedence, as confirmed by M2: daemon JVM criteria > `org.gradle.java.home` > `JAVA_HOME`. The docs say "If the Daemon JVM criteria configuration is provided, it takes precedence over JAVA_HOME and org.gradle.java.home". For `org.gradle.java.home` itself, command line > `~/.gradle/gradle.properties` > project `gradle.properties`, strictly honoured since Gradle 9.4. **Without** the criteria file, a user-global `org.gradle.java.home` overrides `JAVA_HOME`.
  - (c) With `org.gradle.java.installations.auto-detect=false`, a JDK 25 must be listed in `org.gradle.java.installations.paths` (M4 result).
  - (d) A user-global `org.gradle.jvmargs` replaces the repository's jvmargs (SC-4).
  - (e) How to verify: `./gradlew --version`, the Daemon JVM line.

  Other stale rows (otto, firebase-jobdispatcher, minSdk 14 history) are outside this story, except the minSdk row handled by M.7. List them as follow-up in Implementation Notes and do not edit them.

12.2 **`.claude/CLAUDE.md`, "Local Build & Device Notes" (Engine-Librarian-owned).** The Developer **must not edit it**. Delegate to the Engine Librarian (`amp:engine-librarian`) with the exact replacement for the "Build JDK" bullet, adjusted to the observed M2/M4 results as in 12.1, and record the Librarian's confirmation in Implementation Notes. Proposed text:
> - **Build JDK:** Gradle, javac, kotlinc and unit tests run on **JDK 25**, declared in `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=25`). That file takes precedence over both `JAVA_HOME` and any `org.gradle.java.home` (including a user-global one in `~/.gradle/gradle.properties`). Without it, a user-global `org.gradle.java.home` overrides `JAVA_HOME`. If your user-global file sets `org.gradle.java.installations.auto-detect=false`, `org.gradle.java.installations.paths` must include a JDK 25 (e.g. Android Studio's JBR 25). Check with `./gradlew --version` (the "Daemon JVM" line). Gates: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` (JaCoCo enforces per-package LINE floors). Note: a user-global `org.gradle.jvmargs` replaces the repository's jvmargs.

  The rest of the section (`MSYS_NO_PATHCONV`, test-count command, adb screenshots) stays unchanged. The `export JAVA_HOME=… jbr-17` instruction is removed, since it no longer has any effect.

12.3 Write the Implementation Notes in the story. Include:
- the baseline commit (Step 0.1)
- the version table with the **verbatim** citations from this plan (AC-11, AC-15), and SC-5 copied verbatim
- SC-1..SC-5
- baseline vs after: test counts, class counts per package, class-root counts, LINE ratios
- the AC-16 debt list
- the AC-17 statement "no opt-outs"
- the AC-18 per-flag decisions
- the AC-7 M1–M4 results
- the AC-8 decision
- the verification-metadata diff summary
- B1 outcome: "B1 PASS — minSdk unchanged at 21", or the full Branch M record (M.10)

### Step 13 — Cleanup and commit hand-off

- Run `./gradlew --stop`.
- Delete `C:/Code/u060-baseline` and the scratch Gradle homes after the evidence is saved.
- Commit only when the pipeline or user authorizes it, as **one atomic commit** on `sdlc/modernization-plan`. It contains the wrapper, build files, metadata, criteria file, CI, proguard rule and code-location, plus the Branch M files if the branch triggered. The `.claude/CLAUDE.md` change lands through the Engine Librarian.

---

## Files Modified/Created

| File | Change | Owner |
|---|---|---|
| `gradle/wrapper/gradle-wrapper.properties` | 9.5.1 bin URL + `distributionSha256Sum` (bootstrap Step 1, regenerated Step 5) | Developer |
| `gradle/wrapper/gradle-wrapper.jar` | regenerated (sha256 `497c8c2a…a9c7`) | Developer |
| `gradlew` | regenerated (mode 100755 preserved) | Developer |
| `gradlew.bat` | **new tracked file** (was untracked/absent; Gradle 9.5 Windows template) | Developer |
| `gradle/gradle-daemon-jvm.properties` | **new**, `toolchainVersion=25` | Developer (generated by `updateDaemonJvm`) |
| `build.gradle` | AGP/KGP/KSP/Hilt classpath; `layout.buildDirectory`; `updateDaemonJvm` config; comments | Developer |
| `app/build.gradle` | remove `kotlin-android`; `kotlin { compilerOptions }`; proguard-optimize file; dependency bumps; JaCoCo 0.8.14; shared class roots + guard; `layout.buildDirectory`; comments. **Branch M:** minSdk 23, androidx.hilt 1.3.0 (T2: Hilt 2.60.1; T3: Robolectric 4.16.1) | Developer |
| `app/proguard-rules.pro` | append `-dontoptimize` (behavior preservation) | Developer |
| `gradle.properties` | comments only (+ jvmargs only if Step 9 proves a flag harmful) | Developer |
| `gradle/verification-metadata.xml` | additions only, Gradle writer | Developer |
| `.github/workflows/ci.yml` | temurin 25, build-tools 36.0.0 step, comment | Developer |
| `sdlc/artifacts/engagement/code-location.md` | build-environment sections; minSdk row (M.7) | Developer |
| `.claude/CLAUDE.md` | "Build JDK" bullet | **Engine Librarian** |
| `sdlc/artifacts/build/sprints/sprint-017/U-060-…md` | Implementation Notes only (ACs amended by the Requirements Analyst if Branch M triggers) | Developer / Requirements Analyst |
| `app/src/test/java/com/zegoggles/smssync/compat/SmsReceiverTest.java` | **Branch M only:** 4 `@Config` LOLLIPOP→M + comment lines 23–25 | Developer |
| `CHANGES` | **Branch M only:** line 3 → "Android minSdk now 23 (6.0, Marshmallow)" | Developer |
| `app/lint-baseline.xml` | **Branch M only, conditional:** `ObsoleteSdkInt` message refresh per M.5 | Developer |
| `k9mail-vendored/build.gradle`, `metadata/build.gradle`, `settings.gradle` | **expected no change** (only if Step 7.4 reports an error) | Developer |

Without Branch M, no production or test source file changes. With Branch M, the only source-tree change is the 4-line test retarget plus its comment (M.2); no production source changes in either case. Every tracked path above except `sdlc/**` and `.claude/**` appears in `U060_TRACKED`, and both new files appear in `U060_NEW`.

---

## Contracts Verification

`integration_contracts: []`. This story changes no component boundary: it touches build tooling only, with no API, schema or inter-module interface change. A minSdk raise under Branch M is a platform-floor change, not an inter-component interface. The `:app` → `:k9mail-vendored` dependency (`implementation project(...)`, plus the `api` dependencies of the vendored module) is unchanged. The Contract Gate does not apply.

## Standards / Requirements Conformance

The story has `requirements: []`, and the build domain has no REQ, DES or CNTR artifacts. No `type: standard` REQ exists (grep across `sdlc/artifacts/requirements`). The approved **REQ-MODERNIZATION-003** (`type: constraint`) still governs the build, and this plan conforms to it:
- CI keeps running `test lint jacocoTestReport assembleRelease` (its AC-1).
- The release build keeps `minifyEnabled true` (AC-2).
- The JaCoCo gate is preserved and protected (AC-3).
- Robolectric stays ≥ 4.12.x (AC-7a).
- `gradle/verification-metadata.xml` stays present and is regenerated through `--write-verification-metadata` (AC-8).

---

## Risks & Mitigations

| # | Risk | Likelihood / impact | Mitigation / detection |
|---|---|---|---|
| R1 | androidx.hilt-compiler 1.2.0 fails under KSP2 (SC-2) | Medium / High | Checkpoint B1 with explicit pass/fail criteria → **take the pre-approved Branch M (minSdk 23 + androidx.hilt 1.3.0, then 1.4.0)**. No B2 hack, no stop |
| R2 | JaCoCo Kotlin class root moves (`BUILT_IN_KOTLINC`), so coverage silently shrinks | High / High | Step 3.3 B5 discovery, shared root list, fail-fast guard, per-package `<class>` count comparison (AC-9) |
| R3 | `hiltJavaCompileDebug` or app javac gets an `[options]` (or `-proc`) warning on JDK 25 → `-Werror` failure | Medium / Medium | Checkpoint B-javac; scoped `-Xlint:-options` fallback, recorded |
| R4 | JaCoCo 0.8.14 Kotlin filters change LINE totals and push a package below its floor (`di` at 3.1% vs 3%) | Low-Medium / High | Baseline LINE counters vs after; never lower floors; escalate (stop condition) |
| R5 | AGP 9 lint adds new checks, so `lint` fails in CI | Medium / Medium | Step 8.2 local CI-parity run; no silent baseline regeneration; escalate. The only pre-authorised exception is Branch M's `ObsoleteSdkInt` message refresh (M.5) |
| R6 | R8 default changes (`strictFullModeForKeepRules`, optimize file) alter the release APK | Medium / Medium | `-dontoptimize` keeps the prior optimization posture. Project keep rules all use `{ *; }` or keepclassmembers, so strict mode does not affect them. Consumer rules may still be affected, and release is not device-tested in this story: record as residual risk |
| R7 | Verification-metadata churn hides an unexpected artifact | Medium / High (supply chain) | Pristine single-file restore, then one writer pass; additions-only script; manual review of added groups (incl. M.6 list) |
| R8 | Robolectric 4.15.1 has an undocumented JDK 25 issue | Low-Medium / High | ASM 9.8 reads major 69. If it fails, add the documented `--add-opens` flags. If it still fails, **take Branch M trigger T3** (Robolectric 4.16.1 + minSdk 23 + test retarget) |
| R9 | Mockito inline self-attach is blocked in a future JDK | Low (warn only on 25) / Low | Record as debt (Mockito javaagent configuration later) |
| R10 | Daemon criteria behave unexpectedly with `auto-detect=false` | Low / Medium | M1–M4 matrix; document the observed limitation (12.1 written from results) |
| R11 | Hilt 2.59.2 cannot read Kotlin 2.3.21 metadata | Low / High | Its kotlin-metadata-jvm reads up to 2.3.0, and only 2.4.0 metadata is known to fail (#5177/#5190), so Low. Checkpoint B covers it. If it fails, **take Branch M trigger T2** (Hilt 2.60.1 at all 5 sites; minSdk 23 satisfies 2.60) |
| R12 | `dependency.useConstraints=false` (AGP 9 default) changes resolved test-classpath versions | Low / Low | Test pass plus executed-count comparison; optional `:app:dependencies` diff if tests regress |
| R13 | Windows MAX_PATH on deep intermediates in scratch copies | Low / Low | Baseline at `C:/Code/u060-baseline` (short path) |
| R14 | CI cannot be exercised before push; on the runner, the daemon criteria (`toolchainVersion=25`, no download repository) may fail to find a JDK 25 | Certain (unexercised) / Medium | YAML review plus local CI-command parity. Failure signature to watch for on the first PR run: "Cannot find a Java installation on your machine matching the Daemon JVM defined requirements: {languageVersion=25 …}". setup-java's `JAVA_HOME` (JDK 25) is normally used as the launcher and detected. **Containment if it fails:** append `-Dorg.gradle.java.installations.fromEnv=JAVA_HOME_25_X64` (or `-Dorg.gradle.java.installations.paths="$JAVA_HOME"`) to both `./gradlew` lines in `ci.yml`. Both properties are documented in docs.gradle.org/9.5.1/userguide/toolchains.html. Record the outcome |
| R15 | `gradlew.bat` becomes a newly tracked file | Certain / Low | Intentional (AC-14); called out in the commit message |
| R16 | KGP 2.3.21 on Gradle 9.5.1 is above the Kotlin table's "fully supported" Gradle max (9.3.0) (SC-5) | Low / Medium | Gradle 9.5 tests and embeds Kotlin 2.3.20; the Kotlin docs permit newer Gradle with "deprecation warnings or some new features might not work". Detection: Checkpoints A/B, `--warning-mode all`. Warnings are AC-16 debt; any KGP *error* is a stop condition (no version change outside this plan) |

## Quality Attribute Coverage (ISO/IEC 25010)

- **Functional Suitability:** app behavior is unchanged: same compile/target SDK and bytecode 17. AC-5 checks launch on API 36. The test suite (697 `@Test`) must pass with an executed count ≥ baseline. **Branch M exception:** the minSdk floor rises to 23. This is pre-approved and recorded (M.8), with no behavior change for API 23+, and the retargeted tests exercise the same code branch.
- **Performance Efficiency:** build-time only. KSP 2.3.6 includes a Windows KSP2 recompilation fix, and Hilt 2.59.2 fixes slow AGP-9 incremental builds. Release R8 optimization stays disabled (`-dontoptimize`), so APK size and runtime are unchanged. No runtime performance impact is expected.
- **Compatibility:** the core of the story. The version set is chosen so that **both AC-11 sources list it literally at stable versions** (SC-5); the Kotlin table is a secondary check with a documented allowance. Co-existence with user-global Gradle settings is handled by the daemon criteria and documented. Device compatibility is unchanged unless Branch M triggers (API 21–22 then lose updates, M.8).
- **Usability (developer usability):** removes an undocumented, machine-specific prerequisite. `./gradlew --version` shows which JDK is used and why. Docs are updated for the `org.gradle.java.home` / `JAVA_HOME` / criteria precedence, written from observed results.
- **Reliability:** fail-fast JaCoCo class-root guard; checkpoints with explicit pass/fail criteria and pre-specified outcomes; atomic, uncommitted work with an explicit-path rollback that cannot touch pipeline state; the baseline lives in an isolated tree.
- **Security:** dependency verification stays enforced. Additions are made only by the Gradle writer and proven by script. The wrapper `distributionSha256Sum` is present **from the first 9.5.1 download** (Step 1), and the wrapper-jar hash is checked against the official value. No toolchain auto-download, so no foojay and no new network trust. Hilt 2.59.2 and Mockito 5.18.0 (and the Branch M artifacts, if triggered) are supply-chain additions recorded in the metadata.
- **Maintainability:** removes the deprecated `kotlin-android`/`kotlinOptions` and `buildDir`, de-duplicates the JaCoCo class-root lists, and refreshes stale version comments. Remaining deprecations are listed as debt (AC-16). Known future blockers are recorded: Hilt 2.60, androidx.hilt 1.3 and Kotlin 2.4 (via Hilt) need minSdk 23, and Robolectric 4.16 drops SDK 21. Branch M pre-pays those if triggered.
- **Portability:** the build now runs on JDK 25 locally and in CI (temurin 25 on ubuntu-22.04), still within Gradle 9's supported JVM range. The criteria file is OS-neutral (no platform URLs). R14 containment is pre-written for the CI runner.

## Test Strategy

- **Regression:** all existing unit tests run on JDK 25 through `:app:testDebugUnitTest`. Without Branch M there are no test-source changes and `git diff app/src/test` must be empty. With Branch M, exactly the 4 `@Config` retargets plus their comment are allowed (Step 7.5 rule). Executed totals (JUnit XML via the scratch init script) are compared with the JDK 17 baseline.
- **Codegen integrity (B1):** the generated-KSP file-name set matches the baseline, and the Hilt worker factories exist.
- **Coverage-gate integrity:** `jacocoTestCoverageVerification`, plus the per-package class-count and LINE-counter comparison against the baseline, plus the class-root guard.
- **Build integrity:** `assembleDebug`, `assembleRelease`, `lint`, `--warning-mode all`, and `javap` major versions (61/61/52). Under Branch M, also the merged-manifest minSdk check (M.3).
- **Supply chain:** an enforced-verification build after the single writer pass; additions-only script; wrapper hashes (bootstrap and regenerated).
- **Environment:** the M1–M4 Gradle-user-home matrix (AC-7, AC-18).
- **Runtime smoke:** install and launch on the API 36 emulator `Pixel_9_Pro` (AC-5); API 23 too if an image exists under Branch M.
- **CI:** YAML review plus the local CI command; the first PR run confirms (R14 containment ready).

No new automated tests are added. The story is a toolchain change, and its correctness criteria are the existing gates plus the equivalence evidence above.

## Revision Log (iteration 2, responding to plan-review iteration 1)

| Finding | Change | Where |
|---|---|---|
| **B-1** misquoted Gradle compatibility citations; missing interpretation SC | Re-fetched all versioned pages (9.1.0–9.7.1) and the Kotlin table. Gradle row and SC-1 now quote verbatim. Added **SC-5** (no tri-stable triple on any line; the reading applied; residual risk; why not 9.3.x / 9.4.x / 2.4-based). **The re-stated AC-15 argument changes the decision: Gradle 9.3.1 → 9.5.1**, and every dependent value is updated (Step 1, 3.1, 5, 8.1, 12.1, checksums `bafc141b…` / `497c8c2a…`, R16, AC mapping). Added Gradle 9.4/9.5 upgrade-guide review | Version Decisions; SC-1; SC-5; Steps 1, 3.1, 5, 7.4, 8.1, 12.1; R16 |
| **B-2** repo-wide `git restore -- .` | Replaced with one shared definition (`U060_TRACKED`, `U060_NEW`) and an explicit-path procedure. The list is complete (adds `gradlew`, `settings.gradle`, `k9mail-vendored/build.gradle`, `metadata/build.gradle`, plus Branch M files). Daemons are stopped *before* restore. It never touches `sdlc/**` or `.claude/**`; code-location.md is reversed only by hand edit. Step 6.1 restore is now explicit single-path `--worktree` | "Shared definitions"; Step 3.3; Step 6.1 |
| **B-3** product-owner minSdk approval | SC-2 rewritten as option (a) with the verbatim PO quote. New **Branch M** fully specified (triggers T1–T3, M.1–M.10, stop conditions). R1/R8/R11 now route to Branch M. Step 7.5 diff rule, Files table, AC mapping, QA coverage and Test Strategy are made conditional | SC-2; Branch M; Steps 3.3, 7.1, 7.5, 11, 12; Risks |
| **N-1** B2 no-op | B2 **dropped**, not repaired. Evidence: androidx.hilt-compiler 1.3.0 (KSP2-documented) still uses `room-compiler-processing 2.6.0`, so forcing XProcessing targets the wrong component, and the approved Branch M is documented. Pre-specified 1.4.0 second step inside Branch M (it uses XProcessing 2.8.4) | SC-2 (3); Branch M M.1 |
| **N-2** mixed androidx.hilt versions | Not adopted (SC-2 (3)(iii)) | SC-2 |
| **N-3** bootstrap checksum | Step 1 now sets `distributionSha256Sum` with the URL | Step 1 |
| **N-4** HEAD baseline | Step 0.1 records the actual HEAD (`2334fc6d` at planning), does not stop on mismatch, and proves `git diff --stat b09bda5d HEAD -- . ':!sdlc'` is empty | Step 0.1 |
| **N-5** CI daemon-JVM resolution | R14 extended with the failure signature and a documented containment (`installations.fromEnv` / `paths`) | R14; Step 8.1 |
| **N-6** AC-6 wording | 12.1(b)/(c) and 12.2 are written from the observed M2/M4 results; the precedence quote is cited from the 9.5.1 docs | Step 12 |
| **N-7** `-proc` | Added to the B-javac watch list (renamed from "B2-javac") | Step 3.3 |
| B1 criteria | Explicit PASS/FAIL definitions, baseline KSP file list (Step 0.5), and separation of R11 from B1 | Steps 0.5, 3.3 |

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| Plan review (iteration 1) | sdlc/artifacts/build/sprints/sprint-017/U-060/plan-review.md | B-1..B-3, N-1..N-7, PO input |
| Prior plan | sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md (git `2334fc6d`) | Base for targeted revision |
| U-060 story | sdlc/artifacts/build/sprints/sprint-017/U-060-jdk-25-build-toolchain-upgrade.md | ACs AC-1..19, IC-1..3, scope statement, preserve list |
| BUG-017 | sdlc/artifacts/stories/BUG-017-build-fails-on-jdk-25.md | Source bug, advisory approach |
| REQ-MODERNIZATION-003 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-003-establish-ci-coverage-gate.md | Approved constraint: CI command, minify, JaCoCo gate, verification metadata |
| Root build | build.gradle | Plugin pins, `clean`/`buildDir`, Test config (lines 16–27) |
| App build | app/build.gradle | DSL, minSdk (line 26), lint (65–77), deps (152, 174–186), JaCoCo paths/rules, `-Werror`; no `hilt {}` flags, no multidex |
| Vendored build | k9mail-vendored/build.gradle | Java 8 target, minSdk 21 (line 54) |
| Metadata build | metadata/build.gradle | `task lint` (Groovy 4) |
| Settings | settings.gradle | Module list; no toolchain resolver |
| Properties | gradle.properties | jvmargs, R flags, comments |
| Wrapper | gradle/wrapper/gradle-wrapper.properties | 8.14.1, no checksum |
| Verification | gradle/verification-metadata.xml | Header/config; 546 components; junit-bom entries |
| ProGuard | app/proguard-rules.pro | Keep-rule shape vs AGP 9 R8 defaults |
| Lint baseline | app/lint-baseline.xml | 39 issues; 10 `ObsoleteSdkInt` with minSdk-bearing messages (M.5) |
| Robolectric config | app/src/test/resources/robolectric.properties | sdk=29 pin |
| SmsReceiverTest | app/src/test/java/com/zegoggles/smssync/compat/SmsReceiverTest.java | 4 × `@Config(sdk = LOLLIPOP)` at lines 27/32/37/42; comment 23–25 |
| SmsReceiver | app/src/main/java/com/zegoggles/smssync/compat/SmsReceiver.java | `isSmsBackupDefaultSmsApp` SDK branches (46–55) |
| Tests (grep) | app/src/test/** | Configured SDKs {21, 23, 29}; 697 `@Test`; 15 `.kt` tests |
| Main sources (grep) | app/src/main/** | `SDK_INT`/`@TargetApi` sites; no API-22/23 checks; res `-vNN` folders |
| Manifest | app/src/main/AndroidManifest.xml | Launcher `.activity.MainActivity`; no `tools:overrideLibrary` / `uses-sdk` (grep) |
| CI | .github/workflows/ci.yml | JDK 17 (lines 19–26), commands (45, 52) |
| CHANGES | CHANGES | Unreleased `1.6.0 (xxx)` block, stale "minSdk now 14" (line 3) |
| README | README.md | Line 38 stale tested-versions claim |
| Metadata listings (grep) | metadata/** | No Android-version claims |
| Project rules | .claude/CLAUDE.md | Build/device notes; ownership |
| Engagement doc | sdlc/artifacts/engagement/code-location.md | Stale build notes; minSdk row line 28 (AC-6, M.7) |
| User-global (read-only) | ~/.gradle/gradle.properties | java.home, auto-detect, jvmargs (SC-4) |
| Git state | `git rev-parse`, `git log`, `git diff --stat b09bda5d HEAD`, `git ls-files` | HEAD `2334fc6d`; sdlc-only commits since `b09bda5d`; tracked-file list for rollback |
| Templates | plugin cache templates/build-artifacts/{build-plan,gate-report-template}.md | Frontmatter/sections |
| External (re-fetched 2026-11-11) | docs.gradle.org/{9.1.0,9.2.1,9.3.0,9.3.1,9.4.0,9.4.1,9.5.0,9.5.1,9.6.0,9.6.1,9.7.0,9.7.1}/userguide/compatibility.html; docs.gradle.org/9.5.1/userguide/{gradle_daemon,upgrading_version_9,toolchains}.html; services.gradle.org/versions/all and gradle-{9.3.1,9.5.1}-{bin.zip,wrapper.jar}.sha256; kotlinlang.org/docs/gradle-configure-project.html; JetBrains/kotlin releases v2.3.20, v2.3.21; google/ksp releases 2.3.1–2.3.13; google/dagger releases 2.57.2–2.60.1 and issues #5177/#5180/#5190; developer.android.com/jetpack/androidx/releases/hilt; Google Maven androidx.hilt metadata, hilt-{work,compiler,common}-{1.2.0,1.3.0,1.4.0} POMs, hilt-work-{1.3.0,1.4.0}.aar manifests; Maven Central robolectric/dagger metadata; robolectric-4.16 release notes; dl.google.com AGP metadata. Iteration-1 sources retained: AGP 9.0.0 release notes, migrate-to-built-in-kotlin, whatsnew23, JaCoCo changes, ByteBuddy notes, ASM versions, runner-images, setup-java, gradle-api 9.0.1 javap | Version and compatibility evidence |

## Phase Completion Report
---
story_id: "U-060"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
