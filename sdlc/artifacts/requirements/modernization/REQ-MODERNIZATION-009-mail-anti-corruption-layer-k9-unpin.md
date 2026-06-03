---
id: REQ-MODERNIZATION-009
title: Mail Anti-Corruption Layer and k-9 Dependency Unpin
artifact_type: requirement
domain: modernization
status: approved
type: functional
priority: medium
epic: EPIC-MODERNIZATION-003
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-002
related_stories: []
related_design_docs: []
traces_to: []
change_records: []
notes: |
  Traceability: DEP-005 (k-9 JitPack SHA pin), ARCH-001 (State.java:33 magic-string leak, block 32-34); migration unit MU-008; source assessment 20260529-modernization. k-9 coupling breadth: 19 files / 62 occurrences (synced with DES-MODERNIZATION-009).
---

# Mail Anti-Corruption Layer and k-9 Unpin

## Description

Introduce a `MailTransport` anti-corruption layer so that no `com.fsck.k9.*` type
crosses into application code, and unpin the k-9 mail library from its JitPack git-SHA
to a reproducible coordinate (or vendor it behind the ACL).

## Context

`mail/BackupImapStore extends ImapStore` couples directly to k-9 internal types, and
the domain `service/state/State.java:33` (magic-string block, lines 32–34) contains a
match against a k-9 exception message — a leaky abstraction that lets a vendor library shape the domain.
The dependency itself is pinned to a raw JitPack commit hash
(`com.github.jberkel.k-9:k9mail-library:eaf689025e`), so the build is unreproducible if
JitPack evicts the artifact, and no security fixes from upstream k-9/Thunderbird are
reachable. A `MailTransport` port + adapter isolates k-9 behind an app-owned interface
so the dependency can be moved or replaced without touching app code.

## Acceptance Criteria

1. A `MailTransport` port (app-owned types only) plus a k-9 adapter exists; all IMAP
   operations go through the port.
2. No `com.fsck.k9.*` import remains in the engine/domain scope outside the adapter,
   verified by grep over `service/`, `mail/`, and `service/state/`. Source currently has
   k-9 coupling across **19 files / 62 occurrences**; this AC covers the engine + domain
   surface — specifically including the in-`service/*` residuals `service/SmsBackupService.java`
   (`MessagingException`), `service/SmsRestoreService.java` (`MessagingException`,
   `BinaryTempFileBody`), plus `mail/BackupImapStore.java`. Two files are explicitly
   **carved out** (not in scope for this AC, documented residuals): `App.java`
   (`K9MailLib` bootstrap) and `preferences/AuthPreferences.java` (`AuthType` enum); and
   the MIME converter residual under `mail/` is handled per the design's bucket model.
   Synced with DES-MODERNIZATION-009's four-bucket scope.
3. The `State.java:33` magic-string match (block lines 32–34) against a k-9 exception
   message is removed; the domain no longer depends on k-9 error text.
4. k-9 is referenced by a reproducible, versioned coordinate (or vendored as a local
   module); the JitPack git-SHA pin is removed.
5. All mail conversion/IMAP tests pass through the adapter unchanged in behavior.

## Rationale

The ACL removes the last leaky vendor abstraction and makes the most operationally
risky dependency (an unreproducible SHA pin of an evolving library) swappable. It also
makes a future k-9 replacement or upgrade a contained, adapter-only change.

## Constraints

- Depends on REQ-MODERNIZATION-002 (trust-all TLS removal) — both touch
  `BackupImapStore`; sequence the security fix first, then introduce the ACL.
- k-9 has no API-compatible released coordinate; vendoring may be required as the
  fallback if a reproducible published coordinate cannot be sourced.

## Notes

Boundary: `mail/BackupImapStore.java`, `service/state/State.java:33` (magic-string block 32–34),
`service/SmsBackupService.java`, `service/SmsRestoreService.java`, new `MailTransport`
port + adapter. Carved out (documented residuals): `App.java` (`K9MailLib`),
`preferences/AuthPreferences.java` (`AuthType`).
Traceability: DEP-005, ARCH-001; MU-008; source assessment 20260529-modernization.
