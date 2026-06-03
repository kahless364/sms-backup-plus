package com.zegoggles.smssync.activity.donation;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.zegoggles.smssync.BuildConfig;
import com.zegoggles.smssync.R;
import com.zegoggles.smssync.activity.ThemeActivity;
import com.zegoggles.smssync.activity.donation.DonationListFragment.ProductSelectionListener;
import com.zegoggles.smssync.utils.BundleBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.widget.Toast.LENGTH_LONG;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.*;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_NOT_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED;
import static com.android.billingclient.api.BillingClient.ProductType.INAPP;
import static com.android.billingclient.api.Purchase.PurchaseState.PURCHASED;
import static com.zegoggles.smssync.App.TAG;
import static com.zegoggles.smssync.Consts.Billing.ALL_SKUS;
import static com.zegoggles.smssync.Consts.Billing.DONATION_PREFIX;
import static com.zegoggles.smssync.activity.donation.DonationActivity.DonationStatusListener.State.DONATED;
import static com.zegoggles.smssync.activity.donation.DonationActivity.DonationStatusListener.State.NOT_AVAILABLE;
import static com.zegoggles.smssync.activity.donation.DonationActivity.DonationStatusListener.State.NOT_DONATED;
import static com.zegoggles.smssync.activity.donation.DonationActivity.DonationStatusListener.State.UNKNOWN;
import static com.zegoggles.smssync.activity.donation.DonationListFragment.SKUS;

