# Mutation log - U-068

Protocol: U-068 plan 4.8 (write-ahead entry, one Edit, one targeted run, restore by copying the pristine file, hash and clean check, `restored` line). Repository-relative paths.

START: `4300ffa98370b77dcc4dd8d12142079aada8313a`

Recovery note: if a session is lost while an entry says APPLYING without a restored line, nothing else runs until the file's git hash-object has been compared with git rev-parse HEAD:<file> and, if it differs, the file has been restored from its pristine copy or with git restore --source=HEAD --worktree -- <file>.

### M-B1 APPLYING 2026-11-21T11:53:29Z

- file: `app/src/main/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiver.java`; HEAD blob `b038d7fdb8b50d5cddd32ef1899736c29efc7274`; hash before `b038d7fdb8b50d5cddd32ef1899736c29efc7274`; main-clean before.
- edit: the opt-in gate removed: `if (new Preferences(context).isAllow3rdPartyIntegration()) {` becomes `if (true || new Preferences(context).isAllow3rdPartyIntegration()) {`
- expected: failing `onReceive_integrationOff_neverSchedules` (No interactions wanted, type A); passing `onReceive_integrationOn_schedulesImmediateOnce`

- run: exit=1; files=1 cases=2 pass=1 fail=1 error=0 skip=0; diffstat while mutated:  1 file changed, 1 insertion(+), 1 deletion(-)
- failing/erroring cases:
  - com.zegoggles.smssync.receiver.BackupBroadcastReceiverTest#onReceive_integrationOff_neverSchedules FAIL org.mockito.exceptions.verification.NoInteractionsWanted
- passing cases: 1
- messages: see `mutations/M-B1-message.txt`
- restored: 2026-11-21T11:54:54Z hash-ok main-clean (hash `b038d7fdb8b50d5cddd32ef1899736c29efc7274` equals HEAD blob)

- expected: matches (exactly the "off" test failed with NoInteractionsWanted, type A; the "on" test passed). verdict: detected.

### M-B2 APPLYING 2026-11-21T11:55:02Z

- file: `app/src/main/java/com/zegoggles/smssync/receiver/BackupBroadcastReceiver.java`; HEAD blob `b038d7fdb8b50d5cddd32ef1899736c29efc7274`; hash before `b038d7fdb8b50d5cddd32ef1899736c29efc7274`; main-clean before.
- edit: the `scheduleImmediate()` call removed: `getScheduler(context).scheduleImmediate();` becomes `if (false) getScheduler(context).scheduleImmediate();`
- expected: failing `onReceive_integrationOn_schedulesImmediateOnce` (Wanted but not invoked, type A); passing the "off" test

- run: exit=1; files=1 cases=2 pass=1 fail=1 error=0 skip=0; diffstat while mutated:  1 file changed, 1 insertion(+), 1 deletion(-)
- failing/erroring cases:
  - com.zegoggles.smssync.receiver.BackupBroadcastReceiverTest#onReceive_integrationOn_schedulesImmediateOnce FAIL org.mockito.exceptions.verification.WantedButNotInvoked
- passing cases: 1
- messages: see `mutations/M-B2-message.txt`
- restored: 2026-11-21T11:56:28Z hash-ok main-clean (hash `b038d7fdb8b50d5cddd32ef1899736c29efc7274` equals HEAD blob)

- expected: matches (exactly the "on" test failed with WantedButNotInvoked, type A; the "off" test passed). verdict: detected.

### M-R1 APPLYING 2026-11-21T12:05:28Z

- file: `app/src/main/java/com/zegoggles/smssync/compat/SmsReceiver.java`; HEAD blob `30b78e48aefc73fae08ab8f38cf7189bf859abdd`; hash before `30b78e48aefc73fae08ab8f38cf7189bf859abdd`; main-clean before.
- edit: the role branch made to throw: `RoleManager roleManager = ...ROLE_SERVICE);` is preceded by `if (true) throw new IllegalStateException("U-068 M-R1");`
- expected: failing `testOnReceive_q` and `testIsSmsBackupDefaultSmsApp_q_usesRoleManager` (IllegalStateException "U-068 M-R1", type X); passing the two `_belowQ` cases

- run: exit=1; files=1 cases=4 pass=2 fail=2 error=0 skip=0; diffstat while mutated:  1 file changed, 1 insertion(+), 1 deletion(-)
- failing/erroring cases:
  - com.zegoggles.smssync.compat.SmsReceiverTest#testIsSmsBackupDefaultSmsApp_q_usesRoleManager FAIL java.lang.IllegalStateException
  - com.zegoggles.smssync.compat.SmsReceiverTest#testOnReceive_q FAIL java.lang.IllegalStateException
- passing cases: 2
- messages: see `mutations/M-R1-message.txt`
- restored: 2026-11-21T12:07:08Z hash-ok main-clean (hash `30b78e48aefc73fae08ab8f38cf7189bf859abdd` equals HEAD blob)

- expected: matches (the two `_q` cases failed with IllegalStateException "U-068 M-R1", type X; the two `_belowQ` cases passed, so they never reach the role branch). verdict: detected.

### M-R2 APPLYING 2026-11-21T12:07:16Z

- file: `app/src/main/java/com/zegoggles/smssync/compat/SmsReceiver.java`; HEAD blob `30b78e48aefc73fae08ab8f38cf7189bf859abdd`; hash before `30b78e48aefc73fae08ab8f38cf7189bf859abdd`; main-clean before.
- edit: the package branch made to throw: `return context.getPackageName().equals(Sms.getDefaultSmsPackage(context));` is preceded by `if (true) throw new IllegalStateException("U-068 M-R2");`
- expected: failing `testOnReceive_belowQ` and `testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage` (IllegalStateException "U-068 M-R2", type X); passing the two `_q` cases

