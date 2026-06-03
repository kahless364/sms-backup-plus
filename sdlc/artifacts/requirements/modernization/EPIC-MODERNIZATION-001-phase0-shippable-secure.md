---
id: EPIC-MODERNIZATION-001
artifact_type: epic
title: "Phase-0: Restore Play-Store Eligibility & Close Security Exposure"
status: approved
domain: modernization
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-001
  - REQ-MODERNIZATION-002
  - REQ-MODERNIZATION-003
related_stories: []
change_records: []
spec_ref: "sdlc/analysis/20260529-modernization/reports/modernization-plan.md"
notes: |
  Sequencing constraints:
  (1) Robolectric uplift (REQ-MODERNIZATION-003, MU-002) is a hard co-requisite of the
  SDK raise (REQ-MODERNIZATION-001, MU-001) — Robolectric 4.3.1 cannot execute tests
  above SDK 29; both changes must land in the same build transaction.
  (2) FLAG_IMMUTABLE at AlarmManagerDriver.java:127 and ServiceBase.java:204 must
  co-land inside the SDK raise or the raise introduces an IllegalArgumentException crash
  on API 31+ scheduling paths.
  (3) The CI gate (REQ-MODERNIZATION-003, Gate G2) must pass before any Phase-2
  substrate work (MU-005 WorkManager, MU-006 Otto→Flow, MU-007 Hilt, MU-008 k-9 ACL,
  Kotlin promotion, MU-009 Billing 7.x) may be planned or started.
  (4) REQ-MODERNIZATION-002 (MU-003, trust-all TLS) is independent of the SDK gate and
  starts day 1 in parallel; the SDK raise worsens the MITM exposure via
  AuthPreferences.migrate() so early start is mandatory.
  Source findings: TECH-001, QUAL-001/CAP-002, TECH-007 (REQ-001); SEC-001/ARCH-003/
  ARCH-008 (REQ-002); OPS-001, OPS-002, TECH-004/DEP-004 (REQ-003).
  Migration units: MU-001 (REQ-001), MU-002 (REQ-003), MU-003 (REQ-002).
  Source assessment: 20260529-modernization.
---

# EPIC-MODERNIZATION-001: Phase-0 — Restore Play-Store Eligibility & Close Security Exposure

## Description

