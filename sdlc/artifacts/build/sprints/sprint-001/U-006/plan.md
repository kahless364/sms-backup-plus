---
artifact_type: plan
story_id: "U-006"
verdict: PASS
agent: "Developer"
timestamp: "2026-06-02"
---

# Plan: U-006 — Fix JaCoCo Package Glob + Backfill Coverage to 70%

## Problem Statement

Two distinct bugs must be fixed:

1. **FALSE GREEN JaCoCo gate**: The `jacocoTestCoverageVerification` task used `element = 'PACKAGE'` with `includes` patterns in slash notation (`com/zegoggles/smssync/service/**`). JaCoCo PACKAGE element requires DOT notation for package names. The slash patterns matched nothing, so the gate silently passed regardless of coverage. Pre-fix coverage was service=65.1%, mail=67.4%, auth=35.5% — all below 70%, yet gate reported PASS.

2. **Coverage below 70%**: Even with a working gate, all three gated packages were below the 70% threshold. New characterization tests must bring each to ≥70%.

## Implementation Approach

### Step 1: Fix the glob (app/build.gradle)

Change `includes` patterns from slash notation to dot notation:
- `'com/zegoggles/smssync/service/**'` → `'com.zegoggles.smssync.service*'`
- `'com/zegoggles/smssync/mail/**'` → `'com.zegoggles.smssync.mail*'`
- `'com/zegoggles/smssync/auth/**'` → `'com.zegoggles.smssync.auth*'`

The `*` suffix (without trailing slash) matches the root package itself AND any sub-packages, because JaCoCo PACKAGE element evaluates each package node independently.

### Step 2: Verify gate now fails (pre-backfill)

Confirm `jacocoTestCoverageVerification` fails with violations naming each package at its real coverage below 70%.

### Step 3: Add mandatory story ACs (AC-1 through AC-8)

Per story ACs:
- AC-1/2/3: Three `AuthPreferences.migrate()` tests in `AuthPreferencesTest.java`
- AC-4/5: Two `BackupJobs` retry strategy tests
- AC-6/7: Two `BackupJobs` network constraint tests
- AC-8: Fix `assertNotificationShown` helper in `SmsBackupServiceTest.java`

### Step 4: Backfill additional coverage

Add targeted characterization tests for the gap packages:
- **auth/**: OAuth2Client, TokenRefresher, FeedHandler, OAuth2Token
- **service/**: AlarmManagerDriver, CancelEvent, SmsJobService, SmsRestoreService, BackupConfig, RestoreConfig, BackupCursors
- **service.exception/**: SmsProviderNotWritableException, MissingPermissionException
- **service.state/**: RestoreState, BackupState
- **mail/**: BackupStoreConfig

## AC-1 Handling Decision

AC-1 (`migrate_legacySslTls_doesNotEnableTrustAll`) is authored RED — it asserts behavior that `migrate()` does NOT currently implement (migrate() sets trust_all=true at line 284). It is annotated `@Ignore` per Technical Notes option (a): the `@Ignore` annotation is the living documentation of the pending U-007 work. The test is authored with the exact comment block required by the story, and remains `@Ignore`d until U-007 rewrites `migrate()`.

## Verification Plan

1. Run `jacocoTestCoverageVerification` — must FAIL before backfill (proves gate is real)
2. Add tests, run full suite — must stay GREEN
3. Run `jacocoTestReport` — inspect per-package LINE coverage XML
4. Run `jacocoTestCoverageVerification` — must PASS after backfill