public class DonationActivity extends ThemeActivity implements
        ProductDetailsResponseListener,
        PurchasesUpdatedListener,
        ProductSelectionListener {

    public interface DonationStatusListener {
        enum State {
            DONATED,
            NOT_DONATED,
            UNKNOWN,
            NOT_AVAILABLE
        }
        void userDonationState(State state);
    }

    private static boolean DEBUG_IAB = BuildConfig.DEBUG;
    private @Nullable BillingClient billingClient;
    private boolean stateSaved;
    // Holds the sorted ProductDetails list presented in DonationListFragment,
    // so that selectedProduct(int) can resolve by index without JSON round-trips.
    private List<ProductDetails> presentedProducts;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        billingClient = BillingClient.newBuilder(this).setListener(this).enablePendingPurchases().build();
        billingClient.startConnection(new BillingClientStateListener() {

            @Override public void onBillingSetupFinished(BillingResult resultCode) {
                log("onBillingSetupFinished(" + resultCode + ")" + Thread.currentThread().getName());

                switch (resultCode.getResponseCode()) {
                    case OK:
                        queryAvailableProducts();
                        break;

                    case BILLING_UNAVAILABLE:
                    case DEVELOPER_ERROR:
                    case ERROR:
                    case FEATURE_NOT_SUPPORTED:
                    case ITEM_ALREADY_OWNED:
                    case ITEM_NOT_OWNED:
                    case ITEM_UNAVAILABLE:
                    case SERVICE_DISCONNECTED:
                    case SERVICE_UNAVAILABLE:
                    case USER_CANCELED:
                    case SERVICE_TIMEOUT:
                    default:
                        Toast.makeText(DonationActivity.this, R.string.donation_error_iab_unavailable, LENGTH_LONG).show();
                        Log.w(TAG, "Problem setting up in-app billing: " + resultCode);
                        finish();
                        break;
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "onBillingServiceDisconnected");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        stateSaved = true;
    }

    // ProductDetailsResponseListener
    @Override
    public void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> details) {
        log("onProductDetailsResponse(" + billingResult + ", " + details + ")");
        if (billingResult.getResponseCode() != OK) {
            Log.w(TAG, "failed to query inventory: " + billingResult);
            return;
        }

        if (isFinishing() || stateSaved) {
            Log.w(TAG, "activity no longer active");
            return;
        }

        List<ProductDetails> filtered = new ArrayList<ProductDetails>();
        for (ProductDetails d : details) {
            if (d.getProductId().startsWith(DONATION_PREFIX)) {
                filtered.add(d);
            }
        }
        showSelectDialog(filtered);
    }

    /**
     * @param result    response code of the update
     * @param purchases list of updated purchases if present
     */
    @Override
    public void onPurchasesUpdated(BillingResult result, @Nullable List<Purchase> purchases) {
        log("onPurchasesUpdated(" + result + ", " + purchases + ")");
        String message;
        switch (result.getResponseCode()) {
            case OK:
                if (purchases != null) {
                    for (Purchase p : purchases) {
                        acknowledgePurchase(p);
                    }
                }
                message = getString(R.string.ui_donation_success_message);
                break;
            case ITEM_UNAVAILABLE:
                message = getString(R.string.ui_donation_failure_message,
                        getString(R.string.donation_error_unavailable));
                break;
            case ITEM_ALREADY_OWNED:
                message = getString(R.string.ui_donation_failure_message,
                        getString(R.string.donation_error_already_owned));
                break;
            case USER_CANCELED:
                message = getString(R.string.ui_donation_failure_message,
                        getString(R.string.donation_error_canceled));
                break;
            case BILLING_UNAVAILABLE:
            case FEATURE_NOT_SUPPORTED:
            case ITEM_NOT_OWNED:
            case SERVICE_DISCONNECTED:
            case SERVICE_UNAVAILABLE:
            case DEVELOPER_ERROR:
            case ERROR:
            case SERVICE_TIMEOUT:
            default:
                message = getString(R.string.ui_donation_failure_message,
                        getString(R.string.donation_unspecified_error, result.getResponseCode()));
                break;
        }

        Toast.makeText(this, message, LENGTH_LONG).show();
        finish();
    }

    // ProductSelectionListener — index-based; DonationListFragment no longer reconstructs
    // billing objects, so the activity resolves the ProductDetails from its in-memory list.
    @Override
    public void selectedProduct(int index) {
        if (billingClient == null || presentedProducts == null) return;
        if (index < 0 || index >= presentedProducts.size()) return;
        ProductDetails pd = presentedProducts.get(index);
        if (DEBUG_IAB) {
            Log.v(TAG, "selectedProduct(" + pd.getProductId() + ")");
        }
        List<BillingFlowParams.ProductDetailsParams> paramsList = new ArrayList<BillingFlowParams.ProductDetailsParams>();
        paramsList.add(BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(pd)
                .build());
        billingClient.launchBillingFlow(this, BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(paramsList)
                .build());
    }

    private void queryAvailableProducts() {
        if (billingClient == null) return;
        List<QueryProductDetailsParams.Product> productList = new ArrayList<QueryProductDetailsParams.Product>();
        for (String id : ALL_SKUS) {
            productList.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(INAPP)
                    .build());
        }
        billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder()
                        .setProductList(productList)
                        .build(),
                this);
    }

    private void showSelectDialog(List<ProductDetails> productDetails) {
        if (billingClient == null) return;
        ArrayList<Sku> skus = new ArrayList<Sku>(productDetails.size());
        for (ProductDetails detail : productDetails) {
            skus.add(new Sku(detail));
        }
        if (DEBUG_IAB) {
            Collections.addAll(skus, Sku.Test.SKUS);
        }
        Collections.sort(skus);

        // Build a parallel sorted list of ProductDetails that mirrors the Sku ordering,
        // so selectedProduct(int index) can resolve ProductDetails by the same index.
        presentedProducts = new ArrayList<ProductDetails>(productDetails.size());
        for (Sku sku : skus) {
            // Match each sorted Sku back to its ProductDetails by productId.
            // Sku.Test entries have no ProductDetails; skip them (index will be out-of-range
            // for those, and they are only shown in DEBUG_IAB builds for testing display).
            for (ProductDetails pd : productDetails) {
                if (pd.getProductId().equals(sku.getSku())) {
                    presentedProducts.add(pd);
                    break;
                }
            }
        }

        final DonationListFragment donationList = new DonationListFragment();
        donationList.setArguments(new BundleBuilder().putParcelableArrayList(SKUS, skus).build());
        donationList.show(getSupportFragmentManager(), null);
    }

    private static void log(String s) {
        if (DEBUG_IAB) {
            Log.d(TAG, s);
        }
    }

    // https://developer.android.com/google/play/billing/billing_library_overview#acknowledge
    private void acknowledgePurchase(final Purchase purchase) {
        if (purchase.getPurchaseState() == PURCHASED && !purchase.isAcknowledged() && billingClient != null) {
            AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();

            billingClient.acknowledgePurchase(params, new AcknowledgePurchaseResponseListener() {
                @Override
                public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                    log("onAcknowledgePurchaseResponse(" + billingResult + ")");
                    if (billingResult.getResponseCode() != OK) {
                        Log.w(TAG, "not acknowledged purchase " + purchase + ":" + billingResult);
                    }
                }
            });
        }
    }


    /**
     * Check the user's donation status asynchronously.
     * <p>
     * The public signature and {@link DonationStatusListener} callback contract are preserved
     * unchanged (U-028). The internal implementation replaces the removed synchronous
     * {@code queryPurchases(String)} with the async {@code queryPurchasesAsync}.
     * All dispatch and {@code endConnection()} happen inside the callback — the calling
     * thread ({@code MainSettings.onResume()}) is never blocked.
     * </p>
     *
     * @param context  application context
     * @param listener callback that receives one of:
     *                 {@link DonationStatusListener.State#DONATED},
     *                 {@link DonationStatusListener.State#NOT_DONATED},
     *                 {@link DonationStatusListener.State#UNKNOWN},
     *                 {@link DonationStatusListener.State#NOT_AVAILABLE}
     */
    public static void checkUserDonationStatus(Context context,
                                               final DonationStatusListener listener) {
        final BillingClient helper = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(BillingResult result, @Nullable List<Purchase> purchases) {
                        log("checkUserDonationStatus: onPurchasesUpdated(" + result + ", " + purchases + ")");
                    }
                }).build();
        checkUserDonationStatusWithClient(helper, listener);
    }

    /**
     * Package-private overload used by unit tests to inject a pre-built (mock) BillingClient.
     */
    static void checkUserDonationStatusWithClient(final BillingClient helper,
                                                   final DonationStatusListener listener) {
        helper.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                log("checkUserHasDonated: onBillingSetupFinished(" + result + ")");
                if (result.getResponseCode() == OK) {
                    helper.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder()
                                    .setProductType(INAPP)
                                    .build(),
                            new PurchasesResponseListener() {
                                @Override
                                public void onQueryPurchasesResponse(BillingResult queryResult,
                                                                     List<Purchase> purchases) {
                                    log("checkUserHasDonated: onQueryPurchasesResponse(" + queryResult + ")");
                                    if (queryResult.getResponseCode() == OK) {
                                        listener.userDonationState(
                                                userHasDonated(purchases) ? DONATED : NOT_DONATED);
                                    } else {
                                        listener.userDonationState(UNKNOWN);
                                    }
                                    helper.endConnection();
                                }
                            });
                } else {
                    listener.userDonationState(
                            result.getResponseCode() == BILLING_UNAVAILABLE ? NOT_AVAILABLE : UNKNOWN);
                    helper.endConnection();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Connection dropped without a setup-finished call; treat as NOT_AVAILABLE.
                log("checkUserHasDonated: onBillingServiceDisconnected");
            }
        });
    }

    private static boolean userHasDonated(List<Purchase> purchases) {
        for (String sku : ALL_SKUS) {
            for (Purchase purchase : purchases) {
                if (purchase.getProducts().contains(sku)) {
                    return true;
                }
            }
        }
        return false;
    }
}
