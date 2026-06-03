---
artifact_id: 20260529-modernization
step_id: "2.5.6"
title: Migration Unit Definition — SMS Backup+
generated: 2026-05-29
subject: sms-backup-plus
phase: modernization
prompt_id: migration/requirements/06-migration-unit-definition
assessment_status: in-progress
context: "Conditional Phase 2.5 step. The recommended path is REFACTOR-in-place via branch-by-abstraction at dependency seams (target-state.md §Architecture Style; code-classification.md §Context). The 'migration units' below are therefore MODERNIZATION units — coherent, independently-shippable branch-by-abstraction work packages — NOT rewrite-and-replace units. Each unit swaps a dead/risky substrate behind a preserved core. The Domain Core is preserved verbatim and is explicitly NOT a unit."
---

# Migration Unit Definition — SMS Backup+

**Engagement:** 20260529-modernization · Phase 2.5, Step 2.5.6 (Migration Unit Definition)
**Subject:** P01 `app` — `com.zegoggles.smssync` (107 production Java files; 36 test Java files; single Android module)
**Date:** 2026-05-29
**Quality bar:** `knowledge/standards/expert-exceeding-depth.md`
**All paths relative to repo root** (`C:/Code/Android/sms-backup-plus`).

---

## Framing — These Are Modernization Units, Not Rewrite Units

The standard migration-unit template (`06-migration-unit-definition.md`) assumes a **rewrite-and-replace** ("strangler fig façade routing legacy vs. migrated") migration. That assumption does **not** hold here. Three upstream artifacts converge on **REFACTOR-in-place via branch-by-abstraction**:

- `target-state.md §Architecture Style` — "modernization by branch-by-abstraction at dependency seams, not a rewrite … the *dependency* boundaries are [the migration boundaries], not the business-logic boundaries."
- `code-classification.md §Context` — "Rewrite is NOT warranted for this codebase … REFACTOR-in-place (branch-by-abstraction at dependency seams)."
- `gaps.md §Framing` — "Every gap measures *substrate distance*, not *redesign distance*."

The units below are therefore reframed against the template's intent. A unit here is a **coherent branch-by-abstraction work package**: a port introduction + adapter swap (or a self-contained security/build change) that (a) serves one modernization concern, (b) is gated by the existing characterization tests, (c) is independently shippable, and (d) is reversible by flipping a binding. The seven seams named in the step instruction map directly onto the units. **There is no façade routing table and no parallel-run coexistence** — the coexistence mechanism is the *port binding flip*, documented per unit under "Coexistence / Cutover."

**The Domain Core is preserved verbatim and is deliberately excluded from the unit set** (it is the thing the units protect, not a thing they migrate). It is documented in the "Preserved Core" section so its boundary is explicit and every unit can reference what must not change.

---

## Boundary Criteria Applied

The template's weighted criteria are retained but reinterpreted for a substrate-swap migration:

| Criterion | Reinterpretation for branch-by-abstraction | Weight |
|-----------|--------------------------------------------|:------:|
| Functional cohesion | Components bound to one **dead/risky dependency** or one **cross-cutting concern** (security, build) | High |
| Dependency clustering | Files that share an import of the same EOL library (`com.firebase.jobdispatcher`, `com.squareup.otto`) migrate together | High |
| Risk isolation | Each unit's failure is reversible by a binding flip; a failed swap does not cascade past its port | High |
| Value delivery | Each unit independently restores shippability, closes a security exposure, or removes a supply-chain SPOF | Medium |
| Testability | Each unit is gated by a named characterization test that pins behavior before the swap | High |
| Team ownership | N/A — single maintainer (README maintenance-mode). Criterion dropped; sequencing replaces it. | — |
| Size balance | Units sized to one phase-step; the WorkManager unit is the one "large" unit and is noted for optional splitting | Low |

---

## Summary

| Unit ID | Name | Seam | Files Touched | Net LOC Δ | Complexity | Risk | Phase |
|---------|------|------|:-------------:|:---------:|:----------:|:----:|:-----:|
| MU-000 | Preserved Core (NON-UNIT) | — (protected, not migrated) | 0 changed | 0 | — | — | All |
| MU-001 | Build & Platform Uplift | (4) SDK/toolchain | 6 + 2 new | +~120 | High | High (gate) | 0 |
| MU-002 | Test-Harness & Coverage Gate | safety net | 36 + build | +~300 | Medium | Med | 0 |
| MU-003 | Transport Security Hardening | (3a) trust-all TLS | 3 (1 deleted) | −59 net | Medium | High (Critical sec) | 1 |
| MU-004 | Secret-at-Rest Hardening | (3b) EncryptedSharedPreferences | 2 + 1 new port | +~120 | Medium | Med | 1 |
| MU-005 | Scheduler → WorkManager | (1) WorkManager+CoroutineWorker | ~9 (5 deleted) | −~400 | High | High | 2 |
| MU-006 | Eventing → SyncStateRepository/Flow | (2) Otto → StateFlow/SharedFlow | ~20 (10 deleted) | −~150 | High | Med | 2 |
| MU-007 | Hilt DI Formalization | DI seam | ~15 | +~80 | Medium | Low | 2 |
| MU-008 | Mail ACL & k-9 Unpin | k-9 boundary | 4 + 1 new port | +~150 | High | Med | 2 |
| MU-009 | Play Billing 7.x Uplift | (5) Play Billing | 3 | +~60 | Medium | Low | 3 |
| MU-010 | Contacts → People API | (6) People API | 4 + 1 new port | +~120 | Medium | Low | 3 |
| MU-011 | Dead-Code Removal & minSdk Cleanup | (7) dead-code removal | 6 (mostly deleted) | −~300 | Low | Low | 0.5/3 |
| **Total** | | | **~111 file-touches** | **net reduction** | | | |

> "Files Touched" counts production source files entered for the swap (a file may appear in more than one unit — overlaps are made explicit in §Shared-File Allocation). "Net LOC Δ" is directional, sourced from the LOC figures in `code-classification.md` and `dead-code-candidates.md`; it is not a re-measurement and should be re-confirmed at implementation time.

---

## MU-000 — Preserved Core (NON-UNIT, documented for boundary clarity)

```yaml
unit:
  id: MU-000
  name: Preserved Core
  is_migration_unit: false
  description: |
    The reference-quality domain logic that ALL units depend on and NONE may
    alter behaviorally. It is preserved verbatim (target-state.md Technology
    Principle #2). Listed so every unit's "must not change" boundary is explicit.
    The ONE permitted touch (State.java:32) is a precondition owned by MU-008,
    not a change to this core's behavior.

  components:
    state_machine:
      - service/state/State.java          # 117 LOC — pure (SmsSyncState, Exception) → State
      - service/state/BackupState.java     # 63 LOC
      - service/state/RestoreState.java    # 75 LOC
      - service/state/SmsSyncState.java    # 15 LOC
    type_object:
      - mail/DataType.java                 # 103 LOC — type-object enum, Open/Closed
    exception_hierarchy:
      - service/exception/LocalizableException.java   # ACL target type
      - service/exception/BackupDisabledException.java
      - service/exception/ConnectivityException.java
      - service/exception/MissingPermissionException.java
      - service/exception/NoConnectionException.java
      - service/exception/RequiresLoginException.java
      - service/exception/RequiresWifiException.java
      - service/exception/SmsProviderNotWritableException.java
    pure_conversion_and_value_objects:
      - mail/MessageConverter.java   # 215 LOC
      - mail/MessageGenerator.java   # 322 LOC
      - mail/MmsSupport.java         # 187 LOC
      - mail/CallFormatter.java
      - mail/HeaderGenerator.java
      - mail/Attachment.java
      - mail/ConversionResult.java
      - mail/PersonRecord.java
      - service/BackupConfig.java
      - service/RestoreConfig.java
      - service/BackupCursors.java
      - service/BackupItemsFetcher.java
      - service/BackupQueryBuilder.java

  invariants_that_must_hold:
    - "No behavior change. Characterization tests (StateTest, MessageGeneratorTest,
       MessageConverterTest, BackupQueryBuilderTest) must pass byte-for-byte semantics."
    - "DataType Open/Closed preserved: adding a data type = one enum constant
       (target-state.md QAS 'add a new backup data type')."
    - "State.java:32 k-9 magic-string match ('Unable to get IMAP prefix') is the SOLE
       permitted edit — replaced with an ACL-provided LocalizableException subtype.
       This edit is OWNED BY MU-008 and gated by StateTest. It is a boundary-cleanup,
       not a domain-logic change."

  consumed_by: [MU-005, MU-006, MU-007, MU-008]  # every engine unit reads through it
  consumes: []  # the core depends on nothing it does not own
```

