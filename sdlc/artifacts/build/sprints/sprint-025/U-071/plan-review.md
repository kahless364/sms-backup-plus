---
artifact_type: gate-result
story_id: "U-071"
gate: plan-review
gate_id: "plan-review"
checkpoint: plan-gate
verdict: PASS
iteration: 3
amendment: "A2 (re-check, ruling 65)"
agent: "Solution Architect (separate instance, plan-review)"
timestamp: "2026-11-21T16:50:58Z"
artifact_reviewed: "sdlc/artifacts/build/sprints/sprint-025/U-071/plan.md"
base_commit: "fd70325ac851febe8447e428ed0192a10710a89d"
blockers: 0
observations: 1
---

# Gate Report: gate-result — U-071

## Gate: plan-review @ plan-gate

## Verdict: PASS (iteration 3, Amendment A2 re-check)

## Summary (Amendment A2)

Ruling 65 sets the scope: B-8 and the self-test table only. The plan is uncommitted, with SHA-256 prefix `eb3c19ed51119212`; HEAD is `fd70325a`. I read nothing under `app/`.

**Result.** B-8 is resolved, and the self-test claims I spot-checked hold. Nothing outside A2's stated change moved. There are no open blockers.

### Checks

**1. B-8 resolved.**
- **The tool change.** I extracted `r8_check.py` from the plan as it now stands. `git diff --no-index` against my A1 extraction shows exactly one changed line: the `section` mode's `open(…, 'w', encoding='utf-8', newline='\n')`.
- **The 2.10 command** (`plan.md:752`) still strips CR from the jar copy.
- **My reproduction, re-run:** U-068's committed `I5-configuration.txt` was normalised, and section `jar-rules:lib/META-INF/com.android.tools/r8/r8.pro` was compared with `META-INF/com.android.tools/r8/r8.pro` from the cached `dagger-2.59.2.jar`.
  - Identical text: `section-equals-jar-exit=0`, with 0 CR bytes in both files.
  - One word changed in the jar copy: `exit=1`.

**2. The self-test tables. I ran the following myself:**

| Row | What I ran | Result |
|-----|------------|--------|
| 2, 3 | the B-8 comparison above (identical; changed word) | `0` and `1`, as the table states |
| 4 | blank-line fallback: jar copy plus two trailing blank lines, raw comparison, then `grep -v '^[[:space:]]*$'` copies | raw `1`, fallback `0`, as stated |
| 5, 6 | `services` on my A1 test APKs (I2, renamed I3, changed-implementation I3), then CMP's `grep '^PAIR '` plus `git diff --no-index --quiet` | renamed `0`, changed `1`, as stated |
| 7, 9 | `seeds-diff` on `seeds-core` files written by `r8_check.py core` (CRLF) with an LF `--prev-ksp` file and fake generated roots at the plan's relative paths; a `Hilt_App` member at I4, then the same line at I3 | `MATCH Hilt_*`, exit 0; at I3 `NO MATCH`, exit 1, as stated |
| Table 2, "same producer" | CMP's byte comparison of `seeds-core.txt` written twice by `core` from the same input | both CRLF (2 CR in 2 lines), `exit=0`. So the "same producer, same line endings" claim holds |
| Machine fact | `python -c "print('a')" > f` | `b'a\r\n'`. Python's redirected stdout is CRLF here, as A2 says |
| 13 | CMP's `$(grep …)` lines over a mixed CRLF/LF `r8.txt` | no CR in the resulting line |

**3. Nothing else moved.**
- Apart from the one `section` line, the tool is byte-equal to my A1 extraction.
- `services` (rows 5 and 6) and `seeds-diff` (rows 7 and 9) behave as they did in the A1 re-check.

### Observations (ruling 65 item 3: can only cause a false stop)

**O-25 (low): a fresh checkout of committed evidence can give a false stop.**
- **Where:** A2's own carried note (`plan.md:~222`).
- **What:** same-producer comparisons of CRLF evidence files written in an earlier session (for example `<p>-seeds-core.txt`, `<p>-jacoco-classes.txt`) rely on the working-tree copy keeping its CRLF. A fresh checkout or a `git restore` of those files would rewrite them under `core.autocrlf=true`. A byte comparison could then differ by line endings only.
- **Why it is not a blocker:** it can only make a comparison report "different", so the developer stops and reports. It can never pass a real difference, and no step of the plan restores evidence files.
- **Fix:** recorded only; the build's stop-and-amend route handles it if it occurs.

### Open blockers

None.

## Phase Completion Report (Amendment A2)
---
story_id: "U-071"
phase: "plan-gate/plan-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-025/U-071/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---

---

# Amendment A1 record (superseded by the Amendment A2 re-check above)

## Verdict: FAIL (iteration 3, Amendment A1 delta re-check, superseded)

## Summary (Amendment A1)

I reviewed only what Amendment A1 changed (`plan.md:139-165`) and what those changes touch. The plan is uncommitted, with SHA-256 prefix `d96504d043853c39`; HEAD is `fd70325a`. I read nothing under `app/`.

