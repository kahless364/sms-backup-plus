# Findings — Security & Data Integrity (sms-backup-plus)

**Dimension:** Security & Data Integrity · **Score: 91/100 · Grade: A−**
*Justification:* Mature, defense-in-depth posture — Keystore-backed credential encryption, fail-closed TLS with hostname verification + pinning, tokens off the IPC surface, and a watermark that provably cannot advance on a failed upload. One exported-receiver gap and a couple of low-severity items keep it just shy of A.

## Strengths
- **Credentials encrypted at rest** — `EncryptedSharedPreferences` (AES-256-GCM values, AES-256-SIV keys, StrongBox-best-effort MasterKey) `EncryptedPrefsSecretStore.java:109-120`; `commit()` for durability.
- **Rollback-safe migration** — plaintext buffered, ciphertext committed, plaintext cleared only after commit (`:273-285`); Keystore failure sets a persisted `ENCRYPTION_DEGRADED` flag and retries next launch (`:250-262`).
- **TLS fail-closed** — platform `checkServerTrusted` + `StrictHostnameVerifier.verify()` (`TrustManagerFactory.java:60-86`); no trust-all path; pinned factory rejects empty chains/mismatch/expiry (`PinnedCertificateSocketFactory.java:128-149`); SNI skips IP-literals per RFC 6066 (`DefaultTrustedSocketFactory.java:216-254`, BUG-015).
- **OAuth token off IPC** (BUG-007) — persisted via `AuthPreferences.setOauth2Token()` before `setResult()`; intent carries only account name (`AccountManagerAuthActivity.java:159-170`).
- **Watermark integrity** (BUG-010) — `setMaxSyncedDate` is fed only the `appendMessages` return; a throw exits before the watermark write (`BackupWorker.kt:334-345`).
- **Backup exclusion + secret redaction** — `credentials.xml`/`pinned_certs.xml` excluded from cloud backup; token logs masked + DEBUG-gated. R8 on release.

## Weaknesses / Risks
- **SE-001 [Low — accepted, contract-governed] `BackupBroadcastReceiver` exported with no permission** (`AndroidManifest.xml:163-167`) — any app can fire `com.zegoggles.smssync.BACKUP`. **Resolution (post-review):** this is a *documented public API*, not a defect. `CNTR-MODERNIZATION-005` (status: approved) classifies it HARD-PRESERVE and **explicitly PROHIBITS** both `exported="false"` and adding any `android:permission`/signature requirement — either would silently break the third-party integration (Tasker/MacroDroid/ADB) the feature exists to provide. The actual security control is the **opt-in gate** `third_party_integration` (default `false`, `Preferences.java:179`): a broadcast is a silent no-op unless the user explicitly enabled it. Severity downgraded Medium→Low (accepted). The only contract-compatible improvement is a UX-copy clarification of the opt-in toggle (see improvement #2).
- **SE-002 [Low] `ENCRYPTION_DEGRADED` flag is informational only** — `isEncryptionDegraded()` exists (`:315`) but has no caller gating/surfacing backup; degraded (plaintext-on-disk) state is not enforced. **(NEW — completes BUG-008.)**
- **SE-003 [Low] `security-crypto` pinned to `1.1.0-alpha06`** — alpha dep on the credential-encryption critical path (supply-chain/stability).
- **SE-004 [Low] No `networkSecurityConfig`** — cleartext policy relies on transport code rather than a manifest-level `cleartextTrafficPermitted=false`.
- **SE-005 [Info] `allowBackup="true"`** — intentional and safe given file-level excludes; `debuggable` correctly unset.

## Improvement Items
1. Wire `isEncryptionDegraded()` into a backup gate + user-visible warning so plaintext-credential state actually blocks/flags sync. *(S-M, med)*
2. ~~Constrain `BackupBroadcastReceiver` with a custom `signature` permission~~ — **PROHIBITED by CNTR-MODERNIZATION-005** (would break the public broadcast API). Contract-compatible alternative only: clarify the third-party-trigger risk in the `third_party_integration` opt-in UI copy (`strings.xml`). *(S, low)*
3. Move `security-crypto` to a stable release; add a regression test pinning the encryption scheme. *(S, low)*
4. Add a manifest `networkSecurityConfig` with `cleartextTrafficPermitted=false`. *(S, low)*

## Prior-fix confirmation
BUG-007 (token off IPC), BUG-008 (degraded migration — flag set, but see SE-002), BUG-009 (Q+ role), BUG-010 (watermark), BUG-015 (SNI IP guard) all confirmed present in the current code.
