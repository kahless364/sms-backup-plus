---
artifact_type: plan
story_id: "U-028"
verdict: "PASS"
agent: "Developer"
timestamp: "2026-06-02"
---

# Plan: U-028 — Upgrade Play Billing 2.1.0 to 7.x

## Approach

Contained leaf-scope migration. Exactly 6 files are changed:
1. `app/build.gradle` — bump billing dependency to `7.1.1`
2. `DonationActivity.java` — ProductDetailsResponseListener, queryProductDetailsAsync, selectedProduct(int), checkUserDonationStatus async
3. `DonationListFragment.java` — ProductSelectionListener, index-based callback
4. `Sku.java` — ProductDetails constructor, primitive Parcel, remove originalJson
5. `DonationActivityTest.java` — updated imports + 5 new async-callback tests
6. `SkuTest.java` — updated 6-arg constructor, added fixture ID assertions

## Risk: checkUserDonationStatus async conversion
Highest-attention edit. Synchronous `queryPurchases(String)` removed in 7.x.
Replaced with `queryPurchasesAsync` callback. `endConnection()` and state dispatch move
into callback. Public signature and DonationStatusListener contract unchanged.
Tested via Mockito synchronous callback doubles.

## Prerequisite
U-003 (compileSdk 35, AGP 8.7.3) is merged. Play Billing 7.x requires modern AGP/SDK.
