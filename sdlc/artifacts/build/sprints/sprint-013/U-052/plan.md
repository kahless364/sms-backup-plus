---
artifact_type: plan
story_id: U-052
verdict: PASS
---
# U-052 Plan — GreenMail IMAP integration coverage + remove worker/transport exclusions (TE-002)
## Root cause
The riskiest code (K9MailTransport$BackupImapStoreDelegate, BackupWorker, RestoreWorker) was excluded from the coverage gate (assessment finding TE-002).
## Approach
1. Add GreenMail (`com.icegreen:greenmail*:2.1.3`) as `testImplementation`; pin specific SHA-256 entries in `gradle/verification-metadata.xml` (no wildcard; verify-metadata stays true).
2. Write JVM/Robolectric integration tests against an embedded GreenMail IMAP server covering the delegate (connect/create/open/append/fetch) and the worker doWork paths.
3. Remove the 3 jacocoFileFilter exclusions; verify the gate still passes with those classes included (mail*/service* ≥70%).
4. Honestly document any path still uncovered (e.g. backupCursors needs seeded SMS data) rather than re-excluding.
## Verdict
PASS — 26 integration tests; exclusions removed; BackupImapStoreDelegate 87%/RestoreWorker 80%/BackupWorker 56%; mail* 71.5%, service* 75.6%; build + dep-verification green; ACL boundary preserved.