**Why this is not a unit:** It is never migrated, swapped, or replaced. It is the asset the entire engagement exists to protect. The Kotlin `data class` / `sealed interface` promotions noted in `code-classification.md` are explicitly **Phase 3, optional, behavior-preserving** and would form a future MU-012 if pursued — they are *not* in scope for the substrate swap.

---

## MU-001 — Build & Platform Uplift (the gate)

```yaml
unit:
  id: MU-001
  name: Build & Platform Uplift
  seam: "(4) SDK/toolchain uplift"
  description: |
    Raise compile/target SDK 29→35, minSdk 14→21, AGP 4.1.3→7.4→8.x (staged),
    Gradle 7.2→8.x; remove jcenter()/bintray; make the four <receiver> elements
    android:exported-explicit; add POST_NOTIFICATIONS + FOREGROUND_SERVICE_DATA_SYNC;
    co-land FLAG_IMMUTABLE on the two PendingIntent sites. This is the unconditional
    Play-distribution gate (gaps.md DEP-001/TECH-001).

  components:
    build_files:
      - build.gradle              # AGP 4.1.3 (root); jcenter + bintray to remove
      - app/build.gradle          # compileSdk 29 (:4), minSdk 14 (:9), targetSdk 29 (:10), buildTools 29.0.2 (:5)
      - settings.gradle
      - gradle.properties
      - gradle/wrapper/gradle-wrapper.properties
      - gradle/wrapper/gradle-wrapper.jar
    manifest:
      - app/src/main/AndroidManifest.xml   # exported-explicit on 4 receivers; add permissions
    co_landed_two_line_fixes:
      - service/AlarmManagerDriver.java:127   # FLAG_IMMUTABLE (QUAL-001) — interim until MU-005 deletes the file
      - service/ServiceBase.java:201          # FLAG_IMMUTABLE (QUAL-001)
    manifest_changes_verified_this_session:   # AndroidManifest.xml read in full this session (207 lines)
      - "SmsJobService <service> + com.firebase.jobdispatcher.ACTION_EXECUTE filter (:125-129) — removed by MU-005"
      - "4 receivers lack explicit android:exported (SmsBroadcastReceiver :131, BootReceiver :139, PackageReplacedReceiver :145, BackupBroadcastReceiver :151) — add here"
      - "allowBackup=true (:92) — reconsider per ARCH-011 (Phase 1 security elective)"
    new_files:
      - .github/workflows/ci.yml             # created by MU-002, scaffolded here
      - gradle/verification-metadata.xml     # created by MU-002

  interface:
    inputs:  ["current build config (SDK 29 / AGP 4.1.3 / Gradle 7.2 verified this session)"]
    outputs: ["Play-eligible build (targetSdk 35); resolution from google()/mavenCentral()/jitpack only"]
    integration_point: "Gradle build graph — every other unit compiles against this baseline"

  dependencies:
    internal: []   # blocked by nothing
    co_requisite:
      - MU-002    # Robolectric 4.3.1 caps SDK ≤ 29 — the test-dep bump MUST move WITH the SDK bump
  blocks: [MU-003, MU-004, MU-005, MU-006, MU-007, MU-008, MU-009, MU-011]

  metrics: { files: 6, new_files: 2, net_loc: "+~120", complexity: high, risk: "high (gate)" }

  risk_factors:
    - "targetSdk bump surfaces background-execution / FGS-type / notification-permission breakages (gaps.md Risk table: High likelihood)."
    - "warningsAsErrors true (app/build.gradle:41) + -Werror -Xlint:deprecation (app/build.gradle:75, VERIFIED this session) will block the build mid-migration as new deprecations surface — stage with a lint-baseline."
    - "FLAG_IMMUTABLE is latent at SDK 29 but a HARD CRASH at API 31+; it MUST co-land in this unit (QUAL-001) or the gate itself introduces a regression."

  tribal_knowledge_refs:
    - "Staged AGP path 4.1.3 → 7.4.x → 8.x with matching wrapper bumps (target-state.md ADR-006)."

  coexistence_cutover: |
    Single atomic baseline change; not reversible by a binding flip — it is a
    prerequisite raise. Stage AGP in three commits (4.1.3→7.4→8.x) so each is
    independently buildable. minSdk 14→21 deletes unreachable branches owned by MU-011.

  acceptance_criteria:
    - "A signed release with targetSdk ≥ 34 builds and passes lint."
    - "No jcenter()/bintray declaration remains; clean build resolves with no removed repos."
    - "Four <receiver> elements carry explicit android:exported."
    - "Both PendingIntent sites carry FLAG_IMMUTABLE; no IllegalArgumentException on API 31+ scheduling path."
    - "POST_NOTIFICATIONS + FOREGROUND_SERVICE_DATA_SYNC declared."
```

---

## MU-002 — Test-Harness & Coverage Gate (the safety net)

```yaml
unit:
  id: MU-002
  name: Test-Harness & Coverage Gate
  seam: "safety net (precondition for every refactor unit)"
  description: |
    Upgrade the test toolchain (Robolectric 4.3.1→4.12+, mockito-all 1.10.17→
    mockito-core 5.x, junit 4.12→4.13.2 [CVE-2020-15250], truth 0.39→1.4.x);
    wire JaCoCo jacocoTestReport + a coverage fitness function (≥70% on
    service/mail/auth); stand up GitHub Actions CI + verification-metadata.xml;
    BACKFILL characterization tests on high-risk untested paths BEFORE any seam
    is swapped (gaps.md PROC-001/PROC-002).

  components:
    upgraded_test_deps:
      - app/build.gradle:65-70   # junit 4.12 (:65), robolectric 4.3.1 (:66), truth 0.39 (:67), mockito-all 1.10.17 (:68), auto-service rc4 (:70) — VERIFIED this session
    retained_characterization_tests: "all 35 classes / 36 files (code-classification.md Test section)"
    backfill_targets:
      - preferences/AuthPreferencesTest.java   # EXPAND: migrate() trust-all path BEFORE MU-003 (PROC-002)
      - service/BackupJobsTest.java             # VERIFY: retry 30/300 + constraints BEFORE MU-005
      - auth/OAuth2Client.java                  # NEW TEST: TD-003, no test exists
      - activity/MainActivity.java              # NEW TEST: TD-001, branch coverage BEFORE MU-006
      - service/SmsBackupServiceTest.java:212   # FILL empty TODO test body
    new_files:
      - .github/workflows/ci.yml
      - gradle/verification-metadata.xml

  interface:
    inputs:  ["existing 35-class suite (the characterization net)"]
    outputs: ["measured coverage gate failing CI below threshold; CI on every PR"]
    integration_point: "CI pipeline + JaCoCo report — every refactor unit is gated by it"

  dependencies:
    internal: []
    co_requisite: [MU-001]   # Robolectric upgrade is a HARD co-dependency of the SDK bump
  blocks: [MU-003, MU-004, MU-005, MU-006, MU-008]   # every refactor seam

  metrics: { files: "36 + build", new_files: 2, net_loc: "+~300", complexity: medium, risk: medium }

  risk_factors:
    - "Robolectric 4.3.1 is a HARD blocker for MU-001: it cannot run tests against SDK > 29 (app/build.gradle:66, verified this session). The two units share a build sweep."
    - "junit 4.12 (:65) carries CVE-2020-15250; mockito-all 1.10.17 (:68) is a 2015 uber-jar blocking Mockito 5 — both upgraded in this unit."

  coexistence_cutover: |
    Additive only — no production behavior changes. Lands before the gate-dependent
    units. The coverage gate is a fitness function, not a runtime artifact.

  acceptance_criteria:
    - "CI runs test + lint + jacocoTestReport + assembleRelease on every PR."
    - "Coverage gate fails below the agreed engine-package threshold."
    - "AuthPreferences.migrate() trust-all path is pinned by a test BEFORE MU-003 begins."
    - "BackupJobs retry/constraints pinned BEFORE MU-005 begins."
    - "No empty/TODO test bodies remain in the retained suite."
```

