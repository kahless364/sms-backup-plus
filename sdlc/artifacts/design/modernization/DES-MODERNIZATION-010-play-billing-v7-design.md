---
status: approved
artifact_type: design-document
applicable_prompts:
  - system-architecture
  - integration-design
  - design-validation
related_requirements:
  - REQ-MODERNIZATION-010
related_stories: []
related_design_docs: []
integration_contracts: []
change_records: []
type: ''
id: DES-MODERNIZATION-010
title: ''
domain: modernization
---

# DES-MODERNIZATION-010: Play Billing 7.x Uplift (ProductDetails) for the Donation Subsystem

## Overview

This design specifies the migration of the in-app **donation** subsystem from Google
Play Billing Library `2.1.0` to the `7.x` series, replacing the deprecated `SkuDetails`
API with `ProductDetails` and the `queryProductDetailsAsync` flow. It traces
**REQ-MODERNIZATION-010** (migration unit **MU-009**, elective M4) and is the design
counterpart to the contained, low-risk dependency uplift described there.

The change is deliberately **leaf-scoped**: it touches only the four source files in
`app/src/main/java/com/zegoggles/smssync/activity/donation/` (`DonationActivity.java`,
`DonationListFragment.java`, `Sku.java`, plus the two test files
`DonationActivityTest.java` and `SkuTest.java`) and the single dependency line
`app/build.gradle:59`. No core-engine, scheduling, mail-transport, security, or
eventing file is modified. SMS Backup+ has **no paid tier** — billing exists solely for
optional donations — so this is value-completing (keeping the donation flow functional
under current Play billing-policy enforcement), not blocking.

> **Scope note (verified via grep this session).** There is **one external consumer** of
> the donation subsystem: `activity/fragments/MainSettings.java` calls the static
> `DonationActivity.checkUserDonationStatus(...)` from `onResume()` (`MainSettings.java:69,
> 154-156`) to decide whether to show the "Donate" preference. The migration **must not
> change** the `checkUserDonationStatus(Context, DonationStatusListener)` signature or its
> `DonationStatusListener` callback contract, and — because the 7.x replacement
> (`queryPurchasesAsync`) is asynchronous — must not block `MainSettings.onResume()`
> (SCR-DON-001 §System Integration). This is the one place the "leaf" touches the rest of
> the UI; the contract is preserved, so no other file changes.

## Context

`com.android.billingclient:billing:2.1.0` (verified at `app/build.gradle:59` this
session) is five major versions behind. Its `SkuDetails` query/launch/parcel surface is
deprecated, and Google Play billing-policy enforcement now mandates the `6.x+`
`ProductDetails` API. As Google retires `SkuDetails`, the donation flow will stop
returning products and will silently break. Because the billing integration is fully
isolated to the donation subsystem and has no domain coupling, it can land independently
once the SDK/AGP uplift (REQ-MODERNIZATION-001 / `DES-MODERNIZATION-001`) is in place.

### Current state — verified by full read this session

The current donation flow (`DonationActivity.java`, read in full) is a self-contained
`SkuDetails` pipeline:

1. **Connect** — `onCreate` builds a `BillingClient` with `enablePendingPurchases()` and
   `startConnection`; on `OK` it calls `queryAvailableSkus()`.
2. **Query** — `queryAvailableSkus()` (`:207-213`) calls
   `billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder().setType(INAPP)
   .setSkusList(Arrays.asList(ALL_SKUS)).build(), this)`. The activity implements
   `SkuDetailsResponseListener`.
3. **Filter + present** — `onSkuDetailsResponse(BillingResult, List<SkuDetails>)`
   (`:127-146`) filters by `d.getSku().startsWith(DONATION_PREFIX)`, wraps each
   `SkuDetails` in a `Sku` value object, and shows `DonationListFragment`.
4. **Re-hydrate + launch** — `DonationListFragment.onCreateDialog` (`:43-64`)
   re-constructs `new SkuDetails(skus.get(which).getOriginalJson())` from the parcelled
   JSON and calls back `selectedSku(SkuDetails)`; `DonationActivity.selectedSku`
   (`:196-205`) launches `billingClient.launchBillingFlow(this,
   BillingFlowParams.newBuilder().setSkuDetails(details).build())`.
5. **Acknowledge** — `onPurchasesUpdated` (`:152-193`) iterates purchases and calls
   `acknowledgePurchase(Purchase)` (`:237-253`), which builds
   `AcknowledgePurchaseParams` from `getPurchaseToken()` and calls
   `billingClient.acknowledgePurchase(...)`.
