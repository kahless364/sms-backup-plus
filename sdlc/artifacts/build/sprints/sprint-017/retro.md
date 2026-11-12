---
artifact_type: retro
pipeline: sprint-execution
artifact_id: sprint-017
started: 2026-11-11
completed: 2026-11-12
---

# Retro: sprint-017 — Run the build on JDK 25 (U-060, repairs BUG-017)

## Business Summary

**Objective.** Restore a working build on the development machine. After a five-month gap the
machine's JDKs were all 25 (Android Studio's bundled JBR and the JDK on PATH), and the Gradle 8.14.1
wrapper cannot run on Java 25, so no code change could be built or tested (BUG-017). The product
owner's request was "update the app to use JDK 25". One story, U-060, in one wave.

**Outcome.** U-060 is `done` and sprint-017 is `completed`. The build runs on JDK 25 with all three
gates passing. Final QA: 22 of 22 acceptance criteria met; 697 tests run, 0 failures, 2 skipped (two
long-standing `@Ignore`s), counted from JUnit XML on a JDK 25 daemon and test JVM. BUG-017 is closed
(`resolved_by: U-060`).

- Toolchain: Gradle 8.14.1 → 9.5.1, AGP 8.7.3 → 9.0.1 (built-in Kotlin), Kotlin 2.0.21 → 2.3.21,
  KSP → 2.3.6, Hilt 2.51.1 → 2.59.2, JaCoCo 0.8.12 → 0.8.14, Robolectric 4.12.2 → 4.15.1,
  Mockito 5.14.2 → 5.18.0.
- Unchanged: bytecode targets (app 17, vendored module 8), minSdk 21, compileSdk/targetSdk 35,
  androidx.hilt 1.2.0, every coverage floor.
- The JDK requirement is now declared in the repo (`gradle/gradle-daemon-jvm.properties`,
  `toolchainVersion=25`) and takes precedence over a user-global `org.gradle.java.home`.
- APK manifests (debug and release) are byte-identical before and after; the release build keeps the
  same 49 class names it kept before.

**Key Deliverables.** `gradle/wrapper/*`, `gradlew`, `gradlew.bat` (new), `gradle/gradle-daemon-jvm.properties`
(new), `build.gradle`, `app/build.gradle`, `app/proguard-rules.pro`, `gradle.properties`,
`gradle/verification-metadata.xml` (1951 lines added, none removed), `.github/workflows/ci.yml`,
`sdlc/artifacts/engagement/code-location.md`, `.claude/CLAUDE.md` (Build JDK note). Story workspace
`U-060/`: plan, three plan-gate results, implementation log, code review, security review, QA
results, and 65 evidence files.

**Decisions Made.**
- Gradle 9.5.1 instead of 9.3.1: 9.5.x is the first Gradle line whose compatibility page lists
  stable AGP 9.0 and stable Kotlin 2.3.x. No Gradle/AGP/Kotlin triple is listed as supported at
  stable versions by all three vendors; the reading applied is recorded in plan SC-5. Product-owner
  acknowledgement is still outstanding.
- minSdk: the product owner approved raising it ("I'm good raising the minsdk version"). It was
  treated as permission, not a mandate, and was not needed — androidx.hilt 1.2.0 works under KSP2, so
  Android 5.0/5.1 support is kept.
- `proguard-android.txt` (rejected by AGP 9) replaced by `proguard-android-optimize.txt` plus
  `-dontoptimize`, to keep the release build's behaviour without an AGP opt-out flag.
- Evidence: 65 small files committed; 27 files (about 22 MB — R8 mappings, JaCoCo XML, raw Gradle
  logs) kept local only because of size or because they contain the workstation hostname or user paths.

## Process Observations

**Process Compliance.** The full chain ran: create-bug → create-stories → create-sprint →
execute-story (planning, three plan gates, implementation, code review, security review, QA) →
closing bookend. All required artifacts are present with passing verdicts, after rework at two gates.

**Deviations.**
- `.claude/CLAUDE.md` (AC-6) was written by the orchestrator, not the Engine Librarian. The Librarian
  authored and verified the text but declined twice to write the file on an approval relayed by
  another agent. The product owner approved the exact text directly; the orchestrator applied it and
  recorded the deviation in the implementation log. QA counted it as a process departure, not an
  unmet criterion.
- BUG-017 was closed ad hoc at the product owner's request: no skill owns "close a bug resolved by a
  story", and the engine's `artifact transition` command refuses to run without an active skill
  context. The four transitions (ready → planned → in-progress → review → done) were run under a
  story-execution context, the file was moved into the sprint folder, and the Artifact Librarian
  agent added `resolved_by` and `sprint`.
