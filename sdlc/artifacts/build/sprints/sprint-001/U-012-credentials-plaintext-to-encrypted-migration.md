---
type: story
status: planned
sprint: "000001"
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-004
design_docs:
  - DES-MODERNIZATION-004
integration_contracts:
  - CNTR-MODERNIZATION-003
dependencies:
  - U-011
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-012
title: Idempotent plaintext-to-encrypted credential migration on first launch after upgrade
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-012: Idempotent Plaintext-to-Encrypted Credential Migration on First Launch After Upgrade

## Story

As a user upgrading SMS Backup+ from a version that stored credentials in plaintext,
I want the app to automatically and safely migrate my stored IMAP password and OAuth2 tokens to encrypted storage on first launch after upgrade,
so that my Gmail credentials are protected at rest by AES-256-GCM encryption immediately and without any action on my part, while a process crash at any point during migration can never leave me unable to authenticate.

## Acceptance Criteria

- [ ] **AC-1 — No plaintext credential value remains in any on-device preferences file after a completed migration**

  Given a device that has upgraded from a version where `login_password`, `oauth2_token`, and/or `oauth2_refresh_token` were stored as plaintext strings in the `credentials` `SharedPreferences` file,
  when `migrateFromPlaintext()` executes to completion (the `__secretstore_migration_complete__` marker is present in the encrypted store and the step-4 plaintext clear has committed),
  then reading the raw `SharedPreferences("credentials", MODE_PRIVATE)` for the keys `login_password`, `oauth2_token`, and `oauth2_refresh_token` returns `null` for every key that held a plaintext value before migration; a test using `InMemorySecretStore` seeds a plain `HashMap`-backed legacy store with all three keys, runs migration, and asserts all three keys return `null` from the legacy map after migration completes.

- [ ] **AC-2 — Migration is idempotent: running it a second time produces no exception and leaves values and the marker stable**

  Given a device on which `migrateFromPlaintext()` has already completed successfully (the `__secretstore_migration_complete__` marker is present in the encrypted store),
  when `migrateFromPlaintext()` is called again,
  then the method returns immediately without writing any key or throwing any exception; the `__secretstore_migration_complete__` marker is still present exactly once; each of the three secret values readable through `SecretStore.get()` returns the same value it held after the first migration run; a test runs migration twice in sequence and asserts stable values and a single marker.

- [ ] **AC-3 — Migration is interruption-safe: a simulated crash between the step-3 commit and the step-4 plaintext clear leaves credentials reachable and migration completable on next launch**

  Given a device on which the encrypted store has been durably committed with all three secrets and the `__secretstore_migration_complete__` marker (step 3 has completed) but the plaintext clear (step 4) has not yet executed — simulated in test by writing the marker and secrets into the `InMemorySecretStore` but leaving the legacy backing map intact —
  when `migrateFromPlaintext()` is called on next launch,
  then the method detects the marker, returns immediately, and all three secrets are reachable through `SecretStore.get()`; a test seeds this mid-flight state and asserts secrets are intact, no exception is thrown, and after the test the plaintext keys are eventually cleared (or already absent in the Option A same-file case); the safety invariant "no window exists in which both stores lack a secret" is never violated.

- [ ] **AC-4 — Fresh install: no plaintext credential is ever written; the first write of each secret key goes directly through the encrypted store**

  Given a device on which SMS Backup+ is freshly installed (no pre-existing `credentials.xml` or any prior `SharedPreferences` for the `credentials` file name),
  when `setImapPassword()` is called with any non-null password value, or `setOauth2Token()` is called with any non-null access and refresh token values,
  then `SecretStore.get("login_password")` returns the stored password value, `SecretStore.get("oauth2_token")` returns the stored access token, and `SecretStore.get("oauth2_refresh_token")` returns the stored refresh token; reading the raw `SharedPreferences("credentials", MODE_PRIVATE)` for those keys returns `null`; a test using `InMemorySecretStore` (no legacy entries seeded) exercises both `setImapPassword()` and `setOauth2Token()` and asserts round-trip correctness through the encrypted store and absence of plaintext entries.

