---
source_assessment: 20260529-modernization
source_file: reports/modernization-plan.md (Remediation Roadmap → Immediate 0–30 days); outputs/sms-backup-plus/modernization/gaps.md; outputs/sms-backup-plus/modernization/requirements/migration-units.md
finding_ref: phase0-shippable-secure
promoted_date: 2026-05-30
domain: modernization
---

# Finding: Phase-0 "Shippable + Secure" Modernization Milestone (Month 1)

The SMS Backup+ modernization assessment (`20260529-modernization`) concluded the app
has a reference-quality domain core on a decade-rotted external substrate, and that the
correct path is **substrate replacement, not rewrite**. The single highest-value slice is
the **Phase-0 / Month-1 milestone**: the work required to make the app **shippable to Google
Play again** and **close the active security exposure**. Per the roadmap, ~80% of the
business value is front-loaded here, and all three work items below have **no upstream
dependency** — they start in parallel on day one.

This promotion bundles three findings into one cohesive milestone. They are grouped because
they share a milestone (M0/M1), are mutually parallelizable, and together define the
"app is shippable and secure" gate. They may be decomposed into separate REQ-* artifacts
during authoring if that improves clarity — but their traceability and milestone grouping
must be preserved.

---

## Finding 1 — TECH-001 (Critical): targetSdk 29 blocks Google Play updates → MU-001

**Severity:** Critical · **Migration Unit:** MU-001 (Build & Platform Uplift — "the gate")
**Source IDs:** TECH-001 / DEBT-001 (gaps.md), QUAL-001 / CAP-002 (FLAG_IMMUTABLE), TECH-007 / DEP-006 (jcenter)

`compileSdkVersion 29` / `targetSdkVersion 29` (`app/build.gradle:10,15`) sit 5 API levels
below Google Play's enforced floor (API 34, enforced since Aug 2024). **No update — including
security fixes — can reach users through Play until this is raised.** This is the unconditional
prerequisite for every other gap closure that must reach users.

Scope of the conformance work when raising to targetSdk 35:
- Stage the Android Gradle Plugin upgrade incrementally: AGP 4.1.3 → 7.4 → 8.x (avoid skipping
  too many breaking changes at once); Gradle 7.2 → 8.x to match.
- Co-land `FLAG_IMMUTABLE` on the two `PendingIntent` constructions that hard-crash
  (`IllegalArgumentException`) on API 31+ once the target is raised:
  `service/AlarmManagerDriver.java:127` and `service/ServiceBase.java:204` (QUAL-001/CAP-002).
  **This MUST co-land inside the SDK raise** or the raise introduces a crash on the scheduling path.
- Declare `android:exported` explicitly on the four `<receiver>` elements in `AndroidManifest.xml`.
- Add `FOREGROUND_SERVICE_DATA_SYNC` foreground-service type; request `POST_NOTIFICATIONS` at runtime.
- Remove `jcenter()` (declared twice in root `build.gradle` lines 4 & 21, plus `jcenter.bintray.com`
  at line 25) — JCenter shut down Feb 2022 (TECH-007, ~30-min fix).

**Outcome / acceptance:** A signed AAB at targetSdk 35 passes Play pre-launch review;
no `PendingIntent` lacks a mutability flag; `jcenter` declarations are absent (Gate G0 / Milestone M0).

---

## Finding 2 — SEC-001 (Critical): Trust-all TLS + silent security-downgrade migration → MU-003

**Severity:** Critical · **Migration Unit:** MU-003 (Transport Security Hardening)
**Source IDs:** SEC-001 / ARCH-003 (gaps.md), ARCH-008 (silent migration)
**Dependency:** NONE — runs in parallel with TECH-001 from day one.

`mail/AllTrustedSocketFactory.java:42-57` contains an `InsecureX509TrustManager` whose
`checkServerTrusted()` body is **empty** — it accepts any certificate from any server (CWE-295).
`preferences/AuthPreferences.migrate()` (`:276-288`) **silently** enables this trust-all path
for users who previously configured a `+ssl`/`+tls` IMAP server address, upgrading them into an
MITM-vulnerable state without their knowledge.

Exploit path: a user with trust-all enabled connects over hostile Wi-Fi; an on-path attacker
presents a self-signed cert for `imap.gmail.com`; `checkServerTrusted` accepts it; the attacker
terminates TLS, reads the entire SMS/MMS/call-log backup stream, and captures the Gmail app
password / OAuth2 token in transit.

Required resolution:
- Delete `AllTrustedSocketFactory`; replace with validated TLS.
- Provide an **explicit, user-initiated** pinned-certificate path for users who genuinely
  self-host an IMAP server with a private CA (opt-in, never silent).
- Rewrite `migrate()` so it **never** silently sets `SERVER_TRUST_ALL_CERTIFICATES = true`;
  surface a one-time notice instead of a silent downgrade.

**Outcome / acceptance:** No code path accepts an unvalidated TLS certificate; `migrate()` is
covered by a test confirming it never silently enables trust-all (Gate G1 / Milestone M1).

