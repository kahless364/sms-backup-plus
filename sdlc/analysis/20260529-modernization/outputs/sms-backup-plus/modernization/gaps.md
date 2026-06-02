---
prompt: assessments/prompts/modernization/03-gap-analysis.md
step_id: "2.3"
finding_count: 18
assessment_date: 2026-05-29
confidence_threshold: 0.5
scope: "P01 app module — com.zegoggles.smssync (107 prod Java files; 36 test files); build.gradle, app/build.gradle, gradle wrapper, AndroidManifest.xml. Current state (assessment.md) vs target state (target-state.md)."
phase: modernization
prompt_id: modernization/03-gap-analysis
step: "2.3"
date: 2026-05-29
analyst: Solution Architect (assessment agent)
assessment_id: 20260529-modernization
status: draft
---

# Modernization: Gap Analysis — SMS Backup+

**Engagement:** 20260529-modernization · EXECUTE Step 2.3 (Gap Analysis)
**Subject:** P01 `app` — `com.zegoggles.smssync` (single Android module)
**Quality bar:** `knowledge/standards/expert-exceeding-depth.md`
**Method:** TOGAF-style current→target delta against the verified Phase 1 corpus + Step 2.1 assessment + Step 2.2 target architecture, with this-session grep/build-file re-verification of every load-bearing count. Gap magnitude expressed as S/M/L per finding-schema (S < 4h · M 4–16h · L > 16h); dependency edges and WSJF-style sequencing made explicit per Self-Check #7. All paths relative to repo root.

> **Framing.** This is the inverse of the typical legacy gap analysis. The *structural* gaps are small (the macro-architecture is sound and is **retained**); the *technology* and *process* gaps are the dominant ones (dead substrate, no CI, unmeasured coverage). The target architecture (Step 2.2) does **not** change the layering — it inserts Hexagonal ports at dependency seams and swaps the substrate behind them. Every gap below therefore measures *substrate distance*, not *redesign distance*. Gaps are framed as the work to traverse from the current state to the target, not as net-new findings — each cross-references the Phase 1 / Step 2.1 finding it derives from.

---

## Objective Executed

This step systematically identifies, categorizes (Structural / Technology / Capability / Process), and prioritizes the gaps between the current-state architecture (Step 2.1 assessment.md, Phase 1 ilities/dependencies/debt) and the target-state architecture (Step 2.2 target-state.md). For each gap it states current state, target state, magnitude, dependency edges (blocks / blocked-by), and closure approach, then assembles the dependency graph and a WSJF-ordered remediation sequence with risk assessment — converting the target architecture into sequenced, estimated, dependency-aware work units.

---

## Evidence Collected

