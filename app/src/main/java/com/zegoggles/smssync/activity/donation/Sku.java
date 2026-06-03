package com.zegoggles.smssync.activity.donation;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.billingclient.api.ProductDetails;

import static com.android.billingclient.api.BillingClient.ProductType.INAPP;

/**
 * A parcelable and comparable value object wrapping Google Play ProductDetails primitives.
 * Replaces the former SkuDetails-backed implementation (U-028 / 2026-06-02).
 */
public class Sku implements Parcelable, Comparable<Sku> {
    private final String type;
    private final String sku;
    private final String price;
    private final String title;
    private final String description;
    private final long priceAmountMicros;

    /**
     * Construct from a Play Billing 7.x {@link ProductDetails}.
     * Safe-defaults for null {@code getOneTimePurchaseOfferDetails()} — only INAPP
     * products are used here, so this should never be null in practice.
     */
    Sku(ProductDetails detail) {
        this.type = detail.getProductType();
        this.sku = detail.getProductId();
        this.title = detail.getTitle();
        this.description = detail.getDescription();
        ProductDetails.OneTimePurchaseOfferDetails offer = detail.getOneTimePurchaseOfferDetails();
        if (offer != null) {
            this.price = offer.getFormattedPrice();
            this.priceAmountMicros = offer.getPriceAmountMicros();
        } else {
            this.price = "";
            this.priceAmountMicros = 0L;
        }
    }

    /**
     * Primitive-field constructor used by {@link Sku.Test} fixtures and Parcel deserialization.
     *
     * @param type             product type (e.g. {@code "inapp"})
     * @param sku              the product ID
     * @param price            formatted price including currency sign
     * @param title            the title of the product
     * @param description      the description of the product
     * @param priceAmountMicros price in micro-units (1,000,000 = 1 currency unit)
     */
    Sku(String type, String sku, String price, String title, String description, long priceAmountMicros) {
        this.type = type;
        this.sku = sku;
        this.price = price;
        this.title = title;
        this.description = description;
        this.priceAmountMicros = priceAmountMicros;
    }

    private Sku(Parcel in) {
        this.sku = in.readString();
        this.title = in.readString();
        this.description = in.readString();
        this.price = in.readString();
        this.priceAmountMicros = in.readLong();
        this.type = in.readString();
    }

    public static final Creator<Sku> CREATOR = new Creator<Sku>() {
        @Override
        public Sku createFromParcel(Parcel in) {
            return new Sku(in);
        }

        @Override
        public Sku[] newArray(int size) {
            return new Sku[size];
        }
    };

    public String getType() {
        return type;
    }

    public String getSku() {
        return sku;
    }

    public String getPrice() {
        return price;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(sku);
        parcel.writeString(title);
        parcel.writeString(description);
        parcel.writeString(price);
        parcel.writeLong(priceAmountMicros);
        parcel.writeString(type);
    }

    @Override
    public int compareTo(Sku other) {
        int diff = Long.valueOf(priceAmountMicros).compareTo(other.priceAmountMicros);
        if (diff == 0) {
            diff = title.compareTo(other.title);
        }
        return diff;
    }

    /**
     * To test your implementation with static responses, you make an In-app Billing request using a special
     * item that has a reserved product ID. Each reserved product ID returns a specific static response
     * from Google Play. No money is transferred when you make In-app Billing requests with the reserved product
     * IDs. Also, you cannot specify the form of payment when you make a billing request with a
     * reserved product ID.
     *
     * @see <a href="http://developer.android.com/google/play/billing/billing_testing.html">
     * Testing in-app purchases with static responses
     * </a>
     */
    static class Test {
        static final String TEST_PREFIX = "android.test.";
        static final String TEST_PRICE = "$0.00";

        /**
         * When you make an In-app Billing request with this product ID, Google Play responds as though
         * you successfully purchased an item.
         */
        static final Sku PURCHASED =
                new Sku(INAPP, TEST_PREFIX + "purchased", TEST_PRICE, "Test (purchased)", "Purchased", 0);

        /**
         * When you make an In-app Billing request with this product ID Google Play responds as though
         * the purchase was canceled.
         */
        static final Sku CANCELED =
                new Sku(INAPP, TEST_PREFIX + "canceled", TEST_PRICE, "Test (canceled)", "Canceled", 0);

        /**
         * When you make an In-app Billing request with this product ID, Google Play responds as though
         * the purchase was refunded.
         */
        static final Sku REFUNDED =
                new Sku(INAPP, TEST_PREFIX + "refunded", TEST_PRICE, "Test (refunded)", "Refunded", 0);

        /**
         * When you make an In-app Billing request with this product ID, Google Play responds as though
         * the item being purchased was not listed in your application's product list.
         */
        static final Sku UNAVAILABLE =
                new Sku(INAPP, TEST_PREFIX + "item_unavailable", TEST_PRICE, "Test (unavailable)", "Unavailable", 0);

        static final Sku[] SKUS = {
                PURCHASED, CANCELED, REFUNDED, UNAVAILABLE
        };
    }
}
