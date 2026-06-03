# Wave 6 Smoke Test — PASS

**Date:** 2026-06-03 · **Device:** emulator-5554 (API 37 / Android 17)
**Build:** fresh debug APK from merged Wave-6 tree, 8.8 MB (added androidx.work 2.9.1 + androidx.security-crypto 1.1.0-alpha06)

| Check | Result |
|-------|--------|
| Install | Success |
| Launch / MainActivity | pid stable, 0 FATAL, resumed |
| UI render | Main screen "Idle" + BACKUP/RESTORE + settings list |
| SecretStore (U-011) | EncryptedPrefsSecretStore.<init> → AndroidKeyStore.generateKey ran on-device (the path Robolectric couldn't test) — credential encryption is live |
| WorkManager (U-014) | WM-WrkMgrInitializer "Initializing WorkManager with default configuration" — clean init |
| TLS factory (U-008) / MailTransport (U-025) | present, no construction crash |
| App errors | None (benign: Otto Bus.register warning, SQLite/PackageConfig OS noise, StrictMode disk-I/O debug) |

**Conclusion:** Wave 6 (TLS factory + SecretStore/EncryptedSharedPreferences + WorkManagerScheduler adapter + MailTransport ACL) is runtime-verified on a current Android image. Notably the on-device Keystore-backed credential encryption works — exercising a path that could not run under Robolectric. Evidence: wave6-launch-api37.png.

## Still pending live verification (need real Google account / not unit-testable)
- Actual backup/restore round-trip over the new TrustedSocketFactory + MailTransport (U-026 rewires engine; not yet)
- WorkManager production cutover (U-017) — currently Legacy scheduler is still the active binding
- Credential plaintext→encrypted migration on real upgrade (U-012, not yet built)
