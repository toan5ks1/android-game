package com.toan5ks1.iap;


import java.util.List;

/**
 * This is a simple example used in Amazon InAppPurchase Sample App, to show how
 * developer's application holding the customer's InAppPurchase data.
 * 
 * 
 */
public class UserIapData {
    private List<SubscriptionRecord> subscriptionRecords;

    private boolean subsActive;
    private long subsFrom;
    private volatile int remainingOranges = 0;
    private volatile int consumedOranges = 0;
    private final String amazonUserId;
    private final String amazonMarketplace;
    public void setSubscriptionRecords(final List<SubscriptionRecord> subscriptionRecords) {
        this.subscriptionRecords = subscriptionRecords;
    }

    public List<SubscriptionRecord> getSubscriptionRecords() {
        return subscriptionRecords;
    }
    public String getAmazonUserId() {
        return amazonUserId;
    }

    public String getAmazonMarketplace() {
        return amazonMarketplace;
    }
    public boolean isSubsActiveCurrently() {
        return subsActive;
    }

    public long getCurrentSubsFrom() {
        return subsFrom;
    }

    public void setRemainingOranges(final int remaining) {
        this.remainingOranges = remaining;
    }

    public void setConsumedOranges(final int consumed) {
        this.consumedOranges = consumed;
    }

    public int getRemainingOranges() {
        return this.remainingOranges;
    }

    public int getConsumedOranges() {
        return this.consumedOranges;
    }

    public UserIapData(final String amazonUserId, final String amazonMarketplace) {
        this.amazonUserId = amazonUserId;
        this.amazonMarketplace = amazonMarketplace;
    }
    /**
     * Reload current subscription status from SubscriptionRecords
     */
    public void reloadSubscriptionStatus() {
        this.subsActive = false;
        this.subsFrom = 0;
        for (final SubscriptionRecord record : subscriptionRecords) {
            if (record.isActiveNow()) {
                this.subsActive = true;
                this.subsFrom = record.getFrom();
                return;
            }
        }
    }
}
