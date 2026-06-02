---
type: story
status: planned
sprint: "000001"
artifact_type: user-story
priority: high
complexity: medium
parallel_eligible: false
iteration: 2
requirements:
  - REQ-MODERNIZATION-004
design_docs:
  - DES-MODERNIZATION-004
integration_contracts:
  - CNTR-MODERNIZATION-003
dependencies:
  - U-006
  - U-007
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-011
title: Introduce SecretStore port, EncryptedPrefsSecretStore adapter, and InMemorySecretStore fake; wire into AuthPreferences via manual construction
pipeline: ''
domain: modernization
requirement_source: authored
---

# U-011: Introduce SecretStore Port, EncryptedPrefsSecretStore Adapter, and InMemorySecretStore Fake; Wire into AuthPreferences via Manual Construction

## Story

As a developer maintaining the SMS Backup+ codebase,
I want a `SecretStore` interface (get/put/remove/contains/clear), a Keystore-backed `EncryptedPrefsSecretStore` adapter (AES-256-GCM values, AES-256-SIV key names, `"credentials"` file, Android Keystore master key), and an `InMemorySecretStore` test fake wired into `AuthPreferences` via the existing manual-construction path,
so that the three on-disk secrets (`login_password`, `oauth2_token`, `oauth2_refresh_token`) are encrypted at rest behind the unchanged public `AuthPreferences` typed-accessor API, and every downstream caller — OAuth2/IMAP auth, `hasOAuth2Tokens()`, `isLoginInformationSet()` — continues to function without modification.

## Acceptance Criteria

- [ ] **AC-1 — SecretStore interface exists with the five-method surface defined in CNTR-MODERNIZATION-003**

  Given the file `app/src/main/java/com/zegoggles/smssync/preferences/SecretStore.java` is created in the `com.zegoggles.smssync.preferences` package,
  when a developer reads the interface,
  then it declares exactly five methods — `String get(String key)`, `void put(String key, String value)`, `void remove(String key)`, `boolean contains(String key)`, `void clear()` — with no `androidx.security.crypto.*` or `android.*` imports in the interface file itself; the interface is importable and compilable via `./gradlew :app:compileDebugJavaSources` with zero errors.

- [ ] **AC-2 — EncryptedPrefsSecretStore implements SecretStore with AES-256-GCM / Keystore construction**

  Given the file `app/src/main/java/com/zegoggles/smssync/preferences/EncryptedPrefsSecretStore.java` exists,
  when a developer reads its constructor,
  then it constructs an `EncryptedSharedPreferences` instance using:
  (a) `MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).setRequestStrongBoxBacked(true).build()`;
  (b) file name `"credentials"` (the exact string, identical to the existing plaintext store name — Option A from DES-MODERNIZATION-004 §Migration);
  (c) `PrefKeyEncryptionScheme.AES256_SIV`;
  (d) `PrefValueEncryptionScheme.AES256_GCM`;
  and the adapter implements all five `SecretStore` methods by delegating to that `EncryptedSharedPreferences` instance, with `put` and `remove` using `.commit()` (not `.apply()`) to satisfy the durability-barrier contract clause in CNTR-MODERNIZATION-003.

- [ ] **AC-3 — get() returns null on absence and on decrypt failure; never throws to caller**

  Given an `EncryptedPrefsSecretStore` instance,
  when `get("any_absent_key")` is called on a store that was never written,
  then the method returns `null` without throwing.

  Given an `EncryptedPrefsSecretStore` instance whose backing `EncryptedSharedPreferences` throws an `AEADBadTagException`, `InvalidKeyException`, or any `GeneralSecurityException`/`IOException` during a read operation (simulated in a unit test via a fake/spy),
  when `get("login_password")` is called,
  then the method returns `null` without rethrowing the exception;
  a unit test using `InMemorySecretStore` confirms the null-on-absent contract, and code review confirms the adapter catches and suppresses the decrypt-failure exceptions for the read path (write failures remain unchecked per the contract).

