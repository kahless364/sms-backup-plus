---
artifact_type: plan
story_id: "U-071"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-21T14:30:00Z"
iteration: 1
base_commit: "7c5e6073ebd4ddd2d9e737ded20e7c134b2fb97f"   # planning HEAD (PLAN_HEAD): U-068 S1-I6; the start commit is read at I0, after U-068, U-069, U-070 and U-074 are done
contracts_verified:
  - CNTR-MODERNIZATION-001
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-005
risks_identified: 18
---

# Implementation Plan: U-071 — Build Toolchain and Test Tooling to the Latest Stable Release, One Component per Verified Increment

Story: `sdlc/artifacts/build/sprints/sprint-025/U-071-build-toolchain-and-test-tooling-latest-stable.md` (story S2 of
DES-MODERNIZATION-014). Model for structure, tools and conventions: `sdlc/artifacts/build/sprints/sprint-024/U-068/plan.md`
(iteration 2) and its plan review; toolchain method and traps: `sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md`
(Steps 5 to 8), its implementation log and `sprint-017/retro.md`; mutation protocol: `sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md`
section I6 and `.../U-067/evidence/mutation-log.md`.

**Planning state.** Planned at HEAD `7c5e6073` (U-068 S1-I6) on `sdlc/modernization-plan`, with U-068's developer still
running (I7, documents only, is left). No Gradle was run, no build, test or device step was taken, nothing was fetched.
Every build file was read from the committed blob (`git show`), never from the working tree. Between `c8e98a7f` (U-068
S1-I4) and HEAD no build file changed: the blobs of `build.gradle`, `app/build.gradle`, the wrapper files, `gradlew`,
`gradlew.bat`, the daemon criteria, `ci.yml`, `verification-metadata.xml`, `gradle.properties`, `settings.gradle`,
`k9mail-vendored/build.gradle` and `code-location.md` are identical at both (L1). **What still lands before `<start>`:**
U-068 I7 (`CHANGES`, `DEVELOPMENT.md`, `code-location.md:28`, the bug-report template; no build file), U-069 (two lines of
`app/build.gradle`, Rule 15, `:579` and `:586`, plus its worker and test), U-070 and U-074 (service, preferences and mail
sources and tests; no build file). I0 re-reads this start state with decision rows (I0 step 1, rows D2 to D7).

**The start commit is not fixed by this plan.** `<start>` is `HEAD` when I0 runs, after U-074 (sprint 025 wave 1) is done.
The start count `C0`, every coverage figure, the KSP list, the lint figures and the verification counts are measured
there; no step depends on a fixed count. For orientation only: the authoritative count is 756 at PLAN_HEAD.

**Size.** Tracked files changed: `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`,
`gradlew`, `gradlew.bat`, `build.gradle`, `app/build.gradle`, `gradle/verification-metadata.xml`, `.github/workflows/ci.yml`,
`sdlc/artifacts/engagement/code-location.md`, plus this story's log and evidence. Production code: **none expected**
(four pre-specified `@param:` edits only if Risk 9 fires, row D58). Tests: **none added, none changed** (one throwaway
probe is copied in and deleted twice, never committed, 4.P). **63 Gradle runs planned** (I0 5, I1 5, I2 3, I3 4, I4 5,
I5 11, I6 4, I7 5, I8 to I11 3 each, I12 4, I13 2, I14 3), plus up to about 12 conditional ones (VMP at I1 under
row D25; the re-runs after a forced edit under D55, D56, D58 or the flags of D69; one retry each under D20 and D73). **14 local commits**, one per increment I1 to I14 (13 if the KSP reorder fires), plus fix commits only if a
later check fails because of an earlier, committed increment. Six developer sessions (section 7).

## 0. Binding Inputs

| Source | What binds this plan |
|--------|----------------------|
| Story, "Orchestrator rulings before planning" (Record 17) | Runs after U-069 and U-070 (SC-8, SC-9); one local commit per increment, explicit paths, the plan's subjects, no push, no amend, fixes are further commits; the orchestrator supplies a refreshed research file and nobody fetches pages; Scope Concerns 3, 4, 5, 7 to 10 acknowledged as written (a forced test edit stops the story for an SA amendment); IC-5, IC-6 recorded "not run: needs the product owner's decision" and judged "not applicable until decided"; **a fallback that fires stops the developer at that increment, who reports** |
| Delegation of 2026-11-21 (orchestrator) | The planning date is 2026-11-21 and `sdlc/artifacts/build/research/2026-11-21-latest-stable-versions.md` is the supplied research (ruling 30); missing verbatim quotes and checksums go to the orchestrator as `needs_check` (2.3); the U-069 test `ic1_…` is never edited silently; `.claude/CLAUDE.md` only through the Engine Librarian; Record 18: no external `diff` or `cmp`, modest shell use, stop on a vanished tool or an unexplained kill |
| Story, Bound decisions | One component per increment in the design's order with the two pre-specified moves; D1 targets re-resolved by D1a; no triple is vendor-supported (A17 contradicted); warnings are debt, triggers are KGP/KSP/Hilt-plugin/AGP errors; F1 to F5 only; Build Tools 36.0.0; Truth in, JUnit unchanged; verification stays on, D5 per dependency increment; no floor lowered, no `@Ignore`, no loosened test; `-Werror` never removed; Robolectric default `sdk=29` and its comment unchanged; lint recorded, not gated |
| DES-MODERNIZATION-014 | S2 table (`:554-575`); D1 rows (`:364-383`) and D1a (`:435-460`); D2 N-6 (`:590-596`); D3 (`:701-793`: reading, G1 to G6, evidence that decides, triggers, javac rule, F1 to F5, who decides); D5 (`:882-936`); D6 (`:938-979`); D7 (`:981-1015`); D8 (`:1017-1066`); D9 (`:1068-1084`); D10 F-1 rule (`:1104-1138`); D16 (`:1543-1604`); D17 (`:1606-1648`); D18 (`:1650-1673`); D19 (`:1675-1692`); A13 to A22 (`:247-256`); INT-001 to INT-006 (`:1734-1766`); Risks 3, 5 to 10, 12, 13 (`:2035-2045`); PO-2 to PO-4 (`:2067-2093`) |
| REQ-MODERNIZATION-015 | AC-1 to AC-4, AC-11 to AC-18; AC-13 as amended by CR-016 Amendment 1 (start = HEAD before the first change, end = the last commit or the working tree with `--untracked`, foreign tests listed and not counted, no fixed baseline) |
| Story AC-1 to AC-24, IC-1 to IC-6 | As written; where this plan reads a criterion in one of two ways it says so in 9.2 (SC-P1 to SC-P5) and takes the stricter reading by default |

## 1. Assumption Ledger

`verified` = read or run read-only in this planning session, with its location. `needs_check` = a developer or
orchestrator step with a decision row for each outcome. `plausible, not verified` = believed from general knowledge or an
unread source; no step relies on it without a check. Line numbers are at PLAN_HEAD; U-069 edits `app/build.gradle:579`
and `:586` in place, so no line this plan names moves (L3).

