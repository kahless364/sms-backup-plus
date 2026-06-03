---
artifact_type: review-code
story_id: "U-012"
verdict: "PASS"
agent: "Code Reviewer"
timestamp: "2026-06-03"
blockers: 0
warnings: 2
---

# Code Review: U-012

## Review Summary

| Aspect | Status |
|--------|--------|
| Contract compliance (CNTR-MODERNIZATION-003) | PASS |
| Test coverage (AC-1 through AC-4, AC-6, IC-2) | PASS |
| Code quality | PASS |
| Option A file-name confirmed | PASS |
| rollback-safe step ordering | PASS |
| Idempotency | PASS |
| Interruption safety | PASS |

## Verdict: PASS

## Findings

### Blockers

None.

### Warnings

1. **Placement of migrateFromPlaintext() relative to useXOAuth() guard** — Story's Technical
   Context said to place after the early-return guard. Developer correctly deviated from this
   guidance: placing it after would leave OAuth2 users' tokens unmigrated, causing a login
   regression for all OAuth2 users after upgrade. Placing it before the guard is the correct
   behavior and satisfies all ACs.

2. **PlaintextSharedPrefsSecretStore fallback removed** — U-011 had a `buildEncryptedStoreSafe`
   that fell back to `PlaintextSharedPrefsSecretStore` when Keystore was unavailable. U-012's
   lazy init change removed this because the constructor no longer throws. Under Robolectric,
   `get()` returns null (try-catch) and `migrateFromPlaintext()` skips gracefully (try-catch).
   Tests that need credentials use InMemorySecretStore via the two-arg constructor. This is
   architecturally correct but should be noted for future maintainers.

### Observations

1. **Option A confirmed** — `EncryptedPrefsSecretStore.CREDENTIALS_FILE_NAME = "credentials"`
   (EncryptedPrefsSecretStore.java:41). backup_descriptor.xml already contains
   `<exclude domain="sharedpref" path="credentials.xml"/>`. No descriptor edit required. AC-5 PASS.

2. **Lazy init double-checked locking** — The `getEncrypted()` method uses correct DCL pattern
   with `volatile SharedPreferences encrypted` field and synchronized block. Thread-safe.

3. **Step ordering in EncryptedPrefsSecretStore** — The raw SharedPreferences handle is opened
   and all three values buffered (lines 215-220) BEFORE `getEncrypted()` is called (line 227).
   This is the critical Option-A ordering invariant. Correctly implemented.

4. **InMemorySecretStore.legacyStore** — Exposed as package-private (not public), appropriate
   for test-only seeding within the same package. Tests in `AuthPreferencesTest` (same package)
   access it directly without casting or reflection.

5. **All CNTR-MODERNIZATION-003 key strings** verified: `login_password`, `oauth2_token`,
   `oauth2_refresh_token`, `__secretstore_migration_complete__` are used exactly as specified.

6. **Robolectric smoke test @Ignore'd** — Per story instructions, the production-adapter test
   is `@Ignore`d with an explanatory comment referencing AC-11 and U-011 Tech Notes. Intent
   preserved for future CI infrastructure upgrades.

## Phase Completion Report
---
story_id: "U-012"
phase: "code-review"
verdict: "PASS"
artifact_path: "sdlc/artifacts/build/sprints/sprint-001/U-012/review-code.md"
story_status: "done"
current_build_phase: "security-review"
blockers: 0
warnings: 2
errors: []
---
