---
artifact_type: qa-results
story_id: "U-011"
verdict: "PASS"
agent: "Developer (QA)"
timestamp: "2026-06-03"
ac_total: 12
ac_passed: 12
ac_failed: 0
tests_run: 389
tests_passed: 388
---

# QA Validation: U-011

## Verdict: PASS

## Acceptance Criteria Results

> **Rule**: Every AC marked PASS must cite at least one `file:line` reference in the Evidence column.

| AC | Status | Evidence (file:line references required) |
|----|--------|------------------------------------------|
| AC-1: SecretStore interface, 5 methods, no android imports | PASS | SecretStore.java:20-44; no android.*/androidx.* imports confirmed |
| AC-2: EncryptedPrefsSecretStore with AES256_GCM, AES256_SIV, file "credentials" | PASS | EncryptedPrefsSecretStore.java:57-72 (MasterKey.Builder); :73-78 (create call); CREDENTIALS_FILE_NAME="credentials" :41 |
| AC-3: get() returns null on absent/decrypt-failure, never throws | PASS | EncryptedPrefsSecretStore.java:84-96 (catch(Exception) returns null); InMemorySecretStoreTest.java:45 (null-on-absent test) |
| AC-4: Only 3 secret keys cross SecretStore boundary; oauth2_user/login_user excluded | PASS | AuthPreferences.java:163-168 (oauth2_user in prefs); grep confirms zero secretStore calls with "oauth2_user" or "login_user" as arguments |
| AC-5: Public typed-accessor signatures unchanged | PASS | AuthPreferences.java:147 (getOauth2Token), :151 (getOauth2RefreshToken), :158 (setOauth2Token), :178 (clearOauth2Data), :197 (setImapPassword), :305 (getImapPassword); AuthPreferencesTest.java:393-437 (9 new tests using these methods) |
| AC-6: getCredentials() removed; credentials field replaced by SecretStore | PASS | AuthPreferences.java has no `getCredentials()` method; grep "getSharedPreferences.*credentials" AuthPreferences.java = zero results; `private final SecretStore secretStore` at :35 |
| AC-7: InMemorySecretStore in src/test, HashMap-backed, no android imports, round-trip test | PASS | InMemorySecretStore.java:1-57 (no android imports); InMemorySecretStoreTest.java:28-57 (round-trip test) |
| AC-8: Two-arg constructor; single-arg delegates to buildEncryptedStoreSafe | PASS | AuthPreferences.java:116 (two-arg ctor); :96-108 (single-arg delegates via buildEncryptedStoreSafe()); AuthPreferencesTest.java:376 (injection test) |
| AC-9: File name "credentials"; backup_descriptor.xml unchanged | PASS | EncryptedPrefsSecretStore.java:41 (CREDENTIALS_FILE_NAME="credentials"); git diff backup_descriptor.xml = empty (no change) |
| AC-10: put() and remove() use commit(), zero apply() in EncryptedPrefsSecretStore | PASS | EncryptedPrefsSecretStore.java:106 (put .commit()); :116 (remove .commit()); :124 (clear .commit()); grep "\.apply()" EncryptedPrefsSecretStore.java = zero results |
| AC-11: All new production classes compile; test suite green | PASS | ./gradlew :app:testDebugUnitTest = BUILD SUCCESSFUL; 389 tests, 388 passed, 1 skipped |
| AC-12: androidx.security:security-crypto dependency declared | PASS | app/build.gradle:112 (`implementation 'androidx.security:security-crypto:1.1.0-alpha06'`) |

## Integration Path Verification

> **Rule**: Any new function, class, or component with no verified call path from a production entry point is a BLOCK finding.

| Component | Entry Point | Call Path | Verified (yes/no) |
|-----------|-------------|-----------|--------------------|
| EncryptedPrefsSecretStore | App.onCreate() | App.onCreate() → new Preferences(context).migrate() (Preferences.java:120) → new AuthPreferences(context) (Preferences.java:295) → buildEncryptedStoreSafe(context) → new EncryptedPrefsSecretStore(context) | yes |
| SecretStore.get(OAUTH2_TOKEN) | BackupTask / auth flow | AuthPreferences.getOauth2Token() → secretStore.get(OAUTH2_TOKEN) | yes |
| SecretStore.get(IMAP_PASSWORD) | BackupTask via getStoreUri() | AuthPreferences.getImapPassword() → secretStore.get(IMAP_PASSWORD) | yes |
| SecretStore.put(OAUTH2_TOKEN) | OAuth2 token callback | AuthPreferences.setOauth2Token() → secretStore.put(OAUTH2_TOKEN, ...) | yes |
| SecretStore.remove(OAUTH2_TOKEN) | OAuth2 clear | AuthPreferences.clearOauth2Data() → secretStore.remove(OAUTH2_TOKEN) | yes |
| InMemorySecretStore | Unit tests | new AuthPreferences(context, new InMemorySecretStore()) in AuthPreferencesTest.java:34 | yes (test path only) |
| PlaintextSharedPrefsSecretStore | Single-arg constructor fallback | buildEncryptedStoreSafe() catch block → new PlaintextSharedPrefsSecretStore(context) | yes (Robolectric test environment only) |