**Result.** B-7 is resolved, and the other amended items are consistent. The amendment introduces one new blocker, B-8: the new section-versus-jar comparison (`plan.md:691`) cannot pass in Windows Git Bash. It is a one-word fix.

### Checks

**1. B-7 resolved.**
- **The data is written before it is read, at every increment.** EV-B writes `$E/I<k>-services.txt` (`:1573`) from that increment's own release APK and `mapping.txt`, and EV-B runs before CMP. CMP (`:1601-1603`) reads I0's, `<p>`'s and its own `PAIR` lines, all of which are in STD or I0SET. So I3's `clean` no longer matters.
- **2.11 is now mechanical.** The Q-13 row (`:725`) pairs I2's and I3's `ENTRY` lines by original name and names the stops. D101 (`:2304`) and D44 carry them.
- **Test of the tool.** I extracted `r8_check.py` verbatim (`:1219-1428`; `zipfile` imported) and built zips and mappings in scratch:
  - an I2 APK with `q4.b0`, `v4.t` and an unmapped `CharsetProvider` entry, against an I3 APK where the same services are renamed `b0`, `t` and the implementations `c`, `d`: the `PAIR` lines are equal (`services-pairs exit=0`), and the `ENTRY` lines show the renames;
  - a changed implementation: `exit=1`;
  - an unmapped (non-obfuscated) entry whose APK name changed: shown as `(unmapped)`, and its `PAIR` line differs, so `exit=1`, the stop 2.11 names;
  - `#` comments and blank lines in a services file are ignored.

**2. Other amended items.**
- **O-21 (`--prev-ksp`).** A removed `RestoreWorker_HiltModule` gives `MATCH … (generated at the previous increment)` with the previous KSP list, and `NOT-GENERATED` without it.
- **Tightened refusals.** `androidx.work.impl.WorkDatabase_Factory` at I4 gives `NO MATCH`. Lines under `androidx.work.`, `androidx.core.app.`, `androidx.profileinstaller.` and `androidx.room.`, and non-`smssync` lines at I5, are refused. All of this is stricter than ruling 58, only in the safe direction, and D42 states it (`:2245`). D43 is unchanged.
- **O-22.** The "after dropping the leading `lib/`" wording is at `:689`.
- **O-23.** `B29_ID=U-075` and its path are at `:1738`; `WorkerForeground.kt` is named at `:46` and `:1745`, and is added to the I5 F-1 grep at `:1931`.
- **O-24.** U-069's expected drift is `worker/BackupTriggerWorker.kt` only (`:1744`), matching `git diff --stat 7c5e6073 HEAD -- app/src/main`.
- **Counts.** The brace lists hold 28 entries (STD), 40 (I0SET) and 9 (TOOLS), equal to the stated counts. The Writers list names `services` under EV-B.

**3. Breakage check.**
- Every `D` row reference resolves.
- The `services`, `seeds-diff` and CMP commands work in Git Bash.
- One new command does not (B-8).

### Blocker

**B-8: the section-versus-jar comparison always fails on Windows.**
- **Where:** 2.10, `plan.md:691` (ruling 48 evidence, contract-verification O-15):

  `r8_check.py section … $WORK/sec.txt; unzip -p <jar> <path> | tr -d '\r' > $WORK/jar.txt; git -c core.autocrlf=false diff --no-index --quiet -- $WORK/sec.txt $WORK/jar.txt` → `0` expected.
- **Why it fails:**
  - `r8_check.py` writes its outputs in Python text mode with no `newline=` argument. On Windows that writes CRLF, so `sec.txt` has CRLF line ends.
  - The jar side is stripped of CR, so it has LF.
  - `core.autocrlf=false` compares bytes, so the two files never compare equal.
- **What I ran:** U-068's normalised `I5-configuration.txt`, section `jar-rules:lib/META-INF/com.android.tools/r8/r8.pro`, against `META-INF/com.android.tools/r8/r8.pro` from the cached `dagger-2.59.2.jar`. That is the jar U-068 built with, read from the Gradle cache only.
  - `section-equals-jar-exit=1`, with identical text: `cat -A` shows `^M$` on every section line.
  - The fallback the plan gives (repeating on `grep -v '^[[:space:]]*$'` copies) removes blank lines only, so it fails the same way.
  - With CR stripped from the section side too, the comparison passes (`exit=0`).
