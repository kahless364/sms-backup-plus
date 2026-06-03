---
type: story
status: done
sprint: '000001'
artifact_type: user-story
priority: low
complexity: low
parallel_eligible: false
iteration: 1
requirements:
  - REQ-MODERNIZATION-010
design_docs:
  - DES-MODERNIZATION-010
integration_contracts: []
dependencies:
  - U-003
change_records: []
platforms: []
tags: []
gate_additions: []
id: U-028
title: 'Upgrade Play Billing 2.1.0 → 7.x: replace SkuDetails pipeline with ProductDetails/queryProductDetailsAsync across the donation subsystem'
pipeline: ''
domain: modernization
requirement_source: authored
updated_at: '2026-06-03T03:13:07.290Z'
resolution: done
---

# U-028: Upgrade Play Billing 2.1.0 → 7.x — Replace SkuDetails Pipeline with ProductDetails/queryProductDetailsAsync Across the Donation Subsystem

## Story

As a developer maintaining the SMS Backup+ donation subsystem,
I want to upgrade `com.android.billingclient:billing` from `2.1.0` to `7.x` and replace every `SkuDetails`-era API call with the `ProductDetails`/`queryProductDetailsAsync` surface across `DonationActivity.java`, `DonationListFragment.java`, and `Sku.java`,
so that the donation flow continues to function under current Google Play billing-policy enforcement (which mandates the `ProductDetails` API introduced in `6.x+`) and the codebase no longer carries a deprecated billing surface that the build's `-Werror -Xlint:deprecation` / `warningsAsErrors` toolchain will reject.

## Acceptance Criteria

- [ ] **AC-1 — `billing` dependency is upgraded to `7.x` in `app/build.gradle:59`**

  Given the file `app/build.gradle` before this story declares `implementation 'com.android.billingclient:billing:2.1.0'` at line 59,
  when the story is implemented,
  then the declaration at that line reads `implementation 'com.android.billingclient:billing:7.x'` (with the patch version pinned to the latest stable `7.x` release at implementation time, e.g. `7.1.1`, recorded in an inline comment);
  `./gradlew :app:dependencies --configuration debugRuntimeClasspath` resolves `com.android.billingclient:billing` at the declared `7.x` version with no competing version constraints.

- [ ] **AC-2 — No `SkuDetails`-era deprecated billing API remains anywhere under `app/src/`**

  Given the upgraded `billing:7.x` dependency and the project's `-Werror -Xlint:deprecation` flag (`app/build.gradle:75`) and `lintOptions.warningsAsErrors true` (`:41`),
  when `./gradlew :app:assembleDebug` completes,
  then the build exits with code 0 and zero deprecation warnings — meaning no instance of any of the following identifiers survives in `app/src/`: `SkuDetails`, `SkuDetailsParams`, `SkuDetailsResponseListener`, `querySkuDetailsAsync`, `setSkuDetails`, `SkuType`, `getSku()` (on `Purchase`), `queryPurchases(String)` (synchronous overload), or `new SkuDetails(`;
  a repository-wide search (`grep -r "SkuDetails\|querySkuDetailsAsync\|setSkuDetails\|SkuType\|getSku()\|queryPurchases(" app/src/`) returns zero matches.

- [ ] **AC-3 — Product-query path: `queryProductDetailsAsync` returns the three donation products and renders them in `DonationListFragment`**

  Given a device or emulator running `targetSdk ≥ 34`, signed in with a Play license-tester account, with a `DEBUG_IAB` build that exposes the `Sku.Test` reserved-ID fixtures,
  when `DonationActivity` starts and the `BillingClient` connection reaches `OK`,
  then `queryAvailableProducts()` calls `billingClient.queryProductDetailsAsync(QueryProductDetailsParams)` built from `Consts.Billing.ALL_SKUS` mapped to `QueryProductDetailsParams.Product` entries with `ProductType.INAPP`;
  `onProductDetailsResponse(BillingResult, List<ProductDetails>)` filters results by `productDetails.getProductId().startsWith(DONATION_PREFIX)` and passes the filtered `List<ProductDetails>` to construct `Sku` value objects using `Sku(ProductDetails)` (the new constructor);
  `DonationListFragment` displays the three donation-product titles and formatted prices without error.