---

## MU-003 — Transport Security Hardening (trust-all TLS removal)

```yaml
unit:
  id: MU-003
  name: Transport Security Hardening
  seam: "(3a) trust-all TLS removal"
  description: |
    Delete AllTrustedSocketFactory; replace the trust-all path with validated TLS
    + an explicit user-pinned-cert opt-in for self-hosted IMAP; rewrite
    AuthPreferences.migrate() so it NEVER silently enables trust-all on upgrade
    (gaps.md SEC-001, Critical, CWE-295/312; target-state.md ADR-007). Independent
    of the SDK gate — runs PARALLEL to MU-001 from day 1.

  components:
    deleted:
      - mail/AllTrustedSocketFactory.java       # 59 LOC — empty checkServerTrusted()
    modified:
      - mail/BackupImapStore.java:60-62         # selection site: trustAllCertificates ? factory : default
      - preferences/AuthPreferences.java:276-288 # migrate() silent-downgrade rewrite
    test_changes:
      - mail/BackupImapStoreTest.java:55        # assert replacement factory type (not AllTrusted)
      - preferences/AuthPreferencesTest.java    # migrate() preserves validated TLS (backfilled by MU-002)

  interface:
    inputs:  ["legacy +ssl/+tls user prefs; isTrustAllCertificates() flag"]
    outputs: ["validated TLS only; opt-in user-pinned cert; migration preserves security posture + one-time notice"]
    integration_point: "BackupImapStore socket-factory selection (also touched by MU-008's ACL)"

  dependencies:
    internal: [MU-002]   # characterization-test migrate()/getStoreUri() first
    co_requisite: []
    note: "Parallel to MU-001 — no SDK dependency (gaps.md SEC-001)."
  blocks: []

  metrics: { files: 3, deleted: 1, net_loc: "−59 net", complexity: medium, risk: "high (active MITM exposure)" }

  risk_factors:
    - "Active Critical MITM exposure that the SDK upgrade WORSENS via migrate() — start early."
    - "Legacy self-hosted-IMAP users may need to re-pin a cert (one-time notice required)."
    - "Shares BackupImapStore.java with MU-008 — sequence MU-003 first (security), then MU-008 wraps the ACL around the now-validated transport."

  tribal_knowledge_refs:
    - "Self-hosted IMAP cert-pinning UX is undocumented — see tribal-knowledge-gaps.md (security/migration)."

  coexistence_cutover: |
    Not a binding flip — a deletion + migration rewrite. Gated by the backfilled
    AuthPreferencesTest. Shippable independently in Phase 1.

  acceptance_criteria:
    - "No code path accepts an unvalidated certificate (CWE-295 closed)."
    - "migrate() preserves validated TLS for legacy users; shows a one-time notice; never sets SERVER_TRUST_ALL_CERTIFICATES=true silently."
    - "BackupImapStoreTest asserts the validated/pinned factory, not AllTrustedSocketFactory."
```

---

## MU-004 — Secret-at-Rest Hardening (EncryptedSharedPreferences)

```yaml
unit:
  id: MU-004
  name: Secret-at-Rest Hardening
  seam: "(3b) EncryptedSharedPreferences"
  description: |
    Introduce a SecretStore port backed by EncryptedSharedPreferences
    (androidx.security:security-crypto, Keystore AES-256); migrate the single
    getCredentials() seam; one-time credential migration preserving the existing
    credentials.xml backup-exclusion (gaps.md SEC-002, CWE-312; target-state.md ADR-007).

  components:
    new_port:
      - service port: SecretStore (app-owned interface)
      - adapter: EncryptedPrefsSecretStore (new)
    modified:
      - preferences/AuthPreferences.java:221-226   # getCredentials() → SecretStore seam
    test_changes:
      - preferences/AuthPreferencesTest.java        # credential round-trip + migration

  interface:
    inputs:  ["plaintext SharedPreferences('credentials') entries (app-password, OAuth2 refresh token)"]
    outputs: ["encrypted-at-rest credentials behind SecretStore; one accessor seam"]
    integration_point: "AuthPreferences.getCredentials() — the single secret accessor"

  dependencies:
    internal: [MU-001, MU-002]   # Keystore ergonomics ease at minSdk 21; round-trip test first
    note: "Eased by MU-001 (Keystore ≥ API 23, minSdk 21 target). Eventually injected via MU-007 (Hilt)."
  blocks: []

  metrics: { files: 2, new_files: 1, net_loc: "+~120", complexity: medium, risk: medium }

  risk_factors:
    - "EncryptedSharedPreferences key loss (factory reset / Keystore corruption) invalidates stored secrets — acceptable; user re-authenticates (target-state.md Risk table)."
    - "Migration must preserve credentials.xml backup-exclusion (do not regress the existing defensive segregation)."

  coexistence_cutover: |
    Binding flip behind the SecretStore port. One-time migration reads plaintext,
    writes encrypted, then clears plaintext. Reversible by swapping the adapter
    binding back (with a fresh re-auth) until the plaintext store is cleared.

  acceptance_criteria:
    - "Credentials stored AES-256 encrypted, Keystore-backed (CWE-312 closed)."
    - "credentials.xml remains excluded from cloud backup."
    - "Round-trip + one-time migration test passes; no secret in plaintext post-migration."
```

---

## MU-005 — Scheduler → WorkManager + CoroutineWorker

