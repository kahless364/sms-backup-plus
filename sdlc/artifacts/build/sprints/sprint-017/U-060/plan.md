---
artifact_type: plan
story_id: "U-060"
gate_id: "planning"
checkpoint: "plan-gate"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-11"
contracts_verified: []
risks_identified: 15
---

# Implementation Plan: U-060 — Run the build on JDK 25 (Gradle 9.x / AGP 9.x) keeping the Java 17 bytecode target

## Verdict: PASS

The story can be planned and implemented as written. Every version decision below is backed by an official source that was fetched on 2026-11-11. Three items could not be proven without building, and each is a numbered Developer checkpoint with a defined fallback or stop condition: androidx.hilt 1.2.0 under KSP2, the AGP 9 built-in-Kotlin class-output path, and `--system` propagation into `hiltJavaCompileDebug`. Two findings need the orchestrator's attention (see Scope Concerns), but neither blocks planning:
- **SC-1:** The story's premise "Gradle 9.x is tested only with AGP 9.x" is wrong for Gradle 9.1 through 9.4.
- **SC-2:** If androidx.hilt 1.2.0 fails under KSP2, the only fix that does not touch minSdk is blocked by minSdk 21.

---

## Story Overview

Gradle on this machine runs on JDK 25: the user-global `org.gradle.java.home` points at the Android Studio JBR 25.0.4. The pinned toolchain (Gradle 8.14.1, AGP 8.7.3, KGP 2.0.21) cannot compile build scripts on class-file major version 69. The story moves Gradle, javac, kotlinc and the unit-test JVM to JDK 25 as one atomic change. App bytecode stays at 17 and the vendored module stays at 8. compileSdk/targetSdk 35, dependency verification, JaCoCo floors and the test count must all be preserved.

**Why it is one change set:** Gradle 9 is required to run on JDK 25, so AGP 9 is required (per AC-11), and that forces KGP ≥ 2.2.10. Built-in Kotlin then forces KSP ≥ 2.3.1, which is KSP2-only. KSP2 in turn forces the Hilt and androidx.hilt choices. Separately, the JDK 25 test runtime forces the Robolectric, Mockito/ByteBuddy and JaCoCo versions. No intermediate state builds on JDK 25.

---

## Version Decisions (the compatible set)

All sources were fetched on 2026-11-11 unless marked otherwise. "Lowest" means the lowest documented-compatible **minor line**, at the latest **patch** of that line. Patch releases are bug-fix-only within a line, and the compatibility tables state compatibility per line (AC-15 rationale).