- [ ] **AC-4 — Purchase-launch path: selecting a product launches the Play purchase sheet via `setProductDetailsParamsList`**

  Given a `DonationListFragment` showing donation products (AC-3 satisfied),
  when the user selects one product,
  then the fragment reports the selection by index (or by `productId`) to `DonationActivity` via the renamed `ProductSelectionListener.selectedProduct(ProductDetails)` callback (there is no `new SkuDetails(originalJson)` reconstruction anywhere in `DonationListFragment`);
  `DonationActivity.selectedProduct(ProductDetails)` launches the billing flow via `billingClient.launchBillingFlow(this, BillingFlowParams.newBuilder().setProductDetailsParamsList(List.of(ProductDetailsParams.newBuilder().setProductDetails(pd).build())).build())` with no offer token (one-time INAPP product);
  the Play purchase sheet appears.

- [ ] **AC-5 — Acknowledgement path: completed purchase reaches `onPurchasesUpdated` and is acknowledged**

  Given a completed test-account purchase,
  when `onPurchasesUpdated(BillingResult, List<Purchase>)` is invoked,
  then the method iterates the purchase list, checks `getPurchaseState() == PURCHASED` and `!isAcknowledged()`, and calls `acknowledgePurchase(Purchase)`;
  `acknowledgePurchase` builds `AcknowledgePurchaseParams` from `purchase.getPurchaseToken()` and calls `billingClient.acknowledgePurchase(params, callback)`;
  no `SkuDetails`-era API is invoked in these paths;
  the toast/log output matches the pre-story behavior verified by `DonationActivityTest` (`onPurchasesUpdated` happy-path and error-path cases all pass).

- [ ] **AC-6 — Status-check path: `checkUserDonationStatus` delivers `DONATED`, `NOT_DONATED`, `UNKNOWN`, and `NOT_AVAILABLE` via `DonationStatusListener` without blocking the caller thread**

  Given `MainSettings.onResume()` calling the static `DonationActivity.checkUserDonationStatus(Context, DonationStatusListener)` with its pre-story signature (the signature is unchanged),
  when the method executes,
  then internally it opens a short-lived `BillingClient`, calls `helper.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build(), purchasesResponseListener)` (replacing the removed synchronous `helper.queryPurchases(INAPP)`);
  within the `PurchasesResponseListener` callback: on `OK` with a non-empty purchase list whose `getProducts()` contains a member of `Consts.Billing.ALL_SKUS`, `listener.userDonationState(DonationStatus.DONATED)` is dispatched; on `OK` with no matching purchase, `NOT_DONATED`; on any `BillingResponseCode` query error, `UNKNOWN`; on a `BILLING_UNAVAILABLE` connection failure, `NOT_AVAILABLE`; `helper.endConnection()` is called inside the callback (not in a `finally` block on the calling thread);
  `MainSettings.onResume()` returns immediately without waiting for the callback — it is not blocked;
  `MainSettings.java` is not modified.

- [ ] **AC-7 — `Sku` value object is re-based on `ProductDetails` primitives; no `originalJson` field or JSON re-parse remains**

  Given the rewritten `Sku.java`,
  when a developer reads the file,
  then:
  (a) the constructor `Sku(SkuDetails)` no longer exists and is replaced by `Sku(ProductDetails)` that populates fields from `getProductId()`, `getTitle()`, `getDescription()`, `getOneTimePurchaseOfferDetails().getFormattedPrice()`, and `getOneTimePurchaseOfferDetails().getPriceAmountMicros()`;
  (b) the `originalJson` field (used in the old `Parcel` constructor to re-create `new SkuDetails(originalJson)`) no longer exists — `writeToParcel`/`CREATOR` parcels and un-parcels the primitive fields (`productId`, `title`, `description`, `formattedPrice`, `priceAmountMicros`, `type`) directly;
  (c) `import com.android.billingclient.api.SkuDetails` does not appear in the file;
  (d) the `Comparable` ordering by `priceAmountMicros` ascending, then `title` ascending, is preserved unchanged;
  (e) the `Sku.Test` inner class reserved-ID fixtures (`android.test.purchased`, `android.test.canceled`, `android.test.refunded`, `android.test.unavailable`) are preserved, now built via the primitive-field constructor — their product-ID strings, prices, and titles are unchanged.

