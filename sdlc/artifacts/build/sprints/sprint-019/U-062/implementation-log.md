---
artifact_type: implementation-log
story_id: "U-062"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-11-12"
files_changed: 0
files_created: 2
tests_added: 0
tests_passing: 0
---

# Implementation Log: U-062

Analysis-only story. No product, build or dependency file is changed. Placeholders: `<repo>`, `<scratch>`, `<sdk>`.
Runtime Claims: none (empty by design; this story makes no runtime-provable claim about product behaviour).
Contract Adherence: no `integration_contracts` in the story; CNTR-MODERNIZATION-007 and -001 are read-only context.

## I0 START 2026-11-12 (UTC)

Inputs: plan.md (iteration 3), story file, orchestrator turn notes (R1-R6, O-A..O-E, expert-depth notes), research input in the orchestrator's session scratchpad (`thunderbird-mail-libs-research.md`).

### I0.1 Start state

Scratch guard: `mkdir -p <scratch>/logs` then the `case` check; it printed no ABORT.

START (`git rev-parse HEAD`): `c8f04558dff63ab24ed31b3e3e19ee99c5cb8afc`

`git status --porcelain`:
```
 M sdlc/.skill-delegation-audit.log
?? sdlc/.skill-context.json
?? sdlc/artifacts/build/sprints/sprint-019/
```
`git status --porcelain --untracked-files=all`:
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
`git ls-files --others --exclude-standard`:
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
(`implementation-log.md` is created after this snapshot.)

status: done
NEXT: I0.2 input copy (hash source, compare to planning hash, build copy with banner, verify, IC-3 scan)

### I0.2 Input copy (IC-5)

