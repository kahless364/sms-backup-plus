---
artifact_type: plan
story_id: "U-071"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-21T16:01:49Z"   # iteration 3; Amendment A1 written 2026-11-21 (ruling 64)
iteration: 3
amendment: "A2"   # A1 (ruling 64) and A2 (ruling 65)
base_commit: "7c5e6073ebd4ddd2d9e737ded20e7c134b2fb97f"   # PLAN_HEAD of iteration 1 (U-068 S1-I6); iterations 2 and 3 re-read at 9aa6b76c and 0c99149e (build paths unchanged); the start commit is read at I0, after U-068, U-069, U-070, U-074 and the BUG-029 fix story are done
contracts_verified:
  - CNTR-MODERNIZATION-001
  - CNTR-MODERNIZATION-002
  - CNTR-MODERNIZATION-003
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-005
risks_identified: 22
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
The fixes of BUG-025, BUG-026 and BUG-027 (raised 2026-11-21) are placed after story U-072, so no scheduler code changes
before `<start>`; the I0 drift check would show such a change. **The BUG-029 fix story** (a backup started while the
app is off screen fails at `setForeground`, `BackupWorker.kt:123`, on Android 12 and later; `sdlc/artifacts/stories/BUG-029-background-backup-fails-at-foreground-start.md`)
runs after U-069 and U-070 and **before this story**: it will change `BackupWorker.kt` (where mutation anchor M-1
sits), probably `RestoreWorker.kt`, and adds tests. Its commits are part of `<start>`; its tests are inside `C0`; I0
records its commit ids, the `app/src/main` drift it brings, and re-checks every anchor by exact text (row D6).
**The fix story is U-075** (`sdlc/artifacts/build/sprints/sprint-024/U-075-automatic-backup-runs-when-the-app-is-off-screen.md`,
sprint 024 wave 4 after U-070, `e40a8f80`); by its text it changes `BackupWorker.kt` and `RestoreWorker.kt` and adds
`app/src/main/java/com/zegoggles/smssync/service/WorkerForeground.kt` (plan-review O-23).
**State at Amendment A1 (2026-11-21):** U-069 is done (implementation `d7a69974`, closed at `fd70325a`; 771 tests;
its two predicted `app/build.gradle` lines are the only build-file drift); U-075 is being planned; U-070 is being built.

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

**Iteration 2 count:** **71 Gradle runs planned** (I0 6, I1 5, I2 4, I3 4, I4 6, I5 13, I6 5, I7 6, I8 to I11 3 each,
I12 4, I13 2, I14 4): the dependency report now runs once per configuration (two runs per DEP, plan-review O-3) and one
up-to-date run follows G(2) and G(5) (depth check). The figures 63 and "I0 5 … I14 3" above are iteration 1's.

## Iteration 2 (after the plan gates, 2026-11-21)

