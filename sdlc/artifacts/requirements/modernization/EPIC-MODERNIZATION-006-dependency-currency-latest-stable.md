---
status: approved
artifact_type: epic
related_requirements: []
related_stories: []
change_records: []
id: EPIC-MODERNIZATION-006
title: 'Dependency Currency: Latest Stable Toolchain, Libraries, Platform Level, and Mail Stack'
domain: modernization
updated_at: '2026-11-12T16:45:22.311Z'
---

# Dependency Currency: Latest Stable Toolchain, Libraries, Platform Level, and Mail Stack

## Description

Sprint-017 moved the build onto JDK 25 (Gradle 9.5.1 / AGP 9.0.1) with 697 unit tests green,
but the app still ships a mixed-age dependency set: build and test tooling one or more
releases behind, runtime libraries several minor versions behind (WorkManager 2.9.1,
Lifecycle 2.6.2, coroutines 1.7.3, preference 1.1.0), a Play Billing 7.1.1 dependency that
exists only for the donation flow, and a vendored copy of the K-9 mail library
(`:k9mail-vendored` plus commons-io, apache-mime4j, jzlib, jutf7) isolated behind the
`MailTransport` anti-corruption layer. The product owner's goal is "the app on the latest of
everything it uses", with the Thunderbird for Android migration in scope, the donation flow
removed rather than upgraded, and a minSdk raise to 23 accepted. This epic brings every
dependency to the latest stable release available when its story is planned, and retires the
vendored mail stack, without changing user-visible backup or restore behavior (other than the
removed donation flow and the dropped Android 5.0/5.1 support).

"Latest stable" is defined per story as the newest non-pre-release version published on
Maven Central, Google Maven, or the Gradle plugin portal on the date that story is planned.
Version figures that appear in the requirements are a snapshot taken on 2026-11-12 and are
not targets; each story re-resolves them.

## Scope

### In Scope

- **Build toolchain and test tooling on latest stable, minSdk 23** — Gradle, AGP, Kotlin,
  KSP, Hilt, androidx.hilt, Robolectric, Mockito, GreenMail, JaCoCo, auto-service.
  Snapshot 2026-11-12: Gradle 9.5.1 to 9.9.1; AGP 9.0.1 to 9.5.1; Kotlin 2.3.21 to 2.4.21;
  KSP 2.3.6 to 2.3.13; Hilt 2.59.2 to 2.60.1; androidx.hilt 1.2.0 to 1.4.0; Robolectric
  4.15.1 to 4.17; Mockito 5.18.0 to 5.24.0; GreenMail 2.1.3 to 2.1.15; JaCoCo 0.8.14 to
  0.8.16; auto-service 1.0 to 1.1.1. Hilt 2.60, androidx.hilt 1.3+, and Robolectric 4.16+
  require minSdk 23, so `minSdkVersion` is raised from 21 to 23 and Android 5.0/5.1 support
  ends. This supersedes the literal minSdk 21 statements in REQ-MODERNIZATION-001 (AC-2) and
  REQ-MODERNIZATION-006 (AC-3, AC-5); those artifacts must be revised through change
  records, not silently contradicted.
- **Runtime libraries on latest stable** — WorkManager 2.9.1 to 2.12.0; Lifecycle 2.6.2 to
  2.11.0; kotlinx-coroutines 1.7.3 to 1.11.0; androidx.preference 1.1.0 to 1.2.1;
  androidx.annotation 1.1.0 / 1.7.1 to 1.11.0; androidx.core:core-role 1.0.0-beta01 to 1.1.0
  (snapshot figures as above).
- **Platform level** — raise `compileSdk` and `targetSdk` from 35 to the current stable
  Android API level, with on-device validation of the affected behavior changes.
- **Donation flow removal** — delete the donation flow entirely (UI, preferences, strings,
  manifest entries, tests) and remove the Play Billing 7.1.1 dependency rather than upgrade
  it. This supersedes REQ-MODERNIZATION-010 (upgrade Play Billing to v7).
- **Mail library replacement** — replace `:k9mail-vendored` and its commons-io /
  apache-mime4j / jzlib / jutf7 dependencies with Thunderbird for Android's current mail
  libraries, behind the existing `MailTransport` ACL. A written feasibility analysis must be
  accepted before any design or implementation story starts.

### Out of Scope

- RCS support and the full Google OAuth sign-in flow (later product goals), unless the
  Thunderbird feasibility analysis shows they are inseparable from the mail migration; in
  that case the analysis records the dependency and the product owner decides before design.
- Publishing and release decisions (store listing, release signing, rollout).
- New user-facing features other than what is stated above.
- Fixing BUG-018 (open, unscheduled); this epic neither schedules nor depends on it.
- Lowering any per-package JaCoCo floor, disabling dependency verification, or moving off
  JDK 25.

## Success Criteria

Each is verifiable without a live IMAP account unless stated.

