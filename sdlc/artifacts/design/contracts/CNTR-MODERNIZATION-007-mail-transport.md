---
status: approved
artifact_type: interface-contract
consumers: []
related_requirements: []
related_design_docs: []
related_stories: []
change_records: []
id: CNTR-MODERNIZATION-007
title: ''
domain: modernization
contract_type: ''
producer: ''
---

# CNTR-MODERNIZATION-007: MailTransport — the k-9 Anti-Corruption-Layer Port

## Overview

This contract defines `MailTransport`, the app-owned **Anti-Corruption-Layer (ACL) port** that
isolates the SMS Backup+ engine (`service.*`) from the k-9 mail library
(`com.fsck.k9.mail.*`). It formalizes the single cross-component boundary introduced by
**DES-MODERNIZATION-009** (MU-008): the engine's only door to IMAP. It also fixes the
**invariant** that no `com.fsck.k9.*` type may cross this boundary — every operation
signature, return type, and thrown exception on the port is expressed in **app-owned types**.

The producer side is the k-9 adapter `K9MailTransport` (the reshaped `BackupImapStore`,
`app/src/main/java/com/zegoggles/smssync/mail/BackupImapStore.java:55`, today
`extends ImapStore`). The consumer side is the engine: `BackupTask`, `RestoreTask`, and the
`ServiceBase` construction seam (`getBackupImapStore()` at `ServiceBase.java:99` method /
`:104` construction), plus the `State` error-classification path
(`service/state/State.java:32-40`).