- [ ] **AC-8 — Updated tests pass: `DonationActivityTest` covers `onPurchasesUpdated` outcomes and the `queryPurchasesAsync` async-callback paths; `SkuTest` covers `Sku` ordering and `Test` fixture invariants**

  Given the updated `DonationActivityTest.java` and `SkuTest.java`,
  when `./gradlew :app:testDebugUnitTest` is executed from the repo root,
  then the command exits with code 0 with zero test failures;
  `DonationActivityTest` asserts the `onPurchasesUpdated` toast/log outcomes (happy-path `PURCHASED + !acknowledged → acknowledge call`, error-code paths) using `billing:7.x` types, and includes at least one test that exercises the `queryPurchasesAsync`-callback state mapping in `checkUserDonationStatus` (simulating `OK`/`DONATED`, `OK`/`NOT_DONATED`, query error → `UNKNOWN`, connection failure → `NOT_AVAILABLE`) via a test double for `BillingClient`;
  `SkuTest` asserts that two `Sku` instances created from `ProductDetails`-backed data sort in ascending `priceAmountMicros` order and that the four `Sku.Test` reserved-ID fixtures each return the expected `productId` string.

- [ ] **AC-9 — Change is contained to the six permitted files; no file outside `activity/donation/` (and `app/build.gradle:59`) is modified**

  Given the completed implementation,
  when `git diff --name-only` is inspected,
  then the only files changed are:
  `app/build.gradle` (line 59 only),
  `app/src/main/java/com/zegoggles/smssync/activity/donation/DonationActivity.java`,
  `app/src/main/java/com/zegoggles/smssync/activity/donation/DonationListFragment.java`,
  `app/src/main/java/com/zegoggles/smssync/activity/donation/Sku.java`,
  `app/src/test/java/com/zegoggles/smssync/activity/donation/DonationActivityTest.java`,
  `app/src/test/java/com/zegoggles/smssync/activity/donation/SkuTest.java`;
  no file under `service/`, `mail/`, `auth/`, `preferences/`, `service/state/`, or `activity/fragments/` (and specifically not `MainSettings.java`) appears in the diff.

## Affected Code

| File | Current Behavior | Change |
|------|-----------------|--------|
| `app/build.gradle` | Declares `billing:2.1.0` at line 59 | Upgrade to `billing:7.x`; add inline comment with pinned patch version |
| `app/src/main/java/com/zegoggles/smssync/activity/donation/DonationActivity.java` | Implements `SkuDetailsResponseListener`; calls `querySkuDetailsAsync`; `selectedSku(SkuDetails)` launches via `setSkuDetails`; `checkUserDonationStatus` calls synchronous `queryPurchases(INAPP)`; `userHasDonated` uses `purchase.getSku()` | Replace with `ProductDetailsResponseListener`; `queryAvailableProducts()` calls `queryProductDetailsAsync`; `selectedProduct(ProductDetails)` launches via `setProductDetailsParamsList`; `checkUserDonationStatus` uses async `queryPurchasesAsync` with result dispatched in `PurchasesResponseListener` callback; `userHasDonated` uses `purchase.getProducts().contains(sku)` |
| `app/src/main/java/com/zegoggles/smssync/activity/donation/DonationListFragment.java` | `onCreateDialog` re-creates `new SkuDetails(skus.get(which).getOriginalJson())` and calls `selectedSku(SkuDetails)` via `SkuSelectionListener` | Remove JSON re-hydration; report selection by index/productId; rename interface to `ProductSelectionListener` and callback to `selectedProduct(ProductDetails)` |
| `app/src/main/java/com/zegoggles/smssync/activity/donation/Sku.java` | `Sku(SkuDetails)` constructor; `originalJson` field; Parcel constructor re-parses `new SkuDetails(originalJson)`; orders by `priceAmountMicros`/`title` | Replace constructor with `Sku(ProductDetails)` reading primitive fields from `getOneTimePurchaseOfferDetails()`; remove `originalJson` field and JSON re-parse; parcel primitive fields directly; preserve `Comparable` ordering and `Sku.Test` fixtures |
| `app/src/test/java/com/zegoggles/smssync/activity/donation/DonationActivityTest.java` | Asserts `onPurchasesUpdated` toast outcomes using `billing:2.1.0` types | Update to `billing:7.x` types; add async-callback tests for `checkUserDonationStatus` state mapping |
| `app/src/test/java/com/zegoggles/smssync/activity/donation/SkuTest.java` | Asserts `Sku` ordering and `Sku.Test` fixture invariants using `SkuDetails`-backed construction | Update to `ProductDetails`-backed construction via the new `Sku(ProductDetails)` constructor; preserve ordering and fixture-ID assertions |

