---
status: approved
severity: medium
artifact_type: change-request
requirements:
  - REQ-MODERNIZATION-012
design_docs:
  - DES-MODERNIZATION-012
  - CNTR-MODERNIZATION-007
related_stories: []
tags: []
id: CR-002
title: 'CNTR-MODERNIZATION-007 v2: narrow K9MailTransport constructor + MessageConverter.convertMessages throws clause to app-owned MailException (ACL sealing)'
type: DCR
domain: change-records
summary: 'CNTR-MODERNIZATION-007 amended v1→v2 during EPIC-MODERNIZATION-005 design phase to seal the remaining two k-9 exception leaks below the ACL boundary: (C-1) K9MailTransport public constructor narrows from throws MailException, MessagingException to throws MailException only; (C-2) MessageConverter.convertMessages narrows from throws MessagingException to throws MailException. MailTransport interface is byte-unchanged (REQ-012 AC-8). Amendment already applied in-place on 2026-06-04; this CR records it retroactively for traceability.'
---

# CR-002: CNTR-MODERNIZATION-007 v2 — K9MailTransport constructor + MessageConverter.convertMessages throws-clause narrowing (ACL sealing)

## Metadata

| Field | Value |
|-------|-------|
| **CR Number** | CR-002 |
| **Type** | DCR (Design Correction) |
| **Severity** | Medium |
| **Status** | Open |
| **Reported By** | Michael Horsley |
| **Date Reported** | 2026-06-04 |
| **Source** | Sprint work — EPIC-MODERNIZATION-005 design phase (DES-MODERNIZATION-012) |

---

## Finding

### Description

During the EPIC-MODERNIZATION-005 design phase, DES-MODERNIZATION-012 identified two residual
`com.fsck.k9.*` exception leaks that would survive the MU-008 / U-026 port wiring and prevent
REQ-MODERNIZATION-012 from being satisfiable at the story-implementation level:

1. **`K9MailTransport` public constructor** (`K9MailTransport.java:117-118`) declares
   `throws MailException, MessagingException`. Because `com.fsck.k9.mail.MessagingException` is a
   checked exception, the compiler forces every `service.*` call site (specifically
   `ServiceBase.java:171-175`) to either catch or re-declare it — meaning a k-9 type
   propagates across the ACL boundary in the constructor signature even though k-9 is otherwise
   confined to `mail.transport.*`. This defeats the ACL invariant (CNTR-MODERNIZATION-007
   Validation Rule 1: `grep com.fsck.k9 service/` → 0).

2. **`MessageConverter.convertMessages`** (`MessageConverter.java:120-121`) declares
   `throws com.fsck.k9.mail.MessagingException`. Because `MessageConverter` lives in `mail.*`
   (a permitted k-9 zone), the internal use of k-9 types is acceptable; however, the
   *declared checked exception* escapes that zone, forcing the `service.*` caller
   (`BackupTask.java:306-310`) to name `com.fsck.k9.mail.MessagingException` by FQN in a
   catch clause — the first of the two surviving FQN-catch leaks documented in REQ-012's
   Context section. This is compile-time coupling to a k-9 type from within `service.*`,
   which the U-026 import-grep (AC-10) cannot detect (no `import` statement is present; the
   FQN is used inline).

To close both leaks, CNTR-MODERNIZATION-007 was amended in place (v1 → v2) on 2026-06-04 with
two clauses:

**Clause C-1 — `K9MailTransport` constructor throws narrowing.**
The public constructor must narrow its `throws` clause to `throws MailException` only. The
`com.fsck.k9.mail.MessagingException` from `new BackupImapStoreDelegate(context, config.storeUri,
resolvedSocketFactory)` (lines 120-121) is wrapped inside the constructor body as
`catch (com.fsck.k9.mail.MessagingException e) { throw new MailException(e); }`. The k-9 import
stays inside `K9MailTransport.java` (a permitted zone); only the constructor *signature* narrows.
Effect: the `ServiceBase.java:171-175` FQN catch collapses to a plain `return new K9MailTransport(...)`.

**Clause C-2 — `MessageConverter.convertMessages` thrown-type narrowing.**
`convertMessages` must declare `throws MailException` (app-owned) instead of
`throws com.fsck.k9.mail.MessagingException`, wrapping internally as `new MailException(e)` with
cause preserved. Effect: the `BackupTask.java:306-310` FQN catch collapses to a direct
`result = converter.convertMessages(...)` covered by the existing outer `catch (MailException e)`.
`BackupWorker.kt:310` (the other live call site) already catches `MailException` at line 138 and
already imports it at line 34 — the narrowing is compile-safe at both call sites.