- run: exit=1; files=1 cases=4 pass=2 fail=2 error=0 skip=0; diffstat while mutated:  1 file changed, 1 insertion(+), 1 deletion(-)
- failing/erroring cases:
  - com.zegoggles.smssync.compat.SmsReceiverTest#testIsSmsBackupDefaultSmsApp_belowQ_usesDefaultSmsPackage FAIL java.lang.IllegalStateException
  - com.zegoggles.smssync.compat.SmsReceiverTest#testOnReceive_belowQ FAIL java.lang.IllegalStateException
- passing cases: 2
- messages: see `mutations/M-R2-message.txt`
- restored: 2026-11-21T12:08:44Z hash-ok main-clean (hash `30b78e48aefc73fae08ab8f38cf7189bf859abdd` equals HEAD blob)

- expected: matches (the two `_belowQ` cases failed with IllegalStateException "U-068 M-R2", type X; the two `_q` cases passed). verdict: detected. M-R1 and M-R2 together show which pair runs which branch (AC-4 (g), with the I5 line record).

### M-W1 APPLYING 2026-11-21T12:08:52Z

- file: `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt`; HEAD blob `0ae361ec16c09a3632eef1aff7c6d184344cf4c7`; hash before `0ae361ec16c09a3632eef1aff7c6d184344cf4c7`; main-clean before.
- edit: SMS URI trigger no longer for descendants: `.addContentUriTrigger(Consts.SMS_PROVIDER, true)` becomes `.addContentUriTrigger(Consts.SMS_PROVIDER, false)`
- expected: failing `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` and `inv4_contentTrigger_api29_smsUriPresent_withDescendants` ("expected to be true" on isTriggeredForDescendants, type A); every other case passes

- run: exit=1; files=1 cases=25 pass=23 fail=2 error=0 skip=0; diffstat while mutated:  1 file changed, 1 insertion(+), 1 deletion(-)
- failing/erroring cases:
  - com.zegoggles.smssync.service.WorkManagerSchedulerTest#inv4_contentTrigger_api29_smsUriPresent_withDescendants FAIL com.google.common.truth.AssertionErrorWithFacts
  - com.zegoggles.smssync.service.WorkManagerSchedulerTest#inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger FAIL com.google.common.truth.AssertionErrorWithFacts
- passing cases: 23
- messages: see `mutations/M-W1-message.txt`
- restored: 2026-11-21T12:11:10Z hash-ok main-clean (hash `0ae361ec16c09a3632eef1aff7c6d184344cf4c7` equals HEAD blob)

- expected: matches (exactly the two INV-4 tests that assert `isTriggeredForDescendants` failed, "expected to be true", type A; the call-log tests compare URIs only and passed). verdict: detected.

### M-W2 APPLYING 2026-11-21T12:11:17Z

- file: `app/src/main/java/com/zegoggles/smssync/scheduler/WorkManagerScheduler.kt`; HEAD blob `0ae361ec16c09a3632eef1aff7c6d184344cf4c7`; hash before `0ae361ec16c09a3632eef1aff7c6d184344cf4c7`; main-clean before.
- edit: the dead branch moved onto the floor: `if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {` becomes `if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {`
- expected: failing `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` ("expected not to be null", type A); every API 29 case and the U-054 case pass (the boundary mutant only the API 24 test sees)

- run: exit=1; files=1 cases=25 pass=24 fail=1 error=0 skip=0; diffstat while mutated:  1 file changed, 1 insertion(+), 1 deletion(-)
- failing/erroring cases:
  - com.zegoggles.smssync.service.WorkManagerSchedulerTest#inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger FAIL com.google.common.truth.AssertionErrorWithFacts
- passing cases: 24
- messages: see `mutations/M-W2-message.txt`
- restored: 2026-11-21T12:13:18Z hash-ok main-clean (hash `0ae361ec16c09a3632eef1aff7c6d184344cf4c7` equals HEAD blob)

- expected: matches (exactly `inv4_contentTrigger_atMinSdk24_enqueuesContentTrigger` failed, Truth "expected not to be: null", type A; the 24 other cases passed). verdict: detected.

### M-C1 APPLYING 2026-11-21T12:24:55Z

- file: `app/src/main/java/com/zegoggles/smssync/compat/SmsReceiver.java`; HEAD blob `30b78e48aefc73fae08ab8f38cf7189bf859abdd`; hash before `30b78e48aefc73fae08ab8f38cf7189bf859abdd`; main-clean before.
- edit: inside `disable`, `COMPONENT_ENABLED_STATE_DISABLED,` becomes `COMPONENT_ENABLED_STATE_ENABLED,` (disable no longer disables)
- expected: failing `testEnableThenDisable_togglesReceiverComponent` (Truth "expected: 2 ... but was: 1" on the second assertion, type A); passing the four retargeted cases

- run: exit=1; files=1 cases=5 pass=4 fail=1 error=0 skip=0; diffstat while mutated:  1 file changed, 1 insertion(+), 1 deletion(-)
- failing/erroring cases:
  - com.zegoggles.smssync.compat.SmsReceiverTest#testEnableThenDisable_togglesReceiverComponent FAIL com.google.common.truth.ComparisonFailureWithFacts
- passing cases: 4
- messages: see `mutations/M-C1-message.txt`
- restored: 2026-11-21T12:26:34Z hash-ok main-clean (hash `30b78e48aefc73fae08ab8f38cf7189bf859abdd` equals HEAD blob)

- expected: matches (exactly `testEnableThenDisable_togglesReceiverComponent` failed, Truth "expected: 2 ... but was : 1" on the second assertion, type A; the four retargeted cases passed). verdict: detected. No mutation runs after M-C1 (plan 4.8).

