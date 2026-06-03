---
artifact_type: plan
story_id: "U-011"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-03"
contracts_verified:
  - CNTR-MODERNIZATION-003
risks_identified: 2
---

# Implementation Plan: U-011

## Story Overview

Introduce the `SecretStore` port (interface), `EncryptedPrefsSecretStore` adapter (AES-256-GCM/Keystore-backed), and `InMemorySecretStore` test fake, and wire them into `AuthPreferences` via manual construction. The three at-rest secrets (`login_password`, `oauth2_token`, `oauth2_refresh_token`) are redirected through the port. Public `AuthPreferences` typed-accessor signatures are unchanged.

## Acceptance Criteria Mapping

| AC | Implementation Step | Notes |
|----|-------------------|-------|
| AC-1 | Create `SecretStore.java` interface with five methods; no android/androidx imports | New file |
| AC-2 | Create `EncryptedPrefsSecretStore.java` with `MasterKey.Builder(AES256_GCM)`, file `"credentials"`, `PrefKeyEncryptionScheme.AES256_SIV`, `PrefValueEncryptionScheme.AES256_GCM` | New file |
| AC-3 | `get()` catches all exceptions, returns null; confirmed by InMemorySecretStore test | Code review + InMemorySecretStoreTest |
| AC-4 | Only 3 secret keys cross SecretStore boundary; oauth2_user/login_user stay in plaintext prefs | AuthPreferences rewrite |
| AC-5 | Public typed-accessor signatures unchanged; tested via InMemorySecretStore injection | AuthPreferences rewrite + AuthPreferencesTest |
| AC-6 | `getCredentials()` removed; `SecretStore secretStore` field replaces `SharedPreferences credentials` | AuthPreferences rewrite |
| AC-7 | `InMemorySecretStore` in src/test, backed by HashMap, no android imports; round-trip test | New file + InMemorySecretStoreTest |
| AC-8 | Two-arg constructor `AuthPreferences(Context, SecretStore)` added; single-arg delegates via `buildEncryptedStoreSafe()` | AuthPreferences rewrite |
| AC-9 | File name `"credentials"` (Option A) — backup_descriptor.xml unchanged | EncryptedPrefsSecretStore constant |
| AC-10 | All editor flushes use `.commit()`, zero `.apply()` in EncryptedPrefsSecretStore | Code review |
| AC-11 | Full test suite green (389 tests); EncryptedPrefsSecretStore NOT exercised in JVM tests | Verified |
| AC-12 | `androidx.security:security-crypto:1.1.0-alpha06` in app/build.gradle | build.gradle edit |

## Implementation Steps

1. Merge `sdlc/modernization-plan` into worktree (fast-forward, no conflicts)
2. Add `androidx.security:security-crypto:1.1.0-alpha06` to `app/build.gradle`
3. Create `SecretStore.java` interface (5 methods, no android imports)
4. Create `EncryptedPrefsSecretStore.java` (MasterKey.Builder AES256_GCM, file "credentials", AES256_SIV keys, AES256_GCM values, commit() for all writes, null-on-decrypt-failure in get())
5. Create `InMemorySecretStore.java` in `src/test/` (HashMap-backed, no android imports)
6. Create `PlaintextSharedPrefsSecretStore.java` in `src/main/` as fallback for single-arg constructor when Keystore unavailable (Robolectric test path)
7. Rewrite `AuthPreferences.java`: add two-arg constructor, single-arg delegates via `buildEncryptedStoreSafe()`, redirect all 6 credential I/O sites to SecretStore, remove `getCredentials()` and `credentials` field
8. Write `InMemorySecretStoreTest.java` (8 tests, plain JVM)
9. Update `AuthPreferencesTest.java`: inject InMemorySecretStore via two-arg constructor, add 10 new U-011 tests

## Files Modified/Created

| File | Action | Reason |
|------|--------|--------|
| `app/build.gradle` | Modified | Add security-crypto dependency (AC-12) |
| `app/src/main/java/com/zegoggles/smssync/preferences/SecretStore.java` | Created | Port interface (AC-1) |
| `app/src/main/java/com/zegoggles/smssync/preferences/EncryptedPrefsSecretStore.java` | Created | Crypto adapter (AC-2, AC-10) |
| `app/src/main/java/com/zegoggles/smssync/preferences/PlaintextSharedPrefsSecretStore.java` | Created | Robolectric fallback for single-arg constructor |
| `app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java` | Modified | Wire SecretStore port (AC-4–AC-9) |
| `app/src/test/java/com/zegoggles/smssync/preferences/InMemorySecretStore.java` | Created | Test fake (AC-7) |
| `app/src/test/java/com/zegoggles/smssync/preferences/InMemorySecretStoreTest.java` | Created | Round-trip tests (AC-7) |
| `app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java` | Modified | Add 10 U-011 tests; switch all tests to InMemorySecretStore injection (AC-5, AC-8) |

## Contracts Verification

CNTR-MODERNIZATION-003 verified:
- Interface surface: 5 methods with exact signatures — implemented in SecretStore.java
- Backing key strings: "login_password", "oauth2_token", "oauth2_refresh_token" — used via Java constants (IMAP_PASSWORD, OAUTH2_TOKEN, OAUTH2_REFRESH_TOKEN) in AuthPreferences
- commit() durability: all put()/remove() calls use .commit() in EncryptedPrefsSecretStore
- null-on-absent/decrypt-failure: get() returns null in both InMemorySecretStore and EncryptedPrefsSecretStore
- Backup exclusion: file name "credentials" (Option A) — existing rule matches
- Migration entry point (migrateFromPlaintext): deferred to U-012 per story scope

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Option A read-during-create ordering: existing plaintext `credentials.xml` causes decrypt failure on first EncryptedPrefsSecretStore construction | Tracked to U-012 (migration story). Does not block U-011 unit tests. |
| Robolectric 4.12.x lacks AndroidKeyStore shadow: EncryptedPrefsSecretStore construction fails in JVM tests | Single-arg constructor falls back to PlaintextSharedPrefsSecretStore via buildEncryptedStoreSafe(). All credential tests use two-arg constructor with InMemorySecretStore. |

## Test Strategy

- `InMemorySecretStoreTest`: 8 plain-JVM tests covering the full SecretStore contract (put, get, contains, remove, clear, null-on-absent, overwrite, no-op remove)
- `AuthPreferencesTest`: 10 new tests using two-arg constructor + InMemorySecretStore injection, covering AC-5 (all public typed-accessor methods), AC-4 (no username keys in SecretStore), AC-8 (injection seam)
- All 25 existing `AuthPreferencesTest` migrate() tests preserved and green

## Phase Completion Report
---
story_id: "U-011"
phase: "planning"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-001/U-011/plan.md"
story_status: "in-progress"
current_build_phase: "implementation"
errors: []
---