- [ ] **AC-4 — Only the three secret keys cross the SecretStore boundary; username keys do not**

  Given the rewritten `AuthPreferences.java`,
  when a developer reads every site that calls `secretStore.put(...)`, `secretStore.get(...)`, `secretStore.remove(...)`, or `secretStore.contains(...)`,
  then the only key strings passed are `"login_password"`, `"oauth2_token"`, and `"oauth2_refresh_token"` (and the migration marker `"__secretstore_migration_complete__"` written only by the migration routine in U-012);
  `oauth2_user` (read/written via `preferences` at `AuthPreferences.java:87, 124`) and `login_user` (IMAP_USER, `:36`) are NOT routed through the `SecretStore`; a code search for `secretStore` in `AuthPreferences.java` confirms zero occurrences of `"oauth2_user"` or `"login_user"` as arguments.

- [ ] **AC-5 — AuthPreferences public typed-accessor signatures are unchanged**

  Given the rewritten `AuthPreferences.java`,
  when the file is compiled as part of `./gradlew :app:compileDebugJavaSources`,
  then the following method signatures remain byte-for-byte identical to the pre-story source:
  `public String getOauth2Token()` (`:72`),
  `public String getOauth2RefreshToken()` (`:76`),
  `public void setOauth2Token(String username, String accessToken, String refreshToken)` (`:85`),
  `public void clearOauth2Data()` (`:98`),
  `public void setImapPassword(String username, String password)` (`:119`),
  `private String getImapPassword()` (`:236`);
  a unit test that calls every one of these methods via an `AuthPreferences` instance backed by `InMemorySecretStore` compiles and passes, confirming the call sites are unaffected.

- [ ] **AC-6 — The private getCredentials() seam body is redirected to the SecretStore; direct SharedPreferences("credentials") access is removed from AuthPreferences**

  Given the rewritten `AuthPreferences.java`,
  when a developer reads the class,
  then:
  (a) there is no `context.getSharedPreferences("credentials", ...)` call anywhere in `AuthPreferences.java` — the `credentials` field (previously a raw `SharedPreferences`) is replaced by a `SecretStore` field;
  (b) the private `getCredentials()` method (`:221-226`) either is removed entirely or is redirected to return the `SecretStore` reference rather than opening a raw `SharedPreferences`;
  (c) a code search `grep -n "getSharedPreferences.*credentials" AuthPreferences.java` from the repo root returns zero results.

- [ ] **AC-7 — InMemorySecretStore implements SecretStore with a HashMap; exempt from crypto and backup-exclusion clauses**

  Given the file `app/src/test/java/com/zegoggles/smssync/preferences/InMemorySecretStore.java` exists (or, if placed under `src/main/` for use by production instrumentation tests, a code comment marks it as test-only),
  when a developer reads the implementation,
  then it implements all five `SecretStore` methods using a `HashMap<String, String>` with no `android.*` or `androidx.*` dependencies, making it runnable on the plain JVM;
  a standalone unit test instantiates `InMemorySecretStore`, calls all five methods in a round-trip (`put` → `get` → `contains` → `remove` → `contains` → `clear`), and asserts the expected result at each step; the test passes via `./gradlew :app:testDebugUnitTest`.

- [ ] **AC-8 — AuthPreferences constructor(s) accept a SecretStore and the manual-construction path provides EncryptedPrefsSecretStore**

  Given the rewritten `AuthPreferences.java`,
  when a developer reads its constructor(s),
  then:
  (a) a constructor `AuthPreferences(Context context, SecretStore secretStore)` exists that accepts the store externally (enabling test injection of `InMemorySecretStore`);
  (b) the single-argument convenience constructor `AuthPreferences(Context context)` — which exists today at `:67-70` and is called by `Preferences.java:294-296` — continues to exist, and its body constructs a new `EncryptedPrefsSecretStore(context)` and delegates to the two-argument constructor (the "manual-construction path" per DES-MODERNIZATION-004 §Hilt Injection);
  (c) the call site `new AuthPreferences(context)` in `Preferences.java:294-296` compiles without change, confirmed by `./gradlew :app:compileDebugJavaSources` passing with zero errors;
  (d) a unit test constructs `new AuthPreferences(context, new InMemorySecretStore())` without error, confirming the injection seam exists.

