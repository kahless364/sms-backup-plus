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

