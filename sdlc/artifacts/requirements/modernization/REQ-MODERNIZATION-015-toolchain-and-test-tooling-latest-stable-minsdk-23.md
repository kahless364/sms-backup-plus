---
status: approved
type: nfr
artifact_type: requirement
related_requirements:
  - REQ-MODERNIZATION-001
  - REQ-MODERNIZATION-006
  - REQ-MODERNIZATION-008
  - REQ-MODERNIZATION-003
related_stories: []
related_design_docs: []
traces_to:
  - EPIC-MODERNIZATION-006
change_records: []
id: REQ-MODERNIZATION-015
title: Build toolchain and test tooling on latest stable, with minSdk raised to 23
domain: modernization
epic: EPIC-MODERNIZATION-006
priority: high
notes: |
  Supersedes the literal minSdk 21 statements in REQ-MODERNIZATION-001 (AC-2) and
  REQ-MODERNIZATION-006 (AC-3, and the minSdk 21 target in its Context, which the epic
  also lists as AC-5). Those artifacts need change records (AC-9 below); the orchestrator
  must create them, because this draft cannot edit files.
  Version figures are a 2026-11-12 snapshot, not targets.
updated_at: '2026-11-12T16:48:33.401Z'
---

# REQ-MODERNIZATION-015: Build toolchain and test tooling on latest stable, with minSdk raised to 23

## Description

Move the build toolchain and test tooling to the latest stable release of each component,
and raise `minSdkVersion` from 21 to 23 in `app/build.gradle` (line 24 at the time of
writing). In scope: Gradle, Android Gradle Plugin (AGP), Kotlin Gradle plugin and stdlib,
KSP, Hilt, androidx.hilt (`hilt-work`, `hilt-compiler`), Robolectric, Mockito, GreenMail,
JaCoCo, auto-service, the CI action versions, and the SDK Build Tools version that the
chosen AGP requires. User-visible backup and restore behavior is unchanged. The one
user-visible effect is that Android 5.0 and 5.1 devices (API 21 and 22) stop receiving
updates.

## Context

Sprint-017 (U-060) put the build on JDK 25 with Gradle 9.5.1 and AGP 9.0.1. It was
deliberately conservative: it pinned the lowest compatible set and kept Kotlin 2.3.x
because Hilt 2.59.2 cannot read Kotlin 2.4 metadata (U-060 plan, Version Decisions). The
product owner has since asked for "the app on the latest of everyhing it uses" and
accepted "raising the minsdk version". Snapshot of 2026-11-12, current to latest stable:

| Component | Current | Latest stable (snapshot) |
|---|---|---|
| Gradle | 9.5.1 | 9.9.1 |
| AGP | 9.0.1 | 9.5.1 |
| Kotlin Gradle plugin / stdlib | 2.3.21 | 2.4.21 |
| KSP | 2.3.6 | 2.3.13 |
| Hilt | 2.59.2 | 2.60.1 |
| androidx.hilt | 1.2.0 | 1.4.0 |
| Robolectric | 4.15.1 | 4.17 |
| Mockito | 5.18.0 | 5.24.0 |
| GreenMail | 2.1.3 | 2.1.15 |
| JaCoCo | 0.8.14 | 0.8.16 |
| auto-service | 1.0 | 1.1.1 |

Known couplings the implementing story must plan around:

- Hilt 2.60, androidx.hilt 1.3 or later, and Robolectric 4.16 or later require minSdk 23.
- Kotlin 2.4 and Hilt 2.60 must move together, because Hilt 2.59 fails on Kotlin 2.4 metadata.
- Four tests in `app/src/test/java/com/zegoggles/smssync/compat/SmsReceiverTest.java`
  run at `Build.VERSION_CODES.LOLLIPOP` (SDK 21) and cannot run once minSdk is 23.
- No Gradle/AGP/Kotlin triple is listed as supported at stable versions by all three
  vendors (U-060 plan.md, SC-5). The chosen set therefore needs its support reading
  recorded, not assumed.
- Under AGP 9, Robolectric receives a unit-test manifest without the app's own
  components. Three `receiver` class-declaration lines stopped being executed in
  sprint-017, and the `receiver` package sits at exactly its 50% line floor.
