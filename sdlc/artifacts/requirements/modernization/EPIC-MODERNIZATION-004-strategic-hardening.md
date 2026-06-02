---
id: EPIC-MODERNIZATION-004
artifact_type: epic
title: 'Strategic Hardening: Billing 7.x and People API (Elective)'
status: approved
domain: modernization
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-010
  - REQ-MODERNIZATION-011
related_stories: []
change_records: []
spec_ref: sdlc/analysis/20260529-modernization/reports/modernization-plan.md
notes: |
  Roadmap band: M4 (Strategic Hardening) — ELECTIVE, Month 4+. These are isolated
  integration upgrades that do not block shippability, security, or the core substrate
  swap. Migration units: MU-009 (REQ-010), MU-010 (REQ-011).
  The roadmap also lists two non-migration-unit elective items (staged Kotlin
  promotion; opt-in observability/crash reporting) that are intentionally NOT captured
  as requirements here — they are cross-cutting and can be promoted later if pursued.
  Source assessment: 20260529-modernization.
---

# Strategic Hardening (Elective)

## Description

This epic captures the elective Month-4+ integration upgrades that complete the
modernization but are not required for a shippable, secure, modernized app. Both items
are self-contained and isolated to their subsystems: upgrading Play Billing to the
current major version, and replacing the deprecated GData Contacts feed with the
People API. They are scheduled last because neither blocks any other work and both
carry low core-engine risk.

## Scope

**In scope**
- REQ-MODERNIZATION-010 (MU-009): migrate Play Billing `2.1.0` → `7.x`; replace the
  deprecated `SkuDetails` API with `ProductDetails`; update the donation flow.
- REQ-MODERNIZATION-011 (MU-010): replace the deprecated GData Contacts `m8/feeds/`
  username-resolution call with the People API behind a `ContactsPort`.

**Out of scope**
- All Phase-0, substrate-core, and engine/integration work (EPIC-001/002/003).
- Staged Kotlin promotion and opt-in observability/crash reporting — listed in the
  roadmap as elective non-MU items; not captured as requirements unless explicitly pursued.

## Success Criteria

- Play Billing on `7.x`; `ProductDetails` API in use; donation purchase flow verified
  end-to-end; `SkuDetails` removed.
- GData `m8/feeds/` call removed; People API live behind `ContactsPort`; OAuth scopes
  updated; username resolution works end-to-end with the new scopes.

## Related Requirements

- REQ-MODERNIZATION-010 — Upgrade Play Billing to 7.x (MU-009; TECH-004 billing portion).
- REQ-MODERNIZATION-011 — Migrate contacts resolution to People API (MU-010; GData m8 deprecation).

## Notes

Both requirements depend only on the Phase-0 SDK/AGP raise (REQ-001). REQ-011 relates
to Hilt (REQ-008) for adapter injection but does not hard-depend on it. Priority is
deliberately lower than EPIC-002/003 — these are value-completing, not blocking.
Traceability: MU-009, MU-010; source assessment 20260529-modernization.