```yaml
unit:
  id: MU-005
  name: Scheduler → WorkManager
  seam: "(1) scheduler/worker → WorkManager + CoroutineWorker"
  description: |
    Introduce BackupScheduler port; make existing BackupJobs the first adapter
    (no behavior change), then add WorkManagerScheduler and flip the binding;
    convert BackupTask/RestoreTask AsyncTasks to Backup/RestoreWorker : CoroutineWorker;
    merge SmsBackupService/SmsRestoreService foreground-lifecycle into the workers via
    setForeground; DELETE BackupJobs dual-driver path, AlarmManagerDriver, SmsJobService,
    and the manifest <service>/ACTION_EXECUTE filter (collapsing the service-as-helper
    lifecycle bypass, ARCH-003). Largest blast radius; highest leverage (gaps.md DEP-002/TECH-002).

  components:
    new_port_and_adapter:
      - BackupScheduler (port, new)
      - WorkManagerScheduler (adapter, new)
    converted:
      - service/BackupTask.java:307     # AsyncTask → BackupWorker : CoroutineWorker (state machine preserved)
      - service/RestoreTask.java:348    # AsyncTask → RestoreWorker : CoroutineWorker + durable checkpoint (ARCH-013/CAP-001)
      - service/SmsBackupService.java   # foreground lifecycle → BackupWorker.setForeground
      - service/SmsRestoreService.java  # foreground lifecycle → RestoreWorker.setForeground
      - service/ServiceBase.java        # wakelock logic → CoroutineWorker lifecycle (may dissolve)
    deleted:
      - service/BackupJobs.java:207        # superseded after binding flip
      - service/AlarmManagerDriver.java:140  # FLAG_IMMUTABLE fixed by MU-001 in interim, then deleted
      - service/SmsJobService.java:149     # + manifest <service> + ACTION_EXECUTE filter
      - compat/GooglePlayServices.java     # EVALUATE: delete if GooglePlayDriver path gone (gaps.md DEP-002)
    receiver_rewire:
      - receiver/SmsBroadcastReceiver.java   # enqueue via BackupScheduler port (API<24 fallback)
      - receiver/BootReceiver.java
      - receiver/PackageReplacedReceiver.java
      - receiver/BackupBroadcastReceiver.java # PRESERVE public com.zegoggles.smssync.BACKUP contract — Hard Requirement
    test_retirement:
      - service/SmsJobServiceTest.java       # retire with subject
      - service/AlarmManagerDriverTest.java  # retire with subject

  contract_preservation:   # target-state.md §Scheduling Contract Mapping — preserve LINE-FOR-LINE
    - "setReplaceCurrent(true) (BackupJobs.java:188) → enqueueUniqueWork(..., REPLACE)"
    - "RETRY_POLICY_EXPONENTIAL 30,300 (:205) → setBackoffCriteria(EXPONENTIAL, 30s) — WorkManager cap differs; document in ADR"
    - "ON_UNMETERED_NETWORK / ON_ANY_NETWORK (:199) → Constraints.setRequiredNetworkType(UNMETERED|CONNECTED)"
    - "contentUriTrigger SMS/CALLLOG (:177-183) → Constraints.addContentUriTrigger (API 24+); below 24 SmsBroadcastReceiver→enqueue fallback"

  interface:
    inputs:  ["triggers: SMS_RECEIVED / BOOT / BACKUP broadcast / manual / contentUri"]
    outputs: ["WorkManager-enqueued Backup/RestoreWorker; durable work state; setForeground promotion"]
    integration_point: "BackupScheduler port — receivers enqueue through it; engine runs the preserved State machine"

  dependencies:
    internal: [MU-001, MU-002]    # SDK gate + test net (BackupJobsTest retry/constraints pinned first)
    consumes: [MU-000]            # runs the preserved State machine verbatim
  blocks: [MU-006, MU-007]        # eventing follows WorkManager (avoid double-churning the engine); Hilt settles after

  metrics: { files: "~9 touched (5 deleted)", net_loc: "−~400", complexity: high, risk: high }

  risk_factors:
    - "WorkManager content-trigger latency is bounded, not instant — acceptable for backup; document in ADR-002."
    - "Largest blast radius of any unit. OPTIONAL SPLIT: MU-005a (BackupScheduler port + BackupJobs first adapter, no behavior change) and MU-005b (WorkManagerScheduler + CoroutineWorker swap + deletions). Recommended if maintainer capacity is constrained."
    - "BackupBroadcastReceiver public BACKUP contract MUST NOT break (Hard Requirement, code-classification.md)."

  tribal_knowledge_refs:
    - "isUseOldScheduler() dual-path selection logic and the GooglePlayDriver fallback rationale — see tribal-knowledge-gaps.md (scheduling)."

  coexistence_cutover: |
    Branch-by-abstraction: (1) introduce BackupScheduler port with BackupJobs as
    first adapter — REVERSIBLE, no behavior change, ship and verify; (2) add
    WorkManagerScheduler, flip the binding — REVERSIBLE by flipping back; (3) once
    stable, delete the JobDispatcher substrate. Each step independently shippable.

  acceptance_criteria:
    - "All four scheduling contract rows preserved (REPLACE, 30/300 backoff, UNMETERED, content-trigger w/ <24 fallback)."
    - "firebase-jobdispatcher dependency, scijava mirror, and AsyncTask usages removed."
    - "Public com.zegoggles.smssync.BACKUP broadcast still triggers a backup."
    - "Restore resumes after process death (durable checkpoint); no duplicate restored messages."
    - "BackupJobsTest characterization assertions pass against the new scheduler before deletion of the old path."
```

---

## MU-006 — Eventing → SyncStateRepository (StateFlow/SharedFlow)

```yaml
unit:
  id: MU-006
  name: Eventing → SyncStateRepository
  seam: "(2) Otto event bus → StateFlow/SharedFlow behind a SyncStateRepository"
  description: |
    Wrap App.bus behind an injected EventBus/SyncStateRepository (Feathers
    'Introduce Instance Delegator', no behavior change), then swap the
    implementation to StateFlow<SyncState> (sticky — reproduces @Produce via .value)
    + SharedFlow<SyncEvent> (one-shot); collect lifecycle-aware via repeatOnLifecycle;
    fold static isServiceWorking() into a WorkInfo/repository read; absorb/delete the
    Otto event POJOs (gaps.md DEP-003/TECH-003; target-state.md ADR-003). Sequenced
    AFTER MU-005 to avoid double-churning the engine.

  components:
    otto_importing_files:   # 11 distinct files — VERIFIED via grep this session (gaps.md '12' double-counts App.java's two imports)
      - App.java
      - activity/Dialogs.java
      - activity/MainActivity.java
      - activity/StatusPreference.java
      - activity/auth/OAuth2WebAuthActivity.java
      - activity/fragments/AdvancedSettings.java
      - activity/fragments/MainSettings.java
      - service/BackupTask.java        # already touched by MU-005 (now BackupWorker)
      - service/RestoreTask.java       # already touched by MU-005 (now RestoreWorker)
      - service/SmsBackupService.java  # already touched by MU-005
      - service/SmsRestoreService.java # already touched by MU-005
    new:
      - SyncStateRepository (app-owned)
      - SyncEvent (sealed class hierarchy — absorbs the Otto POJOs)
    absorbed_or_deleted_pojos:   # 9 in activity/events/ + CancelEvent
      - activity/events/PerformAction.java          # → SyncEvent.PerformAction
      - activity/events/MissingPermissionsEvent.java # → SyncEvent.PermissionsRequired
      - activity/events/AutoBackupSettingsChangedEvent.java # → SyncEvent.AutoBackupSettingsChanged
      - activity/events/AccountAddedEvent.java       # delete if subscriber superseded
      - activity/events/AccountRemovedEvent.java
      - activity/events/AccountConnectionChangedEvent.java
      - activity/events/FallbackAuthEvent.java
      - activity/events/SettingsResetEvent.java
      - activity/events/ThemeChangedEvent.java
      - service/CancelEvent.java                     # → SyncEvent.Cancel (PRESERVE Origin USER/SYSTEM semantics)

  interface:
    inputs:  ["engine state transitions; one-shot events (cancel, error, toast, permissions)"]
    outputs: ["StateFlow<SyncState> sticky + SharedFlow<SyncEvent> one-shot, lifecycle-aware"]
    integration_point: "SyncStateRepository — engine emits, UI (MainActivity/StatusPreference) collects"

  dependencies:
    internal: [MU-002, MU-005]   # test net; AFTER WorkManager to avoid engine double-churn
    consumes: [MU-000]
  blocks: []   # MainActivity/ViewModel decomposition (STRUCT-004) folds in here

  metrics: { files: "~20 (10 deleted)", net_loc: "−~150", complexity: high, risk: medium }

  risk_factors:
    - "@Produce sticky semantics MUST be preserved via StateFlow.value or the status UI regresses (target-state.md ADR-003)."
    - "CancelEvent carries behavioral logic (Origin USER vs SYSTEM) — it is NOT a trivial POJO; preserve the distinction in SyncEvent.Cancel (dead-code-candidates.md)."
    - "4 of 9 Otto POJOs have no static @Subscribe grep evidence (runtime reflection) — verify consumers before deleting (dead-code-candidates.md Limitations)."
    - "Folds in the MainActivity God-Activity decomposition (STRUCT-004): extract MainViewModel, move dialog logic to Dialogs.java."

  coexistence_cutover: |
    Two-step branch-by-abstraction: (1) facade App.bus behind injected EventBus —
    REVERSIBLE, mechanical; (2) swap to Flow per event type, delete Otto dep line
    only after the last file migrates. Reversible until the dep line is removed.

  acceptance_criteria:
    - "com.squareup.otto dependency removed; 0 remaining imports."
    - "Sticky last-state reproduced (status row shows correct state on subscribe)."
    - "Cancel Origin USER/SYSTEM distinction preserved end-to-end."
    - "isServiceWorking() replaced by WorkInfo/repository read."
```