6. **Status check** — the static `checkUserDonationStatus(Context, DonationStatusListener)`
   (`:256-291`) opens a second short-lived `BillingClient`, calls the **synchronous**
   `helper.queryPurchases(INAPP)` (`Purchase.PurchasesResult`, `:272`), and reports
   `DONATED / NOT_DONATED / UNKNOWN / NOT_AVAILABLE`. `userHasDonated` (`:293-302`)
   matches `purchase.getSku()` against `Consts.Billing.ALL_SKUS`.

The product IDs are app-owned constants in `Consts.Billing` (verified
`Consts.java:36-50`): `DONATION_PREFIX = "donation."`, `ALL_SKUS = { "donation.1",
"donation.2", "donation.3" }`. These IDs are **unchanged** by this migration — only the
accessors that read them change.

`Sku.java` (read in full) is a `Parcelable`/`Comparable` value object that today wraps
`SkuDetails`: it is constructed from a `SkuDetails` (`:24-26`), re-parsed from
`getOriginalJson()` on un-parcel (`:46-62`), and orders by `priceAmountMicros` then
`title` (`:107-113`). `Sku.Test` (`:126-175`) provides four reserved-product-ID test SKUs
(`android.test.purchased/canceled/refunded/unavailable`) added only in `DEBUG_IAB`
builds.

**Deprecated/removed API surface in 7.x that this design must replace** (each verified
present in the current source this session):

- `SkuDetails`, `SkuDetailsParams`, `SkuDetailsResponseListener`,
  `querySkuDetailsAsync` — removed in 7.x; replaced by `ProductDetails`,
  `QueryProductDetailsParams`, `ProductDetailsResponseListener`,
  `queryProductDetailsAsync`.
- `BillingFlowParams.setSkuDetails(...)` — replaced by
  `setProductDetailsParamsList(List<ProductDetailsParams>)`.
- `SkuType.INAPP` — replaced by `BillingClient.ProductType.INAPP`.
- `Purchase.getSku()` — removed; replaced by `Purchase.getProducts()`
  (`List<String>`). Affects both `userHasDonated` (`:296`) and the filter logic.
- `BillingClient.queryPurchases(String)` returning `Purchase.PurchasesResult`
  (synchronous) — removed; replaced by the asynchronous
  `queryPurchasesAsync(QueryPurchasesParams, PurchasesResponseListener)`. This is the
  **one shape change** in `checkUserDonationStatus` that is more than a rename.
- `ProductDetails` has **no public JSON constructor** equivalent to
  `new SkuDetails(originalJson)`. The `Sku` re-hydration strategy (`Sku.java:46-62`,
  `DonationListFragment.java:52`) must therefore change — see §System Architecture.

## System Architecture

### Decision: stay on Play Billing (donation-only); uplift in place — no alternative store/IAP abstraction

**ADR-010-A — Remain on Google Play Billing for the donation flow.**
**Status:** Accepted (target). **Context:** The only monetary integration in the app is
an optional donation; there is no paid feature, subscription, or entitlement gating.
**Decision:** Keep Google Play Billing as the donation mechanism and perform a contained
version uplift `2.1.0 → 7.x`; do **not** introduce a payment-processor abstraction,
an alternative billing port, or a third-party IAP wrapper. **Consequences:** (+) Minimal
blast radius, no new abstraction to maintain, fully reversible by reverting the
dependency line and four files; (+) consistent with target-state.md Technology Principle
#5 (no new mandatory network surface — billing IPC already exists). (−) The donation
flow remains coupled to the Play Billing IPC SDK — accepted, because the billing client
is a **leaf integration** with no domain reach (see §Integration Design). **Rejected
alternatives:** (a) a `BillingPort`/adapter behind a Hexagonal seam — rejected as
accidental complexity (Golden Hammer) for a single isolated screen with one consumer;
the ports introduced elsewhere in the modernization (`MailTransport`, `BackupScheduler`,
`SecretStore`) exist because those dependencies are dead/risky and domain-coupled, none
of which is true here. (b) Removing in-app donation entirely in favour of an external
"buy me a coffee" link — out of scope for an isolated dependency uplift and a product
decision, not an architectural one.

### Component-level target design (contained to `activity/donation/`)

