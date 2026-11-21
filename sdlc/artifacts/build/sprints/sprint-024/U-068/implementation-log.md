---
artifact_type: implementation-log
story_id: "U-068"
gate_id: "implementation"
checkpoint: "implementation"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-11-21T11:42:09Z"
iteration: 1
base_commit: "4300ffa98370b77dcc4dd8d12142079aada8313a"
files_changed: 0
files_created: 0
tests_added: 3
tests_passing: 0
---

# Implementation Log: U-068 - Raise minSdk to 24 and remove what the floor makes dead

Plan: `sdlc/artifacts/build/sprints/sprint-024/U-068/plan.md` (iteration 2), followed step by step. Evidence is under `sdlc/artifacts/build/sprints/sprint-024/U-068/evidence/`. Raw logs, saved JaCoCo XML copies, pristine copies and commit messages were kept in a work folder outside the repository. One local commit per increment, explicit paths, nothing pushed. No emulator or device was used (I8 is the orchestrator's).

`START` = `4300ffa98370b77dcc4dd8d12142079aada8313a` (subject "Sprint-023 closed: U-067 done, BUG-022 closed; U-068 plan gates passed"), branch `sdlc/modernization-plan`. `C0` = **753** (authoritative command at `START`).

## Start conditions (SC-1 to SC-6)

| SC | Check and result | Evidence |
|----|------------------|----------|
| SC-1 | U-066 `status: done`, U-067 `status: done`. `git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat CHANGES DEVELOPMENT.md .github sdlc/artifacts/engagement` printed nothing (`porcelain-end` preceded by no line). | `evidence/I0-start.txt` |
| SC-2 | CR-015 and CR-016 `status: implemented`; `3af4ce32` and `f0d0d17e` are ancestors of HEAD (`cr-ancestor-exit=0`). | `evidence/I0-start.txt` |
| SC-3 | `START` and `C0` recorded above. `@Ignore` on code lines: 2 (`AppTest.java:13`, `AuthPreferencesTest.java:1079`). | `evidence/I0-start.txt` |
| SC-4 | `jps -l` listed no Gradle JVM (`jps-gradle-end` preceded by no line); the delegation states that nobody else runs Gradle in this checkout. | `evidence/I0-start.txt` |
| SC-5 | U-067 is done (its fix is `c12a2788`, inside `START`); BUG-023 is U-069, not started. | `evidence/I0-start.txt` |
| SC-6 | No device or emulator was touched by this run. | - |

All 19 files of plan table 2.1: HEAD blob equals working-tree hash equals planning blob (19 of 19, `evidence/I0-start.txt`). `plan-head-ancestor-exit=0`. Commits between the plan head `c12a2788` and START: `36e715e7` and `4300ffa9`, documents only (`moved-files-end` preceded by nothing). Rows D1 to D5: none fired.

## Steps

### I0 - Baseline at START (2026-11-21T11:27:07Z to 11:42Z; no tracked file changed)

- **Gate G-I0** (`clean`, `assembleDebug`, `testDebugUnitTest`, `jacocoTestReport`, `jacocoTestCoverageVerification`, `assembleRelease`, with the read-only init script for the XML): `exit=0`, `BUILD SUCCESSFUL in 10m 51s`, JDK `25.0.5.1`; `files=79 tests=753 failures=0 errors=0 skipped=2`. Gradle 9.5.1, Daemon JVM "Compatible with Java 25 ... (from gradle/gradle-daemon-jvm.properties)". Evidence `I0-gates.txt`, `I0-junit-totals.txt`, `I0-gradle-version.txt`, `I0-cases.txt`. Rows D6, D7: none fired (the Gradle total 753 equals `C0`).
- **Per-package coverage at START** (`I0-jacoco-packages.txt`, `I0-jacoco-inventory.json`; 23 packages): `receiver` 29/58 = 50.0 % (spare 0), `compat` 16/73 = 21.9 % (spare 1), `scheduler` 140/188 = 74.5 %, `activity` 199/1011, `calendar` 37/50. These equal the plan's L9 figures for `receiver`, `compat`, `scheduler` and `calendar`; `activity` is 199 (plan 189, an expectation only; floor 0.10, spare 97). Rows D8, D9: not fired. (`jacocoTestCoverageVerification` passed.)
- **Build outputs** (`I0-merged-*.xml`, `I0-manifest-*.txt`, `I0-kept-names.txt`, `I0-configuration.txt`, `I0-apk-entries-*.txt`, `I0-apk.txt`, `I0-javap.txt`): debug APK 542 entries, `so=0`, 17 dex; release APK 525 entries, `so=0`, 1 dex; 48 kept names; all nine reflection-sensitive classes self-mapped (1 each); class-file major version 61 for the app classes (`App`, `BackupWorker`) and 52 for the vendored `Address`. Row D13: not fired (`.so` count 0).
- **Greps at START** (`I0-greps.txt`): AC-7 grep 24 lines (5 imports); AC-8 grep 29; AC-4 (e) grep 6; AC-11 (f) sweep 9; `maxSdkVersion` in the manifest 2 (`:41`, `:50`); `minSdk 21|SDK 21` in `app/build.gradle` 2 (`:153`, `:160`). All equal the plan.
- **Lint baseline count** (`I0-lint-baseline-count.txt`): 37 by `grep -c '^ *<issue *$'`, 38 by a plain `<issue` match. The design's 38 counts the root `<issues` tag; the story's 37 and its counting command govern (Scope Concern 2, ruled; no design note added). Row D11: not fired.
- **Lint at START** (`I0-lint.txt`, `I0-lint-ids.txt`): `exit=1` (pre-existing red, D17: recorded, not fixed, not gated). "Lint found 39 errors (and 22 errors filtered by baseline)"; "15 errors/warnings were listed in the baseline file but not found in the project" (U0 = **15**; the plan expected 16 from the AGP 9.0.1 record of U-060, an expectation only). Rule-ID table (closes design assumption A22): UnusedResources 18, LogTagMismatch 4, UseRequiresApi 4, CheckResult 2, CustomX509TrustManager 2, ObsoleteSdkInt 2, TrustAllX509TrustManager 2, UseKtx 2, AndroidGradlePluginVersion 1, StaticFieldLeak 1, UnusedAttribute 1 (all Error: 39), plus LintBaseline 1 and LintBaselineFixed 1 (Hint). Row D10: not fired (the XML report was readable).

### I1 - JUnit XML on (AC-2; evidence `I1-*`)

- E1: `build.gradle:30` now reads `reports.junitXml.required = true`; line 29 unchanged. `git diff -U0 -- build.gradle` shows exactly one `-` and one `+` line.
- Gate G3 (no init script): `exit=0`, `BUILD SUCCESSFUL in 4m 25s`. The build's own XML: 79 files (`ls $R/TEST-*.xml`); totals `tests=753 failures=0 errors=0 skipped=2`, equal to I0's init-script totals (`totals-diff-exit=0`, `I1-xml-check.txt`). Rows D14, D15: not fired.
- **Must hold (S1-I1):** "gates green; XML totals equal I0" - held.
- Commit: see "I1 commit" at the top of the I2 entry.

<!-- LOG-ENTRIES-END -->