Both clauses are **narrowing and additive** (a `throws` clause loses a type; an in-body catch is
added). Neither touches the `MailTransport` **interface** — every port method signature and
`throws` clause in CNTR-MODERNIZATION-007 §"Interface: `MailTransport`" is byte-unchanged
(REQ-MODERNIZATION-012 AC-8). The `MailException(Throwable)` cause chain is preserved at both
new translation sites (`MailException.java:44`) so `State.getDetailedErrorMessage()` (reading
`exception.getCause().toString()` for the "underlying=" diagnostic suffix) continues to function
correctly.

### Source of Truth

- **REQ-MODERNIZATION-012** (`sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-012-seal-mailtransport-acl-exception-boundary.md`, status: approved) — the driving requirement mandating zero `com.fsck.k9.*` references in `service.*` in any form (import, FQN catch, FQN throws, FQN local). AC-7 mandates the constructor narrowing; AC-6 mandates the converter narrowing. AC-8 mandates the `MailTransport` interface remain unchanged.
- **DES-MODERNIZATION-012** (`sdlc/artifacts/design/modernization/DES-MODERNIZATION-012-modernization-debt-closure-design.md`, status: approved) — the design that discovered the two residual leaks and drove the amendment.
- **CNTR-MODERNIZATION-007 v2** (`sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md`, status: approved) — §"Adapter-construction & converter-boundary clauses (v2)" contains the authoritative specification for both Clause C-1 and Clause C-2, the Changelog, and the Versioning declaration.

### Evidence

**Current throws clauses (pre-implementation, verified against source this session):**

| Source location | Current declaration | Required by v2 |
|----------------|---------------------|----------------|
| `K9MailTransport.java:117-118` | `public K9MailTransport(Context context, MailTransportConfig config) throws MailException, MessagingException` | `throws MailException` only |
| `MessageConverter.java:120-121` | `public @NonNull ConversionResult convertMessages(final Cursor cursor, DataType dataType) throws MessagingException` | `throws MailException` |

**v2 contract clauses (verified in CNTR-MODERNIZATION-007):**
- §"Adapter-construction & converter-boundary clauses (v2)" — lines 231-328; contains Clause C-1 and Clause C-2 in full specification detail including cause-chain requirements, effect on consumer side, and invariant-strengthening rationale.
- Changelog v2 entry (2026-06-04) — items 1-5 reaffirm C-1, C-2, cause-chain preservation at both sites, interface immutability (AC-8), and narrowing/additive character.

**Live catch sites that collapse under the amendment (verified):**
- `BackupTask.java:306-310` — FQN catch `catch (com.fsck.k9.mail.MessagingException e) { throw new MailException(e); }` around `converter.convertMessages(...)`. Comment in source explicitly labels this a U-026 bounded residual pending REQ-012.
- `ServiceBase.java:171-175` — FQN catch around `return new K9MailTransport(getApplicationContext(), config)` (per contract C-1 text; not re-read to avoid scope creep, cited from contract).

**Cause-chain consumer (verified):**
- `MailException.java:44` — `public MailException(Throwable cause) { super(cause); }`. Confirmed `MailException(Throwable)` constructor exists. `State.getDetailedErrorMessage()` depends on `getCause()` returning the original `MessagingException`.

**`BackupWorker.kt` compile-safety (verified):**
- Line 34: `import com.zegoggles.smssync.mail.transport.MailException` — already present.
- Line 138: `} catch (e: MailException) {` — outer catch already covers `MailException`; `convertMessages` narrowing at line 310 is compile-safe.

**Package-private test constructor unaffected (verified):**
- `K9MailTransport.java:92-96` — `K9MailTransport(BackupImapStoreDelegate delegate, TrustedSocketFactory factory)` — no throws clause; unchanged by C-1.

---

## Triage

### Classification

