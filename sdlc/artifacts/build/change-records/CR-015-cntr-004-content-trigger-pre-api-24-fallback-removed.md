---
status: implemented
severity: high
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-005
  - REQ-MODERNIZATION-015
design_docs:
  - DES-MODERNIZATION-005
  - DES-MODERNIZATION-014
related_stories: []
tags:
  - change-record
  - modernization
  - cntr-004
  - backup-scheduler
  - minsdk-24
  - applied-inline
id: CR-015
title: 'CNTR-MODERNIZATION-004 and DES-MODERNIZATION-005: the pre-API-24 content-trigger fallback is retired by minSdk 24 (a revision, not a behavioural break); INV-4 retargeted to API 24'
type: DCR
domain: change-records
summary: CNTR-MODERNIZATION-004 (backup scheduler) and DES-MODERNIZATION-005 still describe the content-trigger fallback for Android below 7.0 (API 24). With minSdk 24 (CR-011, implemented) the branch can never run and the minSdk story of design DES-MODERNIZATION-014 deletes it and retargets the test that asserted it. The contract lists losing that fallback as a behavioural break gated by its invariant tests. Design DES-MODERNIZATION-014 names this record as precondition P-1 of its first story; the orchestrator ruled it mandatory (decision appendix, Record 14 row 9).
updated_at: '2026-11-21T09:29:59.600Z'
---

# CR-015: The pre-API-24 content-trigger fallback is retired by minSdk 24

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-015 |
| **Type** | DCR (Design Correction), with one clarifying note on an approved requirement (edit R-1) |
| **Severity** | High |
| **Status** | Applied 2026-11-21 (edits C-0 to C-12, D-0 to D-9, R-1 and R-1c); frontmatter `status` is the record of state |
| **Reported By** | Requirements Analyst, on the Design Auditor's finding SF-7 and the orchestrator's ruling (Record 14, row 9) |
| **Date Reported** | 2026-11-21 |
| **Source** | Sprint work: design DES-MODERNIZATION-014 (approved 2026-11-21), decision D4 and its "Integration Contracts" section; precondition P-1 of its first story, S1 (minSdk 24) |

This record is **applied now**, inline, right after it is approved. It is a text amendment to two
approved artifacts (a contract and a design) and one dated note on an approved requirement. It is
**not a story to schedule.** The code branch and the test are changed later by story S1; this record
edits neither.

---

## Finding

### Description

Since CR-011 (applied 2026-11-21) the approved requirements set `minSdkVersion` 24
(REQ-MODERNIZATION-015 AC-5). The scheduler contract CNTR-MODERNIZATION-004 and its design
DES-MODERNIZATION-005 were written when the floor was API 14, and still describe a version split of the
content trigger: content-URI triggers on API 24 and above, "the broadcast fallback" on API 23 and
below. On the approved floor the second half describes a state no supported device can be in.

The split matters because the contract treats it as a guarded behaviour:

- The contract's Versioning section names "losing the content-trigger pre-24 fallback or its
  debounce" as a break of the behavioural contract that "MUST be gated by the INV-1..4 tests".
- DES-MODERNIZATION-005's Design Validation requires an "API ≤23" assertion for INV-4.
- Story S1 of DES-MODERNIZATION-014 deletes the `SDK_INT < VERSION_CODES.N` branch (D4, row S-7) and
  retargets the one test that runs below the floor, `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast`
  (D4, table T). Read literally, that is a behavioural break with its gate removed.

So S1 cannot be planned against the contract as written. Someone has to rule, in writing, whether the
removal is a break, and the text has to say so before S1 is planned (precondition P-1).

### Source of Truth

- **Requirements after CR-011:** REQ-MODERNIZATION-015 AC-5 to AC-7 (minSdk 24; AC-6 names the
  `SDK_INT < VERSION_CODES.N` branch of `scheduleContentTrigger` as a site to delete; AC-7 names the SDK 23
  test in `WorkManagerSchedulerTest` for retargeting or deletion).
- **Approved design DES-MODERNIZATION-014:** decision D4 (table S row S-7, table T), and the
  "Integration Contracts" section, which writes the argument this record rules on.
- **Code:** `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt`, the receiver
  `app/src/main/java/com/zegoggles/smssync/receiver/SmsBroadcastReceiver.java`, `App.java`, the manifest.
- **Decision record:** Record 14, row 9 (the three mandatory change records P-1 to P-3), in the appendix
  "Product owner decision" of
  `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`;
  the product owner's delegation is Record 12.

### Evidence

All lines opened 2026-11-21 on the working tree. Quotes are exact.

**1. The contract, `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` (read in full).** Every statement about the content trigger and the API 24 condition:

| Line | Exact current text (shortened only where marked "...") |
|------|---------------------------------------------------------|
| 10-11 | `change_records:` `  - CR-001` |
| 38 | "...version-branches the content-URI trigger (`SDK_INT >= 24`); re-imposes the 300s backoff cap at the worker..." (producer row) |
| 62-65 | "`scheduleContentTrigger(): Unit` // recurring content-URI observer over SMS provider (+ call-log when isCallLogBackupAfterCallEnabled()); API 24+ only; pre-24 falls back to the broadcast→scheduleIncoming() path. (BackupJobs.java:82)" |
| 170 | "`contentUriTriggers: List<ContentUriTrigger>?  // content-trigger job only; API 24+`" |
| 240 | The INV-4 row: "**INV-4 — content-URI trigger (+ pre-24 fallback)** \| An SMS (and, when call-log-after-call is enabled, call-log) content change MUST trigger backup. On API 24+ via content-URI triggers; on API ≤23 via the **broadcast fallback** (`scheduleIncoming()`). The trigger MUST preserve the **two-stage debounce**: ..." and, in the realization cell, "...API ≤23: SMS-received broadcast path calls `scheduler.scheduleIncoming()` → delayed one-off. The `SDK_INT >= 24` branch lives **inside the adapter**, so the port surface is version-agnostic." |
| 246 | "**Current version:** v2 (amended in place by DES-MODERNIZATION-012 / REQ-MODERNIZATION-013; v1 introduced by MU-005 / DES-MODERNIZATION-005). Amended in place — the contract has not been implemented against the v2 surface yet, so no consumer migration is required; `status` remains `approved`." |
| 248-256 | Changelog: v2 (2026-06-04) and v1. Integer versions only |
| 260 | "**Breaking change policy.** ... Breaking for the **behavioral contract** = any change to the four invariants (different `ExistingPolicy`, a network constraint on `BROADCAST_INTENT`, a backoff value other than `EXPONENTIAL`/30s/300s, or losing the content-trigger pre-24 fallback or its debounce). Behavioral breaks are the higher-severity class: they can silently change scheduling semantics without a compile error and MUST be gated by the INV-1..4 tests (DES-005 §Design Validation)." |
| 261 | "**Backward compatibility (non-breaking).** ... Adapter-internal changes (the `SDK_INT >= 24` branch, the worker-side 300s cap mechanism) are non-breaking." |
| 271 | Validation Rule 5: "The content-URI-triggered worker MUST NOT back up on the raw content change; it MUST enqueue the delayed incoming-backup ..." (no pre-24 text; stays) |
| 363 | "**Example: content-URI trigger (API 24+) and its debounce follow-up**" |
| 381-383 | "// API <= 23 fallback (no content-URI triggers available):" / "// the SMS-received broadcast path calls scheduler.scheduleIncoming() ->" / "//   OneTimeWorkRequest(initialDelay = getIncomingTimeoutSecs())   // same debounce." |

Lines 261 and 260 disagree with each other: line 261 calls the `SDK_INT >= 24` branch an adapter-internal,
non-breaking matter, line 260 calls losing its pre-24 side a break.

**2. The design, `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` (frontmatter `change_records: []` at line 15):**

| Line | Exact current text |
|------|--------------------|
| 88 | "`scheduleContentTrigger()            // recurring content-URI observer (API 24+)`" |
| 238 | "\| AC-3 (d) content-URI trigger + pre-24 fallback \| `addContentUriTrigger` (24+) / delayed one-off (14–23) \|" |
| 281 | "...`Constraints.addContentUriTrigger(uri, triggerForDescendants=true)` per observed URI (API 24+); see fallback below \| INV-4(d)" |
| 317-333 | "The design preserves this two-stage debounce with a version split:" then "**API 24+ (preferred):** ..." and "**API 14–23 (the current floor up to pre-24):** content-URI triggers are unavailable. Retain the **existing broadcast fallback** — the SMS-received path calls `scheduler.scheduleIncoming()`, ... Mandated by AC-3(d) ("broadcast fallback below API 24") and required because the project floor is API 14 (`app/build.gradle:14`; raised to 21 later by MU-011/REQ-MODERNIZATION-006). The fallback applies to the whole pre-24 range regardless of whether that floor is 14 or the later 21." and "The version branch lives inside `WorkManagerScheduler.scheduleContentTrigger()` / `scheduleIncoming()` (an `SDK_INT >= 24` check), so the port surface is version-agnostic and callers are unaffected." |
| 387 | "\| **INV-4 content-URI trigger (AC-3d)** \| API 24+: assert `WorkSpec.constraints.contentUriTriggers` includes the SMS provider URI (and call-log URI when `isCallLogBackupAfterCallEnabled()`), `triggerForDescendants=true`. API ≤23: assert the broadcast fallback enqueues a delayed `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)`. \|" |
| 444 | "\| `scheduleContentTrigger()` \| recurring content-URI observer over SMS (+ call-log when enabled), API 24+ \| `PeriodicWorkRequest` + content-URI triggers \|" |
| 506 | `## Notes` (last section; no amendment note yet) |

Read and not edited (they describe WorkManager's own scheduler choice and the old floor as ADR history,
not the content trigger): line 121 (the driver table row "API 14–22") and lines 183-193 (ADR-005-A).

**3. The code (read, not edited):**

- `scheduler/WorkManagerScheduler.kt:219-258` `scheduleContentTrigger()`. Line 220-223: returns `null` when
  `secretStore.isEncryptionDegraded()`. **Lines 224-230: `if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)`
  logs "API < 24, using broadcast fallback (SmsBroadcastReceiver)" and `return null`.** Nothing is scheduled
  in that branch; it does not call `scheduleIncoming()` or touch the receiver. Lines 232-257, for API 24
  and above: a `PeriodicWorkRequest` of `BackupTriggerWorker` with `addContentUriTrigger(Consts.SMS_PROVIDER, true)`,
  plus `Consts.CALLLOG_PROVIDER` when the call-log type is enabled and `isCallLogBackupAfterCallEnabled()`,
  enqueued as unique periodic work `CONTENT_TRIGGER` (`UPDATE`). The only `Build` use in the file is line 20
  (import) and line 224.