1. **Currency**: for every dependency and plugin named in the in-scope list, the version in
   the build files equals the latest stable version re-resolved on the date its story was
   planned, with the resolution date and source recorded in the story. Zero named
   dependencies remain on a pre-release (alpha/beta/rc) version, including core-role.
2. **minSdk**: `minSdkVersion` is 23 in `app/build.gradle`; no `Build.VERSION.SDK_INT`
   guard for a level below 23 remains in `app/src/main`; REQ-MODERNIZATION-001 and
   REQ-MODERNIZATION-006 carry change records recording the supersession.
3. **Platform level**: `compileSdk` and `targetSdk` equal the current stable API level
   (verified against the Android developer documentation at planning time), and the
   backup, restore, scheduling, and notification flows are exercised on at least one
   physical or emulated device at that API level with results recorded.
4. **Donation removal**: `grep -ril "billing\|donat" app/src/main app/build.gradle` returns
   zero results, the Play Billing coordinate is absent from the dependency graph, and no
   billing-related permission, manifest entry, or preference key remains.
5. **Vendored mail stack retired**: the `:k9mail-vendored` module is absent from
   `settings.gradle`, and commons-io, apache-mime4j, jzlib, and jutf7 are absent from the
   resolved runtime classpath unless the accepted feasibility analysis records a specific
   retained exception; `MailTransport` remains the only mail surface seen by `service.*`
   (`grep -rn "com\.fsck\.k9\|com\.android\.email" app/src/main/java/com/zegoggles/smssync/service/`
   returns zero results).
6. **Feasibility gate honored**: an accepted feasibility analysis exists before the first
   mail-migration design story, and it states whether RCS and Google OAuth are separable.
7. **Tests green**: `./gradlew :app:testDebugUnitTest` completes with zero failures and no
   more skipped tests than the sprint-017 baseline (697 run, 2 skipped), allowing for tests
   deleted with the donation flow, which are listed in the story that deletes them.
8. **Gates hold**: `./gradlew :app:jacocoTestCoverageVerification` passes with no per-package
   floor lowered (receiver stays at or above its 50% floor, others at or above 70%);
   dependency verification remains enforced and `gradle/verification-metadata.xml` covers
   every new artifact; `./gradlew :app:assembleDebug` succeeds on JDK 25.
9. **Behavior preserved**: IMAP backup and restore round-trip results against a test server
   (GreenMail in unit tests; live-account run where available) are identical before and
   after the mail migration for message content, headers, MIME parts, and folder naming.

## Related Requirements

Populated as REQ-* artifacts are authored. Planned requirements:

1. Build toolchain and test tooling on latest stable, with minSdk 23.
2. Runtime libraries on latest stable.
3. Platform level (compileSdk/targetSdk) on current stable, with on-device validation.
4. Remove the donation flow and the Play Billing dependency.
5. Replace the vendored K-9 mail library with Thunderbird for Android mail libraries.

## Notes

### Sequencing

- Requirement 1 first: the toolchain and minSdk raise unlock the versions required by the
  rest. Requirement 4 should precede requirement 3, because removing Play Billing avoids
  validating and migrating a library that is being deleted. Requirement 2 can follow
  requirement 1. Requirement 5 starts with its feasibility analysis, which can run in
  parallel with requirements 1 to 4 and whose outcome gates all mail design work.
- Each version bump should land as its own verified increment so a regression is
  attributable to one change.

### Constraints and Risks

- CI has never run on the current toolchain and its lint step was already failing before
  the JDK 25 move. This epic does not assume CI is a trustworthy gate; the first CI run on
  the updated toolchain is a verification event to plan for, and fixing the lint failure is
  not in scope unless a story elects it.
- The receiver package sits exactly at its 50% JaCoCo floor, so any change that removes
  covered receiver lines (for example, donation removal) can breach it without lowering the
  floor; stories touching receiver code must check coverage.
- Dropping Android 5.0/5.1 is a user-facing support reduction; whether to communicate it is
  a publishing matter and is not decided here.

### Open Questions

- Which Android API level is current stable at planning time is not yet verified.
- Whether Thunderbird's mail libraries are consumable as published artifacts, their
  licensing compatibility with this project's license, and whether they impose minSdk,
  Kotlin, or coroutine-version requirements beyond those above are unknown until the
  feasibility analysis completes.
- Scope Concern: requirement 5 may be large enough to exceed a single sprint and may
  conflict with requirement 2 (coroutines/Kotlin versions the Thunderbird libraries
  require). It is kept in scope as approved; the feasibility analysis should report
  whether it needs to be split into a follow-on epic.

### Traceability

- `sdlc/artifacts/build/sprints/sprint-017/retro.md`
- `sdlc/artifacts/build/sprints/sprint-017/U-060/qa-results.md`
- `sdlc/analysis/20260623-post-migration-assessment/outputs/sms-backup-plus/post-migration-health-report.md`
- Format reference: `sdlc/artifacts/requirements/modernization/EPIC-MODERNIZATION-005-modernization-debt-closure.md`
