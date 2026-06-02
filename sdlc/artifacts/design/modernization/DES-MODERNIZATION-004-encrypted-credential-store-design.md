---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-004
related_stories: []
related_design_docs: []
integration_contracts:
  - CNTR-MODERNIZATION-003
change_records: []
type: ''
id: DES-MODERNIZATION-004
title: ''
domain: modernization
---

# DES-MODERNIZATION-004: Encrypted Credential Store Design

## Overview

This design specifies the at-rest encryption of SMS Backup+'s stored secrets — the Gmail
IMAP app-password and the OAuth2 access/refresh tokens — by introducing an app-owned
`SecretStore` port backed by a Jetpack Security `EncryptedSharedPreferences` adapter
(AES-256-GCM value encryption under an Android Keystore master key). The encryption is
landed *behind the existing, unchanged `AuthPreferences` credential-accessor seam* (the
`getCredentials()` helper at `:221-226` and the typed getters/setters that route through
it — see §Context for the AC-5 method-name nuance) so that no caller is affected, and an
existing install's plaintext
`credentials.xml` is converted to ciphertext by a one-time, idempotent, interruption-safe
migration on first launch. The pre-existing defensive segregation — a dedicated
`credentials` `SharedPreferences` file excluded from Android auto-backup — is preserved.

This design realizes **REQ-MODERNIZATION-004** (MU-004, "Secret-at-Rest Hardening"), closes
**CWE-312** (Cleartext Storage of Sensitive Information / ARCH-009), and implements
**target-state.md ADR-007** for the secrets half. It is the credential-at-rest counterpart
to the in-transit hardening in DES-MODERNIZATION-002 (CWE-295); together they close the
app's confidentiality exposure for the highest-sensitivity data it holds — a full-mailbox
Gmail credential and the user's entire message corpus.

## Context

`AuthPreferences` persists three secrets in a **plaintext** `SharedPreferences` file.
Verified this session by full read of
`app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java`:

- The three secret **preference-key constants** (the actual on-disk key strings — these
  differ from the symbolic labels used in REQ-MODERNIZATION-004 / ARCH-009, which name the
  Java identifiers, not the stored strings):
  - `IMAP_PASSWORD = "login_password"` (`AuthPreferences.java:37`)
  - `OAUTH2_TOKEN = "oauth2_token"` (`:33`)
  - `OAUTH2_REFRESH_TOKEN = "oauth2_refresh_token"` (`:34`)
- They are stored in a dedicated file, created lazily:
  `credentials = context.getSharedPreferences("credentials", Context.MODE_PRIVATE)`
  (`getCredentials()`, `AuthPreferences.java:221-226`) — the file name is literally
  `"credentials"`, persisted on disk as `credentials.xml`.
- The accessor is the **private** `getCredentials()` helper (`:221-226`); the **public**
  read/write seam is the typed accessor set that delegates to it:
  `getOauth2Token()` (`:72`), `getOauth2RefreshToken()` (`:76`), `setOauth2Token()`
  (`:85`), `clearOauth2Data()` (`:98`), `setImapPassword()` (`:119`), and the private
  `getImapPassword()` (`:236`). **Note for the implementation gate:** REQ-MODERNIZATION-004
  AC-5 names `getCredentials()`/`setCredentials()` as the "public accessor signatures."
  In the live source `getCredentials()` is **private** and there is **no method named
  `setCredentials()`** — the real public surface is the typed getters/setters above. AC-5's
  intent ("callers are unaffected") is satisfiable and is the binding requirement; the AC's
  method names are a minor inaccuracy against source and should be read as "the credential
  accessor seam." This is flagged so a downstream story does not chase a non-existent
  `setCredentials()` signature.
- The file is excluded from Android auto-backup via `res/xml/backup_descriptor.xml`
  (read this session, full content):
  `<exclude domain="sharedpref" path="credentials.xml"/>` — wired by
  `AndroidManifest.xml android:fullBackupContent="@xml/backup_descriptor"` (manifest ~line
  92, per ARCH-011).

