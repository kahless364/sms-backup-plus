---
artifact_type: qa-results
story_id: "U-060"
gate_id: "validation"
checkpoint: "quality-gate"
verdict: "PASS"                    # PASS | FAIL
agent: "QA Analyst"
timestamp: "2026-11-12"
iteration: 2
ac_total: 22
ac_passed: 22
ac_failed: 0
tests_run: 697
tests_passed: 695
---

# QA Validation: U-060 (quality-gate iteration 2)

## Verdict: PASS

## Summary

Iteration 1 failed on one criterion, AC-6: `.claude/CLAUDE.md` still told developers to set `JAVA_HOME` to JBR 17. That is now fixed. The new "Build JDK" bullet at `.claude/CLAUDE.md:31` matches the repository and my own iteration-1 probes.

The rework is the uncommitted working tree against `8dc3e417`. It changes `gradlew`, `.claude/CLAUDE.md` and `code-location.md`, adds comments only to `app/build.gradle` and `gradle.properties`, and adds sections to `implementation-log.md`.

**Windows gate re-run.** From a bare Git Bash shell, the three gates plus `jacocoTestReport` were re-run with `--rerun-tasks`. The result was BUILD SUCCESSFUL, with 74 of 74 tasks executed on a JDK 25.0.5 daemon and test JVM. The tests were 697 run, 0 failures, 0 errors and 2 skipped. Receiver and compat coverage is line-for-line identical to iteration 1, at 29/58 and 16/73.

**Linux probe.** I ran a probe in WSL Ubuntu 24.04 with Temurin 25.0.5. On Linux the new `gradlew` does not export the Windows flag. The repository's `org.gradle.jvmargs` selector flag does not break a Linux Gradle 9.5.1 daemon or a forked JVM. The full Android build on Linux, and CI itself, remain **not verified**.

**All 22 criteria are met.** The disclosed residual items below are not required by any criterion.

## What changed since iteration 1 (verified with git)

`git diff --stat 8dc3e417` shows changes to `.claude/CLAUDE.md` (1 line), `app/build.gradle` (comments), `gradle.properties` (comments), `gradlew`, `code-location.md` (1 note) and the sdlc artifacts.

`git diff --quiet 8dc3e417 -- .github gradle/ build.gradle app/proguard-rules.pro gradlew.bat k9mail-vendored metadata settings.gradle app/src` exits 0. So these are all **unchanged since iteration 1**:
- `ci.yml`
- `verification-metadata.xml`
- the wrapper jar and properties
- `gradle-daemon-jvm.properties`
- `build.gradle` and `proguard-rules.pro`
- `gradlew.bat`
- the vendored and metadata modules, `settings.gradle`
- all app sources and tests

**Non-comment changes in `app/build.gradle` and `gradle.properties`:** `git diff 8dc3e417 -- app/build.gradle gradle.properties`, with `+`/`-` lines starting with `//` or `#` filtered out, leaves exactly two changed lines. Both are trailing comments on `minimum =` lines, and the values are unchanged:
- `app/build.gradle:488` stays `minimum = 0.20`.
- `app/build.gradle:544` stays `minimum = 0.50`.

Nothing else differs in those files. No floor, rule, include, exclude or version changed. The `org.gradle.jvmargs` value at `gradle.properties:20` is unchanged, and no test changed.

## Acceptance Criteria Results

Key to the "Basis" column:
- **RE-VERIFIED (it2)**: re-checked against the iteration-2 tree with new evidence.
- **CARRIED FORWARD**: the underlying files are unchanged since iteration 1 (git-verified above), so the iteration-1 evidence stands.

