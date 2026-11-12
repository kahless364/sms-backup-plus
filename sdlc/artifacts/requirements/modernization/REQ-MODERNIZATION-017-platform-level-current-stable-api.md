---
status: approved
type: functional
artifact_type: requirement
related_requirements:
  - REQ-MODERNIZATION-001
  - REQ-MODERNIZATION-003
related_stories: []
related_design_docs: []
traces_to:
  - REQ-MODERNIZATION-001
change_records: []
id: REQ-MODERNIZATION-017
title: Raise compileSdk and targetSdk to the current stable Android API level, with on-device validation
domain: modernization
epic: EPIC-MODERNIZATION-006
priority: high
notes: |
  Platform-level requirement 3 of 5 in EPIC-MODERNIZATION-006. The target API level is
  deliberately not named; AC-1 requires it to be determined and recorded at planning time.
  Sequencing (epic Notes): after the donation-flow removal requirement, and after the
  toolchain and minSdk 23 requirement (referenced in the brief as REQ-MODERNIZATION-015).
  Neither REQ-MODERNIZATION-015 nor -016 exists on disk yet; those references are unverified.
updated_at: '2026-11-12T16:48:41.721Z'
---

# REQ-MODERNIZATION-017: Raise compileSdk and targetSdk to the current stable Android API level, with on-device validation

## Description

Raise `compileSdkVersion` and `targetSdkVersion` in `app/build.gradle` from 35 to the Android
API level that is current stable on the date the implementing story is planned. Adopt that
level only together with (a) a recorded disposition of every platform behavior change that
applies to apps targeting it and (b) validation of the app's core flows on an emulator or
device running that level. This requirement does not name the target level; AC-1 makes
determining it an explicit, evidenced step.

## Context

`app/build.gradle` declares `compileSdkVersion 35` (line 21) and `targetSdkVersion 35`
(line 25). REQ-MODERNIZATION-001 raised the app from 29 to 35. That raise passed unit tests
but its platform-behavior defects were found on a device or in review, not by the JVM suite:

- U-003 (`sdlc/artifacts/build/sprints/sprint-001/U-003-sdk35-conformance-jcenter-removal.md`)
  co-landed `FLAG_IMMUTABLE` on the `PendingIntent` sites because, at targetSdk 35 on API 31+,
  omitting it throws `IllegalArgumentException`. Whether this was first observed as a launch
  crash on a device is not recorded in the files read (not verified).
- BUG-014 (sprint-010): targetSdk 35 forced edge-to-edge, so content drew under the
  status and navigation bars.
- BUG-009 (sprint-008): restore bailed on Android Q+ because the legacy
  `getDefaultSmsPackage()` returned null (default-SMS role flow).
- BUG-016 (sprint-016): WorkManager's `SystemForegroundService` had no
  `android:foregroundServiceType`, crash-looping backup and restore on targetSdk 34+. The
  sprint-016 retro states the check "is unreachable from unit tests" and is "only observable at
  a real Android FGS start".

The lesson is that unit tests and Robolectric cannot prove a platform-level raise. This
requirement therefore makes on-device validation and a documented review of published
behavior changes mandatory acceptance gates, not follow-ups.

Related facts: `app/src/test/resources/robolectric.properties` pins `sdk=29`, and its comment
claims this matches `compileSdkVersion` (stale); Robolectric is 4.15.1 in `app/build.gradle`
(may change under the toolchain requirement). The `:app:lintDebug` step already failed
before the JDK 25 move (sprint-017 retro) and `app/build.gradle` uses `lint-baseline.xml`.
The public `com.zegoggles.smssync.BACKUP` broadcast contract (CNTR-MODERNIZATION-005) is
frozen and must keep working.

## Requirements

### Determine the target level

#### Acceptance Criteria

- [ ] AC-1: A planning-time record in the implementing story names the current stable API
  level (number and Android version name), the source URL on developer.android.com, and the
  date consulted. Evidence that an SDK platform is installed locally is not accepted as
  evidence of stable status. A level labeled preview, beta, or developer preview on the
  source page does not qualify.
- [ ] AC-2: `compileSdkVersion` and `targetSdkVersion` in `app/build.gradle` both equal the
  level recorded under AC-1. Verified by `grep -n "compileSdkVersion\|targetSdkVersion" app/build.gradle`.

### Behavior-change review

#### Acceptance Criteria

- [ ] AC-3: The story contains a table of every behavior change that the platform's
  published documentation lists for apps targeting the new level (and for all apps running on
  it), citing the documentation URL. Each row is dispositioned as exactly one of: not
  applicable (with reason), applicable and fixed (with file or commit), or applicable and
  deferred (with a follow-up artifact id). No row is left undispositioned.
- [ ] AC-4: At minimum the review explicitly covers, with a disposition each: foreground-service
  type rules (WorkManager `SystemForegroundService` override, BUG-016); edge-to-edge and window
  insets on all activities and preference screens (BUG-014); predictive back; runtime
  notification permission; exact-alarm and background-start restrictions affecting auto-backup
  scheduling; broadcast receiver export rules for `BackupBroadcastReceiver`; SMS and
  default-SMS role behavior.
- [ ] AC-5: Every change dispositioned "applicable and fixed" lands in the same PR as the
  level raise or earlier; no commit exists where targetSdk is raised while a known crash
  condition from the AC-3 table remains unfixed (the co-landing rule from U-003). Verified by
  commit history.

