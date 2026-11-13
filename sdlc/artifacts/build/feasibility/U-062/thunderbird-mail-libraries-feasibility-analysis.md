---
artifact_type: feasibility-analysis
story_id: U-062
requirement: REQ-MODERNIZATION-019 Part A
upstream_pin: PENDING
components_pin: PENDING
start_commit: c8f04558dff63ab24ed31b3e3e19ee99c5cb8afc
---
# Feasibility Analysis: Replacing :k9mail-vendored with Thunderbird for Android Mail Libraries
Upstream pin: thunderbird/thunderbird-android @ PENDING (PENDING, "PENDING"); rule: PENDING; main head at I1: PENDING; drift: PENDING
## Baseline

The repo baseline this analysis is measured against. All values were read at the start commit. Test baseline: **682 run, 2 skipped, not REQ-MODERNIZATION-019's 697** (sprint-018 U-061 deleted 15 donation tests; 697 was the sprint-017 baseline).

| Item | Value | File:line |
|------|-------|-----------|
| Start commit | `c8f04558dff63ab24ed31b3e3e19ee99c5cb8afc` (branch `sdlc/modernization-plan`) | `git rev-parse HEAD` |
| Modules | `:app`, `:metadata`, `:k9mail-vendored` | `settings.gradle:1` |
| Gradle | 9.5.1 | `gradle/wrapper/gradle-wrapper.properties:4` |
| AGP | 9.0.1 | `build.gradle:10` |
| Kotlin Gradle plugin | 2.3.21 | `build.gradle:14` |
| JDK (build) | 25 (daemon JVM criteria; run showed `java.version=25.0.5`) | `gradle/gradle-daemon-jvm.properties:2` |
| Bytecode, app | Java 17; Kotlin `JVM_17` | `app/build.gradle:90-91`, `app/build.gradle:107` |
| Bytecode, vendored module | Java 8 | `k9mail-vendored/build.gradle:76-77` |
| minSdk, app | 21 | `app/build.gradle:24` |
| minSdk, vendored module | 21 | `k9mail-vendored/build.gradle:54` |
| compileSdk, app / vendored | 35 / 35 | `app/build.gradle:21`, `k9mail-vendored/build.gradle:52` |
| targetSdk, app | 35 | `app/build.gradle:25` |
| Release minification | on | `app/build.gradle:53` |
| kotlinx-coroutines | 1.7.3 | `app/build.gradle:118-119` |
| Vendored dependencies | mime4j 0.8.11, commons-io 2.16.1, jzlib 1.0.7, jutf7 1.0.0, androidx.annotation 1.7.1 | `k9mail-vendored/build.gradle:100`, `:102`, `:107`, `:109`, `:111`, `:114` |
| Vendored origin | fork `jberkel/k-9`, commit `eaf689025ec28d55e3b62f5ea3b1f185f410ee6b`, branch `sms-backup-plus-1.5.11` | `k9mail-vendored/NOTICE:4-6` |
| k9-referencing files outside `mail/transport/` | 14 (`grep -rln "com\.fsck\.k9" app/src/main/java \| grep -v /mail/transport/ \| wc -l`) | `app/src/main/java` |
| Tests | 682 run, 2 skipped, 0 failures, 0 errors, 73 result files (`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' \| grep -c "@Test"` = 682) | `app/src/test` |
| Release APK (`app-release-unsigned.apk`) | 1995017 bytes | `app/build/outputs/apk/release/` (build output, not tracked) |
| Dex (single `classes.dex`) | 23904 method ids, 3627 class defs (`<sdk>/build-tools/36.0.0/dexdump.exe -f`) | build output, not tracked |
| Vendored footprint in `mapping.txt` | original classes: com.fsck.k9 120; org.apache.james.mime4j 156; org.apache.commons.io 8; com.jcraft.jzlib 8; com.beetstra 7. Member lines (upper bounds; one method can span several lines): com.fsck.k9 2892 methods / 407 fields; mime4j 3111 / 499; commons-io 90 / 33; jzlib 602 / 117; jutf7 (com.beetstra) 161 / 35 | build output (R8 9.0.32, min_api 21) |

### Gate run at the start commit (D2 experiment e)

Command: `./gradlew -I <repo>/sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/tools/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease > <scratch>/logs/I0-gates.log 2>&1`, then `python -I <tools>/junit_totals.py app/build/test-results/testDebugUnitTest`. Exit code 0 (`BUILD SUCCESSFUL in 3m 13s`). Totals: `files=73 tests=682 failures=0 errors=0 skipped=2`. After the run, `git status --porcelain` equals the I0 record below plus the collapsed `?? sdlc/artifacts/build/feasibility/` entry, and `git diff --stat <start> -- app k9mail-vendored metadata gradle settings.gradle build.gradle gradle.properties gradlew gradlew.bat` is empty: the baseline run changed no tracked file.

### I0 start-state records (IC-1, IC-4)

`git status --porcelain` at I0:

```
 M sdlc/.skill-delegation-audit.log
?? sdlc/.skill-context.json
?? sdlc/artifacts/build/sprints/sprint-019/
```

`git status --porcelain --untracked-files=all` at I0 (the form that shows files inside collapsed directories; D4.4):

```
 M sdlc/.skill-delegation-audit.log
?? sdlc/.skill-context.json
?? sdlc/artifacts/build/sprints/sprint-019/U-062-thunderbird-mail-libraries-feasibility-analysis.md
?? sdlc/artifacts/build/sprints/sprint-019/U-062/compiled-pipeline.json
?? sdlc/artifacts/build/sprints/sprint-019/U-062/contract-verification.md
?? sdlc/artifacts/build/sprints/sprint-019/U-062/expert-depth-check.md
?? sdlc/artifacts/build/sprints/sprint-019/U-062/plan-review.md
?? sdlc/artifacts/build/sprints/sprint-019/U-062/plan.md
?? sdlc/artifacts/build/sprints/sprint-019/execution-plan.json
?? sdlc/artifacts/build/sprints/sprint-019/sprint-plan.md
```

`git ls-files --others --exclude-standard` at I0:

```
sdlc/.skill-context.json
sdlc/artifacts/build/sprints/sprint-019/U-062-thunderbird-mail-libraries-feasibility-analysis.md
sdlc/artifacts/build/sprints/sprint-019/U-062/compiled-pipeline.json
sdlc/artifacts/build/sprints/sprint-019/U-062/contract-verification.md
sdlc/artifacts/build/sprints/sprint-019/U-062/expert-depth-check.md
sdlc/artifacts/build/sprints/sprint-019/U-062/plan-review.md
sdlc/artifacts/build/sprints/sprint-019/U-062/plan.md
sdlc/artifacts/build/sprints/sprint-019/execution-plan.json
sdlc/artifacts/build/sprints/sprint-019/sprint-plan.md
```

The tree was dirty at I0 with workflow bookkeeping only (D4.2); this is the list that IC-4 subtracts.

### Research input copy (IC-5)

| Item | Value |
|------|-------|
| Source | the orchestrator's session scratchpad, `thunderbird-mail-libs-research.md` |
| Copy | `sdlc/artifacts/build/feasibility/U-062/inputs/thunderbird-mail-libraries-research-2026-11-12.md` |
| Source SHA-256 | `f2b9610bedc2ca322ef496a1f45ec50b974dc6103a0e63b30ed320ea704aece9` |
| Copy SHA-256 (whole file, banner included) | `342e149aef57ee4d261d50c8a808fba9a6b5c3df87314514e4d6d4be8cdf50e2` |
| Copy SHA-256 with line 1 removed (`tail -n +2`) | `f2b9610bedc2ca322ef496a1f45ec50b974dc6103a0e63b30ed320ea704aece9` (equals the source hash) |
| Banner (line 1) | `UNREVIEWED INPUT - not a citation source` |
| Line counts (`wc -l`) | source 290, copy 291. The story says the source has 291 lines; the measured count is 290 and identity is proven by the hash (D4.3) |
| Redactions (IC-3) | none; the copy is byte-identical to the source apart from the banner line |

The input is an unreviewed hint. No citation in this analysis targets it.

## 1. Consumability
PENDING
## 2. Modules and dependency graph
PENDING
## 3. Licence
PENDING
## 4. Platform requirements
PENDING
## 5. API mapping
PENDING
## 6. Authentication
PENDING
## 7. Custom trust, pinning and SNI
PENDING
## 8. Size and method count
PENDING
## 9. Fallback options
PENDING
## 10. Separability
PENDING
## 11. Sizing and split
PENDING
## 12. Recommendation
PENDING
## Citations

| ID | Claim | URL | Commit/tag/version | Retrieved | Status |
|----|-------|-----|--------------------|-----------|--------|

## Experiment log

Scratch location: `<scratch>`; repo: `<repo>`; Android SDK: `<sdk>`.

### Experiment (e): release baseline in the real tree

Run at I0 (re-run at I4). Upstream commit: not applicable. Gradle 9.5.1; JDK 25.0.5 (`U061-DAEMON-JVM: java.version=25.0.5`). Command: see the gate run in Baseline. Exit code 0. Result: 682 tests, 2 skipped, 0 failures; release APK 1995017 bytes; 23904 methods and 3627 classes by `dexdump -f` on the single `classes.dex`; no tracked file changed (empty `git diff --stat`, porcelain unchanged apart from the permitted new directory).

### Experiment (a): partial clone of thunderbird-android
PENDING
### Experiment (b): upstream build configuration read
PENDING
### Experiment (c): build upstream mail modules
PENDING
### Experiment (d): throwaway copy compiled against upstream
PENDING
### Experiment (f): scratch Gradle project for third-party closure
PENDING
### IC-3, IC-1 and IC-4 commands
PENDING

## Product owner decision

Pending: to be recorded by the orchestrator or Artifact Librarian (AC-15).