## Existing Behavior to Preserve

- `DonationActivity.checkUserDonationStatus(Context, DonationStatusListener)` — the public static method signature must remain byte-for-byte identical. `MainSettings.onResume()` (`:69, 154-156`) calls this method and the call site must compile and function without any change to `MainSettings.java`.
- `DonationStatusListener` callback contract — the four status values `DONATED`, `NOT_DONATED`, `UNKNOWN`, and `NOT_AVAILABLE` and the conditions that trigger each must be preserved exactly. `MainSettings` hides the "Donate" preference on `DONATED` and `NOT_AVAILABLE` (BR-DON-006); an incorrect mapping is a silent regression that is hard to catch in automated tests.
- `Consts.Billing.ALL_SKUS` and `DONATION_PREFIX` — the product-ID strings `donation.1`, `donation.2`, `donation.3` and the prefix `donation.` are unchanged. Only the accessors that read them change (`getProductId()` replacing `getSku()`).
- `onPurchasesUpdated` / `acknowledgePurchase` outer behavior — the acknowledgement of `PURCHASED` and un-acknowledged purchases must continue; these methods require only type migration, not logic changes.
- `Sku` `Comparable` ordering — sort ascending by `priceAmountMicros` then by `title`. `DonationListFragment` relies on this order for display.
- `Sku.Test` reserved-ID fixtures — the four `android.test.*` product IDs used in `DEBUG_IAB` builds must survive with their existing product-ID strings, titles, and price values.
- Full pre-existing unit test suite — all tests in `./gradlew :app:testDebugUnitTest` that pass before this story must still pass after it.

## Verification Steps

1. **AC-1 (dependency upgrade):** Open `app/build.gradle`. Confirm line 59 reads `billing:7.x` (with the pinned patch version). Run `./gradlew :app:dependencies --configuration debugRuntimeClasspath` and confirm `com.android.billingclient:billing` resolves at the declared version.

2. **AC-2 (no deprecated API):** Run `./gradlew :app:assembleDebug` and confirm exit code 0 with zero deprecation warnings (the build is `-Werror`, so any residual deprecated call will produce a non-zero exit). Run `grep -r "SkuDetails\|querySkuDetailsAsync\|setSkuDetails\|SkuType\|\.getSku()\|queryPurchases(" app/src/` and confirm zero matches.

3. **AC-3 (query path):** On a `DEBUG_IAB` device/emulator with a Play license-tester account, launch `DonationActivity`. Observe logcat for `queryProductDetailsAsync` invocation. Confirm `DonationListFragment` displays three entries matching the `donation.1/2/3` product titles and prices returned from Play. Confirm no `SkuDetails` or `SkuDetailsParams` import appears in `DonationActivity.java`.

4. **AC-4 (launch path):** Continuing from step 3, tap one donation product. Confirm the Play purchase sheet appears. Confirm `DonationListFragment.java` contains no `new SkuDetails(` call and no `originalJson` reference. Confirm `DonationActivity.java` calls `setProductDetailsParamsList` in `launchBillingFlow`.

5. **AC-5 (acknowledgement path):** Complete a test purchase on the license-tester account. Observe that `onPurchasesUpdated` fires and `acknowledgePurchase` is called (logcat confirmation). Run `./gradlew :app:testDebugUnitTest --tests "*DonationActivityTest*"` and confirm all `onPurchasesUpdated` test cases pass.

6. **AC-6 (status-check path — highest-attention edit):** Run `./gradlew :app:testDebugUnitTest --tests "*DonationActivityTest*"` and confirm the four `checkUserDonationStatus` async-callback tests pass (`DONATED`, `NOT_DONATED`, `UNKNOWN`, `NOT_AVAILABLE`). In a manual session after a completed test purchase, relaunch `MainSettings` and confirm the "Donate" preference is hidden. Confirm `MainSettings.java` is byte-for-byte identical to its pre-story version (`git diff app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java` returns empty). Confirm `DonationActivity.checkUserDonationStatus` contains no synchronous `queryPurchases` call and no `Purchase.PurchasesResult` type reference.