- **Type:** DCR (Design Correction)

  The amendment refines and narrows an approved contract (CNTR-MODERNIZATION-007) to encode the
  ACL-purity boundary that REQ-MODERNIZATION-012 mandates. This is a design-layer correction: the
  v1 contract specified the ACL invariant in principle (Validation Rule 1) but left two constructor
  and converter `throws` clauses that would force `service.*` callers to name k-9 types — a
  gap between the contract's stated invariant and its implementable surface. The amendment closes
  that gap by pushing the translation obligation fully below the boundary.

  This is NOT a DEF (no implementation exists yet against v2; the source still shows the pre-narrowing
  signatures that v2 requires to change). This is NOT an RCR (no scope change — the ACL invariant
  was already the contract's intent; the clauses make the intent implementable, not broader).

- **Root:** Design / contract layer (CNTR-MODERNIZATION-007 v1 surface left two checked-exception
  leaks that the requirement mandate could not satisfy without contract clarification).

- **Layers Affected:** 2 — the contract itself (already amended in-place v1→v2) and the
  implementation layer (source files that must be updated when REQ-012 stories are authored and
  executed: `K9MailTransport.java`, `MessageConverter.java`, `BackupTask.java`, `ServiceBase.java`).

- **Severity Justification:** **Medium.**
  - The amendment is pre-implementation: no code has been broken or regressed; the pre-v2 source
    signatures are the expected pre-story state.
  - The contract amendment is already applied in-place (v1→v2 on 2026-06-04); the contract is
    self-consistent with REQ-012 and DES-012 as of this date.
  - The fix is isolated to one contract (CNTR-007), one requirement (REQ-012), and a small set of
    well-bounded source files. No cross-domain blast radius.
  - The change is non-blocking for any currently in-flight sprint story (sprint-001 is 26/29
    complete; the REQ-012 implementation stories have not yet been authored or sprint-planned).
  - A Medium severity is appropriate: the gap was real and material (REQ-012 would have been
    unsatisfiable against v1 without the amendment), but its discovery and resolution both occurred
    within the same design phase, before implementation began.

- **Nature:** RETROACTIVE. The amendment was applied in-place to the contract on 2026-06-04 by
  DES-MODERNIZATION-012. This CR is filed after the fact for traceability — to record the
  discovery method, the evidence for the gap, and the downstream impact on story authoring.

---

## Impact Analysis

> `change_records.layers` is not configured in `sdlc/config.yaml`; the layer cascade matrix
> below is produced by manual analysis.

### Layer Impact Matrix

| # | Layer | Affected? | Artifact / Files | Occurrences | Action Needed |
|---|-------|-----------|-----------------|-------------|---------------|
| 1 | Contract (CNTR-MODERNIZATION-007) | YES — already fixed | `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md` | Amendment applied in-place (v1→v2, 2026-06-04); §v2 clauses C-1/C-2 and Changelog present | No further action — amendment complete |
| 2 | Requirement (REQ-MODERNIZATION-012) | Cross-reference only | `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-012-...md` | AC-6, AC-7 already mandate the exact behavior; no text change needed | Confirm `change_records` frontmatter references CR-002 when this CR is closed |
| 3 | Design (DES-MODERNIZATION-012) | Cross-reference only | `sdlc/artifacts/design/modernization/DES-MODERNIZATION-012-...md` | DES-012 drove the amendment; already cites CNTR-007 in `integration_contracts` | Confirm `change_records` frontmatter references CR-002 when this CR is closed |
| 4 | Implementation — `K9MailTransport.java` | YES — pending story | `app/src/main/java/com/zegoggles/smssync/mail/transport/K9MailTransport.java:117-118, 120-121` | Constructor `throws` clause names `MessagingException`; delegate construction unwrapped | REQ-012 story must wrap delegate construction in `try/catch(MessagingException e){throw new MailException(e);}` and narrow signature to `throws MailException` only |
| 5 | Implementation — `MessageConverter.java` | YES — pending story | `app/src/main/java/com/zegoggles/smssync/mail/MessageConverter.java:120-121` | `convertMessages` declares `throws MessagingException` | REQ-012 story must change to `throws MailException`; add internal wrap `new MailException(e)` with `getCause()` preserved; add `import com.zegoggles.smssync.mail.transport.MailException` |
| 6 | Implementation — `BackupTask.java` | YES — collapse pending story | `app/src/main/java/com/zegoggles/smssync/service/BackupTask.java:306-310` | FQN catch `catch (com.fsck.k9.mail.MessagingException e)` around `convertMessages` call | REQ-012 story must remove the FQN-catch wrapper; `result = converter.convertMessages(...)` falls under existing outer `catch (MailException e)` |
| 7 | Implementation — `ServiceBase.java` | YES — collapse pending story | `app/src/main/java/com/zegoggles/smssync/service/ServiceBase.java:171-175` (per contract C-1) | FQN catch around `new K9MailTransport(getApplicationContext(), config)` | REQ-012 story must remove the FQN-catch wrapper; constructor now only declares `throws MailException` |
| 8 | Implementation — `BackupWorker.kt` | NO — already safe | `app/src/main/java/com/zegoggles/smssync/service/BackupWorker.kt:310` | `converter.convertMessages(...)` call already inside `catch (e: MailException)` outer (line 138); `MailException` already imported (line 34) | Verify compile-success after C-2 lands; no catch-clause changes needed |
| 9 | Test layer | YES — new unit test required | REQ-012 story scope | AC-4 requires a unit test: given a mock/stub throwing `MessagingException("password not set")`, the caught `MailException` must satisfy `getCause().isInstanceOf(MessagingException)` and `getCause().getMessage().equals("password not set")` | REQ-012 story author must write the cause-chain unit test |
| 10 | MailTransport interface | NO — explicitly unchanged | `mail.transport.MailTransport` (interface) | AC-8: interface is byte-unchanged | No action; verify by grep that no method signature or `throws` was altered |

### Grep-Zero Validation (post-implementation gate)

When the REQ-012 implementation stories are complete, the following grep must return zero output:

```
grep -rn "com\.fsck\.k9" app/src/main/java/com/zegoggles/smssync/service/
```

This includes FQN catch clauses, FQN `throws` declarations, FQN local variable types, import
statements, and `{@code com.fsck.k9.*}` javadoc references that would represent compile-time
coupling. The javadoc `@throws MessagingException` on `K9MailTransport.java:115` (the stale doc
comment describing the old pre-narrowing behavior) must also be updated or removed; if the word
`MessagingException` appears in a javadoc `{@code}` block inside a `service.*` file it would fail
this grep. (The javadoc on `K9MailTransport.java` itself is in `mail.transport.*` — a permitted
zone — so the stale doc is a quality issue for the REQ-012 story, not an ACL violation.)

---

## Suggested Approach

Advisory only — refined during story planning. The notes below orient the REQ-MODERNIZATION-012
story author and Solution Architect; they are not a plan, not tasks, and not acceptance criteria.

### 1. No contract-repair story is needed — the spec is already correct

This CR is **retroactive**. CNTR-MODERNIZATION-007 was amended v1 → v2 in place on 2026-06-04, and
the two narrowing clauses are already present and approved:

- **C-1** — `K9MailTransport` public constructor narrows to `throws MailException` only.
- **C-2** — `MessageConverter.convertMessages` narrows from `throws MessagingException` to
  `throws MailException` (app-owned).

There is therefore nothing to re-amend. Authoring a "fix the contract" story would be redundant
and risks re-opening an approved artifact. The remaining work is purely to ensure the REQ-012
implementation **honors** the v2 surface — it is downstream of the contract, not a change to it.

### 2. One story, not several — the ACL seal is a single coherent unit of work

The natural temptation is to split this across multiple stories (one per file, or contract vs.
consumer). That would fragment a change whose correctness is only observable end-to-end: a partial
landing leaves the codebase non-compiling or the grep-zero gate red. The advisory is to keep the
following in **one REQ-012 "seal ACL" story**, because they share a single done-condition
(`grep -rn "com\.fsck\.k9" .../service/` → 0) and must land atomically:

- **C-1** — `K9MailTransport.java` constructor narrowing + in-body `catch (MessagingException) → throw new MailException(e)`.
- **C-2** — `MessageConverter.java` `convertMessages` throws-type change + internal wrap.
- **Consumer-side FQN-catch removal** in `service.*`: `BackupTask.java:306-310` and
  `ServiceBase.java:171-175` (per contract C-1) collapse to plain calls under existing outer
  `catch (MailException e)` handlers.
- **Javadoc / comment scrub** — the stale `@throws MessagingException` doc on
  `K9MailTransport.java:115` is corrected to `@throws MailException`, and any `{@code com.fsck.k9.*}`
  references in `service.*` are removed. This belongs in the *same* story because a doc comment can
  itself fail the grep-zero gate (see risk note 4).

Splitting these would create a story that produces a component (a narrowed signature) without
owning the integration that makes it compile. The single-story framing keeps integration ownership
intact.

### 3. Constraints to carry forward into story planning

These are boundaries the implementation must respect; the story author should treat them as
non-negotiable inputs from the contract, not as open design choices:

- **MailTransport interface stays byte-unchanged (REQ-012 AC-8).** Only the `K9MailTransport`
  *concrete class* constructor and the `MessageConverter` *method* narrow. No port method signature
  or `throws` clause on the `mail.transport.MailTransport` interface may change. A quick byte-diff
  of the interface file is the cheapest guard here.
- **Cause chain preserved — the original `MessagingException` MUST remain reachable via
  `getCause()`.** Both new translation sites must wrap via `new MailException(e)`
  (`MailException(Throwable)`, `MailException.java:44`), never `new MailException(e.getMessage())`.
  `State.getDetailedErrorMessage()` reads `getCause()` for the "underlying=" diagnostic suffix;
  dropping the cause silently degrades that diagnostic.
- **The k-9 import is allowed to remain inside `K9MailTransport.java`.** That file lives in
  `mail.transport.*` — the permitted adapter zone. Only the *public throws clause* narrows; the
  in-body `catch (com.fsck.k9.mail.MessagingException e)` and the corresponding import are expected
  to stay. The seal is about what crosses the boundary in a *signature*, not about purging k-9 from
  the adapter internals.

### 4. Risk to flag during planning

- **Checked-exception narrowing must keep both live call sites compiling.** `convertMessages` (C-2)
  has two consumers: `BackupTask.java:306-310` (the FQN catch slated for removal — note this whole
  block is also scheduled for deletion under REQ-013, so coordinate ordering so the two stories do
  not collide) and `BackupWorker.kt` (already inside `catch (e: MailException)` at line 138, import
  present at line 34 — compile-safe). The story should verify *both* paths build, not just the one
  it edits.
- **The grep-to-zero check must include javadoc `{@code}` references, not only code.** A residual
  `{@code com.fsck.k9.mail.MessagingException}` in a `service.*` doc comment, or a stale FQN in a
  comment, will keep the gate red even when all executable coupling is gone. Scope the scrub to
  comments and javadoc as well as code so the done-condition is actually reachable.

---

## Cascade Analysis

**Root Layer:** Contract (CNTR-MODERNIZATION-007 v1 specification gap)

**Root Defect:** The v1 contract specified the ACL invariant (Validation Rule 1) but its
implementable surface — the `K9MailTransport` public constructor signature and the
`MessageConverter.convertMessages` thrown type — left two checked-exception leaks that would force
`service.*` callers to name `com.fsck.k9.mail.MessagingException` at compile time, directly
contradicting the invariant the contract was meant to enforce.

**Cascade Direction:** Mid-stream down (contract → implementation; requirement cross-reference
only; no UI or data-layer impact)

### Propagation Path

| Step | From Layer | To Layer | Propagated? | Evidence |
|------|-----------|----------|-------------|----------|
| 1 | Contract (CNTR-007 v1) | Implementation (`K9MailTransport.java`) | YES — pending fix | Constructor `throws` clause still names `MessagingException` at line 118 |
| 2 | Contract (CNTR-007 v1) | Implementation (`MessageConverter.java`) | YES — pending fix | `convertMessages` still declares `throws MessagingException` at line 121 |
| 3 | `MessageConverter.java` throws leak | `BackupTask.java` | YES — pending collapse | FQN catch at lines 306-310 is a compile-time coupling to `com.fsck.k9.mail.MessagingException` |
| 4 | `K9MailTransport.java` throws leak | `ServiceBase.java` | YES — pending collapse | FQN catch at lines 171-175 (per contract C-1) is a compile-time coupling |
| 5 | Both leaks | `MailTransport` interface | NO | Interface is byte-unchanged (AC-8 confirmed); no propagation |
| 6 | Both leaks | `BackupWorker.kt` | NO (already safe) | Already catches `MailException`; already imports it; C-2 narrowing is compile-safe |

### Fix Order

1. [x] Contract layer: amend CNTR-MODERNIZATION-007 v1 → v2 (Clauses C-1 + C-2) — ROOT FIX — **DONE 2026-06-04**
2. [ ] Implementation — `K9MailTransport.java`: wrap delegate construction; narrow constructor to `throws MailException` — CASCADE FIX (REQ-012 story)
3. [ ] Implementation — `MessageConverter.java`: change to `throws MailException`; add internal wrap; update javadoc — CASCADE FIX (REQ-012 story)
4. [ ] Implementation — `BackupTask.java:306-310`: remove FQN-catch wrapper — CASCADE FIX (REQ-012 story)
5. [ ] Implementation — `ServiceBase.java:171-175`: remove FQN-catch wrapper — CASCADE FIX (REQ-012 story)
6. [ ] Test layer: add AC-4 cause-chain unit test — CASCADE FIX (REQ-012 story)
7. [ ] Verification: `grep -rn "com\.fsck\.k9" app/src/main/java/com/zegoggles/smssync/service/` → 0 lines — GATE

### Affected In-Progress Work

| Story | Sprint | Phase | Action |
|-------|--------|-------|--------|
| REQ-012 stories (not yet authored) | Sprint-002 (planned) | Pre-authoring | Stories must be written against CNTR-007 v2 surface; Clause C-1 and C-2 are implementation requirements |
| U-025, U-026 (sprint-001, closed) | Sprint-001 | Complete | No action; U-026 explicitly deferred this narrowing as a bounded residual; CR records the traceability |

---

## Plan

- **Execution Window:** Current contract amendment is complete. Implementation repair: Next Sprint (sprint-002) via REQ-012 story authoring.
- **Assigned To:** TBD (REQ-012 story assignee)
- **Estimated Effort:** Part of REQ-012 story sizing (narrow scope — 4 source-file changes + 1 unit test)
- **Sprint:** Sprint-002 (REQ-012 stories)

### Fix Checklist

- [x] Contract: CNTR-MODERNIZATION-007 amended v1→v2; §"Adapter-construction & converter-boundary clauses (v2)" and Changelog entry present
- [ ] Implementation: `K9MailTransport.java` — constructor narrowed to `throws MailException`; delegate construction wrapped; `BinaryTempFileBody` call preserved; cause chain verified
- [ ] Implementation: `MessageConverter.java` — `convertMessages` narrowed to `throws MailException`; internal wrap with `new MailException(e)`; `import MailException` added; javadoc updated
- [ ] Consumer collapse: `BackupTask.java:306-310` FQN-catch removed; `ServiceBase.java:171-175` FQN-catch removed
- [ ] Test: AC-4 cause-chain unit test passes (MessagingException → MailException, getCause() verified)
- [ ] Verification: `grep -rn "com\.fsck\.k9" app/src/main/java/com/zegoggles/smssync/service/` → 0 output lines

---

## Execution

### Changes Made

| File | Change | Layer |
|------|--------|-------|
| `sdlc/artifacts/design/contracts/CNTR-MODERNIZATION-007-mail-transport.md` | Amended v1→v2 in place: added §"Adapter-construction & converter-boundary clauses (v2)" (Clauses C-1, C-2); added Changelog v2 entry; updated frontmatter `related_requirements` + `related_design_docs`; updated Versioning | Contract |

*(Implementation changes pending — tracked in REQ-012 stories)*

### Verification Evidence

*(Pending implementation of REQ-012 stories)*

### Commit Traceability

| Commit | Date | Branch | Scope | Author |
|--------|------|--------|-------|--------|
| *(CR filed retroactively — amendment applied during DES-MODERNIZATION-012 design-phase session)* | 2026-06-04 | sdlc/modernization-plan | CNTR-MODERNIZATION-007 v2 amendment | Michael Horsley |

---

## Closure

- **Root Cause Category:** Contract specification gap — the v1 contract stated an invariant (no k-9 types in `service.*`) without specifying the implementable mechanism (constructor/converter throws narrowing) needed to enforce it against checked-exception propagation rules.
- **Prevention Recommendation:** When authoring ACL contracts involving checked exceptions, explicitly specify `throws` narrowing obligations for every public adapter constructor and every mail-conversion collaborator whose declared exception is a vendor (k-9) checked type. The contract should not only state the invariant; it should enumerate every signature that must be narrowed to make that invariant physically enforceable by the Java compiler. A design review checklist item — "Does every public constructor and collaborator method on the adapter/producer side declare only app-owned checked exceptions?" — would catch this class of gap at contract-authoring time.
- **Closed By:** (pending)
- **Date Closed:** (pending)