```
PRESENTATION (activity/donation/)  — TARGET (Billing 7.x)
┌──────────────────────────────────────────────────────────────────────┐
│ DonationActivity                                                       │
│   implements ProductDetailsResponseListener,  (was SkuDetailsResponse) │
│              PurchasesUpdatedListener,                                  │
│              ProductSelectionListener         (was SkuSelectionListener)│
│                                                                        │
│   onCreate ─▶ BillingClient.newBuilder(this)                           │
│                 .setListener(this).enablePendingPurchases().build()    │
│                 .startConnection(...)                                  │
│        OK ─▶ queryAvailableProducts()                                  │
│                 queryProductDetailsAsync(                              │
│                   QueryProductDetailsParams.newBuilder()              │
│                     .setProductList( ALL_SKUS → Product(INAPP) )       │
│                     .build(), this )                                   │
│                                                                        │
│   onProductDetailsResponse(BillingResult, List<ProductDetails>)        │
│        filter productId.startsWith(DONATION_PREFIX)                    │
│        ─▶ showSelectDialog(List<ProductDetails>)                       │
│                                                                        │
│   selectedProduct(ProductDetails) ─▶ launchBillingFlow(               │
│        BillingFlowParams.newBuilder()                                  │
│          .setProductDetailsParamsList([ ProductDetailsParams           │
│              .newBuilder().setProductDetails(pd) .build() ]) .build()) │
│              (one-time INAPP product → no offer token)                 │
│                                                                        │
│   onPurchasesUpdated ─▶ acknowledgePurchase(Purchase)  (UNCHANGED)     │
│                                                                        │
│   checkUserDonationStatus(...)   ← called by MainSettings.onResume()   │
│        queryPurchasesAsync(                                            │
│          QueryPurchasesParams.newBuilder().setProductType(INAPP).build()│
│          , PurchasesResponseListener)        (was sync queryPurchases) │
│        userHasDonated: purchase.getProducts().contains(sku)  (was getSku)│
│        SIGNATURE + DonationStatusListener contract PRESERVED           │
└──────────────────────────────────────────────────────────────────────┘
        │ holds                                  │ presents
        ▼                                        ▼
┌─────────────────────────┐          ┌──────────────────────────────────┐
│ Sku (value object)      │          │ DonationListFragment             │
│  ProductDetails-backed: │          │  interface ProductSelectionListener│
│  productId, title,      │◀─────────│  onCreateDialog: index-based      │
│  formattedPrice,        │ Parcel   │   re-selection → activity resolves │
│  priceAmountMicros,     │ (no JSON │   ProductDetails by productId      │
│  type; Parcelable on    │  re-parse)│  (NO new SkuDetails(json))        │
│  primitive fields only  │          └──────────────────────────────────┘
└─────────────────────────┘
```

#### Per-file change specification