- [ ] **AC-5 — The `credentials.xml` backup-exclusion invariant is preserved: the backing file for the encrypted store is never included in Android auto-backup**

  Given that `app/src/main/res/xml/backup_descriptor.xml` contains `<exclude domain="sharedpref" path="credentials.xml"/>` wired through `AndroidManifest.xml android:fullBackupContent="@xml/backup_descriptor"`,
  when the encrypted store's backing `SharedPreferences` file is created by `EncryptedPrefsSecretStore`,
  then: if **Option A** (same file name `"credentials"`) is chosen, `backup_descriptor.xml` requires zero edits and the existing exclude rule matches the encrypted file as verified by a code reviewer confirming the file name string literal `"credentials"` in the adapter; if **Option B** (file name `"credentials_enc"`) is chosen, an `<exclude domain="sharedpref" path="credentials_enc.xml"/>` line has been added to `backup_descriptor.xml` as mandatory scope of this story, and the existing `credentials.xml` exclude is also retained; in either case, no file holding any of the three secret keys (in plaintext or ciphertext) appears in a successful Android auto-backup; the implementing developer MUST confirm the chosen option in the PR description and reference this AC explicitly.

- [ ] **AC-6 — The public typed-accessor signatures on `AuthPreferences` are unchanged: existing callers compile and behave identically**

  Given the post-migration `AuthPreferences` class in which `getCredentials()` (`:221-226`) now delegates to `SecretStore` instead of calling `context.getSharedPreferences("credentials", MODE_PRIVATE)` directly,
  when the following public methods are invoked with the same arguments as before: `getOauth2Token()` (`:72`), `getOauth2RefreshToken()` (`:76`), `setOauth2Token(String username, String accessToken, String refreshToken)` (`:85`), `clearOauth2Data()` (`:98`), `setImapPassword(String password)` (`:119`), and private `getImapPassword()` (`:236`),
  then their return types, parameter types, parameter names, checked-exception declarations, and observable behavior under `InMemorySecretStore` are identical to the pre-migration signatures; the project compiles without error (`./gradlew :app:assembleDebug` is green); no call site outside `AuthPreferences.java` is modified; a test that calls each typed accessor and checks round-trip values passes without modification to the test's signature expectations.

### Integration Criteria

- [ ] **IC-1** — `EncryptedPrefsSecretStore` is bound to the `SecretStore` interface via the Phase-1 manual construction path (mirroring the existing `AuthPreferences` dual-constructor Humble-Object seam at `AuthPreferences.java:67-70`); the production binding is confirmed by the app launching on a device or emulator and executing `migrateFromPlaintext()` without crashing.

- [ ] **IC-2** — `AuthPreferences.migrate()` (`:276-288`, already rewritten by U-007 to remove the trust-all write) is extended to call `secretStore.migrateFromPlaintext()` as a new step within the existing `migrate()` method body; the call is confirmed present by code review and by the migration running on first launch of a fresh APK install over an existing data directory.

- [ ] **IC-3** — `Preferences.migrate()` (`preferences/Preferences.java:294-296`), which calls `new AuthPreferences(context).migrate()` on every `App.onCreate()`, continues to compile and execute without error after this story's changes; confirmed by a full `./gradlew :app:testDebugUnitTest` run that is green.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | `getCredentials()` (`:221-226`) returns `context.getSharedPreferences("credentials", MODE_PRIVATE)` — raw plaintext `SharedPreferences`; `migrate()` (`:276-288`) has no credential-encryption step (post-U-007: trust-all path removed, notice flag added, but secrets still plaintext) | Redirect `getCredentials()` to delegate to injected `SecretStore`; insert `secretStore.migrateFromPlaintext()` call into `migrate()` body; inject `SecretStore` via the dual-constructor seam |
| `app/src/main/java/com/zegoggles/smssync/preferences/SecretStore.java` | Does not exist (introduced by U-011) | Consumed by `AuthPreferences` via the `getCredentials()` seam; `migrateFromPlaintext()` entry point called from `migrate()` |
| `app/src/main/java/com/zegoggles/smssync/preferences/EncryptedPrefsSecretStore.java` | Does not exist (introduced by U-011) | Production adapter; holds the `migrateFromPlaintext()` implementation with the rollback-safe step ordering |
| `app/src/main/res/xml/backup_descriptor.xml` | Contains `<exclude domain="sharedpref" path="credentials.xml"/>` | No change required under Option A; under Option B must add `<exclude domain="sharedpref" path="credentials_enc.xml"/>` — implementer MUST confirm chosen option |
| `app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java` | Contains characterization tests from U-006 and rewrite tests from U-007; no migration path tests | Add migration-path test, idempotency test, interruption-safety test, and fresh-install test using `InMemorySecretStore` |

