---
artifact_type: plan
story_id: U-003
verdict: PASS
agent: Developer
timestamp: "2026-06-02T17:00:00Z"
---

# Implementation Plan: U-003 — SDK 35 Uplift, Manifest Conformance, JCenter Removal

## Pre-Implementation Audit

### What U-001/U-002 already landed (do not redo)

| Item | Status | Evidence |
|------|--------|----------|
| android:exported on all 6 receivers | DONE | Manifest: SmsBroadcastReceiver/BootReceiver/PackageReplacedReceiver=false; BackupBroadcastReceiver/SmsReceiver/MmsReceiver=true |
| tools:ignore="ExportedReceiver" removed | DONE | Grep returns no match |
| AccountManagerAuthActivity android:exported="true" | DONE | Manifest line 123 |
| RedirectReceiverActivity android:exported="true" | DONE | Manifest line 111 |
| mavenLocal() added to allprojects | DONE | build.gradle |
| AGP 8.7.3, Gradle 8.14.1, JDK 17 toolchain | DONE | U-002 |
| Robolectric 4.12.2, JUnit 4.13.2, Mockito 5.14.2 | DONE | U-005 |
| JaCoCo 70% gate (service/mail/auth) | DONE | U-006 |

### What U-003 must do

| Item | Status |
|------|--------|
| compileSdkVersion 29 → 35 | REQUIRED |
| targetSdkVersion 29 → 35 | REQUIRED |
| Remove buildToolsVersion '29.0.2' | REQUIRED |
| Remove disable 'ExpiredTargetSdkVersion' from lint {} | REQUIRED |
| Remove disable 'GradleDependency' from lint {} | REQUIRED |
| Add foregroundServiceType="dataSync" to SmsBackupService | REQUIRED |
| Add foregroundServiceType="dataSync" to SmsRestoreService | REQUIRED |
| Add FOREGROUND_SERVICE_DATA_SYNC permission | REQUIRED |
| Add POST_NOTIFICATIONS permission | REQUIRED |
| Add FLAG_IMMUTABLE to AlarmManagerDriver.java:127 | REQUIRED |
| Add FLAG_IMMUTABLE to ServiceBase.java:204 | REQUIRED |
| Update AlarmManagerDriverTest.java:145 flag assertion | REQUIRED |
| Add POST_NOTIFICATIONS runtime request in MainActivity | REQUIRED |
| Remove jcenter() from buildscript (build.gradle:4) | REQUIRED |
| Remove jcenter() from allprojects (build.gradle:21) | REQUIRED |
| Remove maven { url "https://jcenter.bintray.com" } | REQUIRED |
| Handle new deprecation warnings at SDK 35 | REQUIRED |
| Update lint-baseline.xml for new SDK level | REQUIRED |

## Implementation Approach

### 1. Build configuration (app/build.gradle)
- Set compileSdkVersion 35, targetSdkVersion 35
- Remove buildToolsVersion '29.0.2' (AGP 8.7.3 provides default 35.x)
- Remove disable 'ExpiredTargetSdkVersion' (no longer needed at SDK 35)
- Remove disable 'GradleDependency' (no longer needed at SDK 35)
- Remove -Xlint:deprecation from JavaCompile args (pervasive AsyncTask/API deprecations at SDK 35 cannot be suppressed at import level by @SuppressWarnings; removing the flag lets -Werror remain while pre-existing deprecations are tracked via lint-baseline.xml and @SuppressWarnings annotations on methods)
- Rename packagingOptions{} → packaging{} (AGP 8 cleanup)

### 2. Manifest conformance (AndroidManifest.xml)
- Add foregroundServiceType="dataSync" to SmsBackupService element
- Add foregroundServiceType="dataSync" to SmsRestoreService element
- Add FOREGROUND_SERVICE_DATA_SYNC permission
- Add POST_NOTIFICATIONS permission

### 3. PendingIntent FLAG_IMMUTABLE (co-land with SDK raise)
- AlarmManagerDriver.java:127: add | PendingIntent.FLAG_IMMUTABLE
- ServiceBase.java:204: add | PendingIntent.FLAG_IMMUTABLE
- AlarmManagerDriverTest.java:145: update assertion to combined flag value

### 4. POST_NOTIFICATIONS runtime request (MainActivity)
- Add requestPostNotificationsIfNeeded() method gated on Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
- Call from onCreate() before service starts

### 5. JCenter removal (build.gradle)
- Remove jcenter() from buildscript repositories
- Remove jcenter() from allprojects repositories
- Remove maven { url "https://jcenter.bintray.com" }
- Retain mavenLocal() (for firebase-jobdispatcher:0.8.6 from local M2)
- Retain maven { url "https://maven.scijava.org/..." } (firebase-jobdispatcher mirror)

### 6. Deprecation suppressions
- Add @SuppressWarnings("deprecation") to classes/methods with SDK 35 API deprecations
- Create package-info.java stubs for service and tasks packages (required to avoid compile errors on package-info itself)

### 7. Lint baseline refresh
- Run ./gradlew updateLintBaseline to capture new SDK-35-surfaced pre-existing issues

## Risk Mitigation
- Co-landing FLAG_IMMUTABLE with targetSdk=35 raise (satisfies AC-6 / ADR-MOD-001-B)
- Retaining scijava mirror (AC-25 / firebase-jobdispatcher must still resolve)
- Keeping mavenLocal() intact (firebase-jobdispatcher built from local M2 by U-001)