| # | Claim | Status | Evidence |
|---|-------|--------|----------|
| L1 | HEAD `7c5e6073` (U-068 S1-I6). Blobs at `c8e98a7f` and HEAD are equal for: `build.gradle` `ee06fa7f`, `app/build.gradle` `f626cfab`, `gradle-wrapper.properties` `bd82f36b`, `gradle-wrapper.jar` `b1b8ef56`, `gradlew` `63901d88` (mode 100755), `gradlew.bat` `24c62d56`, `gradle-daemon-jvm.properties` `5a334ba9`, `ci.yml` `0f4f011a`, `verification-metadata.xml` `05490567`, `gradle.properties` `827a333c`, `settings.gradle` `b2723041`, `k9mail-vendored/build.gradle` `6eaf1978`, `code-location.md` `61b41765`, `.claude/CLAUDE.md` `e219559f`, `robolectric.properties` `ccf01406`. `git diff --stat c8e98a7f HEAD` on the build paths lists only `app/lint-baseline.xml` (I6) and `app/src/main`/`app/src/test` (I5) | verified | `git rev-parse c8e98a7f:<p> HEAD:<p>`, `git ls-files -s`, `git diff --stat` |
| L2 | Line endings (`core.autocrlf true`, no `.gitattributes`): `app/build.gradle`, `gradle-wrapper.properties`, `gradlew`, `code-location.md`, `gradle-daemon-jvm.properties` are LF in index and working tree; `build.gradle`, `verification-metadata.xml`, `gradlew.bat` and the two worker files are CRLF in the working tree; `ci.yml` is **mixed** in the working tree (LF in the index); the jar is `-text`. `git hash-object` normalises, so a working-tree hash compares with an index blob. The Edit tool keeps a file's line endings | verified | `git ls-files --eol`; `git config core.autocrlf` |
| L3 | `build.gradle`: AGP `:10`, KGP `:14`, KSP `:17`, Hilt plugin `:21`, comments `:7-9`, `:12-13`, `:15-16`, `:18-20`, `reports.junitXml.required = true` `:30`, `updateDaemonJvm { toolchainPlatforms = [] }` `:53`. `app/build.gradle` (594 lines): `compileSdkVersion 35` `:21`, `minSdkVersion 24` `:24`, `targetSdkVersion 35` `:25`, `versionCode 1602` `:26`, `testOptions.unitTests { includeAndroidResources = true }` `:94-101`, stdlib `:117`, JUnit `:159`, Robolectric `:160` (with a trailing comment), Truth `:161` (double quotes), Mockito `:163` (trailing comment), auto-service `:166`, GreenMail `:173`, Hilt comment `:176`, Hilt and androidx.hilt `:182-187`, `-Werror -Xlint:unchecked` `:203`, JaCoCo `:211-213`, class roots `:217-221`, guard `:222-229`, 18 rules `:340-589`, Rule 15 `:575-588` (note `:579`, floor `:586`), the Robolectric comment `:592-594` | verified | both files read in full from `c8e98a7f` |
| L4 | `gradlew` (260 lines, LF, 100755): `case "$( uname )"` block `:110-115`, line `:116` blank, LOCAL ADDITION block `:117-127` (exact text in 3.1), `:128-130` blank, `:131` `# Determine the Java command…`. SHA-256 of `sed -n 117,127p` = `45364f3dcdb5a4a12f728c90e42c3a4f28dd0c4511527c72746feee5880ea760`; 2 lines contain `LOCAL ADDITION` | verified | `git show HEAD:gradlew` |
| L5 | The tracked wrapper jar's SHA-256 is `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`, the official 9.5.1 jar value U-060 recorded. So the `wrapper` task writes the **running** Gradle's jar: one run on 9.5.1 updates the properties but leaves a 9.5.1 jar, and only a second run on 9.9.2 writes 9.9.2's jar and scripts (procedure 3.1) | verified (hash); the Gradle-docs sentence is P1 | `git show HEAD:gradle/wrapper/gradle-wrapper.jar \| sha256sum`; `sprint-017/U-060/implementation-log.md:45` |
| L6 | `gradle-wrapper.properties`: `:3` `distributionSha256Sum=bafc141b…b031f`, `:4` `…gradle-9.5.1-bin.zip`, then `networkTimeout`, `retries=0`, `retryBackOffMs=500`, `validateDistributionUrl=true`, `zipStoreBase`, `zipStorePath` (10 lines) | verified | file read |
| L7 | `ci.yml` (57 lines): triggers `:3-9`, `ubuntu-22.04` `:13`, `checkout@v4` `:17`, comment `:19-20`, `setup-java@v4` `:22`, comment `:27`, `sdkmanager "build-tools;36.0.0"` `:28-30`, `cache@v4` `:33` and `:41`, commands `:49` and `:56` | verified | file read |
| L8 | `verification-metadata.xml` at HEAD: schema `dependency-verification-1.3.xsd`, namespace `https://schema.gradle.org/dependency-verification`; configuration `:3-10` (`verify-metadata` true, `verify-signatures` false, two `<trust file=… regex="true"/>`); `grep -c '<component '` 803, `'<artifact '` 1460, `'<trust'` **3** (the two trust lines and `<trusted-artifacts>`), 5999 lines; no `android-all` entry; no `<pgp`/`also-trust`/`ignored-keys` | verified | `git show HEAD:… \| grep -c` |
| L9 | The AC-16 (c) sweep at HEAD lists exactly the story's set: `ci.yml:17,22,33,41`; `CHANGES:141`; `app/build.gradle:2,5,6,116,117,160,161,163,173,176,182,183,186,187,212`; `DefaultTrustedSocketFactoryJdkProviderTest.java:60`; `build.gradle:7,10,12,13,14,16,17,21`; `gradle.properties:15`; `gradle-wrapper.properties:4`; `metadata/f-droid/com.zegoggles.smssync.txt:32,33,34` | verified | the AC-16 `git grep` run read-only on HEAD |
| L10 | AC-22 (b) at HEAD: `app/build.gradle:136` (`core-role:1.0.0-beta01`, S4's) and comments `:138`, `:140`, `:144`; no `SNAPSHOT` in the four build files | verified | `git grep` on HEAD |
| L11 | **Simulation of section 3** on `git show` copies of the start-state files: every `old_string` occurs exactly once at its increment (`cache@v4` twice, by design); `git diff --no-index --numstat` per increment is `build.gradle 1 1` (I2, I3, I4, I5), `app/build.gradle 4 4` (I4), `1 1` (I5), `2 2` (I6), `4 4` (I7), `1 1` (I8 to I12), `ci.yml 8 7` (I13, hunks `@@ -17 +17 @@`, `@@ -19,2 +19,3 @@`, `@@ -22 +23 @@`, `@@ -27 +28 @@`, `@@ -33 +34 @@`, `@@ -41 +42 @@`), I14 `build.gradle 9 9`, `app/build.gradle 8 8`, `code-location.md 3 3`; after I14 the AC-16 sweep finds **no** hit in the three build files and the AC-22 (b) grep only the four lines of L10; the YAML parse of the I13 file prints the values in I13 step 5; the 34 changed lines of I14 in the two build files all contain `//`. `oldtext_check.py` (4.0) run on a `git archive HEAD` copy prints `ALL OK`; `gradlew_block.py` run on a synthetic generated copy (the start `gradlew` without lines 116-127) prints all `OK` and the block SHA of L4, and `git diff --no-index --numstat` of that copy against the start file is `12	0`; the embedded tool texts of 4.0 are byte-equal to the tested copies | verified (simulation) | planner scripts in the session scratch folder |
| L12 | Local Gradle cache (informative): present KGP 2.4.21, Hilt plugin and `hilt-android` 2.60.1, Mockito 5.24.0, ByteBuddy 1.17.7, auto-service 1.1.1, `androidx.test:monitor` 1.8.0, AGP 9.5.1; absent AGP 9.5.2, KSP 2.3.14 (only the defective 2.3.13), androidx.hilt 1.4.0, Robolectric 4.18.1, Truth 1.4.5, GreenMail 2.1.15, JaCoCo 0.8.16; wrapper distributions 9.9.1 present, 9.9.2 absent. So I1, I2, I3, I6, I7, I9, I10, I12 download through the build's own path | verified | listing of the cache folders (no path written here) |
| L13 | Cached `monitor-1.8.0.aar` has **no** `aar-metadata.properties` (so states no `minCompileSdk`) and manifest `minSdkVersion="21"`; cached `mockito-core-5.24.0.pom` depends on `byte-buddy` and `byte-buddy-agent` 1.17.7; `hilt-android-2.60.1.pom` on `kotlin-stdlib` 2.3.21 and `androidx.annotation:annotation` 1.7.1; `hilt-work-1.2.0.module` runtime deps `annotation:1.1.0`, `hilt-common:1.2.0`, `work-runtime:2.3.4`, `hilt-android:2.49` | verified (cached files; I7 re-reads the resolved artifact by checksum) | `unzip -p`, `grep` of the cached POM/module files |
| L14 | U-068 measured: `C0`(U-068) 753, after I5 756; `receiver` 39/58 (spare 10), `compat` 18/71 (spare 3), `scheduler` 137/185; `activity` covered **alternates 199 / 189 between runs** (test-scheduling dependent); lint at I6 "33 errors (and 18 errors filtered by baseline)", baseline 26 entries; APKs `so=0`, debug 23 dex, release 1; 48 kept names, nine reflection-sensitive classes self-mapped; `extractNativeLibs` now `false` (declared); javap 61/61/52; the AC-21 invariant greps of 2.9 give 1/0/1/1/1/1 on its I5 files | verified (by reading evidence) | `sprint-024/U-068/implementation-log.md`; `…/evidence/I5-*` |
| L15 | The current `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml` (U-068 state): `cov_floors.py` reads **18 rules**; `compat` spare 3, `receiver` 10, `scheduler` 26, `worker` 0/16 | verified (tool run read-only) | 4.0 tools |
| L16 | U-067: check lines `BackupWorker.kt:332` (16 spaces) and `RestoreWorker.kt:311`, `:399` (20 spaces), each `currentCoroutineContext().ensureActive()`, with context lines `val item = msgs[currentRestoredItem]` (`:313`) and `val values = items[currentRestoredItem]` (`:401`); blobs `eb535f5c`, `ffa9c57e`; NEW3 = `BackupWorkerLoopCancellationTest`, `RestoreWorkerIntegrationTest`, `RestoreWorkerCheckpointTest` (31 cases at U-067); the six expected failing sets of `mutation-log.md:82-87` | verified | `git grep -A2`; U-067 plan `:298-305`, `:948-955`; mutation log |
| L17 | U-069 (plan iteration 2): Rule 15 becomes `minimum = 0.95 // MEASURED <P>% …` and its note changes (`:579`, `:586`); `worker` holds only `BackupTriggerWorker` (16 lines today) plus generated classes JaCoCo filters (`@DaggerGenerated`); test `ic1_hiltWiring_assistedFactoryIsBoundUnderThePersistedClassName` reads `<Name>_AssistedFactory`, `<Name>_HiltModule` and `bind`; its AC-9 tests use `ShadowLog`. **Derived:** with 16 lines and a 0.95 floor, `⌈0.95·16⌉ = 16`, so the package has **zero spare lines**: one line more counted, or one line less covered, fails the gate (a floor of 0.95 tolerates a lost line only from 20 lines up) | verified (plan read; arithmetic) | U-069 plan `:948-966`, `:785-811`, `:1511-1516` |
| L18 | U-074 adds tests to `RestoreWorkerIntegrationTest` and `RestoreWorkerCheckpointTest` (R1 to R8, C1, C2) and leaves U-067's check lines alone ("The new call sits inside the catches, not on the check lines"); U-070 edits `service`, `preferences`, `mail`; neither edits a build file | verified (stories and plan read) | U-074 story AC-2, AC-3, AC-11, Affected Code; U-070 plan `:86-87`, `:3986` |
| L19 | Risk 9 sites (Kotlin "annotation is currently applied to the value parameter only"): `MainViewModel.kt:55`, `WorkManagerScheduler.kt:80` (`:81` before U-068 I5), `SharedPreferencesCheckpointStore.kt:49`, `WorkerEngineFactory.kt:61`, each `    @ApplicationContext private val context: Context` (once per file). U-067 I7 recorded 6 `w:` lines, 4 of them these | verified | `git grep` on HEAD; `sprint-023/U-067/evidence/I7-kotlinc.txt` |
| L20 | TLS: `DefaultTrustedSocketFactoryJdkProviderTest` has `@ConscryptMode(OFF)` `:140`, `@LooperMode(LEGACY)` `:141`, the `@Before checkIsolationGuards()` whose failure messages start `A1-G3`, `A1-G1`, `A1-G3 (via factory static init)`; the exact-list tests are in `DefaultTrustedSocketFactoryHardeningTest` (`containsExactlyElementsIn(ALLOWED_CIPHERS_…)`) and `…JdkProviderTest` (`…areExactlyTheAllowedList…`). No test uses `TextLayoutMode`, `shadows-httpclient` or `org.apache.http` (Robolectric 4.18 removes them); `ShadowLog` is used by nothing at HEAD (U-069 adds it) | verified | `git grep` on HEAD |
| L21 | Tools on PATH: `python` 3.13.17 with PyYAML 6.0.2 under `-I`; `unzip`, `sha256sum`, `sha512sum`, `awk`, `sed`, `comm`; `diff` is gone (quarantined); `cmp` exists and is **not used**; `node` exists and is not used | verified | `python --version`, `python -I -c "import yaml"`, `command -v` |
| L22 | `org.gradle.java.home` occurs only in prose: `.claude/CLAUDE.md:31` and `DEVELOPMENT.md:25`, `:28`, `:30`, `:49` (the last says the project "pins JDK 17 via `org.gradle.java.home`", stale since U-060) | verified | `git grep` on HEAD |
| L23 | `gradle.properties`: `org.gradle.daemon=false` (`:21`): every Gradle call starts a single-use daemon, so no Gradle JVM outlives a run; `org.gradle.jvmargs` (`:20`) carries the Windows NIO flags | verified | file read |
| L24 | The research of 2026-11-21 carries every S2 version of the design's D1 table, so on the planning date (the same day) **no row moved** (2.1); it does not carry the three GitHub action versions (the design's D1 rows `:375-377`, read the same day, do), the Gradle 9.9.2 checksums, or several verbatim quotations AC-3 needs (2.3) | verified | research §1, §2.1, §2.2, §3.1, §6 |
| L25 | Reused read-only tools exist and are tracked: `sprint-020/U-063/evidence/tools/{sanitize.py,junit_totals.py,junit_cases.py,jacoco_pkgs.py}`, `sprint-024/U-068/evidence/tools/{sdk_dir.py,jacoco_inventory.py,jacoco_lines.py,lint_ids.py}`; this workspace's `compiled-pipeline.json` is excluded in `.git/info/exclude` | verified | `git ls-files`; `.git/info/exclude:45` |
| N1 | The Gradle 9.9.2 distribution and wrapper-jar SHA-256 values (Q-1, Q-2) | needs_check (orchestrator) | 2.3; rows D1, D22 |
| N2 | The 9.9.2 `gradlew` template keeps the `case "$( uname )"` block and the `cygwin`/`msys` variables the LOCAL ADDITION block tests | needs_check | I1 step 6 (`gradlew_block.py`); row D17 |
| N3 | `./gradlew wrapper` on 9.9.2 configures this build (the root `updateDaemonJvm { toolchainPlatforms = [] }` included) | needs_check | I1 step 5; row D21 |
| N4 | KSP 2.3.14 runs on KGP 2.3.21 (C9) | needs_check | I2; row D54 |
| N5 | A13: AGP 9.5.2 accepts `compileSdkVersion`/`minSdkVersion`/`targetSdkVersion` | needs_check | I3; row D56 |
| N6 | The JaCoCo class roots of `:217-221` exist after `clean` on AGP 9.5.2 | needs_check | I3; row D55 |
| N7 | `configuration.txt` is unchanged at I3 to I6 | needs_check (likely to differ, P2) | rows D43; SC-P1 |
| N8 | G4: the Hilt plugin 2.60.1 transform and `hiltJavaCompileDebug` on AGP 9.5.2 / Gradle 9.9.2 | needs_check | I4; row D59 |
| N9 | JaCoCo still reports no lines for the Hilt/androidx.hilt generated classes in `worker` (L17) after I4, I6, I12 | needs_check | rows D38, D60 |
| N10 | Risk 9: Kotlin 2.4's annotation-target default does not turn the four sites into a Hilt or KSP error | needs_check | I5; row D58 |
| N11 | F-1 rule and the six mutations hold on Kotlin 2.4.21 | needs_check | I5; rows D61 to D64 |
| N12 | androidx.hilt 1.4.0 keeps the generated names `ic1_…` reads | needs_check | I6; row D66 |
| N13 | Hilt 2.60.1 and androidx.hilt 1.4.0 change no resolved version of an S4-owned runtime library (`androidx.annotation:annotation` resolved 1.8.1 at U-060, above both POMs' 1.7.1 and 1.1.0 for the parts read) | needs_check | I4, I6 step DEP; row D31 |
| N14 | A14, A15 (Robolectric 4.18.1 offers the two modes; no `--add-opens` needed at SDK 24 and 29 on JDK 25) | needs_check | I7; rows D68 to D70 |
| N15 | A21 on the artifact Gradle resolves at I7 | needs_check | I7 step 6; row D74 |
| N16 | The Conscrypt that Robolectric 4.18.1 brings leaves U-066's exact-list results unchanged | needs_check | I7; row D72 |
| N17 | The `android-all` artifacts Robolectric 4.18.1 resolves (names, versions) | needs_check | I7 step 7; row D73 |
| N18 | ByteBuddy 1.17.7 Java 25 support statement (Q-9) | needs_check (orchestrator) | I8; row D76 |
| N19 | JaCoCo 0.8.16 class inventory equals 0.8.14's | needs_check | I12; row D79 |
| N20 | A18 `ubuntu-22.04` still offered (Q-10) | needs_check (orchestrator) | I13; row D83 |
| N21 | `lintVitalRelease` (inside `assembleRelease`) on AGP 9.5.2 finds no new fatal issue | needs_check | I3; row D51 |
| N22 | Gradle 9.9.2's writer output parses as the same schema and the U-060 proof can compare it | needs_check | I2 onward; row D28 |
| P1 | Gradle's user guide says the `wrapper` task must run a second time to update the jar (Q-12) | plausible, not verified | not relied on: L5 and the jar-checksum check (D22) decide |
| P2 | AGP 9.5's `proguard-android-optimize.txt` and the bumped libraries' consumer rules differ textually from today's, so `configuration.txt` differs at I3 to I6 | plausible, not verified | N7; SC-P1 |
| P3 | Kotlin 2.4 changes the default use-site target of a constructor-parameter annotation (parameter plus property) | plausible, not verified | N10 |
| P4 | Robolectric 4.18 resolves new `android-all` artifact names at test time | plausible, not verified | N17 |
| P5 | androidx.hilt 1.4.0's POM adds no runtime dependency above today's resolved versions beyond research C14's two | plausible, not verified | N13 |
| P6 | JaCoCo compares `COVEREDRATIO` after rounding to the minimum's decimals | plausible, not verified | `cov_floors.py` uses the exact (conservative) ratio |

## 2. Design (decided)

### 2.0 Principles applied

Book chapters are cited as commonly given; I did not re-read them in this session.

- **One change per increment, each integrated and green** (Humble and Farley, *Continuous Delivery*, ch. 1 "deliver
  small changes" and ch. 3 on continuous integration). Every increment changes one component and is committed only with
  all four gates green, so `git bisect` over the story lands on one version bump and `git revert` newest-first is the
  rollback (D19). The known weakness of one-factor-at-a-time is that it hides interactions; the design's order handles
  the one interaction that matters (Kotlin 2.4 needs Hilt 2.60, not the reverse: I4 before I5).
- **Fitness functions as gates** (Ford, Parsons and Kua, *Building Evolutionary Architectures*, ch. 2). The four gates,
  the per-package floors, the manifest invariants, the R8 kept-name set, the `.so` count, the class-file majors, the KSP
  file list and the verification-metadata additions-only proof are executable checks run at every increment, each
  against the I0 record and the previous increment (2.7 says what each one can and cannot show).
- **Characterisation tests are the oracle, never the variable** (Feathers, *Working Effectively with Legacy Code*,
  ch. 13). No test is edited, loosened or skipped; a toolchain move that changes a test's result is either a regression
  or an environment difference, and is investigated as such (U-066's exact-list tests above all; Scope Concern 4).
- **Mutation analysis re-run, not only the tests** (DeMillo, Lipton and Sayward, "Hints on Test Data Selection", IEEE
  Computer, 1978). The Kotlin raise can change how the three cancellation checks bind; U-067's six mutants show the
  tests still tell the fixed code from the broken one under Kotlin 2.4 (I5).
- **Write-ahead logging for every temporary edit** (Mohan et al., "ARIES", ACM TODS 1992: log before you change,
  so recovery knows what to undo). Each mutation and each probe copy-in has an `APPLYING` line before the edit and a
  `restored`/`removed` line after the hash proof; recovery reads the log (4.M, 4.P).
- **Supply-chain pinning with a monotone ratchet** (Gradle user guide, "Verifying dependencies"; the additions-only
  rule of DES-013 D4). The verification file only grows, by Gradle's own writer, and the U-060 semantic proof is the
  arbiter; the wrapper distribution is pinned by `distributionSha256Sum` and the jar by its published hash.
- **Evidence hierarchy for compatibility** (the U-060 SC-5 method, design D3 step 2). Primary sources bind (the
  chosen Gradle's compatibility page, the AGP release notes); secondary sources are checks (Kotlin table, KSP and Dagger
  notes, Robolectric table). Where they disagree the gap is named (G1 to G6) with its detector, and the build decides.

### 2.1 Latest stable re-resolved on the planning date (AC-2; D1a)

Planning date 2026-11-21 (the date of the supplied research, ruling 30). "File:line" is the start state as read at
PLAN_HEAD; I0 re-reads each line (`oldtext_check.py`). Source abbreviations: MC = Maven Central `maven-metadata.xml`,
GM = Google Maven `maven-metadata.xml` (research header). **Result: no row moved**, so D1a rules 2 and 3 are not
applied; rule 4 (no pre-release) and rule 5 (no defective release) are applied where named; rule 6 has no case.

| # | Component | At `<start>` | Newest stable, 2026-11-21 (published) | Source | Target | D1a | Increment |
|---|-----------|--------------|---------------------------------------|--------|--------|-----|-----------|
| 1 | Gradle wrapper | 9.5.1 `gradle/wrapper/gradle-wrapper.properties:4` | 9.9.2 (2026-11-19) | https://services.gradle.org/versions/current ; https://docs.gradle.org/9.9.2/release-notes.html | 9.9.2 | none moved | I1 |
| 2 | AGP | 9.0.1 `build.gradle:10` | 9.5.2 (2026-11-20) | GM `com/android/tools/build/gradle` ; https://developer.android.com/build/releases/agp-9-5-0-release-notes | 9.5.2 | rule 4: 9.6.0-alpha05 excluded | I3 |
| 3 | Kotlin Gradle plugin | 2.3.21 `build.gradle:14` | 2.4.21 (2026-10-08) | MC `org/jetbrains/kotlin/kotlin-gradle-plugin` ; https://kotlinlang.org/docs/releases.html | 2.4.21 | rule 4: 2.5.0-RC excluded | I5 |
| 4 | `kotlin-stdlib` | 2.3.21 `app/build.gradle:117` | 2.4.21 | MC `org/jetbrains/kotlin/kotlin-stdlib` | 2.4.21 (= plugin) | — | I5 |
| 5 | KSP plugin | 2.3.6 `build.gradle:17` | 2.3.14 (2026-11-13) | MC `com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin` ; https://github.com/google/ksp/releases/tag/2.3.14 | 2.3.14 | rule 5: 2.3.13 "Defect in release" excluded | I2 |
| 6 | Hilt Gradle plugin | 2.59.2 `build.gradle:21` | 2.60.1 (2026-07-06) | MC `com/google/dagger/hilt-android-gradle-plugin` ; https://github.com/google/dagger/releases/tag/dagger-2.60.1 | 2.60.1 | — | I4 |
| 7 | `hilt-android`, `hilt-compiler` (ksp, kspTest), `hilt-android-testing` | 2.59.2 `app/build.gradle:182`, `:183`, `:187`, `:186` | 2.60.1 | MC `com/google/dagger/…` | 2.60.1, all five identical | — | I4 |
| 8 | `androidx.hilt:hilt-work`, `hilt-compiler` | 1.2.0 `app/build.gradle:184`, `:185` | 1.4.0 (2026-07-01) | GM `androidx/hilt/hilt-work` ; https://developer.android.com/jetpack/androidx/releases/hilt | 1.4.0 | — | I6 |
| 9 | JaCoCo | 0.8.14 `app/build.gradle:212` | 0.8.16 (change history 2026-10-24, pom 2026-10-26) | MC `org/jacoco/org.jacoco.core` ; https://www.jacoco.org/jacoco/trunk/doc/changes.html | 0.8.16 | rule 4: 0.8.17 snapshot excluded | I12 |
| 10 | SDK Build Tools | none declared; CI `ci.yml:30` 36.0.0 | 37.0.0 newest package; AGP 9.5 minimum and default 36.0.0 | https://dl.google.com/android/repository/repository2-3.xml ; agp-9-5-0 notes | 36.0.0, unchanged (D1 `:373`: REQ-015 asks for "the SDK Build Tools version that the chosen AGP requires") | — | I13 (comments) |
| 11 | `actions/checkout` | v4 `ci.yml:17` | v7 (7.0.1) | https://github.com/actions/checkout/releases (design D1 `:375`, read 2026-11-21) | v7 | major tag kept | I13 |
| 12 | `actions/setup-java` | v4 `ci.yml:22` | v6 (6.3.0) | https://github.com/actions/setup-java/releases (D1 `:376`) | v6 | — | I13 |
| 13 | `actions/cache` | v4 `ci.yml:33`, `:41` | v6 (6.1.0) | https://github.com/actions/cache/releases (D1 `:377`) | v6 | — | I13 |
| 14 | `junit:junit` | 4.13.2 `app/build.gradle:159` | 4.13.2 (2021-02-13) | MC `junit/junit` | unchanged | — | none (recorded) |
| 15 | Robolectric | 4.15.1 `app/build.gradle:160` | 4.18.1 (2026-11-18) | MC `org/robolectric/robolectric` ; https://robolectric.org/compatibility_table/ | 4.18.1 | — | I7 |
| 16 | `mockito-core` | 5.18.0 `app/build.gradle:163` | 5.24.0 (2026-09-23) | MC `org/mockito/mockito-core` | 5.24.0 (ByteBuddy 1.17.7, L13) | — | I8 |
| 17 | Truth | 1.4.4 `app/build.gradle:161` | 1.4.5 (2025-09-10) | MC `com/google/truth/truth` | 1.4.5 | — | I9 |
| 18 | `greenmail-junit4` | 2.1.3 `app/build.gradle:173` | 2.1.15 (2026-10-25) | MC `com/icegreen/greenmail-junit4` | 2.1.15 | — | I10 |
| 19 | auto-service | 1.0 `app/build.gradle:166` | 1.1.1 (2023-06-12) | MC `com/google/auto/service/auto-service` | 1.1.1 | — | I11 |

If the orchestrator's supplement (2.3) reports a newer non-pre-release for any row, row D14 applies: a newer patch is
taken by substituting only the version token in that increment's edit and in the I14 texts that name it, the
component's constraint rows are re-checked, and the substitution is recorded; a newer minor or major stops before I1
for an SA amendment (D1a rule 3 needs a re-read of every constraint it takes part in).

### 2.2 Compatibility reading (AC-3, AC-4; design D3 steps 1 and 2)

The developer writes `$E/compatibility-record.md` at I0 step 11 from this text, the research's §3.1 and the
orchestrator's supplement, quoting verbatim and never paraphrasing. Its sections:

1. **Reading applied** (U-060 SC-5 method): primary and binding = the Gradle 9.9.2 compatibility page and the AGP 9.5
   release notes (minimum Gradle, minimum JDK, Build Tools, maximum API level); secondary checks = the Kotlin Gradle
   plugin table (`https://kotlinlang.org/docs/gradle-configure-project.html`), the KSP and Dagger release notes, the
   Robolectric compatibility table (`https://robolectric.org/compatibility_table/`).
2. **The research's "newest triple that sits inside every published range" (Gradle 9.7.0 + AGP 9.3.1 + KGP 2.4.21,
   research §3.2 point 2) is contradicted** (design A17, D3 Context): the Gradle 9.7.x page, as read for U-060, lists
   Kotlin only "through 2.4.20-Beta1" and AGP "9.0 through 9.4.0-alpha03". It is not used as evidence. **F2 is "inside
   Kotlin's range", never "inside every range"** (FD-VAL-004).
3. **Quotation table**: one row per C1 to C21 with the research's quoted text and URL, a column "verbatim in the
   research: yes / partly / values read from a table or artifact", and, for each "partly" or "values" row the plan
   relies on, the orchestrator's verbatim quotation from the supplement (Q-3 to Q-9). A citation that no longer says
   what the research says is recorded as such (row D13). The research rows the plan relies on: C1, C2 (Gradle); C3, C4,
   C5, C6, C20 (AGP); C7, C8 (Kotlin); C9, C12 (KSP); C10, C11, C13 (Dagger); C14 (androidx.hilt); C15, C16
   (Robolectric); C17 (JaCoCo).
4. **Residual gaps, verbatim from D3 (`:728-735`), with their detection and the planning-date re-read:**

   | Gap | What the sources say | Detection | Re-read on 2026-11-21 |
   |-----|----------------------|-----------|-----------------------|
   | G1 | The Gradle 9.9 page tests AGP "9.0 through 9.5.0-alpha10" (C2); AGP 9.5.2 is beyond that pre-release bound | Build plus `--warning-mode all`; any AGP-originated error | C2 (and Q-3 at the versioned URL) |
   | G2 | KGP 2.4.21 is fully supported only up to Gradle 9.7.0 and AGP 9.3.1 (C7) | KGP-originated warnings (debt) or errors (trigger) | C7 (and Q-5) |
   | G3 | KSP 2.3.14 is tested against AGP base 9.3.0-alpha01 (C9) | KSP tasks pass, and the KSP generated-file list equals the S2-I0 list (U-060: 49 files plus the four Hilt LazyMapKey additions) | C9 (and Q-6) |
   | G4 | The Hilt Gradle plugin 2.60.1 has no statement for AGP 9.5 or Gradle 9.9 (§3.2 point 4) | `hiltJavaCompileDebug`, the ASM transform, Hilt tests, R8 C-2 | research §3.2 point 4, §6.5 (and Q-7) |
   | G5 | AGP 9.5.2 has no release notes (§6.1) | Build; F1 | research §1, §6.1 (same day) |
   | G6 | Robolectric 4.18.1 on JDK 25 without flags is not re-tested (§6.7) | D7 | research §6.7 (and Q-8) |

   At I14 the record gains a column "symptom seen: none / <increment and evidence file>" per gap (AC-4, last sentence).
5. **Fallbacks** (2.4) and the statement "not triggered" with the `--warning-mode all` evidence if none fired (AC-5).

### 2.3 What the orchestrator must supply before the build (needs_check; nobody here fetches)

One file, `sdlc/artifacts/build/research/2026-11-21-u071-quote-supplement.md` (`$SUPP`), made from the vendors' own
pages, each item with the verbatim text and its URL. **Q-1 to Q-9 are needed before I0 step 3** (row D1); Q-10 before
I13 (row D83); Q-11 and Q-12 are optional.

| Id | What | URL | Needed by | Why the research does not suffice |
|----|------|-----|-----------|-----------------------------------|
| Q-1 | SHA-256 of `gradle-9.9.2-bin.zip` | https://services.gradle.org/distributions/gradle-9.9.2-bin.zip.sha256 | I1 (`--gradle-distribution-sha256-sum`) | not in the research |
| Q-2 | SHA-256 of `gradle-9.9.2-wrapper.jar` | https://services.gradle.org/distributions/gradle-9.9.2-wrapper.jar.sha256 | I1 (AC-6 (c)) | not in the research |
| Q-3 | Gradle 9.9.2 compatibility page, versioned: verbatim the Java table row for Java 25, the sentence on the JVM range for running Gradle, the Kotlin "tested with" sentence, the AGP "tested with" sentence | https://docs.gradle.org/9.9.2/userguide/compatibility.html | I0 (AC-3) | C1 and C2 cite `/current/`; C1's Java 25 row is paraphrased |
| Q-4 | AGP: verbatim the about-agp table row giving AGP 9.5's minimum Gradle; the AGP 9.5 release notes' minimum JDK, SDK Build Tools minimum and default, and the maximum API level statement; whether 9.5.2 is now listed | https://developer.android.com/build/releases/about-agp ; https://developer.android.com/build/releases/agp-9-5-0-release-notes | I0 (AC-3), I13 (AC-15 (b), (c)) | C3 gives table values only; C4 quotes only the API sentence |
| Q-5 | Kotlin: verbatim the table row(s) for KGP 2.4.20 to 2.4.21 (Gradle and AGP ranges) and the full sentence "You can also use Gradle and AGP versions up to the latest releases …" without the ellipsis | https://kotlinlang.org/docs/gradle-configure-project.html | I0 | C7 gives values and an elided quote |
| Q-6 | KSP: verbatim the 2.3.14 release notes, and the "Defect in release" mark on 2.3.13 | https://github.com/google/ksp/releases/tag/2.3.14 ; …/tag/2.3.13 | I0 | C9 cites 2.3.0, 2.3.10, 2.3.12 and repository files of the 2.3.14 tag only |
| Q-7 | Dagger: verbatim the 2.60.1 release notes | https://github.com/google/dagger/releases/tag/dagger-2.60.1 | I0 | C10, C11, C13 quote 2.59, 2.59.1, 2.60 only |
| Q-8 | Robolectric: verbatim the compatibility-table row for 4.18 (SDK range, JDK notes) and the 4.18.1 release notes | https://robolectric.org/compatibility_table/ ; https://github.com/robolectric/robolectric/releases/tag/robolectric-4.18.1 | I0 | C15 paraphrases the table row; only 4.18's notes are quoted |
| Q-9 | ByteBuddy Java 25 support at 1.17.7 (Mockito 5.24.0): verbatim from the Mockito or ByteBuddy release notes | https://github.com/mockito/mockito/releases ; https://github.com/raphw/byte-buddy/releases | I0 (record), I8 (AC-13) | research §6.8: not re-checked |
| Q-10 | GitHub-hosted runners: verbatim that `ubuntu-22.04` is still offered (A18); optionally re-confirm checkout 7.0.1, setup-java 6.3.0, cache 6.1.0 as newest non-pre-release | https://github.com/actions/runner-images ; the three releases pages | I13 (AC-15 (d)) | not in the research (the design's D1 rows carry the versions) |
| Q-11 | (optional) JaCoCo 0.8.16 entry, verbatim, if the record wants more than C17's sentence | https://www.jacoco.org/jacoco/trunk/doc/changes.html | — | C17 is verbatim already |
| Q-12 | (optional) Gradle user guide, "Upgrading the Gradle Wrapper": the sentence on running the `wrapper` task a second time | https://docs.gradle.org/9.9.2/userguide/gradle_wrapper.html | — | P1; the procedure stands on L5 and D22 without it |

If a fallback fires, the orchestrator also supplies that fallback's inputs when it authorises resumption: F2 needs the
Gradle 9.7.0 `bin.zip` and `wrapper.jar` SHA-256 values and the Gradle 9.7 compatibility page re-read.

### 2.4 Fallbacks (AC-5; design D3 steps 3 to 5) — stop and report when one fires

**The binding rule** (orchestrator, Implementation Notes "Fallbacks", stricter than D3 step 5): when a fallback's trigger
is met, the developer records the error verbatim, restores the increment's working changes (`git restore
--source=HEAD --worktree -- <the increment's files> gradle/verification-metadata.xml`), writes the log entry and
**stops**. The orchestrator records the flag for the product owner (F2 and F4 with their Part B consequence) and
authorises resumption; the developer then re-runs the increment in full, including D5, with the fallback edit below.
Never taken: a JDK change, a version below today's, a pre-release. A failure F1 to F5 do not cover stops the story for
an SA amendment (below today's version or the JDK: the product owner's decision). A checksum mismatch on an unchanged
artifact is a stop, never re-written (INT-001).

| Fallback | Trigger (verbatim error required) | Pre-specified edit on resumption | Re-run |
|----------|-----------------------------------|----------------------------------|--------|
| F1 | An AGP-originated error (`com.android.build.*`) or a 9.5.2-only regression at I3 or later | `build.gradle`: `'com.android.tools.build:gradle:9.5.2'` → `'com.android.tools.build:gradle:9.5.1'` | I3 in full |
| F2 | A KGP- or KSP-originated error that names a Gradle or AGP API, version or behaviour | Gradle 9.9.2 → 9.7.0 (the I1 procedure with `9.7.0` and the supplied 9.7.0 checksums) and AGP → `9.3.1` | I1, then I3, each in full |
| F3 | A KSP-originated error (`com.google.devtools.ksp.*`) not covered by the reorder (row D54) | `'…ksp.gradle.plugin:2.3.14'` → `'…ksp.gradle.plugin:2.3.12'` (never 2.3.13) | I2 (or I5 if reordered) |
| F4 | Hilt 2.60.1 or KSP rejects Kotlin 2.4 metadata at I5 | I5's two Kotlin edits are not made (Kotlin stays 2.3.21, Hilt 2.60.1 kept); I5 then commits nothing for Kotlin and its mutations are not run (no Kotlin change to guard) | — (the SA rewrites I14's Kotlin texts) |
| F5 | Robolectric 4.18.1 cannot run the suite on JDK 25 even with the D7 flags | `'org.robolectric:robolectric:4.18.1'` → `'org.robolectric:robolectric:4.17'`; E7.2's text names 4.17 | I7 in full |

After a fallback, every later text of section 3 that names the fallen-back version is adapted by substituting the
version token only; the orchestrator's resumption note lists the substitutions.

### 2.5 Decisions and trade-offs

| Decision | Gained | Gives way | Not chosen | Why |
|----------|--------|-----------|------------|-----|
| Run the `wrapper` task twice (on 9.5.1, then on 9.9.2), re-applying the block after each (I1) | The jar and scripts are 9.9.2's own, so AC-6 (c) can compare the jar with the published 9.9.2 hash | One more short Gradle run; the block is re-applied twice | (a) one run as the design writes it; (b) U-060's bootstrap: hand-edit the properties first | (a) leaves the 9.5.1 jar (L5) and AC-6 (c) fails; (b) is a hand edit of a generated file, which AC-6 (a) reviews against "only what the generator changes" |
| `clean` in every gate run, and the gate run carries `--warning-mode all` | Coverage after an AGP or Kotlin change is never measured on stale classes (U-060's stale Kotlin directory); one run gives both the gate and the warning record | About 3 extra minutes per gate | `clean` only after AGP/Kotlin; a separate warning-mode run | Uniform commands remove a class of mistakes; a separate run adds 11 Gradle runs for the same information |
| The same evidence set at every increment (STD, 4.0) | Every commit list is derivable from one rule; every comparison is available at every increment | About 21 small files per increment | Evidence only where an AC names it | Plan-review B-4 of U-068 (a list naming a file no step writes) cannot recur |
| A throwaway provider probe at I0 and I7 (4.P) | AC-12 (d)'s "both lists" exist whether or not a test fails; design D7 item 5's "environment probes" are run | A file copied into `app/src/test` twice for one targeted run each, under a write-ahead protocol | (a) no probe; (b) re-adding U-063's helper smoke test | (a) leaves only Truth's failure message, which shows the allow-list and the filtered result but not the provider's default-enabled set; (b) depends on fixtures whose API may have moved since U-063 (SC-P2) |
| Strict equality for `configuration.txt` (AC-8 (c), AC-21 (b)), with the rule origin in the report | Nothing in R8's input changes unseen | Likely stops at I3 to I6 (P2), each needing an orchestrator decision | Accepting differences from the bumped component's own rules | The story says "any difference is a stop and is investigated, never waved"; SC-P1 asks for a standing ruling so the stops become one decision |
| Stop on a fallback (ruled) | The product owner sees every fallback before the next increment builds on it | A pause per fallback | D3 step 5's "take it and continue" | Ruled |
| Mutations before the I5 commit | The I5 commit carries its own mutation evidence (AC-24) | The protocol must prove the restore against the working tree, not only HEAD | Mutations after the commit | AC-24: each commit contains its increment's evidence |
| Forced production fixes: only the pre-specified `@param:` edits; any other forced fix stops for an SA-supplied exact edit (SC-P4) | Every production edit is exact text a reviewer can check | A stop for a javac `-Werror` warning that D3 lets the developer fix | Letting the developer write the call-site fix | The delegation asks for exact old/new text for every edit; no such warning is predicted |
| One `:app:dependencies` report per DEP step, parsed per configuration | One run gives both `releaseRuntimeClasspath` and `debugUnitTestRuntimeClasspath` | A large raw log in the work folder | Two `--configuration` runs | Half the runs |

### 2.6 Coverage arithmetic (re-derived at I0; rows D38, D39, D60, D79)

For a package with `c` covered of `t` lines and floor `f`, the gate needs `c/t ≥ f`; spare lines = `c − ⌈f·t⌉`
(`cov_floors.py`, exact fractions). Known margins after U-068 and U-069 (L14, L15, L17; re-measured at I0):

| Package (floor) | Expected at I0 | Spare | What can move it in this story |
|-----------------|----------------|-------|--------------------------------|
| `worker` (0.95, U-069) | 16/16 = 100 % (if U-069's measurement holds) | **0** | a generated class starting to report lines (Hilt 2.60.1 at I4, androidx.hilt 1.4.0 at I6, JaCoCo 0.8.16 filters at I12), or Kotlin 2.4 changing line attribution (I5) — any single line fails the gate; rows D38, D60 |
| `compat` (0.20) | 18/71 = 25.4 % | 3 | AGP unit-test manifest (I3), Kotlin (none: Java package) |
| `receiver` (0.50) | 39/58 = 67.2 % | 10 | AGP 9.5's unit-test manifest (I3; AC-8 (d)) |
| `scheduler` (0.60) | measured (U-069 edits it) | measured | Kotlin 2.4 (I5) |
| `activity` (0.10) | 189 or 199 of about 1008 (alternates, L14) | about 90 | test scheduling: a covered-count change in `activity` alone within that band is known noise and is not investigated further |
| every other package | measured | measured | — |

A breach is never fixed by a floor (AC-20 (c)); it stops the story (row D38), and a coverage-restoring test is an SA
amendment. "JaCoCo counting" is never given as a cause without the per-class and per-line evidence (`jacoco_class_lines.py`,
`jacoco_lines.py`; U-060's lesson).

### 2.7 What each before/after comparison can and cannot show

| Comparison | Can show | Cannot show |
|------------|----------|-------------|
| Four gates green | The build compiles, every test passes, every floor holds, R8 runs | That runtime behaviour on a device is unchanged (IC-4 to IC-6) |
| JUnit totals and the full case list equal to I0 | No test was lost, renamed or skipped by a toolchain change | That a test still detects what it was written to detect (only I5's mutations show that, for three loops) |
| Merged and packaged manifests equal, invariants hold | No component, permission or attribute moved; CNTR-005 and BUG-016 invariants intact | Whether the platform honours them at run time |
| Kept-name set and `configuration.txt` equal | R8's keep input and its kept classes are unchanged | Whether R8's optimisation of non-kept code changed (`-dontoptimize` is set; mapping line counts are recorded, not compared) |
| APK entry lists, `.so` = 0, dex counts | Packaging is unchanged except what the toolchain explains | Byte-level equality (APK hashes always differ) |
| javap majors 61 / 52 | Bytecode targets unchanged (AC-15) | Anything about the bytecode beyond the version |
| KSP file list equal | Hilt code generation produced the same set of files | That the generated code is identical (only names are compared) |
| Per-package coverage and class inventory | A toolchain change did not drop classes or lines from measurement | Whether two different line sets of equal size are measured (per-class figures are recorded to narrow this) |
| Verification proof | Gradle's writer only added entries and the configuration is unchanged | That an added artifact is genuine (the group review and the trusted network do that, D5); anything outside Gradle's verification (`android-all`, the wrapper distribution, SDK packages, actions) |
| Mutations at I5 | The cancellation tests still distinguish the fixed check from the old binding and from a deleted check, under Kotlin 2.4 | Any WorkManager suspension behaviour (unchanged in S2) |
| Provider probe I0 vs I7 | The default provider, its version, default-enabled protocols and suites under Robolectric before and after | Platform TLS on a device |
| Local CI-parity run | The CI commands and tasks exist and run on this machine | That the Linux runner builds, or that the new action majors run (IC-6) |

### 2.8 Edge cases the steps handle

- **The generated `gradlew` changes shape** (template moved, no `uname` case): `gradlew_block.py` fails, row D17 stops.
- **The wrapper task drops `distributionSha256Sum`** or changes other keys: I1 step 8 and row D23.
- **The first 9.9.2 run cannot download** (network): retried once (row D20); a checksum failure is a stop (D18).
- **Gradle 9.9.2 rejects the `updateDaemonJvm` configuration** (`build.gradle:53`): a Gradle error outside F1 to F5 (D21).
- **The verification writer re-serialises the file** (whitespace, order): the semantic proof decides; a "configuration
  block unchanged: False" caused only by whitespace is still a stop for review (D28).
- **`lintVitalRelease` fails `assembleRelease` on AGP 9.5's lint**: the fix would touch `lint.xml`, the baseline or
  source; stop (D51).
- **AGP 9.5 moves the Kotlin class directory**: the JaCoCo guard fails; the path in `jacocoClassRootPaths` is updated
  as AC-8 (b) pre-specifies (D55); the EV-B javap paths follow (recorded).
- **A13 forces `compileSdk` and the like**: pre-specified edits for `:app` only; the same in `:k9mail-vendored` would
  need a forbidden edit, so it stops (D56).
- **Kotlin 2.4 qualifier targets** (Risk 9): pre-specified `@param:` edits (D58).
- **U-074's tests in the NEW3 classes fail under a mutation**: allowed only for a mutation of the loop the test drives (D64).
- **Robolectric 4.18 downloads new `android-all` jars**: recorded with coordinates and checksums against their
  `.sha512` files, as U-068 ruling P-1 did; a failed download is retried once (D73).
- **`androidx.test:monitor` or another AAR states a `minCompileSdk` above 35**: the N-6 move (D74).
- **A test-only bump shifts a test-classpath version of an S4 library** (Robolectric pulling a newer `androidx.*`
  onto `debugUnitTestRuntimeClasspath`): recorded at I7 and I14 (IC-1); only `releaseRuntimeClasspath` changes are
  AC-17 (e) stops.
- **The harness moves a long Gradle run to the background**: wait for the completion notice, never poll (constraint 2).
- **Endpoint security removes another tool** (`unzip`, `sha256sum`, `git` parts): stop and report (D52).
- **A session ends mid-increment**: the resume protocol (4.R) and the write-ahead lines decide what is undone.
- **The orchestrator commits documents while the developer runs**: every end check is scoped to this story's subjects
  and paths (I14 step 7).
- **`ci.yml` has mixed line endings in the working tree** (L2): edits are by exact text; `git` normalises on diff and
  commit; the YAML parse reads the edited file.
- **PyYAML reads `on:` as the boolean `True`** (YAML 1.1): expected in the parse output (I13 step 5).

### 2.9 Invariants per increment (AC-21) and what may legitimately change

"=" means equal to I0 (and to the previous increment); a difference is a stop unless the cell names it.

| Check | I1 | I2 | I3 | I4 | I5 | I6 | I7 | I8–I11 | I12 | I14 |
|-------|----|----|----|----|----|----|----|--------|-----|-----|
| Merged and packaged manifests (normalised), invariants | = | = | = | = | = | = | = | = | = | = |
| Kept-name set | = | = | = | = | = | = | = | = | = | = |
| `configuration.txt` | = | = | = (SC-P1) | = (SC-P1) | = (SC-P1) | = (SC-P1) | = | = | = | = |
| APK entry lists | = | = | explained by AGP only | explained by Hilt only | explained by Kotlin only | explained by androidx.hilt only | = | = | = | = |
| `.so` count | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| KSP file list | = | = (AC-7) | = | differences explained (AC-9) | = I4's list (SC-P3) | = I4's list | = | = | = | = I4's list |
| javap majors 61, 61, 52 | = | = | = | = | = | = | = | = | = | = |
| Case list, JUnit totals | = | = | = | = | = | = | = | = | = | = |
| Coverage `t` per package; class inventory | = | = | explained by AGP only | explained by Hilt only | explained by Kotlin only | explained by androidx.hilt only | = | = | = I11 (AC-14) | = |
| `releaseRuntimeClasspath` (S4-owned versions) | — | — | — | = | = | = | = | — | — | = |

The invariant greps run on the packaged manifests (`aapt2 dump xmltree`) and the merged ones, each expected `1` except
`InitializationProvider` `0` (values derived from U-068's I5 files, L14): `grep -A4
'android:name(0x01010003)="androidx.work.impl.foreground.SystemForegroundService"' F | grep -c
'foregroundServiceType(0x01010599)=0x00000001'`; `grep -c InitializationProvider F`; `grep -A2
'android:name(0x01010003)="com.zegoggles.smssync.receiver.BackupBroadcastReceiver"' F | grep -c
'exported(0x01010010)=true'`; the same `grep -A5 … | grep -c '"com.zegoggles.smssync.BACKUP"'`; `grep -c
'minSdkVersion(0x0101020c)=24' F`; and in the merged files `grep -c 'android:minSdkVersion="24"'`.

## 3. Exact Changes

Every edit is one Edit-tool call whose `old_string` is the text given, copied from a Read of the file. Each occurs
exactly once at its increment (twice for `cache@v4`, replaced with `replace_all`) — verified by simulation (L11) and
re-checked at I0 by `oldtext_check.py` (row D6). Expected `git diff --numstat HEAD` of the file after the increment's
edits is given per row (L11). U-069's two Rule 15 lines are not touched by any edit.

### 3.1 S2-I1 — the wrapper (generator) and the `gradlew` block (hand, byte for byte)

Generated by `./gradlew wrapper --gradle-version 9.9.2 --distribution-type bin --gradle-distribution-sha256-sum <Q-1>`
run twice (I1 steps 2 and 5). The LOCAL ADDITION block re-applied after each run is `<start>`'s `gradlew:117-127`,
verbatim:

```
# >>> LOCAL ADDITION to the generated script (U-002 / U-060, AC-18): Windows-only NIO workaround. <<<
# Under Git Bash/MSYS/Cygwin any JDK 17-25 JVM here fails Selector.open() with "Unable to establish loopback
# connection" unless the unix-domain tmpdir is overridden. JAVA_TOOL_OPTIONS is inherited by every forked JVM
# (launcher AND the Gradle daemon), which the org.gradle.jvmargs entries alone do not guarantee.
# NOT benign elsewhere: sun.nio.ch.WindowsSelectorProvider does not exist on Linux/macOS JDKs, and naming it makes
# the JVM throw ServiceConfigurationError, so this is exported only under Windows shells (cygwin/msys, detected above).
# Re-apply after every `./gradlew wrapper` regeneration.
if "$cygwin" || "$msys" ; then
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS} -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider -Djdk.net.unixdomain.tmpdir=/nonexistent"
fi
# <<< END LOCAL ADDITION <<<
```

**E1 (Edit on the generated `gradlew`, after each run).** `old_string`: the two lines `  NONSTOP* )        nonstop=true ;;`
and `esac` (as read from the generated file). `new_string`: the same two lines, then one empty line, then the eleven
block lines above (taken from the Read of `$WORK/gradlew-block.txt`, I1 step 1). The generated file's own lines after
`esac` follow unchanged. Proof: `gradlew_block.py <start gradlew> <generated copy> gradlew` → all `OK`, and
`git diff --no-index --numstat <generated copy> gradlew` → `12	0` (one blank line and eleven block lines; derived on a
synthetic generated copy, L11). If the generated file's last case line is not `  NONSTOP* )        nonstop=true ;;`,
`old_string` is the line actually preceding the first `esac` after `case "$( uname )" in`, plus `esac` (the check
tool locates the insertion point independently, so the proof is the same). `gradlew.bat` is never edited.

### 3.2 S2-I2 to S2-I12 — version lines

| Id | Inc. | File | `old_string` → `new_string` | numstat |
|----|------|------|-----------------------------|---------|
| E2 | I2 | `build.gradle` | `        classpath 'com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6'` → `        classpath 'com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.14'` | `1 1` |
| E3 | I3 | `build.gradle` | `        classpath 'com.android.tools.build:gradle:9.0.1'` → `        classpath 'com.android.tools.build:gradle:9.5.2'` | `1 1` |
| E4.1 | I4 | `build.gradle` | `        classpath 'com.google.dagger:hilt-android-gradle-plugin:2.59.2'` → `…:2.60.1'` | `1 1` |
| E4.2 | I4 | `app/build.gradle` | `    implementation 'com.google.dagger:hilt-android:2.59.2'` → `    implementation 'com.google.dagger:hilt-android:2.60.1'` | with E4.3 to E4.5: `4 4` |
| E4.3 | I4 | `app/build.gradle` | `    ksp 'com.google.dagger:hilt-compiler:2.59.2'` → `    ksp 'com.google.dagger:hilt-compiler:2.60.1'` (does not match the `kspTest` line) | |
| E4.4 | I4 | `app/build.gradle` | `    testImplementation 'com.google.dagger:hilt-android-testing:2.59.2'` → `…:2.60.1'` | |
| E4.5 | I4 | `app/build.gradle` | `    kspTest 'com.google.dagger:hilt-compiler:2.59.2'` → `    kspTest 'com.google.dagger:hilt-compiler:2.60.1'` | |
| E5.1 | I5 | `build.gradle` | `        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21'` → `…:2.4.21'` | `1 1` |
| E5.2 | I5 | `app/build.gradle` | `    implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.3.21'` → `    implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.4.21'` | `1 1` |
| E6.1 | I6 | `app/build.gradle` | `    implementation 'androidx.hilt:hilt-work:1.2.0'` → `    implementation 'androidx.hilt:hilt-work:1.4.0'` | with E6.2: `2 2` |
| E6.2 | I6 | `app/build.gradle` | `    ksp 'androidx.hilt:hilt-compiler:1.2.0'` → `    ksp 'androidx.hilt:hilt-compiler:1.4.0'` | |
| E7.1 | I7 | `app/build.gradle` | `'org.robolectric:robolectric:4.15.1'` → `'org.robolectric:robolectric:4.18.1'` (the trailing comment of `:160` stays until I14) | with E7.2: `4 4` |
| E7.2 | I7 | `app/build.gradle` | the three lines `:592-594` (below) → the three lines below (main path, row D69 not fired) | |
| E8 | I8 | `app/build.gradle` | `'org.mockito:mockito-core:5.18.0'` → `'org.mockito:mockito-core:5.24.0'` | `1 1` |
| E9 | I9 | `app/build.gradle` | `"com.google.truth:truth:1.4.4"` → `"com.google.truth:truth:1.4.5"` (double quotes kept) | `1 1` |
| E10 | I10 | `app/build.gradle` | `'com.icegreen:greenmail-junit4:2.1.3'` → `'com.icegreen:greenmail-junit4:2.1.15'` | `1 1` |
| E11 | I11 | `app/build.gradle` | `'com.google.auto.service:auto-service:1.0'` → `'com.google.auto.service:auto-service:1.1.1'` | `1 1` |
| E12 | I12 | `app/build.gradle` | `toolVersion = "0.8.14"` → `toolVersion = "0.8.16"` (the `:212` comment stays until I14) | `1 1` |

**E7.2** `old_string`:
```
// U-005: The --add-opens jvmArgs block that was added as a stopgap for Robolectric 4.3.1
// incompatibility with JDK 17 has been removed. Robolectric 4.15.x handles JDK 17-25
// natively and no longer requires these JVM flags (re-verified on JDK 25 in U-060).
```
`new_string` (main path, D7 zero hits; `<YYYY-MM-DD>` from `date -u +%F` at I7):
```
// U-071 (<YYYY-MM-DD>): no --add-opens JVM flag is set. Robolectric 4.18.1 runs this suite at SDK 24 and 29 on JDK 25
// without one: the full run showed no InaccessibleObjectException, IllegalAccessError, 'does not "opens"' or
// 'module java.base' error (design D7). The U-005 stopgap flags for Robolectric 4.3.1 stay removed.
```
**E7.3 (only under row D69, D7 hits).** `old_string` `            includeAndroidResources = true` → `new_string`:
```
            includeAndroidResources = true
            // U-071 (<YYYY-MM-DD>): Robolectric 4.18.1 on JDK 25 names this module access (design D7):
            // "<the first error line naming it, verbatim>". robolectric.org: "When running tests on Java 17 or higher,
            // the JVM requires --add-opens flags".
            all {
                jvmArgs '<--add-opens=module/package=ALL-UNNAMED, exactly as the error names it>'
            }
```
one `jvmArgs` argument per distinct `module/package` the errors name, nothing else. Under D69, E7.2's `old_string`
(the same three lines) is replaced by this single line instead of the main-path text:
`// U-071 (<YYYY-MM-DD>): the --add-opens flags Robolectric 4.18.1 needs on JDK 25 are set in android.testOptions.unitTests.all above (design D7).`
The numstat of I7 is then recorded as measured (it is no longer `4 4`).

**Risk 9 forced edits (only under row D58).** In each of `activity/MainViewModel.kt`, `scheduler/WorkManagerScheduler.kt`,
`service/SharedPreferencesCheckpointStore.kt`, `service/WorkerEngineFactory.kt` (under `app/src/main/java/com/zegoggles/smssync/`):
`old_string` `    @ApplicationContext private val context: Context` → `new_string` `    @param:ApplicationContext private val context: Context`
(the rest of the line, a trailing comma or not, is outside `old_string` and stays). Recorded as forced changes with the
error verbatim.

**A13 forced edits (only under row D56).** `app/build.gradle`: `    compileSdkVersion 35` → `    compileSdk 35`;
`        minSdkVersion 24` → `        minSdk 24`; `        targetSdkVersion 35` → `        targetSdk 35`. Values unchanged.

**Class-root forced edit (only under row D55).** The one `jacocoClassRootPaths` line whose directory is missing, e.g.
`    kotlin: 'intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes', // U-060: …` → the same line with the
directory AGP 9.5.2 writes (found by I3 step 9's `find`), comment `// U-071: AGP 9.5.2 output` appended; the guard
(`:222-229`) is not touched.

### 3.3 S2-I13 — `.github/workflows/ci.yml` (numstat `8 7`, six `-U0` hunks, L11)

| Id | `old_string` → `new_string` |
|----|-----------------------------|
| E13.1 | `        uses: actions/checkout@v4` → `        uses: actions/checkout@v7` |
| E13.2 | `        uses: actions/setup-java@v4` → `        uses: actions/setup-java@v6` |
| E13.3 | `        uses: actions/cache@v4` → `        uses: actions/cache@v6` (`replace_all`, two occurrences) |

**E13.4** `old_string`:
```
      # Gradle 9.5 / AGP 9.0 run on JDK 17+; this project builds and tests on JDK 25, declared in
      # gradle/gradle-daemon-jvm.properties. AGP 9.0 requires SDK Build Tools >= 36.0.0.
```
`new_string`:
```
      # Gradle 9.9 / AGP 9.5 run on JDK 17+; this project builds and tests on JDK 25, declared in
      # gradle/gradle-daemon-jvm.properties. AGP 9.5's SDK Build Tools minimum and default is 36.0.0
      # (developer.android.com/build/releases/agp-9-5-0-release-notes).
```
**E13.5** `      # AGP 9.0 needs Build Tools 36.0.0 (the runner image ships it today; this guards against image drift).` →
`      # AGP 9.5 needs Build Tools 36.0.0, its minimum and default (this step guards against runner-image drift).`

Nothing else in the workflow changes: `runs-on` (`:13`), triggers, cache keys and paths, the `sdkmanager` step and both
`./gradlew` commands keep their text (AC-15 (e), (h)). If Q-4's verbatim wording differs from "minimum and default",
E13.4 and E13.5 keep this wording (it restates C4) and the log quotes Q-4 beside it.

### 3.4 S2-I14 — comments and `code-location.md` (numstat `9 9`, `8 8`, `3 3`, L11)

`build.gradle`:

| Id | `old_string` | `new_string` |
|----|--------------|--------------|
| E14.1 | the three lines `:7-9`: `        // U-060: Gradle 9.5.1 / AGP 9.0.1 / KGP 2.3.21 run the build on JDK 25 (app bytecode stays 17).` / `        // AGP 9.0 needs Gradle >= 9.1.0, JDK >= 17, KGP >= 2.2.10, SDK Build Tools >= 36.0.0` / `        // (developer.android.com/build/releases/agp-9-0-0-release-notes).` | `        // U-071: Gradle 9.9.2 / AGP 9.5.2 / KGP 2.4.21 run the build on JDK 25 (app bytecode stays 17).` / `        // AGP 9.5 needs Gradle >= 9.6.0, JDK >= 17, KGP >= 2.2.10 (built-in Kotlin); its SDK Build Tools` / `        // minimum and default is 36.0.0 (developer.android.com/build/releases/agp-9-5-0-release-notes).` |
| E14.2 | `:12-13`: `        // U-060: KGP 2.3.21 (AGP 9 built-in Kotlin needs >= 2.2.10; declared here so a KGP newer than` / `        // AGP's runtime 2.2.10 is used). 2.4.x is excluded: Hilt 2.59.2 cannot read Kotlin 2.4 metadata.` | `        // U-060/U-071: KGP 2.4.21 (AGP 9 built-in Kotlin needs >= 2.2.10; declared here so a KGP newer than` / `        // AGP's runtime 2.2.10 is used). Kotlin 2.4 metadata needs Hilt 2.60 or newer (Dagger 2.60 release notes).` |
| E14.3 | `:15-16`: `        // U-057/U-060: KSP replaces kapt for Hilt. KSP 2.3.x is KSP2-only and the first line (2.3.1+)` / `        // with AGP 9 built-in Kotlin support; 2.3.6 includes the Windows KSP2 recompilation fix.` | `        // U-057/U-071: KSP replaces kapt for Hilt. KSP 2.3.x is KSP2-only and no longer tied to the Kotlin` / `        // version; never 2.3.13 (marked "Defect in release"). Dagger's KSP support is stable from KSP 2.3.9.` |
| E14.4 | `        // U-022/U-060: Hilt Gradle plugin (must precede app/build.gradle application of` | `        // U-022/U-071: Hilt Gradle plugin (must precede app/build.gradle application of` |
| E14.5 | `        // dependencies in app/build.gradle. 2.59.x is the first Hilt line with AGP 9 support.` | `        // dependencies in app/build.gradle. Hilt 2.59 and newer need AGP 9; 2.60 reads Kotlin 2.4 metadata.` |

`app/build.gradle`:

| Id | `old_string` | `new_string` |
|----|--------------|--------------|
| E14.6 | `// U-060: the standalone Kotlin Android plugin application was removed - AGP 9 provides built-in Kotlin (KGP 2.3.21 on the root classpath).` | `// U-060: the standalone Kotlin Android plugin application was removed - AGP 9 provides built-in Kotlin (KGP 2.4.21 on the root classpath, U-071).` |
| E14.7 | `:5-6`: `// U-060: KSP 2.3.6 (KSP2-only) matches the Kotlin 2.3.x line and supports AGP 9 built-in Kotlin.` / `// Hilt 2.59.2 supports KSP for mixed Java+Kotlin source modules and requires AGP 9.` | `// U-071: KSP 2.3.14 (KSP2-only; not tied to the Kotlin version since KSP 2.3.0) supports AGP 9 built-in Kotlin.` / `// Hilt 2.60.1 supports KSP for mixed Java+Kotlin source modules and requires AGP 9.` |
| E14.8 | `    // U-057/U-060: kotlin-stdlib matches the Kotlin Gradle plugin (2.3.21).` | `    // U-057/U-071: kotlin-stdlib matches the Kotlin Gradle plugin (2.4.21).` |
| E14.9 | `' // U-060: ASM 9.8 reads Java 25 class files. U-068: no test is configured below the minSdk of 24.` (end of `:160`) | `' // U-071: runs SDK 24 to 37 (sandbox ASM 9.11.1). U-068: no test is configured below the minSdk of 24.` |
| E14.10 | `' // U-060: bundles ByteBuddy 1.17.5 (Java 25)` (end of `:163`) | `' // U-071: bundles ByteBuddy 1.17.7 (Java 25)` |
| E14.11 | `    // U-060: Hilt 2.59.2 (plugin, runtime, compiler, testing all identical) - AGP 9 + Kotlin 2.3.x + KSP2.` | `    // U-071: Hilt 2.60.1 (plugin, runtime, compiler, testing all identical) - AGP 9 + Kotlin 2.4.x + KSP2.` |
| E14.12 | `" // U-060: first release with official Java 25 support` (end of `:212`) | `" // U-071: officially supports class files up to Java 27 (JaCoCo change history)` |

`sdlc/artifacts/engagement/code-location.md` (LF; rows `:25`, `:26`, `:29`; `:28` is U-068's):
```
E14.13  | Build system | Gradle 9.5.1 (wrapper: `./gradlew`), running on **JDK 25** (see Build Environment) |
     -> | Build system | Gradle 9.9.2 (wrapper: `./gradlew`), running on **JDK 25** (see Build Environment) |
E14.14  | Android Gradle Plugin | 9.0.1 (`com.android.application`, built-in Kotlin); Kotlin 2.3.21, KSP 2.3.6, Hilt 2.59.2 |
     -> | Android Gradle Plugin | 9.5.2 (`com.android.application`, built-in Kotlin); Kotlin 2.4.21, KSP 2.3.14, Hilt 2.60.1, androidx.hilt 1.4.0 |
E14.15  | Build tools | 36.0.0+ (AGP 9.0 minimum) |
     -> | Build tools | 36.0.0 (AGP 9.5's minimum and default; no `buildToolsVersion` is declared) |
```
Every new text avoids the AC-16 sweep strings and `alpha|beta|rc[0-9]` (L11). If ByteBuddy's resolved version at I8 is
not 1.17.7 (row D76), E14.10 names the resolved one. No new sentence claims that CI has run (AC-16 (e)).

## 4. Implementation Steps

### 4.0 Environment and conventions (every step)

```bash
# From the repository root. Never set MSYS_NO_PATHCONV for Gradle.
unset MSYS_NO_PATHCONV
W=sdlc/artifacts/build/sprints/sprint-025/U-071; E=$W/evidence; M=$E/mutations; TL=$E/tools
T=sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools      # read-only: sanitize.py junit_totals.py junit_cases.py jacoco_pkgs.py
T68=sdlc/artifacts/build/sprints/sprint-024/U-068/evidence/tools   # read-only: sdk_dir.py jacoco_inventory.py jacoco_lines.py lint_ids.py
R=app/build/test-results/testDebugUnitTest
JX=app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml
S=app/src/main/java/com/zegoggles/smssync; TS=app/src/test/java/com/zegoggles/smssync
BW=$S/service/BackupWorker.kt; RW=$S/service/RestoreWorker.kt
PROBE=$TS/mail/ssl/U071ProviderProbeTest.java
PLAN_HEAD=7c5e6073ebd4ddd2d9e737ded20e7c134b2fb97f; U068_START=4300ffa98370b77dcc4dd8d12142079aada8313a
SUPP=sdlc/artifacts/build/research/2026-11-21-u071-quote-supplement.md
SDK="$(python -I -X utf8 $T68/sdk_dir.py)"; AAPT2="$SDK/build-tools/36.0.0/aapt2"
JDKBIN="$(ls -d "/c/Program Files/Java/"jdk-25* | tail -1)/bin"
WORK=<a u071-work folder inside the developer's own session scratch folder>   # raw logs, copies, messages
START=<the commit recorded in I0 step 1>
```
Shell state does not persist; every call sets what it needs. `WORK`, `SDK`, `JDKBIN` and the home folder are never
written into a repository file (evidence says "the work folder", "the SDK", "the JDK", "the Gradle cache").
`<p>` below is the evidence prefix of the previous increment that ran (I0 for I1; I1 for I3 if I2 moved).

**Helper tools** (I0 step 2, Write tool, into `$TL/`; QA re-runs them). Their text is fixed here.

| Tool | Source | Purpose |
|------|--------|---------|
| `oldtext_check.py` | this plan (tested on a `git archive HEAD` copy, L11) | every `old_string` of section 3 and the mutation and Risk 9 anchors occur the expected number of times |
| `gradlew_block.py` | this plan (tested on a synthetic generated file, L11) | AC-6 (b) proof: the block re-applied byte for byte, nothing else changed by hand |
| `vm_proof.py` | U-060 plan Step 6.3 script, verbatim lines marked `# U-060`, plus the added-component list | AC-17 (c), (d), (h) |
| `deps_resolved.py` | this plan (tested on U-060's `after-deps-releaseRuntime.txt`: 96 coordinates, `MULTI 0`) | AC-1 (k), AC-17 (e), IC-1 |
| `cov_floors.py` | this plan (tested on the current report: `RULES 18`) | per-package ratio against every floor, spare lines (AC-20 (b)) |
| `jacoco_class_lines.py` | this plan (tested on the current report) | per-class covered/total and classes without a counter (AC-14, AC-20 (d)) |
| `scan.py` | the detection rules of U-063's `sanitize.py` (IC-8), one process for many files (tested on U-067/U-068 evidence: no false hit) | hygiene check of every evidence file before `git add` |
| `U071ProviderProbeTest.java.txt` | this plan (U-063 IC-11 form) | the throwaway probe source (4.P); a `.txt` so it is never compiled where it is stored |
| `sanitize.py`, `junit_totals.py`, `junit_cases.py`, `jacoco_pkgs.py` | U-063 (`$T`, read-only) | log filtering with machine details replaced; XML totals; full case list; package ratios |
| `sdk_dir.py`, `jacoco_inventory.py`, `jacoco_lines.py`, `lint_ids.py` | U-068 (`$T68`, read-only) | SDK folder; U-060 class inventory; per-line coverage; lint rule IDs |

`oldtext_check.py`:
```python
"""U-071 start-state text check (plan section 3, row D6): every old_string the plan edits or mutates must occur
exactly the expected number of times. usage (from the repository root): python -I -X utf8 oldtext_check.py [root]
Files are read with CRLF folded to LF (the working tree is CRLF for most files under core.autocrlf=true).
Prints 'OK|BAD <id> <file> count=<n> expected=<m>' per entry and 'ALL OK' or 'MISMATCH <k>'; exit 1 on any mismatch."""
import os, sys
root = sys.argv[1] if len(sys.argv) > 1 else '.'
B, A, C, L = 'build.gradle', 'app/build.gradle', '.github/workflows/ci.yml', 'sdlc/artifacts/engagement/code-location.md'
S = 'app/src/main/java/com/zegoggles/smssync/'
BW, RW = S + 'service/BackupWorker.kt', S + 'service/RestoreWorker.kt'
CHK = 'currentCoroutineContext().ensureActive()'
E = [
 ('E2', B, "        classpath 'com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.6'\n", 1),
 ('E3', B, "        classpath 'com.android.tools.build:gradle:9.0.1'\n", 1),
 ('E4.1', B, "        classpath 'com.google.dagger:hilt-android-gradle-plugin:2.59.2'\n", 1),
 ('E4.2', A, "    implementation 'com.google.dagger:hilt-android:2.59.2'\n", 1),
 ('E4.3', A, "    ksp 'com.google.dagger:hilt-compiler:2.59.2'\n", 1),
 ('E4.4', A, "    testImplementation 'com.google.dagger:hilt-android-testing:2.59.2'\n", 1),
 ('E4.5', A, "    kspTest 'com.google.dagger:hilt-compiler:2.59.2'\n", 1),
 ('E5.1', B, "        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21'\n", 1),
 ('E5.2', A, "    implementation 'org.jetbrains.kotlin:kotlin-stdlib:2.3.21'\n", 1),
 ('E6.1', A, "    implementation 'androidx.hilt:hilt-work:1.2.0'\n", 1),
 ('E6.2', A, "    ksp 'androidx.hilt:hilt-compiler:1.2.0'\n", 1),
 ('E7.1', A, "'org.robolectric:robolectric:4.15.1'", 1),
 ('E7.2', A, "// U-005: The --add-opens jvmArgs block that was added as a stopgap for Robolectric 4.3.1\n"
             "// incompatibility with JDK 17 has been removed. Robolectric 4.15.x handles JDK 17-25\n"
             "// natively and no longer requires these JVM flags (re-verified on JDK 25 in U-060).\n", 1),
 ('E7.3-anchor', A, "            includeAndroidResources = true\n", 1),
 ('E8', A, "'org.mockito:mockito-core:5.18.0'", 1),
 ('E9', A, '"com.google.truth:truth:1.4.4"', 1),
 ('E10', A, "'com.icegreen:greenmail-junit4:2.1.3'", 1),
 ('E11', A, "'com.google.auto.service:auto-service:1.0'", 1),
 ('E12', A, 'toolVersion = "0.8.14"', 1),
 ('E13.1', C, "        uses: actions/checkout@v4\n", 1),
 ('E13.2', C, "        uses: actions/setup-java@v4\n", 1),
 ('E13.3', C, "        uses: actions/cache@v4\n", 2),
 ('E13.4', C, "      # Gradle 9.5 / AGP 9.0 run on JDK 17+; this project builds and tests on JDK 25, declared in\n"
              "      # gradle/gradle-daemon-jvm.properties. AGP 9.0 requires SDK Build Tools >= 36.0.0.\n", 1),
 ('E13.5', C, "      # AGP 9.0 needs Build Tools 36.0.0 (the runner image ships it today; this guards against image drift).\n", 1),
 ('E14.1', B, "        // U-060: Gradle 9.5.1 / AGP 9.0.1 / KGP 2.3.21 run the build on JDK 25 (app bytecode stays 17).\n"
              "        // AGP 9.0 needs Gradle >= 9.1.0, JDK >= 17, KGP >= 2.2.10, SDK Build Tools >= 36.0.0\n"
              "        // (developer.android.com/build/releases/agp-9-0-0-release-notes).\n", 1),
 ('E14.2', B, "        // U-060: KGP 2.3.21 (AGP 9 built-in Kotlin needs >= 2.2.10; declared here so a KGP newer than\n"
              "        // AGP's runtime 2.2.10 is used). 2.4.x is excluded: Hilt 2.59.2 cannot read Kotlin 2.4 metadata.\n", 1),
 ('E14.3', B, "        // U-057/U-060: KSP replaces kapt for Hilt. KSP 2.3.x is KSP2-only and the first line (2.3.1+)\n"
              "        // with AGP 9 built-in Kotlin support; 2.3.6 includes the Windows KSP2 recompilation fix.\n", 1),
 ('E14.4', B, "        // U-022/U-060: Hilt Gradle plugin (must precede app/build.gradle application of\n", 1),
 ('E14.5', B, "        // dependencies in app/build.gradle. 2.59.x is the first Hilt line with AGP 9 support.\n", 1),
 ('E14.6', A, "// U-060: the standalone Kotlin Android plugin application was removed - AGP 9 provides built-in Kotlin (KGP 2.3.21 on the root classpath).\n", 1),
 ('E14.7', A, "// U-060: KSP 2.3.6 (KSP2-only) matches the Kotlin 2.3.x line and supports AGP 9 built-in Kotlin.\n"
              "// Hilt 2.59.2 supports KSP for mixed Java+Kotlin source modules and requires AGP 9.\n", 1),
 ('E14.8', A, "    // U-057/U-060: kotlin-stdlib matches the Kotlin Gradle plugin (2.3.21).\n", 1),
 ('E14.9', A, "' // U-060: ASM 9.8 reads Java 25 class files. U-068: no test is configured below the minSdk of 24.\n", 1),
 ('E14.10', A, "' // U-060: bundles ByteBuddy 1.17.5 (Java 25)\n", 1),
 ('E14.11', A, "    // U-060: Hilt 2.59.2 (plugin, runtime, compiler, testing all identical) - AGP 9 + Kotlin 2.3.x + KSP2.\n", 1),
 ('E14.12', A, '" // U-060: first release with official Java 25 support\n', 1),
 ('E14.13', L, "| Build system | Gradle 9.5.1 (wrapper: `./gradlew`), running on **JDK 25** (see Build Environment) |\n", 1),
 ('E14.14', L, "| Android Gradle Plugin | 9.0.1 (`com.android.application`, built-in Kotlin); Kotlin 2.3.21, KSP 2.3.6, Hilt 2.59.2 |\n", 1),
 ('E14.15', L, "| Build tools | 36.0.0+ (AGP 9.0 minimum) |\n", 1),
 ('M-1', BW, "                " + CHK + "\n", 1),
 ('M-2', RW, "                    " + CHK + "\n\n                    val item = msgs[currentRestoredItem]\n", 1),
 ('M-3', RW, "                    " + CHK + "\n\n                    val values = items[currentRestoredItem]\n", 1),
 ('R9.1', S + 'activity/MainViewModel.kt', "    @ApplicationContext private val context: Context", 1),
 ('R9.2', S + 'scheduler/WorkManagerScheduler.kt', "    @ApplicationContext private val context: Context", 1),
 ('R9.3', S + 'service/SharedPreferencesCheckpointStore.kt', "    @ApplicationContext private val context: Context", 1),
 ('R9.4', S + 'service/WorkerEngineFactory.kt', "    @ApplicationContext private val context: Context", 1),
]
bad = 0
for i, f, old, want in E:
    text = open(os.path.join(root, f), encoding='utf-8', newline='').read().replace('\r\n', '\n')
    n = text.count(old)
    bad += n != want
    print(f"{'OK ' if n == want else 'BAD'} {i} {f} count={n} expected={want}")
print('ALL OK' if not bad else f'MISMATCH {bad}')
sys.exit(1 if bad else 0)
```

`gradlew_block.py`:
```python
"""Check the gradlew LOCAL ADDITION block after a wrapper regeneration (U-071 S2-I1, story AC-6 (b)).
usage: python -I -X utf8 gradlew_block.py <start gradlew> <generated gradlew> <final gradlew>
Passes (exit 0) only if: all three files are LF-only; the start file's lines 117-127 are the block (first line
starts '# >>> LOCAL ADDITION', last is '# <<< END LOCAL ADDITION <<<') with line 116 blank; the generated file
has no block and still defines cygwin=false and msys=false; and the final file equals the generated file
with one blank line and the block inserted directly after the first 'esac' line that follows the
'case "$( uname )" in' line. Prints the block's SHA-256 (equal to `sed -n 117,127p <start> | sha256sum`)."""
import hashlib, sys
def read(p):
    b = open(p, 'rb').read()
    return b, b.split(b'\n')
(sb, s), (gb, g), (fb, f) = read(sys.argv[1]), read(sys.argv[2]), read(sys.argv[3])
ok = True
def check(cond, msg):
    global ok
    ok = ok and bool(cond)
    print(('OK   ' if cond else 'FAIL ') + msg)
check(not any(b'\r' in x for x in (sb, gb, fb)), 'LF-only line endings in the three files')
block = s[116:127]
check(len(block) == 11 and block[0].startswith(b'# >>> LOCAL ADDITION') and block[-1] == b'# <<< END LOCAL ADDITION <<<',
      'start lines 117-127 are the LOCAL ADDITION block')
check(s[115] == b'', 'start line 116 is blank')
check(not any(x.startswith(b'# >>> LOCAL ADDITION') for x in g), 'generated file carries no block')
check(b'cygwin=false' in g and b'msys=false' in g, 'generated file defines cygwin=false and msys=false')
c = next((i for i, x in enumerate(g) if x.startswith(b'case "$( uname )" in')), None)
e = None if c is None else next((i for i in range(c, len(g)) if g[i] == b'esac'), None)
check(e is not None, 'generated file has the uname case block and its esac')
if e is not None:
    check(f == g[:e + 1] + [b''] + block + g[e + 1:],
          'final = generated + one blank line + the block after the esac at generated line %d' % (e + 1))
print('block sha256', hashlib.sha256(b'\n'.join(block) + b'\n').hexdigest())
sys.exit(0 if ok else 1)
```

`vm_proof.py`:
```python
"""Additions-only proof for gradle/verification-metadata.xml. Lines marked U-060 are the U-060 plan Step 6.3
script, unchanged; U-071 adds the list of added components and their groups (design D5 step 5).
usage: python -I -X utf8 vm_proof.py <old.xml> <new.xml> <added-components-out>
Exit 0 only if the configuration block is unchanged, 0 entries are removed or altered and more than 0 added."""
import collections, sys, xml.etree.ElementTree as ET
NS = '{https://schema.gradle.org/dependency-verification}'                                   # U-060
def load(p):                                                                                  # U-060
    r = ET.parse(p).getroot(); s = set()                                                      # U-060
    for c in r.iter(NS + 'component'):                                                        # U-060
        for a in c.findall(NS + 'artifact'):                                                  # U-060
            for h in a: s.add((c.get('group'), c.get('name'), c.get('version'), a.get('name'), h.tag[len(NS):], h.get('value')))  # U-060
    return s, ET.tostring(r.find(NS + 'configuration'))                                       # U-060
old, oc = load(sys.argv[1]); new, nc = load(sys.argv[2])                                      # U-060
print('configuration block unchanged:', oc == nc)                                             # U-060
print('removed-or-altered entries:', len(old - new)); [print('  -', e) for e in sorted(old - new)]  # U-060
print('added entries:', len(new - old))                                                       # U-060
comps = sorted({e[:3] for e in new - old})
open(sys.argv[3], 'w', encoding='utf-8').write(''.join(f'{g}:{n}:{v}\n' for g, n, v in comps))
print('added components:', len(comps))
for g, k in sorted(collections.Counter(c[0] for c in comps).items()):
    print(f'  group {g}: {k}')
sys.exit(0 if (oc == nc and not (old - new) and (new - old)) else 1)
```

`deps_resolved.py`:
```python
"""Resolved coordinates of one configuration from `gradlew :app:dependencies` text output (U-071, AC-17 (e), IC-1).
usage: python -I -X utf8 deps_resolved.py <dependencies.log> <configuration> <coords-out> <tree-out>
Writes the configuration's tree lines to <tree-out>, and to <coords-out> the sorted unique resolved
'group:artifact:version' (for 'a:b:1 -> 2' the 2; for 'a:b:1 -> c:d:3' the c:d:3; 'project :x' as is).
Constraint lines '(c)' and not-resolved lines '(n)' are skipped; '(*)' repeats count once.
Prints 'TOTAL <n> MULTI <k>' where MULTI counts group:artifact pairs resolved at more than one version.
Exit 1 if the configuration is not found or empty."""
import collections, re, sys
log, conf, coords_out, tree_out = sys.argv[1:5]
tree, coords, inside = [], set(), False
pat = re.compile(r'^[| ]*[+\\]--- (.+?)(?: \((c|n|\*)\))?$')
for raw in open(log, encoding='utf-8', errors='replace'):
    line = raw.rstrip('\r\n')
    if not inside:
        inside = line.startswith(conf + ' - ')
        if inside:
            tree.append(line)
        continue
    if line.strip() == '':
        break
    tree.append(line)
    m = pat.match(line)
    if not m or m.group(2) in ('c', 'n'):
        continue
    body = m.group(1)
    if body.startswith('project '):
        coords.add(body.split(' (')[0])
        continue
    left, _, right = body.partition(' -> ')
    g = left.split(':')
    if len(g) < 2:
        continue
    if right and ':' in right:
        r = right.split(' ')[0].split(':')
        coords.add(f'{r[0]}:{r[1]}:{r[2] if len(r) > 2 else "?"}')
    else:
        ver = right.split(' ')[0] if right else (g[2].split(' ')[0] if len(g) > 2 else '?')
        coords.add(f'{g[0]}:{g[1]}:{ver}')
open(tree_out, 'w', encoding='utf-8').write('\n'.join(tree) + '\n')
open(coords_out, 'w', encoding='utf-8').write(''.join(c + '\n' for c in sorted(coords)))
multi = [k for k, n in collections.Counter(c.rsplit(':', 1)[0] for c in coords if not c.startswith('project ')).items() if n > 1]
print(f'TOTAL {len(coords)} MULTI {len(multi)}' + ''.join(f'\nMULTI {k}' for k in sorted(multi)))
sys.exit(0 if coords else 1)
```

`cov_floors.py`:
```python
"""Per-package LINE coverage against the configured JaCoCo floors, with spare lines (U-071, design D8).
usage: python -I -X utf8 cov_floors.py <app/build.gradle> <jacocoTestReport.xml>
Floors are read from the violationRules block: each "includes = ['<pattern>']" with the next "minimum = <f>".
A package is checked against every rule whose pattern matches its dotted name ('*' matches any characters).
spare = covered - ceil(f * total), computed exactly (fractions); if JaCoCo rounds the ratio to the floor's
decimals, that can only make a pass easier, so 'spare' is the conservative margin. One line per (package,
rule), then 'RULES <n> MIN-SPARE <m>'. Exit 1 if a package is below a floor or no rule was read."""
import math, re, sys
import xml.etree.ElementTree as ET
from fractions import Fraction
text = open(sys.argv[1], encoding='utf-8').read()
block = text[text.index('violationRules'):]
found = re.findall(r"includes = \['([^']+)'\][\s\S]*?minimum = ([0-9.]+)", block)
rules = [(p, Fraction(f), re.compile('^' + re.escape(p).replace(r'\*', '.*') + '$')) for p, f in found]
root = ET.parse(sys.argv[2]).getroot()
bad, low = False, None
for pkg in sorted(root.findall('package'), key=lambda x: x.get('name')):
    name = pkg.get('name').replace('/', '.')
    line = next((c for c in pkg.findall('counter') if c.get('type') == 'LINE'), None)
    cov = int(line.get('covered')) if line is not None else 0
    tot = cov + (int(line.get('missed')) if line is not None else 0)
    for pat, floor, rx in rules:
        if not rx.match(name):
            continue
        spare = cov - math.ceil(floor * tot)
        ok = tot == 0 or Fraction(cov, tot) >= floor
        bad = bad or not ok
        low = spare if low is None else min(low, spare)
        print(f"{name} rule={pat} floor={float(floor):.2f} {cov}/{tot} "
              f"ratio={(cov / tot if tot else 0):.4f} spare={spare} {'OK' if ok else 'BELOW'}")
print(f'RULES {len(rules)} MIN-SPARE {low}')
sys.exit(1 if bad or not rules else 0)
```

`jacoco_class_lines.py`:
```python
"""Per-class LINE counters of a JaCoCo XML report (U-071; class-level companion of U-060's inventory).
usage: python -I -X utf8 jacoco_class_lines.py <jacocoTestReport.xml>
Prints '<package/Class> covered/total' for every class with a LINE counter, then 'NOCOUNTER <package/Class>'
for classes without one (interfaces, generated classes JaCoCo filters), each group sorted.
Exit 1 if the report lists no class."""
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
rows, none = [], []
for pkg in root.iter('package'):
    for cls in pkg.findall('class'):
        c = next((x for x in cls.findall('counter') if x.get('type') == 'LINE'), None)
        if c is None:
            none.append(f"NOCOUNTER {cls.get('name')}")
        else:
            cov, mis = int(c.get('covered')), int(c.get('missed'))
            rows.append(f"{cls.get('name')} {cov}/{cov + mis}")
for r in sorted(rows) + sorted(none):
    print(r)
sys.exit(0 if rows or none else 1)
```

`scan.py`:
```python
"""Fail-closed hygiene scan of evidence files before a commit (the detection rules of U-063's sanitize.py, IC-8),
one process for many files. usage (from the repository root): python -I -X utf8 scan.py <file> [<file> ...]
Flags the repository root, the home directory, the user name, the host name, a profile path, the
application-data folder name and e-mail-like tokens. Prints 'OK <file>' or 'HIT <file>:<line numbers>'
(never the text). Exit 1 on any hit. The folder-name pattern is assembled from parts so this file passes."""
import os, re, socket, sys
def forms(path):
    if not path:
        return []
    fwd = path.replace('\\', '/')
    out = {path, fwd, fwd.replace('/', '\\')}
    m = re.match(r'^([A-Za-z]):/(.*)$', fwd)
    if m:
        out.add('/' + m.group(1).lower() + '/' + m.group(2))
    return list(out)
tokens = forms(os.getcwd()) + forms(os.path.expanduser('~'))
tokens += [os.environ.get('USERNAME') or os.environ.get('USER') or '', socket.gethostname(), os.environ.get('COMPUTERNAME') or '']
tokens = [t.lower() for t in tokens if len(t) >= 3]
bad = re.compile('Users[/\\\\]|' + 'App' + 'Data' + r'|[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+\.[A-Za-z]{2,}', re.I)
rc = 0
for path in sys.argv[1:]:
    hits = [n for n, line in enumerate(open(path, encoding='utf-8', errors='replace'), 1)
            if bad.search(line) or any(t in line.lower() for t in tokens)]
    if hits:
        rc = 1
        print('HIT ' + path + ':' + ','.join(map(str, hits[:20])))
    else:
        print('OK ' + path)
sys.exit(rc)
```

`U071ProviderProbeTest.java.txt` (copied to `$PROBE` only under 4.P):
```java
package com.zegoggles.smssync.mail.ssl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

/**
 * U-071 THROWAWAY environment probe (the U-063 IC-11 form): prints U071-PROBE lines, never fails.
 * Copied in for one targeted run at S2-I0 and one at S2-I7, deleted right after each; never committed.
 */
@RunWith(RobolectricTestRunner.class)
public class U071ProviderProbeTest {

    private static void print(String key, Object value) {
        System.out.println("U071-PROBE " + key + "=" + value);
    }

    @Test
    public void probe() throws Exception {
        print("sdk_int", android.os.Build.VERSION.SDK_INT);
        List<String> providers = new ArrayList<>();
        for (Provider p : Security.getProviders()) {
            providers.add(p.getName() + ":" + p.getVersionStr());
        }
        print("providers", providers);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        print("tls_provider", context.getProvider().getName() + ":" + context.getProvider().getVersionStr());
        print("default_protocols", Arrays.toString(context.getDefaultSSLParameters().getProtocols()));
        print("supported_protocols", Arrays.toString(context.getSupportedSSLParameters().getProtocols()));
        try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket()) {
            print("socket_enabled_protocols", Arrays.toString(socket.getEnabledProtocols()));
            print("socket_enabled_cipher_suites", Arrays.toString(socket.getEnabledCipherSuites()));
        }
        print("supported_cipher_suites_count", context.getSupportedSSLParameters().getCipherSuites().length);
    }
}
```

**Gradle calls.** Foreground, tool timeout 600000 ms, output to the work folder, exit appended. Never two at once.
- **G(k)** gate: `rm -rf $R; ./gradlew --console=plain --warning-mode all clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease > $WORK/logs/I<k>-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I<k>-gates.log`
  (the four gates plus `clean` and the report task that writes the XML; `--warning-mode all` is AC-18 (c)'s record).
- **WP(k)** writer pass, the design's exact command (D5 step 3): `./gradlew --write-verification-metadata sha256 --continue clean help test lint jacocoTestReport assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification > $WORK/logs/I<k>-writer.log 2>&1; echo "exit=$?" >> $WORK/logs/I<k>-writer.log`
- **ENF(k)** enforced re-run: `./gradlew --console=plain :app:assembleDebug > $WORK/logs/I<k>-enforced.log 2>&1; echo "exit=$?" >> …`
- **KC(k)** forced full Kotlin compile (U-067 form, no init script): `./gradlew --console=plain :app:compileDebugKotlin --rerun --no-build-cache > $WORK/logs/I<k>-kotlinc.log 2>&1; echo "exit=$?" >> …`
- **TR(name, fqcn…)** targeted: `rm -rf $R; ./gradlew --console=plain :app:testDebugUnitTest --tests "<fqcn>" [--tests …] > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> …`
- **L(k)** lint: `./gradlew --console=plain :app:lintDebug > $WORK/logs/I<k>-lint.log 2>&1; echo "exit=$?" >> …` (expected `exit=1`, pre-existing, D17)
- **DEP(k)**: `./gradlew --console=plain :app:dependencies > $WORK/logs/I<k>-deps.log 2>&1; echo "exit=$?" >> …`
- **VER(name)**: `./gradlew --version > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> …`

**Filters for `sanitize.py`** (`python -I -X utf8 $T/sanitize.py <in> <out> --grep '<regex>'`):
- gate **G**: `'^(BUILD |FAILURE|> Task :app:|exit=)|tests completed|FAILED$'`
- warnings **WR**: `'^w: |^e: |[Ww]arning|WARNING|[Dd]eprecat|scheduled to be removed|will fail with an error|is not supported|error:'`
- writer **WF**: `'^(BUILD |FAILURE|exit=)|> Task .*FAILED|[Vv]erification'`
- lint **LF**: `'^(BUILD |FAILURE|exit=)|Lint found|listed in the baseline file|First failure|Wrote .*report'`
- Kotlin **KF**: `'^(w|e): |> Task :app:compileDebugKotlin|^BUILD |exit='`
- version **VF**: none (whole file)

**Error-class items** (AC-18 (c), D3 "no error-class item"): a line matching `^e: `, `error:`, `FAILURE:`, `BUILD FAILED`
or `> Task .* FAILED` in a gate log. Gradle's "This will fail with an error in Gradle 10" lines are deprecations (debt),
counted separately.

**EV-T(k)** after a G(k) that exited 0:
```bash
python -I -X utf8 $T/sanitize.py $WORK/logs/I<k>-gates.log $E/I<k>-gates.txt --grep '<G>'
python -I -X utf8 $T/sanitize.py $WORK/logs/I<k>-gates.log $E/I<k>-warnings.txt --grep '<WR>'
python -I -X utf8 $T/junit_totals.py $R > $E/I<k>-junit-totals.txt
python -I -X utf8 $T/junit_cases.py $R > $WORK/logs/I<k>-cases.log; python -I -X utf8 $T/sanitize.py $WORK/logs/I<k>-cases.log $E/I<k>-cases.txt
cp $JX $WORK/I<k>-jacoco.xml
python -I -X utf8 $T/jacoco_pkgs.py $JX > $E/I<k>-jacoco-packages.txt
python -I -X utf8 $T68/jacoco_inventory.py $JX $E/I<k>-jacoco-inventory.json
python -I -X utf8 $TL/jacoco_class_lines.py $JX > $E/I<k>-jacoco-classes.txt
python -I -X utf8 $TL/cov_floors.py app/build.gradle $JX > $E/I<k>-coverage.txt; echo "cov-floors-exit=$?"
```

**EV-B(k)** (U-068 recipe plus the KSP list):
```bash
find app/build/generated/ksp -type f | sed 's#.*/generated/ksp/##' | LC_ALL=C sort > $E/I<k>-ksp-files.txt
MD=app/build/intermediates/merged_manifests
cp $MD/debug/processDebugManifest/AndroidManifest.xml $E/I<k>-merged-debug.xml
cp $MD/release/processReleaseManifest/AndroidManifest.xml $E/I<k>-merged-release.xml
"$AAPT2" dump xmltree --file AndroidManifest.xml app/build/outputs/apk/debug/app-debug.apk > $E/I<k>-manifest-debug.txt
"$AAPT2" dump xmltree --file AndroidManifest.xml app/build/outputs/apk/release/app-release-unsigned.apk > $E/I<k>-manifest-release.txt
MAP=app/build/outputs/mapping/release
grep -oE '^([A-Za-z0-9_.$]+) -> \1:' $MAP/mapping.txt | sed 's/ -> .*//' | grep -E '^(com\.zegoggles\.smssync|com\.fsck\.k9|com\.google\.crypto\.tink|androidx\.work)\.' | LC_ALL=C sort > $E/I<k>-kept-names.txt
python -I -X utf8 $T/sanitize.py $MAP/configuration.txt $E/I<k>-configuration.txt
for a in debug/app-debug release/app-release-unsigned; do unzip -Z1 app/build/outputs/apk/$a.apk | LC_ALL=C sort > $E/I<k>-apk-entries-${a%%/*}.txt; done
{ for a in debug/app-debug release/app-release-unsigned; do f=app/build/outputs/apk/$a.apk
    echo "$a entries=$(unzip -Z1 $f | wc -l) so=$(unzip -Z1 $f | grep -c '\.so$') dex=$(unzip -Z1 $f | grep -c '^classes[0-9]*\.dex$') size=$(stat -c %s $f) sha256=$(sha256sum $f | cut -d' ' -f1)"; done
  echo "mapping-lines=$(wc -l < $MAP/mapping.txt) kept-names=$(wc -l < $E/I<k>-kept-names.txt)"
  for c in App activity.MainActivity receiver.SmsBroadcastReceiver receiver.BootReceiver receiver.PackageReplacedReceiver receiver.BackupBroadcastReceiver service.BackupWorker service.RestoreWorker worker.BackupTriggerWorker; do
    echo "self-mapped com.zegoggles.smssync.$c=$(grep -c "^com\.zegoggles\.smssync\.${c//./\\.} -> com\.zegoggles\.smssync\.${c//./\\.}:" $MAP/mapping.txt)"; done
} > $E/I<k>-apk.txt
{ for c in "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/zegoggles/smssync/App.class" \
           "app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes/com/zegoggles/smssync/service/BackupWorker.class" \
           "k9mail-vendored/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/fsck/k9/mail/Address.class"; do
    echo "$c $("$JDKBIN/javap.exe" -v "$c" | grep 'major version')"; done } > $E/I<k>-javap.txt
```
Expected: `so=0` for both APKs; nine `self-mapped …=1`; `major version: 61`, `61`, `52`. If AGP 9.5.2 moves a class
directory (row D55), the javap path follows and the log says so.

**CMP(k)** (k ≥ 1, one call, into `$E/I<k>-compare.txt`; `norm` is U-061's packaged-manifest normaliser):
```bash
norm() { sed -E 's/ ?\(line=[0-9]+\)//g; s/@0x7f[0-9a-f]{6}/@0x7fRESID/g' "$1"; }
{ for f in merged-debug.xml merged-release.xml kept-names.txt configuration.txt apk-entries-debug.txt apk-entries-release.txt javap.txt ksp-files.txt cases.txt jacoco-inventory.json jacoco-classes.txt; do
    for b in I0 <p>; do git -c core.autocrlf=false diff --no-index --quiet -- $E/$b-$f $E/I<k>-$f; r=$?; echo "$f vs $b exit=$r"
      [ $r -ne 0 ] && git -c core.autocrlf=false diff --no-index --numstat -- $E/$b-$f $E/I<k>-$f | cut -f1,2; done; done
  for a in debug release; do norm $E/I<k>-manifest-$a.txt > $WORK/n-I<k>-$a.txt
    for b in I0 <p>; do norm $E/$b-manifest-$a.txt > $WORK/n-$b-$a.txt
      git -c core.autocrlf=false diff --no-index --quiet -- $WORK/n-$b-$a.txt $WORK/n-I<k>-$a.txt; echo "manifest-$a(norm) vs $b exit=$?"; done; done
  for b in I0 <p> I<k>; do sed 's/^files=[0-9]* //' $E/$b-junit-totals.txt > $WORK/t-$b.txt; done
  for b in I0 <p>; do git -c core.autocrlf=false diff --no-index --quiet -- $WORK/t-$b.txt $WORK/t-I<k>.txt; echo "totals vs $b exit=$?"; done
  for a in debug release; do F=$E/I<k>-manifest-$a.txt
    echo "$a fgs=$(grep -A4 'android:name(0x01010003)="androidx.work.impl.foreground.SystemForegroundService"' $F | grep -c 'foregroundServiceType(0x01010599)=0x00000001') init=$(grep -c InitializationProvider $F) bbr-exported=$(grep -A2 'android:name(0x01010003)="com.zegoggles.smssync.receiver.BackupBroadcastReceiver"' $F | grep -c 'exported(0x01010010)=true') bbr-action=$(grep -A5 'android:name(0x01010003)="com.zegoggles.smssync.receiver.BackupBroadcastReceiver"' $F | grep -c '"com.zegoggles.smssync.BACKUP"') minsdk=$(grep -c 'minSdkVersion(0x0101020c)=24' $F) merged-minsdk=$(grep -c 'android:minSdkVersion="24"' $E/I<k>-merged-$a.xml)"; done
  grep -oE '(entries|so|dex)=[0-9]+' $E/I<k>-apk.txt | paste -sd' '; grep -c 'self-mapped .*=1$' $E/I<k>-apk.txt
} > $E/I<k>-compare.txt
```
Expected (2.9): every `exit=0` except the cells 2.9 allows; the invariant line `fgs=1 init=0 bbr-exported=1
bbr-action=1 minsdk=1 merged-minsdk=1` for both APKs; `so=0` twice; `9` self-mapped. For each `exit=1` the developer
saves the full `git diff --no-index` into the work folder and writes each differing line, classified, into the log
(sanitised). The `jacoco-inventory.json` and `jacoco-classes` comparisons may differ in `activity` covered counts
(the known alternation, 2.6) without further investigation; any other difference in them is classified by 2.9.

**CK(k)** increment checks, one call into `$E/I<k>-checks.txt` (k = 1 … 14; the increment adds its own lines):
```bash
git diff --numstat HEAD -- build.gradle app/build.gradle .github/workflows/ci.yml gradle/wrapper gradlew gradlew.bat sdlc/artifacts/engagement/code-location.md
git diff -U0 HEAD -- build.gradle app/build.gradle | grep -E '^[-+][^-+]'
git diff --stat HEAD -- app/src/main app/src/test k9mail-vendored gradle/gradle-daemon-jvm.properties gradle.properties settings.gradle app/lint.xml app/lint-baseline.xml app/proguard-rules.pro app/src/test/resources/robolectric.properties; echo "must-not-change-end"
git status --porcelain -- app/src/test; echo "test-porcelain-end"
test -e $PROBE; echo "probe-present-exit=$?"
```
Expected: the numstat of section 3 for this increment; the `-`/`+` lines are exactly the increment's edits; nothing
before `must-not-change-end` (a forced, pre-specified `app/src/main` edit is the only exception, rows D55 to D58);
nothing before `test-porcelain-end`; `probe-present-exit=1`.

**VMP(k)** — the D5 protocol, for every increment I2 to I12 (and I1 only under row D25):
1. `git show HEAD:gradle/verification-metadata.xml > $WORK/vm-I<k>-old.xml; git restore --source=HEAD --worktree -- gradle/verification-metadata.xml; git diff --quiet HEAD -- gradle/verification-metadata.xml; echo "vm-pristine-exit=$?"` → `0`. HEAD is the previous increment's commit; exploratory writer output is gone.
2. **WP(k)**. Expected `exit=1` with exactly one failed task: `grep -E '^> Task .* FAILED$' $WORK/logs/I<k>-writer.log` → `> Task :app:lintDebug FAILED` (pre-existing, `--continue` covers it, D5 step 3). Sanitise with **WF** to `$E/I<k>-writer.txt`.
3. **Proof**: `python -I -X utf8 $TL/vm_proof.py $WORK/vm-I<k>-old.xml gradle/verification-metadata.xml $E/I<k>-vm-added.txt > $WORK/logs/I<k>-vm-proof.log; echo "proof-exit=$?" >> $WORK/logs/I<k>-vm-proof.log; { git diff --numstat HEAD -- gradle/verification-metadata.xml; echo "components=$(grep -c '<component ' gradle/verification-metadata.xml) artifacts=$(grep -c '<artifact ' gradle/verification-metadata.xml) trust=$(grep -c '<trust' gradle/verification-metadata.xml)"; } >> $WORK/logs/I<k>-vm-proof.log`, sanitised to `$E/I<k>-vm-proof.txt`. Pass: `configuration block unchanged: True`, `removed-or-altered entries: 0`, `added entries:` above 0, `proof-exit=0`, `trust=3`; "additions only" is proven by the set comparison, **not** by a text diff; the numstat (expected `<n>	0`) is the text review: a non-zero deletion count can only come from re-serialisation and the proof arbitrates (D5 "The arbiter").
4. **Group review**: read `$E/I<k>-vm-added.txt` and the group counts; write into the log, per group, the version change or transitive closure it belongs to, against the expected groups: I2 `com.google.devtools.ksp` (and any `org.jetbrains.kotlin` buildscript artifacts KSP 2.3.14 pulls); I3 `com.android.tools*` (build, lint, ddms, layoutlib, analytics, utp, `aapt2` with its Windows classifier, `r8`), `com.google.protobuf`, `io.grpc`, `io.netty`, `com.google.guava`, `org.bouncycastle`, `org.jetbrains.kotlin` POMs AGP declares; I4 `com.google.dagger` and its `kotlin-metadata`/`javapoet`/`kotlinpoet` closure; I5 `org.jetbrains.kotlin` (plugin, compiler, build tools, stdlib 2.4.21), `org.jetbrains.kotlinx` artifacts of the compiler classpath; I6 `androidx.hilt`, `androidx.room` (`room-compiler-processing`) and its closure; I7 `org.robolectric`, `org.ow2.asm`, `androidx.test`, `org.conscrypt`, `org.bouncycastle`; I8 `org.mockito`, `net.bytebuddy`, `org.objenesis`; I9 `com.google.truth` and its `guava`/`checker-qual`/`error_prone` closure; I10 `com.icegreen`, `org.eclipse.angus`, `jakarta.*`, `org.slf4j`; I11 `com.google.auto.service`, `com.google.auto`; I12 `org.jacoco`, `org.ow2.asm`. Anything else is flagged in the log (row D30), not deleted.
5. **ENF(k)**: `exit=0`; sanitised with **G** to `$E/I<k>-enforced.txt`. Then G(k) (the "full gates" of D5 step 6).

Stale entries are kept; nothing is hand-edited or regenerated wholesale; writer mode is never left on in a commit.

**Commit procedure CM(k)** (the ruled exception to "the developer commits nothing"):
1. `git diff --cached --quiet; echo "index-clean-exit=$?"` → `0` (row D86).
2. `ls -- <the list of 4.9> > /dev/null; echo "files-exist-exit=$?"` → `0` (row D87).
3. `python -I -X utf8 $TL/scan.py <every file of the list under $W>; echo "scan-exit=$?"` → `0`, output saved to `$WORK/logs/I<k>-scan.log` (row D88).
4. `git add -- <the same list>` (every path written out or brace-expanded by bash to explicit paths; never `-A`, `.`, `-u` or a bare directory).
5. `git diff --cached --name-only | LC_ALL=C sort > $WORK/logs/I<k>-staged.txt; printf '%s\n' <the same list> | LC_ALL=C sort > $WORK/logs/I<k>-list.txt; git -c core.autocrlf=false diff --no-index --quiet -- $WORK/logs/I<k>-list.txt $WORK/logs/I<k>-staged.txt; echo "staged-equal-exit=$?"` → `0`. For I1 also `git diff --cached --summary -- gradlew` → no `mode change` line.
6. Message with the Write tool into `$WORK/msg-I<k>.txt`: the subject of 4.9, a blank line, two to four body lines (the change, the "must hold" result, "Plan: sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md, S2-I<k>"), a blank line, and the attribution trailer the developer's environment prescribes. Then `git commit -F "$WORK/msg-I<k>.txt"`. Never `--amend`, `-a`, `--no-verify`, `push`, `rebase`, `reset`, `revert`.
7. `git rev-parse HEAD` → recorded at the top of the next increment's log entry ("I<k> commit: <id>").

**Timestamps** come from `date -u +%Y-%m-%dT%H:%M:%SZ` (dates `date -u +%F`), run at that moment, never typed.

### 4.P Probe protocol (throwaway provider probe; I0 step 10 and I7 step 8)

1. `test -e $PROBE; echo "probe-absent-exit=$?"` → `1`; `git status --porcelain -- app/src/test` → empty.
2. Write-ahead line in the log: `PROBE APPLYING <timestamp> (copy of $TL/U071ProviderProbeTest.java.txt to $PROBE)`.
3. `cp $TL/U071ProviderProbeTest.java.txt $PROBE`.
4. TR(`I<k>-probe`, `com.zegoggles.smssync.mail.ssl.U071ProviderProbeTest`); then
   `grep -ho 'U071-PROBE [^<]*' $R/TEST-com.zegoggles.smssync.mail.ssl.U071ProviderProbeTest.xml > $WORK/logs/I<k>-probe-lines.log; python -I -X utf8 $T/sanitize.py $WORK/logs/I<k>-probe-lines.log $E/I<k>-probe.txt`.
5. `rm -- $PROBE`, whatever the result; `test -e $PROBE; echo "probe-absent-exit=$?"` → `1`; `git status --porcelain -- app/src/test` → empty; log line `PROBE removed <timestamp> absent-ok test-porcelain-ok`.

No gate, writer pass, commit or count runs while the probe is present. Recovery: a session that finds `$PROBE` present
deletes it first and records that. The probe runs after the increment's EV-T and EV-B (its targeted run deletes `$R`).
Expected lines: `sdk_int`, `providers`, `tls_provider`, `default_protocols`, `supported_protocols`,
`socket_enabled_protocols`, `socket_enabled_cipher_suites`, `supported_cipher_suites_count` (8 lines; row D12).

### 4.R Resume protocol (start of every developer session after the first)

One call, into `$WORK/logs/resume-<timestamp>.log` (not evidence), before any other step:
```bash
git rev-parse HEAD; git log --oneline -3; git log --format='%h %s' --grep='^U-071 S2-' $START..HEAD
git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat .github/workflows sdlc/artifacts/engagement/code-location.md; echo "porcelain-end"
git diff --quiet HEAD -- gradle/verification-metadata.xml; echo "vm-clean-exit=$?"
grep -c 'LOCAL ADDITION' gradlew
test -e $PROBE; echo "probe-present-exit=$?"
grep -c '^### M-.* APPLYING' $E/mutation-log.md 2>/dev/null; grep -c '^- restored:' $E/mutation-log.md 2>/dev/null
"$JDKBIN/jps.exe" -l | grep -i gradle; echo "jps-gradle-end"
```
Expected at a clean hand-over: HEAD = the last commit the log names; nothing before `porcelain-end`;
`vm-clean-exit=0`; `2`; `probe-present-exit=1`; the two mutation counts equal (or the file absent); no Gradle JVM. Then
VER(`resume-version`) shows the wrapper version the log expects and the Daemon JVM line. Any difference: stop and report
(row D90), except the documented recoveries: a present probe is deleted (4.P); an `APPLYING` mutation without
`restored` is recovered by 4.M's recovery note; an interrupted writer pass is never continued (VMP step 1 restores the
file and the pass is run again from the start); an interrupted wrapper step restores `gradle/wrapper`, `gradlew` and
`gradlew.bat` from HEAD (`git restore --source=HEAD --worktree -- gradle/wrapper gradlew gradlew.bat`) and restarts
I1 at step 2.

### I0 — Baseline at the start commit (no tracked file changes; AC-1 to AC-4; SC-1 to SC-9)

1. **Start state** (one call into `$WORK/logs/I0-start.log`, sanitised to `$E/I0-start.txt`):
   ```bash
   date -u +%Y-%m-%dT%H:%M:%SZ; git rev-parse HEAD; git rev-parse --abbrev-ref HEAD
   git merge-base --is-ancestor $PLAN_HEAD HEAD; echo "plan-head-ancestor-exit=$?"
   git log --oneline $PLAN_HEAD..HEAD
   git diff --numstat $PLAN_HEAD HEAD -- build.gradle app/build.gradle gradle gradlew gradlew.bat settings.gradle gradle.properties k9mail-vendored .github/workflows; echo "build-drift-end"
   git diff -U0 $PLAN_HEAD HEAD -- app/build.gradle | grep '^@@'; echo "app-hunks-end"
   git diff -U0 $PLAN_HEAD HEAD -- sdlc/artifacts/engagement/code-location.md | grep '^@@'; echo "codeloc-hunks-end"
   git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat .github/workflows sdlc/artifacts/engagement/code-location.md; echo "porcelain-end"
   for p in build.gradle app/build.gradle gradle/wrapper/gradle-wrapper.properties gradle/wrapper/gradle-wrapper.jar gradlew gradlew.bat gradle/gradle-daemon-jvm.properties .github/workflows/ci.yml gradle/verification-metadata.xml sdlc/artifacts/engagement/code-location.md $BW $RW; do echo "$(git rev-parse HEAD:$p) $(git hash-object $p) $p"; done
   git ls-files -s -- gradlew
   grep -n '^status' sdlc/artifacts/build/sprints/sprint-023/U-067-*.md sdlc/artifacts/build/sprints/sprint-024/U-068-*.md sdlc/artifacts/build/sprints/sprint-024/U-069-*.md sdlc/artifacts/build/sprints/sprint-024/U-070-*.md sdlc/artifacts/build/sprints/sprint-025/U-074-*.md sdlc/artifacts/build/change-records/CR-015-*.md sdlc/artifacts/build/change-records/CR-016-*.md
   git merge-base --is-ancestor c12a2788 HEAD; echo "u067-fix-ancestor-exit=$?"
   git log --format='%h %s' --grep='^U-068 S1-' --grep='^U-069' --grep='^U-070' --grep='^U-074' $U068_START..HEAD
   git diff --stat $U068_START HEAD -- gradle/verification-metadata.xml; echo "vm-since-u068-start-end"
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   git grep -nE '^\s*@Ignore(\s|\(|$)' HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt'
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   "$JDKBIN/jps.exe" -l | grep -i gradle; echo "jps-gradle-end"
   test -s $SUPP; echo "supplement-exit=$?"
   ```
   Expected: `plan-head-ancestor-exit=0`; the commits after PLAN_HEAD are U-068 I7, U-069's, U-070's, U-074's and
   orchestrator documents; before `build-drift-end` exactly `2	2	app/build.gradle`; app hunks `@@ -579 +579 @@` and
   `@@ -586 +586 @@`; code-location hunk `@@ -28 +28 @@` (U-068 E7.3); nothing before `porcelain-end`; each line's two
   hashes equal; `100755` for `gradlew`; every story `status: done`, both CRs `status: implemented`;
   `u067-fix-ancestor-exit=0`; nothing before `vm-since-u068-start-end` (SC-2); the count `C0` as printed; the `@Ignore`
   lines (2 expected: `AppTest.java:13`, `AuthPreferencesTest.java:1079`); `toolchainVersion=25`; no Gradle JVM;
   `supplement-exit=0`. Record `START=$(git rev-parse HEAD)`, `C0`, the skipped baseline (from step 6) and the
   commit ids of U-067 (`c12a2788`), U-068, U-069, U-070 and U-074 with the tests each added (from their logs) at the
   top of the log (SC-1, SC-9). **Rows:** D1 to D8.
2. `mkdir -p "$WORK/logs" "$WORK/pristine" "$M" "$TL"`; write the eight tools of 4.0 with the Write tool into `$TL/`;
   copy `<start>`'s block: `git show HEAD:gradlew | sed -n 117,127p > $WORK/gradlew-block.txt; sha256sum < $WORK/gradlew-block.txt`
   → `45364f3d…ea760` (L4); `git show HEAD:gradlew > $WORK/gradlew-start`.
3. `python -I -X utf8 $TL/oldtext_check.py > $WORK/logs/I0-oldtext.log; echo "exit=$?" >> $WORK/logs/I0-oldtext.log`,
   sanitised to `$E/I0-oldtext.txt`. Expected `ALL OK`, `exit=0`. **Row:** D6.
4. **Greps at `<start>`** (one call into `$E/I0-greps.txt`):
   ```bash
   grep -rn "LOLLIPOP\|sdk = \[\?2[0-3]" app/src/test; echo "sc2-grep-exit=$?"
   grep -n "minSdkVersion" app/build.gradle; grep -n "reports.junitXml.required = true" build.gradle
   test -e $TS/receiver/BackupBroadcastReceiverTest.java; echo "bbr-test-exit=$?"
   git grep -nE "9\.5\.1|9\.0\.1|2\.3\.21|2\.3\.6\b|2\.59\.2|4\.15\.1|0\.8\.14|5\.18\.0|1\.4\.4|2\.1\.3|checkout@v4|setup-java@v4|cache@v4" -- . ':!sdlc' ':!gradle/verification-metadata.xml'; echo "sweep-end"
   grep -nE "alpha|beta|rc[0-9]" build.gradle app/build.gradle k9mail-vendored/build.gradle; echo "prerelease-end"
   grep -n SNAPSHOT build.gradle app/build.gradle k9mail-vendored/build.gradle settings.gradle; echo "snapshot-exit=$?"
   git grep -n "TextLayoutMode\|shadows-httpclient\|org\.apache\.http" -- app/src/test app/build.gradle; echo "robo418-removed-api-exit=$?"
   git grep -c "ShadowLog" -- app/src/test; echo "shadowlog-end"
   git grep -nE "^[[:space:]]*org\.gradle\.java\.home[[:space:]]*=" -- '*.properties' '*.gradle' gradlew gradlew.bat .github; echo "java-home-set-exit=$?"
   grep -c '^ *<issue *$' app/lint-baseline.xml
   ```
   Expected: `sc2-grep-exit=1`; `:24 minSdkVersion 24`; `:30`; `bbr-test-exit=0`; the sweep equals L9 (with whatever
   U-069 to U-074 add, each explained); `prerelease-end` preceded by L10's four lines; `snapshot-exit=1`;
   `robo418-removed-api-exit=1`; the `ShadowLog` users (U-069's test expected) recorded; `java-home-set-exit=1` (the
   prose hits of L22 are not settings; SC-P5); the baseline count (26 expected after U-068 I6, measured). **Rows:** D3
   (the SC-2 checks), D10 (the baseline count is recorded, not gated).
5. **Verification counts** into `$E/I0-vm-counts.txt`: `git rev-parse HEAD:gradle/verification-metadata.xml`;
   `git show HEAD:gradle/verification-metadata.xml | sha256sum`; the three `grep -c` counts of VMP step 3 on the same
   `git show` output. Expected `803`, `1460`, `3` (L8; re-counted). **Row:** D11.
6. VER(`I0-gradle-version`) → `$E/I0-gradle-version.txt` (Gradle 9.5.1; "Daemon JVM: … (from
   gradle/gradle-daemon-jvm.properties)"). Then **G(0)**, EV-T(0), EV-B(0). Expected `exit=0`; `failures=0 errors=0`;
   the skipped count recorded as "the start" (2 expected); `cov-floors-exit=0`; the I0 coverage table of 2.6 filled
   from `I0-coverage.txt`. **Row:** D9.
7. **L(0)**; sanitise with **LF** into `$E/I0-lint.txt`; `python -I -X utf8 $T68/lint_ids.py app/build/reports/lint-results-debug.xml > $WORK/logs/I0-lint-ids.log`,
   sanitised to `$E/I0-lint-ids.txt`; `grep -c '^ *<issue *$' app/lint-baseline.xml` and the "listed in the baseline
   file but not found" count into `$E/I0-lint-baseline-count.txt`. Expected `exit=1`, the rule-ID table (U-068 I6's
   33 errors expected, re-measured: AC-1 (h)'s "before" figure). **Row:** D10.
8. **DEP(0)**; then `python -I -X utf8 $TL/deps_resolved.py $WORK/logs/I0-deps.log releaseRuntimeClasspath $WORK/I0-rel.txt $WORK/I0-rel-tree.txt`
   and `… debugUnitTestRuntimeClasspath $WORK/I0-test.txt $WORK/I0-test-tree.txt`; sanitise `$WORK/I0-rel.txt` →
   `$E/I0-deps-releaseRuntime.txt`, `$WORK/I0-rel-tree.txt` → `$E/I0-deps-releaseRuntime-tree.txt`, `$WORK/I0-test.txt`
   → `$E/I0-deps-debugUnitTestRuntime.txt`. Expected `MULTI 0` for both (AC-1 (k)).
9. `ls "$HOME/.m2/repository/org/robolectric/android-all-instrumented" > $WORK/logs/I0-android-all.log 2>&1`, sanitised
   to `$E/I0-android-all.txt` (folder names only; the "before" of AC-12 (f)).
10. **Probe (before)** by 4.P with k = 0 → `$E/I0-probe.txt`. **Row:** D12.
11. **Records.** Create `$W/implementation-log.md`: header; SC-1 to SC-9 each with check and result; `<start>`, `C0`,
    the `@Ignore` lines, the skipped baseline; the **D1a table** (2.1, rows checked against `I0-oldtext.txt` and
    `$SUPP`: the "at `<start>`" column must equal the build files); the I0 results by reference to the evidence
    files. Create `$E/compatibility-record.md` from 2.2 with the supplement's quotations. **Rows:** D13, D14.
12. `git status --porcelain` lists only `?? $W/implementation-log.md` and `?? $E/` (AC-1, last sentence) besides the
    checkout's other untracked orchestrator files, which are left alone. I0 makes no commit; its evidence goes into the
    I1 commit.

### I1 — Gradle wrapper 9.9.2 (AC-6; AC-17 by Scope Concern 3)

1. Pre-check: `git status --porcelain -- gradle gradlew gradlew.bat build.gradle app/build.gradle` empty; no Gradle JVM.
2. **W1** (on 9.5.1): `./gradlew --console=plain wrapper --gradle-version 9.9.2 --distribution-type bin --gradle-distribution-sha256-sum <Q-1> > $WORK/logs/I1-wrapper-run1.log 2>&1; echo "exit=$?" >> $WORK/logs/I1-wrapper-run1.log`
   → `exit=0`; sanitised to `$E/I1-wrapper-run1.txt`. **Row:** D15.
3. At once, before any other Gradle call: `grep -c 'LOCAL ADDITION' gradlew` → `0` (the block is lost, Risk 8);
   `cp gradlew $WORK/gradlew-gen1`; apply **E1** (3.1); `python -I -X utf8 $TL/gradlew_block.py $WORK/gradlew-start $WORK/gradlew-gen1 gradlew`
   → all `OK` and the block SHA `45364f3d…ea760`; `git hash-object gradlew` (expected equal to `git rev-parse HEAD:gradlew`,
   because run 1 used 9.5.1's template; a difference is recorded, not a stop, as run 2 regenerates). **Rows:** D16, D17.
4. **VER(`I1-gradle-version-run1`)**: the first run on the new distribution downloads `gradle-9.9.2-bin.zip` through
   the wrapper's own path and verifies it against `distributionSha256Sum`. Expected `exit=0`, "Gradle 9.9.2", the
   Daemon JVM line, no "Unable to establish loopback connection", a `Downloading https://services.gradle.org/distributions/gradle-9.9.2-bin.zip`
   line. Then `Z=$(ls "$HOME"/.gradle/wrapper/dists/gradle-9.9.2-bin/*/gradle-9.9.2-bin.zip | head -1); echo "dist-sha-match=$([ "$(sha256sum "$Z" | cut -d' ' -f1)" = "<Q-1>" ] && echo yes || echo no)"`
   → `yes` (the log records that the downloaded file equals the pinned checksum; the path is not printed). Sanitised to
   `$E/I1-gradle-version-run1.txt`. **Rows:** D18 to D20.
5. **W2** (on 9.9.2): the same command into `$WORK/logs/I1-wrapper-run2.log` → `exit=0`; `$E/I1-wrapper-run2.txt`. **Row:** D21.
6. At once: `grep -c 'LOCAL ADDITION' gradlew` → `0`; `cp gradlew $WORK/gradlew-gen2`; apply **E1** again;
   `gradlew_block.py $WORK/gradlew-start $WORK/gradlew-gen2 gradlew` → all `OK`;
   `git -c core.autocrlf=false diff --no-index --numstat -- $WORK/gradlew-gen2 gradlew | cut -f1,2` → `12	0`.
   Record `sha256sum gradlew.bat` now (the generator's output; never opened afterwards). **Row:** D17.
7. **VER(`I1-gradle-version`)** at once, before any other step (AC-6 (f)): "Gradle 9.9.2", the Daemon JVM line citing
   `gradle/gradle-daemon-jvm.properties`, no loopback error. **Row:** D19.
8. **Wrapper checks** (one call into `$E/I1-wrapper.txt`):
   ```bash
   sha256sum gradle/wrapper/gradle-wrapper.jar | cut -d' ' -f1; echo "official=<Q-2>"
   git diff -U0 HEAD -- gradle/wrapper/gradle-wrapper.properties | grep -E '^(@@|[-+][^-+])'
   grep -c '^distributionSha256Sum=<Q-1>$' gradle/wrapper/gradle-wrapper.properties; grep -c '^validateDistributionUrl=true$' gradle/wrapper/gradle-wrapper.properties
   git diff --numstat HEAD -- gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar
   sha256sum gradlew.bat
   python -I -X utf8 $TL/gradlew_block.py $WORK/gradlew-start $WORK/gradlew-gen2 gradlew
   git diff --stat HEAD -- gradle/gradle-daemon-jvm.properties; echo "daemon-criteria-end"
   git grep -nE "^[[:space:]]*org\.gradle\.java\.home[[:space:]]*=" -- '*.properties' '*.gradle' gradlew gradlew.bat .github; echo "java-home-set-exit=$?"
   ```
   Expected: the jar SHA equals Q-2; the properties hunk `@@ -3,2 +3,2 @@` with the old and new `:3` and `:4` lines
   (`gradle-9.9.2-bin.zip`) and nothing else if the 9.9.2 generator keeps L6's keys and order (not derivable here
   without running it), otherwise each further generated line listed (row D23); `1` and `1`; the `gradlew` numstat
   recorded (template changes plus `12` block lines relative to the template; the block proof is the arbiter); the
   `gradlew.bat` SHA equal to step 6's; all `OK`; nothing before `daemon-criteria-end`; `java-home-set-exit=1`.
   **Rows:** D22 to D24.
9. **G(1)**, EV-T(1), EV-B(1), CMP(1), CK(1). AGP is still 9.0.1 (C2: Gradle 9.9 tests AGP 9.0). Expected `exit=0` and
   every CMP cell `=` (2.9). If the gate fails for a missing verification entry, VMP(1) runs (Scope Concern 3) and then
   G(1) again. **Rows:** D25, D26. **Must hold (S2-I1, AC-6):** gates green on Gradle 9.9.2 with AGP 9.0.1; the block
   present and proven; the jar official; the daemon criteria unchanged.
10. Log entry; CM(1).

### I2 — KSP 2.3.14 on Kotlin 2.3.21 (AC-7; G3)

1. CK pre-check: the build files equal HEAD. Apply **E2**. VMP(2), G(2), EV-T(2), EV-B(2), CMP(2), CK(2).
2. Expected: `build.gradle` numstat `1	1`; Kotlin lines unchanged; `ksp-files.txt vs I0 exit=0` (G3); every other CMP cell `=`.
   **Rows:** D54 (the pre-specified reorder), D27 to D53. **Must hold:** KSP tasks pass and the KSP list equals I0.
3. Log entry; CM(2). **Reorder case** (row D54): no commit; `git restore --source=HEAD --worktree -- build.gradle gradle/verification-metadata.xml`;
   the failing error verbatim into `$E/I2-moved.txt` (sanitised) and the log ("I2 moved to I5"); E2 is applied in I5.

### I3 — AGP 9.5.2 (AC-8; G1, G5, A13, D8 item 3)

1. Apply **E3**. VMP(3), G(3), EV-T(3), EV-B(3), CMP(3), CK(3).
2. Checks into `$E/I3-checks.txt` (appended to CK): `grep -n "classpath 'com.android.tools.build:gradle:" build.gradle`;
   for each of the three `jacocoClassRootPaths` directories, `find app/build/<dir> -name '*.class' | wc -l` (each above
   0, AC-8 (b)); `grep -E "^com\.zegoggles\.smssync\.receiver " $E/I3-coverage.txt` (ratio above 0.50, AC-8 (d)); the
   `compileSdkVersion`/`minSdkVersion`/`targetSdkVersion` lines with their values 35, 24, 35 (A13, AC-8 (e)).
3. **L(3)**; `$E/I3-lint.txt`, `$E/I3-lint-ids.txt`; compare with I0: `git -c core.autocrlf=false diff --no-index -- $E/I0-lint-ids.txt $E/I3-lint-ids.txt`
   lines into the log, each new rule ID named (AC-8 (g), AC-18 (e)).
4. Every AGP-originated line of `I3-warnings.txt` is listed in the log as debt (AC-8 (f)).
   **Rows:** D51, D55 to D57, D27 to D53 (F1 by D35). **Must hold:** gates green; class roots non-empty after `clean`;
   manifests and the R8 set equal I0; `receiver` above 0.50; every package at or above its floor.
5. Log entry; CM(3).

### I4 — Hilt 2.60.1, all five coordinates (AC-9; G4)

1. Apply **E4.1** to **E4.5**. VMP(4), G(4), EV-T(4), EV-B(4), CMP(4), CK(4).
2. **KC(4)**: sanitise with **KF** into `$E/I4-kotlinc.txt`; summary into `$E/I4-kotlinc-summary.txt`:
   `{ echo "task-line=$(grep -c '^> Task :app:compileDebugKotlin$' $E/I4-kotlinc.txt)"; echo "task-skipped=$(grep -cE '^> Task :app:compileDebugKotlin (UP-TO-DATE|FROM-CACHE|NO-SOURCE|SKIPPED)' $E/I4-kotlinc.txt)"; echo "warnings=$(grep -c '^w: ' $E/I4-kotlinc.txt)"; echo "worker-coroutineContext=$(grep -E '^w: .*(BackupWorker|RestoreWorker)\.kt' $E/I4-kotlinc.txt | grep -c 'coroutineContext')"; }`.
   Expected `task-line=1 task-skipped=0`, `warnings` recorded (the I5 baseline), `worker-coroutineContext=0`.
3. **DEP(4)** → `$E/I4-deps-releaseRuntime.txt`, `$E/I4-deps-releaseRuntime-tree.txt` (as I0 step 8); compare with
   I0 by `git diff --no-index`: expected changed lines only for `com.google.dagger:*` (2.60.1) and closure artifacts
   below S4 ownership; `grep -E '^(androidx\.work|androidx\.lifecycle|org\.jetbrains\.kotlinx:kotlinx-coroutines|androidx\.preference|androidx\.annotation:annotation:|androidx\.core:core-role|androidx\.security:security-crypto)' `
   on both files compared equal (AC-17 (e)).
4. Checks appended to CK: all five Hilt lines carry `2.60.1` (`grep -c "2\.60\.1" build.gradle app/build.gradle` →
   `1` and `4`); `androidx.hilt` lines unchanged; the KSP list difference against I0, each line explained (Hilt
   2.60.1's generated names) or none; `hiltJavaCompileDebug` and `transformDebugClassesWithAsm` present in
   `I4-gates.txt` without `FAILED`; the `worker` line of `I4-coverage.txt`.
   **Rows:** D59, D60, D31, D27 to D53. **Must hold:** G4 (Hilt tasks, ASM transform, Hilt tests, R8 C-2) green; five
   identical versions; KSP differences explained.
5. Log entry; CM(4).

### I5 — Kotlin 2.4.21 (AC-10; F-1 stop rule; Risk 9; U-067's six mutations)

1. Apply **E5.1**, **E5.2** (and **E2** if I2 moved). VMP(5), G(5), EV-T(5), EV-B(5), CMP(5), CK(5).
2. **KC(5)** → `$E/I5-kotlinc.txt`, `$E/I5-kotlinc-summary.txt` (as I4). Compare the `w:` lines: `grep '^w: ' $E/I4-kotlinc.txt | sed 's#^w: file:///<repo>/##' | LC_ALL=C sort > $WORK/w4.txt`
   (likewise `w5.txt`), `git -c core.autocrlf=false diff --no-index -- $WORK/w4.txt $WORK/w5.txt` → every `+` line is
   a new warning, recorded as debt in the log (AC-10 (b)).
3. **F-1 stop rule** (AC-10 (c)) into CK: `git diff --stat HEAD -- app/src/test; echo "test-diff-end"` (nothing);
   `grep -E '^w: .*(BackupWorker|RestoreWorker)\.kt' $E/I5-kotlinc.txt | grep -c 'coroutineContext'` → `0`; and in
   `I5-cases.txt` every case of the NEW3 classes `PASS`. **Row:** D61.
4. **DEP(5)** → `$E/I5-deps-releaseRuntime.txt`, `…-tree.txt`; compared with I4: expected changes only
   `org.jetbrains.kotlin:kotlin-stdlib*` (2.4.21) and Kotlin artifacts; S4-owned lines equal (row D31).
5. **Mutations** by 4.M (M-1a to M-3b) on this uncommitted tree. **Rows:** D62 to D64.
6. Risk 9 and fallbacks: rows D58, D65 (F2, F4). KSP list equals I4's (AC-10 (g), SC-P3). **Must hold:** gates green;
   F-1 rule; six mutations detected by the same named tests; KSP list as I4.
7. Log entry; CM(5).

### I6 — androidx.hilt 1.4.0 (AC-11)

1. Apply **E6.1**, **E6.2**. VMP(6), G(6), EV-T(6), EV-B(6), CMP(6), CK(6).
2. **DEP(6)** → `$E/I6-deps-releaseRuntime.txt`, `…-tree.txt`; compared with I5: expected changes only `androidx.hilt:*`
   (1.4.0); S4-owned lines equal (row D31; N13).
3. Checks appended to CK: both lines `1.4.0`; `WorkManagerSchedulerTest`, the worker tests and `BackupTriggerWorkerTest`
   (including `ic1_hiltWiring_assistedFactoryIsBoundUnderThePersistedClassName`) `PASS` in `I6-cases.txt`; KSP list
   equals I4's; the `worker` coverage line. **Rows:** D66, D67, D60, D31. **Must hold:** gates green; the factory path
   tests pass unmodified; KSP list as I4.
4. Log entry; CM(6).

### I7 — Robolectric 4.18.1 (AC-12; D7; A14, A15, A21; In-flight work)

1. Apply **E7.1** only (E7.2 waits for the D7 result). VMP(7), G(7), EV-T(7), EV-B(7), CMP(7).
2. **D7 scan, no flags** (into `$E/I7-d7.txt`):
   ```bash
   for p in 'InaccessibleObjectException' 'does not "opens"' 'IllegalAccessError' 'module java.base' 'A1-G1' 'A1-G3'; do
     echo "$p log=$(grep -c -F "$p" $WORK/logs/I7-gates.log) xml=$(grep -o -F "$p" $R/TEST-*.xml | wc -l)"; done
   ```
   Expected all `0`. Zero hits → apply **E7.2** (main path; numstat then `4 4` with E7.1). Any hit → row D69 (E7.3,
   the exact flags the error names, then G(7) again with EV-T/EV-B/CMP re-run).
3. **A14** (AC-12 (b)): `grep -n "@ConscryptMode(OFF)\|@LooperMode(LEGACY)" $TS/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`
   (two lines) and every case of that class `PASS` (its `@Before` guards ran). **Row:** D68.
4. **TLS classes** (AC-12 (c), (d)): in `I7-cases.txt` every case of `DefaultTrustedSocketFactoryHandshakeTest`,
   `…HardeningTest`, `…JdkProviderTest`, `…SniTest` and `PinnedCertificateSocketFactory*Test` `PASS`, and
   `git diff --stat HEAD -- app/src/test` empty. **Row:** D72.
5. **A21** (AC-12 (e)) into `$E/I7-monitor.txt`: from DEP(7)'s test classpath the resolved `androidx.test:monitor`
   version; the AAR Gradle resolved, located by coordinate in the Gradle cache (`ls "$HOME"/.gradle/caches/modules-2/files-2.1/androidx.test/monitor/<v>/*/monitor-<v>.aar`),
   its SHA-256 compared with the `<sha256>` of `monitor-<v>.aar` in `gradle/verification-metadata.xml` (`match` expected),
   `unzip -p <aar> META-INF/com/android/build/gradle/aar-metadata.properties` (absent at 1.8.0, L13, or its
   `minCompileSdk`), and the manifest `minSdkVersion`. Nothing downloaded by hand; the path is not printed. **Row:** D74.
6. **DEP(7)** → `$E/I7-deps-releaseRuntime.txt` (equal to I6: a test-only bump) and `$E/I7-deps-debugUnitTestRuntime.txt`
   (changes recorded: Robolectric, `androidx.test`, Conscrypt, BouncyCastle, ASM; any `androidx.*` S4 library version
   moved on the test classpath is recorded and handed over, 2.8).
7. **android-all** (AC-12 (f)) into `$E/I7-android-all.txt`: the folder listing as I0 step 9, the new folders named,
   and for each new jar and POM its size and SHA-512 compared with its `.sha512` file (U-068 ruling P-1 method). A
   failed download was retried once (row D73).
8. **Probe (after)** by 4.P with k = 7 → `$E/I7-probe.txt`; `git -c core.autocrlf=false diff --no-index -- $E/I0-probe.txt $E/I7-probe.txt`
   lines into the log (provider version, default-enabled suites before and after; AC-12 (d)'s "both lists").
9. CK(7) with: `git diff --stat HEAD -- app/src/test/resources/robolectric.properties` empty (AC-12 (g)).
   **Rows:** D68 to D74, D27 to D53 (F5 by D69). **Must hold:** gates green with no flags (or exactly the named flags);
   both modes offered; guards pass; TLS classes and U-066's exact-list tests unchanged and passing.
10. Log entry; CM(7).

### I8 to I11 — Mockito, Truth, GreenMail, auto-service (AC-13), one commit each

For k = 8, 9, 10, 11 in that order: apply E<k>; VMP(k), G(k), EV-T(k), EV-B(k), CMP(k), CK(k); log entry; CM(k).
- **I8:** `I8-checks.txt` adds the resolved `net.bytebuddy:byte-buddy` version from `I8-vm-added.txt` (1.17.7 expected,
  L13), Q-9's statement quoted in the log, and `grep -c "Mockito is currently self-attaching\|A Java agent has been loaded dynamically" $WORK/logs/I8-gates.log`
  recorded as JEP 451 debt with nothing changed (D7). **Row:** D76.
- **I9:** the line keeps its double quotes. **I10:** `BackupImapStoreDelegateIntegrationTest` cases `PASS` (row D77).
  **I11:** `testAnnotationProcessor` compiles (row D78).
**Must hold (each):** gates green; one component changed; all CMP cells `=`.

### I12 — JaCoCo 0.8.16 (AC-14; D8)

1. Apply **E12**. VMP(12), G(12), EV-T(12), EV-B(12), CMP(12) with `<p>` = I11, CK(12).
2. Checks appended: `python -I -c "import json, sys; a, b = (json.load(open(p)) for p in sys.argv[1:3]); print('class-counts-equal', {k: v['classes'] for k, v in a.items()} == {k: v['classes'] for k, v in b.items()})" $E/I11-jacoco-inventory.json $E/I12-jacoco-inventory.json`
   → `class-counts-equal True` (the inventory is the per-package class count; covered lines are compared separately,
   because `activity`'s alternate, 2.6); `cut -d' ' -f1 $E/I11-jacoco-classes.txt > $WORK/c11.txt; cut -d' ' -f1 $E/I12-jacoco-classes.txt > $WORK/c12.txt; git -c core.autocrlf=false diff --no-index --quiet -- $WORK/c11.txt $WORK/c12.txt; echo "class-names-equal-exit=$?"`
   → `0`; `I12-coverage.txt` against `I11-coverage.txt` per package (ratio and spare, stated in the log, `worker` and
   `compat` named); `grep -c "Unsupported class file major version" $WORK/logs/I12-gates.log` → `0`.
3. **L(12)** → `$E/I12-lint.txt`, `$E/I12-lint-ids.txt`; new rule IDs against I0 named (AC-18 (e)).
   **Rows:** D79 to D81. **Must hold:** inventory equal to I11; every package at or above its floor.
4. Log entry; CM(12).

### I13 — CI workflow (AC-15; IC-3)

1. Pre: Q-10 present in `$SUPP` (row D83). Apply **E13.1** to **E13.5**.
2. CK(13) with `git diff -U0 HEAD -- .github/workflows/ci.yml | grep '^@@'` → the six hunks of L11 and
   `git diff --numstat HEAD -- .github/workflows/ci.yml` → `8	7`.
3. **CIP, step 1**: `./gradlew --console=plain --warning-mode all test lint jacocoTestReport assembleRelease --continue > $WORK/logs/I13-ci-parity.log 2>&1; echo "exit=$?" >> …`
   → expected `exit=1` with only lint tasks failed (`> Task :app:lintDebug FAILED`); sanitised with **G** plus
   `'Lint found'` into `$E/I13-ci-parity.txt` and with **WR** into `$E/I13-warnings.txt`; the lint rule IDs from the
   report into `$E/I13-lint-ids.txt` (the follow-up is S3, D17; the step is not removed from the workflow).
4. **CIP, step 2**: `./gradlew --console=plain :app:jacocoTestCoverageVerification > $WORK/logs/I13-ci-coverage.log 2>&1; echo "exit=$?" >> …`
   → `exit=0`; `$E/I13-ci-coverage.txt`.
5. **YAML parse** into `$E/I13-yaml.txt`:
   `python -I -c "import sys, yaml; d = yaml.safe_load(open(sys.argv[1], encoding='utf-8')); j = d['jobs']['build']; print('parser: PyYAML', yaml.__version__); print('top-level keys:', sorted(map(str, d))); print('runs-on:', j['runs-on']); print('uses:', [s['uses'] for s in j['steps'] if 'uses' in s]); print('run:', [s['run'].strip() for s in j['steps'] if 'run' in s]); print('steps:', len(j['steps']))" .github/workflows/ci.yml`
   → `parser: PyYAML 6.0.2`; `top-level keys: ['True', 'jobs', 'name']` (`on:` read as YAML 1.1 boolean); `runs-on:
   ubuntu-22.04`; `uses: ['actions/checkout@v7', 'actions/setup-java@v6', 'actions/cache@v6', 'actions/cache@v6']`;
   `run:` the `sdkmanager` line and the two `./gradlew` commands unchanged; `steps: 7` (derived on the simulated file, L11).
6. Log: the AGP minimum Build Tools value relied on (36.0.0) with the AGP release-notes URL and Q-4's quotation
   (AC-15 (c), REQ-015 AC-16); A18 from Q-10 (AC-15 (d)); "what IC-3 proves and does not" (story IC-3).
   **Rows:** D82 to D85. **Must hold:** YAML parses; the three commands and the triggers unchanged; the parity run
   reaches every step.
7. CM(13).

### I14 — Documentation, final gates and hand-off (AC-16, AC-18 to AC-24, IC-1, IC-2)

1. Apply **E14.1** to **E14.15**. CK(14) with the numstat `9	9` (`build.gradle`), `8	8` (`app/build.gradle`), `3	3`
   (`code-location.md`), and `git diff -U0 HEAD -- build.gradle app/build.gradle | grep -E '^[-+]' | grep -vE '^(\+\+\+|---) ' | grep -vc '//'`
   → `0` (every changed line is a comment line or keeps its code and changes only its trailing comment; derived on the
   simulated I14 files: the 34 changed lines all contain `//`).
2. **G(14)**, EV-T(14), EV-B(14), CMP(14) (`<p>` = I12: I13 made no gate run), VER(`I14-gradle-version`) →
   `$E/I14-gradle-version.txt`.
3. **DEP(14)** → `$E/I14-deps-releaseRuntime.txt`, `$E/I14-deps-releaseRuntime-tree.txt`, `$E/I14-deps-debugUnitTestRuntime.txt`;
   IC-1: `MULTI 0` in both summaries; each S2 coordinate resolves at one version (`kotlin-stdlib:2.4.21`,
   `hilt-android:2.60.1`, `hilt-work:1.4.0` in release; `robolectric:4.18.1`, `mockito-core:5.24.0`, `truth:1.4.5`,
   `greenmail-junit4:2.1.15`, `hilt-android-testing:2.60.1` in the test file); S4-owned lines equal I0 (AC-17 (e)).
4. **End proof** (AC-17 (h)): `git show $START:gradle/verification-metadata.xml > $WORK/vm-start.xml; python -I -X utf8 $TL/vm_proof.py $WORK/vm-start.xml gradle/verification-metadata.xml $E/I14-vm-added.txt > $WORK/logs/I14-vm-proof.log; echo "proof-exit=$?" >> …`
   with the three counts appended → `$E/I14-vm-proof.txt`: unchanged configuration, 0 removed, added above 0, `trust=3`
   (the same as at `<start>`).
5. **Sweep** (AC-16 (c)) into `$E/I14-sweep.txt`: the I0 step 4 sweep command. Expected after I14: only
   `CHANGES:141`, `gradle.properties:15`, `metadata/f-droid/com.zegoggles.smssync.txt:32`, `:33`, `:34` and
   `DefaultTrustedSocketFactoryJdkProviderTest.java:60` (L11), each dispositioned in the log; also the AC-22 (b)
   pre-release grep (L10's four lines), the `SNAPSHOT` grep (`exit=1`) and the IC-2 `org.gradle.java.home` grep
   (`exit=1`). **AC-22 (a)** (the 2.1 table equals the build files):
   `grep -cE "gradle-9\.9\.2-bin|gradle:9\.5\.2|kotlin-gradle-plugin:2\.4\.21|kotlin-stdlib:2\.4\.21|ksp\.gradle\.plugin:2\.3\.14|hilt-android-gradle-plugin:2\.60\.1|hilt-android:2\.60\.1|hilt-compiler:2\.60\.1|hilt-android-testing:2\.60\.1|hilt-work:1\.4\.0|hilt-compiler:1\.4\.0|toolVersion = \"0\.8\.16\"|junit:4\.13\.2|robolectric:4\.18\.1|mockito-core:5\.24\.0|truth:1\.4\.5|greenmail-junit4:2\.1\.15|auto-service:1\.1\.1|checkout@v7|setup-java@v6|cache@v6|build-tools;36\.0\.0" gradle/wrapper/gradle-wrapper.properties build.gradle app/build.gradle .github/workflows/ci.yml`
   → `1`, `4`, `14`, `5` (24 lines; derived on the simulated I14 files, L11), with no `buildToolsVersion` line in
   `app/build.gradle` (`grep -c buildToolsVersion` → `0`).
6. **Counts** (AC-19): `git grep --untracked -h "@Test" -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`
   (the working tree before the I14 commit; the log says so) → `C0`; `I14-junit-totals.txt` against `I0-junit-totals.txt`
   (tests, failures 0, errors 0, skipped equal, row D48); the Gradle total equals `C0`.
7. **Final checks** (one call into `$E/I14-final-checks.txt`; scoped to this story while the orchestrator commits documents):
   ```bash
   git log --format='%h %s' $START..HEAD                                             # every commit since START, recorded
   git log --reverse --format='%s' --grep='^U-071 S2-' $START..HEAD                  # this story's, in order (AC-24)
   git log --format='%h %s' --invert-grep --grep='^U-071 S2-' $START..HEAD -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat .github/workflows sdlc/artifacts/engagement/code-location.md; echo "foreign-code-end"
   { git log --format= --name-only --grep='^U-071 S2-' $START..HEAD; git diff --name-only HEAD -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat .github sdlc/artifacts/engagement/code-location.md $W; } | LC_ALL=C sort -u
   git log --format=%h --grep='^U-071 S2-' $START..HEAD -- gradle/gradle-daemon-jvm.properties settings.gradle gradle.properties k9mail-vendored app/lint.xml app/lint-baseline.xml app/proguard-rules.pro app/src/main/AndroidManifest.xml app/src/test; echo "forbidden-paths-end"
   git diff --stat $START -- app/src/main; echo "main-diff-end"
   git diff $START -- app/build.gradle | grep -E '^[-+] *(minimum|limit|value|includes|excludes)'; echo "floor-lines-end"
   grep -n "compileSdkVersion\|minSdkVersion\|targetSdkVersion\|versionCode" app/build.gradle
   git diff $START -- app/build.gradle | grep -E "^[-+] +(implementation|testImplementation|ksp|kspTest|testAnnotationProcessor) '(org\.jetbrains\.kotlinx|androidx\.(lifecycle|annotation|preference|core|security|work))"; echo "runtime-libs-end"
   ```
   Expected: (i) recorded; (ii) the 14 subjects of 4.9 in order (fix commits interleaved, recorded); (iii) nothing
   before `foreign-code-end` (except U-072's commits under N-6, listed); (iv) a list inside AC-23's allowed set
   (`gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`,
   `build.gradle`, `app/build.gradle`, `gradle/verification-metadata.xml`, `.github/workflows/ci.yml`,
   `sdlc/artifacts/engagement/code-location.md`, files under `$W`, and forced pre-specified edits only); (v) nothing
   before `forbidden-paths-end`; (vi) nothing before `main-diff-end` (or only pre-specified forced edits); (vii) nothing
   before `floor-lines-end`; (viii) `35`, `24`, `35`, `1602` (A13 names may differ only under D56); (ix) nothing before
   `runtime-libs-end` (AC-22 (d)). **Row:** D89.
8. **Records**: complete `$E/compatibility-record.md` (gap symptoms column; "fallback: not triggered" with
   `I<k>-warnings.txt` as evidence, or each fallback with its flag); the log's Contract Adherence, Capabilities
   Inventory (the `gradlew` block, the daemon criteria, `-Werror`, R8 posture, JaCoCo floors and guard, verification
   configuration: each kept) and "Runtime claims" (none: no device run by the developer); IC-5 and IC-6 recorded "not
   run: needs the product owner's decision" with 10.2's text; then the hygiene scan of every evidence file of the story:
   `python -I -X utf8 $TL/scan.py $W/implementation-log.md $E/*.txt $E/*.json $E/*.xml $E/*.md $M/*.txt > $WORK/logs/I14-scan.log`
   sanitised to `$E/I14-scan.txt` (all `OK`).
9. CM(14). The hand-off message (not the log) carries `git log --reverse --format='%h %s' --grep='^U-071 S2-' $START..HEAD`
   and `git rev-parse HEAD`.

### 4.M Mutation protocol and table (I5 step 5; write-ahead; restore proven by hash)

`$E/mutation-log.md` header: the title; "Protocol: U-071 plan 4.M, the U-067 plan I6 form (write-ahead entry with clock
time, one Edit, one targeted run, restore by copying the pristine file, hash, diff-hash and porcelain check, `restored`
line). Repository-relative paths."; `START`; `BW_BLOB` and `RW_BLOB` = `git hash-object $BW $RW` at I5 step 5 (equal to
`git rev-parse HEAD:$BW HEAD:$RW` unless a Risk 9 edit is in the tree; then `git hash-object -w` stores them);
`PROD_DIFF` = `git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin` (the empty-input blob
`e69de29bb2d1d6434b8b29ae775ad8c2e48c5391` when no forced edit exists); the NEW3 case list at I5 from `I5-cases.txt`,
split into **U-067's 31 originals** (the names of U-067's mutation log and test table) and **later cases** (added by
U-070 or U-074, named). Recovery note: "If a session is lost while an entry says `APPLYING` and has no `restored`
line, nothing else runs until the file's `git hash-object` has been compared with `BW_BLOB` or `RW_BLOB` and, if it
differs, the file has been restored from `$WORK/pristine/`; if that copy is lost, `git cat-file --filters --path=<path> <BW_BLOB|RW_BLOB> > <path>`
(writes the working-tree CRLF form). `git checkout` is never used."

**Clean check** (before and after each entry): `git hash-object $BW $RW` equal to `BW_BLOB`, `RW_BLOB`;
`git diff HEAD -- app/src/main k9mail-vendored | git hash-object --stdin` equal to `PROD_DIFF`;
`git status --porcelain -- app/src/main` empty (or exactly the forced-edit files).

**Each entry, one at a time:** (1) clean check; `cp $BW $RW "$WORK/pristine/"` once; (2) write-ahead `### <id> APPLYING <timestamp>`
with file, hash before, edit, class set, expected; (3) one Edit (Read the lines first; copy `old_string` from the Read);
(4) TR(`<id>`, the three NEW3 classes), then `python -I -X utf8 $T/sanitize.py $WORK/logs/<id>.log $M/<id>-run.txt --grep '<G>'`,
`python -I -X utf8 $T/junit_cases.py $R BackupWorkerLoopCancellationTest RestoreWorkerIntegrationTest RestoreWorkerCheckpointTest > $WORK/logs/<id>-cases.log`
→ sanitised `$M/<id>-cases.txt`, and `grep -o 'message="[^"]*"' $R/TEST-*.xml > $WORK/logs/<id>-msg.log` → sanitised
`$M/<id>-message.txt`; (5) `cp "$WORK/pristine/<file>" <path>` at once, whatever the result; (6) clean check, then
append the list lines `- run_exit: …`, `- cases: …`, `- failed: …`, `- passed: …`, `- expected: matches | does not match`,
`- verdict: …` and `- restored: <timestamp> hash-ok diff-hash-ok porcelain-ok` (the U-067 log's form; 4.R counts the
`### M-… APPLYING` headings against the `- restored:` lines). No gate, review or commit runs while an entry lacks `restored`.

| Id | File | Exact edit (`old_string` → `new_string`) | Must fail (U-067's originals) | Must pass |
|----|------|------------------------------------------|-------------------------------|-----------|
| M-1a | `$BW` | `                currentCoroutineContext().ensureActive()` → `                coroutineContext.ensureActive()` (old binding) | `backupCursors_cancelledAfterSecondAppend_stopsAtNextPass` ("expected 2 but was 4") | the other 30 originals |
| M-1b | `$BW` | same `old_string` → `                if (false) currentCoroutineContext().ensureActive()` | the same test | the other 30 |
| M-2a | `$RW` | the three lines `                    currentCoroutineContext().ensureActive()` / (blank) / `                    val item = msgs[currentRestoredItem]` → the same with `coroutineContext.ensureActive()` | `doWork_imapRestore_cancelledDuringSecondImport_stopsAtNextPass` ("expected 2 but was 4") | the other 30 |
| M-2b | `$RW` | the same three lines → the same with `if (false) currentCoroutineContext().ensureActive()` | the same test | the other 30 |
| M-3a | `$RW` | the three lines with `                    val values = items[currentRestoredItem]` → with `coroutineContext.ensureActive()` | `executeRestoreWithValues_cancelledAfterThirdInsert_stopsAtNextPass` ("expected 3 but was 5") and `executeRestoreWithValues_cancelledBeforeFirstPass_insertsNothing` ("expected 0 but was 5") | the other 29 |
| M-3b | `$RW` | the same three lines → with `if (false) currentCoroutineContext().ensureActive()` | the same two | the other 29 |

Later cases (U-070, U-074) in the three classes: each outcome recorded; a failure is expected and accepted only
under a mutation of the loop the case drives (`backupCursors_*` ↔ M-1x, `doWork_imapRestore_*` ↔ M-2x,
`executeRestoreWithValues_*` ↔ M-3x); any other failure is row D64. Order M-1a, M-1b, M-2a, M-2b, M-3a, M-3b; six runs;
a summary table at the end (id, site, failing of the originals, later cases failing, verdict). "Failing the same named
tests as in U-067's `mutation-log.md`" (AC-10 (d)) means: exactly the named originals fail and the other originals pass.

### 4.9 Commits (exact file lists and subjects)

Shorthands (bash brace expansion gives explicit paths; `$W` = `sdlc/artifacts/build/sprints/sprint-025/U-071`):
- **STD(k)** = `$W/evidence/I<k>-{gates.txt,warnings.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,jacoco-inventory.json,jacoco-classes.txt,coverage.txt,ksp-files.txt,merged-debug.xml,merged-release.xml,manifest-debug.txt,manifest-release.txt,kept-names.txt,configuration.txt,apk-entries-debug.txt,apk-entries-release.txt,apk.txt,javap.txt,compare.txt,checks.txt}` (21 files)
- **VM(k)** = `$W/evidence/I<k>-{writer.txt,vm-proof.txt,vm-added.txt,enforced.txt}` (4 files)
- **I0SET** = `$W/evidence/I0-{start.txt,oldtext.txt,greps.txt,vm-counts.txt,gradle-version.txt,gates.txt,warnings.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,jacoco-inventory.json,jacoco-classes.txt,coverage.txt,ksp-files.txt,merged-debug.xml,merged-release.xml,manifest-debug.txt,manifest-release.txt,kept-names.txt,configuration.txt,apk-entries-debug.txt,apk-entries-release.txt,apk.txt,javap.txt,lint.txt,lint-ids.txt,lint-baseline-count.txt,deps-releaseRuntime.txt,deps-releaseRuntime-tree.txt,deps-debugUnitTestRuntime.txt,android-all.txt,probe.txt}` (32 files)
- **TOOLS** = `$W/evidence/tools/{oldtext_check.py,gradlew_block.py,vm_proof.py,deps_resolved.py,cov_floors.py,jacoco_class_lines.py,scan.py,U071ProviderProbeTest.java.txt}` (8 files)
- **LOG** = `$W/implementation-log.md`

| Commit | Subject | Files |
|--------|---------|-------|
| I1 | `U-071 S2-I1: Gradle wrapper 9.9.2 (Windows launcher block re-applied)` | `gradle/wrapper/gradle-wrapper.properties`; `gradle/wrapper/gradle-wrapper.jar`; `gradlew`; `gradlew.bat`; LOG; `$W/evidence/compatibility-record.md`; TOOLS; I0SET; STD(1); `$W/evidence/I1-{wrapper-run1.txt,gradle-version-run1.txt,wrapper-run2.txt,gradle-version.txt,wrapper.txt}`; plus `gradle/verification-metadata.xml` and VM(1) only if row D25 fired |
| I2 | `U-071 S2-I2: KSP 2.3.14` | `build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(2); VM(2) — **no commit** under row D54 |
| I3 | `U-071 S2-I3: Android Gradle Plugin 9.5.2` | `build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(3); VM(3); `$W/evidence/I3-{lint.txt,lint-ids.txt}`; plus `$W/evidence/I2-moved.txt` under D54; plus `app/build.gradle` under D55 or D56 |
| I4 | `U-071 S2-I4: Hilt 2.60.1 (plugin, runtime, compiler, testing)` | `build.gradle`; `app/build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(4); VM(4); `$W/evidence/I4-{kotlinc.txt,kotlinc-summary.txt,deps-releaseRuntime.txt,deps-releaseRuntime-tree.txt}` |
| I5 | `U-071 S2-I5: Kotlin 2.4.21 (plugin and stdlib)` (under D54: `U-071 S2-I5: Kotlin 2.4.21 and KSP 2.3.14 (KSP moved from I2)`) | `build.gradle`; `app/build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(5); VM(5); `$W/evidence/I5-{kotlinc.txt,kotlinc-summary.txt,deps-releaseRuntime.txt,deps-releaseRuntime-tree.txt}`; `$W/evidence/mutation-log.md`; `$W/evidence/mutations/{M-1a,M-1b,M-2a,M-2b,M-3a,M-3b}-{run,cases,message}.txt` (18 files); plus the four Risk 9 files under D58 |
| I6 | `U-071 S2-I6: androidx.hilt 1.4.0` | `app/build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(6); VM(6); `$W/evidence/I6-{deps-releaseRuntime.txt,deps-releaseRuntime-tree.txt}` |
| I7 | `U-071 S2-I7: Robolectric 4.18.1` | `app/build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(7); VM(7); `$W/evidence/I7-{d7.txt,monitor.txt,deps-releaseRuntime.txt,deps-debugUnitTestRuntime.txt,android-all.txt,probe.txt}` |
| I8 | `U-071 S2-I8: Mockito 5.24.0` | `app/build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(8); VM(8) |
| I9 | `U-071 S2-I9: Truth 1.4.5` | `app/build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(9); VM(9) |
| I10 | `U-071 S2-I10: GreenMail 2.1.15` | `app/build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(10); VM(10) |
| I11 | `U-071 S2-I11: auto-service 1.1.1` | `app/build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(11); VM(11) |
| I12 | `U-071 S2-I12: JaCoCo 0.8.16` | `app/build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(12); VM(12); `$W/evidence/I12-{lint.txt,lint-ids.txt}` |
| I13 | `U-071 S2-I13: CI workflow on checkout v7, setup-java v6, cache v6` | `.github/workflows/ci.yml`; LOG; `$W/evidence/I13-{checks.txt,ci-parity.txt,ci-coverage.txt,warnings.txt,lint-ids.txt,yaml.txt}` |
| I14 | `U-071 S2-I14: version comments and code-location for the new toolchain` | `build.gradle`; `app/build.gradle`; `sdlc/artifacts/engagement/code-location.md`; LOG; `$W/evidence/compatibility-record.md`; STD(14); `$W/evidence/I14-{gradle-version.txt,deps-releaseRuntime.txt,deps-releaseRuntime-tree.txt,deps-debugUnitTestRuntime.txt,vm-proof.txt,vm-added.txt,sweep.txt,final-checks.txt,scan.txt}` |
| fix | `U-071 S2-I<k> fix: <what>` | only the files the fix touches, plus LOG and its evidence (row D91) |

Every name in a list is written by a step above (STD by EV-T, EV-B, CMP, CK; VM by VMP; the rest named in the steps).
The story file, the sprint plan, `plan.md`, the plan-gate reports, `compiled-pipeline.json`, `$SUPP` and the
checkout's other orchestrator files are never staged by the developer. `.claude/CLAUDE.md` is never edited by the
developer (8, item H-3).

## 5. Tests and Counts

- **Added, deleted, changed:** none. The probe is copied in and deleted twice and never committed (4.P). The count at
  every commit equals `C0`; the end count is taken with `--untracked` before the I14 commit, and with the last commit
  in the hand-off (AC-19; REQ-015 AC-13 as amended by CR-016 Amendment 1).
- **Foreign tests:** tests added by U-069, U-070 and U-074 land before `<start>` and are inside `C0` (their commit ids
  and counts are recorded under SC-9); a test added by any other commit after `<start>` is listed with its commit id and
  not counted (I14 step 7 (i) and (iii)). None is expected.
- **Totals:** from the build's own JUnit XML (`junit_totals.py`; JUnit XML is on since U-068 I1); after every increment
  equal to I0's (tests, failures 0, errors 0, skipped equal to the start and not above 2); the Gradle total equals `C0`
  (row D48).
- **Case list:** the full `junit_cases.py` list equals I0's at every increment (row D49): no case renamed, lost or
  newly skipped by a toolchain change.
- **Mutation runs:** six at I5 (4.M), each a targeted run of the three NEW3 classes.

## 6. Decision Table

| Row | Situation | Decision |
|-----|-----------|----------|
| D1 | `$SUPP` missing or lacking any of Q-1 to Q-9 at I0 step 1 | Stop before I0 step 3 (no Gradle run); report the missing items |
| D2 | A story of SC-1/SC-9 (U-067, U-068, U-069, U-070) or U-074 is not `done`, or `c12a2788` is not an ancestor | Stop (SC-1, SC-9; U-074 edits the files I5 mutates) |
| D3 | An SC-2 check fails (minSdk, merged manifest, `junitXml`, the receiver test, the grep, the metadata diff since `$U068_START`) | Stop (SC-2) |
| D4 | CR-015 or CR-016 not `implemented` | Stop (SC-3) |
| D5 | Build-file drift since PLAN_HEAD other than `2 2 app/build.gradle` at `:579`/`:586`, or a code-location hunk other than `:28` | Stop and report: an edit text of section 3 may have moved; the SA re-derives |
| D6 | `oldtext_check.py` reports `MISMATCH` ("old text not found exactly once") | Stop and report with the failing ids; no edit is attempted |
| D7 | Porcelain on the story's paths not empty at I0, or a Gradle JVM is live | Stop; wait or ask the orchestrator (SC-5) |
| D8 | `C0` cannot be computed, or `@Ignore` lines differ from the two known | Record; stop if a new `@Ignore` appeared (not this story's) |
| D9 | G(0) fails | Stop: the start is not green (not this story's to fix) |
| D10 | Lint at I0 exits 0, or its rule IDs differ from U-068 I6's | Record (lint is recorded, not gated, D17) |
| D11 | Verification counts differ from 803/1460/3 | Record; if `git diff $U068_START HEAD -- gradle/verification-metadata.xml` is not empty, stop (SC-2) |
| D12 | The probe does not compile or run at I0 | Record "probe unavailable", delete it (4.P step 5), continue; at I7 AC-12 (d) rests on the exact-list tests and their failure messages |
| D13 | A supplied quotation no longer says what the research says | Record verbatim; if a constraint row no longer holds (for example AGP 9.5's minimum Gradle above 9.9.2), stop before I1 (D1a rule 3 / D3) |
| D14 | The supplement reports a newer non-pre-release for a row | Patch: substitute the version token (2.1), re-check that component's constraint rows, record; minor or major: stop before I1 for an SA amendment |
| D15 | W1 exits non-zero | Stop; `git restore --source=HEAD --worktree -- gradle/wrapper gradlew gradlew.bat`; report verbatim |
| D16 | The generated file after a wrapper run still contains the block (`grep -c 'LOCAL ADDITION' gradlew` is not `0`) | Unexpected generator behaviour: E1 is not applied; restore the wrapper files from HEAD (as D15); stop and report |
| D17 | `gradlew_block.py` reports `FAIL` | Stop; restore the wrapper files from HEAD (as D15); report which check failed (N2) |
| D18 | The first 9.9.2 run reports a distribution checksum failure, or `dist-sha-match=no` | Stop (INT-002): never edit the checksum; restore the wrapper files; report |
| D19 | "Unable to establish loopback connection" after a block re-application | Stop: the block is not effective; report (Risk 8) |
| D20 | The distribution download fails (network) | Retry the VER once; a second failure stops |
| D21 | W2 or any 9.9.2 run fails configuring the build (for example `updateDaemonJvm`) | Stop: a Gradle error outside F1 to F5; SA amendment |
| D22 | The jar SHA-256 is not Q-2 | Stop; do not commit; report |
| D23 | The properties diff has lines beyond `:3`/`:4` | Record each generated line; stop only if `distributionSha256Sum` or `validateDistributionUrl=true` is missing |
| D24 | `gradle/gradle-daemon-jvm.properties` changed | Stop; report (it must not change) |
| D25 | G(1) fails for a missing verification entry | VMP(1) (Scope Concern 3; additions-only conditions as written), then G(1) |
| D26 | G(1) fails otherwise | Stop; report verbatim |
| D27 | The writer pass shows a failed task other than `:app:lintDebug` | Classify by D34 to D37; nothing is committed |
| D28 | The proof shows a changed configuration block, a removed or altered entry, or `trust` ≠ 3 | Stop; restore the file from HEAD; never hand-edit; report (also when the change is only re-serialisation of the configuration block) |
| D29 | The proof shows 0 added entries at a dependency increment | Stop: check that the edit took effect; report |
| D30 | An added component outside the expected groups | Flag in the log with its likely origin (D5 step 5); continue unless D31 applies |
| D31 | A resolved `releaseRuntimeClasspath` version of an S4-owned library changed (I4, I5, I6, I7, I14) | Stop: it would be an S4 change made silently (AC-17 (e)) |
| D32 | ENF fails for a missing entry after the writer pass | Stop: the pass did not cover a configuration; report; the pass is not repeated on a guess |
| D33 | A checksum mismatch on an unchanged artifact | Stop (INT-001: possible tampering); never re-written |
| D34 | A compile error or a javac `-Werror` warning in `:app` main code | Record verbatim; apply only the pre-specified forced edits (D55 to D58); any other call-site fix stops for an SA-supplied exact edit (SC-P4) |
| D35 | An error from KGP, KSP, the Hilt plugin or AGP | Identify the fallback (2.4); stop and report (ruling); resume only on the orchestrator's authorisation |
| D36 | An error outside D34, D35 and every other row | Stop; SA amendment |
| D37 | A test fails at a gate | Record the case and message verbatim; stop; never edit or skip a test (Scope Concern 4); TLS cases go to D72 |
| D38 | `jacocoTestCoverageVerification` fails or a package is `BELOW` | Stop; never edit a floor; report with `I<k>-coverage.txt` and the `jacoco-classes` diff; a coverage-restoring test is an SA amendment (AC-20 (c)) |
| D39 | Coverage changed but every package above its floor | Record the per-class difference; `activity` alternation (2.6) is noise; any other change is explained from per-class/per-line evidence or reported |
| D40 | Class inventory or a package's `t` changed outside what 2.9 allows | Stop and report (I12: AC-14) |
| D41 | A merged or packaged manifest differs from I0, or an invariant count is wrong | Stop and investigate; report (AC-21 (a)) |
| D42 | The kept-name set differs | Stop and report (AC-21 (b)) |
| D43 | `configuration.txt` differs | Stop and report with each differing rule and the section (source file) it comes from (strict reading; SC-P1 may give a standing ruling) |
| D44 | APK entry lists differ | Record; each entry explained by the increment's component (2.9); an unexplained entry stops |
| D45 | `so` ≠ 0 | Stop |
| D46 | A class-file major other than 61, 61, 52 | Stop (AC-15); record the forcing error if any |
| D47 | The KSP list differs where 2.9 says "=" | Stop and report; at I4 each difference is explained or the story stops |
| D48 | JUnit totals differ from I0, or skipped rises | Stop and report (AC-19) |
| D49 | The case list differs from I0 | Stop and report (a renamed, lost or newly skipped case) |
| D50 | An error-class item in the warnings | Stop; warnings are debt only |
| D51 | `lintVitalRelease` fails `assembleRelease` on the new AGP | Stop: the fix would edit `lint.xml`, the baseline or source; SA amendment |
| D52 | "bash.exe not found", a vanished tool, or a process killed without a visible reason | Stop and report (endpoint security); no workaround, nothing restored from quarantine, no exclusion |
| D53 | The harness moves a Gradle run to the background | Wait for its completion notice; never poll; start nothing else |
| D54 | At I2, a KSP-originated error that names the Kotlin version, KGP or Kotlin metadata | The pre-specified reorder (AC-7): no I2 commit; E2 moves into I5; recorded, not a fallback. Any other KSP error at I2 is F3 (D35) |
| D55 | At I3, a JaCoCo class root missing or empty after `clean` | `find app/build/intermediates -type d -path '*debug*' -name classes` and the like; update that one `jacocoClassRootPaths` line (3.2); guard unchanged; recorded as a forced change |
| D56 | At I3, AGP rejects the `*SdkVersion` DSL names | Apply the A13 edits for `:app` (3.2), values unchanged; if `:k9mail-vendored` is rejected too, stop (AC-23 forbids its edit) |
| D57 | `receiver` at or below 0.50 at I3 | Stop (AC-8 (d)); report |
| D58 | At I5, a Hilt or KSP error naming `@ApplicationContext` or a qualifier at one of the four Risk 9 sites | Apply the four `@param:` edits (3.2), record the error verbatim as a forced change, re-run from VMP step 1 |
| D59 | At I4, `hiltJavaCompileDebug`, the ASM transform or a Hilt test fails | Hilt-plugin origin → D35; otherwise D36 |
| D60 | `worker` below 0.95 (a generated class reports lines, or a counting change) | Stop; never lower the floor; report with the per-class lines (L17: zero spare lines) |
| D61 | F-1 stop rule fails (a NEW3 case fails, `app/src/test` differs, or a `coroutineContext` warning in either worker) | Stop and report before the mutations |
| D62 | A mutation's must-fail test passes | Finding "AC-10 unmet for <id>"; finish the remaining entries; stop before CM(5) and report (I5 stays uncommitted, every mutation restored) |
| D63 | A mutant does not compile | Record "invalid"; restore; stop and report |
| D64 | An original that must pass fails, or a later case fails under a mutation of another loop | Restore; stop and report |
| D65 | At I5, a KGP/KSP error naming a Gradle or AGP API (F2), or Hilt/KSP rejecting Kotlin 2.4 metadata (F4) | D35 |
| D66 | At I6, `ic1_…` fails (androidx.hilt renamed a generated class or method) | Stop; the fix is an SA amendment, never a silent test edit (delegation item 5) |
| D67 | At I6, `WorkManagerSchedulerTest` or a worker test fails | Stop and report (a `HiltWorkerFactory` regression points here) |
| D68 | A14: `@LooperMode(LEGACY)` or `@ConscryptMode(OFF)` no longer compiles | Stop (a test edit would be needed); report; not F5 (not a JDK-flag case) |
| D69 | D7 scan has a hit | Apply E7.3 with exactly the named `--add-opens` (one per distinct `module/package`), re-run G(7) once; if the suite still cannot run on JDK 25, F5 (D35) |
| D70 | The isolation guards (`A1-G1`, `A1-G3`) fail | Stop and report (A14; U-063 deviation 14) |
| D71 | `robolectric.properties` differs | Stop (it is not edited by this story) |
| D72 | A TLS case fails, or U-066's exact-list tests fail | Record both lists (the probe's before/after and Truth's message); never loosen; stop and report so CR-014's applier check is re-confirmed (In-flight work) |
| D73 | An `android-all` download fails | Retry once (a repeat of the run that needed it), record; a second failure stops; never skip a test, never fetch by hand |
| D74 | Any failure caused by compileSdk 35 being too low (an AAR `minCompileSdk` above 35, a missing platform API), here or later | N-6: stop at that increment, record verbatim, report; U-072's S3-I0, S3-I1, S3-I2, S3-I4 run, then this increment re-runs in full from VMP step 1 |
| D75 | A runtime-classpath or test-classpath change at a test-only increment (I8 to I12) | Release: D31; test: record, explain |
| D76 | At I8, ByteBuddy resolved at a version other than 1.17.7, or Q-9 says Java 25 is unsupported | Record; adapt E14.10's version; unsupported → stop and report |
| D77 | At I10, `BackupImapStoreDelegateIntegrationTest` fails | Stop and report |
| D78 | At I11, a test-compile error from the annotation processor | Stop and report |
| D79 | At I12, the inventory or class names differ from I11 | Stop and report (AC-14) |
| D80 | At I12, a package `BELOW` | D38 |
| D81 | At I12, "Unsupported class file major version" | Stop and report |
| D82 | The YAML parse fails or prints other values | Restore `ci.yml` from HEAD (`git restore --source=HEAD --worktree -- .github/workflows/ci.yml`) and re-apply E13; a second failure stops |
| D83 | Q-10 missing, or says `ubuntu-22.04` is retired | Missing: wait for it before I13. Retired: record; changing `runs-on` is not pre-specified, stop for an SA amendment |
| D84 | The parity run fails in a step other than lint | Stop and report |
| D85 | The coverage step of the parity run fails | D38 |
| D86 | The index is not clean before `git add` | Stop: someone else staged something |
| D87 | A listed file does not exist | Stop; find the step that did not write it |
| D88 | `scan.py` reports a hit | Fix the evidence (re-sanitise from the raw log); never commit a hit |
| D89 | A final check of I14 step 7 is not as expected | Stop and report (scoped: an orchestrator document commit is recorded, never a reason to stop) |
| D90 | The resume check (4.R) differs from the log | Stop and report, except the documented recoveries |
| D91 | A check fails after a commit because of an earlier, committed increment | A further commit `U-071 S2-I<k> fix: …` with only the fix's files; never amend, reset or rebase |

## 7. Session Plan

Six developer sessions, each ending at a clean hand-over state: every increment it touched committed, nothing
half-edited, no mutation or probe in place, `gradle/verification-metadata.xml` equal to HEAD, no Gradle JVM, and the
log's last line naming the next step ("Next: I<k> step 1"). Each session after the first starts with 4.R.

| Session | Increments | Gradle runs (planned) | Compute estimate | Why the boundary is here |
|---------|------------|-----------------------|------------------|--------------------------|
| A | I0, I1, I2 | 13 | about 1 h 45 min | Baseline and the two changes AGP 9.5 needs first; ends on a Gradle 9.9.2 + KSP 2.3.14 tree |
| B | I3, I4 | 9 | about 1 h 30 min | AGP is the largest dependency change (G1, G5, A13, C-1/C-2, likely SC-P1 stop) |
| C | I5 | 11 | about 1 h 45 min | Kotlin plus six mutations: the session must not end with an `APPLYING` entry |
| D | I6, I7 | 9 | about 1 h 30 min | androidx.hilt and Robolectric: the TLS and probe work needs a fresh context |
| E | I8, I9, I10, I11 | 12 | about 1 h 45 min | Four low-risk bumps of the same shape |
| F | I12, I13, I14 | 9 | about 1 h 30 min | JaCoCo last among build changes, then CI, documents and the final checks |

Estimates: G about 9 to 11 min (with `clean`), WP 12 to 16 min, ENF 2 to 3, L 3, KC 2, DEP 1 to 2, TR 2 to 3,
VER under 1 (the first 9.9.2 one downloads the distribution). A stop (any "stop" row) ends the session at once with the
working changes either restored (fallbacks) or left as the row says, and the log says which. A session may end earlier
at any increment boundary.

## 8. Items for the Orchestrator (hand-over)

1. **H-1, before the build:** the supplement `$SUPP` with Q-1 to Q-9 (Q-10 before I13; Q-11, Q-12 optional), 2.3.
2. **H-2, rulings requested on the plan's Scope Concerns SC-P1 to SC-P5** (9.2). Each has a default the plan follows
   without a ruling, so none blocks the start; SC-P1 in particular would turn up to four likely stops into one decision.
3. **H-3, `.claude/CLAUDE.md`** (Engine Librarian; the developer does not edit it). No gate text changes, so AC-16 (d)
   needs no edit. Optional, if I1 confirms the two-run behaviour (L5): in the "Local Build & Device Notes" bullet
   "Build JDK", replace "`gradlew` carries a hand-maintained Windows `JAVA_TOOL_OPTIONS` block; re-apply it after any
   `./gradlew wrapper` regeneration." with "`gradlew` carries a hand-maintained Windows `JAVA_TOOL_OPTIONS` block;
   re-apply it after every `./gradlew wrapper` run (a version upgrade runs the task twice, on the old and then on the
   new Gradle, and each run drops the block)."
4. **H-4, fallbacks:** if one fires, the developer stops; the orchestrator records the product-owner flag (F2, F4 with
   their Part B consequence, verbatim from 2.4) and authorises resumption with the inputs 2.3 names.
5. **H-5, devices:** IC-4 is the orchestrator's (10.1); IC-5 and IC-6 are recorded "not run: needs the product owner's
   decision" (10.2), and the questions PO-A, PO-B, PO-G, PO-H are put when the orchestrator chooses, no later than S2-I15.
6. **H-6, for U-072 (S3):** the AGP 9.5.2 lint rule-ID table of I12 and I13 is S3-I0's "before"; `robolectric.properties`'
   comment (S3-I3); `ci.yml` lines move by one from `:21` on after I13 (the comment gains a line); Build Tools stays
   36.0.0; the N-6 move if D74 fires.
7. **H-7, for U-073 (S4):** `I14-deps-releaseRuntime.txt` is S4's baseline; the `worker` floor has zero spare lines
   (L17); the Mockito JEP 451 warning (I8) is debt; the test-classpath versions Robolectric 4.18.1 pulls (I7); U-069's
   recommendation to re-run its mutations at S4-W; the F-1 rule at S4-C and S4-W.
8. **H-8, follow-ups outside this story:** `DEVELOPMENT.md:25-30` and `:49` still describe `org.gradle.java.home` and
   "pins JDK 17" (stale since U-060); `DefaultTrustedSocketFactoryJdkProviderTest.java:60` names Robolectric 4.15.1
   (left, Scope Concern 4); `code-location.md` "Key Dependencies" lists libraries removed long ago (otto,
   firebase-jobdispatcher, the JitPack k-9 coordinate); pinning the GitHub actions by commit SHA (design security
   section, a recommendation).
9. **H-9, a correction for U-069's record:** its rationale that a 0.95 floor under a measured 1.00 lets "a JaCoCo
   line-counting change … not fail the gate" holds only from 20 lines up; `worker` has 16 (L17). Not this story's to
   change (no floor is touched); I12's stop rule covers it.
10. **H-10, IC-1/IC-2 reading recorded:** AC-6 (f)/IC-2 "no `org.gradle.java.home` anywhere in the repository" is read
    as "not set in any properties, Gradle, wrapper or CI file" because the phrase appears in prose (L22) (SC-P5).

## 9. Contracts, Scope Concerns, Risks, Out of Scope

### 9.1 Contracts (approved; read, not edited)

- **CNTR-MODERNIZATION-001** (TLS socket factories): touched by I7 in the test provider only; production TLS code is
  not edited; U-066's exact-list tests must pass unchanged (row D72); no revision needed.
- **CNTR-MODERNIZATION-004** (scheduler, v3) and **-005** (the `BACKUP` broadcast, frozen): the `HiltWorkerFactory`
  path is guarded at I6 (D66, D67), the `BackupBroadcastReceiver` manifest entry by the invariants at every increment
  (2.9); no revision.
- **CNTR-MODERNIZATION-003** and **-006**: their runtime libraries are S4's and stay at today's resolved versions
  (D31); no revision.
- No new boundary is introduced, so no new contract is needed (contract gate satisfied).

### 9.2 Scope Concerns of this plan (each with the default the plan follows)

| Id | Concern | Default without a ruling | Ruling asked |
|----|---------|--------------------------|--------------|
| SC-P1 | AC-8 (c) and AC-21 (b) require `configuration.txt` equal; AGP 9.5's default ProGuard file and the bumped libraries' consumer rules will probably change it at I3 to I6 (P2) | Stop and report at each such increment (D43) | A standing ruling: a difference is accepted as "investigated" when the kept-name set is equal and every added or removed rule lies in a section whose source is the increment's own component (AGP's default file at I3; that library's consumer rules at I4 to I6), each rule listed in the log |
| SC-P2 | D7 item 5 and AC-12 (c) ask for "environment probes (U-063 IC-11)", which U-063 ran as a throwaway, uncommitted test | The 4.P probe at I0 and I7, copied in and deleted under a write-ahead protocol | Confirm, or rule "no probe" (AC-12 (d) then rests on the tests' own messages) |
| SC-P3 | AC-21 (d) says the KSP list equals AC-1 (g) at I4, I5, I6 and the end, while AC-9 allows explained differences at I4 | From I5 on the list must equal I4's; I4's explained differences carry forward | Confirm |
| SC-P4 | D3 lets the developer fix a javac `-Werror` warning at its call site; the delegation asks for exact text for every edit | Only the pre-specified forced edits are applied; any other forced production fix stops for an SA-supplied exact edit (D34) | Confirm, or allow the developer one-line call-site fixes recorded verbatim |
| SC-P5 | AC-6 (f) and IC-2: "no `org.gradle.java.home` appears anywhere in the repository"; the phrase appears in `CLAUDE.md` and `DEVELOPMENT.md` prose (L22) | Read as "not set in any properties, Gradle, wrapper or CI file" (`java-home-set-exit=1`) | Confirm |

### 9.3 Risks (likelihood / impact)

| # | Risk | L / I | Mitigation | Detection |
|---|------|-------|------------|-----------|
| R1 | The set fails outside the vendors' ranges (G1 to G6) | Medium / High (effort) | One component per increment; F1 to F5; stop-and-report | Gates, warnings, KSP list (D35) |
| R2 | `configuration.txt` differs at I3 to I6 and each difference stops the story | High / Medium (delay) | SC-P1 asks for one standing ruling | CMP (D43) |
| R3 | `worker` fails with zero spare lines | Medium / Medium | Per-class lines at every increment; stop, never lower | `cov_floors.py` (D60) |
| R4 | Robolectric 4.18.1's Conscrypt changes the TLS results | Medium / Medium | Probe before/after; tests never loosened | D72 |
| R5 | The writer hides an unexpected artifact | Low / High (supply chain) | Pristine restore, one pass, semantic proof, group review | VMP (D28 to D30) |
| R6 | The Windows block is lost | Certain if forgotten / High | E1 after each wrapper run; the check tool; VER at once | D17, D19 |
| R7 | Kotlin 2.4 turns the four warnings into a Hilt qualifier error (Risk 9) | Low / Medium | Pre-specified `@param:` edits | D58 |
| R8 | Cancellation detection weakens under Kotlin 2.4 | Low / Medium | F-1 rule; six mutations | D61 to D64 |
| R9 | A javac `-Werror` warning from the new toolchain | Medium / Low | Recorded; SA-supplied exact fix | D34 |
| R10 | `lintVitalRelease` fatal on AGP 9.5's lint | Low / Medium | Stop for an SA amendment | D51 |
| R11 | An endpoint-security kill mid-step (mutation, writer pass, wrapper) | Medium / Medium | Write-ahead lines, 4.R recoveries, single-path restores | D52, D90 |
| R12 | The story outlasts one session's context | High / Medium | Six sessions with clean boundaries (7) | 4.R |
| R13 | A download fails (distribution, plugins, `android-all`) | Low / Low | One retry; never a hand download | D20, D73 |
| R14 | The N-6 move pauses the story | Low / Medium | Pre-specified; U-072 must be ready | D74 |
| R15 | Hilt 2.60.1 or androidx.hilt 1.4.0 raises an S4-owned resolved version | Low / Medium | DEP at I4, I6 | D31 |
| R16 | The supplement is late or incomplete | Medium / Low (delay) | Required before I0 step 3 | D1 |
| R17 | AGP 9.5's unit-test manifest shifts `receiver` coverage | Low / Medium | 10 spare lines after U-068 | D57 |
| R18 | Mockito's dynamic agent is refused | Low (JDK 25 warns only) / Medium | Recorded as debt | I8 |

### 9.4 Out of scope

S1 (done), S3 (except N-6), S4 (every runtime library, including JUnit's status record), S5, BUG-021 and BUG-023,
`k9mail-vendored/`, the JDK, overriding undeclared transitive versions, replacing `security-crypto`, pinning actions by
SHA, running Mockito as `-javaagent`, fixing the pre-existing lint errors, publishing and rollout.

## 10. Device and CI Criteria (for the orchestrator; the developer runs none of this)

### 10.1 IC-4 — launch smoke on `Pixel_9_Pro` (S2-I15; authorised 2026-11-20, "Start it for me")

After the developer's I14 commit, with the debug APK built from it (`app/build/outputs/apk/debug/app-debug.apk` of
G(14); if the orchestrator is unsure, it runs `./gradlew :app:assembleDebug` itself, in a shell without
`MSYS_NO_PATHCONV`). Text evidence only, sanitised, into `$E/IC4-launch.txt` (the orchestrator commits it).
1. `emulator -list-avds` lists `Pixel_9_Pro`; start only that AVD if no device is running. `Pixel_10_Pro` is never
   started, listed for use or touched.
2. `adb devices` → the serial; `adb -s <serial> emu avd name` → first line `Pixel_9_Pro`, else stop;
   `adb -s <serial> shell getprop ro.build.version.sdk` → `36`.
3. Read-only pre-check: `adb -s <serial> shell run-as com.zegoggles.smssync cat shared_prefs/com.zegoggles.smssync_preferences.xml | grep -c 'name="third_party_integration" value="true"'`
   → `0` (integration off, so no backup can start). `1` or an unreadable file: stop and ask the owner (changing the
   setting is PO-3).
4. `adb -s <serial> shell dumpsys package com.zegoggles.smssync | grep -E 'versionCode|versionName'` (1602 expected);
   device time `adb -s <serial> shell date +'%m-%d %H:%M:%S.000'` as `T`.
5. `adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk` → `Success`. `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
   or any failure: stop (never uninstall, never `pm clear`).
6. `adb -s <serial> shell am start -W -n com.zegoggles.smssync/.activity.MainActivity` → `Status: ok`;
   `adb -s <serial> shell pidof com.zegoggles.smssync` → a process id.
7. `adb -s <serial> logcat -d -v threadtime -T "<T>"` into the work folder; counts of `FATAL EXCEPTION`, Hilt
   initialisation errors (`Hilt`, `dagger`), `IllegalArgumentException`, `SecurityException`,
   `ForegroundServiceStartNotAllowedException` on lines of the app's process id → all `0`.
8. No preference is changed, nothing is tapped; `adb -s <serial> emu kill`. `MSYS_NO_PATHCONV=1` only in this adb
   shell, never for Gradle.
What it proves: the new toolchain's build installs over the configured app, starts and initialises Hilt on Android 16.
What it does not prove (not claimed): that a backup or restore completes, scheduling, any level near the floor.

### 10.2 IC-5 and IC-6 — recorded without the product owner's decision

- **IC-5:** "not run: needs the product owner's decision (PO-A, PO-G; design PO-2 (a), (e), PO-3)". Stands instead: the
  unit suite at SDK 24 and 29 on Robolectric 4.18.1 (I7), the manifests reporting minSdk 24 (every CMP), IC-4 at API
  36. Not proven: a backup and restore on the new toolchain's build on any device; anything about API 24 on a real
  system. REQ-015 AC-17 is **not met** until run. `Pixel_4_2` is not started (PO-H).
- **IC-6:** "not reached: needs the product owner's decision (PO-B; design PO-4)". Stands instead: the YAML parses (I13
  step 5) and the local parity run reached every step (I13 steps 3, 4). Not proven: that the Linux runner builds, that
  the new action majors run, that `sdkmanager` works there. REQ-015 AC-18 is **not met** until run. Nothing claims CI
  has run.

## 11. Constraints for the Developer (binding)

1. Run Gradle in the foreground with the tool timeout at 600000 ms on every call, never with `MSYS_NO_PATHCONV=1`, never
   while another Gradle run is live.
2. If the tool moves a run to the background by itself, wait for its completion notice; never poll; start nothing else.
3. No background or detached shells, no `&`, no `sleep` or polling loops, no helper agents for shell work, no heredocs
   for editing (Write and Edit tools only); keep shell activity modest (no loop that starts hundreds of processes).
4. Never call `diff` or `cmp`; compare with `git -c core.autocrlf=false diff --no-index` (`--quiet`, `--numstat`, `--stat`)
   or `sha256sum`. On "bash.exe … not found", a vanished tool or a process killed without a visible reason: stop and
   report; restore nothing from quarantine, add no exclusion, work around nothing.
5. Raw logs, copies, pristine files and commit messages stay in the work folder outside the repository; only filtered
   evidence (via `sanitize.py`) enters `$W`; every evidence file passes `scan.py` before `git add`.
6. Timestamps from `date -u`, never typed. No e-mail address, host name, user name or user-profile path in any
   repository file.
7. **Commits:** one local commit per increment by CM with the subject and explicit list of 4.9, on
   `sdlc/modernization-plan`; never `git add -A`, `.`, `-u`, `git commit -a`; never push, amend, rebase, reset, revert or
   force-push; a failure after a commit is a further fix commit (D91). The orchestrator pushes after the reviews.
8. Edit only what section 3 lists (and the pre-specified forced edits under their rows); `gradlew.bat` and every
   generated wrapper file are the generator's; no edit to `gradle/gradle-daemon-jvm.properties`, `settings.gradle`,
   `gradle.properties`, `k9mail-vendored/`, `app/lint.xml`, `app/lint-baseline.xml`, `app/proguard-rules.pro`,
   `app/src/main/AndroidManifest.xml`, any file under `app/src/test` (the probe is copied in and deleted, never edited
   in place or committed), `.claude/CLAUDE.md`.
9. Verification stays on: no `<trust>` entry, no `verify-metadata` change, no hand-edited checksum, no wholesale
   regeneration, no hand-deleted entry; only VMP writes the file.
10. No `@Ignore` or `Assume`; no assertion loosened; no JaCoCo `minimum`, `limit`, `value`, `includes` or `excludes`
    changed; no floor lowered; `-Werror` never removed; no `-Xlint:-…` suppression.
11. Nothing downloaded by hand from anywhere; the build's own downloads from its configured repositories, the wrapper's
    distribution download and Robolectric's resolver are the normal path.
12. No device, emulator or adb command (section 10 is the orchestrator's). `Pixel_10_Pro` is never touched.
13. Do not touch the other work in the checkout: `sdlc/.skill-*`; other stories' folders and files; `sdlc/artifacts/stories/**`;
    the decision appendix; any design, requirement, contract or change record; the story file, this plan and the gate
    reports. The orchestrator may commit documents while you run; changes outside this story's paths are recorded if
    noticed and are never a reason to stop.
14. A fallback trigger, a "stop" row or anything the plan does not cover: stop, record verbatim, report. Never decide a
    scope question yourself.

## 12. Quality Attribute Coverage (ISO/IEC 25010)

| Attribute | How this story serves it |
|-----------|--------------------------|
| Functional suitability | No production behaviour changes (no `app/src/main` edit expected); correctness is guarded by the unchanged suite, the case list equal at every increment, the F-1 rule and six mutations (I5), U-066's exact-list tests (I7) and the manifest invariants of CNTR-005 and BUG-016 (2.9) |
| Performance efficiency | Runtime: none expected (R8 input and kept names compared at every increment). Build: about 63 Gradle runs over six sessions; Robolectric 4.18.1 may fetch new `android-all` jars; the verification file grows by an expected several hundred entries; test sandboxes stay at SDK 29 (D6), so no new per-fork memory for a higher level |
| Compatibility | Co-existence with the toolchain the later stories and the Thunderbird libraries need (AGP 9.5 for compileSdk 37, Kotlin 2.4 for Part B); interoperability of Gradle/AGP/KGP outside the vendors' joint ranges is recorded as G1 to G6 with detectors; the debug build installs over the configured app (IC-4) |
| Usability | Not applicable to this story's scope (no UI or user-facing text changes) |
| Reliability | Recoverability: one commit per increment, revert newest-first (D19), the wrapper block restored with a reverted wrapper commit; fault tolerance of the process: write-ahead lines, single-path restores, the resume protocol; maturity: gates and fitness checks at every step |
| Security | Supply chain: verification enforced with an additions-only semantic proof and a group review per increment; wrapper distribution pinned by SHA-256 and the jar compared with its published hash; Gradle 9.9.2's dependency-verification fix; actions move to current majors (SHA pinning recommended, H-8); `android-all` jars recorded with SHA-512 against their checksum files; no secret touched; evidence hygiene by `sanitize.py` and `scan.py` |
| Maintainability | Analysability: one version per commit, a compatibility record that tells a future upgrade an expected warning from a defect, stale comments corrected (I14); modifiability: unblocks S3, S4 and Part B; testability: the probe and the per-class coverage records |
| Portability | JDK 25 kept; the Windows `gradlew` block preserved and proven; the CI Linux runner checked by Q-10 and the parity run (IC-6 not claimed); bytecode targets 17 and 8 unchanged |

## 13. Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-071 story | `sdlc/artifacts/build/sprints/sprint-025/U-071-build-toolchain-and-test-tooling-latest-stable.md` | Read in full: SC-1 to SC-9, AC-1 to AC-24, IC-1 to IC-6, targets, Affected Code, Technical Context, Scope Concerns, rulings |
| DES-MODERNIZATION-014 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-014-toolchain-runtime-libraries-platform-level-design.md` | Assumption Ledger `:228-256`; D1 and D1a `:351-460`; D2 S1/S2 and N-6 `:532-596`; D3 `:701-793`; D5 to D10 `:882-1140`; D16 to D19 `:1543-1692`; cross-cutting, quality, integration `:1694-1786`; risks, open items, trade-offs `:2027-2132` |
| REQ-MODERNIZATION-015 | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-015-toolchain-and-test-tooling-latest-stable-minsdk-23.md` | Read in full (AC-13 as amended) |
| CR-015, CR-016 | `sdlc/artifacts/build/change-records/CR-015-*.md`, `CR-016-*.md` | Status (both `implemented`); CR-016 Amendment 1 `:537-588` |
| Research | `sdlc/artifacts/build/research/2026-11-21-latest-stable-versions.md` | Read in full: versions, C1 to C21, §3.2, §6 |
| REQ type standard | `sdlc/artifacts/requirements/` | None with `type: standard` (as U-068 found; no new file since) |
| U-068 plan, review, log, evidence | `sdlc/artifacts/build/sprints/sprint-024/U-068/{plan.md,plan-review.md,implementation-log.md}`, `evidence/I5-*`, `evidence/tools/*` | Model; blockers to avoid; measured figures (L14); invariant values; tools |
| U-069 plan | `sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md` (`:40`, `:61`, `:944-966`, `:785-811`, `:1505-1526`) | Rule 15 edits, `ic1_…`, the worker floor |
| U-070 plan, U-074 story | `sdlc/artifacts/build/sprints/sprint-024/U-070/plan.md` (searched); `sdlc/artifacts/build/sprints/sprint-025/U-074-cancelled-run-is-not-reported-as-a-failure.md` (AC-2, AC-3, AC-11, AC-16, Affected Code) | Overlap with NEW3 and the build files |
| U-067 plan, mutation log | `sdlc/artifacts/build/sprints/sprint-023/U-067/plan.md` (`:296-316`, `:753-972`), `evidence/mutation-log.md`, `evidence/I7-kotlinc.txt` | Mutation texts, expected sets, KC form, warnings |
| U-060 plan, log, retro, evidence | `sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md` (`:59`, `:637-746`), `implementation-log.md`, `sprint-017/retro.md`, `evidence/{after-ksp-files.txt,after-deps-releaseRuntime.txt,c2-configuration-diff.txt}` | Wrapper, block, writer, proof script, KSP and classpath methods |
| U-063 tools and story | `sdlc/artifacts/build/sprints/sprint-020/U-063/evidence/tools/*` (five read), `U-063-tls-socket-factory-characterisation-tests.md` (IC-11) | Tools; the probe form |
| Build files (committed blobs) | `build.gradle`, `app/build.gradle`, `gradle/wrapper/gradle-wrapper.properties`, `gradlew` (`:80-160`), `.github/workflows/ci.yml`, `gradle.properties`, `gradle/gradle-daemon-jvm.properties`, `settings.gradle`, `k9mail-vendored/build.gradle` (searched), `metadata/build.gradle`, `app/src/test/resources/robolectric.properties`, `gradle/verification-metadata.xml` (header, counts) | Read via `git show` at `c8e98a7f`/HEAD (L1) |
| Sources | `service/BackupWorker.kt`, `RestoreWorker.kt` (check lines), the four Risk 9 files (the lines), `DefaultTrustedSocketFactoryJdkProviderTest.java` (`:140-200`), TLS test names (searched) | Mutation anchors, Risk 9, TLS guards |
| Documents | `sdlc/artifacts/engagement/code-location.md` (full), `.claude/CLAUDE.md:31` | I14 edits; H-3 |
| Local caches | the Gradle module cache and wrapper distributions (listings); cached `monitor-1.8.0.aar`, `mockito-core-5.24.0.pom`, `hilt-android-2.60.1.pom`, `hilt-work-1.2.0.module` | L12, L13 (no path recorded) |
| Knowledge | plugin cache `knowledge/standards/expert-exceeding-depth.md`, `truth-and-accuracy.md` (preloaded), `templates/build-artifacts/build-plan.md` | Quality bar, template |

## 14. Criterion-to-Step Map

| Criterion | Step | Evidence |
|-----------|------|----------|
| SC-1 to SC-9 | I0 step 1 | `I0-start.txt`, log |
| AC-1 (a)-(l) | I0 steps 1-10 | `I0-*` (gates, `gradle-version`, count, totals, coverage and inventory, manifests, kept names and configuration, KSP list, lint and baseline count, apk (`so`), javap, deps, vm-counts) |
| AC-2 | 2.1; I0 step 11 | log (D1a table); `$SUPP` |
| AC-3, AC-4 | 2.2; I0 step 11; I14 step 8 | `compatibility-record.md` |
| AC-5 | 2.4; rows D35, D54 | log; `I<k>-warnings.txt` ("not triggered") |
| AC-6 | I1 | `I1-*`, `gradlew_block.py` output |
| AC-7 | I2 | `I2-*` (or `I2-moved.txt`) |
| AC-8 | I3 | `I3-*` |
| AC-9 | I4 | `I4-*` |
| AC-10 | I5, 4.M | `I5-*`, `mutation-log.md`, `mutations/*` |
| AC-11 | I6 | `I6-*` |
| AC-12 | I7, 4.P | `I7-*`, `I0-probe.txt`, `I0-android-all.txt` |
| AC-13 | I8 to I11 | `I8-*` … `I11-*` |
| AC-14 | I12 | `I12-*` |
| AC-15, IC-3 | I13 | `I13-*` |
| AC-16 | I14 steps 1, 5 | `I14-checks.txt`, `I14-sweep.txt` |
| AC-17 | VMP at I2-I12 (I1 under D25); I4-I7, I14 DEP; I14 step 4 | `I<k>-{writer,vm-proof,vm-added,enforced}.txt`, `I<k>-deps-*`, `I14-vm-proof.txt` |
| AC-18 | every G; L at I0, I3, I12 | `I<k>-gates.txt`, `-warnings.txt`, lint files, `gradle-version` files |
| AC-19 | I0 step 1; every EV-T; I14 step 6 | `I0-start.txt`, `I<k>-junit-totals.txt`, `-cases.txt`, log |
| AC-20 | every EV-T, CMP | `I<k>-coverage.txt`, `-jacoco-*`, `I14-final-checks.txt` (vii) |
| AC-21 | every EV-B, CMP | `I<k>-compare.txt` |
| AC-22 | I14 steps 5, 7 | `I14-sweep.txt`, `I14-final-checks.txt` |
| AC-23 | every CK; I14 step 7 | `I<k>-checks.txt`, `I14-final-checks.txt` |
| AC-24 | CM(1)-CM(14) | `I14-final-checks.txt` (ii), hand-off |
| IC-1 | I14 step 3 | `I14-deps-*` |
| IC-2 | I1 steps 7-8; I14 steps 2, 5 | `I1-wrapper.txt`, `I1/I14-gradle-version.txt`, `I14-sweep.txt` |
| IC-4 | 10.1 (orchestrator) | `IC4-launch.txt` |
| IC-5, IC-6 | 10.2 | log |

## Verdict

**PASS.** The story can be built as written, after U-068, U-069, U-070 and U-074, once the orchestrator has supplied
Q-1 to Q-9 (Q-10 by I13). Every fact the plan relies on was read in this session from committed blobs, the research,
the design, the earlier stories' plans, logs and evidence, or the local caches (L1 to L25); every fact that needs
Gradle, AGP, Kotlin, KSP, Hilt, Robolectric, JaCoCo, R8, lint or a vendor page is a `needs_check` with its step and
decision rows (N1 to N22; D1 to D91), and the plausible-only claims (P1 to P6) carry no step. The plan:
- re-resolves the nineteen rows on the planning date (no row moved) and lists the quotations and checksums the record
  still needs instead of inventing them (2.1, 2.3);
- keeps the design's order, the two pre-specified moves, F1 to F5 under the stricter stop-and-report ruling, and one
  local commit per increment with explicit, derivable file lists checked to exist and to equal the staged set;
- gives every edit as exact text, simulated on the start-state blobs with every expected line and hunk count derived
  (L11), and re-checked at I0 with a stop row;
- regenerates the wrapper twice so the jar is 9.9.2's own, and proves the Windows block byte for byte with a tool;
- applies D5 at every dependency increment with the U-060 proof as arbiter, never `diff`;
- re-runs U-067's six mutations at I5 under a write-ahead protocol that tolerates U-070's and U-074's later tests;
- states coverage margins from measured values, including the `worker` package's zero spare lines;
- plans six sessions with clean boundaries and a resume check.
Five Scope Concerns (SC-P1 to SC-P5) ask for rulings but none blocks the start: each has a stricter default the plan
follows.

## Phase Completion Report
---
story_id: "U-071"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
notes: "Iteration 1. Needs before the build: the orchestrator's quote supplement (Q-1 to Q-9 before I0 step 3, Q-10 before I13; section 2.3). Scope Concerns SC-P1 to SC-P5 ask for rulings, each with a default the plan follows (section 9.2). No Gradle, build, test, device or network step was run in planning."
---