7. **AC-7 (Sku value object):** Open `Sku.java`. Confirm `import com.android.billingclient.api.SkuDetails` is absent. Confirm `originalJson` field is absent. Confirm `writeToParcel` writes `productId`, `title`, `description`, `formattedPrice`, `priceAmountMicros`, `type` as primitives. Confirm `CREATOR.createFromParcel` reads those same fields without any `new SkuDetails(...)` call. Confirm the `Comparable compareTo` body is unchanged in logic (compare by `priceAmountMicros`, then `title`). Confirm the four `Sku.Test` entries (`android.test.*`) are present with unmodified product-ID strings.

8. **AC-8 (tests pass):** Run `./gradlew :app:testDebugUnitTest` from the repo root. Confirm exit code 0. Confirm zero test failures across `DonationActivityTest` and `SkuTest`. Open `DonationActivityTest.java` and confirm at least one test method directly exercises the `queryPurchasesAsync` callback path in `checkUserDonationStatus` (search for method names containing `checkUserDonationStatus` or `queryPurchases`).

9. **AC-9 (scope containment):** Run `git diff --name-only`. Confirm the output contains exactly the six permitted paths and nothing else. Confirm `app/src/main/java/com/zegoggles/smssync/activity/fragments/MainSettings.java` does not appear in the diff.

## Platform Work Breakdown

| Platform | Scope | Agent |
|----------|-------|-------|
| Android (Java) | Upgrade `billing` dependency; rewrite `DonationActivity`, `DonationListFragment`, `Sku` for `ProductDetails` API; update `DonationActivityTest`, `SkuTest` | Developer |

## Technical Notes

**`checkUserDonationStatus` async conversion is the highest-attention edit in this story.**
The current implementation (`:256-291`) is straight-line: it opens a `BillingClient`, calls the synchronous `helper.queryPurchases(INAPP)` (which returns `Purchase.PurchasesResult`), evaluates the result, dispatches `listener.userDonationState(...)`, and calls `helper.endConnection()` in a `finally` block — all on the calling thread. The `queryPurchases` synchronous overload is removed in `billing:7.x`; it must be replaced by `helper.queryPurchasesAsync(QueryPurchasesParams, PurchasesResponseListener)`.

The control-flow transformation is non-trivial: the `PurchasesResponseListener.onQueryPurchasesResponse(BillingResult, List<Purchase>)` callback fires asynchronously on a billing thread. All of the following logic must move **into** the callback body (and into the connection-failure branch of the `BillingClientStateListener`):
- The `userHasDonated(List<Purchase>)` evaluation (now using `purchase.getProducts().contains(sku)` instead of `purchase.getSku().equals(sku)`)
- The `listener.userDonationState(DONATED / NOT_DONATED / UNKNOWN)` dispatch
- The `helper.endConnection()` call

The `NOT_AVAILABLE` path (currently triggered by `BILLING_UNAVAILABLE` in the `onBillingSetupFinished` handler) is unchanged in position. The `UNKNOWN` state should be dispatched from `onQueryPurchasesResponse` when `BillingResult.getResponseCode() != OK`.

The public method signature `checkUserDonationStatus(Context context, DonationStatusListener listener)` and the `DonationStatusListener` callback contract are PRESERVED. `MainSettings.onResume()` is the verified sole caller; it must not block. Do not add synchronization, `CountDownLatch`, or any blocking wait inside `checkUserDonationStatus`. The result is delivered exclusively via the `DonationStatusListener.userDonationState(DonationStatus)` callback.

`DonationActivityTest` must be extended with test cases that verify each of the four async-callback outcomes by constructing a test double for `BillingClient` that calls back synchronously in the test — the existing `onPurchasesUpdated` tests are insufficient alone to characterize this path.

**JSON re-hydration removal in `DonationListFragment`.**
`DonationListFragment.onCreateDialog` currently calls `new SkuDetails(skus.get(which).getOriginalJson())` (`:52`) to reconstruct a `SkuDetails` for the `selectedSku` callback. `ProductDetails` has no public JSON constructor, so this pattern cannot be ported. The target design: the fragment passes the selected index (or `productId`) back to `DonationActivity`, which resolves the live `ProductDetails` from the list it holds in memory from `onProductDetailsResponse`. This makes `DonationListFragment` a pure presentation component and eliminates the `JSONException`-prone Parcel round-trip. Rename the nested listener interface from `SkuSelectionListener` to `ProductSelectionListener` and rename the callback method from `selectedSku(SkuDetails)` to `selectedProduct(ProductDetails)`.

