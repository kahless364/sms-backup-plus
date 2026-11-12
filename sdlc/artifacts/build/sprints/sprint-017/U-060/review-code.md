---
artifact_type: review-code
story_id: "U-060"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-11-12"
blockers: 0
warnings: 2
---

# Code Review: U-060 (quality-gate iteration 2, rework review)

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance | PASS. The rework changes only comments, `gradlew` shell logic, docs and `.claude/CLAUDE.md`. `git diff 8dc3e417 --stat -- app/src .github gradle build.gradle` is empty. |
| Test coverage | PASS. No test or source change. The gate re-run (`evidence/rework-gates.exit`: `exit=0`) is BUILD SUCCESSFUL in 1m 41s with 73 of 73 tasks executed. |
| Code quality | PASS. The `gradlew` fix is correct. The coverage comments are accurate. Two minor evidence and documentation gaps are noted. |

Basis: read-only review of `git diff 8dc3e417` (the working tree against HEAD). No Gradle build was run. Runtime claims rest on `evidence/rework-gates.log`, `rework-gates.exit`, `rework-gradle-version.txt`, `linux-*.txt` and `qa-results.md`. The story file and `sprint-plan.md` changes were checked: they are status, phase and timestamp only (`status: in-progress` to `review`, `current_build_phase: implementation` to `validation`, `updated_at`, and the sprint-plan row's status cell).

## Verdict: PASS

There are no blockers. All findings are non-blocking and tagged `defer-justified`. The classification standard's `fix-in-cycle` list is a closed four-type list, and none of these findings match a type.

## Iteration-1 disposition

| # | Iteration-1 finding | Disposition |
|---|---------------------|-------------|
| W1 | `receiver` coverage drop attributed to "JaCoCo counting" without evidence; zero headroom | **Resolved as to explanation and comments.** QA's line-level diff and the log's item 1a show three class-declaration lines (`BackupBroadcastReceiver:38`, `PackageReplacedReceiver:15`, `MmsReceiver:7`) lost their coverage. The cause is that AGP 9's unit-test manifest has no app components, so Robolectric no longer instantiates the manifest receivers. This matches my suspicion that execution, not counting, had changed. The log is corrected and the comments in `app/build.gradle` are fixed. **Still deferred:** the zero-headroom gate and the missing `BackupBroadcastReceiver` test, carried as a follow-up story. |
| W2 | `DEVELOPMENT.md` stale (JDK 17, "pins `org.gradle.java.home`") | **Still deferred.** It was not touched, and the log lists it as a follow-up (item 6). See W-B below. |
| W3 | `gradlew` workaround: `gradlew.bat` lacks it; comment placement | **Resolved.** The `gradlew` block is now conditional and marked, and the stock MAX_FD comment is restored. `gradlew.bat` is documented as lacking the workaround in `code-location.md`. A residual `gradlew.bat` line-ending item is deferred (O-3). |
| W4 | CI lint step red before and after; `sdkmanager` step unrun | **Still deferred.** `ci.yml` is unchanged in the rework. The log lists the lint and CI decision as a follow-up (item 6). The first GitHub CI run is the only verification. |
| W5 | JaCoCo guard checks non-empty only, not content | **Still deferred.** `app/build.gradle` has no logic change in the rework, and this was an enhancement suggestion, not a defect. |

## Findings

### Blockers
None.

### Warnings

**W-A. The Linux probe evidence supports the `org.gradle.jvmargs` claim for the Gradle daemon and Kotlin compile daemon, but only thinly, and not for the whole build.**
- The question is whether `-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.WindowsSelectorProvider` in the repo's `org.gradle.jvmargs` (`gradle.properties`) breaks a Linux build.
- What the evidence shows:
  - `evidence/linux-jdk25-selector-probe.txt` is a good, falsifiable probe on real Linux JDK 25.0.5. It shows `Selector.open()` works with no flags and with the tmpdir flag alone, and throws `ServiceConfigurationError` with the WindowsSelectorProvider flag.
  - `evidence/linux-gradle-daemon-probe.txt` and `linux-kotlin-daemon-probe.txt` each report `BUILD SUCCESSFUL` for the shipped jvmargs (probe A), and also with a Linux-safe user-home override (probe B).
  - The Kotlin probe prints "a single-use Daemon process will be forked" to honour the JVM settings. That is consistent with the jvmargs actually having been applied to the forked daemon, and it still succeeded.
- Gaps:
  - The files are summaries (a header and a `BUILD SUCCESSFUL` line). They do not record the commands, the `GRADLE_USER_HOME` and project layout, or the effective jvmargs. In the Gradle probe nothing proves the flag was in the daemon's JVM arguments (only the Kotlin probe's "forked" line hints at it).
  - The mechanism stated in the `gradle.properties` comment and the log ("Gradle applies -D jvmargs after the JVM has started") is not demonstrated, only the success outcome.
  - Both probes used minimal projects. The full app build on Linux is NOT verified, as `gradle.properties` and the log both say, honestly. That includes AGP, KSP, lint, R8, Robolectric test JVMs, the `sdkmanager` step, and JDK 25 daemon criteria detection on the runner.
  - The log's claim "(i) Linux sh with a stub JDK: `JAVA_TOOL_OPTIONS=[]`" has no saved evidence file among the `linux-*` files.