**1. `app/build.gradle:59` — dependency uplift (the gated entry point).**
`implementation 'com.android.billingclient:billing:2.1.0'` →
`implementation 'com.android.billingclient:billing:7.x'` (pin the latest stable `7.x`
patch at implementation time; e.g. `7.1.1`). This is the only build change. Note the
project enforces `-Werror -Xlint:deprecation` (`app/build.gradle:75`, verified) and
`lintOptions.warningsAsErrors true` (`:41`) — **any residual deprecated billing call
will fail the build.** This is a design *constraint*, not a side note: AC-2 ("no
deprecated billing API remains") is enforced by the toolchain, so the migration must be
complete, not partial.

**2. `DonationActivity.java` — listener interfaces and the three async flows.**
- Replace `SkuDetailsResponseListener` with `ProductDetailsResponseListener`; rename
  `onSkuDetailsResponse(BillingResult, List<SkuDetails>)` to
  `onProductDetailsResponse(BillingResult, List<ProductDetails>)`. Filter on
  `productDetails.getProductId().startsWith(DONATION_PREFIX)` (replacing `getSku()`).
- Replace `queryAvailableSkus()` with `queryAvailableProducts()` building
  `QueryProductDetailsParams` from `ALL_SKUS` mapped to
  `QueryProductDetailsParams.Product.newBuilder().setProductId(id)
  .setProductType(ProductType.INAPP).build()`.
- Replace `selectedSku(SkuDetails)` with `selectedProduct(ProductDetails)` and rebuild
  the launch via `setProductDetailsParamsList`. For one-time (INAPP) products there is no
  offer token; construct a single `ProductDetailsParams` per selected product.
- `checkUserDonationStatus` — convert the synchronous `helper.queryPurchases(INAPP)`
  (`Purchase.PurchasesResult`) to `helper.queryPurchasesAsync(QueryPurchasesParams
  .newBuilder().setProductType(ProductType.INAPP).build(), listener)`. **This changes the
  control flow from straight-line to callback**: `endConnection()` and the
  `listener.userDonationState(...)` dispatch must move into the `PurchasesResponseListener`
  callback (and its connection-failure / disconnect branches) rather than the current
  `finally` block. **The method signature and the `DonationStatusListener` callback
  contract are PRESERVED** — `MainSettings.onResume()` is the caller (verified
  `MainSettings.java:69,154-156`) and the result is delivered on the
  `DonationStatusListener` callback, so it must not block the main thread. This is the
  single highest-attention edit in the migration and the most likely regression site —
  its design intent (report `DONATED/NOT_DONATED` on a successful query, `UNKNOWN` on
  query error, `NOT_AVAILABLE` on `BILLING_UNAVAILABLE`) must be preserved exactly.
- `userHasDonated(List<Purchase>)` — replace `purchase.getSku().equals(sku)` with
  `purchase.getProducts().contains(sku)` (`getProducts()` returns the product-ID list in
  7.x).
- `onPurchasesUpdated` / `acknowledgePurchase` — **unchanged**: `BillingResult`,
  `Purchase`, `getPurchaseState()`, `isAcknowledged()`, `getPurchaseToken()`,
  `AcknowledgePurchaseParams`, and `acknowledgePurchase(...)` carry forward across 7.x.
  The response-code `switch` (and the `BillingResponseCode.*` static imports `:33-42`)
  carry forward; verify `SERVICE_TIMEOUT` still exists in 7.x (it does).

**3. `DonationListFragment.java` — eliminate the JSON re-hydration coupling.**
The current `onCreateDialog` re-creates `new SkuDetails(skus.get(which).getOriginalJson())`
(`:52`) and passes a `SkuDetails` to `selectedSku`. `ProductDetails` has **no public
JSON constructor**, so this pattern cannot survive. **Target design:** the fragment no
longer reconstructs a billing object; it reports the user's selection by index (or by
`productId`), and `DonationActivity` resolves the chosen `ProductDetails` from the live
list it already holds from `onProductDetailsResponse`. This removes the
`getOriginalJson()` dependency entirely and is the cleaner design — the fragment becomes
a pure presentation component over the `Sku` value objects. Rename the nested interface
`SkuSelectionListener` → `ProductSelectionListener` and `selectedSku` →
`selectedProduct`.

**4. `Sku.java` — re-base the value object on `ProductDetails` primitives.**
Replace the `Sku(SkuDetails)` constructor with one populated from `ProductDetails`:
`getProductId()`, `getTitle()`, `getDescription()`, and — for one-time products — the
one-time-purchase offer details:
`getOneTimePurchaseOfferDetails().getFormattedPrice()` and
`getOneTimePurchaseOfferDetails().getPriceAmountMicros()`. **Remove the
`originalJson` field and the JSON re-parse in the `Parcel` constructor** (`:46-62`):
parcel the primitive fields (`productId`, `formattedPrice`, `title`, `description`,
`priceAmountMicros`, `type`) directly, which is both simpler and severs the dependency on
the removed JSON constructor. Preserve the `Comparable` ordering (price-micros, then
title — `:107-113`) and the `Sku.Test` reserved-ID fixtures (note: `Sku.Test` SKUs are
built via the **primitive-field constructor** `:36-44`, which has no `SkuDetails`
dependency and therefore needs only field-name alignment, not a billing-API change).

### Traceability