| Component | From | **To** | Why this version (and why not lower or higher) | Source |
|---|---|---|---|---|
| Gradle wrapper | 8.14.1 | **9.3.1** (bin, `distributionSha256Sum=b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06`) | JDK 25 needs Gradle ≥ 9.1.0. The **first Gradle whose own compatibility page lists AGP 9.0 as tested is 9.3.0**: 9.1.0 lists AGP 8.4–8.12, 9.2.x lists 8.4–8.13, and 9.3.x lists "8.13 through 9.0". 9.3.x also lists Kotlin "2.0.0 through 2.3". 9.3.1 is the latest 9.3 patch. Going higher is not needed. | docs.gradle.org/{9.1.0,9.2.0,9.2.1,9.3.0,9.3.1}/userguide/compatibility.html; services.gradle.org/distributions/gradle-9.3.1-bin.zip.sha256; wrapper jar sha256 `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13` |
| AGP | 8.7.3 | **9.0.1** | The lowest AGP 9 line, which AC-11 requires (see SC-1 for the AGP 8.13 alternative). AGP 9.0 requires Gradle ≥ 9.1.0, JDK ≥ 17, KGP ≥ 2.2.10 and Build Tools ≥ 36.0.0, and supports max API 36.1. 9.0.1 is the latest 9.0 patch (dl.google.com metadata: 9.0.0, 9.0.1). | developer.android.com/build/releases/agp-9-0-0-release-notes ("Compatibility" table); dl.google.com/.../gradle/maven-metadata.xml |
| Kotlin Gradle plugin | 2.0.21 | **2.3.21** | AGP 9.0 needs ≥ 2.2.10, but **KGP 2.2.x is documented only up to Gradle 8.14 and AGP 8.10–8.11**. The **KGP 2.3.20–2.3.21** row covers Gradle ≤ 9.3.0 and AGP ≤ 9.0.0, the lowest row that covers both lines. Kotlin 2.3.0 adds "Kotlin/JVM: Support for Java 25". KSP 2.3.x is built against KGP 2.3.0. | kotlinlang.org/docs/gradle-configure-project.html (KGP/Gradle/AGP table); kotlinlang.org/docs/whatsnew23.html |
| kotlin-stdlib | 2.0.21 | **2.3.21** | IC-1: matches KGP. | — |
| KSP | 2.0.21-1.0.28 | **2.3.6** | "Added support for AGP 9.0 and built-in Kotlin" first appears in **KSP 2.3.1**, and 2.2.x does not have it. 2.3.6 is chosen over 2.3.2 because it "Fixed an issue where module recompilation would fail on Windows environments when KSP2 was enabled (#2774)" and "Improved the detection of built-in Kotlin" (#2772). Both matter on this Windows build. KSP 2.3.x is KSP2-only (KSP1 deprecated in 2.3.0), and 2.3.6 is built against `kotlinBaseVersion=2.3.0` / `agpBaseVersion=9.0.0`. | github.com/google/ksp/releases (2.3.0, 2.3.1, 2.3.2, 2.3.6); raw.githubusercontent.com/google/ksp/2.3.6/gradle.properties |
| Hilt (plugin, `hilt-android`, `hilt-compiler`, `hilt-android-testing`; all identical) | 2.51.1 | **2.59.2** | **Dagger 2.59** is the first release with AGP 9 support in the Hilt Gradle plugin, and it *requires* AGP 9. 2.59.1 fixed jetifier-on-AGP-9. 2.59.2 replaced the "android-classes" transform and fixed slow incremental builds from HiltSyncTask on AGP 9. **2.60 is excluded: "minSDK for Hilt is now 23"**, and the app is minSdk 21. hilt-android 2.59.2's AAR manifest says minSdkVersion 14. dagger-compiler 2.59.2 uses KSP API 2.2.20-2.0.3 and kotlin-metadata-jvm 2.2.20. | github.com/google/dagger/releases (2.58–2.60.1); repo1 POMs/AAR for 2.59.2 |
| androidx.hilt (`hilt-work`, `hilt-compiler`) | 1.2.0 | **1.2.0 (kept), conditional** | 1.3.0+ is the line documented for KSP2 ("Updates the library and annotation processor to target Kotlin 2.0 to support newer Kotlin toolchain including KSP2", 1.3.0-alpha01). **However, `hilt-work-1.3.0.aar` declares `minSdkVersion="23"`**, which would break manifest merge at minSdk 21. 1.2.0 (room-compiler-processing 2.6.0, KSP API 1.9.22) is kept and must pass Checkpoint B. The fallback chain is in SC-2. | developer.android.com/jetpack/androidx/releases/hilt; hilt-work-1.3.0.aar `AndroidManifest.xml`; hilt-compiler-1.2.0.pom |
| JaCoCo | 0.8.12 | **0.8.14** | "JaCoCo now officially supports Java 25" first appears in 0.8.14. 0.8.13 had only experimental support. | jacoco.org/jacoco/trunk/doc/changes.html |
| Robolectric | 4.12.2 | **4.15.1** | **4.16 "removes support for Android L (SDK 21 and 22)"**, and `SmsReceiverTest` configures `sdk = Build.VERSION_CODES.LOLLIPOP` (4 uses). 4.15.x is therefore the newest line that covers the configured SDK set {21, 23, 29}, which satisfies AC-10. 4.15 moved to **ASM 9.8**, the first ASM with `Opcodes.V25`. Robolectric's class-node provider can read JDK classes, and on a JDK 25 runtime those are major 69. 4.12.2 uses ASM 9.7 (Java 23 max). | github.com/robolectric/robolectric/releases (4.15, 4.15.1, 4.16); sandbox-4.15.1.pom (asm 9.8); asm.ow2.io/versions.html |
| Mockito (`mockito-core`) | 5.14.2 | **5.18.0** | The lowest Mockito that bundles **ByteBuddy 1.17.5**, which is "Update ASM to version 9.8 to support Java 25". 5.14.2 bundles 1.15.4 (Java 24). 5.15.2–5.17.0 bundle 1.15.11. | mockito-core-{5.14.2..5.20.0}.pom; raphw/byte-buddy release-notes.md |
| SDK Build Tools | (AGP default) | **36.0.0** (AGP 9.0 minimum and default) | Installed locally (`Sdk/build-tools/36.0.0`) and on the GitHub `ubuntu-22.04` image ("36.0.0 36.1.0"). | AGP 9.0 notes; actions/runner-images Ubuntu2204-Readme.md |
| Daemon JDK declaration | none | **`gradle/gradle-daemon-jvm.properties` with `toolchainVersion=25`** | Gradle docs: "If the Daemon JVM criteria configuration is provided, it takes precedence over JAVA_HOME and org.gradle.java.home". This fixes the root cause (an undeclared, user-global JDK) in a repository-tracked way (AC-7). | docs.gradle.org/9.3.1/userguide/gradle_daemon.html |
| CI JDK | temurin 17 | **temurin 25** | Gives parity: the same Robolectric, ByteBuddy and JaCoCo code paths as local builds. The runner image ships `JAVA_HOME_25_X64`, and `actions/setup-java@v4` accepts `java-version: '25'`. | runner-images readme; setup-java README |

**Unchanged on purpose:** coroutines 1.7.3, lifecycle 2.6.2, work 2.9.1, truth, junit 4.13.2, greenmail 2.1.3, `auto-service`, billing, security-crypto, k9mail-vendored dependencies. None of them is forced by the toolchain, and moving them would widen the verification diff (AC-15 spirit).

---

## Scope Concerns (for orchestrator / product owner)

- **SC-1 (premise correction, non-blocking).** The story's Technical Context and AC-11 say "Gradle 9.x is tested only with AGP 9.x". That is only true of the *current* (9.9.1) page. The versioned pages show Gradle 9.1.0 "tested with AGP 8.4 through 8.12", 9.2.x "8.4 through 8.13", and 9.3.x / 9.4.x "8.13 through 9.0/9.1". A documented Option A therefore exists (AGP 8.13 + Gradle 9.3.1 + KGP 2.3.21 + Hilt ≤ 2.58). **This plan still selects AGP 9.0.1, as AC-11 requires:**
  - Option A still needs KGP 2.3.x, because KGP 2.2.x is not documented for Gradle 9. That means KSP 2.3.x / KSP2, so it carries **the same androidx.hilt/KSP2 risk** (SC-2) and removes none of the biggest unknown.
  - AGP 8.x documents no JDK 25 runtime support either.
  - 8.13 is the final 8.x line. AGP 9's own notes say opt-outs are removed in AGP 10, so Option A would have to be redone.

  Record this correction in Implementation Notes. No AC change is required.
- **SC-2 (conditional blocker; triggers only if Checkpoint B fails).** KSP ≥ 2.3.1 is mandatory under AGP 9 built-in Kotlin and is KSP2-only. androidx.hilt documents KSP2 support only from 1.3.0, whose `hilt-work` requires minSdk 23. If androidx.hilt-compiler 1.2.0 fails under KSP2 and Fallback B2 also fails, **stop and escalate** with these options. Do not implement any of them without a decision:
  - (a) raise minSdk to 23. This is a product change and also breaks the LOLLIPOP Robolectric tests.
  - (b) androidx.hilt 1.3.0 with `tools:overrideLibrary` for `androidx.hilt.work`. This is a runtime risk on API 21–22.
  - (c) replace `HiltWorkerFactory` with a hand-written `WorkerFactory`. This is a code change across BackupWorker, RestoreWorker, CheckpointModule and App.

  Do not use `android.builtInKotlin=false`/kapt as an escape. It does not help, because KGP 2.2.x / KSP1 are not documented for Gradle 9.
- **SC-3 (pre-existing defect found during planning).** On this machine, HEAD **fails dependency verification on JDK 17 too**: `./gradlew help -Dorg.gradle.java.home=<JBR17>` reports "2 artifacts failed verification: junit-bom-5.9.2.module, junit-bom-5.9.3.module (configuration 'classpath')". The metadata holds only the `.pom` checksums for those two versions. This does not change the plan; the regeneration in Step 6 covers it if those modules are still in the graph. It does affect how the baseline is captured (Step 0.4) and must be recorded in Implementation Notes as a pre-existing gap.
- **SC-4 (informational).** The user-global `~/.gradle/gradle.properties` sets its own `org.gradle.jvmargs`. User-home properties take precedence over project properties, so **on this machine the repository's `org.gradle.jvmargs` (AC-18 flags) are never applied**. AC-18 revalidation must use a scratch Gradle user home (Step 9). This interaction belongs in the AC-6 documentation.

The story fits one sprint item; no split is needed.

---

## Acceptance Criteria Mapping

| AC | Implementation step(s) | Evidence the Developer records |
|----|-------------------|-------|
| AC-1 | Steps 1–5, 10 | `./gradlew --version` output (Launcher and Daemon JVM 25, "from gradle/gradle-daemon-jvm.properties"); `assembleDebug` exit 0; grep shows no "major version 69" |
| AC-2 | Step 0.2/0.5, Step 7 | `@Test` count ≥ 697 (baseline confirmed at HEAD during planning); JUnit-XML totals (via init script) failures=0, errors=0, executed ≥ baseline; `git diff --stat HEAD -- app/src/test` empty |
| AC-3 | Step 6, Step 11 | grep for `dependency-verification\|verify-metadata` across build files, properties, CI and docs; enforced run green; additions-only script output |
| AC-4 | Step 3.4, Step 11 | `git diff HEAD -- app/build.gradle` shows every `minimum =` and `includes =` line unchanged; no new `excludes` entries in `jacocoFileFilter` |
| AC-5 | Step 10 | adb install/launch transcript; `pidof`; `logcat` grep shows no `FATAL EXCEPTION`; screenshot |
| AC-6 | Step 12 | `.claude/CLAUDE.md` edit routed through the Engine Librarian; `code-location.md` "Build Environment" section |
| AC-7 | Step 4, Step 9 | criteria file; scratch-user-home matrix (4 runs) proving precedence and auto-detect=false behavior |
| AC-8 | Step 8 | `ci.yml` diff (JDK 25, Build Tools 36.0.0 step, corrected comment) |
| IC-1 | Step 2 | Version table in Implementation Notes; `:app:dependencies` excerpt showing a single Hilt version and stdlib 2.3.21 |
| IC-2 | Step 2.3, Step 7.3 | vendored module still `VERSION_1_8`; `javap` major 52; `-Werror` scope proof |
| IC-3 | Step 2 | grep shows zero `kotlin-android`, `kotlinOptions`, `buildDir` left in the 4 build files |
| AC-9 | Step 0.6, Step 7.2 | per-package `<class>` counts, baseline vs after (script output), class-root guard |
| AC-10 | Step 2.2, Step 7 | versions table; test totals; no ASM/ByteBuddy/JaCoCo/class-version errors in the test log |
| AC-11 | Version Decisions table | citations copied into Implementation Notes |
| AC-12 | Step 7.3 | `javap -v` major 61 (one Java and one Kotlin app class), 52 (vendored) |
| AC-13 | Step 6 | writer-mode provenance, additions-only script, enforced re-run |
| AC-14 | Step 5 | `./gradlew wrapper ...` transcript; `distributionSha256Sum`; wrapper jar sha256 = official |
| AC-15 | Version Decisions table | rationale copied into Implementation Notes |
| AC-16 | Step 7.4 | `--warning-mode all` log; error-class items fixed; the rest listed as debt |
| AC-17 | Step 2 (no opt-outs) | statement "no AGP opt-out flags introduced" plus grep evidence |
| AC-18 | Step 9 | scratch-user-home run with repo jvmargs on JDK 25; retain/remove decision per flag; comments updated |
| AC-19 | Step 2.5, Step 11 | grep: `compileSdkVersion 35`, `targetSdkVersion 35`, both R flags present, no `enableJetifier`, no `org.gradle.java.home` in repo |

---

## Implementation Steps

Shell rules for every step: use Git Bash with **no `MSYS_NO_PATHCONV`** for Gradle, and `unset MSYS_NO_PATHCONV` first. Use repo root `C:/Code/Android/sms-backup-plus`. Keep every evidence file under `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/`, except scratch copies, which go to the session scratchpad or `C:/Code/u060-*`.

### Step 0 — Baseline capture BEFORE any change (AC-2, AC-9, SC-3)

0.1 **Record the starting point.** Run `git rev-parse HEAD` (expect `b09bda5d…`). Run `git status --short -- build.gradle app k9mail-vendored metadata gradle gradle.properties settings.gradle .github` and confirm it is clean.

0.2 **Test count.** Run `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`. Planning measured **697**; record the value you get.

0.3 **Isolated baseline tree.** Leave the working tree alone and do not change git state. Use a short path to stay clear of Windows MAX_PATH:
```bash
mkdir -p /c/Code/u060-baseline && git -C /c/Code/Android/sms-backup-plus archive HEAD | tar -x -C /c/Code/u060-baseline
cp /c/Code/Android/sms-backup-plus/local.properties /c/Code/u060-baseline/
```

0.4 **Run on JDK 17 without touching `~/.gradle/gradle.properties`.** Command-line system properties have the highest precedence (docs.gradle.org build_environment, "Command line > Gradle properties"). **Planning verified this works on this machine:** `./gradlew --version -Dorg.gradle.java.home=C:/Users/Michael.Horsley/.jdks/jbr-17.0.14` printed `Daemon JVM: C:\Users\Michael.Horsley\.jdks\jbr-17.0.14 (from org.gradle.java.home)`.
```bash
cd /c/Code/u060-baseline
export JAVA_HOME="C:/Users/Michael.Horsley/.jdks/jbr-17.0.14"
J17=-Dorg.gradle.java.home=C:/Users/Michael.Horsley/.jdks/jbr-17.0.14
./gradlew --version $J17 | tee <evidence>/baseline-gradle-version.txt     # proof of JDK
```
Because of SC-3, first close the pre-existing verification gap **in the baseline copy only**, using Gradle's writer. This leaves verification enforced for the measured run. The checksums do not influence compiled classes or coverage.
```bash
./gradlew $J17 --write-verification-metadata sha256 --continue help :app:assembleDebug :app:testDebugUnitTest lint assembleRelease
```
Save `git diff --no-index <repo>/gradle/verification-metadata.xml gradle/verification-metadata.xml` as `evidence/baseline-preexisting-verification-gap.diff`. It documents SC-3.

0.5 **Measured baseline run (verification enforced).** Write this init script to the scratchpad, not the repo. Root `build.gradle` disables JUnit XML, so the script re-enables it for measurement:
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

0.6 **Baseline coverage inventory (AC-9).** Use the script below; save it to scratch, run it with `python -I`, and output JSON to evidence:
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
Input: `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml`. Also record the `.class` counts per class root:
```bash
for d in intermediates/classes/debugUnitTest/transformDebugUnitTestClassesWithAsm/dirs intermediates/javac/debug/compileDebugJavaWithJavac/classes tmp/kotlin-classes/debug; do
  echo "$(find app/build/$d -name '*.class' | wc -l) $d"; done | tee <evidence>/baseline-class-roots.txt
```
Also save the JUnit-XML totals: sum the `tests`, `failures`, `errors` and `skipped` attributes over `app/build/test-results/testDebugUnitTest/*.xml`.

**Checkpoint 0 (stop if not met):** the baseline gates passed on JDK 17, and the inventory JSON, class-root counts and test totals are saved. If the baseline gates fail on JDK 17 for a reason other than SC-3, stop and report. The baseline is not trustworthy.

### Step 1 — Bootstrap the wrapper URL (temporary; regenerated in Step 5)

The old Gradle cannot run the `wrapper` task. It fails on JDK 25, and on JDK 17 the root project cannot configure because of SC-3. In `gradle/wrapper/gradle-wrapper.properties`, set `distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip` only, so the next `./gradlew` invocation runs Gradle 9.3.1. AC-14's "regenerated via `./gradlew wrapper`" is met in Step 5, which executes on 9.3.1.

### Step 2 — Atomic build-script change (IC-1, IC-3, AC-10, AC-12, AC-17, AC-19)

**2.1 `build.gradle` (root)**
- classpath: `com.android.tools.build:gradle:9.0.1`, `org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21`, `com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6`, `com.google.dagger:hilt-android-gradle-plugin:2.59.2`. Keep the KGP classpath line: AGP 9 docs say to declare it to use a KGP higher than AGP's runtime 2.2.10.
- Rewrite the comments on lines 8–17 to the new versions, with sources (no stale "AGP 8.7.3 / JDK 17").
- `clean` task: `delete rootProject.buildDir` → `delete rootProject.layout.buildDirectory`. The `task clean(type: Delete)` syntax may stay unless `--warning-mode all` reports it as an error.
- Add the configuration needed for AC-7 generation (no URL generation, so no toolchain-download repository and no foojay):
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
- **`getDefaultProguardFile('proguard-android.txt')` → `getDefaultProguardFile('proguard-android-optimize.txt')`.** AGP 9 flips `android.r8.proguardAndroidTxt.disallowed` to true. To **preserve current release semantics** (the app was built with `-dontoptimize`), append to `app/proguard-rules.pro`:
  ```
  # U-060: AGP 9 disallows proguard-android.txt; keep pre-AGP-9 release behaviour (no R8 optimisation).
  # Enabling optimisation is a separate, release-tested change (recorded debt).
  -dontoptimize
  ```
  The AGP 9 notes recommend exactly this ("explicitly specify -dontoptimize in a custom proguardFile … alongside using proguard-android-optimize.txt"). Do **not** use the `android.r8.proguardAndroidTxt.disallowed=false` opt-out (AC-17).
- Dependencies: `kotlin-stdlib:2.3.21`; `robolectric:4.15.1`; `mockito-core:5.18.0`; `hilt-android`, `hilt-compiler` (both `ksp` and `kspTest`) and `hilt-android-testing` all at `2.59.2`; androidx.hilt stays `1.2.0` (subject to Checkpoint B).
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
  Use `jacocoClassRootFiles.asm`, `.javac` and `.kotlin` in the three `fileTree(dir: …)` entries, keeping **exactly** the same `excludes` lists as today. Add `doFirst { assertJacocoClassRoots() }` to both `jacocoTestReport` and `jacocoTestCoverageVerification`. Also change `destinationFile = file("${buildDir}/jacoco/testDebugUnitTest.exec")` and both `executionData` paths to `layout.buildDirectory.file('jacoco/testDebugUnitTest.exec')`. The `.get().asFile` form is fine for `destinationFile`, and `files(provider)` works for `executionData`.
- **Leave unchanged:** `-Werror -Xlint:unchecked` (unless Checkpoint B2-javac triggers), every `violationRules` rule, `jacocoFileFilter`, `excludes = ['jdk.internal.*']`, `includeNoLocationClasses`, `testOptions`, `lint {}`, `packaging {}`, `compileSdkVersion 35`, `targetSdkVersion 35`, `minSdkVersion 21`. Planning confirmed that `compileSdkVersion(int)`, `minSdkVersion(int)`, `targetSdkVersion(int)`, `Packaging.exclude(String)` (deprecated), `LintImpl.disable(String...)`, `NdkOptions.abiFilters(String...)` and `BuildType.proguardFiles(Object...)` all still exist in AGP 9.0.1 (javap of `gradle-api-9.0.1.jar` / `gradle-9.0.1.jar`). Expect deprecation warnings, not errors (AC-16 debt).
- Update stale comments to match reality (AC-18): lines 3–8, 85–88, 94, 101–103, 114, 174–178, 199–201, 249, 483 ("generated by kapt" → KSP), 574–576.

**2.3 `k9mail-vendored/build.gradle` (IC-2)**
- **No functional change.** `VERSION_1_8` stays.
- Planning proved that JDK 25 `javac -source 8 -target 8` exits 0 with "[options] source value 8 is obsolete…" warnings. It fails only with `-Werror` (scratch test with JBR 25.0.4).
- `-Werror` reaches only `:app`. It is set by `tasks.withType(JavaCompile)` inside `app/build.gradle`, which is project-scoped (scope verified via grep: `-Werror` appears only at `app/build.gradle:202`).
- The obsolete-option warning is recorded as AC-16 debt. Do not re-add `android.javaCompile.suppressSourceTargetDeprecationWarning`; it is cosmetic and would be a new flag.
- Optionally update the "AGP 8" wording on comment lines 31 and 113. This is not required.

**2.4 `metadata/build.gradle`, `settings.gradle`:** no change unless Step 7.4 reports an *error*. Gradle 9 removes no construct used there: `task` syntax and `buildDir` are deprecated, not removed (upgrading_major_version_9 page). The plain `metadata` project gets `:metadata:lint` from root `lint` in CI. Its Groovy-4 behavior is checked in Step 8.3.

**2.5 `gradle.properties` (AC-18, AC-19)**
- Keep `android.useAndroidX`, `android.nonTransitiveRClass=true` and `android.nonFinalResIds=true`. AGP 9 does not list them among removed or enforced properties.
- Do not add `android.builtInKotlin`, `android.newDsl` or any `org.gradle.java.home`.
- Update the comments on lines 23 and 31–36: "AGP 8 defaults" becomes "defaults since AGP 8, unchanged in AGP 9", and the JDK 17 text is reworded to describe JDK 25. The vendored Java 8 module now *does* emit the obsolete-source warning (without -Werror).
- `org.gradle.jvmargs`: decide in Step 9.

**Explicitly not done (AC-17):** no `android.builtInKotlin=false`, no `android.newDsl=false`, no `android.r8.*` opt-outs, no kapt, no `legacy-kapt`, no `java { toolchain }`, and no foojay.

### Step 3 — First run in writer mode, then checkpoints A–B (de-risk the unknowns early)

New artifacts are not yet in the metadata, so the first compile attempts run in Gradle's writer mode. Writer output is **discarded and regenerated cleanly** in Step 6, so exploratory iterations do not leave stale entries behind.

3.1 Run `./gradlew --version`. Expect `Gradle 9.3.1`, Launcher JVM 25 and Daemon JVM 25. The daemon is the JBR, via the user-global `org.gradle.java.home` for now.

3.2 **Checkpoint A (configuration).** Run `./gradlew --write-verification-metadata sha256 help --warning-mode all`. Any *configuration error* must be fixed here. Classify each failure as one of:
- (i) a construct that Gradle 9 or AGP 9 treats as an error: fix it (AC-16)
- (ii) a plugin incompatibility: check the version table, and stop if a version change is needed that is not in this plan

3.3 **Checkpoint B (compile and codegen).** Run `./gradlew --write-verification-metadata sha256 :app:assembleDebug --continue`. Then:
- **B1, androidx.hilt 1.2.0 under KSP2 (SC-2).** `kspDebugKotlin` and `hiltJavaCompileDebug` must succeed. Generated code must exist for each `@HiltWorker`: `find app/build/generated/ksp/debug -name "*BackupWorker*" -o -name "*RestoreWorker*"`. Expect `*_AssistedFactory*` and `*_HiltModule*` sources. Compare the names with the baseline tree's `app/build/generated/ksp/debug` (same file-name set).
  - **Fallback B2:** in `app/build.gradle`, constrain only the processor configurations to a KSP2-capable `androidx.room:room-compiler-processing`:
    ```groovy
    configurations.matching { it.name in ['ksp', 'kspDebug', 'kspTest', 'kspDebugUnitTest'] }.configureEach {
        resolutionStrategy.force 'androidx.room:room-compiler-processing:<lowest 2.7.x>'
    }
    ```
    Before using it, verify against Maven metadata and the Room release notes that the chosen version documents KSP2 support. It must not reach any runtime classpath. Re-run B1 and record the evidence.
  - **If B2 fails, stop** (SC-2 escalation). Restore with `git restore --source=HEAD --staged --worktree -- .` for the files listed in this plan, and delete the untracked new files.
- **B2-javac, `--system` propagation.** Planning proved that javac 25 with `-source 17 -target 17` and no system-modules location emits "[options] location of system modules is not set…", which fails under `-Werror`. AGP passes `--system` (JdkImageTransform) to `compileDebugJavaWithJavac`, but `hiltJavaCompileDebug` is a Hilt-owned JavaCompile that also receives `-Werror` (its name has no "test"). If either task fails with an `[options]` warning:
  - Primary fix: confirm the version set is as planned.
  - Fallback: add `'-Xlint:-options'` to the app's non-test `compilerArgs`. It suppresses **only** command-line-option warnings, not type-safety warnings. Record it in Implementation Notes as a justified, scoped change that reverses part of U-057.
- **B3, JdkImageTransform/jlink on JDK 25.** If `:app:compileDebugJavaWithJavac` fails in the jlink transform, stop. No supported workaround fits this plan; an AGP minor bump would require re-justifying AC-15. Report it.
- **B4, Kotlin sources under `src/*/java`.** Built-in Kotlin must compile the 31 main and 15 test `.kt` files that live in `java/` directories. Check that `BackupWorker.class` and `SyncStateRepository`-family classes exist in the Kotlin output. **Fallback:** `android { sourceSets { main { kotlin.srcDirs += 'src/main/java' }; test { kotlin.srcDirs += 'src/test/java' } } }`. This is the only supported way under built-in Kotlin, per the migrate guide.
- **B5, locate the class roots (AC-9).** List every directory that holds project classes:
  ```bash
  find app/build -type d -path "*/com/zegoggles/smssync" | sed 's#/com/zegoggles/smssync$##' | while read d; do echo "$(find "$d" -name '*.class' | wc -l) $d"; done
  ```
  Planning found an AGP 9.0.1 internal artifact type `BUILT_IN_KOTLINC` (`InternalArtifactType$BUILT_IN_KOTLINC`). The Kotlin output is therefore **expected to move** from `tmp/kotlin-classes/debug` to an `intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes`-style path. This is unverified until the build runs. Put the observed paths into `jacocoClassRootPaths` (Step 2.2). Confirm that the ASM path and the javac path still exist.

**Rollback at any checkpoint:** nothing is committed. Run `git restore --source=HEAD --worktree -- build.gradle app/build.gradle app/proguard-rules.pro gradle.properties gradle/wrapper gradle/verification-metadata.xml .github/workflows/ci.yml sdlc/artifacts/engagement/code-location.md`, then `rm -f gradle/gradle-daemon-jvm.properties gradlew.bat`, then `./gradlew --stop`. The baseline tree at `C:/Code/u060-baseline` is unaffected. Use `git stash push -- <paths>` instead if you want to keep partial work.

### Step 4 — Repository JDK declaration (AC-7)

Run `./gradlew updateDaemonJvm --jvm-version=25`. Expect `gradle/gradle-daemon-jvm.properties` with `toolchainVersion=25`, no `toolchainUrl.*` lines, and the "generated by updateDaemonJvm" header. Re-run `./gradlew --version` and expect the Daemon JVM line to cite the criteria file. If Gradle labels the feature incubating, record that in Implementation Notes.

### Step 5 — Wrapper regeneration (AC-14)

This runs on Gradle 9.3.1 because of the URL bootstrap. Verification is still in writer mode until Step 6, or run this step after Step 6.
```bash
./gradlew wrapper --gradle-version 9.3.1 --distribution-type bin \
  --gradle-distribution-sha256-sum b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06
sha256sum gradle/wrapper/gradle-wrapper.jar   # must equal b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13 (official gradle-9.3.1-wrapper.jar.sha256)
git diff --summary -- gradlew gradle/wrapper   # gradlew must keep mode 100755
```
**Note:** `gradlew.bat` is **not tracked today** (`git ls-files` returns nothing for it). The wrapper task creates it, and AC-14 names it, so add it as a new tracked file. Do not run `./gradlew wrapper` a second time without the checksum flag; confirm that `distributionSha256Sum` remains in the properties file.

### Step 6 — Clean verification-metadata regeneration (AC-3, AC-13)

1. `git show HEAD:gradle/verification-metadata.xml > <scratch>/vm-head.xml`, then `git restore --source=HEAD -- gradle/verification-metadata.xml`. This discards exploratory writer output from Step 3.
2. Run one writer pass over every configuration CI and the gates touch, from this trusted network:
   `./gradlew --write-verification-metadata sha256 --continue clean help test lint jacocoTestReport assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`
3. Run the additions-only proof. The script below goes in scratch and is run with `python -I`:
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
   **Pass condition:** configuration unchanged (`verify-metadata` true, `verify-signatures` false, the same two trusted-artifact regexes), 0 removed or altered, added > 0.
4. Review the added components. Each must belong to the version changes in this plan, their transitive dependencies, or the SC-3 junit-bom `.module` gap. Flag anything unexpected in Implementation Notes.
5. **Enforced re-run (no writer):** `./gradlew :app:assembleDebug` must pass (AC-13). The full gate run in Step 7 then proves completeness for all gate configurations.

Note on scope: Robolectric downloads `android-all-instrumented` jars at test runtime through its own resolver (into `~/.m2`). They are outside Gradle verification both before and after this change, so this is not a regression. Record it as an observation.

### Step 7 — Gates on JDK 25 (AC-1, AC-2, AC-4, AC-9, AC-10, AC-12, AC-16)

7.1 Run:
- `./gradlew --version > evidence/after-gradle-version.txt`
- `./gradlew -I <scratch>/junitxml.init.gradle clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification 2>&1 | tee evidence/after-gates.log`
- `./gradlew :app:jacocoTestReport`

The first gate command must exit 0. `grep -c "major version 69" evidence/after-gates.log` must be 0. Also grep the log for `UnsupportedClassVersion|Unsupported class file|ByteBuddy|IllegalArgumentException: Unsupported|jacoco.*(Error|Exception)|InaccessibleObjectException`, and record the hits (expected: none).
- If Robolectric raises `InaccessibleObjectException`, add the specific `--add-opens` jvmArgs to `testDebugUnitTest`. This is Robolectric's documented JDK ≥ 17 configuration, not an opt-out. Record it.
- Mockito's "self-attaching … inline-mock-maker" and JDK dynamic-agent warnings are expected on JDK 21+. Record them as debt; they do not fail.

7.2 **AC-9 comparison.** Run `jacoco_inventory.py` on the new XML, then compare it with the baseline JSON. **Pass:** for every baseline package, `after.classes >= baseline.classes`, and no baseline package is missing. Record both tables and the class-root counts (Step 0.6 command with the new paths).

The JaCoCo 0.8.13/0.8.14 Kotlin filters (inline functions, elvis/safe-call) can legitimately change *line totals* for Kotlin packages. If a package's LINE ratio falls below its floor **only** because of counting changes, **do not lower the floor** (AC-4). Stop and escalate. Watch `di` especially: measured 3.1% (1/32) against a 0.03 floor, and the package has 9 `.kt` files.

7.3 **AC-12 / IC-2 bytecode.** Run `javap -v` on:
- `App.class` in the javac root: expect `major version: 61`
- one Kotlin class (for example `service/BackupWorker.class`) in the Kotlin root: expect 61
- `k9mail-vendored/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/fsck/k9/mail/Address.class`: expect 52

7.4 **AC-16.** Run `./gradlew :app:assembleDebug --warning-mode all 2>&1 | tee evidence/warning-mode-all.log`. Fix anything reported as an error or as "will fail". List every remaining deprecation in Implementation Notes. Expected debt includes:
- Groovy space-assignment (`namespace '…'`, `minifyEnabled true`, `url "…"`)
- `packaging.exclude`
- `task` syntax
- vendored "source/target value 8 is obsolete"
- Mockito self-attach
- any AGP DSL deprecations

7.5 Re-count `@Test` (≥ 697). Compare the JUnit-XML totals with the baseline: executed ≥ baseline, failures = errors = 0, skipped ≤ baseline. Confirm `git diff --stat HEAD -- app/src/test` is empty.

**Checkpoint C:** all of 7.1–7.5 hold. Otherwise roll back (Step 3) or escalate.

### Step 8 — CI parity and `ci.yml` (AC-8, REQ-MODERNIZATION-003 AC-1/AC-2)

8.1 Edit `.github/workflows/ci.yml`:
- `Set up JDK 25` with `java-version: '25'` and `distribution: 'temurin'`. `actions/setup-java@v4` supports it; leave the action version alone.
- Replace the stale comment with: "Gradle 9.3 / AGP 9.0 run on JDK 17+; this project builds and tests on JDK 25, declared in `gradle/gradle-daemon-jvm.properties`. AGP 9.0 requires SDK Build Tools ≥ 36.0.0."
- Add a deterministic step before the Gradle run: `"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "build-tools;36.0.0"`. The image ships 36.0.0 today, and the step guards against image drift.
- Leave the Gradle commands (`test lint jacocoTestReport assembleRelease` and `:app:jacocoTestCoverageVerification`) and the caches as they are.

8.2 Run the CI command locally: `./gradlew test lint jacocoTestReport assembleRelease`, then `./gradlew :app:jacocoTestCoverageVerification`. Compare the result with the baseline Step 0.5 `lint`/`assembleRelease` outcome.
- **New lint findings from AGP 9's lint** must not be silenced by regenerating `lint-baseline.xml` or adding `disable` entries without review. List them in Implementation Notes and escalate as a Scope Concern if they fail `warningsAsErrors`.
- `assembleRelease` must succeed with `minifyEnabled true` (REQ-MODERNIZATION-003 AC-2).
- Note the AGP 9 behavior change `android.onlyEnableUnitTestForTheTestedBuildType=true`: `./gradlew test` no longer runs `testReleaseUnitTest`. Record that CI's `test` now runs the debug unit tests only. This is accepted AGP 9 default behavior and not an opt-out. The local gate already used `testDebugUnitTest`.

8.3 Check that `:metadata:lint` (Groovy 4) behaves the same as in the baseline.

CI cannot be executed from this machine without a push. Record the YAML diff as evidence, and note that the first CI run on the PR is the remaining verification.

### Step 9 — AC-7 fresh-clone matrix and AC-18 jvmargs (scratch Gradle user homes; no user-global edits)

Use `-g <dir>` (`--gradle-user-home`) to simulate machines. Each fresh home also re-downloads and **verifies the wrapper zip against `distributionSha256Sum`**, which is additional AC-14 evidence. Run `./gradlew -g <home> --version` (plus `help` for M1) for each case:

| Run | `<home>/gradle.properties` | Expected |
|---|---|---|
| M1 | none (fresh clone) | Daemon JVM = a detected JDK 25 (Oracle 25.0.5 or JBR), "from gradle/gradle-daemon-jvm.properties"; `help` succeeds. **This run also applies the repo's `org.gradle.jvmargs`** (SC-4), which validates AC-18 |
| M2 | `org.gradle.java.home=C:/Users/Michael.Horsley/.jdks/jbr-17.0.14` | Daemon JVM is still 25. This proves criteria take precedence over `org.gradle.java.home` |
| M3 | `org.gradle.java.installations.auto-detect=false` + `org.gradle.java.installations.paths=C:/Program Files/Java/jdk-25.0.5` | Daemon JVM = 25.0.5 |
| M4 | `org.gradle.java.installations.auto-detect=false`, no paths | Record the actual behavior (an error naming the unmet criteria is expected). This is the **documented limitation** for AC-7: with auto-detect off, a JDK 25 must be listed in `org.gradle.java.installations.paths` |

The current machine configuration (auto-detect=false, paths = Android Studio JBR 25) is effectively M3 with the JBR; Step 7 already proves it.

**AC-18 decision rule per flag:** if M1 starts and builds `help` with `-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider` and `-Djdk.net.unixdomain.tmpdir=/nonexistent` in place, **retain** both. Planning verified that `sun.nio.ch.WindowsSelectorProvider` still exists in JDK 25.0.4 (`javap --module java.base`). Before writing the stated reason, look up why the flags were added: `git log -S"WindowsSelectorProvider" --oneline -- gradle.properties`. Remove a flag only if it demonstrably breaks the daemon on JDK 25, and record the reason. Update the line 9 comment to describe what each flag does.

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
Per `.claude/CLAUDE.md`, set `MSYS_NO_PATHCONV=1` only in an adb-only shell if you use any `/sdcard/...` path. Never set it in a Gradle shell.

### Step 11 — Static compliance sweep (AC-3, AC-4, AC-17, AC-19, IC-3)

Run each of the following and paste the output into evidence:
- `grep -rnE "dependency-verification|verify-metadata" build.gradle app/build.gradle k9mail-vendored/build.gradle metadata/build.gradle settings.gradle gradle.properties .github/workflows/ci.yml .claude/CLAUDE.md sdlc/artifacts/engagement/code-location.md`. The only hit allowed is `<verify-metadata>true` in the XML, which is not among these paths.
- `grep -nE "kotlin-android|kotlinOptions|buildDir" build.gradle app/build.gradle k9mail-vendored/build.gradle metadata/build.gradle` → expect 0 code hits. Comment mentions must be reworded.
- `grep -nE "builtInKotlin|newDsl|proguardAndroidTxt|org.gradle.java.home|enableJetifier" gradle.properties build.gradle app/build.gradle` → expect 0.
- `git diff HEAD -- app/build.gradle | grep -E "^[-+].*(minimum|includes|excludes)"` → only the expected `buildDir` → `layout.buildDirectory` rewrites. No `minimum` or `includes` lines change.
- `grep -nE "compileSdkVersion 35|targetSdkVersion 35" app/build.gradle` and `grep -nE "nonTransitiveRClass=true|nonFinalResIds=true" gradle.properties`.

### Step 12 — Documentation (AC-6)

12.1 **`sdlc/artifacts/engagement/code-location.md` (Developer edits directly).**
- Update the build-environment rows of Tech Stack:
  - Language: Java + Kotlin; app bytecode Java 17; vendored k-9 module Java 8
  - Gradle 9.3.1; AGP 9.0.1; Kotlin 2.3.21
  - compileSdk/targetSdk 35; minSdk 21
  - Build tools 36.0.0+
- Fix the Build/Test/Run table: debug APK path `app/build/outputs/apk/debug/app-debug.apk`, and the three gates.
- Fix the stale `-Xlint:deprecation` note.
- Add a **"Build Environment"** section that states:
  - (a) Gradle, javac, kotlinc and unit tests run on **JDK 25**, declared by `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=25`).
  - (b) Precedence: daemon JVM criteria > `org.gradle.java.home` (command line > project/user `gradle.properties`) > `JAVA_HOME`. **Without** the criteria file, a user-global `org.gradle.java.home` overrides `JAVA_HOME`.
  - (c) With `org.gradle.java.installations.auto-detect=false`, a JDK 25 must be listed in `org.gradle.java.installations.paths` (M4 result).
  - (d) User-global `org.gradle.jvmargs` replaces the repository's jvmargs (SC-4).
  - (e) How to verify: `./gradlew --version`, Daemon JVM line.

  Other stale rows (otto, firebase-jobdispatcher, minSdk 14 history) are outside this story. List them as follow-up in Implementation Notes and do not edit them.

