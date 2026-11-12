---
artifact_type: implementation-log
story_id: "U-060"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-11-12"
files_changed: 10
files_created: 2
tests_added: 0
tests_passing: 697
---

# Implementation Log: U-060 (running record; appended after every step)

Resume protocol: find the last "STATE:" line at the bottom.

## Step 0 - baseline capture
- 0.1 HEAD at (re)start: 77de23da (only sdlc/** differs from b09bda5d: `git diff --stat b09bda5d HEAD -- . ':!sdlc'` empty, verified earlier at cbe34e03; re-verify below). Build-input status clean.
- 0.2 @Test count at HEAD: 697.
- 0.3 baseline tree C:/Code/u060-baseline created via git archive (session 1). Session 1 was killed during 0.4 writer pass; no app/build exists and its verification-metadata.xml is identical to repo's (diff -q empty) -> restarting 0.4 from clean.
- Env note: JAVA_TOOL_OPTIONS in this shell is set to the SelectorProvider/unixdomain flags, which makes Gradle fork a single-use daemon (cosmetic).
STATE: Step 0.4 restarting (repo tree untouched).
- 0.4 DONE (session 2): baseline writer pass on JDK 17 (Gradle 8.14.1, `-Dorg.gradle.java.home=<jbr17>`), `--continue`. assembleDebug, testDebugUnitTest, assembleRelease succeeded; `:app:lintDebug` FAILED ("Lint found 48 errors, 25 filtered by baseline", first: ContactAccessorTest CheckResult) -> PRE-EXISTING lint failure at HEAD on JDK 17/AGP 8.7.3 (recorded; not a regression). Evidence: evidence/baseline-writer-pass.log. SC-3 gap closed in baseline copy only: evidence/baseline-preexisting-verification-gap.diff (91 added lines, no removals).
STATE: Step 0.5 (measured baseline run) next.
- 0.5/0.6 DONE: measured baseline on JDK 17 (clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification) exit 0 (evidence/baseline-gates.log). `lint assembleRelease --continue`: lintDebug FAILS (pre-existing), assembleRelease succeeds (evidence/baseline-lint-release.log).
  - JUnit totals (baseline): tests=697 failures=0 errors=0 skipped=2 (evidence/baseline-junit-totals.txt)
  - javap App.class: major 61 (evidence/baseline-javap.txt)
  - Class roots (.class counts): asm 182, javac 296, kotlin(tmp/kotlin-classes/debug) 116 (evidence/baseline-class-roots.txt)
  - JaCoCo inventory: 24 packages, 399 classes total, per-package JSON evidence/baseline-jacoco-inventory.json
  - KSP generated file list: 49 files (evidence/baseline-ksp-files.txt)
  - C-1 baseline manifests (aapt2 36.0.0): evidence/baseline-manifest-{debug,release}.txt; C-2 baseline R8: evidence/baseline-mapping.txt, baseline-configuration.txt
  - Baseline debug APK copy: C:/Code/u060-baseline-app-debug.apk
- CHECKPOINT 0: MET (baseline gates pass on JDK 17; only deviation is the pre-existing lintDebug failure and SC-3 gap).
STATE: Step 1 (wrapper bootstrap) next - tree becomes intermediate; rollback = plan "Rollback procedure".
STATE: Step 1 applied (wrapper properties -> 9.5.1+sha). Tree INTERMEDIATE; step 2 edits follow.
- Step 1/2 applied: wrapper props (9.5.1 + sha), build.gradle, app/build.gradle, proguard -dontoptimize, gradle.properties comments. STATE: Step 3 (3.1 version, 3.2 Checkpoint A) next.
- CHECKPOINT A: PASS (Gradle 9.5.1, Launcher JVM 25.0.5, Daemon JBR 25; help --warning-mode all exit 0; evidence/checkpointA.log, after-gradle-version-step3.1.txt). STATE: Checkpoint B (3.3) running; verification-metadata.xml in writer-mode exploratory state (restored in Step 6).
- CHECKPOINT B (3.3): app:assembleDebug + compileDebugUnitTestKotlin on Gradle 9.5.1/JDK 25 SUCCEEDED (evidence/checkpointB.log, 4m25s, exit 0).
  - B1 PASS (androidx.hilt 1.2.0 kept, minSdk stays 21): ksp/hiltJavaCompile/javac all succeed; BackupWorker/RestoreWorker _AssistedFactory,_Factory,_HiltModule generated. KSP file set vs baseline (evidence/after-ksp-files.txt): nothing missing; 4 ADDED files, all Hilt 2.59-generated for MainViewModel (`MainViewModel_HiltModules_{BindsModule_Binds,KeyModule_Provide}_LazyMapKey.java` and two `META-INF/proguard/*_LazyClassKeys.pro`) - a new Hilt codegen feature, not a build-tool-internal path difference. Step 7 unit tests are criterion (d).
  - B-javac: no `[options]` warning for app javac / hiltJavaCompileDebug (only the vendored Java 8 module prints "source value 8 is obsolete", no -Werror there). No -Xlint:-options needed.
  - B3 (jlink): not hit. B4: Kotlin in src/*/java compiled (BackupWorker.class present in Kotlin output; no sourceSet fallback needed).
  - B5: **Kotlin output MOVED** to app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes (116 classes = baseline 116). app/build/tmp/kotlin-classes/debug still exists but is STALE (mtime 2026-06-24, from the pre-upgrade build) - it would have made the JaCoCo guard pass on stale classes. jacocoClassRootPaths.kotlin updated to the new path. ASM root intermediates/classes/debugUnitTest/transformDebugUnitTestClassesWithAsm/dirs (182) and javac root (297 vs 296 baseline: +2 Hilt LazyMapKey classes, -1 `CallLogTypes$1` switch-map synthetic) exist. Evidence: evidence/after-b5-class-dirs.txt. Step 7 will run `clean` first so no stale dirs remain.
STATE: Step 3.4 not triggered. Step 4 next (updateDaemonJvm).
- Step 4 DONE: `./gradlew updateDaemonJvm --jvm-version=25` -> gradle/gradle-daemon-jvm.properties = header + `toolchainVersion=25` (no URLs). `./gradlew --version`: "Daemon JVM: Compatible with Java 25, any vendor, nativeImageCapable=false (from gradle/gradle-daemon-jvm.properties)" (evidence/after-gradle-version-step4.txt). (Gradle did not label it incubating in output.)
- Step 5 DONE: `./gradlew wrapper --gradle-version 9.5.1 --distribution-type bin --gradle-distribution-sha256-sum bafc141b...b031f` exit 0; wrapper jar sha256 497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7 == official; distributionSha256Sum retained; gradlew mode 100755 kept; gradlew.bat newly created (untracked -> new tracked file, R15). Evidence: evidence/step5-*.
STATE: Step 6 (clean metadata regeneration) next.
- ENV NOTE (session 2): every Bash call must export JAVA_TOOL_OPTIONS=' -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider -Djdk.net.unixdomain.tmpdir=/nonexistent' before ./gradlew; without it any JDK 25 JVM here fails Selector.open() with 'Unable to establish loopback connection' (default unix-domain tmpdir unusable; verified with a standalone Sel.java). This is a machine/shell property (it is the same workaround as the repo's org.gradle.jvmargs, AC-18).
- Step 6 DONE: pristine single-path restore of verification-metadata.xml (HEAD copy kept in scratch), one writer pass (`--write-verification-metadata sha256 --continue clean help test lint jacocoTestReport assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`, evidence/step6-writer-pass.log). Only failing task: `:app:lintDebug` (same pre-existing first failure as baseline: ContactAccessorTest CheckResult; AGP 9 lint counts 39 errors/22 filtered vs baseline 48/25).
  - Additions-only proof (evidence/step6-vm-diff-summary.txt): configuration block unchanged: True; removed-or-altered: 0; added entries: 479 (257 distinct group:name:version, evidence/step6-vm-added-components.txt, full list step6-vm-added-full.txt). Groups: kotlin (43), AGP utp/build/lint tooling, grpc/netty/protobuf (AGP 9 + UTP), dagger (10), robolectric (13), asm (9), ksp (7), jacoco (5), bytebuddy (3), mockito (1)... C-5: NO com.google.crypto.tink and NO androidx.security entries added.
- Step 6.5 enforced (no writer) :app:assembleDebug: exit 0 (evidence/step6-enforced-assembleDebug.log). STATE: Step 7 gates running.
- Step 7 (Gradle 9.5.1, JDK 25.0.5 launcher / criteria daemon): `clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` (+init script for JUnit XML) -> exit 0, BUILD SUCCESSFUL 1m45s (evidence/after-gates.log); `grep -c "major version 69"` = 0; no Unsupported/ByteBuddy/Jacoco/InaccessibleObject hits; no --add-opens needed (T3 not triggered; Robolectric 4.15.1 PASS on JDK 25; minSdk stays 21). `:app:jacocoTestReport` exit 0.
  - 7.5 tests: JUnit XML after: tests=697 failures=0 errors=0 skipped=2 (baseline: 697/0/0/2). `@Test` count at HEAD = 697, worktree = 697. `git diff --stat HEAD -- app/src/test` empty (no test-source change).
  - 7.2 AC-9: classes per package after >= baseline for all 24 packages, no missing/extra package; total 399 -> 401 (activity 72->74 = the two Hilt LazyMapKey classes... see below) (evidence/after-jacoco-inventory.json vs baseline-jacoco-inventory.json). Class roots after (fresh after `clean`): asm 182 (=182), javac 297 (296), kotlin(built_in_kotlinc) 116 (=116); tmp/kotlin-classes no longer exists. Line totals shift slightly (smssync 20/34->18/30, receiver 31/58->29/58, preferences 310/413->315/413, ...) - JaCoCo 0.8.14 counting; every package still above its floor (the gate passed): e.g. di 2/29=6.9% (floor 3%), smssync 60% (floor 50%), receiver 50.0%... (floor 50% - see note) .
  - 7.3 javap majors: App.class 61, BackupWorker.class (Kotlin) 61, vendored Address.class 52 (evidence/after-javap.txt).
  - 7.4 `--warning-mode all` (evidence/warning-mode-all.log): exit 0. Only configuration-time Groovy "space assignment" deprecations (abortOnError, baseline, buildConfig, namespace x2, signingConfig, url, warningsAsErrors) = debt; the run was largely UP-TO-DATE so task-level deprecations (packaging.exclude, minifyEnabled etc.) were not re-emitted. Vendored "source/target value 8 is obsolete" warning (no -Werror) = debt. No error-class items.
STATE: Step 8 (ci.yml) next.
- Step 8.1 DONE: .github/workflows/ci.yml -> Set up JDK 25 (temurin), corrected comment, sdkmanager build-tools;36.0.0 step (YAML parse-checked). R14 containment NOT applied (needs first PR run). STATE: 8.2 local CI parity running.
- Step 8.2 DONE: `./gradlew test lint jacocoTestReport assembleRelease --continue` -> only `:app:lintDebug` FAILED (same task and same first finding as the JDK 17 baseline; pre-existing); assembleRelease succeeded (minifyEnabled true); `:app:jacocoTestCoverageVerification` exit 0 (evidence/step8-ci-command.log, step8-coverage-gate.log). `:metadata:lint` ran OK on Groovy 4 (8.3). Lint finding classes baseline vs AGP 9 (evidence/lint-ids-*.txt): AGP 9 adds UseRequiresApi(4), UseKtx(2), ObsoleteSdkInt(+1), AndroidGradlePluginVersion(1); drops GradleDependency(17), KtxExtensionAvailable(1). lint-baseline.xml NOT touched / not regenerated. SCOPE CONCERN for orchestrator: lintDebug (warningsAsErrors) is red both before and after, so CI `lint` step is already failing at HEAD; U-060 does not fix it. AGP 9 also means CI `test` runs only debug unit tests (onlyEnableUnitTestForTheTestedBuildType).
- C-1 PASS: aapt2 xmltree dumps (debug and release) baseline vs after: diff EMPTY (evidence/{baseline,after}-manifest-{debug,release}.txt). After-dumps assert: package com.zegoggles.smssync, versionCode 1602, minSdk 21/target 35, BackupBroadcastReceiver exported=true, no permission attr, action com.zegoggles.smssync.BACKUP, SystemForegroundService foregroundServiceType=0x1, no InitializationProvider element, networkSecurityConfig + fullBackupContent present.
- C-2 PASS: R8 (release): the 9 reflection-sensitive classes (App, MainActivity, 4 receivers, BackupWorker, RestoreWorker, BackupTriggerWorker) map to themselves in both builds; kept-name set (smssync/k9/tink/androidx.work) baseline 49 = after 49, `comm -23` empty. configuration.txt diff (evidence/c2-configuration-diff.txt): (1) `-allowaccessmodification` added = the default-file swap to proguard-android-optimize.txt (with project -dontoptimize); (2) Hilt EarlyEntryPoint rule differs only by a trailing comment text; (3) new `-keep,allowobfuscation,allowshrinking class ...activity.MainViewModel` from Hilt 2.59.2 LazyClassKeys consumer rule. R8 version changed (AGP 9); keep behaviour verified equivalent - NOT claiming runtime unchanged beyond this evidence; release APK not exercised on device.
- C-5 PASS: no tink/security-crypto in the added verification components; releaseRuntimeClasspath resolves androidx.security:security-crypto:1.1.0 and tink-android:1.8.0 in both baseline and after (evidence/{baseline,after}-deps-releaseRuntime.txt).
- IC-1 evidence (evidence/after-deps-debugRuntime.txt): hilt-android 2.49->2.59.2, hilt-core->2.59.2, androidx.hilt hilt-work/hilt-common 1.2.0, work-runtime 2.3.4->2.9.1, kotlin-stdlib -> 2.3.21 everywhere; KSP processor configs: evidence/after-ksp-configs.txt.
STATE: Step 9 (scratch Gradle user home matrix) next. Every gradle call needs the JAVA_TOOL_OPTIONS export (see ENV NOTE).
- !! CAPABILITY REGRESSION FOUND AND FIXED (gradlew): HEAD's gradlew carried a local Windows workaround from U-002 (commit 4c908f69): `export JAVA_TOOL_OPTIONS=... SelectorProvider=sun.nio.ch.WindowsSelectorProvider -Djdk.net.unixdomain.tmpdir=/nonexistent` (plus DEFAULT_JVM_OPTS). `./gradlew wrapper` (Step 5) regenerated the script without it; every Gradle call then failed with "java.io.IOException: Unable to establish loopback connection" until JAVA_TOOL_OPTIONS was exported manually (a standalone Sel.java proves any JDK 25 JVM on this machine needs the flag). Fix: re-added the export block to the regenerated gradlew (documented in-file, mode 100755 kept). Wrapper jar/properties remain exactly as generated (jar sha256 official). Future `./gradlew wrapper` runs must re-apply it. Verified: a bare shell (no JAVA_TOOL_OPTIONS) `./gradlew help` -> BUILD SUCCESSFUL.
- Step 9 DONE (scratch Gradle user homes C:/Code/u060-gh-M1..M4, GRADLE_RO_DEP_CACHE=~/.gradle/caches to avoid re-download; evidence/step9-M*-version.txt, step9-M*-help.log). In every case the daemon actually started is C:\Program Files\Java\jdk-25.0.5 (Oracle), `--version` says "Daemon JVM: Compatible with Java 25, any vendor (from gradle/gradle-daemon-jvm.properties)", `help` BUILD SUCCESSFUL, with verification enforced:
  - M1 (no user-global file): works; repo jvmargs apply (-Xmx2048m...).
  - M2 (org.gradle.java.home=jbr-17): daemon still JDK 25 -> the criteria file takes precedence over org.gradle.java.home (proves AC-6 precedence).
  - M3 (auto-detect=false + paths=jdk-25.0.5): daemon = 25.0.5.
  - M4 (auto-detect=false, NO paths): ALSO works here - Gradle falls back to the launcher JVM (JDK 25.0.5 from PATH) because it satisfies the criteria. Observed limitation: with auto-detect off and a launcher JVM that is not 25, a JDK 25 must be listed in org.gradle.java.installations.paths (not exercisable here since PATH java is 25; stated as expectation, unverified).
  - Gradle applies `-D` org.gradle.jvmargs entries to the daemon after start (they are not in the daemon command line); `-Xmx` is. 
  - AC-18: both flags retained (M1 starts/builds with them; also builds with them overridden away because gradlew now exports JAVA_TOOL_OPTIONS - redundant but harmless/defence for IDE starts). Reason lookup: `git log -S WindowsSelectorProvider -- gradle.properties` = 4c908f69 U-002 (Windows + JDK17 + Gradle 8 daemon fix). WindowsSelectorProvider class works on JDK 25.0.5 (Sel.java OK). gradle.properties comment updated.
STATE: Step 10 (device) next.
- Step 10: starting emulator Pixel_9_Pro (log in scratchpad/emulator.log).
- Step 10 DONE (AC-5, evidence/step10-device.txt, ac5-launch.png): Pixel_9_Pro (API 36, serial resolved by `emu avd name`; Pixel_10_Pro untouched) booted; `adb install -r app/build/outputs/apk/debug/app-debug.apk` (built by this story's toolchain) -> Success; `am start -W MainActivity` Status ok; pidof -> 4933; `logcat | grep "FATAL EXCEPTION|AndroidRuntime: Process: com.zegoggles.smssync"` -> no match (exit 1). Screenshot shows main screen "Idle". (Screenshot shows the PO's configured account name - local evidence only, do not publish.)
- C-3 PARTIAL: `am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync` -> "Broadcast completed: result=0", no FATAL. BUT the installed app has third-party integration ENABLED (PO's configuration), so the log shows the enabled branch ("backup requested via broadcast intent", then BackupWorker.doWork type=BROADCAST_INTENT ... "nothing to backup"), NOT the "...but ignored" branch the check specifies. I did not change the PO's preference/data. Net evidence: packaged receiver reachable by an unprivileged sender with the frozen action; opt-in gate evaluated; HiltWorker BackupWorker instantiated and ran at runtime under the new toolchain. The integration-OFF "ignored" line is NOT VERIFIED on device.
STATE: Step 11 sweep next.
- Step 11 DONE (evidence/step11-sweep.txt): no dependency-verification/verify-metadata hit in build files/properties/CI/docs; no kotlin-android/kotlinOptions/buildDir left (comments reworded; AC-17 grep for builtInKotlin|newDsl|proguardAndroidTxt|org.gradle.java.home|enableJetifier: only a pre-existing U-059 comment "enableJetifier removed" in gradle.properties); `git diff HEAD -- app/build.gradle | grep minimum|includes|excludes` EMPTY (no floor/include/exclude changed; AC-4); compileSdk 35 / targetSdk 35 / minSdk 21; nonTransitiveRClass=true, nonFinalResIds=true; no org.gradle.java.home added to repo. No AGP opt-outs introduced (AC-17). k9mail-vendored/build.gradle, metadata/build.gradle, settings.gradle: unchanged (IC-2/IC-3: vendored VERSION_1_8 kept).
STATE: Step 12 (docs) next.
- Step 12.1 DONE: code-location.md updated (tech stack rows, build/test table, new 'Build Environment' section from observed M2/M3/M4). STATE: final re-run of the 3 gates from a bare shell (no JAVA_TOOL_OPTIONS, no MSYS_NO_PATHCONV) after last comment-only edits.

## Summary
Path run: MAIN path. No Branch M, no T3 path, no Step 3.4 (Gradle 9.3.1) fallback, no R14 CI containment. minSdk stays 21; androidx.hilt stays 1.2.0 (B1 PASS); Robolectric 4.15.1 passes on JDK 25 with no `--add-opens`; no test source changed.
Checkpoints: 0 MET (JDK 17 baseline; only pre-existing lintDebug failure + SC-3 gap) | A PASS | B PASS (B1 PASS, B-javac clean, B3/B4 not hit, B5 Kotlin class root MOVED and fixed) | C PASS (7.1-7.5) | Step 8.2 CI parity: only the pre-existing lintDebug failure.
Version set: Gradle 9.5.1 (sha bafc141b...b031f), AGP 9.0.1, KGP/kotlin-stdlib 2.3.21, KSP 2.3.6, Hilt 2.59.2 (plugin/android/compiler x2/testing), androidx.hilt 1.2.0, JaCoCo 0.8.14, Robolectric 4.15.1, Mockito 5.18.0, Build Tools 36.0.0, CI temurin 25. Rationale and verbatim vendor citations: plan.md "Version Decisions" and SC-1..SC-5 (SC-5 primary reading applies: Gradle 9.5.1 + AGP 9.0.1 + KGP 2.3.21 satisfies both AC-11 sources literally; plan.md SC-5 is the AC-11/AC-15 evidence).

Before/after (gate 1 = assembleDebug, 2 = testDebugUnitTest, 3 = jacocoTestCoverageVerification; all PASS on JDK 25; final re-run from a bare shell: evidence/final-gates.log, exit 0):

| Metric | Baseline (JDK 17, Gradle 8.14.1) | After (JDK 25, Gradle 9.5.1) |
|---|---|---|
| `@Test` count (authoritative cmd) | 697 | 697 |
| JUnit XML | tests 697, fail 0, err 0, skipped 2 | tests 697, fail 0, err 0, skipped 2 |
| JaCoCo classes (24 pkgs) | 399 | 401 (every package >= baseline; activity 72->74, others equal) |
| Class roots (.class) asm / javac / kotlin | 182 / 296 / 116 | 182 / 297 / 116 (kotlin now `intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes`) |
| javap major App / Kotlin / vendored | 61 / - / - | 61 / 61 / 52 |

Line-total shifts are JaCoCo 0.8.14 counting (e.g. receiver 31/58 -> 29/58 = exactly 50.0% vs floor 0.50: passes with zero headroom, RISK noted; di 2/29 = 6.9% vs 0.03).

## Files Modified
- gradle/wrapper/gradle-wrapper.properties, gradle/wrapper/gradle-wrapper.jar, gradlew: Gradle 9.5.1 via `./gradlew wrapper` with checksum (AC-14); gradlew additionally carries the re-applied Windows JAVA_TOOL_OPTIONS block (see capability inventory).
- build.gradle: AGP 9.0.1 / KGP 2.3.21 / KSP 2.3.6 / Hilt 2.59.2; `layout.buildDirectory`; `updateDaemonJvm { toolchainPlatforms = [] }`; comments.
- app/build.gradle: removed kotlin-android; `kotlin { compilerOptions { jvmTarget = JVM_17 } }`; proguard-android-optimize.txt; dependency bumps; JaCoCo 0.8.14; shared class-root list + fail-fast guard (`doFirst` in both JaCoCo tasks); `layout.buildDirectory`; comments. No `minimum`/`includes`/`excludes` change.
- app/proguard-rules.pro: appended `-dontoptimize`.
- gradle.properties: comments only (AC-18 flags retained, notes).
- gradle/verification-metadata.xml: +479 entries, additions only, via writer mode.
- .github/workflows/ci.yml: JDK 25, build-tools 36.0.0 step, corrected comment (AC-8).
- sdlc/artifacts/engagement/code-location.md: tech stack + Build Environment section (AC-6, developer-owned part).

## Files Created
- gradle/gradle-daemon-jvm.properties (`toolchainVersion=25`), gradlew.bat (new tracked file; R15).
- sdlc/artifacts/build/sprints/sprint-017/U-060/implementation-log.md and evidence/.

Not changed: k9mail-vendored/build.gradle, metadata/build.gradle, settings.gradle, app/src/**, CHANGES, lint-baseline.xml, .claude/CLAUDE.md.

## Contract Adherence
`integration_contracts: []`. Contract-verification checks: C-1 (manifests identical), C-2 (R8 equivalence), C-3 (partial, see below), C-5 (pins unchanged) recorded above; C-4 not applicable (no Branch M).

## Capabilities Inventory (regenerated / rewritten files)
- gradlew (regenerated): Windows NIO selector JAVA_TOOL_OPTIONS export (U-002) was dropped by the regeneration (REGRESSION found) -> RETAINED after fix: export block in gradlew before `MAX_FD=maximum`; reaches launcher and daemon via the env var. Old DEFAULT_JVM_OPTS override not needed. Mode 100755 retained.
- gradle-wrapper.properties: URL/zip store keys RETAINED; distributionSha256Sum ADDED; generated extras networkTimeout/retries/validateDistributionUrl.
- build.gradle: `clean` task RETAINED (layout.buildDirectory); allprojects Test config and repositories RETAINED unchanged.
- app/build.gradle: signingConfigs, lint, packaging, testOptions, `-Werror -Xlint:unchecked`, JaCoCo filters, 15 rules, includeNoLocationClasses, `excludes jdk.internal.*`, finalizedBy RETAINED unchanged; Kotlin jvmTarget 17 RETAINED as kotlin.compilerOptions (AC-12); Kotlin class root moved.
- gradle.properties: jvmargs flags, daemon=false, AndroidX, R flags RETAINED.
- ci.yml: commands and caches RETAINED.
- Release R8 posture (non-optimizing) RETAINED via -dontoptimize (C-2 evidence).

## Proposed .claude/CLAUDE.md change (for Engine Librarian)
Replace the "Build JDK" bullet of "Local Build & Device Notes (Windows / Git Bash)" with the text below (and drop the `export JAVA_HOME=... jbr-17` instruction, which no longer has any effect):
> - **Build JDK:** Gradle, javac, kotlinc and unit tests run on **JDK 25**, declared in `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=25`). That file takes precedence over both `JAVA_HOME` and any `org.gradle.java.home` (including a user-global one in `~/.gradle/gradle.properties`) - verified: a user-global `org.gradle.java.home` pointing at JDK 17 still yields a JDK 25 daemon. Without the file, a user-global `org.gradle.java.home` overrides `JAVA_HOME`. If your user-global file sets `org.gradle.java.installations.auto-detect=false`, list a JDK 25 in `org.gradle.java.installations.paths` (e.g. Android Studio's JBR 25) unless `java` on PATH is already JDK 25. Check with `./gradlew --version` (the "Daemon JVM" line cites the criteria file). Gates: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` (JaCoCo enforces per-package LINE floors). Notes: a user-global `org.gradle.jvmargs` replaces the repository's jvmargs; `gradlew` exports the Windows NIO `JAVA_TOOL_OPTIONS` workaround itself - re-apply it after any `./gradlew wrapper` regeneration.

## AC evidence table
| AC | Result | Evidence |
|---|---|---|
| AC-1 | MET | final-gradle-version.txt (Gradle 9.5.1, Launcher 25.0.5, Daemon from criteria file); final-gates.log exit 0, 0x "major version 69"; bare shell, no MSYS_NO_PATHCONV |
| AC-2 | MET | three gates exit 0 (after-gates.log, final-gates.log); @Test 697 = baseline; JUnit 697/0/0/2; no test diff |
| AC-3 | MET | step11-sweep.txt; additions-only proof (step6-vm-diff-summary.txt: config unchanged, 0 removed/altered, 479 added); enforced run green |
| AC-4 | MET | no minimum/includes/excludes diff; gate passes |
| AC-5 | MET | step10-device.txt: install -r, launch ok, pid, no FATAL EXCEPTION (API 36 AVD) |
| AC-6 | MET for developer part; PENDING Engine Librarian for `.claude/CLAUDE.md` | code-location.md Build Environment; proposed text above |
| AC-7 | MET | criteria file; M1-M4 matrix (Step 9 entry); limitation documented |
| AC-8 | MET (CI unexercised; first PR run = R14) | ci.yml diff |
| IC-1 | MET | after-deps-debugRuntime.txt single versions; Hilt 2.59.2 at all 5 sites |
| IC-2 | MET | app VERSION_17; vendored VERSION_1_8; javap 61/61/52; -Werror scoped to :app |
| IC-3 | MET | no stale constructs in the 4 build files; the two others needed no change |
| AC-9 | MET | after-jacoco-inventory.json vs baseline: all 24 packages >=; class roots non-empty; guard in both tasks (note: the stale tmp/kotlin-classes dir was caught by B5 evidence, a non-empty guard alone would not catch staleness) |
| AC-10 | MET | Robolectric 4.15.1 / Mockito 5.18.0 / JaCoCo 0.8.14; 0 failures/errors; no class-version/ASM/ByteBuddy/JaCoCo hits; SDK set {21,23,29} unchanged |
| AC-11 | MET | plan SC-5 primary reading |
| AC-12 | MET | javap 61 (Java + Kotlin), 52 vendored |
| AC-13 | MET | writer-mode additions only; enforced `assembleDebug` after |
| AC-14 | MET with caveat | wrapper regenerated with checksum; jar sha256 official; gradlew.bat created; gradlew has the documented Windows block re-added after regeneration |
| AC-15 | MET | lowest pair per SC-5 reading |
| AC-16 | MET | warning-mode-all.log: only config-time Groovy space-assignment deprecations + vendored obsolete-source warning; no error-class item |
| AC-17 | MET | no opt-outs/kapt/toolchain block |
| AC-18 | MET | both flags retained with reason; comments updated |
| AC-19 | MET | step11-sweep.txt |

AC-16 debt: Groovy space-assignment (abortOnError, baseline, buildConfig, namespace, signingConfig, url, warningsAsErrors observed; task-level ones such as `packaging.exclude`/`minifyEnabled` expected but not re-emitted because tasks were up-to-date); vendored "source/target value 8 is obsolete"; `task` syntax; R8 optimisation disabled via -dontoptimize (separate story). Opt-outs introduced: none.

## Notes / Scope concerns / follow-ups
- SCOPE CONCERN (pre-existing): `:app:lintDebug` is red at HEAD on JDK 17 and after (first finding ContactAccessorTest CheckResult). AGP 9 lint adds UseRequiresApi(4)/UseKtx(2)/ObsoleteSdkInt(+1)/AndroidGradlePluginVersion(1) and drops GradleDependency/KtxExtensionAvailable. CI `lint` step cannot be green; not fixed here; lint-baseline.xml not regenerated.
- SC-3: junit-bom `.module` verification gap at HEAD; closed by the Step 6 regeneration.
- Robolectric android-all jars are downloaded outside Gradle verification (unchanged).
- AGP 9: `./gradlew test` runs only debug unit tests.
- Follow-ups not done: README line 38 stale; stale rows in code-location.md (otto, firebase-jobdispatcher); stale Otto/JobDispatcher proguard rules; CI has no wrapper-validation action; `.claude/CLAUDE.md` via Engine Librarian.

## Runtime Claims
No runtime-provable claim is made by the toolchain change itself. Release R8 equivalence is evidenced by build-output diffs (C-2); "release runtime unchanged" is NOT claimed. On-device checks AC-5 and C-3 are recorded above (C-3 only partial).

## Not verified
- C-3 integration-OFF branch ("...but ignored"): the AVD has third-party integration enabled and I did not alter product-owner data.
- Release (R8-minified) APK never installed/run on a device.
- GitHub CI (JDK 25 daemon criteria on the runner = R14; sdkmanager step): first PR run.
- A case where the launcher JVM is not JDK 25 with auto-detect off (not constructible here).
- Task-level deprecations in AC-16.

## Signals
No `STORY AMENDMENT REQUIRED` (Branch M did not run), no `STORY NOTE REQUIRED` (T3 not triggered), no `PO ACKNOWLEDGEMENT ADVISED` for a Gradle fallback (9.3.1 not taken). SC-5 primary reading still merits product-owner awareness.
Cleanup: baseline copy and scratch Gradle homes deleted; emulator Pixel_9_Pro shut down; daemons stopped; nothing committed.