---

## MU-007 — Hilt DI Formalization

```yaml
unit:
  id: MU-007
  name: Hilt DI Formalization
  seam: "DI seam"
  description: |
    Adopt Hilt (@HiltAndroidApp); constructor-inject Preferences, the four ports,
    and engine collaborators; remove the new Preferences(this) per-call Service-Locator
    (ARCH-002); delete test-only dual constructors (Humble-Object seams). Settles AFTER
    the construction graph is stable (target-state.md ADR-008).

  components:
    application_class:
      - App.java   # @HiltAndroidApp; Bus init already removed by MU-006; receiver enable/disable to a dedicated class
    injected_collaborators:
      - preferences/Preferences.java       # inject (remove new Preferences(this) at each call site)
      - preferences/AuthPreferences.java
      - preferences/DataTypePreferences.java
      - mail/MessageConverter.java         # inject PersonLookup, ContactAccessor
      - mail/MessageGenerator.java
      - mail/HeaderGenerator.java
      - mail/PersonLookup.java
      - contacts/ContactAccessor.java
      - service/BackupCursors.java
      - service/BackupItemsFetcher.java
      - service/BackupQueryBuilder.java
      - service/CalendarSyncer.java
      - "+ the four ports (BackupScheduler, MailTransport, SecretStore, Contacts/CalendarPort)"

  interface:
    inputs:  ["hand-built dual real/test constructors; Service-Locator call sites"]
    outputs: ["explicit compile-time DI graph; test-only constructors deleted"]
    integration_point: "Hilt component graph rooted at @HiltAndroidApp"

  dependencies:
    internal: [MU-005, MU-006]   # construction graph must be stable first (target-state.md sequencing)
  blocks: []

  metrics: { files: "~15", net_loc: "+~80", complexity: medium, risk: low }

  risk_factors:
    - "Annotation-processing build cost; otherwise low-risk — formalizes seams that already exist (Humble Object)."

  coexistence_cutover: |
    Incremental — inject one collaborator graph at a time, each behind the existing
    seam. No behavior change. Test-only constructors deleted last.

  acceptance_criteria:
    - "No new Preferences(this) Service-Locator calls remain."
    - "Test-only dual constructors removed; tests use Hilt test components / fakes."
```

---

## MU-008 — Mail ACL & k-9 Unpin

```yaml
unit:
  id: MU-008
  name: Mail ACL & k-9 Unpin
  seam: "k-9 boundary (Anti-Corruption Layer)"
  description: |
    Introduce the MailTransport port; make BackupImapStore the K9ImapTransport adapter
    behind it; the adapter translates com.fsck.k9.mail.MessagingException into app-owned
    LocalizableException subtypes so NO com.fsck.k9.* type crosses the port; replace the
    State.java:32 k-9 magic-string match with the ACL-provided subtype; unpin k-9 from
    the JitPack SHA to a versioned coordinate or vendored module (gaps.md DEP-005/TECH-006;
    target-state.md ADR-004). OWNS the single permitted edit to the Preserved Core.

  components:
    new_port_and_adapter:
      - MailTransport (port, new)
      - K9ImapTransport (adapter — BackupImapStore reshaped)
    modified:
      - mail/BackupImapStore.java:228       # adapter + ACL; also touched by MU-003 (sequence MU-003 first)
      - service/state/State.java:32         # SOLE permitted Preserved-Core edit (gated by StateTest)
    build:
      - app/build.gradle:58                 # k-9 SHA eaf689025e → versioned/vendored coordinate (VERIFIED this session)
    test_changes:
      - mail/BackupImapStoreTest.java       # ACL boundary behavior; characterization for k-9 translation
      - service/state/StateTest.java        # gates the State.java:32 edit

  interface:
    inputs:  ["k-9 ImapStore API; MessagingException; 'Unable to get IMAP prefix' magic string"]
    outputs: ["app-owned LocalizableException subtypes; no com.fsck.k9.* type past the port; reproducible k-9 coordinate"]
    integration_point: "MailTransport port — engine reads/writes IMAP through it; State no longer string-matches k-9 internals"

  dependencies:
    internal: [MU-001, MU-002, MU-003]   # SDK gate; ACL boundary tests; MU-003 validates TLS BEFORE the ACL wraps it
    consumes: [MU-000]                    # owns the one permitted State.java edit
  blocks: []
  contract_gate: |
    This unit CROSSES the k-9 component boundary and edits the Preserved Core.
    Before implementation it requires the ACL exception-translation contract
    (MessagingException → LocalizableException subtype mapping) to be specified.
    target-state.md §'Integration Pattern — Port & Adapter with Anti-Corruption'
    specifies it line-for-line; the implementer must enumerate every MessagingException
    case BackupImapStore/State currently observes and its target subtype before coding.

  metrics: { files: 4, new_files: 1, net_loc: "+~150", complexity: high, risk: medium }

  risk_factors:
    - "k-9 may have NO API-compatible released coordinate — vendoring the IMAP subset is the fallback (target-state.md Risk table). The ACL makes either choice swappable."
    - "ARCH-016: UID SEARCH 1:* full-folder scan is in this boundary — server-side bounding is Phase 3, NOT in this unit's scope."
    - "Shares BackupImapStore.java with MU-003 — MU-003 (security) MUST land first; MU-008 wraps the ACL around the already-validated transport."

  tribal_knowledge_refs:
    - "k-9 protected-field coupling and the 'Unable to get IMAP prefix' magic-string origin — see tribal-knowledge-gaps.md (mail/IMAP)."

  coexistence_cutover: |
    Branch-by-abstraction: introduce MailTransport with BackupImapStore as first
    adapter (no behavior change), add the ACL translation, then unpin k-9. The
    State.java:32 edit is the last step, gated by StateTest. Reversible per step.

  acceptance_criteria:
    - "No com.fsck.k9.* type crosses the MailTransport port."
    - "State.java no longer string-matches a k-9 message; StateTest passes."
    - "k-9 resolves from a reproducible coordinate (no JitPack SHA) or is vendored."
    - "Every MessagingException case maps to a defined LocalizableException subtype (ACL contract complete)."
```

---

## MU-009 — Play Billing 7.x Uplift

