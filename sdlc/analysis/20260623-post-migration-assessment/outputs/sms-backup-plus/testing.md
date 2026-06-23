# Findings — Testing & Coverage (sms-backup-plus)

**Dimension:** Testing & Coverage · **Score: 78/100 · Grade: B−**
*Justification:* Strong, meaningful unit suite (672 tests) with a real gate and CI, but the gate is narrowed to 3 of ~13 packages and the highest-risk code (IMAP I/O, worker execution) is deliberately excluded and verified only on-device.

## Strengths
- **672 `@Test` methods** (authoritative `git grep` count) across ~70 files, well-distributed: `service/**`, `mail/**`, `auth/**`, `preferences/**`, `activity/**`, `di/**`, `receiver/**`.
- **Robolectric-based** so framework code runs in-JVM (e.g. `MainActivityRestoreTest` drives the real `MainActivity.startRestore()` and asserts the SMS-role request intent).
- **Tests assert real behavior** — `DefaultTrustedSocketFactorySniTest` verifies actual `SSLParameters`/`SNIHostName` state + IP-literal skip via `verify(never())`; `K9MailTransportCreateOpenTest` injects mock folders and verifies create/open/retry; `EncryptedPrefsSecretStoreDegradedTest` covers the keystore-degraded path.
- **Real CI** — `.github/workflows/ci.yml` runs `test lint jacocoTestReport assembleRelease` on every PR/push (JDK 17).

## Weaknesses / Gaps
- **TE-001 [High] Coverage gate covers only 3 packages** — `jacocoTestCoverageVerification` enforces LINE ≥70% on `service*`/`mail*`/`auth*` only; ~10 packages (`activity`, `preferences`, `calendar`, `contacts`, `di`, `receiver`, `scheduler`, `compat`, `utils`, `worker`) are ungated/unmeasured.
- **TE-002 [High] Critical classes excluded from the gate** (`jacocoFileFilter`) — `K9MailTransport$BackupImapStoreDelegate*` (real IMAP protocol/append/search) and `BackupWorker*`/`RestoreWorker*` (actual backup/restore execution) are exempt by design. The riskiest code is not coverage-gated.
- **TE-003 [Medium] Watermark test validates a re-implementation** — `BackupWorkerWatermarkTest` runs a local `processWatermarkLoop` helper that *replicates* `BackupWorker.backupCursors`; it proves the invariant's intent but would not catch a regression in the real worker body.
- **TE-004 [Medium] No `androidTest`/instrumentation suite** — no on-device/Espresso tests, no integration test against a real/embedded IMAP server; the end-to-end backup/restore path is validated only manually on-device.

## Improvement Items
1. Expand the coverage gate to all packages (even at a lower floor, e.g. 50%) so untested code becomes visible. *(S-M, high)*
2. Add IMAP integration coverage (GreenMail/embedded IMAP in JVM/Robolectric) so `BackupImapStoreDelegate` + worker execution can be covered and the gate exclusions removed. *(M-L, high)*
3. Test the real `BackupWorker` body via an extracted testable seam, not a copied helper. *(M, med)*
