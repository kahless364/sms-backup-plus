---
id: REQ-MODERNIZATION-010
title: Upgrade Play Billing to 7.x (ProductDetails API)
artifact_type: requirement
domain: modernization
status: approved
type: functional
priority: low
epic: EPIC-MODERNIZATION-004
source_analysis: 20260529-modernization
related_requirements:
  - REQ-MODERNIZATION-001
related_stories: []
related_design_docs: []
traces_to: []
change_records: []
notes: |
  Traceability: TECH-004/DEP-004 (billing portion); migration unit MU-009; source assessment 20260529-modernization. Elective (M4) — isolated to the donation subsystem.
---

# Upgrade Play Billing to 7.x

## Description

Migrate the Google Play Billing client from `2.1.0` to the current `7.x` series,
replacing the deprecated `SkuDetails` API with `ProductDetails`, and update the in-app
donation flow accordingly.

## Context

`com.android.billingclient:billing:2.1.0` is five major versions behind. Its
`SkuDetails` API is deprecated, and Google Play billing-policy enforcement mandates the
`6.x+` `ProductDetails` API. The billing integration is isolated to the donation
subsystem (`activity/donation/`, ~3 files) and has no core-engine impact, so this can
land independently once the SDK/AGP uplift is in place. SMS Backup+ has no paid tier —
billing exists solely for optional donations — so this is value-completing, not blocking.

## Acceptance Criteria

1. `com.android.billingclient:billing` is upgraded to `7.x`.
2. All `SkuDetails` usage is replaced with `ProductDetails`; no deprecated billing API remains.
3. The donation purchase flow works end-to-end (query products, launch flow, acknowledge
   purchase) verified against a test account.
4. The change is contained to `activity/donation/`; no core-engine files are modified.

## Rationale

Keeps the donation feature functional under current Google Play billing-policy
enforcement and removes a deprecated API. Low risk, isolated, but required for the
donation flow to keep working as Play deprecates `SkuDetails`.

## Constraints

- Depends on REQ-MODERNIZATION-001 (SDK/AGP level required by Billing 7.x).
- Self-contained; can land independently of the substrate-core and engine epics.

## Notes

Boundary: `activity/donation/` (DonationActivity + billing helpers, ~3 files).
Traceability: TECH-004/DEP-004 (billing); MU-009; source assessment 20260529-modernization.
