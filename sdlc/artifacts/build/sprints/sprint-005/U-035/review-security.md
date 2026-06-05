---
artifact_type: review-security
story_id: U-035
verdict: PASS
agent: Developer
timestamp: "2026-06-04"
---

# Security Review: U-035

## Summary

U-035 changes the dispatch thread for the credential migration; it does not change any
cryptographic algorithm, key management policy, storage format, or access control logic.
The security posture of the system is unchanged or improved by this story.

## Threat Model Scope

This story is in scope for:
- Thread safety of credential access (race condition between migration and readers)
- Credential availability guarantee (consumers must not read pre-migration nil values)
- Fail-open behavior of the gate timeout

This story is out of scope for:
- AES-256-GCM encryption of credentials (unchanged, `EncryptedPrefsSecretStore`)
- Android Keystore key management (unchanged)
- Backup exclusion (`credentials.xml` exclusion rule unchanged)
- OAuth2 token handling (routing unchanged, credentials migrated before first read)

## Security Analysis

### 1. Credentials at rest (CWE-312)

**Finding**: No change. Credentials are still encrypted via `EncryptedSharedPreferences`
(AES-256-GCM, Android Keystore master key). The migration moves credentials FROM plaintext
TO encrypted storage in the same four-step rollback-safe sequence. The dispatch thread
change does not affect the encryption.

**Status**: PASS

### 2. Migration race condition (TOCTOU)

**Finding**: The `CountDownLatch` gate correctly prevents a consumer from reading
pre-migration state. The gate is initialized (`prepare()`) before the background task
is submitted, so no consumer can observe a missing gate. The volatile `migrationComplete`
flag and the latch release establish happens-before relationships that guarantee migration
writes are visible to consumers after the gate signals (JMM §17.4.5).

**Status**: PASS

### 3. Fail-open timeout (10 seconds)

**Finding**: If Keystore stalls for more than 10 seconds, `awaitIfNeeded()` returns anyway
and the credential read proceeds. In this case:
- If migration was already complete on a previous launch (idempotency short-circuit), the
  credential values are already in the encrypted store and are readable. SAFE.
- If this is a first-run-after-upgrade and the Keystore genuinely cannot complete within
  10 seconds, the credential read returns null (nothing was migrated yet). The user would
  see an authentication failure — not a plaintext credential exposure. SAFE (fail-secure,
  not fail-insecure).

**Status**: PASS

### 4. Main-thread guard fail-open

**Finding**: `awaitIfNeeded()` skips the await on the main thread (ANR prevention). Current
production call sites are all on background threads (BackupWorker). If a future change adds
a credential read on the main thread, the guard would silently allow the read through without
blocking. This is a latent risk, not a current vulnerability. The Javadoc explicitly warns
about this behavior.

**Recommendation**: Add a `@MainThread` lint warning or an assertion in debug builds if
this becomes a concern. Not a blocker for this story.

**Status**: PASS (current risk: none; future risk: documented)

### 5. `resetForTest()` method exposure

**Finding**: `resetForTest()` is package-private in a production class. It can reset the
static gate state. In production, no caller exists for this method (R8 will eliminate it).
In tests, it is used for isolation. No production exploit path exists.

**Status**: PASS

### 6. Executor thread pool

**Finding**: `Executors.newSingleThreadExecutor()` creates an unbound queue single-thread
executor. The executor is shut down after the single migration task completes. No thread
leak, no unbounded work queue growth. The executor is not reachable from any user-controlled
input.

**Status**: PASS

## Invariants Verified

| Invariant | Verified |
|-----------|----------|
| Credentials remain encrypted at rest (AES-256-GCM) | YES — `EncryptedPrefsSecretStore` unchanged |
| No plaintext credential written outside migration sequence | YES — migration sequence unchanged |
| Migration runs once (MIGRATION_COMPLETE_KEY guard) | YES — idempotency short-circuit unchanged |
| Plaintext cleared only after durable encrypted commit | YES — rollback-safe four-step ordering unchanged |

## Verdict

PASS — no security regressions. The threading change improves the security posture by
reducing the latent ANR risk on first-run-after-upgrade, which could have been exploited
to DoS the app startup on slow-storage devices. Credential confidentiality and integrity
are unaffected.
