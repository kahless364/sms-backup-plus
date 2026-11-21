---
artifact_type: gate-result
gate: expert-depth-check
gate_id: "expert-depth-check"
checkpoint: plan-gate
pipeline: story-build
story_id: "U-075"
artifact_under_review: sdlc/artifacts/build/sprints/sprint-024/U-075/plan.md
artifact_reviewed: sdlc/artifacts/build/sprints/sprint-024/U-075/plan.md
producing_agent: amp/agents/solution-architect
verdict: PASS
severity: warning
agent: "Solution Architect"
reviewer_agent: amp/agents/solution-architect
reviewer_mode: separate_instance
iteration: 1
timestamp: "2026-11-21T17:33:05Z"
produced_at: 2026-11-21T17:33:05Z
recommendations_count: 2
criterion_results:
  - criterion: "Pattern grounding"
    verdict: satisfied
    deficit_present: false
  - criterion: "Anti-pattern recognition"
    verdict: satisfied
    deficit_present: false
  - criterion: "Trade-off articulation"
    verdict: satisfied
    deficit_present: false
  - criterion: "Citation depth"
    verdict: satisfied
    deficit_present: false
  - criterion: "NFR and quality-attribute coverage"
    verdict: satisfied
    deficit_present: false
  - criterion: "Tooling evidence"
    verdict: satisfied_with_recommendation
    deficit_present: false
  - criterion: "Sequencing rigor"
    verdict: satisfied
    deficit_present: false
  - criterion: "Anti-AI-slop check"
    verdict: satisfied_with_recommendation
    deficit_present: false
---

# Gate Report: expert-depth-check — U-075

## Gate: expert-depth-check @ plan-gate

## Verdict: PASS with recommendations (severity warning, observe-only), 2 recommendations

Six criteria are satisfied. Two are satisfied with a recommendation:
- **Criterion 6 (tooling).** The dex check in I6 step 4 reads only the debug APK, but the question it is there to answer
  (has R8 moved the API 31 reference?) only arises in the release APK.
- **Criterion 8 (depth).** Four things are missing or misstated:
  - the stop of a long automatic run, now that automatic runs are no longer in the foreground. This covers the hard
    10-minute limit on the owner's Android 11 phones (recalled), the interaction with BUG-024 before U-074, and the
    unlimited default batch size;
  - where WorkManager 2.9.1 refuses a start a second time. The plan says 2.12.0, but the second refusal point is
    already in 2.9.1;
  - why `getForegroundInfo()` does not need an override;
  - three gaps in the evidence of the device check: process state at the worker's start, which scheduler started the
    work, and the notification permission.

Each recommendation is a change to plan text or evidence steps. None changes the design, a test, a count or the number
of Gradle runs.

## How this review was done

- **Plan.** Read in full: sections 0 to 2.7, 3.3, 3.5 (`dex_refs.py`), 4.0 to 4.10, 5 to 13 and the Verdict. Section
  3.4's test text was read through line 599 (the shared doubles and the log helpers).
- **Story and bug.** The story was read in full, including the rulings SC-1 to SC-9. BUG-029 was read in full. The
  observation record was searched for its timings.
- **Code, read at `fd70325a`, post-U-070 blob `ba667c43…` and read-only checks:**
  - `doWork` and its catches at post-U-070 `:118-171`;
  - `git grep` for `setExpedited`, `getForegroundInfo`, `POST_NOTIFICATIONS`, `-dontoptimize`, `-dontobfuscate` and
    `proguard-android`;
  - `Defaults.java:23`, `MAX_ITEMS_PER_SYNC = -1`.
- **Library facts, read with `javap` on `work-runtime-2.9.1.aar` from the local Gradle cache:**
  - `SystemForegroundService` declares no `onTimeout` method;
  - `SystemForegroundService$Api31Impl.startForeground` catches `ForegroundServiceStartNotAllowedException` and
    `SecurityException` and only logs "Unable to start foreground service";
  - `CoroutineWorker.getForegroundInfo` throws `IllegalStateException("Not implemented")` by default;
  - `GreedyScheduler` and `DelayedWorkTracker` are present and log "Starting work for".
