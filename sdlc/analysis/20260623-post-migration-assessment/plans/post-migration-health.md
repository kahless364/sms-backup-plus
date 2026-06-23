---
title: "Post-Migration Health Assessment"
plan_type: assessment-execution
version: "1.0.0"
assessment_type: modernization
tags: ["modernization", "tech-debt"]
---

# Post-Migration Health Assessment Plan

## Overview
- **Purpose**: Score the modernized SMS Backup+ app across four dimensions and produce a prioritized improvement backlog to inform future sprint planning.
- **Audience**: Maintainer / technical lead.
- **Decision informed**: Which remaining tech-debt / risk items to schedule next, in what order.
- **Subject**: `com.zegoggles.smssync` Android app (post-modernization: AGP 8.7.3, Gradle 8.14.1, JDK 17, targetSdk 35, Kotlin 1.9.25, Hilt, WorkManager/CoroutineWorker, EncryptedSharedPreferences, vendored `:k9mail-vendored`). 672 unit tests; jacoco per-package LINE ≥70% gate. BUG-001..015 all resolved.

## Prerequisites
- [ ] Source code access confirmed (single repo at project root).
- [ ] Build toolchain documented in `.claude/CLAUDE.md` (Local Build & Device Notes).

## Quality Standards
Apply the Expert-Exceeding Depth doctrine to every step. Verify against the 8 Self-Check Criteria before finalizing. See `knowledge/standards/expert-exceeding-depth.md`. **Every score MUST cite `file:line` evidence read from the actual current code — do NOT trust prior summaries, retros, or this plan's framing.** Each evaluative step emits a 0–100 score and an A–F grade with a one-sentence justification.

## Scoring Rubric (applied per dimension)
Each dimension is scored 0–100 and graded A–F against:
- **Correctness/robustness** (does it work, are invariants enforced)
- **Modernity/currency** (current, supported, forward-compatible)
- **Maintainability** (clarity, coupling, completeness of the migration)
- **Risk exposure** (security, data-integrity, breakage surface)
Weights for the overall score (Phase 2): Security & Data Integrity 0.30, Architecture & Code Quality 0.30, Testing & Coverage 0.25, Build & Toolchain 0.15.

## Phase 1: Dimension Assessment

### Step 1.1: Build & Toolchain
**Agent**: Legacy Analyst
**Parallel Group**: dimensions
**Output Format**: findings
**Output**: `outputs/sms-backup-plus/build-toolchain.md`
**Instructions** (inline): Read `app/build.gradle`, root `build.gradle`, `settings.gradle`, `gradle/wrapper/gradle-wrapper.properties`, `gradle.properties`, `gradle/verification-metadata.xml`, `k9mail-vendored/build.gradle`, `.github/workflows/`. Assess: toolchain currency & support status (AGP/Gradle/JDK/Kotlin/SDK levels); dependency verification & supply-chain posture; the vendored k-9 module's freshness (commit vintage, transitive lib versions, `org.apache.http.legacy`); forward-compat hazards (Kotlin 1.9→2.x/kapt→KSP, JVM target 8, AGP-8 `nonTransitiveRClass`/`nonFinalResIds`/`enableJetifier`, removed-in-Gradle-9 deprecations, alpha deps like `security-crypto`); repo hygiene (jcenter/jitpack); CI coverage/verification enforcement. Emit score (0–100), grade (A–F), strengths, weaknesses (severity-tagged, `file:line`), and concrete improvement items.
**Completion criteria**: A scored findings doc citing real versions/paths, with a discrete list of improvement items each tagged severity + rough effort.

### Step 1.2: Architecture & Code Quality
**Agent**: Solution Architect
**Parallel Group**: dimensions
**Output Format**: findings
**Output**: `outputs/sms-backup-plus/architecture.md`
**Instructions** (inline): Read the DI modules (`app/src/main/java/com/zegoggles/smssync/di/`), `App.java`, the workers (`service/BackupWorker.kt`, `RestoreWorker.kt`, schedulers), the MailTransport ACL (`mail/transport/`), the vendored boundary, `SyncStateRepository`/Flow, `activity/MainActivity.java` + `MainViewModel`. Assess: Hilt DI completeness (any `@Provides`-returning-`new` / static-singleton aliasing / `TODO(U-022/U-023)`); worker/coroutine correctness (cancellation, cleanup, watermark write-ordering); ACL boundary integrity (no `com.fsck.k9.*` in `service.*`); the retained legacy Service dual-dispatch layer (`SmsBackupService`/`SmsRestoreService`/`ServiceBase`) — size, necessity, complexity; god-class/decomposition state; hand-built per-run object graphs in workers; dead seams/stale docs. Emit score, grade, strengths, weaknesses (severity-tagged, `file:line`), improvement items.
**Completion criteria**: Scored findings doc; architectural debt items enumerated with severity + effort.