- [ ] **AC-9 — Backing file name is "credentials"; the existing backup-exclusion rule continues to match without any descriptor edit**

  Given the `EncryptedPrefsSecretStore` is constructed with file name `"credentials"` (Option A),
  when a developer reads `app/src/main/res/xml/backup_descriptor.xml`,
  then the file is byte-for-byte identical to its pre-story content — no lines are added, removed, or changed;
  a code review confirms the `<exclude domain="sharedpref" path="credentials.xml"/>` rule matches the encrypted store's backing file, satisfying REQ-MODERNIZATION-004 AC-4 and CNTR-MODERNIZATION-003 §Backing-store invariant without any descriptor change.

- [ ] **AC-10 — put() and remove() use commit() (synchronous/durable) not apply()**

  Given the `EncryptedPrefsSecretStore` implementation,
  when a developer reads every `SharedPreferences.Editor` flush call inside the class,
  then every flush uses `.commit()`, not `.apply()`; a code search `grep -n "\.apply()" EncryptedPrefsSecretStore.java` from the repo root returns zero results; this satisfies the CNTR-MODERNIZATION-003 durability-barrier clause which requires `put` to be "synchronous and durable on return" so the migration in U-012 can rely on the commit as a durability barrier.

- [ ] **AC-11 — All new production classes compile and existing test suite remains green**

  Given the three new files (`SecretStore.java`, `EncryptedPrefsSecretStore.java`, `InMemorySecretStore.java`) and the rewritten `AuthPreferences.java`,
  when `./gradlew :app:testDebugUnitTest` is executed from the repo root,
  then the command exits with code 0, all pre-existing tests pass (including the U-007 `AuthPreferencesTest` tests for `migrate()`), and no new test failures are introduced; the `EncryptedPrefsSecretStore` constructor is NOT exercised by unit tests that run on the plain JVM (it requires a real Android Keystore) — only `InMemorySecretStore`-backed tests run in the JVM test suite.