## Existing Behavior to Preserve

- All public typed-accessor methods on `AuthPreferences` — `getOauth2Token()`, `getOauth2RefreshToken()`, `setOauth2Token()`, `clearOauth2Data()`, `setImapPassword()`, and private `getImapPassword()` — must have identical signatures and behavior before and after this story.
- `hasOAuth2Tokens()` (`:80`) and `isLoginInformationSet()` (`:153`), which treat a `null` return as "not set", must continue to work correctly when `SecretStore.get()` returns `null` for a missing or undecryptable key; behavior is preserved because `null` semantics are identical to the current `getString(key, null)` idiom.
- `Preferences.migrate()` calling `new AuthPreferences(context).migrate()` on `App.onCreate()` must compile and execute without error.
- The U-007 migrate() rewrite (trust-all removal, stale-true clear, `transport_security_notice_pending` flag, `apply()` flush) must remain entirely intact; this story only adds a `secretStore.migrateFromPlaintext()` call inside `migrate()`, it does not replace the U-007 logic.
- The `credentials.xml` backup exclusion must remain in force; the file holding any of the three secret keys must never appear in Android auto-backup output.
- The pre-existing `AuthPreferencesTest` characterization tests (U-006) and rewrite tests (U-007) must remain green; run `./gradlew :app:testDebugUnitTest` and confirm zero regressions.
- `oauth2_user` (`:32`) and `login_user` (`IMAP_USER`, `:36`) are NOT secrets, do NOT pass through `SecretStore`, and continue to be stored in the default plaintext `SharedPreferences` (`preferences` field, `:69, 87, 124, 212`) unchanged.

## Verification Steps

1. **AC-1 (no plaintext post-migration):** In `AuthPreferencesTest`, run the migration-path test: seed the `InMemorySecretStore`'s legacy-side map with `login_password = "app-password"`, `oauth2_token = "access"`, `oauth2_refresh_token = "refresh"`; call `secretStore.migrateFromPlaintext()`; then assert `legacyMap.get("login_password") == null`, `legacyMap.get("oauth2_token") == null`, `legacyMap.get("oauth2_refresh_token") == null`; and assert `secretStore.get("login_password").equals("app-password")`, `secretStore.get("oauth2_token").equals("access")`, `secretStore.get("oauth2_refresh_token").equals("refresh")`; run `./gradlew :app:testDebugUnitTest --tests "*AuthPreferencesTest*"` and confirm the test passes.

2. **AC-2 (idempotency):** In `AuthPreferencesTest`, run the idempotency test: seed legacy map with all three keys; call `migrateFromPlaintext()` once; record the three `secretStore.get()` values; call `migrateFromPlaintext()` again; assert no exception; assert the three values are unchanged; assert `secretStore.contains("__secretstore_migration_complete__")` is `true` after both calls; confirm the test passes.

3. **AC-3 (interruption safety):** In `AuthPreferencesTest`, seed the `InMemorySecretStore`'s encrypted-side map directly with all three secrets and the `__secretstore_migration_complete__` marker (simulating a completed step-3 but uncompleted step-4 state); also leave the legacy map intact; call `migrateFromPlaintext()`; assert the method returns without throwing; assert all three secrets are reachable via `secretStore.get()`; confirm the test passes.

4. **AC-4 (fresh install):** In `AuthPreferencesTest`, construct `AuthPreferences` with an `InMemorySecretStore` that has an empty legacy map and an empty encrypted map; call `setImapPassword("test-pw")` and `setOauth2Token("user@example.com", "at", "rt")`; assert `secretStore.get("login_password").equals("test-pw")`, `secretStore.get("oauth2_token").equals("at")`, `secretStore.get("oauth2_refresh_token").equals("rt")`; assert `legacyMap.containsKey("login_password") == false`; confirm the test passes.