Commands (source = the orchestrator's session scratchpad, `thunderbird-mail-libs-research.md`; hashed from stdin so no path is printed):
```
sha256sum < "<src>" | cut -d' ' -f1
{ printf '%s\n' 'UNREVIEWED INPUT - not a citation source'; cat "<src>"; } > $IN
head -1 $IN ; tail -n +2 $IN | sha256sum ; wc -l < <src> ; wc -l < $IN
```
Results:
- source SHA-256: `f2b9610bedc2ca322ef496a1f45ec50b974dc6103a0e63b30ed320ea704aece9` = planning hash (MATCH)
- copy line 1: `UNREVIEWED INPUT - not a citation source` (equals banner)
- `tail -n +2` of copy SHA-256: `f2b9610bedc2ca322ef496a1f45ec50b974dc6103a0e63b30ed320ea704aece9` (equals source hash)
- whole copy SHA-256 (banner included): `342e149aef57ee4d261d50c8a808fba9a6b5c3df87314514e4d6d4be8cdf50e2`
- `wc -l`: source 290, copy 291 (story says 291 for the source: discrepancy D4.3, identity proven by hash)
- IC-3 procedure on copy and log: pattern files in scratch (`ic3-email.re`, `ic3-paths.re`, `ic3-control.txt`) plus runner `ic3.sh` in scratch. Control counts `paths=4 email=1 host_user=2` (the expected 4/1/2). Guard did not fire. Matches on copy and log: 0 for email, paths and host/user.
- Note: first control attempt gave paths=3 because a shell layer halved the doubled backslash in the path pattern file; the file was re-written with the Write tool, control then gave 4. That run was treated as invalid and not recorded as "0 matches".
- Redactions: none. Identity check is therefore plain (copy minus banner equals source).

status: done
NEXT: I0.3 baseline facts (re-run Verified Facts greps)

### I0.3 Baseline facts at START (`c8f04558`)

Commands: the Verified Facts greps (settings, daemon properties, wrapper, root/app/vendored build files, `grep -rln "com\.fsck\.k9" app/src/main/java | grep -v /mail/transport/ | wc -l`, Headers, SNI test, port, adapter, workers). Measured values (`file:line`):

| Fact | Measured | Matches plan table? |
|------|----------|---------------------|
| Modules | `settings.gradle:1` `include ':app', ':metadata', ':k9mail-vendored'` | yes |
| JDK daemon | `gradle/gradle-daemon-jvm.properties:2` `toolchainVersion=25` | yes |
| Gradle | `gradle/wrapper/gradle-wrapper.properties:4` gradle-9.5.1-bin | yes |
| AGP / KGP | `build.gradle:10` 9.0.1; `build.gradle:14` 2.3.21 | yes |
| compile/min/target SDK | `app/build.gradle:21` 35, `:24` 21, `:25` 35 | yes |
| App bytecode | `app/build.gradle:90-91` VERSION_17; `:107` JVM_17 | yes |
| Release minify | `app/build.gradle:53` `minifyEnabled true`; signing `:55`, keystoreProperties `:13-16` | yes |
| Coroutines | `app/build.gradle:118-119` 1.7.3 | yes |
| Vendored module | `k9mail-vendored/build.gradle:52` compileSdk 35, `:54` minSdk 21, `:76-77` Java 8 | yes |
| Vendored deps | mime4j `:100,:102` 0.8.11; commons-io `:107` 2.16.1; jzlib `:109` 1.0.7; jutf7 `:111` 1.0.0; androidx.annotation `:114` 1.7.1; bump history `:16-19` | yes |
| NOTICE | origin and SHA `k9mail-vendored/NOTICE:4-8`; versions at capture `:19-21`; JitPack 404 `:25-30` | yes (lines 20-21 hold the old versions) |
| k9 files outside transport | 14 | yes |
| Headers.java | 13 `"X-smssync-*"` constants within `:8-22`; 15 `String` lines in `:8-26` | yes |
| SNI test | `app/src/test/java/com/zegoggles/smssync/mail/ssl/DefaultTrustedSocketFactorySniTest.java`: 10 `@Test` annotations at lines 47, 69, 97, 132, 150, 170, 186, 201, 215, 230 (plan's 48..231 are the following method lines) | yes, one-line offset |
| Vendored SNI guard | `DefaultTrustedSocketFactory.java:246` `new SNIHostName` (import `:20`) | yes |
| Pinning | `PinnedCertificateSocketFactory.java:55` implements TrustedSocketFactory; `:75` four-argument `createSocket` | yes |
| Live transport | `K9MailTransport.java:414` `BackupImapStoreDelegate extends ImapStore`; `:696` `BackupFolder extends ImapFolder`; `Item.BODY` `:256`, `:299`; `Item.DATE` `:258`, `:731`; `BackupImapStore.java:181` `Item.DATE` | yes |
| Internal date | `HeaderGenerator.java:56` `setInternalDate`; vendored `ImapFolder.java:1177` `APPEND %s (%s) "%s" {%d}` | yes |
| Vendored ENVELOPE list | `ImapFolder.java:686-689` | yes |
| Port | `MailTransport.java`: `checkSettings` `:50`, `openFolder` `:62`, `appendMessages` `:87`, `getMessages` `:102`, `fetch` `:117`, `closeFolders` `:128`, `importMessageBody` `:149` | yes |
| Adapter | `BinaryTempFileBody.setTempDirectory` `K9MailTransport.java:129`; "Unable to get IMAP prefix" text match `:164,181,212,238,266,395`; `closePooledConnections()` `:558`; `NONEXISTENT` comments `:572,:609,:649`; `MessageComparator` `:735`, class `:765` | yes |
| Workers | `BackupWorker.kt:265` catch, `:442` `e.status == 400`, `:377` `closeFolders()`; RestoreWorker uses same pattern | yes |
| X-GM / XLIST in app + vendored main | 0 matches | yes |
| `@Test` count at HEAD (story's `git grep`) | 682 | yes |
| JUnit tooling | `sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/tools/` holds `junit-xml.init.gradle`, `junit_totals.py` | yes |
| `local.properties` has `sdk.dir` | yes (value not recorded) | n/a |

No difference from the plan's table.

status: done
NEXT: I0.4 gate run (assembleDebug, testDebugUnitTest, jacocoTestCoverageVerification, assembleRelease), then APK/dex/mapping baseline

### I0.4 Gates and size baseline (D2-e)

Command (shell without `MSYS_NO_PATHCONV`; raw log in `<scratch>/logs/I0-gates.log`, not in the repo):
```
rm -rf app/build/test-results/testDebugUnitTest
./gradlew -I $T/junit-xml.init.gradle --console=plain :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification :app:assembleRelease > <scratch>/logs/I0-gates.log 2>&1
python -I $T/junit_totals.py app/build/test-results/testDebugUnitTest
```
- exit code: 0; `BUILD SUCCESSFUL in 3m 13s`; 152 actionable tasks (77 executed, 75 up-to-date). Gradle 9.5.1.
- JDK evidence line from the init script: `U061-DAEMON-JVM: java.version=25.0.5`
- totals: `files=73 tests=682 failures=0 errors=0 skipped=2`; `@Test` git-grep at HEAD = 682 (match)
- release APK: `app/build/outputs/apk/release/app-release-unsigned.apk` (exactly one; unsigned because no keystore properties); bytes (`wc -c <`) = **1995017**
- dex: `unzip -q -o <apk> 'classes*.dex' -d <scratch>/dex0` gives a single `classes.dex`; tool `<sdk>/build-tools/36.0.0/dexdump.exe -f` (attempt 1 worked; `apkanalyzer` not used, per the sprint-018 finding). `method_ids_size : 23904`, `class_defs_size : 3627`. Totals: **23904 methods, 3627 classes**.
- mapping (`app/build/outputs/mapping/release/mapping.txt` copied to scratch; R8 9.0.32, min_api 21): original classes per prefix by `grep -cE '^<prefix>\.[^ ]+ -> '`: com.fsck.k9 = 120; org.apache.james.mime4j = 156; org.apache.commons.io = 8; com.jcraft.jzlib = 8; com.beetstra = 7. Whole-file class lines: 3631 (includes classes that R8 removed from the dex view; dex `class_defs_size` is 3627).
- member lines under those classes (awk over the class blocks; a method line contains `(`; one method can span several mapping lines, so these are upper bounds on retained methods): com.fsck.k9: 2892 method lines, 407 field lines; mime4j: 3111 / 499; commons.io: 90 / 33; jzlib: 602 / 117; beetstra: 161 / 35.
- Baseline-changes-no-tracked-file proof: `git status --porcelain` after the run =
```
 M sdlc/.skill-delegation-audit.log
?? sdlc/.skill-context.json
?? sdlc/artifacts/build/feasibility/
?? sdlc/artifacts/build/sprints/sprint-019/
```
  equals the I0.1 output plus the new collapsed `feasibility/` directory (the input copy); `git diff --stat c8f04558dff63ab24ed31b3e3e19ee99c5cb8afc -- app k9mail-vendored metadata gradle settings.gradle build.gradle gradle.properties gradlew gradlew.bat` is empty (0 bytes).
- Environment notes: a shell layer here halves doubled backslashes, so the SDK path was derived by a scratch helper (`sdk.py`, in scratch) instead of the plan's `sed` line; `<sdk>` is never written to a file. The first attempt of the plan's sed form failed (attempt 1 of the helper, not of the dex tool).

status: done
NEXT: I0.5 create the analysis skeleton with the Baseline section

### I0.5 Skeleton

Created `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md` with the frontmatter (pins `PENDING`, start commit filled), the header line, the filled Baseline section (values with `file:line`, "682, not 697" statement, gate run, I0 porcelain and untracked records, input hashes and line counts, redactions: none), sections 1-12 as `PENDING`, an empty Citations table with the 6 columns, the Experiment log (entry (e) filled; (a)-(d), (f) and the command entry `PENDING`), and the Product owner decision pending line.
Heading check `grep -nE '^## '`: Baseline, 1..12 in order, Citations, Experiment log, Product owner decision; no level-3 heading starts with a digit (count 0). Citation IDs used: 0; rows: 0 (nothing to diff).
status: done

## I0 END

End checks:
1. Banner: line 1 of the copy equals `UNREVIEWED INPUT - not a citation source`; `tail -n +2` hash equals the source hash `f2b9610b...aece9`. PASS.
2. Gates: exit 0, tests=682 skipped=2 failures=0 errors=0; `@Test` git-grep = 682. PASS.
3. Porcelain: unchanged from I0.1 except the new collapsed `?? sdlc/artifacts/build/feasibility/` (analysis + input copy); the log lives under the already-untracked sprint-019 folder. `git diff --stat <start> -- <product paths>` empty. PASS.
4. IC-3 full procedure over the analysis, the input copy and this log: control `paths=4 email=1 host_user=2`, guard did not fire, 0 matches in all three greps. PASS.

Files touched by this increment (all new, none tracked-file edits): the analysis, the input copy, this log. Scratch holds Gradle log, dex, mapping copy, IC-3 pattern files and helper scripts.
Deviations from plan: none of substance. Notes: the plan's `sed` for the SDK path failed under this shell (backslash handling), so a scratch helper was used; the first IC-3 control run gave 3 instead of 4 for the same reason and was fixed and re-run; the SNI test `@Test` annotation lines are 47..230 (the plan's 48..231 are the method lines).
Carry-forward notes applied from the gates (R1-R6, O-A..O-E, expert-depth) are not yet exercised; they apply from I1 onward.

NEXT: I1 — I1.1 scratch guard, partial clone of thunderbird-android at the pin (credential-free, HTTPS), D1 ancestor check, main head and drift list

## I1 START 2026-11-12 (UTC)
Inputs: log tail (NEXT from I0), plan sections I1, Citation Mechanics, Q1-Q3, coordinator turn message (run I1a first; I1b if time allows). Research input used as a search hint only.

### I1.1 Clone (D2-a) and D1
Commands (scratch guard printed no ABORT):
```
env -u GH_TOKEN -u GITHUB_TOKEN git -c credential.helper= -c core.longpaths=true clone --filter=blob:none --no-checkout https://github.com/thunderbird/thunderbird-android.git <scratch>/tb   # exit 0, 12 s
git -C <scratch>/tb config credential.helper ""
git -C <scratch>/tb checkout --detach ac1f0e85f63c1b600ac443d1fb2d939bffff7de5                # exit 0
git -C <scratch>/tb merge-base --is-ancestor $PIN origin/main                               # ANCESTOR-YES
git -C <scratch>/tb log --format='%h %ad %s' --date=short $PIN..origin/main -- <in-scope paths>  # empty
(same for thunderbird-mobile-components at 7aa2590ff4ff915efb721a9b1a69fa84ce8fac3f, clone in <scratch>/tmc)
```
Results: clone date 2026-11-12. `rev-parse HEAD` = pin. Pin is an ancestor of `origin/main` and equals the `origin/main` head (`ac1f0e85f6`, 2026-11-12, "chore(deps): bump androidGradlePlugin from 9.5.0 to 9.5.1 (#11883)"; committer date 2026-11-12T14:41:40+01:00 = 13:41:40Z, agreeing with the input). 0 commits in `PIN..origin/main`; drift list over the in-scope paths: none. Components repo: pin `7aa2590ff4ff915efb721a9b1a69fa84ce8fac3f` (2026-11-11, "chore(deps): bump com.lemonappdev:konsist from 0.17.3 to 0.18.1 (#188)") is an ancestor of and equal to `origin/main`; 0 newer commits. Decision D1: pins kept (research pins).
status: done
NEXT: I1.2 fork position (fetch jberkel/k-9 branch, ahead/behind, merge base, fork-only commits)

### I1.2 Fork position (AC-3)
Commands:
```
env -u GH_TOKEN -u GITHUB_TOKEN git -c credential.helper= -C <scratch>/tb fetch --filter=blob:none https://github.com/jberkel/k-9.git sms-backup-plus-1.5.11   # exit 0
git -C <scratch>/tb rev-list --left-right --count $PIN...eaf689025ec28d55e3b62f5ea3b1f185f410ee6b
git -C <scratch>/tb merge-base $PIN eaf68902...; git log --format='%h %ad %s' --date=short <merge-base>..eaf68902...
```
Results: `cat-file -t eaf689025e...` = commit (exists). Left/right = **15032 behind / 11 ahead** (left = commits only at pin, right = only on fork). Merge base `23bf474d9389fa658b7395cf2637c0ec88d45709` (2016-10-20, "Merge pull request #1735 from jberkel/use-properties-for-config"). The pin is the `main` head, so the counts equal the input's "11 ahead / 15,032 behind" with no difference to explain. Fork commit `eaf689025e` is dated 2018-03-13. Fork-only commits (11): eaf689025e Fix potential ArrayIndexOutOfBoundsException; 7e6d627a7a No longer needed, publishing via jitpack.io now; 69eef8239b Retry socket connections; 013948a758 Increae read timeout; 605b321574 Fail early if username/password not set; 75bb293984 Publish to bintray; ccff8da295 Make classes public / protected; 30985d7ff0 Just fetch the date header; c16449248c Set internal date on APPEND; ac837a0156 SDK 7 compat fixes; 737c582cc1 Add XOAuth2. GitHub compare page still to be opened for the citation.
status: done
NEXT: Q1 registry probes with controls (curl, generic client, HTTPS), then write Section 1

### RESUMED 2026-11-13 after session interruption
Found on disk: log ends at I1.2 `status: done` with `NEXT: Q1 registry probes...`; the analysis file has no Section 1 text (`## 1. Consumability` is still `PENDING`; no citation rows; pins still `PENDING`). Scratch holds the clones `tb/` and `tmc/` (`rev-parse HEAD` equals the pins `ac1f0e85...` and `7aa2590f...`, confirmed today) and unlogged Q1 probe outputs from the interrupted turn (`google-master-index.xml`, `jp-*.json`, `ghp-*`, `page-*.html`, `compare.html`). The tracked tree is unchanged (porcelain as at I0 end). Decision: the clones are reused (I1.1/I1.2 results keep their 2026-11-12 date); every Q1 probe is re-run today with its command and HTTP status, outputs now go to `<scratch>/q1/`, and anything fetched today is dated 2026-11-13. The earlier outputs are used only as hints.
status: done
NEXT: Q1 probe 1 of 5: Maven Central (with control), logged before the next registry

### I1.3a Q1 registry 1 of 5: Maven Central (re-run 2026-11-13; curl -sS -L, status via `-w '%{http_code}'`, generic client)
Base `https://repo1.maven.org/maven2/`. Probes: `com/fsck/` 404; `com/fsck/k9/` 404; `app/k9mail/` 404; `net/thunderbird/` 200 (children: `components/` only); `net/thunderbird/mail/` 404; `net/thunderbird/android/` 404. Control `org/apache/james/apache-mime4j-core/` 200 (directory listing works, so 404 means absence).
Found: group `net.thunderbird.components` (children `bom/`, `core/`). `core/` holds `logging/` (children `core`, `core-android`, `core-jvm`, ... `file*`, `testing*`) , `outcome*` (android, iosarm64, iossimulatorarm64, jvm, wasm-js) and `testing*`. `maven-metadata.xml`: release/version 0.1.0 only (bom, outcome, logging/core, testing), lastUpdated 2026-10-12. `.asc` present (200) for bom 0.1.0 pom, outcome 0.1.0 module, outcome-jvm 0.1.0 jar, logging/core-jvm 0.1.0 jar, testing 0.1.0 module. None is a mail module.
status: done
NEXT: Q1 registry 2 of 5: Google Maven (master-index.xml with androidx.annotation control, group paths), then log

### I1.3b Q1 registry 2 of 5: Google Maven (2026-11-13)
`https://dl.google.com/dl/android/maven2/master-index.xml` 200 (10422 bytes, 341 group elements). Case-insensitive search for `thunderbird|k9|fsck`: 0 matches. Control in the same index: line 14 `<androidx.annotation/>` present. Group paths: `net/thunderbird/` 404, `com/fsck/k9/` 404, `app/k9mail/` 404. Control file `androidx/annotation/annotation/maven-metadata.xml` 200. Result: nothing found.
status: done

### I1.3c Q1 registry 3 of 5: Mozilla Maven (2026-11-13)
Base `https://maven.mozilla.org/maven2/`. Directory probes `net/thunderbird/`, `org/mozilla/thunderbird/`, `com/fsck/`, `com/fsck/k9/`, `app/k9mail/` all 404. Control: the directory `org/mozilla/components/` is ALSO 404, so this registry does not list directories and a directory 404 proves nothing. Metadata-file probes (which do work: controls `org/mozilla/geckoview/geckoview/maven-metadata.xml` 200 and `org/mozilla/components/browser-engine-gecko/maven-metadata.xml` 200): `net/thunderbird/components/bom/maven-metadata.xml` 404, `net/thunderbird/mail/mail-common/maven-metadata.xml` 404, `com/fsck/k9/mail/maven-metadata.xml` 404. Result: nothing found at the candidate coordinates; absence is shown only for those candidate coordinates (limit stated in the analysis).
status: done
NEXT: Q1 registry 4 of 5: JitPack builds API for the three owners (no artifact URLs), then log

### I1.3d Q1 registry 4 of 5: JitPack builds API (2026-11-13; no artifact URLs requested)
`https://jitpack.io/api/builds/com.github.thunderbird/thunderbird-android` 200: 7 recorded builds, 0 "ok" (6.901, 6.902, 6.903, 6.904, `main-321a3be2c9-1` and two bare SHAs, all "Error"). `.../com.github.k9mail/k-9` 200: 27 builds, "ok" only for tags 5.006, 5.110, 5.500, 5.503, 5.600; 22 others (5.7xx, 5.9xx ...) "Error". `.../com.github.jberkel/k-9` 200: 19 builds, 11 "ok" including `eaf689025e`, `eaf689025` and `sms-backup-plus-1.5.11-3.702-geaf6890-4497`; the `sms-backup-plus-1.5.11-eaf689025e-1` build is "Error". Control `.../com.github.jitpack/gradle-simple` 200 with 95 "ok" entries (API works). Result: no thunderbird-android build succeeded. Observation to reconcile in Section 9: the builds API lists the vendored commit as an "ok" build record, while `k9mail-vendored/NOTICE:25-27` records an HTTP 404 for the artifact on 2026-06-03. A build record is not the artifact; JitPack builds on demand and evicts, so the record does not show the artifact is retrievable (not tested: artifact URLs trigger third-party builds).
status: done
NEXT: Q1 registry 5 of 5: GitHub Packages page and API, then log

### I1.3e Q1 registry 5 of 5: GitHub Packages (2026-11-13)
`https://github.com/orgs/thunderbird/packages?ecosystem=maven` 200, page heading "0 packages". Controls: `https://github.com/orgs/thunderbird/packages` (all ecosystems) 200 with "23 packages"; `https://github.com/orgs/github/packages?ecosystem=maven` 200 with "7 packages" (the ecosystem filter works). `https://api.github.com/orgs/thunderbird/packages?package_type=maven` (no token) 401 "Requires authentication" (the input recorded 403 with a token lacking `read:packages`; the status differs because no credential is sent here, same conclusion: the API cannot be used). Result: no public Maven package. Limit: private packages are not visible.
status: done
NEXT: provenance pages and repository API re-fetch (3 announcement pages, repos API redirect, compare API/page), then write Section 1