---

## Finding 3 — MU-002 (High): No CI, no coverage gate — the safety net → MU-002

**Severity:** High · **Migration Unit:** MU-002 (Test-Harness & Coverage Gate)
**Source IDs:** OPS-001 / PROC-001 (no CI), OPS-002 / PROC-002 (coverage unmeasured), TECH-004 / DEP-004 (test toolchain)
**Constraint:** This is the governance safety net — **no substrate swap (Phase 2+) may begin until
this gate is in place.** Robolectric upgrade is a co-requisite of the SDK raise (Finding 1).

There is no `.github/workflows/` directory (the repo has only a stale Travis badge). No automated
build/test/lint/coverage runs on PRs. The 35-class Robolectric suite has good breadth but
**unmeasured** total coverage (no JaCoCo config; test:prod LOC ratio 0.33:1). `MainActivity`
(highest-complexity file) and `OAuth2Client` have no behavioral tests;
`SmsBackupServiceTest.java:212` has an empty `// TODO` body. `minifyEnabled false`
(`app/build.gradle:33`) means release builds never exercise R8.

Required scope:
- GitHub Actions workflow running `./gradlew test lint jacocoTestReport assembleRelease` on every PR.
- Add JaCoCo with a **≥70% coverage gate** on `service/`, `mail/`, and `auth/` packages.
- Add **characterization tests** (Feathers Ch. 2) pinning behavior **before** any refactor —
  specifically `AuthPreferences.migrate()` (protects the SEC-001 fix) and `BackupJobs` retry
  (30/300 exponential backoff) + network-constraint behavior.
- Upgrade test toolchain: Robolectric `4.3.1 → 4.12.x` (**hard co-requisite** of the SDK raise —
  4.3.1 cannot run tests above SDK 29); address `junit:4.12` (CVE-2020-15250) and
  `mockito-all:1.10.17` (conflicts with mockito-core 5.x).
- Add `gradle/verification-metadata.xml` for supply-chain verification.

**Outcome / acceptance:** CI green on PRs; JaCoCo ≥70% on service/mail/auth; characterization
tests for `migrate()` and `BackupJobs` pass (Gate G2 — precondition for all Phase-2 substrate work).

---

## Evidence

All evidence is from the completed, review-passed assessment `20260529-modernization`
(static source analysis, file:line-cited). Key source artifacts (paths relative to repo root):

- `sdlc/analysis/20260529-modernization/reports/modernization-plan.md` — Remediation Roadmap →
  "Immediate (0–30 days)" table (MU-001, MU-002, MU-003); Decision Gates G0/G1/G2; Milestones M0/M1.
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/gaps.md` — canonical
  findings: TECH-001, SEC-001, OPS-001, OPS-002, QUAL-001/CAP-002, TECH-004, TECH-007.
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md` —
  MU-001, MU-002, MU-003 definitions with boundaries, dependencies, and acceptance criteria.
- `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/architecture/ilities-assessment.md` —
  Security (🔴) and the underlying ARCH-003/008/009 detail.

Code locations cited (in this repository):
- `app/build.gradle:10,15` (SDK), `:33` (minifyEnabled false), root `build.gradle:4,21,25` (jcenter)
- `app/src/main/java/com/zegoggles/smssync/mail/AllTrustedSocketFactory.java:42-57`
- `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java:276-288` (migrate), `:221-226` (getCredentials)
- `app/src/main/java/com/zegoggles/smssync/service/AlarmManagerDriver.java:127`, `service/ServiceBase.java:204`
- `app/src/main/AndroidManifest.xml` (receivers, FGS type, permissions)

## Suggested Requirement Framing

Author as a **modernization milestone** comprising (recommended) three REQ-* artifacts under the
`modernization` domain, milestone-grouped as "Phase-0 / Shippable + Secure":

1. **Capability + Constraint** — *Restore Google Play eligibility* (SDK/AGP uplift + API-31+
   conformance + jcenter removal). Acceptance: Play-accepted signed AAB at targetSdk 35; no
   crash-on-raise regressions.
2. **Security / Quality Attribute** — *Eliminate transport-security MITM exposure* (remove trust-all
   TLS; consensual pinned-cert path; non-silent migration). Acceptance: no unvalidated-cert path;
   `migrate()` proven non-downgrading by test.
3. **Process / Quality Gate (enabling)** — *Establish CI + coverage safety net* (GitHub Actions;
   JaCoCo ≥70% on service/mail/auth; characterization tests; Robolectric uplift; verification-metadata).
   Acceptance: green CI gate that must pass before any Phase-2 substrate work.

Preserve traceability from each REQ back to its source finding IDs (TECH-001/SEC-001/OPS-001/OPS-002/
QUAL-001/TECH-004/TECH-007) and migration units (MU-001/MU-002/MU-003). Sequencing note for design:
all three start in parallel; the Robolectric uplift (Finding 3) is a co-requisite of the SDK raise
(Finding 1); FLAG_IMMUTABLE (Finding 1) must co-land with the SDK raise.