### On-device validation at the new level

Run on an emulator (or device) whose system image is at the AC-1 level, using the debug APK
built from the story branch. Record steps, logcat excerpts, and results as text evidence
only; capture no screenshots and no account names. Third-party integration, if enabled for
testing, is restored to its original state afterwards (sprint-016 and sprint-017 retros).

#### Acceptance Criteria

- [ ] AC-6: Launch: the app starts to `MainActivity` and each preference screen opens with no
  crash and no `FATAL EXCEPTION` for `com.zegoggles.smssync` in logcat.
- [ ] AC-7: Edge-to-edge and back: on `MainActivity` and every preference screen, no
  toolbar title or interactive control is occluded by the status or navigation bars, and the
  system back gesture/button returns or exits correctly. Recorded per screen.
- [ ] AC-8: Permissions: first-run permission prompts (SMS, contacts, call log, notifications
  as applicable to the level) appear, and both grant and deny paths complete without crash.
- [ ] AC-9: Backup: a manual backup starts the WorkManager foreground worker, shows its
  notification, and reaches the IMAP `LOGIN` stage without `IllegalArgumentException` or
  foreground-service-type errors. A completed append is required where a working test account
  is available; otherwise the story records "not reached" with the reason.
- [ ] AC-10: Restore: restore performs the default-SMS role hand-over and hand-back and
  returns the device's original default SMS app. Where AC-9 could not reach the server,
  record "not reached" with the reason; do not report it as passed.
- [ ] AC-11: Auto-backup scheduling: enabling a schedule enqueues the periodic work, and
  after a forced run (`adb shell cmd jobscheduler run` or equivalent) the worker executes.
  Recorded with the command and logcat evidence.
- [ ] AC-12: Public broadcast: `adb shell am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync`
  is received by `BackupBroadcastReceiver` and enqueues a backup when third-party integration
  is enabled, and is ignored when disabled. Both branches are recorded (the "ignored" branch
  was unverified in sprint-017).

### Build, lint, and tests

#### Acceptance Criteria

- [ ] AC-13: Robolectric supports the new level: either the declared Robolectric version lists
  it as supported, with `app/src/test/resources/robolectric.properties` (and its stale
  comment) updated accordingly, or the story records the highest supported level and the
  reason tests are pinned below targetSdk. No `@Config(sdk=...)` is left at a level that the
  chosen Robolectric cannot run.
- [ ] AC-14: Lint decision: the story records one of (a) lint run at the new compileSdk with
  `lint-baseline.xml` refreshed, each newly baselined issue category listed, and no new
  baselined entry in category `NewApi`, `ExportedReceiver`, or `ForegroundServiceType`; or (b)
  lint explicitly left out of scope, with the reason, and the pre-existing failure reproduced
  unchanged at the new level. A silent change of baseline content is not acceptable.
- [ ] AC-15: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestCoverageVerification`
  passes on JDK 25 with the unit-test count not below the sprint-017 baseline (697 run,
  2 skipped, adjusted only for tests deleted by other epic requirements and listed in
  their stories), no per-package JaCoCo floor lowered, and `gradle/verification-metadata.xml`
  updated for any new artifact with dependency verification still enforced.
- [ ] AC-16: The manifest produced for the release build is diffed against the pre-raise
  manifest, and every difference is explained in the story (sprint-017 established a
  byte-identical-manifest baseline for toolchain moves).

## Rationale

Raising the platform level is the only epic requirement whose defects are invisible to the
JVM test suite. The history above shows four on-device-only or review-found defects from the
35 move alone. Requiring a written behavior-change disposition and a device run at the new
level converts that history into a gate. Naming the level at planning time, from an
authoritative source, prevents adopting a level on the strength of locally installed SDK
platforms.

## Constraints

- Build on JDK 25; dependency verification stays enforced; no JaCoCo floor lowered.
- `BackupBroadcastReceiver` remains exported and its action unchanged (CNTR-MODERNIZATION-005).
- Behavior preserved: backup and restore results are identical for the user before and
  after, apart from platform-mandated UI changes recorded under AC-3.
- Informational only, not acceptance criteria: Google Play target-level policy deadlines are
  noted in the story for the product owner; publishing is not decided here (epic Out of Scope).
- minSdk is 23 once the toolchain requirement lands; if it has not, the story records the
  minSdk in effect and validates on one emulator at the minSdk as well.

## Verification Method

Static: AC-2, AC-13, AC-16 by grep and diff. Documentation: AC-1, AC-3, AC-4, AC-14 by
story review. Build: AC-15 by the three Gradle gates. Device: AC-6 to AC-12 by emulator run
with text evidence; any step not reached is recorded as such, never as passed.

## Notes

Traceability: `sdlc/artifacts/requirements/modernization/EPIC-MODERNIZATION-006-dependency-currency-latest-stable.md`
(Platform level, Success Criterion 3); `sdlc/artifacts/build/sprints/sprint-016/retro.md`
(BUG-016); `sdlc/artifacts/build/sprints/sprint-017/retro.md` (lint, receiver coverage,
unverified broadcast branch). Supersedes the literal "35" in REQ-MODERNIZATION-001 AC-1 and
AC-29 only for future state; REQ-MODERNIZATION-001 should carry a change record.