| AC | Status | Basis | Evidence (file:line / QA evidence file) |
|----|--------|-------|-----------------------------------------|
| AC-1 | PASS | RE-VERIFIED (it2) | **`--version`** (`evidence/qa/qa-it2-gradle-version.txt`): Gradle 9.5.1, `Launcher JVM: 25.0.5`, `Daemon JVM: Compatible with Java 25 ... (from gradle/gradle-daemon-jvm.properties)`.<br>**Actual daemon:** `qa-it2-gates.log` prints `QA-DAEMON-JVM: java.version=25.0.5`.<br>**Build:** `Task :app:assembleDebug` ran and the build ended with `EXIT=0`. "major version 69" = 0 and "Unsupported class" = 0. |
| AC-2 | PASS | RE-VERIFIED (it2) | **Gates:** under `--rerun-tasks` all of `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:jacocoTestReport` and `:app:jacocoTestCoverageVerification` executed, and the build was SUCCESSFUL with 74 of 74 tasks executed (`qa-it2-gates.log`).<br>**JUnit XML** (enabled by the init script `evidence/qa/qa-junit-xml.init.gradle`; no tracked file edited): 75 files, tests=697, failures=0, errors=0, skipped=2 (`qa-it2-junit-totals.txt`). The two skips are the pre-existing `AppTest#shouldGetVersionName` and `AuthPreferencesTest#u012_...` `@Ignore`s.<br>**`@Test` count:** the authoritative command gives 697, equal to the baseline at `77de23da`. There is no `app/src` diff. |
| AC-3 | PASS | CARRIED FORWARD | `gradle/verification-metadata.xml` and `ci.yml` are unchanged. The iteration-1 evidence stands: `verify-metadata` is true at `:4`, there are no disabling flags, and the enforced build passes. The iteration-2 run also enforced verification with no write mode. |
| AC-4 | PASS | RE-VERIFIED (it2) | **Floors:** all `minimum =` values are unchanged (see "What changed"): receiver at `app/build.gradle:544` is 0.50 and compat at `:488` is 0.20. No package was dropped.<br>**Comments:** the stale MEASURED comments are corrected at `:399`, `:480-481`, `:505` and `:533-537`.<br>**Headroom:** zero headroom on `receiver` (29/58 = 50.0%) remains and is recorded at `:533`. |
| AC-5 | PASS | CARRIED FORWARD | The rework changes no app source, manifest, resource, dependency or build setting. Only comments, `gradlew`, `CLAUDE.md` and docs changed, so the APK is functionally unchanged and nothing on the device side changed. I did not restart the emulator.<br>**Iteration-1 evidence** (`evidence/qa/qa-ac5-device.txt`): `install -r` Success, launch `Status: ok`, and 0 `FATAL EXCEPTION` on the API 36 Pixel_9_Pro. |
| AC-6 | PASS | RE-VERIFIED (it2) | **CLAUDE.md content:** `.claude/CLAUDE.md:31` now says the build runs on JDK 25, declared by `gradle/gradle-daemon-jvm.properties`, and that this file takes precedence over `JAVA_HOME` and any `org.gradle.java.home`, including a user-global one. It also gives the `auto-detect=false` failure and its remedy (list a JDK 25 in `paths`, or launch Gradle on JDK 25), the `--version` check, and the hand-maintained `gradlew` block. The diff is 1 line.<br>**Fact-check against my own probes:**<br>• A JDK 17 `JAVA_HOME` launcher produced a JBR 25 daemon (`qa-ac7-launcher17-userglobal.txt`).<br>• The user-global `org.gradle.java.home` (the Android Studio JBR) was **not** the daemon used in either QA gate run. The daemon was `C:\Program Files\Java\jdk-25.0.5`, so `org.gradle.java.home` does not select the daemon when the criteria file is present.<br>• The quoted error text matches `qa-ac7-launcher17-autodetect-off-nopaths.txt`.<br>• A JDK 25 launcher with `paths` set used the launcher JVM (iteration-1 run 1).<br>**code-location.md:** `code-location.md:90-95` states the full ordering, including that without the criteria file a user-global `org.gradle.java.home` overrides `JAVA_HOME`.<br>**Routing:** see "AC-6 routing judgement" below. Met. |
| AC-7 | PASS | CARRIED FORWARD (plus doc re-check) | `gradle/gradle-daemon-jvm.properties` is unchanged. The iteration-1 probes stand: with auto-detect off and a JDK 25 listed, it works; with auto-detect off, no paths and a JDK 17 launcher, it fails with a clear error. The limitation is documented at `code-location.md:96-99` and `.claude/CLAUDE.md:31`. |
| AC-8 | PASS (as written; CI runtime NOT VERIFIED) | CARRIED FORWARD | **Workflow file:** `ci.yml` is unchanged: temurin 25 (`:21,24`), a build-tools 36.0.0 step (`:28-30`), and the stale comment removed.<br>**F3 resolved:** `gradlew:117-127` now exports the Windows flags only under cygwin/msys (Linux probe below).<br>**Still open:** CI has never run. |
| AC-9 | PASS | RE-VERIFIED (it2, partial) | **Line-level coverage:** receiver and compat are identical to iteration 1 run 1 (`qa-it2-head-lines.txt` vs `qa-head-lines-run1.txt`: `diff` empty). The JaCoCo class roots and guard (`app/build.gradle:218-230,293,339`) are unchanged.<br>**Class counts:** the per-package comparison against my JDK 17 baseline (`qa-ac9-class-counts.txt`: every package ≥ baseline, 399 → 401) is carried forward. No source or build input that affects the class set changed. |
| AC-10 | PASS | RE-VERIFIED (it2) | **Test JVM:** JDK 25 (`qa-it2-gates.log`: `QA-TEST-JVM ... jdk-25.0.5 ver=25`).<br>**Results:** 0 failures and 0 errors, with no class-version, ASM or ByteBuddy hits.<br>**Versions** (Robolectric 4.15.1, Mockito 5.18.0, JaCoCo 0.8.14, SDKs 21/23/29) are carried forward unchanged (`app/build.gradle:161,164,213`). |
| AC-11 | PASS | RE-VERIFIED (doc) | **Implementation Notes:** SC-5 is now copied verbatim into implementation-log.md "Rework iteration 2 §1d", with source URLs, as `plan.md:157` required.<br>**Official pages:** my iteration-1 fetch of the Gradle 9.5.1 compatibility page and the AGP 9.0.0 release notes stands. Versions are unchanged (`build.gradle` is unchanged). |
| AC-12 | PASS | CARRIED FORWARD | No source or target setting changed (`app/build.gradle:90-91,107` and `k9mail-vendored/build.gradle:76-77` are unchanged). Iteration-1 `javap` found all 413 app classes at major 61 and vendored at 52. |
| AC-13 | PASS | CARRIED FORWARD | `verification-metadata.xml` is unchanged since iteration 1: +1951 lines, 0 deletions, all `origin="Generated by Gradle"`. The iteration-2 enforced build passed. |
| AC-14 | PASS | RE-VERIFIED (it2) | **Unchanged since iteration 1:** the wrapper jar (sha256 = official `497c8c2a…`), `gradle-wrapper.properties` (`distributionSha256Sum` = official `bafc141b…`) and `gradlew.bat` (stock).<br>**`gradlew` against the stock 9.5.1 script:** it differs only by the clearly marked LOCAL ADDITION block at `gradlew:117-127`, placed after the stock `uname` detection (`:113` `MSYS* \| MINGW* ) msys=true`). The stock `MAX_FD` comment is restored above `:92`. The file mode is still 100755 (diff index line). |
| AC-15 | PASS | CARRIED FORWARD | Versions are unchanged. The rationale is now in the log (§1d). Product-owner acknowledgement of SC-5 remains advisable. |
| AC-16 | PASS | CARRIED FORWARD (plus log re-check) | **Deprecations:** the iteration-1 `--rerun-tasks --warning-mode all` result stands: only the 8 Groovy space-assignment deprecations and the vendored javac warning. The rework adds comments only.<br>**Log corrections:** the log now records the corrected AC-16 note (§1b) and lists the JDK 25 runtime warnings and Kotlin warnings as debt (§1e). |
| AC-17 | PASS | RE-VERIFIED (it2) | The `gradle.properties` diff is comment-only. There are still no `builtInKotlin`/`newDsl` opt-outs and no kapt. |
| AC-18 | PASS | RE-VERIFIED (it2) | **Retained:** both flags are still in `gradle.properties:20`. The reason comment at `:9-19` is corrected: they are Windows-specific, the Linux behaviour is stated with its limits, and an override recipe is given.<br>**`gradlew`:** the export is now OS-gated (`gradlew:124-126`).<br>**Linux behaviour of the jvmargs flag:** probed independently by QA (see Linux probe). |
| AC-19 | PASS | RE-VERIFIED (it2) | `compileSdk`/`targetSdk` 35 and `minSdk` 21 are unchanged (`app/build.gradle` has no non-comment change). `nonTransitiveRClass`/`nonFinalResIds` are true. There is no `org.gradle.java.home` in the repo `gradle.properties`; the comment-only diff adds none. |
| IC-1 | PASS | CARRIED FORWARD | No dependency or plugin line changed. The iteration-1 resolved-graph evidence (`qa-deps-debugRuntime.txt`: Hilt 2.59.2, stdlib 2.3.21) stands. |
| IC-2 | PASS | CARRIED FORWARD | `compileOptions`, `jvmTarget` and the vendored module are unchanged. |
| IC-3 | PASS | CARRIED FORWARD | The four build files have no DSL change in the rework, only comments. |

