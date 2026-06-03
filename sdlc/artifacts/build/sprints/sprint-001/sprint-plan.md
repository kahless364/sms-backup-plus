---
status: active
execution_mode: parallel
id: sprint-001
sprint: '000001'
artifact_type: sprint-plan
---

# Sprint Plan: 001 — SMS Backup+ Modernization Roadmap

## Goals

- Deliver the complete SMS Backup+ modernization roadmap across 29 stories (U-001..U-029) organized into 11 dependency waves
- Upgrade the build toolchain from AGP 4.1.3 / Gradle 6.x / JDK 8 to AGP 8.x / Gradle 8.x / JDK 17 with targetSdk 35 (EPIC-001)
- Harden the TLS and credential security posture: eliminate AllTrustedSocketFactory, introduce TlsTrustPolicy, PinnedCertStore, and encrypted SecretStore (EPIC-001 / EPIC-002)
- Replace the legacy scheduler (AlarmManager / firebase-jobdispatcher) with WorkManager and rewrite BackupTask/RestoreTask as CoroutineWorkers (EPIC-002)
- Decouple the engine from k-9 mail internals via a MailTransport port/adapter; remove Otto; introduce SyncStateRepository and MainViewModel (EPIC-003)
- Land Hilt dependency injection, upgrade Play Billing to v7, and replace deprecated GData contacts with the People API (EPIC-003 / EPIC-004)

## Stories

| ID | Title | Type | Points | Complexity |
|----|-------|------|--------|------------|
| U-001 | AGP S1: Bump AGP 4.1.3 to 7.4.x, Gradle wrapper to 7.5.x, and raise minSdkVersion to 21 | build | 3 | medium |
| U-002 | AGP Stage S2: Bump AGP to 8.x, Gradle wrapper to 8.x, and pin JDK 17 toolchain | build | 3 | medium |
| U-003 | Stage S3: raise targetSdk to 35, co-land API-31-through-34 conformance fixes, and remove JCenter | build | 5 | high |
| U-004 | Add GitHub Actions CI workflow with JaCoCo per-package coverage gate and R8 enablement | ci | 3 | medium |
| U-005 | Upgrade test toolchain (Robolectric 4.12.x, JUnit 4.13.2, Mockito-core 5.x, Truth 1.4.x) | test | 2 | low |
| U-006 | Characterization tests: AuthPreferences.migrate(), BackupJobs retry/constraints, SmsBackupService notification | test | 3 | medium |
| U-007 | Rewrite AuthPreferences.migrate() to eliminate silent trust-all write and clear stale downgrade | security | 3 | medium |
| U-008 | Introduce TlsTrustPolicy enum, PinnedCertificateSocketFactory, PinnedCertStore, rewrite factory selection | security | 5 | high |
| U-009 | TLS enrollment UI: pin-cert preference action, one-time security notice, and string cleanup | ui | 3 | medium |
| U-010 | Delete AllTrustedSocketFactory, audit every trust write site, and declare Gate G1 | security | 2 | low |
| U-011 | Introduce SecretStore port, EncryptedPrefsSecretStore adapter, InMemorySecretStore fake | security | 3 | medium |
| U-012 | Idempotent plaintext-to-encrypted credential migration on first launch after upgrade | migration | 3 | medium |
| U-013 | Introduce BackupScheduler port and LegacyScheduler adapter (branch-by-abstraction seam) | architecture | 3 | medium |
| U-014 | WorkManagerScheduler: production BackupScheduler adapter with REPLACE semantics and backoff | scheduler | 5 | high |
| U-015 | CoroutineWorker rewrite: extract backup/restore use-cases and replace BackupTask/RestoreTask | refactor | 8 | high |
| U-016 | Durable Restore Checkpoint — Resumable Restore Worker with Fault-Injection Verification | feature | 5 | high |
| U-017 | Flip production binding LegacyScheduler→WorkManagerScheduler; delete legacy scheduler code; declare Gate G3 | cleanup | 3 | medium |
| U-018 | Dead-Code Removal and minSdk Cleanup: delete CalendarAccessorPre40, collapse sub-21 guards | cleanup | 2 | low |
| U-019 | SyncStateRepository Facade: define interface + SyncEvent sealed class and Otto-delegating impl | architecture | 3 | medium |
| U-020 | Otto removal: swap facade to MutableStateFlow/MutableSharedFlow, migrate 28 handler sites, delete App.bus | refactor | 8 | high |
| U-021 | Introduce MainViewModel and decompose MainActivity: migrate 9 @Subscribe handlers to Flow collection | ui | 5 | high |
| U-022 | Hilt bootstrap: @HiltAndroidApp on App, Gradle plugin, kapt/ksp, and all SingletonComponent @Module classes | di | 3 | medium |
| U-023 | Annotate @Inject constructors on eight BackupTask collaborators, remove manual wiring, migrate tests | di | 5 | high |
| U-024 | Wire HiltWorkerFactory into WorkManager Configuration.Provider; annotate BackupWorker/RestoreWorker | di | 3 | medium |
| U-025 | Define MailTransport port, app-owned ACL types, and reshape BackupImapStore into K9MailTransport adapter | architecture | 5 | high |
| U-026 | Engine Rewire to MailTransport: wire BackupTask/RestoreTask/ServiceBase to MailTransport port | refactor | 5 | high |
| U-027 | Unpin k-9 mail library from JitPack SHA to a reproducible coordinate; record in verification-metadata.xml | build | 2 | medium |
| U-028 | Upgrade Play Billing 2.1.0 → 7.x: replace SkuDetails pipeline with ProductDetails/queryProductDetailsAsync | upgrade | 3 | low |
| U-029 | Replace deprecated GData contacts call with ContactsPort + People API adapter | feature | 3 | medium |

