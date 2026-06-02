---
status: approved
artifact_type: interface-contract
consumers: []
related_requirements: []
related_design_docs: []
related_stories: []
change_records: []
id: CNTR-MODERNIZATION-003
title: ''
domain: modernization
contract_type: ''
producer: ''
---

# CNTR-MODERNIZATION-003: SecretStore Credential Port

## Overview

This contract specifies the **`SecretStore` port** — the in-process interface boundary
through which SMS Backup+'s three at-rest secrets (the Gmail IMAP app-password and the
OAuth2 access/refresh tokens) are read, written, removed, and migrated. It is the binding
realization of the contract candidate flagged in **DES-MODERNIZATION-004 §Integration
Contracts** (realizing **REQ-MODERNIZATION-004 / MU-004**, closing **CWE-312**).

The port exists so that the credential persistence mechanism can be replaced — from a
plaintext `SharedPreferences("credentials", MODE_PRIVATE)` store to a Keystore-backed
AES-256-GCM `EncryptedSharedPreferences` store — **without altering any caller**. The
consumer side (`AuthPreferences`) keeps its public typed-accessor signatures unchanged
(REQ-MODERNIZATION-004 AC-5); only the body of its private `getCredentials()` seam
(`AuthPreferences.java:221-226`, verified this session) is redirected to delegate to a
`SecretStore`.

This contract governs three things that both sides MUST honor:
1. the **method surface and types** of the port;
2. the **exact on-disk key strings** the port keys on (`login_password`, `oauth2_token`,
   `oauth2_refresh_token` — verified against live source, NOT the symbolic Java
   identifiers);
3. the **cross-cutting invariants** the production adapter must guarantee: AES-256-GCM /
   Android Keystore encryption, the one-time plaintext→ciphertext migration semantics
   (idempotent, rollback-safe, no plaintext residue), and the `credentials.xml`
   backup-exclusion invariant.

## Contract Boundary

| Side | Component | Role across boundary |
|------|-----------|----------------------|
| **Producer** | `EncryptedPrefsSecretStore` (the `EncryptedSharedPreferences` adapter specified in DES-MODERNIZATION-004 §Cryptographic Construction) | Implements `SecretStore`; owns the crypto construction, the migration routine, and the file-name/backup-exclusion invariant. The test-only `InMemorySecretStore` fake implements the same surface (and the migration ordering) but is **exempt** from the crypto and backup-exclusion clauses. |
| **Consumer** | `AuthPreferences` (`app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java`) via its `getCredentials()` seam at `:221-226`. Downstream consumers (OAuth2/IMAP credential readers) are unaffected — they go through `AuthPreferences`'s unchanged public typed accessors. | Calls the port; MUST treat a `null` return as "credential unavailable / re-auth", never as a fatal error. |
| **Injection** | Phase 1: manual construction (mirroring the existing dual-constructor Humble-Object seam). Phase 2: Hilt `@Binds @Singleton SecretStore ← EncryptedPrefsSecretStore` (DES-MODERNIZATION-008 / MU-007). The port is what makes the Phase-1-manual → Phase-2-Hilt swap mechanical and contract-invisible. | — |

This is a **service / in-process interface** contract — not a network or data-schema
contract. There is no wire format; the "payloads" are Java method arguments and return
values, and the persisted artifact is a local encrypted `SharedPreferences` file.

## Contract Definition

### Interface: `SecretStore`

```
Interface: com.zegoggles.smssync.preferences.SecretStore   // package per DES-004 (domain stays androidx-crypto-free)

Methods:
  - get(key: String)                : String     // decrypted plaintext value, or null if absent / undecryptable
  - put(key: String, value: String) : void       // encrypt-and-persist synchronously (commit, not apply)
  - remove(key: String)             : void        // delete a single key, synchronously
  - contains(key: String)           : boolean     // true iff the key is present in the store
  - clear()                         : void        // remove all keys (used by clearOauth2Data-style flows + tests)

Migration entry point (producer-side obligation; see Validation Rules §Migration):
  - migrateFromPlaintext()          : void        // one-time, idempotent, rollback-safe; no-op once MIGRATION_COMPLETE marker is set
```

