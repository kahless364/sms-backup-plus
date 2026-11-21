---
status: implemented
severity: high
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-015
  - REQ-MODERNIZATION-016
  - REQ-MODERNIZATION-017
design_docs: []
related_stories:
  - U-062
tags:
  - change-record
  - modernization
  - thunderbird-migration
  - platform-level
id: CR-011
title: 'Platform floor and levels for the Thunderbird Android variants: minSdk 24 (was 23), compileSdk 37 and targetSdk 37 (REQ-MODERNIZATION-015, -016, -017 and EPIC-MODERNIZATION-006)'
type: RCR
domain: change-records
summary: 'PO-1/PO-2: REQ-MODERNIZATION-015 sets minSdk 23. Product-owner decision 2026-11-17 (Record 4): consume a fork of thunderbird-android as Android variants, which needs minSdk 24 and compileSdk 37; minSdk is raised to 24 and targetSdk goes to 37 with compileSdk (REQ-MODERNIZATION-017 names no level today).'
updated_at: '2026-11-21T06:38:22.504Z'
---

# CR-011: Platform floor and levels for the Thunderbird Android variants

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-011 |
| **Type** | RCR (Requirement Change) |
| **Severity** | High |
| **Status** | Open |
| **Reported By** | Requirements Analyst, on the product owner's batch approval (Record 4, 2026-11-17) |
| **Date Reported** | 2026-11-17 |
| **Source** | Sprint work: the accepted feasibility analysis of story U-062 (items PO-1 and PO-2) and the product owner's decisions of 2026-11-17; no design or story exists yet for REQ-015, -016 or -017 |

This is one of the seven change records that the product owner approved as a batch on 2026-11-17
(Record 4, last row). Nothing is blocked today. This record describes and proposes text; no
requirement, epic, build file, code or test is edited by it.

---

## Finding

### Description

REQ-MODERNIZATION-015 sets the new minimum Android level to **23** (title, description, AC-5 and
more). On 2026-11-17 the product owner decided to take Thunderbird's mail code from a fork of
`thunderbird-android`, built as **Android variants** (Option 2, mode M2). Against the orchestrator's
recommendation, this route was chosen on purpose (Record 4, "Fork, Android build"). That route needs:

- **minSdk 24** (approved today: 23), and
- **compileSdk 37** (today 35), and REQ-MODERNIZATION-017 names no level at all, and
- the product owner also chose **targetSdk 37** ("Raise to 37 (Recommended)"). M2 does not force
  targetSdk (the analysis says `sdkTarget` 36 is "for upstream's apps only"); this is the owner's own
  choice, not a technical consequence.

The owner "accepted both" (minSdk 24 and compileSdk 37). The consequence for the requirements is:

1. REQ-015 must say 24 everywhere it says 23 as the new floor.
2. REQ-017 must carry level 37, and its minSdk statement must say 24.
3. REQ-016 and EPIC-MODERNIZATION-006 repeat the 23 and must follow.
4. REQ-015, -016 and -017 now **precede** the migration (Part B), not only the library work. The
   analysis condition 1 already requires REQ-015 and -016 first; Record 4 adds REQ-017.

The toolchain versions that M2 and the fork's build need (Gradle, AGP, Kotlin, coroutines, JDK) are
already inside the wording "latest stable at planning time" of REQ-015 and -016; the table in
Evidence and Impact shows which need a text change (none) and which carry a risk instead.

### Source of Truth

