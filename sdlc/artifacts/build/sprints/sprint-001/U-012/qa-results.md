---
artifact_type: qa-results
story_id: "U-012"
verdict: "PASS"
agent: "QA Analyst"
timestamp: "2026-06-03"
ac_total: 6
ac_passed: 6
ac_failed: 0
tests_run: 545
tests_passed: 545
---

# QA Validation: U-012

## Verdict: PASS

## Acceptance Criteria Results

> **Rule**: Every AC marked PASS must cite at least one `file:line` reference in the Evidence column.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1: No plaintext credential remains post-migration | PASS | AuthPreferencesTest.java:u012_ac1_migration_clearsPlaintextAndEncryptsCredentials; InMemorySecretStore.java:88-99 (step 4 removes); EncryptedPrefsSecretStore.java:245-249 (step 4 legacy clear) |
| AC-2: Idempotent migration | PASS | AuthPreferencesTest.java:u012_ac2_migration_isIdempotent; InMemorySecretStore.java:79-80 (step 1 return); EncryptedPrefsSecretStore.java:207-209, 229-232 (step 1 checks) |
| AC-3: Interruption safety | PASS | AuthPreferencesTest.java:u012_ac3_migration_interruptionSafe_markerPresentPlaintextNotCleared; EncryptedPrefsSecretStore.java:196-252 (full step ordering) |
| AC-4: Fresh install only writes encrypted | PASS | AuthPreferencesTest.java:u012_ac4_freshInstall_writesOnlyToEncryptedStore; AuthPreferences.java:196-197 (setImapPassword routes to secretStore.put), AuthPreferences.java:162-173 (setOauth2Token routes to secretStore) |
| AC-5: Backup exclusion preserved (Option A) | PASS | EncryptedPrefsSecretStore.java:41 (CREDENTIALS_FILE_NAME = "credentials"); backup_descriptor.xml:4 (exclude credentials.xml unchanged) |
| AC-6: Public accessor signatures unchanged | PASS | AuthPreferences.java:149-154 (getOauth2Token/getOauth2RefreshToken unchanged); AuthPreferences.java:196 (setImapPassword unchanged); assembleDebug: BUILD SUCCESSFUL |

## Integration Path Verification

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| `SecretStore.migrateFromPlaintext()` | `App.onCreate()` | App.java:120 -> Preferences.java:295 -> AuthPreferences.java:346 -> secretStore.migrateFromPlaintext() | yes |
| `InMemorySecretStore.migrateFromPlaintext()` | Test injection via two-arg constructor | AuthPreferencesTest.java:@Before -> new AuthPreferences(context, secretStore) -> secretStore.migrateFromPlaintext() | yes |
| `EncryptedPrefsSecretStore.migrateFromPlaintext()` | Production path via single-arg constructor | AuthPreferences(context) -> buildEncryptedStoreSafe() -> new EncryptedPrefsSecretStore(context) -> migrate() -> secretStore.migrateFromPlaintext() | yes |