Type notes (binding):
- `key` and `value` are `String`. Values are UTF-8 text (the three secrets are all string
  tokens/passwords; matches the existing `getString`/`putString` idiom at
  `AuthPreferences.java:73, 91, 120`).
- `get` returns **`null`**, never throws, for: (a) a never-written key, and (b) a present
  key whose ciphertext cannot be decrypted (Keystore-loss / `AEADBadTagException` /
  `InvalidKeyException`). See **Error Handling**.
- `put` MUST be **synchronous and durable on return** (`commit()` semantics, matching the
  existing `.commit()` calls at `:88, 92, 95, 103, 108, 120`) so the migration durability
  barrier (Validation Rules §Migration step 3) is meaningful. `apply()` is non-conformant.
- The interface is intentionally a thin key/value port (not typed `getImapPassword()` etc.)
  so the typed semantics stay in `AuthPreferences` and the port stays storage-agnostic and
  trivially fakeable. This matches DES-004's ASCII port diagram
  (`get/put/remove/contains/clear`).

### Backing key set (EXACT on-disk strings — binding)

The producer MUST key the store on these exact strings. These are the **stored
preference-key strings**, verified this session against `AuthPreferences.java` — they are
NOT the Java identifier names and NOT the symbolic labels used in REQ-MODERNIZATION-004 /
ARCH-009:

| Secret | On-disk key string | Source-of-truth |
|--------|--------------------|-----------------|
| Gmail IMAP app-password | `login_password` | `AuthPreferences.IMAP_PASSWORD`, `:37` |
| OAuth2 access token | `oauth2_token` | `AuthPreferences.OAUTH2_TOKEN`, `:33` |
| OAuth2 refresh token | `oauth2_refresh_token` | `AuthPreferences.OAUTH2_REFRESH_TOKEN`, `:34` |
| Migration completion marker | `__secretstore_migration_complete__` | this contract (new; written inside the encrypted store) |

> Note: `oauth2_user` (`:32`) and `login_user` (`IMAP_USER`, `:36`) are **NOT** secrets and
> do **NOT** pass through this port — they live in the default `SharedPreferences`
> (`preferences` field, `:69, 87, 124, 212`) and remain unencrypted/plaintext. Only the
> three rows above cross this boundary. A producer that encrypts the username keys, or a
> consumer that routes them through the port, violates this contract.

### Backing-store invariant: file name and backup exclusion (binding)

- The production adapter MUST back the store with a `SharedPreferences` file whose name
  preserves the **backup-exclusion invariant**: the file matched by
  `<exclude domain="sharedpref" path="credentials.xml"/>` in
  `app/src/main/res/xml/backup_descriptor.xml` (verified this session) MUST remain excluded
  from Android auto-backup.
  - **Option A (preferred, per DES-004):** reuse file name `"credentials"` → on-disk
    `credentials.xml` → existing `<exclude>` matches with **zero descriptor edit**.
  - **Option B:** a distinct file name (e.g. `"credentials_enc"`) → the implementing story
    MUST add `<exclude domain="sharedpref" path="credentials_enc.xml"/>` to
    `backup_descriptor.xml`, or the contract is violated.
- Whichever option is chosen, the invariant the contract enforces is: **no file holding the
  three secret keys (in plaintext or ciphertext) is ever included in Android auto-backup.**