### AC-6 routing judgement (stated plainly)

The story's ownership note says the Developer "must route that edit through the Engine Librarian rather than editing it directly". This is what happened, per implementation-log.md "Engine Librarian confirmation":
- The Developer routed the change and did **not** edit the file.
- The Engine Librarian authored and verified the replacement text.
- The Librarian declined to perform the write, because it would only have had a product-owner approval relayed by another agent.
- The product owner then approved the exact before/after text directly in the main session, and the orchestrator applied it byte for byte.

**AC-6 is met.** The ownership note forbids the Developer editing the file directly, and that did not happen. The file owner authored and verified the content. The write was authorised by the product owner directly, which is a higher authority than the Librarian.

This **deviates from plan Step 12.2's mechanism** (the Librarian performing the write), and the deviation is recorded transparently in the log. I treat it as a process deviation, not an unmet criterion. If the process owner requires the Librarian itself to perform every write to `.claude/CLAUDE.md`, that is a process decision for the product owner, not a content gap. The content was fact-checked above.

One wording note, not a gap. The AC's parenthetical "(the user-global property overrides `JAVA_HOME`)" describes behaviour without the criteria file:
- `CLAUDE.md:31` states the operative fact: the criteria file overrides both, so neither matters.
- `code-location.md:90-95` states the fallback ordering explicitly.