### Step 1.3: Testing & Coverage
**Agent**: QA Analyst
**Parallel Group**: dimensions
**Output Format**: findings
**Output**: `outputs/sms-backup-plus/testing.md`
**Instructions** (inline): Determine the authoritative `@Test` count (`git grep -h "@Test" HEAD -- 'app/src/test/**/*.java' 'app/src/test/**/*.kt' | grep -c "@Test"`). Survey test distribution by package under `app/src/test/`. Read the jacoco config in `app/build.gradle` (`jacocoTestCoverageVerification`, `jacocoFileFilter`): record the threshold, which packages are GATED, and which classes/packages are EXCLUDED — explicitly flag any high-risk excluded code (e.g. `K9MailTransport$BackupImapStoreDelegate*`, `BackupWorker*`, `RestoreWorker*`) and any ungated packages. Spot-check whether key tests assert real production behavior vs. re-implementations (e.g. `BackupWorkerWatermarkTest`). Identify structural gaps (no `androidTest`/instrumentation, no IMAP integration test, on-device-only paths). Emit score, grade, strengths, weaknesses (severity-tagged), improvement items. **Flag coverage exclusions that hide untested code as a first-class risk.**
**Completion criteria**: Scored findings doc using the authoritative test count; exclusions enumerated; improvement items with severity + effort.

### Step 1.4: Security & Data Integrity
**Agent**: Security Reviewer
**Parallel Group**: dimensions
**Output Format**: findings
**Output**: `outputs/sms-backup-plus/security.md`
**Instructions** (inline): Read `preferences/EncryptedPrefsSecretStore.java` (encryption at rest, plaintext→encrypted migration, `ENCRYPTION_DEGRADED` flag — and whether `isEncryptionDegraded()` is actually enforced/surfaced anywhere), `mail/transport/` + vendored `mail/ssl/` (TLS trust manager, hostname verification, cert pinning, SNI), `activity/auth/` (OAuth token handling, no token in Intent extras), `AndroidManifest.xml` (permissions, exported components, `BackupBroadcastReceiver`, allowBackup/backup rules, networkSecurityConfig), and `service/BackupWorker.kt` (watermark cannot advance on failed upload). Confirm the BUG-007/008/009/010/015 fixes hold and surface any NEW issues. Emit score, grade, strengths, weaknesses (severity-tagged critical/high/med/low, `file:line`), improvement items.
**Completion criteria**: Scored findings doc; any residual/new security or data-integrity risks enumerated with severity + effort; explicit confirmation (or refutation) that prior bug fixes hold.

## Phase 2: Synthesis & Prioritized Backlog

### Step 2.1: Synthesis & Prioritized Improvement Backlog
**Agent**: Solution Architect
**Parallel Group**: synthesis
**Depends On**: Step 1.1, 1.2, 1.3, 1.4
**Output Format**: findings
**Output**: `outputs/sms-backup-plus/post-migration-health-report.md`
**Instructions** (inline): Read the four dimension findings docs. Compute the weighted overall score (weights in the rubric above) and an overall A–F grade with justification. Produce a **prioritized improvement backlog**: a single ranked table where each row has `Rank | Item | Dimension | Severity | Effort (S/M/L) | Impact | Suggested action/sprint`. Rank by (impact × severity ÷ effort), and propose a sensible sprint sequencing (which items to batch). De-duplicate items that span dimensions. Include a short executive summary (3–5 sentences) stating the headline verdict and the top 3 things to do next. Cite the per-dimension scores. Do not introduce findings not supported by the dimension docs.
**Completion criteria**: A single report doc with: overall weighted score + grade, executive summary, the ranked backlog table, and the per-dimension score breakdown. Every backlog item traceable to a dimension finding.

## Deliverables
- `outputs/sms-backup-plus/build-toolchain.md` — scored Build & Toolchain findings
- `outputs/sms-backup-plus/architecture.md` — scored Architecture & Code Quality findings
- `outputs/sms-backup-plus/testing.md` — scored Testing & Coverage findings
- `outputs/sms-backup-plus/security.md` — scored Security & Data Integrity findings
- `outputs/sms-backup-plus/post-migration-health-report.md` — overall grade + prioritized improvement backlog (primary deliverable)
