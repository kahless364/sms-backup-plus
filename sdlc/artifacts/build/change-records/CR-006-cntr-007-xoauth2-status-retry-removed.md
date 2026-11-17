---
status: approved
severity: high
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-019
  - REQ-MODERNIZATION-012
design_docs:
  - CNTR-MODERNIZATION-007
related_stories:
  - U-062
tags:
  - change-record
  - cntr-007
  - thunderbird-migration
  - part-b-precondition
  - oauth
id: CR-006
title: 'CNTR-MODERNIZATION-007: XOAuth2FailedException no longer carries getStatus(); the engine status-400 token refresh is removed and the token provider refreshes when the library invalidates the token (P-2, product owner Record 4)'
type: RCR
domain: change-records
summary: 'P-2: CNTR-MODERNIZATION-007 says XOAuth2FailedException preserves getStatus() and the engine refreshes the token when the status is 400. Upstream Thunderbird exposes no XOAUTH2 failure type and no status; it invalidates the token and retries once itself. Product-owner decision 2026-11-17 (Record 4): use upstream''s retry and remove the engine''s status-400 retry.'
updated_at: '2026-11-17T18:01:48.627Z'
---
> **CONSOLIDATED:** Change Records are now tracked as Bugs (type: bug, tags: ["change-record"]).
> A CR is a bug — a defect found during cross-layer consistency checking. Use the `bug.md`
> template with `origin: cross-layer-audit` and `tags: ["change-record"]` to preserve provenance.
> This template is retained for reference.

# CR-006: CNTR-MODERNIZATION-007 XOAUTH2 failure signalling: no status, no engine status-400 retry, refresh behind the token provider (P-2)

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-006 |
| **Type** | RCR (Requirement Change: changed behaviour on a product owner decision) |
| **Severity** | High |
| **Status** | Open |
| **Reported By** | Requirements Analyst agent, on the product owner's decision (Record 4, 2026-11-17) |
| **Date Reported** | 2026-11-17 |
| **Source** | Sprint work: story U-062 (Thunderbird mail-libraries feasibility analysis, accepted), item P-2 / PO-4 |

---

## Finding

### Description

Today the app handles a rejected Google token in the engine. The IMAP library (the vendored K-9 copy)
throws `XOAuth2AuthenticationFailedException`, which carries the HTTP status read from the server's reply.
The adapter copies that status into the app-owned `XOAuth2FailedException`. `BackupWorker` and
`RestoreWorker` read `getStatus()`; on status 400 they call `TokenRefresher.refreshOAuth2Token()` once,
build a new transport, and run again. Any other status ends the run as an authentication error. The vendored
library itself never retries (analysis line 597: "Retry and refresh: None in the library").

CNTR-MODERNIZATION-007 makes this a contract promise: `XOAuth2FailedException` "MUST expose: int getStatus()"
and the translation row says it "preserves `getStatus()`".

After the migration to Thunderbird for Android's mail libraries that promise cannot hold:

- upstream has **no XOAUTH2 failure type and no status** (the class does not exist at the pin; the failure is a plain
  `AuthenticationFailedException`);
- the status is parsed from the server's challenge into a yes/no answer and thrown away, and the underlying cause is
  not reachable, so the adapter cannot recover the real status;
- the library invalidates the token through the app-supplied `OAuth2TokenProvider` and **retries once itself**.

Inventing a status in the adapter would be wrong both ways: a constant 400 would make the engine call the token endpoint
on failures the library already treats as permanent, and -1 would stop the refresh entirely.

The product owner has decided (Record 4, item PO-4, "Use upstream's retry"): the engine's status-400 retry is removed;
the app's token provider refreshes the token when the library calls `invalidateToken()`. This record changes the contract
text to match the decision, states the new failure-signalling rules in testable terms, and lists everything the decision
removes or requires.

### Source of Truth

- The accepted feasibility analysis, `sdlc/artifacts/build/feasibility/U-062/thunderbird-mail-libraries-feasibility-analysis.md`:
  Section 5 "Q5.p", exception-mapping sub-table row `:222` (line 535) and "The three load-bearing checks" (line 541);
  the "P items" table, P-2 (line 548); Section 6 "Q6.b" item 4 (line 578), "Q6.c" (lines 583-586) and "Q6.d" rows
  "Failure signalling" and "Retry and refresh" (lines 596-597); Section 12 "Q12.d" PO-4 (line 1333) and condition 7 in
  "Q12.e" (line 1355).
- The product owner's decision, same file, appendix "Product owner decision": Record 3 (2026-11-17, `accepted`, full
  migration, line 2151) and Record 4 (2026-11-17, line 2164): row PO-4 (P-2) (line 2178), row "PO-3, PO-5, PO-6, PO-7, PO-10
  (and the two above)" (line 2180: approved as a batch, raised before any Part B design), row PO-11 route (line 2170: the code
  is consumed from a fork built as Android variants).