- Judgement: adding no CI containment step is defensible. The comment's recipe for a user-global jvmargs override is safe. The disclosures are honest. The residual risk is that the first Linux CI run fails with `ServiceConfigurationError`.
- Classification: defer-justified. A fuller probe needs a Linux Android SDK and a full build, which cannot be run here. The first PR CI run is the verification, as the log already states.

**W-B. Stale documentation remains for a contributor.**
- `DEVELOPMENT.md` lines ~5-50 still say JDK 17 is required and that `org.gradle.java.home` is pinned in `gradle.properties`, which is false.
- The new `.claude/CLAUDE.md` bullet and `code-location.md` now say the opposite, so the repo has contradictory guidance.
- Classification: defer-justified. The file is outside this story's touched set, outside AC-6's two named files, and listed as a follow-up in the log.

### Observations

- **O-1. `gradlew` shell logic is correct.** Classification: defer-justified. No action needed.
  - Placement: the block sits after the stock `case "$( uname )" ... esac` detection and before "Determine the Java command to use", so `cygwin` and `msys` are already set when it runs.
  - Condition: `if "$cygwin" || "$msys" ; then` executes the `true`/`false` builtins. This is the same idiom the stock script uses later for its own cygwin/msys branch, and it is POSIX `sh` compatible.
  - Windows: Git Bash `uname` returns `MINGW64_NT-...` (matches `MINGW*`, so `msys=true`). `evidence/rework-gradle-version.txt` prints "Picked up JAVA_TOOL_OPTIONS: ... WindowsSelectorProvider ..." and the gates pass.
  - Linux and macOS: `uname` is `Linux` or `Darwin`, so both flags are `false`, the `if` is skipped, and `JAVA_TOOL_OPTIONS` is not exported. The log records a WSL stub-JDK check showing `JAVA_TOOL_OPTIONS=[]` (no saved file; see W-A). WSL reports `Linux` and so stays Linux-safe.
  - Quoting: the `export` line is unchanged from HEAD's block, so it behaves as before, including the harmless leading space.
  - Marking: it is bracketed by `LOCAL ADDITION` and `END LOCAL ADDITION` markers, with a "Re-apply after every `./gradlew wrapper` regeneration" note and a corrected "NOT benign elsewhere" comment.
  - Stock restoration: the MAX_FD comment is back directly above `MAX_FD=maximum`.
  - Mode and endings: `git ls-files -s gradlew` is `100755`, the diff shows no mode line, and `git ls-files --eol` is `i/lf w/lf`.

- **O-2. `app/build.gradle` and `gradle.properties` changes are comment-only.** Classification: defer-justified. Confirmed with `git diff 8dc3e417 -U0`.
  - `app/build.gradle`: the `-` side of the diff is only two trailing-comment lines (compat `minimum = 0.20` and receiver `minimum = 0.50`, values kept) plus one comment line. No `limit`, `counter`, `value`, `element`, `includes` or `excludes` line changed. All 15 rules are retained.
  - The comments now match QA's figures: root 60.0% (18/30), compat 21.9% (16/73), receiver exactly 50.0% (29/58) with zero headroom, and di 6.9% (2/29). The receiver comment names the true cause (the AGP 9 unit-test manifest) and the follow-up.
  - One residual stale note: the root comment says "Kotlin 2.3 code generation no longer emits some inlined helper lines". That is QA's inference. I did not see a per-line artifact for it, and other comments (activity, preferences, scheduler) remain stale. The log says so openly.
  - `gradle.properties`: only the comment block changed. The `org.gradle.jvmargs` line, `org.gradle.daemon=false`, `nonTransitiveRClass` and `nonFinalResIds` are untouched.

- **O-3. `gradlew.bat` follow-up.** Classification: defer-justified.
  - `code-location.md` now says `gradlew.bat` is the stock script without the workaround and that Windows users should use Git Bash `./gradlew`. That addresses my W3.
  - The log records `gradlew.bat` as `i/lf w/crlf` with no `*.bat eol=crlf` in `.gitattributes`. That is deferred as a follow-up.