5. **AC-5 (backup exclusion):** Open `app/src/main/res/xml/backup_descriptor.xml` and confirm it contains an `<exclude>` rule matching the file name used by `EncryptedPrefsSecretStore`; if Option A was chosen, confirm the file-name string literal in the adapter is `"credentials"`; if Option B was chosen, confirm `<exclude domain="sharedpref" path="credentials_enc.xml"/>` is present; the developer must state the chosen option in the PR description. On a debug build, run `adb backup -apk com.zegoggles.smssync` and confirm `shared_prefs/credentials.xml` (or the Option-B equivalent) does not appear in the backup archive.

6. **AC-6 (accessor signatures unchanged):** Run `./gradlew :app:assembleDebug` from the repo root; confirm zero compilation errors; confirm no call site outside `AuthPreferences.java` was modified; open `AuthPreferences.java` and confirm the method signatures for `getOauth2Token()`, `getOauth2RefreshToken()`, `setOauth2Token()`, `clearOauth2Data()`, `setImapPassword()`, and `getImapPassword()` are byte-for-byte identical to the pre-story source; run `./gradlew :app:testDebugUnitTest` and confirm all pre-existing `AuthPreferencesTest` cases are green.

7. **IC-2 (migrate() call present):** Open the post-story `AuthPreferences.migrate()` method body; confirm a call to `secretStore.migrateFromPlaintext()` appears after the `useXOAuth()` early-return guard; confirm the U-007 logic (stale-true clear, notice flag, protocol normalization, `apply()` flush) is intact and unmodified; run `./gradlew :app:testDebugUnitTest` and confirm all migration tests pass.

8. **Full regression gate:** Run `./gradlew :app:testDebugUnitTest` from the repo root; confirm zero test failures across the full suite; confirm the U-006 characterization tests and U-007 rewrite tests are both still green.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java) | Implement `migrateFromPlaintext()` rollback-safe migration in `EncryptedPrefsSecretStore`; redirect `AuthPreferences.getCredentials()` seam to `SecretStore`; insert migration call in `migrate()`; wire Phase-1 manual construction; author all four test cases in `AuthPreferencesTest` using `InMemorySecretStore`; confirm backup-exclusion option and update `backup_descriptor.xml` if Option B | Developer |

## Technical Context

- **Dependency on U-011:** U-011 introduces the `SecretStore` interface, `EncryptedPrefsSecretStore` adapter, and `InMemorySecretStore` test fake. This story MUST NOT be started until U-011 is complete and merged. The `SecretStore` interface definition — `get(String)`, `put(String, String)`, `remove(String)`, `contains(String)`, `clear()`, `migrateFromPlaintext()` — is the fixed contract defined in CNTR-MODERNIZATION-003; this story must not modify it.

- **Dependency on U-007:** U-007 rewrites `AuthPreferences.migrate()` to remove the trust-all write, clear stale trust-all, write the notice flag, normalize the protocol string, and use `apply()`. This story adds a `secretStore.migrateFromPlaintext()` call inside that already-rewritten method. The developer MUST confirm the U-007 migrate() rewrite is merged to master before modifying `migrate()` again, to honor the MU-003 (U-007) → MU-004 (this story) edit-ordering constraint in DES-MODERNIZATION-004 §Integration Design and REQ-MODERNIZATION-004 Constraints.

- **Rollback-safe migration step ordering — the critical implementation detail:** The exact ordering in `migrateFromPlaintext()` mandated by CNTR-MODERNIZATION-003 §Validation Rules §One-time migration semantics is non-negotiable:
  1. If `encrypted.contains("__secretstore_migration_complete__")` → return immediately (idempotent short-circuit).
  2. Open raw `SharedPreferences("credentials", MODE_PRIVATE)` and read the three legacy plaintext values into local `String` variables (`String pw`, `String at`, `String rt`).
  3. Open the encrypted store's `Editor`; write each non-null value via `put()`; write `put("__secretstore_migration_complete__", "1")`; call `commit()` synchronously — this is the **durability barrier**.
  4. ONLY after step-3 `commit()` returns successfully: clear the legacy plaintext entries with their own `commit()` call.
  The safety invariant is: **plaintext is never cleared until ciphertext is durably committed.** A process kill between step 3 and step 4 leaves the marker and secrets in the encrypted store, so the next launch short-circuits at step 1 — the user is never unable to authenticate.