- Other text about the split in code: KDoc `WorkManagerScheduler.kt:58-61` and `:208-210`, log text `:227-228`
  and `:256` ("(API 24+)"), `scheduler/BackupScheduler.java:74-77` (the port javadoc: "content-URI triggers are
  only available on API 24+ ... or null if the adapter does not support it"), `App.java:379-380` (comment:
  "content-URI triggers on API 24+").
- `worker/BackupTriggerWorker.kt:53-73`: stage 2 of the debounce. It enqueues
  `OneTimeWorkRequest` of `BackupWorker` with `setInitialDelay(incomingTimeoutSecs)` as **unique work
  `INCOMING`, `REPLACE`**. It does not back up directly.
- `receiver/SmsBroadcastReceiver.java:44-84` and `AndroidManifest.xml:146-153`: the receiver listens to
  `android.provider.Telephony.SMS_RECEIVED` (permission `BROADCAST_SMS`, `exported="false"`, `enabled="true"`).
  `onReceive` calls `incomingSMS` for that action only; `incomingSMS` calls `getScheduler(context).scheduleIncoming()`
  when `autoBackupEnabled && loginInformationSet && !firstBackup` (`:58-60`, `:66-84`). **No `SDK_INT` check
  anywhere in the receiver.** The receiver code has no level condition; whether the platform delivers the broadcast
  to a non-exported receiver is not verified.
- **What the receiver logs** (`SmsBroadcastReceiver.java:47-64, 86-91`): `onReceive` logs only through
  `Log.v` guarded by `LOCAL_LOGV` (`App.java:92`: `LOCAL_LOGV = DEBUG`), so on a non-debug build nothing is logged at
  receipt. When the message is accepted (`shouldSchedule` true) the receiver itself logs **nothing**; the only line is
  the scheduler's own `WorkManagerScheduler.scheduleIncoming: enqueued INCOMING ...` (`WorkManagerScheduler.kt:156`).
  When not set up it logs "Received SMS but not set up to back up." (`:62`, `Log.i`) or the `autoBackup=...` line
  (`:81`, `Log.d`). Tag is `SMSBackup+` (`App.java:93`).