## Behavioral Contract Verification (CNTR-MODERNIZATION-003)

| Contract | Clause | Implementation (file:line) | Match (yes/no) |
|----------|--------|----------------------------|----------------|
| CNTR-MODERNIZATION-003 | 5-method interface | SecretStore.java:20-44 | yes |
| CNTR-MODERNIZATION-003 | get() null on absent | InMemorySecretStore.java:30 (HashMap.get null) | yes |
| CNTR-MODERNIZATION-003 | get() null on decrypt failure | EncryptedPrefsSecretStore.java:88-96 (catch returns null) | yes |
| CNTR-MODERNIZATION-003 | put() commit() semantics | EncryptedPrefsSecretStore.java:106 | yes |
| CNTR-MODERNIZATION-003 | remove() commit() | EncryptedPrefsSecretStore.java:116 | yes |
| CNTR-MODERNIZATION-003 | zero .apply() in adapter | EncryptedPrefsSecretStore.java (grep = 0 results) | yes |
| CNTR-MODERNIZATION-003 | Backing key "login_password" | AuthPreferences.java:197,305 (IMAP_PASSWORD constant) | yes |
| CNTR-MODERNIZATION-003 | Backing key "oauth2_token" | AuthPreferences.java:150,171,184 (OAUTH2_TOKEN constant) | yes |
| CNTR-MODERNIZATION-003 | Backing key "oauth2_refresh_token" | AuthPreferences.java:154,172,185 (OAUTH2_REFRESH_TOKEN constant) | yes |
| CNTR-MODERNIZATION-003 | No username keys through port | AuthPreferences.java: no secretStore call with "oauth2_user"/"login_user" | yes |
| CNTR-MODERNIZATION-003 | File name "credentials" (Option A) | EncryptedPrefsSecretStore.java:41 | yes |
| CNTR-MODERNIZATION-003 | backup_descriptor.xml unchanged | git diff = empty | yes |
| CNTR-MODERNIZATION-003 | AES-256-GCM master key | EncryptedPrefsSecretStore.java:61 (KeyScheme.AES256_GCM) | yes |
| CNTR-MODERNIZATION-003 | AES-256-SIV key encryption | EncryptedPrefsSecretStore.java:76 (PrefKeyEncryptionScheme.AES256_SIV) | yes |
| CNTR-MODERNIZATION-003 | AES-256-GCM value encryption | EncryptedPrefsSecretStore.java:77 (PrefValueEncryptionScheme.AES256_GCM) | yes |
| CNTR-MODERNIZATION-003 | Keystore-backed (StrongBox best-effort) | EncryptedPrefsSecretStore.java:63 (setRequestStrongBoxBacked(true)) | yes |
| CNTR-MODERNIZATION-003 | Public typed-accessor signatures unchanged | AuthPreferences.java:147,151,158,178,197,305 | yes |
| CNTR-MODERNIZATION-003 | null from get = "credential unavailable" | hasOAuth2Tokens() returns false when get() returns null; no NPE | yes |
| CNTR-MODERNIZATION-003 | migrateFromPlaintext() | Not implemented in this story (U-012 scope) | N/A (deferred) |

## Requirement Scope Coverage

| Requirement | Scope Item | Covered (yes/no) | Evidence (file:line) |
|-------------|------------|-------------------|----------------------|
| REQ-MODERNIZATION-004 | Credentials encrypted at rest (AES-256-GCM) | yes | EncryptedPrefsSecretStore.java:57-78 |
| REQ-MODERNIZATION-004 | Android Keystore master key | yes | EncryptedPrefsSecretStore.java:57-64 |
| REQ-MODERNIZATION-004 | Backup exclusion for credential file | yes | backup_descriptor.xml (unchanged, Option A) |
| REQ-MODERNIZATION-004 | Public API unchanged | yes | AuthPreferences typed-accessor signatures verified |
| REQ-MODERNIZATION-004 | Migration (plaintext → encrypted) | Partial — U-011 provides the seam; migration is U-012 | plan.md notes deferred scope |

## Test Results

- Command: `./gradlew :app:testDebugUnitTest` with `JAVA_HOME="C:/Users/Michael.Horsley/.jdks/jbr-17.0.14"`
- Result: **BUILD SUCCESSFUL**
- Tests: **389 total, 388 passed, 1 skipped** (AppTest.shouldGetVersionName is a known skip)
- New tests added: **18** (8 InMemorySecretStoreTest + 10 new AuthPreferencesTest U-011 tests)

Coverage gate:
- Command: `./gradlew :app:jacocoTestCoverageVerification`
- Result: **BUILD SUCCESSFUL** — 70% LINE coverage gate holds for `service*`, `mail*`, `auth*` packages

Assembly:
- Command: `./gradlew :app:assembleDebug`
- Result: **BUILD SUCCESSFUL**

## Regression Results

All 25 pre-existing `AuthPreferencesTest` tests (U-007 migrate() tests + U-006 characterization tests + testStoreUri/testStoreUriWithXOAuth2) continue to pass. The migrate() body was not modified.

## Phase Completion Report
---
story_id: "U-011"
phase: "validation"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-001/U-011/qa-results.md"
story_status: "done"
current_build_phase: "done"
ac_passed: 12
ac_total: 12
errors: []
---
