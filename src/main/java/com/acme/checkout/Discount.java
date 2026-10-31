package com.acme.checkout;

import java.util.Objects;

/**
 * Promotional discount rule applied during checkout.
 */
public final class Discount {

    public enum Type {
        PERCENTAGE,
        FIXED_AMOUNT,
        FREE_SHIPPING
    }

    private final String code;
    private final Type type;
    private final double value; // percentage (e.g. 10.0 for 10%) or fixed amount in minor units
    private final long minOrderMinor;

    public Discount(String code, Type type, double value, long minOrderMinor) {
        this.code = Objects.requireNonNull(code, "code must not be null").toUpperCase();
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.value = value;
        this.minOrderMinor = minOrderMinor;
    }

    public String code() {
        return code;
    }

    public Type type() {
        return type;
    }

    public double value() {
        return value;
    }

    public long minOrderMinor() {
        return minOrderMinor;
    }

    public long computeDiscountMinor(long orderTotalMinor) {
        if (orderTotalMinor < minOrderMinor) {
            return 0L;
        }
        if (type == Type.PERCENTAGE) {
            return Math.round(orderTotalMinor * (value / 100.0));
        } else if (type == Type.FIXED_AMOUNT) {
            return Math.min(orderTotalMinor, (long) value);
        }
        return 0L;
    }
}