- **Observation from an emulator run, with its limits.** The orchestrator reports one run on an Android 16 (API 36)
  emulator on 2026-11-20: auto backup on, one incoming SMS injected through the emulator console. The app's log shows
  the content-URI trigger firing ten seconds later ("BackupTriggerWorker.doWork: content-URI trigger fired; enqueueing
  delayed incoming backup with initialDelay=60s") and shows no line from the SMS receiver for that message. What this
  proves depends on the receiver's logging: on the accepted path the receiver logs nothing of its own, and the
  `Log.v` line exists only in debug builds (`LOCAL_LOGV`). So the missing line is **not** evidence that the broadcast
  was not delivered, unless the run was a debug build, in which case a missing "onReceive(" line (verbose) would
  suggest it was not; and a following `WorkManagerScheduler.scheduleIncoming: enqueued INCOMING` line would have been
  the sign of delivery (the run's build type and whether that line appeared are not known to this record). The raw
  log is outside the repository; this is one run on one emulator and was not designed to answer this question. It
  supports the architect's needs_check (whether a non-exported manifest receiver gets `SMS_RECEIVED`) and belongs to
  the new bug (Scope Concern 2), not to this record's ruling.
- `scheduleIncoming()` (`WorkManagerScheduler.kt:134-159`): returns `null` when encryption is degraded, auto backup is off or
  the timeout is 0 or less; otherwise enqueues unique work `INCOMING`, `REPLACE`, `setInitialDelay(incomingTimeoutSecs)`.
- `App.java:258` and `:289`: `setBroadcastReceiversEnabled(preferences.isAutoBackupEnabled())`
  (`:358-361`: enables or disables `SmsBroadcastReceiver` and `BootReceiver`). `App.java:373-385`
  `rescheduleJobs()`: `cancelAll()`, then if auto backup is on `scheduleRegular()` and, if
  `getIncomingTimeoutSecs() > 0`, `scheduleContentTrigger()`; the return value is ignored.
  So on API 24 and above **both** triggers are live at once for incoming SMS; the broadcast path is not a
  fallback there. Both end in the same unique work `INCOMING` with `REPLACE`, so INV-1 collapses them.
- The manifest has no `PHONE_STATE`, `NEW_OUTGOING` or `CallLog` string (grep of `AndroidManifest.xml` and `App.java`): no
  receiver serves the call-log-after-call feature. Below API 24 the "fallback" therefore covered incoming
  SMS only; the call-log half of INV-4's clause had no mechanism below 24.
- `app/build.gradle:21,24,25`: `compileSdkVersion 35`, `minSdkVersion 21`, `targetSdkVersion 35`. **The build
  still declares 21 today**; story S1-I4 changes it to 24 (DES-MODERNIZATION-014 line 533). That is the interval
  discussed under "Order".

**4. The tests (read, not edited):**

- `service/WorkManagerSchedulerTest.kt:43` `@Config(sdk = [29])` for the class. INV-4 tests: `:308`
  `inv4_contentTrigger_api29_smsUriPresent_withDescendants`, `:332` `inv4_contentTrigger_callLogEnabled_bothUrisPresent`,
  `:356` `inv4_contentTrigger_callLogDisabled_onlySmsUri` (all at API 29).
- **`:377-384`: `@Config(sdk = [23])` `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast`. It asserts
  only `assertThat(result).isNull()`.** It gates the branch's `return null`. It does not assert that any
  fallback work is enqueued, so the "API ≤23: assert the broadcast fallback enqueues a delayed
  `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)`" row of DES-MODERNIZATION-005:387 was never met by it.
- `:397-460` the AC-5 tests (`ac5_scheduleIncoming_withTimeout_isEnqueued` and others): assert the work is
  ENQUEUED or that `null` is returned. A search of the file for `initialDelay` finds no assertion of the delay length.
- `receiver/SmsBroadcastReceiverTest.java:45` `shouldScheduleIncomingBackupAfterIncomingMessage` (verifies
  `scheduleIncoming()` once), `:51`, `:58`, `:65` (not scheduled when auto backup is off, login is missing, first
  backup not run). No `@Config` in that file, so it is independent of the level.
- No test references `BackupTriggerWorker` (search of `app/src/test`): the stage-2 debounce has no unit test.
  The worker also diverges from the contract's wording (it does not call `scheduleIncoming()`). See Scope Concern 2.

**5. The requirements:**

- `REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md:52-56` (status approved, `change_records: []`,
  line 17), AC-3: "All four scheduling invariants are preserved and test-verified: ... (d) content-URI trigger (with
  broadcast fallback below API 24)." **This is an approved requirement that states the pre-24 behaviour.** It is
  not contradicted by the removal (no level is below 24 any more, so the clause has no case), but a reader
  who takes it literally will ask for a below-24 test that cannot exist. A contract edit alone cannot
  settle that; this record carries a dated note on the requirement (edit R-1).
- `REQ-MODERNIZATION-015` AC-5 to AC-7 as amended by CR-011 (lines 122-142 of the file): AC-6 lists
  `WorkManagerScheduler.kt` ("the `SDK_INT < VERSION_CODES.N` branch of `scheduleContentTrigger`, line 224") as a
  site to delete; AC-7 says the SDK 23 test in `WorkManagerSchedulerTest` is "retargeted to a level of 24 or above,
  or deleted together with the dead branch they covered". REQ-015 already **decides** the removal. It does not
  say anything about CNTR-MODERNIZATION-004. That is the gap this record closes.
- `REQ-MODERNIZATION-016` AC-8 (line 99): "Every test covering CNTR-MODERNIZATION-004 passes unmodified, or is
  modified only for a documented library API change with identical asserted behavior." This applies to S4, after S1.

**6. DES-MODERNIZATION-014:** decision D4 (lines 783-833: table S row S-7 at 806; table T row at 826: the SDK 23
test becomes `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger`, `sdk = [24]`, "Result non-null; unique work
`CONTENT_TRIGGER_UNIQUE_NAME` enqueued with the SMS URI trigger"); "Integration Contracts" (lines 1775-1826: the
line list at 1799-1804 and the argument at 1806-1822, which ends "The change record rules on this before S1 is planned
(precondition P-1). If it rules that the fallback must stay asserted, S1 keeps an SMS_RECEIVED-path test as the INV-4
fallback evidence instead."); S1 table, S1-I2 to I5 (lines 520-537); preconditions line 522: "U-066 closed; change
records P-1 and P-2 exist."

---

## Triage

### Classification

- **Type:** DCR. The contract and the design were correct when approved, but they no longer match the
  approved requirement text (REQ-MODERNIZATION-015 after CR-011) and the code S1 will leave. The
  requirement change itself (minSdk 24) was already made by CR-011 (an RCR). The note on REQ-MODERNIZATION-005
  (edit R-1) adds no requirement and changes no value; it states that a clause is vacuous. If the
  Requirements Reviewer reads AC-3(d) as contradicted rather than vacuous, the type becomes RCR and the edits
  stay the same.
- **Root:** Design and contracts layer. CNTR-MODERNIZATION-004 is where the stale statement became a rule ("a
  break gated by tests"); DES-MODERNIZATION-005 repeats it and carries the validation row.
- **Layers Affected:** 2 layers need text edits now (design and contracts: 2 files; requirements: 1 file, a note).
  Code, tests and documentation change later through story S1.
- **Severity Justification:** High, using the create-cr rubric (critical: blocks active work or 5+ files; high:
  affects multiple stories but does not block current work; medium: isolated and non-blocking). It affects two
  stories (S1 deletes the branch and retargets the test; S4 re-runs the scheduler tests on WorkManager 2.12.0 and
  must find the contract consistent) and is a mandatory precondition of S1 (Record 14, row 9). It does not block
  current work (sprint-022 holds only the TLS story U-066, which touches no scheduler file), so it is not Critical.
  It is not Medium because S1 cannot be planned without it. Nothing is wrong in production: the branch is
  correct for the levels the build still declares, and no behaviour changes for any device in use (owner's
  devices are Android 11 or newer, Record 5 row 8, quoted in CR-011). If the orchestrator reads "S1 cannot be
  planned" as "blocks active work" it may raise this to Critical; the next steps are the same.

---

## The ruling this record makes

### The question

Is removing the pre-API-24 content-trigger fallback at minSdk 24 a **behavioural break** of
CNTR-MODERNIZATION-004 in the sense of its Versioning section (line 260), so that it needs the
"gated by the INV-1..4 tests" treatment as a break, a major version and a consumer notice?

### The argument that it is a break (from the contract's own words)

1. Line 260 lists "losing the content-trigger pre-24 fallback or its debounce" by name. S1 deletes the
   code that carries the words "using broadcast fallback".
2. INV-4's clause (line 240) says the obligation holds "on API ≤23 via the broadcast fallback". Deleting a
   binding clause's mechanism looks like a break.
3. REQ-MODERNIZATION-005 AC-3(d) states the pre-24 fallback as a requirement, "test-verified".
4. "MUST be gated" suggests the test must keep asserting the fallback, not vanish.

### The argument that it is not a break

1. **What line 260 protects is behaviour on a supported level.** "Losing" a behaviour means some supported
   device stops having it. At minSdk 24 there is none: the branch runs only when `SDK_INT < 24`
   (`WorkManagerScheduler.kt:224`). The levels that ran it (14 to 23) were dropped by a requirement decision,
   REQ-MODERNIZATION-015 AC-5 (CR-011, Record 4), whose own consequences are listed there (Description: "Android 5.0,
   5.1 and 6.0 devices ... stop receiving updates"). That loss of device support is not a scheduling-semantics
   change; it belongs to REQ-015, which is approved and owns it. The contract is overtaken, not violated.
2. **The contract itself calls the branch adapter-internal and non-breaking.** Line 261: "Adapter-internal
   changes (the `SDK_INT >= 24` branch, ...) are non-breaking", and line 240 says the branch "lives inside the
   adapter, so the port surface is version-agnostic". Line 260 and line 261 cannot both govern the branch; the second
   matches the port design (no signature, enum or state variant changes).
3. **The removed branch does nothing on any level.** Its body only logs and does `return null`
   (`WorkManagerScheduler.kt:224-230`); it enqueues nothing and calls nothing. Deleting it changes nothing on any
   level. **The ruling rests on points 1 to 3.** It does not depend on the receiver path being live. For information:
   the path the old text called the fallback is the `SMS_RECEIVED` receiver (`SmsBroadcastReceiver.java:58-60` calls
   `scheduler.scheduleIncoming()`; no `SDK_INT` check; manifest `:146-153`; `App.java:258`). S1 does not touch it.
   Whether the platform delivers `SMS_RECEIVED` to a non-exported manifest receiver is not verified (Scope Concern 1).
4. **The debounce is not lost.** The two-stage debounce (the content change enqueues a delayed incoming-backup and
   never backs up on the raw change) is carried by `BackupTriggerWorker.kt:53-73` on every level that has the
   content trigger, which is every supported level.
5. **The gate never asserted the fallback.** The only test of the pre-24 side asserts `isNull()` of a return
   value (`WorkManagerSchedulerTest.kt:383`). It does not assert that the broadcast path enqueues anything. What
   "gated" means for a condition that can no longer occur: the gate protects a behaviour on a level a device can
   be on; a test at API 23 would pass or fail on a state no device has, and Robolectric at a level below the
   declared floor is at best unsupported (REQ-015 line 78 takes the approved view that such tests "cannot run once
   minSdk is 24"; whether Robolectric refuses `sdk = [23]` is not verified). The honest reading of "gated" is:
   the invariant stays covered at the **lowest level that still exists**, which is API 24.
6. **Where the argument needs care.** (a) Below API 24 INV-4's clause was met at most by the receiver, for incoming
   SMS only: sent messages and call-log changes are seen only by the content trigger, and no call-log receiver exists
   (Evidence 3). The "API ≤23" assertion in DES-005 was never implemented. History is not rewritten beyond that; the
   contract says it once (C-5, C-7). (b) The ruling must not be quoted later as permission to remove the `SMS_RECEIVED`
   path; this record does not rule on it (Scope Concern 1).

### Recommended ruling

1. **Not a behavioural break of CNTR-MODERNIZATION-004.** It is a revision that removes obsolete scope: no
   supported level can run the removed branch, which only returned `null`; the contract's own line 261 makes
   branching inside the adapter non-breaking; the two-stage debounce is intact.
2. **The gate is retargeted, not waived.** INV-4's version-boundary assertion moves to the lowest supported level
   (API 24: content trigger scheduled, SMS URI, descendants) beside the existing API 29 tests. INV-1 to INV-3 stay
   asserted where they are (API 29). INV-4's substance holds on every supported level.
3. **The text says so in the contract.** Line 260 is reworded so it cannot be read as broken by this change (edit C-8).

### Version step the contract's own rules require

The contract has **no minor or major rule**. Its Versioning section defines what is *breaking* (line 260) and
what is *non-breaking* (line 261), but ties neither to a number. It uses whole integers only: v1 to v2 was an
additive amendment (`scheduleManual`, changelog lines 250-255), and CNTR-MODERNIZATION-007's v2, called "non-breaking for
consumers" (line 319 of that contract), also took the next integer. No contract in `sdlc/artifacts/design/contracts` uses a
decimal version (all are v1 or v2). Recommendation: **v3**, in place (`status: approved` stays), with a changelog entry
that says in its first line that the change is non-breaking and changes no port signature, `BackupType` value,
`SchedulerState` variant or invariant substance. Rejected: a major-style "breaking" step with a consumer-migration
notice (the ruling above is that nothing breaks); a new decimal notation such as v2.1 (no precedent in the corpus; it
would need a rule the contract does not have). If the orchestrator prefers v2.1, edits C-6 and C-7 change in the version
label only.

---

## Impact Analysis

Searches run 2026-11-21 (read-only). `change_records.layers` is not configured in `sdlc/config.yaml`; the layers named
in the dispatch are used. Search patterns: `pre-?24|API ?(≤|<=) ?23|below API 24|API < 24|broadcast fallback|API 14.?23|SDK_INT >= 24|API 24`,
over `sdlc/artifacts` (excluding CR-011 to CR-016) and `app/src`.

### Layer Impact Matrix

| # | Layer | Affected? | Files | Occurrences | Action Needed |
|---|-------|-----------|-------|-------------|---------------|
| 1 | Ground truth: the approved requirement text after CR-011 (REQ-015 AC-5 to AC-7), approved design DES-014 D4, the code, Record 14 row 9 | Ground truth | see Source of Truth | n/a | None (read-only) |
| 2 | Requirements (`sdlc/artifacts/requirements`) | **Yes, a note** | `REQ-MODERNIZATION-005` (AC-3(d) at line 56; frontmatter line 17; Notes at line 80). `REQ-MODERNIZATION-015` AC-6 and AC-7 already name the site and the test: no edit. `REQ-MODERNIZATION-016` AC-8: no edit | 1 file, 2 changes (frontmatter, note) | Edit R-1 |
| 3 | Design and contracts (`sdlc/artifacts/design`) | **Yes** | `CNTR-MODERNIZATION-004` (frontmatter + 10 text places: lines 38, 62-65, 170, 240, 246, 248-250, 260, 261, 363, 381-383, and Notes); `DES-MODERNIZATION-005` (frontmatter + lines 88, 238, 281, 317-333, 387, 444, Notes). `DES-MODERNIZATION-014`: no edit (its text says the ruling is pending and is not false; S1's plan cites this record) | 2 files, 19 changes | Edits C-0 to C-11, D-0 to D-7 |
| 4 | Stories, sprints, change records (`sdlc/artifacts/build`, `sdlc/artifacts/stories`) | No in-flight work | Story S1 does not exist yet (no story file; sprint-022 holds U-066 only). Historic sprint-001, -010, -017, -018, -021 records mention the pre-24 split as history | 0 active | None. S1's plan cites this record (see below). CR-011's Impact table row for `WorkManagerScheduler.kt:224` stays as history |
| 5 | Application code (`app/src/main`) | **Later (S1)** | `scheduler/WorkManagerScheduler.kt` (`:20` import, `:58-61` KDoc, `:208-210` KDoc, `:224-230` branch, `:256` log text); **not in D4's list:** `scheduler/BackupScheduler.java:74-77` (javadoc), `App.java:379-380` (comment) | 8 sites in 3 files (D4 names 5 of them) | S1 deletes or rewords. Not by this record |
| 6 | Vendored library (`k9mail-vendored`) | No | The `SDK_INT` guards there are TLS-related and are out of scope (DES-014 D4b) | 0 | None |
| 7 | Tests (`app/src/test`) | **Later (S1)** | `service/WorkManagerSchedulerTest.kt`: `:377-384` the sdk-23 test (retarget); **not in D4's list:** comments at `:34` and `:296-299` ("pre-24 fallback"), KDoc at `:371-376`. `receiver/SmsBroadcastReceiverTest.java`: no edit | 1 test, 3 comments | S1 retargets the test and rewords the comments |
| 8 | Documentation (README, `sdlc/artifacts/engagement`) | No | No pre-24 content-trigger statement found in `README.md` or `sdlc/artifacts/engagement` | 0 | None |

Layers touched: 5 of 8 (2 now, 3 later through S1). Layers edited by this record: 2 (requirements note, design and contracts).

### What S1's plan must cite

1. This record (CR-015) and the ruling "not a behavioural break", with contract v3 and the reworded policy lines (C-8 and C-9) as the rule the plan is built against.
2. The retargeted test name `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` and its assertions at `@Config(sdk = [24])`:
   `scheduleContentTrigger()` returns non-null, unique work `CONTENT_TRIGGER_UNIQUE_NAME` is enqueued with the SMS URI trigger and
   `triggerForDescendants = true` (D4 table T; DES-005 D-5; contract INV-4).
3. The sites D4 does not list, so that the stale-comment review finds nothing left: `BackupScheduler.java:74-77`, `App.java:379-380`,
   `WorkManagerScheduler.kt:256` (log text), and the test comments at `WorkManagerSchedulerTest.kt:34`, `:296-299`, `:371-376`.
   The comments that say "via scheduleIncoming()" (`BackupTriggerWorker.kt:39`, `WorkManagerSchedulerTest.kt:537`) go to the new bug, not to S1.
4. `scheduleContentTrigger()` keeps its `ScheduledJob?` return type (the encryption-degraded path still returns `null`, as D4 row S-7 says), and the port
   Javadoc at `BackupScheduler.java:77` ("or null if the adapter does not support it") is reworded to that real reason.
5. Empty diffs for `SmsBroadcastReceiver.java`, the receiver's entry in `AndroidManifest.xml`, and `BackupTriggerWorker.kt` (the last is the bug's scope, not S1's).
6. The plan does **not** cite the broadcast path as evidence for INV-4.

### Interaction with S4 (WorkManager 2.12.0)

S4 changes the same scheduler's library. S1 runs first (D2). After S1 the retargeted API 24 test is part of
the baseline that S4 must keep green: REQ-MODERNIZATION-016 AC-8 requires "every test covering CNTR-MODERNIZATION-004
passes unmodified, or is modified only for a documented library API change with identical asserted behavior". Two
consequences. (a) S4 must not re-edit the INV-4 text or the retargeted test: its diff is evidence for AC-8 and any
change must be justified as an API change. (b) WorkManager 2.12.0 itself needs minSdk 24 (DES-014 line 54), which is
the same floor this record relies on; the two changes reinforce each other. Whether the content-URI trigger API
(`Constraints.Builder.addContentUriTrigger`, `Constraints.contentUriTriggers`) is unchanged in 2.12.0 is **not verified**
here; S4 planning reads the release notes (needs_check).

### Interaction with BUG-021 and the Android 17 measurement

DES-014 D14 measures, among other things, how the content trigger and the `SMS_RECEIVED` receiver behave on Android 17
(Run R reads only the `SmsBroadcastReceiver` and `BackupTriggerWorker` log lines; DES-014 line 1320 notes that the
`SMS_RECEIVED` trigger is withheld for a one-time-code message). This record adds nothing to that measurement. It makes
thing easier to read: the contract states that the broadcast path covers incoming SMS only. Whether that path is delivered at all is
not verified (Scope Concern 1), so Run R's "no receiver line" needs an ordinary-SMS control. BUG-021's completeness rule
(REQ-MODERNIZATION-020) is where a delayed-visibility loss is handled, not INV-4. Nothing here changes BUG-021's
placement (after BUG-022, before S4). The two should not be confused: INV-4 says *when a backup is triggered*;
REQ-020 and BUG-021 say *which messages a triggered backup must include*.

---

## Proposed Change

Applied inline, right after this record is approved (see "Order relative to the code"). "Current text" is quoted
from the lines found 2026-11-21; apply by the quoted text, not by line number. After each edit
`status: approved` stays. A grep for the old phrases returns only the intended history (Verification).

### Dated-note and frontmatter convention

How earlier amendments did it: CR-001 amended CNTR-MODERNIZATION-004 in place to v2 with a Changelog entry, an
updated Versioning line and frontmatter additions (`related_requirements`, `related_design_docs`), and added
`CR-001` to `change_records`; CR-002 did the same for CNTR-MODERNIZATION-007; CR-011 added the record id to
`change_records` and a body note "Amended {date} by CR-nnn (approved {date}): ..." to each requirement it
changed. This record follows the same pattern: the id in `change_records` of every file edited, a version bump and
changelog line on the contract, a dated note in the body of the design and the requirement.

### CNTR-MODERNIZATION-004 (edits C-0 to C-11)

| # | Place | Current text | Proposed text |
|---|-------|--------------|---------------|
| C-0 | Frontmatter | `related_requirements:` `  - REQ-MODERNIZATION-013`; `related_design_docs:` `  - DES-MODERNIZATION-012`; `change_records:` `  - CR-001` | Add `  - REQ-MODERNIZATION-015` under `related_requirements`; add `  - DES-MODERNIZATION-014` under `related_design_docs`; add `  - CR-015` under `change_records` |
| C-1 | Line 38 (producer row) | "...version-branches the content-URI trigger (`SDK_INT >= 24`); re-imposes the 300s backoff cap at the worker..." | "...the content-URI trigger needs no version branch at the minSdk 24 floor (REQ-MODERNIZATION-015; the code branch that served the lower levels is removed by story S1 of DES-MODERNIZATION-014); re-imposes the 300s backoff cap at the worker..." (true before and after S1) |
| C-2 | Lines 62-65 | "// recurring content-URI observer over SMS provider (+ call-log when" / "// isCallLogBackupAfterCallEnabled()); API 24+ only; pre-24 falls back to" / "// the broadcast→scheduleIncoming() path. (BackupJobs.java:82)" | "// recurring content-URI observer over SMS provider (+ call-log when" / "// isCallLogBackupAfterCallEnabled()); available on every supported level" / "// (minSdk 24). (BackupJobs.java:82)" |
| C-3 | Line 170 | "`contentUriTriggers: List<ContentUriTrigger>?  // content-trigger job only; API 24+`" | "`contentUriTriggers: List<ContentUriTrigger>?  // content-trigger job only`" |
| C-4a | Line 240, INV-4 title | "**INV-4 — content-URI trigger (+ pre-24 fallback)**" | "**INV-4 — content-URI trigger**" |
| C-4b | Line 240, clause | "An SMS (and, when call-log-after-call is enabled, call-log) content change MUST trigger backup. On API 24+ via content-URI triggers; on API ≤23 via the **broadcast fallback** (`scheduleIncoming()`). The trigger MUST preserve the **two-stage debounce**:" | "An SMS (and, when call-log-after-call is enabled, call-log) content change MUST trigger backup, on every supported level (minSdk 24 and above), via content-URI triggers. The trigger MUST preserve the **two-stage debounce**:" (the rest of the cell, "the content change enqueues a *delayed* incoming-backup (delay = `getIncomingTimeoutSecs()`), it does NOT back up on the raw change.", is unchanged) |
| C-4c | Line 240, realization | "API 24+: `Constraints.addContentUriTrigger(uri, triggerForDescendants=true)` per observed URI; the triggered worker enqueues the delayed `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)`. API ≤23: SMS-received broadcast path calls `scheduler.scheduleIncoming()` → delayed one-off. The `SDK_INT >= 24` branch lives **inside the adapter**, so the port surface is version-agnostic." | "`Constraints.addContentUriTrigger(uri, triggerForDescendants=true)` per observed URI; the triggered worker enqueues the delayed `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)` as unique work `INCOMING` (REPLACE). The port surface is version-agnostic. The SMS-received broadcast path (`SmsBroadcastReceiver` → `scheduler.scheduleIncoming()`) is unchanged and has no version check; see the note below the table." |
| C-5 | New blockquote, directly after the INV-4 table row (before the "Degenerate execution window" quote) | (none) | "> **INV-4 at the minSdk 24 floor (v3, CR-015).** Every supported level (minSdk 24, REQ-MODERNIZATION-015) has content-URI triggers, so INV-4 is one mechanism with no version split. The earlier clause "on API ≤23 via the broadcast fallback" is retired as obsolete scope, not as a lost capability: no supported level can run it, and this is not a behavioural break (see Breaking change policy). Below API 24 that clause was met at most by the SMS-received receiver, for incoming SMS only, and its API ≤23 assertion was never implemented. The SMS-received broadcast path (`SmsBroadcastReceiver`, manifest receiver for `SMS_RECEIVED`, no version check) is unchanged. When it is delivered, and auto backup is on, login information is set and the first backup has run, it calls `scheduler.scheduleIncoming()`. That enqueues a delayed one-off of `BackupWorker` under the same unique name (`INCOMING`, REPLACE) as the content trigger's follow-up, so INV-1 collapses the two at enqueue. It covers incoming SMS only: sent messages and call-log changes are seen only by the content trigger. Whether that path is itself a binding clause is not decided here; removing it would be a separate, recorded change. Test gate: INV-4's version-boundary assertion is made at the lowest supported level, API 24 (DES-MODERNIZATION-005 §Design Validation). The `SDK_INT < VERSION_CODES.N` branch of `WorkManagerScheduler.scheduleContentTrigger` and the test `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` are removed and retargeted by story S1 of DES-MODERNIZATION-014 (D4, row S-7 and table T). This contract states the target the code reaches with S1." |
| C-6 | Line 246 (Versioning, current version) | "**Current version:** v2 (amended in place by DES-MODERNIZATION-012 / REQ-MODERNIZATION-013; v1 introduced by MU-005 / DES-MODERNIZATION-005). Amended in place — the contract has not been implemented against the v2 surface yet, so no consumer migration is required; `status` remains `approved`." | "**Current version:** v3 (amended in place by CR-015, 2026-11-21; v2 amended in place by DES-MODERNIZATION-012 / REQ-MODERNIZATION-013; v1 introduced by MU-005 / DES-MODERNIZATION-005). v3 is non-breaking: it changes no port signature, no `BackupType` value, no `SchedulerState` variant and no invariant's substance, so no consumer migration is required; `status` remains `approved`." (The dropped v2 sentence is stale: `scheduleManual` is implemented at `WorkManagerScheduler.kt:349`; see Scope Concern 4) |
| C-7 | Changelog, new entry above the v2 entry (line 250) | (none) | "- **v3 — 2026-11-21 (CR-015; REQ-MODERNIZATION-015 / CR-011; DES-MODERNIZATION-014 story S1).** Non-breaking amendment: the minSdk 24 floor retires the pre-API-24 clauses. 1. **INV-4 is one mechanism on every supported level.** Removed "on API ≤23 via the broadcast fallback" from the INV-4 clause and realization, and "(+ pre-24 fallback)" from its title. Kept: an SMS (and, when enabled, call-log) content change MUST trigger backup via content-URI triggers, with the two-stage debounce. 2. **Ruling: not a behavioural break.** No supported level can run the removed branch, which only returned `null`; the SMS-received broadcast path is unchanged; the debounce is intact. 3. Removed the API 24+ / pre-24 wording from `scheduleContentTrigger()`, `contentUriTriggers`, the producer row and the content-trigger example; added a note stating what the broadcast path covers (incoming SMS only). 4. **Test gate retargeted, not dropped:** the INV-4 version-boundary assertion moves to the lowest supported level, API 24. The API ≤23 assertion that DES-MODERNIZATION-005 required was never implemented (the only test asserted a `null` return). 5. No change to any port signature, `BackupType` value, `SchedulerState` variant, other invariant or Validation Rule. 6. Frontmatter: `CR-015` in `change_records`, `REQ-MODERNIZATION-015` in `related_requirements`, `DES-MODERNIZATION-014` in `related_design_docs`. 7. The branch and its test are deleted and retargeted by story S1 of DES-MODERNIZATION-014 (D4); this entry states the target." |
| C-8 | Line 260 (Breaking change policy) | "...or losing the content-trigger pre-24 fallback or its debounce). Behavioral breaks are the higher-severity class: they can silently change scheduling semantics without a compile error and MUST be gated by the INV-1..4 tests (DES-005 §Design Validation)." | "...or losing the content-trigger or its debounce on any supported level). Behavioral breaks are the higher-severity class: they can silently change scheduling semantics without a compile error and MUST be gated by the INV-1..4 tests (DES-005 §Design Validation); where an invariant's realization depends on the API level, it is asserted at the lowest supported level. From v3 the pre-24 fallback is not on this list: the minSdk 24 floor (REQ-MODERNIZATION-015) retired the pre-24 levels, so removing the branch that served them loses no behaviour on any supported level (CR-015)." |
| C-9 | Line 261 (Backward compatibility) | "Adapter-internal changes (the `SDK_INT >= 24` branch, the worker-side 300s cap mechanism) are non-breaking." | "Adapter-internal changes (the worker-side 300s cap mechanism; an API-level branch inside the adapter, such as the `SDK_INT >= 24` branch of the content trigger that story S1 of DES-MODERNIZATION-014 removes, provided every supported level still meets INV-1..4) are non-breaking." |
| C-10a | Line 363 (example heading) | "**Example: content-URI trigger (API 24+) and its debounce follow-up**" | "**Example: content-URI trigger and its debounce follow-up**" |
| C-10b | Lines 381-383 (end of the example) | "// API <= 23 fallback (no content-URI triggers available):" / "// the SMS-received broadcast path calls scheduler.scheduleIncoming() ->" / "//   OneTimeWorkRequest(initialDelay = getIncomingTimeoutSecs())   // same debounce." | "// Separately, the SMS-received broadcast path (incoming SMS only) calls scheduler.scheduleIncoming() ->" / "//   OneTimeWorkRequest(initialDelay = getIncomingTimeoutSecs())   // same debounce, same unique name INCOMING;" / "//   INV-1 (REPLACE) collapses it with the content trigger's follow-up." |
| C-11 | Notes, new last bullet (after the "v2 implementation note" bullet) | (none) | "- **Amended 2026-11-21 by CR-015 (v3):** the pre-API-24 clauses are removed because the minSdk 24 floor (REQ-MODERNIZATION-015, CR-011) leaves no supported level below API 24; ruled not a behavioural break. INV-4's version-boundary assertion moves to API 24. The code branch and the test follow in story S1 of DES-MODERNIZATION-014. See the Changelog, v3." |

INV-4 reads, after the edit (clause cell): "An SMS (and, when call-log-after-call is enabled, call-log) content change MUST trigger backup, on every
supported level (minSdk 24 and above), via content-URI triggers. The trigger MUST preserve the **two-stage debounce**:
the content change enqueues a *delayed* incoming-backup (delay = `getIncomingTimeoutSecs()`), it does NOT back up on the
raw change." What still holds on every supported level: a trigger on SMS and (when enabled) call-log change; the debounce;
no backup on the raw change (Validation Rule 5, unchanged).

### DES-MODERNIZATION-005 (edits D-0 to D-7)

| # | Place | Current text | Proposed text |
|---|-------|--------------|---------------|
| D-0 | Frontmatter, line 15 | `change_records: []` | `change_records:` `  - CR-015` |
| D-1 | Line 88 | "`scheduleContentTrigger()            // recurring content-URI observer (API 24+)`" | "`scheduleContentTrigger()            // recurring content-URI observer (every supported level, minSdk 24)`" |
| D-2 | Line 238 (traceability row) | "\| AC-3 (d) content-URI trigger + pre-24 fallback \| `addContentUriTrigger` (24+) / delayed one-off (14–23) \|" | "\| AC-3 (d) content-URI trigger (the clause "with broadcast fallback below API 24" has no case at minSdk 24; CR-015) \| `addContentUriTrigger` on every supported level; the delayed one-off (`initialDelay = incomingTimeoutSecs`) as the second stage \|" |
| D-3 | Line 281 | "...per observed URI (API 24+); see fallback below \| INV-4(d)" | "...per observed URI (every supported level); see "Incoming-SMS trigger" below \| INV-4(d)" |
| D-4 | Lines 317-333 ("Incoming-SMS trigger (AC-3d)", from "The design preserves this two-stage debounce with a version split:" to the last sentence "...so the port surface is version-agnostic and callers are unaffected.") | the text quoted in Evidence 2 | "The design preserves this two-stage debounce with one mechanism on every supported level (minSdk 24, REQ-MODERNIZATION-015):" / "- **Content-URI trigger (every supported level):** `Constraints.addContentUriTrigger(SMS_PROVIDER, triggerForDescendants = true)` (the `FLAG_NOTIFY_FOR_DESCENDANTS` equivalent verified at `:179,181`), plus the call-log URI under the same `isCallLogBackupAfterCallEnabled()` condition. The triggered worker enqueues the delayed incoming-backup work (preserving the `getIncomingTimeoutSecs()` debounce) — it does not back up on the raw content change, matching today's follow-up behavior." / "- **SMS-received broadcast (no version check, unchanged):** when delivered, the SMS-received path calls `scheduler.scheduleIncoming()`, enqueueing a delayed `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)` as unique work `INCOMING` (REPLACE). It runs beside the content trigger for incoming SMS (INV-1 collapses the two at enqueue); it does not see sent messages or call-log changes." / "History: before the minSdk 24 floor (CR-011, CR-015) the project floor was API 14, later 21, and AC-3(d)'s "broadcast fallback below API 24" was the only trigger on those levels, for incoming SMS only. Ruled by CR-015: the removal of that obligation is not a behavioural break of CNTR-MODERNIZATION-004." / "The content trigger needs no version branch: after story S1 of DES-MODERNIZATION-014 (D4, row S-7) `WorkManagerScheduler.scheduleContentTrigger()` has no `SDK_INT` check, so the port surface is version-agnostic and callers are unaffected." |
| D-5 | Line 387 (INV-4 validation row) | "API 24+: assert `WorkSpec.constraints.contentUriTriggers` includes the SMS provider URI (and call-log URI when `isCallLogBackupAfterCallEnabled()`), `triggerForDescendants=true`. API ≤23: assert the broadcast fallback enqueues a delayed `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)`." | "On every supported level, asserted at the lowest, API 24 (`@Config(sdk = [24])`): `scheduleContentTrigger()` returns a job, unique work `CONTENT_TRIGGER` is enqueued, and `WorkSpec.constraints.contentUriTriggers` includes the SMS provider URI with `triggerForDescendants=true`. The test that carries this after story S1 is named `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` (DES-MODERNIZATION-014 D4, table T; retargeted from `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast`). The call-log URI case and the SMS-only case are asserted at API 29 by `inv4_contentTrigger_callLogEnabled_bothUrisPresent` and `inv4_contentTrigger_callLogDisabled_onlySmsUri`. The SMS-received path's receiver logic is asserted by `SmsBroadcastReceiverTest.shouldScheduleIncomingBackupAfterIncomingMessage` (`scheduleIncoming()` is called once); delivery of the broadcast to the receiver is not unit-tested." |
| D-6 | Line 444 (port table) | "\| `scheduleContentTrigger()` \| recurring content-URI observer over SMS (+ call-log when enabled), API 24+ \| `PeriodicWorkRequest` + content-URI triggers \|" | "\| `scheduleContentTrigger()` \| recurring content-URI observer over SMS (+ call-log when enabled), every supported level (minSdk 24) \| `PeriodicWorkRequest` + content-URI triggers \|" |
| D-7 | Notes, new last paragraph (after line 517) | (none) | "Amended 2026-11-21 by CR-015: the pre-API-24 content-trigger fallback is retired by the minSdk 24 floor (REQ-MODERNIZATION-015, CR-011) and ruled not a behavioural break of CNTR-MODERNIZATION-004 (now v3). The INV-4 validation row now specifies the API 24 assertion. The code branch and the test follow in story S1 of DES-MODERNIZATION-014. Lines in ADR-005-A that name API 14 to 22 and the driver table (line 121) describe WorkManager's own scheduler choice and the old floor as history and are not edited." |

### REQ-MODERNIZATION-005 (edit R-1; a note, no requirement value changes)

| # | Place | Current text | Proposed text |
|---|-------|--------------|---------------|
| R-1a | Frontmatter, line 17 | `change_records: []` | `change_records:` `  - CR-015` |
| R-1b | Notes (end of file, after the "Traceability:" line) | (none) | "Amended 2026-11-21 by CR-015: AC-3(d), "content-URI trigger (with broadcast fallback below API 24)", is unchanged in wording. Since minSdk 24 (REQ-MODERNIZATION-015, CR-011) no supported level is below API 24, so the clause has no case and no test of it is required; AC-3(d) is met by the content-URI trigger tests on every supported level, the lowest being API 24. The `SMS_RECEIVED` path is not removed. Ruled not a behavioural break of CNTR-MODERNIZATION-004 (v3)." |

The wording of AC-3(d) is **not** changed: it is vacuous, not wrong, and the dated note keeps the history. If the Requirements
Reviewer holds that an approved AC must read correctly on its face, the alternative is an in-place rewording
("(d) content-URI trigger on every supported level; the pre-API-24 broadcast fallback is retired by the minSdk 24 floor"), as CR-011 did for REQ-015;
type then becomes RCR. Not recommended: it edits an approved requirement for no behavioural reason.

### Not edited, on purpose

- `DES-MODERNIZATION-014` (approved today): its "Integration Contracts" section says the ruling is pending. That is true until this record is
  applied and not false after; S1's plan cites this record. DES-014 is not amended for it.
- `REQ-MODERNIZATION-015` AC-6 and AC-7: they already name the branch and the SDK 23 test.
- CNTR-MODERNIZATION-004 Validation Rule 5 (line 271): no pre-24 text; stays.
- Code, tests, `README.md`, historic sprint records.

---

## Amendment 1 (2026-11-21): four texts the applied-text check found

**Source.** After the edits C-0 to C-11, D-0 to D-7 and R-1 were applied (2026-11-21), an independent Design Auditor checked the applied text and wrote `sdlc/artifacts/build/change-records/evidence/CR-015-applied-text-check.md`. Its verdict is `Verdict: FAIL`. The applied edits match this record word for word and nothing else in the three files changed. The FAIL rests on four one-line texts this record never covered: finding F-1 (must-fix) and findings F-2, F-3 and F-4 (should-fix). This record's verification grep had no pattern for "version-branches", which is how F-1 was missed.

**Ruling (orchestrator, 2026-11-21).** All four are fixed now, by this amendment. This amendment is insert-only and adds edits D-8, D-9, C-12 and R-1c. It changes no ruling of this record: "not a behavioural break", the retargeting of INV-4 to API 24, option A (apply now, before story S1) and the wording rules of "Order relative to the code" all stand. Where an edit below replaces the wording of an earlier edit of this record (D-9 replaces a reference inside the D-7 note; R-1c replaces a clause inside the R-1b note), the amendment's text governs for that clause only.

**No new version step.** The contract stays v3. Edit C-12 corrects a stale reference (the Status note still says v2) and removes an absolute local repository path; D-8, D-9 and R-1c correct a description, a reference and a test fact in the design and the requirement. None changes a port signature, a `BackupType` value, a `SchedulerState` variant, an invariant's substance or any value, so the v3 claim of edit C-6 and the changelog entry of C-7 are unchanged.

### New edits

"Current text" was checked against the working-tree files on 2026-11-21 and is quoted exactly. Apply by the quoted text, not by line number. The three files are not edited by this record's author; an applier does that. `status: approved` and each file's `id` and `artifact_type` stay.

| # | File and place | Current text | Replacement text | Finding |
|---|----------------|--------------|------------------|---------|
| D-8 | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md`, Components table, third cell of the `WorkManagerScheduler` (adapter) row | "Sole production impl; maps port calls to `WorkRequest`s; version-branches content-URI trigger" | "Sole production impl; maps port calls to `WorkRequest`s; the content-URI trigger needs no version branch at the minSdk 24 floor (CR-015; the code branch is removed by story S1 of DES-MODERNIZATION-014)" (true before and after S1; it is the same wording as C-1 and D-4) | F-1 (must-fix) |
| D-9 | Same design file, Notes, the D-7 paragraph ("Amended 2026-11-21 by CR-015: ..."), last sentence | "Lines in ADR-005-A that name API 14 to 22 and the driver table (line 121) describe WorkManager's own scheduler choice and the old floor as history and are not edited." | "Lines in ADR-005-A that name API 14 to 22 and the "AlarmManager fallback driver" row of the legacy-mechanism table under "WorkManager + CoroutineWorker: four mechanisms collapse to one, flipped once" describe WorkManager's own scheduler choice and the old floor as history and are not edited." | F-2 (should-fix) |
| C-12 | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md`, Notes, the Status bullet. Match the bullet by this fragment, which excludes the path: "- **Status:** approved (v2 amended in place per DES-MODERNIZATION-012 / REQ-MODERNIZATION-013). All paths relative to repo root (". The applier replaces the whole bullet, up to the end of its line | The fragment above, followed on the same line by an absolute local repository path in parentheses and a full stop (the path is not quoted in this record) | "- **Status:** approved (v3 amended in place by CR-015; v2 amended in place per DES-MODERNIZATION-012 / REQ-MODERNIZATION-013). All paths are relative to the repository root." | F-3 (should-fix) |
| R-1c | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md`, Notes, the R-1b note, clause after the semicolon | "AC-3(d) is met by the content-URI trigger tests on every supported level, the lowest being API 24." | "AC-3(d) is met by the content-URI trigger tests; their lowest-level assertion is made at API 24 once story S1 of DES-MODERNIZATION-014 retargets `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` (CNTR-MODERNIZATION-004 v3, INV-4 note)." (true today and after S1: today the content-URI tests run at API 29 and the one lower-level test asserts only `null`) | F-4 (should-fix) |

Replacements were taken from the check report unchanged, except D-9, where the report's wording is made unambiguous: the report points to "the "AlarmManager fallback driver" row of the System Architecture table", but a row with the same first cell also exists in the earlier component table of the Overview/Context part of the design, so the text names the legacy-mechanism table by its subsection heading instead. No line number is used, so the reference cannot go stale again.

Left untouched on purpose (the report says leave): the "Draft; not finalized." sentence in the contract's one-line note near the top. It is older than this record, is not about the content trigger, and was ruled out of scope (Scope Concern 4). The next contract revision should clear it.

**Applier's check, added.** Re-run the verification grep of "Execution" with one more pattern, `version-branch`, over the same three files. Expected after D-8: no match. Also check that the contract's Notes carry no absolute path and that the contract's Current version and its Status bullet both say v3.

### Notes (no edits)

- **Report finding 5 (the example at the contract's content-trigger example, sentence about `scheduleIncoming()`).** Left on purpose. The contract states the intended behaviour; the divergence is a code defect (`BackupTriggerWorker.kt` builds its own `INCOMING` request and does not call `scheduleIncoming()`) with its own two-route decision in `sdlc/artifacts/stories/BUG-023-content-trigger-backup-skips-scheduling-checks.md` (AC-10). Rewriting the example here would decide that bug's route without its analysis. Optional once BUG-023 is approved: a Notes pointer "Known divergence: the trigger worker does not call `scheduleIncoming()` today (BUG-023)". The BUG-023 author should also note that its line references to the design's Risks row and port table moved by the edits (the Risks row and the port table now sit about nine lines higher).
- **Report finding 8 (points for the author of story S1, minSdk 24).** (1) Build against contract v3: the reworded policy lines (C-8, C-9) and the INV-4 note (C-5). (2) The retargeted API 24 test, `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger`, should also assert `triggerForDescendants = true` (design D-5 and contract INV-4 require it; DES-MODERNIZATION-014 table T names only "the SMS URI trigger", and the design text is the stricter one). (3) `SmsBroadcastReceiver.java`, its manifest entry and `BackupTriggerWorker.kt` must show empty diffs; the comments at `BackupTriggerWorker.kt:39` and `WorkManagerSchedulerTest.kt:537` belong to BUG-023. (4) Keep S1-I4 (minSdk 24) before S1-I5 (delete the branch). (5) The scheduler tests live under `app/src/test/java/com/zegoggles/smssync/service/` (`WorkManagerSchedulerTest.kt`), not in a `scheduler/` test package.
- **Report finding 9 (this record's own bookkeeping).** The Metadata `Status`, "Changes Made", "Verification Evidence", "Commit Traceability" and the Fix Checklist of this record are filled in by the orchestrator when the record is marked implemented, together with the commit that carries edits C-0 to C-12, D-0 to D-9, R-1 and R-1c. They are not filled in by this amendment.
- **Report findings 6 and 7** need no action: the design's Risks row claim about a debounce test is covered by BUG-023 (AC-7, AC-10); the v3 "non-breaking" claim holds while the branch is deleted together with or after the minSdk raise.

---

## Order relative to the code

**The interval.** Today the build declares `minSdkVersion 21` (`app/build.gradle:24`), the branch is at
`WorkManagerScheduler.kt:224-230`, and the sdk-23 test runs. Story S1 changes the build to 24 in S1-I4, deletes the
branch in S1-I5 and retargets the test in S1-I3. Between applying this record and S1 closing, the contract
describes a state the code has not reached.

**Sentences that would be false in that interval if worded as plain present-tense statements about the code:**

1. C-1: "the adapter does not version-branch the content-URI trigger" (the code branches at `:224`). Worded instead as "needs no version
   branch at the minSdk 24 floor ... the code branch that served the lower levels is removed by story S1": true before and after.
2. C-4c and D-4: "`scheduleContentTrigger()` has no `SDK_INT` check". Worded "after story S1 ... has no `SDK_INT` check" (D-4) and left out of C-4c.
3. C-5, C-7 item 7, C-11, D-7: "the branch is removed". Worded "are removed and retargeted by story S1": a statement about what S1 does, true before and after.
4. D-5: the test name `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` does not exist until S1. Worded "the test that carries this after story S1 is named ...".
5. C-4b and C-5: "every supported level (minSdk 24)". While the build still declares 21, API 21 to 23 devices exist as installable targets.
   The approved floor is 24 (REQ-015 AC-5) and the text says "supported level" in that sense. This is the one sentence that is
   **true by requirement and not yet by build**. It is the normal state of a contract ahead of its code (CNTR-004 v2 was ahead of the code the same way,
   its line 246), and the owner's devices are Android 11 or newer (Record 5 row 8), so no observable behaviour differs. It is stated here
   rather than hidden.

No sentence in the edited text is false after S1 and none needs a second edit then.

**Recommendation: apply the text edits now, in their own commit, before S1 is planned (option A).** Reasons:

- The design's precondition for S1 is "change records P-1 and P-2 exist" (DES-014 line 522), and the orchestrator's ruling is that they are "raised
  and applied before the minSdk story is planned" (Record 14, row 9). An S1 plan has to cite the contract it is built against. If the edits wait for S1's commit, the plan
  cites a contract that still says "break", and the planner has to override it in the plan, which is the silent revision the process exists to avoid.
- The wording above is true before and after S1, so no later edit is needed and nothing is left stale if S1 slips.
- A contract that is ahead of the code is this project's normal state for approved-but-unbuilt design (CNTR-004 v2).

**Option B (same commit as S1), not recommended:** the text would be applied atomically with the code, so no interval exists. It costs: S1's plan is written against
a contract that is not yet amended; the planner and the Solution Architect review an unratified ruling; S1's commit mixes a ruling with a build change; and "P-1 exists" would mean only that
the record exists, not that the contract reflects it. If B is chosen anyway, "exists before S1 is planned" means this record is approved and the planned S1 diff includes C-0 to C-11, D-0 to D-7 and R-1 as
a listed increment (an S1-I0).

Stage only the three artifact files (contract, design, REQ-005) and this record in that commit: other uncommitted work (TLS) is in the tree.

**What "exists before S1 is planned" means under A:** CR-015 is approved; its edits are committed to the contract (v3), the design and REQ-005; S1's plan cites contract v3 and edit D-5's test name; Fix Checklist items 1 to 4 are ticked before the S1 plan is written.

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Solution Architect, 2026-11-21. Advisory. Code, tests and artifacts were re-read in this session; line numbers are
from the working tree that day.

**What the code shows that the record did not have.**

- **The content trigger's stage 2 does not go through `scheduleIncoming()`.** `worker/BackupTriggerWorker.kt:61-71`
  builds its own `INCOMING` one-off with the delay and `REPLACE`, but with **no network constraint**, no explicit
  backoff, and no encryption-degraded or auto-backup check. `scheduleIncoming()` has all of these
  (`WorkManagerScheduler.kt:135-148`). The contract says the triggered worker "calls scheduler.scheduleIncoming()"
  (CNTR-004 `:378`), and so do the worker's KDoc (`BackupTriggerWorker.kt:39`) and a test comment
  (`WorkManagerSchedulerTest.kt:537`). The code diverges from INV-2 (the network rule for each type) and from the
  U-054 gate. This is not about the pre-24 branch, so it belongs in a bug, not in this record (see 6.2). It does make
  one phrase in C-5 false ("the same delayed one-off").
- **The `SMS_RECEIVED` receiver is declared `android:exported="false"`** (`AndroidManifest.xml:148`; added by U-001).
  The broadcast comes from the telephony process, not from this app or the system server. Whether a non-exported
  manifest receiver gets it is **not verified**. If it does not, the "second live trigger" has been dead since U-001.
  The ruling below does not depend on that path, and the contract text should not claim more than the manifest shows.
  `SmsBroadcastReceiverTest` calls `onReceive` directly (`:45-48`), so it tests the receiver's logic, not whether the
  broadcast is delivered.
- **"And call-log when enabled" is true of the code**, through the content trigger, not through a receiver:
  `WorkManagerScheduler.kt:235-237` adds the call-log URI when call-log backup **and**
  `isCallLogBackupAfterCallEnabled()` are both on. The second condition is not in the contract. It is a harmless
  narrowing; leave it for a later revision.

**Rulings.**

1. **Break or revision: confirmed, not a behavioural break.** The contract's own words settle it: line 260 protects
   against *losing* a behaviour, and line 261 makes adapter-internal API-level branching non-breaking. No supported level
   can run the branch. Its body only logs and returns `null` (`WorkManagerScheduler.kt:224-230`), so deleting it changes
   nothing on any level. The ruling rests on that alone. It does **not** need the broadcast path to be live (record
   argument 3 supports the ruling but is not needed for it; see above). On "what INV-4 always was": do not rewrite
   history. The contract should say, once, that below API 24 the clause was met at most by the receiver, for incoming
   SMS only, and that DES-005's API ≤23 assertion was never implemented. The only test asserted `isNull()`
   (`WorkManagerSchedulerTest.kt:383`). The changelog wording below adds this.
2. **Version step: v3 confirmed.** Whole integers are the corpus convention, and v2 shows an additive change taking the
   next integer. Reject v2.1.
3. **Type: DCR with note R-1 confirmed.** AC-3(d)'s parenthesis qualifies a range of levels that no longer exists, so the
   clause is vacuous, not contradicted. REQ-015 AC-6 and AC-7 (approved later) already decide the removal. Rewording an
   approved AC for no change in behaviour is not warranted. One fix in R-1b: drop "or possible". Whether Robolectric
   refuses `sdk = [23]` is unverified, so the note should claim only "not required".
4. **Edit text: mostly sound; seven edits need changes (exact text below).** C-8 as written would make every INV-1..3
   test non-conformant: they run at `sdk = [29]` (`WorkManagerSchedulerTest.kt:43`), and C-8 says the gate is "at the
   lowest supported level". C-5, C-11 and D-7 state present-tense test facts that are false until S1 ("INV-4 is asserted
   at API 24"). C-5 and D-4 say the receiver "runs on every level", and C-5 says it enqueues "the same delayed one-off".
   D-5 overstates what `SmsBroadcastReceiverTest` covers. C-1 to C-4, C-6, C-10, D-1 to D-3 and D-6 are true against
   the code as worded. C-8 and C-9 now agree with each other.
5. **Order: option A confirmed.** Apply the text in its own commit before S1 is planned, with the replacements below
   (they remove the false-in-the-interval sentences the record's list of five missed). The one remaining caveat is as
   the record states: "minSdk 24" is true by requirement (REQ-015 AC-5) and not by build (`app/build.gradle:24` still
   reads 21) until S1-I4. Stage only the three artifact files: another agent has uncommitted TLS work in the tree.
6. **Scope Concerns.**
   1. *`SMS_RECEIVED` unbound:* **leave it unbound** in this record (C-5 already says "not decided here"). Add the
      exported-delivery question to BUG-021's Run R interpretation (below). **Raise a bug only on evidence** that the
      receiver never fires.
   2. *Debounce barely tested:* **raise a bug** for the `BackupTriggerWorker` divergence (INV-2 network constraint
      missing, U-054 gate missing, contract `:378` not met). Its fix brings the missing worker test. Place it before S4,
      so that REQ-016 AC-8's baseline includes the worker. Neither S1 nor S4 should absorb it. DES-005 `:503` ("INV-4 test
      asserts the debounce delay") is a further unmet claim for the same bug.
   3. *Sites missing from D4:* **S1's plan includes them.** No dated note on DES-014: D4's tables list code with
      behaviour, and comments fall under S1's stale-comment hygiene. The full list: `BackupScheduler.java:74-77`,
      `App.java:379-380`, `WorkManagerScheduler.kt:256` (log text), and `WorkManagerSchedulerTest.kt:34`, `:296-299` and
      `:371-376`. The comments that say "via scheduleIncoming()" (`BackupTriggerWorker.kt:39`, `WorkManagerSchedulerTest.kt:537`)
      go to the bug in 6.2, not to S1.
   4. *Other stale contract lines:* **leave** (lines 21, 62's `: Unit` return types, 403, 404) for the next contract
      revision, except the clause C-6 already drops. Line 378 goes to the bug in 6.2. Do not "correct" it to match the
      code: the contract is likely the intended behaviour.
   5. *Call-log below 24:* **leave**. The changelog wording covers it.
   6. *Robolectric `sdk = [23]`:* **leave**. It is moot once S1 retargets the only test below 24, and AC-7's grep keeps
      it moot. Only R-1b's wording changes.

**Replacement text.**

- **C-5** (whole blockquote): "> **INV-4 at the minSdk 24 floor (v3, CR-015).** Every supported level (minSdk 24,
  REQ-MODERNIZATION-015) has content-URI triggers, so INV-4 is one mechanism with no version split. The earlier clause
  "on API ≤23 via the broadcast fallback" is retired as obsolete scope, not as a lost capability: no supported level can
  run it, and this is not a behavioural break (see Breaking change policy). Below API 24 that clause was met at most by
  the SMS-received receiver, for incoming SMS only, and its API ≤23 assertion was never implemented. The SMS-received
  broadcast path (`SmsBroadcastReceiver`, manifest receiver for `SMS_RECEIVED`, no version check) is unchanged. When it
  is delivered, and auto backup is on, login information is set and the first backup has run, it calls
  `scheduler.scheduleIncoming()`. That enqueues a delayed one-off of `BackupWorker` under the same unique name
  (`INCOMING`, REPLACE) as the content trigger's follow-up, so INV-1 collapses the two at enqueue. It covers incoming SMS
  only: sent messages and call-log changes are seen only by the content trigger. Whether that path is itself a binding
  clause is not decided here; removing it would be a separate, recorded change. Test gate: INV-4's version-boundary
  assertion is made at the lowest supported level, API 24 (DES-MODERNIZATION-005 §Design Validation). The
  `SDK_INT < VERSION_CODES.N` branch of `WorkManagerScheduler.scheduleContentTrigger` and the test
  `inv4_contentTrigger_pre24_returnsNull_fallbackToBroadcast` are removed and retargeted by story S1 of
  DES-MODERNIZATION-014 (D4, row S-7 and table T). This contract states the target the code reaches with S1."
- **C-7**, items 2 and 4 only: "2. **Ruling: not a behavioural break.** No supported level can run the removed branch,
  which only returned `null`; the SMS-received broadcast path is unchanged; the debounce is intact." and "4. **Test gate
  retargeted, not dropped:** the INV-4 version-boundary assertion moves to the lowest supported level, API 24. The API
  ≤23 assertion that DES-MODERNIZATION-005 required was never implemented (the only test asserted a `null` return)."
- **C-8** (from "...or losing" to the end of the bullet): "...or losing the content-trigger or its debounce on any
  supported level). Behavioral breaks are the higher-severity class: they can silently change scheduling semantics
  without a compile error and MUST be gated by the INV-1..4 tests (DES-005 §Design Validation); where an invariant's
  realization depends on the API level, it is asserted at the lowest supported level. From v3 the pre-24 fallback is not
  on this list: the minSdk 24 floor (REQ-MODERNIZATION-015) retired the pre-24 levels, so removing the branch that served
  them loses no behaviour on any supported level (CR-015)."
- **C-9**: "Adapter-internal changes (the worker-side 300s cap mechanism; an API-level branch inside the adapter, such as
  the `SDK_INT >= 24` branch of the content trigger that story S1 of DES-MODERNIZATION-014 removes, provided every
  supported level still meets INV-1..4) are non-breaking."
- **C-11**: replace "INV-4 is asserted at API 24." with "INV-4's version-boundary assertion moves to API 24."
- **D-4**, second bullet: "- **SMS-received broadcast (no version check, unchanged):** when delivered, the SMS-received
  path calls `scheduler.scheduleIncoming()`, enqueueing a delayed `OneTimeWorkRequest(initialDelay = incomingTimeoutSecs)`
  as unique work `INCOMING` (REPLACE). It runs beside the content trigger for incoming SMS (INV-1 collapses the two at
  enqueue); it does not see sent messages or call-log changes." In the "History:" sentence, after "the only trigger on
  those levels", add: ", for incoming SMS only".
- **D-5**, last sentence: "The SMS-received path's receiver logic is asserted by
  `SmsBroadcastReceiverTest.shouldScheduleIncomingBackupAfterIncomingMessage` (`scheduleIncoming()` is called once);
  delivery of the broadcast to the receiver is not unit-tested."
- **D-7**: replace "The INV-4 validation row is asserted at API 24." with "The INV-4 validation row now specifies the
  API 24 assertion."
- **R-1b**: replace "no test of it is required or possible" with "no test of it is required".
- **Housekeeping in this record:** the Impact matrix (row 3) and the DES-005 heading say "D-0 to D-8", but the table
  ends at D-7. Read it as D-7.

**What S1's plan must cite from this record:** CR-015 and contract v3 (the ruling, and C-8 and C-9 as the rule it is
built against); D-5's test name and its assertions at `sdk = [24]`; the site list in 6.3; that
`scheduleContentTrigger()` keeps `ScheduledJob?` and that `BackupScheduler.java:77` is reworded to the
encryption-degraded reason; an empty diff for `SmsBroadcastReceiver.java`, `AndroidManifest.xml`'s receiver entry and
`BackupTriggerWorker.kt` (the last is the bug's scope, not S1's). The plan should not cite the broadcast path as
evidence for INV-4.

**Interaction with BUG-021 and BUG-022.** This record changes nothing in either. Three points for whoever reads
BUG-021's Run R:

- When a held-back message is released, the content trigger is the only candidate: `SMS_RECEIVED` is withheld for such
  messages (DES-014 `:1320`).
- If the receiver is not delivered at all because it is not exported, then "no `SmsBroadcastReceiver` line" means
  nothing about the one-time-code case unless an ordinary SMS in the same run shows the line.
- The content trigger is periodic work with URI triggers (`WorkManagerScheduler.kt:240-247`). How soon it fires again
  after its last run is not verified, so Run R should not assume it fires immediately.

BUG-022 (cancellation checks that never throw) bears on C-5's "INV-1 collapses the two". The collapse is guaranteed at
enqueue. When an `INCOMING` backup is already running, REPLACE depends on cancellation, and that is BUG-022's defect.
The C-5 wording above claims only the collapse at enqueue.

---

## Cascade Analysis

**Root Layer:** Design and contracts (CNTR-MODERNIZATION-004, which turned the pre-24 branch into a guarded rule; DES-MODERNIZATION-005, which repeats it)
**Root Defect:** The contract and the design carry an obligation (the pre-24 content-trigger fallback) for levels the approved floor has removed, and classify its loss as a behavioural break, so the work that deletes the branch (S1) appears to violate the contract.
**Cascade Direction:** Mid-stream down. The origin is a requirement change already applied (CR-011); the stale text is in design and contracts; the code and test follow through S1.

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Requirements (REQ-015 after CR-011) | Design and contracts | Yes, stale text | 19 changes in 2 files (Impact matrix row 3) |
| 2 | Requirements (REQ-015) | Requirements (REQ-005 AC-3(d)) | Yes, a vacuous clause | `REQ-MODERNIZATION-005:56`; note R-1 |
| 3 | Design and contracts | Stories and sprints | No | No story exists for S1; sprint-022 holds U-066 only |
| 4 | Design and contracts | Application code | Yes, not yet changed | `WorkManagerScheduler.kt:224-230`, KDoc, `BackupScheduler.java:74-77`, `App.java:379-380` |
| 5 | Design and contracts | Tests | Yes, not yet changed | `WorkManagerSchedulerTest.kt:377-384` and comments |
| 6 | Design and contracts | Documentation | No | no match in `README.md`, `sdlc/artifacts/engagement` |

### Fix Order

1. [ ] Design and contracts: amend CNTR-MODERNIZATION-004 (C-0 to C-11, version v3) — ROOT FIX
2. [ ] Design and contracts: amend DES-MODERNIZATION-005 (D-0 to D-7) — CASCADE FIX
3. [ ] Requirements: dated note on REQ-MODERNIZATION-005 (R-1) — CASCADE FIX
4. [ ] Requirements Reviewer: check that INV-4 still states its substance, the ruling matches the evidence, and no sentence is false in the interval
5. [ ] Stories: S1 is planned citing CR-015 (the code sites D4 omits, the retargeted test, the empty-diff evidence for the receiver, its manifest entry and the trigger worker) — CASCADE
6. [ ] Code and tests: S1 deletes the branch, rewords the three code sites and the test comments, retargets the test — CASCADE (S1, not this record)
7. [ ] Verification: the search under "Verification Evidence" shows only intended history in the contract, the design and REQ-005; after S1, AC-6's grep and the retargeted test confirm the code

### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| S1 (minSdk 24), not yet a story | none | not planned | Hold the plan until this record is applied; the plan then cites it |
| S4 (runtime libraries, WorkManager 2.12.0), not yet a story | none | not planned | Continue; keep the retargeted INV-4 test and the contract text unchanged (REQ-016 AC-8) |
| U-066 (TLS cipher suites) | sprint-022 | in progress | None: touches `mail/` and `k9mail-vendored/`, no scheduler file |

---

## Plan

- **Execution Window:** Inline. A text amendment applied right after approval and before S1 is planned. **Not a story to schedule.**
- **Assigned To:** Requirements Analyst (text), Requirements Reviewer (check)
- **Estimated Effort:** about 1 hour (19 changes in 2 files plus one note), plus the review
- **Sprint:** None

### Fix Checklist

- [x] Design and contracts: CNTR-MODERNIZATION-004 C-0 to C-11 (frontmatter, v3, changelog, INV-4, Versioning, example, Notes)
- [x] Design and contracts: DES-MODERNIZATION-005 D-0 to D-7 (frontmatter, lines 88, 238, 281, 317-333, 387, 444, Notes)
- [x] Requirements: REQ-MODERNIZATION-005 R-1 (frontmatter and dated note; no AC wording changed)
- [x] Requirements Reviewer: check the three files and this record's ruling; confirm no approved text is contradicted (done by a Design Auditor instead, because two of the three files are a contract and a design: `sdlc/artifacts/build/change-records/evidence/CR-015-applied-text-check.md`, PASS after Amendment 1)
- [x] Orchestrator: confirm the ruling "not a behavioural break" and the version step (v3 or v2.1) before the edits are applied (confirmed 2026-11-21: not a break; v3; Amendment 1 edits C-12, D-8, D-9 and R-1c also applied)
- [ ] S1 plan: cite CR-015, contract v3, the retargeted test name and the code sites D4 omits (the full list under "What story S1's plan must cite")
- [ ] Verification: the pre-24 phrases return only intended history in the three files (search below); after S1, AC-6 and AC-7 greps pass and the retargeted test is green

### Next steps (High severity)

The create-cr skill's wording for High is: "This should go into the current or next sprint. CR filed. Run
`/amp:create-stories --from-cr CR-015` to create a repair story." **That does not apply here.** This record is a text
amendment applied inline, not a story to schedule: apply edits C-0 to C-11, D-0 to D-7 and R-1, have the Requirements
Reviewer check them, then plan S1 citing this record. Do not run `/amp:create-stories --from-cr CR-015`; the code and test
work it makes possible is already story S1 of DES-MODERNIZATION-014 (D4). For the orchestrator: the record is
mandatory before S1 is planned (Record 14, row 9).

### Scope Concerns

1. **Scope Concern: the `SMS_RECEIVED` path stays unbound by the contract.** It is described by v3 (C-5) but not made a clause, and this record
   does not decide that. Whether the platform delivers `SMS_RECEIVED` to this non-exported manifest receiver (`AndroidManifest.xml:148`) is **not verified**
   (the architect's needs_check). The single emulator observation under Evidence 3 is consistent with either answer and was not designed to decide it.
   A bug is to be raised only on evidence that the receiver never fires. For BUG-021's Run R: if the receiver is not delivered, "no `SmsBroadcastReceiver`
   line" means nothing about the one-time-code case unless an ordinary SMS in the same run shows the line.
2. **Scope Concern: `BackupTriggerWorker` does not do what the contract says, and INV-4's debounce is barely tested.** The worker enqueues the
   delayed incoming backup itself (`BackupTriggerWorker.kt:61-71`), with no network constraint, no explicit backoff, and without the encryption-degraded and
   auto-backup checks that `scheduleIncoming()` applies (`WorkManagerScheduler.kt:135-148`), although the contract (`:378`: "it calls scheduler.scheduleIncoming()"),
   the worker's own KDoc (`:39`) and a test comment (`WorkManagerSchedulerTest.kt:537`) say it goes through `scheduleIncoming()`. No test references
   `BackupTriggerWorker`, none asserts the INCOMING delay length, and DES-005 `:503` ("INV-4 test asserts the debounce delay") is unmet. **A bug is raised by the
   orchestrator**, to be fixed before the runtime-library story (S4) so that REQ-016 AC-8's baseline includes the worker; neither S1 nor S4 absorbs it.
   **This record does not "correct" the contract's sentence to match the code**: the contract is likely the intended behaviour. The comments that say "via
   scheduleIncoming()" go to that bug, not to S1.
3. **Scope Concern: code sites and comments that design decision D4 does not list** go into S1's plan (see "What story S1's plan must cite"):
   `BackupScheduler.java:74-77`, `App.java:379-380`, `WorkManagerScheduler.kt:256` (log text), and `WorkManagerSchedulerTest.kt:34`, `:296-299` and
   `:371-376`. REQ-015 AC-6's grep (`SDK_INT|VERSION_CODES|TargetApi|RequiresApi`) will not find comments and javadoc. No dated note on DES-MODERNIZATION-014.
4. **Scope Concern: stale statements in CNTR-MODERNIZATION-004 that this record only partly touches.** Line 21 ("Draft; not finalized"), line 246's
   "has not been implemented against the v2 surface yet" (dropped by C-6: `scheduleManual` exists at `WorkManagerScheduler.kt:349`), line 404 ("currently reads ... nine
   operations") and line 403 (an absolute path in the Status note, against the path convention) are not about the content trigger and are left, except for the clause in C-6.
   A later contract revision should clear them.
5. **Scope Concern: the call-log half of the old INV-4 clause was never served below API 24.** No receiver exists for it (Evidence 3). The contract overstated the
   fallback. v3 removes the overstatement; nothing is lost. Raised in case the owner expects call-log-after-call to work on any level that
   does not have the content trigger (none is supported now).
6. **Scope Concern: whether Robolectric refuses `@Config(sdk = [23])` at minSdk 24** is not verified here (REQ-015 line 78 assumes it "cannot run"). It does not change the ruling:
   the branch is dead either way.

---

## Execution

### Changes Made

Applied 2026-11-21, artifacts first (option A; the code change is story S1 of DES-MODERNIZATION-014).

| File | Change | Layer |
|------|--------|-------|
| `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md` | C-0 to C-11 (v3, non-breaking), and C-12 (Amendment 1: the Status note) | Design and contracts |
| `sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md` | D-0 to D-7, and D-8, D-9 (Amendment 1) | Design and contracts |
| `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md` | R-1 (frontmatter entry and dated note; R-1b in its amended wording), and R-1c (Amendment 1) | Requirements |

Applied by a Solution Architect (contract and design) and a Requirements Analyst (requirement note). Checked by a Design Auditor: `sdlc/artifacts/build/change-records/evidence/CR-015-applied-text-check.md` (first verdict FAIL on four texts the record had not covered; "Verdict after Amendment 1: PASS").

### Verification Evidence

Run when the edits are applied. Before applying (2026-11-21), the search that must change:

```
grep -nE "pre-?24|API ?(≤|<=) ?23|API <= 23|below API 24|broadcast fallback|API 14.?23|SDK_INT >= 24" \
  sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-004-backup-scheduler.md \
  sdlc/artifacts/design/modernization/DES-MODERNIZATION-005-workmanager-scheduler-design.md \
  sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-005-migrate-scheduler-to-workmanager.md
```

Expected after the edits: only the history in C-5, C-7, C-8, C-9, C-11, D-4 ("History:"), D-7 and the R-1 note, and the unedited ADR-005-A lines
(DES-005 `:121`, `:183-193`). Also check that `change_records` lists `CR-015` in the three files and that the contract's Current version reads v3.

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| The commit with the subject "CR-015 applied: scheduler contract v3, pre-API-24 content-trigger fallback retired" (find it with `git log --grep`) | 2026-11-21 | sdlc/modernization-plan | The contract, the design, the requirement note, this record and the check report | Orchestrator (text by the Requirements Analyst and the Solution Architect, checked by the Design Auditor) |

---

## Not verified

- **Whether Robolectric refuses `@Config(sdk = [23])`** once minSdk is 24 (Scope Concern 6).
- **Whether `Constraints.Builder.addContentUriTrigger` is unchanged in WorkManager 2.12.0.** S4 planning reads the release notes.
- **That no other call-log trigger exists.** The search covered `AndroidManifest.xml` and `App.java` for three strings; the code was not
  traced for another route.
- **The test-count effect.** D4 says the count is unchanged (a retarget); not re-counted here.
- **The create-cr skill's own next-step wording was read; the process owner's rule for "applied inline" records was inferred from CR-011.**
- **Behaviour on a real device below API 24**: not exercised (none in use; Record 5 row 8).

---

## Closure

- **Root Cause Category:** A requirement change (the platform floor raised to API 24 by CR-011) was not carried into the contract and design that state a version-split behaviour
- **Prevention Recommendation:** When a requirement raises a platform floor, the change record for it lists every approved contract and design that states a version-split behaviour for the dropped levels, and says in the same record whether losing that split is a break of the contract. CR-011 listed the code branch and the test but not the contract, so the ruling arrived a day later through a design audit. Search `sdlc/artifacts/design/contracts` for `pre-?N` and `API <= n` wording in the same step as the code search.
- **Closed By:** Orchestrator, after the Design Auditor's delta check (PASS)
- **Date Closed:** 2026-11-21 (the artifact edits; the code change stays with story S1)