This contract is a **service / module-interface** contract. It is the contract gate for any
MU-008 story: per DES-MODERNIZATION-009 §Integration Contracts, no MU-008 story may be
sprint-planned until this artifact is `approved`, and the k-9-throwable → app-exception
mapping below must be **exhaustive** (DES-009 Validation check #5).

## Contract Boundary

```
  CONSUMERS  (service.* — engine + state machine; ZERO com.fsck.k9.* after MU-008)
  ┌──────────────────────────────────────────────────────────────────┐
  │ BackupTask   : openFolder → appendMessages → close                  │
  │ RestoreTask  : openFolder → getMessages → fetch → close             │
  │ ServiceBase  : obtains MailTransport (construction seam → injection)│
  │ State        : classifies thrown app-owned exceptions BY TYPE       │
  └───────────────────────────────┬──────────────────────────────────┘
                                   │  MailTransport port — APP-OWNED TYPES ONLY
  ═════════════════════════════════▼═══════════════ (the ACL invariant) ════════
  PRODUCER  (mail.transport.* — the only place k-9 transport types live)
  ┌──────────────────────────────────────────────────────────────────┐
  │ K9MailTransport implements MailTransport  (was BackupImapStore)     │
  │   • holds ImapStore / BackupFolder(ImapFolder) / FetchProfile       │
  │   • receives a resolved TrustedSocketFactory  ◀── CNTR-MODERNIZATION-001 │
  │   • resolves MailMessageHandle ⇄ k-9 Message for Preserved-Core      │
  │     converters (inside mail.* only — never crosses the port)        │
  │   • translates MessagingException &c → app-owned MailException tree  │
  └───────────────────────────────┬──────────────────────────────────┘
                                   │  k-9 ImapStore API (UNCHANGED — not rewritten)
                                   ▼
                          IMAP server (Gmail 993 / self-hosted)
```

**Producer responsibilities.** Own all `com.fsck.k9.mail.*` transport types (`ImapStore`,
`ImapFolder`, `ImapMessage`, `FetchProfile`, `ImapSearcher`, `MessagingException` and its
subtypes). Translate every k-9 throwable to an app-owned exception **before it returns to a
consumer**. Resolve the opaque `MailMessageHandle` back to a k-9 `Message` only for the
Preserved-Core converter call, entirely inside `mail.*`. Receive the resolved
`TrustedSocketFactory` from CNTR-MODERNIZATION-001 (never select trust behavior itself).

**Consumer responsibilities.** Depend only on `MailTransport` and the app-owned value/exception
types below. Never import `com.fsck.k9.*`. Classify failures by app-owned exception **type**,
never by exception **message text** (this removes the `State.java:33`
`"Unable to get IMAP prefix"` string-match — REQ-MODERNIZATION-009 AC-3).

## Contract Definition

### Interface: `MailTransport`

`com.zegoggles.smssync.mail.transport.MailTransport`

The operation set is derived from the **verified** engine call sites, not invented:
`store.checkSettings()` (`BackupTask.java:262`, `RestoreTask.java:103`); `getFolder(...)`
(`BackupTask.java:280`, `RestoreTask.java:110,113`); `folder.appendMessages(...)`
(`BackupTask.java:280`); `folder.getMessages(...)` (`RestoreTask.java:110,113`, impl
`BackupImapStore.java:148`); per-message `fetch(...)` (`RestoreTask.java:225`, impl
`BackupImapStore.java:169`); `store.closeFolders()` (`BackupTask.java:300`,
`RestoreTask.java:153`).

```
Interface: MailTransport
Methods:
  - checkSettings(): void
        throws MailException
        // replaces store.checkSettings() (k-9 ImapStore). Verifies the connection /
        // credentials before backup/restore. Verified call sites: BackupTask:262, RestoreTask:103.

  - openFolder(type: DataType, prefs: DataTypePreferences): BackupFolderHandle
        throws MailException
        // replaces getFolder(type, prefs) (BackupImapStore.java:65). DataType and
        // DataTypePreferences are ALREADY app-owned (com.zegoggles.smssync.mail.DataType,
        // .preferences.DataTypePreferences) — they may appear in the signature.
        // Returns an OPAQUE app-owned handle; the engine never sees ImapFolder.

  - appendMessages(folder: BackupFolderHandle, result: ConversionResult): void
        throws MailException
        // replaces folder.appendMessages(result.getMessages()) (BackupTask.java:280).
        // ConversionResult is Preserved-Core and app-owned; the adapter unwraps the k-9
        // Message list from it INSIDE mail.* — the k-9 List<Message> never crosses the port.

  - getMessages(folder: BackupFolderHandle,
                max: int, flagged: boolean, since: java.util.Date): List<MailMessageHandle>
        throws MailException
        // replaces folder.getMessages(max, flagged, since) (BackupImapStore.java:148, which
        // returns List<ImapMessage>). Returns app-owned OPAQUE handles, NOT ImapMessage.
        // 'since' nullable (engine passes null today).

  - fetch(folder: BackupFolderHandle,
          handles: List<MailMessageHandle>, profile: FetchSpec): void
        throws MailException
        // replaces message.getFolder().fetch(messages, fetchProfile, null)
        // (RestoreTask.java:225 — per-message BODY fetch; BackupImapStore.java:169 — envelope
        // DATE fetch). FetchSpec is an app-owned enum replacing k-9 FetchProfile.Item.

  - closeFolders(): void
        // replaces store.closeFolders() (BackupImapStore.java:77). No throw — current impl
        // swallows per-folder close errors (BackupImapStore.java:80-84); preserved.
```

### App-owned value types crossing the port

These replace k-9 types in port signatures. All live under
`com.zegoggles.smssync.mail.transport`.

```
Type: MailTransportConfig          // assembled by ServiceBase / Hilt; passed to the adapter
Fields:
  - storeUri: String                       [required]  // the IMAP store URI (was the String
                                                        //   ctor arg, BackupImapStore.java:58)
  - tlsPolicy: TlsTrustPolicy              [required]  // app-owned; the adapter maps it to a
                                                        //   resolved TrustedSocketFactory via
                                                        //   CNTR-MODERNIZATION-001. NOT a boolean
                                                        //   trust-all (that arm is deleted by DES-002).

Type: BackupFolderHandle           // OPAQUE app-owned handle for an open backup folder
  - implementation note: the adapter's concrete handle wraps the k-9 BackupFolder/ImapFolder;
    the wrapped k-9 reference is package-private to mail.transport and NEVER exposed.

Type: MailMessageHandle            // OPAQUE app-owned handle for a server-side message
  - the adapter resolves handle ⇄ k-9 Message for the Preserved-Core converter call only,
    inside mail.*; the engine treats it as an opaque token (uid + adapter-private k-9 ref).

Enum: FetchSpec                    // replaces k-9 com.fsck.k9.mail.FetchProfile.Item
Values:
  - ENVELOPE_DATE   // == FetchProfile.Item.DATE  (envelope sort, BackupImapStore.java:168)
  - BODY            // == FetchProfile.Item.BODY  (full body, RestoreTask.java:221)
```

> **`MailTransportConfig.tlsPolicy` is the CNTR-MODERNIZATION-001 seam.** The engine expresses
> *intent* (`SYSTEM_VALIDATED` / `PINNED_CERTIFICATE`) in an app-owned `TlsTrustPolicy`; the
> adapter alone maps it to the resolved `com.fsck.k9.mail.ssl.TrustedSocketFactory`. The k-9
> `TrustedSocketFactory` type therefore **does not cross this port** — it is consumed by the
> adapter, supplied per CNTR-MODERNIZATION-001 (the TLS-factory boundary established by
> DES-MODERNIZATION-002, which must land first; see Dependencies).

### App-owned exception hierarchy (replaces `MessagingException` at the boundary)

**Verified prerequisite:** `com.zegoggles.smssync.service.exception.LocalizableException` is an
**interface** — `int errorResourceId()` (`LocalizableException.java:3-5`), NOT a base class.
Existing exceptions follow the pattern `extends Exception implements LocalizableException`
(e.g. `RequiresLoginException.java:5`). The new ACL exceptions follow the same pattern. The
DES-009 phrase "LocalizableException base/subtype" is corrected here to this verified shape.

```
mail.transport.MailException                          // NEW
    extends Exception implements LocalizableException
    // The base app-owned transport error. errorResourceId() →
    //   R.string.err_communication_error (the existing generic-IMAP-failure string;
    //   confirm exact id at impl). Catch-all backstop so an unmapped k-9 throwable
    //   NEVER leaks across the port (DES-009 Risk row 2). Carries the original k-9
    //   throwable as getCause() for getDetailedErrorMessage() (State.java:42-57, which
    //   reads exception.getCause()).

mail.transport.TemporaryImapException                 // NEW
    extends MailException
    // errorResourceId() → R.string.status_gmail_temp_error.
    // Replaces the State.java:32-34 string-match on "Unable to get IMAP prefix".
    // The adapter throws THIS when it observes that specific k-9 condition; State then
    // selects on TYPE via the existing LocalizableException branch (State.java:35-36).

mail.transport.XOAuth2FailedException                 // NEW
    extends MailException
    // Replaces k-9 com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException at the
    // boundary. MUST expose: int getStatus()  — the engine branches on getStatus()==400
    // for token refresh (BackupTask.java:184, RestoreTask.java:158). Recognized by
    // State.isAuthException() (rewired from the k-9 type at State.java:79-81).

(reused, existing — Preserved Core, NOT new)
service.exception.RequiresLoginException
    // The app-owned target for k-9 AuthenticationFailedException. Already in
    // State.isAuthException() (State.java:81). The adapter translates k-9
    // AuthenticationFailedException → RequiresLoginException.
```

### Exception-translation mapping (the ACL core — MUST be exhaustive)

The adapter catches every k-9 throwable at the boundary and translates **before return**.
This table is the seed enumerated from verified catch sites; the implementing story must prove
it covers every `MessagingException` subtype reachable from `checkSettings`/`getFolder`/
`appendMessages`/`getMessages`/`fetch` (DES-009 Validation check #5). The `MailException`
base is the **mandatory backstop** — any k-9 throwable not matched by a more specific row maps
to `MailException`, so no k-9 type can escape.

| k-9 throwable (current, verified) | Observed at (verified) | App-owned target | Engine/State consumer |
|-----------------------------------|------------------------|------------------|------------------------|
| `MessagingException` with message `"Unable to get IMAP prefix"` | `State.java:32-34` (string-match — REMOVED) | `TemporaryImapException` | `State` LocalizableException branch (by TYPE) |
| `com.fsck.k9.mail.AuthenticationFailedException` | `BackupTask.java:170`, `RestoreTask.java:143`, `State.java:79` | `RequiresLoginException` (existing) | `State.isAuthException()` |
| `com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException` | `BackupTask.java:168,183`, `RestoreTask.java:141,157`, `State.java:79` | `XOAuth2FailedException` (preserves `getStatus()`) | `State.isAuthException()`; engine `getStatus()==400` retry |
| generic `MessagingException` (connect/folder/append/getMessages/fetch failure) | `BackupTask.java:172`, `RestoreTask.java:145`, `SmsRestoreService.java:105`, `ServiceBase.java:99,102` | `MailException` (base) | `State.getErrorMessage` LocalizableException branch |
| any other `com.fsck.k9.mail.*` throwable (incl. `IllegalArgumentException` re-wrapped at `BackupImapStore.java:129-133`) | adapter internals | `MailException` (backstop) | generic error |

> **`ServiceBase.getBackupImapStore()` (`:99` method, `:104` construction)** today declares
> `throws MessagingException` and itself throws `new MessagingException("No valid IMAP URI...")`
> (`ServiceBase.java:102`). Under this contract the seam returns a `MailTransport` and throws
> `MailException` instead; the k-9 import at `ServiceBase.java:36` is removed.

## Versioning
- **Current version:** v1.
- **Breaking change policy:** Adding a new k-9 throwable on the producer side that maps to a
  **new** app-owned exception subtype is a breaking change for `State` (a new `instanceof`
  branch may be required) — it requires a contract revision and a `State` update. Adding a new
  port operation, removing one, or changing a signature is breaking for the engine. The
  `MailException` backstop means an unmapped k-9 throwable degrades gracefully to a generic
  error rather than leaking — that is **not** a break, but it is a contract defect to be fixed.
- **Backward compatibility:** The producer may change *which* k-9 mechanism implements an
  operation (e.g. swap the resolved k-9 coordinate for the vendored `:k9mail-vendored` module
  per DES-009 ADR-009-A) without affecting consumers, as long as the port signatures and the
  exception mapping are preserved. This swappability is the entire point of the ACL.

## Validation Rules

1. **The ACL invariant — no `com.fsck.k9.*` crosses `MailTransport`.** No port method
   signature, return type, field of a crossing value type, or thrown exception names a
   `com.fsck.k9.*` type. Enforced by grep: `com.fsck.k9` in `service/` → **0**; in `State.java`
   → **0** (DES-009 AC-2, scoped per ADR-009-B). k-9 transport imports are confined to
   `mail.transport.K9MailTransport`.
2. **Classify by type, never by text.** Consumers (esp. `State`) branch on app-owned exception
   **type**. The string `"Unable to get IMAP prefix"` must not appear in `app/src/main` (AC-3).
3. **Total exception coverage.** Every k-9 throwable the boundary can emit maps to a defined
   app-owned exception; `MailException` is the mandatory backstop (no unmapped leak).
4. **Opaque handles.** `BackupFolderHandle` / `MailMessageHandle` expose no k-9 type via any
   public accessor; the wrapped k-9 reference is package-private to `mail.transport`.
5. **MIME residual stays behind the adapter** (see Notes — `BinaryTempFileBody`).

## Error Handling

All cross-boundary failures are communicated as app-owned `MailException` (or a subtype). The
adapter never lets a k-9 `MessagingException` (or subtype) propagate to a consumer. The
original k-9 throwable is preserved as `getCause()` so existing diagnostic output continues to
work: `State.getDetailedErrorMessage()` (`State.java:42-57`) reads
`exception.getCause().toString()` for the "underlying=" suffix — behavior preserved (AC-5).
`closeFolders()` does not throw (it swallows per-folder errors today,
`BackupImapStore.java:80-84`); that is preserved so `finally { closeFolders() }` blocks
(`BackupTask.java:299-301`, `RestoreTask.java:152-154`) keep their current semantics.

## Example Payloads

**Backup write path (engine, app-owned types only):**

```java
// in BackupTask.backupCursors(...) — AFTER MU-008
MailTransport transport = service.getMailTransport();   // injected; was getBackupImapStore()
transport.checkSettings();                               // throws MailException (was MessagingException)
try {
    while (!isCancelled() && cursors.hasNext()) {
        BackupCursors.CursorAndType cursor = cursors.next();
        ConversionResult result = converter.convertMessages(cursor.cursor, cursor.type);   // unchanged (Preserved Core)
        if (!result.isEmpty()) {
            BackupFolderHandle folder =
                transport.openFolder(cursor.type, preferences.getDataTypePreferences());
            transport.appendMessages(folder, result);   // adapter unwraps k-9 Message list INSIDE mail.*
            // ... maxSyncedDate bookkeeping unchanged ...
        }
    }
} catch (XOAuth2FailedException e) {                     // app-owned (was k-9 XOAuth2AuthenticationFailedException)
    return handleAuthError(config, e);                   // branches on e.getStatus() == 400
} catch (MailException e) {                              // app-owned (was MessagingException)
    return transition(ERROR, e);
} finally {
    transport.closeFolders();
}
```

**Restore read path (engine):**

```java
// in RestoreTask.restore(...) — AFTER MU-008
transport.checkSettings();
List<MailMessageHandle> handles = new ArrayList<>();
if (config.restoreSms)
    handles.addAll(transport.getMessages(
        transport.openFolder(SMS, prefs), config.maxRestore, config.restoreOnlyStarred, null));
// per-message body fetch (was message.getFolder().fetch(msgs, fp, null)):
transport.fetch(folder, Collections.singletonList(handle), FetchSpec.BODY);
DataType dataType = converter.getDataType(/* adapter-resolved message */);   // converter call lives behind the port
```

**Exception translation (producer, inside the adapter — the only place k-9 appears):**

```java
// in K9MailTransport (mail.transport) — the ACL boundary
@Override public BackupFolderHandle openFolder(DataType type, DataTypePreferences prefs)
        throws MailException {
    try {
        return new K9BackupFolderHandle(delegate.getFolder(type, prefs));   // k-9 BackupFolder wrapped, never exposed
    } catch (com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException e) {
        throw new XOAuth2FailedException(e.getStatus(), e);
    } catch (com.fsck.k9.mail.AuthenticationFailedException e) {
        throw new RequiresLoginException();                                 // existing Preserved-Core type
    } catch (com.fsck.k9.mail.MessagingException e) {
        if ("Unable to get IMAP prefix".equals(e.getMessage()))
            throw new TemporaryImapException(e);                            // was the State.java:33 string-match
        throw new MailException(e);                                          // backstop — no k-9 type escapes
    }
}
```

## Dependencies

- **CNTR-MODERNIZATION-001** (TLS socket-factory boundary). The adapter consumes a resolved
  `com.fsck.k9.mail.ssl.TrustedSocketFactory` produced by the TLS-policy layer
  (DES-MODERNIZATION-002 / MU-003). `MailTransportConfig.tlsPolicy` (app-owned
  `TlsTrustPolicy`) is the engine-side handle the adapter maps to that factory. **Sequencing is
  a hard constraint:** MU-003 (and CNTR-MODERNIZATION-001) land **before** MU-008/this contract,
  because both touch `BackupImapStore.java` and the ACL must wrap an already-validated transport
  (DES-009 §Sequencing; DES-002 §Composition). The two contracts MUST agree on the
  `TlsTrustPolicy` shape (`SYSTEM_VALIDATED` / `PINNED_CERTIFICATE`).
- **DES-MODERNIZATION-008** (Hilt). The `MailTransport` binding is provided via Hilt
  (`MailTransport` → `K9MailTransport`, or a fake in tests). Per DES-009 §Hilt the first binding
  may be hand-wired on the existing `ServiceBase.getBackupImapStore()` seam if MU-007 has not
  yet landed; the port/adapter do not depend on Hilt existing.
- **DES-MODERNIZATION-009 / MU-008** — the design this contract formalizes.

## Notes

- **`BinaryTempFileBody` MIME residual.** `SmsRestoreService.onCreate()` calls
  `com.fsck.k9.mail.internet.BinaryTempFileBody.setTempDirectory(getCacheDir())`
  (`SmsRestoreService.java:51`, import `:10`) — a k-9 MIME body-staging type leaking into the
  engine (`service.*`). Per DES-009 Context bucket 4 this is **in scope** for MU-008: the
  temp-directory configuration moves **behind the adapter** (it belongs on the `mail.transport`
  side, where the converter resolves `MailMessageHandle` → k-9 `Message`), so
  `SmsRestoreService.java:10` reaches **0** under the AC-2 engine grep. This is the notable
  catch — a MIME value type that had escaped `mail.*` into the engine, pulled back behind the
  port. The Preserved-Core MIME *converters* (`MessageConverter` &c) keep their k-9 value-type
  imports inside `mail.*` — a documented, bounded residual (DES-009 ADR-009-B), NOT a violation
  of this port's invariant (they do not cross `MailTransport`).
- **One-line note:** Contract content draft for CNTR-MODERNIZATION-007; frontmatter is
  Artifact-Librarian-owned and untouched; NOT finalized.
