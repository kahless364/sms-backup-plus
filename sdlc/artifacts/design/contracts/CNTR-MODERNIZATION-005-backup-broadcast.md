---
status: approved
artifact_type: interface-contract
consumers: []
related_requirements: []
related_design_docs: []
related_stories: []
change_records: []
id: CNTR-MODERNIZATION-005
title: 'Public com.zegoggles.smssync.BACKUP Broadcast Trigger'
domain: modernization
contract_type: 'event'
producer: 'external-third-party (uncontrolled: Tasker / MacroDroid / Automate / ADB)'
consumer: 'com.zegoggles.smssync — BackupBroadcastReceiver'
---

# CNTR-MODERNIZATION-005: Public `com.zegoggles.smssync.BACKUP` Broadcast Trigger

## Overview

This contract documents an **existing, published, externally-consumed public API**: the
implicit broadcast `Intent` with action string `com.zegoggles.smssync.BACKUP` that any
third-party app (Tasker, MacroDroid, Automate, automation scripts, ADB) sends to trigger an
on-demand SMS Backup+ backup. It is the project's only inbound integration surface for
external callers.

The contract exists in production today and is documented to end users in the project README
(`README.md:141-146`, "3rd party app integration"): *"If you want to trigger backups from
another app, enable `3rd party integration` in Advanced Settings and send the broadcast intent
`com.zegoggles.smssync.BACKUP`. This will work even when Auto Backup is disabled."* It is
classified **HARD-PRESERVE** in the modernization analysis (candidates.md:186;
migration-units.md:893; target-state.md:331,445; code-classification.md:179).

The reason this contract is written down now — when it is not new — is that two in-flight
modernization changes touch the receiver that implements it:

- **DES-MODERNIZATION-001 (MU-001 / Gate G0)** adds an explicit `android:exported="true"` to
  `BackupBroadcastReceiver` and removes its `tools:ignore="ExportedReceiver"` suppression, as a
  consequence of raising `targetSdk` to 35 (API 31+ requires explicit export on every
  intent-filtered receiver). Setting this attribute to `false` would silently delete the public
  API. (DES-001 §Manifest Conformance, AC-15..18/AC-20.)
- **DES-MODERNIZATION-005 (MU-005, "THE SPINE")** rewrites the receiver's *body* — replacing
  `new BackupJobs(context).scheduleImmediate()` with a call to the injected `BackupScheduler`
  port's `scheduleImmediate()` — as part of collapsing four schedulers into one WorkManager
  spine. (DES-005 §"Public broadcast contract preservation (AC-4)".)

This contract pins the **observable external behavior** so both changes can proceed without
breaking the third-party callers. Everything an external sender can observe — the action string,
the target package, the implicit-broadcast delivery semantics, the exported state, and the
user-gated activation — is the contract and **must not change**. Everything behind the receiver's
`onReceive` (which scheduler, which background-execution API) is implementation and may change
freely, provided the post-condition (a backup is enqueued) is preserved.

Verified by direct read this session: `app/src/main/AndroidManifest.xml` (full),
`app/src/main/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiver.java` (full),
`app/src/main/java/com/zegoggles/smssync/preferences/Preferences.java:178-179`,
`app/src/main/res/values/strings.xml:251-252`, `README.md:141-146`, plus DES-005 and DES-001 in
full.

## Contract Boundary

| Side | Role | Who owns it |
|------|------|-------------|
| **Producer / sender** | Any external Android app or shell that constructs and sends the `com.zegoggles.smssync.BACKUP` broadcast | **Third parties** — Tasker, MacroDroid, Automate, automation scripts, `adb shell am broadcast`. NOT owned by this project. |
| **Consumer / receiver** | `BackupBroadcastReceiver` — registered statically in the manifest, exported, which on matching action enqueues an immediate backup | **SMS Backup+** (`com.zegoggles.smssync`). Source: `app/src/main/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiver.java`; manifest registration `AndroidManifest.xml:151-156`. |

