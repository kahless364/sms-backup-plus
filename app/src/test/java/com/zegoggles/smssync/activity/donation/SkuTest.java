package com.zegoggles.smssync.activity.donation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.android.billingclient.api.BillingClient.ProductType.INAPP;
import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for the {@link Sku} value object.
 * Updated in U-028: constructor no longer takes {@code originalJson}; Parcel round-trip
 * uses primitive fields directly. {@link Sku.Test} reserved-ID fixtures are preserved.
 */
@RunWith(RobolectricTestRunner.class)
public class SkuTest {
    // Primitive-field constructor: (type, sku, price, title, description, priceAmountMicros)
    private Sku sku1 = new Sku(INAPP, "test.sku.1", "$10.00", "Test 1", "Test 1", 10000);
    private Sku sku2 = new Sku(INAPP, "test.sku.2", "$7.00",  "Test 2", "Test 2", 7000);
    private Sku sku3 = new Sku(INAPP, "test.sku.3", "$3.50",  "Test 3", "Test 3", 3500);
    private Sku sku4 = new Sku(INAPP, "test.sku.4", "$3.50",  "Test 4", "Test 3", 3500);

    @Test public void testComparableComparesByPrice() {
        assertThat(sku1).isGreaterThan(sku2);
        assertThat(sku1).isGreaterThan(sku3);
        assertThat(sku3).isLessThan(sku2);
        assertThat(sku3).isLessThan(sku1);
    }

    @Test public void testSameObjectIsEqual() {
        assertThat(sku1).isEquivalentAccordingToCompareTo(sku1);
    }

    @Test public void testEqualPricesAreComparedByTitle() {
        assertThat(sku4).isGreaterThan(sku3);
    }

    @Test public void testTestSkusHaveAndroidTestPrefix() {
        for (Sku testSku : Sku.Test.SKUS) {
            assertThat(testSku.getSku()).startsWith("android.test.");
        }
    }

    @Test public void testTestSkuProductIds() {
        assertThat(Sku.Test.PURCHASED.getSku()).isEqualTo("android.test.purchased");
        assertThat(Sku.Test.CANCELED.getSku()).isEqualTo("android.test.canceled");
        assertThat(Sku.Test.REFUNDED.getSku()).isEqualTo("android.test.refunded");
        assertThat(Sku.Test.UNAVAILABLE.getSku()).isEqualTo("android.test.item_unavailable");
    }
}