- Upstream facts, as the analysis cites them (C-253 `OAuth2TokenProvider`, C-299 `AuthenticationFailedException`, C-300 the retry,
  C-301 and C-302 the challenge parser, commit `ac1f0e85f63c1b600ac443d1fb2d939bffff7de5`). I did not re-open the upstream
  repository for this record; I read the analysis's statements and its citation table (lines 1693, 1739-1742).

### Evidence

**1. The contract** (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md`):

Lines 195-200:

```
mail.transport.XOAuth2FailedException                 // NEW
    extends MailException
    // Replaces k-9 com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException at the
    // boundary. MUST expose: int getStatus()  — the engine branches on getStatus()==400
    // for token refresh (BackupTask.java:184, RestoreTask.java:158). Recognized by
    // State.isAuthException() (rewired from the k-9 type at State.java:79-81).
```

Line 222:

```
| `com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException` | `BackupTask.java:168,183`, `RestoreTask.java:141,157`, `State.java:79` | `XOAuth2FailedException` (preserves `getStatus()`) | `State.isAuthException()`; engine `getStatus()==400` retry |
```

Lines 388-389 and 419-420 (the example payloads) show the same behaviour: `catch (XOAuth2FailedException e) { return handleAuthError(config, e); // branches on e.getStatus() == 400` and `throw new XOAuth2FailedException(e.getStatus(), e);`.
The contract's engine citations (`BackupTask.java`, `RestoreTask.java`) are stale: those classes no longer exist in `service/`
(the engine is `BackupWorker.kt` and `RestoreWorker.kt`).

**2. The engine** (read for this record):

- `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt`: `} catch (e: XOAuth2FailedException) {` at line 265 calling `handleAuthError(...)` at 267;
  `if (e.status == 400) {` at line 442; `if (config.currentTry < 1) {` at 444; `tokenRefresher.refreshOAuth2Token()` at 446;
  `val newTransport = mailTransportFactory.create()` at 452; `Log.w(TAG, "BackupWorker: unexpected XOAuth2 status ${e.status}")` at 478;
  `return Result.failure(workDataOf(KEY_FAILURE_REASON to "auth_error"))` at 480.
- `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt`: `} catch (e: XOAuth2FailedException) {` at line 260; `if (e.status == 400) {` at 655;
  `if (config.tries < 1) {` at 657; `tokenRefresher.refreshOAuth2Token()` at 659; `val newTransport = mailTransportFactory.create()` at 662;
  `Log.w(TAG, "RestoreWorker: unexpected XOAuth2 status ${e.status}")` at 677; `"auth_error"` at 679.
- `app/src/main/java/com/zegoggles/smssync/mail/transport/XOAuth2FailedException.java:39-53`: constructor `XOAuth2FailedException(int status, Throwable cause)` and `public int getStatus()`.
- `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java`: `throw new XOAuth2FailedException(e.getStatus(), e);` at lines 160, 177, 208, 234, 262 (five port methods) and
  `return new XOAuth2FailedException(((XOAuth2AuthenticationFailedException) e).getStatus(), e);` at line 390.
- `app/src/main/java/com/zegoggles/smssync/service/state/State.java:101-104`: `isAuthException()` returns `exception instanceof XOAuth2FailedException || exception instanceof RequiresLoginException`.

**3. The existing token refresh** (`app/src/main/java/com/zegoggles/smssync/auth/TokenRefresher.java`): `refreshOAuth2Token()` (line 42) uses the stored
refresh token if there is one (`refreshUsingOAuth2Client`, line 111) and otherwise invalidates the token and asks Android's `AccountManager` for a new one
(`refreshUsingAccountManager`, lines 59-83); it throws `TokenRefreshException` on failure. The app today passes the token to the library inside the
store URI: `AuthPreferences.getStoreUri()` (`app/src/main/java/com/zegoggles/smssync/preferences/AuthPreferences.java:247-255`) builds the SASL string
with `generateXOAuth2Token()` (line 335) and passes it as the password.

**4. The analysis' finding** (line 535, row `:222`, verdict "needs contract change (CR) (P-2)"): "**absent as a type** [C-082]. An XOAUTH2 failure is a plain
`AuthenticationFailedException(...)` [C-299], after the library has invalidated the token and retried once [C-300]" and "**no**. The status is parsed from the
challenge into a boolean and dropped [C-302, C-301]; the cause is `internal` with a private response list [C-303]".

**5. Bounds upstream enforces** (analysis line 586): "At the pin the OAuth branch is bounded: per connection open it asks the provider for a token at most twice
and calls `invalidateToken()` at most twice, and it has no path to a password mechanism, because the branch is chosen by the configured `AuthType` (Q6.b) ...
A second attempt happens only when the challenge status is '400' or cannot be read [C-302, C-301]." Line 578: after a negative response the library
"calls `invalidateToken()` and retries once with `getToken(...)`; a second failure is treated as permanent and thrown as `AuthenticationFailedException("Authentication failed", ...)`".

**6. Where the status is used, searched for this record** (`getStatus()`, `.status`, `XOAuth2FailedException`, `XOAuth2AuthenticationFailedException` in `app/src`):
production use only in the five files above; tests in six files (Impact Analysis, tests layer). No worker-level test exercises the status-400 branch: no
test under `app/src/test/java/com/zegoggles/smssync/service/` other than `state/BackupStateCoverageTest.java` mentions `XOAuth2FailedException`.

---

## Triage

### Classification

- **Type:** RCR (Requirement Change). Reason: the product owner chose between two behaviours (use upstream's retry, or patch the fork to
  expose the status) and chose to change observable behaviour: who refreshes the token, when, and with which failure types. Part of the
  record is also a design correction (the contract promises a status upstream cannot supply), which on its own would be a DCR; the
  behaviour change decides the type, because the contract text must now state new rules, not only drop a promise.
- **Root:** Design and contracts layer. CNTR-MODERNIZATION-007 promised a status that came from a vendored-library detail (the fork
  commit that parses it, analysis line 535). The engine code and the adapter code were built from that promise (stories U-025, U-026).
- **Layers Affected:** 4 (design and contracts; requirements, one related criterion; application code; tests). Documentation and the
  vendored library are described but not changed by this record.
- **Severity Justification:** High. It touches more than one future story: the adapter rewrite, the engine, the token provider,
  `State`, and a set of tests, and it removes working behaviour (the status-400 refresh) so the order of the Part B stories
  matters. It is not Low or Medium because it removes behaviour, and doing the removal in the wrong order leaves the app with no
  token refresh at all. It is not Critical, although the literal "5+ files" clause could be read that way (5 to 8 production files and
  about 8 test files will change): nothing is blocked today, no migration story exists, and the files change inside the Part B stories,
  not under this record, whose own change is to two documents. A reviewer who reads the clause literally may raise it.

---

## Impact Analysis

`change_records.layers` is not configured in `sdlc/config.yaml`. The layers below are the ones the requester named. Every row was found by a
search run for this record.

### Layer Impact Matrix

| # | Layer | Affected? | Files and lines | Occurrences | Action needed |
|---|-------|-----------|-----------------|-------------|---------------|
| 1 | Ground truth: accepted analysis, Record 3, Record 4, upstream at the pin | — | analysis lines 535, 541, 578, 586, 596-597, 1333, 1355, 2178-2180 | — | Ground truth (read-only) |
| 2 | Design and contracts | YES (root) | `CNTR-MODERNIZATION-007-mail-transport.md`: 195-200 (class block), 222 (translation row), 305 (Clause C-2 note that names the k-9 type; no change needed, describes the converter), 388-389 and 419-420 (examples); plus a new section for the failure-signalling rules | 4 places to edit | Amend with Part B design (see Timing) |
| 2a | Design: approved designs of finished work | Described, not amended | `DES-MODERNIZATION-012-modernization-debt-closure-design.md:285, 322-336, 784, 1012`; `DES-MODERNIZATION-009-mail-acl-k9-unpin-design.md:71, 181, 189, 287` | 16 matches | None; they describe the vendored state. Part B design supersedes them in its own scope |
| 3 | Requirements | Scope Concern | `REQ-MODERNIZATION-012-seal-mailtransport-acl-exception-boundary.md:51` (AC-5: the k-9 XOAuth2 exception "must continue to surface to `service.*` as the existing app-owned types (`XOAuth2FailedException` ...)" and "The retry and backoff semantics ... must be unchanged"): true for the vendored code, false after the migration. `REQ-MODERNIZATION-019-...md:84` ("Behaviour is preserved"): this record is the one authorised exception for the token refresh. REQ-019 does not mention XOAuth2, status or the token provider (search: 0 matches) | 2 | No change proposed here; see Scope Concern |
| 4 | Stories and sprints | NO action | No migration story exists. Closed stories and records mention the types (U-025, U-026, U-030, U-042, U-052 and the sprint-019 U-062 records; 17 files in `sdlc/artifacts/build/sprints` match `getStatus` or `XOAuth2FailedException`) | historical | None. The conditions of the epic (analysis row 7) are the future acceptance tests |
| 5 | Application code (`app/src/main`) | YES, later (Part B) | See "Code touched" below: 5 production files definitely, up to 3 more | 5 to 8 files | Change inside the Part B stories, not under this record |
| 6 | Tests (`app/src/test`) | YES, later (Part B) | See "Tests touched" below: 6 files with 54 occurrences of the type, 2 config tests, no worker-level test of the 400 branch exists | 8 files | Change inside the Part B stories |
| 7 | Vendored library (`k9mail-vendored`) | NO | `k9mail-vendored/src/main/java/com/fsck/k9/mail/store/imap/XOAuth2AuthenticationFailedException.java` and the status parsing stay while the library is live; removed with the module | — | None here |
| 8 | Documentation (`README.md`, `sdlc/artifacts/engagement`) | NO change | `README.md`: no match. `sdlc/artifacts/engagement/endpoints.md:17-19` and `code-location.md:116-118` say Google's June 2019 policy stops XOAuth2 writing to Gmail over IMAP; neither says anything about status or retry. See Observation | — | None; see Observation |
| | **Total** | | 4 layers changed by the later work (design, requirements if the Scope Concern is accepted, code, tests) | | |

### Code touched (application code layer)

| File | Lines | What changes |
|------|-------|--------------|
| `app/src/main/java/com/zegoggles/smssync/mail/transport/XOAuth2FailedException.java` | 1-54 (whole class) | Removed. Its only purpose is to carry the status |
| `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java` | import 38; 159-160, 176-177, 207-208, 233-234, 261-262; 389-390 | The six `XOAuth2AuthenticationFailedException` branches disappear. The adapter is rewritten against the new library anyway (analysis Q5.p, "preserved with adapter change"); the XOAUTH2 rejection arrives as `AuthenticationFailedException` and maps to `RequiresLoginException` |
| `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt` | import 43; doc 224; catch 265-267; `handleAuthError` with its doc, 426-481 | The catch and the whole function are removed. The `tokenRefresher` passed to it (created at 246) and the imports of `TokenRefresher` (33) and `TokenRefreshException` (32) are then unused in the worker |
| `app/src/main/java/com/zegoggles/smssync/service/RestoreWorker.kt` | import 55; catch 260-263; `handleAuthError` with its doc, 638-680 | Same. `tokenRefresher` (created at 191, a parameter at 224) and the imports at 44-45 become unused |
| `app/src/main/java/com/zegoggles/smssync/service/state/State.java` | import 11; comments 7-8, 28-29, 92-100; code 102 | `isAuthException()` keeps its result by testing `RequiresLoginException` alone |
| `app/src/main/java/com/zegoggles/smssync/service/BackupConfig.java` (`currentTry`, `retryWithTransport`, `retryWithStore`: 19-64); `RestoreConfig.java` (`tries`, `retryWithStore`: 29-41); `WorkerEngineFactory.kt` (`createTokenRefresher`: 90-94) | as listed | Possibly removable once their only caller is gone: a search finds `currentTry` and `tries` read only in the two `handleAuthError` functions. Whether to remove them or keep them is a design decision, not made here |
| New: an `OAuth2TokenProvider` implementation for the app | — | Required by the library; wraps `TokenRefresher` and `AuthPreferences` (Part B design). `AuthPreferences.getStoreUri()` (247-262), which today passes the SASL string as the password, is also affected by the same design |

### Tests touched (tests layer)

| File | Lines | What changes |
|------|-------|--------------|
| `app/src/test/java/com/zegoggles/smssync/mail/transport/MailExceptionTest.java` | 64-86, 102-107 (four tests) | Deleted with the class |
| `app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportTranslationTest.java` | 28-60, 146-155 (17 occurrences) | Rewritten: XOAUTH2 rejection maps to `RequiresLoginException` |
| `app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportTest.java` | 257-264, 329-335, 551-572, 719-744 (16 occurrences) | Same |
| `app/src/test/java/com/zegoggles/smssync/mail/transport/K9MailTransportPortMethodsTest.java` | 123-143, 389-412 (11 occurrences) | Same |
| `app/src/test/java/com/zegoggles/smssync/service/state/BackupStateCoverageTest.java` | 4-5, 175-181 (`state_isAuthException_forXOAuth2Failure`) | Rewritten around `RequiresLoginException`, or dropped if a test for that type already covers `isAuthException()` (not checked) |
| `app/src/test/java/com/zegoggles/smssync/service/BackupConfigTest.java` (70-78) and `RestoreConfigTest.java` (35-42) | as listed | Only if the retry methods are removed |
| `app/src/test/java/com/zegoggles/smssync/preferences/AuthPreferencesTest.java` | 62 (`testStoreUriWithXOAuth2`) | Only if the Part B design changes the store URI |
| `app/src/test/java/com/zegoggles/smssync/auth/TokenRefresherTest.java` | — | Unchanged (`TokenRefresher` stays; the provider calls it) |
| New tests | — | The tests in "New failure-signalling contract" below (analysis condition 7); no worker-level test of the old status-400 path exists to carry over |

### Cascade Analysis

**Root Layer:** Design and contracts (CNTR-MODERNIZATION-007).
**Root Defect:** The contract promises a status (`getStatus()`) and an engine retry keyed to it, which exist only because of a detail of the vendored
library that upstream does not have.
**Cascade Direction:** Full downstream (contract, then engine and adapter code, then tests; requirements only for the related criterion).

#### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Contract (class block, row `:222`, examples) | Adapter code | YES | `K9MailTransport.java:160, 177, 208, 234, 262, 390` |
| 2 | Contract | Engine code | YES | `BackupWorker.kt:265-267, 442`, `RestoreWorker.kt:260-263, 655` |
| 3 | Contract | State | YES | `State.java:102` |
| 4 | Code | Tests | YES | six test files, 54 occurrences |
| 5 | Contract | Requirements | YES (one criterion) | `REQ-MODERNIZATION-012:51` (AC-5) |
| 6 | Code | Vendored library | NO | The vendored class stays until Part B removes the module |
| 7 | Contract | Stories | NO | No migration story exists |

#### Fix Order

1. [ ] Design and contracts: amend CNTR-MODERNIZATION-007 (class block, row `:222`, examples, new failure-signalling section) — ROOT FIX
2. [ ] Requirements: decide and, if accepted, annotate or amend REQ-MODERNIZATION-012 AC-5 — CASCADE FIX
3. [ ] Application code: token provider first, then library swap, adapter, removal of the engine retry and of `XOAuth2FailedException`, `State` — CASCADE FIX (Part B stories)
4. [ ] Tests: the new tests below, deletion or rewrite of the old ones — CASCADE FIX (Part B stories)
5. [ ] Verification: a search for `getStatus()`, `XOAuth2FailedException` and `e.status` in `app/src` returns 0; the new tests pass; the live-account check is recorded — GATE

#### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| None | — | — | U-062 is done, sprint-019 is closed, and no Part B story exists |

### Scope Concern (raised, not acted on)

- REQ-MODERNIZATION-012 AC-5 (approved, implemented by the closed story U-030) will be false after the migration. Record 4 approves changes to
  CNTR-MODERNIZATION-007; it does not say whether the requirement is amended. The Solution Architect and product owner should say whether the
  same revision annotates AC-5 as superseded for XOAUTH2 by this record.
- REQ-MODERNIZATION-019 line 84 says behaviour is preserved. This record is a recorded exception for one behaviour. Whether REQ-019 should cross-reference it is for the Solution Architect.
- Removing the class `XOAuth2FailedException` (rather than keeping a status-less subtype) is my reading of what Record 4 requires; Record 4 states the retry decision,
  not the class. I propose removal because no consumer could act on a status-less subtype (the engine no longer branches, `State` treats it like `RequiresLoginException`).
  A status-less marker type is a design option for Part B; the impact list above is the same either way.

### Observation (not a finding against the record)

`sdlc/artifacts/engagement/endpoints.md` (lines 17-19) says the backup and restore IMAP connection requires an app password because XOAuth2 can no longer write
to Gmail since June 2019. The code still has the XOAUTH2 path, and Record 4 asks that the new behaviour be "proven on a live account". Whether a live Gmail account can
exercise IMAP XOAUTH2 today was not checked. If it cannot, the live proof needs an account or server that accepts XOAUTH2, which the Part B design must settle.

---

## Proposed Change

Nothing is amended by this record. These are the exact changes to apply later (see Timing).

### Change 1 — CNTR-MODERNIZATION-007, exception hierarchy (lines 195-200)

**Current text:**

```
mail.transport.XOAuth2FailedException                 // NEW
    extends MailException
    // Replaces k-9 com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException at the
    // boundary. MUST expose: int getStatus()  — the engine branches on getStatus()==400
    // for token refresh (BackupTask.java:184, RestoreTask.java:158). Recognized by
    // State.isAuthException() (rewired from the k-9 type at State.java:79-81).
```

**Proposed text:** the block is removed, and this note takes its place:

```
(removed in v3) mail.transport.XOAuth2FailedException and its getStatus().
    // The mail library exposes no XOAUTH2-specific failure type and no status, and the
    // adapter does not invent one. A rejected XOAUTH2 login surfaces as
    // service.exception.RequiresLoginException, like any other authentication failure.
    // See "XOAUTH2 failure signalling (v3)".
```

### Change 2 — CNTR-MODERNIZATION-007, translation row (line 222)

**Current text:**

```
| `com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException` | `BackupTask.java:168,183`, `RestoreTask.java:141,157`, `State.java:79` | `XOAuth2FailedException` (preserves `getStatus()`) | `State.isAuthException()`; engine `getStatus()==400` retry |
```

**Proposed text:** the row is replaced by a row for what the replacement library throws:

```
| `com.fsck.k9.mail.AuthenticationFailedException`, thrown for a rejected XOAUTH2 login after the library's own single retry (no XOAUTH2-specific type and no status exist in the library) | `BackupWorker.kt`, `RestoreWorker.kt` (via the port methods) | `RequiresLoginException` (existing) | `State.isAuthException()`; no engine retry |
```

The existing row at line 221 (`AuthenticationFailedException` to `RequiresLoginException`) already covers password failures; the two rows then differ only in the cause.

### Change 3 — CNTR-MODERNIZATION-007, example payloads (lines 388-389 and 419-420)

**Current text (388-389):**

```
} catch (XOAuth2FailedException e) {                     // app-owned (was k-9 XOAuth2AuthenticationFailedException)
    return handleAuthError(config, e);                   // branches on e.getStatus() == 400
```

**Proposed text:** both lines are deleted; `RequiresLoginException` is handled by the existing catch.

**Current text (419-420):**

```
    } catch (com.fsck.k9.mail.store.imap.XOAuth2AuthenticationFailedException e) {
        throw new XOAuth2FailedException(e.getStatus(), e);
```

**Proposed text:** both lines are deleted; the `AuthenticationFailedException` catch that follows covers the case.

### Change 4 — CNTR-MODERNIZATION-007, new section "XOAUTH2 failure signalling (v3)"

These rules are testable as written. The numbers come from the analysis, Section 6, which read the upstream code; they were not
tested at the pin and must be confirmed by the tests named in each rule (analysis condition 7).

**What the adapter throws**

- **R1.** When the server rejects XOAUTH2 authentication after the library's own retry, each of the five port methods that translate authentication
  failures today (`checkSettings`, `openFolder`, `appendMessages`, `getMessages`, `fetch`; the five sites in `K9MailTransport.java`, lines 159, 176, 207, 233, 261)
  signals `RequiresLoginException`, the same type as for a password failure. It does not throw `MailException`, no XOAUTH2-specific type, and no
  status. *Test:* a loopback IMAP server that rejects every AUTHENTICATE; each of the five port methods throws exactly `RequiresLoginException`.
  `importMessageBody` is not changed by this record: it turns every `MessagingException`, an authentication failure included, into a failure result
  (`K9MailTransport.java:307-309`), and authentication has already happened in the earlier calls.
- **R2.** The adapter never fabricates a status (no constant 400, no -1) and the port has no status accessor. *Test:* a search for `getStatus` and
  `XOAuth2FailedException` in `app/src` returns 0.

**What the engine does**

- **R3.** The engine has no XOAUTH2-specific handling. A `RequiresLoginException` from a login that used XOAUTH2 ends the run exactly as a password
  failure does: `Result.failure` with reason `auth_failed` (the mapping already in `BackupWorker.kt:268-271` and `RestoreWorker.kt:264-266`).
  *Test (new, worker level):* a fake transport whose `checkSettings` throws `RequiresLoginException`; the result is `Result.failure` with reason
  `auth_failed`; `mailTransportFactory.create()` was called once; `TokenRefresher.refreshOAuth2Token()` was called zero times by the worker.
- **R4.** The engine does not retry an authentication failure: no second transport is created for it and no second run is started by the worker for it.
  The reasons `auth_error`, `auth_error_after_refresh` and `token_refresh_failed` are no longer produced. (A search of `app/src` finds these strings only at
  `BackupWorker.kt:469, 472, 480` and `RestoreWorker.kt:668, 671, 679`; no other code or test reads them.)

**How many attempts**

- **R5.** Per connection open the library makes at most 2 authentication attempts: at most 2 `getToken` calls and at most 2 `invalidateToken()` calls, and a second
  attempt happens only after a first rejection whose status is 400 or unreadable. A provider that returns the same rejected token on both calls produces exactly 2
  attempts and then R1; there is no third attempt. *Test (analysis condition 7, second test):* a provider returning the same token twice; the server sees exactly 2
  AUTHENTICATE commands; `getToken` was called 2 times; the port method throws `RequiresLoginException`.
- **R6.** Because a failed login ends the run (R3, R4), a single run makes at most 2 authentication attempts and at most 2 token refreshes before it stops. There is no loop.
  *Test:* the run in R5 is observed end to end through the worker: 2 attempts, one `Result.failure`.

**What the token provider does**

- **R7.** The app's `OAuth2TokenProvider` refreshes behind the library's call: `invalidateToken()` makes the provider discard the rejected token and obtain a new one through
  `TokenRefresher.refreshOAuth2Token()` (the refresh-token path or the `AccountManager` path that exist today), and the next `getToken` returns the refreshed token from
  `AuthPreferences`. *Test (analysis condition 7, first test):* a provider backed by a fake `TokenRefresher` and a server that rejects the first token and accepts the second:
  the port method succeeds; the refresher was called once; the server saw 2 AUTHENTICATE commands with 2 different tokens.
- **R8.** If the refresh fails (`TokenRefreshException`, or no new token), the provider does not return the rejected token as if it were new, and the failure reaches the
  engine as `RequiresLoginException` (R1) within R5's bound. How the provider signals the failure to the library is a Part B design decision; the observable outcome is fixed here.
  *Test:* a fake refresher that throws; the port method throws `RequiresLoginException`; the server saw at most 2 AUTHENTICATE commands.

**What must never happen**

- **R9.** No fallback to a password mechanism: for a store configured with XOAUTH2 the server never receives `LOGIN`, `AUTHENTICATE PLAIN` or `AUTHENTICATE CRAM-MD5`,
  before or after a failure. *Test:* the recording server in R5 asserts the full list of commands received.
- **R10.** No endless refresh loop: R5 and R6 hold for every input, including an unreadable challenge. *Test:* a server that answers the challenge with unparsable text; at most 2 attempts.
- **R11.** The token (rejected or refreshed) and the SASL initial response never appear in a log line or in the message of any exception produced by the adapter or the provider.
  *Test:* capture the log output and the exception messages in the tests above and assert the token value is absent.

### Change 5 — what happens to `XOAuth2FailedException` and `getStatus()` in the port

Both are removed from the port: the class, its constructor `XOAuth2FailedException(int, Throwable)` and `getStatus()`. `State.isAuthException()` returns true for
`RequiresLoginException` alone, so the status-bar and notification behaviour for a rejected token (`StatusPreference.java:399` reads `isAuthException()`) is unchanged.
No replacement type is added by this record (see the Scope Concern). The contract's changelog gets a v3 entry that names this record.

### Behaviour that changes for the user, stated plainly

1. A rejected token is retried by the library inside the connection call (up to 2 attempts) instead of by the worker with a new transport. The result for the user is the
   same: one refresh, one more try.
2. The library also retries when the status cannot be read, which today's engine does not (today only status 400 refreshes). This is a small widening of the retry.
3. A refresh failure now shows as `auth_failed`; the reasons `token_refresh_failed` and `auth_error_after_refresh` disappear.
4. The refresh runs inside the library's connection call on the calling thread, bounded by the provider's token timeout (the library's default is 30000 ms, analysis line 583).

---

## Timing

The contract describes the vendored implementation, and the vendored library stays the live code until the migration stories land. So:

- **When to apply.** Apply Changes 1 to 5 with the Part B design, as a new version of CNTR-MODERNIZATION-007 approved together with that design. This record is a
  precondition of Part B design (Record 4: raised before any Part B design); it is approved first. The rules in Change 4 become true only with the migration stories
  that replace the vendored module, so the contract version that states them is `approved` at design time and implemented by those stories.
- **Order inside Part B (what this record requires of the stories).** The token provider with its refresh and its tests (R7, R8) must exist, and pass, before or in the same
  change set as the removal of the engine retry. The engine retry and `XOAuth2FailedException` are removed in the same change set as the library swap, because the vendored
  exception that feeds them no longer exists after the swap.
- **What must not happen before then.** (a) `XOAuth2FailedException`, `getStatus()`, the `handleAuthError` functions or the catch clauses must not be removed or changed
  while the vendored library is live: the vendored library does not retry (analysis line 597), so removing the engine retry first would leave an expired token with no refresh
  at all. (b) The contract must not be edited before approval and design time; today it is true of the live code. (c) No code or test is changed under this record.
  (d) The adapter must never synthesise a status to keep the old engine working.
- **Different timing considered.** Removing the status-400 retry before the migration was rejected for reason (a). Applying the contract text now was rejected because it would
  describe a behaviour the live code does not have.

---

## Suggested Approach
<!-- Advisory only. Written during intake by the Solution Architect lightweight pass.
     Refined or replaced during story planning. Not authoritative. -->

Advisory only — refined during story planning.

**What changes architecturally.** Today the engine retries, keyed to a status taken from inside the vendored library. After the change, the library's connection call retries and the app plugs in a token provider (Fowler, *PoEAA*, "Plugin"). The port gets narrower: one exception type and its status disappear. **On removing `XOAuth2FailedException`:** I agree with removal rather than a status-less marker type. `State.isAuthException()` would treat a marker exactly like `RequiresLoginException`. The only distinction users see between an OAuth failure and a password failure is already made by auth mode, not by exception type (`StatusPreference.java:320-324` reads `useXOAuth()`). Record 4 decides the retry, not the class, so the CNTR-MODERNIZATION-007 v3 changelog should state the class removal as a recommendation the product owner may overrule. It is a breaking change for the engine under the contract's own policy (contract lines 331-337), which is why it is a new major version. RCR and High: agreed.

**Design detail to settle in Part B.** R7 places the refresh inside `invalidateToken()`. Keep `invalidateToken()` cheap (mark the token stale) and do the network refresh in the next `getToken(timeout)`. The refresh is then bounded by the timeout the library passes (analysis line 583: 30000 ms by default). The outcome R7 and R8 can observe is the same, and no unbounded blocking call sits in a method the library does not time.

**Cascade.** The contract changes first (v3, with CR-005). The adapter, the engine (`BackupWorker.kt` 265-267 and 426-481, `RestoreWorker.kt` 260-263 and 638-680), `State.java:102` and about eight test files follow. Timing rule (a) is right and is the main ordering constraint: the vendored library never retries, so the engine retry must not be removed before the swap.

**Story shape and order.** No story is needed for the text. The code is one Part B story (provider, adapter mapping, removal of the engine retry), delivered in or right after the library-swap change set. It cannot be split across the swap.

**Cross-record links.** CR-008's `ImapStore.create(serverSettings, config, trustedSocketFactory, oauthTokenProvider)` (analysis line 632) is where this provider is injected, so the adapter is the single construction site for both. Under CR-005 the provider class lives in `mail/transport/`.

**Live-account proof: must be settled before design (product owner).** `sdlc/artifacts/engagement/endpoints.md` lines 17-19 say that since Google's June 2019 policy change "XOAuth2 can no longer be used to write messages into Gmail via IMAP" and that the app "requires a generated Gmail **app password**". `code-location.md:116-118` says the same. Read at face value, the live Gmail run of analysis condition 14 exercises the password path, not XOAUTH2, so Record 4's "proven on a live account" cannot be met on that account. Two cautions apply. First, the note is a project statement that was not re-checked against Google's current policy, and the same file is stale elsewhere: it still lists Google Play Billing, which `app/build.gradle` no longer declares. Second, the XOAUTH2 mode is still live in code (`AuthMode.XOAUTH`; `AuthPreferences.java:240-255`; `MainSettings.java:101`). The owner's options:
- (a) supply an account or server that accepts IMAP XOAUTH2 with APPEND;
- (b) accept the loopback-server tests R5 to R10 as the proof and record that no live XOAUTH2 proof is possible;
- (c) retire the IMAP XOAUTH2 mode. This removes most of this record's code work, but it is a scope change that needs its own RCR.

**Other Scope Concerns.** REQ-012 AC-5 (line 51) becomes false after the swap. Prefer one supersession note on REQ-012 for CR-005, CR-006 and CR-008 over three separate edits. REQ-019 line 84 ("Behaviour is preserved") should cite this record as the one authorised exception. Apply that note in the same REQ-019 pass as CR-005 and CR-010.

---

## Plan

- **Execution Window:** Backlog. Precondition of Part B design: approve before the design starts; apply with the design; the code follows in the Part B stories in the order given in Timing.
- **Assigned To:** TBD (Solution Architect for the contract revision; Part B developer for the provider and the engine change)
- **Estimated Effort:** The change record itself is a text edit of one contract (plus a decision on one criterion). The code and tests it describes are sized in Part B.
- **Sprint:** Not scheduled. This record is not a story to schedule on its own.

### Fix Checklist

- [ ] Design and contracts: CNTR-MODERNIZATION-007 amended (Changes 1 to 5), changelog v3 entry names CR-006
- [ ] Requirements: decision recorded on REQ-MODERNIZATION-012 AC-5 and the REQ-019 cross-reference (Scope Concern)
- [ ] Decision recorded on the live-account proof (Observation)
- [ ] Application code (Part B): token provider; adapter mapping; engine retry and `XOAuth2FailedException` removed; `State` updated
- [ ] Tests (Part B): R1 to R11 tests exist and pass; old XOAuth2 status tests removed or rewritten
- [ ] Verification: 0 matches for `getStatus()` / `XOAuth2FailedException` / `e.status` in `app/src`

### Next Steps

Severity is High, so the create-cr wording applies: this should go into the current or next sprint; CR filed. Run `/amp:create-stories --from-cr CR-006` to create a repair story.
Read this with the note in Plan and Timing: CR-006 is a precondition of Part B design, not a story to schedule on its own. The product owner approved it as part of a batch
(Record 4). No story should be created from it before the Part B epic is planned; the story that follows from it is the token-provider and engine story inside that epic, built
in the order given in Timing.

---

## Execution

### Changes Made

| File | Change | Layer |
|------|--------|-------|
| | (pending) | |

### Verification Evidence

(pending)

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| | | | | |

---

## Closure

- **Root Cause Category:** (pending; `root_cause_categories` is not configured in `sdlc/config.yaml`)
- **Prevention Recommendation:** (pending) Candidate: when a contract preserves a value that comes from a vendored library's internals (here a status parsed in a fork commit), say so in the contract and name the source of the value, so a library swap shows at once which promises are the library's and which are the app's.
- **Closed By:** (pending)
- **Date Closed:** (pending)