- CI has never run on the current toolchain, and its lint step was already failing
  before the JDK 25 move (`.github/workflows/ci.yml` runs
  `./gradlew test lint jacocoTestReport assembleRelease`).

## Requirements

### Latest stable, re-resolved at planning time

"Latest stable" means the newest non-pre-release version on Maven Central, Google Maven,
or the Gradle plugin portal on the date the implementing story is planned. The table
above is a snapshot, not a target. The story re-resolves every row and records the date
and source URL for each.

- [ ] AC-1: The implementing story records, per in-scope component, the resolution date
  and source. The version in the build files (`gradle/wrapper/gradle-wrapper.properties`,
  `build.gradle`, `app/build.gradle`, `gradle/libs.versions.toml` or equivalent) equals
  the recorded latest stable. No in-scope component is on an alpha, beta, or rc version.
  Verified by comparing the recorded table to the build files.

### Mutual compatibility evidence

- [ ] AC-2: The implementing story records, with verbatim quotations and URLs, how the
  chosen Gradle, AGP, Kotlin, KSP, Hilt, and Robolectric versions are documented as
  compatible. Sources are the Gradle compatibility page for the chosen Gradle version,
  the AGP release notes (minimum Gradle, JDK, Build Tools), the Kotlin Gradle plugin
  table, and the KSP and Dagger release notes.
- [ ] AC-3: Where no triple is listed as supported at stable versions by all vendors,
  the record states which reading was applied, using the SC-5 method (primary sources
  treated as binding, secondary sources as checks). It lists each source gap as a
  residual risk and names the detection method. Verified in code review; it is a
  documentation criterion.
- [ ] AC-4: If the latest-stable set cannot build, the story records the failing error
  verbatim and the version it fell back to for that component, and flags the fallback for
  product-owner acknowledgement. It does not silently pin an older set.

### minSdk 23 and its consequences

- [ ] AC-5: `app/build.gradle` declares `minSdkVersion 23` and the resolved merged
  manifest reports `android:minSdkVersion="23"`. Verified by inspecting the merged
  manifest of the debug build. The stale `minSdk 21` comment near the WorkManager
  dependency in `app/build.gradle` is corrected.
- [ ] AC-6: No `Build.VERSION.SDK_INT` comparison or `@TargetApi`/`@RequiresApi`
  annotation for a level below 23 remains in `app/src/main`. Sites found in the repo at
  draft time (the story re-greps and deletes the dead branches): `compat/SmsReceiver.java`
  (`KITKAT`), `activity/SmsDefaultRoleHelper.kt` (`KITKAT`), `activity/MainActivity.java`
  (`KITKAT` annotation), `activity/Dialogs.java` (`ICE_CREAM_SANDWICH`), and
  `calendar/CalendarAccessorPost40.java` (`ICE_CREAM_SANDWICH` annotation). Verified by
  `grep -rn "SDK_INT\|VERSION_CODES\|TargetApi" app/src/main`, with every remaining match
  at level 23 or above.
- [ ] AC-7: The four SDK 21 tests in `SmsReceiverTest` are retargeted to a level of 23
  or above, or deleted together with the dead branch they covered (recorded in the
  story). `grep -rn "LOLLIPOP\|sdk = \[\?2[0-2]" app/src/test` returns zero matches, and
  the test count change is listed by name.
- [ ] AC-8: `app/lint-baseline.xml` is regenerated or pruned so that every entry made
  stale by the minSdk raise (including `ObsoleteSdkInt` entries) is removed. No new
  suppression is added to `app/lint.xml` to hide a minSdk-related finding. The story
  lists the baseline entry count before and after.
- [ ] AC-9: Change records are created on REQ-MODERNIZATION-001 and
  REQ-MODERNIZATION-006 recording that their literal minSdk 21 statements are superseded
  by this requirement. Neither artifact is edited silently.
- [ ] AC-10: A user-facing changelog or release-notes entry states that Android 5.0 and
  5.1 are no longer supported, in the location the project already uses for release
  notes. The story names that location. Publishing and rollout remain out of scope.

### Build integrity and gates