- **Facts recalled.** Platform facts that I recall but did not read are marked "recalled, not verified".
- **Nothing was run.** No Gradle, build, test, device or network.

## Answers to the six questions

### 1. Is the design right, and what must the plan state?

The design is right. Ruling 60's option (c) is what a senior engineer would choose:
- automatic work never asks for the foreground;
- user-started work asks, and degrades when refused;
- the refusal is decided by who started the run, not by guessing process importance.

The helper's exception boundary is exact (plan.md:171-179):
- it rethrows cancellation first;
- it continues only on the refusal, and only behind `SDK_INT >= S`;
- the Android 14 type exceptions, `SecurityException` and `Error` all stay loud.

| Point | Plan | Consequence |
|-------|------|-------------|
| 10-minute limit of ordinary work, a long automatic run | **Covered partly** (2.6 item 3, plan.md:287-289; R10, plan.md:1782): "on a busy system it is subject to the job time limit; a stopped run is rescheduled and continues above the watermark" | Correct for Android 12+, as BUG-029 L5 says. Three things are missing:<br>(a) **Android 11 and earlier.** Ordinary jobs are stopped at 10 minutes whatever the load (recalled, not verified). The owner's phones are "Android 11 or later" (story :61). On Android 11 this story changes automatic runs from foreground (allowed there) to ordinary, so a long automatic run on that phone is now always cut at 10 minutes.<br>(b) **Run size is not bounded.** `MAX_ITEMS_PER_SYNC` defaults to `-1`, unlimited (`Defaults.java:23`), so a REGULAR run after a long gap is not bounded by size.<br>(c) **Until U-074, a stop reaches the broad catch** (`BackupWorker.kt:168-171`). It logs "BackupWorker: unrecoverable error" at ERROR with the `CancellationException`, and returns `Result.failure`. WorkManager ignores that result for interrupted work and reschedules (BUG-029 L6). So the effect is one misleading ERROR line, no loss and no FAILED state. Section 9 (i) lists that same line as a **Fail** (plan.md:1864-1865), so a system stop during the device step would be misread as a fix failure.<br>Android 16's runtime quotas for jobs make stops more likely (recalled, not verified). |
| Doze and App Standby without the foreground | **Not stated** | When a job starts does not change: JobScheduler decides that before `doWork`, with or without the foreground. A running ordinary job can now be stopped by a device-state change (Doze, a lost network constraint). Before, the foreground map ignored such a stop (L10). The watermark makes this lossless. One sentence in 2.6 item 3 is enough. |
| Android 14 foreground service type | **Covered correctly** | The type is kept (`createBackupForegroundInfo` unchanged, AC-4). The type exceptions propagate (F10, R4, M-F3). |
| Android 15 six-hour `dataSync` budget | **Covered partly** (2.6 item 7; section 12, Performance Efficiency) | The next-start case is handled correctly. The running case is missing: WorkManager 2.9.1's `SystemForegroundService` has no `onTimeout` (`javap`, verified). On Android 15+ the system then stops or crashes a `dataSync` service that runs past the budget (recalled, not verified). This is pre-existing. After this story it concerns only long manual backups and restores, and the exposure goes down. It belongs in hand-over item 6 for U-073, where 2.10+ handles `onTimeout` (recalled). |
| `BOOT_COMPLETED` may not start `dataSync` services (apps targeting 35) | **Not stated** | Moot after the fix: the boot backup is `REGULAR` and never asks. Even before, WorkManager started the service from a job, not from the receiver (BUG-029 Not verified 3). The plan should say the rule is moot, so that the question is closed rather than inherited. |
| `setForeground` refused: does WorkManager still treat the work as in the foreground? Does the `SystemForegroundService` binding leak? | **Covered correctly** (2.5 items 1, 4; L10) | The work is foreground-mapped until it ends, and stops are ignored. `cleanUpWorkerUnsafe` stops the service when the map is empty. Now this applies only to user-started runs (R9). |
| A second refusal point inside the service | **Covered wrongly** (2.6 item 8, plan.md:297-298: "WorkManager 2.12.0 has a second refusal point inside `SystemForegroundService.Api31Impl`") | **2.9.1 already has it.** `SystemForegroundService$Api31Impl.startForeground` catches `ForegroundServiceStartNotAllowedException` and `SecurityException` and only logs "Unable to start foreground service" (`javap`, verified; the plan's own L19 names that class in 2.9.1). If it is ever reached, today the worker is not told and continues without the app's warning. The behaviour is harmless, but the attribution to 2.12.0 is wrong, and the device evidence should look for the line. |
| API 24 to 30 | **Covered correctly** (2.6 item 3; story "Existing Behavior to Preserve") | No restriction there. User-started runs are promoted as today. Automatic runs change in the same way as on 31+, with the 10-minute caveat above. |
| `getForegroundInfo()` override | **Not stated** | Not needed, but the reason belongs in the plan:<br>- No request is expedited (`git grep setExpedited` at `fd70325a` prints nothing).<br>- WorkManager calls `getForegroundInfoAsync` on API < 31 only for expedited work (BUG-029 L8).<br>- The default `CoroutineWorker.getForegroundInfo` throws `IllegalStateException("Not implemented")` (`javap`, verified).<br>So a later story that expedites `BROADCAST_INTENT` or `MANUAL` must add the override, or the run fails on API 24 to 30. That is a hand-over line. |
| `POST_NOTIFICATIONS` denied on API 33+, and a manual backup | **Not stated** (BUG-029 Not verified 9) | The app asks at run time (`MainActivity.java:507-521`). If the permission is denied, the foreground service still starts and the run works, but the notification is not shown in the shade (recalled, not verified). Two effects:<br>- **The fix is not affected.**<br>- **Device step (iii) is affected.** Its pass needs the notification record or a screenshot (plan.md:1832-1838). With the permission denied it could only end "not conclusive" twice, although the foreground start succeeded. |

### 2. The class-loading guard

- **The technique is correct and more than sufficient on ART.**
  - Since ART, an unresolvable type in `instance-of` or a catch list is a soft verification failure. The class is then
    re-verified at run time, and the code throws only if that instruction runs (recalled, not verified; the plan marks
    it P3 and does not rely on it).
  - The holder object keeps even the soft failure out of `WorkerForegroundKt`.
  - The `SDK_INT` short-circuit means `ForegroundRefusal` is never loaded below 31.
  - Catching `IllegalStateException` instead of the API 31 type keeps the type out of the exception table. That is
    the decisive point, and the plan makes it (plan.md:173).
- **R8.**
  - L22 shows `-dontoptimize` present (`proguard-rules.pro:57`, used with `proguard-android-optimize.txt`,
    `app/build.gradle:51-54`). With it, R8 neither inlines nor merges classes (recalled for AGP 9's R8; the file read
    proves the flag, not R8's response to it).
  - **The release dex is not checked.** I6 step 4 (plan.md:1499-1510) unzips both APKs but runs `dexdump` and
    `dex_refs.py` over `dex-debug` only. The release build is the one R8 touches. Obfuscation is on: there is no
    `-dontobfuscate` under `app/`. So a filter on `Lcom/zegoggles/` would also miss renamed classes.
  - `dex_refs.py` lists every class, not only the app's, and the plan already greps the two mapping lines. So the
    check can detect merging if it translates names through `mapping.txt`. Without that it cannot. (Criterion 6
    recommendation.)
  - If R8 were to add a stub or outline class for a library type newer than `minSdk` (recalled, not verified), it
    would show up there as well.
- **F13 at SDK 24 is not ART evidence, and the plan says so with the right weight** (plan.md:186-191):
  - (b) is "JVM";
  - (d), the optional device step on `Pixel_API_24`, is "the only proof on ART itself";
  - M-X1 is honestly informational: F13 may pass without the guard if the sandbox resolves the class elsewhere (for
    example from the compile SDK's `android.jar` on the unit-test classpath; not read).
  - D19 then says the proof rests on bytecode and the device step.

  The plan could add one plain sentence: "F13 proves only that the helper loads and rethrows on a HotSpot JVM in a
  sandbox without the class; it says nothing about ART's verifier."

### 3. Test design

- **The third shape is how production delivers the refusal.**
  - `WorkForegroundUpdater` returns a `SettableFuture` that its task fails on the task thread (L6).
  - `CoroutineWorker.setForeground` normally finds the future not done and suspends in `await` (L5).
  - F5 reproduces this: the future is failed after the run is shown to be suspended before `create()`.
  - F1 (thrower) and F4 (already-failed future) cover the other two branches. Comparing by class, not identity (P1),
    is right.
- **What the device check still has to show.** The platform delivers this class on a real start. BUG-029 observed that
  on Android 16 (log :20, :51). Nothing in the unit tests reaches `Processor.startForeground` or `ContextCompat`.
- **Edge cases an expert would add** (none blocks; "could"):
  - **A refusal on a retry**, `setRunAttemptCount(1)` with the throwing updater. It pins that the helper runs before
    the backoff check (2.6 item 1) and that the "starting backup … attempt=1" line follows.
  - **Cancellation while suspended on the pending future.** This is the production shape of AC-5 (d). F12 and R5 use
    a thrower that cancels first. A variant of F5 that cancels the job while it is suspended exercises
    `await`'s cancel branch and then line 48.
  - **A manual run that loses the screen before `doWork`.** This is the refusal itself, covered by F1 to F5. On a
    device it is step (v), which SC-3 rules out.
  - **A restore while the app holds the default-SMS role.** The unit tests do not differ. Whether the role exempts
    the app from the background-start rule was not read. I recall no such exemption (recalled, not verified). Either
    way the helper is correct.

### 4. Proportion and risk

- **The scale is proportionate:** 18 tests, 12 injection runs, and 16 Gradle runs of which 2 are full gates.
  - The two gates are what AC-9 asks for.
  - The 14 targeted runs leave out the 5.5-minute class.
  - M-X1 costs one targeted run and answers whether F13 means anything.
  - The three U-070 re-runs are AC-11's.
- **Nothing is redundant.**
- **One check is missing, and it needs no Gradle run:** the release-dex check.
- **The stop conditions that matter are present:**
  - D11 (c), F13 failing at class load;
  - D10, D26, no download;
  - D24, a mapping line missing;
  - D29, a cut-off run.
- **D25 is correctly not a stop,** because the `SDK_INT` guard still governs execution.

### 5. The device check

- **The decisive pass condition holds whatever the screen state.** Step (i) passes only if there is no "Moving WorkSpec
  (<id>) to the foreground" for the INCOMING work (plan.md:1859-1863). So a still-asking automatic run fails the step
  even inside a grace period. The off-screen rule matters only for the second claim, "succeeds off screen".
- **The timing is reasonable:**
  - the trigger fires about 11 s after the SMS, then the delay is 60 s, so the run starts about 70 s or more after
    Home;
  - the 58 s threshold is the observed refused interval;
  - the top-activity reads at steps 1 and 3 are good.
- **What it does not prove is the process state.** Two additions fix that (Criterion 8 recommendation):
  - At the start line, read `dumpsys activity processes com.zegoggles.smssync` (`procState`/`curProcState`, `adj`)
    and `dumpsys activity services com.zegoggles.smssync` (no `SystemForegroundService` record, or
    `isForeground=false`).
  - Add `WM-SystemJobService|WM-GreedyScheduler` to the extract filter (plan.md:1902). That records whether
    JobScheduler or WorkManager's in-process scheduler started the work. The latter starts delayed work in a live
    process without a job ("Starting work for", verified in 2.9.1 strings), so its time limit and process protection
    differ.
- **The boot step's "not reached" rule avoids a false pass.** Pass needs both a start and SUCCESS. One outcome is left
  undefined: no start with a reason other than `WITHIN_QUOTA` (an unmet network constraint, the `restricted` bucket).
  It should be recorded "not reached: <reason line>" instead of being left without a label ("could").
- **Step (iii) can be made robust against a denied notification permission** with a read-only precondition (Criterion
  8 recommendation).

### 6. What the plan cannot prove, and whether it says so

Section 9 "What it does not prove" (plan.md:1906-1909) names:
- a refused manual run on a device;
- Android 12 to 15;
- a physical device;
- the owner's phones;
- a held boot backup.

It is missing three items:
- **OEM background restrictions.** On the owner's phones these can hold or kill jobs whatever this fix does.
- **The stop of a long automatic run** (10 minutes, Doze, quota), which no step exercises.
- **The Android 11 behaviour change** in question 1 (a).

These belong in the same paragraph.

## Findings

### Criterion 1 — Pattern grounding

**Verdict:** satisfied

**Evidence present:** Section 2.0 (plan.md:122-150) names each pattern and applies it to this code:
- the AndroidX `ApiNNImpl` isolation pattern, tied to WorkManager's own `SystemForegroundService$Api31Impl` (L19);
- Test Stub and Test Spy (Meszaros ch. 11), at the library's `ForegroundUpdater` seam;
- Fail Fast versus Steady State, graceful degradation (Nygard ch. 5);
- cooperative cancellation (kotlinx guide; Goetz ch. 7);
- characterisation, then failing-first, then mutation analysis (Feathers ch. 13; Beck; DeMillo, Lipton and Sayward
  1978).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 2 — Anti-pattern recognition

**Verdict:** satisfied

**Evidence present:**
- Exception swallowing (Bloch Item 77) is named as the risk of a broad catch. M-F2 is the injection that proves the
  tests see it (plan.md:181-184).
- Catching the parent `ServiceStartNotAllowedException` is rejected because it hides the BUG-016 class of defect.
- Assertion Roulette is guarded by named assertion messages (plan.md:150).
- The root cause is a misread platform rule ("call early to avoid ANR"). It is named, and its stale comments are
  listed rather than silently kept (2.7).

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 3 — Trade-off articulation

**Verdict:** satisfied

**Evidence present:**
- **The decision.** Section 2.1 states it: kept for `MANUAL`, `SKIP` and restores; dropped for automatic types.
- **The alternatives.** Section 2.2 (plan.md:193-204) gives each one with its reason, and one of them is decided on
  a measured cost: the inline `is` "costs two lines and removes the question" versus the holder.
- **What gives way.** It is explicit in R9 (a refused manual run is not stoppable, an accepted trade under ruling 60)
  and in R10.

**Evidence missing:** (none at criterion level). The cost side of R10 is understated (Criterion 8).

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 4 — Citation depth

**Verdict:** satisfied

**Evidence present:** Book citations carry an item or chapter:
- Bloch Items 73 and 77;
- Nygard ch. 4, 5 and 10;
- Meszaros ch. 11 and 16;
- GOOS ch. 8;
- Feathers ch. 13;
- JCIP ch. 7.

The plan states that they are "cited as commonly given; I did not re-read them" (plan.md:124). Every library fact is
anchored to a class, a member, and a bytecode offset or exception-table range (L5 to L15, L19). Vendor pages cited by
name are marked "not fetched".

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 5 — NFR and quality-attribute coverage

**Verdict:** satisfied

**Evidence present:** Section 12 (plan.md:1953-1964) evaluates all eight ISO/IEC 25010 characteristics with findings
specific to this story. Two examples: the `dataSync` budget under Performance Efficiency, and API 24 to 36 behaviour
under Portability.

**Evidence missing:** (none at criterion level). The Reliability row's "a stopped automatic run is rescheduled and
resumes" should carry the costs that Criterion 8's recommendation adds.

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 6 — Tooling evidence

**Verdict:** satisfied_with_recommendation

**Evidence present:** This is unusually strong tooling evidence for a plan:
- `javap` on WorkManager, work-testing and Robolectric (L5 to L15);
- SHA-512 of the cached SDK jars (L13);
- a scratch Kotlin compile of the helper, its mutants and both test files (with stubs, labelled);
- a D8 and `dexdump` check of the helper at `--min-api 24` (L19);
- JaCoCo baselines from U-069's evidence (L23);
- every hunk, numstat and blob derived on a text simulation of the start tree (L20, L21).

**Evidence missing:** The one dex check in the build reads the **debug** APK only (I6 step 4, plan.md:1503-1510; row
D25). The question it answers is whether a code shrinker moved the API 31 reference into the caller. That question
exists only in the release APK, where R8 runs and obfuscation is on: there is no `-dontobfuscate` under `app/`.
`-dontoptimize` (L22) is verified as present in the file. Its effect on AGP 9's R8 is recalled, not measured.

**Passage:** Section I6 step 4 (plan.md:1499-1510); row D25 (plan.md:1730); L22 (plan.md:106).

**Recommendation / Remediation (should):** Add to I6 step 4, after the debug loop:

```bash
FR=$(grep -E '^com\.zegoggles\.smssync\.service\.ForegroundRefusal -> ' app/build/outputs/mapping/release/mapping.txt | sed -E 's/.* -> (.*):$/\1/')
WK=$(grep -E '^com\.zegoggles\.smssync\.service\.WorkerForegroundKt -> ' app/build/outputs/mapping/release/mapping.txt | sed -E 's/.* -> (.*):$/\1/')
echo "release names: ForegroundRefusal=$FR WorkerForegroundKt=$WK"
for d in "$WORK"/dex-release/classes*.dex; do echo "== release $(basename "$d")"; "$BT/dexdump.exe" -d "$d" | python -I -X utf8 $TL/dex_refs.py 'Landroid/app/ForegroundServiceStartNotAllowedException;'; done
```

Expected: among the app's classes, the only one that names the type is `L<FR with / for .>;`, or a synthetic class
derived from it. `L<WK>;` is not listed. Library classes are recorded as library. Extend D25 with this rule: "In the
**release** dex, `L<WK>;` or any other app class naming the type means R8 merged or inlined the holder: record it, not
a stop, and the orchestrator runs device step (vi) on the **release** build, or records why not." Add one line to L22:
"the release dex check (I6 step 4) measures what `-dontoptimize` is only expected to do".

### Criterion 7 — Sequencing rigor

**Verdict:** satisfied

**Evidence present:**
- I0 to I7 are ordered with stated reasons: the baseline before any write, characterisation and red before the fix,
  pristine copies before injections, the final gate after every `restored` line.
- Each step names its decision rows.
- Section 4.10 gives the safe stopping points and the resume checks.
- Parallelism is consciously excluded, with the reason: one Gradle at a time, background gates, D29.

**Evidence missing:** (none)

**Passage:** (N/A)

**Recommendation / Remediation:** (none)

### Criterion 8 — Anti-AI-slop check

**Verdict:** satisfied_with_recommendation

**Evidence present:** A senior Android engineer would not push back on the core:
- **The exception boundary** is specified line by line, with the reason for each line (plan.md:169-179).
- **The three delivery shapes** of the refusal are tied to WorkManager's own branches (2.4).
- **A refused user-started run** is settled from bytecode and the status-screen code (2.5).
- **The interactions** with U-074, U-071 and U-073 are exact (2.7).
- **The device check** has a falsifiable pass rule and an explicit "not reached" outcome.

**Evidence missing:** These are platform consequences a practitioner would state, plus one wrong attribution. The
details are in question 1 above.
1. **The stop of a long automatic run** (2.6 item 3, R10). Three costs are missing:
   - the hard 10-minute limit on Android 11 and earlier, the owner's oldest phones (recalled);
   - the unlimited default batch size (`MAX_ITEMS_PER_SYNC = -1`);
   - the interim BUG-024 effect: an ERROR line "BackupWorker: unrecoverable error" and an ignored `Result.failure`,
     until U-074.

   Section 9 (i) would then mislabel such a stop as a Fail.
2. **Doze, device-state and constraint stops** of a running ordinary job are not stated.
3. **2.6 item 8 attributes the service's own refusal catch to 2.12.0.** It is already in 2.9.1 (`Api31Impl`, verified
   by `javap`; the plan's own L19 names the class).
4. **The reasons for leaving out the `getForegroundInfo()` override**, and **the `BOOT_COMPLETED` `dataSync` rule
   being moot**, are not recorded.
5. **Three evidence gaps in section 9:**
   - the process state at the INCOMING start;
   - which scheduler started the work;
   - the notification-permission precondition for (iii).

   And "What it does not prove" lacks OEM restrictions and the long-run stop.

**Passage:** Section 2.6 items 3 and 8 (plan.md:287-289, :297-298); R10 (plan.md:1782); section 9 steps 4, (iii), (i),
Record and "What it does not prove" (plan.md:1812-1814, :1832-1840, :1842-1865, :1902, :1906-1909); section 10 item
6.

**Recommendation / Remediation (should):** These are plan-text and evidence-step changes only.
- **Replace 2.6 item 3** with:
  > An automatic backup on any Android version no longer enters the foreground, so it runs as an ordinary job and the
  > system's stops are honoured (BUG-029 L6): the job time limit (on Android 12+ when the system is busy, BUG-029 L5;
  > on Android 11 and earlier at 10 minutes regardless, recalled, not verified), Doze or another device-state change,
  > a lost network constraint, and on Android 16 the job runtime quota (recalled, not verified). With
  > `MAX_ITEMS_PER_SYNC` at its default `-1` (`Defaults.java:23`) a REGULAR run after a long gap is not bounded by
  > size. A stopped run is rescheduled and continues above the watermark (BUG-010), so nothing confirmed is lost.
  > Until U-074 the stop reaches the broad catch (`BackupWorker.kt:168-171`): one ERROR line "BackupWorker:
  > unrecoverable error" with the `CancellationException`, and a `Result.failure` that WorkManager ignores for
  > interrupted work. When a job starts (Doze windows, standby bucket, quota) is decided by JobScheduler before
  > `doWork` and does not change.
- **R10:** likelihood "Medium on Android 11 for large runs / low"; mitigation "watermark; reschedule; U-074 removes the
  ERROR line".
- **Replace 2.6 item 8's first clause** with: "WorkManager 2.9.1 already has (and 2.12.0 keeps) a second refusal point
  inside `SystemForegroundService$Api31Impl.startForeground`, which catches the refusal and `SecurityException` and
  logs only 'Unable to start foreground service' (`javap`)". Add `Unable to start foreground service` to the section 9
  extract and to (iv)'s zero count.
- **Add 2.6 item 10:**
  > `getForegroundInfo()` is not overridden and needs no override: no request is expedited (`git grep setExpedited`
  > prints nothing), WorkManager calls it on API < 31 only for expedited work (BUG-029 L8), and the 2.9.1 default
  > throws `IllegalStateException("Not implemented")`. A later story that expedites a request must add it.
  > Android 15's rule that `BOOT_COMPLETED` receivers may not start `dataSync` services is moot: the boot backup is
  > `REGULAR` and never asks.

  Add the first sentence to hand-over item 6. Also add there that 2.9.1's `SystemForegroundService` has no
  `onTimeout` (Android 15 six-hour budget, now only for user-started runs).
- **Section 9:**
  - **Set-up step 4.** Add `shell dumpsys package com.zegoggles.smssync | grep -E "POST_NOTIFICATIONS: granted"`
    (read only). If it is not granted, (iii)'s notification condition is replaced by `shell dumpsys activity services
    com.zegoggles.smssync` showing the `SystemForegroundService` record with `isForeground=true` and `foregroundId=1`,
    and this is recorded as such.
  - **(i) step 3.** Chain after the start line `shell dumpsys activity processes com.zegoggles.smssync | grep -E
    "procState|curProcState|setAdj"` and `shell dumpsys activity services com.zegoggles.smssync`. Record both in
    `AC12-emulator.txt` as the process state at the start.
  - **The Record filter.** Add `|WM-SystemJobService|WM-GreedyScheduler`, and record which one started the INCOMING
    work.
  - **(i) Fail rule.** Add: "an 'unrecoverable error' with a `CancellationException` after a JobScheduler stop line
    is 'stopped by the system', recorded and repeated once, not a fail."
  - **"What it does not prove".** Add: "OEM background restrictions on the owner's phones; the stop of a long
    automatic run (10 minutes, Doze, quota); the Android 11 change from foreground to ordinary work for automatic
    runs".

## Could (not counted; for the producing agent's discretion)

- **A refusal on a retry.** Add one case to F1: `setRunAttemptCount(1)` with the throwing updater, then `Success`,
  one warning, and "starting backup, type=MANUAL, attempt=1".
- **Cancellation during the pending future.** A variant of F12 that cancels the job while it is suspended on
  `PendingFutureForegroundUpdater.future`. Expected: a `CancellationException`, no warning, no `create()`.
- **What F13 proves.** In 2.1 (b), add: "F13 proves only HotSpot linkage in a sandbox without the class; it is not
  evidence about ART's verifier."
- **Boot step (ii).** Add the outcome "not reached: <reason line>" for a missing start with a reason other than
  `WITHIN_QUOTA`.
- **Restore and the default-SMS role.** Record in 2.6 that whether holding the default-SMS role exempts a restore
  from the background-start rule was not read. The helper is correct either way.

## What I could not verify

- **Recalled, not verified:**
  - the hard 10-minute job limit on Android 11 and earlier;
  - Android 16's job runtime quotas;
  - the effect of a denied `POST_NOTIFICATIONS` on a foreground notification;
  - the Android 15 timeout consequence of a missing `onTimeout`;
  - ART's soft-verification treatment of unresolvable types;
  - R8's handling of `-dontoptimize` in AGP 9, and whether it stubs or outlines newer library types;
  - whether the default-SMS role is an exemption.
- **Not checked against the source or the books:** the plan's book chapters. I did not re-read them; the plan says
  the same.
- **Not read:** section 3.4 after plan.md line 599 (the test bodies F1 to R5). I relied on 5.1, 5.2 and the mutation
  table for what they assert.
- **Not run:** the scratch compile and the simulations, which I did not re-run.

## Overall Verdict

**Verdict:** PASS (with 2 recommendations)

**Rationale:** The design and the exception boundary are right and are justified line by line:
- the refusal is caught via `IllegalStateException`, so the API 31 type stays out of the exception table;
- cancellation is rethrown first;
- the type exceptions stay loud;
- the API 31 reference is isolated in a holder behind `SDK_INT`.

The tests drive all three shapes WorkManager can deliver, including the pending-future shape production uses. P-1
was settled offline with SHA-verified jars, and the plan is honest about what Robolectric at API 24 does not prove.
Two criteria carry a recommendation:
- **Criterion 6:** check the release dex through the mapping, where R8 could actually move the reference.
- **Criterion 8:** state the costs of running automatic backups as ordinary jobs:
  - the 10-minute stop, unconditional on Android 11 (recalled);
  - the unlimited batch;
  - the BUG-024 ERROR line before U-074, which section 9 would misread;
  - Doze and constraint stops.

  Also under Criterion 8:
  - correct 2.6 item 8: 2.9.1 already has the service-side refusal catch;
  - record why `getForegroundInfo()` and the `BOOT_COMPLETED` rule do not apply;
  - make the device evidence show process state, the starting scheduler and the notification permission.

**Iteration:** 1

**Rework routing (FAIL only):** Not applicable (PASS).

## Phase Completion Report
---
artifact_id: "U-075"
phase: "plan-gate/expert-depth-check"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-024/U-075/expert-depth-check.md"
---