Read together, the two named documents state the interaction.

## Linux probe of the conditional `gradlew` (QA-run, independent of the Developer)

**Setup.** WSL Ubuntu 24.04, kernel 6.18 (WSL2), Temurin JDK 25.0.5 downloaded into WSL `/tmp`. I used a minimal Gradle project containing the **working-tree** `gradlew`, the wrapper jar and properties, the criteria file and the repo `gradle.properties`, copied byte-identically (the sha256 of each was checked against the repo). Evidence is in `evidence/qa/qa-it2-linux-probe.txt`. The scratch directory was deleted and WSL terminated afterwards.

| Probe | Result |
|-------|--------|
| 1. Stub `java` (no JDK) under Linux `sh ./gradlew --version` | `STUB-JAVA sees JAVA_TOOL_OPTIONS=[]`: the Windows block is **not exported** on Linux |
| 2. Real JDK 25 + `gradlew` + repo `gradle.properties` (Windows selector flag in `org.gradle.jvmargs`); a task inside the daemon prints its state | `env JAVA_TOOL_OPTIONS=[]`. Daemon `inputArguments=[-Xmx2048m, -Dfile.encoding=UTF-8, ...]`: the `-D` selector flag is **not** on the daemon command line. The system property **is** set afterwards (`SelectorProvider=sun.nio.ch.WindowsSelectorProvider`), but `SelectorProvider.provider()` = `sun.nio.ch.EPollSelectorProvider` and `Selector.open OK` → EXIT=0 |
| 3. Forked JVM (`JavaExec`) from that daemon | `JAVA_TOOL_OPTIONS=[null]`, provider `EPollSelectorProvider` → EXIT=0 |