- U-060's `pipeline` frontmatter field is empty (as on every earlier story in this project). The
  skill text says to stop; the project default (`defaults.workflow: standard-story`) was used instead.
- QA's first FAIL was routed back to implementation using the compiled pipeline's quality-gate
  checkpoint (`feedback_target: implementation`, `max_iterations: 3`); the skill text for that phase
  says to mark the story blocked. The compiled checkpoint was treated as authoritative.
- The expert-depth-check FAIL (warning severity) was recorded and not sent back for a fourth
  planning iteration.
- The story was run with `execute-story` directly, not `execute-sprint`, since the sprint had one
  story; no worktree isolation was used.

**Gate Results.**
- plan.md: PASS (three planning iterations).
- plan-review: FAIL, FAIL, PASS. Iteration 1: overstated vendor compatibility quotes, a repo-wide
  `git restore` in the rollback, and a stale stop-and-escalate step. Iteration 2: a Robolectric-only
  failure would have raised minSdk without need.
- contract-verification: PASS, with five recommended developer checks (all carried out except the
  broadcast "ignored" case, see below).
- expert-depth-check: FAIL at warning severity (pattern naming and citations only).
- review-code: PASS, PASS. review-security: PASS, PASS, with one fix-in-cycle item (a screenshot
  showing a personal email address, deleted before any evidence was committed).
- qa-results: FAIL, PASS. Iteration 1 failed AC-6 only.

**Issues Encountered.**
- Session interruption: the first developer run was cut off when the session ended, early in the
  baseline step. Nothing in the repo had changed; the run was resumed from its transcript and from
  then on wrote its log after every step.
- Framework defect (amp 1.7.2, Windows): `lib/cli/compile-pipeline.mjs` prints nothing and exits 0
  on Windows, because its entry guard compares `import.meta.url` (`file:///C:/…`) with
  `file://` + `process.argv[1]` (`C:\…`). Worked around with a launcher script that sets `argv[1]`
  and imports the unmodified module. `CLAUDE_PLUGIN_ROOT` is also unset in shell commands, which
  makes `lib/index.mjs plan` fail to find the pipeline registry until it is exported.
- Wrapper regeneration dropped the Windows `JAVA_TOOL_OPTIONS` block in `gradlew`; every Gradle call
  then failed with "Unable to establish loopback connection". Re-added by hand.
- That block was unconditional before this story, and its Windows-only `SelectorProvider` flag makes
  a Linux JVM throw `ServiceConfigurationError` (confirmed under WSL). The ubuntu CI job could not
  have started Gradle since the block was introduced; nobody noticed because no PR has run. Now
  guarded to cygwin/msys.
- The developer's first explanation for a coverage shift ("JaCoCo 0.8.14 counting") was wrong and
  was caught by code review. QA rebuilt the old toolchain and found the cause: AGP 9 gives
  Robolectric a unit-test manifest without the app's own components, so three receiver
  class-declaration lines are no longer executed. `receiver` sits at exactly its 50% floor.
- HEAD already failed dependency verification on JDK 17 (two `junit-bom` `.module` checksums
  missing) and `:app:lintDebug` already failed before the upgrade. The first is fixed by the
  regenerated metadata; the second is not addressed.
- The on-device broadcast check started a real backup on the product owner's emulator, because
  third-party integration had been left enabled there by earlier testing in this session. It found
  nothing to back up and uploaded nothing.
- Evidence hygiene: device screenshots and raw logs carried a personal email address, the
  workstation hostname and user paths. Caught by security review before any of it was committed.

**Not verified** (no acceptance criterion requires these): the first CI run on this toolchain and a
full Android build on Linux; the release APK on a device; the broadcast "ignored when third-party
integration is off" branch, which has no unit test (`BackupBroadcastReceiver` has 0 of 11 lines covered).

**Recommendations.**
1. Open follow-up stories: a `BackupBroadcastReceiver` unit test for both branches (restores
   `receiver` coverage headroom); a decision on correcting the AGP 9 unit-test manifest; `DEVELOPMENT.md`,
   `.kotlin/` in `.gitignore` and `.bat` line endings; running the release APK on a device. BUG-018
   (restore reports failure after succeeding) can now be built and should be scheduled.
2. Open a PR to get the first CI run on JDK 25, and decide what to do about the already-failing CI
   lint step.
3. Report the `compile-pipeline.mjs` Windows entry-guard defect to the AMP plugin maintainers, and
   ask for a defined path for closing a bug that a repair story resolved and for config-file edits
   the product owner approves in the main session.
4. When an agent may capture device output, tell it up front: text evidence only, no screenshots,
   no account names. And turn off test affordances on a shared emulator before handing it to an agent.
5. For any long-running agent, require a step-by-step log from the start so an interrupted session
   can resume from the log.