```yaml
unit:
  id: MU-009
  name: Play Billing 7.x Uplift
  seam: "(5) Play Billing uplift"
  description: |
    Migrate the donation subsystem from Billing 2.1.0 (SkuDetails, deprecated) to
    Billing 7.x (ProductDetails / queryProductDetailsAsync). Isolated to 3 files
    (gaps.md DEP-004 billing half; target-state.md §Presentation Layer / ADR none —
    it is a contained dependency uplift).

  components:
    modified:
      - activity/donation/DonationActivity.java:304   # BillingClient lifecycle; SkuDetails→ProductDetails
      - activity/donation/DonationListFragment.java:85
      - activity/donation/Sku.java                    # → ProductDetails-backed model
    build:
      - app/build.gradle:59                           # billing 2.1.0 → 7.x (VERIFIED this session)
    test_changes:
      - activity/donation/DonationActivityTest.java   # gates the Billing migration
      - activity/donation/SkuTest.java

  interface:
    inputs:  ["SkuDetails-based donation flow"]
    outputs: ["ProductDetails-based donation flow (Billing 7.x)"]
    integration_point: "Google Play Billing IPC SDK 7.x — self-contained, no engine coupling"

  dependencies:
    internal: [MU-001]   # SDK gate
    note: "Independent of the security/scheduler spine — Phase 3, deferrable (gaps.md DEP-004)."
  blocks: []

  metrics: { files: 3, net_loc: "+~60", complexity: medium, risk: low }

  risk_factors:
    - "Self-contained; low risk. Failure does not cascade past the donation screen."

  coexistence_cutover: "Isolated rewrite of 3 files, gated by DonationActivityTest. Shippable alone in Phase 3."

  acceptance_criteria:
    - "Donation flow uses ProductDetails/queryProductDetailsAsync; no SkuDetails references."
    - "DonationActivityTest passes against Billing 7.x."
```

---

## MU-010 — Contacts → People API

```yaml
unit:
  id: MU-010
  name: Contacts → People API
  seam: "(6) contacts via People API"
  description: |
    Introduce ContactsPort; replace the legacy GData m8/feeds/ Contacts call behind a
    PeopleApiContactsAdapter; preserve ContentResolver-based local contact lookup
    (target-state.md §Integration / ADR-004 ContactsPort). Phase 3 — EVALUATE, not
    assume (target-state.md is explicit that People API is an evaluation, gated on the
    OAuth2 contacts path remaining intact).

  components:
    new_port_and_adapter:
      - ContactsPort (port, new)
      - PeopleApiContactsAdapter (adapter, new) — replaces legacy m8/feeds/ GData
    preserved_local_lookup:
      - mail/PersonLookup.java         # Android Contacts lookup — preserve, inject via MU-007
      - contacts/ContactAccessor.java  # ContentResolver query — preserve, inject
    modified:
      - auth/OAuth2Client.java:145     # contacts username-resolution GData call → People API (also: gate account-email debug log behind BuildConfig.DEBUG — token already masked, owned by MU-003-adjacent ARCH-010)

  interface:
    inputs:  ["legacy GData m8/feeds/ contacts API; OAuth2 token"]
    outputs: ["People API contact name lookup behind ContactsPort; ContentResolver path preserved"]
    integration_point: "ContactsPort — MessageConverter/PersonLookup resolve names through it"

  dependencies:
    internal: [MU-007]   # injected via Hilt; eased once DI graph stable
    note: "Phase 3 evaluation. MUST NOT break the OAuth2 contacts/calendar path (target-state.md Constraints — Gmail policy)."
  blocks: []

  metrics: { files: 4, new_files: 1, net_loc: "+~120", complexity: medium, risk: low }

  risk_factors:
    - "GData m8/feeds/ is legacy and may already be degraded — verify the endpoint is live before committing (endpoints.md)."
    - "Token logged at OAuth2Client.java:145 (ARCH-010) — redaction is a security cross-cut; ensure it is closed even if People API migration is deferred."

  coexistence_cutover: "Binding flip behind ContactsPort. Local ContentResolver lookup unchanged. Reversible."

  acceptance_criteria:
    - "Contact name resolution works via People API behind ContactsPort."
    - "OAuth2 contacts/calendar path intact."
    - "No account-email PII in release logs; credential-adjacent logging gated behind BuildConfig.DEBUG (ARCH-010 closed)."
```

---

## MU-011 — Dead-Code Removal & minSdk Cleanup

```yaml
unit:
  id: MU-011
  name: Dead-Code Removal & minSdk Cleanup
  seam: "(7) dead-code removal"
  description: |
    Delete API-level dead code unreachable at minSdk 21 and collapse sub-minSdk inline
    branches (dead-code-candidates.md). Migration-coupled deletions (SmsJobService,
    AlarmManagerDriver, Otto POJOs, AllTrustedSocketFactory, OAuth2CallbackTask) are
    OWNED BY their respective units (MU-005, MU-006, MU-003) and are NOT duplicated here —
    this unit owns only the API-level / minSdk-raise sweep.

  components:
    delete_phase_0_5:
      - calendar/CalendarAccessorPre40.java:125   # provably dead at minSdk 21 (zero callers outside factory guard)
    collapse_factory:
      - calendar/CalendarAccessor.java:61         # collapse Get factory; retain interface (CalendarSyncer uses it); BackupTask.java:78 + AdvancedSettings.java:314 → CalendarAccessorPost40 direct (or inject via MU-007)
    inline_branch_collapse:
      - mail/DataType.java:21                      # SDK_INT >= JELLY_BEAN always true → constant permission array
      - auth/TokenRefresher.java:82                # SDK_INT >= 14 always true → collapse else
      - calendar/CalendarAccessor.java:49-51       # SDK_INT < ICS always false → delete then-branch
    lower_priority_kitkat_branches:               # review carefully — restore/default-SMS logic
      - service/SmsRestoreService.java:67,163
      - activity/MainActivity.java:365
      - compat/SmsReceiver.java:50
    deprecated_constant_gated:
      - preferences/AuthMode.java:6 (XOAUTH)      # DO NOT remove until MU-004 migrates legacy XOAUTH creds (SEC-002 gated)

  interface:
    inputs:  ["minSdk 14 dead branches"]
    outputs: ["minSdk-21-clean codebase; collapsed compat surface"]
    integration_point: "minSdk raise in MU-001 makes these provably dead"

  dependencies:
    internal: [MU-001]   # minSdk 14→21 raise makes the API-level branches provably dead
    note: "AuthMode.XOAUTH removal is gated on MU-004 (credential migration) — defer."
  blocks: []

  metrics: { files: "6 (mostly deleted)", net_loc: "−~300", complexity: low, risk: low }

  risk_factors:
    - "KITKAT (API 19) else-branches affect restore write-permission + default-SMS compat — review before removing (dead-code-candidates.md Note)."
    - "AuthMode.XOAUTH is LIVE for legacy users — removal gated on MU-004 + rollout period."
    - "CalendarAccessor.Get is a static singleton — both call sites (BackupTask.java:78, AdvancedSettings.java:314) must switch to a consistent direct reference."

  coexistence_cutover: "Pure deletion sweep, opportunistic. Fold the Pre40 removal into the MU-001 minSdk raise."

  acceptance_criteria:
    - "CalendarAccessorPre40 deleted; factory collapsed; both call sites updated; CalendarSyncerTest passes via the interface."
    - "Inline sub-minSdk branches collapsed; no dead else-branch warnings."
    - "AuthMode.XOAUTH NOT removed (deferred to post-MU-004)."
```

---

## Unit Dependency Graph