| REQ-MODERNIZATION-010 AC | Design coverage |
|---|---|
| AC-1: billing upgraded to 7.x | `app/build.gradle:59` dependency uplift (§System Architecture, per-file #1) |
| AC-2: all `SkuDetails` replaced with `ProductDetails`; no deprecated API | `DonationActivity`/`Sku`/`DonationListFragment` per-file specs; enforced by `-Werror -Xlint:deprecation` (`build.gradle:75`) + `warningsAsErrors` (`:41`) |
| AC-3: donation flow works end-to-end (query/launch/acknowledge) on a test account | Query=`queryProductDetailsAsync`; launch=`setProductDetailsParamsList`; acknowledge=unchanged `acknowledgePurchase`; status=`queryPurchasesAsync`; validated per §Design Validation using `Sku.Test` reserved IDs + a real test account |
| AC-4: contained to `activity/donation/`; no core-engine files modified | Scope is the 4 donation files + 1 build line; the only inter-component edge (`MainSettings`→`checkUserDonationStatus`) is contract-preserved with **no `MainSettings` edit**; §Integration Design demonstrates leaf isolation |

## Integration Design

### Dependency on DES-MODERNIZATION-001 (SDK/AGP gate)

This design **depends on REQ-MODERNIZATION-001 / `DES-MODERNIZATION-001`** (the SDK/AGP/
Gradle uplift, target-state.md ADR-006). Play Billing `7.x` requires a modern AGP/Gradle
toolchain and `compileSdk` level above the current `29` (verified `app/build.gradle:10`);
it cannot resolve or compile against the present AGP `4.1.3` / Gradle `7.2` baseline.
Per MU-009's dependency edge (`internal: [MU-001]`) and the migration-units dependency
matrix, **MU-009 is blocked by MU-001** and is sequenced in Phase 3. No story
implementing this design may proceed until the SDK/AGP gate has landed. There is **no
other inbound dependency** — this design does not depend on the security spine (MU-003/
004), the scheduler swap (MU-005), eventing (MU-006), Hilt (MU-007), or the mail ACL
(MU-008), and nothing depends on it (`blocks: []`).

> **Note on the DES-001 source-of-truth.** `DES-MODERNIZATION-001` is currently an
> un-authored template stub (verified this session — frontmatter only, no design body).
> The SDK/AGP-7.x-gate dependency claimed here is therefore grounded in
> `REQ-MODERNIZATION-001` and `target-state.md` ADR-006 / Supporting Technologies
> (AGP 8.x + Gradle 8.x, staged), not in the DES-001 body. When DES-001 is authored, this
> dependency edge should be re-confirmed against it.

### Play billing-policy / ProductDetails enforcement

The `ProductDetails` API is not merely a rename — it is the API surface Google Play
billing-policy enforcement now requires (`6.x+`). The design enforces this by:
(a) querying exclusively through `queryProductDetailsAsync`/`QueryProductDetailsParams`,
(b) launching exclusively through `setProductDetailsParamsList` (the only launch path
accepted by 7.x), and (c) relying on the build's `-Werror -Xlint:deprecation` +
`warningsAsErrors` as a fitness function so that any lingering `SkuDetails`-era call is a
**hard build failure**, not a runtime surprise. This makes "no deprecated billing API
remains" (AC-2) machine-checked at compile time.

### Isolation from substrate work (leaf integration)

The integration is a **leaf**: the donation subsystem is a near-terminal node in the
component graph with no domain reach and exactly one inbound UI edge.

- **No core-engine coupling.** The donation files import only the billing SDK, AndroidX
  UI types, `R`/`BuildConfig`, `ThemeActivity`, `BundleBuilder`, and the app-owned
  `Consts.Billing` product-ID constants (`ALL_SKUS`, `DONATION_PREFIX`, static-imported
  at `DonationActivity.java:46-47`, verified). They touch none of the Preserved Core
  (MU-000): not `service/state/*`, not `mail/*`, not the scheduler, not `Preferences`.
- **Single inbound UI edge, contract-preserved (verified via grep).**
  `MainSettings.java:69,154-156` is the only external caller — it invokes the static
  `DonationActivity.checkUserDonationStatus(Context, DonationStatusListener)` from
  `onResume()` to hide/show the "Donate" preference (SCR-DON-001 BR-DON-006). Because the
  method signature and the `DonationStatusListener` contract are preserved (only the
  internal `queryPurchases` → `queryPurchasesAsync` mechanism changes), **`MainSettings.java`
  is not edited** and the scope stays inside `activity/donation/`. The one design
  obligation this edge imposes: the async result must be delivered via the existing
  callback without blocking `MainSettings.onResume()` (SCR-DON-001 §System Integration).
- **No shared-file contention.** Per the migration-units Shared-File Allocation table,
  none of the four donation files appears in any other unit's touch set, and
  `app/build.gradle:59` is the **only** billing dependency line — each unit edits its own
  dependency line, so MU-009 edits `:59` alone with no merge conflict against MU-001's
  SDK lines, MU-005's jobdispatcher line (`:60`), MU-006's otto line (`:57`), or
  MU-008's k-9 line (`:58`).
- **Failure containment.** A regression in the donation flow cannot cascade past the
  donation screen; the `checkUserDonationStatus` entry point already degrades to
  `UNKNOWN`/`NOT_AVAILABLE` on any billing error, so the rest of the app (including
  `MainSettings`) is unaffected by billing unavailability.

Because the binding to Play Billing is leaf-level and single-consumer, **no port/adapter
abstraction is introduced** (see ADR-010-A) — adding one would be unjustified indirection
for a contained, reversible, low-risk uplift.

## Design Validation

Validation criteria, each mapped to a REQ-MODERNIZATION-010 acceptance criterion and to
the verification method:

1. **Billing on 7.x (AC-1).** `app/build.gradle` declares
   `com.android.billingclient:billing:7.x` and the project resolves and assembles a
   release with the new coordinate on the post-DES-001 AGP/Gradle/SDK baseline.
   *Method:* clean build + dependency-tree inspection.
2. **No `SkuDetails` / no deprecated billing API (AC-2).** A repository-wide search for
   `SkuDetails`, `SkuDetailsParams`, `SkuDetailsResponseListener`, `querySkuDetailsAsync`,
   `setSkuDetails`, `SkuType`, `Purchase.getSku(`, `queryPurchases(` (synchronous), and
   `new SkuDetails(` returns **zero** matches in `app/src`. *Method:* grep gate in
   review + the `-Werror -Xlint:deprecation` / `warningsAsErrors` build (`build.gradle:75`,
   `:41`) failing on any residual deprecated call.
3. **Donation flow end-to-end on a test account (AC-3).** Against a Play test account
   (license-tester), with `Sku.Test` reserved-ID fixtures available in `DEBUG_IAB`
   builds: (a) `queryProductDetailsAsync` returns the donation products and they render
   in `DonationListFragment`; (b) selecting a product launches the Play purchase sheet
   via `setProductDetailsParamsList`; (c) a completed purchase reaches
   `onPurchasesUpdated` and is acknowledged via the unchanged `acknowledgePurchase`
   (acknowledged within Google's 3-day window so the purchase is not auto-refunded);
   (d) `checkUserDonationStatus` reports `DONATED` afterward via the new
   `queryPurchasesAsync` callback, and `NOT_DONATED`/`UNKNOWN`/`NOT_AVAILABLE` in the
   corresponding negative paths; (e) `MainSettings` correctly hides the "Donate"
   preference when `DONATED`/`NOT_AVAILABLE` is reported (BR-DON-006 / BR-ADV-008) and
   `onResume()` is not blocked. *Method:* manual test-account run on a `targetSdk ≥ 34`
   device/emulator + the migrated `DonationActivityTest`/`SkuTest` (which today assert the
   `onPurchasesUpdated` toast outcomes `:30-53` and the `Sku` ordering/test-ID invariants
   `:16-35`; these must be updated to the 7.x types and continue to pass — they are the
   characterization gate for the migration per MU-009 `test_changes`).
4. **No `SkuDetails` left in the value object / fragment (AC-2/AC-3).** `Sku.java` carries
   no `SkuDetails` import, no `originalJson` field, and no JSON re-parse; the `Parcel`
   round-trip reconstructs from primitive fields and the `SkuTest` comparability tests
   pass. `DonationListFragment` carries no `SkuDetails` import and no
   `new SkuDetails(...)`. *Method:* import-level grep + `SkuTest` green.
5. **Change contained to `activity/donation/` (AC-4).** The diff for the implementing
   story touches only the four files under
   `app/src/main/java/.../activity/donation/` (+ the two test files) and
   `app/build.gradle:59`. No file under `service/`, `mail/`, `auth/`, `preferences/`,
   `service/state/`, or `activity/fragments/` (notably **not** `MainSettings.java`)
   appears in the diff. *Method:* diff-scope review against the migration-units
   Shared-File Allocation table.

**Negative / regression checks (verified-source-driven):**
- The `checkUserDonationStatus` async conversion preserves the exact state mapping (the
  most likely regression — see §System Architecture) **and** its public signature/callback
  contract (so `MainSettings` need not change and `onResume()` is not blocked);
  `DonationActivityTest` should be extended to cover the async-callback paths, not only
  `onPurchasesUpdated`.
- The `DONATION_PREFIX` filter and `ALL_SKUS` membership (`Consts.Billing`) are unchanged
  in meaning — only the accessor changes (`getProductId()` / `getProducts()`); product
  IDs themselves (`donation.1/2/3`) are not altered.

## Integration Contracts

> **None.** This design introduces **no cross-component boundary** and therefore requires
> **no CNTR-\* artifact**. The Google Play Billing client is a **leaf integration**: the
> donation subsystem is a near-terminal presentation node that consumes the Play Billing
> IPC SDK directly. Its one inbound UI edge — `MainSettings` →
> `DonationActivity.checkUserDonationStatus(...)` — is an **intra-module static call whose
> signature and `DonationStatusListener` contract are preserved unchanged** by this
> migration; it is not a producer/consumer boundary being newly introduced or altered,
> and it is governed by the existing in-process method contract, not a CNTR-\*. No
> app-owned interface crosses a producer/consumer boundary here — there is no port, no
> event, no schema, and no new service contract. The only external surface is the Play
> Billing IPC itself, whose contract is owned by Google and pinned by the SDK version
> (`7.x`), not by this project. Per the Contract-Gate model, a leaf integration whose
> single intra-module consumer contract is held invariant does not constitute a
> cross-boundary that a CNTR-\* artifact would govern.

| Boundary | Producer | Consumer(s) | Contract Type | CNTR Artifact | Status |
|----------|----------|-------------|---------------|---------------|--------|
| _(none — leaf integration; donation subsystem ↔ Play Billing IPC SDK, owned externally by Google, pinned by `billing:7.x`)_ | — | — | — | — | n/a |
| _(unchanged intra-module call: `MainSettings` → `DonationActivity.checkUserDonationStatus`; signature + `DonationStatusListener` contract preserved — not a newly introduced boundary)_ | DonationActivity (static) | MainSettings | in-process method (preserved) | — | n/a |

### Contracts Needed (pre-sprint gate)

- None. No boundary in this design lacks an approved CNTR-\*, because no
  cross-component boundary is introduced or altered. A story implementing
  REQ-MODERNIZATION-010 / `DES-MODERNIZATION-010` may be sprint-planned without a
  `/amp:create-contracts` run, subject only to the DES-MODERNIZATION-001 (SDK/AGP gate)
  dependency.

## Trade-offs

- **Uplift in place vs. introduce a billing port.** Chosen: in-place uplift (ADR-010-A).
  Trade-off: keeps the donation flow coupled to the Play Billing SDK, but avoids
  unjustified indirection for a single-screen, single-consumer leaf. A port would buy
  testability/swappability the donation feature does not need and is not asked for by any
  driver.
- **Eliminate JSON re-hydration vs. preserve the existing `getOriginalJson()` round-trip.**
  Chosen: eliminate it (resolve `ProductDetails` by index/`productId` in the activity).
  Forced by the absence of a public `ProductDetails` JSON constructor, but also the
  cleaner design — it removes a parse-on-un-parcel that could throw `JSONException` and
  makes `Sku` a pure primitive value object.
- **Preserve `checkUserDonationStatus` signature vs. redesign the status check.** Chosen:
  preserve the public signature + `DonationStatusListener` contract so `MainSettings` is
  untouched and scope stays in `activity/donation/`. Trade-off: the internal async
  conversion is slightly more intricate (callback vs. `finally`), but it keeps the change
  contained (AC-4) and avoids a second-file edit.
- **Complete migration vs. incremental coexistence.** Chosen: complete, single-pass
  migration. The `-Werror -Xlint:deprecation` + `warningsAsErrors` build makes partial
  migration impossible to ship anyway, so there is no value in a coexistence path.

## Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| `checkUserDonationStatus` async-conversion regresses the `DONATED/NOT_DONATED/UNKNOWN/NOT_AVAILABLE` mapping (callback vs. straight-line `finally`) | Medium | Medium (wrong donation status shown in `MainSettings`) | Preserve the exact state mapping; move `endConnection()` + dispatch into the `PurchasesResponseListener` callback and its error/disconnect branches; extend `DonationActivityTest` to cover async paths |
| Async conversion blocks or leaks across `MainSettings.onResume()` | Low | Medium | Deliver the result only via the existing `DonationStatusListener` callback off the main thread; preserve the public signature so `MainSettings` is unmodified (SCR-DON-001 §System Integration) |
| Residual deprecated call slips through and is masked at a lower SDK | Low | Low | `-Werror -Xlint:deprecation` (`build.gradle:75`) + `warningsAsErrors` (`:41`) make any residual `SkuDetails`-era call a hard build failure; grep gate in review |
| `Sku.Test` reserved-ID fixtures behave differently under 7.x test flow | Low | Low | Validate against a Play license-tester account; `android.test.*` reserved IDs remain supported; keep fixtures `DEBUG_IAB`-only |
| Purchase not acknowledged within Google's 3-day window during testing | Low | Medium (auto-refund) | `acknowledgePurchase` path is unchanged and invoked in `onPurchasesUpdated`; validate acknowledgement in the end-to-end test |
| Blocked indefinitely if DES-MODERNIZATION-001 (SDK/AGP gate) slips | Medium | Low (elective M4, deferrable) | MU-009 is explicitly Phase 3 / deferrable; the donation flow continues on `2.1.0` until Google retires it — no other work depends on this |

## Notes

- This is an isolated **elective (M4)** dependency uplift; depth is proportionate to its
  contained, low-risk, leaf scope (per expert-exceeding-depth, proportionate).
- All claims about current behaviour were verified by **full read** of
  `DonationActivity.java`, `DonationListFragment.java`, `Sku.java`,
  `DonationActivityTest.java`, `SkuTest.java`, `Consts.java`, and `app/build.gradle`
  (billing `2.1.0` at `:59`) **this session**; the single inbound caller
  (`MainSettings.checkUserDonationStatus`) and product-ID constants were confirmed via
  grep this session. The dependency on DES-001 and the leaf-scope claims are grounded in
  `target-state.md` (ADR-006, Presentation Layer) and `migration-units.md` (MU-009,
  Shared-File Allocation, dependency matrix), both read in full this session.
- `7.x` patch version to be pinned at implementation time (latest stable, e.g. `7.1.1`).

## Artifacts Consulted

| Artifact | Path | Purpose |
|----------|------|---------|
| REQ-MODERNIZATION-010 | sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-010-upgrade-play-billing-to-v7.md | Source requirement + acceptance criteria (traced) |
| Migration Units (MU-009) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/migration-units.md | Unit scope, dependency edges, shared-file allocation, Phase 3 sequencing |
| Target State (ADR-006, Presentation Layer, Billing row) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/target-state.md | SDK/AGP gate dependency; Play Billing 7.x target; leaf-isolation rationale |
| SCR-DON-001 (donation UI spec) | sdlc/analysis/20260529-modernization/outputs/sms-backup-plus/modernization/requirements/ui-requirements/SCR-DON-001-donation-activity.md | BR-DON-006 status check, MainSettings integration, queryPurchasesAsync note (grep-consulted) |
| DonationActivity.java | app/src/main/java/com/zegoggles/smssync/activity/donation/DonationActivity.java | Current SkuDetails connect/query/launch/acknowledge/status flow (full read, verified) |
| DonationListFragment.java | app/src/main/java/com/zegoggles/smssync/activity/donation/DonationListFragment.java | Current SkuDetails JSON re-hydration + selection listener (full read, verified) |
| Sku.java | app/src/main/java/com/zegoggles/smssync/activity/donation/Sku.java | Current SkuDetails-backed Parcelable value object + Test fixtures (full read, verified) |
| DonationActivityTest.java | app/src/test/java/com/zegoggles/smssync/activity/donation/DonationActivityTest.java | Characterization gate — onPurchasesUpdated toast outcomes (full read, verified) |
| SkuTest.java | app/src/test/java/com/zegoggles/smssync/activity/donation/SkuTest.java | Characterization gate — Sku comparability + test-ID invariants (full read, verified) |
| Consts.java | app/src/main/java/com/zegoggles/smssync/Consts.java | Verified Billing constants: DONATION_PREFIX, ALL_SKUS = donation.1/2/3 (full read) |
| MainSettings.java (grep) | app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java | Verified sole external caller of checkUserDonationStatus from onResume (:69,154-156) |
| app/build.gradle | app/build.gradle | Verified billing 2.1.0 at :59; -Werror -Xlint:deprecation :75; warningsAsErrors :41; compileSdk 29 :10 |
| DES-MODERNIZATION-001 | sdlc/artifacts/design/modernization/DES-MODERNIZATION-001-restore-play-store-eligibility-design.md | SDK/AGP gate dependency — verified currently a template stub; dependency grounded via REQ-MODERNIZATION-001 + target-state.md ADR-006 |