## Versioning
- **Current version: v1.**
- **Breaking changes** (require a new contract version + all consumers updated): changing or
  removing any of the five method signatures; changing a return type; changing any of the
  three on-disk key strings (`login_password` / `oauth2_token` / `oauth2_refresh_token`) —
  doing so would orphan existing installs' stored secrets; changing `get` to throw instead
  of returning `null` on decrypt failure (would break the consumer's re-auth contract);
  weakening `put` from `commit()` to `apply()` (breaks the migration durability barrier).
- **Backward-compatible** (no version bump): swapping the producer implementation behind the
  port (EncryptedSharedPreferences ↔ raw-Keystore ↔ InMemory fake); the Phase-1-manual →
  Phase-2-Hilt injection swap; bumping `androidx.security:security-crypto` (e.g. 1.0.0
  `MasterKeys` idiom ↔ 1.1.x `MasterKey` idiom) — the port shields callers from the crypto
  API choice; changing StrongBox best-effort behavior.

## Validation Rules

Both sides MUST honor the following.

### Cryptographic guarantee (producer; AC-1)
- Stored **values** MUST be encrypted with **AES-256-GCM** (authenticated encryption:
  confidentiality + integrity per value).
- Stored **keys** MUST be encrypted with **AES-256-SIV** (the library-mandated deterministic
  pairing), so the three key strings above are themselves ciphertext at rest.
- The data-encryption key MUST be wrapped by an **AES-256-GCM master key held in the Android
  Keystore** (non-exportable; StrongBox-backed best-effort). Plaintext key material MUST NOT
  leave the Keystore.
- The `InMemorySecretStore` test fake is exempt from this clause (it backs onto a `HashMap`);
  it MUST still honor the method-surface, migration-ordering, and `null`-on-absent clauses.

### One-time migration semantics (producer; AC-2, AC-3) — contract clauses
The producer's `migrateFromPlaintext()` MUST satisfy ALL of:
1. **Idempotent.** Guarded by the `__secretstore_migration_complete__` marker written
   *inside the encrypted store*. If the marker is present, the routine is a no-op (return
   immediately). Running it twice MUST NOT throw and MUST leave values and the single marker
   stable.
2. **Rollback-safe ordering — the safety invariant: plaintext is NEVER cleared until
   ciphertext is durably committed.** Required order:
   1. if marker present → return;
   2. buffer the present legacy plaintext values in memory;
   3. `put` each present secret as ciphertext, then `put` the marker, then **synchronously
      commit** (durability barrier);
   4. only after step 3's commit succeeds, clear the legacy plaintext entries.
3. **No plaintext residue (AC-3).** After a completed migration, the three key strings MUST
   NOT be readable as plaintext from any backing file. (Option A same-file: the encrypted
   store overwrites in place; Option B: the legacy `credentials.xml` entries are cleared.)
4. **Interruption safety (AC-2).** At every crash point at least one store holds the
   secrets; there MUST be no window in which both the plaintext and the encrypted store lack
   a secret. Crash before step-3 commit → replay from intact plaintext; crash after step-3
   commit before step-4 clear → marker short-circuits and an opportunistic clear removes any
   residue.

### Backup exclusion (producer; AC-4)
The file-name/exclusion invariant in §"Backing-store invariant" above MUST hold for the
chosen option. The plan/implementation gate MUST confirm which option is taken and that the
`<exclude>` rule matches the resulting file.

### Consumer obligations (`AuthPreferences`; AC-5)
- The consumer MUST NOT change its public typed-accessor signatures (`getOauth2Token`,
  `getOauth2RefreshToken`, `setOauth2Token`, `clearOauth2Data`, `setImapPassword`, and the
  private `getImapPassword`). Only the `getCredentials()` seam body is redirected to the
  port.
- The consumer MUST treat a `null` from `get` as "credential unavailable" (→ re-auth flow),
  identical to today's `getString(key, null)` behavior — so the swap is observably
  transparent to existing logic (`hasOAuth2Tokens()` `:80`, `isLoginInformationSet()`
  `:153`).

## Error Handling
- **Decrypt failure / Keystore-loss** (factory reset, Keystore corruption, app-data clear):
  the producer MUST catch `AEADBadTagException` / `InvalidKeyException` (and related
  `GeneralSecurityException`/`IOException` from store construction or read) and surface it as
  **`get` → `null`**, NOT as a thrown exception or crash. Rationale (DES-004 trade-off,
  accepted): the only consequence is a one-time re-auth; a hard failure would strand the
  user.
- **Write failure** (`put` cannot commit): the producer MAY propagate as an unchecked
  exception (write-time failures are not part of the silent-`null` contract, which governs
  reads only) — but the migration routine MUST NOT have cleared any plaintext if its
  ciphertext commit did not succeed (Validation Rules §Migration step 2).
- The port has **no checked exceptions** in its signatures; all failure modes are expressed
  as `null` (reads) or unchecked exceptions (writes).

## Example Payloads

In-process call examples (no wire format — this is a Java interface).

```java
// --- Consumer: AuthPreferences.getCredentials() seam, post-redirect (AC-5: public API unchanged) ---
public String getOauth2Token() {
    return secretStore.get("oauth2_token");           // was: getCredentials().getString(OAUTH2_TOKEN, null)
}
public void setOauth2Token(String username, String accessToken, String refreshToken) {
    preferences.edit().putString(OAUTH2_USER, username).commit();   // username stays plaintext (NOT a secret)
    secretStore.put("oauth2_token", accessToken);                   // synchronous commit
    secretStore.put("oauth2_refresh_token", refreshToken);
}
public void clearOauth2Data() {
    secretStore.remove("oauth2_token");
    secretStore.remove("oauth2_refresh_token");
    // ...preferences.remove(OAUTH2_USER); token invalidation unchanged...
}

// --- Producer: EncryptedPrefsSecretStore.migrateFromPlaintext() — rollback-safe ordering ---
void migrateFromPlaintext() {
    if (encrypted.contains("__secretstore_migration_complete__")) return;          // 1. idempotent no-op
    SharedPreferences legacy = ctx.getSharedPreferences("credentials", MODE_PRIVATE);
    String pw  = legacy.getString("login_password", null);                          // 2. buffer in memory
    String at  = legacy.getString("oauth2_token", null);
    String rt  = legacy.getString("oauth2_refresh_token", null);
    SharedPreferences.Editor e = encrypted.edit();                                  // 3. ciphertext first...
    if (pw != null) e.putString("login_password", pw);
    if (at != null) e.putString("oauth2_token", at);
    if (rt != null) e.putString("oauth2_refresh_token", rt);
    e.putString("__secretstore_migration_complete__", "1");
    e.commit();                                                                     //    ...durability barrier
    legacy.edit().remove("login_password")                                          // 4. ONLY now clear plaintext
                 .remove("oauth2_token").remove("oauth2_refresh_token").commit();
}
```

Crash-safety (informative; matches DES-004 table):

| Crash point | Plaintext | Encrypted | Next-launch recovery |
|---|---|---|---|
| Before step-3 commit | intact | no marker | replay from plaintext (idempotent) |
| After step-3 commit, before step-4 clear | intact (Opt B) / overwritten (Opt A) | complete + marker | marker short-circuits; opportunistic clear |
| After step-4 | cleared | complete + marker | steady-state no-op |

## Dependencies
- **DES-MODERNIZATION-004** (Encrypted Credential Store Design) — the source design; this
  contract is the binding interface extraction of its §Integration Contracts candidate.
- **REQ-MODERNIZATION-004** (MU-004, Secret-at-Rest Hardening) — governing requirement
  (AC-1…AC-6, CWE-312).
- **DES-MODERNIZATION-008** (Hilt DI) — Phase-2 injection of this port (`@Binds`); MUST NOT
  contradict this contract (currently a stub — see DES-004 §Risks).
- **DES-MODERNIZATION-002** (Transport Security Hardening) — co-requisite, shares the
  `AuthPreferences` seam; MU-003 (`migrate()` TLS rewrite) lands before MU-004 inserts the
  credential-migration call. No interface dependency, edit-ordering only.
- **REQ-MODERNIZATION-001** (AndroidX / SDK 35 / minSdk 21 platform) — enables
  `androidx.security:security-crypto`; producer-side build dependency.

## Notes

Body authored against live source (`AuthPreferences.java`, `backup_descriptor.xml` re-read this session); on-disk key strings and the private `getCredentials()` seam verified. Frontmatter is Artifact-Librarian-owned and was left unmodified — this contract is **not finalized** (still `draft`).