- [ ] **AC-12 — androidx.security:security-crypto dependency is declared in app/build.gradle**

  Given the `EncryptedPrefsSecretStore` adapter imports `androidx.security.crypto.EncryptedSharedPreferences` and `androidx.security.crypto.MasterKey`,
  when a developer reads `app/build.gradle`,
  then a dependency declaration for `androidx.security:security-crypto` is present in the `dependencies {}` block with a pinned version (e.g., `"androidx.security:security-crypto:1.1.0-alpha06"` or the latest stable 1.0.x / alpha 1.1.x available at implementation time — the exact version must be recorded in a comment citing DES-MODERNIZATION-004 §ADR trade-off note); `./gradlew :app:compileDebugJavaSources` resolves the dependency without error.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/src/main/java/com/zegoggles/smssync/preferences/SecretStore.java` | Does not exist | Create: `SecretStore` interface with `get`, `put`, `remove`, `contains`, `clear`; no android imports |
| `app/src/main/java/com/zegoggles/smssync/preferences/EncryptedPrefsSecretStore.java` | Does not exist | Create: `SecretStore` adapter backed by `EncryptedSharedPreferences`; `MasterKey` AES-256-GCM; file name `"credentials"`; `put`/`remove` use `commit()`; `get` catches and suppresses decrypt exceptions as `null` |
| `app/src/test/java/com/zegoggles/smssync/preferences/InMemorySecretStore.java` | Does not exist | Create: `SecretStore` fake backed by `HashMap<String, String>`; no android/androidx imports; for unit test injection only |
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | `getCredentials()` (`:221-226`) opens `context.getSharedPreferences("credentials", MODE_PRIVATE)` and all six typed accessors read/write through that raw `SharedPreferences` | Add `SecretStore secretStore` field; add two-arg constructor accepting `SecretStore`; keep single-arg constructor constructing `EncryptedPrefsSecretStore`; redirect all three secret read/write sites (`login_password`, `oauth2_token`, `oauth2_refresh_token`) to the `SecretStore`; remove direct `credentials` `SharedPreferences` access |
| `app/build.gradle` | No `androidx.security:security-crypto` dependency | Add pinned `androidx.security:security-crypto` dependency |
| `app/src/main/res/xml/backup_descriptor.xml` | Contains `<exclude domain="sharedpref" path="credentials.xml"/>` | No change (Option A: same file name, existing rule continues to match) |

## Existing Behavior to Preserve

- `Preferences.java:294-296` calls `new AuthPreferences(context).migrate()` on every `App.onCreate()`. The single-argument constructor must survive this story intact; the call must compile and execute without change.
- `AuthPreferences.migrate()` — the rewritten body from U-007 must remain untouched by this story. This story does NOT add a `migrateFromPlaintext()` call to `migrate()`; that is U-012's scope.
- `getOauth2Token()`, `getOauth2RefreshToken()`, `setOauth2Token()`, `clearOauth2Data()`, `setImapPassword()`, and `getImapPassword()` must keep their exact signatures. No visibility changes, no parameter additions, no return type changes.
- `hasOAuth2Tokens()` (`:80`) and `isLoginInformationSet()` (`:153`) rely on `getOauth2Token()` and `getOauth2RefreshToken()` returning `null` when absent. Because `SecretStore.get()` returns `null` for absent keys (same as `getString(key, null)`), these methods continue to function correctly without modification.
- `clearOauth2Data()` (`:98`) calls through to remove `oauth2_token` and `oauth2_refresh_token`. After this story, those removals go through `secretStore.remove(...)`. The method body changes internally but the external behavior (`hasOAuth2Tokens()` returning `false` afterward) must be preserved.
- `oauth2_user` and `login_user` preference keys are read/written via `preferences` (the default `SharedPreferences`), not `getCredentials()`. They must remain on `preferences` unchanged.
- The existing `AuthPreferencesTest.java` test cases (`testStoreUri`, `testStoreUriWithXOAuth2`, and the U-007 `migrate()` tests) must all continue to pass. This story adds new tests to `AuthPreferencesTest.java` (AC-5, AC-7, AC-8) using `InMemorySecretStore` injection; it must not break existing test setup.

## Verification Steps

1. **AC-1 (interface compiles):** Run `./gradlew :app:compileDebugJavaSources`. Confirm zero errors. Open `SecretStore.java` and confirm the five method signatures match CNTR-MODERNIZATION-003 §Interface exactly. Confirm no `android.*` or `androidx.*` import is present in the interface file.

2. **AC-2 (adapter construction):** Open `EncryptedPrefsSecretStore.java`. Confirm `MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).setRequestStrongBoxBacked(true).build()` is present. Confirm `"credentials"` (not `"credentials_enc"` or any other variant) is passed as the file name. Confirm `PrefKeyEncryptionScheme.AES256_SIV` and `PrefValueEncryptionScheme.AES256_GCM` appear in the `EncryptedSharedPreferences.create(...)` call.

3. **AC-3 (null-on-absent and null-on-decrypt-failure):** Run `./gradlew :app:testDebugUnitTest --tests "*InMemorySecretStoreTest*"`. Confirm the test asserting `get("absent_key") == null` passes. Open `EncryptedPrefsSecretStore.java` and confirm `get()` wraps the `EncryptedSharedPreferences` read in a try/catch covering at minimum `GeneralSecurityException` and `IOException`, returning `null` in the catch block.

4. **AC-4 (only secret keys cross the port):** Run `grep -n "secretStore" app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` from the repo root. Inspect every call site. Confirm the only key-string arguments are `"login_password"`, `"oauth2_token"`, and `"oauth2_refresh_token"`. Confirm `"oauth2_user"` and `"login_user"` do not appear as arguments to any `secretStore.*` call.

5. **AC-5 (public signatures unchanged):** Run `./gradlew :app:compileDebugJavaSources`. Confirm zero errors. Run `./gradlew :app:testDebugUnitTest --tests "*AuthPreferencesTest*"`. Confirm all tests pass. Open `AuthPreferences.java` and confirm the six method signatures listed in AC-5 are identical to the pre-story file (compare with the read performed during story authoring: `:72`, `:76`, `:85`, `:98`, `:119`, `:236`).

6. **AC-6 (no direct credentials SharedPreferences access):** Run `grep -n "getSharedPreferences.*credentials" app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` from the repo root. Confirm zero results. Confirm the `credentials` field (previously `SharedPreferences`) either no longer exists or is renamed to `secretStore` of type `SecretStore`.

7. **AC-7 (InMemorySecretStore round-trip):** Run `./gradlew :app:testDebugUnitTest --tests "*InMemorySecretStoreTest*"`. Confirm the round-trip test (`put` → `get` → `contains` → `remove` → `contains` → `clear`) passes. Confirm `InMemorySecretStore.java` has no `import android.*` or `import androidx.*` statements.

8. **AC-8 (two-arg constructor and manual-construction path):** Open `AuthPreferences.java`. Confirm the two-arg constructor `AuthPreferences(Context context, SecretStore secretStore)` exists. Confirm the single-arg `AuthPreferences(Context context)` delegates to it by constructing `new EncryptedPrefsSecretStore(context)`. Run `./gradlew :app:compileDebugJavaSources` and confirm `Preferences.java` compiles without change. Run `./gradlew :app:testDebugUnitTest --tests "*AuthPreferencesTest*"` and confirm the injection test (new, added by this story) passes.

9. **AC-9 (backup_descriptor.xml unchanged):** Run `git diff app/src/main/res/xml/backup_descriptor.xml`. Confirm the diff is empty — the file is unmodified. This confirms Option A is taken and the existing `<exclude>` rule continues to match `credentials.xml`.

10. **AC-10 (commit not apply in adapter):** Run `grep -n "\.apply()" app/src/main/java/com/zegoggles/smssync/preferences/EncryptedPrefsSecretStore.java` from the repo root. Confirm zero results. Confirm `.commit()` appears at every `SharedPreferences.Editor` flush site.

11. **AC-11 (full test suite green):** Run `./gradlew :app:testDebugUnitTest` from the repo root. Confirm exit code 0. Confirm zero new test failures. Review the test report and confirm all U-007 `AuthPreferencesTest` migrate() tests remain at their prior pass/fail state (the authored-red AC-1 from U-006, if still `@Ignore`d, should remain `@Ignore`d).

12. **AC-12 (dependency declared):** Run `grep -n "security-crypto" app/build.gradle` from the repo root. Confirm a result with a pinned version string. Run `./gradlew :app:dependencies --configuration debugRuntimeClasspath` and confirm `androidx.security:security-crypto` appears in the resolved dependency tree.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java) | Create `SecretStore.java`, `EncryptedPrefsSecretStore.java`, `InMemorySecretStore.java`; rewrite `AuthPreferences.java` secret I/O seam; update `app/build.gradle` with dependency | Developer |

## Technical Notes

**File layout and package placement.** All three new files belong in `com.zegoggles.smssync.preferences`:
- `SecretStore.java` — `app/src/main/java/com/zegoggles/smssync/preferences/`
- `EncryptedPrefsSecretStore.java` — `app/src/main/java/com/zegoggles/smssync/preferences/`
- `InMemorySecretStore.java` — `app/src/test/java/com/zegoggles/smssync/preferences/`

Placing `InMemorySecretStore` under the test source set keeps it off the production APK. `AuthPreferencesTest.java` (same test package) can import it directly with no visibility issues.

**Exact on-disk key strings (binding — do not use Java identifier names).** CNTR-MODERNIZATION-003 §Backing key set specifies the three keys as `"login_password"`, `"oauth2_token"`, and `"oauth2_refresh_token"`. These are the *stored preference-key strings* from `AuthPreferences.java:37, 33, 34`, not the Java symbolic constants (`IMAP_PASSWORD`, `OAUTH2_TOKEN`, `OAUTH2_REFRESH_TOKEN`). The adapter and the `AuthPreferences` call sites must use the Java constants (e.g., `AuthPreferences.IMAP_PASSWORD`) so the on-disk strings remain consistent — do not use the string literals inline; call the constants. This matters for the migration routine in U-012 that reads the legacy store with the same key strings.

**AuthPreferences.java sites to redirect.** Based on the full source read during design authoring, the exact sites that read or write through `getCredentials()` are:
- `getOauth2Token()` (`:72`): `getCredentials().getString(OAUTH2_TOKEN, null)` → `secretStore.get(OAUTH2_TOKEN)`
- `getOauth2RefreshToken()` (`:76`): `getCredentials().getString(OAUTH2_REFRESH_TOKEN, null)` → `secretStore.get(OAUTH2_REFRESH_TOKEN)`
- `setOauth2Token()` (`:85-95`): `getCredentials().edit().putString(OAUTH2_TOKEN, ...).putString(OAUTH2_REFRESH_TOKEN, ...).commit()` → `secretStore.put(OAUTH2_TOKEN, ...); secretStore.put(OAUTH2_REFRESH_TOKEN, ...)`
- `clearOauth2Data()` (`:98-108`): `getCredentials().edit().remove(OAUTH2_TOKEN).remove(OAUTH2_REFRESH_TOKEN).commit()` → `secretStore.remove(OAUTH2_TOKEN); secretStore.remove(OAUTH2_REFRESH_TOKEN)`
- `setImapPassword()` (`:119-120`): `getCredentials().edit().putString(IMAP_PASSWORD, ...).commit()` → `secretStore.put(IMAP_PASSWORD, ...)`
- `getImapPassword()` (`:236`): `getCredentials().getString(IMAP_PASSWORD, null)` → `secretStore.get(IMAP_PASSWORD)`

Each site that previously called `.commit()` on the `getCredentials()` editor must now call `secretStore.put(...)` or `secretStore.remove(...)`, which are themselves `commit()`-durable per the CNTR-MODERNIZATION-003 contract. The net write-behavior is identical — synchronous and durable — so no behavioral regression occurs.

**Option A read-during-create ordering (key implementation risk per DES-MODERNIZATION-004 §Migration).** This story does NOT implement migration (that is U-012). However, the `EncryptedPrefsSecretStore` constructor creates `EncryptedSharedPreferences` over the file name `"credentials"`. If an existing install's `credentials.xml` still holds plaintext entries when `EncryptedPrefsSecretStore` is first constructed, the library will attempt to decrypt those plaintext bytes as ciphertext and will fail. This is the stated key risk of Option A. It is resolved by U-012, which must be implemented before U-011 ships in production — U-012 buffers the plaintext values in memory *before* constructing `EncryptedPrefsSecretStore`, then writes ciphertext, then clears plaintext. For this story's scope: the adapter is written, wired, and tested; migration is absent; the ordering risk is tracked in U-012 and does not block this story's unit tests (which use `InMemorySecretStore`).

**`put()` durability: commit() not apply().** `EncryptedPrefsSecretStore.put()` MUST call `.commit()` on the editor, not `.apply()`. CNTR-MODERNIZATION-003 §Type notes is explicit: "put MUST be synchronous and durable on return (`commit()` semantics) so the migration durability barrier (§Migration step 3) is meaningful." `apply()` is non-conformant and violates the contract. This is the inverse of the AC-7 rule in U-007 (which forbids `commit()` in `migrate()` for main-thread perf reasons) — the two are not contradictory: `migrate()` is main-thread and has idempotent retry; `SecretStore.put()` is called from typed accessors that are not latency-sensitive on launch.

**`security-crypto` version pinning.** DES-MODERNIZATION-004 §ADR notes that the `MasterKey` API (used in this story) is the 1.1.x alpha API (`androidx.security:security-crypto:1.1.0-alpha06` or later). The 1.0.0 API uses `MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)` instead. The developer must decide which version to pin and add a comment to `app/build.gradle` citing the decision. The port (`SecretStore`) shields all callers from this choice. If 1.0.0 stable is preferred over 1.1.x alpha, use the `MasterKeys` idiom instead — the AC remains satisfied either way as long as the crypto parameters (AES-256-GCM master key, Keystore-backed, StrongBox best-effort) are equivalent.

**StrongBox best-effort.** `setRequestStrongBoxBacked(true)` is a best-effort hint — the Keystore will silently fall back to TEE or software if StrongBox is unavailable. This is correct behavior; the adapter must not fail on devices without StrongBox.

**No migration logic in this story.** This story establishes the port and adapter only. The `migrateFromPlaintext()` method defined in CNTR-MODERNIZATION-003 §Interface is U-012's responsibility. Do not add it here. The migration marker key `"__secretstore_migration_complete__"` is not written by this story.

**Phase-1 manual construction vs. Phase-2 Hilt.** DES-MODERNIZATION-004 §Hilt Injection explains that the single-arg `AuthPreferences(Context)` constructor constructs `EncryptedPrefsSecretStore` directly (Phase 1, this story). When DES-MODERNIZATION-008 (Hilt) lands, the `@Binds @Singleton SecretStore <- EncryptedPrefsSecretStore` binding replaces the manual construction — a mechanical swap that requires no change to `SecretStore`, `EncryptedPrefsSecretStore`, or any caller. The two-arg constructor (AC-8) is the seam that Hilt will ultimately target; its existence makes Phase 2 a one-line swap.

**Test strategy.** Unit tests (JVM, Robolectric) must use `InMemorySecretStore` for all `AuthPreferences` tests in this story. `EncryptedPrefsSecretStore` requires a real Android Keystore and cannot be unit-tested on the plain JVM without Robolectric's keystore support (which is limited). Robolectric integration tests for the encrypted adapter are acceptable as a follow-on but are not required for story completion. The contract for `EncryptedPrefsSecretStore` is verified by (a) code review of the constructor parameters and `commit()` calls, and (b) the AC-2 / AC-10 verification steps above.

**Dependency on U-007.** U-007 must have landed (master, CI green) before this story is sprint-planned, because U-007 rewrites `AuthPreferences.migrate()` and this story further rewrites the rest of `AuthPreferences`. The `depends_on: U-007` frontmatter encodes this as a hard sequencing constraint — the edit-ordering requirement from DES-MODERNIZATION-004 §Sequencing and migration-units.md MU-003 → MU-004. A developer must not begin this story's `AuthPreferences.java` edits until U-007's merge is confirmed.

**Dependency on U-006 (Gate G2).** `depends_on: U-006` encodes the Gate G2 prerequisite. U-006's characterization tests must be green on master CI before any `AuthPreferences.java` modifications from this story are merged, consistent with the pattern established for U-007 AC-8.

## Supporting Documentation

- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-004-encrypt-credentials-at-rest.md` — governing requirement; AC-1 through AC-6 that this story and U-012 together satisfy
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-004-encrypted-credential-store-design.md` — §System Architecture (port diagram), §Cryptographic Construction (exact MasterKey/EncryptedSharedPreferences recipe), §Integration Design §Phase-1-manual vs Phase-2-Hilt, §Risks (read-during-create ordering, Option A), §Design Validation
- `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-003-secret-store.md` — binding interface specification, backing key strings, durability clause, null-on-decrypt-failure clause, backup-exclusion invariant, versioning and breaking-change list
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-002-transport-security-hardening-design.md` — §Integration Design §Sequencing: explains why U-007 (TLS migrate() rewrite) must land before this story modifies `AuthPreferences.java`

## Integration Contract References

This story implements the **CNTR-MODERNIZATION-003** (`SecretStore` port) contract. The story introduces both sides of the boundary:
- **Producer side:** `EncryptedPrefsSecretStore` (the adapter) satisfies the cryptographic guarantee, `put()` durability, and `get()` null-on-failure clauses.
- **Consumer side:** `AuthPreferences` satisfies the public typed-accessor stability clause (AC-5) and the null-as-credential-unavailable consumer obligation.
- **Test fake:** `InMemorySecretStore` satisfies the method-surface and null-on-absent clauses while being exempt from the crypto and backup-exclusion clauses per the contract.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Not finalized. This story introduces the SecretStore port and EncryptedPrefsSecretStore adapter (AC-1 through AC-12) and wires them into AuthPreferences via manual construction; it explicitly excludes migration logic (U-012) and Hilt injection (U-013/DES-MODERNIZATION-008); the load-bearing implementation risk is the Option-A read-during-create ordering, which is carried to U-012 and does not block this story's unit tests.