## Execution Waves

| Wave | Story ID | Title | Dev Agent | Model | Assigned To | Depends On | Status | Priority |
|------|----------|-------|-----------|-------|-------------|------------|--------|----------|
| wave-1 | U-001 | AGP S1: Bump AGP 4.1.3 to 7.4.x, Gradle wrapper to 7.5.x, and raise minSdkVersion to 21 | developer | sonnet | lead | — | done | critical |
| wave-1 | U-004 | Add GitHub Actions CI workflow with JaCoCo per-package coverage gate and R8 enablement | developer | sonnet | lead | — | done | high |
| wave-2 | U-002 | AGP Stage S2: Bump AGP to 8.x, Gradle wrapper to 8.x, and pin JDK 17 toolchain | developer | sonnet | lead | U-001 | done | critical |
| wave-3 | U-005 | Upgrade test toolchain (Robolectric 4.12.x, JUnit 4.13.2, Mockito-core 5.x, Truth 1.4.x) | developer | sonnet | lead | U-002, U-004 | done | high |
| wave-4 | U-003 | Stage S3: raise targetSdk to 35, co-land conformance fixes, and remove JCenter | developer | sonnet | lead | U-002, U-005 | done | critical |
| wave-4 | U-006 | Characterization tests: AuthPreferences.migrate(), BackupJobs retry/constraints, notification assertion | developer | sonnet | lead | U-004, U-005 | done | high |
| wave-5 | U-007 | Rewrite AuthPreferences.migrate() to eliminate silent trust-all write and clear stale downgrade | developer | sonnet | lead | U-006 | done | critical |
| wave-5 | U-013 | Introduce BackupScheduler port and LegacyScheduler adapter (branch-by-abstraction seam) | developer | sonnet | lead | U-006 | done | high |
| wave-5 | U-018 | Dead-Code Removal and minSdk Cleanup: delete CalendarAccessorPre40, collapse sub-21 guards | developer | sonnet | lead | U-006, U-003 | done | medium |
| wave-5 | U-019 | SyncStateRepository Facade: define interface + SyncEvent sealed class and Otto-delegating impl | developer | sonnet | lead | U-003 | done | high |
| wave-5 | U-028 | Upgrade Play Billing 2.1.0 → 7.x: replace SkuDetails pipeline with ProductDetails/queryProductDetailsAsync | developer | sonnet | lead | U-003 | done | low |
| wave-5 | U-029 | Replace deprecated GData contacts call with ContactsPort + People API adapter | developer | sonnet | lead | U-003 | done | low |
| wave-6 | U-008 | Introduce TlsTrustPolicy enum, PinnedCertificateSocketFactory, PinnedCertStore, rewrite factory selection | developer | sonnet | lead | U-007, U-003 | done | critical |
| wave-6 | U-011 | Introduce SecretStore port, EncryptedPrefsSecretStore adapter, InMemorySecretStore fake | developer | sonnet | lead | U-006, U-007 | done | high |
| wave-6 | U-014 | WorkManagerScheduler: production BackupScheduler adapter with REPLACE semantics and backoff | developer | sonnet | lead | U-013, U-003 | done | high |
| wave-6 | U-025 | Define MailTransport port, app-owned ACL types, and reshape BackupImapStore into K9MailTransport adapter | developer | sonnet | lead | U-007 | done | medium |
| wave-7 | U-009 | TLS enrollment UI: pin-cert preference action, one-time security notice, and string cleanup | developer | sonnet | lead | U-008 | done | high |
| wave-7 | U-012 | Idempotent plaintext-to-encrypted credential migration on first launch after upgrade | developer | sonnet | lead | U-011 | done | high |
| wave-7 | U-015 | CoroutineWorker rewrite: extract backup/restore use-cases and replace BackupTask/RestoreTask | developer | sonnet | lead | U-014 | done | high |
| wave-7 | U-027 | Unpin k-9 mail library from JitPack SHA to a reproducible coordinate | developer | sonnet | lead | U-025 | done | medium |
| wave-8 | U-010 | Delete AllTrustedSocketFactory, audit every trust write site, and declare Gate G1 | developer | sonnet | lead | U-009 | done | high |
| wave-8 | U-016 | Durable Restore Checkpoint — Resumable Restore Worker with Fault-Injection Verification | developer | sonnet | lead | U-015 | done | high |
| wave-8 | U-020 | Otto removal: swap facade to MutableStateFlow/MutableSharedFlow, migrate 28 handler sites, delete App.bus | developer | sonnet | lead | U-019, U-015 | done | high |
| wave-9 | U-017 | Flip production binding LegacyScheduler→WorkManagerScheduler; delete legacy scheduler code; declare Gate G3 | developer | sonnet | lead | U-015, U-016 | planned | high |
| wave-9 | U-021 | Introduce MainViewModel and decompose MainActivity: migrate 9 @Subscribe handlers to Flow collection | developer | sonnet | lead | U-020 | done | high |
| wave-9 | U-022 | Hilt bootstrap: @HiltAndroidApp on App, Gradle plugin, kapt/ksp, and all SingletonComponent @Module classes | developer | sonnet | lead | U-020, U-003 | planned | medium |
| wave-10 | U-023 | Annotate @Inject constructors on eight BackupTask collaborators, remove manual wiring, migrate tests | developer | sonnet | lead | U-022 | planned | medium |
| wave-10 | U-026 | Engine Rewire to MailTransport: wire BackupTask/RestoreTask/ServiceBase to MailTransport port | developer | sonnet | lead | U-025, U-022 | planned | medium |
| wave-11 | U-024 | Wire HiltWorkerFactory into WorkManager Configuration.Provider; annotate BackupWorker/RestoreWorker | developer | sonnet | lead | U-023, U-015 | planned | medium |