- **Option A vs Option B — file-name reconciliation (MUST be resolved before merge):** DES-MODERNIZATION-004 §Migration "File-name reconciliation" and CNTR-MODERNIZATION-003 §"Backing-store invariant" define two valid options. Option A is strongly preferred: pass the file name `"credentials"` to `EncryptedSharedPreferences.create()` — the backing file on disk is still `credentials.xml`, the existing `<exclude>` rule in `backup_descriptor.xml` matches with zero edits (AC-5 satisfied for free). The implementation risk for Option A is ordering: the legacy plaintext read (step 2) MUST complete and the local variables be populated BEFORE the `EncryptedSharedPreferences` store is constructed over the same file name, because the library must not attempt to decrypt pre-existing plaintext entries; constructing the encrypted store causes it to read the file. Buffer the values first; then create the encrypted store; then write ciphertext. If Option B is chosen instead (distinct file name `"credentials_enc"`), adding `<exclude domain="sharedpref" path="credentials_enc.xml"/>` to `backup_descriptor.xml` is **mandatory scope of this story**, not optional.

- **`migrateFromPlaintext()` call site inside `migrate()`:** The call should appear after the `useXOAuth()` early-return guard (line 276 post-U-007) so OAuth2-only users skip it, consistent with the existing pattern. It must appear before the `edit.apply()` flush of the TLS prefs so that a crash in the TLS path cannot leave the credential migration half-done without having run at all. The developer should consult the exact method body produced by U-007 before inserting the call.

- **`put()` uses `commit()` semantics (not `apply()`):** CNTR-MODERNIZATION-003 §Contract Definition notes that `put` MUST be synchronous and durable (`commit()` semantics) because the migration durability barrier depends on it. The step-3 barrier `commit()` in the `InMemorySecretStore` fake is a no-op but must not be changed to `apply()` in the production adapter. This is a contract clause — weakening it from `commit()` to `apply()` is a breaking contract change.

- **`null` on decrypt failure:** The `EncryptedPrefsSecretStore.get()` method must catch `AEADBadTagException`, `InvalidKeyException`, and related `GeneralSecurityException`/`IOException` from store construction or read and surface them as `null` — never as a thrown exception. This is already specified in U-011 (adapter contract) but the test in this story should confirm that `AuthPreferences.hasOAuth2Tokens()` (`:80`) returns `false` when `SecretStore.get()` returns `null`, preserving the same null-means-absent semantics as the current `getString(key, null)` idiom.

- **`InMemorySecretStore` for migration tests:** The four ACs (AC-1 through AC-4) use `InMemorySecretStore` so the tests run without a device or Keystore. The `InMemorySecretStore` must implement the same `migrateFromPlaintext()` step ordering as the production adapter — it is exempt from the crypto and backup-exclusion clauses (CNTR §Validation Rules "Cryptographic guarantee") but must honor the rollback-safe ordering, idempotency, and null-on-absent clauses. Confirm that `InMemorySecretStore.migrateFromPlaintext()` (introduced in U-011) performs steps 1–4 using two `HashMap` instances (one for the legacy side, one for the encrypted side) and that the test can pre-seed the legacy map to simulate existing plaintext.

- **Robolectric for production-crypto smoke path:** In addition to the four `InMemorySecretStore` unit tests, one Robolectric test should exercise the `EncryptedPrefsSecretStore.migrateFromPlaintext()` round-trip on the Android SDK to confirm that the `MasterKey`/`EncryptedSharedPreferences` construction does not crash under the Robolectric shadow. This test need not assert every AC — a single `put`/`get` round-trip and a marker-present check suffices. If Robolectric does not support the `security-crypto` Keystore path at the available shadow version, this test should be `@Ignore`d with a comment referencing the shadow limitation rather than deleted (preserving the intent for future CI infrastructure upgrades).

- **Sequencing with U-011:** Because `migrateFromPlaintext()` is defined on the `SecretStore` interface (CNTR-MODERNIZATION-003), any refactoring of that interface in U-011 must be finalized before this story begins. If U-011 and U-012 are being built in the same sprint, the U-011 implementation of `SecretStore` and `InMemorySecretStore` must be merged and green on master before the U-012 branch is created from master.

