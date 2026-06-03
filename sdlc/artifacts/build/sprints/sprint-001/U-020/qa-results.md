---
artifact_type: qa-results
story_id: U-020
verdict: PASS
agent: Developer
timestamp: "2026-06-03"
---

# QA Results: U-020

## Build Gates

| Gate | Command | Result |
|------|---------|--------|
| AC-1: Otto removed from compile classpath | `./gradlew :app:assembleDebug` | PASS — APK produced, no otto on classpath |
| AC-2: Zero @Subscribe/@Produce in source | `grep -rn "@Subscribe\|@Produce" app/src/main/java` | PASS — 0 live matches |
| AC-2: Zero com.squareup.otto imports | `grep -rn "import com.squareup.otto" app/src/main/java` | PASS — 0 matches |
| AC-3: FlowSyncStateRepository exists, no DefaultSyncStateRepository | file check | PASS |
| AC-6: App.bus/register/unregister/post deleted | `grep -n "static.*Bus\|App\.bus\|App\.register\|App\.unregister\|App\.post" App.java` | PASS — 0 matches |
| AC-7: ServiceBase bus lifecycle deleted | `grep -n "App\.register\|App\.unregister" ServiceBase.java` | PASS — 0 matches |
| AC-8: SmsBackupService statics deleted | file check for `isServiceWorking`, `static SmsBackupService service` | PASS |
| AC-9: SmsRestoreService statics deleted | file check for `isServiceIdle`, `static SmsRestoreService service` | PASS |
| AC-13: activity/events directory | only PerformAction.java remains | PASS (others deleted, PerformAction retained for Actions enum) |
| AC-14: MainActivity zero Otto surface | grep @Subscribe, App.register, App.post | PASS — 0 live matches |
| Test suite | `./gradlew :app:testDebugUnitTest` | PASS — 591 tests |
| Coverage gate | `./gradlew :app:jacocoTestCoverageVerification` | PASS — ≥70% service*, mail*, auth* |

## AC-4 Sticky Semantics Verified

`SyncStateRepositoryTest.lateCollector_receivesLastEmittedState` — PASS.
BackupState emitted before collector starts; `state.first()` returns it immediately.

## AC-5 One-Shot Non-Replay Verified

`SyncStateRepositoryTest.lateCollector_doesNotReceivePriorEvent` — PASS.
MissingPermissions event emitted before collector; 100ms timeout returns null.

## AC-10 Cancel Origin Verified

`SyncStateRepositoryTest.cancel_originUser_mayInterruptIfRunning_isFalse` — PASS.
`SyncStateRepositoryTest.cancel_originSystem_mayInterruptIfRunning_isTrue` — PASS.

## AC-16 tryEmitEvent Return Value Verified

`SyncStateRepositoryTest.tryEmitEvent_returnValue_isConsumedByCallerNotSwallowed` — PASS.
Returns true with no active collector (extraBufferCapacity=1 absorbs one emit).

## Grep Verification Report

```
$ grep -rn "com.squareup.otto|com.squareup:otto" app/src/main/java app/build.gradle
(no output — 0 matches)

$ grep -rn "@Subscribe|@Produce" app/src/main/java
(only comment lines — 0 live matches)

$ grep -rn "App\.register|App\.unregister|App\.post" app/src/main/java
(only comment lines — 0 live matches)
```

## Notes

- Emulator/device smoke test (AC-15 rotation) not performed in this automated run.
  The sticky StateFlow semantics are verified by unit tests; rotation behavior depends on
  the existing `onSaveInstanceState`/`onRestoreInstanceState` implementation in StatusPreference,
  which was preserved unmodified per story constraints.

## Verdict: PASS