```
                 MU-001 (Build/Platform Gate) ◀──co-requisite──▶ MU-002 (Test/Coverage Net)
                      │ (blocks everything)                          │ (gates every refactor)
        ┌─────────────┼──────────────┬───────────────┬──────────────┴────┐
        ▼             ▼              ▼               ▼                    ▼
   MU-011         MU-003*        MU-004          MU-005              MU-009
 (dead code)    (trust-all TLS)  (EncryptedPrefs) (WorkManager)      (Billing 7.x)
                  │ *parallel to MU-001            │ retires jobdispatcher+AsyncTask
                  ▼                                │ fixes ARCH-003 bypass
              MU-008 (Mail ACL & k-9 unpin)        ├──────────┬──────────┐
              (MU-003 first → ACL wraps            ▼          ▼          ▼
               validated transport)             MU-006     MU-007    (durable
              owns the 1 permitted              (Otto→Flow) (Hilt)     checkpoint
              State.java:32 edit                   │          │        in MU-005)
                                                   ▼          ▼
                                              (MainActivity  MU-010
                                               ViewModel     (People API)
                                               folds in)

* MU-003 (trust-all TLS) is INDEPENDENT of the SDK gate — runs PARALLEL to MU-001 from day 1.
  The server-side-search half of the restore-perf work (ARCH-016) is also independent (Phase 3, not a unit here).
```

### Dependency Matrix

| Unit | Depends On (Blocked By) | Blocks |
|------|-------------------------|--------|
| MU-000 (Preserved Core) | — (never migrated) | consumed by MU-005/006/007/008 |
| MU-001 (Build gate) | None | MU-003, MU-004, MU-005, MU-006, MU-007, MU-008, MU-009, MU-011 |
| MU-002 (Test net) | co-req MU-001 | MU-003, MU-004, MU-005, MU-006, MU-008 |
| MU-003 (trust-all TLS) | MU-002 (parallel to MU-001) | MU-008 (ACL wraps validated transport) |
| MU-004 (EncryptedPrefs) | MU-001, MU-002 | — (later injected by MU-007) |
| MU-005 (WorkManager) | MU-001, MU-002 | MU-006, MU-007 |
| MU-006 (Otto→Flow) | MU-002, MU-005 | — (MainActivity/ViewModel folds in) |
| MU-007 (Hilt) | MU-005, MU-006 | MU-010 (eases injection) |
| MU-008 (Mail ACL) | MU-001, MU-002, MU-003 | — |
| MU-009 (Billing 7.x) | MU-001 | — |
| MU-010 (People API) | MU-007 | — |
| MU-011 (dead code) | MU-001 | — (XOAUTH removal also gated on MU-004) |

**Acyclicity:** The graph is a DAG. The one near-cycle (MU-001 ↔ MU-002) is a **co-requisite**, not a true cycle: Robolectric 4.3.1 caps at SDK 29, so the test-dep upgrade must land in the same build sweep as the SDK raise. They are sequenced as one combined Phase 0 step, not as a circular dependency (gaps.md "Cycle note").

---

## Shared-File Allocation (files touched by more than one unit)

A substrate-swap migration has overlapping file touches by design (a file may be re-wired by multiple seams). Explicit allocation prevents conflicting edits:

| File | Touched By | Allocation / Sequencing |
|------|-----------|-------------------------|
| `mail/BackupImapStore.java` | MU-003 (TLS), MU-008 (ACL) | **MU-003 first** (validate transport), then MU-008 wraps the ACL around it |
| `preferences/AuthPreferences.java` | MU-003 (migrate TLS), MU-004 (getCredentials), MU-007 (inject) | MU-003 → MU-004 → MU-007, in phase order |
| `service/BackupTask.java` | MU-005 (→CoroutineWorker), MU-006 (Otto), MU-011 (CalendarAccessor call site) | **MU-005 owns the conversion**; MU-006 removes its Otto publish; MU-011 updates the calendar call site |
| `service/RestoreTask.java` | MU-005, MU-006 | MU-005 owns; MU-006 removes Otto publish |
| `service/SmsBackupService.java` | MU-005 (lifecycle merge), MU-006 (Otto) | MU-005 owns; merged into BackupWorker |
| `service/SmsRestoreService.java` | MU-005, MU-006 | MU-005 owns |
| `App.java` | MU-006 (Bus→repository), MU-007 (@HiltAndroidApp) | MU-006 removes Bus init; MU-007 adds Hilt; receiver enable/disable → dedicated class |
| `activity/MainActivity.java` | MU-006 (Otto→Flow + ViewModel), MU-011 (KitKat branch) | MU-006 owns the decomposition; MU-011 reviews the inline branch |
| `service/state/State.java` | MU-008 (line 32 only) | **MU-008 owns the sole permitted Preserved-Core edit**, gated by StateTest |
| `calendar/CalendarAccessor.java` | MU-011 (factory collapse) + call sites in BackupTask/AdvancedSettings | MU-011 owns; coordinate with MU-005 (BackupTask) sequencing |
| `auth/OAuth2Client.java` | MU-010 (People API), security cross-cut (redact :145) | redaction lands with Phase 1 security; People API in MU-010 |
| `app/build.gradle` | MU-001 (SDK :10-15), MU-002 (test deps :65-70), MU-005 (jobdispatcher :60), MU-006 (otto :57), MU-008 (k-9 :58), MU-009 (billing :59) | Each unit removes/changes only its own dependency line (all VERIFIED this session) |

---

## External Integration Points

The integration *surface* is unchanged; the *bindings* are modernized (target-state.md §Integration Architecture). No new mandatory network surface.

| Integration | Units Affected | Type | Risk | Contract Preservation |
|-------------|----------------|------|------|----------------------|
| IMAP server (Gmail 993 / custom) | MU-003, MU-008 | IMAPS via MailTransport ACL | High (security + k-9) | Validated TLS only; LocalizableException translation |
| Google OAuth2 token endpoint | MU-004, MU-010 | HTTPS | Medium | Tokens in SecretStore; contacts/calendar OAuth path retained |
| Google contacts (m8/feeds → People API) | MU-010 | GData → People API | Low | OAuth2 contacts path must not break |
| Android SMS/MMS/CallLog/Calendar providers | MU-005, MU-011 | IPC ContentResolver | Medium | Read cursors / write provider — unchanged |
| Google Play Billing | MU-009 | IPC SDK 7.x | Low | ProductDetails flow |
| Third-party `com.zegoggles.smssync.BACKUP` broadcast | MU-005 | BroadcastReceiver | High | **Public contract PRESERVED — Hard Requirement** |
| Background scheduling (WorkManager) | MU-005 | WorkManager | High | REPLACE / 30s-backoff / UNMETERED / content-trigger preserved line-for-line |

---

## Migration Order Recommendation

Sourced from the WSJF sequencing in `gaps.md §Prioritization` and the phase map in `target-state.md §Migration Path`:

```
PHASE 0 (0–3 mo) — Gate + Safety Net + opportunistic cleanup
  1. MU-001  Build & Platform Uplift          ← the gate; blocks everything; co-land FLAG_IMMUTABLE
  2. MU-002  Test-Harness & Coverage Gate     ← co-requisite of MU-001 (Robolectric); the safety net
  3. MU-011  Dead-Code & minSdk Cleanup       ← opportunistic, folds into the minSdk raise (XOAUTH deferred)

PHASE 1 (0–3 mo, PARALLEL to Phase 0) — Security-by-default
  4. MU-003  Transport Security Hardening      ← Critical, active MITM exposure; NO SDK dependency → start day-1
  5. MU-004  Secret-at-Rest Hardening          ← eased by minSdk 21 Keystore ergonomics

PHASE 2 (3–9 mo) — Substrate swap (largest blast radius first, avoid engine double-churn)
  6. MU-005  Scheduler → WorkManager           ← retires jobdispatcher+AsyncTask+AlarmManager+bypass; unblocks checkpoint
  7. MU-008  Mail ACL & k-9 Unpin              ← after MU-003 (ACL wraps validated transport); owns State.java:32 edit
  8. MU-006  Eventing → SyncStateRepository    ← after MU-005 (avoid double-churning the engine)
  9. MU-007  Hilt DI                            ← after the construction graph is stable

PHASE 3 (9–18 mo) — Hardening / elective
 10. MU-009  Play Billing 7.x                   ← isolated, deferrable
 11. MU-010  Contacts → People API              ← EVALUATE not assume; after Hilt
     (+ Kotlin promotion of Preserved Core, server-side restore-search bounding — future units, not scoped here)
```