`MODE_PRIVATE` defends against *other apps on a non-rooted device* but provides **no
at-rest encryption**: ADB backup on a debuggable/rooted device, or forensic imaging of the
data partition, recovers the secrets in cleartext (CWE-312). The existing backup-exclusion
is good *segregation* but is not *encryption*. This design replaces the plaintext store
behind the same accessor with Keystore-backed AES-256-GCM ciphertext, preserving the
segregation and the exclusion.

This design assumes the platform substrate delivered by **REQ-MODERNIZATION-001 / MU-001**
(AndroidX, `compileSdk`/`targetSdk` 35, `minSdk` 21) — `EncryptedSharedPreferences`
(`androidx.security:security-crypto`) requires AndroidX and its Keystore ergonomics are
materially simpler at `minSdk 21+` (StrongBox/`MasterKey` paths assume API 23+). MU-004 is
therefore blocked by MU-001 (build/platform gate) and MU-002 (test net), and is
co-requisite-sequenced with DES-MODERNIZATION-002 — see §Integration Design.

---

## System Architecture

### Design Decision: Introduce a `SecretStore` Port Behind the Unchanged Accessor Seam

The single architectural move is **branch-by-abstraction at the secret-storage seam**
(target-state.md Technology Principle #1; Fowler). We introduce an app-owned port,
`SecretStore`, and make the encrypted-preferences implementation an adapter behind it.
`AuthPreferences` is rewritten so its secret read/write goes through the injected
`SecretStore` instead of reaching into its own `credentials` `SharedPreferences` field
directly — **but its public method signatures do not change** (AC-5). Concretely, the body
of `getCredentials()` (`:222-223`) stops calling
`context.getSharedPreferences("credentials", MODE_PRIVATE)` and instead returns/uses the
`SecretStore`; every public typed accessor that calls `getCredentials()` keeps its exact
signature.

```
                AuthPreferences (public API UNCHANGED)
                ┌───────────────────────────────────────────────┐
   callers ───▶ │ getOauth2Token() / getOauth2RefreshToken() /   │
   (unchanged)  │ setOauth2Token() / clearOauth2Data() /         │
                │ setImapPassword() / getImapPassword()          │
                │   └── all route through getCredentials() seam  │
                └───────────────────────┬───────────────────────┘
                                        │ delegates secret I/O to
                                        ▼
                ┌───────────────────────────────────────────────┐
                │  SecretStore (app-owned PORT — contract)        │
                │   String  get(String key)                       │
                │   void    put(String key, String value)         │
                │   void    remove(String key)                    │
                │   boolean contains(String key)                  │
                │   void    clear()                               │
                └───────────────────────┬───────────────────────┘
            ┌───────────────────────────┴───────────────────────┐
            ▼ (production binding)                                ▼ (test binding)
 ┌──────────────────────────────────────────┐      ┌──────────────────────────────┐
 │ EncryptedPrefsSecretStore (adapter)        │      │ InMemorySecretStore (fake)    │
 │  EncryptedSharedPreferences                │      │  HashMap-backed, for unit test│
 │   • file "credentials" (name preserved)    │      └──────────────────────────────┘
 │   • PrefKeyEncryptionScheme  AES256_SIV    │
 │   • PrefValueEncryptionScheme AES256_GCM   │
 │   • MasterKey: AES256_GCM, Android Keystore│
 │     (StrongBox best-effort)                │
 └──────────────────────────────────────────┘
```

**Why a port and not a direct call to `EncryptedSharedPreferences`:** the port keeps the
domain free of `androidx.security.crypto.*` types, makes the adapter swappable (InMemory
fake for tests, future hardware-keystore variants), gives the one-time migration a clean
target to write into, and lets Hilt inject the store (DES-008). This is the same hexagonal
pattern target-state.md applies to `BackupScheduler`, `MailTransport`, and the
contacts/calendar ports.

### Design Decision: Cryptographic Construction (AES-256-GCM, Keystore Master Key)

The adapter constructs the store with the Jetpack Security canonical recipe:

```java
MasterKey masterKey = new MasterKey.Builder(context)            // androidx.security 1.1.x API
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setRequestStrongBoxBacked(true)                        // best-effort hardware keystore
        .build();

SharedPreferences enc = EncryptedSharedPreferences.create(
        context,
        "credentials",                                          // SAME file name — preserves exclusion
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
```

- **Value encryption: AES-256-GCM** (authenticated encryption — confidentiality +
  integrity of each stored secret), satisfying AC-1.
- **Key encryption: AES-256-SIV** (deterministic, so a given preference-key string maps to
  a stable ciphertext lookup) — the library-mandated pairing; the *key names*
  (`login_password`, `oauth2_token`, `oauth2_refresh_token`) are themselves encrypted at
  rest, so even the presence/shape of a secret is not disclosed.
- **Master key: AES-256-GCM in the Android Keystore**, non-exportable, hardware-backed
  where StrongBox is available. The data-encryption keys are wrapped by this master key;
  plaintext keys never leave the Keystore.

**Trade-off (target-state.md Risk table, accepted):** Keystore master-key loss — factory
reset, Keystore corruption, or app-data clear — renders the ciphertext undecryptable. The
adapter MUST treat an `AEADBadTagException` / `InvalidKeyException` on read as "credential
unavailable" (return `null`), not as a fatal crash, so the user is routed to
re-authenticate rather than into an unrecoverable state. This is acceptable: the only
consequence is a one-time re-auth, and the alternative (an exportable key) would reintroduce
the very at-rest exposure we are closing.

#### ADR: EncryptedSharedPreferences vs. Raw Android Keystore

**Status:** Accepted (this design; refines target-state.md ADR-007).

**Context:** Two viable mechanisms exist to encrypt the three secrets at rest:
(a) Jetpack Security `EncryptedSharedPreferences`; (b) hand-rolled encryption using the
Android Keystore directly (generate an AES key in the Keystore, encrypt each value with a
`Cipher`, manage the IV/tag, persist ciphertext into the existing `SharedPreferences`).

**Decision:** Use **`EncryptedSharedPreferences`**.

**Rationale / trade-offs:**
- **Surface-area match.** The current store *is* a `SharedPreferences`; the encrypted
  variant is a drop-in `SharedPreferences` implementation. The adapter wraps it with no
  change to the read/write idiom and — critically — **the same file name `"credentials"`**,
  so the existing backup-exclusion rule continues to match (AC-4) with zero descriptor
  change.
- **Correct crypto by construction.** Raw-Keystore code must correctly manage GCM IV
  uniqueness, authentication-tag handling, key rotation, and per-value framing — the exact
  places hand-rolled crypto fails. The library encapsulates all of this and is the
  Google-recommended baseline.
- **Maintenance posture.** A single-maintainer, maintenance-mode OSS project (README)
  should not own bespoke cryptographic framing. The library is Apache-2.0 and already in
  the target dependency set (dependencies.md license summary).
- **Rejected — raw Keystore:** more code, more failure modes, no offsetting benefit at this
  sensitivity tier. *Reconsider only* if Jetpack Security is later deprecated without a
  successor (track upstream; the port boundary makes that swap mechanical).
- **Rejected — Tink directly:** more flexible but heavier; `EncryptedSharedPreferences` is
  Tink under the hood with the SharedPreferences ergonomics we need.

**Consequences:** (+) Minimal code, correct crypto, preserved exclusion, swap-behind-port.
(−) A new `androidx.security:security-crypto` dependency and the accepted Keystore-loss
re-auth trade-off. (−) The `security-crypto` 1.1.0-alpha API (`MasterKey`) must be pinned
deliberately; if only 1.0.0 (`MasterKeys`/`MasterKeys.getOrCreate(AES256_GCM_SPEC)`) is
acceptable, the adapter uses the 1.0.0 idiom — the port shields callers from the choice
either way.

### Design Decision: One-Time, Idempotent, Rollback-Safe Plaintext → Encrypted Migration

On first launch after upgrade, existing installs hold the three secrets as plaintext in
`credentials.xml`. The migration converts them, behind the port, with an ordering that is
**safe if the process dies at any step** (AC-2, AC-3; Constraint: "idempotent and safe to
re-run if interrupted").

**Safety invariant: never clear plaintext until ciphertext is durably committed.**

```
migrateCredentialsToEncrypted():
  1. if encrypted store already has the MIGRATION_COMPLETE marker → RETURN (idempotent no-op)
  2. read the three legacy plaintext values into memory via a transient raw
     SharedPreferences("credentials", MODE_PRIVATE) handle
  3. for each present key in {login_password, oauth2_token, oauth2_refresh_token}:
        encryptedStore.put(key, bufferedValue)        // write ciphertext
     encryptedStore.put(MIGRATION_COMPLETE, "1")
     encryptedStore COMMIT (synchronous)              // DURABILITY BARRIER — must succeed before step 4
  4. ONLY AFTER step 3 commit succeeds:
        clear the plaintext entries (AC-3)
  5. (steady state) SecretStore reads/writes only the encrypted store
```

**File-name reconciliation (must be resolved at implementation/plan time):** AC-4 requires
the encrypted successor to remain backup-excluded. Two correct realizations exist; the
implementer MUST pick one explicitly:

- **Option A (preferred): keep the file name `"credentials"`.** Buffer the three plaintext
  values in memory (step 2) *before* the `EncryptedSharedPreferences` is created over the
  same `"credentials"` backing file; then write ciphertext. Because the file name is
  unchanged, the existing `<exclude path="credentials.xml"/>` continues to match with
  **zero descriptor edit** (AC-4 satisfied for free). **Key implementation risk:** the
  plaintext read (raw handle) must complete and the raw entries be cleared *before* the
  encrypted store is asked to read that file, so the library never attempts to decrypt
  pre-existing plaintext entries. This read-during-create ordering is the single
  highest-care step.
- **Option B: new encrypted file `"credentials_enc"`.** Cleaner separation (distinct read
  source vs. write target) but **requires adding
  `<exclude domain="sharedpref" path="credentials_enc.xml"/>` to `backup_descriptor.xml`**
  or AC-4 regresses. If Option B is chosen, that descriptor edit is mandatory and is part
  of this unit's scope.

The plan/implementation gate MUST confirm which option is taken and that AC-4 holds for it.

**Idempotency mechanism:** a `MIGRATION_COMPLETE` key written *inside the encrypted store*
in the same commit as the last secret (step 3). Its presence short-circuits the routine
(step 1). Re-run after completion → no-op. Re-run after interruption *before* step-3 commit
→ no marker, plaintext still intact (step 4 never ran) → replay. Re-run after step-3 commit
but step-4 interruption → marker present, short-circuit, and an opportunistic plaintext
clear removes any residue (none in the Option-A same-file case). **No window exists in
which both stores lack the secret.**

**Crash-safety case analysis:**

| Crash point | Plaintext state | Encrypted state | Recovery on next launch |
|---|---|---|---|
| Before step-3 commit | intact | no marker; partial/none | Replay from plaintext (idempotent) — user unaffected |
| After step-3 commit, before step 4 | intact (Opt B) / N/A (Opt A) | complete + marker | Marker short-circuits; opportunistic plaintext clear |
| After step 4 | cleared | complete + marker | Steady state; no-op |

This guarantees AC-2's "a failure mid-migration must not leave the user unable to
authenticate."

### Requirement Traceability

| AC (REQ-MODERNIZATION-004) | Where satisfied in this design |
|---|---|
| AC-1 EncryptedSharedPreferences, AES-256-GCM, Keystore master key | §Cryptographic Construction |
| AC-2 one-time migration read→write→clear, atomic + rollback-safe | §Migration step ordering + crash-safety table |
| AC-3 no plaintext remains post-migration | §Migration step 4 (clear after durable ciphertext commit) |
| AC-4 credentials.xml (or successor) stays backup-excluded | §Migration "File-name reconciliation"; ADR (same-file name) |
| AC-5 credential accessor signatures unchanged (callers unaffected) | §`SecretStore` Port (delegation inside `getCredentials()`; public typed accessors untouched). NB: AC-5 names `getCredentials()`/`setCredentials()`; in source the seam is private `getCredentials()` + typed setters — see Context |
| AC-6 test: migration path + fresh-install-encrypted-only | §Design Validation; CNTR test obligations |

CWE-312 closed (ARCH-009); realizes target-state.md ADR-007 (secrets half).

---

## Integration Design

### Sequencing With DES-MODERNIZATION-002 (Both Touch `AuthPreferences`)

REQ-MODERNIZATION-004 names DES-002 a **co-requisite** because both modify
`AuthPreferences`: DES-002 rewrites `migrate()` (`AuthPreferences.java:276-288`, read this
session — it currently sets `SERVER_TRUST_ALL_CERTIFICATES = true` for legacy `+ssl`/`+tls`
users, ARCH-008) to stop the silent trust-all downgrade; this design adds the
encrypted-secret migration. migration-units.md Shared-File Allocation allocates
`AuthPreferences.java` as **MU-003 (DES-002, TLS `migrate()`) → MU-004 (this design,
credential seam) → MU-007 (Hilt inject), in phase order.**

**The DES-002 `migrate()` rewrite lands first; the encryption migration layers on top.**
Rationale:
- DES-002's `migrate()` rewrite corrects an **active Critical** consent defect; it must not
  be blocked behind the encryption work.
- Landing TLS-`migrate()` first yields a single, already-corrected `migrate()` body into
  which the credential-encryption migration call is *inserted* — avoiding two agents
  editing the same method and conflicting on the exact migration path REQ-MODERNIZATION-004
  Constraints warn about ("avoid conflicting edits to the same migration path").
- Both migrations are invoked from the same first-launch hook (`App.onCreate()` →
  `preferences.migrate()`, per ARCH-008). They are independent in *effect* — TLS prefs live
  in the default `SharedPreferences` (`preferences` field, `:69`); secrets live in the
  `credentials` file (`:223`) — but share the method seam, so the sequencing is about *edit
  ownership*, not data coupling.

This design therefore takes a **hard dependency on DES-002 having landed** before its
`migrate()` insertion point exists in corrected form. (At time of writing, DES-002 is still
a template stub — flagged in §Risks.)

### Hilt Injection of `SecretStore` (DES-MODERNIZATION-008)

`SecretStore` is provided through Hilt (DES-008 / MU-007). Today `AuthPreferences` is
constructed ad-hoc (`new AuthPreferences(context)`, constructor at `:67-70`); target-state
ARCH-002/ADR-008 move it to constructor injection.

```
@Module @InstallIn(SingletonComponent.class)
abstract class SecretStoreModule {
    @Binds @Singleton
    abstract SecretStore bindSecretStore(EncryptedPrefsSecretStore impl);
}
// EncryptedPrefsSecretStore @Inject constructor(@ApplicationContext Context ctx)
// AuthPreferences @Inject constructor(SecretStore secretStore, ...)  // public typed
//                                                                    // accessor signatures
//                                                                    // UNCHANGED
```

**Sequencing nuance:** MU-004 (this design) is **Phase 1**; MU-007/DES-008 (Hilt) is
**Phase 2** — encryption lands *before* the Hilt graph exists. This is intentional: in
Phase 1 the `SecretStore` is wired via the **existing manual construction path** (a small
factory mirroring the codebase's current Humble-Object dual-constructor seam). When DES-008
lands, the `@Binds` above replaces the manual factory with the Hilt binding — a mechanical
swap behind the already-stable port, with **no change to `SecretStore`, `AuthPreferences`'s
public API, or the adapter.** The port is what makes the Phase-1-manual → Phase-2-Hilt
transition free. (DES-008 is currently a template stub — its design must not contradict
this port; flagged in §Risks.)

### Unchanged External Surface

The integration *surface* is unchanged (migration-units.md External Integration Points):
the OAuth2 token endpoint and IMAP credential consumers read through the same accessors;
tokens now live in `SecretStore`; the contacts/calendar OAuth path is unaffected. No new
network surface; no new permission.

---

## Design Validation

| Validation question | Verdict | Evidence in this design |
|---|---|---|
| No plaintext remains post-migration? | **Yes** | Step 4 clears plaintext only after the encrypted step-3 commit succeeds (AC-3); verified by a test asserting legacy plaintext keys absent after first launch. |
| Migration idempotent? | **Yes** | `MIGRATION_COMPLETE` marker short-circuit (step 1); re-run after completion is a no-op; re-run after interruption replays from intact plaintext. |
| Safe if interrupted at any point? | **Yes** | Crash-safety table: at every crash point at least one store holds the secrets; plaintext never cleared before ciphertext durably committed (AC-2). |
| Accessor signatures unchanged? | **Yes** | The redirect lives inside `getCredentials()` (`:221-226`); public typed accessors (`getOauth2Token`, `setOauth2Token`, `setImapPassword`, …) keep their exact signatures (AC-5). |
| Fresh install writes only encrypted values? | **Yes** | No legacy plaintext on fresh install; first `setOauth2Token()`/`setImapPassword()` writes through `EncryptedPrefsSecretStore`, so the `credentials` file holds only ciphertext (AC-6); migration is a marker-guarded no-op. |
| Backup exclusion preserved? | **Yes, conditional on file-name option** | Option A (same `"credentials"` name) preserves the existing `<exclude>` with no edit; Option B requires an added `<exclude>` line — flagged mandatory. The plan gate MUST confirm. |
| Keystore-loss handled without hard failure? | **Yes** | Adapter maps decrypt failures to `null` ("credential unavailable") → re-auth, not crash. |

**Required tests (AC-6, contract obligations):**
1. **Migration path:** seed plaintext `credentials.xml` with `login_password`,
   `oauth2_token`, `oauth2_refresh_token` → run first-launch migration → assert (a) each
   secret readable through `SecretStore`, (b) backing file no longer holds plaintext for
   those keys, (c) marker set.
2. **Idempotency:** run migration twice → no exception, values stable, single marker.
3. **Interruption safety:** simulate process death between step 3 and step 4 (marker set,
   plaintext not yet cleared) → re-run → secrets intact and reachable, plaintext ultimately
   cleared.
4. **Fresh install:** no plaintext → `setImapPassword()` then `getImapPassword()` (and the
   OAuth2 equivalents) round-trip through the encrypted store; backing file holds only
   ciphertext.
5. **Signature stability:** an existing caller of the typed accessors compiles unchanged
   (guards AC-5).

Pure-logic migration tests use an `InMemorySecretStore` fake to stay device-independent;
the production crypto path is exercised via Robolectric where supported (migration-units.md
MU-002 characterization gate). This aligns with the existing `AuthPreferencesTest`
(already in the suite per MU-002 backfill targets).

**Residual risks (carried, with mitigation):**
- *Keystore key loss → re-auth* — accepted (target-state.md Risk table); mitigated by
  graceful `null` and a one-time re-auth notice.
- *`security-crypto` 1.1.x alpha API churn* — pin deliberately; port isolates callers.
- *Option-A read-during-create ordering* — buffer plaintext in memory before the encrypted
  store is created over the same name; the single highest-care implementation step.

---

## Integration Contracts

> This design introduces one new cross-component boundary: the `SecretStore` port,
> consumed by `AuthPreferences` (preferences) and produced by `EncryptedPrefsSecretStore`
> (data/integration adapter). It is a **contract candidate**.

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| `SecretStore` port (get/put/remove/contains/clear + one-time migration semantics) | `EncryptedPrefsSecretStore` (EncryptedSharedPreferences adapter) | `AuthPreferences` (credential accessor seam, `getCredentials()` `:221-226`) | service (in-process interface) | _none yet_ | **needed** |

**Contract surface to specify** (for `/amp:create-contracts`):
- `String get(String key)` — decrypted value or `null` (including on Keystore-loss/decrypt
  failure; MUST NOT throw to callers).
- `void put(String key, String value)`; `void remove(String key)`;
  `boolean contains(String key)`; `void clear()`.
- Stored key set (exact on-disk strings, verified this session): `login_password`,
  `oauth2_token`, `oauth2_refresh_token`.
- **Migration semantics as contract clauses:** idempotent (marker-guarded), rollback-safe
  ordering (ciphertext durable before plaintext cleared), and the AC-4 backup-exclusion
  invariant for the chosen file-name option.

### Contracts Needed (pre-sprint gate)

- [ ] **CNTR needed:** `SecretStore` port (get/put/remove/contains/clear + one-time
  migration semantics) between `EncryptedPrefsSecretStore` and `AuthPreferences`. Because
  MU-004 crosses the preferences↔data-integration boundary, this contract MUST be created
  via `/amp:create-contracts` and reach `approved` status before any story implementing
  this design can be sprint-planned; such stories must reference the resulting CNTR-*
  artifact in their `integration_contracts` frontmatter.

## Trade-offs

- **Library (`EncryptedSharedPreferences`) over raw Keystore** — correct-crypto-by-default
  and preserved file/exclusion ergonomics vs. a new dependency and alpha-API pinning (see
  ADR). Strongly favors the library for a single-maintainer project.
- **Keystore-bound master key (non-exportable)** — strongest at-rest posture vs. a one-time
  re-auth on Keystore loss. Accepted: re-auth is cheap; an exportable key would reopen
  CWE-312.
- **Same-file-name migration (Option A)** — zero descriptor edit and automatic AC-4
  preservation vs. read-during-create ordering care. Preferred, with the ordering called
  out as the key implementation risk.

## Risks

- **Keystore master-key loss invalidates stored secrets** (target-state.md Risk table) —
  mitigated by graceful `null`-on-decrypt-failure + one-time re-auth notice.
- **AC-4 regression if Option B chosen without the added `<exclude>` line** — mitigated by
  making the descriptor edit mandatory scope under Option B plus a plan-gate check.
- **Merge conflict on `migrate()` with DES-002** — mitigated by the strict MU-003 → MU-004
  edit ordering on `AuthPreferences.java`.
- **Upstream dependency on stub designs** — DES-MODERNIZATION-002 and -008 are currently
  template stubs (verified this session). This design's `migrate()` insertion point depends
  on DES-002's corrected method, and its Hilt binding depends on DES-008's graph. Those two
  must be authored consistently before MU-004 stories are sprint-planned.
- **`security-crypto` alpha API surface** — mitigated by deliberate version pin and the
  port boundary.

## Notes

Frontmatter (id/status/artifact_type/domain/related_requirements) is Artifact-Librarian-
owned and was left unmodified. This artifact is **not finalized**.

### Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| REQ-MODERNIZATION-004 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-004-encrypt-credentials-at-rest.md | Governing requirement: 6 ACs, constraints, CWE-312, co-requisite with REQ-002 |
| target-state.md | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/target-state.md | SecretStore port, EncryptedSharedPreferences adapter, ADR-007, secure-by-default policy |
| migration-units.md (MU-004) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md | MU-004 scope, deps (MU-001/002), shared-file allocation, MU-003/007 sequencing |
| ilities-assessment.md (ARCH-009) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/architecture/ilities-assessment.md | CWE-312 evidence; credentials.xml + backup-exclusion; AuthPreferences seam |
| DES-MODERNIZATION-002 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-002-transport-security-hardening-design.md | Co-requisite TLS migrate() rewrite (currently a stub) — shared AuthPreferences seam ordering |
| DES-MODERNIZATION-008 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-008-hilt-di-design.md | Hilt injection target for SecretStore (currently a stub) |
| AuthPreferences.java | app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java | FULL READ this session: secret-key constants (`login_password`/`oauth2_token`/`oauth2_refresh_token`), `getCredentials()` `:221-226` (private), typed accessors, `migrate()` `:276-288`, constructor `:67-70` |
| backup_descriptor.xml | app/src/main/res/xml/backup_descriptor.xml | FULL READ this session: `<exclude domain="sharedpref" path="credentials.xml"/>` |
