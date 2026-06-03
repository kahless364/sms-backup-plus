package com.zegoggles.smssync.activity.donation;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.QueryPurchasesParams;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_UNAVAILABLE;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.buildActivity;
import static org.robolectric.shadows.ShadowToast.getTextOfLatestToast;
import static org.robolectric.shadows.ShadowToast.shownToastCount;

/**
 * Unit tests for {@link DonationActivity}.
 * Updated in U-028: uses Billing 7.x types; adds async-callback tests for
 * {@code checkUserDonationStatus} via a mocked {@link BillingClient}.
 */
@RunWith(RobolectricTestRunner.class)
public class DonationActivityTest {
    DonationActivity activity;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        activity = buildActivity(DonationActivity.class).get();
    }

    // -----------------------------------------------------------------------
    // onPurchasesUpdated tests (Billing 7.x types; logic unchanged from 2.x)
    // -----------------------------------------------------------------------

    @Test public void testResultMessageOK() {
        activity.onPurchasesUpdated(BillingResult.newBuilder().setResponseCode(OK).build(),
                Collections.<Purchase>emptyList());
        assertToast("Donation successful, thank you!");
    }

    @Test public void testResultMessageItemUnavailable() {
        activity.onPurchasesUpdated(
                BillingResult.newBuilder().setResponseCode(ITEM_UNAVAILABLE).build(),
                Collections.<Purchase>emptyList());
        assertToast("Donation failed: not available");
    }

    @Test public void testResultMessageItemAlreadyOwned() {
        activity.onPurchasesUpdated(
                BillingResult.newBuilder().setResponseCode(ITEM_ALREADY_OWNED).build(),
                Collections.<Purchase>emptyList());
        assertToast("Donation failed: you have already donated");
    }

    @Test public void testResultMessageUserCanceled() {
        activity.onPurchasesUpdated(
                BillingResult.newBuilder().setResponseCode(USER_CANCELED).build(),
                Collections.<Purchase>emptyList());
        assertToast("Donation failed: canceled");
    }

    @Test public void testResultMessageError() {
        activity.onPurchasesUpdated(
                BillingResult.newBuilder().setResponseCode(ERROR).build(),
                Collections.<Purchase>emptyList());
        assertToast("Donation failed: unspecified error: 6");
    }

    // -----------------------------------------------------------------------
    // checkUserDonationStatus async-callback tests (AC-6 / AC-8)
    // -----------------------------------------------------------------------

    /**
     * Helper: build a BillingClient mock that calls {@code onBillingSetupFinished} synchronously
     * when {@code startConnection} is invoked.
     */
    private BillingClient mockClientWithSetupResult(int responseCode) {
        BillingClient mockClient = mock(BillingClient.class);
        BillingResult setupResult = BillingResult.newBuilder().setResponseCode(responseCode).build();
        doAnswer(invocation -> {
            BillingClientStateListener stateListener = invocation.getArgument(0);
            stateListener.onBillingSetupFinished(setupResult);
            return null;
        }).when(mockClient).startConnection(any(BillingClientStateListener.class));
        return mockClient;
    }

    /**
     * Configure a mock BillingClient so that {@code queryPurchasesAsync} calls back synchronously
     * with the supplied purchases and response code.
     */
    private void stubQueryPurchasesAsync(BillingClient mockClient, int responseCode,
                                         List<Purchase> purchases) {
        BillingResult queryResult = BillingResult.newBuilder().setResponseCode(responseCode).build();
        doAnswer(invocation -> {
            PurchasesResponseListener responseListener = invocation.getArgument(1);
            responseListener.onQueryPurchasesResponse(queryResult, purchases);
            return null;
        }).when(mockClient).queryPurchasesAsync(
                any(QueryPurchasesParams.class),
                any(PurchasesResponseListener.class));
    }

    @Test
    public void checkUserDonationStatus_whenDonationPurchaseExists_reportsDONATED() {
        BillingClient mockClient = mockClientWithSetupResult(OK);

        // Create a mock purchase whose getProducts() contains a donation SKU.
        Purchase donationPurchase = mock(Purchase.class);
        org.mockito.Mockito.when(donationPurchase.getProducts())
                .thenReturn(Arrays.asList("donation.1"));

        stubQueryPurchasesAsync(mockClient, OK, Collections.singletonList(donationPurchase));

        DonationActivity.DonationStatusListener listener =
                mock(DonationActivity.DonationStatusListener.class);
        DonationActivity.checkUserDonationStatusWithClient(mockClient, listener);

        verify(listener).userDonationState(DonationActivity.DonationStatusListener.State.DONATED);
        verify(mockClient).endConnection();
    }

    @Test
    public void checkUserDonationStatus_whenNoDonationPurchase_reportsNOT_DONATED() {
        BillingClient mockClient = mockClientWithSetupResult(OK);

        // Empty purchase list: user has not donated.
        stubQueryPurchasesAsync(mockClient, OK, Collections.<Purchase>emptyList());

        DonationActivity.DonationStatusListener listener =
                mock(DonationActivity.DonationStatusListener.class);
        DonationActivity.checkUserDonationStatusWithClient(mockClient, listener);

        verify(listener).userDonationState(DonationActivity.DonationStatusListener.State.NOT_DONATED);
        verify(mockClient).endConnection();
    }

    @Test
    public void checkUserDonationStatus_whenQueryFails_reportsUNKNOWN() {
        BillingClient mockClient = mockClientWithSetupResult(OK);

        // Non-OK query result: UNKNOWN state.
        stubQueryPurchasesAsync(mockClient, ERROR, Collections.<Purchase>emptyList());

        DonationActivity.DonationStatusListener listener =
                mock(DonationActivity.DonationStatusListener.class);
        DonationActivity.checkUserDonationStatusWithClient(mockClient, listener);

        verify(listener).userDonationState(DonationActivity.DonationStatusListener.State.UNKNOWN);
        verify(mockClient).endConnection();
    }

    @Test
    public void checkUserDonationStatus_whenBillingUnavailable_reportsNOT_AVAILABLE() {
        // Connection fails with BILLING_UNAVAILABLE.
        BillingClient mockClient = mockClientWithSetupResult(BILLING_UNAVAILABLE);

        DonationActivity.DonationStatusListener listener =
                mock(DonationActivity.DonationStatusListener.class);
        DonationActivity.checkUserDonationStatusWithClient(mockClient, listener);

        verify(listener).userDonationState(DonationActivity.DonationStatusListener.State.NOT_AVAILABLE);
        verify(mockClient).endConnection();
    }

    @Test
    public void checkUserDonationStatus_whenSetupErrorNotBillingUnavailable_reportsUNKNOWN() {
        // Connection fails with a non-BILLING_UNAVAILABLE error (e.g. SERVICE_UNAVAILABLE).
        BillingClient mockClient = mockClientWithSetupResult(
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE);

        DonationActivity.DonationStatusListener listener =
                mock(DonationActivity.DonationStatusListener.class);
        DonationActivity.checkUserDonationStatusWithClient(mockClient, listener);

        verify(listener).userDonationState(DonationActivity.DonationStatusListener.State.UNKNOWN);
        verify(mockClient).endConnection();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void assertToast(String message) {
        assertThat(getTextOfLatestToast()).isEqualTo(message);
        assertThat(shownToastCount()).isEqualTo(1);
    }
}