12.2 **`.claude/CLAUDE.md`, "Local Build & Device Notes" (Engine-Librarian-owned).** The Developer **must not edit it**. Delegate to the Engine Librarian (`amp:engine-librarian`) with the exact replacement for the "Build JDK" bullet, and record the Librarian's confirmation in Implementation Notes. Proposed text:
> - **Build JDK:** Gradle, javac, kotlinc and unit tests run on **JDK 25**, declared in `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=25`). That file takes precedence over both `JAVA_HOME` and any `org.gradle.java.home` (including a user-global one in `~/.gradle/gradle.properties`). Without it, a user-global `org.gradle.java.home` overrides `JAVA_HOME`. If your user-global file sets `org.gradle.java.installations.auto-detect=false`, `org.gradle.java.installations.paths` must include a JDK 25 (e.g. Android Studio's JBR 25). Check with `./gradlew --version` (the "Daemon JVM" line). Gates: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` (JaCoCo enforces per-package LINE floors). Note: a user-global `org.gradle.jvmargs` replaces the repository's jvmargs.

  The rest of the section (`MSYS_NO_PATHCONV`, test-count command, adb screenshots) stays unchanged. The `export JAVA_HOME=… jbr-17` instruction is removed, since it no longer has any effect.

12.3 Write the Implementation Notes in the story. Include:
- the version table with citations (AC-11, AC-15)
- SC-1..SC-4
- baseline vs after: test counts, class counts per package, class-root counts, LINE ratios
- the AC-16 debt list
- the AC-17 statement "no opt-outs"
- the AC-18 per-flag decisions
- the AC-7 M1–M4 results
- the AC-8 decision
- the verification-metadata diff summary

### Step 13 — Cleanup and commit hand-off

- Run `./gradlew --stop`.
- Delete `C:/Code/u060-baseline` and the scratch Gradle homes after the evidence is saved.
- Commit only when the pipeline or user authorizes it, as **one atomic commit** on `sdlc/modernization-plan`: wrapper, build files, metadata, criteria file, CI, proguard rule, code-location. The `.claude/CLAUDE.md` change lands through the Engine Librarian.

---

## Files Modified/Created

| File | Change | Owner |
|---|---|---|
| `gradle/wrapper/gradle-wrapper.properties` | 9.3.1 bin URL + `distributionSha256Sum` (regenerated) | Developer |
| `gradle/wrapper/gradle-wrapper.jar` | regenerated (sha256 `b3a875dd…`) | Developer |
| `gradlew` | regenerated (mode 100755 preserved) | Developer |
| `gradlew.bat` | **new tracked file** (was untracked/absent) | Developer |
| `gradle/gradle-daemon-jvm.properties` | **new**, `toolchainVersion=25` | Developer (generated by `updateDaemonJvm`) |
| `build.gradle` | AGP/KGP/KSP/Hilt classpath; `layout.buildDirectory`; `updateDaemonJvm` config; comments | Developer |
| `app/build.gradle` | remove `kotlin-android`; `kotlin { compilerOptions }`; proguard-optimize file; dependency bumps; JaCoCo 0.8.14; shared class roots + guard; `layout.buildDirectory`; comments | Developer |
| `app/proguard-rules.pro` | append `-dontoptimize` (behavior preservation) | Developer |
| `gradle.properties` | comments only (+ jvmargs only if Step 9 proves a flag harmful) | Developer |
| `gradle/verification-metadata.xml` | additions only, Gradle writer | Developer |
| `.github/workflows/ci.yml` | temurin 25, build-tools 36.0.0 step, comment | Developer |
| `sdlc/artifacts/engagement/code-location.md` | build-environment sections | Developer |
| `.claude/CLAUDE.md` | "Build JDK" bullet | **Engine Librarian** |
| `sdlc/artifacts/build/sprints/sprint-017/U-060-…md` | Implementation Notes | Developer |
| `k9mail-vendored/build.gradle`, `metadata/build.gradle`, `settings.gradle` | **expected no change** (only if Step 7.4 reports an error) | Developer |

No production or test source file changes are expected. B4/SC-2 fallbacks touch only `app/build.gradle`. The SC-2 escalation options would touch source, which is why they require a decision first.

---

## Contracts Verification

`integration_contracts: []`. This story changes no component boundary: it touches build tooling only, with no API, schema or inter-module interface change. The `:app` → `:k9mail-vendored` dependency (`implementation project(...)`, `api` dependencies of the vendored module) is unchanged. The Contract Gate does not apply.

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
| R1 | androidx.hilt-compiler 1.2.0 fails under KSP2 (SC-2) | Medium / High | Checkpoint B1 → Fallback B2 → stop and escalate; no silent minSdk change |
| R2 | JaCoCo Kotlin class root moves (`BUILT_IN_KOTLINC`), so coverage silently shrinks | High / High | Step 3.3 B5 discovery, shared root list, fail-fast guard, per-package `<class>` count comparison (AC-9) |
| R3 | `hiltJavaCompileDebug` or app javac gets an `[options]` warning on JDK 25 → `-Werror` failure | Medium / Medium | Checkpoint B2-javac; scoped `-Xlint:-options` fallback, recorded |
| R4 | JaCoCo 0.8.14 Kotlin filters change LINE totals and push a package below its floor (`di` at 3.1% vs 3%) | Low-Medium / High | Baseline LINE counters vs after; never lower floors; escalate |
| R5 | AGP 9 lint adds new checks, so `lint` fails in CI | Medium / Medium | Step 8.2 local CI-parity run; no silent baseline regeneration; escalate |
| R6 | R8 default changes (`strictFullModeForKeepRules`, optimize file) alter the release APK | Medium / Medium | `-dontoptimize` keeps the prior optimization posture. Project keep rules all use `{ *; }` or keepclassmembers, so strict mode does not affect them. Consumer rules may still be affected, and release is not device-tested in this story: record as residual risk |
| R7 | Verification-metadata churn hides an unexpected artifact | Medium / High (supply chain) | Pristine-restore then a single writer pass; additions-only script; manual review of added groups |
| R8 | Robolectric 4.15.1 has an undocumented JDK 25 issue | Low-Medium / High | ASM 9.8 reads major 69. If it fails, add the documented `--add-opens` flags. If still failing, escalate: 4.16+ drops SDK 21, which needs a test-config decision |
| R9 | Mockito inline self-attach is blocked in a future JDK | Low (warn only on 25) / Low | Record as debt (Mockito javaagent configuration later) |
| R10 | Daemon criteria behave unexpectedly with `auto-detect=false` | Low / Medium | M1–M4 matrix; document the limitation |
| R11 | Hilt 2.59.2 (kotlin-metadata-jvm 2.2.20) cannot read Kotlin 2.3.21 metadata | Low / High | Checkpoint B1 covers it (Hilt processing of `.kt` modules in `di/`). If it fails, stop: Hilt 2.60 needs minSdk 23 (SC-2 class decision) |
| R12 | `dependency.useConstraints=false` (AGP 9 default) changes resolved test-classpath versions | Low / Low | Test pass plus executed-count comparison; optional `:app:dependencies` diff if tests regress |
| R13 | Windows MAX_PATH on deep intermediates in scratch copies | Low / Low | Baseline at `C:/Code/u060-baseline` (short path) |
| R14 | CI cannot be exercised before push | Certain / Medium | YAML review plus local CI-command parity; first PR run is the final check (recorded) |
| R15 | `gradlew.bat` becomes a newly tracked file | Certain / Low | Intentional (AC-14); called out in the commit message |

## Quality Attribute Coverage (ISO/IEC 25010)

- **Functional Suitability:** app behavior is unchanged: same compile/target SDK, minSdk and bytecode 17. AC-5 checks launch on API 36. The test suite (697 `@Test`) must pass with an executed count ≥ baseline.
- **Performance Efficiency:** build-time only. KSP 2.3.6 includes a Windows KSP2 recompilation fix. Hilt 2.59.2 fixes slow AGP-9 incremental builds. Release R8 optimization stays disabled (`-dontoptimize`), so APK size and runtime are unchanged. No runtime performance impact is expected.
- **Compatibility:** the core of the story. The version set is chosen strictly from documented compatibility tables (Gradle, AGP, KGP) and release notes (KSP, Dagger, JaCoCo, ByteBuddy, Robolectric). Co-existence with user-global Gradle settings is handled by the daemon criteria and documented.
- **Usability (developer usability):** removes an undocumented, machine-specific prerequisite. `./gradlew --version` shows which JDK is used and why. Docs are updated for the `org.gradle.java.home` / `JAVA_HOME` / criteria precedence.
- **Reliability:** fail-fast JaCoCo class-root guard. Checkpoints with stop conditions. Atomic, uncommitted work with a defined rollback. The baseline lives in an isolated tree.
- **Security:** dependency verification stays enforced. Additions are made only by the Gradle writer and proven by script. New: wrapper `distributionSha256Sum` and the wrapper-jar hash checked against the official value. No toolchain auto-download, so no foojay and no new network trust. Hilt 2.59.2 and Mockito 5.18.0 are supply-chain additions recorded in the metadata.
- **Maintainability:** removes deprecated `kotlin-android`/`kotlinOptions` and `buildDir`, de-duplicates the JaCoCo class-root lists, and refreshes stale version comments. Remaining deprecations are listed as debt (AC-16). Known future blockers are recorded: Hilt 2.60 and androidx.hilt 1.3 need minSdk 23; Robolectric 4.16 drops SDK 21.
- **Portability:** the build now runs on JDK 25 locally and in CI (temurin 25 on ubuntu-22.04). Still within Gradle 9's JVM 17–27 range. The criteria file is OS-neutral (no platform URLs).

## Test Strategy

- **Regression:** all existing unit tests on JDK 25 through `:app:testDebugUnitTest`. No test source changes; `git diff app/src/test` must be empty. Executed totals (JUnit XML via scratch init script) are compared with the JDK 17 baseline.
- **Coverage-gate integrity:** `jacocoTestCoverageVerification`, plus the per-package class-count and LINE-counter comparison against the baseline, plus the class-root guard.
- **Build integrity:** `assembleDebug`, `assembleRelease`, `lint`, `--warning-mode all`, and `javap` major versions (61/61/52).
- **Supply chain:** an enforced-verification build after the single writer pass; additions-only script; wrapper hashes.
- **Environment:** the M1–M4 Gradle-user-home matrix (AC-7, AC-18).
- **Runtime smoke:** install and launch on the API 36 emulator `Pixel_9_Pro` (AC-5).
- **CI:** YAML review plus the local CI command; the first PR run confirms.

No new automated tests are added. The story is a toolchain change, and its correctness criteria are the existing gates plus the equivalence evidence above.

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-060 story | sdlc/artifacts/build/sprints/sprint-017/U-060-jdk-25-build-toolchain-upgrade.md | ACs AC-1..19, IC-1..3 |
| BUG-017 | sdlc/artifacts/stories/BUG-017-build-fails-on-jdk-25.md | Source bug, advisory approach |
| REQ-MODERNIZATION-003 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-003-establish-ci-coverage-gate.md | Approved constraint: CI command, minify, JaCoCo gate, verification metadata |
| Root build | build.gradle | Plugin pins, `clean`/`buildDir`, Test config |
| App build | app/build.gradle | DSL, deps, JaCoCo paths/rules, `-Werror` |
| Vendored build | k9mail-vendored/build.gradle | Java 8 target, DSL |
| Metadata build | metadata/build.gradle | `task lint` (Groovy 4) |
| Settings | settings.gradle | Module list; no toolchain resolver |
| Properties | gradle.properties | jvmargs, R flags, comments |
| Wrapper | gradle/wrapper/gradle-wrapper.properties | 8.14.1, no checksum |
| Verification | gradle/verification-metadata.xml | Header/config; 546 components; junit-bom entries |
| ProGuard | app/proguard-rules.pro | Keep-rule shape vs AGP 9 R8 defaults |
| Robolectric config | app/src/test/resources/robolectric.properties | sdk=29 pin |
| Tests (grep) | app/src/test/** | Configured SDKs {21, 23, 29}; 697 `@Test`; 15 `.kt` tests |
| Manifest | app/src/main/AndroidManifest.xml | Launcher `.activity.MainActivity` |
| CI | .github/workflows/ci.yml | JDK 17, commands |
| Project rules | .claude/CLAUDE.md | Build/device notes; ownership |
| Engagement doc | sdlc/artifacts/engagement/code-location.md | Stale build notes (AC-6) |
| User-global (read-only) | ~/.gradle/gradle.properties | java.home, auto-detect, jvmargs (SC-4) |
| Templates | plugin cache templates/build-artifacts/{build-plan,gate-report-template}.md | Frontmatter/sections |
| External | docs.gradle.org compatibility (9.1.0–9.4.1, current), upgrading_major_version_9, gradle_daemon, build_environment (9.3.1); AGP 9.0.0 release notes; migrate-to-built-in-kotlin; kotlinlang gradle-configure-project and whatsnew23; KSP/Dagger/Robolectric GitHub releases; JaCoCo changes; ByteBuddy release notes; ASM versions; androidx.hilt releases; Maven/Google metadata, POMs, AARs; runner-images Ubuntu 22.04; gradle-api/gradle 9.0.1 jars (javap) | Version and compatibility evidence |

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