SMS Backup+ presents an architectural asymmetry that determines the entire modernization
strategy: the application has a reference-quality domain core — an immutable state machine,
a type-object `DataType` enum, a typed exception hierarchy spanning 25 locales, and a
disciplined preferences facade — sitting on a decade-rotted substrate of abandoned build
tooling, dead runtime dependencies, and an active transport-security vulnerability. The
correct response is not a rewrite; it is substrate replacement behind preserved interfaces.
Phase-0 is the first and highest-leverage slice of that work: the "healthy core, dead
shell" framing makes explicit that the app's business value is fully intact and the only
thing blocking users from receiving updates is a set of external substrate failures. This
epic covers the three work items — build and platform uplift (MU-001), transport-security
hardening (MU-003), and test-harness establishment (MU-002) — that together constitute the
**"shippable and secure"** milestone. No other work item in the full modernization roadmap
carries comparable value concentration: approximately 80% of the total engagement's
risk-reduction and user-protective value lands in Month 1. `targetSdkVersion 29` has blocked
every update to every user since August 2024 (Google Play's enforced API 34 floor); the
`AllTrustedSocketFactory` trust-all TLS path has been silently enrolling legacy users into
an MITM-exploitable configuration via `AuthPreferences.migrate()` for an indeterminate
period; and the complete absence of CI or coverage measurement means that every subsequent
substrate swap would proceed against an unmeasured codebase with no regression gate. All
three items are independent of each other and of every Phase-1+ work item, so they start
in parallel on day one. No Phase-2 substrate swap (WorkManager, Otto→Flow, Hilt, k-9 ACL,
Kotlin promotion, Billing 7.x) may begin until the CI gate established by this epic is
green and the JaCoCo ≥70% threshold is passing — that gate is the governance precondition
for all subsequent refactoring.

## Scope

### In Scope

- **Build and platform uplift (MU-001 / REQ-MODERNIZATION-001):** Raise
  `compileSdkVersion`/`targetSdkVersion` from 29 to 35; stage AGP upgrade from 4.1.3 → 7.4
  → 8.x with matching Gradle 8.x; co-land `FLAG_IMMUTABLE` on the two `PendingIntent`
  constructions at `service/AlarmManagerDriver.java:127` and `service/ServiceBase.java:204`
  that will hard-crash (`IllegalArgumentException`) on API 31+ the moment the target is
  raised; declare `android:exported` explicitly on the four `<receiver>` elements in
  `app/src/main/AndroidManifest.xml`; add `FOREGROUND_SERVICE_DATA_SYNC` foreground-service
  type and `POST_NOTIFICATIONS` runtime permission request; remove all three `jcenter()`
  declarations from `build.gradle` (lines 4, 21, 25). Produce a signed AAB that passes Play
  pre-launch review at targetSdk 35.

- **Transport-security hardening (MU-003 / REQ-MODERNIZATION-002):** Delete
  `mail/AllTrustedSocketFactory.java` (the `InsecureX509TrustManager` whose
  `checkServerTrusted()` body is empty, accepting any certificate unconditionally, CWE-295);
  replace all callsites with validated TLS; introduce an explicit, user-initiated
  pinned-certificate path for users who self-host IMAP with a private CA; rewrite
  `preferences/AuthPreferences.migrate()` (lines 276–288) so it never silently sets
  `SERVER_TRUST_ALL_CERTIFICATES = true` — replace the silent downgrade with a one-time
  user-visible notice.

- **Test-harness and coverage gate (MU-002 / REQ-MODERNIZATION-003):** Establish a GitHub
  Actions workflow running `./gradlew test lint jacocoTestReport assembleRelease` on every
  PR; add JaCoCo configuration with a hard ≥70% line/branch gate on the `service/`, `mail/`,
  and `auth/` packages; write characterization tests (Feathers Ch. 2) that pin behavior
  before any refactor — specifically `AuthPreferences.migrate()` (protects the SEC-001 fix)
  and `BackupJobs` retry semantics (30/300 exponential backoff) and network-constraint
  behavior; upgrade Robolectric from 4.3.1 to 4.12.x (a hard co-requisite of MU-001: 4.3.1
  cannot execute tests above SDK 29); address `junit:4.12` (CVE-2020-15250) and
  `mockito-all:1.10.17` conflicts; add `gradle/verification-metadata.xml` for supply-chain
  verification.

### Out of Scope

The following items are explicitly deferred to Phase-1+ and must not be included in
Phase-0 delivery:

- **WorkManager substrate swap (MU-005):** Replacement of `firebase-jobdispatcher`,
  `AsyncTask`, and `AlarmManagerDriver` with WorkManager + `CoroutineWorker`. This depends
  on the Phase-0 CI gate being green (G2) and the Phase-1 Hexagonal port introduction
  (MU-006) completing first.
- **Otto → StateFlow/SharedFlow (MU-006e):** Removal of the `com.squareup:otto:1.3.8`
  event bus and replacement with `StateFlow<SyncState>` + `SharedFlow<SyncEvent>`. Sequenced
  after WorkManager to avoid double-churning the engine concurrently.
- **Hilt DI formalization (MU-007):** Constructor injection and service-locator removal.
  Phase-2 substrate item.
- **k-9 ACL and mail-library unpin (MU-008):** Introduction of the `MailTransport`
  Anti-Corruption Layer and migration from the JitPack SHA pin to a reproducible coordinate.
  Phase-2 substrate item.
- **EncryptedSharedPreferences (MU-004):** Credential-at-rest encryption via Jetpack
  Security. Phase-1 item, sequenced after the Phase-0 gate closes.
- **Kotlin promotion:** Staged Kotlin migration of production files. Phase-3 item.
- **Play Billing 7.x (MU-009):** `SkuDetails` → `ProductDetails` API migration. Phase-3
  item, isolated to three donation-subsystem files.
- **Durable restore checkpoint (MU-010):** Persistent restore cursor and idempotent
  duplicate-detection. Depends on WorkManager (Phase 2).
- **Dead-code sweep and minSdk cleanup (MU-011):** Deletion of `CalendarAccessorPre40.java`
  and related pre-API-14 compat surface. Low-priority sweep, eligible for opportunistic
  co-landing during MU-001 but not a deliverable of this epic.

## Success Criteria

The following gates define the "done" boundary for Phase-0. Each gate must be demonstrated
in CI before Phase-1+ substrate work begins.

**G0 — Play Eligibility Restored (Milestone M0, end of Month 1):**
A signed AAB with `targetSdkVersion 35` and `compileSdkVersion 35` passes Google Play
pre-launch review without policy violations. Every `PendingIntent` in the codebase carries
either `FLAG_IMMUTABLE` or `FLAG_MUTABLE`; no `PendingIntent` construction omits a
mutability flag. `jcenter()` and `jcenter.bintray.com` are absent from all `build.gradle`
files. CI assembles the release APK/AAB successfully on every PR.

**G1 — Active MITM Exposure Eliminated (Milestone M1, end of Month 1, parallel with G0):**
No code path in the application accepts a TLS certificate without performing full chain
validation against the system trust store or a user-explicitly-pinned CA. `AllTrustedSocketFactory`
does not exist anywhere in the codebase. `AuthPreferences.migrate()` is covered by an
automated test that asserts it never sets `SERVER_TRUST_ALL_CERTIFICATES = true` as a side
effect of migration — including for input states that represent users previously configured
with a `+ssl` or `+tls` IMAP server address.

**G2 — CI and Coverage Gate Live (co-requisite for Phase-2, must be green before MU-005
begins):**
The GitHub Actions workflow runs and passes on every PR: `test`, `lint`, `jacocoTestReport`,
and `assembleRelease` all succeed. JaCoCo reports ≥70% line coverage and ≥70% branch
coverage across the `service/`, `mail/`, and `auth/` packages. Characterization tests for
`AuthPreferences.migrate()` and `BackupJobs` retry/constraint behavior are present and
green. `gradle/verification-metadata.xml` is present and enforced in the workflow.

## Related Requirements

- **REQ-MODERNIZATION-001** — Restore Google Play eligibility: SDK/AGP uplift to targetSdk 35,
  `FLAG_IMMUTABLE` co-land, manifest conformance (`android:exported`, FGS type,
  `POST_NOTIFICATIONS`), jcenter removal. Traces to findings TECH-001, QUAL-001/CAP-002,
  TECH-007; migration unit MU-001; gate G0 / milestone M0.

- **REQ-MODERNIZATION-002** — Eliminate transport-security MITM: delete `AllTrustedSocketFactory`,
  replace with validated TLS, introduce opt-in user-pinned-cert path, rewrite `migrate()` as
  non-silent. Traces to finding SEC-001 (ARCH-003, ARCH-008); migration unit MU-003; gate G1 /
  milestone M1.

- **REQ-MODERNIZATION-003** — Establish CI and coverage safety net: GitHub Actions workflow,
  JaCoCo ≥70% on service/mail/auth, characterization tests for `migrate()` and `BackupJobs`,
  Robolectric 4.12.x uplift (co-requisite of MU-001), supply-chain verification metadata.
  Traces to findings OPS-001, OPS-002, TECH-004; migration unit MU-002; gate G2 (Phase-2
  precondition).

## Notes

### Sequencing

All three child requirements are independent of each other and of all Phase-1+ items. They
start in parallel on day one of the engagement. Two co-requisite constraints must be
respected within the parallel execution:

1. **Robolectric uplift (REQ-MODERNIZATION-003) is a hard co-requisite of the SDK raise
   (REQ-MODERNIZATION-001).** Robolectric 4.3.1 cannot execute tests against any SDK level
   above 29. If the SDK raise in MU-001 lands before the Robolectric upgrade in MU-002, the
   existing 35-class test suite breaks immediately with no red-green signal. The two changes
   must be committed in the same build transaction or in a single coordinated PR.

2. **`FLAG_IMMUTABLE` (REQ-MODERNIZATION-001) must co-land inside the SDK raise.** The two
   `PendingIntent` constructions at `AlarmManagerDriver.java:127` and `ServiceBase.java:204`
   are latent at `targetSdk 29` but become `IllegalArgumentException` hard crashes on API 31+
   the instant the target is raised. This is not a follow-up item — it is part of the atomic
   SDK raise commit and must be reviewed as such.

3. **The CI gate (REQ-MODERNIZATION-003, gate G2) must be green before any Phase-2 substrate
   swap begins.** This is an unconditional governance rule: no code that removes or replaces
   a load-bearing library (JobDispatcher, Otto, k-9 binding) may merge until JaCoCo ≥70% is
   confirmed and characterization tests are passing. The rationale is Feathers Ch. 2: a seam
   swap without a pinning test is a refactor against an unmeasured codebase, and coverage
   that is 0.33:1 test:prod LOC leaves most of the engine's behavior unobservable.

### Source Traceability

| Finding ID | Description | Child REQ |
|---|---|---|
| TECH-001 / DEBT-001 | targetSdk 29 blocks Play Store updates | REQ-MODERNIZATION-001 |
| QUAL-001 / CAP-002 | `PendingIntent` without `FLAG_IMMUTABLE` → crash on API 31+ | REQ-MODERNIZATION-001 |
| TECH-007 / DEP-006 | `jcenter()` declarations after 2022 shutdown | REQ-MODERNIZATION-001 |
| SEC-001 / ARCH-003 | Trust-all TLS (`AllTrustedSocketFactory`, CWE-295) | REQ-MODERNIZATION-002 |
| SEC-001 / ARCH-008 | Silent security-downgrade in `AuthPreferences.migrate()` | REQ-MODERNIZATION-002 |
| OPS-001 / PROC-001 | No CI/CD in repository | REQ-MODERNIZATION-003 |
| OPS-002 / PROC-002 | Coverage unmeasured; no fitness function | REQ-MODERNIZATION-003 |
| TECH-004 / DEP-004 | Robolectric 4.3.1 caps at SDK 29; junit CVE-2020-15250 | REQ-MODERNIZATION-003 |

### Migration Unit Boundaries

| MU ID | Name | Phase-0 Role |
|---|---|---|
| MU-001 | Build & Platform Uplift | Primary deliverable of REQ-MODERNIZATION-001; gate G0 |
| MU-002 | Test-Harness & Coverage Gate | Primary deliverable of REQ-MODERNIZATION-003; gate G2 |
| MU-003 | Transport Security Hardening | Primary deliverable of REQ-MODERNIZATION-002; gate G1 |

All migration unit definitions, boundary criteria, dependency edges, and acceptance conditions
are canonical in:
`sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md`

The full remediation roadmap (Immediate 0–30 days table, decision gates G0/G1/G2, milestone
definitions M0/M1) is canonical in:
`sdlc/analysis/20260529-modernization/reports/modernization-plan.md` → Remediation Roadmap →
Immediate (0–30 days).

The canonical gap set (18 findings, severity and dependency edges) is:
`sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/gaps.md`
