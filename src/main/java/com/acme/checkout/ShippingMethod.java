package com.acme.checkout;

/**
 * Fulfillment shipping tier options with cost schedules.
 */
public enum ShippingMethod {
    STANDARD(499L, 5),   // $4.99, 5 business days
    EXPRESS(999L, 2),    // $9.99, 2 business days
    OVERNIGHT(1999L, 1), // $19.99, 1 business day
    STORE_PICKUP(0L, 0); // Free local pickup

    private final long baseRateMinor;
    private final int estimatedDays;

    ShippingMethod(long baseRateMinor, int estimatedDays) {
        this.baseRateMinor = baseRateMinor;
        this.estimatedDays = estimatedDays;
    }

    public long baseRateMinor() {
        return baseRateMinor;
    }

    public int estimatedDays() {
        return estimatedDays;
    }
}