**What this establishes.** On Linux:
- `gradlew` no longer injects the Windows-only class.
- The repository `org.gradle.jvmargs` flag is applied to the daemon only as a system property, after the JDK's `SelectorProvider` singleton is already initialised (EPoll). So it does not crash the daemon, and it does not reach forked JVMs.

This independently confirms the effective jvmargs that the code reviewer noted were missing from the Developer's daemon-probe files.

**What it does not establish:**
- The full Android build on Linux: AGP, KSP, lint, R8 and Robolectric test workers were not run, because there is no Android SDK in WSL.
- Any other daemon that might read the system property before initialising NIO. A Kotlin compile daemon was probed by the Developer only.
- The GitHub runner itself (setup-java, sdkmanager, R14 daemon-criteria detection).

**CI is NOT verified.**

## Not Verified (none required by an AC)

| Item | Required by an AC? | Status |
|------|--------------------|--------|
| First GitHub CI run (JDK 25 criteria on the runner, sdkmanager step, full Linux app build) | No. AC-8 is file content. | NOT VERIFIED. The Linux risk from `gradlew` is removed (probe above), but the full build is unrun. |
| C-3 integration-OFF branch ("...but ignored") | No. This is contract check C-3. | NOT VERIFIED. No unit test covers `BackupBroadcastReceiver` (0 of 11 lines). The device setting must not change. |
| Release (R8) APK run on a device | No. AC-5 is the debug APK. | NOT VERIFIED. Equivalence rests on build-output diffs only (Developer C-2). |
| Full Android build on a Linux JVM | No | NOT VERIFIED (see Linux probe). |
| `gradlew.bat` (cmd/PowerShell) on this Windows machine | No | NOT VERIFIED. The stock script lacks the NIO workaround; this is documented at `code-location.md:105` ("use Git Bash `./gradlew`"). |

## Integration Path Verification

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `gradlew:117-127` OS-gated `JAVA_TOOL_OPTIONS` export | `./gradlew` | stock `uname` case (`:110-115`) sets `msys`/`cygwin` → `if "$cygwin" \|\| "$msys"` → export → inherited by launcher, daemon and test JVMs | yes. Windows Git Bash: "Picked up JAVA_TOOL_OPTIONS" ×5 in `qa-it2-gates.log` and the build succeeds. Linux: not exported (`qa-it2-linux-probe.txt`) |
| `gradle/gradle-daemon-jvm.properties` | `./gradlew` | launcher → criteria → JDK 25 daemon | yes (`qa-it2-gradle-version.txt`; daemon `java.version=25.0.5`) |
| JaCoCo class roots and guard (`app/build.gradle:218-230`) | `jacocoTestReport` / `jacocoTestCoverageVerification` | `doFirst` guard at `:293`, `:339` | yes. Both tasks executed in iteration 2 |
| `.claude/CLAUDE.md:31` Build JDK bullet | developer and agent sessions (project instructions) | read at session start | yes. Content fact-checked (AC-6 row) |
| CI workflow (`ci.yml`) | GitHub push/PR | setup-java 25 → sdkmanager → gradlew | no. Never executed (unchanged since iteration 1) |