- **Phase-1 manual wiring (no Hilt yet):** DES-MODERNIZATION-004 §Integration Design §Hilt explains that Hilt injection of `SecretStore` belongs to Phase-2 (DES-MODERNIZATION-008 / MU-007). In Phase 1, `AuthPreferences` receives its `SecretStore` through the existing dual-constructor Humble-Object seam at `:67-70`: a production constructor that creates `EncryptedPrefsSecretStore`, and a package-private test constructor that accepts a `SecretStore` parameter (allowing `InMemorySecretStore` injection in tests). The developer must not add Hilt annotations in this story.

## Supporting Documentation

- REQ-MODERNIZATION-004 §Acceptance Criteria AC-2, AC-3, AC-4, AC-6 — the requirement ACs this story directly satisfies
- REQ-MODERNIZATION-004 §Constraints — co-requisite sequencing with REQ-MODERNIZATION-002 (DES-002 / U-007 must land first) and idempotency/safety constraint
- DES-MODERNIZATION-004 §"One-Time, Idempotent, Rollback-Safe Plaintext to Encrypted Migration" — the canonical step-ordering specification, crash-safety case analysis table, and file-name reconciliation options (Option A preferred)
- DES-MODERNIZATION-004 §Design Validation — validation table and required test descriptions (tests 1–4 map directly to AC-1 through AC-4 of this story)
- DES-MODERNIZATION-004 §Integration Design §Sequencing With DES-MODERNIZATION-002 — edit-ordering rationale for MU-003 (U-007) → MU-004 (this story)
- DES-MODERNIZATION-004 §Integration Design §Hilt Injection of SecretStore — Phase-1 manual wiring vs. Phase-2 Hilt; why the port makes the swap mechanical
- CNTR-MODERNIZATION-003 §Contract Definition — `SecretStore` method surface, exact on-disk key strings, `migrateFromPlaintext()` entry point
- CNTR-MODERNIZATION-003 §Validation Rules §One-time migration semantics — the four mandatory contract clauses for `migrateFromPlaintext()` (idempotency, rollback-safe ordering, no plaintext residue, interruption safety)
- CNTR-MODERNIZATION-003 §"Backing-store invariant: file name and backup exclusion" — binding rule for AC-5
- CNTR-MODERNIZATION-003 §Example Payloads — reference implementation of `migrateFromPlaintext()` with the rollback-safe commit ordering

## Integration Contract References

- **CNTR-MODERNIZATION-003 §Contract Definition** — `SecretStore` interface method surface (`get`, `put`, `remove`, `contains`, `clear`, `migrateFromPlaintext`); this story calls `migrateFromPlaintext()` from `AuthPreferences.migrate()` and must honor the idempotency/ordering clauses
- **CNTR-MODERNIZATION-003 §Backing key set** — the exact on-disk key strings (`login_password`, `oauth2_token`, `oauth2_refresh_token`, `__secretstore_migration_complete__`) that the migration reads from the legacy store and writes to the encrypted store; deviating from these strings orphans existing installs
- **CNTR-MODERNIZATION-003 §Validation Rules §One-time migration semantics** — the four binding contract clauses that `migrateFromPlaintext()` must satisfy; these are not guidelines, they are contract requirements
- **CNTR-MODERNIZATION-003 §Backing-store invariant** — the backup-exclusion requirement for the chosen file-name option; governs AC-5
- **CNTR-MODERNIZATION-003 §Consumer obligations** — `AuthPreferences` MUST treat `null` from `SecretStore.get()` as "credential unavailable" (re-auth flow), not as a fatal error; governs AC-6 behavior under Keystore-loss

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Not finalized. Depends on U-011 (SecretStore port and EncryptedPrefsSecretStore adapter) being merged and green on master before this story's branch is created. Also depends on U-007 (migrate() rewrite) being merged before the migrate() insertion point is modified. The artifact_path for this story is `sdlc/artifacts/stories/U-012-credentials-plaintext-to-encrypted-migration.md`. Option A (same file name `"credentials"`) is the preferred file-name reconciliation strategy per DES-MODERNIZATION-004 and CNTR-MODERNIZATION-003; the implementer must confirm the chosen option in the PR description and reference AC-5 explicitly.