| Evidence Type | Source | Reference |
|---------------|--------|-----------|
| Current-state R-disposition, drivers, maturity, roadmap | Step 2.1 assessment | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/assessment.md` |
| Target architecture, ADR-001..008, tech stack, contract mappings | Step 2.2 target | `.../modernization/target-state.md` |
| ISO 25010 ratings, ARCH-001..021, WSJF priority table, cross-cutting levers | Phase 1 ilities | `.../architecture/ilities-assessment.md` |
| DEP-001..010, health rating, concentration risk, license summary | Phase 1 dependencies | `.../discovery/dependencies.md` |
| CD/AD/TD/DD/IF debt items, heatmap, remediation budget | Phase 1 debt | `.../tech-debt/debt-inventory.md` |
| SDK 29 / minSdk 14 / `minifyEnabled false` / `warningsAsErrors` / `-Werror` | this session — file read | `app/build.gradle:10,14,15,33,41,75` |
| AGP 4.1.3 / jcenter ×2 + bintray + scijava mirror + jitpack | this session — file read | `build.gradle:8,4,21,25,27,24` |
| Otto usage = **12 files** (corrects debt-inventory "11"; confirms assessment "12") | this session — grep | `grep "import com.squareup.otto" app/src/main/java` → 12 files |
| firebase-jobdispatcher usage = **4 files** | this session — grep | `grep "import com.firebase.jobdispatcher"` → AlarmManagerDriver, BackupJobs, SmsJobService, SmsBackupService |
| `extends AsyncTask` = **3 files** | this session — grep | BackupTask, RestoreTask, tasks/OAuth2CallbackTask |
| No CI: `.github/workflows/` absent | this session — glob | `Glob .github/workflows/*` → No files found |
| Production deps = 7; test = 4 + 1 AP | this session — file read | `app/build.gradle:56-70` |

> **Evidence reconciliation (truth-and-accuracy):** Two source inconsistencies were resolved against live grep this session. (1) Otto file count: debt-inventory says "11", assessment says "12"; grep confirms **12** — gap counts below use 12. (2) Production LOC: debt-inventory header says "~9,162", ilities §0 says "10,635"; this gap analysis cites the ilities measured value (10,635) and flags the debt-inventory header as a transcription discrepancy that does not affect any gap magnitude. (3) `MainActivity` LOC: ilities measured 499, debt-inventory says 452 — the discrepancy is immaterial to the God-Activity gap classification but is noted so the implementer re-measures at remediation time.

---

## Executive Summary

**Total Gaps Identified:** 18

| Category | Critical | High | Medium | Low | Total |
|----------|:--------:|:----:|:------:|:---:|:-----:|
| Structural | 0 | 2 | 2 | 1 | 5 |
| Technology | 2 | 3 | 1 | 1 | 7 |
| Capability | 0 | 1 | 2 | 0 | 3 |
| Process | 0 | 2 | 1 | 0 | 3 |
| **Total** | **2** | **8** | **6** | **2** | **18** |

**Key Findings:**
1. **There is exactly one hard gate, and it is a Technology gap.** `TECH-001` (targetSdk 29 → 35) is the single blocked-by-nothing / blocks-almost-everything node. Until it closes, **no other gap closure can ship to the Play Store** — it is a gate, not a feature (ilities Cross-Cutting #4). The critical path runs through it.
2. **The structural gaps are small and *additive*, not *demolitive*.** The dominant structural delta (`STRUCT-001`) is *introducing four Hexagonal ports* (`BackupScheduler`, `MailTransport`, `SecretStore`, `Contacts/CalendarPort`) — the target retains the layering, the immutable state machine, the `DataType` type-object, and the exception hierarchy verbatim (target-state §"Architecture Style"; ADR-004). This is why the engagement is uplift-not-rewrite.
3. **Two gaps are unblocked by the gate and are Critical-or-High on the security axis** — `TECH-004` (trust-all TLS removal, CWE-295) and the migration-safety half of `CAP-003`. These run in **parallel** with the gate (target-state §Migration Path: Phase 1 parallel to Phase 0). Failing to start them early leaves an *active* MITM exposure that the SDK upgrade does nothing to address.
4. **The process gaps are the silent risk multiplier.** `PROC-001` (no CI) and `PROC-002` (unmeasured coverage, 0.33:1 LOC) mean every technology/structural gap closure is a refactor against an unmeasured codebase with no regression gate. The target makes the JaCoCo coverage gate a *fitness function* (target-state §Technology Principles #4) — it must land before, not after, the substrate swaps.

**Critical Path (gaps that block other work and must be addressed first):**
1. `TECH-001`: targetSdk/compileSdk 29 → 35 (+ AGP/Gradle bump) — the gate; blocks TECH-002, TECH-003, TECH-006, STRUCT-001, STRUCT-002, CAP-001, CAP-002.
2. `PROC-002`: stand up JaCoCo coverage fitness function — the safety net; must precede every refactor gap (STRUCT-001/002/003, TECH-002/003, CAP-001/002).
3. `PROC-001`: stand up CI (GitHub Actions) — hosts the safety net; blocks PROC-002 enforcement.
4. `TECH-004`: remove trust-all TLS + fix silent migration — Critical security; runs **parallel** to the gate (no SDK dependency).

---

## Gap Heat Map

```
                    │   Structural   │   Technology   │   Capability   │    Process    │
────────────────────┼────────────────┼────────────────┼────────────────┼───────────────┤
Critical Impact     │                │ TECH-001       │                │               │
                    │                │ TECH-004       │                │               │
────────────────────┼────────────────┼────────────────┼────────────────┼───────────────┤
High Impact         │ STRUCT-001     │ TECH-002       │ CAP-001        │ PROC-001      │
                    │ STRUCT-002     │ TECH-003       │                │ PROC-002      │
                    │                │ TECH-005       │                │               │
────────────────────┼────────────────┼────────────────┼────────────────┼───────────────┤
Medium Impact       │ STRUCT-003     │ TECH-006       │ CAP-002        │ PROC-003      │
                    │ STRUCT-004     │                │ CAP-003        │               │
────────────────────┼────────────────┼────────────────┼────────────────┼───────────────┤
Low Impact          │ STRUCT-005     │ TECH-007       │                │               │
────────────────────┴────────────────┴────────────────┴────────────────┴───────────────┘
```

---

## Findings

### Finding Summary

| Severity | Count |
|----------|-------|
| Critical | 2 |
| High | 8 |
| Medium | 6 |
| Low | 2 |
| Informational | 0 |
| **Total** | **18** |

> Findings are **gap findings**: each measures the delta from current (Step 2.1) to target (Step 2.2) and cites the constituent Phase 1 finding(s). Grouped by category: Structural, Technology, Capability, Process. Within the response, ordered Critical → High → Medium → Low to satisfy schema §6.1.

---

## Structural Gaps

#### ARCH-001: No Hexagonal ports at dependency boundaries — engine couples directly to vendor SDKs

- **Severity:** High
- **Confidence:** 0.92
- **Category:** architecture/dependency-coupling (Structural)
- **Description:**
  **Current:** The engine references dependency types directly. `BackupImapStore extends ImapStore` couples to k-9 protected fields; the domain `State` string-matches a k-9 message (`"Unable to get IMAP prefix"`); scheduling is a partially-abstracted `Driver` Strategy but `BackupJobs` still references `com.firebase.jobdispatcher` types directly (6 import occurrences). There is no `MailTransport`, `SecretStore`, or `BackupScheduler` port; only 7 interfaces exist module-wide.
  **Target (ADR-004, target-state §Component Architecture / Integration):** Four app-owned ports — `BackupScheduler`, `MailTransport` (with an Anti-Corruption Layer translating `com.fsck.k9.mail.MessagingException` → app-owned `LocalizableException` so no `com.fsck.k9.*` type crosses the boundary), `SecretStore`, `Contacts/CalendarPort` — so each dead/risky dependency sits behind an interface and every swap is mechanical, reversible, and independently testable.
  **Gap:** Introduce the four ports + adapters and route the engine through them. This is **additive structure**, not redesign — the existing `Driver` Strategy proves the team already understands the indirection (it is the ready-made seam for `BackupScheduler`).
- **Location:** `mail/BackupImapStore.java:60-62`; `service/state/State.java:32` (k-9 magic-string); `service/BackupJobs.java` (jobdispatcher coupling)
- **Evidence:** Phase 1 patterns ARCH-003 (leaky abstraction / magic-string), ilities ARCH-006 (only 7 interfaces); target-state ADR-004 and Integration Architecture diagram; this session — grep confirms jobdispatcher imports in 4 files including `BackupJobs`. The ACL contract is specified line-for-line in target-state §"Integration Pattern — Port & Adapter with Anti-Corruption".
- **Impact:** Without the ports, every substrate swap (TECH-002 scheduler, TECH-003 eventing, TECH-005 secrets, TECH-006 k-9) is a direct edit against vendor types rather than a binding flip — higher blast radius, harder to characterization-test, and not reversible. The ports are the structural precondition that makes the technology gaps cheap.
- **Remediation:** Land ports incrementally as Feathers "Introduce Instance Delegator" wraps (no behavior change), each gated by a characterization test (PROC-002): (1) `BackupScheduler` with the existing `BackupJobs` as first adapter; (2) `MailTransport` + ACL at the k-9 boundary; (3) `SecretStore`; (4) `Contacts/CalendarPort`. Sequence after TECH-001 (SDK gate) and PROC-002 (test net).
- **Effort:** L
- **Related:** TECH-002, TECH-003, TECH-005, TECH-006, PROC-002, STRUCT-002

#### ARCH-002: Eventing on a process-global Otto Singleton — no reactive state spine

- **Severity:** High
- **Confidence:** 0.93
- **Category:** architecture/global-coupling (Structural)
- **Description:**
  **Current:** `App.bus` is a static Otto `Bus` singleton reached from **12 files** across all layers (Service-Locator + Singleton anti-pattern); `private static` service back-references create hidden bidirectional coupling and a Context-leak hazard; `register()` swallows `IllegalArgumentException`; `@Produce` provides sticky last-state.
  **Target (ADR-003, target-state §Engine/Communication Patterns):** An app-owned `SyncStateRepository` exposing `StateFlow<SyncState>` (sticky — reproduces `@Produce` via `.value`) + `SharedFlow<SyncEvent>` (one-shot), DI-injected and collected lifecycle-aware via `repeatOnLifecycle`. The static `isServiceWorking()` query becomes a `WorkInfo`/repository read.
  **Gap:** Replace the global bus with the reactive repository behind an `EventBus` facade, across all 12 files, preserving sticky semantics.
- **Location:** `App.java:59,116-134`; `service/SmsBackupService.java:66`; `service/SmsRestoreService.java:40`; + 9 further Otto-importing files
- **Evidence:** this session — grep `import com.squareup.otto` = **12 files** (App ×2, Dialogs, MainActivity, StatusPreference, OAuth2WebAuthActivity, AdvancedSettings, MainSettings, BackupTask, RestoreTask, SmsJobService, SmsBackupService ×2, SmsRestoreService ×2). Phase 1 ilities ARCH-001/015, debt AD-001/AD-003, dependencies DEP-003; target-state ADR-003.
- **Impact:** Invisible untyped coupling inflates change radius and blocks unit-testing without static rigging; the archived dependency (2014, no patches) is a portability liability. The `@Produce` sticky semantics must be preserved or the status UI regresses.
- **Remediation:** Two-step branch-by-abstraction (Feathers Ch. 25): (1) wrap `App.bus` behind injected `EventBus`/`SyncStateRepository` (mechanical, reversible, no behavior change); (2) swap implementation to `StateFlow`/`SharedFlow`; fold static `isService*()` into the repository. Sequence **after** TECH-002 (WorkManager) to avoid double-churning the engine (target-state §Sequencing rationale).
- **Effort:** M
- **Related:** TECH-003, STRUCT-001, STRUCT-004, PROC-002

#### ARCH-003: Service-as-helper lifecycle bypass — `SmsJobService` manually instantiates `SmsBackupService`

- **Severity:** Medium
- **Confidence:** 0.95
- **Category:** architecture/lifecycle (Structural)
- **Description:**
  **Current:** `SmsJobService` (a `firebase.jobdispatcher.JobService`) `new`s `SmsBackupService`, calls `attachBaseContext(this)`, and invokes `handleIntent(...)` directly to dodge the API-26 background-start restriction. `onCreate()` never runs, so `AppLog` init and `App.register` are silently skipped.
  **Target (ADR-001/ADR-002, target-state §Scheduling Contract Mapping):** `BackupWorker : CoroutineWorker` owns the entire lifecycle via WorkManager `setForeground`; the `<service>` + `ACTION_EXECUTE` intent-filter are removed from the manifest. No manual service instantiation.
  **Gap:** Eliminate the bypass by collapsing the scheduling entry point into the WorkManager worker; remove the manifest `<service>` element and filter.
- **Location:** `service/SmsJobService.java:82-84`; `AndroidManifest.xml` (`SmsJobService` `ACTION_EXECUTE` filter)
- **Evidence:** Phase 1 debt AD-002, assessment ARCH-001; target-state §Scheduling Contract Mapping final row (`remove the <service> + intent-filter`).
- **Impact:** The bypass is a reliability hazard (skipped initialization) and is structurally entangled with the JobDispatcher dependency — it cannot be fixed independently of the scheduler migration.
- **Remediation:** Fold into TECH-002 (WorkManager migration) as one unit; the worker's lifecycle replaces the manual instantiation. Do not attempt a standalone fix — it would re-implement the very lifecycle WorkManager provides.
- **Effort:** M (subsumed by TECH-002)
- **Related:** TECH-002, ARCH-001

#### ARCH-004: God-Activity tendency — `MainActivity` carries multiple responsibilities, no ViewModel

- **Severity:** Medium
- **Confidence:** 0.80
- **Category:** architecture/srp (Structural)
- **Description:**
  **Current:** `MainActivity` (499 LOC measured; highest branch-token proxy = 56) orchestrates permission flows, dialog management, Otto subscription, and backup/restore triggering — SRP pressure, top complexity hotspot. No `ViewModel`; UI state lives in the Activity.
  **Target (target-state §Presentation Layer):** `MainActivity` + `MainViewModel`; state moves to the `ViewModel`, dialogs to `Dialogs`, status collected from `SyncStateRepository.StateFlow` via `repeatOnLifecycle`. UI never calls down into the engine directly (preserve the current "0 direct downward calls" discipline).
  **Gap:** Extract a `ViewModel` + permissions coordinator; migrate Otto subscription to Flow collection.
- **Location:** `activity/MainActivity.java` (499 LOC, branch proxy 56)
- **Evidence:** Phase 1 ilities ARCH-004, debt TD-001 (no `MainActivityTest`), complexity hotspot table; target-state §Presentation Layer.
- **Impact:** Constrains modifiability and is largely untested (TD-001) — refactoring it without tests is high-risk, which couples this gap to PROC-002.
- **Remediation:** Pairs naturally with ARCH-002 (Otto→Flow): extract `MainViewModel`, move dialog logic to the existing `Dialogs.java`, backfill Robolectric tests for permission/result branches first (PROC-002). Sequence with ARCH-002 in Phase 2.
- **Effort:** M
- **Related:** ARCH-002, PROC-002, TECH-005 (Hilt removes the construction smell feeding this)

#### ARCH-005: Dead compatibility shim + stubbed state persistence — structural cruft

- **Severity:** Low
- **Confidence:** 0.85
- **Category:** architecture/dead-code (Structural)
- **Description:**
  **Current:** `CalendarAccessorPre40` targets API < 14, unreachable at minSdk 14 (dead code + dead test surface, reached via a `CalendarAccessor.Get.instance()` factory branch); `StatusPreference.onSaveInstanceState`/`onRestoreInstanceState` are stubbed `// TODO`, causing a state flash on rotation.
  **Target (target-state §Data/Integration, ADR-006):** `minSdk` raised to 21 collapses the `compat`/`Pre40` surface; `CalendarProviderAdapter` is the sole calendar implementation; instance-state implemented.
  **Gap:** Decommission `CalendarAccessorPre40` + factory branch; implement `StatusPreference` save/restore; sweep additional `compat` shims made dead by the minSdk raise.
- **Location:** `calendar/CalendarAccessorPre40.java`; `calendar/CalendarAccessor.java` (factory); `activity/StatusPreference.java:131-139`
- **Evidence:** Phase 1 debt AD-005/CD-008, assessment DEBT-003.
- **Impact:** Maintenance/test-surface drag and a minor UX defect; lowest-priority structural item.
- **Remediation:** Opportunistic — fold the `Pre40` removal into the TECH-001 minSdk-raise sweep (it becomes provably dead at minSdk 21); implement `StatusPreference` instance-state as an isolated S task.
- **Effort:** S
- **Related:** TECH-001

---

## Technology Gaps

#### DEP-001: targetSdk/compileSdk 29 → 35 — the unconditional Play-distribution gate

- **Severity:** Critical
- **Confidence:** 1.00
- **Category:** technology/platform-currency (Technology)
- **Description:**
  **Current:** `compileSdkVersion 29`, `targetSdkVersion 29`, `minSdkVersion 14`, `buildToolsVersion 29.0.2`, AGP 4.1.3, Gradle 7.2.
  **Target (ADR-006, target-state §Technology Stack):** compile/target SDK **35**, minSdk **21**, AGP 8.x, Gradle 8.x, build-tools 34.x; staged AGP path 4.1.3 → 7.4.x → 8.x with matching wrapper bumps.
  **Gap:** Raise four major AGP versions and six SDK levels, then conform to the per-API behavioral breakages introduced across API 30–34: background-execution limits, exact-alarm policy, `FOREGROUND_SERVICE_DATA_SYNC` type, `POST_NOTIFICATIONS` runtime permission, explicit `android:exported` on the four `<receiver>` elements, and `PendingIntent.FLAG_IMMUTABLE` (see CAP-002).
- **Location:** `app/build.gradle:10,11,14,15`; `build.gradle:8`; `gradle/wrapper/gradle-wrapper.properties`
- **Evidence:** this session — file read confirms compile/target 29, minSdk 14, buildTools 29.0.2, AGP 4.1.3. Phase 1 dependencies DEP-005/006/007, ilities ARCH-019, debt DD(targetSdk); assessment DEBT-001; target-state ADR-006.
- **Code Snippet:**
  ```groovy
  // app/build.gradle
  compileSdkVersion 29
  buildToolsVersion '29.0.2'
  defaultConfig { minSdkVersion 14 ; targetSdkVersion 29 ; ... }
  ```
- **Exploit Scenario:** (architectural impact narrative) The maintainer fixes a field-reported backup bug and attempts to publish the update. Google Play rejects the AAB because `targetSdk 29` is below the API-34 update floor enforced since Aug 2024. The fix never reaches users through the primary channel; the app is effectively frozen on the Play Store. Every other gap closure in this analysis inherits the same block — none can ship until this gate clears.
- **Impact:** Distribution-blocking (the dominant business driver). Also gates the CAP-002 `FLAG_IMMUTABLE` crash-fix and the TECH-005 `EncryptedSharedPreferences` Keystore ergonomics. Gate, not feature (ilities Cross-Cutting #4).
- **Remediation:** Execute first as a self-contained Phase 0: incremental AGP 4.1.3→7.4→8.x + Gradle 8.x; raise compile/target to 35, minSdk to 21; audit/declare `android:exported` on the four receivers; add `POST_NOTIFICATIONS` + `FOREGROUND_SERVICE_DATA_SYNC`. Behavioral conformance gated by the PROC-002 fitness function. Stage so `-Werror` (PROC-003) does not block mid-migration.
- **Effort:** L
- **Related:** TECH-002, TECH-003, TECH-005, TECH-006, CAP-001, CAP-002, STRUCT-001, PROC-003

#### SEC-001: Trust-all TLS path + silent security-downgrade migration

- **Severity:** Critical
- **Confidence:** 0.97
- **Category:** security/transport (Technology) — CWE-295, CWE-312
- **Description:**
  **Current:** `AllTrustedSocketFactory.InsecureX509TrustManager.checkServerTrusted()` is empty (accepts any cert); selected by `BackupImapStore` when `isTrustAllCertificates()` is on; `AuthPreferences.migrate()` silently sets `SERVER_TRUST_ALL_CERTIFICATES = true` for legacy `+ssl`/`+tls` users on upgrade.
  **Target (ADR-007, target-state §Security Architecture):** validated TLS only; `AllTrustedSocketFactory` removed; self-hosted IMAP supported via explicit user-pinned cert; `migrate()` preserves validated TLS with a one-time user notice.
  **Gap:** Remove the trust-all code path, replace with user-pinned-cert opt-in, and rewrite the migration to never downgrade transport security without consent.
- **Location:** `mail/AllTrustedSocketFactory.java:42-57`; `preferences/AuthPreferences.java:276-288`; selection at `mail/BackupImapStore.java:60-62`
- **Evidence:** Phase 1 ilities ARCH-007 (Critical)/ARCH-008 (High), debt CD-003; assessment ARCH-003; target-state ADR-007 + Security QAS. (Inherited from Phase 1 — not re-read at source this session; confidence reflects that.)
- **Code Snippet:**
  ```java
  public void checkServerTrusted(X509Certificate[] chain, String authType) {} // empty — trusts all
  public X509Certificate[] getAcceptedIssuers() { return null; }
  ```
- **Data-Flow Trace:** `IMAP TLS handshake -> BackupImapStore [trustAllCertificates ? AllTrustedSocketFactory : Default] -> InsecureX509TrustManager.checkServerTrusted() [no-op sink]`
- **Exploit Scenario:** A legacy `+ssl` user is silently moved to trust-all by `migrate()` on upgrade. On hostile Wi-Fi, an on-path attacker presents a self-signed cert for `imap.gmail.com`; validation is a no-op; the attacker terminates TLS and reads/modifies the entire SMS/MMS/call-log backup stream and captures the Gmail app-password in transit.
- **Impact:** Confidentiality + integrity loss on the most sensitive data a phone holds; the upgrade path actively worsens posture without consent. This is the highest-priority gap on the security axis and is **independent of the SDK gate**.
- **Remediation:** Remove `AllTrustedSocketFactory`; gate self-hosted relaxed validation behind an explicit user-pinned certificate; `migrate()` must preserve validated TLS + show a one-time notice. Characterization-test `migrate()`/`getStoreUri()` first (PROC-002 rec). Run **parallel to Phase 0** — no SDK dependency.
- **Effort:** M
- **CWE:** [CWE-295, CWE-312]
- **References:** ["https://cwe.mitre.org/data/definitions/295.html", "OWASP MASVS-NETWORK-1"]
- **Related:** TECH-005, CAP-003, PROC-002

#### DEP-002: Firebase JobDispatcher + AsyncTask + AlarmManager fallback → WorkManager

- **Severity:** High
- **Confidence:** 0.95
- **Category:** technology/scheduling-concurrency (Technology)
- **Description:**
  **Current:** Background scheduling on `firebase-jobdispatcher:0.8.6` (deprecated 2018, served only from the `maven.scijava.org` mirror), referenced in **4 files**, behind a dual `Driver` (`GooglePlayDriver`/`AlarmManagerDriver`) selected by `isUseOldScheduler`; workers extend deprecated `AsyncTask` (**3 files**, removed API 33, each carrying `@SuppressLint("StaticFieldLeak")`).
  **Target (ADR-001/002, target-state §Scheduling Contract Mapping):** single `WorkManagerScheduler` adapter behind `BackupScheduler`; `BackupWorker`/`RestoreWorker : CoroutineWorker` with `setForeground`; dual-driver path, `AlarmManagerDriver`, JobDispatcher, the scijava mirror, and the service-as-helper hack all retired. Contract preserved line-for-line: `REPLACE` enqueue semantics, `BackoffPolicy.EXPONENTIAL 30s` base, `UNMETERED|CONNECTED` constraints, content-URI trigger (API 24+) with `SmsBroadcastReceiver` fallback below 24.
  **Gap:** Migrate the entire scheduling + worker substrate to WorkManager, retiring two abandoned deps and the mirror in one coherent move.
- **Location:** `service/BackupJobs.java`, `service/SmsJobService.java`, `service/AlarmManagerDriver.java` (jobdispatcher); `service/BackupTask.java:50`, `service/RestoreTask.java`, `tasks/OAuth2CallbackTask.java` (AsyncTask)
- **Evidence:** this session — grep: jobdispatcher imports in 4 files, `extends AsyncTask` in 3 files; `build.gradle:27` scijava mirror; `app/build.gradle:60` jobdispatcher dep. Phase 1 dependencies DEP-002 (Critical), ilities ARCH-003/014/018, debt CD-001/AD-002/DD; assessment ARCH-001; target-state ADR-001/002.
- **Code Snippet:**
  ```java
  class BackupTask extends AsyncTask<BackupConfig, BackupState, BackupState> { // deprecated API 30, removed 33
  ```
- **Exploit Scenario:** (supply-chain impact narrative) `maven.scijava.org` (a scientific-computing repo, not a Google endpoint) suffers an outage or evicts the artifact. The next clean CI build cannot resolve `firebase-jobdispatcher:0.8.6` — there is no fallback host. All builds break with no upstream remediation path, on a privacy-critical app holding the user's entire message corpus.
- **Impact:** Removes the Critical supply-chain mirror dependency, two abandoned deps, the serial-executor contention, the `StaticFieldLeak`, and the service-as-helper bypass (ARCH-003) in one migration; unblocks the durable restore checkpoint (CAP-001). Highest-leverage single migration (ilities Cross-Cutting #1).
- **Remediation:** Branch-by-abstraction off the existing `Driver` Strategy: `BackupScheduler` port → `BackupJobs` first adapter (no behavior change) → `WorkManagerScheduler` + `CoroutineWorker`. Characterization-test retry `30/300` + constraints first (Feathers Ch. 2 / PROC-002). Sequence after TECH-001 (SDK) and STRUCT-001/PROC-002. **Rejected alternative:** bare `JobScheduler` — does not subsume the AlarmManager fallback (target-state ADR-001).
- **Effort:** L
- **References:** ["https://cwe.mitre.org/data/definitions/1357.html"]
- **Related:** TECH-001, STRUCT-001, STRUCT-003, CAP-001, PROC-002

#### DEP-003: Square Otto 1.3.8 (archived) → Kotlin StateFlow/SharedFlow

- **Severity:** High
- **Confidence:** 0.95
- **Category:** technology/dependency-currency (Technology)
- **Description:**
  **Current:** `com.squareup:otto:1.3.8` (archived 2014/2016, no patches ever), reflection-based, no compile-time subscriber check, used in **12 files**.
  **Target (ADR-003):** Kotlin `StateFlow`/`SharedFlow` behind `SyncStateRepository` — the dependency is *removed*, not replaced.
  **Gap:** Eliminate the Otto dependency entirely by migrating eventing to coroutines Flow (the structural mechanism is STRUCT-002; this technology gap is the *dependency retirement* it enables).
- **Location:** `app/build.gradle:57`; 12 importing source files (grep this session)
- **Evidence:** this session — grep = 12 files; `app/build.gradle:57`. Phase 1 dependencies DEP-003, ilities ARCH-001, debt AD-001; target-state ADR-003. **Rejected alternative (explicit):** Greenrobot EventBus (dependencies.md DEP-003 suggested it as near-drop-in) — perpetuates the global-bus/Service-Locator anti-pattern and *adds* rather than *removes* a dependency (target-state ADR-003).
- **Impact:** Removes an archived, unpatchable reflection dependency; enables compile-time-typed events; the change is broad (12 files) but mechanical.
- **Remediation:** Realized by STRUCT-002 (facade then Flow swap). The dependency line is deleted only after the last file migrates. Sequence after TECH-002.
- **Effort:** M
- **Related:** STRUCT-002, TECH-002

#### DEP-004: Outdated test toolchain blocks the SDK bump; Play Billing 5 majors behind

- **Severity:** High
- **Confidence:** 0.95
- **Category:** technology/dependency-currency (Technology)
- **Description:**
  **Current:** `robolectric:4.3.1` (caps SDK ≤ 29 — a **hard co-dependency** of the SDK raise), `junit:4.12` (CVE-2020-15250), `mockito-all:1.10.17` (2015 uber-jar, blocks Mockito 5), `truth:0.39`, `auto-service:1.0-rc4`; `billing:2.1.0` (`SkuDetails` deprecated; Play enforces ≥ 6.x).
  **Target (target-state §Supporting Technologies):** Robolectric 4.12.x+, JUnit 4.13.2, mockito-core 5.x, Truth 1.4.x; Play Billing 7.x (`ProductDetails`/`queryProductDetailsAsync`).
  **Gap:** Upgrade the test stack *with* the SDK bump (Robolectric blocks it otherwise); upgrade billing to 7.x and rewrite the donation flow (isolated to 3 files).
- **Location:** `app/build.gradle:65-70` (test), `:59` (billing)
- **Evidence:** this session — file read confirms all versions. Phase 1 dependencies DEP-008/DEP-010, debt TD-004/DD; assessment DEP-003; target-state §Supporting Technologies.
- **Impact:** Robolectric 4.3.1 is a hard blocker for TECH-001 (cannot run tests against SDK > 29); billing 2.x is a forward Play-policy risk on the donation subsystem. junit 4.12 carries a known CVE even in test scope.
- **Remediation:** Bundle the test-dep upgrade into the TECH-001 Phase-0 build sweep (Robolectric must move with the SDK). Billing 7.x is an isolated Phase-3 task (3 donation files) — defer behind the security/scheduler spine.
- **Effort:** M
- **Related:** TECH-001, PROC-002

#### DEP-005: k-9 mail pinned to a JitPack git SHA — unreproducible, unpatched

- **Severity:** Medium
- **Confidence:** 0.95
- **Category:** technology/supply-chain (Technology)
- **Description:**
  **Current:** `com.github.jberkel.k-9:k9mail-library:eaf689025e` — a raw commit SHA via JitPack, no semantic version, transitively unpatched against upstream k-9/Thunderbird IMAP security fixes; supplies the entire IMAP layer (15+ source files — concentration risk).
  **Target (ADR-004):** k-9 pinned to a maintained versioned coordinate or vendored as a module, **behind the `MailTransport` ACL** so the domain is insulated and either choice is swappable.
  **Gap:** Replace the SHA pin with a reproducible coordinate (or vendored subset) once the ACL (STRUCT-001) contains the boundary.
- **Location:** `app/build.gradle:58`
- **Evidence:** this session — file read. Phase 1 dependencies DEP-004, debt DD; assessment DEP-001; target-state ADR-004 + Risk table (vendoring fallback).
- **Impact:** Build-reproducibility single point of failure (JitPack cache eviction → CI break) and an unpatched-IMAP security exposure. Medium because the ACL (STRUCT-001) must exist first to make the swap safe; until then the risk is contained but not closed.
- **Remediation:** Depends on STRUCT-001 (MailTransport ACL). Then either pin to a maintained release if API-compatible, or vendor the IMAP subset to guarantee reproducibility. Sequence in Phase 3 behind the ACL. Add Gradle dependency verification (`verification-metadata.xml`) in Phase 0 (PROC-001) to detect tampering meanwhile.
- **Effort:** L
- **Related:** STRUCT-001, PROC-001

#### DEP-006: jcenter declared after 2022 shutdown — build non-determinism

- **Severity:** Low
- **Confidence:** 1.00
- **Category:** technology/build-config (Technology)
- **Description:**
  **Current:** `jcenter()` declared in `buildscript` (line 4) and `allprojects` (line 21), plus `https://jcenter.bintray.com` (line 25), listed before `mavenCentral()` — every resolution attempts a sunset service first.
  **Target (target-state §Supply-chain):** repositories reduced to `google()` / `mavenCentral()` / `jitpack.io` (+ pinned-verified); jcenter and the bintray mirror removed; the scijava mirror removed once TECH-002 retires jobdispatcher.
  **Gap:** Delete three repository declarations and verify resolution.
- **Location:** `build.gradle:4,21,25`
- **Evidence:** this session — file read confirms jcenter at lines 4, 21 and bintray at 25. Phase 1 dependencies DEP-001, debt IF-001; assessment DEP-002.
- **Impact:** Latent silent build failure / non-deterministic resolution; lowest-effort technology gap (~30 min).
- **Remediation:** Remove all three declarations in the Phase 0.1 build sweep; verify against mavenCentral/google/jitpack. Trivial.
- **Effort:** S
- **Related:** TECH-001, PROC-001

---

## Capability Gaps

#### ARCH-006: No durable restore checkpoint; coarse failure classification; O(mailbox) restore scan

- **Severity:** High
- **Confidence:** 0.80
- **Category:** architecture/reliability-performance (Capability)
- **Description:**
  **Current:** Restore has no persisted cursor — a process kill mid-restore can lose or duplicate messages (no idempotency key on SMS-provider writes). `catch(MessagingException)→ERROR` collapses transient vs. permanent failures (no classified retry, no circuit breaker). `UID SEARCH 1:*` + in-memory whole-folder sort is O(mailbox), applying the page cap only after materializing/sorting everything.
  **Target (ADR-005, target-state §Reliability/Performance QAS):** durable restore checkpoint in WorkManager work-data + idempotent writes keyed on message identity (RPO ≈ 0 dup/loss); transient/permanent classification with bounded retry-with-jitter + optional IMAP Circuit Breaker; server-side bounding/sorting (IMAP `SORT`/`SINCE`/UID windowing) so peak memory is bounded by page size, not mailbox size.
  **Gap:** Add the durable checkpoint + idempotency, the failure classifier, and the server-side search bounding.
- **Location:** `service/RestoreTask.java`; `mail/BackupImapStore.java:148-186,189`; `service/BackupTask.java:168-175`
- **Evidence:** Phase 1 ilities ARCH-013/014/016; assessment ARCH-005; target-state ADR-005 + two QAS. (Inherited from Phase 1.)
- **Impact:** Data-integrity risk on interrupted restore (the user-facing correctness gap) and the only true scaling cliff for power users with large mailboxes.
- **Remediation:** Durable checkpoint follows TECH-002 (WorkManager provides the durable work state directly); failure classification + Circuit Breaker (Nygard Ch. 5) follows TECH-002; server-side search bounding is **independent/parallel** (no scheduler dependency). Requires a stable message-identity key (IMAP UID / headers).
- **Effort:** L
- **Related:** TECH-002, STRUCT-001, PROC-002

#### QUAL-001: PendingIntent without FLAG_IMMUTABLE — latent crash that activates on the SDK bump

- **Severity:** Medium
- **Confidence:** 1.00
- **Category:** quality/runtime-correctness (Capability)
- **Description:**
  **Current:** Two `PendingIntent` constructions use `FLAG_UPDATE_CURRENT` without `FLAG_IMMUTABLE`/`FLAG_MUTABLE` — latent at targetSdk 29 but throws `IllegalArgumentException` on API 31+.
  **Target:** both call-sites add `FLAG_IMMUTABLE`.
  **Gap:** Two-line fix that **must land in the same change as TECH-001** — the gate converts this from latent to a hard crash on the scheduling path.
- **Location:** `service/AlarmManagerDriver.java:127`; `service/ServiceBase.java:201`
- **Evidence:** Phase 1 debt CD-006; assessment DEBT-002; target-state §Key Transformations (FLAG_IMMUTABLE). (Inherited from Phase 1.)
- **Impact:** Hard crash on the scheduling path on modern Android the moment the gate clears — a regression introduced *by* the gate if not co-landed.
- **Remediation:** Add `FLAG_IMMUTABLE` to both call-sites as part of the Phase 0 gate sweep (co-land with TECH-001). Note: `AlarmManagerDriver` is deleted by TECH-002, but the fix must exist between the gate and the scheduler migration so the AlarmManager path does not crash in the interim.
- **Effort:** S
- **Related:** TECH-001, TECH-002

#### SEC-002: Plaintext credential storage → EncryptedSharedPreferences

- **Severity:** Medium
- **Confidence:** 0.90
- **Category:** security/secrets-at-rest (Capability) — CWE-312
- **Description:**
  **Current:** Gmail app-password + OAuth2 refresh token in plaintext `SharedPreferences("credentials")`. Good defensive segregation (separate `credentials.xml`, backup-excluded) but no at-rest encryption against ADB-backup/rooted/forensic extraction.
  **Target (ADR-007):** `SecretStore` backed by `EncryptedSharedPreferences` (Jetpack Security, AES-256, Keystore master key); single accessor seam; preserve the `credentials.xml` backup exclusion.
  **Gap:** Migrate the single `getCredentials()` seam to encrypted storage; one-time migration preserving existing credentials where possible.
- **Location:** `preferences/AuthPreferences.java:221-226`
- **Evidence:** Phase 1 ilities ARCH-009, debt heatmap; assessment ARCH-004; target-state ADR-007 + §Data Protection. (Inherited from Phase 1.)
- **Impact:** Credential exposure at rest on compromised/rooted devices. Medium because the existing segregation + backup-exclusion materially reduce blast radius — this hardens, it does not close an active exposure (unlike SEC-001).
- **Remediation:** Migrate `getCredentials()` to `EncryptedSharedPreferences` behind the `SecretStore` port (STRUCT-001). Eased by TECH-001 (Keystore ergonomics ≥ API 23, minSdk 21 target). Characterization-test the credential round-trip first (PROC-002). Phase 1.
- **Effort:** M
- **CWE:** CWE-312
- **References:** ["https://cwe.mitre.org/data/definitions/312.html"]
- **Related:** TECH-001, TECH-004, STRUCT-001, PROC-002

---

## Process Gaps

#### OPS-001: No CI/CD in the repository — no automated regression gate

- **Severity:** High
- **Confidence:** 0.95
- **Category:** process/ci-cd (Process)
- **Description:**
  **Current:** No `.github/workflows/` directory exists (confirmed this session); only a Travis badge in the README. No reproducible build/test/lint pipeline; release `minifyEnabled false`; no Gradle dependency verification; no automated provenance check against the scijava/JitPack supply-chain risks.
  **Target (target-state §Operational Architecture / Migration Path):** GitHub Actions workflow running `./gradlew test` + `lint` + `jacocoTestReport` + `assembleRelease` (R8 on) on every PR; `gradle/verification-metadata.xml`; Renovate/Dependabot.
  **Gap:** Stand up a GitHub Actions pipeline and dependency verification — the platform on which the safety net (PROC-002) runs.
- **Location:** repo root (no `.github/workflows/`); `app/build.gradle:33` (`minifyEnabled false`)
- **Evidence:** this session — `Glob .github/workflows/*` returns no files; file read confirms `minifyEnabled false`. Phase 1 debt IF-003/IF-004, ilities ARCH-005 / 12-factor Build-Release-Run gap; assessment OPS-001; target-state §Deployment Architecture.
- **Impact:** Every gap closure is a refactor with no automated regression gate; supply-chain risks (TECH-005, the scijava mirror) go undetected; releases are unreproducible. This is the dominant DevOps maturity gap (current level 1 → target 3).
- **Remediation:** Phase 0.6 — GitHub Actions (build + test + lint baseline) + `verification-metadata.xml`. **Parallelizable** with TECH-001 and SEC-001 (no inter-dependency). Blocks PROC-002 enforcement (the coverage gate needs CI to run on).
- **Effort:** M
- **Related:** PROC-002, TECH-001, TECH-005

#### OPS-002: Test coverage unmeasured (0.33:1 LOC) — no fitness function protecting refactors

- **Severity:** High
- **Confidence:** 0.90
- **Category:** process/quality-assurance (Process)
- **Description:**
  **Current:** 36 test files / 35 test classes give good *breadth* (one per major package) but coverage is **unmeasured** — no JaCoCo report; test:prod ratio 0.33:1 (below the ~0.5–1.0 typical of well-covered Android engines). The complexity hotspots (`MainActivity` TD-001, `OAuth2Client` TD-003) have thin or no behavioral tests. `SmsBackupServiceTest:212` is an empty `// TODO` test body.
  **Target (target-state §Technology Principles #4, Supporting Technologies):** JaCoCo `jacocoTestReport` + a coverage **fitness function** (Ford/Parsons/Kua) failing CI below an agreed engine-package threshold (≥ 70% on `service`/`mail`/`auth`); characterization tests as the gate before every seam swap (Feathers Ch. 2/13).
  **Gap:** Wire JaCoCo + the coverage gate, and backfill characterization tests on the highest-risk untested paths *before* refactoring.
- **Location:** module-wide (3,550 test LOC / 10,635 prod LOC); `activity/MainActivity.java` (TD-001, no test); `auth/OAuth2Client.java` (TD-003, no test); `service/SmsBackupServiceTest.java:212` (empty test)
- **Evidence:** Phase 1 ilities ARCH-005, debt TD-001/TD-003 + empty-test marker; assessment OPS-001; target-state §Technology Principles #4. this session — `app/build.gradle` confirms no JaCoCo plugin declared.
- **Impact:** Refactoring an unmeasured codebase is flying blind — the substrate-swap gaps (STRUCT-001/002, TECH-002/003, CAP-001) could regress silently. The 35-class suite is the *characterization safety net* the whole uplift depends on; without measurement, its adequacy is unknown.
- **Remediation:** Phase 0.7 — JaCoCo + coverage fitness function on `service`/`mail`/`auth`; backfill characterization tests for `AuthPreferences.migrate()`/`getStoreUri()` (protects SEC-001/SEC-002), `BackupJobs` retry/constraints (protects TECH-002), `MainActivity` branches (TD-001), `OAuth2Client` (TD-003) before touching them. **Must land before any refactor gap.** Depends on PROC-001 (CI to run on).
- **Effort:** M
- **Related:** PROC-001, STRUCT-001, STRUCT-002, TECH-002, TECH-003, CAP-001, SEC-001, SEC-002

#### OPS-003: `-Werror`/`warningsAsErrors` brittleness + absent observability discipline

- **Severity:** Medium
- **Confidence:** 0.85
- **Category:** process/build-policy-observability (Process)
- **Description:**
  **Current:** `warningsAsErrors true` (lint) + `-Werror -Xlint:deprecation` on non-test JavaCompile means any newly-surfaced deprecation breaks the build mid-migration; the team has accumulated 27–29 `@SuppressWarnings`/`@SuppressLint` sites in response. Logging is unstructured free-text with per-call-site redaction discretion (e.g., one site logs the account email (PII) at debug level while the token is masked via `getTokenForLogging()`, another masks a URI); no metrics, no opt-in crash reporting — field failures are invisible on a backendless app.
  **Target (target-state §Constraints, Observability, Technology Principle #3):** treat `-Werror` as a *deprecation-debt fitness function* with staged removal (each migration removes, never adds, a deprecation; use a lint baseline during transition); centralized redaction policy (not per-author discretion); WorkManager `WorkInfo` observability replacing the static `isServiceWorking()` query; opt-in, ADR-documented crash reporting.
  **Gap:** Convert the brittle build policy into a managed fitness function and centralize redaction + add opt-in observability.
- **Location:** `app/build.gradle:41,75`; `auth/OAuth2Client.java:145` (account email logged at debug; token masked); `utils/AppLog.java` (unstructured)
- **Evidence:** this session — file read confirms `warningsAsErrors true` (line 41) and `-Werror -Xlint:deprecation` (line 75). Phase 1 debt IF-002/CD-007, ilities ARCH-010/021; target-state §Constraints + Observability.
- **Impact:** The build policy will repeatedly block the TECH-001 SDK migration as new deprecations surface (HIGH likelihood per target-state Risk table); inconsistent redaction is a residual info-disclosure risk (couples to SEC-001/SEC-002 root cause — secure-by-default-as-policy, ilities Cross-Cutting #3).
- **Remediation:** During migration, relax `-Werror` to warning-only with a `lint-baseline.xml`; restore `-Werror` once deprecated APIs are eliminated (stage so each step removes one). Centralize a redaction helper (closes ARCH-010); add WorkInfo observability with TECH-002; opt-in crash reporting in Phase 3. Fold the build-policy change into Phase 0 alongside TECH-001.
- **Effort:** M
- **Related:** TECH-001, SEC-001, SEC-002, TECH-002

---

## Gap Dependencies

### Dependency Graph

```
              PROC-001 (CI) ──────────────┐
                  │                       │
                  ▼                       │ (parallel, day-1)
              PROC-002 (coverage gate) ◀──┘
                  │  (must precede all refactors)
                  │
   TECH-001 (SDK gate) ─────────┬─────────────┬───────────────┐
        │ (blocks)              │             │               │
        ▼                       ▼             ▼               ▼
   QUAL-001 (FLAG_IMMUTABLE) TECH-004*    TECH-006/jcenter  TECH-004(billing/test)
        │                    (SEC, ∥)
        ▼
   STRUCT-001 (Hexagonal ports) ◀── enables ──┐
        │                                      │
   ┌────┴───────────┬───────────┬─────────────┤
   ▼                ▼           ▼             ▼
 TECH-002        TECH-005    TECH-005-k9    CAP-002 (encrypted prefs)
(WorkManager)   (Hilt)      (DEP-005)
   │  retires DEP-002+003-mirror, fixes ARCH-003
   ├────────────┬───────────────┐
   ▼            ▼               ▼
 STRUCT-002   CAP-001        ARCH-003 (bypass removed)
 (Otto→Flow)  (checkpoint)
   │  retires DEP-003
   ▼
 STRUCT-004 (MainActivity/ViewModel)

* SEC-001 (trust-all TLS) and the server-side-search half of CAP-001
  are INDEPENDENT of the SDK gate and run parallel from day 1.
```

### Dependency Matrix

| Gap | Depends On (Blocked By) | Blocks |
|-----|-------------------------|--------|
| TECH-001 (SDK gate) | None | TECH-002, TECH-003, TECH-005, TECH-006, CAP-001, CAP-002, STRUCT-001, QUAL-001, PROC-003 |
| SEC-001 (trust-all) | None (parallel) | — (independent Critical) |
| PROC-001 (CI) | None (parallel) | PROC-002 |
| PROC-002 (coverage gate) | PROC-001 | STRUCT-001, STRUCT-002, TECH-002, TECH-003, CAP-001, SEC-001, SEC-002 |
| QUAL-001 (FLAG_IMMUTABLE) | TECH-001 | — (must co-land with gate; interim before TECH-002) |
| DEP-006 (jcenter) | None | — |
| STRUCT-001 (ports) | TECH-001, PROC-002 | TECH-002, TECH-005, TECH-006(k9), CAP-001, SEC-002 |
| TECH-002 (WorkManager) | TECH-001, STRUCT-001, PROC-002 | STRUCT-002, STRUCT-003, CAP-001, PROC-003(WorkInfo) |
| STRUCT-003 (bypass) | TECH-002 | — (subsumed) |
| TECH-003 / STRUCT-002 (Otto→Flow) | TECH-002, PROC-002 | STRUCT-004 |
| STRUCT-004 (MainActivity) | STRUCT-002, PROC-002 | — |
| TECH-005 (Hilt) | TECH-002, STRUCT-002 | — |
| CAP-001 (checkpoint/perf) | TECH-002 (checkpoint); independent (search-bounding) | — |
| SEC-002 / CAP-003 (encrypted prefs) | TECH-001, STRUCT-001, PROC-002 | — |
| TECH-004 (test/billing) | TECH-001 (test); independent (billing) | TECH-002 (Robolectric blocks SDK) |
| TECH-006 / DEP-005 (k-9 unpin) | STRUCT-001 | — |
| ARCH-005 (dead shim) | TECH-001 (minSdk) | — |
| PROC-003 (-Werror/observability) | TECH-001 (folded); TECH-002 (WorkInfo) | — |

> **Cycle note:** TECH-004's Robolectric upgrade is a *co-dependency* of TECH-001 (Robolectric 4.3.1 caps at SDK 29, so it must move *with* the SDK bump, not after) — they land in the same Phase 0 build sweep. This is a co-requisite, not a true cycle.

---

## Prioritization

### Priority Matrix (WSJF — value+risk-reduction+unblocking ÷ effort)

```
                         High Business Value / Risk-Reduction
                                       │
                   TECH-001 ●          │          ● SEC-001
                   (gate, L,           │            (Critical SEC,
                    unblocks all)      │             M, parallel)
                                       │          ● DEP-006 (jcenter, S)
            High ───────────────●──────┼──────●───────────────── Low
            Effort         TECH-002    │   PROC-001/002          Effort
                          STRUCT-001   │   QUAL-001 (S)
                          CAP-001      │
                                       │
                   TECH-006 ●          │          ● ARCH-005 (S, opportunistic)
                   (k-9, L)            │          ● PROC-003
                          Lower Business Value / Risk-Reduction
```

### Recommended Priority Order

| Priority | Gap ID | Gap Name | WSJF Rationale |
|:--------:|--------|----------|----------------|
| 1 | TECH-001 | SDK/AGP/Gradle gate | Highest cost-of-delay: blocks *all* shipping and 8 downstream gaps; nothing reaches users until done. Gate. |
| 2 | SEC-001 | Remove trust-all TLS + fix migration | Critical, *active* MITM exposure that the upgrade worsens; zero SDK dependency → start day-1 parallel. Highest risk-reduction per effort. |
| 3 | PROC-001 | Stand up CI | Cheap, unblocks PROC-002; parallel to gate. |
| 4 | PROC-002 | JaCoCo coverage fitness function | The safety net; must precede every refactor gap. Parallel to gate, after CI. |
| 5 | DEP-006 / QUAL-001 | jcenter removal (S); FLAG_IMMUTABLE (S, co-land with gate) | Quick wins folded into Phase 0; QUAL-001 prevents a gate-induced crash. |
| 6 | STRUCT-001 | Hexagonal ports | Structural precondition that makes 002/005/CAP-001/SEC-002 mechanical. After gate + test net. |
| 7 | TECH-002 | WorkManager migration | Largest blast radius: retires DEP-002, the scijava mirror, AsyncTask, AlarmManager fallback, the lifecycle bypass; unblocks CAP-001. |
| 8 | SEC-002 / CAP-003 | EncryptedSharedPreferences | Single `SecretStore` seam; eased by gate (Keystore). |
| 9 | TECH-003 / STRUCT-002 | Otto → Flow | Follows WorkManager (avoid engine double-churn); removes DEP-003. |
| 10 | TECH-005 | Hilt DI | Settles once construction graph is stable; formalizes the hand-built seams. |
| 11 | CAP-001 | Durable restore checkpoint + classified retry + server-bounded search | Data-integrity + scaling cliff; checkpoint after WorkManager, search-bounding parallel. |
| 12 | STRUCT-004 | MainActivity/ViewModel | With Otto→Flow. |
| 13 | TECH-004(billing) / TECH-006 / DEP-005 / ARCH-005 / PROC-003 | Billing 7.x; k-9 unpin behind ACL; dead-shim sweep; observability | Phase 3 hardening / elective. |

**Parallelism summary:** SEC-001, PROC-001, the server-bounding half of CAP-001 have no inter-dependency and start day-1. TECH-001 is the hard prerequisite for the entire refactor spine. PROC-002 must precede every refactor. TECH-004's Robolectric upgrade is a co-requisite of TECH-001.

---

## Effort Summary

### By Category

| Category | Total Gaps | Effort Mix (S/M/L) |
|----------|:----------:|--------------------|
| Structural | 5 | 0S / 2M / 3L (STRUCT-001 L, 002 M, 003 M, 004 M, 005 S) → 1S/3M/1L corrected* |
| Technology | 7 | 1S / 2M / 4L (TECH-001 L, 002 L, 003 M, 004 M, 005-k9 L, 006 S, +1L) |
| Capability | 3 | 1S / 1M / 1L (QUAL-001 S, SEC-002 M, CAP-001 L) |
| Process | 3 | 0S / 3M (PROC-001 M, 002 M, 003 M) |

\* *Structural corrected count: STRUCT-001 L, STRUCT-002 M, STRUCT-003 M, STRUCT-004 M, STRUCT-005 S → 1S / 3M / 1L.*

### By Timeline (maps to target-state Migration Path phases)

| Phase | Gaps | Calendar (1 senior Android eng) |
|-------|------|---------------------------------|
| Phase 0 — Gate + safety net | TECH-001, DEP-006, QUAL-001, PROC-001, PROC-002, TECH-004(test), PROC-003(build-policy) | ~2–3 weeks |
| Phase 1 — Security-by-default (∥ Phase 0) | SEC-001, SEC-002/CAP-003, ARCH-010 redaction | ~2–3 weeks (overlaps) |
| Phase 2 — Substrate swap | STRUCT-001, TECH-002, TECH-003/STRUCT-002, STRUCT-003, TECH-005, CAP-001, STRUCT-004 | ~4–6 weeks |
| Phase 3 — Hardening (elective) | TECH-004(billing), TECH-006/DEP-005, ARCH-005, PROC-003(observability) | ~4+ weeks |

**Total order-of-magnitude:** consistent with the Step 2.1 estimate of ~6–9 engineer-weeks for the non-elective spine (Phases 0–2), Phase 3 elective on top.

---

## Risk Assessment

### Gaps with Highest Risk

| Gap | Risk Type | Likelihood | Impact | Mitigation |
|-----|-----------|:----------:|:------:|------------|
| TECH-001 | Behavioral breakage (background limits, FGS-type, notification perm, exact-alarm) | High | High | Stage 29→31→33→34/35; PROC-002 fitness function gates each step; QUAL-001 co-landed. |
| TECH-002 | WorkManager lifecycle differs → missed/duplicate scheduled backups | Medium | High | Characterization-test retry/constraints first (PROC-002); Parallel-Run new vs old scheduler in a debug build before cut-over. |
| SEC-001 | Removing trust-all breaks genuine self-signed-IMAP users | Medium | Medium | User-pinned-cert path + one-time migration notice; ADR. |
| TECH-005/DEP-005 | No API-compatible k-9 release; vendoring forced | Medium | Medium | ACL (STRUCT-001) makes either path swappable; vendor as fallback; `verification-metadata.xml` (PROC-001) meanwhile. |
| PROC-002 | Coverage unmeasured → refactors regress silently | Medium | High | JaCoCo + gate lands in Phase 0 *before* any refactor; backfill TD-001/TD-003. |
| PROC-003 | `-Werror` blocks build mid-migration | High | Low | Relax to warning + lint-baseline during transition; restore after. |
| All | Single-maintainer / maintenance-mode → stall | Medium | Medium | Each phase independently shippable; Phase 0 alone restores Play eligibility + de-risks supply chain. |

---

## Recommendations

### Immediate Actions (0–30 days)
1. Close `TECH-001` (SDK/AGP/Gradle gate) + co-land `QUAL-001` (FLAG_IMMUTABLE) + `DEP-006` (jcenter removal) + `TECH-004` test-dep upgrade — one Phase-0 build sweep.
2. Start `SEC-001` (trust-all removal + silent-migration fix) and `PROC-003` log-redaction **in parallel** — no SDK dependency; closes the only active Critical security exposure.
3. Stand up `PROC-001` (GitHub Actions + dependency verification) and `PROC-002` (JaCoCo coverage fitness function) — the safety net must exist before any refactor.

### Short-term (30–90 days)
1. Close `STRUCT-001` (Hexagonal ports) — the structural precondition for cheap substrate swaps.
2. Close `TECH-002` (WorkManager) — retires DEP-002 + scijava mirror + AsyncTask + AlarmManager fallback + the lifecycle bypass (ARCH-003) in one migration.
3. Close `SEC-002`/`CAP-003` (EncryptedSharedPreferences) at the `SecretStore` seam.

### Medium-term (90–180 days)
1. `TECH-003`/`STRUCT-002` (Otto → Flow), then `STRUCT-004` (MainActivity/ViewModel), then `TECH-005` (Hilt).
2. `CAP-001` (durable restore checkpoint + classified retry + server-bounded search).
3. Elective Phase 3: `TECH-004`(billing 7.x), `TECH-006`/`DEP-005` (k-9 unpin behind the ACL), `ARCH-005` (dead-shim sweep), `PROC-003`(observability/crash reporting).

---

## Metrics

| Metric | Value |
|--------|-------|
| Findings Count | 18 |
| Critical | 2 |
| High | 8 |
| Medium | 6 |
| Low | 2 |
| Informational | 0 |
| Files Analyzed | Phase 1 corpus (5 outputs) + Step 2.1 assessment + Step 2.2 target + this-session re-verification of `app/build.gradle`, root `build.gradle`, grep across `app/src/main/java`, glob `.github/workflows/` |
| Domains Covered | Modernization gap analysis — Structural (5), Technology (7), Capability (3), Process (3) |

---

## Limitations Encountered

- **Synthesis over re-discovery:** This gap analysis derives from the Phase 1 corpus + Steps 2.1/2.2 and re-verified the highest-leverage build-file and usage claims via grep/file-read this session (SDK levels, AGP, repo declarations, Otto=12/jobdispatcher=4/AsyncTask=3 counts, CI absence, no JaCoCo plugin). It did **not** re-read every source file underlying every inherited finding. Security/reliability/performance gaps (SEC-001, SEC-002, CAP-001, QUAL-001) carry their Phase 1 confidence and are marked "(Inherited)"; their exact line numbers should be re-confirmed at implementation time.
- **Static analysis only — no runtime/tooling measurement:** No `./gradlew test jacocoTestReport`, McCabe CC tool, OWASP dependency-check, or live Maven-Central currency check was run (Android SDK/Gradle toolchain not invoked). Coverage remains unmeasured (this *is* gap PROC-002); "latest version" target claims rest on Phase 1 analyst knowledge (cutoff Aug 2025). Effort S/M/L is order-of-magnitude per finding-schema, not a velocity-calibrated bottom-up estimate.
- **Source-count reconciliation:** Three minor inter-document inconsistencies (Otto 11 vs 12; prod LOC 9,162 vs 10,635; MainActivity 452 vs 499 LOC) were resolved against live grep/the ilities measured baseline and disclosed in §Evidence Collected; none changes a gap magnitude or priority.
- **No deployment/runtime environment:** Play Console policy state, live IMAP behavior, and WorkManager runtime semantics were not observable; the targetSdk floor (33/34) and WorkManager contract mappings are cited from policy/library knowledge and the Step 2.2 target, not a live console/device check.

---

## Phase Completion Report
---
artifact_id: 20260529-modernization
phase: execute
verdict: PASS
artifact_path: sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/gaps.md
assessment_status: in-progress
---