- [ ] AC-11: `gradle/verification-metadata.xml` covers every new or changed artifact,
  and every addition was produced by Gradle's own writer (`--write-verification-metadata`).
  Dependency verification stays enforced. `git diff` shows no hand-edited checksum and
  no added `<trust>` or `verify-metadata="false"` exception that was not already present.
- [ ] AC-12: On JDK 25 (`gradle/gradle-daemon-jvm.properties` unchanged), the following
  all succeed: `./gradlew :app:assembleDebug`, `./gradlew :app:testDebugUnitTest`, and
  `./gradlew :app:jacocoTestCoverageVerification`.
- [ ] AC-13: `testDebugUnitTest` reports zero failures and no more than 2 skipped tests.
  The run count equals the sprint-017 baseline of 697 plus any tests added, minus only
  the tests deleted under AC-7, each listed by name.
- [ ] AC-14: No per-package JaCoCo floor in `app/build.gradle` is lowered. Every
  package is at or above its configured minimum, with `receiver` at or above 50%. If
  the toolchain move changes receiver coverage, the story restores it with tests (for
  example, a `BackupBroadcastReceiver` unit test) or by correcting the unit-test
  manifest, never by editing the floor.
- [ ] AC-15: Bytecode targets remain app 17 and `:k9mail-vendored` 8, unless a
  toolchain component forces a change. Any change is recorded with the forcing error.
- [ ] AC-16: The CI workflow (`.github/workflows/ci.yml`) uses action versions and an
  SDK Build Tools version consistent with the chosen AGP. The story records the AGP
  minimum Build Tools value it relied on.

### On-device launch and first CI run

- [ ] AC-17: A debug APK built from the story branch is installed on a device or
  emulator at API 23 (the new minimum) and on one at a higher API level. On each, the app
  launches to `MainActivity` and a manual backup and a manual restore run to completion,
  with no `FATAL EXCEPTION` or Hilt initialization error in `adb logcat`. Device, API
  level, and logcat excerpt are recorded.
- [ ] AC-18: The story's pull request triggers the first CI run on the updated toolchain.
  The result of every step is recorded, including `sdkmanager`, `lint`, and the
  coverage gate. A failing `lint` step is either fixed or recorded as pre-existing with
  its failing rule IDs and a follow-up item. It is not hidden by removing the step.

## Rationale

EPIC-MODERNIZATION-006 requires every dependency to be at the latest stable version
when its story is planned. The toolchain moves first, because Hilt 2.60, androidx.hilt
1.3 or later, and Robolectric 4.16 or later are the components that force the minSdk
raise, and the later runtime-library and platform-level requirements depend on the
newer plugins. Recording the compatibility reading (AC-2, AC-3) matters because
vendor support tables disagree. Without a recorded reading, a future upgrade cannot
tell whether a warning is expected.

## Constraints

- **Behavior preserved**: backup, restore, scheduling, and notification behavior is
  identical before and after, apart from the dropped API 21 and 22 support.
- **JDK 25 stays**: the build runs on JDK 25 via `gradle/gradle-daemon-jvm.properties`.
- **Verification enforced**: dependency verification is not disabled; metadata is
  added only through Gradle's writer.
- **No floor lowered**: no per-package JaCoCo floor is lowered.
- **Increments**: each version bump should land as its own verified increment so a
  regression is attributable to one change (epic sequencing note). If Kotlin 2.4 and
  Hilt 2.60 cannot be separated, they are one increment.
- **Out of scope**: runtime libraries (WorkManager, Lifecycle, coroutines), compileSdk
  and targetSdk, donation removal, and the mail library replacement belong to the other
  EPIC-MODERNIZATION-006 requirements.

## Verification Method

1. Static: greps in AC-6, AC-7, and the merged-manifest check in AC-5.
2. Build and test gates on JDK 25: AC-12, AC-13, AC-14.
3. Metadata review: `git diff gradle/verification-metadata.xml` for AC-11.
4. Documentation review: AC-1 to AC-4, AC-9, AC-10 in the implementation log.
5. Device run: AC-17. First CI run: AC-18.

## Notes

- Sprint-017 retro states the cause of the lost receiver lines (AGP 9 unit-test
  manifest) and lists "a decision on correcting the AGP 9 unit-test manifest" as an open
  follow-up. That decision is not made here; AC-14 only requires the floor to hold.