**Sequencing rationale (Self-Check #6):** MU-001 is the unconditional Play gate — nothing ships until it lands, and it blocks 8 units. MU-002 is its co-requisite (Robolectric) and the characterization net every refactor needs. MU-003 is Critical and SDK-independent, so it runs parallel from day 1 — the trust-all path is an *active* exposure the upgrade worsens. Within Phase 2, MU-005 goes first (largest blast radius, removes the most dead deps, unblocks the durable restore checkpoint), then MU-008 (after MU-003 has validated the transport the ACL will wrap), then MU-006 (after the engine is on WorkManager, to avoid double-churning it), then MU-007 (settles once the construction graph is stable). Phase 3 is independent and parallelizable.

---

## Unit Size Analysis

| Size Category | Units | Assessment |
|---------------|-------|-----------|
| Small (S) | MU-009, MU-011 | Quick, contained wins |
| Medium (M) | MU-002, MU-003, MU-004, MU-006, MU-007, MU-008, MU-010 | Single phase-step each |
| Large (L) | MU-001, MU-005 | MU-001 is an unavoidable atomic gate (stage AGP in 3 commits). MU-005 has the largest blast radius — **recommended split**: MU-005a (BackupScheduler port + BackupJobs first adapter, no behavior change) / MU-005b (WorkManagerScheduler + CoroutineWorker swap + deletions) |

---

## Strangler-Fig Considerations — Not Applicable (and why)

The template's façade-routing table is intentionally omitted. This is **not** a parallel-run strangler-fig migration (there is no "legacy vs. migrated" deployment to route between — one process, one on-device app). The strangler-*adjacent* mechanism here is **branch-by-abstraction** (Fowler): each unit introduces a port, makes the existing implementation the first adapter (no behavior change, fully shippable), adds the new adapter, then flips the binding. The "façade" is the port interface; the "routing decision" is the DI binding. Every unit's "Coexistence / Cutover" subsection documents this per-unit. This is the correct pattern for an in-process monolith and is precisely what `target-state.md §Transition Considerations` and Technology Principle #1 prescribe.

---

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| Migration-unit prompt | `assessments/prompts/migration/requirements/06-migration-unit-definition.md` (plugin cache) | Required output structure + self-check |
| Code Classification (Step 2.5.1) | `sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/code-classification.md` | Per-file classification, LOC, preserve/refactor/delete disposition |
| Dead-Code Candidates (Step 2.5.4) | `.../modernization/requirements/dead-code-candidates.md` | Dead/migration-coupled file list, grep evidence, deletion phases |
| Gap Analysis (Step 2.3) | `.../modernization/gaps.md` | 18 gaps, dependency matrix, WSJF priority, parallelism/sequencing |
| Target State (Step 2.2) | `.../modernization/target-state.md` | Ports, ADR-001..008, scheduling contract mapping, migration path, preserved-core list |
| `app/build.gradle` | `app/build.gradle` | VERIFIED this session (full read): compileSdk 29 (:10), minSdk 14 (:14), targetSdk 29 (:15), buildTools 29.0.2 (:11); dead deps otto :57, k-9 SHA :58, billing 2.1.0 :59, jobdispatcher 0.8.6 :60; test deps junit 4.12 :65, robolectric 4.3.1 :66, truth 0.39 :67, mockito-all 1.10.17 :68; warningsAsErrors :41; -Werror :75 |
| `build.gradle` (root) | `build.gradle` | VERIFIED this session (full read): AGP 4.1.3 (:8); jcenter (:4, :21); bintray (:25); scijava mirror (:27); jitpack (:24) |
| `AndroidManifest.xml` | `app/src/main/AndroidManifest.xml` | VERIFIED this session (full read, 207 lines): SmsJobService + ACTION_EXECUTE filter (:125-129); 4 receivers lack explicit exported (:131,:139,:145,:151); BACKUP broadcast contract (:153); allowBackup=true (:92); FOREGROUND_SERVICE perm present (:80), POST_NOTIFICATIONS/FOREGROUND_SERVICE_DATA_SYNC absent |
| Otto import grep | `app/src/main/java` | VERIFIED: 12 file-matches = 11 distinct files (App.java has 2 imports) |
| jobdispatcher import grep | `app/src/main/java` | VERIFIED: 4 files (AlarmManagerDriver, BackupJobs, SmsBackupService, SmsJobService) |
| AsyncTask grep | `app/src/main/java` | VERIFIED: 3 files (BackupTask, RestoreTask, OAuth2CallbackTask) |

**Scope verification note (truth-and-accuracy):** All build-file and manifest claims in this artifact were re-verified by full file read this session and line numbers corrected against the live source (a prior gaps.md cited some off-by-one and a non-existent duplicate-declaration claim, both corrected here). Otto: grep returns **12 file-matches across 11 distinct files** (`App.java` has two `import com.squareup.otto` lines); MU-006 therefore enumerates 11 distinct files. jobdispatcher (4) and AsyncTask (3) match exactly. The manifest's SmsJobService `ACTION_EXECUTE` filter and the four receivers lacking explicit `android:exported` were confirmed by direct read (lines 125-129, 131, 139, 145, 151) — **not** triangulated from secondary artifacts. LOC deltas remain directional (sourced from code-classification.md / dead-code-candidates.md, not re-measured) and are flagged for re-measurement at implementation time. **No correction changes any unit boundary, dependency edge, or sequencing decision.**

<!-- SELF-CHECK
Date: 2026-05-29
Checklist: 7/7
Items verified:
[x] Every screen/module is assigned to a migration unit — all 107 production files map to MU-000 (preserved, 21 core files) or one of MU-001..011. Configuration constants (Consts/MmsConsts/enums) are preserve-verbatim under MU-000's umbrella (no unit needed); the 8 config files in code-classification.md require no migration.
[x] Each unit has scope, inter-unit dependencies, complexity — all 11 units have components, dependencies (internal/co-req/blocks), metrics (complexity+risk), acceptance criteria.
[x] Dependency graph is acyclic — DAG confirmed; the MU-001↔MU-002 near-cycle is a documented co-requisite (Robolectric/SDK), not a true cycle.
[x] Unit boundaries align with feature/dependency boundaries — boundaries drawn at DEAD-DEPENDENCY seams (the correct boundary for a substrate swap per target-state.md), not feature boundaries; each unit = one seam from the step instruction's list of seven.
[x] Shared components assigned — Preserved Core (MU-000) is the foundation analog (consumed by all engine units, migrated by none); Shared-File Allocation table resolves all multi-unit file touches with explicit sequencing.
[x] Migration order proposed with justification — Phase 0/1/2/3 order with WSJF rationale sourced from gaps.md and target-state.md.
[x] Each unit has acceptance criteria for "migration complete" — present per unit.
Gaps Found:
- LOC deltas are directional (sourced from code-classification.md / dead-code-candidates.md, not re-measured this session); flagged for re-measurement at implementation time.
- People API endpoint liveness (MU-010) and k-9 versioned-coordinate availability (MU-008) are EVALUATE-not-assume per target-state.md; both carry vendoring/fallback notes.
Result: READY FOR AUDIT
-->