Iteration 1's gates: plan-review FAIL (B-1 to B-5, O-1 to O-11), contract-verification FAIL (B-1, B-2, O-1 to O-5),
expert-depth-check PASS with two recommendations (`plan-review.md`, `contract-verification.md`, `expert-depth-check.md`
in this workspace). Rulings since then: Record 19 (rulings 47 to 51: SC-P1 to SC-P5, the `worker` floor 0.90), the
supplement `sdlc/artifacts/build/research/2026-11-21-u071-quote-supplement.md` with its ruling on AC-3's form, Record 20
(the product owner's device decisions, ruling 57). Re-read in this session at HEAD `e130182e`: the build paths and
`app/src/main` are unchanged since PLAN_HEAD (`git diff --stat 7c5e6073 HEAD` empty), `code-location.md` differs only at
`@@ -28 +28 @@` (U-068 I7). Every change is made in place; the table says where. No finding is declined.

| Finding | Change, and where |
|---------|-------------------|
| plan-review B-1 (`I1-gradle-version.txt` written by no step) | I1 step 7 now sanitises it to `$E/I1-gradle-version.txt`. Every commit list re-checked against the steps that write it: 4.9 note "Writers" names, for each list entry, the step that writes it |
| plan-review B-2 (`gradlew` may stage nothing) | I1 step 6 records `gradlew-unchanged-exit` (expected `0`: the 9.5.1 and 9.9.1 Unix templates are byte-identical); I1 step 8 expects an empty `gradlew` numstat in that case; CM step 5 now builds the expected staged list from the listed files that are changed or untracked (`git status --porcelain --untracked-files=all`), records every listed file that is unchanged, and compares only the rest; 4.9's I1 row says "`gradlew` (staged only if changed)". The rule applies to every increment |
| plan-review B-3, contract-verification B-1 and B-2 (SC-P1 not carried; R8 comparison too narrow; headers make `configuration.txt` differ) | New R8 evidence R8-1 to R8-6 (tool `r8_check.py`, tested on U-068's files: header normalisation makes U-068 I0 and I5 equal section by section; the seven contract-bearing rules found; 27 manifest classes identity-mapped; 482 seeds and 1,775 usage entries in the contract prefixes): EV-B and CMP rewritten; new 2.10 "R8 checks and the allowed-section table"; D42 and D43 rewritten to ruling SC-P1 (Record 19 ruling 48) plus R8-4 and R8-5; 2.9 cells "= `<p>`, or accepted under SC-P1"; CMP's comparison against `<p>` decides, I0 is recorded; STD gains six files; 2.5, N7, 9.2, R2, H-2 and session B reworded |
| plan-review B-4 (ruling 47: `worker` 0.90) | L17, 2.6, D60, R3, H-7, Verdict: floor 0.90, U-069 expects 10 of 10 counted lines, `⌈0.90·10⌉ = 9`, one spare line (also one at 16 lines); D60 now routes a breach to D38 and records a one-line drop within the floor; H-9 deleted (settled by ruling 47) |
| plan-review B-5 (supplement form) | 2.2, 2.3, D1 and the compatibility record take the supplement's form: exact for table values and checksums, stated fact with address and fetch date for prose, each labelled so; nothing is called "verbatim" that is not; D1 stops only if an item is missing. Q-4 (no Android page names 9.5.2), Q-9 (Byte Buddy 1.17.5 notes carry Java 25; Mockito's are silent: D76 satisfied) and Q-12 (P1 now verified) recorded; the checksums carried into I1 and F2 |
| Coordinator: resolve AGP 9.5.2 against the Android pages under D1a | 2.1 new "D1a resolution of the two supplement facts": D1a rules 1, 4 and 5 quoted; 9.5.2 is the newest non-pre-release artifact on Google Maven and no rule excludes a release whose notes are not yet published, so **9.5.2 stays the target** and F1 stays 9.5.1 (the version the notes describe). D1a decides it; no question is open |
| Coordinator: Kotlin 2.4.21's documented maxima | 2.2: G2 carries Q-5's exact table cell `2.4.20–2.4.21 \| 7.6.3–9.7.0 \| 8.5.2–9.3.1` and the page's caution, as the known gap F2 exists for |
| Coordinator: the tool timeout | New 4.L "Long runs": start in the foreground; if the harness backgrounds the run, wait for its completion notice, start nothing else, never poll, read the result from the log's `exit=` line; a log without `exit=` is a cut-off run (row D93: stop, never repeated at once); 4.R: a cut-off writer pass or gate is re-run once in a new session after the checks, a second cut-off stops |
| plan-review O-1 (resume compares HEAD) | 4.R compares the newest `U-071 S2-` commit with the log and checks that no foreign commit touched the story's paths; other commits recorded |
| plan-review O-2 (no work folder in a new session) | 4.R starts with `mkdir -p`; what is lost is stated (raw logs only; `gradlew-start` and the block are recreated from `git show $START:gradlew`) |
| plan-review O-3 (plain `:app:dependencies`) | DEP runs two reports, `--configuration releaseRuntimeClasspath` and `--configuration debugUnitTestRuntimeClasspath`, as U-060 did; row D97 for a failing report |
| plan-review O-4 (I7 steps 5, 6 order) | Swapped: DEP(7) is step 5, A21 step 6 |
| plan-review O-5 (later cases keyed on names) | I0 step 13 classifies each later case of the NEW3 classes by the loop it drives (from U-070's and U-074's logs and the fixture it calls); I5 copies that table into `mutation-log.md` before M-1a; D64 applies to it |
| plan-review O-6 (`git restore` writes CRLF `gradlew`) | D15, D17, 4.R use `git -c core.autocrlf=false restore --source=HEAD --worktree -- gradlew` and a normal restore for `gradle/wrapper` and `gradlew.bat` |
| plan-review O-7 (D7 scan without baseline) | I0 step 6 runs the D7 scan on G(0)'s log and XML into `$E/I0-d7.txt` (I0SET) |
| plan-review O-8 (SC-2 merged-manifest check) | I0 step 6 adds `grep -c 'android:minSdkVersion="24"' $E/I0-merged-debug.xml` → `1` |
| plan-review O-9 (E14.1 KGP minimum) | E14.1's second and third lines reworded: AGP 9.5 needs Gradle 9.6.0 and JDK 17; "AGP 9 built-in Kotlin needs KGP >= 2.2.10" (still three comment lines: numstat and the `//` check unchanged); `oldtext_check.py` unchanged (old text) |
| plan-review O-10 (checksum host) | I1 step 2 cites the supplement's `downloads.gradle.org` URLs and the redirect note |
| plan-review O-11 (Robolectric content providers) | D37 names "content providers are now installed for lazily created applications" (Q-8) as a candidate cause for an application-context test change at I7 |
| contract-verification O-1 (packaged backup descriptor) | EV-B dumps `xml/backup_descriptor` from both APKs (path resolved with `aapt2 dump resources`; release path is shortened, e.g. `res/mt.xml`); CMP expects 2 `exclude`, `credentials.xml`, `pinned_certs.xml` per APK (derived on the current APKs); `aapt2` verified present under the SDK's `build-tools/36.0.0` |
| contract-verification O-2 (forced manifest or keep-rule edit) | New row D95: stop; the SA reads CNTR-005 `:158-165` and CNTR-004 `:266` and decides whether a change record and a contract version step are needed before any edit |
| contract-verification O-3 (frontmatter, sources) | `contracts_verified` now equals 9.1 (001, 003, 004, 005, 006); 9.1 names CR-014 Amendment 1 as the source of the Robolectric/provider statement |
| contract-verification O-4 (probe diff row) | New row D96: a changed `tls_provider`, provider list or default-enabled suite set with passing tests is classified in the log and reported in the session hand-off |
| contract-verification O-5 (stale contract citations) | Hand-over H-8 adds CNTR-003 `:260` and CNTR-005 `:65` for the next contract amendment; not edited |
| depth check, criterion 3 (four trade-offs) | 2.5 gains four rows: increments not one raise; versions inline, no catalog; what additions-only verification does and does not protect (signatures off, the two `<trust regex>` lines, same-host checksum files); caches off for comparison runs |
| depth check, caches | G, ENF, TR, L, DEP and the up-to-date run pass `--no-build-cache --no-configuration-cache`; WP stays the design's exact command, and I0 step 1 proves without printing a path that no project or user-wide `gradle.properties` turns either cache on (row D94 adds the flags to WP if one does) |
| depth check, criterion 8 (edge cases) | Kotlin daemon: the process checks match `gradle\|KotlinCompileDaemon` (Kotlin daemons recorded, not stopped), new row D92 for a failed delete (stop; never kill a process); incremental check after G(2) and G(5) (`I2-uptodate.txt`, `I5-uptodate.txt`; a re-run task is debt); AGP 9.1 to 9.5 behaviour changes: Q-13 to Q-17 asked of the orchestrator before I3, and 2.8 states which defaults the build sets explicitly; JaCoCo filters: risk R19 and the I12 reading in 2.9 and D40; the ordering-constraint table in 2.2; the timeout (4.L, D93) |
| Coordinator: Record 20 (devices) | 10.2 rewritten: IC-5 is the orchestrator's procedure on `Pixel_9_Pro` (launch, manual backup, manual restore with the role hand-over and back, settings restored) and, if created by then, launch only on the new API 24 device unless the owner has configured an account there; IC-6 stays "not reached" |
| Coordinator: U-069 Amendment A4 | L17 and the I0 drift check re-derived: U-069 still replaces `app/build.gradle:579` and `:586` in place (E-G1, E-G2; floor `0.90`, note "floor is the highest 0.05 step that leaves one spare line at this size"); no anchor of this plan sits on those lines; `ic1_…` still reads androidx.hilt's generated names (D66 unchanged) |
| Coordinator: BUG-025 to BUG-027 | Planning state: their fixes come after U-072; no scheduler code changes before `<start>`; the I0 drift check would show one |

## Iteration 3 (after the plan gates, 2026-11-21)

Iteration 2's gates: plan-review FAIL (one blocker, B-6; O-12 to O-20), contract-verification PASS (O-6 to O-12),
expert-depth-check PASS with two recommendations (should and could items). New binding inputs: ruling 58 (the
orchestrator's answer to SC-P6), ruling 48 restated for plan-review O-14, the supplement's items Q-13 to Q-17 (commit
`0c99149e`), and the BUG-029 fix story placed before this story. Re-read at HEAD `0c99149e`: build paths and
`app/src/main` unchanged since PLAN_HEAD (`git diff --stat 7c5e6073 HEAD` empty). A Gradle daemon of another agent was
live during this iteration, so no build output under `app/build` was read in iteration 3; the checker was re-tested on
committed evidence and on synthetic inputs in the session scratch folder only. Every change is made in place.

| Finding | Change, and where |
|---------|-------------------|
| plan-review B-6, contract-verification O-11, depth check (should): ruling 58 | D42's last sentence replaced by ruling 58's conditions (a) to (d); 2.9's R8-3 cells at I4, I5, I6 "= `<p>`, or accepted under ruling 58"; 2.10 R8-3 rewritten (hard stop except as ruling 58 accepts; the `usage-core` rule exempts lines accepted under ruling 58; "worker constructor line" defined, including the synthetic constructor with `DefaultConstructorMarker`); new `r8_check.py seeds-diff` mode run by CMP at every increment ≥ 1; SC-P6 marked ruled; R20, H-2 and the Verdict reworded. **Tests run (scratch only):** `normalize`/`compare`/`rules` on U-068's committed `I0-` and `I5-configuration.txt` (28 sections, `ALL EQUAL`, 10 `OK` lines, exit 0, as before); `seeds-diff` on synthetic variants of a 482-line `seeds-core` copy made earlier in this session: three Hilt lines at I4 → `MATCH Hilt_*`, `MATCH *_GeneratedInjector`, and `NOT-GENERATED *_HiltComponents*` for an app-package class without a generated source (exit 1); the same lines at I5 → 3 not accepted; at I5 a `$default(` member → `MATCH`, a Tink line and a worker synthetic constructor (`…DefaultConstructorMarker)`) → `FORBIDDEN` (exit 1); an equal pair → `changed=0`, exit 0 |
| plan-review O-16 (seeds at I3) | D42 and 2.10: a `seeds-core` difference at I3 is outside ruling 58: stop and report with the changed lines; the orchestrator decides; named in R20 and H-2 |
| Ruling 48 for plan-review O-14 | R8-5's table and D43: at I4 also `jar-rules:lib/META-INF/com.android.tools/r8/r8.pro` (Dagger's own jar rule; shown by `unzip -l` of the resolved `dagger-2.60.1.jar`), changed or added; at I5 a `jar-rules:` section changed **or added**, shown inside the resolved `kotlin-stdlib-2.4.21.jar`; at I6 `lib:hilt-work/proguard.txt` and any section shown inside an `androidx.hilt` 1.4.0 artifact |
| Coordinator: Q-13 to Q-17 supplied | 2.3 rows; new 2.11 "What AGP 9.1 to 9.5 change at I3, and what the checks will see": repackaging (Q-13) and the `-keepattributes` wildcard change (Q-14) each with the expected effect on every check and the developer's action; no `-dontrepackage` and no keep rule is added (none is shown to be needed); D44 cites 2.11 |
| Coordinator: BUG-029 fix story before this story | Planning state; D2 (the fix story `done`); I0 step 1's expected commits and its new `app/src/main` drift record (contract-verification O-12); I0 step 13's later-case classification includes its tests; D6 now says a moved anchor stops for an SA re-anchoring amendment; D41 note on a changed manifest |
| plan-review O-12 (stale 2.5 row) | Replaced by the two-report row |
| plan-review O-13 (D18 restore form) | D18 restores as D15 (`gradlew` with `core.autocrlf=false`) |
| plan-review O-15, contract-verification O-7 (closure count) | `closure` prints every manifest class missing from `mapping.txt` (`NOT-IN-MAPPING`); CMP compares `manifest-classes-in-mapping` with `<p>`'s (row D42) |
| plan-review O-17 (no `LAST` after an I0-only session) | 4.R: `LAST` is `START` when no `U-071 S2-` commit exists; the first command then prints nothing, as expected |
| plan-review O-18 (IC-5 in the foreground) | IC-5 steps 2 to 4: the app stays in the foreground until the worker's result line; `third_party_integration` stays off and no setting that schedules a backup is toggled (BUG-029) |
| contract-verification O-6 (worker map in the release dex) | EV-B records, per persisted worker name, the count of its dotted string in the release dex files, compared with `<p>` in CMP; **not verified** in planning (a Gradle run of another agent was live, so the release APK was not read): record-only, a difference is classified and reported, not a stop |
| contract-verification O-8 (backup-descriptor stop row) | New D100 |
| contract-verification O-9 (`contracts_verified`) | 001, 002, 003, 004, 005; 006 moved to "considered, no obligation" in 9.1 |
| contract-verification O-10 (cite by heading) | D95 and 9.1 cite "CNTR-004, Breaking change policy" and "CNTR-005, Validation Rules", lines as hints |
| depth check (should): `mapping.txt` header lines; no resource shrinker | EV-B records `# compiler`, `# compiler_version`, `# min_api` in `I<k>-r8.txt`; 2.9 row (`min_api: 24` everywhere; `compiler_version` changes at I3 only, recorded); 2.10 states that `app/build.gradle` has no `shrinkResources` (`git grep` at HEAD: none), so no resource-shrinker output exists |
| depth check (could) | Taken: `dexdump -f` header counts of the release `classes.dex` (`dexdump.exe` verified present under the SDK's `build-tools/36.0.0`) and the SHA-256 of `assets/dexopt/baseline.prof` and `.profm` (both present in U-068's release entry list), all record-only in `I<k>-r8.txt`; D75's detector named (VMP step 4's groups at I8 to I12, and I14's test-classpath report); 4.R finds a cut-off through the D93 entry in the implementation log. **Declined:** dropping ENF — design D5 step 6 binds "Enforced re-run without writer mode: `./gradlew :app:assembleDebug`, then the full gates" (DES-014 `:910`) as its own run; it is also the first run with verification enforced after the writer pass, so a missing entry fails a 2-minute run rather than a 10-minute gate |

## Amendment A1 to iteration 3 (ruling 64, 2026-11-21)

One targeted amendment after plan-review iteration 3 (B-7); contract-verification passed. No other part is rewritten.
Nothing under `app/build` or the working tree under `app/` was read (another build is live; U-070's developer has
uncommitted edits). **Tests run (scratch only):** `r8_check.py services` on two fake release APKs built with Python's
`zipfile` (I2 with `META-INF/services/q4.b0`, `v4.t` and a `CharsetProvider` entry; I3 with the same services renamed
`b0`, `t` and the implementation renamed `x2.a` → `c`) and their mappings: the `PAIR` lines in original names are equal
(`exit=0`), the `ENTRY` lines show the renames; a third APK whose implementation changed gives `exit=1`.
`seeds-diff`: the five iteration-3 cases unchanged (I4 Hilt lines `MATCH`/`NOT-GENERATED`, exit 1; the same at I5,
3 refused; the Tink line and the worker synthetic constructor `FORBIDDEN`, the `$default(` member `MATCH`; no generated
root `NOT-GENERATED`; equal inputs exit 0), plus `androidx.work.impl.WorkDatabase_Factory` at I4 → `NO MATCH` (exit 1),
`androidx.work.impl.Foo: … run$default(int)` at I5 → `NO MATCH` (exit 1), a removed `RestoreWorker_HiltModule` with no
generated root → `NOT-GENERATED`, and with `--prev-ksp` listing `release/java/…/RestoreWorker_HiltModule.java` →
`MATCH … (generated at the previous increment)`, exit 0. `section` writes the three-line Dagger `r8.pro` body from
U-068's normalised `I5-configuration.txt`. The embedded tool is byte-equal to the tested copy.

| Finding | Change, and where |
|---------|-------------------|
| plan-review B-7, contract-verification O-13 | New `r8_check.py services` mode; EV-B writes `$E/I<k>-services.txt` at every increment from that increment's own `mapping.txt` (`ENTRY <apk name> = <original>`; `PAIR <original interface> -> <implementations in original names>`); STD now 28 files, I0SET 40, Writers updated; CMP compares the `PAIR` lines with I0 and `<p>` (`services-pairs … exit=0` required at every increment, I3 included); 2.11's Q-13 row pairs by original name and names the stops; D101 |
| plan-review O-21 | Taken: `seeds-diff --prev-ksp=<p>'s ksp-files.txt` accepts a removed generated class whose source `<p>` had; a Hilt component-source class (not in that list) still stops, stated in D42 as accepted strictness |
| plan-review O-22 | Taken: "equal after dropping the leading `lib/`" (2.10) |
| plan-review O-23 | Taken: U-075 and its file named in the planning state and I0 step 1 (`B29_ID=U-075`, `B29_STORY=…`); `WorkerForeground.kt` named |
| contract-verification O-14 | Taken: the I5 F-1 grep is also run with `WorkerForeground.kt` (the story's own form is kept beside it); the I0 status line expects U-075 `done` and records the bug file's status |
| contract-verification O-15 | Taken: `r8_check.py section` plus `unzip -p` of the jar file, compared with `git diff --no-index --quiet` (2.10) |
| contract-verification O-16 | Taken: `seeds-diff` refuses lines under the four library prefixes at I4 to I6 and non-`smssync` lines at I5; re-tested (above) |
| plan-review O-24 (info) | Taken: I0's expected drift for U-069 is `worker/BackupTriggerWorker.kt` only |
| State changes | U-069 done (`d7a69974`, closed `fd70325a`, 771 tests; only the two predicted `app/build.gradle` lines); U-075 in sprint 024 wave 4, being planned; U-070 being built (planning state) |

## Amendment A2 to iteration 3 (ruling 65, 2026-11-21)

**B-8 fixed.** `r8_check.py section` now writes its output with `newline='\n'` (LF on every OS); the 2.10 command keeps
`tr -d '\r'` on the jar side, so both sides are LF. One form, one place: no other A1 command compares a Python-written
file with shell output byte for byte. Nothing else in the plan is changed. **Machine fact behind B-8, measured:** on
this machine Python writes CRLF both in text mode and to redirected standard output (`print('a') > f` gives
`a \r \n`), while the shell tools (`grep`, `sed`, `sort`, `unzip`, `git`) write LF; MSYS command substitution `$(…)`
drops CR. So a Python-written file compares equal only with another file written the same way, which is the
condition checked in table 2 below.

**Self-test 1: every command and tool mode A1 and A2 added or changed** (Git Bash on this machine; inputs: committed
evidence, the cached Dagger jar, synthetic files in the session scratch folder; no `app/build`, no working-tree file
under `app/`; the plan's commands run as written, with `$E`, `$WORK`, `$TL` pointing at scratch folders and `<p>`, `<k>`
replaced by increment numbers):

| # | Command or mode (as in the plan) | Input | Expected | Observed |
|---|-----------------------------------|-------|----------|----------|
| 1 | B-8 reproduction with the **A1** tool: `r8_check.py section $E/I5-configuration-norm.txt 'jar-rules:lib/META-INF/com.android.tools/r8/r8.pro' $WORK/sec.txt; unzip -p <jar> META-INF/com.android.tools/r8/r8.pro \| tr -d '\r' > $WORK/jar.txt; git -c core.autocrlf=false diff --no-index --quiet -- $WORK/sec.txt $WORK/jar.txt; echo "section-equals-jar-exit=$?"` | U-068's committed `I5-configuration.txt` (normalised by `r8_check.py normalize`); the cached `dagger-2.59.2.jar` | `exit=1` (the defect) | `section-equals-jar-exit=1` |
| 2 | The same command with the **A2** tool (the plan's command, unchanged) | the same | `exit=0`; 0 CR bytes in both files | `section-equals-jar-exit=0`; CR bytes `sec.txt` 0, `jar.txt` 0 |
| 3 | Negative: the same comparison against a jar copy with one word changed (`static` → `STATIC`) | the same, altered | `exit=1` | `exit=1` |
| 4 | Blank-line fallback of 2.10: jar copy with two trailing blank lines; raw comparison, then `grep -v '^[[:space:]]*$'` copies of both | the same | raw `1`, fallback `0` | raw `exit=1`, fallback `exit=0` |
| 5 | EV-B line `r8_check.py services <release apk> $MAP/mapping.txt $E/I<k>-services.txt`, then CMP's `for b in I0 <p> I<k>; do grep '^PAIR ' $E/$b-services.txt > $WORK/svc-$b.txt; done` and `git -c core.autocrlf=false diff --no-index --quiet -- $WORK/svc-$b.txt $WORK/svc-I<k>.txt; echo "services-pairs vs $b exit=$?"` (k = 3, p = 2) | synthetic APKs (`zipfile`): I0 = I2 with `META-INF/services/q4.b0`, `v4.t`, `java.nio.charset.spi.CharsetProvider`; I3 with the services renamed `b0`, `t` and one implementation `x2.a` → `c`; their mappings | `services=3`; both comparisons `exit=0`; `ENTRY` lines show the new APK names | `services=3 unmapped=2` each; `services-pairs vs I0 exit=0`, `vs I2 exit=0`; `ENTRY b0 = kotlinx.coroutines.CoroutineExceptionHandler`, `ENTRY t = …MainDispatcherFactory` |
| 6 | The same CMP lines with an APK whose implementation changed | synthetic `i3bad.apk` | both `exit=1` (a stop, D44/D101) | `services-pairs vs I0 exit=1`, `vs I2 exit=1` |
| 7 | CMP line `r8_check.py seeds-diff $E/<p>-seeds-core.txt $E/I<k>-seeds-core.txt I<k> --prev-ksp=$E/<p>-ksp-files.txt app/build/generated/ksp/release/java app/build/generated/hilt/component_sources/release; echo "seeds-diff-exit=$?"` (k = 4, run from a scratch folder holding fake generated roots at those relative paths) | `seeds-core` files written by `r8_check.py core` (CRLF, 482 and 484 entries) from synthetic seeds; `--prev-ksp` file written by `printf` (LF) | two `MATCH` lines (`Hilt_*`, `*_HiltComponents*`), `changed=2 not-accepted=0`, exit 0 | as expected; `seeds-diff-exit=0` |
| 8 | The same line at k = 6 for a removed `RestoreWorker_HiltModule`, its source only in the previous KSP list | the same, `I5-ksp-files.txt` listing `release/java/…/RestoreWorker_HiltModule.java` | `MATCH *_HiltModule (generated at the previous increment)`, exit 0 | as expected; `seeds-diff-exit=0` |
| 9 | The same line at I3 | the I4 inputs | every line refused, exit 1 | `changed=2 not-accepted=2`, exit 1 |
| 10 | `seeds-diff` refusals of contract-verification O-16 and the A1 cases (iteration-3 set) | synthetic lines | `androidx.work.impl.WorkDatabase_Factory` at I4 and `androidx.work.impl.Foo: … run$default(int)` at I5 → `NO MATCH`, exit 1; Tink line and worker synthetic constructor → `FORBIDDEN`; `$default(` in `smssync` at I5 → `MATCH` | as expected (run in A1 and repeated here on the same files) |
| 11 | I5 step 3, both F-1 greps (`(BackupWorker\|RestoreWorker)\.kt` and `(BackupWorker\|RestoreWorker\|WorkerForeground)\.kt` … `\| grep -c 'coroutineContext'`) | U-067's committed `I7-kotlinc.txt` (6 `w:` lines) | `0` and `0` | `0` and `0` |
| 12 | I0 step 1 U-075 lines (`git log … --grep="^$B29_ID"` with `B29_ID=U-075`; `grep -n '^status' "$B29_STORY" sdlc/artifacts/stories/BUG-029-*.md`) | HEAD (`fd70325a`), committed story files | the U-075 commits listed; both status lines printed (quoting of the path variable holds) | `d70afb52 U-075 written …` listed; `U-075-…md:3:status: in-progress`, `BUG-029-…md:3:status: ready` (at start U-075 must read `done`, D2) |
| 13 | CMP line `for b in <p> I<k>; do echo "$b $(grep -E '^manifest-classes-in-mapping=' $E/$b-r8.txt) $(grep -E '^# min_api:' $E/$b-r8.txt)"; done` | `I<k>-r8.txt` built as in EV-B (a `{ …; python … closure …; …; } >` group) | one clean line per increment | `I3 manifest-classes-in-mapping=24 not-identity=0 # min_api: 24` (and I4); 0 CR bytes in the output (command substitution drops them) |

**Self-test 2: every other place where a Python-written file is compared with, or hashed against, other output.**
"Same producer" means both sides come from the same tool on this machine, so both carry the same line endings.

| Place | The two sides | Both sides LF? | Same line endings? | Tested |
|-------|---------------|----------------|--------------------|--------|
| CMP loop, `seeds-core.txt`, `usage-core.txt` (`r8_check.py core`) | I<k> vs `<p>`/I0, both from `core` | no (CRLF both) | yes | yes: two runs equal (`exit=0`) |
| CMP loop, `jacoco-inventory.json` (U-068 `jacoco_inventory.py`), `jacoco-classes.txt` (`jacoco_class_lines.py` to stdout) | same producers | no (CRLF both) | yes | yes: two runs on a synthetic JaCoCo XML equal |
| CMP loop, `cases.txt` (`junit_cases.py` → `sanitize.py`) | same producers | yes (`sanitize.py` writes LF) | yes | yes: two runs on synthetic JUnit XML equal |
| CMP totals (`junit_totals.py` stdout → `sed`) | same producers | no (CRLF both) | yes | yes: `totals sed exit=0` |
| CMP, `configuration-norm.txt` via `r8_check.py compare` | Python reads both (universal newlines) | n/a (not a byte comparison) | n/a | yes: U-068 I0 vs I5 `ALL EQUAL` (iteration 2) and two runs equal |
| CMP, `services` PAIR lines (`grep` of Python-written files) | same producer | no (CRLF both; `grep` keeps CR) | yes | yes: #5, #6 above |
| CMP, `seeds-diff` reading `seeds-core` (CRLF) and `--prev-ksp` `ksp-files.txt` (LF, `find \| sed`) | Python reads both with `splitlines()`/`strip()` | n/a | n/a (CR removed on read) | yes: #7, #8 |
| CMP loop, `kept-names*.txt`, `apk-entries-*.txt`, `javap.txt`, `ksp-files.txt`, merged manifests, normalised packaged manifests | shell on both sides | yes | yes | not re-run (no Python involved) |
| I4/I5/I6 DEP comparison (`deps_resolved.py` coordinates, I<k> vs `<p>`/I0) | same producer | no (CRLF both) | yes | yes: two runs on U-060's report equal |
| I5 step 2, `w:` lines (`sanitize.py` output → `grep \| sed \| sort`) | same producers | yes | yes | yes (sanitize writes LF) |
| I12 inventory (`python -c` over two JSON files) and class names (`cut` of `jacoco-classes.txt`) | same producers | no (CRLF both) | yes | yes: `class-counts-equal True`, `class-names cut exit=0` |
| 2.10 section vs jar | Python `section` vs `unzip \| tr -d '\r'` | **yes (after A2)** | yes | yes: #1 to #4 |
| I0 step 2 block hash `sed -n 117,127p \| sha256sum` vs `45364f3d…` | shell vs a value derived by the same shell form | yes | yes | yes (iteration 1, L4) |
| I1 `gradlew_block.py` (bytes of `git show` output, the generated copy, the final file) | git and generator output, read as bytes | yes (all LF; the tool fails on a CR) | yes | yes (iteration 1, L11) |
| CM step 4, `comm` of `printf` list vs `git status --porcelain` | shell vs git | yes | yes | yes: on three real paths read-only (one unchanged tracked file listed by `comm`, one untracked and one modified file expected) |
| VMP and I14 proofs (`vm_proof.py` parses both XML files) | Python reads both | n/a | n/a | yes (iteration 1) |

No other place mixes a Python-written file with shell output in a byte comparison, so no further change is needed. One
carried note (record, not a change): evidence written in an earlier session is compared from the working tree, where
`git add` leaves its line endings as written; a fresh checkout of those files would turn them LF in the index and CRLF
in the working tree (`core.autocrlf=true`) and could make a same-producer comparison differ only by line endings. That
can only cause a false stop (ruling 65 item 3); the stop-and-amend route covers it.

## 0. Binding Inputs

| Source | What binds this plan |
|--------|----------------------|
| Story, "Orchestrator rulings before planning" (Record 17) | Runs after U-069 and U-070 (SC-8, SC-9); one local commit per increment, explicit paths, the plan's subjects, no push, no amend, fixes are further commits; the orchestrator supplies a refreshed research file and nobody fetches pages; Scope Concerns 3, 4, 5, 7 to 10 acknowledged as written (a forced test edit stops the story for an SA amendment); IC-5, IC-6 recorded "not run: needs the product owner's decision" and judged "not applicable until decided" (for IC-5 superseded by Record 20, ruling 57: run by the orchestrator, 10.2); **a fallback that fires stops the developer at that increment, who reports** |
| Delegation of 2026-11-21 (orchestrator) | The planning date is 2026-11-21 and `sdlc/artifacts/build/research/2026-11-21-latest-stable-versions.md` is the supplied research (ruling 30); missing verbatim quotes and checksums go to the orchestrator as `needs_check` (2.3); the U-069 test `ic1_…` is never edited silently; `.claude/CLAUDE.md` only through the Engine Librarian; Record 18: no external `diff` or `cmp`, modest shell use, stop on a vanished tool or an unexplained kill |
| Story, Bound decisions | One component per increment in the design's order with the two pre-specified moves; D1 targets re-resolved by D1a; no triple is vendor-supported (A17 contradicted); warnings are debt, triggers are KGP/KSP/Hilt-plugin/AGP errors; F1 to F5 only; Build Tools 36.0.0; Truth in, JUnit unchanged; verification stays on, D5 per dependency increment; no floor lowered, no `@Ignore`, no loosened test; `-Werror` never removed; Robolectric default `sdk=29` and its comment unchanged; lint recorded, not gated |
| DES-MODERNIZATION-014 | S2 table (`:554-575`); D1 rows (`:364-383`) and D1a (`:435-460`); D2 N-6 (`:590-596`); D3 (`:701-793`: reading, G1 to G6, evidence that decides, triggers, javac rule, F1 to F5, who decides); D5 (`:882-936`); D6 (`:938-979`); D7 (`:981-1015`); D8 (`:1017-1066`); D9 (`:1068-1084`); D10 F-1 rule (`:1104-1138`); D16 (`:1543-1604`); D17 (`:1606-1648`); D18 (`:1650-1673`); D19 (`:1675-1692`); A13 to A22 (`:247-256`); INT-001 to INT-006 (`:1734-1766`); Risks 3, 5 to 10, 12, 13 (`:2035-2045`); PO-2 to PO-4 (`:2067-2093`) |
| REQ-MODERNIZATION-015 | AC-1 to AC-4, AC-11 to AC-18; AC-13 as amended by CR-016 Amendment 1 (start = HEAD before the first change, end = the last commit or the working tree with `--untracked`, foreign tests listed and not counted, no fixed baseline) |
| Story AC-1 to AC-24, IC-1 to IC-6 | As written; where this plan reads a criterion in one of two ways it says so in 9.2 (SC-P1 to SC-P5, now ruled) |
| Record 19, rulings 47 to 51 (story Implementation Notes, "Rulings on the plan's scope concerns") | SC-P1: a `configuration.txt` difference is accepted and recorded when the kept-name set equals the previous increment's and every changed rule comes from the raised component's own shipped rules, each source named; any other difference stops. SC-P2: the probe as planned. SC-P3 to SC-P5: as the plan states them. `worker` floor 0.90 (one spare line) |
| The supplement and its ruling (`$SUPP`, "How to read it") | Q-1 to Q-12 supplied; AC-3's "verbatim vendor quotes with URLs" is met with exact table values and checksums and, for prose, the stated fact with the address and fetch date |
| Ruling 58 (orchestrator, 2026-11-21; answers SC-P6) | A `seeds-core` difference at I4, I5 or I6 is accepted and recorded when (a) every changed line names a class generated by that increment's raised component (I4, I6: the Dagger/Hilt patterns of 2.10; I5: the Kotlin compiler-generated members only; matched on the simple class name; an app-package class also shown to have its source under the build's generated-sources folder), (b) each changed line is in the log with its pattern, (c) R8-1, R8-2 and R8-4 hold, (d) no `com.google.crypto.tink.` line and no worker-constructor line changes (Kotlin's synthetic constructor included). Anything else stops. At I3 a `seeds-core` difference is not covered: stop and report |
| Ruling 48 restated (Record 19; for plan-review O-14) | A rule section that is new or changed and comes from the jar of the component raised in that increment (Dagger's own jar at I4; a Kotlin section added at I5) is inside the ruling |
| Ruling 64 (orchestrator, 2026-11-21) | The gate's three iterations are used up; one amendment round (A1) for the single remaining blocker, then a delta re-check by plan-review |
| Ruling 65 (orchestrator, 2026-11-21) | Amendment A2 for B-8; before returning, every command and tool mode that A1 and A2 added or changed is self-tested end to end in Git Bash on inputs that need neither `app/build` nor the working tree under `app/`, with line endings, path separators and quoting checked; every other place where a file an embedded Python tool writes is compared with or hashed against shell output is listed as "both sides LF / tested". After A2, plan-review re-checks B-8 and the self-test table only; a later finding that can only cause a false stop, never a false pass, is an observation handled by the plan's stop-and-amend route during the build, not a blocker |
| Record 20 (product owner, 2026-11-21) and ruling 57 | An API 24 image and device may be created by the orchestrator; on `Pixel_9_Pro` restore with the role hand-over, setting toggles (restored) and backups to the other folders are allowed; a rollback that needs an app-data clear stops first; the device criteria of U-071 are run by the orchestrator; the hosted CI run (PO-B) stays open |

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
| L17 | U-069 (plan iteration 3 with Amendment A4; Record 19 ruling 47): E-G1 replaces the note at `app/build.gradle:579` and E-G2 the floor line `:586` with `                minimum = <F> // MEASURED <P>% (<c>/<t> lines, <YYYY-MM-DD>, U-069); floor is the highest 0.05 step that leaves one spare line at this size`, expected `<F>` = `0.90`, `10/10` (the fixed worker's class has 10 line entries; U-069 iteration 2 wrote 0.95, corrected by ruling 47); both edits are in place, so no line this plan names moves. `worker` holds only `BackupTriggerWorker` plus generated classes JaCoCo filters (`@DaggerGenerated`). Test `ic1_hiltWiring_assistedFactoryIsBoundUnderThePersistedClassName` reads `<Name>_AssistedFactory`, `<Name>_HiltModule` and `bind`; U-069's AC-9 tests use `ShadowLog`; Amendment A4's test reaches WorkManager's work spec by reflection. **Derived:** at 10 lines, `⌈0.90·10⌉ = 9`, **one spare line** (also one at 16 lines, `⌈0.90·16⌉ = 15`): one more uncovered or newly counted uncovered line is absorbed, a second fails the gate | verified (plan read; arithmetic) | U-069 plan `:124`, `:1038-1056`, `:1572`, `:785-811`, `:1511-1516`; story Implementation Notes (ruling 47) |
| L18 | U-074 adds tests to `RestoreWorkerIntegrationTest` and `RestoreWorkerCheckpointTest` (R1 to R8, C1, C2) and leaves U-067's check lines alone ("The new call sits inside the catches, not on the check lines"); U-070 edits `service`, `preferences`, `mail`; neither edits a build file | verified (stories and plan read) | U-074 story AC-2, AC-3, AC-11, Affected Code; U-070 plan `:86-87`, `:3986` |
| L19 | Risk 9 sites (Kotlin "annotation is currently applied to the value parameter only"): `MainViewModel.kt:55`, `WorkManagerScheduler.kt:80` (`:81` before U-068 I5), `SharedPreferencesCheckpointStore.kt:49`, `WorkerEngineFactory.kt:61`, each `    @ApplicationContext private val context: Context` (once per file). U-067 I7 recorded 6 `w:` lines, 4 of them these | verified | `git grep` on HEAD; `sprint-023/U-067/evidence/I7-kotlinc.txt` |
| L20 | TLS: `DefaultTrustedSocketFactoryJdkProviderTest` has `@ConscryptMode(OFF)` `:140`, `@LooperMode(LEGACY)` `:141`, the `@Before checkIsolationGuards()` whose failure messages start `A1-G3`, `A1-G1`, `A1-G3 (via factory static init)`; the exact-list tests are in `DefaultTrustedSocketFactoryHardeningTest` (`containsExactlyElementsIn(ALLOWED_CIPHERS_…)`) and `…JdkProviderTest` (`…areExactlyTheAllowedList…`). No test uses `TextLayoutMode`, `shadows-httpclient` or `org.apache.http` (Robolectric 4.18 removes them); `ShadowLog` is used by nothing at HEAD (U-069 adds it) | verified | `git grep` on HEAD |
| L21 | Tools on PATH: `python` 3.13.17 with PyYAML 6.0.2 under `-I`; `unzip`, `sha256sum`, `sha512sum`, `awk`, `sed`, `comm`; `diff` is gone (quarantined); `cmp` exists and is **not used**; `node` exists and is not used | verified | `python --version`, `python -I -c "import yaml"`, `command -v` |
| L22 | `org.gradle.java.home` occurs only in prose: `.claude/CLAUDE.md:31` and `DEVELOPMENT.md:25`, `:28`, `:30`, `:49` (the last says the project "pins JDK 17 via `org.gradle.java.home`", stale since U-060) | verified | `git grep` on HEAD |
| L23 | `gradle.properties`: `org.gradle.daemon=false` (`:21`): every Gradle call starts a single-use daemon, so no Gradle JVM outlives a run (the Kotlin compile daemon is not covered by this and can outlive it, row D92); `org.gradle.jvmargs` (`:20`) carries the Windows NIO flags; neither `org.gradle.caching` nor `org.gradle.configuration-cache` is set, so both caches are off at project level. I0 step 1's cache check run read-only in planning printed `caches-on=0` twice and `gradle-opts-caches=0` (the user-wide file is checked without printing its path) | verified | file read; the check run |
| L26 | `configuration.txt` section headers embed the Gradle version, a transform hash and the artifact version (U-068 `I5-configuration.txt:252`: `…\caches\9.5.1\transforms\<hash>\transformed\hilt-work-1.2.0\proguard.txt`; `:95` `…proguard-android-optimize.txt-9.0.1`). `r8_check.py normalize` reduces each to `<kind>:<artifact>`: U-068's I0 and I5 files give 28 sections, 28 keys, and `compare` prints `ALL EQUAL` (Gradle 9.5.1, AGP 9.0.1 in both; minSdk 21 → 24 changed no rule). `rules` finds all seven contract-bearing rules, the AGP `-keepattributes` with `Signature` and `RuntimeVisibleAnnotations`, and the `app:proguard-rules.pro` section byte-equal to the committed file with `-dontoptimize`. The current release outputs (U-068 state) list `configuration.txt`, `mapping.prt`, `mapping.txt`, `seeds.txt` (1,634 lines), `usage.txt` (25,582); `closure` finds 27 manifest classes in `mapping.txt`, all identity-mapped; `core` keeps 482 seeds entries (274 Tink, the worker constructors) and 1,775 usage entries (6,479 lines) | verified (tool runs on committed evidence and on build outputs, read-only) | `r8_check.py` (4.0) |
| L27 | The packaged backup descriptor: `aapt2` exists under the SDK's `build-tools/36.0.0`; `aapt2 dump resources` maps `xml/backup_descriptor` to `res/xml/backup_descriptor.xml` (debug) and to a shortened path (release, `res/mt.xml` today); `aapt2 dump xmltree --file <path>` shows two `E: exclude` elements, `domain="sharedpref"` twice, `path="credentials.xml"` and `path="pinned_certs.xml"` once each, in both APKs, equal to `app/src/main/res/xml/backup_descriptor.xml` | verified (read-only on the current APKs; the SDK path is not written) | EV-B |
| L24 | The research of 2026-11-21 carries every S2 version of the design's D1 table, so on the planning date (the same day) **no row moved** (2.1); it does not carry the three GitHub action versions (the design's D1 rows `:375-377`, read the same day, do), the Gradle 9.9.2 checksums, or several verbatim quotations AC-3 needs (2.3) | verified | research §1, §2.1, §2.2, §3.1, §6 |
| L25 | Reused read-only tools exist and are tracked: `sprint-020/U-063/evidence/tools/{sanitize.py,junit_totals.py,junit_cases.py,jacoco_pkgs.py}`, `sprint-024/U-068/evidence/tools/{sdk_dir.py,jacoco_inventory.py,jacoco_lines.py,lint_ids.py}`; this workspace's `compiled-pipeline.json` is excluded in `.git/info/exclude` | verified | `git ls-files`; `.git/info/exclude:45` |
| N1 | The Gradle 9.9.2 distribution and wrapper-jar SHA-256 values (Q-1, Q-2) | supplied (supplement Q-1, Q-2: `971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b` and `621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8`, from `downloads.gradle.org` after the `services.gradle.org` redirect, equal to `gradle.org/release-checksums/`); the download's own check and D22 verify them at I1 | 2.3; rows D18, D22 |
| N2 | The 9.9.2 `gradlew` template keeps the `case "$( uname )"` block and the `cygwin`/`msys` variables the LOCAL ADDITION block tests | needs_check | I1 step 6 (`gradlew_block.py`); row D17 |
| N3 | `./gradlew wrapper` on 9.9.2 configures this build (the root `updateDaemonJvm { toolchainPlatforms = [] }` included) | needs_check | I1 step 5; row D21 |
| N4 | KSP 2.3.14 runs on KGP 2.3.21 (C9) | needs_check | I2; row D54 |
| N5 | A13: AGP 9.5.2 accepts `compileSdkVersion`/`minSdkVersion`/`targetSdkVersion` | needs_check | I3; row D56 |
| N6 | The JaCoCo class roots of `:217-221` exist after `clean` on AGP 9.5.2 | needs_check | I3; row D55 |
| N7 | `configuration.txt`, after R8-6 normalisation, is equal section by section at every increment except the sections 2.10 allows at I3 to I6 | needs_check (I3 to I6 likely to differ in their own section, P2) | 2.10; rows D42, D43 (ruling SC-P1) |
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
| N18 | ByteBuddy 1.17.7 Java 25 support statement (Q-9) | verified (supplement Q-9: Byte Buddy's release notes, 1.17.5, 30 March 2025, ASM 9.8 "to support Java 25"; the Mockito 5.24.0 entry is silent); the resolved Byte Buddy version is still read at I8 | I8; row D76 |
| N19 | JaCoCo 0.8.16 class inventory equals 0.8.14's | needs_check | I12; row D79 |
| N20 | A18 `ubuntu-22.04` still offered (Q-10) | needs_check (orchestrator) | I13; row D83 |
| N21 | `lintVitalRelease` (inside `assembleRelease`) on AGP 9.5.2 finds no new fatal issue | needs_check | I3; row D51 |
| N22 | Gradle 9.9.2's writer output parses as the same schema and the U-060 proof can compare it | needs_check | I2 onward; row D28 |
| P1 | Gradle's user guide says the `wrapper` task must run a second time to update the jar (Q-12) | verified (supplement Q-12: a single run updates `gradle-wrapper.properties` only) | L5 and the jar-checksum check (D22) still decide |
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
for an SA amendment (D1a rule 3 needs a re-read of every constraint it takes part in). The supplement reports no newer
version for any row (Q-10 re-confirms the three action versions).

**D1a resolution of the two facts the supplement adds (iteration 2).**

*(a) AGP 9.5.2, which no Android page yet describes.* Facts (supplement Q-3, Q-4, fetched 2026-11-21): the about-agp
table row reads `9.5 | 9.6.0`; the AGP 9.5 release notes' table gives Gradle `9.6.0 | 9.6.0`, SDK Build Tools
`36.0.0 | 36.0.0`, JDK `17 | 17`, and maximum API level 37; the notes have a heading for 9.5.1 and **no entry for 9.5.2**;
neither page names 9.5.2; Gradle 9.9.2's page says it is tested with AGP "9.0 through 9.5.0-alpha10". The version
research of the same day lists 9.5.2 as published on Google Maven on 2026-11-20 and newest non-pre-release (9.6.0-alpha05
is the newest pre-release). The design's rule D1a (`:439-453`), quoted:
> 1. Each story re-resolves only its own rows, from the same sources, on its planning date, and records date, version and URL (REQ-015 AC-1, REQ-016 AC-1).
> 4. Never a pre-release: […] AGP 9.6.0-alpha […]
> 5. Never a release its vendor marks defective. KSP 2.3.13 is the precedent.

and REQ-015's definition (`:95-96`): "'Latest stable' means the newest non-pre-release version on Maven Central, Google
Maven, or the Gradle plugin portal on the date the implementing story is planned." The source D1 names for this row is
"GM `com/android/tools/build/gradle`" (`:365`). 9.5.2 is a non-pre-release on that source and its vendor has not marked
it defective; D1a has no rule that excludes a release whose notes are not yet published, and the design already
foresees exactly this case as gap G5 ("AGP 9.5.2 has no release notes (§6.1) | Build; F1", `:734`) with F1 "AGP 9.5.2 →
9.5.1 | 9.5.1 has notes" (`:766`). **Decision: the target stays 9.5.2; D1a decides it, so no question is open.** The
record states that the release notes describe 9.5.1 and not 9.5.2 on the planning date, and that Gradle 9.9.2's tested
range ends at a 9.5.0 alpha (G1). **Consequence for F1:** F1 stays as the design words it, 9.5.2 → 9.5.1, taken (by
stop and report) on an AGP-originated error or a 9.5.2-only regression at I3 or later; 9.5.1 is the version the notes
describe and is in the local Gradle cache (L12). (Had 9.5.1 been the target, F1 would have had no lower patch inside
9.5 and would have become an SA amendment; that is one reason the design's value is kept.)

*(b) Kotlin 2.4.21's documented maxima.* Q-5's table cell, exact: `2.4.20–2.4.21 | 7.6.3–9.7.0 | 8.5.2–9.3.1` (columns
"KGP version | Gradle min and max versions | AGP min and max versions"); below it the page says that Gradle and AGP
versions up to the latest releases can also be used, with the caution that deprecation warnings may appear or some new
features might not work. Gradle 9.9.2 and AGP 9.5.2 are above both maxima. This does not move a row under D1a (it is a
compatibility statement, not a version); it is recorded in the compatibility reading as gap **G2**, the known gap
fallback **F2** exists for (Gradle 9.7.0 and AGP 9.3.1, both inside Kotlin's documented range), with its detector:
KGP-originated warnings are debt, a KGP-originated error naming a Gradle or AGP API, version or behaviour is F2's
trigger.

### 2.2 Compatibility reading (AC-3, AC-4; design D3 steps 1 and 2)

The developer writes `$E/compatibility-record.md` at I0 step 11 from this text, the research's §3.1 and the
orchestrator's supplement, giving table values and checksums exactly and prose as the supplement states it (the
orchestrator's ruling of 2026-11-21: the stated fact, the address and the fetch date), each labelled "table value",
"checksum" or "stated fact"; nothing is called verbatim that is not, and nothing is paraphrased beyond the supplement's
own wording. Its sections:

1. **Reading applied** (U-060 SC-5 method): primary and binding = the Gradle 9.9.2 compatibility page and the AGP 9.5
   release notes (minimum Gradle, minimum JDK, Build Tools, maximum API level); secondary checks = the Kotlin Gradle
   plugin table (`https://kotlinlang.org/docs/gradle-configure-project.html`), the KSP and Dagger release notes, the
   Robolectric compatibility table (`https://robolectric.org/compatibility_table/`).
2. **The research's "newest triple that sits inside every published range" (Gradle 9.7.0 + AGP 9.3.1 + KGP 2.4.21,
   research §3.2 point 2) is contradicted** (design A17, D3 Context): the Gradle 9.7.x page, as read for U-060, lists
   Kotlin only "through 2.4.20-Beta1" and AGP "9.0 through 9.4.0-alpha03". It is not used as evidence. **F2 is "inside
   Kotlin's range", never "inside every range"** (FD-VAL-004).
3. **Quotation table**: one row per C1 to C21 with the research's text and URL, a column "in the research: quoted /
   partly quoted / values read from a table or artifact", and, for each row the plan relies on, the supplement's item
   (Q-3 to Q-9: exact for table cells and checksums, stated fact with address and fetch date for prose), with its label.
   A citation that no longer says
   what the research says is recorded as such (row D13). The research rows the plan relies on: C1, C2 (Gradle); C3, C4,
   C5, C6, C20 (AGP); C7, C8 (Kotlin); C9, C12 (KSP); C10, C11, C13 (Dagger); C14 (androidx.hilt); C15, C16
   (Robolectric); C17 (JaCoCo).
4. **Residual gaps, verbatim from D3 (`:728-735`; the design's own text), with their detection and the planning-date
   re-read from the supplement:**

   | Gap | What the sources say | Detection | Re-read on 2026-11-21 |
   |-----|----------------------|-----------|-----------------------|
   | G1 | The Gradle 9.9 page tests AGP "9.0 through 9.5.0-alpha10" (C2); AGP 9.5.2 is beyond that pre-release bound | Build plus `--warning-mode all`; any AGP-originated error | Q-3, stated fact at the versioned URL: "tested with the Android Gradle Plugin 9.0 through 9.5.0-alpha10" — holds |
   | G2 | KGP 2.4.21 is fully supported only up to Gradle 9.7.0 and AGP 9.3.1 (C7) | KGP-originated warnings (debt) or errors (trigger) | Q-5, table value `2.4.20–2.4.21 \| 7.6.3–9.7.0 \| 8.5.2–9.3.1` and the page's caution — holds; F2 is the fallback for it (2.1 (b)) |
   | G3 | KSP 2.3.14 is tested against AGP base 9.3.0-alpha01 (C9) | KSP tasks pass, and the KSP generated-file list equals the S2-I0 list (U-060: 49 files plus the four Hilt LazyMapKey additions) | Q-6, stated fact: six KSP-internal fixes and additions, no AGP statement — holds |
   | G4 | The Hilt Gradle plugin 2.60.1 has no statement for AGP 9.5 or Gradle 9.9 (§3.2 point 4) | `hiltJavaCompileDebug`, the ASM transform, Hilt tests, R8 C-2 | Q-7, stated fact: two bug fixes (issues 5203, 5204), no AGP or Gradle statement — holds |
   | G5 | AGP 9.5.2 has no release notes (§6.1) | Build; F1 | Q-4, stated fact: the 9.5 notes have no entry for 9.5.2 and neither Android page names it — holds (2.1 (a)) |
   | G6 | Robolectric 4.18.1 on JDK 25 without flags is not re-tested (§6.7) | D7 | Q-8, table value `4.18 \| 24 → 37 \| 9.8.2 \| 9.5.0`; the 4.18.1 notes (stated fact) name no JDK flag change — holds |

   At I14 the record gains a column "symptom seen: none / <increment and evidence file>" per gap (AC-4, last sentence).
5. **Fallbacks** (2.4) and the statement "not triggered" with the `--warning-mode all` evidence if none fired (AC-5).
6. **Ordering constraints** (depth check, criterion 8): which component constrains which, the source, and what happens
   if a statement does not hold:

   | Edge | Source | Order it decides | If the statement does not hold |
   |------|--------|------------------|--------------------------------|
   | AGP 9.5 needs Gradle ≥ 9.6.0 | C3; Q-4 table value `9.5 \| 9.6.0` | I1 before I3 | D13: stop before I1 |
   | Gradle 9.9 tests AGP 9.0 | C2; Q-3 | I1 can build on AGP 9.0.1 | D26 detects it |
   | KSP is independent of the Kotlin version since 2.3.0 | C9 | I2 before I5 | D54: the pre-specified reorder |
   | An older KSP under a newer AGP is the riskier pairing (2.3.10 fixed R-class resolution with AGP 9 built-in Kotlin) | design S2 table (`:565`), C9 | I2 before I3 | — (an order choice; the build detects) |
   | Hilt 2.60 is built with Kotlin 2.3.21 | C11 | I4 before I5 | D59, D35 |
   | Kotlin 2.4 metadata needs Hilt 2.60 (Hilt 2.59.2 fails on it) | C10, C11 | I5 after I4 | F4 |
   | androidx.hilt 1.3 and newer need minSdk 23 | C14 | I6 after S1 | — (S1 is done) |
   | Robolectric 4.18 runs no test below SDK 24 | C15; Q-8 | I7 after S1 | — (S1 is done) |
   | JaCoCo last among build changes, so a coverage change from JaCoCo itself is isolated | design S2 table (`:572`) | I12 after I11 | — |

   Every edge has a build-time detector, so the supplement's items change the compatibility record and the D13 stop,
   never the order.

### 2.3 What the orchestrator supplied, and what is still needed (nobody here fetches)

`sdlc/artifacts/build/research/2026-11-21-u071-quote-supplement.md` (`$SUPP`, commit `c9b3954d`) supplies Q-1 to Q-12
in the orchestrator's ruled form: checksums and table cells exact, prose as the stated fact with the page's address and
the fetch date (2026-11-21). The table below keeps iteration 1's request columns and adds what was supplied. **Q-13 to
Q-17 are new (depth check): needed before I3** (row D44): without them an I3 difference that only those notes would
explain stops the story. Q-18 is optional. **Iteration 3:** Q-13 to Q-17 are now supplied (row below; commit
`0c99149e`); Q-18 was not fetched and stays optional.

| Id | Supplied (supplement) |
|----|-----------------------|
| Q-1, Q-2 | 9.9.2: `971fcd71…9914c76b` (bin.zip), `621008af…5f73cdaf8` (wrapper jar), in full in N1; URLs `https://downloads.gradle.org/distributions/gradle-9.9.2-bin.zip.sha256` and `…-wrapper.jar.sha256` (the `services.gradle.org` address redirects there); equal to `https://gradle.org/release-checksums/`. F2: 9.7.0 `84fbba45c7f4c64abc77460e1c00f541e9f960e3c7ed2538f1ede19eacd873ae` (bin.zip), `7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d` (wrapper jar) |
| Q-3 | Table value `25 \| 9.1.0 \| 9.1.0 and after`; stated facts: JVM 17 to 27 to run Gradle; tested with Kotlin 2.0.0 through 2.5.0-Beta1; tested with AGP 9.0 through 9.5.0-alpha10 |
| Q-4 | Table values `9.5 \| 9.6.0`; Gradle `9.6.0 \| 9.6.0`, SDK Build Tools `36.0.0 \| 36.0.0`, JDK `17 \| 17`; stated facts: maximum API 37; a 9.5.1 heading, no 9.5.2 entry, no Android page names 9.5.2 |
| Q-5 | Table value `2.4.20–2.4.21 \| 7.6.3–9.7.0 \| 8.5.2–9.3.1`; stated fact: newer Gradle and AGP allowed with a caution |
| Q-6 | Stated facts: 2.3.14's six KSP-internal changes (incl. the fix for 2.3.13's concurrent-run regression and the incremental skip when no file is dirty); 2.3.13 carries "Defect in release" (issue 3286) |
| Q-7 | Stated facts: 2.60.1 fixes issues 5203 (compiler options such as `fastInit`) and 5204 (`@CanIgnoreReturnValue` import) |
| Q-8 | Table value `4.18 \| 24 → 37 \| 9.8.2 \| 9.5.0`; Java 21 for API 37; stated facts of 4.18.1, including "content providers are now installed for lazily created applications" |
| Q-9 | Stated facts: the Mockito 5.24.0 entry says nothing about Java 25 or Byte Buddy; Byte Buddy's notes, 1.17.5 (30 March 2025): ASM updated to 9.8 to support Java 25 |
| Q-10 | Stated fact: `ubuntu-22.04` listed without a deprecation note; latest checkout v7.0.1, setup-java v6.3.0, cache v6.1.0 (day and month shown, no year) |
| Q-11 | Stated facts: 0.8.16 (2026/10/24): Java 27, experimental Java 28, platform class loader instrumentation, ASM 9.11.1; no filter change listed |
| Q-12 | Stated fact: a single `wrapper` run updates only `gradle-wrapper.properties`; run it a second time for all files |
| Q-13 to Q-17 (iteration 3) | Table values: Gradle minimum and default 9.3.1 (9.1), 9.4.1 (9.2), 9.5.0 (9.3), 9.6.0 (9.4, 9.5); SDK Build Tools `36.0.0 / 36.0.0` and JDK `17 / 17` for all five; KGP row `2.2.10 / 2.2.10` on 9.1 and 9.2 only. Stated facts: no page has a "Behavior changes" or "Breaking changes" section; 9.1 R8 repackages classes it may rename into the unnamed package by default (`-dontrepackage` turns it off); 9.2 wildcard `-keepattributes` patterns no longer keep the three `RuntimeInvisible…Annotations` attributes; 9.3 a new `analyzeReleaseR8Config` task, an `optimization` block (the earlier DSL still supported) and `src/<variant>/keepRules/*.keep`; 9.4 `android.newDsl.optOut`, a dynamic-feature variant warning; 9.5 experimental backup tests (off by default); no item on lint, unit tests, KSP, built-in Kotlin, the manifest merger, BuildConfig or R generation. How each bears on I3: 2.11 |

New items, needed before I3:

| Id | What | URL | Why |
|----|------|-----|-----|
| Q-13 | AGP 9.1 release notes: the "Behavior changes" entries and every entry that changes a default value (DSL, `android.*` property, lint, unit-test option, packaging, R8) | https://developer.android.com/build/releases/agp-9-1-0-release-notes | I3 crosses four minor versions; an I3 difference in the APK entries, manifests, R8 sections, coverage or lint is classified against these entries (2.8, D44) |
| Q-14 | The same for AGP 9.2 | https://developer.android.com/build/releases/agp-9-2-0-release-notes | as Q-13 |
| Q-15 | The same for AGP 9.3 | https://developer.android.com/build/releases/agp-9-3-0-release-notes | as Q-13 |
| Q-16 | The same for AGP 9.4 | https://developer.android.com/build/releases/agp-9-4-0-release-notes | as Q-13 |
| Q-17 | The same for AGP 9.5 (9.5.0 and 9.5.1) | https://developer.android.com/build/releases/agp-9-5-0-release-notes | as Q-13 |
| Q-18 | (optional) Gradle's upgrade guide entries for 9.6 to 9.9 that change a default or a behaviour of tasks this build runs (test, JaCoCo, wrapper, dependency verification) | https://docs.gradle.org/9.9.2/userguide/upgrading_version_9.html | only if an I1 difference needs a source; without it an unexplained I1 difference stops (D26) |

The original request table of iteration 1 (what was asked and why the research did not suffice) follows for the record
only. Its word "verbatim" is superseded by the orchestrator's ruling: the answers are in the ruled form above, and the
record labels them so.

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

If F2 fires, the 9.7.0 checksums are already supplied (Q-1, Q-2 row above); the Gradle 9.7 compatibility page is read
by the orchestrator then (the supplement says so).

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
| The same evidence set at every increment (STD, 4.0) | Every commit list is derivable from one rule; every comparison is available at every increment | About 27 small files per increment | Evidence only where an AC names it | Plan-review B-4 of U-068 (a list naming a file no step writes) cannot recur |
| A throwaway provider probe at I0 and I7 (4.P) | AC-12 (d)'s "both lists" exist whether or not a test fails; design D7 item 5's "environment probes" are run | A file copied into `app/src/test` twice for one targeted run each, under a write-ahead protocol | (a) no probe; (b) re-adding U-063's helper smoke test | (a) leaves only Truth's failure message, which shows the allow-list and the filtered result but not the provider's default-enabled set; (b) depends on fixtures whose API may have moved since U-063 (SC-P2) |
| R8 compared at six levels (R8-1 to R8-6, 2.10) with ruling SC-P1 applied per normalised section | A changed rule is accepted only in the raised component's own section, and only while the kept classes, the manifest closure, the member-level seeds and the contract-bearing rules all hold; path and version noise in section headers cannot stop the story (I1, I2) | Six more evidence files per increment and one tool; a seeds difference from regenerated code stops for a decision (R20) | (a) iteration 1's raw-text equality of `configuration.txt`; (b) class names in four prefixes only | (a) stops at I1 on the Gradle version in every header and cannot judge "every changed rule" (contract-verification B-2); (b) cannot see Tink's protobuf fields, worker constructors or manifest classes outside the prefixes (B-1) |
| Increments, not one raise | Every failure has one suspect; the verification additions can be attributed to one group (D5 step 5) | 14 commits, 71 Gradle runs, six sessions; two intermediate pairings no vendor table lists jointly (Gradle 9.9.2 with AGP 9.0.1 at I1; AGP 9.5.2 with Kotlin 2.3.21 at I3 and I4), each guarded by D26 and D35 | One raise of all versions in one commit | With twelve suspects per failure `git bisect` is useless and the group review cannot attribute an artifact; the design and REQ-015 "Increments" bind it anyway |
| Versions stay inline; no version catalog | The exact-text edits stay valid (L11, `oldtext_check.py`); the AC-16 sweep and AC-23's file set apply unchanged | Version tokens stay spread over two build files and their comments | Moving to `gradle/libs.versions.toml` (it does not exist, research §2.1) | A catalog is a refactoring with its own characterisation (resolved classpaths equal before and after); in this story it would be a second change in every increment. Suitable after S4, as its own story |
| Verification stays additions-only (D5) | No checksum can be removed or altered unseen: the set proof detects tampering with a recorded entry (INT-001) | The file keeps the old 9.0.1-era AGP and 2.3.x Kotlin entries and grows by several hundred; a clean-up needs its own story. **What it does not protect:** `verify-signatures` is `false` (L8), so the writer records whatever the repository serves during the pass (trust on first use: it protects against later substitution, not a bad first fetch; the trusted network and the group review are the only guard there); the two `<trust … regex="true"/>` lines exempt `-javadoc.jar` and `-sources.jar`; `android-all` jars are checked against `.sha512` files from the same host (integrity in transit only) | Wholesale regeneration; turning signature verification on | Regeneration makes a removed or altered checksum invisible to the proof; signatures would need a keyring for every group, which no requirement asks for (a recommendation for later) |
| Caches off on every comparison run | No comparison depends on a developer's user-wide settings (`clean` does not stop `FROM-CACHE` outputs) | Nothing: both caches are off at project level (L23) | Relying on the defaults | `.claude/CLAUDE.md` warns that a user-wide `gradle.properties` replaces project settings; G, ENF, TR, L, DEP and the up-to-date runs pass `--no-build-cache --no-configuration-cache`; WP stays the design's exact command, so I0 step 1 proves no `gradle.properties` turns a cache on (D94) |
| Stop on a fallback (ruled) | The product owner sees every fallback before the next increment builds on it | A pause per fallback | D3 step 5's "take it and continue" | Ruled |
| Mutations before the I5 commit | The I5 commit carries its own mutation evidence (AC-24) | The protocol must prove the restore against the working tree, not only HEAD | Mutations after the commit | AC-24: each commit contains its increment's evidence |
| Forced production fixes: only the pre-specified `@param:` edits; any other forced fix stops for an SA-supplied exact edit (SC-P4) | Every production edit is exact text a reviewer can check | A stop for a javac `-Werror` warning that D3 lets the developer fix | Letting the developer write the call-site fix | The delegation asks for exact old/new text for every edit; no such warning is predicted |
| Two `--configuration` reports per DEP step (U-060's form; iteration 2) | No run resolves configurations the writer pass never covers; each report parses one configuration exactly | One more short run per DEP | One plain `:app:dependencies` report | It may fail verification on androidTest or lint classpaths the gates never resolve (plan-review O-3; D97) |

### 2.6 Coverage arithmetic (re-derived at I0; rows D38, D39, D60, D79)

For a package with `c` covered of `t` lines and floor `f`, the gate needs `c/t ≥ f`; spare lines = `c − ⌈f·t⌉`
(`cov_floors.py`, exact fractions). Known margins after U-068 and U-069 (L14, L15, L17; re-measured at I0):

| Package (floor) | Expected at I0 | Spare | What can move it in this story |
|-----------------|----------------|-------|--------------------------------|
| `worker` (0.90, U-069, ruling 47) | 10/10 = 100 % (U-069 Amendment A4; 16/16 in the ruling's example) | **1** (`⌈0.90·10⌉ = 9`; also 1 at 16 lines) | a generated class starting to report lines (Hilt 2.60.1 at I4, androidx.hilt 1.4.0 at I6, JaCoCo 0.8.16 filters at I12), or Kotlin 2.4 changing line attribution (I5): one more uncovered line is absorbed and recorded, a second fails the gate; rows D38, D60 |
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
| R8-1 to R8-6 (2.10): kept classes, manifest closure, member seeds, contract-bearing rules, normalised sections | R8's keep input per source section, the classes and the members it keeps (Tink's protobuf fields, the worker constructors), and that every manifest class keeps its name | Whether R8's optimisation of non-kept code changed (`-dontoptimize` is set; mapping line counts and `usage-core` are recorded); behaviour of the kept code at run time (IC-4, IC-5) |
| Packaged backup descriptor | The two `sharedpref` exclusions (`credentials.xml`, `pinned_certs.xml`, CNTR-003, CNTR-002) survive resource processing in both APKs | That the platform honours them in a real backup |
| Clean gate runs, plus one up-to-date run after G(2) and G(5) | The full build from scratch; that KSP and Kotlin tasks are up to date on an unchanged tree (the Windows KSP2 recompilation the old KSP comment mentions) | Incremental correctness after a real source edit (no source is edited in this story) |
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
- **The harness moves a long Gradle run to the background**: the expected case for WP (12 to 16 min) and many G runs
  (9 to 11 min, longer on the first run of a new Gradle version with cold caches), against the 600000 ms foreground
  limit; 4.L: wait for the completion notice, never poll, read the result from the log. A run cut off instead is D93.
- **A Kotlin compile daemon outlives the Gradle run** (`org.gradle.daemon=false` covers only Gradle's own daemon; at
  I5 a 2.3.21 daemon may stay beside the 2.4.21 one): the process checks match `gradle|KotlinCompileDaemon` and record
  Kotlin daemons without stopping; a Windows file lock that makes `clean` or `rm -rf $R` fail with "Unable to delete
  file" is D92 (stop; no process is killed, nothing deleted by other means, no retry loop).
- **Defaults AGP 9.1 to 9.5 change**: the build sets `buildFeatures { buildConfig true }` (`app/build.gradle:41-45`),
  `minifyEnabled true` with the optimize file and `-dontoptimize`, the `lint` block, `testOptions.unitTests.includeAndroidResources`,
  and in `gradle.properties` `android.nonTransitiveRClass=true` and `android.nonFinalResIds=true`; those cannot move.
  Any other I3 difference is classified against Q-13 to Q-17's entries; an entry they do not explain stops (D44).
- **JaCoCo 0.8.16 changes a Kotlin filter**: a package's `t` may change with the class inventory equal; Q-11 lists no
  filter change; recorded with the per-class lines, a stop only if a package goes `BELOW` or the inventory differs
  (2.9, D40, R19).
- **The developer's user-wide `gradle.properties` turns the build or configuration cache on**: I0 step 1 checks
  without printing a path; the comparison runs pass `--no-build-cache --no-configuration-cache` anyway (D94).
- **Robolectric 4.18.1 now installs content providers for lazily created applications** (Q-8): a candidate cause if
  an application-context test changes at I7 (D37); the manifest has no `InitializationProvider`, so nil effect expected.
- **Endpoint security removes another tool** (`unzip`, `sha256sum`, `git` parts): stop and report (D52).
- **A session ends mid-increment**: the resume protocol (4.R) and the write-ahead lines decide what is undone.
- **The orchestrator commits documents while the developer runs**: every end check is scoped to this story's subjects
  and paths (I14 step 7).
- **`ci.yml` has mixed line endings in the working tree** (L2): edits are by exact text; `git` normalises on diff and
  commit; the YAML parse reads the edited file.
- **PyYAML reads `on:` as the boolean `True`** (YAML 1.1): expected in the parse output (I13 step 5).

### 2.9 Invariants per increment (AC-21) and what may legitimately change

"=" means equal to the previous increment `<p>` (which decides) and to I0 (recorded; after a difference accepted at an
earlier increment, the I0 comparison carries exactly the accepted differences, listed in the log); a difference is a
stop unless the cell names it.

| Check | I1 | I2 | I3 | I4 | I5 | I6 | I7 | I8–I11 | I12 | I14 |
|-------|----|----|----|----|----|----|----|--------|-----|-----|
| Merged and packaged manifests (normalised), invariants, backup descriptor | = | = | = | = | = | = | = | = | = | = |
| R8-1 kept names (4 prefixes) | = | = | = | = | = | = | = | = | = | = |
| R8-1 kept names (all); R8-2 closure (`not-identity=0`; `manifest-classes-in-mapping` = `<p>`) | = ; OK | = ; OK | = or traced (2.10); OK | = or traced; OK | = or traced; OK | = or traced; OK | = ; OK | = ; OK | = ; OK | = ; OK |
| R8-3 `seeds-core` (`usage-core` classified) | = | = | = (a difference stops for the orchestrator, ruling 58 does not cover I3) | = `<p>`, or accepted under ruling 58 (Hilt/Dagger patterns) | = `<p>`, or accepted under ruling 58 (Kotlin compiler-generated members) | = `<p>`, or accepted under ruling 58 (Hilt/Dagger patterns) | = | = | = | = |
| R8-4 contract-bearing rules | OK | OK | OK | OK | OK | OK | OK | OK | OK | OK |
| R8-5/R8-6 normalised `configuration.txt` sections | = | = | = `<p>`, or accepted under SC-P1 in `agp-default:proguard-android-optimize.txt` only | … in `lib:hilt-android/proguard.txt` or Dagger's `jar-rules:…/r8/r8.pro` | … in a `jar-rules:` section changed or added from the Kotlin stdlib jar | … in `lib:hilt-work/proguard.txt` or a section from an androidx.hilt 1.4.0 artifact | = | = | = | = |
| `mapping.txt` header: `# min_api` / `# compiler_version` | 24 / = | 24 / = | 24 / changes (new R8 with AGP 9.5.2; recorded) | 24 / = | 24 / = | 24 / = | 24 / = | 24 / = | 24 / = | 24 / = |
| Record-only (2.10): release-dex worker-name counts, `dexdump -f` header counts, baseline-profile hashes | recorded | recorded | recorded (counts and hashes expected to change with the new R8 and Q-14) | recorded | recorded | recorded | recorded | recorded | recorded | recorded |
| APK entry lists | = | = | explained by AGP only (2.11: e.g. `META-INF/services/<obfuscated name>` entries renamed by repackaging) | explained by Hilt only | explained by Kotlin only | explained by androidx.hilt only | = | = | = | = |
| `.so` count | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 | 0 |
| KSP file list | = | = (AC-7) | = | differences explained (AC-9) | = I4's list (SC-P3) | = I4's list | = | = | = | = I4's list |
| javap majors 61, 61, 52 | = | = | = | = | = | = | = | = | = | = |
| Case list, JUnit totals | = | = | = | = | = | = | = | = | = | = |
| Coverage `t` per package; class inventory | = | = | explained by AGP only | explained by Hilt only | explained by Kotlin only | explained by androidx.hilt only | = | = | inventory = I11 (AC-14); a `t` change with an equal inventory is recorded with the per-class lines and Q-11's change history, a stop only if a package goes `BELOW` (D38) | = |
| `releaseRuntimeClasspath` (S4-owned versions) | — | — | — | = | = | = | = | — | — | = |

The invariant greps run on the packaged manifests (`aapt2 dump xmltree`) and the merged ones, each expected `1` except
`InitializationProvider` `0` (values derived from U-068's I5 files, L14): `grep -A4
'android:name(0x01010003)="androidx.work.impl.foreground.SystemForegroundService"' F | grep -c
'foregroundServiceType(0x01010599)=0x00000001'`; `grep -c InitializationProvider F`; `grep -A2
'android:name(0x01010003)="com.zegoggles.smssync.receiver.BackupBroadcastReceiver"' F | grep -c
'exported(0x01010010)=true'`; the same `grep -A5 … | grep -c '"com.zegoggles.smssync.BACKUP"'`; `grep -c
'minSdkVersion(0x0101020c)=24' F`; and in the merged files `grep -c 'android:minSdkVersion="24"'`. The backup
descriptor dump (EV-B) per APK: `grep -c 'E: exclude'` → `2`, `grep -c 'domain="sharedpref"'` → `2`,
`grep -c 'path="credentials.xml"'` → `1`, `grep -c 'path="pinned_certs.xml"'` → `1` (L27).

### 2.10 R8 checks and the allowed-section table (contract-verification B-1, B-2; ruling SC-P1)

No contract mentions R8; the contract obligations live in keep rules shipped by libraries (CNTR-004 and DES-008: the
`@HiltWorker` class names WorkManager persists; CNTR-003: Tink's protobuf fields). So the comparison works at six levels,
all from the release build's own outputs (`app/build/outputs/mapping/release/`; I0 lists the folder, row D98), via
`r8_check.py` (4.0), at every increment k ≥ 1 against `<p>` (decides) and I0 (recorded):

- **R8-1 Kept classes.** `kept-names.txt` (four prefixes, as U-060 C-2) must equal `<p>`: hard stop. `kept-names-all.txt`
  (every identity line of `mapping.txt`): a difference outside the four prefixes is accepted only if traced to a changed
  rule of the raised component's own section (R8-5); otherwise stop.
- **R8-2 Manifest closure.** Every `android:name`/`appComponentFactory` value of the packaged release manifest that is a
  class in `mapping.txt` is identity-mapped (27 classes today, L26): hard stop. The tool also prints `NOT-IN-MAPPING`
  for each manifest class name absent from `mapping.txt` (actions and permissions are not listed), and CMP compares
  `manifest-classes-in-mapping` with `<p>`'s: a lower count is a stop (plan-review O-15, contract-verification O-7).
- **R8-3 Member seeds.** `seeds-core.txt` (`seeds.txt` restricted to `com.zegoggles.smssync.`, `androidx.work.`,
  `com.google.crypto.tink.`, `androidx.core.app.CoreComponentFactory`, `androidx.profileinstaller.`,
  `androidx.room.MultiInstance`) must equal `<p>` (it sees Tink's fields and the worker constructors): a hard stop,
  **except as ruling 58 accepts at I4, I5 and I6**, judged by `r8_check.py seeds-diff` (CMP), which prints every added
  (`+`) or removed (`-`) line with its verdict:
  - `MATCH <pattern>`: at I4 and I6 the class's simple name (or, for an inner class, its top-level simple name) matches
    one of the Dagger/Hilt patterns `Hilt_*`, `*_Factory`, `*_HiltModules*`, `*_HiltModule`, `*_AssistedFactory*`,
    `*_MembersInjector`, `*_GeneratedInjector`, `*_HiltComponents*`, `*_ComponentTreeDeps`, `Dagger*`, or the full
    name matches `hilt_aggregated_deps.*` or `dagger.hilt.internal.aggregatedroot.codegen.*`; and for a class in
    `com.zegoggles.smssync.` its source file `<package path>/<TopLevel>.java` exists under one of the generated roots
    passed to the tool (`app/build/generated/ksp/release/java`, `app/build/generated/hilt/component_sources/release`;
    if AGP 9.5.2 writes them elsewhere, the developer lists `app/build/generated` one level deep, passes the folders
    found and records it) — otherwise `NOT-GENERATED`. At I5 only the Kotlin compiler-generated members: the member
    (or, for a class-only line, the simple name) contains `$default(`, `access$`, the word `Companion`,
    `$WhenMappings`, `$EntriesMappings` or `$$delegatedProperties`; any other member is `NO MATCH` (a stop).
  - `FORBIDDEN`: a line under `com.google.crypto.tink.`, or a **worker-constructor line** — a line of the form
    `com.zegoggles.smssync.service.BackupWorker: BackupWorker(…`, `….service.RestoreWorker: RestoreWorker(…` or
    `….worker.BackupTriggerWorker: BackupTriggerWorker(…` (the form `seeds.txt` uses, read earlier in this session:
    `com.zegoggles.smssync.service.BackupWorker: BackupWorker(android.content.Context,androidx.work.WorkerParameters,…)`),
    which includes Kotlin's synthetic constructor ending in `kotlin.jvm.internal.DefaultConstructorMarker)`.
  - `NO MATCH` for anything else, for every changed line at an increment other than I4, I5, I6, for every line under
    `androidx.work.`, `androidx.core.app.`, `androidx.profileinstaller.` or `androidx.room.`, and at I5 for every line
    outside `com.zegoggles.smssync.` (Amendment A1, contract-verification O-16). A removed generated class may show its
    source in `<p>`'s KSP file list instead (`--prev-ksp`, plan-review O-21).

  The tool exits 0 only when every changed line is `MATCH`. Ruling 58 then also needs R8-1, R8-2 and R8-4 to hold and
  each changed line copied into the log with its pattern (D42). **At I3** (the new R8 that comes with AGP 9.5.2) a
  `seeds-core` difference is not covered by ruling 58: stop and report with the changed lines; the orchestrator decides
  (plan-review O-16).
  `usage-core.txt` (the same prefixes of `usage.txt`, class blocks with their removed members) is compared the same way
  and every difference is classified in the log; it stops only when a removed entry is a class or member listed in
  `<p>`'s `seeds-core.txt` or the closure (that would be a kept thing removed) — **except** a line that `seeds-diff`
  accepted under ruling 58 at the same increment, which is recorded with it.
- **R8-4 Contract-bearing rules present, whatever their source** (`r8_check.py rules`): (a) androidx.hilt's
  `-keepnames @androidx.hilt.work.HiltWorker class * extends androidx.work.ListenableWorker` or (b) work-runtime's
  `-keep public class * extends androidx.work.ListenableWorker {` with `public <init>(...);` — at least one, and the
  three `@HiltWorker` classes identity-mapped (R8-1); (c) `-keep class androidx.work.WorkerParameters`; (d) Tink's
  `-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {` with `<fields>;`;
  (e) `-keep class androidx.work.impl.foreground.SystemForegroundService { <init>(); }`; (f) hilt-android's
  `-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class *`; (g) the `app:proguard-rules.pro` section
  equal to the committed file, with `-dontoptimize`; and the AGP default file's `-keepattributes` still naming
  `Signature` and `RuntimeVisibleAnnotations`. A missing or reworded rule among these stops the story even when it comes
  from the raised component (a contract depends on it); the SA decides (D95).
- **R8-5 Allowed sections** (ruling SC-P1's "the raised component's own shipped rules", made checkable):

  | Increment | Section whose body may change (after R8-6) | Condition |
  |-----------|--------------------------------------------|-----------|
  | I1, I2, I7 to I12, I14 | none | every key `EQUAL` |
  | I3 | `agp-default:proguard-android-optimize.txt` | its `-keepattributes` keeps `Signature` and `RuntimeVisibleAnnotations` (R8-4) |
  | I4 | `lib:hilt-android/proguard.txt`; `jar-rules:lib/META-INF/com.android.tools/r8/r8.pro` (Dagger's own jar rule, `-identifiernamestring @dagger.internal.IdentifierNameString`, U-068 `I5-configuration.txt:623-628`), changed or added, shown by `unzip -l` of the resolved `dagger-2.60.1.jar` (ruling 48; plan-review O-14) | R8-4 (f) holds |
  | I5 | a `jar-rules:` section **changed or added** (none comes from `kotlin-stdlib` at the start, so `ADDED` is the likely form), only if the developer shows the same path inside the resolved `kotlin-stdlib-2.4.21.jar` (`unzip -l` of the jar the build resolved; no path written) (ruling 48) | not a `coroutines.pro` section (coroutines are S4's) |
  | I6 | `lib:hilt-work/proguard.txt`, and any section changed or added that the developer shows inside a resolved androidx.hilt 1.4.0 artifact (`hilt-work`, `hilt-common`) (ruling 48) | R8-4 (a) or (b) holds |

  Every increment: the `aapt:aapt_rules.txt` section is manifest-derived, so any change there is a stop; a section
  `ADDED` or `REMOVED` that belongs to another library is a stop (and a changed resolved S4 library is also D31); the
  `generated:proguard.txt` and `unknown` sections must stay `EQUAL`. A section `ADDED` or `CHANGED` is inside ruling 48
  only when the developer names the raised component's jar it comes from (`unzip -l`, the path in the jar equal to the
  section key's path **after dropping the leading `lib/`** of `jar-rules:` keys, plan-review O-22) **and** shows that the
  section body is that file's content (contract-verification O-15):
  `python -I -X utf8 $TL/r8_check.py section $E/I<k>-configuration-norm.txt '<key>' $WORK/sec.txt; unzip -p <jar> <path in jar> | tr -d '\r' > $WORK/jar.txt; git -c core.autocrlf=false diff --no-index --quiet -- $WORK/sec.txt $WORK/jar.txt; echo "section-equals-jar-exit=$?"`
  → `0`, recorded in the log (both sides LF: `section` writes LF on every OS and `tr -d '\r'` makes the jar copy LF,
  Amendment A2; `section` strips leading and trailing blank lines; if only blank lines differ, the
  comparison is repeated on `grep -v '^[[:space:]]*$'` copies of both files and that is recorded). Tested in planning: `section`
  writes the three-line Dagger `r8.pro` body from U-068's normalised `I5-configuration.txt`.
- **Record-only R8 and dex characterisation** (depth check; contract-verification O-6), written to `I<k>-r8.txt`:
  the `mapping.txt` header lines `# compiler`, `# compiler_version`, `# min_api` (`min_api: 24` at every increment, a
  stop if not; `compiler_version` changes at I3 only, recorded); for each persisted worker name
  (`com.zegoggles.smssync.service.BackupWorker`, `…service.RestoreWorker`, `…worker.BackupTriggerWorker`) the count of
  its dotted string in the release dex files (the `@StringKey` value of the worker map, which only the generated
  component references) — **not verified in planning** (the release APK was not read while another agent's Gradle run
  was live), so the count is recorded and compared with `<p>`, and a difference is classified and reported, never a
  stop by itself; the `dexdump -f` header counts (`magic`, `class_defs_size`, `method_ids_size`, `field_ids_size`) of
  the release `classes.dex`; and the SHA-256 of `assets/dexopt/baseline.prof` and `baseline.profm` (both present in
  U-068's release entry list). **No resource shrinker:** `app/build.gradle` has no `shrinkResources` (`git grep` at
  HEAD: none), so no resource-shrinker output exists; resources are covered by the APK entry lists and the manifest and
  backup-descriptor dumps.
- **R8-6 Header normalisation.** `r8_check.py normalize` reduces each section header to `<kind>:<artifact without
  version>` (`aapt`, `agp-default`, `app`, `generated`, `lib:<artifact>/<file>`, `jar-rules:<path>`, `unknown`), dropping
  the Gradle version, the transform hash and the version; sections are sorted by key; the raw `configuration.txt` is
  kept as evidence. On U-068's I0 and I5 files the two normalised forms are equal (L26).

**Ruling SC-P1 applied (D43):** a section difference is accepted and recorded when R8-1's four-prefix set equals `<p>`
and every added or removed rule lies in the section R8-5 allows for this increment (its source named in the log, rule
by rule), with R8-2, R8-3 (as ruling 58 reads it) and R8-4 holding; any other difference stops.

### 2.11 What AGP 9.1 to 9.5 change at I3, and what the checks will see (Q-13 to Q-17)

None of the five release-notes pages has a "Behavior changes" section (supplement). Two R8 changes bear on I3; the
rest (9.3's analyzer task, `optimization` block and `keepRules` folder; 9.4's `newDsl.optOut` and dynamic-feature
warning; 9.5's experimental backup tests, off by default) touch nothing this build uses: it keeps the earlier DSL
(`minifyEnabled`, `proguardFiles`), which 9.3's page says is still supported, and has no dynamic-feature module.

| Change | What each check sees at I3 | Recorded (explained by the item) | A stop |
|--------|----------------------------|----------------------------------|--------|
| **Q-13 (9.1): R8 repackages classes it may rename into the unnamed package by default** (as `-repackageclasses`; `-dontrepackage` turns it off) | `mapping.txt`: the obfuscated targets of renamed classes and the line count change; `kept-names*.txt` (identity lines) and `seeds-core`/`usage-core` (original names) do not; the R8 sections of `configuration.txt` do not (it is a default, not a rule); APK entries named after an obfuscated class change — U-068's release list has `META-INF/services/q4.b0` and `…/v4.t` (the two coroutines service interfaces), whose names will differ; the release-dex record-only counts change | the mapping-line count, and each `META-INF/services/<name>` entry whose APK name changed while its original name did not: `I<k>-services.txt` (Amendment A1; written at every increment from that increment's own `mapping.txt`, so no earlier `mapping.txt` is needed after I3's `clean`) gives each entry as `ENTRY <apk name> = <original name>`; the developer pairs I2's and I3's `ENTRY` lines **by original name** and lists each pair with Q-13 (for U-068's `q4.b0` and `v4.t` the originals are the two coroutines service interfaces); the number of `ENTRY` lines stays equal | a `PAIR` line (original interface → its implementations in original names, the same file) that differs from I0's or `<p>`'s (`services-pairs … exit=1`); an entry whose original name is `(unmapped)` (a non-obfuscated name) but whose APK name changed; an original name present at `<p>` and missing at I3; a kept class, a manifest class (R8-2), a `seeds-core` line or a `kept-names` line changing (D44, D101) |
| **Q-14 (9.2): wildcard `-keepattributes` no longer keeps the three `RuntimeInvisible…Annotations` attributes** | the merged configuration at the start has one wildcard, `-keepattributes *Annotation*` in `lib:coordinatorlayout/proguard.txt` (U-068 `I5-configuration.txt:452`); the AGP default file and lifecycle-runtime name their attributes in full and name no `RuntimeInvisible…` one; the rule text does not change, so R8-5/R8-6 see no difference; runtime-invisible annotations cannot be read by reflection at run time, so no kept name, seed or manifest entry depends on them; the release dex may lose annotation items: record-only dex counts and APK size change | the dex header counts and sizes, with Q-14 | any R8-1 to R8-4 check failing |

No `-dontrepackage` and no keep rule is added: no check is expected to fail from either change, and nothing in the
app reads an obfuscated class name or a runtime-invisible annotation. If a check does fail, the developer stops (D42,
D44, D95) and the SA decides with the evidence; adding a rule would be a `proguard-rules.pro` edit, outside AC-23.

## 3. Exact Changes

Every edit is one Edit-tool call whose `old_string` is the text given, copied from a Read of the file. Each occurs
exactly once at its increment (twice for `cache@v4`, replaced with `replace_all`) — verified by simulation (L11) and
re-checked at I0 by `oldtext_check.py` (row D6). Expected `git diff --numstat HEAD` of the file after the increment's
edits is given per row (L11). U-069's two Rule 15 lines are not touched by any edit.

### 3.1 S2-I1 — the wrapper (generator) and the `gradlew` block (hand, byte for byte)

Generated by `./gradlew wrapper --gradle-version 9.9.2 --distribution-type bin --gradle-distribution-sha256-sum 971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b` (Q-1)
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
`./gradlew` commands keep their text (AC-15 (e), (h)). Q-4's table (Build Tools `36.0.0 | 36.0.0` under
"Minimum version | Default version") is what "minimum and default" restates; the log gives Q-4's table values and URL
beside it.

### 3.4 S2-I14 — comments and `code-location.md` (numstat `9 9`, `8 8`, `3 3`, L11)

`build.gradle`:

| Id | `old_string` | `new_string` |
|----|--------------|--------------|
| E14.1 | the three lines `:7-9`: `        // U-060: Gradle 9.5.1 / AGP 9.0.1 / KGP 2.3.21 run the build on JDK 25 (app bytecode stays 17).` / `        // AGP 9.0 needs Gradle >= 9.1.0, JDK >= 17, KGP >= 2.2.10, SDK Build Tools >= 36.0.0` / `        // (developer.android.com/build/releases/agp-9-0-0-release-notes).` | `        // U-071: Gradle 9.9.2 / AGP 9.5.2 / KGP 2.4.21 run the build on JDK 25 (app bytecode stays 17).` / `        // AGP 9.5 needs Gradle >= 9.6.0 and JDK >= 17, SDK Build Tools minimum and default 36.0.0 (agp-9-5-0 notes);` / `        // AGP 9 built-in Kotlin needs KGP >= 2.2.10 (developer.android.com/build/releases/agp-9-0-0-release-notes).` |
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
| `r8_check.py` | this plan, iteration 2 (contract-verification R8-1 to R8-6; tested on U-068's `I0`/`I5-configuration.txt` and the current release outputs, L26) | header normalisation, section comparison, contract-bearing rules, manifest closure, contract-prefix seeds and usage |
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

`r8_check.py` (iteration 2; `seeds-diff` mode and the `closure` count lines added in iteration 3):
```python
"""R8 evidence for U-071 (contract-verification R8-1 to R8-6). usage (from the repository root):
  python -I -X utf8 r8_check.py normalize <configuration.txt> <out>     R8-6: one '=== <key>' line per section, then its
        body; key = kind:artifact without version, Gradle version or transform hash; sections sorted by key
  python -I -X utf8 r8_check.py compare <old normalized> <new normalized>   per key: EQUAL, CHANGED +a -r, ADDED, REMOVED
  python -I -X utf8 r8_check.py rules <normalized> <proguard-rules.pro>    R8-4: the contract-bearing rules are present
  python -I -X utf8 r8_check.py closure <packaged release manifest dump> <mapping.txt>   R8-2: manifest classes identity-mapped
  python -I -X utf8 r8_check.py core <seeds.txt|usage.txt> <out>        R8-3: entries of the contract prefixes only
  python -I -X utf8 r8_check.py seeds-diff <old seeds-core> <new seeds-core> <I4|I5|I6|other> [--prev-ksp=<file>] [<generated root> ...]
        ruling 58: every added (+) or removed (-) line printed with the pattern it matched, or FORBIDDEN / NO MATCH /
        NOT-GENERATED; a class of the app's own package must have its source under one of the generated roots (a
        removed line may instead be found as '.../java/<path>.java' in the previous increment's KSP file list)
  python -I -X utf8 r8_check.py services <release apk> <mapping.txt> <out>   Amendment A1: each META-INF/services entry
        with its name and implementation lines translated to original names through mapping.txt
  python -I -X utf8 r8_check.py section <normalized> <key> <out>        Amendment A1: the body of one section, to compare
        with the jar file's own content
Exit 1 when a check fails (compare: when any key is not EQUAL; seeds-diff: when any line is not MATCH)."""
import fnmatch, os, re, sys, zipfile
PREFIXES = ('com.zegoggles.smssync.', 'androidx.work.', 'com.google.crypto.tink.', 'androidx.core.app.CoreComponentFactory',
            'androidx.profileinstaller.', 'androidx.room.MultiInstance')
HEAD = '# The proguard configuration file for the following section is '
END = '# End of content from '
def key(path):
    p = path.strip().replace('\\', '/')
    if p == '<unknown>':
        return 'unknown'
    name = p.rsplit('/', 1)[-1]
    if '/aapt_proguard_file/' in p:
        return 'aapt:' + name
    if '/default_proguard_files/' in p:
        return 'agp-default:' + re.sub(r'-\d[\w.\-]*$', '', name)
    if '/generated_proguard_file/' in p:
        return 'generated:' + name
    if p.endswith('/app/proguard-rules.pro'):
        return 'app:proguard-rules.pro'
    m = re.search(r'/transformed/shrink-rules/(.+)$', p)
    if m:
        return 'jar-rules:' + m.group(1)
    m = re.search(r'/transformed/([^/]+)/(.+)$', p)
    if m:
        return 'lib:' + re.sub(r'-\d[\w.\-]*$', '', m.group(1)) + '/' + m.group(2)
    return 'other:' + name
def sections(path):
    out, cur, body = [], None, []
    for raw in open(path, encoding='utf-8', errors='replace'):
        line = raw.rstrip('\r\n')
        if line.startswith(HEAD):
            cur, body = key(line[len(HEAD):]), []
        elif line.startswith(END) and cur is not None:
            out.append((cur, body)); cur = None
        elif cur is not None:
            body.append(line)
    return out
def read_norm(path):
    d, cur = {}, None
    for raw in open(path, encoding='utf-8'):
        line = raw.rstrip('\n')
        if line.startswith('=== '):
            cur = line[4:]; d.setdefault(cur, [])
        elif cur is not None:
            d[cur].append(line)
    return d
mode = sys.argv[1]
if mode == 'normalize':
    secs = sections(sys.argv[2])
    seen = {}
    with open(sys.argv[3], 'w', encoding='utf-8') as f:
        for k, body in sorted(secs, key=lambda s: s[0]):
            n = seen[k] = seen.get(k, 0) + 1
            f.write(f'=== {k}' + (f' #{n}' if n > 1 else '') + '\n' + ''.join(b + '\n' for b in body))
    print(f'sections={len(secs)} keys={len(seen)}')
    sys.exit(0 if secs else 1)
if mode == 'compare':
    a, b = read_norm(sys.argv[2]), read_norm(sys.argv[3])
    bad = 0
    for k in sorted(set(a) | set(b)):
        if k not in b: print('REMOVED', k); bad += 1
        elif k not in a: print('ADDED', k); bad += 1
        elif a[k] == b[k]: print('EQUAL', k)
        else:
            add = sum(1 for x in b[k] if x not in a[k]); rem = sum(1 for x in a[k] if x not in b[k])
            print(f'CHANGED +{add} -{rem}', k); bad += 1
    print('ALL EQUAL' if not bad else f'DIFFERENT {bad}')
    sys.exit(1 if bad else 0)
if mode == 'rules':
    d = read_norm(sys.argv[2]); text = '\n'.join(l for v in d.values() for l in v)
    def has(s): return s in text
    checks = [
        ('a|b worker names or constructors', has('-keepnames @androidx.hilt.work.HiltWorker class * extends androidx.work.ListenableWorker')
             or ('-keep public class * extends androidx.work.ListenableWorker {' in text and 'public <init>(...);' in text)),
        ('a -keepnames @HiltWorker', has('-keepnames @androidx.hilt.work.HiltWorker class * extends androidx.work.ListenableWorker')),
        ('b -keep ListenableWorker constructors', has('-keep public class * extends androidx.work.ListenableWorker {') and has('public <init>(...);')),
        ('c -keep WorkerParameters', has('-keep class androidx.work.WorkerParameters')),
        ('d Tink protobuf fields', has('-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {') and has('<fields>;')),
        ('e SystemForegroundService <init>', has('-keep class androidx.work.impl.foreground.SystemForegroundService { <init>(); }')),
        ('f Hilt @EntryPoint', has('-keep,allowobfuscation,allowshrinking @dagger.hilt.EntryPoint class *')),
    ]
    agp = next((v for k, v in d.items() if k.startswith('agp-default:proguard-android-optimize.txt')), [])
    agptext = '\n'.join(agp)
    checks.append(('AGP default -keepattributes has Signature and RuntimeVisibleAnnotations',
                   '-keepattributes' in agptext and 'Signature' in agptext and 'RuntimeVisibleAnnotations' in agptext))
    strip = lambda ls: [x for x in '\n'.join(ls).rstrip('\n').split('\n')]
    app = strip(d.get('app:proguard-rules.pro', []))
    pro = strip([l.rstrip('\r') for l in open(sys.argv[3], encoding='utf-8').read().split('\n')])
    checks.append(('g app:proguard-rules.pro section equals the committed file', app == pro))
    checks.append(('g -dontoptimize present', '-dontoptimize' in app))
    for name, ok in checks: print(('OK   ' if ok else 'FAIL ') + name)
    sys.exit(0 if all(ok for name, ok in checks if not name.startswith(('a -', 'b -'))) else 1)
if mode == 'closure':
    names = set(re.findall(r':(?:name|appComponentFactory)\(0x[0-9a-f]+\)="([A-Za-z0-9_.$]+)"', open(sys.argv[2], encoding='utf-8').read()))
    mapped = {}
    for line in open(sys.argv[3], encoding='utf-8', errors='replace'):
        m = re.match(r'^([A-Za-z0-9_.$]+) -> ([A-Za-z0-9_.$]+):$', line.rstrip('\r\n'))
        if m: mapped[m.group(1)] = m.group(2)
    bad = 0
    for n in sorted(names):
        if n in mapped:
            ok = mapped[n] == n; bad += not ok
            print(('OK   ' if ok else 'FAIL ') + f'{n} -> {mapped[n]}')
        elif re.match(r'^[a-z][\w]*(\.[\w$]+)+$', n) and re.match(r'^[A-Z]\w*[a-z]', n[n.rfind('.') + 1:]):
            print(f'NOT-IN-MAPPING {n}')
    print(f'manifest-classes-in-mapping={sum(1 for n in names if n in mapped)} not-identity={bad}')
    sys.exit(1 if bad else 0)
if mode == 'seeds-diff':
    HILT = ['Hilt_*', '*_Factory', '*_HiltModules*', '*_HiltModule', '*_AssistedFactory*', '*_MembersInjector',
            '*_GeneratedInjector', '*_HiltComponents*', '*_ComponentTreeDeps', 'Dagger*']
    HILT_PKG = ['hilt_aggregated_deps.*', 'dagger.hilt.internal.aggregatedroot.codegen.*']
    KOTLIN = [r'\$default\(', r'access\$', r'\bCompanion\b', r'\$WhenMappings', r'\$EntriesMappings', r'\$\$delegatedProperties']
    WORKER = re.compile(r'^com\.zegoggles\.smssync\.(service\.BackupWorker|service\.RestoreWorker|worker\.BackupTriggerWorker): '
                        r'(BackupWorker|RestoreWorker|BackupTriggerWorker)\(')
    old = open(sys.argv[2], encoding='utf-8').read().splitlines()
    new = open(sys.argv[3], encoding='utf-8').read().splitlines()
    inc = sys.argv[4]
    prev_ksp = [a.split('=', 1)[1] for a in sys.argv[5:] if a.startswith('--prev-ksp=')]
    roots = [a for a in sys.argv[5:] if not a.startswith('--prev-ksp=')]
    prev = [l.strip() for p in prev_ksp for l in open(p, encoding='utf-8')]
    LIBRARY = ('androidx.work.', 'androidx.core.app.', 'androidx.profileinstaller.', 'androidx.room.')
    from collections import Counter
    co, cn = Counter(old), Counter(new)
    changed = [('-', l) for l in sorted((co - cn).elements())] + [('+', l) for l in sorted((cn - co).elements())]
    bad = 0
    for sign, line in changed:
        cls, _, member = line.partition(': ')
        simple = cls[cls.rfind('.') + 1:]
        top = simple.split('$')[0]
        if cls.startswith('com.google.crypto.tink.') or WORKER.match(line):
            verdict = 'FORBIDDEN'
        elif cls.startswith(LIBRARY) or (inc == 'I5' and not cls.startswith('com.zegoggles.smssync.')):
            verdict = 'NO MATCH'
        elif inc in ('I4', 'I6'):
            pat = next((p for p in HILT if fnmatch.fnmatchcase(simple, p) or fnmatch.fnmatchcase(top, p)), None) \
                or next((p for p in HILT_PKG if fnmatch.fnmatchcase(cls, p)), None)
            verdict = f'MATCH {pat}' if pat else 'NO MATCH'
            if pat and cls.startswith('com.zegoggles.smssync.'):
                rel = cls.split('$')[0].replace('.', '/') + '.java'
                found = any(os.path.isfile(os.path.join(r, rel)) for r in roots)
                if not found and sign == '-' and any(l.endswith('/java/' + rel) for l in prev):
                    found, verdict = True, f'MATCH {pat} (generated at the previous increment)'
                if not found:
                    verdict = f'NOT-GENERATED {pat}'
        elif inc == 'I5':
            text = member or simple
            pat = next((p for p in KOTLIN if re.search(p, text)), None)
            verdict = f'MATCH {pat}' if pat else 'NO MATCH'
        else:
            verdict = 'NO MATCH'
        bad += not verdict.startswith('MATCH')
        print(f'{verdict} {sign} {line}')
    print(f'changed={len(changed)} not-accepted={bad}')
    sys.exit(1 if bad else 0)
if mode == 'core':
    blocks = []
    for raw in open(sys.argv[2], encoding='utf-8', errors='replace'):
        line = raw.rstrip('\r\n')
        if line and not line[0].isspace():
            blocks.append([line])
        elif blocks and line:
            blocks[-1].append(line)
    keep = sorted([b[0]] + sorted(b[1:]) for b in blocks if b[0].startswith(PREFIXES))
    out = [l for b in keep for l in b]
    open(sys.argv[3], 'w', encoding='utf-8').write(''.join(l + '\n' for l in out))
    print(f'core-entries={len(keep)} core-lines={len(out)}')
    sys.exit(0 if out else 1)
if mode == 'services':
    orig = {}
    for line in open(sys.argv[3], encoding='utf-8', errors='replace'):
        m = re.match(r'^([A-Za-z0-9_.$]+) -> ([A-Za-z0-9_.$]+):$', line.rstrip('\r\n'))
        if m: orig[m.group(2)] = m.group(1)
    tr = lambda n: orig.get(n, n + ' (unmapped)')
    entries, pairs = [], []
    with zipfile.ZipFile(sys.argv[2]) as z:
        for name in sorted(z.namelist()):
            if not name.startswith('META-INF/services/') or name.endswith('/'):
                continue
            iface = name[len('META-INF/services/'):]
            impls = [l.split('#')[0].strip() for l in z.read(name).decode('utf-8', 'replace').splitlines()]
            impls = sorted(tr(i) for i in impls if i)
            entries.append(f'ENTRY {iface} = {tr(iface)}')
            pairs.append(f'PAIR {tr(iface)} -> ' + ', '.join(impls))
    open(sys.argv[4], 'w', encoding='utf-8').write(''.join(l + '\n' for l in sorted(pairs) + entries))
    print(f'services={len(entries)} unmapped={sum(l.count("(unmapped)") for l in pairs)}')
    sys.exit(0 if entries else 1)
if mode == 'section':
    d = read_norm(sys.argv[2])
    if sys.argv[3] not in d:
        sys.exit(f'no section {sys.argv[3]}')
    body = '\n'.join(d[sys.argv[3]]).strip('\n')
    open(sys.argv[4], 'w', encoding='utf-8', newline='\n').write(body + '\n')   # LF on every OS (Amendment A2, B-8)
    print(f'section {sys.argv[3]} lines={body.count(chr(10)) + 1}')
    sys.exit(0)
sys.exit('unknown mode')
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

**Gradle calls.** Foreground, tool timeout 600000 ms, output to the work folder, exit appended, run by 4.L. Never two
at once. `NC='--no-build-cache --no-configuration-cache'` (depth check; both caches are off at project level, L23, and
these flags keep a user-wide setting from changing that).
- **G(k)** gate: `rm -rf $R; ./gradlew --console=plain $NC --warning-mode all clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification :app:assembleRelease > $WORK/logs/I<k>-gates.log 2>&1; echo "exit=$?" >> $WORK/logs/I<k>-gates.log`
  (the four gates plus `clean` and the report task that writes the XML; `--warning-mode all` is AC-18 (c)'s record).
- **WP(k)** writer pass, the design's exact command (D5 step 3; no added flag, row D94): `./gradlew --write-verification-metadata sha256 --continue clean help test lint jacocoTestReport assembleRelease :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification > $WORK/logs/I<k>-writer.log 2>&1; echo "exit=$?" >> $WORK/logs/I<k>-writer.log`
- **ENF(k)** enforced re-run: `./gradlew --console=plain $NC :app:assembleDebug > $WORK/logs/I<k>-enforced.log 2>&1; echo "exit=$?" >> …`
- **UTD(k)** up-to-date check (I2, I5; depth check): right after EV-B(k), `./gradlew --console=plain $NC :app:assembleDebug > $WORK/logs/I<k>-uptodate.log 2>&1; echo "exit=$?" >> …` (no `clean`), sanitised with `'^> Task :app:(kspDebugKotlin|compileDebugKotlin|compileDebugJavaWithJavac|hiltJavaCompileDebug)|^BUILD |exit='` to `$E/I<k>-uptodate.txt`. Expected `exit=0` and those four tasks `UP-TO-DATE`; a task that re-runs is recorded as debt (KSP2 recompilation on Windows), not a stop
- **KC(k)** forced full Kotlin compile (U-067 form, no init script): `./gradlew --console=plain $NC :app:compileDebugKotlin --rerun > $WORK/logs/I<k>-kotlinc.log 2>&1; echo "exit=$?" >> …`
- **TR(name, fqcn…)** targeted: `rm -rf $R; ./gradlew --console=plain $NC :app:testDebugUnitTest --tests "<fqcn>" [--tests …] > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> …`
- **L(k)** lint: `./gradlew --console=plain $NC :app:lintDebug > $WORK/logs/I<k>-lint.log 2>&1; echo "exit=$?" >> …` (expected `exit=1`, pre-existing, D17)
- **DEP(k)** (two runs, as U-060; plan-review O-3): `./gradlew --console=plain $NC :app:dependencies --configuration releaseRuntimeClasspath > $WORK/logs/I<k>-deps-release.log 2>&1; echo "exit=$?" >> …` and the same with `--configuration debugUnitTestRuntimeClasspath` into `$WORK/logs/I<k>-deps-test.log`; `deps_resolved.py` then reads `$WORK/logs/I<k>-deps-release.log` for `releaseRuntimeClasspath` and `…-deps-test.log` for `debugUnitTestRuntimeClasspath` (row D97)
- **VER(name)**: `./gradlew --version > $WORK/logs/<name>.log 2>&1; echo "exit=$?" >> …`

### 4.L Long runs and the 600000 ms tool limit (every Gradle call)

WP takes about 12 to 16 minutes and G about 9 to 11 (more on the first run of a new Gradle version, whose caches are
cold), above the tool's 10-minute foreground limit. On this machine the harness then moves the run to the background
and notifies when it ends (every long Gradle run of U-066 to U-069 went that way). The procedure:
1. Start the command in the foreground with the 600000 ms timeout. The command itself writes the log and appends
   `exit=$?`, so the result never depends on the tool's own output.
2. If the harness reports that the run continues in the background: wait for its completion notice; start no other
   command meanwhile (no Gradle, no git, no edit); never poll the log or the process list.
3. After the notice (or the foreground end): read the last line of the log (`tail -n 1 $WORK/logs/<name>.log`) — it
   must be `exit=<n>` — and proceed by that value as the step says.
4. **A cut-off run**: the log has no `exit=` line, or the harness reports that the command was terminated rather than
   moved to the background. Row D93: record it, check that no Gradle JVM is left (`"$JDKBIN/jps.exe" -l | grep -iE
   'gradle|KotlinCompileDaemon'`), stop and report. The run is not repeated in the same session. In a later session
   4.R allows one re-run of that step from its start (a writer pass from VMP step 1); a second cut-off of the same step
   stops the story for the orchestrator.

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

**EV-B(k)** (U-068 recipe plus the KSP list, the R8 files of 2.10 and the packaged backup descriptor):
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
grep -oE '^([A-Za-z0-9_.$]+) -> \1:' $MAP/mapping.txt | sed 's/ -> .*//' | LC_ALL=C sort > $E/I<k>-kept-names-all.txt
python -I -X utf8 $TL/r8_check.py normalize $E/I<k>-configuration.txt $E/I<k>-configuration-norm.txt
python -I -X utf8 $TL/r8_check.py core $MAP/seeds.txt $E/I<k>-seeds-core.txt
python -I -X utf8 $TL/r8_check.py core $MAP/usage.txt $E/I<k>-usage-core.txt
{ ls $MAP
  python -I -X utf8 $TL/r8_check.py rules $E/I<k>-configuration-norm.txt app/proguard-rules.pro; echo "rules-exit=$?"
  python -I -X utf8 $TL/r8_check.py closure $E/I<k>-manifest-release.txt $MAP/mapping.txt; echo "closure-exit=$?"
  grep -E '^# (compiler|compiler_version|min_api):' $MAP/mapping.txt
  RA=app/build/outputs/apk/release/app-release-unsigned.apk
  for n in service.BackupWorker service.RestoreWorker worker.BackupTriggerWorker; do
    echo "dex-string com.zegoggles.smssync.$n=$(unzip -p $RA 'classes*.dex' | grep -a -o "com\.zegoggles\.smssync\.${n//./\\.}" | wc -l)"; done
  unzip -p $RA classes.dex > $WORK/I<k>-classes.dex; "$SDK/build-tools/36.0.0/dexdump" -f $WORK/I<k>-classes.dex | grep -E '^(magic|class_defs_size|method_ids_size|field_ids_size)'
  for p in assets/dexopt/baseline.prof assets/dexopt/baseline.profm; do echo "$p sha256=$(unzip -p $RA $p | sha256sum | cut -d' ' -f1)"; done
} > $E/I<k>-r8.txt
python -I -X utf8 $TL/r8_check.py services app/build/outputs/apk/release/app-release-unsigned.apk $MAP/mapping.txt $E/I<k>-services.txt   # Amendment A1 (B-7, contract O-13)
{ for a in debug/app-debug release/app-release-unsigned; do f=app/build/outputs/apk/$a.apk
    p=$("$AAPT2" dump resources $f | grep -A1 'xml/backup_descriptor$' | grep -o '(file) [^ ]*' | cut -d' ' -f2)
    echo "== $a (descriptor entry ${p##*/})"; "$AAPT2" dump xmltree --file "$p" $f | sed 's/^ *//'; done
} > $E/I<k>-backup-descriptor.txt
```
Expected: `so=0` for both APKs; nine `self-mapped …=1`; `major version: 61`, `61`, `52`; in `I<k>-r8.txt` the folder
lists `configuration.txt`, `mapping.txt`, `seeds.txt`, `usage.txt` (row D98 if `seeds.txt` or `usage.txt` is missing),
every `rules` line `OK` with `rules-exit=0`, `not-identity=0` with `closure-exit=0` (each `NOT-IN-MAPPING` line
recorded), `# min_api: 24`, the record-only dex, `dexdump` and baseline-profile lines (2.10); in `I<k>-backup-descriptor.txt` per
APK two `E: exclude`, two `domain="sharedpref"`, one `path="credentials.xml"`, one `path="pinned_certs.xml"` (L27). If
AGP 9.5.2 moves a class directory (row D55), the javap path follows and the log says so.

**CMP(k)** (k ≥ 1, one call, into `$E/I<k>-compare.txt`; `norm` is U-061's packaged-manifest normaliser):
```bash
norm() { sed -E 's/ ?\(line=[0-9]+\)//g; s/@0x7f[0-9a-f]{6}/@0x7fRESID/g' "$1"; }
{ for f in merged-debug.xml merged-release.xml kept-names.txt kept-names-all.txt seeds-core.txt usage-core.txt apk-entries-debug.txt apk-entries-release.txt javap.txt ksp-files.txt cases.txt jacoco-inventory.json jacoco-classes.txt; do
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
  echo "seeds-diff vs <p>:"; python -I -X utf8 $TL/r8_check.py seeds-diff $E/<p>-seeds-core.txt $E/I<k>-seeds-core.txt I<k> --prev-ksp=$E/<p>-ksp-files.txt app/build/generated/ksp/release/java app/build/generated/hilt/component_sources/release; echo "seeds-diff-exit=$?"
  for b in I0 <p> I<k>; do grep '^PAIR ' $E/$b-services.txt > $WORK/svc-$b.txt; done
  for b in I0 <p>; do git -c core.autocrlf=false diff --no-index --quiet -- $WORK/svc-$b.txt $WORK/svc-I<k>.txt; echo "services-pairs vs $b exit=$?"; done
  grep '^ENTRY ' $E/I<k>-services.txt
  for b in <p> I<k>; do echo "$b $(grep -E '^manifest-classes-in-mapping=' $E/$b-r8.txt) $(grep -E '^# min_api:' $E/$b-r8.txt)"; done
  for b in <p> I0; do echo "configuration-norm vs $b:"; python -I -X utf8 $TL/r8_check.py compare $E/$b-configuration-norm.txt $E/I<k>-configuration-norm.txt | grep -v '^EQUAL'; done
  F=$E/I<k>-backup-descriptor.txt; echo "backup exclude=$(grep -c 'E: exclude' $F) sharedpref=$(grep -c 'domain="sharedpref"' $F) credentials=$(grep -c 'path="credentials.xml"' $F) pinned=$(grep -c 'path="pinned_certs.xml"' $F)"
} > $E/I<k>-compare.txt
```
Expected (2.9, 2.10): every `exit=0` except the cells 2.9 allows; **the comparison against `<p>` decides; the one
against I0 is recorded** and may differ only by differences already accepted at earlier increments (each listed in the
log with its increment); the invariant line `fgs=1 init=0 bbr-exported=1 bbr-action=1 minsdk=1 merged-minsdk=1` for both
APKs; `so=0` twice; `9` self-mapped; `configuration-norm vs <p>:` followed by `ALL EQUAL`, or only by the `CHANGED`
section R8-5 allows at this increment; `seeds-diff` with `changed=0` and `seeds-diff-exit=0`, or at I4, I5, I6 every
changed line `MATCH` (ruling 58: each copied into the log with its pattern, D42); `manifest-classes-in-mapping` of
I<k> not below `<p>`'s and `# min_api: 24` on both lines; `services-pairs vs I0 exit=0` and `services-pairs vs <p> exit=0`
at every increment, I3 included (the (interface, implementations) pairs in original names are equal; the `ENTRY` lines
record the APK names and may change at I3 only, D101; a pair difference is D44/D101, a stop); `backup exclude=4 sharedpref=4 credentials=2 pinned=2` (two
APKs; D100). For each
`exit=1` the developer saves the full `git diff --no-index` into the work folder and writes each differing line,
classified, into the log (sanitised). The `jacoco-inventory.json` and `jacoco-classes` comparisons may differ in
`activity` covered counts (the known alternation, 2.6) without further investigation; any other difference in them is
classified by 2.9. `usage-core` differences are classified (2.10 R8-3), the raw `configuration.txt` is not compared.

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
4. Before staging, the expected set (plan-review B-2): `git status --porcelain --untracked-files=all -- <the same list> | cut -c4- | LC_ALL=C sort > $WORK/logs/I<k>-expected.txt; printf '%s\n' <the same list> | LC_ALL=C sort > $WORK/logs/I<k>-list.txt; LC_ALL=C comm -23 $WORK/logs/I<k>-list.txt $WORK/logs/I<k>-expected.txt > $WORK/logs/I<k>-unchanged.txt`
   (verified read-only: the porcelain form with `--untracked-files=all` lists an explicitly named untracked file and
   omits an unchanged tracked one).
   Every line of `I<k>-unchanged.txt` is a listed file equal to HEAD (for example `gradlew` at I1, row D99); each is
   recorded in the log as "listed, unchanged, not staged", and the list's other files are the expected staged set.
   Then `git add -- <the same list>` (every path written out or brace-expanded by bash to explicit paths; never `-A`, `.`, `-u` or a bare directory; an unchanged tracked file stages nothing).
5. `git diff --cached --name-only | LC_ALL=C sort > $WORK/logs/I<k>-staged.txt; git -c core.autocrlf=false diff --no-index --quiet -- $WORK/logs/I<k>-expected.txt $WORK/logs/I<k>-staged.txt; echo "staged-equal-exit=$?"` → `0`. For I1 also `git diff --cached --summary -- gradlew gradlew.bat` → no `mode change` line.
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

A new session has a new scratch folder. First `mkdir -p "$WORK/logs" "$WORK/pristine"` (plan-review O-2). What is
lost with the old folder: raw logs, copies and commit messages of earlier sessions only; no later step reads them (CMP,
the I5 `w:` comparison, the I12 inventory, the DEP comparisons and the I14 end proof read `$E/` files or `git show`).
`$WORK/gradlew-start` and `$WORK/gradlew-block.txt` are needed only inside I1 and are recreated with
`git show $START:gradlew > $WORK/gradlew-start; git show $START:gradlew | sed -n 117,127p > $WORK/gradlew-block.txt`.
`LAST` is the last commit id the log names; when no `U-071 S2-` commit exists yet (a session that ended at I0),
`LAST` is `START`, and the second command below then prints nothing, as expected (plan-review O-17). Then one call, into `$WORK/logs/resume-<timestamp>.log` (not evidence):
```bash
git rev-parse HEAD; git log --format='%h %s' --grep='^U-071 S2-' $START..HEAD | head -1
git log --format='%h %s' $LAST..HEAD; git log --format=%h $LAST..HEAD -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat .github/workflows sdlc/artifacts/engagement/code-location.md $W/implementation-log.md $W/evidence; echo "foreign-on-paths-end"
git status --porcelain -- app k9mail-vendored build.gradle settings.gradle gradle gradle.properties gradlew gradlew.bat .github/workflows sdlc/artifacts/engagement/code-location.md; echo "porcelain-end"
git diff --quiet HEAD -- gradle/verification-metadata.xml; echo "vm-clean-exit=$?"
grep -c 'LOCAL ADDITION' gradlew
test -e $PROBE; echo "probe-present-exit=$?"
grep -c '^### M-.* APPLYING' $E/mutation-log.md 2>/dev/null; grep -c '^- restored:' $E/mutation-log.md 2>/dev/null
tail -n 1 $E/../implementation-log.md
"$JDKBIN/jps.exe" -l | grep -iE 'gradle|KotlinCompileDaemon'; echo "jps-end"
```
Expected at a clean hand-over (plan-review O-1): the newest `U-071 S2-` commit equals `LAST`; the commits after `LAST`
are recorded (orchestrator documents are normal and not a reason to stop) and nothing precedes `foreign-on-paths-end`;
nothing before `porcelain-end`; `vm-clean-exit=0`; `2`; `probe-present-exit=1`; the two mutation counts equal (or the
file absent); the log's last line names the next step; no Gradle JVM (a `KotlinCompileDaemon` line is recorded, not a
stop). Then VER(`resume-version`) shows the wrapper version the log expects and the Daemon JVM line. Any difference:
stop and report (row D90), except the documented recoveries:
- a present probe is deleted (4.P);
- an `APPLYING` mutation without `restored` is recovered by 4.M's recovery note;
- **a cut-off run**, found through the D93 entry in `implementation-log.md` (the raw log, which had no `exit=` line,
  went with the old scratch folder; 4.L): a writer pass is never continued — VMP step 1 restores the
  file and the pass runs once more from the start; a gate or any other step runs once more from its start; a second
  cut-off of the same step is D93 (stop for the orchestrator);
- an interrupted wrapper step restores the wrapper files from HEAD — `git restore --source=HEAD --worktree -- gradle/wrapper gradlew.bat`
  and `git -c core.autocrlf=false restore --source=HEAD --worktree -- gradlew` (LF, as the generator writes it; plan-review
  O-6) — recreates the two `$WORK` copies above and restarts I1 at step 2.

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
   git log --format='%h %s' --grep='^U-068 S1-' --grep='^U-069' --grep='^U-070' --grep='^U-074' --grep="^$B29_ID" $U068_START..HEAD
   grep -n '^status' "$B29_STORY" sdlc/artifacts/stories/BUG-029-*.md
   git diff --stat $PLAN_HEAD HEAD -- app/src/main; echo "main-drift-end"
   git diff --stat $U068_START HEAD -- gradle/verification-metadata.xml; echo "vm-since-u068-start-end"
   git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"
   git grep -nE '^\s*@Ignore(\s|\(|$)' HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt'
   grep -v '^#' gradle/gradle-daemon-jvm.properties
   "$JDKBIN/jps.exe" -l | grep -iE 'gradle|KotlinCompileDaemon'; echo "jps-end"
   test -s $SUPP; echo "supplement-exit=$?"
   for f in gradle.properties "$HOME/.gradle/gradle.properties"; do c=0; [ -f "$f" ] && c=$(grep -cE '^[[:space:]]*org\.gradle\.(caching|configuration-cache)[[:space:]]*=[[:space:]]*true' "$f"); echo "caches-on=$c"; done
   echo "gradle-opts-caches=$(printf '%s' "$GRADLE_OPTS $JAVA_OPTS" | grep -cE 'org\.gradle\.(caching|configuration-cache)=true')"
   ```
   `B29_ID=U-075` and `B29_STORY=sdlc/artifacts/build/sprints/sprint-024/U-075-automatic-backup-runs-when-the-app-is-off-screen.md`
   (the BUG-029 fix story; plan-review O-23). In the status lines `U-075`'s story file must read `status: done`;
   the bug file `BUG-029-*.md` may still read `ready` (a bug closes after its fix story) and is recorded, not a
   condition (contract-verification note on this line).
   Expected: `plan-head-ancestor-exit=0`; the commits after PLAN_HEAD are U-068 I7, U-069's, U-070's, U-074's, the
   U-075's (the BUG-029 fix) and orchestrator documents; before `main-drift-end` the files those stories changed (expected:
   U-069's `worker/BackupTriggerWorker.kt` only (its commit `d7a69974`, plan-review O-24), U-070's `service`/`preferences`/`mail` files, U-074's
   worker files and U-075's `BackupWorker.kt`, `RestoreWorker.kt` and new `service/WorkerForeground.kt`), recorded with each file's story (contract-verification O-12; a file no listed story
   names is D5); before `build-drift-end` exactly `2	2	app/build.gradle`; app hunks `@@ -579 +579 @@` and
   `@@ -586 +586 @@`; code-location hunk `@@ -28 +28 @@` (U-068 E7.3); nothing before `porcelain-end`; each line's two
   hashes equal; `100755` for `gradlew`; every story `status: done`, both CRs `status: implemented`;
   `u067-fix-ancestor-exit=0`; nothing before `vm-since-u068-start-end` (SC-2); the count `C0` as printed; the `@Ignore`
   lines (2 expected: `AppTest.java:13`, `AuthPreferencesTest.java:1079`); `toolchainVersion=25`; no Gradle JVM (a
   `KotlinCompileDaemon` line is recorded by class name only, not a stop); `supplement-exit=0`; `caches-on=0` twice and
   `gradle-opts-caches=0` (the file names are not printed; a missing user-wide file prints `0`; row D94).
   Record `START=$(git rev-parse HEAD)`, `C0`, the skipped baseline (from step 6) and the
   commit ids of U-067 (`c12a2788`), U-068, U-069, U-070 and U-074 with the tests each added (from their logs) at the
   top of the log (SC-1, SC-9). **Rows:** D1 to D8.
2. `mkdir -p "$WORK/logs" "$WORK/pristine" "$M" "$TL"`; write the nine tools of 4.0 with the Write tool into `$TL/`;
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
   the skipped count recorded as "the start" (2 expected); `cov-floors-exit=0` (with `worker` at `floor=0.90`, L17);
   the I0 coverage table of 2.6 filled from `I0-coverage.txt`; in `I0-r8.txt` `rules-exit=0` and `closure-exit=0`, and
   the folder listing (row D98); in `I0-backup-descriptor.txt` the L27 counts. Then, before any other Gradle run (the
   next `rm -rf $R` deletes the XML), the **D7 baseline** (plan-review O-7) into `$E/I0-d7.txt` with I7 step 2's loop
   run on `$WORK/logs/I0-gates.log` and `$R` (expected all `0`; a non-zero count is the "before" that I7 compares with),
   and the **SC-2 merged-manifest check** (plan-review O-8): `grep -c 'android:minSdkVersion="24"' $E/I0-merged-debug.xml`
   → `1`, appended to `$E/I0-greps.txt`. **Rows:** D9, D3.
7. **L(0)**; sanitise with **LF** into `$E/I0-lint.txt`; `python -I -X utf8 $T68/lint_ids.py app/build/reports/lint-results-debug.xml > $WORK/logs/I0-lint-ids.log`,
   sanitised to `$E/I0-lint-ids.txt`; `grep -c '^ *<issue *$' app/lint-baseline.xml` and the "listed in the baseline
   file but not found" count into `$E/I0-lint-baseline-count.txt`. Expected `exit=1`, the rule-ID table (U-068 I6's
   33 errors expected, re-measured: AC-1 (h)'s "before" figure). **Row:** D10.
8. **DEP(0)** (two runs); then `python -I -X utf8 $TL/deps_resolved.py $WORK/logs/I0-deps-release.log releaseRuntimeClasspath $WORK/I0-rel.txt $WORK/I0-rel-tree.txt`
   and `python -I -X utf8 $TL/deps_resolved.py $WORK/logs/I0-deps-test.log debugUnitTestRuntimeClasspath $WORK/I0-test.txt $WORK/I0-test-tree.txt`; sanitise `$WORK/I0-rel.txt` →
   `$E/I0-deps-releaseRuntime.txt`, `$WORK/I0-rel-tree.txt` → `$E/I0-deps-releaseRuntime-tree.txt`, `$WORK/I0-test.txt`
   → `$E/I0-deps-debugUnitTestRuntime.txt`. Expected `MULTI 0` for both (AC-1 (k)).
9. `ls "$HOME/.m2/repository/org/robolectric/android-all-instrumented" > $WORK/logs/I0-android-all.log 2>&1`, sanitised
   to `$E/I0-android-all.txt` (folder names only; the "before" of AC-12 (f)).
10. **Probe (before)** by 4.P with k = 0 → `$E/I0-probe.txt`. **Row:** D12.
11. **Records.** Create `$W/implementation-log.md`: header; SC-1 to SC-9 each with check and result; `<start>`, `C0`,
    the `@Ignore` lines, the skipped baseline; the **D1a table** (2.1, rows checked against `I0-oldtext.txt` and
    `$SUPP`: the "at `<start>`" column must equal the build files); the I0 results by reference to the evidence
    files. Create `$E/compatibility-record.md` from 2.1 (the D1a resolution of (a) and (b)) and 2.2, with the
    supplement's items in their labelled form. **Rows:** D13, D14.
12. `git status --porcelain` lists only `?? $W/implementation-log.md` and `?? $E/` (AC-1, last sentence) besides the
    checkout's other untracked orchestrator files, which are left alone. I0 makes no commit; its evidence goes into the
    I1 commit.
13. **Later cases of the NEW3 classes** (plan-review O-5): from `I0-cases.txt` list every case of
    `BackupWorkerLoopCancellationTest`, `RestoreWorkerIntegrationTest` and `RestoreWorkerCheckpointTest` that is not one
    of U-067's 31 (names in U-067's mutation log and plan test table); for each, record in the log the loop it drives —
    `backupCursors` (site 1), `runImapRestoreLoop` via `doWork` (site 2) or `executeRestoreWithValues` (site 3), or none
    — read from U-070's, U-074's and the BUG-029 fix story's implementation logs and from the fixture it calls (`cancellableImapRun` → site 2,
    `CancellingInterceptor` on `executeRestoreWithValues` → site 3, the backup-loop fixture → site 1), never from its
    name alone. I5 copies this table into `mutation-log.md` before M-1a; D64 applies to it.

### I1 — Gradle wrapper 9.9.2 (AC-6; AC-17 by Scope Concern 3)

1. Pre-check: `git status --porcelain -- gradle gradlew gradlew.bat build.gradle app/build.gradle` empty; no Gradle JVM.
   Set `Q1=971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b` and
   `Q2=621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8` (supplement Q-1, Q-2; fetched 2026-11-21 from
   `https://downloads.gradle.org/distributions/gradle-9.9.2-bin.zip.sha256` and `…/gradle-9.9.2-wrapper.jar.sha256`,
   the address `services.gradle.org/distributions/…` redirecting there; both equal to `https://gradle.org/release-checksums/`).
   The log cites these URLs and the redirect note (plan-review O-10). Shell state does not persist, so every later call
   of I1 that uses `$Q1` or `$Q2` sets them first (same two assignments). The download line of step 4 may name either
   host.
2. **W1** (on 9.5.1): `./gradlew --console=plain wrapper --gradle-version 9.9.2 --distribution-type bin --gradle-distribution-sha256-sum $Q1 > $WORK/logs/I1-wrapper-run1.log 2>&1; echo "exit=$?" >> $WORK/logs/I1-wrapper-run1.log`
   → `exit=0`; sanitised to `$E/I1-wrapper-run1.txt`. **Row:** D15.
3. At once, before any other Gradle call: `grep -c 'LOCAL ADDITION' gradlew` → `0` (the block is lost, Risk 8);
   `cp gradlew $WORK/gradlew-gen1`; apply **E1** (3.1); `python -I -X utf8 $TL/gradlew_block.py $WORK/gradlew-start $WORK/gradlew-gen1 gradlew`
   → all `OK` and the block SHA `45364f3d…ea760`; `git hash-object gradlew` (expected equal to `git rev-parse HEAD:gradlew`,
   because run 1 used 9.5.1's template; a difference is recorded, not a stop, as run 2 regenerates). **Rows:** D16, D17.
4. **VER(`I1-gradle-version-run1`)**: the first run on the new distribution downloads `gradle-9.9.2-bin.zip` through
   the wrapper's own path and verifies it against `distributionSha256Sum`. Expected `exit=0`, "Gradle 9.9.2", the
   Daemon JVM line, no "Unable to establish loopback connection", a `Downloading https://services.gradle.org/distributions/gradle-9.9.2-bin.zip`
   line (4.L applies: the download can take minutes). Then `Z=$(ls "$HOME"/.gradle/wrapper/dists/gradle-9.9.2-bin/*/gradle-9.9.2-bin.zip | head -1); echo "dist-sha-match=$([ "$(sha256sum "$Z" | cut -d' ' -f1)" = "$Q1" ] && echo yes || echo no)"`
   → `yes` (the log records that the downloaded file equals the pinned checksum; the path is not printed). Sanitised to
   `$E/I1-gradle-version-run1.txt`. **Rows:** D18 to D20.
5. **W2** (on 9.9.2): the same command into `$WORK/logs/I1-wrapper-run2.log` → `exit=0`; `$E/I1-wrapper-run2.txt`. **Row:** D21.
6. At once: `grep -c 'LOCAL ADDITION' gradlew` → `0`; `cp gradlew $WORK/gradlew-gen2`; apply **E1** again;
   `gradlew_block.py $WORK/gradlew-start $WORK/gradlew-gen2 gradlew` → all `OK`;
   `git -c core.autocrlf=false diff --no-index --numstat -- $WORK/gradlew-gen2 gradlew | cut -f1,2` → `12	0`;
   `git diff --quiet HEAD -- gradlew; echo "gradlew-unchanged-exit=$?"` → `0` expected (plan-review B-2: the Unix
   start-script template `unixStartScript.txt` is byte-identical in the cached 9.5.1 and 9.9.1 distributions, so after
   E1 the file equals `<start>`'s; the log then says "the 9.9.2 generator writes the same `gradlew` as 9.5.1; block
   present, file equal to `<start>`", and the block proof is AC-6 (b)'s evidence); `1` is also acceptable (template
   changed; the block proof decides). Record `sha256sum gradlew.bat` now (the generator's output; never opened
   afterwards; it is expected to change, the Windows template differs). **Rows:** D17, D99.
7. **VER(`I1-gradle-version`)** at once, before any other step (AC-6 (f)): "Gradle 9.9.2", the Daemon JVM line citing
   `gradle/gradle-daemon-jvm.properties`, no loopback error. Sanitised (whole file) to `$E/I1-gradle-version.txt`
   (plan-review B-1). **Row:** D19.
8. **Wrapper checks** (one call into `$E/I1-wrapper.txt`):
   ```bash
   sha256sum gradle/wrapper/gradle-wrapper.jar | cut -d' ' -f1; echo "official=$Q2"
   git diff -U0 HEAD -- gradle/wrapper/gradle-wrapper.properties | grep -E '^(@@|[-+][^-+])'
   grep -c "^distributionSha256Sum=$Q1\$" gradle/wrapper/gradle-wrapper.properties; grep -c '^validateDistributionUrl=true$' gradle/wrapper/gradle-wrapper.properties
   git diff --numstat HEAD -- gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar
   sha256sum gradlew.bat
   python -I -X utf8 $TL/gradlew_block.py $WORK/gradlew-start $WORK/gradlew-gen2 gradlew
   git diff --stat HEAD -- gradle/gradle-daemon-jvm.properties; echo "daemon-criteria-end"
   git grep -nE "^[[:space:]]*org\.gradle\.java\.home[[:space:]]*=" -- '*.properties' '*.gradle' gradlew gradlew.bat .github; echo "java-home-set-exit=$?"
   ```
   Expected: the jar SHA equals Q-2; the properties hunk `@@ -3,2 +3,2 @@` with the old and new `:3` and `:4` lines
   (`gradle-9.9.2-bin.zip`) and nothing else if the 9.9.2 generator keeps L6's keys and order (not derivable here
   without running it), otherwise each further generated line listed (row D23); `1` and `1`; the `gradlew` numstat
   empty if the 9.9.2 template is unchanged (step 6), otherwise template changes only (the block proof is the arbiter); the
   `gradlew.bat` SHA equal to step 6's; all `OK`; nothing before `daemon-criteria-end`; `java-home-set-exit=1`.
   **Rows:** D22 to D24.
9. **G(1)**, EV-T(1), EV-B(1), CMP(1), CK(1). AGP is still 9.0.1 (C2: Gradle 9.9 tests AGP 9.0). Expected `exit=0` and
   every CMP cell `=` (2.9). If the gate fails for a missing verification entry, VMP(1) runs (Scope Concern 3) and then
   G(1) again. **Rows:** D25, D26. **Must hold (S2-I1, AC-6):** gates green on Gradle 9.9.2 with AGP 9.0.1; the block
   present and proven; the jar official; the daemon criteria unchanged.
10. Log entry; CM(1).

### I2 — KSP 2.3.14 on Kotlin 2.3.21 (AC-7; G3)

1. CK pre-check: the build files equal HEAD. Apply **E2**. VMP(2), G(2), EV-T(2), EV-B(2), UTD(2), CMP(2), CK(2).
2. Expected: `build.gradle` numstat `1	1`; Kotlin lines unchanged; `ksp-files.txt vs I0 exit=0` (G3); every other CMP cell
   `=`; `I2-uptodate.txt` with the four tasks `UP-TO-DATE` (a re-run task is debt, recorded).
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
5. Every I3 difference in CMP (APK entries, `t` per package, the `agp-default` R8 section, lint rule IDs) is classified
   in the log against Q-13 to Q-17's behaviour-change and default-value entries, entry by entry; a difference no entry
   explains stops (D44). The defaults the build sets explicitly cannot move (2.8).
   **Rows:** D51, D55 to D57, D27 to D53 (F1 by D35), D42, D43. **Must hold:** gates green; class roots non-empty after
   `clean`; manifests equal `<p>`; R8-1 to R8-4 hold and only the `agp-default` section changed (2.10, ruling SC-P1);
   `receiver` above 0.50; every package at or above its floor.
6. Log entry; CM(3).

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
   **Rows:** D59, D60, D31, D27 to D53, D42, D43. **Must hold:** G4 (Hilt tasks, ASM transform, Hilt tests) green; R8-1
   to R8-4 hold and only `lib:hilt-android/proguard.txt` changed, if anything (2.10, ruling SC-P1); five identical
   versions; KSP differences explained.
5. Log entry; CM(4).

### I5 — Kotlin 2.4.21 (AC-10; F-1 stop rule; Risk 9; U-067's six mutations)

1. Apply **E5.1**, **E5.2** (and **E2** if I2 moved). VMP(5), G(5), EV-T(5), EV-B(5), UTD(5), CMP(5), CK(5).
   `I5-uptodate.txt` as at I2 (the first check if I2 moved).
2. **KC(5)** → `$E/I5-kotlinc.txt`, `$E/I5-kotlinc-summary.txt` (as I4). Compare the `w:` lines: `grep '^w: ' $E/I4-kotlinc.txt | sed 's#^w: file:///<repo>/##' | LC_ALL=C sort > $WORK/w4.txt`
   (likewise `w5.txt`), `git -c core.autocrlf=false diff --no-index -- $WORK/w4.txt $WORK/w5.txt` → every `+` line is
   a new warning, recorded as debt in the log (AC-10 (b)).
3. **F-1 stop rule** (AC-10 (c)) into CK: `git diff --stat HEAD -- app/src/test; echo "test-diff-end"` (nothing);
   `grep -E '^w: .*(BackupWorker|RestoreWorker)\.kt' $E/I5-kotlinc.txt | grep -c 'coroutineContext'` → `0` (the story's
   form), and the same with `(BackupWorker|RestoreWorker|WorkerForeground)\.kt` → `0` (U-075's helper file,
   contract-verification O-14); and in
   `I5-cases.txt` every case of the NEW3 classes `PASS`. **Row:** D61.
4. **DEP(5)** → `$E/I5-deps-releaseRuntime.txt`, `…-tree.txt`; compared with I4: expected changes only
   `org.jetbrains.kotlin:kotlin-stdlib*` (2.4.21) and Kotlin artifacts; S4-owned lines equal (row D31).
5. **Mutations** by 4.M (M-1a to M-3b) on this uncommitted tree, after copying I0 step 13's later-case table into the
   header of `mutation-log.md`. **Rows:** D62 to D64.
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
   Expected all `0`, and in any case each count compared with `I0-d7.txt` (plan-review O-7): only a count above I0's
   is a D7 hit. Zero new hits → apply **E7.2** (main path; numstat then `4 4` with E7.1). Any new hit → row D69 (E7.3,
   the exact flags the error names, then G(7) again with EV-T/EV-B/CMP re-run).
3. **A14** (AC-12 (b)): `grep -n "@ConscryptMode(OFF)\|@LooperMode(LEGACY)" $TS/mail/ssl/DefaultTrustedSocketFactoryJdkProviderTest.java`
   (two lines) and every case of that class `PASS` (its `@Before` guards ran). **Row:** D68.
4. **TLS classes** (AC-12 (c), (d)): in `I7-cases.txt` every case of `DefaultTrustedSocketFactoryHandshakeTest`,
   `…HardeningTest`, `…JdkProviderTest`, `…SniTest` and `PinnedCertificateSocketFactory*Test` `PASS`, and
   `git diff --stat HEAD -- app/src/test` empty. **Row:** D72.
5. **DEP(7)** (two runs) → `$E/I7-deps-releaseRuntime.txt` (equal to I6: a test-only bump) and
   `$E/I7-deps-debugUnitTestRuntime.txt` (changes recorded: Robolectric, `androidx.test`, Conscrypt, BouncyCastle, ASM;
   any `androidx.*` S4 library version moved on the test classpath is recorded and handed over, 2.8).
6. **A21** (AC-12 (e)) into `$E/I7-monitor.txt`: from step 5's test classpath the resolved `androidx.test:monitor`
   version; the AAR Gradle resolved, located by coordinate in the Gradle cache (`ls "$HOME"/.gradle/caches/modules-2/files-2.1/androidx.test/monitor/<v>/*/monitor-<v>.aar`),
   its SHA-256 compared with the `<sha256>` of `monitor-<v>.aar` in `gradle/verification-metadata.xml` (`match` expected),
   `unzip -p <aar> META-INF/com/android/build/gradle/aar-metadata.properties` (absent at 1.8.0, L13, or its
   `minCompileSdk`), and the manifest `minSdkVersion`. Nothing downloaded by hand; the path is not printed. **Row:** D74.
7. **android-all** (AC-12 (f)) into `$E/I7-android-all.txt`: the folder listing as I0 step 9, the new folders named,
   and for each new jar and POM its size and SHA-512 compared with its `.sha512` file (U-068 ruling P-1 method). A
   failed download was retried once (row D73).
8. **Probe (after)** by 4.P with k = 7 → `$E/I7-probe.txt`; `git -c core.autocrlf=false diff --no-index -- $E/I0-probe.txt $E/I7-probe.txt`
   lines into the log (provider version, default-enabled suites before and after; AC-12 (d)'s "both lists"); a
   difference with every test passing is classified and reported (row D96).
9. CK(7) with: `git diff --stat HEAD -- app/src/test/resources/robolectric.properties` empty (AC-12 (g)).
   **Rows:** D68 to D74, D27 to D53 (F5 by D69). **Must hold:** gates green with no flags (or exactly the named flags);
   both modes offered; guards pass; TLS classes and U-066's exact-list tests unchanged and passing.
10. Log entry; CM(7).

### I8 to I11 — Mockito, Truth, GreenMail, auto-service (AC-13), one commit each

For k = 8, 9, 10, 11 in that order: apply E<k>; VMP(k), G(k), EV-T(k), EV-B(k), CMP(k), CK(k); log entry; CM(k).
- **I8:** `I8-checks.txt` adds the resolved `net.bytebuddy:byte-buddy` version from `I8-vm-added.txt` (1.17.7 expected,
  L13), the supplement's Q-9 item in the log as a stated fact (Byte Buddy's release notes, version 1.17.5 of 30 March
  2025: ASM updated to 9.8 to support Java 25, `https://github.com/raphw/byte-buddy/blob/master/release-notes.md`,
  fetched 2026-11-21; the Mockito 5.24.0 entry is silent on Java 25), and `grep -c "Mockito is currently self-attaching\|A Java agent has been loaded dynamically" $WORK/logs/I8-gates.log`
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
   `compat` named); `grep -c "Unsupported class file major version" $WORK/logs/I12-gates.log` → `0`. A package whose
   `t` changed while the inventory is equal (a JaCoCo Kotlin filter, R19) is recorded with the per-class lines of
   `I11-`/`I12-jacoco-classes.txt` and Q-11's change history (which lists no filter change for 0.8.16); it stops only
   if a package goes `BELOW` (D38) or the inventory differs (D79).
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
   configuration: each kept) and "Runtime claims" (none: no device run by the developer); IC-5 named as the
   orchestrator's (10.2) and IC-6 recorded "not reached: needs the product owner's decision" with 10.3's text; then the hygiene scan of every evidence file of the story:
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
- **STD(k)** = `$W/evidence/I<k>-{gates.txt,warnings.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,jacoco-inventory.json,jacoco-classes.txt,coverage.txt,ksp-files.txt,merged-debug.xml,merged-release.xml,manifest-debug.txt,manifest-release.txt,kept-names.txt,kept-names-all.txt,configuration.txt,configuration-norm.txt,seeds-core.txt,usage-core.txt,r8.txt,services.txt,backup-descriptor.txt,apk-entries-debug.txt,apk-entries-release.txt,apk.txt,javap.txt,compare.txt,checks.txt}` (28 files; `services.txt` added by Amendment A1)
- **VM(k)** = `$W/evidence/I<k>-{writer.txt,vm-proof.txt,vm-added.txt,enforced.txt}` (4 files)
- **I0SET** = `$W/evidence/I0-{start.txt,oldtext.txt,greps.txt,vm-counts.txt,gradle-version.txt,gates.txt,warnings.txt,junit-totals.txt,cases.txt,jacoco-packages.txt,jacoco-inventory.json,jacoco-classes.txt,coverage.txt,ksp-files.txt,merged-debug.xml,merged-release.xml,manifest-debug.txt,manifest-release.txt,kept-names.txt,kept-names-all.txt,configuration.txt,configuration-norm.txt,seeds-core.txt,usage-core.txt,r8.txt,services.txt,backup-descriptor.txt,apk-entries-debug.txt,apk-entries-release.txt,apk.txt,javap.txt,d7.txt,lint.txt,lint-ids.txt,lint-baseline-count.txt,deps-releaseRuntime.txt,deps-releaseRuntime-tree.txt,deps-debugUnitTestRuntime.txt,android-all.txt,probe.txt}` (40 files; `services.txt` added by Amendment A1)
- **TOOLS** = `$W/evidence/tools/{oldtext_check.py,gradlew_block.py,vm_proof.py,deps_resolved.py,cov_floors.py,jacoco_class_lines.py,scan.py,r8_check.py,U071ProviderProbeTest.java.txt}` (9 files)
- **LOG** = `$W/implementation-log.md`

| Commit | Subject | Files |
|--------|---------|-------|
| I1 | `U-071 S2-I1: Gradle wrapper 9.9.2 (Windows launcher block re-applied)` | `gradle/wrapper/gradle-wrapper.properties`; `gradle/wrapper/gradle-wrapper.jar`; `gradlew` (staged only if changed: expected unchanged, CM step 4 records it, row D99); `gradlew.bat`; LOG; `$W/evidence/compatibility-record.md`; TOOLS; I0SET; STD(1); `$W/evidence/I1-{wrapper-run1.txt,gradle-version-run1.txt,wrapper-run2.txt,gradle-version.txt,wrapper.txt}`; plus `gradle/verification-metadata.xml` and VM(1) only if row D25 fired |
| I2 | `U-071 S2-I2: KSP 2.3.14` | `build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(2); VM(2); `$W/evidence/I2-uptodate.txt` — **no commit** under row D54 |
| I3 | `U-071 S2-I3: Android Gradle Plugin 9.5.2` | `build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(3); VM(3); `$W/evidence/I3-{lint.txt,lint-ids.txt}`; plus `$W/evidence/I2-moved.txt` under D54; plus `app/build.gradle` under D55 or D56 |
| I4 | `U-071 S2-I4: Hilt 2.60.1 (plugin, runtime, compiler, testing)` | `build.gradle`; `app/build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(4); VM(4); `$W/evidence/I4-{kotlinc.txt,kotlinc-summary.txt,deps-releaseRuntime.txt,deps-releaseRuntime-tree.txt}` |
| I5 | `U-071 S2-I5: Kotlin 2.4.21 (plugin and stdlib)` (under D54: `U-071 S2-I5: Kotlin 2.4.21 and KSP 2.3.14 (KSP moved from I2)`) | `build.gradle`; `app/build.gradle`; `gradle/verification-metadata.xml`; LOG; STD(5); VM(5); `$W/evidence/I5-{kotlinc.txt,kotlinc-summary.txt,deps-releaseRuntime.txt,deps-releaseRuntime-tree.txt,uptodate.txt}`; `$W/evidence/mutation-log.md`; `$W/evidence/mutations/{M-1a,M-1b,M-2a,M-2b,M-3a,M-3b}-{run,cases,message}.txt` (18 files); plus the four Risk 9 files under D58 |
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

**Writers** (plan-review B-1: every listed file has a step that writes it; checked entry by entry in iteration 2):
- STD: `gates`, `warnings`, `junit-totals`, `cases`, `jacoco-packages`, `jacoco-inventory`, `jacoco-classes`, `coverage`
  by EV-T; `ksp-files`, `merged-*`, `manifest-*`, `kept-names`, `kept-names-all`, `configuration`, `configuration-norm`,
  `seeds-core`, `usage-core`, `r8`, `services` (Amendment A1: `r8_check.py services`, the line after the `I<k>-r8.txt`
  block), `backup-descriptor`, `apk-entries-*`, `apk`, `javap` by EV-B; `compare` by CMP;
  `checks` by CK (plus the increment's appended lines). VM: `writer` (VMP 2), `vm-proof`, `vm-added` (VMP 3),
  `enforced` (VMP 5).
- I0SET: `start` (I0 1), `oldtext` (I0 3), `greps` (I0 4, plus the merged-manifest line of I0 6), `vm-counts` (I0 5),
  `gradle-version`, EV-T/EV-B files and `d7` (I0 6), `lint`, `lint-ids`, `lint-baseline-count` (I0 7), the three
  `deps-*` (I0 8), `android-all` (I0 9), `probe` (I0 10). TOOLS: I0 2. `compatibility-record.md`, LOG: I0 11 (and every
  increment's log entry).
- I1: `wrapper-run1` (step 2), `gradle-version-run1` (4), `wrapper-run2` (5), `gradle-version` (7), `wrapper` (8).
  I2/I5 `uptodate`: UTD. I2 `moved`: I2 step 3 (D54 only). I3, I12 `lint`, `lint-ids`: their L step. I4, I5 `kotlinc`,
  `kotlinc-summary`: KC step; `deps-*`: DEP step. I5 `mutation-log.md` and `mutations/*`: 4.M. I6 `deps-*`: step 2.
  I7: `d7` (2), `deps-*` (5), `monitor` (6), `android-all` (7), `probe` (8). I13: `checks` (2), `ci-parity`,
  `warnings`, `lint-ids` (3), `ci-coverage` (4), `yaml` (5). I14: STD by steps 1-2, `gradle-version` (2), `deps-*` (3),
  `vm-proof`, `vm-added` (4), `sweep` (5), `final-checks` (7), `scan` (8); `compatibility-record.md` (8).
A listed file that is unchanged against HEAD drops out of the staged comparison and is recorded (CM step 4).
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
| D1 | `$SUPP` missing, or lacking an item for any of Q-1 to Q-9 at I0 step 1 (an item in the supplement's ruled form — exact table values and checksums, stated facts for prose — counts; a missing full quotation is not a reason to stop) | Stop before I0 step 3 (no Gradle run); report the missing items. Q-13 to Q-17 missing at I3: not a stop by itself; an I3 difference only they could explain then stops (D44) |
| D2 | A story of SC-1/SC-9 (U-067, U-068, U-069, U-070), U-074 or the BUG-029 fix story is not `done`, or `c12a2788` is not an ancestor | Stop (SC-1, SC-9; U-074 and the BUG-029 fix edit the files I5 mutates) |
| D3 | An SC-2 check fails (minSdk, merged manifest, `junitXml`, the receiver test, the grep, the metadata diff since `$U068_START`) | Stop (SC-2) |
| D4 | CR-015 or CR-016 not `implemented` | Stop (SC-3) |
| D5 | Build-file drift since PLAN_HEAD other than `2 2 app/build.gradle` at `:579`/`:586`, a code-location hunk other than `:28`, or an `app/src/main` file changed since PLAN_HEAD that none of U-069, U-070, U-074 and the BUG-029 fix story names | Stop and report: an edit text or anchor of section 3 or 4.M may have moved; the SA re-derives |
| D6 | `oldtext_check.py` reports `MISMATCH` ("old text not found exactly once"; for example the BUG-029 fix moved or reworded mutation anchor M-1 in `BackupWorker.kt`) | Stop and report with the failing ids; no edit is attempted, and the developer never adapts an anchor: the SA writes a re-anchoring amendment from the start-state text |
| D7 | Porcelain on the story's paths not empty at I0, or a Gradle JVM is live (a `KotlinCompileDaemon` alone is recorded, not a stop) | Stop; wait or ask the orchestrator (SC-5) |
| D8 | `C0` cannot be computed, or `@Ignore` lines differ from the two known | Record; stop if a new `@Ignore` appeared (not this story's) |
| D9 | G(0) fails | Stop: the start is not green (not this story's to fix) |
| D10 | Lint at I0 exits 0, or its rule IDs differ from U-068 I6's | Record (lint is recorded, not gated, D17) |
| D11 | Verification counts differ from 803/1460/3 | Record; if `git diff $U068_START HEAD -- gradle/verification-metadata.xml` is not empty, stop (SC-2) |
| D12 | The probe does not compile or run at I0 | Record "probe unavailable", delete it (4.P step 5), continue; at I7 AC-12 (d) rests on the exact-list tests and their failure messages |
| D13 | A supplied item no longer says what the research says | Record it in the supplement's form (table value, checksum or stated fact, with address and date); if a constraint row no longer holds (for example AGP 9.5's minimum Gradle above 9.9.2), stop before I1 (D1a rule 3 / D3) |
| D14 | The supplement reports a newer non-pre-release for a row | Patch: substitute the version token (2.1), re-check that component's constraint rows, record; minor or major: stop before I1 for an SA amendment |
| D15 | W1 exits non-zero | Stop; `git restore --source=HEAD --worktree -- gradle/wrapper gradlew.bat` and `git -c core.autocrlf=false restore --source=HEAD --worktree -- gradlew` (LF, plan-review O-6); report verbatim |
| D16 | The generated file after a wrapper run still contains the block (`grep -c 'LOCAL ADDITION' gradlew` is not `0`) | Unexpected generator behaviour: E1 is not applied; restore the wrapper files from HEAD (as D15); stop and report |
| D17 | `gradlew_block.py` reports `FAIL` | Stop; restore the wrapper files from HEAD (as D15, `gradlew` with `core.autocrlf=false`); report which check failed (N2) |
| D18 | The first 9.9.2 run reports a distribution checksum failure, or `dist-sha-match=no` | Stop (INT-002): never edit the checksum; restore the wrapper files as D15 (`gradlew` with `core.autocrlf=false`; plan-review O-13); report |
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
| D37 | A test fails at a gate | Record the case and message verbatim; stop; never edit or skip a test (Scope Concern 4); TLS cases go to D72. At I7 the report names Robolectric 4.18.1's "content providers are now installed for lazily created applications" (supplement Q-8) as a candidate cause when an application-context test changes (plan-review O-11) |
| D38 | `jacocoTestCoverageVerification` fails or a package is `BELOW` | Stop; never edit a floor; report with `I<k>-coverage.txt` and the `jacoco-classes` diff; a coverage-restoring test is an SA amendment (AC-20 (c)) |
| D39 | Coverage changed but every package above its floor | Record the per-class difference; `activity` alternation (2.6) is noise; any other change is explained from per-class/per-line evidence or reported |
| D40 | Class inventory or a package's `t` changed outside what 2.9 allows | Stop and report. At I12 (AC-14): an inventory change stops (D79); a `t` change with an equal inventory is recorded with the per-class lines and Q-11, and stops only through D38 (R19) |
| D41 | A merged or packaged manifest differs from `<p>`, or an invariant count is wrong | Stop and investigate; report (AC-21 (a)). If the invariant line is already not the expected one at I0 (for example the BUG-029 fix changed the `SystemForegroundService` entry before `<start>`), stop at I0: the SA re-derives the expected values from I0 before I1 |
| D42 | R8-1's four-prefix `kept-names.txt` differs from `<p>`; R8-2 `not-identity` above 0; R8-3 `seeds-core.txt` differs from `<p>`; an R8-4 rule `FAIL`; or a `kept-names-all.txt` difference outside the four prefixes not traced to the raised component's allowed section | Stop and report (AC-21 (b); contract-verification B-1); also when `manifest-classes-in-mapping` falls below `<p>`'s. **Ruling 58 (SC-P6):** a `seeds-core.txt vs <p>` difference at I4, I5 or I6 is accepted and recorded when (a) `r8_check.py seeds-diff` prints `MATCH` for every added or removed line — at I4 and I6 the Dagger/Hilt patterns of 2.10 R8-3 (on the simple class name; a class of `com.zegoggles.smssync.` also with its source under a generated root), at I5 only the Kotlin compiler-generated members; (b) each changed line is written into the log with the pattern it matched; (c) R8-1 (four prefixes and the traced rule for the others), R8-2 and R8-4 hold; (d) no line is `FORBIDDEN` (nothing under `com.google.crypto.tink.`, no worker-constructor line, Kotlin's synthetic constructor with `DefaultConstructorMarker` included). A removed (`-`) line of a generated class the raised component no longer generates shows its source through `<p>`'s KSP file list (`--prev-ksp`, plan-review O-21); a class of the Hilt component sources, which that list does not hold, stays `NOT-GENERATED` and stops (stricter than the ruling, accepted). Lines under `androidx.work.`, `androidx.core.app.`, `androidx.profileinstaller.` and `androidx.room.` are never accepted, nor non-`com.zegoggles.smssync.` lines at I5 (contract-verification O-16). Anything else stops. **At I3** a `seeds-core` difference is outside ruling 58: stop and report with the changed lines; the orchestrator decides (plan-review O-16) |
| D43 | The normalised `configuration.txt` (R8-6) differs from `<p>` | **Ruling SC-P1 (Record 19 ruling 48):** accept and record when `kept-names.txt vs <p>` is `exit=0`, R8-2 to R8-4 hold, and every added or removed rule lies in the section 2.10 (R8-5) allows for this increment — `agp-default:proguard-android-optimize.txt` at I3, `lib:hilt-android/proguard.txt` or Dagger's `jar-rules:lib/META-INF/com.android.tools/r8/r8.pro` (changed or added, shown in the resolved `dagger-2.60.1.jar`) at I4, a `jar-rules:` section changed **or added** and shown inside the resolved `kotlin-stdlib-2.4.21.jar` at I5, `lib:hilt-work/proguard.txt` or a section changed or added from a resolved androidx.hilt 1.4.0 artifact at I6 (ruling 48 as restated for plan-review O-14: a section new or changed that comes from the raised component's jar is inside the ruling) — each rule and its source section and jar written into the log. Any other difference (another section, an `aapt` change, a section added or removed that the raised component's jar does not carry, a difference at I1, I2, I7 to I12 or I14) stops. The comparison against I0 then carries the accepted difference forward and is not a new stop |
| D44 | APK entry lists differ (or, at I3, any other CMP difference 2.9 allows only "explained by AGP") | Record; each entry explained by the increment's component (2.9) — at I3 by an entry of Q-13 to Q-17 named in the log, as 2.11 lays out (repackaging renames `META-INF/services/<obfuscated>` entries; D101); an unexplained entry stops |
| D45 | `so` ≠ 0 | Stop |
| D46 | A class-file major other than 61, 61, 52 | Stop (AC-15); record the forcing error if any |
| D47 | The KSP list differs where 2.9 says "=" | Stop and report; at I4 each difference is explained or the story stops |
| D48 | JUnit totals differ from I0, or skipped rises | Stop and report (AC-19) |
| D49 | The case list differs from I0 | Stop and report (a renamed, lost or newly skipped case) |
| D50 | An error-class item in the warnings | Stop; warnings are debt only |
| D51 | `lintVitalRelease` fails `assembleRelease` on the new AGP | Stop: the fix would edit `lint.xml`, the baseline or source; SA amendment |
| D52 | "bash.exe not found", a vanished tool, or a process killed without a visible reason | Stop and report (endpoint security); no workaround, nothing restored from quarantine, no exclusion |
| D53 | The harness moves a Gradle run to the background | 4.L: wait for its completion notice; never poll; start nothing else; read `exit=` from the log |
| D54 | At I2, a KSP-originated error that names the Kotlin version, KGP or Kotlin metadata | The pre-specified reorder (AC-7): no I2 commit; E2 moves into I5; recorded, not a fallback. Any other KSP error at I2 is F3 (D35) |
| D55 | At I3, a JaCoCo class root missing or empty after `clean` | `find app/build/intermediates -type d -path '*debug*' -name classes` and the like; update that one `jacocoClassRootPaths` line (3.2); guard unchanged; recorded as a forced change |
| D56 | At I3, AGP rejects the `*SdkVersion` DSL names | Apply the A13 edits for `:app` (3.2), values unchanged; if `:k9mail-vendored` is rejected too, stop (AC-23 forbids its edit) |
| D57 | `receiver` at or below 0.50 at I3 | Stop (AC-8 (d)); report |
| D58 | At I5, a Hilt or KSP error naming `@ApplicationContext` or a qualifier at one of the four Risk 9 sites | Apply the four `@param:` edits (3.2), record the error verbatim as a forced change, re-run from VMP step 1 |
| D59 | At I4, `hiltJavaCompileDebug`, the ASM transform or a Hilt test fails | Hilt-plugin origin → D35; otherwise D36 |
| D60 | `worker` below its configured floor (0.90 after U-069, ruling 47) | D38: stop, never lower the floor, report with the per-class lines. A drop of one covered line that stays within the floor (10/10 to 9/10, or 16/16 to 15/16) is recorded and explained from the per-class lines (a generated class now reporting lines, or a counting change); it is not a stop (L17: one spare line) |
| D61 | F-1 stop rule fails (a NEW3 case fails, `app/src/test` differs, or a `coroutineContext` warning in either worker) | Stop and report before the mutations |
| D62 | A mutation's must-fail test passes | Finding "AC-10 unmet for <id>"; finish the remaining entries; stop before CM(5) and report (I5 stays uncommitted, every mutation restored) |
| D63 | A mutant does not compile | Record "invalid"; restore; stop and report |
| D64 | An original that must pass fails, or a later case fails under a mutation of a loop other than the one I0 step 13's table assigns it (or a case the table marks "none" fails) | Restore; stop and report |
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
| D75 | A runtime-classpath or test-classpath change at a test-only increment (I8 to I12) | Detector: VMP step 4's added groups (an `androidx.*` group other than the increment's expected closure is the signal) and I14's DEP reports. Release: D31; test: record, explain |
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
| D92 | `clean` or `rm -rf $R` fails with "Unable to delete file" or a file in use under `app/build` (a Windows lock, typically a Kotlin compile daemon of the previous KGP) | Record verbatim and the `jps` line; stop and report. Never kill a process, never delete by other means, never retry in a loop |
| D93 | A Gradle run is cut off (its log has no `exit=` line, or the harness reports it terminated rather than backgrounded) | 4.L: stop and report; not repeated in the same session; a later session re-runs that step once from its start (a writer pass from VMP step 1); a second cut-off of the same step stops for the orchestrator |
| D94 | I0 step 1 finds a cache turned on (`caches-on` or `gradle-opts-caches` above 0) | Record (no path printed); WP then also carries `--no-build-cache --no-configuration-cache`, recorded as the only change to the design's command; the other runs carry them already |
| D95 | A check can pass only with an edit to `AndroidManifest.xml`, `proguard-rules.pro` or a keep rule, or CK shows such a file changed | Stop; nothing edited. The SA reads "CNTR-005, Validation Rules" and the compatibility clauses after them (hint: `:158-165` in v1) and "CNTR-004, Breaking change policy" (hint: `:266` in v3; v4 will move it) and decides whether a change record and a contract version step are needed before any edit (AC-23; contract-verification O-2) |
| D96 | The probe at I7 differs from I0 (`tls_provider`, the provider list or versions, the default-enabled protocols or suites) while every TLS test passes | Classify each difference in the log (provider version change from Robolectric's Conscrypt, suite set change, or protocol change) with both lists; report it in the session hand-off; not a stop (the tests decide, D72); CR-014 Amendment 1's applier check is named for re-confirmation in the hand-off |
| D97 | A DEP run fails (for example on a configuration verification never covered) | Record verbatim; never write metadata for it; stop and report if it is `releaseRuntimeClasspath` or `debugUnitTestRuntimeClasspath` (both are resolved by the writer pass, so a failure there is D32) |
| D98 | `seeds.txt` or `usage.txt` is missing from the mapping folder | Record the listing; R8-3 cannot run: stop and report (the SA decides whether `-printseeds`/`-printusage` must be checked in the AGP version) |
| D99 | A listed file is unchanged against HEAD at CM (for example `gradlew` at I1) | Record "listed, unchanged, not staged" in the log; not a stop (CM step 4) |
| D100 | The `backup exclude=… sharedpref=… credentials=… pinned=…` line of CMP differs from `4 4 2 2` (contract-verification O-8) | Stop and report (CNTR-003, CNTR-002: the backup exclusions); nothing edited |
| D101 | At I3 an R8 difference that 2.11 classifies as explained by Q-13 (repackaging) or Q-14 (`-keepattributes` wildcard) | Record it with the item; not a stop. `ENTRY` lines of `I3-services.txt` are paired with `<p>`'s by original name. A difference 2.11 marks "a stop" — including `services-pairs vs <p>` or `vs I0` `exit=1` at any increment (Amendment A1) — or any R8-1 to R8-4 failure, stops (D42, D44); no `-dontrepackage` or keep rule is added |

## 7. Session Plan

Six developer sessions, each ending at a clean hand-over state: every increment it touched committed, nothing
half-edited, no mutation or probe in place, `gradle/verification-metadata.xml` equal to HEAD, no Gradle JVM, and the
log's last line naming the next step ("Next: I<k> step 1"). Each session after the first starts with 4.R.

| Session | Increments | Gradle runs (planned) | Compute estimate | Why the boundary is here |
|---------|------------|-----------------------|------------------|--------------------------|
| A | I0, I1, I2 | 15 | about 2 h | Baseline and the two changes AGP 9.5 needs first; ends on a Gradle 9.9.2 + KSP 2.3.14 tree |
| B | I3, I4 | 10 + 1 resume VER | about 1 h 45 min | AGP is the largest dependency change (G1, G5, A13, C-1/C-2; the `agp-default` section accepted under SC-P1 if it changes) |
| C | I5 | 13 + 1 | about 2 h | Kotlin plus six mutations: the session must not end with an `APPLYING` entry |
| D | I6, I7 | 11 + 1 | about 1 h 45 min | androidx.hilt and Robolectric: the TLS and probe work needs a fresh context |
| E | I8, I9, I10, I11 | 12 + 1 | about 1 h 45 min | Four low-risk bumps of the same shape |
| F | I12, I13, I14 | 10 + 1 | about 1 h 45 min | JaCoCo last among build changes, then CI, documents and the final checks |

**Totals:** 71 Gradle runs in the increments plus one VER at each of the five resumptions = **76**, over six sessions
(iteration 1: 63 and 6). Estimates: G about 9 to 11 min (with `clean`), WP 12 to 16 min, ENF 2 to 3, UTD 1 to 2, L 3,
KC 2, DEP 1 to 2 per configuration, TR 2 to 3, VER under 1 (the first 9.9.2 one downloads the distribution). **WP and
most G runs, and the first G on Gradle 9.9.2 with cold caches, exceed the 600000 ms foreground limit and are expected to
be moved to the background (4.L, D53); a run cut off instead is D93.** A stop (any "stop" row) ends the session at once
with the working changes either restored (fallbacks) or left as the row says, and the log says which. A session may
end earlier at any increment boundary.

## 8. Items for the Orchestrator (hand-over)

1. **H-1:** Q-1 to Q-12 are supplied (`$SUPP`, commit `c9b3954d`). **New, before I3:** Q-13 to Q-17, the
   "Behavior changes" and changed-default entries of the AGP 9.1 to 9.5 release notes (2.3); Q-18 (Gradle 9.6 to 9.9
   upgrade-guide entries) is optional.
2. **H-2, rulings:** SC-P1 to SC-P5 are ruled (Record 19, rulings 48 to 51) and carried (D43, 2.10, 9.2); SC-P6 is ruled by ruling 58 and
   carried (D42, 2.9, 2.10 R8-3, `r8_check.py seeds-diff`). **Decisions the orchestrator may still be asked for:** a
   `seeds-core` difference at I3 (the new R8 with AGP 9.5.2; outside ruling 58, plan-review O-16); a `seeds-core` line
   at I4 to I6 that `seeds-diff` does not accept; an R8 difference at I3 that 2.11 does not explain. No new scope
   concern is raised: no `-dontrepackage` or keep rule is needed in advance (2.11).
3. **H-3, `.claude/CLAUDE.md`** (Engine Librarian; the developer does not edit it). No gate text changes, so AC-16 (d)
   needs no edit. Optional, if I1 confirms the two-run behaviour (L5): in the "Local Build & Device Notes" bullet
   "Build JDK", replace "`gradlew` carries a hand-maintained Windows `JAVA_TOOL_OPTIONS` block; re-apply it after any
   `./gradlew wrapper` regeneration." with "`gradlew` carries a hand-maintained Windows `JAVA_TOOL_OPTIONS` block;
   re-apply it after every `./gradlew wrapper` run (a version upgrade runs the task twice, on the old and then on the
   new Gradle, and each run drops the block)."
4. **H-4, fallbacks:** if one fires, the developer stops; the orchestrator records the product-owner flag (F2, F4 with
   their Part B consequence, verbatim from 2.4) and authorises resumption with the inputs 2.3 names.
5. **H-5, devices:** IC-4 and IC-5 are the orchestrator's (10.1, 10.2; Record 20, ruling 57); IC-6 is recorded "not
   reached: needs the product owner's decision (PO-B)"; `Pixel_4_2` stays unstarted (PO-H).
6. **H-6, for U-072 (S3):** the AGP 9.5.2 lint rule-ID table of I12 and I13 is S3-I0's "before"; `robolectric.properties`'
   comment (S3-I3); `ci.yml` lines move by one from `:21` on after I13 (the comment gains a line); Build Tools stays
   36.0.0; the N-6 move if D74 fires.
7. **H-7, for U-073 (S4):** `I14-deps-releaseRuntime.txt` is S4's baseline; the `worker` floor (0.90) leaves one spare
   line (L17); the Mockito JEP 451 warning (I8) is debt; the test-classpath versions Robolectric 4.18.1 pulls (I7); U-069's
   recommendation to re-run its mutations at S4-W; the F-1 rule at S4-C and S4-W.
8. **H-8, follow-ups outside this story:** `DEVELOPMENT.md:25-30` and `:49` still describe `org.gradle.java.home` and
   "pins JDK 17" (stale since U-060); `DefaultTrustedSocketFactoryJdkProviderTest.java:60` names Robolectric 4.15.1
   (left, Scope Concern 4); `code-location.md` "Key Dependencies" lists libraries removed long ago (otto,
   firebase-jobdispatcher, the JitPack k-9 coordinate); pinning the GitHub actions by commit SHA (design security
   section, a recommendation); for the next contract amendment, CNTR-003 `:260` ("minSdk 21 platform") and CNTR-005
   `:65` (a stale manifest line citation), neither edited here (contract-verification O-5); a later story that turns
   signature verification on or moves the versions into a catalog (2.5).
9. **H-9:** deleted in iteration 2 (ruling 47 settled the `worker` floor at 0.90).
10. **H-10, IC-1/IC-2 reading recorded:** AC-6 (f)/IC-2 "no `org.gradle.java.home` anywhere in the repository" is read
    as "not set in any properties, Gradle, wrapper or CI file" because the phrase appears in prose (L22) (SC-P5).

## 9. Contracts, Scope Concerns, Risks, Out of Scope

### 9.1 Contracts (approved; read, not edited)

- **CNTR-MODERNIZATION-001** (TLS socket factories): touched by I7 in the test provider only; production TLS code is
  not edited; U-066's exact-list tests must pass unchanged (row D72); no revision needed. The contract itself says
  nothing about providers or Robolectric; the binding statement is CR-014 Amendment 1 (the allow-list filtered from the
  platform's default-enabled suites), whose applier check D96 names for re-confirmation.
- **CNTR-MODERNIZATION-004** (scheduler, v3) and **-005** (the `BACKUP` broadcast, frozen): the `HiltWorkerFactory`
  path is guarded at every gate from I4 on by `ic1_…` and the KSP list, and at I6 by D66 and D67; the persisted worker
  names by R8-1, R8-2 and R8-4 (a), (b), (c) (2.10); the `BackupBroadcastReceiver` manifest entry by the invariants at
  every increment (2.9); a forced manifest or keep-rule edit is D95, which cites "CNTR-004, Breaking change policy"
  and "CNTR-005, Validation Rules" by heading (line numbers only as hints; v4 of CNTR-004 will move them); no revision.
- **CNTR-MODERNIZATION-002** (pinned-certificate enrolment): the `pinned_certs.xml` backup exclusion is checked in the
  packaged backup descriptor at every increment (D100); no revision.
- **CNTR-MODERNIZATION-003** (secret store): Tink's protobuf field rule and the Tink seeds are guarded by R8-4 (d) and
  R8-3; the backup exclusions of `credentials.xml` (and `pinned_certs.xml`, CNTR-002) by the packaged backup descriptor
  at every increment; its runtime libraries are S4's and stay at today's resolved versions (D31); no revision.
- **CNTR-MODERNIZATION-006**: considered, no obligation a toolchain raise reaches (it names the event and state-flow
  surface only); its runtime libraries are S4's and stay at today's resolved versions (D31); not listed in
  `contracts_verified` (contract-verification O-9).
- No new boundary is introduced, so no new contract is needed (contract gate satisfied).

### 9.2 Scope Concerns of this plan

| Id | Concern | Status and what the plan does |
|----|---------|-------------------------------|
| SC-P1 | AC-8 (c) and AC-21 (b) require `configuration.txt` equal; AGP 9.5's default file and the raised libraries' consumer rules will probably change it at I3 to I6 (P2) | **Ruled (Record 19 ruling 48):** accepted and recorded when the kept-name set equals the previous increment's and every changed rule comes from the raised component's own shipped rules, each source named; any other difference stops. Carried by D43 and 2.10 (R8-5's allowed-section table), with R8-2 to R8-4 as conditions |
| SC-P2 | The throwaway provider probe (D7 item 5, AC-12 (c)) | **Ruled:** allowed as planned (4.P) |
| SC-P3 | The KSP list from I5 on equals I4's | **Ruled:** as stated (2.9) |
| SC-P4 | Only the pre-specified forced edits; any other forced production fix stops for an SA-supplied exact edit | **Ruled:** as stated (D34) |
| SC-P5 | "No `org.gradle.java.home` anywhere" = not set in any properties, Gradle, wrapper or CI file | **Ruled:** as stated (I0 step 4, I1 step 8, I14 step 5) |
| SC-P6 | R8-3 made any `seeds-core` difference a hard stop; Hilt 2.60.1 (I4), Kotlin 2.4 (I5) and androidx.hilt 1.4.0 (I6) regenerate code | **Ruled (ruling 58):** accepted and recorded at I4 and I6 for the Dagger/Hilt patterns and at I5 for the Kotlin compiler-generated members only, each changed line listed with its pattern, R8-1, R8-2, R8-4 holding, no Tink and no worker-constructor line changing (synthetic constructor included); anything else and any difference at I3 stops. Carried by D42, 2.9, 2.10 R8-3 and `r8_check.py seeds-diff` |

### 9.3 Risks (likelihood / impact)

| # | Risk | L / I | Mitigation | Detection |
|---|------|-------|------------|-----------|
| R1 | The set fails outside the vendors' ranges (G1 to G6) | Medium / High (effort) | One component per increment; F1 to F5; stop-and-report | Gates, warnings, KSP list (D35) |
| R2 | A `configuration.txt` section outside R8-5's allowed table changes (for example a `jar-rules:` section at I3 because AGP extracts library rules differently) | Medium / Medium (delay) | Ruling SC-P1 applied per normalised section (2.10); a change outside the table stops for a decision | CMP (D43) |
| R3 | `worker` loses two lines against a floor with one spare line | Low / Medium | Per-class lines at every increment; one line is absorbed and recorded; stop, never lower | `cov_floors.py` (D60, D38) |
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
| R19 | JaCoCo 0.8.16's Kotlin filters change a package's `t` at I12 with the inventory equal | Medium / Low (delay) | Per-class lines against I11; Q-11's change history (no filter change listed); a stop only through `BELOW` | D40, D38 |
| R20 | A `seeds-core` difference stops the story: at I3 from the new R8 (outside ruling 58), or at I4 to I6 in a line `seeds-diff` does not accept | Low to Medium / Medium (delay) | Ruling 58 accepts generated-code lines at I4 to I6; Tink and the worker constructors stay hard stops; at I3 the orchestrator decides with the changed lines | D42 |
| R22 | An I3 difference from repackaging (Q-13) is misread as a lost keep, or a real lost keep is waved as repackaging | Low / High | 2.11 separates the two by check: identity lines, seeds and the closure are unaffected by repackaging, so any change there is a stop; only obfuscated names, mapping lines and `META-INF/services/<obfuscated>` entries are explained | D42, D101 |
| R21 | A Gradle run is cut off at the tool limit instead of being backgrounded | Low / Medium | 4.L: the log's `exit=` line decides; one re-run in a later session | D93 |

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

### 10.2 IC-5 — backup and restore on `Pixel_9_Pro`, and the API 24 device (orchestrator; Record 20, ruling 57)

The product owner allowed on 2026-11-21 (Record 20): on `Pixel_9_Pro` a restore with the default-SMS-role hand-over,
toggling an app setting and restoring it, and backups that write the other mail folders; an Android 7.0 (API 24) image
and one new virtual device created by the orchestrator with the SDK's own tool. Still the owner's: the account password
(Record 14 row 3), the emulators' system settings (row 4), `Pixel_10_Pro` (row 2), `Pixel_4_2` (PO-H). If a rollback
would need the app's data cleared, the orchestrator stops and tells the owner first. Text evidence only, sanitised, into
`$E/IC5-devices.txt` (no account name, no host name, no message body; counts and status lines).

**On `Pixel_9_Pro`** (after IC-4, same build, same identity checks 10.1 steps 1-2, `MSYS_NO_PATHCONV=1` only for adb):
1. Record the app's current settings that the run will touch (read-only: `run-as … cat shared_prefs/com.zegoggles.smssync_preferences.xml`,
   the keys only, values of `third_party_integration` and the backup-type keys); device time `T`.
   **Throughout steps 2 to 4** (plan-review O-18): the app stays in the foreground on `MainActivity` until each
   worker's result line (no Home, no app switch, no screen-off); `third_party_integration` stays off and no setting
   that schedules a backup (the regular schedule, the incoming-SMS backup) is toggled, so no backup starts in the
   background, the case BUG-029 describes (`setForeground` refused for a background start on Android 12 and later).
2. **Manual backup:** start it from the screen of `MainActivity` (the orchestrator's usual UI method); wait for the end of the run in
   `adb logcat` (the `BackupWorker` result line); record the worker result (success, items backed up as a count), and
   `FATAL EXCEPTION`, Hilt/`dagger` initialisation errors, `IllegalArgumentException`, `SecurityException`,
   `ForegroundServiceStartNotAllowedException` on the app's process id → `0`.
3. **Manual restore:** start it from the screen of `MainActivity`; accept the default-SMS-role request when the system shows it;
   record the `RestoreWorker` result line and the same error counts → `0`; after the restore, hand the default-SMS role
   back to the app that held it before (the system dialog the app opens), and record which app holds it at the end
   (equal to before).
4. Any setting toggled for the run (only a backup data-type setting, never one that schedules a backup) is set back to
   the value of step 1 and re-read (equal). Nothing is uninstalled,
   nothing cleared; `adb -s <serial> emu kill`.

**On the API 24 device** (only if the orchestrator has created it by then, DV-3): identity check by `emu avd name` and
`getprop ro.build.version.sdk` → `24`; `adb -s <serial> install` of the same debug APK; first-run permission prompts
recorded before anything else; `am start -W`, `pidof`, the logcat counts as 10.1 step 7. A backup and a restore there
need a configured account, which only the owner can enter: without it the API 24 check is **launch only**, and the log
says so. If the device does not exist yet, IC-5's API 24 part is recorded "not run: device not yet created" with the
same statement of what stands instead (the SDK 24 unit tests on Robolectric 4.18.1, the manifests' minSdk 24).

What IC-5 proves: a backup and a restore complete on the new toolchain's build on Android 16, and (if run) the app
installs and starts at the floor. What it does not prove unless the owner configures the API 24 device: a backup and
restore at API 24. REQ-015 AC-17 is met in full only with both.

### 10.3 IC-6 — recorded without the product owner's decision

- **IC-6:** "not reached: needs the product owner's decision (PO-B; design PO-4)". Stands instead: the YAML parses (I13
  step 5) and the local parity run reached every step (I13 steps 3, 4). Not proven: that the Linux runner builds, that
  the new action majors run, that `sdkmanager` works there. REQ-015 AC-18 is **not met** until run. Nothing claims CI
  has run.

## 11. Constraints for the Developer (binding)

1. Run Gradle in the foreground with the tool timeout at 600000 ms on every call, never with `MSYS_NO_PATHCONV=1`, never
   while another Gradle run is live.
2. If the tool moves a run to the background by itself, wait for its completion notice; never poll; start nothing else;
   read the result from the log's `exit=` line (4.L). A run cut off without `exit=` is D93: stop. Process checks match
   `gradle|KotlinCompileDaemon`; never kill a process (D92).
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
| Performance efficiency | Runtime: none expected (R8 input and kept names compared at every increment). Build: 71 Gradle runs in the increments plus five resume checks (76) over six sessions, the long ones backgrounded by the harness (4.L); Robolectric 4.18.1 may fetch new `android-all` jars; the verification file grows by an expected several hundred entries; test sandboxes stay at SDK 29 (D6), so no new per-fork memory for a higher level |
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
| Iteration 2: gate reports | `sdlc/artifacts/build/sprints/sprint-025/U-071/{plan-review.md,contract-verification.md,expert-depth-check.md}` | Read in full (the depth check's criteria 3 and 8 in full, the rest by section) |
| Iteration 2: supplement | `sdlc/artifacts/build/research/2026-11-21-u071-quote-supplement.md` | Read in full: Q-1 to Q-12 and the ruling on AC-3's form |
| Iteration 2: rulings | story Implementation Notes (Record 17, Record 19 rulings 48 to 51, ruling 47); decision appendix Record 20 and ruling 57 (`sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md:2459-2495`) | SC-P1 to SC-P5, the `worker` floor, the devices |
| Iteration 2: U-069 plan iteration 3 | `sdlc/artifacts/build/sprints/sprint-024/U-069/plan.md` (`:124`, `:1038-1056`, `:1572`) | Rule 15 texts after Amendment A4 (floor 0.90, 10/10) |
| Iteration 2: R8 and resource evidence | U-068 `evidence/I0-configuration.txt`, `I5-configuration.txt` (headers, the hilt-work, hilt-android, work-runtime, protobuf, aapt and AGP-default sections), `I5-manifest-*.txt`; the current `app/build/outputs/mapping/release/` listing, `seeds.txt`, `usage.txt` (formats), the two APKs via `aapt2 dump resources` and `dump xmltree`; `app/src/main/res/xml/backup_descriptor.xml` and `app/proguard-rules.pro` at HEAD | L26, L27, `r8_check.py` |
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
| IC-5 | 10.2 (orchestrator; Record 20) | `IC5-devices.txt` |
| IC-6 | 10.3 | log ("not reached") |
| Contract-bearing R8 clauses (CNTR-003, -004, -005; DES-008) | EV-B, CMP, 2.10 at every increment | `I<k>-r8.txt`, `-configuration-norm.txt`, `-seeds-core.txt`, `-kept-names-all.txt`, `-backup-descriptor.txt`, `-compare.txt` |

## Verdict

**PASS (iteration 3).** The story can be built as written, after U-068, U-069, U-070, U-074 and the BUG-029 fix
story. The supplement supplies Q-1 to Q-17 in the ruled form (Q-18 optional, not fetched); 2.11 states what Q-13 and
Q-14 change at I3 and which differences are recorded or stop. Ruling 58 is carried with a checker mode that lists every
changed seeds line with its pattern (D42, 2.10). Every fact the plan relies on was read in this
session from committed blobs, the research and its supplement, the design, the earlier stories' plans, logs and
evidence, the current build outputs (read-only), or the local caches (L1 to L27); every fact that needs Gradle, AGP,
Kotlin, KSP, Hilt, Robolectric, JaCoCo, R8, lint or a device is a `needs_check` with its step and decision rows (N1 to
N22; D1 to D101), and the plausible-only claims (P2 to P6) carry no step. Every finding of the three gates is taken up
(Iteration 2 and Iteration 3 tables; one depth-check could-item, dropping ENF, declined with the design's reason).
Iteration 3 read no build output under `app/build` (another agent's Gradle run was live); the release-dex worker-name
count is therefore not verified and is record-only. The plan:
- re-resolves the nineteen rows on the planning date (no row moved) and resolves the supplement's two facts under D1a:
  AGP stays 9.5.2 with F1 9.5.1; Kotlin 2.4.21's documented maxima are gap G2, F2's reason (2.1);
- compares the release build at six levels (R8-1 to R8-6) so that ruling SC-P1 can be applied section by section while
  the contract-bearing rules, members and manifest classes stay hard stops (2.10);
- keeps the design's order, the two pre-specified moves, F1 to F5 under the stricter stop-and-report ruling, and one
  local commit per increment with explicit, derivable file lists checked to exist and to equal the staged set;
- gives every edit as exact text, simulated on the start-state blobs with every expected line and hunk count derived
  (L11), and re-checked at I0 with a stop row;
- regenerates the wrapper twice so the jar is 9.9.2's own, and proves the Windows block byte for byte with a tool;
- applies D5 at every dependency increment with the U-060 proof as arbiter, never `diff`;
- re-runs U-067's six mutations at I5 under a write-ahead protocol that tolerates U-070's and U-074's later tests;
- states coverage margins from measured values, including the `worker` package's one spare line at floor 0.90;
- plans six sessions with clean boundaries, a resume check against this story's newest commit, and a procedure for
  runs longer than the tool limit.
SC-P1 to SC-P6 are ruled and carried. No scope concern is open; the orchestrator may be asked for a decision only on a
`seeds-core` difference at I3 or an R8 difference at I3 that 2.11 does not explain.

## Phase Completion Report
---
story_id: "U-071"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
notes: "Amendment A2 (ruling 65): B-8 fixed (r8_check.py section writes LF; the jar side keeps tr -d '\\r'); B-8 reproduced with the A1 tool (exit=1) and fixed with the A2 tool (exit=0) on U-068's committed configuration against the cached dagger-2.59.2.jar; every A1/A2 command self-tested end to end in Git Bash (13 cases) and every other Python-vs-shell comparison listed with its line endings (no further false stop found; one fresh-checkout note recorded). Amendment A1 (ruling 64): plan-review B-7 fixed with contract-verification O-13 (r8_check.py services; I<k>-services.txt at every increment; PAIR lines in original names equal at I0, <p> and I3, a difference stops); plan-review O-21 to O-24 and contract-verification O-14 to O-16 and the :1638 note taken up, none declined; tools re-tested in scratch on synthetic APKs, mappings and seeds inputs and on U-068's committed configuration; no build output read. Iteration 3: plan-review B-6 (ruling 58) and O-12 to O-18, contract-verification O-6 to O-12, the depth check's should-items and four of its five could-items taken up (Iteration 3 table); dropping ENF declined (design D5 step 6 binds it). r8_check.py gains seeds-diff and closure counts, re-tested on U-068's committed configuration evidence and synthetic seeds inputs in scratch. Q-13 to Q-17 carried (2.11: repackaging and the -keepattributes wildcard at I3; no -dontrepackage or keep rule). BUG-029 fix story placed before this story at I0. No new quote item needed (Q-18 optional). 71 Gradle runs in the increments plus 5 resume checks (76), six sessions, unchanged. No Gradle, build, test, device or network step was run in planning; no build output was read in iteration 3."
---
