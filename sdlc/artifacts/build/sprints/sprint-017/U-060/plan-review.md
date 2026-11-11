---
artifact_type: gate-result
story_id: "U-060"
gate_id: "plan-review"
gate: plan-review
checkpoint: "plan-gate"
verdict: "FAIL"
agent: "Solution Architect (separate instance, plan-review gate)"
timestamp: "2026-11-11"
iteration: 1
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md"
blockers: 3
warnings: 6
inputs_received_during_review:
  - "Product-owner approval to raise minSdk (relayed by coordinator): \"I'm good raising the minsdk version\" — assessed as B-3"
---

# Gate Report: gate-result — U-060

## Gate: plan-review @ plan-gate

## Verdict: FAIL

## Summary

The plan is strong. It is thorough and executable, maps every AC (AC-1..AC-19, IC-1..IC-3) to concrete steps and evidence, uses sound checkpoints, and relies on none of the forbidden mechanisms (opt-out flags, kapt, lowered floors, weakened verification, or `org.gradle.java.home` in the repo). Most of its load-bearing claims held up when re-checked against primary sources. It fails on two narrow, fixable defects:
- **B-1:** The Gradle compatibility-page citations that carry AC-11 and AC-15 are misquoted. The real text weakens the "documented as supported" claim, and the misquotes would go into Implementation Notes as AC-11 evidence.
- **B-2:** One rollback command is repository-wide. It would discard the pipeline's uncommitted edits to the tracked story file.

A third blocker comes from a product-owner input received during this review:
- **B-3:** The product owner has approved raising minSdk. The plan's SC-2 "stop and escalate" branch, and the R8/R11 stop-points that depend on minSdk 21, must be rewritten as a pre-approved, fully specified contingency branch.

None of the three defects invalidates the chosen version set. All three are plan-text corrections.

## Findings

### Blocking (must be fixed in plan.md before implementation)

**B-1: Gradle compatibility citations for AC-11/AC-15 are inaccurate; an interpretation Scope Concern is missing.**

The plan states that "9.3.x lists '8.13 through 9.0'" and "Kotlin '2.0.0 through 2.3'", and that it is "the first Gradle whose own compatibility page lists AGP 9.0 as tested". SC-1 repeats this, citing "9.3.x / 9.4.x '8.13 through 9.0/9.1'". I fetched the versioned pages on 2026-11-11. They actually say:

| Gradle page | "tested with Android Gradle Plugin …" | "tested with Kotlin …" |
|---|---|---|
| 9.1.0 | 8.4 through 8.12 | 2.0.0 through 2.2.20-Beta2 |
| 9.2.1 | 8.4 through 8.13 | 2.0.0 through 2.2.20 |
| 9.3.0 / 9.3.1 | **8.13 through 9.0.0-beta02** | **2.0.0 through 2.3.0-RC** |
| 9.4.0 / 9.4.1 | 8.13 through 9.1.0-alpha04 | 2.0.0 through 2.3.20-Beta1 |
| 9.5.0 / 9.5.1 | **9.0 through 9.2.0-alpha05** | 2.0.0 through 2.3.20 |

The same pages add "Alpha and beta versions may or may not work" and "Beta and RC versions may or may not work". The Kotlin table at kotlinlang.org/docs/gradle-configure-project.html lists KGP 2.3.20–2.3.21 as Gradle 7.6.3–**9.3.0** and AGP 8.2.2–**9.0.0**.

So no Gradle/AGP/KGP triple is listed at stable versions by all three sources at once:
- Gradle 9.3.1 lists AGP 9.0 and Kotlin 2.3 only as pre-releases.
- Gradle 9.3.1 and AGP 9.0.1 are each one patch above the KGP-table maxima.
- Gradle 9.5.x is the first page that lists stable AGP 9.0, but it falls outside KGP 2.3.21's Gradle range.

AC-11 is worded strictly ("documented as supported"). If the misquotes are copied into Implementation Notes (Steps 12.3 and AC-11 mapping), QA will find the cited sources do not say what is claimed.

Required change to plan.md:
1. Correct the Gradle row of the Version Decisions table and SC-1 to the verbatim ranges above.
2. Add a Scope Concern (e.g. SC-5) that records two things:
   - No combination satisfies AC-11 literally at stable versions.
   - The interpretation applied, with its justification. Pre-release-tested means the stable line is covered. Patch-above-listed-max is covered by the "compatibility per line" rule the plan already states. The KGP table is the only first-party statement that names both Gradle 9.3 and AGP 9.0 together.
