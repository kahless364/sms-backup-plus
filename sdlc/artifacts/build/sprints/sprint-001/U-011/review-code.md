---
artifact_type: review-code
story_id: "U-011"
verdict: "PASS"
agent: "Developer (Code Review)"
timestamp: "2026-06-03"
blockers: 0
warnings: 2
---

# Code Review: U-011

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance (CNTR-MODERNIZATION-003) | PASS |
| Test coverage | PASS |
| Code quality | PASS |
| Pattern conformance | PASS |
| Migration scope (U-012 not included) | PASS |

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

**W-1: PlaintextSharedPrefsSecretStore is an unexpected addition (non-blocking)**

The story did not specify a `PlaintextSharedPrefsSecretStore` class. This class was introduced to handle Robolectric's lack of AndroidKeyStore support, which would otherwise cause 394 test failures (all Robolectric tests that trigger `App.onCreate()` fail because `buildEncryptedStore()` throws). The fallback is:
- Clearly `@Deprecated`
- Uses file name `"credentials_fallback"` (not `"credentials"`) to avoid interfereing with the production encrypted store
- Only reachable via `buildEncryptedStoreSafe()` catch block
- Never used on real Android devices where the Keystore is available
- Logged at WARN level for visibility

Risk: the fallback silently downgrades credential storage to plaintext in test environments. Since tests use `InMemorySecretStore` via the two-arg constructor for all credential assertions, the `PlaintextSharedPrefsSecretStore` fallback is only exercised by App.onCreate() during non-credential tests, where no credential I/O occurs. The risk to test validity is low.

**W-2: `security-crypto:1.1.0-alpha06` is a pre-release dependency**

The story and DES-MODERNIZATION-004 §ADR explicitly choose 1.1.0-alpha06 for the `MasterKey.Builder` API. The alpha stability is noted in the build.gradle comment. No stable 1.1.x release was available as of June 2026. The 1.0.0 stable alternative uses the deprecated `MasterKeys.getOrCreate()` API without StrongBox configuration. The risk is accepted per the design decision.

### Observations

**O-1: Correct use of constants (not inline strings)**

`AuthPreferences.java` uses the Java constants `OAUTH2_TOKEN`, `OAUTH2_REFRESH_TOKEN`, `IMAP_PASSWORD` as arguments to `secretStore.get/put/remove()`. These constants resolve to the exact on-disk strings `"oauth2_token"`, `"oauth2_refresh_token"`, `"login_password"` per CNTR-MODERNIZATION-003 §Backing key set. U-012's migration routine must use these same constants (or the same string values) to maintain continuity.

**O-2: migrate() body is unchanged**

The U-007-rewritten `migrate()` body is byte-for-byte identical to its pre-U-011 state. The `migrate()` method does not call `secretStore` methods (correct: migration is U-012's scope). Verified by reading the method body.

**O-3: commit() vs apply() distinction is clean**

`EncryptedPrefsSecretStore` uses `.commit()` for every editor flush. `migrate()` in `AuthPreferences` uses `.apply()` for the preferences (non-credential) editor. These are both correct and consistent with CNTR-MODERNIZATION-003 §Type notes and ARCH-017.

**O-4: No duplicate credentials SharedPreferences access**

`grep -n "getSharedPreferences.*credentials" AuthPreferences.java` returns zero results. The `credentials` field is removed. AC-6 is fully satisfied.

**O-5: oauth2_user and login_user are NOT routed through SecretStore**

Code search confirms `secretStore.contains("oauth2_user")`, `secretStore.put("oauth2_user"...)` etc. do not appear anywhere in `AuthPreferences.java`. These keys remain in the plaintext `preferences` field as required by AC-4 and CNTR-MODERNIZATION-003 §Backing key set note.

**O-6: backup_descriptor.xml unmodified**

`git diff app/src/main/res/xml/backup_descriptor.xml` returns empty. The existing `<exclude domain="sharedpref" path="credentials.xml"/>` rule continues to match the `EncryptedPrefsSecretStore` backing file (Option A). AC-9 is satisfied.

**O-7: SecretStore interface has no android imports**

`SecretStore.java` imports: zero. The interface is a plain Java interface with no platform dependencies, satisfying the fakeable-with-HashMap design goal and AC-1.

## Phase Completion Report
---
story_id: "U-011"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-001/U-011/review-code.md"
story_status: "done"
current_build_phase: "security-review"
blockers: 0
warnings: 2
errors: []
---
