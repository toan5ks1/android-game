package com.toan5ks1.iap;

import java.util.HashSet;
import java.util.Set;

import android.util.Log;

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserDataResponse;

/**
 * Implementation of {@link PurchasingListener} that listens to Amazon
 * InAppPurchase SDK's events, and call {@link SampleIapManager} to handle the
 * purchase business logic.
 */
public class SamplePurchasingListener implements PurchasingListener {

    private static final String TAG = "SampleIAPConsumablesApp";

    private final SampleIapManager iapManager;

    public SamplePurchasingListener(final SampleIapManager iapManager) {
        this.iapManager = iapManager;
    }

    /**
     * This is the callback for {@link PurchasingService#getUserData}. For
     * successful case, get the current user from {@link UserDataResponse} and
     * call {@link SampleIapManager#setAmazonUserId} method to load the Amazon
     * user and related purchase information
     *
     * @param response response containing data of currently logged in user in the app
     */
    @Override
    public void onUserDataResponse(final UserDataResponse response) {
        Log.d(TAG, "onGetUserDataResponse: requestId (" + response.getRequestId()
                   + ") userIdRequestStatus: "
                   + response.getRequestStatus()
                   + ")");

        final UserDataResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
        case SUCCESSFUL:
            Log.d(TAG, "onUserDataResponse: get user id (" + response.getUserData().getUserId()
                       + ", marketplace ("
                       + response.getUserData().getMarketplace()
                       + ") ");
            iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
            break;

        case FAILED:
        case NOT_SUPPORTED:
            Log.d(TAG, "onUserDataResponse failed, status code is " + status);
            iapManager.setAmazonUserId(null, null);
            break;
        }
    }

    /**
     * This is the callback for {@link PurchasingService#getProductData}. After
     * SDK sends the product details and availability to this method, it will
     * call {@link SampleIapManager#enablePurchaseForSkus}
     * {@link SampleIapManager#disablePurchaseForSkus} or
     * {@link SampleIapManager#disableAllPurchases} method to set the purchase
     * status accordingly.
     */
    @Override
    public void onProductDataResponse(final ProductDataResponse response) {
        final ProductDataResponse.RequestStatus status = response.getRequestStatus();
        Log.d(TAG, "onProductDataResponse: RequestStatus (" + status + ")");

        switch (status) {
        case SUCCESSFUL:
            Log.d(TAG, "onProductDataResponse: successful.  The item data map in this response includes the valid SKUs");
            final Set<String> unavailableSkus = response.getUnavailableSkus();
            Log.d(TAG, "onProductDataResponse: " + unavailableSkus.size() + " unavailable skus");
            iapManager.enablePurchaseForSkus(response.getProductData());
            iapManager.disablePurchaseForSkus(response.getUnavailableSkus());
            break;
        case FAILED:
        case NOT_SUPPORTED:
            Log.d(TAG, "onProductDataResponse: failed, should retry request");
            iapManager.disableAllPurchases();
            break;
        }
    }

    /**
     * This is the callback for {@link PurchasingService#getPurchaseUpdates}.
     * 
     * We will receive Consumable receipts from this callback if the consumable
     * receipts are not marked as "FULFILLED" in Amazon Appstore. So for every
     * single Consumable receipts in the response, we need to call
     * {@link SampleIapManager#handleReceipt} to fulfill the purchase.
     * 
     */
    @Override
    public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse response) {
        Log.d(TAG, "onPurchaseUpdatesResponse: requestId (" + response.getRequestId()
                   + ") purchaseUpdatesResponseStatus ("
                   + response.getRequestStatus()
                   + ") userId ("
                   + response.getUserData().getUserId()
                   + ")");
        final PurchaseUpdatesResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
        case SUCCESSFUL:
            iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
            for (final Receipt receipt : response.getReceipts()) {
                iapManager.handleReceipt(receipt, response.getUserData());
            }
            if (response.hasMore()) {
                PurchasingService.getPurchaseUpdates(false);
            }
            iapManager.refreshOranges();
            break;
        case FAILED:
        case NOT_SUPPORTED:
            Log.d(TAG, "onProductDataResponse: failed, should retry request");
            iapManager.disableAllPurchases();
            break;
        }

    }

    /**
     * This is the callback for {@link PurchasingService#purchase}. For each
     * time the application sends a purchase request
     * {@link PurchasingService#purchase}, Amazon Appstore will call this
     * callback when the purchase request is completed. If the RequestStatus is
     * Successful or AlreadyPurchased then application needs to call
     * {@link SampleIapManager#handleReceipt} to handle the purchase
     * fulfillment. If the RequestStatus is INVALID_SKU, NOT_SUPPORTED, or
     * FAILED, notify corresponding method of {@link SampleIapManager} .
     */
    @Override
    public void onPurchaseResponse(final PurchaseResponse response) {
        final String requestId = response.getRequestId().toString();
        final String userId = response.getUserData().getUserId();
        final PurchaseResponse.RequestStatus status = response.getRequestStatus();
        Log.d(TAG, "onPurchaseResponse: requestId (" + requestId
                   + ") userId ("
                   + userId
                   + ") purchaseRequestStatus ("
                   + status
                   + ")");

        switch (status) {
        case SUCCESSFUL:
            final Receipt receipt = response.getReceipt();
            iapManager.setAmazonUserId(response.getUserData().getUserId(), response.getUserData().getMarketplace());
            Log.d(TAG, "onPurchaseResponse: receipt json:" + receipt.toJSON());
            iapManager.handleReceipt(receipt, response.getUserData());
            iapManager.refreshOranges();
            break;
        case PENDING:
            Log.d(TAG, "onPurchaseResponse: pending remote approval -- purchase will be returned in getPurchaseUpdates if approved");
            break;
        case ALREADY_PURCHASED:
            Log.d(TAG, "onPurchaseResponse: already purchased, should never get here for a consumable.");
            // This is not applicable for consumable item. It is only
            // application for entitlement and subscription.
            // check related samples for more details.
            break;
        case INVALID_SKU:
            Log.d(TAG,
                  "onPurchaseResponse: invalid SKU!  onProductDataResponse should have disabled buy button already.");
            final Set<String> unavailableSkus = new HashSet<>();
            unavailableSkus.add(response.getReceipt().getSku());
            iapManager.disablePurchaseForSkus(unavailableSkus);
            break;
        case FAILED:
        case NOT_SUPPORTED:
            Log.d(TAG, "onPurchaseResponse: failed so remove purchase request from local storage");
            iapManager.showPurchaseFailedMessage(response.getReceipt().getSku());
            break;
        }

    }

}