**`Sku.java` primitive-field Parcel.**
Today the Parcel constructor calls `new SkuDetails(in.readString())` to re-hydrate from JSON. After this story it reads the primitive fields directly from the Parcel in the same order `writeToParcel` writes them: `productId`, `title`, `description`, `formattedPrice`, `priceAmountMicros` (`long`), `type`. The `Sku.Test` reserved-ID fixtures use the existing primitive-field constructor (`:36-44`), which already populates these fields without a `SkuDetails` dependency — they need only field-name alignment if any field is renamed, not a billing-API change.

For `getOneTimePurchaseOfferDetails()`: this method can return `null` if the `ProductDetails` is not a one-time INAPP product. Since `Consts.Billing.ALL_SKUS` products are all `INAPP` one-time purchases this should never be `null` in production, but the constructor should null-check and fall back to a safe default (e.g., empty string / 0) to avoid an NPE if a product is mis-configured.

**`SkuType.INAPP` → `BillingClient.ProductType.INAPP`.**
This is a simple constant rename. It appears in `queryProductDetailsAsync` (the `setProductType` call in `QueryProductDetailsParams.Product`), in `queryPurchasesAsync` (the `setProductType` call in `QueryPurchasesParams`), and potentially in `userHasDonated`. Confirm all three sites.

**`BillingResponseCode.SERVICE_TIMEOUT` in 7.x.**
The `onPurchasesUpdated` response-code `switch` at `:159-193` references `SERVICE_TIMEOUT`. This constant survives in `billing:7.x` — no change needed — but confirm during implementation that all `static import com.android.billingclient.api.BillingClient.BillingResponseCode.*` entries at `:33-42` resolve cleanly.

**`onPurchasesUpdated` and `acknowledgePurchase` are structurally unchanged.**
`BillingResult`, `Purchase`, `getPurchaseState()`, `isAcknowledged()`, `getPurchaseToken()`, `AcknowledgePurchaseParams`, and `billingClient.acknowledgePurchase(...)` all carry forward in `billing:7.x` without API-breaking changes. These paths require only compilation verification, not logic rewrites.

**No port/adapter abstraction is introduced.**
Per DES-MODERNIZATION-010 ADR-010-A, no `BillingPort` interface or Hexagonal seam is introduced. The donation subsystem is a leaf integration with a single consumer; the added indirection would be unjustified for this scope.

**Dependency on U-003 (SDK/AGP gate).**
`billing:7.x` requires a modern AGP/Gradle/compileSdk baseline that `billing:2.1.0` / AGP `4.1.3` / Gradle `7.2` / `compileSdk 29` cannot satisfy. This story must not be sprint-planned until U-003 (the SDK/AGP/`compileSdk` uplift, REQ-MODERNIZATION-001) is merged to master and CI is green. The `depends_on: U-003` frontmatter encodes this as a hard gate.

**`7.x` patch version.**
Pin the latest stable `7.x` patch (e.g., `7.1.1`) at implementation time. Record the exact version in an inline comment on the dependency line citing this story and the date pinned.

## Supporting Documentation

- `sdlc/artifacts/requirements/modernization/REQ-MODERNIZATION-010-upgrade-play-billing-to-v7.md` — governing requirement; AC-1 through AC-4 that this story satisfies
- `sdlc/artifacts/design/modernization/DES-MODERNIZATION-010-play-billing-v7-design.md` — §System Architecture (per-file change specification, component diagram), §Integration Design (leaf isolation, MainSettings caller contract, scope containment), §Design Validation (grep gates and test account verification), §Risks (async-conversion regression, `checkUserDonationStatus` attention note)

## Integration Contract References

None. This is a leaf integration (DES-MODERNIZATION-010 §Integration Contracts). The Play Billing IPC contract is owned externally by Google and pinned by `billing:7.x`. The single intra-module consumer edge (`MainSettings` → `DonationActivity.checkUserDonationStatus`) has its signature and `DonationStatusListener` callback contract preserved unchanged — it is not a newly introduced cross-component boundary and does not require a CNTR-\* artifact.

## Implementation Notes

<!-- Added by agents during build -->

## Review Findings

<!-- Summarized from workspace artifacts -->

## Notes

Not finalized. Leaf-scoped elective (M4) billing uplift; the async conversion of `checkUserDonationStatus` (`queryPurchases` → `queryPurchasesAsync` with callback-based dispatch) is the single highest-attention edit and must be covered by extended `DonationActivityTest` async-callback test cases; `MainSettings.java` is not modified.