- **O-4. `.claude/CLAUDE.md` Build JDK bullet: accurate, with two small precision gaps.** Classification: defer-justified. I do not edit it, as instructed.
  - Accurate against the repo:
    - `gradle/gradle-daemon-jvm.properties` has `toolchainVersion=25`.
    - The criteria file taking precedence over `JAVA_HOME` and `org.gradle.java.home` matches M2 and `code-location.md`.
    - The auto-detect failure text matches QA's evidence (`qa-ac7-launcher17-autodetect-off-nopaths.txt`) and `code-location.md` item 3.
    - "per-package LINE floors" is correct, since floors range from 0.00 to 0.70.
    - The `./gradlew --version` "Daemon JVM" line check matches `rework-gradle-version.txt`.
  - The sentence "`gradlew` carries a hand-maintained Windows `JAVA_TOOL_OPTIONS` block; re-apply it after any `./gradlew wrapper` regeneration" is still true now that the block is conditional. It could add "(runs only under Git Bash/MSYS/Cygwin)", which `code-location.md` item 4 already says.
  - Precision gap: "so no `JAVA_HOME` export is needed" holds when a JDK 17+ `java` is on PATH (the launcher JVM). The rework run was a bare shell with no `JAVA_HOME`, and the launcher was 25.0.5 from PATH. On a machine with no `java` on PATH, or with JDK 8 or 11 first on PATH, `gradlew` still needs `JAVA_HOME`. The bullet does not say a launcher JVM is required.
  - The bullet was approved by the product owner and applied by the orchestrator. The log records the deviation from plan Step 12.2 (the Librarian authored and verified the text, the PO approved, the orchestrator wrote it) honestly. This resolves QA's AC-6 failure. The only residual is the launcher-JVM wording above.

- **O-5. Implementation-log accuracy.** Classification: defer-justified.
  - The corrected coverage explanation (items 1a and 2) matches QA's F1: three constructor-line flips, the manifest cause, the root package as Kotlin codegen, and activity and preferences as flaky. The "JaCoCo 0.8.14 counting" attribution is struck out in both places it appeared (lines 53 and 96).
  - Nothing is overstated about Linux and CI. The log says "CI must NOT be claimed to work; the first PR run remains the verification", names the not-verified items, and `gradle.properties` says the full Linux build is not verified.
  - Cosmetic: the section 5 text repeats "No settings or data were changed." twice. Test counts were not re-extracted in the rework run, which the log states.
  - C-3: the log honestly records that the Developer's `am broadcast` ran with integration enabled on the PO's emulator and that the full logcat tail was not saved.

## Patterns Verified
- [x] Follows existing code patterns (the stock-script `if "$cygwin" || "$msys"` idiom; U-NNN comment tags)
- [x] Error handling is appropriate (no logic change outside `gradlew`)
- [x] Tests cover new functionality (no new functionality; 697 tests unchanged per QA's identical-tree run)
- [x] No hardcoded values that should be configurable
- [x] No unnecessary complexity (the block is 3 lines of logic, bracketed and commented)

## Integration Verified
- [x] New code is reachable from production entry points (the `gradlew` block runs on every wrapper invocation, before the Java command is chosen)
- [x] Registries/dispatch maps updated: not applicable to the rework
- [x] Function signatures match at all call sites: no signature changed
- [x] No dead code introduced
- [x] Integration path documented in implementation-log.md (item 3)

## Regression Check
- [x] Capabilities Inventory present in implementation-log.md (iteration 1; unchanged)
- [x] The Windows selector workaround (AC-18) is retained for Git Bash and gated off on Linux and macOS
- [x] All JaCoCo floors, rules, includes and excludes are unchanged
- [x] Removals are comment text only

## Contract Verification
- [x] `gradlew`, `gradle.properties`, `code-location.md` and `.claude/CLAUDE.md` agree on the precedence and Windows-workaround facts (JDK 25 via the criteria file; Git Bash only; re-apply after wrapper regeneration)
- [x] No application or CI-contract change in this rework (`.github` and `app/src` unchanged)

## Runtime Claims Verified
- [x] `## Runtime Claims` inventory read: unchanged (declared, no runtime-provable claims; "release runtime unchanged" is not claimed)
- [x] The new Linux and Windows `gradlew` behaviour claims are backed by probe outputs; the Linux daemon probes are thin (W-A)
- [x] No escape-hatch justifications
- [x] No probe wrote into `../amp` (no sibling repository in scope)
- [x] UNVERIFIED, disclosed: the full app build on Linux and the first GitHub CI run (JDK 25 daemon criteria on the runner, the `sdkmanager` step); the release R8 APK on a device; the C-3 integration-OFF branch

## Phase Completion Report
---
story_id: "U-060"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-017/U-060/review-code.md"
story_status: "in-progress"
current_build_phase: "code-review"
---