This is an inverted boundary relative to most CNTR artifacts: the **project is the consumer**,
and the **producers are external and uncontrolled**. The project therefore cannot coordinate a
breaking change with the senders — there is no central registry of who sends this broadcast. That
is precisely why the contract is unilateral and frozen: any change to the receiver's observable
surface is a silent break with no migration path for the callers. The only safe disposition is
byte-for-byte preservation (target-state.md classifies it "Stable — HARD-PRESERVE";
roadmap.md:565 notes consumers need no communication *unless it ever changes*).

## Contract Definition

This is an **event / message contract** (an Android implicit broadcast `Intent`).

```
Event:           com.zegoggles.smssync.BACKUP   (Android implicit broadcast Intent)
Action string:   "com.zegoggles.smssync.BACKUP"   ← FROZEN, verbatim, case-sensitive
Target package:  com.zegoggles.smssync
Receiver class:  com.zegoggles.smssync.receiver.BackupBroadcastReceiver
Manifest filter: <intent-filter>
                   <action android:name="com.zegoggles.smssync.BACKUP"/>
                   <category android:name="android.intent.category.DEFAULT"/>
                 </intent-filter>
Exported:        android:exported="true"  (required; see Validation Rules)
Permission:      none — the receiver declares no android:permission; any app may send
Payload/extras:  NONE — the action string is the entire message. No extras are read.
Delivery:        fire-and-forget. No result, no ordered broadcast, no acknowledgement.
Produced by:     external third-party apps / automation / ADB (not project-owned)
Consumed by:     BackupBroadcastReceiver.onReceive → (gated) BackupScheduler.scheduleImmediate()
```

### Receiver behavior (the post-condition the contract guarantees)

Verified against `BackupBroadcastReceiver.java` this session:

1. `onReceive(context, intent)` checks `BACKUP_ACTION.equals(intent.getAction())` where
   `BACKUP_ACTION = "com.zegoggles.smssync.BACKUP"` (line 28, 34). No extras are inspected.
2. If the action matches, it calls a private `backupRequested(context, intent)`.
3. `backupRequested` consults the user-controlled gate
   `new Preferences(context).isAllow3rdPartyIntegration()` (line 40), which reads SharedPreferences
   key `third_party_integration`, **default `false`** (`Preferences.java:178-179`).
   - If enabled: logs `"backup requested via broadcast intent"` and **enqueues an immediate
     backup** — today `new BackupJobs(context).scheduleImmediate()` (line 42); after MU-005,
     `scheduler.scheduleImmediate()` on the injected `BackupScheduler` port.
   - If disabled: logs `"backup requested via broadcast intent but ignored"` and does nothing
     (line 44). The broadcast is silently dropped — **no error, no toast, no exception**.

The user-facing gate is surfaced in Advanced Settings as preference `third_party_integration`,
labeled *"3rd party integration"* / *"Allow other apps to trigger backups via broadcast intents"*
(`strings.xml:251-252`, `preferences.xml:206-208`). The broadcast triggering a backup is
contingent on the user having opted in; this gate is part of the contract and must be preserved
across the migration.

### Canonical sender invocations

