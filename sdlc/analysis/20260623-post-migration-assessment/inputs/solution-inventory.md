# Solution Inventory — sms-backup-plus

Single-repo Android application (`com.zegoggles.smssync`) at repo root `.`. Post-modernization snapshot @ git `f540eccb` (branch `sdlc/modernization-plan`).

## Technologies & Frameworks
- **Build:** Gradle 8.14.1, AGP 8.7.3, JDK 17 (JBR), Kotlin 1.9.25 + kapt; `compileSdk`/`targetSdk` 35 (Android 15), `minSdk` 21.
- **DI:** Hilt 2.51.1 (`@HiltAndroidApp`, modules under `di/`, `@HiltViewModel`) — partial migration (4 `@Provides` + static-singleton aliasing remain, `TODO(U-022/U-023)`).
- **Background work:** WorkManager / CoroutineWorker (`service/BackupWorker.kt`, `RestoreWorker.kt`, schedulers); legacy Services (`SmsBackupService`/`SmsRestoreService`/`ServiceBase`) retained as foreground-notification + WorkInfo-bridge shells that delegate to the workers.
- **Mail transport:** vendored `:k9mail-vendored` module (k-9 fork frozen ~2015, `apache-mime4j 0.7.2`, `commons-io 2.4`, `org.apache.http.legacy`) behind a `MailTransport` ACL (`mail/transport/K9MailTransport.java`) — no `com.fsck.k9.*` leakage into `service.*`.
- **Security/crypto:** EncryptedSharedPreferences (`androidx.security-crypto 1.1.0-alpha06`), TLS trust manager + cert pinning (`mail/ssl/`), OAuth2 + IMAP username/password, RoleManager ROLE_SMS for restore.
- **State:** Flow-based `SyncStateRepository` (replaced Otto); `MainViewModel`.
- **Tests:** Robolectric 4.x, 672 `@Test` methods; jacoco per-package LINE ≥70% gate on `service*`/`mail*`/`auth*`. CI: `.github/workflows/ci.yml` (JDK 17, runs test/lint/jacocoTestReport/assembleRelease).

## Source layout (key packages under app/src/main/java/com/zegoggles/smssync/)
`activity/` (MainActivity, auth/), `service/` (workers, legacy services, state repo), `mail/` (transport ACL, converters), `preferences/` (EncryptedPrefsSecretStore), `di/` (Hilt modules), `calendar/`, `contacts/`, `compat/`, `utils/` (WindowInsetsUtil), `receiver/`. Vendored mail in `k9mail-vendored/`.

## Modernization history
EPIC-005 modernization + 15 resolved bugs (BUG-001..015) across sprints 001–011. All sprint plans `completed`.