## Behavioral Contract Verification

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| CNTR-MODERNIZATION-003 | `migrateFromPlaintext()` on interface | SecretStore.java:71 | yes |
| CNTR-MODERNIZATION-003 | Step 1: idempotent marker check | EncryptedPrefsSecretStore.java:207, 229; InMemorySecretStore.java:79 | yes |
| CNTR-MODERNIZATION-003 | Step 2: buffer plaintext before encrypted store open | EncryptedPrefsSecretStore.java:215-220 (before getEncrypted() at 227) | yes |
| CNTR-MODERNIZATION-003 | Step 3: synchronous commit() | EncryptedPrefsSecretStore.java:242; InMemorySecretStore.java:93 (no-op comment) | yes |
| CNTR-MODERNIZATION-003 | Step 4: clear only after commit | EncryptedPrefsSecretStore.java:244-249; InMemorySecretStore.java:96-99 | yes |
| CNTR-MODERNIZATION-003 | Exact key `login_password` | EncryptedPrefsSecretStore.java:218, 238 | yes |
| CNTR-MODERNIZATION-003 | Exact key `oauth2_token` | EncryptedPrefsSecretStore.java:219, 239 | yes |
| CNTR-MODERNIZATION-003 | Exact key `oauth2_refresh_token` | EncryptedPrefsSecretStore.java:220, 240 | yes |
| CNTR-MODERNIZATION-003 | Marker key `__secretstore_migration_complete__` | EncryptedPrefsSecretStore.java:44 (MIGRATION_COMPLETE_KEY) | yes |
| CNTR-MODERNIZATION-003 | Option A: file name "credentials" | EncryptedPrefsSecretStore.java:41 | yes |
| CNTR-MODERNIZATION-003 | `put()` commit() semantics | EncryptedPrefsSecretStore.java:141 | yes |
| CNTR-MODERNIZATION-003 | `get()` null on decrypt failure | EncryptedPrefsSecretStore.java:113-118 | yes |
| CNTR-MODERNIZATION-003 | Consumer: null means unavailable | AuthPreferences.java:157-159 (hasOAuth2Tokens checks null) | yes |

## Requirement Scope Coverage

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| REQ-MODERNIZATION-004 | AC-2: one-time migration, rollback-safe | yes | EncryptedPrefsSecretStore.java:195-252 |
| REQ-MODERNIZATION-004 | AC-3: no plaintext remains post-migration | yes | EncryptedPrefsSecretStore.java:244-249 |
| REQ-MODERNIZATION-004 | AC-4: backup exclusion preserved | yes | EncryptedPrefsSecretStore.java:41; backup_descriptor.xml:4 |
| REQ-MODERNIZATION-004 | AC-5: public accessor signatures unchanged | yes | AuthPreferences.java:149-304 (all signatures identical) |
| REQ-MODERNIZATION-004 | AC-6: migration test + fresh-install test | yes | AuthPreferencesTest.java:u012_ac1..u012_ac4 |

## Test Results

- Tests run: 545 (546 total, 1 @Ignore'd)
- Tests passed: 545
- Tests failed: 0
- Tests skipped: 1 (`u012_robolectricSmoke` — @Ignore'd, Robolectric 4.12.x AndroidKeyStore limitation per U-011 AC-11)
- New tests added: 8 (7 active + 1 @Ignore'd)
  - u012_ac1_migration_clearsPlaintextAndEncryptsCredentials
  - u012_ac2_migration_isIdempotent
  - u012_ac3_migration_interruptionSafe_markerPresentPlaintextNotCleared
  - u012_ac4_freshInstall_writesOnlyToEncryptedStore
  - u012_ac6_hasOAuth2Tokens_returnsFalseWhenSecretStoreReturnsNull
  - u012_ic2_migrateCallsThroughToMigrateFromPlaintext
  - u012_robolectricSmoke_encryptedPrefsSecretStore_migrateFromPlaintext (@Ignore)

## Regression Results

All pre-existing tests from U-006, U-007, U-008, U-009, U-011 remain green.
`./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL.
`./gradlew :app:jacocoTestCoverageVerification` BUILD SUCCESSFUL (70% gate holds).
`./gradlew :app:assembleDebug` BUILD SUCCESSFUL (with JDK 17).

## Notes on @Ignore'd Smoke Test

`u012_robolectricSmoke_encryptedPrefsSecretStore_migrateFromPlaintext` tests the production
`EncryptedPrefsSecretStore` adapter directly. It is `@Ignore`d because Robolectric 4.12.x does
not shadow the AndroidKeyStore JCA provider — `EncryptedSharedPreferences.create()` throws
`NoSuchAlgorithmException`. This is the same limitation documented in U-011 Tech Notes (AC-11).
The test is preserved (not deleted) per story instructions so the intent survives for future CI
infrastructure upgrades when Robolectric shadow support lands.

## Phase Completion Report
---
story_id: "U-012"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-001/U-012/qa-results.md"
story_status: "done"
current_build_phase: "complete"
ac_passed: 6
ac_total: 6
errors: []
---