- **Product owner decision, Record 4, 2026-11-17** (analysis lines 2170-2173):
  PO-11 route "Fork, Android build"; PO-1/PO-2 "Accept both" (minSdk 24, compileSdk 37; "the
  platform-level work precedes the migration"); PO-2 targetSdk "Raise to 37 (Recommended)"
  ("Not assessed in this analysis: the behaviour changes of the newer platform levels; belongs to
  REQ-MODERNIZATION-017"); and the batch approval (line 2180).
- **Accepted feasibility analysis** `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`:
  Section 4 (lines 364-416; consumption modes M1, M2, M3 at 368-375; platform table at 381-390),
  Q9.a Option 2 (line 924), Q12.d PO-1 and PO-2 (lines 1330-1331), Q12.e conditions 1 and 2
  (lines 1349-1350).
- Upstream pin: `thunderbird-android` commit `ac1f0e85f63c1b600ac443d1fb2d939bffff7de5`.

### Evidence

All repository lines opened on 2026-11-17. Upstream facts are quoted from the analysis and its
citation ids; the upstream repository was not re-opened for this record.

**1. Build values today (verified)**

| File:line | Exact current text |
|-----------|--------------------|
| `app/build.gradle:21` | `compileSdkVersion 35` |
| `app/build.gradle:24` | `minSdkVersion 21` |
| `app/build.gradle:25` | `targetSdkVersion 35` |
| `k9mail-vendored/build.gradle:52` | `compileSdkVersion 35` |
| `k9mail-vendored/build.gradle:54` | `minSdkVersion 21` |
| `gradle/wrapper/gradle-wrapper.properties:4` | `distributionUrl=https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip` |
| `build.gradle:10` | `classpath 'com.android.tools.build:gradle:9.0.1'` |
| `build.gradle:14` | `classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21'` |
| `gradle/gradle-daemon-jvm.properties:2` | `toolchainVersion=25` |
| `app/build.gradle:118-119` | `kotlinx-coroutines-core:1.7.3`, `kotlinx-coroutines-android:1.7.3` |
| `app/build.gradle:90` | `sourceCompatibility JavaVersion.VERSION_17` (app); `k9mail-vendored/build.gradle:76-77` `VERSION_1_8` |

The analysis line citations `REQ-MODERNIZATION-015:34` and `:113` were re-found and are correct.

**2. What the analysis says Option 2 / mode M2 asks for (analysis Section 4, lines 383-390, and Q9.a line 924)**

| Item | Upstream at pin (analysis) | This repository now | Analysis verdict |
|------|----------------------------|---------------------|------------------|
| minSdk | 24 for the KMP Android targets [C-221, C-222]; the Android set adds modules declaring 23 [C-216 to C-219, C-241] | 21 | conflict with the approved 23 (M2 only) |
| compileSdk | 37 for the KMP Android targets [C-221, C-222]; androidx.core and core-ktx 1.19.1 have AAR `minCompileSdk` 37 [C-216, C-217] | 35 | conflict: REQ-017 names no level |
| Gradle | wrapper `gradle-9.7.0-all` [C-231] (the catalogue says 9.6.1 [C-227]; the wrapper runs) | 9.5.1 | no conflict with REQ-015 snapshot 9.9.1 |
| AGP | 9.5.1 [C-226] | 9.0.1 | no conflict with REQ-015 snapshot 9.5.1 |
| Kotlin | 2.4.21 [C-228, C-230] | 2.3.21 | no conflict with REQ-015 snapshot 2.4.21 |
| kotlinx-coroutines | 1.11.0 [C-229] | 1.7.3 | no conflict with REQ-016 snapshot 1.11.0 |
| JDK to build | 21 or newer [C-233 to C-235]; "not tested on JDK 25 (experiment (c) not run)" | 25 | no conflict as declared; untested |
| JVM target | 17 [C-220, C-222, C-224, C-225] | app 17, vendored module 8 | no conflict with the app |

**3. Lines in the five artifacts that state a platform level or a toolchain version (every hit)**

*REQ-MODERNIZATION-015* (`REQ-MODERNIZATION-015-toolchain-and-test-tooling-latest-stable-minsdk-23.md`):

| Line | Exact current text (shortened only where marked) |
|------|---------------------------------------------------|
| file name | `...-latest-stable-minsdk-23.md` |
| 16 (frontmatter title) and 29 (H1) | "Build toolchain and test tooling on latest stable, with minSdk raised to 23" |
| 20-25 (notes) | "Supersedes the literal minSdk 21 statements in REQ-MODERNIZATION-001 (AC-2) and REQ-MODERNIZATION-006 (AC-3, ...)" (no level stated; will need an amendment note) |
| 34 | "and raise `minSdkVersion` from 21 to 23 in `app/build.gradle` (line 24 at the time of writing)." |
| 38-40 | "The one user-visible effect is that Android 5.0 and 5.1 devices (API 21 and 22) stop receiving updates." |
| 52-54 | Snapshot rows: Gradle 9.5.1 to 9.9.1, AGP 9.0.1 to 9.5.1, Kotlin 2.3.21 to 2.4.21 |
| 66 | "Hilt 2.60, androidx.hilt 1.3 or later, and Robolectric 4.16 or later require minSdk 23." (a true fact about those libraries; the floor is now above it) |
| 68-69 | "Four tests in `.../compat/SmsReceiverTest.java` run at `Build.VERSION_CODES.LOLLIPOP` (SDK 21) and cannot run once minSdk is 23." |
| 113-116 (AC-5) | "`app/build.gradle` declares `minSdkVersion 23` and the resolved merged manifest reports `android:minSdkVersion="23"`. ..." |
| 117-124 (AC-6) | "No `Build.VERSION.SDK_INT` comparison or `@TargetApi`/`@RequiresApi` annotation for a level below 23 remains in `app/src/main`. ... with every remaining match at level 23 or above." |
| 125-128 (AC-7) | "The four SDK 21 tests in `SmsReceiverTest` are retargeted to a level of 23 or above, ... `grep -rn "LOLLIPOP\|sdk = \[\?2[0-2]" app/src/test` returns zero matches" |
| 133-135 (AC-9) | "Change records are created on REQ-MODERNIZATION-001 and REQ-MODERNIZATION-006 recording that their literal minSdk 21 statements are superseded by this requirement." (no level named; unchanged) |
| 136-138 (AC-10) | "A user-facing changelog or release-notes entry states that Android 5.0 and 5.1 are no longer supported, ..." |
| 165-169 (AC-17) | "... installed on a device or emulator at API 23 (the new minimum) and on one at a higher API level." |
| 177-180 (Rationale) | "... Hilt 2.60, androidx.hilt 1.3 or later, and Robolectric 4.16 or later are the components that force the minSdk raise ..." |
| 187-188 (Constraint) | "identical before and after, apart from the dropped API 21 and 22 support." |
| 189 (Constraint) | "**JDK 25 stays**: the build runs on JDK 25 via `gradle/gradle-daemon-jvm.properties`." (no change; see Impact) |
| 196-198 (Out of scope) | "runtime libraries (WorkManager, Lifecycle, coroutines), compileSdk and targetSdk, donation removal, and the mail library replacement belong to the other EPIC-MODERNIZATION-006 requirements." (no change: REQ-015 owns neither compileSdk nor targetSdk) |

*REQ-MODERNIZATION-016* (`REQ-MODERNIZATION-016-runtime-libraries-latest-stable.md`):

| Line | Exact current text |
|------|--------------------|
| 18-19 (notes) | "REQ-MODERNIZATION-015 (toolchain and minSdk 23) may need to land first if a library's minimum requires it." |
| 41 | "The build runs on JDK 25 (Gradle 9.5.1 / AGP 9.0.1) with 697 unit tests (2 skipped)." (current-state fact, true today; no change) |
| 127-129 (Constraint) | "minSdk will be 23 once REQ-MODERNIZATION-015 lands. If a target library's minimum SDK or Kotlin version exceeds what the current build provides, this requirement is sequenced after REQ-MODERNIZATION-015 and the story records that dependency." |
| 126 | "Build stays on JDK 25." (no change) |

REQ-016 states no compileSdk or targetSdk level (searched: no match).

*REQ-MODERNIZATION-017* (`REQ-MODERNIZATION-017-platform-level-current-stable-api.md`):

| Line | Exact current text |
|------|--------------------|
| 18-20 (notes) | "The target API level is deliberately not named; AC-1 requires it to be determined and recorded at planning time." |
| 21-22 (notes) | "Sequencing (epic Notes): after the donation-flow removal requirement, and after the toolchain and minSdk 23 requirement (referenced in the brief as REQ-MODERNIZATION-015)." |
| 31-36 (Description) | "Raise `compileSdkVersion` and `targetSdkVersion` in `app/build.gradle` from 35 to the Android API level that is current stable on the date the implementing story is planned. ... This requirement does not name the target level; AC-1 makes determining it an explicit, evidenced step." |
| 40-41 (Context) | "`app/build.gradle` declares `compileSdkVersion 35` (line 21) and `targetSdkVersion 35` (line 25)." (verified true today) |
| 74-78 (AC-1) | "A planning-time record in the implementing story names the current stable API level (number and Android version name), the source URL on developer.android.com, and the date consulted. Evidence that an SDK platform is installed locally is not accepted as evidence of stable status. A level labeled preview, beta, or developer preview on the source page does not qualify." |
| 79-80 (AC-2) | "`compileSdkVersion` and `targetSdkVersion` in `app/build.gradle` both equal the level recorded under AC-1." |
| 86-90 (AC-3) | "... every behavior change that the platform's published documentation lists for apps targeting the new level (and for all apps running on it) ..." |
| 173-174 (Constraint) | "minSdk is 23 once the toolchain requirement lands; if it has not, the story records the minSdk in effect and validates on one emulator at the minSdk as well." |
| 171-172 | "Informational only ... Google Play target-level policy deadlines" (moot for a private app; already flagged by CR-003; not touched here) |

*REQ-MODERNIZATION-019* (`REQ-MODERNIZATION-019-replace-vendored-k9-with-thunderbird-mail-libraries.md`):

| Line | Exact current text | Change? |
|------|--------------------|---------|
| 35 | "`k9mail-vendored/build.gradle`: ... `minSdkVersion 21`; `compileSdkVersion 35`" | No: current-state fact about a module that is deleted |
| 70 | "any conflict with REQ-MODERNIZATION-015 (minSdk 23) and REQ-MODERNIZATION-016 ..." (Part A question 4) | No: it is the question Part A answered; the answer is in the analysis Section 4 |
| 93 (AC-A4) | "beside the versions that REQ-MODERNIZATION-015 and -016 will set" | No: Part A, accepted |
| 117 | "JDK 25 build, enforced dependency verification, and no lowered JaCoCo floor are binding" | No text change; see Impact (M2 and JDK 25) |
| 128 | "Part B starts only after AC-A8 and, if Part A so concludes, after the toolchain versions it needs are in place." | Optional clarification (C-8 below) |

*EPIC-MODERNIZATION-006* (`EPIC-MODERNIZATION-006-dependency-currency-latest-stable.md`):

| Line | Exact current text |
|------|--------------------|
| 25 | "... removed rather than upgraded, and a minSdk raise to 23 accepted." |
| 28 | "... other than the removed donation flow and the dropped Android 5.0/5.1 support)." |
| 39 | "- **Build toolchain and test tooling on latest stable, minSdk 23** — Gradle, AGP, Kotlin," |
| 44-48 | "Hilt 2.60, androidx.hilt 1.3+, and Robolectric 4.16+ require minSdk 23, so `minSdkVersion` is raised from 21 to 23 and Android 5.0/5.1 support ends. This supersedes the literal minSdk 21 statements in REQ-MODERNIZATION-001 (AC-2) and REQ-MODERNIZATION-006 (AC-3, AC-5) ..." |
| 53-54 | "- **Platform level** — raise `compileSdk` and `targetSdk` from 35 to the current stable Android API level, with on-device validation of the affected behavior changes." |
| 82-84 (criterion 2) | "**minSdk**: `minSdkVersion` is 23 in `app/build.gradle`; no `Build.VERSION.SDK_INT` guard for a level below 23 remains in `app/src/main`; ..." |
| 85-88 (criterion 3) | "**Platform level**: `compileSdk` and `targetSdk` equal the current stable API level (verified against the Android developer documentation at planning time), ..." |
| 115 | "1. Build toolchain and test tooling on latest stable, with minSdk 23." |
| 117 | "3. Platform level (compileSdk/targetSdk) on current stable, with on-device validation." |
| 125-129 (Sequencing) | "Requirement 1 first: ... Requirement 5 starts with its feasibility analysis, which can run in parallel with requirements 1 to 4 and whose outcome gates all mail design work." |
| 142-143 | "Dropping Android 5.0/5.1 is a user-facing support reduction; whether to communicate it is a publishing matter and is not decided here." |
| 147 | "Which Android API level is current stable at planning time is not yet verified." |

**4. Other statements outside the five artifacts that still say 23 (not edited by this record)**

- Accepted analysis, Q12.e condition 2 (line 1350): "a backup and restore smoke test completes on an API 23 device or emulator". It is read-only
  (accepted); under minSdk 24 the story that carries condition 2 must use an API 24 device or emulator.
  Also Q4.b/Q12.d rows quote the old target 23 as history.
- `sdlc/artifacts/requirements/modernization/_index.md:8` lists REQ-015 with the old title.
- `sdlc/artifacts/engagement/code-location.md:27-28`: "compileSdk / targetSdk | 35" and "minSdk | 21 (Android 5.0)".
  These are current-state facts and change when the build changes, not now.

---

## Triage

### Classification

- **Type:** RCR. A product owner decision (Record 4) changes an approved requirement's value (minSdk 23
  to 24) and adds a level the requirement left open (compileSdk and targetSdk 37). It is not a DEF:
  the requirements were correct for the route known when they were approved. It is not a DCR: no
  design exists for REQ-015, -016 or -017.
- **Root:** Requirements layer. The first requirement in the chain (REQ-015) carries the floor; REQ-016,
  REQ-017 and the epic repeat it. The route decision itself comes from the ground-truth layer (the
  accepted analysis plus Record 4).
- **Layers Affected:** 1 layer needs text edits now (requirements: 4 files plus the generated index
  row). 5 more layers (build configuration, application code, tests, CI, documentation) are
  changed later by the stories for REQ-015 and -017.
- **Severity Justification:** High. The change touches four approved requirement files and the
  sequencing of the whole migration, and it will shape the acceptance criteria of at least two
  stories. It is not Medium because the floor and levels cross requirements and a developer would get
  a wrong answer from REQ-015 as written. It is not Critical because nothing is blocked: no story, design
  or code exists for REQ-015, -016 or -017.
  Rubric note: read literally, "affects 5+ files" would give Critical. The count of files needing text
  edits now is four requirement files plus one mechanically regenerated index row. The later build, code
  and test files are changed by the stories whatever this record says. The orchestrator may raise it
  to Critical; the next steps are the same.

---

## Impact Analysis

Searches run on 2026-11-17 (read-only). `change_records.layers` is not configured in
`sdlc/config.yaml`; the layers named in the dispatch are used.

### Layer Impact Matrix

| # | Layer | Affected? | Files | Occurrences | Action Needed |
|---|-------|-----------|-------|-------------|---------------|
| 1 | Ground truth: Record 4 and the accepted analysis Section 4 | Ground truth | `thunderbird-mail-libraries-feasibility-analysis.md` | n/a | None (read-only) |
| 2 | Requirements (`sdlc/artifacts/requirements`) | **Yes** | `REQ-MODERNIZATION-015` (title and 13 changes; the file name is only recommended to stay), `-016` (2 changes), `-017` (5 changes), `EPIC-MODERNIZATION-006` (11 changes), `_index.md:8`. `REQ-MODERNIZATION-019`: no edit (one optional note) | 31 changes in 4 files plus 1 index row | Amend in place (Proposed Change) |
| 3 | Design and contracts (`sdlc/artifacts/design`) | No design for REQ-015/016/017; one existing design is affected in a cosmetic way | `DES-MODERNIZATION-006-dead-code-minsdk-cleanup-design.md` mentions "the minSdk raise" (lines 82, 101, 265) for the earlier 21 raise; contracts do not state a platform level | 3 | None. The earlier design is history |
| 4 | Stories and sprints (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | No in-flight work | No story, plan or sprint file exists for REQ-015/016/017/019 (`sdlc/artifacts/stories` holds only BUG-018). Sprint-017, -018 and -019 records mention the requirements as history | 0 active | None |
| 5 | Build configuration | Later (REQ-015 and REQ-017 stories) | `app/build.gradle` (21, 24, 25, comment 153, comment 160), `k9mail-vendored/build.gradle` (52, 54; goes in Part B), `gradle/wrapper/gradle-wrapper.properties:4`, `build.gradle` (10, 14, 17, 21), `.github/workflows/ci.yml:30` | about 13 lines | Stories change them. Not by this record |
| 6 | Application code (`app/src/main`) | Later: cleanup this change makes possible | 6 files for SDK_INT/annotations, 1 manifest (see "Cleanup made possible") | 9 sites | Stories delete the dead branches |
| 7 | Tests (`app/src/test`) | Later | `SmsReceiverTest.java` (27, 32, 37, 42), `WorkManagerSchedulerTest.kt:378` | 5 tests | Stories retarget or delete |
| 8 | Documentation | Later | `sdlc/artifacts/engagement/code-location.md:27-28`, `.github/ISSUE_TEMPLATE/500_bug_report.yml:54-56` (device list), `CHANGES` (if REQ-015 AC-10 stays) | 3 files | Stories update |

Layers touched: 8 of 8. Layers edited by this record's proposal: 1. The severity multiplier does not
apply as a rework risk, because nothing downstream has been built from the old values.

### Which toolchain statements are already inside "latest stable", and which need a text change

| Item (needed by M2 / the fork's build) | Upstream at pin | Existing wording that covers it | Needs a text change? | Risk instead (not a wording problem) |
|----------------------------------------|-----------------|---------------------------------|----------------------|--------------------------------------|
| Gradle | 9.7.0 [C-231] | REQ-015 AC-1 and the table row (snapshot 9.9.1, "latest stable at planning date") | **No**: 9.9.1 is not older than 9.7.0 | This repo is on 9.5.1, so the upstream build cannot run in this build until REQ-015 lands (sequencing; analysis condition 1). Upstream's build on a newer Gradle was not tested |
| AGP | 9.5.1 [C-226] | REQ-015 table row (snapshot 9.5.1) | **No** | Same sequencing. This repo is on 9.0.1. If "latest stable" at planning date is newer than 9.5.1, upstream's own plugins stay on their pin; interplay not tested |
| Kotlin | 2.4.21 [C-228] | REQ-015 table row (snapshot 2.4.21), AC-1 | **No** | This repo is on 2.3.21 because Hilt 2.59.2 cannot read Kotlin 2.4 metadata (`build.gradle:12-13`, per the analysis); REQ-015 already handles the Hilt coupling |
| kotlinx-coroutines | 1.11.0 [C-229] | REQ-016 table row (snapshot 1.11.0) | **No** | This repo is on 1.7.3 until REQ-016 lands |
| JDK to build | 21 or newer [C-233] | REQ-015:189 "JDK 25 stays" (25 satisfies "21 or newer") | **No** | **Upstream's build on JDK 25 was not tested.** If it fails, the options (a different JDK for the included build, or another consumption mode) collide with the epic's Out of Scope ("moving off JDK 25", EPIC-006:71-72) and need a product owner decision. Analysis condition 2 owns the proof |
| JVM target | 17 [C-220] | REQ-015 AC-15 (app 17, vendored 8) | **No** | The vendored module at 8 goes in Part B |
| minSdk | 24 [C-221, C-222] | REQ-015 (23) | **Yes** | Raising from 21 to 24 |
| compileSdk | 37 [C-221, C-222] | REQ-017 names no level | **Yes** (add the decided level) | Needs an AGP that supports 37 and a Robolectric that runs it (REQ-017 AC-13); not verified |
| targetSdk | not bound by M2 | REQ-017 names no level | **Yes** (owner's decision, 37) | Behaviour changes of API 36 and 37 not assessed |

### What raising minSdk from 21 to 24 drops

Android 5.0 (API 21), Android 5.1 (API 22) and Android 6.0 (API 23, including 6.0.1). REQ-015 as
written dropped only the first two. Everything from Android 7.0 (API 24) upward stays supported. For a
private, single-user app that is not published (no store listing and no rollout to consider), the only
question is whether any device the owner uses runs API 23 or lower. The repository does not record the
owner's devices, and the owner has already accepted minSdk 24 (Record 4), so this record does not
re-open it.

### Cleanup this change makes possible (a consequence, not part of this record)

Searches: `Build.VERSION.SDK_INT`, `@RequiresApi`, `@TargetApi`, `VERSION_CODES` over `app/src/main` and
`app/src/test`; version-qualified resource folders; manifest `maxSdkVersion`. The sites below exist only
for levels below 24. They are listed so the stories for REQ-015 can find them. This record edits none of
them.

| File:line | Code | Level | Already in REQ-015 AC-6 list? |
|-----------|------|-------|-------------------------------|
| `compat/SmsReceiver.java:31` | `@TargetApi(Build.VERSION_CODES.KITKAT)` | 19 | yes |
| `compat/SmsReceiver.java:50-54` | `else if (SDK_INT >= KITKAT) {...} else { return false; }` | 19 | yes (the file) |
| `activity/SmsDefaultRoleHelper.kt:74` | `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)` | 19 | yes |
| `activity/MainActivity.java:425` | `@TargetApi(Build.VERSION_CODES.KITKAT)` | 19 | yes |
| `activity/Dialogs.java:172` | `SDK_INT >= ICE_CREAM_SANDWICH` | 14 | yes |
| `calendar/CalendarAccessorPost40.java:23` | `@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)` | 14 | yes |
| `scheduler/WorkManagerScheduler.kt:224-230` | `if (SDK_INT < VERSION_CODES.N) { ... return null }` ("Below API 24 ... broadcast fallback") | 24 | **no: new at 24** (not below 23) |
| `app/src/main/AndroidManifest.xml:40-41` | `USE_CREDENTIALS` with `android:maxSdkVersion="22"` | 22 | no (an XML attribute, outside the grep of AC-6) |
| `app/src/main/AndroidManifest.xml:49-50` | `WRITE_EXTERNAL_STORAGE` with `android:maxSdkVersion="18"` | 18 | no (already moot at the current minSdk 21) |
| `app/src/test/.../compat/SmsReceiverTest.java:27,32,37,42` | four tests at `Build.VERSION_CODES.LOLLIPOP` | 21 | yes (AC-7) |
| `app/src/test/.../service/WorkManagerSchedulerTest.kt:378` | `@Config(sdk = [23])` for `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` | 23 | **no: new at 24**; the existing AC-7 grep `sdk = \[\?2[0-2]` would not find it |

Still needed at minSdk 24 (not dead): `SDK_INT >= Q` guards (`SmsReceiver.java:47`, `SmsDefaultRoleHelper.kt:77,111,135`,
`BackupWorker.kt:533`, `RestoreWorker.kt:707`, `MainActivity.java:242`), `>= O` (`App.java:246,350`), `>= 26`
(`ThemeActivity.java:20,26`), `>= TIRAMISU` (`MainActivity.java:517`). The only version-qualified resource
folder, `res/drawable-anydpi-v26`, also stays. In the vendored module, `DefaultTrustedSocketFactory.java:158,192,198`
and `KeyChainKeyManager.java:35,105` have guards for levels at or below 24; they are deleted with the module
in Part B and should not be touched. `k9mail-vendored/build.gradle:54` (`minSdkVersion 21`) can stay below the
app's value; whether to raise it first is a design question, not a text change. Comments to correct:
`app/build.gradle:153` ("compatible with compileSdk 35 + minSdk 21") and `app/build.gradle:160`;
REQ-015 AC-5 already names the first. `app/lint-baseline.xml` has 10 lines matching
`ObsoleteSdkInt|NewApi`; they were not analysed (REQ-015 AC-8 handles the baseline).

Sequencing consequence (Record 4 and analysis conditions 1 and 2): REQ-015, REQ-016 and REQ-017 are
complete before any Part B row that compiles upstream code. REQ-015 first (toolchain and floor);
REQ-016 and REQ-017 can follow in either order, but REQ-017 needs the AGP and Robolectric versions that
REQ-015 sets. REQ-018 (donation removal) is done (sprint-018), which satisfies the epic's rule that it
precede the platform-level work.

### Scope notes found while analysing

Scope Concern: REQ-017 AC-1 says the target level is determined at planning time and "a level labeled
preview, beta, or developer preview on the source page does not qualify". The owner's decision names 37.
The proposal below follows the decision (37) and keeps AC-1's evidence step as a confirmation. If the
developer.android.com page does not show 37 as stable on the planning date, the story must stop and ask,
not choose. That is the only way both texts hold.

Scope Concern: REQ-017 AC-3 reviews behaviour changes "for apps targeting the new level". Moving from 35 to
37 crosses level 36 as well. A reader could take AC-3 to cover only the pages for 37. The proposal below
names both levels. This is a consequence of the owner's two-level jump, not a change of the decision.

Scope Concern: CR-003 already flagged three sibling wording items as a separate follow-up: REQ-015 AC-10
(a "user-facing changelog or release-notes entry"), EPIC-006 line 68 and lines 142-143 (publishing wording),
and REQ-017 lines 171-172 (Google Play policy). This record edits AC-10 and the epic's lines 142-143 only to
change the list of dropped Android versions. Whether the release-note deliverable itself stays for a private,
unpublished app is not decided here and remains with that follow-up. Applying both in one pass would avoid
editing the same lines twice.

---

## Proposed Change

Nothing below is applied by this record. "Current text" is quoted from the line numbers above.

### Decision: does REQ-017's level belong in this record?

**Yes, in this record, as three small amendments, not in a separate one.** Reasons:

1. REQ-017 line 173 states the minSdk value itself ("minSdk is 23"), so REQ-017 must be edited by this
   record whatever else is decided. Splitting would put one REQ-017 line in CR-011 and the level in
   another record for the same file.
2. The level and the floor come from one answer to one question (Record 4, "Accept both"), they are applied
   at the same time (before the design of REQ-015 and REQ-017), and REQ-017 reads the minSdk from REQ-015.
3. Strictly, level 37 does not contradict REQ-017 (it names no level). So these are additions that prevent
   a conflict at planning time (AC-1 "current stable" could otherwise give a different number), not a
   reversal. Putting the decision into the requirement also keeps the Prime Directive: a developer reading
   REQ-017 does not need the analysis appendix to learn the level.
4. The alternative, leaving REQ-017's text alone and recording 37 only in the story, is what the analysis
   suggests ("set when REQ-017 is planned"). It would work, but the decision would then live only in an
   appendix of a feasibility analysis. If the orchestrator prefers it, drop changes 17-1 to 17-3 and keep
   17-4 and 17-5; nothing else here depends on them.

### Decision: the REQ-015 file name

**Recommend: leave the file name `REQ-MODERNIZATION-015-...-minsdk-23.md` as it is and change the title.**
The id (`REQ-MODERNIZATION-015`) is the reference: many artifacts cite it (a combined search for
REQ-015, REQ-017 and "minSdk 23" found 32 files under `sdlc`) and none cites the file name (a search
for `latest-stable-minsdk-23` found 0 matches). So a rename would break nothing today, but it
is also not needed, and file names and ids belong to the Artifact Librarian. The mismatch is cosmetic
and is explained by the amendment note (15-2). If the Librarian prefers to rename it, `...-minsdk-24.md`
is the obvious name and `_index.md` would follow. This record does not rename anything.

### REQ-MODERNIZATION-015

| # | Line(s) | Current text | Proposed text |
|---|---------|--------------|---------------|
| 15-1 | 16 and 29 | "Build toolchain and test tooling on latest stable, with minSdk raised to 23" | "Build toolchain and test tooling on latest stable, with minSdk raised to 24" |
| 15-2 | notes, after line 25 | (none) | Add: "Amended 2026-11-17 by CR-011: minSdk 24 (was 23), because the product owner chose to consume the Thunderbird mail libraries as Android variants, which need minSdk 24 (feasibility analysis Section 4, decision Record 4). The file name keeps 'minsdk-23' for history; the title and acceptance criteria govern." |
| 15-3 | 34 | "raise `minSdkVersion` from 21 to 23 in `app/build.gradle`" | "raise `minSdkVersion` from 21 to 24 in `app/build.gradle`" |
| 15-4 | 38-40 | "Android 5.0 and 5.1 devices (API 21 and 22) stop receiving updates." | "Android 5.0, 5.1 and 6.0 devices (API 21, 22 and 23) stop receiving updates." |
| 15-5 | after 66 | (none) | Add a bullet: "The Thunderbird mail libraries, consumed as Android variants from a fork (analysis Section 4, mode M2), require minSdk 24 and compileSdk 37 [C-221, C-222]. That sets the floor." Line 66 itself stays (Hilt, androidx.hilt and Robolectric need 23; 24 is above it). |
| 15-6 | 68-69 | "Four tests in `.../SmsReceiverTest.java` run at `Build.VERSION_CODES.LOLLIPOP` (SDK 21) and cannot run once minSdk is 23." | "Four tests in `.../SmsReceiverTest.java` run at `Build.VERSION_CODES.LOLLIPOP` (SDK 21), and one test in `.../service/WorkManagerSchedulerTest.kt` (line 378) runs at `@Config(sdk = [23])`; they cannot run once minSdk is 24." |
| 15-7 | 113-114 (AC-5) | "`minSdkVersion 23` ... `android:minSdkVersion="23"`" | "`minSdkVersion 24` ... `android:minSdkVersion="24"`" |
| 15-8 | 117-124 (AC-6) | "for a level below 23 ... at level 23 or above" | "for a level below 24 ... at level 24 or above". Add to the site list: "`scheduler/WorkManagerScheduler.kt` (the `SDK_INT < VERSION_CODES.N` branch of `scheduleContentTrigger`, line 224 at the time of writing)". |
| 15-9 | 125-128 (AC-7) | "The four SDK 21 tests in `SmsReceiverTest` are retargeted to a level of 23 or above, ... `grep -rn "LOLLIPOP\|sdk = \[\?2[0-2]" app/src/test`" | "The four SDK 21 tests in `SmsReceiverTest` and the SDK 23 test in `WorkManagerSchedulerTest` are retargeted to a level of 24 or above, ... `grep -rn "LOLLIPOP\|sdk = \[\?2[0-3]" app/src/test`" (the regex now reaches `sdk = [23]`) |
| 15-10 | 136-137 (AC-10) | "states that Android 5.0 and 5.1 are no longer supported" | "states that Android 5.0, 5.1 and 6.0 (API 21 to 23) are no longer supported". Whether this deliverable stays for a private app is the CR-003 follow-up, not decided here. |
| 15-11 | 165-166 (AC-17) | "at API 23 (the new minimum)" | "at API 24 (the new minimum)" |
| 15-12 | 177-180 (Rationale) | "... Robolectric 4.16 or later are the components that force the minSdk raise, ..." | "... Robolectric 4.16 or later require at least minSdk 23, and the Thunderbird Android variants require minSdk 24, which sets the value; ..." |
| 15-13 | 187-188 (Constraint) | "apart from the dropped API 21 and 22 support." | "apart from the dropped API 21, 22 and 23 support." |

Unchanged on purpose: line 48 (quotes the owner's earlier words), AC-9 (names no level), line 189 (JDK 25),
lines 196-198 (compileSdk and targetSdk stay with REQ-017), the version snapshot table (52-62).

### REQ-MODERNIZATION-016

| # | Line(s) | Current text | Proposed text |
|---|---------|--------------|---------------|
| 16-1 | 18-19 (notes) | "REQ-MODERNIZATION-015 (toolchain and minSdk 23)" | "REQ-MODERNIZATION-015 (toolchain and minSdk 24)" |
| 16-2 | 127 (Constraint) | "minSdk will be 23 once REQ-MODERNIZATION-015 lands." | "minSdk will be 24 once REQ-MODERNIZATION-015 lands." |

### REQ-MODERNIZATION-017

| # | Line(s) | Current text | Proposed text |
|---|---------|--------------|---------------|
| 17-1 | 18-20 (notes) | "The target API level is deliberately not named; AC-1 requires it to be determined and recorded at planning time." | "The level is 37 for both compileSdk and targetSdk (product owner decision 2026-11-17, Record 4, applied by CR-011). AC-1 requires the planning-time record to confirm that 37 is a stable level." |
| 17-2 | 31-36 (Description) | "... from 35 to the Android API level that is current stable on the date the implementing story is planned. ... This requirement does not name the target level; AC-1 makes determining it an explicit, evidenced step." | "... from 35 to API level 37 (decided 2026-11-17; the Thunderbird Android variants need compileSdk 37, and the product owner chose targetSdk 37 as well). AC-1 makes confirming that 37 is a stable level on the planning date an explicit, evidenced step." |
| 17-3 | 74-80 (AC-1, AC-2) | AC-1 as quoted above; AC-2: "both equal the level recorded under AC-1." | Add to AC-1: "The recorded level must be 37. If the source page does not show 37 as the current stable level (37 is a preview, or a higher level is stable), the story records that and asks the product owner; it neither substitutes a level nor proceeds on 37 silently." AC-2: "both equal 37, the level recorded under AC-1." |
| 17-4 | 86-90 (AC-3) | "... for apps targeting the new level (and for all apps running on it) ..." | "... for each platform level above the current targetSdk 35 up to the new level (that is, 36 and 37), as listed for apps targeting that level (and for all apps running on it) ..." |
| 17-5 | 21-22 (notes) and 173-174 (Constraint) | "after the toolchain and minSdk 23 requirement" and "minSdk is 23 once the toolchain requirement lands" | "after the toolchain and minSdk 24 requirement" and "minSdk is 24 once the toolchain requirement lands" |

### EPIC-MODERNIZATION-006

| # | Line(s) | Current text | Proposed text |
|---|---------|--------------|---------------|
| E-1 | 25 | "a minSdk raise to 23 accepted" | "a minSdk raise to 24 accepted" |
| E-2 | 28 | "the dropped Android 5.0/5.1 support" | "the dropped Android 5.0 to 6.0 support" |
| E-3 | 39 | "Build toolchain and test tooling on latest stable, minSdk 23" | "... minSdk 24" |
| E-4 | 44-46 | "Hilt 2.60, androidx.hilt 1.3+, and Robolectric 4.16+ require minSdk 23, so `minSdkVersion` is raised from 21 to 23 and Android 5.0/5.1 support ends." | "Hilt 2.60, androidx.hilt 1.3+, and Robolectric 4.16+ require minSdk 23, and the Thunderbird Android variants require minSdk 24, so `minSdkVersion` is raised from 21 to 24 and Android 5.0 to 6.0 support ends." |
| E-5 | 53-54 | "raise `compileSdk` and `targetSdk` from 35 to the current stable Android API level" | "raise `compileSdk` and `targetSdk` from 35 to 37 (the current stable Android API level, confirmed at planning time)" |
| E-6 | 82-84 (criterion 2) | "`minSdkVersion` is 23 ...; no `Build.VERSION.SDK_INT` guard for a level below 23 remains" | "`minSdkVersion` is 24 ...; no `Build.VERSION.SDK_INT` guard for a level below 24 remains" |
| E-7 | 85-88 (criterion 3) | "`compileSdk` and `targetSdk` equal the current stable API level (verified against the Android developer documentation at planning time)" | "`compileSdk` and `targetSdk` equal 37, and 37 is confirmed as the current stable API level against the Android developer documentation at planning time" |
| E-8 | 115 and 117 | "... with minSdk 23." and "Platform level (compileSdk/targetSdk) on current stable, ..." | "... with minSdk 24." and "Platform level (compileSdk/targetSdk 37), ..." |
| E-9 | 142-143 | "Dropping Android 5.0/5.1 is a user-facing support reduction" | "Dropping Android 5.0 to 6.0 (API 21 to 23) is a user-facing support reduction" |
| E-10 | 125-129 (Sequencing) | "... Requirement 5 starts with its feasibility analysis, which can run in parallel with requirements 1 to 4 and whose outcome gates all mail design work." | Add: "The feasibility analysis is accepted and the migration (Part B) is a go (Record 3). Requirements 1, 2 and 3 (toolchain and minSdk 24, runtime libraries, platform level 37) are complete before any Part B row that compiles Thunderbird code (analysis condition 1, decision Record 4)." |
| E-11 | 147 (Open Questions) | "Which Android API level is current stable at planning time is not yet verified." | "The level is decided: 37 (Record 4). Whether 37 is a stable level on the planning date is confirmed under REQ-MODERNIZATION-017 AC-1." |

### REQ-MODERNIZATION-019 (optional, no change required)

| # | Line | Proposed text |
|---|------|---------------|
| C-8 | 128 | Optional clarification: "... after the toolchain versions it needs are in place (REQ-MODERNIZATION-015, -016 and -017; minSdk 24, compileSdk 37; CR-011)." No other REQ-019 line changes: lines 35 and 70 are current-state or Part A history. |

### Frontmatter and notes

Add `CR-011` to `change_records` on REQ-015, -016, -017 and EPIC-006, and a dated body note in each
("Amended 2026-11-17 by CR-011: ..."), as CR-003 did. `status: approved` stays. `_index.md:8` takes the
new REQ-015 title (a mechanical regeneration by the Librarian).

### Related, kept out of this record

The earlier accepted analysis text that still says 23 (Q12.e condition 2) is read-only and is not edited;
the story that carries that condition uses API 24. The cleanup of the dead branches is done by the
REQ-015 story under its AC-6 to AC-8, with the new sites listed above.

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only — refined during story planning.

**Toolchain and platform ordering.** REQ-015 goes first: Gradle, AGP, Kotlin with Hilt, minSdk 24 and the dead-branch cleanup. REQ-017 (compileSdk and targetSdk 37) needs an AGP that supports 37 and a Robolectric that runs it, and REQ-015 sets both. The record says REQ-016 and REQ-017 may come in either order. I would qualify that. The analysis (Section 4) records that androidx.core and core-ktx 1.19.1 carry an AAR `minCompileSdk` of 37. If any "latest stable" upgrade under REQ-016 pulls such an AAR in transitively, REQ-016 cannot land before REQ-017. REQ-016 names only `core-role` directly (its line 65), so check the transitive graph when REQ-016 is planned. Until that check is done, use the order 015 → 017 → 016. RCR and High: agreed.

**Fork wiring (Record 4: "Decide at the proof story").** minSdk 24 and compileSdk 37 hold under any wiring, because they come from the variants' AAR metadata. The rows for Gradle 9.7.0, AGP 9.5.1 and "JDK 21 or newer, not tested on JDK 25" matter only if the fork is built inside the app's build (a submodule or included build). Publishing the fork's artifacts from a separate build would:
- remove the JDK 25 collision with EPIC-MODERNIZATION-006's Out of Scope;
- keep upstream's build logic out of the app's build (security review L-5).

The cost is a publish step plus the still-open question of who hosts the fork. REQ-015's text does not depend on the choice, but the product owner should know about it before the proof story.

**Condition 2 under minSdk 24.** Analysis condition 2 (line 1350) asks for a smoke test "on an API 23 device or emulator". With minSdk 24 the app cannot be installed on API 23. The accepted analysis is read-only, so the Part B epic or the proof story must restate condition 2 as API 24, in writing. The parenthetical matters more than the number: "(minimum API of upstream's third-party libraries not measured)". Plain JAR libraries brought in by the fork (CR-010 lists okio, moshi, httpclient5 among others) declare no minimum API. A call into a newer platform API would therefore fail only at run time. The API 24 backup-and-restore smoke test over TLS is the runtime check for that, so keep it mandatory.

**Cascade.** Text edits now: four requirement files plus the index row. Later, through the REQ-015 and REQ-017 stories: build files, 9 code sites, 5 tests and the documentation. The record correctly finds the two branches that become dead at 24 and are missed by AC-6/AC-7 as written (`WorkManagerScheduler.kt:224`, `WorkManagerSchedulerTest.kt:378`); keep its widened regex. The vendored module can stay at minSdk 21 until Part B deletes it (agreed).

**Proposed wording.** I agree with writing 37 into REQ-017, keeping AC-1 as a confirmation with stop-and-ask. Whether 37 is the stable level on the planning date was not checked here. AC-1 is the step that checks it, and locally installed platforms do not count as evidence. I also agree that AC-3 should name both 36 and 37.

**Cross-record.** CR-009's moved default factory loses its pre-Android 7.0 SNI path because of this record (see CR-009). CR-010 edits other lines of the same epic. Apply both together with CR-005's AC-B3 edit and the CR-003 sibling wording, in one requirements pass with one Requirements Reviewer check.

**Story shape.** No story of its own. The text pass comes first; the REQ-015 and REQ-017 stories are then written from the amended text.

**Product owner.** Confirm that no device the owner still uses runs Android 6.0 or older (the record's checklist item).

---

## Cascade Analysis

**Root Layer:** Requirements (REQ-MODERNIZATION-015 is the first holder of the value; REQ-016, REQ-017 and the epic repeat it)
**Root Defect:** The approved floor (23) is below what the chosen consumption route needs (24), and the
platform-level requirement names no level, so compileSdk 37 and targetSdk 37 exist only in an analysis appendix.
**Cascade Direction:** Mid-stream down. Nothing is built from the old values, so the cascade is text-only today.

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Requirements (REQ-015) | Requirements (REQ-016, REQ-017, EPIC-006) | Yes, wording | 31 changes in 4 files (Impact matrix row 2) |
| 2 | Requirements | Design | No | No design exists for REQ-015/016/017; contracts state no platform level |
| 3 | Requirements | Stories | No | No story for REQ-015/016/017/019 |
| 4 | Requirements | Build configuration | No (not yet) | `app/build.gradle:21,24,25` still 35/21/35; nothing changed for the new values |
| 5 | Requirements | Code and tests | No (not yet) | 7 dead-branch code sites, 2 manifest attributes, 5 tests exist (listed above); none edited |

### Fix Order

1. [ ] Requirements: amend REQ-015 (title and the other changes 15-1 to 15-13) — ROOT FIX
2. [ ] Requirements: amend REQ-016, REQ-017 and the epic to match — CASCADE FIX
3. [x] Requirements Reviewer pass on REQ-015 AC-6/AC-7 and REQ-017 AC-1/AC-3, because they change what counts as a pass
4. [ ] Design: design REQ-015 and REQ-017 against the amended text — CASCADE
5. [ ] Stories: author the REQ-015 and REQ-017 stories from the amended text — CASCADE
6. [ ] Verification: `grep -nE "minSdk 23|minSdk raised to 23|to 23|API 23|below 23|23 or above" ` over the four files shows only intended history (REQ-015 line 66 and the amendment note)

### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| None. No story exists for REQ-015, -016, -017 or -019; sprint-019 (U-062) is closed | n/a | n/a | n/a |

---

## Plan

- **Execution Window:** Inline: apply before the design of REQ-015 and REQ-017 starts.
- **Assigned To:** Requirements Analyst (text), Requirements Reviewer (check)
- **Estimated Effort:** 1 to 2 hours (31 changes in 4 files, plus review)
- **Sprint:** None

### Timing

Apply the proposed text **before the design of REQ-015 and REQ-017 starts** (neither is designed yet), and
before any story is authored from them. Reason: a design written against "minSdk 23" would have to be
reworked, and AC-6 and AC-7 are the greps the stories will run. This is the earliest useful point and
there is no reason to apply it later. Apply it in the same pass as CR-010 (same epic file) and,
preferably, together with the CR-003 sibling wording follow-up on REQ-015 AC-10, the epic's publishing
lines and REQ-017 lines 171-172, so those lines are edited once. Not applied by this record.

### Fix Checklist

- [x] Requirements: REQ-015 15-1 to 15-13, REQ-016 16-1 and 16-2, REQ-017 17-1 to 17-5, epic E-1 to E-11; add `CR-011` to `change_records` and the dated notes (done 2026-11-21; also C-8 on REQ-019)
- [x] Requirements Reviewer: check REQ-015 AC-6 and AC-7, REQ-017 AC-1 and AC-3 (FAIL on iteration 1, PASS on iteration 2, 2026-11-21)
- [x] Librarian: regenerate the `_index.md` row (row changed by hand, title only, 2026-11-21); decide whether to rename the REQ-015 file (decided by the orchestrator: leave as it is)
- [x] Product owner: confirm the owner's own devices all run Android 7.0 or later (settled by decision Record 5, row 8: "all my devices are 11 or newer")
- [x] Verification: 0 remaining unintended "minSdk 23" / "below 23" statements in the four files (clean after RAR-003; see Verification Evidence items 6 to 9)

### Next steps (High severity)

Run `/amp:create-stories --from-cr CR-011` when ready. This record is a **precondition of design work** for
REQ-015 and REQ-017 (the text amendment comes first, and no story of its own is needed for it). The work
it makes possible is **stories to schedule**: the REQ-015 story (floor, toolchain, dead-branch cleanup,
tests) and the REQ-017 story (levels, behaviour-change review, device run) are created from the amended text.

### Not verified

Neither this record nor the analysis checked the following.

- **Android API 37 platform in this environment.** Checked now: the Android SDK folder named in
  `local.properties` holds a platform folder `android-37.0` whose `source.properties` reads
  `AndroidVersion.ApiLevel=37.0`, an empty `CodeName` and `PreviewSdkInt=0`, next to `android-34`, `android-35`,
  `android-36` and `android-36.1`. That shows a non-preview 37 platform is installed here. It does **not**
  show that 37 is the current stable level on developer.android.com, and REQ-017 AC-1 rejects local
  installation as evidence of stable status. Whether the CI runner has the platform is also unchecked
  (`.github/workflows/ci.yml:30` installs only `build-tools;36.0.0`; CI has never run on the current
  toolchain, per the epic).
- **Behaviour changes of API 36 and 37 for this app.** The analysis did not assess them (Record 4 says
  so). REQ-017 AC-3 and AC-4 are where they are assessed. Nothing in this record says they are harmless.
- **Whether upstream's build runs on JDK 25** (analysis experiment (c) "not run"). Gradle 9.7.0 is what the
  upstream wrapper names; this repository will be on the latest stable (9.9.1 as of the snapshot).
- **Whether AGP 9.0.1 or 9.5.1 supports compileSdk 37**, and whether Robolectric 4.17 can run API 37 tests
  (REQ-017 AC-13 asks for this evidence at planning time).
- **Whether the libraries in REQ-016's table need a compileSdk above 35**, and whether any needs a higher
  minSdk than 24. REQ-016 states no compileSdk. Not read.
- **Whether Robolectric refuses `@Config(sdk = [23])` once minSdk is 24.** REQ-015 line 69 assumes that
  tests below the floor "cannot run"; that is the approved reading and is extended here by analogy. In
  either case the SDK 23 test covers a branch that is dead at minSdk 24.
- **The owner's devices.** The repository does not record them. The owner has accepted minSdk 24; the only
  residual risk is a device on Android 6.0 or lower that the owner still uses.
- **Upstream facts** (floors, versions) come from the analysis and its citation ids, which were not
  re-opened for this record.

---

## Execution

### Changes Made

Applied 2026-11-21 by the Requirements Analyst, in one requirements pass with CR-010 (product owner
instruction of 2026-11-21, Record 11 item 3: "3 start there"). Locations were found by the quoted text,
not by the line numbers of 2026-11-17. Every quoted "current text" was found exactly once where the
record expected one place. `status: approved`, `id`, `artifact_type` and `updated_at` are unchanged on
all files; no file was renamed.

| File | Section or criterion | Edit id | What changed | Layer |
|------|----------------------|---------|--------------|-------|
| `REQ-MODERNIZATION-015-...-minsdk-23.md` | frontmatter | (frontmatter) | `CR-011` appended to `change_records` | Requirements |
| REQ-015 | frontmatter `title` and H1 | 15-1 | "raised to 23" became "raised to 24" | Requirements |
| REQ-015 | frontmatter `notes` | 15-2 | Amendment note added (dated 2026-11-21; says the record was approved 2026-11-17; file name keeps `minsdk-23` for history) | Requirements |
| REQ-015 | Description | 15-3 | "from 21 to 23" became "from 21 to 24" | Requirements |
| REQ-015 | Description | 15-4 | "Android 5.0 and 5.1 devices (API 21 and 22)" became "Android 5.0, 5.1 and 6.0 devices (API 21, 22 and 23)" | Requirements |
| REQ-015 | Context, known couplings | 15-5 | New bullet: the Thunderbird variants (mode M2) need minSdk 24 and compileSdk 37; the Hilt bullet is unchanged | Requirements |
| REQ-015 | Context, known couplings | 15-6 | SmsReceiverTest sentence extended with the `WorkManagerSchedulerTest.kt` test at `@Config(sdk = [23])`; "once minSdk is 24" | Requirements |
| REQ-015 | AC-5 | 15-7 | `minSdkVersion 24` and `android:minSdkVersion="24"` | Requirements |
| REQ-015 | AC-6 | 15-8 | "below 24" as proposed; `scheduler/WorkManagerScheduler.kt` (`SDK_INT < VERSION_CODES.N` branch of `scheduleContentTrigger`, line 224) added to the site list; "level 24 or above" as proposed. Later corrected by RAR-002 to "24 or lower" / "25 or above" (see Departures) | Requirements |
| REQ-015 | AC-7 | 15-9 | SDK 23 test in `WorkManagerSchedulerTest` added; "level of 24 or above"; regex `sdk = \[\?2[0-3]` | Requirements |
| REQ-015 | AC-10 | 15-10 | "Android 5.0, 5.1 and 6.0 (API 21 to 23)". Nothing else in AC-10 touched (the release-notes deliverable is the CR-003 sibling follow-up) | Requirements |
| REQ-015 | AC-17 | 15-11 | "API 24 (the new minimum)" | Requirements |
| REQ-015 | Rationale | 15-12 | "require at least minSdk 23, and the Thunderbird Android variants require minSdk 24, which sets the value;" | Requirements |
| REQ-015 | Constraints | 15-13 | "dropped API 21, 22 and 23 support" | Requirements |
| REQ-015 | Notes (body) | note | Dated "Amended 2026-11-21 by CR-011" bullet added | Requirements |
| `REQ-MODERNIZATION-016-runtime-libraries-latest-stable.md` | frontmatter | (frontmatter) | `CR-011` appended to `change_records` | Requirements |
| REQ-016 | frontmatter `notes` | 16-1 | "toolchain and minSdk 24" | Requirements |
| REQ-016 | Constraints | 16-2 | "minSdk will be 24 once REQ-MODERNIZATION-015 lands." | Requirements |
| REQ-016 | Notes (body) | note | Dated "Amended 2026-11-21 by CR-011" bullet added | Requirements |
| `REQ-MODERNIZATION-017-platform-level-current-stable-api.md` | frontmatter | (frontmatter) | `CR-011` appended to `change_records` | Requirements |
| REQ-017 | frontmatter `notes` | 17-1 | Level 37 stated for compileSdk and targetSdk, with its source; AC-1 confirms 37 is a stable level | Requirements |
| REQ-017 | Description | 17-2 | "from 35 to API level 37 (decided 2026-11-17; ...)"; last sentence now "AC-1 makes confirming that 37 is a stable level on the planning date an explicit, evidenced step" | Requirements |
| REQ-017 | AC-1, AC-2 | 17-3 | AC-1: recorded level must be 37, stop-and-ask rule added; AC-2: "both equal 37, the level recorded under AC-1" | Requirements |
| REQ-017 | AC-3 | 17-4 | Table covers each level above targetSdk 35 up to the new level (36 and 37) | Requirements |
| REQ-017 | frontmatter `notes` and Constraints | 17-5 | "minSdk 24 requirement" and "minSdk is 24 once the toolchain requirement lands" | Requirements |
| REQ-017 | Notes (body) | note | Dated "Amended 2026-11-21 by CR-011" paragraph added | Requirements |
| `EPIC-MODERNIZATION-006-...md` | frontmatter | (frontmatter) | `CR-011` and `CR-010` appended to `change_records` | Requirements |
| Epic | Description | E-1, E-2 | "minSdk raise to 24"; "dropped Android 5.0 to 6.0 support" | Requirements |
| Epic | In Scope, toolchain bullet | E-3, E-4 | "minSdk 24" in the bullet title; Thunderbird variants require 24, so 21 to 24 and "Android 5.0 to 6.0 support ends" | Requirements |
| Epic | In Scope, platform level | E-5 | "from 35 to 37 (the current stable Android API level, confirmed at planning time)" | Requirements |
| Epic | Success criteria 2 and 3 | E-6, E-7 | minSdk 24 and "below 24"; compileSdk and targetSdk equal 37, with 37 confirmed at planning time | Requirements |
| Epic | Related Requirements 1 and 3 | E-8 | "with minSdk 24"; "(compileSdk/targetSdk 37)" | Requirements |
| Epic | Sequencing | E-10 | Sentences added: analysis accepted, migration is a go (Record 3); requirements 1, 2, 3 complete before any Part B row that compiles Thunderbird code | Requirements |
| Epic | Constraints and Risks | E-9 | "Dropping Android 5.0 to 6.0 (API 21 to 23)" | Requirements |
| Epic | Open Questions | E-11 | "The level is decided: 37 (Record 4). ... confirmed under REQ-MODERNIZATION-017 AC-1." | Requirements |
| Epic | new "Amendments" subsection | note | Dated "Amended 2026-11-21 by CR-011" bullet (the CR-010 bullet is in the same subsection) | Requirements |
| `REQ-MODERNIZATION-019-...md` | frontmatter | (frontmatter) | `CR-011` appended to `change_records` (after `CR-010`) | Requirements |
| REQ-019 | Dependencies and Sequencing | C-8 | "(REQ-MODERNIZATION-015, -016 and -017; minSdk 24, compileSdk 37; CR-011)" added. Applied, as the record offers it | Requirements |
| REQ-019 | Traceability (end) | note | Dated "Amended 2026-11-21 by CR-011" paragraph added | Requirements |
| `sdlc/artifacts/requirements/modernization/_index.md` | REQ-015 row | index | Title changed to "...with minSdk raised to 24" (mechanical consequence of 15-1); nothing else | Requirements |

### Departures from the proposed text

No edit departs from the record in substance. Wording and placement choices, each stated here:

- **Dates.** Every note says "Amended 2026-11-21 by CR-011 (approved 2026-11-17)", not the proposed
  "2026-11-17": the amendment is made today (instruction of the orchestrator for this pass).
- **15-2 in two places.** The proposed note was applied in the frontmatter `notes` field, where the
  record puts it ("notes, after line 25"). A second dated note was added to the body Notes section,
  as the CR-003 precedent does, and it lists the criteria changed. The record asked for one note.
- **Reading of "..." in the proposed texts.** Where a record row shows an ellipsis (15-7 to 15-9,
  17-4, E-6, E-7), only the quoted fragments were changed and the rest of the sentence kept.
  17-4: the new wording reads "lists for each platform level above the current targetSdk 35 up to the
  new level (that is, 36 and 37), as listed for apps targeting that level (and for all apps running
  on it)".
- **15-12.** The proposed text ended in "; ..."; the sentence continues "the later runtime-library
  and platform-level requirements depend on the newer plugins" after a semicolon.
- **15-6.** The path of the second test is written out in full
  (`app/src/test/java/com/zegoggles/smssync/service/WorkManagerSchedulerTest.kt`), as the first is.
- **15-8 and E-6, AC-6 pass rule (reviewer correction RAR-002; added after review).** The record's text,
  "a level below 24 ... at level 24 or above", would accept the very site it adds
  (`SDK_INT < VERSION_CODES.N` is a comparison against level 24, always decided at minSdk 24). To carry
  out the record's stated intent (dead branches go), AC-6 now reads "a level of 24 or lower", its grep
  adds `RequiresApi`, and its pass rule is "every remaining comparison or annotation names a level of 25
  or above; `import` lines are not counted". Epic criterion 2 reads "guard against a level of 24 or
  lower". This is not traceable to a product-owner answer; it is the reviewer's ruling, accepted by the
  orchestrator.
- No product-owner answer of Record 5 changes any CR-011 edit. Row 8 ("all my devices are 11 or
  newer") only settles the checklist line on the owner's devices. The REQ-017 decision (17-1 to
  17-3 applied) and the REQ-015 file name (left as it is) follow the orchestrator's instruction
  and the record's own recommendations.

### Not applied, left for a decision

- **REQ-015 heading "### minSdk 23 and its consequences"** was not one of the 31 edits and was first
  left unchanged. It is now changed to "minSdk 24" (reviewer correction RAR-003).
- **REQ-017 title and `_index.md` row 10** ("...to the current stable Android API level") and
  REQ-017's frontmatter note "Neither REQ-MODERNIZATION-015 nor -016 exists on disk yet" are not in
  the record; unchanged.
- The CR-003 sibling wording follow-up (store-publishing wording in REQ-015 AC-10, the epic's
  publishing lines, REQ-017 lines 171-172): no change record exists; nothing edited beyond 15-10
  and E-9 as written.

### Verification Evidence

Run 2026-11-21 on the working tree at HEAD `4a888b16`. No Gradle was run.

1. Search of the four requirement files and the epic for the old values
   (`minSdk 23|minSdkVersion 23|below 23|to 23|API 21 and 22|5\.0 and 5\.1|5\.0/5\.1|current stable Android API level|minSdk raised to 23|minsdk-23|API 23|23 or above|\[23\]|2\[0-2\]`).
   Remaining hits, each judged:
   - REQ-015 `notes` (amendment note) and body Notes: name `minsdk-23` and the old value as history. Kept on purpose.
   - REQ-015 line 71 "Hilt 2.60 ... require minSdk 23" and the Rationale "require at least minSdk 23": true facts about those libraries. Kept (the record says so).
   - REQ-015 line 78 `@Config(sdk = [23])`: names the test that 15-6 adds. Kept.
   - REQ-015 line 149 (AC-10) "6.0 (API 21 to 23)": the new text.
   - REQ-015 line 120 `### minSdk 23 and its consequences`: **unintended, not in the 31 edits.** Left; reported above.
   - Epic line 47 "require minSdk 23, and the Thunderbird Android variants require minSdk 24": true fact kept by E-4. Epic line 164 "(API 21 to 23)": the new text (E-9).
   - REQ-017 title (line 15 and H1): "current stable Android API level": not in the record. Left.
   - REQ-019 line 72 (Part A question 4, "REQ-MODERNIZATION-015 (minSdk 23)"): the record names it "No change" (Part A history).
   - `_index.md` row 10 (REQ-017 title): same as the REQ-017 title.
   - REQ-016: no hit. REQ-015 AC-1 to AC-18 contain no other old value.
2. Criterion-5 grep as now written in the epic
   (`grep -rn "com\.fsck\.k9\|com\.android\.email\|net\.thunderbird" app/src/main/java/com/zegoggles/smssync/service/`):
   no output, exit status 1 (no match). True on today's tree.
3. AC-7 grep with the widened regex (`grep -rn "LOLLIPOP\|sdk = \[\?2[0-3]" app/src/test`): 6 lines: `SmsReceiverTest.java` 24 (a comment), 27, 32, 37, 42 (the four SDK 21 tests) and `WorkManagerSchedulerTest.kt:378` (`@Config(sdk = [23])`). The old regex (`2[0-2]`) found 5; the new one adds line 378. This is expected today: they are what the future story retargets.
4. AC-6 grep (`grep -rn "SDK_INT\|VERSION_CODES\|TargetApi" app/src/main`) on today's tree: the sites below 24 are the five listed in AC-6 (`SmsReceiver.java` 31 and 50, `SmsDefaultRoleHelper.kt` 74, `MainActivity.java` 425, `Dialogs.java` 172, `CalendarAccessorPost40.java` 23) and `WorkManagerScheduler.kt:224`, which the amended AC-6 now names. Expected today.
5. `git diff --stat -- sdlc/artifacts/requirements`: the epic, REQ-012 (one note), REQ-015, REQ-016, REQ-017, REQ-019 and `_index.md`, nothing else. Other files modified on the same day are other work, not this pass: CR-013, CR-009 and CR-014 belong to change record CR-014 (committed as `042054ba`); the decision appendix of the analysis and the audit log are the orchestrator's.

Re-run after the reviewer corrections (2026-11-21):

6. Old values (same pattern, plus `a stable level|below 24`): remaining hits are only the intended ones listed in item 1 (amendment notes, true facts about Hilt/androidx.hilt/Robolectric needing 23, the "API 21 to 23" wording, the REQ-017 title and index row, REQ-019 line 72). The REQ-015 heading no longer matches. No `below 24` and no `a stable level` remain.
7. Absence statements about the four libraries: none unqualified (hits are criterion 4's Play Billing, criterion 5's `:k9mail-vendored` module, and the amendment notes). No wording about a forced version remains in the epic or REQ-019.
8. Criterion-5 grep (`com\.fsck\.k9\|com\.android\.email\|net\.thunderbird` over `service/`): no output, exit status 1.
9. AC-6 search on `app/src/main` (`SDK_INT|VERSION_CODES|TargetApi|RequiresApi`, import lines excluded): 19 lines. Sites at level 24 or lower, exactly the ones AC-6 lists: `Dialogs.java:172` (ICE_CREAM_SANDWICH, 14), `CalendarAccessorPost40.java:23` (ICE_CREAM_SANDWICH, 14), `SmsReceiver.java:31` and `:50` (KITKAT, 19), `MainActivity.java:425` (KITKAT, 19), `SmsDefaultRoleHelper.kt:74` (KITKAT, 19), `WorkManagerScheduler.kt:224` (N, 24). Sites at 25 or above (stay): Q (29) at `MainActivity.java:242`, `SmsDefaultRoleHelper.kt:77,111,135`, `SmsReceiver.java:47`, `BackupWorker.kt:533`, `RestoreWorker.kt:707`; TIRAMISU (33) at `MainActivity.java:517`; O (26) at `App.java:246,350`; literal 26 at `ThemeActivity.java:20,26`. 7 + 12 = 19.

### Reviewer corrections (2026-11-21)

Requirements Reviewer verdict on the first pass: FAIL (iteration 1), two blocking findings; every
numbered edit otherwise confirmed as applied faithfully. Corrections applied the same day:

| RAR | Applies to this record | What changed |
|-----|------------------------|--------------|
| RAR-002 (blocking) | Yes | REQ-015 AC-6: "a level of 24 or lower"; grep adds `RequiresApi`; pass rule "every remaining comparison or annotation names a level of 25 or above ... `import` lines are not counted". Epic criterion 2: "guard against a level of 24 or lower". Site list confirmed before writing (Verification Evidence item 9) |
| RAR-003 | Yes | REQ-015 heading changed to "### minSdk 24 and its consequences" |
| RAR-004 | Yes | "a stable level" became "the current stable level" in REQ-017 (frontmatter note, Description) and the epic's Open Questions line |
| RAR-008 | Yes | REQ-015 body note: "The owner's earlier words in Context ("raising the minsdk version")" (the words are in Context) |
| RAR-010 | Yes | Verification Evidence item 5 limited to `sdlc/artifacts/requirements`; other files named as other work of the same day |
| RAR-001, RAR-005, RAR-006 | No (CR-010) | Recorded in CR-010's Execution |

### Reviewer notes left as they are

By the orchestrator's ruling, not applied; one line each:

- RAR-007: the more robust AC-7 regex is for the REQ-015 story; AC-7 stays as CR-011 gives it.
- RAR-009: REQ-019's stricter sequencing is aligned when CR-005 and CR-006 are applied.
- RAR-011: CR-005 and CR-006 are deliberately not in this pass; their own Timing sections apply them with the Part B design.
- RAR-012, RAR-013: left for the REQ-015 and REQ-016 designs.
- RAR-014, RAR-015: left.
- The private-app wording follow-up (REQ-015 AC-10 and the other publishing lines) gets no change record now: none of those lines blocks a story (AC-10 is met by a `CHANGES` entry in the repository; the others are informational). It is raised only if a story is blocked.

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| The commit with the subject "Requirements pass: CR-010 and CR-011 applied (minSdk 24, levels 37, retained libraries)" (find it with `git log --grep`) | 2026-11-21 | sdlc/modernization-plan | The requirement files amended by this record, `_index.md`, and this record | Orchestrator (text by the Requirements Analyst, checked by the Requirements Reviewer) |

---

## Closure

- **Root Cause Category:** (pending)
- **Prevention Recommendation:** When a requirement fixes a platform floor before the largest dependency it must host has been assessed, state the floor as "the highest minimum of the planned dependencies" with the evidence step, so that a later route decision does not require a requirement change. Name the platform level in the platform-level requirement as soon as the product owner decides it.
- **Closed By:** (pending)
- **Date Closed:** (pending)