- **Effect:** every section that ruling 48 should accept at I4, I5 or I6 (Dagger's `r8.pro`, a Kotlin stdlib section, an androidx.hilt section) would fail its proof and stop the story. It is a false stop, not a false pass. The planner's test covered only the `section` output, not this comparison.
- **Fix:** in `:691`, replace `$WORK/sec.txt` in the comparison with a CR-stripped copy:

  `…; tr -d '\r' < $WORK/sec.txt > $WORK/sec-lf.txt; unzip -p <jar> <path in jar> | tr -d '\r' > $WORK/jar.txt; git -c core.autocrlf=false diff --no-index --quiet -- $WORK/sec-lf.txt $WORK/jar.txt; …`

  Alternatively, open the output with `newline='\n'` in the `section` mode. Apply the same `tr -d '\r'` to both copies in the blank-line fallback.
- **The tool's other CRLF outputs are harmless.** `normalize`, `core` and `services` also write CRLF, but every comparison of them is tool-to-tool (both sides CRLF) or reads with universal newlines.

## Phase Completion Report (Amendment A1, superseded)
---
story_id: "U-071"
phase: "plan-gate/plan-review"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-025/U-071/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---

---

# Iteration 3 record (superseded by the Amendment A1 re-check above)

## Verdict: FAIL (iteration 3, superseded)

## Summary (iteration 3)

I reviewed iteration 3 of the plan: 2,566 lines, uncommitted in the working tree, SHA-256 prefix `f403d474b59dd48d`. The hash was the same at the start and the end of the review.

**Base.** HEAD is `e40a8f80`. U-069's code commit (`d7a69974`) has landed. Since PLAN_HEAD the build-path drift is exactly `2 2 app/build.gradle` at `@@ -579` and `@@ -586`, as the plan predicts. The only `app/src/main` drift is `worker/BackupTriggerWorker.kt`. Nothing under `app/build` was read.

**Result.**
- B-6 is resolved. The rows and the checker carry ruling 58 consistently.
- Ruling 48 is carried for Dagger's jar rule at I4, a Kotlin stdlib section at I5 and androidx.hilt sections at I6.
- The U-075 (BUG-029 fix) placement is handled by D2, D5, D6, D41 and I0 step 1.
- O-12, O-13, O-15, O-17 and O-18 are resolved.
- Every `D` and `R` cross-reference resolves: D1 to D101 and R1 to R22 are all defined.

**Open blocker:** one, new in the iteration-3 text, B-7. 2.11's "recorded" rule for repackaging at I3 asks for old-to-new pairs that no step keeps the data for.

## Iteration-2 findings, re-checked in the plan text

| Finding | Status | Line checked |
|---------|--------|--------------|
| B-6 ruling 58 | **Resolved** | D42 `plan.md:2151` (conditions (a) to (d), I3 outside the ruling); 2.9 `:561`; 2.10 R8-3 `:598-627` (including the `usage-core` exemption); SC-P6 `:2304` "Ruled"; R20 `:2329`; CMP `:1516`, `:1526-1527`. All agree with each other and with the ruling's final form |
| O-12 stale 2.5 row | **Resolved** | `:463` |
| O-13 D18 restore form | **Resolved** | `:2127`: "as D15 (`gradlew` with `core.autocrlf=false`)" |
| O-14 / ruling 48 | **Resolved** | R8-5 `:644-646`; D43 `:2152`; `:650-652`; condition `kept-names.txt vs <p>` `exit=0` |
| O-15 closure count | **Resolved** | `r8_check.py` closure now prints `NOT-IN-MAPPING`; CMP `:1517` compares `manifest-classes-in-mapping` with `<p>`; D42 stops on a lower count |
| O-16 seeds at I3 | **Resolved** | D42, 2.10 `:621-623`, R20 |
| O-17 no `LAST` | **Resolved** | 4.R `:1593-1594` |
| O-18 IC-5 foreground | **Resolved** | `:2379-2382`. Both the backup and the restore are started from `MainActivity`, which stays on screen; no setting that schedules a backup is toggled. IC-5 does not depend on a background start |

## Checks requested for iteration 3

**1. `seeds-diff` (ruling 58).** I extracted `r8_check.py` verbatim (`plan.md:1177-1345`, imports `fnmatch, os, re, sys` present) and ran `seeds-diff` in scratch on inputs I made:

| Case | Result |
|------|--------|
| `+ …service.BackupWorker_HiltModule: BackupWorker_HiltModule()` at I4, source present under the generated root | `MATCH *_HiltModule`, exit 0 |
| the same line at I5 | `NO MATCH`, exit 1 |
| `+ com.google.crypto.tink.proto.HmacKey: int version_` at I4 | `FORBIDDEN`, exit 1 |
| `+ …BackupWorker: BackupWorker(…,int,kotlin.jvm.internal.DefaultConstructorMarker)` at I5 | `FORBIDDEN`, exit 1 |
| identical inputs | `changed=0`, exit 0 |
| `run$default(` member at I5 | `MATCH \$default\(`, exit 0 |
| app-package `*_Factory` with no generated source at I4 | `NOT-GENERATED`, exit 1 |
| the Hilt line at I3 | `NO MATCH`, exit 1 |
| `androidx.work.impl.Foo_Factory` at I4 | `MATCH` on the name alone, which the ruling permits outside the app's package |

The tool, D42, 2.9, 2.10, SC-P6 and R20 agree with each other and with the ruling's final form. See O-21 for one case where the tool is stricter than the ruling.

**2. Ruling 48.** R8-5 and D43 accept a section that is new or changed and comes from the raised component's own jar, when the developer shows it inside that jar and the kept-name set is unchanged. This covers Dagger's `r8.pro` at I4, a Kotlin stdlib section at I5 and `hilt-work`/`hilt-common` at I6. See O-22 for one wording point.

**3. Section 2.11 and D101.**
- The facts match the supplement (`:180-227`):
  - Q-13 (9.1) repackaging into the unnamed package by default;
  - Q-14 (9.2) wildcard `-keepattributes` no longer keeps the `RuntimeInvisible…` attributes;
  - nothing else bears on this build.
- The plan's reading of U-068's files is correct:
  - `-keepattributes *Annotation*` is the only wildcard (`I5-configuration.txt:452`), and the AGP default names its attributes in full (`:108`);
  - the release APK has `META-INF/services/q4.b0` and `v4.t` (obfuscated) and `java.nio.charset.spi.CharsetProvider` (`I5-apk-entries-release.txt:61-63`).
- What each check will see at I3 is stated correctly. Identity lines, seeds, usage and the closure use original names and are unaffected by repackaging; the configuration sections are unaffected, because repackaging is a default, not a rule.
- The Q-14 line is sharp enough to apply. The Q-13 line is not (B-7).

**4. U-075 (BUG-029 fix), now in `sprint-024`.**
- **D2** (`:2111`) requires it to be `done`. Bug records do reach `status: done` (BUG-022).
- **I0 step 1** (`:1631-1656`) expects its commits through `$B29_ID`, records the `app/src/main` drift by story, and greps its status through `$B29_STORY`.
- **D5** (`:2114`) stops on unexplained `app/src/main` drift.
- **D6** (`:2115`) stops a moved anchor for an SA re-anchoring amendment and never adapts it.
- **D41** (`:2150`) stops at I0 if the manifest invariants have moved.
- U-075's own AC-8 rules out manifest and build changes (`U-075-…md:119`), and its new tests are not in the NEW3 classes. So nothing in this plan's lists changes. See O-23.

**5. Breakage check.**
- The new EV-B commands work in Git Bash: `unzip -p` with a quoted `classes*.dex`, `grep -a -o`, `dexdump -f` on a work-folder copy, and `unzip -p … | sha256sum` for the baseline profiles.
- No new evidence file is listed without a writer; the new lines go into the existing `I<k>-r8.txt`.
- Cross-references resolve.

## Findings (iteration 3)

### Blockers

**B-7: the Q-13 "old-to-new pairs" at I3 need data that no step keeps.**
- **Where:** 2.11 (`plan.md:683`, "Recorded" column): "each `META-INF/services/<name>` entry whose name changed from one obfuscated name to another (U-068's `q4.b0`, `v4.t`), listed old-to-new in pairs with Q-13". The "A stop" column: "a `META-INF/services` entry named after a non-obfuscated class changes". D44 and D101 (`:2153`, `:2210`) rest on that column.
- **What:** a pair can be matched only through the original interface name behind each obfuscated name. That name is in `<p>`'s `mapping.txt`, and no step keeps it:
  - EV-B records only `mapping-lines`, the identity lines and the header lines (`:1463-1483`);
  - G(3) runs `clean`, so I2's `mapping.txt` is gone at I3.
- **What happens:** the developer can neither pair `q4.b0` and `v4.t` with their new names, nor show mechanically that an old name was obfuscated, without judgment. That is wrong evidence, or a stop the plan did not intend.
- **Fix:** add one line to EV-B's `I<k>-r8.txt` block:

  `unzip -Z1 $RA | grep '^META-INF/services/' | while read -r e; do n=${e#META-INF/services/}; echo "service $n <- $(grep -F " -> $n:" $MAP/mapping.txt | grep -v '^ ' | cut -d' ' -f1)"; done`

  Then reword 2.11's Q-13 row:
  - **Recorded:** each `service` line whose original name (after `<-`) is equal to a line in `<p>`'s `I<p>-r8.txt` while the entry's name changed, listed as a pair by that original name; the number of `service` lines is equal.
  - **A stop:** a `service` line with an empty original name (a non-obfuscated entry) whose name changed, or an original name present at `<p>` and missing at I3.

### Observations (non-blocking)

**O-21 (medium): `seeds-diff` refuses a removed generated class.**
- **Where:** `r8_check.py seeds-diff` (`plan.md:1300-1312`).
- **What:** for an app-package class, the tool checks for the source only under the **current** generated roots. A `-` line for a generated class that Hilt 2.60.1 or androidx.hilt 1.4.0 no longer generates therefore prints `NOT-GENERATED` and stops. I confirmed this with an empty root: `NOT-GENERATED *_HiltModule - …`, exit 1. The ruling's "its source is shown to be under the generated-sources folder" can be met for such a class from `<p>`'s `I<p>-ksp-files.txt`.
- **Effect:** the tool is stricter than the ruling, in the safe direction. The orchestrator decides at the stop.
- **Fix, if wanted:** for `-` lines also accept `release/java/<path>/<Top>.java` listed in `$E/<p>-ksp-files.txt` (pass it as an extra argument).

**O-22 (low): the jar path does not literally equal the section key.**
- **Where:** `:650-652`: "the path in the jar equal to the section key's path".
- **What:** the `jar-rules:` keys carry the transform's `lib/` prefix (`jar-rules:lib/META-INF/com.android.tools/r8/r8.pro`), but inside the jar the entry is `META-INF/com.android.tools/r8/r8.pro`.
- **Fix:** "equal after dropping the leading `lib/`".

**O-23 (low): name U-075 in the planning state.**
- **Where:** the planning-state paragraph (`:38-42`) and I0 step 1 (`:1649-1650`) call it "the BUG-029 fix story". `$B29_ID` and `$B29_STORY` are taken from the sprint plan.
- **What:** the story now exists as U-075 at `sdlc/artifacts/build/sprints/sprint-024/U-075-automatic-backup-runs-when-the-app-is-off-screen.md` (sprint 024, wave 4, `e40a8f80`).
- **Fix:** write that id and path in the planning state, so that the developer does not have to resolve the placeholders.

**O-24 (info): I0's expected `app/src/main` drift for U-069.**
- **Where:** I0 step 1's expected list (`:1653`) names "U-069's `BackupTriggerWorker.kt` and its scheduler file".
- **What:** U-069's commit changed only `worker/BackupTriggerWorker.kt` (`git diff --stat 7c5e6073 HEAD -- app/src/main`).
- **Effect:** none. The developer records what is there, and nothing extra appears.

### Not reached (iteration 3)

- I did not read the release APK's dex for the worker-name counts. They are record-only, and `app/build` was not read.
- I did not run `closure` on a live mapping.

## Phase Completion Report (iteration 3, superseded)
---
story_id: "U-071"
phase: "plan-gate/plan-review"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-025/U-071/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---

---

# Iteration 2 record (superseded by iteration 3 above)

Iteration 2: verdict FAIL, 1 blocker (B-6), 9 observations. Timestamp 2026-11-21T15:59:30Z, base `0c99149e7e1e10027cdb2a5a2417e40f4fef83e0`.

## Verdict: FAIL (iteration 2, superseded)

## Summary (iteration 2)

I reviewed iteration 2 of the plan: 2,390 lines, uncommitted in the working tree, SHA-256 prefix `0668ef8bbc61640d`. The hash was the same at the start and the end of the review.

**Base.** HEAD is `0c99149e`. `git diff --quiet 9aa6b76c HEAD` gives 0 over the build files, the wrapper files, `.github` and `code-location.md`. So the iteration-1 simulation (39 edits applied in increment order, every count) still applies to the same blobs. The only section-3 text that changed is E14.1's new text (`plan.md:728`). It is still three comment lines, with no sweep or pre-release hit, so the I14 numstat `9 9`, the 34-line `//` check and the AC-22 (a) counts stand. The U-069 working-tree changes under `app/` were not touched.

**Result.**
- **Iteration-1 findings:** all five blockers and all eleven observations are resolved in the plan text itself. The lines I checked are in the table below.
- **New in iteration 2:**
  - I extracted `r8_check.py` verbatim (`plan.md:1082-1207`) and ran it on U-068's committed evidence. It reproduces the plan's L26 result: 28 sections, 28 keys, `ALL EQUAL`, and every `rules` check `OK`. It fails correctly when the Tink rule is removed.
  - The new CM step 4 listing behaves as stated. I tested it read-only on files in this checkout: it lists modified and untracked files and omits unchanged tracked ones.
  - The backup-descriptor commands work in Git Bash on the APKs now in `app/build/outputs`. `aapt2` prints LF, the paths resolve to `res/xml/backup_descriptor.xml` and `res/mt.xml`, and each gives the 4 lines L27 states.
  - I checked every name in the Writers list against the step that writes it, and every new step's inputs against the step that writes them.
- **Open blocker:** one, B-6. Ruling 58 contradicts row D42, so the plan stops at I4 to I6 where the ruling says accept and record.

## Iteration-1 findings, re-checked in the plan text

| Finding | Status | Line checked |
|---------|--------|--------------|
| B-1 `I1-gradle-version.txt` written by no step | **Resolved** | `plan.md:1600-1602`: "Sanitised (whole file) to `$E/I1-gradle-version.txt`" |
| B-2 `gradlew` stages nothing | **Resolved** | `:1594-1599`: `gradlew-unchanged-exit` expects `0`. `:1410-1416` CM steps 4 and 5: the expected set comes from `git status --porcelain --untracked-files=all`; I tested this read-only. `:1895`: I1 row. `:2049`: D99 |
| B-3 SC-P1 not carried | **Resolved** | D43 `:1993` carries ruling 48 and the carry-forward against I0. 2.9 `:515-517`, `:526`. 2.10 `:573-593`. CMP expected `:1375-1379`. 2.5 `:418` |
| B-4 `worker` 0.90 | **Resolved** | D60 `:2010` (breach goes to D38; a one-line drop is recorded). 2.6 `:435`. L17 `:139`. H-9 is gone. No "zero spare" text is left (grep) |
| B-5 supplement form | **Resolved** | 2.2 `:280-284` ("table value", "checksum", "stated fact"). 2.3 `:335-368`. D1 `:1951`. D13 `:1963`. I8 `:1739-1742` |
| O-1 resume compares HEAD | **Resolved** | 4.R `:1445-1456`: newest `U-071 S2-` commit = `LAST`, and no foreign commit on the story's paths |
| O-2 no work folder | **Resolved** | `:1438-1442`: `mkdir -p`; what is lost is stated; the two `$WORK` copies are recreated |
| O-3 plain `:app:dependencies` | **Resolved** in the steps (`:1271`, D97 `:2047`). See O-12: the 2.5 row was not updated |
| O-4 I7 step order | **Resolved** | `:1717-1724`: DEP(7) is step 5, A21 is step 6 and reads "step 5's test classpath" |
| O-5 later cases keyed on names | **Resolved** | I0 step 13 `:1560-1566` (classification by fixture and log, "never from its name alone"); I5 step 5 `:1684-1685`; D64 `:2014` |
| O-6 CRLF `gradlew` on restore | **Resolved** in D15, D17 and 4.R (`:1965`, `:1967`, `:1466-1468`). See O-13 for D18 |
| O-7 D7 baseline | **Resolved** | I0 step 6 `:1536-1539` (`$E/I0-d7.txt`, in I0SET `:1889`); I7 step 2 `:1709-1711` compares counts with I0 |
| O-8 SC-2 merged-manifest check | **Resolved** | `:1539-1540` |
| O-9 E14.1 KGP minimum | **Resolved** | `:728`: "AGP 9 built-in Kotlin needs KGP >= 2.2.10 (…agp-9-0-0-release-notes)" |
| O-10 checksum host | **Resolved** | `:1571-1577` |
| O-11 Robolectric content providers | **Resolved** | D37 `:1987`; 2.8 `:503-504` |

## Findings (iteration 2)

### Blockers

**B-6: ruling 58 (SC-P6) is contradicted by D42.**
- **Where the plan contradicts it:**
  - D42 (`plan.md:1992`) ends: "A `seeds-core` difference that consists only of members of regenerated Hilt or Kotlin code is still a stop: the orchestrator decides (R20)."
  - 2.9 (`:524`) makes R8-3 `=` at every increment.
  - 2.10 R8-3 (`:557-559`) calls it a "hard stop".
  - 9.2 SC-P6 (`:2137`) still says "Ruling asked".
- **What ruling 58 says:** such a difference is accepted and recorded when all of these hold:
  - every changed line names a class generated by the component raised in that increment: the Hilt/Dagger patterns at I4 and I6, and the Kotlin compiler-generated members at I5 only;
  - each line is listed with the pattern it matched;
  - R8-1, R8-2 and R8-4 hold;
  - no `com.google.crypto.tink.` line and no worker-constructor line changes.
- **What happens:** the rows stop the developer where the ruling says continue.
- **The plan's own proposal does not match the ruling either.** The SC-P6 text (`:2137`) allows the Hilt and Kotlin patterns together at I4 to I6. The ruling splits them by increment, and requires each changed line to be listed with its matched pattern.
- **Fix:**
  - Replace D42's last sentence with: "**Ruling 58 (SC-P6):** a `seeds-core.txt vs <p>` difference is accepted and recorded when every added or removed line names a class generated by this increment's raised component — at I4 and I6 the Hilt/Dagger patterns `Hilt_*`, `*_Factory`, `*_HiltModule`, `*_AssistedFactory*`, `*_MembersInjector`, `hilt_aggregated_deps.*`; at I5 only the Kotlin compiler-generated members `$default`, `access$…`, `Companion` — each line written into the log with the pattern it matched, R8-1 (four prefixes), R8-2 and R8-4 hold, and no line of the `com.google.crypto.tink.` prefix or of a `@HiltWorker` class's constructor changes. Any other `seeds-core` difference, or any at another increment, stops."
  - In 2.9 `:524`, write "= `<p>`, or accepted under ruling 58" in the I4, I5 and I6 cells.
  - In 2.10 `:559`, add "(except as D42 records under ruling 58)".
  - Mark 9.2 `:2137` "Ruled (ruling 58)", and reword H-2 (`:2079-2082`), R20 (`:2162`) and the Verdict (`:2377`).
  - The evidence goes into LOG, which every commit lists, so no commit list changes.

### Observations (non-blocking)

**O-12 (low): the 2.5 row on the dependency report is stale.**
- **Where:** 2.5 (`plan.md:426`): "One `:app:dependencies` report per DEP step … Not chosen: Two `--configuration` runs … Half the runs".
- **What:** this contradicts DEP (`:1271`) and the Iteration 2 table (`:79`).
- **Fix:** "Two `--configuration` reports per DEP step (U-060's form) | no run resolves configurations the writer pass never covers | one more short run per DEP | one plain `:app:dependencies` | it may fail verification on androidTest or lint classpaths (D97)".

**O-13 (low): D18 restores the wrapper without the LF form.**
- **Where:** D18 (`:1968`), "restore the wrapper files".
- **What:** it does not say "as D15". D16 and D17 do.
- **Fix:** "restore the wrapper files as D15 (`gradlew` with `core.autocrlf=false`)".

**O-14 (medium): R8-5's allowed sections do not match where Dagger's and Kotlin's rules sit, so I4 and I5 can stop where ruling SC-P1 would accept.**
- **Dagger at I4:** in U-068's `I5-configuration.txt:623-628`, `jar-rules:lib/META-INF/com.android.tools/r8/r8.pro` is Dagger's jar rule (`-identifiernamestring @dagger.internal.IdentifierNameString`). Dagger is the component raised at I4. R8-5 (`:579`) and D43 (`:1993`) allow only `lib:hilt-android/proguard.txt` at I4. The `jar-rules:` key drops the source jar's name (`:1108-1110`), so the developer must name the jar.
- **Kotlin at I5:** no `jar-rules:` section comes from `kotlin-stdlib` at the start. The 28 keys hold only coroutines, androidx-annotations, protobuf and Dagger. So at I5 the only possible case is an `ADDED` section, and D43 stops on any added section.
- **Fix:**
  - R8-5 and D43 at I4: add "`jar-rules:lib/META-INF/com.android.tools/r8/r8.pro`, shown by `unzip -l` of the resolved `dagger-2.60.1.jar`".
  - At I5: "a `jar-rules:` section CHANGED or ADDED, shown inside the resolved `kotlin-stdlib-2.4.21.jar`".

**O-15 (low): the closure check can pass without checking its count.**
- **Where:** `r8_check.py closure` (`:1181-1193`) and the EV-B expected result (`:1353`).
- **What:** the tool ignores a manifest class that is absent from `mapping.txt`, which is what R8 does with a class it removes. If nothing matches, it still exits 0. The expectation checks `not-identity=0` but not `manifest-classes-in-mapping`. A removed class in the contract prefixes is still caught by R8-3's `usage-core` rule (`:560-562`), so this is not a blocker.
- **Fix:** add to CMP "`manifest-classes-in-mapping` equal to `<p>`'s (27 at U-068)", and print the manifest names missing from the mapping.

**O-16 (low): R8-3 is a hard stop at I3, where AGP 9.5.2 brings a new R8.**
- **What:** a member-level `seeds.txt` difference caused by R8 itself stops at I3. R20 and ruling 58 cover only I4 to I6.
- **Fix:** none needed, because the stricter reading is safe. Name it in R20 and H-2, so that the orchestrator expects a possible decision at I3.

**O-17 (low): a resume after an I0-only session has no `LAST`.**
- **Where:** 4.R (`:1443`, `:1455`). `LAST` is "the last commit id the log names". After a session that ends at I0, which makes no commit, there is no `U-071 S2-` commit, and the first check differs. The session plan (`:2059`) ends session A after I2, so this happens only if a session stops early.
- **Fix:** "`LAST` is `START` when no `U-071 S2-` commit exists yet; then the first command prints nothing and that is expected".

**O-18 (low): IC-5 relies on the app staying on screen without saying so.**
- **Where:** IC-5 (`:2208-2220`).
- **What:** the backup and the restore are both started from `MainActivity` (steps 2 and 3), with the app on screen. So IC-5 does not depend on a background start, and BUG-029 does not affect it as written. But step 4 lets a setting be toggled.
- **Fix:** state it in steps 2 to 4: "the app stays in the foreground until the worker's result line (no Home, no app switch); `third_party_integration` stays off and no setting that schedules a backup is toggled, so no backup starts in the background (BUG-029)".

**O-19 (info): the ordering-constraint table is consistent.**
- I checked the 2.2 item 6 table (`:318-328`) against the design's S2 table (`:564-572`) and against rows D13, D26, D54, D59, D35 and F4. Each edge has the order the design gives and a detector. There is no change.

**O-20 (info): Q-13 to Q-17 are still absent.**
- They are not in the supplement yet, which the brief rules is not a blocker.
- D1 (`:1951`) and D44 (`:1994`) handle the gap: a difference at I3 that only those items could explain stops the run.

### New parts checked and sound (no finding)

- **4.L (long runs).** It runs in the foreground and, if the harness moves the run to the background, the developer waits for the notice and starts nothing else, with no polling. The result is read from the log's own `exit=` line. A cut-off run is D93: one re-run in a later session, a second cut-off stops. This matches how the harness behaved during this review.
- **D92 to D99.** Each row has a trigger that a step can produce and a decision that ends in record, stop or a named recovery. None asks for a forbidden action: D92 never kills a process, D97 never writes metadata, D98 stops for the SA.
- **CM steps 4 and 5 (D99).** I verified the semantics read-only, as stated above. `comm -23` over two `LC_ALL=C`-sorted lists gives the unchanged files, which are recorded. The staged list is compared only with the changed or untracked rest.
- **Writers (4.9, `:1911-1927`).** Every STD (27), VM (4), I0SET (39) and TOOLS (9) entry, and every per-increment name, has a writing step. New reads have earlier writers:
  - `I0-configuration-norm.txt` and `I0-d7.txt` are read by CMP and I7 and written by EV-B(0) and I0 step 6;
  - `I<k>-uptodate.txt` is written by UTD and listed at I2 and I5;
  - the later-case table is written at I0 step 13 and read at I5 step 5.

### Not reached (iteration 2)

- I did not run `r8_check.py closure` or `core` on live `mapping.txt`, `seeds.txt` or `usage.txt`. Those files belong to the U-069 developer's current build, and I limited myself to committed evidence plus the APK reads above.
- I did not re-run the 39-edit simulation. Its inputs are unchanged, and only E14.1's new text was re-checked.
- I did not check 2.1's new "D1a resolution" text in detail beyond its conclusion (9.5.2 stays the target; F1 9.5.1).

## Phase Completion Report (iteration 2, superseded)
---
story_id: "U-071"
phase: "plan-gate/plan-review"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-025/U-071/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---

---

# Iteration 1 record (superseded by iteration 2 above)

Iteration 1: verdict FAIL, 5 blockers, 11 observations. Timestamp 2026-11-21T14:32:47Z, base `9aa6b76c5752c4d9641392e3a91d08102c13f777`.

## Summary (iteration 1)

I reviewed iteration 1 of the plan (1,846 lines) against the story, design DES-MODERNIZATION-014 (S2 table, D1, D3, D5, D7, N-6, D19), U-067's mutation log, U-060's proof script, the research, and the orchestrator's supplement (committed during that review, `c9b3954d`).

**What I re-derived, all by script on `git show HEAD:` copies in scratch (U-069's two lines stood in for):**
- **Old texts:** all 39 edits applied in increment order, each old text occurring exactly once (`cache@v4` twice).
- **Counts:** every numstat and the six `ci.yml` hunks are right.
- **Versions:** all 19 equal the design's D1 table and the story's.
- **End state:** the sweep, AC-22 (a) `1/4/14/5`, AC-22 (b) and final checks (vii) to (ix) are as expected.
- **CI YAML:** the PyYAML parse equals I13 step 5.
- **Wrapper block:** `gradlew_block.py` passes a correct result and fails a one-byte change.
- **Mutations:** equal U-067's log.
- **`vm_proof.py`:** equals U-060's script.
- **`cov_floors.py`:** reads 18 rules, `worker` 0.90 on the stand-in.

## Blockers (iteration 1; all resolved in iteration 2)

- **B-1:** `I1-gradle-version.txt` was in the I1 list (`:1436`), but I1 step 7 (`:1157`) wrote only the raw log.
- **B-2:** the cached 9.5.1 and 9.9.1 `unixStartScript.txt` templates are byte-identical (SHA-256 prefix `81a2c1ec4cad61c1`). So `gradlew` would likely be unchanged and stage nothing, and CM step 5 would fail.
- **B-3:** ruling SC-P1 was not carried. D43 stopped on any `configuration.txt` difference, and the comparison against I0 re-stopped at every later increment.
- **B-4:** ruling 47 was not carried. D60 stopped below 0.95 while the gate passes at 0.90.
- **B-5:** the supplement's ruled form was not carried. 2.2, 2.3 and D1 demanded verbatim quotations.

## Observations (iteration 1; all resolved in iteration 2)

- **O-1:** 4.R compared HEAD instead of the newest story commit.
- **O-2:** no `mkdir` of the work folder in a new session.
- **O-3:** plain `:app:dependencies` had not been tried before.
- **O-4:** I7 steps 5 and 6 were swapped.
- **O-5:** later cases were keyed on name prefixes.
- **O-6:** `git restore` writes a CRLF `gradlew` (260 CRLF). Git for Windows' `sh` ignored CR in a test.
- **O-7:** the D7 scan had no I0 baseline.
- **O-8:** no command for the SC-2 merged-manifest check.
- **O-9:** E14.1 stated an AGP 9.5 KGP minimum without a source.
- **O-10:** the checksum host is `downloads.gradle.org`.
- **O-11:** Robolectric 4.18.1 installs content providers for lazily created applications.

## Phase Completion Report (iteration 1, superseded)
---
story_id: "U-071"
phase: "plan-gate/plan-review"
verdict: "FAIL"
artifact_path: "sdlc/artifacts/build/sprints/sprint-025/U-071/plan-review.md"
story_status: "in-progress"
current_build_phase: "planning"
errors: []
---