ADB (the documented reference invocation; used by the AC-4 instrumented test in DES-005 §"Public
broadcast contract — ADB test"):

```
adb shell am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync
```

The trailing `com.zegoggles.smssync` argument scopes the broadcast to the SMS Backup+ package
(equivalent to `-p com.zegoggles.smssync` / explicit package targeting), which is also the
correct, modern form for an app-targeted broadcast on API 26+ where unrestricted implicit
broadcasts are limited. Either the package-scoped form above or `-a com.zegoggles.smssync.BACKUP`
with an explicit package must continue to reach the receiver.

In-app sender (e.g., Tasker "Send Intent" action, or any third-party app):

```
Action:   com.zegoggles.smssync.BACKUP
Cat:      android.intent.category.DEFAULT   (or none — DEFAULT is satisfied by the filter)
Target:   Broadcast Receiver
Package:  com.zegoggles.smssync   (recommended on API 26+ to satisfy implicit-broadcast limits)
Extras:   none
```

Java/Kotlin sender (illustrative third-party caller):

```java
Intent i = new Intent("com.zegoggles.smssync.BACKUP");
i.setPackage("com.zegoggles.smssync");   // explicit package: required on API 26+
context.sendBroadcast(i);
```

## Versioning

- **Current version:** v1 (the only version; this action string has been the public trigger since
  the feature's introduction).
- **Breaking change policy:** This is a published external API consumed by uncontrolled third
  parties. The following are **breaking changes and are PROHIBITED** through the MU-001 and MU-005
  migrations (and indefinitely, absent a deliberate, communicated deprecation):
  - Changing the action string `com.zegoggles.smssync.BACKUP` (any character, case, rename,
    namespacing change).
  - Changing the target package `com.zegoggles.smssync`.
  - Setting `android:exported="false"` on `BackupBroadcastReceiver`, or removing the receiver,
    or removing/altering the `<action>` in its `<intent-filter>`.
  - Adding a sender-side `android:permission` requirement to the receiver, or otherwise requiring
    the sender to hold a permission, signature, or extra it does not have today (would break
    callers that send with no permission).
  - **Requiring** any `Intent` extra to be present (today none are read; making one mandatory
    breaks every existing sender).
  - Changing the post-condition: a matching broadcast received while
    `isAllow3rdPartyIntegration()` is `true` MUST still enqueue an immediate backup.
- **Backward-compatible (allowed) changes:** the receiver's *internal implementation* —
  specifically the MU-005 swap from `BackupJobs.scheduleImmediate()` to
  `BackupScheduler.scheduleImmediate()`, the underlying background-execution mechanism
  (JobDispatcher → WorkManager), the worker that runs the backup, logging text, and any
  *optional, ignored* extras a future version might read. These are invisible to senders and do
  not break the contract, provided the observable post-condition holds.

## Validation Rules

Both sides must honor:

1. **Action string is verbatim and frozen.** `com.zegoggles.smssync.BACKUP` — exact, case
   sensitive. The receiver constant `BackupBroadcastReceiver.BACKUP_ACTION` (line 28) and the
   manifest `<action android:name="...">` (`AndroidManifest.xml:153`) MUST remain this exact
   value and MUST remain equal to each other.
2. **`android:exported="true"` is required** (DES-001 hard requirement). On `targetSdk` 31+ an
   intent-filtered receiver MUST declare explicit `android:exported`; for this receiver the only
   non-breaking value is `true`, because the senders are external. The current manifest
   (`:151`) lacks the explicit attribute and carries `tools:ignore="ExportedReceiver"`; MU-001
   replaces this with `android:exported="true"` and removes the `tools:ignore`. Setting it to
   `false` would compile and pass lint but **silently delete the public API**.
3. **No sender permission.** The receiver MUST NOT add `android:permission` (none today). Adding
   one would break callers that send with no permission.
4. **User gate preserved.** The `third_party_integration` preference (default `false`) governs
   whether a received broadcast results in a backup. The gate behavior — opt-in, silent no-op when
   off — is part of the contract.
5. **Idempotent / safe under repeat.** Senders may fire the broadcast repeatedly (e.g., a Tasker
   profile). The receiver's `scheduleImmediate()` is single-flight under MU-005
   (`ExistingWorkPolicy.REPLACE` — DES-005 INV-1), so repeated triggers coalesce rather than
   stacking duplicate backups. Senders must not assume one-broadcast-equals-exactly-one-backup if
   they fire within a single backup's execution window.

## Error Handling

Across this boundary, errors are **not** propagated to the sender — by design, this is a
fire-and-forget implicit broadcast with no result mechanism:

- **Action does not match:** `onReceive` does nothing (guard at line 34). No effect.
- **Third-party integration disabled** (`isAllow3rdPartyIntegration() == false`, the default):
  the broadcast is silently ignored; only a debug log line is written
  (`"...but ignored"`, line 44). The sender receives no error. This is intended — it is the user's
  privacy gate, not a failure.
- **Backup enqueue/execution failure:** any failure of the enqueued backup (network down, auth
  expired, IMAP error) is surfaced through the app's own notification/state channels, **never back
  to the broadcast sender**. The broadcast contract guarantees *enqueue*, not *successful backup
  completion*.
- **Sender-side delivery:** if the broadcast is sent without targeting the package on API 26+,
  the OS may drop it under implicit-broadcast restrictions. This is an OS behavior, not an app
  error; the documented mitigation is the package-scoped invocation above.

The contract therefore guarantees a **post-condition (an immediate backup is enqueued when the
gate is open)**, not a synchronous response. There is no error channel to the external sender, and
none may be introduced (it would not be consumed by existing callers anyway).

## Example Payloads

The "payload" is the bare action string; there are no extras. Concrete examples:

**1. ADB (reference / AC-4 test invocation):**
```
$ adb shell am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync
Broadcasting: Intent { act=com.zegoggles.smssync.BACKUP pkg=com.zegoggles.smssync }
Broadcast completed: result=0
```
Expected effect (with `3rd party integration` enabled): an immediate backup work item is enqueued.
DES-005's AC-4 instrumented test asserts this via
`WorkManager.getWorkInfosForUniqueWork(<immediate/BROADCAST_INTENT unique name>)` showing an
enqueued item.

**2. Intent shape on the wire (no extras):**
```
Intent {
  action  = "com.zegoggles.smssync.BACKUP"
  package = "com.zegoggles.smssync"
  categories = [ android.intent.category.DEFAULT ]   // optional from sender; filter declares it
  extras  = (none)
}
```

**3. Negative example — gate closed (default state):**
```
$ adb shell am broadcast -a com.zegoggles.smssync.BACKUP com.zegoggles.smssync
Broadcast completed: result=0
```
The broadcast is *received* (result=0) but, because `third_party_integration` defaults to `false`,
no backup is enqueued and only the `"...but ignored"` debug log is written. result=0 means
*delivered*, not *acted on* — the sender cannot distinguish the two, by design.

## Dependencies

- **CNTR-MODERNIZATION-005-scheduler** (`BackupScheduler` port, core → app) — the receiver, after
  MU-005, fulfills this broadcast contract by calling `BackupScheduler.scheduleImmediate()`. The
  broadcast contract's post-condition (enqueue an immediate backup) is delegated to the
  `scheduleImmediate()` operation of that internal contract. This broadcast contract sits *in
  front of* the scheduler port: external senders see only this broadcast; the port is the internal
  realization. The two must be consistent — `scheduleImmediate()` MUST remain the no-network,
  REPLACE, immediate one-off that maps from the legacy `BROADCAST_INTENT` job (DES-005
  Integration Design table; `BackupJobs.java:197` `new int[0]` → `Constraints.NONE`).
- **DES-MODERNIZATION-001** — adds `android:exported="true"` and removes
  `tools:ignore="ExportedReceiver"` on `BackupBroadcastReceiver` (`AndroidManifest.xml:151`); the
  manifest precondition (rule 2) is satisfied by MU-001.
- **DES-MODERNIZATION-005** — rewrites the receiver body to call the port; preserves this contract
  byte-for-byte (DES-005 §"Public broadcast contract preservation (AC-4)").

## Notes

One-line: documents the EXISTING public `com.zegoggles.smssync.BACKUP` broadcast (action string, package, no-permission/no-extras delivery, exported=true, opt-in gate) as a HARD-PRESERVE external contract that MU-001 receiver-export and MU-005 WorkManager changes must keep verbatim.

**Status:** approved/finalized (2026-06-23). Reconciled metadata (title, contract_type=event, producer=external-third-party, consumer) and removed the stale "draft, not finalized" note — the MU-001 and MU-005 changes shipped (sprints 001–002) and the receiver in the current tree honors this contract verbatim (verified `AndroidManifest.xml:163-167` exported=true + action string; `Preferences.java:179` gate default `false`). Re-confirmed during the 2026-06-23 post-migration assessment (finding SE-001).