3. Re-state the AC-15 "lowest pair" argument against the corrected data, including why 9.3.1 is preferred over 9.4.x/9.5.x.

My assessment is that **9.3.1 / AGP 9.0.1 / KGP 2.3.21 remains the most defensible choice**, so the selection can stand. Only the evidence and its recorded interpretation must be fixed. This is a planner correction, not a product-owner decision. Product-owner acknowledgement of the SC is advisable because AC-11 is strict, but it does not block implementation.

**B-2: Step 3.3 escalation rollback is repository-wide and destroys pipeline state.**

Step 3.3 ("If B2 fails, stop") instructs `git restore --source=HEAD --staged --worktree -- .`. The trailing prose says "for the files listed in this plan", but the command restores the whole tree. The story file `sdlc/artifacts/build/sprints/sprint-017/U-060-jdk-25-build-toolchain-upgrade.md` is tracked and currently modified (` M` in `git status`, carrying pipeline frontmatter `status`/`current_build_phase` and later the Implementation Notes). This command would silently revert it. A Developer agent executes commands literally, and the trigger (R1) is rated Medium likelihood.

Required change: replace it with the explicit path list. Also make that list complete and identical to the "Rollback at any checkpoint" list, which currently omits:
- `gradlew` (regenerated in Step 5)
- `k9mail-vendored/build.gradle`, `metadata/build.gradle` and `settings.gradle` (possible Step 7.4 edits)

**B-3: The product owner pre-approved a minSdk raise; SC-2, R8 and R11 must become a specified contingency branch, not an escalation stop.**

Input: the coordinator relayed the product owner's approval to raise minSdk ("I'm good raising the minsdk version"). The approval is permission, not a requirement.

I assessed two options:
- **(a) Keep the current path.** Try androidx.hilt 1.2.0 at Checkpoint B1, then Fallback B2. If both fail, take minSdk 23 as the pre-approved outcome.
- **(b) Go straight to minSdk 23 with androidx.hilt 1.3.0.**

**Recommendation: (a).** Reasons:
1. Option (b) drops every Android 5.0/5.1 user (Play stops offering updates on API 21–22; existing installs keep running the last compatible version). It may be avoidable at zero risk, because B1 is a cheap build-time test that fails loudly.
2. The story's "Existing Behavior to Preserve" requires "app runtime behavior unchanged". A minSdk raise is a product-visible change that the story should take on only when it is forced.
3. AC-15 (minimal version choice) favours the smallest change.

The minSdk-23 branch does not cascade as far as feared. If it triggers, the plan should specify the following:

| Item | At minSdk 23 (if triggered) | Basis |
|---|---|---|
| androidx.hilt (`hilt-work`, `hilt-compiler`) | **1.3.0**: the lowest line documented for KSP2, and whose `hilt-work` AAR declares minSdk 23 | AAR manifest checked. The 1.3.0 POM depends on `hilt-android` 2.54, which Gradle resolves upward to 2.59.2, so IC-1 still has one Hilt version. The Developer must confirm this in `:app:dependencies`. |
| Hilt | **Stays 2.59.2.** Move to 2.60 only if R11 actually triggers (2.60's minSdk 23 would then be satisfied). | AC-15: minSdk 23 does not by itself force 2.60. |
| Robolectric | **Stays 4.15.1.** It supports SDK 23–36, so minSdk 23 does not force 4.16. 4.16 is only needed if R8 triggers. | Robolectric 4.16 notes |
| `SmsReceiverTest` (4 tests at `@Config(sdk = LOLLIPOP)`) | Robolectric refuses to run below the manifest minSdk, so the 4 tests must be **retargeted to `Build.VERSION_CODES.M`**, not deleted or `@Ignore`d. This preserves their meaning: they exercise the `SDK_INT >= KITKAT && < Q` branch of `compat/SmsReceiver.java:47-50`, which API 23 still takes. | AC-2: count stays 697. Step 7.5 "`git diff app/src/test` empty" must become "only these 4 `@Config` lines changed". |
| `minSdkVersion 21` → `23` | `app/build.gradle` only. The vendored module keeps its own minSdk unless the merge requires otherwise. The comment in `SmsReceiverTest.java:24` is updated. | — |
| Story / ACs | The orchestrator records a story amendment, or a Scope Concern with the product-owner approval quoted, **at trigger time**. It adds an AC: minSdk = 23, rationale, and the LOLLIPOP→M test retarget. AC-19 and "runtime behavior unchanged" get an explicit exception. | Traceability |
| User-facing docs | Add a `CHANGES` entry ("Android minSdk now 23 (6.0, Marshmallow)", following the existing "minSdk now 14" style). Update the `code-location.md` minSdk row. README line 38 ("Tested with Android 4.x …") is already stale: record it as follow-up, or fix it in the same commit. Check `metadata/play` and `metadata/f-droid` text for version claims. | Repo grep |
| Release impact | Record in Implementation Notes: API 21–22 devices stop receiving updates, existing installs are unaffected, and no data migration is involved. AC-5 (API 36 launch) is unchanged. Add a launch check on an API 23 image if one is available. | — |

**Why this is blocking:** plan.md currently tells the Developer to stop and to "not implement any of [the options] without a decision". That decision now exists, so the plan's instructions are stale. The minSdk-23 branch also changes test sources, Step 7.5's evidence rule, the docs and the story's ACs. A Developer agent cannot carry out those changes safely unless the plan specifies them in advance. plan.md is going back for B-1/B-2 anyway, so the rework costs little.

Required change to plan.md:
1. Rewrite SC-2 and the "If B2 fails, stop" text of Step 3.3 as option (a), with the branch above specified as the pre-approved outcome.
2. Update R1, R8 and R11 so that "escalate / needs minSdk 23" becomes "take the pre-approved minSdk-23 branch".
3. Keep a stop condition only for failures that the minSdk-23 branch does not fix.

### Non-blocking observations

- **N-1: Fallback B2 is probably a no-op as written.** It sets `resolutionStrategy.force` on the declarable buckets `ksp`, `kspDebug`, `kspTest` and `kspDebugUnitTest`. Resolution strategy is not inherited through `extendsFrom`. What KSP actually resolves is `ksp<Variant>KotlinProcessorClasspath`.
  - Recommended fix: use a dependency constraint, which *is* inherited, e.g. `dependencies { constraints { ksp('androidx.room:room-compiler-processing:<2.7.x>') { version { strictly '<2.7.x>' } } } }` (and the same for `kspTest`), or target `*ProcessorClasspath` configurations.
  - Proof step: `./gradlew :app:dependencies --configuration kspDebugKotlinProcessorClasspath`.
  - Failure mode today is safe: B1 re-fails, then the plan escalates.
- **N-2: Superseded by B-3 (the product owner has approved a minSdk raise).** SC-1, SC-3 and SC-4 need no product-owner decision before implementation. Do not adopt the mixed-version idea (androidx.hilt-compiler 1.3.0 processor with hilt-work 1.2.0 runtime). Its compatibility is undocumented, and the approved minSdk-23 branch is now a better-supported way to keep 1.3.0.
- **N-3: Step 1 bootstrap.** Add `distributionSha256Sum=b266d5ff…beaff06` together with the URL edit, so the first 9.3.1 download is verified. I confirmed both checksums against services.gradle.org: the bin zip is `b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06` and the wrapper jar is `b3a875ddc1f044746e1b1a55f645584505f4a10438c1afea9f15e92a7c42ec13`.
- **N-4: Step 0.1 expects HEAD `b09bda5d`.** Current HEAD is `fabc2880`, a commit that touches sdlc files only. Record the actual HEAD as the baseline commit; do not stop on the mismatch.
- **N-5: CI daemon-JVM resolution (extend R14).** The plan configures no toolchain download repository. On the GitHub runner, the criteria therefore resolve only if Gradle detects the setup-java JDK 25, either as the launcher JVM or from a standard location. Add the failure signature ("cannot find a Java installation matching…") and a containment option to R14 for the first PR run.
- **N-6: AC-6 doc wording (Step 12.1(b), 12.2).** The precedence claim is confirmed by docs.gradle.org/9.3.1 gradle_daemon.html: "If the Daemon JVM criteria configuration is provided, it takes precedence over JAVA_HOME and org.gradle.java.home". Still, make the final text conditional on the observed M2 and M4 results.
- **N-7: Step 3.3 B2-javac watch list.** Add JDK 23+ javac's annotation-processing default change (`-proc`). Gradle passes an explicit `-processorpath`, so no impact is expected, but a stray note or warning under `-Werror` belongs in the same checkpoint.

### Claims spot-checked and confirmed

| Claim | Check |
|---|---|
| `@Test` baseline 697 | `git grep` authoritative command: 697 |
| `gradlew.bat` absent / new tracked file | not in `git ls-files`, not on disk, not ignored (`git check-ignore` exit 1) |
| SC-4 user-global jvmargs override | `~/.gradle/gradle.properties` line 1 sets `org.gradle.jvmargs` (plus `java.home`, auto-detect=false, paths = JBR) |
| `-Werror` reaches `:app` only | only `app/build.gradle:202`; vendored module is `VERSION_1_8` with no compilerArgs |
| IC-3 scope | `buildDir`/`kotlin-android`/`kotlinOptions` absent from `k9mail-vendored/build.gradle`, `metadata/build.gradle`, `settings.gradle`; root `build.gradle:46`, app sites as listed |
| No legacy variant API; unique namespaces | grep: no `applicationVariants`/`libraryVariants`; namespaces `com.zegoggles.smssync` and `com.fsck.k9.mail` |
| AGP 9.0 minimums | release notes: Gradle 9.1.0, Build Tools 36.0.0, JDK 17, KGP 2.2.10 |
| AGP 9 `proguard-android.txt` disallowed; `onlyEnableUnitTestForTheTestedBuildType`; `strictFullModeForKeepRules`; `resValues` default false | release notes confirm; app has no `resValue` usage (grep) |
| KGP 2.3.21 is the lowest row covering Gradle 9.3 / AGP 9.0 | kotlinlang table (2.3.10 maxes at Gradle 9.0.0; 2.3.0 at AGP 8.13.0) |
| KSP 2.3.1 first with AGP 9 / built-in Kotlin | google/ksp release 2.3.1: "Added support for AGP 9.0 and built-in Kotlin #2674" |
| Dagger 2.60 minSdk 23 | dagger-2.60 release: "minSDK for Hilt is now 23" |
| hilt-work 1.3.0 minSdk 23 | `hilt-work-1.3.0.aar` manifest `minSdkVersion="23"` |
| Robolectric 4.16 drops SDK 21/22 | robolectric-4.16 release notes confirm |
| `updateDaemonJvm` with `toolchainPlatforms = []` and no repository | 9.3.1 docs: the task fails only "if no such repositories are configured and the toolchainPlatforms property has at least one value" |

### Assessment against the gate checklist

- **AC coverage:** complete; every AC and IC has steps and evidence.
- **Forbidden mechanisms:** none relied upon. The `-Xlint:-options` fallback is scoped and recorded, the `--add-opens` contingency is documented Robolectric configuration, and both are acceptable.
- **Ordering and rollback:** workable. The baseline lives in an isolated tree on JDK 17 via a command-line `org.gradle.java.home` (repo untouched). The work is atomic and uncommitted, there is one clean writer pass with an additions-only proof, and the JaCoCo fail-fast guard plus per-package `<class>` comparison properly closes the AC-9 silent-failure risk. B-2 is the exception.
- **File list:** complete. `app/proguard-rules.pro` and `gradlew.bat` are correctly identified as additions beyond the story's Affected Code table.

## Artifacts Consulted

| Artifact | Path | Purpose |
|---|---|---|
| U-060 story | sdlc/artifacts/build/sprints/sprint-017/U-060-jdk-25-build-toolchain-upgrade.md | ACs, ICs, constraints |
| BUG-017 | sdlc/artifacts/stories/BUG-017-build-fails-on-jdk-25.md | Source bug, advisory approach |
| Plan under review | sdlc/artifacts/build/sprints/sprint-017/U-060/plan.md | Full read (568 lines) |
| Root build | build.gradle | Plugin pins, `clean` |
| App build | app/build.gradle | DSL, deps, JaCoCo roots/rules, `-Werror` |
| Vendored build | k9mail-vendored/build.gradle | Java 8 target (grep) |
| Metadata build / settings | metadata/build.gradle, settings.gradle | Gradle 9 constructs |
| Properties / wrapper | gradle.properties, gradle/wrapper/gradle-wrapper.properties | jvmargs, flags, 8.14.1 |
| Verification metadata | gradle/verification-metadata.xml | junit-bom entries (SC-3) |
| CI | .github/workflows/ci.yml | JDK 17, commands |
| User-global (read-only) | ~/.gradle/gradle.properties | SC-4 confirmation |
| External | docs.gradle.org/{9.1.0,9.2.1,9.3.0,9.3.1,9.4.0,9.4.1,9.5.0,9.5.1}/userguide/compatibility.html; docs.gradle.org/9.3.1/userguide/gradle_daemon.html; kotlinlang.org/docs/gradle-configure-project.html; developer.android.com AGP 9.0.0 release notes; services.gradle.org 9.3.1 sha256 files; GitHub releases (dagger-2.60, robolectric-4.16, ksp 2.3.1); hilt-work-1.3.0.aar | Compatibility spot-checks |
| Template | plugin cache templates/build-artifacts/gate-report-template.md | Report format |

## Phase Completion Report
---
story_id: "U-060"
phase: "plan-gate/plan-review"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-017/U-060/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
