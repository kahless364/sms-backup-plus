---
artifact_type: plan
story_id: "U-061"
gate_id: "planning"
checkpoint: "planning"
verdict: "PASS"
agent: "Solution Architect"
timestamp: "2026-11-12"
iteration: 2
base_commit: "218387187371237b288ca60db390441b6e66da80"
contracts_verified:
  - CNTR-MODERNIZATION-001
  - CNTR-MODERNIZATION-002
  - CNTR-MODERNIZATION-003
  - CNTR-MODERNIZATION-004
  - CNTR-MODERNIZATION-005
risks_identified: 10
---

# Implementation Plan: U-061 — Remove the Donation Flow and the Play Billing Dependency

## Revision History

| Iteration | Change |
|-----------|--------|
| 1 | Initial plan. Plan-review returned FAIL (`plan-review.md`) with B1 and B2. |
| 2 | **B1:** the I8 comment no longer contains "billing" or "donat". The AC-1, IC-10 and IC-13 greps now run after the I8 edit, as the last check before the final gate (I8.2). **B2:** snapshot-and-replay is dropped. The [commit mechanism](#commit-mechanism-for-ic-2-one-increment-per-developer-turn) is now one increment per Developer turn, with the orchestrator committing in between. It specifies the per-increment expected file lists, the orchestrator's staging rules and the mid-increment failure procedure. **Non-blocking:** (1) the third-party-integration check moves to I0 and fails closed; (2) `r8_config_diff.py`, the logcat FATAL count and `ui_titles.py` now fail closed; (3) IC-4's scratch copy is recorded as deviation 8; (4) I8 evidence is taken after the final gate; (5) QA reads the supersession record's content; (6) orchestrator and QA notes for deviation 1 and the IC-8(4) D6 reading. |
| 2a | Plan-review iteration 2 PASSED. Its four non-blocking observations are applied: the I8.8 lenient/off grep is path-limited (other checks audited); the revert restores only tracked paths and `rm`s untracked ones; reverted increments keep their log and evidence through a path-limited commit; deviation 9 covers the I8 final-gate ordering. |

## Story Overview

Delete the in-app donation feature and `com.android.billingclient:billing:7.1.1` (REQ-MODERNIZATION-018 as
amended by CR-003; design DES-MODERNIZATION-013 D1–D8 and D4a). The work is deletion only. The single new
symbol is the test-only nested `EnrollmentDialogThemeTest.HostActivity`. The design fixes every decision.
This plan turns them into ordered, verifiable increments (I0–I8), names the exact edits at base commit
`21838718`, and defines the evidence, the per-increment commit protocol, the stop conditions and rollback.

Every fact below was re-checked against the working tree at `21838718`, and the plan-review gate
re-verified them independently. `git diff --stat d931fde8 HEAD -- app build.gradle gradle README.md CHANGES`
is empty. The read-only probes run were `./gradlew --version`, `:app:processReleaseManifest` (build output
only), `:app:dependencies` for both runtime classpaths, `dexdump` on the existing debug APK, and a dry run
plus a write run of the strings script on a scratch copy of `res/`.

## Assumption Ledger

```yaml
assumptions:
  verified:
    - claim: "Base commit is 218387187371237b288ca60db390441b6e66da80. No app or build file has changed since d931fde8"
      evidence: "git rev-parse HEAD; git diff --stat d931fde8 HEAD -- app build.gradle gradle README.md CHANGES (empty)"
    - claim: "Build runs Gradle 9.5.1 with a JDK 25 daemon (criteria toolchainVersion=25). Launcher is JDK 25.0.5; JAVA_HOME is unset"
      evidence: "./gradlew --version; gradle/gradle-daemon-jvm.properties"
    - claim: "The release merged manifest path exists on AGP 9.0.1: app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml"
      evidence: "./gradlew :app:processReleaseManifest, then ls"
    - claim: "Both merged manifests contain the full AC-3 removal set. INTERNET and ACCESS_NETWORK_STATE are present"
      evidence: "grep -n over both merged manifests"
    - claim: "No keystore.properties exists, so the release signingConfig is null and assembleRelease produces app-release-unsigned.apk"
      evidence: "app/build.gradle:13-17,55; U-060 contract-verification.md:90"
    - claim: "JUnit XML is disabled in the root build.gradle:30; the U-060 QA init script re-enables it without tracked edits"
      evidence: "build.gradle:24-30; sprint-017/U-060/evidence/qa/qa-junit-xml.init.gradle"
    - claim: "697 @Test at HEAD (10 DonationActivityTest, 5 SkuTest, 3 EnrollmentDialogThemeTest); local JUnit XML gives tests=697 failures=0 errors=0 skipped=2"
      evidence: "CLAUDE.md git grep; junit_totals.py"
    - claim: "Local JaCoCo: root 18/30 60.0%; activity 199/1011 19.7%; activity.donation 79/236 33.5%; activity.fragments 58/483 12.0%; preferences 315/413 76.3%; receiver 29/58 50.0%; utils 30/199 15.1%"
      evidence: "jacoco_pkgs.py over app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"
    - claim: "Exactly 12 modules are reachable only through billing (billing plus 5 gms, 3 datatransport, 3 firebase-encoders). Debug and release graphs are identical (95 modules); no module resolves to more than one version"
      evidence: "graph reachability over fresh :app:dependencies trees"
    - claim: "The strings inventory is 235 lines across 22 files; per-file counts are in strip_strings.py EXPECTED. All strings are single-line. Lines are not contiguous in da, de, fr, pl and sk"
      evidence: "per-file greps; strip_strings.py dry run and write run on a scratch copy (235 deleted, 0 added, BOM and CRLF preserved, AC-1 and IC-13 greps 0)"
    - claim: "core.autocrlf=true. Working tree is CRLF except app/build.gradle, EnrollmentDialogThemeTest.java and code-location.md (LF). values-zh-rTW has a BOM. git apply and git restore work on the CRLF worktree"
      evidence: "git ls-files --eol; file; git apply -R --check probe"
    - claim: "Keys.DONATE is used only at MainSettings.java:24,172. Consts.Billing is used only at DonationActivity.java:46-47"
      evidence: "grep over app/src"
    - claim: "Removing checkUserDonationStatus() orphans the Log import (line 5) and the TAG import (line 22)"
      evidence: "grep 'Log\\.|TAG' MainSettings.java gives only lines 22 and 180"
    - claim: "BuildConfig.DEBUG is used by App.java:91 and auth/OAuth2Client.java:183"
      evidence: "grep -rn BuildConfig app/src/main/java"
    - claim: "ThemeActivity is public abstract with no abstract methods; onCreate sets an AppCompat-descendant theme from Preferences.getAppTheme()"
      evidence: "ThemeActivity.java:13-24; Preferences.java:289-292; styles.xml:24,31"
    - claim: "apkanalyzer is unusable (no cmdline-tools; the legacy tool fails on missing JAXB). dexdump from build-tools 36.0.0 works on the extracted dex (190 billingclient and 16 activity/donation classes today)"
      evidence: "probe runs"
    - claim: "The third_party_integration setting gates only the inbound com.zegoggles.smssync.BACKUP broadcast. It was ON on the Pixel_9_Pro AVD in sprint-017"
      evidence: "Preferences.java:179; preferences.xml:210-213 (default false); U-060 implementation-log.md:75,169,266"
    - claim: "The U-060 kept-name recipe and the norm() normaliser reproduce U-060 evidence exactly"
      evidence: "diff against U-060 kept-after.txt (same); empty normalised diff of U-060 baseline/after dumps"
  assumed:
    - claim: "No surviving module changes version (D4a)"
      reason: "Becomes the I5 deps_drift.py check (exit 1 = stop)"
    - claim: "R8 kept names lose only DonationActivity; configuration.txt loses only AAPT keeps for AC-3 components and removed-library rules"
      reason: "Becomes the I8 comm and r8_config_diff.py checks (fail closed)"
    - claim: "No JaCoCo floor is breached (root predicted 17/29 = 58.6%)"
      reason: "Measured at I6 and I8, with the IC-9 route if breached"
  needs_check:
    - claim: "Third-party integration is OFF on the AVD when IC-11 runs"
      blocker: "It was ON in sprint-017 and the Developer may not change it. A fail-closed, read-only check runs at I0 so the product owner can act during the build, and again at I8 before the device check"
```

## Deviations From the Story's Literal Text (1-8 ruled correct by plan-review; 9 added after review iteration 2)

1. **BuildConfig comment (I5).** The story says to reword `app/build.gradle:43` to name only `App.java`.
   `auth/OAuth2Client.java:183` also uses `BuildConfig.DEBUG`, so the plan names both files. **The
   orchestrator must record acceptance of this deviation in the story's Implementation Notes**, so that Code
   Review and QA do not flag the literal mismatch.
2. **Unused imports (I2).** The plan also deletes `MainSettings.java` line 5 (`import android.util.Log;`) and
   line 22 (`import static …App.TAG;`). Both are orphaned only by this story's deletion.
3. **AC-4 / IC-8(2) tooling.** The plan uses `dexdump` (build-tools 36.0.0) on the extracted dex instead of
   `apkanalyzer`, which cannot run, or `unzip -l`, which lists only `classes*.dex`. The Developer still
   records the failed `apkanalyzer` attempt.
4. **IC-8(4) is read per design D6.** A removed `configuration.txt` line passes if it is an AAPT `-keep`
   for one of the 7 AC-3 components or a rule from a removed library's section. **The orchestrator must
   tell QA that IC-8(4) is read per D6.**
5. **AC-1 timing.** `app/build.gradle` contains "donation" until I6, so AC-1 is asserted from I6 onward.
   The final AC-1 assertion runs after the I8 edit (B1).
6. **Orphan blank lines.** The plan also deletes the blank line next to a deleted block in preferences.xml
   (318), Consts.java (35), lint-baseline.xml (124) and README.md (45). The strings files have only their 235
   identified lines removed.
7. **Doc drift outside scope.** `sdlc/artifacts/engagement/endpoints.md:62` and `systems.md:55` still describe
   Play Billing. They are not edited, because AC-11 names only `code-location.md`.
8. **IC-4 uses a scratch copy, not a scratch branch.** The Developer may not create branches. The writer
   runs in a throwaway copy outside the repository, built with `git archive` plus the uncommitted I8 diff.
   This gives the same evidence with better isolation and no change to git state. The Developer records this
   deviation in `implementation-log.md` at I8.
9. **I8 final gate runs before the commit, on the identical tree.** IC-2 says the I8 edit "is its own commit,
   followed by the final gate run". Under the one-increment-per-turn mechanism, the Developer runs the
   final gate (I8.3) on the working tree that contains exactly the I8 edit. The orchestrator then commits
   that same tree, and its end check confirms that only ` M app/build.gradle` is pending. The committed
   tree is byte-identical to the gated tree, so this is equivalent to "commit, then gate". **Note for QA:**
   accept this ordering, and optionally re-run
   `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification` on the I8
   commit as confirmation.

## Commit Mechanism for IC-2: One Increment per Developer Turn

**Decision (orchestrator, plan-review iteration 2).** Snapshot-and-replay is dropped. The Developer does
exactly **one increment per turn**: I0, I1, …, I8, so nine turns. The orchestrator commits between turns.
The Developer **never** runs `git commit`, `git add`, `git stash`, `git branch`/`checkout -b`, or
`git worktree`. The only git write the Developer may make is the explicit-path revert in the
[failure procedure](#mid-increment-gate-failure-developer).

### Developer: each turn

1. **Start check.** `git status --porcelain --untracked-files=all -- app README.md CHANGES sdlc/artifacts/engagement/code-location.md metadata gradle`
   must be **empty**, meaning every previous increment was committed. If it is not empty, stop and report
   without editing. Record `git rev-parse HEAD` in the log.
2. Perform exactly the steps of the current increment. Edit only the paths on that increment's expected
   list (below).
3. Run the increment's gate. Every gate log ends with `exit=<code>`.
4. **End check.** Run `git status --porcelain --untracked-files=all -- app README.md CHANGES sdlc/artifacts/engagement/code-location.md metadata gradle`.
   Its output must equal the increment's expected list exactly, path for path and status for status. If it
   does not, stop.
5. Append the increment section to `sdlc/artifacts/build/sprints/sprint-018/U-061/implementation-log.md`,
   and write new evidence under `…/U-061/evidence/`.
6. **Stop and return this hand-off**, then do nothing else:

```yaml
handoff:
  increment: "I3"
  gate: {command: "<exact command>", log: "evidence/I3-gates.log", exit: 0, junit: "tests=682 failures=0 errors=0 skipped=2"}
  product_paths:            # verbatim `git status --porcelain --untracked-files=all -- <product pathspec>`
    - " D app/src/main/java/com/zegoggles/smssync/activity/donation/DonationActivity.java"
    - "..."
  workspace_paths:          # verbatim porcelain for sdlc/artifacts/build/sprints/sprint-018/U-061 (log and evidence only)
    - "?? sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/I3-gates.log"
  proposed_commit_message: "U-061 I3: delete the donation package, its tests and Consts.Billing"
  escalations: []           # any stop condition hit, otherwise empty
```

### Orchestrator: between turns

1. Run `git diff --cached --name-only`. It must be empty, so that nothing unrelated is staged.
2. Run `git status --porcelain --untracked-files=all -- app README.md CHANGES sdlc/artifacts/engagement/code-location.md metadata gradle`.
   It must equal both the hand-off's `product_paths` and the expected list below. Reject the increment if
   any path is outside the list, any path under `metadata/play/**` or `gradle/` appears, or
   `gradle/verification-metadata.xml` changed.
3. Stage **by explicit path only**: `git add -A -- <each product path in the list> sdlc/artifacts/build/sprints/sprint-018/U-061/implementation-log.md sdlc/artifacts/build/sprints/sprint-018/U-061/evidence`.
   `-A` on an explicit path records deletions.
4. Commit with the proposed message, adding the attribution lines from your system reminder. Then check that
   `git status --porcelain -- <product pathspec>` is empty.
5. Resume the Developer for the next increment.

**Rules for the orchestrator for the life of this story:**
- Never commit any of the 38 product paths, or an IC-9 test, except in the commit for the increment that
  changed it.
- Every other commit (artifact, plan, review, AC-10/IC-14 records, sprint plan) must be **path-limited**.
  Never use `git add -A`, `git add .`, `git add sdlc`, `git add sdlc/artifacts` or `git commit -a`. These
  could sweep in `sdlc/artifacts/engagement/code-location.md` or another story file.
- I8 must be committed before the code-review gate runs, so reviewers see the nine commits (I0 to I8;
  I0 holds only log and evidence) and QA's HEAD-based `git grep` for AC-7 is meaningful.

### Expected file list per increment (porcelain, product pathspec)

`M` = modified, `D` = deleted, `??` = new. Paths are relative to the repository root.

| Inc | Expected `git status --porcelain` lines (product pathspec) | Count |
|-----|----------------------------------------------------------|-------|
| I0 | *(none)* | 0 |
| I1 | ` M app/src/test/java/com/zegoggles/smssync/activity/fragments/EnrollmentDialogThemeTest.java` | 1 |
| I2 | ` M app/src/main/AndroidManifest.xml`; ` M app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java`; ` M app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java`; ` M app/src/main/res/xml/preferences.xml` | 4 |
| I3 | ` M app/src/main/java/com/zegoggles/smssync/Consts.java`; ` D app/src/main/java/com/zegoggles/smssync/activity/donation/DonationActivity.java`; ` D app/src/main/java/com/zegoggles/smssync/activity/donation/DonationListFragment.java`; ` D app/src/main/java/com/zegoggles/smssync/activity/donation/Sku.java`; ` D app/src/test/java/com/zegoggles/smssync/activity/donation/DonationActivityTest.java`; ` D app/src/test/java/com/zegoggles/smssync/activity/donation/SkuTest.java` | 6 |
| I4 | ` M app/src/main/res/values/strings.xml` and ` M app/src/main/res/values-<q>/strings.xml` for q in ca, da, de, fr, gl, gr, hu, it, ko, nl, pl, pt-rPT, ru, sk, sq, sr, sv, tr, uk, zh-rCN, zh-rTW | 22 |
| I5 | ` M app/build.gradle`; ` M app/lint-baseline.xml` | 2 |
| I6 | ` M app/build.gradle`. Only if IC-9 is triggered, also `?? app/src/test/java/com/zegoggles/smssync/<package>/<Name>Test.java` (one or more, declared in the hand-off with the escalation) | 1 (+N) |
| I7 | ` M CHANGES`; ` M README.md`; ` M sdlc/artifacts/engagement/code-location.md` | 3 |
| I8 | ` M app/build.gradle` | 1 |

Totals: 33 distinct edited paths and 5 deleted paths, which is 38 (build.gradle is edited in I5, I6 and
I8). Workspace paths (`implementation-log.md`, `evidence/**`) come on top of these in every increment,
including I0. At planning time (HEAD `21838718`), the product-pathspec porcelain is empty (verified), so
the I0 start check holds today.

| Inc | Proposed commit message (first line) |
|-----|--------------------------------------|
| I0 | `U-061 I0: baseline evidence (no product change)` |
| I1 | `U-061 I1: re-host EnrollmentDialogThemeTest on a nested ThemeActivity subclass` |
| I2 | `U-061 I2: remove donation entry points (settings, MainSettings, Keys.DONATE, manifest)` |
| I3 | `U-061 I3: delete the donation package, its tests and Consts.Billing` |
| I4 | `U-061 I4: remove donation strings from all 22 strings.xml files` |
| I5 | `U-061 I5: remove the Play Billing dependency and its stale lint-baseline entry` |
| I6 | `U-061 I6: retire the activity.donation JaCoCo rule` |
| I7 | `U-061 I7: update README, CHANGES and code-location for the donation removal` |
| I8 | `U-061 I8: record the re-measured root-package coverage` |

### Mid-increment gate failure (Developer)

1. Fix within the current increment, touching only paths on that increment's expected list. In I6 only, the
   IC-9 route may add a characterization test, which must then be declared. Never change a test assertion,
   lower a floor, or touch a path outside the list.
2. If the fix is not possible within those limits, revert this increment and stop:
   1. Take the current `git status --porcelain --untracked-files=all -- $PP`. Split it into tracked paths
      (` M`/` D`) and new untracked paths (`??`).
   2. Run `git restore --source=HEAD --staged --worktree -- <only the  M and  D paths>`. This also brings
      deleted files back. **Never pass a `??` path to `git restore`.** It aborts on an untracked pathspec and
      then restores nothing.
   3. `rm` each `??` path. In practice this can only be an IC-9 test added in I6.
   4. Confirm that `git status --porcelain --untracked-files=all -- $PP` is empty.
   5. Record "ESCALATION: <increment> gate failure (reverted)" with the log path in
      `implementation-log.md`. Return a hand-off with `gate.exit != 0`, empty `product_paths`, the reverted
      path list and the escalation.

   Previously committed increments stay as they are.
3. **Orchestrator handling of a reverted increment.** Keep the failure record. Make a path-limited commit
   of `sdlc/artifacts/build/sprints/sprint-018/U-061/implementation-log.md` and that increment's new
   `…/U-061/evidence/` files, and no product path. Use a message such as
   `U-061 I<k>: reverted after gate failure (<one-line reason>); no product change`. A retry of the same
   increment appends to the same `implementation-log.md` and writes new evidence names (for example
   `I6-gates-retry1.log`). The retry's eventual product commit uses the normal I<k> message.

## Environment and Conventions (Developer)

- Run every command from the repository root, using the absolute repo path in each tool call's `cd`. Repeat
  this preamble in every shell:

```bash
unset MSYS_NO_PATHCONV        # NEVER set for ./gradlew
W=sdlc/artifacts/build/sprints/sprint-018/U-061; E=$W/evidence; T=$E/tools
BASE=218387187371237b288ca60db390441b6e66da80
PP="app README.md CHANGES sdlc/artifacts/engagement/code-location.md metadata gradle"   # product pathspec for porcelain checks
BT="$LOCALAPPDATA/Android/Sdk/build-tools/36.0.0"; AAPT2="$BT/aapt2"; DEXDUMP="$BT/dexdump"
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb"; EMU="$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe"
INIT=$T/junit-xml.init.gradle
SCRATCH=<a directory outside the repository, e.g. the session scratchpad>
```

- **JDK:** no `JAVA_HOME` export is needed, because the daemon runs on JDK 25 via
  `gradle/gradle-daemon-jvm.properties`. Exporting the JBR 17 per `.claude/CLAUDE.md` is harmless (it
  affects the launcher only). AC-5 evidence is the `U061-DAEMON-JVM: java.version=25…` line the init
  script prints into every gate log.
- **Gate log idiom:** `./gradlew -I "$INIT" --console=plain <tasks> > "$E/<name>.log" 2>&1; echo "exit=$?" >> "$E/<name>.log"`.
  Before every run that counts tests, run `rm -rf app/build/test-results/testDebugUnitTest`. It is
  untracked output, and removing it means stale XML from deleted test classes cannot inflate totals.
- **Standard increment gate (G):**
  `rm -rf app/build/test-results/testDebugUnitTest; ./gradlew -I "$INIT" --console=plain :app:assembleDebug :app:testDebugUnitTest > "$E/I<k>-gates.log" 2>&1; echo "exit=$?" >> "$E/I<k>-gates.log"; python -I -X utf8 $T/junit_totals.py app/build/test-results/testDebugUnitTest > "$E/I<k>-junit-totals.txt"`.
- **Helper tools.** Create them with the **Write tool**, not a bash heredoc. In this environment a heredoc
  collapsed `\\` to `\` and silently broke regexes. Tools go in `$T/` as text evidence, so QA can re-run
  them. Full source is in [Appendix A](#appendix-a--helper-tools-create-verbatim-with-the-write-tool). Run
  Python as `python -I -X utf8 <script>`.
- **Evidence is text only.** No screenshots, no account names, and no raw UI dumps in the repository (the
  IMAP summary can show an account). Line numbers are at `BASE`. They drift after each increment, so locate
  edits by symbol and use line numbers only to confirm.
- **Device commands:** give `MSYS_NO_PATHCONV=1` as a per-command prefix only, on `adb` calls that carry an
  absolute device path. Never export it.

## Implementation Steps

### I0 — Baseline capture and device precondition (no product edit) — Turn 1

1. Run the start check. Then check
   `git diff --quiet "$BASE" HEAD -- app build.gradle gradle README.md CHANGES sdlc/artifacts/engagement/code-location.md`.
   If it fails, the code changed under the plan: stop. Record `git rev-parse HEAD` (artifact commits may
   have moved it past `BASE`; that is expected). Create `$E/` and `$T/`, and write the Appendix A tools.
2. `@Test` count: `git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test" > $E/before-test-count.txt`.
   The expected count is 697. If it differs, IC-12 applies: the target becomes I0 − 15. Do not reuse 697.
3. Baseline build (this is also the I0 gate):
   `rm -rf app/build/test-results/testDebugUnitTest; ./gradlew -I "$INIT" --console=plain clean :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification > $E/before-gates.log 2>&1; echo "exit=$?" >> $E/before-gates.log`.
   It must exit 0, which shows the three gates are green. Then run
   `./gradlew --console=plain :app:processDebugManifest :app:processReleaseManifest >> $E/before-gates.log 2>&1` (up to date).
4. Copy the evidence:
   - `junit_totals.py` writes `$E/before-junit-totals.txt`. Expected: `tests=697 failures=0 errors=0 skipped=2`.
   - Copy `app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml` to `$E/before-jacoco.xml`, then
     `jacoco_pkgs.py` writes `$E/before-jacoco-packages.txt`.
   - Copy `app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml` to
     `$E/before-merged-debug.xml`, and the release one (`…/release/processReleaseManifest/…`) to
     `$E/before-merged-release.xml`.
   - `ls app/build/outputs/apk/release/` (expected `app-release-unsigned.apk`). Then
     `"$AAPT2" dump xmltree --file AndroidManifest.xml app/build/outputs/apk/debug/app-debug.apk > $E/before-manifest-debug.txt`,
     and the same for the release APK into `$E/before-manifest-release.txt`.
   - `ls app/build/outputs/mapping/release/`. Copy `mapping.txt` to `$E/before-mapping.txt` and
     `configuration.txt` to `$E/before-configuration.txt`.
   - Kept names: `grep -oE '^([A-Za-z0-9_.$]+) -> \1:' $E/before-mapping.txt | sed 's/ -> .*//' | grep -E '^(com\.zegoggles\.smssync|com\.fsck\.k9|com\.google\.crypto\.tink|androidx\.work)\.' | LC_ALL=C sort > $E/kept-before.txt`.
     The file must contain `com.zegoggles.smssync.activity.donation.DonationActivity`.
   - Mapping prefix count (about 965 expected; informational, shows the check detects the names):
     `grep -cE '^(com\.android\.billingclient|com\.android\.vending\.billing|com\.google\.android\.(gms|datatransport)|com\.google\.firebase\.encoders|com\.zegoggles\.smssync\.activity\.donation)\.' $E/before-mapping.txt > $E/before-mapping-prefix-count.txt`.
   - Dependency trees: `./gradlew -q :app:dependencies --configuration debugRuntimeClasspath > $E/before-deps-debugRuntime.txt 2>&1`, and the same for `releaseRuntimeClasspath`.
   - Dex classes (AC-4 baseline). First try
     `"$LOCALAPPDATA/Android/Sdk/tools/bin/apkanalyzer" dex packages --defined-only app/build/outputs/apk/debug/app-debug.apk`
     and record the result (a JAXB failure is expected). Then use the fallback:
     `rm -rf "$SCRATCH/dex"; mkdir -p "$SCRATCH/dex"; unzip -q -o app/build/outputs/apk/debug/app-debug.apk 'classes*.dex' -d "$SCRATCH/dex"; for d in "$SCRATCH"/dex/classes*.dex; do "$DEXDUMP" "$d" | grep "Class descriptor"; done | sed -E "s/.*'L(.*);'.*/\1/" | LC_ALL=C sort -u > $E/before-dex-classes.txt`.
     In `$E/before-dex-counts.txt`, record `grep -c '^com/android/billingclient/'` (expected 190) and
     `grep -c '^com/zegoggles/smssync/activity/donation/'` (expected 16).
   - APK sizes (informational): `stat -c '%s %n' app/build/outputs/apk/*/*.apk > $E/before-apk-sizes.txt`.
5. **Third-party-integration precondition for IC-11. Read-only and fail closed. Write to `$E/I0-device-precondition.txt`.**
   1. If `"$ADB" devices` shows no emulator, start `"$EMU" -avd Pixel_9_Pro` as a background command.
      Read the serial from `"$ADB" devices`; serials change between boots. Then run
      `"$ADB" -s $SER wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 2; done'`.
   2. `"$ADB" -s $SER shell getprop ro.build.version.sdk` must be `36`. Then run
      `"$ADB" -s $SER shell pm list packages com.zegoggles.smssync`.
   3. Classify the state:
      - **Not installed** (empty list): no preferences exist, so the default is `false` (preferences.xml:213). State: **OFF**.
      - **Installed:** run `"$ADB" -s $SER shell run-as com.zegoggles.smssync ls shared_prefs; echo "rc=$?"`.
        - If `rc` is not 0, or the output contains `run-as:`, the state is **UNKNOWN**.
        - If `com.zegoggles.smssync_preferences.xml` is not listed, the state is **OFF** (defaults only).
        - If it is listed, run `"$ADB" -s $SER shell run-as com.zegoggles.smssync cat shared_prefs/com.zegoggles.smssync_preferences.xml > "$SCRATCH/prefs.xml"; echo "rc=$?"`.
          `rc` must be 0 and the file must contain `<map`; otherwise the state is **UNKNOWN**. Then run
          `grep -o 'name="third_party_integration" value="[a-z]*"' "$SCRATCH/prefs.xml"`. A match of
          `value="true"` means **ON**. A match of `value="false"`, or no match, means **OFF**.
      - Record only the state and the evidence lines: rc values, file listed yes/no, the single matched
        attribute. Delete `$SCRATCH/prefs.xml`, which may contain account data. **Never** change the
        setting or any other data.
   4. If the state is **ON** or **UNKNOWN**, put an escalation in the I0 hand-off: "IC-11 precondition:
      third-party integration ON/UNKNOWN on Pixel_9_Pro; the Developer will not change it". Include this
      information for the product owner: the setting gates only the inbound `com.zegoggles.smssync.BACKUP`
      broadcast (`Preferences.java:179`). IC-11 sends no broadcast and only opens Settings, so a waiver is
      low-risk. The story still requires the setting to be off, so the product owner either switches it off
      in the app or waives the precondition before I8. The build continues meanwhile.
   5. If the Developer started the emulator, run `"$ADB" -s $SER emu kill`. Otherwise leave it running.
6. Log I0 and return the hand-off. Expected `product_paths` is empty. The orchestrator commits the log and
   evidence (including `tools/`) with the I0 message.

### I1 — Re-host `EnrollmentDialogThemeTest` (IC-3, AC-6) — Turn 2

File: `app/src/test/java/com/zegoggles/smssync/activity/fragments/EnrollmentDialogThemeTest.java` (LF).

1. Line 9: replace `import com.zegoggles.smssync.activity.donation.DonationActivity;` with
   `import com.zegoggles.smssync.activity.ThemeActivity;`.
2. Lines 113-117: replace the block with:
   ```java
           // Obtain an AppCompat-themed Activity from Robolectric.
           // HostActivity (nested below) extends ThemeActivity extends AppCompatActivity.
           // The AppCompat theme comes from ThemeActivity.onCreate(), which calls
           // setTheme(new Preferences(this).getAppTheme()), not from the manifest: the AGP 9
           // unit-test manifest declares no app components and no application theme.
           HostActivity activity = Robolectric.buildActivity(HostActivity.class).create().get();
   ```
3. Javadoc lines 164-167: replace with:
   ```java
        * <p>Uses {@link HostActivity} (which extends {@code ThemeActivity → AppCompatActivity})
        * as the host. The AppCompat theme comes from {@code ThemeActivity.onCreate()}, which calls
        * {@code setTheme(new Preferences(this).getAppTheme())} (a {@code Theme.AppCompat}
        * descendant), not from the manifest, so the AppCompat delegate finds a valid theme.
   ```
4. Lines 179 and 244: replace each with
   `        HostActivity activity = Robolectric.buildActivity(HostActivity.class).create().get();`.
5. Before the final `}` of the class, after `loadCert`, add:
   ```java

       // -------------------------------------------------------------------------
       // Host activity
       // -------------------------------------------------------------------------

       /**
        * Test-only AppCompat host: a concrete subclass of the production {@code ThemeActivity}
        * (which is abstract). It is not registered in any manifest; Robolectric launches it
        * undeclared. It is nested so that it compiles to
        * {@code EnrollmentDialogThemeTest$HostActivity} and stays under the JaCoCo
        * {@code *Test*} class-file filter.
        */
       public static class HostActivity extends ThemeActivity {}
   ```
   Do **not** write the glob `**/*Test*.*` in the javadoc; its `*/` would close the comment.
6. Change nothing else, including assertions. Self-check: `git diff -U0` on the file shows only the import,
   the two comment blocks, the 3 host lines and the added class. `grep -c "DonationActivity"` on the file
   gives 0, and `grep -c "@Test"` gives 3.
7. **IC-3 isolated run:**
   `rm -rf app/build/test-results/testDebugUnitTest; ./gradlew -I "$INIT" --console=plain :app:testDebugUnitTest --tests 'com.zegoggles.smssync.activity.fragments.EnrollmentDialogThemeTest' > $E/I1-isolated-theme-test.log 2>&1; echo "exit=$?" >> $E/I1-isolated-theme-test.log`.
   Then `junit_totals.py` must give `files=1 tests=3 failures=0 errors=0 skipped=0`
   (`$E/I1-isolated-junit-totals.txt`). Also record:
   - `find app/build/intermediates -name 'EnrollmentDialogThemeTest$HostActivity.class'`: hits under
     test-class directories only.
   - `grep -rn "HostActivity" app/src/main`: empty.
8. Run gate G. It must exit 0, with totals tests=697 (I0), failures 0, errors 0, skipped 2. Run the end
   check, log, and hand off.

### I2 — Remove the entry points (AC-2, AC-12) — Turn 3

1. `app/src/main/res/xml/preferences.xml` (CRLF): delete lines 318-327, the blank line and the
   `<Preference android:key="donate">…</Preference>` block. Line 317 `    </PreferenceScreen>` is then
   followed directly by `</PreferenceScreen>`.
2. `app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java` (CRLF): delete lines 5, 14,
   15, 22, 24, 75 and 163-182 (deviation 2; the method and its preceding blank line). `onResume()` becomes
   `super.onResume();`, a blank line, `updateAutoBackupPreferences();`,
   `addPreferenceListener(ENABLE_AUTO_BACKUP.key);`. Keep the `Preference`, `ArrayList` and `List` imports.
3. `app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java`: delete line 101
   `        DONATE("donate"),`. Do not touch `reset()` (lines 255-260).
4. `app/src/main/AndroidManifest.xml`: delete line 129 `<activity android:name=".activity.donation.DonationActivity"/>` only.
5. Checks:
   - `grep -n 'key="donate"' app/src/main/res/xml/preferences.xml`: empty.
   - `grep -rn "DONATE\|DonationActivity" app/src/main --include=*.java --include=*.xml | grep -v /activity/donation/`: empty.
   - **AC-12 evidence:** record that `git grep -n '"donate"\|DONATE' $BASE -- app/src/main` hits only
     `Preferences.java:101`, `MainSettings.java:24,172` (a `findPreference` read) and
     `preferences.xml:320`. No `putX`/`edit()` uses the key, and `reset()` removes only
     `SMS_DEFAULT_PACKAGE_CHANGE_SEEN` and `SMS_DEFAULT_PACKAGE`. This is reviewed-code evidence.
6. Run gate G. It must exit 0 with tests=697; the donation tests still run against the undeclared
   `DonationActivity`. Run the end check, log, and hand off.

### I3 — Delete the donation code, its tests and `Consts.Billing` together — Turn 4

1. `rm -r app/src/main/java/com/zegoggles/smssync/activity/donation app/src/test/java/com/zegoggles/smssync/activity/donation`.
2. `app/src/main/java/com/zegoggles/smssync/Consts.java` (CRLF): delete lines 35-50, the blank line and the
   `Billing` class. Line 34 is then followed by `}`.
3. **IC-10:** `grep -rn "DonationActivity\|Consts.Billing\|Keys.DONATE\|DONATE" app/src` gives 0 results.
   Save the command and its empty output to `$E/I3-ic10-grep.txt`.
4. Run gate G. It must exit 0 with `tests = I0 − 15` (682), failures 0, errors 0, skipped 2. Also record the
   working-tree count `grep -rh "@Test" app/src/test --include=*.java --include=*.kt | grep -c "@Test"`
   (expected I0 − 15). Run the end check, log, and hand off.

### I4 — Strings, all 22 files, by `name` (AC-1 items 7, IC-13) — Turn 5

1. Dry run: `python -I -X utf8 $T/strip_strings.py app/src/main/res > $E/I4-strings-dryrun.txt`. Every folder
   must report `OK`, and the last line must be `files=25 expected_total=235 ok=True`. Review the listed
   lines: each must be one of the 10 named `<string>` lines or one of the two exact comments.
2. Apply: `python -I -X utf8 $T/strip_strings.py app/src/main/res --write > $E/I4-strings-apply.txt`.
3. Proofs, written to `$E/I4-strings-proof.txt`:
   - `grep -rniE "billing|donat" app/src/main/res` gives 0.
   - `grep -rn "purchase errors" app/src/main/res` gives 0 (IC-13).
   - `git diff --numstat -- 'app/src/main/res/values*/strings.xml'` lists 22 files, all with 0 added, and
     deletions that match the per-file table and total 235.
   - `git diff -U0 -- 'app/src/main/res/values*/strings.xml' | grep '^+' | grep -vc '^+++'` gives 0.
   - `git diff -U0 -- 'app/src/main/res/values*/strings.xml' | grep '^-' | grep -v '^---' | grep -vcE 'name="(donate|donate_summary|ui_dialog_donate_message|ui_donation_success_message|ui_donation_failure_message|donation_error_iab_unavailable|donation_error_unavailable|donation_error_canceled|donation_error_already_owned|donation_unspecified_error)"|<!-- (donation related|purchase errors) -->'` gives 0.
   - `head -c3 app/src/main/res/values-zh-rTW/strings.xml | od -c` still shows the BOM `357 273 277`.
4. Run gate G. It must exit 0 (682). Run the end check, log, and hand off.

### I5 — Dependency, BuildConfig comment, lint baseline (AC-4, IC-5, AC-12) — Turn 6

1. `app/build.gradle` (LF):
   - Line 43 becomes `        // app uses BuildConfig.DEBUG in App.java and auth/OAuth2Client.java.` (deviation 1).
   - Delete line 132 `    implementation 'com.android.billingclient:billing:7.1.1' // U-028 …`.
2. `app/lint-baseline.xml` (CRLF): delete lines 114-124, the billing 2.1.0 `GradleDependency` entry and the
   blank line after it. `git diff --numstat app/lint-baseline.xml` gives `0 11`, and
   `grep -c billing app/lint-baseline.xml` gives 0.
3. **D4a drift guard (IC-5):**
   - Run `./gradlew -q :app:dependencies --configuration debugRuntimeClasspath > $E/I5-deps-debugRuntime.txt 2>&1`
     and the same for `releaseRuntimeClasspath`.
   - Run `python -I -X utf8 $T/deps_drift.py $E/before-deps-debugRuntime.txt $E/I5-deps-debugRuntime.txt > $E/I5-drift-debug.txt; echo "exit=$?" >> $E/I5-drift-debug.txt`,
     and the same for release.
   - `grep -c com.android.billingclient $E/I5-deps-*Runtime.txt` gives 0 (AC-4 part 1).
   - **On exit 1, stop and escalate.** Do not add `constraints {}` or any pin. Still run gate G so the
     compile state is known. Put the drift table in the log under "ESCALATION: D4a", and return the
     hand-off with the two I5 files listed and the escalation. The orchestrator decides with the product
     owner whether to commit I5 and how to continue. A version pin is not pre-approved.
4. Run gate G. It must exit 0 (682). Run the end check, log, and hand off.

### I6 — Retire the JaCoCo rule (AC-8, IC-9) and the binding coverage read — Turn 7

1. `app/build.gradle` (line numbers are shifted −1 by I5):
   - In the Rule 5 comment (now line 410), change
     `        // MEASURED: activity=15.4%, activity.auth=0%, activity.donation=33.5%,` to
     `        // MEASURED: activity=15.4%, activity.auth=0%,`. The next line is unchanged.
   - Delete the donation rule with its comment (now lines 434-443): from
     `        // activity.donation: DonationActivity coverage.` through that rule's closing `        }`.
   - Self-check: `git diff -U0 app/build.gradle` shows only the Rule 5 edit and 10 deleted lines.
     `git diff app/build.gradle | grep -E '^[-+].*minimum'` shows only `- … minimum = 0.30 …`.
2. Measurement and gate:
   `rm -rf app/build/test-results/testDebugUnitTest; ./gradlew -I "$INIT" --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:jacocoTestCoverageVerification > $E/I6-gates.log 2>&1; echo "exit=$?" >> $E/I6-gates.log`.
   It must exit 0. Write the JUnit totals (682 expected). Copy the XML to `$E/I6-jacoco.xml` and run
   `jacoco_pkgs.py` into `$E/I6-jacoco-packages.txt`. Check:
   - The `activity.donation` package is absent.
   - Root is at least 50.0% (prediction 58.6%).
   - activity ≥ 10%, activity.fragments ≥ 10%, receiver ≥ 50.0%, preferences ≥ 65%, utils ≥ 10%.
   - `grep -c "HostActivity" $E/I6-jacoco.xml` gives 0.
3. **AC-1 (first full assertion):** `ls app/src/main/java/com/zegoggles/smssync/activity/donation/` fails,
   and `grep -rniE "billing|donat" app/src/main app/build.gradle` gives 0 results (`$E/I6-ac1-grep.txt`).
4. **IC-9 breach route.** If any rule fails, do not lower a floor, add an exclusion, or rename a helper.
   Add a characterization test of real behavior in the breached package, as a new file named `*Test.java`
   under `app/src/test/java/com/zegoggles/smssync/<package>/`. Re-run step 2. Declare each new file in the
   hand-off (`??`) together with "ESCALATION: IC-9 shortfall" and the new test-count arithmetic (682 + N).
   If this is not achievable, follow the failure procedure.
5. Run the end check, log, and hand off.

### I7 — Docs (AC-11, D7) — Turn 8

1. `README.md` (CRLF): delete line 241 (`  * Google Play billing service - used for in-app donations`) first.
   Then delete lines 43-45. Lines 40-41 stay.
2. `CHANGES` (CRLF): after line 5 `  * Updated Swedish localization (#890)`, insert
   `  * Removed the in-app donation feature and the in-app billing (Play Billing) dependency`. Do not touch
   lines 23, 120, 138 or 168.
3. `sdlc/artifacts/engagement/code-location.md` (LF): delete line 37
   (``- `com.android.billingclient:billing` — in-app donations``).
4. `git diff --stat -- metadata/play sdlc/artifacts/engagement/endpoints.md sdlc/artifacts/engagement/systems.md` gives empty.
5. Run gate G. It must exit 0, because IC-2 requires a gate after every increment. Run the end check, log,
   and hand off.

### I8 — The single edit, final verification, final gate, then after-evidence — Turn 9

Order: edit, then the greps, then the final gate, then evidence on the unchanged tree. After I8.1, **no
tracked file may change.** If any later check fails, stop and escalate. Do not edit to make it pass.

**I8.1 The single edit (AC-8 third bullet).** The root value comes from `$E/I6-jacoco-packages.txt` (after
`Consts.Billing` was removed; I7 changed no code). In `app/build.gradle`, insert one line in the Rule 4
header comment, directly after `        // U-060 re-measured: 60.0% (18/30) - …` (originally line 399, now 398):

`        // U-061 re-measured: <pct>% (<covered>/<total>) - the nested SKU-constants class was removed from Consts.`

The line must contain neither "billing" nor "donat" in any case (B1). Do not edit `minimum = 0.50` or its
trailing comment.

**I8.2 Final verification greps, after the edit and before the final gate (B1).** Write to `$E/I8-final-greps.txt`:
- AC-1: `ls app/src/main/java/com/zegoggles/smssync/activity/donation/` fails, and
  `grep -rniE "billing|donat" app/src/main app/build.gradle` gives 0 results.
- IC-10: `grep -rn "DonationActivity\|Consts.Billing\|Keys.DONATE\|DONATE" app/src` gives 0.
- IC-13: `grep -rn "purchase errors" app/src/main/res` gives 0.

Any hit means stop.

**I8.3 Final gate (IC-2, AC-5):**
`rm -rf app/build/test-results/testDebugUnitTest; ./gradlew -I "$INIT" --console=plain clean :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification > $E/final-gates.log 2>&1; echo "exit=$?" >> $E/final-gates.log`.
It must exit 0, and the log must show `U061-DAEMON-JVM: java.version=25…`. `$E/final-junit-totals.txt` must
give `tests=682 failures=0 errors=0 skipped=2` (682 + N if IC-9 added tests).

**I8.4 After-evidence builds on the same tree (incremental; no clean, no edit):**
`./gradlew -I "$INIT" --console=plain :app:assembleRelease :app:jacocoTestReport :app:processDebugManifest :app:processReleaseManifest > $E/after-evidence-build.log 2>&1; echo "exit=$?" >> $E/after-evidence-build.log`.
It must exit 0. Then capture, with the same commands as I0.4 and an `after-` prefix:
- `after-jacoco.xml` and `after-jacoco-packages.txt`. The root value must equal the I6 value written in
  I8.1. If it differs, stop and report; do not edit.
- `after-merged-{debug,release}.xml` and `after-manifest-{debug,release}.txt`.
- `after-mapping.txt`, `after-configuration.txt` and `kept-after.txt`.
- `after-deps-{debug,release}Runtime.txt`.
- `after-dex-classes.txt` and `after-dex-counts.txt` (both counts 0).
- `after-apk-sizes.txt`.

**I8.5 Manifest equivalence (IC-6, AC-3).** Write to `$E/I8-manifest-diffs.txt`:
```bash
diff $E/before-merged-debug.xml   $E/after-merged-debug.xml
diff $E/before-merged-release.xml $E/after-merged-release.xml
norm() { sed -E 's/ ?\(line=[0-9]+\)//g; s/@0x7f[0-9a-f]{6}/@0x7fRESID/g' "$1"; }
diff <(norm $E/before-manifest-debug.txt)   <(norm $E/after-manifest-debug.txt)
diff <(norm $E/before-manifest-release.txt) <(norm $E/after-manifest-release.txt)
```
**Pass** requires all of the following:
- Each of the four diffs is non-empty.
- Every hunk is a `d` (deletion), and no line starts with `>`.
- The removed lines group into exactly these elements: the BILLING uses-permission; `<queries>` with both
  intents; the `billingclient.version` meta-data; `ProxyBillingActivity` and `ProxyBillingActivityV2`;
  `.activity.donation.DonationActivity`; `GoogleApiActivity`; the `com.google.android.gms.version`
  meta-data; `TransportBackendDiscovery` with its `backend:` meta-data; `JobInfoSchedulerService`;
  `AlarmManagerSchedulerBroadcastReceiver`.

Blank, attribute and child lines count as part of their element. Annotate each hunk in the log. Any other
line means stop: explain it or escalate it.

**I8.6 Unchanged invariants (IC-7).** Write to `$E/I8-ic7.txt`, for both `after-manifest-*.txt` files:
- `package="com.zegoggles.smssync"` and `versionCode(0x0101021b)=1602`.
- In `grep -n -A8 'receiver.BackupBroadcastReceiver'`: `exported(0x01010010)=true`, no
  `permission(0x01010006)` inside that element, and the action `com.zegoggles.smssync.BACKUP`
  (CNTR-MODERNIZATION-005).
- `foregroundServiceType(0x01010599)=0x00000001` on `SystemForegroundService`.
- `grep -c InitializationProvider` gives 0.
- `networkSecurityConfig(0x01010527)` and `fullBackupContent(0x010104eb)` are present.
- `grep -c 'DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"'` gives 2.
- `INTERNET` and `ACCESS_NETWORK_STATE` are present.

**I8.7 Release and APK checks (IC-8, AC-4).** Write to `$E/I8-ic8.txt`:
1. The mapping prefix count `grep -cE '<pattern as I0>' $E/after-mapping.txt` gives 0. The I0 count must
   have been above 0.
2. `grep -c '^com/android/billingclient/' $E/after-dex-classes.txt` and
   `grep -c '^com/zegoggles/smssync/activity/donation/'` both give 0. The I0 counts must have been above 0,
   and `after-dex-classes.txt` must be non-empty.
3. `LC_ALL=C comm -23 $E/kept-before.txt $E/kept-after.txt` prints exactly
   `com.zegoggles.smssync.activity.donation.DonationActivity`. `LC_ALL=C comm -13 …` prints nothing.
   `grep -c com.android.billingclient $E/after-deps-*Runtime.txt` gives 0.
4. `python -I -X utf8 $T/r8_config_diff.py $E/before-configuration.txt $E/after-configuration.txt > $E/I8-r8-config-diff.txt; echo "exit=$?" >> $E/I8-r8-config-diff.txt`
   must exit 0 (read per D6, deviation 4). The script fails closed: it fails on zero removed lines, on any
   added line, on any unattributed removal, and if any of the 7 AC-3 AAPT keeps was not removed.

**I8.8 Remaining checks.** Write to `$E/I8-checks.txt`:
- `deps_drift.py` on `before-` vs `after-deps-*` exits 0.
- `git diff --stat "$BASE" -- gradle/verification-metadata.xml metadata/play app/proguard-rules.pro` is empty.
- `git diff "$BASE" -- app build.gradle settings.gradle gradle.properties gradle gradlew gradlew.bat .github | grep -nE 'dependency-verification=off|lenient'` gives 0. The search is limited to product and build paths on purpose. An unbounded `git diff "$BASE"` would also cover the committed plan, plan-review and story, whose text contains these words, and would stop I8 on a false positive. The plan's other `git diff`/`git grep` checks were audited for the same problem. Each already has a pathspec: I0.1, I0.2, I2.5, I4.3, I5.2, I6.1, I8.8 (verification metadata, `metadata/play`, proguard), and the QA REQ-010 frontmatter check.
- `grep -A8 '<configuration>' gradle/verification-metadata.xml` shows the block unchanged.
- AC-7 arithmetic: the working-tree `@Test` grep gives I0 − 15 (or + N), and the JUnit totals agree. QA
  re-runs the HEAD-based `git grep` after the I8 commit.

**I8.9 Verification-metadata writer, as evidence only (IC-4, AC-9, deviation 8).** This makes no change to
git state:
```bash
CP=C:/Code/u061-vmcheck; PATCH=C:/Code/u061-i8.patch; ROOT=$(pwd); rm -rf "$CP"; mkdir -p "$CP"
git -c core.autocrlf=false archive HEAD | tar -x -C "$CP"            # LF tree at HEAD (I0-I7 committed)
git diff HEAD --binary -- app > "$PATCH"                              # the uncommitted I8 comment
(cd "$CP" && git apply "$PATCH")                                       # patch outside a repo
cp local.properties "$CP/"                                             # SDK location (untracked)
(cd "$CP" && ./gradlew --console=plain --write-verification-metadata sha256 help > "$ROOT/$E/I8-ic4-writer.log" 2>&1; echo "exit=$?" >> "$ROOT/$E/I8-ic4-writer.log")
git show HEAD:gradle/verification-metadata.xml | diff - "$CP/gradle/verification-metadata.xml" > $E/I8-ic4-writer.diff; echo "diff-exit=$?" >> $E/I8-ic4-writer.diff
(cd "$CP" && ./gradlew --stop); rm -rf "$CP" "$PATCH"
```
Classify the result as empty (expected), removal-only, or containing additions, and record it in the log.
The repository file is never changed (IC-4). `--stop` also stops the main repository's daemons, which is
harmless.

**I8.10 On-device check (IC-11, AC-2).** Use AVD `Pixel_9_Pro`, API 36, text evidence only. Never uninstall,
never clear data, never change a setting, take no screenshots, record no account names.
1. Start the emulator if it is not running, as in I0.5.1, and re-run the I0.5.3 classification. The product
   owner may have changed the setting since I0. Write it to `$E/I8-device.txt`. If the state is ON or
   UNKNOWN and no waiver has been recorded by the orchestrator, skip steps 2-5 and escalate. All other I8
   results stand.
2. Run `"$ADB" -s $SER install -r app/build/outputs/apk/debug/app-debug.apk`. If it fails with
   `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, stop IC-11 and escalate. Never uninstall.
3. Set `T0=$("$ADB" -s $SER shell date '+%m-%d %H:%M:%S.000' | tr -d '\r')`, then run
   `"$ADB" -s $SER shell am start -W -n com.zegoggles.smssync/.activity.MainActivity`. Record the `Status: ok` line.
4. Take a dump at the top of the screen. Then swipe up three times (`wm size` gives W×H;
   `input swipe W/2 0.8H W/2 0.2H 300`) and take a bottom dump. To take each dump, run
   `MSYS_NO_PATHCONV=1 "$ADB" -s $SER shell uiautomator dump /data/local/tmp/u061-ui.xml` and then
   `MSYS_NO_PATHCONV=1 "$ADB" -s $SER exec-out cat /data/local/tmp/u061-ui.xml > "$SCRATCH/ui-top.xml"`
   (`ui-bottom.xml` for the second). Then run:
   - `python -I -X utf8 $T/ui_titles.py "$SCRATCH/ui-top.xml" >> $E/I8-device.txt; echo "exit=$?" >> $E/I8-device.txt`.
     This needs at least one title and 0 `donat` nodes.
   - `python -I -X utf8 $T/ui_titles.py "$SCRATCH/ui-bottom.xml" --expect-last "Auto backup settings" >> $E/I8-device.txt; echo "exit=$?" >> $E/I8-device.txt`.
     The last title must equal `Auto backup settings`, the final `PreferenceScreen`.

   Both runs must exit 0.
5. Run `"$ADB" -s $SER shell input keyevent KEYCODE_BACK`. Then check the log for crashes. The check must
   fail if `logcat` itself fails:
   ```bash
   "$ADB" -s $SER logcat -d -T "$T0" > "$SCRATCH/logcat.txt"; rc=$?
   if [ $rc -ne 0 ] || [ ! -s "$SCRATCH/logcat.txt" ]; then echo "LOGCAT FAILED rc=$rc" >> $E/I8-device.txt; else echo "fatal_count=$(grep -c 'FATAL EXCEPTION' "$SCRATCH/logcat.txt")" >> $E/I8-device.txt; fi
   ```
   `fatal_count=0` is required. "LOGCAT FAILED" is an IC-11 failure: retry once, then escalate. Then run
   `MSYS_NO_PATHCONV=1 "$ADB" -s $SER shell rm /data/local/tmp/u061-ui.xml` and delete
   `$SCRATCH/ui-*.xml` and `$SCRATCH/logcat.txt`. Run `emu kill` only if the Developer started the emulator.

**I8.11 Close-out.** Complete `implementation-log.md` with:
- the before and after test counts (AC-7)
- the root value and the five other re-measured JaCoCo values (AC-8)
- the IC-4 result, and deviation 8
- the IC-5 result
- the per-increment table (HEAD at the start of each turn, files, gate exit, totals, proposed message)
- every evidence file, and any escalations

Run the end check. Expected: ` M app/build.gradle` only. Hand off. The orchestrator commits I8 before the
code-review gate.

## Acceptance Criteria Mapping

| AC / IC | Implementation step | Evidence (under `sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/`) |
|---------|---------------------|-------------------|
| AC-1 | I2–I6 (first full at I6.3); **final at I8.2, after the I8 edit** | `I8-final-greps.txt` (authoritative), `I6-ac1-grep.txt`, `I4-strings-proof.txt` |
| AC-2 | I2; IC-11 at I8.10 | `I8-device.txt`; diffs of MainSettings and preferences.xml |
| AC-3 | I0.4 + I8.5 (four diffs) | `before-/after-merged-*.xml`, `before-/after-manifest-*.txt`, `I8-manifest-diffs.txt` |
| AC-4 | I5.3, I8.4, I8.7 | `I5-deps-*`, `after-deps-*`, `after-dex-counts.txt` (dexdump, deviation 3) |
| AC-5 | I8.3 | `final-gates.log` (exit 0, JDK 25 line) |
| AC-6 | I1, I3 | the I1 commit diff, `I1-isolated-*`, and the two deleted test files in the I3 commit |
| AC-7 | I0.2/I0.4, I3.4, I8.3, I8.8 | `before-test-count.txt`, `before-/final-junit-totals.txt`; QA's HEAD `git grep` after the I8 commit |
| AC-8 | I6, I8.1, I8.4 | `I6-/after-jacoco-packages.txt`; diffs of the I6 and I8 commits |
| AC-9 | I8.8, I8.9 | `I8-checks.txt`, `I8-ic4-writer.{log,diff}` |
| AC-10 | Not Developer work (see below) | QA reads the change record content |
| AC-11 | I7 | the I7 commit diff; `metadata/play` diff empty |
| AC-12 | I2.5, I5.2 | log (reviewed-code evidence); lint-baseline numstat `0 11` |
| IC-1 | I0 | all `before-*` files |
| IC-2 | Commit mechanism; one turn and one commit per increment | `I*-gates.log`, hand-offs, nine commits |
| IC-3 | I1.7 | `I1-isolated-theme-test.log`, `I1-isolated-junit-totals.txt` |
| IC-4 | I8.9 (deviation 8) | `I8-ic4-writer.*` |
| IC-5 | I5.3 | `I5-drift-{debug,release}.txt` |
| IC-6 / IC-7 | I8.5 / I8.6 | `I8-manifest-diffs.txt`, `I8-ic7.txt` |
| IC-8 | I8.7 (read per D6, deviation 4) | `I8-ic8.txt`, `I8-r8-config-diff.txt`, `kept-before.txt`, `kept-after.txt` |
| IC-9 | I6.4 (if triggered) | log escalation; hand-off `??` paths |
| IC-10 | I3.3, I8.2 | `I3-ic10-grep.txt`, `I8-final-greps.txt` |
| IC-11 | I0.5 (precondition), I8.10 | `I0-device-precondition.txt`, `I8-device.txt` |
| IC-12 | I0.2 | `before-test-count.txt` |
| IC-13 | I4.3, I8.2 | `I4-strings-proof.txt`, `I8-final-greps.txt` |
| IC-14 | Not Developer work | as AC-10 |

## Files Modified/Created

The Developer may touch exactly these 38 product paths, each only in its increment: 33 edited and 5
deleted. The one exception is a declared IC-9 test in I6. Any other product path means stop.

| # | File | Inc | Action |
|---|------|-----|--------|
| 1 | `app/src/test/java/com/zegoggles/smssync/activity/fragments/EnrollmentDialogThemeTest.java` | I1 | edit |
| 2 | `app/src/main/res/xml/preferences.xml` | I2 | edit |
| 3 | `app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java` | I2 | edit |
| 4 | `app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java` | I2 | edit |
| 5 | `app/src/main/AndroidManifest.xml` | I2 | edit |
| 6-8 | `app/src/main/java/com/zegoggles/smssync/activity/donation/{DonationActivity,DonationListFragment,Sku}.java` | I3 | delete |
| 9-10 | `app/src/test/java/com/zegoggles/smssync/activity/donation/{DonationActivityTest,SkuTest}.java` | I3 | delete |
| 11 | `app/src/main/java/com/zegoggles/smssync/Consts.java` | I3 | edit |
| 12-33 | `app/src/main/res/values/strings.xml` and `values-{ca,da,de,fr,gl,gr,hu,it,ko,nl,pl,pt-rPT,ru,sk,sq,sr,sv,tr,uk,zh-rCN,zh-rTW}/strings.xml` | I4 | edit |
| 34 | `app/build.gradle` | I5, I6, I8 | edit |
| 35 | `app/lint-baseline.xml` | I5 | edit |
| 36 | `README.md` | I7 | edit |
| 37 | `CHANGES` | I7 | edit |
| 38 | `sdlc/artifacts/engagement/code-location.md` | I7 | edit |

The Developer also creates workspace files: `sdlc/artifacts/build/sprints/sprint-018/U-061/implementation-log.md`
and `sdlc/artifacts/build/sprints/sprint-018/U-061/evidence/**` (including `tools/`). Outside the
repository, the Developer creates scratch files (`$SCRATCH`, `C:/Code/u061-vmcheck`, `C:/Code/u061-i8.patch`),
all deleted after use.

These are never touched: `gradle/verification-metadata.xml`, `metadata/play/**`, `app/proguard-rules.pro`,
`values-{cs,es,nb-rNO}/strings.xml`, `ThemeActivity`, `Dialogs`, `BundleBuilder`,
`compat/GooglePlayServices.java`, the story, the sprint plan, and every REQ, DES, CR and CNTR file.

## Stop Conditions

At any of these, halt, record "ESCALATION: …" in the log, and return the hand-off.

1. The start check is not empty, or the I0.1 `git diff --quiet "$BASE" HEAD -- …` fails.
2. A gate fails and cannot be fixed within the increment's list. Use the failure procedure (revert, then report).
3. IC-3: the isolated run is not 3/3 passing.
4. IC-5: `deps_drift.py` exits 1.
5. The end-check porcelain differs from the expected list.
6. IC-6/IC-7: a manifest diff is empty, contains a `>` line or an unlisted removal, or an invariant changed.
7. IC-8: the mapping or dex counts are not 0 (or their I0 counts were 0), the `comm` results are wrong, or
   `r8_config_diff.py` exits 1.
8. The test totals are not I0 − 15 (+ N for IC-9), or there are failures or errors.
9. A JaCoCo breach. Take the IC-9 route; never lower a floor.
10. `strip_strings.py` reports a mismatch.
11. Any hit in the I8.2 greps, or a mismatch between the I8.4 root value and I8.1.
12. Device: the setting is ON or UNKNOWN without a waiver, the install shows a signature mismatch,
    `ui_titles.py` exits non-zero, `logcat` fails twice, or the fatal count is above 0. These stop IC-11
    only; the rest of I8 stands.

## Rollback

- **Within an increment (before the orchestrator commits):** follow the
  [failure procedure](#mid-increment-gate-failure-developer), step 2. Run `git restore` on the tracked
  ` M`/` D` paths only, `rm` the `??` paths, and confirm the porcelain is empty. HEAD always holds the last
  committed increment.
- **After commits:** `git revert` the U-061 increment commits, newest first. Nothing is persisted or
  migrated (AC-12), and verification metadata is unchanged, so a revert restores the prior APK with no
  user-data consequence. The device install is an in-place upgrade. Reinstalling an older debug APK needs
  `-d` (downgrade), and that is the product owner's decision.

## Not Developer Work: AC-10 / IC-14

The orchestrator dispatches the Artifact Librarian / Requirements Analyst in sprint-018. The work must be
complete before the qa-validation gate, and each of its commits must be path-limited (commit mechanism
rules):
- Create a new supersession change record, using the next free id (CR-001..003 exist).
- Append that id to `change_records` of REQ-MODERNIZATION-010 (currently `[]`).
- Append the same id to REQ-MODERNIZATION-018, next to `CR-003`. This is an append, not a replace.
- Add a superseded annotation to DES-MODERNIZATION-010 naming DES-MODERNIZATION-013.

**QA must check the content, not only that the IDs are present:**
- Read the new change record and confirm that it states:
  - the decision date 2026-11-12;
  - the verbatim product-owner decision "remove the donation flow";
  - that REQ-MODERNIZATION-010's intent is superseded by REQ-MODERNIZATION-018;
  - that REQ-MODERNIZATION-010 AC-1..4 are not applicable.
- Confirm that REQ-MODERNIZATION-010's body text is unedited. `git diff 21838718 HEAD -- sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-010-upgrade-play-billing-to-v7.md`
  may touch only the frontmatter block, between the `---` lines.
- Confirm the reciprocal entry on REQ-018 (`CR-003` retained).
- Confirm that the DES-010 annotation names DES-013.

**Orchestrator notes for downstream gates:**
1. Record acceptance of deviation 1 (the `build.gradle:43` comment naming `App.java` and
   `auth/OAuth2Client.java`) in the story's Implementation Notes.
2. Tell QA (and Code Review) that IC-8(4) is read per design D6 (deviation 4), and that deviations 2, 3,
   5, 6 and 8 were ruled correct at plan-review. Also tell QA about deviation 9: the I8 final gate runs on
   the identical tree before the orchestrator's commit.

## Contracts Verification

No interface is introduced or altered (`integration_contracts: []`).
`grep -nicE "billing|donat"` over CNTR-MODERNIZATION-001..008, all approved, gives 0 in each file. The
contract surfaces that the removal regenerates are asserted unchanged:

| Contract | Surface | Checked in |
|----------|---------|------------|
| CNTR-005 | Exported `BACKUP` receiver | I8.6 |
| CNTR-004 | Worker class names in the kept-name set; no `InitializationProvider` | I8.7(3), I8.6 |
| CNTR-001/002 | `networkSecurityConfig` | I8.6 |
| CNTR-003 | `fullBackupContent`; tink kept names | I8.6, I8.7(3) |

## Risks & Mitigations

| # | Risk | Likelihood / impact | Mitigation |
|---|------|--------------------|------------|
| R1 | A surviving module's version changes (D4a) | Low / Med | I5 drift guard with an exit code; stop |
| R2 | The `receiver` rule is at 50.0% with zero headroom | Low / gate fail | No receiver code is touched; I6 read; IC-9 route |
| R3 | An unexplained manifest diff line, including release-only | Low / Med | Four diffs with the verified `norm`, which fail on an empty diff; per-hunk annotation |
| R4 | Robolectric will not launch the undeclared `HostActivity` | Very low / test fail | I1 isolated run before I2 |
| R5 | R8 kept names lost beyond DonationActivity | Low / release regression | `comm -23`/`-13`; `r8_config_diff.py` fails closed |
| R6 | Third-party integration ON/UNKNOWN on the AVD, or a signature mismatch | Medium / IC-11 only | Fail-closed check at I0 so the product owner can act during the build; never change settings or uninstall |
| R7 | CRLF/BOM damage or wrong lines removed in the strings files | Low / lint or translation breakage | Byte-preserving script with per-file counts; dry run; pure-deletion proofs |
| R8 | Helper scripts corrupted by heredoc backslash collapse | Observed / silent false pass | Write tool only; scripts proven at planning; non-empty and fail-closed conditions |
| R9 | Stale JUnit XML inflates the counts | Medium / wrong AC-7 evidence | `rm -rf …/test-results/testDebugUnitTest` before every counting run; `clean` at I0, I6 and I8 |
| R10 | An orchestrator artifact commit sweeps in a product path | Low / IC-2 broken | Path-limited staging rule; start check and end check of the porcelain in every turn |

## Quality Attribute Coverage (ISO/IEC 25010)

- **Functional Suitability.** Only the Donate entry and its screen go. AC-2 and IC-11 prove that no entry
  leads to a missing class. Backup, restore, scheduling and settings code paths are untouched, and the full
  suite runs at every increment.
- **Performance Efficiency.** `MainSettings.onResume()` no longer starts a Play Billing connection, which was
  an IPC on every settings resume. 12 modules and about 965 R8 mapping entries leave. APK sizes before and
  after are recorded as information, with no target.
- **Compatibility.** minSdk, targetSdk and compileSdk are unchanged. The `<queries>` package-visibility entries
  are removed, and no remaining code needs them. `com.android.vending` stays only as a string constant in
  `GooglePlayServices.java`.
- **Usability.** The settings list ends at "Auto backup settings", with no dead entry. All 22 locales are
  cleaned in one increment, which avoids lint `ExtraTranslation` in release builds.
- **Reliability.** A swallowed-exception path and an asynchronous billing-client lifecycle are removed. No
  persisted state changes (AC-12), so upgrade and rollback are safe.
- **Security.** One permission, two package-visibility queries, six library components and one IPC client
  are removed. The supply-chain posture is unchanged: verification metadata is untouched and nothing is
  lenient.
- **Maintainability.** Three production classes, two test classes, 235 string lines, a JaCoCo rule and a
  stale dependency are removed. The re-host is confined to one file and is nested, so coverage does not
  move. Nine commits keep the increments bisectable.
- **Portability.** The upgrade installs in place with `install -r` and needs no migration. The build is
  unchanged on JDK 25.

## Test Strategy

- **Regression:** `:app:testDebugUnitTest` runs in every increment with JUnit XML enabled by an init script
  and no tracked edit. The count is 697 through I2, then 682 from I3. Failures and errors stay 0, and
  skipped stays 2 (`AppTest.shouldGetVersionName` and the AndroidKeyStore test in `AuthPreferencesTest`).
- **Targeted:** IC-3 runs in isolation (3/3) before the old host is deleted. `PreferenceTitlesTest` parses
  the edited `preferences.xml` at every gate.
- **Coverage:** JaCoCo XML at I0, I6 and I8, and every rule is enforced by `jacocoTestCoverageVerification`.
  Six packages are re-measured. No floor changes.
- **Equivalence:** four manifest diffs, R8 mapping/kept-name/configuration attribution, the dependency
  drift guard, and the dex class listing. All are taken after the final gate, on the final tree.
- **On-device:** a text-only Settings check on API 36, with fail-closed title and logcat checks.
- **Not covered (stated limits):** the release (R8) APK is not run on a device, and there are no
  instrumented tests.

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| U-061 story | sdlc/artifacts/build/sprints/sprint-018/U-061-remove-donation-flow-and-play-billing.md | AC-1..12, IC-1..14, scope, verification |
| plan-review (iteration 1) | sdlc/artifacts/build/sprints/sprint-018/U-061/plan-review.md | B1, B2, rulings, observations |
| REQ-MODERNIZATION-018 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-018-remove-donation-flow-and-play-billing.md | Governing requirement (amended) |
| CR-003 | sdlc/artifacts/build/change-records/CR-003-req-018-manifest-scope-private-app.md | Amendments, triage notes |
| DES-MODERNIZATION-013 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-013-donation-flow-removal-design.md | D1–D8, D4a, Design Validation |
| REQ-MODERNIZATION-010, DES-MODERNIZATION-010 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-010-upgrade-play-billing-to-v7.md; sdlc/artifacts/design/modernization/DES-MODERNIZATION-010-play-billing-v7-design.md | Frontmatter state for AC-10 |
| REQ-MODERNIZATION-003 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-003-establish-ci-coverage-gate.md | Floor and verification constraints |
| REQ-MODERNIZATION-001..019 (frontmatter scan) | sdlc/artifacts/requirements/modernization/ | All approved; none of `type: standard` |
| CNTR-MODERNIZATION-001..008 | sdlc/artifacts/design/contracts/ | All approved; billing/donat grep = 0 |
| sprint-018 plan | sdlc/artifacts/build/sprints/sprint-018/sprint-plan.md | AC-10 dispatch note, IC-11 AVD |
| U-060 log, plan, QA, contract verification, evidence | sdlc/artifacts/build/sprints/sprint-017/U-060/ | Init script, C-1/C-2 recipes, release APK name, device precedent |
| Gate and plan templates | plugin cache `templates/build-artifacts/{gate-report-template,build-plan}.md` | Frontmatter and sections |
| Source | EnrollmentDialogThemeTest.java; MainSettings.java; Consts.java; Preferences.java (90-110, 250-292); ThemeActivity.java; activity/donation/*.java (imports, R refs); PreferenceTitlesTest.java | Edit targets, integration points |
| Resources and manifest | app/src/main/AndroidManifest.xml; res/xml/preferences.xml; res/values*/strings.xml (25); res/values/styles.xml | Inventory, line identity, encodings |
| Build | app/build.gradle; build.gradle; gradle/gradle-daemon-jvm.properties; gradle/verification-metadata.xml; app/lint-baseline.xml; local.properties (existence) | Edits, gates, verification |
| Docs | README.md; CHANGES; sdlc/artifacts/engagement/{code-location,endpoints,systems}.md | AC-11, drift note |
| Build outputs (probes) | merged manifests (debug, release); app-debug.apk (dexdump); jacocoTestReport.xml; test-results; fresh dependency trees | Baseline facts |

## Appendix A — Helper Tools (create verbatim with the Write tool)

`deps_drift.py`, `jacoco_pkgs.py`, `junit_totals.py`, `strip_strings.py` and `r8_config_diff.py` were
executed against real inputs during planning. In iteration 2, the hardened `r8_config_diff.py` and
`ui_titles.py` were extracted from this appendix and run on synthetic inputs. `r8_config_diff.py` passed a
simulated removal and failed closed on identical inputs. `ui_titles.py` passed the expected last title and
failed on a wrong last title and on a "Donate" node. `junit-xml.init.gradle` uses the same mechanism as
U-060 QA's proven script.

`$T/junit-xml.init.gradle`
```groovy
// U-061: enable JUnit XML for Test tasks without editing tracked build files (U-060 QA method).
gradle.projectsEvaluated {
    gradle.rootProject.allprojects { p ->
        p.tasks.withType(Test).configureEach { t -> t.reports.junitXml.required = true }
    }
}
println "U061-DAEMON-JVM: java.version=${System.getProperty('java.version')}"
```

`$T/junit_totals.py`
```python
"""Sum JUnit XML results. usage: python -I junit_totals.py <test-results-dir>"""
import glob, os, sys
import xml.etree.ElementTree as ET
t = f = e = s = n = 0
for p in sorted(glob.glob(os.path.join(sys.argv[1], 'TEST-*.xml'))):
    r = ET.parse(p).getroot()
    n += 1
    t += int(r.get('tests', 0)); f += int(r.get('failures', 0))
    e += int(r.get('errors', 0)); s += int(r.get('skipped', 0))
print(f'files={n} tests={t} failures={f} errors={e} skipped={s}')
sys.exit(0 if n > 0 else 1)
```

`$T/jacoco_pkgs.py`
```python
"""Print package-level LINE coverage from a JaCoCo XML report. usage: python -I jacoco_pkgs.py <xml>"""
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
pkgs = sorted(root.findall('package'), key=lambda p: p.get('name'))
for pkg in pkgs:
    line = [c for c in pkg.findall('counter') if c.get('type') == 'LINE']
    if not line:
        continue
    missed, covered = int(line[0].get('missed')), int(line[0].get('covered'))
    total = missed + covered
    print(f"{pkg.get('name').replace('/', '.')} {covered}/{total} {100.0 * covered / total if total else 0.0:.1f}%")
sys.exit(0 if pkgs else 1)
```

`$T/deps_drift.py`
```python
"""U-061 IC-5 / D4a drift guard. usage: python -I deps_drift.py <before-tree> <after-tree>
Exit 0 = PASS (no surviving module changed version, nothing added, removed set == EXPECTED). Exit 1 = STOP."""
import re, sys
NODE = re.compile(r'^(?P<indent>[| ]*)[+\\]--- (?P<coord>\S+?)(?: -> (?P<to>\S+))?(?P<flags>(?: \((?:\*|c|n)\))*)\s*$')
EXPECTED_REMOVED = {
    'com.android.billingclient:billing',
    'com.google.android.datatransport:transport-api',
    'com.google.android.datatransport:transport-backend-cct',
    'com.google.android.datatransport:transport-runtime',
    'com.google.android.gms:play-services-base',
    'com.google.android.gms:play-services-basement',
    'com.google.android.gms:play-services-location',
    'com.google.android.gms:play-services-places-placereport',
    'com.google.android.gms:play-services-tasks',
    'com.google.firebase:firebase-encoders',
    'com.google.firebase:firebase-encoders-json',
    'com.google.firebase:firebase-encoders-proto',
}

def resolved(path):
    out = {}
    for line in open(path, encoding='utf-8', errors='replace'):
        m = NODE.match(line.rstrip('\r\n'))
        if not m or m.group('coord').startswith('project ') or '(c)' in m.group('flags'):
            continue
        parts = m.group('coord').split(':')
        if len(parts) < 2:
            continue
        ga = parts[0] + ':' + parts[1]
        out.setdefault(ga, set()).add(m.group('to') or (parts[2] if len(parts) > 2 else '?'))
    return out

b, a = resolved(sys.argv[1]), resolved(sys.argv[2])
drift = {k: (sorted(b[k]), sorted(a[k])) for k in b.keys() & a.keys() if b[k] != a[k]}
added = sorted(a.keys() - b.keys())
removed = set(b.keys() - a.keys())
print(f'before={len(b)} after={len(a)} common={len(b.keys() & a.keys())}')
print('version drift (STOP if any):', drift or 'none')
print('added modules (STOP if any):', added or 'none')
print('removed modules:', sorted(removed))
print('removed == expected 12:', removed == EXPECTED_REMOVED,
      '| unexpected removals:', sorted(removed - EXPECTED_REMOVED) or 'none',
      '| expected but still present:', sorted(EXPECTED_REMOVED - removed) or 'none')
sys.exit(0 if b and a and not drift and not added and removed == EXPECTED_REMOVED else 1)
```

`$T/r8_config_diff.py`
```python
"""U-061 IC-8(4), read per DES-013 D6: attribute every line of the R8 configuration.txt delta
(U-060 C-2: sorted-unique non-comment lines).
usage: python -I r8_config_diff.py <before-configuration.txt> <after-configuration.txt>
Exit 0 only if: at least one line was removed; no line was added; every removed line comes from a removed
library's section or is an AAPT -keep for an AC-3 component; and all 7 AC-3 AAPT keeps were removed.
Otherwise exit 1 (explain or escalate)."""
import re, sys
HDR = re.compile(r'^# The proguard configuration file for the following section is (?P<src>.*)$')
REMOVED_LIB = re.compile(r'[\\/](billing-7\.1\.1|play-services-[a-z-]+-[0-9.]+|transport-(api|backend-cct|runtime)-[0-9.]+|firebase-encoders(-json|-proto)?-[0-9.]+)[\\/]')
AC3_CLASSES = (
    'com.android.billingclient.api.ProxyBillingActivity ',
    'com.android.billingclient.api.ProxyBillingActivityV2 ',
    'com.google.android.gms.common.api.GoogleApiActivity ',
    'com.google.android.datatransport.runtime.backends.TransportBackendDiscovery ',
    'com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService ',
    'com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver ',
    'com.zegoggles.smssync.activity.donation.DonationActivity ',
)

def rules(path):
    src, out = '<preamble>', {}
    for raw in open(path, encoding='utf-8', errors='replace'):
        line = raw.rstrip('\r\n')
        m = HDR.match(line)
        if m:
            src = m.group('src')
            continue
        if not line.strip() or line.lstrip().startswith('#'):
            continue
        out.setdefault(line, set()).add(src)
    return out

b, a = rules(sys.argv[1]), rules(sys.argv[2])
removed, added = sorted(b.keys() - a.keys()), sorted(a.keys() - b.keys())
bad, aapt_seen = 0, set()
print(f'before_rules={len(b)} after_rules={len(a)} removed={len(removed)} added={len(added)}')
if not b or not a or not removed:
    bad += 1
    print('FAIL-CLOSED: empty input or zero removed lines (stale or uncopied after-file?)')
for line in added:
    bad += 1
    print('ADDED (explain/escalate):', line)
for line in removed:
    srcs = b[line]
    hits = [c for c in AC3_CLASSES if c in line + ' ']
    if any('aapt_rules' in s for s in srcs) and hits:
        tag = 'OK AAPT keep for AC-3 removed component'
        aapt_seen.update(hits)
    elif any(REMOVED_LIB.search(s) for s in srcs):
        tag = 'OK removed-library rule'
    else:
        tag = 'UNATTRIBUTED (explain/escalate)'
        bad += 1
    print(f'{tag}: {line}  <- ' + ' | '.join(sorted(re.sub(r".*[\\/](transformed|intermediates)[\\/]", "", s) for s in srcs)))
missing = [c.strip() for c in AC3_CLASSES if c not in aapt_seen]
if missing:
    bad += 1
    print('AAPT keeps NOT removed (explain/escalate):', missing)
print('RESULT:', 'PASS' if bad == 0 else f'{bad} problem(s) need explanation')
sys.exit(0 if bad == 0 else 1)
```

`$T/strip_strings.py`
```python
"""U-061 I4: delete the donation strings and the two donation comments from every strings.xml.
usage: python -I -X utf8 strip_strings.py <res-dir> [--write]
Selection is by `name` attribute (10 names) plus the two exact comment lines. All other bytes (CRLF, the
values-zh-rTW BOM) are preserved. Aborts (exit 1, nothing written) if any per-file count differs."""
import glob, os, re, sys

NAMES = ('donate', 'donate_summary', 'ui_dialog_donate_message', 'ui_donation_success_message',
         'ui_donation_failure_message', 'donation_error_iab_unavailable', 'donation_error_unavailable',
         'donation_error_canceled', 'donation_error_already_owned', 'donation_unspecified_error')
NAME_RE = re.compile(r'<string\s+name="(?:' + '|'.join(NAMES) + r')"[^>]*>.*</string>\s*$')
COMMENT_RE = re.compile(r'^\s*<!-- (?:donation related|purchase errors) -->\s*$')
EXPECTED = {  # verified at 21838718; total 235
    'values': 12, 'values-ca': 11, 'values-da': 12, 'values-de': 10, 'values-fr': 12,
    'values-gl': 12, 'values-gr': 11, 'values-hu': 11, 'values-it': 3, 'values-ko': 12,
    'values-nl': 12, 'values-pl': 10, 'values-pt-rPT': 11, 'values-ru': 11, 'values-sk': 4,
    'values-sq': 11, 'values-sr': 11, 'values-sv': 12, 'values-tr': 12, 'values-uk': 11,
    'values-zh-rCN': 12, 'values-zh-rTW': 12,
    'values-cs': 0, 'values-es': 0, 'values-nb-rNO': 0,
}

def plan(path):
    raw = open(path, 'rb').read()
    keep, dropped = [], []
    for i, bline in enumerate(raw.splitlines(keepends=True), 1):
        text = bline.decode('utf-8').lstrip('\ufeff').rstrip('\r\n')
        if NAME_RE.search(text) or COMMENT_RE.match(text):
            dropped.append((i, text.strip()))
        else:
            keep.append(bline)
    return b''.join(keep), dropped

def main(res, write):
    results, ok = {}, True
    for path in sorted(glob.glob(os.path.join(res, 'values*', 'strings.xml'))):
        folder = os.path.basename(os.path.dirname(path))
        new, dropped = plan(path)
        exp = EXPECTED.get(folder)
        ok &= exp == len(dropped)
        print(f"{folder}: delete {len(dropped)} {'OK' if exp == len(dropped) else f'MISMATCH (expected {exp})'}")
        for n, t in dropped:
            print(f'    -{n}: {t}')
        results[path] = new
    print(f'files={len(results)} expected_total={sum(EXPECTED.values())} ok={ok}')
    if not ok or len(results) != 25:
        print('ABORT: nothing written')
        return 1
    if write:
        for path, new in results.items():
            open(path, 'wb').write(new)
        print('WRITTEN')
    return 0

if __name__ == '__main__':
    sys.exit(main(sys.argv[1], '--write' in sys.argv[2:]))
```

`$T/ui_titles.py`
```python
"""U-061 IC-11: print preference titles only (no summaries, which may hold account names) and the count
of nodes mentioning 'donat'. Fails closed.
usage: python -I -X utf8 ui_titles.py <uiautomator-dump.xml> [--expect-last "<title>"]
Exit 0 only if: at least one title was found; no node mentions 'donat'; and, when --expect-last is
given, the last title equals it exactly."""
import sys
import xml.etree.ElementTree as ET
args = sys.argv[1:]
expect = args[args.index('--expect-last') + 1] if '--expect-last' in args else None
root = ET.parse(args[0]).getroot()
titles = [n.get('text') for n in root.iter('node') if n.get('resource-id') == 'android:id/title']
donat = sum(1 for n in root.iter('node')
            if 'donat' in (n.get('text', '') + ' ' + n.get('content-desc', '')).lower())
last = titles[-1] if titles else None
print('titles:', titles)
print('last_title:', last)
print('donat_nodes:', donat)
ok = bool(titles) and donat == 0 and (expect is None or last == expect)
print('RESULT:', 'PASS' if ok else 'FAIL')
sys.exit(0 if ok else 1)
```

## Verdict: PASS

The story can be planned and this plan is complete. Both blocking findings from plan-review iteration 1
are resolved:
- **B1:** the I8 comment no longer contains either term, and AC-1, IC-10 and IC-13 are re-asserted after
  the edit, before the final gate.
- **B2:** snapshot-and-replay is replaced by one increment per Developer turn, with orchestrator commits,
  per-increment expected file lists, path-limited staging rules, porcelain start and end checks, and an
  explicit-path revert on failure.

Every AC and IC maps to a step with named evidence. Every fact used was re-verified at `21838718`. The
device precondition is checked fail-closed at I0 and escalated without blocking the rest of the story.

## Phase Completion Report
---
story_id: "U-061"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-018/U-061/plan.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