## Wave Summary

| Wave | Stories | Can Parallel? | Gate |
|------|---------|---------------|------|
| wave-1 | U-001, U-004 | Yes | wave-1-gate |
| wave-2 | U-002 | No | wave-2-gate |
| wave-3 | U-005 | No | wave-3-gate |
| wave-4 | U-003, U-006 | Yes | wave-4-gate |
| wave-5 | U-007, U-013, U-018, U-019, U-028, U-029 | Yes | wave-5-gate |
| wave-6 | U-008, U-011, U-014, U-025 | Yes | wave-6-gate |
| wave-7 | U-009, U-012, U-015, U-027 | Yes | wave-7-gate |
| wave-8 | U-010, U-016, U-020 | Yes | wave-8-gate |
| wave-9 | U-017, U-021, U-022 | Yes | wave-9-gate |
| wave-10 | U-023, U-026 | Yes | wave-10-gate |
| wave-11 | U-024 | No | wave-11-gate |

## Capacity and Sequencing Notes

### Team

- **Team size:** Single maintainer (lead). All stories are unassigned to a named individual — they are owned by the lead.
- **Agent/model:** Every story executes via `developer` agent using `sonnet` model.

### Sprint Framing

This is not a single two-week sprint. Sprint-001 represents a **full-roadmap execution map** for the entire SMS Backup+ modernization initiative. With 29 stories spanning build toolchain, security hardening, scheduler migration, event-bus removal, dependency injection, mail transport decoupling, and strategic library upgrades, the realistic wall-clock duration under a single-maintainer model is approximately **14–18 weeks**, assuming focused execution on one wave at a time and typical code review / integration turnaround.

### Epic Grouping

| Epic | Stories | Theme |
|------|---------|-------|
| EPIC-001 Phase-0 | U-001 .. U-010 | Build toolchain, CI pipeline, TLS hardening |
| EPIC-002 Substrate Core | U-011 .. U-018 | Encrypted credentials, WorkManager scheduler, CoroutineWorker |
| EPIC-003 Engine | U-019 .. U-027 | Otto removal, Hilt DI, MailTransport port, k-9 unpin |
| EPIC-004 Strategic | U-028, U-029 | Play Billing v7, People API contacts adapter |

### Dependency Highlights

- **U-001 is the root of the build chain**: U-002 → U-003 (and U-005) depend on it sequentially. No other work can begin until the AGP 7.4 baseline is stable.
- **U-004 is independently parallelizable** with U-001 in wave-1: CI scaffolding has no build-toolchain dependency.
- **U-003 is a convergence point**: it is a prerequisite for U-008, U-014, U-018, U-019, U-022, U-028, and U-029 — delays here cascade broadly.
- **U-006 characterization tests** gate all security and scheduler work (U-007, U-011, U-013, U-018) in wave-5.
- **U-015 CoroutineWorker rewrite** is the most complex story (8 pts, high complexity) and gates U-016, U-017, U-020, and U-024.
- **U-020 Otto removal** (8 pts, high complexity, 28 handler sites across 12 files) gates the ViewModel and Hilt bootstrap work in wave-9.
- **Contract gates**: U-003 (G0), U-010 (G1), and U-014/U-017 (G3) declare explicit integration milestones that must pass before downstream waves proceed.

### Risk Notes

- The JCenter removal in U-003 may expose undocumented transitive dependencies; budget additional time for dependency resolution.
- U-015 and U-020 are the two highest-risk stories: both carry 8-point estimates and touch large cross-cutting surfaces. Consider breaking them into sub-tasks during story execution.
- U-027 (k-9 unpin) depends on the k-9 library being available at a stable Maven coordinate or requires vendoring — the outcome determines whether downstream build times change significantly.
