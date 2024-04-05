package com.toan5ks1.iap;

import java.util.Arrays;
import java.util.List;

/**
 * 
 * MySku enum contains all In App Purchase products definition that the sample
 * app will use. The product definition includes two properties: "SKU" and
 * "Available Marketplace".
 * 
 */
public enum MySku {

    // This is the product to purchase and eat in the sample code.
    ORANGE("com.toan5ks1.floppybee.orange", Arrays.asList("AU", "BR", "CA", "CN", "DE", "ES", "FR", "GB", "IN", "IT",
            "JP", "MX", "US")),

    // This is a sample product to show how IAP SDK handle not supported
    // products
    APPLE("com.toan5ks1.floppybee.apple", Arrays.asList("AU", "BR", "CA", "CN", "DE", "ES", "FR", "GB", "IN", "IT",
            "JP", "MX", "US")),

    PLUS("com.toan5ks1.floppybee.premium.month",
                                  Arrays.asList("AU", "BR", "CA", "CN", "DE", "ES", "FR", "GB", "IN", "IT",
                                          "JP", "MX", "US"));

    private final String sku;
    private final List<String> availableMarkpetplaces;

    /**
     * Returns the MySku object from the specified Sku and marketplace value.
     * @param sku sku in string format
     * @param marketplace marketplace
     * @return MySku object corresponding to the sku and marketplace if possible, null otherwise
     */
    public static MySku fromSku(final String sku, final String marketplace) {
        if (ORANGE.getSku().equals(sku) && (ORANGE.getAvailableMarketplaces().contains(marketplace.toUpperCase()))) {
            return ORANGE;
        }
        return null;
    }

    /**
     * Returns the Sku string of the MySku object
     * 
     * @return sku
     */
    public String getSku() {
        return this.sku;
    }

    /**
     * Returns the Available Marketplace of the MySku object
     * @return list of available marketplaces
     */
    public List<String> getAvailableMarketplaces() {
        return this.availableMarkpetplaces;
    }

    MySku(final String sku, final List<String> availableMarkpetplaces) {
        this.sku = sku;
        this.availableMarkpetplaces = availableMarkpetplaces;
    }

}