## Behavioral Contract Verification

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| Build gate contract | three gates succeed on JDK 25 | `qa-it2-gates.log` BUILD SUCCESSFUL, 74/74 executed | yes |
| Test inventory | 697 tests, 0 failed, the same 2 skipped | `qa-it2-junit-totals.txt` | yes |
| JaCoCo floors | 15 rules, floors unchanged | `app/build.gradle:358-598`; non-comment diff = 0 value changes | yes (receiver at zero headroom) |
| Coverage stability | receiver 29/58, compat 16/73 as in iteration 1 | `qa-it2-head-lines.txt` = `qa-head-lines-run1.txt` | yes |
| `gradlew` Windows workaround (capability retained, U-002) | exported under Git Bash, not elsewhere | `gradlew:124-126` | yes (Windows and Linux probes) |
| Dependency verification and wrapper checksums | unchanged and enforced | `git diff --quiet 8dc3e417 -- gradle/` exits 0 | yes |
| CNTR-MODERNIZATION-005 opt-in gate (OFF branch) | "...but ignored" | `BackupBroadcastReceiver.java:51-57` (unchanged) | not verified (no test; follow-up) |

## Requirement Scope Coverage

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| BUG-017 | Gradle, javac, kotlinc and tests run on JDK 25 | yes | `qa-it2-gates.log` (daemon and test JVM 25.0.5); `gradle/gradle-daemon-jvm.properties:2` |
| BUG-017 | bytecode targets unchanged (17 / 8) | yes | `app/build.gradle:90-91,107`; `k9mail-vendored/build.gradle:76-77` |
| BUG-017 | compileSdk/targetSdk 35 and debug runtime unchanged | yes | `app/build.gradle:21,25`; `qa-ac5-device.txt` |
| BUG-017 | supported toolchain combination | yes | log §1d (SC-5 + sources); iteration-1 page fetch |
| BUG-017 | dependency verification not weakened | yes | `gradle/` unchanged since iteration 1 |
| BUG-017 | gates not silently shrunk | yes | floors unchanged; class counts ≥ baseline; line shift explained in log §1a and `app/build.gradle:533-537` |
| BUG-017 | build docs describe the real JDK | yes | `.claude/CLAUDE.md:31`; `code-location.md:85-105` |
| REQ-MODERNIZATION-003 (CI contract) | CI commands and JaCoCo gate | file content yes; runtime not verified | `ci.yml` (unchanged) |

## Test Results

| Run | Tree / JDK | Command | Result |
|-----|-----------|---------|--------|
| QA iteration 2 | working tree on `8dc3e417` / JDK 25.0.5, bare Git Bash | `clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification --rerun-tasks` + JUnit-XML init script | BUILD SUCCESSFUL, 74/74 executed. **697 tests, 0 failures, 0 errors, 2 skipped** |
| QA iteration 1 runs 1 and 2 | `8dc3e417` / JDK 25.0.5 | same (run 2: tests + report) | 697/0/0/2 both |
| QA baseline | `77de23da` / JBR 17.0.14 | same | 697/0/0/2 |
| `@Test` count | HEAD | authoritative command | 697 |

## Regression Results

- **Rework diff:** confined to `gradlew` (OS gate), comments in `app/build.gradle` and `gradle.properties`, one `CLAUDE.md` bullet, one `code-location.md` note, and log sections. All other build inputs are byte-identical to iteration 1 (git-verified).
- **Behaviour:** the build and tests behave the same, and the coverage of the two sensitive packages is identical line for line.
- **Capability:** the Windows `JAVA_TOOL_OPTIONS` workaround is retained on Windows (the build would otherwise fail with "Unable to establish loopback connection") and removed elsewhere.

## Follow-ups (iteration-1 list, updated)

