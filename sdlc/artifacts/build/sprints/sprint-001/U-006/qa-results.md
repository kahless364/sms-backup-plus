---
artifact_type: qa-results
story_id: "U-006"
verdict: PASS
agent: "Developer"
timestamp: "2026-06-02"
---

# QA Results: U-006

## Coverage: Before vs After

### BEFORE Fix (baseline from U-005, pre-U-006 changes)

| Package | Covered/Total | Coverage | Gate (≥70%) |
|---------|--------------|----------|-------------|
| com.zegoggles.smssync.service | 624/959 | 65.1% | FAIL |
| com.zegoggles.smssync.service.state | 63/92 | 68.5% | FAIL |
| com.zegoggles.smssync.service.exception | 11/16 | 68.8% | FAIL |
| com.zegoggles.smssync.mail | 466/691 | 67.4% | FAIL |
| com.zegoggles.smssync.auth | 65/183 | 35.5% | FAIL |

**Gate result BEFORE fix**: BUILD SUCCESSFUL (FALSE GREEN — gate silently passed because slash-format glob matched no packages)

### AFTER Fix (post-U-006 glob fix + backfill)

| Package | Covered/Total | Coverage | Gate (≥70%) |
|---------|--------------|----------|-------------|
| com.zegoggles.smssync.service | 675/959 | 70.4% | PASS |
| com.zegoggles.smssync.service.state | 92/92 | 100.0% | PASS |
| com.zegoggles.smssync.service.exception | 16/16 | 100.0% | PASS |
| com.zegoggles.smssync.mail | 485/691 | 70.2% | PASS |
| com.zegoggles.smssync.auth | 138/183 | 75.4% | PASS |

**Gate result AFTER fix**: BUILD SUCCESSFUL (real green — gate actually checks coverage)

## Gate Enforcement Proof

### Step 1: Confirmed FALSE GREEN with broken glob (BEFORE fix)
```
$ ./gradlew :app:jacocoTestCoverageVerification
BUILD SUCCESSFUL
```
Despite service=65.1%, mail=67.4%, auth=35.5% — all below 70%.

### Step 2: Confirmed gate FAILS with correct dot notation (AFTER glob fix, BEFORE backfill)
```
$ ./gradlew :app:jacocoTestCoverageVerification --rerun-tasks
[ant:jacocoReport] Rule violated for package com.zegoggles.smssync.service.exception: lines covered ratio is 0.68, but expected minimum is 0.70
[ant:jacocoReport] Rule violated for package com.zegoggles.smssync.mail: lines covered ratio is 0.67, but expected minimum is 0.70
[ant:jacocoReport] Rule violated for package com.zegoggles.smssync.service: lines covered ratio is 0.65, but expected minimum is 0.70
[ant:jacocoReport] Rule violated for package com.zegoggles.smssync.service.state: lines covered ratio is 0.68, but expected minimum is 0.70
[ant:jacocoReport] Rule violated for package com.zegoggles.smssync.auth: lines covered ratio is 0.35, but expected minimum is 0.70
> Task :app:jacocoTestCoverageVerification FAILED
BUILD FAILED
```
This confirms the gate is now REAL and would have caught the original coverage gaps.

### Step 3: Confirmed gate PASSES after backfill
```
$ ./gradlew :app:jacocoTestCoverageVerification
BUILD SUCCESSFUL
```
All packages at or above 70%.

## Test Suite Status

| Metric | Before U-006 | After U-006 | Delta |
|--------|-------------|-------------|-------|
| Tests passing | 216 | ~305 | +89 |
| Tests skipped | 1 | 2 | +1 (AC-1 @Ignored) |
| Tests failing | 0 | 0 | 0 |

**Note**: The 2nd skip is AC-1 (`migrate_legacySslTls_doesNotEnableTrustAll`) which is intentionally @Ignored. It is authored-red against current code and will be made executable (remove @Ignore) in U-007 when `migrate()` is rewritten.

## AC Verification

| AC | Description | Status | Evidence |
|----|-------------|--------|----------|
| AC-1 | migrate() legacy SSL/TLS authored RED (present + @Ignored) | PASS | `AuthPreferencesTest.migrate_legacySslTls_doesNotEnableTrustAll` present with @Ignore + required comment block |
| AC-2 | migrate() clean state authored GREEN | PASS | `migrate_cleanState_doesNotEnableTrustAll` passes |
| AC-3 | migrate() explicit trust-all preserved GREEN | PASS | `migrate_explicitTrustAll_isPreserved` passes |
| AC-4 | BackupJobs retry base = 30s | PASS | `defaultRetryStrategy_hasBaseIntervalOf30Seconds` asserts `retryStrategy.getInitialBackoff() == 30` |
| AC-5 | BackupJobs retry max = 300s | PASS | `defaultRetryStrategy_hasMaxIntervalOf300Seconds` asserts `retryStrategy.getMaximumBackoff() == 300` |
| AC-6 | wifi-only → ON_UNMETERED_NETWORK | PASS | `scheduleRegular_wifiOnly_constraintIsUnmetered` stubs isWifiOnly()=true, asserts ON_UNMETERED, !ON_ANY |
| AC-7 | any-network → ON_ANY_NETWORK | PASS | `scheduleRegular_anyNetwork_constraintIsAny` stubs isWifiOnly()=false, asserts ON_ANY, !ON_UNMETERED |
| AC-8 | assertNotificationShown // TODO removed | PASS | No `// TODO` in SmsBackupServiceTest.java; replaced with documented blocking comment |
| AC-9 | Gate G2 real enforcement | PASS | Gate fails below 70% (verified), passes after backfill (verified) |

## Notes

The JaCoCo glob bug explanation: With `element = 'PACKAGE'`, JaCoCo represents package IDs using Java-style dot notation (`com.zegoggles.smssync.service`). The `includes` filter is a pattern matching against these dot-notation strings. The previous patterns used slash notation (`com/zegoggles/smssync/service/**`) which are class-file path format, not package name format. Since no package names contain slashes, the patterns matched nothing — producing a vacuously-true gate.

The fix uses `com.zegoggles.smssync.service*` (dot notation, trailing `*`) which:
- Matches `com.zegoggles.smssync.service` (root package)
- Matches `com.zegoggles.smssync.service.state` (sub-package)
- Matches `com.zegoggles.smssync.service.exception` (sub-package)
Each matched package is evaluated independently against the 70% threshold.