| # | Item | Status | Owner |
|---|------|--------|-------|
| 1 | `.claude/CLAUDE.md` Build JDK bullet (AC-6) | **CLOSED.** `.claude/CLAUDE.md:31`, PO-approved, routing deviation recorded | — |
| 2 | Log corrections: coverage cause, AC-16 note, AC-7 note, SC-5 with sources | **CLOSED.** Log §1a-§1d | — |
| 3 | `gradlew` "benign on non-Windows" defect (F3) | **CLOSED.** OS-gated at `gradlew:117-127`; QA Linux probe confirms | — |
| 4 | Stale MEASURED comments in `app/build.gradle` | **CLOSED.** `:399,480-481,505,533-537` | — |
| 10 | JDK 25 / Kotlin 2.3 warnings recorded as debt | **CLOSED.** Log §1e | — |
| 5 | `BackupBroadcastReceiver` Robolectric test for the ON and OFF branches. Closes C-3 and restores `receiver` headroom (now 0) | OPEN | new story |
| 6 | AGP 9 `processDebugUnitTestManifest` lacks the app's components (Robolectric fidelity): investigate and decide | OPEN | new story |
| 7 | First GitHub CI run on JDK 25 (setup-java, sdkmanager, full Linux build, R14) | OPEN | product-owner decision / sprint follow-up |
| 8 | Pre-existing red `:app:lintDebug` CI step (regenerate or fix the lint baseline on AGP 9, or accept known-red) | OPEN | product-owner decision |
| 9 | `DEVELOPMENT.md` stale JDK 17 / `org.gradle.java.home` claims; `.kotlin/` in `.gitignore` (pre-existing); `*.bat text eol=crlf` in `.gitattributes`; optional `gradlew.bat` workaround | OPEN | new story (docs/hygiene) |
| 11 | Acknowledge the SC-5 reading of AC-11/AC-15 (Gradle 9.5.1 rather than 9.3.1) | OPEN (advisory) | product-owner decision |
| 12 | Record whether the AC-6 routing deviation (orchestrator write after direct PO approval) is acceptable process going forward | NEW (advisory) | product-owner decision |
| 13 | `evidence/` (about 24 MB) should be pruned before any commit; see review-security "Evidence hygiene". QA's own `evidence/qa/` is about 2.5 MB, mostly three JaCoCo XMLs that may be dropped once this gate closes | NEW | product-owner decision / orchestrator |
| 14 | Release (R8) APK device run | OPEN (residual risk) | new story |

## Evidence Files (QA-produced)

All files are under `sdlc/artifacts/build/sprints/sprint-017/U-060/evidence/qa/`:
- **Iteration 2:** `qa-it2-gradle-version.txt`, `qa-it2-gates.log`, `qa-it2-junit-totals.txt`, `qa-it2-head-lines.txt` and `qa-it2-linux-probe.txt`. A redundant `qa-it2-head-jacoco.xml` copy was deleted after extracting the lines.
- **Iteration 1 (still valid for the carried-forward rows):** `qa-gates-run1.log`, `qa-gates-run2.log`, `qa-baseline-77de23da-run1.log`, `qa-junit-totals-run1.txt`, `qa-junit-totals-run2.txt`, `qa-head-jacoco-run1.xml`, `qa-head-jacoco-run2.xml`, `qa-baseline-jacoco-run1.xml`, `qa-head-lines-run1.txt`, `qa-baseline-lines-run1.txt`, `qa-ac9-class-counts.txt`, `qa-deps-*.txt`, `qa-ac5-device.txt`, `qa-ac7-*.txt` and `qa-junit-xml.init.gradle`.

**Cleanup:**
- Gradle daemons stopped ("No Gradle daemons are running").
- WSL scratch `/tmp/u060qa` deleted and the distro terminated (it was stopped before).
- The emulator was not started in iteration 2.
- No tracked file modified and no git state changed.
- The text evidence contains no personal data.

## Phase Completion Report
---
story_id: "U-060"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-017/U-060/qa-results.md"
story_status: "in-progress"
current_build_phase: "validation"
ac_passed: 22
ac_total: 22
errors: []
---
